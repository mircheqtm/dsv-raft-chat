package cz.cvut.fel.dsv.raftchat.modes;

import cz.cvut.fel.dsv.raftchat.Node;
import cz.cvut.fel.dsv.raftchat.base.Log;
import cz.cvut.fel.dsv.raftchat.base.dtos.NodeDto;
import cz.cvut.fel.dsv.raftchat.base.messages.AppendEntryMessage;
import cz.cvut.fel.dsv.raftchat.base.messages.AppendEntryResponse;
import cz.cvut.fel.dsv.raftchat.model.NodeState;

import java.rmi.RemoteException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

import static cz.cvut.fel.dsv.raftchat.Node.LEADER_SEND_APPEND_ENTRY_DELAY;

public class LeaderMode extends AbstractMode {

    private final Node node;
    private CountDownLatch latchInner;

    public LeaderMode(Node node, CountDownLatch latch) {
        super(latch);
        this.node = node;
        this.latchInner = new CountDownLatch(node.getAllNodes().size() - 1);
    }

    @Override
    public void run() {
        Logger.getGlobal().log(Node.LEVEL, () ->
                String.format("%s now in LEADER MODE (term %s)", node.getId(), node.getTerm())
        );
        for (Map.Entry<String, NodeDto> nodeDto : node.getAllNodes().entrySet()) {
            if (!nodeDto.getValue().getId().equals(node.getId()))
                startSenderThread(nodeDto.getValue());
        }
        try {
            latchInner.await();
        } catch (InterruptedException e) {
            System.out.println("Latch failed");
        }
        latch.countDown();
    }

    private void startSenderThread(NodeDto target) {
        Runnable sendRequestVoteLambda = () -> {
            while (!kill) {
                try {
                    Thread.sleep(LEADER_SEND_APPEND_ENTRY_DELAY);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (node.getState() != NodeState.LEADER) {
                    break;
                }
                Log newLog = null;
                try {
                    newLog = node.getLogs().get(target.getNextIndex());
                } catch (IndexOutOfBoundsException ignored) {
                }
                int prevLogIndex = target.getMatchIndex();
                int prevLogTerm;
                int committedIndex = findCommittedIndex(target);
                try {
                    prevLogTerm = node.getLogs().get(target.getMatchIndex()).getTerm();
                } catch (NullPointerException | IndexOutOfBoundsException n) {
                    prevLogTerm = 0;
                }
                AppendEntryMessage message = AppendEntryMessage.builder()
                        .id(node.getId() + "Message" + node.getLastIdSent())
                        .leaderId(node.getId())
                        .prevLogIndex(prevLogIndex)
                        .prevLogTerm(prevLogTerm)
                        .commitIndex(committedIndex)
                        .newLog(newLog)
                        .term(node.getTerm())
                        .build();
                node.incrementLastSendId();
                AppendEntryResponse response;
                if (node.isConnected()) {
                    response = getResponse(message, target);
                    resolveLogs(target, response);
                }
            }
            latchInner.countDown();
        };
        Thread thread = new Thread(sendRequestVoteLambda);
        thread.start();
    }

    private int findCommittedIndex(NodeDto target) {
        for (int i = node.getLogs().size() - 1; i >= 1; i--) {
            if (node.getLogs().get(i).isCommitted() && target.getNextIndex() >= i) {
                return i;
            }
        }

        return 0;
    }

    private void resolveLogs(NodeDto target, AppendEntryResponse response) {
        if (response != null) {
            target.setMatchIndex(response.getMatchIndex());
            target.setNextIndex(response.getMatchIndex() + 1);
            int sum = 0;
            for (Map.Entry<String, NodeDto> dtoEntry : node.getAllNodes().entrySet()) {
                NodeDto dto = dtoEntry.getValue();
                if (dto.getMatchIndex() >= response.getMatchIndex()) {
                    sum++;
                }
            }
            if (sum >= node.getMajority()) {
                node.commitLog(response.getMatchIndex());
            }
        }
    }

    private AppendEntryResponse getResponse(AppendEntryMessage appendEntryMessage, NodeDto target) {
        AppendEntryResponse response = null;
        if (target.getCommands() == null) {
            target.setCommands(Node.getCommandsForNodeDto(target));
        }
        try {
            response = target.getCommands().visitAppendEntry(appendEntryMessage);
        } catch (RemoteException e) {
            target.setCommands(Node.getCommandsForNodeDto(target));
        } catch (NullPointerException ignored) {

        }

        return response;
    }
}

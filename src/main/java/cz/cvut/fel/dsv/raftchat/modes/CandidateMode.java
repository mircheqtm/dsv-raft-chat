package cz.cvut.fel.dsv.raftchat.modes;

import cz.cvut.fel.dsv.raftchat.Node;
import cz.cvut.fel.dsv.raftchat.base.dtos.NodeDto;
import cz.cvut.fel.dsv.raftchat.base.messages.RequestVote;
import cz.cvut.fel.dsv.raftchat.base.messages.RequestVoteResponse;
import cz.cvut.fel.dsv.raftchat.helpers.RandomTimeGenerator;
import cz.cvut.fel.dsv.raftchat.model.NodeState;
import lombok.SneakyThrows;

import java.rmi.RemoteException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

import static cz.cvut.fel.dsv.raftchat.Node.CANDIDATE_NO_ANSWER_DELAY;


public class CandidateMode extends AbstractMode {

    private final Node node;
    private int votedForMe = 1;

    public CandidateMode(Node node, CountDownLatch latch) {
        super(latch);
        this.node = node;
    }

    @SneakyThrows
    @Override
    public void run() {
        Logger.getGlobal().log(Node.LEVEL, () ->
                "Entering CANDIDATE MODE and starting VOTES"
        );
        node.incrementTerm();
        for (Map.Entry<String, NodeDto> nodeDto : node.getAllNodes().entrySet()) {
            if (!nodeDto.getValue().getId().equals(node.getId()))
                sendRequestVote(nodeDto.getValue());
        }

        int timeToWait = RandomTimeGenerator.generateRandomWaitTime();
        Thread.sleep(timeToWait);
        if (votedForMe >= node.getMajority() && node.getState() == NodeState.CANDIDATE) {
            node.setState(NodeState.LEADER);
        }
        kill = true;
    }

    private void sendRequestVote(NodeDto target) {
        Runnable sendRequestVoteLambda = () -> {
            RequestVote requestVote = RequestVote.builder()
                    .idFrom(node.getId())
                    .term(node.getTerm())
                    .build();
            RequestVoteResponse response = null;
            while (response == null && !kill) {
                if (node.isConnected() && node.getState() == NodeState.CANDIDATE) {
                    response = getResponse(requestVote, target);
                    if (response == null) {
                        try {
                            Thread.sleep(CANDIDATE_NO_ANSWER_DELAY);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    break;
                }
            }
            if (!kill && response != null && response.isApproved()) {
                incrementVotedForMe();
            }
            latch.countDown();
        };
        Thread thread = new Thread(sendRequestVoteLambda);
        thread.start();
    }

    private synchronized void incrementVotedForMe() {
        this.votedForMe++;
    }

    private RequestVoteResponse getResponse(RequestVote requestVote, NodeDto target) {
        RequestVoteResponse response = null;
        if (target.getCommands() == null) {
            target.setCommands(Node.getCommandsForNodeDto(target));
        }
        try {
            response = target.getCommands().visitRequestVote(requestVote);
        } catch (RemoteException | NullPointerException e) {
            target.setCommands(Node.getCommandsForNodeDto(target));
        }

        return response;
    }


}

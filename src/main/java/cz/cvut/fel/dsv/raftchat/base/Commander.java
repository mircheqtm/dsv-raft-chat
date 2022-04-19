package cz.cvut.fel.dsv.raftchat.base;

import cz.cvut.fel.dsv.raftchat.Node;
import cz.cvut.fel.dsv.raftchat.base.dtos.MessageDto;
import cz.cvut.fel.dsv.raftchat.base.dtos.NodeDto;
import cz.cvut.fel.dsv.raftchat.base.messages.AppendEntryMessage;
import cz.cvut.fel.dsv.raftchat.base.messages.AppendEntryResponse;
import cz.cvut.fel.dsv.raftchat.base.messages.RequestVote;
import cz.cvut.fel.dsv.raftchat.base.messages.RequestVoteResponse;
import cz.cvut.fel.dsv.raftchat.model.NodeState;

import java.rmi.RemoteException;
import java.util.List;
import java.util.logging.Logger;

public class Commander implements Commands {

    private final Node myNode;

    public Commander(Node node) {
        this.myNode = node;
    }

    @Override
    public void disconnect(String id) throws RemoteException {
        if (id.equals(myNode.getId())) {
            myNode.disconnect();
            return;
        }
        NodeDto node = Node.findNodeById(id);
        node.getCommands().disconnect(id);
    }

    @Override
    public void connect(String id) throws RemoteException {
        if (id.equals(myNode.getId())) {
            myNode.connect();
            return;
        }
        NodeDto node = Node.findNodeById(id);
        node.getCommands().connect(id);
    }

    @Override
    public boolean findLeaderAndSendMessage(MessageDto message) throws RemoteException {
        if (myNode.getState() == NodeState.LEADER) {
            myNode.addNewLogOutOfMessageDto(message);
            return true;
        } else {
            String leaderId = myNode.getLeaderId();
            NodeDto leaderDto = Node.findNodeById(leaderId);
            return leaderDto.getCommands().findLeaderAndSendMessage(message);
        }
    }

    @Override
    public AppendEntryResponse visitAppendEntry(AppendEntryMessage message) throws RemoteException {
        if (!myNode.isConnected()) {
            throw new RemoteException();
        }
        Logger.getGlobal().log(Node.LEVEL, () ->
                String.format("got appendEntry with id : %s my term is : %s message term is: %s message got from: %s my state: %s \n my current logs are: %s", message.getId(), myNode.getTerm(), message.getTerm(), message.getLeaderId(), myNode.getState(), myNode.getLogs())
        );
        AppendEntryResponse badResponse = AppendEntryResponse.builder()
                .success(false)
                .term(myNode.getTerm())
                .build();
        AppendEntryResponse goodResponse = AppendEntryResponse.builder()
                .success(true)
                .term(myNode.getTerm())
                .build();

        if (message.getTerm() < myNode.getTerm()) {
            return badResponse;
        } else if (message.getTerm() >= myNode.getTerm()) {
            myNode.setTerm(message.getTerm());
            if (myNode.getState() != NodeState.FOLLOWER) {
                myNode.setState(NodeState.FOLLOWER);
            }
        }
        myNode.setLeaderId(message.getLeaderId());
        myNode.setLastMessage(message);
        Log l = message.getNewLog();
        int matchIndex = myNode.getLogs().size() - 1;
        if (l == null) {
            myNode.getLogs().get(message.getCommitIndex()).setCommitted(true);
            goodResponse.setMatchIndex(matchIndex);
            return goodResponse;
        } else {
            matchIndex = myNode.processAppendEntry(message);
        }
        goodResponse.setMatchIndex(matchIndex);
        return goodResponse;
    }

    @Override
    public RequestVoteResponse visitRequestVote(RequestVote requestVote) throws RemoteException {
        if (!myNode.isConnected()) {
            throw new RemoteException();
        }
        RequestVoteResponse response = new RequestVoteResponse();
        if (myNode.getTerm() < requestVote.getTerm()) {
            myNode.setTerm(requestVote.getTerm());
            response.setApproved(true);
            response.setTerm(requestVote.getTerm());
        } else {
            response.setApproved(false);
            response.setTerm(myNode.getTerm());
        }
        return response;
    }

    @Override
    public void startWork(List<NodeDto> initDtos, int slowMoMultiplier) throws RemoteException {
        myNode.initDtos(initDtos);
        myNode.setSlowMo(slowMoMultiplier);
        myNode.getStartLatch().countDown();
    }

}

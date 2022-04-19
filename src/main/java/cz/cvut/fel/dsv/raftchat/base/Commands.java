package cz.cvut.fel.dsv.raftchat.base;

import cz.cvut.fel.dsv.raftchat.base.dtos.MessageDto;
import cz.cvut.fel.dsv.raftchat.base.dtos.NodeDto;
import cz.cvut.fel.dsv.raftchat.base.messages.AppendEntryMessage;
import cz.cvut.fel.dsv.raftchat.base.messages.AppendEntryResponse;
import cz.cvut.fel.dsv.raftchat.base.messages.RequestVote;
import cz.cvut.fel.dsv.raftchat.base.messages.RequestVoteResponse;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface Commands extends Remote {
    void disconnect(String id) throws RemoteException;

    void connect(String id) throws RemoteException;

    boolean findLeaderAndSendMessage(MessageDto message) throws RemoteException;

    AppendEntryResponse visitAppendEntry(AppendEntryMessage message) throws RemoteException;

    RequestVoteResponse visitRequestVote(RequestVote requestVote) throws RemoteException;

    void startWork(List<NodeDto> initDtos, int slowMoMultiplier) throws RemoteException ;
}

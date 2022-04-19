package cz.cvut.fel.dsv.raftchat.client;

import cz.cvut.fel.dsv.raftchat.base.dtos.NodeDto;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface ClientRMIInterface extends Remote {
    void initAllNodes(List<NodeDto> nodes) throws RemoteException;
}

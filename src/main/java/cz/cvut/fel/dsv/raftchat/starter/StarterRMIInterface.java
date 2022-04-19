package cz.cvut.fel.dsv.raftchat.starter;

import cz.cvut.fel.dsv.raftchat.base.Address;
import cz.cvut.fel.dsv.raftchat.base.dtos.NodeDto;

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface StarterRMIInterface extends Remote {

    void addToNodePool(NodeDto dto) throws RemoteException;

    void addClient(Address clientAddress) throws RemoteException, NotBoundException;

    void startBefore() throws RemoteException;
}

package cz.cvut.fel.dsv.raftchat.helpers;

import cz.cvut.fel.dsv.raftchat.base.Address;
import cz.cvut.fel.dsv.raftchat.base.Commands;
import cz.cvut.fel.dsv.raftchat.client.ClientRMIInterface;
import cz.cvut.fel.dsv.raftchat.starter.StarterRMIInterface;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import static cz.cvut.fel.dsv.raftchat.Node.COMMANDS_NAME;

public class RMIFinder {

    public static Commands getNodeCommands(Address address) throws RemoteException, NotBoundException {
        Registry leaderRegistry = LocateRegistry.getRegistry(address.getHostname(), address.getPort());
        return (Commands) leaderRegistry.lookup(COMMANDS_NAME);
    }

    public static StarterRMIInterface getStarterRMI(Address address) throws RemoteException, NotBoundException {
        Registry starterReg = LocateRegistry.getRegistry(address.getHostname(), address.getPort());
        return (StarterRMIInterface) starterReg.lookup("starter");
    }

    public static ClientRMIInterface getClientRMI(Address address) throws RemoteException, NotBoundException {
        Registry clientReg = LocateRegistry.getRegistry(address.getHostname(), address.getPort());
        return (ClientRMIInterface) clientReg.lookup("client");
    }
}


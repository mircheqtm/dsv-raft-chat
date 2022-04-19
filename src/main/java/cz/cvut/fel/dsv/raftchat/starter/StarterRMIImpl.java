package cz.cvut.fel.dsv.raftchat.starter;

import cz.cvut.fel.dsv.raftchat.Node;
import cz.cvut.fel.dsv.raftchat.base.Address;
import cz.cvut.fel.dsv.raftchat.base.dtos.NodeDto;
import cz.cvut.fel.dsv.raftchat.helpers.RMIFinder;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.logging.Logger;

public class StarterRMIImpl implements StarterRMIInterface {
    private final Collector collector;

    public StarterRMIImpl(Collector collector) {
        this.collector = collector;
    }

    @Override
    public void addToNodePool(NodeDto dto) throws RemoteException {
        collector.getInitDtos().add(dto);
        Logger.getGlobal().log(Node.LEVEL, () ->
                String.format("Added new node : %s , NEED %s MORE", dto, collector.getAmountOfNodes() - collector.getInitDtos().size())
        );
        if (collector.getInitDtos().size() == collector.getAmountOfNodes()) {
            System.out.println("sending start");
            collector.sendStartMessage();
            System.out.println("init client");
            collector.initClient();
        }
    }

    @Override
    public void addClient(Address clientAddress) throws RemoteException, NotBoundException {
        Logger.getGlobal().log(Node.LEVEL, () ->
                String.format("Got a client with ip %s and port %s ", clientAddress.getHostname(), clientAddress.getPort())
        );
        collector.setClient(RMIFinder.getClientRMI(clientAddress));
    }

    @Override
    public void startBefore() throws RemoteException {

    }
}

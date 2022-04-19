package cz.cvut.fel.dsv.raftchat.starter;

import cz.cvut.fel.dsv.raftchat.Node;
import cz.cvut.fel.dsv.raftchat.base.Commander;
import cz.cvut.fel.dsv.raftchat.base.Commands;
import cz.cvut.fel.dsv.raftchat.base.dtos.NodeDto;
import cz.cvut.fel.dsv.raftchat.client.ClientRMIInterface;
import lombok.Data;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

@Data
public class Collector {
    private int amountOfNodes;
    private Registry registry;
    private StarterRMIInterface rmiInterface;
    private StarterRMIImpl rmiImpl;
    private int port;
    private List<NodeDto> initDtos = new ArrayList<>();
    private final int slowMoMultiplier;
    private ClientRMIInterface client;

    public Collector(int port, int amountOfNodes, int slowMoMultiplier) {
        this.amountOfNodes = amountOfNodes;
        this.port = port;
        this.slowMoMultiplier = slowMoMultiplier;
    }

    public void configRegistry() throws RemoteException {
        this.registry = LocateRegistry.createRegistry(this.port);
        this.rmiImpl = new StarterRMIImpl(this);
        this.rmiInterface = (StarterRMIInterface) UnicastRemoteObject.exportObject(rmiImpl, port);
        registry.rebind("starter", this.rmiInterface);
    }

    public void sendStartMessage() {
        for (NodeDto dto : initDtos) {
            System.out.println(dto.getId());
            try {
                Node.getCommandsForNodeDto(dto).startWork(initDtos, slowMoMultiplier);
            } catch (RemoteException e) {
                System.out.println("Couldn't start, failed connection with " + dto.getId());
            }
        }
        System.out.println("aaaaaaa");
    }

    public void initClient() throws RemoteException {
        client.initAllNodes(initDtos);
    }
}

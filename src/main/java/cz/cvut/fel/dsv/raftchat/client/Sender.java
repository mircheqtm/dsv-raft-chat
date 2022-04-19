package cz.cvut.fel.dsv.raftchat.client;

import cz.cvut.fel.dsv.raftchat.Node;
import cz.cvut.fel.dsv.raftchat.base.Address;
import cz.cvut.fel.dsv.raftchat.base.dtos.MessageDto;
import cz.cvut.fel.dsv.raftchat.base.dtos.NodeDto;
import cz.cvut.fel.dsv.raftchat.helpers.IpResolver;
import cz.cvut.fel.dsv.raftchat.helpers.RMIFinder;
import cz.cvut.fel.dsv.raftchat.starter.StarterRMIInterface;
import lombok.Data;

import java.net.SocketException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Data
public class Sender {

    private Registry registry;

    private List<NodeDto> nodes = new ArrayList<>();
    private ClientRMIInterface rmiInterface;
    private ClientRMIImpl rmiImpl;
    private StarterRMIInterface starterRMI;
    private Address myAdress;

    public void configRegistry() throws RemoteException {
        this.registry = LocateRegistry.createRegistry(this.myAdress.getPort());
        this.rmiImpl = new ClientRMIImpl(this);
        this.rmiInterface = (ClientRMIInterface) UnicastRemoteObject.exportObject(rmiImpl, this.myAdress.getPort());
        registry.rebind("client", this.rmiInterface);
    }

    public Sender(int port, String starterIP, int starterPort) throws NotBoundException, RemoteException, SocketException {
        this.myAdress = new Address(IpResolver.getIp(), port);
        this.starterRMI = RMIFinder.getStarterRMI(new Address(starterIP, starterPort));
    }

    public void sendMessage(MessageDto messageDto) {
        for (NodeDto dto : nodes) {
            if (dto.getCommands() == null) {
                dto.setCommands(Node.getCommandsForNodeDto(dto));
            }
            try {
                boolean sent = dto.getCommands().findLeaderAndSendMessage(messageDto);
                if (sent) {
                    break;
                }
            } catch (RemoteException e) {
                System.out.println("Couldn't send a message from " + dto.getId());
            }

        }
    }

    public void disconnectNode(String id) {
        System.out.println(nodes);
        for (NodeDto dto : nodes) {
            if (dto.getCommands() == null) {
                dto.setCommands(Node.getCommandsForNodeDto(dto));
            }
            try {
                dto.getCommands().disconnect(id);
                return;
            } catch (RemoteException e) {
                System.out.println("Couldn't send a message from " + dto.getId());
            }

        }
    }

    public void connectNode(String id) {
        for (NodeDto dto : nodes) {
            if (dto.getCommands() == null) {
                dto.setCommands(Node.getCommandsForNodeDto(dto));
            }
            try {
                dto.getCommands().connect(id);
                return;
            } catch (RemoteException e) {
                System.out.println("Couldn't send a message from " + dto.getId());
            }

        }
    }

    public void initAllNodes(List<NodeDto> nodes) {
        this.nodes = nodes;
        Logger.getGlobal().log(Node.LEVEL, () ->
                "Got all nodes, ready for commands"
        );
    }

}

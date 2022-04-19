package cz.cvut.fel.dsv.raftchat.client;

import cz.cvut.fel.dsv.raftchat.base.dtos.NodeDto;

import java.util.List;

public class ClientRMIImpl implements ClientRMIInterface{

    private final Sender sender;

    public ClientRMIImpl(Sender sender) {
        this.sender = sender;
    }

    @Override
    public void initAllNodes(List<NodeDto> nodes) {
        sender.initAllNodes(nodes);
    }

}

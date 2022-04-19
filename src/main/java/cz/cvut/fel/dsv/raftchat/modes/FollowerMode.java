package cz.cvut.fel.dsv.raftchat.modes;

import cz.cvut.fel.dsv.raftchat.Node;
import cz.cvut.fel.dsv.raftchat.helpers.RandomTimeGenerator;
import cz.cvut.fel.dsv.raftchat.model.NodeState;
import lombok.SneakyThrows;

import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;


public class FollowerMode extends AbstractMode {

    private int timeToWait;
    private final Node node;

    public FollowerMode(Node node, CountDownLatch latch) {
        super(latch);
        this.node = node;
        updateTimer();
    }

    @SneakyThrows
    @Override
    public void run() {
        Logger.getGlobal().log(Node.LEVEL, () ->
                "Entering FOLLOWER MODE"
        );
        while (!kill) {
            if(!node.isConnected()){
                Thread.sleep(30);
            }else{
                String lastIdBefore = node.getLastMessage().getId();
                Thread.sleep(timeToWait);
                if (lastIdBefore.equals(node.getLastMessage().getId()) && !kill) {
                    node.setState(NodeState.CANDIDATE);
                    kill = true;
                } else {
                    updateTimer();
                }
            }
        }
        latch.countDown();
    }

    public void updateTimer() {
        timeToWait = RandomTimeGenerator.generateRandomWaitTime();
    }

}


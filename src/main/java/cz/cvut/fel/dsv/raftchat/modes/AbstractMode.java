package cz.cvut.fel.dsv.raftchat.modes;

import java.util.concurrent.CountDownLatch;

public abstract class AbstractMode {

    protected boolean kill;
    protected CountDownLatch latch;


    public AbstractMode(CountDownLatch latch) {
        this.latch = latch;
    }

    public abstract void run();

    public void killMode() {
        this.kill = true;
    }
}

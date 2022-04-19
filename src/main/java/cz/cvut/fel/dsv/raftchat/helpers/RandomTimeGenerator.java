package cz.cvut.fel.dsv.raftchat.helpers;

import java.util.Random;

import static cz.cvut.fel.dsv.raftchat.Node.FOLLOWER_CHECK_APPEND_ENTRY_DELAY;

public class RandomTimeGenerator {
    public static int generateRandomWaitTime(){
        Random rnd = new Random();
        return FOLLOWER_CHECK_APPEND_ENTRY_DELAY + rnd.nextInt(FOLLOWER_CHECK_APPEND_ENTRY_DELAY);
    }
}

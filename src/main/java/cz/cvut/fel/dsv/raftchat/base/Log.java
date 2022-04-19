package cz.cvut.fel.dsv.raftchat.base;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class Log implements Serializable {
    private String from;
    private String to;
    private String message;
    private int term;
    private boolean committed;

    @Override
    public String toString() {
        return "from: " + from + " to " + to + " message: " + message + " term:" + term + " commit: " + committed;
    }
}

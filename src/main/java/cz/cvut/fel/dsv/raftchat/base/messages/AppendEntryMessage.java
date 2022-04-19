package cz.cvut.fel.dsv.raftchat.base.messages;

import cz.cvut.fel.dsv.raftchat.base.Log;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Builder
public class AppendEntryMessage implements Serializable {
    private String id;
    private int term;
    private String leaderId;
    private int prevLogIndex;
    private int prevLogTerm;
    private int commitIndex;
    private Log newLog;
}

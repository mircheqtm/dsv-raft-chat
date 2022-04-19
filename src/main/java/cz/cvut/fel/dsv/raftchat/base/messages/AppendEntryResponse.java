package cz.cvut.fel.dsv.raftchat.base.messages;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Builder
public class AppendEntryResponse implements Serializable {
    private int term;
    private boolean success;
    private int matchIndex;
}

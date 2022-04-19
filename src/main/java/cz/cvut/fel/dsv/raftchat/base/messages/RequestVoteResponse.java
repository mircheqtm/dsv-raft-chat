package cz.cvut.fel.dsv.raftchat.base.messages;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
public class RequestVoteResponse implements Serializable {
    private int term;
    private boolean approved;
}

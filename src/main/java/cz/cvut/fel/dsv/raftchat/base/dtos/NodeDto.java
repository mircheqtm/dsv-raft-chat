package cz.cvut.fel.dsv.raftchat.base.dtos;

import cz.cvut.fel.dsv.raftchat.base.Address;
import cz.cvut.fel.dsv.raftchat.base.Commands;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
public class NodeDto implements Serializable {
    private String id;
    private Address address;
    private Commands commands;
    private int nextIndex = 1;
    private int matchIndex;
}

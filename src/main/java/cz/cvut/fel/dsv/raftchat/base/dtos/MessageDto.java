package cz.cvut.fel.dsv.raftchat.base.dtos;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class MessageDto implements Serializable {
    private String text;
}

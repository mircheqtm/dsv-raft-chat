package cz.cvut.fel.dsv.raftchat.base;

import lombok.Getter;

import java.io.Serializable;

@Getter
public class Address implements Comparable<Address>, Serializable {

    private final String hostname;
    private final Integer port;

    public Address (String hostname, Integer port) {
        this.hostname = hostname;
        this.port = port;
    }

    @Override
    public String toString() {
        return("Addr[host:'"+hostname+"', port:'"+port+"']");
    }

    @Override
    public int compareTo(Address address) {
        int retval = 0;
        if ((retval = hostname.compareTo(address.hostname)) == 0 ) {
            if ((retval = port.compareTo(address.port)) == 0 ) {
                return 0;
            }
            else
                return retval;
        }
        else
            return retval;
    }
}
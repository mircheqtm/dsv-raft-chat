package cz.cvut.fel.dsv.raftchat.starter;

import cz.cvut.fel.dsv.raftchat.helpers.IpResolver;

import java.net.SocketException;
import java.rmi.RemoteException;
import java.util.Objects;

import static java.lang.System.exit;

public class Starter {

    public static final int PORT = 4529;

    public static void main(String[] args) throws RemoteException, SocketException {
        if (args.length != 2) {
            System.out.println("wrong amount of arguments. Need to be two - number of all nodes and slowMo multiplier");
            exit(1);
        } else {
            System.setProperty("java.rmi.server.hostname", Objects.requireNonNull(IpResolver.getIp()));
            int slowMo = Integer.parseInt(args[1]);
            System.out.println("STARTER IP: " + IpResolver.getIp());
            System.out.println("STARTER PORT: " + PORT);
            System.out.println("SLOW MO: " + slowMo);
            Collector collector = new Collector(PORT, Integer.parseInt(args[0]), slowMo);
            collector.configRegistry();
        }
    }
}

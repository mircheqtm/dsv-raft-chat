package cz.cvut.fel.dsv.raftchat.client;

import cz.cvut.fel.dsv.raftchat.base.dtos.MessageDto;
import cz.cvut.fel.dsv.raftchat.helpers.IpResolver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.SocketException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Objects;

public class Client {

    public static void main(String[] args) throws RemoteException, NotBoundException, SocketException {
        if (args.length != 3) {
            throw new IllegalArgumentException("Wrong amount of args, there must be 3: <port of client> <ip of starter> <port of starter>");
        }
        System.setProperty("java.rmi.server.hostname", Objects.requireNonNull(IpResolver.getIp()));

        Sender sender = new Sender(Integer.parseInt(args[0]), args[1], Integer.parseInt(args[2]));
        sender.configRegistry();
        sender.getStarterRMI().addClient(sender.getMyAdress());

        boolean reading = true;
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        PrintStream err = System.err;

        String commandline = "";
        while (reading) {
            System.out.print("\ncmd > ");
            try {
                commandline = reader.readLine();
                parseCommands(commandline, sender);
            } catch (IOException e) {
                err.println("ConsoleHandler - error in rading console input.");
                e.printStackTrace();
                reading = false;
            }
        }

    }

    public static void parseCommands(String commandLine, Sender sender) {
        String[] split = commandLine.split(" ");
        switch (split[0]) {
            case "send-message":
                MessageDto dto = MessageDto.builder()
                        .text(split[1])
                        .build();
                sender.sendMessage(dto);
                break;
            case "disconnect":
                sender.disconnectNode(split[1]);
                break;
            case "connect":
                sender.connectNode(split[1]);
                break;
            case "?":
                System.out.println("There are 3 commands:\nsend-message <text>" +
                        "\ndisconnect <Node id>" +
                        "\nconnect <Node id>");
                break;
            default:
                System.out.println("Unknown command, type ? for help");
                break;
        }
    }
}

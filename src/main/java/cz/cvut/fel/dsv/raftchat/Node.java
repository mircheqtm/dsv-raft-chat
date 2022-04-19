package cz.cvut.fel.dsv.raftchat;

import cz.cvut.fel.dsv.raftchat.base.Address;
import cz.cvut.fel.dsv.raftchat.base.Commander;
import cz.cvut.fel.dsv.raftchat.base.Commands;
import cz.cvut.fel.dsv.raftchat.base.Log;
import cz.cvut.fel.dsv.raftchat.base.dtos.MessageDto;
import cz.cvut.fel.dsv.raftchat.base.dtos.NodeDto;
import cz.cvut.fel.dsv.raftchat.base.messages.AppendEntryMessage;
import cz.cvut.fel.dsv.raftchat.helpers.IpResolver;
import cz.cvut.fel.dsv.raftchat.helpers.RMIFinder;
import cz.cvut.fel.dsv.raftchat.model.NodeState;
import cz.cvut.fel.dsv.raftchat.modes.CandidateMode;
import cz.cvut.fel.dsv.raftchat.modes.FollowerMode;
import cz.cvut.fel.dsv.raftchat.modes.LeaderMode;
import cz.cvut.fel.dsv.raftchat.starter.StarterRMIInterface;
import lombok.Getter;
import lombok.Setter;

import java.net.SocketException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.System.exit;

@Getter
@Setter
public class Node implements Runnable {
    public static int LEADER_SEND_APPEND_ENTRY_DELAY = 50;
    public static int FOLLOWER_CHECK_APPEND_ENTRY_DELAY = 150;
    public static int CANDIDATE_NO_ANSWER_DELAY = 150;

    public static Logger LOGGER = Logger.getLogger("InfoLogging");
    public static final Level LEVEL = Level.INFO;

    private boolean connected = true;

    // This Node
    public static Node thisNode = null;
    public static final String COMMANDS_NAME = "commander";

    private Address address;
    private String leaderId;
    private String id;
    private Map<String, NodeDto> allNodes;
    protected CountDownLatch startLatch;

    private Registry registry;
    private FollowerMode timer;
    private int majority;

    private AppendEntryMessage lastMessage;
    private long lastIdSent;

    private int committedIndex;
    private NodeState state;
    private Integer term;
    private List<Log> logs;

    private Commands commander;
    private Commander commanderImpl = null;
    private StarterRMIInterface starter;

    private CandidateMode candidateMode;
    private FollowerMode followerMode;
    private LeaderMode leaderMode;


    private List<MessageDto> newMessagesPool;

    public static void main(String[] args) throws NotBoundException, SocketException {
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "%1$tl:%1$tM:%1$tS.%1$tL %4$s %2$s %5$s%6$s%n");
        System.setProperty("java.rmi.server.hostname", Objects.requireNonNull(IpResolver.getIp()));
        try {
            thisNode = initNode(args);
            thisNode.sendMyInfoToStarter();
            thisNode.getStartLatch().await();
            thisNode.run();
        } catch (RemoteException | InterruptedException e) {
            Logger.getGlobal().log(Node.LEVEL, () ->
                    "Got an error during connection to starter, check if starter is running and initial (ip port) data"
            );
            exit(1);
        }

    }

    public void initDtos(List<NodeDto> initDtos) {
        Map<String, NodeDto> finalDtos = new HashMap<>();

        for (NodeDto dto : initDtos) {
            NodeDto newDto = NodeDto.builder()
                    .address(dto.getAddress())
                    .id(dto.getId())
                    .nextIndex(1)
                    .matchIndex(0)
                    .build();
            finalDtos.put(newDto.getId(), newDto);
        }
        thisNode.majority = (int) Math.ceil((float) finalDtos.size() / 2);
        thisNode.setAllNodes(finalDtos);
    }

//    private static Map<String, NodeDto> initAllNodes() {
//        Map<String, NodeDto> dtos = new HashMap<>();
//        List<Address> addresses = new ArrayList<>();
//        addresses.add(new Address("192.168.56.1", 6765));
//        addresses.add(new Address("192.168.56.1", 6766));
//        addresses.add(new Address("192.168.56.1", 6767));
//        addresses.add(new Address("192.168.56.1", 6768));
//        addresses.add(new Address("192.168.56.1", 6769));
//        for (int i = 0; i < addresses.size(); i++) {
//            NodeDto newDto = NodeDto.builder()
//                    .address(addresses.get(i))
//                    .id("Node" + i)
//                    .nextIndex(1)
//                    .matchIndex(0)
//                    .build();
//            dtos.put(newDto.getId(), newDto);
//        }
//        return dtos;
//    }

    private static Node initNode(String[] args) throws RemoteException, NotBoundException, SocketException {
        if (args.length != 4) {
            throw new IllegalArgumentException("Illegal amount of parameters");
        }

        Node node;

        String id = args[0];
        String myPort = args[1];

        String starterId = args[2];
        String starterPort = args[3];
        Address starterAddress = new Address(starterId, Integer.parseInt(starterPort));

        String host = IpResolver.getIp();
        Integer port = Integer.parseInt(myPort);


        node = new Node(host, port, id);
        node.timer = new FollowerMode(node, null);
        node.majority = node.allNodes.size() / 2;
        node.setState(NodeState.FOLLOWER);
        node.configRegistry(port);
        node.setNewMessagesPool(new ArrayList<>());
        node.setLogs(new ArrayList<>());
        AppendEntryMessage initMessage = AppendEntryMessage
                .builder()
                .id("initId")
                .build();
        Log fakeLog = Log.builder()
                .message("fakeLog")
                .term(0)
                .committed(true)
                .build();
        node.getLogs().add(fakeLog);
        node.setLastMessage(initMessage);
        node.setStarter(RMIFinder.getStarterRMI(starterAddress));
        node.setStartLatch(new CountDownLatch(1));
        return node;
    }

    private void sendMyInfoToStarter() throws RemoteException {
        NodeDto myDto = NodeDto.builder()
                .id(this.getId())
                .address(this.address)
                .build();
        this.starter.addToNodePool(myDto);
    }

    private Node(String host, Integer port, String id) {
        this.term = 0;
        this.address = new Address(host, port);
        this.id = id;
        this.allNodes = new HashMap<>();
    }

    public static NodeDto findNodeById(String id) {
        for (Map.Entry<String, NodeDto> nodeDto : thisNode.allNodes.entrySet()) {
            if (nodeDto.getValue().getId().equals(id))
                return nodeDto.getValue();
        }
        return null;
    }

    private void configRegistry(Integer port) throws RemoteException {
        this.registry = LocateRegistry.createRegistry(port);
        commanderImpl = new Commander(this);
        this.commander = (Commands) UnicastRemoteObject.exportObject(commanderImpl, port);
        registry.rebind(COMMANDS_NAME, this.commander);
    }

    @Override
    public void run() {
        checkState();
    }

    public void checkState() {
        if (this.state == NodeState.LEADER) {
            leaderMode();
        } else if (this.state == NodeState.CANDIDATE) {
            candidateMode();
        } else if (this.state == NodeState.FOLLOWER) {
            followerMode();
        }
    }

    private void followerMode() {
        CountDownLatch latch = new CountDownLatch(1);
        this.followerMode = new FollowerMode(this, latch);
        if (candidateMode != null) {
            this.candidateMode.killMode();
        }
        if (leaderMode != null) {
            this.leaderMode.killMode();
        }
        followerMode.run();
        try {
            latch.await();
        } catch (InterruptedException e) {
            System.out.println("latch failed");
        }

        checkState();
    }

    private void candidateMode() {
        CountDownLatch latch = new CountDownLatch(1);
        this.candidateMode = new CandidateMode(this, latch);
        if (followerMode != null) {
            this.followerMode.killMode();
        }
        if (leaderMode != null) {
            this.leaderMode.killMode();
        }
        candidateMode.run();
        try {
            latch.await();
        } catch (InterruptedException e) {
            System.out.println("latch failed");
        }
        checkState();
    }

    private void leaderMode() {
        CountDownLatch latch = new CountDownLatch(1);
        this.leaderMode = new LeaderMode(this, latch);
        if (followerMode != null) {
            this.followerMode.killMode();
        }
        if (candidateMode != null) {
            this.candidateMode.killMode();
        }
        leaderMode.run();
        try {
            latch.await();
        } catch (InterruptedException e) {
            System.out.println("latch failed");
        }
        checkState();
    }

    public void commitLog(int index) {
        this.getLogs().get(index).setCommitted(true);
    }

    public void addNewLogOutOfMessageDto(MessageDto messageDto) {
        Log newLog = Log.builder()
                .message(messageDto.getText())
                .term(this.getTerm())
                .committed(false)
                .build();
        this.getLogs().add(newLog);
    }

    public int processAppendEntry(AppendEntryMessage message) {
        //Collision detection
        boolean detected = true;
        while (detected) {
            if (message.getPrevLogIndex() != this.logs.size() - 1 && message.getPrevLogTerm() != this.logs.get(this.logs.size() - 1).getTerm()) {
                this.logs.remove(this.logs.size() - 1);
            } else {
                detected = false;
            }
        }
        logs.add(message.getNewLog());
        commitLog(message.getCommitIndex());
        return logs.size() - 1;

    }

    public synchronized void incrementTerm() {
        this.term++;
    }

    public static Commands getCommandsForNodeDto(NodeDto target) {
        Logger.getGlobal().log(Node.LEVEL, () ->
                String.format("Trying to get commands from %s", target.getId())
        );
        Commands commands = null;
        try {
            commands = RMIFinder.getNodeCommands(target.getAddress());
        } catch (RemoteException | NotBoundException e) {
            Logger.getGlobal().log(Node.LEVEL, () ->
                    String.format("CONNECTION REFUSED - couldn't get commands from %s", target.getId())
            );
        }
        return commands;
    }

    public void incrementLastSendId() {
        this.lastIdSent++;
    }

    public void disconnect() {
        this.connected = false;
        Logger.getGlobal().log(Node.LEVEL, () ->
                "DISCONNECTED"
        );
    }

    public void connect() {
        this.connected = true;
        Logger.getGlobal().log(Node.LEVEL, () ->
                "CONNECTED"
        );
    }

    public void setSlowMo(int i) {
        LEADER_SEND_APPEND_ENTRY_DELAY *= i;
        FOLLOWER_CHECK_APPEND_ENTRY_DELAY *= i;
        CANDIDATE_NO_ANSWER_DELAY *= i;
    }
}

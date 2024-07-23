package org.example.Server;

import org.example.Parser;
import org.w3c.dom.Document;

import org.xml.sax.InputSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Server {
    private Selector selector;
    private ServerSocketChannel serverSocket;
    private InetSocketAddress address;
    private ByteBuffer buffer;
    private ArrayList<User> members;
    private int maxInd;
    ArrayList<String> lastMessages;
    private final int capacityOfHistory=5;
    private Parser parser;
    public static final Logger logger = LogManager.getLogger(Server.class);

    Server(String ipAddr, int port) throws IOException {
    selector = Selector.open();
    serverSocket = ServerSocketChannel.open();
    address = new InetSocketAddress(ipAddr, port);
    serverSocket.bind(address);
    serverSocket.configureBlocking(false);
    serverSocket.register(selector, SelectionKey.OP_ACCEPT);
    buffer = ByteBuffer.allocate(256);
    members= new ArrayList<>();
    lastMessages=new ArrayList<>();
    maxInd=0;
    parser = new Parser();
    }

    public void run() throws IOException, ServerException {
        while (true) {
            selector.select();
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iter = selectedKeys.iterator();
            while (iter.hasNext()) {

                SelectionKey key = iter.next();

                if (key.isAcceptable()) {
                    handleAccept(key);
                }

                if (key.isReadable()) {
                    try {
                        handleRead(key);
                    } catch (NotFinishReadingException e){
                        continue;
                    }
                }
                iter.remove();
            }
        }
    }

    private void handleAccept(SelectionKey key) throws IOException {
        SocketChannel client = serverSocket.accept();
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ, new MessageState());
        members.add(new User(-1, "", client));
    }

    private void handleRead(SelectionKey key) throws IOException, ServerException, NotFinishReadingException {
        SocketChannel client = (SocketChannel) key.channel();
        String message;
        User currentUser=getUser(client);

        try {
            message = recvXML(key, client);
        } catch (ReadingException e){
            key.cancel();
            removeUser(currentUser);
            return;
        }

        Document document = convertStringToXml(message);
        String commandName=getCommandName(document);
        try {
            switch (commandName) {
                case "login":
                    login(document, currentUser);
                    break;
                case "list":
                    getMembers(document, currentUser);
                    break;
                case "message":
                    message(document, currentUser);
                    break;
                case "logout":
                    logout(document, currentUser, key);
                    break;
                default:
                    throw new ServerException();
            }
        } catch (InvalidUserException e){
            key.cancel();
            removeUser(currentUser);
        }
    }

    private void removeUser(User user) throws IOException {
        user.socket.close();
        String logoutMessage = getLogoutMessage(user);
        if(user.id!=-1) {
            broadCast(logoutMessage, user);
        }
        members.remove(user);
        logger.info("member : "+user.nickname+" successfully logout");
    }

    private void login(Document document, User currentUser) throws ServerException, InvalidUserException {
        String nickname=null;
        ArrayList<String> tmp = parser.getValueByName(document, "name", null);
        if(tmp==null){
            throw new ServerException();
        } else {
            nickname = tmp.get(0);
        }

        if(!isUniqueNickname(nickname)){
            sendXML(currentUser, getErrorMessage("nickname"));
            return;
        }

        currentUser.id=maxInd;
        currentUser.nickname=nickname;

        for (String lastMessage : lastMessages) {
            sendXML(currentUser, lastMessage);
        }

        String loginMessage=getLoginMessage(currentUser);
        sendXML(currentUser, getLoginSuccessMessage(currentUser));
        broadCast(loginMessage, currentUser);
        maxInd++;
        logger.info("member : "+nickname+" successfully login");
    }

    private void getMembers(Document document, User currentUser) throws InvalidUserException {
        String allMembers=getReqMembersSuccessMessage();
        sendXML(currentUser, allMembers);
    }

    private void message(Document document, User currentUser) throws ServerException, InvalidUserException {
        String message;
        ArrayList<String> tmp = parser.getValueByName(document, "message", null);
        if(tmp==null){
            throw new ServerException();
        } else {
            message = tmp.get(0);
        }

        String XMLMessage=getMessageToClientMessage(message, currentUser);

        sendXML(currentUser, getDefaultSuccessMessage());
        broadCast(XMLMessage, currentUser);

    }

    private void logout(Document document, User currentUser, SelectionKey key) throws InvalidUserException {
        String logoutMessage=getLogoutMessage(currentUser);
        sendXML(currentUser, getDefaultSuccessMessage());
        broadCast(logoutMessage, currentUser);
        members.remove(currentUser);
        key.cancel();
        logger.info("member : "+currentUser.nickname+" successfully logout");
    }

    private void broadCast(String message, User sender) {
        appendToHistory(message);
        for (User member : members) {
            if(member.id!=-1 && !sender.equals(member)) {
                try {
                    sendXML(member, message);
                } catch (InvalidUserException ignored){
                }

            }
        }
        buffer.clear();
    }

    private void sendXML(User addressee, String message) throws InvalidUserException {
        byte[] byteMessage=message.getBytes();
        ByteBuffer length = ByteBuffer.allocate(4).putInt(byteMessage.length);

        ByteBuffer buffer = ByteBuffer.wrap(byteMessage);

        length.rewind();
        buffer.rewind();
        while (length.hasRemaining()) {
            try {
                addressee.socket.write(length);
            } catch (IOException e){
                throw new InvalidUserException();
            }

        }
        while (buffer.hasRemaining()) {
            try {
                addressee.socket.write(buffer);
            } catch (IOException e){
                throw new InvalidUserException();
            }
        }
    }


    private  void appendToHistory(String message){
        if(lastMessages.size()<capacityOfHistory){
            lastMessages.add(message);
            return;
        }
        for(int i = 0; i < capacityOfHistory-1; i++){
            lastMessages.set(i, lastMessages.get(i+1));
        }
        lastMessages.set(capacityOfHistory-1, message);
    }

    private String recvXML(SelectionKey key, SocketChannel client) throws IOException, NotFinishReadingException, ReadingException {
        MessageState state = (MessageState) key.attachment();
        int readRetVal=client.read(state.buffer);
        if(readRetVal==-1) {
            throw new ReadingException();
        }
        state.currentPointer+=readRetVal;

        int lenOfContent;

        if(state.currentPointer == state.bufferSize) {
            if(!state.isGetContentLen) {
                state.buffer.rewind();
                lenOfContent = state.buffer.getInt();
                state.setBuffer(lenOfContent);
                throw new NotFinishReadingException();
            } else {
                byte[] byteMessage= new byte[state.bufferSize];

                for(int i=0;i<state.bufferSize;i++){
                    byteMessage[i]=state.buffer.get(i);
                }
                state.resetBuffer();
                return new String(byteMessage).trim();
            }
        } else{
            throw new NotFinishReadingException();
        }
    }

    private User getUser(SocketChannel channel){
        for (User member : members) {
            if(member.socket.equals(channel)){
                return member;
            }
        }
        return null;
    }

    private String getErrorMessage(String reason){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<error> <message>").append(reason).append("</message> </error>");
        return stringBuilder.toString();
    }

    private String getLoginSuccessMessage(User sender){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<success> <session>").append(sender.id).append("</session></success>");
        return stringBuilder.toString();
    }

    private String getReqMembersSuccessMessage(){
        StringBuilder tmp = new StringBuilder("<success><listusers>");
        for (User member : members) {
            tmp.append("<user ><name >").append(member.nickname).append("</name ><type>").append("default").append("</type ></user >");
        }
	    tmp.append("</listusers></success>");
        return tmp.toString();
    }

    private String getDefaultSuccessMessage(){
        return "<success></success>";
    }

    private String getMessageToClientMessage(String message, User sender){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<event name=\"message\"> <message>").append(message).append("</message> <name>").append(sender.nickname).append("</name></event>");
        return stringBuilder.toString();
    }

    private String getLoginMessage(User sender){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<event name=\"userlogin\"><name>").append(sender.nickname).append("</name></event >");
        return stringBuilder.toString();
    }

    private String getLogoutMessage(User sender){
        if(sender==null){
            System.out.println("nol");
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<event name=\"userlogout\"><name>").append(sender.nickname).append("</name></event >");
        return stringBuilder.toString();
    }

    private String getCommandName(Document document){
        return parser.getAttributeOfNode(document, "command", "name");
    }

    private String getHistory(){
        StringBuilder history= new StringBuilder();
        for(int i=0;i<capacityOfHistory;i++){
            history.append(lastMessages.get(i)).append("\n");
        }
        return history.toString();
    }

    private Document convertStringToXml(String xmlString) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
            return builder.parse(new InputSource(new StringReader(xmlString)));
        } catch (Exception e) {
            throw new RuntimeException();
        }

    }

    private boolean isUniqueNickname(String nickname){
        for(User member:members){
            if(member.id!=-1){
                if(nickname.equals(member.nickname) || nickname.equals("You")){
                    return false;
                }
            }
        }
        return true;
    }

}

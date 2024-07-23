package org.example.Client;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.example.Parser;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class Client {
    public InetSocketAddress address;
    public SocketChannel client;
    public ByteBuffer buffer;
    public String nickname;
    public int id;
    public String currentMessage;
    private Parser parser;
    private Thread listenerThread;
    public boolean isLogin;
    Client(String addr, int port, String nickname) throws IOException, ClientException {
        isLogin=false;
        currentMessage="";
        this.nickname=nickname;
        parser = new Parser();
        id=-1;
        address = new InetSocketAddress(addr, port);
        buffer = ByteBuffer.allocate(256);
        try {
            client = SocketChannel.open(address);
        } catch (SocketException e){
            System.out.println("something wrong");
            throw new ClientException();
        }
        client.configureBlocking(true);
        String XMLMessage=getLoginMessage();
        sendXML(XMLMessage);
    }

    public void setListenerThread(Thread listenerThread){
        this.listenerThread=listenerThread;
    }

    public void terminate(){
        listenerThread.interrupt();
        try {
            client.close();
        } catch (IOException e){
            e.printStackTrace();
        }
        exit();
    }

    public void sendToServer(String message) throws IOException, ClientException {
        String XMLMessage;
        if(id==-1){
            XMLMessage=getLoginMessage();
        } else if(message.equals("@getMembers")){
            XMLMessage=getMembersMessage(id);
        } else{
            XMLMessage=getDefaultMessage(message, id);
        }

        sendXML(XMLMessage);
    }

    public void exit(){
        System.exit(0);
    }

    private void sendXML(String message) throws IOException {
        byte[] byteMessage=message.getBytes();
        ByteBuffer length = ByteBuffer.allocate(4).putInt(byteMessage.length);

        ByteBuffer content = ByteBuffer.wrap(byteMessage);
        length.rewind();
        content.rewind();

        buffer = ByteBuffer.allocate(length.limit() + content.limit()).put(length).put(content);

        buffer.rewind();
        while(buffer.hasRemaining()) {
            client.write(buffer);
        }
        buffer.rewind();
        buffer.clear();
    }

    private String getLoginMessage(){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<command name=\"login\"><name>").append(nickname).append("</name><type>default</type></command>");
        return stringBuilder.toString();
    }

    private String getMembersMessage(int id){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<command name=\"list\"><session>").append(id).append("</session></command>");
        return stringBuilder.toString();
    }

    private String getDefaultMessage(String message, int id){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<command name=\"message\"> <message>").append(message).append("</message> <session>").append(id).append("</session></command>");
        return stringBuilder.toString();
    }

    public String parseMessage(String XMLMessage) throws ClientException, IOException {
        Document document = convertStringToXml(XMLMessage);
        String elementName = parser.getRootName(document);

        if(elementName.equals("event")){
            return handleEvent(document);
        } else if(elementName.equals("success")){
            return handleSuccess(document);
        } else{
            return handleError(document);
        }
    }

    private Document convertStringToXml(String xmlString) throws ClientException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xmlString)));
            return document;
        } catch (Exception e) {
            System.out.println("wrong xml message");
            throw new ClientException();
        }
    }

    private String getEventName(Document document){
        return parser.getAttributeOfRoot(document,"name");
    }

    private String handleEvent(Document document) throws ClientException {
        String eventName=getEventName(document);
        String nickname;

        ArrayList<String> tmp = parser.getValueByName(document, "name", null);
        if(tmp==null){
            throw new ClientException();
        } else {
            nickname = tmp.get(0);
        }

        if(eventName.equals("userlogin")){
            return "\t\tuser " + nickname + " join to chat\n";
        } else if(eventName.equals("userlogout")){
            return "\t\tuser "+nickname+" leave chat\n";
        } else{
            tmp = parser.getValueByName(document, "message", null);
            if(tmp==null){
                throw new ClientException();
            } else {
                String message = tmp.get(0);
                return nickname + " : " + message + "\n";
            }
        }
    }

    private String handleSuccess(Document document) throws ClientException {
        String childName = parser.getRootChildName(document);

        if(childName==null){
            return "You : " + currentMessage + "\n";
        } else if(childName.equals("session")){
            isLogin=true;
            this.id= Integer.parseInt(parser.getValueByName(document, childName, null).get(0));
            return "\t\tYou join to chat\n";
        } else{
            StringBuilder members = new StringBuilder("members:");
            ArrayList<String> tmp= parser.getValueByName(document, "user", "name");
            if(tmp==null){
                throw new ClientException();
            } else {
                for (String s : tmp) {
                    members.append("\t\t").append(s).append("\n");
                }
                return members.toString();
            }
        }
    }

    public String getSomething(String something){
        return JOptionPane.showInputDialog("enter ur "+something);
    }

    private String handleError(Document document) throws ClientException, IOException {
        ArrayList<String> tmp = parser.getValueByName(document, "message", null);
        if(tmp==null){
            throw new ClientException();
        } else {
            String reason = tmp.get(0);
            if(reason.equals("nickname")){
                nickname = getSomething("error, this nickname is taken, try again");
                String XMLMessage=getLoginMessage();
                sendXML(XMLMessage);
                return "\t\tu try again, wait\n";
            }
        }
        return "\t\terror";
    }
}

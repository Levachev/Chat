package org.example.Server;

import java.nio.channels.SocketChannel;

public class User {
    public int id;
    public String nickname;
    public SocketChannel socket;
    User(int id, String nickname, SocketChannel socket){
        this.id=id;
        this.nickname=nickname;
        this.socket=socket;
    }
}

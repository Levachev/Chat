package org.example.Server;

import java.nio.ByteBuffer;

public class MessageState {
    public boolean isGetContentLen;
    public int bufferSize;
    public ByteBuffer buffer;
    public int currentPointer;
    public MessageState(){
        isGetContentLen=false;
        bufferSize=4;
        buffer = ByteBuffer.allocate(bufferSize);
        currentPointer=0;
    }

    public void setBuffer(int newBufferSize){
        isGetContentLen=true;
        bufferSize=newBufferSize;
        buffer = ByteBuffer.allocate(bufferSize);
        currentPointer=0;
    }

    public void resetBuffer(){
        isGetContentLen=false;
        bufferSize=4;
        buffer = ByteBuffer.allocate(bufferSize);
        currentPointer=0;
    }
}

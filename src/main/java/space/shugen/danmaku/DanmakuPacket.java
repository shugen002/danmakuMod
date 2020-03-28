package space.shugen.danmaku;

import java.nio.ByteBuffer;

import static space.shugen.danmaku.GoIMConsts.*;

public class DanmakuPacket {
    public int packetLength;
    public short headerLength;
    public short version;
    public int sequence;
    public int operation;
    public byte[] data;

    public DanmakuPacket(ByteBuffer buffer) {
        packetLength=buffer.getInt(WS_PACKAGE_OFFSET);
        headerLength=buffer.getShort(WS_HEADER_OFFSET);
        version = buffer.getShort(WS_VERSION_OFFSET);
        operation = buffer.getInt(WS_OPERATION_OFFSET);
        sequence = buffer.getInt(WS_SEQUENCE_OFFSET);
        data = new byte[packetLength-headerLength];
        if(packetLength-headerLength > 0){
            buffer.position(headerLength);
            buffer.get(data,0,packetLength-headerLength);
        }

    }
}

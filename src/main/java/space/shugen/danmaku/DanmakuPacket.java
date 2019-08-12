package space.shugen.danmaku;

import java.nio.ByteBuffer;

public class DanmakuPacket {
    private static final int WS_PACKAGE_OFFSET = 0;
    private static final int WS_HEADER_OFFSET = 4;
    private static final int WS_VERSION_OFFSET = 6;
    private static final int WS_OPERATION_OFFSET = 8;
    private static final int WS_SEQUENCE_OFFSET = 12;
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

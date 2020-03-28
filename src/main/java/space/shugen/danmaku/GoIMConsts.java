package space.shugen.danmaku;

public class GoIMConsts {
    public static final short WS_HEADER_LENGTH = 16;
    public static final int WS_PACKAGE_OFFSET = 0;
    public static final int WS_HEADER_OFFSET = 4;
    public static final int WS_VERSION_OFFSET = 6;
    public static final int WS_OPERATION_OFFSET = 8;
    public static final int WS_SEQUENCE_OFFSET = 12;
    public static final short WS_VERSION = 1;
    public static final int WS_OP_HEARTBEAT = 2;
    public static final int WS_OP_HEARTBEAT_REPLY = 3;
    public static final int WS_OP_MESSAGE = 5;
    public static final int WS_OP_USER_AUTHENTICATION = 7;
    public static final int WS_OP_CONNECT_SUCCESS = 8;
}

package space.shugen.danmaku;

import com.google.gson.*;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.*;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static space.shugen.danmaku.GoIMConsts.*;

public class Main implements ModInitializer {
    public static String MOD_ID = "danmaku_mod";
    public static Main self;
    private WebSocketClient webSocketClient;
    public Logger logger;
    private Timer timer;
    private CloseableHttpClient httpClient;
    private DanmakuConfig config;
    private int currentRoomId = 0;
    private TimerTask lastTasks =null;
    public static final Gson GSON = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).setPrettyPrinting().create();

    @Override
    public void onInitialize() {
        self = this;
        logger = LogManager.getLogger("Danmaku Mod");
        logger.info("Danmaku Mod Loaded");
        ConfigManager.initializeConfig();
        config = ConfigManager.getConfig();
        timer = new Timer();
        List<Header> headers = new ArrayList();
        headers.add(new BasicHeader("Content-Type", "application/x-www-form-urlencoded"));
        httpClient = HttpClientBuilder.create()
                .setUserAgent("Minecraft " + MinecraftClient.getInstance().getGameVersion())
                .setDefaultHeaders(headers)
                .build();
    }

    public void Connect(int roomId) {
        disconnect();
        currentRoomId = roomId;
        webSocketClient = new WebSocketClient(URI.create("wss://broadcastlv.chat.bilibili.com:2245/sub")) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                logger.info("WebSocket Connected . Authing ..");

                MinecraftClient.getInstance().player.sendMessage(new LiteralText("WebSocket Connected . Authing .."));
                String token = "{\"uid\":0,\"roomid\":" + roomId + "}";
                this.send(encode(WS_OP_USER_AUTHENTICATION, token.getBytes()).array());
            }

            @Override
            public void onMessage(String message) {
                MinecraftClient.getInstance().player.sendMessage(new LiteralText("未知消息: " + message));
                return;
            }

            @Override
            public void onMessage(ByteBuffer bytes) {
                ArrayList<DanmakuPacket> packets = decode(bytes);
                for (int i = 0; i < packets.size(); i++) {
                    switch (packets.get(i).operation) {
                        case WS_OP_CONNECT_SUCCESS:
                            getPlayer().sendMessage(new LiteralText("Auth Success"));
                            timer.schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    lastTasks=this;
                                    if (webSocketClient != null && webSocketClient.isOpen()) {
                                        webSocketClient.send(encode(WS_OP_HEARTBEAT, "[object Object]".getBytes()).array());
                                    } else {
                                        this.cancel();
                                    }
                                }
                            }, 0, 30 * 1000);
                            break;
                        case WS_OP_HEARTBEAT_REPLY:
                            break;
                        case WS_OP_MESSAGE:
                            Gson gson = new Gson();
                            JsonParser parser = new JsonParser();
                            JsonObject jsonObject = parser.parse(new String(packets.get(i).data, StandardCharsets.UTF_8)).getAsJsonObject();
                            String cmd = jsonObject.getAsJsonObject().get("cmd").getAsString();
                            switch (cmd) {
                                case "DANMU_MSG":
                                    JsonArray data = jsonObject.getAsJsonArray("info");

                                    JsonArray texts = new JsonArray();
                                    texts.add("");
                                    if (data.get(3).getAsJsonArray().size() >= 4) {
                                        JsonArray badgeData = data.get(3).getAsJsonArray();
                                        JsonObject badge = simpleText("[" + badgeData.get(1).getAsString() + " " + badgeData.get(0).getAsInt() + "]");
                                        badge.add("hoverEvent", parser.parse("{\"action\":\"show_text\",\"value\":{}}").getAsJsonObject());
                                        badge.get("hoverEvent").getAsJsonObject().get("value").getAsJsonObject().addProperty("text", "主播:   " + badgeData.get(2).getAsString() + "\n房间号: " + badgeData.get(3).getAsInt());
                                        texts.add(badge);
                                    }
                                    if (data.get(4).getAsJsonArray().size() > 0) {
                                        texts.add(simpleText("[UL " + data.get(4).getAsJsonArray().get(0).getAsInt() + "]"));
                                    }
                                    JsonObject user = simpleText(data.get(2).getAsJsonArray().get(1).getAsString());
                                    user.add("hoverEvent", parser.parse("{\"action\":\"show_text\",\"value\":{}}").getAsJsonObject());
                                    user.get("hoverEvent").getAsJsonObject().get("value").getAsJsonObject().addProperty("text", "UID: " + data.get(2).getAsJsonArray().get(0).getAsInt());
                                    texts.add(user);
                                    texts.add(simpleText(" : "));
                                    texts.add(simpleText(data.get(1).getAsString()));
                                    getPlayer().sendMessage(Text.Serializer.fromJson(texts));
                                    break;
                                case "SEND_GIFT":

                                default:
                                    break;
                            }

                            break;
                        default:
                            logger.info("Unknown operation ID " + packets.get(i).operation);
                            break;
                    }
                }

            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                if(lastTasks!= null ){
                    lastTasks.cancel();
                    lastTasks=null;
                }
                getPlayer().sendMessage(new LiteralText("WebSocket Close with Code " + code + " . Reason : " + reason));
            }

            @Override
            public void onError(Exception ex) {
                getPlayer().sendMessage(new LiteralText("Error : " + ex.getLocalizedMessage()));
                ex.printStackTrace();
            }
        };
        webSocketClient.connect();
    }

    public void disconnect() {
        if(lastTasks!= null ){
            lastTasks.cancel();
            lastTasks=null;
        }
        if (webSocketClient != null) {
            webSocketClient.close(1000, "disconnect");
            getPlayer().sendMessage(new LiteralText("Websocket Close."));
        }
        ;

    }

    public ByteBuffer encode(int operation, byte[] data) {
        int packetLength = WS_HEADER_LENGTH + data.length;
        short headerLength = WS_HEADER_LENGTH;
        ByteBuffer buffer = ByteBuffer.allocate(packetLength);
        buffer.putInt(packetLength);
        buffer.putShort(headerLength);
        buffer.putShort(WS_VERSION);
        buffer.putInt(operation);
        buffer.putInt(1);
        buffer.put(ByteBuffer.wrap(data));
        return buffer;
    }

    public ArrayList<DanmakuPacket> decode(ByteBuffer buffer) {
        ArrayList<DanmakuPacket> packets = new ArrayList<DanmakuPacket>();
        int pointer = 0;
        int length = buffer.capacity();
        while (pointer + 1 < length) {
            int packetLength = buffer.getInt(WS_PACKAGE_OFFSET + pointer);
            byte[] packet = new byte[packetLength];
            buffer.position(pointer);
            buffer.get(packet, 0, packetLength);
            pointer += packetLength;
            packets.add(new DanmakuPacket(ByteBuffer.wrap(packet)));
        }
        return packets;
    }

    public static Main getIt() {
        return self;
    }

    public JsonObject simpleText(String text) {
        JsonObject result = new JsonObject();
        result.addProperty("text", text);
        return result;
    }

    private ClientPlayerEntity getPlayer() {
        return MinecraftClient.getInstance().player;
    }

    public boolean send(String content) {
        if(config.cookies.length()>=20){

        } else {
            return  false;
        }
        Thread sendThread = new Thread(() -> {
            CloseableHttpResponse httpResponse = null;
            HttpPost httpPost = new HttpPost("https://api.live.bilibili.com/msg/send");
            httpPost.setHeader("Cookies", config.cookies);
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("msg", content));
            params.add(new BasicNameValuePair("roomid", "" + currentRoomId));
            try {
                httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            try {
                httpResponse = httpClient.execute(httpPost);
                HttpEntity entity = httpResponse.getEntity();
                logger.info(EntityUtils.toString(entity));
            } catch (IOException e) {
                e.printStackTrace();
            }

        }, "Danmaku Send Thread");
        sendThread.start();
        return  true;
    }
}

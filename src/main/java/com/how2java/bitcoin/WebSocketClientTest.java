package com.how2java.bitcoin;

import com.alibaba.fastjson.JSON;
import com.how2java.bitcoin.mode.AuthorizationTextMessage;
import com.how2java.bitcoin.util.CheckSumBuilder;
import com.how2java.bitcoin.util.HttpClientVM;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.WebSocket.READYSTATE;
import org.java_websocket.handshake.ServerHandshake;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.NotYetConnectedException;

/**
 * @author zhangmiao3
 * @Description: 模拟websocket客户端发送请求Demo
 * @date 14:46 2018/5/24
 */
public class WebSocketClientTest {
    public static WebSocketClient client;

    public static void main(String[] args) throws URISyntaxException, NotYetConnectedException, InterruptedException {
        client = new WebSocketClient(new URI("ws://localhost:8088/ws/api/audiostream?appkey=zzzz&lan=zh&trans=1"), new Draft_17()) {

            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                System.out.println("打开websocket连接");
            }

            @Override
            public void onMessage(String arg0) {
                System.out.println("收到消息:" + arg0);
            }

            @Override
            public void onError(Exception arg0) {
                arg0.printStackTrace();
                System.out.println("发生错误已关闭");
            }

            @Override
            public void onClose(int arg0, String arg1, boolean arg2) {
                System.out.println("连接已关闭");
            }

            @Override
            public void onMessage(ByteBuffer bytes) {
                try {
                    System.out.println(new String(bytes.array(), "utf-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        };

        // websocket客户端发送连接请求
        client.connect();

        while (!client.getReadyState().equals(READYSTATE.OPEN)) {
            System.out.println("连接还没有打开");
        }
        System.out.println("连接打开了");
        // 创建连接鉴权控制报文
        AuthorizationTextMessage authorizationTextMessage = new AuthorizationTextMessage();
        // 随机值
        String nonce = "1234";
        // 当前时间戳
        String timeStamp = String.valueOf(System.currentTimeMillis());
        // 注册的appkey对应的appsecret
        String appsecret = "zzzz_shenqingneice";
        authorizationTextMessage.setNonce(nonce);
        authorizationTextMessage.setTimestamp(timeStamp);
        // 设置验签checksum值（appSecret+nonce+curtime字符串的sha1值）
        authorizationTextMessage.setChecksum(CheckSumBuilder.getCheckSum(appsecret, nonce, timeStamp));
        // 发送鉴权控制报文
        sendTextMessage(JSON.toJSONString(authorizationTextMessage));
        // 从保存的pcm文件中取出字节码，来模拟发送音频流
        byte[] bytesFromRemoteFile = HttpClientVM.getBytesFromRemoteFile("http://owq01tqh9.bkt.clouddn.com/076ab4ecf5b64a108d56bbb481c1c883_634870.pcm");
        int count = 0;
        int length;
        while (true) {
            length = 1024;
            // 每次发送1kb
            byte[] sendBytes = new byte[length];
            int srcPos = length * count;
            // 超出文件范围停止发送音频流
            if (srcPos >= bytesFromRemoteFile.length - length - 1) {
                break;
            }
            System.arraycopy(bytesFromRemoteFile,  length * count, sendBytes, 0, length);
            sendByte(sendBytes);
            ++ count;
            Thread.sleep(100);
        }
        // 空的二进制消息，表示语音流结束
        sendByte("".getBytes());

        // 关闭websocket连接
        client.close();
    }

    // 发送字节流信息
    public static void sendByte(byte[] bytes) {
        client.send(bytes);
    }

    // 发送文本信息
    public static void sendTextMessage(String jsonString) {
        client.send(jsonString);
    }
}

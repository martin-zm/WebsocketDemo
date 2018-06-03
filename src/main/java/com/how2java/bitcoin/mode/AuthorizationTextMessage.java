package com.how2java.bitcoin.mode;

/**
 * @author zhangmiao3
 * @Description: ws接口鉴权传递参数
 * @date 11:19 2018/5/24
 */
public class AuthorizationTextMessage {
    // 必填，随机字符串用于生成哈希值
    private String nonce;

    // 时间戳 1970-1-01 00:00:00.000 至当前的秒数
    private String timestamp;

    // appSecret+nonce+curtime 字符串的 sha1 值
    private String checksum;

    public AuthorizationTextMessage() {
    }

    public AuthorizationTextMessage(String nonce, String timestamp, String checksum) {
        this.nonce = nonce;
        this.timestamp = timestamp;
        this.checksum = checksum;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }
}

package com.how2java.bitcoin.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @Description: http请求工具类
 * @author yuchongjun
 * @date 2017年3月27日
 */
public class HttpClientVM {

    private final static String USER_AGENT = "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0)";

    private DefaultHttpClient client;

    public DefaultHttpClient getClient() {
        return client;
    }

    public String accessGet(String url) throws Exception {
        return accessGet(url, null);
    }

    public String accessGet(String url, String encoding) throws Exception {
        if (url.startsWith("http")) {}
        if (StringUtils.isBlank(encoding)) {
            encoding = "utf-8";
        }
        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("User-Agent", USER_AGENT);
        HttpResponse response = client.execute(httpGet);
        if (response.getStatusLine().getStatusCode() == 302) {
            String redirectUrl = response.getHeaders("location")[0].getValue();
            return accessGet(redirectUrl, encoding);
        }
        String entuity = EntityUtils.toString(response.getEntity(), encoding);
        return entuity;
    }

    public String accessPost(String url, Map<String, String> keyValue, String encoding) throws Exception {
        if (StringUtils.isBlank(encoding)) {
            encoding = "utf-8";
        }
        List<NameValuePair> formparams = new ArrayList<NameValuePair>();
        for (String key : keyValue.keySet()) {
            formparams.add(new BasicNameValuePair(key, keyValue.get(key)));
        }
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, encoding);
        HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader("User-Agent", USER_AGENT);
        httpPost.setEntity(entity);
        HttpResponse response = client.execute(httpPost);
        if (response.getStatusLine().getStatusCode() == 302) {
            String redirectUrl = response.getHeaders("location")[0].getValue();
            return accessGet(redirectUrl, encoding);
        }
        String entuity = EntityUtils.toString(response.getEntity(), encoding);
        return entuity;
    }

    public String accessPost(String url, String paramsStr) throws Exception {
        return accessPost(url, paramsStr, null);
    }

    public String accessPost(String url, String paramsStr, Map<String, String> headMap) throws Exception {
        String encoding = "utf-8";
        StringEntity entity = new StringEntity(paramsStr, encoding);
        entity.setContentType("application/json");
        HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader("User-Agent", USER_AGENT);
        if (headMap != null && headMap.size() > 0) {
            for (Map.Entry<String, String> entry : headMap.entrySet()) {
                httpPost.setHeader(entry.getKey(), entry.getValue());
            }
        }
        httpPost.setEntity(entity);
        HttpResponse response = client.execute(httpPost);
        String entuity = IOUtils.toString(response.getEntity().getContent(), encoding);
        return entuity;
    }

    public void close() {
        shutdown();
    }

    private void shutdown() {
        try {
            if (client != null) {
                client.getConnectionManager().shutdown();
            }
        } catch (Exception e) {}
    }

    public HttpClientVM(int timeout){
        BasicHttpParams params = new BasicHttpParams();
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setUseExpectContinue(params, true);
        HttpConnectionParams.setStaleCheckingEnabled(params, true);
        HttpConnectionParams.setConnectionTimeout(params, timeout * 1000);
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            X509TrustManager tm = new X509TrustManager() {

                @Override
                public void checkClientTrusted(java.security.cert.X509Certificate[] arg0, String arg1)
                        throws java.security.cert.CertificateException {
                }

                @Override
                public void checkServerTrusted(java.security.cert.X509Certificate[] arg0, String arg1)
                        throws java.security.cert.CertificateException {
                }

                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

            };

            ctx.init(null, new TrustManager[] { tm}, null);
            SSLSocketFactory ssf = new SSLSocketFactory(ctx, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            schemeRegistry.register(new Scheme("https", 443, ssf));
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        ClientConnectionManager ccm = new ThreadSafeClientConnManager(schemeRegistry);
        client = new DefaultHttpClient(ccm, params);
        HttpClientParams.setCookiePolicy(client.getParams(), CookiePolicy.BROWSER_COMPATIBILITY);
    }

    public HttpClientVM(){
        this(60);
    }

    /**
     * 获取远程文件
     * 
     * @param fileUrl
     * @return
     */
    public static byte[] getBytesFromRemoteFile(String fileUrl) {
        if (StringUtils.isBlank(fileUrl)) {
            return null;
        }
        URL url = null;
        HttpURLConnection conn = null;
        InputStream input = null;
        byte[] bytes = null;
        try {
            url = new URL(fileUrl);
            conn = (HttpURLConnection) url.openConnection();
            input = conn.getInputStream();
            // 不能通过input.available()获取文件大小，因为available()返回的是可以不受阻塞地从此输入流读取（或跳过）的估计字节数
            // 而网络下载时，read()方法是阻塞的
            bytes = new byte[conn.getContentLength()];
            IOUtils.readFully(conn.getInputStream(), bytes);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(input);
            if (conn != null) {
                conn.disconnect();
            }
        }
        return bytes;
    }

    private static String getExtension(String fileName) {
        if (-1 == StringUtils.indexOf(fileName, "."))
            return "";
        String ext = StringUtils.substring(fileName, StringUtils.lastIndexOf(fileName, "."));
        return StringUtils.trimToEmpty(ext);
    }

    /**
     * 获取远程文件流
     * 
     * @param fileUrl
     * @return
     */
    public static File getRemoteFile(String fileUrl) {
        if (StringUtils.isBlank(fileUrl)) {
            return null;
        }
        URL url = null;
        HttpURLConnection conn = null;
        InputStream input = null;
        File file = null;
        try {
            url = new URL(fileUrl);
            conn = (HttpURLConnection) url.openConnection();
            input = conn.getInputStream();
            file = File.createTempFile("tmp", getExtension(fileUrl));
            FileUtils.copyInputStreamToFile(input, file);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(input);
            if (conn != null) {
                conn.disconnect();
            }
        }
        return file;
    }

    /**
     * 获取远程文件大小
     * 
     * @param fileUrl
     * @return
     */
    public static int getRemoteFileSize(String fileUrl) {
        int size = 0;
        if (StringUtils.isBlank(fileUrl)) {
            return size;
        }
        URL url = null;
        HttpURLConnection conn = null;
        try {
            url = new URL(fileUrl);
            conn = (HttpURLConnection) url.openConnection();
            size = conn.getContentLength();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return size;
    }

    public static void main(String[] args) {
        // System.out.println(getRemoteFileSize("http://nos.netease.com/nmtp/COMMON_VIDEO_201711161551016594ded3f19b4b5d90dfecf4941f079c.mp4"));
        System.out.println(getRemoteFileSize(
                "http://nos.netease.com/nmtp/COMMON_VIDEO_20180427163733e2b3c504f130403b9a1416a75749de68.pcm"));
    }
}

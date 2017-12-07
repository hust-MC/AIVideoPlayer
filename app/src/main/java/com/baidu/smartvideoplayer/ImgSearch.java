package com.baidu.smartvideoplayer;

import android.os.Environment;
import android.util.Log;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class ImgSearch {
    static final String DATEFORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    static final String imgFile = Environment.getExternalStorageDirectory()
            + "/DCIM/Camera/" + "IMG_20171123_151121.jpg";// 待处理的图片

    // 图片转化成base64字符串
    public static String getImageStr(byte[] imgArray) throws UnsupportedEncodingException {
        // 将图片文件转化为字节数组字符串，并对其进行Base64编码处理
        InputStream in = null;
        byte[] data = null;

        int bytes = imgArray.length;

        ByteBuffer buf = ByteBuffer.allocate(bytes);

        byte[] byteArray = buf.array();

        // 读取图片字节数组
        try {
            in = new ByteArrayInputStream(byteArray);
            data = new byte[in.available()];
            in.read(data);
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new String(Base64.encodeBase64(imgArray), "UTF-8");
    }

    static void sampleOfNormalInterface(final byte[] imgArray, final ImageSearchListener listener) {

        new Thread(new Runnable() {
            @Override
            public void run() {

                HttpClient httpClient;
                try {
                    httpClient = new DefaultHttpClient();
                    HttpPost httpost = new HttpPost(
                            "http://openapi-test.jpaas-matrixoff00.baidu.com/rest/2" +
                                    ".0/vis-guessword/v1/guessword?access_token=21" +
                                    ".21cda41bd9739ce5a083f3326f64b610" +
                                    ".2592000.1469180474.1686270206-11101624");
                    httpost.addHeader("Content-Type", "application/x-www-form-urlencoded");
                    List<NameValuePair> nameValuePairs = new ArrayList<>();
                    String imgBase64 = getImageStr(imgArray);
                    Log.d("MC", "img : " + imgBase64);
                    nameValuePairs.add(new BasicNameValuePair("image", imgBase64));
                    httpost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                    // Executing the request.
                    HttpResponse response = httpClient.execute(httpost);
                    BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity()
                            .getContent()));
                    String line;
                    StringBuilder stringBuilder = new StringBuilder();
                    try {
                        while ((line = rd.readLine()) != null) {
                            stringBuilder.append(line);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    Log.d("MC", stringBuilder.toString());

                    JSONArray resultArray = new JSONObject(stringBuilder.toString()).getJSONArray("result");
                    String result = resultArray.getJSONObject(0).getString("keyword");
                    listener.onSuccess(result);
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    interface ImageSearchListener {
        void onSuccess(String result);

        void onFail();
    }
}


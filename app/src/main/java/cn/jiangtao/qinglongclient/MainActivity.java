package cn.jiangtao.qinglongclient;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private Button uploadCookieButton;

    Handler handler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            String str = (String) msg.obj;
            if (str != null) {
                Toast.makeText(getBaseContext(), str, Toast.LENGTH_SHORT).show();
            }
        }
    };
    private QLApi api = new QLApi();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 隐藏标题
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_main);



        webView = findViewById(R.id.webView);
        uploadCookieButton = findViewById(R.id.uploadCookieButton);

        // 设置 WebView 的基本属性
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient());
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl("https://m.jd.com");


        // 上传 Cookie
        uploadCookieButton.setOnClickListener(v -> uploadCookie());

        login();
    }


    private void login() {
        new Thread() {
            @Override
            public void run() {
                try {
                    api.login();
                    MainActivity.this.info("登录成功");
                } catch (Exception e) {
                    MainActivity.this.err(e);
                }
            }
        }.start();

    }

    private void info(String msg) {
        handler.sendMessage(handler.obtainMessage(1, msg));
    }

    private void err(String msg) {
        handler.sendMessage(handler.obtainMessage(-1, msg));
    }

    private void err(Exception e) {
        String msg = e.getMessage();
        if (msg == null) {
            msg = e.getClass().getSimpleName();
        }
        handler.sendMessage(handler.obtainMessage(-1, msg));
    }

    private void uploadCookie() {
        String deviceInfo = getDeviceInfo();
        String cookies = CookieManager.getInstance().getCookie(webView.getUrl());

        String[] cs = cookies.split(";");

        StringBuilder sb = new StringBuilder();
        for (String c : cs) {
            if(c.contains("pt_key") || c.contains("pt_pin")){
                sb.append(c.trim()).append(";");
            }
        }
        if (sb.length() == 0) {
            err("cookie未登录");
            return;
        }


        new Thread() {
            @Override
            public void run() {
                try {
                    JSONArray list = api.list();
                    for(int i = 0; i < list.length(); i++){
                        JSONObject  o = (JSONObject) list.get(i);

                        if(o.getString("remarks").equals(deviceInfo)){
                            api.delete(o.getInt("id"));
                            info("删除" + deviceInfo +"成功");
                        }

                    }

                    JSONObject param = new JSONObject();
                    param.put("remarks", deviceInfo);
                    param.put("name", "JD_COOKIE");
                    param.put("value", sb.toString());


                    JSONArray arr = new JSONArray();
                    arr.put(param);
                    api.add(arr);
                    info("添加成功");
                } catch (Exception e) {
                    MainActivity.this.err(e);
                }
            }
        }.start();

    }

    private String getDeviceInfo() {
        String info = Build.FINGERPRINT;
        return info;
    }


}
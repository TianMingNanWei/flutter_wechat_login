package com.kakarot.flutter_wechat_login;

import static android.content.ContentValues.TAG;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import com.tencent.mm.opensdk.constants.ConstantsAPI;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

/**
 * FlutterWechatLoginPlugin
 */
public class FlutterWechatLoginPlugin extends BroadcastReceiver implements FlutterPlugin, MethodCallHandler,
        ActivityAware {

    private MethodChannel channel;
    private Activity activity;
    private Context context;

    private String appId;
    private String secret;
    private IWXAPI api; 
    private Result loginResult;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "flutter_wechat_login");
        channel.setMethodCallHandler(this);
        context = flutterPluginBinding.getApplicationContext();
        IntentFilter intentFilter = new IntentFilter("flutter_wechat_login");
        if (Build.VERSION.SDK_INT >= 33) {
            flutterPluginBinding.getApplicationContext().registerReceiver(this, intentFilter,
                    Context.RECEIVER_EXPORTED);

        } else {
            flutterPluginBinding.getApplicationContext().registerReceiver(this, intentFilter);
        }
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        if (call.method.equals("init")) {
            this.appId = call.argument("appId");
            this.secret = call.argument("secret");
            Log.i("flutter_wechat_login", "call init -> appId= " + appId);
            regToWx();
            result.success(null);
        } else if (call.method.equals("isInstalled")) {
            boolean isInstalled = api.isWXAppInstalled();
            result.success(isInstalled);
        } else if (call.method.equals("login")) {
            SendAuth.Req req = new SendAuth.Req();
            req.scope = "snsapi_userinfo"; 
            req.state = "flutter_wechat_login";
            api.sendReq(req);
            this.loginResult = result;
        }
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
        loginResult = null;
    }

    private void regToWx() {
        // 通过WXAPIFactory工厂，获取IWXAPI的实例
        Log.i("flutter_wechat_login", "通过WXAPIFactory工厂，获取IWXAPI的实例");
        api = WXAPIFactory.createWXAPI(this.context, this.appId, true);
        // api = WXAPIFactory.createWXAPI(this.context, null);
        // 将应用的appId注册到微信
        Log.i("flutter_wechat_login", "将应用的appId注册到微信");
        api.registerApp(this.appId);
        // 建议动态监听微信启动广播进行注册到微信

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            new ContextWrapper(context).registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    // 将该app注册到微信
                    Log.i("flutter_wechat_login", "监听微信启动广播进行注册到微信");
                    api.registerApp(appId);
                }
            }, new IntentFilter(ConstantsAPI.ACTION_REFRESH_WXAPP), Context.RECEIVER_EXPORTED);
        } else {
            new ContextWrapper(context).registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    // 将该app注册到微信
                    Log.i("flutter_wechat_login", "监听微信启动广播进行注册到微信");
                    api.registerApp(appId);
                }
            }, new IntentFilter(ConstantsAPI.ACTION_REFRESH_WXAPP));
        }
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        this.activity = binding.getActivity();
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {

    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {

    }

    @Override
    public void onDetachedFromActivity() {

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        int errCode = intent.getIntExtra("errCode", -999);
        String errStr = intent.getStringExtra("errStr");
        int type = intent.getIntExtra("type", -1);

        JSONObject jsonObject = new JSONObject();
        try {
            if (this.loginResult != null) {
                if (errCode == 0) {
                    if (type == ConstantsAPI.COMMAND_SENDAUTH) {
                        jsonObject.put("errCode", errCode);
                        jsonObject.put("errStr", errStr);
                        jsonObject.put("code", intent.getStringExtra("code"));
                        jsonObject.put("state", intent.getStringExtra("state"));
                        jsonObject.put("lang", intent.getStringExtra("lang"));
                        jsonObject.put("country", intent.getStringExtra("country"));
                        Log.i("flutter_wechat_login", "auth -> " + jsonObject.toString());
                        this.loginResult.success(jsonObject.toString());
                        this.loginResult = null;
                    }
                } else {
                    this.loginResult.success(jsonObject.toString());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (this.loginResult != null) {
                this.loginResult.success(jsonObject.toString());
            }
        } finally {
            this.loginResult = null;
        }
    }
}

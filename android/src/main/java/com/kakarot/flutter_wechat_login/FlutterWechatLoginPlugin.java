package com.kakarot.flutter_wechat_login;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import com.tencent.mm.opensdk.constants.ConstantsAPI;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import org.json.JSONException;
import org.json.JSONObject;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;

public class FlutterWechatLoginPlugin extends BroadcastReceiver
        implements FlutterPlugin, MethodChannel.MethodCallHandler, ActivityAware {

    private static final String TAG = "FlutterWechatLoginPlugin";
    private MethodChannel channel;
    private Activity activity;
    private Context context;
    private String appId;
    private String secret;
    private IWXAPI api;
    private MethodChannel.Result loginResult;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "flutter_wechat_login");
        channel.setMethodCallHandler(this);
        context = flutterPluginBinding.getApplicationContext();
        registerReceiver(context);
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull MethodChannel.Result result) {
        switch (call.method) {
            case "init":
                appId = call.argument("appId");
                secret = call.argument("secret");
                Log.i(TAG, "call init -> appId= " + appId);
                registerToWechat();
                result.success(null);
                break;
            case "isInstalled":
                boolean isInstalled = api.isWXAppInstalled();
                result.success(isInstalled);
                break;
            case "login":
                SendAuth.Req req = new SendAuth.Req();
                req.scope = "snsapi_userinfo";
                req.state = "flutter_wechat_login";
                api.sendReq(req);
                loginResult = result;
                break;
            default:
                result.notImplemented();
        }
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
        loginResult = null;
    }

    private void registerToWechat() {
        Log.i(TAG, "获取IWXAPI实例");
        api = WXAPIFactory.createWXAPI(context, appId, true);
        Log.i(TAG, "注册appId到微信");
        api.registerApp(appId);
        registerWechatRefreshReceiver(context);
    }

    private void registerReceiver(Context context) {
        IntentFilter intentFilter = new IntentFilter("flutter_wechat_login");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(this, intentFilter, Context.RECEIVER_EXPORTED);
        } else {
            context.registerReceiver(this, intentFilter);
        }
    }

    private void registerWechatRefreshReceiver(Context context) {
        new ContextWrapper(context).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i(TAG, "监听微信启动广播进行注册到微信");
                api.registerApp(appId);
            }
        }, new IntentFilter(ConstantsAPI.ACTION_REFRESH_WXAPP), getReceiverFlag());
    }

    private int getReceiverFlag() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ? Context.RECEIVER_EXPORTED : 0;
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        activity = binding.getActivity();
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        // No-op
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        // No-op
    }

    @Override
    public void onDetachedFromActivity() {
        // No-op
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        int errCode = intent.getIntExtra("errCode", -999);
        String errStr = intent.getStringExtra("errStr");
        int type = intent.getIntExtra("type", -1);

        JSONObject jsonObject = new JSONObject();
        try {
            if (loginResult != null) {
                if (errCode == 0) {
                    if (type == ConstantsAPI.COMMAND_SENDAUTH) {
                        jsonObject.put("errCode", errCode);
                        jsonObject.put("errStr", errStr);
                        jsonObject.put("code", intent.getStringExtra("code"));
                        jsonObject.put("state", intent.getStringExtra("state"));
                        jsonObject.put("lang", intent.getStringExtra("lang"));
                        jsonObject.put("country", intent.getStringExtra("country"));
                        Log.i(TAG, "auth -> " + jsonObject.toString());
                        loginResult.success(jsonObject.toString());
                        loginResult = null;
                    }
                } else {
                    loginResult.success(jsonObject.toString());
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "JSON异常", e);
            if (loginResult != null) {
                loginResult.success(jsonObject.toString());
            }
        } finally {
            loginResult = null;
        }
    }
}
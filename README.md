# flutter_wechat_login

Flutter Wechat Login Plugin

|             | Android | iOS |
| ----------- | ------- | --- |
| **Support** | YES     | YES |

## Features

This plugin has integrated the function of WeChat login.

## Getting Started

Before using this plugin, it is strongly recommended to read the official documentation in detail

- [Android access guide](https://developers.weixin.qq.com/doc/oplatform/Mobile_App/Access_Guide/Android.html)
- [iOS access guide](https://developers.weixin.qq.com/doc/oplatform/Mobile_App/Access_Guide/iOS.html)
- [Mobile App WeChat Login Development Guide](https://developers.weixin.qq.com/doc/oplatform/Mobile_App/WeChat_Login/Development_Guide.html)

### Usage

```dart
import 'package:flutter_wechat_login/flutter_wechat_login.dart';

// Create FlutterWechatLogin
final flutterWechatLogin = FlutterWechatLogin();

// Initialization
await flutterQqLogin.init(appId: "Your AppID", secret: "Your AppSecret", universalLink: "Your Universal Links(iOS Required)");

// Determine whether the WeChat application is currently installed
bool isInstalled = await flutterWechatLogin.isInstalled();

// Call up WeChat login, and return code after successful login
Map<String, dynamic> wechatInfo = await flutterWechatLogin.login();

```

### Configure Android version

- Create a package name `wxapi` under the project `android` directory `/app/src/main/java/packageName`, and then create a new `WXEntryActivity` under this package name, the code is as follows:

```java
package packageName.wxapi;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.tencent.mm.opensdk.constants.ConstantsAPI;
import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.modelbiz.SubscribeMessage;
import com.tencent.mm.opensdk.modelbiz.WXLaunchMiniProgram;
import com.tencent.mm.opensdk.modelbiz.WXOpenBusinessView;
import com.tencent.mm.opensdk.modelbiz.WXOpenBusinessWebview;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

public class WXEntryActivity extends Activity implements IWXAPIEventHandler {

    private IWXAPI api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("flutter_wechat_login", "onCreate");
        super.onCreate(savedInstanceState);
        api = WXAPIFactory.createWXAPI(this, "", false);
        try {
            Intent intent = getIntent();
            api.handleIntent(intent, this);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        api.handleIntent(intent, this);
    }

    @Override
    public void onReq(BaseReq req) {
    }

    @Override
    public void onResp(BaseResp resp) {
        Log.d("flutter_wechat_login", "onResp -> " + resp.errCode);

        Intent intent = new Intent("flutter_wechat_login");
        intent.putExtra("errCode", resp.errCode);
        intent.putExtra("errStr", resp.errStr);
        intent.putExtra("type", resp.getType());

        if (resp.getType() == ConstantsAPI.COMMAND_SENDAUTH) {
            SendAuth.Resp authResp = (SendAuth.Resp) resp;
            Log.i("flutter_wechat_login", "COMMAND_SENDAUTH");
            intent.putExtra("code", authResp.code);
            intent.putExtra("state", authResp.state);
            intent.putExtra("lang", authResp.lang);
            intent.putExtra("country", authResp.country);
        }

        sendBroadcast(intent);
        finish();
    }
}
```

- Configure `android/app/src/main/AndroidManifest.xml`

>WeChat needs to verify the package name, so the path of the Activity must be `your package name.wxapi.WXEntryActivity`, where `your package name` must be the package name filled in by the WeChat open platform registration application.

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
 <!-- new content start -->
 <uses-permission android:name="android.permission.INTERNET" />
 <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
 <!-- new content end -->
 <application>
  ...
  <!-- new content start -->
  <activity
   android:name="Your package name.wxapi.WXEntryActivity"
   android:theme="@android:style/Theme.Translucent.NoTitleBar"
   android:exported="true"
   android:taskAffinity="Your package name"
   android:launchMode="singleTask">
  </activity>
  <!-- new content end -->
  ...
 </application>
 <!-- new content start -->
 <queries>
  <package android:name="com.tencent.mm" />
 </queries>
 <!-- new content end -->
</manifest>
```

### Configure iOS version

Configure `URL Types`

- Use `xcode` to open your iOS project `Runner.xcworkspace`
- In the `info` configuration tab under `URL Types`, add a new entry
  - `identifier` fills in `weixin`
  - `URL Schemes` fills in `Your APPID`
  - As shown below:
      ![xcode configuration example](https://raw.githubusercontent.com/yechong/flutter_wechat_login/main/doc/images/ios_screenshot_01.png)

Configure `LSApplicationQueriesSchemes`

- Method 1, configure `info` in `xcode`
  - Open `info` configuration, add a `LSApplicationQueriesSchemes`, namely `Queried URL Schemes`
  - Add these items:
    - weixin
    - weixinULAPI
    - weixinURLParamsAPI
  - As shown below：
      ![xcode configuration example](https://raw.githubusercontent.com/yechong/flutter_wechat_login/main/doc/images/ios_screenshot_02.png)

- Method 2, modify `Info.plist` directly
  - Use `Android Studio` to open `ios/Runner/Info.plist` under the project project
  - Add the following configuration under the `dict` node (refer to the configuration format in the file):

```xml
<key>LSApplicationQueriesSchemes</key>
<array>
 <string>weixin</string>
 <string>weixinULAPI</string>
 <string>weixinURLParamsAPI</string>
</array>
```

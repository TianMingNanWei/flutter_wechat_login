import 'dart:convert';
import 'package:http/http.dart' as http;

import 'flutter_wechat_login_platform_interface.dart';

class FlutterWechatLogin {
  static final FlutterWechatLogin _instance = FlutterWechatLogin._internal();

  factory FlutterWechatLogin() {
    return _instance;
  }

  FlutterWechatLogin._internal();

  late String _appId;
  late String _secret;

  init({required String appId, required String secret, String? universalLink}) {
    _appId = appId;
    _secret = secret;
    return FlutterWechatLoginPlatform.instance
        .init(appId: appId, secret: secret, universalLink: universalLink);
  }

  Future<bool> isInstalled() {
    return FlutterWechatLoginPlatform.instance.isInstalled();
  }

  Future<Map<String, dynamic>> login() async {
    String data = await FlutterWechatLoginPlatform.instance.login() ?? "";
    if (data.isNotEmpty) {
      return jsonDecode(data);
    }
    return {};
  }

  Future<Map<String, dynamic>> getAccessToken(String code) async {
    final url = Uri.parse(
        'https://api.weixin.qq.com/sns/oauth2/access_token?appid=$_appId&secret=$_secret&code=$code&grant_type=authorization_code');
    final response = await http.get(url);
    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to get access token');
    }
  }

  Future<Map<String, dynamic>> checkToken(
      String accessToken, String openid) async {
    final url = Uri.parse(
        'https://api.weixin.qq.com/sns/auth?access_token=$accessToken&openid=$openid');
    final response = await http.get(url);
    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to check token');
    }
  }

  Future<Map<String, dynamic>> refreshToken(String refreshToken) async {
    final url = Uri.parse(
        'https://api.weixin.qq.com/sns/oauth2/refresh_token?appid=$_appId&grant_type=refresh_token&refresh_token=$refreshToken');
    final response = await http.get(url);
    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to refresh token');
    }
  }

  Future<Map<String, dynamic>> getUserInfo(
      String accessToken, String openid) async {
    final url = Uri.parse(
        'https://api.weixin.qq.com/sns/userinfo?access_token=$accessToken&openid=$openid');
    final response = await http.get(url);
    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to get user info');
    }
  }
}

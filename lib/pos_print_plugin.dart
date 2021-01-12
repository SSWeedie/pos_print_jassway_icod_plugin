import 'dart:async';

import 'package:PosPrintPlugin/models/result_print.dart';
import 'package:flutter/services.dart';

class PosPrintPlugin {
  static const MethodChannel _channel = const MethodChannel('PosPrintPlugin');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  static Future<bool> get connectPrint async {
    final connect = await _channel.invokeMethod('connect_print');
    return connect;
  }

  static Future<ResultPrint> printText({String value}) async {
    final printText =
        await _channel.invokeMethod('print_text', {"text": value});
    print(printText);
    final resultPrint = ResultPrint(success: true, message: 'printing....');
    return resultPrint;
  }

  static Future<ResultPrint> printQRCode(String content, {int modeSize}) async {
    final printText = await _channel.invokeMethod(
        'print_qrcode', {"content": content, "modeSize": modeSize ?? 13});
    final resultPrint = ResultPrint(success: true, message: 'printing....');
    return resultPrint;
  }

  static Future<ResultPrint> printLogo(List<int> data) async {
    await _channel.invokeMethod('print_image', {"data": data});
    return ResultPrint();
  }

  static Future printThreeColumn() async {
    await _channel.invokeMethod('print_three_column');
  }

  static Future printBreakLine() async {
    await _channel.invokeMethod('print_break_line');
  }
}

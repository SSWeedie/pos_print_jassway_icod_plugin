import 'dart:async';

import 'package:PosPrintPlugin/pos_print_plugin.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _platformVersion = 'Unknown';

  var _isConnect = 'Unconnected';

  @override
  void initState() {
    super.initState();
    initPlatformState();
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    String platformVersion;
    // Platform messages may fail, so we use a try/catch PlatformException.
    try {
      platformVersion = await PosPrintPlugin.platformVersion;
    } on PlatformException {
      platformVersion = 'Failed to get platform version.';
    }

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;

    setState(() {
      _platformVersion = platformVersion;
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.spaceAround,
            children: [
              Text('Running on: $_platformVersion\n'),
              RaisedButton(
                onPressed: () async {
                  final connected = await PosPrintPlugin.connectPrint;
                  setState(() {
                    _isConnect = connected ? 'Connected' : 'Connect failure';
                  });
                },
                child: Text('$_isConnect'),
              ),
              RaisedButton(
                onPressed: () async {
                  final value = "--------------------------------------------"
                      "\nCà phê               1                16.000"
                      "\nCà phê               1                16.000"
                      "\nCà phê               1                16.000"
                      "\nCà phê               1                16.000"
                      "\nCà phê               1                16.000"
                      "\nCà phê               1                16.000"
                      "\nCà phê               1                16.000"
                      "\nCà phê               1                16.000"
                      "\n---------------------------------------------";
                  PosPrintPlugin.printText(value: value);
                },
                child: Text('Print text'),
              ),
              RaisedButton(
                onPressed: () async {
                  PosPrintPlugin.printQRCode("Đây là mã barcode", modeSize: 6);
                },
                child: Text('Print barcode'),
              ),
              RaisedButton(
                onPressed: () async {
                  ByteData bytes = await rootBundle.load('assets/logo.bmp');
                  List<int> values = bytes.buffer.asUint8List();
                  PosPrintPlugin.printLogo(values);
                },
                child: Text('Print image'),
              ),
              RaisedButton(
                onPressed: () async {
                  PosPrintPlugin.printBreakLine();
                },
                child: Text('Print break line'),
              ),
              RaisedButton(
                onPressed: () async {
                  PosPrintPlugin.printThreeColumn();
                },
                child: Text('Print three column'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

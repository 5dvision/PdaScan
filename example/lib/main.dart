import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:pda_scan/pda_scan.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _platformVersion = 'Unknown';


  @override
  void initState() {
    super.initState();
    if(mounted) {
      initScan();
    }
  }

  // Platform messages are asynchronous, so we initialize in an async method.


  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: FlatButton(child: Text('扫描'),onPressed: (){

          },)
        ),
      ),
    );
  }

  void initScan() {
    var pdaScan = PdaScan();
    pdaScan.onScanResult.listen(_onEvent, onError: _onError);
  }

  void _onEvent(Object event) {
    print("返回成功的数据"+event.toString());
  }
  void _onError(Object error) {
    print("扫描错误的信息"+error.toString());
  }
}





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

  String result =" 没有显示内容";

  @override
  void initState() {
    super.initState();

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
          children: <Widget>[
          FlatButton(child: Text('扫描'),onPressed: (){
          initScan();
           },),
          Text(result)
  ],
  )
        ),
      ),
    );
  }

  Future<void> initScan() async {
    var  isPDA = await PdaScan.isPDA;
    if(isPDA == true){
      var pdaScan = PdaScan();
      pdaScan.onScanResult.listen(_onEvent, onError: _onError);
    }else{
      String content = await PdaScan.scanResult;
      print("扫描结果是：" + content);
      setState(() {
        result  = content;
      });
    }
  }

  void _onEvent(Object event) {
    print("返回成功的数据"+event.toString());
    print("PDA 结果是：" + event);
    setState(() {
      result  = event;
    });
  }
  void _onError(Object error) {
    print("扫描错误的信息"+error.toString());
  }
}





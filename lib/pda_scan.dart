import 'dart:async';

import 'package:flutter/services.dart';
import 'package:meta/meta.dart' show visibleForTesting;

class PdaScan {

  static PdaScan _instance;
  final EventChannel _eventChannel;
  Stream<String> _onScanString;

  static const MethodChannel methodChannel = const MethodChannel("plugins.flutter.io/missfresh.scan.device");

  factory PdaScan(){
    if(_instance == null) {
      if(isPDA == true) {
        final EventChannel eventChannel = const EventChannel(
            "plugins.flutter.io/missfresh.scan");
        _instance = PdaScan.private(eventChannel);
      }
    }
    return _instance;
  }

  static Future<bool> get isPDA async {
     return await methodChannel.invokeMethod('isPDA');
  }
  
  static Future<String> get scanResult async {
    return await methodChannel.invokeMethod("scan");
  }

  @visibleForTesting
  PdaScan.private(this._eventChannel);

  Stream<String> get onScanResult {
    if (_onScanString == null) {
      _onScanString = _eventChannel.receiveBroadcastStream().map((dynamic event) => event.toString());
    }
    return _onScanString;
  }



}

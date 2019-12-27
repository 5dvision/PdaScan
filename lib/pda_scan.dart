import 'dart:async';

import 'package:flutter/services.dart';
import 'package:meta/meta.dart' show visibleForTesting;

class PdaScan {

  static PdaScan _instance;
  final EventChannel _eventChannel;
  Stream<String> _onScanString;


  factory PdaScan(){
    if(_instance == null) {
      final EventChannel eventChannel = const EventChannel("plugins.flutter.io/missfresh.scan");
      _instance = PdaScan.private(eventChannel);
    }
    return _instance;
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

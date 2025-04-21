
import 'my_flutter_rfid_reader_platform_interface.dart';
import 'dart:async';

import 'package:flutter/services.dart';

class MyFlutterRfidReader {
  Future<String?> getPlatformVersion() {
    return MyFlutterRfidReaderPlatform.instance.getPlatformVersion();
  }
  static const MethodChannel _channel =
  MethodChannel('my_flutter_rfid_reader');

  static Future<String?> Init() async {
    final String? code = await _channel.invokeMethod('init');
    return code;
  }

}

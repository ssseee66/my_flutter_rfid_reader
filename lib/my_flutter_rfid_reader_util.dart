import 'dart:convert';

import 'package:flutter/services.dart';

class MyFlutterRfidReaderUtil {
  MyFlutterRfidReaderUtil._();

  factory MyFlutterRfidReaderUtil() => _instance;
  static final MyFlutterRfidReaderUtil _instance = MyFlutterRfidReaderUtil._();

  BasicMessageChannel flutterChannel = const BasicMessageChannel(
      "flutter_rfid_android",
      StandardMessageCodec()
  );

  void sendMessageToAndroid(String methodName, dynamic arg) async {
    flutterChannel.send({methodName: arg});
  }
  void connect() {
    flutterChannel.send({"startConnect": true});
  }
  void turnOnPower() {
    flutterChannel.send({"turnOnPower": true});
  }
  void turnOffPower() {
    flutterChannel.send({"turnOffPower": true});
  }
  void reader() {
    flutterChannel.send({"startReader": true});
  }
  void readerEpc() {
    flutterChannel.send({"startReaderEpc": true});
  }
  void readerOver() {
    flutterChannel.send({"readerOver": true});
  }
  void writeEpcData(int epcDataArea, String epcData) {
    List<int> bytes = utf8.encode(epcData);
    String hexEpcData = bytes.map((byte) => byte.toRadixString(16).padLeft(2, '0')).join('');
    String dataArea = epcData.toString();
    String data = "$hexEpcData&$dataArea";
    flutterChannel.send({"writeEpcData": data});
  }
  void startListenerBroadcast() {
    flutterChannel.send({"startListenerBroadcast": true});
  }
  void stopListenerBroadcast() {
    flutterChannel.send({"stopListenerBroadcast": true});
  }

}
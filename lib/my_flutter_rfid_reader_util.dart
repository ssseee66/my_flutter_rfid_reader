import 'dart:convert';

import 'package:flutter/services.dart';
import 'operation_code.dart';

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
  Enum getOperation(int code) {
    switch (code) {
      case 0:
        return OperationCode.CONNECT;
      case 1:
        return OperationCode.CLOSE_CONNECT;
      case 2:
        return OperationCode.TURN_ON_POWER;
      case 3:
        return OperationCode.TURN_OFF_POWER;
      case 4:
        return OperationCode.QUERY_SERIAL_NUMBER;
      case 5:
        return OperationCode.READER;
      case 6:
        return OperationCode.READER_DATA;
      case 7:
        return OperationCode.WRIATE_DATA;
      case 8:
        return OperationCode.REGISTER_BROADCAST;
      case 9:
        return OperationCode.UNREGISTER_BROADCAST;
      case 10:
        return OperationCode.LISTENER_BROADCAST;
      default:
        return OperationCode.ERROR_CODE;
    }
  }

}
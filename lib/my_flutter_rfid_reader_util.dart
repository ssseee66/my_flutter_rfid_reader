import 'dart:convert';

import 'package:flutter/services.dart';
import 'package:my_flutter_rfid_reader/rfid_operation_failed_code.dart';
import 'rfid_operation_code.dart';

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
  void closeConnect() {
    flutterChannel.send({"closeConnect": true});
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
  void querySerialNumber() {
    flutterChannel.send({"querySerialNumber": true});
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
        return RfidOperationCode.CONNECT;
      case 1:
        return RfidOperationCode.CLOSE_CONNECT;
      case 2:
        return RfidOperationCode.TURN_ON_POWER;
      case 3:
        return RfidOperationCode.TURN_OFF_POWER;
      case 4:
        return RfidOperationCode.QUERY_SERIAL_NUMBER;
      case 5:
        return RfidOperationCode.READER;
      case 6:
        return RfidOperationCode.READER_DATA;
      case 7:
        return RfidOperationCode.WRITE_DATA;
      case 8:
        return RfidOperationCode.REGISTER_BROADCAST;
      case 9:
        return RfidOperationCode.UNREGISTER_BROADCAST;
      case 10:
        return RfidOperationCode.LISTENER_BROADCAST;
      case 11:
        return RfidOperationCode.READER_OVER;
      case 12:
        return RfidOperationCode.APPEAR_OVER;
      default:
        return RfidOperationCode.ERROR_CODE;
    }
  }
  Enum getFailedCode(int code) {
      switch (code) {
        case 0:
          return RfidOperationFailedCode.NOT_SUPPORT_RIFD;
        case 1:
          return RfidOperationFailedCode.NOT_CONNECTED;
        case 2:
          return RfidOperationFailedCode.NOT_TURN_ON_POWER;
        case 3:
          return RfidOperationFailedCode.NOT_APPEAR_OVER;
        case 4:
          return RfidOperationFailedCode.QUERY_SERIAL_NUMBER_FAILED;
        case 5:
          return RfidOperationFailedCode.READER_OPERATION_FAILED;
        case 6:
          return RfidOperationFailedCode.WRITE_DATA_FAILED;
        case 7:
          return RfidOperationFailedCode.UNREGISTER_BROADCAST;
        default:
          return RfidOperationFailedCode.OTHER_FAILED;
      }
  }
  String? getWriteDataInfo(int code) {
    if (writeDataInfoMap.containsKey(code)) {
      return writeDataInfoMap[code];
    } else {
      return "Other error";
    }
  }
  static const Map<int, String> writeDataInfoMap = {
    // 写入成功
    0X00 : "Write successfully",
    // 天线端口参数错误
    0X01 : "The antenna port parameters are incorrect",
    // 选择参数错误
    0X02 : "Incorrect selection of parameters",
    // 写入参数错误
    0X03 : "Writing parameter error",
    // CPC校验错误
    0X04 : "CPC verification error",
    // 功率不足
    0X05 : "Underpowered",
    // 数据区溢出
    0X06 : "Data area overflow",
    // 数据区被锁定
    0X07 : "The data area is locked",
    // 访问密码错误
    0X08 : "Incorrect access password",
    // 其他标签错误
    0X09 : "Other label errors",
    // 标签丢失
    0X0A : "The label is lost",
    // 读写器发送指令错误
    0X0B : "The reader sent instructions incorrectly",
  };

}
import 'dart:async';

import 'package:flutter/material.dart';
import 'package:my_flutter_rfid_reader/my_flutter_rfid_reader_util.dart';

mixin MyFlutterRfidReaderMixin<T extends StatefulWidget> on State<T> {
  late StreamSubscription streamSubscription;
  final MyFlutterRfidReaderUtil util = MyFlutterRfidReaderUtil();

  @override
  void initState() {
    super.initState();
    util.flutterChannel.setMessageHandler(listenerAndroidHandle);
  }

  Future<void> listenerAndroidHandle(dynamic message);

}
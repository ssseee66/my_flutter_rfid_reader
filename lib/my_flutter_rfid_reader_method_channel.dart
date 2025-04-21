import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'my_flutter_rfid_reader_platform_interface.dart';

/// An implementation of [MyFlutterRfidReaderPlatform] that uses method channels.
class MethodChannelMyFlutterRfidReader extends MyFlutterRfidReaderPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('my_flutter_rfid_reader');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }
}

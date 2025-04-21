import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'my_flutter_rfid_reader_method_channel.dart';

abstract class MyFlutterRfidReaderPlatform extends PlatformInterface {
  /// Constructs a MyFlutterRfidReaderPlatform.
  MyFlutterRfidReaderPlatform() : super(token: _token);

  static final Object _token = Object();

  static MyFlutterRfidReaderPlatform _instance = MethodChannelMyFlutterRfidReader();

  /// The default instance of [MyFlutterRfidReaderPlatform] to use.
  ///
  /// Defaults to [MethodChannelMyFlutterRfidReader].
  static MyFlutterRfidReaderPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [MyFlutterRfidReaderPlatform] when
  /// they register themselves.
  static set instance(MyFlutterRfidReaderPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }
}

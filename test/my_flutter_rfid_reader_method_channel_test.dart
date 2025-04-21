import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:my_flutter_rfid_reader/my_flutter_rfid_reader_method_channel.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  MethodChannelMyFlutterRfidReader platform = MethodChannelMyFlutterRfidReader();
  const MethodChannel channel = MethodChannel('my_flutter_rfid_reader');

  setUp(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(
      channel,
      (MethodCall methodCall) async {
        return '42';
      },
    );
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(channel, null);
  });

  test('getPlatformVersion', () async {
    expect(await platform.getPlatformVersion(), '42');
  });
}

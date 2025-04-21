import 'package:flutter_test/flutter_test.dart';
import 'package:my_flutter_rfid_reader/my_flutter_rfid_reader.dart';
import 'package:my_flutter_rfid_reader/my_flutter_rfid_reader_platform_interface.dart';
import 'package:my_flutter_rfid_reader/my_flutter_rfid_reader_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockMyFlutterRfidReaderPlatform
    with MockPlatformInterfaceMixin
    implements MyFlutterRfidReaderPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final MyFlutterRfidReaderPlatform initialPlatform = MyFlutterRfidReaderPlatform.instance;

  test('$MethodChannelMyFlutterRfidReader is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelMyFlutterRfidReader>());
  });

  test('getPlatformVersion', () async {
    MyFlutterRfidReader myFlutterRfidReaderPlugin = MyFlutterRfidReader();
    MockMyFlutterRfidReaderPlatform fakePlatform = MockMyFlutterRfidReaderPlatform();
    MyFlutterRfidReaderPlatform.instance = fakePlatform;

    expect(await myFlutterRfidReaderPlugin.getPlatformVersion(), '42');
  });
}

package com.example.my_flutter_rfid_reader;

import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import com.gg.reader.api.dal.GClient;
import com.gg.reader.api.dal.HandlerDebugLog;
import com.gg.reader.api.protocol.gx.EnumG;
import com.gg.reader.api.protocol.gx.MsgAppGetReaderInfo;
import com.gg.reader.api.protocol.gx.MsgBaseInventoryEpc;
import com.gg.reader.api.protocol.gx.MsgBaseWriteEpc;
import com.gg.reader.api.utils.BitBuffer;
import com.gg.reader.api.utils.HexUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.BasicMessageChannel;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.StandardMessageCodec;

/** MyRfidReaderPlugin */
public class MyFlutterRfidReaderPlugin implements FlutterPlugin{
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private EventChannel eventChannel;
    // Flutter端与Android端通讯的通道名称
    private static final String FLUTTER_TO_ANDROID_CHANNEL = "flutter_rfid_android";
    // Flutter端与Android端通讯的通道
    private BasicMessageChannel<Object> flutter_channel;
    // Android端发送给Flutter端的消息Map
    private final Map<String, Object> message_map = new HashMap<>();

    // RFID的连接对象
    private final GClient client = new GClient();
    // RFID连接标志
    private boolean CONNECT_SUCCESS = false;
    // RFID上电标志
    private boolean POWER_ON = false;
    // EPC标签上报标志
    private boolean APPEAR_OVER = false;
    // 读写完毕标志
    private boolean READER_OVER = false;
    // Flutter端发送过来的消息Map
    private Map<String, Object> arguments;
    // 动作映射Map,将各个动作对应的方法的引用存放在Map中以减少if分支
    private final Map<String, Consumer<String>> action_map = new HashMap<>();
    // EPC标签数据列表
    private final List<String> epc_message = new LinkedList<>();

    // 应用上下文
    private Context applicationContext;
    // 广播接收器
    private BroadcastReceiver startScanBroadcastReceiver;
    // RFID广播动作
    private final String ACTION_SCAN = "com.rfid.SCAN_CMD";

    public MyFlutterRfidReaderPlugin() {
    }

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        Log.i("onAttachedToEngine", "onAttachedToEngine");
        applicationContext = flutterPluginBinding.getApplicationContext();

        flutter_channel = new BasicMessageChannel<>(
                flutterPluginBinding.getBinaryMessenger(),
                FLUTTER_TO_ANDROID_CHANNEL,
                StandardMessageCodec.INSTANCE
        );

        subscriberHandler();     // 订阅标签事件

        setAction_map();

        flutter_channel.setMessageHandler((message, reply) -> {
            arguments = castMap(message, String.class, Object.class);
            if (arguments == null) return;
            String key = getCurrentKey();
            Log.i("currentKey", key);
            Log.i("action", action_map.keySet().toString());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Objects.requireNonNull(action_map.get(key)).accept(key);
            }
        });
    }

    private String getCurrentKey() {
        String key = null;
        if (arguments.containsKey("startConnect"))                  key = "startConnect";
        else if (arguments.containsKey("closeConnect"))             key = "closeConnect";
        else if (arguments.containsKey("turnOnPower"))              key = "turnOnPower";
        else if (arguments.containsKey("turnOffPower"))             key = "turnOffPower";
        else if (arguments.containsKey("querySerialNumber"))        key = "querySerialNumber";
        else if (arguments.containsKey("startReader"))              key = "startReader";
        else if (arguments.containsKey("startReaderEpc"))           key = "startReaderEpc";
        else if (arguments.containsKey("readerOver"))               key = "readerOver";
        else if (arguments.containsKey("writeEpcData"))             key = "writeEpcData";
        else if (arguments.containsKey("startListenerBroadcast"))   key = "startListenerBroadcast";
        else if (arguments.containsKey("stopListenerBroadcast"))    key = "stopListenerBroadcast";
        return key;
    }
    private void setAction_map() {
        action_map.put("startConnect",             this :: startConnect);
        action_map.put("closeConnect",             this :: closeConnect);
        action_map.put("turnOnPower",              this :: turnOnPower);
        action_map.put("turnOffPower",             this :: turnOffPower);
        action_map.put("querySerialNumber",        this :: querySerialNumber);
        action_map.put("startReader",              this :: startReader);
        action_map.put("startReaderEpc",           this :: startReaderEpc);
        action_map.put("readerOver",               this :: readerOver);
        action_map.put("writeEpcData",             this :: writeEpcData);
        action_map.put("startListenerBroadcast",   this :: startListenerBroadcast);
        action_map.put("stopListenerBroadcast",    this :: stopListenerBroadcast);
    }
    private void startConnect(String key) {
        Object value = arguments.get(key);
        if (value == null) return;
        if (!(boolean) value) return;
        CONNECT_SUCCESS = client.openHdSerial("13:115200", 1000);
        // 连接中
        Log.i("connectInfo", "Connecting......");
        if (CONNECT_SUCCESS) {
            // 连接成功
            Log.i("connectInfo", "Successful connection");
            message_map.clear();
            message_map.put("message", "Successful connection");
            message_map.put("isSuccessful", true);
            message_map.put("operationCode", 0);
            flutter_channel.send(message_map);
        } else {
            // 连接失败，此设备不支持RFID
            Log.e("connectInfo", "Connection failed. This device does not support RFID");
            message_map.clear();
            message_map.put("message", "Connection failed. This device does not support RFID");
            message_map.put("isSuccessful", false);
            message_map.put("operationCode", 0);
            flutter_channel.send(message_map);
        }
    }
    private void closeConnect(String key) {
        Object value = arguments.get(key);
        if (value == null) return;
        if (!(boolean) value) return;
        client.close();
        CONNECT_SUCCESS = false;
        // 连接已关闭
        Log.i("connectInfo", "The connection has been closed.");
        message_map.clear();
        message_map.put("message", "The connection has been closed.");
        message_map.put("isSuccessful", true);
        message_map.put("operationCode", 1);
        flutter_channel.send(message_map);
    }
    private void turnOnPower(String key) {
        Object value = arguments.get(key);
        if (value == null) return;
        if (!(boolean) value) return;
        if (!CONNECT_SUCCESS) {
            // 上电失败，未建立连接
            Log.e("powerInfo", "Power-on failed. No connection was established. Please establish a connection first.");
            message_map.clear();
            message_map.put("message", "Power-on failed. No connection was established. Please establish a connection first");
            message_map.put("isSuccessful", false);
            message_map.put("operationCode", 2);
            flutter_channel.send(message_map);
            POWER_ON = false;
            return;
        }
        client.hdPowerOn();
        POWER_ON = true;
        // 上电成功
        Log.i("powerInfo", "Power-on successful");
        message_map.clear();
        message_map.put("message", "Power-on successful");
        message_map.put("isSuccessful", true);
        message_map.put("operationCode", 2);
        flutter_channel.send(message_map);
    }
    private void turnOffPower(String key) {
        Object value = arguments.get(key);
        if (value == null) return;
        if (!(boolean) value) return;
        if (CONNECT_SUCCESS) {
            client.hdPowerOff();
            POWER_ON = false;
            // 下电成功
            Log.i("powerInfo", "Power-off successful");
            message_map.clear();
            message_map.put("message", "Power-off successful");
            message_map.put("isSuccessful", true);
            message_map.put("operationCode", 3);
            flutter_channel.send(message_map);

            epc_message.clear();
        } else {
            // 下电失败
            Log.i("powerInfo", "Power-off failed");
            message_map.clear();
            message_map.put("message", "Power-off failed. No connection was established. Please establish a connection firs.");
            message_map.put("isSuccessful", false);
            message_map.put("operationCode", 3);
            flutter_channel.send(message_map);
        }
    }
    private void querySerialNumber(String key) {
        Object value = arguments.get(key);
        if (value == null) return;
        if (!(boolean) value) return;
        MsgAppGetReaderInfo msgAppGetReaderInfo = new MsgAppGetReaderInfo();
        client.sendSynMsg(msgAppGetReaderInfo);
        String serial_number;
        if (msgAppGetReaderInfo.getRtCode() == 0) {
            serial_number = msgAppGetReaderInfo.getReaderSerialNumber();
            // 设备流水号
            Log.e("serial_number", serial_number);
            message_map.clear();
            message_map.put("message", "Equipment serial number:" + serial_number);
            message_map.put("isSuccessful", true);
            message_map.put("operationCode", 4);
            flutter_channel.send(message_map);
        } else {
            Log.e("serial_number", msgAppGetReaderInfo.getRtCode() + "");
            message_map.clear();
            // 查询设备流水号失败
            message_map.put("message", "Failed to query the device serial number");
            message_map.put("isSuccessful", false);
            message_map.put("operationCode", 4);
            flutter_channel.send(message_map);
        }
    }
    private void startReader(String key) {
        Object value = arguments.get(key);
        if (value == null) return;
        if (!(boolean) value) return;
        if (!CONNECT_SUCCESS) {
            message_map.clear();
            // 未连接，请先进行连接并上电操作
            Log.e("readerOperationInfo", "It is not connected");
            message_map.put("message", "It is not connected. Please connect and power on first");
            message_map.put("isSuccessful", false);
            message_map.put("operationCode", 5);
            flutter_channel.send(message_map);
            return;
        }
        if (!POWER_ON) {
            message_map.clear();
            // 未上电，请先进行上电操作
            Log.e("readerOperationInfo", "It has not been powered on");
            message_map.put("message", "It has not been powered on. Please perform the power-on operation first");
            message_map.put("isSuccessful", false);
            message_map.put("operationCode", 5);
            flutter_channel.send(message_map);
            return;
        }
        MsgBaseInventoryEpc msgBaseInventoryEpc = new MsgBaseInventoryEpc();
        msgBaseInventoryEpc.setAntennaEnable(EnumG.AntennaNo_1);
        msgBaseInventoryEpc.setInventoryMode(EnumG.InventoryMode_Single);
        client.sendSynMsg(msgBaseInventoryEpc);
        boolean operationSuccess = false;
        if (0x00 == msgBaseInventoryEpc.getRtCode()) {
            // 读卡成功
            Log.i("readerOperationInfo", "Card reading was successful.");
            operationSuccess = true;
        } else {
            message_map.clear();
            message_map.put("message",
                    "Card reading was failed:" + msgBaseInventoryEpc.getRtCode() + msgBaseInventoryEpc.getRtMsg());
            message_map.put("isSuccessful", false);
            message_map.put("operationCode", 5);
            flutter_channel.send(message_map);
            Log.i("readerOperationInfo", "Card reading was failed");
        }
        // 搞不懂为什么要在外层进行通讯才行，在里面发送的话会发送不了
        // 并且通讯方法只能在主线程中调用，无法通过创建新线程处理
        if (operationSuccess) {
            Log.i("readerOperationInfo", "Card reading was successful.");
            message_map.clear();
            message_map.put("message", "Card reading was successful.");
            message_map.put("isSuccessful", true);
            message_map.put("operationCode", 5);
            flutter_channel.send(message_map);
        }
    }
    private void startReaderEpc(String key) {
        Object value = arguments.get(key);
        if (value == null) return;
        if (!(boolean) value) return;
        // 开始读取数据
        Log.i("readerEpcInfo", "Start reading the data");
        if (APPEAR_OVER) {
            Log.i("readerEpcInfo", "Reading the data ......");
            message_map.clear();
            // epc_message.add("OK");   // 当未读取到数据时，Flet端会收取不到信息，添加一条读取数据完成的标识
            message_map.put("message", epc_message);
            message_map.put("isSuccessful", true);
            message_map.put("operationCode", 6);
            flutter_channel.send(message_map);
            epc_message.clear();
            APPEAR_OVER = false;
        } else {
            message_map.clear();
            List<String> message_list = new LinkedList<>();
            // 未上报结束
            Log.e("readerEpcInfo", "Unfinished reporting");
            message_list.add("Unfinished reporting");
            message_map.put("message", message_list);
            message_map.put("isSuccessful", false);
            message_map.put("operationCode", 6);
            flutter_channel.send(message_map);
            Log.i("appear_over_not", "Unfinished reporting");
        }
    }
    private void writeEpcData(String key) {
        String write_epc_data = (String) arguments.get(key);
        if (write_epc_data == null) return;
        Log.i("writeEpcDataInfo", write_epc_data);
        String epc_data = write_epc_data.split("&")[0];
        int epc_data_area = Integer.parseInt(write_epc_data.split("&")[1]);
        MsgBaseWriteEpc msgBaseWriteEpc = new MsgBaseWriteEpc();
        // 0:保留区; 1:EPC 区; 2: TID 区; 3:用户数据区
        msgBaseWriteEpc.setArea(epc_data_area);
        msgBaseWriteEpc.setStart(1);     // 起始地址
        msgBaseWriteEpc.setAntennaEnable(EnumG.AntennaNo_1);
        if (epc_data != null) {
            int valueLen = getValueLen(epc_data);
            if (msgBaseWriteEpc.getArea() == 1 && msgBaseWriteEpc.getStart() == 1) {
                epc_data = getPc(valueLen) + padLeft(epc_data, valueLen * 4);
            }
            msgBaseWriteEpc.setHexWriteData(epc_data);
            Log.i("writeEpcDataInfo", epc_data);
            Log.i("writeEpcDataInfo", epc_data + Arrays.toString(epc_data.getBytes()));
        }
        client.sendSynMsg(msgBaseWriteEpc);
        byte code = msgBaseWriteEpc.getRtCode();
        setWriteMessage(code);
    }
    private void readerOver(String key) {
        Object value = arguments.get(key);
        if (value == null) return;
        if (!(boolean) value) return;
        READER_OVER = true;
    }
    private void startListenerBroadcast(String key) {
        Object value = arguments.get(key);
        if (value == null) return;
        if (!(boolean) value) return;
        startScanBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (READER_OVER) {
                    Log.i("startRfidBroadcast", "startScan");
                    message_map.clear();
                    message_map.put("message", "startScan");
                    message_map.put("isSuccessful", true);
                    message_map.put("operationCode", 10);
                    flutter_channel.send(message_map);
                    Log.i("startRfidBroadcast", message_map.toString());
                    READER_OVER = false;
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_SCAN);
        applicationContext.registerReceiver(
                startScanBroadcastReceiver, filter);
        READER_OVER = true;
        message_map.clear();
        // 已经注册广播
        Log.i("rfidBroadcastInfo", "Registered for broadcasting");
        message_map.put("message", "Registered for broadcasting:" + startScanBroadcastReceiver.toString());
        message_map.put("isSuccessful", true);
        message_map.put("operationCode", 8);
        flutter_channel.send(message_map);
        Log.i("startRfidBroadcast", message_map.toString());
    }
    private void stopListenerBroadcast(String key) {
        Object value = arguments.get(key);
        if (value == null) return;
        if (!(boolean) value) return;
        if (startScanBroadcastReceiver == null) {
            Log.i("rfidBroadcastInfo", "RFID broadcasting has not been registered yet");
            message_map.clear();
            message_map.put("message", "RFID broadcasting has not been registered yet");
            message_map.put("isSuccessful", false);
            message_map.put("operationCode", 9);
            flutter_channel.send(message_map);
            return;
        }
        applicationContext.unregisterReceiver(startScanBroadcastReceiver);
        message_map.clear();
        Log.i("rfidBroadcastInfo", "RFID broadcasting has been cancelled");
        message_map.put("message", "RFID broadcasting has been cancelled");
        message_map.put("isSuccessful", true);
        message_map.put("operationCode", 9);
        flutter_channel.send(message_map);
    }
    private void setWriteMessage(byte code) {
        String write_tag = "message";
        String write_code = "writerEpcMessageCode";
        String write_message;
        String log_write_tag = "writeEpcDataInfo";
        String log_write_message;
        switch (code) {
            case 0X00 :
                // 写入成功
                log_write_message = "Write successfully ==> " + code;
                write_message = "Write successfully";
                break;
            case 0X01 :
                // 天线端口参数错误
                log_write_message = "The antenna port parameters are incorrect ==> " + code;
                write_message = "The antenna port parameters are incorrect";
                break;
            case 0X02 :
                // 选择参数错误
                log_write_message = "Incorrect selection of parameters ==> " + code;
                write_message = "Incorrect selection of parameters";
                break;
            case 0X03 :
                // 写入参数错误
                log_write_message = "Writing parameter error ==> " + code;
                write_message = "Writing parameter error";
                break;
            case 0X04 :
                // CPC校验错误
                log_write_message = "CPC verification error ==> " + code;
                write_message = "CPC verification error";
                break;
            case 0X05 :
                // 功率不足
                log_write_message = "Underpowered ==> " + code;
                write_message = "Underpowered";
                break;
            case 0X06 :
                // 数据区溢出
                log_write_message = "Data area overflow ==> " + code;
                write_message = "Data area overflow";
                break;
            case 0X07 :
                // 数据区被锁定
                log_write_message = "The data area is locked ==> " + code;
                write_message = "The data area is locked";
                break;
            case 0X08 :
                // 访问密码错误
                log_write_message = "Incorrect access password ==> " + code;
                write_message = "Incorrect access password";
                break;
            case 0X09 :
                // 其他标签错误
                log_write_message = "Other label errors ==> " + code;
                write_message = "Other label errors";
                break;
            case 0X0A :
                // 标签丢失
                log_write_message = "The label is lost ==> " + code;
                write_message = "The label is lost";
                break;
            case 0X0B :
                // 读写器发送指令错误
                log_write_message = "The reader sent instructions incorrectly ==> " + code;
                write_message = "The reader sent instructions incorrectly";
                break;
            default :
                // 其他错误
                log_write_message = "Other error";
                write_message = "Other error";
                break;
        }
        Log.i(log_write_tag, log_write_message);
        message_map.clear();
        message_map.put(write_tag, write_message);
        message_map.put(write_code, code);
        message_map.put("isSuccessful", code == 0X00);
        message_map.put("operationCode", 7);
        flutter_channel.send(message_map);
    }
    @NonNull
    private String padLeft(@NonNull String src, int len) {
        int diff = len - src.length();
        if (diff <= 0) {
            return src;
        }

        char[] chars = new char[len];
        System.arraycopy(src.toCharArray(), 0, chars, 0, src.length());
        for (int i = src.length(); i < len; i++) {
            chars[i] = '0';
        }
        return new String(chars);
    }
    private int getValueLen(String data) {
        data = data.trim();
        return data.length() % 4 == 0 ? data.length() / 4
                : (data.length() / 4) + 1;
    }
    private String getPc(int pcLen) {
        int iPc = pcLen << 11;
        BitBuffer buffer = BitBuffer.allocateDynamic();
        buffer.put(iPc);
        buffer.position(16);
        byte[] bTmp = new byte[2];
        buffer.get(bTmp);
        return HexUtils.bytes2HexString(bTmp);
    }
    public static <K, V> Map<K, V> castMap(Object obj, Class<K> key, Class<V> value) {
        Map<K, V> map = new HashMap<>();
        if (obj instanceof Map<?, ?>) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) obj).entrySet()) {
                map.put(key.cast(entry.getKey()), value.cast(entry.getValue()));
            }
            return map;
        }
        return null;
    }
    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        eventChannel.setStreamHandler(null);
    }
    private void subscriberHandler() {
        client.onTagEpcLog = (s, logBaseEpcInfo) -> {
            if (logBaseEpcInfo.getResult() == 0) {
                Log.i("readerEPCInfo", logBaseEpcInfo.getEpc());
                epc_message.add(logBaseEpcInfo.getEpc());
            }
        };
        client.onTagEpcOver = (s, logBaseEpcOver) -> {
            Log.i("tagAppearOverInfo", logBaseEpcOver.getRtMsg());
            // send();
            Log.i("epcAppearOver", epc_message.toString());
            APPEAR_OVER = true;
        };

        client.debugLog = new HandlerDebugLog() {
            public void sendDebugLog(String msg) {
                Log.e("sendDebugInfo",msg);
            }
            public void receiveDebugLog(String msg) {
                Log.e("receiveDebugInfo",msg);
            }

            @Override
            public void crcErrorLog(String msg) {
                Log.i("crcErrorInfo", msg);
            }
        };
    }
}

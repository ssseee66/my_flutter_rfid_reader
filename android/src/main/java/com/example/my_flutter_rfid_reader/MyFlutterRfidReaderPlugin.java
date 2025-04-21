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
        Log.e("onAttachedToEngine", "onAttachedToEngine");
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
            Log.e("currentKey", key);
            Log.e("action", action_map.keySet().toString());
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
        Log.e("连接", "连接中。。。。。");
        if (CONNECT_SUCCESS) {
            Log.e("连接", "连接成功");
            message_map.clear();
            message_map.put("connectMessage", "连接成功");
            flutter_channel.send(message_map);
        } else {
            Log.e("连接", "连接失败，设备不支持RFID");
            message_map.clear();
            message_map.put("connectMessage", "连接失败");
            flutter_channel.send(message_map);
        }
    }
    private void closeConnect(String key) {
        Object value = arguments.get(key);
        if (value == null) return;
        if (!(boolean) value) return;
        client.close();
        CONNECT_SUCCESS = false;
        message_map.clear();
        message_map.put("connectMessage", "连接已关闭");
        flutter_channel.send(message_map);
    }
    private void turnOnPower(String key) {
        Object value = arguments.get(key);
        if (value == null) return;
        if (!(boolean) value) return;
        if (!CONNECT_SUCCESS) {
            Log.e("上电", "上电失败");
            message_map.clear();
            message_map.put("powerMessage", "上电失败，未建立连接，请先建立连接");
            flutter_channel.send(message_map);
            POWER_ON = false;
            return;
        }
        Log.e("上电", "上电成功");
        client.hdPowerOn();
        MsgAppGetReaderInfo msgAppGetReaderInfo = new MsgAppGetReaderInfo();
        client.sendSynMsg(msgAppGetReaderInfo);
        String serial_number;
        if (msgAppGetReaderInfo.getRtCode() == 0) {
            serial_number = msgAppGetReaderInfo.getReaderSerialNumber();
            Log.e("serial_number", serial_number);
            message_map.clear();
            message_map.put("powerMessage", "上电成功#" + serial_number);
            flutter_channel.send(message_map);
            POWER_ON = true;
        } else {
            Log.e("serial_number", msgAppGetReaderInfo.getRtCode() + "");
            message_map.clear();
            message_map.put("powerMessage", "未查询到流水号");
            flutter_channel.send(message_map);
        }
    }
    private void turnOffPower(String key) {
        Object value = arguments.get(key);
        if (value == null) return;
        if (!(boolean) value) return;
        if (CONNECT_SUCCESS) {
            Log.e("下电", "下电成功");
            client.hdPowerOff();
            message_map.clear();
            message_map.put("powerMessage", "下电成功");
            flutter_channel.send(message_map);
            POWER_ON = false;
            epc_message.clear();
        } else {
            Log.e("下电", "下电失败");
            message_map.clear();
            message_map.put("powerMessage", "下电失败，未建立连接，请先建立连接");
            flutter_channel.send(message_map);
        }
    }
    private void startReader(String key) {
        Object value = arguments.get(key);
        if (value == null) return;
        if (!(boolean) value) return;
        Log.e("connect_stat", CONNECT_SUCCESS + "");
        if (!CONNECT_SUCCESS) {
            message_map.clear();
            message_map.put("readerOperationMessage", "未连接，请先进行连接并上电操作");
            flutter_channel.send(message_map);
            return;
        }
        if (!POWER_ON) {
            message_map.clear();
            message_map.put("readerOperationMessage", "未上电，请先进行上电操作");
            flutter_channel.send(message_map);
            return;
        }
        MsgBaseInventoryEpc msgBaseInventoryEpc = new MsgBaseInventoryEpc();
        msgBaseInventoryEpc.setAntennaEnable(EnumG.AntennaNo_1);
        msgBaseInventoryEpc.setInventoryMode(EnumG.InventoryMode_Single);
        client.sendSynMsg(msgBaseInventoryEpc);
        boolean operationSuccess = false;
        if (0x00 == msgBaseInventoryEpc.getRtCode()) {
            // Log.e("读卡", "操作成功");
            Log.e("读卡", "操作成功");
            operationSuccess = true;
        } else {
            // Log.e("读卡", "操作失败");
            message_map.clear();
            message_map.put("readerOperationMessage",
                    "读卡操作失败：" + msgBaseInventoryEpc.getRtCode() + msgBaseInventoryEpc.getRtMsg());
            flutter_channel.send(message_map);
            Log.e("读卡", "操作失败");
        }
        // 搞不懂为什么要在外层进行通讯才行，在里面发送的话会发送不了
        // 并且通讯方法只能在主线程中调用，无法通过创建新线程处理
        if (operationSuccess) {
            Log.e("读卡操作", "读卡操作成功");
            message_map.clear();
            message_map.put("readerOperationMessage", "读卡操作成功");
            flutter_channel.send(message_map);
        }
    }
    private void startReaderEpc(String key) {
        Object value = arguments.get(key);
        if (value == null) return;
        if (!(boolean) value) return;
        Log.e("start_reader_epc", "开始读数据");
        if (APPEAR_OVER) {
            Log.e("start_reader_epc", "读数据");
            message_map.clear();
            epc_message.add("OK");   // 当未读取到数据时，Flet端会收取不到信息，添加一条读取数据完成的标识
            message_map.put("epcMessages", epc_message);
            flutter_channel.send(message_map);
            epc_message.clear();
            APPEAR_OVER = false;
        } else {
            message_map.clear();
            List<String> message_list = new LinkedList<>();
            message_list.add("未上报结束");
            message_map.put("epcMessages", message_list);
            flutter_channel.send(message_map);
            Log.e("appear_over_not", "未上报结束");
        }
    }
    private void writeEpcData(String key) {
        String write_epc_data = (String) arguments.get(key);
        if (write_epc_data == null) return;
        Log.e("write_epc_data", write_epc_data);
        String epc_data = write_epc_data.split("&")[0];
        int epc_data_area = Integer.parseInt(write_epc_data.split("&")[1]);
        MsgBaseWriteEpc msgBaseWriteEpc = new MsgBaseWriteEpc();
        msgBaseWriteEpc.setArea(epc_data_area);
        msgBaseWriteEpc.setStart(1);     // 起始地址
        msgBaseWriteEpc.setAntennaEnable(EnumG.AntennaNo_1);
        if (epc_data != null) {
            int valueLen = getValueLen(epc_data);
            if (msgBaseWriteEpc.getArea() == 1 && msgBaseWriteEpc.getStart() == 1) {
                epc_data = getPc(valueLen) + padLeft(epc_data, valueLen * 4);
            }
            msgBaseWriteEpc.setHexWriteData(epc_data);
            Log.e("writeEpcData", epc_data);
            Log.e("writeEpcData", epc_data + Arrays.toString(epc_data.getBytes()));
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
                    Log.e("startRfidBroadcast", "startScan");
                    message_map.clear();
                    message_map.put("rfidBroadcast", "startScan");
                    flutter_channel.send(message_map);
                    Log.e("startRfidBroadcast", message_map.toString());
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
        message_map.put("rfidBroadcast", "已经注册广播：" + startScanBroadcastReceiver.toString());
        flutter_channel.send(message_map);
        Log.e("startRfidBroadcast", message_map.toString());
    }
    private void stopListenerBroadcast(String key) {
        Object value = arguments.get(key);
        if (value == null) return;
        if (!(boolean) value) return;
        applicationContext.unregisterReceiver(startScanBroadcastReceiver);
        message_map.clear();
        message_map.put("rfidBroadcastMessage", "rfid广播已注销");
        flutter_channel.send(message_map);
    }
    private void setWriteMessage(byte code) {
        String write_tag = "writeEpcMessage";
        String write_message;
        String log_write_tag = "writeEpcData";
        String log_write_message;
        switch (code) {
            case 0X00:
                log_write_message = "写入成功";
                write_message = "写入成功" + code;
                break;
            case 0X01:
                log_write_message = "天线端口参数错误";
                write_message = "天线端口参数错误" + code;
                break;
            case 0X02:
                log_write_message = "选择参数错误";
                write_message = "选择参数错误" + code;
                break;
            case 0X03:
                log_write_message = "写入参数错误";
                write_message = "选择参数错误" + code;
                break;
            case 0X04:
                log_write_message = "CPC校验错误";
                write_message = "CPC校验错误" + code;
                break;
            case 0X05:
                log_write_message = "功率不足";
                write_message = "功率不足" + code;
                break;
            case 0X06:
                log_write_message = "数据区溢出";
                write_message = "数据区溢出" + code;
                break;
            case 0X07:
                log_write_message = "数据区被锁定";
                write_message = "数据区被锁定" + code;
                break;
            case 0X08:
                log_write_message = "访问密码错误";
                write_message = "访问密码错误" + code;
                break;
            case 0X09:
                log_write_message = "其他标签错误";
                write_message = "其他标签错误" + code;
                break;
            case 0X0A:
                log_write_message = "标签丢失";
                write_message = "标签丢失" + code;
                break;
            case 0X0B:
                log_write_message = "读写器发送指令错误";
                write_message = "读写器发送指令错误" + code;
                break;
            default:
                log_write_message = "其他错误";
                write_message = "其他错误";
                break;
        }
        Log.e(log_write_tag, log_write_message);
        message_map.clear();
        message_map.put(write_tag, write_message);
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
                Log.e("readerEPC", logBaseEpcInfo.getEpc());
                epc_message.add(logBaseEpcInfo.getEpc());
            }
        };
        client.onTagEpcOver = (s, logBaseEpcOver) -> {
            Log.e("HandlerTagEpcOver", logBaseEpcOver.getRtMsg());
            // send();
            Log.e("epcAppearOver", epc_message.toString());
            APPEAR_OVER = true;
        };

        client.debugLog = new HandlerDebugLog() {
            public void sendDebugLog(String msg) {
                Log.e("sendDebugLog",msg);
            }
            public void receiveDebugLog(String msg) {
                Log.e("receiveDebugLog",msg);
            }

            @Override
            public void crcErrorLog(String msg) {
                Log.e("crcErrorLog", msg);
            }
        };
    }
}

package com.example.PosPrintPlugin;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.szsicod.print.escpos.PrinterAPI;
import com.szsicod.print.io.InterfaceAPI;
import com.szsicod.print.io.USBAPI;
import com.szsicod.print.utils.BitmapUtils;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

/**
 * PosPrintPlugin
 */
public class PosPrintPlugin implements FlutterPlugin, MethodCallHandler {

    private static final String TAG = "MainActivity";
    private static final int DISCONNECT = -5;           // 断开连接
    private static final int NO_CONNECT = -6;            // 未连接
    private static final int TOAST_CODE = 10001;        // 吐司弹出
    private static final int REQUEST_CODE_INTENT = 10002;

    private static final String PRINT_TEXT = "print_text";
    private static final String PRINT_QRCODE = "print_qrcode";
    private static final String PRINT_BREAK_LINE = "print_break_line";
    private static final String PRINT_IMAGE = "print_image";
    private static final String PRINT_THREE_COLUMN = "print_three_column";

    private MethodChannel channel;
    private UsbBroadCastReceiver mUsbBroadCastReceiver;
    private PrinterAPI mPrinter;
    private Runnable runnable;
    private Context context;

    static {
        //如果使用UsbNativeAPI 必须加载 适用root板 这个不需要权限窗口和申请权限
        System.loadLibrary("usb1.0");
        //串口
        System.loadLibrary("serial_icod");
        //图片
        System.loadLibrary("image_icod");
    }

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger()
                , "PosPrintPlugin");
        channel.setMethodCallHandler(this);
        context = flutterPluginBinding.getApplicationContext();
        initData();
    }

    private void initData() {
        mPrinter = PrinterAPI.getInstance();
        initPermission();
    }

    private static class UsbBroadCastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            UsbDevice device =
                    (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            switch (intent.getAction()) {
                case UsbManager.ACTION_USB_DEVICE_ATTACHED:
                    //connected;
                    break;
                case UsbManager.ACTION_USB_DEVICE_DETACHED:
                    //unconnected
                    break;
            }
        }
    }

    private final Handler handler = new Handler() {
        @SuppressLint("HandlerLeak")
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case PrinterAPI.SUCCESS:
                    Log.d(TAG, "handleMessage: onnected " + msg);
                    break;
                case PrinterAPI.FAIL:
                case PrinterAPI.ERR_PARAM:
                    Log.d(TAG, "handleMessage: unconnected " + msg);
                    break;
                case DISCONNECT:
                    Log.d(TAG, "handleMessage: disconnected " + msg);
                    break;
                case NO_CONNECT:
                    Log.d(TAG, "handleMessage: no connected " + msg);
                    break;
                case TOAST_CODE:
                    Log.d(TAG, "handleMessage: " + msg);
                    break;
            }
        }
    };

    private void initPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            //检查是否已经给了权限
            int checkpermission =
                    ContextCompat.checkSelfPermission(context
                            , Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (checkpermission != PackageManager.PERMISSION_GRANTED) {//没有给权限
                //参数分别是当前活动，权限字符串数组，requestcode
                ActivityCompat.requestPermissions((Activity) context,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        }
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        switch (call.method) {
            case "getPlatformVersion":
                result.success("Android " + Build.VERSION.RELEASE);
                break;
            case "connect_print":
                connectPrint(result);
                break;
            case PRINT_TEXT:
                String text = call.argument("text");
                printText(text, result);
                break;
            case PRINT_QRCODE:
                String content = call.argument("content");
                int modeSize = call.argument("modeSize");
                printQRCode(result, content, modeSize);
                break;
            case PRINT_BREAK_LINE:
                mPrinter.cutPaper(66, 0);
                break;
            case PRINT_IMAGE:
                byte[] bytes = call.argument("data");
                printImage(result, bytes);
                break;
            case PRINT_THREE_COLUMN:
                printThreeColumn("Tên", "Số lượng", "Thành tiền");
                printThreeColumn("Cà phê ông bầu", "2", "32.000");
                printThreeColumn("Cà phê đen", "1", "16.000");
                printThreeColumn("Cà phê ông bầu rang xay mộc",
                        "2",
                        "32.000");
                printThreeColumn("Cà phê ông bầu rang xay mộc túi 250g",
                        "2",
                        "32.000");
                printThreeColumn("Cà phê Express ông bầu rang xay mộc túi 250g",
                        "2",
                        "32.000");
                printThreeColumn("Cà phê đen", "1", "16.000");
                break;
        }
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
    }

    void connectPrint(final Result result) {
        try {
            runnable = new Runnable() {
                @Override
                public void run() {
                    if (mPrinter.isConnect()) {
                        mPrinter.pageRemoveAllData();
                        mPrinter.disconnect();
                        handler.obtainMessage(DISCONNECT).sendToTarget();
                    }
                    InterfaceAPI io = new USBAPI(context);
                    handler.obtainMessage(mPrinter.connect(io)).sendToTarget();
                }
            };
            new Thread(runnable).start();
            result.success(true);
        } catch (Exception e) {
            Log.d(TAG, "connectPrint: " + e);
            result.success(false);
        }
    }

    void printText(final String value, final Result result) {
        try {
            runnable = new Runnable() {
                @Override
                public void run() {
                    if (mPrinter.isConnect()) {
                        try {
                            mPrinter.setAlignMode(0);
                            mPrinter.printString("Tên");
                            mPrinter.printFeed();
                            mPrinter.setAlignMode(1);
                            mPrinter.printString("Số lượng");
                            mPrinter.printFeed();
                            mPrinter.setAlignMode(2);
                            mPrinter.printString("Thành tiền");
                            mPrinter.setAlignMode(1);
                            int ret = mPrinter.printString(value,
                                    String.valueOf("utf-8"),
                                    true);
                            handler.obtainMessage(TOAST_CODE,
                                    ret == PrinterAPI.SUCCESS ?
                                            "print success"
                                            : "print failure").sendToTarget();

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        handler.obtainMessage(NO_CONNECT).sendToTarget();
                    }
                }
            };
            new Thread(runnable).start();
            result.success(true);
        } catch (Exception e) {
            result.success(false);
        }
    }

    void printThreeColumn(String value1, String value2, String value3) {
        int value1Length = value1.length();
        int value2Length = value2.length();
        int value3Length = value3.length();
        if (value1Length > 20) {
            int maxIndex = 15;
            String frsValue = "";
            if (value1.charAt(maxIndex) == ' ') {
                frsValue = value1.substring(0, 15);

            } else {
                while (maxIndex > 0) {
                    if (value1.charAt(maxIndex) == ' ') {
                        break;
                    }
                    maxIndex = maxIndex - 1;
                }
                frsValue = value1.substring(0, maxIndex);
            }
            final String finalFrsValue = frsValue;
            runnable = new Runnable() {
                @Override
                public void run() {
                    if (mPrinter.isConnect()) {
                        try {
                            int ret = mPrinter.printString(finalFrsValue,
                                    "utf-8",
                                    true);
                            handler.obtainMessage(TOAST_CODE,
                                    ret == PrinterAPI.SUCCESS ?
                                            "print success"
                                            : "print failure").sendToTarget();

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        handler.obtainMessage(NO_CONNECT).sendToTarget();
                    }
                }
            };
            new Thread(runnable).start();
            value1 = value1.substring(finalFrsValue.length()).trim();
            printThreeColumn(value1, value2, value3);
        } else {
            StringBuilder value1Builder = new StringBuilder(value1);
            while (value1Length < 20) {
                value1Builder.append(' ');
                value1Length++;
            }
            value1 = value1Builder.toString();
            StringBuilder value2Builder = new StringBuilder(value2);
            int totalChar = 10 - value2Length;
            int firstChar = totalChar / 2;
            int index = 0;
            while (index < firstChar) {
                value2Builder.append(" ");
                value2Length++;
                index++;
            }
            while (value2Length < 10) {
                value2Builder.append(' ');
                value2Length++;
            }
            value2 = value2Builder.toString();
            StringBuilder value3Builder = new StringBuilder(value3);
            while (value3Length < 10) {
                value3Builder.insert(0, ' ');
                value3Length++;
            }
            value3 = value3Builder.toString();
            final String str = value1 + value2 +
                    value3;
            runnable = new Runnable() {
                @Override
                public void run() {
                    if (mPrinter.isConnect()) {
                        try {
                            int ret = mPrinter.printString(str, "utf-8", true);
                            handler.obtainMessage(TOAST_CODE,
                                    ret == PrinterAPI.SUCCESS ?
                                            "print success"
                                            : "print failure").sendToTarget();

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        handler.obtainMessage(NO_CONNECT).sendToTarget();
                    }
                }
            };
            new Thread(runnable).start();
        }
    }

    void printQRCode(final Result result, final String content,
                     final int modeSize) {
        try {
            runnable = new Runnable() {
                @Override
                public void run() {
                    if (mPrinter.isConnect()) {
                        mPrinter.printQRCode(content, 6,
                                false);
                        int ret = mPrinter.printFeed();
                    } else {
                        handler.obtainMessage(NO_CONNECT).sendToTarget();
                    }
                }
            };
            new Thread(runnable).start();
            result.success(true);
        } catch (Exception e) {
            result.success(false);
        }
    }

    void printImage(Result result, byte[] bytes) {
        final Bitmap[] bitmap = {BitmapFactory.decodeByteArray(bytes, 0,
                bytes.length)};
        runnable = new Runnable() {
            @Override
            public void run() {
                if (mPrinter.isConnect()) {
                    try {
                        mPrinter.init();
                        int printWidth = 20 * 8;
                        bitmap[0] = BitmapUtils.reSize(bitmap[0], printWidth,
                                (printWidth * bitmap[0].getHeight() / bitmap[0].getWidth()));
                        mPrinter.printRasterBitmap(bitmap[0]);
                        int ret = mPrinter.printFeed();
                        handler.obtainMessage(TOAST_CODE,
                                ret == PrinterAPI.SUCCESS ?
                                        "Success"
                                        : "Failure").sendToTarget();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } else {
                    handler.obtainMessage(NO_CONNECT).sendToTarget();
                }
            }
        };
        result.success(true);
        new Thread(runnable).start();
    }
}

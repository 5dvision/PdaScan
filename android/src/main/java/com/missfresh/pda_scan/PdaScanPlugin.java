package com.missfresh.pda_scan;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.device.ScanManager;
import android.device.scanner.configuration.PropertyID;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import io.flutter.BuildConfig;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;


/**
 * PdaScanPlugin
 */
public class PdaScanPlugin implements FlutterPlugin, EventChannel.StreamHandler {

    private Context applicationContext;
    private BroadcastReceiver scanResultReceiver;
    private EventChannel eventChannel;
    private ScanManager mScanManager;
    private Vibrator mVibrator;
    private SoundPool soundpool;
    private int soundId;

    @Override
    public void onAttachedToEngine(FlutterPluginBinding binding) {
        onAttachedToEngine(binding.getApplicationContext(), binding.getBinaryMessenger());
    }

    private void onAttachedToEngine(Context applicationContext, BinaryMessenger messenger) {
        this.applicationContext = applicationContext;
        eventChannel = new EventChannel(messenger, "plugins.flutter.io/missfresh.scan");
        eventChannel.setStreamHandler(this);
    }


    public static void registerWith(Registrar registrar) {
        final PdaScanPlugin instance = new PdaScanPlugin();
        instance.onAttachedToEngine(registrar.context(), registrar.messenger());
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        applicationContext = null;
        eventChannel.setStreamHandler(null);
        eventChannel = null;
    }


    private void initScanManager(Context context) throws Exception {
        mScanManager = new ScanManager();
        mScanManager.openScanner();
        mScanManager.switchOutputMode(0);
        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        soundpool = new SoundPool(1, AudioManager.STREAM_NOTIFICATION, 100);
        soundId = soundpool.load("/etc/Scan_new.ogg", 1);
    }

    private BroadcastReceiver createScanStateChangeReceiver(final EventChannel.EventSink events) {
        return new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                soundpool.play(soundId, 1, 1, 0, 0, 1);
                //回调播放声音和抖动
                mVibrator.vibrate(100);
                byte[] barcode = intent.getByteArrayExtra(ScanManager.DECODE_DATA_TAG);
                int barcodelen = intent.getIntExtra(ScanManager.BARCODE_LENGTH_TAG, 0);
                String scanString = new String(barcode, 0, barcodelen);
                String code = DecodeUtils.getSvStr(scanString.trim());
                events.success(code);
            }
        };
    }

    @Override
    public void onListen(Object arguments, EventChannel.EventSink events) {
        try {
            initScanManager(applicationContext);
        } catch (Exception e) {
            events.error("996", "Device does not support scanning", null);
        }
        scanResultReceiver = createScanStateChangeReceiver(events);
        applicationContext.registerReceiver(
                scanResultReceiver, getIntentFilter());
    }

    private IntentFilter getIntentFilter() {
        IntentFilter filter = new IntentFilter();
        int[] buffer = new int[]{PropertyID.WEDGE_INTENT_ACTION_NAME, PropertyID.WEDGE_INTENT_DATA_STRING_TAG};
        String[] value_buf = mScanManager.getParameterString(buffer);
        if (value_buf != null && value_buf[0] != null && !value_buf[0].equals("")) {
            filter.addAction(value_buf[0]);
        } else {
            filter.addAction(ScanManager.ACTION_DECODE);
        }
        return filter;
    }

    @Override
    public void onCancel(Object arguments) {
        applicationContext.unregisterReceiver(scanResultReceiver);
        scanResultReceiver = null;
        if (mScanManager != null) {
            mScanManager.closeScanner();
            mScanManager.switchOutputMode(1);
        }

    }
}

package com.missfresh.pda_scan;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.device.ScanManager;
import android.device.scanner.configuration.PropertyID;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.uuzuche.lib_zxing.activity.CodeUtils;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.Registrar;

import static com.uuzuche.lib_zxing.activity.CodeUtils.RESULT_TYPE;
import static com.uuzuche.lib_zxing.activity.CodeUtils.RESULT_SUCCESS;


/**
 * PdaScanPlugin
 */
public class PdaScanPlugin implements FlutterPlugin,EventChannel.StreamHandler,MethodChannel.MethodCallHandler , PluginRegistry.ActivityResultListener, ActivityAware {

    private Context applicationContext;
    private BroadcastReceiver scanResultReceiver;
    private EventChannel eventChannel;
    private ScanManager mScanManager;
    private Vibrator mVibrator;
    private SoundPool soundpool;
    private int soundId;
    private MethodChannel methChannel;
    private Result scanResultByPhone;
    private Activity activity;
    private int REQUEST_CODE = 100;
    public static final String TAG = PdaScanPlugin.class.getSimpleName();

    @Override
    public void onAttachedToEngine(FlutterPluginBinding binding) {
        onAttachedToEngine(binding.getApplicationContext(), binding.getBinaryMessenger());
    }

    private void onAttachedToEngine(Context applicationContext, BinaryMessenger messenger) {
        this.applicationContext = applicationContext;
        eventChannel = new EventChannel(messenger, "plugins.flutter.io/missfresh.scan");
        methChannel = new MethodChannel(messenger,"plugins.flutter.io/missfresh.scan.device");
        eventChannel.setStreamHandler(this);
        methChannel.setMethodCallHandler(this);
    }


    public static void registerWith(Registrar registrar) {
        final PdaScanPlugin instance = new PdaScanPlugin();
        instance.onAttachedToEngine(registrar.context(), registrar.messenger());
        registrar.addActivityResultListener(instance);
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

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        this.scanResultByPhone = result;
        if (call.method.equals("scan")){
            CheckPermissionUtils.initPermission(activity);
            showBarcodeView();
        }
        else if (call.method.equals("isPDA")){
            if ("qcom".equals(Build.BOARD)) {
                scanResultByPhone.success(true);
            }else{
                scanResultByPhone.success(false);
            }
        }
    }

    private void showBarcodeView() {
        Intent intent = new Intent(activity, ScanViewActivity.class);
        activity.startActivityForResult(intent, REQUEST_CODE);
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && intent != null) {
                Bundle secondBundle = intent.getBundleExtra("secondBundle");
                if (secondBundle != null) {
                    try {
                        CodeUtils.AnalyzeCallback analyzeCallback = new CustomAnalyzeCallback(this.scanResultByPhone, intent);
                        CodeUtils.analyzeBitmap(secondBundle.getString("path"), analyzeCallback);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    Bundle bundle = intent.getExtras();
                    if (bundle != null) {
                        if (bundle.getInt(RESULT_TYPE) == RESULT_SUCCESS) {
                            String barcode = bundle.getString(CodeUtils.RESULT_STRING);
                            if (scanResultByPhone!=null && !TextUtils.isEmpty(barcode)) {
                                this.scanResultByPhone.success(barcode);
                            }
                        }else{
                            this.scanResultByPhone.success(null);
                        }
                    }
                }
            } else {
                String errorCode = intent != null ? intent.getStringExtra("ERROR_CODE") : null;
                if (errorCode != null) {
                    this.scanResultByPhone.error(errorCode, null, null);
                }
            }
        }
        return true;
    }

    @Override
    public void onAttachedToActivity(ActivityPluginBinding binding) {
        binding.addActivityResultListener(this);
        this.activity = binding.getActivity();

    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {

    }

    @Override
    public void onReattachedToActivityForConfigChanges(ActivityPluginBinding binding) {
        binding.removeActivityResultListener(this);
    }

    @Override
    public void onDetachedFromActivity() {

    }
}

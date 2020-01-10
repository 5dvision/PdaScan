package com.missfresh.pda_scan;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.uuzuche.lib_zxing.activity.CaptureFragment;
import com.uuzuche.lib_zxing.activity.CodeUtils;

public class ScanViewActivity extends AppCompatActivity {

    private static final String TAG = ScanViewActivity.class.getSimpleName();

    public static boolean isLightOpen = false;
    private int REQUEST_IMAGE = 101;
    private LinearLayout lightLayout;
    private LinearLayout backLayout;
    private LinearLayout photoLayout;
    private SensorManager sensorManager;
    private Sensor lightSensor;
    private SensorEventListener sensorEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_view);
        CaptureFragment captureFragment = new CaptureFragment();
        CodeUtils.setFragmentArgs(captureFragment, R.layout.my_camera);
        captureFragment.setAnalyzeCallback(analyzeCallback);
        getSupportFragmentManager().beginTransaction().replace(R.id.fl_my_container, captureFragment).commit();

        lightLayout = findViewById(R.id.scan_light);
        backLayout = findViewById(R.id.scan_back);
        photoLayout = findViewById(R.id.choose_photo);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        sensorEventListener = new LightSensorEventListener(lightLayout);

        initView();
    }

    @Override
    protected void onResume() {
        // System.out.println("---------------------|||||||||||||---onResume---|||||||||||-------------------------");
        super.onResume();
        if (lightSensor != null) {
            sensorManager.registerListener(sensorEventListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        // System.out.println("---------------------|||||||||||||---onPause---|||||||||||-------------------------");
        sensorManager.unregisterListener(sensorEventListener);
        super.onPause();
    }

    private void initView() {
        lightLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isLightOpen) {
                    CodeUtils.isLightEnable(true);
                    isLightOpen = true;
                } else {
                    CodeUtils.isLightEnable(false);
                    isLightOpen = false;
                }
            }
        });
        backLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ScanViewActivity.this.finish();
            }
        });
        photoLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_PICK);
                intent.setType("image/*");
                ScanViewActivity.this.startActivityForResult(intent, REQUEST_IMAGE);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE) {
            if (data != null) {
                Uri uri = data.getData();
                String path = ImageUtil.getImageAbsolutePath(ScanViewActivity.this, uri);
                Intent intent = new Intent();
                intent.setClass(ScanViewActivity.this, PdaScanPlugin.class);
                Bundle bundle = new Bundle();
                bundle.putString("path", path);
                intent.putExtra("secondBundle", bundle);
                setResult(Activity.RESULT_OK, intent);
                ScanViewActivity.this.finish();
            }
        }
    }

    @Override
    public void onBackPressed() {
        Intent resultIntent = new Intent();
        Bundle bundle = new Bundle();
        bundle.putInt(CodeUtils.RESULT_TYPE, CodeUtils.RESULT_FAILED);
        resultIntent.putExtras(bundle);
        ScanViewActivity.this.setResult(RESULT_OK, resultIntent);
        ScanViewActivity.this.finish();
    }

    private CodeUtils.AnalyzeCallback analyzeCallback = new CodeUtils.AnalyzeCallback() {
        @Override
        public void onAnalyzeSuccess(Bitmap mBitmap, String result) {
            Intent resultIntent = new Intent();
            Bundle bundle = new Bundle();
            bundle.putInt(CodeUtils.RESULT_TYPE, CodeUtils.RESULT_SUCCESS);
            bundle.putString(CodeUtils.RESULT_STRING, result);
            resultIntent.putExtras(bundle);
            ScanViewActivity.this.setResult(RESULT_OK, resultIntent);
            ScanViewActivity.this.finish();
        }

        @Override
        public void onAnalyzeFailed() {
            Intent resultIntent = new Intent();
            Bundle bundle = new Bundle();
            bundle.putInt(CodeUtils.RESULT_TYPE, CodeUtils.RESULT_FAILED);
            bundle.putString(CodeUtils.RESULT_STRING, "");
            resultIntent.putExtras(bundle);
            ScanViewActivity.this.setResult(RESULT_OK, resultIntent);
            ScanViewActivity.this.finish();
        }
    };
}
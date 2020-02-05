package eit.fourspace.stufftracker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LifecycleOwner;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;



public class MainActivity extends AppCompatActivity implements LifecycleOwner {
    private static final int REQUEST_CODE_PERM = 11;
    private static final String[] REQUIRED_PERMS = {Manifest.permission.CAMERA, Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE};

    private PreviewView cameraView;
    private OverlayDrawable canvas;
    private ProcessCameraProvider provider;

    private SensorHandler sensorFragment;
    private DataManager dataFragment;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        cameraView = findViewById(R.id.camera_view);
        cameraView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        if (allPermissionsGranted()) {
            startActivityProcesses();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMS, REQUEST_CODE_PERM);
        }


        canvas = new OverlayDrawable();
        ImageView image = findViewById(R.id.canvas_view);
        image.setImageDrawable(canvas);
        image.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        image.setOnSystemUiVisibilityChangeListener
                (visibility -> {
                    // If we're no longer in fullscreen, wait three seconds, then re-enter fullscreen.
                    // Trust me it looks real neat.
                    if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                        Log.println(Log.VERBOSE, "MainActivity", "Exit fullscreen!");
                        Handler handler = new Handler();
                        handler.postDelayed(() -> {
                            cameraView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
                            image.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
                        }, 3000);
                    }
                });

    }

    private void startActivityProcesses() {
        cameraView.post(this::startCamera);
        sensorFragment = new SensorHandler();
        dataFragment = new DataManager();
        getSupportFragmentManager().beginTransaction()
                .add(sensorFragment, "sensorFragment")
                .add(dataFragment, "dataFragment")
                .commit();
    }


    private void startCamera() {
        DisplayMetrics metrics = new DisplayMetrics();
        cameraView.getDisplay().getRealMetrics(metrics);

        Integer screenAspectRatio = aspectRatio(metrics.heightPixels, metrics.widthPixels);

        int rotation = cameraView.getDisplay().getRotation();
        LifecycleOwner self = this;
        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
        ListenableFuture<ProcessCameraProvider> providerFuture = ProcessCameraProvider.getInstance(getBaseContext());
        providerFuture.addListener(() -> {
                Preview preview = new Preview.Builder()
                        .setTargetAspectRatio(screenAspectRatio)
                        .setTargetRotation(rotation)
                        .build();
                preview.setPreviewSurfaceProvider(cameraView.getPreviewSurfaceProvider());
                try {
                    provider = providerFuture.get();
                }
                catch (Exception ex) {
                    return;
                }
                provider.unbindAll();
                provider.bindToLifecycle(self, cameraSelector, preview);
        }, ContextCompat.getMainExecutor(getBaseContext()));
    }

    private Integer aspectRatio(int height, int width) {
        double previewRatio = (double)Math.max(width, height) / Math.min(width, height);
        if (Math.abs(previewRatio - 4.0/3.0) <= Math.abs(previewRatio - 16.0/9.0)) {
            return AspectRatio.RATIO_4_3;
        }
        return AspectRatio.RATIO_16_9;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_PERM) {
            if (allPermissionsGranted()) {
                startActivityProcesses();
            } else {
                Toast.makeText(this,"Permissions not granted by user", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
    private boolean allPermissionsGranted() {
        for (String perm : REQUIRED_PERMS) {
            Log.w("Main", perm + ", " + String.valueOf(ContextCompat.checkSelfPermission(getBaseContext(), perm)));
            if (ContextCompat.checkSelfPermission(getBaseContext(), perm) != PackageManager.PERMISSION_GRANTED) return false;
        }
        return true;
    }
}

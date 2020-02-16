package eit.fourspace.stufftracker;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;

import androidx.annotation.NonNull;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.navigation.Navigation;
import eit.fourspace.stufftracker.calculationflow.DataManager;
import eit.fourspace.stufftracker.calculationflow.ItemRenderer;
import eit.fourspace.stufftracker.calculationflow.LocationManager;
import eit.fourspace.stufftracker.calculationflow.RotationSensorManager;
import eit.fourspace.stufftracker.calculationflow.TLEManager;


public class CameraFragment extends Fragment {
    private PreviewView cameraView;
    private OverlayDrawable canvas;
    private ProcessCameraProvider provider;
    private DataManager dataManager;
    private TLEManager tleManager;
    private ItemRenderer itemRenderer;

    private RotationSensorManager sensorManager;
    private LocationManager locationManager;

    private Handler asyncMessageHandler;

    private static final String TAG = "CameraFragment";

    private Executor mainExecutor;
    @Override
    public void onResume() {
        super.onResume();
        // Make sure that all permissions are still present, since the
        // user could have removed them while the app was in paused state.
        if (!PermissionsFragment.allPermissionsGranted(requireContext())) {
            Navigation.findNavController(requireActivity(), R.id.nav_container).navigate(
                    CameraFragmentDirections.actionCameraFragmentToPermissionsFragment()
            );
            return;
        }
        sensorManager.onResume();
        tleManager.onResume();
        locationManager.onResume();
        itemRenderer.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        sensorManager.onPause();
        tleManager.onPause();
        locationManager.onPause();
        itemRenderer.onPause();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainExecutor = ContextCompat.getMainExecutor(requireContext());
        sensorManager = new RotationSensorManager(requireContext());
        Context context = requireContext();
        asyncMessageHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message message) {
                switch(message.what) {
                    case DataManager.TLE_DATA_NOT_AVAILABLE:
                        Toast.makeText(context, "Unable to retrieve TLE data", Toast.LENGTH_LONG).show();
                        break;
                    case DataManager.TLE_DATA_READY:
                        Toast.makeText(context, "TLE data ready to use", Toast.LENGTH_LONG).show();
                        break;
                    default:
                        super.handleMessage(message);
                }
            }
        };

        dataManager = new DataManager(requireContext(), asyncMessageHandler);
        locationManager = new LocationManager(requireContext());
        tleManager = new TLEManager(requireContext(), dataManager, locationManager);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.camera_fragment, container, false);
        itemRenderer = new ItemRenderer(requireContext(), tleManager, sensorManager, view.findViewById(R.id.canvas_view));
        return view;
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration config) {
        super.onConfigurationChanged(config);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ConstraintLayout container = (ConstraintLayout)view;

        canvas = new OverlayDrawable(itemRenderer);
        ImageView image = container.findViewById(R.id.canvas_view);
        image.setImageDrawable(canvas);

        cameraView = container.findViewById(R.id.camera_view);
        cameraView.post(this::startCamera);
    }

    private void startCamera() {
        DisplayMetrics metrics = new DisplayMetrics();
        cameraView.getDisplay().getRealMetrics(metrics);

        Integer screenAspectRatio = aspectRatio(metrics.heightPixels, metrics.widthPixels);

        int rotation = cameraView.getDisplay().getRotation();
        LifecycleOwner self = this;
        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
        ListenableFuture<ProcessCameraProvider> providerFuture = ProcessCameraProvider.getInstance(requireContext());
        providerFuture.addListener(() -> {
            Preview preview = new Preview.Builder()
                    .setTargetAspectRatio(screenAspectRatio)
                    .setTargetRotation(rotation)
                    .build();
            preview.setSurfaceProvider(cameraView.getPreviewSurfaceProvider());

            try {
                provider = providerFuture.get();
            }
            catch (Exception ex) {
                return;
            }
            provider.unbindAll();
            provider.bindToLifecycle(self, cameraSelector, preview);
        }, mainExecutor);
    }

    private Integer aspectRatio(int height, int width) {
        double previewRatio = (double)Math.max(width, height) / Math.min(width, height);
        if (Math.abs(previewRatio - 4.0/3.0) <= Math.abs(previewRatio - 16.0/9.0)) {
            return AspectRatio.RATIO_4_3;
        }
        return AspectRatio.RATIO_16_9;
    }


}

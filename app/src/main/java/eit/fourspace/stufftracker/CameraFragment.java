package eit.fourspace.stufftracker;
import android.app.Service;
import android.content.Context;
import android.content.res.Configuration;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.util.SizeF;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.Switch;
import android.widget.TableLayout;
import android.widget.TextView;

import com.google.common.util.concurrent.ListenableFuture;

import net.steamcrafted.materialiconlib.MaterialIconView;

import java.util.Locale;
import java.util.concurrent.Executor;

import androidx.annotation.NonNull;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import eit.fourspace.stufftracker.calculationflow.DataManager;
import eit.fourspace.stufftracker.calculationflow.ItemRenderer;
import eit.fourspace.stufftracker.calculationflow.LocationManager;
import eit.fourspace.stufftracker.calculationflow.ObjectDataModel;
import eit.fourspace.stufftracker.calculationflow.ObjectWrapper;
import eit.fourspace.stufftracker.calculationflow.RotationSensorManager;
import eit.fourspace.stufftracker.calculationflow.TLEManager;
import eit.fourspace.stufftracker.config.ConfigData;


public class CameraFragment extends Fragment {
    private PreviewView cameraView;
    private OverlayDrawable canvas;
    private ProcessCameraProvider provider;
    private TLEManager tleManager;
    private ItemRenderer itemRenderer;

    private RotationSensorManager sensorManager;
    private LocationManager locationManager;
    private ConfigData configData;

    private Size screenSize;

    private ConstraintLayout layoutRoot;

    private ObjectWrapper selectedWrapper;

    private CameraManager manager;

    private PopupWindow popup;

    private OverlayTouchListener touchListener;

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
        manager = (CameraManager) requireActivity().getSystemService(Service.CAMERA_SERVICE);
        mainExecutor = ContextCompat.getMainExecutor(requireContext());
        sensorManager = new RotationSensorManager(requireContext());
        locationManager = new LocationManager(requireContext());
        ViewModelProvider provider = new ViewModelProvider(requireActivity());
        ObjectDataModel objectDataModel = provider.get(ObjectDataModel.class);
        configData = new ViewModelProvider(requireActivity()).get(ConfigData.class);
        tleManager = new TLEManager(requireContext(), objectDataModel,
                locationManager, provider.get(ConfigData.class));

        objectDataModel.getAzimuth().observeForever(value -> {
            if (popup == null || value == null) return;
            ((TextView)popup.getContentView().findViewById(R.id.azimuth)).setText(String.format(Locale.ENGLISH,"%.4f\u00B0", value));
        });
        objectDataModel.getElevation().observeForever(value -> {
            if (popup == null || value == null) return;
            ((TextView)popup.getContentView().findViewById(R.id.elevation)).setText(String.format(Locale.ENGLISH,"%.4f\u00B0", value));
        });
        objectDataModel.getVelocity().observeForever(value -> {
            if (popup == null || value == null) return;
            ((TextView)popup.getContentView().findViewById(R.id.velocity)).setText(String.format(Locale.ENGLISH,"%.2fm/s", value));
        });
        objectDataModel.getLatitude().observeForever(value -> {
            if (popup == null || value == null) return;
            ((TextView)popup.getContentView().findViewById(R.id.latitude)).setText(String.format(Locale.ENGLISH,"%.4f\u00B0", value));
        });
        objectDataModel.getLongitude().observeForever(value -> {
            if (popup == null || value == null) return;
            ((TextView)popup.getContentView().findViewById(R.id.longitude)).setText(String.format(Locale.ENGLISH,"%.4f\u00B0", value));
        });
        objectDataModel.getAltitude().observeForever(value -> {
            if (popup == null || value == null) return;
            ((TextView)popup.getContentView().findViewById(R.id.altitude)).setText(String.format(Locale.ENGLISH,"%.2fm", value));
        });
        objectDataModel.getEccentricity().observeForever(value -> {
            if (popup == null || value == null) return;
            ((TextView)popup.getContentView().findViewById(R.id.eccentricity)).setText(String.format(Locale.ENGLISH,"%.4f", value));
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.camera_fragment, container, false);
        layoutRoot = view.findViewById(R.id.camera_container);
        DisplayMetrics dm = new DisplayMetrics();
        requireActivity().getWindowManager().getDefaultDisplay().getRealMetrics(dm);
        screenSize = new Size(dm.widthPixels, dm.heightPixels);
        View canvasView = view.findViewById(R.id.canvas_view);
        itemRenderer = new ItemRenderer(requireContext(), tleManager, sensorManager, canvasView, configData);
        touchListener = new OverlayTouchListener(tleManager, this);
        canvasView.setOnTouchListener(touchListener);
        return view;
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration config) {
        super.onConfigurationChanged(config);
        Display display = ((WindowManager) requireContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        itemRenderer.orientation = display.getRotation();
        dismissPopup(touchListener);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ConstraintLayout container = (ConstraintLayout)view;

        canvas = new OverlayDrawable(itemRenderer, configData);
        ImageView image = container.findViewById(R.id.canvas_view);
        image.setImageDrawable(canvas);

        cameraView = container.findViewById(R.id.camera_view);
        getCameraAngles();
        cameraView.post(this::startCamera);

        Display display = ((WindowManager) requireContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        itemRenderer.orientation = display.getRotation();
        Log.w(TAG, "" + display.getRotation());
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
            Camera camera = provider.bindToLifecycle(self, cameraSelector, preview);
            camera.getCameraControl().setLinearZoom(0);
            getCameraAngles();

        }, mainExecutor);
    }

    private Integer aspectRatio(int height, int width) {
        double previewRatio = (double)Math.max(width, height) / Math.min(width, height);
        if (Math.abs(previewRatio - 4.0/3.0) <= Math.abs(previewRatio - 16.0/9.0)) {
            Log.w(TAG, "AR: 4:3");
            return AspectRatio.RATIO_4_3;
        }
        Log.w(TAG, "AR: 16:9");

        return AspectRatio.RATIO_16_9;
    }

    private void getCameraAngles() {
        if (manager == null) {
            Log.e(TAG, "Unable to get camera manager");
            return;
        }

        try {
            String[] ids = manager.getCameraIdList();
            for (String id : ids) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(id);
                Integer lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (lensFacing != null && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                    float[] focus = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
                    /*for (int j = 0; j < focus.length; j++) {
                        Log.w(TAG, "Focal: " + focus[j]);
                    }*/
                    SizeF size = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
                    Size pSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE);
                    if (size == null || focus == null || pSize == null) {
                        Log.e(TAG, "Unable to get camera properties");
                        return;
                    }
                    itemRenderer.physFocal = focus[0];
                    float w = size.getWidth();
                    float h = size.getHeight();
                    float pw = pSize.getWidth();
                    float ph = pSize.getHeight();
                    // float diagAngle = (float) (2 * Math.atan(Math.sqrt(w*w+h*h) / (focus[0] * 2)));
                    itemRenderer.width = pw;
                    itemRenderer.height = ph;
                    itemRenderer.physWidth = w;
                    itemRenderer.physHeight = h;
                    itemRenderer.updateConstants();

                    // Log.w(TAG, "Diagonal focal angle: " + Math.toDegrees(diagAngle));
                    return;
                }
            }
        }
        catch (CameraAccessException e) {
            Log.e(TAG, "Failed to get camera characteristics: " + e.getMessage());
        }
    }

    synchronized void showPopup(ObjectWrapper wrapper, OverlayTouchListener listener) {
        LayoutInflater inflater = (LayoutInflater)requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater == null) {
            listener.visible = false;
            wrapper.selected = false;
            return;
        }

        if (selectedWrapper != null) {
            selectedWrapper.selected = false;
            tleManager.removeOrbit(selectedWrapper);
        }

        View content;

        if (popup == null) {
            content = inflater.inflate(R.layout.popup, null);
            popup = new PopupWindow(content, (int)(screenSize.getWidth()*0.9), FrameLayout.LayoutParams.WRAP_CONTENT);
            popup.setFocusable(true);
        } else {
            content = popup.getContentView();
        }


        popup.setElevation(5.0f);

        MaterialIconView closeButton = content.findViewById(R.id.close_button);
        closeButton.setClickable(true);

        selectedWrapper = wrapper;

        Switch favorite = content.findViewById(R.id.favorite);
        favorite.setChecked(wrapper.favorite);

        tleManager.addOrbit(wrapper);
        selectedWrapper.selected = true;

        closeButton.setOnClickListener(view -> {
            tleManager.removeOrbit(wrapper);
            popup.dismiss();
            listener.visible = false;
            wrapper.selected = false;
        });
        popup.setOnDismissListener(() -> {
            tleManager.removeOrbit(wrapper);
            listener.visible = false;
            wrapper.selected = false;
        });
        favorite.setOnCheckedChangeListener((view, val) -> {
            if (selectedWrapper != wrapper) return;
            wrapper.favorite = val;
            if (val) {
                configData.addFavorite(wrapper.designation);
            } else {
                configData.removeFavorite(wrapper.designation);
            }
        });
        ((TextView)content.findViewById(R.id.launch_year)).setText(wrapper.launchYear);
        ((TextView)content.findViewById(R.id.launch_number)).setText(wrapper.launchNumber);
        ((TextView)content.findViewById(R.id.launch_part)).setText(wrapper.launchPart);
        ((TextView)content.findViewById(R.id.nameText)).setText(wrapper.name);

        popup.showAtLocation(layoutRoot, Gravity.START | Gravity.BOTTOM,10, 10);
    }
    synchronized void dismissPopup(OverlayTouchListener listener) {
        if (popup == null || !popup.isShowing()) return;
        popup.dismiss();
        listener.visible = false;
        if (selectedWrapper != null) {
            Log.w(TAG, "Dismiss wrapper");
            selectedWrapper.selected = false;
            tleManager.removeOrbit(selectedWrapper);
            selectedWrapper = null;
        } else {
            Log.w(TAG, "Selected wrapper is null");
        }
    }

}

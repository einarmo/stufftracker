package eit.fourspace.stufftracker;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import org.orekit.data.DataProvider;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.ZipJarCrawler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

public class PermissionsFragment extends Fragment {
    private static final int REQUEST_CODE_PERM = 11;
    private static final String[] REQUIRED_PERMS = {Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE};

    private static final String TAG = "Permissions";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!allPermissionsGranted(requireContext())) {
            requestPermissions(REQUIRED_PERMS, REQUEST_CODE_PERM);
        } else {
            Navigation.findNavController(requireActivity(), R.id.nav_container).navigate(
                    PermissionsFragmentDirections.actionPermissionsFragmentToCameraFragment());
        }
        DataProvidersManager manager = DataProvidersManager.getInstance();
        setupOrekitResource();
    }

    private void setupOrekitResource() {
        File data = new File("orekitdata.zip");
        DataProvidersManager manager = DataProvidersManager.getInstance();
        if (data.exists()) {
            manager.addProvider(new ZipJarCrawler(data));
            return;
        }
        InputStream stream = getResources().openRawResource(getResources().getIdentifier("orekitdata", "raw", requireContext().getPackageName()));
        File out;
        try {
            out = File.createTempFile("orekitdata", ".zip");
            byte[] buffer = new byte[stream.available()];
            stream.read(buffer);
            stream.close();
            OutputStream outStream = new FileOutputStream(out);
            outStream.write(buffer);
            outStream.flush();
            outStream.close();
            manager.addProvider(new ZipJarCrawler(out));
        }
        catch (FileNotFoundException ex) {
            Log.w(TAG, "Orekit out not found");
        }
        catch (IOException ex) {
            Log.w(TAG, "Failed to read from stream");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_PERM) {
            if (allPermissionsGranted(requireContext())) {
                Navigation.findNavController(requireActivity(), R.id.nav_container).navigate(
                        PermissionsFragmentDirections.actionPermissionsFragmentToCameraFragment());
            } else {
                Toast.makeText(requireContext(),"Permissions not granted by user", Toast.LENGTH_SHORT).show();
            }
        }
    }
    static boolean allPermissionsGranted(Context context) {
        for (String perm : REQUIRED_PERMS) {
            if (ContextCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED) return false;
        }
        return true;
    }

}

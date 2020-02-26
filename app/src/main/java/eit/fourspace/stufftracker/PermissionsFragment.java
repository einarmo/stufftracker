package eit.fourspace.stufftracker;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.data.ZipJarCrawler;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
        setupOrekitResource();
    }

    private void setupOrekitResource() {
        String path = requireContext().getFilesDir().getPath() + "/orekitdata2/";
        File data = new File(path);
        DataProvidersManager manager = DataProvidersManager.getInstance();
        if (data.exists()) {
            manager.addProvider(new DirectoryCrawler(data));
            return;
        }
        data.mkdirs();
        InputStream stream = getResources().openRawResource(getResources().getIdentifier("orekitdata", "raw", requireContext().getPackageName()));
        ZipInputStream zis;
        try
        {
            String filename;
            zis = new ZipInputStream(new BufferedInputStream(stream));
            ZipEntry ze;
            byte[] buffer = new byte[1024];
            int count;

            while ((ze = zis.getNextEntry()) != null)
            {
                filename = ze.getName();
                // Need to create directories if not exists, or
                // it will generate an Exception...
                if (ze.isDirectory()) {
                    File fmd = new File(path + filename);
                    fmd.mkdirs();
                    continue;
                }

                FileOutputStream fout = new FileOutputStream(path + filename);

                while ((count = zis.read(buffer)) != -1)
                {
                    fout.write(buffer, 0, count);
                }

                fout.close();
                zis.closeEntry();
            }

            zis.close();
        }
        catch(IOException e)
        {
            Log.e(TAG, "Error extracting data", e);
            return;
        }
        data = new File(path);
        manager.addProvider(new DirectoryCrawler(data));
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

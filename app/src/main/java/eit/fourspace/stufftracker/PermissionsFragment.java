package eit.fourspace.stufftracker;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

public class PermissionsFragment extends Fragment {
    private static final int REQUEST_CODE_PERM = 11;
    private static final String[] REQUIRED_PERMS = {Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!allPermissionsGranted(requireContext())) {
            requestPermissions(REQUIRED_PERMS, REQUEST_CODE_PERM);
        } else {
            Navigation.findNavController(requireActivity(), R.id.nav_container).navigate(
                    PermissionsFragmentDirections.actionPermissionsFragmentToCameraFragment());
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
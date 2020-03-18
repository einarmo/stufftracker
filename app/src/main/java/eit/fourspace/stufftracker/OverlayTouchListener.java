package eit.fourspace.stufftracker;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import org.hipparchus.util.FastMath;

import java.util.Collections;
import java.util.LinkedList;

import eit.fourspace.stufftracker.calculationflow.ObjectWrapper;
import eit.fourspace.stufftracker.calculationflow.TLEManager;

public class OverlayTouchListener implements View.OnTouchListener {
    private final TLEManager manager;
    private static int TOUCH_LOCALITY = 40;
    boolean visible = false;
    private static final String TAG = "OverlayTouchListener";
    private final CameraFragment root;

    OverlayTouchListener(TLEManager manager, CameraFragment root) {
        this.manager = manager;
        this.root = root;
    }
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        LinkedList<ObjectWrapper> hits = new LinkedList<>();
        if (event.getActionMasked() != MotionEvent.ACTION_UP) return true;
        //if (visible) {
        //    root.dismissPopup(this);
        //}
        int x = (int)event.getX();
        int y = (int)event.getY();
        for (ObjectWrapper wrapper : manager.getObjects()) {
            if (wrapper.visible && !wrapper.filtered && FastMath.abs(wrapper.projection.getX() - x) < TOUCH_LOCALITY
                    && FastMath.abs(wrapper.projection.getY() - y) < TOUCH_LOCALITY) {
                hits.add(wrapper);
            }
        }
        if (hits.size() > 0) {
            Collections.sort(hits, (o1, o2) -> (int)(o1.projection.getNorm() - o2.projection.getNorm()));
            ObjectWrapper hit = hits.getFirst();
            Log.w(TAG, hit.name);
            root.showPopup(hit, this);
            visible = true;
        } else {
            Log.w(TAG, "No hits: " + x + ", " + y);
            root.dismissPopup(this);
        }
        v.performClick();
        return true;
    }
}

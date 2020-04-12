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
    private static final int TOUCH_LOCALITY = 40;
    boolean visible = false;
    private static final String TAG = "OverlayTouchListener";
    private final CameraFragment root;

    OverlayTouchListener(TLEManager manager, CameraFragment root) {
        this.manager = manager;
        this.root = root;
    }
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        v.performClick();
        LinkedList<ObjectWrapper> hits = new LinkedList<>();
        if (event.getAction() != MotionEvent.ACTION_UP) return true;
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
            root.showPopup(hit, this);
            Log.i(TAG, "View popup for: " + hit.name);
            visible = true;
        } else {
            root.dismissPopup(this);
        }
        return true;
    }
}

package eit.fourspace.stufftracker.calculationflow;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.geometry.euclidean.twod.Vector2D;

public class OrbitWrapper {
    public static final int NUM_POINTS = 100;
    public ObjectWrapper obj;
    Vector3D[] positions = new Vector3D[NUM_POINTS];
    Vector3D[] transformedPositions = new Vector3D[NUM_POINTS];
    public Vector3D[] rotatedPositions = new Vector3D[NUM_POINTS];
    public Vector2D[] projections = new Vector2D[NUM_POINTS];
    public boolean initialized = false;
    public boolean rendered = false;
    public OrbitWrapper(ObjectWrapper obj) {
        this.obj = obj;
    }
}

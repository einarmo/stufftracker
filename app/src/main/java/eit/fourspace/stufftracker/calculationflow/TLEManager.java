package eit.fourspace.stufftracker.calculationflow;

import android.content.Context;
import android.hardware.GeomagneticField;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.frames.TopocentricFrame;
import org.orekit.frames.Transform;
import org.orekit.models.earth.ReferenceEllipsoid;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;

import eit.fourspace.stufftracker.config.ConfigData;

public class TLEManager {
    private boolean paused = false;
    private Handler tleWorker;

    private static final String TAG = "TLEManager";
    private ObjectDataModel dataModel;
    private DataManager dataManager;
    private LocationManager locManager;
    private final double[] locationVector = new double[3];

    private ConfigData configDataModel;

    private ReferenceEllipsoid earth = null;
    private boolean filterChanged = false;
    private boolean filterReset = false;
    private boolean showSatellites = true;
    private boolean showRocketBodies = true;
    private boolean showDebris = true;
    private String filter = "";
    private String[] splitFilter;
    private LinkedList<OrbitWrapper> orbits = new LinkedList<>();

    private Runnable tleRunnable = new Runnable() {
        @Override
        public void run() {
            if (!paused) {
                tleWorker.postDelayed(this, 200);
            }
            if (dataManager != null && dataManager.initialized) {
                if (earth == null) {
                    earth = ReferenceEllipsoid.getWgs84(dataManager.ITRF);
                }
                dataManager.propagateTLEs();
                dataManager.refreshPositions();
                Vector3D[] positions = dataManager.getPositions();

                boolean showAll = false;
                if (configDataModel != null && configDataModel.getShowAll().getValue() != null) {
                    showAll = configDataModel.getShowAll().getValue();
                }

                ArrayList<ObjectWrapper> objects = dataManager.objects;

                locManager.getLocation(locationVector);
                GeodeticPoint location = new GeodeticPoint(FastMath.toRadians(locationVector[0]), FastMath.toRadians(locationVector[1]), locationVector[2]);
                TopocentricFrame localFrame = new TopocentricFrame(earth, location, "CurrentLocation");

                Date rawDate = new Date();

                AbsoluteDate date = new AbsoluteDate(rawDate, TimeScalesFactory.getUTC());

                Transform tf = dataManager.ITRF.getTransformTo(localFrame, date);

                if (configDataModel != null && configDataModel.getTrueNorth().getValue() != null && configDataModel.getTrueNorth().getValue()) {
                    double geoMagAngle = new GeomagneticField((float)locationVector[0], (float)locationVector[1], (float)locationVector[2], rawDate.getTime()).getDeclination();
                    Rotation rot = new Rotation(new Vector3D(0, 0, 1), FastMath.toRadians(geoMagAngle), RotationConvention.FRAME_TRANSFORM);
                    tf = new Transform(date, tf, new Transform(date, rot));
                }

                for (int i = 0; i < positions.length; i++) {
                    ObjectWrapper obj = objects.get(i);
                    if (filterChanged && (filterReset || !obj.filtered)) {
                        boolean filter =
                                !showSatellites && obj.objectClass == ObjectClass.PAYLOAD
                                || !showRocketBodies && obj.objectClass == ObjectClass.ROCKET_BODY
                                || !showDebris && (obj.objectClass == ObjectClass.DEBRIS || obj.objectClass == ObjectClass.UNKNOWN);
                        if (!filter && splitFilter != null && splitFilter.length != 0) {
                            for (String s : splitFilter) {
                                if (s.equals("")) continue;
                                if (!obj.name.toLowerCase().contains(s) && !obj.designation.toLowerCase().contains(s)) {
                                    filter = true;
                                    break;
                                }
                            }
                        }
                        obj.filtered = filter;
                    }
                    obj.filtered &= !obj.selected && !obj.favorite;
                    if (obj.filtered) continue;
                    obj.position = tf.transformPosition(positions[i]);
                    obj.baseVisible = obj.favorite || obj.selected || showAll || obj.objectClass == ObjectClass.STATION || obj.position.getZ() > 0;
                    if (obj.selected) {
                        double elevation = 90 - Math.toDegrees(Math.acos(obj.position.getZ() / obj.position.getNorm()));
                        double rawAzimuth = Math.toDegrees(Math.atan2(obj.position.getX(), obj.position.getY()));
                        double azimuth = (rawAzimuth > 0 ? rawAzimuth : 360 + rawAzimuth);
                        dataModel.setElevation(elevation);
                        dataModel.setAzimuth(azimuth);
                    }
                    //if (obj.position.getZ() > 0 && Math.abs(obj.position.getX()) < 100000 && Math.abs(obj.position.getY()) < 100000) {
                    //    Log.w(TAG, "OVERHEAD: " + obj.name + ", " + obj.position.toString());
                    //}
                }
                filterChanged = false;
                filterReset = false;

                for (int i = 0; i < orbits.size(); i++) {
                    OrbitWrapper orbit = orbits.get(i);
                    if (!orbit.initialized) {
                        dataManager.initializeOrbit(orbit);
                    }
                    for (int j = 0; j < OrbitWrapper.NUM_POINTS; j++) {
                        orbit.transformedPositions[j] = tf.transformPosition(orbit.positions[j]);
                    }
                    orbit.initialized = true;
                }
                // Log.w(TAG, "TLE Calculations took " + TimeUnit.MILLISECONDS.convert(end.getTime() - start.getTime(), TimeUnit.MILLISECONDS) + " milliseconds");
            }
        }
    };

    public TLEManager(Context context, ObjectDataModel dataModel, LocationManager locManager, ConfigData configDataModel) {
        this.dataModel = dataModel;
        this.locManager = locManager;
        this.dataManager = dataModel.getDataManager().getValue();
        HandlerThread tleWorkerThread = new HandlerThread("TLEWorker", 5);
        tleWorkerThread.start();
        tleWorker = new Handler(tleWorkerThread.getLooper());
        this.configDataModel = configDataModel;
        configDataModel.getFilter().observeForever(val -> {
            filterReset |= (filter.indexOf(val) != 0 || val.equals(""));
            filterChanged |= !filter.equals(val);
            filter = val.toLowerCase();
            splitFilter = filter.split(" ");
        });
        configDataModel.getShowSatellite().observeForever(val -> {
            filterChanged |= val != showSatellites;
            filterReset |= val && !showSatellites;
            showSatellites = val;
        });
        configDataModel.getShowRocketBody().observeForever(val -> {
            filterChanged |= val != showRocketBodies;
            filterReset |= val && !showRocketBodies;
            showRocketBodies = val;
        });
        configDataModel.getShowDebris().observeForever(val -> {
            filterChanged |= val != showDebris;
            filterReset |= val && !showDebris;
            showDebris = val;
        });
    }

    public void onPause() {
        paused = true;
        tleWorker.removeCallbacks(tleRunnable);
    }

    public void onResume() {
        paused = false;
        tleWorker.postDelayed(tleRunnable,1000);
        dataManager.resetIteratorCount();
    }

    public ArrayList<ObjectWrapper> getObjects() {
        return dataManager.objects;
    }
    public LinkedList<OrbitWrapper> getOrbits() { return orbits; }
    public void removeOrbit(ObjectWrapper wrapper) {
        OrbitWrapper toRemove = null;
        for (int i = 0; i < orbits.size(); i++) {
            if (wrapper == orbits.get(i).obj) {
                toRemove = orbits.get(i);
            }
        }
        if (toRemove == null) return;
        orbits.remove(toRemove);
    }
    public void addOrbit(ObjectWrapper wrapper) {
        OrbitWrapper old = null;
        for (int i = 0; i < orbits.size(); i++) {
            if (wrapper == orbits.get(i).obj) {
                old = orbits.get(i);
            }
        }
        if (old != null) {
            orbits.remove(old);
        }
        orbits.add(new OrbitWrapper(wrapper));
    }
}

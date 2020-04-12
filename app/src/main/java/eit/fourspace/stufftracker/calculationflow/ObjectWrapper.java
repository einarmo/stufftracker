package eit.fourspace.stufftracker.calculationflow;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.geometry.euclidean.twod.Vector2D;
import org.jetbrains.annotations.NotNull;

public class ObjectWrapper {
    public final String name;
    public final String designation;
    public ObjectClass objectClass;
    // Relative position in Magnetic/True north Topocentric Frame
    Vector3D position;
    // Relative position in android rotated frame
    public Vector3D rotatedPosition;

    public Vector2D projection;
    // public int size; // Size on screen, based on distance (don't know if we will be using this).

    // YYYY-NNN[part] launch number NNN of year, with a letter to designate the last part.
    public final String launchYear;
    public final String launchNumber;
    public final String launchPart;

    public boolean visible;
    public boolean filtered;
    boolean baseVisible;
    public boolean selected;
    boolean invalid;
    public boolean favorite;

    ObjectWrapper(String name, String type, String designation) {
        this.name = name;
        switch (type) {
            case "PAYLOAD":
                objectClass = ObjectClass.PAYLOAD;
                break;
            case "ROCKET BODY":
                objectClass = ObjectClass.ROCKET_BODY;
                break;
            case "DEBRIS":
                objectClass = ObjectClass.DEBRIS;
                break;
            default:
                objectClass = ObjectClass.UNKNOWN;
                break;
        }
        this.designation = designation;
        if (designation.length() < 4) {
            launchYear = designation;
            launchPart = "";
            launchNumber = "";
        } else if (designation.length() < 8) {
            launchYear = designation.substring(0, 4);
            if (designation.length() > 4) {
                launchNumber = designation.substring(5);
            } else {
                launchNumber = "";
            }

            launchPart = "";
        } else {
            launchYear = designation.substring(0, 4);
            launchNumber = designation.substring(5, 8);
            launchPart = designation.substring(8);
        }
        if (name.equals("ISS (ZARYA)")) {
            objectClass = ObjectClass.STATION;
        }
        visible = false;
        filtered = false;
        baseVisible = false;
        position = new Vector3D(0, 0, 0);
        projection = new Vector2D(0, 0);
        rotatedPosition = new Vector3D(0, 0, 0);
    }
    @NotNull
    @Override
    public String toString() {
        return "TLE Object: {\n" +
                "\tName: " + name + "\n" +
                "\tObjectClass: " + objectClass + "\n" +
                "\tLaunch: " + launchNumber + ", part: " + launchPart + "\n" +
                "\tCurrent visible: " + (visible && !filtered) + "\n" +
                "}\n";
    }
}

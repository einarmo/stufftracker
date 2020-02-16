package eit.fourspace.stufftracker.calculationflow;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.geometry.euclidean.twod.Vector2D;
import org.jetbrains.annotations.NotNull;

public class ObjectWrapper {
    public final String name;
    public final ObjectClass objectClass;
    // Relative position after all rotations
    public Vector3D position;

    public Vector2D projection;
    public int size; // Size on screen, based on distance (don't know if we will be using this).

    // YYYY-NNN[part] launch number NNN of year, with a letter to designate the last part.
    public final String launchNumber;
    public final String launchPart;

    public boolean visible;
    public boolean filtered;

    ObjectWrapper(String name, String type, String designation) {
        this.name = name;
        switch (type) {
            case "PAYLOAD":
                objectClass = ObjectClass.PAYLOAD;
                break;
            case "ROCKET_BODY":
                objectClass = ObjectClass.ROCKET_BODY;
                break;
            case "DEBRIS":
                objectClass = ObjectClass.DEBRIS;
                break;
            default:
                objectClass = ObjectClass.UNKNOWN;
                break;
        }
        if (designation.length() < 8) {
            launchNumber = designation;
            launchPart = "";
        } else {
            launchNumber = designation.substring(0, 8);
            launchPart = designation.substring(8);
        }
        visible = false;
        filtered = false;
        position = new Vector3D(0, 0, 0);
        projection = new Vector2D(0, 0);
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

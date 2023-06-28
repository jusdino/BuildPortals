package space.frahm.buildportals;

import java.util.ArrayList;

import org.bukkit.World;
import org.bukkit.util.Vector;

public class PortalFrame {
    // TODO: Move some of the Portal logic that is specific to frame geometry into this class.
    public final World world;
    public final ArrayList<Vector> interior;
    public final ArrayList<Vector> exterior;
    public final Float yaw;

    public PortalFrame(
        World world,
        ArrayList<Vector> interior,
        ArrayList<Vector> exterior,
        float yaw
        ) {
        this.world = world;
        this.interior = interior;
        this.exterior = exterior;
        this.yaw = yaw;
    }
}

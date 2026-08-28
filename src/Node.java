import utils.Vec3;

public class Node {

    /* The position of the node */
    private Vec3 position;

    /* The initial velocity, used to initialize the Verlet position history */
    private final Vec3 initialVelocity;

    /* The position of the node in the previous simulation step */
    private Vec3 previous_pos;

    /* The position of the node for the next simulation step */
    private Vec3 next_pos;

    /* The mass of the node */
    private double mass;

    /* Wether the node is pinned or not */
    private boolean pinned = false;

    /* The constructors of the class */
    public Node(Vec3 position, Vec3 speed, double mass) {
        this.position = new Vec3(position.getX(), position.getY(), position.getZ());
        this.previous_pos = new Vec3(position.getX(), position.getY(), position.getZ());
        this.next_pos = new Vec3(position.getX(), position.getY(), position.getZ());
        this.initialVelocity = new Vec3(speed.getX(), speed.getY(), speed.getZ());
        this.mass = mass;
    }

    public Node(Vec3 position, Vec3 speed) {
        this(position, speed, 1.0);
    }

    public Node(Vec3 position) {
        this(position, new Vec3(0.0, 0.0, 0.0), 1.0);
    }

    public Node(double x, double y, double z,
                double dx, double dy, double dz,
                double mass) {
        this(new Vec3(x, y, z),
             new Vec3(dx, dy, dz),
             mass);
    }

    public Node(double x, double y, double z,
                double dx, double dy, double dz) {
        this(x, y, z, dx, dy, dz, 1.0);
    }

    public Node(double x, double y, double z) {
        this(x, y, z, 0.0, 0.0, 0.0, 1.0);
    }

    public Node() {
        this(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0);
    }

    /* The getters/setters for the position */
    public double getX() { return position.getX(); }
    public double getY() { return position.getY(); }
    public double getZ() { return position.getZ(); }
    public Vec3 getPosition() { return this.position; }

    public void setX(double x) { position.setX(x); }
    public void setY(double y) { position.setY(y); }
    public void setZ(double z) { position.setZ(z); }
    public void setPosition(Vec3 pos) { this.position = pos; }


    /* Initial velocity is encoded into previous_pos before the first step. */
    public Vec3 getInitialVelocity() { return this.initialVelocity; }


    /* The getter/setter for the previous step attribute */
    public Vec3 getPreviousPosition() { return this.previous_pos; }
    public void setPreviousPosition(Vec3 pos) { previous_pos = pos; }


    /* The getters/setters for the next step attributes */
    public Vec3 getNextPosition() { return this.next_pos; }
    public void setNextPosition(Vec3 np) { next_pos = np; }


    /* The getter/setter for the mass */
    public double getMass() { return this.mass; }
    public void setMass(double mass) { this.mass = mass; }


    /* Methods for pinning/unpinng the node */
    public void pin() { this.pinned = true; }
    public void unpin() { this.pinned = false; }

    /* Method for knowing wether the node is pinned or not */
    public boolean isPinned() { return this.pinned; }
}

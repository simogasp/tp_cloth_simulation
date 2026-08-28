package utils;

import javax.management.RuntimeErrorException;

public class Vec3 {
 
    private double x;
    private double y;
    private double z;

    public Vec3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vec3() {
        this(0.0, 0.0, 0.0);
    }

    public double getX() { return this.x; }
    public double getY() { return this.y; }
    public double getZ() { return this.z; }

    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
    public void setZ(double z) { this.z = z; }


    public Vec3 add(Vec3 other) {
        return new Vec3(
            this.x + other.getX(),
            this.y + other.getY(),
            this.z + other.getZ());
    }

    public Vec3 sub(Vec3 other) {
        return new Vec3(
            this.x - other.getX(),
            this.y - other.getY(),
            this.z - other.getZ());
    }

    public Vec3 scale(double k) {
        return new Vec3(
            this.x * k,
            this.y * k,
            this.z * k);
    }

    public double dot(Vec3 other) {
        return (this.x * other.getX() +
                this.y * other.getY() +
                this.z * other.getZ());
    }

    public double norm() {
        return Math.sqrt(
            this.x * this.x +
            this.y * this.y +
            this.z * this.z);
    }

    public Vec3 normalize() {
        double n = this.norm();
        if(n > 0) {
            return new Vec3(
                this.x / n, 
                this.y / n, 
                this.z / n);
        } else {
            throw new RuntimeErrorException(null);
        }
    }

    public Vec3 cross(Vec3 other) {
        double ox = other.getX();
        double oy = other.getY();
        double oz = other.getZ();
        
        return new Vec3(
            this.y * oz - this.z * y,
            this.z * ox - this.x * z,
            this.x * oy - this.y * x
        );
    }

    public void reinitialize() {
        this.x = 0.0;
        this.y = 0.0;
        this.z = 0.0;
    }
}

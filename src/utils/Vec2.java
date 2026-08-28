package utils;

public class Vec2 {
    
    private double x;
    private double y;

    public Vec2(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Vec2() {
        this(0.0, 0.0);
    }

    public double getX() { return this.x; }
    public double getY() { return this.y; }

    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }

    public Vec2 add(Vec2 other) {
        return new Vec2(this.x + other.x, this.y + other.y);
    }

    public Vec2 sub(Vec2 other) {
        return new Vec2(this.x - other.x, this.y - other.y);
    }

    public Vec2 scale(double k) {
        return new Vec2(this.x * k, this.y * k);
    }

    public double dot(Vec2 other) {
        return (this.x * other.getX() + this.y * other.getY());
    }

    public double norm() {
        return Math.sqrt(this.x * this.x + this.y * this.y);
    }

    public Vec2 normalize() {
        double n = this.norm();
        return new Vec2(this.x / n, this.y / n);
    }

    public void reinitialize() {
        this.x = 0.0;
        this.y = 0.0;
    }
}

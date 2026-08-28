import javax.management.RuntimeErrorException;

import utils.Vec3;

public class Simulation {
    
    /* The gravity vector */
    private Vec3 g = new Vec3(0.0, -9.81, 0.0);

    /* The spring (strength) constant */
    private final double k;

    /* The damping coefficient applied along each spring */
    private final double springDampingCoeff;

    /* Global velocity decay rate, in inverse seconds */
    private final double airDampingRate;

    /* Position Verlet requires a constant time step */
    private final double timeStep;

    /* Permitted spring lengths, expressed as ratios of rest length */
    private final double minimumLengthRatio;
    private final double maximumLengthRatio;

    /* Number of Gauss-Seidel passes over all spring constraints */
    private final int stretchConstraintIterations;

    /* Whether previous positions encode the nodes' initial velocities */
    private boolean positionHistoryInitialized = false;

    /* The resting length for horizontal/vertical spring */
    private final double rest_length;
    
    /* The resting length for diagonal springs */
    private final double rest_length_diag;

    /* The handle to the main Node array */
    private Node[][] cloth;

    /* The sizes of the cloth array */
    private final int width;
    private final int height;

    /* The constructor of the class */
    public Simulation(Node[][] cloth, double size, double k,
                      double springDampingCoeff, double airDampingRate,
                      double timeStep, double minimumLengthRatio,
                      double maximumLengthRatio, int stretchConstraintIterations) {
        if(timeStep <= 0.0) {
            throw new IllegalArgumentException("The simulation time step must be positive");
        }
        if(springDampingCoeff < 0.0 || airDampingRate < 0.0) {
            throw new IllegalArgumentException("Damping coefficients cannot be negative");
        }
        if(minimumLengthRatio <= 0.0 || minimumLengthRatio > 1.0
                || maximumLengthRatio < 1.0
                || minimumLengthRatio > maximumLengthRatio) {
            throw new IllegalArgumentException(
                "Spring length ratios must satisfy 0 < minimum <= 1 <= maximum"
            );
        }
        if(stretchConstraintIterations <= 0) {
            throw new IllegalArgumentException(
                "The stretch constraint iteration count must be positive"
            );
        }

        this.cloth = cloth;
        this.rest_length = size;
        this.rest_length_diag = size * Math.sqrt(2.0);
        this.k = k;
        this.springDampingCoeff = springDampingCoeff;
        this.airDampingRate = airDampingRate;
        this.timeStep = timeStep;
        this.minimumLengthRatio = minimumLengthRatio;
        this.maximumLengthRatio = maximumLengthRatio;
        this.stretchConstraintIterations = stretchConstraintIterations;

        try {
            this.height = cloth.length;
            this.width = cloth[0].length;
        } catch(Exception e) {
            System.out.println("The cloth array is not correctly instanciated");
            throw(new RuntimeErrorException(null));
        }
    }

    /* Encode each node's initial velocity in its position history. */
    private void initializePositionHistory() {
        for(int j = 0; j < this.height ; j++) {
            for(int i = 0; i < this.width ; i++) {
                Node node = cloth[j][i];
                Vec3 initialVelocity = node.isPinned()
                    ? new Vec3()
                    : node.getInitialVelocity();

                node.setPreviousPosition(
                    node.getPosition().sub(initialVelocity.scale(this.timeStep))
                );
                node.setNextPosition(node.getPosition());
            }
        }
        this.positionHistoryInitialized = true;
    }

    /* Commit all predicted positions simultaneously. */
    private void update() {
        for(int j = 0; j < this.height ; j++) {
            for(int i = 0; i < this.width ; i++) {
                // The current position becomes the previous position
                cloth[j][i].setPreviousPosition(cloth[j][i].getPosition());
                // The predicted position becomes the current position
                cloth[j][i].setPosition(cloth[j][i].getNextPosition());
            }
        }
    }

    /*
     * Project one spring back into its permitted length interval. The position
     * correction is distributed according to inverse mass. A pinned endpoint
     * has zero inverse mass, so the other endpoint receives the full correction.
     */
    private boolean projectSpringConstraint(Node first, Node second,
                                            double restLength) {
        double firstInverseMass = first.isPinned() ? 0.0 : 1.0 / first.getMass();
        double secondInverseMass = second.isPinned() ? 0.0 : 1.0 / second.getMass();
        double inverseMassSum = firstInverseMass + secondInverseMass;
        if(inverseMassSum <= 0.0) {
            return false;
        }

        double dx = second.getX() - first.getX();
        double dy = second.getY() - first.getY();
        double dz = second.getZ() - first.getZ();
        double currentLength = Math.sqrt(dx * dx + dy * dy + dz * dz);

        double minimumLength = this.minimumLengthRatio * restLength;
        double maximumLength = this.maximumLengthRatio * restLength;
        double targetLength;
        if(currentLength < minimumLength) {
            targetLength = minimumLength;
        } else if(currentLength > maximumLength) {
            targetLength = maximumLength;
        } else {
            return false;
        }

        /* If the endpoints coincide, recover a direction from position history. */
        double directionLength = currentLength;
        if(directionLength <= 1e-12) {
            dx = second.getPreviousPosition().getX() - first.getPreviousPosition().getX();
            dy = second.getPreviousPosition().getY() - first.getPreviousPosition().getY();
            dz = second.getPreviousPosition().getZ() - first.getPreviousPosition().getZ();
            directionLength = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if(directionLength <= 1e-12) {
                return false;
            }
        }

        double correctionScale = (currentLength - targetLength)
            / (directionLength * inverseMassSum);
        double correctionX = dx * correctionScale;
        double correctionY = dy * correctionScale;
        double correctionZ = dz * correctionScale;

        if(!first.isPinned()) {
            first.setX(first.getX() + correctionX * firstInverseMass);
            first.setY(first.getY() + correctionY * firstInverseMass);
            first.setZ(first.getZ() + correctionZ * firstInverseMass);
        }
        if(!second.isPinned()) {
            second.setX(second.getX() - correctionX * secondInverseMass);
            second.setY(second.getY() - correctionY * secondInverseMass);
            second.setZ(second.getZ() - correctionZ * secondInverseMass);
        }

        return true;
    }

    /* Iteratively constrain every structural and diagonal spring exactly once. */
    private void enforceSpringLengthConstraints() {
        for(int iteration = 0; iteration < this.stretchConstraintIterations; iteration++) {
            boolean positionWasCorrected = false;

            /* Sweep away from the pinned upper edge; alternate horizontally. */
            int rowStep = -1;
            int columnStep = (iteration % 2 == 0) ? 1 : -1;
            int firstRow = (rowStep < 0) ? this.height - 1 : 0;
            int firstColumn = (columnStep > 0) ? 0 : this.width - 1;

            for(int j = firstRow; j >= 0 && j < this.height; j += rowStep) {
                for(int i = firstColumn; i >= 0 && i < this.width; i += columnStep) {
                    Node current = cloth[j][i];
                    int neighborRow = j + rowStep;
                    int neighborColumn = i + columnStep;

                    if(neighborColumn >= 0 && neighborColumn < this.width) {
                        positionWasCorrected |= projectSpringConstraint(
                            current, cloth[j][neighborColumn], this.rest_length
                        );
                    }
                    if(neighborRow >= 0 && neighborRow < this.height) {
                        positionWasCorrected |= projectSpringConstraint(
                            current, cloth[neighborRow][i], this.rest_length
                        );
                        if(i + 1 < this.width) {
                            positionWasCorrected |= projectSpringConstraint(
                                current, cloth[neighborRow][i + 1], this.rest_length_diag
                            );
                        }
                        if(i - 1 >= 0) {
                            positionWasCorrected |= projectSpringConstraint(
                                current, cloth[neighborRow][i - 1], this.rest_length_diag
                            );
                        }
                    }
                }
            }

            if(!positionWasCorrected) {
                break;
            }
        }
    }


    /* The main simulation method */
    public void simulate() {
        boolean firstStep = !this.positionHistoryInitialized;
        if(!this.positionHistoryInitialized) {
            initializePositionHistory();
        }

        double inverseTimeStep = 1.0 / this.timeStep;
        double squaredTimeStep = this.timeStep * this.timeStep;
        double velocityRetention = Math.exp(-this.airDampingRate * this.timeStep);

        for(int j = 0; j < this.height ; j++) {
            for(int i = 0; i < this.width ; i++) {
                Node node = cloth[j][i];

                /* A pinned node stays at its current position. */
                if(node.isPinned()) {
                    node.setNextPosition(node.getPosition());
                    continue;
                }

                Vec3 pos = node.getPosition();
                double mass = node.getMass();
                if(mass <= 0.0) {
                    throw new IllegalStateException("Every unpinned node must have positive mass");
                }

                /* Verlet stores velocity implicitly in the position difference. */
                Vec3 velocity = pos.sub(node.getPreviousPosition())
                    .scale(inverseTimeStep);

                /* Gravity is converted to force so all forces can be summed first. */
                Vec3 force = this.g.scale(mass);

                for(int kk = -1; kk <= 1; kk++) {
                    for(int ll = -1; ll <= 1; ll++) {
                        /* Skip self */
                        if(kk == 0 && ll == 0) { continue; }
                        if(i + ll < 0 || i + ll >= width) { continue; }
                        if(j + kk < 0 || j + kk >= height) { continue; }

                        Node other = cloth[j+kk][i+ll];
                        Vec3 o_pos = other.getPosition();

                        Vec3 curr_to_other = o_pos.sub(pos);
                        double curr_length = curr_to_other.norm();

                        if(curr_length <= 1e-8) { continue; }

                        Vec3 direction = curr_to_other.scale(1.0 / curr_length);
                        double restLength = (Math.abs(kk) == 1 && Math.abs(ll) == 1)
                            ? this.rest_length_diag
                            : this.rest_length;

                        /* Hooke force along the spring. */
                        double springMagnitude = this.k * (curr_length - restLength);

                        /*
                         * Damp only the endpoints' relative motion along the spring.
                         * Tangential and whole-cloth motion are left unaffected.
                         */
                        Vec3 otherVelocity = o_pos.sub(other.getPreviousPosition())
                            .scale(inverseTimeStep);
                        double relativeSpeed = otherVelocity.sub(velocity).dot(direction);
                        double dampingMagnitude = this.springDampingCoeff * relativeSpeed;

                        force = force.add(
                            direction.scale(springMagnitude + dampingMagnitude)
                        );
                    }
                }

                /* Divide by mass to get acceleration */
                Vec3 acceleration = force.scale(1.0 / mass);

                if(firstStep) {
                    /* Bootstrap Verlet with x(h) = x(0) + v(0)h + a(0)h^2/2. */
                    node.setNextPosition(
                        pos.add(velocity.scale(this.timeStep))
                            .add(acceleration.scale(0.5 * squaredTimeStep))
                    );
                } else {
                    /*
                     * Damped Verlet:
                     * x(t+h) = x(t) + r * (x(t) - x(t-h)) + a(t)*h^2,
                     * where r = exp(-airDampingRate*h).
                     */
                    node.setNextPosition(
                        pos.add(pos.sub(node.getPreviousPosition()).scale(velocityRetention))
                            .add(acceleration.scale(squaredTimeStep))
                    );
                }
            }
        }

        /* Commit updates */
        update();

        /* Project the predicted positions back into the permitted stretch range. */
        enforceSpringLengthConstraints();
    }

}

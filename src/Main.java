import utils.Vec3;

public class Main {

    /* Method that creates a cloth */
    private static Node[][] createCloth(int width, int height, double mass, double size) {

        /* Create an empty cloth */
        Node[][] cloth = new Node[height][width];

        /* Create all the nodes */
        for(int j = 0; j < height ; j++) {
            for(int i = 0; i < width ; i++) {
                
                Vec3 pos = new Vec3(i * size, j * size, 0.0);
                Vec3 spd = new Vec3(0.0, 0.0, 0.0);

                cloth[j][i] = new Node(pos, spd, mass);
            }
        }

        cloth[height-1][0].pin();
        cloth[height-1][width-1].pin();

        return cloth;
    }

    /* The simulation parameters */
    private static double bigger = 10;

    private static int width = 50;
    private static int height = 50;
    private static double mass = 0.00016 * bigger;
    private static double size = 0.008 * bigger;
    private static double k = 50.0;

    /* The damping forces coefficients */
    private static double springDampingCoeff = 0.1;
    private static double airDampingRate = 1.0;

    /* Target range for structural and diagonal spring lengths. */
    private static double minimumLengthRatio = 0.9;
    private static double maximumLengthRatio = 1.1;
    private static int stretchConstraintIterations = 10;

    /* Physics advances independently of the rendering frame rate. */
    private static final double physicsTimeStep = 1.0 / 500.0;
    private static final double maxFrameTime = 0.1;
    
    private static float zoomOutFactor = 1.4f;
    private static double simulationSpeed = 1.0;


    /* The main application method */
    public static void main(String[] args) {

        /* Create a cloth */
        Node[][] cloth = createCloth(width, height, mass, size);

        /* Create a Simulation */
        Simulation sim = new Simulation(
            cloth, size, k, springDampingCoeff, airDampingRate, physicsTimeStep,
            minimumLengthRatio, maximumLengthRatio, stretchConstraintIterations
        );

        try (ClothRenderer renderer = new ClothRenderer(cloth, zoomOutFactor)) {
            /* Initialize the timer */
            long lastTime = System.nanoTime();
            double accumulatedTime = 0.0;

            while (!renderer.shouldClose()) {
                long currentTime = System.nanoTime();
                double deltaTime = (currentTime - lastTime) / 1_000_000_000.0;
                lastTime = currentTime;

                /* Avoid one huge catch-up after a pause or delayed frame. */
                deltaTime = Math.min(deltaTime, maxFrameTime);
                accumulatedTime += deltaTime * simulationSpeed;

                /* Advance physics in constant-size steps, then display it. */
                while(accumulatedTime >= physicsTimeStep) {
                    sim.simulate();
                    accumulatedTime -= physicsTimeStep;
                }
                renderer.render();
            }
        }
    }

}

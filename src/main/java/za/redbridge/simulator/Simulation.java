package za.redbridge.simulator;

import org.jbox2d.common.Settings;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.World;
import org.jbox2d.common.Transform;

import java.util.Set;

import sim.engine.SimState;
import sim.engine.Stoppable;
import sim.engine.Repeat;
import sim.field.continuous.Continuous2D;
import sim.field.grid.SparseGrid2D;

import sim.util.Double2D;
import za.redbridge.simulator.config.SimConfig;
import za.redbridge.simulator.config.SchemaConfig;
import za.redbridge.simulator.factories.ResourceFactory;
import za.redbridge.simulator.factories.RobotFactory;
import za.redbridge.simulator.object.PhysicalObject;
import za.redbridge.simulator.object.RobotObject;
// import za.redbridge.simulator.object.TargetAreaObject;
import za.redbridge.simulator.object.ResourceObject;
import za.redbridge.simulator.object.WallObject;
import za.redbridge.simulator.object.DetectionPoint;
import za.redbridge.simulator.physics.SimulationContactListener;
import za.redbridge.simulator.portrayal.DrawProxy;
import za.redbridge.simulator.novelty.PhenotypeBehaviour;
import za.redbridge.simulator.paramTuning.*;
import za.redbridge.simulator.Main.SEARCH_MECHANISM;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;

/**
 * The main simulation state.
 *
 * Created by jamie on 2014/07/24.
 */
public class Simulation extends SimState {

    private static final float VELOCITY_THRESHOLD = 0.000001f;
    public static final float DISCR_GAP = 0.25f;

    private Continuous2D environment;
    private SparseGrid2D constructionEnvironment;
    private World physicsWorld;
    private PlacementArea placementArea;
    private DrawProxy drawProxy;

    private final SimulationContactListener contactListener = new SimulationContactListener();

    public static final float TIME_STEP = 1f / 10f;
    private static final int VELOCITY_ITERATIONS = 6;
    private static final int POSITION_ITERATIONS = 3;

    // private TargetAreaObject targetArea;
    private RobotFactory robotFactory;
    private ConstructionTask construction;
    private SchemaConfig schema;
    private final SimConfig config;

    private double averageDistancesOverRun = 0D;

    private boolean stopOnceCollected = true;

    private ResourceFactory resourceFactory;

    private final double [] parameters;

    private final Map<PhysicalObject,Repeat> stoppables;

    private final SEARCH_MECHANISM searchMechanism;

    private PhenotypeBehaviour simBehaviour;

    private ContToDiscrSpace discr;



    public Simulation(SimConfig config, RobotFactory robotFactory, double [] parameters, ResourceFactory resourceFactory, SEARCH_MECHANISM searchMechanism) {
        super(config.getSimulationSeed());
        this.config = config;
        this.robotFactory = robotFactory;
        Settings.velocityThreshold = VELOCITY_THRESHOLD;
        this.parameters = parameters;
        this.resourceFactory = resourceFactory;
        this.searchMechanism = searchMechanism;
        stoppables = new HashMap<>();

    }

    @Override
    public void start() {
        super.start();
        // discr.clearGrid();

        environment =
                new Continuous2D(1.0, config.getEnvironmentWidth(), config.getEnvironmentHeight());
        constructionEnvironment = new SparseGrid2D(20, 20);
        drawProxy = new DrawProxy(environment.getWidth(), environment.getHeight());
        environment.setObjectLocation(drawProxy, new Double2D());

        physicsWorld = new World(new Vec2());
        placementArea =
                new PlacementArea((float) environment.getWidth(), (float) environment.getHeight());
        placementArea.setSeed(System.nanoTime());
        schedule.reset();
        System.gc();

        physicsWorld.setContactListener(contactListener);

        // Create ALL the objects
        createWalls();

        robotFactory.placeInstances(placementArea.new ForType<>(), physicsWorld);

        schema = new SchemaConfig("configs/schemaConfig.yml", 10, 3);
        discr = new ContToDiscrSpace(20,20,1D,1D, DISCR_GAP, schema, config.getConfigNumber());
        resourceFactory.setResQuantity(schema.getResQuantity(config.getConfigNumber()));
        resourceFactory.placeInstances(placementArea.new ForType<>(), physicsWorld);
        construction = new ConstructionTask(schema,resourceFactory.getPlacedResources(),robotFactory.getPlacedRobots(),physicsWorld, (int)parameters[0], config.getConfigNumber(), environment.getWidth(), environment.getHeight());

        int cnt = 0;
        // Now actually add the objects that have been placed to the world and schedule
        for (PhysicalObject object : placementArea.getPlacedObjects()) {
            // if (object instanceof ResourceObject) {
            //     // ResourceObject res = (ResourceObject)object;
            //     // res.getBodyAngle();
            //     // DetectionPoint[] dps = res.getDPs();

            //     // for (int i = 0; i < dps.length; i++) {
            //     //     // for (int j = 0; j < 3; j++) {
            //     //         DetectionPoint dpI = new DetectionPoint(dps[i], 0);
            //     //         drawProxy.registerDrawable(dps[i].getPortrayal());
            //     //         schedule.scheduleRepeating(dpI);
            //     //     // }

            //     // }
            //     discr.getNearestDiscrPos(object.getBody().getPosition());
            // }
            drawProxy.registerDrawable(object.getPortrayal());
            // Repeat objRepeat = (Repeat)schedule.scheduleRepeating(object);
            schedule.scheduleRepeating(object);
            // stoppables.put(object,objRepeat);
            // System.out.println("Ordering for " + object + " = " + stoppables.get(object).getOrdering());
        }
        // stoppables.put(schedule.scheduleRepeating(construction));
        schedule.scheduleRepeating(construction);
        schedule.scheduleRepeating(simState -> physicsWorld.step(TIME_STEP, VELOCITY_ITERATIONS, POSITION_ITERATIONS));

    }

    /**
    Once a simulation run is finished, a behaviour characterization is then made and stored (only if novelty search is being used)
    **/
    @Override
    public void finish() {
        super.finish();
        if (searchMechanism == SEARCH_MECHANISM.NOVELTY || searchMechanism == SEARCH_MECHANISM.HYBRID) {
            construction.updateCZs();
            simBehaviour = new PhenotypeBehaviour(construction.getConstructionZones(), discr, construction.getOverallConstructionOrder(), construction.getResources(), construction.getRobots(), 10);
        }
        // else {
        //     // System.out.println("FINISH");

        //     // discr.printGrid();
        //     construction.updateCZs();
        //     getFitness();
        //     // construction.printSizes();
        // }
    }

    // Walls are simply added to environment since they do not need updating
    private void createWalls() {
        int environmentWidth = config.getEnvironmentWidth();
        int environmentHeight = config.getEnvironmentHeight();
        // Left
        Double2D pos = new Double2D(0, environmentHeight / 2.0);
        Double2D v1 = new Double2D(0, -pos.y);
        Double2D v2 = new Double2D(0, pos.y);
        WallObject wall = new WallObject(physicsWorld, pos, v1, v2);
        drawProxy.registerDrawable(wall.getPortrayal());

        // Right
        pos = new Double2D(environmentWidth, environmentHeight / 2.0);
        wall = new WallObject(physicsWorld, pos, v1, v2);
        drawProxy.registerDrawable(wall.getPortrayal());

        // Top
        pos = new Double2D(environmentWidth / 2.0, 0);
        v1 = new Double2D(-pos.x, 0);
        v2 = new Double2D(pos.x, 0);
        wall = new WallObject(physicsWorld, pos, v1, v2);
        drawProxy.registerDrawable(wall.getPortrayal());

        // Bottom
        pos = new Double2D(environmentWidth / 2.0, environmentHeight);
        wall = new WallObject(physicsWorld, pos, v1, v2);
        drawProxy.registerDrawable(wall.getPortrayal());
    }

    public void checkConstructionTask(){
        construction.checkSchema(0);
    }

    public Map<PhysicalObject,Repeat> getStoppables () {
        return stoppables;
    }

    public SEARCH_MECHANISM getSM() {
        return searchMechanism;
    }

    @Override
    public void setSeed(long seed) {
        super.setSeed(seed);
        config.setSimulationSeed(seed);
    }

    private double getRobotAvgPolygonArea() {
        Set<PhysicalObject> objects = placementArea.getPlacedObjects();
        double totalArea = 0.0;

        for (PhysicalObject object: objects) {
            if (object instanceof RobotObject) {
                totalArea += ((RobotObject) object).getAverageCoveragePolgygonArea();
            }
        }
        return totalArea/config.getObjectsRobots();
    }

    /** Get the environment (forage area) for this simulation. */
    public Continuous2D getEnvironment() {
        return environment;
    }

    public SparseGrid2D getConstructionEnvironment() {
        return constructionEnvironment;
    }

    public ContToDiscrSpace getDiscr() {
        return discr;
    }

    /**
    Get the behavioural characterization for this simulation run
    **/
    public PhenotypeBehaviour getSimBehaviour () {
        return simBehaviour;
    }

    /**
     * Run the simulation for the number of iterations specified in the config.
     */
    public void run() {
        final int iterations = (int)parameters[0];
        // System.out.println("About to start a phenotype test.");
        runForNIterations(iterations);
        // System.out.println(construction.getConstructionFitness());
        // return getFitness();
    }

    /**
     * Run the simulation for a certain number of iterations.
     * @param n the number of iterations
     */
    public void runForNIterations(int n) {
        start();

        for (int i = 0; i < n; i++) {
            schedule.step(this);

            if (stopOnceCollected && construction.isComplete()) {
                break;
            }
        }
        finish();
    }

    /** If true, this simulation will stop once all the resource objects have been collected. */
    public boolean isStopOnceCollected() {
        return stopOnceCollected;
    }

    /** If set true, this simulation will stop once all the resource objects have been collected. */
    public void setStopOnceCollected(boolean stopOnceCollected) {
        this.stopOnceCollected = stopOnceCollected;
    }

    public Vec2 getConstructionZoneCenter(int czNum) {
        if (construction == null) {
            return new Vec2 (0f, 0f);
        }
        else {
            return construction.getConstructionZoneCenter(czNum);
        }
    }

    public Vec2[] getConstructionZoneCenter() {
        if (construction == null) {
            Vec2[] blankReturn = {new Vec2 (0f, 0f)};
            return blankReturn;
        }
        else {
            return construction.getConstructionZoneCenter();
        }
    }

    public double getNumAdjacent () {
        return construction.getNumAdjacentResources();
    }

    public double getNumResources() {
        return resourceFactory.getPlacedResources().size();
    }

    public double getNumAs () {
        return construction.getNumAsConnected();
    }

    public double getNumBs () {
        return construction.getNumBsConnected();
    }

    public double getNumCs () {
        return construction.getNumCsConnected();
    }

    public double getNumCZsBuilt() {
        return construction.getConstructionZones().length;
    }

    /**
    return the score at this point in the simulation
    A - average distance between robot and closest resource to it
    B - average number of times that robots connected to resources
    C - average distance between resources
    D - the number of adjacent resources (within construction set)
    E - the number of adjacent resources that are in the correct schema
    F - average distance between resources and the construction starting area
    **/
    public double getFitness() {
        // System.out.println("GETTING FITNESS");
        // return targetArea.getFitnessStats();
        // System.out.println("Number of connected resources: " + construction.getNumConnectedResources());
        if (schedule.getSteps() == 0) {
            return 0;
        }
        else {
            // double [] weights = new double[6];
            double[] weights = new double[parameters.length - 1];
            for (int i = 1; i < parameters.length; i++) {
                weights[i-1] = parameters[i];
            }
            return construction.getObjectiveFitness(weights);
        }

    }

    /** Gets the progress of the simulation as a percentage */
    public double getProgressFraction() {
        return (double) schedule.getSteps() / (int)parameters[0];
    }

    /** Get the number of steps this simulation has been run for. */
    public long getStepNumber() {
        return schedule.getSteps();
    }

    /**
     * Launching the application from this main method will run the simulation in headless mode.
     */
    public static void main (String[] args) {
        doLoop(Simulation.class, args);
        System.exit(0);
    }
}

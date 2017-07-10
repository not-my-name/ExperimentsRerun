package za.redbridge.simulator;

import org.jbox2d.common.Settings;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.World;

import java.util.Set;

import sim.engine.SimState;
import sim.field.continuous.Continuous2D;
import sim.util.Double2D;
import za.redbridge.simulator.config.SimConfig;
import za.redbridge.simulator.factories.RobotFactory;
import za.redbridge.simulator.object.PhysicalObject;
import za.redbridge.simulator.object.RobotObject;
import za.redbridge.simulator.object.TargetAreaObject;
import za.redbridge.simulator.object.ResourceObject;
import za.redbridge.simulator.object.WallObject;
import za.redbridge.simulator.physics.SimulationContactListener;
import za.redbridge.simulator.portrayal.DrawProxy;
import java.util.ArrayList;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Iterator;
import za.redbridge.simulator.config.SchemaConfig;

import za.redbridge.simulator.NoveltyBehaviour;

//import za.redbridge.simulator.FitnessMonitor;
import za.redbridge.simulator.factories.ResourceFactory;

/**
 * The main simulation state.
 *
 * Created by jamie on 2014/07/24.
 */
public class Simulation extends SimState {

    private static final float VELOCITY_THRESHOLD = 0.000001f;
    public static final float DISCR_GAP = 0.25f;

    private Continuous2D environment;
    private World physicsWorld;
    private PlacementArea placementArea;
    private DrawProxy drawProxy;

    private final SimulationContactListener contactListener = new SimulationContactListener();

    private static final float TIME_STEP = 1f / 10f;
    private static final int VELOCITY_ITERATIONS = 6;
    private static final int POSITION_ITERATIONS = 3;

    private TargetAreaObject targetArea;
    private RobotFactory robotFactory;
    private ConstructionTask construction;
    private final SimConfig config;

    private SchemaConfig schema;

    private boolean stopOnceCollected = true;

    private double averageDistancesOverRun = 0D;

    //private FitnessMonitor fitnessMonitor;

    private int schemaConfigNum;
    private ResourceFactory resourceFactory;
    private ContToDiscrSpace discreteGrid;

    private boolean performingNovelty;
    private NoveltyBehaviour simBehaviour;

    //private final RobotObject[] existingRobots;

    public Simulation(SimConfig config, RobotFactory robotFactory, ResourceFactory resourceFactory, boolean isNovelty) {

        super(config.getSimulationSeed());
        this.config = config;
        this.robotFactory = robotFactory;
        this.resourceFactory = resourceFactory;
        Settings.velocityThreshold = VELOCITY_THRESHOLD;
        this.performingNovelty = isNovelty;
        schemaConfigNum = 0;
    }

    @Override
    public void start() {

        super.start();

        environment =
                new Continuous2D(1.0, config.getEnvironmentWidth(), config.getEnvironmentHeight());
        drawProxy = new DrawProxy(environment.getWidth(), environment.getHeight());
        environment.setObjectLocation(drawProxy, new Double2D());

        physicsWorld = new World(new Vec2());
        placementArea =
                new PlacementArea((float) environment.getWidth(), (float) environment.getHeight());
        placementArea.setSeed(config.getSimulationSeed());
        schedule.reset();
        System.gc();

        physicsWorld.setContactListener(contactListener);

        // Create ALL the objects
        createWalls();
        robotFactory.placeInstances(placementArea.new ForType<>(), physicsWorld, config.getTargetAreaPlacement());

        schema = new SchemaConfig("configs/schemaConfig.yml", 10, 3);
        //schema = new SchemaConfig("configs/schemaConfig.yml", 3, 3);
        discreteGrid = new ContToDiscrSpace(20,20,1D,1D, DISCR_GAP, schema, schemaConfigNum);
        resourceFactory.setResQuantity(schema.getResQuantity(config.getConfigNumber()));
        resourceFactory.placeInstances(placementArea.new ForType<>(), physicsWorld);
        construction = new ConstructionTask(schema,resourceFactory.getPlacedResources(),robotFactory.getPlacedRobots(),physicsWorld, config.getConfigNumber(), environment.getWidth(), environment.getHeight());

        // Now actually add the objects that have been placed to the world and schedule
        int count = 0;
        for (PhysicalObject object : placementArea.getPlacedObjects()) {

            drawProxy.registerDrawable(object.getPortrayal());
            schedule.scheduleRepeating(object);
        }

        schedule.scheduleRepeating(construction);

        schedule.scheduleRepeating(simState ->
            physicsWorld.step(TIME_STEP, VELOCITY_ITERATIONS, POSITION_ITERATIONS)
        );
    }

    /**
    Once a simulation run is finished, a behaviour characterization is then made and stored (only if novelty search is being used)
    **/
    @Override
    public void finish() {

        super.finish();

	//ArrayList<RobotObject> tempBots = robotFactory.getPlacedRobots();
        //ArrayList<ResourceObject> tempResources = resourceFactory.getPlacedResources();

        construction.updateConstructionZones();

        //THIS IS FOR THE OBJECTIVE FITNESS
        //Behaviour behaviour = new Behaviour(construction, schemaConfigNum);

	//System.out.println("Simulation: The stats after the demo");
	//System.out.println("Simulation: num A = " + behaviour.getConnectedA());
	//System.out.println("Simulation: num B = " + behaviour.getConnectedB());
	//System.out.println("Simulation: num C = " + behaviour.getConnectedC());

        //construction.updateConstructionZones();
    }

    public void setSchemaConfigNumber(int i) {
        schemaConfigNum = i;
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
        System.out.println("Simulation checkConstructionTask: THIS METHOD IS BEING CALLED FROM SOMEWHERE, DONT THINK IT SHOULD");
        //construction.checkSchema(0);
    }

    //create target area
    private void createTargetArea() {
        int environmentWidth = config.getEnvironmentWidth();
        int environmentHeight = config.getEnvironmentHeight();

        final int width, height;
        final Vec2 position;

        width = 3;
        height = width;
        position = new Vec2(8f, 16f);

        // targetArea = new TargetAreaObject(physicsWorld, position, width, height,
        //         config.getResourceFactory().getTotalResourceValue(), config.getSimulationIterations());

        // Add target area to placement area (trust that space returned since nothing else placed
        // yet).
        PlacementArea.Space space = placementArea.getRectangularSpace(width, height, position, 0f);
        placementArea.placeObject(space, targetArea);
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

    /**
    remeber that some of these might need to be changed if we use the postiteration method that josh mentioned
    */

    public NoveltyBehaviour runNovel() {
        final int iterations = config.getSimulationIterations();
        return noveltySimulation(iterations);
    }

    private NoveltyBehaviour noveltySimulation(int n) {

        start();

        for(int i = 0; i < n; i++) {
            schedule.step(this);
        }

        //get the positions of the robots at the end of the simulation
        ArrayList<RobotObject> currentRobots = robotFactory.getPlacedRobots();
        //get the final positions of the resources in the simulation
        ArrayList<ResourceObject> currentResources = resourceFactory.getPlacedResources();

        //updating construction zones to join neighboursing ones before calculating the score
        construction.updateConstructionZones();

        //creating the noveltyBehaviour object to be used in the fitness calculation
        NoveltyBehaviour noveltyBehaviour = new NoveltyBehaviour(currentRobots, currentResources, construction);

        finish();

        return noveltyBehaviour;
    }

        /**
     * Run the simulation for the number of iterations specified in the config.
     */
    public Behaviour runObjective() {
        //System.out.println("Simulation runObjective(): running method");
        final int iterations = 15000;
        return objectiveSimulation(iterations);
    }

    //just a temp method to test another way of implementing the fitness functions
    private Behaviour objectiveSimulation(int n) {

        start();

        for(int i = 0; i < n; i++) {

            schedule.step(this);
        }

        ArrayList<RobotObject> tempBots = robotFactory.getPlacedRobots();
        ArrayList<ResourceObject> tempResources = resourceFactory.getPlacedResources();

        construction.updateConstructionZones();

        //THIS IS FOR THE OBJECTIVE FITNESS
        Behaviour behaviour = new Behaviour(construction, schemaConfigNum);

        finish();

        return behaviour;

    }

    public int getTotalNumResources() {
        return resourceFactory.getPlacedResources().size();
    }

    public int [] getResTypeCount() {
        if(schema == null) {
            System.out.println("Simulation: the schema is null");
        }
        return schema.getResQuantity(schemaConfigNum);
    }

    public ContToDiscrSpace getDiscreteGrid() {
        return discreteGrid;
    }

    private float calculateDistance(Vec2 originLocation, Vec2 destLocation) {

        double firstX = originLocation.x;
        double firstY = originLocation.y;

        double secondX = destLocation.x;
        double secondY = destLocation.y;

        //System.out.println("FitnessMonitor: the locations = " + firstX + "," + firstY + " -> " + secondX + "," + secondY);

        float distance = (float) Math.sqrt(
                        Math.pow(firstX-secondX, 2) +
                        Math.pow(firstY-secondY, 2));

        //System.out.println("FitnessMonitor: distance calculated = " + distance);

        return distance;

    }

    // /**
    //  * Run the simulation for a certain number of iterations.
    //  * @param n the number of iterations
    //  */
    // public void runForNIterations(int n) {
    //     start();
    //     double aveDistOverNIterations = 0D;

    //     for (int i = 0; i < n; i++) {

    //         schedule.step(this);

    //         for(int k = 0; k < robotFactory.getPlacedRobots().size(); k++) {
    //             Vec2 before = robotFactory.getPlacedRobots().get(k).getPreviousPosition();
    //             Vec2 after = robotFactory.getPlacedRobots().get(k).getBody().getPosition();
    //             //fitnessMonitor.incrementDistanceTravelled(before, after);
    //         }

    //     }
    //     ArrayList<RobotObject> tempBots = robotFactory.getPlacedRobots();
    //     //fitnessMonitor.savePickupCounts(tempBots);
    //     //fitnessMonitor.setPlacedResources(resourceFactory.getPlacedResources()); //retrieving the final positions of the resources after the simulation
    //     finish();
    // }

    public boolean allResourcesCollected() {
        return resourceFactory.getNumberOfResources()
                == targetArea.getNumberOfContainedResources();
    }

    /** If true, this simulation will stop once all the resource objects have been collected. */
    public boolean isStopOnceCollected() {
        return stopOnceCollected;
    }

    /** If set true, this simulation will stop once all the resource objects have been collected. */
    public void setStopOnceCollected(boolean stopOnceCollected) {
        this.stopOnceCollected = stopOnceCollected;
    }

    /** Gets the progress of the simulation as a percentage */
    public double getProgressFraction() {
        return (double) schedule.getSteps() / config.getSimulationIterations();
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

    /**
    Method to store the average distances over n iterations
    **/
    public void storeDistances(double distances, int n) {
        this.averageDistancesOverRun = distances/n;
    }
}

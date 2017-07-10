package za.redbridge.simulator.phenotype;

import org.jbox2d.common.Transform;

import java.awt.Graphics2D;
import java.util.List;
import java.util.Map;

import sim.portrayal.DrawInfo2D;
import sim.util.Double2D;
import za.redbridge.simulator.config.SimConfig;
import za.redbridge.simulator.object.RobotObject;
import za.redbridge.simulator.phenotype.heuristics.CollisionAvoidanceHeuristic;
import za.redbridge.simulator.phenotype.heuristics.HeuristicSchedule;
import za.redbridge.simulator.phenotype.heuristics.PickupHeuristic;
import za.redbridge.simulator.portrayal.Drawable;
import za.redbridge.simulator.sensor.AgentSensor;
import za.redbridge.simulator.sensor.CollisionSensor;
import za.redbridge.simulator.sensor.PickupSensor;
import za.redbridge.simulator.khepera.WallCollisionSensor;
import za.redbridge.simulator.phenotype.heuristics.CollisionHeuristic;

/**
 * Phenotype that applies any relevant heuristics first before falling through to the controller
 * phenotype.
 * Created by shsu on 2014/08/27.
 */
public class HeuristicPhenotype implements Phenotype, Drawable {

    private static final boolean PICKUP_HEURISTIC_ENABLED = true;
    private static final boolean COLLISION_HEURISTIC_ENABLED = false;
    private static final boolean WALL_HEURISTIC_ENABLED = true;

    private static final float PICKUP_SENSOR_WIDTH = 0.1f;
    private static final float PICKUP_SENSOR_HEIGHT = 0.2f;

    // Make sure this is > robot radius
    private static final float COLLISION_SENSOR_RADIUS = 0.55f;

    private final Phenotype controllerPhenotype;
    private final RobotObject robot;
    private final HeuristicSchedule schedule;

    private CollisionSensor collisionSensor;
    private PickupSensor pickupSensor;
    private WallCollisionSensor wallSensor;

    public HeuristicPhenotype(Phenotype controllerPhenotype, RobotObject robot) {
        this.controllerPhenotype = controllerPhenotype;
        this.robot = robot;

        schedule = new HeuristicSchedule();
        initHeuristics(robot);
    }

    private void initHeuristics(RobotObject robot) {
        if (COLLISION_HEURISTIC_ENABLED) {
            collisionSensor = new CollisionSensor(COLLISION_SENSOR_RADIUS);
            collisionSensor.attach(robot);
            schedule.addHeuristic(new CollisionAvoidanceHeuristic(collisionSensor, robot));
        }

        if (PICKUP_HEURISTIC_ENABLED) {
            pickupSensor = new PickupSensor(PICKUP_SENSOR_WIDTH, PICKUP_SENSOR_HEIGHT);
            pickupSensor.attach(robot);
            schedule.addHeuristic(new PickupHeuristic(pickupSensor, robot));
        }

        if(WALL_HEURISTIC_ENABLED){
            wallSensor = new WallCollisionSensor(0, 0, 1);
            wallSensor.attach(robot);
            schedule.addHeuristic(new CollisionHeuristic(wallSensor, robot));
        }
    }

    @Override
    public HeuristicPhenotype clone() {
        return new HeuristicPhenotype(controllerPhenotype.clone(), robot);
    }

    @Override
    public void configure(Map<String, Object> phenotypeConfigs) {
        // TODO: Make heuristic configurable from file
    }

    @Override
    public List<AgentSensor> getSensors()
    {
        return null;
    }

    @Override
    public Double2D step(List<List<Double>> list) {
        // check if heuristic decision
        Double2D wheelDrives = schedule.step(list);

        if (wheelDrives == null) {
            robot.setColor(null);
            // else move using controller
            wheelDrives = controllerPhenotype.step(list);
        }

        return wheelDrives;
    }

    @Override
    public void draw(Object object, Graphics2D graphics, DrawInfo2D info) {
        if (COLLISION_HEURISTIC_ENABLED) {
            collisionSensor.getPortrayal().draw(object, graphics, info);
        }
        if (PICKUP_HEURISTIC_ENABLED) {
            pickupSensor.getPortrayal().draw(object, graphics, info);
        }
        if(WALL_HEURISTIC_ENABLED){
            wallSensor.getPortrayal().draw(object, graphics, info);
        }
    }

    @Override
    public void setTransform(Transform transform) {
        if (COLLISION_HEURISTIC_ENABLED) {
            collisionSensor.getPortrayal().setTransform(transform);
        }
        if (PICKUP_HEURISTIC_ENABLED) {
            pickupSensor.getPortrayal().setTransform(transform);
        }
        if(WALL_HEURISTIC_ENABLED){
            wallSensor.getPortrayal().setTransform(transform);
        }
    }

    public String getActiveHeuristic() { return schedule.getActiveHeuristic(); }
}

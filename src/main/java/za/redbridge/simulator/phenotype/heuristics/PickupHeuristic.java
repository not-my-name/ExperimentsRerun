package za.redbridge.simulator.phenotype.heuristics;

import java.awt.Color;
import java.util.List;

import org.omg.CORBA.Current;
import sim.util.Double2D;
import za.redbridge.simulator.config.SimConfig;
import za.redbridge.simulator.object.ResourceObject;
import za.redbridge.simulator.object.RobotObject;
import za.redbridge.simulator.sensor.PickupSensor;
import za.redbridge.simulator.sensor.Sensor;


import static za.redbridge.simulator.Utils.wrapAngle;

/**
 * Heuristic for picking up things and carrying them to target area
 * Created by racter on 2014/09/01.
 */
public class PickupHeuristic extends Heuristic {

    private static final Color COLOR = Color.GREEN;
    private static final boolean ENABLE_PICKUP_POSITIONING = true;

    protected final PickupSensor pickupSensor;

    private int SimStepCount = 0;
    private final int MaxStepCounter = 350; // I think this is like 2 seconds
    private int CurrentNumPushingRobots = 0;
    private ResourceObject currentResource = null;

    private ResourceObject stuckResource = null;

    public PickupHeuristic(PickupSensor pickupSensor, RobotObject robot) {

        super(robot);
        this.pickupSensor = pickupSensor;
        setPriority(2);
    }

    @Override
    public Double2D step(List<List<Double>> list) {

        // Check for a resource in the sensor
        ResourceObject resource = pickupSensor.sense().map(o -> (ResourceObject) o.getObject()).orElse(null);

        if( (resource != null) && (resource == stuckResource) ) {
            return wheelDriveForTargetAngle(awayResourceTargetAngle());
        }

        if (resource == null && !robot.isBoundToResource()) {
            // no longer has resource, reset the counter
            resetCounter(currentResource);
            currentResource = null;
            return null; // No viable resource, nothing to do
        }else if(resource != null && resource.canBePickedUp()){
            // set the current resource, this is the one the robot is about the pick up (becomes null later)
            currentResource = resource;
            // Try pick it up
            if (resource.tryPickup(robot)) {

                robot.setBoundToResource(true);
                stuckResource = null;

                resetCounter(currentResource);
                return null;
            }
        }

        if (robot.isBoundToResource()) {
            // check that the robot has not been holding onto the resource for too long (or it can hold into it
            // for long if there are enough pushers)
            if(SimStepCount < MaxStepCounter){
                if(currentResource.pushedByMaxRobots()){
                    // Go for the target area if we've managed to attach to a resource
                    return null;
                }else{
                    // reset the counter if a new robot has attached to the resource
                    updateCounter(currentResource);
                    return null;
                }
            }else if(SimStepCount >= MaxStepCounter){
                // been holding this resourse for too long, detach from it and drive away
                //System.out.println("PickupHeuristic: the robot is about to detach");
                currentResource.forceDetach();
                resetCounter(currentResource);

                stuckResource = currentResource;
                robot.setBoundToResource(false);

                return wheelDriveForTargetAngle(awayResourceTargetAngle());
            }
        }

        if (!resource.canBePickedUp()) {
            // no longer has resource, reset the counter
            resetCounter(currentResource);
            //  chuck : todo Check if sensor directly above target area
            return null; // No viable resource, nothing to do
        }

        return null;
    }

    public void resetCounter(ResourceObject resource){
        SimStepCount = 0;
        if(resource != null){ // set to the remaining pushing robots
            CurrentNumPushingRobots = resource.getNumberPushingRobots();
        }else{ // reset if no resource
            CurrentNumPushingRobots = 0;
        }
    }

    public void updateCounter(ResourceObject resource){
        if(resource.getNumberPushingRobots() != CurrentNumPushingRobots){
            SimStepCount = 0;
            CurrentNumPushingRobots = resource.getNumberPushingRobots();
        }else{
            SimStepCount ++;
        }
    }

    @Override
    Color getColor() {
        return COLOR;
    }

    @Override
    public Sensor getSensor() {
        return pickupSensor;
    }

    protected double awayResourceTargetAngle( ){
        double robotAngle = robot.getBody().getAngle();
        return wrapAngle(-robotAngle);
    }
}

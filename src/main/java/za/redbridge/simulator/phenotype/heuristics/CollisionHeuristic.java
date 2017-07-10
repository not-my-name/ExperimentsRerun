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
import za.redbridge.simulator.khepera.WallCollisionSensor;

import static za.redbridge.simulator.Utils.wrapAngle;

/**
 * Heuristic for moving away from walls and constructed resources
 */
public class CollisionHeuristic extends Heuristic {

    private static final Color COLOR = Color.RED;
    protected final WallCollisionSensor collisionSensor;

    public CollisionHeuristic(WallCollisionSensor collisionSensor, RobotObject robot) {
        super(robot);
        this.collisionSensor = collisionSensor;
        setPriority(1);
    }

    @Override
    public Double2D step(List<List<Double>> list) {
        List<Double> output = collisionSensor.sense();

        if(output.get(0) == 1.0){
            return wheelDriveForTargetAngle(-Math.PI);
        }
        else{
            return null;
        }
    }


    @Override
    Color getColor() {
        return COLOR;
    }

    @Override
    public Sensor getSensor() {
        return collisionSensor;
    }

    protected double awayResourceTargetAngle( ){
        double robotAngle = robot.getBody().getAngle();
        System.out.println("Robot angle: "+robotAngle);
        System.out.println("Wrapped angle: "+wrapAngle(-robotAngle));
        return wrapAngle(-robotAngle);
    }
}

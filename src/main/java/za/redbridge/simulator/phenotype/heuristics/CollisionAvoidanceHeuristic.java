package za.redbridge.simulator.phenotype.heuristics;

import org.jbox2d.common.Vec2;

import java.awt.Color;
import java.util.List;
import java.util.Optional;

import sim.util.Double2D;
import za.redbridge.simulator.object.RobotObject;
import za.redbridge.simulator.sensor.ClosestObjectSensor;
import za.redbridge.simulator.sensor.CollisionSensor;
import za.redbridge.simulator.sensor.Sensor;

import za.redbridge.simulator.object.ResourceObject;
import za.redbridge.simulator.object.PhysicalObject;

import static za.redbridge.simulator.Utils.jitter;

/**
 * Created by racter on 2014/09/01.
 */
public class CollisionAvoidanceHeuristic extends Heuristic {

    private static final Color COLOR = Color.RED;

    protected final CollisionSensor collisionSensor;

    private Vec2 targetPoint = null;

    public CollisionAvoidanceHeuristic(CollisionSensor collisionSensor, RobotObject robot) {
        super(robot);
        this.collisionSensor = collisionSensor;

        setPriority(4);
    }

    @Override
    public Double2D step(List<List<Double>> list) {
        Optional<ClosestObjectSensor.ClosestObject> collision = collisionSensor.sense();

        PhysicalObject resource =
            collisionSensor.sense().map(o -> (PhysicalObject) o.getObject()).orElse(null);
        if (resource == null || !(resource instanceof ResourceObject)) {
            return collision.map(o -> o.getVectorToObject())
                .map(o -> wheelDriveForTargetPosition(jitter(o.negate(), 0.2f)))
                .orElse(null);
        } else {
            Vec2 newPosition = nextStep((ResourceObject) resource);
            jitter(newPosition, 0.1f);

            if (newPosition != null) {
                return wheelDriveForTargetPosition(robot.getBody().getLocalPoint(newPosition));
            }
        }
        return null;
    }

    private Vec2 nextStep(ResourceObject resource) {
        Vec2 robotPosition = robot.getBody().getPosition();

        ResourceObject.Side stickySide = resource.getStickySide();
        ResourceObject.Side robotSide = resource.getSideClosestToPoint(robotPosition);

        // same side or side not yet set
        if (stickySide == robotSide || stickySide == null) {
            // Head for the anchor point
            return calculateTargetPoint(resource);
        } else { // Different side, navigate to corner
            final Vec2 corner;
            float halfWidth = (float) resource.getWidth() / 2;
            float halfHeight = (float) resource.getHeight() / 2;
            if (robotSide == ResourceObject.Side.LEFT) {
                if (stickySide == ResourceObject.Side.TOP) {
                    // Top left corner
                    corner = new Vec2(-halfWidth, halfHeight);
                } else {
                    // Bottom left corner
                    corner = new Vec2(-halfWidth, -halfHeight);
                }
            } else if (robotSide == ResourceObject.Side.RIGHT) {
                if (stickySide == ResourceObject.Side.TOP) {
                    // Top right
                    corner = new Vec2(halfWidth, halfHeight);
                } else {
                    // Bottom right
                    corner = new Vec2(halfWidth, -halfHeight);
                }
            } else if (robotSide == ResourceObject.Side.TOP) {
                if (stickySide == ResourceObject.Side.LEFT) {
                    // Top left
                    corner = new Vec2(-halfWidth, halfHeight);
                } else {
                    // Top right
                    corner = new Vec2(halfWidth, halfHeight);
                }
            } else if (robotSide == ResourceObject.Side.BOTTOM) {
                if (stickySide == ResourceObject.Side.LEFT) {
                    // Bottom left
                    corner = new Vec2(-halfWidth, -halfHeight);
                } else {
                    // Bottom right
                    corner = new Vec2(halfWidth, -halfHeight);
                }
            } else {
                throw new RuntimeException("Robot side not found!");
            }

            // "Pad" the corner by radius x radius
            float radius = robot.getRadius();
            corner.addLocal(Math.copySign(radius, corner.x), Math.copySign(radius, corner.y));

            // Transform relative to resource
            resource.getBody().getLocalPointToOut(corner, corner);

            return corner;
        }

    }

    private Vec2 calculateTargetPoint(ResourceObject resource) {
        // Remember where we're headed, don't get stuck choosing sides
        if (targetPoint != null) {
            return targetPoint;
        }

        Vec2 position = robot.getBody().getPosition();
        // Get the anchor point and side normal
        ResourceObject.AnchorPoint anchorPoint = resource.getClosestAnchorPoint(position);
        if (anchorPoint == null) { // TODO: Shouldn't happen but does on occasion
            return null;
        }

        Vec2 normal = resource.getNormalToSide(anchorPoint.getSide());

        // Get a distance away from the anchor point
        targetPoint =
            normal.mulLocal(robot.getRadius()).addLocal(anchorPoint.getPosition());
        return targetPoint;
    }

    @Override
    Color getColor() {
        return COLOR;
    }

    @Override
    public Sensor getSensor() {
        return collisionSensor;
    }

}

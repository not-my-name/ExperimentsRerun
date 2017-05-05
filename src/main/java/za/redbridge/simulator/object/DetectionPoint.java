package za.redbridge.simulator.object;

import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.World;
import org.jbox2d.dynamics.joints.Joint;
import org.jbox2d.dynamics.joints.JointDef;
import org.jbox2d.dynamics.joints.WeldJointDef;
import org.jbox2d.collision.AABB;
import org.jbox2d.collision.shapes.MassData;
import org.jbox2d.common.Transform;

import java.awt.Color;
import java.awt.Paint;
import java.time.temporal.ValueRange;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.Arrays;

import sim.engine.SimState;
import za.redbridge.simulator.Simulation;
import za.redbridge.simulator.physics.BodyBuilder;
import za.redbridge.simulator.physics.FilterConstants;
import za.redbridge.simulator.portrayal.PolygonPortrayal;
import za.redbridge.simulator.portrayal.Portrayal;
import za.redbridge.simulator.portrayal.DPPortrayal;
import za.redbridge.simulator.portrayal.RectanglePortrayal;
import za.redbridge.simulator.portrayal.STRTransform;
import za.redbridge.simulator.physics.AABBUtil;
import sim.engine.Steppable;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;

import sim.util.Double2D;

public class DetectionPoint implements Steppable{
    private static final long serialVersionUID = 1L;
    
    private final Vec2 [] positions;
    private boolean collided;
    private Vec2 worldPosition = null;
    private final Body body;
    private Portrayal portrayal;
    private int posNum;
    private final int side;

    // public DetectionPoint () {

    // }

    public DetectionPoint(Vec2 [] positions, Body resBody, int posNum, int sideNum){
        this.positions = positions;
        this.body = resBody;
        double width = (double)Math.abs(positions[1].x - positions[2].x);
        double height = 0.1D;
        portrayal = createPortrayal(width, height);
        this.posNum = posNum;
        this.side = sideNum;
    }

    public DetectionPoint(DetectionPoint dp, int posNum) {
        this.positions = dp.getPositions();
        this.body = dp.getBody();
        portrayal = dp.getPortrayal();
        this.posNum = posNum;
        this.side = dp.getSide();
    }

    public void markColliding() {
        collided = true;
    }

    public Vec2[] getPositions() {
        return positions;
    }

    public int getSide() {
        return side;
    }

    // public Vec2 [] getRelativePositions(Vec2 resourcePos){
    //     Vec2 [] relativePositions = new Vec2 [3];
    //     for (int i = 0; i < positions.length; i++) {
    //         relativePositions[i] = positions[i].add(resourcePos);
    //     }
    //     return relativePositions;
    // }

    public Vec2 [] getRelativePositions(){
        // Vec2 [] relativePositions = new Vec2 [3];
        // for (int i = 0; i < positions.length; i++) {
        //     relativePositions[i] = positions[i].add(body.getPosition());
        // }
        // return relativePositions;

        Vec2 [] relativePositions = new Vec2 [3];
        Transform bodyXFos = body.getTransform();
        for (int i = 0; i < positions.length; i++) {
            relativePositions[i] = Transform.mul(bodyXFos, positions[i]);
        }
        return relativePositions;
    }

    public Body getBody() {
        return body;
    }

    @Override
    public void step(SimState simState) {
        Simulation s = (Simulation) simState;
        portrayal.setTransform(body.getTransform());
        Vec2[] relPos = getRelativePositions();

        // These lines register the object's position to the model so that the MASON portrayals move with the simulator objects
        float objX = relPos[posNum].x;
        float objY = (float)s.getEnvironment().getHeight() - relPos[posNum].y;

        s.getEnvironment().setObjectLocation(this, new Double2D(objX, objY));
        // s.getEnvironment().setObjectLocation(new DetectionPoint(), new Double2D(objX + 2D, objY));
        // for (Object o : s.getEnvironment().getAllObjects()) {
        //     System.out.println(o);
        // }
    }

    public boolean isNearCenter(Vec2 otherResPos) {
        Vec2[] relPos = getRelativePositions();
        float distBetween = relPos[0].sub(otherResPos).length();
        if (distBetween < (0.1f + Simulation.DISCR_GAP)) {
            return true;
        }
        else {
            return false;
        }
    }

    public Vec2 [] getWorldPositions(ResourceObject r) {
        Vec2 [] worldPositions = new Vec2 [3];
        if (worldPosition == null) {
            for (int i = 0; i < positions.length; i++) {
                worldPosition = r.getBody().getWorldPoint(positions[i]);
            }
            
        }
        return worldPositions;
    }

    public boolean isTaken() {
        return collided;
    }

    public Portrayal getPortrayal() {
        return portrayal;
    }

    public void setPosNum (int i) {
        this.posNum = i;
    }

    protected Portrayal createPortrayal(double width, double height) {

        return new DPPortrayal(width, height);
    }

    // private class DPPortrayal extends PolygonPortrayal {

    //     // public DPPortrayal(Paint paint, boolean filled) {
    //     //     super(2, paint, filled);

    //     //     Vec2[] dps = getRelativePositions(body.getPosition());

    //     //     final float width = 0.1f;
    //     //     final float height = 0.1f;

    //     //     // final float dy = (float) getHeight() * 0.3f;

    //     //     // System.out.println(aabb);
    //     //     // final float width = aabb.upperBound.x - aabb.lowerBound.x;
    //     //     // final float height = aabb.upperBound.y - aabb.lowerBound.y;

    //     //     float halfWidth = width / 2;
    //     //     float halfHeight = height / 2;
    //     //     // // vertices[0].set(-halfWidth, -halfHeight - dy);
    //     //     // // vertices[1].set(halfWidth, -halfHeight - dy);
    //     //     // // vertices[2].set(halfWidth, halfHeight - dy);
    //     //     // // vertices[3].set(-halfWidth, halfHeight - dy);
    //     //     vertices[0].set(dps[1].x, dps[1].y);
    //     //     vertices[1].set(dps[2].x, dps[2].y);
    //     //     // vertices[2].set(halfWidth, halfHeight);
    //     //     // vertices[3].set(-halfWidth, halfHeight);
    //     // }
    //     private transient Rectangle2D preciseRect;

    //     private final double width;
    //     private final double height;

    //     public DPPortrayal(double width, double height) {
    //         this(width, height, Color.BLACK, true);
    //     }

    //     public DPPortrayal(double width, double height, Paint paint, boolean filled) {
    //         super(4, paint, filled);
    //         this.width = width;
    //         this.height = height;

    //         float halfWidth = (float) (width / 2);
    //         float halfHeight = (float) (height / 2);
    //         vertices[0].set(-halfWidth, -halfHeight);
    //         vertices[1].set(halfWidth, -halfHeight);
    //         vertices[2].set(halfWidth, halfHeight);
    //         vertices[3].set(-halfWidth, halfHeight);
    //     }

    //     public double getWidth() {
    //         return width;
    //     }

    //     public double getHeight() {
    //         return height;
    //     }

    //     @Override
    //     protected void drawPrecise(Graphics2D graphics, STRTransform transform,
    //             boolean transformUpdated) {
    //         if (preciseRect == null) {
    //             preciseRect = new Rectangle2D.Double(-width / 2.0, -height / 2.0, width, height);
    //         }

    //         Shape transformedShape = transform.getAffineTransform().createTransformedShape(preciseRect);

    //         if (filled) {
    //             graphics.fill(transformedShape);
    //         } else {
    //             graphics.draw(transformedShape);
    //         }
    //     }
    // }

    // //ALIGNMENT
    // public boolean isWithinResource (Vec2 r1Pos, Vec2 r2Pos, float r2Width) {
    //     float r1DPToR2 = r2Pos.sub(this.getRelativePosition(r1Pos)).length();
    //     if (r1DPToR2 < r2Width/2) {
    //         return true;
    //     }
    //     else {
    //         return false;
    //     }
    // }
}
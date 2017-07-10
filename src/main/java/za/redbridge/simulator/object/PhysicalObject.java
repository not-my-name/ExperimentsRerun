package za.redbridge.simulator.object;

import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.common.Vec2;

import sim.engine.SimState;
import sim.engine.Steppable;
import za.redbridge.simulator.portrayal.Portrayal;

import sim.util.Double2D;
import za.redbridge.simulator.portrayal.*;
import za.redbridge.simulator.*;
import java.awt.*;

/**
 * Created by jamie on 2014/07/25.
 */
public class PhysicalObject implements Steppable {

    private final Portrayal portrayal;
    private final Body body;

    public PhysicalObject(Portrayal portrayal, Body body) {
        if (portrayal == null || body == null) {
            throw new NullPointerException("Portrayal and body must not be null");
        }

        this.portrayal = portrayal;
        this.body = body;

        // Make this body trackable
        this.body.setUserData(this);

        // Make sure we're drawn in the right place
        this.portrayal.setTransform(this.body.getTransform());
    }

    @Override
    public void step(SimState simState) {
        // Nothing to update if we're static or sleeping
        if (body.getType() == BodyType.STATIC || !body.isAwake()) {
            return;
        }

        Simulation s = (Simulation) simState;
        portrayal.setTransform(body.getTransform());

        // These lines register the object's position to the model so that the MASON portrayals move with the simulator objects
        float objX = Vec2.dot(this.body.getPosition(), new Vec2(1.0f, 0.0f));
        float objY = (float)s.getEnvironment().getHeight() - Vec2.dot(this.body.getPosition(), new Vec2(0.0f, 1.0f));
        s.getEnvironment().setObjectLocation(this, new Double2D(objX, objY));
    }

    public Body getBody() {
        return body;
    }

    public Portrayal getPortrayal() {
        return portrayal;
    }
}

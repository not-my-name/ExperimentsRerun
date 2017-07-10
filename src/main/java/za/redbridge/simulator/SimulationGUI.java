package za.redbridge.simulator;

import sim.display.Controller;
import sim.display.Display2D;
import sim.display.GUIState;
import sim.engine.SimState;
import sim.portrayal.continuous.ContinuousPortrayal2D;
import sim.portrayal.*;
import sim.portrayal.simple.*;
import sim.util.Bag;
import za.redbridge.simulator.portrayal.*;
import za.redbridge.simulator.object.*;
import java.awt.geom.Ellipse2D;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Created by jamie on 2014/07/24.
 */

//this should be ExperimentGUI
public class SimulationGUI extends GUIState {

    private Display2D display;
    private JFrame displayFrame;
    private ContinuousPortrayal2D environmentPortrayal = new ContinuousPortrayal2D();

    public SimulationGUI(SimState state) {
        super(state);
    }

    @Override
    public void init (Controller controller) {
        super.init(controller);

        display = new Display2D(600, 600, this);

        display.setClipping(false);

        displayFrame = display.createFrame();
        displayFrame.setTitle("CASAIRT Simulation");

        controller.registerFrame(displayFrame);

        displayFrame.setVisible(true);
        display.attach(environmentPortrayal, "Construction Area");
    }

    @Override
    public void start() {
        super.start();

        // Set the portrayal to display the environment
        final Simulation simulation = (Simulation) state;
        environmentPortrayal.setField(simulation.getEnvironment());

        /**
        Adds invisible MASON portrayals for each instance of the resource, robot and targetArea objects
        => double click on an object in the simulation and it'll show the inspector for that object
        **/
        environmentPortrayal.setPortrayalForClass(ResourceObject.class, new RectanglePortrayal2D(new Color(0,0,0,0)));
        environmentPortrayal.setPortrayalForClass(RobotObject.class, new OvalPortrayal2D(new Color(255,255,255,0)));
        environmentPortrayal.setPortrayalForClass(TargetAreaObject.class, new RectanglePortrayal2D(new Color(255,255,255,0)));

        // Set up the display
        display.reset();
        display.setBackdrop(Color.white);
        display.repaint();
    }

    @Override
    public boolean step() {
        final Simulation simulation = (Simulation) state;
        // Checks if all resources are collected and stops the simulation
        // if (simulation.allResourcesCollected()) {
        //     simulation.finish();
        //     simulation.start();
        //     start();
        // }
        // System.out.println(simulation.getStepNumber());

        // if(simulation.getStepNumber() == simulation.getSimulationIterations()){
        //     simulation.checkConstructionTask();
        //     simulation.finish();
        // }

        return super.step();
    }

    // These lines add a live updating inspector to for the model (can be found by clicking the model tab in the console on the left)
    public Object getSimulationInspectedObject() { return state; }
    //Make model's inspector live updatable
    public Inspector getInspector()
    {
        Inspector i = super.getInspector();
        i.setVolatile(true);
        return i;
    }

    @Override
    public void quit() {
        super.quit();

        if (displayFrame != null) {
            displayFrame.dispose();
        }

        displayFrame = null;

        display = null;
    }

}

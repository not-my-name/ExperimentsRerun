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
import org.jbox2d.common.Vec2;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

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

        display = new Display2D(600, 600, this) {
            @Override
            public boolean handleMouseEvent(MouseEvent event) {
                boolean returnB = super.handleMouseEvent(event);
                // System.out.println(event.getX() + " " + event.getY());
                return returnB;
            }
        };

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
        environmentPortrayal.setPortrayalForClass(ResourceObject.class, new LabelledPortrayal2D(new RectanglePortrayal2D(new Color(0,0,0,0)), null){
            @Override
            public void draw(Object object, java.awt.Graphics2D graphics, DrawInfo2D info) {
                ResourceObject res = (ResourceObject) object;

                if (res.isConstructed()) {
                    this.setLabelShowing(true);
                    this.setLabelScaling(5);
                    this.label = res.getType();
                    this.align = LabelledPortrayal2D.ALIGN_CENTER;
                    this.offsety = -10D;
                    Color resColor = (Color)res.getPortrayal().getPaint();
                    double y = (299 * resColor.getRed() + 587 * resColor.getGreen() + 114 * resColor.getBlue()) / 1000;
                    Color toUse = y >= 128 ? Color.black : Color.white;
                    this.paint = toUse;
                }
                else {
                    this.setLabelShowing(false);
                }
                super.draw(object, graphics, info);
            }    
        });
        // environmentPortrayal.setPortrayalForClass(ResourceObject.class, new RectanglePortrayal2D(new Color(0,0,0,0)){
        //     @Override
        //     public void draw(Object object, java.awt.Graphics2D graphics, DrawInfo2D info) {
        //         ResourceObject res = (ResourceObject) object;

        //         if (res.isConstructed()) {
        //             LabelledPortrayal2D lp = new LabelledPortrayal2D(this, res.getType() + "");
        //             lp.draw(object, graphics, info);
        //             return;
        //             // this.setLabelShowing(true);
        //         }
        //         else {
        //             super.draw(object, graphics, info);
        //         }
        //         // super.draw(object, graphics, info);
        //     }    
        // });

        environmentPortrayal.setPortrayalForClass(RobotObject.class, new OvalPortrayal2D(new Color(0,0,0,0)));
        environmentPortrayal.setPortrayalForClass(DetectionPoint.class, new OvalPortrayal2D(0.2D) {
            @Override
            public void draw(Object object, java.awt.Graphics2D graphics, DrawInfo2D info) {

                // DetectionPoint dp = (DetectionPoint) object;
                // // PolygonPortrayal dpPortrayal = (PolygonPortrayal) dp.getPortrayal();
                // // dpPortrayal.drawImprecise(graphics, dpPortrayal.getTransform(), false);
                // // info.draw.width = (double)Math.abs(dp.getPositions()[1].x - dp.getPositions()[2].x);
                // // info.draw.width = 1D;
                // // info.draw.height = 1D;
                // // System.out.println(info.draw.width);
                // // info.draw.width = 5D;
                // // System.out.println(super.getP1());
                // if (dp.getSide() == 0) {
                //     paint = Color.BLACK;
                // }
                // else if (dp.getSide() == 1) {
                //     paint = Color.BLUE;
                // }
                // else if (dp.getSide() == 2) {
                //     paint = Color.YELLOW;
                // }
                // else {
                //     paint = Color.GREEN;
                // }
                super.draw(object, graphics, info);
                // graphics.fillPolygon(resPortrayal.getXPoints(), resPortrayal.getYPoints(), resPortrayal.getNVertices());



                // super.draw(object, graphics, info);
                // DetectionPoint dp = (DetectionPoint)object;
                // Vec2[] dpPos = dp.getRelativePositions();
                // double[] xPs = new double[dpPos.length];
                // double[] yPs = new double[dpPos.length];
                // for (int i = 0; i < xPs.length; i++) {
                //     xPs[i] = (double)dpPos[i].x;
                //     yPs[i] = (double)dpPos[i].y;
                // }
                // double width = 0D;
                // if (dpPos[1].x == dpPos[2].x) {
                //     width = (double) dpPos[1].y - (double) dpPos[2].y;
                // }
                // else {
                //     width = (double) dpPos[1].y - (double) dpPos[2].y;
                // }
                // // System.out.println(width);
                // // System.out.println(dpPos[1] + " " + info.draw.x);
                // // graphics.draw(new Rectangle2D.Double((double)dpPos[1].x, (double)dpPos[1].y, 0.1D,0.1D));
                // // graphics.setPaint(Color.RED);
                // paint = Color.RED;
                // filled = false;
                // Shape s = new Polygon(xPs, yPs, xPs.length);
                // // ShapePortrayal2D s = new ShapePortrayal2D(xPs, yPs);
                // // s.draw(object, graphics, info);
                // // graphics.drawRect((int)info.draw.x,(int)info.draw.y,(int)width,(int)width);
                // graphics.draw(s);
                // super.draw(object, graphics, info);
            }
        });

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
    // public Object getSimulationInspectedObject() { return state; }
    // //Make model's inspector live updatable
    // public Inspector getInspector()
    // {
    //     Inspector i = super.getInspector();
    //     i.setVolatile(true);
    //     return i;
    // }

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

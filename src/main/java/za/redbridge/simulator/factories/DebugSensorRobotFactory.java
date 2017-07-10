package za.redbridge.simulator.factories;

import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.World;

import java.awt.Color;

import za.redbridge.simulator.PlacementArea;
import za.redbridge.simulator.config.SimConfig;
import za.redbridge.simulator.object.RobotObject;
import za.redbridge.simulator.phenotype.HeuristicPhenotype;
import za.redbridge.simulator.phenotype.Phenotype;

import java.util.ArrayList;

public class DebugSensorRobotFactory implements RobotFactory {
    protected float mass;
    protected float radius;
    protected Color color;
    protected Phenotype phenotype;
    protected HeuristicPhenotype heuristicPhenotype;
    protected int numRobots;

    private ArrayList<RobotObject> placedRobotsFirst;
    private ArrayList<RobotObject> placedRobotsSecond;

    public DebugSensorRobotFactory(Phenotype phenotype, float mass, float radius, Color color,
            int numRobots) {
        this.phenotype = phenotype;
        this.mass = mass;
        this.radius = radius;
        this.color = color;
        this.numRobots = numRobots;

        placedRobotsFirst = new ArrayList<RobotObject>();
        placedRobotsSecond = new ArrayList<RobotObject>();
    }

    @Override
    public void placeInstances(PlacementArea.ForType<RobotObject> placementArea, World world,
                            SimConfig.Direction targetAreaPlacement) {
        PlacementArea.Space space = placementArea.getCircularSpace(radius, new Vec2(50, 50), 0f);
        RobotObject r1 = new RobotObject(world, space.getPosition(), space.getAngle(), radius, mass,
                color, phenotype.clone(), targetAreaPlacement);

        placedRobotsFirst.add(r1);

        placementArea.placeObject(space, r1);

        space = placementArea.getCircularSpace(radius, new Vec2(70, 50), 0f);
        RobotObject r2 = new RobotObject(world, space.getPosition(), space.getAngle(), radius, mass,
                color, phenotype.clone(), targetAreaPlacement);

        placedRobotsSecond.add(r2);

        placementArea.placeObject(space, r2);
    }

    public void setNumRobots(int numRobots) { this.numRobots = numRobots; }

    public int getNumRobots() { return numRobots; }

    public ArrayList<RobotObject> getPlacedRobots() {return placedRobotsFirst;}
}

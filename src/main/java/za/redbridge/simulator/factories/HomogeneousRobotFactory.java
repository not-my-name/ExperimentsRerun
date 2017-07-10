package za.redbridge.simulator.factories;

import org.jbox2d.dynamics.World;

import java.awt.Color;
import org.jbox2d.common.Vec2;

import za.redbridge.simulator.PlacementArea;
import za.redbridge.simulator.config.SimConfig;
import za.redbridge.simulator.object.RobotObject;
import za.redbridge.simulator.phenotype.Phenotype;
import java.util.ArrayList;

public class HomogeneousRobotFactory implements RobotFactory {
    protected float mass;
    protected float radius;
    protected Color color;
    protected Phenotype phenotype;
    protected int numRobots;
    protected ArrayList<RobotObject> placedRobots;

    public HomogeneousRobotFactory(Phenotype phenotype, float mass, float radius, Color color,
                                   int numRobots) {
        this.phenotype = phenotype;
        this.mass = mass;
        this.radius = radius;
        this.color = color;
        this.numRobots = numRobots;
        placedRobots = new ArrayList<RobotObject>();
    }

    @Override
    public void placeInstances(PlacementArea.ForType<RobotObject> placementArea, World world,
                               SimConfig.Direction targetAreaPlacement) {

        placedRobots.clear();

        for (int i = 0; i < numRobots; i++) {
            PlacementArea.Space space = null;
            String agentLocaiton = za.redbridge.simulator.config.SimConfig.agentLocation;
            if(agentLocaiton.equals("")){
                space = placementArea.getRandomCircularSpace(radius);
            }else{space = placementArea.getAgentStartingSpace(radius, agentLocaiton);}

            Phenotype phenotype = this.phenotype.clone();

            RobotObject robot = new RobotObject(world, space.getPosition(), space.getAngle(),
                radius, mass, color, phenotype, targetAreaPlacement);

            placementArea.placeObject(space, robot);
            placedRobots.add(robot);
        }

        // placeTestInstances(placementArea, world, targetAreaPlacement,"welding0");
    }

    public void placeTestInstances(PlacementArea.ForType<RobotObject> placementArea, World world, SimConfig.Direction targetAreaPlacement, String demo){
        Vec2 pos1 = new Vec2(0,0);
        Vec2 pos2 = new Vec2(0,0);
        Vec2 pos3 = new Vec2(0,0);
        Vec2 pos4 = new Vec2(0,0);
        Vec2 pos5 = new Vec2(0,0);
        Vec2 pos6 = new Vec2(0,0);
        Vec2 pos7 = new Vec2(0,0);

        Vec2 pos8 = new Vec2(0,0);
        Vec2 pos9 = new Vec2(0,0);

        if(demo.equals("robot_pushing")){
            pos1 = new Vec2(11.6f,4);
            pos2 = new Vec2(11.95f,4);
            pos3 = new Vec2(12.35f,4);
            pos4 = new Vec2(6,10);
            pos5 = new Vec2(5.8f,3);
            pos6 = new Vec2(8.8f,3);
            pos7 = new Vec2(9.3f,3);

            pos8 = new Vec2(8.8f,8);
            pos9 = new Vec2(9.3f,8);
        }
        else if(demo.equals("welding0")){
            pos1 = new Vec2(3f,7);
            pos2 = new Vec2(3f,10);
        }
        else if(demo.equals("welding1")){
            pos1 = new Vec2(9.25f,3);
            pos2 = new Vec2(9.60f,3);
        }
        else if(demo.equals("colour_sensor")){

        }

        PlacementArea.Space space1 = placementArea.getCircularSpace(0.15f, pos1, 0f);
        PlacementArea.Space space2 = placementArea.getCircularSpace(0.15f, pos2, 0f);
        PlacementArea.Space space3 = placementArea.getCircularSpace(0.15f, pos3, 1.5f);
        PlacementArea.Space s1 = placementArea.getCircularSpace(0.15f, pos4, 1.5f);
        PlacementArea.Space s2 = placementArea.getCircularSpace(0.15f, pos5, 1.5f);
        PlacementArea.Space s3 = placementArea.getCircularSpace(0.15f, pos6, 1.5f);
        PlacementArea.Space s4 = placementArea.getCircularSpace(0.15f, pos7, 1.5f);
        PlacementArea.Space s5 = placementArea.getCircularSpace(0.15f, pos8, 1.5f);
        PlacementArea.Space s6 = placementArea.getCircularSpace(0.15f, pos9, 1.5f);

        Phenotype phenotype1 = this.phenotype.clone();
        Phenotype phenotype2 = this.phenotype.clone();
        Phenotype phenotype3 = this.phenotype.clone();
        Phenotype phenotype4 = this.phenotype.clone();
        Phenotype phenotype5 = this.phenotype.clone();
        Phenotype phenotype6 = this.phenotype.clone();
        Phenotype phenotype7 = this.phenotype.clone();
        Phenotype phenotype8 = this.phenotype.clone();
        Phenotype phenotype9 = this.phenotype.clone();

        RobotObject robot1 = new RobotObject(world, space1.getPosition(), space1.getAngle(),
            radius, mass, color, phenotype1, targetAreaPlacement);
        RobotObject robot2 = new RobotObject(world, space2.getPosition(), space2.getAngle(),
            radius, mass, color, phenotype2, targetAreaPlacement);
        RobotObject robot3 = new RobotObject(world, space3.getPosition(), space3.getAngle(),
            radius, mass, color, phenotype3, targetAreaPlacement);
        RobotObject robot4 = new RobotObject(world, s1.getPosition(), s1.getAngle(),
            radius, mass, color, phenotype4, targetAreaPlacement);
        RobotObject robot5 = new RobotObject(world, s2.getPosition(), s2.getAngle(),
            radius, mass, color, phenotype5, targetAreaPlacement);
        RobotObject robot6 = new RobotObject(world, s3.getPosition(), s3.getAngle(),
            radius, mass, color, phenotype6, targetAreaPlacement);
        RobotObject robot7 = new RobotObject(world, s4.getPosition(), s4.getAngle(),
            radius, mass, color, phenotype7, targetAreaPlacement);
        RobotObject robot8 = new RobotObject(world, s5.getPosition(), s5.getAngle(),
            radius, mass, color, phenotype8, targetAreaPlacement);
        RobotObject robot9 = new RobotObject(world, s6.getPosition(), s6.getAngle(),
            radius, mass, color, phenotype9, targetAreaPlacement);

        if(demo.equals("robot_pushing")){
            placementArea.placeObject(space1, robot1);
            placementArea.placeObject(space2, robot2);
            placementArea.placeObject(space3, robot3);
            placementArea.placeObject(s1, robot4);
            placementArea.placeObject(s2, robot5);
            placementArea.placeObject(s3, robot6);
            placementArea.placeObject(s4, robot7);
            placementArea.placeObject(s5, robot8);
            placementArea.placeObject(s6, robot9);
        }
        else if(demo.equals("welding0")){
            placementArea.placeObject(space1, robot1);
            placementArea.placeObject(space2, robot2);
        }
        else if(demo.equals("welding1")){
            placementArea.placeObject(space1, robot1);
            placementArea.placeObject(space2, robot2);
        }
        else if(demo.equals("colour_sensor")){

        }
    }

    public void setNumRobots(int numRobots) { this.numRobots = numRobots; }

    public int getNumRobots() { return numRobots; }

    public ArrayList<RobotObject> getPlacedRobots(){ return placedRobots; }
}

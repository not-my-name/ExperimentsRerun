package za.redbridge.simulator;

import org.jbox2d.callbacks.QueryCallback;
import org.jbox2d.collision.AABB;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.Fixture;
import org.jbox2d.dynamics.World;
import org.jbox2d.dynamics.contacts.Contact;
import org.jbox2d.collision.shapes.MassData;

import java.awt.Color;
import java.awt.Paint;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedList;
import java.util.Set;
import java.util.Arrays;
import sim.util.Double2D;
import java.util.Random;

import sim.engine.SimState;
import za.redbridge.simulator.FitnessStats;
// import za.redbridge.simulator.Simulation;
import za.redbridge.simulator.physics.BodyBuilder;
import za.redbridge.simulator.physics.Collideable;
import za.redbridge.simulator.physics.FilterConstants;
import za.redbridge.simulator.portrayal.Portrayal;
import za.redbridge.simulator.portrayal.RectanglePortrayal;
import za.redbridge.simulator.object.*;
import za.redbridge.simulator.config.SchemaConfig;


import static za.redbridge.simulator.physics.AABBUtil.getAABBHeight;
import static za.redbridge.simulator.physics.AABBUtil.getAABBWidth;
import static za.redbridge.simulator.physics.AABBUtil.resizeAABB;

/**
 * Created by shsu on 2014/08/13.
 */
public class ConstructionZone {

    private static final boolean ALLOW_REMOVAL = false;

    private static final float BLAME_BOX_EXPANSION_RATE = 1.5f;
    private static final int BLAME_BOX_TRIES = 5;

    // private int width, height;
    // private final AABB aabb;

    //total resource value in this target area
    // private final FitnessStats fitnessStats;

    //hash set so that object values only get added to forage area once
    private final Set<ResourceObject> connectedResources = new HashSet<>();

    private final List<ResourceObject> resOrder;  //update size to be the vonfig's resource size

    // resources never get added to watched fixtures list
    private final List<Fixture> watchedFixtures = new ArrayList<>();

    private Vec2 czPos;

    private int resource_count = 0;
    private int ACount = 0;
    private int BCount = 0;
    private int CCount = 0;
    private final int czNum;
    private final Color czColor;
    //keeps track of what has been pushed into this place
    // public ConstructionZoneObject(World world, Vec2 position, int width, int height,
    //         double totalResourceValue, int maxSteps) {
    //     super(createPortrayal(width, height), createBody(world, position, width, height));

    //     this.width = width;
    //     this.height = height;
        // this.fitnessStats = new FitnessStats(totalResourceValue, maxSteps);

    //     aabb = getBody().getFixtureList().getAABB(0);
    // }

    // public ConstructionZoneObject (World world, ResourceObject r1, ResourceObject r2, int maxSteps) {
    //     Vec2 r1Pos = r1.getBody().getPosition();
    // }

    public ConstructionZone (int maxSteps, int czNum) {
        // this.fitnessStats = new FitnessStats (maxSteps);
        this.czNum = czNum;
        resOrder = new LinkedList<>();
        Random rand = new Random();
        int randVal1 = rand.nextInt((255 - 1) + 1) + 1;
        int randVal2 = rand.nextInt((255 - 1) + 1) + 1;
        int randVal3 = rand.nextInt((255 - 1) + 1) + 1;
        czColor = new Color(255 - randVal1, 255 - randVal2, 255 - randVal3);
    }

    public ConstructionZone(List<ResourceObject>updatedResources, int czNum) {
        // System.out.println("CZ: " + czNum);
        this.czNum = czNum;
        Random rand = new Random();
        int randVal1 = rand.nextInt((255 - 1) + 1) + 1;
        int randVal2 = rand.nextInt((255 - 1) + 1) + 1;
        int randVal3 = rand.nextInt((255 - 1) + 1) + 1;
        czColor = new Color(255 - randVal1, 255 - randVal2, 255 - randVal3);
        resOrder = new LinkedList<>();
        for (ResourceObject r : updatedResources) {
            addResource(r, true);
        }
        updateCZCenter();
    }

    public ConstructionZone(ResourceObject[] updatedResources, int czNum) {
        // System.out.println("CZ: " + czNum);
        this.czNum = czNum;
        Random rand = new Random();
        int randVal1 = rand.nextInt((255 - 1) + 1) + 1;
        int randVal2 = rand.nextInt((255 - 1) + 1) + 1;
        int randVal3 = rand.nextInt((255 - 1) + 1) + 1;
        czColor = new Color(255 - randVal1, 255 - randVal2, 255 - randVal3);
        resOrder = new LinkedList<>();
        for (ResourceObject r : updatedResources) {
            addResource(r, true);   
        }
        
        updateCZCenter();
    }

    public void startConstructionZone(ResourceObject r1, ResourceObject r2) {
        Vec2 r1Pos = r1.getBody().getPosition();
        Vec2 r2Pos = r2.getBody().getPosition();
        float aveX = (r1Pos.x + r2Pos.x)/2;
        float aveY = (r1Pos.y + r2Pos.y)/2;
        czPos = new Vec2(aveX, aveY);

        // System.out.println("Starting CZ with " + r1 + " " + r2);

        // System.out.println(r1.getConnectionReason() + " " + r2.getConnectionReason());
        
        // if ((r1NumAdj == 0)||(r2NumAdj == 0)) {
        //     System.out.println("SHIIIIIIIIIITTTTTT");
        // }
        addResource(r1, true);
        addResource(r2, true); 

    }

    public void updateCZCenter() {
        Vec2 result = new Vec2();
        for (ResourceObject res : connectedResources) {
            result.add(res.getBody().getPosition());
        }
        czPos = new Vec2(result.x/connectedResources.size(), result.y/connectedResources.size());
    }

    /**
    Method that adds resource to the construction zone/target area
    @param ResourceObject resource: the resource that is to be added

    This method adds fitness:
        FResource(resource) = F(resource)*{1 if fitsInSchema; 0 otherwise}
    **/
    public void addResource(ResourceObject resource, boolean isFirstConnection) {
        double FResource = 0D;
        if (connectedResources.add(resource)) {
            // fitnessStats.addToTeamFitness(resource.getValue());
            if (isFirstConnection) {
                // System.out.println("Adding resource to CZ (first)" + czNum + ": " + resource);
                if(resource.getValue() > 0) {
                    resOrder.add(resource);
                    resource_count++;
                }

                if (resource.getType().equals("A")) {
                    ACount++;
                }
                else if (resource.getType().equals("B")) {
                    BCount++;
                }
                else if (resource.getType().equals("C")) {
                    CCount++;
                }

                // Get the robots joined to the resource
                // Set<RobotObject> pushingBots = resource.getPushingRobots();

                // // // If no robots joined, get nearby robots
                // // if (pushingBots.isEmpty()) {
                // //     pushingBots = findRobotsNearResource(resource);
                // // }

                // // Update the fitness for the bots involved
                // if (!pushingBots.isEmpty()) {
                //     double adjustedFitness = resource.getAdjustedValue() / pushingBots.size();
                //     for (RobotObject robot : pushingBots) {
                        // fitnessStats
                //                 .addToPhenotypeFitness(robot.getPhenotype(), adjustedFitness);
                //     }
                // }
                // Mark resource as collected (this breaks the joints)
                // resource.setCollected(true);
                resource.setConstructed();
                resource.setCzNumber(czNum);
                resource.getPortrayal().setPaint(czColor);
                resource.getBody().setType(BodyType.STATIC);

                // MassData md = new MassData();
                // resource.getBody().getMassData(md);
                // md.mass = 1000f;
                // System.out.println(md.mass);
                // resource.getBody().setMassData(md);
                // System.out.println(resource.getBody().getMass());
                // resource.getBody().setLinearDamping(1000f);
                // resource.getBody().setActive(false);
                // resource.getPortrayal().setEnabled(false);
                // resource = null; // Naeem Ganey code.
                // this.fitnessStats.addToTeamFitness(200D); 
            }
            else {
                // System.out.println("Adding resource to CZ " + czNum + ": " + resource);
                // System.out.println("HERE");
                if (resource.pushedByMaxRobots()) {
                    if(resource.getValue() > 0) {
                        resOrder.add(resource);
                        resource_count++;
                    }

                    if (resource.getType().equals("A")) {
                        ACount++;
                    }
                    else if (resource.getType().equals("B")) {
                        BCount++;
                    }
                    else if (resource.getType().equals("C")) {
                        CCount++;
                    }

                    // Get the robots joined to the resource
                    // Set<RobotObject> pushingBots = resource.getPushingRobots();

                    // // // If no robots joined, get nearby robots
                    // // if (pushingBots.isEmpty()) {
                    // //     pushingBots = findRobotsNearResource(resource);
                    // // }

                    // // Update the fitness for the bots involved
                    // if (!pushingBots.isEmpty()) {
                    //     double adjustedFitness = resource.getAdjustedValue() / pushingBots.size();
                    //     for (RobotObject robot : pushingBots) {
                            // fitnessStats
                    //                 .addToPhenotypeFitness(robot.getPhenotype(), adjustedFitness);
                    //     }
                    // }
                    // Mark resource as collected (this breaks the joints)
                    resource.setConstructed();
                    resource.setCzNumber(czNum);

                    //Check for multiple possible connections
                    // checkForOtherConnections(resource);

                    resource.getPortrayal().setPaint(czColor);
                    resource.getBody().setType(BodyType.STATIC);
                    // MassData md = new MassData();
                    // resource.getBody().getMassData(md);
                    // md.mass = 1000f;
                    // System.out.println(md.mass);
                    // resource.getBody().setMassData(md);
                    // System.out.println(resource.getBody().getMass());
                    // resource.getBody().setLinearDamping(1000f);
                    // resource.getBody().setActive(false);
                    // resource.getPortrayal().setEnabled(false);
                    // resource = null; // Naeem Ganey code.
                    // this.fitnessStats.addToTeamFitness(200D);   
                }
            }
        }
    }

    /**
    Method to update any other connections that the addition of this block creates (L shaped example)
    **/
    // public void checkForOtherConnections(ResourceObject res) {
    //     String[] adjList = res.getAdjacentList();
    //     ResourceObject cRes;
    //     String[] cResAdj;
    //     ResourceObject[] cResAdjRes;
    //     int sideNum = 0;
    //     for (int i = 0; i < adjList.length; i++) {
    //         if (!adjList[i].equals("_")) {
    //             sideNum = i;
    //             cRes = res.getAdjacentResources()[sideNum];
    //             cResAdj = cRes.getAdjacentList();
    //             cResAdjRes = cRes.getAdjacentResources();
    //         }
    //     }
    //     //Check the other's top and bottom for other possible connections
    //     if (sideNum == 0) {
    //         //check top
    //         if ((!cResAdj[2].equals("_"))) {
    //             //check this one's right side
    //             ResourceObject
    //         }
    //         //check bottom
    //         else if (!cResAdj[3].equals("_"))) {
                
    //         }
    //     }
    // }

    public List<ResourceObject> updateCZNumber(int newCZNum) {
        List<ResourceObject> returnResources = new LinkedList<>();
        for (ResourceObject r : resOrder) {
            r.setCzNumber(newCZNum);
            returnResources.add(r);
        }
        return returnResources;
    }

    public void addNewResources (List<ResourceObject> newResources) {
        for (ResourceObject r : newResources) {
            addResource(r, true);
        }
    }

    public void clearCZ() {
        connectedResources.clear();
        resOrder.clear();
        resource_count = 0;
        ACount = 0;
        BCount = 0;
        CCount = 0;
        czPos = null;
    }

    public Vec2 getCZPos () {
        return czPos;
    }

    public Set<ResourceObject> getConnectedResources () {
        return connectedResources;
    }

    public List<ResourceObject> getConstructionOrder () {
        return resOrder;
    }

    public boolean isInConstructionZone(ResourceObject r) {
        if (connectedResources.contains(r)) {
            return true;
        }
        else {
            return false;
        }
    }

    // private void removeResource(ResourceObject resource) {
    //     if (connectedResources.remove(resource)) {
    //         // fitnessStats.addToTeamFitness(-resource.getValue());

    //         if(resource.getValue() > 0) resource_count--;
    //         // Mark resource as no longer collected
    //         resource.setCollected(false);
    //         resource.getPortrayal().setPaint(Color.MAGENTA);

    //         // Set<RobotObject> pushingBots = findRobotsNearResource(resource);

    //         // if (!pushingBots.isEmpty()) {
    //         //     double adjustedFitness = resource.getAdjustedValue() / pushingBots.size();
    //         //     for (RobotObject robot : pushingBots) {
    //                 // fitnessStats.addToPhenotypeFitness(robot.getPhenotype(), -adjustedFitness);
    //         //     }
    //         // }
    //     }
    // }

    /*
     * Finds robots very close to the ResourceObject that can be blamed for pushing the resource
     * in/out of target area.
     */
    // private Set<RobotObject> findRobotsNearResource(ResourceObject resource) {
    //     // Check which robots pushed the resource out based on a bounding box
    //     Fixture resourceFixture = resource.getBody().getFixtureList();
    //     AABB resourceBox = resourceFixture.getAABB(0);

    //     // Try query robots within the AABB of the resource
    //     Set<RobotObject> robots = new HashSet<>();
    //     RobotObjectQueryCallback callback = new RobotObjectQueryCallback(robots);
    //     getBody().getWorld().queryAABB(callback, resourceBox);

    //     if (!robots.isEmpty()) {
    //         return robots;
    //     }

    //     // If no robots found, iteratively expand the dimensions of the query box
    //     AABB blameBox = new AABB(resourceBox);
    //     for (int i = 0; i < BLAME_BOX_TRIES; i++) {
    //         float width = getAABBWidth(blameBox) * BLAME_BOX_EXPANSION_RATE;
    //         float height = getAABBHeight(blameBox) * BLAME_BOX_EXPANSION_RATE;
    //         resizeAABB(blameBox, width, height);
    //         getBody().getWorld().queryAABB(callback, blameBox);

    //         if (!robots.isEmpty()) {
    //             break;
    //         }
    //     }
    //     return robots;
    // }

    public int getNumberOfConnectedResources() {
        return resource_count;
        //connectedResources.size();
    }

    // public AABB getAabb() {
    //     return aabb;
    // }

    // public int getWidth() {
    //     return width;
    // }

    // public int getHeight() {
    //     return height;
    // }

    public int getNumCorrectlyConnected (SchemaConfig schema, int configNum) {
        int numCorrect = 0;
        for (ResourceObject res : connectedResources) {
            String [] adjacent = res.getAdjacentList();
            if (schema.checkConfig(configNum, res.getType(), adjacent) == 4) {
                numCorrect++;
            }
        }
        return numCorrect;
    }

    /**
    Calculates the Fcorr for this construction zone:
        Fcorr = ((#correct sides/#shared sides) per block in CZ)/#simulation resources
    **/
    public double getCZCorrectness(SchemaConfig schema, int configNum) {
        double correctness = 0D;
        for (ResourceObject res : connectedResources) {
            String[] adjacent = res.getAdjacentList();
            // System.out.println(Arrays.toString(adjacent));
            int sharedSides = 0;  //counter for the number of sides this resource shares with other resources
            for (int i = 0; i < adjacent.length; i++) {
                if (adjacent[i] != "_") {
                    sharedSides++;
                }
            }
            if (sharedSides == 0) {
                // System.out.println(res.getConnectionReason());
            }
            // System.out.println("sharedSides = " + sharedSides);
            correctness += schema.checkConfig(configNum, res.getType(), adjacent)/(double)sharedSides;
            // System.out.println("shared sides = " + sharedSides + ", correctlyConnected = " + schema.checkConfig(configNum, res.getType(), adjacent));
        }
        
        return correctness;
    }

    public int[] getResTypeCount() {
        int [] typeCount = {ACount, BCount, CCount};
        return typeCount;
    }

    public double getTotalResourceValue() {
        double totalValue = 0D;
        for (ResourceObject res : connectedResources) {
            totalValue += res.getValue();
        }
        return totalValue;
    }

    public static int [] getOverallTypeCount (ConstructionZone[] czs) {
        int[] typeCount = new int[3];
        for (int i = 0;  i < czs.length; i++) {
            int[] czTypeCount = czs[i].getResTypeCount();
            typeCount[0] += czTypeCount[0];
            typeCount[1] += czTypeCount[1];
            typeCount[2] += czTypeCount[2];
        }
        return typeCount;
    }

    public Color getCZColor() {
        return czColor;
    }

    public static ResourceObject[] getOverallConstructionOrder (ConstructionZone[] czs) {
        List<ResourceObject> overallOrder = new LinkedList<>();
        for (int i = 0; i < czs.length; i++) {
            for (ResourceObject r : czs[i].getConstructionOrder()) {
                overallOrder.add(r);
            }
        }
        return overallOrder.toArray(new ResourceObject[0]);
    }

    // public FitnessStats getFitnessStats() {
    //     // return fitnessStats;
    // }

    // @Override
    public void handleBeginContact(Contact contact, Fixture otherFixture) {
        if (!(otherFixture.getBody().getUserData() instanceof ResourceObject)) {
            return;
        }

        if (!watchedFixtures.contains(otherFixture)) {
            watchedFixtures.add(otherFixture);
        }
    }

    // // @Override
    // public void handleEndContact(Contact contact, Fixture otherFixture) {
    //     if (!(otherFixture.getBody().getUserData() instanceof ResourceObject)) {
    //         return;
    //     }

    //     // Remove from watch list
    //     watchedFixtures.remove(otherFixture);

    //     // Remove from the score
    //     if (ALLOW_REMOVAL) {
    //         ResourceObject resource = (ResourceObject) otherFixture.getBody().getUserData();
    //         // removeResource(resource);
    //     }
    // }

    // private static class RobotObjectQueryCallback implements QueryCallback {

    //     final Set<RobotObject> robots;

    //     RobotObjectQueryCallback(Set<RobotObject> robots) {
    //         this.robots = robots;
    //     }

    //     @Override
    //     public boolean reportFixture(Fixture fixture) {
    //         if (!fixture.isSensor()) { // Don't detect robot sensors, only bodies
    //             Object bodyUserData = fixture.getBody().getUserData();
    //             if (bodyUserData instanceof RobotObject) {
    //                 robots.add((RobotObject) bodyUserData);
    //             }
    //         }

    //         return true;
    //     }
    // }

}

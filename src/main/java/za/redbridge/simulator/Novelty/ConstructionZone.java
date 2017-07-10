package za.redbridge.simulator;

import org.jbox2d.callbacks.QueryCallback;
import org.jbox2d.collision.AABB;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.Fixture;
import org.jbox2d.dynamics.World;
import org.jbox2d.dynamics.contacts.Contact;

import java.awt.Color;
import java.awt.Paint;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Arrays;
import sim.util.Double2D;
import java.util.*;

import sim.engine.SimState;
// import za.redbridge.simulator.FitnessStats;
// import za.redbridge.simulator.Simulation;
import za.redbridge.simulator.physics.BodyBuilder;
import za.redbridge.simulator.physics.Collideable;
import za.redbridge.simulator.physics.FilterConstants;
import za.redbridge.simulator.portrayal.Portrayal;
import za.redbridge.simulator.portrayal.RectanglePortrayal;
import za.redbridge.simulator.object.*;


import static za.redbridge.simulator.physics.AABBUtil.getAABBHeight;
import static za.redbridge.simulator.physics.AABBUtil.getAABBWidth;
import static za.redbridge.simulator.physics.AABBUtil.resizeAABB;

/**
 * Created by shsu on 2014/08/13.
 */

/*
this is the class that handles the main construction site
the first connected blocks become the construction zone

once the construction zone is created, robots only get rewarded
for connecting blocks to the zone, not for connecting separate blocks
*/

public class ConstructionZone {

    private Vec2 cZonePosition; //variable to store the coordinates of the center of the construction zone

    private static final boolean ALLOW_REMOVAL = false;

    private static final float BLAME_BOX_EXPANSION_RATE = 1.5f;
    private static final int BLAME_BOX_TRIES = 5;

    // private int width, height;
    // private final AABB aabb;

    //total resource value in this target area
    //private final FitnessStats fitnessStats;

    //hash set so that object values only get added to forage area once
    private final Set<ResourceObject> connectedResources;

    // resources never get added to watched fixtures list
    private final List<Fixture> watchedFixtures = new ArrayList<>();

    /**
    check how to make the connectionOrder array a variable size so that the number of resources in the simulation can be changed
    */

    //private ResourceObject[] connectionOrder = new ResourceObject[15]; //change the size to the number of resource in the zone
    private LinkedList<ResourceObject> connectionOrder;
    private int numConnected; //variable to keep track of how many resources are in the construction zone

    private int cZoneIndex; //indicate the index of the current construction zone
    private final Color czColor;

    /**
    check what the difference is between the numConnected var and the resource_count var
    check where resource count gets incremented below in the addResource method, if using numConnected then check if it needs to be incremented in the same if statement
    */

    //int resource_count = 0;
    int ACount = 0;
    int BCount = 0;
    int CCount = 0;

    public ConstructionZone(List<ResourceObject>updatedResources, int czNum) {

        //this.cZoneIndex = czNum;
        connectionOrder = new LinkedList<>();
        connectedResources = new HashSet<>();
        numConnected = 0;
        cZoneIndex = czNum;

        Random rand = new Random();
        int randVal1 = rand.nextInt((255 - 1) + 1) + 1;
        int randVal2 = rand.nextInt((255 - 1) + 1) + 1;
        int randVal3 = rand.nextInt((255 - 1) + 1) + 1;
        czColor = new Color(255 - randVal1, 255 - randVal2, 255 - randVal3);

        for (ResourceObject r : updatedResources) {
            addResource(r, true);
        }

        updateCZCenter();
    }

    public ConstructionZone(ResourceObject[] updatedResources, int czNum) {

        this.cZoneIndex = czNum;
        numConnected = 0;
        Random rand = new Random();
        int randVal1 = rand.nextInt((255 - 1) + 1) + 1;
        int randVal2 = rand.nextInt((255 - 1) + 1) + 1;
        int randVal3 = rand.nextInt((255 - 1) + 1) + 1;
        czColor = new Color(255 - randVal1, 255 - randVal2, 255 - randVal3);
        connectionOrder = new LinkedList<>();
        connectedResources = new HashSet<>();

        for (ResourceObject r : updatedResources) {
            addResource(r, true);
        }

        updateCZCenter();
    }

    public ConstructionZone (int czNum) {

        this.cZoneIndex = czNum;
        numConnected = 0;
        connectionOrder = new LinkedList<>();
        connectedResources = new HashSet<>();
        Random rand = new Random();
        int randVal1 = rand.nextInt((255 - 1) + 1) + 1;
        int randVal2 = rand.nextInt((255 - 1) + 1) + 1;
        int randVal3 = rand.nextInt((255 - 1) + 1) + 1;
        czColor = new Color(255 - randVal1, 255 - randVal2, 255 - randVal3);
    }

    public void startConstructionZone(ResourceObject r1, ResourceObject r2) {

        Vec2 r1Pos = r1.getBody().getPosition();
        Vec2 r2Pos = r2.getBody().getPosition();
        float aveX = (r1Pos.x + r2Pos.x)/2;
        float aveY = (r1Pos.y + r2Pos.y)/2;
        cZonePosition = new Vec2(aveX, aveY);

        addResource(r1);
        addResource(r2);
    }

    /*
    Method that adds resource to the construction zone/target area
    @param ResourceObject resource: the resource that is to be added
    **/
    public void addResource(ResourceObject resource) {

        double FResource = 0D;

        if (connectedResources.add(resource)) { //checks if the resource hasnt already been added

            if (resource.getType().equals("A")) {
                ACount++;
            }
            else if (resource.getType().equals("B")) {
                BCount++;
            }
            else if (resource.getType().equals("C")) {
                CCount++;
            }

            numConnected++;
            connectionOrder.add(resource);

            resource.setConstructed();
            resource.setConstructionZoneID(cZoneIndex);
            resource.getPortrayal().setPaint(czColor);
            //resource.getBody().setActive(false);
            resource.getBody().setType(BodyType.STATIC);
        }
    }

    public void addResource(ResourceObject resource, boolean isFirstConnection) {

        //System.out.println("ConstructionZone: adding resource " + resource + " with adjacent list = " + Arrays.toString(resource.getAdjacentList()) );

        double FResource = 0D;
        if (connectedResources.add(resource)) {

            if (isFirstConnection) {

                if (resource.getType().equals("A")) {
                    ACount++;
                }
                else if (resource.getType().equals("B")) {
                    BCount++;
                }
                else if (resource.getType().equals("C")) {
                    CCount++;
                }

                numConnected++;
                connectionOrder.add(resource);

                resource.setConstructed();
                resource.setConstructionZoneID(cZoneIndex);
                resource.getPortrayal().setPaint(czColor);
                resource.getBody().setType(BodyType.STATIC);
            }
            else {

                if (resource.pushedByMaxRobots()) {

                    if (resource.getType().equals("A")) {
                        ACount++;
                    }
                    else if (resource.getType().equals("B")) {
                        BCount++;
                    }
                    else if (resource.getType().equals("C")) {
                        CCount++;
                    }

                    numConnected++;
                    connectionOrder.add(resource);

                    // Mark resource as collected (this breaks the joints)
                    resource.setConstructed();
                    resource.setConstructionZoneID(cZoneIndex);
                    resource.getPortrayal().setPaint(czColor);
                    resource.getBody().setType(BodyType.STATIC);
                }
            }
        }
    }

    public double getTotalResourceValue() {

        //System.out.println("ConstructionZone: check where the getTotalResourceValue method is being called from");

        int total = 0;
        for(ResourceObject resObj : connectedResources) {
            total += resObj.getValue();
        }

        return total;
    }

    // public List<ResourceObject> updateCZNumber() {
    // 
    //     List<ResourceObject> returnResources = new LinkedList<>();
    //     for (ResourceObject r : connectionOrder) {
    //
    //         r.setConstructionZoneID(cZoneIndex);
    //         returnResources.add(r);
    //     }
    //
    //     return returnResources;
    // }

    /**
    check that these new addResource methods do not conflict with each other
    cause some or other weird results
    */
    public void addNewResources (List<ResourceObject> newResources) {

        for (ResourceObject r : newResources) {
            addResource(r, true);
        }
    }

    public void clearCZ() {

        connectedResources.clear();
        connectionOrder.clear();
        numConnected = 0;
        ACount = 0;
        BCount = 0;
        CCount = 0;
        cZonePosition = null;
    }

    public void updateCZCenter() {

        Vec2 result = new Vec2();

        for (ResourceObject res : connectedResources) {
            result.add(res.getBody().getPosition());
        }

        cZonePosition = new Vec2(result.x/connectedResources.size(), result.y/connectedResources.size());
    }

    public Color getCZoneColour() {
        return czColor;
    }

    public int getNumConnected() {
        return numConnected;
    }

    public int getACount() {
        return ACount;
    }

    public int getBCount() {
        return BCount;
    }

    public int getCCount() {
        return CCount;
    }

    public Vec2 getCZonePosition() {
        return cZonePosition;
    }

    public LinkedList<ResourceObject> getConnectionOrder() {
        return this.connectionOrder;
    }

    public Set<ResourceObject> getConnectedResources () {
        return connectedResources;
    }

    public boolean isInConstructionZone(ResourceObject r) {

        if (connectedResources.contains(r)) {
            return true;
        }
        else {
            return false;
        }
    }

    private void removeResource(ResourceObject resource) {

        if (connectedResources.remove(resource)) {
            //fitnessStats.addToTeamFitness(-resource.getValue());

            if(resource.getValue() > 0) numConnected--;
            // Mark resource as no longer collected
            resource.setConstructed(false);
            resource.getPortrayal().setPaint(Color.MAGENTA);
        }
    }

    // public int getNumberOfConnectedResources() {
    //     return numConnected;
    //     //connectedResources.size();
    // }

    public int [] getResourceTypeCount () {
        int [] typeCount = {ACount, BCount, CCount};
        return typeCount;
    }

    // @Override
    public void handleBeginContact(Contact contact, Fixture otherFixture) {
        if (!(otherFixture.getBody().getUserData() instanceof ResourceObject)) {
            return;
        }

        if (!watchedFixtures.contains(otherFixture)) {
            watchedFixtures.add(otherFixture);
        }
    }

    // @Override
    public void handleEndContact(Contact contact, Fixture otherFixture) {
        if (!(otherFixture.getBody().getUserData() instanceof ResourceObject)) {
            return;
        }

        // Remove from watch list
        watchedFixtures.remove(otherFixture);

        // Remove from the score
        if (ALLOW_REMOVAL) {
            ResourceObject resource = (ResourceObject) otherFixture.getBody().getUserData();
            removeResource(resource);
        }
    }

    private static class RobotObjectQueryCallback implements QueryCallback {

        final Set<RobotObject> robots;

        RobotObjectQueryCallback(Set<RobotObject> robots) {
            this.robots = robots;
        }

        @Override
        public boolean reportFixture(Fixture fixture) {
            if (!fixture.isSensor()) { // Don't detect robot sensors, only bodies
                Object bodyUserData = fixture.getBody().getUserData();
                if (bodyUserData instanceof RobotObject) {
                    robots.add((RobotObject) bodyUserData);
                }
            }

            return true;
        }
    }

}

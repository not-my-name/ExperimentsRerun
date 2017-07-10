package za.redbridge.simulator.object;

import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.World;
import org.jbox2d.dynamics.joints.Joint;
import org.jbox2d.dynamics.joints.JointDef;
import org.jbox2d.dynamics.joints.WeldJointDef;

import java.awt.Color;
import java.awt.Paint;
import java.time.temporal.ValueRange;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
//CONCURRENCY CHANGES
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Iterator;

import java.util.*;

import sim.engine.SimState;
import za.redbridge.simulator.Simulation;
import za.redbridge.simulator.physics.BodyBuilder;
import za.redbridge.simulator.physics.FilterConstants;
import za.redbridge.simulator.portrayal.PolygonPortrayal;
import za.redbridge.simulator.portrayal.Portrayal;
import za.redbridge.simulator.portrayal.RectanglePortrayal;

import org.jbox2d.collision.AABB;

import org.jbox2d.common.Transform;

/**
 * Object to represent the resources in the environment. Has a value and a weight.
 *
 * Created by jamie on 2014/07/23.
 */
public class ResourceObject extends PhysicalObject {

    //private static final Paint DEFAULT_COLOUR = new Color(255, 235, 82);
    private static final Paint DEFAULT__TRASH_COLOUR = new Color(43, 54, 50);
    private static final Paint DEFAULT__RESOURCE_COLOUR = new Color(2, 12, 156);
    private static final boolean DEBUG = true;

    public enum Side {
        LEFT, RIGHT, TOP, BOTTOM
    }

    private Side stickySide;

    private final AnchorPoint[] leftAnchorPoints;
    private final AnchorPoint[] rightAnchorPoints;
    private final AnchorPoint[] topAnchorPoints;
    private final AnchorPoint[] bottomAnchorPoints;

    // private final WeldPoint[] leftWeldPoints;
    // private final WeldPoint[] rightWeldPoints;
    // private final WeldPoint[] topWeldPoints;
    // private final WeldPoint[] bottomWeldPoints;

    private final WeldPoint [] weldPoints;

    private final int weldPointN = 2;
    private final ArrayList<WeldPoint[]> weldList;

    private final Vec2 pool = new Vec2();

    private final double width;
    private final double height;
    private final int pushingRobots;
    private final double value;
    private final String type;

    private double adjustedValue;

    private boolean fullyWelded;
    private boolean isConstructed; //if it is part of the construction zone
    private boolean hasMoved; //if the robot has moved it

    private final Map<RobotObject, JointDef> pendingJoints;
    private final Map<RobotObject, Joint> joints;

    private final Map<Integer, String[]> adjacencyMap;


    private boolean connected;

    private final DetectionPoint[] detectionPoints;
    private String[] adjacentResources;
    private ResourceObject[] adjacentList;

    private LinkedList<Vec2> resourceTrajectory = new LinkedList<Vec2>();

    private int countConnected; //var to count the number of resources connected
    private Vec2 initialPos; //starting location of the resource

    //variables to store the initial starting location of the resource object
    private final float initialX;
    private final float initialY;

    private int constructionZoneID = -1; //keep track of which construction zone this resource is connected to

    private boolean visited; //variable to check whether or not this resource object has been traversed

    //private Vec2 gridPosition; //variable to keep track of the resource's position in the grid for later comparison

    // is a hack
    // private ArrayList<ResourceObject> otherResources = new ArrayList<ResourceObject>();

    public ResourceObject(World world, Vec2 position, float angle, float width, float height,
            float mass, int pushingRobots, double value, String type) {
        super(createPortrayal(width, height, type),
                createBody(world, position, angle, width, height, mass));
        this.width = width;
        this.height = height;
        this.pushingRobots = pushingRobots;
        this.value = value;
        this.type = type;

        this.fullyWelded = false;
        this.isConstructed = false;
        this.hasMoved = false;
        this.visited = false;

        adjustedValue = value;

        leftAnchorPoints = new AnchorPoint[pushingRobots];
        rightAnchorPoints = new AnchorPoint[pushingRobots];
        topAnchorPoints = new AnchorPoint[pushingRobots];
        bottomAnchorPoints = new AnchorPoint[pushingRobots];
        initAnchorPoints();

        // leftWeldPoints = new WeldPoint[weldPointN];
        // rightWeldPoints = new WeldPoint[weldPointN];
        // topWeldPoints = new WeldPoint[weldPointN];
        // bottomWeldPoints = new WeldPoint[weldPointN];
        weldList = new ArrayList<WeldPoint[]>();

        weldPoints = new WeldPoint[4];
        initWeldPoints();

        detectionPoints = new DetectionPoint[4];
        initDetectionPoints();

        adjacentResources = new String[4];

        joints = new HashMap<>(pushingRobots);
        pendingJoints = new HashMap<>(pushingRobots);

        adjacentList = new ResourceObject[4];

        //Populate the 4 possible adjacency maps
        adjacencyMap = new HashMap<>(4);
        String[] firstQuad = {"L","R","T","B"};
        String[] secondQuad = {"B","T","L","R"};
        String[] thirdQuad = {"R","L","B","T"};
        String[] fourthQuad = {"T","B","R","L"};
        adjacencyMap.put(0, firstQuad);
        adjacencyMap.put(1, secondQuad);
        adjacencyMap.put(2, thirdQuad);
        adjacencyMap.put(3, fourthQuad);

        countConnected = 0;

        if (DEBUG) {
            getPortrayal().setChildDrawable(new DebugPortrayal(Color.BLACK, false));
        }

        initialPos = getBody().getPosition();
        initialX = initialPos.x;
        initialY = initialPos.y;
    }

    protected static Portrayal createPortrayal(double width, double height, String type) {
        Paint color;
        switch (type) {
            case "A":
                color = new Color(2,12,156);
                break;
            case "B":
                color = new Color(0,194,16);
                break;
            case "C":
                color = new Color(194,0,0);
                break;
            default:
                color = new Color(219,110,0);
                break;
        }
        return new RectanglePortrayal(width, height, color, true);
    }

    protected static Body createBody(World world, Vec2 position, float angle, float width,
            float height, float mass) {
        BodyBuilder bb = new BodyBuilder();
        return bb.setBodyType(BodyType.DYNAMIC)
                .setPosition(position)
                .setAngle(angle)
                .setRectangular(width, height, mass)
                .setFriction(0.3f)
                .setRestitution(0.4f)
                .setGroundFriction(0.6f, 0.1f, 0.05f, 0.01f)
                .setFilterCategoryBits(FilterConstants.CategoryBits.RESOURCE)
                .build(world);
    }

    private void initAnchorPoints() {
        float halfWidth = (float) width / 2;
        float halfHeight = (float) height / 2;
        float horizontalSpacing = (float) (width / pushingRobots);
        float verticalSpacing = (float) (height / pushingRobots);

        for (Side side : Side.values()) {
            AnchorPoint[] anchorPoints = getAnchorPointsForSide(side);
            for (int i = 0; i < pushingRobots; i++) {
                final float x, y;
                if (side == Side.LEFT) {
                    x = -halfWidth;
                    y = -halfHeight + verticalSpacing * (i + 0.5f);
                } else if (side == Side.RIGHT) {
                    x = halfWidth;
                    y = -halfHeight + verticalSpacing * (i + 0.5f);
                } else if (side == Side.TOP) {
                    x = -halfWidth + horizontalSpacing * (i + 0.5f);
                    y = halfHeight;
                } else { // Side.BOTTOM
                    x = -halfWidth + horizontalSpacing * (i + 0.5f);
                    y = -halfHeight;
                }
                anchorPoints[i] = new AnchorPoint(new Vec2(x, y), side);
            }
        }
    }

    private void initDetectionPoints(){
        float halfWidth = (float) width / 2;
        float halfHeight = (float) height / 2;
        float xspacing = halfWidth;
        float yspacing = halfHeight;

        Vec2 leftPos = new Vec2(-halfWidth-xspacing, -halfHeight);
        Vec2 rightPos = new Vec2(halfWidth+xspacing, -halfHeight);
        Vec2 topPos = new Vec2(0, halfHeight+yspacing);
        Vec2 bottomPos = new Vec2(0, -halfHeight-yspacing);

        DetectionPoint leftPoint = new DetectionPoint(leftPos, getBody());
        DetectionPoint rightPoint = new DetectionPoint(rightPos, getBody());
        DetectionPoint topPoint = new DetectionPoint(topPos, getBody());
        DetectionPoint bottomPoint = new DetectionPoint(bottomPos, getBody());

        detectionPoints[0] = leftPoint;
        detectionPoints[1] = rightPoint;
        detectionPoints[2] = topPoint;
        detectionPoints[3] = bottomPoint;
    }

    private void initWeldPoints(){

        float halfWidth = (float) width / 2;
        float halfHeight = (float) height / 2;
        float xspacing = 0.1f;
        float yspacing = 0.1f;

        Vec2 leftPos = new Vec2(-halfWidth-xspacing, 0);
        Vec2 rightPos = new Vec2(halfWidth+xspacing, 0);
        Vec2 topPos = new Vec2(0, halfHeight+yspacing);
        Vec2 bottomPos = new Vec2(0, -halfHeight-yspacing);

        WeldPoint leftPoint = new WeldPoint(leftPos);
        WeldPoint rightPoint = new WeldPoint(rightPos);
        WeldPoint topPoint = new WeldPoint(topPos);
        WeldPoint bottomPoint = new WeldPoint(bottomPos);

        weldPoints[0] = leftPoint;
        weldPoints[1] = rightPoint;
        weldPoints[2] = topPoint;
        weldPoints[3] = bottomPoint;
    }

    // public void setGridPosition(int[] gridPos) {
    //
    //     gridPosition = new Vec2();
    //     gridPosition.set( (float) gridPos[0], (float) gridPos[1] );
    // }

    // public Vec2 getGridPosition() {
    //     return gridPosition;
    // }

    public void setVisited(boolean flag) {
        visited = flag;
    }

    public boolean getVisited() {
        return visited;
    }

    public int getConstructionZoneID() {
        return constructionZoneID;
    }

    public void setConstructionZoneID(int id) {
        constructionZoneID = id;
    }

    // public String getConnectionReason() {
    //
    //         String reason = "";
    //         boolean isAligned = false;
    //         boolean isClose = false;
    //
    //         Body thisBody = this.getBody();
    //         Vec2 thisPos = thisBody.getPosition();
    //         AABB thisAABB = this.getAabb();
    //         float thisWidth = (float)this.getWidth();
    //
    //         Body closestBody = closestResource.getBody();
    //         Vec2 closestPos = closestBody.getPosition();
    //         AABB closestAABB = closestResource.getAabb();
    //         float closestWidth = (float)closestResource.getWidth();
    //
    //         // System.out.println("\tcomparing with " + closest);
    //         // System.out.println(this);
    //         //test for closeness
    //         int sideClosestToclosest = this.getSideNearestTo(closestPos);
    //         int sideClosestTothis = closestResource.getSideNearestTo(thisPos);
    //         // System.out.println("\t\t" + sideClosestTothis + " " + sideClosestToclosest);
    //
    //         if (pushedByMaxRobots()) {
    //             reason += "Is pushed by max robots ";
    //         }
    //
    //         if ((sideClosestTothis >= 0)&&(sideClosestToclosest >= 0)) {
    //
    //             isClose = true;
    //         }
    //         else {
    //             reason += "Is not near enough. ";
    //             isClose = false;
    //         }
    //
    //         if (isClose) {
    //             reason += "Is near enough ";
    //             //test for alignment
    //             float thisAngle = thisBody.getAngle();
    //             float closestAngle = closestBody.getAngle();
    //
    //             float angleDiff = thisAngle - closestAngle;
    //             float divBy90 = (float)Math.abs(angleDiff/(((float)Math.PI)/2));
    //
    //             float error = divBy90 - (float)Math.floor(divBy90);
    //
    //             if ((Math.abs(error) < 0.02)||(Math.abs(error) > 0.98)) {
    //                 reason += "is aligned.";
    //                 isAligned = true;
    //             }
    //             else {
    //                 reason += "is not aligned.";
    //             }
    //         }
    //
    //         // if ((isAligned == true) && (isClose == true)) {
    //         //     r1.setAdjacency(closest, closest.getType(), sideClosestToclosest);
    //         //     closest.setAdjacency(r1, r1.getType(), sideClosestToR1);
    //         //     return true;
    //         // }
    //         // else {
    //
    //         //     return false;
    //         // }
    //         return reason;
    // }

    /**
    Method that checks whether two resources are aligned and close enough to eachother
    @param ResourceObject r1: the first resource
    @param ResourceObject r2: the second resource
    For the two resources to be algined, the vectors, made by taking the diagonal of the AABB box's bottom left and top right corners, are either parallel or purpendicular
    For the two resources to be close enough, both resources must have one detectionPoint within the bounds of the other resource respectively
    **/

    //ALIGNMENT
    public static boolean isAdjacentAndAligned (ResourceObject r1, ResourceObject r2) {
        boolean isAligned = false;
        boolean isClose = false;

        Body r1Body = r1.getBody();
        Vec2 r1Pos = r1Body.getPosition();
        float r1Width = (float)r1.getWidth();

        Body r2Body = r2.getBody();
        Vec2 r2Pos = r2Body.getPosition();
        float r2Width = (float)r2.getWidth();

        // System.out.println(r1);
        // System.out.println("\tcomparing with " + r2);
        //test for closeness
        int sideClosestToR2 = r1.getSideNearestTo(r2Pos);
        int sideClosestToR1 = r2.getSideNearestTo(r1Pos);
        // System.out.println("\t\t" + sideClosestToR1 + " " + sideClosestToR2);

        if ((sideClosestToR1 >= 0)&&(sideClosestToR2 >= 0)) {
            // System.out.println(r1 + " " + r2);
            // if (sideClosestToR2 > 0) {
            //     sideClosestToR2--;
            // }
            // if (sideClosestToR1 > 0) {
            //     sideClosestToR1--;
            // }
            // if (sideClosestToR1 < 0) {
            //     sideClosestToR1 = Math.abs(sideClosestToR1) - 1;
            // }
            // if (sideClosestToR2 < 0) {
            //     sideClosestToR2 = Math.abs(sideClosestToR2) - 1;
            // }

            isClose = true;
        }
        else {
            isClose = false;
        }

        if (isClose) {
            //test for alignment
            // float R1Angle = r1Body.getAngle();
            // float R2Angle = r2Body.getAngle();

            // float angleDiff = R1Angle - R2Angle;
            // float divBy90 = (float)Math.abs(angleDiff/(((float)Math.PI)/2));

            // float error = divBy90 - (float)Math.floor(divBy90);

            // if ((Math.abs(error) < 0.02)||(Math.abs(error) > 0.98)) {
            //     isAligned = true;
            // }

            //r1.setAdjacency(r2, r2.getType(), sideClosestToR2);
            //r2.setAdjacency(r1, r1.getType(), sideClosestToR1);

            return true;
        }
        else {
            return false;
        }

        // if ((isAligned == true) && (isClose == true)) {
            // r1.setAdjacency(r2, r2.getType(), sideClosestToR2);
            // r2.setAdjacency(r1, r1.getType(), sideClosestToR1);
        //     return true;
        // }
        // else {

        //     return false;
        // }
    }

    /**
    Method that calculates whether or not a resource is near enough to this resource to be considered 'connected'
    @param Vec2 otherResPos
    @return int side: the side that this other resource is nearby to (-1 if not near)
    **/
    //ALIGNMENT
    public int getSideNearestTo (Vec2 otherResPos) {
        boolean result = false;
        int side = 0;
        float min = 20f;
        int closestSide = -1;

        for (int i = 0; i < detectionPoints.length; i++) {
            //Vec2 [] dpPos = detectionPoints[i].getRelativePositions();

            if (detectionPoints[i].isNearCenter(otherResPos)) {
                result = true;
                int angleQuadrant = (int)roundAngle(getBody().getAngle());
                String sideName = adjacencyMap.get(angleQuadrant)[i];
                if (sideName.equals("L")) {
                    side = 0;
                }
                else if (sideName.equals("R")) {
                    side = 1;
                }
                else if (sideName.equals("T")) {
                    side = 2;
                }
                else {
                    side = 3;
                }
                break;
            }
        }

        if (result) {
            return side;
        }

        else {
            return closestSide;
        }
    }

    /**
    Sets the adjacent lists based on an array of resources taken from the corresponding Von Neumann points in the
    discritized construction space.
    **/
    public void setAdjacency(ResourceObject[] adjRes) {
        for (int i = 0; i < adjRes.length; i++) {
            if (adjRes[i] != null) {
                adjacentResources[i] = adjRes[i].getType();
                adjacentList[i] = adjRes[i];
            }
        }
    }

    //ALIGNMENT
    public void setAdjacency (ResourceObject r, String rType, int i) {
        adjacentList[i] = r;
        adjacentResources[i] = rType;
    }

    public void updateAdjacent(ArrayList<ResourceObject> resourceArray){

        //System.out.println("ResourceObject: updating the adjacent resources");

        countConnected = 0;

        for(int i=0;i<adjacentResources.length;i++){
            adjacentResources[i] = "_";
            adjacentList[i] = null;
        }

        for(int j=0;j<resourceArray.size();j++){

            if(this == resourceArray.get(j)){
                continue;
            }
            else {

                ResourceObject otherResource = resourceArray.get(j);
                Body resourceBody = otherResource.getBody();
                Vec2 resourcePosition = resourceBody.getPosition();

                for(int i = 0; i < 4; i++) {

                    if (resourcePosition.sub(detectionPoints[i].getRelativePosition(this.getBody())).length() < (0.1f+Simulation.DISCR_GAP) ) {

                        int side = getSideNearestTo(resourcePosition);

                        if(side > -1) {
                            // System.out.println("ResourceObject: printing the neighbours for: " + this);
                            // System.out.println("ResourceObject: PRINTING THE NEIGHBOURS BEFORE: " + Arrays.toString(adjacentList));
                            // System.out.println("ResourceObject: PRINTING THE NEIGHBOURS BEFORE: " + Arrays.toString(adjacentResources));
                            if(isAdjacentAndAligned(this, otherResource)) {
                                adjacentList[i] = otherResource;
                                adjacentResources[i] = otherResource.getType();
                                countConnected++;
                            }
                            // System.out.println("ResourceObject: PRINTING THE NEIGHBOURS AFTER: " + Arrays.toString(adjacentList));
                            // System.out.println("ResourceObject: PRINTING THE NEIGHBOURS AFTER: " + Arrays.toString(adjacentResources));
                            // System.out.println("");
                        }
                    }
                }
            }
        }
    }

    public boolean isFullyWelded(){
        return fullyWelded;
    }

    public boolean hasMoved(){
        return this.hasMoved;
    }

    public boolean checkPotentialWeld(ResourceObject otherResource){

        for(int i=0;i<weldPoints.length;i++){
            for(int j=0;j<otherResource.getWeldPoints().length;j++){
                Vec2 otherResourcePosition = otherResource.getWeldPoints()[j].getRelativePosition();
                Vec2 otherResourcePositionLocal = getCachedLocalPoint(otherResourcePosition);
                float distance = weldPoints[i].getPosition().sub(otherResourcePositionLocal).length();
                if(weldPoints[i].getTaken()==false && otherResource.getWeldPoints()[j].getTaken()==false && distance < 0.5){
                    return true;
                }
            }
        }
        return false;
    }

    public WeldPoint [] getWeldPoints(){
        return weldPoints;
    }

    public String getType(){
        return type;
    }

    public void setStatic(){
        getBody().setType(BodyType.STATIC);
        this.isConstructed = true;
    }

    public boolean isConstructed(){
        return this.isConstructed;
    }

    public void setConstructed(){
        this.isConstructed = true;
    }

    public void setConstructed(boolean val) {
        this.isConstructed = val;
    }

    public String getConnectedLeft() {
        return adjacentResources[0];
    }

    public String getConnectedRight() {
        return adjacentResources[1];
    }

    public String getConnectedTop() {
        return adjacentResources[2];
    }

    public String getConnectedBottom() {
        return adjacentResources[3];
    }

    public int getNumConnected() {
        return countConnected;
    }

    public int getNumAdjacent() {

        int adjCount = 0;
        for(int k = 0; k < 4; k++) {

            if( !adjacentResources[k].equals("_") ) { //if the adjacent side of the resource is not empty
                adjCount++;
            }
        }

        return adjCount;
    }

    public String [] getAdjacentResources(){
        return adjacentResources;
    }

    public ResourceObject[] getAdjacentList() {
        return adjacentList;
    }

    private AnchorPoint[] getAnchorPointsForSide(Side side) {
        switch (side) {
            case LEFT:
                return leftAnchorPoints;
            case RIGHT:
                return rightAnchorPoints;
            case TOP:
                return topAnchorPoints;
            case BOTTOM:
                return bottomAnchorPoints;
            default:
                return null;
        }
    }

    public boolean isTrash()
    {
        return (value < 0);
    }

    public void adjustValue(SimState simState) {
        Simulation simulation = (Simulation) simState;
        this.adjustedValue = value - 0.9 * simulation.getProgressFraction() * value;
    }

    @Override
    public void step(SimState simState) {
        super.step(simState);

        if (!pendingJoints.isEmpty()) {
            // Create all the pending joints and then clear them
            for (Map.Entry<RobotObject, JointDef> entry : pendingJoints.entrySet()) {
                Joint joint = getBody().getWorld().createJoint(entry.getValue());
                joints.put(entry.getKey(), joint);
            }
            pendingJoints.clear();
        }

        // Add an additional check here in case joints fail to be destroyed
        if (isConstructed && !joints.isEmpty()) {
            for (Map.Entry<RobotObject, Joint> entry : joints.entrySet()) {
                RobotObject robot = entry.getKey();
                robot.setBoundToResource(false);
                getBody().getWorld().destroyJoint(entry.getValue());
            }
            joints.clear();
        }

        // check if all weld points have been taken
        if(fullyWelded == false){
            for(int i=0;i<weldPoints.length;i++){
                int n = 0;
                if(weldPoints[i].taken){
                    n++;
                }
                if(n==4){
                    fullyWelded = true;
                }
            }
        }

        //to get the position of the resource every 5 timesteps
        //add the position of the resource to the collection
        // if( (simState.schedule.getSteps() % 5 == 0) && (!isConstructed) ) {
        //     Vec2 currentPosition = this.getBody().getPosition();
        //     resourceTrajectory.add(currentPosition);
        // }

        if( simState.schedule.getSteps() % 5 == 0 ) {
            Vec2 currentPosition = this.getBody().getPosition();
            Vec2 resultantPosition = currentPosition.sub(new Vec2(initialX, initialY));
            resourceTrajectory.add(resultantPosition);
        }
    }

    public LinkedList<Vec2> getTrajectory() {
        return this.resourceTrajectory;
    }


    /**
     * Try join this resource to the provided robot. If successful, a weld joint will be created
     * between the resource and the robot and the method will return true.
     * @param robot The robot trying to pick up this resource
     * @return true if the pickup attempt was successful
     */
    public boolean tryPickup(RobotObject robot) {
        // Check if already collected or max number of robots already attached
        if (!canBePickedUp()) {
            //System.out.println("Pickup failed: is collected.");
            return false;
        }

        // Check if this robot is not already attached or about to be attached
        if (joints.containsKey(robot) || pendingJoints.containsKey(robot)) {
            //System.out.println("Pickup failed: about to be joined.");
            return false;
        }

        Body robotBody = robot.getBody();
        Vec2 robotPosition = robotBody.getPosition();
        Vec2 robotPositionLocal = getCachedLocalPoint(robotPosition);

        // Check the side that the robot wants to attach to
        // If it is not the sticky side don't allow it to attach
        final Side attachSide = getSideClosestToPointLocal(robotPositionLocal);
        if (stickySide != null && stickySide != attachSide) {
            return false;
        }

        AnchorPoint closestAnchor = getClosestAnchorPointLocal(robotPositionLocal);
        if (closestAnchor == null) {
            return false; // Should not happen but apparently can...
        }

        // Check robot is not unreasonably far away
        if (robotPositionLocal.sub(closestAnchor.getPosition()).length()
                > robot.getRadius() * 2.5) {
            return false;
        }

        // Set the sticky side if unset
        if (stickySide == null) {
            stickySide = attachSide;
        }

        createPendingWeldJoint(robot, closestAnchor.position);

        // Mark the anchor as taken and the robot as bound to a resource.
        closestAnchor.markTaken();
        robot.setBoundToResource(true);

        robot.incPickups();

        return true;
    }

    public boolean canBePickedUp() {
        return !isConstructed && !pushedByMaxRobots();
    }

    /**
     * Creates a weld joint definition between the resource and the robot and adds it to the set of
     * pending joints to be created.
     * @param robot The robot to weld to
     * @param anchorPoint The local point on the resource to create the weld
     */
    private void createPendingWeldJoint(RobotObject robot, Vec2 anchorPoint) {
        WeldJointDef wjd = new WeldJointDef();
        wjd.bodyA = getBody();
        wjd.bodyB = robot.getBody();
        wjd.referenceAngle = getReferenceAngle();
        wjd.localAnchorA.set(anchorPoint);
        wjd.localAnchorB.set(robot.getRadius() + 0.01f, 0); // Attach to front of robot
        wjd.collideConnected = true;

        pendingJoints.put(robot, wjd);
    }

    /**
     * Creates a weld joint definition between two resources that have been pushed together and adds it to the set of
     * pending joints to be created.
     * @param robot The resource to weld to
     * @param anchorPoint The local point on the resource to create the weld
     */
    public WeldJointDef createResourceWeldJoint(ResourceObject resource){
        // Creates weld joint attached to the center of each resource
        getBody().setTransform(getBody().getPosition(), 0);
        resource.getBody().setTransform(resource.getBody().getPosition(), 0);
        int n = 0;
        int m = 0;
        Vec2 weldJointPos1 = new Vec2(0,0);
        Vec2 weldJointPos2 = new Vec2(0,0);
        float shortestDistance = Float.MAX_VALUE;
        for(int i=0;i<weldPoints.length;i++){
            for(int j=0;j<resource.getWeldPoints().length;j++){
                if(weldPoints[i].getTaken()==false && resource.getWeldPoints()[j].getTaken()==false){
                    Vec2 otherResourcePosition = resource.getWeldPoints()[j].getRelativePosition();
                    Vec2 otherResourcePositionLocal = getCachedLocalPoint(otherResourcePosition);
                    float distance = weldPoints[i].getPosition().sub(otherResourcePositionLocal).length();
                    if(distance < shortestDistance){
                        shortestDistance = distance;
                        weldJointPos1 = weldPoints[i].position;
                        weldJointPos2 = resource.getWeldPoints()[j].getPosition();
                        n = i;
                        m = j;
                    }
                }
            }
        }

        weldPoints[n].setTaken();
        resource.getWeldPoints()[m].setTaken();

        // Creates weld joint attached to the center of each resource
        WeldJointDef wjd = new WeldJointDef();
        wjd.bodyA = getBody();
        wjd.bodyB = resource.getBody();
        wjd.localAnchorA.set(weldJointPos1);
        wjd.localAnchorB.set(weldJointPos2);
        wjd.collideConnected = true;

        return wjd;
    }

    /* Get a local point from a global one. NOTE: for internal use only */
    private Vec2 getCachedLocalPoint(Vec2 worldPoint) {
        final Vec2 localPoint = pool;
        getBody().getLocalPointToOut(worldPoint, localPoint);
        return localPoint;
    }

    /**
     * Get the side of this resource closest to the given point.
     * @param point A point in world-space
     * @return the side closest to the given point
     */
    public Side getSideClosestToPoint(Vec2 point) {
        return getSideClosestToPointLocal(getCachedLocalPoint(point));
    }

    private Side getSideClosestToPointLocal(Vec2 localPoint) {
        float halfWidth = (float) (width / 2);
        float halfHeight = (float) (height / 2);
        final Side side;
        if (localPoint.y > -halfHeight && localPoint.y < halfHeight) {
            if (localPoint.x > 0) {
                side = Side.RIGHT;
            } else {
                side = Side.LEFT;
            }
        } else if (localPoint.x > -halfWidth && localPoint.x < halfWidth) {
            if (localPoint.y > 0) {
                side = Side.TOP;
            } else {
                side = Side.BOTTOM;
            }
        } else if (Math.abs(localPoint.x) - halfWidth > Math.abs(localPoint.y) - halfHeight) {
            if (localPoint.x > 0) {
                side = Side.RIGHT;
            } else {
                side = Side.LEFT;
            }
        } else {
            if (localPoint.y > 0) {
                side = Side.TOP;
            } else {
                side = Side.BOTTOM;
            }
        }
        return side;
    }

    /** Get the side that robots can currently attach to. */
    public Side getStickySide () {
        return stickySide;
    }

    /**
     * Get the position of the closest anchor point in world coordinates, or null if all anchor
     * points have been taken.
     * @param position A position in world coordinates
     * @return The position of the closest available anchor point (in world coordinates), or null if
     *          none is available.
     */
    public AnchorPoint getClosestAnchorPoint(Vec2 position) {
        return getClosestAnchorPointLocal(getCachedLocalPoint(position));
    }

    /**
     * Get the closest anchor point to a position in world space, or null if none is available.
     * @param localPoint point in local coordinates
     * @return an {@link AnchorPoint} object that has not been taken yet, or null if unavailable
     */
    private AnchorPoint getClosestAnchorPointLocal(Vec2 localPoint) {
        // Get the side and corresponding anchor points
        final Side side = stickySide != null ? stickySide : getSideClosestToPointLocal(localPoint);
        final AnchorPoint[] anchorPoints = getAnchorPointsForSide(side);

        // Fast path for single robot resource
        if (pushingRobots == 1) {
            AnchorPoint anchorPoint = anchorPoints[0];
            return !anchorPoint.taken ? anchorPoint : null;
        }

        // Else iterate through anchor points finding closest one (generally only 2 options)
        AnchorPoint closestAnchorPoint = null;
        float shortestDistance = Float.MAX_VALUE;
        for (AnchorPoint anchorPoint : anchorPoints) {
            if (anchorPoint.taken) {
                continue;
            }

            float distance = anchorPoint.position.sub(localPoint).lengthSquared();
            if (distance < shortestDistance) {
                shortestDistance = distance;
                closestAnchorPoint = anchorPoint;
            }
        }

        return closestAnchorPoint;
    }

    /** Get the reference angle for joints for the current sticky side. */
    private float getReferenceAngle() {
        final float referenceAngle;
        if (stickySide == Side.LEFT) {
            referenceAngle = 0f;
        } else if (stickySide == Side.RIGHT) {
            referenceAngle = (float) Math.PI;
        } else if (stickySide == Side.BOTTOM) {
            referenceAngle = (float) Math.PI / 2;
        } else if (stickySide == Side.TOP) {
            referenceAngle = (float) -Math.PI / 2;
        } else {
            throw new IllegalStateException("Sticky side not set yet, cannot get reference angle");
        }
        return referenceAngle;
    }

    public Vec2 getNormalToSide(Side side) {
        final Vec2 normal;
        if (side == Side.LEFT) {
            normal = new Vec2(-1, 0);
        } else if (side == Side.RIGHT) {
            normal = new Vec2(1, 0);
        } else if (side == Side.TOP) {
            normal = new Vec2(0, 1);
        } else if (side == Side.BOTTOM) {
            normal = new Vec2(0, -1);
        } else {
            return null;
        }

        getBody().getWorldVectorToOut(normal, normal);
        return normal;
    }

    /**
     * detach the robot from the resource regardless of whether or not the resource
     * in the target area
     */
    public void forceDetach( ){
        // Break all the joints
        for (Map.Entry<RobotObject, Joint> entry : joints.entrySet()) {
            RobotObject robot = entry.getKey();
            robot.setBoundToResource(false);
            getBody().getWorld().destroyJoint(entry.getValue());
        }
        joints.clear();

        // Reset the anchor points
        if(stickySide == null){return;} // only reset them if they exist
        AnchorPoint[] anchorPoints = getAnchorPointsForSide(stickySide);
        for (AnchorPoint anchorPoint : anchorPoints) {
            anchorPoint.taken = false;
        }
    }

    /**
    Used to group rotation angles of the resource into 4 main blocks for calculating the correct adjacent sides
    **/
    public double roundAngle (double a) {

        double divBy2Pi = a/(Math.PI*2);
        double fractionalPart = divBy2Pi % 1;
        // double integralPart = divBy2Pi - fractionalPart;
        double refAngle;
        if (fractionalPart < 0) {
            refAngle = Math.PI*2 + fractionalPart*Math.PI*2;
        }
        else {
            refAngle = fractionalPart*(Math.PI*2);
        }

        double d45 = Math.PI/4;
        double d135 = 3*Math.PI/4;
        double d225 = 5*Math.PI/4;
        double d315 = 7*Math.PI/4;

        double returnQuad;

        if ((refAngle < d45)||(refAngle > d315)) {
            returnQuad = 0D;
        }
        else if ((refAngle >= d45)&&(refAngle < d135)) {
            returnQuad = 1D;
        }
        else if ((refAngle >= d135)&&(refAngle < d225)) {
            returnQuad = 2D;
        }
        else {
            returnQuad = 3D;
        }

        return returnQuad;
    }

    public double getBodyAngle () {
        // System.out.println(rotXAxis + " " + rotXAxis.length());
        return roundAngle((double)this.getBody().getAngle());
    }

    /** Check whether this resource already has the max number of robots attached to it. */
    public boolean pushedByMaxRobots() {
        return getNumberPushingRobots() >= pushingRobots;
    }

    /** Get the number of robots currently pushing/attached to this resource. */
    public int getNumberPushingRobots() {
        return joints.size() + pendingJoints.size();
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public Set<RobotObject> getPushingRobots(){
        return joints.keySet();
    }

    public double getValue() {
        return value;
    }

    /** Fitness value adjusted (decreased) for the amount of time the simulation has been running */
    public double getAdjustedValue() {
        return adjustedValue;
    }

    /**
     * Container class for points along the sticky edge of the resource where robots can attach to
     * the resource.
     */
    public class AnchorPoint {
        private final Vec2 position;
        private final Side side;

        private boolean taken = false;

        private Vec2 worldPosition = null;

        /** @param position position local to the resource */
        private AnchorPoint(Vec2 position, Side side) {
            this.position = position;
            this.side = side;
        }

        private void markTaken() {
            if (side != stickySide) {
                throw new IllegalStateException("Anchor point not on sticky side");
            }

            taken = true;
        }

        public Vec2 getPosition() {
            return position;
        }

        public Vec2 getWorldPosition() {
            if (worldPosition == null) {
                worldPosition = getBody().getWorldPoint(position);
            }

            return worldPosition;
        }

        public Side getSide() {
            return side;
        }

        public boolean isTaken() {
            return taken;
        }
    }

    public class DetectionPoint{
        private final Vec2 position;
        private boolean collided;
        private Vec2 worldPosition = null;
        private Body body;

        private DetectionPoint(Vec2 position, Body body){
            this.position = position;
            this.body = body;
        }

        private void markColliding() {
            collided = true;
        }

        public Vec2 getPosition() {
            return position;
        }

        public Vec2 getRelativePosition(Body thisResource){

            Transform bodyXFos = thisResource.getTransform();
            Vec2 relativePos = Transform.mul(bodyXFos, this.position);

            return relativePos;
        }

        public Vec2 getWorldPosition() {
            if (worldPosition == null) {
                worldPosition = getBody().getWorldPoint(position);
            }
            return worldPosition;
        }

        public boolean isNearCenter(Vec2 otherResPos) {

            Vec2 relPos = getRelativePosition(this.body);
            float distBetween = relPos.sub(otherResPos).length();

            if (distBetween < (0.1f + Simulation.DISCR_GAP)) {
                return true;
            }
            else {
                return false;
            }
        }

        public boolean isTaken() {
            return collided;
        }
    }

    public class WeldPoint {
        private final Vec2 position;
        private Vec2 worldPosition = null;
        private ResourceObject alignedResource;
        private boolean taken;

        public WeldPoint(Vec2 position) {
            this.position = position;
            this.taken = false;
        }

        public Vec2 getPosition() {
            return position;
        }

        public Vec2 getWorldPosition() {
            if (worldPosition == null) {
                worldPosition = getBody().getWorldPoint(position);
            }

            return worldPosition;
        }

        public Vec2 getRelativePosition(){
            return position.add(getBody().getPosition());
        }

        public boolean getTaken(){
            return taken;
        }

        public void setTaken(){
            this.taken = true;
        }
    }

    /*
     * Simple portrayal for drawing an additional line along the bottom of the resource to help
     * determine which way round the resource is.
     */
    private class DebugPortrayal extends PolygonPortrayal {

        public DebugPortrayal(Paint paint, boolean filled) {
            super(4, paint, filled);

            final float width = (float) getWidth();
            final float height = (float) getHeight();

            //final float dy = (float) getHeight() * 0.3f;

            float halfWidth = width / 2;
            float halfHeight = height / 2;
            vertices[0].set(-halfWidth, -halfHeight);
            vertices[1].set(halfWidth, -halfHeight);
            vertices[2].set(halfWidth, halfHeight);
            vertices[3].set(-halfWidth, halfHeight);
        }
    }

    //return size of the resource
    public int getSize()
    {
        return pushingRobots;
    }
}

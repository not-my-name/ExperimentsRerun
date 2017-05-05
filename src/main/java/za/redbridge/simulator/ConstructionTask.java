 package za.redbridge.simulator;

import org.jbox2d.dynamics.World;
import org.jbox2d.common.Vec2;
import org.jbox2d.common.Transform;
import org.jbox2d.dynamics.Body;
import org.jbox2d.common.Rot;
import org.jbox2d.dynamics.BodyType;

import za.redbridge.simulator.object.ResourceObject;
import za.redbridge.simulator.object.RobotObject;
import za.redbridge.simulator.config.SchemaConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import sim.engine.Steppable;
import org.jbox2d.dynamics.joints.Joint;
import org.jbox2d.dynamics.joints.JointDef;
import org.jbox2d.dynamics.joints.WeldJointDef;
import sim.engine.SimState;
import sim.engine.Stoppable;
import sim.util.Bag;
import sim.field.grid.SparseGrid2D;
import java.util.Collections;
import java.util.List;
import java.util.LinkedList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.TreeSet;

/*
 *  The construction task class
 *
 */

public class ConstructionTask implements Steppable{
    public static final int MAX_CZS = 3;

    private ArrayList<ResourceObject> resources;
    private ArrayList<RobotObject> robots;
    private SchemaConfig schema;
    private HashMap<ResourceObject, ArrayList<ResourceObject>> weldMap;
    private World physicsWorld;
    private FitnessStats fitnessStats;
    private ConstructionZone constructionZone;
    private List<ConstructionZone> czs;
    // private boolean[] isStarted;
    private boolean IS_FIRST_CONNECTED = true;
    private int maxSteps = 0;
    // private final boolean paramTuning = true;

    private double aveRobotDistance;
    private double avePickupCount;
    private double aveResDistance;
    private double numAdjacentResources;
    private double numCorrectlyConnected;
    private double aveResDistanceFromCZ;
    private final double maxDistance;

    private int totalResourceValue = 0;

    private final int schemaNumber;
    private final List<ResourceObject> overallConstructionOrder;
    private ContToDiscrSpace discr;

    public ConstructionTask(SchemaConfig schema, ArrayList<ResourceObject> r, ArrayList<RobotObject> robots, World world, int maxSteps, int schemaNumber, double envWidth, double envHeight){
        this.schema = schema;
        resources = r;
        this.robots = robots;
        weldMap = new HashMap<ResourceObject, ArrayList<ResourceObject>>();
        for(int i=0;i<resources.size();i++){
            ArrayList<ResourceObject> temp = new ArrayList<ResourceObject>();
            weldMap.put(resources.get(i), temp);
        }

        for (ResourceObject res : resources) {
            totalResourceValue += res.getValue();
        }
        // System.out.println("ROBOT POSITIONS");
        // for (RobotObject robot : robots) {
        //     System.out.println("\t" + robot.getBody().getPosition());
        // }

        double numResources = (double)resources.size();
        // int maxCZs = (int)Math.floor(numResources/2D);
        czs = new LinkedList<>();
        // isStarted = new boolean[MAX_CZS];

        physicsWorld = world;
        // constructionZone = new ConstructionZone(maxSteps);  //initialise the target area (which will be updated once the first two resources have been joined)
        // update();
        this.maxSteps = maxSteps;
        fitnessStats = new FitnessStats(maxSteps);

        this.schemaNumber = schemaNumber;

        maxDistance = Math.sqrt(Math.pow(envWidth, 2) + Math.pow(envHeight, 2));
        overallConstructionOrder = new LinkedList<>();
        
        // System.out.println(constructionZone);
    }

    public void addResources(ArrayList<ResourceObject> r){
        resources = r;
        for(ResourceObject resource : resources){
            resource.updateAdjacent(resources);
        }
        for(int i=0;i<resources.size();i++){
            ArrayList<ResourceObject> temp = new ArrayList<ResourceObject>();
            weldMap.put(resources.get(i), temp);
        }
    }

    public ArrayList getSimulationResources() {
        return resources;
    }

    // public void addResources(ArrayList<ResourceObject> r){
    //     resources = r;
    //     for(ResourceObject resource : resources){
    //         resource.updateAdjacent(resources);
    //     }
    //     for(int i=0;i<resources.size();i++){
    //         ArrayList<ResourceObject> temp = new ArrayList<ResourceObject>();
    //         weldMap.put(resources.get(i), temp);
    //     }
    //     checkPotentialWeld(r.get(0), r.get(1));
    // }

    @Override
    public void step(SimState simState) {
        Simulation s = (Simulation) simState;
        discr = s.getDiscr();

        // if (getNumAdjacentResources() == resources.size()) {
        //     System.out.println("All constructed!");
        //     s.finish();
        // }
        
        for (ResourceObject resource : resources) {
            resource.updateAdjacent(resources);
        }

        // updateCZs();

        for(ResourceObject resource : resources){
            if (!resource.isConstructed()) {
                String [] resAdjacentList = resource.getAdjacentList();

                for (int i = 0; i < resAdjacentList.length; i++) {
                    /**
                    Way of doing single CZ
                    **/
                    // if (IS_FIRST_CONNECTED) {
                        // if (!resAdjacentList[i].equals("_")) {
                        //     ResourceObject otherRes = resource.getAdjacentResources()[i];
                        //     if (resource.pushedByMaxRobots() || otherRes.pushedByMaxRobots()) {
                        //         constructionZone.startConstructionZone(resource, otherRes);
                        //         // tryCreateWeld(resource, otherRes);
                        //         IS_FIRST_CONNECTED = false;
                        //     }
                            
                        // }
                    // }
                    // else {
                        // if ((!resAdjacentList[i].equals("_"))&&(!constructionZone.isInConstructionZone(resource))) {
                        //     ResourceObject otherRes = resource.getAdjacentResources()[i];
                        //     if (constructionZone.isInConstructionZone(otherRes)) {
                        //         constructionZone.addResource(resource, false);
                        //         // tryCreateWeld(resource, otherRes);
                        //     }
                        // }
                    // }
                    /**
                    End of single CZ way
                    **/
                    //If there is no CZ yet
                    // if (!isStarted[0]) {
                    if (czs.size() == 0) {
                        if (!resAdjacentList[i].equals("_")) {
                            ResourceObject neighbour = resource.getAdjacentResources()[i];
                            if (resource.pushedByMaxRobots() || neighbour.pushedByMaxRobots()) {
                                //If both resources are connected correctly according to the schema
                                if (((schema.checkConfig(schemaNumber, resource.getType(), resAdjacentList)/resource.getNumSidesConnectedTo()) == 1)) {
                                    if (discr.canBeConnected(resource, neighbour, i)) {
                                        czs.add(new ConstructionZone(maxSteps, 1));
                                        czs.get(0).startConstructionZone(resource, neighbour);
                                        // isStarted[0] = true;
                                        alignResource(resource);
                                        alignResource(neighbour);

                                        overallConstructionOrder.add(resource);
                                        overallConstructionOrder.add(neighbour);
                                    }
                                }
                            }
                        }
                    }
                    else {
                        //If this resource is adjacent to a constructed neighbour: add resource to neighbour's CZ
                        if ((!resAdjacentList[i].equals("_"))&&(resource.pushedByMaxRobots())) {
                            // boolean shouldSTartNewCZ = true;
                            ResourceObject neighbour = resource.getAdjacentResources()[i];
                            if (neighbour.isConstructed() && ((schema.checkConfig(schemaNumber, resource.getType(), resAdjacentList)/resource.getNumSidesConnectedTo()) == 1)) {
                                if (discr.canBeConnected(resource, neighbour, i)) {
                                    int czNum = neighbour.getCzNumber();
                                    czs.get(czNum-1).addResource(resource, false);
                                    alignResource(resource);
                                    overallConstructionOrder.add(resource);
                                }
                                
                            }
                            //If this resource is adjacent to an unconstructed neighbour: check that maxCZs has not been started and only create new CZ if that's not the case
                            else {
                                if (czs.size() < MAX_CZS) {
                                    if (resource.pushedByMaxRobots() || neighbour.pushedByMaxRobots()) {
                                        if ((schema.checkConfig(schemaNumber, resource.getType(), resAdjacentList)/resource.getNumSidesConnectedTo()) == 1) {
                                            updateCZs();
                                            if (discr.canBeConnected(resource, neighbour, i)) {
                                                int currNumCZs =czs.size();
                                                //Convert to Discr space
                                                czs.add(new ConstructionZone(maxSteps, currNumCZs + 1));
                                                czs.get(currNumCZs).startConstructionZone(resource, neighbour);
                                                // tryCreateWeld(resource, neighbour);
                                                // isStarted[(numCZsStarted-1)] = true;
                                                alignResource(resource);
                                                alignResource(neighbour);

                                                //Update the overall construction order (for Novelty)
                                                overallConstructionOrder.add(resource);
                                                overallConstructionOrder.add(neighbour);
                                            }
                                        }
                                        
                                    }   
                                }
                            }
                        }
                    }
                }
            }
            
        }
    }

    public boolean checkPotentialWeld(ResourceObject r1, ResourceObject r2){
        if(r1.checkPotentialWeld(r2) || r2.checkPotentialWeld(r1)){
            return true;
        }
        return false;
    }

    private WeldJointDef createWeld(ResourceObject r1, ResourceObject r2){
        WeldJointDef wjd = new WeldJointDef();
        wjd.bodyA = r1.getBody();
        wjd.bodyB = r2.getBody();
        wjd.localAnchorA.set(wjd.bodyA.getPosition());
        wjd.localAnchorB.set(wjd.bodyB.getPosition());
        wjd.collideConnected = true;
        return wjd;
    }

    private void tryCreateWeld(ResourceObject r1, ResourceObject r2){
        if(r1 != r2 && !r1.isFullyWelded() && !r2.isFullyWelded()){
            // check if join between resources has been made before
            boolean t = false;
            for(int i=0;i<weldMap.get(r1).size();i++){
                if(weldMap.get(r1).get(i)==r2){
                    t = true;
                    break;
                }
            }

            float distance = r1.getBody().getPosition().sub(r2.getBody().getPosition()).length();

            if(distance < 3f && t==false){
                if(checkPotentialWeld(r1, r2)){
                    // System.out.println("Creating weld between " + r1 + " and " + r2);
                    WeldJointDef weldDef = r1.createResourceWeldJoint(r2);
                    Joint joint = physicsWorld.createJoint(weldDef);
                    weldMap.get(r1).add(r2);
                    weldMap.get(r2).add(r1);
                    // constructionZone.getNumberOfCOnnectedResources()== 0                    // constructionZone.getNumberOfCOnnectedResources()== 0                    r1.setConstructed();
                    r2.setConstructed();
                    // TODO: work on setting static after welding
                    r1.getBody().setActive(false);
                    r2.getBody().setActive(false);
                    // r1.setStatic();
                    // r2.setStatic();
                }
            }
        }
    }

    /**
    Aligns a given resource according to the discritized constructionZone space 
    DECISION: Returns a boolean wether or not the alignment means that a new CZ must be started
    Should be triggered/called when resources are aligned in continuous space
    **/
    public void alignResource(ResourceObject res) {
        // System.out.println("CALCULATING VELOCITY");
        Body resBody = res.getBody();
        Transform xFos = resBody.getTransform();

        Transform newDiscrTransform = new Transform(discr.addResourceToDiscrSpace(res), new Rot(0f));

        /**
        Possible snapping with adhereing to the physics
        **/
        // Vec2 toTravel = newDiscrTransform.p.sub(xFos.p);

        // System.out.println("\tdisp = " + toTravel);

        // System.out.println("\tv = " + new Vec2(toTravel.x/Simulation.TIME_STEP, toTravel.y/Simulation.TIME_STEP));

        // resBody.setLinearVelocity(new Vec2(toTravel.x/Simulation.TIME_STEP, toTravel.y/Simulation.TIME_STEP));

        // float bodyAngle = resBody.getAngle();
        // float totalRotation = 0f - bodyAngle;
        // while ( totalRotation < -Math.toRadians(180)) totalRotation += Math.toRadians(360);
        // while ( totalRotation >  Math.toRadians(180) ) totalRotation -= Math.toRadians(360);
        // resBody.applyTorque( totalRotation < 0 ? -10 : 10 );
        
        xFos.set(newDiscrTransform);
        res.getPortrayal().setTransform(xFos);
        res.getBody().setTransform(xFos.p, xFos.q.getAngle());
    }

    /**
    Method to update the CZs according to the updated adjacent lists
    **/
    public void updateCZs() {
        if (czs.size() > 0) {
            List<ResourceObject[]> generatedTraversals = new LinkedList<>();
            boolean[] hasBeenChecked = new boolean[czs.size()];
            // System.out.println(czs.size() + " " + numCZsStarted);
            //For each resource (in order of construction)
            for (ResourceObject res : overallConstructionOrder) {
                int resCZNum = res.getCzNumber();

                if (!hasBeenChecked[resCZNum-1]) {
                    //generate all possible traversals

                    //Lists to be updated as traversal happens
                    List<ResourceObject> traversal = new LinkedList<>();
                    List<ResourceObject> ignoreList = new LinkedList<>();

                    //Generate the traversal
                    discr.generateTraversal(traversal, res, ignoreList);

                    //If there is no equivalent traversal already generated
                    if (!ConstructionTask.traversalsContains(traversal, generatedTraversals)) {
                        
                        //Calculate the value of the traversal
                        int tValue = 0;
                        for (ResourceObject tRes : traversal) {
                            tValue += tRes.getValue();
                        }
                        //If this traversal has a higher value than the starting resource's CZ value
                        if (tValue > czs.get(resCZNum-1).getTotalResourceValue()) {
                            generatedTraversals.add(traversal.toArray(new ResourceObject[0])); //add this traversal to the generated traversals list (should become a CZ)
                            hasBeenChecked[resCZNum-1] = true;
                        }
                        else {
                            generatedTraversals.add(czs.get(resCZNum-1).getConstructionOrder().toArray(new ResourceObject[0]));
                            hasBeenChecked[resCZNum-1] = true;
                        }
                    }
                    for (ResourceObject generalResource : resources) {
                        generalResource.setVisited(false);
                    }   
                }
            }

            if (generatedTraversals.size() > 0) {
                List<ResourceObject> newConstructionOrder = new LinkedList<>();
                int czNum = 0;
                czs.clear();
                // System.out.println("New #CZs = " + numNewCZs);
                for (ResourceObject[] newCZTraversal : generatedTraversals) {
                    // System.out.println("\t" + Arrays.toString(newCZTraversal));
                    czs.add(new ConstructionZone(newCZTraversal, czNum+1));
                    for (ResourceObject tRes : czs.get(czNum).getConstructionOrder()) {
                        newConstructionOrder.add(tRes);
                    }
                    czNum++;
                }

            }

            

            // System.out.println("BEFORE: " + czs.length);
            // List<List<ResourceObject>> bestTraversals = new LinkedList<>();
            // int newCZNumber = 1;
            // for (int i = 0; i < czs.length; i++) {
            //     List<List<ResourceObject>> czPossibilities = new LinkedList<>(); 
            //     if (czs[i] != null) {
            //         System.out.println("ORGINAL CZ: ");
            //         for (ResourceObject r : czs[i].getConnectedResources()) {
            //             System.out.println("\t" + r);
            //         }
            //         for (ResourceObject r : czs[i].getConstructionOrder()) {
            //             List<ResourceObject> updatedCZList = new LinkedList<>();
            //             List<ResourceObject> ignoreList = new LinkedList<>();
            //             discr.updateCZ(updatedCZList, r, ignoreList);
            //             czPossibilities.add(updatedCZList);
                        // for (ResourceObject res : resources) {
                        //     res.setVisited(false);
                        // }
            //         }
            //         List<ResourceObject> mostValCZ = new LinkedList<>();
            //         int bestValue = 0;
            //         for (List<ResourceObject> possibleCz : czPossibilities) {
            //             int listValue = 0;
            //             for (ResourceObject possibleRes : possibleCz) {
            //                 listValue += possibleRes.getValue();
            //             }

            //             if (listValue > bestValue) {
            //                 mostValCZ = possibleCz;
            //                 bestValue = listValue;
                            
            //             }
            //         }
            //         if (bestValue > czs[i].getTotalResourceValue()) {
            //             System.out.println("NEW BEST:");
            //             System.out.println("\t" + Arrays.toString(mostValCZ.toArray(new ResourceObject[0])));
            //             // for (List<ResourceObject> alreadyGenerated : ) {
                            
            //             // }
            //             // if (mostValCZ.equals()) {
                            
            //             // }
            //             // newCZs.add(new ConstructionZone(mostValCZ, newCZNumber));
            //             // // System.out.println("\t\t" + Arrays.toString(newCZs.get(newCZNumber-1).getConstructionOrder().toArray(new ResourceObject[0])));
            //             // newCZNumber++;
            //         }
                    
            //         // System.out.println("AFTER:");
            //         // for (ResourceObject r : mostValCZ) {
            //         //     System.out.println("\t" + r);
            //         // }
            //     }
            // }
            
            // if (newCZs.size() == 0) {
            //     System.out.println("Nothing to update");
            // }
            // else {
            //     czs = new ConstructionZone[newCZs.size()];
            //     System.out.println("AFTER: " + czs.length);
            //     for (int i = 0; i < czs.length; i++) {
            //         czs[i] = new ConstructionZone(newCZs.get(i).getConstructionOrder(), i);
            //         System.out.println("\t" + Arrays.toString(czs[i].getConstructionOrder().toArray(new ResourceObject[0])));
            //     }
            // }
            

            // System.out.println("BEFORE UPDATE: " + czs.length + "/" + numCZsStarted);
            // for (int i = 0; i < numCZsStarted; i++) {
            //     System.out.println("CZ: " + czs[i]);
            //     int cnt = 1;
            //     for (ResourceObject r : czs[i].getConstructionOrder()) {
            //         System.out.println("\t" + cnt + " " + r);
            //         cnt ++;
            //     }
            // }
            // // ConstructionZone[] updated = discr.getUpdatedCZs(overallConstructionOrder);
            // ConstructionZone[] updated = discr.getUpdatedCZs(czs);
            // if (updated != czs) {
            //     System.out.println("UPDATED!!");
            //     System.out.println(Arrays.toString(updated));
            //     int numReturned = 0;
            //     for (int i = 0; i < updated.length; i++) {
            //         if (updated[i] != null) {
            //             numReturned++;
            //         }
            //     }

            //     czs = new ConstructionZone[numReturned];
            //     for (int i = 0; i < numReturned; i++) {
            //         czs[i] = updated[i];
            //         // System.out.println("\t" + czs[i]);
            //     }
            //     numCZsStarted = czs.length;
            //     System.out.println("AFTER UPDATE: " + czs.length);
            //     for (int i = 0; i < czs.length; i++) {
            //         System.out.println("CZ: " + czs[i]);
            //         int cnt = 1;
            //         for (ResourceObject r : czs[i].getConstructionOrder()) {
            //             System.out.println("\t" + cnt + " " + r);
            //             cnt ++;
            //         }
            //     }
            // }
            // czs = new ConstructionZone[updated.length];
            // for (int i = 0; i < updated.length; i++) {
            //     czs[i] = updated[i];
            //     System.out.println(czs[i].getCZColor());
            // }
            // discr.printGrid();
            // for (ResourceObject res : resources) {
            //     if (res.isConstructed()) {

            //         res.setAdjacency(discr.getResNeighbourhood(res));
            //         ResourceObject[] adjResources = res.getAdjacentResources();
            //         for (int i = 0; i < adjResources.length; i++) {
            //             if (adjResources[i] != null) {
            //                 if (adjResources[i].isConstructed()) {
            //                     if (adjResources[i].getCzNumber() != res.getCzNumber()) {
            //                         adjResources[i].setCzNumber(res.getCzNumber());
            //                     }
            //                 }
            //             }
                        
            //         }
            //         // System.out.println("THE NEIGHBOURS FOR: " + res);
            //         // ResourceObject[] neighbours = discr.getResNeighbourhood(res);
            //         // // Object[] adjRes = discr.getResourceNeighbourhood(res).toArray(new ResourceObject[0]);
            //         // // System.out.println(Arrays.toString(neighbours.objs)); 
            //         // for (int i = 0; i < neighbours.length; i++) {
            //         //     System.out.println("\t" + neighbours[i] + ": " + Arrays.toString(discr.getGridPos(res.getBody().getPosition())));      
            //         // }  
            //     }
            // }
            // czs = new ConstructionZone[(numCZsStarted-1) + 1];
            // for (int i = 0; i < czs.length; i++) {
            //     czs[i] = new ConstructionZone(maxSteps, i);
            // }

            // for (ResourceObject res : resources) {
            //     if (res.isConstructed()) {
            //         czs[res.getCzNumber()].addResource(res, true);
            //     }
            // }

            // for (int i = 0; i < czs.length; i++) {
            //     czs[i].updateCZCenter();
            // }
        }
        
    }

    /**
    Compare generated traversal with all previous traversals for any equivalence. If so, return true (and don't add t)
    **/
    public static boolean traversalsContains(List<ResourceObject> t, List<ResourceObject[]> traversals) {
        ResourceObject[] tCopy = t.toArray(new ResourceObject[0]);
        // List<ResourceObject> tCopy = new LinkedList<>();
        // for (ResourceObject resInTraversal : t) {
        //     tCopy.add(resInTraversal);
        // }
        // boolean doesContain = false;
        // for (List<ResourceObject> prevTraversal : traversals) {
        //     List<ResourceObject> ptCopy = new LinkedList<>();
        //     for (ResourceObject resInTraversal : prevTraversal) {
        //         ptCopy.add(resInTraversal);
        //     }
        //     if (tCopy.equals(ptCopy)) {
        //         doesContain = true;
        //         break;
        //     }
        // }
        // return doesContain;

        boolean doesContain = false;

        for ( ResourceObject[] prevTraversal : traversals) {
            // System.out.println("\tComparing t:");
            // System.out.println("\t\t" + Arrays.toString(t.toArray(new ResourceObject[0])));
            // System.out.println("\twith pt:");
            // System.out.println("\t\t" + Arrays.toString(prevTraversal));

            //Loop through prev. traversal and check if all elements are contained in t

            boolean isTraversalEquiv = true;

            for (ResourceObject ptRes : prevTraversal) {
                if (!t.contains(ptRes)) {
                    isTraversalEquiv = false;
                    break;
                }
                
            }
            if (isTraversalEquiv) {
                doesContain = true;
                break;
            }
            // if (Arrays.equals(tCopy, prevTraversal)) {
            //     doesContain = true;
            //     break;
            // }
        }

        return doesContain;
    }

    // public void update(ArrayList<ResourceObject> r){
    //     resources = r;
    //     for(ResourceObject resource : resources){
    //         resource.updateAdjacent(resources);
    //     }
    // }

    // public void update(){  
        // for(ResourceObject resource : resources){
        //     if (!resource.isConstructed()) {
        //         resource.updateAdjacent(resources);
        //         String [] resAdjacentList = resource.getAdjacentList();
        //         for (int i = 0; i < resAdjacentList.length; i++) {
        //             /**
        //             Way of doing single CZ
        //             **/
        //             // if (IS_FIRST_CONNECTED) {
        //                 // if (!resAdjacentList[i].equals("_")) {
        //                 //     ResourceObject otherRes = resource.getAdjacentResources()[i];
        //                 //     if (resource.pushedByMaxRobots() || otherRes.pushedByMaxRobots()) {
        //                 //         constructionZone.startConstructionZone(resource, otherRes);
        //                 //         // tryCreateWeld(resource, otherRes);
        //                 //         IS_FIRST_CONNECTED = false;
        //                 //     }
                            
        //                 // }
        //             // }
        //             // else {
        //                 // if ((!resAdjacentList[i].equals("_"))&&(!constructionZone.isInConstructionZone(resource))) {
        //                 //     ResourceObject otherRes = resource.getAdjacentResources()[i];
        //                 //     if (constructionZone.isInConstructionZone(otherRes)) {
        //                 //         constructionZone.addResource(resource, false);
        //                 //         // tryCreateWeld(resource, otherRes);
        //                 //     }
        //                 // }
        //             // }
        //             /**
        //             End of single CZ way
        //             **/
        //             //If there is no CZ yet
        //             if (!isStarted[0]) {
        //                 if (!resAdjacentList[i].equals("_")) {
        //                     ResourceObject otherRes = resource.getAdjacentResources()[i];
        //                     if (resource.pushedByMaxRobots() || otherRes.pushedByMaxRobots()) {
        //                         //If both resources are connected correctly according to the schema
        //                         // System.out.println(resource + ": " + Arrays.toString(resource.getAdjacentList()) + " " + schema.checkConfig(schemaNumber, resource.getType(), resAdjacentList));
        //                         // System.out.println(otherRes + ": " + Arrays.toString(otherRes.getAdjacentList())+ " " + schema.checkConfig(schemaNumber, resource.getType(), resAdjacentList));
        //                         if ((schema.checkConfig(schemaNumber, resource.getType(), resAdjacentList) > 0)&&(schema.checkConfig(schemaNumber, otherRes.getType(), otherRes.getAdjacentList()) > 0)) {
        //                             czs[0] = new ConstructionZone(maxSteps, 0);
        //                             czs[0].startConstructionZone(resource, otherRes);
        //                             isStarted[0] = true;
        //                         }
                                

        //                         // czs[0].startConstructionZone(resource, otherRes);
        //                         // tryCreateWeld(resource, otherRes);
                                
                                
        //                     }
        //                 }
        //             }
        //             else {
        //                 if ((!resAdjacentList[i].equals("_"))&&(!resource.isConstructed())) {
        //                     // boolean shouldSTartNewCZ = true;
        //                     ResourceObject otherRes = resource.getAdjacentResources()[i];
        //                     if (otherRes.isConstructed() && (schema.checkConfig(schemaNumber, resource.getType(), resAdjacentList) > 0)) {
        //                         int czNum = otherRes.getCzNumber();
        //                         czs[czNum-1].addResource(resource, false);
        //                     }
        //                     else {
        //                         if (resource.pushedByMaxRobots() || otherRes.pushedByMaxRobots()) {
        //                             if ((schema.checkConfig(schemaNumber, resource.getType(), resAdjacentList) > 0)&&(schema.checkConfig(schemaNumber, otherRes.getType(), otherRes.getAdjacentList()) > 0)) {
        //                                 numCZsStarted++;
        //                                 czs[(numCZsStarted-1)] = new ConstructionZone(maxSteps, numCZsStarted);
        //                                 czs[(numCZsStarted-1)].startConstructionZone(resource, otherRes);
        //                                 // tryCreateWeld(resource, otherRes);
        //                                 isStarted[(numCZsStarted-1)] = true;
        //                             }
                                    
        //                         }
        //                     }
        //                 }
        //             }
        //         }
        //     }
            
        // }
    // }

    /**
    Get the average distance of each robot to their respective nearest resources
    **/
    public double getAveRobotDistance() {
        double aveDist = 0D;
        // System.out.println(resources.get(0));
        ResourceObject firstResource = resources.get(0);
        double [] robotDistances = new double [robots.size()];
        double sumDistances = 0D;
        Vec2 frPos = firstResource.getBody().getPosition();
        int index = 0;
        //initiliaze distances with each robot's distance to firstResource
        for (RobotObject r : robots) {
            Vec2 robotPos = r.getBody().getPosition();
            Vec2 dist = robotPos.add(frPos.negate());
            robotDistances[index] = Math.sqrt(Math.pow(dist.x,2) + Math.pow(dist.y,2));
        }
        for (RobotObject r : robots) {
            Vec2 robotPos = r.getBody().getPosition();
            int cnt = 0;
            for (ResourceObject res : resources) {
                if (cnt > 0) {
                    Vec2 resPos = res.getBody().getPosition();
                    Vec2 dist = robotPos.add(resPos.negate());
                    double distBetween = Math.sqrt(Math.pow(dist.x,2) + Math.pow(dist.y,2));
                    if (distBetween < robotDistances[index]) {
                        robotDistances[index] = distBetween;
                    }
                }
            }
            sumDistances += robotDistances[index];
        }
        return sumDistances/(double)robots.size();
    }

    /**
    Get the average distance between the resources and their closest (furthest??) resource
    **/
    public double getAveResourceDistance() {
        double sumDistances = 0D;

        for (ResourceObject res : resources) {
            Vec2 resPos = res.getBody().getPosition();
            int cnt = 0;
            // for (ResourceObject otherRes : resources) {
            //     if (res != otherRes) {
            Vec2 otherResPos = res.getClosestResource().getBody().getPosition();
            Vec2 dist = resPos.add(otherResPos.negate());

            // if (res.getCzNumber() == res.getClosestResource().getCzNumber()) {
            //     System.out.println("YES!!!!");
            // }
                
            /**
            DECISION: still count resources that have been constructed?
            **/
            if (!(res.isConstructed())) {
                double distBetween = dist.length();
                sumDistances += distBetween;
            }

            
                // }
            // }
            
        }
        return sumDistances/(double)resources.size();
    }

    public double getAveRobotPickups () {
        double pickupCount = 0D;
        for (RobotObject r : robots) {
            pickupCount += r.getPickupCount();
        }
        return pickupCount/robots.size();
    }

    public double getTotalRobotPickups () {
        double pickupCount = 0D;
        for (RobotObject r : robots) {
            pickupCount += r.getPickupCount();
        }
        return pickupCount;
    }

    public double getMostValuableCZ() {
        
        if (czs.size() == 0) {
            return 0D;
        }
        else {
            ConstructionZone largest = czs.get(0);
            double largestValue = largest.getTotalResourceValue();
            for (ConstructionZone cz : czs) {
                if (cz.getTotalResourceValue() > largestValue) {
                    largest = cz;
                    largestValue = cz.getTotalResourceValue();
                }
            }
            double czValue = largest.getTotalResourceValue()/totalResourceValue;
            return czValue;
        }
        
    }

    public double getLargestCZ () {
        if (czs.size() == 0) {
            return 0D;
        }
        else {
            ConstructionZone largest = czs.get(0);
            double largestSize = largest.getNumberOfConnectedResources();
            for (ConstructionZone cz : czs) {
                if (cz.getNumberOfConnectedResources() > largestSize) {
                    largest = cz;
                    largestSize = cz.getNumberOfConnectedResources();
                }
            }
            // double czValue = largest.getTotalResourceValue()/totalResourceValue;
            return largestSize;
        }
    }

    public int getNumAdjacentResources () {

        int connectedCount = 0;
        if (czs.size() > 0) {
            // System.out.println(numCZsStarted);
            for (ConstructionZone cz : czs) {
                connectedCount += cz.getNumberOfConnectedResources();
            }
        }
        return connectedCount;
    }

    public double getNumAsConnected() {
        double aCount = 0D;
        if (czs.size() > 0) {
            for (ConstructionZone cz : czs) {
                aCount += cz.getResTypeCount()[0];
            }
        }
        return aCount;
    }

    public double getNumBsConnected() {
        double bCount = 0D;
        if (czs.size() > 0) {
            for (ConstructionZone cz : czs) {
                bCount += cz.getResTypeCount()[1];
            }
        }
        return bCount;
    }

    public double getNumCsConnected() {
        double cCount = 0D;
        if (czs.size() > 0) {
            for (ConstructionZone cz : czs) {
                cCount += cz.getResTypeCount()[2];
            }
        }
        return cCount;
    }

    // public double getAveDistanceFromCZ () {
    //     if (getNumConstructionZoneResources() > 0) {
    //         Vec2 czPos = constructionZone.getCZPos();
    //         float dist = 0f;
    //         for (ResourceObject res : resources) {
    //             Vec2 resPos = res.getBody().getPosition();
    //             dist += resPos.sub(czPos).length();
    //         }
    //         double aveDist = (double)dist/resources.size();
    //         return aveDist;
    //     }
    //     else {
    //         return -1D;
    //     }
    // }

    public double getAveDistanceToNearestCZ () {
        Vec2[] czCenters = getConstructionZoneCenter();

        double aveDist = 0D;
        int numResOutsideCZ = 0;
        if (czs.size() > 0) {
            for (ResourceObject r : resources) {
                if (!r.isConstructed()) {
                    numResOutsideCZ ++;
                    Vec2 rPos = r.getBody().getPosition();
                    double nearest = maxDistance;
                    int cnt = 0;
                    for (ConstructionZone cz : czs) {
                        double distToCZ = (double)rPos.sub(czCenters[cnt]).length();
                        if (distToCZ < nearest) {
                            nearest = distToCZ;
                        }
                        cnt++;
                    }
                    aveDist += nearest;
                }
            }
        }

        if (numResOutsideCZ == 0) {
            return 0;
        }
        else {
            return aveDist/numResOutsideCZ;
        }
    }

    public int getNumConstructionZoneResources () {
        // return constructionZone.getNumberOfConnectedResources();
        int numResources = 0;
        if (czs.size() > 0) {
            for (ConstructionZone cz : czs) {
                numResources += cz.getNumberOfConnectedResources();
            }
        }
        
        return numResources;
    }

    public void printConnected(){
        for(ResourceObject resource : resources){
            String [] adjacentResources = resource.getAdjacentList();
            for(int i=0;i<adjacentResources.length;i++){
                System.out.print(adjacentResources[i]+" ");
            }
            System.out.println();
        }
    }

    public int checkSchema(int i){
        int correctSides = 0;
        for(ResourceObject resource : resources){
            correctSides += schema.checkConfig(i,resource.getType(), resource.getAdjacentList());
        }
        return correctSides;
    }

    public int[] configResQuantity(int i){
        return schema.getResQuantity(i);
    }

    public ArrayList<RobotObject> getRobots() {
        return robots;
    }

    public ArrayList<ResourceObject> getResources() {
        return resources;
    }

    public Vec2 getConstructionZoneCenter(int czNum) {
        return czs.get(czNum-1).getCZPos();
    }

    public Vec2[] getConstructionZoneCenter () {
        Vec2[] czCenters = new Vec2[czs.size()];
        int cnt = 0;
        for (ConstructionZone cz : czs) {
            czCenters[cnt] = cz.getCZPos();
            cnt++;
        }
        // return constructionZone.getCZPos();
        return czCenters;
    }

    public ResourceObject[] getOverallConstructionOrder() {
        return overallConstructionOrder.toArray(new ResourceObject[0]);
    }

    public ConstructionZone getConstructionZone() {
        return constructionZone;
    }

    public ConstructionZone[] getConstructionZones() {
        ConstructionZone[] returnedCZs;
        if (czs.size() == 0) {
            returnedCZs = new ConstructionZone[1];
            returnedCZs[0] = new ConstructionZone(maxSteps, 0);
        }

        else {
            returnedCZs = czs.toArray(new ConstructionZone[0]);
        }
        
        return returnedCZs;
    }

    // public int getNumResourcesCorrectlyConnected() {
    //     return constructionZone.getNumCorrectlyConnected(schema, schemaNumber);
    // }

    public double getNumResourcesCorrectlyConnected() {
        double numCorrect = 0D;
        if (czs.size() > 0) {
            for (ConstructionZone cz : czs) {
                numCorrect += cz.getCZCorrectness(schema, schemaNumber)/resources.size();
            }
        }
        return numCorrect;
    }

    /**
    DECISION: what is considered complete??
    **/
    public boolean isComplete() {
        if (getNumAdjacentResources() == resources.size()) {
            return true;
        }
        else {
            return false;
        }
    }

    /**
    Methods for fitness evaluation
    **/

    /**
    Method to evaluate the overall objective fitness of a simulation run:
    @param w the weights for each factor of the fitness function
    If it is given 6 weights, assess fitness based on:
        A - average distance between robot and closest resource to it
        B - average number of times that robots connected to resources
        C - average distance between resources
        D - the number of adjacent resources
        E - the number of adjacent resources that are in the correct schema
        F - average distance between resources and the construction starting area
    If 4 are given (use the function given in Geoff's 2015 paper):
        A - number of times a robot successfully found blocks
        B - number of times type A blocks were pushed by one robot and connected with a built structure
        C - number of times type B blocks were pushed by two robots and connected with a built structure
        D - number of times type C blocks were pushed by two robots and connected with a built structure
    **/
    public double getObjectiveFitness (double [] w) {
        if (w.length == 6) {
            double normalizationFactor = 0D;
            for (int i = 0; i < w.length; i++) {
                normalizationFactor += w[i];
            }

            // if (paramTuning) {
            //     aveRobotDistance = 1;
            //     avePickupCount = 1;
            //     aveResDistance = 1;
            //     numAdjacentResources = 1;
            //     numCorrectlyConnected = 1;
            //     aveResDistanceFromCZ = 1;
            // }
            // else {
            aveRobotDistance = (maxDistance - getAveRobotDistance())/maxDistance;

            /**
            NEED TO CHECK@!!!
            **/
            avePickupCount = getAveRobotPickups()/schema.getTotalRobotsRequired(schemaNumber);

            aveResDistance = (maxDistance - getAveResourceDistance())/maxDistance;

            
            // System.out.println(numCorrectlyConnected + " " + numAdjacentResources + " = " + (numCorrectlyConnected + numAdjacentResources));
            // return 0D;
            
            if (czs.size() == 0) {
                double totalFitness = w[0]*aveRobotDistance + w[1]*avePickupCount + w[2]*aveResDistance;
                totalFitness = totalFitness/normalizationFactor;
                // System.out.println("Fitness from sim run (no construction zones):");
                // System.out.println("Factors: aveRobotDistance avePickupCount aveResDistance");
                // System.out.println("\t(" + w[0] + ")" + aveRobotDistance + " + (" + w[1] + ")" + avePickupCount + " + (" + w[2] + ")" + aveResDistance);
                // System.out.println("Total fitness = " + totalFitness);
                // System.out.println("ASDASDASD");
                // System.out.println(aveRobotDistance + " " + avePickupCount + " " + aveResDistance + " " + numAdjacentResources);
                return totalFitness;
            }
            else {
                updateCZs();
                numAdjacentResources = (double)getNumAdjacentResources()/resources.size();
                /**
                TAKE AWAY BECAUSE BLOCKS CAN ONLY CONNECT IF CORRECT
                **/
                // numCorrectlyConnected = (double)getNumResourcesCorrectlyConnected()/(numCZsStarted+1);
                numCorrectlyConnected = getMostValuableCZ();
                aveResDistanceFromCZ = 1/(1 + getAveDistanceToNearestCZ());
                // System.out.println(aveRobotDistance + " " + avePickupCount + " " + aveResDistance + " " + numAdjacentResources + " " + numCorrectlyConnected + " " + aveResDistanceFromCZ);
                // double constructionZoneFitness = constructionZone.getFitnessStats().getTeamFitness();
                // constructionFitness += constructionZoneFitness;
                double totalFitness = w[0]*aveRobotDistance + w[1]*avePickupCount + w[2]*aveResDistance + w[3]*numAdjacentResources + w[4]*numCorrectlyConnected + w[5]*aveResDistanceFromCZ;
                totalFitness = totalFitness/normalizationFactor;
                // System.out.println("Fitness from sim run (with construction zones):");
                // System.out.println("Factors: aveRobotDistance avePickupCount aveResDistance numAdjacentResources numCorrectlyConnected aveResDistanceFromCZ");
                // System.out.println("\t(" + w[0] + ")" + aveRobotDistance + " + (" + w[1] + ")" + avePickupCount + " + (" + w[2] + ")" + aveResDistance + " + (" + w[3] + ")" + numAdjacentResources + " + (" + w[4] + ")" + numCorrectlyConnected + " + (" + w[5] + ")" + aveResDistanceFromCZ);
                // System.out.println("Total fitness = " + totalFitness);
                return totalFitness;
            }
                
            // }   
        }
        else {
            double objectiveFitness = 0D;
            double normalization = w[0]*schema.getTotalRobotsRequired(schemaNumber) + w[1]*schema.getResQuantity(schemaNumber)[0] + w[2]*schema.getResQuantity(schemaNumber)[1] + w[3]*schema.getResQuantity(schemaNumber)[2];
            // double normalization = w[3]*schema.getTotalResources(schemaNumber) + w[0]*schema.getTotalRobotsRequired(schemaNumber);
            // System.out.println("normalization = " + normalization);
            objectiveFitness += w[0]*getTotalRobotPickups()/normalization;
            if (czs.size() > 0) {
                updateCZs();
                for (ConstructionZone cz : czs) {
                    objectiveFitness += w[1]*cz.getResTypeCount()[0]/normalization;
                    objectiveFitness += w[2]*cz.getResTypeCount()[1]/normalization;
                    objectiveFitness += w[3]*cz.getResTypeCount()[2]/normalization;
                }
            }
            // System.out.println("Objective Fitness = " + objectiveFitness);
            return objectiveFitness;
        }
    }

    // public void updateObjectives() {
    //     aveRobotDistance = getAveRobotDistance();
    //     avePickupCount = getAveRobotPickups();
    //     aveResDistance = getAveResourceDistance();
    //     numAdjacentResources = getNumAdjacentResources();
    //     numCorrectlyConnected = constructionZone.getNumCorrectlyConnected(schema, schemaNumber);
    //     aveResDistanceFromCZ = getAveDistanceToNearestCZ();
    // }

    public String fitnessString () {
        double [] w = {0D, 0D, 0D, 0D, 0D, 0D};
        getObjectiveFitness(w);
        String returnString = "Ave robot distance: " + aveRobotDistance + "\n";
        returnString += "Ave pickup count: " + avePickupCount + "\n";
        returnString += "Ave res distance: " + aveResDistance + "\n";
        returnString += "num adjacentResources: " + numAdjacentResources + "\n";
        returnString += "num correctly connected: " + numCorrectlyConnected + "\n";
        returnString += "ave res distance from CZ: " + aveResDistanceFromCZ + "\n";
        return returnString;
    }

    // public void printSizes() {
    //     System.out.println("#resources: " + resources.size());
    //     System.out.println("#robots: " + robots.size());
    //     discr.printSizes();
    // }

}

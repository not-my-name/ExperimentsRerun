package za.redbridge.simulator;

import org.jbox2d.dynamics.World;
import org.jbox2d.common.Vec2;
import za.redbridge.simulator.object.ResourceObject;
import za.redbridge.simulator.config.SchemaConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import sim.engine.Steppable;
import org.jbox2d.dynamics.joints.Joint;
import org.jbox2d.dynamics.joints.JointDef;
import org.jbox2d.dynamics.joints.WeldJointDef;
import sim.engine.SimState;
import org.jbox2d.dynamics.Body;
import za.redbridge.simulator.object.RobotObject;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Iterator;
import za.redbridge.simulator.ConstructionZone;
import java.util.HashSet;

import java.util.*;
import za.redbridge.simulator.ContToDiscrSpace;

import org.jbox2d.common.Transform;
import org.jbox2d.common.Rot;

/*
 *  The construction task class
 *
 */

public class ConstructionTask implements Steppable{
    //CONCURRENCY CHANGES
    private ArrayList<ResourceObject> resources;
    private SchemaConfig schema;
    private HashMap<ResourceObject, ArrayList<ResourceObject>> weldMap;
    private World physicsWorld;
    private ArrayList<RobotObject> currentRobots;

    private Vec2[] prevBotLocations;

    private FitnessStats fitnessStats;
    //private double teamFitness;

    //change this to an array of construction zones to support multiple construction zones
    //private ConstructionZone[] constructionZones;
    private ArrayList<ConstructionZone> constructionZones;
    //private ConstructionZone constructionZone;
    //private int maxSteps = 0;

    private boolean IS_FIRST_CONNECTED = true;

    private int schemaNumber;
    private HashSet<ResourceObject> movedResources;
    //private HashSet<ResourceObject> constructedResources;

    private ArrayList<ResourceObject> globalConstructionOrder;

    private Set<ResourceObject> uniqueResources = new HashSet<ResourceObject>();

    private double idealScore; //variable to store the total score possible by connecting all the resources in the simulation
    private double maxDistance; //max possible distance between two entities in the environment

    private double environmentWidth;
    private double environmentHeight;

    private ContToDiscrSpace discreteGrid; //the discretized grid

    private int constructionZoneID; //keep track of the ID of the construction zones that get created

    public ConstructionTask(SchemaConfig schema, ArrayList<ResourceObject> r, ArrayList<RobotObject> robots,
                        World world, int schemaNumber, double envWidth, double envHeight) {

        this.schema = schema;
        this.schemaNumber = schemaNumber;
        resources = r;
        currentRobots = robots;
        weldMap = new HashMap<ResourceObject, ArrayList<ResourceObject>>();

        for(int k = 0; k < resources.size(); k++) {
            ArrayList<ResourceObject> temp = new ArrayList<ResourceObject>();
            weldMap.put(resources.get(k), temp);
        }

        idealScore = 0;
        for(ResourceObject resObj : resources) {
            idealScore += resObj.getValue();
        }

        physicsWorld = world;
        int numResources = resources.size();
        this.constructionZones = new ArrayList<ConstructionZone>(); //the maximum number of construction zones possible
        movedResources = new HashSet<ResourceObject>();

        this.environmentHeight = envHeight;
        this.environmentWidth = envWidth;
        maxDistance = Math.sqrt(Math.pow(environmentWidth, 2) + Math.pow(environmentHeight, 2));
        constructionZoneID = 0; //no construction zones yet

        globalConstructionOrder = new ArrayList<>();
    }

    public ContToDiscrSpace getDiscreteGrid() {
      return discreteGrid;
    }

    public ArrayList<ResourceObject> getSimulationResources() {
        return resources;
    }

    @Override
    public void step(SimState simState) {

        Simulation simulation = (Simulation) simState;
        discreteGrid = simulation.getDiscreteGrid();

        for(ResourceObject resource : resources){
            resource.updateAdjacent(resources);
        }

        if(getTotalResourcesConnected() == resources.size()) {
            System.out.println("ConstructionTask: all resources constructed");
            simulation.finish();
        }

        for(ResourceObject resource : resources) {

            if(!resource.isConstructed()) { //only check the resources that are not in construction zones

                //get the neighbours of the current resource
                String [] resAdjacentList = resource.getAdjacentResources();
                ResourceObject[] adjObjects = resource.getAdjacentList();

                for(int i = 0; i < resAdjacentList.length; i++) { //iterate over each of the current resource's neighbours

                    if( !resAdjacentList[i].equals("_") ) { //check if the current resource has a neighbouring resource

                        ResourceObject neighbour = adjObjects[i];

                        if(resource.pushedByMaxRobots() || neighbour.pushedByMaxRobots()) { //check that at least one of the resources are being pushed by the correct number of robots

                            if( checkSchema(resource) == resource.getNumConnected() &&
                                    checkSchema(neighbour) == neighbour.getNumConnected() ) { //check that the resources can be connected to each other according to the schema

                                if(neighbour.isConstructed()) { //check if the neighbouring resource belongs to a construction zone

                                    if(discreteGrid.canBeConnected(resource, neighbour, i) ) {

                                        constructionZones.get(neighbour.getConstructionZoneID()-1).addResource(resource); //adding the resource to the existing construction zone
                                        globalConstructionOrder.add(resource);

					uniqueResources.add(resource);

                                        alignResource(resource);
                                    }

                                }
                                else { //if the 2 resources are creating a new construction zone

                                    if(constructionZones.size() < 3) { //limiting the number of construction zones for later calculations
                                        updateConstructionZones();
                                      if(discreteGrid.canBeConnected(resource, neighbour, i)) {

                                          constructionZoneID++;
                                          ConstructionZone newZone = new ConstructionZone(constructionZoneID);
                                          //constructionZoneID++;
                                          newZone.startConstructionZone(resource, neighbour);
                                          constructionZones.add(newZone);
                                          globalConstructionOrder.add(resource);
                                          globalConstructionOrder.add(neighbour);

					  uniqueResources.add(resource);
					  uniqueResources.add(neighbour);

                                          alignResource(resource);
                                          alignResource(neighbour); //WORKING
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

    /**
    should this not be changed to an &&
    */
    public boolean checkPotentialWeld(ResourceObject r1, ResourceObject r2){
        if(r1.checkPotentialWeld(r2) || r2.checkPotentialWeld(r1)){
            return true;
        }
        return false;
    }

    public ArrayList<ResourceObject> getGlobalConstructionOrder() {
        return globalConstructionOrder;
    }

    //method to find the most valuable construction zone
    //from the list of existing construction zones
    public ConstructionZone getBestConstructionZone() {

        /**
        remember to actually find the best one
        **/

        ConstructionZone returnZone; //the empty construction zone that needs to be assigned and returned
        double maxScore = 0;
        int maxIndex = -1;

        for(int k = 0; k < constructionZones.size(); k++) {

          ArrayList<ResourceObject> connectedRes = new ArrayList<>();
          connectedRes.addAll( constructionZones.get(k).getConnectionOrder() );
          double tempScore = getConstructionValue(connectedRes);

          if(tempScore > maxScore) {

            maxScore = tempScore;
            maxIndex = k;
          }
        }

        return constructionZones.get(maxIndex);
    }


    //method to return the total number of resources that are placed in the particular simulation
    public int getTotalNumResources() {
        return resources.size();
    }

    public void printConnected() {

        for(ResourceObject resource : resources){
            String [] adjacentResources = resource.getAdjacentResources();
            for(int i=0;i<adjacentResources.length;i++){
                System.out.print(adjacentResources[i]+" ");
            }
            System.out.println();
        }
    }

    public double getIdealScore() {
        return this.idealScore;
    }

    public double getMaxDistance() {
        return maxDistance;
    }

    //method to align a given resource to the underlying discretized grid
    //isFirst -> whether or not this resource is the first resource in a construction zone
    public void alignResource(ResourceObject resObj) {

        Body resourceBody = resObj.getBody();
        Transform xFos = resourceBody.getTransform();

        Transform newDiscrTransform = new Transform(discreteGrid.addResourceToDiscrSpace(resObj), new Rot(0f));
        xFos.set(newDiscrTransform);
        resObj.getPortrayal().setTransform(xFos);
        resObj.getBody().setTransform(xFos.p, xFos.q.getAngle());
    }

    public void updateConstructionZones() {

        //System.out.println("ConstructionTask: checkng the traversals");

        for(ResourceObject resObj : globalConstructionOrder) {
            resObj.updateAdjacent(globalConstructionOrder);
        }

        ArrayList<ConstructionZone> newConstructionZones = new ArrayList<>();

        // check if there are construction zones
        if (constructionZones.size() > 0) {

            //int czNum = 0;
            constructionZoneID = 0;

            // go through every construction zone
            for(ConstructionZone cz : constructionZones){
                // list of possible traversals for a construction zone
                ArrayList<ArrayList<ResourceObject>> generatedTraversals = new ArrayList<>();

                //For each resource (in order of construction)
                for (ResourceObject res : cz.getConnectionOrder()) {

                    //int resCZNum = res.getConstructionZoneID();
                    //Lists to be updated as traversal happens
                    ArrayList<ResourceObject> traversal = new ArrayList<>();
                    ArrayList<ResourceObject> ignoreList = new ArrayList<>();

                    //Generate the traversal
                    discreteGrid.generateTraversal(traversal, res, ignoreList);

                    //If there is no equivalent traversal already generated
                    if (!traversalsContains(traversal, generatedTraversals)) {

                        //Calculate the value of the traversal
                        int tValue = 0;
                        for (ResourceObject tRes : traversal) {
                            tValue += tRes.getValue();
                        }

                        //If this traversal has a higher value than the starting resource's CZ value
                        if (tValue >= cz.getTotalResourceValue()) {
                            // System.out.println("This traversal is more valuable!");
                            generatedTraversals.add(traversal); //add this traversal to the generated traversals list (should become a CZ)
                        }
                    }

                    for (ResourceObject generalResource : resources) {
                        generalResource.setVisited(false);
                    }
                }

                // loop through possibilites and get best
                if(generatedTraversals.size() > 0){
                    ArrayList<ResourceObject> bestPossibility = generatedTraversals.get(0);
                    int bestValue = 0;
                    for(ArrayList<ResourceObject> possibleTraversal : generatedTraversals){
                        int tempValue = getConstructionValue(possibleTraversal);
                        if(tempValue > bestValue){
                            bestValue = tempValue;
                            bestPossibility = possibleTraversal;
                        }
                    }
                    constructionZoneID++;
                    if(constructionZoneID == 0) {
                        System.out.println("ConstructionTask: creating zone with 0 index");
                    }
                    newConstructionZones.add(new ConstructionZone(bestPossibility, constructionZoneID));
                }
            }

		//System.out.println("ConstructionTask: updating the construcion zones");
		//System.out.println("ConstructionTask: total resources before = " + getTotalResourcesConnected());

            if(newConstructionZones.size() > 0) { //writing the new constructionZones to the old list
                constructionZones.clear();
                for(ConstructionZone cZone : newConstructionZones) {
                    constructionZones.add(cZone);
		    for(ResourceObject resTemp : cZone.getConnectedResources()) {
			uniqueResources.add(resTemp);
		    }
                }
            }

		//System.out.println("ConstructionTask: total resources after = " + getTotalResourcesConnected());
        }
    }

    private int getConstructionValue(ArrayList<ResourceObject> possibleConstructionZone) {

      int totalScore = 0;

      for(ResourceObject resObj : possibleConstructionZone) {
        totalScore += resObj.getValue();
      }

      return totalScore;
    }


    /**
    check that the method below works
    needs to join adjacent construction zones

    stop creating new construction zones after the first 3
    */

    // //method to update the construction zones according to the updated neighbour lists
    // //this method is used to join neighbouring construction zones
    // public void updateCZones() {
    //
    //     if(constructionZones.size() > 0) { //if there are existing construction zones
    //
    //         List<ResourceObject[]> generatedTraversals = new LinkedList<>();
    //         boolean[] hasBeenChecked = new boolean[constructionZones.size()]; //array to indicate which of the construction zones have been checked
    //
    //         for(ResourceObject resObj : globalConstructionOrder) {
    //             int constructionZoneID = resObj.getConstructionZoneID(); //get the ID of the construction zone that the resource is connected to
    //
    //             if(!hasBeenChecked[constructionZoneID-1]) { //check that the construction zone has not been traversed before
    //
    //                 List<ResourceObject> traversal = new LinkedList<>(); //list of resources that are correctly connected
    //                 List<ResourceObject> ignoreList = new LinkedList<>(); //list of resources that should be ignored because of incorrect connection schemas
    //
    //                 discreteGrid.generateTraversal(traversal, resObj, ignoreList);
    //
    //                 if (!traversalsContains(traversal, generatedTraversals)) {
    //
    //                     //Calculate the value of the traversal
    //                     int tValue = 0;
    //                     for (ResourceObject tRes : traversal) {
    //                         tValue += tRes.getValue();
    //                     }
    //
    //                     if (tValue > constructionZones.get(constructionZoneID-1).getTotalResourceValue()) {
    //                         generatedTraversals.add(traversal.toArray(new ResourceObject[0])); //add this traversal to the generated traversals list (should become a CZ)
    //                         hasBeenChecked[constructionZoneID-1] = true;
    //                     }
    //                     else {
    //                         generatedTraversals.add(constructionZones.get(constructionZoneID-1).getConnectionOrder().toArray(new ResourceObject[0]));
    //                         hasBeenChecked[constructionZoneID-1] = true;
    //                     }
    //                 }
    //
    //                 for (ResourceObject r : resources) { //resetting these values in preparation for the next traversal
    //                     r.setVisited(false);
    //                 }
    //             }
    //         }
    //
    //         if (generatedTraversals.size() > 0) {
    //
    //             int czNum = 1;
    //             constructionZones.clear();
    //
    //             for (ResourceObject[] newCZTraversal : generatedTraversals) {
    //
    //                 constructionZones.add( new ConstructionZone(newCZTraversal, czNum) );
    //                 czNum++;
    //                 System.out.println("ConstructionTask: the newCZTraversal = " + Arrays.toString(newCZTraversal));
    //             }
    //         }
    //     }
    // }

    //method to compare the generated traversal with previous traversals
    public boolean traversalsContains(List<ResourceObject> t, ArrayList<ArrayList<ResourceObject>> generatedTraversals) {

        ResourceObject[] tCopy = t.toArray(new ResourceObject[0]);
        boolean doesContain = false;

        for ( ArrayList<ResourceObject> prevTraversal : generatedTraversals) { //iterate over all the previous traversals

            boolean isTraversalEquiv = true;

            for (ResourceObject ptRes : prevTraversal) { //iterate over the individual resources in the traversal

                if (!t.contains(ptRes)) {

                    isTraversalEquiv = false;
                    break;
                }
            }

            if (isTraversalEquiv) { //check if the traversals are equal
                doesContain = true;
                break;
            }
        }

        return doesContain;
    }

    public int getFinalResCount() {
	return uniqueResources.size();
    }

    public Set<ResourceObject> getUniqueResources() {
	return uniqueResources;
    }

    // //method to compare the generated traversal with previous traversals
    // public boolean traversalsContains(List<ResourceObject> t, List<ResourceObject[]> traversals) {
    //
    //     ResourceObject[] tCopy = t.toArray(new ResourceObject[0]);
    //     boolean doesContain = false;
    //
    //     for ( ResourceObject[] prevTraversal : traversals) { //iterate over all the previous traversals
    //
    //         boolean isTraversalEquiv = true;
    //
    //         for (ResourceObject ptRes : prevTraversal) { //iterate over the individual resources in the traversal
    //
    //             if (!t.contains(ptRes)) {
    //
    //                 isTraversalEquiv = false;
    //                 break;
    //             }
    //         }
    //
    //         if (isTraversalEquiv) { //check if the traversals are equal
    //             doesContain = true;
    //             break;
    //         }
    //     }
    //
    //     return doesContain;
    // }

    //method to return the total number of resources in the environment that have been
    //connected to another resource
    public int getTotalResourcesConnected() {

        int total = 0;

        if(constructionZones.size() > 0) {

            for(ConstructionZone cZone : constructionZones) {

                total += cZone.getNumConnected();
            }
        }

        return total;
    }

    public ArrayList<ResourceObject> getConstructionOrder() {
        return this.globalConstructionOrder;
    }

    public int checkSchema(int i) {

        int correctSides = 0;
        for(ResourceObject resource : resources) {
            correctSides += schema.checkConfig(i, resource.getType(), resource.getAdjacentResources());
        }

        return correctSides;
    }

    /**
    added this method so that the schema can be checked for each resource at a time
    this method is being called in the Behaviour class to calculate the number of correctly connected resources for each resource
    needed to be able to send through the specific resource that needed to be checked
    */
    public int checkSchema(int schemaConfigNum, ResourceObject resObj) {

        int correctSides = 0;
        correctSides += schema.checkConfig(schemaConfigNum, resObj.getType(), resObj.getAdjacentResources());
        return correctSides;
    }

    public int checkSchema(ResourceObject resObj) {

        int correctSides = 0;
        correctSides += schema.checkConfig(schemaNumber, resObj.getType(), resObj.getAdjacentResources());
        return correctSides;
    }

    public int[] configResQuantity(int i){
        return schema.getResQuantity(i);
    }

    public ArrayList<ConstructionZone> getConstructionZones() {
        return constructionZones;
    }
}

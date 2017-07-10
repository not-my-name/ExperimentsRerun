package za.redbridge.simulator;

import java.util.*;
import za.redbridge.simulator.object.ResourceObject;
import za.redbridge.simulator.object.RobotObject;
import za.redbridge.simulator.ConstructionTask;
import za.redbridge.simulator.ConstructionZone;
import org.jbox2d.common.Vec2;
import sim.field.grid.ObjectGrid2D;

/*
this is the class that will be created for each time that an individual gets tested
one of these classes per each simulation iteration

this class needs to store and manage all the necessary information and characteristics
that are needed to calculate the novelty fitness of a solution
*/

public class NoveltyBehaviour {

	//array list of linked lists, each linked list is a trajectory (linked list of positions)
	private ArrayList<LinkedList<Vec2>> robotTrajectories;
	private ArrayList<LinkedList<Vec2>> resourceTrajectories;

	//arrays to be used to check the configurations of the blocks in the construction zone
	private ArrayList<String[]> AConnectionsList;
	private ArrayList<String[]> BConnectionsList;
	private ArrayList<String[]> CConnectionsList;

	private Vec2[] avgRobotTrajectory;
	private Vec2[] avgResourceTrajectory;

	//stores the final construction at the end of the simulation
	//private ConstructionZone[] constructionZones;
	private ArrayList<ConstructionZone> constructionZones;
	private ConstructionTask constructionTask;
	private ObjectGrid2D discreteConstructionZone;

	private ArrayList<ResourceObject> connectionOrder;

	//number of samples in a trajectory
	private int numRobotPosSamples; //number of positions that get saved in order to prepresent a trajectory (size of each trajectory array)
	private int numResPosSamples; //same as above but for the resources
	private int numRobots;
	private int numResources;

	private double populationNoveltyScore; //var to store the novelty of behaviour as compared to the rest of the individuals in the generation
	private double archiveNoveltyScore; //var to store the novelty of behaviour compared to behaviours stored in the archive
	private double simulationNoveltyScore; //var to store the novelty of the behaviour compared to the behaviours produced in the other simulation runs

	private ArrayList<Double> simulationNeighbourhood;
	private ArrayList<Double> populationNeighbourhood;
	private ArrayList<Double> archiveNeighbourhood;

	private boolean archived;

	private int envHeight = 20;
	private int envWidth = 20;

	public NoveltyBehaviour(ArrayList<RobotObject> currentRobots, ArrayList<ResourceObject> currentResources, ConstructionTask constructionTask) {

		this.numRobots = currentRobots.size();
		this.numResources = currentResources.size();

		robotTrajectories = new ArrayList<LinkedList<Vec2>>(); //num elements = num robots
		resourceTrajectories = new ArrayList<LinkedList<Vec2>>(); //num elements = num resources

		initRobotTrajectories(currentRobots);
		initResourceTrajectories(currentResources);

		//get the number of samples that were saved for each trajectory
		this.numRobotPosSamples = robotTrajectories.get(0).size();
		this.numResPosSamples = resourceTrajectories.get(0).size();

		avgRobotTrajectory = new Vec2[numRobotPosSamples];
		avgResourceTrajectory = new Vec2[numResPosSamples];

		calcAvgTrajectory(numRobots, numRobotPosSamples, avgRobotTrajectory, robotTrajectories);
		calcAvgTrajectory(numResources, numResPosSamples, avgResourceTrajectory, resourceTrajectories);

		this.constructionTask = constructionTask;
		this.connectionOrder = this.constructionTask.getConstructionOrder();

		/**
		check for if this gets copied over properly
		should it be done in a loop for every individual

		change this to only work with 3 construction zones
		*/
		this.constructionZones = this.constructionTask.getConstructionZones();
		this.discreteConstructionZone = this.constructionTask.getDiscreteGrid().getGrid();


		//create the cumulative representation
		populateConnections();
		/**
		change this to work with the variable environment dimensions
		*/

		populationNoveltyScore = 0;
		archiveNoveltyScore = 0;
		simulationNoveltyScore = 0;

		archived = false;

		simulationNeighbourhood = new ArrayList<Double>();
		populationNeighbourhood = new ArrayList<Double>();
		archiveNeighbourhood = new ArrayList<Double>();

	}

	//method to populate the list of robot trajectories
	private void initRobotTrajectories(ArrayList<RobotObject> robots) {

		for(RobotObject r : robots) {

			LinkedList<Vec2> trajectory = r.getTrajectory();
			robotTrajectories.add(trajectory); //each element in the robotTrajectories list will be a LinkedList of Vec2
		}

	}

	//method to populate the list of resource trajectories;
	private void initResourceTrajectories(ArrayList<ResourceObject> resources) {

		for(ResourceObject res : resources) {

			LinkedList<Vec2> trajectory = res.getTrajectory();
			resourceTrajectories.add(trajectory); //each element in the resourceTrajectories list will be a LinkedList of Vec2
		}

	}

	//rewriting this method to sue lists instead of arrays for the connections
	//if using arrays, creating the arrays to a max dimension and then need to keep track
	//how many of the actual positions are filled and not empty elements
	//dont need to check that with lists, just use fancy for loop over elements
	private void populateConnections() {

		/**
		make these lists global variables
		*/
		AConnectionsList = new ArrayList<String[]>();
		BConnectionsList = new ArrayList<String[]>();
		CConnectionsList = new ArrayList<String[]>();

		int numConstructionZones = getNumConstructionZones();

		//for(int k = 0; k < numConstructionZones; k++) { //iterate over all the existing construction zones
		for(ConstructionZone cZone : constructionZones) {

			//for(ResourceObject resObj : constructionZones[k].getConnectedResources()) { //iterate over the resources connected to the current construction zone
			for(ResourceObject resObj : cZone.getConnectedResources()) {

				/**
				make sure that this is moved over by value
				should i create a new array and then add that to the list? or just add the list that gets returned
				*/
				String[] sideConnections = resObj.getAdjacentResources();

				if(resObj.getType().equals("A")) {
					AConnectionsList.add(sideConnections);
				}
				else if(resObj.getType().equals("B")) {
					BConnectionsList.add(sideConnections);
				}
				else if(resObj.getType().equals("C")) {
					CConnectionsList.add(sideConnections);
				}
			}
		}
	}

	//a method to create a combined representation of the structures in the various construction zones
	//this method is done using arrays to represent the connections
	// private void populateConnections() {

	// 	//vars to store the total number of each respective resource
	// 	//summed over all the existing construction zones
	// 	int totalA = 0;
	// 	int totalB = 0;
	// 	int totalC = 0;

	// 	int numConstructionZones = getNumConstructionZones();

	// 	/**
	// 	what if num construction zones == 0
	// 	*/

	// 	for(int k = 0; k < numConstructionZones; k++) {

	// 		int[] typeCount = constructionZones[k].getResourceTypeCount();

	// 		totalA += typeCount[0];
	// 		totalB += typeCount[1];
	// 		totalC += typeCount[2];
	// 	}

	// 	/**
	// 	could possibly make the connection lists actual lists of int[4]
	// 	*/

	// 	//creating the respective lists of connected resources
	// 	AConnections = new String[totalA][4];
	// 	BConnections = new String[totalB][4];
	// 	CConnections = new String[totalC][4];

	// 	//now iterate over the construction zones and populate these arrays with the actual connections between resources
	// 	int APos = 0;
	// 	int BPos = 0;
	// 	int CPos = 0;

	// 	for(int k = 0; k < numConstructionZones; k++) { //iterate over the existing construction zones

	// 		for(ResourceObject resObj : constructionZones[k].getConnectedResources()) { //iterate over the resources connected in the current construction zone

	// 			//getting the connections on all sides of the current res object
	// 			//neeed to write this to the connections array for the current resource
	// 			String[] sides = resObj.getAdjacentResources();

	// 			if(resObj.getType().equals("A")) { //check the type of the current object to know which array to add to

	// 				for(int j = 0; j < sides.length; j++) {

	// 					AConnections[APos][j] = sides[j];
	// 					APos++;
	// 				}
	// 			}
	// 			else if(resObj.getType().equals("B")) {

	// 				for(int j = 0; j < sides.length; j++) {

	// 					BConnections[BPos][j] = sides[j];
	// 					BPos++;
	// 				}
	// 			}
	// 			else if(resObj.getType().equals("C")) {

	// 				for(int j = 0; j < sides.length; j++) {

	// 					CConnections[CPos][j] = sides[j];
	// 					CPos++;
	// 				}
	// 			}
	// 		}
	// 	}

	// }

	/*
	method to calculate an average trajectory that represents the combined overall
	trajectories of all the robots in the team
	so that there is one average trajectory per controller that can be compared to
	the trajectories of other controllers in the generation

	iterate over the trajectories of each robot in a team
	for each trajectory, sum the x and y coordinates at each respective position in the trajectory

	eg. firstPositionAvgTrajectory = firstPositionFirstRobot + firstPositionSecondRobot + firstPositionThirdRobot + firstPositionFourthRobot etc / numRobots
	*/
	private void calcAvgTrajectory(int outerBound, int innerBound, Vec2[] avgTrajectory, ArrayList<LinkedList<Vec2>> originalTrajectories) {

		float[] pathXCoords = new float[innerBound]; //store all the x coords of the positions along a trajectory
		float[] pathYCoords = new float[innerBound]; //store all the y coords of the positions along a trajectory

		//iterate over all the trajectories for this robot team
		for(int k = 0; k < outerBound; k++) { //robotTrajectories.size() should == numRobots
			LinkedList<Vec2> tempTrajectory = originalTrajectories.get(k);

			//iterate over all the positions in the current trajectory
			for(int j = 0; j < innerBound; j++) { //originalTrajectory.size() should == numRobotPosSamples
				Vec2 tempPosition = tempTrajectory.get(j); //get the coords at the current time step

				//summing the x and y values of the positions
				pathXCoords[j] += tempPosition.x;
				pathYCoords[j] += tempPosition.y;
			}
		}

		for(int k = 0; k < innerBound; k++) { //constructing the representative trajectory

			float avgXCoord = pathXCoords[k] / numRobots;
			float avgYCoord = pathYCoords[k] / numRobots;

			avgTrajectory[k] = new Vec2(avgXCoord, avgYCoord); //constructing the average trajectory
		}
	}

	//method to calculate this behaviour's novelty compared to the other results obtained from the simulation
	//find the average distance between its neighbours
	public double calculateSimulationNovelty() {

		simulationNoveltyScore = 0;
		int numNeighbours = 0;

		/**
		change this to work with the k nearest neighbours instead of all individuals
		same goes for the method below
		*/

		for(double d : simulationNeighbourhood) { //iterate over the distances to its neighbours

			simulationNoveltyScore += d;
			numNeighbours++;
		}

		simulationNoveltyScore = simulationNoveltyScore / numNeighbours;
		return simulationNoveltyScore;

	}

	//method to calculate the individuals novelty based on its average distance to k-nearest neighbours
	public double calculatePopulationNovelty() {

		populationNoveltyScore = 0;
		int numNeighbours = 0;

		for(double d : populationNeighbourhood) { //iterate over the nearest neighbours in the population

			populationNoveltyScore += d;
			numNeighbours++;
		}

		populationNoveltyScore = populationNoveltyScore / numNeighbours;

		//System.out.println("NoveltyBehaviour: finished calculating population score = " + populationNoveltyScore);
		return populationNoveltyScore;
	}

	/*
	method to print out the discretised representation of the constructed structures
	*/
	// private void printGrid() {
	//
	// 	System.out.println("NoveltyBehaviour: Printing the grid");
	//
	// 	for(int r = 0; r < envHeight; r++) {
	// 		for(int c = 0; c < envWidth; c++) {
	// 			System.out.print(discreteConstructionZone[r][c] + " ");
	// 		}
	// 		System.out.println("");
	// 	}
	// 	System.out.println("");
	// }

	//check if the current Behaviour has been added to the archive
	public boolean isArchived() {
		return archived;
	}

	public int getNumConstructionZones() {
		//return constructionZones.length;
		return constructionZones.size();
	}

	//method to indicate that the current behaviour has been added to the archive
	public void setArchived() {
		archived = true;
	}

	public void addSimulationNeighbour(double newNeighbour) {
		simulationNeighbourhood.add(newNeighbour);
	}

	public void addPopulationNeighbour(double newNeighbour) {
		//System.out.println("NoveltyBehaviour: adding a new novelty neighbour = " + newNeighbour);
		populationNeighbourhood.add(newNeighbour);
	}

	public void addArchiveNeighbour(double newNeighbour) {
		archiveNeighbourhood.add(newNeighbour);
	}

	public int getNumResources() {
		return this.numResources;
	}

	// public ConstructionZone[] getConstructionZone() {
	// 	return this.constructionZones;
	// }

	public double getPopulationScore() {
		return populationNoveltyScore;
	}

	public ArrayList<ConstructionZone> getConstructionZone() {
		return constructionZones;
	}

	public ConstructionTask getConstructionTask() {
		return this.constructionTask;
	}

	public Vec2[] getResourceTrajectory() {
		return avgResourceTrajectory;
	}

	public Vec2[] getRobotTrajectory() {
		return avgRobotTrajectory;
	}

	public ObjectGrid2D getDiscreteConstructionZone() {
		return discreteConstructionZone;
	}

	// public String[][] getAConnections() {
	// 	return AConnections;
	// }

	// public String[][] getBConnections() {
	// 	return BConnections;
	// }

	// public String[][] getCConnections() {
	// 	return CConnections;
	// }

	public ArrayList<String[]> getAConnections() {
		return AConnectionsList;
	}

	public ArrayList<String[]> getBConnections() {
		return BConnectionsList;
	}

	public ArrayList<String[]> getCConnections() {
		return CConnectionsList;
	}

}

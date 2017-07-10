package za.redbridge.simulator;

import java.util.*;
import za.redbridge.simulator.AggregateBehaviour;
import org.jbox2d.common.Vec2;
import za.redbridge.simulator.ConstructionZone;
import za.redbridge.simulator.object.ResourceObject;
import sim.field.grid.ObjectGrid2D;

/*
a class to monitor and manage all the necessary structures to calculate the novelty fitness
*/

public class NoveltyFitness{

	/**
	Notes from Josh

	for comparing the trajectories to each other, compare each robot in a team to the same robot in the other team then sum the differences in an array
	one array element corresponds to the total trajectory difference for a pair of robots

	sum the values from that array into one double and then divide by the number of robots in a team (length of the array)

	since each controller is run for several runs, find the most novel of all those runs and use that as the representative behaviour for that controller and use that to compare to
	the other individuals in the population
	the most novel individual in the population then gets compared to the archive and if its relatively novel compared to the archive then it gets added to the archive

	add the morphology simplifications that daniel mentioned
	**/

	//order of construction zone connections (order resources are added)
	//compare the construction zone itself
	//resource's trajectory (save position every 5 time steps) regardless of whether or not its been picked up
	//trajectory of the robot  (save position every 5 time steps) regardles of whether it picked up a robot

	private float localNoveltyWeight;
	private float archiveNoveltyWeight;

	private float robotTrajectoryWeight;
	private float resourceTrajectoryWeight;
	private float constructionOrderWeight;
	private float constructionZonesWeight;

	private Archive archive; //the archive of novel behaviours

	private NoveltyBehaviour[] currentGeneration; //keep track of individual controllers in the current generation of individuals
	private int numBehaviours; //keep track of how many behaviours there are in a generation

	//private double[][] robotTrajectoryDifferences; //an array to store the position differences at each sample point
	//private double[][] resourceTrajectoryDifferences; //an array to store the position differences for resources at each sample point their respective trajectories

	private int numRobotSamples; //number of position samples that make up the robots trajectory
	private int numResSamples; //number of position samples that make up the trajectory of a resource
	private int numResources; //the number of resources that were used in a simulation
	private int populationSize; //keep track of how many results are in each generation

	private int envHeight = 20;
	private int envWidth = 20;
	private double maxDist;

	public NoveltyFitness() {
		System.out.println("NoveltyFitness: calling the empty constructor");

	}

	/**
	check that all these calculations are done the same way and correct

	might need to change this constructor a lot depending on how the novelty fitness objects are going to be created

	find a way to create the object and get the weights from somewhere so that this class can be instantiated in a bunch of different places and still produce
	change the constructor to take in a vector of weights
	*/

	//currentGeneration -> collection of behaviours that are going to be used to compute the relative novelty of each behaviour
	public NoveltyFitness(NoveltyBehaviour[] currentGeneration) {

		localNoveltyWeight = 1;
		archiveNoveltyWeight = 1;

		robotTrajectoryWeight = 1;
		resourceTrajectoryWeight = 1;
		constructionOrderWeight = 1;
		constructionZonesWeight = 1;

		populationSize = currentGeneration.length;
		numBehaviours = currentGeneration.length;
		this.currentGeneration = new NoveltyBehaviour[numBehaviours];
		for(int k = 0; k < numBehaviours; k++) { //copying over the elements in the array to a local array
			this.currentGeneration[k] = currentGeneration[k];
		}

		this.numRobotSamples = this.currentGeneration[0].getRobotTrajectory().length; //they should all have the same number of positions in their trajectories
		//robotTrajectoryDifferences = new double[numBehaviours][numRobotSamples];

		this.numResources = this.currentGeneration[0].getNumResources(); //all of the simulations within a generation were run using the same number of resources
		this.numResSamples = this.currentGeneration[0].getResourceTrajectory().length; //the number of times the position was sampled to build the trajectory

		this.maxDist = Math.sqrt( Math.pow(envHeight, 2) + Math.pow(envWidth, 2) );
	}

	//rewriting this method for the same reason the calculatePopulationNovelty method below was rewritten
	/**
	remember to add an int for the k nearest neighbours
	*/
	public NoveltyBehaviour calcSimulationLocalNovelty() {

		for(int k = 0;k < numBehaviours; k++) {
			NoveltyBehaviour currentBehaviour = currentGeneration[k];

			for(int j = 0; j < numBehaviours; j++) {

				if(j != k) {

					NoveltyBehaviour otherBehaviour = currentGeneration[j];
					double noveltyDistance = calculateNoveltyDistance(currentBehaviour, otherBehaviour);
					currentBehaviour.addSimulationNeighbour(noveltyDistance);
				}
			}
		}

		double max = -1;
		int index = -1; //index of the most novel behaviour

		for(int k = 0; k < numBehaviours; k++) {

			double tempValue = currentGeneration[k].calculateSimulationNovelty();

			if(tempValue > max) {

				max = tempValue;
				index = k;
			}
		}

		return currentGeneration[index];
	}

	//rewriting this method to work with the current compareConstructionOrder method
	//cant do dynamic programming since comparing constructionZones requires each behaviour to be analysed separately
	//the novelty of currentConstructionZone compare to otherCZone is not the same as novelty between otherCZone and currentCZone
	public void calculatePopulationNovelty() {

		for(int  k = 0; k < numBehaviours; k++) {
			NoveltyBehaviour currentBehaviour = currentGeneration[k];

			if(currentBehaviour.isArchived()) {
				continue; //dont need to recalculate individuals already in the archive
			}
			else {

				for(int j = 0; j < numBehaviours; j++) {

					if(j != k) { // so as not to compare the same behaviour with itself

						NoveltyBehaviour otherBehaviour = currentGeneration[j];
						double noveltyDistance = calculateNoveltyDistance(currentBehaviour, otherBehaviour);
						currentBehaviour.addPopulationNeighbour(noveltyDistance);
					}
				}
			}
		}

		for(NoveltyBehaviour novBeh : currentGeneration) {
			novBeh.calculatePopulationNovelty();
		}
	}

	// public void calulatePopulationNovelty() {

	// 	for(int k = 0; k < numBehaviours-1; k++) { //iterate over the idividuals in the current generation
	// 		NoveltyBehaviour currentBehaviour = currentGeneration[k];

	// 		for(int j = k+1; j < numBehaviours; j++) { //iterating over the remaining behaviours
	// 			NoveltyBehaviour otherBehaviour = currentGeneration[j];

	// 			if( currentBehaviour.isArchived() && otherBehaviour.isArchived() ) { //check that at least one of the behaviours is not in the archive (dont need to calc distance between 2 archived behaviours)
	// 				continue;
	// 			}
	// 			else {

	// 				double noveltyDistance = calculateNoveltyDistance(currentBehaviour, otherBehaviour);

	// 				//record the nearest neighbours of all the individuals not in the  archive
	// 				if(!currentBehaviour.isArchived()) {
	// 					currentBehaviour.addPopulationNeighbour(otherBehaviour);
	// 				}

	// 				if(!otherBehaviour.isArchived()) {
	// 					otherBehaviour.addPopulationNeighbour(currentBehaviour);
	// 				}
	// 			}
	// 		}
	// 	}
	// }

	private double calculateNoveltyDistance(NoveltyBehaviour currentBehaviour, NoveltyBehaviour otherBehaviour) {

		double dist = 0;

		dist += compareRobotTrajectories(currentBehaviour, otherBehaviour) * robotTrajectoryWeight;
		dist += compareResourceTrajectories(currentBehaviour, otherBehaviour) * resourceTrajectoryWeight;
		dist += compareConstructionOrder(currentBehaviour, otherBehaviour) * constructionOrderWeight;
		dist += compareConstructionZones(currentBehaviour, otherBehaviour) * constructionZonesWeight;

		System.out.println("");

		return dist;
	}

	//method to calculate the average distance between the robot trajectories of these 2 behaviours
	//sum the distance between each behaviour at each time step in the trajectories
	private double compareRobotTrajectories(NoveltyBehaviour currentBehaviour, NoveltyBehaviour otherBehaviour) {

		//System.out.println("NoveltyFitness: comparing robot trajectories");

		Vec2[] currentTrajectory = currentBehaviour.getRobotTrajectory();
		Vec2[] otherTrajectory = otherBehaviour.getRobotTrajectory();

		double totalDistance = 0; //total difference between these trajectories (sum of distance between each point in the trajectory)

		for(int k = 0; k < numRobotSamples; k++) { //iterate over all timesteps

			Vec2 origin = currentTrajectory[k];
			Vec2 destination = otherTrajectory[k];
			totalDistance += calculateDistance(origin, destination);
		}

		totalDistance = totalDistance / numRobotSamples; //avg distance per time step in the trajectory

		System.out.println("NoveltyFItness compareRobotTrajectories = " + totalDistance);

		return totalDistance;
	}

	//method to calculate the average distance between the resource trajectory of these 2 behaviours
	private double compareResourceTrajectories(NoveltyBehaviour currentBehaviour, NoveltyBehaviour otherBehaviour) {
		/**
		there might be a problem when comparing the resource trajectories since they might not all be the same length
		check the conditional statement in ResourceObject on line 420, usually it checks if the resource is constructed or not
		changed this statement to keep updating the trajectory even if its in a constructionzone
		temporary fix
		*/

		//System.out.println("NoveltyFitness: comparing resource trajectories");

		Vec2[] currentTrajectory = currentBehaviour.getResourceTrajectory();
		Vec2[] otherTrajectory = otherBehaviour.getResourceTrajectory();

		double totalDistance = 0;

		for(int k = 0; k < numResSamples; k++) {

			Vec2 origin = currentTrajectory[k];
			Vec2 destination = otherTrajectory[k];
			totalDistance += calculateDistance(origin, destination);
		}

		totalDistance = totalDistance / numResSamples;

		System.out.println("NoveltyFItness compareResourceTrajectories = " + totalDistance);

		return totalDistance;
	}

	/**
	these methods take in an array of behaviours so that the same method can be used to compare
	calculate with the current generation and the archive without rewriting the code
	*/
	private double compareConstructionOrder(NoveltyBehaviour currentBehaviour, NoveltyBehaviour otherBehaviour) {

		double differenceScore = 0;

		//System.out.println("NoveltyFitness: comparing construction order");

		ArrayList<ResourceObject> currentConstructionOrder = currentBehaviour.getConstructionTask().getGlobalConstructionOrder();
		ArrayList<ResourceObject> otherConstructionOrder = otherBehaviour.getConstructionTask().getGlobalConstructionOrder();

		int currentCOLength = currentConstructionOrder.size();
		int otherCOLength = otherConstructionOrder.size();
		int differenceLength = Math.abs(currentCOLength - otherCOLength);
		differenceScore += differenceLength;

		//check which behaviour has the shortest connection order to see which one to iterate over
		if(currentCOLength <= otherCOLength) {

			for (int k = 0; k < currentCOLength; k++) {

				if( !currentConstructionOrder.get(k).getType().equals( otherConstructionOrder.get(k).getType() ) ) {
					differenceScore++;
				}
			}
		}
		else { //iterate over the other construction order

			for(int k = 0; k < otherCOLength; k++) {

				if( !otherConstructionOrder.get(k).getType().equals( otherConstructionOrder.get(k).getType() ) ) {
					differenceScore++;
				}
			}
		}

		System.out.println("NoveltyFitness: compareConstructionOrder = " + differenceScore);

		return differenceScore;
	}

	// public int[][] compareConstructionZones(int[][] currentDiscreteGrid, int[][] otherDiscreteGrid) {
	//
	// 	double totalDifferenceScore = 0;
	// 	double perfectScore = 0;
	//
	// 	int[][] returnGrid = new int[20][20];
	//
	// 	for(int k = 0; k < 20; k++) {
	// 		for(int j = 0; j < 20; j++) {
	// 			returnGrid[k][j] = 0;
	// 		}
	// 	}
	//
	// 	for(int k = 0; k < 20; k++) {
	// 		for(int j = 0; j < 20; j++) {
	//
	// 			if(currentDiscreteGrid[k][j] != otherDiscreteGrid[k][j]) {
	// 				returnGrid[k][j] = 9;
	// 			}
	// 			else {
	// 				returnGrid[k][j] = 0;
	// 			}
	// 		}
	// 	}
	//
	// 	return returnGrid;
	// }

	/**
	writing this method to work with the discrete construction zones
	check which of these methods work better
	*/

	private double compareConstructionZones(NoveltyBehaviour currentBehaviour, NoveltyBehaviour otherBehaviour) {

		//System.out.println("NoveltyFitness: comparing constructionZones");

		ObjectGrid2D currentDiscreteGrid = currentBehaviour.getDiscreteConstructionZone();
		ObjectGrid2D otherDiscreteGrid = otherBehaviour.getDiscreteConstructionZone();

		double totalDifferenceScore = 0;

		for(int k = 0; k < 20; k++) {
			for(int j = 0; j < 20; j++) {

				ResourceObject currentResObj = (ResourceObject) currentDiscreteGrid.get(k,j);
				ResourceObject otherResObj = (ResourceObject) otherDiscreteGrid.get(k,j);

				if ( (currentResObj == null) && (otherResObj == null) ) { //if both locations are empty
					continue;
				}
				else if( (currentResObj == null) || (otherResObj == null) ) { //if one of the locations is empty
					totalDifferenceScore++;
				}
				else if ( !currentResObj.getType().equals(otherResObj.getType()) ) { //check if the resources at the same locations have the same type of block
					totalDifferenceScore++;
				} //neither of the grid locations are empty
			}
		}

		System.out.println("NoveltyFitness: compareConstructionZones = " + totalDifferenceScore);

		return totalDifferenceScore;
	}

	public NoveltyBehaviour[] getGeneration() {
		return this.currentGeneration;
	}

	/**
	check how to do these calculations
	*/

	/*
	method to calculate the difference between the structures that
	were constructed at the end of the simulation
	*/
	// private double compareConstructionZones(NoveltyBehaviour currentBehaviour, NoveltyBehaviour otherBehaviour) {

	// 	ArrayList<String[]> currentAConnections = currentBehaviour.getAConnections();
	// 	ArrayList<String[]> currentBConnections = currentBehaviour.getBConnections();
	// 	ArrayList<String[]> currentCConnections = currentBehaviour.getCConnections();

	// 	ArrayList<String[]> otherAConnections = otherBehaviour.getAConnections();
	// 	ArrayList<String[]> otherBConnections = otherBehaviour.getBConnections();
	// 	ArrayList<String[]> otherCConnections = otherBehaviour.getCConnections();

	// 	//keep track of how many A blocks there are with unique connections
	// 	int totalADiffs = 0;
	// 	totalADiffs += compareConnections(currentAConnections, otherAConnections); //count how many differences in currentConnections

	// 	int totalBDiffs = 0;
	// 	totalBDiffs += compareConnections(currentBConnections, otherBConnections);

	// 	int totalCDiffs = 0;
	// 	totalCDiffs += compareConnections(currentCConnections, otherCConnections);

	// 	int totalReturn = totalADiffs + totalBDiffs + totalCDiffs;

	// 	return totalReturn;
	// }

	//method to count how many blocks in currentConnections have a unique set of connections
	//compared to the otherConnections
	// private int compareConnections(ArrayList<String[]> currentConnections, ArrayList<String[]> otherConnections) {

	// 	int diffCounter = 0; //count how many resources have a unique collection of connections

	// 	for(String[] current : currentConnections) { //iterate over all the resource connection configurations
	// 		boolean found = false; //check if a resource with identical connections has been found

	// 		for(String[] other : otherConnections) {

	// 			int sideCounter = 0; //count how many of the connections are the same between the 2 resources

	// 			for(int k = 0; k < 4; k++) {

	// 				if( current[k].equals(other[k]) ) { //check if they have the same type of block connected to the same side

	// 					sideCounter++;
	// 				}
	// 			}

	// 			if(sideCounter == 4) { //if they share all the same connections
	// 				found = true;
	// 				break;
	// 			}
	// 		}

	// 		if(!found) { //counting how many resources have a unique set of connections
	// 			diffCounter++;
	// 		}
	// 	}

	// 	return diffCounter;
	// }

	/**
	the float being returned gets cast to a double in the compareRobotTrajectories method
	check that this does not cause some sort of error
	*/

	/*
	method to calculate the distance between 2 points
	*/
	private float calculateDistance(Vec2 origin, Vec2 destination) {

		double originX = origin.x;
		double originY = origin.y;

		double destinationX = destination.x;
		double destinationY = destination.y;

		// float distance = (float) (Math.pow(destinationX - originX, 2) +
		// 				 								  Math.pow(destinationY - originY, 2));

		float distance = (float) Math.sqrt(
										Math.pow(destinationX - originX, 2) +
										Math.pow(destinationY - originY, 2));

		return distance;

	}

}

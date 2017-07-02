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

	// private float localNoveltyWeight;
	// private float archiveNoveltyWeight;

	private float constructionOrderWeight;
	private float constructionZonesWeight;
	private float endpointPositionWeight;

	private Archive archive; //the archive of novel behaviours

	private NoveltyBehaviour[] currentGeneration; //keep track of individual controllers in the current generation of individuals
	private int numBehaviours; //keep track of how many behaviours there are in a generation
	private int numResources; //the number of resources that were used in a simulation
	private int populationSize; //keep track of how many results are in each generation

	private int envHeight = 20;
	private int envWidth = 20;
	private double maxDist;

	public NoveltyFitness() {
		//System.out.println("NoveltyFitness: calling the empty constructor");
	}

	/**
	check that all these calculations are done the same way and correct

	might need to change this constructor a lot depending on how the novelty fitness objects are going to be created

	find a way to create the object and get the weights from somewhere so that this class can be instantiated in a bunch of different places and still produce
	change the constructor to take in a vector of weights
	*/

	//currentGeneration -> collection of behaviours that are going to be used to compute the relative novelty of each behaviour
	public NoveltyFitness(NoveltyBehaviour[] currentGeneration) {

		constructionOrderWeight = 1;
		constructionZonesWeight = 1;
		endpointPositionWeight = 1;

		populationSize = currentGeneration.length;
		numBehaviours = currentGeneration.length;
		this.currentGeneration = new NoveltyBehaviour[numBehaviours];
		for(int k = 0; k < numBehaviours; k++) { //copying over the elements in the array to a local array
			this.currentGeneration[k] = currentGeneration[k];
		}
	}

	/*
	method to calculate the local novelty of individuals within the current population (current generation)
	relative to each other and return the behaviour with the greatest novelty score */
	public NoveltyBehaviour calcSimulationLocalNovelty() {

		for(int k = 0;k < numBehaviours; k++) {
			NoveltyBehaviour currentBehaviour = currentGeneration[k];

			if(currentBehaviour == null) { //check if something went terribly wrong in the score calculator
				//System.out.println("NoveltyFitness: found a NULL element!!!!!!!");
				continue;
			}

			for(int j = 0; j < numBehaviours; j++) {

				if(j != k) {

					NoveltyBehaviour otherBehaviour = currentGeneration[j];

					if(otherBehaviour == null) {
						//System.out.println("NoveltyFitness: found a NULL element!!!!!!!");
						continue;
					}

					double noveltyDistance = calculateNoveltyDistance(currentBehaviour, otherBehaviour);
					currentBehaviour.addSimulationNeighbour(noveltyDistance);
				}
			}
		}

		double max = -1;
		int index = -1; //index of the most novel behaviour

		for(int k = 0; k < numBehaviours; k++) {

			if(currentGeneration[k] == null) {
				//System.out.println("NoveltyFitness: found a NULL element!!!!!!!");
				continue;
			}

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

		//System.out.println("NoveltyFitness (calculatePopulationNovelty line 119): starting the novelty calculation for the current population");

		for(int  k = 0; k < numBehaviours; k++) {

			NoveltyBehaviour currentBehaviour = currentGeneration[k];

			if(currentBehaviour == null) {
				//System.out.println("NoveltyFitness: found a NULL element!!!!!!!");
				continue;
			}

			if(currentBehaviour.isArchived()) {
				continue; //dont need to recalculate individuals already in the archive
			}
			else {

				for(int j = 0; j < numBehaviours; j++) {

					if(j != k) { // so as not to compare the same behaviour with itself

						NoveltyBehaviour otherBehaviour = currentGeneration[j];

						if(otherBehaviour == null) {
							//System.out.println("NoveltyFitness: found a NULL element!!!!!!!");
							continue;
						}

						double noveltyDistance = calculateNoveltyDistance(currentBehaviour, otherBehaviour);
						currentBehaviour.addPopulationNeighbour(noveltyDistance); //add neighbour to appropriate list for behaviour
					}
				}
			}
		}

		//System.out.println("NoveltyFitness (line 153): finished comparing each behaviour to every other behaviour in the current population");

		for(NoveltyBehaviour novBeh : currentGeneration) { //calculates the mean novelty distance between a behaviour and k-nearest neighbours

			if(novBeh != null) {
				novBeh.calculatePopulationNovelty();
			}
			else {
				//System.out.println("NoveltyFitness: found a NULL element!!!!!!!");
			}
		}
	}

	private double calculateNoveltyDistance(NoveltyBehaviour currentBehaviour, NoveltyBehaviour otherBehaviour) {

		double dist = 0;

		//dist += compareRobotTrajectories(currentBehaviour, otherBehaviour) * robotTrajectoryWeight;
		//dist += compareResourceTrajectories(currentBehaviour, otherBehaviour) * resourceTrajectoryWeight;
		dist += compareRobotEndPositions(currentBehaviour, otherBehaviour) * endpointPositionWeight;
		dist += compareConstructionOrder(currentBehaviour, otherBehaviour) * constructionOrderWeight;
		dist += compareConstructionZones(currentBehaviour, otherBehaviour) * constructionZonesWeight;

		return dist;
	}

	private double compareRobotEndPositions(NoveltyBehaviour currentBehaviour, NoveltyBehaviour otherBehaviour) {

		double endPositionScore = 0;

		ArrayList<Vec2> currentEndPositions = currentBehaviour.getRobotPositions();
		ArrayList<Vec2> otherEndPositions = otherBehaviour.getRobotPositions();

		int numRobots = currentBehaviour.getNumRobots();

		for(int k = 0; k < numRobots; k++) {

			Vec2 currentPosition = currentEndPositions.get(k);
			Vec2 otherPosition = otherEndPositions.get(k);

			endPositionScore += calculateDistance(currentPosition, otherPosition);
		}

		endPositionScore = endPositionScore / numRobots;

		return endPositionScore;
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

		return differenceScore;
	}

	private double compareConstructionZones(NoveltyBehaviour currentBehaviour, NoveltyBehaviour otherBehaviour) {

		//System.out.println("NoveltyFitness: comparing constructionZones");

		ObjectGrid2D currentDiscreteGrid = currentBehaviour.getDiscreteConstructionZone();
		ObjectGrid2D otherDiscreteGrid = otherBehaviour.getDiscreteConstructionZone();

		double totalDifferenceScore = 0;

		//iterating over the positions in the dicretized grid
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

		return totalDifferenceScore;
	}

	public NoveltyBehaviour[] getGeneration() {
		return this.currentGeneration;
	}

	/*
	method to calculate the distance between 2 points
	*/
	private float calculateDistance(Vec2 origin, Vec2 destination) {

		double originX = origin.x;
		double originY = origin.y;

		double destinationX = destination.x;
		double destinationY = destination.y;

		float distance = (float) (Math.pow(destinationX - originX, 2) +
						 								  Math.pow(destinationY - originY, 2));

		return distance;

	}

}

package za.redbridge.simulator.novelty;

import org.jbox2d.common.Vec2;

import za.redbridge.simulator.ConstructionZone;
import za.redbridge.simulator.ContToDiscrSpace;
import za.redbridge.simulator.object.ResourceObject;
import za.redbridge.simulator.object.RobotObject;

import sim.field.grid.ObjectGrid2D;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Queue;
import java.util.PriorityQueue;
import java.util.Iterator;
import java.util.LinkedList;

/**
Behaviour:
	ConstrucitonZone
	Array of resources and order in which they were connected
	Arrays for resource/robot paths
	Array of block pickup type per robot
	
**/

public class PhenotypeBehaviour {

	private final ConstructionZone[] constructionZones;

	private final ContToDiscrSpace discr;

	private final String [][] AConnections;
	private final String [][] BConnections;
	private final String [][] CConnections;

	private final ArrayList<Vec2[]> resourcePosSample;
	private final ArrayList<Vec2[]> robotPosSample;

	private final ArrayList<ResourceObject> resources;
	private final ArrayList<RobotObject> robots;

	private final ResourceObject[] constructionOrder;

	private final int k;  //limiting parameter: the k nearest individuals

	private int nearestCount = 0;

	private LinkedList<Double> nearestNeighbours;  //stores the distance between this and the nearest k neighbours

	public PhenotypeBehaviour (ConstructionZone[] constructionZones, ContToDiscrSpace discr, ResourceObject[] overallConstructionOrder, ArrayList<ResourceObject> resources, ArrayList<RobotObject> robots, int k) {
		this.constructionZones = constructionZones;

		this.discr = discr;

		int [] czTypeCount = ConstructionZone.getOverallTypeCount(constructionZones);

		// System.out.println(Arrays.toString(czTypeCount));
		
		AConnections = new String [czTypeCount[0]][4];
		BConnections = new String [czTypeCount[1]][4];
		CConnections = new String [czTypeCount[2]][4];
		resourcePosSample = new ArrayList<>();
		robotPosSample = new ArrayList<>();
		// constructionOrder = ConstructionZone.getOverallConstructionOrder(constructionZones);
		constructionOrder = overallConstructionOrder;
		this.resources = resources;
		this.robots = robots;
		populateResourcePushedSampling(resources);
		populateRobotsPositions(robots);
		// if ((czTypeCount[0] + czTypeCount[1] + czTypeCount[2]) == 0) {
			
		// }
		populateConnections();
		this.k = k;
		nearestNeighbours = new LinkedList<>();

	}

	/**
	Method that populates the array of Vec2 positions for each resource recorded every 5 timesteps when they were being pushed
	**/
	public void populateResourcePushedSampling(ArrayList<ResourceObject> resources) {
		for (ResourceObject r : resources) {
			Vec2[] rPushSampling = r.getPushSampling().toArray(new Vec2[0]);
			resourcePosSample.add(rPushSampling);
		}
	}

	/**
	Method that populates the array of Vec2 positions for each robot recorded every 5 timesteps when they were being pushed
	**/
	public void populateRobotsPositions(ArrayList<RobotObject> robots) {
		for (RobotObject r : robots) {
			Vec2[] rSampling = r.getPosSampling().toArray(new Vec2[0]);
			robotPosSample.add(rSampling);
		}
	}

	public void populateConnections() {
		int APos = 0;
		int BPos = 0;
		int CPos = 0;

		// System.out.println("Populating connections");

		if (constructionZones[0].getConnectedResources().size() == 0) {
			for (int i = 0; i < AConnections.length; i++) {
				for (int j = 0; j < AConnections[0].length; j++) {
					AConnections[i][j] = "";
				}
			}

			for (int i = 0; i < BConnections.length; i++) {
				for (int j = 0; j < BConnections[0].length; j++) {
					BConnections[i][j] = "";
				}
			}

			for (int i = 0; i < CConnections.length; i++) {
				for (int j = 0; j < CConnections[0].length; j++) {
					CConnections[i][j] = "";
				}
			}
		}

		else {
			// System.out.println("\tAConnections length: " + AConnections.length);
			// System.out.println("\tcz Size: " + constructionZones[0].getConnectedResources().size());
			// for (ResourceObject r : constructionZones[0].getConnectedResources()) {
			// 	// System.out.println("\t" + r + " " + r.getType());
			// }
			for (int czNum = 0; czNum < constructionZones.length; czNum++) {
				// System.out.println("\t#connected = " + constructionZones[czNum].getConnectedResources());
				for (ResourceObject r : constructionZones[czNum].getConnectedResources()) {
					if (r.getType().equals("A")) {
						// System.out.println("\t\tAPOS: " + APos);
						String [] sides = r.getAdjacentList();
						for (int i = 0; i < sides.length; i++) {
							// System.out.println(AConnections[APos][i]);
							AConnections[APos][i] = sides[i];
						}
						// System.out.println(APos);
						APos++;
					}
					else if (r.getType().equals("B")) {
						String [] sides = r.getAdjacentList();
						for (int i = 0; i < sides.length; i++) {
							BConnections[BPos][i] = sides[i];
						}
						BPos++;
					}
					else if (r.getType().equals("C")) {
						String [] sides = r.getAdjacentList();
						for (int i = 0; i < sides.length; i++) {
							CConnections[CPos][i] = sides[i];
						}
						CPos++;
					}
				}	
			}
		}
		// System.out.println(Arrays.deepToString(AConnections));
		// System.out.println(Arrays.deepToString(BConnections));
		// System.out.println(Arrays.deepToString(CConnections));
	}

	public String [][] getAConnections() {
		return AConnections;
	}

	public String [][] getBConnections() {
		return BConnections;
	}

	public String [][] getCConnections() {
		return CConnections;
	}

	// public ConstructionZone getConstructionZone() {
	// 	return constructionZones[0];
	// }
	public ConstructionZone[] getConstructionZones() {
		return constructionZones;
	}

	public ResourceObject[] getConstructionOrder() {
		return constructionOrder;
	}

	public ArrayList<Vec2[]> getResourcePosSample() {
		return resourcePosSample;
	}

	public ArrayList<Vec2[]> getRobotPosSample() {
		return robotPosSample;
	}

	public ArrayList<ResourceObject> getResources() {
		return resources;
	}

	public ArrayList<RobotObject> getRobots() {
		return robots;
	}

	public int getK () {
		return k;
	}

	public ContToDiscrSpace getDiscrSpace() {
		return discr;
	}

	public ObjectGrid2D getDiscreteConstructionZone() {
		return discr.getGrid();
	}

	public void nearestNeighbourhoodValues () {
		for (Double d : nearestNeighbours) {
			System.out.println(d);
		}
	}

	/**
	check to see if the individual is one of the k nearest neighbours to this. If so, add the distance to the nearestNeighbours array
	@param dist the behavioural distance between this and some other individual
	**/
	public void checkNearestNeighbourhood(double dist) {
		//If we have not found all k nearest neighbours yet
		if (nearestNeighbours.size() < k) {
			nearestNeighbours.add(dist);
			nearestCount++;
		}

		else {
			nearestNeighbours.add(dist);
			Double [] currList = nearestNeighbours.toArray(new Double[0]);
			Arrays.sort(currList);
			nearestNeighbours.clear();
			// System.out.println("BEFORE CLEAREING:");
			// nearestNeighbours.add(new Double(dist));
			// nearestNeighbourhoodValues();
			// Double[] currElements = nearestNeighbours.toArray(new Double[0]);
			// nearestNeighbours.clear();
			// for (int i = 0; i < k; i++) {
			// 	nearestNeighbours.add(currElements[i]);
			// }
			// System.out.println("AFTER");
			for (int i = 0; i < k; i++) {
				nearestNeighbours.add(currList[i]);
			}
		}
	} 

	public void clearNearestNeighbours() {
		nearestNeighbours.clear();
		nearestCount = 0;
	}

	/**
	
	**/
	public double getBehaviouralSparseness() {
		double sparseness = 0D;
		int numNeighbours = nearestCount == k ? k : nearestCount;
		for (int i = 0; i < numNeighbours; i++) {
			sparseness += nearestNeighbours.get(i);
		}
		return sparseness/k;
	}

	/**
	Method that calculates the novelty distance between this genotype's behaviour and some other genotype (either from current population or archive)
	**/
	public void noveltyDistance (PhenotypeBehaviour u) {
		// System.out.println("Calculating novelty distance between two PBs");
		double dist = 0D;
		dist += compareResourcePosSample(u.getResourcePosSample());
		dist += compareRobotPosSample(u.getRobotPosSample());
		dist += (double)compareConstructionZones(u.getDiscreteConstructionZone());
		dist += compareConstructionOrder(u.getConstructionOrder());
		checkNearestNeighbourhood(dist);  
	}

	/**
	Compares the difference in movement of the resources between two individuals
	@param otherResourcePosSample list of position samples for each resource from another individual's simulation run
	**/
	public double compareResourcePosSample (ArrayList<Vec2[]> otherResourcePosSample) {
		// for (Map.Entry<ResourceObject, Vec2[]> entry : resourcePosSample.entrySet()) {
		//     ResourceObject res = entry.getKey();
		//     Vec2[] resPosSample = entry.getValue();
		//     if (otherResourcePosSample.containsKey(res)) {
		//     	System.out.println("YAY");
		//     }
		// }
		double [] totalDistance = new double[resourcePosSample.size()];
		//loop through the resources' position samples
		for (int i = 0; i < resourcePosSample.size(); i++) {
			Vec2 [] thisResSample = resourcePosSample.get(i);
			Vec2 [] otherResSample = otherResourcePosSample.get(i);
			double distance = 0D;
			// int shortestLength = thisResSample.length < otherResSample.length ? thisResSample.length : otherResSample.length;
			//loop through the samples and calculate the sum of squared Euclidian distance between resource's sample
			for (int j = 0; j < thisResSample.length; j++) {
				float dist = (float)Math.pow(thisResSample[j].sub(otherResSample[j]).length(), 2);
				distance += (double) dist;
			}
			
			totalDistance[i] = distance;
		}

		double overallDistance = 0D;
		//calculate the average difference over all resources
		for (int i = 0; i < totalDistance.length; i++) {
			overallDistance += totalDistance[i];
		}

		return overallDistance/totalDistance.length;
	}

	/**
	Compares the difference in movement of the robots between two individuals
	@param otherRobotPosSample list of position samples for each robot from another individual's simulation run
	**/
	public double compareRobotPosSample (ArrayList<Vec2[]> otherRobotPosSample) {
		// for (Map.Entry<RobotObject, Vec2[]> entry : RobotPosSample.entrySet()) {
		//     RobotObject res = entry.getKey();
		//     Vec2[] resPosSample = entry.getValue();
		//     if (otherRobotPosSample.containsKey(res)) {
		//     	System.out.println("YAY");
		//     }
		// }
		double [] totalDistance = new double[robotPosSample.size()];
		//loop through the Robots' position samples
		for (int i = 0; i < robotPosSample.size(); i++) {
			Vec2 [] thisRobotSample = robotPosSample.get(i);
			Vec2 [] otherRobotSample = otherRobotPosSample.get(i);
			double distance = 0D;
			//loop through the samples and calculate the sum of squared Euclidian distance between robot's sample
			for (int j = 0; j < thisRobotSample.length; j++) {
				float dist = (float)Math.pow(thisRobotSample[j].sub(otherRobotSample[j]).length(), 2);
				distance += (double) dist;
			}
			
			totalDistance[i] = distance;
		}

		double overallDistance = 0D;
		//calculate the average difference over all robots
		for (int i = 0; i < totalDistance.length; i++) {
			overallDistance += totalDistance[i];
		}

		return overallDistance/totalDistance.length;
	}

	public int compareStructure (String [][] otherAConnections, String [][] otherBConnections, String [][] otherCConnections) {
		int diffCounter = 0;
		boolean isIdentical = false;
		//For each A resource in the other CZ:
		for (int i = 0; i < otherAConnections.length; i++) {
			//Compare with each A resource in this CZ for the same adjacent list (i.e no difference)
			for (int j = 0; j < AConnections.length; j++) {
				int sideCount = 0;
				for (int side = 0; side < AConnections[0].length; side++) {
					if (!otherAConnections[i][side].equals(AConnections[j][side])) {
						sideCount++;
					}
				}
				if (sideCount == 0) {
					isIdentical = true;
					break;
				}
			}
			if (!isIdentical) {
				diffCounter++;
			}
			isIdentical = false;
		}

		return diffCounter;
	}

	private double compareConstructionZones(ObjectGrid2D otherObjectGrid) {


		//System.out.println("NoveltyFitness: comparing constructionZones");


		ObjectGrid2D currentDiscreteGrid = getDiscreteConstructionZone();
		ObjectGrid2D otherDiscreteGrid = otherObjectGrid;


		double totalDifferenceScore = 0;


		for(int k = 0; k < currentDiscreteGrid.field.length; k++) {
			for(int j = 0; j < currentDiscreteGrid.field[0].length; j++) {


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

	public int compareConstructionOrder (ResourceObject[] otherConstructionOrder) {
		int difference = 0;
		int smallestSize;
		if (constructionOrder.length != otherConstructionOrder.length) {
			difference = Math.abs(constructionOrder.length - otherConstructionOrder.length);
			smallestSize = constructionOrder.length < otherConstructionOrder.length ? constructionOrder.length : otherConstructionOrder.length;

			for (int i = 0; i < smallestSize; i++) {
				if (constructionOrder[i] != null && otherConstructionOrder != null) {
					//If the ith added resource is not of the same type
					if (!constructionOrder[i].getType().equals(otherConstructionOrder[i].getType())) {
						difference++;	
					}
				}
			}
		}
		else {
			for (int i = 0; i < constructionOrder.length; i++) {
				if (constructionOrder[i] != null && otherConstructionOrder[i] != null) {
					//If the ith added resource is not of the same type
					if (!constructionOrder[i].getType().equals(otherConstructionOrder[i].getType())) {
						difference++;	
					}
				}
			}
		}
		
		return difference;
	}

	/**
	Calculates and returns the most novel behaviour for an individual (called by ScoreCalculator)
	@param indivBehaviours the behaviours the individual exhibited during its simulation tests/trials
	**/
	public static PhenotypeBehaviour getMostNovelForIndiv(PhenotypeBehaviour[] indivBehaviours) {
		double mostSparse = -1D;
		int mostSparsePos = 0;
		double[] novelties = new double[indivBehaviours.length];
		for (int i = 0; i < indivBehaviours.length; i++) {
			//Compare each behaviour with each other and calculate their novelty distances
			for (int j = 0; j < indivBehaviours.length; j++) {
				if (i != j) {
					indivBehaviours[i].noveltyDistance(indivBehaviours[j]);
				}
			}
			novelties[i] = indivBehaviours[i].getBehaviouralSparseness();
			//Find the most novel behaviour
			if (mostSparse < 0) {
				mostSparse = indivBehaviours[i].getBehaviouralSparseness();
				mostSparsePos = i;
			}
			else {
				double s = indivBehaviours[i].getBehaviouralSparseness();
				if (s > mostSparse) {
					mostSparse = s;
					mostSparsePos = i;
				}
			}
		}

		// System.out.println("\t" + Arrays.toString(novelties));
		// System.out.println("\t\tMost novel = " + novelties[mostSparsePos]);

		return indivBehaviours[mostSparsePos];
	}
}










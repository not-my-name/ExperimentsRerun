package za.redbridge.simulator;

import za.redbridge.simulator.ConstructionZone;
import za.redbridge.simulator.ConstructionTask;
import za.redbridge.simulator.object.ResourceObject;
import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.*;
import za.redbridge.simulator.object.RobotObject;
import org.jbox2d.common.Vec2;

/*
use this class to manage and monitor all the different behaviours that are needed to find the novelty
score of an individual
*/

public class Behaviour {

	private ConstructionTask constructionTask;
	//private ConstructionZone[] constructionZones;
	private ArrayList<ConstructionZone> constructionZones;
	//private ConstructionZone mostValuableCZ; //variable to hold the most valuable construction zone that the robot team created during the simulation

	private int numConstructionZones;
	//variables to count how many different types of each block there are in the construction zone
	private int connectedA;
	private int connectedB;
	private int connectedC;

	private int totalConnected;

	private int schemaConfigNum;

	//variables to hold the respective scores for evaluating the constructoin zones of the behaviour
	// private double adjacentScore;
	// private double correctSchemaScore;

	public Behaviour(ConstructionTask constructionTask, int schemaConfigNum) {

		this.constructionTask = constructionTask;
		this.constructionZones = this.constructionTask.getConstructionZones();
		numConstructionZones = this.constructionZones.size();
		this.schemaConfigNum = schemaConfigNum;

		connectedA = 0;
		connectedB = 0;
		connectedC = 0;
		totalConnected = 0;

		countConnected();
	}

	/*
	method to count how many of each of the different types of blocks were
	connected across all the construction zones */
	private void countConnected() {

		for (ResourceObject resObj : constructionTask.getUniqueResources()) {
			if(resObj.getType().equals("A")) {
				connectedA += 1;
			}
			else if(resObj.getType().equals("B")) {
				connectedB += 1;
			}
			else if(resObj.getType().equals("C")) {
				connectedC += 1;
			}
		}

		totalConnected = connectedA + connectedB + connectedC;

		//for(ConstructionZone cZone : constructionZones) {

			//connectedA += cZone.getACount();
			//connectedB += cZone.getBCount();
			//connectedC += cZone.getCCount();
		//}
	}

	public ArrayList<ConstructionZone> getConstructionZones() {
		return constructionZones;
	}

	public ConstructionTask getConstructionTask() {
		return this.constructionTask;
	}

	public int getNumConstructionZones() {
		return this.numConstructionZones;
	}

	public int getConnectedA() {
		return this.connectedA;
	}

	public int getConnectedB() {
		return this.connectedB;
	}

	public int getConnectedC() {
		return this.connectedC;
	}

	public int getTotalConnected() {
		return totalConnected;
	}
}

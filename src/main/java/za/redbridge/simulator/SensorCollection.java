
package za.redbridge.simulator;

import java.util.Random;
import za.redbridge.simulator.sensor.AgentSensor;
import za.redbridge.simulator.khepera.*;
import java.util.*;
import za.redbridge.simulator.Morphology;
import za.redbridge.simulator.config.MorphologyConfig;
import java.text.ParseException;

public class SensorCollection {

	//REMEMBER TO CHANGE THESE TO THE REAL SENSOR NAMES
	private static boolean PROXIMITY_SENSOR = false;
	private static boolean BOTTOM_PROXIMITY_SENSOR = false;
	private static boolean ULTRASONIC_SENSOR = false;
	private static boolean COLOUR_PROXIMITY_SENSOR = false;
	private static boolean COLOUR_RANGED_SENSOR = false;
	private static boolean LOW_RES_CAMERA_SENSOR = false;
	private static boolean CONSTRUCTION_SENSOR = false;

	//all the different types of sensors that can be connected to the robot
	private String[] possibleSensors = new String[]{"ProximitySensor", "BottomProximitySensor",
													"UltrasonicSensor", "ColourProximitySensor",
													"ColourRangedSensor", "LowResCameraSensor",
													"ConstructionSensor", "EMPTY"};

	//an array list to store the current morphology that is going to be attached to the robots
	private ArrayList<AgentSensor> finalSensorList = new ArrayList<AgentSensor>();

	private Morphology actualMorphology;
	private Morphology idealMorphology;
	private MorphologyConfig morphConfig;
	private int numSensors;

	//a constructor for the class to  set up the default morphology
	////takes in the file path to the morphology config yaml file
	public SensorCollection(String filePath) throws ParseException {

		morphConfig = new MorphologyConfig(filePath);
		idealMorphology = morphConfig.getMorphology(1); //the ideal morphology that has all the sensors connected

		numSensors = idealMorphology.getNumSensors();

	}

	public Morphology getMorph(int i) {
		return morphConfig.getMorphology(i);
	}

	public Morphology getIdealMorph() {
		return idealMorphology;
	}

	//this is just so that the demo method will work with all the sensors in the ideal morphology
	//without needing to change/add a bunch of methods to the other classes like phenotype
	public int[] getIdealBitMask() {

		int[] idealMask = new int[11];
		for(int k = 0; k < 11; k++) {
			idealMask[k] = 0;
		}

		return idealMask;
	}

	//a method to create a random bit array of 1s and 0s in order to indicate which of the sensors
	//have been disabled and which ones are still active
	public int[] generateRandomMask() {

		Random rand = new Random();

		int[] newRandomMask = new int[numSensors];
		int numToDisable = rand.nextInt(11);

		for(int k = 0; k < numSensors; k++) {

			int disabilityProbability = rand.nextInt(11);

			//check if the randomly generated probability is within the necessary bounds
			if (disabilityProbability <= numToDisable) {
				newRandomMask[k] = 0;
			}
			else {
				newRandomMask[k] = 1;
			}
		}

		return newRandomMask;
	}

	public int getNumSensors() {
		return numSensors;
	}

}

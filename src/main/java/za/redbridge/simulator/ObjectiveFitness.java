package za.redbridge.simulator;

import za.redbridge.simulator.object.ResourceObject;
import java.util.*;
import za.redbridge.simulator.config.SchemaConfig;

/*
a class that will manage all the necessary calculations for the objective fitness of an individual
*/

public class ObjectiveFitness {

	//the weights that will reward the robots for connecting more complex resources
	private float connectionAWeight;
	private float connectionBWeight;
	private float connectionCWeight;

	private SchemaConfig schemaConfig;

	private int configIndex; //the schema config being used in this experiment
	private int[] resTypeCount;

	public ObjectiveFitness(int configIndex, int[] resTypeCount) {

		this.schemaConfig = schemaConfig;
		this.configIndex = configIndex;
		this.resTypeCount = resTypeCount;

		connectionAWeight = 0.3f;
		connectionBWeight = 0.6f;
		connectionCWeight = 1f;
	}

	public double calculate(Behaviour behaviour) {

		double finalFitness = 0;

		// System.out.println("ObjectiveFitness: printing the score criteria");
		// System.out.println("Num A connected = " + behaviour.getConnectedA());
		// System.out.println("Num B connected = " + behaviour.getConnectedB());
		// System.out.println("Num C connected = " + behaviour.getConnectedC());

		double scoreA = behaviour.getConnectedA() * connectionAWeight;
		double scoreB = behaviour.getConnectedB() * connectionBWeight;
		double scoreC = behaviour.getConnectedC() * connectionCWeight;

		// System.out.println("Score for A connections = " + scoreA);
		// System.out.println("Score for B connections = " + scoreB);
		// System.out.println("Score for C connections = " + scoreC);

		//the different type of resource counts for the entire environment
		int totalA = resTypeCount[0];
		int totalB = resTypeCount[1];
		int totalC = resTypeCount[2];

		// System.out.println("The total different types of blocks in the environment");
		// System.out.println("Total A blocks = " + totalA);
		// System.out.println("Total B blocks = " + totalB);
		// System.out.println("Total C blocks = " + totalC);
		// System.out.println("");

		double idealA = totalA * connectionAWeight;
		double idealB = totalB * connectionBWeight;
		double idealC = totalC * connectionCWeight;

		// System.out.println("The maximum possible score for each type of block");
		// System.out.println("Ideal A score = " + idealA);
		// System.out.println("Ideal B score = " + idealB);
		// System.out.println("Ideal C score = " + idealC);

		double normalizationVal = idealA + idealB + idealC;

		//System.out.println("Total ideal score = " + normalizationVal);

		finalFitness = (scoreA + scoreB + scoreC) / normalizationVal;

		// System.out.println("Final calculated and normalized fitness = " + finalFitness);
		// System.out.println("");
		// System.out.println("");

		return finalFitness;
	}
}

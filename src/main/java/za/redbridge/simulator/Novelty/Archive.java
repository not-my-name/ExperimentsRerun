/*
Author: Ruben Putter
Date: 22 September 2016

This is a class to maintain a history of the novel behaviours
that get produced by different controllers

Given a behaviour, compare to history of behaviours
*/

package za.redbridge.simulator;

import org.encog.ml.MLMethod;
import java.util.*;
import za.redbridge.simulator.NoveltyBehaviour;

/**
check if there will be concurrency issues with accessing the archive
*/

public class Archive {

	private ArrayList<NoveltyBehaviour> noveltyArchive;
	private ArrayList<NoveltyBehaviour> currentGeneration; //arraylist to store the individual behaviours that are part of the same generation
	private int numNearest; //number of nearest neighbours to use when calculating novelty


	public Archive() {

		noveltyArchive = new ArrayList<NoveltyBehaviour>();
		currentGeneration = new ArrayList<NoveltyBehaviour>();

		numNearest = 10;

	}

	/*
	method that takes in an array of behaviours that have already been created in the simulation and
	uses the novelty fitness class to calculate the relative novelty of the behaviours in the given collection

	this method is called from the getNoveltyBehaviour() method in scoreCalculator
	*/
	public NoveltyBehaviour findMostNovel(NoveltyBehaviour[] behaviourCollection) {


		/**
		when calculating an individuals final novelty score, use a population that consists of the individuals from the current generation
		combined with the individuals from the archive
		use this combined population to calculate the k nearest neighbours score for each individual in the current generation
		*/

		NoveltyFitness noveltyFitness = new NoveltyFitness(behaviourCollection);
		/**
		check that the novelty behaviour that gets sent back here is the same one that was selected in the NoveltyFitness class
		do this by checking that its nearest neighbours arrays are populated
		*/
		NoveltyBehaviour mostNovel = noveltyFitness.calcSimulationLocalNovelty();
		currentGeneration.add(mostNovel); //adding the most novel result from the simulation runs to the current generation

		return mostNovel;

	}

	public void calculatePopulationNovelty() {

		/**
		when creating the noveltyFItness object for this, send through the combined population of the current generation and the archive
		*/

		//var to calc the size of the combined population
		int numNovelty = noveltyArchive.size();
		int numGeneration = currentGeneration.size();
		int totalSize = numNovelty + numGeneration;
		NoveltyBehaviour[] currentPopulation = new NoveltyBehaviour[totalSize]; //array to store the cumulative population

		//iterating over the novelty archive in order to
		//populate the global collection
		for(int k = 0; k < numNovelty; k++) {

			//should there be a copy constructor
			//first create the empty object and then assign?
			currentPopulation[k] = noveltyArchive.get(k);
		}

		//repeat for the current generation
		for(int k = 0; k < numGeneration; k++) {

			currentPopulation[numNovelty+k] = currentGeneration.get(k);
		}

		NoveltyFitness noveltyFitness = new NoveltyFitness(currentPopulation);
		/**
		check that the values for the behaviours in currentPopulation have been changed accordingly after the calculate novelty method has
		been called
		check that they are modified by reference

		add the most novel behaviour to the archive
		calculate most novel in the NoveltyFitness method and return either an object or index to the current population
		*/

		System.out.println("Archive: printing fitness vals BEFORE:");
		for(int k = 0; k < totalSize; k++) {
			System.out.println("Behaviour " + k + " = " + currentPopulation[k].getPopulationScore() );
		}
		System.out.println("");

		noveltyFitness.calculatePopulationNovelty();

		currentPopulation = noveltyFitness.getGeneration();

		System.out.println("Archive: printing fitness vals AFTER:");
		for(int k = 0; k < totalSize; k++) {
			System.out.println("Behaviour " + k + " = " + currentPopulation[k].getPopulationScore() );
		}

	}

	public double getNovelty(NoveltyBehaviour novBeh) {
		/**
		find the given behaviour in the current generation
		and get its behaviouralSparseness (average distance to k nearest neighbours)
		*/
		System.out.println("Archive: The getNovelty() method is being called");
		double dummyReturn = 10;
		return dummyReturn;
	}

	public void clearGeneration() {
		//System.out.println("Archive: clearing the generation");
		currentGeneration.clear();
	}

}

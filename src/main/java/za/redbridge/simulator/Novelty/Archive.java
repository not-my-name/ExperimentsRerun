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

	private final int MAX_ARCHIVE_SIZE = 150; //archvie can never be larger than population size

	private ArrayList<NoveltyBehaviour> noveltyArchive;
	private ArrayList<NoveltyBehaviour> currentGeneration; //arraylist to store the individual behaviours that are part of the same generation
	private int numNearest; //number of nearest neighbours to use when calculating novelty


	public Archive() {

		//System.out.println("Archive: creating an archive object (constructor)");

		noveltyArchive = new ArrayList<NoveltyBehaviour>();
		currentGeneration = new ArrayList<NoveltyBehaviour>();

		numNearest = 10;

	}

	/*
	method that takes in an array of behaviours that have already been created in the simulation and
	uses the novelty fitness class to calculate the relative novelty of the behaviours in the given collection

	specifically used to find the most novel behaviour produced out of the simulation runs
	does not get averaged like in objective

	this method is called from the getNoveltyBehaviour() method in scoreCalculator
	*/
	public NoveltyBehaviour calculateSimulationNovelty(NoveltyBehaviour[] behaviourCollection) {

		NoveltyFitness noveltyFitness = new NoveltyFitness(behaviourCollection);
		NoveltyBehaviour mostNovel = noveltyFitness.calcSimulationLocalNovelty();
		currentGeneration.add(mostNovel); //adding the most novel result from the simulation runs to the current generation

		return mostNovel;
	}

	//method to calculate the novelty of the individuals in the current generation
	//by comparing them with the archive content as well
	public void calculatePopulationNovelty() {

		//System.out.println("Archive (calculatePopulationNovelty line 63): starting to calculate the novelty");

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
			currentPopulation[k] = noveltyArchive.get(k);
		}

		//System.out.println("Archive (calculatePopulationNovelty): finished filling population with archive individuals");

		//repeat for the current generation
		for(int k = 0; k < numGeneration; k++) {
			currentPopulation[numNovelty+k] = currentGeneration.get(k);
		}

		//System.out.println("Archive: finished filling the population with currentGeneration");

		NoveltyFitness noveltyFitness = new NoveltyFitness(currentPopulation);
		noveltyFitness.calculatePopulationNovelty();
		currentPopulation = noveltyFitness.getGeneration(); //get the entire population instead of most novel because of the different ways to add to the archive

		currentGeneration.clear();
		//iterate over all the behaviours with novelty scores and add the not archived ones to the currentGeneration to keep track of the new scores
		for(NoveltyBehaviour novBeh : currentPopulation) {

			if(!novBeh.isArchived()) {
				currentGeneration.add(novBeh);
			}
		}

		addToArchive();

		//System.out.println("Archive (calculatePopulationNovelty) : finished running calculatePopulationNovelty");
	}

	private void addToArchive() {

		//System.out.println("Archive (addToArchive): adding the latest individuals to the persistent archive");

		//find the most novel behaviour in the current generation
		double maxNovelty = 0;
		NoveltyBehaviour mostNovel = null;

		for(NoveltyBehaviour novBeh : currentGeneration) {

			if(novBeh.getPopulationNoveltyScore() > maxNovelty) {
				maxNovelty = novBeh.getPopulationNoveltyScore();
				mostNovel = novBeh;
			}
		}

		if(mostNovel != null) {
			noveltyArchive.add(mostNovel);
		}

		//System.out.println("Archive (addToArchive): finished adding the behaviours to the archive");
	}

	public ArrayList<NoveltyBehaviour> getArchiveList() {
		return noveltyArchive;
	}

	// /*
	// method to add the most novel behaviour from the currnet population
	// to the archive if its novelty score is larger than a certain threshold
	// currently using the novelty score of least novel in archive*/
	// private void addToArchiveThreshold() {
	//
	// 	double thresholdVal = getLowestArchiveNovelty();
	//
	// 	double max = 0;
	// 	NoveltyBehaviour mostNovel;
	//
	// 	//find the behaviour with the highest novelty score in the current generation
	// 	for(NoveltyBehaviour novBeh : currentGeneration) {
	//
	// 		if(novBeh.getPopulationNoveltyScore() > max) {
	//
	// 			max = novBeh.getPopulationNoveltyScore();
	// 			mostNovel = novBeh;
	// 		}
	// 	}
	//
	// 	if(mostNovel != null) {
	//
	// 		if(noveltyArchive.size() < MAX_ARCHIVE_SIZE) { //check if there is still space in the archive
	// 			noveltyArchive.add(mostNovel);
	// 		}
	// 		else if(mostNovel.getPopulationNoveltyScore() > thresholdVal) { //check if the most novel behaviour is more novel than the lowest archive value
	//
	// 			noveltyArchive.add(mostNovel);
	// 			pruneArchive(); //in case archive exceeds in size
	// 		}
	// 	}
	// }

	/*
	method to add novel behaviours to the archive
	iterate over all the individuals in the current generation and check
	if their novelty score is greater than the threshold (lowest novelty score in archive)
	add all behaviours that have novelty scores greater than the threshold
	if the archive size gets too big, prune the bottom x behaviours */
	// private void addToArchiveBatchThreshold() {
	//
	// 	double thresholdVal = getLowestArchiveNovelty();
	//
	// 	for(NoveltyBehaviour novBeh : currentGeneration) { //iterate over all the individuals in the generation (not archived)
	//
	// 		//add any behaviours with a novelty score higher than the threshold
	// 		if(novBeh.getPopulationNoveltyScore() > thresholdVal) {
	// 			noveltyArchive.add(novBeh);
	// 		}
	// 	}
	//
	// 	pruneArchive();
	// }

	/*
	method that checks if the archive size is within the required bounds
	if it is too big, remove the x least novel behaviours */
	// private void pruneArchive() {
	//
	// 	while(noveltyArchive.size() > MAX_ARCHIVE_SIZE) {
	// 		noveltyArchive.poll();
	// 	}
	//
	// }

	/*
	a method to return the novelty value of the least novel behaviour
	in the archive
	will be used when the priority queue is implemented
	just a dummy return at the moment */
	private double getLowestArchiveNovelty() {
		double dummyReturn = 3;
		return dummyReturn;
	}

	public double getNovelty(NoveltyBehaviour novBeh) {
		/**
		find the given behaviour in the current generation
		and get its behaviouralSparseness (average distance to k nearest neighbours)
		*/
		//System.out.println("Archive: The getNovelty() method is being called");
		double dummyReturn = 10;
		return dummyReturn;
	}

	public void clearGeneration() {
		//System.out.println("Archive: clearing the generation");
		currentGeneration.clear();
	}

}

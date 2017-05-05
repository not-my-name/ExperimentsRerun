package za.redbridge.simulator.paramTuning;

import java.util.concurrent.RecursiveAction;
import java.util.Map;
import java.util.HashMap;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import org.encog.Encog;
import org.encog.ml.MLMethod;
import org.encog.ml.MethodFactory;
import org.encog.ml.ea.train.basic.TrainEA;
import org.encog.neural.neat.NEATNetwork;
import org.encog.neural.neat.NEATPopulation;
import org.encog.neural.networks.BasicNetwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// import za.redbridge.controller.NEAT.NEATPopulation;
import za.redbridge.simulator.NEAT.NEATUtil;

import java.io.IOException;
import java.text.ParseException;

import java.util.Arrays;
import java.util.ArrayList;

import za.redbridge.simulator.config.SimConfig;
import za.redbridge.simulator.khepera.KheperaIIIPhenotype_simple;
import za.redbridge.simulator.khepera.KheperaIIIPhenotype;
import za.redbridge.simulator.phenotype.*;
import za.redbridge.simulator.config.MorphologyConfig;
import za.redbridge.simulator.Main;
import za.redbridge.simulator.ScoreCalculator;
import za.redbridge.simulator.StatsRecorder;
import za.redbridge.simulator.Morphology;


import static za.redbridge.simulator.Utils.isBlank;
import static za.redbridge.simulator.Utils.readObjectFromFile;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

/**
Author: Josh Buchalter
		BCHJOS003

Method that uses forks to compute parameter performance in parallel
*/
public class ParamTester extends RecursiveAction {
	private int sequentialCutoff;  //TO DO: Work out the best value
	private int hi;  //stores the highest index
	private int lo;	 //stores the lowest index
	private ArrayList<double[]> paramList;  //stores the list of possible parameters
	private Main.Args options;

    private static final Logger log = LoggerFactory.getLogger(Main.class);
	public static int thread_count = 0;

	/*
	Paramterized constructor method

	@Params: -transArr     -> float []
			-recArr       -> float []
			-lo 	      -> int
			-hi 	  	  -> int
			-crossCorrArr -> float []
			-sequentialCutoff -> int
	*/
	public ParamTester(ArrayList<double[]> paramList, int lo, int hi, int sequentialCutoff, Main.Args options) {
		this.lo = lo;
		this.hi = hi;
		this.paramList = paramList;
		this.sequentialCutoff = sequentialCutoff;
		this.options = options;
	}

	/**
	Method to test different parameters
	@return: null
	*/
	public void compute () {
		if (hi - lo <= sequentialCutoff) {   //If the size of the data is small enough to make it more feasable to run the sequential algorithm

			try	{
				SimConfig simConfig;
				if (!isBlank(options.configFile)) {
				    simConfig = new SimConfig(options.configFile);
				} else {
				    simConfig = new SimConfig();
				}

				MorphologyConfig mc = new MorphologyConfig("configs/morphologyConfig.yml");
				Morphology morphology = mc.getMorphology(1);
				//Loop through the parameter sets and calculate their performance
				for (int i = lo; i < hi; i++) {
					double [] param = paramList.get(i);
					System.out.println("Running NEAT with objective search");
					ScoreCalculator calculateScore =
					        new ScoreCalculator(simConfig, options.simulationRuns, morphology, param, true, Main.SEARCH_MECHANISM.OBJECTIVE, options.populationSize, 0);
					System.out.println("Parameters " + Arrays.toString(param) + " " + i);

					final NEATPopulation population;

					//create new NEAT population: #input neurons, #output neurons, population size
					population = new NEATPopulation(morphology.getSensors().size(), 2, options.populationSize);
					population.setInitialConnectionDensity(options.connectionDensity);
					population.reset();

					log.debug("Population initialized : "+options.populationSize);

					//DO training
					TrainEA train;
					train = NEATUtil.constructNEATTrainer(population, calculateScore); //a trainer for a score function.
					//set #threads to use
					if (thread_count > 0) {
					    train.setThreadCount(thread_count);
					}
					//intialise the stats recorder
					final StatsRecorder statsRecorder = new StatsRecorder(train, calculateScore, true, Arrays.toString(param), simConfig.toString());

					statsRecorder.recordIterationStats();
					// calculateScore.demo(train.getCODEC().decode(train.getBestGenome()));
					for (int j = train.getIteration(); j < options.numGenerations; j++) {
					    train.iteration();
					    statsRecorder.recordIterationStats();
					}
					// calculateScore.demo(train.getCODEC().decode(train.getBestGenome()));
					log.debug("Training for parameters: "+Arrays.toString(param)+" complete");
					// Encog.getInstance().shutdown();
				}
			}
			catch (IOException ioe) {
				System.out.println(ioe);
			}
			catch (ParseException pe) {
				System.out.println(pe);
			}


		}
		//In the following code: helperThread => left; motherThread => right;
		else {  //If the data size is too big to run the sequential algorithm: split the data in half and run the program on each half in parallel
			int mid = lo + (hi - lo) / 2;
			ParamTester helperThread = new ParamTester(paramList, lo, mid, sequentialCutoff, options);  //creates the left half of the data (to be processed by the helper thread which is forked off the mother thread)
			ParamTester motherThread = new ParamTester(paramList, mid, hi, sequentialCutoff, options); //creates the right half (to be worked on by the mother thread)
			helperThread.fork();  //creates the helper thread to process the left half of data in parallel with the mother thread
			motherThread.compute();  //direct call to compute() to calculate the right half of the data from the mother thread
			helperThread.join();	//wait until the left thread has terminated

		}
	}

	public void endParamTraining() {
		Encog.getInstance().shutdown();
	}
}

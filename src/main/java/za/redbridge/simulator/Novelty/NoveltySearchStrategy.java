//this is one of the classes that have been overriden for the novelty search part
package za.redbridge.simulator;

import org.encog.ml.train.strategy.Strategy;
import org.encog.ml.train.MLTrain;
import org.encog.util.logging.EncogLogging;
import org.encog.ml.ea.train.basic.TrainEA;
import org.encog.ml.ea.species.Species;
import org.encog.ml.ea.genome.Genome;
import org.encog.neural.neat.NEATNetwork;
import org.encog.neural.networks.BasicNetwork;
import org.encog.ml.MLMethod;
import org.encog.neural.neat.NEATLink;

import java.util.List;
import java.util.LinkedList;

import za.redbridge.simulator.Simulation;
import za.redbridge.simulator.SimulationGUI;
import za.redbridge.simulator.ScoreCalculator;
import za.redbridge.simulator.config.SimConfig;
import za.redbridge.simulator.factories.HomogeneousRobotFactory;
import za.redbridge.simulator.factories.RobotFactory;
import za.redbridge.simulator.factories.ConfigurableResourceFactory;
import za.redbridge.simulator.factories.ResourceFactory;
import za.redbridge.simulator.phenotype.*;
import za.redbridge.simulator.Morphology;
import za.redbridge.simulator.NoveltyCodec;


public class NoveltySearchStrategy implements Strategy {

	private final int popSize;

	private NoveltyTrainEA mainTrain;
	private final ScoreCalculator scoreCalculator;
	private NoveltyCodec codec;

	public NoveltySearchStrategy (int popSize, ScoreCalculator scoreCalculator) {
		//System.out.println("NoveltySearchStrategy: creating the new search strategy object");
		this.popSize = popSize;
		this.scoreCalculator = scoreCalculator;
	}

	/**
	 * Initialize this strategy.
	 * @param train The training algorithm.
	 */
	public void init(MLTrain train) {
		//System.out.println("NoveltySearchStrategy: initialising the search strategy");
		this.mainTrain = (NoveltyTrainEA)train;
		codec = (NoveltyCodec)mainTrain.getCODEC();
	}

	/**
	 * Called just before a training iteration. Generate all individuals in the population
	 this method gets called from the TrainEA preIteration() method
	 */
	public void preIteration() {

		//System.out.println("NoveltySearchStrategy (line 59): starting the preiteration method");

		int tempCounter = 0; //remember to remove all mentions of the counter once the testing is completed

		if(mainTrain.getIteration() == 0) {
			//System.out.println("NoveltySearchStrategy: mainTrain iteration == 0 / start of a generation");

			//System.out.println("NoveltySearchStrategy: number of species = " + mainTrain.getPopulation().getSpecies().size());

			for (Species species : mainTrain.getPopulation().getSpecies()) {
				//System.out.println("NoveltySearchStrategy: number of members for current species = " + species.getMembers().size());
				for (Genome g : species.getMembers()) {

					tempCounter++;
					//System.out.println("NoveltySearchStrategy: the number of the current individual = " + tempCounter);

					//System.out.println("NoveltySearchStrategy: decoding all the individual ML methods to be tested in the simulator");

					MLMethod method = codec.decode(g); //this method is just called in order to
				}
			}

			//System.out.println("NoveltySearchStrategy (line 75): finished decoding all the networks");
			//System.out.println("NoveltySearchStrategy: about to calculate the novelty for each of the individuals in the score calculator");

			scoreCalculator.calculateNoveltyForPopulation();
		}
		else {
			//System.out.println("NoveltySearchStrategy: mainTrain iteration != 0");
			scoreCalculator.clearCurrentGeneration();
		}

		//System.out.println("NoveltySearchStrategy (preIteration): finished running the preIteration method");
	}

	/**
	 * Called just after a training iteration.
	 */
	public void postIteration() {

		//scoreCalculator.printArchive();
	}
}

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
		this.popSize = popSize;
		this.scoreCalculator = scoreCalculator;
	}

	/**
	 * Initialize this strategy.
	 * @param train The training algorithm.
	 */
	public void init(MLTrain train) {
		this.mainTrain = (NoveltyTrainEA)train;
		codec = (NoveltyCodec)mainTrain.getCODEC();
	}
	
	/**
	 * Called just before a training iteration. Generate all individuals in the population
	 this method gets called from the TrainEA preIteration() method
	 */
	public void preIteration() {

		if(mainTrain.getIteration() == 0) {

			for (Species species : mainTrain.getPopulation().getSpecies()) {
				for (Genome g : species.getMembers()) {

					MLMethod method = codec.decode(g); //this method is just called in order to 
				}
			}
			
			scoreCalculator.calculateNoveltyForPopulation();
		}
		else {
			scoreCalculator.clearCurrentGeneration();
		}
	}
	
	/**
	 * Called just after a training iteration.
	 */
	public void postIteration() {
		
	}
}
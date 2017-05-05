package za.redbridge.simulator.novelty;

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
import za.redbridge.simulator.novelty.*;

/**
A class that etends the Encog Strategy interface. This strategy class generates all individuals for a generation
before actual 'fitness' is calculated (in TrainEA.iteration()). This allows novelty search to reward individuals
by comparing them to ALL members of the current generation as well as the archive (which is stored in NoveltyArchive).
**/
public class NoveltySearchStrategy implements Strategy {

	private final int popSize;

	private NoveltyTrainer mainTrain;
	private final ScoreCalculator calculateScore;
	private NoveltyPhenotypeCODEC codec;

	public NoveltySearchStrategy (int popSize, ScoreCalculator calculateScore) {
		this.popSize = popSize;
		this.calculateScore = calculateScore;
	}

	/**
	 * Initialize this strategy.
	 * @param train The training algorithm.
	 */
	public void init(MLTrain train) {
		this.mainTrain = (NoveltyTrainer)train;
		codec = (NoveltyPhenotypeCODEC)mainTrain.getCODEC();
	}
	
	/**
	 * Called just before a training iteration. Generate all individuals in the population
	 */
	public void preIteration() {
		// System.out.println("PREITERATION:");
		if (mainTrain.getIteration() == 0) {
			for (Species species : mainTrain.getPopulation().getSpecies()) {
				for (Genome g : species.getMembers()) {
					codec.decode(g);
					// NoveltyPhenotype method = (NoveltyPhenotype)codec.decode(g);
					// double newScore = calculateScore.calculateScore(method);
					// g.setScore(newScore);
					// if (g.getScore() > bestScore) {
					// 	// System.out.println("best genome: " + g);
					// 	bestScore = g.getScore();
					// 	bestGenome = g;
					// }
					// System.out.println(g);
				}
			}
			calculateScore.calculateNoveltyForPopulation();
		}
		else {
			// List<Genome> currPop = new LinkedList<>();
			// for (Species species : mainTrain.getPopulation().getSpecies()) {
			// 	for (Genome g : species.getMembers()) {
			// 		// System.out.println(g);
			// 		currPop.add(g);
			// 	}
			// }

			// codec.clearCurrPop(currPop);
			calculateScore.clearCurrPop();
		}
		// System.out.println("Beginng of preiteration");
		// // System.out.println("Preiteration BG: " + mainTrain.getPopulation().getBestGenome());
		// for (Species species : mainTrain.getPopulation().getSpecies()) {
		// 	for (Genome g : species.getMembers()) {
		// 		codec.decode(g);
		// 		// NoveltyPhenotype method = (NoveltyPhenotype)codec.decode(g);
		// 		// double newScore = calculateScore.calculateScore(method);
		// 		// g.setScore(newScore);
		// 		// if (g.getScore() > bestScore) {
		// 		// 	// System.out.println("best genome: " + g);
		// 		// 	bestScore = g.getScore();
		// 		// 	bestGenome = g;
		// 		// }
		// 		// System.out.println("\t" + g);
		// 	}
		// }
		// System.out.println("End of preiteration");
	}
	
	/**
	 * Called just after a training iteration.
	 */
	public void postIteration() {
		// System.out.println("Beginning of postiteration");
		// List<Genome> currPop = new LinkedList<>();
		// for (Species species : mainTrain.getPopulation().getSpecies()) {
		// 	for (Genome g : species.getMembers()) {
		// 		// System.out.println(g);
		// 		currPop.add(g);
		// 	}
		// }

		// codec.clearCurrPop(currPop);
		// System.out.println("End of postiteration");
		
		// System.out.println("Size of currPop at end of postIteration: " + codec.getCODECHistorySize());
		// calculateScore.calculateNoveltyForPopulation();

		// Genome bestGenome = mainTrain.getPopulation().getBestGenome();
		// double bestScore = bestGenome.getScore();

		// for (Species species : mainTrain.getPopulation().getSpecies()) {
		// 	for (Genome g : species.getMembers()) {
		// 		NoveltyPhenotype method = (NoveltyPhenotype)codec.decode(g);
		// 		double newScore = calculateScore.calculateScore(method);
		// 		g.setScore(newScore);
		// 		if (g.getScore() > bestScore) {
		// 			// System.out.println("best genome: " + g);
		// 			bestScore = g.getScore();
		// 			bestGenome = g;
		// 		}
		// 		// System.out.println("\t" + g);
		// 	}
		// }
		// // System.out.println("FINAL BG: " + bestGenome);
		// mainTrain.getPopulation().setBestGenome(bestGenome);
		// codec.clearCurrPop(currPop);
	}
}
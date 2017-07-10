package za.redbridge.simulator;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import org.encog.neural.hyperneat.substrate.Substrate;
import za.redbridge.simulator.ScoreCalculator;
import org.encog.neural.neat.NEATPopulation; //importing the neat population
import za.redbridge.simulator.config.MorphologyConfig;
import org.encog.neural.neat.NEATNetwork;
//import org.encog.neural.hyperneat.substrate.SubstrateFactory;
import org.encog.ml.ea.train.basic.TrainEA;
import za.redbridge.simulator.StatsRecorder;
import za.redbridge.simulator.Morphology;
import za.redbridge.simulator.config.SimConfig;
import za.redbridge.simulator.NEATUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.text.ParseException;
import org.encog.neural.neat.training.species.OriginalNEATSpeciation;

import org.encog.Encog;

import za.redbridge.simulator.phenotype.ChasingPhenotype;

import org.encog.ml.train.strategy.Strategy;
import java.util.List;

import static za.redbridge.simulator.Utils.isBlank;
import static za.redbridge.simulator.Utils.readObjectFromFile;

import za.redbridge.simulator.Archive;

import za.redbridge.simulator.Utils;

public class Main {

	private final static Logger log = LoggerFactory.getLogger(Main.class);
	private final static double convergenceScore = 1000;

	private final static boolean PerformingObjectiveSearch = false;
	private final static boolean PerformingNoveltySearch = true;
	private final static boolean PerformingHybridSearch = false;

	private static int numInputs;
	private int numOutputs = 2;
	private int populationSize;
	private int simulationRuns;
	private int numIterations;
	private int threadCount;

	private static Archive archive;

	private static int schemaConfigIndex; //the schema against which the construction zone should be checked

	private static String resConfig;

	private static double envHeight;
	private static double envWidth;

	public static void main(String args[]) throws IOException, ParseException{

		Args options = new Args();
		new JCommander(options, args);

		log.info(options.toString());
		int ind = 1;

		double connectionDensity = 0.5;
		//fetching the correct simConfig for each experiment
		String simConfigFP = "configs/simConfig" + Integer.toString(ind) + ".yml";
		//String experimentConfigFP = "configs/experimentConfig.yml";
		String morphologyConfigFP = "configs/morphologyConfig.yml";
		String folderDir = "/Results";
		Utils.setDirectoryName(folderDir);

		MorphologyConfig morphologyConfig = new MorphologyConfig(morphologyConfigFP);
		Morphology morphology = morphologyConfig.getMorphology(1);
		numInputs = morphology.getNumSensors();

		SimConfig simConfig = new SimConfig(simConfigFP);

		resConfig = options.environment;

		envWidth = simConfig.getEnvironmentWidth();
		envHeight = simConfig.getEnvironmentHeight();

		schemaConfigIndex = simConfig.getConfigNumber();
		ScoreCalculator scoreCalculator = new ScoreCalculator(simConfig, options.simulationRuns,
						morphology, options.populationSize, schemaConfigIndex, envHeight, envWidth); //got this from the Main class in last years Controller Master folder

		NEATNetwork network = (NEATNetwork) readObjectFromFile("/home/p/pttand010/Desktop/HonoursExperimentResults/EvaluationResults/FinalEvalResults/Schema_1/BestNoveltyNetwork/Run_2/epoch-87/network.ser");

		final StatsRecorder statsRecorder = new StatsRecorder(scoreCalculator); //this is basically where the simulation runs

		//scoreCalculator.demo(network);

		scoreCalculator.runEvaluation(network);

		//for(int i = 0; i < 20; i++) {
			//scoreCalculator.runEvaluation(network);
			//statsRecorder.recordIterationStats(i);
		//}
		Encog.getInstance().shutdown();
	}

	private static class Args {
        @Parameter(names = "-c", description = "Simulation config file to load")
        private String configFile = "configs/simConfig.yml";

        @Parameter(names = "-i", description = "Number of generations to train for")
        private int numGenerations = 100;

        @Parameter(names = "-p", description = "Initial population size")
        private int populationSize = 150;

        @Parameter(names = "--sim-runs", description = "Number of simulation runs per iteration")
        private int simulationRuns = 5;

        @Parameter(names = "--conn-density", description = "Adjust the initial connection density"
                + " for the population")
        private double connectionDensity = 0.5;
        @Parameter(names = "--demo", description = "Show a GUI demo of a given genome")
        private String genomePath = null;
        //private String genomePath = "results/Hex-20160920T2134_null__NEAT/best networks/epoch-5/network.ser";
        //private String genomePath = "results/ruben-GE72-2QD-20161030T1126_null/best networks/epoch-1/network.ser";
        //private String genomePath = "results/ruben-GE72-2QD-20161102T1342_null/best networks/epoch-1/network.ser";

        @Parameter(names = "--control", description = "Run with the control case")
        private boolean control = false;

        @Parameter(names = "--advanced", description = "Run with advanced envrionment and morphology")
        private boolean advanced = true;

        @Parameter(names = "--environment", description = "Run with advanced envrionment and morphology")
        private String environment = "";

        @Parameter(names = "--morphology", description = "For use with the control case, provide"
                + " the path to a serialized MMNEATNetwork to have its morphology used for the"
                + " control case")
        private String morphologyPath = null;

        @Parameter(names = "--population", description = "To resume a previous controller, provide"
                + " the path to a serialized population")
        private String populationPath = null;

        @Override
        public String toString() {
            return "Options: \n"
                    + "\tConfig file path: " + configFile + "\n"
                    + "\tNumber of generations: " + numGenerations + "\n"
                    + "\tPopulation size: " + populationSize + "\n"
                    + "\tNumber of simulation tests per iteration: " + simulationRuns + "\n"
                    + "\tInitial connection density: " + connectionDensity + "\n"
                    + "\tDemo network config path: " + genomePath + "\n"
                    + "\tRunning with the control case: " + control + "\n"
                    + "\tMorphology path: " + morphologyPath + "\n"
                    + "\tPopulation path: " + populationPath;
        }
    }

}

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

	private static int numInputs;
	private int numOutputs = 2;
	private int populationSize;
	private int simulationRuns;
	private int numIterations;
	private int threadCount;

	private static Archive archive;

	private int morphCollection = [2,5,6];

	public static void main(String args[]) throws IOException, ParseException{

		for(int j = 0; j < 3; j++) { //iterating over the different morphologies
			morphIndex = morphCollection[j];

			for(int k = 0; k < 3; k++) { //iterating over the different complexity levels

				Args options = new Args();
				new JCommander(options, args);
				log.info(options.toString());

				int difficulty = k+1;

				//getting the correct simulation configuration for this experiment case
				//simconfig shows the types of blocks present, as well as their properties and the connection schema that is to be used
				String simConfigFP = "configs/simConfig" + Integer.toString(difficulty) + ".yml";
				SimConfig simConfig = new SimConfig(simConfigFP);

				SensorCollection sensorCollection = new SensorCollection("configs/morphologyConfig.yml");
				Morphology morphology = sensorCollection.getMorph(morphIndex);
				numInputs = morphology.getNumSensors();

				//creating the folder directory for the results
				String difficultyLevel = "";
				String dLevel = "";
				if (difficulty == 1) {
	                difficultyLevel = "Level_1_nocoop_simple";
					dLevel = "Level_1";
	            }
	            else if (difficulty == 2) {
	                difficultyLevel = "Level_2_coop_simple";
					dLevel = "Level_2";
	            }
	            else if (difficulty == 3) {
	                difficultyLevel = "Level_3_nocoop_complex";
					dLevel = "Level_3";
	            }
	            else if(difficulty == 4) {
	                difficultyLevel = "Level_4_coop_complex";
					dLevel = "Level_4";
	            }

				String folderDir = "/EvaluationRuns/Morphology_" + str(morphIndex) + "/" + difficultyLevel;
				Utils.setDirectoryName(folderDir);

				ScoreCalculator scoreCalculator = new ScoreCalculator(simConfig, options.simulationRuns,
									morphology, options.populationSize, sensorCollection);

				if (!isBlank(options.genomePath)) {
					   NEATNetwork network = (NEATNetwork) readObjectFromFile(options.genomePath);
					   scoreCalculator.demo(network);
					   return;
			    }

				//String networkSourceDirectory = "/home/ruben/Masters_2017/Experiments/EvaluationRuns/ExperimentsRerun/ResultNetworks/network.ser";
				String networkSourceDirectory = "/home/ruben/Masters_2017/Experiments/ConferenceEvalRuns/First_Fixed/ExperimentsRerun/ConferenceResults/First/" + dLevel + "/network.ser";

				//final StatsRecorder statsRecorder = new StatsRecorder(trainer, scoreCalculator); //this is basically where the simulation runs

				NEATNetwork network = (NEATNetwork) readObjectFromFile(networkSourceDirectory);

				//final StatsRecorder statsRecorder = new StatsRecorder(scoreCalculator); //this is basically where the simulation runs

				scoreCalculator.runEvaluation(network);
				log.debug("Evaluation Complete");
				Encog.getInstance().shutdown();
			}
		}
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

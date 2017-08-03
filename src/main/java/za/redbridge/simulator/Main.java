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

import java.nio.file.*;
import za.redbridge.simulator.GraphvizEngine;

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

	//have an array of folder directories for each of the network controllers
	//private static String[] controllerDirectories = [];

	public static void main(String args[]) throws IOException, ParseException{

		GraphvizEngine gvEngine = new GraphvizEngine();

		String sourceDirectory = "/home/ruben/Masters_2017/Network_Visualisations/ExperimentsRerun/ConferenceResults/TestingVisualisations/Level_";

		for(int k = 1; k < 4; k++) { //iterating over the different complexity levels

			String networkSourceDirectory = sourceDirectory + Integer.toString(k);
			String bestNetworkSourceDir = networkSourceDirectory + "/Best/";
			String worstNetworkSourceDir = networkSourceDirectory + "/Worst/";

			String bestNetworkLocation = bestNetworkSourceDir + "network.ser";
			String worstNetworkLocation = worstNetworkSourceDir + "network.ser";

			String outBestNetwork = bestNetworkSourceDir + "Level_" + Integer.toString(k) + "_Best";
			String outWorstNetwork = worstNetworkSourceDir + "Level_" + Integer.toString(k) + "_Worst";

			Path bestPath = Paths.get(outBestNetwork);
			Path worstPath = Paths.get(outWorstNetwork);

			NEATNetwork bestNetwork = (NEATNetwork) readObjectFromFile(bestNetworkLocation);
			NEATNetwork worstNetwork = (NEATNetwork) readObjectFromFile(worstNetworkLocation);

			System.out.println("Level " + k + " Best input count = " + bestNetwork.getInputCount());
			System.out.println("Level " + k + " Worst input count = " + worstNetwork.getInputCount());

			gvEngine.saveNetwork(bestNetwork, bestPath);
			gvEngine.saveNetwork(worstNetwork, worstPath);
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

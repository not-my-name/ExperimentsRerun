package za.redbridge.simulator;

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
import za.redbridge.simulator.novelty.NoveltySearchStrategy;
import za.redbridge.simulator.NEAT.NEATUtil;

import java.io.IOException;
import java.text.ParseException;

import java.util.Arrays;
import java.util.concurrent.ForkJoinPool;

import za.redbridge.simulator.config.SimConfig;
import za.redbridge.simulator.khepera.KheperaIIIPhenotype_simple;
import za.redbridge.simulator.khepera.KheperaIIIPhenotype;
import za.redbridge.simulator.phenotype.*;
import za.redbridge.simulator.config.MorphologyConfig;
import za.redbridge.simulator.paramTuning.*;
import za.redbridge.simulator.novelty.NoveltyTrainer;

import za.redbridge.simulator.Utils;

import static za.redbridge.simulator.Utils.isBlank;
import static za.redbridge.simulator.Utils.readObjectFromFile;

/**
 * Entry point for the controller platform.
 *
 * Created by jamie on 2014/09/09.
 */
public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private static final double CONVERGENCE_SCORE = 110;

    // public static final boolean IS_NOVELTY = false;
    // public static final boolean IS_HYBRID = true;

    public static enum SEARCH_MECHANISM {
        OBJECTIVE, NOVELTY, HYBRID
    }

    public static final boolean PARAM_TUNING = false;

    public static String RES_CONFIG;

    public static int thread_count = 0;

    public static void main(String[] args) throws IOException, ParseException {

        for(int k = 0; k < 3; k++) { //iterating over the different simConfig files in order to change the difficulty

            Args options = new Args();
            new JCommander(options, args);

            log.info(options.toString());
            int difficulty = k+1; //so that it can access simconfig1 simconfig2 etc for difficulty

            String simConfigFP = "configs/simConfig" + Integer.toString(difficulty) + ".yml";

            SimConfig simConfig = new SimConfig(simConfigFP);
            // if (!isBlank(options.configFile)) {
            //     simConfig = new SimConfig(options.configFile);
            // } else {
            //     simConfig = new SimConfig();
            // }

            //MorphologyConfig mc = new MorphologyConfig("configs/morphologyConfig.yml");
            SensorCollection sensorCollection = new SensorCollection("configs/morphologyConfig.yml");

            //Morphology morphology = mc.getMorphology(1);
            Morphology morphology = sensorCollection.getIdealMorph();

            String difficultyLevel = "";
            if (difficulty == 1) {
                difficultyLevel = "Easy";
            }
            else if (difficulty == 2) {
                difficultyLevel = "Medium";
            }
            else if (difficulty == 3) {
                difficultyLevel = "Hard";
            }

            String folderDir = "/NEATExperiments/Novelty/" + difficultyLevel;
            Utils.setDirectoryName(folderDir);

            //System.out.println("Sensors count :" + morphology.getNumSensors());
            RES_CONFIG = options.environment;

            /////////////////////////////////////////////////////////////////////////////////////////////
            //System.out.println("Running NEAT with objective search: RUN " + (run+1));
            double[] params = {1000, 0, 0.3, 0.6, 1};
            // double[] params = {1000, 0.3, 0, 0, 0.2, 1, 0};
            // double[] params = {1000, 0.5, 0.3, 0.1, 0.2, 1, 0.2};
            ScoreCalculator calculateScore =
                    new ScoreCalculator(simConfig, options.simulationRuns, morphology, params, PARAM_TUNING,
                    SEARCH_MECHANISM.NOVELTY, options.populationSize, 0, sensorCollection);

            if (!isBlank(options.genomePath)) {
                NEATNetwork network = (NEATNetwork) readObjectFromFile(options.genomePath);
                calculateScore.demo(network);
                return;
            }

            final NEATPopulation population;

            //create new NEAT population: #input neurons, #output neurons, population size
            //population = new NEATPopulation(5, 2, options.populationSize);
            population = new NEATPopulation(morphology.getSensors().size(), 2, options.populationSize);
            population.setInitialConnectionDensity(options.connectionDensity);
            population.reset();

            log.debug("Population initialized : "+options.populationSize);

            //DO training
            NoveltyTrainer train;
            train = NEATUtil.constructNEATNoveltyTrainer(population, calculateScore, options.searchMechanism); //a trainer for a score function.
            train.addStrategy(new NoveltySearchStrategy(options.populationSize, calculateScore));
            //set #threads to use
            if (thread_count > 0) {
                train.setThreadCount(thread_count);
            }

            //intialise the stats recorder
            final StatsRecorder statsRecorder = new StatsRecorder(train, calculateScore, PARAM_TUNING, Arrays.toString(params), simConfig.toString());
            statsRecorder.recordIterationStats();
            // calculateScore.demo(train.getCODEC().decode(train.getBestGenome()));
            for (int i = train.getIteration(); i < options.numGenerations; i++) {
                train.iteration();
                statsRecorder.recordIterationStats();
                // if (train.getBestGenome().getScore() >= CONVERGENCE_SCORE) {
                //     log.info("Convergence reached at epoch " + train.getIteration());
                //     break;
                // }
            }
            // calculateScore.demo(train.getCODEC().decode(train.getBestGenome()));
            log.debug("Training complete");
            Encog.getInstance().shutdown();
        }

        //     NEAT with Objective search
        //
        //     objective fitness of a simulation run:
        //     A - average distance between robot and closest resource to it
        //     B - average number of times that robots connected to resources
        //     C - average distance between resources
        //     D - the number of adjacent resources
        //     E - the number of adjacent resources that are in the correct schema
        //     F - average distance between resources and the construction starting area
        //     OR:
        //     A - number of times a robot successfully found blocks
        //     B - number of times type A blocks were pushed by one robot and connected with a built structure
        //     C - number of times type B blocks were pushed by two robots and connected with a built structure
        //     D - number of times type C blocks were pushed by two robots and connected with a built structure
        //     **/
    }

    public static class Args {
        @Parameter(names = "-c", description = "Simulation config file to load")
        public String configFile = "configs/simConfig.yml";

        @Parameter(names = "-i", description = "Number of generations to train for")
        public int numGenerations = 2;//100;

        @Parameter(names = "-p", description = "Initial population size")
        public int populationSize = 3;//150;

        @Parameter(names = "--sim-runs", description = "Number of simulation runs per iteration")
        public int simulationRuns = 2;//5;

        @Parameter(names = "--search-mechanism", description = "The search mechanism to be used")
        public SEARCH_MECHANISM searchMechanism = SEARCH_MECHANISM.NOVELTY;

        @Parameter(names = "--conn-density", description = "Adjust the initial connection density"
                + " for the population")
        public double connectionDensity = 0.5;
        @Parameter(names = "--demo", description = "Show a GUI demo of a given genome")
        public String genomePath = null;
        // public String genomePath = "results/Hex-20160925T1813_null__NEAT/best networks/epoch-7/network.ser";
        // public String genomePath = "paramTuning/results/[500.0, 100.0, 100.0, 100.0, 100.0, 50.0, 2.0]/Hex-20161002T0312_null__NEAT/best networks/epoch-10/network.ser";
        // public String genomePath = "results/Hex-20161002T1530_null__NEAT/best networks/epoch-49/network.ser";
        // public String genomePath = "paramTuning/results/[500.0, 0.5, 0.5, 0.0, 0.0, 0.0, 0.0]/Hex-20161004T1412_null__NEAT/best networks/epoch-23/network.ser";
        // public String genomePath = "paramTuning/results/[500.0, 0.5, 0.5, 0.3, 0.0, 0.0, 0.0]/Hex-20161004T1412_null__NEAT/best networks/epoch-13/network.ser";
        // public String genomePath = "paramTuning/results/[500.0, 0.0, 0.5, 1.0, 3.0]/Hex-20161004T2135_null__NEAT/best networks/epoch-1/network.ser";
        // public String genomePath = "paramTuning/results/[500.0, 1.0, 0.5, 1.0, 3.0]/Hex-20161004T2134_null__NEAT/best networks/epoch-9/network.ser";
        // public String genomePath = "paramTuning/results/[500.0, 0.5, 0.5, 0.3, 0.0, 0.0, 0.0]/Hex-20161004T1412_null__NEAT/best networks/epoch-13/network.ser";
        // public String genomePath = "paramTuning/results/[500.0, 0.5, 0.5, 0.3, 0.3, 0.0, 0.0]/Hex-20161004T1642_null__NEAT/best networks/epoch-27/network.ser";
        // public String genomePath = "paramTuning/results/[500.0, 0.5, 0.5, 0.5, 0.3, 0.0, 0.0]/Hex-20161004T1642_null__NEAT/best networks/epoch-28/network.ser";
        // public String genomePath = "paramTuning/results/[500.0, 100.0, 100.0, 100.0, 100.0, 50.0, 2.0]/Hex-20161002T0312_null__NEAT/best networks/epoch-10/network.ser";
        // public String genomePath = "paramTuning/results/[500.0, 1.0, 1.0, 1.0, 1.0]/Hex-20161006T1541_null__NEAT/best networks/epoch-30/network.ser";
        // public String genomePath = "paramTuning/results/[500.0, 0.0, 0.0, 0.5, 0.5, 0.0, 0.0]/Hex-20161008T2317_null__NEAT/best networks/epoch-27/network.ser";
        // public String genomePath = "paramTuning/results/[500.0, 0.0, 0.0, 0.5, 0.5, 0.3, 0.0]/Hex-20161008T1126_null__NEAT/best networks/epoch-27/network.ser";
        // public String genomePath = "paramTuning/results/[500.0, 0.0, 0.0, 0.5, 0.5, 0.5, 0.3]/Hex-20161008T1126_null__NEAT/best networks/epoch-23/network.ser";
        // public String genomePath = "paramTuning/results/[500.0, 0.0, 0.0, 0.5, 0.5, 0.5, 0.5]/Hex-20161008T2314_null__NEAT/best networks/epoch-24/network.ser";
        // public String genomePath = "paramTuning/results/[500.0, 0.0, 0.7, 0.9, 1.0]/Hex-20161006T1645_null__NEAT/best networks/epoch-28/network.ser";
        // public String genomePath = "paramTuning/results/[500.0, 0.0, 1.0, 1.0, 1.0]/Hex-20161006T1645_null__NEAT/best networks/epoch-30/network.ser";
        // public String genomePath = "paramTuning/results/[500.0, 0.3, 0.3, 0.3, 0.3, 0.3, 0.3]/Hex-20161019T1149_null__NEAT/best networks/epoch-29/network.ser";
        // public String genomePath = "paramTuning/results/[500.0, 0.5, 0.5, 0.5, 0.5, 0.5, 0.3]/Hex-20161019T1149_null__NEAT/best networks/epoch-14/network.ser";
        // public String genomePath = "paramTuning/results/[500.0, 0.0, 0.0, 0.5, 0.5, 0.0, 0.0]/Hex-20161019T2030_null__NEAT/best networks/epoch-22/network.ser";
        // public String genomePath = "results/Hex-20161023T2133_null__NEAT/best networks/epoch-1/network.ser";
        // public String genomePath = "results/Hex-20161027T1306_null__NEAT/best networks/epoch-7/network.ser";
        // public String genomePath = "results/Hex-20161028T1449_null__NEAT/best networks/epoch-3/network.ser";

        //UPDATED MUTATION RUNS
        // public String genomePath = "/Users/joshbuchalter/Google Drive/Varsity/Honours/CS/Honours_Project/TestRuns/UpdatedMutation/Hex-20161105T1642_null__NEAT/best networks/epoch-57/network.ser";

        //1 CZ TEST RUN
        // public String genomePath = "/Users/joshbuchalter/Google Drive/Varsity/Honours/CS/Honours_Project/TestRuns/SingleCZTest/Hex-20161105T1314_null__NEAT/best networks/epoch-48/network.ser";

        //SHORT FITNESS FUNCTION BEST PERFORMING NETOWRK
        // public String genomePath = "/Users/joshbuchalter/Google Drive/Varsity/Honours/CS/Honours_Project/TestRuns/ShortFitness Function/[500.0, 0.0, 1.0, 1.0, 1.0]/Hex-20161104T2250_null__NEAT/best networks/epoch-30/network.ser";
        //Best from updated mutation
        // public String genomePath = "/Users/joshbuchalter/Google Drive/Varsity/Honours/CS/Honours_Project/TestRuns/ShortFitness/ShortFitness1/[500.0, 0.0, 1.0, 1.0, 1.0]/Hex-20161107T0215_null__NEAT/best networks/epoch-38/network.ser";
        // public String genomePath = "/Users/joshbuchalter/Google Drive/Varsity/Honours/CS/Honours_Project/TestRuns/ShortFitness/ShortFitness2/[500.0, 1.0, 0.3, 0.6, 1.0]/Hex-20161106T2245_null__NEAT/best networks/epoch-10/network.ser";

        //LONG FITNESS FUNCTION BEST PERFORMING
        // public String genomePath = "/Users/joshbuchalter/Google Drive/Varsity/Honours/CS/Honours_Project/TestRuns/LongFitnessFunctionRuns/[500.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0]/Hex-20161104T2243_null__NEAT/best networks/epoch-18/network.ser";
        //Best from updated mutation
        // public String genomePath = "/Users/joshbuchalter/Google Drive/Varsity/Honours/CS/Honours_Project/TestRuns/LongFitness/LongFitness1/[500.0, 0.8, 0.7, 0.9, 1.0, 1.0, 1.0]/Hex-20161106T2208_null__NEAT/best networks/epoch-40/network.ser";

        //NOVELTY RUNS
        // public String genomePath = "results/Hex-20161102T2333_null__NEAT/best networks/epoch-78/network.ser";

        //HYBRID RUNS

        //LONG OBJECTIVE RUNS
        // public String genomePath = "/Users/joshbuchalter/Documents/casairt-2016-Josh-implementation/ObjectiveRunsSoFar/CZs-Not_Updated/Hex-20161031T1703_null__NEAT/best networks/epoch-37/network.ser";
        // public String genomePath = "/Users/joshbuchalter/Documents/casairt-2016-Josh-implementation/ObjectiveRunsSoFar/CZs-Not_Updated/Hex-20161031T1703_null__NEAT/best networks/epoch-88/network.ser";
        // public String genomePath = "/Users/joshbuchalter/Documents/casairt-2016-Josh-implementation/ObjectiveRunsSoFar/CZs_UPDATED/Hex-20161103T0000_null__NEAT/best networks/epoch-73/network.ser";

        @Parameter(names = "--control", description = "Run with the control case")
        public boolean control = false;

        @Parameter(names = "--advanced", description = "Run with advanced envrionment and morphology")
        public boolean advanced = true;

        @Parameter(names = "--environment", description = "Run with advanced envrionment and morphology")
        public String environment = "";

        @Parameter(names = "--morphology", description = "For use with the control case, provide"
                + " the path to a serialized MMNEATNetwork to have its morphology used for the"
                + " control case")
        public String morphologyPath = null;

        @Parameter(names = "--population", description = "To resume a previous controller, provide"
                + " the path to a serialized population")
        public String populationPath = null;

        @Override
        public String toString() {
            return "Options: \n"
                    + "\tConfig file path: " + configFile + "\n"
                    + "\tNumber of generations: " + numGenerations + "\n"
                    + "\tPopulation size: " + populationSize + "\n"
                    + "\tNumber of simulation tests per iteration: " + simulationRuns + "\n"
                    + "\tSearch mechanism implemented: " + searchMechanism.name() + "\n"
                    + "\tInitial connection density: " + connectionDensity + "\n"
                    + "\tDemo network config path: " + genomePath + "\n"
                    + "\tRunning with the control case: " + control + "\n"
                    + "\tMorphology path: " + morphologyPath + "\n"
                    + "\tPopulation path: " + populationPath;
        }
    }
}

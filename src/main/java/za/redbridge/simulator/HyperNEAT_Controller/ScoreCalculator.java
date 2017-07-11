package za.redbridge.simulator;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SynchronizedDescriptiveStatistics;
import org.encog.ml.CalculateScore;
import org.encog.ml.MLMethod;
import org.encog.neural.neat.NEATNetwork;
import org.encog.neural.networks.BasicNetwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import sim.display.Console;
import za.redbridge.simulator.Simulation;
import za.redbridge.simulator.SimulationGUI;
import za.redbridge.simulator.config.SimConfig;
import za.redbridge.simulator.factories.HomogeneousRobotFactory;
import za.redbridge.simulator.factories.RobotFactory;
import za.redbridge.simulator.phenotype.Phenotype;

import za.redbridge.simulator.HyperNEATPhenotype;
import za.redbridge.simulator.Morphology;

import za.redbridge.simulator.factories.ConfigurableResourceFactory;
import za.redbridge.simulator.factories.ResourceFactory;

import za.redbridge.simulator.SensorCollection;

import java.util.*;

import za.redbridge.simulator.StatsRecorder;

public class ScoreCalculator implements CalculateScore {

    private static final Logger log = LoggerFactory.getLogger(ScoreCalculator.class);

    private final SimConfig simConfig;
    private final int simulationRuns;
    private final Morphology sensorMorphology;

    private int schemaConfigNum;
    private int experimentRun; //keep track of which number experiment is being run

    private final DescriptiveStatistics performanceStats = new SynchronizedDescriptiveStatistics(); //record the time duration
    private final DescriptiveStatistics fitnessStats = new SynchronizedDescriptiveStatistics(); //record the fitness scores

///////////////////////////////////////////////////////
    private final DescriptiveStatistics avgFitnessStats = new SynchronizedDescriptiveStatistics();
    private final DescriptiveStatistics avgAConnected_Stats = new SynchronizedDescriptiveStatistics();
    private final DescriptiveStatistics avgBConnected_Stats = new SynchronizedDescriptiveStatistics();
    private final DescriptiveStatistics avgCConnected_Stats = new SynchronizedDescriptiveStatistics();

    private final DescriptiveStatistics normalisedAConnected_Stats = new SynchronizedDescriptiveStatistics();
    private final DescriptiveStatistics normalisedBConnected_Stats = new SynchronizedDescriptiveStatistics();
    private final DescriptiveStatistics normalisedCConnected_Stats = new SynchronizedDescriptiveStatistics();

    private final DescriptiveStatistics avgNormTotalConnected_Stats = new DescriptiveStatistics();

    private final DescriptiveStatistics avgNormAConnected_Stats = new SynchronizedDescriptiveStatistics();
    private final DescriptiveStatistics avgNormBConnected_Stats = new SynchronizedDescriptiveStatistics();
    private final DescriptiveStatistics avgNormCConnected_Stats = new SynchronizedDescriptiveStatistics();

    private final DescriptiveStatistics totalBlocksConnected_Stats = new SynchronizedDescriptiveStatistics();


////////////////////////////////////////////////////////////////////////



    //files to store the statistics regarding the number of different types of blocks that get connected
    private final DescriptiveStatistics numAConnected_Stats = new SynchronizedDescriptiveStatistics(); //avg number of blocks connected per simulation
    private final DescriptiveStatistics numBConnected_Stats = new SynchronizedDescriptiveStatistics();
    private final DescriptiveStatistics numCConnected_Stats = new SynchronizedDescriptiveStatistics();
    private final DescriptiveStatistics avgBlocksConnected_Stats = new SynchronizedDescriptiveStatistics(); //avg number of blocks connected per simulation
    private final DescriptiveStatistics normNumBlocksConnected_Stats = new SynchronizedDescriptiveStatistics(); //number of blocks connected each simulation / divided by total number of blocks available average over each simulation run
    //avg number of construction zones built per simulation run
    private final DescriptiveStatistics numConstructionZones_Stats = new SynchronizedDescriptiveStatistics();

    private Archive archive;

    //private ArrayList<NoveltyBehaviour> currentPopulation; //list to store the novelty functions and the aggregate behaviours of the current generation
    private int populationSize;
    private int numResults; //the number of results that will be produced for populationSize individuals being tested * numSimulationRuns (since one result per simulation) this is only for novelty
    private NoveltyBehaviour[] currentPopulation;
    private int currentBehaviour; //keep track of how many of the individuals in the generation have been processed

    /**
    need to set this from the main method in order to run the experiments
    */
    private boolean PerformingNoveltyCalcs = false;

    private SensorCollection sensorCollection;

    public ScoreCalculator(SimConfig simConfig, int simulationRuns,
            Morphology sensorMorphology, int populationSize, SensorCollection sensorCollection) {

        this.simConfig = simConfig;
        this.simulationRuns = simulationRuns;
        this.sensorMorphology = sensorMorphology;
        this.populationSize = populationSize;
        this.schemaConfigNum = this.simConfig.getConfigNumber();

        this.sensorCollection = sensorCollection;

        //there is only one ScoreCalculator that gets used
        //dont have to worry about different threads having different instances of the object
        //can maintain the archive from here
        this.archive = new Archive();
    }

    @Override
    public double calculateScore(MLMethod method) {

        // long start = System.nanoTime();
        //
        // NoveltyNetwork novNet = (NoveltyNetwork)method;
        // NoveltyBehaviour beh = novNet.getNoveltyBehaviour();
        //
        // if(beh == null) { //check if this behaviour has already been "processed"
        //     return 0;
        // }
        //
        // double noveltyScore = beh.getPopulationNoveltyScore();
        // double objectiveScore = beh.getObjectiveScore();
        // double hybridScore = (noveltyScore + objectiveScore) / 2;
        // if(noveltyScore == 0) {
        //     System.out.println("ScoreCalculator: soemthing weird heppened");
        // }
        // fitnessStats.addValue(hybridScore);
        //
        // AggregateBehaviour aggregateBehaviour = beh.getAggregateBehaviour();
        //
        // if(aggregateBehaviour == null) {
        //     System.out.println("ScoreCalculator: the error is still there");
        // }
        // numAConnected_Stats.addValue(aggregateBehaviour.getAvgABlocksConnected());
        // numBConnected_Stats.addValue(aggregateBehaviour.getAvgBBlocksConnected());
        // numCConnected_Stats.addValue(aggregateBehaviour.getAvgCBlocksConnected());
        // avgBlocksConnected_Stats.addValue(aggregateBehaviour.getAvgNumBlocksConnected());
        // normNumBlocksConnected_Stats.addValue(aggregateBehaviour.getNormalisedNumConnected());
        // numConstructionZones_Stats.addValue(aggregateBehaviour.getAvgNumConstructionZones());
        //
        // log.debug("HybridScore calculation completed: " + hybridScore);
        return 0;
    }

    public DescriptiveStatistics getPerformanceStatsFile() {
        return performanceStats;
    }

    public DescriptiveStatistics getFitnessStatsFile() {
        return fitnessStats;
    }

    public DescriptiveStatistics getConnectedAFile() {
        return numAConnected_Stats;
    }

    public DescriptiveStatistics getConnectedBFile() {
        return numBConnected_Stats;
    }

    public DescriptiveStatistics getConnectedCFile() {
        return numCConnected_Stats;
    }

    public DescriptiveStatistics getTotalBlocksConnectedFile() {
        return totalBlocksConnected_Stats;
    }

    public DescriptiveStatistics getNumConstructionZonesFile() {
        return numConstructionZones_Stats;
    }

    public DescriptiveStatistics getNormNumConnectedFile() {
        return normNumBlocksConnected_Stats;
    }

    public DescriptiveStatistics getAvgFitnessFile() {
        return avgFitnessStats;
    }

    public DescriptiveStatistics getAvgAConFile() {
        return avgAConnected_Stats;
    }

    public DescriptiveStatistics getAvgBConFile() {
        return avgBConnected_Stats;
    }

    public DescriptiveStatistics getAvgCConFile() {
        return avgCConnected_Stats;
    }

    public DescriptiveStatistics getTotalAvgCon() {
        return avgBlocksConnected_Stats;
    }

    public DescriptiveStatistics getAvgNormA() {
        return avgNormAConnected_Stats;
    }

    public DescriptiveStatistics getAvgNormB() {
        return avgNormBConnected_Stats;
    }

    public DescriptiveStatistics getAvgNormC() {
        return avgNormCConnected_Stats;
    }

    public DescriptiveStatistics getNormAConnectedFile() {
        return normalisedAConnected_Stats;
    }

    public DescriptiveStatistics getNormBConnectedFile() {
        return normalisedBConnected_Stats;
    }

    public DescriptiveStatistics getNormCConnectedFile() {
        return normalisedCConnected_Stats;
    }

    public DescriptiveStatistics getAvgNormTotalConnected() {
        return avgNormTotalConnected_Stats;
    }


    /*
    method to run the given network a certain number of times
    in the simulator in order to evaluate its average performance*/
    public void runEvaluation(MLMethod method) {

        NEATNetwork neat_network = null;
        RobotFactory robotFactory;

	    final StatsRecorder statsRecorder = new StatsRecorder(this); //this is basically where the simulation runs

        int[] bitMask = sensorCollection.generateRandomMask();

        //System.out.println("ScoreCalculator: PHENOTYPE for NEATNetwork: " + getPhenotypeForNetwork(neat_network));
        neat_network = (NEATNetwork) method;
        robotFactory = new HomogeneousRobotFactory(getPhenotypeForNetwork(neat_network, bitMask),
                    simConfig.getRobotMass(), simConfig.getRobotRadius(), simConfig.getRobotColour(),
                    simConfig.getObjectsRobots());

        // Create the simulation and run it
        //System.out.println("ScoreCalculator: creating the simulation and starting the GUI");
        // create new configurable resource factory
        String [] resQuantity = {"0","0","0"};
        ResourceFactory resourceFactory = new ConfigurableResourceFactory();
        resourceFactory.configure(simConfig.getResources(), resQuantity);

        Simulation simulation = new Simulation(simConfig, robotFactory, resourceFactory, false);
        simulation.setSchemaConfigNumber(schemaConfigNum);

        AggregateBehaviour aggregateBehaviour = new AggregateBehaviour(20);
        double avgFitness = 0;

        double avgNormA = 0;
        double avgNormB = 0;
        double avgNormC = 0;


	    for(int k = 0; k < 20; k++) {

		    Behaviour resultantBehaviour = simulation.runObjective();
        	int [] resTypeCount = simulation.getResTypeCount();
        	int totalResCount = resTypeCount[0] + resTypeCount[1] + resTypeCount[2];

            aggregateBehaviour.setTotalNumRes(totalResCount);
            aggregateBehaviour.addBehaviour(resultantBehaviour);

            ObjectiveFitness objectiveFitness = new ObjectiveFitness(schemaConfigNum, resTypeCount);
            double runFitness = objectiveFitness.calculate(resultantBehaviour);
            avgFitness += runFitness;

            fitnessStats.addValue(runFitness);

    		numAConnected_Stats.addValue(resultantBehaviour.getConnectedA());
    		numBConnected_Stats.addValue(resultantBehaviour.getConnectedB());
    		numCConnected_Stats.addValue(resultantBehaviour.getConnectedC());

            double normalisedA = resultantBehaviour.getConnectedA() / resTypeCount[0];

            double normalisedB = 0;
            if(resTypeCount[1] != 0) {
                normalisedB = resultantBehaviour.getConnectedB() / resTypeCount[1];
            }

            double normalisedC = 0;
            if(resTypeCount[2] != 0) {
                normalisedC = resultantBehaviour.getConnectedC() / resTypeCount[2];
            }

            avgNormA += normalisedA;
            avgNormB += normalisedB;
            avgNormC += normalisedC;

            normalisedAConnected_Stats.addValue(normalisedA);
            normalisedBConnected_Stats.addValue(normalisedB);
            normalisedCConnected_Stats.addValue(normalisedC);

            totalBlocksConnected_Stats.addValue(resultantBehaviour.getTotalConnected());
    		double resConnectedRatio = resultantBehaviour.getTotalConnected() / totalResCount;
    		normNumBlocksConnected_Stats.addValue(resConnectedRatio);

    		statsRecorder.recordIterationStats(k);
	    }

        aggregateBehaviour.finishRecording();

        avgNormA /= 20;
        avgNormB /= 20;
        avgNormC /= 20;
        avgFitness /= 20;

        avgFitnessStats.addValue(avgFitness);
        avgAConnected_Stats.addValue(aggregateBehaviour.getAvgABlocksConnected());
        avgBConnected_Stats.addValue(aggregateBehaviour.getAvgBBlocksConnected());
        avgCConnected_Stats.addValue(aggregateBehaviour.getAvgCBlocksConnected());
        avgBlocksConnected_Stats.addValue(aggregateBehaviour.getAvgNumBlocksConnected());
        avgNormAConnected_Stats.addValue(avgNormA);
        avgNormBConnected_Stats.addValue(avgNormB);
        avgNormCConnected_Stats.addValue(avgNormC);
        avgNormTotalConnected_Stats.addValue(aggregateBehaviour.getNormalisedNumConnected());

        statsRecorder.recordEvaluationStats();
    }

    public void demo(MLMethod method) {
        // Create the robot and resource factories

        NEATNetwork neat_network = null;
        BasicNetwork basic_network = null;
        RobotFactory robotFactory;

        int[] bitMask = sensorCollection.getIdealBitMask();

        neat_network = (NEATNetwork) method;
        robotFactory = new HomogeneousRobotFactory(getPhenotypeForNetwork(neat_network, bitMask),
                simConfig.getRobotMass(), simConfig.getRobotRadius(), simConfig.getRobotColour(),
                simConfig.getObjectsRobots());

        // Create the simulation and run it
        String [] resQuantity = {"0","0","0"};
        ResourceFactory resourceFactory = new ConfigurableResourceFactory();
        resourceFactory.configure(simConfig.getResources(), resQuantity);

        Simulation simulation = new Simulation(simConfig, robotFactory, resourceFactory, PerformingNoveltyCalcs);
        simulation.start();

        SimulationGUI video = new SimulationGUI(simulation);

        //new console which displays this simulation
        Console console = new Console(video);
        console.setVisible(true);
    }

    public void setPerformNovelty(boolean flag) {
      PerformingNoveltyCalcs = flag;
    }

    /*
    method to calculate the novelty of the individuals in the current population */
    public void calculateNoveltyForPopulation() {

        archive.calculatePopulationNovelty();
    }

    public void clearCurrentGeneration() {
        archive.clearGeneration();
    }

    public void printArchive() {

        // ArrayList<NoveltyBehaviour> archiveList = archive.getArchiveList();
        //
        // System.out.println("ScoreCalculator: the archive size is = " + archiveList.size());
    }

    public NoveltyBehaviour getNoveltyBehaviour(MLMethod method) {

        // try {
        //
        //     NEATNetwork neat_network = null;
        //     RobotFactory robotFactory;
        //
        //     //System.out.println("ScoreCalculator: PHENOTYPE for NEATNetwork: " + getPhenotypeForNetwork(neat_network));
        //     neat_network = (NEATNetwork) method;
        //     robotFactory = new HomogeneousRobotFactory(getPhenotypeForNetwork(neat_network),
        //                 simConfig.getRobotMass(), simConfig.getRobotRadius(), simConfig.getRobotColour(),
        //                 simConfig.getObjectsRobots());
        //
        //     // Create the simulation and run it
        //     //System.out.println("ScoreCalculator: creating the simulation and starting the GUI");
        //     // create new configurable resource factory
        //     String [] resQuantity = {"0","0","0"};
        //     ResourceFactory resourceFactory = new ConfigurableResourceFactory();
        //     resourceFactory.configure(simConfig.getResources(), resQuantity);
        //
        //     Simulation simulation = new Simulation(simConfig, robotFactory, resourceFactory, PerformingNoveltyCalcs);
        //     simulation.setSchemaConfigNumber(schemaConfigNum);
        //
        //     //creating an arraylist to store the novelty behaviours that are produced at the end of each simulation run
        //     //this is used to calculate the most novel behaviour of the produced runs
        //     //ArrayList<NoveltyBehaviour> simulationResults = new ArrayList<NoveltyBehaviour>();
        //
        //     ArrayList<NoveltyBehaviour> simulationResults = new ArrayList<NoveltyBehaviour>();
        //     AggregateBehaviour aggregateBehaviour = new AggregateBehaviour(simulationRuns);
        //
        //     double objectiveScore = 0;
        //
        //     for(int k = 0; k < simulationRuns; k++) {
        //
        //         NoveltyBehaviour resultantBehaviour = simulation.runNovel();
        //         simulationResults.add(resultantBehaviour);
        //
        //         int [] resTypeCount = simulation.getResTypeCount();
        //         int tempTotal = resTypeCount[0] + resTypeCount[1] + resTypeCount[2];
        //         aggregateBehaviour.setTotalNumRes(tempTotal);
        //         ObjectiveFitness objectiveFitness = new ObjectiveFitness(schemaConfigNum, simulation.getResTypeCount());
        //         Behaviour objectiveBeh = new Behaviour(resultantBehaviour.getConstructionTask(), schemaConfigNum);
        //         aggregateBehaviour.addBehaviour(objectiveBeh);
        //
        //         objectiveScore += objectiveFitness.calculate(objectiveBeh);
        //     }
        //
        //     aggregateBehaviour.finishRecording();
        //     objectiveScore = objectiveScore / simulationRuns;
        //
        //     NoveltyBehaviour[] resultsArray = new NoveltyBehaviour[simulationResults.size()];
        //     simulationResults.toArray(resultsArray);
        //
        //     //find and store the most novel behaviour produced in the various simulation runs
        //     NoveltyBehaviour finalNovelBehaviour = archive.calculateSimulationNovelty(resultsArray);
        //     finalNovelBehaviour.setObjectiveScore(objectiveScore);
        //     finalNovelBehaviour.setAggregateBehaviour(aggregateBehaviour);
        //
        //     return finalNovelBehaviour;
        //
        // }
        // catch(Exception e) {
        //     System.out.println("ScoreCalculator getNoveltyBehaviour method: SOMETHING WENT HORRIBLY WRONG");
        //     e.printStackTrace();
        //     return null;
        // }
        return null;
    }

    //HyperNEAT uses the NEATnetwork as well
    private Phenotype getPhenotypeForNetwork(NEATNetwork network, int[] bitMask) {
        //System.out.println("ScoreCalculator: network input = " + network.getInputCount());
        //System.out.println("ScoreCalculator: network output = " + network.getOutputCount());
        return new HyperNEATPhenotype(network, sensorMorphology, bitMask);
    }

    public boolean isEvolvingMorphology() {
        return false;
    }

    @Override
    public boolean shouldMinimize() {
        return false;
    }

    @Override
    public boolean requireSingleThreaded() {
        return false;
    }

}

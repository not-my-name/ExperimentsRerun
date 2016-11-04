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

import java.util.*;

/**
 * Test runner for the simulation.
 *
 * Created by jamie on 2014/09/09.
 */
public class ScoreCalculator implements CalculateScore {

    private static final Logger log = LoggerFactory.getLogger(ScoreCalculator.class);

    private final SimConfig simConfig;
    private final int simulationRuns;
    private final Morphology sensorMorphology;

    private int schemaConfigNum;

    private final DescriptiveStatistics performanceStats = new SynchronizedDescriptiveStatistics();
    private final DescriptiveStatistics scoreStats = new SynchronizedDescriptiveStatistics();
    //private final DescriptiveStatistics sensorStats;

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

    public ScoreCalculator(SimConfig simConfig, int simulationRuns,
            Morphology sensorMorphology, int populationSize, int schemaConfigNum) {

        this.simConfig = simConfig;
        this.simulationRuns = simulationRuns;
        this.sensorMorphology = sensorMorphology;
        this.populationSize = populationSize;

        //currentPopulation = new ArrayList<NoveltyBehaviour>();
        currentBehaviour = 0;
        numResults = populationSize * simulationRuns;
        currentPopulation = new NoveltyBehaviour[numResults];

        /**
        need to set this from the main method in order to run the experiments
        */
        //PerformingNoveltyCalcs = true;

        /**
        need to change the schema config to work with the variables so that the config number can change
        */
        this.schemaConfigNum = schemaConfigNum;

        //there is only one ScoreCalculator that gets used
        //dont have to worry about different threads having different instances of the object
        //can maintain the archive from here
        this.archive = new Archive();
    }

    @Override
    public double calculateScore(MLMethod method) {

        long start = System.nanoTime();

        /**
        all of the actual simulation runs and score calculations are performed in the preIteration methods of the NoveltySStrategy class
        when TrainEA calls iteration() and then calls .calculateScore() which should just return the score for the current behaviour
        */

        if(PerformingNoveltyCalcs) { //need to have a way of checking when performing objective or novelty

            NoveltyNetwork novNet = (NoveltyNetwork)method;
            NoveltyBehaviour beh = novNet.getNoveltyBehaviour();

            if(beh == null) { //check if this behaviour has already been "processed"
                return 0;
            }

            double noveltyScore = beh.getPopulationScore();
            scoreStats.addValue(noveltyScore);
            log.debug("NoveltyScore calculation completed: " + noveltyScore);
            return noveltyScore;
        }
        else { //FOR OBJECTIVE SEARCH

            NEATNetwork neat_network = null;
            RobotFactory robotFactory;

            neat_network = (NEATNetwork) method;
            robotFactory = new HomogeneousRobotFactory(getPhenotypeForNetwork(neat_network),
                        simConfig.getRobotMass(), simConfig.getRobotRadius(), simConfig.getRobotColour(),
                        simConfig.getObjectsRobots());

            // create new configurable resource factory
            String [] resQuantity = {"0","0","0"};
            ResourceFactory resourceFactory = new ConfigurableResourceFactory();
            resourceFactory.configure(simConfig.getResources(), resQuantity);

            Simulation simulation = new Simulation(simConfig, robotFactory, resourceFactory, PerformingNoveltyCalcs);
            simulation.setSchemaConfigNumber(schemaConfigNum);

            double fitness = 0;
            for(int i = 0; i < simulationRuns; i++) {

                ObjectiveFitness objectiveFitness = new ObjectiveFitness(schemaConfigNum);
                Behaviour resultantBehaviour = simulation.runObjective();
                double tempFitness = objectiveFitness.calculate(resultantBehaviour);
                fitness += tempFitness;
            }

            double score = fitness / simulationRuns;
            scoreStats.addValue(score);

            log.debug("Score calculation completed: " + score);

            long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            performanceStats.addValue(duration);

            return score;

        }

    }

    public void demo(MLMethod method) {
        // Create the robot and resource factories

        NEATNetwork neat_network = null;
        BasicNetwork basic_network = null;
        RobotFactory robotFactory;

        neat_network = (NEATNetwork) method;
        robotFactory = new HomogeneousRobotFactory(getPhenotypeForNetwork(neat_network),
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

    public void calculateNoveltyForPopulation() {
        //have access to the archive
        //in archive calculate novelty for each individual in the current generation

        archive.calculatePopulationNovelty();

        //method in the Archive:
        //iterate over all the behaviours in the current generation
        //calculate their behavioural sparseness for each one
    }

    public void clearCurrentGeneration() {
        archive.clearGeneration();
    }

    public NoveltyBehaviour getNoveltyBehaviour(MLMethod method) {

        NEATNetwork neat_network = null;
        RobotFactory robotFactory;

        //System.out.println("ScoreCalculator: PHENOTYPE for NEATNetwork: " + getPhenotypeForNetwork(neat_network));
        neat_network = (NEATNetwork) method;
        robotFactory = new HomogeneousRobotFactory(getPhenotypeForNetwork(neat_network),
                    simConfig.getRobotMass(), simConfig.getRobotRadius(), simConfig.getRobotColour(),
                    simConfig.getObjectsRobots());

        // Create the simulation and run it
        //System.out.println("ScoreCalculator: creating the simulation and starting the GUI");
        // create new configurable resource factory
        String [] resQuantity = {"0","0","0"};
        ResourceFactory resourceFactory = new ConfigurableResourceFactory();
        resourceFactory.configure(simConfig.getResources(), resQuantity);

        Simulation simulation = new Simulation(simConfig, robotFactory, resourceFactory, PerformingNoveltyCalcs);

        simulation.setSchemaConfigNumber(schemaConfigNum);

        //creating an arraylist to store the novelty behaviours that are produced at the end of each simulation run
        //this is used to calculate the most novel behaviour of the produced runs
        ArrayList<NoveltyBehaviour> simulationResults = new ArrayList<NoveltyBehaviour>();

        for(int k = 0; k < simulationRuns; k++) {

            //recording all the resultant behaviours that the network produced in the different simulation runs
            simulationResults.add(simulation.runNovel());
        }

        NoveltyBehaviour[] resultsArray = new NoveltyBehaviour[simulationResults.size()];
        simulationResults.toArray(resultsArray);

        // double index = archive.findMostNovel(resultsArra);
        // return resultsArray(index);
        //OR
        return archive.findMostNovel(resultsArray);
    }

    // public void setSchemaConfigNumber(int i) {
    //     schemaConfigNum = i;
    // }

    // public void setArchive(Archive archive) {
    //     this.archive = archive;
    // }

    //HyperNEAT uses the NEATnetwork as well
    private Phenotype getPhenotypeForNetwork(NEATNetwork network) {
        //System.out.println("ScoreCalculator: network input = " + network.getInputCount());
        //System.out.println("ScoreCalculator: network output = " + network.getOutputCount());
        return new HyperNEATPhenotype(network, sensorMorphology);
    }

    public boolean isEvolvingMorphology() {
        return false;
    }

    public DescriptiveStatistics getPerformanceStatistics() {
        return performanceStats;
    }

    public DescriptiveStatistics getScoreStatistics() {
        return scoreStats;
    }

    /*public DescriptiveStatistics getSensorStatistics() {
        return sensorStats;
    }*/

    @Override
    public boolean shouldMinimize() {
        return false;
    }

    @Override
    public boolean requireSingleThreaded() {
        return false;
    }

}

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
import java.util.Arrays;
import java.util.List;

import sim.display.Console;
import za.redbridge.simulator.Simulation;
import za.redbridge.simulator.SimulationGUI;
import za.redbridge.simulator.config.SimConfig;
import za.redbridge.simulator.factories.HomogeneousRobotFactory;
import za.redbridge.simulator.factories.RobotFactory;
import za.redbridge.simulator.factories.ConfigurableResourceFactory;
import za.redbridge.simulator.factories.ResourceFactory;
import za.redbridge.simulator.phenotype.*;
import za.redbridge.simulator.Morphology;
import za.redbridge.simulator.novelty.*;
import  za.redbrige.simulator.hybrid.HybridPhenotype;
import za.redbridge.simulator.NEAT.NEATPhenotype;
import za.redbridge.simulator.Main.SEARCH_MECHANISM;

import za.redbridge.simulator.SensorCollection;

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

    private SensorCollection sensorCollection;

    private final DescriptiveStatistics timeElapsedStats = new SynchronizedDescriptiveStatistics();
    private final DescriptiveStatistics numAdjacentStats = new SynchronizedDescriptiveStatistics();
    private final DescriptiveStatistics fitnessStats = new SynchronizedDescriptiveStatistics();
    private final DescriptiveStatistics normalizedNumAdjacentStats = new SynchronizedDescriptiveStatistics();
    private final DescriptiveStatistics numTypeAsConnectedStats = new SynchronizedDescriptiveStatistics();
    private final DescriptiveStatistics numTypeBsConnectedStats = new SynchronizedDescriptiveStatistics();
    private final DescriptiveStatistics numTypeCsConnectedStats = new SynchronizedDescriptiveStatistics();
    private final DescriptiveStatistics numCZsBuiltStats = new SynchronizedDescriptiveStatistics();
    private final DescriptiveStatistics sensorStats;
    private final double[] parameters;
    private double avePerformance;
    private final boolean isParamTuning;
    private int popCounter = 0;
    private final double nMinThreshold = 50D;  //the threshold that must be matched in order to be added to the archive
    private NoveltyArchive archive;
    private PhenotypeBehaviour latestPB;
    private final SEARCH_MECHANISM searchMechanism;
    private final int runNumber;

    public ScoreCalculator(SimConfig simConfig, int simulationRuns,
            Morphology sensorMorphology, double [] parameters, boolean isParamTuning, SEARCH_MECHANISM searchMechanism,
            int populationSize, int runNumber, SensorCollection sensorCollection) {
        this.simConfig = simConfig;
        this.simulationRuns = simulationRuns;
        this.sensorMorphology = sensorMorphology;

        this.sensorCollection = sensorCollection;

        // If fixed morphology then don't record sensor stats
        this.sensorStats =  null;
        this.parameters = parameters;
        this.isParamTuning = isParamTuning;
        this.searchMechanism = searchMechanism;
        if (searchMechanism == SEARCH_MECHANISM.NOVELTY || searchMechanism == SEARCH_MECHANISM.HYBRID) {
            archive = new NoveltyArchive(populationSize);
        }
        this.runNumber = runNumber;
    }

    //this is just a second constructor to be used by the parameter tuning classes to avoid having to change a whole bunch
    //of crap in all the classes that use the parameter tuning class
    public ScoreCalculator(SimConfig simConfig, int simulationRuns,
            Morphology sensorMorphology, double [] parameters, boolean isParamTuning, SEARCH_MECHANISM searchMechanism,
            int populationSize, int runNumber) {
        this.simConfig = simConfig;
        this.simulationRuns = simulationRuns;
        this.sensorMorphology = sensorMorphology;

        // If fixed morphology then don't record sensor stats
        this.sensorStats =  null;
        this.parameters = parameters;
        this.isParamTuning = isParamTuning;
        this.searchMechanism = searchMechanism;
        if (searchMechanism == SEARCH_MECHANISM.NOVELTY || searchMechanism == SEARCH_MECHANISM.HYBRID) {
            archive = new NoveltyArchive(populationSize);
        }
        this.runNumber = runNumber;
    }

    /**
    method will need to have acces to its respective PhenotypeBehaviour for this calculation
    **/
    @Override
    public double calculateScore(MLMethod method) {

        //System.out.println("ScoreCalculator (line 87): performing the score calculation");

        int[] idealMask = sensorCollection.getIdealBitMask();

        if (searchMechanism == SEARCH_MECHANISM.NOVELTY) {
            NoveltyPhenotype n = (NoveltyPhenotype)method;

            if (n.getPhenotypeBehaviour() == null) {
                // System.out.println("No PB");
                return 0D;
            }

            else {
                // System.out.println("Has PB");
                PhenotypeBehaviour pb = n.getPhenotypeBehaviour();  //get this phenotype's behaviour characterization
                // System.out.println("ABOUT TO CALCULATE SCORE");
                // System.out.println(archive.toStringCurrPop());
                double noveltyScore = archive.getNovelty(pb);


                fitnessStats.addValue(noveltyScore);

                log.debug("Novelty score calculation completed: " + noveltyScore);
                return noveltyScore;
            }

        }
        else if (searchMechanism == SEARCH_MECHANISM.HYBRID) {
            NoveltyPhenotype n = (NoveltyPhenotype)method;

            if (n.getPhenotypeBehaviour() == null) {
                // System.out.println("No PB");
                return 0D;
            }

            else {
                HybridPhenotype hpb = (HybridPhenotype)n.getPhenotypeBehaviour();  //get this phenotype's behaviour characterization
                // System.out.println("ABOUT TO CALCULATE SCORE");
                // System.out.println(archive.toStringCurrPop());
                double noveltyScore = archive.getNovelty(n.getPhenotypeBehaviour());

                double aveObjectiveFitnessScore = ((HybridPhenotype)hpb).getAveFitnessForPhenotype();

                System.out.println(aveObjectiveFitnessScore + " " + noveltyScore);

                double hybridScore = 0.5*aveObjectiveFitnessScore + 0.5*noveltyScore;

                fitnessStats.addValue(hybridScore);

                log.debug("Hybrid score calculation completed: " + hybridScore);
                return hybridScore;
            }
        }
        else {
            try {
                // System.out.println("Calculating Score");
                //System.out.println("ScoreCalculator (line 141): performing an objective calculation");
                long start = System.nanoTime();

                NEATNetwork neat_network = null;
                BasicNetwork basic_network = null;
                RobotFactory robotFactory;

                neat_network = (NEATNetwork) method;

                //Create the robots with the phenotype created by the NEATNetwork
                robotFactory = new HomogeneousRobotFactory(getPhenotypeForNetwork(neat_network, idealMask),
                        simConfig.getRobotMass(), simConfig.getRobotRadius(), simConfig.getRobotColour(),
                        simConfig.getObjectsRobots());

                // create new configurable resource factory
                String [] resQuantity = {"0","0","0"};
                ResourceFactory resourceFactory = new ConfigurableResourceFactory();
                resourceFactory.configure(simConfig.getResources(), resQuantity);

                Simulation simulation = new Simulation(simConfig, robotFactory, parameters, resourceFactory, searchMechanism);

                double fitness = 0D;
                double numAdjacent = 0D;
                double normalizedNumAdjacent = 0D;
                double numAs = 0D;
                double numBs = 0D;
                double numCs = 0D;
                double numCZsbuilt = 0D;

                for (int i = 0; i < simulationRuns; i++) {
                    simulation.run();

                    fitness += simulation.getFitness();
                    numAdjacent += simulation.getNumAdjacent();
                    normalizedNumAdjacent += simulation.getNumAdjacent()/simulation.getNumResources();
                    numAs += simulation.getNumAs();
                    numBs += simulation.getNumBs();
                    numCs += simulation.getNumCs();
                    numCZsbuilt += simulation.getNumCZsBuilt();
                }
                // Get the fitness and update the total score
                double score = fitness / simulationRuns;
                double aveNumAdjacent = numAdjacent / simulationRuns;
                double aveNormalizedAdj = normalizedNumAdjacent / simulationRuns;
                double aveNumAs = numAs / simulationRuns;
                double aveNumBs = numBs / simulationRuns;
                double aveNumCs = numCs / simulationRuns;
                double aveNumCZsBuilt = numCZsbuilt / simulationRuns;

                long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

                timeElapsedStats.addValue(duration);
                fitnessStats.addValue(score);
                numAdjacentStats.addValue(aveNumAdjacent);
                normalizedNumAdjacentStats.addValue(aveNormalizedAdj);
                numTypeAsConnectedStats.addValue(aveNumAs);
                numTypeBsConnectedStats.addValue(aveNumBs);
                numTypeCsConnectedStats.addValue(aveNumCs);
                numCZsBuiltStats.addValue(aveNumCZsBuilt);
                log.debug("Objective score calculation completed (run "+ runNumber + "): " + score + " " + aveNumAdjacent + " " + aveNormalizedAdj + " " + duration);

                return score;
            }
            catch (Exception e) {
                System.out.println("ERROR: " + e);
                return 0D;
            }
        }
    }

    public String checkPBPresence(PhenotypeBehaviour pb) {
        String result = "";
        result += archive.isInCurrPopArchive(pb) + " " + archive.isInArchive(pb);
        return result;
    }

    /**
    Method that compares each individual from the most recent generation with every other member of that generation + archive
    **/
    public void calculateNoveltyForPopulation() {
        // System.out.println("Generating population novelty");
        archive.calculatePopulationNovelty();
    }

    public double getUpdatedNoveltyScoreForPB(PhenotypeBehaviour pb) {
        // calculateNoveltyForPopulation();
        double noveltyScore = archive.getNovelty(pb);
        // fitnessStats.addValue(noveltyScore);
        // log.debug("noveltyScore calculation completed: " + noveltyScore);
        return noveltyScore;
    }

    // public PhenotypeBehaviour getPBForGenome(MLMethod method) {
    //     return archive.getPBForGenome(method);
    // }

    public String printPop() {
        // System.out.println("Printing curr Population:");
        return archive.toStringCurrPop();
    }

    public String printArchive() {
        return archive.toString();
    }

    public void addToPopulationBehaviours(PhenotypeBehaviour pb) {
        archive.addPhenotypeToCurrPopArchive(pb);
    }

    public void demo(MLMethod method) {
        // Create the robot and resource factories
        NEATNetwork neat_network = null;
        BasicNetwork basic_network = null;
        RobotFactory robotFactory;

        int[] idealMask = sensorCollection.getIdealBitMask();

        neat_network = (NEATNetwork) method;
        robotFactory = new HomogeneousRobotFactory(getPhenotypeForNetwork(neat_network, idealMask),
                simConfig.getRobotMass(), simConfig.getRobotRadius(), simConfig.getRobotColour(),
                simConfig.getObjectsRobots());


        // create new configurable resource factory
        String [] resQuantity = {"0","0","0"};
        ResourceFactory resourceFactory = new ConfigurableResourceFactory();
        resourceFactory.configure(simConfig.getResources(), resQuantity);

        Simulation simulation = new Simulation(simConfig, robotFactory, parameters, resourceFactory, searchMechanism);

        SimulationGUI video = new SimulationGUI(simulation);

        //new console which displays this simulation
        Console console = new Console(video);
        console.setVisible(true);
    }

    private Phenotype getPhenotypeForNetwork(NEATNetwork network, int[] bitMask) {
            return new NEATPhenotype(network, sensorMorphology, bitMask);
    }
    public boolean isEvolvingMorphology() {
        return false;
    }

    public DescriptiveStatistics getTimeElapsedStats() {
        return timeElapsedStats;
    }

    public DescriptiveStatistics getNumAdjacentStats() {
        return numAdjacentStats;
    }

    public DescriptiveStatistics getNormalizedNumAdjacentStats() {
        return normalizedNumAdjacentStats;
    }

    public DescriptiveStatistics getNumAsConnectedStats() {
        return numTypeAsConnectedStats;
    }

    public DescriptiveStatistics getNumBsConnectedStats() {
        return numTypeBsConnectedStats;
    }

    public DescriptiveStatistics getNumCsConnectedStats() {
        return numTypeCsConnectedStats;
    }

    public DescriptiveStatistics getFitnessStats() {
        return fitnessStats;
    }

    public DescriptiveStatistics getNumCZsBuiltStats () {
        return numCZsBuiltStats;
    }

    public DescriptiveStatistics getSensorStatistics() {
        return sensorStats;
    }

    @Override
    public boolean shouldMinimize() {
        return false;
    }

    @Override
    public boolean requireSingleThreaded() {
        return false;
    }

    public void clearCurrPop() {
        archive.clearPopulation();
    }

    public void clearCurrPop(List<PhenotypeBehaviour> currPop) {
        archive.clearPopulation(currPop);
    }

    public PhenotypeBehaviour getLatestPhenotype() {
        // System.out.println("latestPB = " + latestPB + " novelty = " + latestPB.getBehaviouralSparseness());
        return latestPB;
    }

    public PhenotypeBehaviour getPBForPhenotype(MLMethod method) {

        log.debug("Generating PhenotypeBehaviours");

        long start = System.nanoTime();

        NEATNetwork neat_network = null;
        BasicNetwork basic_network = null;
        RobotFactory robotFactory;

        neat_network = (NEATNetwork) method;

        int[] idealMask = sensorCollection.getIdealBitMask();

        //Create the robots with the phenotype created by the NEATNetwork
        robotFactory = new HomogeneousRobotFactory(getPhenotypeForNetwork(neat_network, idealMask),
                simConfig.getRobotMass(), simConfig.getRobotRadius(), simConfig.getRobotColour(),
                simConfig.getObjectsRobots());

        // create new configurable resource factory
        String [] resQuantity = {"0","0","0"};
        ResourceFactory resourceFactory = new ConfigurableResourceFactory();
        resourceFactory.configure(simConfig.getResources(), resQuantity);

        Simulation simulation = new Simulation(simConfig, robotFactory, parameters, resourceFactory, searchMechanism);

        PhenotypeBehaviour[] individualBehaviours = new PhenotypeBehaviour[simulationRuns];

        double performance = 0D;
        double objectiveFitness = 0D;
        double aveObjectiveFitnessScore = 0D;
        for (int i = 0; i < simulationRuns; i++) {
            simulation.run();

            if (searchMechanism == SEARCH_MECHANISM.HYBRID) {
                objectiveFitness += simulation.getFitness();
            }

            // if (isParamTuning) {
            performance += simulation.getNumAdjacent();
            // }
            individualBehaviours[i] = simulation.getSimBehaviour();  //add behaviour to indivual's beahvioural list

            // fitness += simulation.getFitness().getTeamFitness();
            // fitness += 20 * (1.0 - simulation.getProgressFraction()); // Time bonus
        }

        long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        timeElapsedStats.addValue(duration);

        //Get the most novel behaviour for this individual
        PhenotypeBehaviour mostNovel = PhenotypeBehaviour.getMostNovelForIndiv(individualBehaviours);

        if (searchMechanism == SEARCH_MECHANISM.HYBRID) {
            aveObjectiveFitnessScore = objectiveFitness / simulationRuns;
            HybridPhenotype hpb = new HybridPhenotype(mostNovel, aveObjectiveFitnessScore);
            archive.addPhenotypeToCurrPopArchive(hpb);
            return hpb;
        }
        else {
            archive.addPhenotypeToCurrPopArchive(mostNovel);
        }

        return mostNovel;
    }

}

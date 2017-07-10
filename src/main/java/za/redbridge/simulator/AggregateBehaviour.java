
package za.redbridge.simulator;

import za.redbridge.simulator.Behaviour;
import za.redbridge.simulator.config.SchemaConfig;

public class AggregateBehaviour{

    //variables to store the total number of different block types connected during all simulations
    private int totalAConnected;
    private int totalBConnected;
    private int totalCConnected;

    private int totalBlocksConnected; //the total number of blocks connected through all simulations
    private double avgNumBlocksConnected; //the average number of blocks connected for each simulation

    private int numSimulationRuns; //number of times the simulation gets run

    private double avgNumConstructionZones; //average number of construction zones created per simulation
    private int totalConstructionZones; //total number of construction zones in all simulations

    private double totalFitnessScore; //summing the resultant fitness at the end of each simulation
    private double avgFitness; //the avg fitness scored per simulation

    //the average number of times each block was connected across all the simulation runs`
    private double avgAConnected;
    private double avgBConnected;
    private double avgCConnected;

    private double normalisedNumConnected;

    private int totalNumBlocksInEnvironment;

    public AggregateBehaviour(int numSimulationRuns) {

        this.numSimulationRuns = numSimulationRuns;

        totalAConnected = 0;
        totalBConnected = 0;
        totalCConnected = 0;
        avgAConnected = 0;
        avgBConnected = 0;
        avgCConnected = 0;
        totalBlocksConnected = 0;
        totalConstructionZones = 0;
        avgNumConstructionZones = 0;
        totalFitnessScore = 0;
        avgFitness = 0;
        normalisedNumConnected = 0;
        totalNumBlocksInEnvironment = 0;
    }

    /*
    method to add anew behaviour to the aggregate collection
    and recalculate the average scores */
    public void addBehaviour(Behaviour newBehaviour) {

        totalAConnected += newBehaviour.getConnectedA();
        totalBConnected += newBehaviour.getConnectedB();
        totalCConnected += newBehaviour.getConnectedC();

        totalBlocksConnected += totalAConnected;
        totalBlocksConnected += totalBConnected;
        totalBlocksConnected += totalCConnected;

        totalConstructionZones += newBehaviour.getNumConstructionZones();

        normalisedNumConnected += (totalBlocksConnected / totalNumBlocksInEnvironment);
    }

    public void setTotalNumRes(int totalNumRes) {
        totalNumBlocksInEnvironment = totalNumRes;
    }

    /*
    method that gets called once all the simulation runs have been done
    it is used to calculate all the average values and write
    the details to the specified file and folder */
    public void finishRecording() {

        avgAConnected = totalAConnected / numSimulationRuns;
        avgBConnected = totalBConnected / numSimulationRuns;
        avgCConnected = totalCConnected / numSimulationRuns;

        avgNumBlocksConnected = totalBlocksConnected / numSimulationRuns;
        avgNumConstructionZones = totalConstructionZones / numSimulationRuns;

        avgFitness = totalFitnessScore / numSimulationRuns;

        normalisedNumConnected = normalisedNumConnected / numSimulationRuns; //gives avg ration of connected to unconnected blocks per simulation
    }

    public int getTotalAConnected() {
        return totalAConnected;
    }

    public int getTotalBConnected() {
        return totalBConnected;
    }

    public int getTotalCConnected() {
        return totalCConnected;
    }

    public double getAvgABlocksConnected() {
        return avgAConnected;
    }

    public double getAvgBBlocksConnected() {
        return avgBConnected;
    }

    public double getAvgCBlocksConnected() {
        return avgCConnected;
    }

    public int getTotalBlocksConnected() {
        return totalBlocksConnected;
    }

    public double getAvgNumBlocksConnected() {
        //avg number of blocks connected per simulation
        return avgNumBlocksConnected;
    }

    public int getTotalNumConstructionZones() {
        return totalConstructionZones;
    }

    public double getAvgNumConstructionZones() {
        return avgNumConstructionZones;
    }

    public double getAvgFitnessScore() {
        return avgFitness;
    }

    public double getTotalFitness() {
        return totalFitnessScore;
    }

    public double getNormalisedNumConnected() {
        return normalisedNumConnected;
    }
}

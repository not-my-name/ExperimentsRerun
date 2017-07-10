package za.redbridge.simulator;

import org.encog.ml.data.MLData;
import org.encog.ml.data.basic.BasicMLData;
import org.encog.neural.neat.NEATNetwork;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import sim.util.Double2D;
//import za.redbridge.experiment.NEATM.sensor.SensorMorphology;
import za.redbridge.simulator.phenotype.Phenotype;
import za.redbridge.simulator.sensor.AgentSensor;

import za.redbridge.simulator.NEATPhenotype;

public class HyperNEATPhenotype implements Phenotype {

	private final NEATNetwork network;
    private final Morphology morphology;

    private final MLData input;
    private final List<AgentSensor> sensors;

    public HyperNEATPhenotype(NEATNetwork network, Morphology morphology) { //need to change the SensorMorpholgy to just Morphology that Daniel made
        this.network = network;
        this.morphology = morphology;

        //System.out.println("HyperNEATPhenotype: network input = " + network.getInputCount());
        //System.out.println("HyperNEATPhenotype: network output = " + network.getOutputCount());

        // Initialise sensors
        final int numSensors = morphology.getNumSensors();
        sensors = new ArrayList<>(numSensors);
        for (int i = 0; i < numSensors; i++) {
            sensors.add(morphology.getSensorList().get(i)); //reading in the sensors from the current assigned morphology 
        }                                         //remember that there is a different collection of sensors for each experiment                  

        input = new BasicMLData(numSensors);
        //System.out.println("HyperNEATPhenotype: number sensors = " + numSensors);
    }

    @Override
    public List<AgentSensor> getSensors() {
        return sensors;
    }

    @Override
    public Double2D step(List<List<Double>> sensorReadings) {
        final MLData input = this.input;
        for (int i = 0, n = input.size(); i < n; i++) {
            input.setData(i, sensorReadings.get(i).get(0)); //assigning the sensor inputs to the input nodes
        }

        //System.out.println("HyperNEATPhenotype: size of input = " + input.size());
        //System.out.println("HyperNEATPhenotype: size of network input = " + network.getInputCount());

        MLData output = network.compute(input); //sending the inputs from the robot sensors to the network

        //System.out.println("HyperNEATPhenotype step method: printing the output for the network  " + new Double2D(output.getData(0) * 2.0 - 1.0, output.getData(1) * 2.0 - 1.0));

        return new Double2D(output.getData(0) * 2.0 - 1.0, output.getData(1) * 2.0 - 1.0);
    }

    @Override
    public Phenotype clone() {
        return new HyperNEATPhenotype(network, this.morphology.clone());
    }

    @Override
    public void configure(Map<String, Object> stringObjectMap) {
        throw new UnsupportedOperationException();
    }

}
package za.redbridge.simulator.NEAT;

import org.encog.ml.data.MLData;
import org.encog.ml.data.basic.BasicMLData;
import org.encog.neural.neat.NEATNetwork;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import sim.util.Double2D;
import za.redbridge.simulator.phenotype.Phenotype;
import za.redbridge.simulator.sensor.AgentSensor;
import za.redbridge.simulator.Morphology;

public class NEATPhenotype implements Phenotype {

	private final NEATNetwork network;
	private final Morphology morphology;

	private final MLData input;
	private final List<AgentSensor> sensors;

	private final int[] sensorBitMask;

	/**
	Constructs an experiment phenotype (ANN + morphology)
	@param NEATNetwork network: NEAT's phenotype, the ANN for the robot's controller
	@param Morphology morphology: the morphology that each robot will take on
	**/
	public NEATPhenotype (NEATNetwork network, Morphology morphology, int[] sensorBitMask) {

		this.network = network;
		this.morphology = morphology;

		this.sensorBitMask = sensorBitMask;

		// Initialise sensors
		final int numSensors = morphology.getNumSensors();
		sensors = new ArrayList<>(numSensors);
		for (int i = 0; i < numSensors; i++) {
		    sensors.add(morphology.getSensor(i));
		}

		//Construct this object with blank data and a specified size.
		input = new BasicMLData(numSensors);
	}

	public String bitMaskToString() {

		String finalReturnString = "[";

		for(int k = 0; k < sensorBitMask.length; k++) {
			finalReturnString += Integer.toString(sensorBitMask[k]) + " | ";
		}

		return finalReturnString;
	}

	public NEATNetwork getNetwork () {
		return network;
	}

	public Morphology getMorphology () {
		//System.out.println("NEATPhenotype (line 49): something is calling to get the morphology object");
		return morphology;
	}

	@Override
	public List<AgentSensor> getSensors() {
		//System.out.println("NEATPhenotype (line 55): something is calling to get the sensors in the morph");
	    return sensors;
	}

	@Override
	public Double2D step(List<List<Double>> sensorReadings) {

		//System.out.println("NEATPhenotype (line 61): printing out the sensor readings");
		//System.out.println(sensorReadings);

	    final MLData input = this.input;
	    for (int i = 0, n = input.size(); i < n; i++) {

			if(sensorBitMask[i] == 0) { //if the current sensor has been disabled
				input.setData(i, 0);
			}
			else {
				input.setData(i, sensorReadings.get(i).get(0));
			}
	    }

	    MLData output = network.compute(input);
	    return new Double2D(output.getData(0) * 2.0 - 1.0, output.getData(1) * 2.0 - 1.0);
	}

	@Override
	public Phenotype clone() { //this is where the phenotype is cloned in order to produce the team of homogeneous robots
	    return new NEATPhenotype(network, this.morphology.clone(), this.sensorBitMask);
	}

	@Override
	public void configure(Map<String, Object> stringObjectMap) {
	    throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		String toPrint = "Experiment phenotype: \n";
		for (AgentSensor a : sensors) {
			toPrint += "\t " + a.toString();
		}
		return toPrint;
	}
}

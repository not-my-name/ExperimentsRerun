package za.redbridge.simulator.novelty;

import org.encog.neural.neat.NEATNetwork;
import org.encog.engine.network.activation.ActivationFunction;
import org.encog.ml.MLError;
import org.encog.ml.MLRegression;
import org.encog.ml.data.MLData;
import org.encog.ml.data.MLDataSet;
import org.encog.ml.data.basic.BasicMLData;
import org.encog.util.EngineArray;
import org.encog.util.simple.EncogUtility;
import org.encog.neural.neat.NEATLink;
import org.encog.ml.MLMethod;

import java.util.List;
import java.util.LinkedList;

public class NoveltyPhenotype extends NEATNetwork {

	private transient PhenotypeBehaviour pb;

	public NoveltyPhenotype(final int inputNeuronCount, final int outputNeuronCount,
			final List<NEATLink> connectionArray,
			final ActivationFunction[] theActivationFunctions)  {
		super(inputNeuronCount, outputNeuronCount, connectionArray, theActivationFunctions);
	}

	public void setPhenotypeBehaviour(PhenotypeBehaviour pb) {
		this.pb = pb;
	}

	public PhenotypeBehaviour getPhenotypeBehaviour() {
		return pb;
	}
}
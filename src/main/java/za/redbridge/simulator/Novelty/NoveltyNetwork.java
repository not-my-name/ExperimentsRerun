
package za.redbridge.simulator;

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

import za.redbridge.simulator.NoveltyBehaviour;

/*
this is another one of the files that had to be extended for the generational novelty thing
*/

public class NoveltyNetwork extends NEATNetwork {

	private transient NoveltyBehaviour novBeh;

	public NoveltyNetwork(final int inputNeuronCount, final int outputNeuronCount,
			final List<NEATLink> connectionArray,
			final ActivationFunction[] theActivationFunctions)  {
		super(inputNeuronCount, outputNeuronCount, connectionArray, theActivationFunctions);
	}

	public void setNoveltyBehaviour(NoveltyBehaviour novBeh) {
		this.novBeh = novBeh;
	}

	public NoveltyBehaviour getNoveltyBehaviour() {
		return novBeh;
	}
}

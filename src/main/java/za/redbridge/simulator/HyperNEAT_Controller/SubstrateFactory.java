package za.redbridge.simulator;

import org.encog.neural.hyperneat.substrate.Substrate;
import org.encog.neural.hyperneat.substrate.SubstrateNode;

public class SubstrateFactory{
	
	public static Substrate createSubstrate(int numInputs, int numOutputs) {

		Substrate result = new Substrate(2);

		for(int k = 0; k < numInputs; k++) {
			SubstrateNode inputNode = result.createInputNode();
		}

		for(int k = 0; k < numOutputs; k++) {
			SubstrateNode outputNode = result.createOutputNode();
			for(SubstrateNode inputNode : result.getInputNodes()) {
				result.createLink(inputNode, outputNode);
			}
		}

		return result;

	} 
}
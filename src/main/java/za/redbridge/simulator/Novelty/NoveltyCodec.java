package za.redbridge.simulator;

/*
this is one of the classes that had to be extended in order to get the Novelty comparisons
working between generations
*/

import org.encog.neural.hyperneat.HyperNEATCODEC;
import org.encog.ml.MLMethod;
import org.encog.ml.ea.genome.Genome;
import org.encog.neural.neat.NEATNetwork;
import org.encog.neural.neat.NEATLink;
import org.encog.ml.CalculateScore;

import za.redbridge.simulator.ScoreCalculator;

import java.util.List;
import java.util.LinkedList;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;

public class NoveltyCodec extends HyperNEATCODEC {

	private Map<Genome,NoveltyBehaviour> genomePBMap;
	private Map<Genome,NoveltyNetwork> genomeNNMap;
	private ScoreCalculator scoreCalculator;
	private int currGenNumber = 0;

	public NoveltyCodec(CalculateScore scoreCalculator) {
		this.scoreCalculator = (ScoreCalculator)scoreCalculator;
		genomePBMap = new HashMap<>();
		genomeNNMap = new HashMap<>();
	}

	@Override
	public MLMethod decode(final Genome genome) {

		NoveltyNetwork novNetwork;

		/*
		this method gets called first from the NoveltySearchStrategy in order to generate the current population
		so that they can be evaluated at the same time

		this method then gets called from the iteration method in the trainer

		*/

		if( genomePBMap.containsKey(genome) ) { //check if the genome has already been decoded before during the current generation

			novNetwork = genomeNNMap.get(genome); //fetches the existing network for the current genome
			novNetwork.setNoveltyBehaviour(genomePBMap.get(genome)); //places the existing behaviour for the genome (associates it with respective network)
		}
		else { //if this is the first time the genome comes through

			//decode the genome to get the resultant network that then needs to be tested
			NEATNetwork decoded = (NEATNetwork)super.decode(genome);

			if(decoded == null) {
				return null;
			}

				List<NEATLink> connectionArray = new LinkedList<>();
				NEATLink[] connections = decoded.getLinks();

				for (int i = 0; i < connections.length; i++) {
					connectionArray.add(connections[i]);
				}

				//sets up the network class
				novNetwork = new NoveltyNetwork(decoded.getInputCount(), decoded.getOutputCount(), connectionArray, decoded.getActivationFunctions());
				genomePBMap.put(genome, scoreCalculator.getNoveltyBehaviour(novNetwork)); //run the network in the simulation to establish the resultant behaviour
				genomeNNMap.put(genome, novNetwork);
		}

		return novNetwork;
	}

	public void clearMaps(LinkedList<Genome> persisted) {
		//clear the maps
		//delete all the entries that are not found in the persisted list

		genomePBMap.clear();
		genomeNNMap.clear();
		scoreCalculator.clearCurrentGeneration();
	}

	//clears all the maps except for the genomes that get passed through (currPopGenomes)
	//all the genomes that survive to pass to the next generation
	public void clearCurrPop(List<Genome> currPopGenomes) {

		List<NoveltyBehaviour> currPopPBs = new LinkedList<>();
		Map<Genome,NoveltyNetwork> phenotypesToBeKept = new HashMap<>();

		for (Genome g : currPopGenomes) {

			NoveltyBehaviour pb = genomePBMap.get(g);
			NoveltyNetwork phen = genomeNNMap.get(g);
			phen.setNoveltyBehaviour(null);  //clears the associated novelty behaviour
			currPopPBs.add(pb);
			phenotypesToBeKept.put(g,phen);
		}

		//Clear current maps
		genomePBMap.clear();
		genomeNNMap.clear();

		//Add the new generation
		genomeNNMap.putAll(phenotypesToBeKept);
	}

}

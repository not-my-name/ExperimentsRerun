package za.redbridge.simulator.novelty;

import org.encog.neural.neat.NEATCODEC;
import org.encog.ml.MLMethod;
import org.encog.ml.ea.genome.Genome;
import org.encog.neural.neat.NEATNetwork;
import org.encog.neural.neat.NEATLink;
import org.encog.ml.CalculateScore;

import za.redbridge.simulator.ScoreCalculator;
import za.redbridge.simulator.Main.SEARCH_MECHANISM;
import za.redbrige.simulator.hybrid.HybridPhenotype;

import java.util.List;
import java.util.LinkedList;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

public class NoveltyPhenotypeCODEC extends NEATCODEC {
	private Map<Genome,NoveltyPhenotype> genomePMap;
	private Map<Genome,PhenotypeBehaviour> genomePBMap;
	private ScoreCalculator calculateScore;
	private int currGenNumber = 0;
	private final SEARCH_MECHANISM sm;

	public NoveltyPhenotypeCODEC(CalculateScore calculateScore, SEARCH_MECHANISM sm) {
		this.calculateScore = (ScoreCalculator)calculateScore;
		genomePMap = new HashMap<>();
		genomePBMap = new HashMap<>();
		this.sm = sm;
	}

	@Override
	public MLMethod decode(final Genome genome) {

		NoveltyPhenotype noveltyP;

		if (sm == SEARCH_MECHANISM.NOVELTY) {
			if (genomePBMap.containsKey(genome)) {
				// System.out.println("\tGENOME has passed through already: " + genome);
				noveltyP = genomePMap.get(genome); //fetches the already built network for this genome

				noveltyP.setPhenotypeBehaviour(genomePBMap.get(genome));  //places the corresponding behaviour characterisation for this network

				// System.out.println("YES!!");

				// NEATNetwork decoded = (NEATNetwork)super.decode(genome);
				// List<NEATLink> connectionArray = new LinkedList<>();
				// NEATLink[] connections = decoded.getLinks();
				// for (int i = 0; i < connections.length; i++) {
				// 	connectionArray.add(connections[i]);
				// }
				// noveltyP = new NoveltyPhenotype(decoded.getInputCount(), decoded.getOutputCount(), connectionArray, decoded.getActivationFunctions());

				// noveltyP.setPhenotypeBehaviour(genomePMap.get(genome));
			}
			else {
				// System.out.println("\tGenome decoded for first time: " + genome);

				/**
				Generate our network (phenotype)
				**/	
				NEATNetwork decoded = (NEATNetwork)super.decode(genome);
				List<NEATLink> connectionArray = new LinkedList<>();
				NEATLink[] connections = decoded.getLinks();
				for (int i = 0; i < connections.length; i++) {
					connectionArray.add(connections[i]);
				}

				noveltyP = new NoveltyPhenotype(decoded.getInputCount(), decoded.getOutputCount(), connectionArray, decoded.getActivationFunctions());

				genomePBMap.put(genome, calculateScore.getPBForPhenotype(noveltyP));  //generate behaviour characterisation for this network

				// noveltyP.setPhenotypeBehaviour(calculateScore.getPBForPhenotype(noveltyP));
				
				genomePMap.put(genome, noveltyP);
				// System.out.println("Passing through CODEC: " + noveltyP);
			}
			
		}
		else {
			if (genomePBMap.containsKey(genome)) {
				// System.out.println("\tGENOME has passed through already: " + genome);
				noveltyP = genomePMap.get(genome); //fetches the already built network for this genome

				noveltyP.setPhenotypeBehaviour((HybridPhenotype)genomePBMap.get(genome));  //places the corresponding behaviour characterisation for this network

				// System.out.println("YES!!");

				// NEATNetwork decoded = (NEATNetwork)super.decode(genome);
				// List<NEATLink> connectionArray = new LinkedList<>();
				// NEATLink[] connections = decoded.getLinks();
				// for (int i = 0; i < connections.length; i++) {
				// 	connectionArray.add(connections[i]);
				// }
				// noveltyP = new NoveltyPhenotype(decoded.getInputCount(), decoded.getOutputCount(), connectionArray, decoded.getActivationFunctions());

				// noveltyP.setPhenotypeBehaviour(genomePMap.get(genome));
			}
			else {
				// System.out.println("\tGenome decoded for first time: " + genome);

				/**
				Generate our network (phenotype)
				**/	
				NEATNetwork decoded = (NEATNetwork)super.decode(genome);
				List<NEATLink> connectionArray = new LinkedList<>();
				NEATLink[] connections = decoded.getLinks();
				for (int i = 0; i < connections.length; i++) {
					connectionArray.add(connections[i]);
				}

				noveltyP = new NoveltyPhenotype(decoded.getInputCount(), decoded.getOutputCount(), connectionArray, decoded.getActivationFunctions());

				genomePBMap.put(genome, calculateScore.getPBForPhenotype(noveltyP));  //generate behaviour characterisation for this network

				// noveltyP.setPhenotypeBehaviour(calculateScore.getPBForPhenotype(noveltyP));
				
				genomePMap.put(genome, noveltyP);
				// System.out.println("Passing through CODEC: " + noveltyP);
			}
			
		}

		return noveltyP;
	}

	public void clearCurrPop() {
		genomePBMap.clear();
		genomePMap.clear();
		calculateScore.clearCurrPop();
	}

	// public void clearCurrPop(int generationCutoff) {
	// 	Map<Genome,NoveltyPhenotype> phenotypesToBeKept = new HashMap<>();
	// 	for (Genome g : genomePMap.keySet()) {
	// 		NoveltyPhenotype phen = genomePMap.get(g);

	// 		phenotypesToBeKept.put(g,phen);
	// 	}

	// 	//Clear current maps
	// 	genomePBMap.clear();
	// 	genomePMap.clear();

	// 	//Add the new generation
	// 	genomePMap.putAll(phenotypesToBeKept);

	// 	calculateScore.clearCurrPop();
	// }

	/**
	Clears mappings between genome and PBs as well as genome -> Networks (except for the surviving genomes passed through in currPopGenomes)
	@param currPopGenomes the genomes that survived into the new population (usually the best genome from the previous generation)
	**/
	public void clearCurrPop(List<Genome> currPopGenomes) {
		List<PhenotypeBehaviour> currPopPBs = new LinkedList<>();
		Map<Genome,NoveltyPhenotype> phenotypesToBeKept = new HashMap<>();
		for (Genome g : currPopGenomes) {
			PhenotypeBehaviour pb = genomePBMap.get(g);
			NoveltyPhenotype phen = genomePMap.get(g);
			phen.setPhenotypeBehaviour(null);  //clears the associated PB
			currPopPBs.add(pb);

			phenotypesToBeKept.put(g,phen);
		}

		//Clear current maps
		genomePBMap.clear();
		genomePMap.clear();

		//Add the new generation
		genomePMap.putAll(phenotypesToBeKept);
	}

	public void clearMap () {
		genomePMap.clear();
	}

	public String getCODECHistorySize() {
		return genomePMap.size() + " " + genomePBMap.size();
	}

	// public boolean isInList(Genome g) {
	// 	return useList.contains(g);
	// }

	public void getCurrEntries() {
		// calculateScore.calculateNoveltyForPopulation();
		for (Map.Entry<Genome, NoveltyPhenotype> entry : genomePMap.entrySet()) {
		    Genome g = entry.getKey();
		    PhenotypeBehaviour pb = entry.getValue().getPhenotypeBehaviour();
		    System.out.println("\t" + g + " " + pb + " " + calculateScore.getUpdatedNoveltyScoreForPB(pb));
		    // g.setScore(calculateScore.getUpdatedNoveltyScoreForPB(pb));
		}
	}

	// public void updateGenerationScores() {
	// 	calculateScore.calculateNoveltyForPopulation();
	// 	for (Map.Entry<Genome, PhenotypeBehaviour> entry : genomePMap.entrySet()) {
	// 	    Genome g = entry.getKey();
	// 	    PhenotypeBehaviour pb = entry.getValue();
	// 	    // System.out.println(g + " " + pb + " " + calculateScore.getUpdatedNoveltyScoreForPB(pb));
	// 	    g.setScore(calculateScore.getUpdatedNoveltyScoreForPB(pb));
	// 	}
	// }
}
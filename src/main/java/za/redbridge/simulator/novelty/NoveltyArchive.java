package za.redbridge.simulator.novelty;

import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Queue;
import java.util.PriorityQueue;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

import org.encog.ml.MLMethod;
import org.encog.ml.ea.genome.Genome;

public class NoveltyArchive {

	private final List<PhenotypeBehaviour> archive;  //the history of novel individuals
	private List<PhenotypeBehaviour> currPopulationArchive;  //the archive of individuals from a current generation
	private final double minThreshold = 200D;  //The minimum novelty required to be placed in the archive
	private double mostNovelScore = 0D;  //normalization for the archive
	private double mostNovelCurrScore = 0D;  //normalization for the current population archive

	public NoveltyArchive(int popSize) {
		archive = new LinkedList<>();
		currPopulationArchive = new LinkedList<>();
	}

	/**
	Add a phenotype to the current population's archive
	@param pb the behaviour characterization of the individual to be added
	**/
	public void addPhenotypeToCurrPopArchive(PhenotypeBehaviour pb) {
		// System.out.println("Adding to currPop");
		currPopulationArchive.add(pb);
	}
	
	/**
	Add a phenotype to the archive only if it is novel enough
	@param pb the behaviour characterization of the individual to be added
	@param novelty the novelty score (sparseness) of the individual
	@return true if novel enough
			false if not novel enough
	**/
	public boolean addPhenotypeToArchive(PhenotypeBehaviour pb, double novelty) {
		// System.out.println("Adding to archive");
		if (novelty > minThreshold) {
			if (novelty > mostNovelScore) {
				mostNovelScore = novelty;
			}
			archive.add(pb);
			// System.out.println("Adding " + pb + " to archive");
			return true;
		}
		else {
			return false;
		}
	}

	public double getNovelty(PhenotypeBehaviour pb) {
		
		if (archive.contains(pb)) {
			// System.out.println("IN ARCHIVE");
			return pb.getBehaviouralSparseness();
		}
		/**
		DECISION: to calculate novelty individually => calculate novelty in terms of pop and archive here or
				  to calculate novelty for population in one go (from somewhere else) and then just return the already calculated novelty
		**/
		//OPTION 1
		else if (currPopulationArchive.contains(pb)) {
			// System.out.println("In curr POP");
			// return pb.getBehaviouralSparseness()/mostNovelCurrScore;
			return pb.getBehaviouralSparseness();
		}
		//OPTION 2
		// else if (currPopulationArchive.contains(pb)) {
		// 	// System.out.println("Calculating novelty NOW");
		// 	for (PhenotypeBehaviour otherPB : currPopulationArchive) {
		// 		if (pb != otherPB) {
		// 			pb.noveltyDistance(otherPB);
		// 		}
		// 	}
		// 	compareToArchive(pb);
		// 	if (pb.getBehaviouralSparseness() > mostNovelCurrScore) {
		// 		mostNovelCurrScore = pb.getBehaviouralSparseness();
		// 	}
		// 	return pb.getBehaviouralSparseness();
		// }
		else {
			// System.out.println("Sorry, PB not found for " + pb);
			return 0;
		}
	}

	/**
	Compare a behaviour characterization to the individuals in the archive
	@param pb the behaviour to be compared
	@return the normalized novelty score w.r.t the archive
	**/
	public double compareToArchive(PhenotypeBehaviour pb) {
		// Iterator I = archive.iterator();
		// //loop through all individuals and compare with pb.
		// while (I.hasNext()) {
			// PhenotypeBehaviour archiveIndividual = I.next();
			// pb.noveltyDistance(archiveIndividual); //this method also populates the k nearest neighbours for pb
		// }
		// System.out.println("Comparing to archive");
		if (archive.size() > 0) {
			// pb.clearNearestNeighbours();
			for (PhenotypeBehaviour p : archive) {
				pb.noveltyDistance(p); //this method also populates the k nearest neighbours for pb	
			}
			if (addPhenotypeToArchive(pb, pb.getBehaviouralSparseness())) {
				// System.out.println("Novel enough. Added to archive " + pb.getBehaviouralSparseness());
			}
			else {
				// System.out.println("Not novel enough.. " + pb.getBehaviouralSparseness());
			}
		}

		else {
			if (addPhenotypeToArchive(pb, pb.getBehaviouralSparseness())) {
				// System.out.println("Novel enough. First individual added to archive " + pb.getBehaviouralSparseness());
			}
			else {
				// System.out.println("Not novel enough.. " + pb.getBehaviouralSparseness());
			}
			
		}
		return pb.getBehaviouralSparseness();
	}

	public boolean isInCurrPopArchive(PhenotypeBehaviour pb) {
		return currPopulationArchive.contains(pb);
	}

	public boolean isInArchive(PhenotypeBehaviour pb) {
		return archive.contains(pb);
	}

	public void calculatePopulationNovelty() {
		// System.out.println("calculating current pop novelty:");
		for (PhenotypeBehaviour pb : currPopulationArchive) {
			// System.out.println("\t" + pb);
			for (PhenotypeBehaviour otherPB : currPopulationArchive) {
				if (pb != otherPB) {
					pb.noveltyDistance(otherPB);
				}
			}
			compareToArchive(pb);
			if (pb.getBehaviouralSparseness() > mostNovelCurrScore) {
				mostNovelCurrScore = pb.getBehaviouralSparseness();
			}
		}
	}

	public void clearPopulation() {
		currPopulationArchive.clear();
		mostNovelCurrScore = 0D;
	}

	public void clearPopulation(List<PhenotypeBehaviour> currPop) {
		// System.out.println("Clearing current population");
		currPopulationArchive.clear();
		for (PhenotypeBehaviour pb : currPop) {
			currPopulationArchive.add(pb);
		}
		mostNovelCurrScore = 0D;
	}

	public String toStringCurrPop () {
		String result = "";
		for (PhenotypeBehaviour pb : currPopulationArchive) {
			result += pb + ": " + pb.getBehaviouralSparseness() + " \n";
		}

		return result;
	}

	public String toString () {
		String result = "";
		for (PhenotypeBehaviour p : archive) {
			result += p.getBehaviouralSparseness()/mostNovelScore + "\n";
		}
		return result;
	}
}
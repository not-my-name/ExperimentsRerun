package za.redbrige.simulator.hybrid;

import za.redbridge.simulator.novelty.PhenotypeBehaviour;
import za.redbridge.simulator.ConstructionZone;
import za.redbridge.simulator.ContToDiscrSpace;
import za.redbridge.simulator.object.ResourceObject;
import za.redbridge.simulator.object.RobotObject;

import java.util.List;
import java.util.ArrayList;

/**
Class that stores a phenotype behaviour(PB) (for novelty) as well as the average objective fitness score over the 5 
runs in which this PB was the most novel
**/
public class HybridPhenotype extends PhenotypeBehaviour {
	private double aveFitnessForPhenotype;

	public HybridPhenotype(ConstructionZone[] constructionZones, ContToDiscrSpace discr, ResourceObject[] overallConstructionOrder, ArrayList<ResourceObject> resources, ArrayList<RobotObject> robots, int k, double aveFitnessForPhenotype) {
		super(constructionZones, discr, overallConstructionOrder, resources, robots, k);

		this.aveFitnessForPhenotype = aveFitnessForPhenotype;
	}

	public HybridPhenotype(PhenotypeBehaviour pb, double aveFitnessForPhenotype) {
		super(pb.getConstructionZones(), pb.getDiscrSpace(), pb.getConstructionOrder(), pb.getResources(), pb.getRobots(), pb.getK());
		this.aveFitnessForPhenotype = aveFitnessForPhenotype;
	}

	public double getAveFitnessForPhenotype() {
		return aveFitnessForPhenotype;
	}
}
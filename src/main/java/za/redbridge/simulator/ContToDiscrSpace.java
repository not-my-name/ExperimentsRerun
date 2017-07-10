package za.redbridge.simulator;

import sim.field.grid.ObjectGrid2D;
import sim.util.Bag;
import sim.util.IntBag;
import sim.field.grid.Grid2D;

import org.jbox2d.common.Vec2;

import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.*;

import za.redbridge.simulator.object.ResourceObject;
import za.redbridge.simulator.config.SchemaConfig;

public class ContToDiscrSpace {
	private float resWidth;
	private float resHeight;
	private float spaceWidth;
	private float spaceHeight;
	private Vec2[] centrePoints;
	private float hWidth;
	private float hHeight;
	private int nWidth;
	private int nHeight;
	private ObjectGrid2D grid;
	private Map<Vec2,int[]> spaceToGrid;
	private final Integer nullPlacer = new Integer(0);
	private final float gap;
	private SchemaConfig schema;
	private int schemaNumber;

	//a map to keep the mapping between a resources physical location and the discretized grid location
	private Map<ResourceObject, int[]> resToGridPosMap;

	public ContToDiscrSpace(int nWidth, int nHeight, double resWidth, double resHeight, float gap, SchemaConfig schema, int schemaNumber) {

		this.schema = schema;
		this.schemaNumber = schemaNumber;
		this.nWidth = nWidth;
		this.nHeight = nHeight;
		this.gap = gap;
		this.resWidth = (float)resWidth + gap;
		this.resHeight = (float)resHeight + gap;
		this.spaceWidth = (float)nWidth*this.resWidth;
		this.spaceHeight = (float)nHeight*this.resHeight;
		// System.out.println(spaceWidth + " " + spaceHeight);
		centrePoints = new Vec2[nWidth*nHeight];  //rows x cols
		this.hWidth = (float)this.resWidth/2;
		this.hHeight = (float)this.resHeight/2;
		// grid = new ObjectGrid2D(nWidth, nHeight, nullPlacer);
		grid = new ObjectGrid2D(nWidth, nHeight);
		spaceToGrid = new HashMap<>();
		resToGridPosMap = new HashMap<>();
		initCentrePoints();
	}

	public void initCentrePoints() {

		Vec2 firstP = new Vec2((float)hWidth, (float)hHeight);
		float dx = resWidth;
		float dy = resHeight;
		int cnt = 0;
		//loop through the y axis
		for (int y = 0; y < nHeight; y++) {
			//loop through the x axis
			for (int x = 0; x < nWidth; x++) {

				float newX = firstP.x + x*dx;
				float newY = spaceHeight - (firstP.y + y*dy);

				centrePoints[cnt] = new Vec2(newX, newY);
				int[] gridPos = {x,y};
				spaceToGrid.put(centrePoints[cnt], gridPos);

				// System.out.println(centrePoints[cnt] + " " + Arrays.toString(gridPos));
				cnt++;
			}
		}
	}

	/**
	Method that calculates the corresponding discritized position of a resource
	@param res the resource that should be discritized
	@param resToConnectTo the resource to be connected to (null if res is first for a constructionZone)
	@param connectionType the side (L,R,T,B) that res must be connect to resToConnectTo
		connectionType = 0 => resToConnectTo 'res _ _ _'
		connectionType = 1 => resToConnectTo '_ res _ _'
		connectionType = 2 => resToConnectTo '_ _ res _'
		connectionType = 3 => resToConnectTo '_ _ _ res'
	**/
	// public Vec2 addResourceToDiscrSpace (ResourceObject res, ResourceObject resToConnectTo, int connectionType) {
	// 	// System.out.println("res pos = " + res.getBody().getPosition());
	// 	// System.out.println("res adjacentList: " + Arrays.toString(res.getAdjacentList()));
	// 	if (resToConnectTo == null) {
	//
	// 		Vec2 discrPos = getNearestDiscrPos(res.getBody().getPosition());
	// 		res.setGridPosition(getGridPos(discrPos));
	//
	// 		int[] gridPos = spaceToGrid.get(discrPos);
	// 			// System.out.println("Pos in grid " + Arrays.toString(gridPos));
	// 		grid.set(gridPos[0], gridPos[1], res);
	// 		//System.out.println("Adding " + res + " at " + res.getBody().getPosition() + "=> " + discrPos + "(" + Arrays.toString(gridPos) + ")" + " IS FIRST");
	// 		return discrPos;
	// 	}
	// 	else {
	// 		//System.out.println("Pos of neighbour: " + resToConnectTo.getBody().getPosition());
	// 		int[] finalPosInGrid = new int[2];
	// 		int[] gridPos = spaceToGrid.get(resToConnectTo.getBody().getPosition());
	// 		//System.out.println("Pos of neighbour in grid = " + Arrays.toString(gridPos));
	// 		if (connectionType == 0) {
	// 			grid.set(gridPos[0]-1, gridPos[1], res);
	// 			finalPosInGrid[0] = gridPos[0]-1;
	// 			finalPosInGrid[1] = gridPos[1];
	// 		}
	// 		else if (connectionType == 1) {
	// 			grid.set(gridPos[0]+1, gridPos[1], res);
	// 			finalPosInGrid[0] = gridPos[0]+1;
	// 			finalPosInGrid[1] = gridPos[1];
	// 		}
	// 		else if (connectionType == 2) {
	// 			grid.set(gridPos[0], gridPos[1]-1, res);
	// 			finalPosInGrid[0] = gridPos[0];
	// 			finalPosInGrid[1] = gridPos[1]-1;
	// 		}
	// 		else if (connectionType == 3) {
	// 			grid.set(gridPos[0], gridPos[1]+1, res);
	// 			finalPosInGrid[0] = gridPos[0];
	// 			finalPosInGrid[1] = gridPos[1]+1;
	// 		}
	//
	// 		res.setGridPosition(finalPosInGrid);
	//
	// 		//System.out.println("Adding " + res + " at " + res.getBody().getPosition() + "=> " +  getDiscrPos(finalPosInGrid) + "(" + Arrays.toString(finalPosInGrid) + ")" + " ISNT FIRST");
	// 		return getDiscrPos(finalPosInGrid);
	// 	}
	//
	// 	// return discrPos;
	// }

	public int[] getGridPos(Vec2 resPos) {

		if (spaceToGrid.get(resPos) == null) {

			System.out.println("ISSUE: ");
			System.out.println("\t resPos = " + resPos);
		}

		return spaceToGrid.get(resPos);
	}

	public Vec2 getDiscrPos(int[] gridPos) {

		Object[][] fieldx = grid.field;
		Vec2 discrPos = new Vec2();

		for (Map.Entry<Vec2, int[]> entry : spaceToGrid.entrySet()) {
		    Vec2 dPos = entry.getKey();
		    int[] gridNums = entry.getValue();
		    if (Arrays.equals(gridNums, gridPos)) {
		    	discrPos = dPos;
		    }
		}

		return discrPos;
	}

	public void printGrid() {
		System.out.println(Arrays.deepToString(grid.field));
	}

	public ResourceObject[] getResNeighbourhood(ResourceObject res) {

		// System.out.println("NEIGHBOURHOOD FOR " + res);
		int[] resGridPos = getGridPos(res.getBody().getPosition());
		Bag neighbours = getVonNeumannNeighbors(res);
		Object[] nRes = neighbours.objs;
		// System.out.println("\tNeoighbourhood: " + Arrays.toString(nRes));
		ResourceObject[] adjacentResources = new ResourceObject[4];
		for (int i = 0; i < nRes.length; i++) {
			if (nRes[i] != null) {
				// System.out.println("\t\t" + nRes[i] + ": " + ((ResourceObject)nRes[i]).getBody().getPosition());
				ResourceObject adjRes = (ResourceObject)nRes[i];
				int[] gridPos = getGridPos(adjRes.getBody().getPosition());
				// System.out.println("\t\tAdjPos: " + Arrays.toString(gridPos));
				//If they are above/below eachother
				if (gridPos[0] == resGridPos[0]) {
					//If adjRes is above this resource
					if (gridPos[1] < resGridPos[1]) {
						adjacentResources[2] = adjRes;
					}
					else {
						adjacentResources[3] = adjRes;
					}
				}
				//If they are left/right of eachother
				else {
					//If adjRes is to the left of this resource
					if (gridPos[0] < resGridPos[0]) {
						adjacentResources[0] = adjRes;
					}
					else {
						adjacentResources[1] = adjRes;
					}
				}
			}
		}

		return adjacentResources;
	}

	public Bag getVonNeumannNeighbors(ResourceObject res) {
		int[] gridPos = new int[2];
		//search for the position of the resource in the grid
		for (int i = 0; i < centrePoints.length; i++) {
			// System.out.println(res.getBody().getPosition().sub(centrePoints[i]).length());
			if (res.getBody().getPosition().sub(centrePoints[i]).length() == 0) {
				gridPos = spaceToGrid.get(centrePoints[i]);
				break;
			}
		}

		// System.out.println(Arrays.deepToString(grid.field));

		return grid.getVonNeumannNeighbors(gridPos[0], gridPos[1], 1, Grid2D.BOUNDED, false, new Bag(), new IntBag(), new IntBag() );
	}

	public void clearGrid() {
		grid.clear();
	}

	/**
	Method to work out wether two resources can start a new CZ given their gridPositions and connectiontype (r1 cType r2)
	connectionType = 0 => r1 'r2 _ _ _'
	connectionType = 1 => r1 '_ r2 _ _'
	connectionType = 2 => r1 '_ _ r2 _'
	connectionType = 3 => r1 '_ _ _ r2'
	@return true if both resources can be placed without overlapping previous
	**/
	public boolean canBeConnected (ResourceObject r1, ResourceObject r2, int connectionType) {

		//Get discriticesd position of r1
		int[] r2GridPos;
		int[] r1GridPos = new int[2];

		if (r2.isConstructed()) {
			r2GridPos = spaceToGrid.get(r2.getBody().getPosition());
		}
		else {
			r2GridPos = spaceToGrid.get(getNearestDiscrPos(r2.getBody().getPosition()));
			if ((r2GridPos[0] > 0 && r2GridPos[0] < grid.field.length)&&(r2GridPos[1] > 0 && r2GridPos[1] < grid.field.length)) {
				if (grid.get(r2GridPos[0], r2GridPos[1]) != null) {
					return false;
				}
			}
			else {
				return false;
			}
		}

		//Given where r2 is placed, figure out where r1 would need to be placed:
		//If r2 is to the left of r1
		if (connectionType == 0) {
			r1GridPos[0] = r2GridPos[0] + 1;
			r1GridPos[1] = r2GridPos[1];
			if ((r1GridPos[0] > 0 && r1GridPos[0] < grid.field.length)&&(r1GridPos[1] > 0 && r1GridPos[1] < grid.field.length)) {
				//If this position is not taken, return true
				if (grid.get(r1GridPos[0], r1GridPos[1]) == null) {
					// grid.set(r1GridPos[0], r1GridPos[1], r1);
					// grid.set(r2GridPos[0], r2GridPos[1], r2);
					resToGridPosMap.put(r1, r1GridPos);
					resToGridPosMap.put(r2, r2GridPos);
					return true;
				}
				else {
					return false;
				}
			}
			else {
				return false;
			}
		}
		//If r2 is to the right of r1
		else if (connectionType == 1) {
			r1GridPos[0] = r2GridPos[0] - 1;
			r1GridPos[1] = r2GridPos[1];
			if ((r1GridPos[0] > 0 && r1GridPos[0] < grid.field.length)&&(r1GridPos[1] > 0 && r1GridPos[1] < grid.field.length)) {
				//if this pos is not taken:
				if (grid.get(r1GridPos[0], r1GridPos[1]) == null) {
					// grid.set(r1GridPos[0], r1GridPos[1], r1);
					// grid.set(r2GridPos[0], r2GridPos[1], r2);
					resToGridPosMap.put(r1, r1GridPos);
					resToGridPosMap.put(r2, r2GridPos);
					return true;
				}
				else {
					return false;
				}
			}
			else {
				return false;
			}
		}
		//If r2 is above r1
		else if (connectionType == 2) {
			r1GridPos[0] = r2GridPos[0];
			r1GridPos[1] = r2GridPos[1] + 1;
			if ((r1GridPos[0] > 0 && r1GridPos[0] < grid.field.length)&&(r1GridPos[1] > 0 && r1GridPos[1] < grid.field.length)) {
				//if this pos is not taken:
				if (grid.get(r1GridPos[0], r1GridPos[1]) == null) {
					// grid.set(r1GridPos[0], r1GridPos[1], r1);
					// grid.set(r2GridPos[0], r2GridPos[1], r2);
					resToGridPosMap.put(r1, r1GridPos);
					resToGridPosMap.put(r2, r2GridPos);
					return true;
				}
				else {
					return false;
				}
			}
			else {
				return false;
			}
		}
		else {
			r1GridPos[0] = r2GridPos[0];
			r1GridPos[1] = r2GridPos[1] - 1;
			if ((r1GridPos[0] > 0 && r1GridPos[0] < grid.field.length)&&(r1GridPos[1] > 0 && r1GridPos[1] < grid.field.length)) {
				//if this pos is not taken:
				if (grid.get(r1GridPos[0], r1GridPos[1]) == null) {
					// grid.set(r1GridPos[0], r1GridPos[1], r1);
					// grid.set(r2GridPos[0], r2GridPos[1], r2);
					resToGridPosMap.put(r1, r1GridPos);
					resToGridPosMap.put(r2, r2GridPos);
					return true;
				}
				else {
					return false;
				}
			}
			else {
				return false;
			}
		}
	}

	/**
	Method that calculates the corresponding discritized position of a resource
	@param res the resource that should be discritized
	@param resToConnectTo the resource to be connected to (null if res is first for a constructionZone)
	@param connectionType the side (L,R,T,B) that res must be connect to resToConnectTo
		connectionType = 0 => res 'resToConnectTo _ _ _'
		connectionType = 1 => res '_ resToConnectTo _ _'
		connectionType = 2 => res '_ _ resToConnectTo _'
		connectionType = 3 => res '_ _ _ resToConnectTo'
	**/
	public Vec2 addResourceToDiscrSpace (ResourceObject res) {
		// System.out.println("res pos = " + res.getBody().getPosition());
		// System.out.println("res adjacentList: " + Arrays.toString(res.getAdjacentList()));
		// if (resToConnectTo == null) {
			Vec2 discrPos;
			int[] gridPos;
				// System.out.println("Pos in grid " + Arrays.toString(gridPos));
			if (resToGridPosMap.containsKey(res)) {
				discrPos = getDiscrPos(resToGridPosMap.get(res));
				gridPos = resToGridPosMap.get(res);
			}
			else {
				discrPos = getNearestDiscrPos(res.getBody().getPosition());
				gridPos = spaceToGrid.get(discrPos);
			}
			grid.set(gridPos[0], gridPos[1], res);
			// System.out.println("Adding " + res + " at " + res.getBody().getPosition() + "=> " + discrPos + "(" + Arrays.toString(gridPos) + ")" + " IS FIRST");
			//res.setGridPosition(gridPos);
			return discrPos;
}



	/**
	Method to work out wether two resources can start a new CZ given their gridPositions and connectiontype (r1 cType r2)
	connectionType = 0 => r1 'r2 _ _ _'
	connectionType = 1 => r1 '_ r2 _ _'
	connectionType = 2 => r1 '_ _ r2 _'
	connectionType = 3 => r1 '_ _ _ r2'
	@return true if both resources can be placed without overlapping previous
	**/
	// public boolean canBeConnected (ResourceObject r1, ResourceObject r2, int connectionType) {
	// 	//Get discriticesd position of r1
	// 	int[] r1GridPos = spaceToGrid.get(getNearestDiscrPos(r1.getBody().getPosition()));
	//
	//
	// 	//Calculate where r2 should be placed according to the connectionType
	// 	int[] r2GridPos = new int[2];
	// 	if (connectionType == 0) {
	// 		// r2 is to the left of r1
	// 		r2GridPos[0] = r1GridPos[0]-1;
	// 		r2GridPos[1] = r1GridPos[1];
	// 		// this position is not taken
	// 		if (grid.get(r2GridPos[0], r2GridPos[1]) == null) {
	// 			return true;
	// 		}
	// 		else {
	// 			System.out.println("ISSUE with:");
	// 			System.out.println("\t" + r1);
	// 			System.out.println("\t" + r2);
	// 			System.out.println("Connection type = " + connectionType);
	// 			return false;
	// 		}
	// 	}
	// 	else if (connectionType == 1) {
	// 		// r2 is to the right of r1
	// 		r2GridPos[0] = r1GridPos[0]+1;
	// 		r2GridPos[1] = r1GridPos[1];
	// 		// this position is not taken
	// 		if (grid.get(r2GridPos[0], r2GridPos[1]) == null) {
	// 			return true;
	// 		}
	// 		else {
	// 			System.out.println("ISSUE with:");
	// 			System.out.println("\t" + r1);
	// 			System.out.println("\t" + r2);
	// 			System.out.println("Connection type = " + connectionType);
	// 			return false;
	// 		}
	// 	}
	// 	else if (connectionType == 2) {
	// 		// r2 is above r1
	// 		r2GridPos[0] = r1GridPos[0];
	// 		r2GridPos[1] = r1GridPos[1]-1;
	// 		// this position is not taken
	// 		if (grid.get(r2GridPos[0], r2GridPos[1]) == null) {
	// 			return true;
	// 		}
	// 		else {
	// 			System.out.println("ISSUE with:");
	// 			System.out.println("\t" + r1);
	// 			System.out.println("\t" + r2);
	// 			System.out.println("Connection type = " + connectionType);
	// 			return false;
	// 		}
	// 	}
	// 	else {
	// 		// r2 is below r1
	// 		r2GridPos[0] = r1GridPos[0];
	// 		r2GridPos[1] = r1GridPos[1]+1;
	// 		// this position is not taken
	// 		if (grid.get(r2GridPos[0], r2GridPos[1]) == null) {
	// 			return true;
	// 		}
	// 		else {
	// 			System.out.println("ISSUE with:");
	// 			System.out.println("\t" + r1);
	// 			System.out.println("\t" + r2);
	// 			System.out.println("Connection type = " + connectionType);
	// 			return false;
	// 		}
	// 	}
	// }

	public ObjectGrid2D getGrid() {
		return grid;
	}

	/*
	Recursively move through all neighbours and collate all resources that are considered within the same cz
	**/
	public void generateTraversal(List<ResourceObject> czList, ResourceObject currRes, List<ResourceObject> ignoreList) {

		//Count up number of neighbours that need to be looked at
		List<ResourceObject> nToCheck = new LinkedList<>();
		ResourceObject[] newNeighbours = getResNeighbourhood(currRes);
		//ResourceObject[] neighbours = currRes.getAdjacentList();

		/*
		create methods to set the neighbours of the resource once they have been connected to
		another construction zone
		after the generate traversal at some point
		*/

		int temp = newNeighbours.length;
		String[] neighboursString = new String[temp];

		for(int k = 0; k < temp; k++) {
			if(newNeighbours[k] != null) {

				String tempType = newNeighbours[k].getType();
				if(tempType != null) {
					neighboursString[k] = newNeighbours[k].getType();
				}
				else{
					neighboursString[k] = "_";
				}
			}
		}

		/*
		check that the fitness values are correct for each simulation
		checkin that using the above hack to get the neighbours does not affect the
		fitness calculations
		*/

		// System.out.println("ContToDiscrSpace: the newly created neighbours string = " + Arrays.toString(neighboursString) );
		// System.out.println("ContToDiscrSpace: the string array from YOUR method = " + Arrays.toString(currRes.getAdjacentResources()));
		// System.out.println("");

		//int[] incorrectSides =  schema.getIncorrectAdjacentSides(schemaNumber, currRes.getType(), neighboursString);
		//int[] incorrectSides =  schema.getIncorrectAdjacentSides(schemaNumber, currRes.getType(), currRes.getAdjacentResources());
		int[] incorrectSides =  schema.getIncorrectAdjacentSides(schemaNumber, currRes.getType(), neighboursString);
		//System.out.println("ContToDiscrSpace: num incorrect sides = " + Arrays.toString(incorrectSides));
		//System.out.println("ContToDiscrSpace: the neighbouring resources");
		//System.out.println(Arrays.toString(neighbours));

		for (int i = 0; i < newNeighbours.length; i++) {

			if (newNeighbours[i] != null && !newNeighbours[i].getVisited() && incorrectSides[i] == 0 && !ignoreList.contains(newNeighbours[i])) {
				nToCheck.add(newNeighbours[i]);
			}
			else if (newNeighbours[i] != null && incorrectSides[i] == 1 && !ignoreList.contains(newNeighbours[i])) {
				ignoreList.add(newNeighbours[i]);
			}
		}

		//System.out.println("ContToDiscrSpace: the list of neighbours to chec:");
		//System.out.println(nToCheck);
		//System.out.println("ContToDiscrSpace: the ignore list:");
		//System.out.println(ignoreList);
		//System.out.println("");

		//System.out.println("ContToDiscrSpace: current resource = " + currRes + " the neighbours list = " + Arrays.toString(nToCheck.toArray(new ResourceObject[0])));

		//Base Case: no neighbours to look at
		if (nToCheck.size() == 0) {
			currRes.setVisited(true);
			czList.add(currRes);
			return;
		}
		else {
			currRes.setVisited(true);
			czList.add(currRes);

			//Go through each neighbour,
			for (ResourceObject nRes : nToCheck) {
				generateTraversal(czList, nRes, ignoreList);
			}
		}
	}

	public Vec2 getNearestDiscrPos (Vec2 resPos) {
		float maxDist = 10000f;
		Vec2 nearestDiscrPos = new Vec2();
		for (int i = 0; i < centrePoints.length; i++) {
			if (resPos.sub(centrePoints[i]).length() < maxDist) {
				nearestDiscrPos = centrePoints[i];
				maxDist = resPos.sub(centrePoints[i]).length();
			}
		}
		Vec2 transToNewPos = new Vec2();
		transToNewPos.x = nearestDiscrPos.sub(resPos).x;
		transToNewPos.y = nearestDiscrPos.sub(resPos).y;
		return nearestDiscrPos;
	}

	public void printSizes() {
		System.out.println("Map size: " + spaceToGrid.size());
		System.out.println("Grid size: " + grid.elements().size());
	}
}

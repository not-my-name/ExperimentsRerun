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
	private Map<ResourceObject, int[]> resToGridPosMap;
	private final Integer nullPlacer = new Integer(0);
	private final float gap;
	private SchemaConfig schema;
	private int schemaNumber;

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
	Method to work out wether two resources can start a new CZ given their gridPositions and connectiontype (r1 cType r2)
	connectionType = 0 => r1 'r2 _ _ _'
	connectionType = 1 => r1 '_ r2 _ _'
	connectionType = 2 => r1 '_ _ r2 _'
	connectionType = 3 => r1 '_ _ _ r2'
	@return true if both resources can be placed without overlapping previous
	**/
	public boolean canBeConnected (ResourceObject r1, ResourceObject r2, int connectionType) {

		// System.out.println("CHECKING  CONNECTION:");
		// System.out.println("r1 = " + r1);
		// System.out.println("r2 = " + r2);

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

		// System.out.println("\tr2's place in the grid = " + Arrays.toString(r2GridPos));

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
				System.out.println("OYYYYYYYYYYY");
				discrPos = getNearestDiscrPos(res.getBody().getPosition());
				gridPos = spaceToGrid.get(discrPos);
			}
			grid.set(gridPos[0], gridPos[1], res);
			// System.out.println("Adding " + res + " at " + res.getBody().getPosition() + "=> " + discrPos + "(" + Arrays.toString(gridPos) + ")" + " IS FIRST");
			return discrPos;
		// }
		// else {
		// 	System.out.println("SHOULDNT HAPPEN");
		// 	return new Vec2();
		// }
		// else {
		// 	// System.out.println("Pos of neighbour: " + resToConnectTo.getBody().getPosition());
		// 	// int[] finalPosInGrid = new int[2];
		// 	int[] cResGridPos;

		// 	if (resToGridPosMap.containsKey(res)) {

		// 		// cResGridPos = resToGridPosMap.get(resToConnectTo);
		// 		// System.out.println("resToCOnnectTo: " + Arrays.toString(cResGridPos));
		// 	}
		// 	else {
		// 		System.out.println("OY!!");
		// 		cResGridPos = spaceToGrid.get(resToConnectTo.getBody().getPosition());
		// 	}
		// 	// System.out.println("Pos of neighbour in grid = " + Arrays.toString(cResGridPos));
		// 	// System.out.println("Pos of resource beign added: " + res.getBody().getPosition());
		// 	// System.out.println("Possible snapping: " + Arrays.toString(spaceToGrid.get(getNearestDiscrPos(res.getBody().getPosition()))));
		// 	// if (connectionType == 0) {
		// 	// 	finalPosInGrid[0] = cResGridPos[0]+1;
		// 	// 	finalPosInGrid[1] = cResGridPos[1];
		// 	// 	// System.out.println("Adding " + res + " at " + res.getBody().getPosition() + "=> " +  getDiscrPos(finalPosInGrid) + "(" + Arrays.toString(finalPosInGrid) + ")" + " ISNT FIRST");
		// 	// 	grid.set(finalPosInGrid[0], finalPosInGrid[1], res);
		// 	// 	return getDiscrPos(finalPosInGrid);
		// 	// }
		// 	// else if (connectionType == 1) {
		// 	// 	finalPosInGrid[0] = cResGridPos[0]-1;
		// 	// 	finalPosInGrid[1] = cResGridPos[1];
		// 	// 	// System.out.println("Adding " + res + " at " + res.getBody().getPosition() + "=> " +  getDiscrPos(finalPosInGrid) + "(" + Arrays.toString(finalPosInGrid) + ")" + " ISNT FIRST");
		// 	// 	grid.set(finalPosInGrid[0], finalPosInGrid[1], res);
		// 	// 	return getDiscrPos(finalPosInGrid);
		// 	// }
		// 	// else if (connectionType == 2) {
		// 	// 	finalPosInGrid[0] = cResGridPos[0];
		// 	// 	finalPosInGrid[1] = cResGridPos[1]+1;
		// 	// 	// System.out.println("Adding " + res + " at " + res.getBody().getPosition() + "=> " +  getDiscrPos(finalPosInGrid) + "(" + Arrays.toString(finalPosInGrid) + ")" + " ISNT FIRST");
		// 	// 	grid.set(finalPosInGrid[0], finalPosInGrid[1], res);
		// 	// 	return getDiscrPos(finalPosInGrid);
		// 	// }
		// 	// else {
		// 	// 	finalPosInGrid[0] = cResGridPos[0];
		// 	// 	finalPosInGrid[1] = cResGridPos[1]-1;
		// 	// 	// System.out.println("Adding " + res + " at " + res.getBody().getPosition() + "=> " +  getDiscrPos(finalPosInGrid) + "(" + Arrays.toString(finalPosInGrid) + ")" + " ISNT FIRST");
		// 	// 	grid.set(finalPosInGrid[0], finalPosInGrid[1], res);
		// 	// 	return getDiscrPos(finalPosInGrid);
		// 	// }
			
		// }
		
		// return discrPos;
	}

	public int[] getGridPos(Vec2 resPos) {
		if (spaceToGrid.get(resPos) == null) {
			System.out.println("ISSUE with getting gridPos: ");
			System.out.println("\t resPos = " + resPos);
		}
		return spaceToGrid.get(resPos);
	}

	public Vec2 getDiscrPos(int[] gridPos) {
		Object[][] fieldx = grid.field;
		Vec2 discrPos = new Vec2();
		// int[] gridPos = new int[2];
		// for(int i = 0; i < fieldx.length; i++)
		// {
		// 	Object[] fieldy = fieldx[i];
		// 	for(int j=0; j < fieldy.length; j++) {
		// 		ResourceObject r = (ResourceObject)grid.get(i,j);
		// 		if (r == res) {
		// 			gridPos = {i,j};
		// 		}
		// 	}
		// }

		for (Map.Entry<Vec2, int[]> entry : spaceToGrid.entrySet()) {
		    Vec2 dPos = entry.getKey();
		    int[] gridNums = entry.getValue();
		    if (Arrays.equals(gridNums, gridPos)) {
		    	discrPos = dPos;
		    }
		}


		// Object[] members = grid.toArray();
		// Vec2 discrPos = new Vec2();
		// for (int i = 0; i < members.length; i++) {
		// 	if (res == members[i]) {
		// 		discrPos = centrePoints[i];
		// 		break;
		// 	}
		// }

		// System.out.println("New pos = " + discrPos);
		return discrPos;
	}

	public ObjectGrid2D getGrid() {
		return grid;
	}

	public void printGrid() {
		System.out.println(Arrays.deepToString(grid.field));
	}

	public ResourceObject[] getResNeighbourhood(ResourceObject res) {
		// System.out.println("NEIGHBOURHOOD FOR " + res);
		int[] resGridPos = getGridPos(res.getBody().getPosition());
		// System.out.println("\tRes gridPos: " + Arrays.toString(resGridPos));
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

	// /**
	// Loops through the grid from L-R and T-B and updates the CZS giving preference to left and up values
	// **/
	// // public ConstructionZone[] getUpdatedCZs(ResourceObject[] constructionOrder) {
	// // public ConstructionZone[] getUpdatedCZs(List<ResourceObject> constructionOrder) {
	// public ConstructionZone[] getUpdatedCZs(ConstructionZone[] czs) {
	// 	ConstructionZone[] newCZs;
	// 	int numCzs = 0;
	// 	for (int i = 0; i < czs.length; i++) {
	// 		if (czs[i] != null) {
	// 			numCzs ++;
	// 		}
	// 	}
	// 	System.out.println("UPDATING CZS NOW");
	// 	newCZs = new ConstructionZone[numCzs];
	// 	boolean hasBeenChanged = false;
	// 	for (int i = 0; i < newCZs.length; i++) {
	// 		if (czs[i] != null) {
	// 			System.out.println(i + ": " + czs[i]);
	// 			boolean isMoved = false;
	// 			for (ResourceObject czRes : czs[i].getConnectedResources()) {
	// 				// System.out.println("res: " + czRes);
	// 				ResourceObject[] nResources = czRes.getAdjacentResources();
	// 				for (int j = 0; j < nResources.length; j++) {
	// 					if ((nResources[j] != null) && (nResources[j].isConstructed())) {
	// 						// System.out.println("isConstructed: " + nResources[j].isConstructed());
	// 						if (nResources[j].getCzNumber() < czRes.getCzNumber()) {
	// 							// System.out.println("OYYYY lowerCZNUM: " + nResources[j]);
	// 							// System.out.println("\tNew CZNUM = " + nResources[j].getCzNumber());
	// 							List<ResourceObject> membersToMove = czs[i].updateCZNumber(nResources[j].getCzNumber());
	// 							// System.out.println("\t\t" + membersToMove.get(0).getCzNumber());
	// 							czs[(nResources[j].getCzNumber()-1)].addNewResources(membersToMove);
	// 							czs[i].clearCZ();
	// 							newCZs[(nResources[j].getCzNumber()-1)] = czs[(nResources[j].getCzNumber()-1)];
	// 							czs[i] = null;
	// 							isMoved = true;
	// 							hasBeenChanged = true;
	// 							break;
	// 						}
	// 					}
	// 				}
	// 				if (isMoved) {
	// 					break;
	// 				}
	// 			}
	// 			if (!isMoved) {
	// 				newCZs[i] = czs[i];
	// 			}
	// 		}
	// 	}
		
	// 	if (hasBeenChanged) {
	// 		List<ConstructionZone> nonNull = new LinkedList<>();
	// 		System.out.println("NEW CZS:");
	// 		for (int i = 0; i < newCZs.length; i++) {
	// 			if (newCZs[i] != null) {
	// 				System.out.println("CZ: " + newCZs[i]);
	// 				int cnt = 1;
	// 				for (ResourceObject r : newCZs[i].getConstructionOrder()) {
	// 				    System.out.println("\t" + cnt + " " + r);
	// 				    cnt ++;
	// 				}
	// 				nonNull.add(newCZs[i]);				
	// 			}
	// 		}

	// 		newCZs = nonNull.toArray(new ConstructionZone[0]);
	// 		// for (int i = 0; i < numUsed; i++) {
	// 		// 	newCZs[i] = temp[i];
	// 		// }
	// 		// return Arrays.copyOf(newCZs, numUsed + 1);
	// 		return newCZs;
	// 	}
		
	// 	else {
	// 		return czs;
	// 	}
	// }

		/**
		Works except in the case that there is:  
		    x		
		x x x
		**/
		//public....
		// int latestCZNum = -1;
		// boolean isFirst = true;
		// List<ResourceObject> seenResources = new LinkedList<>();
		// for (int y = 0; y < nWidth; y++) {
		// 	for (int x = 0; x < nHeight; x++) {
		// 		ResourceObject thisRes = (ResourceObject)grid.get(x,y);
		// 		if (!seenResources.contains(thisRes)) {

		// 			if ((thisRes != null)) {
		// 				System.out.println("Seen " + thisRes);
		// 				latestCZNum++;
		// 				thisRes.setCzNumber(latestCZNum);
						
		// 				seenResources.add(thisRes);
		// 				System.out.println("Adding " + thisRes + " " + latestCZNum);
		// 				// isFirst = false;
		// 				ResourceObject[] nResources = getResNeighbourhood(thisRes);
		// 				for (int i = 0; i < nResources.length; i++) {
		// 					if (nResources[i] != null) {
		// 						System.out.println("\tneighbour: " + nResources[i]);
		// 						//If adj resources are part of different CZs, set the adj one to this cz.
		// 						if (!seenResources.contains(nResources[i])) {
		// 							nResources[i].setCzNumber(thisRes.getCzNumber());
		// 							seenResources.add(nResources[i]);
		// 							System.out.println("\t\tAaaaaadding " + nResources[i] + " " + latestCZNum);
		// 						}
		// 					}
		// 				}

		// 			}

		// 		}
		// 		else {
		// 			if ((thisRes != null)) {
		// 				System.out.println("Already seen " + thisRes);
		// 				// thisRes.setCzNumber(latestCZNum);
		// 				// latestCZNum++;
		// 				// seenResources.add(thisRes);
		// 				// isFirst = false;
		// 				ResourceObject[] nResources = getResNeighbourhood(thisRes);
		// 				for (int i = 0; i < nResources.length; i++) {
		// 					if (nResources[i] != null) {
		// 						System.out.println("\tneighbour: " + nResources[i]);
		// 						//If adj resources are part of different CZs, set the adj one to this cz.
		// 						if (!seenResources.contains(nResources[i])) {
		// 							nResources[i].setCzNumber(thisRes.getCzNumber());
		// 							seenResources.add(nResources[i]);
		// 							System.out.println("\t\tAaadding " + nResources[i] + " " + latestCZNum);
		// 						}
		// 					}
		// 				}
		// 			}
		// 		}
		// 	}
		// }

		// for (ResourceObject r : seenResources) {
		// 	System.out.println(r + " " + r.getCzNumber());
		// }

		// ConstructionZone[] newCZs = new ConstructionZone[latestCZNum + 1];

		// for (int i = 0; i < newCZs.length; i++) {
		// 	newCZs[i] = new ConstructionZone(seenResources, i);
		// }

		// return newCZs;
		
	// }

	/**
	Recursively move through all neighbours and collate all resources that are considered within the same cz
	**/
	public void generateTraversal(List<ResourceObject> czList, ResourceObject currRes, List<ResourceObject> ignoreList) {
		//Count up number of neighbours that need to be looked at
		List<ResourceObject> nToCheck = new LinkedList<>();
		ResourceObject[] neighbours = getResNeighbourhood(currRes);

		int[] incorrectSides =  schema.getIncorrectAdjacentSides(schemaNumber, currRes.getType(), currRes.getAdjacentList());

		for (int i = 0; i < neighbours.length; i++) {
			// System.out.println("\tNeighbour: " + neighbours[i]);
			if (neighbours[i] != null && !neighbours[i].isVisited() && incorrectSides[i] == 0 && !ignoreList.contains(neighbours[i])) {
				nToCheck.add(neighbours[i]);
			}
			else if (neighbours[i] != null && incorrectSides[i] == 1 && !ignoreList.contains(neighbours[i])) {
				ignoreList.add(neighbours[i]);
			}
		}

		//Base Case: no neighbours to look at
		if (nToCheck.size() == 0) {
			// System.out.println("BASE CASE");
			currRes.setVisited(true);
			czList.add(currRes);
			// System.out.println("ADDED TO TRAVERSAL: " + currRes);
			return;
		}
		else {
			// System.out.println("Otherwise");
			currRes.setVisited(true);
			czList.add(currRes);
			// System.out.println("ADDED TO TRAVERSAL: " + currRes);
			//Go through each neighbour, 
			for (ResourceObject nRes : nToCheck) {
				generateTraversal(czList, nRes, ignoreList);
			}
			
			// ResourceObject[] neighbours = currRes.getResNeighbourhood();
			// //build the neighbourhood resources to go and check
			// for (int i = 0; i < neighbours.length; i++) {
			// 	if (neighbours[i] != null) {
			// 		if (!czList.contains(neighbours[i])) {
			// 			nToCheck.add(neighbours[i]);
			// 		}
			// 	}
			// }

			// for (ResourceObject nRes : nToCheck) {
			// 	Collections.addAll(czList, updateCZ(czList, nRes, ))
			// }
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
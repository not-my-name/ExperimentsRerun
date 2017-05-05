package za.redbridge.simulator.paramTuning;

import java.util.Map;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Arrays;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import za.redbridge.simulator.ConstructionTask;

import static za.redbridge.simulator.Utils.getParamLoggingDirectory;

/**
A class that will tune the parameters of the NEA.
Parameters being tuned:
	Max timesteps for simulation runs
	Objective weights:
		A - average distance between robot and closest resource to it
		B - average number of times that robots connected to resources
		C - average distance between resources
		D - the number of adjacent resources
		E - the number of adjacent resources that are in the correct schema
		F - average distance between resources and the construction starting area

Assessment of parameter's performance = how many blocks connected + how many correctly connected blocks
**/

public class ParameterTuning {
	// private ConstructionT
	private double[] maxTimeSteps = {100, 200, 500, 1000, 5000, 10000};
	// private double[] maxTimeSteps = {200};

	// private double[] weightValues = {0.1, 0.3, 0.5, 1, 2, 5, 10, 20, 50, 100};
	private double[] weightValues = {0.3};

	// private double[][] parameterList;
	private final ArrayList<double[]> parameterList;

	private HashMap<double[], double[]> parameterPerformanceArchive;

	public ParameterTuning() {
		parameterPerformanceArchive = new HashMap<>();
		// parameterList = new double[2000][3];
		parameterList = new ArrayList<>();
		populateParameters();
	}

	/**
	Parameterized constructor
	@param filename string repr. of the filepath for a list of parameters to test
	**/
	public ParameterTuning(String filename) {
		parameterPerformanceArchive = new HashMap<>();
		parameterList = new ArrayList<>();
		try {
			BufferedReader f = new BufferedReader(new FileReader(filename));
			String s = f.readLine();

			
			while (s != null) {
				String [] p = s.split(",");
				double[] parameters = new double[p.length];

				for (int i = 0; i < p.length; i++) {
					parameters[i] = Double.parseDouble(p[i]);
				}

				parameterList.add(Arrays.copyOf(parameters, parameters.length));
				s = f.readLine();
			}
		}
		catch (IOException e) {
			System.out.println(e);
		}

		for (double[] p : parameterList) {
			System.out.println(Arrays.toString(p));
		}
	}

	public void populateParameters() {
		double[] parameters = new double[7];
		int cnt = 0;
		//For every timestep
		for (int p0 = 0; p0 < maxTimeSteps.length; p0++) {
			
			//For weights
			for (int w1 = 0; w1 < weightValues.length; w1++) {
				for (int w2 = 0; w2 < weightValues.length; w2++) {
					for (int w3 = 0; w3 < weightValues.length; w3++) {
						for (int w4 = 0; w4 < weightValues.length; w4++) {
							for (int w5 = 0; w5 < weightValues.length; w5++) {
								for (int w6 = 0; w6 < weightValues.length; w6++) {
									parameters[0] = maxTimeSteps[p0];
									parameters[1] = weightValues[w1];
									parameters[2] = weightValues[w2];
									parameters[3] = weightValues[w3];
									parameters[4] = weightValues[w4];
									parameters[5] = weightValues[w5];
									parameters[6] = weightValues[w6];
									// double[] params = {maxTimeSteps[p0], weightValues[w1], weightValues[w2], weightValues[w3], weightValues[w4], weightValues[w5], weightValues[w6]};
									// parameterPerformanceArchive.put(parameters, new Double(0));
									// System.out.println(Arrays.toString(parameterList[cnt]));
									// parameterList[cnt] = parameters;
									// System.out.println(Arrays.toString(parameters));
									parameterList.add(Arrays.copyOf(parameters, parameters.length));
									// System.out.println(Arrays.toString(parameterList.get(cnt)) + " " + cnt);
									// System.out.println("++++++++++++++++++++");
									// cnt++;
								}
							}
						}
					}
					// System.out.println(Arrays.toString(parameters));
					
				}
			}
			
		}
		// System.out.println("After loop:");
		// System.out.println(Arrays.toString(parameterList.get(0)));
		// int count = 0;
		// for (double[] ps : parameterList) {
		// 	// System.out.println(Arrays.toString(ps));
		// 	if (!(parameterList.lastIndexOf(ps) - parameterList.indexOf(ps) == 0)) {
		// 		// System.out.println("YEAH!!");
		// 		// count++;
		// 			System.out.println("NOOOO");
		// 	}
		// 	else {
		// 		System.out.println(count);
		// 		count++;
		// 	}
		// }

		// System.out.println(count + " - " + parameterList.size());
		// for (int i = 0;	i < cnt; i++) {
		// 	System.out.println(Arrays.toString(parameterList[i]));
		// }
		// double[] test = {100.0, 0.1, 0.3, 1.0, 0.3, 0.5, 1.0};
		// System.out.println(parameterList.contains(test));
		// System.out.println(parameterList.size());
	}

	public ArrayList<double[]> getParamList () {
		return parameterList;
	}

	public HashMap<double [], double []> getArchive () {
		return parameterPerformanceArchive;
	}

	public int getParameterListSize() {
		return parameterList.size();
		// return parameterList.length;
	}

	public int getMaxTimeStepLength () {
		return maxTimeSteps.length;
	}

	public double [] getNextParameterSet(int i) {
		double[] nextParams = parameterList.get(i);
		// double[] nextParams = parameterList[i];
		//Increment weights
		return nextParams;
	}

	public static double getPerformance (ConstructionTask ct) {
		if (ct != null) {
			double performance = ct.getNumAdjacentResources() + 2*ct.getNumResourcesCorrectlyConnected();
			// System.out.println("number of connected resources = " + ct.getNumAdjacentResources() + ", number correctly connected = " + ct.getNumResourcesCorrectlyConnected());
			return performance;
		}
		else {
			return 0D;
		}
	}

	public void setArchive(HashMap<double[], double[]> perfArchive) {
		this.parameterPerformanceArchive = perfArchive;
	}

	public void logParameterPerformance(double[] params, DescriptiveStatistics d) {
		// double performance = ct.getNumAdjacentResources() + 2*ct.getNumResourcesCorrectlyConnected();
		// System.out.println();
		double [] performanceLog = {d.getMean(), d.getMax()};
		System.out.println(performanceLog[0] + " " + performanceLog[1]);
		parameterPerformanceArchive.put(params, performanceLog);
	}

	public void printPerformance() {
		for (Map.Entry<double[], double[]> entry : parameterPerformanceArchive.entrySet()) {
		    double[] param = entry.getKey();
		    double[] perfLog = entry.getValue();
		    System.out.println(Arrays.toString(param) + " " + Arrays.toString(perfLog));
		}
	}

	// public int getConstructionZoneSize () {
	// 	return cz.getNumConstructionZoneResources();
	// }
}
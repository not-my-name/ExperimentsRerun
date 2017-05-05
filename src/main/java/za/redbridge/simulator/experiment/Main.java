package za.redbridge.simulator.experiment;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import sim.display.Console;
import za.redbridge.simulator.Simulation;
import za.redbridge.simulator.SimulationGUI;
import za.redbridge.simulator.config.ExperimentConfig;
import za.redbridge.simulator.config.MorphologyConfig;
import za.redbridge.simulator.config.SimConfig;
import za.redbridge.simulator.factories.HomogeneousRobotFactory;
import za.redbridge.simulator.phenotype.ChasingPhenotype;
import za.redbridge.simulator.phenotype.ChasingPhenotype_simple;

//entry point into simulator

/**
 * Created by racter on 2014/08/19.
 */
public class Main {

    //config files for this experiment

    // @Option(name="--experiment-config",aliases = "-e", usage="Filename for experiment configuration", metaVar="<experiment config>")
    // private String experimentConfig = "configs/experimentConfig.yml";;

    // @Option (name="--simulation-config",aliases = "-s",usage="Filename for simulation configuration", metaVar="<simulation config>")
    // private String simulationConfig = "configs/mediumSimConfig.yml";;

    // @Option (name="--show-visuals", aliases="-v", usage="Show visualisation for simulation")
    // private boolean showVisuals = true;

    // public static void main (String[] args) throws IOException {
    //     Main options = new Main();
    //     CmdLineParser parser = new CmdLineParser(options);

    //     try {
    //         parser.parseArgument(args);
    //     }
    //     catch (CmdLineException c) {
    //         System.out.println("Error parsing command-line arguments.");
    //         System.exit(1);
    //     }

    //     ExperimentConfig experimentConfiguration = new ExperimentConfig(options.getExperimentConfig());
    //     SimConfig simulationConfiguration = new SimConfig(options.getSimulationConfig());

    //     // Default configs
    //     // ExperimentConfig experimentConfiguration = new ExperimentConfig(options.getExperimentConfig());
    //     // SimConfig simulationConfiguration = new SimConfig();

    //     //TODO: work with multiple morphology configs (specifically, filter sensitivities)
    //     // MorphologyConfig morphologyConfig = null;
    //     //
    //     // try {
    //     //     morphologyConfig = new MorphologyConfig(experimentConfiguration.getMorphologyConfigFile());
    //     // }
    //     // catch(ParseException p) {
    //     //     System.out.println("Error parsing morphology file.");
    //     //     p.printStackTrace();
    //     // }

    //     //if we need to show a visualisation
    //     if (options.showVisuals()) {
    //         //UGUGGHGHUHGHGGH, this is just with chasing phenotype, no ML stuff

    //         HomogeneousRobotFactory robotFactory = new HomogeneousRobotFactory(
    //                 new ChasingPhenotype_simple(), simulationConfiguration.getRobotMass(),
    //                 simulationConfiguration.getRobotRadius(), simulationConfiguration.getRobotColour(),
    //                 simulationConfiguration.getObjectsRobots());

    //         // Simulation simulation = new Simulation(simulationConfiguration, robotFactory);

    //         SimulationGUI video =
    //                 new SimulationGUI(simulation);

    //         //new console which displays this simulation
    //         Console console = new Console(video);
    //         console.setVisible(true);

    //     }
    //     else {

    //         //headless option

    //     }

    // }

    // public String getExperimentConfig() { return experimentConfig; }
    // public String getSimulationConfig() { return simulationConfig; }
    // public boolean showVisuals() { return showVisuals; }


}

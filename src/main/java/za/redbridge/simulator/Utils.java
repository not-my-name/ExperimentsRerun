package za.redbridge.simulator;

import org.jbox2d.common.MathUtils;
import org.jbox2d.common.Vec2;

import java.util.Random;

import ec.util.MersenneTwisterFast;
import sim.util.Double2D;

//==============================
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import za.redbridge.simulator.Simulation;
import za.redbridge.simulator.factories.SimulationFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * Created by jamie on 2014/08/01.
 */
public final class Utils {

    public static final double TWO_PI = Math.PI * 2;
    public static final double EPSILON = 1e-6;

    public static final Random RANDOM = new Random();

    private static final Logger log = LoggerFactory.getLogger(Utils.class);

    private static String directoryName;

    private Utils() {
    }

    public static void setDirectoryName(String newDirName) {
        directoryName = newDirName;
    }

    public static double randomRange(MersenneTwisterFast rand, double from, double to) {
        if (from >= to) {
            throw new IllegalArgumentException("`from` must be less than `to`");
        }

        double range = to - from;
        return rand.nextDouble() * range + from;
    }

    public static float randomRange(MersenneTwisterFast rand, float from, float to) {
        if (from >= to) {
            throw new IllegalArgumentException("`from` must be less than `to`");
        }

        float range = to - from;
        return rand.nextFloat() * range + from;
    }

    public static float randomUniformRange(float from, float to) {

        Random rand = new Random();

        if (from >= to) {
            throw new IllegalArgumentException("`from` must be less than `to`");
        }

        float range = to - from;
        return rand.nextFloat() * range + from;
    }

    public static Vec2 toVec2(Double2D double2D) {
        return new Vec2((float) double2D.x, (float) double2D.y);
    }

    public static Double2D toDouble2D(Vec2 vec2) {
        return new Double2D(vec2.x, vec2.y);
    }

    /** Wrap an angle between (-PI, PI] */
    public static double wrapAngle(double angle) {
        angle %= TWO_PI;
        if (angle > Math.PI) {
            angle -= TWO_PI;
        } else if (angle <= -Math.PI) {
            angle += TWO_PI;
        }
        return angle;
    }

    public static boolean isNearlyZero(double x) {
        return x > -EPSILON && x < EPSILON;
    }

    public static Vec2 jitter(Vec2 vec, float magnitude) {
        if (vec != null) {
            vec.x += magnitude * RANDOM.nextFloat() - magnitude / 2;
            vec.y += magnitude * RANDOM.nextFloat() - magnitude / 2;
            return vec;
        }
        return null;
    }

    //=================================================================================
    //copied over from last years Utils.java

    /** Get a random angle in the range [-PI / 2, PI / 2] */
    public static float randomAngle(MersenneTwisterFast random) {
        return MathUtils.TWOPI * random.nextFloat() - MathUtils.PI;
    }

    /** Check if a String is either null or empty. */
    public static boolean isBlank(String s) {
        return s == null || s.isEmpty();
    }

    /**
     * Reads a serialized object instance from a file.
     * @param filepath the String representation of the path to the file
     * @return the deserialized object, or null if the object could not be deserialised
     */
   /* public static Object readObjectFromFile(String filepath) {
        Path path = Paths.get(filepath);
        Object object = null;
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(path))) {
            object = in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            log.error("Unable to load object from file", e);
        }
        return object;
    }*/

    public static void saveObjectToFile(Serializable object, String filepath) {
        Path path = Paths.get(filepath);
        saveObjectToFile(object, path);
    }

    public static void saveObjectToFile(Serializable object, Path path) {
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(path))) {
            out.writeObject(object);
        } catch (IOException e) {
            log.error("Unable to save object to file", e);
        }
    }

    public static Path getLoggingDirectory() {
        String hostname = getLocalHostName();
        if (hostname == null) {
            hostname = "unknown";
        }

        //String objectiveFolder = "/ObjectiveResults/Schema_1/";
        //String objectiveFolder = "/ObjectiveResults/Schema_2/";
        //String objectiveFolder = "/ObjectiveResults/Schema_3/";

        return Paths.get("results", directoryName);

        // String date = new SimpleDateFormat("yyyyMMdd'T'HHmm").format(new Date());
        //
        // String HexArrayCounter = System.getenv().get("PBS_ARRAYID");
        //
        // return Paths.get("results", hostname + "-" + date + "_" + HexArrayCounter);
    }

    public static String getLocalHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (IOException e) {
            log.error("Unable to query host name", e);
        }
        return null;
    }

    /** Check if a String is either null or empty. */
    /*public static boolean isBlank(String s) {
        return s == null || s.isEmpty();
    }*/

    /**
     * Reads a serialized object instance from a file.
     * @param filepath the String representation of the path to the file
     * @return the deserialized object, or null if the object could not be deserialised
     */
    public static Object readObjectFromFile(String filepath) {
        Path path = Paths.get(filepath);
        Object object = null;
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(path))) {
            object = in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            log.error("Unable to load object from file", e);
        }
        return object;
    }

    /*public static void saveObjectToFile(Serializable object, String filepath) {
        Path path = Paths.get(filepath);
        saveObjectToFile(object, path);
    }

    public static void saveObjectToFile(Serializable object, Path path) {
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(path))) {
            out.writeObject(object);
        } catch (IOException e) {
            log.error("Unable to save object to file", e);
        }
    }*/

    /*public static Path getLoggingDirectory() {
        String hostname = getLocalHostName();
        if (hostname == null) {
            hostname = "unknown";
        }

        String date = new SimpleDateFormat("yyyyMMdd'T'HHmm").format(new Date());

        String method = "HyperNEAT";
        //if (Main.NEAT_EVOLUTION)method = "NEAT";
        //else method = "SANE";
        //return Paths.get("results", hostname + "-" + date);
        String HexArrayCounter = System.getenv().get("PBS_ARRAYID");
        return Paths.get("results", "Hex" + "-" + date + "_" + HexArrayCounter+"_"+Main.RES_CONFIG+"_"+method);
    }*/

    /*public static String getLocalHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (IOException e) {
            log.error("Unable to query host name", e);
        }
        return null;
    }*/


}

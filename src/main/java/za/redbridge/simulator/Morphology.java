package za.redbridge.simulator;

import org.yaml.snakeyaml.Yaml;
import za.redbridge.simulator.sensor.AgentSensor;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Morphology{
    private final List<AgentSensor> sensorList;
    private final int numSensors;

    //total number of readings provided by this morphology
    private final int totalReadingSize;
    private Map<String,Object> yamlCache;

    public Morphology(List<AgentSensor> sensorList, int numSensors) {
        this.sensorList = sensorList;
        this.numSensors = numSensors;

        int readSize = 0;

        for (AgentSensor sensor: sensorList) {
            readSize += sensor.getReadingSize();
        }

        totalReadingSize = readSize;
    }

    public List<AgentSensor> getSensorList() { return sensorList; }

    public int getTotalReadingSize() { return totalReadingSize; }

    public int getNumSensors() { return numSensors; }

    public Morphology clone() {
        List<AgentSensor> newSensorList = new ArrayList<>();
        for (AgentSensor sensor: sensorList) {
            newSensorList.add(sensor.clone());
        }
        return new Morphology(newSensorList, numSensors);
    }

    public void dumpMorphology(String filename) {

        Map<String,Object> yamlDump = new HashMap<>();
        Map<String,Object> meta = new HashMap<>();
        meta.put("numSensors", new Integer(sensorList.size()));
        yamlDump.put("meta", meta);

        int sensorID = 1;
        for (AgentSensor sensor: sensorList) {

            Map<String,Object> sensorMap = sensor.getAdditionalConfigs();
            Map<String,Object> thisMap = new HashMap<>();

            thisMap.put("type", sensor.getClass().getName());
            thisMap.put("readingSize", sensor.getReadingSize());
            thisMap.put("bearing", Math.toDegrees(sensor.getBearing()));
            thisMap.put("orientation", Math.toDegrees(sensor.getOrientation()));
            thisMap.put("fieldOfView", Math.toDegrees(sensor.getFieldOfView()));
            thisMap.put("range", sensor.getRange());

            for (Map.Entry<String,Object> entry: sensorMap.entrySet()) {

                try {
                    Field field = sensor.getClass().getDeclaredField(entry.getKey());

                    try {

                        field.setAccessible(true);
                        thisMap.put(entry.getKey(), field.get(sensor));
                    }
                    catch(IllegalAccessException i) {
                        System.out.println("Illegal access of field " + entry.getKey());
                    }
                }
                catch (NoSuchFieldException n) {
                    //System.out.println("No such field: " + entry.getKey() + " " + sensorID);
                }
            }

            yamlDump.put(sensorID+"s", thisMap);
            sensorID++;
        }

        Yaml yaml = new Yaml();
        StringWriter stringWriter = new StringWriter();
        FileWriter fileWriter = null;

        try {
            fileWriter = new FileWriter(filename);
            yaml.dump(yamlDump, stringWriter);
            fileWriter.write(stringWriter.toString());
            //System.out.println(stringWriter.toString());
            fileWriter.close();
        }
        catch (IOException e) {
            System.out.println("Error dumping morphology.");
            e.printStackTrace();
            System.exit(-1);
        }
    }
}

package za.redbridge.simulator.config;

import org.yaml.snakeyaml.Yaml;
import za.redbridge.simulator.config.Config;
import za.redbridge.simulator.sensor.AgentSensor;
import za.redbridge.simulator.Morphology;

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
import za.redbridge.simulator.khepera.*;

/**
 * Created by shsu on 2014/09/08.
 */
public class MorphologyConfig extends Config {
    private Map<String,Object> yamlCache;
    private int morphNum;
    private ArrayList<Morphology> morphologyList;

    public MorphologyConfig(String filepath) throws ParseException {
        morphologyList = new ArrayList<Morphology>();
        morphNum = 0;

        Yaml yaml = new Yaml();
        Map<String, Object> config = null;

        try (Reader reader = Files.newBufferedReader(Paths.get(filepath))) {
            config = (Map<String, Object>) yaml.load(reader);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Map meta = (Map) config.get("meta");
        if (checkFieldPresent(meta, "meta")) {
            // Number of morphologies
            Number noMorph = (Number) meta.get("numMorph");
            if (checkFieldPresent(noMorph, "meta:numMorph")) {
                morphNum = noMorph.intValue();
            }
            else {
                throw new ParseException("Error: Number of morphologies not found.", 0);
            }
        }

        for(int i=1;i<=morphNum;i++){
            ArrayList<AgentSensor> sensorList = new ArrayList<>();
            int sensors = 0;
            int totalReadingSize = 0;

            String id = "m"+ i;
            Map morph = (Map) config.get(id);
            if (checkFieldPresent(morph,id)){
                Map sensorMeta = (Map) morph.get("meta");
                // change here
                Number noSensors = (Number) sensorMeta.get("numSensor");
                sensors = noSensors.intValue();
            }

            for (int j=1;j<=sensors;j++) {
                String sid = "s"+j;
                String type = null;
                float bearing, orientation, fieldOfView, range;

                AgentSensor agentSensor = null;

                Map sensor = (Map) morph.get(sid);
                if (checkFieldPresent(sensor, sid)) {
                    Number bear = (Number) sensor.get("bearing");
                    if (checkFieldPresent(bear, sid + ":bearing")) {
                        bearing = bear.floatValue();
                    }
                    else {
                        throw new ParseException("No bearing found for sensor " + sid, j);
                    }

                    Number orient = (Number) sensor.get("orientation");
                    if (checkFieldPresent(orient, sid + ":orientation")) {
                        orientation = orient.floatValue();
                    }
                    else {
                        throw new ParseException("No orientation found for sensor " + sid, j);
                    }

                    type = (String) sensor.get("type");
                    if (checkFieldPresent(type, sid+":type")) {
                        if(type.equalsIgnoreCase("ProximitySensor")){
                            agentSensor = new ProximitySensor((float) Math.toRadians(bearing), orientation, 3);
                        }
                        else if(type.equalsIgnoreCase("BottomProximitySensor")){
                            agentSensor = new BottomProximitySensor();
                        }
                        else if(type.equalsIgnoreCase("UltrasonicSensor")){
                            agentSensor = new UltrasonicSensor((float) Math.toRadians(bearing), orientation, 3);
                        }
                        else if(type.equalsIgnoreCase("ColourProximitySensor")){
                            agentSensor = new ColourProximitySensor((float) Math.toRadians(bearing), orientation, 3);
                        }
                        else if(type.equalsIgnoreCase("ColourRangedSensor")){
                            agentSensor = new ColourRangedSensor((float) Math.toRadians(bearing), orientation, 4);
                        }
                        else if(type.equalsIgnoreCase("LowResCameraSensor")){
                            agentSensor = new LowResCameraSensor((float) Math.toRadians(bearing), orientation, 1);
                        }
                        else if(type.equalsIgnoreCase("ConstructionSensor")){
                            agentSensor = new ConstructionSensor((float) Math.toRadians(bearing), orientation, 4);
                        }

                    }
                    else {
                        throw new ParseException("Error: No sensor type found. ", j);
                    }
                }
                else {
                    throw new ParseException("Error: " + j + " Sensor not found.", j);
                }
                sensorList.add(agentSensor);
            }
            Morphology morphology = new Morphology(sensorList, sensors);
            morphologyList.add(morphology);
        }

        yamlCache = config;
    }

    public int getMorphNumber(){
        return morphNum;
    }

    public Morphology getMorphology(int i){
        return morphologyList.get(i-1);
    }
}

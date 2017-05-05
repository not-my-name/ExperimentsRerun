package za.redbridge.simulator.khepera;

import za.redbridge.simulator.object.ResourceObject;
import za.redbridge.simulator.object.RobotObject;
import za.redbridge.simulator.object.WallObject;
import za.redbridge.simulator.portrayal.ConePortrayal;
import za.redbridge.simulator.portrayal.Portrayal;
import za.redbridge.simulator.sensor.AgentSensor;
import za.redbridge.simulator.sensor.sensedobjects.SensedObject;

import java.awt.*;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

/**
 * Created by jae on 2015/10/10.
 * Sensor responsible for detecting the type of object
 */
public class ColourRangedSensor extends AgentSensor
{
    private static final float COLOR_SENSOR_RANGE = 3.0f;
    private static final float COLOR_SENSOR_FOV = 1.5f; // This is a guess

    public static final float RANGE = 3.0f;
    public static final float FIELD_OF_VIEW = 1.5f; // This is a guess

    private static final Paint color = new Color(255, 4, 187, 112);
    private int readingSize;

    //constructor
    public ColourRangedSensor(float bearing, float orientation, int readingSize)
    {
        this(bearing, orientation, COLOR_SENSOR_RANGE, COLOR_SENSOR_FOV, readingSize);
    }

    public ColourRangedSensor(float bearing, float orientation, float range, float fieldOfView, int readingSize)
    {
        super(bearing, orientation, range, fieldOfView);
        this.readingSize = readingSize;
    }

    /**
     * Converts a list of objects that have been determined to fall within the sensor's range into
     * a list of readings in the range [0.0, 1.0].
     *
     * @param sensedObjects the objects in the sensor's field, *sorted by distance*
     * @param output  the output vector for this sensor. Write the sensor output to this list (which
     */
    @Override
    protected void provideObjectReading(List<SensedObject> sensedObjects, List<Double> output)
    {
        if (!sensedObjects.isEmpty()) {
            for(int i=0;i<readingSize;i++){
                if(i<sensedObjects.size()){
                    //returns higher number for obstacles and trash
                    SensedObject closest = sensedObjects.get(i);
                    if (closest.getObject() instanceof ResourceObject)
                    {
                        // System.out.println("Resource detected");
                        ResourceObject temp = (ResourceObject) closest.getObject();
                        if(temp.getType().equals("A")){
                            output.add(0.2);
                            // System.out.println("Type A detected");
                        }
                        else if(temp.getType().equals("B")){
                            output.add(0.4);
                            // System.out.println("Type B detected");
                        }
                        else if(temp.getType().equals("C")){
                            output.add(0.6);
                            // System.out.println("Type C detected");
                        }
                    }
                    else if (closest.getObject() instanceof RobotObject)
                    {
                        output.add(0.8);
                    }
                    else if (closest.getObject() instanceof WallObject)
                    {
                        output.add(1.0);
                    }
                    else output.add(0.0);
                }
                else{
                    output.add(0.0);
                }
            }
        }
        else
        {
            for(int i=0;i<readingSize;i++){
                output.add(0.0);
            }
        }
        // System.out.println("Colour ranged sensor expected: "+readingSize);
        // System.out.println("Colour ranged sensor: "+output.size());
    }

    @Override
    public int getReadingSize()
    {
        return readingSize;
    }

    @Override
    public void readAdditionalConfigs(Map<String, Object> stringObjectMap) throws ParseException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Object> getAdditionalConfigs()
    {
        return null;
    }

    @Override
    public AgentSensor clone() {
        return new ColourRangedSensor(bearing, orientation, range, fieldOfView, readingSize);
    }

    @Override
    protected Portrayal createPortrayal() {
        return new ConePortrayal(range, fieldOfView, color);
    }
}

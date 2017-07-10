package za.redbridge.simulator.khepera;

import org.apache.commons.math3.distribution.GammaDistribution;

import java.awt.*;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

import za.redbridge.simulator.object.WallObject;
import za.redbridge.simulator.portrayal.ConePortrayal;
import za.redbridge.simulator.portrayal.Portrayal;

import za.redbridge.simulator.sensor.AgentSensor;
import za.redbridge.simulator.sensor.sensedobjects.SensedObject;

/**
 * A rough estimation of the ultrasonic sensor used in the Khepera III robot: the Vishay TCRT5000
 * Created by jamie on 2014/09/23.
 */
public class ProximitySensor extends AgentSensor {


    // Naeem : there is no lower range for the ProximitySensor.
    private static final float PROXIMITY_SENSOR_RANGE = 1.0f;
    private static final float PROXIMITY_SENSOR_FOV = 0.2f; // This is a guess

//    public static final float RANGE = 0.2f;
    public static final float RANGE = 1.0f;
    public static final float FIELD_OF_VIEW = 0.2f; // This is a guess

    private static final Paint color = new Color(255, 0, 0, 50);

    private final GammaDistribution function = new GammaDistribution(2.5, 2.0);
    private int readingSize;

    public ProximitySensor(float bearing, float orientation, int readingSize) {
        this(bearing, orientation, PROXIMITY_SENSOR_RANGE, PROXIMITY_SENSOR_FOV, readingSize);
    }

    public ProximitySensor(float bearing, float orientation, float range, float fieldOfView, int readingSize) {
        super(bearing, orientation, range, fieldOfView);
        this.readingSize = readingSize;
    }

    @Override
    protected void provideObjectReading(List<SensedObject> sensedObjects, List<Double> output) {
        if (!sensedObjects.isEmpty())
        {
            for(int i=0;i<readingSize;i++){
                if(i<sensedObjects.size()){
                    SensedObject closestObject = sensedObjects.get(i);
                    float closestDistance = closestObject.getDistance();
                    double value = 1.0 - closestDistance / PROXIMITY_SENSOR_RANGE;

                    if(value < 0) value = 0;
                    else if(value >1) value = 1;
                    output.add(value);
                }
                else{
                    output.add(0.0);
                }
            }
        } else {
            for(int i=0;i<readingSize;i++){
                output.add(0.0);
            }
        }
        // System.out.println("Proximity sensor expected: "+readingSize);
        // System.out.println("Proximity sensor: "+output.size());
    }

    @Override
    public AgentSensor clone() {
        return new ProximitySensor(bearing, orientation, range, fieldOfView, readingSize);
    }

    @Override
    public int getReadingSize() {
        return this.readingSize;
    }

    @Override
    public void readAdditionalConfigs(Map<String, Object> stringObjectMap) throws ParseException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, Object> getAdditionalConfigs() {
        return null;
    }

    protected double readingCurve(float distance) {
        // Output curve of the TCRT5000 seems to produce something like a Gamma distribution curve
        // See the datasheet for more information
        return Math.min(function.density(distance * 1000) * 6.64, 1.0);
    }

    @Override
    protected Portrayal createPortrayal() {
        return new ConePortrayal(range, fieldOfView, color);
    }

}

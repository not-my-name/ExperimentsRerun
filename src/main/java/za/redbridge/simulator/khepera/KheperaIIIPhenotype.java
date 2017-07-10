package za.redbridge.simulator.khepera;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import za.redbridge.simulator.phenotype.Phenotype;
import za.redbridge.simulator.sensor.AgentSensor;

/**
 * Phenotype that mimics the morphology of a Khepera III robot.
 * Created by jamie on 2014/09/22.
 */


// (Naeem) We need to copy all the configurations for the other sensors and add them for the colourproximitysensor
// I also noticed that the number of sensors is found here.
// I have already configured this class to work with the ColourProximitySensor! *** NB ***


public abstract class KheperaIIIPhenotype implements Phenotype {

    private final List<AgentSensor> sensors;

    private final Configuration configuration;

    /**
     * Constructor used for phenotypes that make use of {@link #configure(Map)}. Sensors will not
     * be initialized (i.e. there will be no sensors) if this constructor is used until the
     * phenotype is configured.
     * TODO: Implement configure
     */
    public KheperaIIIPhenotype() {
        configuration = new Configuration();
        sensors = new ArrayList<>();
    }

    /**
     * Default constructor for this phenotype. Sensors will be configured according to
     * configuration.
     */
    public KheperaIIIPhenotype(Configuration config) {
        configuration = new Configuration(config);
        sensors = new ArrayList<>(config.getNumberOfSensors());
        initSensors();
    }

    private void initSensors() {
        // Proximity sensors
        if (configuration.enableProximitySensor45Degrees) {
            sensors.add(createProximitySensor((float) Math.toRadians(45), 0f));}

        if (configuration.enableProximitySensor90Degrees) {
            sensors.add(createProximitySensor((float) Math.toRadians(90), 0f));}

        if (configuration.enableProximitySensor135Degrees) {
            sensors.add(createProximitySensor((float) Math.toRadians(135), 0f));}

        if (configuration.enableProximitySensor180Degrees) {
            sensors.add(createProximitySensor((float) Math.toRadians(180), 0f));}

        if (configuration.enableProximitySensor225Degrees) {
            sensors.add(createProximitySensor((float) Math.toRadians(-45), 0f));}

        if (configuration.enableProximitySensor270Degrees) {
            sensors.add(createProximitySensor((float) Math.toRadians(-90), 0f));}

        if (configuration.enableProximitySensor315Degrees) {
            sensors.add(createProximitySensor((float) Math.toRadians(-135), 0f));}

        if (configuration.enableProximitySensor360Degrees) {
            sensors.add(createProximitySensor((float) Math.toRadians(0), 0f));}


        // Bottom Proximity Sensos
        if (configuration.enableBottomProximitySensor) {
            sensors.add(createBottomProximitySensor());}

        // Ultrasonic sensors
        if (configuration.enableUltrasonicSensor60Degrees) {
            sensors.add(createUltrasonicSensor((float) Math.toRadians(60), 0f));}

        if (configuration.enableUltrasonicSensor120Degrees) {
        sensors.add(createUltrasonicSensor((float) Math.toRadians(120), 0f));}

        if (configuration.enableUltrasonicSensor180Degrees) {
        sensors.add(createUltrasonicSensor((float) Math.toRadians(180), 0f));}

        if (configuration.enableUltrasonicSensor240Degrees) {
        sensors.add(createUltrasonicSensor((float) Math.toRadians(-60), 0f));}

        if (configuration.enableUltrasonicSensor300Degrees) {
            sensors.add(createUltrasonicSensor((float) Math.toRadians(-120), 0f));}

        if (configuration.enableUltrasonicSensor360Degrees) {
            sensors.add(createUltrasonicSensor((float) Math.toRadians(0), 0f));}


        if (configuration.enableColourProximitySensor180Degrees){
            sensors.add(createColourProximitySensor((float) Math.toRadians(180), 0f));}

        if (configuration.enableColourProximitySensor360Degrees){
            sensors.add(createColourProximitySensor((float) Math.toRadians(0), 0f));}


    }

    /** Method can be overridden to customize proximity sensor */
    protected AgentSensor createProximitySensor(float bearing, float orientation) {
        return new ProximitySensor(bearing, orientation, 1);
    }

    /** Method can be overridden to customize bottom proximity sensor */
    protected AgentSensor createBottomProximitySensor() {
        return new BottomProximitySensor();
    }

    /** Method can be overridden to customize ultrasonic sensor */
    protected AgentSensor createUltrasonicSensor(float bearing, float orientation) {
        return new UltrasonicSensor(bearing, orientation, 1);
    }

    /** Method can be overridden to customize colour proximity sensor */
    protected AgentSensor createColourProximitySensor(float bearing, float orientation){
        return new ColourProximitySensor(bearing, orientation, 1);
    }

    /** Returns a copy of the current configuration */
    public Configuration getConfiguration() {
        return new Configuration(configuration);
    }

    @Override
    public List<AgentSensor> getSensors() {
        return sensors;
    }

    @Override
    public KheperaIIIPhenotype clone() throws UnsupportedOperationException {
        throw new UnsupportedOperationException(
                "This is an abstract class, implement clone in your subclass");
    }

    @Override
    public void configure(Map<String, Object> phenotypeConfigs) {
        // TODO: Implementation
        //initSensors();
    }

    /**
     * Configuration for the Khepera III phenotype that allows sensors to be enabled or disabled.
     * By default all sensors are disabled.
     */


    // 360 Degrees will be configured as 0 Degrees
    // 225 will  be configured as -45 degress.
    // if(degrees > 180) : degrees = 180 - degrees.

    public static class Configuration {

        // Proximity Sensors
        public boolean enableProximitySensor45Degrees = false;
        public boolean enableProximitySensor90Degrees = false;
        public boolean enableProximitySensor135Degrees = false;
        public boolean enableProximitySensor180Degrees = false;

        public boolean enableProximitySensor225Degrees = false;
        public boolean enableProximitySensor270Degrees = false;
        public boolean enableProximitySensor315Degrees = false;
        public boolean enableProximitySensor360Degrees = false;

        // Ultrasonic Sensors
        public boolean enableUltrasonicSensor60Degrees = false;
        public boolean enableUltrasonicSensor120Degrees = false;
        public boolean enableUltrasonicSensor180Degrees = false;

        public boolean enableUltrasonicSensor240Degrees = false;
        public boolean enableUltrasonicSensor300Degrees = false;
        public boolean enableUltrasonicSensor360Degrees = false;

        // Colour Proximity Sensor
        public boolean enableColourProximitySensor180Degrees = false;
        public boolean enableColourProximitySensor360Degrees = false;

        public boolean enableBottomProximitySensor = false;

        public Configuration() {
        }

        private Configuration(Configuration other) {

            // Proximity Sensors
            this.enableProximitySensor45Degrees = other.enableProximitySensor45Degrees;
            this.enableProximitySensor90Degrees = other.enableProximitySensor90Degrees;
            this.enableProximitySensor135Degrees = other.enableProximitySensor135Degrees;
            this.enableProximitySensor180Degrees = other.enableProximitySensor180Degrees;

            this.enableProximitySensor225Degrees = other.enableProximitySensor225Degrees;
            this.enableProximitySensor270Degrees = other.enableProximitySensor270Degrees;
            this.enableProximitySensor315Degrees = other.enableProximitySensor315Degrees;
            this.enableProximitySensor360Degrees = other.enableProximitySensor360Degrees;

            // Ultrasonic Sensors
            this.enableUltrasonicSensor60Degrees = other.enableUltrasonicSensor60Degrees;
            this.enableUltrasonicSensor120Degrees = other.enableUltrasonicSensor120Degrees;
            this.enableUltrasonicSensor180Degrees = other.enableUltrasonicSensor180Degrees;

            this.enableUltrasonicSensor240Degrees = other.enableUltrasonicSensor240Degrees;
            this.enableUltrasonicSensor300Degrees = other.enableUltrasonicSensor300Degrees;
            this.enableUltrasonicSensor360Degrees = other.enableUltrasonicSensor360Degrees;

            // Colour Proximity Sensors
            this.enableColourProximitySensor180Degrees = other.enableColourProximitySensor180Degrees;
            this.enableColourProximitySensor360Degrees = other.enableColourProximitySensor360Degrees;

            // Bottom Proximity Sensor
            this.enableBottomProximitySensor = other.enableBottomProximitySensor;

        }

        public int getNumberOfSensors() {
            int numSensors = 0;

            // Proximity Sensors
            if(enableProximitySensor45Degrees) numSensors += 1;
            if(enableProximitySensor90Degrees) numSensors += 1;
            if(enableProximitySensor135Degrees) numSensors += 1;
            if(enableProximitySensor180Degrees) numSensors += 1;

            if(enableProximitySensor225Degrees) numSensors += 1;
            if(enableProximitySensor270Degrees) numSensors += 1;
            if(enableProximitySensor315Degrees) numSensors += 1;
            if(enableProximitySensor360Degrees) numSensors += 1;

            // Ultrasonic Sensors
            if(enableUltrasonicSensor60Degrees) numSensors += 1;
            if(enableUltrasonicSensor120Degrees) numSensors += 1;
            if(enableUltrasonicSensor180Degrees) numSensors += 1;

            if(enableUltrasonicSensor240Degrees) numSensors += 1;
            if(enableUltrasonicSensor300Degrees) numSensors += 1;
            if(enableUltrasonicSensor360Degrees) numSensors += 1;

            // Colour Proximity Sensors
            if(enableColourProximitySensor180Degrees) numSensors += 1;
            if(enableColourProximitySensor360Degrees) numSensors += 1;

            // Bottom Proximity Sensor
            if(enableBottomProximitySensor) numSensors +=1;

            return numSensors;
        }
    }

}

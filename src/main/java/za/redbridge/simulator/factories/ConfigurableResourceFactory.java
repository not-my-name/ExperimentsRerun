package za.redbridge.simulator.factories;

import org.jbox2d.dynamics.World;
import za.redbridge.simulator.PlacementArea;
import za.redbridge.simulator.config.Config;
import za.redbridge.simulator.object.ResourceObject;

import java.io.BufferedReader;
import java.util.Scanner;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import org.jbox2d.common.Vec2;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by shsu on 2014/08/29.
 */
public class ConfigurableResourceFactory extends Config implements ResourceFactory {

    private static final int DEFAULT_LARGE_QUANTITY = 10;
    private static final int DEFAULT_MEDIUM_QUANTITY = 15;
    private static final int DEFAULT_SMALL_QUANTITY = 15;

    private static final float DEFAULT_SMALL_WIDTH = 0.4f;
    private static final float DEFAULT_SMALL_HEIGHT = 0.4f;
    private static final float DEFAULT_SMALL_MASS = 1.0f;
    private static final int DEFAULT_SMALL_PUSHING_BOTS = 1;
    private static final double DEFAULT_SMALL_VALUE = 1;

    private static final float DEFAULT_MEDIUM_WIDTH = 0.6f;
    private static final float DEFAULT_MEDIUM_HEIGHT = 0.6f;
    private static final float DEFAULT_MEDIUM_MASS = 3.0f;
    private static final int DEFAULT_MEDIUM_PUSHING_BOTS = 2;
    private static final double DEFAULT_MEDIUM_VALUE = 2;

    private static final float DEFAULT_LARGE_WIDTH = 0.8f;
    private static final float DEFAULT_LARGE_HEIGHT = 0.8f;
    private static final float DEFAULT_LARGE_MASS = 5.0f;
    private static final int DEFAULT_LARGE_PUSHING_BOTS = 3;
    private static final double DEFAULT_LARGE_VALUE = 3;

    private static final String TYPE_FIRST = "A";
    private static final String TYPE_SECOND = "B";
    private static final String TYPE_THIRD = "C";

    private ResourceSpec smallResourceSpec;
    private ResourceSpec mediumResourceSpec;
    private ResourceSpec largeResourceSpec;

    private ArrayList<ResourceObject> placedResources;

    public ConfigurableResourceFactory() {
        smallResourceSpec = new ResourceSpec(DEFAULT_SMALL_QUANTITY, DEFAULT_SMALL_WIDTH,
                DEFAULT_SMALL_HEIGHT, DEFAULT_SMALL_MASS, DEFAULT_SMALL_PUSHING_BOTS,
                DEFAULT_SMALL_VALUE, TYPE_FIRST);

        mediumResourceSpec = new ResourceSpec(DEFAULT_MEDIUM_QUANTITY, DEFAULT_MEDIUM_WIDTH,
                DEFAULT_MEDIUM_HEIGHT, DEFAULT_MEDIUM_MASS, DEFAULT_MEDIUM_PUSHING_BOTS,
                DEFAULT_MEDIUM_VALUE, TYPE_SECOND);

        largeResourceSpec = new ResourceSpec(DEFAULT_LARGE_QUANTITY, DEFAULT_LARGE_WIDTH,
                DEFAULT_LARGE_HEIGHT, DEFAULT_LARGE_MASS, DEFAULT_LARGE_PUSHING_BOTS,
                DEFAULT_LARGE_VALUE, TYPE_THIRD);

        placedResources = new ArrayList<ResourceObject>();
    }

    @Override
    public void placeInstances(PlacementArea.ForType<ResourceObject> placementArea, World world) {
        // Place resources randomly
        
        placedResources.clear();

        placeInstances(smallResourceSpec, placementArea, world);
        placeInstances(mediumResourceSpec, placementArea, world);
        placeInstances(largeResourceSpec, placementArea, world);

        // Place test resources
        // placeTestInstances(smallResourceSpec, mediumResourceSpec, largeResourceSpec, placementArea, world, "robot_pushing");
    }

    private void placeInstances(ResourceSpec spec, PlacementArea.ForType<ResourceObject> placementArea, World world) {

        for (int i = 0; i < spec.quantity; i++) {
            PlacementArea.Space space = placementArea.getRandomRectangularSpace(spec.width, spec.height);
            ResourceObject resource = new ResourceObject(world, space.getPosition(),
                    space.getAngle(), spec.width, spec.height, spec.mass, spec.pushingBots,
                    spec.value, spec.type);
            placedResources.add(resource);
            placementArea.placeObject(space, resource);
        }
    }

    private void placeTestInstances(ResourceSpec smallSpec, ResourceSpec mediumSpec, ResourceSpec largeSpec,PlacementArea.ForType<ResourceObject> placementArea, World world, String demo) {
        Vec2 pos1 = new Vec2(0,0);
        Vec2 pos2 = new Vec2(0,0);
        Vec2 pos3 = new Vec2(0,0);
        Vec2 pos4 = new Vec2(0,0);
        Vec2 pos5 = new Vec2(0,0);


        if(demo.equals("robot_pushing")){
            pos1 = new Vec2(8,7);
            pos2 = new Vec2(11,7);
            pos3 = new Vec2(10,10);
            pos4 = new Vec2(13,10);
        }
        else if(demo.equals("welding0")){
            pos1 = new Vec2(10,8);
            pos2 = new Vec2(10,10);
        }
        else if(demo.equals("welding1")){
            pos1 = new Vec2(9.25f,8);
            pos2 = new Vec2(10,8);
        }
        else if(demo.equals("colour_sensor")){

        }

        PlacementArea.Space smallSpace = placementArea.getRectangularSpace(smallSpec.width, smallSpec.height, pos1, 0);
        PlacementArea.Space mediumSpace = placementArea.getRectangularSpace(mediumSpec.width, mediumSpec.height, pos2, 0);
        PlacementArea.Space largeSpace = placementArea.getRectangularSpace(largeSpec.width, largeSpec.height, pos3, 0);
        PlacementArea.Space s1 = placementArea.getRectangularSpace(mediumSpec.width, mediumSpec.height, pos4, 0);
        // PlacementArea.Space s2 = placementArea.getRectangularSpace(largeSpec.width, largeSpec.height, pos5, 0);

        ResourceObject smallResource = new ResourceObject(world, smallSpace.getPosition(),
                smallSpace.getAngle(), smallSpec.width, smallSpec.height, smallSpec.mass, smallSpec.pushingBots,
                smallSpec.value, smallSpec.type);
        ResourceObject mediumResource = new ResourceObject(world, mediumSpace.getPosition(),
                mediumSpace.getAngle(), mediumSpec.width, mediumSpec.height, mediumSpec.mass, mediumSpec.pushingBots,
                mediumSpec.value, mediumSpec.type);
        ResourceObject largeResource = new ResourceObject(world, largeSpace.getPosition(),
                largeSpace.getAngle(), smallSpec.width, smallSpec.height, smallSpec.mass, smallSpec.pushingBots,
                smallSpec.value, smallSpec.type);
        ResourceObject r1 = new ResourceObject(world, s1.getPosition(),
                s1.getAngle(), mediumSpec.width, mediumSpec.height, mediumSpec.mass, mediumSpec.pushingBots,
                mediumSpec.value, mediumSpec.type);

        if(demo.equals("robot_pushing")){
            placementArea.placeObject(smallSpace, smallResource);
            placementArea.placeObject(mediumSpace, mediumResource);
            placementArea.placeObject(largeSpace, largeResource);
            placementArea.placeObject(s1, r1);

            placedResources.add(smallResource);
            placedResources.add(mediumResource);
            placedResources.add(largeResource);
            placedResources.add(r1);
        }
        else if(demo.equals("welding0")){
            placementArea.placeObject(smallSpace, smallResource);
            placementArea.placeObject(mediumSpace, mediumResource);

            placedResources.add(smallResource);
            placedResources.add(mediumResource);
        }
        else if(demo.equals("welding1")){
            placementArea.placeObject(smallSpace, smallResource);
            placementArea.placeObject(mediumSpace, mediumResource);

            placedResources.add(smallResource);
            placedResources.add(mediumResource);
        }
        else if(demo.equals("colour_sensor")){

        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void configure(Map<String, Object> resourceConfigs, String[] resQuantity) {

        Map<String, Object> smallConfig = (Map<String, Object>) resourceConfigs.get("small");
        Map<String, Object> mediumConfig = (Map<String, Object>) resourceConfigs.get("medium");
        Map<String, Object> largeConfig = (Map<String, Object>) resourceConfigs.get("large");

        smallResourceSpec = readConfig(smallConfig, "resourceConfig:small:", DEFAULT_SMALL_VALUE, Integer.parseInt(resQuantity[0]));
        mediumResourceSpec = readConfig(mediumConfig, "resourceConfig:medium:", DEFAULT_MEDIUM_VALUE, Integer.parseInt(resQuantity[1]));
        largeResourceSpec = readConfig(largeConfig, "resourceConfig:large:", DEFAULT_LARGE_VALUE, Integer.parseInt(resQuantity[2]));
    }

    private ResourceSpec readConfig(Map<String, Object> config, String path, double value, int q) {
        int quantity = q;
        int pushingBots = 0;
        float width = 0;
        float height = 0;
        float mass = 0;
        String type = "0";

//        Number quantityField = (Number) config.get("quantity");
//        if (checkFieldPresent(quantityField, path + "quantity")) {
//            quantity = quantityField.intValue();
//        }

        Number widthField = (Number) config.get("width");
        if (checkFieldPresent(widthField, path + "width")) {
            width = widthField.floatValue();
        }

        Number heightField = (Number) config.get("height");
        if (checkFieldPresent(heightField, path + "height")) {
            height = heightField.floatValue();
        }

        Number massField = (Number) config.get("mass");
        if (checkFieldPresent(heightField, path + "mass")) {
            mass = massField.floatValue();
        }

        Number pushingBotsField = (Number) config.get("pushingBots");
        if (checkFieldPresent(pushingBotsField, path + "pushingBots")) {
            pushingBots = pushingBotsField.intValue();
        }

        String typeField = (String) config.get("type");
        if (checkFieldPresent(typeField, path + "type")) {
            type = typeField;
        }

        return new ResourceSpec(quantity, width, height, mass, pushingBots, value, type);
    }

    @Override
    public int getNumberOfResources() {
        return smallResourceSpec.quantity + mediumResourceSpec.quantity
                + largeResourceSpec.quantity;
    }

    @Override
    public double getTotalResourceValue(){
        return smallResourceSpec.getTotalValue() + mediumResourceSpec.getTotalValue() +
                largeResourceSpec.getTotalValue();
    }

    public void setResQuantity(int [] n){
        smallResourceSpec.setQuantity(n[0]);
        mediumResourceSpec.setQuantity(n[1]);
        largeResourceSpec.setQuantity(n[2]);
    }

    public ArrayList<ResourceObject> getPlacedResources(){
        return placedResources;
    }

    private static class ResourceSpec {
        private int quantity;
        private float width;
        private float height;
        private float mass;
        private int pushingBots;
        private double value;
        private String type;

        ResourceSpec(int quantity, float width, float height, float mass, int pushingBots,
                double value, String type) {
            this.quantity = quantity;
            this.width = width;
            this.height = height;
            this.mass = mass;
            this.pushingBots = pushingBots;
            this.value = value;
            this.type = type;
        }

        double getTotalValue() {
            return quantity * value;
        }

        void setQuantity(int n){
            quantity = n;
        }
    }
}

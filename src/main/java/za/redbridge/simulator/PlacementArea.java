package za.redbridge.simulator;

import com.sun.javafx.geom.Vec2d;
import org.jbox2d.collision.AABB;
import org.jbox2d.common.MathUtils;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.Fixture;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import ec.util.MersenneTwisterFast;
import za.redbridge.simulator.object.PhysicalObject;


import static za.redbridge.simulator.Utils.randomAngle;
import static za.redbridge.simulator.Utils.randomRange;
import static za.redbridge.simulator.physics.AABBUtil.createAABB;
import static za.redbridge.simulator.physics.AABBUtil.moveAABB;
import static za.redbridge.simulator.physics.AABBUtil.resizeAABB;

/**
 * Describes an area for placing objects. Typically, an object factory will request some space in
 * the area and the PlacementArea instance will return a {@link Space} object if that space is
 * available. The factory must then confirm the usage of that space by registering the object to be
 * placed with {@link #placeObject(Space, PhysicalObject)}.
 * Created by jamie on 2014/08/21.
 */
public class PlacementArea {

    // The physics engine adds a small margin to objects placed in the world
    private static final float PADDING = 0.03f;

    private static final int MAX_PLACEMENT_TRIES = 1000;

    private final float width;
    private final float height;

    private final MersenneTwisterFast random = new MersenneTwisterFast();

    // Need an ordered map, hence the use of a linked hashmap
    private final Map<PhysicalObject, Space> placements = new LinkedHashMap<>();

    PlacementArea(float width, float height) {
        this.width = width;
        this.height = height;
    }

    double getWidth() {
        return width;
    }

    double getHeight() {
        return height;
    }

    void setSeed(long seed) {
        random.setSeed(seed);
    }

    Space getRandomRectangularSpace(float objectWidth, float objectHeight) {
        AABB aabb = new AABB();
        float angle;
        int tries = 1;
        do {
            if (tries++ >= MAX_PLACEMENT_TRIES) {
                throw new RuntimeException("Unable to find space for object");
            }

            angle = randomAngle(random);
            float sin = MathUtils.abs(MathUtils.sin(angle));
            float cos = MathUtils.abs(MathUtils.cos(angle));
            float width = objectHeight * sin + objectWidth * cos + PADDING;
            float height = objectWidth * sin + objectHeight * cos + PADDING;
            resizeAABB(aabb, width, height);

            float x = randomRange(random, width, this.width - width);
            float y = randomRange(random, height, this.height - height);
            moveAABB(aabb, x, y);
        } while (overlappingWithOtherObject(aabb));

        return new Space(aabb, angle);
    }

    Space getRandomCircularSpace(float objectRadius) {
        float diameter = objectRadius * 2 + PADDING;

        float halfDiameter = diameter / 2;
        float maxX = this.width - halfDiameter;
        float maxY = this.height - halfDiameter;

        AABB aabb = new AABB();
        resizeAABB(aabb, diameter, diameter);
        float angle = randomAngle(random);
        int tries = 1;
        do {
            if (tries++ >= MAX_PLACEMENT_TRIES) {
                throw new RuntimeException("Unable to find space for object");
            }

            float x = randomRange(random, halfDiameter, maxX);
            float y = randomRange(random, halfDiameter, maxY);
            moveAABB(aabb, x, y);
        } while (overlappingWithOtherObject(aabb));

        return new Space(aabb, angle);
    }


    Space getAgentStartingSpace(float objectRadius, String agentLocation){
        float diameter = objectRadius * 2 + PADDING;

        float halfDiameter = diameter / 2;
        float maxX = this.width - halfDiameter;
        float maxY = this.height - halfDiameter;

        AABB aabb = new AABB();
        resizeAABB(aabb, diameter, diameter);
        float angle = MathUtils.PI/2;// randomAngle(random);
        int tries = 1;
        Vec2 startLocation = getLocation(agentLocation,maxX,maxY);
        float x = startLocation.x; float y = startLocation.y;
        do {
            if (tries++ >= MAX_PLACEMENT_TRIES) {
                throw new RuntimeException("Unable to find space for object");
            }
            Vec2 nextPoint = getNextPoint(agentLocation,x,y,diameter);
            x = nextPoint.x;
            y = nextPoint.y;
            moveAABB(aabb, x, y);
        } while (overlappingWithOtherObject(aabb));

        return new Space(aabb, angle);
    }

    /**
     * Convert the user defined location into a vector
     * @param agentLocation the user defined location
     * @param maxX the maximum x coordinate of the area
     * @param maxY the maximum y coordinate of the area
     * @returns the location in vector form
     */
    Vec2 getLocation(String agentLocation, float maxX, float maxY){
        Vec2 location = new Vec2(0.0f, 0.0f);
        switch(agentLocation.toLowerCase()){
            case("nw"):
                location.x = 0;
                location.y = maxY;
                break;
            case("ne"):
                location.x = maxX;
                location.y = maxY;
                break;
            case("se"):
                location.x = maxX;
                location.y = 0;
                break;
            case("sw"):
                location.x = 0;
                location.y = 0;
                break;
            default:
                break;
        }
        return location;
    }

    Vec2 getNextPoint(String agentLocation, float x, float y, float diameter){
        Vec2 newPoint = new Vec2(x,y);
        switch(agentLocation.toLowerCase()){
            case("nw"):
                newPoint.x += (diameter + PADDING);
                newPoint.y -= (diameter + PADDING);
                break;
            case("ne"):
                newPoint.x -= (diameter + PADDING);
                newPoint.y -= (diameter + PADDING);
                break;
            case("se"):
                newPoint.x -= (diameter + PADDING);
                newPoint.y += (diameter + PADDING);
                break;
            case("sw"):
                newPoint.x += (diameter + PADDING);
                newPoint.y += (diameter + PADDING);
                break;
            default:
                break;
        }
        return newPoint;
    }

    Space getRectangularSpace(float objectWidth, float objectHeight, Vec2 position, float angle) {
        float sin = MathUtils.abs(MathUtils.sin(angle));
        float cos = MathUtils.abs(MathUtils.cos(angle));
        float width = objectHeight * sin + objectWidth * cos + PADDING;
        float height = objectWidth * sin + objectHeight * cos + PADDING;

        AABB aabb = createAABB(position.x, position.y, width, height);
        if (overlappingWithOtherObject(aabb)) {
            return null;
        }

        return new Space(aabb, angle);
    }

    Space getCircularSpace(float radius, Vec2 position, float angle) {
        float diameter = radius * 2 + PADDING;
        AABB aabb = createAABB(position.x, position.y, diameter, diameter);
        if (overlappingWithOtherObject(aabb)) {
            return null;
        }

        return new Space(aabb, angle);
    }

    boolean overlappingWithOtherObject(float objectWidth, float objectHeight, Vec2 position,
                                       float angle) {
        float sin = MathUtils.abs(MathUtils.sin(angle));
        float cos = MathUtils.abs(MathUtils.cos(angle));
        float width = objectHeight * sin + objectWidth * cos + PADDING;
        float height = objectWidth * sin + objectHeight * cos + PADDING;
        AABB aabb = createAABB(position.x, position.y, width, height);
        return overlappingWithOtherObject(aabb);
    }

    boolean overlappingWithOtherObject(AABB aabb) {
        for (Space space : placements.values()) {
            if (AABB.testOverlap(aabb, space.aabb)) {
                return true;
            }
        }
        return false;
    }

    void placeObject(Space space, PhysicalObject object) {
        if (space.isUsed()) {
            throw new IllegalArgumentException("Space already used");
        }

        if (overlappingWithOtherObject(space.aabb)) {
            throw new IllegalArgumentException("Placement space is not available");
        }

        if (!space.aabb.contains(getObjectAABB(object))) {
            throw new IllegalArgumentException("Object space does not match placement space");
        }

        placements.put(object, space);
        space.markUsed();
    }

    /**
     * Get the AABB for the given object. Iterates through the fixture list to create the AABB for
     * all the fixtures in the object.
     * @param object the PhysicalObject
     * @return the overall AABB for the object
     */
    private static AABB getObjectAABB(PhysicalObject object) {
        Body body = object.getBody();
        Vec2 position = body.getPosition();
        AABB aabb = new AABB(position, position);

        // Iterate through the fixtures, adding the AABBs for the ones that aren't sensors
        for (Fixture f = body.getFixtureList(); f != null; f = f.getNext()) {
            if (!f.isSensor()) {
                aabb.combine(f.getAABB(0));
            }
        }

        return aabb;
    }

    Set<PhysicalObject> getPlacedObjects() {
        return placements.keySet();
    }

    /**
     * Describes some space available in the PlacementArea
     */
    public static class Space {
        private final AABB aabb;
        private final float angle;

        private boolean used = false;

        private Space(AABB aabb, float angle) {
            this.aabb = aabb;
            this.angle = angle;
        }

        /** Get the center position of the space */
        public Vec2 getPosition() {
            return aabb.getCenter();
        }

        /** Get the width of the space */
        public float getWidth() {
            return aabb.upperBound.x - aabb.lowerBound.x;
        }

        /** Get the height of the space */
        public float getHeight() {
            return aabb.upperBound.y - aabb.lowerBound.x;
        }

        /** Get the angle of the object placed within this space */
        public float getAngle() {
            return angle;
        }

        private boolean isUsed() {
            return used;
        }

        private void markUsed() {
            used = true;
        }

    }

    /**
     * A wrapper for a typed placement area. Simply delegates calls to the underlying
     * PlacementArea while preserving type safety of objects to be added.
     * @param <T> The type that can be placed in this placement area
     */
    public class ForType<T extends PhysicalObject> {
        public ForType() {
        }

        /** Get the width of the placement area */
        public double getWidth() {
            return PlacementArea.this.getWidth();
        }

        /** Get the height of the placement area */
        public double getHeight() {
            return PlacementArea.this.getHeight();
        }

        /**
         * Try and get a placement space for a rectangular object of the given size at the given
         * position. Returns null if insufficient space at position.
         * @param objectWidth width of the object to be placed
         * @param objectHeight height of the object to be placed
         * @param position desired position to be placed
         * @param angle desired angle to be placed at
         * @return Space if available
         */
        public Space getRectangularSpace(float objectWidth, float objectHeight, Vec2 position,
                                         float angle) {
            return PlacementArea.this.getRectangularSpace(objectWidth, objectHeight, position,
                angle);
        }

        /**
         * Try and get a placement space for a circular object of the given size at the given
         * position. Returns null if insufficient space at position.
         * @param objectRadius radius of the object to be placed
         * @param position desired position to be placed
         * @param angle desired angle to be placed at
         * @return Space if available
         */
        public Space getCircularSpace(float objectRadius, Vec2 position, float angle) {
            return PlacementArea.this.getCircularSpace(objectRadius, position, angle);
        }

        /**
         * Get a random placement space of the given size.
         * @param objectWidth width of the object to be placed
         * @param objectHeight height of the object to be placed
         * @return a free random space
         */
        public Space getRandomRectangularSpace(float objectWidth, float objectHeight) {
            return PlacementArea.this.getRandomRectangularSpace(objectWidth, objectHeight);
        }

        /**
         * Get a random placement space of the given size.
         * @param objectRadius radius of the object to be placed
         * @return a free random space
         */
        public Space getRandomCircularSpace(float objectRadius) {
            return PlacementArea.this.getRandomCircularSpace(objectRadius);
        }

        /**
         * Get the next available placement space of the given size.
         * @param objectRadius radius of the object to be placed
         * @return a free space next to occupied space
         */
        public Space getAgentStartingSpace(float objectRadius, String agentLocation) {
            return PlacementArea.this.getAgentStartingSpace(objectRadius, agentLocation);
        }

        /**
         * Check if a rectangular object of the given size at the given position would overlap with
         * another object that has already been placed.
         * @param objectWidth width of the object to be placed
         * @param objectHeight height of the object to be placed
         * @param position position to check
         * @param angle angle of object to be placed
         * @return true if the object would overlap
         */
        public boolean overlappingWithOtherObject(float objectWidth, float objectHeight,
                                                  Vec2 position, float angle) {
            return PlacementArea.this.overlappingWithOtherObject(objectWidth, objectHeight, position, angle);
        }

        /**
         * Register the object a placed in the given space.
         * Will throw exception if space already taken or space is not large enough to contain
         * object. Note that it is possible to ask for more space than is needed for the object to
         * be placed.
         * @param space the space to place the object
         * @param object the object to be placed
         */
        public void placeObject(Space space, T object) {
            PlacementArea.this.placeObject(space, object);
        }
    }

}

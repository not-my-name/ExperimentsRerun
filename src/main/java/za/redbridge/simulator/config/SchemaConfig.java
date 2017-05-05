package za.redbridge.simulator.config;

import org.yaml.snakeyaml.Yaml;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

public class SchemaConfig extends Config{
    private Config [] configs;
    private String [] resourceArray = {"A","B","C","D","E"};

    public SchemaConfig(String filepath, int n, int k){
        Map<String, Object> config = null;
        configs = new Config[n];
        Yaml yaml = new Yaml();
        try (Reader reader = Files.newBufferedReader(Paths.get(filepath))) {
            config = (Map<String, Object>) yaml.load(reader);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Loop through overall schema configs
        for(int i=0;i<n;i++){
            Config newConfig = new Config(k);
            Map schemaConfig1 = (Map) config.get("config"+Integer.toString(i));
            if (checkFieldPresent(schemaConfig1, "config"+Integer.toString(i))) {
                String [] resArray = ((String) schemaConfig1.get("resQuantity")).split(" ");
                newConfig.setResQuantity(resArray);

                //Loop through resource schema
                for(int j=0;j<k;j++){
                    Map resourceA = (Map) schemaConfig1.get("resource"+Integer.toString(j));
                    if(checkFieldPresent(resourceA, "config"+Integer.toString(i)+":resource"+Integer.toString(j))){
                        String [] l = ((String) resourceA.get("left")).split(" ");
                        String [] r = ((String) resourceA.get("right")).split(" ");
                        String [] u = ((String) resourceA.get("up")).split(" ");
                        String [] d = ((String) resourceA.get("down")).split(" ");

                        ResourceSchema resourceScheme = new ResourceSchema(l,r,u,d);
                        newConfig.add(resourceArray[j],resourceScheme);
                    }
                }
            }  
            configs[i] = newConfig;
            // System.out.println(configs[i].getSchemaConfigComplexity());
        }
    }

    public int checkConfig(int i, String type ,String [] adjacent){
        return configs[i].checkSchema(type, adjacent);
    }

    public int[] getIncorrectAdjacentSides (int i, String type, String[] adjacent) {
        return configs[i].getIncorrectAdjacentSides(type, adjacent);
    }

    public int [] getResQuantity(int i){
        return configs[i].getResQuantity();
    }

    public int getTotalResources(int i) {
        int [] rq = configs[i].getResQuantity();
        return rq[0] + rq[1] + rq[2];
    }

    public int getTotalRobotsRequired(int i) {
        int [] rq = configs[i].getResQuantity();
        return rq[0] + 2*rq[1] + 3*rq[2];
    }

    // public int getSchemaOccurence(String [] l, String [] r, String [] u, String [] d, String resType) {
    //     int counter = 0;
    //     for (int i = 0; i < l.length; i++) {
    //         if (l[i].equals(resType)) {
    //             counter++;
    //         }
    //     }
    //     for (int i = 0; i < r.length; i++) {
    //         if (r[i].equals(resType)) {
    //             counter++;
    //         }
    //     }
    //     for (int i = 0; i < u.length; i++) {
    //         if (u[i].equals(resType)) {
    //             counter++;
    //         }
    //     }
    //     for (int i = 0; i < d.length; i++) {
    //         if (d[i].equals(resType)) {
    //             counter++;
    //         }
    //     }
    //     return counter;
    // }

    // public double getSchemaComplexity() {
        
    // }

    /**
    Class to hold a schema config
    **/
    private static class Config{
        private HashMap<String,ResourceSchema> schema;
        private int [] resQuantityArray;
        private int ACount = 0;
        private int BCount = 0;
        private int CCount = 0;
        private int totalResources = 0;
        private int numSidesInSchema;

        public Config(int k){
            schema = new HashMap<String,ResourceSchema>();
            resQuantityArray = new int[k];
        }

        public void add(String type, ResourceSchema s){
            schema.put(type, s);
            ACount += s.getACount();
            BCount += s.getBCount();
            CCount += s.getCCount();
        }

        public void setResQuantity(String [] t){
            for(int i=0;i<resQuantityArray.length;i++){
                resQuantityArray[i] = Integer.parseInt(t[i]);
                totalResources += resQuantityArray[i];
            }
        }

        public int[] getResQuantity(){
            return resQuantityArray;
        }

        public int checkSchema(String type, String [] adjacent){
            int correctSides = 0;
            if(schema.get(type).checkLeft(adjacent[0])){
                correctSides++;
            }
            if(schema.get(type).checkRight(adjacent[1])){
                correctSides++;
            }
            if(schema.get(type).checkUp(adjacent[2])){
                correctSides++;
            }
            if(schema.get(type).checkDown(adjacent[3])){
                correctSides++;
            }
            return correctSides;
        }

        public int getACount() {
            return ACount;
        }

        public int getBCount() {
            return BCount;
        }

        public int getCCount() {
            return CCount;
        }

        public int[] getIncorrectAdjacentSides (String type, String[] adjacent) {
            int[] incorrectSides = new int[4];
            if(!schema.get(type).checkLeft(adjacent[0])){
                incorrectSides[0] = 1;
            }
            if(!schema.get(type).checkRight(adjacent[1])){
                incorrectSides[1] = 1;
            }
            if(!schema.get(type).checkUp(adjacent[2])){
                incorrectSides[2] = 1;
            }
            if(!schema.get(type).checkDown(adjacent[3])){
                incorrectSides[3] = 1;
            }
            return incorrectSides;
        }

        /**
        A measure for how much cooperation is required to successfully construct the schema
        **/
        public double getCooperationMeasure () {
            //If only A resources in environment (no cooperation required)
            if (resQuantityArray[0] == totalResources) {
                return 0D;
            }
            //If only B resources or B and A resources only
            else if (resQuantityArray[2] == 0) {
                return 0.5;
            }
            //If C resources are present.. (most amount of cooperation)
            else {
                return 1D;
            }
        }

        public double getComplexityForResource (String resType) {
            double resComplexity = 0D;
            int numTypes = 0;
            for (int i = 0; i < 3; i++) {
                if (resQuantityArray[i] > 0) {
                    numTypes++;
                }
            }
            numTypes = numTypes*4;
            if (resType.equals("A")) {
                return (1 - ACount/(double)numTypes)*((double)resQuantityArray[0]/totalResources);
            }
            else if (resType.equals("B")) {
                return (1 - BCount/(double)numTypes)*((double)resQuantityArray[1]/totalResources);
            }
            else {
                return (1 - CCount/(double)numTypes)*((double)resQuantityArray[2]/totalResources);
            }
        }

        public double getSchemaConfigComplexity () {
            double complexity = 0.5*getCooperationMeasure();
            double schemaComplexity = 0D;
            schemaComplexity += getComplexityForResource("A");
            schemaComplexity += getComplexityForResource("B");
            schemaComplexity += getComplexityForResource("C");
            complexity += 0.5*schemaComplexity;
            return complexity;
        }
    }

    /**
    Class to hold a schema for a resource
    **/
    private static class ResourceSchema{
        private String [] left;
        private String [] right;
        private String [] up;
        private String [] down;
        private int ACount = 0;
        private int BCount = 0;
        private int CCount = 0;

        public ResourceSchema(String [] l, String [] r, String [] u, String [] d){
            left = copyArray(l);
            right = copyArray(r);
            up = copyArray(u);
            down = copyArray(d);
        }

        private String [] copyArray(String [] temp){
            String [] list = new String[temp.length];
            for(int i=0;i<temp.length;i++){
                list[i] = temp[i];
                if (list[i].equals("A")) {
                    ACount++;
                }
                else if (list[i].equals("B")) {
                    BCount++;
                }
                else if (list[i].equals("C")) {
                    CCount++;
                }
            }
            return list;
        }

        public ArrayList<String[]> getSides(){
            ArrayList<String[]> temp = new ArrayList<String[]>();
            temp.add(left);
            temp.add(right);
            temp.add(up);
            temp.add(down);
            return temp;
        }

        public boolean checkLeft(String s){
            for(int i=0;i<left.length;i++){
                if(left[i].equals(s)){
                    return true;
                }
            }
            return false;
        }

        public boolean checkRight(String s){
            for(int i=0;i<right.length;i++){
                if(right[i].equals(s)){
                    return true;
                }
            }
            return false;
        }

        public boolean checkUp(String s){
            for(int i=0;i<up.length;i++){
                if(up[i].equals(s)){
                    return true;
                }
            }
            return false;
        }

        public boolean checkDown(String s){
            for(int i=0;i<down.length;i++){
                if(down[i].equals(s)){
                    return true;
                }
            }
            return false;
        }

        public String [] getLeft(){
            return left;
        }

        public String [] getRight(){
            return right;
        }

        public String [] getUp(){
            return up;
        }

        public String [] getDown(){
            return down;
        }

        public int getACount() {
            return ACount;
        }

        public int getBCount() {
            return BCount;
        }

        public int getCCount() {
            return CCount;
        }
    }
}

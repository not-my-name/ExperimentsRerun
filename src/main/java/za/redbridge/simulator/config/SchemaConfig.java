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

import java.util.*;

public class SchemaConfig extends Config{
    private Config [] configs;
    private String [] resourceArray = {"A","B","C"};

    public SchemaConfig(String filepath, int n, int k){
        Map<String, Object> config = null;
        configs = new Config[n];
        Yaml yaml = new Yaml();
        try (Reader reader = Files.newBufferedReader(Paths.get(filepath))) {
            config = (Map<String, Object>) yaml.load(reader);
        } catch (IOException e) {
            e.printStackTrace();
        }

        for(int i=0;i<n;i++){
            Config newConfig = new Config(k);
            Map schemaConfig1 = (Map) config.get("config"+Integer.toString(i));
            if (checkFieldPresent(schemaConfig1, "config"+Integer.toString(i))) {
                String [] resArray = ((String) schemaConfig1.get("resQuantity")).split(" ");
                newConfig.setResQuantity(resArray);

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
        }
    }

    public int checkConfig(int i, String type ,String [] adjacent){
        return configs[i].checkSchema(type, adjacent);
    }

    public int [] getResQuantity(int i){
        return configs[i].getResQuantity();
    }

    public int[] getIncorrectAdjacentSides (int i, String type, String[] adjacent) {
        return configs[i].getIncorrectAdjacentSides(type, adjacent);
    }

    // public int getTotalResources(int i) {
    //     int [] rq = configs[i].getResQuantity();
    //     return rq[0] + rq[1] + rq[2];
    // }

    // public int getTotalRobotsRequired(int i) {
    //     int [] rq = configs[i].getResQuantity();
    //     return rq[0] + 2*rq[1] + 3*rq[2];
    // }

    private static class Config{
        private HashMap<String,ResourceSchema> schema;
        private int [] resQuantityArray;

        public Config(int k){
            schema = new HashMap<String,ResourceSchema>();
            resQuantityArray = new int[k];
        }

        public void add(String type, ResourceSchema s){
            schema.put(type, s);
        }

        public void setResQuantity(String [] t){
            for(int i=0;i<resQuantityArray.length;i++){
                resQuantityArray[i] = Integer.parseInt(t[i]);
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
    }

    private static class ResourceSchema{
        private String [] left;
        private String [] right;
        private String [] up;
        private String [] down;

        public ResourceSchema(String [] l, String [] r, String [] u, String [] d){
            left = copyArray(l);
            right = copyArray(r);
            up = copyArray(u);
            down = copyArray(d);

            // System.out.println("");
            // System.out.println("SchemaConfig: printing the resource schema");
            // System.out.println("left = " + Arrays.toString(left));
            // System.out.println("right = " + Arrays.toString(right));
            // System.out.println("up = " + Arrays.toString(up));
            // System.out.println("down = " + Arrays.toString(down));
            // System.out.println("");
        }

        private String [] copyArray(String [] temp){
            String [] list = new String[temp.length];
            for(int i=0;i<temp.length;i++){
                list[i] = temp[i];
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
    }
}

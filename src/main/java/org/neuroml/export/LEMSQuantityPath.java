package org.neuroml.export;

import java.util.ArrayList;

/**
 * @author Padraig Gleeson
 */
public class LEMSQuantityPath {

    private final String quantity;
    protected String variable;
    protected String population;
    protected String scale = "1";
    protected int num = 0;

    public final static String DUMMY_POPULATION_PREFIX = "population_";

    public LEMSQuantityPath(String q) {
        quantity = q;
        parseQuantity();
    }

    public LEMSQuantityPath(String q, String s) {
        quantity = q;
        scale = s;
        parseQuantity();
    }

    public void setPopulation(String p) {
        population = p;
    }

    public String getPopulationArray() {
        return "a_" + population;
    }

    public String getQuantity() {
        return quantity;
    }

    public String getVariable() {
        return variable;
    }

    public String getPopulation() {
        return population;
    }

    public String getScale() {
        return scale;
    }

    public int getNum() {
        return num;
    }

    public boolean isVariableInPopulation() {
        return quantity.indexOf("/") >= 0;
    }

    private void parseQuantity() {
        String[] parts = quantity.split("/");
        if (parts.length == 1) {
            variable = parts[0];
            population = DUMMY_POPULATION_PREFIX;
        } else if (quantity.indexOf('[') > 0) {
            population = quantity.split("\\[")[0];
            num = Integer.parseInt(quantity.split("\\[")[1].split("\\]")[0]);
            variable = quantity.split("/")[1];
        } else {
            population = parts[0];
            num = Integer.parseInt(parts[1]);
            variable = "";
            String[] varParts = new String[parts.length - 2];
            for (int i = 2; i < parts.length; i++) {
                if (i > 2) {
                    variable += "_";
                }
                variable += parts[i];
                varParts[i - 2] = parts[i];
            }
        }

    }

    @Override
    public String toString() {
        return "Original:       " + quantity + "\n"
                + "Variable:       " + variable + "\n"
                + "Is population:  " + this.isVariableInPopulation() + (this.isVariableInPopulation() ? " (" + this.getPopulation() + ")" : "") + "\n"
                + "Num:            " + num;
    }

    public static void main(String[] args) throws Exception {
        ArrayList<String> paths = new ArrayList<String>();
        paths.add("X1__S");
        paths.add("hhpop[6]/bioPhys1/membraneProperties/naChans/naChan/m/q");
        paths.add("fnPop1[0]/V");

        for (String path : paths) {
            LEMSQuantityPath l1 = new LEMSQuantityPath(path);
            System.out.println("\n--------\n" + l1);
        }
    }

}

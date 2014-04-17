package org.neuroml.export;

import java.util.ArrayList;

/**
 * @author Padraig Gleeson
 */
public class LEMSQuantityPath {

    private final String quantity;
    protected String population;
    protected String scale = "1";
    protected int num = 0;
    
    protected enum Type { VAR_IN_SINGLE_COMP, VAR_IN_CELL_IN_POP, VAR_IN_CELL_IN_POP_LIST}
    protected Type myType;
    
    protected String[] variableParts;
    

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

    public String getVariableParts() {
        StringBuilder var = new StringBuilder();
        for (String varPart: variableParts) {
            var.append("_").append(varPart);
        }
        return var.toString().substring(1);
    }
    
    public String getVariable() {
        
        switch (myType) {
            case VAR_IN_SINGLE_COMP: 
                return variableParts[0];
            case VAR_IN_CELL_IN_POP: 
                return getVariableParts();
            case VAR_IN_CELL_IN_POP_LIST: 
                return getVariableParts();
            
            default: return getVariableParts();
        }
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
            variableParts = new String[]{parts[0]};
            population = DUMMY_POPULATION_PREFIX;
            myType = Type.VAR_IN_SINGLE_COMP;
        } else if (quantity.indexOf('[') > 0) {
            population = quantity.split("\\[")[0];
            num = Integer.parseInt(quantity.split("\\[")[1].split("\\]")[0]);
            
            variableParts = new String[parts.length - 1];
            for (int i = 1; i < parts.length; i++) {
                
                variableParts[i - 1] = parts[i];
            }
            
            myType = Type.VAR_IN_CELL_IN_POP;
        } else {
            population = parts[0];
            num = Integer.parseInt(parts[1]);
            myType = Type.VAR_IN_CELL_IN_POP_LIST;
            variableParts = new String[parts.length - 3];
            for (int i = 3; i < parts.length; i++) {
                
                variableParts[i - 3] = parts[i];
            }
        }

    }

    @Override
    public String toString() {
        return "Original:       " + quantity + "\n"
             + "Variable parts: " + getVariableParts() + "\n"
             + "Variable:       " + getVariable() + "\n"
             + "Is population:  " + this.isVariableInPopulation() + (this.isVariableInPopulation() ? " (" + this.getPopulation() + ")" : "") + "\n"
             + "Num:            " + num;
    }

    public static void main(String[] args) throws Exception {
        ArrayList<String> paths = new ArrayList<String>();
        paths.add("X1__S");
        paths.add("hhpop[6]/bioPhys1/membraneProperties/naChans/naChan/m/q");
        paths.add("fnPop1[0]/V");
        paths.add("Gran/0/Granule_98/v");

        for (String path : paths) {
            LEMSQuantityPath l1 = new LEMSQuantityPath(path);
            System.out.println("\n--------\n" + l1);
        }
    }

}

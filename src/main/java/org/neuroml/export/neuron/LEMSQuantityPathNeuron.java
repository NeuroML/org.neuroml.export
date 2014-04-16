package org.neuroml.export.neuron;

import java.util.ArrayList;
import java.util.HashMap;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.type.Component;
import org.lemsml.jlems.core.type.Lems;
import org.neuroml.export.LEMSQuantityPath;
import static org.neuroml.export.neuron.NeuronWriter.getNrnSectionName;
import org.neuroml.model.Cell;

/**
 * @author Padraig Gleeson
 */
public class LEMSQuantityPathNeuron extends LEMSQuantityPath {

    HashMap<String, String> compMechNamesHoc = null;
    ArrayList<Component> popsOrComponents = null;
    HashMap<String, Cell> compIdsVsCells = null;
    Component targetComp = null;
    Lems lems = null;

    public LEMSQuantityPathNeuron(String q,
            String s,
            Component targetComp,
            HashMap<String, String> compMechNamesHoc,
            ArrayList<Component> popsOrComponents,
            HashMap<String, Cell> compIdsVsCells,
            Lems lems) {
        super(q, s);
        this.targetComp = targetComp;
        this.compMechNamesHoc = compMechNamesHoc;
        this.popsOrComponents = popsOrComponents;
        this.compIdsVsCells = compIdsVsCells;
        this.lems = lems;
    }
    
    public String getNeuronVariableLabel() throws ContentError {

        if (!isVariableInPopulation()) {
            return variable;
        } else {
            return getPopulationArray() + "[" + num + "]." + variable;
        }
        
    }

    public String getNeuronVariableReference() throws ContentError {

        if (!isVariableInPopulation()) {
            String hoc = population+targetComp.getName()+"[i]";
            //String mechRef = "??";
            String mechRef = compMechNamesHoc.get(hoc).replaceAll("\\[i\\]", "[" + num + "]");
            String varRef = mechRef+"." + getVariable();
            return varRef;
        } else {
            Component popComp = null;
            for (Component popsOrComponent : popsOrComponents) {
                if (popsOrComponent.getID().equals(population)) {
                    popComp = lems.getComponent(popsOrComponent.getStringValue("component"));
                }
            }
            if (compIdsVsCells.containsKey(popComp.getID())) {
                Cell cell = compIdsVsCells.get(popComp.getID());
                String varInst = getNrnSectionName(cell.getMorphology().getSegment().get(0));
                String varRef = getPopulationArray() + "[" + num + "]." + varInst + "." + getVariable();
                return varRef;
            } else {
                String hoc = population+"[i]";
                String mechRef = compMechNamesHoc.get(hoc).replaceAll("\\[i\\]", "[" + num + "]");
                String varRef = mechRef+"." + getVariable();
                return varRef;
            }
        }

    }

    @Override
    public String toString() {
        String ref;
        try {
            ref = getNeuronVariableReference();
        } catch (ContentError ex) {
            ref= "Unable to determine reference!!";
        }
        return super.toString()
                + "\nNeuron ref:     " + ref;
    }

    public static void main(String[] args) throws Exception {
        HashMap<String, String> compMechNamesHoc = new HashMap<String, String>();
        compMechNamesHoc.put("fnPop1[i]", "m_fitzHughNagumoCell[i]");
        ArrayList<String> paths = new ArrayList<String>();
        paths.add("X1__S");
        paths.add("hhpop[6]/bioPhys1/membraneProperties/naChans/naChan/m/q");
        paths.add("fnPop1[0]/V");
        /*
         for (String path : paths) {
         LEMSQuantityPathNeuron l1 = new LEMSQuantityPathNeuron(path, "1", compMechNamesHoc);
         System.out.println("\n--------\n" + l1);
         }*/
    }

}

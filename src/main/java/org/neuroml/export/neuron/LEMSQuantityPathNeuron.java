package org.neuroml.export.neuron;

import java.util.ArrayList;
import java.util.HashMap;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.type.Component;
import org.lemsml.jlems.core.type.Lems;
import org.neuroml.export.LEMSQuantityPath;
import static org.neuroml.export.neuron.NeuronWriter.getNrnSectionName;
import org.neuroml.model.Cell;
import org.neuroml.model.util.NeuroMLElements;

/**
 * @author Padraig Gleeson
 */
public class LEMSQuantityPathNeuron extends LEMSQuantityPath {

    HashMap<String, String> compMechNamesHoc = null;
    ArrayList<Component> popsOrComponents = null;
    HashMap<String, Cell> compIdsVsCells = null;
    Component targetComp = null;
    Lems lems = null;
    Component popComp = null;

    public LEMSQuantityPathNeuron(String q,
            String s,
            Component targetComp,
            HashMap<String, String> compMechNamesHoc,
            ArrayList<Component> popsOrComponents,
            HashMap<String, Cell> compIdsVsCells,
            Lems lems) throws ContentError {
        super(q, s);
        this.targetComp = targetComp;
        this.compMechNamesHoc = compMechNamesHoc;
        this.popsOrComponents = popsOrComponents;
        this.compIdsVsCells = compIdsVsCells;
        this.lems = lems;

        if (myType != Type.VAR_IN_SINGLE_COMP) {
            for (Component popsOrComponent : popsOrComponents) {
                if (popsOrComponent.getID().equals(population)) {
                    popComp = lems.getComponent(popsOrComponent.getStringValue("component"));
                }
            }
        }

    }

    public String getNeuronVariableLabel() throws ContentError {

        if (!isVariableInPopulation()) {
            return getVariable();
        } else {
            return getPopulationArray() + "[" + num + "]." + getVariable();
        }
    }

    private String convertToNeuronVariable() throws ContentError {

        HashMap<String, String> topSubstitutions = new HashMap<String, String>();
        topSubstitutions.put("caConc", "cai");

        String var = new String();

        if (variableParts.length == 1) {
            var = variableParts[0];
        } else {
            if (variableParts[1].indexOf("membraneProperties") >= 0) {

                if (variableParts.length == 4) {

                    var = variableParts[3];

                    if (var.equals("gDensity") || var.equals("iDensity")) {
                        String channelDensId = variableParts[2];
                        ArrayList<Component> channelDensityComps = popComp.getChild("biophysicalProperties").getChild("membraneProperties").getChildrenAL("channelDensities");
                        if (var.equals("gDensity")) {
                            for (Component c : channelDensityComps) {
                                if (c.getID().equals(channelDensId)) {
                                    var = "gion_" + c.getStringValue("ionChannel");
                                }
                            }
                        } else if (var.equals("iDensity")) {
                            for (Component c : channelDensityComps) {
                                if (c.getID().equals(channelDensId)) {
                                    var = "i" + c.getStringValue("ion");
                                }
                            }
                        }
                    }
                }

                if (variableParts.length > 4) {
                    for (int i = 4; i < variableParts.length; i++) {
                        var += variableParts[i] + "_";
                    }

                    var += variableParts[3];
                }
            }
        }
        if (var.length() == 0) {
            var = getVariable();
        }

        for (String key : topSubstitutions.keySet()) {
            if (var.equals(key)) {
                var = topSubstitutions.get(key);
            }
        }

        return var;

    }

    public String getNeuronVariableReference() throws ContentError {

        if (myType == Type.VAR_IN_SINGLE_COMP) {
            String hoc = getPopulation() + targetComp.getName() + "[i]";
            String mechRef = compMechNamesHoc.get(hoc).replaceAll("\\[i\\]", "[" + num + "]");
            String varRef = mechRef + "." + getVariable();
            return varRef;
        } else {

            if (popComp != null
                    && (popComp.getComponentType().isOrExtends(NeuroMLElements.CELL_COMP_TYPE)
                    || popComp.getComponentType().isOrExtends(NeuroMLElements.BASE_CELL_CAP_COMP_TYPE))) {
                if (compIdsVsCells.containsKey(popComp.getID())) {
                    Cell cell = compIdsVsCells.get(popComp.getID());
                    String varInst = getNrnSectionName(cell.getMorphology().getSegment().get(0));
                    String varRef = getPopulationArray() + "[" + num + "]." + varInst + "." + convertToNeuronVariable();
                    return varRef;
                } else {
                    String varRef = getPopulation() + "[" + num + "]." + convertToNeuronVariable();
                    return varRef;
                }

            } else {
                String hoc = population + "[i]";
                //System.out.println(this);
                //System.out.println(popComp);
                String mechRef = compMechNamesHoc.get(hoc).replaceAll("\\[i\\]", "[" + num + "]");
                String varRef = mechRef + "." + getVariable();
                return varRef;
            }
        }

    }

    @Override
    public String toString() {
        String ref;
        try {
            ref = getNeuronVariableReference();
        } catch (Exception ex) {
            ref = "Unable to determine reference!!";
        }
        return super.toString()
                + "\nNeuron ref:     " + ref
                + "\ncompIdsVsCells: " + compIdsVsCells
                + "\npopsOrComponents: " + popsOrComponents
                + "\ntargetComp: " + targetComp;
    }

    public static void main(String[] args) throws Exception {
        HashMap<String, String> compMechNamesHoc = new HashMap<String, String>();
        compMechNamesHoc.put("fnPop1[i]", "m_fitzHughNagumoCell[i]");
        ArrayList<String> paths = new ArrayList<String>();
        paths.add("X1__S");
        paths.add("hhpop[6]/bioPhys1/membraneProperties/naChans/naChan/m/q");
        paths.add("fnPop1[0]/V");

        for (String path : paths) {
            LEMSQuantityPathNeuron l1 = new LEMSQuantityPathNeuron(path, "1", null, compMechNamesHoc, null, null, null);
            System.out.println("\n--------\n" + l1);
        }
    }

}

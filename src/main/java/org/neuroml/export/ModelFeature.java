
package org.neuroml.export;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javax.xml.bind.JAXBException;
import org.lemsml.jlems.core.expression.ParseError;
import org.lemsml.jlems.core.logging.E;
import org.lemsml.jlems.core.logging.MinimalMessageHandler;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.type.Component;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.core.type.Target;
import org.neuroml.model.Cell;
import org.neuroml.model.util.NeuroMLElements;


/**
 *
 * @author padraig
 */
public enum ModelFeature {
    
    SINGLE_COMP_MODEL("Model with only a single component (no network)"),
    NETWORK_MODEL("Network model"),
    MULTI_POPULATION_MODEL("Network model with multiple populations of cells"),
    NETWORK_WITH_INPUTS_MODEL("Network model with external inputs to cells"),
    NETWORK_WITH_PROJECTIONS_MODEL("Network model with projections between populations"),
    ABSTRACT_CELL_MODEL("Model with abstract (non conductance based) cell(s)"),
    COND_BASED_CELL_MODEL("Model with conductance based cell(s)"),
    MULTICOMPARTMENTAL_CELL_MODEL("Model with multicompartmental cell(s)"),
    HH_CHANNEL_MODEL("Model with HH ion channel(s)");
        
    private final String description;
    
    private ModelFeature(String description) {
        this.description = description;
    }
    
    @Override
    public String toString() {
        return this.name()+" ("+description+")";
    }
    

    public static ArrayList<ModelFeature> analyseModelFeatures(Lems lems) throws ContentError, ParseError, IOException, JAXBException {
        ArrayList<ModelFeature> mfs = new ArrayList<ModelFeature>();
        
		Target target = lems.getTarget();

		Component simCpt = target.getComponent();

		String targetId = simCpt.getStringValue("target");

		Component targetComp = lems.getComponent(targetId);
        
        ArrayList<Component> populations = targetComp.getChildrenAL("populations");

		if (populations.isEmpty()) {
			addIfNotPresent(mfs, SINGLE_COMP_MODEL);
		} else {
			addIfNotPresent(mfs, NETWORK_MODEL);
            if (populations.size()>1) 
            {
                addIfNotPresent(mfs, MULTI_POPULATION_MODEL);
            }
            for (Component pop : populations) {
        
                if (pop.getComponentType().getName().equals(NeuroMLElements.POPULATION) ||
                    pop.getComponentType().getName().equals(NeuroMLElements.POPULATION_LIST)) {

                    String compReference = pop.getStringValue(NeuroMLElements.POPULATION_COMPONENT);
                    Component popComp = lems.getComponent(compReference);
                    
                    if (popComp.getComponentType().isOrExtends(NeuroMLElements.CELL_COMP_TYPE) ||
                        popComp.getComponentType().isOrExtends(NeuroMLElements.BASE_CELL_CAP_POINT_COND_BASED) ||
                        popComp.getComponentType().isOrExtends(NeuroMLElements.BASE_CELL_CAP_POINT_COND_BASED_CA)) {
                        
                        addIfNotPresent(mfs, COND_BASED_CELL_MODEL);
                        if (popComp.getComponentType().isOrExtends(NeuroMLElements.CELL_COMP_TYPE)) {
                            Cell cell = Utils.getCellFromComponent(popComp);
                            if (cell.getMorphology().getSegment().size()>1) {
                                addIfNotPresent(mfs, MULTICOMPARTMENTAL_CELL_MODEL);
                            } 
                        }
                    } else {
                         addIfNotPresent(mfs, ABSTRACT_CELL_MODEL);
                    }
                }
            }
        }
        
        if (targetComp.getChildrenAL("inputs").size() > 0 ||
            targetComp.getChildrenAL("explicitInputs").size() > 0)
        {
            addIfNotPresent(mfs, NETWORK_WITH_INPUTS_MODEL);
        };
        
        if (targetComp.getChildrenAL("projections").size() > 0 ||
            targetComp.getChildrenAL("synapticConnections").size() > 0)
        {
            addIfNotPresent(mfs, NETWORK_WITH_PROJECTIONS_MODEL);
        };
        
        
        
        return mfs;
    }
    
    private static void addIfNotPresent(ArrayList<ModelFeature> mfs, ModelFeature mf) {
        if (!mfs.contains(mf)) mfs.add(mf);
    }
    
    public static void main(String[] args)  {
        
        MinimalMessageHandler.setVeryMinimal(true);
        E.setDebug(false);
        
        File lemsFileDir = new File("../NeuroML2/LEMSexamples/");
        for (File f: lemsFileDir.listFiles()) {
            try {
                if (f.getName().startsWith("LEMS") && f.getName().endsWith(".xml")) {
                    System.out.println("----------------------------------");
                    Lems lems = Utils.readLemsNeuroMLFile(f).getLems();
                    ArrayList<ModelFeature> mfs = analyseModelFeatures(lems);
                    for (ModelFeature mf: mfs) {
                        System.out.println(mf);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

            
    
}

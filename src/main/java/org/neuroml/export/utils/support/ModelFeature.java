package org.neuroml.export.utils.support;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import org.lemsml.jlems.core.logging.E;
import org.lemsml.jlems.core.logging.MinimalMessageHandler;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.sim.LEMSException;
import org.lemsml.jlems.core.type.Component;
import org.lemsml.jlems.core.type.ComponentType;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.core.type.Target;
import org.neuroml.export.utils.Utils;
import org.neuroml.model.Cell;
import org.neuroml.model.ChannelDensity;
import org.neuroml.model.ChannelDensityGHK;
import org.neuroml.model.ChannelDensityNernst;
import org.neuroml.model.ChannelPopulation;
import org.neuroml.model.util.NeuroMLElements;
import org.neuroml.model.util.NeuroMLException;

/**
 * 
 * @author padraig
 */
public enum ModelFeature
{

    SINGLE_COMP_MODEL("Model with only a single component (no network)"), 
    NETWORK_MODEL("Network model"), 
    MULTI_CELL_MODEL("Network model with multiple cells in a population"), 
    MULTI_POPULATION_MODEL("Network model with multiple populations of cells"), 
    NETWORK_WITH_INPUTS_MODEL("Network model with external inputs to cells"), 
    NETWORK_WITH_PROJECTIONS_MODEL("Network model with projections between populations"), 
    NETWORK_WITH_GAP_JUNCTIONS_MODEL("Network model with gap junctions/electrical connections between cells"),
    NETWORK_WITH_ANALOG_CONNS_MODEL("Network model with analog/continuously communicating connections between cells"),
    ABSTRACT_CELL_MODEL("Model with abstract (non conductance based) cell(s)"), 
    COND_BASED_CELL_MODEL("Model with conductance based cell(s)"), 
    MULTICOMPARTMENTAL_CELL_MODEL("Model with multicompartmental cell(s)"), 
    CHANNEL_POPULATIONS_CELL_MODEL("Model with channel populations"), 
    CHANNEL_DENSITY_ON_SEGMENT("Model with channel density specified per segment (aot segmentGroup)"), 
    HH_CHANNEL_MODEL("Model with HH based ion channel(s)"), 
    KS_CHANNEL_MODEL("Model with kinetic scheme based ion channel(s)"), 
    ALL("ALL");

    private final String description;

    private ModelFeature(String description)
    {
        this.description = description;
    }

    @Override
    public String toString()
    {
        return this.name() + " (" + description + ")";
    }

    public static ArrayList<ModelFeature> analyseModelFeatures(Lems lems) throws LEMSException, NeuroMLException
    {

        ArrayList<ModelFeature> mfs = new ArrayList<ModelFeature>();

        Target target = null;
        try
        {
            target = lems.getTarget();
        }
        catch(ContentError ce)
        {
            for(Component comp : lems.getComponents())
            {
                analyseSingleComponent(comp, mfs, lems);
            }
            return mfs;
        }
        Component simCpt = target.getComponent();

        String targetId = simCpt.getStringValue("target");

        Component targetComp = lems.getComponent(targetId);

        ArrayList<Component> populations = targetComp.getChildrenAL("populations");

        if(populations.isEmpty())
        {
            addIfNotPresent(mfs, SINGLE_COMP_MODEL);
        }
        analyseSingleComponent(targetComp, mfs, lems);
        


        return mfs;
    }

    private static void analyseSingleComponent(Component component, ArrayList<ModelFeature> mfs, Lems lems) throws LEMSException, NeuroMLException
    {

        if(component.getComponentType().isOrExtends(NeuroMLElements.CELL_COMP_TYPE) || 
           component.getComponentType().isOrExtends(NeuroMLElements.BASE_CELL_CAP_POINT_COND_BASED) || 
           component.getComponentType().isOrExtends(NeuroMLElements.BASE_CELL_CAP_POINT_COND_BASED_CA))
        {

            addIfNotPresent(mfs, COND_BASED_CELL_MODEL);

            if(component.getComponentType().isOrExtends(NeuroMLElements.CELL_COMP_TYPE))
            {
                Cell cell = Utils.getCellFromComponent(component);
                if(cell.getMorphology() != null)
                {
                    if(cell.getMorphology().getSegment().size() > 1)
                    {
                        addIfNotPresent(mfs, MULTICOMPARTMENTAL_CELL_MODEL);
                    }

                    if(cell.getBiophysicalProperties() != null)
                    {
                        for(Component channelDensity : component.getChild("biophysicalProperties").getChild("membraneProperties").getChildrenAL("channelDensities"))
                        {
                            checkForChannels(channelDensity.getRefHM().get("ionChannel"), mfs, lems);
                            if (channelDensity.hasAttribute("segment"))
                                addIfNotPresent(mfs, CHANNEL_DENSITY_ON_SEGMENT);
                        }
                        for(Component channelDensity : component.getChild("biophysicalProperties").getChild("membraneProperties").getChildrenAL("channelDensitiesNernst"))
                        //for(ChannelDensityNernst cd : cell.getBiophysicalProperties().getMembraneProperties().getChannelDensityNernst())
                        {
                            checkForChannels(channelDensity.getRefHM().get("ionChannel"), mfs, lems);
                            if (channelDensity.hasAttribute("segment"))
                                addIfNotPresent(mfs, CHANNEL_DENSITY_ON_SEGMENT);
                        }
                        for(Component channelDensity : component.getChild("biophysicalProperties").getChild("membraneProperties").getChildrenAL("channelDensitiesGHK"))
                        //for(ChannelDensityGHK cd : cell.getBiophysicalProperties().getMembraneProperties().getChannelDensityGHK())
                        {
                        	checkForChannels(channelDensity.getRefHM().get("ionChannel"), mfs, lems);
                        	if (channelDensity.hasAttribute("segment"))
                                addIfNotPresent(mfs, CHANNEL_DENSITY_ON_SEGMENT);
                        }
                        for(Component population : component.getChild("biophysicalProperties").getChild("membraneProperties").getChildrenAL("populations"))
                        //for(ChannelPopulation cd : cell.getBiophysicalProperties().getMembraneProperties().getChannelPopulation())
                        {
                        	addIfNotPresent(mfs, CHANNEL_POPULATIONS_CELL_MODEL);
                            System.out.println("cp");
                            checkForChannels(population.getRefHM().get("ionChannel"), mfs, lems);
                        }
                    }
                }
            }
            else if(component.getComponentType().isOrExtends(NeuroMLElements.BASE_CELL_CAP_POINT_COND_BASED)
                    || component.getComponentType().isOrExtends(NeuroMLElements.BASE_CELL_CAP_POINT_COND_BASED_CA))
            {
                ArrayList<Component> channelPopulations = component.getChildrenAL("populations");
                for(Component chanPop : channelPopulations)
                {
                    checkForChannels(chanPop.getStringValue("ionChannel"), mfs, lems);
                }
            }

        }
        else if(component.getComponentType().isOrExtends(NeuroMLElements.BASE_CELL_COMP_TYPE))
        {
            addIfNotPresent(mfs, ABSTRACT_CELL_MODEL);
        }
        else if(component.getComponentType().isOrExtends(NeuroMLElements.ION_CHANNEL_HH_COMP_TYPE))
        {
            addIfNotPresent(mfs, HH_CHANNEL_MODEL);
        }
        else if(component.getComponentType().isOrExtends(NeuroMLElements.ION_CHANNEL_KS_COMP_TYPE))
        {
            addIfNotPresent(mfs, KS_CHANNEL_MODEL);
        }
        else if(component.getComponentType().isOrExtends(NeuroMLElements.NETWORK))
        {
            addIfNotPresent(mfs, NETWORK_MODEL);
            
            ArrayList<Component> populations = component.getChildrenAL("populations");
        
            if(populations.size() > 1)
            {
                addIfNotPresent(mfs, MULTI_POPULATION_MODEL);
                addIfNotPresent(mfs, MULTI_CELL_MODEL);
            }
            for(Component pop : populations)
            {
                if(pop.getComponentType().isOrExtends(NeuroMLElements.POPULATION_LIST))
                {
                    int size = pop.getChildrenAL("instances").size();
                    if(size > 1)
                    {
                        addIfNotPresent(mfs, MULTI_CELL_MODEL);
                    }
                }
                else if(pop.getComponentType().getName().equals(NeuroMLElements.POPULATION))
                {
                    int size = Integer.parseInt(pop.getStringValue(NeuroMLElements.POPULATION_SIZE));
                    if(size > 1)
                    {
                        addIfNotPresent(mfs, MULTI_CELL_MODEL);
                    }
                }

                if(pop.getComponentType().getName().equals(NeuroMLElements.POPULATION) || pop.getComponentType().getName().equals(NeuroMLElements.POPULATION_LIST))
                {
                	Component popComp = pop.getRefComponents().get("component");
                	
                    //String compReference = pop.getStringValue(NeuroMLElements.POPULATION_COMPONENT);
                    //Component popComp = lems.getComponent(compReference);
                    analyseSingleComponent(popComp, mfs, lems);
                }
            }
            
            if(component.getChildrenAL("inputs").size() > 0 || component.getChildrenAL("explicitInputs").size() > 0)
            {
                addIfNotPresent(mfs, NETWORK_WITH_INPUTS_MODEL);
            }

            if(component.getChildrenAL("projections").size() > 0 || component.getChildrenAL("synapticConnections").size() > 0)
            {
                addIfNotPresent(mfs, NETWORK_WITH_PROJECTIONS_MODEL);
            }

            if(component.getChildrenAL("electricalProjection").size() > 0)
            {
                addIfNotPresent(mfs, NETWORK_WITH_GAP_JUNCTIONS_MODEL);
            }

            if(component.getChildrenAL("continuousProjection").size() > 0)
            {
                addIfNotPresent(mfs, NETWORK_WITH_ANALOG_CONNS_MODEL);
            }
        }
    }

    private static void checkForChannels(Component ionChannel, ArrayList<ModelFeature> mfs, Lems lems) throws ContentError
    {
        if(ionChannel.getComponentType().isOrExtends(NeuroMLElements.ION_CHANNEL_HH_COMP_TYPE))
        {
            addIfNotPresent(mfs, HH_CHANNEL_MODEL);
        }
        if(ionChannel.getComponentType().isOrExtends(NeuroMLElements.ION_CHANNEL_KS_COMP_TYPE))
        {
            addIfNotPresent(mfs, KS_CHANNEL_MODEL);
        }
    }

    private static void checkForChannels(String ionChannel, ArrayList<ModelFeature> mfs, Lems lems) throws ContentError
    {
        if(lems.getComponent(ionChannel).getComponentType().isOrExtends(NeuroMLElements.ION_CHANNEL_HH_COMP_TYPE))
        {
            addIfNotPresent(mfs, HH_CHANNEL_MODEL);
        }
        if(lems.getComponent(ionChannel).getComponentType().isOrExtends(NeuroMLElements.ION_CHANNEL_KS_COMP_TYPE))
        {
            addIfNotPresent(mfs, KS_CHANNEL_MODEL);
        }
    }

    private static void addIfNotPresent(ArrayList<ModelFeature> mfs, ModelFeature mf)
    {
        if(!mfs.contains(mf))
        {
            mfs.add(mf);
        }
    }

    public static void main(String[] args)
    {

        MinimalMessageHandler.setVeryMinimal(true);
        E.setDebug(false);

        File lemsFileDir = new File("../NeuroML2/LEMSexamples/");
        for(File f : lemsFileDir.listFiles())
        {
            try
            {
                if(f.getName().startsWith("LEMS") && f.getName().endsWith(".xml"))
                {
                    System.out.println("----------------------------------");
                    Lems lems = Utils.readLemsNeuroMLFile(f).getLems();
                    ArrayList<ModelFeature> mfs = analyseModelFeatures(lems);
                    for(ModelFeature mf : mfs)
                    {
                        System.out.println(mf);
                    }
                }
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
        File exFileDir = new File("../NeuroML2/examples");
        for(File f : exFileDir.listFiles())
        {
            ArrayList<String> ignores = new ArrayList<String>();
            //Collections.addAll(ignores, "NML2_NestedNetworks.nml", "NML2_CellVariableParams.nml", "NML2_SimpleNetwork3D.nml", "NML2_SimpleNetwork.nml", "NML2_InhomogeneousParams.nml",
             //       "NML2_FullCell.nml");
            try
            {
                if(f.getName().startsWith("NML2") && f.getName().endsWith(".nml") && !ignores.contains(f.getName()))
                {
                    System.out.println("\n----------------------------------");
                    Lems lems = Utils.readNeuroMLFile(f).getLems();
                    ArrayList<ModelFeature> mfs = analyseModelFeatures(lems);
                    for(ModelFeature mf : mfs)
                    {
                        System.out.println(mf);
                    }
                }
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
    }

}

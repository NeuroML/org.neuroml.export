package org.neuroml.export.neuron;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.type.Component;
import org.lemsml.jlems.core.type.Dimension;
import org.lemsml.jlems.core.type.Exposure;
import org.lemsml.jlems.core.type.Lems;
import org.neuroml.export.utils.LEMSQuantityPath;
import org.neuroml.export.utils.Utils;
import org.neuroml.model.Cell;
import org.neuroml.model.NeuroMLDocument;
import org.neuroml.model.Segment;
import org.neuroml.model.util.CellUtils;
import org.neuroml.model.util.NeuroMLConverter;
import org.neuroml.model.util.NeuroMLElements;
import org.neuroml.model.util.NeuroMLException;

/**
 * @author Padraig Gleeson
 */
public class LEMSQuantityPathNeuron extends LEMSQuantityPath
{

    HashMap<String, String> compMechNamesHoc = null;
    ArrayList<Component> popsOrComponents = null;
    HashMap<String, Cell> compIdsVsCells = null;
    HashMap<String, String> hocRefsVsInputs = null;
    Component targetComp = null;
    Lems lems = null;
    Component popComp = null;

    public LEMSQuantityPathNeuron(String q, 
                                  String s, 
                                  Component targetComp, 
                                  HashMap<String, String> compMechNamesHoc, 
                                  ArrayList<Component> popsOrComponents, 
                                  HashMap<String, Cell> compIdsVsCells,
                                  HashMap<String, String> hocRefsVsInputs,
                                  Lems lems) throws ContentError
    {
        super(q, s);
        this.targetComp = targetComp;
        this.compMechNamesHoc = compMechNamesHoc;
        this.popsOrComponents = popsOrComponents;
        this.compIdsVsCells = compIdsVsCells;
        this.hocRefsVsInputs = hocRefsVsInputs;
        this.lems = lems;

        if (myType != Type.VAR_IN_SINGLE_COMP)
        {
            for (Component popsOrComponent : popsOrComponents)
            {
                if (popsOrComponent.getID().equals(population))
                {
                    popComp = popsOrComponent.getRefComponents().get("component");
                }
            }
        }
    }
    public LEMSQuantityPathNeuron(String q, 
                                  String s, 
                                  Lems lems) throws ContentError
    {
        super(q, s);
        this.targetComp = null;
        this.compMechNamesHoc = new HashMap<String, String>();
        this.popsOrComponents = new ArrayList<Component>();
        this.compIdsVsCells = new HashMap<String, Cell>();
        this.hocRefsVsInputs = new HashMap<String, String>();
        this.lems = lems;

        if (myType != Type.VAR_IN_SINGLE_COMP)
        {
            for (Component popsOrComponent : popsOrComponents)
            {
                if (popsOrComponent.getID().equals(population))
                {
                    popComp = popsOrComponent.getRefComponents().get("component");
                }
            }
        }
    }

    private static Exposure getExposure(Component c, String path) throws ContentError
    {
        try
        {
            Exposure e = c.getComponentType().getExposure(path);
            return e;
        }
        catch (ContentError e)
        {
            String child = path.substring(0, path.indexOf("/"));
            String pathInChild = path.substring(path.indexOf("/") + 1);
            Component ch = null;
            for (Component chi : c.getAllChildren())
            {
                if (chi.getID() != null && chi.getID().equals(child))
                {
                    ch = chi;
                }
            }
            if (ch == null)
            {
                for (Component chi : c.getAllChildren())
                {
                    if (chi.getTypeName().equals(child))
                    {
                        ch = chi;
                    }
                }
            }

            return getExposure(ch, pathInChild);
        }

    }
    
    public Dimension getDimensionOfVariableOnCellInPopComp(String[] variableParts, Component popComp) throws ContentError
    {
        String path = getVariablePathInPopComp(variableParts, Type.VAR_IN_CELL_IN_POP_LIST);
        // TODO: this is quick & dirty to handle inputs!! Replace!
        if (path.endsWith("/i"))
        {
            return lems.getDimensions().getByName("current");
        }
        return getExposure(popComp, path).getDimension();
        
    }

    public Dimension getDimension() throws ContentError, NeuroMLException
    {
        if (isVariableOnSynapse())
        {
            String synId = getSynapseType();
            Component synComp = lems.getComponent(synId);
            return getExposure(synComp, getPathforVariableOnSyn()).getDimension();
        }
        // TODO: this is quick & dirty to handle inputs!! Replace!
        else if (getNeuronVariableReference().endsWith(".i"))
        {
            return lems.getDimensions().getByName("current");
        }
        else
        {
            String path = getVariablePathInPopComp();
            Component comp = (myType != Type.VAR_IN_SINGLE_COMP) ? popComp : targetComp;
            return getExposure(comp, path).getDimension();
        }
    }

    public String getNeuronVariableLabel() throws ContentError
    {
        if (!isVariableInPopulation())
        {
            return getVariable();
        }
        else
        {
            return getPopulationArray() + "[" + populationIndex + "]." + getVariable();
        }
    }

    
    private String convertToNeuronVariable() throws ContentError 
    {
        return convertToNeuronVariable(variableParts, popComp);
    }
    
    public static String convertToNeuronVariable(String[] variableParts, Component popComp) throws ContentError
    {
        HashMap<String, String> topSubstitutions = new HashMap<String, String>();
        topSubstitutions.put("caConc", "cai");

        String var = new String();

        if (variableParts.length == 1)
        {
            var = variableParts[0];
        }
        else
        {
            if (variableParts[1].contains("membraneProperties"))
            {

                if (variableParts.length == 4)
                {

                    var = variableParts[3];

                    if (var.equals("gDensity") || var.equals("iDensity"))
                    {
                        String channelDensId = variableParts[2];
                        ArrayList<Component> channelDensityComps = popComp.getChild("biophysicalProperties").getChild("membraneProperties").getChildrenAL("channelDensities");
                        if (var.equals("gDensity"))
                        {
                            for (Component c : channelDensityComps)
                            {
                                if (c.getID().equals(channelDensId))
                                {
                                    var = "gion_" + c.getStringValue("ionChannel");
                                }
                            }
                        }
                        else if (var.equals("iDensity"))
                        {
                            for (Component c : channelDensityComps)
                            {
                                if (c.getID().equals(channelDensId))
                                {
                                    var = "i" + c.getStringValue("ion");
                                }
                            }
                        }
                    }
                }

                if (variableParts.length > 4)
                {
                    for (int i = 4; i < variableParts.length; i++)
                    {
                        var += variableParts[i] + "_";
                    }

                    var += variableParts[3];
                }
            }
        }
        if (var.length() == 0)
        {
            var = getVariable(variableParts, Type.VAR_IN_CELL_IN_POP_LIST);
        }

        for (String key : topSubstitutions.keySet())
        {
            if (var.equals(key))
            {
                var = topSubstitutions.get(key);
            }
        }

        return var;

    }
    
    
    public String getSynapseType()
    {
        if (!isVariableOnSynapse())
        {
            return null;
        }

        String var = getVariable();
        
        String[] synInfo = var.split(":");
        return synInfo[1];
    }

    
    public int getSynapseIndex()
    {
        if (!isVariableOnSynapse())
        {
            return -1;
        }

        String var = getVariable();
        String[] synInfo = var.split(":");
        int index = Integer.parseInt(synInfo[2].split("_")[0]);
        return index;
    }

    
    public String getVariableOnSyn()
    {
        if (!isVariableOnSynapse())
        {
            return null;
        }

        String var = getVariable();
        String varInfo = var.split(":")[2];
        return varInfo.substring(varInfo.indexOf("_") + 1);

    }

    
    public String getPathforVariableOnSyn()
    {
        if (!isVariableOnSynapse())
        {
            return null;
        }

        String var = getVariablePartsAsString("/",variableParts);
        return var.substring(var.indexOf("/") + 1);

    }
    
    public boolean valid()
    {
        try
        {
            getNeuronVariableReference();
        }
        catch (ContentError ex)
        {
            return false;
        }
        catch (NeuroMLException ex)
        {
            return false;
        }
        return true;
    }

        
    
    public String getNeuronVariableReference() throws ContentError, NeuroMLException
    {
        try 
        {
            if (myType == Type.VAR_IN_SINGLE_COMP)
            {
                String hoc = getPopulation() + targetComp.getName() + "[i]";
                String mechRef = compMechNamesHoc.get(hoc).replaceAll("\\[i\\]", "[" + populationIndex + "]");
                String varRef = mechRef + "." + getVariable();
                return varRef;
            }
            else if (isVariableOnSynapse())
            {
                String varRef = "syn" + "_" + getPopulation() + "_" + getPopulationIndex() + "_" + getSegmentId() + "_" + getSynapseType() + "_" + getSynapseIndex() + "." + getVariableOnSyn();
                return varRef;
            }
            else
            {
                if (popComp != null && 
                    (popComp.getComponentType().isOrExtends(NeuroMLElements.CELL_COMP_TYPE) || 
                     ((popComp.getComponentType().isOrExtends(NeuroMLElements.BASE_CELL_CAP_COMP_TYPE) || 
                       popComp.getComponentType().isOrExtends(NeuroMLElements.BASE_IAF_CELL)|| 
                       popComp.getComponentType().isOrExtends(NeuroMLElements.BASE_PYNN_CELL)) && 
                      convertToNeuronVariable().equals(NRNUtils.NEURON_VOLTAGE))))
                {
                    if (compIdsVsCells.containsKey(popComp.getID()))
                    {
                        Cell cell = compIdsVsCells.get(popComp.getID());
                        NamingHelper nh = new NamingHelper(cell);
                        Segment segment = CellUtils.getSegmentWithId(cell, segmentId);
                        String varInst = nh.getNrnSectionName(segment);
                        
                        float fract;
                        if (cell.getMorphology().getSegment().size() == 1)
                        {
                            fract = 0.5f;
                        }
                        else if (!CellUtils.hasSegmentGroup(cell, varInst) && segment.getName().equals(varInst))
                        {
                            // No real segment group, segment ids being used for sections...
                            fract = 0.5f;
                        }
                        else
                        {
                            fract = (float) CellUtils.getFractionAlongSegGroupLength(cell, varInst, segmentId, 0.5f);
                        }

                        String varRef;
                        String possibleInputRef = getPopulationArray() + "[" + populationIndex + "]." + varInst +":"+ variableParts[0];
                        
                        if (hocRefsVsInputs.containsKey(possibleInputRef))
                        {
                            varRef = hocRefsVsInputs.get(possibleInputRef)+ "." + variableParts[1];
                        }
                        else
                        {
                            varRef = getPopulationArray() + "[" + populationIndex + "]." + varInst + "." + convertToNeuronVariable() + "(" + fract + ")";
                        }
                        return varRef;
                    }
                    else
                    {
                        String nrnVar = convertToNeuronVariable();
                        //String possibleInputRef = getPopulationArray() + "[" + populationIndex + "]:"+ variableParts[0];
                        
                        String varRef = getPopulation() + "[" + populationIndex + "]." + nrnVar;

                        if (nrnVar.equals(NRNUtils.NEURON_VOLTAGE))
                        { // redundant..?
                            varRef += "(0.5)";
                        }
                        return varRef;
                    }
                }
                else
                {
                    String hoc = population + "[i]";
                    String mechRef = compMechNamesHoc.get(hoc).replaceAll("\\[i\\]", "[" + populationIndex + "]");
                    String varRef = mechRef + "." + getVariable();
                    return varRef;
                }
            }
        }
        catch (Exception e)
        {
            throw new NeuroMLException("Error converting the path: "+this.getQuantity()+" into the corresponding reference to the NEURON variable.\n"+
                "Ensure the population id, component id, cell index and variable used in this path are correct!\n"+"Exception: "+e+"\n"+super.toString());
        }
    }

    @Override
    public String toString()
    {
        String ref;
        try
        {
            ref = getNeuronVariableReference();
        }
        catch (ContentError ex)
        {
            ref = "=== Unable to determine reference: " + ex;
        }
        catch (NeuroMLException ex)
        {
            ref = "=== Unable to determine reference: " + ex;
        }

        ref += super.toString() 
            + "\n    ** Neuron ref:    " + ref
            + "\n    popsOrComponents: " + popsOrComponents 
            + "\n    targetComp:       " + targetComp 
            + "\n    popComp:          " + popComp
            + "\n    hocRefsVsInputs:  " + hocRefsVsInputs;
        
        if (this.isVariableOnSynapse())
        {
            ref 
            +="\n    Synapse type:     " + this.getSynapseType()
            + "\n    Synapse index:    " + this.getSynapseIndex()
            + "\n    Synapse var:      " + this.getVariableOnSyn();
        }
        return ref;
    }

    public static void main(String[] args) throws Exception
    {
        HashMap<String, String> compMechNamesHoc = new HashMap<String, String>();
        
        compMechNamesHoc.put("fnPop1[i]", "m_fitzHughNagumoCell[i]");
        ArrayList<Component> popsOrComponents = new ArrayList<Component>();
        ArrayList<String> paths = new ArrayList<String>();
        paths.add("fnPop1[0]/V");

        for (String path : paths)
        {
            LEMSQuantityPathNeuron l1 = new LEMSQuantityPathNeuron(path, "1", null, compMechNamesHoc, popsOrComponents, null, null, null);
            System.out.println("\n--------\n" + l1);
            if (!l1.valid()) throw new Exception("Should be valid");
        }
        
        compMechNamesHoc = new HashMap<String, String>();
        
        paths = new ArrayList<String>();
        paths.add("hhpop/0/hhneuron/v");
        paths.add("hhpop/0/hhneuron/biophysics/membraneProperties/kChans/gDensity");
        paths.add("hhpop/0/hhneuron/biophysics/membraneProperties/naChans/naChan/m/q");
        paths.add("hhpop/0/hhneuron/biophysics/membraneProperties/kChans/kChan/n/q");
        paths.add("hhpop/0/hhneuron/biophysics/membraneProperties/naChans/iDensity");
        paths.add("hhpop/0/hhneuron/biophysics/membraneProperties/kChans/gDensity");
        paths.add("hhpop/0/hhneuron/IClamp/i");
        paths.add("hhpop/0/hhneuron/0/v");
        paths.add("hhpop/0/hhneuron/0/synapses:AMPA:0/g");
        paths.add("hhpop/0/hhneuron/0/synapses:bc_syn:0/g");
        
        Lems lems = Utils.readLemsNeuroMLFile(new File("../neuroConstruct/osb/generic/hodgkin_huxley_tutorial/Tutorial2/NeuroML2/LEMS_HHTutorial.xml")).getLems();
        NeuroMLConverter nmlc = new NeuroMLConverter();
        NeuroMLDocument nmldoc = nmlc.loadNeuroML(new File("../neuroConstruct/osb/generic/hodgkin_huxley_tutorial/Tutorial2/NeuroML2/HHTutorial.net.nml"), true);
        
        Component hhnet = lems.getComponent("HHTutorial");
        popsOrComponents = hhnet.getChildrenAL("populations");
        Cell c = nmldoc.getCell().get(0);
        HashMap<String, Cell> compIdsVsCells = new HashMap<String, Cell>();
        HashMap<String, String> hocRefsVsInputs = new HashMap<String, String>();
        hocRefsVsInputs.put("a_hhpop[0].soma:IClamp","Input_0_0");
        
        for (String path : paths)
        {
            LEMSQuantityPathNeuron l1 = new LEMSQuantityPathNeuron(path, "1", hhnet, compMechNamesHoc, popsOrComponents, compIdsVsCells, hocRefsVsInputs, lems);
            System.out.println("\n===================\n" + l1);
            if (!l1.valid()) throw new Exception("Should be valid");
            
        }
    }

}

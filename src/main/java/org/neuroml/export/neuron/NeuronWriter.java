package org.neuroml.export.neuron;

import static org.neuroml.export.neuron.ProcessManager.findNeuronHome;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.lemsml.export.dlems.DLemsWriter;
import org.lemsml.jlems.core.expression.ParseError;
import org.lemsml.jlems.core.logging.E;
import org.lemsml.jlems.core.logging.MinimalMessageHandler;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.sim.LEMSException;
import org.lemsml.jlems.core.type.Component;
import org.lemsml.jlems.core.type.Exposure;
import org.lemsml.jlems.core.type.InstanceRequirement;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.core.type.LemsCollection;
import org.lemsml.jlems.core.type.ParamValue;
import org.lemsml.jlems.core.type.Requirement;
import org.lemsml.jlems.core.type.Target;
import org.lemsml.jlems.core.type.dynamics.Case;
import org.lemsml.jlems.core.type.dynamics.ConditionalDerivedVariable;
import org.lemsml.jlems.core.type.dynamics.DerivedVariable;
import org.lemsml.jlems.core.type.dynamics.Dynamics;
import org.lemsml.jlems.core.type.dynamics.OnCondition;
import org.lemsml.jlems.core.type.dynamics.OnEntry;
import org.lemsml.jlems.core.type.dynamics.OnEvent;
import org.lemsml.jlems.core.type.dynamics.OnStart;
import org.lemsml.jlems.core.type.dynamics.Regime;
import org.lemsml.jlems.core.type.dynamics.StateAssignment;
import org.lemsml.jlems.core.type.dynamics.StateVariable;
import org.lemsml.jlems.core.type.dynamics.TimeDerivative;
import org.lemsml.jlems.core.type.dynamics.Transition;
import org.lemsml.jlems.core.type.simulation.EventWriter;
import org.lemsml.jlems.io.util.FileUtil;
import org.neuroml.export.base.ANeuroMLBaseWriter;
import org.neuroml.export.exceptions.GenerationException;
import org.neuroml.export.exceptions.ModelFeatureSupportException;
import org.neuroml.export.utils.Format;
import org.neuroml.export.utils.LEMSQuantityPath;
import org.neuroml.export.utils.ProcessOutputWatcher;
import org.neuroml.export.utils.Utils;
import org.neuroml.export.utils.VelocityUtils;
import org.neuroml.export.utils.support.ModelFeature;
import org.neuroml.export.utils.support.SupportLevelInfo;
import org.neuroml.model.BiophysicalProperties;
import org.neuroml.model.BiophysicalProperties2CaPools;
import org.neuroml.model.Cell;
import org.neuroml.model.Cell2CaPools;
import org.neuroml.model.IntracellularProperties;
import org.neuroml.model.MembraneProperties;
import org.neuroml.model.MembraneProperties2CaPools;
import org.neuroml.model.Segment;
import org.neuroml.model.Species;
import org.neuroml.model.SpikeThresh;
import org.neuroml.model.util.CellUtils;
import org.neuroml.model.util.NeuroMLElements;
import org.neuroml.model.util.NeuroMLException;

@SuppressWarnings("StringConcatenationInsideStringBufferAppend")
public class NeuronWriter extends ANeuroMLBaseWriter
{
    private final ArrayList<String> generatedModComponents = new ArrayList<String>();

    private final List<File> outputFiles = new ArrayList<File>();

    boolean nogui = false;

    static boolean debug = false;

    public static final String NEURON_HOME_ENV_VAR = "NEURON_HOME";

    public HashMap<String, String> modWritten = new HashMap<String, String>();

    private final HashMap<String, Cell> compIdsVsCells = new HashMap<String, Cell>();

    private final HashMap<String, String> popIdsVsCellIds = new HashMap<String, String>();

    private final HashMap<String, Component> popIdsVsComps = new HashMap<String, Component>();

    private final HashMap<String, IntracellularProperties> convertedCells = new HashMap<String, IntracellularProperties>();

    public enum ChannelConductanceOption
    {
        FIXED_REVERSAL_POTENTIAL, USE_NERNST, USE_GHK;
        float erev;
    };

    public NeuronWriter(Lems lems) throws ModelFeatureSupportException, LEMSException, NeuroMLException
    {
        super(lems, Format.NEURON);
        E.info("Creating NeuronWriter");
    }

    public NeuronWriter(Lems lems, File outputFolder, String outputFileName) throws ModelFeatureSupportException, NeuroMLException, LEMSException
    {
        super(lems, Format.NEURON, outputFolder, outputFileName);
        E.info("Creating NeuronWriter for "+outputFileName);
    }

    @Override
    public void setSupportedFeatures()
    {
        sli.addSupportInfo(format, ModelFeature.ABSTRACT_CELL_MODEL, SupportLevelInfo.Level.MEDIUM);
        sli.addSupportInfo(format, ModelFeature.COND_BASED_CELL_MODEL, SupportLevelInfo.Level.MEDIUM);
        sli.addSupportInfo(format, ModelFeature.SINGLE_COMP_MODEL, SupportLevelInfo.Level.MEDIUM);
        sli.addSupportInfo(format, ModelFeature.NETWORK_MODEL, SupportLevelInfo.Level.LOW);
        sli.addSupportInfo(format, ModelFeature.MULTI_CELL_MODEL, SupportLevelInfo.Level.MEDIUM);
        sli.addSupportInfo(format, ModelFeature.MULTI_POPULATION_MODEL, SupportLevelInfo.Level.MEDIUM);
        sli.addSupportInfo(format, ModelFeature.NETWORK_WITH_INPUTS_MODEL, SupportLevelInfo.Level.MEDIUM);
        sli.addSupportInfo(format, ModelFeature.NETWORK_WITH_GAP_JUNCTIONS_MODEL, SupportLevelInfo.Level.MEDIUM);
        sli.addSupportInfo(format, ModelFeature.NETWORK_WITH_ANALOG_CONNS_MODEL, SupportLevelInfo.Level.MEDIUM);
        sli.addSupportInfo(format, ModelFeature.NETWORK_WITH_PROJECTIONS_MODEL, SupportLevelInfo.Level.LOW);
        sli.addSupportInfo(format, ModelFeature.MULTICOMPARTMENTAL_CELL_MODEL, SupportLevelInfo.Level.LOW);
        sli.addSupportInfo(format, ModelFeature.HH_CHANNEL_MODEL, SupportLevelInfo.Level.MEDIUM);
        sli.addSupportInfo(format, ModelFeature.KS_CHANNEL_MODEL, SupportLevelInfo.Level.LOW);
    }

    public List<File> generateAndRun(boolean nogui, boolean run) throws LEMSException, GenerationException, NeuroMLException, IOException, ModelFeatureSupportException
    {

        this.nogui = nogui;
        List<File> files = generateMainScriptAndMods();

        if(run)
        {
            E.info("Trying to compile mods in: " + this.getOutputFolder());

            ProcessManager.compileFileWithNeuron(this.getOutputFolder(), false);

            File neuronHome = findNeuronHome();
            String nrncmd = nogui ? "nrniv" : "nrngui";
            String commandToExecute = neuronHome.getCanonicalPath() + System.getProperty("file.separator") + "bin" + System.getProperty("file.separator") + nrncmd + " -python "
                    + new File(this.getOutputFolder(), this.getOutputFileName()).getCanonicalPath();

            Runtime rt = Runtime.getRuntime();
            Process currentProcess = rt.exec(commandToExecute, null, this.getOutputFolder());
            ProcessOutputWatcher procOutputMain = new ProcessOutputWatcher(currentProcess.getInputStream(), "NRN Output >>");
            procOutputMain.start();

            ProcessOutputWatcher procOutputError = new ProcessOutputWatcher(currentProcess.getErrorStream(), "NRN Error  >>");
            procOutputError.start();

            E.info("Have successfully executed command: " + commandToExecute);

            try
            {
                currentProcess.waitFor();

                E.info("Exit value for compilation: " + currentProcess.exitValue());
            }
            catch(InterruptedException e)
            {
                E.info("Problem executing Neuron " + e);
            }
        }
        return files;
    }

    @Override
    protected void addComment(StringBuilder sb, String comment)
    {

        if(!comment.contains("\n"))
        {
            sb.append(NRNUtils.comm + comment + "\n");
        }
        else
        {
            sb.append(NRNUtils.commPre + "\n" + comment + "\n" + NRNUtils.commPost + "\n");
        }
    }

    private void reset()
    {
        outputFiles.clear();
    }

    public void setNoGui(boolean nogui)
    {
        this.nogui = nogui;
    }

    public boolean isNoGui()
    {
        return nogui;
    }

    public List<File> generateMainScriptAndMods() throws LEMSException, GenerationException, NeuroMLException
    {
        File mainFile = new File(this.getOutputFolder(), this.getOutputFileName());
        try
        {
            String main = getMainScript();
            FileUtil.writeStringToFile(main, mainFile);
            outputFiles.add(mainFile);
        }
        catch(IOException ex)
        {
            throw new GenerationException("Error writing to file: " + mainFile.getAbsolutePath(), ex);
        }
        return outputFiles;
    }

    public String getMainScript() throws GenerationException, NeuroMLException
    {
        try
        {
            reset();
            StringBuilder main = new StringBuilder();

            addComment(main, "Neuron simulator export for:\n\n" + lems.textSummary(false, false) + "\n\n" + Utils.getHeaderComment(format) + "\n");

            main.append("\nimport neuron\n");
            main.append("\nimport time\n");
            main.append("h = neuron.h\n");

            if(nogui)
            {
                main.append("h.load_file(\"stdlib.hoc\")\n\n");
                main.append("h.load_file(\"stdgui.hoc\")\n\n");
            }
            else
            {
                main.append("h.load_file(\"nrngui.hoc\")\n\n");
            }

            main.append("h(\"objref p\")\n");
            main.append("h(\"p = new PythonObject()\")\n\n");

            Target target = lems.getTarget();

            Component simCpt = target.getComponent();

            Component targetComp = simCpt.getRefComponents().get("target");
            String info = "Adding simulation " + simCpt + " of network/component: " + targetComp.summary();

            E.info(info);

            addComment(main, info);

            ArrayList<Component> popsOrComponents = targetComp.getChildrenAL("populations");

            //E.info("popsOrComponents: " + popsOrComponents);

            HashMap<String, Integer> compMechsCreated = new HashMap<String, Integer>();
            HashMap<String, String> compMechNamesHoc = new HashMap<String, String>();

            if(popsOrComponents.isEmpty())
            {
                popsOrComponents.add(targetComp);
            }
            else
            {
                if(targetComp.getComponentType().getName().equals("networkWithTemperature") || (targetComp.hasAttribute("type") && targetComp.getStringValue("type").equals("networkWithTemperature")))
                {
                    String temp = targetComp.getStringValue("temperature");
                    float tempSI = Utils.getMagnitudeInSI(temp);
                    main.append("\n# Temperature used for network: " + tempSI + " K\n");
                    main.append("h.celsius = " + tempSI + " - 273.15\n\n");

                }
            }

            for(Component popsOrComponent : popsOrComponents)
            {
                String compReference;
                String popName;
                int number;
                Component popComp;
                

                HashMap<Integer,String> locations = new HashMap<Integer,String>();


                if(popsOrComponent.getComponentType().getName().equals(NeuroMLElements.POPULATION))
                {
                    number = Integer.parseInt(popsOrComponent.getStringValue(NeuroMLElements.POPULATION_SIZE));
                    popComp = popsOrComponent.getRefComponents().get("component");
                    popName = popsOrComponent.getID();
                    popIdsVsCellIds.put(popName, popComp.getID());
                    popIdsVsComps.put(popName, popComp);
                }
                else if(popsOrComponent.getComponentType().getName().equals(NeuroMLElements.POPULATION_LIST))
                {
                    compReference = popsOrComponent.getStringValue(NeuroMLElements.POPULATION_COMPONENT);
                    popComp = popsOrComponent.getRefComponents().get("component");
                    number = 0;
                    for(Component instance : popsOrComponent.getAllChildren())
                    {
                        if(instance.getComponentType().getName().equals(NeuroMLElements.INSTANCE))
                        {
                            number++;
                            Component loc = instance.getChild(NeuroMLElements.LOCATION);
                            String location = "("+loc.getAttributeValue(NeuroMLElements.LOCATION_X)
                                    +", "+loc.getAttributeValue(NeuroMLElements.LOCATION_Y)
                                    +", "+loc.getAttributeValue(NeuroMLElements.LOCATION_Z)+")";

                            locations.put(Integer.parseInt(instance.getID()), location);
                        }
                    }
                    popComp.getAllChildren().size();
                    popName = popsOrComponent.getID();
                    popIdsVsCellIds.put(popName, compReference);

                    popIdsVsComps.put(popName, popComp);
                }
                else
                {
                    // compReference = popsOrComponent.getComponentType().getName();
                    number = 1;
                    popComp = popsOrComponent;
                    popName = LEMSQuantityPath.DUMMY_POPULATION_PREFIX + popComp.getName();

                }
                E.info("Adding population: "+popName);

                String compTypeName = popComp.getComponentType().getName();

                main.append("print(\"Population " + popName + " contains " + number + " instance(s) of component: " + popComp.getID() + " of type: " + popComp.getComponentType().getName() + "\")\n\n");

                if(popComp.getComponentType().isOrExtends(NeuroMLElements.CELL_COMP_TYPE))
                {

                    Cell cell = getCellFromComponent(popComp);
                    
                    IntracellularProperties ip;
                    if (convertedCells.containsKey(popComp.id))
                    {
                        ip = convertedCells.get(popComp.id);
                    }
                    else
                    {
                        ip = convertCellWithMorphology(popComp);
                        convertedCells.put(popComp.id, ip);
                    }
                    NamingHelper nh = new NamingHelper(cell);
                    
                    String cellName = popComp.getID();
                    String fileName = cellName + ".hoc";
                    
                    
                    for (Species species: ip.getSpecies()) {

                        float internal = NRNUtils.convertToNeuronUnits(Utils.getMagnitudeInSI(species.getInitialConcentration()), "concentration");
                        float external = NRNUtils.convertToNeuronUnits(Utils.getMagnitudeInSI(species.getInitialExtConcentration()), "concentration");
                        main.append("print(\"Setting the default initial concentrations for " + species.getIon() + " (used in "+cellName
                                +") to "+internal+" mM (internal), "+external+" mM (external)\")\n");

                        main.append("h(\"" + species.getIon() + "i0_" + species.getIon() + "_ion = " + internal + "\")\n");
                        main.append("h(\"" + species.getIon() + "o0_" + species.getIon() + "_ion = " + external + "\")\n\n");

                    }
                    
                    StringBuilder popInfo = new StringBuilder();

                    popInfo.append("h.load_file(\"" + fileName + "\")\n");

                    popInfo.append("a_" + popName + " = []\n");

                    popInfo.append("h(\"n_" + popName + " = " + number + "\")\n");

                    popInfo.append("h(\"objectvar a_" + popName + "[n_" + popName + "]\")\n");

                    popInfo.append("for i in range(int(h.n_" + popName + ")):\n");
                    // main.append("    cell = h."+cellName+"()\n");
                    popInfo.append("    h(\"a_" + popName + "[%i] = new " + cellName + "()\"%i)\n");
                    // main.append("    cell."+getNrnSectionName(cell.getMorphology().getSegment().get(0))+".push()\n");
                    popInfo.append("    h(\"access a_" + popName + "[%i]." + nh.getNrnSectionName(cell.getMorphology().getSegment().get(0)) + "\"%i)\n\n");

                    for (Integer cell_id: locations.keySet()) {
                        popInfo.append("h(\"a_" + popName + "["+cell_id+"].position"+locations.get(cell_id)+"\")\n");
                    }

                    popInfo.append(String.format("\nh(\"proc initialiseV_%s() { for i = 0, n_%s-1 { a_%s[i].set_initial_v() } }\")\n", popName, popName, popName));
                    popInfo.append(String.format("h(\"objref fih_%s\")\n", popName));
                    popInfo.append(String.format("h(\'{fih_%s = new FInitializeHandler(0, \"initialiseV_%s()\")}\')\n\n", popName, popName));

                    popInfo.append(String.format("h(\"proc initialiseIons_%s() { for i = 0, n_%s-1 { a_%s[i].set_initial_ion_properties() } }\")\n", popName, popName, popName));
                    popInfo.append(String.format("h(\"objref fih_ion_%s\")\n", popName));
                    popInfo.append(String.format("h(\'{fih_ion_%s = new FInitializeHandler(1, \"initialiseIons_%s()\")}\')\n\n", popName, popName));
                    main.append(popInfo);


                }
                else
                {
                    generateModForComp(popComp);

                    main.append("h(\" {n_" + popName + " = " + number + "} \")\n");

                    String mechName = NRNUtils.getMechanismName(popComp, popName);

                    addComment(main, "Population " + popName + " contains instances of " + popComp + "\n" +
                                     "whose dynamics will be implemented as a mechanism (" + popComp.getID() + ") in a mod file");

                    main.append("h(\" create " + popName + "[" + number + "]\")\n");
                    main.append("h(\" objectvar " + mechName + "[" + number + "] \")\n\n");

                    main.append("for i in range(int(h.n_" + popName + ")):\n");
                    String instName = popName + "[i]";
                    // main.append(instName + " = h.Section()\n");
                    double defaultRadius = 5;
                    main.append("    h." + instName + ".L = " + defaultRadius * 2 + "\n");
                    main.append("    h." + instName + "(0.5).diam = " + defaultRadius * 2 + "\n");

                    if(popComp.getComponentType().isOrExtends(NeuroMLElements.BASE_CELL_CAP_COMP_TYPE))
                    {
                        double capTotSI = popComp.getParamValue("C").getDoubleValue();
                        double area = 4 * Math.PI * defaultRadius * defaultRadius;
                        double specCapNeu = 10e13 * capTotSI / area;
                        main.append("    h." + instName + "(0.5).cm = " + specCapNeu + "\n");
                    }
                    else
                    {
                        main.append("    h." + instName + "(0.5).cm = 318.31927\n");
                    }

                    main.append("    h." + instName + ".push()\n");
                    main.append("    h(\" " + instName.replaceAll("\\[i\\]", "[%i]") + "  { " + mechName + "[%i] = new " + popComp.getID() + "(0.5) } \"%(i,i))\n\n");

                    if(!compMechsCreated.containsKey(compTypeName))
                    {
                        compMechsCreated.put(compTypeName, 0);
                    }

                    compMechsCreated.put(compTypeName, compMechsCreated.get(compTypeName) + 1);

                    String hocMechName = NRNUtils.getMechanismName(popComp, popName) + "[i]";

                    compMechNamesHoc.put(instName, hocMechName);

                    LemsCollection<ParamValue> pvs = popComp.getParamValues();
                    for(ParamValue pv : pvs)
                    {
                        main.append("    " + "h." + hocMechName + "." + pv.getName() + " = " + NRNUtils.convertToNeuronUnits((float) pv.getDoubleValue(), pv.getDimensionName()) + "\n");
                    }
                    main.append("    h.pop_section()\n");

                }

            }

            // / Add projections/connections
            E.info("Adding projections/connections...");

            ArrayList<Component> projections = targetComp.getChildrenAL("projections");

            for(Component projection : projections)
            {
                String id = projection.getID();
                String prePop = projection.getStringValue("presynapticPopulation");
                String postPop = projection.getStringValue("postsynapticPopulation");
                Cell preCell = compIdsVsCells.get(popIdsVsCellIds.get(prePop));
                Cell postCell = compIdsVsCells.get(popIdsVsCellIds.get(postPop));
                String synapse = projection.getStringValue("synapse");
                int number = 0;
                for(Component comp : projection.getAllChildren())
                {

                    if(comp.getComponentType().isOrExtends(NeuroMLElements.CONNECTION))
                    {
                        number++;
                    }
                }

                addComment(main, String.format("Adding projection: %s, from %s to %s with synapse %s, %d connection(s)", id, prePop, postPop, synapse, number));

                Component synapseComp = projection.getRefComponents().get("synapse");

                generateModForComp(synapseComp);

                String synObjName = String.format("syn_%s_%s", id, synapse);

                main.append(String.format("h(\"objectvar %s[%d]\")\n\n", synObjName, number));

                NamingHelper nhPre = new NamingHelper(preCell);
                NamingHelper nhPost = new NamingHelper(postCell);

                int index = 0;
                for(Component conn : projection.getAllChildren())
                {
                    if(conn.getComponentType().isOrExtends(NeuroMLElements.CONNECTION))
                    {
                        int preCellId = Utils.parseCellRefStringForCellNum(conn.getStringValue("preCellId"));
                        int postCellId = Utils.parseCellRefStringForCellNum(conn.getStringValue("postCellId"));

                        int preSegmentId = conn.hasAttribute("preSegmentId") ? Integer.parseInt(conn.getAttributeValue("preSegmentId")) : 0;
                        int postSegmentId = conn.hasAttribute("postSegmentId") ? Integer.parseInt(conn.getAttributeValue("postSegmentId")) : 0;

                        float preFractionAlong = conn.hasAttribute("preFractionAlong") ? Float.parseFloat(conn.getAttributeValue("preFractionAlong")) : 0.5f;
                        float postFractionAlong = conn.hasAttribute("postFractionAlong") ? Float.parseFloat(conn.getAttributeValue("postFractionAlong")) : 0.5f;

                        String comment = String.format(Locale.US, "Connection %s: %d, seg %d (%f) -> %d, seg %d (%f)", conn.getID(), preCellId, preSegmentId, preFractionAlong, postCellId, postSegmentId, postFractionAlong);
                        //System.out.println("comment@: "+comment);
                        addComment(main, comment);


                        String preSecName;

                        if(preCell != null)
                        {
                            preSecName = String.format("a_%s[%s].%s", prePop, preCellId, nhPre.getNrnSectionName(CellUtils.getSegmentWithId(preCell, preSegmentId)));
                        }
                        else
                        {
                            preSecName = prePop + "[" + preCellId + "]";
                        }

                        String postSecName;
                        if(postCell != null)
                        {
                            postSecName = String.format("a_%s[%s].%s", postPop, postCellId, nhPost.getNrnSectionName(CellUtils.getSegmentWithId(postCell, postSegmentId)));
                        }
                        else
                        {
                            postSecName = postPop + "[" + postCellId + "]";
                        }

                        main.append(String.format(Locale.US, "h(\"%s %s[%d] = new %s(%f)\")\n", postSecName, synObjName, index, synapse, postFractionAlong));

                        String sourceVarToListenFor = "&v("+ preFractionAlong+")";

                        float weight = 1;
                        float delay = 0;
                        if (conn.getComponentType().isOrExtends(NeuroMLElements.CONNECTION_WEIGHT_DELAY))
                        {
                            weight = Float.parseFloat(conn.getAttributeValue("weight"));
                            delay = NRNUtils.convertToNeuronUnits(conn.getAttributeValue("delay"), lems);
                        }

                        if(preCell != null)
                        {
                            float threshold;
                            
                            SpikeThresh st = getMembraneProperties(preCell).getSpikeThresh().get(0);
                            if (!st.getSegmentGroup().equals(NeuroMLElements.SEGMENT_GROUP_ALL))
                            {
                                throw new NeuroMLException("Cannot yet handle <spikeThresh> when it is not on segmentGroup all");
                            }

                            threshold = NRNUtils.convertToNeuronUnits(st.getValue(), lems);
                            main.append(String.format("h(\"%s a_%s[%d].synlist.append(new NetCon(%s, %s[%d], %s, %s, %s))\")\n\n", preSecName, postPop, postCellId, sourceVarToListenFor, synObjName,
                                    index, threshold, delay, weight));
                        }
                        else
                        {
                            Component preComp = popIdsVsComps.get(prePop);
                            float threshold = 0;
                            if(preComp.getComponentType().isOrExtends(NeuroMLElements.BASE_IAF_CAP_CELL) || preComp.getComponentType().isOrExtends(NeuroMLElements.BASE_IAF_CELL))
                            {
                                threshold = NRNUtils.convertToNeuronUnits(preComp.getStringValue("thresh"), lems);
                            }
                            else if(preComp.getComponentType().isOrExtends(NeuroMLElements.BASE_SPIKE_SOURCE_COMP_TYPE))
                            {
                                String hocMechName = NRNUtils.getMechanismName(preComp, prePop) + "["+preCellId+"]";
                                sourceVarToListenFor = hocMechName;
                            }
                            main.append(String.format("h(\"objectvar nc_%s_%d\")\n", synObjName, index));
                            main.append(String.format(Locale.US, "h(\"%s nc_%s_%d = new NetCon(%s, %s[%d], %f, %s, %s)\")  \n\n", preSecName, synObjName, index, sourceVarToListenFor, synObjName, index, threshold, delay, weight));
                        }
                        index++;
                    }
                }

            }

            /* <synapticConnection> and <synapticConnectionWD> elements */

            ArrayList<Component> synapticConnections = targetComp.getChildrenAL("synapticConnections");
            synapticConnections.addAll(targetComp.getChildrenAL("synapticConnectionWDs"));

            /* First, group connections by synapse type */
            HashMap<Component, ArrayList<Component>> connectionsByType = new HashMap<Component, ArrayList<Component>>();
            for(Component connection : synapticConnections)
            {
                Component synType = connection.getRefComponents().get("synapse");
                if(connectionsByType.containsKey(synType) == false)
                {
                    connectionsByType.put(synType, new ArrayList<Component>());
                }
                connectionsByType.get(synType).add(connection);
            }

            for(Map.Entry<Component, ArrayList<Component>> entry : connectionsByType.entrySet())
            {

                Component synapseComp = entry.getKey();
                ArrayList<Component> connections = entry.getValue();


                generateModForComp(synapseComp);

                /* Array of synapses of this type */
                String info0 = String.format("Adding synapse %s used in %s connections", synapseComp.getID(), connections.size());
                addComment(main, info0);

                String synArrayName = String.format("synapses_%s", synapseComp.getID());
                main.append(String.format("h(\"objectvar %s[%d]\")\n\n", synArrayName, connections.size()));

                for(int i = 0; i < connections.size(); i++)
                {
                    Component connection = connections.get(i);

                    String fromRef = connection.getStringValue("from");
                    int fromCellId = Utils.parseCellRefStringForCellNum(fromRef);
                    String fromPop = Utils.parseCellRefStringForPopulation(fromRef);
                    Cell fromCell = compIdsVsCells.get(popIdsVsCellIds.get(fromPop));
                    String fromSecName;

                    if(fromCell != null)
                    {
                        NamingHelper nh0 = new NamingHelper(fromCell);
                        fromSecName = String.format("a_%s[%s].%s", fromPop, fromCellId, nh0.getNrnSectionName(fromCell.getMorphology().getSegment().get(0)));
                    }
                    else
                    {
                        fromSecName = fromPop + "[" + fromCellId + "]";
                    }

                    String toRef = connection.getStringValue("to");
                    int toCellId = Utils.parseCellRefStringForCellNum(toRef);
                    String toPop = Utils.parseCellRefStringForPopulation(toRef);
                    Cell toCell = compIdsVsCells.get(popIdsVsCellIds.get(toPop));
                    String toSecName;
                    if(toCell != null)
                    {
                        NamingHelper nh0 = new NamingHelper(toCell);
                        toSecName = String.format("a_%s[%s].%s", toPop, toCellId, nh0.getNrnSectionName(toCell.getMorphology().getSegment().get(0)));
                    }
                    else
                    {
                        toSecName = toPop + "[" + toCellId + "]";
                    }

                    String info1 = String.format("Adding connection from %s to %s", fromRef, toRef);
                    addComment(main, info1);

                    main.append(String.format(Locale.US, "h(\"%s %s[%d] = new %s(%f)\")\n", toSecName, synArrayName, i, synapseComp.getID(), 0.5));

                    float delay = connection.hasParam("delay") ? (float) connection.getParamValue("delay").getDoubleValue() * 1000 : 0.0f;
                    //this also accounts for dimensional weights, if we ever want to support that.
                    float weight = connection.hasParam("weight") ? (float) NRNUtils.convertToNeuronUnits(connection.getAttributeValue("weight") , lems) : 1.0f;

                    if(toCell != null)
                    {
                        Component fromComp = popIdsVsComps.get(fromPop);
                        float fract = 0.5f;
                        String sourceVarToListenFor = "&v("+fract+")";
                        float threshold = 0;

                        if(fromComp.getComponentType().isOrExtends(NeuroMLElements.BASE_IAF_CAP_CELL) || fromComp.getComponentType().isOrExtends(NeuroMLElements.BASE_IAF_CELL))
                        {
                            threshold = NRNUtils.convertToNeuronUnits(fromComp.getStringValue("thresh"), lems);
                        }

                        if(fromComp.getComponentType().isOrExtends(NeuroMLElements.BASE_SPIKE_SOURCE_COMP_TYPE))
                        {
                            String hocMechName = NRNUtils.getMechanismName(fromComp, fromPop) + "["+fromCellId+"]";
                            sourceVarToListenFor = hocMechName;
                        }

                        main.append(String.format("h(\"%s a_%s[%d].synlist.append(new NetCon(%s, %s[%d], %g, %g, %g))\") # ...\n\n", fromSecName, toPop, toCellId, sourceVarToListenFor, synArrayName, i, threshold, delay, weight));
                    }
                    else
                    {
                        Component fromComp = popIdsVsComps.get(fromPop);
                        float threshold = 0;

                        String sourceVarToListenFor = "&v(0.5)";

                        if(fromComp.getComponentType().isOrExtends(NeuroMLElements.BASE_IAF_CAP_CELL) ||
                           fromComp.getComponentType().isOrExtends(NeuroMLElements.BASE_IAF_CELL))
                        {
                            threshold = NRNUtils.convertToNeuronUnits(fromComp.getStringValue("thresh"), lems);
                        }

                        if(fromComp.getComponentType().isOrExtends(NeuroMLElements.BASE_SPIKE_SOURCE_COMP_TYPE))
                        {
                            String hocMechName = NRNUtils.getMechanismName(fromComp, fromPop) + "["+fromCellId+"]";
                            sourceVarToListenFor = hocMechName;
                        }
                        main.append(String.format("h(\"objectvar nc_%s_%d\")\n", synArrayName, i));
                        main.append(String.format("h(\"%s nc_%s_%d = new NetCon(%s, %s[%d], %g, %g, %g)\")  # ,,,\n\n", fromSecName, synArrayName, i, sourceVarToListenFor, synArrayName, i, threshold, delay, weight));
                    }

                }
            }

            ArrayList<Component> electricalProjections = targetComp.getChildrenAL(NeuroMLElements.ELECTRICAL_PROJECTION);

            for(Component ep : electricalProjections)
            {

                String id = ep.getID();
                String prePop = ep.getStringValue("presynapticPopulation");
                String postPop = ep.getStringValue("postsynapticPopulation");
                Cell preCell = compIdsVsCells.get(popIdsVsCellIds.get(prePop));
                Cell postCell = compIdsVsCells.get(popIdsVsCellIds.get(postPop));
                int number = 0;
                for(Component comp : ep.getAllChildren())
                {

                    if(comp.getComponentType().isOrExtends(NeuroMLElements.ELECTRICAL_CONNECTION) ||
                       comp.getComponentType().isOrExtends(NeuroMLElements.ELECTRICAL_CONNECTION_INSTANCE))
                    {
                        number++;
                    }
                }

                String info0 = String.format("Adding projection: %s\nFrom %s to %s, with %d connection(s)", id, prePop, postPop, number);
                // System.out.println(info0);
                addComment(main, info0);

                Component synapseComp = ep.getChildrenAL("connections").get(0).getRefComponents().get("synapse");

                generateModForComp(synapseComp);

                String synObjNameA = String.format("syn_%s_%s_A", id, synapseComp.getID());
                String synObjNameB = String.format("syn_%s_%s_B", id, synapseComp.getID());

                main.append(String.format("h(\"objectvar %s[%d]\")\n\n", synObjNameA, number));
                main.append(String.format("h(\"objectvar %s[%d]\")\n\n", synObjNameB, number));

                int index = 0;
                for(Component ec : ep.getChildrenAL("connections"))
                {

                    if(ec.getComponentType().isOrExtends(NeuroMLElements.ELECTRICAL_CONNECTION) ||
                       ec.getComponentType().isOrExtends(NeuroMLElements.ELECTRICAL_CONNECTION_INSTANCE))
                    {
                        int preCellId = -1;
                        int postCellId = -1;

                        if(ec.getComponentType().isOrExtends(NeuroMLElements.ELECTRICAL_CONNECTION))
                        {
                            preCellId = Integer.parseInt(ec.getStringValue("preCell"));
                            postCellId = Integer.parseInt(ec.getStringValue("postCell"));
                        }
                        else if (ec.getComponentType().isOrExtends(NeuroMLElements.ELECTRICAL_CONNECTION_INSTANCE))
                        {
                            preCellId = Utils.parseCellRefStringForCellNum(ec.getStringValue("preCell"));
                            postCellId = Utils.parseCellRefStringForCellNum(ec.getStringValue("postCell"));
                        }

                        int preSegmentId = ec.hasAttribute("preSegmentId") ? Integer.parseInt(ec.getAttributeValue("preSegmentId")) : 0;
                        int postSegmentId = ec.hasAttribute("postSegmentId") ? Integer.parseInt(ec.getAttributeValue("postSegmentId")) : 0;

                        float preFractionAlong = ec.hasAttribute("preFractionAlong") ? Float.parseFloat(ec.getAttributeValue("preFractionAlong")) : 0.5f;
                        float postFractionAlong = ec.hasAttribute("postFractionAlong") ? Float.parseFloat(ec.getAttributeValue("postFractionAlong")) : 0.5f;

                        // System.out.println("preCellId: "+preCellId+", preSegmentId: "+preSegmentId+", preFractionAlong: "+preFractionAlong);

                        String preSecName;

                        if(preCell != null)
                        {
                            NamingHelper nhPre = new NamingHelper(preCell);
                            preSecName = String.format("a_%s[%s].%s", prePop, preCellId, nhPre.getNrnSectionName(CellUtils.getSegmentWithId(preCell, preSegmentId)));
                        }
                        else
                        {
                            preSecName = prePop + "[" + preCellId + "]";
                        }

                        String postSecName;
                        if(postCell != null)
                        {
                            NamingHelper nhPost = new NamingHelper(postCell);
                            postSecName = String.format("a_%s[%s].%s", postPop, postCellId, nhPost.getNrnSectionName(CellUtils.getSegmentWithId(postCell, postSegmentId)));
                        }
                        else
                        {
                            postSecName = postPop + "[" + postCellId + "]";
                        }

                        main.append(String.format(Locale.US, "h(\"%s { %s[%d] = new %s(%f) }\")\n", preSecName, synObjNameA, index, synapseComp.getID(), preFractionAlong));
                        main.append(String.format(Locale.US, "h(\"%s { %s[%d] = new %s(%f) }\")\n", postSecName, synObjNameB, index, synapseComp.getID(), postFractionAlong));

                        // addComment(main, "setpointer elecsyn_NetConn_PrePassiveCG_PostPassiveCG_GapJunc2_A[0].vgap, a_PrePassiveCG[0].Soma.v(0.5)");

                        /*
                         * TODO: remove hard coded vpeer/v link & figure this out from Component(Type) definition!!
                         */
                        main.append(String.format(Locale.US, "h(\"setpointer %s[%d].vpeer, %s.v(%f)\")\n", synObjNameA, index, postSecName, postFractionAlong));
                        main.append(String.format(Locale.US, "h(\"setpointer %s[%d].vpeer, %s.v(%f)\")\n", synObjNameB, index, preSecName, preFractionAlong));

                        index++;
                    }
                }
            }

            ArrayList<Component> continuousProjections = targetComp.getChildrenAL(NeuroMLElements.CONTINUOUS_PROJECTION);

            for(Component ep : continuousProjections)
            {

                String id = ep.getID();
                String prePop = ep.getStringValue("presynapticPopulation");
                String postPop = ep.getStringValue("postsynapticPopulation");
                Cell preCell = compIdsVsCells.get(popIdsVsCellIds.get(prePop));
                Cell postCell = compIdsVsCells.get(popIdsVsCellIds.get(postPop));
                int number = 0;
                for(Component comp : ep.getAllChildren())
                {

                    if(comp.getComponentType().getName().equals(NeuroMLElements.CONTINUOUS_CONNECTION) ||
                       comp.getComponentType().getName().equals(NeuroMLElements.CONTINUOUS_CONNECTION_INSTANCE))
                    {
                        number++;
                    }
                }

                String info0 = String.format("Adding projection: %s\nFrom %s to %s %d connection(s)", id, prePop, postPop, number);
                // System.out.println(info0);
                addComment(main, info0);

                Component preComponent = ep.getChildrenAL("connections").get(0).getRefComponents().get("preComponent");
                Component postComponent = ep.getChildrenAL("connections").get(0).getRefComponents().get("postComponent");

                generateModForComp(preComponent);
                generateModForComp(postComponent);

                String preCompObjName = String.format("syn_%s_%s_pre", id, preComponent.getID());
                String postCompObjName = String.format("syn_%s_%s_post", id, postComponent.getID());

                main.append(String.format("h(\"objectvar %s[%d]\")\n\n", preCompObjName, number));
                main.append(String.format("h(\"objectvar %s[%d]\")\n\n", postCompObjName, number));

                int index = 0;
                for(Component ec : ep.getChildrenAL("connections"))
                {

                    if(ec.getComponentType().getName().equals(NeuroMLElements.CONTINUOUS_CONNECTION) ||
                       ec.getComponentType().getName().equals(NeuroMLElements.CONTINUOUS_CONNECTION_INSTANCE))
                    {
                        int preCellId = -1;
                        int postCellId = -1;
                        
                        if(ec.getComponentType().getName().equals(NeuroMLElements.CONTINUOUS_CONNECTION))
                        {
                            preCellId = Integer.parseInt(ec.getStringValue("preCell"));
                            postCellId = Integer.parseInt(ec.getStringValue("postCell"));
                        }
                        else if(ec.getComponentType().getName().equals(NeuroMLElements.CONTINUOUS_CONNECTION_INSTANCE))
                        {
                            preCellId = Utils.parseCellRefStringForCellNum(ec.getStringValue("preCell"));
                            postCellId = Utils.parseCellRefStringForCellNum(ec.getStringValue("postCell"));
                        }
                        int preSegmentId = ec.hasAttribute("preSegment") ? Integer.parseInt(ec.getAttributeValue("preSegment")) : 0;
                        int postSegmentId = ec.hasAttribute("postSegment") ? Integer.parseInt(ec.getAttributeValue("postSegment")) : 0;

                        float preFractionAlong = ec.hasAttribute("preFractionAlong") ? Float.parseFloat(ec.getAttributeValue("preFractionAlong")) : 0.5f;
                        float postFractionAlong = ec.hasAttribute("postFractionAlong") ? Float.parseFloat(ec.getAttributeValue("postFractionAlong")) : 0.5f;

                        // System.out.println("preCellId: "+preCellId+", preSegmentId: "+preSegmentId+", preFractionAlong: "+preFractionAlong);

                        String preSecName;

                        if(preCell != null)
                        {
                            NamingHelper nhPre = new NamingHelper(preCell);
                            preSecName = String.format("a_%s[%s].%s", prePop, preCellId, nhPre.getNrnSectionName(CellUtils.getSegmentWithId(preCell, preSegmentId)));
                        }
                        else
                        {
                            preSecName = prePop + "[" + preCellId + "]";
                        }

                        String postSecName;
                        if(postCell != null)
                        {
                            NamingHelper nhPost = new NamingHelper(postCell);
                            postSecName = String.format("a_%s[%s].%s", postPop, postCellId, nhPost.getNrnSectionName(CellUtils.getSegmentWithId(postCell, postSegmentId)));
                        }
                        else
                        {
                            postSecName = postPop + "[" + postCellId + "]";
                        }

                        main.append(String.format(Locale.US, "h(\"%s { %s[%d] = new %s(%f) }\")\n", preSecName, preCompObjName, index, preComponent.getID(), preFractionAlong));
                        main.append(String.format(Locale.US, "h(\"%s { %s[%d] = new %s(%f) }\")\n", postSecName, postCompObjName, index, postComponent.getID(), postFractionAlong));

                        // addComment(main, "setpointer elecsyn_NetConn_PrePassiveCG_PostPassiveCG_GapJunc2_A[0].vgap, a_PrePassiveCG[0].Soma.v(0.5)");

                        /*
                         * TODO: remove hard coded vpeer/v link & figure this out from Component(Type) definition!!
                         */
                        main.append(String.format(Locale.US, "h(\"setpointer %s[%d].vpeer, %s.v(%f)\")\n", preCompObjName, index, postSecName, postFractionAlong));
                        main.append(String.format(Locale.US, "h(\"setpointer %s[%d].vpeer, %s.v(%f)\")\n", postCompObjName, index, preSecName, preFractionAlong));

                        index++;
                    }
                }
            }

            processInputLists(main, targetComp);
            processExplicitInputs(main, targetComp);

            main.append("trec = h.Vector()\n");
            main.append("trec.record(h._ref_t)\n\n");

            StringBuilder toRec = new StringBuilder();

            ArrayList<String> displayGraphs = new ArrayList<String>();
            HashMap<String, ArrayList<String>> plots = new HashMap<String, ArrayList<String>>();

            if(!nogui)
            {
                for(Component dispComp : simCpt.getAllChildren())
                {
                    if(dispComp.getTypeName().equals("Display"))
                    {
                        String dispId = dispComp.getID();
                        int plotColour = 1;

                        String dispGraph = "display_" + dispId;
                        if(!displayGraphs.contains(dispGraph))
                        {
                            displayGraphs.add(dispGraph);
                        }

                        for(Component lineComp : dispComp.getAllChildren())
                        {
                            if(lineComp.getTypeName().equals("Line"))
                            {
                                String quantity = lineComp.getStringValue("quantity");
                                String scale = lineComp.getStringValue("scale");

                                LEMSQuantityPathNeuron lqp = new LEMSQuantityPathNeuron(quantity, scale, targetComp, compMechNamesHoc, popsOrComponents, compIdsVsCells, lems);

                                if(plots.get(dispGraph) == null)
                                {
                                    plots.put(dispGraph, new ArrayList<String>());
                                }

                                plots.get(dispGraph).add("# Line, plotting: " + lqp.getQuantity());
                                // plots.get(dispGraph).add("# compMechNamesHoc: " + compMechNamesHoc);
                                // plots.get(dispGraph).add("# " + lqp.toString().replaceAll("\n", "\n# "));

                                plots.get(dispGraph).add(
                                        dispGraph + ".addexpr(\"" + lqp.getNeuronVariableReference() + "\", \"" + lqp.getNeuronVariableReference() + "\", " + plotColour + ", 1, 0.8, 0.9, 2)");
                                plotColour++;
                                if(plotColour > 10)
                                {
                                    plotColour = 1;
                                }

                            }
                        }
                    }
                }
            }
            main.append(toRec);

            String len = simCpt.getStringValue("length");
            len = len.replaceAll("ms", "");
            if(len.indexOf("s") > 0)
            {
                len = len.replaceAll("s", "").trim();
                len = "" + Float.parseFloat(len) * 1000;
            }

            String dt = simCpt.getStringValue("step");
            dt = dt.replaceAll("ms", "").trim();
            if(dt.indexOf("s") > 0)
            {
                dt = dt.replaceAll("s", "").trim();
                dt = "" + Float.parseFloat(dt) * 1000;
            }

            main.append("h.tstop = " + len + "\n\n");
            main.append("h.dt = " + dt + "\n\n");
            main.append("h.steps_per_ms = " + (float) (1d / Double.parseDouble(dt)) + "\n\n");

            if(!nogui)
            {
                for(String dg : displayGraphs)
                {
                    addComment(main, "Display: " + dg);
                    main.append(dg + " = h.Graph(0)\n");
                    main.append(dg + ".size(0,h.tstop,-80.0,50.0)\n");
                    main.append(dg + ".view(0, -80.0, h.tstop, 130.0, 80, 330, 330, 250)\n");
                    main.append("h.graphList[0].append(" + dg + ")\n");
                    for(String plot : plots.get(dg))
                    {
                        main.append(plot + "\n");
                    }
                    main.append("\n");
                }
            }

            main.append("\n\n");

            HashMap<String, String> outfiles = new HashMap<String, String>();
            HashMap<String, ArrayList<String>> columnsPre = new HashMap<String, ArrayList<String>>();
            HashMap<String, ArrayList<String>> columnsPost0 = new HashMap<String, ArrayList<String>>();
            HashMap<String, ArrayList<String>> columnsPostTraces = new HashMap<String, ArrayList<String>>();
            HashMap<String, ArrayList<String>> columnsPostSpikes = new HashMap<String, ArrayList<String>>();

            String timeRef = "time";
            String timefileName = target.timesFile != null ? target.timesFile : "time.dat";
            outfiles.put(timeRef, timefileName);

            columnsPre.put(timeRef, new ArrayList<String>());
            columnsPost0.put(timeRef, new ArrayList<String>());
            columnsPostTraces.put(timeRef, new ArrayList<String>());
            columnsPostSpikes.put(timeRef, new ArrayList<String>());

            columnsPre.get(timeRef).add("# Column: " + timeRef);
            columnsPre.get(timeRef).add("h(' objectvar v_" + timeRef + " ')");
            columnsPre.get(timeRef).add("h(' { v_" + timeRef + " = new Vector() } ')");
            columnsPre.get(timeRef).add("h(' v_" + timeRef + ".record(&t) ')");
            columnsPre.get(timeRef).add("h.v_" + timeRef + ".resize((h.tstop * h.steps_per_ms) + 1)");

            columnsPost0.get(timeRef).add("py_v_" + timeRef + " = [ t/1000 for t in h.v_" + timeRef + ".to_python() ]  # Convert to Python list for speed...");

            columnsPostTraces.get(timeRef).add("    f_" + timeRef + "_f2.write('%f'% py_v_" + timeRef + "[i])  # Save in SI units...");

            for(Component ofComp : simCpt.getAllChildren())
            {
                if(ofComp.getTypeName().equals("OutputFile"))
                {
                    String outfileId = ofComp.getID().replaceAll(" ", "_");
                    outfiles.put(outfileId, ofComp.getTextParam("fileName"));
                    if(columnsPre.get(outfileId) == null)
                    {
                        columnsPre.put(outfileId, new ArrayList<String>());
                    }
                    if(columnsPostTraces.get(outfileId) == null)
                    {
                        columnsPostTraces.put(outfileId, new ArrayList<String>());
                    }
                    if(columnsPost0.get(outfileId) == null)
                    {
                        columnsPost0.put(outfileId, new ArrayList<String>());
                    }

                    columnsPostTraces.get(outfileId).add("    f_" + outfileId + "_f2.write('%e\\t'% py_v_" + timeRef + "[i] ");

                    ArrayList<String> colIds = new ArrayList<String>();

                    for(Component colComp : ofComp.getAllChildren())
                    {
                        if(colComp.getTypeName().equals("OutputColumn"))
                        {
                            String colId = colComp.getID().replaceAll(" ", "_") + "_" + outfileId;
                            while(colIds.contains(colId))
                            {
                                colId += "_";
                            }
                            colIds.add(colId);
                            String quantity = colComp.getStringValue("quantity");
                            String scale = "1";

                            LEMSQuantityPathNeuron lqp = new LEMSQuantityPathNeuron(quantity, scale, targetComp, compMechNamesHoc, popsOrComponents, compIdsVsCells, lems);

                            columnsPre.get(outfileId).add("# Column: " + lqp.getQuantity());
                            columnsPre.get(outfileId).add("h(' objectvar v_" + colId + " ')");
                            columnsPre.get(outfileId).add("h(' { v_" + colId + " = new Vector() } ')");
                            columnsPre.get(outfileId).add("h(' v_" + colId + ".record(&" + lqp.getNeuronVariableReference() + ") ')");
                            columnsPre.get(outfileId).add("h.v_" + colId + ".resize((h.tstop * h.steps_per_ms) + 1)");

                            float conv = NRNUtils.getNeuronUnitFactor(lqp.getDimension().getName());
                            String factor = (conv == 1) ? "" : " / " + conv;

                            columnsPost0.get(outfileId).add(
                                    "py_v_" + colId + " = [ float(x " + factor + ") for x in h.v_" + colId + ".to_python() ]  # Convert to Python list for speed, variable has dim: " + lqp.getDimension().getName());

                            columnsPostTraces.get(outfileId).add(
                                    " + '%e\\t'%(py_v_" + colId + "[i]) ");

                        }
                    }
                }
                if(ofComp.getTypeName().equals("EventOutputFile"))
                {
                    String outfileId = ofComp.getID().replaceAll(" ", "_");
                    outfiles.put(outfileId, ofComp.getTextParam("fileName"));
                    String eofFormat = ofComp.getTextParam("format");
                    if(columnsPre.get(outfileId) == null)
                    {
                        columnsPre.put(outfileId, new ArrayList<String>());
                    }
                    if(columnsPostSpikes.get(outfileId) == null)
                    {
                        columnsPostSpikes.put(outfileId, new ArrayList<String>());
                    }
                    if(columnsPost0.get(outfileId) == null)
                    {
                        columnsPost0.put(outfileId, new ArrayList<String>());
                    }

                    ArrayList<String> colIds = new ArrayList<String>();

                    String spikeVecName = "spiketimes_" + outfileId;
                    String ncName = "netConnSpike_" + outfileId;
                    columnsPre.get(outfileId).add("h(' objectvar " + spikeVecName + ", t_" + spikeVecName + " ')");
                    columnsPre.get(outfileId).add("h(' { " + spikeVecName + " = new Vector() } ')");
                    columnsPre.get(outfileId).add("h(' { t_" + spikeVecName + " = new Vector() } ')");
                    columnsPre.get(outfileId).add("h(' objref "+ncName+", nil ')");

                    columnsPostSpikes.get(outfileId).add("h(' objref "+ncName+" ')");

                    columnsPostSpikes.get(outfileId).add("spike_ids = h." + spikeVecName + ".to_python()  ");
                    columnsPostSpikes.get(outfileId).add("spike_times = h.t_" + spikeVecName + ".to_python()");
                    columnsPostSpikes.get(outfileId).add("for i, id in enumerate(spike_ids):");
                    columnsPostSpikes.get(outfileId).add("    # Saving in format: "+eofFormat);
                    if (eofFormat.equals(EventWriter.FORMAT_TIME_ID))
                    {
                        columnsPostSpikes.get(outfileId).add("    f_" + outfileId + "_f2.write(\"%s\\t%i\\n\"%(spike_times[i],id))");
                    }
                    else if (eofFormat.equals(EventWriter.FORMAT_ID_TIME))
                    {
                        columnsPostSpikes.get(outfileId).add("    f_" + outfileId + "_f2.write(\"%i\\t%s\\n\"%(id,spike_times[i]))");
                    }


                    for(Component colComp : ofComp.getAllChildren())
                    {
                        if(colComp.getTypeName().equals("EventSelection"))
                        {
                            String colId = colComp.getID().replaceAll(" ", "_") + "_" + outfileId;
                            while(colIds.contains(colId))
                            {
                                colId += "_";
                            }
                            colIds.add(colId);
                            String select = colComp.getStringValue("select");
                            String id = colComp.getID();

                            int srcCellNum = Utils.parseCellRefStringForCellNum(select);
                            String srcCellPop = Utils.parseCellRefStringForPopulation(select);
                            Cell srcCell = compIdsVsCells.get(popIdsVsCellIds.get(srcCellPop));
                            String srcSecName;
                            float threshold = 0;

                            if(srcCell != null)
                            {
                                NamingHelper nh0 = new NamingHelper(srcCell);
                                srcSecName = String.format("a_%s[%s].%s", srcCellPop, srcCellNum, nh0.getNrnSectionName(srcCell.getMorphology().getSegment().get(0)));
                                SpikeThresh st = srcCell.getBiophysicalProperties().getMembraneProperties().getSpikeThresh().get(0);
                                if (!st.getSegmentGroup().equals(NeuroMLElements.SEGMENT_GROUP_ALL))
                                {
                                    throw new NeuroMLException("Cannot yet handle <spikeThresh> when it is not on segmentGroup all");
                                }
                                threshold = NRNUtils.convertToNeuronUnits(st.getValue(), lems);
                            }
                            else
                            {
                                srcSecName = srcCellPop + "[" + srcCellNum + "]";
                                Component preComp = popIdsVsComps.get(srcCellPop);
                                if(preComp.getComponentType().isOrExtends(NeuroMLElements.BASE_IAF_CAP_CELL) || preComp.getComponentType().isOrExtends(NeuroMLElements.BASE_IAF_CELL))
                                {
                                    threshold = NRNUtils.convertToNeuronUnits(preComp.getStringValue("thresh"), lems);
                                }
                            }

                            columnsPre.get(outfileId).add("# Column: " + select+" ("+id+") "+srcSecName);
                            columnsPre.get(outfileId).add("h(' "+srcSecName+" { "+ncName+" = new NetCon(&v(0.5), nil, "+threshold+", 0, 1) } ')");
                            columnsPre.get(outfileId).add("h(' { "+ncName+".record(t_"+spikeVecName+", "+spikeVecName+", "+id+") } ')");

                        }
                    }
                }
            }

            for(String f : outfiles.keySet())
            {
                addComment(main, "File to save: " + f);
                for(String col : columnsPre.get(f))
                {
                    main.append(col + "\n");
                }
                main.append("\n");
            }

            main.append("\n\n");

            if(!nogui)
            {
                main.append("h.nrncontrolmenu()\n");
            }

            main.append("sim_start = time.time()\n");
            main.append("print(\"Running a simulation of %sms (dt = %sms)\" % (h.tstop, h.dt))\n\n");
            main.append("h.run()\n\n");
            main.append("sim_end = time.time()\n");
            main.append("sim_time = sim_end - sim_start\n");
            main.append("print(\"Finished simulation in %f seconds (%f mins), saving results...\"%(sim_time, sim_time/60.0))\n\n");

            // main.append("objref SampleGraph\n");
            for(String dg : displayGraphs)
            {
                main.append(dg + ".exec_menu(\"View = plot\")\n");
            }
            main.append("\n");

            // Ensure time gets handled first
            Set<String> refs = outfiles.keySet();
            List<String> refList = new ArrayList<String>(refs);
            refList.remove(timeRef);
            refList.add(0, timeRef);

            for(String f : refList)
            {
                addComment(main, "File to save: " + f);
                for(String col : columnsPost0.get(f))
                {
                    main.append(col + "\n");
                }
                main.append("\nf_" + f + "_f2 = open('" + outfiles.get(f) + "', 'w')\n");

                if (columnsPostTraces.containsKey(f))
                {
                    main.append("for i in range(int(h.tstop * h.steps_per_ms) + 1):\n");
                    for(String col : columnsPostTraces.get(f))
                    {
                        main.append(col);
                    }
                    main.append("+ '\\n\')\n");
                }
                if (columnsPostSpikes.containsKey(f))
                {
                    //main.append("for i in range(int(h.tstop * h.steps_per_ms) + 1):\n");
                    for(String col : columnsPostSpikes.get(f))
                    {
                        main.append(""+col+"\n");
                    }
                    //main.append("+ '\\n\')\n");
                }
                main.append("f_" + f + "_f2.close()\n");
                main.append("print(\"Saved data to: " + outfiles.get(f) + "\")\n\n");
            }

            main.append("save_end = time.time()\n");
            main.append("save_time = save_end - sim_end\n");
            main.append("print(\"Finished saving results in %f seconds\"%(save_time))\n\n");
            main.append("print(\"Done\")\n\n");
            if(nogui)
            {
                main.append("quit()\n");
            }

            return main.toString();
        }
        catch(LEMSException e)
        {
            throw new GenerationException("Error with LEMS content", e);
        }

    }

    private void processExplicitInputs(StringBuilder main, Component targetComp)
            throws LEMSException, ContentError, NeuroMLException {
        ArrayList<Component> explicitInputs = targetComp.getChildrenAL("explicitInputs");

        for(Component explInput : explicitInputs)
        {
            HashMap<String, Component> inputReference = explInput.getRefComponents();

            Component inputComp = inputReference.get("input");

            String safeName = NRNUtils.getSafeName(inputComp.getID());
            String inputName = explInput.getTypeName() + "_" + safeName;
            generateModForComp(inputComp);

            String secName = parseInputSecName(explInput);
//            inputName += "_" + popName + "_" + cellNum + "_" + secName.replaceAll("\\[", "").replaceAll("\\]", "").replaceAll("\\.", "_");
            inputName += secName.replaceAll("\\[", "").replaceAll("\\]", "").replaceAll("\\.", "_");

            generateHocForInput(main, explInput, inputComp, inputName);
        }
    }


    private void processInputLists(StringBuilder main, Component targetComp) throws LEMSException,
            ContentError, NeuroMLException {

        for(Component inputList : targetComp.getChildrenAL("inputs")) {

            generateModForComp(inputList.getRefComponents().get("component"));

            for(Component input : inputList.getChildrenAL("inputs")) {
                String inputName = NRNUtils.getSafeName(inputList.getID()) + "_" + input.getID();
                Component inputComp = inputList.getRefComponents().get("component");
                generateHocForInput(main, input, inputComp, inputName);
            }
        }
    }

    private void generateHocForInput(StringBuilder main, Component input, Component inputComp, String inputName) throws ContentError,
            NeuroMLException, ParseError {

        float fractionAlong = parseFractionAlong(input);

        addComment(main, "Adding input: " + input);

        String safeInputName = NRNUtils.getSafeName(inputComp.getID());

        if(!inputComp.getComponentType().isOrExtends("timedSynapticInput")) {
            main.append(String.format("\nh(\"objref %s\")\n", inputName));
            main.append(String.format("h(\"%s { %s = new %s(%f) } \")\n\n", parseInputSecName(input), inputName, safeInputName, fractionAlong));
        } else {
            processTimeDependentLiterals(main, input, inputComp);
        }
    }

    private String parseInputSecName(Component input) throws ContentError,
            NeuroMLException {
        String targetString = input.getStringValue("target");
        int segmentId = parseSegmentId(input);
        int cellNum = Utils.parseCellRefStringForCellNum(targetString);
        String popName = Utils.parseCellRefStringForPopulation(targetString);
        String secName = generateSecName(popName, cellNum, segmentId);
        return secName;
    }

    private String generateSecName(String popName, int cellNum, int segmentId)
            throws NeuroMLException {
        String secName;
        String cellId = popIdsVsCellIds.get(popName);
        Cell cell = compIdsVsCells.get(cellId);
        if (cell != null) {
            Segment segment;
            if (segmentId > 0)
                segment = CellUtils.getSegmentWithId(cell, segmentId);
            else
                segment = cell.getMorphology().getSegment().get(0);
            NamingHelper nh0 = new NamingHelper(cell);
            secName = String.format("a_%s[%s].%s", popName, cellNum, nh0.getNrnSectionName(segment));
        } else {
            secName = popName + "[" + cellNum + "]";
        }
        return secName;
    }

    private float parseFractionAlong(Component input) throws ContentError {
        return input.hasAttribute("fractionAlong") ? Float.parseFloat(input.getAttributeValue("fractionAlong")) : 0.5f;
    }

    private int parseSegmentId(Component input) throws ContentError {
        return input.hasAttribute("segmentId") ? Integer.parseInt(input.getAttributeValue("segmentId")) : -1;
    }

    private void processTimeDependentLiterals(StringBuilder main, Component input, Component inputComp)
            throws ContentError, NeuroMLException, ParseError {
        // TODO Auto-generated method stub
        String inputName = NRNUtils.getSafeName(inputComp.getID());
        addComment(main, "Generating event source for point process " + input);
        Component synapse = inputComp.getRefComponents().get("synapse");
        String synSafeName = NRNUtils.getSafeName(synapse.getID());
        String synFullName =  inputName + "_" + synSafeName+"_"+input.getID();
        main.append(String.format("%s = h.%s(%f, sec=h.%s)\n",
                                        synFullName,
                                        synSafeName,
                                        parseFractionAlong(input),
                                        parseInputSecName(input)));
        String helperFunc = "def singleNetStimT(tstim):\n\tn=h.NetStim()\n\tn.number = 1\n\tn.start=tstim\n\treturn n\n";
        //if (main.indexOf("def singleNetStimT(tstim):\n\tn=h.NetStim()\n\tn.number = 1\n\tn.start=tstim\n\treturn n\n"))
        main.append(helperFunc);
        List<String> spkTimes = new ArrayList<String>();
        for(Component spk : inputComp.getComponents()) {
            float spkTime = NRNUtils.convertToNeuronUnits(spk.getAttributeValue("time"), lems);
            spkTimes.add(Float.toString(spkTime));
        }
        String stimName = String.format("%s_stims_%s", inputName,input.getID());
        //stimName = String.format("%s_stims", inputName);
        main.append(String.format("%s = [singleNetStimT(t) for t in %s]\n", stimName, spkTimes));
        main.append(String.format("%s_netCons_%s = [h.NetCon(s, %s, 0, 0, 1) for s in %s]\n", inputName, input.getID(), synFullName, stimName));

    }


    private void generateModForComp(Component comp) throws LEMSException, ContentError {
        if(comp.getComponentType().isOrExtends("timedSynapticInput")) {
            // timedSynapticInput leverages netstim, so no mod generation needed
            // TODO: probably all "literal" time dependency should be implemented this way
            comp = comp.getRefComponents().get("synapse");
        }
        if(modWritten.containsKey(comp.getID()))
        {
            E.info("-- Mod file for: " + comp.getID() + " has already been created");
            return;
        }
        
        String mod = generateModFile(comp);
        saveModToFile(comp, mod);
    }

    private void writeModFile(Component comp, ChannelConductanceOption option) throws LEMSException
    {
        if(!generatedModComponents.contains(comp.getID()))
        {
            String mod = generateModFile(comp, option);
            generatedModComponents.add(comp.getID());

            saveModToFile(comp, mod);
        }
    }

    public File saveModToFile(Component comp, String mod) throws ContentError
    {
        File modFile = new File(getOutputFolder(), NRNUtils.getSafeName(comp.getID()) + ".mod");
        if(modWritten.containsKey(comp.getID()) && modWritten.get(comp.getID()).equals(mod))
        {
            E.info("-- Mod file for: " + comp.getID() + " has already been written");
            return modFile;
        }
        E.info("-- Writing to mod: " + modFile.getAbsolutePath());

        try
        {
            FileUtil.writeStringToFile(mod, modFile, true);
            this.outputFiles.add(modFile);
            modWritten.put(comp.getID(), mod);
        }
        catch(IOException ex)
        {
            throw new ContentError("Error writing to file: " + modFile.getAbsolutePath(), ex);
        }
        return modFile;
    }
    
    public MembraneProperties getMembraneProperties(Cell cell) 
    {
        if (cell instanceof Cell2CaPools) {
            Cell2CaPools cell2ca = (Cell2CaPools)cell;
            BiophysicalProperties2CaPools bp2 = cell2ca.getBiophysicalProperties2CaPools();
            return bp2.getMembraneProperties2CaPools();
        }
        else
        {
            BiophysicalProperties bp = cell.getBiophysicalProperties();
            return bp.getMembraneProperties();
        }
    }
    
    private Cell getCellFromComponent(Component cellComponent) throws LEMSException, NeuroMLException 
    {
        Cell cell;
        if (compIdsVsCells.containsKey(cellComponent.getID())) 
        {
            cell = compIdsVsCells.get(cellComponent.getID());
        }
        else
        {
            cell = Utils.getCellFromComponent(cellComponent);
            compIdsVsCells.put(cellComponent.getID(), cell);
        }
        return cell;
    }
    
    public IntracellularProperties convertCellWithMorphology(Component cellComponent) throws LEMSException, NeuroMLException 
    {
        
        Cell cell = getCellFromComponent(cellComponent);
        
        String cellString = generateCellFile(cell);
        String cellName = cellComponent.getID();

        String fileName = cellName + ".hoc";
        File cellFile = new File(getOutputFolder(), fileName);
        IntracellularProperties ip;
        MembraneProperties2CaPools mpCa2 = null;
        Component bpComp;
        Component mpComp;
        Component ipComp;
        if (cell instanceof Cell2CaPools) {
            Cell2CaPools cell2ca = (Cell2CaPools)cell;
            BiophysicalProperties2CaPools bp2 = cell2ca.getBiophysicalProperties2CaPools();
            mpCa2 = bp2.getMembraneProperties2CaPools();
            ip = bp2.getIntracellularProperties2CaPools();
            bpComp = cellComponent.getChild("biophysicalProperties2CaPools");
            mpComp = bpComp.getChild("membraneProperties2CaPools");
            ipComp = bpComp.getChild("intracellularProperties2CaPools");
        }
        else
        {
            BiophysicalProperties bp = cell.getBiophysicalProperties();
            ip = bp.getIntracellularProperties();
            bpComp = cellComponent.getChild("biophysicalProperties");
            mpComp = bpComp.getChild("membraneProperties");
            ipComp = bpComp.getChild("intracellularProperties");
        }

        try
        {
            if (!outputFiles.contains(cellFile))
            {
                E.info("-- Writing to hoc: " + cellFile);
                FileUtil.writeStringToFile(cellString, cellFile);
                this.outputFiles.add(cellFile);

                for(Component channelDensity : mpComp.getChildrenAL("channelDensities"))
                {
                    if (channelDensity.getTypeName().equals("channelDensity") || channelDensity.getTypeName().equals("channelDensityNonUniform")){
                        ChannelConductanceOption option = ChannelConductanceOption.FIXED_REVERSAL_POTENTIAL;
                        option.erev = NRNUtils.convertToNeuronUnits((float)channelDensity.getParamValue("erev").getDoubleValue(), "voltage");

                        writeModFile(channelDensity.getRefHM().get("ionChannel"), option);
                    }
                    else if (channelDensity.getTypeName().equals("channelDensityNernst")){
                        ChannelConductanceOption option = ChannelConductanceOption.USE_NERNST;
                        writeModFile(channelDensity.getRefHM().get("ionChannel"), option);
                    }
                    else if (channelDensity.getTypeName().equals("channelDensityNernstCa2") && mpCa2!=null){
                        ChannelConductanceOption option = ChannelConductanceOption.USE_NERNST;
                        writeModFile(channelDensity.getRefHM().get("ionChannel"), option);
                    }
                    else if (channelDensity.getTypeName().equals("channelDensityGHK") || channelDensity.getTypeName().equals("channelDensityNonUniformGHK") ){
                        ChannelConductanceOption option = ChannelConductanceOption.USE_GHK;
                        writeModFile(channelDensity.getRefHM().get("ionChannel"), option);
                    }
                }

                for(Component species : ipComp.getChildrenAL("speciesList"))
                {
                    writeModFile(species.getRefHM().get("concentrationModel"),null);
                }
            }
        }
        catch(IOException ex)
        {
            throw new ContentError("Error writing to file: " + cellFile.getAbsolutePath(), ex);
        }
        
        return ip;
    }

    public static String generateCellFile(Cell cell) throws LEMSException, NeuroMLException
    {
        StringBuilder cellString = new StringBuilder();

        cellString.append("// Cell: " + cell.getId() + "\n");
        String json = JSONCellSerializer.cellToJson(cell, SupportedUnits.NEURON);
        cellString.append("/*\n" + json + "\n*/");

        VelocityUtils.initializeVelocity();

        VelocityContext context = new VelocityContext();

        try
        {
            DLemsWriter.putIntoVelocityContext(json, context);
        }
        catch(IOException ex)
        {
            throw new NeuroMLException("Problem converting Cell to JSON format", ex);
        }

        VelocityEngine ve = VelocityUtils.getVelocityEngine();

        StringWriter sw1 = new StringWriter();
        boolean generationStatus = ve.evaluate(context, sw1, "LOG", VelocityUtils.getTemplateAsReader(VelocityUtils.neuronCellTemplateFile));
        cellString.append(sw1.toString());
        return cellString.toString();
    }

    // TODO: to be made more general
    public enum SupportedUnits
    {
        SI(1, 1, 1, 1, 1, 1, 1), PHYSIOLOGICAL(1000, 1e-6f, 100, 0.1f, 0.1f, 1e-6f, 1e-1f), NEURON(1000, 1e-6f, 100, 100, 1e-4f, 1, 100);

        SupportedUnits(float voltageFactor, float lengthFactor, float specCapFactor, float resistivityFactor, float condDensFactor, float concentrationFactor, float permeabilityFactor)
        {
            this.voltageFactor = voltageFactor;
            this.lengthFactor = lengthFactor;
            this.specCapFactor = specCapFactor;
            this.resistivityFactor = resistivityFactor;
            this.condDensFactor = condDensFactor;
            this.concentrationFactor = concentrationFactor;
            this.permeabilityFactor = permeabilityFactor;
        }

        public float voltageFactor;
        public float lengthFactor;
        public float specCapFactor;
        public float resistivityFactor;
        public float condDensFactor;
        public float concentrationFactor;
        public float permeabilityFactor;
    };

    public static String formatDefault(float num)
    {
        // final DecimalFormat formatter = new DecimalFormat("#.#");
        return num + "";// +formatter.format(num);
    }

    public String generateModFile(Component comp) throws LEMSException
    {
        return generateModFile(comp, null);
    }

    public String generateModFile(Component comp, ChannelConductanceOption condOption) throws LEMSException
    {
        StringBuilder mod = new StringBuilder();

        String mechName = comp.getID();

        mod.append("TITLE Mod file for component: " + comp + "\n\n");

        mod.append("COMMENT\n\n" + Utils.getHeaderComment(Format.NEURON) + "\n\nENDCOMMENT\n\n");

        StringBuilder blockNeuron = new StringBuilder();
        StringBuilder blockUnits = new StringBuilder();
        StringBuilder blockParameter = new StringBuilder();
        StringBuilder blockAssigned = new StringBuilder();
        StringBuilder blockState = new StringBuilder();
        StringBuilder blockKinetic = new StringBuilder();
        StringBuilder blockInitial = new StringBuilder();
        StringBuilder blockInitial_v = new StringBuilder();
        StringBuilder blockBreakpoint = new StringBuilder();
        StringBuilder blockBreakpoint_regimes = new StringBuilder();
        StringBuilder blockDerivative = new StringBuilder();
        StringBuilder blockNetReceive = new StringBuilder();
        StringBuilder blockFunctions = new StringBuilder();

        String blockNetReceiveParams;
        StringBuilder ratesMethod = new StringBuilder("\n");

        HashMap<String, HashMap<String, String>> paramMappings = new HashMap<String, HashMap<String, String>>();

        if(comp.getComponentType().isOrExtends(NeuroMLElements.BASE_CELL_COMP_TYPE))
        {
            HashMap<String, String> paramMappingsComp = new HashMap<String, String>();
            paramMappings.put(comp.getUniqueID(), paramMappingsComp);
        }

        blockUnits.append(NRNUtils.generalUnits);

        ArrayList<String> locals = new ArrayList<String>();

        boolean hasCaDependency = false;
        String ionSpecies = "<unknown ion>";

        if(comp.getComponentType().isOrExtends(NeuroMLElements.BASE_ION_CHANNEL_COMP_TYPE))
        {

            for(Component child1 : comp.getAllChildren())
            {
                if(child1.getComponentType().isOrExtends(NeuroMLElements.BASE_GATE_COMP_TYPE))
                {
                    for(Component child2 : child1.getAllChildren())
                    {
                        if(child2.getComponentType().isOrExtends(NeuroMLElements.BASE_CONC_DEP_VAR_COMP_TYPE) || child2.getComponentType().isOrExtends(NeuroMLElements.BASE_CONC_DEP_RATE_COMP_TYPE))
                        {
                            hasCaDependency = true;
                        }

                        for(Component child3 : child2.getAllChildren())
                        {
                            if(child3.getComponentType().isOrExtends(NeuroMLElements.BASE_CONC_DEP_VAR_COMP_TYPE) || child3.getComponentType().isOrExtends(NeuroMLElements.BASE_CONC_DEP_RATE_COMP_TYPE))
                            {
                                hasCaDependency = true;
                            }
                        }
                    }
                    for(Requirement r : child1.getComponentType().getRequirements())
                    {
                        if(r.getName().equals(NRNUtils.caConc))
                        {
                            hasCaDependency = true;
                        }
                    }
                }

                if(child1.getComponentType().isOrExtends(NeuroMLElements.BASE_COND_SCALING_CA))
                {
                    hasCaDependency = true;
                }
            }

            mechName = NRNUtils.getSafeName(comp.getID());
            blockNeuron.append("SUFFIX " + mechName + "\n");

            String species = comp.getTextParam("species");

            if(species == null || species.equals("non_specific"))
            {
                blockNeuron.append("NONSPECIFIC_CURRENT i\n");
                blockNeuron.append("RANGE e\n");
            }
            else
            {
                String readRevPot = "";

                if(condOption == null || (condOption.equals(ChannelConductanceOption.USE_NERNST)))
                {
                    readRevPot = "READ e" + species + " ";
                }
                else if(condOption.equals(ChannelConductanceOption.FIXED_REVERSAL_POTENTIAL))
                {
                    blockInitial.append("e" + species + " = " + condOption.erev + "\n\n");
                }
                else if(condOption.equals(ChannelConductanceOption.USE_GHK))
                {

                    blockFunctions.append(NRNUtils.ghkFunctionDefs);
                    blockUnits.append(NRNUtils.ghkUnits);
                    readRevPot = String.format("READ %si, %so ", species, species);

                }
                if(readRevPot.length() == 0 && hasCaDependency)
                {
                    if(species.contains("ca"))
                    {
                        readRevPot = "READ cai,cao ";
                    }
                    else
                    {
                        blockNeuron.append("USEION ca READ cai,cao VALENCE 2\n"); // TODO check valence
                    }
                }

                if(species.contains("ca"))
                {
                    blockNeuron.append("USEION " + species + " " + readRevPot + "WRITE i" + species + " VALENCE 2 ? Assuming valence = 2 (Ca ion); TODO check this!!\n");
                }
                else
                {
                    blockNeuron.append("USEION " + species + " " + readRevPot + "WRITE i" + species + " VALENCE 1 ? Assuming valence = 1; TODO check this!!\n");
                }
            }

            blockNeuron.append("\nRANGE gion                           ");

            if(condOption == null || condOption.equals(ChannelConductanceOption.FIXED_REVERSAL_POTENTIAL) || condOption.equals(ChannelConductanceOption.USE_NERNST))
            {
                blockNeuron.append("\nRANGE gmax                              : Will be changed when ion channel mechanism placed on cell!\n");
                blockParameter.append("\ngmax = 0  (S/cm2)                       : Will be changed when ion channel mechanism placed on cell!\n");
            }
            else if(condOption.equals(ChannelConductanceOption.USE_GHK))
            {
                blockNeuron.append("\nRANGE permeability                      : Will be changed when ion channel mechanism placed on cell!\n");
                blockParameter.append("\npermeability = 0  (cm/s)                       : Will be changed when ion channel mechanism placed on cell!\n");
                blockNeuron.append("RANGE cai\n");
                blockNeuron.append("RANGE cao\n");
                blockAssigned.append("cai (mM)\n");
                blockAssigned.append("cao (mM)\n");
            }

            blockAssigned.append("\ngion   (S/cm2)                          : Transient conductance density of the channel");

        }
        else if(comp.getComponentType().isOrExtends(NeuroMLElements.CONC_MODEL_COMP_TYPE))
        {
            mechName = NRNUtils.getSafeName(comp.getID());
            blockNeuron.append("SUFFIX " + mechName + "\n");

            String ion = comp.getStringValue("ion");
            ionSpecies = ion;

            if(ion != null)
            {
                blockNeuron.append("USEION " + ion + " READ " + ion + "i, " + ion + "o, i" + ion + " WRITE " + ion + "i VALENCE 2\n"); // TODO check valence
            }

            blockNeuron.append("RANGE "+ion+"i\n");
            blockNeuron.append("RANGE "+ion+"o\n");

            blockAssigned.append(""+ion+"i (mM)\n");
            blockAssigned.append(""+ion+"o (mM)\n");
            blockAssigned.append("i"+ion+" (mA/cm2)\n");
            blockAssigned.append("diam (um)\n");
            blockAssigned.append("area (um2)\n");

            blockParameter.append(NeuroMLElements.CONC_MODEL_SURF_AREA + " "+NRNUtils.getNeuronUnit("area")+"\n");

            // This works for ca -> iCa & ca2 -> iCa2, but should be more general...
            String totalCaCurrent = "i"+ion.replaceAll("ca", "Ca");

            blockParameter.append(totalCaCurrent + " "+NRNUtils.getNeuronUnit("current")+"\n");

            ratesMethod.append(NeuroMLElements.CONC_MODEL_SURF_AREA + " = area   : "
                               +NeuroMLElements.CONC_MODEL_SURF_AREA+" has units "+NRNUtils.getNeuronUnit("area")
                               +", area (built in to NEURON) is in um^2...\n\n");

            ratesMethod.append(totalCaCurrent + " = -1 * (0.01) * i"+ion+" * " + NeuroMLElements.CONC_MODEL_SURF_AREA
                    + " :   "+totalCaCurrent+" has units "+NRNUtils.getNeuronUnit("current")+" ; i"+ion+" (built in to NEURON) has units (mA/cm2)...\n\n");

            blockNeuron.append("GLOBAL " + NeuroMLElements.CONC_MODEL_INIT_CONC + "\n");
            blockNeuron.append("GLOBAL " + NeuroMLElements.CONC_MODEL_INIT_EXT_CONC + "\n");
            blockParameter.append(NeuroMLElements.CONC_MODEL_INIT_CONC + " "+NRNUtils.getNeuronUnit("concentration")+"\n");
            blockParameter.append(NeuroMLElements.CONC_MODEL_INIT_EXT_CONC + " "+NRNUtils.getNeuronUnit("concentration")+"\n");

            blockInitial.append(NeuroMLElements.CONC_MODEL_INIT_CONC + " = "+ion+"i" + "\n");
            blockInitial.append(NeuroMLElements.CONC_MODEL_INIT_EXT_CONC + " = "+ion+"o" + "\n");

        }
        else if(comp.getComponentType().isOrExtends(NeuroMLElements.BASE_POINT_CURR_COMP_TYPE))
        {
            mechName = NRNUtils.getSafeName(comp.getID());
            blockNeuron.append("POINT_PROCESS " + mechName + "\n");
            if(!comp.getComponentType().isOrExtends(NeuroMLElements.BASE_SYNAPSE_COMP_TYPE))
            {
                blockNeuron.append("ELECTRODE_CURRENT i\n");
            }
        }
        else if(comp.getComponentType().isOrExtends(NeuroMLElements.BASE_SPIKE_SOURCE_COMP_TYPE))
        {
            blockNeuron.append("POINT_PROCESS " + mechName + "\n");
        }
        else
        {
            blockNeuron.append("POINT_PROCESS " + mechName + "\n");
        }

        if(comp.getComponentType().isOrExtends(NeuroMLElements.BASE_VOLT_DEP_CURR_SRC_SPIKING_COMP_TYPE))
        {
            blockAssigned.append("v (mV)\n");
        }

        if(comp.getComponentType().isOrExtends(NeuroMLElements.BASE_SYNAPSE_COMP_TYPE))
        {
            blockAssigned.append("? Standard Assigned variables with baseSynapse\n");
            blockAssigned.append("v (mV)\n");
            blockAssigned.append(NRNUtils.NEURON_TEMP + " (degC)\n");
            blockAssigned.append(NeuroMLElements.TEMPERATURE + " (K)\n");
            blockNetReceiveParams = "weight"; //dimensionless!
        }
        else
        {
            blockNetReceiveParams = "flag";
        }

        String prefix = "";

        ArrayList<String> rangeVars = new ArrayList<String>();
        ArrayList<String> stateVars = new ArrayList<String>();

        parseStateVars(comp, prefix, rangeVars, stateVars, blockNeuron, blockParameter, blockAssigned, blockState, paramMappings);

        parseParameters(comp, prefix, prefix, rangeVars, stateVars, blockNeuron, blockParameter, paramMappings);

        if(comp.getComponentType().isOrExtends(NeuroMLElements.BASE_ION_CHANNEL_COMP_TYPE))
        {
            blockAssigned.append("? Standard Assigned variables with ionChannel\n");
            blockAssigned.append("v (mV)\n");
            blockAssigned.append(NRNUtils.NEURON_TEMP + " (degC)\n");
            blockAssigned.append(NeuroMLElements.TEMPERATURE + " (K)\n");

            String species = comp.getTextParam("species");

            if(species == null || species.equals("non_specific"))
            {
                blockAssigned.append("e (mV)\n");
                blockAssigned.append("i (mA/cm2)\n");
            }
            else
            {
                blockAssigned.append("e" + species + " (mV)\n");
                blockAssigned.append("i" + species + " (mA/cm2)\n");
            }
            blockAssigned.append("\n");
            if(hasCaDependency)
            {
                blockAssigned.append("cai (mM)\n\n");
                blockAssigned.append("cao (mM)\n\n");

                locals.add("caConc");
                ratesMethod.append("caConc = cai\n\n");

            }
        }

        parseDerivedVars(comp, prefix, rangeVars, ratesMethod, blockNeuron, blockParameter, blockAssigned, blockBreakpoint, paramMappings);

        if(comp.getComponentType().isOrExtends(NeuroMLElements.BASE_ION_CHANNEL_COMP_TYPE))
        {

            if(condOption == null || condOption.equals(ChannelConductanceOption.FIXED_REVERSAL_POTENTIAL) || condOption.equals(ChannelConductanceOption.USE_NERNST))
            {
                blockBreakpoint.append("gion = gmax * fopen \n\n");
            }
            else if(condOption.equals(ChannelConductanceOption.USE_GHK))
            {
                blockBreakpoint.append("gion = permeability * fopen \n");
            }
            String species = comp.getTextParam("species");

            // Only for ohmic!!
            if(species == null || species.equals("non_specific"))
            {
                blockBreakpoint.append("i = gion * (v - e)\n");
            }
            else
            {
                if(condOption == null || condOption.equals(ChannelConductanceOption.FIXED_REVERSAL_POTENTIAL) || condOption.equals(ChannelConductanceOption.USE_NERNST))
                {
                    blockBreakpoint.append("i" + species + " = gion * (v - e" + species + ")\n");
                }
                else if(condOption.equals(ChannelConductanceOption.USE_GHK))
                {
                    blockBreakpoint.append("i" + species + " = gion * ghk(v, cai, cao)\n");
                }
            }
        }
//        else if(comp.getComponentType().isOrExtends(NeuroMLElements.BASE_SYNAPSE_COMP_TYPE))
//        {
//            // ratesMethod.append("i = -1 * i ? Due to different convention in synapses\n");
//        }

        parseTimeDerivs(comp, prefix, locals, blockDerivative, blockBreakpoint, blockAssigned, ratesMethod, paramMappings, ionSpecies);

        if(blockDerivative.length() > 0)
        {
            blockBreakpoint.insert(0, "SOLVE states METHOD cnexp\n\n");
        }

        if(comp.getComponentType().isOrExtends(NeuroMLElements.ION_CHANNEL_KS_COMP_TYPE))
        {
            blockBreakpoint.insert(0, "SOLVE activation METHOD sparse ? "+comp.summary()+"\n\n");
            parseKS(comp, blockKinetic, prefix);
        }

        HashMap<String, Integer> flagsVsRegimes = new HashMap<String, Integer>();

        if(comp.getComponentType().hasDynamics())
        {

            int regimeFlag = 5000;
            for(Regime regime : comp.getComponentType().getDynamics().getRegimes())
            {
                flagsVsRegimes.put(regime.name, regimeFlag); // fill
                regimeFlag++;
            }

            for(Regime regime : comp.getComponentType().getDynamics().getRegimes())
            {
                String regimeStateName = NRNUtils.REGIME_PREFIX + regime.name;
                for(OnCondition oc : regime.getOnConditions())
                {

                    String cond = NRNUtils.checkForBinaryOperators(oc.test);

                    blockNetReceive.append("\nif (flag == 1) { : Setting watch condition for " + regimeStateName + "\n");
                    blockNetReceive.append("    WATCH (" + NRNUtils.checkForStateVarsAndNested(cond, comp, paramMappings) + ") " + regimeFlag + "\n");
                    blockNetReceive.append("}\n\n");

                    // test.append("    if (" + NRNUtils.checkForStateVarsAndNested(cond,
                    // comp, paramMappings) + ") {\n");
                    blockNetReceive.append("if (" + regimeStateName + " == 1 && flag == " + regimeFlag + ") { : Setting actions for " + regimeStateName + "\n");

                    if(debug)
                    {
                        blockNetReceive.append("\n        printf(\"+++++++ Start condition (" + oc.test + ") for " + regimeStateName + " at time: %g, v: %g\\n\", t, v)\n");
                    }

                    blockNetReceive.append("\n        : State assignments\n");
                    for(StateAssignment sa : oc.getStateAssignments())
                    {
                        blockNetReceive.append("\n        " + NRNUtils.getStateVarName(sa.getStateVariable().getName()) + " = "
                                + NRNUtils.checkForStateVarsAndNested(sa.getValueExpression(), comp, paramMappings) + "\n");
                    }
                    for(Transition trans : oc.getTransitions())
                    {
                        blockNetReceive.append("\n        : Change regime flags\n");
                        blockNetReceive.append("        " + regimeStateName + " = 0\n");
                        blockNetReceive.append("        " + NRNUtils.REGIME_PREFIX + trans.regime + " = 1\n");

                        Regime targetRegime = comp.getComponentType().getDynamics().getRegimes().getByName(trans.regime);
                        if(targetRegime != null)
                        {
                            blockNetReceive.append("\n        : OnEntry to " + targetRegime + "\n");
                            for(OnEntry oe : targetRegime.getOnEntrys())
                            {
                                for(StateAssignment sa : oe.getStateAssignments())
                                {
                                    blockNetReceive.append("\n        " + sa.getStateVariable().getName() + " = " + NRNUtils.checkForStateVarsAndNested(sa.getValueExpression(), comp, paramMappings)
                                            + "\n");
                                }
                            }
                        }
                    }

                    if(debug)
                    {
                        blockNetReceive.append("\n        printf(\"+++++++ End condition (" + oc.test + ") for " + regimeStateName + " at time: %g, v: %g\\n\", t, v)\n");
                    }

                    blockNetReceive.append("}\n");

                }
                regimeFlag++;
            }
        }

        String localsLine = "";
        if(!locals.isEmpty())
        {
            localsLine = "LOCAL ";
        }
        for(String local : locals)
        {
            localsLine = localsLine + local;
            if(!locals.get(locals.size() - 1).equals(local))
            {
                localsLine = localsLine + ", ";
            }
            else
            {
                localsLine = localsLine + "\n";
            }

        }

        ratesMethod.insert(0, localsLine);

        if(comp.getComponentType().isOrExtends(NeuroMLElements.BASE_ION_CHANNEL_COMP_TYPE) || comp.getComponentType().isOrExtends(NeuroMLElements.BASE_SYNAPSE_COMP_TYPE))
        {
            blockInitial.append(NeuroMLElements.TEMPERATURE + " = " + NRNUtils.NEURON_TEMP + " + 273.15\n\n");
        }

        blockInitial.append("rates()\n");
        blockInitial.append("rates() ? To ensure correct initialisation.\n");

        if(comp.getComponentType().isOrExtends(NeuroMLElements.ION_CHANNEL_KS_COMP_TYPE))
        {
            blockInitial.append("SOLVE activation STEADYSTATE sparse\n");
        }

        parseOnStart(comp, prefix, blockInitial, blockInitial_v, blockNetReceive, paramMappings, lems);

        int conditionFlag = 1000;
        Dynamics dyn = comp.getComponentType().getDynamics();

        parseOnCondition(comp, prefix, blockBreakpoint, blockNetReceive, paramMappings, conditionFlag);

        parseOnEvent(comp, blockNetReceive, paramMappings);

        if(comp.getComponentType().isOrExtends(NeuroMLElements.CONC_MODEL_COMP_TYPE) && comp.getComponentType().getDynamics().getTimeDerivatives().isEmpty())
        {
            blockBreakpoint.append("\n"+ionSpecies+"i = " + NeuroMLElements.CONC_MODEL_CONC_STATE_VAR + "\n\n");
        }

        if(blockInitial_v.toString().trim().length() > 0)
        {
            blockNetReceive.append("if (flag == 1) { : Set initial states\n");
            blockNetReceive.append(blockInitial_v.toString());
            blockNetReceive.append("}\n");
        }

        if(dyn != null)
        {
            for(StateVariable sv : dyn.getStateVariables())
            {

                if(sv.getName().equals(NRNUtils.NEURON_VOLTAGE))
                {

                    blockBreakpoint.append("\n" + NRNUtils.V_COPY_PREFIX + NRNUtils.NEURON_VOLTAGE + " = " + NRNUtils.NEURON_VOLTAGE);

                    if(comp.getComponentType().isOrExtends(NeuroMLElements.BASE_CELL_CAP_COMP_TYPE))
                    {
                        blockBreakpoint.append("\ni = " + NRNUtils.getStateVarName(NRNUtils.NEURON_VOLTAGE) + " * C");
                    }
                    else
                    {
                        blockBreakpoint.append("\ni = " + NRNUtils.getStateVarName(NRNUtils.NEURON_VOLTAGE) + "");
                    }
                }
            }
        }

        writeModBlock(mod, "NEURON", blockNeuron.toString());

        writeModBlock(mod, "UNITS", blockUnits.toString());

        writeModBlock(mod, "PARAMETER", blockParameter.toString());

        if(blockAssigned.length() > 0)
        {
            writeModBlock(mod, "ASSIGNED", blockAssigned.toString());
        }

        writeModBlock(mod, "STATE", blockState.toString());
        writeModBlock(mod, "INITIAL", blockInitial.toString());

        if(blockDerivative.length() == 0 && !comp.getComponentType().isOrExtends(NeuroMLElements.ION_CHANNEL_KS_COMP_TYPE))
        {
            blockBreakpoint.insert(0, "rates()\n");
        }

        if(dyn != null)
        {
            for(Regime regime : dyn.getRegimes())
            {
                String reg = "regime_" + regime.getName();
                if(debug)
                {
                    blockBreakpoint.insert(0, "printf(\"     " + reg + ": %g\\n\", " + reg + ")\n");
                }
            }
        }
        if(debug)
        {
            blockBreakpoint.insert(0, "printf(\"+++++++ Entering BREAKPOINT in " + comp.getName() + " at time: %g, v: %g\\n\", t, v)\n");
        }

        writeModBlock(mod, "BREAKPOINT", blockBreakpoint_regimes.toString() + "\n" + blockBreakpoint.toString());

        if(blockKinetic.length()>0)
        {
            writeModBlock(mod, "KINETIC activation", blockKinetic.toString());
        }

        if(blockNetReceive.length() > 0)
        {
            writeModBlock(mod, "NET_RECEIVE(" + blockNetReceiveParams + ")", blockNetReceive.toString());
        }

        if(blockDerivative.length() > 0)
        {
            blockDerivative.insert(0, "rates()\n");
            writeModBlock(mod, "DERIVATIVE states", blockDerivative.toString());
        }

        writeModBlock(mod, "PROCEDURE rates()", ratesMethod.toString());

        if (blockInitial.indexOf("random")>0 ||
            blockNetReceive.indexOf("random")>0 ||
            ratesMethod.indexOf("random")>0)
        {
            blockFunctions.append(NRNUtils.randomFunctionDefs);
        }

        if (blockInitial.indexOf("H(")>0 ||
            blockNetReceive.indexOf("H(")>0 ||
            ratesMethod.indexOf("H(")>0)
        {
            blockFunctions.append(NRNUtils.heavisideFunctionDefs);
        }

        if(blockFunctions.length() > 0)
        {
            mod.append(blockFunctions.toString());
        }

        return mod.toString();
    }

    private void parseKS(Component comp, StringBuilder blockKinetic, String prefix) throws ContentError {
        /*******
         *
         * What follows is a hard coded implementation of the ionChannelKS for mod files
         * This is required since the <KineticScheme> element in LEMS does most of the
         * "magic" when solving the initial state, generating the rates of changes of states
         * etc.
         *
         *******/

        blockKinetic.insert(0,"rates()\n\n");
        HashMap<String,String> lines = new HashMap<String, String>();

        for (Component c: comp.getAllChildren())
        {
            if (c.getComponentType().isOrExtends("gateKS")){
                String prefix2 = prefix;
                Set<String> gateMicroStates = new HashSet<String>();
                prefix2 = prefix2+c.id+"_";
                for (Component cc: c.getAllChildren())
                {
                    if (cc.getComponentType().isOrExtends("forwardTransition"))
                    {
                        String from = prefix2+cc.getStringValue("from")+"_occupancy";
                        String to = prefix2+cc.getStringValue("to")+"_occupancy";
                        gateMicroStates.addAll(Arrays.asList(from, to));
                        String rate = prefix2+cc.id+"_rate_r";
                        lines.put(from+"_"+to, "~ "+from+" <-> "+to+"    ("+rate+",REV_RATE)\n");

                    }
                    if (cc.getComponentType().isOrExtends("reverseTransition"))
                    {
                        String from = prefix2+cc.getStringValue("from")+"_occupancy";
                        String to = prefix2+cc.getStringValue("to")+"_occupancy";
                        gateMicroStates.addAll(Arrays.asList(from, to));
                        String rate = prefix2+cc.id+"_rate_r";
                        blockKinetic.append(""+lines.get(from+"_"+to).replaceAll("REV_RATE", rate));

                    }
                    if (cc.getComponentType().isOrExtends("tauInfTransition"))
                    {
                        String from = prefix2+cc.getStringValue("from")+"_occupancy";
                        String to = prefix2+cc.getStringValue("to")+"_occupancy";
                        String fwdrate = prefix2+cc.id+"_rf";
                        String revrate = prefix2+cc.id+"_rr";
                        gateMicroStates.addAll(Arrays.asList(from, to));
                        blockKinetic.append("~ "+from+" <-> "+to+"    ("+fwdrate+", "+revrate+")\n");
                    }
                }

                blockKinetic.append("CONSERVE " + StringUtils.join(gateMicroStates.toArray(), '+') + "= 1\n\n");
            }
        }
    }

    private void parseOnStart(Component comp, String prefix, StringBuilder blockInitial, StringBuilder blockInitial_v, StringBuilder blockNetReceive, HashMap<String, HashMap<String, String>> paramMappings, Lems lems)
            throws LEMSException
    {

        HashMap<String, String> paramMappingsComp = paramMappings.get(comp.getUniqueID());

        if(comp.getComponentType().hasDynamics())
        {
            for(Regime regime : comp.getComponentType().getDynamics().getRegimes())
            {
                String regimeStateName = NRNUtils.REGIME_PREFIX + regime.name;
                if(regime.initial != null && regime.initial.equals("true"))
                {
                    blockInitial.append("\n" + regimeStateName + " = 1\n");
                }
                else
                {
                    blockInitial.append("\n" + regimeStateName + " = 0\n");
                }
            }
            String addAfterwards = "";
            for(OnStart os : comp.getComponentType().getDynamics().getOnStarts())
            {
                for(StateAssignment sa : os.getStateAssignments())
                {
                    String var = NRNUtils.getStateVarName(sa.getStateVariable().getName());

                    if(paramMappingsComp.containsKey(var))
                    {
                        var = paramMappingsComp.get(var);
                    }

                    if(sa.getStateVariable().getName().equals(NRNUtils.NEURON_VOLTAGE))
                    {
                        var = sa.getStateVariable().getName();

                        blockInitial.append("\nnet_send(0, 1) : go to NET_RECEIVE block, flag 1, for initial state\n");
                        blockInitial_v.append("\n    " + var + " = " + NRNUtils.checkForStateVarsAndNested(sa.getValueExpression(), comp, paramMappings) + "\n");
                    }
                    else
                    {
                        blockInitial.append("\n" + var + " = " + NRNUtils.checkForStateVarsAndNested(sa.getValueExpression(), comp, paramMappings) + "\n");
                    }
                }
                if(comp.getComponentType().isOrExtends(NeuroMLElements.BASE_SPIKE_SOURCE_COMP_TYPE) ||
                   comp.getComponentType().isOrExtends(NeuroMLElements.BASE_VOLT_DEP_CURR_SRC_SPIKING_COMP_TYPE))
                {
                    addAfterwards += "\nnet_send(0, 1) : go to NET_RECEIVE block, flag 1, for initial state\n";
                }
            }
            blockInitial.append(addAfterwards);
        }

        int flag = 1;
        for(Component childComp : comp.getAllChildren())
        {

            String prefixNew = getPrefix(childComp, prefix);

            if(!comp.getComponentType().isOrExtends(NeuroMLElements.SPIKE_ARRAY)) { // since this will be hard coded as a more efficient impl, see below
                parseOnStart(childComp, prefixNew, blockInitial, blockInitial_v, blockNetReceive, paramMappings, lems);
            } else {

                float time = NRNUtils.convertToNeuronUnits(childComp.getAttributeValue("time"), lems);

                blockNetReceive.append(": Adding watch for spike "+comp.id+" at "+time+"\n");
                blockNetReceive.append("if (flag == "+flag+") { \n");
                if (flag>1) {
                    blockNetReceive.append("    tsince = 0\n");
                    blockNetReceive.append("    net_event(t)\n");
                }
                flag += 1;
                blockNetReceive.append("    WATCH ( t > "+time+") " + flag + "\n");
                blockNetReceive.append("}\n\n");

            }
        }

        if(comp.getComponentType().isOrExtends(NeuroMLElements.SPIKE_ARRAY)) {
            blockNetReceive.append("if (flag == "+flag+") { \n");
            blockNetReceive.append("    tsince = 0\n");
            blockNetReceive.append("    net_event(t)\n");
            blockNetReceive.append("}\n\n");
        }

    }

    private static void parseOnCondition(Component comp, String prefix, StringBuilder blockBreakpoint, StringBuilder blockNetReceive, HashMap<String, HashMap<String, String>> paramMappings, int conditionFlag) throws ContentError
    {
        if(comp.getComponentType().getDynamics() != null)
        {
            for(OnCondition oc : comp.getComponentType().getDynamics().getOnConditions())
            {
                String cond = NRNUtils.checkForBinaryOperators(oc.test);

                boolean resetVoltage = false;
                for(StateAssignment sa : oc.getStateAssignments())
                {
                    resetVoltage = resetVoltage || sa.getStateVariable().getName().equals(NRNUtils.NEURON_VOLTAGE);
                }

                if(!resetVoltage)  // A "normal" OnCondition
                {
                    if (! (comp.getComponentType().isOrExtends(NeuroMLElements.BASE_SPIKE_SOURCE_COMP_TYPE) ||
                           comp.getComponentType().isOrExtends(NeuroMLElements.BASE_VOLT_DEP_CURR_SRC_SPIKING_COMP_TYPE)))
                    {
                        blockBreakpoint.append("if (" + NRNUtils.checkForStateVarsAndNested(cond, comp, paramMappings) + ") {");
                        for(StateAssignment sa : oc.getStateAssignments())
                        {
                            blockBreakpoint.append("\n    " + NRNUtils.getStateVarName(sa.getStateVariable().getName()) + " = "
                                    + NRNUtils.checkForStateVarsAndNested(sa.getValueExpression(), comp, paramMappings) + " ? standard OnCondition\n");
                        }
                        blockBreakpoint.append("}\n\n");
                    }
                    else
                    {
                        blockNetReceive.append("\nLOCAL weight\n\n");
                        blockNetReceive.append("\nif (flag == 1) { : Setting watch for top level OnCondition...\n");
                        blockNetReceive.append("    WATCH (" + NRNUtils.checkForStateVarsAndNested(cond, comp, paramMappings) + ") " + conditionFlag + "\n");

                        blockNetReceive.append("}\n");
                        blockNetReceive.append("if (flag == " + conditionFlag + ") {\n");
                        if(debug)
                        {
                            blockNetReceive.append("    printf(\"Condition (" + NRNUtils.checkForStateVarsAndNested(cond, comp, paramMappings) + "), " + conditionFlag
                                    + ", satisfied at time: %g, v: %g\\n\", t, v)\n");
                        }
                        for(StateAssignment sa : oc.getStateAssignments())
                        {
                            blockNetReceive.append("\n    " + prefix + sa.getStateVariable().getName() + " = " + NRNUtils.checkForStateVarsAndNested(sa.getValueExpression(), comp, paramMappings) +"\n");
                        }
                        for(Component childComp : comp.getAllChildren())
                        {
                            blockNetReceive.append("\n    : Child: "+childComp+"\n");
                            if (childComp.getComponentType().isOrExtends(NeuroMLElements.BASE_SYNAPSE_COMP_TYPE))
                            {
                                blockNetReceive.append("\n    : This child is a synapse; defining weight\n" +
                                                       "    weight = 1\n");
                            }
                            for(OnEvent oe : childComp.getComponentType().getDynamics().getOnEvents())
                            {
                                if(oe.getPortName().equals(NeuroMLElements.SYNAPSE_PORT_IN))
                                {
                                    for(StateAssignment sa : oe.getStateAssignments())
                                    {
                                        blockNetReceive.append("\n    : paramMappings: "+paramMappings+"\n");
                                        blockNetReceive.append("?    state_discontinuity(" + NRNUtils.checkForStateVarsAndNested(sa.getStateVariable().getName(), childComp, paramMappings) + ", "
                                                + NRNUtils.checkForStateVarsAndNested(sa.getValueExpression(), childComp, paramMappings) + ")\n");
                                        blockNetReceive.append("    " + NRNUtils.checkForStateVarsAndNested(sa.getStateVariable().getName(), childComp, paramMappings) + " = "
                                                + NRNUtils.checkForStateVarsAndNested(sa.getValueExpression(), childComp, paramMappings) + "\n");
                                    }
                                }
                            }
                        }
                        blockNetReceive.append("\n    net_event(t)\n");
                        blockNetReceive.append("    WATCH (" + NRNUtils.checkForStateVarsAndNested(cond, comp, paramMappings) + ") " + conditionFlag + "\n");
                        blockNetReceive.append("\n}\n");

                    }
                }
                else
                {
                    blockNetReceive.append("\nif (flag == 1) { : Setting watch for top level OnCondition...\n");
                    blockNetReceive.append("    WATCH (" + NRNUtils.checkForStateVarsAndNested(cond, comp, paramMappings) + ") " + conditionFlag + "\n");

                    blockNetReceive.append("}\n");
                    blockNetReceive.append("if (flag == " + conditionFlag + ") {\n");
                    if(debug)
                    {
                        blockNetReceive.append("    printf(\"Condition (" + NRNUtils.checkForStateVarsAndNested(cond, comp, paramMappings) + "), " + conditionFlag
                                + ", satisfied at time: %g, v: %g\\n\", t, v)\n");
                    }
                    for(StateAssignment sa : oc.getStateAssignments())
                    {
                        blockNetReceive.append("\n    " + prefix + sa.getStateVariable().getName() + " = " + NRNUtils.checkForStateVarsAndNested(sa.getValueExpression(), comp, paramMappings) + "\n");
                    }
                    blockNetReceive.append("}\n");

                }
                conditionFlag++;
            }
        }
    }

    private static void parseOnEvent(Component comp, StringBuilder blockNetReceive, HashMap<String, HashMap<String, String>> paramMappings) throws ContentError
    {
        // Add appropriate state discontinuities for synaptic events in
        // NET_RECEIVE block. Do this for all child elements as well, in case
        // the spike event is meant to be relayed down from the parent synaptic
        // mechanism to a child plasticityMechanism.
        if(comp.getComponentType().getDynamics() != null)
        {
            for(OnEvent oe : comp.getComponentType().getDynamics().getOnEvents())
            {
                if(oe.getPortName().equals(NeuroMLElements.SYNAPSE_PORT_IN))
                {
                    for(StateAssignment sa : oe.getStateAssignments())
                    {
                        blockNetReceive.append("?state_discontinuity(" + NRNUtils.checkForStateVarsAndNested(sa.getStateVariable().getName(), comp, paramMappings) + ", "
                                + NRNUtils.checkForStateVarsAndNested(sa.getValueExpression(), comp, paramMappings) + ") : From "+comp.id+"\n");
                        blockNetReceive.append("" + NRNUtils.checkForStateVarsAndNested(sa.getStateVariable().getName(), comp, paramMappings) + " = "
                                + NRNUtils.checkForStateVarsAndNested(sa.getValueExpression(), comp, paramMappings) + " : From "+comp.id+"\n");
                    }
                }
            }
        }
        for(Component childComp : comp.getAllChildren())
        {
            if(childComp.getComponentType().isOrExtends(NeuroMLElements.BASE_PLASTICITY_MECHANISM_COMP_TYPE))
            {
                parseOnEvent(childComp, blockNetReceive, paramMappings);
            }
        }
    }

    private static void parseParameters(Component comp, String prefix, String prefixParent, ArrayList<String> rangeVars, ArrayList<String> stateVars, StringBuilder blockNeuron,
            StringBuilder blockParameter, HashMap<String, HashMap<String, String>> paramMappings)
    {

        HashMap<String, String> paramMappingsComp = paramMappings.get(comp.getUniqueID());

        if(paramMappingsComp == null)
        {
            paramMappingsComp = new HashMap<String, String>();
            paramMappings.put(comp.getUniqueID(), paramMappingsComp);
        }

        for(ParamValue pv : comp.getParamValues())
        {
            String mappedName = prefix + pv.getName();
            rangeVars.add(mappedName);
            paramMappingsComp.put(pv.getName(), mappedName);

            String range = "RANGE " + mappedName;
            while(range.length() < NRNUtils.commentOffset)
            {
                range = range + " ";
            }

            blockNeuron.append(range + ": parameter\n");
            float val = NRNUtils.convertToNeuronUnits((float) pv.getDoubleValue(), pv.getDimensionName());
            String valS = val + "";
            if((int) val == val)
            {
                valS = (int) val + "";
            }
            blockParameter.append("\n" + mappedName + " = " + valS + " " + NRNUtils.getNeuronUnit(pv.getDimensionName()));
        }

        for(Exposure exp : comp.getComponentType().getExposures())
        {
            String mappedName = prefix + exp.getName();

            if(!rangeVars.contains(mappedName) && !stateVars.contains(mappedName) && !exp.getName().equals(NRNUtils.NEURON_VOLTAGE))
            {
                rangeVars.add(mappedName);
                paramMappingsComp.put(exp.getName(), mappedName);

                String range = "\nRANGE " + mappedName;
                while(range.length() < NRNUtils.commentOffset)
                {
                    range = range + " ";
                }
                blockNeuron.append(range + " : exposure\n");

                if(comp.getComponentType().isOrExtends(NeuroMLElements.BASE_SYNAPSE_COMP_TYPE) &&
                   exp.getName().equals(NeuroMLElements.POINT_CURR_CURRENT) &&
                   prefix.length()==0)
                {
                    blockNeuron.append("\n\nNONSPECIFIC_CURRENT " + NeuroMLElements.POINT_CURR_CURRENT + " \n");
                }
            }
        }

        for(Requirement req : comp.getComponentType().getRequirements())
        {
            String mappedName = null;
            if(!req.getName().equals("v") && !req.getName().equals("temperature") && !req.getName().equals("caConc"))
            {
                for (String compid: paramMappings.keySet())
                {
                    if (mappedName==null)
                    {
                        for(String key: paramMappings.get(compid).keySet())
                        {
                            String mapped = paramMappings.get(compid).get(key);
                            String rootPrefix = prefix.split("_")[0];
                            String mapPrefix = mapped.split("_")[0];
                            //System.out.println("Checking mapping of "+req.getName()+" in "+comp.getID()+" ("+comp.getComponentType().getName()+", "+prefix+") against "+compid+" mapping: "+key+"->"+mapped);
                            if (key.equals(req.getName()) && rootPrefix.equals(mapPrefix))
                            {
                                mappedName = mapped;
                            }
                        }
                    }

                }
                if (mappedName!=null)
                {
                    paramMappingsComp.put(req.getName(), mappedName);
                }
            }
        }

        for(Component childComp : comp.getAllChildren())
        {
            String prefixNew = getPrefix(childComp, prefix);

            if(!comp.getComponentType().isOrExtends(NeuroMLElements.SPIKE_ARRAY)) { // since this will be hard coded as a more efficient impl, see parseOnStart
                parseParameters(childComp, prefixNew, prefix, rangeVars, stateVars, blockNeuron, blockParameter, paramMappings);
            }
        }

        if(comp.getComponentType().isOrExtends(NeuroMLElements.BASE_CELL_COMP_TYPE))
        {
            blockNeuron.append("\nRANGE " + NRNUtils.V_COPY_PREFIX + NRNUtils.NEURON_VOLTAGE + "                           : copy of v on section\n");
        }

    }

    private static void parseStateVars(Component comp, String prefix, ArrayList<String> rangeVars, ArrayList<String> stateVars, StringBuilder blockNeuron, StringBuilder blockParameter,
            StringBuilder blockAssigned, StringBuilder blockState, HashMap<String, HashMap<String, String>> paramMappings) throws ContentError
    {

        HashMap<String, String> paramMappingsComp = paramMappings.get(comp.getUniqueID());

        if(paramMappingsComp == null)
        {
            paramMappingsComp = new HashMap<String, String>();
            paramMappings.put(comp.getUniqueID(), paramMappingsComp);
        }

        if(comp.getComponentType().hasDynamics())
        {

            for(Regime regime : comp.getComponentType().getDynamics().getRegimes())
            {
                String regimeStateName = NRNUtils.REGIME_PREFIX + regime.name;
                stateVars.add(regimeStateName);
                blockState.append(regimeStateName + " (1)\n");
            }

            for(StateVariable sv : comp.getComponentType().getDynamics().getStateVariables())
            {

                String svName = prefix + NRNUtils.getStateVarName(sv.getName());
                stateVars.add(svName);
                String dim = NRNUtils.getNeuronUnit(sv.getDimension().getName());

                if(!svName.equals(NRNUtils.NEURON_VOLTAGE) && !NRNUtils.getStateVarName(sv.getName()).equals(NRNUtils.getStateVarName(NRNUtils.NEURON_VOLTAGE)))
                {
                    paramMappingsComp.put(NRNUtils.getStateVarName(sv.getName()), svName);
                }

                if(sv.getName().equals(NRNUtils.NEURON_VOLTAGE))
                {
                    blockNeuron.append("\n\nNONSPECIFIC_CURRENT i                    : To ensure v of section follows " + svName + "\n");
                    blockAssigned.append("v (mV)\n");
                    blockAssigned.append("i (mA/cm2)\n\n");

                    blockAssigned.append(NRNUtils.V_COPY_PREFIX + NRNUtils.NEURON_VOLTAGE + " (mV)\n\n");

                    dim = "(nA)";
                }
                String bounds = "";
                if (comp.getComponentType().isOrExtends(NeuroMLElements.KS_STATE_COMP_TYPE)) {
                    bounds = " FROM 0 TO 1";
                }

                blockState.append(svName +bounds + " "+ dim + " "+"\n");
            }
        }

        for(Component childComp : comp.getAllChildren())
        {
            String prefixNew = getPrefix(childComp, prefix);

            if(!comp.getComponentType().isOrExtends(NeuroMLElements.SPIKE_ARRAY)) { // since this will be hard coded as a more efficient impl, see parseOnStart
                parseStateVars(childComp, prefixNew, rangeVars, stateVars, blockNeuron, blockParameter, blockAssigned, blockState, paramMappings);
            }
        }
    }

    private static String getPrefix(Component comp, String prefix)
    {
        String prefixNew = prefix + comp.getID() + "_";
        if(comp.getID() == null)
        {
            if(comp.getName() == null)
            {
                if(comp.getDeclaredType() == null)
                {
                    prefixNew = prefix + comp.getTypeName() + "_";
                }
                else
                {
                    prefixNew = prefix + comp.getDeclaredType() + "_";
                }
            }
            else
            {
                prefixNew = prefix + comp.getName() + "_";
            }
        }
        return prefixNew;
    }

    private static void parseTimeDerivs(Component comp, String prefix, ArrayList<String> locals, StringBuilder blockDerivative, StringBuilder blockBreakpoint, StringBuilder blockAssigned,
            StringBuilder ratesMethod, HashMap<String, HashMap<String, String>> paramMappings, String ionSpecies) throws ContentError
    {

        StringBuilder ratesMethodFinal = new StringBuilder();

        if(comp.getComponentType().hasDynamics())
        {
            HashMap<String, String> rateNameVsRateExpr = new HashMap<String, String>();

            for(TimeDerivative td : comp.getComponentType().getDynamics().getTimeDerivatives())
            {

                String rateName = NRNUtils.RATE_PREFIX + prefix + td.getStateVariable().getName();
                String rateUnits = NRNUtils.getDerivativeUnit(td.getStateVariable().getDimension().getName());

                blockAssigned.append(rateName + " " + rateUnits + "\n");

                // ratesMethod.append(rateName + " = " +
                // NRNUtils.checkForStateVarsAndNested(td.getEvaluable().toString(),
                // comp, paramMappings) + " ? \n");
                String rateExpr = NRNUtils.checkForStateVarsAndNested(td.getValueExpression(), comp, paramMappings);
                rateNameVsRateExpr.put(rateName, rateExpr);

                if(!td.getStateVariable().getName().equals(NRNUtils.NEURON_VOLTAGE))
                {

                    String stateVarToUse = NRNUtils.getStateVarName(td.getStateVariable().getName());

                    String line = prefix + stateVarToUse + "' = " + rateName;

                    if(comp.getComponentType().isOrExtends(NeuroMLElements.CONC_MODEL_COMP_TYPE) &&
                        td.getStateVariable().getName().equals(NeuroMLElements.CONC_MODEL_CONC_STATE_VAR))
                    {
                        line = line + "\n"+ionSpecies+"i = " + td.getStateVariable().getName();
                    }

                    if(!blockDerivative.toString().contains(line))
                    {
                        blockDerivative.append(line + " \n");
                    }
                }
                else
                {
                    ratesMethodFinal.append(prefix + NRNUtils.getStateVarName(td.getStateVariable().getName()) + " = -1 * " + rateName + "\n");
                }

            }

            for(Regime regime : comp.getComponentType().getDynamics().getRegimes())
            {

                if(regime.getTimeDerivatives().isEmpty())
                {
                    // need to hold voltage fixed

                    for(OnEntry oe : regime.getOnEntrys())
                    {
                        for(StateAssignment sa : oe.getStateAssignments())
                        {

                            if(sa.getStateVariable().getName().equals(NRNUtils.NEURON_VOLTAGE))
                            {
                                String rateName = NRNUtils.RATE_PREFIX + prefix + sa.getStateVariable().getName();

                                if(!rateNameVsRateExpr.containsKey(rateName))
                                {
                                    rateNameVsRateExpr.put(rateName, "0");
                                }

                                String rateExprPart = rateNameVsRateExpr.get(rateName);

                                String rateUnits = NRNUtils.getDerivativeUnit(sa.getStateVariable().getDimension().getName());
                                if(blockAssigned.indexOf("\n" + rateName + " " + rateUnits + "\n") < 0)
                                {
                                    blockAssigned.append("\n" + rateName + " " + rateUnits + "\n");
                                }
                                rateExprPart = rateExprPart + " + " + NRNUtils.REGIME_PREFIX + regime.getName() + " * (10000000 * (" + sa.getValueExpression() + " - " + NRNUtils.NEURON_VOLTAGE + "))";

                                rateNameVsRateExpr.put(rateName, rateExprPart);
                            }
                        }
                    }
                }
                for(TimeDerivative td : regime.getTimeDerivatives())
                {
                    String rateName = NRNUtils.RATE_PREFIX + prefix + td.getStateVariable().getName();
                    String rateUnits = NRNUtils.getDerivativeUnit(td.getStateVariable().getDimension().getName());
                    if(!rateNameVsRateExpr.containsKey(rateName))
                    {
                        rateNameVsRateExpr.put(rateName, "0");
                    }

                    String rateExprPart = rateNameVsRateExpr.get(rateName);
                    if(blockAssigned.indexOf("\n" + rateName + " " + rateUnits + "\n") < 0)
                    {
                        blockAssigned.append("\n" + rateName + " " + rateUnits + "\n");
                    }
                    rateExprPart = rateExprPart + " + " + NRNUtils.REGIME_PREFIX + regime.getName() + " * (" + NRNUtils.checkForStateVarsAndNested(td.getValueExpression(), comp, paramMappings) + ")";

                    rateNameVsRateExpr.put(rateName, rateExprPart);

                    if(!td.getStateVariable().getName().equals(NRNUtils.NEURON_VOLTAGE))
                    {
                        String line = prefix + NRNUtils.getStateVarName(td.getStateVariable().getName()) + "' = " + rateName;

                        if(!blockDerivative.toString().contains(line))
                        {
                            blockDerivative.append(line + " \n");
                        }
                    }
                    else
                    {
                        ratesMethodFinal.append(prefix + NRNUtils.getStateVarName(td.getStateVariable().getName()) + " = -1 * " + rateName + "\n"); // //
                    }
                }
            }

            for(String rateName : rateNameVsRateExpr.keySet())
            {
                String rateExpr = rateNameVsRateExpr.get(rateName);

                ratesMethod.append(rateName + " = " + rateExpr + " ? Note units of all quantities used here need to be consistent!\n");

            }

            ratesMethod.append("\n" + ratesMethodFinal + " \n");

        }

        for(Component childComp : comp.getAllChildren())
        {

            String prefixNew = getPrefix(childComp, prefix);

            if(!comp.getComponentType().isOrExtends(NeuroMLElements.SPIKE_ARRAY)) { // since this will be hard coded as a more efficient impl, see parseOnStart
                parseTimeDerivs(childComp, prefixNew, locals, blockDerivative, blockBreakpoint, blockAssigned, ratesMethod, paramMappings, ionSpecies);
            }
        }
    }

    private static void parseDerivedVars(Component comp, String prefix, ArrayList<String> rangeVars, StringBuilder ratesMethod, StringBuilder blockNeuron, StringBuilder blockParameter,
            StringBuilder blockAssigned, StringBuilder blockBreakpoint, HashMap<String, HashMap<String, String>> paramMappings) throws ContentError
    {

        HashMap<String, String> paramMappingsComp = paramMappings.get(comp.getUniqueID());
        if(paramMappingsComp == null)
        {
            paramMappingsComp = new HashMap<String, String>();
            paramMappings.put(comp.getUniqueID(), paramMappingsComp);
        }

        for(Component childComp : comp.getAllChildren())
        {
            String prefixNew = getPrefix(childComp, prefix);
            parseDerivedVars(childComp, prefixNew, rangeVars, ratesMethod, blockNeuron, blockParameter, blockAssigned, blockBreakpoint, paramMappings);
        }

        ArrayList<String> instanceRequirements = new ArrayList<String>();
        for(InstanceRequirement ir : comp.getComponentType().instanceRequirements)
        {
            instanceRequirements.add(ir.getName());
        }
        // ratesMethod.append("? Looking at"+comp+"\n");
        if(comp.getComponentType().hasDynamics())
        {

            StringBuilder blockForEqns = ratesMethod;
            if(comp.getComponentType().isOrExtends(NeuroMLElements.BASE_ION_CHANNEL_COMP_TYPE))
            {
                blockForEqns = blockBreakpoint;
            }

            for(DerivedVariable dv : comp.getComponentType().getDynamics().getDerivedVariables())
            {

                StringBuilder block = new StringBuilder();
                String mappedName = prefix + dv.getName();

                if(dv.getPath() != null && instanceRequirements.contains(dv.getPath().split("/")[0]))
                {
                    blockNeuron.append("POINTER " + dv.getName() + ": derived variable as pointer...\n");
                }
                else if(!rangeVars.contains(mappedName))
                {
                    rangeVars.add(mappedName);

                    String range = "RANGE " + mappedName;
                    while(range.length() < NRNUtils.commentOffset)
                    {
                        range = range + " ";
                    }

                    blockNeuron.append(range + ": derived variable\n");
                    paramMappingsComp.put(dv.getName(), mappedName);
                }

                String assig = "\n" + prefix + dv.getName() + " " + NRNUtils.getNeuronUnit(dv.dimension);
                while(assig.length() < NRNUtils.commentOffset)
                {
                    assig = assig + " ";
                }

                blockAssigned.append(assig + ": derived variable\n");

                if(dv.getValueExpression() != null)
                {

                    String rate = NRNUtils.checkForStateVarsAndNested(dv.getValueExpression(), comp, paramMappings);

                    String synFactor = "";
                    if(comp.getComponentType().isOrExtends(NeuroMLElements.BASE_SYNAPSE_COMP_TYPE) &&
                       dv.getName().equals(NeuroMLElements.POINT_CURR_CURRENT)
                       && prefix.length()==0)  /* to ensure it's not a child synapse on a point process/spiker */
                    {
                        // since synapse currents differ in sign from NEURON
                        synFactor = "-1 * ";
                    }

                    block.append(prefix + dv.getName() + " = " + synFactor + rate + " ? evaluable\n");

                }
                else
                {
                    String firstChild = dv.getPath().substring(0, dv.getPath().indexOf("/"));
                    if(firstChild.contains("["))
                    {
                        firstChild = firstChild.substring(0, firstChild.indexOf("["));
                    }

                    Component child = null;

                    try
                    {
                        child = comp.getChild(firstChild);
                    }
                    catch(ContentError ce)
                    {
                        // E.info("No child of " + firstChild);// do nothing...
                    }

                    if(child == null)
                    {
                        ArrayList<Component> children = comp.getChildrenAL(firstChild);
                        if(children.size() > 0)
                        {
                            child = children.get(0);
                        }
                    }

                    String info = "? DerivedVariable is based on path: " + dv.getPath() + ", on: " + comp + ", from " + firstChild + "; " + child + "\n";

                    block.append(info);

                    if(child == null && !dv.getPath().contains("synapse"))
                    {

                        if(instanceRequirements.contains(dv.getPath().split("/")[0]))
                        {
                            block.append("? Derived variable: " + dv.getName() + "; its value will be set by a pointer...\n\n");
                        }
                        else
                        {

                            String alt = "???";
                            if(dv.getReduce().equals("multiply"))
                            {
                                alt = "1";
                            }
                            else if(dv.getReduce().equals("add"))
                            {
                                alt = "0";
                            }
                            block.append("? Path not present in component, using factor: " + alt + "\n\n");
                            String rate = NRNUtils.checkForStateVarsAndNested(alt, comp, paramMappings);
                            block.append(prefix + dv.getName() + " = " + rate + " \n\n");
                        }
                    }
                    else
                    {
                        String localVar = dv.getPath().replaceAll("/", "_");
                        String globalVar = prefix + dv.getPath().replaceAll("/", "_");

                        //TODO: replacewith more thorough check...
                        if (globalVar.startsWith("synapse_"))
                        {
                            globalVar = globalVar.replaceFirst("synapse_", comp.getChild("synapse").getID()+"_");
                        }

                        String eqn = globalVar;
                        String comment = "";
                        if(globalVar.contains("[*]") && globalVar.contains("syn"))
                        {
                            eqn = "0 ? Was: " + localVar + " but insertion of currents from external attachments not yet supported";
                        }
                        else if(localVar.contains("[*]"))
                        {
                            String children = localVar.substring(0, localVar.indexOf("[*]"));
                            String path = localVar.substring(localVar.indexOf("[*]_") + 4);
                            String reduce = dv.getReduce();
                            String op = null;
                            if(reduce.equals("multiply"))
                            {
                                op = " * ";
                            }
                            if(reduce.equals("add"))
                            {
                                op = " + ";
                            }
                            eqn = "";

                            for(Component childComp : comp.getChildrenAL(children))
                            {
                                // var = var + childComp.getID()+" --";
                                if(eqn.length() > 0)
                                {
                                    eqn = eqn + op;
                                }
                                eqn = eqn + getPrefix(childComp, prefix) + path;
                            }
                            comment = "? " + reduce + " applied to all instances of " + path + " in: <" + children + "> (" + comp.getChildrenAL(children) + ")" /*+ " c2 (" + comp.getAllChildren()
                                    */+ ")";
                        }
                        if (comment.length()>0) {
                            block.append(NRNUtils.checkCommentLineLength(comment)+"\n");
                        }
                        block.append(prefix + dv.getName() + " = " + eqn + " ? path based\n\n");
                    }
                }
                // blockForEqns.insert(0, block);
                blockForEqns.append(block);
            }

            for(ConditionalDerivedVariable cdv : comp.getComponentType().getDynamics().getConditionalDerivedVariables())
            {

                StringBuilder block = new StringBuilder();

                String mappedName = prefix + cdv.getName();
                if(!rangeVars.contains(mappedName))
                {
                    rangeVars.add(mappedName);

                    String range = "\nRANGE " + mappedName;
                    while(range.length() < NRNUtils.commentOffset)
                    {
                        range = range + " ";
                    }

                    blockNeuron.append(range + ": conditional derived var\n");
                    paramMappingsComp.put(cdv.getName(), mappedName);
                }

                String assig = "\n" + prefix + cdv.getName() + " " + NRNUtils.getNeuronUnit(cdv.dimension);
                while(assig.length() < NRNUtils.commentOffset)
                {
                    assig = assig + " ";
                }

                blockAssigned.append(assig + ": conditional derived var...\n");

                for(Case c : cdv.cases)
                {

                    String rate = NRNUtils.checkForStateVarsAndNested(c.getValueExpression(), comp, paramMappings);

                    String cond = "\n} else ";
                    if(c.condition != null)
                    {
                        String cond_ = NRNUtils.checkForStateVarsAndNested(c.condition, comp, paramMappings);
                        cond = "if (" + cond_ + ") ";
                        if(block.length() != 0)
                        {
                            cond = "\n} else " + cond;
                        }
                    }

                    block.append(cond + " { \n    " + prefix + cdv.getName() + " = " + rate + " ? evaluable cdv");
                }

                blockForEqns.append(block + "\n}\n\n");

            }
        }
    }

    public static void writeModBlock(StringBuilder main, String blockName, String contents)
    {
        contents = contents.replaceAll("\n", "\n    ");
        if(!contents.endsWith("\n"))
        {
            contents = contents + "\n";
        }
        main.append(blockName + " {\n    " + contents + "}\n\n");
    }

    public class CompInfo
    {

        StringBuilder params = new StringBuilder();
        StringBuilder eqns = new StringBuilder();
        StringBuilder initInfo = new StringBuilder();
    }

    public static void main(String[] args) throws Exception
    {

        MinimalMessageHandler.setVeryMinimal(true);
        E.setDebug(false);

        ArrayList<File> lemsFiles = new ArrayList<File>();
        //lemsFiles.add(new File("../neuroConstruct/osb/invertebrate/celegans/CElegansNeuroML/CElegans/pythonScripts/c302/examples/LEMS_c302_C1_Oscillator.xml"));

        //lemsFiles.add(new File("../neuroConstruct/osb/cerebellum/cerebellar_golgi_cell/SolinasEtAl-GolgiCell/NeuroML2/LEMS_KAHP_Test.xml"));
        //lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex12_Net2.xml"));
        //lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex16_Inputs.xml"));
        //lemsFiles.add(new File("../neuroConstruct/osb/cerebellum/networks/VervaekeEtAl-GolgiCellNetwork/NeuroML2/LEMS_Pacemaking.xml"));
        //lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex9_FN.xml"));
        //lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex5_DetCell.xml"));
        //lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex20a_AnalogSynapsesHH.xml"));
        //lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex20_AnalogSynapses.xml"));
        
        
        //lemsFiles.add(new File("../neuroConstruct/osb/invertebrate/celegans/CElegansNeuroML/CElegans/pythonScripts/c302/examples/LEMS_c302_C1_Muscles.xml"));
        lemsFiles.add(new File("../neuroConstruct/osb/invertebrate/celegans/CElegansNeuroML/CElegans/pythonScripts/c302/examples/LEMS_c302_C1_Syns.xml"));
        
        /*lemsFiles.add(new File("../neuroConstruct/osb/cerebral_cortex/networks/ACnet2/neuroConstruct/generatedNeuroML2/LEMS_StimuliTest.xml"));
        lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex5_DetCell.xml"));
        lemsFiles.add(new File("../neuroConstruct/osb/cerebellum/cerebellar_granule_cell/GranuleCell/neuroConstruct/generatedNeuroML2/LEMS_GranuleCell.xml"));
        
        lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex23_Spiketimes.xml"));
        lemsFiles.add(new File("../neuroConstruct/osb/cerebellum/networks/GranCellLayer/neuroConstruct/generatedNeuroML2/LEMS_GranCellLayer.xml"));
        lemsFiles.add(new File("../neuroConstruct/osb/cerebellum/cerebellar_golgi_cell/SolinasEtAl-GolgiCell/NeuroML2/LEMS_Soma_Test_HELPER.xml"));

        lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex19_GapJunctions.xml"));
        lemsFiles.add(new File("../neuroConstruct/osb/cerebral_cortex/networks/Thalamocortical/neuroConstruct/generatedNeuroML2/LEMS_Thalamocortical.xml"));

        lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex16_Inputs.xml"));

        lemsFiles.add(new File("../neuroConstruct/osb/showcase/AllenInstituteNeuroML/CellTypesDatabase/models/NeuroML2/LEMS_SomaTest.xml"));
        lemsFiles.add(new File("../neuroConstruct/osb/cerebral_cortex/networks/ACnet2/neuroConstruct/generatedNeuroML2/LEMS_MediumNet.xml"));
        lemsFiles.add(new File("../neuroConstruct/osb/hippocampus/CA1_pyramidal_neuron/CA1PyramidalCell/neuroConstruct/generatedNeuroML2/LEMS_CA1PyramidalCell.xml"));

        /*
        lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex21_CurrentBasedSynapses.xml"));

        //lemsFiles.add(new File("../neuroConstruct/osb/showcase/AllenInstituteNeuroML/CellTypesDatabase/models/NeuroML2/LEMS_SomaTest.xml"));

        lemsFiles.add(new File("../neuroConstruct/osb/cerebral_cortex/networks/IzhikevichModel/NeuroML2/LEMS_SmallNetwork.xml"));
        lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex19_GapJunctions.xml"));
        lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex20a_AnalogSynapsesHH.xml"));

        lemsFiles.add(new File("../neuroConstruct/osb/cerebral_cortex/neocortical_pyramidal_neuron/L5bPyrCellHayEtAl2011/neuroConstruct/generatedNeuroML2/LEMS_L5bPyrCellHayEtAl2011_LowDt.xml"));

        lemsFiles.add(new File("../neuroConstruct/osb/invertebrate/celegans/muscle_model/NeuroML2/LEMS_NeuronMuscle.xml"));
        lemsFiles.add(new File("../neuroConstruct/osb/cerebral_cortex/multiple/PospischilEtAl2008/NeuroML2/channels/IL/LEMS_IL.nonernst.xml"));

        //lemsFiles.add(new File("../neuroConstruct/osb/cerebellum/networks/Cerebellum3DDemo/neuroConstruct/generatedNeuroML2/LEMS_Cerebellum3DDemo.xml"));


        //lemsFiles.add(new File("../git/neuroml_use_case/LEMS_sim.xml"));



        lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex0_IaF.xml"));
        lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex5_DetCell.xml"));

        //lemsFiles.add(new File("../git/SpinyStellateNMDA/NeuroML2/LEMS_TestMultiSim.xml"));

        //lemsFiles.add(new File("../NeuroML2/LEMSexamples/InputTest.xml"));

        lemsFiles.add(new File("../git/L5bPyrCellHayEtAl2011/neuroConstruct/generatedNeuroML2/LEMS_TestL5PC.xml"));
        lemsFiles.add(new File("../neuroConstruct/osb/hippocampus/networks/nc_superdeep/neuroConstruct/generatedNeuroML2/LEMS_TestBasket.xml"));
        lemsFiles.add(new File("../neuroConstruct/osb/cerebral_cortex/neocortical_pyramidal_neuron/MainenEtAl_PyramidalCell/neuroConstruct/generatedNeuroML2/LEMS_MainenEtAl_PyramidalCell.xml"));


        //lemsFiles.add(new File("../neuroConstruct/osb/cerebral_cortex/neocortical_pyramidal_neuron/L5bPyrCellHayEtAl2011/neuroConstruct/generatedNeuroML2/LEMS_TestL5PC.xml"));
        lemsFiles.add(new File("../neuroConstruct/osb/cerebral_cortex/networks/ACnet2/neuroConstruct/generatedNeuroML2/LEMS_ACnet2.xml"));
        lemsFiles.add(new File("src/test/resources/BIOMD0000000185_LEMS.xml"));

        //lemsFiles.add(new File("../neuroConstruct/osb/hippocampus/networks/nc_superdeep/neuroConstruct/generatedNeuroML2/LEMS_nc_superdeep.xml"));

        //lemsFiles.add(new File("../neuroConstruct/osb/cerebral_cortex/neocortical_pyramidal_neuron/L5bPyrCellHayEtAl2011/neuroConstruct/generatedNeuroML2/LEMS_L5bPyrCellHayEtAl2011.xml"));

        lemsFiles.add(new File("../neuroConstruct/osb/invertebrate/lobster/PyloricNetwork/neuroConstruct/generatedNeuroML2/LEMS_PyloricPacemakerNetwork.xml"));

        lemsFiles.add(new File("../git/GPUShowcase/NeuroML2/LEMS_simplenet.xml"));
        //lemsFiles.add(new File("../git/BlueBrainProjectShowcase/ChannelTest/LEMS_TestVClamp.xml"));


        //lemsFiles.add(new File("../neuroConstruct/osb/invertebrate/celegans/CElegansNeuroML/CElegans/pythonScripts/c302/LEMS_c302_A_Pharyngeal.xml"));
        //lemsFiles.add(new File("../neuroConstruct/osb/invertebrate/celegans/CElegansNeuroML/CElegans/pythonScripts/c302/LEMS_Single.xml"));

        //lemsFiles.add(new File("../neuroConstruct/osb/invertebrate/celegans/CElegansNeuroML/CElegans/pythonScripts/c302/LEMS_c302_A_Pharyngeal.xml"));
        lemsFiles.add(new File("../neuroConstruct/osb/invertebrate/celegans/CElegansNeuroML/CElegans/pythonScripts/c302/examples/LEMS_c302_B_Syns.xml"));

        lemsFiles.add(new File("../neuroConstruct/osb/invertebrate/celegans/CElegansNeuroML/CElegans/pythonScripts/c302/examples/LEMS_c302_B_Social.xml"));
                /* */
        String testScript = "set -e\n";

        NeuronWriter nw;
        for(File lemsFile : lemsFiles)
        {
            Lems lems = Utils.readLemsNeuroMLFile(lemsFile.getAbsoluteFile()).getLems();
            System.out.println("fff");
            nw = new NeuronWriter(lems, lemsFile.getParentFile(), lemsFile.getName().replaceAll(".xml", "_nrn.py"));
            System.out.println("fff");

            List<File> ff = nw.generateAndRun(false, false);
            for(File f : ff)
            {
                System.out.println("Generated: " + f.getAbsolutePath());
            }
            testScript += "\necho Testing " + lemsFile.getAbsolutePath() + "\n";
            testScript += "echo\n";
            testScript += "echo\n";
            testScript += "cd " + lemsFile.getParentFile().getCanonicalPath() + "\n";
            testScript += "nrnivmodl\n";
            String nrn = "nrngui";
            testScript += nrn + " -python " + lemsFile.getName().replaceAll(".xml", "_nrn.py") + " \n";
            testScript += "\n";

        }
        File t = new File("test.sh");
        FileUtil.writeStringToFile(testScript, t);
        System.out.println("Written file to test conversions to: " + t.getAbsolutePath());

        /*
         * String[] qs = {"1 mV", "1.234mV", "1.2e-4V", "1.23e-5A", "1.23e4A", "1.45E-8 m", "1.23E-8m2", "60", "6000", "123000"}; for (String s : qs) { DimensionalQuantity dq =
         * QuantityReader.parseValue(s, nw.lems.getUnits()); System.out.println("String " + s + " converts to: " + nw.NRNUtils.convertToNeuronUnits(s) + " (units: " +
         * NRNUtils.getNeuronUnit(dq.getDimension().getName()) + ")"); }
         */
    }

    @Override
    public List<File> convert() throws GenerationException
    {
        try
        {
            generateMainScriptAndMods();
        }
        catch(LEMSException e)
        {
            throw new GenerationException("Problem generating "+format+" files", e);
        }
        catch(NeuroMLException e)
        {
            throw new GenerationException("Problem generating "+format+" files", e);
        }

        return outputFiles;
    }

}

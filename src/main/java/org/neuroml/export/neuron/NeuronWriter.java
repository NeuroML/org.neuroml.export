package org.neuroml.export.neuron;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.lemsml.export.dlems.DLemsWriter;
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

import static org.neuroml.export.neuron.ProcessManager.findNeuronHome;
import org.neuroml.model.Cell;
import org.neuroml.model.ChannelDensity;
import org.neuroml.model.ChannelDensityGHK;
import org.neuroml.model.ChannelDensityNernst;
import org.neuroml.model.ChannelDensityNonUniform;
import org.neuroml.model.Species;
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

    public enum ChannelConductanceOption
    {
        FIXED_REVERSAL_POTENTIAL, USE_NERNST, USE_GHK;
        float erev;
    };

    public NeuronWriter(Lems lems) throws ModelFeatureSupportException, LEMSException, NeuroMLException
    {
        super(lems, Format.NEURON);
    }

    public NeuronWriter(Lems lems, File outputFolder, String outputFileName) throws ModelFeatureSupportException, NeuroMLException, LEMSException
    {
        super(lems, Format.NEURON, outputFolder, outputFileName);
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
        sli.addSupportInfo(format, ModelFeature.NETWORK_WITH_PROJECTIONS_MODEL, SupportLevelInfo.Level.LOW);
        sli.addSupportInfo(format, ModelFeature.MULTICOMPARTMENTAL_CELL_MODEL, SupportLevelInfo.Level.LOW);
        sli.addSupportInfo(format, ModelFeature.HH_CHANNEL_MODEL, SupportLevelInfo.Level.MEDIUM);
        sli.addSupportInfo(format, ModelFeature.KS_CHANNEL_MODEL, SupportLevelInfo.Level.NONE);
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

            String targetId = simCpt.getStringValue("target");

            Component targetComp = lems.getComponent(targetId);
            String info = "Adding simulation " + simCpt + " of network/component: " + targetComp.summary();

            E.info(info);

            addComment(main, info);

            ArrayList<Component> popsOrComponents = targetComp.getChildrenAL("populations");

            E.info("popsOrComponents: " + popsOrComponents);

            HashMap<String, Integer> compMechsCreated = new HashMap<String, Integer>();
            HashMap<String, String> compMechNamesHoc = new HashMap<String, String>();

            HashMap<String, Cell> compIdsVsCells = new HashMap<String, Cell>();
            HashMap<String, String> popIdsVsCellIds = new HashMap<String, String>();
            HashMap<String, Component> popIdsVsComps = new HashMap<String, Component>();

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

                if(popsOrComponent.getComponentType().getName().equals(NeuroMLElements.POPULATION))
                {
                    compReference = popsOrComponent.getStringValue(NeuroMLElements.POPULATION_COMPONENT);
                    number = Integer.parseInt(popsOrComponent.getStringValue(NeuroMLElements.POPULATION_SIZE));
                    popComp = lems.getComponent(compReference);
                    popName = popsOrComponent.getID();
                    popIdsVsCellIds.put(popName, compReference);
                    popIdsVsComps.put(popName, popComp);
                }
                else if(popsOrComponent.getComponentType().getName().equals(NeuroMLElements.POPULATION_LIST))
                {
                    compReference = popsOrComponent.getStringValue(NeuroMLElements.POPULATION_COMPONENT);
                    popComp = lems.getComponent(compReference);
                    number = 0;
                    for(Component comp : popsOrComponent.getAllChildren())
                    {
                        if(comp.getComponentType().getName().equals(NeuroMLElements.INSTANCE))
                        {
                            number++;
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

                String compTypeName = popComp.getComponentType().getName();

                main.append("print(\"Population " + popName + " contains " + number + " instance(s) of component: " + popComp.getID() + " of type: " + popComp.getComponentType().getName() + "\")\n\n");

                if(popComp.getComponentType().isOrExtends(NeuroMLElements.CELL_COMP_TYPE))
                {

                    Cell cell = Utils.getCellFromComponent(popComp);
                    NamingHelper nh = new NamingHelper(cell);
                    compIdsVsCells.put(popComp.getID(), cell);
                    String cellString = generateCellFile(cell);
                    String cellName = popComp.getID();

                    String fileName = cellName + ".hoc";
                    File cellFile = new File(getOutputFolder(), fileName);
                    E.info("Writing to: " + cellFile);

                    main.append("h.load_file(\"" + fileName + "\")\n");

                    main.append("a_" + popName + " = []\n");

                    main.append("h(\"n_" + popName + " = " + number + "\")\n");

                    main.append("h(\"objectvar a_" + popName + "[n_" + popName + "]\")\n");

                    main.append("for i in range(int(h.n_" + popName + ")):\n");
                    // main.append("    cell = h."+cellName+"()\n");
                    main.append("    h(\"a_" + popName + "[%i] = new " + cellName + "()\"%i)\n");
                    // main.append("    cell."+getNrnSectionName(cell.getMorphology().getSegment().get(0))+".push()\n");
                    main.append("    h(\"access a_" + popName + "[%i]." + nh.getNrnSectionName(cell.getMorphology().getSegment().get(0)) + "\"%i)\n\n");

                    main.append(String.format("h(\"proc initialiseV_%s() { for i = 0, n_%s-1 { a_%s[i].set_initial_v() } }\")\n", popName, popName, popName));
                    main.append(String.format("h(\"objref fih_%s\")\n", popName));
                    main.append(String.format("h(\'{fih_%s = new FInitializeHandler(0, \"initialiseV_%s()\")}\')\n\n", popName, popName));

                    main.append(String.format("h(\"proc initialiseIons_%s() { for i = 0, n_%s-1 { a_%s[i].set_initial_ion_properties() } }\")\n", popName, popName, popName));
                    main.append(String.format("h(\"objref fih_ion_%s\")\n", popName));
                    main.append(String.format("h(\'{fih_ion_%s = new FInitializeHandler(1, \"initialiseIons_%s()\")}\')\n\n", popName, popName));

                    try
                    {
                        FileUtil.writeStringToFile(cellString, cellFile);
                        this.outputFiles.add(cellFile);
                    }
                    catch(IOException ex)
                    {
                        throw new ContentError("Error writing to file: " + cellFile.getAbsolutePath(), ex);
                    }

                    for(ChannelDensity cd : cell.getBiophysicalProperties().getMembraneProperties().getChannelDensity())
                    {
                        String ionChannel = cd.getIonChannel();
                        ChannelConductanceOption option = ChannelConductanceOption.FIXED_REVERSAL_POTENTIAL;
                        option.erev = NRNUtils.convertToNeuronUnits(Utils.getMagnitudeInSI(cd.getErev()), "voltage");
                        writeModFile(ionChannel, option);
                    }

                    for(ChannelDensityNonUniform cd : cell.getBiophysicalProperties().getMembraneProperties().getChannelDensityNonUniform())
                    {
                        String ionChannel = cd.getIonChannel();
                        ChannelConductanceOption option = ChannelConductanceOption.FIXED_REVERSAL_POTENTIAL;
                        option.erev = NRNUtils.convertToNeuronUnits(Utils.getMagnitudeInSI(cd.getErev()), "voltage");
                        writeModFile(ionChannel, option);
                    }

                    for(ChannelDensityNernst cdn : cell.getBiophysicalProperties().getMembraneProperties().getChannelDensityNernst())
                    {
                        String ionChannel = cdn.getIonChannel();
                        ChannelConductanceOption option = ChannelConductanceOption.USE_NERNST;
                        writeModFile(ionChannel, option);
                    }

                    for(ChannelDensityGHK cdg : cell.getBiophysicalProperties().getMembraneProperties().getChannelDensityGHK())
                    {
                        String ionChannel = cdg.getIonChannel();
                        ChannelConductanceOption option = ChannelConductanceOption.USE_GHK;
                        writeModFile(ionChannel, option);
                    }

                    for(Species sp : cell.getBiophysicalProperties().getIntracellularProperties().getSpecies())
                    {
                        String concModel = sp.getConcentrationModel();
                        writeModFile(concModel);
                    }

                }
                else
                {
                    String mod = generateModFile(popComp);
                    dumpModToFile(popComp, mod);

                    main.append("h(\" {n_" + popName + " = " + number + "} \")\n");

                    String mechName = NRNUtils.getMechanismName(compTypeName, popName);

                    addComment(main, "Population " + popName + " contains instances of " + popComp + "\n" + "whose dynamics will be implemented as a mechanism (" + compTypeName + ") in a mod file");

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
                    main.append("    h(\" " + instName.replaceAll("\\[i\\]", "[%i]") + "  { " + mechName + "[%i] = new " + compTypeName + "(0.5) } \"%(i,i))\n\n");

                    if(!compMechsCreated.containsKey(compTypeName))
                    {
                        compMechsCreated.put(compTypeName, 0);
                    }

                    compMechsCreated.put(compTypeName, compMechsCreated.get(compTypeName) + 1);

                    String hocMechName = NRNUtils.getMechanismName(compTypeName, popName) + "[i]";

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

                    if(comp.getComponentType().getName().equals(NeuroMLElements.CONNECTION))
                    {
                        number++;
                    }
                }

                addComment(main, String.format("Adding projection: %s, from %s to %s with synapse %s, %d connection(s)", id, prePop, postPop, synapse, number));

                Component synapseComp = lems.getComponent(synapse);

                String mod = generateModFile(synapseComp);
                dumpModToFile(synapseComp, mod);

                String synObjName = String.format("syn_%s_%s", id, synapse);

                main.append(String.format("h(\"objectvar %s[%d]\")\n\n", synObjName, number));

                int index = 0;
                for(Component conn : projection.getAllChildren())
                {

                    if(conn.getComponentType().getName().equals(NeuroMLElements.CONNECTION))
                    {
                        int preCellId = Utils.parseCellRefStringForCellNum(conn.getStringValue("preCellId"));
                        int postCellId = Utils.parseCellRefStringForCellNum(conn.getStringValue("postCellId"));

                        int preSegmentId = conn.hasTextParam("preSegmentId") ? Integer.parseInt(conn.getStringValue("preSegmentId")) : 0;
                        int postSegmentId = conn.hasTextParam("postSegmentId") ? Integer.parseInt(conn.getStringValue("postSegmentId")) : 0;

                        float preFractionAlong = conn.hasTextParam("preFractionAlong") ? Float.parseFloat(conn.getStringValue("preFractionAlong")) : 0.5f;
                        float postFractionAlong = conn.hasTextParam("postFractionAlong") ? Float.parseFloat(conn.getStringValue("postFractionAlong")) : 0.5f;

                        if(preSegmentId != 0 || postSegmentId != 0)
                        {
                            throw new GenerationException("Connections on locations other than segment id=0 not yet supported...");
                        }

                        String preSecName;

                        if(preCell != null)
                        {
                            NamingHelper nhPre = new NamingHelper(preCell);
                            preSecName = String.format("a_%s[%s].%s", prePop, preCellId, nhPre.getNrnSectionName(preCell.getMorphology().getSegment().get(0)));
                        }
                        else
                        {
                            preSecName = prePop + "[" + preCellId + "]";
                        }

                        String postSecName;
                        if(postCell != null)
                        {
                            NamingHelper nhPost = new NamingHelper(postCell);
                            postSecName = String.format("a_%s[%s].%s", postPop, postCellId, nhPost.getNrnSectionName(postCell.getMorphology().getSegment().get(0)));
                        }
                        else
                        {
                            postSecName = postPop + "[" + postCellId + "]";
                        }

                        main.append(String.format("h(\"%s %s[%d] = new %s(%f)\")\n", postSecName, synObjName, index, synapse, postFractionAlong));
                        
                        String sourceVarToListenFor = "&v("+ preFractionAlong+")";
                        

                        if(preCell != null)
                        {
                            main.append(String.format("h(\"%s a_%s[%d].synlist.append(new NetCon(%s, %s[%d], 0, 0, 1))\")\n\n", preSecName, postPop, postCellId, sourceVarToListenFor, synObjName,
                                    index));
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
                                String hocMechName = NRNUtils.getMechanismName(preComp.getComponentType().getName(), prePop) + "["+preCellId+"]";
                                sourceVarToListenFor = hocMechName;
                            }
                            main.append(String.format("h(\"objectvar nc_%s_%d\")\n", synObjName, index));
                            main.append(String.format("h(\"%s nc_%s_%d = new NetCon(%s, %s[%d], %f, 0, 1)\")  \n\n", preSecName, synObjName, index, sourceVarToListenFor, synObjName, index, threshold));
                        }
                        index++;
                    }
                }

            }

            /* <synapticConnection> and <synapticConnectionWD> elements */

            ArrayList<Component> synapticConnections = targetComp.getChildrenAL("synapticConnections");
            synapticConnections.addAll(targetComp.getChildrenAL("synapticConnectionWDs"));

            /* First, group connections by synapse type */
            HashMap<String, ArrayList<Component>> connectionsByType = new HashMap<String, ArrayList<Component>>();
            for(Component connection : synapticConnections)
            {
                String synType = connection.getStringValue("synapse");
                if(connectionsByType.containsKey(synType) == false)
                {
                    connectionsByType.put(synType, new ArrayList<Component>());
                }
                connectionsByType.get(synType).add(connection);
            }

            for(Map.Entry<String, ArrayList<Component>> entry : connectionsByType.entrySet())
            {

                String synType = entry.getKey();
                ArrayList<Component> connections = entry.getValue();

                Component synapseComp = lems.getComponent(synType);

                /* Generate a .mod file for the synapse type (if one doesn't already exist) */
                String mod = generateModFile(synapseComp);
                dumpModToFile(synapseComp, mod);

                /* Array of synapses of this type */
                String info0 = String.format("Adding synapse %s used in %s connections", synType, connections.size());
                addComment(main, info0);

                String synArrayName = String.format("synapses_%s", synType);
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

                    main.append(String.format("h(\"%s %s[%d] = new %s(%f)\")\n", toSecName, synArrayName, i, synType, 0.5));

                    float delay = connection.hasParam("delay") ? (float) connection.getParamValue("delay").getDoubleValue() * 1000 : 0.0f;
                    float weight = connection.hasParam("weight") ? (float) connection.getParamValue("weight").getDoubleValue() : 1.0f;

                    if(toCell != null)
                    {
                        main.append(String.format("h(\"%s a_%s[%d].synlist.append(new NetCon(&v(%f), %s[%d], 0, %f, %f))\")\n\n", fromSecName, toPop, toCellId, 0.5, synArrayName, i, delay, weight));
                    }
                    else
                    {
                        Component fromComp = popIdsVsComps.get(fromPop);
                        float threshold = 0;
                        if(fromComp.getComponentType().isOrExtends(NeuroMLElements.BASE_IAF_CAP_CELL) || fromComp.getComponentType().isOrExtends(NeuroMLElements.BASE_IAF_CELL))
                        {
                            threshold = NRNUtils.convertToNeuronUnits(fromComp.getStringValue("thresh"), lems);
                        }
                        main.append(String.format("h(\"objectvar nc_%s_%d\")\n", synArrayName, i));
                        main.append(String.format("h(\"%s nc_%s_%d = new NetCon(&v(%f), %s[%d], %f, %f, %f)\")  \n\n", fromSecName, synArrayName, i, 0.5, synArrayName, i, threshold, delay, weight));
                    }

                }
            }

            ArrayList<Component> electricalProjections = targetComp.getChildrenAL("electricalProjection");

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

                    if(comp.getComponentType().getName().equals(NeuroMLElements.ELECTRICAL_CONNECTION))
                    {
                        number++;
                    }
                }

                String info0 = String.format("Adding projection: %s\nFrom %s to %s %d connection(s)", id, prePop, postPop, number);
                // System.out.println(info0);
                addComment(main, info0);

                String synapse = ep.getChildrenAL("connections").get(0).getStringValue("synapse");

                Component synapseComp = lems.getComponent(synapse);

                String mod = generateModFile(synapseComp);
                dumpModToFile(synapseComp, mod);

                String synObjNameA = String.format("syn_%s_%s_A", id, synapse);
                String synObjNameB = String.format("syn_%s_%s_B", id, synapse);

                main.append(String.format("h(\"objectvar %s[%d]\")\n\n", synObjNameA, number));
                main.append(String.format("h(\"objectvar %s[%d]\")\n\n", synObjNameB, number));

                int index = 0;
                for(Component ec : ep.getChildrenAL("connections"))
                {

                    if(ec.getComponentType().getName().equals(NeuroMLElements.ELECTRICAL_CONNECTION))
                    {
                        int preCellId = Integer.parseInt(ec.getStringValue("preCell"));
                        int postCellId = Integer.parseInt(ec.getStringValue("postCell"));

                        int preSegmentId = ec.hasStringValue("preSegment") ? Integer.parseInt(ec.getStringValue("preSegment")) : 0;
                        int postSegmentId = ec.hasStringValue("postSegment") ? Integer.parseInt(ec.getStringValue("postSegment")) : 0;

                        float preFractionAlong = ec.hasStringValue("preFractionAlong") ? Float.parseFloat(ec.getStringValue("preFractionAlong")) : 0.5f;
                        float postFractionAlong = ec.hasStringValue("postFractionAlong") ? Float.parseFloat(ec.getStringValue("postFractionAlong")) : 0.5f;

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

                        main.append(String.format("h(\"%s { %s[%d] = new %s(%f) }\")\n", preSecName, synObjNameA, index, synapse, preFractionAlong));
                        main.append(String.format("h(\"%s { %s[%d] = new %s(%f) }\")\n", postSecName, synObjNameB, index, synapse, postFractionAlong));

                        // addComment(main, "setpointer elecsyn_NetConn_PrePassiveCG_PostPassiveCG_GapJunc2_A[0].vgap, a_PrePassiveCG[0].Soma.v(0.5)");

                        /*
                         * TODO: remove hard coded vpeer/v link & figure this out from Component(Type) definition!!
                         */
                        main.append(String.format("h(\"setpointer %s[%d].vpeer, %s.v(%f)\")\n", synObjNameA, index, postSecName, postFractionAlong));
                        main.append(String.format("h(\"setpointer %s[%d].vpeer, %s.v(%f)\")\n", synObjNameB, index, preSecName, preFractionAlong));

                        index++;
                    }
                }

            }

            ArrayList<Component> inputLists = targetComp.getChildrenAL("inputs");

            for(Component inputList : inputLists)
            {
                String inputReference = inputList.getStringValue("component");
                Component inputComp = lems.getComponent(inputReference);

                String mod = generateModFile(inputComp);
                dumpModToFile(inputComp, mod);

                ArrayList<Component> inputs = inputList.getChildrenAL("inputs");

                for(Component input : inputs)
                {
                    String targetString = input.getStringValue("target");
                    int segmentId = input.hasStringValue("segmentId") ? Integer.parseInt(input.getStringValue("segmentId")) : 0;
                    float fractionAlong = input.hasStringValue("fractionAlong") ? Float.parseFloat(input.getStringValue("fractionAlong")) : 0.5f;

                    int cellNum = Utils.parseCellRefStringForCellNum(targetString);
                    String popName = Utils.parseCellRefStringForPopulation(targetString);

                    String secName;
                    String cellId = popIdsVsCellIds.get(popName);
                    Cell cell = compIdsVsCells.get(cellId);

                    if(cell != null)
                    {
                        NamingHelper nh0 = new NamingHelper(cell);
                        secName = String.format("a_%s[%s].%s", popName, cellNum, nh0.getNrnSectionName(CellUtils.getSegmentWithId(cell, segmentId)));
                    }
                    else
                    {
                        secName = popName + "[" + cellNum + "]";
                    }

                    String inputName = NRNUtils.getSafeName(inputList.getID()) + "_" + input.getID();

                    addComment(main, "Adding input: " + input);

                    main.append(String.format("\nh(\"objectvar %s\")\n", inputName));
                    main.append(String.format("h(\"%s { %s = new %s(%f) } \")\n\n", secName, inputName, NRNUtils.getSafeName(inputComp.getID()), fractionAlong));

                }

            }

            ArrayList<Component> explicitInputs = targetComp.getChildrenAL("explicitInputs");

            for(Component explInput : explicitInputs)
            {
                HashMap<String, Component> inputReference = explInput.getRefComponents();

                Component inputComp = inputReference.get("input");

                String safeName = NRNUtils.getSafeName(inputComp.getID());
                String inputName = explInput.getTypeName() + "_" + safeName;
                String mod = generateModFile(inputComp);

                dumpModToFile(inputComp, mod);

                String targetString = explInput.getStringValue("target");

                int cellNum = Utils.parseCellRefStringForCellNum(targetString);
                String popName = Utils.parseCellRefStringForPopulation(targetString);

                String secName;
                String cellId = popIdsVsCellIds.get(popName);
                Cell cell = compIdsVsCells.get(cellId);

                if(cell != null)
                {
                    NamingHelper nh0 = new NamingHelper(cell);
                    secName = String.format("a_%s[%s].%s", popName, cellNum, nh0.getNrnSectionName(cell.getMorphology().getSegment().get(0)));
                }
                else
                {
                    secName = popName + "[" + cellNum + "]";
                }
                inputName += "_" + popName + "_" + cellNum + "_" + secName.replaceAll("\\[", "").replaceAll("\\]", "").replaceAll("\\.", "_");

                addComment(main, "Adding input: " + explInput);

                main.append(String.format("\nh(\"objectvar %s\")\n", inputName));
                main.append(String.format("h(\"%s { %s = new %s(0.5) } \")\n\n", secName, inputName, safeName));

            }

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
            HashMap<String, ArrayList<String>> columnsPost = new HashMap<String, ArrayList<String>>();

            String timeRef = "time";
            String timefileName = target.timesFile != null ? target.timesFile : "time.dat";
            outfiles.put(timeRef, timefileName);

            columnsPre.put(timeRef, new ArrayList<String>());
            columnsPost.put(timeRef, new ArrayList<String>());

            columnsPre.get(timeRef).add("# Column: " + timeRef);
            columnsPre.get(timeRef).add("h(' objectvar v_" + timeRef + " ')");
            columnsPre.get(timeRef).add("h(' { v_" + timeRef + " = new Vector() } ')");
            columnsPre.get(timeRef).add("h(' v_" + timeRef + ".record(&t) ')");
            columnsPre.get(timeRef).add("h.v_" + timeRef + ".resize((h.tstop * h.steps_per_ms) + 1)");
            columnsPost.get(timeRef).add("    f_" + timeRef + "_f2.write('%f'% (float(h.v_" + timeRef + ".get(i))/1000.0))  # Save in SI units...");

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
                    if(columnsPost.get(outfileId) == null)
                    {
                        columnsPost.put(outfileId, new ArrayList<String>());
                    }

                    columnsPost.get(outfileId).add("    f_" + outfileId + "_f2.write('%f\\t'% (float(h.v_" + timeRef + ".get(i))/1000.0)) # Time in first column, save in SI units...");

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
                            columnsPost.get(outfileId).add(
                                    "    f_" + outfileId + "_f2.write('%f\\t'%(float(h.v_" + colId + ".get(i))" + factor + ")) # Saving as SI, variable has dim: " + lqp.getDimension().getName());

                        }
                    }
                }
            }

            for(String f : outfiles.keySet())
            {
                addComment(main, "File to save: " + f);
                // main.append(f + " = ???\n");

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

            main.append("print(\"Running a simulation of %sms (dt = %sms)\" % (h.tstop, h.dt))\n\n");
            main.append("h.run()\n\n");
            main.append("print(\"Finished simulation, saving results...\")\n\n");

            // main.append("objref SampleGraph\n");
            for(String dg : displayGraphs)
            {
                main.append(dg + ".exec_menu(\"View = plot\")\n");
            }
            main.append("\n");

            for(String f : outfiles.keySet())
            {
                addComment(main, "File to save: " + f);
                // String contents = "f_" + f + "_contents";
                // main.append(contents+" = ''\n");

                main.append("f_" + f + "_f2 = open('" + outfiles.get(f) + "', 'w')\n");
                main.append("for i in range(int(h.tstop * h.steps_per_ms) + 1):\n");
                for(String col : columnsPost.get(f))
                {
                    main.append(col + "\n");
                }
                main.append("    f_" + f + "_f2.write(\"\\n\")\n");

                // main.append("f_" + f + "_f2.write(f_" + f + "_contents)\n");
                main.append("f_" + f + "_f2.close()\n");
                main.append("print(\"Saved data to: " + outfiles.get(f) + "\")\n");

                main.append("\n");
            }

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

    private void writeModFile(String compName) throws ContentError
    {
        writeModFile(compName, null);
    }

    private void writeModFile(String compName, ChannelConductanceOption option) throws ContentError
    {
        if(!generatedModComponents.contains(compName))
        {
            Component comp = lems.getComponent(compName);
            String mod = generateModFile(comp, option);
            generatedModComponents.add(compName);

            dumpModToFile(comp, mod);
        }
    }

    private void dumpModToFile(Component comp, String mod) throws ContentError
    {
        File modFile = new File(getOutputFolder(), NRNUtils.getSafeName(comp.getID()) + ".mod");
        if(modWritten.containsKey(comp.getID()) && modWritten.get(comp.getID()).equals(mod))
        {
            E.info("-- Mod file for: " + comp.getID() + " has already been written");
            return;
        }
        E.info("-- Writing to: " + modFile.getAbsolutePath());

        try
        {
            FileUtil.writeStringToFile(mod, modFile);
            this.outputFiles.add(modFile);
            modWritten.put(comp.getID(), mod);
        }
        catch(IOException ex)
        {
            throw new ContentError("Error writing to file: " + modFile.getAbsolutePath(), ex);
        }
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
        @SuppressWarnings("unused")
        private float lengthFactor;
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

    public static String generateModFile(Component comp) throws ContentError
    {
        return generateModFile(comp, null);
    }

    public static String generateModFile(Component comp, ChannelConductanceOption condOption) throws ContentError
    {
        StringBuilder mod = new StringBuilder();

        String mechName = comp.getComponentType().getName();

        mod.append("TITLE Mod file for component: " + comp + "\n\n");

        mod.append("COMMENT\n\n" + Utils.getHeaderComment(Format.NEURON) + "\n\nENDCOMMENT\n\n");

        StringBuilder blockNeuron = new StringBuilder();
        StringBuilder blockUnits = new StringBuilder();
        StringBuilder blockParameter = new StringBuilder();
        StringBuilder blockAssigned = new StringBuilder();
        StringBuilder blockState = new StringBuilder();
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
            // /paramMappingsComp.put(NEURON_VOLTAGE,
            // NRNUtils.getStateVarName(NEURON_VOLTAGE));
            paramMappings.put(comp.getUniqueID(), paramMappingsComp);
        }

        blockUnits.append(NRNUtils.generalUnits);

        ArrayList<String> locals = new ArrayList<String>();

        boolean hasCaDependency = false;

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
                    }
                    for(Requirement r : child1.getComponentType().getRequirements())
                    {
                        if(r.getName().equals(NRNUtils.caConc))
                        {
                            hasCaDependency = true;
                        }
                    }
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

            if(ion != null)
            {
                blockNeuron.append("USEION " + ion + " READ " + ion + "i, " + ion + "o, i" + ion + " WRITE " + ion + "i VALENCE 2\n"); // TODO check valence
            }

            blockNeuron.append("RANGE cai\n");
            blockNeuron.append("RANGE cao\n");

            blockAssigned.append("cai (mM)\n");
            blockAssigned.append("cao (mM)\n");

            blockAssigned.append("ica (mA/cm2)\n");

            blockAssigned.append("diam (um)\n");

            blockAssigned.append("area (um2)\n");

            blockParameter.append(NeuroMLElements.CONC_MODEL_SURF_AREA + " (um2)\n");
            blockParameter.append(NeuroMLElements.CONC_MODEL_CA_TOT_CURR + " (nA)\n");

            ratesMethod.append(NeuroMLElements.CONC_MODEL_SURF_AREA + " = area\n\n");
            ratesMethod.append(NeuroMLElements.CONC_MODEL_CA_TOT_CURR + " = -1 * (0.01) * ica * " + NeuroMLElements.CONC_MODEL_SURF_AREA + " : To correct units...\n\n");

            // locals.add(NeuroMLElements.CONC_MODEL_SURF_AREA);
            // locals.add(NeuroMLElements.CONC_MODEL_CA_CURR_DENS);
            blockNeuron.append("GLOBAL " + NeuroMLElements.CONC_MODEL_INIT_CONC + "\n");
            blockNeuron.append("GLOBAL " + NeuroMLElements.CONC_MODEL_INIT_EXT_CONC + "\n");
            blockParameter.append(NeuroMLElements.CONC_MODEL_INIT_CONC + " (mM)\n");
            blockParameter.append(NeuroMLElements.CONC_MODEL_INIT_EXT_CONC + " (mM)\n");

            blockInitial.append(NeuroMLElements.CONC_MODEL_INIT_CONC + " = cai" + "\n");
            blockInitial.append(NeuroMLElements.CONC_MODEL_INIT_EXT_CONC + " = cao" + "\n");

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

        if(comp.getComponentType().isOrExtends(NeuroMLElements.BASE_SYNAPSE_COMP_TYPE))
        {
            blockNetReceiveParams = "weight (uS)";
            blockAssigned.append("? Standard Assigned variables with baseSynapse\n");
            blockAssigned.append("v (mV)\n");
            blockAssigned.append(NRNUtils.NEURON_TEMP + " (degC)\n");
            blockAssigned.append(NeuroMLElements.TEMPERATURE + " (K)\n");
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

        // ratesMethod.append("? - \n");
        parseDerivedVars(comp, prefix, rangeVars, ratesMethod, blockNeuron, blockParameter, blockAssigned, blockBreakpoint, paramMappings);

        // ratesMethod.append("? + \n");
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
        else if(comp.getComponentType().isOrExtends(NeuroMLElements.BASE_SYNAPSE_COMP_TYPE))
        {
            // ratesMethod.append("i = -1 * i ? Due to different convention in synapses\n");

        }

        parseTimeDerivs(comp, prefix, locals, blockDerivative, blockBreakpoint, blockAssigned, ratesMethod, paramMappings);

        if(blockDerivative.length() > 0)
        {
            blockBreakpoint.insert(0, "SOLVE states METHOD cnexp\n\n");
        }

        ArrayList<String> regimeNames = new ArrayList<String>();
        HashMap<String, Integer> flagsVsRegimes = new HashMap<String, Integer>();

        if(comp.getComponentType().hasDynamics())
        {

            int regimeFlag = 5000;
            for(Regime regime : comp.getComponentType().getDynamics().getRegimes())
            {
                flagsVsRegimes.put(regime.name, regimeFlag); // fill
                regimeFlag++;
            }

            // //String elsePrefix = "";
            for(Regime regime : comp.getComponentType().getDynamics().getRegimes())
            {
                String regimeStateName = NRNUtils.REGIME_PREFIX + regime.name;
                regimeNames.add(regimeStateName);

                // StringBuilder test = new
                // StringBuilder(": Testing for "+regimeStateName+ "\n");
                // test.append(elsePrefix+"if ("+regimeStateName+" == 1 ) {\n");
                // elsePrefix = "else ";
                // blockNetReceive.append("if ("+regimeStateName+" == 1 && flag == "+regimeFlag+") { "
                // +
                // ": Setting watch for OnCondition in "+regimeStateName+"...\n");
                // ////////blockNetReceive.append("    WATCH (" +
                // NRNUtils.checkForStateVarsAndNested(cond, comp, paramMappings) +
                // ") "+conditionFlag+"\n");
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
                        // int flagTarget =
                        // flagsVsRegimes.get(trans.getRegime());
                        // test.append("    net_send(0,"+flagTarget+") : Sending to regime_"+trans.getRegime()+"\n");

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
                // blockNetReceive.append("}\n");
                // blockBreakpoint_regimes.insert(0, test.toString()+ "\n");
                // blockBreakpoint_regimes.append(blockNetReceive.toString()+
                // "\n");
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
        // blockDerivative.insert(0, localsLine);

        blockInitial.append("rates()\n");

        if(comp.getComponentType().isOrExtends(NeuroMLElements.BASE_ION_CHANNEL_COMP_TYPE) || comp.getComponentType().isOrExtends(NeuroMLElements.BASE_SYNAPSE_COMP_TYPE))
        {
            blockInitial.append("\n" + NeuroMLElements.TEMPERATURE + " = " + NRNUtils.NEURON_TEMP + " + 273.15\n");
        }

        parseOnStart(comp, prefix, blockInitial, blockInitial_v, paramMappings);

        /*
         * for (OnStart os : comp.getComponentClass().getDynamics().getOnStarts()) { for (StateAssignment sa : os.getStateAssignments()) { blockInitial.append("\n" +
         * NRNUtils.getStateVarName(sa.getStateVariable().getName()) + " = " + sa.getEvaluable() + "\n"); } }
         */
        int conditionFlag = 1000;
        Dynamics dyn = comp.getComponentType().getDynamics();
        if(dyn != null)
        {

            for(OnCondition oc : dyn.getOnConditions())
            {
                String cond = NRNUtils.checkForBinaryOperators(oc.test);

                boolean resetVoltage = false;
                for(StateAssignment sa : oc.getStateAssignments())
                {
                    resetVoltage = resetVoltage || sa.getStateVariable().getName().equals(NRNUtils.NEURON_VOLTAGE);
                }

                if(!resetVoltage)  // A "normal" OnCondition
                {
                    if (!comp.getComponentType().isOrExtends(NeuroMLElements.BASE_SPIKE_SOURCE_COMP_TYPE)) 
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
                            blockNetReceive.append("\n    " + sa.getStateVariable().getName() + " = " + NRNUtils.checkForStateVarsAndNested(sa.getValueExpression(), comp, paramMappings) + "\n");
                        }
                        blockNetReceive.append("    net_event(t)\n");
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
                        blockNetReceive.append("\n    " + sa.getStateVariable().getName() + " = " + NRNUtils.checkForStateVarsAndNested(sa.getValueExpression(), comp, paramMappings) + "\n");
                    }
                    blockNetReceive.append("}\n");

                }
                conditionFlag++;
            }
        }

        parseOnEvent(comp, blockNetReceive, paramMappings);

        if(comp.getComponentType().isOrExtends(NeuroMLElements.CONC_MODEL_COMP_TYPE) && comp.getComponentType().getDynamics().getTimeDerivatives().isEmpty())
        {
            blockBreakpoint.append("\ncai = " + NeuroMLElements.CONC_MODEL_CONC_STATE_VAR + "\n\n");
        }

        // if (comp.getComponentType().hasDynamics()) {
        // for (Regime regime : comp.getComponentType().getDynamics()
        // .getRegimes()) {
        // String regimeStateName = "regime_" + regime.name;
        // blockNetReceive.append(": Conditions for " + regimeStateName);
        // int regimeFlag = flagsVsRegimes.get(regime.name);
        // blockNetReceive.append("if (flag == " + regimeFlag
        // + ") { : Entry into " + regimeStateName + "\n");
        // for (String r : regimeNames) {
        // blockNetReceive.append("    " + r + " = "
        // + (r.equals(regimeStateName) ? 1 : 0) + "\n");
        // }
        // for (OnEntry oe : regime.getOnEntrys()) {
        //
        // for (StateAssignment sa : oe.getStateAssignments()) {
        // blockNetReceive.append("\n    "
        // + NRNUtils.getStateVarName(sa.getStateVariable()
        // .getName())
        // + " = "
        // + NRNUtils.checkForStateVarsAndNested(sa.getEvaluable()
        // .toString(), comp, paramMappings)
        // + "\n");
        // }
        // }
        // blockNetReceive.append("}\n");
        // }
        // }
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
                    // blockBreakpoint.append("\ni = " + HIGH_CONDUCTANCE_PARAM
                    // + "*(v-" + NRNUtils.getStateVarName(NEURON_VOLTAGE) +
                    // ") ? To ensure v of section rapidly follows " +
                    // NRNUtils.getStateVarName(sv.getName()));

                    blockBreakpoint.append("\n" + NRNUtils.V_COPY_PREFIX + NRNUtils.NEURON_VOLTAGE + " = " + NRNUtils.NEURON_VOLTAGE);

                    if(comp.getComponentType().isOrExtends(NeuroMLElements.BASE_CELL_CAP_COMP_TYPE))
                    {
                        // blockBreakpoint.append("\ni = -1 * " +
                        // ABSTRACT_CELL_COMP_TYPE_CAP__I_MEMB + "");
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

        if(blockDerivative.length() == 0)
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

        if(blockFunctions.length() > 0)
        {
            mod.append(blockFunctions.toString());
        }

        return mod.toString();
    }

    private static void parseOnStart(Component comp, String prefix, StringBuilder blockInitial, StringBuilder blockInitial_v, HashMap<String, HashMap<String, String>> paramMappings)
            throws ContentError
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
                if(comp.getComponentType().isOrExtends(NeuroMLElements.BASE_SPIKE_SOURCE_COMP_TYPE))
                {
                    blockInitial.append("\nnet_send(0, 1) : go to NET_RECEIVE block, flag 1, for initial state\n");
                }
            }
        }
        for(Component childComp : comp.getAllChildren())
        {

            String prefixNew = getPrefix(childComp, prefix);

            parseOnStart(childComp, prefixNew, blockInitial, blockInitial_v, paramMappings);
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
                        blockNetReceive.append("state_discontinuity(" + NRNUtils.checkForStateVarsAndNested(sa.getStateVariable().getName(), comp, paramMappings) + ", "
                                + NRNUtils.checkForStateVarsAndNested(sa.getValueExpression(), comp, paramMappings) + ")\n");
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

                if(comp.getComponentType().isOrExtends(NeuroMLElements.BASE_POINT_CURR_COMP_TYPE) && exp.getName().equals(NeuroMLElements.POINT_CURR_CURRENT))
                {
                    blockNeuron.append("\n\nNONSPECIFIC_CURRENT " + NeuroMLElements.POINT_CURR_CURRENT + " \n");
                }
            }
        }

        for(Requirement req : comp.getComponentType().getRequirements())
        {
            String mappedName = prefixParent + req.getName();
            if(!req.getName().equals("v") && !req.getName().equals("temperature") && !req.getName().equals("caConc"))
            {
                // Make the assumption that the requirement is in the parent...
                paramMappingsComp.put(req.getName(), mappedName);
            }

        }

        for(Component childComp : comp.getAllChildren())
        {

            String prefixNew = getPrefix(childComp, prefix);

            parseParameters(childComp, prefixNew, prefix, rangeVars, stateVars, blockNeuron, blockParameter, paramMappings);

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
                    // //blockNeuron.append("\nRANGE " + HIGH_CONDUCTANCE_PARAM
                    // +
                    // "                  : High conductance for above current");
                    // //blockParameter.append("\n\n" + HIGH_CONDUCTANCE_PARAM +
                    // " = 1000 (S/cm2)");
                    blockAssigned.append("v (mV)\n");
                    blockAssigned.append("i (mA/cm2)\n\n");

                    blockAssigned.append(NRNUtils.V_COPY_PREFIX + NRNUtils.NEURON_VOLTAGE + " (mV)\n\n");

                    dim = "(nA)";
                }

                blockState.append(svName + " " + dim + "\n");
            }
        }

        for(Component childComp : comp.getAllChildren())
        {
            String prefixNew = getPrefix(childComp, prefix);

            parseStateVars(childComp, prefixNew, rangeVars, stateVars, blockNeuron, blockParameter, blockAssigned, blockState, paramMappings);
        }
    }

    private static String getPrefix(Component comp, String prefix)
    {
        // System.out.println("Getting prefix for " + comp);
        // System.out.println("comp " + comp.getID() + "; d " + comp.getDeclaredType() + "; e " + comp.getExtendsName());
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
            StringBuilder ratesMethod, HashMap<String, HashMap<String, String>> paramMappings) throws ContentError
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

                    if(comp.getComponentType().isOrExtends(NeuroMLElements.CONC_MODEL_COMP_TYPE) && td.getStateVariable().getName().equals(NeuroMLElements.CONC_MODEL_CONC_STATE_VAR))
                    {
                        line = line + "\ncai = " + td.getStateVariable().getName();
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
                // ratesMethod.insert(0,rateName + " = " + rateExpr + " \n");
                if(rateName.equals("rate_concentration") && rateExpr.contains("Faraday"))
                {
                    ratesMethod.append(rateName + " = (1e6) * " + rateExpr + " ? To correct units...\n");
                }
                else
                {
                    ratesMethod.append(rateName + " = " + rateExpr + " \n");
                }
            }

            ratesMethod.append("\n" + ratesMethodFinal + " \n");

        }

        for(Component childComp : comp.getAllChildren())
        {

            String prefixNew = getPrefix(childComp, prefix);

            parseTimeDerivs(childComp, prefixNew, locals, blockDerivative, blockBreakpoint, blockAssigned, ratesMethod, paramMappings);
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
                    if(comp.getComponentType().isOrExtends(NeuroMLElements.BASE_POINT_CURR_COMP_TYPE) && dv.getName().equals(NeuroMLElements.POINT_CURR_CURRENT))
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
                        // String var0 = var;

                        String eqn = globalVar;
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
                            eqn = eqn + " ? " + reduce + " applied to all instances of " + path + " in: <" + children + "> (" + comp.getChildrenAL(children) + ")" + " c2 (" + comp.getAllChildren()
                                    + ")";
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
        
        //lemsFiles.add(new File("../git/neuroml_use_case/LEMS_sim.xml"));
        
        lemsFiles.add(new File("../NeuroML2/LEMSexamples/InputTest.xml"));
        lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex0_IaF.xml")); 
        lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex9_FN.xml")); 
        lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex5_DetCell.xml"));
        
        lemsFiles.add(new File("../git/L5bPyrCellHayEtAl2011/neuroConstruct/generatedNeuroML2/LEMS_TestL5PC.xml")); 
        lemsFiles.add(new File("../neuroConstruct/osb/hippocampus/networks/nc_superdeep/neuroConstruct/generatedNeuroML2/LEMS_TestBasket.xml")); 
        lemsFiles.add(new File("../neuroConstruct/osb/cerebral_cortex/neocortical_pyramidal_neuron/MainenEtAl_PyramidalCell/neuroConstruct/generatedNeuroML2/LEMS_MainenEtAl_PyramidalCell.xml"));


        //lemsFiles.add(new File("../neuroConstruct/osb/cerebral_cortex/neocortical_pyramidal_neuron/L5bPyrCellHayEtAl2011/neuroConstruct/generatedNeuroML2/LEMS_TestL5PC.xml")); 
        lemsFiles.add(new File("../neuroConstruct/osb/cerebral_cortex/networks/ACnet2/neuroConstruct/generatedNeuroML2/LEMS_ACnet2.xml")); lemsFiles.add(new File("src/test/resources/BIOMD0000000185_LEMS.xml"));

        //lemsFiles.add(new File("../neuroConstruct/osb/hippocampus/networks/nc_superdeep/neuroConstruct/generatedNeuroML2/LEMS_nc_superdeep.xml"));

        //lemsFiles.add(new File("../neuroConstruct/osb/cerebral_cortex/neocortical_pyramidal_neuron/L5bPyrCellHayEtAl2011/neuroConstruct/generatedNeuroML2/LEMS_L5bPyrCellHayEtAl2011.xml"));
        lemsFiles.add(new File("../git/L5bPyrCellHayEtAl2011/neuroConstruct/generatedNeuroML2/LEMS_L5bPyrCellHayEtAl2011.xml"));

        lemsFiles.add(new File("../neuroConstruct/osb/invertebrate/lobster/PyloricNetwork/neuroConstruct/generatedNeuroML2/LEMS_PyloricPacemakerNetwork.xml"));

        lemsFiles.add(new File("../git/GPUShowcase/NeuroML2/LEMS_simplenet.xml")); 
        lemsFiles.add(new File("../git/BlueBrainProjectShowcase/ChannelTest/LEMS_TestVClamp.xml"));



        lemsFiles.add(new File("../neuroConstruct/osb/cerebellum/cerebellar_granule_cell/GranuleCell/neuroConstruct/generatedNeuroML2/LEMS_GranuleCell_LowDt.xml"));


        lemsFiles.add(new File("../neuroConstruct/osb/invertebrate/celegans/muscle_model/NeuroML2/LEMS_NeuronMuscle.xml"));


        //lemsFiles.add(new File("../neuroConstruct/osb/invertebrate/celegans/CElegansNeuroML/CElegans/pythonScripts/c302/LEMS_c302_A_Pharyngeal.xml")); 
        //lemsFiles.add(new File("../neuroConstruct/osb/invertebrate/celegans/CElegansNeuroML/CElegans/pythonScripts/c302/LEMS_Single.xml"));

        //lemsFiles.add(new File("../neuroConstruct/osb/invertebrate/celegans/CElegansNeuroML/CElegans/pythonScripts/c302/LEMS_c302_A_Pharyngeal.xml")); 
        lemsFiles.add(new File("../neuroConstruct/osb/invertebrate/celegans/CElegansNeuroML/CElegans/pythonScripts/c302/LEMS_c302_B_Syns.xml")); 

        lemsFiles.add(new File("../neuroConstruct/osb/invertebrate/celegans/CElegansNeuroML/CElegans/pythonScripts/c302/LEMS_c302_B_Social.xml"));
        /* */
        String testScript = "set -e\n";

        NeuronWriter nw;
        for(File lemsFile : lemsFiles)
        {
            Lems lems = Utils.readLemsNeuroMLFile(lemsFile.getAbsoluteFile()).getLems();
            nw = new NeuronWriter(lems, lemsFile.getParentFile(), lemsFile.getName().replaceAll(".xml", "_nrn.py"));
            // nw.setNoGui(true);

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
    public List<File> convert()
    {
        List<File> outputFiles = new ArrayList<File>();

        try
        {
            outputFiles = this.generateMainScriptAndMods();
        }
        catch(LEMSException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch(GenerationException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch(NeuroMLException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return outputFiles;
    }

}

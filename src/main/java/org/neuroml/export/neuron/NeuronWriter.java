package org.neuroml.export.neuron;

import static org.neuroml.export.neuron.ProcessManager.findNeuronHome;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
import org.lemsml.jlems.core.type.Dimension;
import org.lemsml.jlems.core.type.Exposure;
import org.lemsml.jlems.core.type.InstanceRequirement;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.core.type.LemsCollection;
import org.lemsml.jlems.core.type.ParamValue;
import org.lemsml.jlems.core.type.Property;
import org.lemsml.jlems.core.type.Requirement;
import org.lemsml.jlems.core.type.Target;
import org.lemsml.jlems.core.type.Meta;
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
import org.lemsml.jlems.io.IOUtil;
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

    // if false, generate populations etc. in order found in LEMS/NeuroML (hopefully)
    boolean generateAlphabetical = false;

    public static final String NEURON_HOME_ENV_VAR = "NEURON_HOME";

    public HashMap<String, String> modWritten = new HashMap<String, String>();

    private final HashMap<String, Cell> compIdsVsCells = new HashMap<String, Cell>();

    private final HashMap<String, String> popIdsVsCellIds = new HashMap<String, String>();

    private final HashMap<String, Component> popIdsVsComps = new HashMap<String, Component>();

    private final HashMap<String, IntracellularProperties> convertedCells = new HashMap<String, IntracellularProperties>();

    private final String bIndent = "        ";

    private boolean parallelMode = false; // Some of the mod files etc. will have to be slightly different for Parallel NEURON

    private static int MAX_LENGTH_LINE_MOD_FILE = 350;

    private final HashMap<String, String> hocRefsVsInputs = new HashMap<String, String>();

    public enum ChannelConductanceOption
    {
        FIXED_REVERSAL_POTENTIAL, USE_NERNST, USE_GHK, USE_GHK2;
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
        E.info("Creating NeuronWriter to output files to "+outputFolder.getAbsolutePath());
    }

    @Override
    public void setSupportedFeatures()
    {
        sli.addSupportInfo(format, ModelFeature.ABSTRACT_CELL_MODEL, SupportLevelInfo.Level.MEDIUM);
        sli.addSupportInfo(format, ModelFeature.COND_BASED_CELL_MODEL, SupportLevelInfo.Level.MEDIUM);
        sli.addSupportInfo(format, ModelFeature.EXT_MORPH_BIOPHYS_CELL_MODEL, SupportLevelInfo.Level.HIGH);
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

    public List<File> generateAndRun(boolean nogui, boolean compileMods, boolean run) throws LEMSException, GenerationException, NeuroMLException, IOException, ModelFeatureSupportException
    {
        return generateAndRun(nogui, compileMods, run, true);
    }

    public List<File> generateAndRun(boolean nogui, boolean compileMods, boolean run, boolean useNrnivForNoGui) throws LEMSException, GenerationException, NeuroMLException, IOException, ModelFeatureSupportException
    {

        this.nogui = nogui;
        List<File> files = generateMainScriptAndMods();

        if(compileMods || run)
        {
            if (compileMods)
            {
                E.info("Trying to compile all the mods in: " + this.getOutputFolder());

                boolean complied = ProcessManager.compileFileWithNeuron(this.getOutputFolder(), false);

                E.info("Success in compiling mods: " + complied);

                if (!complied)
                {
                    String mods = "";
                    for (File f: files )
                    {
                        if (f.getName().endsWith("mod"))
                            mods+=f.getAbsolutePath()+"; ";
                    }

                    throw new NeuroMLException("Error compiling mod files: "+mods);
                }
            }
            if (run)
            {
                File neuronHome = findNeuronHome();

                String nrncmd = nogui ? "nrniv" : "nrngui";
                String fullPath = new File(this.getOutputFolder(), this.getOutputFileName()).getCanonicalPath();

                List<String> commandToExecute = new ArrayList<String>();


                commandToExecute.add(neuronHome.getCanonicalPath() + System.getProperty("file.separator") + "bin" + System.getProperty("file.separator") + nrncmd);
                commandToExecute.add("-python");
                commandToExecute.add(fullPath);


                if (nogui && !useNrnivForNoGui)
                {
                    commandToExecute.add("python");
                    commandToExecute.add(fullPath);
                }

                Runtime rt = Runtime.getRuntime();
                //Process currentProcess = rt.exec(commandToExecute, null, this.getOutputFolder());
                String[] commandToExecuteArr = new String[ commandToExecute.size() ];
    						commandToExecute.toArray( commandToExecuteArr );
                Process currentProcess = rt.exec(commandToExecuteArr, null, this.getOutputFolder());

                ProcessOutputWatcher procOutputMain = new ProcessOutputWatcher(currentProcess.getInputStream(), "NRN Output >>");
                procOutputMain.start();

                ProcessOutputWatcher procOutputError = new ProcessOutputWatcher(currentProcess.getErrorStream(), "NRN Error  >>");
                procOutputError.start();

                E.info("Have successfully executed NEURON command: " + commandToExecute);

                try
                {
                    currentProcess.waitFor();

                    E.info("Exit value for running NEURON: " + currentProcess.exitValue());
                    String err = "Error, exit value from running this command: ["+commandToExecute
                                +"] in NEURON: "+currentProcess.exitValue()+"\n";

                    err += Utils.sysEnvInfo("  ");
                    if (currentProcess.exitValue()!=0)
                        throw new NeuroMLException(err);

                }
                catch(InterruptedException e)
                {
                    E.error("Problem executing Neuron " + e);
                    E.error("with: " + Utils.sysEnvInfo("  "));
                }
            }
        }
        return files;
    }

    @Override
    protected void addComment(StringBuilder sb, String comment)
    {
        addComment(sb, comment, "");
    }

    protected void addComment(StringBuilder sb, String comment, String indent)
    {
        if(!comment.contains("\n"))
        {
            sb.append(indent+NRNUtils.comm + comment + "\n");
        }
        else
        {
            sb.append(indent+NRNUtils.commPre + "\n" + indent+ comment.replaceAll("\n", "\n"+indent+"") + "\n" + indent + NRNUtils.commPost + "\n");
        }
    }

    private void reset()
    {
        outputFiles.clear();
    }

    public void setParallelMode(boolean parallel)
    {
        parallelMode = parallel;
    }

    public void setNoGui(boolean nogui)
    {
        this.nogui = nogui;
    }

    public boolean isNoGui()
    {
        return nogui;
    }

    public void setGenerateAlphabetical(boolean generateAlphabetical)
    {
        this.generateAlphabetical = generateAlphabetical;
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
        this.setParallelMode(false);

        try
        {
            reset();
            StringBuilder main = new StringBuilder();

            addComment(main, "Neuron simulator export for:\n\n" + lems.textSummary(false, false) + "\n\n" + Utils.getHeaderComment(format) + "\n");

            main.append("\nimport neuron\n");
            main.append("\nimport time");
            main.append("\nimport datetime");
            main.append("\nimport sys\n");
            main.append("\nimport hashlib\n");
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

            String len = simCpt.getStringValue("length");
            len = len.replaceAll("ms", "");
            if(len.indexOf("s") > 0)
            {
                len = len.replaceAll("s", "").trim();
                len = "" + Float.parseFloat(len) * 1000;
            }

            /* cvode usage:
             * https://nrn.readthedocs.io/en/latest/hoc/simctrl/cvode.html
             * - we do not currently support the local variable time step method
             */
            boolean nrn_cvode = false;
            String dt = "0.01";
            /* defaults from NEURON */
            String abs_tol = "None";
            String rel_tol = "None";
            LemsCollection<Meta> metas = simCpt.metas;
            for(Meta m : metas)
            {
                HashMap<String, String> attributes = m.getAttributes();
                if (attributes.getOrDefault("for", "").equals("neuron"))
                {
                    if (attributes.getOrDefault("method", "").equals("cvode"))
                    {
                        nrn_cvode = true;
                        abs_tol = attributes.getOrDefault("abs_tolerance", abs_tol);
                        rel_tol = attributes.getOrDefault("rel_tolerance", rel_tol);
                        E.info("CVode with abs_tol="+abs_tol+" , rel_tol="+rel_tol+" selected for NEURON simulation");
                    }
                }

            }

            if (nrn_cvode == false)
            {
                dt = simCpt.getStringValue("step");
                dt = dt.replaceAll("ms", "").trim();
                if(dt.indexOf("s") > 0)
                {
                    dt = dt.replaceAll("s", "").trim();
                    dt = "" + Float.parseFloat(dt) * 1000;
                }
            }

            main.append("class NeuronSimulation():\n\n");
            int seed = DLemsWriter.DEFAULT_SEED;
            if (simCpt.hasStringValue("seed"))
                seed = Integer.parseInt(simCpt.getStringValue("seed"));

            main.append("    def __init__(self, tstop, dt=None, seed="+seed+", abs_tol=None, rel_tol=None):\n\n");


            Component targetComp = simCpt.getRefComponents().get("target");

            main.append(bIndent+"print(\"\\n    Starting simulation in NEURON of %sms generated from NeuroML2 model...\\n\"%tstop)\n\n");
            main.append(bIndent+"self.setup_start = time.time()\n");
            main.append(bIndent+"self.seed = seed\n");
            main.append(bIndent+"self.abs_tol = abs_tol\n");
            main.append(bIndent+"self.rel_tol = rel_tol\n");

            if (target.reportFile!=null)
            {
                main.append(bIndent+"import socket\n");
                String reportFile = IOUtil.getCompleteReportFileName(target.reportFile, "NEURON", null);
                main.append(bIndent+"self.report_file = open('"+reportFile+"','w')\n");
                main.append(bIndent+"print('Simulator version:  %s'%h.nrnversion())\n");
                main.append(bIndent+"self.report_file.write('# Report of running simulation with %s\\n'%h.nrnversion())\n");
                main.append(bIndent+"self.report_file.write('Simulator=NEURON\\n')\n");
                main.append(bIndent+"self.report_file.write('SimulatorVersion=%s\\n'%h.nrnversion())\n\n");
                main.append(bIndent+"self.report_file.write('SimulationFile=%s\\n'%__file__)\n");
                main.append(bIndent+"self.report_file.write('PythonVersion=%s\\n'%sys.version.replace('\\n',' '))\n");
                main.append(bIndent+"print('Python version:     %s'%sys.version.replace('\\n',' '))\n");
                main.append(bIndent+"self.report_file.write('NeuroMLExportVersion="+Utils.ORG_NEUROML_EXPORT_VERSION+"\\n')\n");
                main.append(bIndent+"self.report_file.write('SimulationSeed=%s\\n'%self.seed)\n");
                main.append(bIndent+"self.report_file.write('Hostname=%s\\n'%socket.gethostname())\n");

            }

            main.append(bIndent+"self.randoms = []\n");
            main.append(bIndent+"self.next_global_id = 0  # Used in Random123 classes for elements using random(), etc. \n\n");
            main.append(bIndent+"self.next_spiking_input_id = 0  # Used in Random123 classes for elements using random(), etc. \n\n");

            String info = "Adding simulation " + simCpt + " of network/component: " + targetComp.summary();

            E.info(info);

            addComment(main, info+"\n", bIndent);

            ArrayList<Component> popsOrComponents = targetComp.getChildrenAL("populations", generateAlphabetical);

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
                    main.append("\n        # Temperature used for network: " + tempSI + " K\n");
                    main.append(bIndent+"h.celsius = " + tempSI + " - 273.15\n\n");

                }
            }

            for(Component popsOrComponent : popsOrComponents)
            {
                String compReference;
                String popName;
                int number;
                Component popComp;


                HashMap<Integer,String> locations = new HashMap<Integer,String>();
                HashMap<Integer,String> locationStrs = new HashMap<Integer,String>();


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

                            String location = "("+Float.parseFloat(loc.getAttributeValue(NeuroMLElements.LOCATION_X))
                                    +", "+Float.parseFloat(loc.getAttributeValue(NeuroMLElements.LOCATION_Y))
                                    +", "+Float.parseFloat(loc.getAttributeValue(NeuroMLElements.LOCATION_Z))+")";

                            String locationStr = "("+Float.parseFloat(loc.getAttributeValue(NeuroMLElements.LOCATION_X))
                                    +", "+Float.parseFloat(loc.getAttributeValue(NeuroMLElements.LOCATION_Y))
                                    +" + HALF_LENGTH, "+Float.parseFloat(loc.getAttributeValue(NeuroMLElements.LOCATION_Z))+", DIAMETER)";

                            locations.put(Integer.parseInt(instance.getID()), location);
                            locationStrs.put(Integer.parseInt(instance.getID()), locationStr);
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
                addComment(main, "######################   Population: "+popName,"        ");
                E.info("Adding population: "+popName);

                String compTypeName = popComp.getComponentType().getName();

                main.append(bIndent+"print(\"Population " + popName + " contains " + number + " instance(s) of component: " + popComp.getID() + " of type: " + popComp.getComponentType().getName() + "\")\n\n");

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
                        main.append(bIndent+"print(\"Setting the default initial concentrations for " + species.getIon() + " (used in "+cellName
                                +") to "+internal+" mM (internal), "+external+" mM (external)\")\n");

                        main.append(bIndent+"h(\"" + species.getIon() + "i0_" + species.getIon() + "_ion = " + internal + "\")\n");
                        main.append(bIndent+"h(\"" + species.getIon() + "o0_" + species.getIon() + "_ion = " + external + "\")\n\n");

                    }

                    StringBuilder popInfo = new StringBuilder();

                    popInfo.append(bIndent+"h.load_file(\"" + fileName + "\")\n");

                    popInfo.append("        a_" + popName + " = []\n");

                    popInfo.append(bIndent+"h(\"{ n_" + popName + " = " + number + " }\")\n");

                    popInfo.append(bIndent+"h(\"objectvar a_" + popName + "[n_" + popName + "]\")\n");

                    popInfo.append("        for i in range(int(h.n_" + popName + ")):\n");
                    // main.append("            cell = h."+cellName+"()\n");
                    popInfo.append("            h(\"a_" + popName + "[%i] = new " + cellName + "()\"%i)\n");
                    // main.append("            cell."+getNrnSectionName(cell.getMorphology().getSegment().get(0))+".push()\n");
                    popInfo.append("            h(\"access a_" + popName + "[%i]." + nh.getNrnSectionName(cell.getMorphology().getSegment().get(0)) + "\"%i)\n\n");
                    popInfo.append("            self.next_global_id+=1\n\n");

                    for (Integer cell_id: locations.keySet()) {
                        popInfo.append(bIndent+"h(\"{ a_" + popName + "["+cell_id+"].position"+locations.get(cell_id)+" }\")\n");
                    }

                    popInfo.append(String.format("\n        h(\"proc initialiseV_%s() { for i = 0, n_%s-1 { a_%s[i].set_initial_v() } }\")\n", popName, popName, popName));
                    popInfo.append(String.format(bIndent+"h(\"objref fih_%s\")\n", popName));
                    popInfo.append(String.format(bIndent+"h(\'{fih_%s = new FInitializeHandler(0, \"initialiseV_%s()\")}\')\n\n", popName, popName));

                    popInfo.append(String.format(bIndent+"h(\"proc initialiseIons_%s() { for i = 0, n_%s-1 { a_%s[i].set_initial_ion_properties() } }\")\n", popName, popName, popName));
                    popInfo.append(String.format(bIndent+"h(\"objref fih_ion_%s\")\n", popName));
                    popInfo.append(String.format(bIndent+"h(\'{fih_ion_%s = new FInitializeHandler(1, \"initialiseIons_%s()\")}\')\n\n", popName, popName));
                    main.append(popInfo);


                }
                else
                {
                    generateModForComp(popComp);

                    main.append(bIndent+"h(\" {n_" + popName + " = " + number + "} \")\n");

                    String mechName = NRNUtils.getMechanismName(popComp, popName);

                    addComment(main, "Population " + popName + " contains instances of " + popComp + "\n" +
                                     "whose dynamics will be implemented as a mechanism (" + popComp.getID() + ") in a mod file","        ");

                    main.append(bIndent+"h(\" create " + popName + "[" + number + "]\")\n");
                    main.append(bIndent+"h(\" objectvar " + mechName + "[" + number + "] \")\n\n");

                    main.append(bIndent+"for i in range(int(h.n_" + popName + ")):\n");
                    String instName = popName + "[i]";
                    // main.append(instName + " = h.Section()\n");
                    double defaultRadius = 5;
                    double length = defaultRadius * 2;
                    double diameter = defaultRadius * 2;

                    if(popComp.getComponentType().isOrExtends(NeuroMLElements.BASE_CELL_CAP_COMP_TYPE) ||
                       popComp.getComponentType().isOrExtends(NeuroMLElements.BASE_PYNN_CELL))
                    {
                        double capTotSI = -1;

                        if (popComp.getComponentType().isOrExtends(NeuroMLElements.BASE_CELL_CAP_COMP_TYPE))
                        {
                            //main.append(bIndent+"    #...\n");
                            if (popComp.hasParam("length"))
                            {
                                length = popComp.getParamValue("length").getDoubleValue() * 1e6;
                            }
                            if (popComp.hasParam("diameter"))
                            {
                                diameter = popComp.getParamValue("diameter").getDoubleValue() * 1e6;
                            }

                            if (popComp.hasParam("refract") && popComp.getParamValue("refract").getDoubleValue()==0)
                            {
                                throw new NeuroMLException("Unfortunately the NEURON export for IaF cells cannot *YET* handle "
                                    + "cases when refract = 0 (as in cell "+popComp.getID()+")");
                            }
                            capTotSI = popComp.getParamValue("C").getDoubleValue();
                        }
                        else if (popComp.getComponentType().isOrExtends(NeuroMLElements.BASE_PYNN_CELL))
                        {
                            if (popComp.hasParam("tau_refrac") && popComp.getParamValue("tau_refrac").getDoubleValue()==0)
                            {
                                throw new NeuroMLException("Unfortunately the NEURON export for PyNN cells cannot *YET* handle "
                                    + "cases when tau_refrac = 0 (as in cell "+popComp.getID()+")");
                            }
                            capTotSI = popComp.getParamValue("cm").getDoubleValue() * 1e-9;
                        }

                        double area = Math.PI * length * diameter;
                        double specCapNeu = 10e13 * capTotSI / area;
                        main.append(bIndent+"    h." + instName + "(0.5).cm = " + specCapNeu + "   # Computed from area "+area+"\n");
                    }
                    else
                    {
                        // See https://github.com/NeuroML/org.neuroml.export/issues/60
                        main.append(bIndent+"    h." + instName + "(0.5).cm = 318.31\n");
                    }

                    main.append(bIndent+"    h." + instName + ".L = " + length + "               # length to use for section in Neuron\n");
                    main.append(bIndent+"    h." + instName + "(0.5).diam = " + diameter + "       # diameter to use for section in Neuron\n");

                    main.append(bIndent+"    h." + instName + ".push()\n");
                    main.append(bIndent+"    h(\" " + instName.replaceAll("\\[i\\]", "[%i]") + "  { " + mechName + "[%i] = new " + popComp.getID() + "(0.5) } \"%(i,i))\n\n");

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
                        main.append(bIndent+"    h." + hocMechName + "." + pv.getName()
                                    + " = " + NRNUtils.convertToNeuronUnits((float) pv.getDoubleValue(), pv.getDimensionName())
                                    + " # NRN unit is: "+NRNUtils.getNeuronUnit(pv.getDimensionName())+"\n");
                    }

                    if (!popComp.getComponentType().isOrExtends(NeuroMLElements.SPIKE_ARRAY) &&
                        !popComp.getComponentType().isOrExtends(NeuroMLElements.SPIKE_GENERATOR) &&
                        (popComp.getComponentType().isOrExtends(NeuroMLElements.BASE_SPIKE_SOURCE_COMP_TYPE) ||
                        popComp.getComponentType().isOrExtends(NeuroMLElements.BASE_VOLT_DEP_CURR_SRC_SPIKING_COMP_TYPE)))
                    {
                        main.append(bIndent+"    # Spiking element ("+popComp.getComponentType().getName()+"), will require seeding...\n");
                        main.append(bIndent+"    rand = h.Random()\n");
                        main.append(bIndent+"    self.randoms.append(rand)\n");

                        main.append(bIndent+"    #print(\"Seeding random generator on "+hocMechName+" (i=%i) with stim seed %s\"%(i, self.seed))\n");
                        main.append(bIndent+"    self._init_stim_randomizer(rand,\""+popName+"\",i, self.seed)\n");
                        main.append(bIndent+"    rand.negexp(1)\n");
                        main.append(bIndent+"    h."+hocMechName+".noiseFromRandom(rand)\n\n");

                    }

                    if(popComp.getComponentType().isOrExtends(NeuroMLElements.BASE_RATE_UNIT))
                    {
                        main.append(bIndent+"    # use internal i_cap to gather info on incoming currents\n");
                        main.append(bIndent+"    h(\"setpointer " + NRNUtils.getMechanismName(popComp, popName) + "[%i].isyn_in, "+popName+"[%i].i_cap(0.5)\"%(i,i))\n\n");
                    }

                    main.append(bIndent+"    h.pop_section()\n\n");
                    main.append(bIndent+"    self.next_global_id+=1\n\n");


                    for (Integer cell_id: locationStrs.keySet()) {
                        main.append(bIndent+"h(\" " + popName + "["+cell_id+"] { pt3dclear() } \")\n");
                        main.append(bIndent+"h(\" " + popName + "["+cell_id+"] { pt3dadd"+locationStrs.get(cell_id).replace("HALF_LENGTH","("+length/2.+")").replace("DIAMETER","("+diameter+")")+" } \")\n");
                        main.append(bIndent+"h(\" " + popName + "["+cell_id+"] { pt3dadd"+locationStrs.get(cell_id).replace("HALF_LENGTH","("+length/-2.+")").replace("DIAMETER","("+diameter+")")+" } \")\n");
                    }
                    main.append("\n");

                }

            }

            // / Add projections/connections
            E.info("Adding projections/connections...");

            String synObjArrayName = null;

            ArrayList<Component> projections = targetComp.getChildrenAL("projections", generateAlphabetical);

            // TODO: change this to use arrays when vars are recorded too...
            boolean synArrayFormat = !NRNUtils.isPlottingSavingSynVariables(simCpt, nogui);

            HashMap<String, Integer> segmentSynapseCounts = new HashMap<String, Integer>();

            for(Component projection : projections)
            {
                String projId = projection.getID();
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
                addComment(main, "######################   Projection: "+projId,"        ");
                info = String.format("Adding projection: %s, from %s to %s with synapse %s, %d connection(s)", projId, prePop, postPop, synapse, number);
                main.append(bIndent+"print(\""+info+"\")\n\n");

                Component synapseComp = projection.getRefComponents().get("synapse");

                generateModForComp(synapseComp);

                String netConnObjArrayName = null;

                if (synArrayFormat)
                {
                    synObjArrayName = String.format("syn_%s_%s", projId, synapse);
                    netConnObjArrayName = String.format("netConn_%s_%s", projId, synapse);

                    main.append(String.format(bIndent+"h(\"objectvar %s[%d]\")\n\n", synObjArrayName, number));
                    main.append(String.format(bIndent+"h(\"objectvar %s[%d]\")\n\n", netConnObjArrayName, number));
                }

                NamingHelper nhPre = new NamingHelper(preCell);
                NamingHelper nhPost = new NamingHelper(postCell);

                int connIndex = 0;
                for(Component conn : projection.getAllChildren())
                {
                    if(conn.getComponentType().isOrExtends(NeuroMLElements.CONNECTION))
                    {
                        int preCellId = Utils.parseCellRefStringForCellNum(conn.getStringValue("preCellId"));
                        int postCellId = Utils.parseCellRefStringForCellNum(conn.getStringValue("postCellId"));

                        int preSegmentId = conn.hasAttribute("preSegmentId") ? Integer.parseInt(conn.getAttributeValue("preSegmentId")) : 0;
                        int postSegmentId = conn.hasAttribute("postSegmentId") ? Integer.parseInt(conn.getAttributeValue("postSegmentId")) : 0;

                        float preFractionAlong0 = conn.hasAttribute("preFractionAlong") ? Float.parseFloat(conn.getAttributeValue("preFractionAlong")) : 0.5f;
                        float postFractionAlong0 = conn.hasAttribute("postFractionAlong") ? Float.parseFloat(conn.getAttributeValue("postFractionAlong")) : 0.5f;

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

                        String segSynRef = String.format("syn_%s_%s_%s_%s", postPop,postCellId, postSegmentId, synapse);
                        if (!segmentSynapseCounts.containsKey(segSynRef))
                            segmentSynapseCounts.put(segSynRef, 0);
                        int synIndex = segmentSynapseCounts.get(segSynRef);

                        String synObjName = null;
                        if (synArrayFormat)
                        {
                            synObjName = String.format("%s[%s]", synObjArrayName, connIndex);
                        }
                        else
                        {
                            synObjName = String.format("%s_%s", segSynRef, connIndex);
                        }
                        segmentSynapseCounts.put(segSynRef, synIndex+1);

                        float postFract = postFractionAlong0;
                        if(postCell != null)
                        {
                            postFract = !CellUtils.hasUnbranchedNonOverlappingInfo(postCell) ? postFractionAlong0 : (float) CellUtils.getFractionAlongSegGroupLength(postCell, postSecName.split("\\.")[1], postSegmentId, postFractionAlong0);
                        }

                        float preFract = preFractionAlong0;

                        if(preCell != null)
                        {
                            preFract = !CellUtils.hasUnbranchedNonOverlappingInfo(preCell) ? preFractionAlong0 : (float) CellUtils.getFractionAlongSegGroupLength(preCell, preSecName.split("\\.")[1], preSegmentId, preFractionAlong0);
                        }

                        String sourceVarToListenFor = "&v("+ preFract+")";

                        float weight = 1;
                        float delay = 0;
                        if (conn.getComponentType().isOrExtends(NeuroMLElements.CONNECTION_WEIGHT_DELAY))
                        {
                            weight = Float.parseFloat(conn.getAttributeValue("weight"));
                            delay = NRNUtils.convertToNeuronUnits(conn.getAttributeValue("delay"), lems);
                        }

                        String comment = String.format(Locale.US, "Connection %s: cell %d, seg %d (%s) [%s on %s] -> cell %d, seg %d (%s) [%s on %s], weight: %s, delay %s", conn.getID(), preCellId, preSegmentId, preFractionAlong0, preFract, preSecName, postCellId, postSegmentId, postFractionAlong0, postFract, postSecName, weight, delay);
                        //System.out.println("comment@: "+comment);
                        addComment(main, comment,"        ");

                        if (!synArrayFormat)
                        {
                            main.append(String.format(bIndent+"h(\"objectvar %s\")\n", synObjName));
                        }
                        main.append(String.format(Locale.US, bIndent+"h(\"%s %s = new %s(%s)\")\n", postSecName, synObjName, synapse, postFract));

                        if(preCell != null)
                        {
                            float threshold;

                            if (getMembraneProperties(preCell).getSpikeThresh().size()>0)
                            {
                                SpikeThresh st = getMembraneProperties(preCell).getSpikeThresh().get(0);
                                if (!st.getSegmentGroup().equals(NeuroMLElements.SEGMENT_GROUP_ALL))
                                {
                                    throw new NeuroMLException("Cannot yet handle <spikeThresh> when it is not on segmentGroup all");
                                }

                                threshold = NRNUtils.convertToNeuronUnits(st.getValue(), lems);
                            }
                            else
                            {
                                threshold = 0;
                            }
                            if(postCell != null)
                            {
                                main.append(String.format(bIndent+"h(\"%s a_%s[%d].synlist.append(new NetCon(%s, %s, %s, %s, %s))\") # *->cell\n\n", preSecName, postPop, postCellId, sourceVarToListenFor, synObjName,
                                    threshold, delay, weight));
                            }
                            else
                            {
                                main.append(String.format(bIndent+"h(\"%s %s[%d] = new NetCon(%s, %s, %g, %g, %g)\")  # cell->abst\n\n", preSecName, netConnObjArrayName, connIndex, sourceVarToListenFor, synObjName, threshold, delay, weight));
                            }
                        }
                        else
                        {
                            Component preComp = popIdsVsComps.get(prePop);
                            float threshold = NRNUtils.getThreshold(preComp, lems);
                            if(preComp.getComponentType().isOrExtends(NeuroMLElements.BASE_SPIKE_SOURCE_COMP_TYPE))
                            {
                                String hocMechName = NRNUtils.getMechanismName(preComp, prePop) + "["+preCellId+"]";
                                sourceVarToListenFor = hocMechName;
                            }

                            if (synArrayFormat)
                            {
                                main.append(String.format(Locale.US, bIndent+"h(\"%s %s[%d] = new NetCon(%s, %s, %s, %s, %s)\")  \n\n", preSecName, netConnObjArrayName, connIndex, sourceVarToListenFor, synObjName, threshold, delay, weight));
                            }
                            else
                            {
                                String netConnName = String.format("nc_%s_%d", synObjName, connIndex);
                                main.append(String.format(bIndent+"h(\"objectvar %s\")\n", netConnName));
                                main.append(String.format(Locale.US, bIndent+"h(\"%s %s = new NetCon(%s, %s, %s, %s, %s)\")  \n\n", preSecName, netConnName, sourceVarToListenFor, synObjName, threshold, delay, weight));
                            }

                           }
                        connIndex++;
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
                addComment(main, info0,bIndent);

                //String synArrayName = String.format("synapses_%s", synapseComp.getID());
                //main.append(String.format(bIndent+"h(\"objectvar %s[%d]\")\n\n", synArrayName, connections.size()));

                for(int i = 0; i < connections.size(); i++)
                {
                    Component connection = connections.get(i);

                    String fromRef = connection.getStringValue("from");
                    int preCellId = Utils.parseCellRefStringForCellNum(fromRef);
                    String prePop = Utils.parseCellRefStringForPopulation(fromRef);
                    Cell preCell = compIdsVsCells.get(popIdsVsCellIds.get(prePop));
                    String preSecName;

                    if(preCell != null)
                    {
                        NamingHelper nh0 = new NamingHelper(preCell);
                        preSecName = String.format("a_%s[%s].%s", prePop, preCellId, nh0.getNrnSectionName(preCell.getMorphology().getSegment().get(0)));
                    }
                    else
                    {
                        preSecName = prePop + "[" + preCellId + "]";
                    }

                    String postRef = connection.getStringValue("to");
                    int postCellId = Utils.parseCellRefStringForCellNum(postRef);
                    String postPop = Utils.parseCellRefStringForPopulation(postRef);
                    Cell postCell = compIdsVsCells.get(popIdsVsCellIds.get(postPop));
                    String toSecName;
                    if(postCell != null)
                    {
                        NamingHelper nh0 = new NamingHelper(postCell);
                        toSecName = String.format("a_%s[%s].%s", postPop, postCellId, nh0.getNrnSectionName(postCell.getMorphology().getSegment().get(0)));
                    }
                    else
                    {
                        toSecName = postPop + "[" + postCellId + "]";
                    }

                    String info1 = String.format("Adding connection from %s to %s", fromRef, postRef);
                    addComment(main, info1,bIndent);

                    String segSynRef = String.format("syn_%s_%s_%s_%s", postPop, postCellId, 0, synapseComp.getID());
                    if (!segmentSynapseCounts.containsKey(segSynRef))
                        segmentSynapseCounts.put(segSynRef, 0);
                    int synIndex = segmentSynapseCounts.get(segSynRef);
                    String synObjName = String.format("%s_%s", segSynRef, synIndex);
                    main.append(String.format(bIndent+"h(\"objectvar %s\")\n\n", synObjName));
                    segmentSynapseCounts.put(segSynRef, synIndex+1);

                    main.append(String.format(Locale.US, bIndent+"h(\"%s %s = new %s(%s)\")\n", toSecName, synObjName, synapseComp.getID(), 0.5));

                    float delay = connection.hasParam("delay") ? (float) connection.getParamValue("delay").getDoubleValue() * 1000 : 0.0f;
                    //this also accounts for dimensional weights, if we ever want to support that.
                    float weight = connection.hasParam("weight") ? (float) NRNUtils.convertToNeuronUnits(connection.getAttributeValue("weight") , lems) : 1.0f;

                    if(postCell != null)
                    {
                        Component fromComp = popIdsVsComps.get(prePop);
                        float fract = 0.5f;
                        String sourceVarToListenFor = "&v("+fract+")";
                        float threshold = NRNUtils.getThreshold(fromComp, lems);

                        if(fromComp.getComponentType().isOrExtends(NeuroMLElements.BASE_SPIKE_SOURCE_COMP_TYPE))
                        {
                            String hocMechName = NRNUtils.getMechanismName(fromComp, prePop) + "["+preCellId+"]";
                            sourceVarToListenFor = hocMechName;
                        }

                        main.append(String.format(bIndent+"h(\"%s a_%s[%d].synlist.append(new NetCon(%s, %s, %g, %g, %g))\") # ...\n\n", preSecName, postPop, postCellId, sourceVarToListenFor, synObjName, threshold, delay, weight));
                    }
                    else
                    {
                        Component fromComp = popIdsVsComps.get(prePop);
                        float threshold = NRNUtils.getThreshold(fromComp, lems);

                        String sourceVarToListenFor = "&v(0.5)";

                        if(fromComp.getComponentType().isOrExtends(NeuroMLElements.BASE_SPIKE_SOURCE_COMP_TYPE))
                        {
                            String hocMechName = NRNUtils.getMechanismName(fromComp, prePop) + "["+preCellId+"]";
                            sourceVarToListenFor = hocMechName;
                        }
                        main.append(String.format(bIndent+"h(\"objectvar nc_%s_%d\")\n", synObjName, i));
                        main.append(String.format(bIndent+"h(\"%s nc_%s_%d = new NetCon(%s, %s, %g, %g, %g)\")  # ,,,\n\n", preSecName, synObjName, i, sourceVarToListenFor, synObjName, threshold, delay, weight));
                    }

                }
            }

            ArrayList<Component> electricalProjections = targetComp.getChildrenAL(NeuroMLElements.ELECTRICAL_PROJECTION, generateAlphabetical);

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

                addComment(main, "######################   Electrical Projection: "+id,"        ");
                info = String.format("Adding electrical projection: %s from %s to %s, with %d connection(s)", id, prePop, postPop, number);
                main.append(bIndent+"print(\""+info+"\")\n\n");

                ArrayList<Component> connChildren = ep.getChildrenAL("connections");
                connChildren.addAll(ep.getChildrenAL("connectionInstances"));

                Component synapseComp = connChildren.get(0).getRefComponents().get("synapse");

                generateModForComp(synapseComp);

                String synObjNameA = String.format("syn_%s_%s_A", id, synapseComp.getID());
                String synObjNameB = String.format("syn_%s_%s_B", id, synapseComp.getID());

                main.append(String.format(bIndent+"h(\"objectvar %s[%d]\")\n", synObjNameA, number));
                main.append(String.format(bIndent+"h(\"objectvar %s[%d]\")\n\n", synObjNameB, number));

                int index = 0;
                for(Component ec : connChildren)
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

                        int preSegment = ec.hasAttribute("preSegment") ? Integer.parseInt(ec.getAttributeValue("preSegment")) : 0;
                        int postSegment = ec.hasAttribute("postSegment") ? Integer.parseInt(ec.getAttributeValue("postSegment")) : 0;

                        float preFractionAlong0 = ec.hasAttribute("preFractionAlong") ? Float.parseFloat(ec.getAttributeValue("preFractionAlong")) : 0.5f;
                        float postFractionAlong0 = ec.hasAttribute("postFractionAlong") ? Float.parseFloat(ec.getAttributeValue("postFractionAlong")) : 0.5f;

                        // System.out.println("preCellId: "+preCellId+", preSegmentId: "+preSegmentId+", preFractionAlong: "+preFractionAlong);

                        String preSecName;

                        if(preCell != null)
                        {
                            NamingHelper nhPre = new NamingHelper(preCell);
                            preSecName = String.format("a_%s[%s].%s", prePop, preCellId, nhPre.getNrnSectionName(CellUtils.getSegmentWithId(preCell, preSegment)));
                        }
                        else
                        {
                            preSecName = prePop + "[" + preCellId + "]";
                        }

                        String postSecName;
                        if(postCell != null)
                        {
                            NamingHelper nhPost = new NamingHelper(postCell);
                            postSecName = String.format("a_%s[%s].%s", postPop, postCellId, nhPost.getNrnSectionName(CellUtils.getSegmentWithId(postCell, postSegment)));
                        }
                        else
                        {
                            postSecName = postPop + "[" + postCellId + "]";
                        }

                        float postFract = postFractionAlong0;
                        if(postCell != null)
                        {
                            postFract = !CellUtils.hasUnbranchedNonOverlappingInfo(postCell) ? postFractionAlong0 : (float) CellUtils.getFractionAlongSegGroupLength(postCell, postSecName.split("\\.")[1], postSegment, postFractionAlong0);
                        }

                        float preFract = preFractionAlong0;
                        if(preCell != null)
                        {
                            preFract = !CellUtils.hasUnbranchedNonOverlappingInfo(preCell) ? preFractionAlong0 : (float) CellUtils.getFractionAlongSegGroupLength(preCell, preSecName.split("\\.")[1], preSegment, preFractionAlong0);
                        }

                        float weight = 1;
                        if (ec.getComponentType().isOrExtends(NeuroMLElements.ELECTRICAL_CONNECTION_INSTANCE_WEIGHT))
                        {
                            weight = Float.parseFloat(ec.getAttributeValue("weight"));
                        }

                        String comment = String.format(Locale.US, "Elect Connection %s: cell %d, seg %d (%s) [%s on %s] -> cell %d, seg %d (%s) [%s on %s], weight: %s", ec.getID(), preCellId, preSegment, preFractionAlong0, preFract, preSecName, postCellId, postSegment, postFractionAlong0, postFract, postSecName, weight);

                        addComment(main, comment,"        ");

                        main.append(String.format(Locale.US, bIndent+"h(\"%s { %s[%d] = new %s(%s) }\")\n", preSecName, synObjNameA, index, synapseComp.getID(), preFract));
                        main.append(String.format(Locale.US, bIndent+"h(\"%s { %s[%d] = new %s(%s) }\")\n", postSecName, synObjNameB, index, synapseComp.getID(), postFract));

                        if (weight!=1)
                        {
                            main.append(String.format(Locale.US, bIndent+"h(\"%s { %s[%d].weight = %s }\")\n", preSecName, synObjNameA, index, weight));
                            main.append(String.format(Locale.US, bIndent+"h(\"%s { %s[%d].weight = %s }\")\n", postSecName, synObjNameB, index, weight));
                        }

                        // addComment(main, "setpointer elecsyn_NetConn_PrePassiveCG_PostPassiveCG_GapJunc2_A[0].vgap, a_PrePassiveCG[0].Soma.v(0.5)");

                        /*
                         * TODO: remove hard coded vpeer/v link & figure this out from Component(Type) definition!!
                         */
                        main.append(String.format(Locale.US, bIndent+"h(\"setpointer %s[%d].vpeer, %s.v(%s)\")\n", synObjNameA, index, postSecName, postFract));
                        main.append(String.format(Locale.US, bIndent+"h(\"setpointer %s[%d].vpeer, %s.v(%s)\")\n\n", synObjNameB, index, preSecName, preFract));

                        index++;
                    }
                }
            }

            ArrayList<Component> continuousProjections = targetComp.getChildrenAL(NeuroMLElements.CONTINUOUS_PROJECTION, generateAlphabetical);

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
                    if(comp.getComponentType().isOrExtends(NeuroMLElements.CONTINUOUS_CONNECTION) ||
                       comp.getComponentType().isOrExtends(NeuroMLElements.CONTINUOUS_CONNECTION_INSTANCE))
                    {
                        number++;
                    }
                }

                addComment(main, "######################   Continuous Projection: "+id,"        ");
                info = String.format("Adding continuous projection: %s from %s to %s, with %d connection(s)", id, prePop, postPop, number);
                main.append(bIndent+"print(\""+info+"\")\n\n");

                ArrayList<Component> connChildren = ep.getChildrenAL("connections");
                connChildren.addAll(ep.getChildrenAL("connectionInstances"));

                Component preComponent = connChildren.get(0).getRefComponents().get("preComponent");
                Component postComponent = connChildren.get(0).getRefComponents().get("postComponent");

                generateModForComp(preComponent);
                generateModForComp(postComponent);

                String preCompObjName = String.format("syn_%s_%s_pre", id, preComponent.getID());
                String postCompObjName = String.format("syn_%s_%s_post", id, postComponent.getID());

                main.append(String.format(bIndent+"h(\"objectvar %s[%d]\")\n", preCompObjName, number));
                main.append(String.format(bIndent+"h(\"objectvar %s[%d]\")\n\n", postCompObjName, number));

                int contConnIndex = 0;
                for(Component cc : connChildren)
                {
                    if(cc.getComponentType().isOrExtends(NeuroMLElements.CONTINUOUS_CONNECTION) ||
                       cc.getComponentType().isOrExtends(NeuroMLElements.CONTINUOUS_CONNECTION_INSTANCE))
                    {
                        int preCellId = -1;
                        int postCellId = -1;

                        if(cc.getComponentType().isOrExtends(NeuroMLElements.CONTINUOUS_CONNECTION))
                        {
                            preCellId = Integer.parseInt(cc.getStringValue("preCell"));
                            postCellId = Integer.parseInt(cc.getStringValue("postCell"));
                        }
                        else if(cc.getComponentType().isOrExtends(NeuroMLElements.CONTINUOUS_CONNECTION_INSTANCE))
                        {
                            preCellId = Utils.parseCellRefStringForCellNum(cc.getStringValue("preCell"));
                            postCellId = Utils.parseCellRefStringForCellNum(cc.getStringValue("postCell"));
                        }
                        int preSegment = cc.hasAttribute("preSegment") ? Integer.parseInt(cc.getAttributeValue("preSegment")) : 0;
                        int postSegment = cc.hasAttribute("postSegment") ? Integer.parseInt(cc.getAttributeValue("postSegment")) : 0;

                        float preFractionAlong0 = cc.hasAttribute("preFractionAlong") ? Float.parseFloat(cc.getAttributeValue("preFractionAlong")) : 0.5f;
                        float postFractionAlong0 = cc.hasAttribute("postFractionAlong") ? Float.parseFloat(cc.getAttributeValue("postFractionAlong")) : 0.5f;


                        String preSecName;

                        if(preCell != null)
                        {
                            NamingHelper nhPre = new NamingHelper(preCell);
                            preSecName = String.format("a_%s[%s].%s", prePop, preCellId, nhPre.getNrnSectionName(CellUtils.getSegmentWithId(preCell, preSegment)));
                        }
                        else
                        {
                            preSecName = prePop + "[" + preCellId + "]";
                        }

                        String postSecName;
                        if(postCell != null)
                        {
                            NamingHelper nhPost = new NamingHelper(postCell);
                            postSecName = String.format("a_%s[%s].%s", postPop, postCellId, nhPost.getNrnSectionName(CellUtils.getSegmentWithId(postCell, postSegment)));
                        }
                        else
                        {
                            postSecName = postPop + "[" + postCellId + "]";
                        }

                        float postFract = postFractionAlong0;
                        if(postCell != null)
                        {
                            postFract = !CellUtils.hasUnbranchedNonOverlappingInfo(postCell) ? postFractionAlong0 : (float) CellUtils.getFractionAlongSegGroupLength(postCell, postSecName.split("\\.")[1], postSegment, postFractionAlong0);
                        }

                        float preFract = preFractionAlong0;
                        if(preCell != null)
                        {
                            preFract = !CellUtils.hasUnbranchedNonOverlappingInfo(preCell) ? preFractionAlong0 : (float) CellUtils.getFractionAlongSegGroupLength(preCell, preSecName.split("\\.")[1], preSegment, preFractionAlong0);
                        }

                        float weight = 1;
                        if (cc.getComponentType().isOrExtends(NeuroMLElements.CONTINUOUS_CONNECTION_INSTANCE_WEIGHT))
                        {
                            weight = Float.parseFloat(cc.getAttributeValue("weight"));
                        }

                        String comment = String.format(Locale.US, "Continuous Connection %s: cell %d, seg %d (%s) [%s on %s] -> cell %d, seg %d (%s) [%s on %s], weight: %s", cc.getID(), preCellId, preSegment, preFractionAlong0, preFract, preSecName, postCellId, postSegment, postFractionAlong0, postFract, postSecName, weight);

                        addComment(main, comment,"        ");

                        main.append(String.format(Locale.US, bIndent+"h(\"%s { %s[%d] = new %s(%f) }\")\n", preSecName, preCompObjName, contConnIndex, preComponent.getID(), preFract));
                        main.append(String.format(Locale.US, bIndent+"h(\"%s { %s[%d] = new %s(%f) }\")\n", postSecName, postCompObjName, contConnIndex, postComponent.getID(), postFract));

                        if (weight!=1)
                        {
                            main.append(String.format(Locale.US, bIndent+"h(\"%s { %s[%d].weight = %s }\")\n", preSecName, preCompObjName, contConnIndex, weight));
                            main.append(String.format(Locale.US, bIndent+"h(\"%s { %s[%d].weight = %s }\")\n", postSecName, postCompObjName, contConnIndex, weight));
                        }

                        String peerVar = "v";
                        String prePrefix = "";
                        String postPrefix = "";
                        String preArg = "("+preFract+")";
                        String postArg = "("+postFract+")";

                        if(postComponent.getComponentType().getName().toLowerCase().contains("rate"))
                        {
                            peerVar = "r";
                            prePrefix = "m_"+popIdsVsComps.get(prePop).getID()+"_";
                            postPrefix = "m_"+popIdsVsComps.get(postPop).getID()+"_";
                            preArg = "";
                            postArg = "";
                        }
                        /*
                         * TODO: remove hard coded vpeer/v link & figure this out from Component(Type) definition!!
                         */
                        main.append(String.format(Locale.US, bIndent+"h(\"setpointer %s[%d].%speer, %s%s.%s%s\") # %s - %s\n", preCompObjName, contConnIndex, peerVar, postPrefix, postSecName, peerVar, postArg, preComponent, postComponent));
                        main.append(String.format(Locale.US, bIndent+"h(\"setpointer %s[%d].%speer, %s%s.%s%s\")\n\n", postCompObjName, contConnIndex, peerVar, prePrefix, preSecName, peerVar, preArg));

                        contConnIndex++;
                    }
                }
            }

            processInputLists(main, targetComp);
            processExplicitInputs(main, targetComp);

            main.append(bIndent+"trec = h.Vector()\n");
            main.append(bIndent+"trec.record(h._ref_t)\n\n");

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

                        String dispGraph = "self.display_" + dispId;
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

                                LEMSQuantityPathNeuron lqp = new LEMSQuantityPathNeuron(quantity, scale, targetComp, compMechNamesHoc, popsOrComponents, compIdsVsCells, hocRefsVsInputs, lems);

                                //System.out.println("lqp: "+lqp);

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

            main.append(bIndent+"h.tstop = tstop\n\n");
            main.append(bIndent+"if self.abs_tol is not None and self.rel_tol is not None:\n");
            main.append(bIndent+"    cvode = h.CVode()\n");
            main.append(bIndent+"    cvode.active(1)\n");
            main.append(bIndent+"    cvode.atol(self.abs_tol)\n");
            main.append(bIndent+"    cvode.rtol(self.rel_tol)\n");
            main.append(bIndent+"else:\n");
            main.append(bIndent+"    h.dt = dt\n");
            main.append(bIndent+"    h.steps_per_ms = 1/h.dt\n\n");

            if(!nogui)
            {
                for(String dg : displayGraphs)
                {
                    addComment(main, "######################   Display: " + dg, bIndent);
                    main.append(bIndent+dg + " = h.Graph(0)\n");
                    main.append(bIndent+dg + ".size(0,h.tstop,-80.0,50.0)\n");
                    main.append(bIndent+dg + ".view(0, -80.0, h.tstop, 130.0, 80, 330, 330, 250)\n");
                    main.append(bIndent+"h.graphList[0].append(" + dg + ")\n");
                    if(plots.containsKey(dg))
                    {
                        for(String plot : plots.get(dg))
                        {
                            main.append(bIndent+plot + "\n");
                        }
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

            columnsPre.get(timeRef).add(bIndent+"# Column: " + timeRef);
            columnsPre.get(timeRef).add(bIndent+"h(' objectvar v_" + timeRef + " ')");
            columnsPre.get(timeRef).add(bIndent+"h(' { v_" + timeRef + " = new Vector() } ')");
            columnsPre.get(timeRef).add(bIndent+"h(' { v_" + timeRef + ".record(&t) } ')");
            columnsPre.get(timeRef).add(bIndent+"if self.abs_tol is None or self.rel_tol is None:\n");
            columnsPre.get(timeRef).add(bIndent+"    h.v_" + timeRef + ".resize((h.tstop * h.steps_per_ms) + 1)");

            columnsPost0.get(timeRef).add(bIndent+"py_v_" + timeRef + " = [ t/1000 for t in h.v_" + timeRef + ".to_python() ]  # Convert to Python list for speed...");

            columnsPostTraces.get(timeRef).add(bIndent+"    f_" + timeRef + "_f2.write('%f'% py_v_" + timeRef + "[i])  # Save in SI units...");


            HashMap<String, ArrayList<String>> writingVariables = new HashMap<String, ArrayList<String>>();

            for(Component ofComp : simCpt.getAllChildren())
            {
                if(ofComp.getTypeName().equals("OutputFile"))
                {
                    String outfileId = ofComp.getID().replaceAll("[\\s\\[\\]]", "_");
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
                    if (writingVariables.get(outfileId) == null) {
                        writingVariables.put(outfileId, new ArrayList<String>());
                    }

                    //columnsPostTraces.get(outfileId).add(bIndent+"    f_" + outfileId + "_f2.write('%e\\t'% py_v_" + timeRef + "[i] ");
                    columnsPostTraces.get(outfileId).add(bIndent+"    f_" + outfileId + "_f2.write('%e\\t");
                    writingVariables.get(outfileId).add("py_v_" + timeRef + "[i], ");

                    ArrayList<String> colIds = new ArrayList<String>();

                    for(Component colComp : ofComp.getAllChildren())
                    {
                        if(colComp.getTypeName().equals("OutputColumn"))
                        {
                            /* replace white spaces, [, ] to _ */
                            String colId = colComp.getID().replaceAll("[\\s\\[\\]]", "_") + "_" + outfileId;
                            while(colIds.contains(colId))
                            {
                                colId += "_";
                            }
                            colIds.add(colId);
                            String quantity = colComp.getStringValue("quantity");
                            String scale = "1";

                            LEMSQuantityPathNeuron lqp = new LEMSQuantityPathNeuron(quantity, scale, targetComp, compMechNamesHoc, popsOrComponents, compIdsVsCells, hocRefsVsInputs, lems);

                            //System.out.println("lqp: "+lqp);

                            columnsPre.get(outfileId).add(bIndent+"# Column: " + lqp.getQuantity());
                            columnsPre.get(outfileId).add(bIndent+"h(' objectvar v_" + colId + " ')");
                            columnsPre.get(outfileId).add(bIndent+"h(' { v_" + colId + " = new Vector() } ')");
                            columnsPre.get(outfileId).add(bIndent+"h(' { v_" + colId + ".record(&" + lqp.getNeuronVariableReference() + ") } ')");
                            columnsPre.get(outfileId).add(bIndent+"if self.abs_tol is None or self.rel_tol is None:\n");
                            columnsPre.get(outfileId).add(bIndent+"    h.v_" + colId + ".resize((h.tstop * h.steps_per_ms) + 1)");

                            float conv = NRNUtils.getNeuronUnitFactor(lqp.getDimension().getName());
                            if (lqp.getDimension().getName().equals("conductanceDensity"))
                            {
                                conv = conv = NRNUtils.getNeuronUnitFactor("conductanceDensity_hoc");
                            }
                            String factor = (conv == 1) ? "" : " / " + conv;

                            columnsPost0.get(outfileId).add(bIndent+
                                    "py_v_" + colId + " = [ float(x " + factor + ") for x in h.v_" + colId + ".to_python() ]  # Convert to Python list for speed, variable has dim: " + lqp.getDimension().getName());

                            /*columnsPostTraces.get(outfileId).add(
                                    " + '%e\\t'%(py_v_" + colId + "[i]) ");*/

                            columnsPostTraces.get(outfileId).add("%e\\t");
                            writingVariables.get(outfileId).add("py_v_" + colId + "[i], ");

                        }
                    }
                }
                if(ofComp.getTypeName().equals("EventOutputFile"))
                {
                    String outfileId = ofComp.getID().replaceAll("[\\s\\[\\]]", "_");
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
                    columnsPre.get(outfileId).add(bIndent+"h(' objectvar " + spikeVecName + ", t_" + spikeVecName + " ')");
                    columnsPre.get(outfileId).add(bIndent+"h(' { " + spikeVecName + " = new Vector() } ')");
                    columnsPre.get(outfileId).add(bIndent+"h(' { t_" + spikeVecName + " = new Vector() } ')");

                    columnsPre.get(outfileId).add(bIndent+"h(' objref "+ncName+", nil ')");

                    columnsPostSpikes.get(outfileId).add(bIndent+"h(' objref "+ncName+" ')");

                    columnsPostSpikes.get(outfileId).add(bIndent+"spike_ids = h." + spikeVecName + ".to_python()  ");
                    columnsPostSpikes.get(outfileId).add(bIndent+"spike_times = h.t_" + spikeVecName + ".to_python()");
                    columnsPostSpikes.get(outfileId).add(bIndent+"for i, id in enumerate(spike_ids):");
                    columnsPostSpikes.get(outfileId).add(bIndent+"    # Saving in format: "+eofFormat);
                    if (eofFormat.equals(EventWriter.FORMAT_TIME_ID))
                    {
                        columnsPostSpikes.get(outfileId).add(bIndent+"    f_" + outfileId + "_f2.write(\"%s\\t%i\\n\"%(spike_times[i]/1000.0,id))");
                    }
                    else if (eofFormat.equals(EventWriter.FORMAT_ID_TIME))
                    {
                        columnsPostSpikes.get(outfileId).add(bIndent+"    f_" + outfileId + "_f2.write(\"%i\\t%s\\n\"%(id,spike_times[i]/1000.0))");
                    }


                    for(Component colComp : ofComp.getAllChildren())
                    {
                        if(colComp.getTypeName().equals("EventSelection"))
                        {
                            String colId = colComp.getID().replaceAll("[\\s\\[\\]]", "_") + "_" + outfileId;
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
                            Component comp = this.popIdsVsComps.get(srcCellPop);

                            if (comp.getComponentType().isOrExtends(NeuroMLElements.BASE_SPIKE_SOURCE_COMP_TYPE) ||
                                comp.getComponentType().isOrExtends(NeuroMLElements.BASE_VOLT_DEP_CURR_SRC_SPIKING_COMP_TYPE))
                            {

                                String mechName = NRNUtils.getMechanismName(comp, srcCellPop);

                                columnsPre.get(outfileId).add(bIndent+"# It's a spike source, will listen to "+mechName+"..." );

                                columnsPre.get(outfileId).add(bIndent+"h(' "+ncName+" = new NetCon("+mechName+"["+srcCellNum+"], nil) ')");

                                columnsPre.get(outfileId).add(bIndent+"h(' { "+ncName+".record(t_"+spikeVecName+", "+spikeVecName+", "+id+") } ')");
                            }
                            else
                            {
                                if(srcCell != null)
                                {
                                    NamingHelper nh0 = new NamingHelper(srcCell);
                                    srcSecName = String.format("a_%s[%s].%s", srcCellPop, srcCellNum, nh0.getNrnSectionName(srcCell.getMorphology().getSegment().get(0)));

                                    if (getMembraneProperties(srcCell).getSpikeThresh().isEmpty())
                                    {
                                        threshold = 0;
                                    }
                                    else
                                    {
                                        SpikeThresh st = getMembraneProperties(srcCell).getSpikeThresh().get(0);
                                        if (!st.getSegmentGroup().equals(NeuroMLElements.SEGMENT_GROUP_ALL))
                                        {
                                            throw new NeuroMLException("Cannot yet handle <spikeThresh> when it is not on segmentGroup all");
                                        }
                                        threshold = NRNUtils.convertToNeuronUnits(st.getValue(), lems);
                                    }
                                }
                                else
                                {
                                    srcSecName = srcCellPop + "[" + srcCellNum + "]";
                                    Component preComp = popIdsVsComps.get(srcCellPop);

                                    threshold = NRNUtils.getThreshold(preComp, lems);
                                }

                                columnsPre.get(outfileId).add(bIndent+"# Column: " + select+" ("+id+") "+srcSecName);
                                columnsPre.get(outfileId).add(bIndent+"h(' "+srcSecName+" { "+ncName+" = new NetCon(&v(0.5), nil, "+threshold+", 0, 1) } ')");

                                columnsPre.get(outfileId).add(bIndent+"h(' { "+ncName+".record(t_"+spikeVecName+", "+spikeVecName+", "+id+") } ')");
                            }

                        }
                    }
                }
            }

            for(String f : outfiles.keySet())
            {
                addComment(main, "######################   File to save: " + outfiles.get(f)+" ("+f+")", bIndent);
                for(String col : columnsPre.get(f))
                {
                    main.append(col + "\n");
                }
                main.append("\n");
            }

            main.append(bIndent+"self.initialized = False\n\n");
            main.append(bIndent+"self.sim_end = -1 # will be overwritten\n\n");

            main.append(bIndent+"setup_end = time.time()\n");
            main.append(bIndent+"self.setup_time = setup_end - self.setup_start\n");
            //setup_time = save_end - self.sim_end
            main.append(bIndent+"print(\"Setting up the network to simulate took %f seconds\"%(self.setup_time))\n\n");


            if(!nogui)
            {
                main.append(bIndent+"h.nrncontrolmenu()\n\n\n");
            }

            main.append("    def run(self):\n\n");

            main.append(bIndent+"self.initialized = True\n");
            main.append(bIndent+"sim_start = time.time()\n");

            main.append(bIndent+"if self.abs_tol is not None and self.rel_tol is not None:\n");
            main.append(bIndent+"    print(\"Running a simulation of %sms (cvode abs_tol = %sms, rel_tol = %sms; seed=%s)\" % (h.tstop, self.abs_tol, self.rel_tol, self.seed))\n");
            main.append(bIndent+"else:\n");
            main.append(bIndent+"    print(\"Running a simulation of %sms (dt = %sms; seed=%s)\" % (h.tstop, h.dt, self.seed))\n\n");

            main.append(bIndent+"try:\n");
            main.append(bIndent+"    h.run()\n");
            main.append(bIndent+"except Exception as e:\n");
            main.append(bIndent+"    print(\"Exception running NEURON: %s\" % (e))\n");
            if(nogui)
            {
                main.append(bIndent+"    quit()\n\n\n");
            }
            else
            {
                main.append(bIndent+"    return\n\n\n");
            }

            main.append(bIndent+"self.sim_end = time.time()\n");
            main.append(bIndent+"self.sim_time = self.sim_end - sim_start\n");
            main.append(bIndent+"print(\"Finished NEURON simulation in %f seconds (%f mins)...\"%(self.sim_time, self.sim_time/60.0))\n\n");

            main.append(bIndent+"try:\n");
            main.append(bIndent+"    self.save_results()\n");
            main.append(bIndent+"except Exception as e:\n");
            main.append(bIndent+"    print(\"Exception saving results of NEURON simulation: %s\" % (e))\n");
            if(nogui)
            {
                main.append(bIndent+"    quit()\n\n\n");
            }
            else
            {
                main.append(bIndent+"    return\n\n\n");
            }

            main.append("    def advance(self):\n\n");
            main.append(bIndent+"if not self.initialized:\n");
            main.append(bIndent+"    h.finitialize()\n");
            main.append(bIndent+"    self.initialized = True\n\n");
            main.append(bIndent+"h.fadvance()\n\n\n");

            main.append("    ###############################################################################\n");
            main.append("    # Hash function to use in generation of random value\n");
            main.append("    # This is copied from NetPyNE: https://github.com/Neurosim-lab/netpyne/blob/master/netpyne/simFuncs.py\n");
            main.append("    ###############################################################################\n");
            main.append("    def _id32 (self,obj): \n");
            main.append(bIndent+"return int(hashlib.md5(obj.encode('utf-8')).hexdigest()[0:8],16)  # convert 8 first chars of md5 hash in base 16 to int\n\n\n");

            main.append("    ###############################################################################\n");
            main.append("    # Initialize the stim randomizer\n");
            main.append("    # This is copied from NetPyNE: https://github.com/Neurosim-lab/netpyne/blob/master/netpyne/simFuncs.py\n");
            main.append("    ###############################################################################\n");
            main.append("    def _init_stim_randomizer(self,rand, stimType, gid, seed): \n");
            main.append(bIndent+"#print(\"INIT STIM  %s; %s; %s; %s\"%(rand, stimType, gid, seed))\n");
            main.append(bIndent+"rand.Random123(self._id32(stimType), gid, seed)\n\n\n");

            main.append("    def save_results(self):\n\n");
            main.append(bIndent+"print(\"Saving results at t=%s...\"%h.t)\n\n");
            main.append(bIndent+"if self.sim_end < 0: self.sim_end = time.time()\n\n");

            // main.append("objref SampleGraph\n");
            for(String dg : displayGraphs)
            {
                main.append(bIndent+dg + ".exec_menu(\"View = plot\")\n");
            }
            main.append("\n");

            // Ensure time gets handled first
            Set<String> refs = outfiles.keySet();
            List<String> refList = new ArrayList<String>(refs);
            refList.remove(timeRef);
            refList.add(0, timeRef);

            for(String f : refList)
            {
                addComment(main, "######################   File to save: " + outfiles.get(f)+" ("+f+"). Note, saving in SI units",bIndent);
                for(String col : columnsPost0.get(f))
                {
                    main.append(col + "\n");
                }
                main.append("\n"+bIndent+"f_" + f + "_f2 = open('" + outfiles.get(f) + "', 'w')\n");

                if (columnsPostTraces.containsKey(f))
                {
                    main.append(bIndent+"num_points = len(py_v_time)  # Simulation may have been stopped before tstop...\n\n");
                    main.append(bIndent+"for i in range(num_points):\n");
                    for(String col : columnsPostTraces.get(f))
                    {
                        main.append(col);
                    }

                    if (!f.equals(timeRef)) {
                        main.append("\\n' % (");

                        if (writingVariables.containsKey(f)) {
                            for (String writingVar : writingVariables.get(f)) {
                                main.append(writingVar);
                            }
                        }

                        main.append("))");
                    }

                    //main.append("+ '\\n\')\n");
                    main.append("\n");
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
                main.append(bIndent+"f_" + f + "_f2.close()\n");
                main.append(bIndent+"print(\"Saved data to: " + outfiles.get(f) + "\")\n\n");
            }

            main.append(bIndent+"save_end = time.time()\n");
            main.append(bIndent+"save_time = save_end - self.sim_end\n");
            main.append(bIndent+"print(\"Finished saving results in %f seconds\"%(save_time))\n\n");

            if (target.reportFile!=null)
            {
                main.append(bIndent+"self.report_file.write('StartTime=%s\\n'%datetime.datetime.fromtimestamp(self.setup_start).strftime('%Y-%m-%d %H:%M:%S'))\n");
                main.append(bIndent+"self.report_file.write('SetupTime=%s\\n'%self.setup_time)\n");
                main.append(bIndent+"self.report_file.write('RealSimulationTime=%s\\n'%self.sim_time)\n");
                main.append(bIndent+"self.report_file.write('SimulationSaveTime=%s\\n'%save_time)\n");
                main.append(bIndent+"self.report_file.close()\n\n");
                main.append(bIndent+"print(\"Saving report of simulation to %s\"%('"+target.reportFile+"'))\n\n");
            }

            main.append(bIndent+"print(\"Done\")\n\n");
            if(nogui)
            {
                main.append(bIndent+"quit()\n\n\n");
            }


            main.append("if __name__ == '__main__':\n\n");

            main.append("    ns = NeuronSimulation(tstop="+len+", dt="+dt+", seed="+seed+", abs_tol="+abs_tol+", rel_tol="+rel_tol+")\n\n");

            main.append("    ns.run()\n\n");

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

        for(int i=0; i<explicitInputs.size();i++)
        {
            Component explInput = explicitInputs.get(i);
            HashMap<String, Component> inputReference = explInput.getRefComponents();

            Component inputComp = inputReference.get("input");

            String safeName = NRNUtils.getSafeName(inputComp.getID());
            String inputName = explInput.getTypeName() + "_" + safeName;
            generateModForComp(inputComp);

            String secName = parseInputSecName(explInput);
//            inputName += "_" + popName + "_" + cellNum + "_" + secName.replaceAll("\\[", "").replaceAll("\\]", "").replaceAll("\\.", "_");
            inputName += secName.replaceAll("[\\s\\[\\]]", "_").replaceAll("\\.", "_");

            generateHocForInput(main, explInput, inputComp, inputName, i);
        }
    }


    private void processInputLists(StringBuilder main, Component targetComp) throws LEMSException,
            ContentError, NeuroMLException {

        ArrayList<Component> ils = targetComp.getChildrenAL("inputs", generateAlphabetical);

        if (ils.size()>0)
        {
            main.append(bIndent+"print(\"Processing "+ils.size()+" input lists\")\n\n");

            for(Component inputList : ils) {

                addComment(main, "######################   Input List: "+inputList.id,"        ");
                String info = String.format("Adding input list: %s to %s, with %s inputs of type %s", inputList.id, inputList.getStringValue("population"), inputList.getChildrenAL("inputs").size(), inputList.getStringValue("component"));
                //main.append(bIndent+"print(\""+info+"\")\n\n");

                generateModForComp(inputList.getRefComponents().get("component"));

                ArrayList<Component> cl = inputList.getChildrenAL("inputs");
                for(int i=0; i<cl.size();i++) {
                    Component input = cl.get(i);
                    String inputName = NRNUtils.getSafeName(inputList.getID()) + "_" + input.getID();
                    Component inputComp = inputList.getRefComponents().get("component");
                    generateHocForInput(main, input, inputComp, inputName, i);
                }
            }
            main.append(bIndent+"print(\"Finished processing "+ils.size()+" input lists\")\n\n");
        }
    }

    private void generateHocForInput(StringBuilder main, Component input, Component inputComp, String inputName, int index) throws ContentError,
            NeuroMLException, ParseError, LEMSException {

        String nrnSection = parseInputSecName(input);
        float fractionAlong = parseFractionAlong(input);

        addComment(main, "Adding single input: " + input + (input.hasStringValue("weight") ? ", weight: "+input.getStringValue("weight") : ""), bIndent);

        String safeInputName = NRNUtils.getSafeName(inputComp.getID());
        String inputRef = nrnSection+":"+inputComp.getID();
        hocRefsVsInputs.put(inputRef, inputName);

        if(!inputComp.getComponentType().isOrExtends("timedSynapticInput")) {
            main.append(String.format(bIndent+"h(\"objref %s\")\n", inputName));
            main.append(String.format(bIndent+"h(\"%s { %s = new %s(%s) } \")\n", nrnSection, inputName, safeInputName, fractionAlong));
            if (input.getComponentType().isOrExtends("inputW"))
            {
                main.append(String.format(bIndent+"h(\"%s { %s.weight = %s } \")\n", nrnSection, inputName, input.getStringValue("weight")));
            }
            if (inputComp.getComponentType().isOrExtends(NeuroMLElements.BASE_VOLT_DEP_CURR_SRC_SPIKING_COMP_TYPE))
            {
                main.append(bIndent+"rand = h.Random()\n");
                main.append(bIndent+"self.randoms.append(rand)\n");
                main.append(bIndent+"#print(\"Seeding random generator on "+inputName+" with stim seed %s\"%(self.seed))\n");
                main.append(bIndent+"self._init_stim_randomizer(rand,\""+inputName.substring(0,inputName.lastIndexOf("_"))+"\","+index+", self.seed)\n");
                main.append(bIndent+"rand.negexp(1)\n");
                main.append(bIndent+"h."+inputName+".noiseFromRandom(rand)\n");
                main.append(bIndent+"self.next_spiking_input_id+=1\n");
            }
            main.append("\n");
        } else {
            processTimeDependentLiterals(main, input, inputComp, inputName);
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
        //System.out.println("generateSecName: "+popName+", "+cellNum+", "+segmentId+", "+cell);
        if (cell != null) {
            Segment segment;
            if (segmentId > 0)
            {
                segment = CellUtils.getSegmentWithId(cell, segmentId);
            }
            else
            {
                segment = cell.getMorphology().getSegment().get(0);
            }

            NamingHelper nh0 = new NamingHelper(cell);
            secName = String.format("a_%s[%s].%s", popName, cellNum, nh0.getNrnSectionName(segment));
        } else {
            secName = popName + "[" + cellNum + "]";
        }
        return secName;
    }

    private float parseFractionAlong(Component input) throws ContentError, NeuroMLException {

        String targetString = input.getStringValue("target");
        String popName = Utils.parseCellRefStringForPopulation(targetString);
        String cellId = popIdsVsCellIds.get(popName);
        float fractSeg = input.hasAttribute("fractionAlong") ? Float.parseFloat(input.getAttributeValue("fractionAlong")) : 0.5f;
        Cell cell = compIdsVsCells.get(cellId);
        if (cell==null)
            return fractSeg;

        if (cell.getMorphology().getSegment().size()==1)
            return fractSeg;
        NamingHelper nh0 = new NamingHelper(cell);
        int segmentId = parseSegmentId(input);
        Segment segment;
        if (segmentId > 0)
        {
            segment = CellUtils.getSegmentWithId(cell, segmentId);
        }
        else
        {
            segment = cell.getMorphology().getSegment().get(0);
        }
        String secName = nh0.getNrnSectionName(segment);


        float fract = !CellUtils.hasUnbranchedNonOverlappingInfo(cell) ? fractSeg : (float) CellUtils.getFractionAlongSegGroupLength(cell, secName, segmentId, fractSeg);

        return fract;
    }

    private int parseSegmentId(Component input) throws ContentError {
        return input.hasAttribute("segmentId") ? Integer.parseInt(input.getAttributeValue("segmentId")) : 0;
    }

    boolean timeDepLiteralHelperMethodAdded = false;
    ArrayList<String> createdTimedInputs = new ArrayList<String>();

    private void processTimeDependentLiterals(StringBuilder main, Component input, Component inputComp, String inputList)
            throws ContentError, NeuroMLException, ParseError, LEMSException {

        // TODO Auto-generated method stub
        String inputListName = NRNUtils.getSafeName(inputList);
        float weight = input.hasStringValue("weight") ? Float.parseFloat(input.getStringValue("weight")) : 1;

        Component synapse = inputComp.getRefComponents().get("synapse");
        String synSafeName = NRNUtils.getSafeName(synapse.getID());
        String synFullName =  "self."+inputListName + "_" + synSafeName+"_"+input.getID();

        addComment(main, "Generating event source for input " + input +", comp "+inputComp+", weight: "+weight, bIndent);
        addComment(main, "Name: " + synFullName, bIndent);

        main.append(String.format(bIndent+"%s = h.%s(%s, sec=h.%s) # Synapse of type %s on %s\n",
                                        synFullName,
                                        synSafeName,
                                        parseFractionAlong(input),
                                        parseInputSecName(input),
                                        synapse.getID(),
                                        parseInputSecName(input)));

        if (!timeDepLiteralHelperMethodAdded)
        {
            String helperFunc = bIndent+"# Helper method for creating NetStim to emit at time tstim\n"+bIndent+"def singleNetStimT(tstim):\n"+bIndent+"\tn=h.NetStim()\n"+bIndent+"\tn.number = 1\n"+bIndent+"\tn.start=tstim\n"+bIndent+"\treturn n\n";
            //if (main.indexOf("def singleNetStimT(tstim):\n\tn=h.NetStim()\n\tn.number = 1\n\tn.start=tstim\n\treturn n\n"))
            main.append(helperFunc);
            timeDepLiteralHelperMethodAdded = true;
        }
        List<String> spkTimes = new ArrayList<String>();
        for(Component spk : inputComp.getComponents()) {
            float spkTime = NRNUtils.convertToNeuronUnits(spk.getAttributeValue("time"), lems);
            spkTimes.add(Float.toString(spkTime));
        }
        String synInputName = String.format("syn_input_netstims_%s", inputComp.getID());
        String stimName = String.format("self.%s", synInputName);

        //stimName = String.format("%s_stims", inputName);
        if (!createdTimedInputs.contains(synInputName))
        {
            main.append(String.format(bIndent+"%s = [singleNetStimT(t) for t in %s]\n", stimName, spkTimes));
            createdTimedInputs.add(synInputName);
        }
        main.append(String.format(bIndent+"self.%s_netCons_%s = [h.NetCon(s, %s, 0, 0, %s) for s in %s] \n\n", inputListName, input.getID(), synFullName, weight, stimName));

    }

    public HashMap<String, Integer> modWarnings = new HashMap<String, Integer>();

    private void generateModForComp(Component comp) throws LEMSException, ContentError {
        if(comp.getComponentType().isOrExtends("timedSynapticInput")) {
            // timedSynapticInput leverages netstim, so no mod generation needed
            // TODO: probably all "literal" time dependency should be implemented this way
            comp = comp.getRefComponents().get("synapse");
        }
        if(modWritten.containsKey(comp.getID()))
        {
            int warns = modWarnings.containsKey(comp.getID()) ? modWarnings.get(comp.getID()) : 0;
            if (warns<=5)
            {
                E.info("-- Mod file for: " + comp.getID() + " has already been created"+(warns==5?" (supressing further warnings)":""));
                modWarnings.put(comp.getID(),warns+1);
            }
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
            E.warning("-- Mod file for: " + comp.getID() + " has already been written");
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
            cell = Utils.getCellFromComponent(cellComponent, lems);
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
            bpComp = cellComponent.quietGetChild("biophysicalProperties");
            if (bpComp==null)
            {
                bpComp = lems.getComponent(bp.getId());
            }
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
                    if (channelDensity.getTypeName().equals("channelDensity") ||
                        channelDensity.getTypeName().equals("channelDensityNonUniform")||
                        channelDensity.getTypeName().equals("channelDensityVShift")) {
                        ChannelConductanceOption option = ChannelConductanceOption.FIXED_REVERSAL_POTENTIAL;
                        option.erev = NRNUtils.convertToNeuronUnits((float)channelDensity.getParamValue("erev").getDoubleValue(), "voltage");

                        writeModFile(channelDensity.getRefHM().get("ionChannel"), option);
                    }
                    else if (channelDensity.getTypeName().equals("channelDensityNernst") || channelDensity.getTypeName().equals("channelDensityNonUniformNernst") ){
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
                    else if (channelDensity.getTypeName().equals("channelDensityGHK2")  ){
                        ChannelConductanceOption option = ChannelConductanceOption.USE_GHK2;
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

        LinkedHashMap<String, LinkedHashMap<String, String>> paramMappings = new LinkedHashMap<String, LinkedHashMap<String, String>>();

        if(comp.getComponentType().isOrExtends(NeuroMLElements.BASE_CELL_COMP_TYPE))
        {
            LinkedHashMap<String, String> paramMappingsComp = new LinkedHashMap<String, String>();
            paramMappings.put(comp.getUniqueID(), paramMappingsComp);
        }

        blockUnits.append(NRNUtils.generalUnits);

        ArrayList<String> locals = new ArrayList<String>();

        boolean hasCaDependency = false;
        boolean hasVShift = false;
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

                        for(Requirement r : child2.getComponentType().getRequirements())
                        {
                            if(r.getName().equals(NRNUtils.vShift))
                            {
                                hasVShift = true;
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
                else if(condOption.equals(ChannelConductanceOption.USE_GHK2))
                {

                    blockFunctions.append(NRNUtils.ghk2FunctionDefs);
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

            blockNeuron.append("\nRANGE gion");
            blockNeuron.append("\nRANGE i__" + mechName + " : a copy of the variable for current which makes it easier to access from outside the mod file");

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
            else if(condOption.equals(ChannelConductanceOption.USE_GHK2))
            {
                blockNeuron.append("\nRANGE gmax                              : Will be changed when ion channel mechanism placed on cell!\n");
                blockParameter.append("\ngmax = 0  (S/cm2)                       : Will be changed when ion channel mechanism placed on cell!\n");
                blockParameter.append("\nki=.001 (mM)\n");
                blockNeuron.append("\nRANGE cai\n");
                blockNeuron.append("RANGE cao\n");
                //blockAssigned.append("cai (mM)\n");
                //blockAssigned.append("cao (mM)\n");
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
            blockAssigned.append("i__"+mechName+" (mA/cm2)\n");
            blockAssigned.append("diam (um)                               : Added to facilitate access to section's diam\n");
            blockAssigned.append("area (um2)                              : Added to facilitate access to section's area\n");

            blockParameter.append(NeuroMLElements.CONC_MODEL_SURF_AREA + " "+NRNUtils.getNeuronUnit("area")+"\n");

            // This works for ca -> iCa & ca2 -> iCa2, but should be more general...
            String totalCaCurrent = "i"+ion.replaceAll("ca", "Ca");

            blockParameter.append(totalCaCurrent + " "+NRNUtils.getNeuronUnit("current")+"\n");

            ratesMethod.append(NeuroMLElements.CONC_MODEL_SURF_AREA + " = (1e-08)*(area)   : "
                               +NeuroMLElements.CONC_MODEL_SURF_AREA+" has units "+NRNUtils.getNeuronUnit("area")
                               +", area (built in to NEURON) is in um^2...\n\n");

            ratesMethod.append(totalCaCurrent + " = (1e6) * (-1 * i"+ion+" * " + NeuroMLElements.CONC_MODEL_SURF_AREA
                    + ") :   "+totalCaCurrent+" has units "+NRNUtils.getNeuronUnit("current")+" ; i"+ion+" (built in to NEURON) has units (mA/cm2)...\n\n");

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

        if(comp.getComponentType().isOrExtends("baseRateUnit"))
        {
            blockNeuron.append("? Add pointer for incoming current\n");
            blockNeuron.append("THREADSAFE\n");
            blockNeuron.append("POINTER isyn_in\n\n");
            blockAssigned.append("? Pointer for incoming current\n");
            blockAssigned.append("isyn_in (nA)\n\n");

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
                blockAssigned.append("i__"+mechName+" (mA/cm2)\n");
            }
            else
            {
                blockAssigned.append("e" + species + " (mV)\n");
                blockAssigned.append("i" + species + " (mA/cm2)\n");
                blockAssigned.append("i__"+mechName+" (mA/cm2)\n");
            }
            blockAssigned.append("\n");
            if(hasCaDependency)
            {
                blockAssigned.append("cai (mM)\n\n");
                blockAssigned.append("cao (mM)\n\n");

                locals.add("caConc");
                ratesMethod.append("caConc = cai\n\n");
            }
            if(hasVShift)
            {

                if(!comp.getComponentType().isOrExtends(NeuroMLElements.ION_CHANNEL_V_SHIFT_TYPE))
                {
                    blockParameter.append("\n"+NRNUtils.vShift + " = 0 "+NRNUtils.getNeuronUnit("voltage")+" ? Will be used in rate expressions\n\n");

                    blockNeuron.append("RANGE "+NRNUtils.vShift + "                            : Can be set externally\n");
                }
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
            else if(condOption.equals(ChannelConductanceOption.USE_GHK2))
            {
                blockBreakpoint.append("gion = gmax * fopen\n\n");
            }
            String species = comp.getTextParam("species");

            // Only for ohmic!!
            if(species == null || species.equals("non_specific"))
            {
                blockBreakpoint.append("i = gion * (v - e)\n");
                blockBreakpoint.append("i__" + mechName + " = -1 * i  : set this variable to the current also - note -1 as channel current convention for LEMS used!\n");
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
                else if(condOption.equals(ChannelConductanceOption.USE_GHK2))
                {
                    blockBreakpoint.append("i" + species + " = gion * ghk2(v, cai, cao)\n");
                }
                blockBreakpoint.append("i__" + mechName + " =  -1 * i" + species + " : set this variable to the current also - note -1 as channel current convention for LEMS used!\n");
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

                    ////////////blockBreakpoint.append("\n" + NRNUtils.V_COPY_PREFIX + NRNUtils.NEURON_VOLTAGE + " = " + NRNUtils.NEURON_VOLTAGE);

                    if(comp.getComponentType().isOrExtends(NeuroMLElements.BASE_CELL_CAP_COMP_TYPE))
                    {
                        blockBreakpoint.append("\ni = " + NRNUtils.getStateVarName(NRNUtils.NEURON_VOLTAGE) + " * C");
                    }
                    else if(comp.getComponentType().isOrExtends(NeuroMLElements.BASE_PYNN_CELL))
                    {
                        blockBreakpoint.append("\ni = " + NRNUtils.getStateVarName(NRNUtils.NEURON_VOLTAGE) + " * cm");
                    }
                    else
                    {
                        blockBreakpoint.append("\ni = " + NRNUtils.getStateVarName(NRNUtils.NEURON_VOLTAGE) + "");
                    }
                }
            }
        }

        if (blockInitial.indexOf("random")>0 ||
            blockNetReceive.indexOf("random")>0 ||
            ratesMethod.indexOf("random")>0||
            blockBreakpoint.indexOf("random")>0)
        {
            blockFunctions.append(NRNUtils.randomFunctionDefs);
            blockNeuron.append(": Based on netstim.mod\nTHREADSAFE : only true if every instance has its own distinct Random\n" +
                               "POINTER donotuse");
            blockAssigned.append("donotuse");
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

    private void parseOnStart(Component comp, String prefix, StringBuilder blockInitial, StringBuilder blockInitial_v, StringBuilder blockNetReceive, LinkedHashMap<String, LinkedHashMap<String, String>> paramMappings, Lems lems)
            throws LEMSException
    {

        LinkedHashMap<String, String> paramMappingsComp = paramMappings.get(comp.getUniqueID());

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

    private void parseOnCondition(Component comp, String prefix, StringBuilder blockBreakpoint, StringBuilder blockNetReceive, LinkedHashMap<String, LinkedHashMap<String, String>> paramMappings, int conditionFlag) throws ContentError
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
                    boolean hasEvent = oc.getEventOuts().size()>=1;

                    if (! (comp.getComponentType().isOrExtends(NeuroMLElements.BASE_SPIKE_SOURCE_COMP_TYPE) ||
                           comp.getComponentType().isOrExtends(NeuroMLElements.BASE_VOLT_DEP_CURR_SRC_SPIKING_COMP_TYPE)))
                    {
                        blockBreakpoint.append("if (" + NRNUtils.checkForStateVarsAndNested(cond, comp, paramMappings) + ") {");
                        for(StateAssignment sa : oc.getStateAssignments())
                        {
                            blockBreakpoint.append("\n    " + prefix + NRNUtils.getStateVarName(sa.getStateVariable().getName()) + " = "
                                    + NRNUtils.checkForStateVarsAndNested(sa.getValueExpression(), comp, paramMappings) + " ? standard OnCondition\n");
                        }
                        blockBreakpoint.append("}\n\n");
                    }
                    else
                    {
                        if (!hasEvent)
                        {
                            blockBreakpoint.append("if (" + NRNUtils.checkForStateVarsAndNested(cond, comp, paramMappings) + ") {");
                            for(StateAssignment sa : oc.getStateAssignments())
                            {
                                blockBreakpoint.append("\n    " + prefix + NRNUtils.getStateVarName(sa.getStateVariable().getName()) + " = "
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
                                        + ", satisfied in " + comp.getTypeName() + " at time: %g, v: %g\\n\", t, v)\n");
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
                                            String pm = paramMappings.toString();
                                            if (pm.length()>MAX_LENGTH_LINE_MOD_FILE-10)
                                                pm = pm.substring(0,MAX_LENGTH_LINE_MOD_FILE-10)+"...";
                                            blockNetReceive.append("\n    : paramMappings are: "+pm+"\n");
                                            blockNetReceive.append("    : state_discontinuity(" + NRNUtils.checkForStateVarsAndNested(sa.getStateVariable().getName(), childComp, paramMappings) + ", "
                                                    + NRNUtils.checkForStateVarsAndNested(sa.getValueExpression(), childComp, paramMappings) + ")\n");
                                            blockNetReceive.append("    " + NRNUtils.checkForStateVarsAndNested(sa.getStateVariable().getName(), childComp, paramMappings) + " = "
                                                    + NRNUtils.checkForStateVarsAndNested(sa.getValueExpression(), childComp, paramMappings) + "\n");
                                        }
                                    }
                                }

                                // Particularly important for <doubleSynapse>
                                for(Component childComp2 : childComp.getAllChildren())
                                {
                                    if(childComp2.getComponentType().getDynamics()!=null)
                                    {
                                        blockNetReceive.append("\n    : Child2: "+childComp2+"\n");
                                        for(OnEvent oe : childComp2.getComponentType().getDynamics().getOnEvents())
                                        {
                                            if(oe.getPortName().equals(NeuroMLElements.SYNAPSE_PORT_IN))
                                            {
                                                for(StateAssignment sa : oe.getStateAssignments())
                                                {
                                                    String pm = paramMappings.toString();
                                                    if (pm.length()>MAX_LENGTH_LINE_MOD_FILE-10)
                                                        pm = pm.substring(0,MAX_LENGTH_LINE_MOD_FILE-10)+"...";
                                                    blockNetReceive.append("\n    : paramMappings are: "+pm+"\n");
                                                    blockNetReceive.append("    : state_discontinuity(" + NRNUtils.checkForStateVarsAndNested(sa.getStateVariable().getName(), childComp2, paramMappings) + ", "
                                                            + NRNUtils.checkForStateVarsAndNested(sa.getValueExpression(), childComp2, paramMappings) + ")\n");
                                                    blockNetReceive.append("    " + NRNUtils.checkForStateVarsAndNested(sa.getStateVariable().getName(), childComp2, paramMappings) + " = "
                                                            + NRNUtils.checkForStateVarsAndNested(sa.getValueExpression(), childComp2, paramMappings) + "\n");
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            if(debug)
                            {
                                //int y = 9;
                                //if (comp.getTypeName().indexOf("Syn")<0 && !comp.getTypeName().equals("spikeGenerator"))
                                //blockNetReceive.append("    printf(\"End Condition (" + NRNUtils.checkForStateVarsAndNested(cond, comp, paramMappings) + "), " + conditionFlag
                                //        + ", satisfied in " + comp.getTypeName() + " at time: %g, v: %g, isi: %g, tnext: %g\\n\", t, v, isi, tnext)\n");
                            }

                            blockNetReceive.append("\n    net_event(t)\n");
                            blockNetReceive.append("    WATCH (" + NRNUtils.checkForStateVarsAndNested(cond, comp, paramMappings) + ") " + conditionFlag + "\n");
                            blockNetReceive.append("\n}\n");
                        }

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

                        if(sa.getStateVariable().getName().equals(NRNUtils.NEURON_VOLTAGE))
                        {
                            blockNetReceive.append("\n    " + prefix + NRNUtils.getStateVarName(sa.getStateVariable().getName()) + " = 0 : Setting rate of change of v to 0\n");

                        }
                    }
                    blockNetReceive.append("}\n");

                }
                conditionFlag++;
            }
        }

        for(Component childComp : comp.getAllChildren())
        {
            String prefixNew = getPrefix(childComp, prefix);

            if(!comp.getComponentType().isOrExtends(NeuroMLElements.SPIKE_ARRAY)) { // since this will be hard coded as a more efficient impl, see parseOnStart
                //parseParameters(childComp, prefixNew, prefix, rangeVars, stateVars, blockNeuron, blockParameter, paramMappings);

                //blockNetReceive.append("\n    : Parsing child: "+childComp+" of "+comp+"\n");
                parseOnCondition(childComp, prefixNew, blockBreakpoint, blockNetReceive, paramMappings, conditionFlag);
            }
            else
            {
                //blockNetReceive.append("\n    : Not parsing child: "+childComp+" of "+comp+"\n");
            }
        }
    }

    private void parseOnEvent(Component comp, StringBuilder blockNetReceive, LinkedHashMap<String, LinkedHashMap<String, String>> paramMappings) throws ContentError
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
                        String pm = paramMappings.toString();
                        if (pm.length()>MAX_LENGTH_LINE_MOD_FILE-10)
                            pm = pm.substring(0,MAX_LENGTH_LINE_MOD_FILE-10)+"...";
                        blockNetReceive.append("\n: paramMappings . : "+pm+"\n");
                        blockNetReceive.append(": state_discontinuity(" + NRNUtils.checkForStateVarsAndNested(sa.getStateVariable().getName(), comp, paramMappings) + ", "
                                + NRNUtils.checkForStateVarsAndNested(sa.getValueExpression(), comp, paramMappings) + ") : From "+comp.id+"\n");
                        blockNetReceive.append("" + NRNUtils.checkForStateVarsAndNested(sa.getStateVariable().getName(), comp, paramMappings) + " = "
                                + NRNUtils.checkForStateVarsAndNested(sa.getValueExpression(), comp, paramMappings) + " : From "+comp.id+"\n");
                    }
                }
            }
        }
        for(Component childComp : comp.getAllChildren())
        {
            if(childComp.getComponentType().isOrExtends(NeuroMLElements.BASE_PLASTICITY_MECHANISM_COMP_TYPE) ||
               comp.getComponentType().isOrExtends(NeuroMLElements.DOUBLE_SYNAPSE_COMP_TYPE))
            {
                parseOnEvent(childComp, blockNetReceive, paramMappings);
            }
        }
    }

    private void parseParameters(Component comp, String prefix, String prefixParent, ArrayList<String> rangeVars, ArrayList<String> stateVars, StringBuilder blockNeuron,
            StringBuilder blockParameter, LinkedHashMap<String, LinkedHashMap<String, String>> paramMappings) throws LEMSException
    {

        LinkedHashMap<String, String> paramMappingsComp = paramMappings.get(comp.getUniqueID());

        if(paramMappingsComp == null)
        {
            paramMappingsComp = new LinkedHashMap<String, String>();
            paramMappings.put(comp.getUniqueID(), paramMappingsComp);
        }

        for (Property prop: comp.getComponentType().getPropertys())
        {
            if(comp.getComponentType().isOrExtends(NeuroMLElements.GAP_JUNCTION) ||
               comp.getComponentType().isOrExtends(NeuroMLElements.BASE_GRADED_SYNAPSE)||
               (comp.getComponentType().isOrExtends(NeuroMLElements.BASE_POINT_CURR_COMP_TYPE) &&
                !comp.getComponentType().isOrExtends(NeuroMLElements.BASE_SYNAPSE_COMP_TYPE))||
               comp.getComponentType().isOrExtends(NeuroMLElements.BASE_POINT_CURR_DL_COMP_TYPE) ||
               comp.getComponentType().isOrExtends(NeuroMLElements.DOUBLE_SYNAPSE_COMP_TYPE)) // This may be required for other Properties
            {
                String mappedName = prefix + prop.getName();
                rangeVars.add(mappedName);
                paramMappingsComp.put(prop.getName(), mappedName);

                String range = "RANGE " + mappedName;
                while(range.length() < NRNUtils.commentOffset)
                {
                    range = range + " ";
                }

                blockNeuron.append(range + ": property\n");
                float val = NRNUtils.convertToNeuronUnits(Float.parseFloat(prop.getDefaultValue()), Dimension.NO_DIMENSION);
                String valS = val + "";
                if((int) val == val)
                {
                    valS = (int) val + "";
                }
                blockParameter.append("\n" + mappedName + " = " + valS);
            }
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
            String d = "\n" + mappedName + " = " + valS + " " + NRNUtils.getNeuronUnit(pv.getDimensionName());
            while(d.length() < NRNUtils.commentOffset)
            {
                d = d + " ";
            }
            blockParameter.append(d + ": was: "+pv.getDoubleValue() +" (" +pv.getDimensionName()+")");
        }

        for(Exposure exp : comp.getComponentType().getExposures())
        {
            String mappedName = prefix + exp.getName();

            if(!rangeVars.contains(mappedName) && !stateVars.contains(mappedName) && !exp.getName().equals(NRNUtils.NEURON_VOLTAGE))
            {
                rangeVars.add(mappedName);
                paramMappingsComp.put(exp.getName(), mappedName);

                String range = "RANGE " + mappedName;
                while(range.length() < NRNUtils.commentOffset)
                {
                    range = range + " ";
                }
                blockNeuron.append(range + ": exposure\n");

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
            if(!req.getName().equals("v") && !req.getName().equals("temperature") && !req.getName().equals("caConc") && !req.getName().equals("vShift"))
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
        ArrayList<Component> orderedChildren = comp.getAllChildren();

        for(Component childComp : orderedChildren)
        {
            String prefixNew = getPrefix(childComp, prefix);

            if(!comp.getComponentType().isOrExtends(NeuroMLElements.SPIKE_ARRAY)) { // since this will be hard coded as a more efficient impl, see parseOnStart
                parseParameters(childComp, prefixNew, prefix, rangeVars, stateVars, blockNeuron, blockParameter, paramMappings);
            }
        }

        /*if(comp.getComponentType().isOrExtends(NeuroMLElements.BASE_CELL_COMP_TYPE))
        {
            blockNeuron.append("\nRANGE " + NRNUtils.V_COPY_PREFIX + NRNUtils.NEURON_VOLTAGE + "                            : copy of v on section\n");
        }*/

    }

    private void parseStateVars(Component comp, String prefix, ArrayList<String> rangeVars, ArrayList<String> stateVars, StringBuilder blockNeuron, StringBuilder blockParameter,
            StringBuilder blockAssigned, StringBuilder blockState, LinkedHashMap<String, LinkedHashMap<String, String>> paramMappings) throws ContentError
    {

        LinkedHashMap<String, String> paramMappingsComp = paramMappings.get(comp.getUniqueID());

        if(paramMappingsComp == null)
        {
            paramMappingsComp = new LinkedHashMap<String, String>();
            paramMappings.put(comp.getUniqueID(), paramMappingsComp);
        }

        if(comp.getComponentType().hasDynamics())
        {

            for(Regime regime : comp.getComponentType().getDynamics().getRegimes())
            {
                String regimeStateName = NRNUtils.REGIME_PREFIX + regime.name;
                stateVars.add(regimeStateName);
                blockAssigned.append(regimeStateName + " (1)\n");
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
                    blockNeuron.append("\n\nNONSPECIFIC_CURRENT i                   : To ensure v of section follows " + svName + "\n");
                    blockAssigned.append("v (mV)\n");
                    blockAssigned.append("i (nA)                                 : the point process current \n\n");
                    //blockAssigned.append("i (mA/cm2)  : point process current \n\n");

                    //blockAssigned.append(NRNUtils.V_COPY_PREFIX + NRNUtils.NEURON_VOLTAGE + " (mV)\n\n");

                    dim = "(mV/ms)                             : for rate of change of voltage";
                }
                String bounds = "";
                if (comp.getComponentType().isOrExtends(NeuroMLElements.KS_STATE_COMP_TYPE)) {
                    bounds = " FROM 0 TO 1";
                }

                if(sv.getName().equals(NRNUtils.NEURON_VOLTAGE))
                {
                    blockAssigned.append(svName +bounds + " "+ dim + " "+"\n");
                }
                else if(Arrays.asList(NRNUtils.NON_NRN_STATE_VARS).contains(sv.getName()))
                {
                    blockAssigned.append(svName +bounds + " "+ dim + "                      : Not a state variable as far as Neuron's concerned..."+"\n");
                }
                else
                {
                    blockState.append(svName +bounds + " "+ dim + " : dimension: "+sv.getDimension().getName()+"\n");
                }
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

    private void parseTimeDerivs(Component comp, String prefix, ArrayList<String> locals, StringBuilder blockDerivative, StringBuilder blockBreakpoint, StringBuilder blockAssigned,
            StringBuilder ratesMethod, LinkedHashMap<String, LinkedHashMap<String, String>> paramMappings, String ionSpecies) throws ContentError
    {

        StringBuilder ratesMethodFinal = new StringBuilder();

        if(comp.getComponentType().hasDynamics())
        {
            LinkedHashMap<String, String> rateNameVsRateExpr = new LinkedHashMap<String, String>();

            /*
             * First handle "top level" time derivatives on this component. For gate-like
             * components (BASE_GATE_COMP_TYPE) we inline the derivative expression directly
             * into the DERIVATIVE block, i.e. x' = (x_inf - x)/tau, so that NEURON can
             * recognise and apply cnexp. We do not create intermediate rate_* variables
             * for these gates.
             */
            for(TimeDerivative td : comp.getComponentType().getDynamics().getTimeDerivatives())
            {

                String stateVarName = td.getStateVariable().getName();
                boolean isVoltage = stateVarName.equals(NRNUtils.NEURON_VOLTAGE);
                boolean isGateState = comp.getComponentType().isOrExtends(NeuroMLElements.BASE_GATE_COMP_TYPE) && !isVoltage;

                String stateVarToUse = NRNUtils.getStateVarName(stateVarName);
                String rateExpr = NRNUtils.checkForStateVarsAndNested(td.getValueExpression(), comp, paramMappings);

                if(!isVoltage)
                {
                    String line;

                    if(isGateState)
                    {
                        // Inline derivative for gate-style states: x' = (x_inf - x)/tau
                        line = prefix + stateVarToUse + "' = " + rateExpr;
                    }
                    else
                    {
                        // Generic case (non-gate/complex models) retains the rate_* pattern
                        String rateName = NRNUtils.RATE_PREFIX + prefix + stateVarName;
                        String rateUnits = NRNUtils.getDerivativeUnit(td.getStateVariable().getDimension().getName());

                        if(blockAssigned.indexOf("\n" + rateName + " " + rateUnits + "\n") < 0)
                        {
                            blockAssigned.append("\n" + rateName + " " + rateUnits + "\n");
                        }
                        rateNameVsRateExpr.put(rateName, rateExpr);

                        line = prefix + stateVarToUse + "' = " + rateName;
                    }

                    if(comp.getComponentType().isOrExtends(NeuroMLElements.CONC_MODEL_COMP_TYPE) &&
                        stateVarName.equals(NeuroMLElements.CONC_MODEL_CONC_STATE_VAR))
                    {
                        line = line + "\n"+ionSpecies+"i = " + stateVarName;
                    }

                    if(!blockDerivative.toString().contains(line))
                    {
                        blockDerivative.append(line + " \n");
                    }
                }
                else
                {
                    // Voltage derivative is always handled via an intermediate rate_*
                    String rateName = NRNUtils.RATE_PREFIX + prefix + stateVarName;
                    String rateUnits = NRNUtils.getDerivativeUnit(td.getStateVariable().getDimension().getName());
                    if(blockAssigned.indexOf("\n" + rateName + " " + rateUnits + "\n") < 0)
                    {
                        blockAssigned.append("\n" + rateName + " " + rateUnits + "\n");
                    }
                    rateNameVsRateExpr.put(rateName, rateExpr);

                    ratesMethodFinal.append(prefix + NRNUtils.getStateVarName(stateVarName) + " = -1 * " + rateName + "\n");
                }

            }

            /*
             * Now handle regime-specific contributions. For gate components we again inline
             * directly into the DERIVATIVE block and do not introduce rate_* variables.
             * For other components, the existing rate_* aggregation logic is retained.
             */
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
                    String stateVarName = td.getStateVariable().getName();
                    boolean isVoltage = stateVarName.equals(NRNUtils.NEURON_VOLTAGE);
                    boolean isGateState = comp.getComponentType().isOrExtends(NeuroMLElements.BASE_GATE_COMP_TYPE) && !isVoltage;

                    String stateVarToUse = NRNUtils.getStateVarName(stateVarName);

                    if(isGateState)
                    {
                        String rateExpr = NRNUtils.checkForStateVarsAndNested(td.getValueExpression(), comp, paramMappings);
                        String line = prefix + stateVarToUse + "' = " + rateExpr;

                        if(!blockDerivative.toString().contains(line))
                        {
                            blockDerivative.append(line + " \n");
                        }
                    }
                    else
                    {
                        String rateName = NRNUtils.RATE_PREFIX + prefix + stateVarName;
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

                        if(!isVoltage)
                        {
                            String line = prefix + stateVarToUse + "' = " + rateName;

                            if(!blockDerivative.toString().contains(line))
                            {
                                blockDerivative.append(line + " \n");
                            }
                        }
                        else
                        {
                            ratesMethodFinal.append(prefix + NRNUtils.getStateVarName(stateVarName) + " = -1 * " + rateName + "\n"); // //
                        }
                    }
                }
            }

            // Only emit rate_* helper variables for non-gate / complex cases
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

    private void parseDerivedVars(Component comp, String prefix, ArrayList<String> rangeVars, StringBuilder ratesMethod, StringBuilder blockNeuron, StringBuilder blockParameter,
            StringBuilder blockAssigned, StringBuilder blockBreakpoint, LinkedHashMap<String, LinkedHashMap<String, String>> paramMappings) throws ContentError
    {

        LinkedHashMap<String, String> paramMappingsComp = paramMappings.get(comp.getUniqueID());
        if(paramMappingsComp == null)
        {
            paramMappingsComp = new LinkedHashMap<String, String>();
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

            // For some expressions (e.g. conditional derived vars) we still need a
            // generic target block: ion channels -> BREAKPOINT, others -> rates().
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
                    if (!parallelMode)
                    {
                        blockNeuron.append("POINTER " + dv.getName() + ": derived variable as pointer...\n");
                    }
                    else
                    {
                        blockNeuron.append("RANGE " + dv.getName() + ": derived variable; RANGE, not POINTER for Parallel NEURON...\n");
                    }
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

                String assig = prefix + dv.getName() + " " + NRNUtils.getNeuronUnit(dv.dimension);
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

                        //TODO: replace with more thorough check...
                        if (localVar.startsWith("synapse_"))
                        {
                            localVar = localVar.replaceFirst("synapse_", comp.getChild("synapse").getID()+"_");
                        }
                        else if (localVar.startsWith("synapse1_"))
                        {
                            localVar = localVar.replaceFirst("synapse1_", comp.getChild("synapse1").getID()+"_");
                        }
                        else if (localVar.startsWith("synapse2_"))
                        {
                            localVar = localVar.replaceFirst("synapse2_", comp.getChild("synapse2").getID()+"_");
                        }
                        else if (localVar.startsWith("ionChannel_"))
                        {
                            localVar = localVar.replaceFirst("ionChannel_", comp.getChild("ionChannel").getID()+"_");
                        }

                        String globalVar = prefix + localVar;

                        String eqn = globalVar;
                        String comment = "";

                        if(comp.getComponentType().isOrExtends(NeuroMLElements.BASE_RATE_UNIT) && globalVar.toLowerCase().contains("syn"))
                        {
                            eqn = "isyn_in * 3.14159 ? Using this value, which comes from i_cap on the cell via a pointer, for the synaptic current" ;
                        }
                        else if(globalVar.contains("[*]") && globalVar.contains("syn"))
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
                        block.append(prefix + dv.getName() + " = " + eqn + " ? path based, prefix = "+prefix+"\n\n");
                    }
                }

                /*
                 * Decide where to place the derived variable equation.
                 *
                 *  - For top-level ion channel components we still emit derived variables
                 *    (e.g. fopen, gion) in the BREAKPOINT block.
                 *  - For gate components, fcond-style multipliers should be evaluated
                 *    after SOLVE states, so we also place these in BREAKPOINT instead of
                 *    in rates().
                 *  - All other derived variables go into rates().
                 */
                if(comp.getComponentType().isOrExtends(NeuroMLElements.BASE_ION_CHANNEL_COMP_TYPE) ||
                   (comp.getComponentType().isOrExtends(NeuroMLElements.BASE_GATE_COMP_TYPE) && dv.getName().endsWith("fcond")))
                {
                    blockBreakpoint.append(block);
                }
                else
                {
                    ratesMethod.append(block);
                }
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

                String assig = prefix + cdv.getName() + " " + NRNUtils.getNeuronUnit(cdv.dimension);
                while(assig.length() < NRNUtils.commentOffset)
                {
                    assig = assig + " ";
                }

                blockAssigned.append("\n" + assig + ": conditional derived var...\n");

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

    /*
    * Can be used to generate Neuron hoc/mod files for cells (with channels) and synapses
    */
    public void generateFilesForNeuroMLElements(boolean compileMods) throws LEMSException, NeuroMLException, IOException
    {
        boolean foundMods = false;
        for (Component comp: lems.getComponents())
        {
            if (comp.getComponentType().isOrExtends(NeuroMLElements.CELL_COMP_TYPE))
            {
                convertCellWithMorphology(comp);
            }
             // extends baseCell but not Cell
            else if (comp.getComponentType().isOrExtends(NeuroMLElements.BASE_CELL_COMP_TYPE))
            {
                generateModForComp(comp);
                foundMods = true;
            }
            else if (comp.getComponentType().isOrExtends(NeuroMLElements.BASE_SYNAPSE_COMP_TYPE))
            {
                generateModForComp(comp);
                foundMods = true;
            }
            else if (comp.getComponentType().isOrExtends(NeuroMLElements.BASE_ION_CHANNEL_COMP_TYPE))
            {
                generateModForComp(comp);
                foundMods = true;
            }
            else if (comp.getComponentType().isOrExtends(NeuroMLElements.BASE_POINT_CURR_COMP_TYPE))
            {
                generateModForComp(comp);
                foundMods = true;
            }
            else if (comp.getComponentType().isOrExtends(NeuroMLElements.CONC_MODEL_COMP_TYPE))
            {
                generateModForComp(comp);
                foundMods = true;
            }
            // TODO: more..?

        }
        if (compileMods && foundMods)
        {
            E.info("Trying to compile mods in: " + this.getOutputFolder());

            boolean complied = ProcessManager.compileFileWithNeuron(this.getOutputFolder(), true);

            E.info("Success in compiling mods: " + complied);

            if (!complied)
            {

                throw new NeuroMLException("Error compiling mod files in: "+this.getOutputFolder());
            }
        }
    }

    public static void main(String[] args) throws Exception
    {

        MinimalMessageHandler.setVeryMinimal(true);
        E.setDebug(false);

        ArrayList<File> nmlFiles = new ArrayList<File>();
        nmlFiles.add(new File("../NeuroML2/examples/NML2_SingleCompHHCell.nml"));
        nmlFiles.add(new File("../NeuroML2/examples/NML2_SynapseTypes.nml"));
        //nmlFiles.add(new File("../git/BonoClopath2017/NeuroML2/SimpleNet.net.nml"));

        for(File nmlFile : nmlFiles)
        {
            Lems lems = Utils.readNeuroMLFile(nmlFile.getAbsoluteFile()).getLems();
            System.out.println("Loading: "+nmlFile);
            NeuronWriter nw = new NeuronWriter(lems, nmlFile.getParentFile(),"");
            nw.generateFilesForNeuroMLElements(false);
        }
        //System.exit(0);

        ArrayList<File> lemsFiles = new ArrayList<File>();

        //lemsFiles.add(new File("../neuroConstruct/osb/hippocampus/interneurons/WangBuzsaki1996/NeuroML2/LEMS_ComponentType/LEMS_WangBuzsaki.xml"));
        //lemsFiles.add(new File("../neuroConstruct/osb/generic/HindmarshRose1984/NeuroML2/LEMS_Regular_HindmarshRoseNML.xml"));
        //lemsFiles.add(new File("../neuroConstruct/osb/showcase/StochasticityShowcase/NeuroML2/LEMS_Inputs0.xml"));
        //lemsFiles.add(new File("../neuroConstruct/osb/invertebrate/celegans/CElegansNeuroML/CElegans/pythonScripts/c302/examples/LEMS_c302_C1_Oscillator.xml"));

        //lemsFiles.add(new File("../neuroConstruct/osb/cerebellum/cerebellar_golgi_cell/SolinasEtAl-GolgiCell/NeuroML2/LEMS_KAHP_Test.xml"));
        //lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex12_Net2.xml"));
        //lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex16_Inputs.xml"));
        //lemsFiles.add(new File("../neuroConstruct/osb/cerebellum/networks/VervaekeEtAl-GolgiCellNetwork/NeuroML2/LEMS_Pacemaking.xml"));
        //lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex9_FN.xml"));
        lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex5_DetCell.xml"));
        //lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex15_CaDynamics.xml"));
        //lemsFiles.add(new File("../neuroConstruct/osb/cerebral_cortex/networks/IzhikevichModel/NeuroML2/LEMS_2007One.xml"));
        //lemsFiles.add(new File("../org.neuroml.export/src/test/resources/examples/LEMS_SpikePass2.xml"));
        /*
        lemsFiles.add(new File("../neuroConstruct/osb/showcase/StochasticityShowcase/NeuroML2/LEMS_NoisyCurrentInput.xml"));
        lemsFiles.add(new File("../neuroConstruct/osb/showcase/StochasticityShowcase/NeuroML2/LEMS_OUCurrentInput_test.xml"));
        lemsFiles.add(new File("../neuroConstruct/osb/cerebral_cortex/networks/IzhikevichModel/NeuroML2/LEMS_FiveCells.xml"));*/
        //lemsFiles.add(new File("../git/ca1/NeuroML2/channels/test_Cadynamics/NeuroML2/LEMS_test_Ca.xml"));
        //lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex20a_AnalogSynapsesHH.xml"));
        //lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex20_AnalogSynapses.xml"));
        //lemsFiles.add(new File("../NeuroMLlite/neuromllite/LEMS_Sim_ten_cells_spikes_nest.xml"));
        //lemsFiles.add(new File("../NeuroMLlite/examples/test_files/test_inputs/LEMS_InputTest.xml"));
        //lemsFiles.add(new File("../NeuroMLlite/neuromllite/LEMS_Sim_NML2_300_pointneurons.xml"));

        //
        //lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex26_Weights.xml"));
        //lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex19_GapJunctions.xml"));
        //lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex23_Spiketimes.xml"));
        //lemsFiles.add(new File("../neuroConstruct/osb/showcase/NetPyNEShowcase/NeuroML2/LEMS_Spikers.xml"));
         //lemsFiles.add(new File("../OpenCortex/examples/LEMS_SimpleNet.xml"));
        //lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex16_Inputs.xml"));
        //lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex27_MultiSynapses.xml"));
        //lemsFiles.add(new File("../neuroConstruct/osb/generic/hodgkin_huxley_tutorial/Tutorial2/NeuroML2/LEMS_HHTutorial.xml"));
        //lemsFiles.add(new File("../neuroConstruct/osb/cerebral_cortex/neocortical_pyramidal_neuron/SmithEtAl2013-L23DendriticSpikes/NeuroML2/LEMS_L23_Stim.xml"));

        //lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex19a_GapJunctionInstances.xml"));
//
//        lemsFiles.add(new File("../neuroConstruct/osb/cerebral_cortex/multiple/PospischilEtAl2008/NeuroML2/channels/Na/LEMS_Na.xml"));
//        lemsFiles.add(new File("../neuroConstruct/osb/cerebral_cortex/multiple/PospischilEtAl2008/NeuroML2/channels/Kd/LEMS_Kd.xml"));
        lemsFiles.add(new File("../neuroConstruct/osb/cerebral_cortex/networks/ACnet2/neuroConstruct/generatedNeuroML2/LEMS_ACNet2.xml"));
        lemsFiles.add(new File("../git/morphology_include/LEMS_m_in_b_in.xml"));
        lemsFiles.add(new File("../git/morphology_include/LEMS_m_out_b_in.xml"));
        lemsFiles.add(new File("../git/morphology_include/LEMS_m_in_b_out.xml"));

//        lemsFiles.add(new File("../OpenCortex/examples/LEMS_ACNet.xml"));
//
        //lemsFiles.add(new File("../OpenCortex/examples/LEMS_SpikingNet.xml"));
//        lemsFiles.add(new File("../OpenCortex/examples/LEMS_SimpleNet.xml"));
//
//        lemsFiles.add(new File("../neuroConstruct/osb/cerebral_cortex/networks/IzhikevichModel/NeuroML2/LEMS_2007One.xml"));

//        lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex20a_AnalogSynapsesHH.xml"));
        //lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex14_PyNN.xml"));
       // lemsFiles.add(new File("../neuroConstruct/osb/cerebral_cortex/multiple/PospischilEtAl2008/NeuroML2/cells/RS/LEMS_RS.xml"));
        //lemsFiles.add(new File("../git/WilsonCowan/NeuroML2/LEMS_WC_slow.xml"));
        //lemsFiles.add(new File("../git/del-Molino2017/NeuroML/Fig1/LEMS_RateBased_low_baseline.xml"));
          //lemsFiles.add(new File("../neuroConstruct/osb/cerebral_cortex/networks/WilsonCowan/NeuroML2/LEMS_WC_driven.xml"));
          //lemsFiles.add(new File("../neuroConstruct/osb/cerebral_cortex/networks/MejiasEtAl2016/NeuroML2/LEMS_Test.xml"));

//        lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex25_MultiComp.xml"));
//        lemsFiles.add(new File("../neuroConstruct/osb/showcase/NetPyNEShowcase/NeuroML2/LEMS_HybridTut.xml"));
//        lemsFiles.add(new File("../OpenCortex/examples/LEMS_L23TraubDemo_1cells_0conns.xml"));
        //lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex0_IaF.xml"));

        //lemsFiles.add(new File("../neuroConstruct/osb/invertebrate/celegans/CElegansNeuroML/CElegans/pythonScripts/c302/examples/LEMS_c302_C1_Muscles.xml"));
        //lemsFiles.add(new File("../neuroConstruct/osb/invertebrate/celegans/CElegansNeuroML/CElegans/pythonScripts/c302/examples/LEMS_c302_C1_Syns.xml"));

        //lemsFiles.add(new File("../neuroConstruct/osb/cerebral_cortex/networks/ACnet2/neuroConstruct/generatedNeuroML2/LEMS_StimuliTest.xml"));
        //lemsFiles.add(new File("../git/alex-neuroml-test/LEMS_sim.xml"));
        //lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex6_NMDA.xml"));
        //lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex25_MultiComp.xml"));
        /*
        lemsFiles.add(new File("../neuroConstruct/osb/showcase/NetPyNEShowcase/NeuroML2/LEMS_M1.xml"));
        lemsFiles.add(new File("../git/NML2_Test/AOB_mitral_cell/LEMS_Vm_iMC1_cell_1_origin.xml"));

        //lemsFiles.add(new File("../neuroConstruct/osb/cerebral_cortex/neocortical_pyramidal_neuron/L5bPyrCellHayEtAl2011/neuroConstruct/generatedNeuroML2/LEMS_TestL5PC.xml"));


        //lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex7_STP.xml"));

        lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex5_DetCell.xml"));
        lemsFiles.add(new File("../neuroConstruct/osb/cerebellum/cerebellar_granule_cell/GranuleCell/neuroConstruct/generatedNeuroML2/LEMS_GranuleCell.xml"));

        lemsFiles.add(new File("../neuroConstruct/osb/cerebellum/networks/GranCellLayer/neuroConstruct/generatedNeuroML2/LEMS_GranCellLayer.xml"));
        lemsFiles.add(new File("../neuroConstruct/osb/cerebellum/cerebellar_golgi_cell/SolinasEtAl-GolgiCell/NeuroML2/LEMS_Soma_Test_HELPER.xml"));

        lemsFiles.add(new File("../neuroConstruct/osb/cerebral_cortex/networks/Thalamocortical/neuroConstruct/generatedNeuroML2/LEMS_Thalamocortical.xml"));


        lemsFiles.add(new File("../neuroConstruct/osb/showcase/AllenInstituteNeuroML/CellTypesDatabase/models/NeuroML2/LEMS_SomaTest.xml"));
        lemsFiles.add(new File("../neuroConstruct/osb/hippocampus/CA1_pyramidal_neuron/CA1PyramidalCell/neuroConstruct/generatedNeuroML2/LEMS_CA1PyramidalCell.xml"));

        /*
        lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex21_CurrentBasedSynapses.xml"));

        //lemsFiles.add(new File("../neuroConstruct/osb/showcase/AllenInstituteNeuroML/CellTypesDatabase/models/NeuroML2/LEMS_SomaTest.xml"));

        lemsFiles.add(new File("../neuroConstruct/osb/cerebral_cortex/networks/IzhikevichModel/NeuroML2/LEMS_SmallNetwork.xml"));
        lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex20a_AnalogSynapsesHH.xml"));

        lemsFiles.add(new File("../neuroConstruct/osb/cerebral_cortex/neocortical_pyramidal_neuron/L5bPyrCellHayEtAl2011/neuroConstruct/generatedNeuroML2/LEMS_L5bPyrCellHayEtAl2011_LowDt.xml"));

        lemsFiles.add(new File("../neuroConstruct/osb/invertebrate/celegans/muscle_model/NeuroML2/LEMS_NeuronMuscle.xml"));
        lemsFiles.add(new File("../neuroConstruct/osb/cerebral_cortex/multiple/PospischilEtAl2008/NeuroML2/channels/IL/LEMS_IL.nonernst.xml"));

        //lemsFiles.add(new File("../neuroConstruct/osb/cerebellum/networks/Cerebellum3DDemo/neuroConstruct/generatedNeuroML2/LEMS_Cerebellum3DDemo.xml"));


        //lemsFiles.add(new File("../git/neuroml_use_case/LEMS_sim.xml"));




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
                /*

        lemsFiles.add(new File("../neuroConstruct/osb/showcase/NetPyNEShowcase/NeuroML2/chanDens/LEMS_cck.xml"));

        lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex1_HH.xml"));
        lemsFiles.add(new File("../neuroConstruct/osb/cerebellum/cerebellar_granule_cell/GranuleCell/neuroConstruct/generatedNeuroML2/LEMS_GranuleCell.xml")); */

        String testScript = "set -e\n";

        NeuronWriter nw;
        for(File lemsFile : lemsFiles)
        {
            Lems lems = Utils.readLemsNeuroMLFile(lemsFile.getAbsoluteFile()).getLems();
            nw = new NeuronWriter(lems, lemsFile.getParentFile(), lemsFile.getName().replaceAll(".xml", "_nrn.py"));

            //List<File> ff = nw.generateAndRun(false, false, false);
            List<File> ff = nw.generateAndRun(true, true, true, false);
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

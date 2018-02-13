package org.lemsml.export.dlems;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.velocity.VelocityContext;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.lemsml.export.base.ABaseWriter;
import org.lemsml.jlems.core.expression.ParseError;
import org.lemsml.jlems.core.flatten.ComponentFlattener;
import org.lemsml.jlems.core.logging.E;
import org.lemsml.jlems.core.logging.MinimalMessageHandler;
import org.lemsml.jlems.core.run.ConnectionError;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.sim.LEMSException;
import org.lemsml.jlems.core.type.Component;
import org.lemsml.jlems.core.type.ComponentType;
import org.lemsml.jlems.core.type.Constant;
import org.lemsml.jlems.core.type.DerivedParameter;
import org.lemsml.jlems.core.type.Dimension;
import org.lemsml.jlems.core.type.FinalParam;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.core.type.ParamValue;
import org.lemsml.jlems.core.type.Target;
import org.lemsml.jlems.core.type.dynamics.DerivedVariable;
import org.lemsml.jlems.core.type.dynamics.IVisitable;
import org.lemsml.jlems.core.type.dynamics.OnCondition;
import org.lemsml.jlems.core.type.dynamics.OnEvent;
import org.lemsml.jlems.core.type.dynamics.OnStart;
import org.lemsml.jlems.core.type.dynamics.StateAssignment;
import org.lemsml.jlems.core.type.dynamics.StateVariable;
import org.lemsml.jlems.core.type.dynamics.TimeDerivative;
import org.lemsml.jlems.io.util.FileUtil;
import org.lemsml.jlems.io.xmlio.XMLSerializer;
import org.neuroml.export.exceptions.GenerationException;
import org.neuroml.export.exceptions.ModelFeatureSupportException;
import org.neuroml.export.neuron.LEMSQuantityPathNeuron;
import org.neuroml.export.neuron.NRNUtils;
import org.neuroml.export.neuron.NamingHelper;
import org.neuroml.export.utils.Format;
import org.neuroml.export.utils.LEMSQuantityPath;
import org.neuroml.export.utils.Utils;
import org.neuroml.export.utils.support.ModelFeature;
import org.neuroml.export.utils.support.SupportLevelInfo;
import org.neuroml.export.utils.visitors.CommonLangWriter;
import org.neuroml.model.Cell;
import org.neuroml.model.Segment;
import org.neuroml.model.util.CellUtils;
import org.neuroml.model.util.NeuroMLException;

public class DLemsWriter extends ABaseWriter
{

    static String DEFAULT_POP = "OneComponentPop";

    CommonLangWriter writer;

    boolean populationMode = false; // quick & dirty hack for multi component export
    boolean neuronMode = false; // quick & dirty hack for NEURON friendly export

    private final List<File> outputFiles = new ArrayList<File>();
    
    UnitConverter unitConverter = new SIUnitConverter();
    
    String TIME_DIM = Dimension.getTimeDimension().getName();
    boolean onlyFlattenIfNecessary = false;
    boolean flattenSynapses = true;
    
    public static final int DEFAULT_SEED = 123456789;
    
        
    HashMap<String,String> cellIdsVsPopulations = new HashMap<String, String>();
    HashMap<String,Component> popIdsVsComponents = new HashMap<String,Component>();
    HashMap<String,Set<String>> cellIdsVsSynapses = new HashMap<String, Set<String>>();

    public void setPopulationMode(boolean mode)
    {
        populationMode = mode;
    }
    
    public void setNeuronMode(boolean mode)
    {
        neuronMode = mode;
    }

    public void setOnlyFlattenIfNecessary(boolean onlyFlattenIfNecessary)
    {
        this.onlyFlattenIfNecessary = onlyFlattenIfNecessary;
    }

    public void setFlattenSynapses(boolean flattenSynapses)
    {
        this.flattenSynapses = flattenSynapses;
    }
    
    
    public DLemsWriter(Lems lems) throws ModelFeatureSupportException, LEMSException, NeuroMLException
    {
        super(lems, Format.DLEMS);
        this.writer = null;
    }

    public DLemsWriter(Lems lems, CommonLangWriter writer, boolean checkSupportedFeatures) throws ModelFeatureSupportException, LEMSException, NeuroMLException
    {
        super(lems, Format.DLEMS, checkSupportedFeatures);
        this.writer = writer;
    }

    public DLemsWriter(Lems lems, CommonLangWriter writer) throws ModelFeatureSupportException, LEMSException, NeuroMLException
    {
        super(lems, Format.DLEMS);
        this.writer = writer;
    }

    public DLemsWriter(Lems lems, File outputFolder, String outputFileName) throws ModelFeatureSupportException, LEMSException, NeuroMLException
    {
        super(lems, Format.DLEMS, outputFolder, outputFileName);
        this.writer = null;
    }

    public DLemsWriter(Lems lems, File outputFolder, String outputFileName, CommonLangWriter writer, boolean checkSupportedFeatures) throws ModelFeatureSupportException, LEMSException, NeuroMLException
    {
        super(lems, Format.DLEMS, outputFolder, outputFileName, checkSupportedFeatures);
        this.writer = writer;
    }

    public DLemsWriter(Lems lems, File outputFolder, String outputFileName, CommonLangWriter writer) throws ModelFeatureSupportException, LEMSException, NeuroMLException
    {
        super(lems, Format.DLEMS, outputFolder, outputFileName);
        this.writer = writer;
    }
    
    public void setUnitConverter(UnitConverter unitConv)
    {
        this.unitConverter = unitConv;
    }

    @Override
    public void setSupportedFeatures()
    {
        sli.addSupportInfo(format, ModelFeature.ABSTRACT_CELL_MODEL, SupportLevelInfo.Level.MEDIUM);
        sli.addSupportInfo(format, ModelFeature.COND_BASED_CELL_MODEL, SupportLevelInfo.Level.LOW);
        sli.addSupportInfo(format, ModelFeature.SINGLE_COMP_MODEL, SupportLevelInfo.Level.MEDIUM);
        sli.addSupportInfo(format, ModelFeature.NETWORK_MODEL, SupportLevelInfo.Level.LOW);
        sli.addSupportInfo(format, ModelFeature.MULTI_CELL_MODEL, SupportLevelInfo.Level.NONE);
        sli.addSupportInfo(format, ModelFeature.MULTI_POPULATION_MODEL, SupportLevelInfo.Level.LOW);
        sli.addSupportInfo(format, ModelFeature.NETWORK_WITH_INPUTS_MODEL, SupportLevelInfo.Level.NONE);
        sli.addSupportInfo(format, ModelFeature.NETWORK_WITH_PROJECTIONS_MODEL, SupportLevelInfo.Level.LOW);
        sli.addSupportInfo(format, ModelFeature.MULTICOMPARTMENTAL_CELL_MODEL, SupportLevelInfo.Level.NONE);
        sli.addSupportInfo(format, ModelFeature.HH_CHANNEL_MODEL, SupportLevelInfo.Level.LOW);
        sli.addSupportInfo(format, ModelFeature.KS_CHANNEL_MODEL, SupportLevelInfo.Level.NONE);

    }

    public static void putIntoVelocityContext(String dlems, VelocityContext context) throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();

        LinkedHashMap<String, Object> map = mapper.readValue(dlems, new TypeReference<LinkedHashMap<String, Object>>()
        {
        });

        for (Map.Entry<String, Object> entry : map.entrySet())
        {
            context.put(entry.getKey(), entry.getValue());
        }

    }

    private String visitExpression(IVisitable expr) throws ContentError
    {
        String visited;
        if (writer == null)
        {
            visited = expr.getValueExpression();
        } else
        {
            visited = writer.serialize(expr.getParseTree());
        }
        return visited;
    }

    public String getMainScript() throws LEMSException, IOException
    {
        JsonFactory f = new JsonFactory();
        StringWriter sw = new StringWriter();
        JsonGenerator g = f.createJsonGenerator(sw);
        g.useDefaultPrettyPrinter();
        g.writeStartObject();
        Target target = lems.getTarget();
        Component simCpt = target.getComponent();

        g.writeStringField(DLemsKeywords.DT.get(), convertTime(simCpt.getParamValue("step")));

        int seed = DEFAULT_SEED;
        if (simCpt.hasStringValue("seed"))
            seed = Integer.parseInt(simCpt.getStringValue("seed"));
        
        g.writeStringField(DLemsKeywords.SEED.get(), seed+"");


        String targetId = simCpt.getStringValue("target");

        Component tgtComp = lems.getComponent(targetId);
        
        g.writeStringField(DLemsKeywords.NAME.get(), targetId);

        ArrayList<Component> pops = tgtComp.getChildrenAL("populations");

        ArrayList<Component> projs = tgtComp.getChildrenAL("projections");
        projs.addAll(tgtComp.getChildrenAL("electricalProjection"));
        projs.addAll(tgtComp.getChildrenAL("synapticConnections"));
        
        
        if (pops.size() > 0)
        {
            ArrayList<String> written = new ArrayList<String>();
            ArrayList<String> writtenTypes = new ArrayList<String>();
            
            if (populationMode)
            {
                if (tgtComp.getComponentType().isOrExtends("networkWithTemperature"))
                {
                    ParamValue pv = tgtComp.getParamValue("temperature");
                    g.writeStringField(DLemsKeywords.TEMPERATURE.get(), unitConverter.convert((float)pv.getDoubleValue(),pv.getDimensionName())+"");
                    
                }
                
                for (Component pop : pops)
                {
                    String compRef = pop.getStringValue("component");
                    String popName = pop.getID();
                    cellIdsVsPopulations.put(popName, compRef);
                }
                g.writeObjectFieldStart(DLemsKeywords.SYNAPSES.get());
            }
            for (Component proj : projs)
            {
                ArrayList<Component> connChildren = proj.getChildrenAL("connections");
                connChildren.addAll(proj.getChildrenAL("connectionInstances"));
                
                String synId = proj.hasStringValue("synapse") ? proj.getStringValue("synapse") : connChildren.get(0).getStringValue("synapse");
                
                String postsynapticPopulation = proj.getStringValue("postsynapticPopulation");
                String postCell = cellIdsVsPopulations.get(postsynapticPopulation);
                
                if (cellIdsVsSynapses.get(postCell)==null)
                {
                    cellIdsVsSynapses.put(postCell, new LinkedHashSet<String>());
                }
                cellIdsVsSynapses.get(postCell).add(synId);
                

                if (!written.contains(synId))
                {
                    //System.out.println("-             Adding " + synId);
                    Component synComp = lems.getComponent(synId);

                    if (!writtenTypes.contains(synComp.getTypeName()))
                    {
                        if (flattenSynapses)
                        {
                            createFlattenedCompType(synComp);
                            writtenTypes.add(synComp.getTypeName());
                        }
                    }
                    Component cpFlat = null;
                    
                    if (flattenSynapses)
                        cpFlat = createFlattenedComp(synComp);
                    else
                        cpFlat = synComp;
                    
                    if (populationMode)
                    {
                        g.writeObjectFieldStart(synId);

                        g.writeObjectFieldStart(DLemsKeywords.SYNAPSE.get());
                        writeDLemsForComponent(g, cpFlat);
                        g.writeEndObject();

                        g.writeEndObject();
                        written.add(synId);
                    }

                }

            }
            

            if (populationMode)
            {
                g.writeEndObject(); // synapses
                g.writeObjectFieldStart(DLemsKeywords.POPULATIONS.get());
            }
            
            HashMap<String,Component> processedComps = new HashMap<String, Component>();
            
                            
            for (Component pop : pops)
            {
                String compRef = pop.getStringValue("component");
                String popName = pop.getID();

                //System.out.println("---------       Adding " + compRef);
                Component popComp = lems.getComponent(compRef);

                HashMap<String, Set<String>> extraParameters = new HashMap<String, Set<String>>();
                extraParameters.put("synapses_allowed", cellIdsVsSynapses.get(popComp.id));
                
                if (popComp.getComponentType().isOrExtends("cell")) 
                {
                    processedComps.put(compRef, popComp);
                    if (populationMode)
                    {
                        StringWriter swComp = new StringWriter();
                        JsonGenerator gComp = f.createJsonGenerator(swComp);
                        gComp.useDefaultPrettyPrinter();
                        gComp.writeStartObject();

                        writeDLemsForComponent(gComp, popComp, extraParameters);

                        gComp.writeEndObject();
                        gComp.close();

                        File compFile = new File(this.getOutputFolder(), popComp.getID() + ".cell.json");
                        FileUtil.writeStringToFile(swComp.toString(), compFile);
                        if (!outputFiles.contains(compFile))
                            outputFiles.add(compFile);

                    } 
                    else
                    {
                        //writeDLemsForComponent(g, cpFlat);
                    }
                }
                else 
                {
                    if (!written.contains(compRef))
                    {
                        if (!writtenTypes.contains(popComp.getTypeName()))
                        {
                            createFlattenedCompType(popComp);
                            writtenTypes.add(popComp.getTypeName());
                        }

                        Component cpFlat = createFlattenedComp(popComp);
                        processedComps.put(compRef, cpFlat);
                        if (populationMode)
                        {
                            StringWriter swComp = new StringWriter();
                            JsonGenerator gComp = f.createJsonGenerator(swComp);
                            gComp.useDefaultPrettyPrinter();
                            gComp.writeStartObject();

                            writeDLemsForComponent(gComp, cpFlat, extraParameters);

                            gComp.writeEndObject();
                            gComp.close();

                            File compFile = new File(this.getOutputFolder(), cpFlat.getID() + ".cell.json");
                            FileUtil.writeStringToFile(swComp.toString(), compFile);
                            
                            if (!outputFiles.contains(compFile))
                                outputFiles.add(compFile);

                        } 
                        else
                        {
                            writeDLemsForComponent(g, cpFlat);
                        }

                        written.add(compRef);
                    }
                }
                
                if (populationMode) 
                {

                    g.writeObjectFieldStart(popName);
                    int size;
                    
                    if (pop.hasStringValue("size"))
                    {
                        size = Integer.parseInt(pop.getStringValue("size"));
                    }
                    else
                    {
                        size = pop.getChildrenAL("instances").size();
                    }

                    g.writeStringField(DLemsKeywords.SIZE.get(), size+"");

                    g.writeObjectFieldStart(DLemsKeywords.COMPONENT.get());
                    if (false && popComp.getComponentType().isOrExtends("cell"))
                    {
                        g.writeStringField(DLemsKeywords.NAME.get(), popComp.getID());
                        g.writeStringField(DLemsKeywords.TYPE.get(), popComp.getComponentType().getName());
                    } 
                    else 
                    {
                        writeDLemsForComponent(g, processedComps.get(compRef), extraParameters);
                    }
                    g.writeEndObject();

                    g.writeEndObject();
                }
            }
            
            if (populationMode)
            {

                g.writeEndObject();
                
                ArrayList<String> handledSyns = new ArrayList<String>();
                for (Component proj : projs)
                {
                    ArrayList<Component> connChildren = proj.getChildrenAL("connections");
                    connChildren.addAll(proj.getChildrenAL("connectionInstances"));
                
                    String synRef = proj.hasStringValue("synapse") ? proj.getStringValue("synapse") : connChildren.get(0).getStringValue("synapse");
                    if (!handledSyns.contains(synRef))
                    {
                        //System.out.println("-             Adding " + synRef);
                        Component synComp = lems.getComponent(synRef);
                        //System.out.println("-             Adding " + synRef+", "+synComp.getTypeName()+", "+writtenTypes+", "+written);

                        if (flattenSynapses) {
                            createFlattenedCompType(synComp);
                            writtenTypes.add(synComp.getTypeName());
                        }
                        

                        Component cpFlat = null;
                        if (flattenSynapses)
                            cpFlat = createFlattenedComp(synComp);
                        else
                            cpFlat = synComp;
                        
                        StringWriter swComp = new StringWriter();
                        JsonGenerator gComp = f.createJsonGenerator(swComp);
                        gComp.useDefaultPrettyPrinter();
                        gComp.writeStartObject();

                        writeDLemsForComponent(gComp, cpFlat);

                        gComp.writeEndObject();
                        gComp.close();

                        File synFile = new File(this.getOutputFolder(), cpFlat.getID() + ".synapse.json");
                        FileUtil.writeStringToFile(swComp.toString(), synFile);
                        
                        if (!outputFiles.contains(synFile))
                            outputFiles.add(synFile);
                        handledSyns.add(synRef);
                    }
                  
                    
                }
            
                ArrayList<Component> exInputs = tgtComp.getChildrenAL("explicitInputs");
                HashMap<String, String> inputIdsVsTargets = new HashMap<String, String>();
                HashMap<String, String> inputIdsVsComponents = new HashMap<String, String>();
                int count = 0;
                for (Component input : exInputs)
                {
                    String inputRef = input.getStringValue("input")+count;
                    String tgt = input.getStringValue("target");
                    inputIdsVsTargets.put(inputRef, tgt);
                    inputIdsVsComponents.put(inputRef, input.getStringValue("input"));
                    count +=1;
                }
                for (Component il : tgtComp.getChildrenAL("inputs"))
                {
                    
                    ArrayList<Component> inputs = il.getChildrenAL("inputs");
                    for (int i=0; i<inputs.size(); i++) 
                    {
                        Component input = inputs.get(i);
                        String inputRef = il.getID()+"__"+i;
                        String tgt = input.getStringValue("target");
                        inputIdsVsTargets.put(inputRef, tgt.substring(3)); // remove initial ../
                        inputIdsVsComponents.put(inputRef, il.getStringValue("component"));
                        
                    }
                    
                }
                
                if (!inputIdsVsTargets.isEmpty())
                {
                    g.writeObjectFieldStart(DLemsKeywords.INPUTS.get());
                    for (String inputId : inputIdsVsTargets.keySet())
                    {
                        g.writeObjectFieldStart(inputId);
                        
                        //System.out.println(".... "+inputId+",  "+inputIdsVsComponents);
                        Component inputComp = lems.getComponent(inputIdsVsComponents.get(inputId));
                        String tgt = inputIdsVsTargets.get(inputId);
                        LEMSQuantityPath lqp = new LEMSQuantityPath(tgt+"/xxx");
                        
                        g.writeStringField(DLemsKeywords.POPULATION.get(), lqp.getPopulation());
                        g.writeStringField(DLemsKeywords.POPULATION_INDEX.get(), lqp.getPopulationIndex()+"");
                        
                        //TODO: flatten...
                        //Component inputCompFlat = createFlattenedComp(inputComp);
                        Component inputCompFlat = inputComp;
                        
                        g.writeObjectFieldStart(DLemsKeywords.COMPONENT.get());
                        writeDLemsForComponent(g, inputCompFlat);
                        g.writeEndObject();
                        g.writeEndObject();

                        StringWriter swComp = new StringWriter();
                        JsonGenerator gComp = f.createJsonGenerator(swComp);
                        gComp.useDefaultPrettyPrinter();
                        gComp.writeStartObject();

                        writeDLemsForComponent(gComp, inputCompFlat);

                        gComp.writeEndObject();
                        gComp.close();

                        File inputFile = new File(this.getOutputFolder(), inputCompFlat.getID() + ".input.json");
                        FileUtil.writeStringToFile(swComp.toString(), inputFile);
                        if (!outputFiles.contains(inputFile))
                            outputFiles.add(inputFile);

                    }
                    g.writeEndObject();
                }
            }
            
            
            if (populationMode && projs.size()>0) 
            {
                g.writeObjectFieldStart(DLemsKeywords.PROJECTIONS.get());
                for (Component proj: projs)
                {
                    ArrayList<Component> connChildren = proj.getChildrenAL("connections");
                    connChildren.addAll(proj.getChildrenAL("connectionInstances"));
                    g.writeObjectFieldStart(proj.id);
                    g.writeStringField(DLemsKeywords.PRE_POPULATION.get(), proj.getStringValue("presynapticPopulation"));
                    g.writeStringField(DLemsKeywords.POST_POPULATION.get(), proj.getStringValue("postsynapticPopulation"));
                    String synRef = proj.hasStringValue("synapse") ? proj.getStringValue("synapse") : connChildren.get(0).getStringValue("synapse");
                    g.writeStringField(DLemsKeywords.SYNAPSE.get(), synRef);
                    
                    g.writeArrayFieldStart(DLemsKeywords.CONNECTIONS.get());

                    connChildren.addAll(proj.getChildrenAL("connectionsWD"));
        
                    for (Component conn: connChildren)
                    {
                        g.writeStartObject();
                        g.writeStringField(DLemsKeywords.NAME.get(), conn.id);
                        String pre = (conn.hasStringValue("preCellId") ? conn.getStringValue("preCellId") : conn.getStringValue("preCell")).substring(3);  // remove ../
                        LEMSQuantityPath lqpPre = new LEMSQuantityPath(pre+"/xxx");
                        String post = (conn.hasStringValue("postCellId") ? conn.getStringValue("postCellId") : conn.getStringValue("postCell")).substring(3);  // remove ../
                        LEMSQuantityPath lqpPost = new LEMSQuantityPath(post+"/xxx");
                        g.writeStringField(DLemsKeywords.PRE_CELL_ID.get(), lqpPre.getPopulationIndex()+"");
                        g.writeStringField(DLemsKeywords.POST_CELL_ID.get(), lqpPost.getPopulationIndex()+"");
                        if (conn.hasStringValue("weight"))
                        {
                            g.writeStringField(DLemsKeywords.WEIGHT.get(), conn.getStringValue("weight"));
                        }
                        else
                        {
                            g.writeStringField(DLemsKeywords.WEIGHT.get(), "1");
                        }
                        if (conn.hasStringValue("delay"))
                        {
                            g.writeStringField(DLemsKeywords.DELAY.get(), convertTime(conn.getParamValue("delay")));
                        }
                        else
                        {
                            g.writeStringField(DLemsKeywords.DELAY.get(), "0");
                        }
                        g.writeEndObject();
                    }
                    
                    g.writeEndArray();
                    
                    g.writeEndObject();
                }
                g.writeEndObject();
            }

        } else
        {

            writeDLemsForComponent(g, tgtComp);
        }

        writeSimulationInfo(g, simCpt);
        g.writeStringField(DLemsKeywords.COMMENT.get(), Utils.getHeaderComment(format));

        g.writeEndObject();

        g.close();

        return sw.toString();
    }

    private void writeDLemsForComponent(JsonGenerator g, Component comp) throws ContentError, JsonGenerationException, IOException, LEMSException
    {
        writeDLemsForComponent(g, comp, null);
    }

    private void writeDLemsForComponent(JsonGenerator g, Component comp, HashMap<String, Set<String>> extraParameters) throws ContentError, JsonGenerationException, IOException, LEMSException
    {
        if (comp==null)
            return;
        
        g.writeStringField(DLemsKeywords.NAME.get(), comp.getID());
        g.writeStringField(DLemsKeywords.TYPE.get(), comp.getComponentType().getName());
        
        
        if (comp.getComponentType().isOrExtends("cell")) {
            g.writeStringField(DLemsKeywords.COMMENT.get(), "Not adding all of cell definition in JSON as it is of type <cell>");
            
            g.writeObjectFieldStart(DLemsKeywords.PARAMETERS.get());
            writeParameters(g, comp);
            g.writeEndObject();
            
            for (Component specie: comp.getChild("biophysicalProperties").getChild("intracellularProperties").getChildrenAL("speciesList")) {
                
                try{
                    g.writeStringField(specie.getID()+"_initial_internal_conc", ""+Utils.getMagnitudeInSI(specie.getAttributeValue("initialConcentration")));
                    g.writeStringField(specie.getID()+"_initial_external_conc", ""+Utils.getMagnitudeInSI(specie.getAttributeValue("initialExtConcentration")));
                }
                catch (NeuroMLException ne)
                {
                    throw new ContentError("Unabel to parse NeuroML", ne);
                }
            }
            
        } else {
            g.writeObjectFieldStart(DLemsKeywords.DYNAMICS.get());
            writeDynamics(g, comp);
            g.writeEndObject();

            g.writeArrayFieldStart(DLemsKeywords.EVENTS.get());
            writeEvents(g, comp);
            g.writeEndArray();

            g.writeObjectFieldStart(DLemsKeywords.PARAMETERS.get());
            writeParameters(g, comp);
            g.writeEndObject();

            g.writeObjectFieldStart(DLemsKeywords.STATE.get());
            writeState(g, comp);
            g.writeEndObject();

            g.writeObjectFieldStart(DLemsKeywords.STATE_FUNCTIONS.get());
            writeStateFunctions(g, comp);
            g.writeEndObject();
        }
        if (extraParameters!=null) {
            for (String s1: extraParameters.keySet()) {
                if (extraParameters.get(s1)!=null && !extraParameters.get(s1).isEmpty()) {
                    g.writeArrayFieldStart(s1);
                    for (String s2: extraParameters.get(s1)){
                        g.writeString(s2);
                    }
                    g.writeEndArray();
                }
            }
        }

    }
    
    private String convertTime(ParamValue lemsValue) throws ContentError, LEMSException
    {
        return unitConverter.convert((float)lemsValue.getDoubleValue(),TIME_DIM)+"";
    }

    private void writeSimulationInfo(JsonGenerator g, Component simCpt) throws ContentError, JsonGenerationException, IOException, LEMSException
    {
        g.writeStringField(DLemsKeywords.T_END.get(), convertTime(simCpt.getParamValue("length")));
        g.writeStringField(DLemsKeywords.T_START.get(), "0");

        for (Component dispComp : simCpt.getAllChildren())
        {
            if (dispComp.getTypeName().equals("OutputFile"))
            {
                // Todo: remove, it is only used in older dlems templates...
                g.writeStringField(DLemsKeywords.DUMP_TO_FILE.get(), dispComp.getStringValue("fileName"));
            }
        }
        
        g.writeArrayFieldStart(DLemsKeywords.SPIKE_FILE.get());
        
        for (Component outFile : simCpt.getAllChildren())
        {
            if (outFile.getTypeName().equals("EventOutputFile"))
            {
                g.writeStartObject();
                g.writeStringField(DLemsKeywords.NAME.get(), outFile.getID());
                g.writeStringField(DLemsKeywords.FILE_NAME.get(), outFile.getStringValue("fileName"));
                g.writeStringField(DLemsKeywords.SPIKE_FILE_FORMAT.get(), outFile.getStringValue("format"));
                        
                g.writeArrayFieldStart(DLemsKeywords.EVENT_SELECTIONS.get());
                
                for (Component outputColumn : outFile.getAllChildren())
                {
                    if (outputColumn.getTypeName().equals("EventSelection"))
                    {
                        g.writeStartObject();
                        
                        g.writeStringField(DLemsKeywords.EVENT_SELECTION_ID.get(), outputColumn.getID());
                        g.writeStringField(DLemsKeywords.SELECT.get(), outputColumn.getStringValue("select"));
                        
                        String quantity = outputColumn.getStringValue("select")+"/spike"; // dummy variable...
                        
                        extractQuantityInfo(quantity, g);
                        
                        g.writeEndObject();
                    }
                }
                
                g.writeEndArray();
                
                g.writeEndObject();
                
            }
        }
        
        g.writeEndArray();
        
        g.writeArrayFieldStart(DLemsKeywords.OUTPUT_FILE.get());
        
        for (Component outFile : simCpt.getAllChildren())
        {
            if (outFile.getTypeName().equals("OutputFile"))
            {
                g.writeStartObject();
                g.writeStringField(DLemsKeywords.NAME.get(), outFile.getID());
                g.writeStringField(DLemsKeywords.FILE_NAME.get(), outFile.getStringValue("fileName"));
                
                g.writeArrayFieldStart(DLemsKeywords.OUTPUT_COLUMNS.get());
                
                g.writeStartObject();
                g.writeStringField(DLemsKeywords.NAME.get(), "t");
                
                g.writeStringField(DLemsKeywords.VARIABLE.get(), "t");
                if (neuronMode) 
                {
                    g.writeStringField(DLemsKeywords.NEURON_VARIABLE_SCALE.get(), "1000.0");
                }
                //g.writeStringField(DLemsKeywords., "t");
                g.writeEndObject();
                        
                for (Component outputColumn : outFile.getAllChildren())
                {
                    if (outputColumn.getTypeName().equals("OutputColumn"))
                    {
                        g.writeStartObject();
                        
                        g.writeStringField(DLemsKeywords.NAME.get(), outputColumn.getID());
                        
                        String quantity = outputColumn.getStringValue("quantity");
                        extractQuantityInfo(quantity, g);
                        
                        g.writeEndObject();
                    }
                }
                
                g.writeEndArray();
                
                g.writeEndObject();
            }
        }
        g.writeEndArray();

        g.writeArrayFieldStart(DLemsKeywords.DISPLAY.get());

        for (Component dispComp : simCpt.getAllChildren())
        {
            if (dispComp.getTypeName().equals("Display"))
            {

                g.writeStartObject();
                
                g.writeStringField(DLemsKeywords.NAME.get(), dispComp.getID());
                g.writeStringField(DLemsKeywords.TITLE.get(), dispComp.getStringValue("title"));

                g.writeObjectFieldStart(DLemsKeywords.ABSCISSA_AXIS.get());
                g.writeStringField(DLemsKeywords.MIN.get(), dispComp.getStringValue("xmin"));
                g.writeStringField(DLemsKeywords.MAX.get(), dispComp.getStringValue("xmax"));
                g.writeEndObject();

                g.writeObjectFieldStart(DLemsKeywords.ORDINATE_AXIS.get());
                g.writeStringField(DLemsKeywords.MIN.get(), dispComp.getStringValue("ymin"));
                g.writeStringField(DLemsKeywords.MAX.get(), dispComp.getStringValue("ymax"));
                g.writeEndObject();

                g.writeArrayFieldStart(DLemsKeywords.CURVES.get());

                for (Component lineComp : dispComp.getAllChildren())
                {
                    if (lineComp.getTypeName().equals("Line"))
                    {

                        g.writeStartObject();
                        g.writeStringField(DLemsKeywords.NAME.get(), lineComp.getID());
                        g.writeStringField(DLemsKeywords.ABSCISSA.get(), "t");
                        String quantity = lineComp.getStringValue("quantity");
                        
                        LEMSQuantityPath lqp = new LEMSQuantityPath(quantity);

                        g.writeStringField(DLemsKeywords.QUANTITY.get(), quantity);
                        g.writeStringField(DLemsKeywords.ORDINATE.get(), lqp.getVariable());

                        g.writeStringField(DLemsKeywords.POPULATION.get(), lqp.getPopulation());
                        g.writeStringField(DLemsKeywords.POPULATION_INDEX.get(), lqp.getPopulationIndex()+"");
                        g.writeStringField(DLemsKeywords.SEGMENT_ID.get(), lqp.getSegmentId()+"");
                        g.writeStringField(DLemsKeywords.COLOUR.get(), lineComp.getStringValue("color"));
                        g.writeEndObject();
                    }
                }
                g.writeEndArray();
                g.writeEndObject();
            }
        }

        g.writeEndArray();
    }
    
    private HashMap<String,HashMap<Integer,String>> cachedNrnSecNames = new HashMap<String,HashMap<Integer,String>>();
    private HashMap<String,HashMap<Integer,Float>> cachedNrnFracts = new HashMap<String,HashMap<Integer,Float>>();
    
    private void addNrnSecNameFract(Component comp, int segId, JsonGenerator g) throws LEMSException, NeuroMLException, IOException
    {
        if (!cachedNrnSecNames.containsKey(comp.getID()))
        {
            cachedNrnSecNames.put(comp.getID(), new HashMap<Integer,String>());
        }
        if (!cachedNrnFracts.containsKey(comp.getID()))
        {
            cachedNrnFracts.put(comp.getID(), new HashMap<Integer,Float>());
        }
        
        if(!cachedNrnSecNames.get(comp.getID()).containsKey(segId))
        {
            Cell cell = (Cell)Utils.convertLemsComponentToNeuroML(comp).get(comp.getID());
            
            NamingHelper nh = new NamingHelper(cell);
            Segment segment = CellUtils.getSegmentWithId(cell, segId);
            String nrnsec = nh.getNrnSectionName(segment);
            float fract = cell.getMorphology().getSegment().size()==1 ? 0.5f : (float) CellUtils.getFractionAlongSegGroupLength(cell, nrnsec, segment.getId(), 0.5f);
            cachedNrnSecNames.get(comp.getID()).put(segId, nrnsec);
            cachedNrnFracts.get(comp.getID()).put(segId, fract);
        }
        else
        {
        }
        String nrnsec = cachedNrnSecNames.get(comp.getID()).get(segId);
        float fract  = cachedNrnFracts.get(comp.getID()).get(segId);
        

        g.writeStringField(DLemsKeywords.NEURON_SECTION_NAME.get(), nrnsec);
        g.writeStringField(DLemsKeywords.NEURON_FRACT_ALONG.get(), fract+"");
        
                    
    }
    
    private void extractQuantityInfo(String quantity, JsonGenerator g) throws IOException, ContentError, LEMSException
    {
        LEMSQuantityPath lqp = new LEMSQuantityPath(quantity);

        g.writeStringField(DLemsKeywords.POPULATION.get(), lqp.getPopulation());
        g.writeStringField(DLemsKeywords.POPULATION_INDEX.get(), lqp.getPopulationIndex()+"");

        if (populationMode) 
        {
            if (!popIdsVsComponents.containsKey(lqp.getPopulation()))
            {
                Component comp = lems.getComponent(cellIdsVsPopulations.get(lqp.getPopulation()));
                popIdsVsComponents.put(lqp.getPopulation(),comp);
            }
            Component comp = popIdsVsComponents.get(lqp.getPopulation());
            
            if (comp.getComponentType().isOrExtends("cell")) {
                for (Component seg: comp.getChild("morphology").getChildrenAL("segments")) {
                    if (seg.id.equals(lqp.getSegmentId()+"")) {
                        g.writeStringField(DLemsKeywords.SEGMENT_ID.get(), lqp.getSegmentId()+"");
                        g.writeStringField(DLemsKeywords.SEGMENT_NAME.get(), seg.getName());
                        break;
                    }
                }
                try
                {
                    addNrnSecNameFract(comp, lqp.getSegmentId(), g);
                    
                } 
                catch (NeuroMLException nmle) 
                {
                    throw new LEMSException("Problem building NeuroML for LEMS component: "+comp.summary(), nmle);
                }
            }
            else 
            {
                g.writeStringField(DLemsKeywords.SEGMENT_NAME.get(), "soma");
                g.writeStringField(DLemsKeywords.NEURON_SECTION_NAME.get(), "soma");
                g.writeStringField(DLemsKeywords.NEURON_FRACT_ALONG.get(), "0.5");
            }
            if (neuronMode && !lqp.getVariable().equals("spike")) 
            {
                String nrnVar = LEMSQuantityPathNeuron.convertToNeuronVariable(lqp.getVariableParts(),comp);
                g.writeStringField(DLemsKeywords.NEURON_VARIABLE_NAME.get(), nrnVar);

                float conv = NRNUtils.getNeuronUnitFactor(LEMSQuantityPathNeuron.getDimensionOfVariableOnCellInPopComp(lqp.getVariableParts(), comp).getName());
                g.writeStringField(DLemsKeywords.NEURON_VARIABLE_SCALE.get(), conv+"");

            }
        }
        
        g.writeStringField(DLemsKeywords.VARIABLE.get(), lqp.getVariable());
        g.writeStringField(DLemsKeywords.QUANTITY.get(), quantity);
    }

    private void writeState(JsonGenerator g, Component comp) throws ContentError, JsonGenerationException, IOException
    {
        ComponentType ct = comp.getComponentType();

        for (StateVariable sv : ct.getDynamics().getStateVariables())
        {
            String init = "0";
            for (OnStart os : ct.getDynamics().getOnStarts())
            {
                for (StateAssignment sa : os.getStateAssignments())
                {
                    if (sa.getVariable().equals(sv.getName()))
                    {
                        init = visitExpression(sa);
                    }
                }
            }
            g.writeStringField(sv.getName(), init);
        }
    }

    private void writeStateFunctions(JsonGenerator g, Component comp) throws ContentError, JsonGenerationException, IOException
    {
        ComponentType ct = comp.getComponentType();

        for (DerivedParameter dp : ct.getDerivedParameters())
        {
            g.writeStringField(dp.getName(), visitExpression(dp));
        }

        for (DerivedVariable dv : ct.getDynamics().getDerivedVariables())
        {
            if (dv.value == null || dv.value.length() == 0)
            {
                g.writeStringField(dv.getName(), "0");
            } else
            {
                g.writeStringField(dv.getName(), visitExpression(dv));
            }
        }
    }

    private void writeParameters(JsonGenerator g, Component comp) throws ContentError, JsonGenerationException, IOException, LEMSException
    {
        ComponentType ct = comp.getComponentType();

        for (FinalParam p : ct.getFinalParams())
        {
            ParamValue pv = comp.getParamValue(p.getName());
            float f = unitConverter.convert((float)pv.getDoubleValue(), pv.getDimensionName());
            String s = f+"";
            g.writeStringField(p.getName(), s);
        }

        for (Constant c : ct.getConstants())
        {
            g.writeStringField(c.getName(), unitConverter.convert((float)c.getValue(), c.getDimension().getName()) + "");
        }

    }

    private void writeEvents(JsonGenerator g, Component comp) throws ContentError, JsonGenerationException, IOException
    {
        ComponentType ct = comp.getComponentType();

        for (OnCondition oc : ct.getDynamics().getOnConditions())
        {
            g.writeStartObject();

            g.writeStringField(DLemsKeywords.NAME.get(), oc.test.replace(' ', '_').replace('.', '_'));
            g.writeStringField(DLemsKeywords.CONDITION.get(), inequalityToCondition(oc.test));
            g.writeStringField(DLemsKeywords.DIRECTION.get(), cond2sign(oc.test));

            g.writeObjectFieldStart(DLemsKeywords.EFFECT.get());

            g.writeObjectFieldStart(DLemsKeywords.STATE.get());

            for (StateAssignment sa : oc.getStateAssignments())
            {
                g.writeStringField(sa.getVariable(), visitExpression(sa));
            }

            g.writeEndObject();
            g.writeEndObject();
            g.writeEndObject();

        }

        for (OnEvent oe : ct.getDynamics().getOnEvents())
        {
            g.writeStartObject();

            g.writeStringField(DLemsKeywords.NAME.get(), oe.port);
            g.writeStringField(DLemsKeywords.CONDITION.get(), "EVENT_ON_PORT__" + oe.port);

            g.writeObjectFieldStart(DLemsKeywords.EFFECT.get());

            g.writeObjectFieldStart(DLemsKeywords.STATE.get());

            for (StateAssignment sa : oe.getStateAssignments())
            {
                g.writeStringField(sa.getVariable(), visitExpression(sa));
            }

            g.writeEndObject();
            g.writeEndObject();
            g.writeEndObject();

        }

    }

    private String cond2sign(String cond)
    {
        String ret = "???";
        if (cond.indexOf(".gt.") > 0 || cond.indexOf(".geq.") > 0)
        {
            return "+";
        }
        if (cond.indexOf(".lt.") > 0 || cond.indexOf(".leq.") > 0)
        {
            return "-";
        }
        if (cond.indexOf(".eq.") > 0)
        {
            return "0";
        }
        return ret;
    }

    private String inequalityToCondition(String ineq)
    {
        String[] s = ineq.split("(\\.)[gleqt]+(\\.)");
        // E.info("Split: "+ineq+": len "+s.length+"; "+s[0]+", "+s[1]);
        String expr = s[0].trim() + " - (" + s[1].trim() + ")";
        // sign = comp2sign(s.group(2))
        return expr;
    }

    private void writeDynamics(JsonGenerator g, Component comp) throws ContentError, JsonGenerationException, IOException
    {
        ComponentType ct = comp.getComponentType();

        for (TimeDerivative td : ct.getDynamics().getTimeDerivatives())
        {
            // g.writeStringField(td.getVariable(), td.getValueExpression());
            g.writeStringField(td.getVariable(), visitExpression(td));
        }

    }
    
    private boolean continueFlattenning(Component comp, ComponentFlattener cf) 
    {
        if (onlyFlattenIfNecessary) {
            if (!cf.requiresFlattenning()) {
                return false;
            }
            if (comp.getAllChildren().size()==1 &&
                comp.getAllChildren().get(0).getTypeName().equals("notes")) {
                return false;
            }
        } 
            
        return true;
    }

    private ComponentType createFlattenedCompType(Component compOrig) throws ContentError, ParseError
    {

        ComponentType ctFlat = new ComponentType();
        ComponentFlattener cf = new ComponentFlattener(lems, compOrig, true, true);
        
        if (!continueFlattenning(compOrig, cf)) {
            return compOrig.getComponentType();
        }

        try
        {
            ctFlat = cf.getFlatType();
            try 
            { 
                ComponentType ct = lems.getComponentTypeByName(ctFlat.getName());
                
                //System.out.println("ComponentType already created: "+ct.getName());
                return ct;
                
            } catch (ContentError e) {
                //System.out.println("ComponentType NOT already created...");
                // continue...
            }
            lems.addComponentType(ctFlat);
            String typeOut = XMLSerializer.serialize(ctFlat);
            //System.out.println("Flat type: \n" + typeOut);
            lems.resolve(ctFlat);
        } catch (ConnectionError e)
        {
            throw new ParseError("Error when flattening component: " + compOrig, e);
        }
        return ctFlat;
    }

    private Component createFlattenedComp(Component compOrig) throws ContentError, ParseError
    {

        Component comp = new Component();
        ComponentFlattener cf = new ComponentFlattener(lems, compOrig);

        if (!continueFlattenning(compOrig, cf)) {
            return compOrig;
        }
        
        try
        {
            comp = cf.getFlatComponent();
            if (lems.hasComponent(comp.id))
            {
                //System.out.println("Flat Component for "+comp.getID()+" already created: "+comp.getID());
                return lems.getComponent(comp.id);
            } else {
                //System.out.println("Flat Component for "+comp.getID()+" NOT already created...");
            }
            
            lems.addComponent(comp);
            String compOut = XMLSerializer.serialize(comp);
            //System.out.println("Flat component: \n" + compOut);
            lems.resolve(comp);
        } catch (ConnectionError e)
        {
            throw new ParseError("Error when flattening component: " + compOrig, e);
        }
        return comp;
    }

    public static void main(String[] args) throws Exception
    {
        MinimalMessageHandler.setVeryMinimal(true);
        E.setDebug(false);

        ArrayList<File> lemsFiles = new ArrayList<File>();
        //lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex0_IaF.xml"));
        //lemsFiles.add(new File("../neuroConstruct/osb/cerebral_cortex/networks/IzhikevichModel/NeuroML2/LEMS_SmallNetwork.xml"));
        //lemsFiles.add(new File("../neuroConstruct/osb/cerebral_cortex/networks/IzhikevichModel/NeuroML2/LEMS_2007Cells.xml"));
        //lemsFiles.add(new File("../neuroConstruct/osb/cerebral_cortex/networks/IzhikevichModel/NeuroML2/LEMS_2007One.xml"));
        //lemsFiles.add(new File("../OpenCortex/examples/LEMS_SimpleNet.xml"));
        //lemsFiles.add(new File("../neuroConstruct/osb/generic/hodgkin_huxley_tutorial/Tutorial/Source/LEMS_HH_Simulation.xml"));
        //lemsFiles.add(new File("../OpenCortex/examples/LEMS_SpikingNet.xml"));
        //
        //lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex5_DetCell.xml"));
        lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex25_MultiComp.xml"));
        //lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex19a_GapJunctionInstances.xml"));
        //lemsFiles.add(new File("../neuroConstruct/osb/showcase/NetPyNEShowcase/NeuroML2/scaling/LEMS_Balanced_0.2.xml"));
        
        //lemsFiles.add(new File("../OpenCortex/examples/LEMS_IClamps.xml"));

        for (File lemsFile : lemsFiles)
        {
            Lems lems = Utils.readLemsNeuroMLFile(lemsFile).getLems();
            DLemsWriter dw = new DLemsWriter(lems, lemsFile.getParentFile(), lemsFile.getName().replaceAll(".xml", ".json"), null, false);
            dw.setPopulationMode(true);
            dw.setOnlyFlattenIfNecessary(true);
            dw.setFlattenSynapses(false);

            NRNUtils nrnUtils = new NRNUtils();
            dw.setUnitConverter(nrnUtils);  
            
            List<File> files = dw.convert();
            for (File f : files)
            {
                System.out.println("Have created: " + f.getAbsolutePath());
            }

        }
    }

    @Override
    public List<File> convert() throws GenerationException, IOException
    {
        try
        {
            String code = this.getMainScript();
            File outputFile = new File(this.getOutputFolder(), this.getOutputFileName());
            FileUtil.writeStringToFile(code, outputFile);
            outputFiles.add(outputFile);

        } catch (LEMSException e)
        {
            throw new GenerationException("Issue when converting files", e);
        }

        // TODO Auto-generated method stub
        return outputFiles;
    }

    @Override
    protected void addComment(StringBuilder sb, String comment)
    {
        // TODO Auto-generated method stub

    }


}

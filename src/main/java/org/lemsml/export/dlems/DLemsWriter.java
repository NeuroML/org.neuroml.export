package org.lemsml.export.dlems;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import org.neuroml.export.neuron.NRNUtils;
import org.neuroml.export.utils.Format;
import org.neuroml.export.utils.LEMSQuantityPath;
import org.neuroml.export.utils.Utils;
import org.neuroml.export.utils.support.ModelFeature;
import org.neuroml.export.utils.support.SupportLevelInfo;
import org.neuroml.export.utils.visitors.CommonLangWriter;
import org.neuroml.model.util.NeuroMLException;

public class DLemsWriter extends ABaseWriter
{

    static String DEFAULT_POP = "OneComponentPop";

    CommonLangWriter writer;

    boolean populationMode = false; // quick & dirty hack for multi component export

    private final List<File> outputFiles = new ArrayList<File>();
    
    UnitConverter unitConverter = new SIUnitConverter();
    
    String TIME_DIM = Dimension.getTimeDimension().getName();
    boolean onlyFlattenIfNecessary = false;

    public void setPopulationMode(boolean mode)
    {
        populationMode = mode;
    }

    public void setOnlyFlattenIfNecessary(boolean onlyFlattenIfNecessary)
    {
        this.onlyFlattenIfNecessary = onlyFlattenIfNecessary;
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

        String targetId = simCpt.getStringValue("target");

        Component tgtComp = lems.getComponent(targetId);
        
        g.writeStringField(DLemsKeywords.NAME.get(), targetId);

        ArrayList<Component> pops = tgtComp.getChildrenAL("populations");

        ArrayList<Component> projs = tgtComp.getChildrenAL("projections");
        projs.addAll(tgtComp.getChildrenAL("synapticConnections"));
        
        if (pops.size() > 0)
        {
            ArrayList<String> written = new ArrayList<String>();
            ArrayList<String> writtenTypes = new ArrayList<String>();

            if (populationMode)
            {
                g.writeObjectFieldStart(DLemsKeywords.SYNAPSES.get());

            }
            for (Component proj : projs)
            {
                String synRef = proj.getStringValue("synapse");
                String synName = proj.getStringValue("synapse");

                if (!written.contains(synRef))
                {
                    //System.out.println("-             Adding " + synRef);
                    Component synComp = lems.getComponent(synRef);

                    if (!writtenTypes.contains(synComp.getTypeName()))
                    {
                        createFlattenedCompType(synComp);
                        writtenTypes.add(synComp.getTypeName());
                    }
                    Component cpFlat = createFlattenedComp(synComp);
                    if (populationMode)
                    {

                        g.writeObjectFieldStart(synName);

                        g.writeObjectFieldStart(DLemsKeywords.SYNAPSE.get());
                        writeDLemsForComponent(g, cpFlat);
                        g.writeEndObject();

                        g.writeEndObject();
                    }

                }

            }

            if (populationMode)
            {
                g.writeEndObject();
                g.writeObjectFieldStart(DLemsKeywords.POPULATIONS.get());
            }
            HashMap<String,Component> flatComps = new HashMap<String, Component>();
            for (Component pop : pops)
            {
                String compRef = pop.getStringValue("component");
                String popName = pop.getID();

                //System.out.println("---------       Adding " + compRef);
                Component popComp = lems.getComponent(compRef);
                
                if (false && popComp.getComponentType().isOrExtends("cell")) 
                {
                    //...
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
                        flatComps.put(compRef, cpFlat);
                        if (populationMode)
                        {
                            StringWriter swComp = new StringWriter();
                            JsonGenerator gComp = f.createJsonGenerator(swComp);
                            gComp.useDefaultPrettyPrinter();
                            gComp.writeStartObject();

                            writeDLemsForComponent(gComp, cpFlat);

                            gComp.writeEndObject();
                            gComp.close();

                            File compFile = new File(this.getOutputFolder(), cpFlat.getID() + ".cell.json");
                            FileUtil.writeStringToFile(swComp.toString(), compFile);
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

                    g.writeStringField(DLemsKeywords.SIZE.get(), pop.getStringValue("size"));

                    g.writeObjectFieldStart(DLemsKeywords.COMPONENT.get());
                    if (false && popComp.getComponentType().isOrExtends("cell"))
                    {
                        g.writeStringField(DLemsKeywords.NAME.get(), popComp.getID());
                        g.writeStringField(DLemsKeywords.TYPE.get(), popComp.getComponentType().getName());
                    } 
                    else 
                    {
                        writeDLemsForComponent(g, flatComps.get(compRef));
                    }
                    g.writeEndObject();

                    g.writeEndObject();
                }
            }

            if (populationMode)
            {

                g.writeEndObject();

                for (Component proj : projs)
                {
                    String synRef = proj.getStringValue("synapse");
                    //System.out.println("-             Adding " + synRef);
                    Component synComp = lems.getComponent(synRef);
                    if (!writtenTypes.contains(synComp.getTypeName()))
                    {
                        createFlattenedCompType(synComp);
                        writtenTypes.add(synComp.getTypeName());
                    }
                    Component cpFlat = createFlattenedComp(synComp);
                    StringWriter swComp = new StringWriter();
                    JsonGenerator gComp = f.createJsonGenerator(swComp);
                    gComp.useDefaultPrettyPrinter();
                    gComp.writeStartObject();

                    writeDLemsForComponent(gComp, cpFlat);

                    gComp.writeEndObject();
                    gComp.close();

                    File synFile = new File(this.getOutputFolder(), cpFlat.getID() + ".synapse.json");
                    FileUtil.writeStringToFile(swComp.toString(), synFile);
                    outputFiles.add(synFile);

                    
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
                    System.out.println("cc"+il);
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
                    g.writeObjectFieldStart(proj.id);
                    g.writeStringField(DLemsKeywords.PRE_POPULATION.get(), proj.getStringValue("presynapticPopulation"));
                    g.writeStringField(DLemsKeywords.POST_POPULATION.get(), proj.getStringValue("postsynapticPopulation"));
                    g.writeStringField(DLemsKeywords.SYNAPSE.get(), proj.getStringValue("synapse"));
                    
                    g.writeArrayFieldStart(DLemsKeywords.CONNECTIONS.get());
                    for (Component conn: proj.getChildrenAL("connections"))
                    {
                        g.writeStartObject();
                        g.writeStringField(DLemsKeywords.NAME.get(), conn.id);
                        String pre = conn.getStringValue("preCellId").substring(3);  // remove ../
                        LEMSQuantityPath lqpPre = new LEMSQuantityPath(pre+"/xxx");
                        String post = conn.getStringValue("postCellId").substring(3);  // remove ../
                        LEMSQuantityPath lqpPost = new LEMSQuantityPath(post+"/xxx");
                        g.writeStringField(DLemsKeywords.PRE_CELL_ID.get(), lqpPre.getPopulationIndex()+"");
                        g.writeStringField(DLemsKeywords.POST_CELL_ID.get(), lqpPost.getPopulationIndex()+"");
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

    private void writeDLemsForComponent(JsonGenerator g, Component comp) throws ContentError, JsonGenerationException, IOException
    {

        g.writeObjectFieldStart(DLemsKeywords.DYNAMICS.get());
        writeDynamics(g, comp);
        g.writeEndObject();

        g.writeArrayFieldStart(DLemsKeywords.EVENTS.get());
        writeEvents(g, comp);
        g.writeEndArray();

        g.writeStringField(DLemsKeywords.NAME.get(), comp.getID());
        g.writeStringField(DLemsKeywords.TYPE.get(), comp.getComponentType().getName());

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
    
    private String convertTime(ParamValue lemsValue) throws ContentError
    {
        return (float)unitConverter.convert(lemsValue.getDoubleValue(),TIME_DIM)+"";
    }

    private void writeSimulationInfo(JsonGenerator g, Component simCpt) throws ContentError, JsonGenerationException, IOException
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
                //g.writeStringField(DLemsKeywords., "t");
                g.writeEndObject();
                        
                for (Component outputColumn : outFile.getAllChildren())
                {
                    if (outputColumn.getTypeName().equals("OutputColumn"))
                    {
                        g.writeStartObject();
                        String quantity = outputColumn.getStringValue("quantity");
                        
                        LEMSQuantityPath lqp = new LEMSQuantityPath(quantity);


                        g.writeStringField(DLemsKeywords.NAME.get(), outputColumn.getID());
                        g.writeStringField(DLemsKeywords.POPULATION.get(), lqp.getPopulation());
                        g.writeStringField(DLemsKeywords.POPULATION_INDEX.get(), lqp.getPopulationIndex()+"");
                        g.writeStringField(DLemsKeywords.VARIABLE.get(), lqp.getVariable());
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

                        g.writeStringField(DLemsKeywords.ORDINATE.get(), lqp.getVariable());

                        g.writeStringField(DLemsKeywords.POPULATION.get(), lqp.getPopulation());
                        g.writeStringField(DLemsKeywords.POPULATION_INDEX.get(), lqp.getPopulationIndex()+"");
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

    private void writeParameters(JsonGenerator g, Component comp) throws ContentError, JsonGenerationException, IOException
    {
        ComponentType ct = comp.getComponentType();

        for (FinalParam p : ct.getFinalParams())
        {
            ParamValue pv = comp.getParamValue(p.getName());

            g.writeStringField(p.getName(), (float)unitConverter.convert(pv.getDoubleValue(), pv.getDimensionName()) + "");
        }

        for (Constant c : ct.getConstants())
        {
            g.writeStringField(c.getName(), (float)unitConverter.convert(c.getValue(), c.getDimension().getName()) + "");
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

    private ComponentType createFlattenedCompType(Component compOrig) throws ContentError, ParseError
    {

        ComponentType ctFlat = new ComponentType();
        ComponentFlattener cf = new ComponentFlattener(lems, compOrig, true, true);
        
        if (onlyFlattenIfNecessary && !cf.requiresFlattenning()) {
            return compOrig.getComponentType();
        }

        try
        {
            ctFlat = cf.getFlatType();
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

        if (onlyFlattenIfNecessary && !cf.requiresFlattenning()) {
            return compOrig;
        }
        
        try
        {
            comp = cf.getFlatComponent();
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
        //lemsFiles.add(new File("../OpenCortex/NeuroML2/LEMS_SimpleNet.xml"));
        //lemsFiles.add(new File("../OpenCortex/NeuroML2/LEMS_SpikingNet.xml"));
        lemsFiles.add(new File("../neuroConstruct/osb/generic/hodgkin_huxley_tutorial/Tutorial/Source/LEMS_HH_Simulation.xml"));

        for (File lemsFile : lemsFiles)
        {
            Lems lems = Utils.readLemsNeuroMLFile(lemsFile).getLems();
            DLemsWriter dw = new DLemsWriter(lems, lemsFile.getParentFile(), lemsFile.getName().replaceAll(".xml", ".json"), null, false);
            dw.setPopulationMode(true);
            dw.setOnlyFlattenIfNecessary(true);

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

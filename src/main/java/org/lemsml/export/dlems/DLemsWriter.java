package org.lemsml.export.dlems;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
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
import org.lemsml.jlems.core.run.ConnectionError;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.sim.LEMSException;
import org.lemsml.jlems.core.type.Component;
import org.lemsml.jlems.core.type.ComponentType;
import org.lemsml.jlems.core.type.Constant;
import org.lemsml.jlems.core.type.DerivedParameter;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.core.type.ParamValue;
import org.lemsml.jlems.core.type.Parameter;
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
import org.neuroml.export.utils.Format;
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

    public void setPopulationMode(boolean mode)
    {
        populationMode = mode;
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

    @Override
    public void setSupportedFeatures()
    {
        sli.addSupportInfo(format, ModelFeature.ABSTRACT_CELL_MODEL, SupportLevelInfo.Level.MEDIUM);
        sli.addSupportInfo(format, ModelFeature.COND_BASED_CELL_MODEL, SupportLevelInfo.Level.LOW);
        sli.addSupportInfo(format, ModelFeature.SINGLE_COMP_MODEL, SupportLevelInfo.Level.MEDIUM);
        sli.addSupportInfo(format, ModelFeature.NETWORK_MODEL, SupportLevelInfo.Level.LOW);
        sli.addSupportInfo(format, ModelFeature.MULTI_CELL_MODEL, SupportLevelInfo.Level.NONE);
        sli.addSupportInfo(format, ModelFeature.MULTI_POPULATION_MODEL, SupportLevelInfo.Level.NONE);
        sli.addSupportInfo(format, ModelFeature.NETWORK_WITH_INPUTS_MODEL, SupportLevelInfo.Level.NONE);
        sli.addSupportInfo(format, ModelFeature.NETWORK_WITH_PROJECTIONS_MODEL, SupportLevelInfo.Level.NONE);
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

        g.writeStringField(DLemsKeywords.DT.get(), simCpt.getParamValue("step").stringValue());

        String targetId = simCpt.getStringValue("target");

        Component tgtComp = lems.getComponent(targetId);

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
                    System.out.println("-             Adding " + synRef);
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
            for (Component pop : pops)
            {

                String compRef = pop.getStringValue("component");
                String popName = pop.getID();

                if (!written.contains(compRef))
                {
                    System.out.println("---------       Adding " + compRef);
                    Component popComp = lems.getComponent(compRef);

                    if (!writtenTypes.contains(popComp.getTypeName()))
                    {
                        createFlattenedCompType(popComp);
                        writtenTypes.add(popComp.getTypeName());
                    }

                    Component cpFlat = createFlattenedComp(popComp);
                    if (populationMode)
                    {
                        StringWriter swComp = new StringWriter();
                        JsonGenerator gComp = f.createJsonGenerator(swComp);
                        gComp.useDefaultPrettyPrinter();
                        gComp.writeStartObject();

                        writeDLemsForComponent(gComp, cpFlat);

                        gComp.writeEndObject();
                        gComp.close();

                        File compFile = new File(this.getOutputFolder(), cpFlat.getID() + ".json");
                        FileUtil.writeStringToFile(swComp.toString(), compFile);
                        outputFiles.add(compFile);

                        g.writeObjectFieldStart(popName);

                        g.writeStringField(DLemsKeywords.SIZE.get(), pop.getStringValue("size"));

                        g.writeObjectFieldStart(DLemsKeywords.COMPONENT.get());
                        writeDLemsForComponent(g, cpFlat);
                        g.writeEndObject();

                        g.writeEndObject();

                    } else
                    {
                        writeDLemsForComponent(g, cpFlat);
                    }

                    written.add(compRef);
                }
            }

            if (populationMode)
            {

                g.writeEndObject();

                for (Component proj : projs)
                {
                    String synRef = proj.getStringValue("synapse");
                    System.out.println("-             Adding " + synRef);
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

    private void writeSimulationInfo(JsonGenerator g, Component simCpt) throws ContentError, JsonGenerationException, IOException
    {
        g.writeStringField(DLemsKeywords.T_END.get(), simCpt.getParamValue("length").stringValue());
        g.writeStringField(DLemsKeywords.T_START.get(), "0");

        for (Component dispComp : simCpt.getAllChildren())
        {
            if (dispComp.getTypeName().equals("OutputFile"))
            {
                g.writeStringField(DLemsKeywords.DUMP_TO_FILE.get(), dispComp.getStringValue("fileName"));
            }
        }

        g.writeArrayFieldStart(DLemsKeywords.DISPLAY.get());

        for (Component dispComp : simCpt.getAllChildren())
        {
            if (dispComp.getTypeName().equals("Display"))
            {

                g.writeStartObject();

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
                        g.writeStringField(DLemsKeywords.ABSCISSA.get(), "t");
                        String quantity = lineComp.getStringValue("quantity");

                        g.writeStringField(DLemsKeywords.ORDINATE.get(), quantity.substring(quantity.indexOf("/") + 1));
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

        for (Parameter p : ct.getDimParams())
        {
            ParamValue pv = comp.getParamValue(p.getName());

            g.writeStringField(p.getName(), (float) pv.getDoubleValue() + "");
        }

        for (Constant c : ct.getConstants())
        {
            g.writeStringField(c.getName(), c.getValue() + "");
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
            System.out.println("..................oe " + oe);

            for (StateAssignment sa : oe.getStateAssignments())
            {
                System.out.println(".......................sa " + sa);
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

        try
        {
            ctFlat = cf.getFlatType();
            lems.addComponentType(ctFlat);
            String typeOut = XMLSerializer.serialize(ctFlat);
            // E.info("Flat type: \n" + typeOut);
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

        try
        {
            comp = cf.getFlatComponent();
            lems.addComponent(comp);
            String compOut = XMLSerializer.serialize(comp);
            //E.info("Flat component: \n" + compOut.ge);
            lems.resolve(comp);
        } catch (ConnectionError e)
        {
            throw new ParseError("Error when flattening component: " + compOrig, e);
        }
        return comp;
    }

    public static void main(String[] args) throws Exception
    {

        ArrayList<File> lemsFiles = new ArrayList<File>();
        //lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex0_IaF.xml"));
        lemsFiles.add(new File("../neuroConstruct/osb/cerebral_cortex/networks/IzhikevichModel/NeuroML2/LEMS_SmallNetwork.xml"));
        //lemsFiles.add(new File("../neuroConstruct/osb/cerebral_cortex/networks/IzhikevichModel/NeuroML2/LEMS_2007Cells.xml"));
        //lemsFiles.add(new File("../neuroConstruct/osb/cerebral_cortex/networks/IzhikevichModel/NeuroML2/LEMS_2007One.xml"));

        for (File lemsFile : lemsFiles)
        {
            Lems lems = Utils.readLemsNeuroMLFile(lemsFile).getLems();
            DLemsWriter dw = new DLemsWriter(lems, lemsFile.getParentFile(), lemsFile.getName().replaceAll(".xml", ".json"), null, false);
            dw.setPopulationMode(true);
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

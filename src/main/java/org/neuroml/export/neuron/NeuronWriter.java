package org.neuroml.export.neuron;


import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import javax.xml.bind.JAXBException;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.lemsml.export.base.GenerationException;
import org.lemsml.export.dlems.DLemsWriter;
import org.lemsml.jlems.core.expression.ParseError;

import org.lemsml.jlems.core.type.dynamics.DerivedVariable;
import org.lemsml.jlems.core.type.dynamics.OnCondition;
import org.lemsml.jlems.core.type.dynamics.OnEntry;
import org.lemsml.jlems.core.type.dynamics.OnEvent;
import org.lemsml.jlems.core.type.dynamics.OnStart;
import org.lemsml.jlems.core.type.dynamics.Regime;
import org.lemsml.jlems.core.type.dynamics.StateAssignment;
import org.lemsml.jlems.core.type.dynamics.StateVariable;
import org.lemsml.jlems.core.type.dynamics.TimeDerivative;
import org.lemsml.jlems.core.type.dynamics.Transition;
import org.lemsml.jlems.core.type.Component;
import org.lemsml.jlems.core.type.Target;
import org.lemsml.jlems.core.type.Dimension;
import org.lemsml.jlems.core.type.Exposure;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.core.type.LemsCollection;
import org.lemsml.jlems.core.type.ParamValue;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.logging.E;
import org.lemsml.jlems.core.type.dynamics.Dynamics;
import org.lemsml.jlems.io.util.FileUtil;
import org.neuroml.export.Utils;
import org.neuroml.export.base.BaseWriter;
import org.neuroml.model.BiophysicalProperties;
import org.neuroml.model.Cell;
import org.neuroml.model.ChannelDensity;
import org.neuroml.model.Input;
import org.neuroml.model.IntracellularProperties;
import org.neuroml.model.Member;
import org.neuroml.model.MembraneProperties;
import org.neuroml.model.Morphology;
import org.neuroml.model.Point3DWithDiam;
import org.neuroml.model.Resistivity;
import org.neuroml.model.Segment;
import org.neuroml.model.SegmentGroup;
import org.neuroml.model.SpecificCapacitance;
import org.neuroml.model.Standalone;
import org.neuroml.model.util.NeuroMLElements;

@SuppressWarnings("StringConcatenationInsideStringBufferAppend")
public class NeuronWriter extends BaseWriter {

    final static String NEURON_FORMAT = "NEURON";
    
    final static String NEURON_VOLTAGE = "v";
    final static String NEURON_TEMP = "celsius";
    
    //final static String HIGH_CONDUCTANCE_PARAM = "highConductance";
    final static String RESERVED_STATE_SUFFIX = "I";
    final static String RATE_PREFIX = "rate_";
    final static String REGIME_PREFIX = "regime_";
    final static String V_COPY_PREFIX = "copy_";
    
    final static String DUMMY_POPULATION_PREFIX = "population_";


    ArrayList<File> allGeneratedFiles = new ArrayList<File>();
    static final int commentOffset = 40;

    static boolean debug = false;
    
	private static final String cellTemplateFile = "neuron/cell.vm";
    

    public NeuronWriter(Lems l) {
        super(l, NEURON_FORMAT);
    }

    @Override
    protected void addComment(StringBuilder sb, String comment) {

        String comm = "# ";
        String commPre = "'''";
        String commPost = "'''";
        if (comment.indexOf("\n") < 0) {
            sb.append(comm + comment + "\n");
        } else {
            sb.append(commPre + "\n" + comment + "\n" + commPost + "\n");
        }
    }

    private void reset() {
        allGeneratedFiles.clear();
    }

    public ArrayList<File> generateMainScriptAndMods(File mainFile) throws ContentError, ParseError, IOException, JAXBException {
        String main = generate(mainFile.getParentFile());
        try {
            FileUtil.writeStringToFile(main, mainFile);
            allGeneratedFiles.add(mainFile);
        } catch (IOException ex) {
            throw new ContentError("Error writing to file: " + mainFile.getAbsolutePath(), ex);
        }
        return allGeneratedFiles;
    }

    @Override
    public String getMainScript() throws GenerationException {
        try {
            return generate(null);
        } catch (ContentError e) {
            throw new GenerationException("Error with LEMS content", e);
        } catch (ParseError e) {
            throw new GenerationException("Error parsing LEMS content", e);
        } catch (IOException e) {
            throw new GenerationException("Error with file I/O", e);
        } catch (JAXBException e)
        {
            throw new GenerationException("Error with parsing XML", e);
        }
        
    }

    private static String getStateVarName(String sv) {
        if (sv.equals(NEURON_VOLTAGE)) {
            return NEURON_VOLTAGE + RESERVED_STATE_SUFFIX;
        } else {
            return sv;
        }
    }

    private static String checkForBinaryOperators(String expr)
    {
        return expr.replace(".gt.", ">").replace(".geq.", ">=").replace(".lt.", "<").replace(".leq.", "<=").replace(".and.", "&&");
    }

    private static String checkForStateVarsAndNested(String expr, Component comp, HashMap<String, HashMap<String, String>> paramMappings) {

        if (expr == null) {
            return null;
        }

        String newExpr = expr.trim();

        newExpr = newExpr.replaceAll("greater_than_or_equal_to", ">=");
        newExpr = newExpr.replaceAll("greater_than", ">");
        newExpr = newExpr.replaceAll("less_than_or_equal_to", "<=");
        newExpr = newExpr.replaceAll("less_than", "<=");
        newExpr = newExpr.replaceAll("equal_to", "==");

        newExpr = newExpr.replaceAll(" ln\\(", " log(");

        HashMap<String, String> paramMappingsComp = paramMappings.get(comp.getUniqueID());
        //E.info("Making mappings in "+newExpr+" for "+comp+": "+paramMappingsComp);
        for (String origName : paramMappingsComp.keySet()) {
            String newName = paramMappingsComp.get(origName);
            newExpr = Utils.replaceInExpression(newExpr, origName, newName);
        }

        return newExpr;
    }


    public String generate(File dirForMods) throws ContentError, ParseError, IOException, JAXBException {

        reset();
        StringBuilder main = new StringBuilder();

        addComment(main, "Neuron simulator export for:\n\n" + lems.textSummary(false, false)+"\n\n"+Utils.getHeaderComment(format));

        main.append("import neuron\n");
        main.append("h = neuron.h\n");
        main.append("h.load_file(\"nrngui.hoc\")\n\n");
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

        E.info("popsOrComponents: "+popsOrComponents);

        HashMap<String, Integer> compMechsCreated = new HashMap<String, Integer>();
        HashMap<String, String> compMechNamesHoc = new HashMap<String, String>();
        
        HashMap<String, Cell> cellsVsCompIds = new HashMap<String, Cell>();
        
        boolean simulatingNetwork = true;
        
        if (popsOrComponents.isEmpty()) {
        	popsOrComponents.add(targetComp);
        	simulatingNetwork = false;
        }

        for (Component popsOrComponent : popsOrComponents) {
        	
        	String compReference;
        	String popName;
        	int number;
        	Component popComp;
        	
            boolean isCondBasedCell = false;
            
        	if (popsOrComponent.getComponentType().getName().equals(NeuroMLElements.POPULATION)) {
        		compReference = popsOrComponent.getStringValue("component");
        		number = Integer.parseInt(popsOrComponent.getStringValue("size"));
                popComp = lems.getComponent(compReference);
                popName = popsOrComponent.getID();
        	} else if (popsOrComponent.getComponentType().getName().equals(NeuroMLElements.POPULATION_LIST)) {
        		compReference = popsOrComponent.getStringValue("component");
                popComp = lems.getComponent(compReference);
                number = 0;
                for (Component comp: popsOrComponent.getAllChildren()) {
                    
                    //main.append("print \""+comp+"\"");
                    if (comp.getComponentType().getName().equals(NeuroMLElements.INSTANCE))
                        number++;
                }
                        popComp.getAllChildren().size();
                popName = popsOrComponent.getID();
                isCondBasedCell = true;
        	} else {
        		//compReference = popsOrComponent.getComponentType().getName();
        		number = 1;
                popComp = popsOrComponent;
                popName = DUMMY_POPULATION_PREFIX+popComp.getName();
                
        	}
        	
            ArrayList<String> generatedModComponents = new ArrayList<String>();

            String mechName = popComp.getComponentType().getName();


            main.append("print \"Population " + popName + " contains " + number + " instance(s) of component: "
                    + popComp.getID() + " of type: " + popComp.getComponentType().getName() + " \"\n\n");

            // Build the mod file for the Type
            
            if (popComp.getComponentType().isOrExtends(NeuroMLElements.CELL_COMP_TYPE)) {
                                
                Cell cell = getCellFromComponent(popComp);
                cellsVsCompIds.put(popComp.getID(), cell);
                String cellString = generateCellFile(cell);
                String cellName = popComp.getID();

                String fileName = cellName + ".hoc";
                File cellFile = new File(dirForMods, fileName);
                E.info("Writing to: " + cellFile);
                
                main.append("h.load_file(\""+fileName+"\")\n");
                
                main.append(popName+" = []\n");
                
                main.append("n_"+popName+" = "+number+"\n");
                
                main.append("h(\"objectvar "+popName+"[%i]\"%n_"+popName+")\n");
                
                main.append("for i in range(n_"+popName+"):\n");
                //main.append("    cell = h."+cellName+"()\n");
                main.append("    h(\""+popName+"[%i] = new "+cellName+"()\"%i)\n");
                //main.append("    cell."+getNrnSectionName(cell.getMorphology().getSegment().get(0))+".push()\n");
                main.append("    h(\"access "+popName+"[%i]."+getNrnSectionName(cell.getMorphology().getSegment().get(0))+"\"%i)\n");
                
                try {
                    FileUtil.writeStringToFile(cellString, cellFile);
                    allGeneratedFiles.add(cellFile);
                } catch (IOException ex) {
                    throw new ContentError("Error writing to file: " + cellFile.getAbsolutePath(), ex);
                }
                
                for (ChannelDensity cd: cell.getBiophysicalProperties().getMembraneProperties().getChannelDensity())
                {
                    String ionChannel = cd.getIonChannel();
                    if (!generatedModComponents.contains(ionChannel)) {
                        Component ionChannelComp = lems.getComponent(ionChannel);
                        String mod = generateModFile(ionChannelComp);
                        generatedModComponents.add(ionChannel);

                        File modFile = new File(dirForMods, ionChannelComp.getID() + ".mod");
                        E.info("-- Writing to: " + modFile);

                        try {
                            FileUtil.writeStringToFile(mod.toString(), modFile);
                            allGeneratedFiles.add(modFile);
                        } catch (IOException ex) {
                            throw new ContentError("Error writing to file: " + modFile.getAbsolutePath(), ex);
                        }
                    }
                }
                
            } else {
                String mod = generateModFile(popComp);

                File modFile = new File(dirForMods, popComp.getComponentType().getName() + ".mod");
                E.info("Writing to: " + modFile);

                try {
                    FileUtil.writeStringToFile(mod.toString(), modFile);
                    allGeneratedFiles.add(modFile);
                } catch (IOException ex) {
                    throw new ContentError("Error writing to file: " + modFile.getAbsolutePath(), ex);
                }
                
                for (int i = 0; i < number; i++) {
                    String instName = popName + "_" + i;
                    main.append(instName + " = h.Section()\n");
                    double defaultRadius = 5;
                    main.append(instName + ".L = " + defaultRadius * 2 + "\n");
                    main.append(instName + "(0.5).diam = " + defaultRadius * 2 + "\n");

                    if (popComp.getComponentType().isOrExtends(NeuroMLElements.BASE_CELL_CAP_COMP_TYPE)) {
                        double capTotSI = popComp.getParamValue("C").getDoubleValue();
                        double area = 4 * Math.PI * defaultRadius * defaultRadius;
                        double specCapNeu = 10e13 * capTotSI / area;
                        main.append(instName + "(0.5).cm = " + specCapNeu + "\n");
                    } else {
                        main.append(instName + "(0.5).cm = 318.31927\n");
                    }

                    main.append(instName + ".push()\n");
                    main.append(mechName + "_" + instName + " = h." + mechName + "(0.5, sec=" + instName + ")\n\n");

                    if (!compMechsCreated.containsKey(mechName)) {
                        compMechsCreated.put(mechName, 0);
                    }

                    compMechsCreated.put(mechName, compMechsCreated.get(mechName) + 1);

                    String hocMechName = mechName + "[" + (compMechsCreated.get(mechName) - 1) + "]";

                    compMechNamesHoc.put(instName, hocMechName);

                    LemsCollection<ParamValue> pvs = popComp.getParamValues();
                    for (ParamValue pv : pvs) {
                        main.append("h." + hocMechName + "." + pv.getName() + " = "
                                + convertToNeuronUnits((float) pv.getDoubleValue(), pv.getDimensionName()) + "\n");
                    }
                }
            }


        }
        
        
        ArrayList<Component> inputLists = targetComp.getChildrenAL("inputs");
        for (Component inputList : inputLists) {
            String inputReference = inputList.getStringValue("component");
            Component inputComp = lems.getComponent(inputReference);
            
            String mod = generateModFile(inputComp);

            File modFile = new File(dirForMods, inputComp.getID() + ".mod");
            E.info("Writing to: " + modFile);

            try {
                FileUtil.writeStringToFile(mod.toString(), modFile);
                allGeneratedFiles.add(modFile);
            } catch (IOException ex) {
                throw new ContentError("Error writing to file: " + modFile.getAbsolutePath(), ex);
            }
            
            String pop = inputList.getStringValue("population");
            ArrayList<Component> inputs = inputList.getChildrenAL("inputs");
            
            for (Component input: inputs) {
                String targetString = input.getStringValue("target");
                String[] parts = targetString.split("/");
                String cellNum = parts[2];
                String cellId = parts[3];
                Cell cell = cellsVsCompIds.get(cellId);
                String inputName = inputList.getID()+ "_"+input.getID();
                main.append(String.format("h(\"objectvar %s\")\n", inputName));
                main.append(String.format("h(\"%s[%s].%s { %s = new %s(0.5) } \")\n\n", pop, cellNum, getNrnSectionName(cell.getMorphology().getSegment().get(0)), inputName, inputComp.getID()));
                
            }
            
        }

        addComment(main, "The following code is based on Andrew's test_HH.py example...");

        main.append("trec = h.Vector()\n");
        main.append("trec.record(h._ref_t)\n\n");

        StringBuilder toRec = new StringBuilder();
        StringBuilder toPlot = new StringBuilder();


        ArrayList<String> displayGraphs = new ArrayList<String>();

        for (Component dispComp : simCpt.getAllChildren()) {
            if (dispComp.getName().indexOf("Display") >= 0) {
                toRec.append("# Display: " + dispComp + "\n");
                String dispId = dispComp.getID();
                int plotColour = 1;

                for (Component lineComp : dispComp.getAllChildren()) {
                    if (lineComp.getName().indexOf("Line") >= 0) {
                        //trace=StateMonitor(hhpop,'v',record=[0])
                        /////String trace = "trace_" + lineComp.getID();
                        String ref = lineComp.getStringValue("quantity");
                        String origRef = ref;

                        toRec.append("#   Line, recording: " + ref + "\n");
                        String pop;
                        String num;
                        String var;
                        String[] varParts = null;
                        Component popComp = null;
                        
                        if (ref.indexOf("/")<0 && !simulatingNetwork) {

                        	popComp = targetComp;
                        	var = ref;
                        	num = "0";
	                        pop = DUMMY_POPULATION_PREFIX+popComp.getName();

                        } else {
                            if (ref.indexOf('[')>0) {
                                pop = ref.split("/")[0].split("\\[")[0];
                                num = ref.split("\\[")[1].split("\\]")[0];
                                var = ref.split("/")[1];
                            }
                            else
                            {
                                String[] parts = ref.split("/");
                                pop = parts[0];
                                num = parts[1];
                                var = "";
                                varParts = new String[parts.length-2];
                                for (int i=2;i<parts.length;i++) {
                                    if (i>2)
                                        var +="_";
                                    var +=parts[i];
                                    varParts[i-2]=parts[i];
                                }
                            }
	

	                        for (Component popsOrComponent : popsOrComponents) {
	                            if (popsOrComponent.getID().equals(pop)) {
	                                popComp = lems.getComponent(popsOrComponent.getStringValue("component"));
	                            }
	                        }
                        }
	
                        System.out.println("Recording " + var + " on cell " + num + " in " + pop + " of type " + popComp +" (was: "+origRef+")");

                        String varRef = pop + "_" + num;

                        varRef = compMechNamesHoc.get(varRef) + "." + var;
                        
                        boolean isCondBasedCell = popComp.getComponentType().isOrExtends(NeuroMLElements.CELL_COMP_TYPE);

                        if (var.equals(NEURON_VOLTAGE)) {
                            varRef = compMechNamesHoc.get(pop + "_" + num) + "." + V_COPY_PREFIX + var;

                        }
                        if (isCondBasedCell) {
                            if (varParts==null) {
                                Cell cell = cellsVsCompIds.get(popComp.getID());
                                String varInst = getNrnSectionName(cell.getMorphology().getSegment().get(0));
                                varRef = pop + "[" + num + "]." + varInst+"."+var;
                            }
                            else {
                                Cell cell = cellsVsCompIds.get(varParts[0]);
                                String varInst = getNrnSectionName(cell.getMorphology().getSegment().get(0));
                                for(int i=1; i<varParts.length; i++)
                                {
                                    varInst +="."+varParts[i];
                                }
                                varRef = pop + "[" + num + "]." + varInst;
                            }

                        }
                        System.out.println("");

                        String dispGraph = "display_" + dispId;
                        if (!displayGraphs.contains(dispGraph)) {
                            displayGraphs.add(dispGraph);
                        }
                        
                        String dim = "None";
                        try {
                            Exposure exp = popComp.getComponentType().getExposure(var);
                            dim = exp.getDimension().getName();
                        } catch(Exception e) {
                            
                        }

                        float scale = 1 / convertToNeuronUnits((float) lineComp.getParamValue("scale").getDoubleValue(),
                                dim);

                        String plotRef = "\"" + varRef + "\"";

                        if (scale != 1) {
                            plotRef = "\"(" + scale + ") * " + varRef + "\"";
                        }

                        toPlot.append(dispGraph + ".addexpr(" + plotRef + ", " + plotRef + ", " + plotColour + ", 1, 0.8, 0.9, 2)\n");
                        plotColour++;
                        if (plotColour > 10) {
                            plotColour = 1;
                        }
                    

                    }
                }
            }
        }
        main.append(toRec);

        String len = simCpt.getStringValue("length");
        len = len.replaceAll("ms", "");
        if (len.indexOf("s") > 0) {
            len = len.replaceAll("s", "").trim();
            len = "" + Float.parseFloat(len) * 1000;
        }

        String dt = simCpt.getStringValue("step");
        dt = dt.replaceAll("ms", "").trim();
        if (dt.indexOf("s") > 0) {
            dt = dt.replaceAll("s", "").trim();
            dt = "" + Float.parseFloat(dt) * 1000;
        }

        main.append("h.tstop = " + len + "\n\n");
        main.append("h.dt = " + dt + "\n\n");
        main.append("h.steps_per_ms = " + (float) (1d / Double.parseDouble(dt)) + "\n\n");

        //main.append("objref SampleGraph\n");
        for (String dg : displayGraphs) {
            main.append(dg + " = h.Graph(0)\n");
            main.append(dg + ".size(0,h.tstop,-80.0,50.0)\n");
            main.append(dg + ".view(0, -80.0, h.tstop, 130.0, 80, 330, 330, 250)\n");
            main.append("h.graphList[0].append(" + dg + ")\n");
        }

        main.append(toPlot);
        main.append("\n\n");


        main.append("h.nrncontrolmenu()\n");

        main.append("h.run()\n");

        main.append("print \"Done\"\n\n");

        return main.toString();
    }

    public static Cell getCellFromComponent(Component comp) throws ContentError, ParseError, IOException, JAXBException {
        ArrayList<Standalone> els = Utils.convertLemsComponentToNeuroML(comp);
        Cell cell = (Cell)els.get(0);
        return cell;
    }
        
    public static String generateCellFile(Cell cell) throws ContentError, ParseError, IOException, JAXBException {
        StringBuilder cellString = new StringBuilder();

        
        cellString.append("// Cell: "+cell.getId()+"\n");
        String json = cellToJson(cell, SupportedUnits.NEURON);
        cellString.append("/*\n"+json+"\n*/");
       
        
		Velocity.init();
		
		VelocityContext context = new VelocityContext();
     
        DLemsWriter.putIntoVelocityContext(json, context);

        Properties props = new Properties();
        props.put("resource.loader", "class");
        props.put("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        VelocityEngine ve = new VelocityEngine();
        ve.init(props);
        Template template = ve.getTemplate(cellTemplateFile);

        StringWriter sw1 = new StringWriter();

        template.merge( context, sw1 );
     
        cellString.append(sw1.toString());
        
        return cellString.toString();
    }
    
    //TODO: to be made more general
    public enum SupportedUnits{
        SI(1,1,1,1, 1), 
        PHYSIOLOGICAL(1000, 1e-6f, 100, 0.1f, 0.1f), 
        NEURON(1000, 1e-6f, 100, 100, 1e-4f);
        
        SupportedUnits(float voltageFactor, float lengthFactor, float specCapFactor, float resistivityFactor, float condDensFactor) {
            this.voltageFactor = voltageFactor;
            this.lengthFactor = lengthFactor;
            this.specCapFactor = specCapFactor;
            this.resistivityFactor = resistivityFactor;
            this.condDensFactor = condDensFactor;
        }
        float voltageFactor;
        float lengthFactor;
        float specCapFactor;
        float resistivityFactor;
        float condDensFactor;
    };
    
    private static String getNrnSectionName(Segment seg) {
        
        return (seg.getName()!=null && seg.getName().length()>0) ? "Section_"+seg.getName() : "Section_"+seg.getId();
    }
    
    /*
     * TODO: move to other class as it will be useful for MOOSE, etc.
     */
	public static String cellToJson(Cell cell, SupportedUnits units) throws ContentError, ParseError, IOException
	{
		JsonFactory f = new JsonFactory();
		StringWriter sw = new StringWriter();
		JsonGenerator g = f.createJsonGenerator(sw);
		g.useDefaultPrettyPrinter();
		g.writeStartObject();  

		g.writeStringField("id", cell.getId());
        if (cell.getNotes()!=null && cell.getNotes().length()>0)
        {
            g.writeStringField("notes", cell.getNotes());
        }
        
        g.writeArrayFieldStart("sections");
        Morphology morph = cell.getMorphology();
        HashMap<Integer, String> idsVsNames = new HashMap<Integer, String>();
        
        for (Segment seg: morph.getSegment()) {
            
            g.writeStartObject();
            String name = getNrnSectionName(seg);
            idsVsNames.put(seg.getId(), name);
            //g.writeObjectFieldStart(name);
            g.writeStringField("name",name);
			g.writeStringField("id",seg.getId()+"");
            String parent = (seg.getParent()==null || seg.getParent().getSegment()==null) ? "-1" : idsVsNames.get(seg.getParent().getSegment());
			g.writeStringField("parent",parent);
            String comments = null;
            g.writeArrayFieldStart("points3d");
            Point3DWithDiam p0 = seg.getDistal();
            g.writeString(String.format("%g, %g, %g, %g", p0.getX(), p0.getY(), p0.getZ(), p0.getDiameter()));
            Point3DWithDiam p1 = seg.getProximal();
            if (p0.getX() == p1.getX() &&
                p0.getY() == p1.getY() &&
                p0.getZ() == p1.getZ() &&
                p0.getDiameter() == p1.getDiameter())
            {
                comments = "Section in NeuroML is spherical, so using cylindrical section along Y axis in NEURON";
                p1.setY(p0.getDiameter());
            }
            g.writeString(String.format("%g, %g, %g, %g", p1.getX(), p1.getY(), p1.getZ(), p1.getDiameter()));
            g.writeEndArray();
            if (comments!=null)
                g.writeStringField("comments",comments);

            //g.writeEndObject();
            g.writeEndObject();
        }
        
        g.writeEndArray();
        
        g.writeArrayFieldStart("groups");
        boolean foundAll = false;
        for (SegmentGroup grp: morph.getSegmentGroup()) {
            g.writeStartObject();
			g.writeStringField("name", grp.getId());
            foundAll = grp.getId().equals("all");
            if (!grp.getMember().isEmpty()) {
                g.writeArrayFieldStart("sections");
                for (Member m: grp.getMember()) {
                    g.writeString(idsVsNames.get(m.getSegment()));
                }
                g.writeEndArray();
            }
            if (!grp.getInclude().isEmpty()) {
                g.writeArrayFieldStart("groups");
                for (org.neuroml.model.Include inc: grp.getInclude()) {
                    g.writeString(inc.getSegmentGroup());
                }
                g.writeEndArray();
            }
            g.writeEndObject();
        }
        if (!foundAll) {
            g.writeStartObject();
			g.writeStringField("name", "all");
            g.writeArrayFieldStart("sections");
            for (Segment seg: morph.getSegment()) {
                String name = getNrnSectionName(seg);
                g.writeString(name);
            }
            g.writeEndArray();
            g.writeEndObject();
            
        }
        g.writeEndArray();
        
        
        BiophysicalProperties bp = cell.getBiophysicalProperties();
        g.writeArrayFieldStart("specificCapacitance");
        MembraneProperties mp = bp.getMembraneProperties();
        for (SpecificCapacitance sc: mp.getSpecificCapacitance()) {
            g.writeStartObject();
            String group = sc.getSegmentGroup()==null ? "all" : sc.getSegmentGroup();
			g.writeStringField("group",group);
            float value = Utils.getMagnitudeInSI(sc.getValue()) * units.specCapFactor;
			g.writeStringField("value",formatDefault(value));
            g.writeEndObject();
            
        }
        g.writeEndArray();
        
        g.writeArrayFieldStart("resistivity");
        IntracellularProperties ip = bp.getIntracellularProperties();
        for (Resistivity res: ip.getResistivity()) {
            g.writeStartObject();
            String group = res.getSegmentGroup()==null ? "all" : res.getSegmentGroup();
			g.writeStringField("group",group);
            
            float value = Utils.getMagnitudeInSI(res.getValue()) * units.resistivityFactor;
			g.writeStringField("value",formatDefault(value));
            g.writeEndObject();
            
        }
        g.writeEndArray();
        
        
        g.writeArrayFieldStart("channelDensity");
        for (ChannelDensity cd: mp.getChannelDensity()) {
            g.writeStartObject();
			g.writeStringField("id",cd.getId());
			g.writeStringField("ionChannel",cd.getIonChannel());
            
            if (cd.getIon()!=null) {
                g.writeStringField("ion",cd.getIon());
            } else {
                g.writeStringField("ion","non_specific");
            }
                
            
            String group = cd.getSegmentGroup()==null ? "all" : cd.getSegmentGroup();
			g.writeStringField("group",group);
            
            float valueCondDens = Utils.getMagnitudeInSI(cd.getCondDensity())*units.condDensFactor;
			g.writeStringField("condDens",formatDefault(valueCondDens));
            
            float valueErev = Utils.getMagnitudeInSI(cd.getErev())*units.voltageFactor;
			g.writeStringField("erev",formatDefault(valueErev));
            
            g.writeEndObject();
        }
        g.writeEndArray();
        
        
        
		g.writeEndObject();
		g.close();
        System.out.println(sw.toString());

		return sw.toString();

    }
    
    private static DecimalFormat formatter = new DecimalFormat("#.#");
    public static String formatDefault(float num) 
    {
        return num +"";//formatter.format(num);
    }
    
    public static String generateModFile(Component comp) throws ContentError {
        StringBuilder mod = new StringBuilder();

        String mechName = comp.getComponentType().getName();
        
        mod.append("TITLE Mod file for component: " + comp + "\n\n");

        mod.append("COMMENT\n\n"+Utils.getHeaderComment(NEURON_FORMAT)+"\n\nENDCOMMENT\n\n");
        
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
        String blockNetReceiveParams;
        StringBuilder ratesMethod = new StringBuilder("\n");

        HashMap<String, HashMap<String, String>> paramMappings = new HashMap<String, HashMap<String, String>>();

        if (comp.getComponentType().isOrExtends(NeuroMLElements.BASE_CELL_COMP_TYPE)) {
            HashMap<String, String> paramMappingsComp = new HashMap<String, String>();
            ///paramMappingsComp.put(NEURON_VOLTAGE, getStateVarName(NEURON_VOLTAGE));
            paramMappings.put(comp.getUniqueID(), paramMappingsComp);
        }

        blockUnits.append("\n(nA) = (nanoamp)\n"
                + "(uA) = (microamp)\n"
                + "(mA) = (milliamp)\n"
                + "(mV) = (millivolt)\n"
                + "(mS) = (millisiemens)\n"
                + "(uS) = (microsiemens)\n"
                + "(molar) = (1/liter)\n"
                + "(mM) = (millimolar)\n"
                + "(um) = (micrometer)\n");
                /*+ "(S) = (siemens)\n"
                + "(um) = (micrometer)\n"
                + "(molar) = (1/liter)\n"
                + "(mM) = (millimolar)\n"
                + "(l) = (liter)\n")*/


        ArrayList<String> locals = new ArrayList<String>();

        boolean hasCaDependency = false;

        if (comp.getComponentType().isOrExtends(NeuroMLElements.ION_CHANNEL_COMP_TYPE)) {
            mechName = comp.getID();
            blockNeuron.append("SUFFIX " + mechName + "\n");

            String species = comp.getTextParam("species");
            if (species != null) {
                blockNeuron.append("USEION " + species + " READ e" + species + " WRITE i" + species + "\n"); //TODO check valence
            }

            for (Component child1: comp.getAllChildren()) {
                if (child1.getComponentType().isOrExtends(NeuroMLElements.BASE_GATE_COMP_TYPE)) {
                    //blockNeuron.append("? Checking " + child1 + "\n");
                    for (Component child2: child1.getAllChildren()) {
                        //blockNeuron.append("? Checking " + child2 + "\n");
                        if (child2.getComponentType().isOrExtends(NeuroMLElements.BASE_CONC_DEP_VAR_COMP_TYPE) ||
                            child2.getComponentType().isOrExtends(NeuroMLElements.BASE_CONC_DEP_RATE_COMP_TYPE)) {
                            hasCaDependency = true;

                        }

                    }
                }
            }
            if (hasCaDependency)
            {
                blockNeuron.append("USEION ca READ cai VALENCE 2\n"); //TODO check valence
            }

            blockNeuron.append("\nRANGE gmax                             : Will be set when ion channel mechanism placed on cell!\n");

            blockParameter.append("\ngmax = 0                                : Will be set when ion channel mechanism placed on cell!\n");

        } else if (comp.getComponentType().isOrExtends(NeuroMLElements.CONC_MODEL_COMP_TYPE)) {
            mechName = comp.getID();
            blockNeuron.append("SUFFIX " + mechName + "\n");

            String ion = comp.getStringValue("ion");
            if (ion != null) {
                blockNeuron.append("USEION " + ion + " READ " + ion + "i," + ion + "o, i" + ion + " WRITE " + ion + "i\n"); //TODO check valence
            }
            blockNeuron.append("RANGE cai\n");
            blockNeuron.append("RANGE cao\n");
            
            blockParameter.append("cai (mM)\n");
            blockParameter.append("cao (mM)\n");

            blockAssigned.append("ica		(mA/cm2)\n");

            blockAssigned.append("diam (um)\n");
            
            blockAssigned.append("area (um2)\n");

            ratesMethod.append(NeuroMLElements.CONC_MODEL_SURF_AREA + " = area\n\n");
            ratesMethod.append(NeuroMLElements.CONC_MODEL_CA_CURR_DENS + " = -10000 * ica * "+NeuroMLElements.CONC_MODEL_SURF_AREA+" : To correct units...\n\n");

            locals.add(NeuroMLElements.CONC_MODEL_SURF_AREA);
            locals.add(NeuroMLElements.CONC_MODEL_CA_CURR_DENS);

            blockNeuron.append("GLOBAL "+NeuroMLElements.CONC_MODEL_INIT_CONC + "\n");
            blockNeuron.append("GLOBAL "+NeuroMLElements.CONC_MODEL_INIT_EXT_CONC + "\n");
            blockParameter.append(NeuroMLElements.CONC_MODEL_INIT_CONC + " (mM)\n");
            blockParameter.append(NeuroMLElements.CONC_MODEL_INIT_EXT_CONC + " (mM)\n");

            blockInitial.append(NeuroMLElements.CONC_MODEL_INIT_CONC+" = cai" + "\n");
            blockInitial.append(NeuroMLElements.CONC_MODEL_INIT_EXT_CONC+" = cao" + "\n");
            
        } else if (comp.getComponentType().isOrExtends(NeuroMLElements.BASE_POINT_CURR_COMP_TYPE)) {
            mechName = comp.getID();
            blockNeuron.append("POINT_PROCESS " + mechName+"\n");
            blockNeuron.append("ELECTRODE_CURRENT i\n");
        }
        else {
            blockNeuron.append("POINT_PROCESS " + mechName);
        }

        if (comp.getComponentType().isOrExtends(NeuroMLElements.BASE_SYNAPSE_COMP_TYPE))
        {
            blockNetReceiveParams = "weight (uS)";
            blockAssigned.append("? Standard Assigned variables with baseSynapse\n");
            blockAssigned.append("v (mV)\n");
            blockAssigned.append(NEURON_TEMP + " (degC)\n");
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

        parseParameters(comp, prefix, rangeVars, stateVars, blockNeuron, blockParameter, paramMappings);


        if (comp.getComponentType().isOrExtends(NeuroMLElements.ION_CHANNEL_COMP_TYPE)) {
            blockAssigned.append("? Standard Assigned variables with ionChannel\n");
            blockAssigned.append("v (mV)\n");
            blockAssigned.append(NEURON_TEMP + " (degC)\n");
            blockAssigned.append(NeuroMLElements.TEMPERATURE + " (K)\n");

            String species = comp.getTextParam("species");
            if (species != null) {
                blockAssigned.append("e" + species + " (mV)\n");
                blockAssigned.append("i" + species + " (mA/cm2)\n");
            }
            blockAssigned.append("\n");
            if (hasCaDependency) {
                blockAssigned.append("cai (mM)\n\n");
                
                locals.add("caConc");
                ratesMethod.append("caConc = cai\n\n");

            }
        }

        //ratesMethod.append("? - \n");

        parseDerivedVars(comp, prefix, rangeVars, ratesMethod, blockNeuron, blockParameter, blockAssigned, blockBreakpoint, paramMappings);

        //ratesMethod.append("? + \n");

        if (comp.getComponentType().isOrExtends(NeuroMLElements.ION_CHANNEL_COMP_TYPE)) {

            blockBreakpoint.append("g = gmax * fopen     : Overwriting evaluation of g, assuming gmax set externally\n\n");

            String species = comp.getTextParam("species");
            // Only for ohmic!!
            if (species != null) {
                blockBreakpoint.append("i" + species + " = g * (v - e" + species + ")\n");
            }
        }

        parseTimeDerivs(comp, prefix, locals, blockDerivative, blockBreakpoint, blockAssigned, ratesMethod, paramMappings);

        if (blockDerivative.length() > 0) {
            blockBreakpoint.insert(0, "SOLVE states METHOD cnexp\n\n");
        }


        ArrayList<String> regimeNames = new ArrayList<String>();
        HashMap<String, Integer> flagsVsRegimes = new HashMap<String, Integer>();
        if (comp.getComponentType().hasDynamics()) {

            int regimeFlag = 5000;
            for (Regime regime: comp.getComponentType().getDynamics().getRegimes()) {
                flagsVsRegimes.put(regime.name, regimeFlag);  // fill
                regimeFlag++;
            }

            ////String elsePrefix = "";
            for (Regime regime: comp.getComponentType().getDynamics().getRegimes()) {
                String regimeStateName = REGIME_PREFIX+regime.name;
                regimeNames.add(regimeStateName);

                //StringBuilder test = new StringBuilder(": Testing for "+regimeStateName+ "\n");
                //test.append(elsePrefix+"if ("+regimeStateName+" == 1 ) {\n");
                //elsePrefix = "else ";

                // blockNetReceive.append("if ("+regimeStateName+" == 1 && flag == "+regimeFlag+") { "
                  //      + ": Setting watch for OnCondition in "+regimeStateName+"...\n");
                //////////blockNetReceive.append("    WATCH (" + checkForStateVarsAndNested(cond, comp, paramMappings) + ") "+conditionFlag+"\n");

                for (OnCondition oc: regime.getOnConditions()){

                    String cond = checkForBinaryOperators(oc.test);

                    blockNetReceive.append("\nif (flag == 1) { : Setting watch condition for "+regimeStateName+"\n");
                    blockNetReceive.append("    WATCH (" + checkForStateVarsAndNested(cond, comp, paramMappings) + ") "+regimeFlag+"\n");
                    blockNetReceive.append("}\n\n");

                    //test.append("    if (" + checkForStateVarsAndNested(cond, comp, paramMappings) + ") {\n");

                    blockNetReceive.append("if ("+regimeStateName+" == 1 && flag == "+regimeFlag+") { : Setting actions for "+regimeStateName+"\n");

                    if (debug) blockNetReceive.append("\n        printf(\"+++++++ Start condition ("+oc.test+") for "+regimeStateName+" at time: %g, v: %g\\n\", t, v)\n");

                    blockNetReceive.append("\n        : State assignments\n");
                    for (StateAssignment sa : oc.getStateAssignments()) {
                        blockNetReceive.append("\n        " + getStateVarName(sa.getStateVariable().getName()) + " = "
                                + checkForStateVarsAndNested(sa.getValueExpression(), comp, paramMappings) + "\n");
                    }
                    for (Transition trans: oc.getTransitions()){
                        blockNetReceive.append("\n        : Change regime flags\n");
                        blockNetReceive.append("        "+regimeStateName+" = 0\n");
                        blockNetReceive.append("        "+REGIME_PREFIX+trans.regime+" = 1\n");
                        //int flagTarget = flagsVsRegimes.get(trans.getRegime());
                        //test.append("    net_send(0,"+flagTarget+") : Sending to regime_"+trans.getRegime()+"\n");

                        Regime targetRegime = comp.getComponentType().getDynamics().getRegimes().getByName(trans.regime);
                        if (targetRegime!=null){
                            blockNetReceive.append("\n        : OnEntry to "+targetRegime+"\n");
                            for (OnEntry oe: targetRegime.getOnEntrys()){
                                for (StateAssignment sa: oe.getStateAssignments()){
                                    blockNetReceive.append("\n        " + sa.getStateVariable().getName() + " = "
                                            + checkForStateVarsAndNested(sa.getValueExpression(), comp, paramMappings) + "\n");
                                }
                            }
                        }
                    }

                    if (debug) blockNetReceive.append("\n        printf(\"+++++++ End condition ("+oc.test+") for "+regimeStateName+" at time: %g, v: %g\\n\", t, v)\n");
                    blockNetReceive.append("}\n");
                    
                }
                //blockNetReceive.append("}\n");
                //blockBreakpoint_regimes.insert(0, test.toString()+ "\n");
                //blockBreakpoint_regimes.append(blockNetReceive.toString()+ "\n");
                regimeFlag++;
            }
         }

        String localsLine = "";
        if (!locals.isEmpty()) {
            localsLine = "LOCAL ";
        }
        for (String local : locals) {
            localsLine = localsLine + local;
            if (!locals.get(locals.size() - 1).equals(local)) {
                localsLine = localsLine + ", ";
            } else {
                localsLine = localsLine + "\n";
            }


        }

        ratesMethod.insert(0, localsLine);
        //blockDerivative.insert(0, localsLine);

        blockInitial.append("rates()\n");
        
        if (comp.getComponentType().isOrExtends(NeuroMLElements.ION_CHANNEL_COMP_TYPE) || comp.getComponentType().isOrExtends(NeuroMLElements.BASE_SYNAPSE_COMP_TYPE)) {
        	blockInitial.append("\n" + NeuroMLElements.TEMPERATURE + " = " + NEURON_TEMP + " + 273.15\n");
        }

        parseOnStart(comp, prefix, blockInitial, blockInitial_v, paramMappings);


        /*for (OnStart os : comp.getComponentClass().getDynamics().getOnStarts()) {
        for (StateAssignment sa : os.getStateAssignments()) {
        blockInitial.append("\n" + getStateVarName(sa.getStateVariable().getName()) + " = " + sa.getEvaluable() + "\n");
        }
        }*/



        int conditionFlag = 1000;
        Dynamics dyn = comp.getComponentType().getDynamics();
        if (dyn!=null) {

            for (OnCondition oc : dyn.getOnConditions()) {

                String cond = checkForBinaryOperators(oc.test);

                boolean resetVoltage = false;
                for (StateAssignment sa : oc.getStateAssignments()) {
                    resetVoltage = resetVoltage || sa.getStateVariable().getName().equals(NEURON_VOLTAGE);
                }

                if (!resetVoltage) {
                    blockBreakpoint.append("if (" + checkForStateVarsAndNested(cond, comp, paramMappings) + ") {");
                    for (StateAssignment sa : oc.getStateAssignments()) {
                        blockBreakpoint.append("\n    " + getStateVarName(sa.getStateVariable().getName()) + " = " + checkForStateVarsAndNested(sa.getValueExpression(), comp, paramMappings) + "\n");
                    }
                    blockBreakpoint.append("}\n\n");
                } else {

                    blockNetReceive.append("\nif (flag == 1) { : Setting watch for top level OnCondition...\n");
                    blockNetReceive.append("    WATCH (" + checkForStateVarsAndNested(cond, comp, paramMappings) + ") "+conditionFlag+"\n");

                    blockNetReceive.append("}\n");
                    blockNetReceive.append("if (flag == "+conditionFlag+") {\n");
                    if (debug) blockNetReceive.append("    printf(\"Condition (" + checkForStateVarsAndNested(cond, comp, paramMappings) + "), "+conditionFlag
                            +", satisfied at time: %g, v: %g\\n\", t, v)\n");
                    for (StateAssignment sa : oc.getStateAssignments()) {
                        blockNetReceive.append("\n    " + sa.getStateVariable().getName() + " = " + checkForStateVarsAndNested(sa.getValueExpression(), comp, paramMappings) + "\n");
                    }
                    blockNetReceive.append("}\n");

                }
                conditionFlag++;
            }
        }

        parseOnEvent(comp, blockNetReceive, paramMappings);
        

        if (comp.getComponentType().isOrExtends(NeuroMLElements.CONC_MODEL_COMP_TYPE) &&
            comp.getComponentType().getDynamics().getTimeDerivatives().isEmpty())
        {
            blockBreakpoint.append("\ncai = "+NeuroMLElements.CONC_MODEL_CONC_STATE_VAR+"\n\n");
        }


        /*
        if (comp.getComponentType().hasDynamics()) {
            for (Regime regime: comp.getComponentType().getDynamics().getRegimes()) {
                String regimeStateName = "regime_"+regime.name;
                blockNetReceive.append(": Conditions for "+regimeStateName);
                int regimeFlag = flagsVsRegimes.get(regime.name);
                blockNetReceive.append("if (flag == "+regimeFlag+") { : Entry into "+regimeStateName+"\n");
                for (String r: regimeNames){
                    blockNetReceive.append("    " + r + " = " +(r.equals(regimeStateName)?1:0)+"\n");
                }
                for (OnEntry oe: regime.getOnEntrys()){
                  
                    for (StateAssignment sa: oe.getStateAssignments()){
                        blockNetReceive.append("\n    " + getStateVarName(sa.getStateVariable().getName()) + " = "
                                + checkForStateVarsAndNested(sa.getEvaluable().toString(), comp, paramMappings) + "\n");
                    }
                }
                blockNetReceive.append("}\n");
            }
         }*/
         
         
        if (blockInitial_v.toString().trim().length()>0) {
            blockNetReceive.append("if (flag == 1) { : Set initial states\n");
            blockNetReceive.append(blockInitial_v.toString());
            blockNetReceive.append("}\n");
        }

        if (dyn!=null) {
            for (StateVariable sv : dyn.getStateVariables()) {

                if (sv.getName().equals(NEURON_VOLTAGE)) {
                    //blockBreakpoint.append("\ni = " + HIGH_CONDUCTANCE_PARAM + "*(v-" + getStateVarName(NEURON_VOLTAGE) + ") ? To ensure v of section rapidly follows " + getStateVarName(sv.getName()));

                    blockBreakpoint.append("\n" + V_COPY_PREFIX + NEURON_VOLTAGE + " = " + NEURON_VOLTAGE);

                    if (comp.getComponentType().isOrExtends(NeuroMLElements.BASE_CELL_CAP_COMP_TYPE)) {
                        //blockBreakpoint.append("\ni = -1 * " + ABSTRACT_CELL_COMP_TYPE_CAP__I_MEMB + "");
                        blockBreakpoint.append("\ni = " + getStateVarName(NEURON_VOLTAGE) + " * C");
                    } else {
                        blockBreakpoint.append("\ni = " + getStateVarName(NEURON_VOLTAGE) + "");
                    }
                }
            }
        }



        writeModBlock(mod, "NEURON", blockNeuron.toString());

        writeModBlock(mod, "UNITS", blockUnits.toString());

        writeModBlock(mod, "PARAMETER", blockParameter.toString());

        if (blockAssigned.length() > 0) {
            writeModBlock(mod, "ASSIGNED", blockAssigned.toString());
        }

        writeModBlock(mod, "STATE", blockState.toString());
        writeModBlock(mod, "INITIAL", blockInitial.toString());


        if (blockDerivative.length() == 0) {
            blockBreakpoint.insert(0, "rates()\n");
        }

        if (dyn!=null) {
            for (Regime regime: dyn.getRegimes()) {
                String reg = "regime_"+regime.getName();
                if (debug) blockBreakpoint.insert(0, "printf(\"     "+reg+": %g\\n\", "+reg+")\n");
            }
        }
        if (debug) blockBreakpoint.insert(0, "printf(\"+++++++ Entering BREAKPOINT in "+comp.getName()+" at time: %g, v: %g\\n\", t, v)\n");
        
        writeModBlock(mod, "BREAKPOINT", blockBreakpoint_regimes.toString()+"\n"+blockBreakpoint.toString());

        if (blockNetReceive.length() > 0) {
            writeModBlock(mod, "NET_RECEIVE("+blockNetReceiveParams+")", blockNetReceive.toString());
        }

        if (blockDerivative.length() > 0) {
            blockDerivative.insert(0, "rates()\n");
            writeModBlock(mod, "DERIVATIVE states", blockDerivative.toString());
        }


        writeModBlock(mod, "PROCEDURE rates()", ratesMethod.toString());

        //for (String compK : paramMappings.keySet()) {
            //E.info("  Maps for "+compK);
            ////for (String orig : paramMappings.get(compK).keySet()) {
                //E.info("      "+orig+" -> "+paramMappings.get(compK).get(orig));
            ////}
        //}
        //System.out.println("----  paramMappings: "+paramMappings);

        return mod.toString();
    }

    private static void parseOnStart(Component comp,
            String prefix,
            StringBuilder blockInitial,
            StringBuilder blockInitial_v,
            HashMap<String, HashMap<String, String>> paramMappings) throws ContentError {

        HashMap<String, String> paramMappingsComp = paramMappings.get(comp.getUniqueID());

        if (comp.getComponentType().hasDynamics()) {

            for (Regime regime: comp.getComponentType().getDynamics().getRegimes()) {
                String regimeStateName = REGIME_PREFIX+regime.name;
                if (regime.initial !=null && regime.initial.equals("true")) {
                        blockInitial.append("\n"+regimeStateName+" = 1\n");
                } else {
                        blockInitial.append("\n"+regimeStateName+" = 0\n");
                }

            }


            for (OnStart os : comp.getComponentType().getDynamics().getOnStarts()) {
                for (StateAssignment sa : os.getStateAssignments()) {
                    String var = getStateVarName(sa.getStateVariable().getName());

                    if (paramMappingsComp.containsKey(var)) {
                        var = paramMappingsComp.get(var);
                    }

                    if (sa.getStateVariable().getName().equals(NEURON_VOLTAGE)) {
                        var = sa.getStateVariable().getName();

                        blockInitial.append("\nnet_send(0, 1) : go to NET_RECEIVE block, flag 1, for initial state\n");
                        blockInitial_v.append("\n    " + var + " = " + checkForStateVarsAndNested(sa.getValueExpression(), comp, paramMappings) + "\n");

                    } else {

                        blockInitial.append("\n" + var + " = " + checkForStateVarsAndNested(sa.getValueExpression(), comp, paramMappings) + "\n");
                    }
                }
            }
        }
        for (Component childComp : comp.getAllChildren()) {
            String prefixNew = prefix + childComp.getID() + "_";
            if (childComp.getID() == null) {
                prefixNew = prefix + childComp.getName() + "_";
            }
            parseOnStart(childComp, prefixNew, blockInitial, blockInitial_v, paramMappings);
        }

    }
    
    private static void parseOnEvent(Component comp,
            StringBuilder blockNetReceive,
            HashMap<String, HashMap<String, String>> paramMappings) throws ContentError {
    		// Add appropriate state discontinuities for synaptic events in NET_RECEIVE block. Do this for all child elements as well, in case the spike event is meant to be relayed down from the parent synaptic mechanism to a child plasticityMechanism.  
        if (comp.getComponentType().getDynamics()!=null) {
            for (OnEvent oe : comp.getComponentType().getDynamics().getOnEvents()) {
                if (oe.getPortName().equals(NeuroMLElements.SYNAPSE_PORT_IN))
                {
                    for (StateAssignment sa : oe.getStateAssignments()) {
                        blockNetReceive.append("state_discontinuity("+checkForStateVarsAndNested(sa.getStateVariable().getName(), comp, paramMappings) + ", "+ checkForStateVarsAndNested(sa.getValueExpression(), comp, paramMappings)+")\n");
                    }
                }
            }
        }
        for (Component childComp : comp.getAllChildren()) {
        	if (childComp.getComponentType().isOrExtends(NeuroMLElements.BASE_PLASTICITY_MECHANISM_COMP_TYPE)) {
        		parseOnEvent(childComp, blockNetReceive, paramMappings);
        	}
        }
    }

    private static void parseParameters(Component comp,
            String prefix,
            ArrayList<String> rangeVars,
            ArrayList<String> stateVars,
            StringBuilder blockNeuron,
            StringBuilder blockParameter,
            HashMap<String, HashMap<String, String>> paramMappings) {

        HashMap<String, String> paramMappingsComp = paramMappings.get(comp.getUniqueID());

        if (paramMappingsComp == null) {
            paramMappingsComp = new HashMap<String, String>();
            paramMappings.put(comp.getUniqueID(), paramMappingsComp);
        }

        for (ParamValue pv : comp.getParamValues()) {
            String mappedName = prefix + pv.getName();
            rangeVars.add(mappedName);
            paramMappingsComp.put(pv.getName(), mappedName);

            String range = "\nRANGE " + mappedName;
            while (range.length() < commentOffset) {
                range = range + " ";
            }

            blockNeuron.append(range + ": parameter");
            float val = convertToNeuronUnits((float) pv.getDoubleValue(), pv.getDimensionName());
            String valS = val + "";
            if ((int) val == val) {
                valS = (int) val + "";
            }
            blockParameter.append("\n" + mappedName + " = " + valS
                    + " " + getNeuronUnit(pv.getDimensionName()));
        }


        for (Exposure exp : comp.getComponentType().getExposures()) {
            String mappedName = prefix + exp.getName();

            if (!rangeVars.contains(mappedName)
                    && !stateVars.contains(mappedName)
                    && !exp.getName().equals(NEURON_VOLTAGE)) {
                rangeVars.add(mappedName);
                paramMappingsComp.put(exp.getName(), mappedName);

                String range = "\nRANGE " + mappedName;
                while (range.length() < commentOffset) {
                    range = range + " ";
                }
                blockNeuron.append(range + ": exposure");

                if (comp.getComponentType().isOrExtends(NeuroMLElements.BASE_POINT_CURR_COMP_TYPE) &&
                    exp.getName().equals(NeuroMLElements.POINT_CURR_CURRENT))
                {
                    blockNeuron.append("\n\nNONSPECIFIC_CURRENT "+NeuroMLElements.POINT_CURR_CURRENT+"\n");
                }
            }
        }

        for (Component childComp : comp.getAllChildren()) {
            String prefixNew = prefix + childComp.getID() + "_";
            if (childComp.getID() == null) {
                prefixNew = prefix + childComp.getName() + "_";
            }
            parseParameters(childComp, prefixNew, rangeVars, stateVars, blockNeuron, blockParameter, paramMappings);

            /*
            HashMap<String, String> childMaps = paramMappings.get(childComp.getID());
            for (String mapped: childMaps.keySet()){
            if (!paramMappingsComp.containsKey(mapped)){
            paramMappingsComp.put(mapped, childMaps.get(mapped));
            }
            }*/
        }

        if (comp.getComponentType().isOrExtends(NeuroMLElements.BASE_CELL_COMP_TYPE)) {
            blockNeuron.append("\nRANGE " + V_COPY_PREFIX + NEURON_VOLTAGE + "                           : copy of v on section\n");
        }

    }

    private static void parseStateVars(Component comp,
            String prefix,
            ArrayList<String> rangeVars,
            ArrayList<String> stateVars,
            StringBuilder blockNeuron,
            StringBuilder blockParameter,
            StringBuilder blockAssigned,
            StringBuilder blockState,
            HashMap<String, HashMap<String, String>> paramMappings) throws ContentError {

        HashMap<String, String> paramMappingsComp = paramMappings.get(comp.getUniqueID());

        if (paramMappingsComp == null) {
            paramMappingsComp = new HashMap<String, String>();
            paramMappings.put(comp.getUniqueID(), paramMappingsComp);
        }

        if (comp.getComponentType().hasDynamics()) {

            for (Regime regime: comp.getComponentType().getDynamics().getRegimes()) {
                String regimeStateName = REGIME_PREFIX+regime.name;
                stateVars.add(regimeStateName);
                blockState.append(regimeStateName + " (1)\n");
            }


            for (StateVariable sv : comp.getComponentType().getDynamics().getStateVariables()) {

                String svName = prefix + getStateVarName(sv.getName());
                stateVars.add(svName);
                String dim = getNeuronUnit(sv.getDimension().getName());

                if (!svName.equals(NEURON_VOLTAGE) && !getStateVarName(sv.getName()).equals(getStateVarName(NEURON_VOLTAGE))) {
                    paramMappingsComp.put(getStateVarName(sv.getName()), svName);
                }


                if (sv.getName().equals(NEURON_VOLTAGE)) {
                    blockNeuron.append("\n\nNONSPECIFIC_CURRENT i                    : To ensure v of section follows " + svName);
                    ////blockNeuron.append("\nRANGE " + HIGH_CONDUCTANCE_PARAM + "                  : High conductance for above current");
                    ////blockParameter.append("\n\n" + HIGH_CONDUCTANCE_PARAM + " = 1000 (S/cm2)");
                    blockAssigned.append("v (mV)\n");
                    blockAssigned.append("i (mA/cm2)\n\n");


                    blockAssigned.append(V_COPY_PREFIX + NEURON_VOLTAGE + " (mV)\n\n");

                    dim = "(nA)";
                }

                blockState.append(svName + " " + dim + "\n");
            }
        }

        for (Component childComp : comp.getAllChildren()) {
            String prefixNew = prefix + childComp.getID() + "_";
            if (childComp.getID() == null) {
                prefixNew = prefix + childComp.getName() + "_";
            }
            parseStateVars(childComp, prefixNew, rangeVars, stateVars, blockNeuron, blockParameter, blockAssigned, blockState, paramMappings);
        }
    }

    private static void parseTimeDerivs(Component comp,
            String prefix,
            ArrayList<String> locals,
            StringBuilder blockDerivative,
            StringBuilder blockBreakpoint,
            StringBuilder blockAssigned,
            StringBuilder ratesMethod,
            HashMap<String, HashMap<String, String>> paramMappings) throws ContentError {

        StringBuilder ratesMethodFinal = new StringBuilder();

        if (comp.getComponentType().hasDynamics()) {

            HashMap<String, String> rateNameVsRateExpr = new HashMap<String, String>();

            for (TimeDerivative td : comp.getComponentType().getDynamics().getTimeDerivatives()) {
            	
               
                String rateName = RATE_PREFIX + prefix + td.getStateVariable().getName();
                String rateUnits = getDerivativeUnit(td.getStateVariable().getDimension().getName());
                
                blockAssigned.append(rateName + " " + rateUnits + "\n");

                //ratesMethod.append(rateName + " = " + checkForStateVarsAndNested(td.getEvaluable().toString(), comp, paramMappings) + " ? \n");
                String rateExpr = checkForStateVarsAndNested(td.getValueExpression(), comp, paramMappings);
                rateNameVsRateExpr.put(rateName, rateExpr);
                
                if (!td.getStateVariable().getName().equals(NEURON_VOLTAGE)) {

                    String stateVarToUse = getStateVarName(td.getStateVariable().getName());
                    

                    String line = prefix + stateVarToUse + "' = " + rateName;

                    if (comp.getComponentType().isOrExtends(NeuroMLElements.CONC_MODEL_COMP_TYPE) &&
                        td.getStateVariable().getName().equals(NeuroMLElements.CONC_MODEL_CONC_STATE_VAR))
                    {
                        line = line+ "\ncai = "+td.getStateVariable().getName();
                    }

                    if (blockDerivative.toString().indexOf(line)<0)
                        blockDerivative.append(line+" \n");

                } else {
                    ratesMethodFinal.append(prefix + getStateVarName(td.getStateVariable().getName()) + " = -1 * " + rateName + "\n");
                }
                 
            }


            for (Regime regime: comp.getComponentType().getDynamics().getRegimes()) {
            	
                if (regime.getTimeDerivatives().isEmpty()) {
                	// need to hold voltage fixed

                	for (OnEntry oe: regime.getOnEntrys()){
                        for (StateAssignment sa: oe.getStateAssignments()){

	                        if (sa.getStateVariable().getName().equals(NEURON_VOLTAGE)) {
	                            String rateName = RATE_PREFIX + prefix + sa.getStateVariable().getName();
	                        	
	                            if (!rateNameVsRateExpr.containsKey(rateName)) {
	                                rateNameVsRateExpr.put(rateName, "0");
	                            }
	
	                            String rateExprPart = rateNameVsRateExpr.get(rateName);

	                            String rateUnits = getDerivativeUnit(sa.getStateVariable().getDimension().getName());
	                            if (blockAssigned.indexOf("\n"+rateName + " " + rateUnits + "\n")<0) 
	                            {
	                                blockAssigned.append("\n"+rateName + " " + rateUnits + "\n");
	                            }
	                            rateExprPart = rateExprPart+" + "+REGIME_PREFIX+regime.getName()+" * (10000000 * ("+sa.getValueExpression()+" - "+NEURON_VOLTAGE+"))";
	                            
	                            rateNameVsRateExpr.put(rateName, rateExprPart);
	                        }
                        }
                    }
                }
            	for (TimeDerivative td: regime.getTimeDerivatives()){
                    String rateName = RATE_PREFIX + prefix + td.getStateVariable().getName();
                    String rateUnits = getDerivativeUnit(td.getStateVariable().getDimension().getName());
                    if (!rateNameVsRateExpr.containsKey(rateName)) {
                        rateNameVsRateExpr.put(rateName, "0");
                    }

                    String rateExprPart = rateNameVsRateExpr.get(rateName);
                    if (blockAssigned.indexOf("\n"+rateName + " " + rateUnits + "\n")<0) 
                    {
                        blockAssigned.append("\n"+rateName + " " + rateUnits + "\n");
                    }
                    rateExprPart = rateExprPart+" + "+REGIME_PREFIX+regime.getName()+" * ("+checkForStateVarsAndNested(td.getValueExpression(), comp, paramMappings)+")";
                    
                    rateNameVsRateExpr.put(rateName, rateExprPart);
                    
                    if (!td.getStateVariable().getName().equals(NEURON_VOLTAGE)) {
                        String line = prefix + getStateVarName(td.getStateVariable().getName()) + "' = " + rateName;

                        if (blockDerivative.toString().indexOf(line)<0)
                            blockDerivative.append(line+" \n");
                        
                    } else {
                        ratesMethodFinal.append(prefix + getStateVarName(td.getStateVariable().getName()) + " = -1 * " + rateName + "\n"); ////
                    }
                }
            }
            
            for (String rateName: rateNameVsRateExpr.keySet()){
                String rateExpr = rateNameVsRateExpr.get(rateName);
                //ratesMethod.insert(0,rateName + " = " + rateExpr + " \n");
                ratesMethod.append(rateName + " = " + rateExpr + " \n");
            }

            ratesMethod.append("\n"+ratesMethodFinal + " \n");

        }
        
        for (Component childComp : comp.getAllChildren()) {
            String prefixNew = prefix + childComp.getID() + "_";
            if (childComp.getID() == null) {
                prefixNew = prefix + childComp.getName() + "_";
            }
            parseTimeDerivs(childComp, prefixNew, locals, blockDerivative, blockBreakpoint, blockAssigned, ratesMethod, paramMappings);
        }
    }

    private static void parseDerivedVars(Component comp,
            String prefix,
            ArrayList<String> rangeVars,
            StringBuilder ratesMethod,
            StringBuilder blockNeuron,
            StringBuilder blockParameter,
            StringBuilder blockAssigned,
            StringBuilder blockBreakpoint,
            HashMap<String, HashMap<String, String>> paramMappings) throws ContentError {


        HashMap<String, String> paramMappingsComp = paramMappings.get(comp.getUniqueID());
        if (paramMappingsComp == null) {
            paramMappingsComp = new HashMap<String, String>();
            paramMappings.put(comp.getUniqueID(), paramMappingsComp);
        }

        for (Component childComp : comp.getAllChildren()) {
            String prefixNew = prefix + childComp.getID() + "_";
            if (childComp.getID() == null) {
                prefixNew = prefix + childComp.getName() + "_";
            }
            parseDerivedVars(childComp, prefixNew, rangeVars, ratesMethod, blockNeuron, blockParameter, blockAssigned, blockBreakpoint, paramMappings);
        }
        //ratesMethod.append("? Looking at"+comp+"\n");
        if (comp.getComponentType().hasDynamics()) {

            StringBuilder blockForEqns = ratesMethod;
            if (comp.getComponentType().isOrExtends(NeuroMLElements.ION_CHANNEL_COMP_TYPE)) {
                blockForEqns = blockBreakpoint;
            }
            for (DerivedVariable dv : comp.getComponentType().getDynamics().getDerivedVariables()) {

                StringBuilder block = new StringBuilder();

                String mappedName = prefix + dv.getName();
                if (!rangeVars.contains(mappedName)) {
                    rangeVars.add(mappedName);

                    String range = "\nRANGE " + mappedName;
                    while (range.length() < commentOffset) {
                        range = range + " ";
                    }

                    blockNeuron.append(range + ": derived var\n");
                    paramMappingsComp.put(dv.getName(), mappedName);
                }

                String assig = "\n" + prefix + dv.getName() + " " + getNeuronUnit(dv.dimension);
                while (assig.length() < commentOffset) {
                    assig = assig + " ";
                }

                blockAssigned.append(assig + ": derived var\n");


                if (dv.getValueExpression() != null) {

                    //block.append("? DV was: " + dv.getValueExpression() + "\n");

                    String rate = checkForStateVarsAndNested(dv.getValueExpression(), comp, paramMappings);
                    //block.append("? DV is: " + rate + "\n");

                    String synFactor = "";
                    if (comp.getComponentType().isOrExtends(NeuroMLElements.BASE_POINT_CURR_COMP_TYPE) &&
                    dv.getName().equals(NeuroMLElements.POINT_CURR_CURRENT))
                    {
                        // since synapse currents differ in sign from NEURON
                        synFactor = "-1 * ";
                    }
                    
                    int u=7;
                    if (1<u /*TODOdv.get == null*/) {
                        //if (cond.)
                        block.append(prefix + dv.getName() + " = "+synFactor + rate + " ? evaluable\n\n");
                    } else {

                        block.append("TODO dv.getEvaluableCondition() !!!!!\n");
                        /*
                        String cond = checkForStateVarsAndNested(dv.getEvaluableCondition().toString(), comp, paramMappings);
                        String ifFalse = checkForStateVarsAndNested(dv.getEvaluableIfFalse().toString(), comp, paramMappings);

                        block.append("if (" + cond + ") {\n");
                        block.append("    " + prefix + dv.getName() + " = "+synFactor + rate + " \n");
                        block.append("} else {\n");
                        block.append("    " + prefix + dv.getName() + " = "+synFactor + ifFalse + " \n");
                        block.append("} \n\n");*/

                    }

                } else {
                    String firstChild = dv.getPath().substring(0, dv.getPath().indexOf("/"));
                    if (firstChild.indexOf("[") >= 0) {
                        firstChild = firstChild.substring(0, firstChild.indexOf("["));
                    }

                    Component child = null;

                    try {
                        child = comp.getChild(firstChild);
                    } catch (ContentError ce) {
                        E.info("No child of " + firstChild);// do nothing...
                    }

                    if (child == null) {
                        ArrayList<Component> children = comp.getChildrenAL(firstChild);
                        if (children.size() > 0) {
                            child = children.get(0);
                        }
                    }

                    block.append("? DerivedVariable is based on path: " + dv.getPath() + ", on: "+comp+", from " + firstChild + "; " + child + "\n");

                    if (child == null && dv.getPath().indexOf("synapse") < 0) {
                        String alt = "???";
                        if (dv.getReduce().equals("multiply")) {
                            alt = "1";
                        }
                        else if (dv.getReduce().equals("add")) {
                            alt = "0";
                        }
                        block.append("? Path not present in component, using factor: " + alt + "\n\n");
                        String rate = checkForStateVarsAndNested(alt, comp, paramMappings);
                        block.append(prefix + dv.getName() + " = " + rate + " \n\n");
                    } else {
                        String localVar = dv.getPath().replaceAll("/", "_");
                        String globalVar = prefix+ dv.getPath().replaceAll("/", "_");
                        //String var0 = var;
                        
                        String eqn = globalVar;
                        if (globalVar.indexOf("[*]") >= 0 && globalVar.indexOf("syn") >= 0) {
                            eqn = "0 ? Was: " + localVar + " but insertion of currents from external attachments not yet supported";
                        } else if (localVar.indexOf("[*]") >= 0) {
                            String children = localVar.substring(0, localVar.indexOf("[*]"));
                            String path = localVar.substring(localVar.indexOf("[*]_") + 4);
                            String reduce = dv.getReduce();
                            String op = null;
                            if (reduce.equals("multiply")) {
                                op = " * ";
                            }
                            if (reduce.equals("add")) {
                                op = " + ";
                            }
                            eqn = "";

                            for (Component childComp : comp.getChildrenAL(children)) {
                                //var = var + childComp.getID()+" --";
                                if (eqn.length() > 0) {
                                    eqn = eqn + op;
                                }
                                eqn = eqn + childComp.getID() + "_" + path;
                            }


                            eqn = eqn + " ? " + reduce + " applied to all instances of " + path + " in: <" + children +"> ("+comp.getChildrenAL(children)+")" +" c2 ("+comp.getAllChildren()+")";
                        }
                        block.append(prefix + dv.getName() + " = " + eqn + " ? path based\n\n");
                    }
                }
                //blockForEqns.insert(0, block);
                blockForEqns.append(block);
            }
        }

    }

    private static String getNeuronUnit(String dimensionName) {

        if (dimensionName == null) {
            return ": no units???";
        }

        if (dimensionName.equals("voltage")) {
            return "(mV)";
        } else if (dimensionName.equals("per_voltage")) {
            return "(/mV)";
        } else if (dimensionName.equals("conductance")) {
            return "(uS)";
        } else if (dimensionName.equals("capacitance")) {
            return "(microfarads)";
        } else if (dimensionName.equals("time")) {
            return "(ms)";
        } else if (dimensionName.equals("per_time")) {
            return "(megahertz)";
        } else if (dimensionName.equals("current")) {
            return "(nA)";
        } else if (dimensionName.equals("length")) {
            return "(um)";
        } else if (dimensionName.equals("area")) {
            return "(um2)";
        } else if (dimensionName.equals("concentration")) {
            return "(mM)";
        } else if (dimensionName.equals("charge_per_mole")) {
            return "(coulomb)";
        } else if (dimensionName.equals("temperature")) {
            return "(K)";
        } else if (dimensionName.equals("idealGasConstantDims")) {
	    return "(millijoule / K)";
        } else if (dimensionName.equals(Dimension.NO_DIMENSION)) {
            return "";
        } else {
            return "? Don't know units for : (" + dimensionName + ")";
        }
    }

    private static float convertToNeuronUnits(float val, String dimensionName) {
        float newVal = val * getNeuronUnitFactor(dimensionName);
        return newVal;
    }

    private static float getNeuronUnitFactor(String dimensionName) {

        if (dimensionName.equals("voltage")) {
            return 1000f;
        } else if (dimensionName.equals("per_voltage")) {
            return 0.001f;
        } else if (dimensionName.equals("conductance")) {
            return 1000000f;
        } else if (dimensionName.equals("capacitance")) {
            return 1e6f;
        } else if (dimensionName.equals("per_time")) {
            return 0.001f;
        } else if (dimensionName.equals("current")) {
            return 1e9f;
        } else if (dimensionName.equals("time")) {
            return 1000f;
        } else if (dimensionName.equals("length")) {
            return 1000000f;
        } else if (dimensionName.equals("area")) {
            return 1e12f;
        } else if (dimensionName.equals("concentration")) {
            return 1f;
        } else if (dimensionName.equals("charge_per_mole")) {
            return 1f;
        } else if (dimensionName.equals("idealGasConstantDims")) {
	    return 1000f;
        }
        return 1f;
    }

    private static String getDerivativeUnit(String dimensionName) {
	String unit = getNeuronUnit(dimensionName);
	if (unit.equals("")) {
	    return "(/ms)";
	} else {
	    return unit.replaceAll("\\)", "/ms)");
	}
    }

    public static void writeModBlock(StringBuilder main, String blockName, String contents) {
        contents = contents.replaceAll("\n", "\n    ");
        if (!contents.endsWith("\n")) {
            contents = contents + "\n";
        }
        main.append(blockName + " {\n    "
                + contents + "}\n\n");
    }

    public class CompInfo {

        StringBuilder params = new StringBuilder();
        StringBuilder eqns = new StringBuilder();
        StringBuilder initInfo = new StringBuilder();
    }

    public static void main(String[] args) throws Exception {

        E.setDebug(false);
        ArrayList<File> nml2Channels = new ArrayList<File>();

        //nml2Channels.add(new File("../nCexamples/Ex10_NeuroML2/cellMechanisms/IzhBurst/IzhBurst.nml"));
        nml2Channels.add(new File("../lemspaper/tidyExamples/test/HH_cell.nml"));


        File expDir = new File("src/test/resources/tmp");
        //for (File f : expDir.listFiles()) {
            //f.delete();
        //}
        
        File lemsFile = new File("../lemspaper/tidyExamples/test/Fig_HH.xml");
        lemsFile = new File("../NeuroML2/NeuroML2CoreTypes/LEMS_NML2_Ex5_DetCell.xml");
        lemsFile = new File("../neuroConstruct/osb/cerebral_cortex/networks/ACnet2/neuroConstruct/generatedNeuroML2/LEMS_ACnet2.xml");
        lemsFile = new File("../neuroConstruct/osb/showcase/neuroConstructShowcase/Ex4_HHcell/generatedNeuroML2/LEMS_Ex4_HHcell.xml");
        
        Lems lems = Utils.readLemsNeuroMLFile(lemsFile).getLems();
        File mainFile = new File(expDir, "hh.py");
        
        NeuronWriter nw = new NeuronWriter(lems);
        ArrayList<File> ff = nw.generateMainScriptAndMods(mainFile);
        for (File f: ff){
            System.out.println("Generated: "+f.getAbsolutePath());
        }
            

        /*
        for (File nml2Channel : nml2Channels) {
            String nml2Content = FileUtil.readStringFromFile(nml2Channel);
            //System.out.println("nml2Content: "+nml2Content);
            String lemsified = NeuroMLConverter.convertNeuroML2ToLems(nml2Content);
            //System.out.println("lemsified: "+lemsified);

            Lems lems = Utils.readLemsNeuroMLFile(lemsified).getLems();
            
            for (Component comp : lems.components.getContents()) {
                E.info("Component: " + comp);
                E.info("baseIonChannel: " + comp.getComponentType().isOrExtends(NeuroMLElements.ION_CHANNEL_COMP_TYPE));
                E.info("baseCell: " + comp.getComponentType().isOrExtends(NeuroMLElements.BASE_CELL_COMP_TYPE));
                E.info("concentrationModel: " + comp.getComponentType().isOrExtends(NeuroMLElements.CONC_MODEL_COMP_TYPE));
                E.info("basePointCurrent: " + comp.getComponentType().isOrExtends(NeuroMLElements.BASE_POINT_CURR_COMP_TYPE));

                if (comp.getComponentType().isOrExtends(NeuroMLElements.ION_CHANNEL_COMP_TYPE)
                        || comp.getComponentType().isOrExtends(NeuroMLElements.BASE_CELL_COMP_TYPE)
                        || comp.getComponentType().isOrExtends(NeuroMLElements.CONC_MODEL_COMP_TYPE)
                        || comp.getComponentType().isOrExtends(NeuroMLElements.BASE_POINT_CURR_COMP_TYPE)) {
                    E.info(comp + " is an " + NeuroMLElements.ION_CHANNEL_COMP_TYPE + " or  " + NeuroMLElements.BASE_CELL_COMP_TYPE + " or  " + NeuroMLElements.CONC_MODEL_COMP_TYPE+ " or  " + NeuroMLElements.BASE_POINT_CURR_COMP_TYPE);
                    String mod = generateModFile(comp);

                    File expFile = new File(expDir, comp.getID() + ".mod");


                    E.info("\n----------------------------------------------------   \n");
                    E.info(mod);

                    FileUtil.writeStringToFile(mod, expFile);
                    E.info("Exported file to: " + expFile.getCanonicalPath());

                }

            }
        }*/
    }
}

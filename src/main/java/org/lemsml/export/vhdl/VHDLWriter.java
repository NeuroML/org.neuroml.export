package org.lemsml.export.vhdl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBException;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.JsonFactory;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.lemsml.export.base.GenerationException;
import org.lemsml.export.dlems.DLemsWriter;
import org.lemsml.export.vhdl.edlems.EDCase;
import org.lemsml.export.vhdl.edlems.EDComponent;
import org.lemsml.export.vhdl.edlems.EDCondition;
import org.lemsml.export.vhdl.edlems.EDConditionalDerivedVariable;
import org.lemsml.export.vhdl.edlems.EDDerivedVariable;
import org.lemsml.export.vhdl.edlems.EDDisplay;
import org.lemsml.export.vhdl.edlems.EDDynamic;
import org.lemsml.export.vhdl.edlems.EDEvent;
import org.lemsml.export.vhdl.edlems.EDEventConnection;
import org.lemsml.export.vhdl.edlems.EDEventConnectionItem;
import org.lemsml.export.vhdl.edlems.EDEventPort;
import org.lemsml.export.vhdl.edlems.EDExponential;
//import org.lemsml.export.vhdl.edlems.EDExposure;
import org.lemsml.export.vhdl.edlems.EDLine;
import org.lemsml.export.vhdl.edlems.EDLink;
import org.lemsml.export.vhdl.edlems.EDPower;
import org.lemsml.export.vhdl.edlems.EDRegime;
import org.lemsml.export.vhdl.edlems.EDRequirement;
import org.lemsml.export.vhdl.edlems.EDSimulation;
import org.lemsml.export.vhdl.edlems.EDStateAssignment;
import org.lemsml.export.vhdl.metadata.MetadataWriter;
import org.lemsml.export.vhdl.writer.Constraints;
import org.lemsml.export.vhdl.writer.Entity;
import org.lemsml.export.vhdl.writer.SiElegansTop;
import org.lemsml.export.vhdl.writer.Testbench;
import org.lemsml.export.vhdl.writer.TopSynth;
import org.lemsml.jlems.core.expression.ParseError;
import org.lemsml.jlems.core.flatten.ComponentFlattener;
import org.lemsml.jlems.core.logging.E;
import org.lemsml.jlems.core.logging.MinimalMessageHandler;
import org.lemsml.jlems.core.run.ConnectionError;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.type.Lems;
import org.neuroml.export.base.BaseWriter;
import org.lemsml.jlems.core.type.Attachments;
import org.lemsml.jlems.core.type.Component;
import org.lemsml.jlems.core.type.ComponentType;
import org.lemsml.jlems.core.type.Constant;
import org.lemsml.jlems.core.type.DerivedParameter;
import org.lemsml.jlems.core.type.Dimension;
import org.lemsml.jlems.core.type.EventPort;
import org.lemsml.jlems.core.type.Exposure;
import org.lemsml.jlems.core.type.FinalExposed;
import org.lemsml.jlems.core.type.FinalParam;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.core.type.LemsCollection;
import org.lemsml.jlems.core.type.Link;
import org.lemsml.jlems.core.type.ParamValue;
import org.lemsml.jlems.core.type.Parameter;
import org.lemsml.jlems.core.type.Property;
import org.lemsml.jlems.core.type.Requirement;
import org.lemsml.jlems.core.type.Target;
import org.lemsml.jlems.core.type.dynamics.Case;
import org.lemsml.jlems.core.type.dynamics.ConditionalDerivedVariable;
import org.lemsml.jlems.core.type.dynamics.DerivedVariable;
import org.lemsml.jlems.core.type.dynamics.Dynamics;
import org.lemsml.jlems.core.type.dynamics.EventOut;
import org.lemsml.jlems.core.type.dynamics.OnCondition;
import org.lemsml.jlems.core.type.dynamics.OnEntry;
import org.lemsml.jlems.core.type.dynamics.OnEvent;
import org.lemsml.jlems.core.type.dynamics.OnStart;
import org.lemsml.jlems.core.type.dynamics.Regime;
import org.lemsml.jlems.core.type.dynamics.StateAssignment;
import org.lemsml.jlems.core.type.dynamics.StateVariable;
import org.lemsml.jlems.core.type.dynamics.TimeDerivative;
import org.lemsml.jlems.core.type.dynamics.Transition;
import org.lemsml.jlems.core.util.StringUtil;
import org.lemsml.jlems.io.xmlio.XMLSerializer;
import org.neuroml.export.Utils;



public class VHDLWriter extends BaseWriter {
	
	enum SOMKeywords
	{
	    DT,
	    DYNAMICS,
	    EVENTS,
	    CONDITION,
	    DIRECTION,
	    EFFECT,
	    NAME,
	    ID,
	    PARAMETERS,
	    STATE,
	    STATE_FUNCTIONS,
	    REGIMES,
	    T_END,
	    T_START,
	    COMMENT,
	    EXPOSURES,
	    EVENTPORTS,
	    CONSTANT,
	    CONDITIONS,
	    CONDITIONLIST,
	    DISPLAYS,
	    SIMLENGTH,
	    TRANSITIONS,
	    ONENTRYS,
	    LINKS,
	    DERIVEDPARAMETERS,
	    DERIVEDVARIABLES,
	    CONDITIONALDERIVEDVARIABLES,
	    ATTACHMENTS,
	    REQUIREMENTS,
	    SENSITIVITYLIST,
	    CASES,
	    STEPS;

		public String get()
		{
		return this.toString().toLowerCase();
		}
	}
	
	public enum ScriptType {
		TESTBENCH,
		SYNTH_TOP,
		SIELEGANS_TOP,
		DEFAULTPARAMJSON,
		DEFAULTREADBACKJSON,
		CONSTRAINTS
	}
	
	public enum Method {
		TESTBENCH("vhdl/vhdl_tb.vm"),
		SYNTH_TOP("vhdl/vhdl_synth_top.vm"),
		SIELEGANS_TOP("vhdl/vhdl_sielegans_top.vm"),
		DEFAULTPARAMJSON("vhdl/json_default.vm"),
		DEFAULTREADBACKJSON("vhdl/json_readback_default.vm"),
		MUX("vhdl/ParamMux.vhd"),
		EXP("vhdl/ParamExp.vhd"),
		POW("vhdl/ParamPow.vhd"),
		COUNTER("vhdl/delayDone.vhd");
		
		
	 private String filename;
	 
	 private Method(String f) {
		 filename = f;
	 }
	 
	 public String getFilename() {
	   return filename;
	 }};
	

	public VHDLWriter(Lems lems) {
		super(lems, "VHDL");
		MinimalMessageHandler.setVeryMinimal(true);
		E.setDebug(false);
	}

	String comm = "% ";
	String commPre = "%{";
	String commPost = "%}";
	
	@Override
	protected void addComment(StringBuilder sb, String comment) {
	
		if (comment.indexOf("\n") < 0)
			sb.append(comm + comment + "\n");
		else
			sb.append(commPre + "\n" + comment + "\n" + commPost + "\n");
	}
	


	public Map<String,String> getNeuronModelScripts(String neuronModel, boolean useFlattenedModels) throws ContentError, ParseError, ConnectionError {

		Map<String,String> componentScripts = new HashMap<String,String>();

		
		Velocity.init();
		
		VelocityContext context = new VelocityContext();

		
		JsonFactory f = new JsonFactory();
		
		Target target = lems.getTarget();
		Component simCpt = target.getComponent();
		EDComponent edComponent = new EDComponent();
		
		try
		{
		    if (useFlattenedModels)
		    {
			//try to flatten
		    	ComponentFlattener cf = new ComponentFlattener(lems, lems.getComponent(neuronModel));
		
		        ComponentType ctFlat;
		        Component cpFlat; 
		    	ctFlat = cf.getFlatType();
				cpFlat = cf.getFlatComponent();
		
				lems.addComponentType(ctFlat);
				lems.addComponent(cpFlat);
		
				lems.resolve(ctFlat);
				lems.resolve(cpFlat);
		
		        String typeOut = XMLSerializer.serialize(ctFlat);
		        String cptOut = XMLSerializer.serialize(cpFlat);
		      
		        E.info("Flat type: \n" + typeOut);
		        E.info("Flat cpt: \n" + cptOut);
			
			
				writeSOMForComponent(edComponent, cpFlat,true);
			}
		    else
				writeSOMForComponent(edComponent, lems.getComponent(neuronModel),true);
			
		
			Boolean expUsed = false;
			Boolean powUsed = false;
			Boolean counterUsed = false;
			
			
			loopOverEDComponent(edComponent,componentScripts,expUsed,powUsed);
			for (String script : componentScripts.values())
			{
				if (script.toString().contains(": ParamExp"))
					expUsed = true;
				if (script.toString().contains(": ParamPow"))
					powUsed = true;
				if (script.toString().contains(": delayDone"))
					counterUsed = true;
			}
			if (expUsed )
			{
				componentScripts.put("ParamExp",getExpScript());
			}
			if (powUsed )
			{
				componentScripts.put("ParamPow",getPowScript());
			}
			if (counterUsed )
			{
				componentScripts.put("delayDone",getCounterScript());
			}
			//componentScripts.put("ParamMux",getMuxScript());
			componentScripts.put("top_synth",getSimulationScript(ScriptType.SYNTH_TOP));
			
		}
		catch (IOException e1) {
			throw new ParseError("Problem converting LEMS to SOM",e1);
		}
		catch( ResourceNotFoundException e )
		{
			throw new ParseError("Problem finding template",e);
		}
		catch( ParseErrorException e )
		{
			throw new ParseError("Problem parsing",e);
		}
		catch( MethodInvocationException e )
		{
			throw new ParseError("Problem finding template",e);
		}
		catch( Exception e )
		{
			throw new ParseError("Problem using template",e);
		}
	
		
		
		return componentScripts;

	}
	
	private void loopOverEDComponent(EDComponent edComponent,
			Map<String,String> componentScripts, Boolean expUsed, Boolean powUsed)
	{
		StringBuilder newScript = new StringBuilder();
		Entity.writeEDComponent(edComponent, newScript, edComponent.name.matches("neuron_model"));
		componentScripts.put(edComponent.name,newScript.toString());
		if (newScript.toString().contains(": ParamExp"))
			expUsed = true;
		if (newScript.toString().contains(": ParamPow"))
			powUsed = true;
		for (EDComponent child : edComponent.Children)
		{
			loopOverEDComponent(child, componentScripts, expUsed, powUsed);
		}
	}
	

	public String getSimulationScript(ScriptType scriptType) throws ContentError, ParseError {

		StringBuilder output = new StringBuilder();
		

		//context.put( "name", new String("VelocityOnOSB") );
		try
		{
			DLemsWriter somw = new DLemsWriter(lems);

			Target target = lems.getTarget();
			Component simCpt = target.getComponent();
			EDSimulation edSimulation = new EDSimulation();
			edSimulation.dt = simCpt.getParamValue("step").stringValue();
			edSimulation.simlength =  simCpt.getParamValue("length").stringValue();

			String lengthStr = simCpt.getParamValue("length").stringValue();
			String stepStr = simCpt.getParamValue("step").stringValue();
			Double numsteps = Double.parseDouble(lengthStr)
					/ Double.parseDouble(stepStr);
			numsteps = Math.ceil(numsteps);
			edSimulation.steps = numsteps.toString();
			String targetId = simCpt.getStringValue("target");
			
			List<Component> simComponents = simCpt.getAllChildren();
			writeDisplays(edSimulation, simComponents);
			
			Component networkComp = lems.getComponent(targetId);
			List<Component> networkComponents = networkComp.getAllChildren();
			//TODO: order networkComponents by type so all populations come through in one go
			List<String> neuronTypes = new ArrayList<String>();
			edSimulation.neuronComponents = new ArrayList<EDComponent>();
			int count = 0;
			for (int i = 0; i < networkComponents.size();i++)
			{
				Component comp = networkComponents.get(i);
				ComponentType comT = comp.getComponentType();
				if (comT.name.toLowerCase().matches("population"))
				{
						//add a new neuron component
					Component neuron = lems.getComponent(comp.getStringValue("component"));
					if (!neuronTypes.contains(neuron.getID()) && !neuron.getID().contains("spikeGen"))
					{
						EDComponent edComponent = new EDComponent();
						writeNeuronComponent(edComponent, neuron);
						neuronTypes.add(neuron.getID());
						edSimulation.neuronComponents.add(edComponent);
					}
					else if (neuron.getID().contains("spikeGen"))
					{
						count++;
					}
				}
			}
			edSimulation.neuronInstances = new ArrayList<EDComponent>();

			for (int i = 0; i < networkComponents.size();i++)
			{
				Component comp = networkComponents.get(i);
				ComponentType comT = comp.getComponentType();
				if (comT.name.toLowerCase().matches("population"))
				{
					for (int n = 1; n <= (int)(comp.getParamValue("size").value); n++)
					{
						//add a new neuron component
						EDComponent edComponent = new EDComponent();
						Component neuron = lems.getComponent(comp.getStringValue("component"));
						writeNeuron(edComponent, neuron, n);
						edSimulation.neuronInstances.add(edComponent);
					}
				}
			}

			edSimulation.eventConnections = new ArrayList<EDEventConnection>();
			
			for (int i = 0; i < networkComponents.size();i++)
			{
				Component comp = networkComponents.get(i);
				ComponentType comT = comp.getComponentType();
				if (comT.name.matches("EventConnectivity"))
				{
					String sourceNeuronID = networkComp.components.getByID(comp.getStringValue("source")).getStringValue("component");
					String targetNeuronID = networkComp.components.getByID(comp.getStringValue("target")).getStringValue("component");

					Component sourceComp = lems.getComponent(sourceNeuronID);
					Component targetComp = lems.getComponent(targetNeuronID);

					EDEventConnection edEventConnection = new EDEventConnection();	
					edEventConnection.source = new EDEventConnectionItem();
					edEventConnection.source.name=(sourceNeuronID);
					edEventConnection.source.eventports = writeEventPorts(sourceComp.getComponentType());
						

					edEventConnection.target = new EDEventConnectionItem();
					edEventConnection.target.name=(targetNeuronID);
					edEventConnection.target.eventports =writeEventPorts( targetComp.getComponentType());
					edSimulation.eventConnections.add(edEventConnection);
				}
			}

			edSimulation.synapseCount = (count);
			
			if (scriptType == ScriptType.DEFAULTPARAMJSON){
				MetadataWriter.writeJSONDefaultParameters(edSimulation, output);
			} else if (scriptType == ScriptType.DEFAULTREADBACKJSON){
				MetadataWriter.writeJSONDefaultReadback(edSimulation, output);
			} else if (scriptType == ScriptType.SIELEGANS_TOP){
				SiElegansTop.writeTop(edSimulation, output);
			} else if (scriptType == ScriptType.SYNTH_TOP){
				TopSynth.writeTop(edSimulation, output);
			} else if (scriptType == ScriptType.TESTBENCH){
				Testbench.writeTestBench(edSimulation, output);
			} else if (scriptType == ScriptType.CONSTRAINTS){
				Constraints.writeConstraintsFile(edSimulation, output);
			}
			
			
			
		//simCpt is the simulation tag which translates to the testbench and to the exporter top level
		} 
		catch (IOException e1) {
			throw new ParseError("Problem converting LEMS to SOM",e1);
		}
		catch( ResourceNotFoundException e )
		{
			throw new ParseError("Problem finding template",e);
		}
		catch( ParseErrorException e )
		{
			throw new ParseError("Problem parsing",e);
		}
		catch( MethodInvocationException e )
		{
			throw new ParseError("Problem finding template",e);
		}
		catch( Exception e )
		{
			throw new ParseError("Problem using template",e);
		}
		

		
		return output.toString();
	}
	
	/*public String getMainScript() throws ContentError, ParseError {
		return getMainScript(Method.TESTBENCH);
	}*/

	


	//this file is required by Xilinx Fuse to compile a simulation of the vhdl testbench
	public String getPrjFile(Set<String> files) throws ContentError, ParseError {
	
		StringBuilder sb = new StringBuilder();
		for (String file: files)
		{
			sb.append("vhdl work \"" + file + ".vhdl\"\r\n");
		}
		sb.append("vhdl work \"testbench.vhdl\"\r\n");

		
		return sb.toString();
	}
	
	
	//this file is required by Xilinx ISIM simulations for automation purposes
	public String getTCLScript(double simTime, double simTimeStep) throws ContentError, ParseError {
		StringBuilder sb = new StringBuilder();
	
		//todo: this file should reflect some simulation settings
		sb.append("onerror {resume}\r\nwave add /\r\nrun " + ( 100 + (simTime/simTimeStep) ) + " us;\r\nexit\r\n");
		return sb.toString();
	}
	

	//this file is required by Xilinx ISIM simulations for automation purposes
	private String getMuxScript() throws ContentError, ParseError {
		StringBuilder sb = new StringBuilder();
		
		Velocity.init();
		
		VelocityContext context = new VelocityContext();

		Properties props = new Properties();
		props.put("resource.loader", "class");
		props.put("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
		VelocityEngine ve = new VelocityEngine();
		ve.init(props);
		
		
		Template template = ve.getTemplate(Method.MUX.getFilename());
		StringWriter sw = new StringWriter();
		template.merge( context, sw );
			sb.append(sw);
			
		return sb.toString();
	}
	
	

	//this file is required by Xilinx ISIM simulations for automation purposes
	private String getExpScript() throws ContentError, ParseError {
		StringBuilder sb = new StringBuilder();
		
		Velocity.init();
		
		VelocityContext context = new VelocityContext();

		Properties props = new Properties();
		props.put("resource.loader", "class");
		props.put("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
		VelocityEngine ve = new VelocityEngine();
		ve.init(props);
		
		
		Template template = ve.getTemplate(Method.EXP.getFilename());
		StringWriter sw = new StringWriter();
		template.merge( context, sw );
			sb.append(sw);
			
		return sb.toString();
	}
	

	private String getCounterScript() throws ContentError, ParseError {
		StringBuilder sb = new StringBuilder();
		
		Velocity.init();
		
		VelocityContext context = new VelocityContext();

		Properties props = new Properties();
		props.put("resource.loader", "class");
		props.put("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
		VelocityEngine ve = new VelocityEngine();
		ve.init(props);
		
		
		Template template = ve.getTemplate(Method.COUNTER.getFilename());
		StringWriter sw = new StringWriter();
		template.merge( context, sw );
			sb.append(sw);
			
		return sb.toString();
	}
	
	
	private String getPowScript() throws ContentError, ParseError {
		StringBuilder sb = new StringBuilder();
		
		Velocity.init();
		
		VelocityContext context = new VelocityContext();

		Properties props = new Properties();
		props.put("resource.loader", "class");
		props.put("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
		VelocityEngine ve = new VelocityEngine();
		ve.init(props);
		
		
		Template template = ve.getTemplate(Method.POW.getFilename());
		StringWriter sw = new StringWriter();
		template.merge( context, sw );
			sb.append(sw);
			
		return sb.toString();
	}

	
	


	

	private void writeNeuron(EDComponent edComponent, Component neuron,int id) throws JsonGenerationException, IOException, ContentError
	{
			edComponent.name = (neuron.getName() + "_" + id);
			writeSOMForComponent(edComponent,neuron,true);
	}

	/*private void writeNeuronDefaults(JsonGenerator g, Component neuron) throws JsonGenerationException, IOException, ContentError
	{
		ComponentType ct = neuron.getComponentType();

		for(FinalParam p: ct.getFinalParams())
		{
			ParamValue pv = neuron.getParamValue(p.getName());
			g.writeStartObject();
			g.writeStringField("name","param_" + pv.getDimensionName() + "_" + p.getName());
			
			g.writeStringField("type",pv.getDimensionName()+"");
			g.writeStringField("value",(float)pv.getDoubleValue()+"");
			VHDLFixedPointDimensions.writeBitLengths(g,pv.getDimensionName());
			
			g.writeEndObject();
		}
		
		for (int i = 0; i < neuron.getAllChildren().size();i++)
		{
			Component comp2 = neuron.getAllChildren().get(i);
			for(FinalParam p: comp2.getComponentType().getFinalParams())
			{
				ParamValue pv = comp2.getParamValue(p.getName());
				g.writeStartObject();
				g.writeStringField("name","param_" +  pv.getDimensionName() + "_" + comp2.getName()  +"_" + p.getName());
				g.writeStringField("type",pv.getDimensionName()+"");
				g.writeStringField("value",(float)pv.getDoubleValue()+"");
				VHDLFixedPointDimensions.writeBitLengths(g,pv.getDimensionName());
				
				g.writeEndObject();
			}
		}
		
		for(Attachments attach: neuron.getComponentType().getAttachmentss())
		{
			int numberID = 0;
			for(Component conn: lems.getComponent("net1").getAllChildren())
			{
				String attachName = attach.getName();
				if (conn.getComponentType().getName().matches("synapticConnection")|| conn.getComponentType().getName().matches("synapticConnectionWD") )
				{
					String destination = conn.getTextParam("destination");
					String path = conn.getPathParameterPath("to");
					if (destination.matches(attachName) && path.startsWith(neuron.getID()))
					{
						Component comp2 = (conn.getRefComponents().get("synapse"));
						for(FinalParam p: comp2.r_type.getFinalParams())
						{
							ParamValue pv = comp2.getParamValue(p.getName());
							g.writeStartObject();
							g.writeStringField("name","param_" + pv.getDimensionName()+ "_" + comp2.getID() + "_" + p.getName());
							g.writeStringField("type",pv.getDimensionName()+"");
							g.writeStringField("value",(float)pv.getDoubleValue()+"");
							VHDLFixedPointDimensions.writeBitLengths(g,pv.getDimensionName());
							
							g.writeEndObject();
						}
					}
				}
			}
		}
	}*/
	

	private void writeNeuronComponent(EDComponent edComponent, Component neuron) throws JsonGenerationException, IOException, ContentError
	{	
			//g.writeObjectFieldStart(neuron.getTypeName());

			String compRef =  neuron.getID();
			if (compRef.contains("_flat"))
			{
				compRef = compRef.replace("_flat", "");
			}
			edComponent.name= compRef;
			//g.writeStringField("name",neuron.getTypeName());
			writeSOMForComponent(edComponent,neuron,true);
			
	}
	
	private EDDisplay writeDisplay( Component display) throws JsonGenerationException, IOException, ContentError
	{
			EDDisplay edDisplay = new EDDisplay();
			edDisplay.name=(display.getName());
			edDisplay.timeScale=(display.getParamValue("timescale")+"");
			edDisplay.xmin=(display.getParamValue("xmin")+"");
			edDisplay.xmax=(display.getParamValue("xmax")+"");
			edDisplay.lines = new ArrayList<EDLine>();
			List<Component> lines = display.getAllChildren();
			for (int i = 0; i < lines.size(); i++)
			{
				Component line = lines.get(0);
				EDLine edLine = new EDLine();
				edLine.id=(line.getID()+""); 
				edLine.quantity=(line.getStringValue("quantity")+"");
				edLine.scale=(line.getStringValue("scale")+"");
				edLine.timeScale=(line.getStringValue("timeScale")+"");
				edDisplay.lines.add(edLine);
			}
			return edDisplay;
	}
	
	private void writeDisplays(EDSimulation edSimulation, List<Component> displays) throws JsonGenerationException, IOException, ContentError
	{
		edSimulation.displays = new ArrayList<EDDisplay>();
		for (int i = 0; i < displays.size(); i++)
		{
			Component display = displays.get(i);
			ComponentType comT = display.getComponentType();
			if (comT.name.matches("Display"))
			{
				//add display processes with time resolution and start and end as specified
				edSimulation.displays.add( writeDisplay( display));
			}
		}
		
	}
	
	
	private void writeSOMForComponent(EDComponent edComponent, Component comp, boolean writeChildren) throws JsonGenerationException, IOException, ContentError
	{
		
		ComponentType ct = comp.getComponentType();
		Dynamics dyn =  ct.getDynamics();
		
		
		LemsCollection<FinalParam> parameters = new LemsCollection<FinalParam>();
		parameters.addAll(comp.getComponentType().getFinalParams());
		LemsCollection<ParamValue> parameterValues = new LemsCollection<ParamValue>();
		parameterValues.addAll(comp.getParamValues());

		for (Property prop: ct.getPropertys())
		{
			FinalParam fp = new FinalParam(prop.name,prop.getDimension());
			parameters.add(fp);
			parameterValues.add(new ParamValue(fp,1));
		}
		
		
		if (dyn != null)
			edComponent.dynamics = VHDLDynamics.writeTimeDerivatives( ct, dyn.getTimeDerivatives(),parameters,parameterValues,"noregime_");
		 
		edComponent.derivedparameters = VHDLParameters.writeDerivedParameters( ct, ct.getDerivedParameters(),parameters,parameterValues);
		

		if (dyn != null)
			VHDLDynamics.writeDerivedVariables(edComponent, ct, dyn.getDerivedVariables(),comp,parameters,parameterValues,lems);
		
		if (dyn != null)
			writeConditionalDerivedVariables(edComponent, ct, dyn.getConditionalDerivedVariables(),comp,parameters,parameterValues);
		
		
		if (dyn != null)
			edComponent.conditions = VHDLDynamics.writeConditions( ct,dyn.getOnConditions(),parameters,parameterValues);


		if (dyn != null)
			edComponent.events = VHDLDynamics.writeEvents( ct, dyn.getOnEvents(),parameters,parameterValues);
		 

		String compRef =  comp.getID();
		if (compRef.contains("_flat"))
		{
			compRef = compRef.replace("_flat", "");
		}
		edComponent.name = (compRef);


		writeRequirements(edComponent, comp.getComponentType().getRequirements());

		//edComponent.exposures = writeExposures(comp.getComponentType());
		
		edComponent.eventports = writeEventPorts( comp.getComponentType());
		
		VHDLDynamics.writeState(edComponent, comp,parameters,parameterValues);

		VHDLDynamics.writeStateFunctions(edComponent, comp);
		
		if (writeChildren)
		{
			edComponent.Children = new ArrayList<EDComponent>();
			
			for (int i = 0; i < comp.getAllChildren().size();i++)
			{
				Component comp2 = comp.getAllChildren().get(i);
				EDComponent child = new EDComponent();
				writeNeuronComponent(child, comp2);
				edComponent.Children.add(child);
			}
			//Attachments synapses are children in this initial model of VHDL neurons
			for(Attachments attach: comp.getComponentType().getAttachmentss())
			{
				int numberID = 0;
				for(Component conn: lems.getComponent("net1").getAllChildren())
				{
					String attachName = attach.getName();
					if (conn.getComponentType().getName().matches("synapticConnection")  || conn.getComponentType().getName().matches("synapticConnectionWD")  )
					{
						String destination = conn.getTextParam("destination");
						String path = conn.getPathParameterPath("to");
						if ((destination == null || destination.matches(attachName)) && path.startsWith(comp.getID()))
						{
							Component comp2 = (conn.getRefComponents().get("synapse"));
							//comp2.setID(attach.getName() + "_" +  numberID);
							EDComponent child = new EDComponent();
							writeNeuronComponent(child, comp2);
							edComponent.Children.add(child);
							numberID++;
						}
					}
				}
			}
			//end
		}
		
		writeLinks(edComponent, comp);

		VHDLDynamics.writeRegimes(edComponent, comp,writeChildren, parameters, parameterValues,lems );
		
		if (dyn != null)
			VHDLDynamics.optimiseDerivedVariables(edComponent, ct);
		

		edComponent.parameters = VHDLParameters.writeParameters(comp, parameters, parameterValues );

		for (EDComponent child : edComponent.Children)
		{
			checkExposuresNeeded(child,edComponent);
		}
	}
	
	private void checkExposuresNeeded(EDComponent edComponentCurrent, EDComponent edComponentParent) throws ContentError, JsonGenerationException, IOException
	{
		//check every derivedVariable and conditionalDerivedVariable to see if they have exposures

		for (EDDerivedVariable dv: edComponentCurrent.derivedvariables)
		{
			if (dv.exposure != null && dv.exposure.length() > 0)
			{
				//check if this derivedvariable is used anywhere
				dv.ExposureIsUsed = (checkExposureUsed("exposure_" + dv.type+ "_" + 
						edComponentCurrent.name + "_" + dv.name + "_internal",
				edComponentParent));
				dv.IsUsedForOtherDerivedVariables = (checkDerivedVariableUsedInDerivedVariable("exposure_" + dv.type+ "_" + 
						edComponentCurrent.name + "_" + dv.name + "_internal",
						edComponentParent));
			}	
			else
			{
				dv.ExposureIsUsed = false;
				dv.IsUsedForOtherDerivedVariables = false;
			}
		}
		for (EDConditionalDerivedVariable dv: edComponentCurrent.conditionalderivedvariables)
		{
			if (dv.exposure != null && dv.exposure.length() > 0)
			{
				//check if this derivedvariable is used anywhere
				dv.ExposureIsUsed = (checkExposureUsed("exposure_" + dv.type+ "_" + 
						edComponentCurrent.name + "_" + dv.name + "_internal",
				edComponentParent));
				dv.IsUsedForOtherDerivedVariables = (checkDerivedVariableUsedInDerivedVariable("exposure_" + dv.type+ "_" + 
						edComponentCurrent.name + "_" + dv.name + "_internal",
						edComponentParent));
			}	
		}
		for (EDComponent child : edComponentCurrent.Children)
		{
			checkExposuresNeeded(child,edComponentCurrent);
		}
		
	}
	

	private boolean checkDerivedVariableUsedInDerivedVariable(String exposure, EDComponent edComponentParent)
	{
		for (EDDerivedVariable dv: edComponentParent.derivedvariables)
		{
			if (dv.value.contains(exposure))
				return true;			

			for (EDExponential de: dv.Exponentials)
			{
				if (de.value.contains(exposure))
					return true;		
			}

			for (EDPower dp: dv.Powers)
			{
				if (dp.valueA.contains(exposure))
					return true;	
				if (dp.valueX.contains(exposure))
					return true;		
			}
		}

		for (EDConditionalDerivedVariable cdv: edComponentParent.conditionalderivedvariables)
		{
			for (EDCase cde : cdv.cases)
			{
				if (cde.value.contains(exposure))
					return true;			
	
				for (EDExponential de: cde.Exponentials)
				{
					if (de.value.contains(exposure))
						return true;		
				}
	
				for (EDPower dp: cde.Powers)
				{
					if (dp.valueA.contains(exposure))
						return true;	
					if (dp.valueX.contains(exposure))
						return true;		
				}
			}
		}		
		return false;
	}
	
	private boolean checkExposureUsed(String exposure, EDComponent edComponentParent)
	{
		for (EDDerivedVariable dv: edComponentParent.derivedvariables)
		{
			if (dv.value.contains(exposure))
				return true;			

			for (EDExponential de: dv.Exponentials)
			{
				if (de.value.contains(exposure))
					return true;		
			}

			for (EDPower dp: dv.Powers)
			{
				if (dp.valueA.contains(exposure))
					return true;	
				if (dp.valueX.contains(exposure))
					return true;		
			}
		}

		for (EDConditionalDerivedVariable cdv: edComponentParent.conditionalderivedvariables)
		{
			for (EDCase cde : cdv.cases)
			{
				if (cde.value.contains(exposure))
					return true;			
	
				for (EDExponential de: cde.Exponentials)
				{
					if (de.value.contains(exposure))
						return true;		
				}
	
				for (EDPower dp: cde.Powers)
				{
					if (dp.valueA.contains(exposure))
						return true;	
					if (dp.valueX.contains(exposure))
						return true;		
				}
			}
		}

		for (EDCondition con : edComponentParent.conditions)
		{
			if (con.condition.contains(exposure))
				return true;	
			for (EDStateAssignment conSA : con.stateAssignment)
			{
				if (conSA.expression.contains(exposure))
					return true;	
			}
		}
		for (EDEvent event : edComponentParent.events)
		{
			for (EDStateAssignment conSA : event.stateAssignments)
			{
				if (conSA.expression.contains(exposure))
					return true;	
			}
		}
		for (EDDynamic dynamic : edComponentParent.dynamics)
		{
			if (dynamic.Dynamics.contains(exposure))
				return true;	
			for (EDExponential de: dynamic.Exponentials)
			{
				if (de.value.contains(exposure))
					return true;		
			}

			for (EDPower dp: dynamic.Powers)
			{
				if (dp.valueA.contains(exposure))
					return true;	
				if (dp.valueX.contains(exposure))
					return true;		
			}
		}
		
		for (EDRegime regime : edComponentParent.regimes)
		{
			for (EDConditionalDerivedVariable cdv: regime.conditionalderivedvariables)
			{
				for (EDCase cde : cdv.cases)
				{
					if (cde.value.contains(exposure))
						return true;			
		
					for (EDExponential de: cde.Exponentials)
					{
						if (de.value.contains(exposure))
							return true;		
					}
		
					for (EDPower dp: cde.Powers)
					{
						if (dp.valueA.contains(exposure))
							return true;	
						if (dp.valueX.contains(exposure))
							return true;		
					}
				}
			}

			for (EDCondition con : regime.conditions)
			{
				if (con.condition.contains(exposure))
					return true;	
				for (EDStateAssignment conSA : con.stateAssignment)
				{
					if (conSA.expression.contains(exposure))
						return true;	
				}
			}
			for (EDEvent event : regime.events)
			{
				for (EDStateAssignment conSA : event.stateAssignments)
				{
					if (conSA.expression.contains(exposure))
						return true;	
				}
			}
			for (EDDynamic dynamic : regime.dynamics)
			{
				if (dynamic.Dynamics.contains(exposure))
					return true;	
				for (EDExponential de: dynamic.Exponentials)
				{
					if (de.value.contains(exposure))
						return true;		
				}

				for (EDPower dp: dynamic.Powers)
				{
					if (dp.valueA.contains(exposure))
						return true;	
					if (dp.valueX.contains(exposure))
						return true;		
				}
			}
		}
		
		return false;
	}

	
	

	private void writeLinks(EDComponent edComponent, Component comp) throws ContentError, JsonGenerationException, IOException
	{
		ComponentType ct = comp.getComponentType();
		edComponent.links = new ArrayList<EDLink>();
		for (Link link: ct.getLinks())
		{
			EDLink edLink = new EDLink();
			edLink.name=(link.getName());
			ComponentType compType = lems.getComponentTypeByName(link.getTargetType());
			
			//edLink.exposures = writeExposures( compType);
			
			edLink.eventports = writeEventPorts( compType);

			edComponent.links.add(edLink);
		}
	}

	
	
	
	private void writeRequirements(EDComponent edComponent, LemsCollection<Requirement> requirements) throws ContentError, JsonGenerationException, IOException
	{
		edComponent.requirements = new ArrayList<EDRequirement>();
		for (Requirement req : requirements)
		{
			EDRequirement edRequirement = new EDRequirement();
			edRequirement.name = (req.getName());
			edRequirement.type=(req.getDimension().getName()+"");
			VHDLFixedPointDimensions.writeBitLengths(edRequirement,req.getDimension().getName());
			edComponent.requirements.add(edRequirement);
		}
	}
	
	/*private ArrayList<EDExposure> writeExposures( ComponentType ct) throws ContentError, JsonGenerationException, IOException
	{
		ArrayList<EDExposure> exposures = new ArrayList<EDExposure>();
		for(FinalExposed e: ct.getFinalExposures())
		{
			EDExposure edExposure = new EDExposure();
			edExposure.name=(e.getName());
			edExposure.type=(e.getDimension().getName()+"");
			VHDLFixedPointDimensions.writeBitLengths(edExposure,e.getDimension().getName());
			exposures.add(edExposure);
		}
		return exposures;
	}*/
	

	private ArrayList<EDEventPort> writeEventPorts( ComponentType ct) throws ContentError, JsonGenerationException, IOException
	{
		ArrayList<EDEventPort> eventports = new ArrayList<EDEventPort>();
		for(EventPort e: ct.getEventPorts())
		{
			EDEventPort edEventPort = new EDEventPort();
			edEventPort.name=(e.getName());
			edEventPort.direction=(e.direction+"");
			eventports.add(edEventPort);
		}
		return eventports;
	}
	

	

	
	


	

	private void writeConditionalDerivedVariables(EDComponent edComponent, ComponentType ct, 
			LemsCollection<ConditionalDerivedVariable> conditionalDerivedVariables, Component comp
			, LemsCollection<FinalParam> params,LemsCollection<ParamValue> combinedParameterValues) throws ContentError, JsonGenerationException, IOException
	{
		edComponent.conditionalderivedvariables = new ArrayList<EDConditionalDerivedVariable>();
		
		for (ConditionalDerivedVariable dv: conditionalDerivedVariables)
		{
			EDConditionalDerivedVariable edConditionalDerivedVariable = new EDConditionalDerivedVariable();
			
			edConditionalDerivedVariable.name=(dv.getName()); 
			edConditionalDerivedVariable.exposure=(dv.getExposure() != null ? dv.getExposure().getName() : "");
 
	        StringBuilder sensitivityList = new StringBuilder();
	        edConditionalDerivedVariable.cases = new ArrayList<EDCase>();
			Integer i = 0;
			for (Case dv2: dv.cases)
			{
				EDCase edCase = new EDCase();
				edCase.name=(i.toString()); 
				String val = dv2.getValueExpression();
		
				if (val != null) {
					String value = VHDLEquations.encodeVariablesStyle(dv2.getValueExpression(),
							ct.getFinalParams(),ct.getDynamics().getStateVariables(),ct.getDynamics().getDerivedVariables(),
							ct.getRequirements(),ct.getPropertys(),sensitivityList,params,combinedParameterValues);

					value = VHDLEquations.writeInternalExpLnLogEvaluators(value,edCase,dv.getName(),sensitivityList,"");
					edCase.value = ( value );
					edCase.condition = VHDLDynamics.writeConditionList(ct,dv2.condition,sensitivityList,params,combinedParameterValues);
				} 
				i++;
				edConditionalDerivedVariable.cases.add(edCase);
				
			}

			edConditionalDerivedVariable.type=(dv.getDimension().getName()+"");
			edConditionalDerivedVariable.sensitivityList=(sensitivityList.length() == 0 ? "" : sensitivityList.substring(0,sensitivityList.length()-1));
			VHDLFixedPointDimensions.writeBitLengths(edConditionalDerivedVariable,dv.getDimension().getName());

			edComponent.conditionalderivedvariables.add(edConditionalDerivedVariable);
		}

	}
	

	@Override
	protected void setSupportedFeatures() {
		// TODO Auto-generated method stub
		
	}



	@Override
	public String getMainScript() throws GenerationException, JAXBException,
			Exception {
		// TODO Auto-generated method stub
		return null;
	}
}


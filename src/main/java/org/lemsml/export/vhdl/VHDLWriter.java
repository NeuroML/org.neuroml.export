package org.lemsml.export.vhdl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
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
import org.lemsml.export.dlems.DLemsWriter;
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
	
	public enum Method {
		TESTBENCH("vhdl/vhdl_tb.vm"),
		SYNTH_TOP("vhdl/vhdl_synth_top.vm"),
		DEFAULTPARAMJSON("vhdl/json_default.vm"),
		DEFAULTREADBACKJSON("vhdl/json_readback_default.vm"),
		COMPONENT1("vhdl/vhdl_comp_1_entity.vm"),
		COMPONENT2("vhdl/vhdl_comp_2_arch.vm"),
		COMPONENT3("vhdl/vhdl_comp_3_child_instantiations.vm"),
		COMPONENT4("vhdl/vhdl_comp_4_statemachine.vm"),
		COMPONENT5("vhdl/vhdl_comp_5_derivedvariable.vm"),
		COMPONENT6("vhdl/vhdl_comp_6_statevariable_processes.vm"),
		COMPONENT7("vhdl/vhdl_comp_7_outputport_processes.vm"),
		MUX("vhdl/ParamMux.vhd"),
		EXP("vhdl/ParamExp.vhd"),
		POW("vhdl/ParamPow.vhd");
		
		
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
		StringBuilder sb = new StringBuilder();

		//sb.append("--" + this.format+" simulator compliant export for:--\n--\n");
		
		Velocity.init();
		
		VelocityContext context = new VelocityContext();

		//context.put( "name", new String("VelocityOnOSB") );

		//DLemsWriter somw = new DLemsWriter(lems);
		
		JsonFactory f = new JsonFactory();
		
		Target target = lems.getTarget();
		Component simCpt = target.getComponent();
		//String targetId = simCpt.getStringValue("target");
		//Component tgtComp = lems.getComponent(targetId);
		ArrayList<Component> temppops = new ArrayList<Component>();
		ArrayList<Component> pops = new ArrayList<Component>();
		

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
		
		
        	temppops.add(cpFlat);
		}
        else
        	temppops.add(lems.getComponent(neuronModel));
		
		String targetId = simCpt.getStringValue("target");
		Component networkComp = lems.getComponent(targetId);
		List<Component> networkComponents = networkComp.getAllChildren();
		for (Component comp : networkComponents)
		{
			if (comp.getTypeName().matches("synapticConnection") || comp.getTypeName().matches("synapticConnectionWD"))
			{
				String synapseName = comp.getStringValue("synapse");

		        if (useFlattenedModels)
		        {
				  ComponentFlattener cf2 = new ComponentFlattener(lems, lems.getComponent(synapseName));

			        ComponentType ct2Flat;
			        Component cp2Flat;
			    	ct2Flat = cf2.getFlatType();
					cp2Flat = cf2.getFlatComponent();
					ComponentType typeFlat = null;
					try{
					 typeFlat = lems.getComponentTypeByName(ct2Flat.name);
					}
					catch (Exception e)
					{
						
					}
					if (typeFlat == null)
						lems.addComponentType(ct2Flat);

					lems.addComponent(cp2Flat);
					

					if (typeFlat == null)
						lems.resolve(ct2Flat);
					lems.resolve(cp2Flat);
					

			        String type2Out = XMLSerializer.serialize(ct2Flat);
			        String cpt2Out = XMLSerializer.serialize(cp2Flat);
			      
			        E.info("Flat type: \n" + type2Out);
			        E.info("Flat cpt: \n" + cpt2Out);

		        	temppops.add(cp2Flat);
		        }
			        else
	        	temppops.add(lems.getComponent(synapseName));
			}
		}

		boolean expUsed = false;
		boolean powUsed = false;
		//temppops.addAll(lems.getComponents().getContents());//temppops.add(tgtComp);
		try
		{
			
			while (temppops.size() > 0)
			{
				pops.clear();
				pops.addAll(temppops);
				temppops.clear();
				for	(Component pop : pops)
				{
					if (!useFlattenedModels)
					temppops.addAll(pop.getAllChildren());
					
						StringWriter sw = new StringWriter();
						JsonGenerator g = f.createJsonGenerator(sw);
						//g.useDefaultPrettyPrinter();
						g.writeStartObject();
						pop.getComponentType().getLinks();
						String compRef = pop.getID();// pop.getStringValue("component");
						Component popComp = pop;//pop.getComponentType();//lems.getComponent(compRef);
		
						writeSOMForComponent(g, popComp,true);
						g.writeStringField(SOMKeywords.T_END.get(), simCpt.getParamValue("length").stringValue());
						g.writeStringField(SOMKeywords.T_START.get(), "0");
						g.writeStringField(SOMKeywords.COMMENT.get(), Utils.getHeaderComment(FORMAT));
						
						g.writeEndObject();
		
						g.close();
		
						System.out.println(sw.toString());
						
						String som = sw.toString();
					
						DLemsWriter.putIntoVelocityContext(som, context);
					
						Properties props = new Properties();
						props.put("resource.loader", "class");
						props.put("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
						VelocityEngine ve = new VelocityEngine();
						ve.init(props);
						
						int i = 0;
						Template template = ve.getTemplate(Method.COMPONENT1.getFilename());
						sw = new StringWriter();
						template.merge( context, sw );
						sb.append(sw);
						sw = new StringWriter();
						template = ve.getTemplate(Method.COMPONENT2.getFilename());
						sw = new StringWriter();
						template.merge( context, sw );
						sb.append(sw);
						sw = new StringWriter(); 
						template = ve.getTemplate(Method.COMPONENT3.getFilename());
						sw = new StringWriter();
						template.merge( context, sw );
						sb.append(sw);
						sw = new StringWriter();
						template = ve.getTemplate(Method.COMPONENT4.getFilename());
						sw = new StringWriter();
						template.merge( context, sw );
						sb.append(sw);
						sw = new StringWriter();
						template = ve.getTemplate(Method.COMPONENT5.getFilename());
						sw = new StringWriter();
						template.merge( context, sw );
						sb.append(sw);
						sw = new StringWriter();
						template = ve.getTemplate(Method.COMPONENT6.getFilename());
						sw = new StringWriter();
						template.merge( context, sw );
						sb.append(sw);
						sw = new StringWriter();
						template = ve.getTemplate(Method.COMPONENT7.getFilename());
						sw = new StringWriter();
						template.merge( context, sw );
						sb.append(sw);
						
						if (sb.toString().contains(": ParamExp"))
							expUsed = true;
						if (sb.toString().contains(": ParamPow"))
							powUsed = true;
						
						if (compRef.contains("_flat"))
						{
							compRef = compRef.replace("_flat", "");
						}
						componentScripts.put(compRef,sb.toString().replaceAll("(?m)^[ \t]*\r?\n", "").replace("\r\n\r\n", "\r\n").replace("\r\n\r\n", "\r\n").replace("\n\n", "\n").replace("\n\n", "\n"));
						sb = new StringBuilder();
						//System.out.println(compRef);
						System.out.println(sb.toString());
					} 
				
				}
			if (expUsed )
			{
				componentScripts.put("ParamExp",getExpScript());
			}
			if (powUsed )
			{
				componentScripts.put("ParamPow",getPowScript());
			}
			componentScripts.put("ParamMux",getMuxScript());
			componentScripts.put("top_synth",getMainScript_Synth());
			
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

	
	

	
	

	private String getMainScript_Synth() throws ContentError, ParseError {
		StringBuilder sb = new StringBuilder();

		sb.append("--" + this.FORMAT+" simulator compliant export for:--\n--\n");
		
		Velocity.init();
		
		VelocityContext context = new VelocityContext();

		//context.put( "name", new String("VelocityOnOSB") );
		try
		{
			DLemsWriter somw = new DLemsWriter(lems);
			JsonFactory f = new JsonFactory();
			StringWriter sw = new StringWriter();
			JsonGenerator g = f.createJsonGenerator(sw);
			//g.useDefaultPrettyPrinter();
			g.writeStartObject();
			Target target = lems.getTarget();
			Component simCpt = target.getComponent();
			g.writeStringField(SOMKeywords.DT.get(), simCpt.getParamValue("step").stringValue());
			g.writeStringField(SOMKeywords.SIMLENGTH.get(), simCpt.getParamValue("length").stringValue());
			String lengthStr = simCpt.getParamValue("length").stringValue();
			String stepStr = simCpt.getParamValue("step").stringValue();
			Double numsteps = Double.parseDouble(lengthStr)
					/ Double.parseDouble(stepStr);
			numsteps = Math.ceil(numsteps);
			g.writeStringField(SOMKeywords.STEPS.get(), numsteps.toString());
			String targetId = simCpt.getStringValue("target");
			
			List<Component> simComponents = simCpt.getAllChildren();
			writeDisplays(g, simComponents);
			
			Component networkComp = lems.getComponent(targetId);
			List<Component> networkComponents = networkComp.getAllChildren();
			//TODO: order networkComponents by type so all populations come through in one go
			g.writeObjectFieldStart("NeuronComponents");
			List<String> neuronTypes = new ArrayList<String>();

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
						writeNeuronComponent(g, neuron);
						neuronTypes.add(neuron.getID());
					}
					else if (neuron.getID().contains("spikeGen"))
					{
						count++;
					}
				}
			}
			g.writeEndObject();
			g.writeArrayFieldStart("NeuronInstances");
			for (int i = 0; i < networkComponents.size();i++)
			{
				Component comp = networkComponents.get(i);
				ComponentType comT = comp.getComponentType();
				if (comT.name.toLowerCase().matches("population"))
				{
					for (int n = 1; n <= (int)(comp.getParamValue("size").value); n++)
					{
						//add a new neuron component
						Component neuron = lems.getComponent(comp.getStringValue("component"));
						writeNeuron(g, neuron, n);
					}
				}
			}
			g.writeEndArray();
			
			g.writeArrayFieldStart("Connections");
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

					g.writeStartObject();
					
						g.writeObjectFieldStart("Source");
						g.writeStringField("Name",sourceNeuronID);
						g.writeObjectFieldStart(SOMKeywords.EVENTPORTS.get());
						writeEventPorts(g, sourceComp.getComponentType());
						g.writeEndObject();
						g.writeEndObject();
						
						g.writeObjectFieldStart("Target");
						g.writeStringField("Name",targetNeuronID);
						g.writeObjectFieldStart(SOMKeywords.EVENTPORTS.get());
						writeEventPorts(g, targetComp.getComponentType());
						g.writeEndObject();
						g.writeEndObject();
					
					g.writeEndObject();
				}
			}
			g.writeEndArray();
			g.writeNumberField("SynapseCount",count);
			
			

			g.close();
			
			String som = sw.toString();
			
			DLemsWriter.putIntoVelocityContext(som, context);
		
			Properties props = new Properties();
			props.put("resource.loader", "class");
			props.put("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
			VelocityEngine ve = new VelocityEngine();
			ve.init(props);
			Template template = ve.getTemplate(Method.SYNTH_TOP.getFilename());
		   
			sw = new StringWriter();

			template.merge( context, sw );
			
			sb.append(sw);

			System.out.println("TestBenchData");
			System.out.println(sw.toString());
			System.out.println(som);
			
			
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
		

		
		return sb.toString();
	}
	
	public String getMainScript() throws ContentError, ParseError {
		return getMainScript(Method.TESTBENCH);
	}

	

	public String getMainScript(Method method) throws ContentError, ParseError {
		

		StringBuilder sb = new StringBuilder();

		
		Velocity.init();
		
		VelocityContext context = new VelocityContext();

		//context.put( "name", new String("VelocityOnOSB") );
		try
		{
			DLemsWriter somw = new DLemsWriter(lems);
			JsonFactory f = new JsonFactory();
			StringWriter sw = new StringWriter();
			JsonGenerator g = f.createJsonGenerator(sw);
			//g.useDefaultPrettyPrinter();
			g.writeStartObject();
			Target target = lems.getTarget();
			Component simCpt = target.getComponent();
			g.writeStringField(SOMKeywords.DT.get(), simCpt.getParamValue("step").stringValue());
			g.writeStringField(SOMKeywords.SIMLENGTH.get(), simCpt.getParamValue("length").stringValue());
			String lengthStr = simCpt.getParamValue("length").stringValue();
			String stepStr = simCpt.getParamValue("step").stringValue();
			Double numsteps = Double.parseDouble(lengthStr)
					/ Double.parseDouble(stepStr);
			numsteps = Math.ceil(numsteps);
			g.writeStringField(SOMKeywords.STEPS.get(), numsteps.toString());
			String targetId = simCpt.getStringValue("target");
			
			List<Component> simComponents = simCpt.getAllChildren();
			writeDisplays(g, simComponents);
			
			Component networkComp = lems.getComponent(targetId);
			List<Component> networkComponents = networkComp.getAllChildren();
			//TODO: order networkComponents by type so all populations come through in one go
			g.writeObjectFieldStart("NeuronComponents");
			List<String> neuronTypes = new ArrayList<String>();

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
						writeNeuronComponent(g, neuron);
						neuronTypes.add(neuron.getID());
					}
					else if (neuron.getID().contains("spikeGen"))
					{
						count++;
					}
				}
			}
			g.writeEndObject();
			g.writeArrayFieldStart("NeuronInstances");
			for (int i = 0; i < networkComponents.size();i++)
			{
				Component comp = networkComponents.get(i);
				ComponentType comT = comp.getComponentType();
				if (comT.name.toLowerCase().matches("population"))
				{
					for (int n = 1; n <= (int)(comp.getParamValue("size").value); n++)
					{
						//add a new neuron component
						Component neuron = lems.getComponent(comp.getStringValue("component"));
						writeNeuron(g, neuron, n);
					}
				}
			}
			g.writeEndArray();
			
			g.writeArrayFieldStart("Connections");
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

					g.writeStartObject();
					
						g.writeObjectFieldStart("Source");
						g.writeStringField("Name",sourceNeuronID);
						g.writeObjectFieldStart(SOMKeywords.EVENTPORTS.get());
						writeEventPorts(g, sourceComp.getComponentType());
						g.writeEndObject();
						g.writeEndObject();
						
						g.writeObjectFieldStart("Target");
						g.writeStringField("Name",targetNeuronID);
						g.writeObjectFieldStart(SOMKeywords.EVENTPORTS.get());
						writeEventPorts(g, targetComp.getComponentType());
						g.writeEndObject();
						g.writeEndObject();
					
					g.writeEndObject();
				}
			}
			g.writeEndArray();
			g.writeNumberField("SynapseCount",count);
			
			

			g.close();
			
			String som = sw.toString();
			
			DLemsWriter.putIntoVelocityContext(som, context);
		
			Properties props = new Properties();
			props.put("resource.loader", "class");
			props.put("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
			VelocityEngine ve = new VelocityEngine();
			ve.init(props);
			Template template = ve.getTemplate(method.getFilename());
		   
			sw = new StringWriter();

			template.merge( context, sw );
			
			sb.append(sw);

			System.out.println("TestBenchData");
			System.out.println(sw.toString());
			System.out.println(som);
			
			
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
		

		
		return sb.toString();
	}


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
	
	private static String readFile(String path, Charset encoding) 
	  throws IOException 
	{
	  byte[] encoded = Files.readAllBytes(Paths.get(path));
	  return encoding.decode(ByteBuffer.wrap(encoded)).toString();
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

	
	


	

	private void writeNeuron(JsonGenerator g, Component neuron,int id) throws JsonGenerationException, IOException, ContentError
	{
			g.writeStartObject();
			g.writeStringField("name",neuron.getName() + "_" + id);
			writeSOMForComponent(g,neuron,true);
			g.writeEndObject();
	}

	private void writeNeuronDefaults(JsonGenerator g, Component neuron) throws JsonGenerationException, IOException, ContentError
	{
		ComponentType ct = neuron.getComponentType();

		for(FinalParam p: ct.getFinalParams())
		{
			ParamValue pv = neuron.getParamValue(p.getName());
			g.writeObjectFieldStart("param_" + pv.getDimensionName() + "_" + p.getName());
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
				g.writeObjectFieldStart("param_" +  pv.getDimensionName() + "_" + comp2.getName()  +"_" + p.getName());
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
							g.writeObjectFieldStart("param_" + pv.getDimensionName()+ "_" + comp2.getID() + "_" + p.getName());
							g.writeStringField("type",pv.getDimensionName()+"");
							g.writeStringField("value",(float)pv.getDoubleValue()+"");
							VHDLFixedPointDimensions.writeBitLengths(g,pv.getDimensionName());
							
							g.writeEndObject();
						}
					}
				}
			}
		}
	}
	

	private void writeNeuronComponent(JsonGenerator g, Component neuron) throws JsonGenerationException, IOException, ContentError
	{	
			//g.writeObjectFieldStart(neuron.getTypeName());

			String compRef =  neuron.getID();
			if (compRef.contains("_flat"))
			{
				compRef = compRef.replace("_flat", "");
			}
			g.writeObjectFieldStart(compRef);
			//g.writeStringField("name",neuron.getTypeName());
			writeSOMForComponent(g,neuron,true);
			g.writeEndObject();
	}
	
	private void writeDisplay(JsonGenerator g, Component display) throws JsonGenerationException, IOException, ContentError
	{
			g.writeObjectFieldStart(display.getName());
			g.writeStringField("name",display.getName());
			g.writeStringField("timeScale",display.getParamValue("timescale")+"");
			g.writeStringField("xmin",display.getParamValue("xmin")+"");
			g.writeStringField("xmax",display.getParamValue("xmax")+"");
			g.writeArrayFieldStart("Lines");
			List<Component> lines = display.getAllChildren();
			for (int i = 0; i < lines.size(); i++)
			{
				Component line = lines.get(0);
				g.writeStartObject();
				g.writeStringField("id",line.getID()+""); 
				g.writeStringField("quantity",line.getStringValue("quantity")+"");
				g.writeStringField("scale",line.getStringValue("scale")+"");
				g.writeStringField("timeScale",line.getStringValue("timeScale")+"");
				g.writeEndObject();
			}
			g.writeEndArray();
			g.writeEndObject();
	}
	
	private void writeDisplays(JsonGenerator g, List<Component> displays) throws JsonGenerationException, IOException, ContentError
	{
		for (int i = 0; i < displays.size(); i++)
		{
			Component display = displays.get(i);
			ComponentType comT = display.getComponentType();
			if (comT.name.matches("Display"))
			{
				//add display processes with time resolution and start and end as specified
				g.writeObjectFieldStart(SOMKeywords.DISPLAYS.get());
				writeDisplay(g, display);
				g.writeEndObject();
			}
		}
		
	}
	
	
	private void writeSOMForComponent(JsonGenerator g, Component comp, boolean writeChildren) throws JsonGenerationException, IOException, ContentError
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
		
		g.writeObjectFieldStart(SOMKeywords.DYNAMICS.get());
		if (dyn != null)
			VHDLDynamics.writeTimeDerivatives(g, ct, dyn.getTimeDerivatives(),parameters,parameterValues,"noregime_");
		g.writeEndObject();

		g.writeObjectFieldStart(SOMKeywords.DERIVEDPARAMETERS.get());
		VHDLParameters.writeDerivedParameters(g, ct, ct.getDerivedParameters(),parameters,parameterValues);
		g.writeEndObject();

		g.writeObjectFieldStart(SOMKeywords.DERIVEDVARIABLES.get());
		if (dyn != null)
			VHDLDynamics.writeDerivedVariables(g, ct, dyn.getDerivedVariables(),comp,parameters,parameterValues,lems);
		g.writeEndObject();

		g.writeObjectFieldStart(SOMKeywords.CONDITIONALDERIVEDVARIABLES.get());
		if (dyn != null)
		writeConditionalDerivedVariables(g, ct, dyn.getConditionalDerivedVariables(),comp,parameters,parameterValues);
		g.writeEndObject();
		
		g.writeArrayFieldStart(SOMKeywords.CONDITIONS.get());
		if (dyn != null)
			VHDLDynamics.writeConditions(g, ct,dyn.getOnConditions(),parameters,parameterValues);
		g.writeEndArray();

		g.writeArrayFieldStart(SOMKeywords.EVENTS.get());
		if (dyn != null)
			VHDLDynamics.writeEvents(g, ct, dyn.getOnEvents(),parameters,parameterValues);
		g.writeEndArray();

		String compRef =  comp.getID();
		if (compRef.contains("_flat"))
		{
			compRef = compRef.replace("_flat", "");
		}
		g.writeStringField(SOMKeywords.NAME.get(),compRef);
		
		g.writeObjectFieldStart(SOMKeywords.REQUIREMENTS.get());
		writeRequirements(g, comp.getComponentType().getRequirements());
		g.writeEndObject();

		g.writeObjectFieldStart(SOMKeywords.EXPOSURES.get());
		writeExposures(g, comp.getComponentType());
		g.writeEndObject();
		
		g.writeObjectFieldStart(SOMKeywords.EVENTPORTS.get());
		writeEventPorts(g, comp.getComponentType());
		g.writeEndObject();
		
		g.writeObjectFieldStart(SOMKeywords.STATE.get());
		VHDLDynamics.writeState(g, comp,parameters,parameterValues);
		g.writeEndObject();

		g.writeObjectFieldStart(SOMKeywords.STATE_FUNCTIONS.get());
		VHDLDynamics.writeStateFunctions(g, comp);
		g.writeEndObject();
		
		if (writeChildren)
		{
			g.writeObjectFieldStart("Children");
			
			for (int i = 0; i < comp.getAllChildren().size();i++)
			{
				Component comp2 = comp.getAllChildren().get(i);
				writeNeuronComponent(g, comp2);
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
							writeNeuronComponent(g, comp2);
							numberID++;
						}
					}
				}
			}
			//end
			g.writeEndObject();
		}
		
		g.writeObjectFieldStart(SOMKeywords.LINKS.get());
		writeLinks(g, comp);
		g.writeEndObject();

		g.writeObjectFieldStart(SOMKeywords.REGIMES.get());
		VHDLDynamics.writeRegimes(g, comp,writeChildren, parameters, parameterValues,lems );
		g.writeEndObject();


		g.writeObjectFieldStart(SOMKeywords.PARAMETERS.get());
		VHDLParameters.writeParameters(g, comp, parameters, parameterValues );
		g.writeEndObject();
		
	}
	


	

	
	

	private void writeLinks(JsonGenerator g, Component comp) throws ContentError, JsonGenerationException, IOException
	{
		ComponentType ct = comp.getComponentType();
		
		for (Link link: ct.getLinks())
		{
			g.writeObjectFieldStart(link.getName());
			ComponentType compType = lems.getComponentTypeByName(link.getTargetType());
			
			g.writeObjectFieldStart(SOMKeywords.EXPOSURES.get());
			writeExposures(g, compType);
			g.writeEndObject();
			
			g.writeObjectFieldStart(SOMKeywords.EVENTPORTS.get());
			writeEventPorts(g, compType);
			g.writeEndObject();

			g.writeEndObject();
		}
	}

	
	
	
	private void writeRequirements(JsonGenerator g, LemsCollection<Requirement> requirements) throws ContentError, JsonGenerationException, IOException
	{
		for (Requirement req : requirements)
		{
			g.writeObjectFieldStart(req.getName());
			g.writeStringField("name",req.getName());
			g.writeStringField("type",req.getDimension().getName()+"");
			VHDLFixedPointDimensions.writeBitLengths(g,req.getDimension().getName());
			g.writeEndObject();
		}
	}
	
	private void writeExposures(JsonGenerator g, ComponentType ct) throws ContentError, JsonGenerationException, IOException
	{
		for(FinalExposed e: ct.getFinalExposures())
		{
			g.writeObjectFieldStart(e.getName());
			g.writeStringField("name",e.getName());
			g.writeStringField("type",e.getDimension().getName()+"");
			VHDLFixedPointDimensions.writeBitLengths(g,e.getDimension().getName());
			
			g.writeEndObject();
		}
	}
	

	private void writeEventPorts(JsonGenerator g, ComponentType ct) throws ContentError, JsonGenerationException, IOException
	{
		for(EventPort e: ct.getEventPorts())
		{
			g.writeObjectFieldStart(e.getName());
			g.writeStringField("name",e.getName());
			g.writeStringField("direction",e.direction+"");
			g.writeEndObject();
		}
	}
	

	

	
	


	

	private void writeConditionalDerivedVariables(JsonGenerator g, ComponentType ct, 
			LemsCollection<ConditionalDerivedVariable> conditionalDerivedVariables, Component comp
			, LemsCollection<FinalParam> params,LemsCollection<ParamValue> combinedParameterValues) throws ContentError, JsonGenerationException, IOException
	{
		for (ConditionalDerivedVariable dv: conditionalDerivedVariables)
		{
			g.writeObjectFieldStart(dv.getName());
			g.writeStringField("name",dv.getName()); 
			g.writeStringField("exposure",dv.getExposure() != null ? dv.getExposure().getName() : "");
 
	        StringBuilder sensitivityList = new StringBuilder();

			g.writeObjectFieldStart(SOMKeywords.CASES.get());
			Integer i = 0;
			for (Case dv2: dv.cases)
			{
				g.writeObjectFieldStart(i.toString());
				String val = dv2.getValueExpression();
		
				if (val != null) {
					String value = VHDLEquations.encodeVariablesStyle(dv2.getValueExpression(),
							ct.getFinalParams(),ct.getDynamics().getStateVariables(),ct.getDynamics().getDerivedVariables(),
							ct.getRequirements(),ct.getPropertys(),sensitivityList,params,combinedParameterValues);

					value = VHDLEquations.writeInternalExpLnLogEvaluators(value,g,dv.getName(),sensitivityList,"");
					g.writeStringField("value", value );
					VHDLDynamics.writeConditionList(g,ct,dv2.condition,sensitivityList,params,combinedParameterValues);
				} 
				i++;
				g.writeEndObject();
					
			}
			g.writeEndObject();

			g.writeStringField("type",dv.getDimension().getName()+"");
			g.writeStringField("sensitivityList",sensitivityList.length() == 0 ? "" : sensitivityList.substring(0,sensitivityList.length()-1));
			VHDLFixedPointDimensions.writeBitLengths(g,dv.getDimension().getName());
					
			g.writeEndObject();
		}

	}
	

	@Override
	protected void setSupportedFeatures() {
		// TODO Auto-generated method stub
		
	}
}


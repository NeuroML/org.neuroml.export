package org.lemsml.export.vhdl;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;


import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.lemsml.export.som.SOMWriter;
import org.lemsml.jlems.core.expression.ParseError;
import org.lemsml.jlems.core.flatten.ComponentFlattener;
import org.lemsml.jlems.core.logging.E;
import org.lemsml.jlems.core.logging.MinimalMessageHandler;
import org.lemsml.jlems.core.run.ConnectionError;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.type.Lems;
import org.neuroml.export.base.BaseWriter;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.TypeReference;
import org.lemsml.jlems.core.type.Component;
import org.lemsml.jlems.core.type.ComponentType;
import org.lemsml.jlems.core.type.Constant;
import org.lemsml.jlems.core.type.EventPort;
import org.lemsml.jlems.core.type.Exposure;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.core.type.LemsCollection;
import org.lemsml.jlems.core.type.Link;
import org.lemsml.jlems.core.type.ParamValue;
import org.lemsml.jlems.core.type.Parameter;
import org.lemsml.jlems.core.type.Target;
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
import org.lemsml.jlems.io.xmlio.XMLSerializer;
import org.lemsml.export.som.SOMKeywords;
import org.neuroml.export.Utils;
import org.neuroml.model.NetworkTypes;

public class VHDLWriter extends BaseWriter {
	
	public enum Method {
		ODE("vhdl/vhdl_ode.vm"), 
		EULER("vhdl/vhdl_euler.vm"),
		TESTBENCH("vhdl/vhdl_tb.vm"),
		COMPONENT("vhdl/vhdl_comp.vm");
		
	 private String filename;
	 
	 private Method(String f) {
		 filename = f;
	 }
	 
	 public String getFilename() {
	   return filename;
	 }};
	
	private Method method = Method.COMPONENT;

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
	
	public void setMethod(Method method){
		this.method = method;
	}

	
	
	
	public String getMainScript() throws ContentError, ParseError {
	
		StringBuilder sb = new StringBuilder();

		sb.append("--" + this.format+" simulator compliant export for:--\n--\n");
		
		Velocity.init();
		
		VelocityContext context = new VelocityContext();

		//context.put( "name", new String("VelocityOnOSB") );
		try
		{
			SOMWriter somw = new SOMWriter(lems);
			JsonFactory f = new JsonFactory();
			StringWriter sw = new StringWriter();
			JsonGenerator g = f.createJsonGenerator(sw);
			g.useDefaultPrettyPrinter();
			g.writeStartObject();
			Target target = lems.getTarget();
			Component simCpt = target.getComponent();
			g.writeStringField(SOMKeywords.DT.get(), simCpt.getParamValue("step").stringValue());
			g.writeStringField(SOMKeywords.SIMLENGTH.get(), simCpt.getParamValue("length").stringValue());
			String targetId = simCpt.getStringValue("target");
			
			List<Component> simComponents = simCpt.getAllChildren();
			writeDisplays(g, simComponents);
			
			Component networkComp = lems.getComponent(targetId);
			List<Component> networkComponents = networkComp.getAllChildren();
			//TODO: order networkComponents by type so all populations come through in one go
			g.writeArrayFieldStart("NeuronComponents");
			List<String> neuronTypes = new ArrayList<String>();
			
			for (int i = 0; i < networkComponents.size();i++)
			{
				Component comp = networkComponents.get(i);
				ComponentType comT = comp.getComponentType();
				if (comT.name.matches("Population"))
				{
						//add a new neuron component
					Component neuron = lems.getComponent(comp.getStringValue("component"));
					if (!neuronTypes.contains(neuron.getName()))
					{
						writeNeuronComponent(g, neuron);
						neuronTypes.add(neuron.getName());
					}
				}
			}
			g.writeEndArray();
			g.writeArrayFieldStart("NeuronInstances");
			for (int i = 0; i < networkComponents.size();i++)
			{
				Component comp = networkComponents.get(i);
				ComponentType comT = comp.getComponentType();
				if (comT.name.matches("Population"))
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
			
			

			g.close();
			
			String som = sw.toString();
			
			SOMWriter.putIntoVelocityContext(som, context);
		
			Properties props = new Properties();
			props.put("resource.loader", "class");
			props.put("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
			VelocityEngine ve = new VelocityEngine();
			ve.init(props);
			Template template = ve.getTemplate(method.TESTBENCH.getFilename());
		   
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
	
	
	public Map<String,String> getComponentScripts() throws ContentError, ParseError {

		Map<String,String> componentScripts = new HashMap<String,String>();
		StringBuilder sb = new StringBuilder();

		//sb.append("--" + this.format+" simulator compliant export for:--\n--\n");
		
		Velocity.init();
		
		VelocityContext context = new VelocityContext();

		//context.put( "name", new String("VelocityOnOSB") );

        SOMWriter somw = new SOMWriter(lems);
		
		JsonFactory f = new JsonFactory();
		
		Target target = lems.getTarget();
		Component simCpt = target.getComponent();
		//String targetId = simCpt.getStringValue("target");
		//Component tgtComp = lems.getComponent(targetId);
		ArrayList<Component> temppops = new ArrayList<Component>();
		ArrayList<Component> pops = new ArrayList<Component>();
		temppops.addAll(lems.getComponents().getContents());//temppops.add(tgtComp);
		while (temppops.size() > 0)
		{
			pops.clear();
			pops.addAll(temppops);
			temppops.clear();
			for	(Component pop : pops)
			{
				temppops.addAll(pop.getStrictChildren());
				try
				{
					StringWriter sw = new StringWriter();
					JsonGenerator g = f.createJsonGenerator(sw);
					g.useDefaultPrettyPrinter();
					g.writeStartObject();
					pop.getComponentType().getLinks();
					String compRef = pop.getID();// pop.getStringValue("component");
					Component popComp = pop;//pop.getComponentType();//lems.getComponent(compRef);
					//addComment(sb, "   Population " + pop.getID() + " contains components of: " + popComp + " ");
					
					/*Component cpFlat = new Component();
					ComponentFlattener cf = new ComponentFlattener(lems, popComp);
					try
					{
						cpFlat = cf.getFlatComponent();
						lems.addComponent(cpFlat);
						String compOut = XMLSerializer.serialize(cpFlat);
						E.info("Flat component: \n" + compOut);
						lems.resolve(cpFlat);
					}
					catch(ConnectionError e)
					{
						throw new ParseError("Error when flattening component: " + popComp, e);
					}
					
					
					writeSOMForComponent(g, cpFlat);*/
					
					writeSOMForComponent(g, popComp,true);
					g.writeStringField(SOMKeywords.T_END.get(), simCpt.getParamValue("length").stringValue());
					g.writeStringField(SOMKeywords.T_START.get(), "0");
					g.writeStringField(SOMKeywords.COMMENT.get(), Utils.getHeaderComment(format));
					
					g.writeEndObject();

					g.close();

					System.out.println(sw.toString());
					
					String som = sw.toString();
				
					SOMWriter.putIntoVelocityContext(som, context);
				
					Properties props = new Properties();
					props.put("resource.loader", "class");
					props.put("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
					VelocityEngine ve = new VelocityEngine();
					ve.init(props);
					Template template = ve.getTemplate(method.getFilename());
				   
					sw = new StringWriter();

					template.merge( context, sw );
					
					sb.append(sw);
					
					componentScripts.put(compRef,sb.toString().replaceAll("(?m)^[ \t]*\r?\n", "").replace("\r\n\r\n", "\r\n").replace("\r\n\r\n", "\r\n").replace("\n\n", "\n").replace("\n\n", "\n"));
					sb = new StringBuilder();
					//System.out.println(compRef);
					System.out.println(sb.toString());
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
			}
		}
	
		
		return componentScripts;

	}


	private void writeNeuron(JsonGenerator g, Component neuron,int id) throws JsonGenerationException, IOException, ContentError
	{
			g.writeStartObject();
			g.writeStringField("name",neuron.getName() + "_" + id);
			writeSOMForComponent(g,neuron,false);
			g.writeEndObject();
	}

	private void writeNeuronComponent(JsonGenerator g, Component neuron) throws JsonGenerationException, IOException, ContentError
	{
			g.writeStartObject();
			g.writeStringField("name",neuron.getTypeName());
			writeSOMForComponent(g,neuron,false);
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
		Dynamics dyn = ct.getDynamics();
		g.writeObjectFieldStart(SOMKeywords.DYNAMICS.get());
		if (dyn != null)
		writeTimeDerivatives(g, ct, dyn.getTimeDerivatives());
		g.writeEndObject();

		g.writeArrayFieldStart(SOMKeywords.CONDITIONS.get());
		if (dyn != null)
		writeConditions(g, ct,dyn.getOnConditions());
		g.writeEndArray();

		g.writeArrayFieldStart(SOMKeywords.EVENTS.get());
		if (dyn != null)
		writeEvents(g, ct, dyn.getOnEvents());
		g.writeEndArray();

		g.writeStringField(SOMKeywords.NAME.get(), comp.getID());

		g.writeObjectFieldStart(SOMKeywords.PARAMETERS.get());
		writeParameters(g, comp);
		g.writeEndObject();

		g.writeObjectFieldStart(SOMKeywords.EXPOSURES.get());
		writeExposures(g, comp.getComponentType());
		g.writeEndObject();
		
		g.writeObjectFieldStart(SOMKeywords.EVENTPORTS.get());
		writeEventPorts(g, comp.getComponentType());
		g.writeEndObject();
		
		g.writeObjectFieldStart(SOMKeywords.STATE.get());
		writeState(g, comp);
		g.writeEndObject();

		g.writeObjectFieldStart(SOMKeywords.STATE_FUNCTIONS.get());
		writeStateFunctions(g, comp);
		g.writeEndObject();
		
		if (writeChildren)
		{
			g.writeArrayFieldStart("Children");
			
			for (int i = 0; i < comp.getStrictChildren().size();i++)
			{
				Component comp2 = comp.getStrictChildren().get(i);
				writeNeuronComponent(g, comp2);
			}
			g.writeEndArray();
		}
		
		g.writeObjectFieldStart(SOMKeywords.LINKS.get());
		writeLinks(g, comp);
		g.writeEndObject();

		g.writeObjectFieldStart(SOMKeywords.REGIMES.get());
		writeRegimes(g, comp,writeChildren);
		g.writeEndObject();
		
	}
	
	private void writeRegimes(JsonGenerator g, Component comp, boolean writeChildren) throws JsonGenerationException, IOException, ContentError
	{
		ComponentType ct = comp.getComponentType();
		ct.getAbout();
		Dynamics dyn = ct.getDynamics();
		if (dyn != null)
		{
			LemsCollection<Regime> regimes = dyn.getRegimes();
			if (regimes != null)
				for (Regime reg: regimes)
				{
					g.writeObjectFieldStart(reg.getName());
					g.writeStringField("name",reg.getName());
					if (reg.isInitial())
					g.writeStringField("default","default");
					
					g.writeObjectFieldStart(SOMKeywords.DYNAMICS.get());
					writeTimeDerivatives(g, ct, reg.getTimeDerivatives());
					g.writeEndObject();

					g.writeArrayFieldStart(SOMKeywords.CONDITIONS.get());
					writeConditions(g, ct,reg.getOnConditions());
					g.writeEndArray();

					g.writeArrayFieldStart(SOMKeywords.EVENTS.get());
					writeEvents(g, ct, reg.getOnEvents());
					g.writeEndArray();

					g.writeArrayFieldStart(SOMKeywords.ONENTRYS.get());
					writeEntrys(g, ct, reg.getOnEntrys());
					g.writeEndArray();
					
					
					g.writeEndObject();
				}
		}
	}
	

	private void writeState(JsonGenerator g, Component comp) throws ContentError, JsonGenerationException, IOException
	{
		ComponentType ct = comp.getComponentType();
		Dynamics dyn = ct.getDynamics();
		if (dyn != null)
		for (StateVariable sv: dyn.getStateVariables())
		{
			String init = "0";
			for (OnStart os: ct.getDynamics().getOnStarts())
			{
				for (StateAssignment sa: os.getStateAssignments())
				{
					if (sa.getVariable().equals(sv.getName()))
					{
						init = encodeVariablesStyle(sa.getValueExpression(),ct.getParameters(),ct.getDynamics().getStateVariables());
					}
				}
			}
			g.writeObjectFieldStart(sv.getName());
			g.writeStringField("name",sv.getName());
			g.writeStringField("type",sv.getDimension().getName()+"");
			if (sv.hasExposure())
				g.writeStringField("exposure",sv.getExposure().getName()+"");

			g.writeStringField("onstart",init+"");
			writeBitLengths(g,sv.getDimension().getName());
			
			g.writeEndObject();
			
		}
	}
	
	private String encodeVariablesStyle(String toEncode, LemsCollection<Parameter> params, 
			LemsCollection<StateVariable> stateVariables )
	{
		
		String returnString = toEncode;
		String[] items = toEncode.split("[ ()*/+-]");
		List<String> list = new ArrayList<String>();
		for (int i = 0; i < items.length; i ++)
		{
			list.add(items[i]);
		}
		
		MyComparator comparator = new MyComparator("abc");
		HashSet<String> hs = new HashSet<String>();
		hs.addAll(list);
		list.clear();
		list.addAll(hs);
		java.util.Collections.sort(list, comparator );
		returnString = " " + returnString.replaceAll("\\*"," \\* ").replaceAll("/"," / ").replaceAll("\\("," \\( ").replaceAll("\\)"," \\) ").replaceAll("-"," - ").replaceAll("\\+"," \\+ ") + " ";
		for (int i = list.size() -1; i >= 0 ; i --)
		{
			String toReplace = list.get(i);
			try {
				if (params.hasName(toReplace))
				{
					returnString = returnString.replaceAll(" " + toReplace + " "," param_" + params.getByName(toReplace).dimension + "_" + toReplace + " ");
				}
				else
				if (stateVariables.hasName(toReplace))
				{
					returnString = returnString.replaceAll(" " + toReplace + " "," statevariable_" + stateVariables.getByName(toReplace).dimension + "_" + toReplace + " ");
				}
				else
					if (toReplace.equals("t"))
					{
						returnString = returnString.replaceAll(" t ","sysparam_time_simtime");
					}
			} catch (ContentError e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
		
		return returnString;
	}
	

	private void writeStateFunctions(JsonGenerator g, Component comp) throws ContentError, JsonGenerationException, IOException
	{
		ComponentType ct = comp.getComponentType();
		Dynamics dyn = ct.getDynamics();
		if (dyn != null)
		for (DerivedVariable dv: dyn.getDerivedVariables())
		{
			if (dv.value == null || dv.value.length()==0)
			{
				g.writeStringField(dv.getName(), "0");
			}
			else
			{
				g.writeStringField(dv.getName(), dv.value);
			}
		}
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

	private void writeParameters(JsonGenerator g, Component comp) throws ContentError, JsonGenerationException, IOException
	{
		ComponentType ct = comp.getComponentType();

		for(Parameter p: ct.getDimParams())
		{
			ParamValue pv = comp.getParamValue(p.getName());
			g.writeObjectFieldStart(p.getName());
			g.writeStringField("name",p.getName());
			g.writeStringField("type",pv.getDimensionName()+"");
			g.writeStringField("value",(float)pv.getDoubleValue()+"");
			writeBitLengths(g,pv.getDimensionName());
			
			g.writeEndObject();
		}
		
		//TODO: put this out of Parameters and into constants list
		for(Constant c: ct.getConstants())
		{
			g.writeStringField(c.getName(), c.getValue()+"");
			g.writeObjectFieldStart(SOMKeywords.CONSTANT.get());
			g.writeStringField("name",c.getName());
			g.writeStringField("type",c.getDimension()+"");
			g.writeStringField("value",(float)c.getValue()+"");
			g.writeEndObject();
		}

	}

	private void writeExposures(JsonGenerator g, ComponentType ct) throws ContentError, JsonGenerationException, IOException
	{
		for(Exposure e: ct.getExposures())
		{
			g.writeObjectFieldStart(e.getName());
			g.writeStringField("name",e.getName());
			g.writeStringField("type",e.getDimension().getName()+"");
			writeBitLengths(g,e.getDimension().getName());
			
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
	
	private void writeBitLengths(JsonGenerator g, String dimension) throws JsonGenerationException, IOException
	{
		if (dimension == null || dimension.equals("none"))
		{
			g.writeStringField("integer","15");
			g.writeStringField("fraction","0");
		} else if (dimension.equals("voltage"))
		{
			g.writeStringField("integer","2");
			g.writeStringField("fraction","-18");
		} else if (dimension.equals("time"))
		{
			g.writeStringField("integer","4");
			g.writeStringField("fraction","-24");
		} else if (dimension.equals("capacitance"))
		{
			g.writeStringField("integer","-31");
			g.writeStringField("fraction","-47");
		} else if (dimension.equals("conductance"))
		{
			g.writeStringField("integer","-29");
			g.writeStringField("fraction","-45");
		} else if (dimension.equals("per_time"))
		{
			g.writeStringField("integer","20");
			g.writeStringField("fraction","-2");
		}
	}

	private void writeConditions(JsonGenerator g,ComponentType ct, LemsCollection<OnCondition> onConditions) throws ContentError, JsonGenerationException, IOException
	{
		
		for (OnCondition oc: onConditions)
		{
			g.writeStartObject();

			g.writeStringField(SOMKeywords.NAME.get(), oc.test.replace(' ', '_').replace('.', '_'));
			g.writeStringField(SOMKeywords.CONDITION.get(), encodeVariablesStyle(inequalityToCondition(oc.test),ct.getParameters(),ct.getDynamics().getStateVariables()));
			g.writeStringField(SOMKeywords.DIRECTION.get(), cond2sign(oc.test));
			
			g.writeObjectFieldStart(SOMKeywords.EFFECT.get());

			g.writeObjectFieldStart(SOMKeywords.STATE.get());
			
			for (StateAssignment sa: oc.getStateAssignments())
			{
				g.writeStringField(sa.getVariable(), encodeVariablesStyle(sa.getValueExpression(),ct.getParameters(),ct.getDynamics().getStateVariables()));
			}

			g.writeEndObject();

			g.writeObjectFieldStart(SOMKeywords.EVENTS.get());
			
			for (EventOut eo: oc.getEventOuts())
			{
				g.writeStringField(eo.getPortName(),eo.getPortName());
			}

			g.writeEndObject();
			

			g.writeObjectFieldStart(SOMKeywords.TRANSITIONS.get());
			
			for (Transition tr: oc.getTransitions())
			{
				g.writeStringField(tr.getRegime(),tr.getRegime());
			}

			g.writeEndObject();
			
			
			g.writeEndObject();
			
			g.writeEndObject();
			
		}

	}

	private void writeEvents(JsonGenerator g, ComponentType ct, LemsCollection<OnEvent> onEvents) throws ContentError, JsonGenerationException, IOException
	{
		for (OnEvent oc: onEvents)
		{
			g.writeStartObject();

			g.writeStringField(SOMKeywords.NAME.get(), oc.port.replace(' ', '_').replace('.', '_'));
			
			g.writeObjectFieldStart(SOMKeywords.EFFECT.get());

			g.writeObjectFieldStart(SOMKeywords.STATE.get());
			
			for (StateAssignment sa: oc.getStateAssignments())
			{
				g.writeStringField(sa.getVariable(), encodeVariablesStyle(sa.getValueExpression(),ct.getParameters(),ct.getDynamics().getStateVariables()));
			}

			g.writeEndObject();
			

			g.writeObjectFieldStart(SOMKeywords.EVENTS.get());
			
			for (EventOut eo: oc.getEventOuts())
			{
				g.writeStringField(eo.getPortName(),eo.getPortName());
			}

			g.writeEndObject();

			g.writeObjectFieldStart(SOMKeywords.TRANSITIONS.get());
			
			for (Transition tr: oc.getTransitions())
			{
				g.writeStringField(tr.getRegime(),tr.getRegime());
			}

			g.writeEndObject();
			
			
			g.writeEndObject();
			
			g.writeEndObject();
			
		}
		//g.writeEndArray();

	}
	

	private void writeEntrys(JsonGenerator g, ComponentType ct, LemsCollection<OnEntry> onEntrys) throws ContentError, JsonGenerationException, IOException
	{
		for (OnEntry oe: onEntrys)
		{
			g.writeStartObject();

			g.writeObjectFieldStart(SOMKeywords.EFFECT.get());

			g.writeObjectFieldStart(SOMKeywords.STATE.get());
			
			for (StateAssignment sa: oe.getStateAssignments())
			{
				g.writeStringField(sa.getVariable(), encodeVariablesStyle(sa.getValueExpression(),ct.getParameters(),ct.getDynamics().getStateVariables()));
			}

			g.writeEndObject();
			

			g.writeObjectFieldStart(SOMKeywords.EVENTS.get());
			
			for (EventOut eo: oe.getEventOuts())
			{
				g.writeStringField(eo.getPortName(),eo.getPortName());
			}

			g.writeEndObject();

			g.writeObjectFieldStart(SOMKeywords.TRANSITIONS.get());
			
			for (Transition tr: oe.getTransitions())
			{
				g.writeStringField(tr.getRegime(),tr.getRegime());
			}

			g.writeEndObject();
			
			
			g.writeEndObject();
			
			g.writeEndObject();
			
		}
		//g.writeEndArray();

	}
	

	private String cond2sign(String cond) 
	{
	    String ret = "???";
	    if (cond.indexOf(".gt.")>0 )
	    	return "> 0";
	    if (cond.indexOf(".geq.")>0)
	    	return ">= 0";
	    if (cond.indexOf(".lt.")>0)
	    	return "< 0";
	    if (cond.indexOf(".leq.")>0)
	    	return "<= 0";
	    if (cond.indexOf(".eq.")>0)
	    	return "= 0";
	    return ret;
	}

	
	private String inequalityToCondition(String ineq)
	{
	    String[] s = ineq.split("(\\.)[gleqt]+(\\.)");
	    //E.info("Split: "+ineq+": len "+s.length+"; "+s[0]+", "+s[1]);
	    String expr =  s[0].trim() + " - (" + s[1].trim() + ")";
	    //sign = comp2sign(s.group(2))
	    return expr;
	}

	private void writeTimeDerivatives(JsonGenerator g, ComponentType ct, LemsCollection<TimeDerivative> timeDerivatives) throws ContentError, JsonGenerationException, IOException
	{
		for (TimeDerivative td: timeDerivatives)
		{
			g.writeStringField(td.getVariable(), 
					encodeVariablesStyle(td.getValueExpression(),ct.getParameters(),ct.getDynamics().getStateVariables()));
		}

	}

	
	public class MyComparator implements java.util.Comparator<String> {

	    private int referenceLength;

	    public MyComparator(String reference) {
	        super();
	        this.referenceLength = reference.length();
	    }

	    public int compare(String s1, String s2) {
	        int dist1 = Math.abs(s1.length() - referenceLength);
	        int dist2 = Math.abs(s2.length() - referenceLength);

	        return dist1 - dist2;
	    }
	}
}


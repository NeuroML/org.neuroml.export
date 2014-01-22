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
import org.lemsml.jlems.core.type.ParamValue;
import org.lemsml.jlems.core.type.Parameter;
import org.lemsml.jlems.core.type.Target;
import org.lemsml.jlems.core.type.dynamics.DerivedVariable;
import org.lemsml.jlems.core.type.dynamics.OnCondition;
import org.lemsml.jlems.core.type.dynamics.OnEvent;
import org.lemsml.jlems.core.type.dynamics.OnStart;
import org.lemsml.jlems.core.type.dynamics.StateAssignment;
import org.lemsml.jlems.core.type.dynamics.StateVariable;
import org.lemsml.jlems.core.type.dynamics.TimeDerivative;
import org.lemsml.jlems.io.xmlio.XMLSerializer;
import org.lemsml.export.som.SOMKeywords;
import org.neuroml.export.Utils;

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

	
	public String getJSonScript(SOMWriter somw) throws ContentError, ParseError, IOException
	{
		JsonFactory f = new JsonFactory();
		StringWriter sw = new StringWriter();
		JsonGenerator g = f.createJsonGenerator(sw);
		g.useDefaultPrettyPrinter();
		g.writeStartObject();
		Target target = lems.getTarget();
		Component simCpt = target.getComponent();

		g.writeStringField(SOMKeywords.DT.get(), simCpt.getParamValue("step").stringValue());

		E.info("simCpt: " + simCpt);

		String targetId = simCpt.getStringValue("target");

		Component tgtComp = lems.getComponent(targetId);

		ArrayList<Component> pops = tgtComp.getChildrenAL("populations");

		
		
		if (pops.size()>0) {
			///////////////////////////////////////for (Component pop : pops) {
			Component pop = pops.get(0);
				String compRef = pop.getStringValue("component");
				Component popComp = lems.getComponent(compRef);

				ComponentType ctFlat = null;
				Component cpFlat = null;
				ctFlat = somw.getFlattenedCompType(popComp);
				cpFlat = somw.getFlattenedComp(popComp);
				
				somw.writeSOMForComponent(g, cpFlat);
			/////////////////////////////////////////}
		}
		else {

			somw.writeSOMForComponent(g, tgtComp);
		}
		
		//
		// if(pops.size() > 0)
		// {
		// for(Component pop : pops)
		// {
		// String compRef = pop.getStringValue("component");
		// Component popComp = lems.getComponent(compRef);
		// addComment(sb, "   Population " + pop.getID() + " contains components of: " + popComp + " ");


		g.writeStringField(SOMKeywords.T_END.get(), simCpt.getParamValue("length").stringValue());
		g.writeStringField(SOMKeywords.T_START.get(), "0");
		g.writeStringField(SOMKeywords.COMMENT.get(), Utils.getHeaderComment(format));
		
		g.writeEndObject();

		g.close();

		System.out.println(sw.toString() + "\r\n\r\n\r\n");
		return sw.toString();



		//
		// System.out.println(sb);
		// return sb.toString();
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
		

		System.out.println("TestBenchData");
		System.out.println(sb.toString());
		
		return "";
	}
	
	
	public Map<String,String> getComponentScripts() throws ContentError, ParseError {

		Map<String,String> componentScripts = new HashMap<String,String>();
		StringBuilder sb = new StringBuilder();

		sb.append("--" + this.format+" simulator compliant export for:--\n--\n");
		
		Velocity.init();
		
		VelocityContext context = new VelocityContext();

		//context.put( "name", new String("VelocityOnOSB") );

        SOMWriter somw = new SOMWriter(lems);
		
		JsonFactory f = new JsonFactory();
		
		Target target = lems.getTarget();
		Component simCpt = target.getComponent();
		String targetId = simCpt.getStringValue("target");

		Component tgtComp = lems.getComponent(targetId);
		ArrayList<Component> pops = tgtComp.getChildrenAL("populations");
		if(pops.size() > 0)
		{
			for(Component pop : pops)
			{
				try
				{
					StringWriter sw = new StringWriter();
					JsonGenerator g = f.createJsonGenerator(sw);
					g.useDefaultPrettyPrinter();
					g.writeStartObject();
					String compRef = pop.getStringValue("component");
					Component popComp = lems.getComponent(compRef);
					addComment(sb, "   Population " + pop.getID() + " contains components of: " + popComp + " ");
					
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
					
					writeSOMForComponent(g, popComp);
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
					
					componentScripts.put(compRef,sb.toString());
					sb = new StringBuilder();
					System.out.println(compRef);
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
	
	
	private void writeSOMForComponent(JsonGenerator g, Component comp) throws JsonGenerationException, IOException, ContentError
	{
		ComponentType ct = comp.getComponentType();
		
		g.writeObjectFieldStart(SOMKeywords.DYNAMICS.get());
		writeDynamics(g, comp);
		g.writeEndObject();

		g.writeArrayFieldStart(SOMKeywords.CONDITIONS.get());
		writeConditions(g, comp);
		g.writeEndArray();

		g.writeArrayFieldStart(SOMKeywords.EVENTS.get());
		writeEvents(g, comp);
		g.writeEndArray();

		g.writeStringField(SOMKeywords.NAME.get(), comp.getID());

		g.writeObjectFieldStart(SOMKeywords.PARAMETERS.get());
		writeParameters(g, comp);
		g.writeEndObject();

		g.writeObjectFieldStart(SOMKeywords.EXPOSURES.get());
		writeExposures(g, comp);
		g.writeEndObject();
		
		g.writeObjectFieldStart(SOMKeywords.EVENTPORTS.get());
		writeEventPorts(g, comp);
		g.writeEndObject();
		
		g.writeObjectFieldStart(SOMKeywords.STATE.get());
		writeState(g, comp);
		g.writeEndObject();

		g.writeObjectFieldStart(SOMKeywords.STATE_FUNCTIONS.get());
		writeStateFunctions(g, comp);
		g.writeEndObject();
	}
	

	private void writeState(JsonGenerator g, Component comp) throws ContentError, JsonGenerationException, IOException
	{
		ComponentType ct = comp.getComponentType();
		
		for (StateVariable sv: ct.getDynamics().getStateVariables())
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
		
		for (DerivedVariable dv: ct.getDynamics().getDerivedVariables())
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

	private void writeExposures(JsonGenerator g, Component comp) throws ContentError, JsonGenerationException, IOException
	{
		ComponentType ct = comp.getComponentType();

		for(Exposure e: ct.getExposures())
		{
			ParamValue pv = comp.getParamValue(e.getName());
			g.writeObjectFieldStart(e.getName());
			g.writeStringField("name",e.getName());
			g.writeStringField("type",e.getDimension().getName()+"");
			writeBitLengths(g,e.getDimension().getName());
			
			g.writeEndObject();
		}
		

	}
	

	private void writeEventPorts(JsonGenerator g, Component comp) throws ContentError, JsonGenerationException, IOException
	{
		ComponentType ct = comp.getComponentType();

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
		if (dimension.equals("voltage"))
		{
			g.writeStringField("integer","2");
			g.writeStringField("fraction","-14");
		} else if (dimension.equals("time"))
		{
			g.writeStringField("integer","0");
			g.writeStringField("fraction","-22");
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

	private void writeConditions(JsonGenerator g, Component comp) throws ContentError, JsonGenerationException, IOException
	{
		ComponentType ct = comp.getComponentType();
		
		//E.info("---- getOnConditions: "+ct.getDynamics().getOnConditions()+"");
		
		//g.writeStartArray();
		
		for (OnCondition oc: ct.getDynamics().getOnConditions())
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
			g.writeEndObject();
			
			g.writeEndObject();
			
		}
		//g.writeEndArray();

	}

	private void writeEvents(JsonGenerator g, Component comp) throws ContentError, JsonGenerationException, IOException
	{
		ComponentType ct = comp.getComponentType();
		
		//E.info("---- getOnConditions: "+ct.getDynamics().getOnConditions()+"");
		
		//g.writeStartArray();
		
		for (OnEvent oc: ct.getDynamics().getOnEvents())
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

	private void writeDynamics(JsonGenerator g, Component comp) throws ContentError, JsonGenerationException, IOException
	{
		ComponentType ct = comp.getComponentType();
		
		for (TimeDerivative td: ct.getDynamics().getTimeDerivatives())
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


package org.lemsml.export.dlems;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.velocity.VelocityContext;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.TypeReference;
import org.lemsml.export.base.BaseWriter;
import org.lemsml.jlems.core.expression.ParseError;
import org.lemsml.jlems.core.flatten.ComponentFlattener;
import org.lemsml.jlems.core.logging.E;
import org.lemsml.jlems.core.run.ConnectionError;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.type.Component;
import org.lemsml.jlems.core.type.ComponentType;
import org.lemsml.jlems.core.type.Constant;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.core.type.LemsCollection;
import org.lemsml.jlems.core.type.ParamValue;
import org.lemsml.jlems.core.type.Parameter;
import org.lemsml.jlems.core.type.Target;
import org.lemsml.jlems.core.type.dynamics.DerivedVariable;
import org.lemsml.jlems.core.type.dynamics.Dynamics;
import org.lemsml.jlems.core.type.dynamics.OnCondition;
import org.lemsml.jlems.core.type.dynamics.OnStart;
import org.lemsml.jlems.core.type.dynamics.StateAssignment;
import org.lemsml.jlems.core.type.dynamics.StateVariable;
import org.lemsml.jlems.core.type.dynamics.TimeDerivative;
import org.lemsml.jlems.io.xmlio.XMLSerializer;
import org.neuroml.export.Utils;


public class DLemsWriter extends BaseWriter
{

	static String DEFAULT_POP = "OneComponentPop";

	public DLemsWriter(Lems lems)
	{
		super(lems, "dLEMS");
	}
	
	public static void putIntoVelocityContext(String dlems, VelocityContext context) throws JsonParseException, JsonMappingException, IOException 
	{
		ObjectMapper mapper = new ObjectMapper();

		//HashMap<String,Object> map = new HashMap<String,Object>();
		LinkedHashMap<String,Object> map = new LinkedHashMap<String,Object>();

		map = mapper.readValue(dlems, 
		    new TypeReference<LinkedHashMap<String,Object>>(){});
		
		for (String key: map.keySet())
		{
			Object val = map.get(key);
			//E.info("Putting: "+key+": "+val+", "+val.getClass());

			context.put(key, val);
			/*
			HashMap<String, Object> map2 = new HashMap<String, Object>();
			if (val instanceof LinkedHashMap) 
			{
				LinkedHashMap<String, Object> lhm = (LinkedHashMap<String, Object>)val;

				for (String key2: lhm.keySet())
				{
				    map2.put(key2, lhm.get(key2));
				}
				  
				//Map<String, Object> hm = (Map<String, Object>)lhm;
				context.put(key, map2);
			}
			else
			{
				context.put(key, val);
			}*/
		}

		//E.info("context: "+context.internalGet((String)context.internalGetKeys()[7]));
		
		
	}

	@Override
	public String getMainScript() throws ContentError, ParseError, IOException
	{
		JsonFactory f = new JsonFactory();
		StringWriter sw = new StringWriter();
		JsonGenerator g = f.createJsonGenerator(sw);
		g.useDefaultPrettyPrinter();
		g.writeStartObject();
		Target target = lems.getTarget();
		Component simCpt = target.getComponent();

		g.writeStringField(DLemsKeywords.DT.get(), simCpt.getParamValue("step").stringValue());

		//E.info("simCpt: " + simCpt);

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
				ctFlat = getFlattenedCompType(popComp);
				cpFlat = getFlattenedComp(popComp);
				
				writeDLemsForComponent(g, cpFlat);
			/////////////////////////////////////////}
		}
		else {

			writeDLemsForComponent(g, tgtComp);
		}
		
		//
		// if(pops.size() > 0)
		// {
		// for(Component pop : pops)
		// {
		// String compRef = pop.getStringValue("component");
		// Component popComp = lems.getComponent(compRef);
		// addComment(sb, "   Population " + pop.getID() + " contains components of: " + popComp + " ");


		
        writeSimulationInfo(g, simCpt);
		g.writeStringField(DLemsKeywords.COMMENT.get(), Utils.getHeaderComment(format));
		
		g.writeEndObject();

		g.close();

		return sw.toString();



		//
		// System.out.println(sb);
		// return sb.toString();
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
        
        
        
        for (Component dispComp : simCpt.getAllChildren()) {
            if (dispComp.getName().indexOf("OutputFile") >= 0) {
                g.writeStringField(DLemsKeywords.DUMP_TO_FILE.get(), dispComp.getStringValue("fileName"));
            }
        }
        
        g.writeArrayFieldStart(DLemsKeywords.DISPLAY.get());
        
        for (Component dispComp : simCpt.getAllChildren()) {
            if (dispComp.getName().indexOf("Display") >= 0) {
                
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
                
                for (Component lineComp : dispComp.getAllChildren()) {
                    if (lineComp.getName().indexOf("Line") >= 0) {
                        
                        g.writeStartObject();
                        g.writeStringField(DLemsKeywords.ABSCISSA.get(), "t");
                        String quantity = lineComp.getStringValue("quantity");
                      
                        g.writeStringField(DLemsKeywords.ORDINATE.get(), quantity.substring(quantity.indexOf("/")+1));
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
		
		for (StateVariable sv: ct.getDynamics().getStateVariables())
		{
			String init = "0";
			for (OnStart os: ct.getDynamics().getOnStarts())
			{
				for (StateAssignment sa: os.getStateAssignments())
				{
					if (sa.getVariable().equals(sv.getName()))
					{
						init = sa.getValueExpression();
					}
				}
			}
			g.writeStringField(sv.getName(), init);
		}
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

			g.writeStringField(p.getName(), (float)pv.getDoubleValue()+"");
		}
		
		for(Constant c: ct.getConstants())
		{
			g.writeStringField(c.getName(), c.getValue()+"");
		}

	}

	private void writeEvents(JsonGenerator g, Component comp) throws ContentError, JsonGenerationException, IOException
	{
		ComponentType ct = comp.getComponentType();
		
		//E.info("---- getOnConditions: "+ct.getDynamics().getOnConditions()+"");
		
		//g.writeStartArray();
		
		for (OnCondition oc: ct.getDynamics().getOnConditions())
		{
			g.writeStartObject();

			g.writeStringField(DLemsKeywords.NAME.get(), oc.test.replace(' ', '_').replace('.', '_'));
			g.writeStringField(DLemsKeywords.CONDITION.get(), inequalityToCondition(oc.test));
			g.writeStringField(DLemsKeywords.DIRECTION.get(), cond2sign(oc.test));
			
			g.writeObjectFieldStart(DLemsKeywords.EFFECT.get());

			g.writeObjectFieldStart(DLemsKeywords.STATE.get());
			
			for (StateAssignment sa: oc.getStateAssignments())
			{
				g.writeStringField(sa.getVariable(), sa.getValueExpression());
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
	    if (cond.indexOf(".gt.")>0 || cond.indexOf(".geq.")>0)
	    	return "+";
	    if (cond.indexOf(".lt.")>0 || cond.indexOf(".leq.")>0)
	    	return "-";
	    if (cond.indexOf(".eq.")>0)
	    	return "0";
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
			g.writeStringField(td.getVariable(), td.getValueExpression());
		}

	}


    /*
	private List<String> getConstantNameList(ComponentType compt)
	{
		List<String> cNames = new ArrayList<String>();
		for(Constant c : compt.getConstants())
		{
			cNames.add(c.getName());
		}
		return cNames;
	}

	private List<String> getConstantValueList(ComponentType compt)
	{
		List<String> cVals = new ArrayList<String>();
		for(Constant c : compt.getConstants())
		{
			cVals.add(Double.toString(c.getValue()));
		}
		return cVals;
	}

	private List<String> getStateVariableList(ComponentType compt) throws ContentError
	{
		List<String> svs = new ArrayList<String>();
		for(TimeDerivative td : compt.getDynamics().getTimeDerivatives())
		{
			svs.add(td.getStateVariable().name);
		}
		return svs;
	}

	private List<String> getDynamics(ComponentType compt) throws ContentError
	{
		List<String> dyn = new ArrayList<String>();
		for(TimeDerivative td : compt.getDynamics().getTimeDerivatives())
		{
			dyn.add(td.getValueExpression());
		}
		return dyn;
	}

	private List<String> getDerivedVariableNameList(ComponentType compt) throws ContentError
	{
		List<String> derV = new ArrayList<String>();
		for(DerivedVariable d : compt.getDynamics().getDerivedVariables())
		{
			derV.add(d.getName());
		}
		return derV;
	}

	private List<String> getDerivedVariableExprList(ComponentType compt) throws ContentError
	{
		List<String> derV = new ArrayList<String>();
		for(DerivedVariable d : compt.getDynamics().getDerivedVariables())
		{
			derV.add(d.getValueExpression());
		}
		return derV;
	}

	private List<String> getParameterNameList(ComponentType compt, Component comp)
	{
		List<String> parNames = new ArrayList<String>();
		for(Parameter p : compt.getDimParams())
		{
			parNames.add(p.getName());
		}
		return parNames;
	}

	private List<String> getParameterValueList(ComponentType compt, Component comp) throws ContentError
	{
		ArrayList<String> parVals = new ArrayList<String>();
		for(Parameter p : compt.getDimParams())
		{
			ParamValue pv = comp.getParamValue(p.getName());
			parVals.add(String.valueOf(pv.getDoubleValue()));
		}
		return parVals;
	}

	private List<String> getInitialConditions(ComponentType popCompType) throws ContentError
	{

		ArrayList<String> initVals = new ArrayList<String>();

		Dynamics dyn = popCompType.getDynamics();
		LemsCollection<OnStart> initBlocks = dyn.getOnStarts();

		for(OnStart os : initBlocks)
		{
			LemsCollection<StateAssignment> assigs = os.getStateAssignments();

			for(StateAssignment va : assigs)
			{
				String initVal = va.getValueExpression();
				initVals.add(initVal);
			}

		}
		return initVals;
	}*/



	private ComponentType getFlattenedCompType(Component compOrig) throws ContentError, ParseError
	{

		ComponentType ctFlat = new ComponentType();
		ComponentFlattener cf = new ComponentFlattener(lems, compOrig);

		try
		{
			ctFlat = cf.getFlatType();
			lems.addComponentType(ctFlat);
			String typeOut = XMLSerializer.serialize(ctFlat);
			//E.info("Flat type: \n" + typeOut);
			lems.resolve(ctFlat);
		}
		catch(ConnectionError e)
		{
			throw new ParseError("Error when flattening component: " + compOrig, e);
		}
		return ctFlat;
	}

	private Component getFlattenedComp(Component compOrig) throws ContentError, ParseError
	{

		Component comp = new Component();
		ComponentFlattener cf = new ComponentFlattener(lems, compOrig);

		try
		{
			comp = cf.getFlatComponent();
			lems.addComponent(comp);
			String compOut = XMLSerializer.serialize(comp);
			//E.info("Flat component: \n" + compOut);
			lems.resolve(comp);
		}
		catch(ConnectionError e)
		{
			throw new ParseError("Error when flattening component: " + compOrig, e);
		}
		return comp;
	}

}

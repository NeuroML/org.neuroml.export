package org.lemsml.export.vhdl;

import java.io.IOException;
import java.util.ArrayList;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.lemsml.export.vhdl.VHDLWriter.SOMKeywords;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.type.Attachments;
import org.lemsml.jlems.core.type.Component;
import org.lemsml.jlems.core.type.ComponentType;
import org.lemsml.jlems.core.type.FinalParam;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.core.type.LemsCollection;
import org.lemsml.jlems.core.type.ParamValue;
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

public class VHDLDynamics {

	public static void writeEntrys(JsonGenerator g, ComponentType ct, LemsCollection<OnEntry> onEntrys
			, LemsCollection<FinalParam> params,LemsCollection<ParamValue> combinedParameterValues) throws ContentError, JsonGenerationException, IOException
	{
		for (OnEntry oe: onEntrys)
		{
			g.writeStartObject();
			StringBuilder sensitivityList = new StringBuilder();

			g.writeObjectFieldStart(SOMKeywords.EFFECT.get());

			g.writeObjectFieldStart(SOMKeywords.STATE.get());
			
			for (StateAssignment sa: oe.getStateAssignments())
			{
				g.writeStringField(sa.getVariable(), VHDLEquations.encodeVariablesStyle(sa.getValueExpression(),ct.getFinalParams(),
						ct.getDynamics().getStateVariables(),ct.getDynamics().getDerivedVariables(),
						ct.getRequirements(),sensitivityList,params,combinedParameterValues));
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
	



	public static void writeTimeDerivatives(JsonGenerator g, ComponentType ct, LemsCollection<TimeDerivative> timeDerivatives
			, LemsCollection<FinalParam> params,LemsCollection<ParamValue> combinedParameterValues)  throws ContentError, JsonGenerationException, IOException
	{
		for (TimeDerivative td: timeDerivatives)
		{
			StringBuilder sensitivityList = new StringBuilder();
			g.writeObjectFieldStart(td.getVariable());
			String value = VHDLEquations.encodeVariablesStyle(td.getValueExpression(),ct.getFinalParams(),
					ct.getDynamics().getStateVariables(),ct.getDynamics().getDerivedVariables(),
					ct.getRequirements(),sensitivityList,params,combinedParameterValues);
			value = VHDLEquations.writeInternalExpLnLogEvaluators(value,g,td.getVariable(),sensitivityList);
			g.writeStringField("Dynamics", value);
			if  (sensitivityList.length() > 0)
				g.writeStringField("SensitivityList",sensitivityList.replace(sensitivityList.length()-1, sensitivityList.length(), " ").toString());
			else
				g.writeStringField("SensitivityList"," ");
			g.writeEndObject();
		}

	}
	
	
	public static void writeConditionList(JsonGenerator g, ComponentType ct, String ineq, StringBuilder sensitivityList, LemsCollection<FinalParam> params,LemsCollection<ParamValue> combinedParameterValues) throws ContentError, JsonGenerationException, IOException
	{
	    String[] conditions = ineq.split("(\\.)[andor]+(\\.)");
	    String completeExpr ="";
	    int i = 0;
	    for (i = 0; i < conditions.length; i++)
	    {
	    	completeExpr = completeExpr + VHDLEquations.encodeVariablesStyle(VHDLEquations.inequalityToCondition(conditions[i]),ct.getFinalParams(),
	    			ct.getDynamics().getStateVariables(),ct.getDynamics().getDerivedVariables(),
	    			ct.getRequirements(),sensitivityList,params,combinedParameterValues)+ " " + VHDLEquations.cond2sign(conditions[i]);
	    	if ( i < conditions.length - 1 )
	    	{
	    		String testForAndOR = ineq.substring(ineq.indexOf(conditions[i]) + conditions[i].length(), ineq.indexOf(conditions[i]) + conditions[i].length()+6).toLowerCase();
	    		if (testForAndOR.contains("and"))
	    			completeExpr = completeExpr + " AND ";
	    		else
		    		completeExpr = completeExpr + " OR ";
	    			
	    	}
	    }

    	g.writeStringField(SOMKeywords.CONDITION.get(), completeExpr);

	}
	
	public static void writeRegimes(JsonGenerator g, Component comp, boolean writeChildren
			, LemsCollection<FinalParam> params,LemsCollection<ParamValue> combinedParameterValues, Lems lems) throws JsonGenerationException, IOException, ContentError
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
					writeTimeDerivatives(g, ct, reg.getTimeDerivatives(),params,combinedParameterValues);
					g.writeEndObject();

					g.writeObjectFieldStart(SOMKeywords.DERIVEDPARAMETERS.get());
					VHDLParameters.writeDerivedParameters(g, ct, ct.getDerivedParameters(),params,combinedParameterValues);
					g.writeEndObject();

					g.writeArrayFieldStart(SOMKeywords.CONDITIONS.get());
					writeConditions(g, ct,reg.getOnConditions(),params,combinedParameterValues);
					g.writeEndArray();

					g.writeArrayFieldStart(SOMKeywords.EVENTS.get());
					writeEvents(g, ct, reg.getOnEvents(),params,combinedParameterValues);
					g.writeEndArray();

					g.writeArrayFieldStart(SOMKeywords.ONENTRYS.get());
					writeEntrys(g, ct, reg.getOnEntrys(),params,combinedParameterValues);
					g.writeEndArray();
					
					
					g.writeEndObject();
				}
		}
	}

	public static void writeStateFunctions(JsonGenerator g, Component comp) throws ContentError, JsonGenerationException, IOException
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

	public static void writeState(JsonGenerator g, Component comp
			, LemsCollection<FinalParam> params,LemsCollection<ParamValue> combinedParameterValues)  throws ContentError, JsonGenerationException, IOException
	{
		ComponentType ct = comp.getComponentType();
		Dynamics dyn = ct.getDynamics();
		StringBuilder sensitivityList = new StringBuilder();
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
						init = VHDLEquations.encodeVariablesStyle(sa.getValueExpression(),ct.getFinalParams(),
								ct.getDynamics().getStateVariables(),ct.getDynamics().getDerivedVariables(),
								ct.getRequirements(), sensitivityList,params,combinedParameterValues);
					}
				}
			}
			g.writeObjectFieldStart(sv.getName());
			g.writeStringField("name",sv.getName());
			g.writeStringField("type",sv.getDimension().getName()+"");
			if (sv.hasExposure())
				g.writeStringField("exposure",sv.getExposure().getName()+"");

			g.writeStringField("onstart",init+"");
			VHDLFixedPointDimensions.writeBitLengths(g,sv.getDimension().getName());
			
			g.writeEndObject();
			
		}
	}
	public static void writeEvents(JsonGenerator g, ComponentType ct, LemsCollection<OnEvent> onEvents
			, LemsCollection<FinalParam> params,LemsCollection<ParamValue> combinedParameterValues) throws ContentError, JsonGenerationException, IOException
	{
		for (OnEvent oc: onEvents)
		{
			g.writeStartObject();
			StringBuilder sensitivityList = new StringBuilder();

			g.writeStringField(SOMKeywords.NAME.get(), oc.port.replace(' ', '_').replace('.', '_'));
			
			g.writeObjectFieldStart(SOMKeywords.EFFECT.get());

			g.writeObjectFieldStart(SOMKeywords.STATE.get());
			
			for (StateAssignment sa: oc.getStateAssignments())
			{
				g.writeStringField(sa.getVariable(), VHDLEquations.encodeVariablesStyle(sa.getValueExpression(),
						ct.getFinalParams(),ct.getDynamics().getStateVariables(),ct.getDynamics().getDerivedVariables(),
						ct.getRequirements(),sensitivityList,params,combinedParameterValues));
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
	

	public static void writeDerivedVariables(JsonGenerator g, ComponentType ct, 
			LemsCollection<DerivedVariable> derivedVariables, Component comp
			, LemsCollection<FinalParam> params,LemsCollection<ParamValue> combinedParameterValues, Lems lems)  throws ContentError, JsonGenerationException, IOException
	{
		for (DerivedVariable dv: derivedVariables)
		{
			g.writeObjectFieldStart(dv.getName());
			g.writeStringField("name",dv.getName());
			g.writeStringField("exposure",dv.getExposure() != null ? dv.getExposure().getName() : "");
						
			String val = dv.getValueExpression();
			String sel = dv.getSelect();

	        StringBuilder sensitivityList = new StringBuilder();
			if (val != null) {
				String value = VHDLEquations.encodeVariablesStyle(dv.getValueExpression(),
						ct.getFinalParams(),ct.getDynamics().getStateVariables(),ct.getDynamics().getDerivedVariables(),
						ct.getRequirements(),sensitivityList,params,combinedParameterValues) ;
				value = VHDLEquations.writeInternalExpLnLogEvaluators(value,g,dv.getName(),sensitivityList);
				g.writeStringField("value",value);
				
			} else if (sel != null) {
				String red = dv.getReduce();
				String selval = sel;
				if (red != null) {
					String op = " ? ";
					String dflt = "";
					if (red.equals("add")) {
						op = " + ";
						dflt = "0";
					} else if (red.equals("multiply")) {
						op = " * ";
						dflt = "1";
					} else {
						throw new ContentError("Unrecognized reduce: " + red);
					}
				
					String rt;
					String var;
					if (sel.indexOf("[*]")>0) {
						int iwc = sel.indexOf("[*]");
						rt = sel.substring(0, iwc);
						var = sel.substring(iwc + 4, sel.length());
					} else {
						int iwc = sel.lastIndexOf("/");
						rt = sel.substring(0, iwc);
						var = sel.substring(iwc + 2, sel.length());
					} 
						
					
					ArrayList<String> items = new ArrayList<String>();
					items.add(dflt);
					for (Component c : comp.getChildrenAL(rt)) {
						items.add("exposure_" + dv.getDimension().getName() + "_" + c.getID() + "_" + var + "_internal");
						sensitivityList.append("exposure_" + dv.getDimension().getName() + "_" + c.getID() + "_" + var +  "_internal,");
					}
					LemsCollection<Attachments> attachs = comp.getComponentType().getAttachmentss();
					Attachments attach = attachs.getByName(rt);
					if (attach != null)
					{
						for(Component conn: lems.getComponent("net1").getAllChildren())
						{
							String attachName = attach.getName();
							if (conn.getComponentType().getName().matches("synapticConnection") )
							{
								String destination = conn.getTextParam("destination");
								String path = conn.getPathParameterPath("to");
								if (destination.matches(attachName) && path.startsWith(comp.getID()))
								{
									Component c = (conn.getRefComponents().get("synapse"));
									items.add("exposure_" + dv.getDimension().getName() + "_" + c.getID() + "_" + var+ "_internal");
									sensitivityList.append("exposure_" + dv.getDimension().getName() + "_" + c.getID() + "_" + var + "_internal,");
								
								}
							}
						}
					}
					
					
					
					selval = StringUtil.join(items, op);
					g.writeStringField("value",VHDLEquations.encodeVariablesStyle(selval,
							ct.getFinalParams(),ct.getDynamics().getStateVariables(),ct.getDynamics().getDerivedVariables(),
							ct.getRequirements(),sensitivityList,params,combinedParameterValues));
					
				}
				else
				{
					String rt;
					String var;
					int iwc = sel.lastIndexOf("/");
					rt = sel.substring(0, iwc);
					var = sel.substring(iwc + 1, sel.length());

					Component c  =  comp.getChild(rt);
					selval = "exposure_" + dv.getDimension().getName() + "_" + c.getID() + "_" + var + "_internal";
					sensitivityList.append("exposure_" + dv.getDimension().getName() + "_" + c.getID() + "_" + var + "_internal,");
					
					g.writeStringField("value",VHDLEquations.encodeVariablesStyle(selval,
							ct.getFinalParams(),ct.getDynamics().getStateVariables(),ct.getDynamics().getDerivedVariables(),
							ct.getRequirements(),sensitivityList,params,combinedParameterValues));
					
					
				}
					
			}

			g.writeStringField("type",dv.getDimension().getName()+"");
			g.writeStringField("sensitivityList",sensitivityList.length() == 0 ? "" : sensitivityList.substring(0,sensitivityList.length()-1));
			VHDLFixedPointDimensions.writeBitLengths(g,dv.getDimension().getName());
					
			g.writeEndObject();
		}

	}
	

	public static void writeConditions(JsonGenerator g,ComponentType ct, LemsCollection<OnCondition> onConditions
			, LemsCollection<FinalParam> params,LemsCollection<ParamValue> combinedParameterValues) throws ContentError, JsonGenerationException, IOException
	{
		
		for (OnCondition oc: onConditions)
		{
			g.writeStartObject();
			StringBuilder sensitivityList = new StringBuilder();

			g.writeStringField(SOMKeywords.NAME.get(), oc.test.replace(' ', '_').replace('.', '_'));
			
			writeConditionList(g, ct,oc.test,sensitivityList,params,combinedParameterValues);
			
			g.writeObjectFieldStart(SOMKeywords.EFFECT.get());

			g.writeObjectFieldStart(SOMKeywords.STATE.get());
			
			for (StateAssignment sa: oc.getStateAssignments())
			{
				g.writeStringField(sa.getVariable(), VHDLEquations.encodeVariablesStyle(sa.getValueExpression(),ct.getFinalParams(),
						ct.getDynamics().getStateVariables(),ct.getDynamics().getDerivedVariables(),
						ct.getRequirements(),sensitivityList,params,combinedParameterValues));
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
			g.writeStringField(SOMKeywords.SENSITIVITYLIST.toString(),sensitivityList.deleteCharAt(sensitivityList.length()-1).toString());
				
			g.writeEndObject();
			
		}

	}

	
}

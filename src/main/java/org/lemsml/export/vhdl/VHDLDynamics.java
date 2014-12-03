package org.lemsml.export.vhdl;

import java.io.IOException;
import java.util.ArrayList;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.lemsml.export.vhdl.VHDLWriter.SOMKeywords;
import org.lemsml.export.vhdl.edlems.EDCase;
import org.lemsml.export.vhdl.edlems.EDComponent;
import org.lemsml.export.vhdl.edlems.EDCondition;
import org.lemsml.export.vhdl.edlems.EDDerivedVariable;
import org.lemsml.export.vhdl.edlems.EDDynamic;
import org.lemsml.export.vhdl.edlems.EDEvent;
import org.lemsml.export.vhdl.edlems.EDEventOut;
import org.lemsml.export.vhdl.edlems.EDOnEntry;
import org.lemsml.export.vhdl.edlems.EDRegime;
import org.lemsml.export.vhdl.edlems.EDState;
import org.lemsml.export.vhdl.edlems.EDStateAssignment;
import org.lemsml.export.vhdl.edlems.EDStateFunction;
import org.lemsml.export.vhdl.edlems.EDTransition;
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

	public static ArrayList<EDOnEntry> writeEntrys(ComponentType ct, LemsCollection<OnEntry> onEntrys
			, LemsCollection<FinalParam> params,LemsCollection<ParamValue> combinedParameterValues) throws ContentError, JsonGenerationException, IOException
	{
		ArrayList<EDOnEntry> onEDEntrys = new ArrayList<EDOnEntry>();
		for (OnEntry oe: onEntrys)
		{
			EDOnEntry edOnEntry = new EDOnEntry(); 
			StringBuilder sensitivityList = new StringBuilder();
			
			//g.writeObjectFieldStart(SOMKeywords.EFFECT.get());

			edOnEntry.stateAssignment = new ArrayList<EDStateAssignment>();
			
			for (StateAssignment sa: oe.getStateAssignments())
			{
				EDStateAssignment edStateAssignment = new EDStateAssignment();
				String completeExpr = VHDLEquations.encodeVariablesStyle(sa.getValueExpression(),ct.getFinalParams(),
						ct.getDynamics().getStateVariables(),ct.getDynamics().getDerivedVariables(),
						ct.getRequirements(),ct.getPropertys(),sensitivityList,params,combinedParameterValues);
				completeExpr = completeExpr.replaceAll(" \\$\\# "," \\( ").replaceAll(" \\#\\$ "," \\) ");

				edStateAssignment.name=(sa.getVariable());
				edStateAssignment.expression=(completeExpr );
				edOnEntry.stateAssignment.add(edStateAssignment);
			}


			
			edOnEntry.events = new ArrayList<EDEventOut>();

			
			for (EventOut eo: oe.getEventOuts())
			{
				EDEventOut edEventOut = new EDEventOut();
				edEventOut.name=(eo.getPortName());
				edOnEntry.events.add(edEventOut);
			}


			edOnEntry.transitions = new ArrayList<EDTransition>();
			
			for (Transition tr: oe.getTransitions())
			{
				EDTransition edTransition = new EDTransition();
				edTransition.name=(tr.getRegime());
				edOnEntry.transitions.add(edTransition);
			}
			
			onEDEntrys.add(edOnEntry);
			
		}
		//g.writeEndArray();
		return onEDEntrys;

	}
	



	public static ArrayList<EDDynamic> writeTimeDerivatives( ComponentType ct, LemsCollection<TimeDerivative> timeDerivatives
			, LemsCollection<FinalParam> params,LemsCollection<ParamValue> combinedParameterValues, String regimeAddition)  throws ContentError, JsonGenerationException, IOException
	{
		ArrayList<EDDynamic> eddynamics = new ArrayList<EDDynamic>();
		for (TimeDerivative td: timeDerivatives)
		{
			StringBuilder sensitivityList = new StringBuilder();
			EDDynamic edDynamic = new EDDynamic();
			edDynamic.name = td.getVariable();
			String value = VHDLEquations.encodeVariablesStyle(td.getValueExpression(),ct.getFinalParams(),
					ct.getDynamics().getStateVariables(),ct.getDynamics().getDerivedVariables(),
					ct.getRequirements(),ct.getPropertys(),sensitivityList,params,combinedParameterValues);
			value = VHDLEquations.writeInternalExpLnLogEvaluators(value,edDynamic,td.getVariable(),sensitivityList,regimeAddition);
			edDynamic.Dynamics =  value; 
			if  (sensitivityList.length() > 0)
				edDynamic.sensitivityList = (sensitivityList.replace(sensitivityList.length()-1, sensitivityList.length(), " ").toString());
			else
				edDynamic.sensitivityList = (" ");
			eddynamics.add(edDynamic);
		}
		return eddynamics;
	}
	
	
	public static String writeConditionList( ComponentType ct, String ineq, StringBuilder sensitivityList, LemsCollection<FinalParam> params,LemsCollection<ParamValue> combinedParameterValues) throws ContentError, JsonGenerationException, IOException
	{
	    String[] conditions = ineq.split("(\\.)[andor]+(\\.)");
	    String completeExpr ="";
	    int i = 0;
	    for (i = 0; i < conditions.length; i++)
	    {
	    	completeExpr = completeExpr + VHDLEquations.encodeVariablesStyle(VHDLEquations.inequalityToCondition(conditions[i]),ct.getFinalParams(),
	    			ct.getDynamics().getStateVariables(),ct.getDynamics().getDerivedVariables(),
	    			ct.getRequirements(),ct.getPropertys(),sensitivityList,params,combinedParameterValues)+ " " + VHDLEquations.cond2sign(conditions[i]);

	    	completeExpr = completeExpr.replaceAll(" \\$\\# "," \\( ").replaceAll(" \\#\\$ "," \\) ");
	    	if ( i < conditions.length - 1 )
	    	{
	    		String testForAndOR = ineq.substring(ineq.indexOf(conditions[i]) + conditions[i].length(), ineq.indexOf(conditions[i]) + conditions[i].length()+6).toLowerCase();
	    		if (testForAndOR.contains("and"))
	    			completeExpr = completeExpr + " AND ";
	    		else
		    		completeExpr = completeExpr + " OR ";
	    			
	    	}
	    }

	    return completeExpr;

	}
	
	public static void writeRegimes(EDComponent edComponent, Component comp, boolean writeChildren
			, LemsCollection<FinalParam> params,LemsCollection<ParamValue> combinedParameterValues, Lems lems) throws JsonGenerationException, IOException, ContentError
	{
		ComponentType ct = comp.getComponentType();
		ct.getAbout();
		Dynamics dyn = ct.getDynamics();
		edComponent.regimes = new ArrayList<EDRegime>();
		if (dyn != null)
		{
			LemsCollection<Regime> regimes = dyn.getRegimes();
			if (regimes != null)
				for (Regime reg: regimes)
				{
					EDRegime edRegime = new EDRegime();
					edRegime.name=(reg.getName());
					if (reg.isInitial())
						edRegime.isDefault=("default");
					
					edRegime.dynamics = writeTimeDerivatives( ct, reg.getTimeDerivatives(),params,combinedParameterValues,reg.getName()+"_");
					

					edRegime.derivedparameters = VHDLParameters.writeDerivedParameters(ct, ct.getDerivedParameters(),params,combinedParameterValues);
					

					edRegime.conditions = writeConditions( ct,reg.getOnConditions(),params,combinedParameterValues);
					

					edRegime.events = writeEvents( ct, reg.getOnEvents(),params,combinedParameterValues);
					

					edRegime.onEntrys = writeEntrys( ct, reg.getOnEntrys(),params,combinedParameterValues);
					
					
					edComponent.regimes.add(edRegime);
				}
		}
	}

	public static void writeStateFunctions(EDComponent edComponent, Component comp) throws ContentError, JsonGenerationException, IOException
	{
		ComponentType ct = comp.getComponentType();
		Dynamics dyn = ct.getDynamics();
		edComponent.state_functions = new ArrayList<EDStateFunction>();
		if (dyn != null)
		for (DerivedVariable dv: dyn.getDerivedVariables())
		{
			EDStateFunction edStateFunction = new EDStateFunction();
			edStateFunction.name=(dv.getName());
			if (dv.value == null || dv.value.length()==0)
			{
				edStateFunction.value = ( "0");
			}
			else
			{
				edStateFunction.value=( dv.value);
			}
			edComponent.state_functions.add(edStateFunction);
		}
	}

	public static void writeState(EDComponent edComponent , Component comp
			, LemsCollection<FinalParam> params,LemsCollection<ParamValue> combinedParameterValues)  throws ContentError, JsonGenerationException, IOException
	{
		ComponentType ct = comp.getComponentType();
		Dynamics dyn = ct.getDynamics();
		StringBuilder sensitivityList = new StringBuilder();
		edComponent.state = new ArrayList<EDState>();
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
								ct.getRequirements(),ct.getPropertys(), sensitivityList,params,combinedParameterValues);
						init = init.replaceAll(" \\$\\# "," \\( ").replaceAll(" \\#\\$ "," \\) ");
				    	
					}
				}
			}
			EDState edState = new EDState();
			edState.name=(sv.getName());
			edState.type=(sv.getDimension().getName()+"");
			if (sv.hasExposure())
				edState.exposure=(sv.getExposure().getName()+"");
			edState.sensitivityList = sensitivityList.toString();
			edState.onstart=(init+"");
			VHDLFixedPointDimensions.writeBitLengths(edState,sv.getDimension().getName());
			
			edComponent.state.add(edState);
			
		}
	}
	public static ArrayList<EDEvent> writeEvents(ComponentType ct, LemsCollection<OnEvent> onEvents
			, LemsCollection<FinalParam> params,LemsCollection<ParamValue> combinedParameterValues) throws ContentError, JsonGenerationException, IOException
	{
		ArrayList<EDEvent> events = new ArrayList<EDEvent>();
		for (OnEvent oc: onEvents)
		{
			EDEvent edEvent = new EDEvent();
			StringBuilder sensitivityList = new StringBuilder();

			edEvent.name = ( oc.port.replace(' ', '_').replace('.', '_'));
			
			//g.writeObjectFieldStart(SOMKeywords.EFFECT.get());


			edEvent.stateAssignments = new ArrayList<EDStateAssignment>();
			
			for (StateAssignment sa: oc.getStateAssignments())
			{
				EDStateAssignment edStateAssignment = new EDStateAssignment();
				edStateAssignment.name=(sa.getVariable());
				String completeExpr = VHDLEquations.encodeVariablesStyle(sa.getValueExpression(),
						ct.getFinalParams(),ct.getDynamics().getStateVariables(),ct.getDynamics().getDerivedVariables(),
						ct.getRequirements(),ct.getPropertys(),sensitivityList,params,combinedParameterValues);
				completeExpr = completeExpr.replaceAll(" \\$\\# "," \\( ").replaceAll(" \\#\\$ "," \\) ");
		    	
				edStateAssignment.expression=(completeExpr );
				edStateAssignment.sensitivityList = sensitivityList.toString();
				edEvent.stateAssignments.add(edStateAssignment);
			}


			

			edEvent.events = new ArrayList<EDEventOut>();
			
			for (EventOut eo: oc.getEventOuts())
			{
				EDEventOut edEventOut = new EDEventOut();
				edEventOut.name=(eo.getPortName());
				edEvent.events.add(edEventOut);
			}

			edEvent.transitions = new ArrayList<EDTransition>();
			
			for (Transition tr: oc.getTransitions())
			{
				EDTransition edTransition = new EDTransition();
				edTransition.name=(tr.getRegime());
				edEvent.transitions.add(edTransition);
			}

			events.add(edEvent);
		}
		return events;
	}
	

	public static void writeDerivedVariables(EDComponent edComponent, ComponentType ct, 
			LemsCollection<DerivedVariable> derivedVariables, Component comp
			, LemsCollection<FinalParam> params,LemsCollection<ParamValue> combinedParameterValues, Lems lems)  throws ContentError, JsonGenerationException, IOException
	{
		edComponent.derivedvariables = new ArrayList<EDDerivedVariable>();
		for (DerivedVariable dv: derivedVariables)
		{
			EDDerivedVariable edDerivedVariable = new EDDerivedVariable();
			edDerivedVariable.name = (dv.getName());
			edDerivedVariable.exposure = (dv.getExposure() != null ? dv.getExposure().getName() : "");
						
			String val = dv.getValueExpression();
			String sel = dv.getSelect();

	        StringBuilder sensitivityList = new StringBuilder();
			if (val != null) {
				String value = VHDLEquations.encodeVariablesStyle(dv.getValueExpression(),
						ct.getFinalParams(),ct.getDynamics().getStateVariables(),ct.getDynamics().getDerivedVariables(),
						ct.getRequirements(),ct.getPropertys(),sensitivityList,params,combinedParameterValues) ;
				value = VHDLEquations.writeInternalExpLnLogEvaluators(value,edDerivedVariable,dv.getName(),sensitivityList,"");
				value = value.replaceAll(" \\$\\# "," \\( ").replaceAll(" \\#\\$ "," \\) ");
				edDerivedVariable.value = (value);
				
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
							if (conn.getComponentType().getName().matches("synapticConnection") || conn.getComponentType().getName().matches("synapticConnectionWD") )
							{
								String destination = conn.getTextParam("destination");
								String path = conn.getPathParameterPath("to");
								if ((destination == null || destination.matches(attachName)) && path.startsWith(comp.getID()))
								{
									Component c = (conn.getRefComponents().get("synapse"));
									items.add("exposure_" + dv.getDimension().getName() + "_" + c.getID() + "_" + var+ "_internal");
									sensitivityList.append("exposure_" + dv.getDimension().getName() + "_" + c.getID() + "_" + var + "_internal,");
								
								}
							}
						}
					}
					
					
					
					selval = iterativeReduction(items,op).get(0); //StringUtil.join(items, op);
					
					String encodedValue = VHDLEquations.encodeVariablesStyle(selval,
							ct.getFinalParams(),ct.getDynamics().getStateVariables(),ct.getDynamics().getDerivedVariables(),
							ct.getRequirements(),ct.getPropertys(),sensitivityList,params,combinedParameterValues);
					encodedValue = encodedValue.replaceAll(" \\$\\# "," \\( ").replaceAll(" \\#\\$ "," \\) ");
					edDerivedVariable.value=(encodedValue);
					
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
					String encodedValue = VHDLEquations.encodeVariablesStyle(selval,
							ct.getFinalParams(),ct.getDynamics().getStateVariables(),ct.getDynamics().getDerivedVariables(),
							ct.getRequirements(),ct.getPropertys(),sensitivityList,params,combinedParameterValues);
					encodedValue = encodedValue.replaceAll(" \\$\\# "," \\( ").replaceAll(" \\#\\$ "," \\) ");
			    	
					edDerivedVariable.value = (encodedValue);
					
					
				}
					
			}

			edDerivedVariable.type = (dv.getDimension().getName()+"");
			edDerivedVariable.sensitivityList = (sensitivityList.length() == 0 ? "" : sensitivityList.substring(0,sensitivityList.length()-1));
			VHDLFixedPointDimensions.writeBitLengths(edDerivedVariable,dv.getDimension().getName());

			edComponent.derivedvariables.add(edDerivedVariable);
		}

	}
	
	
	private static ArrayList<String> iterativeReduction(ArrayList<String> input, String reduction)
	{
		ArrayList<String> output = new ArrayList<String>();
		String lastItem = input.get(0);
		int i =1;
		for (i = 1; i < input.size();i++)
		{
			if (i%2 == 1)
			{
				lastItem = "( " + lastItem + " " + reduction +  " " + input.get(i) + " )";
				output.add(lastItem);
			}
			else
				lastItem = input.get(i);
		}
		if (i%2 == 1)
		{
			output.add(lastItem);
		}
		
		if (output.size() > 1)
			output = iterativeReduction(output,reduction);
		return output;		
	}
	
	public static ArrayList<EDCondition> writeConditions(ComponentType ct, LemsCollection<OnCondition> onConditions
			, LemsCollection<FinalParam> params,LemsCollection<ParamValue> combinedParameterValues) throws ContentError, JsonGenerationException, IOException
	{
		ArrayList<EDCondition> conditions = new ArrayList<EDCondition>();
		for (OnCondition oc: onConditions)
		{
			EDCondition edCondition = new EDCondition();
			StringBuilder sensitivityList = new StringBuilder();

			edCondition.name =  (oc.test.replace(' ', '_').replace('.', '_'));
			
			edCondition.condition = writeConditionList(ct,oc.test,sensitivityList,params,combinedParameterValues);
			
			//g.writeObjectFieldStart(SOMKeywords.EFFECT.get());

			edCondition.stateAssignment = new ArrayList<EDStateAssignment>();
			for (StateAssignment sa: oc.getStateAssignments())
			{
				EDStateAssignment edStateAssignment = new EDStateAssignment();
				String completeExpr = VHDLEquations.encodeVariablesStyle(sa.getValueExpression(),ct.getFinalParams(),
						ct.getDynamics().getStateVariables(),ct.getDynamics().getDerivedVariables(),
						ct.getRequirements(),ct.getPropertys(),sensitivityList,params,combinedParameterValues);
		    	completeExpr = completeExpr.replaceAll(" \\$\\# "," \\( ").replaceAll(" \\#\\$ "," \\) ");

		    	edStateAssignment.name = (sa.getVariable());
		    	edStateAssignment.expression = ( completeExpr);
		    	edCondition.stateAssignment.add(edStateAssignment);
			}
			
			edCondition.events = new ArrayList<EDEventOut>();
			
			for (EventOut eo: oc.getEventOuts())
			{
				EDEventOut edEventOut = new EDEventOut();
				edEventOut.name=(eo.getPortName());
				edCondition.events.add(edEventOut);
			}

			

			edCondition.transitions = new ArrayList<EDTransition>();
			
			for (Transition tr: oc.getTransitions())
			{
				EDTransition edTransition = new EDTransition();
				edTransition.name = tr.getRegime();
				edCondition.transitions.add(edTransition);
			}



		
			
			//g.writeEndObject();
			edCondition.sensitivityList = (sensitivityList.deleteCharAt(sensitivityList.length()-1).toString());
			conditions.add(edCondition);
			
		}
		return conditions;
	}

	
}

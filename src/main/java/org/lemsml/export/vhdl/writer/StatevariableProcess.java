package org.lemsml.export.vhdl.writer;

import java.util.Iterator;

import org.lemsml.export.vhdl.edlems.EDComponent;
import org.lemsml.export.vhdl.edlems.EDCondition;
import org.lemsml.export.vhdl.edlems.EDConditionalDerivedVariable;
import org.lemsml.export.vhdl.edlems.EDDerivedParameter;
import org.lemsml.export.vhdl.edlems.EDDerivedVariable;
import org.lemsml.export.vhdl.edlems.EDEvent;
import org.lemsml.export.vhdl.edlems.EDEventOut;
import org.lemsml.export.vhdl.edlems.EDEventPort;
import org.lemsml.export.vhdl.edlems.EDExponential;
import org.lemsml.export.vhdl.edlems.EDOnEntry;
import org.lemsml.export.vhdl.edlems.EDPower;
import org.lemsml.export.vhdl.edlems.EDState;
import org.lemsml.export.vhdl.edlems.EDRegime;
import org.lemsml.export.vhdl.edlems.EDDynamic;
import org.lemsml.export.vhdl.edlems.EDStateAssignment;


public class StatevariableProcess {

	static void writeDynamicsPreCombProc(StringBuilder sb, EDComponent comp,StringBuilder sensitivityList)
	{
		sensitivityList.append("sysparam_time_timestep");

		for(Iterator<EDState> j = comp.state.iterator(); j.hasNext(); ) {
			EDState state = j.next(); 
			for(Iterator<EDRegime> k = comp.regimes.iterator(); k.hasNext(); ) {
				EDRegime regime = k.next(); 
				for(Iterator<EDDynamic> l = regime.dynamics.iterator(); l.hasNext(); ) {
					EDDynamic dynamic = l.next(); 
					if (dynamic.name.matches(state.name))
						sensitivityList.append("," + dynamic.sensitivityList);
				}
			}
			for(Iterator<EDDynamic> l = comp.dynamics.iterator(); l.hasNext(); ) {
				EDDynamic dynamic = l.next(); 
				if (dynamic.name.matches(state.name))
					sensitivityList.append("," + dynamic.sensitivityList);
			}
		}
		
		sb.append("\r\n" + 
				"dynamics_pre_process_comb :process ( " +  sensitivityList.toString() + " )\r\n" + 
				"begin \r\n" + 
				"");
		for(Iterator<EDState> j = comp.state.iterator(); j.hasNext(); ) {
			EDState state = j.next(); 
			for(Iterator<EDRegime> k = comp.regimes.iterator(); k.hasNext(); ) {
				EDRegime regime = k.next(); 
				for(Iterator<EDDynamic> l = regime.dynamics.iterator(); l.hasNext(); ) {
					EDDynamic dynamic = l.next(); 
					if (dynamic.name.matches(state.name))
					{
						for(Iterator<EDExponential> i = dynamic.Exponentials.iterator(); i.hasNext(); ) {
							EDExponential exponential = i.next();
							sb.append("pre_exp_" + regime.name + "_" + state.name + "_" + exponential.name + "_next <=  resize( " + exponential.value + " ,20, -20);" );
							
						}
						for(Iterator<EDPower> i = dynamic.Powers.iterator(); i.hasNext(); ) {
							EDPower power = i.next();
							sb.append("pre_pow_" + regime.name + "_" + state.name + "_" + power.name + "_A_next <=  resize( " + power.valueA + " ,20, -20);" );
							sb.append("pre_pow_" + regime.name + "_" + state.name + "_" + power.name + "_X_next <=  resize( " + power.valueX + " ,20, -20);" );
							
						}
					}
				}
			}
			for(Iterator<EDDynamic> l = comp.dynamics.iterator(); l.hasNext(); ) {
				EDDynamic dynamic = l.next(); 
				if (dynamic.name.matches(state.name))
				{
					for(Iterator<EDExponential> i = dynamic.Exponentials.iterator(); i.hasNext(); ) {
						EDExponential exponential = i.next();
						sb.append("pre_exp_noregime_" + state.name + "_" + exponential.name + "_next <=  resize( " + exponential.value + " ,20, -20);" );
						
					}
					for(Iterator<EDPower> i = dynamic.Powers.iterator(); i.hasNext(); ) {
						EDPower power = i.next();
						sb.append("pre_pow_noregime_" + state.name + "_" + power.name + "_A_next <=  resize( " + power.valueA + " ,20, -20);" );
						sb.append("pre_pow_noregime_" + state.name + "_" + power.name + "_X_next <=  resize( " + power.valueX + " ,20, -20);" );
						
					}
				}
			}
		}
		
		sb.append("\r\n" + 
				"end process dynamics_pre_process_comb;\r\n");
		
	}

	static void writeDynamicsPreSynProc(StringBuilder sb, EDComponent comp)
	{
		sb.append("\r\n" + 
				"dynamics_pre_process_syn :process ( clk, rst )\r\n" + 
				"begin \r\n");
		
		for(Iterator<EDState> j = comp.state.iterator(); j.hasNext(); ) {
			EDState state = j.next(); 
			for(Iterator<EDRegime> k = comp.regimes.iterator(); k.hasNext(); ) {
				EDRegime regime = k.next(); 
				for(Iterator<EDDynamic> l = regime.dynamics.iterator(); l.hasNext(); ) {
					EDDynamic dynamic = l.next(); 
					if (dynamic.name.matches(state.name))
					{
						for(Iterator<EDExponential> i = dynamic.Exponentials.iterator(); i.hasNext(); ) {
							EDExponential exponential = i.next();
							sb.append("if rst  = '1' then\r\n" + 
									"							pre_exp_" + regime.name + "_" + state.name + "_" + exponential.name + " <= to_sfixed(0,20, -20);\r\n" + 
									"						elsif (clk'EVENT AND clk = '1') then	\r\n" + 
									"							if subprocess_all_ready = '1' then\r\n" + 
									"								pre_exp_" + regime.name + "_" + state.name + "_" + exponential.name + " <= pre_exp_" + regime.name + "_" + state.name + "_" + exponential.name + "_next ;\r\n" + 
									"							end if;\r\n" + 
									"						end if;" );
							
						}
						for(Iterator<EDPower> i = dynamic.Powers.iterator(); i.hasNext(); ) {
							EDPower power = i.next();
							sb.append("if rst  = '1' then\r\n" + 
									"							pre_pow_" + regime.name + "_" + state.name + "_" + power.name + "_A <= to_sfixed(0,20, -20);\r\n" + 
									"							pre_pow_" + regime.name + "_" + state.name + "_" + power.name + "_X <= to_sfixed(0,20, -20);\r\n" + 
									"						elsif (clk'EVENT AND clk = '1') then	\r\n" + 
									"							if subprocess_all_ready = '1' then\r\n" + 
									"								pre_pow_" + regime.name + "_" + state.name + "_" + power.name + "_A <= pre_pow_" + regime.name + "_" + state.name + "_" + power.name + "_A_next ;\r\n" + 
									"								pre_pow_" + regime.name + "_" + state.name + "_" + power.name + "_X <= pre_pow_" + regime.name + "_" + state.name + "_" + power.name + "_X_next ;\r\n" + 
									"							end if;\r\n" + 
									"						end if;");
						}
					}
				}
			}
			for(Iterator<EDDynamic> l = comp.dynamics.iterator(); l.hasNext(); ) {
				EDDynamic dynamic = l.next(); 
				if (dynamic.name.matches(state.name))
				{
					for(Iterator<EDExponential> i = dynamic.Exponentials.iterator(); i.hasNext(); ) {
						EDExponential exponential = i.next();
						sb.append("if rst  = '1' then\r\n" + 
								"							pre_exp_noregime_" + state.name + "_" + exponential.name + " <= to_sfixed(0,20, -20);\r\n" + 
								"						elsif (clk'EVENT AND clk = '1') then	\r\n" + 
								"							if subprocess_all_ready = '1' then\r\n" + 
								"								pre_exp_noregime_" + state.name + "_" + exponential.name + " <= pre_exp_noregime_" + state.name + "_" + exponential.name + "_next ;\r\n" + 
								"							end if;\r\n" + 
								"						end if;" );
					}
					for(Iterator<EDPower> i = dynamic.Powers.iterator(); i.hasNext(); ) {
						EDPower power = i.next();
						sb.append("if rst  = '1' then\r\n" + 
								"							pre_pow_noregime_" + state.name + "_" + power.name + "_A <= to_sfixed(0,20, -20);\r\n" + 
								"							pre_pow_noregime_" + state.name + "_" + power.name + "_X <= to_sfixed(0,20, -20);\r\n" + 
								"						elsif (clk'EVENT AND clk = '1') then	\r\n" + 
								"							if subprocess_all_ready = '1' then\r\n" + 
								"								pre_pow_noregime_" + state.name + "_" + power.name + "_A <= pre_pow_noregime_" + state.name + "_" + power.name + "_A_next ;\r\n" + 
								"								pre_pow_noregime_" + state.name + "_" + power.name + "_X <= pre_pow_noregime_" + state.name + "_" + power.name + "_X_next ;\r\n" + 
								"							end if;\r\n" + 
								"						end if;");
					}
				}
			}
		}
		sb.append("\r\n" + 
				"subprocess_dyn_int_pre_ready <= '1';\r\n" + 
				"end process dynamics_pre_process_syn;\r\n");
	}

	static void writeDynamicsInstantiationsProc(StringBuilder sb, EDComponent comp)
	{

		int count_der_int = 0;
		
		for(Iterator<EDState> j = comp.state.iterator(); j.hasNext(); ) {
			EDState state = j.next(); 
			for(Iterator<EDRegime> k = comp.regimes.iterator(); k.hasNext(); ) {
				EDRegime regime = k.next(); 
				for(Iterator<EDDynamic> l = regime.dynamics.iterator(); l.hasNext(); ) {
					EDDynamic dynamic = l.next(); 
					if (dynamic.name.matches(state.name))
					{
						for(Iterator<EDExponential> i = dynamic.Exponentials.iterator(); i.hasNext(); ) {
							EDExponential exponential = i.next();
							sb.append("ParamExp_" + regime.name + "_" + state.name + "_" + exponential.name + " : ParamExp \r\n" + 
									"						port map (	clk => clk,\r\n" + 
									"									rst => rst,\r\n" + 
									"									Start => step_once_go,\r\n" + 
									"									Done => subprocess_dyn_int_ready,\r\n" + 
									"									X => pre_exp_" + regime.name + "_" + state.name + "_" + exponential.name + ",\r\n" + 
									"									Output => exp_" + regime.name + "_" + state.name + "_" + exponential.name + "\r\n" + 
									"									);\r\n" );
							count_der_int++;
						}
						for(Iterator<EDPower> i = dynamic.Powers.iterator(); i.hasNext(); ) {
							EDPower power = i.next();
							sb.append("ParamPow_" + regime.name + "_" + state.name + "_" + power.name + " : ParamPow \r\n" + 
									"						port map (	clk => clk,\r\n" + 
									"									rst => rst,\r\n" + 
									"									Start => step_once_go,\r\n" + 
									"									Done => subprocess_dyn_int_ready,\r\n" + 
									"									X => pre_pow_" + regime.name + "_" + state.name + "_" + power.name + "_X ,\r\n" + 
									"									A => pre_pow_" + regime.name + "_" + state.name + "_" + power.name + "_A ,\r\n" + 
									"									Output => pow_" + regime.name + "_" + state.name + "_" + power.name + "\r\n" + 
									"									);\r\n");
							count_der_int++;
						}
					}
				}
			}
			for(Iterator<EDDynamic> l = comp.dynamics.iterator(); l.hasNext(); ) {
				EDDynamic dynamic = l.next(); 
				if (dynamic.name.matches(state.name))
				{
					for(Iterator<EDExponential> i = dynamic.Exponentials.iterator(); i.hasNext(); ) {
						EDExponential exponential = i.next();
						sb.append("ParamExp_noregime_" + state.name + "_" + exponential.name + " : ParamExp \r\n" + 
								"						port map (	clk => clk,\r\n" + 
								"									rst => rst,\r\n" + 
								"									Start => step_once_go,\r\n" + 
								"									Done => subprocess_dyn_int_ready,\r\n" + 
								"									X => pre_exp_noregime_" + state.name + "_" + exponential.name + ",\r\n" + 
								"									Output => exp_noregime_" + state.name + "_" + exponential.name + "\r\n" + 
								"									);\r\n" );
						count_der_int++;
					}
					for(Iterator<EDPower> i = dynamic.Powers.iterator(); i.hasNext(); ) {
						EDPower power = i.next();
						sb.append("ParamPow_noregime_" + state.name + "_" + power.name + " : ParamPow \r\n" + 
								"						port map (	clk => clk,\r\n" + 
								"									rst => rst,\r\n" + 
								"									Start => step_once_go,\r\n" + 
								"									Done => subprocess_dyn_int_ready,\r\n" + 
								"									X => pre_pow_noregime_" + state.name + "_" + power.name + "_X ,\r\n" + 
								"									A => pre_pow_noregime_" + state.name + "_" + power.name + "_A ,\r\n" + 
								"									Output => pow_noregime_" + state.name + "_" + power.name + "\r\n" + 
								"									);\r\n");
						count_der_int++;
					}
				}
			}
		}
		if (count_der_int == 0)
			sb.append("\r\n" + 
					"subprocess_dyn_int_ready <= '1';\r\n");
		
	}

	static void writeDynamicsCombProcess(StringBuilder sb, EDComponent comp, StringBuilder sensitivityList)
	{
		sb.append("\r\n" + 
				"state_variable_process_dynamics_comb :process (" + sensitivityList + ")\r\n" + 
				"begin\r\n");
		
		for(Iterator<EDState> j = comp.state.iterator(); j.hasNext(); ) {
			EDState state = j.next(); 
			for(Iterator<EDRegime> k = comp.regimes.iterator(); k.hasNext(); ) {
				EDRegime regime = k.next(); 
				for(Iterator<EDDynamic> l = regime.dynamics.iterator(); l.hasNext(); ) {
					EDDynamic dynamic = l.next(); 
					if (dynamic.name.matches(state.name))
					{
						sb.append("statevariable_" + state.type +  "_" + regime.name +  "_" + state.name + "_temp_1_next <= resize(statevariable_" + state.type +  "_" + state.name + "_in + (" + dynamic.Dynamics + ") * sysparam_time_timestep," + state.integer + "," + state.fraction + ");\r\n");
					}
				}
			}
			for(Iterator<EDDynamic> l = comp.dynamics.iterator(); l.hasNext(); ) {
				EDDynamic dynamic = l.next(); 
				if (dynamic.name.matches(state.name))
				{
					sb.append("statevariable_" + state.type +  "_noregime_" + state.name + "_temp_1_next <= resize(statevariable_" + state.type +  "_" + state.name + "_in + (" + dynamic.Dynamics + ") * sysparam_time_timestep," + state.integer + "," + state.fraction + ");\r\n");
				
				}
			}
		}
		sb.append("\r\n" + 
				"subprocess_dyn_ready <= '1';\r\n" + 
				"end process state_variable_process_dynamics_comb;\r\n");
		
	}

	static void writeDynamicsSynProcess(StringBuilder sb, EDComponent comp )
	{

		
		sb.append("state_variable_process_dynamics_syn :process (CLK)\r\n" + 
				"begin\r\n"
				+ "if clk'event and clk = '1' then  \r\n" + 
				"if subprocess_all_ready = '1' then  \r\n");

		for(Iterator<EDState> j = comp.state.iterator(); j.hasNext(); ) {
			EDState state = j.next(); 
			for(Iterator<EDRegime> k = comp.regimes.iterator(); k.hasNext(); ) {
				EDRegime regime = k.next(); 
				for(Iterator<EDDynamic> l = regime.dynamics.iterator(); l.hasNext(); ) {
					EDDynamic dynamic = l.next(); 
					if (dynamic.name.matches(state.name))
					{
						sb.append("statevariable_" + state.type +  "_" + regime.name +  "_" + state.name + "_temp_1 <= statevariable_" + state.type +  "_" + regime.name +  "_" + state.name + "_temp_1_next;\r\n");
					}
				}
			}
			for(Iterator<EDDynamic> l = comp.dynamics.iterator(); l.hasNext(); ) {
				EDDynamic dynamic = l.next(); 
				if (dynamic.name.matches(state.name))
				{
					sb.append("statevariable_" + state.type +  "_noregime_" + state.name + "_temp_1 <= statevariable_" + state.type +  "_noregime_" + state.name + "_temp_1_next;\r\n");
				}
			}
		}
		
		sb.append("\r\n" + 
				"end if;\r\n" + 
				"end if;\r\n" + 
				"end process state_variable_process_dynamics_syn;\r\n" + 
				"");
	}

	static void writeStateVariableDriverProcess(StringBuilder sb, EDComponent comp,
			EDState state , StringBuilder sensitivityList, int currentEDState)
	{
		sb.append("---------------------------------------------------------------------\r\n" + 
				"	-- EDState variable: $par.name Driver Process\r\n" + 
				"	---------------------------------------------------------------------\r\n");

		sensitivityList = new StringBuilder();
		sensitivityList.append("sysparam_time_timestep,reset_model");
		if (state.onstart != null && !state.onstart.matches("0") && sensitivityList != null && sensitivityList.length() > 0)
		{
			sensitivityList.append("," + state.sensitivityList);
		}
		int temporarySignalsRequired = 0;
		for(Iterator<EDEvent> i = comp.events.iterator(); i.hasNext(); ) {
			EDEvent event = i.next();
			for(Iterator<EDStateAssignment> k = event.stateAssignments.iterator(); k.hasNext(); ) {
				EDStateAssignment state2 = k.next(); 
				if (state2.name.matches(state.name))
				{
					temporarySignalsRequired++;
					sensitivityList.append(",eventport_in_" + event.name);
					sensitivityList.append("," + state2.sensitivityList);
				}					
			}
		}
		for(Iterator<EDCondition> i = comp.conditions.iterator(); i.hasNext(); ) {
			EDCondition condition = i.next();
			for(Iterator<EDStateAssignment> k = condition.stateAssignment.iterator(); k.hasNext(); ) {
				EDStateAssignment state2 = k.next(); 
				if (state2.name.matches(state.name))
				{
					temporarySignalsRequired++;
					sensitivityList.append("," + condition.sensitivityList);
				}					
			}
		}
		
		if (comp.regimes.size() > 0)
		{
			sensitivityList.append(",current_regime_in_int,next_regime");
			for(Iterator<EDRegime> l = comp.regimes.iterator(); l.hasNext(); ) {
				EDRegime regime = l.next();
				for(Iterator<EDEvent> i = regime.events.iterator(); i.hasNext(); ) {
					EDEvent event = i.next();
					for(Iterator<EDStateAssignment> k = event.stateAssignments.iterator(); k.hasNext(); ) {
						EDStateAssignment state2 = k.next(); 
						if (state2.name.matches(state.name))
						{
							temporarySignalsRequired++;
							sensitivityList.append(",eventport_in_" + event.name);
							sensitivityList.append("," + state2.sensitivityList);
						}					
					}
				}
				for(Iterator<EDCondition> i = regime.conditions.iterator(); i.hasNext(); ) {
					EDCondition condition = i.next();
					for(Iterator<EDStateAssignment> k = condition.stateAssignment.iterator(); k.hasNext(); ) {
						EDStateAssignment state2 = k.next(); 
						if (state2.name.matches(state.name))
						{
							temporarySignalsRequired++;
							sensitivityList.append("," + condition.sensitivityList);
						}					
					}
				}

				for(Iterator<EDDynamic> i = regime.dynamics.iterator(); i.hasNext(); ) {
					EDDynamic dynamic = i.next();
					if (dynamic.name.matches(state.name))
					{
						temporarySignalsRequired++;
						sensitivityList.append(",statevariable_" + state.type +  "_" + regime.name + "_" + state.name + "_temp_1, " + dynamic.sensitivityList);
					}	
				}

				for(Iterator<EDOnEntry> i = regime.onEntrys.iterator(); i.hasNext(); ) {
					EDOnEntry onEntry = i.next();
					for(Iterator<EDStateAssignment> k = onEntry.stateAssignment.iterator(); k.hasNext(); ) {
						EDStateAssignment state2 = k.next(); 
						if (state2.name.matches(state.name))
						{
							temporarySignalsRequired++;
							//sensitivityList.append(",statevariable_" + state.type +  "_" + regime.name + "_" + state.name + "_temp_1, " + onEntry.sensitivityList);
						}	
					}
				}
				
			}				
		}
		for(Iterator<EDDynamic> i = comp.dynamics.iterator(); i.hasNext(); ) {
			EDDynamic dynamic = i.next();
			if (dynamic.name.matches(state.name))
			{
				temporarySignalsRequired++;
				sensitivityList.append(",statevariable_" + state.type +  "_noregime_" + state.name + "_temp_1, " + dynamic.sensitivityList);
			}	
		}
		
		sensitivityList = new StringBuilder(sensitivityList.toString().replace(" ","").replace(",,",","));
		
		sb.append("state_variable_process_comb_" +currentEDState + " :process (" + sensitivityList.toString() + ")\r\n");
		
		//first write as many temp signals as required
		for (int i = 1; i < temporarySignalsRequired + 1; i++)
		{
			sb.append("variable statevariable_" + state.type +  "_" + state.name + 
					"_temp_" + i + " : sfixed (" + state.integer + " downto " + 
					state.fraction + ");\r\n");
		}
		
		//now begin the actual process
		sb.append("begin\r\n");
		int currentTemporarySignalID = 1;
		//dynamics first
		for(Iterator<EDDynamic> i = comp.dynamics.iterator(); i.hasNext(); ) {
			EDDynamic dynamic = i.next();
			if (dynamic.name.matches(state.name))
			{
				sb.append("statevariable_" + state.type +  "_" + state.name + 
						"_temp_1 := statevariable_" + state.type +  "_noregime_" +
						state.name + "_temp_1;");
				currentTemporarySignalID++;
			}	
		}
		//next events
		String eventInput = "statevariable_" + state.type +  "_" + state.name + "_in" ;
		String eventInputReplace = "statevariable_" + state.type +  "_" + state.name + "_temp_" + (currentTemporarySignalID - 1) ;
		for(Iterator<EDEvent> i = comp.events.iterator(); i.hasNext(); ) {
			EDEvent event = i.next();
			for(Iterator<EDStateAssignment> k = event.stateAssignments.iterator(); k.hasNext(); ) {
				EDStateAssignment state2 = k.next(); 
				if (state2.name.matches(state.name))
				{
					sb.append("if eventport_in_" + event.name + " = '1' then\r\n" + 
							"			statevariable_" + state.type +  "_" + state.name + 
							"_temp_" + currentTemporarySignalID + " := resize(" + state2.expression.replace(eventInput,eventInputReplace) + "," + state.integer + "," + state.fraction + ");\r\n" + 
							"		else\r\n");
					if (currentTemporarySignalID > 1) {
						sb.append("			statevariable_" + state.type +  "_" + state.name + "_temp_" + currentTemporarySignalID + " := statevariable_" + state.type +  "_" + state.name + "_temp_" + (currentTemporarySignalID - 1) + ";\r\n" );
					}
					else
					{
						sb.append("			statevariable_" + state.type +  "_" + state.name + "_temp_" + currentTemporarySignalID + " := statevariable_" + state.type +  "_" + state.name + "_in;\r\n");
					}
					sb.append("		end if;\r\n");
					currentTemporarySignalID++;
				}					
			}
		}
		//next conditions
		for(Iterator<EDCondition> i = comp.conditions.iterator(); i.hasNext(); ) {
			EDCondition condition = i.next();
			for(Iterator<EDStateAssignment> k = condition.stateAssignment.iterator(); k.hasNext(); ) {
				EDStateAssignment state2 = k.next(); 
				if (state2.name.matches(state.name))
				{
					sb.append("if " + condition.condition + " then\r\n" + 
							"			statevariable_" + state.type +  "_" + state.name + 
							"_temp_" + currentTemporarySignalID + " := resize(" + state2.expression.replace(eventInput,eventInputReplace) + "," + state.integer + "," + state.fraction + ");\r\n" + 
							"		else\r\n");
					if (currentTemporarySignalID > 1) {
						sb.append("			statevariable_" + state.type +  "_" + state.name + "_temp_" + currentTemporarySignalID + " := statevariable_" + state.type +  "_" + state.name + "_temp_" + (currentTemporarySignalID - 1) + ";\r\n" );
					}
					else
					{
						sb.append("			statevariable_" + state.type +  "_" + state.name + "_temp_" + currentTemporarySignalID + " := statevariable_" + state.type +  "_" + state.name + "_in;\r\n");
					}
					sb.append("		end if;\r\n");
					currentTemporarySignalID++;
				}					
			}
		}
		
		//next regimes
		for(Iterator<EDRegime> l = comp.regimes.iterator(); l.hasNext(); ) {
			EDRegime regime = l.next();
			boolean hasEDDynamics = false;
			for(Iterator<EDDynamic> i = regime.dynamics.iterator(); i.hasNext(); ) {
				EDDynamic dynamic = i.next();
				if (dynamic.name.matches(state.name))
				{
					hasEDDynamics = true;
					break;
				}	
			}
			if (hasEDDynamics) {
				sb.append("\r\n" + 
						"if ( current_regime_in_int = " + regime.name + " ) then\r\n" + 
						"statevariable_" + state.type +  "_" + state.name + 
						"_temp_1 := statevariable_" + state.type +  "_" + regime.name + 
						"_" + state.name + "_temp_1;\r\n" + 
						"end if;\r\n" + 
						"");
				currentTemporarySignalID++;
			}
			else {
				sb.append("\r\n" + 
						"if ( current_regime_in_int = " + regime.name + " ) then\r\n" + 
						"statevariable_" + state.type +  "_" + state.name + "_temp_1 := resize(statevariable_" + state.type +  "_" + state.name + "_in ," + state.integer + "," + state.fraction + ");\r\n" + 
						"end if;\r\n" + 
						"");
			}
			
			
			
			for(Iterator<EDEvent> i = regime.events.iterator(); i.hasNext(); ) {
				EDEvent event = i.next();
				for(Iterator<EDStateAssignment> k = event.stateAssignments.iterator(); k.hasNext(); ) {
					EDStateAssignment state2 = k.next(); 
					if (state2.name.matches(state.name))
					{
						sb.append("if ( current_regime_in_int = " + regime.name + " ) AND eventport_in_" +
								event.name  + "= '1' then\r\n" + 
								"				statevariable_" + state.type +  "_" + state.name + "_temp_" + 
								currentTemporarySignalID + " := resize(" + state2.expression.replace(eventInput,eventInputReplace) + "," + state.integer + "," + state.fraction + ");\r\n" + 
								"			else\r\n" );
						
						if (currentTemporarySignalID > 1 ) {
							sb.append("					statevariable_" + state.type +  "_"
									+ state.name + "_temp_" + currentTemporarySignalID + " := statevariable_" + state.type
									+  "_" + state.name + "_temp_" + (currentTemporarySignalID - 1) + ";\r\n");
						}
						else {
							sb.append("					statevariable_" + state.type +  "_" 
									+ state.name + "_temp_" + currentTemporarySignalID + " := statevariable_" + state.type +  "_" + state.name + "_in;\r\n");
						}
						sb.append("			end if;");
						
						
						currentTemporarySignalID++;
					}					
				}
			}

			for(Iterator<EDCondition> i = regime.conditions.iterator(); i.hasNext(); ) {
				EDCondition condition = i.next();
				for(Iterator<EDStateAssignment> k = condition.stateAssignment.iterator(); k.hasNext(); ) {
					EDStateAssignment state2 = k.next(); 
					if (state2.name.matches(state.name))
					{
						sb.append("if ( current_regime_in_int = " + regime.name + " ) AND " +
								condition.condition  +  " then\r\n" + 
								"				statevariable_" + state.type +  "_" + state.name + "_temp_" + 
								currentTemporarySignalID + " := resize(" + state2.expression.replace(eventInput,eventInputReplace) + "," + state.integer + "," + state.fraction + ");\r\n" + 
								"			else\r\n" );
						
						if (currentTemporarySignalID > 1 ) {
							sb.append("					statevariable_" + state.type +  "_"
									+ state.name + "_temp_" + currentTemporarySignalID + " := statevariable_" + state.type
									+  "_" + state.name + "_temp_" + (currentTemporarySignalID - 1) + ";\r\n");
						}
						else {
							sb.append("					statevariable_" + state.type +  "_" 
									+ state.name + "_temp_" + currentTemporarySignalID + " := statevariable_" + state.type +  "_" + state.name + "_in;\r\n");
						}
						sb.append("			end if;");
						
						
						currentTemporarySignalID++;
					}					
				}
			}
		}
		
		for(Iterator<EDRegime> l = comp.regimes.iterator(); l.hasNext(); ) {
			EDRegime regime = l.next();
			sb.append("\r\n" + 
					"if (not ( current_regime_in_int = next_regime )) and ( next_regime = " + regime.name + " ) then\r\n");
			

			for(Iterator<EDOnEntry> i = regime.onEntrys.iterator(); i.hasNext(); ) {
				EDOnEntry onEntry = i.next();
				for(Iterator<EDStateAssignment> k = onEntry.stateAssignment.iterator(); k.hasNext(); ) {
					EDStateAssignment state2 = k.next(); 
					if (state2.name.matches(state.name))
					{
						eventInput = "statevariable_" + state.type +  "_" + state.name + "_in" ;
						eventInputReplace = "statevariable_" + state.type +  "_" + state.name + "_temp_" + (currentTemporarySignalID - 1) ;
						sb.append("statevariable_" + state.type +  "_" + state.name + "_temp_"
								+ currentTemporarySignalID + " := resize(" + state2.expression.replace(eventInput,eventInputReplace) + "," 
								+ state.integer + "," + state.fraction + ");\r\n" + 
								"			else\r\n");
						if (currentTemporarySignalID == 1) {
							sb.append("			statevariable_" + state.type +  "_" + 
							state.name + "_temp_" + currentTemporarySignalID + " := statevariable_" + state.type 
							+  "_" + state.name +  "_in;\r\n");
						}
						else {
							sb.append("			statevariable_" + state.type +  "_" + 
							state.name + "_temp_" + currentTemporarySignalID + " := statevariable_" + state.type
							+  "_" + state.name + "_temp_" + (currentTemporarySignalID - 1) + ";\r\n");
						}
						currentTemporarySignalID++;
					}
				}
			}
			sb.append("\r\n" + 
					"end if;\r\n");
			
		}
		
		
		sb.append("\r\n" + 
				"if reset_model = '1' then  \r\n" + 
				"");
		if (state.onstart.matches("0")) {
			sb.append(" statevariable_" + state.type +  "_" + state.name + "_next <= (others => '0');\r\n");
		}
		else
		{
			sb.append(" statevariable_" + state.type +  "_" + state.name + "_next <= resize(" + state.onstart +","+ 
		state.integer + "," + state.fraction + ");\r\n");
		}
		sb.append("\r\n" + 
				"else\r\n" + 
				"");
		
		if (currentTemporarySignalID == 1) {
			sb.append(" statevariable_" + state.type +  "_" + state.name + 
					"_next <= statevariable_" + state.type 
			+  "_" + state.name +  "_in;\r\n");
		}
		else {
			sb.append(" statevariable_" + state.type +  "_" + state.name + 
					"_next <= statevariable_" + state.type +  "_" + state.name + "_temp_" + 
					(currentTemporarySignalID - 1) + ";");
		}
		
		
		sb.append("\r\n" + 
				"end if;\r\n" + 
				"");
		sb.append("\r\n" + 
				"end process;\r\n");
		sb.append("\r\n" + 
				"---------------------------------------------------------------------\r\n");
	}
	
	
	static void writeEventPortDriverProcess(StringBuilder sb, EDComponent comp, EDEventPort port,
			int currentPort)
	{
		StringBuilder sensitivityList = new StringBuilder();
		sensitivityList.append("sysparam_time_timestep");
		

		sensitivityList = new StringBuilder();
		sensitivityList.append("sysparam_time_timestep,reset_model");
		int temporarySignalsRequired = 1;
		StringBuilder finalEDEventPort = new StringBuilder();
		finalEDEventPort.append("eventport_" + port.direction + "_" + port.name + 
				"_internal <= " );
		
		for(Iterator<EDEvent> i = comp.events.iterator(); i.hasNext(); ) {
			EDEvent event = i.next();
			for(Iterator<EDEventOut> k = event.events.iterator(); k.hasNext(); ) {
				EDEventOut event2 = k.next(); 
				if (event2.name.matches(port.name))
				{
					if (temporarySignalsRequired > 2) {
						finalEDEventPort.append(" || ");
					}
					finalEDEventPort.append("eventport_" + port.direction +  "_" + port.name 
							+ "_temp_" + temporarySignalsRequired  );
					temporarySignalsRequired++;
					sensitivityList.append(",eventport_in_" + event.name);
				}					
			}
		}
		for(Iterator<EDCondition> i = comp.conditions.iterator(); i.hasNext(); ) {
			EDCondition condition = i.next();
			for(Iterator<EDEventOut> k = condition.events.iterator(); k.hasNext(); ) {
				EDEventOut event = k.next(); 
				if (event.name.matches(port.name))
				{
					if (temporarySignalsRequired > 1) {
						finalEDEventPort.append(" || ");
					}
					finalEDEventPort.append("eventport_" + port.direction +  "_" + port.name 
							+ "_temp_" + temporarySignalsRequired  );
					temporarySignalsRequired++;
					sensitivityList.append("," + condition.sensitivityList);
				}					
			}
		}
		
		if (comp.regimes.size() > 0)
		{
			for(Iterator<EDRegime> l = comp.regimes.iterator(); l.hasNext(); ) {
				EDRegime regime = l.next();
				for(Iterator<EDEvent> i = regime.events.iterator(); i.hasNext(); ) {
					EDEvent event = i.next();
					for(Iterator<EDEventOut> k = event.events.iterator(); k.hasNext(); ) {
						EDEventOut event2 = k.next(); 
						if (event2.name.matches(port.name))
						{
							if (temporarySignalsRequired > 1) {
								finalEDEventPort.append(" || ");
							}
							finalEDEventPort.append("eventport_" + port.direction +  "_" + port.name 
									+ "_temp_" + temporarySignalsRequired  );
							temporarySignalsRequired++;
							sensitivityList.append(",eventport_in_" + event.name);
						}					
					}
				}
				for(Iterator<EDCondition> i = regime.conditions.iterator(); i.hasNext(); ) {
					EDCondition condition = i.next();
					for(Iterator<EDEventOut> k = condition.events.iterator(); k.hasNext(); ) {
						EDEventOut event = k.next(); 
						if (event.name.matches(port.name))
						{
							if (temporarySignalsRequired > 1) {
								finalEDEventPort.append(" || ");
							}
							finalEDEventPort.append("eventport_" + port.direction +  "_" + port.name 
									+ "_temp_" + temporarySignalsRequired  );
							temporarySignalsRequired++;
							sensitivityList.append("," + condition.sensitivityList);
						}					
					}
				}
				
			}				
		}
		
		sb.append("\r\n" + 
				"eventport_driver" +currentPort+ " :process ( " + sensitivityList.toString() + " )\r\n");
		
		//first write as many temp signals as required
		for (int i = 1; i < temporarySignalsRequired + 1; i++)
		{
			sb.append("variable eventport_" + port.direction +  "_" + port.name + 
					"_temp_" + i + " : std_logic;\r\n");
		}
		
		sb.append("\r\n\r\n" + 
				"		begin\r\n" + 
				"		");
		
		int currentVariableID = 1;
		for(Iterator<EDRegime> l = comp.regimes.iterator(); l.hasNext(); ) {
			EDRegime regime = l.next();
			for(Iterator<EDEvent> i = regime.events.iterator(); i.hasNext(); ) {
				EDEvent event = i.next();
				for(Iterator<EDEventOut> k = event.events.iterator(); k.hasNext(); ) {
					EDEventOut event2 = k.next(); 
					if (event2.name.matches(port.name))
					{
						sb.append("if ( current_regime_in_int = " + regime.name + 
								") and eventport_in_" + event.name + " = '1' then\r\n" + 
								"								eventport_" + port.direction +  "_" + port.name + "_temp_" + currentVariableID + " := '1';\r\n" + 
								"							else\r\n" + 
								"								eventport_" + port.direction +  "_" + port.name + "_temp_" + currentVariableID + " := '0';\r\n" + 
								"							end if;");
						currentVariableID++;
					}					
				}
			}
			for(Iterator<EDCondition> i = regime.conditions.iterator(); i.hasNext(); ) {
				EDCondition condition = i.next();
				for(Iterator<EDEventOut> k = condition.events.iterator(); k.hasNext(); ) {
					EDEventOut event = k.next(); 
					if (event.name.matches(port.name))
					{
						sb.append("if ( current_regime_in_int = " + regime.name + 
								") and " + condition.condition + " then\r\n" + 
								"								eventport_" + port.direction +  "_" + port.name + "_temp_" + currentVariableID + " := '1';\r\n" + 
								"							else\r\n" + 
								"								eventport_" + port.direction +  "_" + port.name + "_temp_" + currentVariableID + " := '0';\r\n" + 
								"							end if;");
						currentVariableID++;
					}					
				}
			}
		}
		for(Iterator<EDEvent> i = comp.events.iterator(); i.hasNext(); ) {
			EDEvent event = i.next();
			for(Iterator<EDEventOut> k = event.events.iterator(); k.hasNext(); ) {
				EDEventOut event2 = k.next(); 
				if (event2.name.matches(port.name))
				{
					sb.append("if eventport_in_" + event.name + " = '1' then\r\n" + 
							"								eventport_" + port.direction +  "_" + port.name + "_temp_" + currentVariableID + " := '1';\r\n" + 
							"							else\r\n" + 
							"								eventport_" + port.direction +  "_" + port.name + "_temp_" + currentVariableID + " := '0';\r\n" + 
							"							end if;");
					currentVariableID++;
				}					
			}
		}
		for(Iterator<EDCondition> i = comp.conditions.iterator(); i.hasNext(); ) {
			EDCondition condition = i.next();
			for(Iterator<EDEventOut> k = condition.events.iterator(); k.hasNext(); ) {
				EDEventOut event = k.next(); 
				if (event.name.matches(port.name))
				{
					sb.append("if  " + condition.condition + " then\r\n" + 
							"								eventport_" + port.direction +  "_" + port.name + "_temp_" + currentVariableID + " := '1';\r\n" + 
							"							else\r\n" + 
							"								eventport_" + port.direction +  "_" + port.name + "_temp_" + currentVariableID + " := '0';\r\n" + 
							"							end if;");
					currentVariableID++;
				}					
			}
		}
		if (currentVariableID == 0) {
			finalEDEventPort.append( " '0' ");
		}
		sb.append(finalEDEventPort.toString() + ";\r\n\r\n" + 
				"	end process;\r\n" + 
				"	---------------------------------------------------------------------\r\n" );
		
		 
	}
	
	static void writeStateVariableProcess(StringBuilder sb, EDComponent comp,boolean isTopLevelNeuronModel)
	{
		StringBuilder sensitivityList = new StringBuilder();
		writeDynamicsPreCombProc(sb,comp,sensitivityList);
		writeDynamicsPreSynProc(sb,comp);
		writeDynamicsInstantiationsProc(sb,comp);
		writeDynamicsCombProcess(sb,comp,sensitivityList);
		writeDynamicsSynProcess(sb,comp);
		
		
		
		
		sb.append("\r\n" + 
				"------------------------------------------------------------------------------------------------------\r\n" + 
				"-- EDState Variable Drivers\r\n" + 
				"------------------------------------------------------------------------------------------------------\r\n");
		int currentEDState = 0;
		for(Iterator<EDState> j = comp.state.iterator(); j.hasNext(); ) {
			EDState state = j.next(); 
			writeStateVariableDriverProcess(sb,comp,state,sensitivityList,currentEDState);
			currentEDState++;
		}
		sb.append("\r\n" + 
				"------------------------------------------------------------------------------------------------------\r\n");
		int currentPort = 0;
		for(Iterator<EDEventPort> j = comp.eventports.iterator(); j.hasNext(); ) {
			EDEventPort port = j.next(); 
			if (port.direction.matches("out")) {
				writeEventPortDriverProcess(sb,comp,port,currentPort);
				currentPort++;
			}
		}
		sb.append("\r\n" + 
				"------------------------------------------------------------------------------------------------------\r\n");
		
		sb.append("\r\n" + 
				"---------------------------------------------------------------------\r\n" + 
				"-- Assign state variables to exposures\r\n" + 
				"---------------------------------------------------------------------\r\n");
		
		for(Iterator<EDState> j = comp.state.iterator(); j.hasNext(); ) {
			EDState state = j.next(); 
			if (state.exposure != null) {
			sb.append("exposure_" + state.type +  "_" + state.exposure +
					" <= statevariable_" + state.type + "_" + state.name + "_in;" );
			}
		}
		sb.append("\r\n" + 
				"---------------------------------------------------------------------\r\n" + 
				"");
		
		sb.append("\r\n" + 
				"---------------------------------------------------------------------\r\n" + 
				"-- Assign state variables to output state variables\r\n" + 
				"---------------------------------------------------------------------\r\n" + 
				"");
		
		for(Iterator<EDState> j = comp.state.iterator(); j.hasNext(); ) {
			EDState state = j.next(); 
			sb.append("statevariable_" + state.type +  "_" + state.name + 
					"_out <= statevariable_" + state.type + "_" + state.name + "_next;" );
		}
		sb.append("\r\n" + 
				"---------------------------------------------------------------------\r\n" + 
				"");
		
		sb.append("\r\n" + 
				"---------------------------------------------------------------------\r\n" + 
				"-- Assign derived variables to exposures\r\n" + 
				"---------------------------------------------------------------------\r\n" + 
				"");
		for(Iterator<EDDerivedVariable> j = comp.derivedvariables.iterator(); j.hasNext(); ) {
			EDDerivedVariable derivedvariable = j.next(); 
			if (derivedvariable.exposure != null && derivedvariable.exposure.length() > 0) { 
			sb.append("exposure_" + derivedvariable.type +  "_" + derivedvariable.exposure + 
					" <= derivedvariable_" + derivedvariable.type + "_" + derivedvariable.name + "_in;" );
			sb.append("derivedvariable_" + derivedvariable.type + "_" + derivedvariable.name + 
					"_out <= derivedvariable_" +derivedvariable.type + "_" + derivedvariable.name + ";" );
			}
		}
		for(Iterator<EDConditionalDerivedVariable> j = comp.conditionalderivedvariables.iterator(); j.hasNext(); ) {
			EDConditionalDerivedVariable conditionalderivedvariable = j.next(); 
			if (conditionalderivedvariable.exposure != null && conditionalderivedvariable.exposure.length() > 0) { 
			sb.append("exposure_" + conditionalderivedvariable.type +  "_" + conditionalderivedvariable.exposure + 
					" <= derivedvariable_" + conditionalderivedvariable.type + "_" + conditionalderivedvariable.name + "_in;" );
			sb.append("derivedvariable_" + conditionalderivedvariable.type + "_" + conditionalderivedvariable.name + 
					"_out <= derivedvariable_" +conditionalderivedvariable.type + "_" + conditionalderivedvariable.name + ";" );
			}
		}
		sb.append("\r\n" + 
				"---------------------------------------------------------------------\r\n" + 
				"");
		
		sb.append("\r\n" + 
				"---------------------------------------------------------------------\r\n" + 
				"-- Subprocess ready process\r\n" + 
				"---------------------------------------------------------------------\r\n" + 
				"\r\n" + 
				"subprocess_all_ready_process: process(subprocess_der_int_ready,subprocess_der_int_pre_ready,subprocess_der_ready,subprocess_dyn_int_pre_ready,subprocess_dyn_int_ready,subprocess_dyn_ready,subprocess_model_ready)\r\n" + 
				"begin\r\n" + 
				"if subprocess_der_int_ready = '1'  and subprocess_der_int_pre_ready = '1'and subprocess_der_ready ='1' and subprocess_dyn_int_ready = '1' and subprocess_dyn_int_pre_ready = '1' and subprocess_dyn_ready = '1' and subprocess_model_ready = '1' then\r\n" + 
				"subprocess_all_ready <= '1';\r\n" + 
				"else\r\n" + 
				"subprocess_all_ready <= '0';\r\n" + 
				"end if;\r\n" + 
				"end process subprocess_all_ready_process;\r\n" + 
				"---------------------------------------------------------------------\r\n" + 
				"\r\n" + 
				"\r\n" + 
				"\r\n" + 
				"count_proc:process(clk)\r\n" + 
				"  	begin \r\n" + 
				"  		if (clk'EVENT AND clk = '1') then\r\n" + 
				"			if step_once_go = '1' then\r\n" + 
				"  				COUNT <= \"000\";\r\n" + 
				"				component_done_int <= '0';\r\n" + 
				"  			elsif COUNT = \"001\" then\r\n" + 
				"				component_done_int <= '1';\r\n" + 
				"			elsif subprocess_all_ready = '1' then\r\n" + 
				"  				COUNT <= COUNT + 1;\r\n" + 
				"				component_done_int <= '0';\r\n" + 
				"			end if;\r\n" + 
				"  		end if;\r\n" + 
				"end process count_proc;");
		if (comp.Children.size() == 0)
		{
			sb.append("\r\n" + 
					"component_done <= component_done_int;");
		}
		else
		{
			StringBuilder childCombiner = new StringBuilder();
			StringBuilder childCombiner2 = new StringBuilder();
			int count = 0;
			for(Iterator<EDComponent> j = comp.Children.iterator(); j.hasNext(); ) {
				EDComponent child = j.next(); 
				childCombiner.append(child.name + "_component_done,");
				if (count > 0)
				{
					childCombiner2.append(" and ");
				}
				childCombiner2.append(child.name + "_component_done = '1'");
				count++;
			}
			sb.append("\r\n" + 
					"childrenCombined_component_done_process:process("+childCombiner.toString() +"CLK)\r\n" + 
					"begin\r\n" + 
					"if ("+childCombiner2.toString() +") then\r\n" + 
					"	childrenCombined_component_done <= '1';\r\n" + 
					"else\r\n" + 
					"	childrenCombined_component_done <= '0';\r\n" + 
					"end if;\r\n" + 
					"end process childrenCombined_component_done_process;\r\n" + 
					"component_done <= component_done_int and childrenCombined_component_done;");
		}
		
		
		if (isTopLevelNeuronModel)
		{
			sb.append("\r\n" + 
					"---------------------------------------------------------------------\r\n" + 
					"-- Control the done signal\r\n" + 
					"---------------------------------------------------------------------\r\n" + 
					"\r\n" + 
					"step_once_complete_synch:process(clk)\r\n" + 
					"  	begin \r\n" + 
					"  		if (clk'EVENT AND clk = '1') then\r\n" + 
					"		\r\n" + 
					"		if component_done = '1' and step_once_complete_fired = '0'  then\r\n" + 
					"			step_once_complete <= '1';\r\n" + 
					"			step_once_complete_fired <= '1';\r\n" + 
					"---------------------------------------------------------------------\r\n" + 
					"-- Assign event ports to exposures\r\n" + 
					"---------------------------------------------------------------------\r\n");
			for(Iterator<EDEventPort> j = comp.eventports.iterator(); j.hasNext(); ) {
				EDEventPort port = j.next(); 
				sb.append("eventport_" + port.direction +  "_" + port.name + 
						" <=  eventport_" + port.direction + "_" + port.name + "_internal ;\r\n" );
				
			}
			sb.append("\r\n" + 
					"---------------------------------------------------------------------\r\n" + 
					"	elsif component_done = '0' then\r\n" + 
					"			step_once_complete <= '0';\r\n" + 
					"			step_once_complete_fired <= '0';\r\n" + 
					"		\r\n" + 
					"---------------------------------------------------------------------\r\n" + 
					"-- Assign event ports to exposures\r\n" + 
					"---------------------------------------------------------------------\r\n" + 
					"");
			for(Iterator<EDEventPort> j = comp.eventports.iterator(); j.hasNext(); ) {
				EDEventPort port = j.next(); 
				sb.append("eventport_" + port.direction +  "_" + port.name + 
						" <=  '0';\r\n" );
				
			}
			sb.append("\r\n" + 
					"---------------------------------------------------------------------\r\n" + 
					"		\r\n" + 
					"	else\r\n" + 
					"		step_once_complete <= '0';\r\n" + 
					"		\r\n" + 
					"---------------------------------------------------------------------\r\n" + 
					"-- Assign event ports to exposures\r\n" + 
					"---------------------------------------------------------------------\r\n" + 
					"");
			for(Iterator<EDEventPort> j = comp.eventports.iterator(); j.hasNext(); ) {
				EDEventPort port = j.next(); 
				sb.append("eventport_" + port.direction +  "_" + port.name + 
						" <=  '0';\r\n" );
				
			}
			sb.append("\r\n" + 
					"---------------------------------------------------------------------\r\n" + 
					"		\r\n" + 
					"		end if;\r\n" + 
					"	end if;\r\n" + 
					"  	end process step_once_complete_synch;\r\n" + 
					"---------------------------------------------------------------------\r\n");
		}
		sb.append("\r\n" + 
				"end RTL;\r\n" + 
				"");
		
	}
	
}

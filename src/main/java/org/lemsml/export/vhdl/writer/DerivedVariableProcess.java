package org.lemsml.export.vhdl.writer;

import java.util.Iterator;

import org.lemsml.export.vhdl.edlems.EDCase;
import org.lemsml.export.vhdl.edlems.EDComponent;
import org.lemsml.export.vhdl.edlems.EDConditionalDerivedVariable;
import org.lemsml.export.vhdl.edlems.EDDerivedParameter;
import org.lemsml.export.vhdl.edlems.EDDerivedVariable;
import org.lemsml.export.vhdl.edlems.EDExponential;
import org.lemsml.export.vhdl.edlems.EDPower;
import org.lemsml.export.vhdl.edlems.EDRegime;

public class DerivedVariableProcess {


	static void writeEDDerivedVariableProcess(StringBuilder sb, EDComponent comp)
	{
		StringBuilder sensitivityList = new StringBuilder();
		sensitivityList.append("sysparam_time_timestep");

		for(Iterator<EDDerivedVariable> j = comp.derivedvariables.iterator(); j.hasNext(); ) {
			EDDerivedVariable derivedvariable = j.next(); 
			if (derivedvariable.sensitivityList != null && derivedvariable.sensitivityList.length() > 0)
				sensitivityList.append("," + derivedvariable.sensitivityList);
		}
		for(Iterator<EDConditionalDerivedVariable> j = comp.conditionalderivedvariables.iterator(); j.hasNext(); ) {
			EDConditionalDerivedVariable conditionalderivedvariable = j.next(); 
			if (conditionalderivedvariable.sensitivityList != null && conditionalderivedvariable.sensitivityList.length() > 0)
				sensitivityList.append("," + conditionalderivedvariable.sensitivityList);
		}
		for(Iterator<EDDerivedParameter> j = comp.derivedparameters.iterator(); j.hasNext(); ) {
			EDDerivedParameter derivedparameter = j.next(); 
			if (derivedparameter.sensitivityList != null && derivedparameter.sensitivityList.length() > 0)
				sensitivityList.append("," + derivedparameter.sensitivityList);
		}

		sb.append("\r\n" + 
				"derived_variable_pre_process_comb :process ( " + sensitivityList.toString() + " )\r\n" + 
				"begin \r\n");
		for(Iterator<EDDerivedVariable> j = comp.derivedvariables.iterator(); j.hasNext(); ) {
			EDDerivedVariable derivedvariable = j.next(); 
			for(Iterator<EDExponential> k = derivedvariable.Exponentials.iterator(); k.hasNext(); ) {
				EDExponential exponential = k.next();
				sb.append("pre_exp_" + derivedvariable.name + "_" + exponential.name + "_next <=  resize( " + exponential.value + " ,20, -20);" );
				
			}
			for(Iterator<EDPower> k = derivedvariable.Powers.iterator(); k.hasNext(); ) {
				EDPower power = k.next();
				sb.append("pre_pow_" + derivedvariable.name + "_" + power.name + "_A_next <=  resize( " + power.valueA + " ,20, -20);" );
				sb.append("pre_pow_" + derivedvariable.name + "_" + power.name + "_X_next <=  resize( " + power.valueX + " ,20, -20);" );
				
			}

		}
		for(Iterator<EDConditionalDerivedVariable> j = comp.conditionalderivedvariables.iterator(); j.hasNext(); ) {
			EDConditionalDerivedVariable conditionalderivedvariable = j.next(); 
			for(Iterator<EDCase> l = conditionalderivedvariable.cases.iterator(); l.hasNext(); ) {
				EDCase thisEDCase = l.next(); 
				for(Iterator<EDExponential> k = thisEDCase.Exponentials.iterator(); k.hasNext(); ) {
					EDExponential exponential = k.next();
					sb.append("pre_exp_" + conditionalderivedvariable.name + "_" + exponential.name + "_next <=  resize( " + exponential.value + " ,20, -20);" );
					
				}
				for(Iterator<EDPower> k = thisEDCase.Powers.iterator(); k.hasNext(); ) {
					EDPower power = k.next();
					sb.append("pre_pow_" + conditionalderivedvariable.name + "_" + power.name + "_A_next <=  resize( " + power.valueA + " ,20, -20);" );
					sb.append("pre_pow_" + conditionalderivedvariable.name + "_" + power.name + "_X_next <=  resize( " + power.valueX + " ,20, -20);" );
					
				}
			}

		}
		sb.append("\r\n" + 
				"end process derived_variable_pre_process_comb;\r\n" + 
				"");
		
		sb.append("\r\nderived_variable_pre_process_syn :process ( clk, rst )\r\n" + 
				"begin \r\n" + 
				"");
		for(Iterator<EDDerivedVariable> j = comp.derivedvariables.iterator(); j.hasNext(); ) {
			EDDerivedVariable derivedvariable = j.next(); 
			for(Iterator<EDExponential> k = derivedvariable.Exponentials.iterator(); k.hasNext(); ) {
				EDExponential exponential = k.next();
				sb.append("\r\n" + 
						"	if rst  = '1' then\r\n" + 
						"	pre_exp_" + derivedvariable.name + "_" + exponential.name + " <= to_sfixed(0,20, -20);\r\n" + 
						"	elsif (clk'EVENT AND clk = '1') then	\r\n" + 
						"		if subprocess_all_ready = '1' then\r\n" + 
						"			pre_exp_" + derivedvariable.name + "_" + exponential.name + " <= pre_exp_" + derivedvariable.name + "_" + exponential.name + "_next;\r\n" + 
						"		end if;\r\n" + 
						"	end if;\r\n" + 
						"");
				
			}
			for(Iterator<EDPower> k = derivedvariable.Powers.iterator(); k.hasNext(); ) {
				EDPower power = k.next();
				sb.append("if rst  = '1' then\r\n" + 
						"					pre_pow_" + derivedvariable.name + "_" + power.name + "_A <= to_sfixed(0,20, -20);\r\n" + 
						"					pre_pow_" + derivedvariable.name + "_" + power.name + "_X <= to_sfixed(0,20, -20);\r\n" + 
						"				elsif (clk'EVENT AND clk = '1') then	\r\n" + 
						"					if subprocess_all_ready = '1' then\r\n" + 
						"						pre_pow_" + derivedvariable.name + "_" + power.name + "_A <= pre_pow_" + derivedvariable.name + "_" + power.name + "_A_next ;\r\n" + 
						"						pre_pow_" + derivedvariable.name + "_" + power.name + "_X <= pre_pow_" + derivedvariable.name + "_" + power.name + "_X_next ;\r\n" + 
						"					end if;\r\n" + 
						"				end if;" );
			}

		}
		for(Iterator<EDConditionalDerivedVariable> j = comp.conditionalderivedvariables.iterator(); j.hasNext(); ) {
			EDConditionalDerivedVariable conditionalderivedvariable = j.next(); 
			for(Iterator<EDCase> l = conditionalderivedvariable.cases.iterator(); l.hasNext(); ) {
				EDCase thisEDCase = l.next(); 
				for(Iterator<EDExponential> k = thisEDCase.Exponentials.iterator(); k.hasNext(); ) {
					EDExponential exponential = k.next();
					sb.append("\r\n" + 
							"	if rst  = '1' then\r\n" + 
							"	pre_exp_" + conditionalderivedvariable.name + "_" + exponential.name + " <= to_sfixed(0,20, -20);\r\n" + 
							"	elsif (clk'EVENT AND clk = '1') then	\r\n" + 
							"		if subprocess_all_ready = '1' then\r\n" + 
							"			pre_exp_" + conditionalderivedvariable.name + "_" + exponential.name + " <= pre_exp_" + conditionalderivedvariable.name + "_" + exponential.name + "_next;\r\n" + 
							"		end if;\r\n" + 
							"	end if;\r\n" + 
							"");
				}
				for(Iterator<EDPower> k = thisEDCase.Powers.iterator(); k.hasNext(); ) {
					EDPower power = k.next();
					sb.append("if rst  = '1' then\r\n" + 
							"					pre_pow_" + conditionalderivedvariable.name + "_" + power.name + "_A  <= to_sfixed(0,20, -20);\r\n" + 
							"					pre_pow_" + conditionalderivedvariable.name + "_" + power.name + "_X <= to_sfixed(0,20, -20);\r\n" + 
							"				elsif (clk'EVENT AND clk = '1') then	\r\n" + 
							"					if subprocess_all_ready = '1' then\r\n" + 
							"						pre_pow_" + conditionalderivedvariable.name + "_" + power.name + "_A <= pre_pow_" + conditionalderivedvariable.name + "_" + power.name + "_A_next ;\r\n" + 
							"						pre_pow_" + conditionalderivedvariable.name + "_" + power.name + "_X <= pre_pow_" + conditionalderivedvariable.name + "_" + power.name + "_X_next ;\r\n" + 
							"					end if;\r\n" + 
							"				end if;" );
				}
			}

		}
		sb.append("\r\nsubprocess_der_int_pre_ready <= '1';\r\n" + 
				"end process derived_variable_pre_process_syn;\r\n");
		
		int count_der_int = 0;
		for(Iterator<EDDerivedVariable> j = comp.derivedvariables.iterator(); j.hasNext(); ) {
			EDDerivedVariable derivedvariable = j.next(); 
			for(Iterator<EDExponential> k = derivedvariable.Exponentials.iterator(); k.hasNext(); ) {
				EDExponential exponential = k.next();
				sb.append("\r\n" + 
						"	ParamExp_" + derivedvariable.name + "_" + exponential.name + " : ParamExp \r\n" + 
						"    port map (	clk => clk,\r\n" + 
						"				rst => rst,\r\n" + 
						"				Start => step_once_go,\r\n" + 
						"				Done => subprocess_der_int_ready,\r\n" + 
						"				X => pre_exp_" + derivedvariable.name + "_" + exponential.name + " ,\r\n" + 
						"				Output => exp_" + derivedvariable.name + "_" + exponential.name + "\r\n" + 
						"				);\r\n" + 
						"");
				count_der_int ++;
			}
			for(Iterator<EDPower> k = derivedvariable.Powers.iterator(); k.hasNext(); ) {
				EDPower power = k.next();
				sb.append("	ParamPow_" + derivedvariable.name + "_" + power.name + " : ParamPow \r\n" + 
						"    port map (	clk => clk,\r\n" + 
						"				rst => rst,\r\n" + 
						"				Start => step_once_go,\r\n" + 
						"				Done => subprocess_der_int_ready,\r\n" + 
						"				X => pre_pow_" + derivedvariable.name + "_" + power.name + "_X ,\r\n" + 
						"				A => pre_pow_" + derivedvariable.name + "_" + power.name + "_X ,\r\n" + 
						"				Output => pow_" + derivedvariable.name + "_" + power.name + "\r\n" + 
						"				);\r\n" );
				count_der_int ++;
			}

		}
		for(Iterator<EDConditionalDerivedVariable> j = comp.conditionalderivedvariables.iterator(); j.hasNext(); ) {
			EDConditionalDerivedVariable conditionalderivedvariable = j.next(); 
			for(Iterator<EDCase> l = conditionalderivedvariable.cases.iterator(); l.hasNext(); ) {
				EDCase thisEDCase = l.next(); 
				for(Iterator<EDExponential> k = thisEDCase.Exponentials.iterator(); k.hasNext(); ) {
					EDExponential exponential = k.next();
					sb.append("\r\n" + 
							"	ParamExp_" + thisEDCase.name + "_" + exponential.name + " : ParamExp \r\n" + 
							"    port map (	clk => clk,\r\n" + 
							"				rst => rst,\r\n" + 
							"				Start => step_once_go,\r\n" + 
							"				Done => subprocess_der_int_ready,\r\n" + 
							"				X => pre_exp_" + conditionalderivedvariable.name + "_" + exponential.name + " ,\r\n" + 
							"				Output => exp_" + conditionalderivedvariable.name + "_" + exponential.name + "\r\n" + 
							"				);\r\n" + 
							"");
					count_der_int ++;
				}
				for(Iterator<EDPower> k = thisEDCase.Powers.iterator(); k.hasNext(); ) {
					EDPower power = k.next();
					sb.append("	ParamPow_" + thisEDCase.name + "_" + power.name + " : ParamPow \r\n" + 
							"    port map (	clk => clk,\r\n" + 
							"				rst => rst,\r\n" + 
							"				Start => step_once_go,\r\n" + 
							"				Done => subprocess_der_int_ready,\r\n" + 
							"				X => pre_pow_" + conditionalderivedvariable.name + "_" + power.name + "_X ,\r\n" + 
							"				A => pre_pow_" + conditionalderivedvariable.name + "_" + power.name + "_X ,\r\n" + 
							"				Output => pow_" + conditionalderivedvariable.name + "_" + power.name + "\r\n" + 
							"				);\r\n" );
					count_der_int ++;
				}
			}

		}
		if (count_der_int == 0 )
			sb.append("\r\n" + "subprocess_der_int_ready <= '1';\r\n");
		
		
		
		sb.append("\r\n\r\nderived_variable_process_comb :process ( " + sensitivityList.toString() +" )\r\n" + 
				"begin\r\n");
		for(Iterator<EDDerivedVariable> j = comp.derivedvariables.iterator(); j.hasNext(); ) {
			EDDerivedVariable derivedvariable = j.next(); 
			sb.append("derivedvariable_" + derivedvariable.type +  "_" + derivedvariable.name + "_next <= resize((" + derivedvariable.value + ")," + derivedvariable.integer + "," + derivedvariable.fraction + ");");
		}
		for(Iterator<EDConditionalDerivedVariable> j = comp.conditionalderivedvariables.iterator(); j.hasNext(); ) {
			EDConditionalDerivedVariable conditionalderivedvariable = j.next(); 
			for(Iterator<EDCase> l = conditionalderivedvariable.cases.iterator(); l.hasNext(); ) {
				EDCase thisEDCase = l.next(); 
				sb.append("\r\n\r\nif " + thisEDCase.condition + " then\r\n" + 
					"derivedvariable_" + conditionalderivedvariable.type +  "_" + conditionalderivedvariable.name + "_next <= resize((" + thisEDCase.value.replaceAll("(derivedvariable_[a-zA-Z0-9_]+)", "derivedvariable_$1_next") + ")," + conditionalderivedvariable.integer + "," + conditionalderivedvariable.fraction + ");\r\n" + 
				"end if;\r\n");
			}
		}
		for(Iterator<EDDerivedParameter> j = comp.derivedparameters.iterator(); j.hasNext(); ) {
			EDDerivedParameter derivedparameter = j.next(); 
			sb.append("derivedparameter_" + derivedparameter.type +  "_" + derivedparameter.name + "_next <= resize((" + derivedparameter.value + ")," + derivedparameter.integer + "," + derivedparameter.fraction + ");\r\n");
		}
		sb.append("\r\n" + 
				"subprocess_der_ready <= '1';\r\n" + 
				"end process derived_variable_process_comb;\r\n");
		
		sb.append("\r\n" + 
				"derived_variable_process_syn :process ( clk )\r\n" + 
				"begin \r\n" + 
				"\r\n" + 
				"if clk'event and clk = '1' then  \r\n" + 
				"if subprocess_all_ready = '1' then  \r\n");
		for(Iterator<EDDerivedVariable> j = comp.derivedvariables.iterator(); j.hasNext(); ) {
			EDDerivedVariable derivedvariable = j.next(); 
			sb.append("derivedvariable_" + derivedvariable.type +  "_" + derivedvariable.name + " <= derivedvariable_" + derivedvariable.type +  "_" + derivedvariable.name + "_next;\r\n");
		}
		for(Iterator<EDConditionalDerivedVariable> j = comp.conditionalderivedvariables.iterator(); j.hasNext(); ) {
			EDConditionalDerivedVariable conditionalderivedvariable = j.next(); 
			for(Iterator<EDCase> l = conditionalderivedvariable.cases.iterator(); l.hasNext(); ) {
				EDCase thisEDCase = l.next(); 
				sb.append("derivedvariable_" + conditionalderivedvariable.type +  "_" + conditionalderivedvariable.name + " <= derivedvariable_" + conditionalderivedvariable.type +  "_" + conditionalderivedvariable.name + "_next;\r\n");
			}
		}
		for(Iterator<EDDerivedParameter> j = comp.derivedparameters.iterator(); j.hasNext(); ) {
			EDDerivedParameter derivedparameter = j.next(); 
			sb.append("derivedparameter_" + derivedparameter.type +  "_" + derivedparameter.name + " <= derivedparameter_" + derivedparameter.type +  "_" + derivedparameter.name + "_next;\r\n");
		}
		
		sb.append("end if;\r\n" + 
				"end if;\r\n" + 
				"end process derived_variable_process_syn;\r\n" + 
				"---------------------------------------------------------------------\r\n");
	}
	
}

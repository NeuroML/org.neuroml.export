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


	static void writeEDDerivedVariableProcess(StringBuilder sb, EDComponent comp, boolean useVirtualSynapses)
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
		/*for(Iterator<EDDerivedParameter> j = comp.derivedparameters.iterator(); j.hasNext(); ) {
			EDDerivedParameter derivedparameter = j.next(); 
			if (derivedparameter.sensitivityList != null && derivedparameter.sensitivityList.length() > 0)
				sensitivityList.append("," + derivedparameter.sensitivityList);
		}*/

		sb.append("\r\n" + 
				"derived_variable_pre_process_comb :process ( " + sensitivityList.toString() + " )\r\n" + 
				"begin \r\n");
		for(Iterator<EDDerivedVariable> j = comp.derivedvariables.iterator(); j.hasNext(); ) {
			EDDerivedVariable derivedvariable = j.next(); 
			for(Iterator<EDExponential> k = derivedvariable.Exponentials.iterator(); k.hasNext(); ) {
				EDExponential exponential = k.next();
				sb.append("  pre_exp_" + derivedvariable.name + "_" + exponential.name + "_next <=  resize( " + exponential.value + " ," + exponential.integer+"," +exponential.fraction +");\r\n" );
				
			}
			for(Iterator<EDPower> k = derivedvariable.Powers.iterator(); k.hasNext(); ) {
				EDPower power = k.next();
				sb.append("  pre_pow_" + derivedvariable.name + "_" + power.name + "_A_next <=  resize( " + power.valueA + " ," + power.integer+"," +power.fraction +");\r\n" );
				sb.append("  pre_pow_" + derivedvariable.name + "_" + power.name + "_X_next <=  resize( " + power.valueX + " ," + power.integer+"," +power.fraction +");\r\n" );
				
			}

		}
		for(Iterator<EDConditionalDerivedVariable> j = comp.conditionalderivedvariables.iterator(); j.hasNext(); ) {
			EDConditionalDerivedVariable conditionalderivedvariable = j.next(); 
			for(Iterator<EDCase> l = conditionalderivedvariable.cases.iterator(); l.hasNext(); ) {
				EDCase thisEDCase = l.next(); 
				for(Iterator<EDExponential> k = thisEDCase.Exponentials.iterator(); k.hasNext(); ) {
					EDExponential exponential = k.next();
					sb.append("  pre_exp_" + conditionalderivedvariable.name + "_" + exponential.name + "_next <=  resize( " + exponential.value + " ," + exponential.integer+"," +exponential.fraction +");\r\n" );
					
				}
				for(Iterator<EDPower> k = thisEDCase.Powers.iterator(); k.hasNext(); ) {
					EDPower power = k.next();
					sb.append("  pre_pow_" + conditionalderivedvariable.name + "_" + power.name + "_A_next <=  resize( " + power.valueA + " ," + power.integer+"," +power.fraction +");\r\n" );
					sb.append("  pre_pow_" + conditionalderivedvariable.name + "_" + power.name + "_X_next <=  resize( " + power.valueX + " ," + power.integer+"," +power.fraction +");\r\n" );
					
				}
			}

		}
		sb.append("\r\n" + 
				"end process derived_variable_pre_process_comb;\r\n" + 
				"");
		
		sb.append("\r\nderived_variable_pre_process_syn :process ( clk, init_model )\r\n" + 
				"begin \r\n" + 
				"");
		for(Iterator<EDDerivedVariable> j = comp.derivedvariables.iterator(); j.hasNext(); ) {
			EDDerivedVariable derivedvariable = j.next(); 
			for(Iterator<EDExponential> k = derivedvariable.Exponentials.iterator(); k.hasNext(); ) {
				EDExponential exponential = k.next();
				sb.append("\r\n" + 
						"if (clk'EVENT AND clk = '1') then	\r\n" + 
						"  if init_model  = '1' then\r\n" + 
						"    pre_exp_" + derivedvariable.name + "_" + exponential.name + " <= to_sfixed(0," + exponential.integer+"," +exponential.fraction +");\r\n" + 
						"  else \r\n" +
						"    if subprocess_all_ready_shot = '1' then\r\n" + 
						"      pre_exp_" + derivedvariable.name + "_" + exponential.name + " <= pre_exp_" + derivedvariable.name + "_" + exponential.name + "_next;\r\n" + 
						"    end if;\r\n" + 
						"  end if;\r\n" + 
						"end if;\r\n" + 
						"\r\n");
				
			}
			for(Iterator<EDPower> k = derivedvariable.Powers.iterator(); k.hasNext(); ) {
				EDPower power = k.next();
				sb.append("if (clk'EVENT AND clk = '1') then	\r\n" + 
						"  if init_model  = '1' then\r\n" + 
						"    pre_pow_" + derivedvariable.name + "_" + power.name + "_A <= to_sfixed(0," + power.integer+"," +power.fraction +");\r\n" + 
						"    pre_pow_" + derivedvariable.name + "_" + power.name + "_X <= to_sfixed(0," + power.integer+"," +power.fraction +");\r\n" + 
						"  else \r\n" +
						"    if subprocess_all_ready_shot = '1' then\r\n" + 
						"      pre_pow_" + derivedvariable.name + "_" + power.name + "_A <= pre_pow_" + derivedvariable.name + "_" + power.name + "_A_next ;\r\n" + 
						"      pre_pow_" + derivedvariable.name + "_" + power.name + "_X <= pre_pow_" + derivedvariable.name + "_" + power.name + "_X_next ;\r\n" + 
						"    end if;\r\n" + 
						"  end if;"+ 
						"end if;" );
			}

		}
		for(Iterator<EDConditionalDerivedVariable> j = comp.conditionalderivedvariables.iterator(); j.hasNext(); ) {
			EDConditionalDerivedVariable conditionalderivedvariable = j.next(); 
			for(Iterator<EDCase> l = conditionalderivedvariable.cases.iterator(); l.hasNext(); ) {
				EDCase thisEDCase = l.next(); 
				for(Iterator<EDExponential> k = thisEDCase.Exponentials.iterator(); k.hasNext(); ) {
					EDExponential exponential = k.next();
					sb.append("\r\n" + 
							"if (clk'EVENT AND clk = '1') then	\r\n" + 
							"  if init_model  = '1' then\r\n" + 
							"    pre_exp_" + conditionalderivedvariable.name + "_" + exponential.name + " <= to_sfixed(0," + exponential.integer+"," +exponential.fraction +");\r\n" + 
							"  else \r\n" + 
							"    if subprocess_all_ready_shot = '1' then\r\n" + 
							"      pre_exp_" + conditionalderivedvariable.name + "_" + exponential.name + " <= pre_exp_" + conditionalderivedvariable.name + "_" + exponential.name + "_next;\r\n" + 
							"    end if;\r\n" + 
							"  end if;\r\n" + 
							"end if;\r\n" + 
							"\r\n");
				}
				for(Iterator<EDPower> k = thisEDCase.Powers.iterator(); k.hasNext(); ) {
					EDPower power = k.next();
					sb.append("if (clk'EVENT AND clk = '1') then	\r\n" + 
							"  if init_model  = '1' then\r\n" + 
							"    pre_pow_" + conditionalderivedvariable.name + "_" + power.name + "_A  <= to_sfixed(0," + power.integer+"," +power.fraction +");\r\n" + 
							"    pre_pow_" + conditionalderivedvariable.name + "_" + power.name + "_X <= to_sfixed(0," + power.integer+"," +power.fraction +");\r\n" + 
							"  else \r\n" + 
							"    if subprocess_all_ready_shot = '1' then\r\n" + 
							"      pre_pow_" + conditionalderivedvariable.name + "_" + power.name + "_A <= pre_pow_" + conditionalderivedvariable.name + "_" + power.name + "_A_next ;\r\n" + 
							"      pre_pow_" + conditionalderivedvariable.name + "_" + power.name + "_X <= pre_pow_" + conditionalderivedvariable.name + "_" + power.name + "_X_next ;\r\n" + 
							"    end if;\r\n" + 
							"  end if;\r\n" + 
							"end if;\r\n\r\n" );
				}
			}

		}
		sb.append("  subprocess_der_int_pre_ready <= '1';\r\n" + 
				"end process derived_variable_pre_process_syn;\r\n");
		
		int count_der_int = 0;
		for(Iterator<EDDerivedVariable> j = comp.derivedvariables.iterator(); j.hasNext(); ) {
			EDDerivedVariable derivedvariable = j.next(); 
			for(Iterator<EDExponential> k = derivedvariable.Exponentials.iterator(); k.hasNext(); ) {
				EDExponential exponential = k.next();
				sb.append("\r\n" + 
						"ParamExp_" + derivedvariable.name + "_" + exponential.name + " : ParamExp \r\n" + 
								"generic map( \r\n" + 
								"  BIT_TOP 	=>  " + exponential.integer +",\r\n" + 
								"  BIT_BOTTOM	=> " + exponential.fraction + "\r\n" + 
								")\r\n" + 
						"port map (	clk => clk,\r\n" + 
						"  init_model => init_model,\r\n" + 
						"  Start => step_once_go,\r\n" + 
						"  Done => subprocess_der_int_ready,\r\n" + 
						"  X => pre_exp_" + derivedvariable.name + "_" + exponential.name + " ,\r\n" + 
						"  Output => exp_" + derivedvariable.name + "_" + exponential.name + "\r\n" + 
						");\r\n" + 
						"\r\n");
				count_der_int ++;
			}
			for(Iterator<EDPower> k = derivedvariable.Powers.iterator(); k.hasNext(); ) {
				EDPower power = k.next();
				sb.append("ParamPow_" + derivedvariable.name + "_" + power.name + " : ParamPow \r\n" + 
						"generic map( \r\n" + 
						"  BIT_TOP 	=>  " + power.integer +",\r\n" + 
						"  BIT_BOTTOM	=> " + power.fraction + "\r\n" + 
						")\r\n" + 
						"port map (	clk => clk,\r\n" + 
						"  init_model => init_model,\r\n" + 
						"  Start => step_once_go,\r\n" + 
						"  Done => subprocess_der_int_ready,\r\n" + 
						"  X => pre_pow_" + derivedvariable.name + "_" + power.name + "_X ,\r\n" + 
						"  A => pre_pow_" + derivedvariable.name + "_" + power.name + "_A ,\r\n" + 
						"  Output => pow_" + derivedvariable.name + "_" + power.name + "\r\n" + 
						");\r\n" );
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
							"ParamExp_" + thisEDCase.name + "_" + exponential.name + " : ParamExp \r\n" + 
							"generic map( \r\n" + 
							"  BIT_TOP 	=>  " + exponential.integer +",\r\n" + 
							"  BIT_BOTTOM	=> " + exponential.fraction + "\r\n" + 
							")\r\n" + 
							"port map (	clk => clk,\r\n" + 
							"  init_model => init_model,\r\n" + 
							"  Start => step_once_go,\r\n" + 
							"  Done => subprocess_der_int_ready,\r\n" + 
							"  X => pre_exp_" + conditionalderivedvariable.name + "_" + exponential.name + " ,\r\n" + 
							"  Output => exp_" + conditionalderivedvariable.name + "_" + exponential.name + "\r\n" + 
							");\r\n" + 
							"\r\n");
					count_der_int ++;
				}
				for(Iterator<EDPower> k = thisEDCase.Powers.iterator(); k.hasNext(); ) {
					EDPower power = k.next();
					sb.append("ParamPow_" + thisEDCase.name + "_" + power.name + " : ParamPow \r\n" + 
							"generic map( \r\n" + 
							"  BIT_TOP 	=>  " + power.integer +",\r\n" + 
							"  BIT_BOTTOM	=> " + power.fraction + "\r\n" + 
							")\r\n" + 
							"port map (	clk => clk,\r\n" + 
							"  init_model => init_model,\r\n" + 
							"  Start => step_once_go,\r\n" + 
							"  Done => subprocess_der_int_ready,\r\n" + 
							"  X => pre_pow_" + conditionalderivedvariable.name + "_" + power.name + "_X ,\r\n" + 
							"  A => pre_pow_" + conditionalderivedvariable.name + "_" + power.name + "_A ,\r\n" + 
							"  Output => pow_" + conditionalderivedvariable.name + "_" + power.name + "\r\n" + 
							");\r\n" );
					count_der_int ++;
				}
			}

		}
		if (count_der_int == 0 )
			sb.append("\r\n\r\n--no complex steps in derived variables\r\nsubprocess_der_int_ready <= '1';\r\n");
		
		
		
		sb.append("\r\n\r\nderived_variable_process_comb :process ( " + sensitivityList.toString() +" )\r\n" + 
				"begin\r\n");
		boolean containsDivider = false;
		for(Iterator<EDDerivedVariable> j = comp.derivedvariables.iterator(); j.hasNext(); ) {
			EDDerivedVariable derivedvariable = j.next(); 
			if (!derivedvariable.isEmpty)
			{
				if (derivedvariable.isSynapseSelect && useVirtualSynapses)
				{
					//step_once_clearCurrent
					//sb.append("  "+name+"step_once_addCurrent : in STD_LOGIC;\r\n");
				}
				else
				{
					sb.append("  derivedvariable_" + derivedvariable.type +  "_" + derivedvariable.name + "_next <= resize((" + derivedvariable.value + ")," + derivedvariable.integer + "," + derivedvariable.fraction + ");\r\n");
					if (derivedvariable.value.contains("/"))
						containsDivider = true;
				}
			}
		}
		for(Iterator<EDConditionalDerivedVariable> j = comp.conditionalderivedvariables.iterator(); j.hasNext(); ) {
			EDConditionalDerivedVariable conditionalderivedvariable = j.next(); 
			for(Iterator<EDCase> l = conditionalderivedvariable.cases.iterator(); l.hasNext(); ) {
				EDCase thisEDCase = l.next(); 
				sb.append("\r\n\r\n  if " + thisEDCase.condition + " then\r\n" + 
					"    derivedvariable_" + conditionalderivedvariable.type +  "_" + conditionalderivedvariable.name + "_next <= resize((" + thisEDCase.value + ")," + conditionalderivedvariable.integer + "," + conditionalderivedvariable.fraction + ");\r\n" + 
				"  end if;\r\n");
				if (thisEDCase.value.contains("/"))
					containsDivider = true;
			}
		}
		/*for(Iterator<EDDerivedParameter> j = comp.derivedparameters.iterator(); j.hasNext(); ) {
			EDDerivedParameter derivedparameter = j.next(); 
			sb.append("  derivedparameter_" + derivedparameter.type +  "_" + derivedparameter.name + "_next <= resize((" + derivedparameter.value + ")," + derivedparameter.integer + "," + derivedparameter.fraction + ");\r\n");
		}*/
		if (!containsDivider)
			sb.append("\r\n" + 
					"  subprocess_der_ready <= '1';\r\n");
		sb.append("end process derived_variable_process_comb;\r\n");

		if (containsDivider)
		{
			//add the count process
			sb.append("uut_delayDone_derivedvariable_"+comp.name+" : delayDone GENERIC MAP(\n" + 
					"  Steps => 10\n" + 
					"  )\n" + 
					"PORT MAP(\n" + 
					"  clk => clk,\n" + 
					"  init_model => init_model,\n" + 
					"  Start => step_once_go,\n" + 
					"  Done => subprocess_der_ready\n" + 
					");");
		}
			
		sb.append("\r\n" + 
				"derived_variable_process_syn :process ( clk,init_model )\r\n");
		for(Iterator<EDDerivedVariable> j = comp.derivedvariables.iterator(); j.hasNext(); ) {
			EDDerivedVariable derivedvariable = j.next(); 
			if (!derivedvariable.isEmpty)
			{
				if (derivedvariable.isSynapseSelect && useVirtualSynapses)
				{
					sb.append("      variable derivedvariable_" + derivedvariable.type +  "_" + derivedvariable.name + 
							"_temp : sfixed (" + derivedvariable.integer + " downto " + derivedvariable.fraction + ") :=" +
							" to_sfixed (0," + derivedvariable.integer + " downto " + derivedvariable.fraction + ");\r\n");
					
				}
			}
		}
		sb.append("begin \r\n" + 
				"\r\n" + 
				"if clk'event and clk = '1' then  \r\n");
		if (useVirtualSynapses)
		{
			sb.append("if step_once_clearCurrent = '1' then \r\n");
			for(Iterator<EDDerivedVariable> j = comp.derivedvariables.iterator(); j.hasNext(); ) {
				EDDerivedVariable derivedvariable = j.next(); 
	
				if (!derivedvariable.isEmpty)
				{
					if (derivedvariable.isSynapseSelect && useVirtualSynapses)
					{
						sb.append("      derivedvariable_" + derivedvariable.type +  "_" + derivedvariable.name + 
								" <= resize(0,"+ derivedvariable.integer + "," + derivedvariable.fraction + ");\r\n");
						
					}
				}
			}
			sb.append(" else \r\n");
			for(Iterator<EDDerivedVariable> j = comp.derivedvariables.iterator(); j.hasNext(); ) {
				EDDerivedVariable derivedvariable = j.next(); 
	
				if (!derivedvariable.isEmpty)
				{
					if (derivedvariable.isSynapseSelect && useVirtualSynapses)
					{
						for(int k = 0; k < derivedvariable.items.size(); k++) {
							String item = derivedvariable.items.get(k); 
				
							sb.append("      if " + derivedvariable.itemsParents.get(k) + "_step_once_addCurrent = '1' then\r\n");
							sb.append("        derivedvariable_" + derivedvariable.type +  "_" + derivedvariable.name + 
								"_temp <= derivedvariable_" + derivedvariable.type +  "_" + derivedvariable.name + 
								"_temp + " + item + ";\r\n");
							sb.append("      end if;\r\n");
						}
						sb.append("      derivedvariable_" + derivedvariable.type +  "_" + derivedvariable.name + 
								" <= resize(derivedvariable_" + derivedvariable.type +  "_" + derivedvariable.name + 
								"_temp,"+ derivedvariable.integer + "," + derivedvariable.fraction + ");\r\n");
						
					}
				}
			}
			sb.append(" end if; \r\n");
		}
		/*sb.append("  if init_model = '1' then \r\n");
		for(Iterator<EDDerivedVariable> j = comp.derivedvariables.iterator(); j.hasNext(); ) {
			EDDerivedVariable derivedvariable = j.next(); 

			if (!derivedvariable.isEmpty)
			{
			sb.append("      derivedvariable_" + derivedvariable.type +  "_" + 
			derivedvariable.name + " <= to_sfixed(0.0 ," +
					derivedvariable.integer +"," + derivedvariable.fraction + ");\r\n");
			}
		}
		sb.append(" else \r\n"); */
		sb.append("    if subprocess_all_ready_shot = '1' then  \r\n");
		for(Iterator<EDDerivedVariable> j = comp.derivedvariables.iterator(); j.hasNext(); ) {
			EDDerivedVariable derivedvariable = j.next(); 

			if (!derivedvariable.isEmpty && !(derivedvariable.isSynapseSelect && useVirtualSynapses) )
			{
			sb.append("      derivedvariable_" + derivedvariable.type +  "_" + derivedvariable.name + " <= derivedvariable_" + derivedvariable.type +  "_" + derivedvariable.name + "_next;\r\n");
			}
		}
		for(Iterator<EDConditionalDerivedVariable> j = comp.conditionalderivedvariables.iterator(); j.hasNext(); ) {
			EDConditionalDerivedVariable conditionalderivedvariable = j.next(); 
			for(Iterator<EDCase> l = conditionalderivedvariable.cases.iterator(); l.hasNext(); ) {
				EDCase thisEDCase = l.next(); 
				sb.append("      derivedvariable_" + conditionalderivedvariable.type +  "_" + conditionalderivedvariable.name + " <= derivedvariable_" + conditionalderivedvariable.type +  "_" + conditionalderivedvariable.name + "_next;\r\n");
			}
		}
		/*for(Iterator<EDDerivedParameter> j = comp.derivedparameters.iterator(); j.hasNext(); ) {
			EDDerivedParameter derivedparameter = j.next(); 
			sb.append("      derivedparameter_" + derivedparameter.type +  "_" + derivedparameter.name + " <= derivedparameter_" + derivedparameter.type +  "_" + derivedparameter.name + "_next;\r\n");
		}*/
		
		sb.append("    end if;\r\n" + 
				//"  end if;\r\n" + 
				"end if;\r\n" + 
				"end process derived_variable_process_syn;\r\n" + 
				"---------------------------------------------------------------------\r\n");
	}
	
}

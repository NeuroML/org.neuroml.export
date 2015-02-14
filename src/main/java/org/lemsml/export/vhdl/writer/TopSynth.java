package org.lemsml.export.vhdl.writer;

import java.util.Iterator;

import org.lemsml.export.vhdl.edlems.EDComponent;
import org.lemsml.export.vhdl.edlems.EDConditionalDerivedVariable;
import org.lemsml.export.vhdl.edlems.EDDerivedVariable;
import org.lemsml.export.vhdl.edlems.EDEventConnection;
import org.lemsml.export.vhdl.edlems.EDEventPort;
//import org.lemsml.export.vhdl.edlems.EDExposure;
import org.lemsml.export.vhdl.edlems.EDParameter;
import org.lemsml.export.vhdl.edlems.EDRequirement;
import org.lemsml.export.vhdl.edlems.EDSimulation;
import org.lemsml.export.vhdl.edlems.EDState;

public class TopSynth {

	public static void writeTop(EDSimulation sim, StringBuilder sb)
	{
		//assume one neuron in a test so one top synth
		EDComponent neuron = sim.neuronComponents.get(0);
		sb.append("\r\n" + 
				"library ieee;\r\n" + 
				"use ieee.std_logic_1164.all;\r\n" + 
				"use ieee.std_logic_unsigned.all;\r\n" + 
				"library ieee_proposed;\r\n" + 
				"use ieee_proposed.fixed_pkg.all;\r\n" + 
				"use ieee_proposed.fixed_float_types.ALL;\r\n" + 
				"use std.textio.all;\r\n" + 
				"use ieee.std_logic_textio.all; -- if you're saving this type of signal\r\n" + 
				"use IEEE.numeric_std.all;\r\n" + 
				"\r\n" + 
				"\r\n" + 
				"\r\n" + 
				"\r\n" + 
				"\r\n" + 
				"\r\n" + 
				"\r\n" + 
				"\r\n" + 
				"\r\n" + 
				"\r\n" + 
				"\r\n" + 
				" \r\n" + 
				"entity top_synth is\r\n" + 
				"    Port ( clk : in STD_LOGIC; --SYSTEM CLOCK, THIS ITSELF DOES NOT SIGNIFY TIME STEPS - AKA A SINGLE TIMESTEP MAY TAKE MANY CLOCK CYCLES\r\n" + 
				"           init_model: in STD_LOGIC; --SYNCHRONOUS RESET\r\n" + 
				"		   step_once_go : in STD_LOGIC; --signals to the neuron from the core that a time step is to be simulated\r\n" + 
				"		   step_once_complete : out STD_LOGIC; --signals to the core that a time step has finished\r\n" + 
				"		   eventport_in_spike_aggregate : in STD_LOGIC_VECTOR(511 downto 0);\r\n" + 
				//"		   SelectSpikesIn :  in STD_LOGIC_VECTOR(4607 downto 0);\r\n" + 
				"		   ");

			String name = "";

			for(Iterator<EDEventPort> i = neuron.eventports.iterator(); i.hasNext(); ) {
			    EDEventPort item = i.next();
			    sb.append("			"+neuron.name + "_eventport_" + item.direction +  "_" + item.name + " : " + item.direction + " STD_LOGIC;\r\n"  );
			}
			if (neuron.regimes.size() > 0)
				sb.append("			"+neuron.name + "_current_regimeRESTORE_stdlv : in STD_LOGIC_VECTOR(1 downto 0);\r\n" + 
						  "			"+neuron.name + "_current_regimeCurrent_stdlv : out STD_LOGIC_VECTOR(1 downto 0);\r\n");
			writeEntitySignals(neuron,sb,name,neuron.name + "_");
			sb.append("\r\n" + 
					"           sysparam_time_timestep : sfixed (-6 downto -22);\r\n" + 
					"           sysparam_time_simtime : sfixed (6 downto -22)\r\n" + 
					"		   );\r\n" + 
					"end top_synth;\r\n\r\n" + 
					"architecture top of top_synth is\r\n" + 
					"\r\n" + 
					
					"signal step_once_complete_int : STD_LOGIC;\r\n" + 
					"signal seven_steps_done : STD_LOGIC;\r\n" + 
					"signal step_once_go_int : STD_LOGIC;\r\n" + 
					"signal seven_steps_done_shot_done : STD_LOGIC;\r\n" + 
					"signal seven_steps_done_shot : STD_LOGIC;\r\n" + 
					"signal COUNT : unsigned(2 downto 0) := \"000\";\r\n" + 
					"signal seven_steps_done_next : STD_LOGIC;\r\n" + 
					"signal COUNT_next : unsigned(2 downto 0) := \"000\";\r\n" + 
					"signal step_once_go_int_next : STD_LOGIC;\r\n" + 
					"");
			for(Iterator<EDEventPort> i = neuron.eventports.iterator(); i.hasNext(); ) {
				EDEventPort eventport = i.next(); 
				sb.append("signal " + neuron.name + "_eventport_" + eventport.direction +  "_" + eventport.name + "_int : STD_LOGIC;\r\n");
			}
			
			sb.append("\r\n" + 
					"component "+neuron.name+"\r\n" + 
					"    Port ( clk : in STD_LOGIC; --SYSTEM CLOCK, THIS ITSELF DOES NOT SIGNIFY TIME STEPS - AKA A SINGLE TIMESTEP MAY TAKE MANY CLOCK CYCLES\r\n" + 
					"           init_model: in STD_LOGIC; --SYNCHRONOUS RESET\r\n" + 
					"		   step_once_go : in STD_LOGIC; --signals to the neuron from the core that a time step is to be simulated\r\n" + 
					"		   step_once_complete : out STD_LOGIC; --signals to the core that a time step has finished\r\n" + 
					"		   eventport_in_spike_aggregate : in STD_LOGIC_VECTOR(511 downto 0);\r\n" + 
					//"		   SelectSpikesIn :  in STD_LOGIC_VECTOR(4607 downto 0);\r\n" + 
					"		   ");
			name = "";
			for(Iterator<EDEventPort> i = neuron.eventports.iterator(); i.hasNext(); ) {
			    EDEventPort item = i.next();
			    sb.append("			eventport_" + item.direction +  "_" + item.name + " : " + item.direction + " STD_LOGIC;\r\n"  );
			}

			if (neuron.regimes.size() > 0)
				sb.append("			current_regime_in_stdlv : in STD_LOGIC_VECTOR(1 downto 0);\r\n" + 
						  "			current_regime_out_stdlv : out STD_LOGIC_VECTOR(1 downto 0);\r\n");
			Entity.writeEntitySignals(neuron,sb,name,"");
			sb.append("\r\n" + 
					"           sysparam_time_timestep : sfixed (-6 downto -22);\r\n" + 
					"           sysparam_time_simtime : sfixed (6 downto -22)\r\n" + 
					"		   );\r\n" + 
					"end component;\r\n" + 
					"\r\n" + 
					"\r\n" + 
					"	\r\n" + 
					"	");
			
			if (neuron.regimes.size() > 0)
			{
				sb.append("signal " + neuron.name + "_current_regime_in_stdlv_int :  STD_LOGIC_VECTOR(1 downto 0);\r\n");
				sb.append("signal " + neuron.name + "_current_regime_out_stdlv_int :  STD_LOGIC_VECTOR(1 downto 0);\r\n");
			}
			writeStateSignals(neuron,sb,"",neuron.name);
			
			sb.append("\r\n" + 
					"begin\r\n" + 
					"\r\n" + 
					"\r\n" +
					neuron.name+"_uut : "+ neuron.name+"\r\n" + 
					"    port map (	clk => clk,\r\n" + 
					"				init_model=> init_model,\r\n" + 
					"		   step_once_go  => step_once_go_int,\r\n" + 
					"		   step_once_complete  => step_once_complete_int,\r\n" + 
					"		   eventport_in_spike_aggregate => eventport_in_spike_aggregate,\r\n" + 
					//"		   SelectSpikesIn => SelectSpikesIn,\r\n" + 
					"		   ");

			int count = 0;
			for(Iterator<EDEventPort> i = neuron.eventports.iterator(); i.hasNext(); ) {
			    EDEventPort item = i.next();
			    sb.append("			eventport_" + item.direction +  "_" +name+ item.name + " => " +
			    		neuron.name + "_eventport_" + item.direction + "_" +name+  item.name + "_int ,\r\n");
			    count++;
			}
			writeNeuronComponentMap(neuron,sb,"",neuron.name);
			sb.append("\r\n" + 
					"           sysparam_time_timestep => sysparam_time_timestep,\r\n" + 
					"           sysparam_time_simtime => sysparam_time_simtime\r\n" + 
					"		   );\r\n" + 
					"		");
			
			sb.append("\r\n" + 
					"\r\n" + 
					"count_proc_comb:process(init_model,step_once_complete_int,COUNT,step_once_go)\r\n" + 
					"  	begin \r\n" + 
					"		seven_steps_done_next <= '0';\r\n" + 
					"		COUNT_next <= COUNT;\r\n" + 
					"		step_once_go_int_next <= '0';\r\n" + 
					"			if (init_model='1') then \r\n" + 
					"				seven_steps_done_next <= '0';\r\n" + 
					"				COUNT_next <= \"110\";\r\n" + 
					"		        step_once_go_int_next <= '0';\r\n" + 
					"			else\r\n" + 
					"				if step_once_complete_int = '1'	then\r\n" + 
					"					if (COUNT = \"110\") then\r\n" + 
					"						seven_steps_done_next <= '1';\r\n" + 
					"					    COUNT_next <= \"110\";\r\n" + 
					"						step_once_go_int_next <= '0';\r\n" + 
					"					else\r\n" + 
					"						seven_steps_done_next <= '0';\r\n" + 
					"						COUNT_next <= COUNT + 1;\r\n" + 
					"						step_once_go_int_next <= '1';\r\n" + 
					"					end if;\r\n" + 
					"				elsif step_once_go = '1' then\r\n" + 
					"					seven_steps_done_next <= '0';\r\n" +
					"					COUNT_next <= \"000\";\r\n" +  
					"					step_once_go_int_next <= '1';\r\n" + 
					"				else\r\n" + 
					"		            seven_steps_done_next <= '0';\r\n" + 
					"		            COUNT_next <= COUNT;\r\n" + 
					"					step_once_go_int_next <= '0';\r\n" + 
					"				end if;\r\n" + 
					"			end if;\r\n" + 
					"end process count_proc_comb;\r\n" + 
					"\r\n" + 
					"\r\n" + 
					"\r\n" + 
					"count_proc_syn:process(clk)\r\n" + 
					"  	begin \r\n" + 
					"      if rising_edge(clk) then  \r\n" + 
					"		 if init_model = '1' then\n" + 
					"		    COUNT <= \"110\";\n" + 
					"			 seven_steps_done <= '1';\n" + 
					"			 step_once_go_int <= '0';\n" + 
					"		  else\n" + 
					"		    COUNT <= COUNT_next;\n" + 
					"		    seven_steps_done <= seven_steps_done_next;\n" + 
					"		    step_once_go_int <= step_once_go_int_next;\n" + 
					"		  end if;" +
					"		end if;\r\n" + 
					"end process count_proc_syn;\r\n" + 
					"\r\n" + 
					"\r\n" + 
					"\r\n" + 
					"shot_process:process(clk)\r\n" + 
					"begin\r\n" + 
					"	if rising_edge(clk) then\r\n" + 
					"			if (init_model='1') then \r\n" + 
					"				seven_steps_done_shot <= '0';\r\n" + 
					"				seven_steps_done_shot_done <= '1';\r\n" + 
					"			else\r\n" + 
					"				if seven_steps_done = '1' and seven_steps_done_shot_done = '0' then\r\n" + 
					"					seven_steps_done_shot <= '1';\r\n" + 
					"					seven_steps_done_shot_done <= '1';\r\n" + 
					"				elsif seven_steps_done_shot = '1' then\r\n" + 
					"					seven_steps_done_shot <= '0';\r\n" + 
					"				elsif seven_steps_done = '0' then\r\n" + 
					"					seven_steps_done_shot <= '0';\r\n" + 
					"					seven_steps_done_shot_done <= '0';\r\n" + 
					"				end if;\r\n" + 
					"			end if;\r\n" + 
					"	end if;\r\n" + 
					"\r\n" + 
					"end process shot_process;\r\n" + 
					"\r\n" + 
					"");
			
			sb.append("\r\n" + 
					"store_state: process (clk)\r\n" + 
					"   begin\r\n" + 
					"      if rising_edge(clk) then  \r\n" + 
					"         if (init_model='1') then   \r\n" + 
					"		\r\n" + 
					"");
			
			writeResetState(neuron,sb,"",neuron.name);
			for(Iterator<EDEventPort> i = neuron.eventports.iterator(); i.hasNext(); ) {
			    EDEventPort item = i.next();
			    sb.append("			"+neuron.name + "_eventport_" + item.direction + "_" +name+  item.name + " <= '0';\r\n");
			}
			sb.append("\r\n" + 
					"		 \r\n" + 
					"         elsif (seven_steps_done_shot='1') then\r\n" + 
					"");
			for(Iterator<EDEventPort> i = neuron.eventports.iterator(); i.hasNext(); ) {
			    EDEventPort item = i.next();
			    sb.append("			"+neuron.name + "_eventport_" + item.direction + "_" +name+  item.name + " <= "+
			    neuron.name + "_eventport_" + item.direction + "_" +name+  item.name + "_int ;\r\n");
			}
			writeStepsDoneState(neuron,sb,"",neuron.name);
			sb.append("\r\n" + 
					"else\r\n" + 
					"");
			for(Iterator<EDEventPort> i = neuron.eventports.iterator(); i.hasNext(); ) {
			    EDEventPort item = i.next();
			    sb.append("			"+neuron.name + "_eventport_" + item.direction + "_" +name+  item.name + " <= '0';\r\n");
			}
			sb.append("		\r\n" + 
					"end if;\r\n" + 
					"end if;\r\n" + 
					"    \r\n" + 
					"\r\n" + 
					" end process store_state;\r\n" + 
					" ");
			
			
			writeCurrentStateAssignments(neuron,sb,"",neuron.name);
			
			
			sb.append("\r\n" + 
					" step_once_complete <= seven_steps_done_shot;\r\n" + 
					"\r\n" + 
					"end top;");
	}
	
	
	public static void writeEntitySignals(EDComponent comp, StringBuilder sb, String name, String prepend)
	{

		for(Iterator<EDParameter> i = comp.parameters.iterator(); i.hasNext(); ) {
			EDParameter item = i.next(); 
			sb.append("			"+prepend+"param_" + item.type +  "_" + name + item.name + " : in sfixed (" + item.integer + " downto " + item.fraction + ");\r\n"  );
		}

		for(Iterator<EDDerivedVariable> j = comp.derivedvariables.iterator(); j.hasNext(); ) {
			EDDerivedVariable dv = j.next(); 
			if (dv.exposure!= null && dv.exposure.length() > 0 && 
					(dv.ExposureIsUsed || dv.IsUsedForOtherDerivedVariables))
			{
				sb.append("			"+prepend+"exposure_" + dv.type +  "_" + name + dv.name + 
						" : out sfixed (" + dv.integer + " downto " + dv.fraction + ");\r\n"  );
			}
		}
		for(Iterator<EDConditionalDerivedVariable> j = comp.conditionalderivedvariables.iterator(); j.hasNext(); ) {
			EDConditionalDerivedVariable dv = j.next(); 
			if (dv.exposure!= null && dv.exposure.length() > 0 && 
					(dv.ExposureIsUsed || dv.IsUsedForOtherDerivedVariables))
			{
				sb.append("			"+prepend+"exposure_" + dv.type +  "_" + name + dv.name + 
						" : out sfixed (" + dv.integer + " downto " + dv.fraction + ");\r\n"  );
			}	
		}
		for(Iterator<EDState> j = comp.state.iterator(); j.hasNext(); ) {
			EDState dv = j.next(); 
			if (dv.exposure!= null && dv.exposure.length() > 0)
			{
				sb.append("			"+prepend+"exposure_" + dv.type +  "_" + name + dv.name + 
						" : out sfixed (" + dv.integer + " downto " + dv.fraction + ");\r\n"  );
			}	
		}

		for(Iterator<EDState> i = comp.state.iterator(); i.hasNext(); ) {
			EDState item = i.next(); 
			sb.append("			"+prepend+"stateCURRENT_" + item.type +  "_" + name + item.name + " : out sfixed (" + item.integer + " downto " + item.fraction + ");\r\n"  );
			sb.append("			"+prepend+"stateRESTORE_" + item.type +  "_" + name + item.name + " : in sfixed (" + item.integer + " downto " + item.fraction + ");\r\n"  );
		}
		for(Iterator<EDDerivedVariable> i = comp.derivedvariables.iterator(); i.hasNext(); ) {
			EDDerivedVariable item = i.next(); 
			if (item.IsUsedForOtherDerivedVariables || item.ExposureIsUsed){
				sb.append("			"+prepend+"stateCURRENT_" + item.type +  "_" + name + item.name + " : out sfixed (" + item.integer + " downto " + item.fraction + ");\r\n"  );
				sb.append("			"+prepend+"stateRESTORE_" + item.type +  "_" + name + item.name + " : in sfixed (" + item.integer + " downto " + item.fraction + ");\r\n"  );
			}
		}
		for(Iterator<EDConditionalDerivedVariable> i = comp.conditionalderivedvariables.iterator(); i.hasNext(); ) {
			EDConditionalDerivedVariable item = i.next(); 
			if (item.IsUsedForOtherDerivedVariables || item.ExposureIsUsed){
				sb.append("			"+prepend+"stateCURRENT_" + item.type +  "_" + name + item.name + " : out sfixed (" + item.integer + " downto " + item.fraction + ");\r\n"  );
				sb.append("			"+prepend+"stateRESTORE_" + item.type +  "_" + name + item.name + " : in sfixed (" + item.integer + " downto " + item.fraction + ");\r\n"  );
			}
		}
		
		for(Iterator<EDComponent> i = comp.Children.iterator(); i.hasNext(); ) {
			EDComponent item = i.next(); 
			String newName = name + item.name + "_";
			 writeEntitySignals(item, sb, newName,prepend);
		}
	
	}

	public static void writeStateSignals(EDComponent comp, StringBuilder sb, String name,String parentName)
	{
		for(Iterator<EDState> i = comp.state.iterator(); i.hasNext(); ) {
			EDState state = i.next(); 
			sb.append("signal " + parentName + "_statevariable_" + state.type +  "_" +  name+state.name + "_out_int : sfixed (" + 
					state.integer + " downto " + state.fraction + ");");
			sb.append("signal " + parentName + "_statevariable_" + state.type +  "_" +  name+state.name + "_in_int : sfixed (" + 
					state.integer + " downto " + state.fraction + ");");
		}
		for(Iterator<EDDerivedVariable> i = comp.derivedvariables.iterator(); i.hasNext(); ) {
			EDDerivedVariable state = i.next(); 
			if (state.IsUsedForOtherDerivedVariables || state.ExposureIsUsed){
				sb.append("signal " + parentName + "_derivedvariable_" + state.type +  "_" +  name+state.name + "_out_int : sfixed (" + 
						state.integer + " downto " + state.fraction + ");");
				sb.append("signal " + parentName + "_derivedvariable_" + state.type +  "_" +  name+state.name + "_in_int : sfixed (" + 
						state.integer + " downto " + state.fraction + ");");
			}
		}
		for(Iterator<EDConditionalDerivedVariable> i = comp.conditionalderivedvariables.iterator(); i.hasNext(); ) {
			EDConditionalDerivedVariable state = i.next(); 
			if (state.IsUsedForOtherDerivedVariables || state.ExposureIsUsed){
				sb.append("signal " + parentName + "_derivedvariable_" + state.type +  "_" +  name+state.name + "_out_int : sfixed (" + 
						state.integer + " downto " + state.fraction + ");");
				sb.append("signal " + parentName + "_derivedvariable_" + state.type +  "_" +  name+state.name + "_in_int : sfixed (" + 
						state.integer + " downto " + state.fraction + ");");
			}
		}

		for(Iterator<EDComponent> i = comp.Children.iterator(); i.hasNext(); ) {
			EDComponent item = i.next(); 
			String newName = name + item.name + "_";
			writeStateSignals(item, sb, newName,parentName);
		}
	
	}
	

	private static void writeCurrentStateAssignments(EDComponent comp, StringBuilder sb, String name, String parentName)
	{
		if (comp.regimes.size() > 0)
		{
		    sb.append("			"+parentName + "_current_" + name+ "regimeCurrent_stdlv <= " + parentName + "_current_regime_in_stdlv_int;\r\n");
		}
		for(Iterator<EDState> i = comp.state.iterator(); i.hasNext(); ) {
			EDState item = i.next(); 
			sb.append("			"+  parentName + "_stateCURRENT_" + item.type+   "_" +name+ item.name + " <= " +  
					parentName + "_statevariable_" + item.type+   "_" +name+ item.name + "_in_int;\r\n" );
		}
		
		for(Iterator<EDDerivedVariable> i = comp.derivedvariables.iterator(); i.hasNext(); ) {
			EDDerivedVariable item = i.next(); 
			if (item.IsUsedForOtherDerivedVariables || item.ExposureIsUsed){
				sb.append("			"+  parentName + "_stateCURRENT_" + item.type+   "_" +name+ item.name + " <= " +  
						parentName + "_derivedvariable_" + item.type+   "_" +name+ item.name + "_in_int;\r\n" );
			}
		}
		for(Iterator<EDConditionalDerivedVariable> i = comp.conditionalderivedvariables.iterator(); i.hasNext(); ) {
			EDConditionalDerivedVariable item = i.next(); 
			if (item.IsUsedForOtherDerivedVariables || item.ExposureIsUsed){
				sb.append("			"+  parentName + "_stateCURRENT_" + item.type+   "_" +name+ item.name + " <= " +  
						parentName + "_derivedvariable_" + item.type+   "_" +name+ item.name + "_in_int;\r\n" );
			}
		}
		for(Iterator<EDComponent> i = comp.Children.iterator(); i.hasNext(); ) {
			EDComponent item = i.next(); 
			String newName = name  + item.name + "_";
			writeCurrentStateAssignments(item, sb, newName, parentName);
		}
	}
	

	private static void writeStepsDoneState(EDComponent comp, StringBuilder sb, String name, String parentName)
	{
		
		if (comp.regimes.size() > 0)
		{
		    sb.append("			" +parentName + "_current_regime_in_stdlv_int <= "+ parentName + "_current_regime_out_stdlv_int;\r\n");
		}		

		for(Iterator<EDState> i = comp.state.iterator(); i.hasNext(); ) {
			EDState item = i.next(); 
			sb.append("			"+  parentName + "_statevariable_" + item.type+   "_" +name+ item.name + "_in_int <= " +  
					parentName + "_statevariable_" + item.type+   "_" +name+ item.name + "_out_int;\r\n" );
		}
		
		for(Iterator<EDDerivedVariable> i = comp.derivedvariables.iterator(); i.hasNext(); ) {
			EDDerivedVariable item = i.next(); 
			if (item.IsUsedForOtherDerivedVariables || item.ExposureIsUsed){
				sb.append("			"+  parentName + "_derivedvariable_" + item.type+   "_" +name+ item.name + "_in_int <= " +  
						parentName + "_derivedvariable_" + item.type+   "_" +name+ item.name + "_out_int;\r\n" );
			}
		}
		for(Iterator<EDConditionalDerivedVariable> i = comp.conditionalderivedvariables.iterator(); i.hasNext(); ) {
			EDConditionalDerivedVariable item = i.next(); 
			if (item.IsUsedForOtherDerivedVariables || item.ExposureIsUsed){
				sb.append("			"+  parentName + "_derivedvariable_" + item.type+   "_" +name+ item.name + "_in_int <= " +  
						parentName + "_derivedvariable_" + item.type+   "_" +name+ item.name + "_out_int;\r\n" );
			}
		}
		for(Iterator<EDComponent> i = comp.Children.iterator(); i.hasNext(); ) {
			EDComponent item = i.next(); 
			String newName = name  + item.name + "_";
			writeStepsDoneState(item, sb, newName, parentName);
		}
	}
	
	private static void writeResetState(EDComponent comp, StringBuilder sb, String name, String parentName)
	{
		int count = 0;
		
		if (comp.regimes.size() > 0)
		{
		    sb.append("			" + parentName + "_current_regime_in_stdlv_int <= " + parentName + "_current_regimeRESTORE_stdlv;\r\n");
		}		

		for(Iterator<EDState> i = comp.state.iterator(); i.hasNext(); ) {
			EDState item = i.next(); 
			sb.append("			"+  parentName + "_statevariable_" + item.type+   "_" +name+ item.name + "_in_int <= " +  parentName +
					"_stateRESTORE_" + item.type+   "_" +name+ item.name + ";\r\n" );
		}
		
		for(Iterator<EDDerivedVariable> i = comp.derivedvariables.iterator(); i.hasNext(); ) {
			EDDerivedVariable item = i.next(); 
			if (item.IsUsedForOtherDerivedVariables || item.ExposureIsUsed){
				sb.append("			"+  parentName + "_derivedvariable_" + item.type+   "_" +name+ item.name + "_in_int <= " +  parentName +
						"_stateRESTORE_" + item.type+   "_" +name+ item.name + ";\r\n" );
			}
		}
		for(Iterator<EDConditionalDerivedVariable> i = comp.conditionalderivedvariables.iterator(); i.hasNext(); ) {
			EDConditionalDerivedVariable item = i.next(); 
			if (item.IsUsedForOtherDerivedVariables || item.ExposureIsUsed){
				sb.append("			"+  parentName + "_derivedvariable_" + item.type+   "_" +name+ item.name + "_in_int <= " +  parentName +
						"_stateRESTORE_" + item.type+   "_" +name+ item.name + ";\r\n" );
			}
		}
		for(Iterator<EDComponent> i = comp.Children.iterator(); i.hasNext(); ) {
			EDComponent item = i.next(); 
			String newName = name  + item.name + "_";
			writeResetState(item, sb, newName, parentName);
		}
		
	
	}
	

	private static void writeNeuronComponentMap(EDComponent comp, StringBuilder sb, String name, String parentName)
	{
		
		if (comp.regimes.size() > 0)
		{
		    sb.append("			current_regime_in_stdlv => " + comp.name + "_current_regime_in_stdlv_int,\r\n");
		    sb.append("			current_regime_out_stdlv => " + comp.name + "_current_regime_out_stdlv_int,\r\n");
		}

		for(Iterator<EDParameter> i = comp.parameters.iterator(); i.hasNext(); ) {
			EDParameter item = i.next(); 
			sb.append("			param_" + item.type +  "_" +name+  item.name + " => " +
					parentName + "_param_" + item.type + "_" +name+  item.name + " ,\r\n" );
		}
		

		for(Iterator<EDState> i = comp.state.iterator(); i.hasNext(); ) {
			EDState item = i.next(); 
			sb.append("			statevariable_" + item.type +  "_" + name+ item.name + "_out => " + parentName +
					"_statevariable_" + item.type+   "_" +name+ item.name + "_out_int,\r\n"  );
			sb.append("			statevariable_" + item.type +  "_" + name+ item.name + "_in => " + parentName +
					"_statevariable_" + item.type+   "_" +name+ item.name + "_in_int,\r\n"  );
		}
		
		for(Iterator<EDDerivedVariable> i = comp.derivedvariables.iterator(); i.hasNext(); ) {
			EDDerivedVariable item = i.next(); 
			if (item.IsUsedForOtherDerivedVariables || item.ExposureIsUsed){
				sb.append("			derivedvariable_" + item.type +  "_" + name+ item.name + "_out => " + parentName +
						"_derivedvariable_" + item.type+   "_" +name+ item.name + "_out_int, \r\n" );
				sb.append("			derivedvariable_" + item.type +  "_" + name+ item.name + "_in => " + parentName +
						"_derivedvariable_" + item.type+   "_" +name+ item.name + "_in_int,\r\n"  );
			}
		}
		for(Iterator<EDConditionalDerivedVariable> i = comp.conditionalderivedvariables.iterator(); i.hasNext(); ) {
			EDConditionalDerivedVariable item = i.next(); 
			if (item.IsUsedForOtherDerivedVariables || item.ExposureIsUsed){
				sb.append("			derivedvariable_" + item.type +  "_" + name+ item.name + "_out => " + parentName +
						"_derivedvariable_" + item.type+   "_" +name+ item.name + "_out_int,\r\n"  );
				sb.append("			derivedvariable_" + item.type +  "_" + name+ item.name + "_in => " + parentName +
						"_derivedvariable_" + item.type+   "_" +name+ item.name + "_in_int,\r\n"  );
			}
		}
		
		for(Iterator<EDComponent> i = comp.Children.iterator(); i.hasNext(); ) {
			EDComponent item = i.next(); 
			String newName = name  + item.name + "_";
			writeNeuronComponentMap(item, sb, newName, parentName);
		}
		
	
	}
}

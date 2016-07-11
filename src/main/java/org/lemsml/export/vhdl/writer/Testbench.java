package org.lemsml.export.vhdl.writer;

import java.util.Iterator;
import java.util.Map;

import org.lemsml.export.vhdl.edlems.EDComponent;
import org.lemsml.export.vhdl.edlems.EDConditionalDerivedVariable;
import org.lemsml.export.vhdl.edlems.EDDerivedVariable;
import org.lemsml.export.vhdl.edlems.EDEventConnection;
import org.lemsml.export.vhdl.edlems.EDEventPort;
import org.lemsml.export.vhdl.edlems.EDParameter;
import org.lemsml.export.vhdl.edlems.EDRequirement;
import org.lemsml.export.vhdl.edlems.EDSimulation;
import org.lemsml.export.vhdl.edlems.EDState;

public class Testbench {

	public static void writeTestBench(EDSimulation sim, StringBuilder sb, 
			Map<String,Float> initialState, boolean useSynapseMux)
	{
		sb.append("\r\n" + 
				"library ieee;\r\n" + 
				"use ieee.std_logic_1164.all;\r\n" + 
				"use ieee.std_logic_unsigned.all;\r\n" + 
				"library ieee_proposed;\r\n" + 
				"use ieee_proposed.fixed_pkg.all;\r\n" + 
				"use ieee_proposed.fixed_float_types.ALL;\r\n" + 
				"use std.textio.all;\r\n" + 
				"use ieee.std_logic_textio.all; -- if you're saving this type of signal\r\n" + 
				"entity tb_simulation is\r\n" + 
				"end tb_simulation;\r\n" + 
				"\r\n" + 
				"\r\n" + 
				"architecture tb of tb_simulation is\r\n" + 
				"\r\n" + 
				"FILE test_out_data: TEXT open WRITE_MODE is \"VHDLoutput.csv\";");
		
		for(Iterator<EDComponent> z = sim.neuronComponents.iterator(); z.hasNext(); ) {
			EDComponent child = z.next(); 
			sb.append("component top_synth \r\n" + 
			"    Port (\r\n" + 
			"		   clk : in STD_LOGIC; --SYSTEM CLOCK, THIS ITSELF DOES NOT SIGNIFY TIME STEPS - AKA A SINGLE TIMESTEP MAY TAKE MANY CLOCK CYCLES\r\n" + 
			"          init_model : in STD_LOGIC; --SYNCHRONOUS RESET\r\n" + 
			"		   step_once_go : in STD_LOGIC; --signals to the neuron from the core that a time step is to be simulated\r\n" + 
			"		   step_once_complete : out STD_LOGIC; --signals to the core that a time step has finished\r\n" + 
			"		   eventport_in_spike_aggregate : in STD_LOGIC_VECTOR(511 downto 0);\r\n");
			if (useSynapseMux) {
				sb.append("		   SelectSpikesIn :  in STD_LOGIC_VECTOR(4607 downto 0); ");
			}
	
			String name = "";

			for(Iterator<EDEventPort> i = child.eventports.iterator(); i.hasNext(); ) {
			    EDEventPort item = i.next();
			    sb.append("			"+child.name + "_eventport_" + item.direction +  "_" + item.name + " : " + item.direction + " STD_LOGIC;\r\n"  );
			}
			if (child.regimes.size() > 0)
				sb.append("			"+child.name + "_current_regimeRESTORE_stdlv : in STD_LOGIC_VECTOR(1 downto 0);\r\n" + 
						  "			"+child.name + "_current_regimeCurrent_stdlv : out STD_LOGIC_VECTOR(1 downto 0);\r\n");
			
			TopSynth.writeEntitySignals(child,sb,name,child.name + "_");
			sb.append("           sysparam_time_timestep : in sfixed (-6 downto -22);\r\n" + 
				  "           sysparam_time_simtime : in sfixed (6 downto -22)\r\n" + 
				  "	);\r\n" + 
				"end component;\r\n");
			
		}

		sb.append("\r\n" +
				"\r\n" + 
				"	signal clk 			: std_logic := '0';\r\n" + 
				"	signal eog 			: std_logic := '0';\r\n" + 
				"	signal init_model 			: std_logic := '1';\r\n" + 
				"	signal step_once_go 			: std_logic := '0';\r\n" + 
				"	\r\n" + 
				"	signal step_once_complete 			: std_logic := '0';\r\n" + 
				"	signal eventport_in_spike_aggregate : STD_LOGIC_VECTOR(511 downto 0);\r\n");
				
				if (useSynapseMux) {
					sb.append("	signal SelectSpikesIn : STD_LOGIC_VECTOR(4607 downto 0);\r\n");
				}
				sb.append("	signal sysparam_time_simtime  : sfixed ( 6 downto -22) := to_sfixed (0.0,6 , -22);\r\n" + 
				"	signal Errors		: integer;\r\n" + 
				"	signal sysparam_time_timestep : sfixed (-6 downto -22) := to_sfixed( " + sim.dt + " ,-6,-22);\r\n" + 
				"	");
		
		for(Iterator<EDComponent> z = sim.neuronComponents.iterator(); z.hasNext(); ) {
			EDComponent child = z.next(); 
			writeStateSignals(child,sb,"",child.name);
			if (child.regimes.size() > 0)
				sb.append("			signal "+child.name + "_current_regimeCurrent_stdlv_internal : STD_LOGIC_VECTOR(1 downto 0);\r\n");
			
		}
		
		sb.append("\r\n" + 
				"file stimulus: TEXT open read_mode is \"stimulus.csv\";\r\n" + 
				"\r\n" + 
				"begin\r\n");

				if (useSynapseMux) {
					sb.append("SelectSpikesIn <= \"111111111111111110111111101111111100111111" + 
				"01111111101011111100111111100011111011111111011011111010111111010011111001111" + 
				"1110010111110001111110000111101111111101110111101101111101100111101011111101" + 
				"01011110100111110100011110011111110011011110010111110010011110001111110001011" + 
				"110000111110000011101111111101111011101110111101110011101101111101101011101100" + 
				"1111011000111010111111010110111010101111010100111010011111010010111010001111010" + 
				"0001110011111110011101110011011110011001110010111110010101110010011110010001110001" + 
				"11111000110111000101111000100111000011111000010111000001111000000110111111110111110" + 
				"110111101110111100110111011110111010110111001110111000110110111110110110110110101110" + 
				"1101001101100111101100101101100011101100001101011111101011101101011011101011001101010" + 
				"11110101010110101001110101000110100111110100110110100101110100100110100011110100010110100001110100000110011111110011110110011101110011100110011011110011010110011001110011000110010111110010110110010101110010100110010011110010010110010001110010000110001111110001110110001101110001100110001011110001010110001001110001000110000111110000110110000101110000100110000011110000010110000001110000000101111111101111110101111101101111100101111011101111010101111001101111000101110111101110110101110101101110100101110011101110010101110001101110000101101111101101110101101101101101100101101011101101010101101001101101000101100111101100110101100101101100100101100011101100010101100001101100000101011111101011110101011101101011100101011011101011010101011001101011000101010111101010110101010101101010100101010011101010010101010001101010000101001111101001110101001101101001100101001011101001010101001001101001000101000111101000110101000101101000100101000011101000010101000001101000000100111111100111110100111101100111100100111011100111010100111001100111000100110111100110110100110101100110100100110011100110010100110001100110000100101111100101110100101101100101100100101011100101010100101001100101000100100111100100110100100101100100100100100011100100010100100001100100000100011111100011110100011101100011100100011011100011010100011001100011000100010111100010110100010101100010100100010011100010010100010001100010000100001111100001110100001101100001100100001011100001010100001001100001000100000111100000110100000101100000100100000011100000010100000001100000000011111111011111110011111101011111100011111011011111010011111001011111000011110111011110110011110101011110100011110011011110010011110001011110000011101111011101110011101101011101100011101011011101010011101001011101000011100111011100110011100101011100100011100011011100010011100001011100000011011111011011110011011101011011100011011011011011010011011001011011000011010111011010110011010101011010100011010011011010010011010001011010000011001111011001110011001101011001100011001011011001010011001001011001000011000111011000110011000101011000100011000011011000010011000001011000000010111111010111110010111101010111100010111011010111010010111001010111000010110111010110110010110101010110100010110011010110010010110001010110000010101111010101110010101101010101100010101011010101010010101001010101000010100111010100110010100101010100100010100011010100010010100001010100000010011111010011110010011101010011100010011011010011010010011001010011000010010111010010110010010101010010100010010011010010010010010001010010000010001111010001110010001101010001100010001011010001010010001001010001000010000111010000110010000101010000100010000011010000010010000001010000000001111111001111110001111101001111100001111011001111010001111001001111000001110111001110110001110101001110100001110011001110010001110001001110000001101111001101110001101101001101100001101011001101010001101001001101000001100111001100110001100101001100100001100011001100010001100001001100000001011111001011110001011101001011100001011011001011010001011001001011000001010111001010110001010101001010100001010011001010010001010001001010000001001111001001110001001101001001100001001011001001010001001001001001000001000111001000110001000101001000100001000011001000010001000001001000000000111111000111110000111101000111100000111011000111010000111001000111000000110111000110110000110101000110100000110011000110010000110001000110000000101111000101110000101101000101100000101011000101010000101001000101000000100111000100110000100101000100100000100011000100010000100001000100000000011111000011110000011101000011100000011011000011010000011001000011000000010111000010110000010101000010100000010011000010010000010001000010000000001111000001110000001101000001100000001011000001010000001001000001000000000111000000110000000101000000100000000011000000010000000001000000000\";\r\n"); 
				}
				sb.append("\r\n" + 
				"");
		
		//now instantiate neurons to be tested

		for(Iterator<EDComponent> z = sim.neuronComponents.iterator(); z.hasNext(); ) {
			EDComponent child = z.next(); 
			sb.append("\r\n" + 
					"top_synth_uut : top_synth \r\n" + 
					"    port map (	clk => clk,\r\n" + 
					"				init_model => init_model,\r\n" + 
					"		   step_once_go  => step_once_go,\r\n" + 
					"		   step_once_complete  => step_once_complete,\r\n" + 
					"		   eventport_in_spike_aggregate => eventport_in_spike_aggregate,\r\n");
					if (useSynapseMux) {
						sb.append("		   SelectSpikesIn => SelectSpikesIn,\r\n");
					}

			int count = 0;
			for(Iterator<EDEventPort> i = child.eventports.iterator(); i.hasNext(); ) {
			    EDEventPort item = i.next();
			    sb.append("			"+child.name+"_eventport_" + item.direction +  "_"+ item.name + " => " +
			    		child.name + "_eventport_" + item.direction + "_"+  item.name + "_internal ,\r\n");
			    count++;
			}
			if (child.regimes.size() > 0)
				sb.append("			"+child.name + "_current_regimeRESTORE_stdlv => (others => '0'),\r\n" + 
						  "			"+child.name + "_current_regimeCurrent_stdlv => "+child.name + "_current_regimeCurrent_stdlv_internal\r\n,");
			
			writeNeuronComponentMap(child,sb,"",child.name,initialState);
			sb.append("\r\n" + 
					"           sysparam_time_timestep => sysparam_time_timestep,\r\n" + 
					"           sysparam_time_simtime => sysparam_time_simtime\r\n" + 
					"		   );\r\n" + 
					"		");
		}
		sb.append("\r\n" + 
				"receive_data: process \r\n" + 
				"\r\n" + 
				"variable l: line;\r\n" + 
				"\r\n" + 
				"variable char : character; \r\n");
		if (sim.synapseCount > 0 )
		{
		sb.append("\r\n" + 
				"variable s : STD_LOGIC_VECTOR("+(sim.synapseCount-1)+" downto 0); \r\n" + 
				"");	
		}
		sb.append("\r\n" + 
				"begin            \r\n" + 
				"   -- wait for Reset to complete\r\n" + 
				"   -- wait until init_model='1';\r\n" + 
				"   wait until init_model='0';\r\n" + 
				"\r\n" + 
				"   \r\n" + 
				"   while not endfile(stimulus) loop\r\n" + 
				"\r\n" + 
				"     -- read digital data from input file\r\n" + 
				"     readline(stimulus, l);\r\n");
		if (sim.synapseCount > 0 )
		{
		sb.append("\r\n" + 
				"\r\n" + 
				"     read(l, s);\r\n" + 
				"");	
			if (sim.synapseCount > 1 )
			{
				sb.append("eventport_in_spike_aggregate("+(sim.synapseCount-1)+" downto 0) <= ");
			}
			else
				sb.append("eventport_in_spike_aggregate(0) <= ");
			for (int i = 0; i < sim.synapseCount;i++)
			{
				sb.append("s("+i+")");
				if (i < sim.synapseCount - 1)
					sb.append(" & ");
				else
					sb.append(";");
				
					
			}
		}
		sb.append("\r\n" + 
				"     wait until step_once_go = '1';\r\n" + 
				"\r\n" + 
				"   end loop;\r\n" + 
				"    \r\n" + 
				"   \r\n" + 
				"   wait;\r\n" + 
				"\r\n" + 
				" end process receive_data;\r\n");
		for(Iterator<EDEventConnection> z = sim.eventConnections.iterator(); z.hasNext(); ) {
			EDEventConnection connection = z.next(); 
			for(Iterator<EDEventPort> y = connection.source.eventports.iterator(); y.hasNext(); ) {
				EDEventPort sourcePort = y.next(); 
				if (sourcePort.direction.matches("out"))
				{
					for(Iterator<EDEventPort> x = connection.target.eventports.iterator(); x.hasNext(); ) {
						EDEventPort targetPort = x.next(); 
						if (targetPort.direction.matches("in"))
						{
							sb.append(connection.target.name + "_eventport_in_" + targetPort.name + "_internal <= "
						+ connection.source.name + "_eventport_out_" + sourcePort.name + "_internal;");
						}
					}
				}
			}
		}
		
		sb.append("\r\n" + 
				"	process\r\n" + 
				"	variable L1              : LINE;\r\n" + 
				"    begin\r\n" + 
				"	write(L1, \"SimulationTime \" );\r\n");
		for(Iterator<EDComponent> z = sim.neuronComponents.iterator(); z.hasNext(); ) {
			EDComponent child = z.next(); 
			writeExposureTitlesToFile(sb,child,"",child.name);
		}
		sb.append("\r\n" + 
				"				writeline(test_out_data, L1); -- write row to output file\r\n" + 
				"        Wait;\r\n" + 
				"    end process;\r\n" + 
				"	\r\n" + 
				"	clk <= not(clk) after 10 ns;\r\n" + 
				"	");
		sb.append("\r\n" + 
				"step_once_go_proc: process \r\n" + 
				"\r\n" + 
				"\r\n" + 
				"begin            \r\n" + 
				"   -- wait for Reset to complete\r\n" + 
				"   -- wait until init_model='1';\r\n" + 
				"   wait until init_model='0';\r\n" + 
				"   \r\n" + 
				"   wait for 180 ns;\r\n" + 
				"   while 1 = 1 loop\r\n" + 
				"		step_once_go <= '1';\r\n" + 
				"		wait for 20 ns;\r\n" + 
				"		step_once_go <= '0';\r\n" + 
				"		wait until step_once_complete = '1';\r\n" + 
				"		wait until step_once_complete = '0';\r\n" + 
				"   end loop;\r\n" + 
				"    \r\n" + 
				"\r\n" + 
				" end process step_once_go_proc;\r\n" + 
				" \r\n" + 
				" \r\n" + 
				" \r\n" + 
				" process\r\n" + 
				" begin\r\n" + 
				"   wait for 20 ns;\r\n" + 
				"	init_model <= '1';\r\n" + 
				"   wait for 20 ns;\r\n" + 
				"	init_model <= '0';\r\n"
				+ "wait;\r\n" + 
				" end process ;\r\n" + 
				"  ");
		
		sb.append("\r\n" + 
				"	--\r\n" + 
				"	-- Print the results at each clock tick.\r\n" + 
				"	--\r\n" + 
				"	process(step_once_complete)\r\n" + 
				"	variable L1              : LINE;\r\n" + 
				"	begin\r\n" + 
				"		if (init_model = '1') then\r\n" + 
				"		\r\n" + 
				"				sysparam_time_simtime <= to_sfixed (0.0,6, -22);\r\n" + 
				"		else\r\n" + 
				"			if (step_once_complete'event and step_once_complete = '1' and init_model = '0') then\r\n" + 
				"				sysparam_time_simtime <= resize(sysparam_time_simtime + sysparam_time_timestep,6, -22);\r\n" + 
				"				write(L1, real'image(to_real( sysparam_time_simtime )));  -- nth value in row\r\n" + 
				"						write(L1, \" \");");

		for(Iterator<EDComponent> z = sim.neuronComponents.iterator(); z.hasNext(); ) {
			EDComponent child = z.next(); 
			writeExposuresToFile(sb,child,"",child.name);
		}
		
		sb.append("\r\n" + 
				"\r\n" + 
				"				\r\n" + 
				"				writeline(test_out_data, L1); -- write row to output file\r\n" + 
				"			end if;\r\n" + 
				"		end if;\r\n" + 
				"	end process;\r\n" + 
				"\r\n" + 
				"end tb;\r\n" + 
				"");
		
	}


	private static void writeExposureTitlesToFile(StringBuilder sb, EDComponent comp,String name, String parentName)
	{

		for(Iterator<EDState> y = comp.state.iterator(); y.hasNext(); ) {
			EDState exposure = y.next(); 
			sb.append("\r\n" + 
					"				write(L1, \"" +parentName + "_stateCURRENT_" + exposure.type +  "_" +  name +
					exposure.name + "\" );\r\n" + 
					"				write(L1, \" \");\r\n" + 
					"		");
		}
		for(Iterator<EDDerivedVariable> y = comp.derivedvariables.iterator(); y.hasNext(); ) {
			EDDerivedVariable exposure = y.next(); 
			if (exposure.ExposureIsUsed || exposure.IsUsedForOtherDerivedVariables) {
				sb.append("\r\n" + 
						"				write(L1, \"" +parentName + "_stateCURRENT_" + exposure.type +  "_" +  name +
						exposure.name + "\" );\r\n" + 
						"				write(L1, \" \");\r\n" + 
						"		");
			}
		}
		for(Iterator<EDConditionalDerivedVariable> y = comp.conditionalderivedvariables.iterator(); y.hasNext(); ) {
			EDConditionalDerivedVariable exposure = y.next(); 
			if (exposure.ExposureIsUsed || exposure.IsUsedForOtherDerivedVariables) {
				sb.append("\r\n" + 
						"				write(L1, \"" +parentName + "_stateCURRENT_" + exposure.type +  "_" +  name +
						exposure.name + "\" );\r\n" + 
						"				write(L1, \" \");\r\n" + 
						"		");
			}
		}
		for(Iterator<EDComponent> z = comp.Children.iterator(); z.hasNext(); ) {
			EDComponent child = z.next(); 
			String newName = name  + child.name + "_";
			writeExposureTitlesToFile(sb,child,newName,parentName);
		}
	}
	

	private static void writeExposuresToFile(StringBuilder sb, EDComponent comp,String name, String parentName)
	{

		for(Iterator<EDState> y = comp.state.iterator(); y.hasNext(); ) {
			EDState exposure = y.next(); 
			sb.append("\r\n" + 
					"				write(L1, real'image(to_real(" +parentName + "_stateCURRENT_" + exposure.type +  "_" +  name +
					exposure.name + "_int)) );\r\n" + 
					"				write(L1, \" \");\r\n" + 
					"		");
		}
		for(Iterator<EDDerivedVariable> y = comp.derivedvariables.iterator(); y.hasNext(); ) {
			EDDerivedVariable exposure = y.next(); 
			if (exposure.ExposureIsUsed || exposure.IsUsedForOtherDerivedVariables) {
				sb.append("\r\n" + 
						"				write(L1, real'image(to_real(" +parentName + "_stateCURRENT_" + exposure.type +  "_" +  name +
						exposure.name + "_int)) );\r\n" + 
						"				write(L1, \" \");\r\n" + 
						"		");
			}
		}
		for(Iterator<EDConditionalDerivedVariable> y = comp.conditionalderivedvariables.iterator(); y.hasNext(); ) {
			EDConditionalDerivedVariable exposure = y.next(); 
			if (exposure.ExposureIsUsed || exposure.IsUsedForOtherDerivedVariables) {
				sb.append("\r\n" + 
						"				write(L1, real'image(to_real(" +parentName + "_stateCURRENT_" + exposure.type +  "_" +  name +
						exposure.name + "_int)) );\r\n" + 
						"				write(L1, \" \");\r\n" + 
						"		");
			}
		}
		for(Iterator<EDComponent> z = comp.Children.iterator(); z.hasNext(); ) {
			EDComponent child = z.next(); 
			String newName = name  + child.name + "_";
			writeExposuresToFile(sb,child,newName,parentName);
		}
	}
	
	

	private static void writeNeuronComponentMap(EDComponent comp, StringBuilder sb,
			String name, String parentName, Map<String,Float> initialState)
	{

		for(Iterator<EDParameter> i = comp.parameters.iterator(); i.hasNext(); ) {
			EDParameter item = i.next(); 
			sb.append("			"+parentName+"_param_" + item.type +  "_" +name+  item.name + " => "  +
					"to_sfixed (" + item.value + "," + item.integer + " , " + item.fraction + "),\r\n" );
		}
		

		for(Iterator<EDState> i = comp.state.iterator(); i.hasNext(); ) {
			EDState item = i.next(); 
			sb.append("			"+parentName + "_stateCURRENT_" + item.type +  "_" + name+ item.name + " => " + parentName +
					"_stateCURRENT_" + item.type+   "_" +name+ item.name + "_int,\r\n"  );
			sb.append("			"+parentName + "_stateRESTORE_" + item.type +  "_" + name+item.name + " => to_sfixed (" + 
					initialState.get(parentName + "_stateCURRENT_" + item.type +  "_" + name+item.name) + "," 
					+ item.integer + " , " + item.fraction + "),\r\n"  );
		}
		for(Iterator<EDDerivedVariable> i = comp.derivedvariables.iterator(); i.hasNext(); ) {
			EDDerivedVariable item = i.next(); 
			if (item.IsUsedForOtherDerivedVariables || item.ExposureIsUsed){
				sb.append("			"+parentName + "_stateCURRENT_" + item.type +  "_" + name+ item.name + " => " + parentName +
						"_stateCURRENT_" + item.type+   "_" +name+ item.name + "_int,\r\n"  );
				sb.append("			"+parentName + "_stateRESTORE_" + item.type +  "_" + name+item.name + " => to_sfixed (" + 
						initialState.get(parentName + "_stateCURRENT_" + item.type +  "_" + name+item.name) + "," 
						+ item.integer + " , " + item.fraction + "),\r\n"  );
			}
		}
		for(Iterator<EDConditionalDerivedVariable> i = comp.conditionalderivedvariables.iterator(); i.hasNext(); ) {
			EDConditionalDerivedVariable item = i.next(); 
			if (item.IsUsedForOtherDerivedVariables || item.ExposureIsUsed){
				sb.append("			"+parentName + "_stateCURRENT_" + item.type +  "_" + name+ item.name + " => " + parentName +
						"_stateCURRENT_" + item.type+   "_" +name+ item.name + "_int,\r\n"  );
				sb.append("			"+parentName + "_stateRESTORE_" + item.type +  "_" + name+item.name + " => to_sfixed (" + 
						initialState.get(parentName + "_stateCURRENT_" + item.type +  "_" + name+item.name) + "," 
						+ item.integer + " , " + item.fraction + "),\r\n"  );
			}
		}
		
		for(Iterator<EDComponent> i = comp.Children.iterator(); i.hasNext(); ) {
			EDComponent item = i.next(); 
			String newName = name  + item.name + "_";
			writeNeuronComponentMap(item, sb, newName, parentName,initialState);
		}
		
	
	}
	
	
	public static void writeStateSignals(EDComponent comp, StringBuilder sb, String name,String parentName)
	{
		for(Iterator<EDState> i = comp.state.iterator(); i.hasNext(); ) {
			EDState state = i.next(); 
			sb.append("signal " + parentName + "_stateCURRENT_" + state.type +  "_" +  name+state.name + "_int : sfixed (" + 
					state.integer + " downto " + state.fraction + ");");
		}
		for(Iterator<EDDerivedVariable> i = comp.derivedvariables.iterator(); i.hasNext(); ) {
			EDDerivedVariable state = i.next(); 
			if (state.IsUsedForOtherDerivedVariables || state.ExposureIsUsed){
				sb.append("signal " + parentName + "_stateCURRENT_" + state.type +  "_" +  name+state.name + "_int : sfixed (" + 
						state.integer + " downto " + state.fraction + ");");
			}
		}
		for(Iterator<EDConditionalDerivedVariable> i = comp.conditionalderivedvariables.iterator(); i.hasNext(); ) {
			EDConditionalDerivedVariable state = i.next(); 
			if (state.IsUsedForOtherDerivedVariables || state.ExposureIsUsed){
				sb.append("signal " + parentName + "_stateCURRENT_" + state.type +  "_" +  name+state.name + "_int : sfixed (" + 
						state.integer + " downto " + state.fraction + ");");
			}
		}
		for(Iterator<EDEventPort> i = comp.eventports.iterator(); i.hasNext(); ) {
			EDEventPort eventport = i.next(); 
			sb.append("signal " + parentName + "_eventport_" + eventport.direction + "_" + name+eventport.name + 
					"_internal : std_logic; ");
		}
		
		for(Iterator<EDComponent> i = comp.Children.iterator(); i.hasNext(); ) {
			EDComponent item = i.next(); 
			String newName = name + item.name + "_";
			writeStateSignals(item, sb, newName,parentName);
		}
	
	}
}

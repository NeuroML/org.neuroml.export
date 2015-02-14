package org.lemsml.export.vhdl.writer;

import java.util.Iterator;

import org.lemsml.export.vhdl.edlems.EDComponent;
import org.lemsml.export.vhdl.edlems.EDConditionalDerivedVariable;
import org.lemsml.export.vhdl.edlems.EDDerivedVariable;
import org.lemsml.export.vhdl.edlems.EDEventConnection;
import org.lemsml.export.vhdl.edlems.EDEventPort;

import org.lemsml.export.vhdl.edlems.EDParameter;
import org.lemsml.export.vhdl.edlems.EDRequirement;
import org.lemsml.export.vhdl.edlems.EDSimulation;
import org.lemsml.export.vhdl.edlems.EDState;

public class SiElegansTop {

	static int currentBit = 511;//5119;
	static int currentLength = 0;
	static int Integer = 0;
	
	public static void writeTop(EDSimulation sim, StringBuilder sb)
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
				"use IEEE.numeric_std.all;\r\n" + 
				"");
		
		sb.append("\r\n" + 
				"entity sielegans_top is\r\n" + 
				"    Port ( clk : in STD_LOGIC; --SYSTEM CLOCK, THIS ITSELF DOES NOT SIGNIFY TIME STEPS - AKA A SINGLE TIMESTEP MAY TAKE MANY CLOCK CYCLES\r\n" + 
				"           init_model : in STD_LOGIC; --SYNCHRONOUS RESET\r\n" + 
				"		   step_once_go : in STD_LOGIC; --signals to the neuron from the core that a time step is to be simulated\r\n" + 
				"		   step_once_complete : out STD_LOGIC; --signals to the core that a time step has finished\r\n" + 
				"		   \r\n" + 
				"           mega_bus_in : in STD_LOGIC_Vector (99999 downto 0 );\r\n" + 
				"           mega_bus_out : out STD_LOGIC_Vector (99999 downto 0 )\r\n" + 
				"		   );\r\n" + 
				"end sielegans_top;\r\n" + 
				"\r\n" + 
				"---------------------------------------------------------------------\r\n" + 
				"\r\n" + 
				"architecture top of sielegans_top is\r\n");
		

		EDComponent neuron = sim.neuronComponents.get(0);
		sb.append("\r\n" + 
				"component top_synth\r\n" + 
				"    Port ( clk : in STD_LOGIC; --SYSTEM CLOCK, THIS ITSELF DOES NOT SIGNIFY TIME STEPS - AKA A SINGLE TIMESTEP MAY TAKE MANY CLOCK CYCLES\r\n" + 
				"           init_model : in STD_LOGIC; --SYNCHRONOUS RESET\r\n" + 
				"		   step_once_go : in STD_LOGIC; --signals to the neuron from the core that a time step is to be simulated\r\n" + 
				"		   step_once_complete : out STD_LOGIC; --signals to the core that a time step has finished\r\n" + 
				"		   eventport_in_spike_aggregate : in STD_LOGIC_VECTOR(511 downto 0);\r\n" + 
				//"		   SelectSpikesIn :  in STD_LOGIC_VECTOR(4607 downto 0);\r\n" + 
				"		   ");
		
		String name = "";
		for(Iterator<EDEventPort> i = neuron.eventports.iterator(); i.hasNext(); ) {
		    EDEventPort item = i.next();
		    sb.append("			" + neuron.name +  "_eventport_" + item.direction +  "_" + item.name + " : " + item.direction + " STD_LOGIC;\r\n"  );
		}

		if (neuron.regimes.size() > 0)
			sb.append("			" + neuron.name + "_current_regimeRESTORE_stdlv : in STD_LOGIC_VECTOR(1 downto 0);\r\n" + 
					  "			" + neuron.name + "_current_regimeCurrent_stdlv : out STD_LOGIC_VECTOR(1 downto 0);\r\n");
		TopSynth.writeEntitySignals(neuron,sb,name,neuron.name + "_");
		sb.append("\r\n" + 
				"           sysparam_time_timestep : sfixed (-6 downto -22);\r\n" + 
				"           sysparam_time_simtime : sfixed (6 downto -22)\r\n" + 
				"		   );\r\n" + 
				"end component;\r\n" + 
				"\r\n" + 
				"\r\n" + 
				"	\r\n" + 
				"	");
		

		for(Iterator<EDEventPort> i = neuron.eventports.iterator(); i.hasNext(); ) {
		    EDEventPort item = i.next();
		    sb.append("signal " + neuron.name + "_eventport_" + item.direction + "_" + item.name + "_internal : std_logic;\r\n"  );
		}
		if (neuron.regimes.size() > 0)
		{
			sb.append("signal " + neuron.name + "_current_regimeCurrent_stdlv_int :  STD_LOGIC_VECTOR(1 downto 0);\r\n");
		}
		writeStateSignals(neuron,sb,"",neuron.name);

		
		sb.append("begin\r\n"
				+ "\r\n"
				+ "top_synth_uut : top_synth \r\n" + 
				"    port map (	clk => clk,\r\n" + 
				"				init_model => init_model, \r\n" + 
				"		   step_once_go  => step_once_go,\r\n" + 
				"		   step_once_complete  => step_once_complete,\r\n" + 
				"		   eventport_in_spike_aggregate =>  mega_bus_in(511 downto 0),\r\n" + 
				//"		   SelectSpikesIn => mega_bus_in(5119 downto 512),\r\n" + 
				"		   ");
		

		for(Iterator<EDEventPort> i = neuron.eventports.iterator(); i.hasNext(); ) {
		    EDEventPort item = i.next();
		    sb.append("			" + neuron.name + "_eventport_" + item.direction +  "_" + item.name + " => " + neuron.name + "_eventport_" +item.direction + "_" + item.name + "_internal ,\r\n"   );
		}
		
		if (neuron.regimes.size() > 0)
		{
			currentBit = currentBit + currentLength + 1;
			currentLength = 1;
			int currentTop = currentBit + currentLength;
			sb.append("" + neuron.name + "_current_regimeRESTORE_stdlv =>  mega_bus_in(" + currentTop + " downto " + 
					currentBit + "),\r\n");
			sb.append("" + neuron.name + "_current_regimeCurrent_stdlv => " + neuron.name + "_current_regimeCurrent_stdlv_int,\r\n");
		}
		
		writeConnectivityMap(neuron,sb,"",neuron.name);

		currentBit = currentBit + currentLength + 1;
		currentLength = 16;
		int currentTop = currentBit + currentLength;
		sb.append("sysparam_time_timestep => to_sfixed (mega_bus_in(" + currentTop + " downto " + currentBit + "),-6 , -22),");

		currentBit = currentBit + currentLength + 1;
		currentLength = 28;
		currentTop = currentBit + currentLength;
		sb.append("sysparam_time_simtime => to_sfixed (mega_bus_in(" + currentTop + " downto " + currentBit + "),6, -22)");
		sb.append("\r\n" + 
				"		   );\r\n");

		currentBit = -1;
		currentLength = 0;
		
		
		for(Iterator<EDEventPort> i = neuron.eventports.iterator(); i.hasNext(); ) {
		    EDEventPort item = i.next();
		    currentBit = currentBit + currentLength + 1;
		    sb.append(" mega_bus_out("  + currentBit + ") <= " + neuron.name + "_eventport_" + item.direction + "_" + item.name + "_internal;\r\n"  );
		}

		if (neuron.regimes.size() > 0)
		{
			currentBit = currentBit + currentLength + 1;
			currentLength = 1;
			currentTop = currentBit + currentLength;
			sb.append(" mega_bus_out(" + currentTop + " downto " + currentBit + 
					") <= " + neuron.name + "_current_regimeCurrent_stdlv_int;\r\n");
		}
		
		
		writeStateToBusSignals(neuron,sb,"",neuron.name);
		
		sb.append("\r\n" + 
				"end top;\r\n" + 
				"");
		
		
	}
	

	public static void writeStateToBusSignals(EDComponent comp, StringBuilder sb, String name,String parentName)
	{
		for(Iterator<EDState> i = comp.state.iterator(); i.hasNext(); ) {
			EDState state = i.next(); 
			currentBit = currentBit + currentLength + 1;
			currentLength = (state.integer) - (state.fraction);
			int currentTop = currentBit + currentLength;
			sb.append(" mega_bus_out(" + currentTop + " downto " + currentBit + 
					") <= to_slv(" + parentName + "_stateCURRENT_" + state.type +  "_" + name + state.name + "_int);\r\n");
		}
		for(Iterator<EDDerivedVariable> i = comp.derivedvariables.iterator(); i.hasNext(); ) {
			EDDerivedVariable state = i.next(); 
			currentBit = currentBit + currentLength + 1;
			currentLength = (state.integer) - (state.fraction);
			int currentTop = currentBit + currentLength;
			sb.append(" mega_bus_out(" + currentTop + " downto " + currentBit + 
					") <= to_slv(" + parentName + "_stateCURRENT_" + state.type +  "_" + name + state.name + "_int);\r\n");
		}
		for(Iterator<EDConditionalDerivedVariable> i = comp.conditionalderivedvariables.iterator(); i.hasNext(); ) {
			EDConditionalDerivedVariable state = i.next(); 
			currentBit = currentBit + currentLength + 1;
			currentLength = (state.integer) - (state.fraction);
			int currentTop = currentBit + currentLength;
			sb.append(" mega_bus_out(" + currentTop + " downto " + currentBit + 
					") <= to_slv(" + parentName + "_stateCURRENT_" + state.type +  "_" + name + state.name + "_int);\r\n");
		}

		for(Iterator<EDComponent> i = comp.Children.iterator(); i.hasNext(); ) {
			EDComponent item = i.next(); 
			String newName = name + item.name + "_";
			writeStateToBusSignals(item, sb, newName,parentName);
		}
	
	}

	public static void writeStateSignals(EDComponent comp, StringBuilder sb, String name,String parentName)
	{
		for(Iterator<EDState> i = comp.state.iterator(); i.hasNext(); ) {
			EDState state = i.next(); 
			sb.append("signal " + parentName + "_stateCURRENT_" + state.type +  "_" +  name+state.name + "_int : sfixed (" + 
					state.integer + " downto " + state.fraction + ");\r\n");
		}
		for(Iterator<EDDerivedVariable> i = comp.derivedvariables.iterator(); i.hasNext(); ) {
			EDDerivedVariable state = i.next(); 
			sb.append("signal " + parentName + "_stateCURRENT_" + state.type +  "_" +  name+state.name + "_int : sfixed (" + 
					state.integer + " downto " + state.fraction + ");\r\n");
		}
		for(Iterator<EDConditionalDerivedVariable> i = comp.conditionalderivedvariables.iterator(); i.hasNext(); ) {
			EDConditionalDerivedVariable state = i.next(); 
			sb.append("signal " + parentName + "_stateCURRENT_" + state.type +  "_" +  name+state.name + "_int : sfixed (" + 
					state.integer + " downto " + state.fraction + ");\r\n");
		}

		for(Iterator<EDComponent> i = comp.Children.iterator(); i.hasNext(); ) {
			EDComponent item = i.next(); 
			String newName = name + item.name + "_";
			writeStateSignals(item, sb, newName,parentName);
		}
	
	}
	

	public static void writeConnectivityMap(EDComponent comp, StringBuilder sb, String name,String parentName)
	{
		for(Iterator<EDParameter> i = comp.parameters.iterator(); i.hasNext(); ) {
			EDParameter item = i.next(); 
			currentBit = currentBit + currentLength + 1;
			currentLength = (item.integer) - (item.fraction);
			int currentTop = currentBit + currentLength;
			sb.append(parentName + "_param_" + item.type +  "_"+name + item.name + " => to_sfixed ( mega_bus_in(" + currentTop + " downto " + 
					currentBit + ")," + item.integer + " , " + item.fraction + "),\r\n" );
		}
		
		for(Iterator<EDState> i = comp.state.iterator(); i.hasNext(); ) {
			EDState state = i.next(); 
			currentBit = currentBit + currentLength + 1;
			currentLength = (state.integer) - (state.fraction);
			int currentTop = currentBit + currentLength;
			sb.append(parentName + "_stateCURRENT_" + state.type +  "_"+name + state.name + 
					" => " + parentName + "_stateCURRENT_" + state.type+   "_"+name + state.name + "_int,\r\n" );
			sb.append(parentName + "_stateRESTORE_" + state.type +  "_"+name + state.name + 
					" => to_sfixed (mega_bus_in(" + currentTop + " downto " + currentBit + ")," + state.integer + " , " + state.fraction + "),\r\n" );
		}
		for(Iterator<EDDerivedVariable> i = comp.derivedvariables.iterator(); i.hasNext(); ) {
			EDDerivedVariable state = i.next(); 
			currentBit = currentBit + currentLength + 1;
			currentLength = (state.integer) - (state.fraction);
			int currentTop = currentBit + currentLength;
			sb.append(parentName + "_stateCURRENT_" + state.type +  "_"+name + state.name + 
					" => " + parentName + "_stateCURRENT_" + state.type+   "_"+name + state.name + "_int,\r\n" );
			sb.append(parentName + "_stateRESTORE_" + state.type +  "_"+name + state.name + 
					" => to_sfixed (mega_bus_in(" + currentTop + " downto " + currentBit + ")," + state.integer + " , " + state.fraction + "),\r\n" );
		}
		for(Iterator<EDConditionalDerivedVariable> i = comp.conditionalderivedvariables.iterator(); i.hasNext(); ) {
			EDConditionalDerivedVariable state = i.next(); 
			currentBit = currentBit + currentLength + 1;
			currentLength = (state.integer) - (state.fraction);
			int currentTop = currentBit + currentLength;
			sb.append(parentName + "_stateCURRENT_" + state.type +  "_"+name + state.name + 
					" => " + parentName + "_stateCURRENT_" + state.type+   "_"+name + state.name + "_int,\r\n" );
			sb.append(parentName + "_stateRESTORE_" + state.type +  "_"+name + state.name + 
					" => to_sfixed (mega_bus_in(" + currentTop + " downto " + currentBit + ")," + state.integer + " , " + state.fraction + "),\r\n" );
		}

		for(Iterator<EDComponent> i = comp.Children.iterator(); i.hasNext(); ) {
			EDComponent item = i.next(); 
			String newName = name + item.name + "_";
			writeConnectivityMap(item, sb, newName,parentName);
		}
	
	}
	
}
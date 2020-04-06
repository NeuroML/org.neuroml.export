package org.lemsml.export.vhdl.writer;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import org.lemsml.export.vhdl.edlems.EDComponent;
import org.lemsml.export.vhdl.edlems.EDConditionalDerivedVariable;
import org.lemsml.export.vhdl.edlems.EDDerivedVariable;
import org.lemsml.export.vhdl.edlems.EDEventConnection;
import org.lemsml.export.vhdl.edlems.EDEventPort;
import org.lemsml.export.vhdl.edlems.EDParameter;
import org.lemsml.export.vhdl.edlems.EDRequirement;
import org.lemsml.export.vhdl.edlems.EDSimulation;
import org.lemsml.export.vhdl.edlems.EDState;

public class NeuronCoreTop {

	static int currentBit = 511;//5119;
	static int currentLength = 0;
	static int Integer = 0;
	
	
	public static void writeSignal(String name, int bottom, int top, 
			int integer, int fractional,JsonGenerator g) throws JsonGenerationException, IOException
	{
		g.writeStartObject();
		g.writeStringField("name",name);
		g.writeNumberField("bottom", bottom);
		g.writeNumberField("top", top);
		g.writeNumberField("integer", integer);
		g.writeNumberField("fractional", fractional);
		g.writeEndObject();
	}
	
	public static void writeNeuronCoreTop(EDSimulation sim, StringBuilder sb
			, JsonGenerator g, Map<String,Float> initialState, String neuronName) throws JsonGenerationException, IOException
	{
		 currentBit = 511;//5119;
		 currentLength = 0;
		 Integer = 0;
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
				"entity neuroncore_top is\r\n" + 
				"    Port ( clk : in STD_LOGIC; --SYSTEM CLOCK, THIS ITSELF DOES NOT SIGNIFY TIME STEPS - AKA A SINGLE TIMESTEP MAY TAKE MANY CLOCK CYCLES\r\n" + 
				"           init_model : in STD_LOGIC; --SYNCHRONOUS RESET\r\n" + 
				"		   step_once_go : in STD_LOGIC; --signals to the neuron from the core that a time step is to be simulated\r\n" + 
				"		   step_once_complete : out STD_LOGIC; --signals to the core that a time step has finished\r\n" + 
				"		   \r\n" + 
				"           mega_bus_in : in STD_LOGIC_Vector (99999 downto 0 );\r\n" + 
				"           mega_bus_out : out STD_LOGIC_Vector (99999 downto 0 )\r\n" + 
				"		   );\r\n" + 
				"end neuroncore_top;\r\n" + 
				"\r\n" + 
				"---------------------------------------------------------------------\r\n" + 
				"\r\n" + 
				"architecture top of neuroncore_top is\r\n");
		

		EDComponent neuron = sim.neuronComponents.get(0);
		for (int i = 0; i < sim.neuronComponents.size(); i++)
		{
			if (sim.neuronComponents.get(i).name.matches(neuronName))
			{
				neuron = sim.neuronComponents.get(i);
				break;
			}
		}
		
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

		if (neuron.regimes.size() > 0){
			sb.append("			" + neuron.name + "current_regime_in_stdlv : in STD_LOGIC_VECTOR(1 downto 0);\r\n" + 
					  "			" + neuron.name + "current_regime_out_stdlv : out STD_LOGIC_VECTOR(1 downto 0);\r\n");
		}
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
			sb.append("signal " + neuron.name + "_current_regime_out_stdlv_int :  STD_LOGIC_VECTOR(1 downto 0);\r\n");
		}
		writeStateSignals(neuron,sb,"",neuron.name);


		g.writeObjectFieldStart(neuron.name);
		g.writeObjectFieldStart("SpikesIn");
		g.writeNumberField("bottom", 0);
		g.writeNumberField("top", 511);
		g.writeEndObject();
		g.writeArrayFieldStart("NeuronVariablesIn");

		sb.append("begin\r\n"
				+ "\r\n"
				+ "top_synth_uut : neuron_model \r\n" + 
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
			while (currentBit%8 != 0)
				currentBit++;
			currentLength = 1;
			int currentTop = currentBit + currentLength;
			writeSignal("regime",currentBit,currentTop,0,0,g);
			sb.append("" + neuron.name + "current_regime_in_stdlv =>  mega_bus_in(" + currentTop + " downto " + 
					currentBit + "),\r\n");
			sb.append("" + neuron.name + "current_regime_out_stdlv => " + neuron.name + "_current_regime_out_stdlv_int,\r\n");
		}
		writeConnectivityMapVar(neuron,sb,"",neuron.name,g);
		
		g.writeEndArray();
		g.writeArrayFieldStart("NeuronParameters");
		writeConnectivityMapPar(neuron,sb,"",neuron.name,g);
		
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
		g.writeEndArray();
		g.writeArrayFieldStart("NeuronVariablesOut");

		currentBit = -1;
		currentLength = 0;
		

		if (neuron.regimes.size() > 0)
		{
			currentBit = currentBit + currentLength + 1;
			while (currentBit%8 != 0)
				currentBit++;
			currentLength = 1;
			currentTop = currentBit + currentLength;
			writeSignal("regime",currentBit,currentTop,0,0,g);
			sb.append(" mega_bus_out(" + currentTop + " downto " + currentBit + 
					") <= " + neuron.name + "_current_regime_out_stdlv_int;\r\n");
		}
		
		
		writeStateToBusSignals(neuron,sb,"",neuron.name,g);

		
		for(Iterator<EDEventPort> i = neuron.eventports.iterator(); i.hasNext(); ) {
		    EDEventPort item = i.next();
		    currentBit = currentBit + currentLength + 1;
			while (currentBit%8 != 0)
				currentBit++;
		    sb.append(" mega_bus_out("  + currentBit + ") <= " + neuron.name + "_eventport_" + item.direction + "_" + item.name + "_internal;\r\n"  );
		}
		sb.append("\r\n" + 
				"end top;\r\n" + 
				"");
		
		g.writeEndArray();
		g.writeEndObject();
		
	}
	

	public static void writeStateToBusSignals(EDComponent comp, StringBuilder sb, 
			String name,String parentName, JsonGenerator g) throws JsonGenerationException, IOException
	{
		for(Iterator<EDState> i = comp.state.iterator(); i.hasNext(); ) {
			EDState state = i.next(); 
			currentBit = currentBit + currentLength + 1;
			while (currentBit%8 != 0)
				currentBit++;
			currentLength = (state.integer) - (state.fraction);
			int currentTop = currentBit + currentLength;
			writeSignal(parentName + "_statevariable_" + state.type +  "_" + name + state.name + "_out"
					,currentBit,currentTop,state.integer,state.fraction,g);
			sb.append(" mega_bus_out(" + currentTop + " downto " + currentBit + 
					") <= to_slv(" + parentName + "_statevariable_" + state.type +  "_" + name + state.name + "_out_int);\r\n");
		}
		for(Iterator<EDDerivedVariable> i = comp.derivedvariables.iterator(); i.hasNext(); ) {
			EDDerivedVariable state = i.next(); 
			currentBit = currentBit + currentLength + 1;
			while (currentBit%8 != 0)
				currentBit++;
			currentLength = (state.integer) - (state.fraction);
			int currentTop = currentBit + currentLength;
			writeSignal(parentName + "_statevariable_" + state.type +  "_" + 
					name + state.name + "_out",currentBit,currentTop,state.integer,state.fraction,g);
			sb.append(" mega_bus_out(" + currentTop + " downto " + currentBit + 
					") <= to_slv(" + parentName + "_statevariable_" + state.type +  "_" + 
					name + state.name + "_out_int);\r\n");
		}
		for(Iterator<EDConditionalDerivedVariable> i = comp.conditionalderivedvariables.iterator(); i.hasNext(); ) {
			EDConditionalDerivedVariable state = i.next(); 
			currentBit = currentBit + currentLength + 1;
			while (currentBit%8 != 0)
				currentBit++;
			currentLength = (state.integer) - (state.fraction);
			int currentTop = currentBit + currentLength;
			writeSignal(parentName + "_statevariable_" + state.type +  "_" + name + state.name + "_out"
					,currentBit,currentTop,state.integer,state.fraction,g);
			sb.append(" mega_bus_out(" + currentTop + " downto " + currentBit + 
					") <= to_slv(" + parentName + "_statevariable_" + state.type +  "_" + name + state.name + "_out_int);\r\n");
		}

		for(Iterator<EDComponent> i = comp.Children.iterator(); i.hasNext(); ) {
			EDComponent item = i.next(); 
			if (!item.isSynapse) {
				String newName = name + item.name + "_";
				writeStateToBusSignals(item, sb, newName,parentName,g);
			}
		}
		
		boolean hasSynapses = false;
		for(Iterator<EDComponent> i = comp.Children.iterator(); i.hasNext(); ) {
			EDComponent item = i.next(); 
			if (item.isSynapse) {
				hasSynapses = true;
			}
		}
		
		if (hasSynapses == true) {
			for(Iterator<EDComponent> i = comp.Children.iterator(); i.hasNext(); ) {
				EDComponent item = i.next(); 
				if (item.isSynapse) {
					g.writeEndArray();
					g.writeArrayFieldStart("SynapseVariablesOut_" + item.name);
					String newName = name + item.name + "_";
					writeStateToBusSignals(item, sb, newName,parentName, g);
				}
			}
		}
	}

	public static void writeStateSignals(EDComponent comp, StringBuilder sb, String name,String parentName)
	{
		for(Iterator<EDState> i = comp.state.iterator(); i.hasNext(); ) {
			EDState state = i.next(); 
			sb.append("signal " + parentName + "_statevariable_" + state.type +  "_" +  name+state.name + "_out_int : sfixed (" + 
					state.integer + " downto " + state.fraction + ");\r\n");
		}
		for(Iterator<EDDerivedVariable> i = comp.derivedvariables.iterator(); i.hasNext(); ) {
			EDDerivedVariable state = i.next(); 
			sb.append("signal " + parentName + "_statevariable_" + state.type +  "_" +  name+state.name + "_out_int : sfixed (" + 
					state.integer + " downto " + state.fraction + ");\r\n");
		}
		for(Iterator<EDConditionalDerivedVariable> i = comp.conditionalderivedvariables.iterator(); i.hasNext(); ) {
			EDConditionalDerivedVariable state = i.next(); 
			sb.append("signal " + parentName + "_statevariable_" + state.type +  "_" +  name+state.name + "_out_int : sfixed (" + 
					state.integer + " downto " + state.fraction + ");\r\n");
		}

		for(Iterator<EDComponent> i = comp.Children.iterator(); i.hasNext(); ) {
			EDComponent item = i.next(); 
			String newName = name + item.name + "_";
			writeStateSignals(item, sb, newName,parentName);
		}
	
	}
	

	public static void writeConnectivityMapVar(EDComponent comp, StringBuilder sb, String name,String parentName, JsonGenerator g) throws JsonGenerationException, IOException
	{
				
		for(Iterator<EDState> i = comp.state.iterator(); i.hasNext(); ) {
			EDState state = i.next(); 
			currentBit = currentBit + currentLength + 1;
			while (currentBit%8 != 0)
				currentBit++;
			currentLength = (state.integer) - (state.fraction);
			int currentTop = currentBit + currentLength;
			writeSignal(parentName + "_statevariable_" + state.type +  "_"+name + state.name + 
					"_in",currentBit,currentTop,state.integer,state.fraction,g);
			sb.append(parentName + "_statevariable_" + state.type +  "_"+name + state.name + 
					"_out => " + parentName + "_statevariable_" + state.type+   "_"+name + state.name + "_out_int,\r\n" );
			sb.append(parentName + "_statevariable_" + state.type +  "_"+name + state.name + 
					"_in => to_sfixed (mega_bus_in(" + currentTop + " downto " + currentBit + ")," + state.integer + " , " + state.fraction + "),\r\n" );
		}
		for(Iterator<EDDerivedVariable> i = comp.derivedvariables.iterator(); i.hasNext(); ) {
			EDDerivedVariable state = i.next(); 
			currentBit = currentBit + currentLength + 1;
			while (currentBit%8 != 0)
				currentBit++;
			currentLength = (state.integer) - (state.fraction);
			int currentTop = currentBit + currentLength;
			writeSignal(parentName + "_statevariable_" + state.type +  "_"+name + state.name + 
					"_in",currentBit,currentTop,state.integer,state.fraction,g);
			sb.append(parentName + "_statevariable_" + state.type +  "_"+name + state.name + 
					"_out => " + parentName + "_statevariable_" + state.type+   "_"+name + state.name + "_out_int,\r\n" );
			sb.append(parentName + "_statevariable_" + state.type +  "_"+name + state.name + 
					"_in => to_sfixed (mega_bus_in(" + currentTop + " downto " + currentBit + ")," + state.integer + " , " + state.fraction + "),\r\n" );
		}
		for(Iterator<EDConditionalDerivedVariable> i = comp.conditionalderivedvariables.iterator(); i.hasNext(); ) {
			EDConditionalDerivedVariable state = i.next(); 
			currentBit = currentBit + currentLength + 1;
			while (currentBit%8 != 0)
				currentBit++;
			currentLength = (state.integer) - (state.fraction);
			int currentTop = currentBit + currentLength;
			writeSignal(parentName + "_statevariable_" + state.type +  "_"+name + state.name + 
					"_in",currentBit,currentTop,state.integer,state.fraction,g);
			sb.append(parentName + "_statevariable_" + state.type +  "_"+name + state.name + 
					"_out => " + parentName + "_statevariable_" + state.type+   "_"+name + state.name + "_out_int,\r\n" );
			sb.append(parentName + "_statevariable_" + state.type +  "_"+name + state.name + 
					"_in => to_sfixed (mega_bus_in(" + currentTop + " downto " + currentBit + ")," + state.integer + " , " + state.fraction + "),\r\n" );
		}

		for(Iterator<EDComponent> i = comp.Children.iterator(); i.hasNext(); ) {
			EDComponent item = i.next(); 
			if (!item.isSynapse) {
				String newName = name + item.name + "_";
				writeConnectivityMapVar(item, sb, newName,parentName, g);
			}
		}

		boolean hasSynapses = false;
		for(Iterator<EDComponent> i = comp.Children.iterator(); i.hasNext(); ) {
			EDComponent item = i.next(); 
			if (item.isSynapse) {
				hasSynapses = true;
			}
		}
		if (hasSynapses == true) {
			for(Iterator<EDComponent> i = comp.Children.iterator(); i.hasNext(); ) {
				EDComponent item = i.next(); 
				if (item.isSynapse) {
					g.writeEndArray();
					g.writeArrayFieldStart("SynapseVariablesIn_" + item.name);
					String newName = name + item.name + "_";
					writeConnectivityMapVar(item, sb, newName,parentName, g);
				}
			}
		}
	
	}
	

	public static void writeConnectivityMapPar(EDComponent comp, StringBuilder sb, String name,String parentName, JsonGenerator g) throws JsonGenerationException, IOException
	{
		for(Iterator<EDParameter> i = comp.parameters.iterator(); i.hasNext(); ) {
			EDParameter item = i.next(); 
			currentBit = currentBit + currentLength + 1;
			while (currentBit%8 != 0)
				currentBit++;
			currentLength = (item.integer) - (item.fraction);
			int currentTop = currentBit + currentLength;
			writeSignal(parentName + "_param_" + item.type +  "_"+name + item.name,currentBit,currentTop,item.integer,item.fraction,g);
			sb.append(parentName + "_param_" + item.type +  "_"+name + item.name + " => to_sfixed ( mega_bus_in(" + currentTop + " downto " + 
					currentBit + ")," + item.integer + " , " + item.fraction + "),\r\n" );
		}
		
		for(Iterator<EDComponent> i = comp.Children.iterator(); i.hasNext(); ) {
			EDComponent item = i.next(); 
			if (!item.isSynapse) {
				String newName = name + item.name + "_";
				writeConnectivityMapPar(item, sb, newName,parentName, g);
			}
		}
		

		boolean hasSynapses = false;
		for(Iterator<EDComponent> i = comp.Children.iterator(); i.hasNext(); ) {
			EDComponent item = i.next(); 
			if (item.isSynapse) {
				hasSynapses = true;
			}
		}
		if (hasSynapses == true) {
			int count = 0;
			for(Iterator<EDComponent> i = comp.Children.iterator(); i.hasNext(); ) {
				EDComponent item = i.next(); 
				if (item.isSynapse) {
					g.writeEndArray();
					g.writeArrayFieldStart("SynapseParameters_" + item.name);
					String newName = name + item.name + "_";
					writeConnectivityMapVar(item, sb, newName,parentName, g);
				}
			}
		}
	
	}
	
}
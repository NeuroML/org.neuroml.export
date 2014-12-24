package org.lemsml.export.vhdl.writer;

import java.util.Iterator;

import org.lemsml.export.vhdl.edlems.*;

public class Entity {

	public static void writeEDComponent(EDComponent comp, StringBuilder sb, boolean isTopLevelNeuronModel)
	{
		writePreamble(sb);
		writeEntityDescription(sb,comp);
		Architecture.writeArchitecture(comp,sb,comp.name);
		Architecture.writeChildrenDeclarations(sb,comp);
		Architecture.writeChildrenInstantiations(comp,sb);
		DerivedVariableProcess.writeEDDerivedVariableProcess(sb,comp);
		RegimeStateMachine.writeEDRegimeEDStateMachine(sb, comp);
		StatevariableProcess.writeStateVariableProcess( sb,  comp, isTopLevelNeuronModel);
	}
	
	private static void writePreamble(StringBuilder sb)
	{
		sb.append("\r\n" + 
				"---------------------------------------------------------------------\r\n" + 
				"-- Standard Library bits \r\n" + 
				"---------------------------------------------------------------------\r\n" + 
				"\r\n" + 
				"\r\n" + 
				"library IEEE;\r\n" + 
				"use IEEE.STD_LOGIC_1164.ALL;\r\n" + 
				"\r\n" + 
				"-- For Modelsim\r\n" + 
				"--use ieee.fixed_pkg.all;\r\n" + 
				"--use ieee.fixed_float_types.ALL;\r\n" + 
				"\r\n" + 
				"-- For ISE\r\n" + 
				"library ieee_proposed;\r\n" + 
				"use ieee_proposed.fixed_pkg.all;\r\n" + 
				"use ieee_proposed.fixed_float_types.ALL;\r\n" + 
				"use IEEE.numeric_std.all;\r\n" + 
				"\r\n" + 
				"---------------------------------------------------------------------\r\n");
	}
	
	private static void writeEntityDescription(StringBuilder sb,EDComponent comp)
	{
		sb.append("---------------------------------------------------------------------\r\n" + 
				"-- Entity Description\r\n" + 
				"---------------------------------------------------------------------\r\n" + 
				"entity " + comp.name + " is\r\n" + 
				"    Port (\r\n" + 
				"		   clk : in STD_LOGIC; --SYSTEM CLOCK, THIS ITSELF DOES NOT SIGNIFY TIME STEPS - AKA A SINGLE TIMESTEP MAY TAKE MANY CLOCK CYCLES\r\n" + 
				"          rst : in STD_LOGIC; --SYNCHRONOUS RESET\r\n" + 
				"          ce : in STD_LOGIC; --FOR THE SAKE OF COMPLETION ALL INTERNAL REGISTERS WILL BE CONNECTED TO THIS\r\n" + 
				"\r\n" + 
				"		   step_once_go : in STD_LOGIC; --signals to the neuron from the core that a time step is to be simulated\r\n" + 
				"          reset_model : in STD_LOGIC; --signal to all components to go into their reset state\r\n" + 
				"		   ");
		
		if (comp.name.contains("neuron_model"))
			sb.append("		   step_once_complete : out STD_LOGIC; --signals to the core that a time step has finished\r\n" + 
					  "		   eventport_in_spike_aggregate : in STD_LOGIC_VECTOR(511 downto 0);\r\n" + 
					  "		   SelectSpikesIn			: Std_logic_vector(4607 downto 0) := (others => '0');\r\n");
		else
			sb.append("		   component_done : out STD_LOGIC;\r\n");
		
		if (comp.regimes.size() > 0)
			sb.append("			current_regime_in_stdlv : in STD_LOGIC_VECTOR(1 downto 0);\r\n" + 
					  "			current_regime_out_stdlv : out STD_LOGIC_VECTOR(1 downto 0);\r\n");
		String name = "";
		

		for(Iterator<EDEventPort> i = comp.eventports.iterator(); i.hasNext(); ) {
		    EDEventPort item = i.next();
		    sb.append("			eventport_" + item.direction +  "_" + item.name + " : " + item.direction + " STD_LOGIC;\r\n"  );
		}

		for(Iterator<EDRequirement> i = comp.requirements.iterator(); i.hasNext(); ) {
			EDRequirement item = i.next(); 
			sb.append("			requirement_" + item.type +  "_" + name + item.name + " : in sfixed (" + item.integer + " downto " + item.fraction + ");\r\n"  );
		}
		writeEntitySignals(comp,sb,name);
		sb.append("           sysparam_time_timestep : in sfixed (-6 downto -22);\r\n" + 
				  "           sysparam_time_simtime : in sfixed (6 downto -22)\r\n" + 
				  "	);\r\n" + 
				"end " + comp.name + ";\r\n"
				+ "---------------------------------------------------------------------\r\n");
		
		
	}
	


	public static void writeEntitySignals(EDComponent comp, StringBuilder sb, String name)
	{
		writeEntitySignals(comp,sb,name,"");
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
					(dv.ExposureIsUsed))
			{
				sb.append("			"+prepend+"exposure_" + dv.type +  "_" + name + dv.name + 
						" : out sfixed (" + dv.integer + " downto " + dv.fraction + ");\r\n"  );
			}
		}
		for(Iterator<EDConditionalDerivedVariable> j = comp.conditionalderivedvariables.iterator(); j.hasNext(); ) {
			EDConditionalDerivedVariable dv = j.next(); 
			if (dv.exposure!= null && dv.exposure.length() > 0 && 
					(dv.ExposureIsUsed))
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
			sb.append("			"+prepend+"statevariable_" + item.type +  "_" + name + item.name + "_out : out sfixed (" + item.integer + " downto " + item.fraction + ");\r\n"  );
			sb.append("			"+prepend+"statevariable_" + item.type +  "_" + name + item.name + "_in : in sfixed (" + item.integer + " downto " + item.fraction + ");\r\n"  );
		}
		for(Iterator<EDDerivedVariable> i = comp.derivedvariables.iterator(); i.hasNext(); ) {
			EDDerivedVariable item = i.next(); 
			if ( item.ExposureIsUsed){
				sb.append("			"+prepend+"derivedvariable_" + item.type +  "_" + name + item.name + "_out : out sfixed (" + item.integer + " downto " + item.fraction + ");\r\n"  );
				sb.append("			"+prepend+"derivedvariable_" + item.type +  "_" + name + item.name + "_in : in sfixed (" + item.integer + " downto " + item.fraction + ");\r\n"  );
			}
		}
		for(Iterator<EDConditionalDerivedVariable> i = comp.conditionalderivedvariables.iterator(); i.hasNext(); ) {
			EDConditionalDerivedVariable item = i.next(); 
			if (item.ExposureIsUsed){
				sb.append("			"+prepend+"derivedvariable_" + item.type +  "_" + name + item.name + "_out : out sfixed (" + item.integer + " downto " + item.fraction + ");\r\n"  );
				sb.append("			"+prepend+"derivedvariable_" + item.type +  "_" + name + item.name + "_in : in sfixed (" + item.integer + " downto " + item.fraction + ");\r\n"  );
			}
		}
		
		for(Iterator<EDComponent> i = comp.Children.iterator(); i.hasNext(); ) {
			EDComponent item = i.next(); 
			String newName = name + item.name + "_";
			 writeEntitySignals(item, sb, newName,prepend);
		}
	
	}
	
	
	
}

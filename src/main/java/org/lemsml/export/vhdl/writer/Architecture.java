package org.lemsml.export.vhdl.writer;

import java.util.Iterator;
import java.util.List;

import org.lemsml.export.vhdl.edlems.EDCase;
import org.lemsml.export.vhdl.edlems.EDComponent;
import org.lemsml.export.vhdl.edlems.EDConditionalDerivedVariable;
import org.lemsml.export.vhdl.edlems.EDDerivedParameter;
import org.lemsml.export.vhdl.edlems.EDDerivedVariable;
import org.lemsml.export.vhdl.edlems.EDDynamic;
import org.lemsml.export.vhdl.edlems.EDEventPort;
import org.lemsml.export.vhdl.edlems.EDExponential;
import org.lemsml.export.vhdl.edlems.EDExposure;
import org.lemsml.export.vhdl.edlems.EDParameter;
import org.lemsml.export.vhdl.edlems.EDPower;
import org.lemsml.export.vhdl.edlems.EDRegime;
import org.lemsml.export.vhdl.edlems.EDRequirement;
import org.lemsml.export.vhdl.edlems.EDState;

public class Architecture {

	static void writeChildrenInstantiations(EDComponent comp, StringBuilder sb)
	{
		sb.append("---------------------------------------------------------------------\r\n" + 
				"-- Begin Internal Processes\r\n" + 
				"---------------------------------------------------------------------\r\n" + 
				"\r\n" + 
				"begin\r\n");
		if (comp.name.contains("neuron_model"))
		{
			sb.append("dut : ParamMux\r\n" + 
					"	generic map (\r\n" + 
					"		NSpikeSources	=> cNSpikeSources,\r\n" + 
					"		NOutputs		=> cNOutputs,\r\n" + 
					"		NSelectBits		=> cNSelectBits)\r\n" + 
					"	port map(\r\n" + 
					"		SpikeIn 	=> EventPort_in_spike_aggregate,\r\n" + 
					"		SelectIn	=> SelectSpikesIn,\r\n" + 
					"		SpikeOut	=> SpikeOut);\r\n" + 
					"");
		}
		sb.append("---------------------------------------------------------------------\r\n" + 
				"-- Child EDComponent Instantiations and corresponding internal variables\r\n" + 
				"---------------------------------------------------------------------\r\n");
		for(Iterator<EDComponent> z = comp.Children.iterator(); z.hasNext(); ) {
			EDComponent child = z.next(); 
			sb.append(child.name + "_uut : " + child.name + " \r\n" + 
			" port map (	clk => clk,\r\n" + 
			"				rst => rst,\r\n" + 
			"				ce => ce,\r\n" + 
			"				step_once_go => step_once_go,\r\n" + 
			"				reset_model => reset_model,\r\n" + 
			"				Component_done => " + child.name + "_Component_done,\r\n"
			);

			int count = 0;
			for(Iterator<EDEventPort> i = child.eventports.iterator(); i.hasNext(); ) {
			    EDEventPort item = i.next();
			    sb.append("			eventport_" + item.direction +  "_" + item.name + " => SpikeOut(" + count +  "),\r\n"  );
			    count++;
			}
			writeEDComponentMap(child,sb,"",child,comp.state);
			sb.append("           sysparam_time_timestep => sysparam_time_timestep,\r\n" + 
					"           sysparam_time_simtime => sysparam_time_simtime\r\n" + 
					"		   );\r\n");
			
			for(Iterator<EDExposure> i = child.exposures.iterator(); i.hasNext(); ) {
				EDExposure EDExposure = i.next(); 
				sb.append("Exposure_" + EDExposure.type +  "_" + child.name + "_" + EDExposure.name + " <= Exposure_" + EDExposure.type +  "_" + child.name + "_" + EDExposure.name + "_internal;\r\n");
			}
			for(Iterator<EDState> i = child.state.iterator(); i.hasNext(); ) {
				EDState state = i.next(); 
				sb.append("statevariable_" + state.type +  "_" + child.name + "_"  + state.name + "_out <= statevariable_" + state.type +  "_"+ child.name + "_"  + state.name + "_out_int;"  );
				sb.append("statevariable_" + state.type +  "_"+ child.name + "_"  + state.name + "_in_int <= statevariable_" + state.type +  "_" + child.name + "_" + state.name + "_in;");
			}	 
			
		}
	}
	

	private static void writeEDComponentMap(EDComponent comp, StringBuilder sb, String name,EDComponent parent, List<EDState> states)
	{

		for(Iterator<EDParameter> i = comp.parameters.iterator(); i.hasNext(); ) {
			EDParameter item = i.next(); 
			sb.append("			param_" + item.type +  "_" + name + item.name + " => param_"  + item.type +  "_" + parent.name + "_" + name + item.name + ",\r\n"  );
		}

		for(Iterator<EDRequirement> i = comp.requirements.iterator(); i.hasNext(); ) {
			EDRequirement item = i.next(); 
			//check if requirement exists in parent EDComponent
			boolean stateexists = false;
			for(Iterator<EDState> j = states.iterator(); j.hasNext(); ) {
				EDState state = j.next(); 
				if (state.name.matches(item.name))
					stateexists = true;
			}
			if (stateexists )
				sb.append("			requirement_" + item.type +  "_" + name + item.name + " => statevariable_" + item.type +  "_" +  name + item.name + "_in,\r\n"  );
			else
				sb.append("			requirement_" + item.type +  "_" + name + item.name + " => requirement_" + item.type +  "_"  +  item.name + ",\r\n"  );
			
		}
		
		for(Iterator<EDExposure> i = comp.exposures.iterator(); i.hasNext(); ) {
			EDExposure item = i.next(); 
			sb.append("			Exposure_" + item.type +  "_" + name + item.name + " => Exposure_" + item.type +  "_" + parent.name + "_" +  name + item.name + "_internal,\r\n"  );
		}

		for(Iterator<EDState> i = comp.state.iterator(); i.hasNext(); ) {
			EDState item = i.next(); 
			sb.append("			statevariable_" + item.type  +  "_" + name +item.name + "_out => statevariable_" + item.type +  "_" + parent.name + "_" +  name + item.name + "_out_int,\r\n"  );
			sb.append("			statevariable_" + item.type  +  "_" + name +item.name + "_in => statevariable_" + item.type +  "_" + parent.name + "_" +  name + item.name + "_in_int,\r\n"  );
		}
		for(Iterator<EDDerivedVariable> i = comp.derivedvariables.iterator(); i.hasNext(); ) {
			EDDerivedVariable item = i.next(); 
			sb.append("			derivedvariable_" + item.type  +  "_" + name +item.name + "_out => derivedvariable_" + item.type +  "_" + parent.name + "_" +  name + item.name + "_out,\r\n"  );
			sb.append("			derivedvariable_" + item.type  +  "_" + name +item.name + "_in => derivedvariable_" + item.type +  "_" +  parent.name + "_" + name + item.name + "_in,\r\n"  );
		}
		for(Iterator<EDConditionalDerivedVariable> i = comp.conditionalderivedvariables.iterator(); i.hasNext(); ) {
			EDConditionalDerivedVariable item = i.next(); 
			sb.append("			derivedvariable_" + item.type  +  "_" + name +item.name + "_out => derivedvariable_" + item.type +  "_" + parent.name + "_" +  name + item.name + "_out,\r\n"  );
			sb.append("			derivedvariable_" + item.type  +  "_" + name +item.name + "_in => derivedvariable_" + item.type +  "_" + parent.name + "_" +  name + item.name + "_in,\r\n"  );
		}
		
		for(Iterator<EDComponent> i = comp.Children.iterator(); i.hasNext(); ) {
			EDComponent item = i.next(); 
			String newName = name  + item.name + "_";
			writeEDComponentMap2(item, sb, newName,parent);
		}
		
	
	}
	
	private static void writeEDComponentMap2(EDComponent comp, StringBuilder sb, String name,EDComponent parent)
	{

		for(Iterator<EDParameter> i = comp.parameters.iterator(); i.hasNext(); ) {
			EDParameter item = i.next(); 
			sb.append("			param_" + item.type +  "_" + name + item.name + " => param_"  + item.type +  "_" + parent.name + "_" + name + item.name + ",\r\n"  );
		}

		for(Iterator<EDExposure> i = comp.exposures.iterator(); i.hasNext(); ) {
			EDExposure item = i.next(); 
			sb.append("			Exposure_" + item.type +  "_" + name + item.name + " => Exposure_" + item.type +  "_" + parent.name + "_" +  name + item.name + "_internal,\r\n"  );
		}

		for(Iterator<EDState> i = comp.state.iterator(); i.hasNext(); ) {
			EDState item = i.next(); 
			sb.append("			statevariable_" + item.type  +  "_" + name +item.name + "_out => statevariable_" + item.type +  "_" + parent.name + "_" +  name + item.name + "_out_int,\r\n"  );
			sb.append("			statevariable_" + item.type  +  "_" + name +item.name + "_in => statevariable_" + item.type +  "_" + parent.name + "_" +  name + item.name + "_in_int,\r\n"  );
		}
		for(Iterator<EDDerivedVariable> i = comp.derivedvariables.iterator(); i.hasNext(); ) {
			EDDerivedVariable item = i.next(); 
			sb.append("			derivedvariable_" + item.type  +  "_" + name +item.name + "_out => derivedvariable_" + item.type +  "_" + parent.name + "_" +  name + item.name + "_out,\r\n"  );
			sb.append("			derivedvariable_" + item.type  +  "_" + name +item.name + "_in => derivedvariable_" + item.type +  "_" +  parent.name + "_" + name + item.name + "_in,\r\n"  );
		}
		for(Iterator<EDConditionalDerivedVariable> i = comp.conditionalderivedvariables.iterator(); i.hasNext(); ) {
			EDConditionalDerivedVariable item = i.next(); 
			sb.append("			derivedvariable_" + item.type  +  "_" + name +item.name + "_out => derivedvariable_" + item.type +  "_" + parent.name + "_" +  name + item.name + "_out,\r\n"  );
			sb.append("			derivedvariable_" + item.type  +  "_" + name +item.name + "_in => derivedvariable_" + item.type +  "_" + parent.name + "_" +  name + item.name + "_in,\r\n"  );
		}
		
		for(Iterator<EDComponent> i = comp.Children.iterator(); i.hasNext(); ) {
			EDComponent item = i.next(); 
			String newName = name  + item.name + "_";
			writeEDComponentMap2(item, sb, newName,parent);
		}
		
	
	}
	

	static void writeArchitecture(EDComponent comp, StringBuilder sb, String name)
	{
		sb.append("-------------------------------------------------------------------------------------------\r\n" + 
				"-- Architecture Begins TEST\r\n" + 
				"------------------------------------------------------------------------------------------- \r\n" + 
				"\r\n" + 
				"architecture RTL of " + name + " is\r\n" + 
				"signal COUNT : unsigned(2 downto 0) := \"000\";\r\n" + 
				"signal childrenCombined_Component_done_single_shot_fired : STD_LOGIC := '0';\r\n" + 
				"signal childrenCombined_Component_done_single_shot : STD_LOGIC := '0';\r\n" + 
				"signal childrenCombined_Component_done : STD_LOGIC := '0';\r\n" + 
				"signal Component_done_int : STD_LOGIC := '0';\r\n" + 
				"\r\n" + 
				"\r\n" + 
				"signal subprocess_der_int_pre_ready : STD_LOGIC := '0';\r\n" + 
				"signal subprocess_der_int_ready : STD_LOGIC := '0';\r\n" + 
				"signal subprocess_der_ready : STD_LOGIC := '0';\r\n" + 
				"signal subprocess_dyn_int_pre_ready : STD_LOGIC := '0';\r\n" + 
				"signal subprocess_dyn_int_ready : STD_LOGIC := '0';\r\n" + 
				"signal subprocess_dyn_ready : STD_LOGIC := '0';\r\n" + 
				"signal subprocess_model_ready : STD_LOGIC := '1';\r\n" + 
				"signal subprocess_all_ready : STD_LOGIC := '0';");
		
		if (name.contains("neuron_model"))
			sb.append("\r\n" + 
					"\r\n" + 
					"signal step_once_complete_fired : STD_LOGIC := '0';\r\n" + 
					"signal Component_done : STD_LOGIC := '0';\r\n" + 
					"\r\n" + 
					"\r\n" + 
					"constant cNSpikeSources : integer := 512;	-- The number of spike sources.\r\n" + 
					"constant cNOutputs		: integer := 512;	-- The number of Synapses in the neuron model.\r\n" + 
					"constant cNSelectBits	: integer := 9;		-- Log2(NOutputs), rounded up.\r\n" + 
					"\r\n" + 
					"component ParamMux\r\n" + 
					"	generic( \r\n" + 
					"		NSpikeSources 	: integer := 512;	-- The number of spike sources.\r\n" + 
					"		NOutputs		: integer := 512;	-- The number of Synapses in the neuron model.\r\n" + 
					"		NSelectBits		: integer := 5);	-- Log2(NSpikeSources), rounded up.\r\n" + 
					"	port(\r\n" + 
					"		SpikeIn			: In  Std_logic_vector(NSpikeSources-1 downto 0);\r\n" + 
					"		SelectIn		: In  Std_logic_vector((NOutputs*NSelectBits)-1 downto 0);\r\n" + 
					"		SpikeOut		: Out Std_logic_vector((NOutputs-1) downto 0));\r\n" + 
					"end component;\r\n" + 
					"signal SpikeOut			: Std_logic_vector((cNOutputs-1) downto 0);\r\n" + 
					"\r\n");
		
		boolean expExists = false;
		boolean powExists = false;
		for(Iterator<EDState> i = comp.state.iterator(); i.hasNext(); ) {
			EDState state = i.next(); 
			for(Iterator<EDRegime> j = comp.regimes.iterator(); j.hasNext(); ) {
				EDRegime regime = j.next(); 
				for(Iterator<EDDynamic> z = regime.dynamics.iterator(); z.hasNext(); ) {
					EDDynamic EDDynamic = z.next(); 
					if (EDDynamic.name.matches(state.name))
					{
						for(Iterator<EDExponential> k = EDDynamic.Exponentials.iterator(); k.hasNext(); ) {
							EDExponential EDExponential = k.next();
							expExists = true;
							writeEDExponentialSignals(sb, regime.name + "_"  +state.name, EDExponential);
						}

						for(Iterator<EDPower> k = EDDynamic.Powers.iterator(); k.hasNext(); ) {
							EDPower EDPower = k.next();
							powExists = true;
							writeEDPowerSignals(sb, regime.name + "_"  +state.name, EDPower);
						}
					}
				}
			}
			for(Iterator<EDDynamic> z = comp.dynamics.iterator(); z.hasNext(); ) {
				EDDynamic EDDynamic = z.next(); 
				if (EDDynamic.name.matches(state.name))
				{
					EDRegime regime = new EDRegime();
					regime.name = "noregime";
					for(Iterator<EDExponential> k = EDDynamic.Exponentials.iterator(); k.hasNext(); ) {
						EDExponential EDExponential = k.next();
						expExists = true;
						writeEDExponentialSignals(sb, regime.name + "_"  +state.name, EDExponential);
					}

					for(Iterator<EDPower> k = EDDynamic.Powers.iterator(); k.hasNext(); ) {
						EDPower EDPower = k.next();
						powExists = true;
						writeEDPowerSignals(sb, regime.name + "_"  +state.name, EDPower);
					}
				}
			
			}
		}
		for(Iterator<EDDerivedVariable> i = comp.derivedvariables.iterator(); i.hasNext(); ) {
			EDDerivedVariable EDDerivedVariable = i.next();
			for(Iterator<EDExponential> k = EDDerivedVariable.Exponentials.iterator(); k.hasNext(); ) {
				EDExponential EDExponential = k.next();
				expExists = true;
				writeEDExponentialSignals(sb, EDDerivedVariable.name, EDExponential);
			}

			for(Iterator<EDPower> k = EDDerivedVariable.Powers.iterator(); k.hasNext(); ) {
				EDPower EDPower = k.next();
				powExists = true;
				writeEDPowerSignals(sb, EDDerivedVariable.name, EDPower);
			}
		}
		
		for(Iterator<EDConditionalDerivedVariable> i = comp.conditionalderivedvariables.iterator(); i.hasNext(); ) {
			EDConditionalDerivedVariable EDConditionalDerivedVariable = i.next();
			for(Iterator<EDCase> k = EDConditionalDerivedVariable.cases.iterator(); k.hasNext(); ) {
				EDCase thisCase = k.next();
				for(Iterator<EDExponential> j = thisCase.Exponentials.iterator(); j.hasNext(); ) {
					EDExponential EDExponential = j.next();
					expExists = true;
					writeEDExponentialSignals(sb, EDConditionalDerivedVariable.name, EDExponential);
				}

				for(Iterator<EDPower> j = thisCase.Powers.iterator(); j.hasNext(); ) {
					EDPower EDPower = j.next();
					powExists = true;
					writeEDPowerSignals(sb, EDConditionalDerivedVariable.name, EDPower);
				}
			}
			
		}
		
		for(Iterator<EDState> i = comp.state.iterator(); i.hasNext(); ) {
			EDState state = i.next(); 
			for(Iterator<EDRegime> j = comp.regimes.iterator(); j.hasNext(); ) {
				EDRegime regime = j.next(); 
				for(Iterator<EDDynamic> z = regime.dynamics.iterator(); z.hasNext(); ) {
					EDDynamic EDDynamic = z.next(); 
					if (EDDynamic.name.matches(state.name))
					{
						sb.append("signal statevariable_" + state.type +  "_" + regime.name +  "_" + state.name + "_temp_1 : sfixed (" + state.integer + " downto " + state.fraction + ");\r\n"  );
						sb.append("signal statevariable_" + state.type +  "_" + regime.name +  "_" + state.name + "_temp_1_next : sfixed (" + state.integer + " downto " + state.fraction + ");\r\n"  );
					}
				}
			}
			for(Iterator<EDDynamic> z = comp.dynamics.iterator(); z.hasNext(); ) {
				EDDynamic EDDynamic = z.next(); 
				if (EDDynamic.name.matches(state.name))
				{
					sb.append("signal statevariable_" + state.type +  "_noregime_" + state.name + "_temp_1 : sfixed (" + state.integer + " downto " + state.fraction + ");\r\n"  );
					sb.append("signal statevariable_" + state.type +  "_noregime_" + state.name + "_temp_1_next : sfixed (" + state.integer + " downto " + state.fraction + ");\r\n"  );
				
				}
			
			}
		}
		
		if (expExists)
			sb.append("Component ParamExp is\r\n" + 
					"	port(\r\n" + 
					"		clk		: In  Std_logic;\r\n" + 
					"		rst		: In  Std_logic;\r\n" + 
					"		Start	: In  Std_logic;\r\n" + 
					"		Done	: Out  Std_logic;\r\n" + 
					"		X		: In sfixed(20 downto -20);\r\n" + 
					"		Output	: Out sfixed(20 downto -20)\r\n" + 
					"		);\r\n" + 
					"end Component;\r\n");
		
		if (powExists)
			sb.append("Component ParamPow is\r\n" + 
					"	port(\r\n" + 
					"		clk		: In  Std_logic;\r\n" + 
					"		rst		: In  Std_logic;\r\n" + 
					"		Start	: In  Std_logic;\r\n" + 
					"		Done	: Out  Std_logic;\r\n" + 
					"		X		: In sfixed(20 downto -20);\r\n" + 
					"		A		: In sfixed(20 downto -20);\r\n" + 
					"		Output	: Out sfixed(20 downto -20)\r\n" + 
					"		);\r\n" + 
					"end Component; \r\n");
		
		sb.append("\r\n" + 
				"---------------------------------------------------------------------\r\n" + 
				"-- Derived Variables and parameters\r\n" + 
				"---------------------------------------------------------------------\r\n");
		for(Iterator<EDDerivedVariable> z = comp.derivedvariables.iterator(); z.hasNext(); ) {
			EDDerivedVariable EDDerivedVariable = z.next(); 
			sb.append("signal DerivedVariable_" + EDDerivedVariable.type +  "_" + EDDerivedVariable.name + " : sfixed (" + EDDerivedVariable.integer + " downto " + EDDerivedVariable.fraction + ");");
			sb.append("signal DerivedVariable_" + EDDerivedVariable.type +  "_" + EDDerivedVariable.name + "_next : sfixed (" + EDDerivedVariable.integer + " downto " + EDDerivedVariable.fraction + ");");
			
		}
		for(Iterator<EDConditionalDerivedVariable> z = comp.conditionalderivedvariables.iterator(); z.hasNext(); ) {
			EDConditionalDerivedVariable EDConditionalDerivedVariable = z.next(); 
			sb.append("signal DerivedVariable_" + EDConditionalDerivedVariable.type +  "_" + EDConditionalDerivedVariable.name + " : sfixed (" + EDConditionalDerivedVariable.integer + " downto " + EDConditionalDerivedVariable.fraction + ");");
			sb.append("signal DerivedVariable_" + EDConditionalDerivedVariable.type +  "_" + EDConditionalDerivedVariable.name + "_next : sfixed (" + EDConditionalDerivedVariable.integer + " downto " + EDConditionalDerivedVariable.fraction + ");");
			
		}
		for(Iterator<EDDerivedParameter> z = comp.derivedparameters.iterator(); z.hasNext(); ) {
			EDDerivedParameter EDDerivedParameter = z.next(); 
			sb.append("signal DerivedParameter_" + EDDerivedParameter.type +  "_" + EDDerivedParameter.name + " : sfixed (" + EDDerivedParameter.integer + " downto " + EDDerivedParameter.fraction + ");");
			sb.append("signal DerivedParameter_" + EDDerivedParameter.type +  "_" + EDDerivedParameter.name + "_next : sfixed (" + EDDerivedParameter.integer + " downto " + EDDerivedParameter.fraction + ");");
			
		}
		

		sb.append("\r\n" + 
				"---------------------------------------------------------------------\r\n\r\n" + 
				"---------------------------------------------------------------------\r\n" + 
				"-- EDState internal Variables\r\n" + 
				"---------------------------------------------------------------------\r\n" );
		
		for(Iterator<EDState> z = comp.state.iterator(); z.hasNext(); ) {
			EDState state = z.next(); 
			sb.append("signal statevariable_" + state.type +  "_" + state.name + "_next : sfixed (" + state.integer + " downto " + state.fraction + ");");
			
		}
		
		sb.append("\r\n" + 
				"---------------------------------------------------------------------\r\n\r\n" + 
				"---------------------------------------------------------------------\r\n" + 
				"-- Output Port internal Variables\r\n" + 
				"---------------------------------------------------------------------\r\n" );

		for(Iterator<EDEventPort> z = comp.eventports.iterator(); z.hasNext(); ) {
			EDEventPort EDEventPort = z.next(); 
			sb.append("signal EventPort_" + EDEventPort.direction + "_" + EDEventPort.name + "_internal : std_logic := '0'; ");
			
		}
		
		sb.append("\r\n" + 
				"---------------------------------------------------------------------\r\n" );
		if (comp.regimes.size() > 0)
		{
			String regimes = "";
			for (int i = 0; i <comp.regimes.size();i++)
			{
				regimes = regimes +  comp.regimes.get(i).name;
				if (i < comp.regimes.size() - 1)
					regimes = regimes + ",";
			}
			sb.append("type regime_type is (" + regimes + ");\r\n"
					+ "signal current_regime_in_int: regime_type;\r\n" + 
					"signal next_regime: regime_type;\r\n"
					+ "function CONV_STDLV_TO_REGIME (DATA :std_logic_vector) return regime_type is\r\n" + 
					"begin\r\n" + 
					"  return regime_type'val(to_integer(unsigned(DATA)));\r\n" + 
					"end CONV_STDLV_TO_REGIME;\r\n" + 
					"\r\n" + 
					"function CONV_REGIME_TO_STDLV (regime :regime_type) return std_logic_vector is \r\n" + 
					"begin\r\n" + 
					"  return std_logic_vector(to_unsigned(regime_type'pos(regime),2));\r\n" + 
					"end CONV_REGIME_TO_STDLV;\r\n" + 
					"---------------------------------------------------------------------\r\n");
		}

	}
	
	static void writeChildrenDeclarations(StringBuilder sb, EDComponent comp)
	{
		sb.append("---------------------------------------------------------------------\r\n" + 
				"-- Child Components\r\n" + 
				"---------------------------------------------------------------------\r\n");
		for(Iterator<EDComponent> z = comp.Children.iterator(); z.hasNext(); ) {
			EDComponent child = z.next(); 
			sb.append("component " + child.name + " \r\n" + 
			"    Port (\r\n" + 
			"		   clk : in STD_LOGIC; --SYSTEM CLOCK, THIS ITSELF DOES NOT SIGNIFY TIME STEPS - AKA A SINGLE TIMESTEP MAY TAKE MANY CLOCK CYCLES\r\n" + 
			"          rst : in STD_LOGIC; --SYNCHRONOUS RESET\r\n" + 
			"          ce : in STD_LOGIC; --FOR THE SAKE OF COMPLETION ALL INTERNAL REGISTERS WILL BE CONNECTED TO THIS\r\n" + 
			"\r\n" + 
			"		   step_once_go : in STD_LOGIC; --signals to the neuron from the core that a time step is to be simulated\r\n" + 
			"          reset_model : in STD_LOGIC; --signal to all EDComponents to go into their reset state\r\n" + 
			"		   ");
	
			sb.append("		   Component_done : out STD_LOGIC;\r\n");
	
			String name = "";
			for(Iterator<EDEventPort> i = child.eventports.iterator(); i.hasNext(); ) {
			    EDEventPort item = i.next();
			    sb.append("			eventport_" + item.direction +  "_" + item.name + " : " + item.direction + " STD_LOGIC;\r\n"  );
			}
			for(Iterator<EDRequirement> i = child.requirements.iterator(); i.hasNext(); ) {
				EDRequirement item = i.next(); 
				sb.append("			requirement_" + item.type +  "_" + name + item.name + " : in sfixed (" + item.integer + " downto " + item.fraction + ");\r\n"  );
			}
			Entity.writeEntitySignals(child,sb,name);
			sb.append("           sysparam_time_timestep : in sfixed (-6 downto -22);\r\n" + 
				  "           sysparam_time_simtime : in sfixed (6 downto -22)\r\n" + 
				  "	);\r\n" + 
				"end component;\r\n");
			
			sb.append("signal " + child.name + "_Component_done : STD_LOGIC ; ");
			writeChildSignals(child,sb,"");
			
			
			sb.append("---------------------------------------------------------------------\r\n");
		}
		
	}
	
	private static void writeChildSignals(EDComponent child,StringBuilder sb,String name)
	{
		for(Iterator<EDExposure> i = child.exposures.iterator(); i.hasNext(); ) {
			EDExposure EDExposure = i.next(); 
			sb.append("signal Exposure_" + EDExposure.type +  "_" + name + child.name + "_" + EDExposure.name + "_internal : sfixed (" + EDExposure.integer + " downto " + EDExposure.fraction + ");\r\n");
			sb.append("signal statevariable_" + EDExposure.type +  "_" +name + child.name + "_" + EDExposure.name + "_out_int : sfixed (" + EDExposure.integer + " downto " + EDExposure.fraction + ");\r\n");
			sb.append("signal statevariable_" + EDExposure.type +  "_" +name + child.name + "_" + EDExposure.name + "_in_int : sfixed (" + EDExposure.integer + " downto " + EDExposure.fraction + ");\r\n");
			
		}
		for(Iterator<EDComponent> i = child.Children.iterator(); i.hasNext(); ) {
			EDComponent child2 = i.next(); 
			writeChildSignals(child2,sb,name + child.name + "_");
		}
	}

	
	
	private static void writeEDPowerSignals(StringBuilder sb, String name, EDPower EDPower)
	{
		sb.append("signal pre_pow_" + name + "_" + EDPower.name + "_A : sfixed(20 downto -20);\r\n");
		sb.append("signal pre_pow_" + name + "_" + EDPower.name + "_A_next : sfixed(20 downto -20);\r\n");
		sb.append("signal pre_pow_" + name + "_" + EDPower.name + "_X : sfixed(20 downto -20);\r\n");
		sb.append("signal pre_pow_" + name + "_" + EDPower.name + "_X_next : sfixed(20 downto -20);\r\n");
		sb.append("signal pow_" + name + "_" + EDPower.name + " : sfixed(20 downto -20);\r\n");
	}
	
	private static void writeEDExponentialSignals(StringBuilder sb, String name, EDExponential EDExponential )
	{
		sb.append("signal pre_exp_" + name + "_" + EDExponential.name + " : sfixed(20 downto -20);\r\n"  );
		sb.append("signal pre_exp_" + name + "_" + EDExponential.name + "_next : sfixed(20 downto -20);\r\n"  );
		sb.append("signal exp_" + name + "_" +  EDExponential.name + " : sfixed(20 downto -20);\r\n");						
	
	}
}

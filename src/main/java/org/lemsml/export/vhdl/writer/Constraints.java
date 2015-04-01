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
import org.lemsml.export.vhdl.edlems.EDParameter;
import org.lemsml.export.vhdl.edlems.EDPower;
import org.lemsml.export.vhdl.edlems.EDRegime;
import org.lemsml.export.vhdl.edlems.EDRequirement;
import org.lemsml.export.vhdl.edlems.EDSimulation;
import org.lemsml.export.vhdl.edlems.EDState;

public class Constraints {
	
	public static void writeConstraintsFile(EDSimulation sim, StringBuilder sb)
	{
		for(Iterator<EDComponent> z = sim.neuronComponents.iterator(); z.hasNext(); ) {
			EDComponent child = z.next(); 
			int count = 0;
			writeConstraints(child,sb,count,"");
		}
	}

	static int writeConstraints(EDComponent comp, StringBuilder sb, int count, String path)
	{
		for(Iterator<EDDerivedVariable> j = comp.derivedvariables.iterator(); j.hasNext(); ) {
			EDDerivedVariable derivedvariable = j.next(); 
			if (!derivedvariable.isEmpty)
			{
				if (derivedvariable.value.contains("/"))
				{
					count++;
					//sb.append("NET \"derivedvariable_" + derivedvariable.type +  "_" + derivedvariable.name + "_next\"  TNM = \"TNM_DERIVED"+count+"\";\r\n");
					sb.append("NET \"U_NM/top_synth_uut/neuron_model_uut/" + path + 
							"derivedvariable_" + 
							derivedvariable.type +  "_" + 
							derivedvariable.name +"*\"" +
									" TNM = FFS \"tnm_multipath" + count + "\";");
					sb.append("TIMESPEC \"ts_multipath"+count+"\" = TO \"tnm_multipath" + count +
							"\"  \"TS_U_NC_U_NC_CLK_iUserClk\" / 10;\r\n");
				}
			}
		}
		for(Iterator<EDConditionalDerivedVariable> j = comp.conditionalderivedvariables.iterator(); j.hasNext(); ) {
			EDConditionalDerivedVariable conditionalderivedvariable = j.next(); 
			for(Iterator<EDCase> l = conditionalderivedvariable.cases.iterator(); l.hasNext(); ) {
				EDCase thisEDCase = l.next(); 
				if (thisEDCase.value.contains("/"))
				{
					count++;
					sb.append("NET \"U_NM/top_synth_uut/neuron_model_uut/" + path + "derivedvariable_" + 
					conditionalderivedvariable.type +  "_" + 
							conditionalderivedvariable.name +"*\"" +
							" TNM = FFS \"tnm_multipath" + count + "\";");
					sb.append("TIMESPEC \"ts_multipath"+count+"\" = TO \"tnm_multipath" + count +
							"\"  \"TS_U_NC_U_NC_CLK_iUserClk\" / 10;\r\n");
				}
			}
		}
		
		for(Iterator<EDState> j = comp.state.iterator(); j.hasNext(); ) {
			EDState state = j.next(); 
			for(Iterator<EDRegime> k = comp.regimes.iterator(); k.hasNext(); ) {
				EDRegime regime = k.next(); 
				for(Iterator<EDDynamic> l = regime.dynamics.iterator(); l.hasNext(); ) {
					EDDynamic dynamic = l.next(); 
					if (dynamic.name.matches(state.name))
					{
						if (dynamic.Dynamics.contains("/"))
						{
							count++;
							sb.append("NET \"U_NM/top_synth_uut/neuron_model_uut/" + path + "statevariable_" + state.type 
									+ "_" + regime.name +  "_" + state.name 
											+ "_temp_1_next*\" TNM = FFS \"tnm_multipath" + count + "\";");
							sb.append("TIMESPEC \"ts_multipath"+count+"\" = TO \"tnm_multipath" + count +
									"\"  \"TS_U_NC_U_NC_CLK_iUserClk\" / 10;\r\n");
							}
					}
				}
			}
			for(Iterator<EDDynamic> l = comp.dynamics.iterator(); l.hasNext(); ) {
				EDDynamic dynamic = l.next(); 
				if (dynamic.name.matches(state.name))
				{
					if (dynamic.Dynamics.contains("/"))
					{
						count++;
						sb.append("NET \"U_NM/top_synth_uut/neuron_model_uut/" + path + "statevariable_"
								+state.type + "_noregime_" + state.name
										+ "_temp_1*\" TNM = FFS \"tnm_multipath" + count + "\";");
						sb.append("TIMESPEC \"ts_multipath"+count+"\" = TO \"tnm_multipath" + count +
								"\"  \"TS_U_NC_U_NC_CLK_iUserClk\" / 10;\r\n");
					}
				}
			}
			
		}

		for(EDComponent child : comp.Children)
		{
			count = writeConstraints(child,sb,count,path + child.name + "_uut/");
		}
		return count;
	}
}

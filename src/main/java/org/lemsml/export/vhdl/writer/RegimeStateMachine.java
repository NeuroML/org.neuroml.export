package org.lemsml.export.vhdl.writer;

import java.util.Iterator;

import org.lemsml.export.vhdl.edlems.EDCase;
import org.lemsml.export.vhdl.edlems.EDComponent;
import org.lemsml.export.vhdl.edlems.EDCondition;
import org.lemsml.export.vhdl.edlems.EDConditionalDerivedVariable;
import org.lemsml.export.vhdl.edlems.EDDerivedParameter;
import org.lemsml.export.vhdl.edlems.EDDerivedVariable;
import org.lemsml.export.vhdl.edlems.EDDynamic;
import org.lemsml.export.vhdl.edlems.EDEvent;
import org.lemsml.export.vhdl.edlems.EDEventPort;
import org.lemsml.export.vhdl.edlems.EDOnEntry;
import org.lemsml.export.vhdl.edlems.EDRegime;
import org.lemsml.export.vhdl.edlems.EDState;
import org.lemsml.export.vhdl.edlems.EDStateAssignment;
import org.lemsml.export.vhdl.edlems.EDTransition;

public class RegimeStateMachine {

	static void writeEDRegimeEDStateMachine(StringBuilder sb, EDComponent comp)
	{
		if (comp.regimes.size() > 0)
		{
			sb.append("---------------------------------------------------------------------\r\n" + 
					"-- EDRegime EDState Machine Process\r\n" + 
					"---------------------------------------------------------------------\r\n" + 
					"");
			String defaultRegime = "none";
			for(Iterator<EDRegime> j = comp.regimes.iterator(); j.hasNext(); ) {
				EDRegime regime = j.next(); 
				if (regime.isDefault != null && regime.isDefault.matches("default"))
					defaultRegime = regime.name;
			}
			StringBuilder sensitivityList = new StringBuilder();
			sensitivityList.append("sysparam_time_simtime,current_regime_in_int,init_model,statevariable_voltage_v_in");
			if (comp.regimes.size() > 0)
			{
				for(Iterator<EDRegime> l = comp.regimes.iterator(); l.hasNext(); ) {
					EDRegime regime = l.next();
					for(Iterator<EDCondition> i = regime.conditions.iterator(); i.hasNext(); ) {
						EDCondition condition = i.next();
						sensitivityList.append("," + condition.sensitivityList);
					}
				}				
			}
			sb.append("\r\n" + 
					"regime_state_process_comb :process (" + sensitivityList.toString()
					+ ")\r\n" + 
					"begin \r\n" + 
					"  next_regime <= current_regime_in_int;\r\n" + 
					"\r\n" + 
					"  if init_model = '1' then  \r\n" + 
					"    next_regime <= " + defaultRegime + ";\r\n" + 
					"  else\r\n");
			for(Iterator<EDRegime> j = comp.regimes.iterator(); j.hasNext(); ) {
				EDRegime regime = j.next(); 
				for(Iterator<EDEvent> k= regime.events.iterator(); k.hasNext(); ) {
					EDEvent event = k.next(); 
					for(Iterator<EDTransition> l = event.transitions.iterator(); l.hasNext(); ) {
						EDTransition transition = l.next(); 
							sb.append("    if ( current_regime_in_int = " + regime.name + " ) and eventport_in_" + event.name + ".name = '1' then\r\n" + 
									"      next_regime <= " + transition.name + ";\r\n" + 
									"    end if;\r\n");
					}
				}
				for(Iterator<EDCondition> k= regime.conditions.iterator(); k.hasNext(); ) {
					EDCondition condition = k.next(); 
					for(Iterator<EDTransition> l = condition.transitions.iterator(); l.hasNext(); ) {
						EDTransition transition = l.next(); 
							sb.append("    if ( current_regime_in_int = " + regime.name + " ) and " + condition.condition + " then\r\n" + 
									"      next_regime <= " + transition.name + ";\r\n" + 
									"    end if;\r\n");
					}
				}
			}
			
			sb.append("\r\n" + 
					"  end if;\r\n" + 
					"\r\n" + 
					"end process;\r\n" + 
					"\r\n" + 
					"current_regime_out_stdlv <= CONV_REGIME_TO_STDLV(next_regime);\r\n" + 
					"current_regime_in_int <= CONV_STDLV_TO_REGIME(current_regime_in_stdlv);\r\n" + 
					"---------------------------------------------------------------------\r\n");
			
			
		}
	}
	
}

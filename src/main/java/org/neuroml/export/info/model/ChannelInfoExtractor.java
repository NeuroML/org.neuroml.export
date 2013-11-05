/**
 * 
 */
package org.neuroml.export.info.model;

import java.awt.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.lemsml.jlems.core.expression.ParseError;
import org.lemsml.jlems.core.sim.ContentError;
import org.neuroml.model.GateHHRates;
import org.neuroml.model.GateHHRatesInf;
import org.neuroml.model.GateHHRatesTau;
import org.neuroml.model.GateHHTauInf;
import org.neuroml.model.GateHHUndetermined;
import org.neuroml.model.HHRate;
import org.neuroml.model.IonChannel;

/**
 * @author borismarin
 *
 */
public class ChannelInfoExtractor {
	private InfoNode gates = new InfoNode();

    /*
    private List<GateHHUndetermined> gate;
    private List<GateHHRates> gateHHrates;
    private List<GateHHRatesTau> gateHHratesTau;
    private List<GateHHTauInf> gateHHtauInf;
    private List<GateHHRatesInf> gateHHratesInf;
    */
	

	public ChannelInfoExtractor(IonChannel chan) 
	{
		for (GateHHUndetermined g : chan.getGate()){
			InfoNode gate = new InfoNode();
			gate.put("instances", g.getInstances());
			gates.put("gate " + g.getId(), gate);
		}


		for (GateHHRates g : chan.getGateHHrates()){
			InfoNode gate = new InfoNode();

			gate.put("instances", g.getInstances());

			String fwd = new CMLStandardExpression<HHRate>(g.getForwardRate()).toString();
			String rev = new CMLStandardExpression<HHRate>(g.getReverseRate()).toString();

			gate.put("forward rate", fwd);
			gate.put("reverse rate", rev);

			gates.put("gate " + g.getId(), gate);
		}

		for(GateHHRatesInf g : chan.getGateHHratesInf()){
			InfoNode gate = new InfoNode();
			gate.put("instances", g.getInstances());

			gates.put("gate " + g.getId(), gate);
		}

		for(GateHHRatesTau g : chan.getGateHHratesTau()){
			InfoNode gate = new InfoNode();
			gate.put("instances", g.getInstances());

			gates.put("gate " + g.getId(), gate);
		}

		for(GateHHTauInf g : chan.getGateHHtauInf()){
			InfoNode gate = new InfoNode();
			gate.put("instances", g.getInstances());

			gates.put("gate " + g.getId(), gate);
		}
	}

	public InfoNode getGates() {
		return gates;
	}
	

}



class HHChannelCharacteristicFunction{
	private String name;
	private CMLExpression function;
	
}
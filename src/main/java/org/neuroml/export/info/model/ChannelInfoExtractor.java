/**
 * 
 */
package org.neuroml.export.info.model;

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

			String fwd = new CMLExpressionInfoExtractor<HHRate>(g.getForwardRate()).toString();
			String rev = new CMLExpressionInfoExtractor<HHRate>(g.getReverseRate()).toString();

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


/**
 * 
 */
package org.neuroml.export.info.model;

import org.neuroml.model.GateHHRates;
import org.neuroml.model.GateHHRatesInf;
import org.neuroml.model.GateHHRatesTau;
import org.neuroml.model.GateHHTauInf;
import org.neuroml.model.GateHHUndetermined;
import org.neuroml.model.IonChannel;

/**
 * @author borismarin
 *
 */
public class ChannelInfoExtractor {
	private InfoNode gates = new InfoNode();


	
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

			HHRateProcessor rateinfo = new HHRateProcessor(g);

			ChannelMLHHExpression fwd = rateinfo.getForwardRate();
			ChannelMLHHExpression rev = rateinfo.getReverseRate();

			gate.put("forward rate", fwd.toString());
			gate.put("reverse rate", rev.toString());
			gate.put("forward rate plot", PlotNodeGenerator.createPlotNode(fwd.getExpression(), -0.08, 0.1, 0.005));
			gate.put("reverse rate plot", PlotNodeGenerator.createPlotNode(rev.getExpression(), -0.08, 0.1, 0.005));
			
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


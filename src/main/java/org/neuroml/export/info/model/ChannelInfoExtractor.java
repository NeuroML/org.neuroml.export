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
import org.neuroml.model.util.NeuroMLException;

/**
 * @author borismarin
 *
 */
public class ChannelInfoExtractor {
	private InfoNode gates = new InfoNode();


	public ChannelInfoExtractor(IonChannel chan) throws NeuroMLException 
	{
		//TODO: use jlems to simulate channels and generate traces to plot
		//Sim simchan = Utils.convertNeuroMLToSim(chan);


		for (GateHHUndetermined g : chan.getGate()){

			HHRateProcessor rateinfo = new HHRateProcessor(g);	
			InfoNode gate = GenerateRatePlots(rateinfo);

			gate.put("instances", g.getInstances());
			gates.put("gate " + g.getId(), gate);
		}


		for (GateHHRates g : chan.getGateHHrates()){

			HHRateProcessor rateinfo = new HHRateProcessor(g);	
			InfoNode gateinfo = GenerateRatePlots(rateinfo);

			gateinfo.put("instances", g.getInstances());
			gates.put("gate " + g.getId(), gateinfo);

		}

		for(GateHHRatesInf g : chan.getGateHHratesInf()){
			InfoNode gateinfo = new InfoNode();

			gateinfo.put("instances", g.getInstances());
			gates.put("gate " + g.getId(), gateinfo);
		}

		for(GateHHRatesTau g : chan.getGateHHratesTau()){
			InfoNode gate = new InfoNode();

			gate.put("instances", g.getInstances());
			gates.put("gate " + g.getId(), gate);
		}

		for(GateHHTauInf g : chan.getGateHHtauInf()){
			InfoNode gate = new InfoNode();

			HHTauInfProcessor tii = new HHTauInfProcessor(g);	
			ChannelMLHHExpression inf = tii.getSteadyStateActivation();
			ChannelMLHHExpression tau = tii.getTimeCourse();

			gate.put("steady state activation", new ExpressionNode(inf.toString()));
			gate.put("time constant", new ExpressionNode(tau.toString()));
			gate.put("steady state activation plot", PlotNodeGenerator.createPlotNode(inf.getExpression(), -0.08, 0.1, 0.005));
			gate.put("time constant plot", PlotNodeGenerator.createPlotNode(tau.getExpression(), -0.08, 0.1, 0.005));

			gate.put("instances", g.getInstances());
			gates.put("gate " + g.getId(), gate);
		}
	}

	private InfoNode GenerateRatePlots(HHRateProcessor rateinfo) {

		InfoNode gate = new InfoNode();

		ChannelMLHHExpression fwd = rateinfo.getForwardRate();
		ChannelMLHHExpression rev = rateinfo.getReverseRate();

		gate.put("forward rate",  new ExpressionNode(fwd.toString()));
		gate.put("reverse rate",  new ExpressionNode(rev.toString()));
		gate.put("forward rate plot", PlotNodeGenerator.createPlotNode(fwd.getExpression(), -0.08, 0.1, 0.005));
		gate.put("reverse rate plot", PlotNodeGenerator.createPlotNode(rev.getExpression(), -0.08, 0.1, 0.005));

		return gate;


	}

	public InfoNode getGates() {
		return gates;
	}


}


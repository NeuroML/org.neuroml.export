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
public class ChannelInfoExtractor
{
	private InfoNode gates = new InfoNode();

	public ChannelInfoExtractor(IonChannel chan) throws NeuroMLException
	{
		// TODO: use jlems to simulate channels and generate traces to plot
		// Sim simchan = Utils.convertNeuroMLToSim(chan);

		for(GateHHUndetermined g : chan.getGate())
		{

			HHRateProcessor rateinfo = new HHRateProcessor(g);
			InfoNode gate = new InfoNode();

			gate.put("instances", g.getInstances());
			generateRatePlots(chan, gate, rateinfo);

			gates.put("gate " + g.getId(), gate);
		}

		for(GateHHRates g : chan.getGateHHrates())
		{

			HHRateProcessor rateinfo = new HHRateProcessor(g);

			InfoNode gate = new InfoNode();

			gate.put("instances", g.getInstances());
			generateRatePlots(chan, gate, rateinfo);

			gates.put("gate " + g.getId(), gate);

		}

		for(GateHHRatesInf g : chan.getGateHHratesInf())
		{
			InfoNode gateinfo = new InfoNode();

			gateinfo.put("instances", g.getInstances());
			gates.put("gate " + g.getId(), gateinfo);
		}

		for(GateHHRatesTau g : chan.getGateHHratesTau())
		{
			InfoNode gate = new InfoNode();

			gate.put("instances", g.getInstances());
			gates.put("gate " + g.getId(), gate);
		}

		for(GateHHTauInf g : chan.getGateHHtauInf())
		{
			InfoNode gate = new InfoNode();

			HHTauInfProcessor tii = new HHTauInfProcessor(g);
			ChannelMLHHExpression inf = tii.getSteadyState();
			ChannelMLHHExpression tau = tii.getTimeCourse();

			if(inf.toString().contains("ChannelMLGenericHHExpression"))
			{
				gate.put("steady state", new ExpressionNode(inf.toString()));
			}
			else
			{
				gate.put("steady state ", new ExpressionNode(inf.toString(), "Ion Channel " + chan.getId() + " - Steady State Activation - " + inf.getExpression().getId(), "V", "ms-1", -0.08, 0.1,
						0.005));
			}
			if(tau.toString().contains("ChannelMLGenericHHExpression"))
			{
				gate.put("time constant", new ExpressionNode(tau.toString()));
			}
			else
			{
				gate.put("time constant", new ExpressionNode(tau.toString(), "Ion Channel " + chan.getId() + " - Time Co - " + tau.getExpression().getId(), "V", "ms-1", -0.08, 0.1, 0.005));
			}
			gate.put("steady state plot", PlotNodeGenerator.createPlotNode(inf.getExpression(), -0.08, 0.1, 0.005, "V", "ms-1"));
			gate.put("time constant plot", PlotNodeGenerator.createPlotNode(tau.getExpression(), -0.08, 0.1, 0.005, "V", "ms-1"));

			gate.put("instances", g.getInstances());
			gates.put("gate " + g.getId(), gate);
		}
	}

	private void generateRatePlots(IonChannel chan, InfoNode gate, HHRateProcessor rateinfo)
	{

		ChannelMLHHExpression fwd = rateinfo.getForwardRate();
		ChannelMLHHExpression rev = rateinfo.getReverseRate();

		if(fwd.toString().contains("ChannelMLGenericHHExpression"))
		{
			gate.put("forward rate", new ExpressionNode(fwd.toString()));
		}
		else
		{
			gate.put("forward rate", new ExpressionNode(fwd.toString(), "Ion Channel " + chan.getId() + " - Forward Rate - " + fwd.getExpression().getId(), "V", "ms-1", -0.08, 0.1, 0.005));
		}
		if(rev.toString().contains("ChannelMLGenericHHExpression"))
		{
			gate.put("reverse rate", new ExpressionNode(rev.toString()));
		}
		else
		{
			gate.put("reverse rate", new ExpressionNode(rev.toString(), "Ion Channel " + chan.getId() + " - Reverse Rate - " + rev.getExpression().getId(), "V", "ms-1", -0.08, 0.1, 0.005));
		}
		gate.put("forward rate plot", PlotNodeGenerator.createPlotNode(fwd.getExpression(), -0.08, 0.1, 0.005, "V", "ms-1"));
		gate.put("reverse rate plot", PlotNodeGenerator.createPlotNode(rev.getExpression(), -0.08, 0.1, 0.005, "V", "ms-1"));

	}

	public InfoNode getGates()
	{
		return gates;
	}

}

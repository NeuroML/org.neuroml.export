/**
 * 
 */
package org.neuroml.export.info.model;

import java.util.ArrayList;
import java.util.List;

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
	

	/**
	 * @param expression
	 * @return
	 */
	public PlotNode createPlot(ChannelMLRateExpression<HHRate> expression)
	{
		PlotNode plot=new PlotNode("RatePlot","mV","ms-1");
		List<Double> x=new ArrayList<Double>();
		List<Double> y=new ArrayList<Double>();
		double dt=0.1f;
		for(int i=-80;i<100;i++)
		{
			x.add((double) i*dt);
			y.add(expression.eval(i*dt*0.001d));
		}
		Data d=new Data(x,y,expression.getId());
		plot.getData().add(d);
		return plot;
	}
	
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

			ChannelMLRateExpression<HHRate> fwd = new ChannelMLRateExpression<HHRate>(g.getForwardRate());
			ChannelMLRateExpression<HHRate> rev = new ChannelMLRateExpression<HHRate>(g.getReverseRate());

			gate.put("forward rate", fwd.toString());
			gate.put("reverse rate", rev.toString());
			gate.put("forward rate plot", createPlot(fwd));
			gate.put("reverse rate plot", createPlot(rev));
			
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


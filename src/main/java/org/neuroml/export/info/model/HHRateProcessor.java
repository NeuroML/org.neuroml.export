package org.neuroml.export.info.model;

import org.neuroml.model.GateHHRates;
import org.neuroml.model.HHRate;

public class HHRateProcessor {
	private ChannelMLHHExpression fwd;
	private ChannelMLHHExpression rev;

	public HHRateProcessor(GateHHRates g) {
		setForwardRate(rateToexpr(g.getForwardRate()));
		setReverseRate(rateToexpr(g.getReverseRate()));
	}

	private ChannelMLHHExpression rateToexpr(HHRate rate) {
		ChannelMLHHExpression expr = new ChannelMLHHExpression(new HHRateToIHHExpressionAdapter(rate));
		return expr; 
	}


	public ChannelMLHHExpression getSteadyStateActivation() {
		// TODO Auto-generated method stub
		return null;
	}

	public ChannelMLHHExpression getTimeConstant() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @return the fwd
	 */
	public ChannelMLHHExpression getForwardRate() {
		return fwd;
	}

	/**
	 * @param fwd the fwd to set
	 */
	public void setForwardRate(ChannelMLHHExpression fwd) {
		this.fwd = fwd;
	}

	/**
	 * @return the rev
	 */
	public ChannelMLHHExpression getReverseRate() {
		return rev;
	}

	/**
	 * @param rev the rev to set
	 */
	public void setReverseRate(ChannelMLHHExpression rev) {
		this.rev = rev;
	}


}

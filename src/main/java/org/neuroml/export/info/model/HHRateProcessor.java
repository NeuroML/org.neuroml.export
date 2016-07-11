package org.neuroml.export.info.model;

import org.neuroml.model.GateHHRates;
import org.neuroml.model.GateHHUndetermined;
import org.neuroml.model.HHRate;

public class HHRateProcessor
{
	private ChannelMLHHExpression _fwd;
	private ChannelMLHHExpression _rev;

	public HHRateProcessor(GateHHRates g)
	{
		setForwardRate(rateToexpr(g.getForwardRate()));
		setReverseRate(rateToexpr(g.getReverseRate()));
	}

	public HHRateProcessor(GateHHUndetermined g)
	{
		setForwardRate(rateToexpr(g.getForwardRate()));
		setReverseRate(rateToexpr(g.getReverseRate()));
	}

	private ChannelMLHHExpression rateToexpr(HHRate rate)
	{
		ChannelMLHHExpression expr = new ChannelMLHHExpression(new HHRateToIHHExpressionAdapter(rate));
		return expr;
	}

	/**
	 * @return the _fwd
	 */
	public ChannelMLHHExpression getForwardRate()
	{
		return _fwd;
	}

	/**
	 * @param fwd
	 *            the _fwd to set
	 */
	public void setForwardRate(ChannelMLHHExpression fwd)
	{
		_fwd = fwd;
	}

	/**
	 * @return the _rev
	 */
	public ChannelMLHHExpression getReverseRate()
	{
		return _rev;
	}

	/**
	 * @param rev
	 *            the _rev to set
	 */
	public void setReverseRate(ChannelMLHHExpression rev)
	{
		_rev = rev;
	}

}

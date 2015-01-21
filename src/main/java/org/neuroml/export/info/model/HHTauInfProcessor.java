package org.neuroml.export.info.model;

import org.neuroml.model.GateHHTauInf;
import org.neuroml.model.HHTime;
import org.neuroml.model.HHVariable;

public class HHTauInfProcessor {

	private ChannelMLHHExpression _tau;
	private ChannelMLHHExpression _inf;


	public HHTauInfProcessor(GateHHTauInf g) {
		setSteadyState(hhVarToExpr(g.getSteadyState()));
		setTimeCourse(hhTimeToExpr(g.getTimeCourse()));
	}


	private ChannelMLHHExpression hhVarToExpr(HHVariable var) {
		ChannelMLHHExpression expr = new ChannelMLHHExpression(new HHVariableToIHHExpressionAdapter(var));
		return expr; 
	}

	private ChannelMLHHExpression hhTimeToExpr(HHTime var) {
		ChannelMLHHExpression expr = new ChannelMLHHExpression(new HHTimeToIHHExpressionAdapter(var));
		return expr; 
	}



	/**
	 * @return the _tau
	 */
	public ChannelMLHHExpression getTimeCourse() {
		return _tau;
	}


	/**
	 * @param tau the _tau to set
	 */
	public void setTimeCourse(ChannelMLHHExpression tau) {
		_tau = tau;
	}


	/**
	 * @return the _inf
	 */
	public ChannelMLHHExpression getSteadyState() {
		return _inf;
	}

	/**
	 * @param inf the _inf to set
	 */
	private void setSteadyState(ChannelMLHHExpression inf) {
		_inf = inf;

	}


}


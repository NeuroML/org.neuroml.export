package org.neuroml.export.info.model;

import java.util.ArrayList;
import java.util.List;

public class PlotNodeGenerator {

	/**
	 * @param expression
	 * @return
	 */
	public static PlotNode createPlotNode(NeuroMLExpression expression, Double x0, Double x1, Double dx) {
		PlotNode plot = new PlotNode(expression.getId(), "V", "ms-1");
		plot.getData().add(createDiscretizedData(expression, x0, x1, dx));
		return plot;
	}

	public static PlotNode createPlotNode(String id, List<Double> X, List<Double> Y) {
		PlotNode plot = new PlotNode(id, "V", "ms-1");
		plot.getData().add(new Data(X, Y, id));
		return plot;
	}

	private static Data createDiscretizedData(NeuroMLExpression expression, Double x0, Double x1, Double dx) {
		List<Double> X = new ArrayList<Double>();
		List<Double> Y = new ArrayList<Double>();


		for (Double x = x0; x < x1; x += dx) {
			X.add(x);
			Y.add(expression.eval(x));
		}
		Data d = new Data(X, Y, expression.getId());
		return d;
	}
}

package org.neuroml.export.utils;

public enum Formats
{
	LEMS("LEMS","xml"),
	C("c","c"),
	DLEMS("DLems", "json"),
	MATLAB("Matlab", "m"),
	NEUROML2("NeuroML2", "nml"),
	SBML("SBML", "sbml"),
	CELLML("CellML", "CELLML"),
	SEDML("SED-ML", "sedml"),
	BRIAN("Brian", "py"),
	MODELICA("Modelica", "Modelica"),
	DN_SIM("DNSim", "DNSim"),
	GEPPETTO("Geppetto", "xml"),
	GRAPH_VIZ("GraphViz", "gv"),
	NEST("NEST", "NEST"),
	NEURON("NEURON", "nrn"),
	NEURON_A("NEURONa", "NEURONa"),
	PYNN("PyNN", "PyNN"),
	SVG("SVG", "svg"),
	NINEML("NineML", "9ml"),
	SPINEML("SpineML", "spineml"),
	INFORMATION("Summary", ""),
	XPP("Xpp", "ode"),
	PNG("PNG", "png");
	
	private final String label;
	private final String extension;

	private Formats(String label, String extension)
	{
		this.label = label;
		this.extension = extension;
	}
	
	public String getExtension()
	{
        return extension;
    }

	public String getLabel()
	{
		return label;
	}
	
	

}

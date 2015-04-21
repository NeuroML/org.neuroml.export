package org.neuroml.export.utils;

public enum Format
{
	LEMS("LEMS","xml"),
	C("c","c"),
	DLEMS("DLems", "json"),
	MATLAB("Matlab", "m"),
	NEUROML2("NeuroML2", "nml"),
	SBML("SBML", "sbml"),
	CELLML("CellML", "cellml"),
	SEDML("SED-ML", "sedml"),
	BRIAN("Brian", "py"),
	MODELICA("Modelica", "mos"),
	DN_SIM("DNSim", "m"),
	GEPPETTO("Geppetto", "xml"),
	GRAPH_VIZ("GraphViz", "gv"),
	NEST("NEST", "py"),
	NEURON("NEURON", "nrn"),
	NEURON_A("NEURONa", "NEURONa"),
	PYNN("PyNN", "py"),
	SVG("SVG", "svg"),
	NINEML("NineML", "9ml"),
	SPINEML("SpineML", "spineml"),
	INFORMATION("Summary", ""),
	VHDL("VHDL", "vhdl"),
	XINEML("Xineml", ""),
	XPP("Xpp", "ode"),
	PNG("PNG", "png");
	
	private final String label;
	private final String extension;

	private Format(String label, String extension)
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

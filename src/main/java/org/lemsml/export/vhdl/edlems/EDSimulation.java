package org.lemsml.export.vhdl.edlems;

import java.util.ArrayList;

public class EDSimulation {
	public String dt;
	public String simlength;
	public String steps;
	public int synapseCount;
	public ArrayList<EDDisplay> displays;
	public ArrayList<EDComponent> neuronComponents;
	public ArrayList<EDComponent> neuronInstances;
	public ArrayList<EDEventConnection> eventConnections;
}

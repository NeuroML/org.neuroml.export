package org.lemsml.export.vhdl.edlems;

import java.util.ArrayList;

public class EDOnEntry {
	public String name;

	public ArrayList<EDStateAssignment> stateAssignment;
	public ArrayList<EDEventOut> events;
	public ArrayList<EDTransition> transitions;
}

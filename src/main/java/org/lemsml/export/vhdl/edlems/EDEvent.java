package org.lemsml.export.vhdl.edlems;

import java.util.ArrayList;

public class EDEvent {
	public String name;

	public ArrayList<EDStateAssignment> stateAssignments;
	public ArrayList<EDEventOut> events;
	public ArrayList<EDTransition> transitions;
}

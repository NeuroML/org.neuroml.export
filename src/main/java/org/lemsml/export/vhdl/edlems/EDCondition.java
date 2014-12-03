package org.lemsml.export.vhdl.edlems;

import java.util.ArrayList;

public class EDCondition {
	public String name;
	public String condition;
	public String sensitivityList;
 
	public ArrayList<EDStateAssignment> stateAssignment;
	public ArrayList<EDEventOut> events;
	public ArrayList<EDTransition> transitions;
}

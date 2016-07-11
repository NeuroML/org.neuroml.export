package org.lemsml.export.vhdl.edlems;

import java.util.ArrayList;

public class EDConditionalDerivedVariable extends EDSignal {

	public String name;
	public String type;
	public String exposure;
	public ArrayList<EDCase> cases;
	public String sensitivityList;
	public boolean ExposureIsUsed = false;
	public boolean IsUsedForOtherDerivedVariables = false;
	
}

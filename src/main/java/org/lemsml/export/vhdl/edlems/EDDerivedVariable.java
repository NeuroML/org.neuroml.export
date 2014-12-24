package org.lemsml.export.vhdl.edlems;

import java.util.ArrayList;
import java.util.List;

public class EDDerivedVariable extends EDSignalComplex {

	public String name;
	public String type;
	public String value;
	public String exposure;
	public String sensitivityList;
	public boolean ExposureIsUsed = false;
	public boolean IsUsedForOtherDerivedVariables = false;
}

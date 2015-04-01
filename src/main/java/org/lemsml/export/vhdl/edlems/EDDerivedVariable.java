package org.lemsml.export.vhdl.edlems;

import java.util.ArrayList;
import java.util.List;

public class EDDerivedVariable extends EDSignalComplex {

	public String name;
	public String type;
	public String value;
	public String exposure;
	public String sensitivityList;
	public String dimension;
	public boolean ExposureIsUsed = false;
	public boolean IsUsedForOtherDerivedVariables = false;
	public boolean isEmpty = false;
	public boolean isSynapseSelect = false;
	public List<String> items;
	public List<String> itemsParents;
}

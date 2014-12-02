package org.lemsml.export.vhdl.edlems;

import java.util.ArrayList;
import java.util.List;

public class EDRegime {
	public String name;
	public String isDefault;
	public List<EDDynamic> dynamics = new ArrayList<EDDynamic>();
	public List<EDDerivedParameter> derivedparameters = new ArrayList<EDDerivedParameter>();
	public List<EDDerivedVariable> derivedvariables = new ArrayList<EDDerivedVariable>();
	public List<EDConditionalDerivedVariable> conditionalderivedvariables = new ArrayList<EDConditionalDerivedVariable>();
	public List<EDCondition> conditions = new ArrayList<EDCondition>();
	public List<EDEvent> events = new ArrayList<EDEvent>();
	public List<EDOnEntry> onEntrys = new ArrayList<EDOnEntry>();
	
}

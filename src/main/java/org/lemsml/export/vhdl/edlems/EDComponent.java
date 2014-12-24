package org.lemsml.export.vhdl.edlems;

import java.util.ArrayList;
import java.util.List;

public class EDComponent {

	public float t_end;
	public float t_start;
	public String name;
	public String comment;
	public List<EDDynamic> dynamics = new ArrayList<EDDynamic>();
	public List<EDDerivedParameter> derivedparameters = new ArrayList<EDDerivedParameter>();
	public List<EDDerivedVariable> derivedvariables = new ArrayList<EDDerivedVariable>();
	public List<EDConditionalDerivedVariable> conditionalderivedvariables = new ArrayList<EDConditionalDerivedVariable>();
	public List<EDCondition> conditions = new ArrayList<EDCondition>();
	public List<EDEvent> events = new ArrayList<EDEvent>();
	public List<EDRequirement> requirements = new ArrayList<EDRequirement>();
	//public List<EDExposure> exposures = new ArrayList<EDExposure>();
	public List<EDEventPort> eventports = new ArrayList<EDEventPort>();
	public List<EDState> state = new ArrayList<EDState>();
	public List<EDStateFunction> state_functions = new ArrayList<EDStateFunction>();
	public List<EDComponent> Children = new ArrayList<EDComponent>();
	public List<EDLink> links = new ArrayList<EDLink>();
	public List<EDRegime> regimes = new ArrayList<EDRegime>();
	public List<EDParameter> parameters = new ArrayList<EDParameter>();
	
	
}

/**
 *
 */
package org.neuroml.export.cellml;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.neuroml.export.base.ANeuroMLXMLWriter;
import org.neuroml.export.exception.GenerationException;
import org.neuroml.export.exception.ModelFeatureSupportException;
import org.neuroml.export.utils.Utils;
import org.neuroml.export.utils.support.ModelFeature;
import org.neuroml.export.utils.support.SupportLevelInfo;

import org.lemsml.jlems.core.type.dynamics.OnCondition;
import org.lemsml.jlems.core.type.dynamics.OnStart;
import org.lemsml.jlems.core.type.dynamics.StateAssignment;
import org.lemsml.jlems.core.type.dynamics.StateVariable;
import org.lemsml.jlems.core.type.dynamics.TimeDerivative;
import org.lemsml.jlems.core.expression.Parser;
import org.lemsml.jlems.core.logging.E;
import org.lemsml.jlems.core.type.Component;
import org.lemsml.jlems.core.type.Target;
import org.lemsml.jlems.core.type.FinalParam;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.sim.LEMSException;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.core.type.dynamics.DerivedVariable;
import org.lemsml.jlems.core.type.dynamics.Dynamics;
import org.lemsml.jlems.io.util.FileUtil;
import org.neuroml.model.util.NeuroMLException;

@SuppressWarnings("StringConcatenationInsideStringBufferAppend")
public class CellMLWriter extends ANeuroMLXMLWriter
{

	public static final String PREF_CELLML_SCHEMA = "http://www.cellml.org/cellml/cellml_1_1.xsd";
	public static final String PREF_CELLML_VERSION = "http://www.cellml.org/cellml/1.1#";
	public static final String LOCAL_CELLML_SCHEMA = "???.xsd";

	public static final String GLOBAL_TIME_CELLML = "time";

	// private final String sbmlTemplateFile = "sbml/template.sbml";
	public CellMLWriter(Lems l) throws ModelFeatureSupportException, LEMSException, NeuroMLException
	{
		super(l, "CellML");
		sli.checkConversionSupported(format, lems);
	}

	@Override
	protected void setSupportedFeatures()
	{
		sli.addSupportInfo(format, ModelFeature.ABSTRACT_CELL_MODEL, SupportLevelInfo.Level.MEDIUM);
		sli.addSupportInfo(format, ModelFeature.COND_BASED_CELL_MODEL, SupportLevelInfo.Level.LOW);
		sli.addSupportInfo(format, ModelFeature.SINGLE_COMP_MODEL, SupportLevelInfo.Level.MEDIUM);
		sli.addSupportInfo(format, ModelFeature.NETWORK_MODEL, SupportLevelInfo.Level.LOW);
		sli.addSupportInfo(format, ModelFeature.MULTI_CELL_MODEL, SupportLevelInfo.Level.NONE);
		sli.addSupportInfo(format, ModelFeature.MULTI_POPULATION_MODEL, SupportLevelInfo.Level.NONE);
		sli.addSupportInfo(format, ModelFeature.NETWORK_WITH_INPUTS_MODEL, SupportLevelInfo.Level.NONE);
		sli.addSupportInfo(format, ModelFeature.NETWORK_WITH_PROJECTIONS_MODEL, SupportLevelInfo.Level.NONE);
		sli.addSupportInfo(format, ModelFeature.MULTICOMPARTMENTAL_CELL_MODEL, SupportLevelInfo.Level.NONE);
		sli.addSupportInfo(format, ModelFeature.HH_CHANNEL_MODEL, SupportLevelInfo.Level.LOW);
		sli.addSupportInfo(format, ModelFeature.KS_CHANNEL_MODEL, SupportLevelInfo.Level.NONE);
	}

	public String getMainScript() throws GenerationException
	{

		Parser p = new Parser();

		StringBuilder main = new StringBuilder();
		StringBuilder connections = new StringBuilder();

		try
		{
			Target target = lems.getTarget();

			Component simCpt = target.getComponent();

			String targetId = simCpt.getStringValue("target");

			Component tgtNet = lems.getComponent(targetId);

			String netId = tgtNet.getID();

			ArrayList<Component> pops = tgtNet.getChildrenAL("populations");

			main.append("<?xml version='1.0' encoding='UTF-8'?>\n");

			String[] attrs = new String[] { "name=" + netId,
			/* "cmeta:id=" + netId, */
			"xmlns=" + PREF_CELLML_VERSION,
			/*
			 * "xmlns:cellml="+PREF_CELLML_VERSION, "xmlns:cmeta="+PREF_CELLML_VERSION,
			 */
			"xmlns:xsi=http://www.w3.org/2001/XMLSchema-instance", "xsi:schemaLocation=" + PREF_CELLML_VERSION + " " + PREF_CELLML_SCHEMA };

			startElement(main, "model", attrs);

			main.append("<!--\n");
			startElement(main, "documentation");
			startElement(main, "p", "xmlns=http://www.w3.org/1999/xhtml");
			main.append("\n" + Utils.getHeaderComment(format) + "\n");
			main.append("\nExport of model:\n" + lems.textSummary(false, false) + "\n");
			endElement(main, "p");
			endElement(main, "documentation");
			main.append("-->\n");
			main.append("\n");

			startElement(main, "units", "name=per_second");
			startEndElement(main, "unit", "units=second", "exponent=-1");
			endElement(main, "units");
			main.append("\n");

			startElement(main, "component", "name=environment");
			startEndElement(main, "variable", "name=time", "units=second", "public_interface=out");
			endElement(main, "component");
			main.append("\n");

			addComment(main, "Adding simulation " + simCpt + " of network: " + tgtNet.summary() + "", true);

			int initAssNum = 0;
			int onCondNum = 0;

			for(Component pop : pops)
			{
				handleComponent(main, connections, pop);
				String compRef = pop.getStringValue("component");
				Component popComp = lems.getComponent(compRef);
				handleComponent(main, connections, popComp);

				/*
				 * ComponentType type = popComp.getComponentType(); initAssNum = initAssNum + type.getDynamics().getOnStarts().size(); onCondNum = onCondNum +
				 * type.getDynamics().getOnConditions().size();
				 * 
				 * int num = Integer.parseInt(pop.getStringValue("size")); addComment(main, "Population " + pop.getID() + " contains " + num + " instances of components of: " + popComp, true);
				 * 
				 * for (int i = 0; i < num; i++) { startEndElement(main, "compartment", "id=" + pop.getID() + "_" + i, "size=1"); }
				 */
			}

			main.append("\n");

			main.append(connections.toString());

			endElement(main, "model");

		}
		catch(ContentError e)
		{
			throw new GenerationException("Error with LEMS content", e);
		}
		return main.toString();
	}

	private void handleComponent(StringBuilder main, StringBuilder connections, Component comp) throws ContentError
	{

		startElement(main, "component", "name=" + comp.id);

		// <variable name="time" units="millisecond" public_interface="in"/>
		startEndElement(main, "variable", "name=time", "units=second", "public_interface=in");

		for(FinalParam param : comp.getComponentType().getFinalParams())
		{
			startEndElement(main, "variable", "name=" + param.getName(), "initial_value=" + (float) comp.getParamValue(param.getName()).getDoubleValue(), "units=dimensionless");
		}

		Dynamics dyn = comp.getComponentType().getDynamics();
		if(dyn != null)
		{
			for(StateVariable sv : dyn.getStateVariables())
			{
				startEndElement(main, "variable", "name=" + sv.getName(), "initial_value=0", "units=dimensionless");

			}

			for(DerivedVariable dv : dyn.getDerivedVariables())
			{
				startEndElement(main, "variable", "name=" + dv.getName(), "initial_value=0", "units=dimensionless");

			}

			startElement(main, "math", "xmlns=http://www.w3.org/1998/Math/MathML");

			for(OnStart os : dyn.getOnStarts())
			{
				for(StateAssignment sa : os.getStateAssignments())
				{
					startElement(main, "apply");
					startEndElement(main, "eq");
					startEndTextElement(main, "ci", sa.getStateVariable().getName());
					processMathML(main, sa.getParseTree(), false);
					endElement(main, "apply");

				}
			}

			for(DerivedVariable dv : dyn.getDerivedVariables())
			{
				startElement(main, "apply");
				startEndElement(main, "eq");
				startEndTextElement(main, "ci", dv.getName());
				processMathML(main, dv.getParseTree(), false);
				endElement(main, "apply");

			}

			for(TimeDerivative td : dyn.getTimeDerivatives())
			{

				startElement(main, "apply");
				startEndElement(main, "eq");
				startElement(main, "apply");
				startEndElement(main, "diff");
				startElement(main, "bvar");
				startEndTextElement(main, "ci", GLOBAL_TIME_CELLML);
				endElement(main, "bvar");
				startEndTextElement(main, "ci", td.getVariable());
				endElement(main, "apply");
				processMathML(main, td.getParseTree(), false);
				endElement(main, "apply");

			}

			endElement(main, "math");
		}

		endElement(main, "component");
		main.append("\n\n");

		startElement(connections, "connection");
		startEndElement(connections, "map_components", "component_1=" + comp.id, "component_2=environment");
		startEndElement(connections, "map_variables", "variable_1=time", "variable_2=time");
		endElement(connections, "connection");
		connections.append("\n");

	}

	private String getSuitableId(String str)
	{
		return str.replace(" ", "_").replace(".", "").replace("(", "_").replace(")", "_").replace("*", "mult").replace("+", "plus").replace("/", "div");
	}

	public static void main(String[] args) throws Exception
	{

		E.setDebug(false);

		ArrayList<File> lemsFiles = new ArrayList<File>();
		// lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex0_IaF.xml"));
		// lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex5_DetCell.xml"));
		lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex9_FN.xml"));
		lemsFiles.add(new File("../git/HindmarshRose1984/NeuroML2/Run_Regular_HindmarshRose.xml"));
		// lemsFiles.add(new File("../neuroConstruct/osb/cerebellum/cerebellar_granule_cell/GranuleCell/neuroConstruct/generatedNeuroML2/LEMS_GranuleCell.xml"));
		// lemsFiles.add(new File("../neuroConstruct/osb/invertebrate/lobster/PyloricNetwork/neuroConstruct/generatedNeuroML2/LEMS_PyloricPacemakerNetwork.xml"));
		// lemsFiles.add(new File("../neuroConstruct/osb/invertebrate/celegans/CElegansNeuroML/CElegans/pythonScripts/c302/LEMS_c302_A.xml"));
		// lemsFiles.add(new File("../git/GPUShowcase/NeuroML2/LEMS_simplenet.xml"));

		for(File lemsFile : lemsFiles)
		{
			Lems lems = Utils.readLemsNeuroMLFile(lemsFile).getLems();
			File mainFile = new File(lemsFile.getParentFile(), lemsFile.getName().replaceAll(".xml", ".cellml"));

			CellMLWriter cw = new CellMLWriter(lems);
			String ff = cw.getMainScript();
			FileUtil.writeStringToFile(ff, mainFile);
			System.out.println("Generated: " + mainFile.getAbsolutePath());
		}

	}

	@Override
	public List<File> convert(Lems lems)
	{
		// TODO Auto-generated method stub
		return null;
	}

}

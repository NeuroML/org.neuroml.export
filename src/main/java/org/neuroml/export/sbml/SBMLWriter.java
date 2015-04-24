/**
 *
 */
package org.neuroml.export.sbml;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.neuroml.export.base.ANeuroMLXMLWriter;
import org.neuroml.export.exceptions.GenerationException;
import org.neuroml.export.exceptions.ModelFeatureSupportException;
import org.neuroml.export.utils.Format;
import org.neuroml.export.utils.Utils;
import org.neuroml.export.utils.support.ModelFeature;
import org.neuroml.export.utils.support.SupportLevelInfo;

import org.lemsml.jlems.core.type.dynamics.OnCondition;
import org.lemsml.jlems.core.type.dynamics.OnStart;
import org.lemsml.jlems.core.type.dynamics.StateAssignment;
import org.lemsml.jlems.core.type.dynamics.StateVariable;
import org.lemsml.jlems.core.type.dynamics.TimeDerivative;
import org.lemsml.jlems.core.expression.ParseError;
import org.lemsml.jlems.core.expression.ParseTree;
import org.lemsml.jlems.core.expression.Parser;
import org.lemsml.jlems.core.type.Component;
import org.lemsml.jlems.core.type.ComponentType;
import org.lemsml.jlems.core.type.Target;
import org.lemsml.jlems.core.type.FinalParam;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.logging.E;
import org.lemsml.jlems.core.sim.LEMSException;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.core.type.dynamics.DerivedVariable;
import org.lemsml.jlems.io.util.FileUtil;
import org.neuroml.model.util.NeuroMLException;

@SuppressWarnings("StringConcatenationInsideStringBufferAppend")
public class SBMLWriter extends ANeuroMLXMLWriter
{

	public static final String PREF_SBML_SCHEMA = "http://sbml.org/Special/xml-schemas/sbml-l2v2-schema/sbml.xsd";

	public static final String GLOBAL_TIME_SBML = "t";
	public static final String GLOBAL_TIME_SBML_MATHML = "<csymbol encoding=\"text\" definitionURL=\"http://www.sbml.org/sbml/symbols/time\"> time </csymbol>";

	public SBMLWriter(Lems lems) throws ModelFeatureSupportException, LEMSException, NeuroMLException
	{
		super(lems, Format.SBML);
	}

	public SBMLWriter(Lems lems, File outputFolder, String outputFileName) throws ModelFeatureSupportException, NeuroMLException, LEMSException
	{
		super(lems, Format.SBML, outputFolder, outputFileName);
	}

	@Override
	public void setSupportedFeatures()
	{

		sli.addSupportInfo(format, ModelFeature.ABSTRACT_CELL_MODEL, SupportLevelInfo.Level.MEDIUM);
		sli.addSupportInfo(format, ModelFeature.COND_BASED_CELL_MODEL, SupportLevelInfo.Level.MEDIUM);
		sli.addSupportInfo(format, ModelFeature.SINGLE_COMP_MODEL, SupportLevelInfo.Level.MEDIUM);
		sli.addSupportInfo(format, ModelFeature.HH_CHANNEL_MODEL, SupportLevelInfo.Level.NONE);
		sli.addSupportInfo(format, ModelFeature.KS_CHANNEL_MODEL, SupportLevelInfo.Level.OUTSIDE_CURRENT_SCOPE);
		sli.addSupportInfo(format, ModelFeature.NETWORK_MODEL, SupportLevelInfo.Level.LOW);
		sli.addSupportInfo(format, ModelFeature.MULTI_CELL_MODEL, SupportLevelInfo.Level.OUTSIDE_CURRENT_SCOPE);
		sli.addSupportInfo(format, ModelFeature.MULTI_POPULATION_MODEL, SupportLevelInfo.Level.OUTSIDE_CURRENT_SCOPE);
		sli.addSupportInfo(format, ModelFeature.NETWORK_WITH_INPUTS_MODEL, SupportLevelInfo.Level.OUTSIDE_CURRENT_SCOPE);
		sli.addSupportInfo(format, ModelFeature.NETWORK_WITH_PROJECTIONS_MODEL, SupportLevelInfo.Level.OUTSIDE_CURRENT_SCOPE);
		sli.addSupportInfo(format, ModelFeature.MULTICOMPARTMENTAL_CELL_MODEL, SupportLevelInfo.Level.OUTSIDE_CURRENT_SCOPE);

	}

	public String getMainScript() throws GenerationException
	{

		Parser p = new Parser();

		StringBuilder main = new StringBuilder();

		try
		{
			Target target = lems.getTarget();

			Component simCpt = target.getComponent();

			String targetId = simCpt.getStringValue("target");

			Component tgtNet = lems.getComponent(targetId);

			String netId = tgtNet.getID();

			ArrayList<Component> pops = tgtNet.getChildrenAL("populations");

			if(false /**
			 * **********************************************************pops.isEmpty() || (pops.size()==1 && pops.get(0).getStringValue("size").equals("1"))
			 */
			)
			{

				/// Nothing...

			}
			else
			{

				main.append("<?xml version='1.0' encoding='UTF-8'?>\n");

				String[] attrs = new String[] { "xmlns=http://www.sbml.org/sbml/level2/version2", "metaid=metaid_0000001", "level=2", "version=2",
						"xmlns:xsi=http://www.w3.org/2001/XMLSchema-instance", "xsi:schemaLocation=http://www.sbml.org/sbml/level2/version2 " + PREF_SBML_SCHEMA };
				startElement(main, "sbml", attrs);
				startElement(main, "notes");
				startElement(main, "p", "xmlns=http://www.w3.org/1999/xhtml");
				main.append("\n" + Utils.getHeaderComment(format) + "\n");
				main.append("\nExport of model:\n" + lems.textSummary(false, false) + "\n");
				endElement(main, "p");
				endElement(main, "notes");

				startElement(main, "model", "id=" + netId, "name=" + netId);
				main.append("\n");

				addComment(main, "Adding simulation " + simCpt + " of network: " + tgtNet.summary() + "", true);

				int initAssNum = 0;
				int onCondNum = 0;

				startElement(main, "listOfCompartments");

				for(Component pop : pops)
				{
					String compRef = pop.getStringValue("component");
					Component popComp = lems.getComponent(compRef);
					ComponentType type = popComp.getComponentType();
					initAssNum = initAssNum + type.getDynamics().getOnStarts().size();
					onCondNum = onCondNum + type.getDynamics().getOnConditions().size();

					int num = Integer.parseInt(pop.getStringValue("size"));
					addComment(main, "Population " + pop.getID() + " contains " + num + " instances of components of: " + popComp, true);

					for(int i = 0; i < num; i++)
					{
						startEndElement(main, "compartment", "id=" + pop.getID() + "_" + i, "size=1");
					}

				}

				endElement(main, "listOfCompartments");

				main.append("\n");

				startElement(main, "listOfParameters");

				for(Component pop : pops)
				{
					String compRef = pop.getStringValue("component");
					Component popComp = lems.getComponent(compRef);

					for(FinalParam param : popComp.getComponentType().getFinalParams())
					{
						startEndElement(main, "parameter", "id=" + param.getName(), "value=" + (float) popComp.getParamValue(param.getName()).getDoubleValue(), "constant=true");
					}

					for(StateVariable sv : popComp.getComponentType().getDynamics().getStateVariables())
					{
						startEndElement(main, "parameter", "id=" + sv.getName(), "value=0", "constant=false");

					}
					for(DerivedVariable dv : popComp.getComponentType().getDynamics().getDerivedVariables())
					{
						startEndElement(main, "parameter", "id=" + dv.getName(), "value=0", "constant=false");

					}
					/*
					 * for(Constant c: popComp.getComponentClass().getConstants()) { startEndElement(main, "parameter", "id="+c.getName(),
					 * "value="+(float)popComp.getParamValue(c.getName()).getDoubleValue(), "constant=false");
					 * 
					 * }
					 */
				}

				endElement(main, "listOfParameters");
				main.append("\n");

				if(initAssNum > 0)
				{
					startElement(main, "listOfInitialAssignments");

					for(Component pop : pops)
					{
						String compRef = pop.getStringValue("component");
						Component popComp = lems.getComponent(compRef);
						ComponentType type = popComp.getComponentType();

						for(OnStart os : type.getDynamics().getOnStarts())
						{
							for(StateAssignment sa : os.getStateAssignments())
							{

								startElement(main, "initialAssignment", "symbol=" + sa.getStateVariable().getName());
								processMathML(main, sa.getParseTree());
								endElement(main, "initialAssignment");

							}
						}
					}

					endElement(main, "listOfInitialAssignments");
				}

				main.append("\n");

				startElement(main, "listOfRules");

				for(Component pop : pops)
				{
					String compRef = pop.getStringValue("component");
					Component popComp = lems.getComponent(compRef);
					ComponentType type = popComp.getComponentType();

					for(TimeDerivative td : type.getDynamics().getTimeDerivatives())
					{
						startElement(main, "rateRule", "variable=" + td.getStateVariable().getName());
						processMathML(main, td.getParseTree());
						endElement(main, "rateRule");
					}
					for(DerivedVariable dv : type.getDynamics().getDerivedVariables())
					{
						startElement(main, "assignmentRule", "variable=" + dv.getName());
                        System.out.println(dv.getName()+" = "+dv.getFunc());
						processMathML(main, dv.getParseTree());
						endElement(main, "assignmentRule");
					}

				}

				endElement(main, "listOfRules");
				main.append("\n");

				if(onCondNum > 0)
				{
					startElement(main, "listOfEvents");

					for(Component pop : pops)
					{
						String compRef = pop.getStringValue("component");
						Component popComp = lems.getComponent(compRef);
						ComponentType type = popComp.getComponentType();

						for(OnCondition oc : type.getDynamics().getOnConditions())
						{

							String id = "check__" + getSuitableId(oc.test);
							startElement(main, "event", "id=" + id);
							startElement(main, "trigger");
							// String tempTestString = oc.test.replace(".gt.", ">").replace(".lt.", "<");

							try
							{
								ParseTree testEval = p.parseCondition(oc.test);
								processMathML(main, testEval);
							}
							catch(ParseError ex)
							{
								throw new ContentError("Problem parsing string for triggering event: " + oc.test, ex);
							}

							endElement(main, "trigger");
							startElement(main, "listOfEventAssignments");

							for(StateAssignment sa : oc.getStateAssignments())
							{

								startElement(main, "eventAssignment", "variable=" + sa.getStateVariable().getName());
								processMathML(main, sa.getParseTree());
								endElement(main, "eventAssignment");

							}
							endElement(main, "listOfEventAssignments");
							endElement(main, "event");

						}
					}
					endElement(main, "listOfEvents");
				}

				main.append("\n");

				endElement(main, "model");
				endElement(main, "sbml");
			}
		}
		catch(ContentError e)
		{
			throw new GenerationException("Error with LEMS content", e);
		}
		return main.toString();
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
		lemsFiles.add(new File("../NeuroML2/LEMSexamples/NoInp0.xml"));
        
		lemsFiles.add(new File("../git/HindmarshRose1984/NeuroML2/Run_Regular_HindmarshRose.xml"));
		// lemsFiles.add(new File("../neuroConstruct/osb/cerebellum/cerebellar_granule_cell/GranuleCell/neuroConstruct/generatedNeuroML2/LEMS_GranuleCell.xml"));
		//lemsFiles.add(new File("../neuroConstruct/osb/invertebrate/lobster/PyloricNetwork/neuroConstruct/generatedNeuroML2/LEMS_PyloricPacemakerNetwork.xml"));
		// lemsFiles.add(new File("../neuroConstruct/osb/invertebrate/celegans/CElegansNeuroML/CElegans/pythonScripts/c302/LEMS_c302_A.xml"));
		// lemsFiles.add(new File("../git/GPUShowcase/NeuroML2/LEMS_simplenet.xml"));

		for(File lemsFile : lemsFiles)
		{
			Lems lems = Utils.readLemsNeuroMLFile(lemsFile).getLems();
			File mainFile = new File(lemsFile.getParentFile(), lemsFile.getName().replaceAll(".xml", ".sbml"));

			SBMLWriter cw = new SBMLWriter(lems);
			String ff = cw.getMainScript();
			FileUtil.writeStringToFile(ff, mainFile);
			System.out.println("Generated: " + mainFile.getAbsolutePath());
		}

	}

	@Override
	public List<File> convert() throws GenerationException, IOException
	{
		List<File> outputFiles = new ArrayList<File>();

        String code = this.getMainScript();

        File outputFile = new File(this.getOutputFolder(), this.getOutputFileName());
        FileUtil.writeStringToFile(code, outputFile);
        outputFiles.add(outputFile);


		return outputFiles;
	}

}

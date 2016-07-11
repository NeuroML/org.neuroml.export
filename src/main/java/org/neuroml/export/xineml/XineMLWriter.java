/**
 * A writer for the various variants of NineML/SpineML
 */
package org.neuroml.export.xineml;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.lemsml.jlems.core.expression.ParseError;
import org.lemsml.jlems.core.flatten.ComponentFlattener;
import org.lemsml.jlems.core.logging.E;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.sim.LEMSException;
import org.lemsml.jlems.core.type.Component;
import org.lemsml.jlems.core.type.ComponentType;
import org.lemsml.jlems.core.type.Constant;
import org.lemsml.jlems.core.type.DimensionalQuantity;
import org.lemsml.jlems.core.type.EventPort;
import org.lemsml.jlems.core.type.Exposure;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.core.type.ParamValue;
import org.lemsml.jlems.core.type.Parameter;
import org.lemsml.jlems.core.type.QuantityReader;
import org.lemsml.jlems.core.type.Target;
import org.lemsml.jlems.core.type.dynamics.DerivedVariable;
import org.lemsml.jlems.core.type.dynamics.Dynamics;
import org.lemsml.jlems.core.type.dynamics.EventOut;
import org.lemsml.jlems.core.type.dynamics.OnCondition;
import org.lemsml.jlems.core.type.dynamics.StateAssignment;
import org.lemsml.jlems.core.type.dynamics.StateVariable;
import org.lemsml.jlems.core.type.dynamics.TimeDerivative;
import org.lemsml.jlems.io.util.FileUtil;
import org.neuroml.export.base.ANeuroMLXMLWriter;
import org.neuroml.export.exceptions.GenerationException;
import org.neuroml.export.exceptions.ModelFeatureSupportException;
import org.neuroml.export.utils.Format;
import org.neuroml.export.utils.Utils;
import org.neuroml.export.utils.support.ModelFeature;
import org.neuroml.export.utils.support.SupportLevelInfo;
import org.neuroml.model.util.NeuroMLException;

public class XineMLWriter extends ANeuroMLXMLWriter
{
	public static final String SCHEMA_9ML = "https://raw.github.com/OpenSourceBrain/NineMLShowcase/master/Schemas/NineML/NineML_v0.3.xsd";
	public static final String NAMESPACE_9ML = "http://nineml.incf.org/9ML/0.3";

	public static final String SCHEMA_SPINEML_COMP_LAYER = "http://bimpa.group.shef.ac.uk/SpineML/schemas/SpineMLComponentLayer.xsd";
	public static final String SCHEMA_SPINEML_NET_LAYER = "http://bimpa.group.shef.ac.uk/SpineML/schemas/SpineMLNetworkLayer.xsd";
	public static final String SCHEMA_SPINEML_EXP_LAYER = "http://bimpa.group.shef.ac.uk/SpineML/schemas/SpineMLExperimentLayer.xsd";

	public static final String NAMESPACE_SPINEML_COMP_LAYER = "http://www.shef.ac.uk/SpineMLComponentLayer";
	public static final String NAMESPACE_SPINEML_NET_LAYER = "http://www.shef.ac.uk/SpineMLNetworkLayer";
	public static final String NAMESPACE_SPINEML_EXP_LAYER = "http://www.shef.ac.uk/SpineMLExperimentLayer";

	private List<File> outputFiles = new ArrayList<File>();

	// public static final String LOCAL_9ML_SCHEMA = "src/test/resources/Schemas/sbml-l2v2-schema/sbml.xsd";
	public XineMLWriter(Lems lems, Format format) throws ModelFeatureSupportException, LEMSException, NeuroMLException
	{
		super(lems, format);
	}

	public XineMLWriter(Lems lems, Format format, File outputFolder, String outputFileName) throws ModelFeatureSupportException, LEMSException, NeuroMLException
	{
		super(lems, format, outputFolder, outputFileName);
	}

	@Override
	public void setSupportedFeatures()
	{
		sli.addSupportInfo(format, ModelFeature.ABSTRACT_CELL_MODEL, SupportLevelInfo.Level.MEDIUM);
		sli.addSupportInfo(format, ModelFeature.COND_BASED_CELL_MODEL, SupportLevelInfo.Level.LOW);
		sli.addSupportInfo(format, ModelFeature.SINGLE_COMP_MODEL, SupportLevelInfo.Level.MEDIUM);
		sli.addSupportInfo(format, ModelFeature.NETWORK_MODEL, SupportLevelInfo.Level.LOW);
		sli.addSupportInfo(format, ModelFeature.MULTI_CELL_MODEL, SupportLevelInfo.Level.LOW);
		sli.addSupportInfo(format, ModelFeature.MULTI_POPULATION_MODEL, SupportLevelInfo.Level.LOW);
		sli.addSupportInfo(format, ModelFeature.NETWORK_WITH_INPUTS_MODEL, SupportLevelInfo.Level.LOW);
		sli.addSupportInfo(format, ModelFeature.NETWORK_WITH_PROJECTIONS_MODEL, SupportLevelInfo.Level.NONE);
		sli.addSupportInfo(format, ModelFeature.MULTICOMPARTMENTAL_CELL_MODEL, SupportLevelInfo.Level.NONE);
		sli.addSupportInfo(format, ModelFeature.HH_CHANNEL_MODEL, SupportLevelInfo.Level.LOW);
		sli.addSupportInfo(format, ModelFeature.KS_CHANNEL_MODEL, SupportLevelInfo.Level.NONE);
	}

	public List<File> generateAllFiles() throws ContentError, GenerationException, IOException, ParseError
	{

		String main = getMainScript();
		File outputFile = new File(getOutputFolder(), this.getOutputFileName());
		try
		{
			FileUtil.writeStringToFile(main, outputFile);
			outputFiles.add(outputFile);
		}
		catch(IOException ex)
		{
			throw new ContentError("Error writing to file: " + outputFile.getAbsolutePath(), ex);
		}
		return outputFiles;
	}

	public String getMainScript() throws GenerationException
	{
		try
		{
			StringBuilder mainFile = new StringBuilder();

			StringBuilder abstLayer = new StringBuilder();
			StringBuilder userNetLayer = new StringBuilder(); // User Layer: 9ml; Network Layer: SpineML
			StringBuilder expLayer = new StringBuilder();
			int expLayerFlag = 3;
			int userNetLayerFlag = 4; // User Layer: 9ml; Network Layer: SpineML

			mainFile.append("<?xml version='1.0' encoding='UTF-8'?>\n");

			String namespace = null;
			String schema = null;
			String extraAttr = "";

			String defaultDimension = null;

			if(format.equals(Format.NINEML))
			{
				namespace = NAMESPACE_9ML;
				schema = SCHEMA_9ML;
				defaultDimension = "none";
			}
			else if(format.equals(Format.SPINEML))
			{
				namespace = NAMESPACE_SPINEML_COMP_LAYER;
				schema = SCHEMA_SPINEML_COMP_LAYER;
				defaultDimension = "?";
			}

			String[] attrs = new String[] { "xmlns=" + namespace, extraAttr, "xmlns:xsi=http://www.w3.org/2001/XMLSchema-instance", "xsi:schemaLocation=" + namespace + " " + schema };

			String root = format.getLabel();

			startElement(mainFile, root, attrs);

			String info = "\n" + Utils.getHeaderComment(format) + "\n" + "\nExport of model:\n" + lems.textSummary(false, false);

			addComment(mainFile, info);

			if(format.equals(Format.SPINEML))
			{

				expLayer.append("<?xml version='1.0' encoding='UTF-8'?>\n");
				String[] attrs_exp = new String[] { "xmlns=" + NAMESPACE_SPINEML_EXP_LAYER, extraAttr, "xmlns:xsi=http://www.w3.org/2001/XMLSchema-instance",
						"xsi:schemaLocation=" + NAMESPACE_SPINEML_EXP_LAYER + " " + SCHEMA_SPINEML_EXP_LAYER };

				startElement(expLayer, root, attrs_exp, expLayerFlag);

				addComment(expLayer, info);

				userNetLayer.append("<?xml version='1.0' encoding='UTF-8'?>\n");
				String[] attrs_net = new String[] { "xmlns=" + NAMESPACE_SPINEML_NET_LAYER, extraAttr, "xmlns:xsi=http://www.w3.org/2001/XMLSchema-instance",
						"xsi:schemaLocation=" + NAMESPACE_SPINEML_NET_LAYER + " " + SCHEMA_SPINEML_NET_LAYER };

				startElement(userNetLayer, root, attrs_net, userNetLayerFlag);

				addComment(userNetLayer, info);
			}

			Target target = lems.getTarget();
			Component simCpt = target.getComponent();

			String targetId = simCpt.getStringValue("target");

			Component tgtNet = lems.getComponent(targetId);

			try
			{

				// //addComment(main, "Adding simulation " + simCpt + " of network: " + tgtNet.summary() + "", true);
				if(format.equals(Format.SPINEML))
				{
					startElement(expLayer, "Experiment", "name=" + simCpt.getID() + "description=Export from LEMS", expLayerFlag);
					startEndElement(expLayer, "Model", "network_layer_url=network_" + tgtNet.id, expLayerFlag);

					// <Simulation duration="1" preferred_simulator="BRAHMS"><EulerIntegration dt="0.1"/></Simulation>
					startElement(expLayer, "Simulation", "duration=" + convertToSIUnits(simCpt.getStringValue("length")), expLayerFlag);
					startEndElement(expLayer, "EulerIntegration", "dt=" + convertToSIUnits(simCpt.getStringValue("step")), expLayerFlag);
					endElement(expLayer, "Simulation", expLayerFlag);

				}

				// //addComment(main, "Adding simulation " + simCpt + " of network: " + tgtNet.summary() + "", true);
				String netId = tgtNet.getID();
				// /startElement(userLayer, "network", "id=" + netId, "name=" + netId);
				userNetLayer.append("\n");

				// indent="";
				ArrayList<Component> pops = tgtNet.getChildrenAL("populations");

				int initAssNum = 0;
				int onCondNum = 0;

				// /startElement(userLayer, "groups?");
				for(Component pop : pops)
				{
					String compRef = pop.getStringValue("component");
					Component popComp = lems.getComponent(compRef);
					ComponentType type = popComp.getComponentType();

					initAssNum = initAssNum + type.getDynamics().getOnStarts().size();
					onCondNum = onCondNum + type.getDynamics().getOnConditions().size();

					int num = Integer.parseInt(pop.getStringValue("size"));
					addComment(userNetLayer, "Population " + pop.getID() + " contains " + num + " instances of components of: " + popComp, true);

					startElement(userNetLayer, "Population", userNetLayerFlag);

					startElement(userNetLayer, "Neuron", "name=" + pop.id, "size=" + num, "url=" + this.getOutputFileName(), userNetLayerFlag);

					for(ParamValue pv : popComp.getParamValues())
					{
						// <Property name="tau_refractory" dimension="mS"><FixedValue value="5"/></Property>
						startElement(userNetLayer, "Property", "name=" + pv.getName(), "dimension=" + defaultDimension, userNetLayerFlag);
						startEndElement(userNetLayer, "FixedValue", "value=" + pv.getDoubleValue(), userNetLayerFlag);
						endElement(userNetLayer, "Property", userNetLayerFlag);

					}
					endElement(userNetLayer, "Neuron", userNetLayerFlag);

					endElement(userNetLayer, "Population", userNetLayerFlag);

				}

				// /endElement(userLayer, "groups?");
				userNetLayer.append("\n");

				ArrayList<String> compTypesAdded = new ArrayList<String>();

				for(Component pop : pops)
				{
					String compRef = pop.getStringValue("component");
					Component popCompFull = lems.getComponent(compRef);
					ComponentType ctFull = popCompFull.getComponentType();

					if(!compTypesAdded.contains(ctFull.getName()))
					{

						compTypesAdded.add(ctFull.getName());

						ComponentType ctFlat;
						Component cpFlat;

						boolean flatten = false;

						if(flatten)
						{
							try
							{
								ComponentFlattener cf = new ComponentFlattener(lems, popCompFull);
								ctFlat = cf.getFlatType();
								cpFlat = cf.getFlatComponent();

								lems.addComponentType(ctFlat);
								lems.addComponent(cpFlat);
								/*
								 * String typeOut = XMLSerializer.serialize(ctFlat); String cptOut = XMLSerializer.serialize(cpFlat);
								 * 
								 * E.info("Flat type: \n" + typeOut); E.info("Flat cpt: \n" + cptOut);
								 * 
								 * lems.resolve(ctFlat); lems.resolve(cpFlat);
								 */

							}
							catch(Exception e)
							{
								throw new GenerationException("Error when flattening component: " + popCompFull, e);
							}
						}
						else
						{
							ctFlat = ctFull;
							cpFlat = popCompFull;
						}

						String dynInitRegInfo = "";
						String ocTargetRegInfo = "";

						String defaultRegime = "defaultRegime";

						if(format.equals(Format.SPINEML))
						{
							dynInitRegInfo = "initial_regime=" + defaultRegime;
							ocTargetRegInfo = "target_regime=" + defaultRegime;
						}

						StringBuilder params = new StringBuilder();
						StringBuilder ports = new StringBuilder();
						StringBuilder dynamics = new StringBuilder();
						StringBuilder stateVars = new StringBuilder();
						StringBuilder regimes = new StringBuilder();

						startElement(abstLayer, "ComponentClass", "name=" + ctFlat.getName());

						for(Parameter param : ctFlat.getParameters())
						{
							startEndElement(params, "Parameter", "name=" + param.getName(), "dimension=" + defaultDimension);
						}
						for(Constant constant : ctFlat.getConstants())
						{
							startEndElement(params, "Parameter", "name=" + constant.getName(), "dimension=" + defaultDimension);
						}
						for(Exposure exp : ctFlat.getExposures())
						{
							if(format.equals(Format.NINEML))
							{
								startEndElement(ports, "AnalogPort", "name=" + exp.getName(), "mode=send", "dimension=" + defaultDimension);
							}
							else if(format.equals(Format.SPINEML))
							{
								startEndElement(ports, "AnalogSendPort", "name=" + exp.getName());
							}
						}
						for(EventPort port : ctFlat.getEventPorts())
						{

							if(format.equals(Format.NINEML))
							{
								startEndElement(ports, "EventPort", "name=" + port.getName(), "mode=" + (port.direction.equals("out") ? "send" : "receive"));
							}
							else if(format.equals(Format.SPINEML))
							{
								startEndElement(ports, (port.direction.equals("out") ? "EventSendPort" : "EventReceivePort"), "name=" + port.getName());
							}
						}

						Dynamics dyn = ctFlat.getDynamics();

						if(dyn.getRegimes().isEmpty())
						{
							startElement(dynamics, "Dynamics", dynInitRegInfo);

							startElement(regimes, "Regime", "name=" + defaultRegime);

							for(TimeDerivative td : dyn.getTimeDerivatives())
							{
								startElement(regimes, "TimeDerivative", "variable=" + td.getVariable());
								startEndTextElement(regimes, "MathInline", td.getValueExpression());
								endElement(regimes, "TimeDerivative");
							}
							for(DerivedVariable dv : dyn.getDerivedVariables())
							{
								if(dv.getReduce() == null || dv.getReduce().equals("null"))
								{
									startElement(regimes, "Alias", "name=" + dv.getName());
									startEndTextElement(regimes, "MathInline", dv.getValueExpression());
									endElement(regimes, "Alias");
								}
							}

							for(OnCondition oc : dyn.getOnConditions())
							{

								startElement(regimes, "OnCondition", ocTargetRegInfo);

								StringBuilder trigger = new StringBuilder();

								startElement(trigger, "Trigger");
								if(oc.test != null)
								{
									startEndTextElement(trigger, "MathInline", oc.test);
								}
								endElement(trigger, "Trigger");
								if(format.equals(Format.NINEML))
								{
									regimes.append(trigger);
								}

								for(StateAssignment sa : oc.getStateAssignments())
								{

									startElement(regimes, "StateAssignment", "variable=" + sa.getVariable());

									startEndTextElement(regimes, "MathInline", sa.getValueExpression());

									endElement(regimes, "StateAssignment");
								}
								for(EventOut eo : oc.getEventOuts())
								{
									startEndElement(regimes, "EventOut", "port=" + eo.port);
								}

								if(format.equals(Format.SPINEML))
								{
									regimes.append(trigger);
								}

								endElement(regimes, "OnCondition");
							}
							endElement(regimes, "Regime");

						}
						else
						{
							startElement(dynamics, "Dynamics-MultiRegimesNotYetImpl", dynInitRegInfo);
						}

						for(StateVariable sv : dyn.getStateVariables())
						{
							startEndElement(stateVars, "StateVariable", "name=" + sv.getName(), "dimension=" + defaultDimension);

						}

						if(format.equals(Format.NINEML))
						{
							abstLayer.append(params);
							abstLayer.append(ports);
							dynamics.append(stateVars);
							dynamics.append(regimes);
							endElement(dynamics, "Dynamics");
							abstLayer.append(dynamics);
						}
						else if(format.equals(Format.SPINEML))
						{
							dynamics.append(regimes);
							dynamics.append(stateVars);
							endElement(dynamics, "Dynamics");
							abstLayer.append(dynamics);
							abstLayer.append(ports);
							abstLayer.append(params);
						}

						endElement(abstLayer, "ComponentClass");
					}
				}

				mainFile.append(abstLayer);
				mainFile.append("\n");

				endElement(mainFile, root);
			}
			catch(ContentError e)
			{
				throw new GenerationException("Error with LEMS content", e);

			}

			if(format.equals(Format.SPINEML))
			{
				endElement(expLayer, "Experiment", expLayerFlag);
				endElement(expLayer, root, expLayerFlag);

				String expFilename = "Experiment_" + this.getOutputFileName();
				File expFile = new File(getOutputFolder(), expFilename);

				FileUtil.writeStringToFile(expLayer.toString(), expFile);
				outputFiles.add(expFile);

				endElement(userNetLayer, root, userNetLayerFlag);
				String userFilename = "NetLayer_" + this.getOutputFileName();
				File netFile = new File(getOutputFolder(), userFilename);

				FileUtil.writeStringToFile(userNetLayer.toString(), netFile);
				outputFiles.add(netFile);
			}

			return mainFile.toString();
		}
		catch(ContentError e)
		{
			throw new GenerationException("Error with LEMS content", e);
		}
		catch(ParseError e)
		{
			throw new GenerationException("Error parsing LEMS content", e);
		}
		catch(IOException e)
		{
			throw new GenerationException("Error with file I/O", e);
		}

	}

	private float convertToSIUnits(String neuromlQuantity) throws ParseError, ContentError
	{
		DimensionalQuantity dq = QuantityReader.parseValue(neuromlQuantity, lems.getUnits());
		return (float) dq.getDoubleValue();
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
		lemsFiles.add(new File("../NeuroML2/LEMSexamples/LEMS_NML2_Ex2_Izh.xml"));
		lemsFiles.add(new File("../git/HindmarshRose1984/NeuroML2/Run_Regular_HindmarshRose.xml"));
		// lemsFiles.add(new File("../neuroConstruct/osb/cerebellum/cerebellar_granule_cell/GranuleCell/neuroConstruct/generatedNeuroML2/LEMS_GranuleCell.xml"));
		// lemsFiles.add(new File("../neuroConstruct/osb/invertebrate/lobster/PyloricNetwork/neuroConstruct/generatedNeuroML2/LEMS_PyloricPacemakerNetwork.xml"));
		// lemsFiles.add(new File("../neuroConstruct/osb/invertebrate/celegans/CElegansNeuroML/CElegans/pythonScripts/c302/LEMS_c302_A.xml"));
		// lemsFiles.add(new File("../git/GPUShowcase/NeuroML2/LEMS_simplenet.xml"));

		for(Format v : new Format[] { Format.NINEML, Format.SPINEML })
		{

			for(File lemsFile : lemsFiles)
			{
				Lems lems = Utils.readLemsNeuroMLFile(lemsFile).getLems();
				XineMLWriter nw = new XineMLWriter(lems, v, lemsFile.getParentFile(), lemsFile.getName().replaceAll(".xml", "." + v.getExtension()));
				List<File> sr = nw.convert();

				System.out.println("Generated: " + sr);
				for(File f : sr)
				{
					System.out.println("Generated file: " + f.getAbsolutePath());
				}
			}
		}

	}

	@Override
	public List<File> convert()
	{
		List<File> outputFiles = new ArrayList<File>();
		try
		{
			outputFiles = this.generateAllFiles();
		}
		catch(GenerationException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch(ContentError e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch(ParseError e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch(IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return outputFiles;
	}

}

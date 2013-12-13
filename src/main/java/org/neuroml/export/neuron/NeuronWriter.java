package org.neuroml.export.neuron;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Properties;

import javax.xml.bind.JAXBException;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.lemsml.export.base.GenerationException;
import org.lemsml.export.dlems.DLemsWriter;
import org.lemsml.jlems.core.expression.ParseError;
import org.lemsml.jlems.core.logging.E;
import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.core.type.Component;
import org.lemsml.jlems.core.type.Dimension;
import org.lemsml.jlems.core.type.Exposure;
import org.lemsml.jlems.core.type.Lems;
import org.lemsml.jlems.core.type.LemsCollection;
import org.lemsml.jlems.core.type.ParamValue;
import org.lemsml.jlems.core.type.Requirement;
import org.lemsml.jlems.core.type.Target;
import org.lemsml.jlems.core.type.dynamics.Case;
import org.lemsml.jlems.core.type.dynamics.ConditionalDerivedVariable;
import org.lemsml.jlems.core.type.dynamics.DerivedVariable;
import org.lemsml.jlems.core.type.dynamics.Dynamics;
import org.lemsml.jlems.core.type.dynamics.OnCondition;
import org.lemsml.jlems.core.type.dynamics.OnEntry;
import org.lemsml.jlems.core.type.dynamics.OnEvent;
import org.lemsml.jlems.core.type.dynamics.OnStart;
import org.lemsml.jlems.core.type.dynamics.Regime;
import org.lemsml.jlems.core.type.dynamics.StateAssignment;
import org.lemsml.jlems.core.type.dynamics.StateVariable;
import org.lemsml.jlems.core.type.dynamics.TimeDerivative;
import org.lemsml.jlems.core.type.dynamics.Transition;
import org.lemsml.jlems.io.util.FileUtil;
import org.neuroml.export.Utils;
import org.neuroml.export.base.BaseWriter;
import org.neuroml.export.base.JSONCellSerializer;
import org.neuroml.model.Cell;
import org.neuroml.model.ChannelDensity;
import org.neuroml.model.ChannelDensityGHK;
import org.neuroml.model.ChannelDensityNernst;
import org.neuroml.model.Segment;
import org.neuroml.model.Species;
import org.neuroml.model.Standalone;
import org.neuroml.model.util.NeuroMLElements;
import org.neuroml.model.util.NeuroMLException;

public class NeuronWriter extends BaseWriter {

	ArrayList<File> allGeneratedFiles = new ArrayList<File>();
	static boolean debug = false;

	public enum ChannelConductanceOption {
		FIXED_REVERSAL_POTENTIAL, USE_NERNST, USE_GHK;
		float erev;

	};

	public NeuronWriter(Lems l) {
		super(l, NRNConst.NEURON_FORMAT);
	}

	@Override
	protected void addComment(StringBuilder sb, String comment) {

		if (comment.indexOf("\n") < 0) {
			sb.append(NRNConst.comm + comment + "\n");
		} else {
			sb.append(NRNConst.commPre + "\n" + comment + "\n"
					+ NRNConst.commPost + "\n");
		}
	}

	private void reset() {
		allGeneratedFiles.clear();
	}

	public ArrayList<File> generateMainScriptAndMods(File mainFile)
			throws ContentError, ParseError, IOException, JAXBException,
			GenerationException, NeuroMLException {
		String main = generate(mainFile.getParentFile());
		try {
			FileUtil.writeStringToFile(main, mainFile);
			allGeneratedFiles.add(mainFile);
		} catch (IOException ex) {
			throw new ContentError("Error writing to file: " + mainFile.getAbsolutePath(), ex);
		}
		return allGeneratedFiles;
	}

	@Override
	public String getMainScript() throws GenerationException, NeuroMLException {
		try {
			return generate(null);
		} catch (ContentError e) {
			throw new GenerationException("Error with LEMS content", e);
		} catch (ParseError e) {
			throw new GenerationException("Error parsing LEMS content", e);
		} catch (IOException e) {
			throw new GenerationException("Error with file I/O", e);
		} catch (JAXBException e) {
			throw new GenerationException("Error with parsing XML", e);
		}

	}

	private static String getStateVarName(String sv) {
		if (sv.equals(NRNConst.NEURON_VOLTAGE)) {
			return NRNConst.NEURON_VOLTAGE + NRNConst.RESERVED_STATE_SUFFIX;
		} else {
			return sv;
		}
	}

	private static String checkForBinaryOperators(String expr) {
		return expr.replace("\\.gt\\.", ">").replace("\\.geq\\.", ">=")
				.replace("\\.lt\\.", "<").replace("\\.leq\\.", "<=")
				.replace("\\.and\\.", "&&");
	}

	private static String checkForStateVarsAndNested(String expr,
			Component comp,
			HashMap<String, HashMap<String, String>> paramMappings) {

		if (expr == null) {
			return null;
		}

		String newExpr = expr.trim();

		newExpr = newExpr.replaceAll("\\.geq\\.", ">="); // TODO, use strings from
		// GreaterThanOrEqualsNode
		// in jLEMS
		newExpr = newExpr.replaceAll("\\.gt\\.", ">");
		newExpr = newExpr.replaceAll("\\.leq\\.", "<=");
		newExpr = newExpr.replaceAll("\\.lt\\.", "<=");
		newExpr = newExpr.replaceAll("\\.eq\\.", "==");
		newExpr = newExpr.replaceAll("\\.and.", "&&");

		newExpr = newExpr.replaceAll(" ln\\(", " log(");

		HashMap<String, String> paramMappingsComp = paramMappings.get(comp.getUniqueID());

		for (String origName : paramMappingsComp.keySet()) {
			String newName = paramMappingsComp.get(origName);
			newExpr = Utils.replaceInExpression(newExpr, origName, newName);
		}

		// Since modlunit is confused about statements with: (100) * v
		// It assumes the 100 is a scaling factor for units & complains
		// Change to: 100 * v
		if (newExpr.charAt(0) == '(') {
			int nextBracket = newExpr.indexOf(")");
			String num = newExpr.substring(1, nextBracket - 1).trim();
			try {
				float f = Float.parseFloat(num);
				newExpr = f + " " + newExpr.substring(nextBracket + 1);
			} catch (NumberFormatException e) {
			}
		}

		return newExpr;
	}

	public String generate(File dirForMods) throws NeuroMLException,
	IOException, JAXBException, GenerationException, ContentError,
	ParseError {

		reset();
		StringBuilder main = new StringBuilder();

		addComment(main, "Neuron simulator export for:\n\n" + lems.textSummary(false, false) + "\n\n" + Utils.getHeaderComment(format) + "\n"); 

		main.append("import neuron\n");
		main.append("h = neuron.h\n");
		main.append("h.load_file(\"nrngui.hoc\")\n\n");
		main.append("h(\"objref p\")\n");
		main.append("h(\"p = new PythonObject()\")\n\n");

		Target target = lems.getTarget();

		Component simCpt = target.getComponent();

		String targetId = simCpt.getStringValue("target");

		Component targetComp = lems.getComponent(targetId);
		String info = "Adding simulation " + simCpt + " of network/component: " + targetComp.summary();

		E.info(info);

		addComment(main, info);

		ArrayList<Component> popsOrComponents = targetComp.getChildrenAL("populations");

		E.info("popsOrComponents: " + popsOrComponents);

		HashMap<String, Integer> compMechsCreated = new HashMap<String, Integer>();
		HashMap<String, String> compMechNamesHoc = new HashMap<String, String>();

		HashMap<String, Cell> compIdsVsCells = new HashMap<String, Cell>();
		HashMap<String, String> popIdsVsCellIds = new HashMap<String, String>();

		boolean simulatingNetwork = true;

		if (popsOrComponents.isEmpty()) {
			popsOrComponents.add(targetComp);
			simulatingNetwork = false;
		} else {
			if (targetComp.getComponentType().getName().equals("networkWithTemperature")
					|| (targetComp.hasAttribute("type") 
							&& targetComp.getStringValue("type").equals("networkWithTemperature"))) {
				String temp = targetComp.getStringValue("temperature");
				float tempSI = Utils.getMagnitudeInSI(temp);
				main.append("\n# Temperature used for network: " + tempSI + " K\n");
				main.append("h.celsius = " + tempSI + " - 273.15\n\n");

			}
		}

		for (Component popsOrComponent : popsOrComponents) {

			String compReference;
			String popName;
			int number;
			Component popComp;

			if (popsOrComponent.getComponentType().getName().equals(NeuroMLElements.POPULATION)) {
				compReference = popsOrComponent.getStringValue(NeuroMLElements.POPULATION_COMPONENT);
				number = Integer.parseInt(popsOrComponent.getStringValue(NeuroMLElements.POPULATION_SIZE));
				popComp = lems.getComponent(compReference);
				popName = popsOrComponent.getID();
				popIdsVsCellIds.put(popName, compReference);
			} else if (popsOrComponent.getComponentType().getName().equals(NeuroMLElements.POPULATION_LIST)) {
				compReference = popsOrComponent.getStringValue(NeuroMLElements.POPULATION_COMPONENT);
				popComp = lems.getComponent(compReference);
				number = 0;
				for (Component comp : popsOrComponent.getAllChildren()) {

					// main.append("print \""+comp+"\"");
					if (comp.getComponentType().getName().equals(NeuroMLElements.INSTANCE))
						number++;
				}
				popComp.getAllChildren().size();
				popName = popsOrComponent.getID();
				popIdsVsCellIds.put(popName, compReference);
			} else {
				// compReference = popsOrComponent.getComponentType().getName();
				number = 1;
				popComp = popsOrComponent;
				popName = NRNConst.DUMMY_POPULATION_PREFIX + popComp.getName();

			}

			ArrayList<String> generatedModComponents = new ArrayList<String>();

			String mechName = popComp.getComponentType().getName();

			main.append("print \"Population " + popName + " contains " + number
					+ " instance(s) of component: " + popComp.getID()
					+ " of type: " + popComp.getComponentType().getName()
					+ " \"\n\n");

			if (popComp.getComponentType().isOrExtends(NeuroMLElements.CELL_COMP_TYPE)) {

				Cell cell = getCellFromComponent(popComp);
				compIdsVsCells.put(popComp.getID(), cell);
				String cellString = generateCellFile(cell);
				String cellName = popComp.getID();

				String fileName = cellName + ".hoc";
				File cellFile = new File(dirForMods, fileName);
				E.info("Writing to: " + cellFile);

				main.append("h.load_file(\"" + fileName + "\")\n");

				main.append("a_" + popName + " = []\n");

				main.append("h(\"n_" + popName + " = " + number + "\")\n");

				main.append("h(\"objectvar a_" + popName + "[n_" + popName + "]\")\n");

				main.append("for i in range(int(h.n_" + popName + ")):\n");
				// main.append("    cell = h."+cellName+"()\n");
				main.append("    h(\"a_" + popName + "[%i] = new " + cellName + "()\"%i)\n");
				// main.append("    cell."+getNrnSectionName(cell.getMorphology().getSegment().get(0))+".push()\n");
				main.append("    h(\"access a_" + popName + "[%i]." + getNrnSectionName(cell.getMorphology().getSegment().get(0)) + "\"%i)\n\n");

				main.append(String.format("h(\"proc initialiseV_%s() { for i = 0, n_%s-1 { a_%s[i].set_initial_v() } }\")\n", popName, popName, popName));
				main.append(String.format("h(\"objref fih_%s\")\n", popName));
				main.append(String.format("h(\'{fih_%s = new FInitializeHandler(0, \"initialiseV_%s()\")}\')\n\n", popName, popName));

				main.append(String.format("h(\"proc initialiseIons_%s() { for i = 0, n_%s-1 { a_%s[i].set_initial_ion_properties() } }\")\n", popName, popName, popName));
				main.append(String.format("h(\"objref fih_ion_%s\")\n", popName));
				main.append(String.format("h(\'{fih_ion_%s = new FInitializeHandler(1, \"initialiseIons_%s()\")}\')\n\n", popName, popName));

				try {
					FileUtil.writeStringToFile(cellString, cellFile);
					allGeneratedFiles.add(cellFile);
				} catch (IOException ex) {
					throw new ContentError("Error writing to file: " + cellFile.getAbsolutePath(), ex);
				}

				for (ChannelDensity cd : cell.getBiophysicalProperties().getMembraneProperties().getChannelDensity()) {
					String ionChannel = cd.getIonChannel();
					if (!generatedModComponents.contains(ionChannel)) {
						Component ionChannelComp = lems.getComponent(ionChannel);
						ChannelConductanceOption option = ChannelConductanceOption.FIXED_REVERSAL_POTENTIAL;
						option.erev = convertToNeuronUnits(Utils.getMagnitudeInSI(cd.getErev()), "voltage");
						String mod = generateModFile(ionChannelComp, option);
						generatedModComponents.add(ionChannel);

						File modFile = new File(dirForMods, ionChannelComp.getID() + ".mod");
						E.info("-- Writing to: " + modFile);

						try {
							FileUtil.writeStringToFile(mod, modFile);
							allGeneratedFiles.add(modFile);
						} catch (IOException ex) {
							throw new ContentError("Error writing to file: " + modFile.getAbsolutePath(), ex);
						}
					}
				}

				for (ChannelDensityNernst cdn : cell.getBiophysicalProperties().getMembraneProperties().getChannelDensityNernst()) {
					String ionChannel = cdn.getIonChannel();
					if (!generatedModComponents.contains(ionChannel)) {
						Component ionChannelComp = lems.getComponent(ionChannel);
						String mod = generateModFile(ionChannelComp, ChannelConductanceOption.USE_NERNST);
						generatedModComponents.add(ionChannel);

						File modFile = new File(dirForMods, ionChannelComp.getID() + ".mod");
						E.info("-- Writing to: " + modFile);

						try {
							FileUtil.writeStringToFile(mod, modFile);
							allGeneratedFiles.add(modFile);
						} catch (IOException ex) {
							throw new ContentError("Error writing to file: " + modFile.getAbsolutePath(), ex);
						}
					}
				}

				for (ChannelDensityGHK cdg : cell.getBiophysicalProperties().getMembraneProperties().getChannelDensityGHK()) {
					String ionChannel = cdg.getIonChannel();
					if (!generatedModComponents.contains(ionChannel)) {
						Component ionChannelComp = lems.getComponent(ionChannel);
						String mod = generateModFile(ionChannelComp, ChannelConductanceOption.USE_GHK);
						generatedModComponents.add(ionChannel);

						File modFile = new File(dirForMods, ionChannelComp.getID() + ".mod");
						E.info("-- Writing to: " + modFile);

						try {
							FileUtil.writeStringToFile(mod, modFile);
							allGeneratedFiles.add(modFile);
						} catch (IOException ex) {
							throw new ContentError("Error writing to file: " + modFile.getAbsolutePath(), ex);
						}
					}
				}

				for (Species sp : cell.getBiophysicalProperties().getIntracellularProperties().getSpecies()) {
					String concModel = sp.getConcentrationModel();
					if (!generatedModComponents.contains(concModel)) {
						Component concModelComp = lems.getComponent(concModel);
						String mod = generateModFile(concModelComp);
						generatedModComponents.add(concModel);

						File modFile = new File(dirForMods, concModelComp.getID() + ".mod");
						E.info("-- Writing to: " + modFile);

						try {
							FileUtil.writeStringToFile(mod, modFile);
							allGeneratedFiles.add(modFile);
						} catch (IOException ex) {
							throw new ContentError("Error writing to file: " + modFile.getAbsolutePath(), ex);
						}
					}

				}

			} else {
				String mod = generateModFile(popComp);

				File modFile = new File(dirForMods, popComp.getComponentType().getName() + ".mod");
				E.info("Writing to: " + modFile);

				try {
					FileUtil.writeStringToFile(mod, modFile);
					allGeneratedFiles.add(modFile);
				} catch (IOException ex) {
					throw new ContentError("Error writing to file: " + modFile.getAbsolutePath(), ex);
				}

				main.append("h(\" n_" + popName + " = " + number + " \")\n");
				main.append("h(\" create " + popName + "[" + number + "]\")\n");
				main.append("h(\" objectvar m_" + mechName + "[" + number + "] \")\n\n");

				main.append("for i in range(int(h.n_" + popName + ")):\n");
				String instName = popName + "[i]";
				// main.append(instName + " = h.Section()\n");
				double defaultRadius = 5;
				main.append("    h." + instName + ".L = " + defaultRadius * 2 + "\n");
				main.append("    h." + instName + "(0.5).diam = " + defaultRadius * 2 + "\n");

				if (popComp.getComponentType().isOrExtends(NeuroMLElements.BASE_CELL_CAP_COMP_TYPE)) {
					double capTotSI = popComp.getParamValue("C").getDoubleValue();
					double area = 4 * Math.PI * defaultRadius * defaultRadius;
					double specCapNeu = 10e13 * capTotSI / area;
					main.append("    h." + instName + "(0.5).cm = " + specCapNeu + "\n");
				} else {
					main.append("    h." + instName + "(0.5).cm = 318.31927\n");
				}

				main.append("    h." + instName + ".push()\n");
				main.append("    h(\" " + instName.replaceAll("\\[i\\]", "[%i]") + "  { m_" + mechName + "[%i] = new " + mechName + "(0.5) } \"%(i,i))\n\n");

				if (!compMechsCreated.containsKey(mechName)) {
					compMechsCreated.put(mechName, 0);
				}

				compMechsCreated.put(mechName,
						compMechsCreated.get(mechName) + 1);

				// String hocMechName = mechName + "[" +
						// (compMechsCreated.get(mechName) - 1) + "]";
				String hocMechName = "m_" + mechName + "[i]";

				compMechNamesHoc.put(instName, hocMechName);

				LemsCollection<ParamValue> pvs = popComp.getParamValues();
				for (ParamValue pv : pvs) {
					main.append("    " + "h." + hocMechName + "." + pv.getName() + " = " + convertToNeuronUnits((float) pv.getDoubleValue(), pv.getDimensionName()) + "\n");
				}

			}

		}

		ArrayList<Component> projections = targetComp.getChildrenAL("projections");

		for (Component projection : projections) {

			String id = projection.getID();
			String prePop = projection.getStringValue("presynapticPopulation");
			String postPop = projection.getStringValue("postsynapticPopulation");
			Cell preCell = compIdsVsCells.get(popIdsVsCellIds.get(prePop));
			Cell postCell = compIdsVsCells.get(popIdsVsCellIds.get(postPop));
			String synapse = projection.getStringValue("synapse");
			int number = 0;
			for (Component comp : projection.getAllChildren()) {

				if (comp.getComponentType().getName().equals(NeuroMLElements.CONNECTION))
					number++;
			}

			addComment(main, String.format("Adding projection: %s, from %s to %s with synapse %s, %d connection(s)", id, prePop, postPop, synapse, number));

			Component synapseComp = lems.getComponent(synapse);

			String mod = generateModFile(synapseComp);

			File modFile = new File(dirForMods, synapseComp.getID() + ".mod");
			E.info("Writing to: " + modFile);

			try {
				FileUtil.writeStringToFile(mod, modFile);
				allGeneratedFiles.add(modFile);
			} catch (IOException ex) {
				throw new ContentError("Error writing to file: " + modFile.getAbsolutePath(), ex);
			}

			String synObjName = String.format("syn_%s_%s", id, synapse);

			main.append(String.format("h(\"objectvar %s[%d]\")\n", synObjName, number));

			int index = 0;
			for (Component conn : projection.getAllChildren()) {

				if (conn.getComponentType().getName().equals(NeuroMLElements.CONNECTION)) {
					int preCellId = Integer.parseInt(conn.getStringValue("preCellId").split("/")[2]);
					int postCellId = Integer.parseInt(conn.getStringValue("postCellId").split("/")[2]);

					int preSegmentId = Integer.parseInt(conn.getStringValue("preSegmentId"));
					int postSegmentId = Integer.parseInt(conn.getStringValue("postSegmentId"));

					float preFractionAlong = Float.parseFloat(conn.getStringValue("preFractionAlong"));
					float postFractionAlong = Float.parseFloat(conn.getStringValue("postFractionAlong"));

					if (preSegmentId != 0 || postSegmentId != 0) {
						throw new GenerationException(
								"Connections on locations other than segment id=0 not yet supported...");
					}
					String preSecName = getNrnSectionName(preCell.getMorphology().getSegment().get(0));
					String postSecName = getNrnSectionName(postCell.getMorphology().getSegment().get(0));

					main.append(String.format("h(\"a_%s[%d].%s %s[%d] = new %s(%f)\")\n",
							postPop, postCellId, postSecName, synObjName,
							index, synapse, postFractionAlong));
					main.append(String.format("h(\"a_%s[%d].%s a_%s[%d].synlist.append(new NetCon(&v(%f), %s[%d], 0, 0, 1))\")\n\n",
							prePop, preCellId, preSecName, postPop,
							postCellId, preFractionAlong, synObjName,
							index));
					index++;
				}
			}

			// main.append(String.format("h(\"objectvar %s\")\n", synObjName));

			// {a_CG2[0].Soma syn_NetConn_1_DoubExpSyn[0] = new DoubExpSyn(0.5)}
			// {a_CG1[0].Soma a_CG2[0].synlist.append(new NetCon(&v(0.5),
			// syn_NetConn_1_DoubExpSyn[0], -20.0, 5.0, 1.0))}

		}

		ArrayList<Component> inputLists = targetComp.getChildrenAL("inputs");

		for (Component inputList : inputLists) {
			String inputReference = inputList.getStringValue("component");
			Component inputComp = lems.getComponent(inputReference);

			String mod = generateModFile(inputComp);

			File modFile = new File(dirForMods, inputComp.getID() + ".mod");
			E.info("Writing to: " + modFile);

			try {
				FileUtil.writeStringToFile(mod, modFile);
				allGeneratedFiles.add(modFile);
			} catch (IOException ex) {
				throw new ContentError("Error writing to file: " + modFile.getAbsolutePath(), ex);
			}

			String pop = inputList.getStringValue("population");
			ArrayList<Component> inputs = inputList.getChildrenAL("inputs");

			for (Component input : inputs) {
				String targetString = input.getStringValue("target");
				Cell cell;
				String cellNum;

				if (targetString.indexOf("[") > 0) {
					String[] parts1 = targetString.split("/");
					String[] parts2 = parts1[1].split("\\[");
					cellNum = parts2[1].split("\\]")[0];
					String popName = parts2[0];
					String cellId = popIdsVsCellIds.get(popName);
					cell = compIdsVsCells.get(cellId);

				} else {
					String[] parts = targetString.split("/");
					cellNum = parts[2];
					String cellId = parts[3];
					cell = compIdsVsCells.get(cellId);
				}

				String inputName = inputList.getID() + "_" + input.getID();

				addComment(main, "Adding input: " + input);

				main.append(String.format("\nh(\"objectvar %s\")\n", inputName));
				main.append(String.format("h(\"a_%s[%s].%s { %s = new %s(0.5) } \")\n\n", pop,
						cellNum, getNrnSectionName(cell.getMorphology().getSegment().get(0)), inputName,
						inputComp.getID()));

			}

		}

		addComment(main,
				"The following code is based on Andrew's test_HH.py example...");

		main.append("trec = h.Vector()\n");
		main.append("trec.record(h._ref_t)\n\n");

		StringBuilder toRec = new StringBuilder();

		ArrayList<String> displayGraphs = new ArrayList<String>();
		HashMap<String, ArrayList<String>> plots = new HashMap<String, ArrayList<String>>();

		for (Component dispComp : simCpt.getAllChildren()) {
			if (dispComp.getName().indexOf("Display") >= 0) {

				String dispId = dispComp.getID();
				int plotColour = 1;

				for (Component lineComp : dispComp.getAllChildren()) {
					if (lineComp.getName().indexOf("Line") >= 0) {

						String ref = lineComp.getStringValue("quantity");
						String origRef = ref;
						String varRef = null;
						String var;

						String dim = "None";

						// if (false && !simulatingNetwork) {
						// varRef = ref;
						// var = ref;
						// try {
						// Exposure exp =
						// targetComp.getComponentType().getExposure(var);
						// dim = exp.getDimension().getName();
						// } catch(ContentError e) {
						//
						// }
						//
						// System.out.println("Plotting " + var +
						// " (dim: "+dim+") on comp: "+targetComp);
						// }
						// else
						// {
						String pop;
						String num;
						String[] varParts = null;
						Component popComp = null;

						if (ref.indexOf("/") < 0 && !simulatingNetwork) {

							popComp = targetComp;
							var = ref;
							num = "0";
							pop = NRNConst.DUMMY_POPULATION_PREFIX + popComp.getName();

						} else {
							if (ref.indexOf('[') > 0) {
								pop = ref.split("/")[0].split("\\[")[0];
								num = ref.split("\\[")[1].split("\\]")[0];
								var = ref.split("/")[1];
							} else {
								String[] parts = ref.split("/");
								pop = parts[0];
								num = parts[1];
								var = "";
								varParts = new String[parts.length - 2];
								for (int i = 2; i < parts.length; i++) {
									if (i > 2)
										var += "_";
									var += parts[i];
									varParts[i - 2] = parts[i];
								}
							}

							for (Component popsOrComponent : popsOrComponents) {
								if (popsOrComponent.getID().equals(pop)) {
									popComp = lems.getComponent(popsOrComponent.getStringValue("component"));
								}
							}
							// }
							String popArray = "a_" + pop;

							varRef = popArray + "_" + num;

							varRef = compMechNamesHoc.get(varRef) + "." + var;

							boolean isCondBasedCell = popComp.getComponentType().isOrExtends(NeuroMLElements.CELL_COMP_TYPE);

							if (var.equals(NRNConst.NEURON_VOLTAGE)) {
								varRef = compMechNamesHoc.get(popArray + "_" + num) + "." + NRNConst.V_COPY_PREFIX + var;

							}
							if (isCondBasedCell) {
								if (varParts == null) {
									Cell cell = compIdsVsCells.get(popComp.getID());
									String varInst = getNrnSectionName(cell.getMorphology().getSegment().get(0));
									varRef = popArray + "[" + num + "]." + varInst + "." + var;
								} else {
									Cell cell = compIdsVsCells.get(varParts[0]);
									String varInst = getNrnSectionName(cell.getMorphology().getSegment().get(0));

									if (varParts.length == 5) {
										String cellId = varParts[0];
										// String biophys = varParts[1];
										// String membProps = varParts[2];
										String channelDensId = varParts[3];
										String variable = varParts[4];
										Component cellComp = lems.getComponent(cellId);
										ArrayList<Component> channelDensityComps = cellComp.getChild("biophysicalProperties").getChild("membraneProperties").getChildrenAL("channelDensities");

										if (variable.equals("gDensity")) {
											for (Component c : channelDensityComps) {
												if (c.getID().equals(channelDensId)) variable = "gion_" + c.getStringValue("ionChannel");
											}
										}

										varInst += "." + variable;

										varRef = popArray + "[" + num + "]." + varInst;
									} else if (varParts.length > 5) {
										// String cellRef = varParts[0];
										// String biophys = varParts[1];
										// String membProps = varParts[2];
										// String channelDensId = varParts[3];
										String ionChanId = varParts[4];

										for (int i = 5; i < varParts.length; i++) {
											if (i == 5)
												varInst += ".";
											else
												varInst += "_";
											varInst += varParts[i];
										}

										varInst += "_" + ionChanId;

										varRef = popArray + "[" + num + "]."
												+ varInst;
									} else {
										for (int i = 1; i < varParts.length; i++) {
											String part = varParts[i];
											if (part.equals("caConc")) {
												part = "cai";
											}
											varInst += "." + part;
										}
										varRef = popArray + "[" + num + "]." + varInst;

									}
								}
							} else {
								System.out.println("Plotting " + var + " on cell " + num + " in " + pop + " of type " + popComp + " (was: " + origRef + "), " + compMechNamesHoc); String mechRef = compMechNamesHoc.get(pop + "[i]");

								varRef = mechRef.replaceAll("\\[i\\]", "["
										+ num + "]")
										+ "." + var;
							}
							System.out.println("Plotting " + var + " on cell "
									+ num + " in " + pop + " of type "
									+ popComp + " (was: " + origRef
									+ "), varRef: " + varRef);

							try {
								Exposure exp = popComp.getComponentType().getExposure(var);
								dim = exp.getDimension().getName();
							} catch (ContentError e) {

							}

						}

						float scale = 1 / convertToNeuronUnits((float) lineComp.getParamValue("scale").getDoubleValue(), dim);

						String plotRef = "\"" + varRef + "\"";

						if (scale != 1) {
							plotRef = "\"(" + scale + ") * " + varRef + "\"";
						}

						System.out.println(varRef + ", " + plotRef + ", " + compMechNamesHoc);

						String dispGraph = "display_" + dispId;
						if (!displayGraphs.contains(dispGraph)) {
							displayGraphs.add(dispGraph);
						}

						if (plots.get(dispGraph) == null)
							plots.put(dispGraph, new ArrayList<String>());

						plots.get(dispGraph).add("# Line, plotting: " + ref);

						plots.get(dispGraph).add(dispGraph + ".addexpr(" + plotRef + ", " + plotRef + ", " + plotColour + ", 1, 0.8, 0.9, 2)");
						plotColour++;
						if (plotColour > 10) {
							plotColour = 1;
						}

					}
				}
			}
		}
		main.append(toRec);

		String len = simCpt.getStringValue("length");
		len = len.replaceAll("ms", "");
		if (len.indexOf("s") > 0) {
			len = len.replaceAll("s", "").trim();
			len = "" + Float.parseFloat(len) * 1000;
		}

		String dt = simCpt.getStringValue("step");
		dt = dt.replaceAll("ms", "").trim();
		if (dt.indexOf("s") > 0) {
			dt = dt.replaceAll("s", "").trim();
			dt = "" + Float.parseFloat(dt) * 1000;
		}

		main.append("h.tstop = " + len + "\n\n");
		main.append("h.dt = " + dt + "\n\n");
		main.append("h.steps_per_ms = " + (float) (1d / Double.parseDouble(dt)) + "\n\n");

		// main.append("objref SampleGraph\n");
		for (String dg : displayGraphs) {
			addComment(main, "Display: " + dg);
			main.append(dg + " = h.Graph(0)\n");
			main.append(dg + ".size(0,h.tstop,-80.0,50.0)\n");
			main.append(dg + ".view(0, -80.0, h.tstop, 130.0, 80, 330, 330, 250)\n");
			main.append("h.graphList[0].append(" + dg + ")\n");
			for (String plot : plots.get(dg)) {
				main.append(plot + "\n");
			}
			main.append("\n");
		}

		main.append("\n\n");

		HashMap<String, String> outfiles = new HashMap<String, String>();
		HashMap<String, ArrayList<String>> columnsPre = new HashMap<String, ArrayList<String>>();
		HashMap<String, ArrayList<String>> columnsPost = new HashMap<String, ArrayList<String>>();

		String timeRef = "time";
		String timefileName = target.timesFile != null ? target.timesFile
				: "times.dat";
		outfiles.put(timeRef, timefileName);

		columnsPre.put(timeRef, new ArrayList<String>());
		columnsPost.put(timeRef, new ArrayList<String>());

		columnsPre.get(timeRef).add("# Column: " + timeRef);
		columnsPre.get(timeRef).add("h(' objectvar v_" + timeRef + " ')");
		columnsPre.get(timeRef).add("h(' { v_" + timeRef + " = new Vector() } ')");
		columnsPre.get(timeRef).add("h(' v_" + timeRef + ".record(&t) ')");
		columnsPre.get(timeRef).add("h.v_" + timeRef + ".resize((h.tstop * h.steps_per_ms) + 1)");
		columnsPost.get(timeRef).add("    h.f_" + timeRef + ".printf(\"%f\\n\", h.v_" + timeRef + ".get(i))");

		for (Component ofComp : simCpt.getAllChildren()) {
			if (ofComp.getName().indexOf("OutputFile") >= 0) {

				String outfileId = ofComp.getID().replaceAll(" ", "_");
				outfiles.put(outfileId, ofComp.getTextParam("fileName"));

				for (Component colComp : ofComp.getAllChildren()) {

					if (colComp.getName().indexOf("OutputColumn") >= 0) {

						String colId = colComp.getID().replaceAll(" ", "_") + "_" + outfileId;
						String ref = colComp.getStringValue("quantity");
						String origRef = ref;

						String pop;
						String num;
						String var;
						String[] varParts = null;
						Component popComp = null;

						if (ref.indexOf("/") < 0 && !simulatingNetwork) {

							popComp = targetComp;
							var = ref;
							num = "0";
							pop = NRNConst.DUMMY_POPULATION_PREFIX + popComp.getName();

						} else {
							if (ref.indexOf('[') > 0) {
								pop = ref.split("/")[0].split("\\[")[0];
								num = ref.split("\\[")[1].split("\\]")[0];
								var = ref.split("/")[1];
							} else {
								String[] parts = ref.split("/");
								pop = parts[0];
								num = parts[1];
								var = "";
								varParts = new String[parts.length - 2];
								for (int i = 2; i < parts.length; i++) {
									if (i > 2)
										var += "_";
									var += parts[i];
									varParts[i - 2] = parts[i];
								}
							}

							for (Component popsOrComponent : popsOrComponents) {
								if (popsOrComponent.getID().equals(pop)) {
									popComp = lems.getComponent(popsOrComponent.getStringValue("component"));
								}
							}
						}
						String popArray = "a_" + pop;

						System.out.println("Recording " + var + " on cell " + num + " in " + pop + " of type " + popComp + " (was: " + origRef + ")");

						String varRef = popArray + "_" + num;

						varRef = compMechNamesHoc.get(varRef) + "." + var;

						boolean isCondBasedCell = popComp.getComponentType().isOrExtends(NeuroMLElements.CELL_COMP_TYPE);

						if (var.equals(NRNConst.NEURON_VOLTAGE)) {
							varRef = compMechNamesHoc.get(popArray + "_" + num) + "." + NRNConst.V_COPY_PREFIX + var;

						}
						if (isCondBasedCell) {
							if (varParts == null) {
								Cell cell = compIdsVsCells.get(popComp.getID());
								String varInst = getNrnSectionName(cell.getMorphology().getSegment().get(0));
								varRef = popArray + "[" + num + "]." + varInst + "." + var;
							} else {
								Cell cell = compIdsVsCells.get(varParts[0]);
								String varInst = getNrnSectionName(cell.getMorphology().getSegment().get(0));

								if (varParts.length == 5) {
									String cellId = varParts[0];
									// String biophys = varParts[1];
									// String membProps = varParts[2];
									String channelDensId = varParts[3];
									String variable = varParts[4];
									Component cellComp = lems.getComponent(cellId);
									ArrayList<Component> channelDensityComps = cellComp.getChild("biophysicalProperties").getChild("membraneProperties").getChildrenAL("channelDensities");

									if (variable.equals("gDensity")) {
										for (Component c : channelDensityComps) {
											if (c.getID().equals(channelDensId))
												variable = "gion_" + c.getStringValue("ionChannel");
										}
									}

									varInst += "." + variable;

									varRef = popArray + "[" + num + "]." + varInst;
								} else if (varParts.length > 5) {
									// String cellRef = varParts[0];
									// String biophys = varParts[1];
									// String membProps = varParts[2];
									// String channelDensId = varParts[3];
									String ionChanId = varParts[4];

									for (int i = 5; i < varParts.length; i++) {
										if (i == 5)
											varInst += ".";
										else
											varInst += "_";
										varInst += varParts[i];
									}

									varInst += "_" + ionChanId;

									varRef = popArray + "[" + num + "]." + varInst;
								} else {
									for (int i = 1; i < varParts.length; i++) {
										String part = varParts[i];
										if (part.equals("caConc")) {
											part = "cai";
										}
										varInst += "." + part;
									}
									varRef = popArray + "[" + num + "]." + varInst;

								}
							}

						}

						else {
							String mechRef = compMechNamesHoc.get(pop + "[i]");

							varRef = mechRef.replaceAll("\\[i\\]", "[" + num + "]") + "." + var;
							System.out.println("Saving " + var + "-> " + varRef + " on cell " + num + " in " + pop + " of type " + popComp + " (was: " + origRef + "), " + compMechNamesHoc);
						}

						// String dim = "None";
						// try {
						// Exposure exp =
						// popComp.getComponentType().getExposure(var);
						// dim = exp.getDimension().getName();
						// } catch(ContentError e) {
						//
						// }

						// TODO check if scale is supported in OutputColumn
						// //float scale = 1 / convertToNeuronUnits((float)
						// lineComp.getParamValue("scale").getDoubleValue(),
						// dim);
						// float scale = 1;

						if (columnsPre.get(outfileId) == null)
							columnsPre.put(outfileId, new ArrayList<String>());
						if (columnsPost.get(outfileId) == null)
							columnsPost.put(outfileId, new ArrayList<String>());

						columnsPre.get(outfileId).add("# Column: " + ref);
						columnsPre.get(outfileId).add(
								"h(' objectvar v_" + colId + " ')");
						columnsPre.get(outfileId).add(
								"h(' { v_" + colId + " = new Vector() } ')");
						columnsPre.get(outfileId).add(
								"h(' v_" + colId + ".record(&" + varRef
								+ ") ')");
						columnsPre.get(outfileId).add("h.v_" + colId + ".resize((h.tstop * h.steps_per_ms) + 1)");

						columnsPost.get(outfileId).add("    h.f_" + outfileId + ".printf(\"%f\\t\", h.v_" + colId + ".get(i))");

					}
				}
			}
		}

		for (String f : outfiles.keySet()) {
			addComment(main, "File to save: " + f);
			// main.append(f + " = ???\n");

			for (String col : columnsPre.get(f)) {
				main.append(col + "\n");
			}
			main.append("\n");
		}

		main.append("\n\n");

		main.append("h.nrncontrolmenu()\n");

		main.append("h.run()\n\n");

		// main.append("objref SampleGraph\n");
		for (String dg : displayGraphs) {
			main.append(dg + ".exec_menu(\"View = plot\")\n");
		}
		main.append("\n");
		/*
		 * for i=0, 0 { f_CG1_seg_Soma_v[i] = new File() strdef filename
		 * {sprint(filename, "%sCG1_%d.dat", targetDir, i)}
		 * f_CG1_seg_Soma_v[i].wopen(filename)
		 * v_CG1_seg_Soma_v[i].printf(f_CG1_seg_Soma_v[i])
		 * f_CG1_seg_Soma_v[i].close()
		 */

		for (String f : outfiles.keySet()) {
			addComment(main, "File to save: " + f);
			main.append("h(' objectvar f_" + f + " ')\n");
			main.append("h(' { f_" + f + " = new File() } ')\n");
			main.append("h.f_" + f + ".wopen(\"" + outfiles.get(f) + "\")\n");
			main.append("for i in range(int(h.tstop * h.steps_per_ms) + 1):\n");
			for (String col : columnsPost.get(f)) {
				main.append(col + "\n");
			}
			main.append("    h.f_" + f + ".printf(\"\\n\")\n");
			main.append("h.f_" + f + ".close()\n");
			main.append("print(\"Saved data to: " + outfiles.get(f) + "\")\n");

			main.append("\n");
		}

		main.append("print \"Done\"\n\n");

		return main.toString();
	}

	public static Cell getCellFromComponent(Component comp)
			throws ContentError, ParseError, IOException, JAXBException {
		LinkedHashMap<String, Standalone> els = Utils.convertLemsComponentToNeuroML(comp);
		Cell cell = (Cell) els.values().iterator().next();
		return cell;
	}

	public static String generateCellFile(Cell cell) throws ContentError, ParseError, IOException, JAXBException, NeuroMLException {
		StringBuilder cellString = new StringBuilder();

		cellString.append("// Cell: " + cell.getId() + "\n");
		String json = JSONCellSerializer.cellToJson(cell, SupportedUnits.NEURON);
		cellString.append("/*\n" + json + "\n*/");

		Velocity.init();

		VelocityContext context = new VelocityContext();

		DLemsWriter.putIntoVelocityContext(json, context);

		Properties props = new Properties();
		props.put("resource.loader", "class");
		props.put("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
		VelocityEngine ve = new VelocityEngine();
		ve.init(props);
		Template template = ve.getTemplate(NRNConst.cellTemplateFile);

		StringWriter sw1 = new StringWriter();

		template.merge(context, sw1);

		cellString.append(sw1.toString());

		return cellString.toString();
	}

	// TODO: to be made more general
	public enum SupportedUnits {
		SI(1, 1, 1, 1, 1, 1, 1),
		PHYSIOLOGICAL(1000, 1e-6f, 100, 0.1f, 0.1f, 1e-6f, 1000),
		NEURON(1000, 1e-6f, 100, 100, 1e-4f, 1, 1000);

		SupportedUnits(float voltageFactor, float lengthFactor,
				float specCapFactor, float resistivityFactor,
				float condDensFactor, float concentrationFactor,
				float permeabilityFactor) {
			this.voltageFactor = voltageFactor;
			this.lengthFactor = lengthFactor;
			this.specCapFactor = specCapFactor;
			this.resistivityFactor = resistivityFactor;
			this.condDensFactor = condDensFactor;
			this.concentrationFactor = concentrationFactor;
			this.permeabilityFactor = permeabilityFactor;
		}

		public float voltageFactor;
		@SuppressWarnings("unused")
		private float lengthFactor;
		public float specCapFactor;
		public float resistivityFactor;
		public float condDensFactor;
		public float concentrationFactor;
		public float permeabilityFactor;
	};

	public static String getNrnSectionName(Segment seg) {

		return (seg.getName() != null && seg.getName().length() > 0) ? "Section_" + seg.getName() : "Section_" + seg.getId();
	}

	public static String formatDefault(float num) {
		// final DecimalFormat formatter = new DecimalFormat("#.#");
		return num + "";// +formatter.format(num);
	}

	public static String generateModFile(Component comp) throws ContentError {
		return generateModFile(comp, null);
	}

	public static String generateModFile(Component comp, ChannelConductanceOption condOption) throws ContentError {
		StringBuilder mod = new StringBuilder();

		String mechName = comp.getComponentType().getName();

		mod.append("TITLE Mod file for component: " + comp + "\n\n");

		mod.append("COMMENT\n\n" + Utils.getHeaderComment(NRNConst.NEURON_FORMAT) + "\n\nENDCOMMENT\n\n");

		StringBuilder blockNeuron = new StringBuilder();
		StringBuilder blockUnits = new StringBuilder();
		StringBuilder blockParameter = new StringBuilder();
		StringBuilder blockAssigned = new StringBuilder();
		StringBuilder blockState = new StringBuilder();
		StringBuilder blockInitial = new StringBuilder();
		StringBuilder blockInitial_v = new StringBuilder();
		StringBuilder blockBreakpoint = new StringBuilder();
		StringBuilder blockBreakpoint_regimes = new StringBuilder();
		StringBuilder blockDerivative = new StringBuilder();
		StringBuilder blockNetReceive = new StringBuilder();
		StringBuilder blockFunctions = new StringBuilder();

		String blockNetReceiveParams;
		StringBuilder ratesMethod = new StringBuilder("\n");

		HashMap<String, HashMap<String, String>> paramMappings = new HashMap<String, HashMap<String, String>>();

		if (comp.getComponentType().isOrExtends(NeuroMLElements.BASE_CELL_COMP_TYPE)) {
			HashMap<String, String> paramMappingsComp = new HashMap<String, String>();
			// /paramMappingsComp.put(NEURON_VOLTAGE,
			// getStateVarName(NEURON_VOLTAGE));
			paramMappings.put(comp.getUniqueID(), paramMappingsComp);
		}

		blockUnits.append(NRNConst.generalUnits);

		ArrayList<String> locals = new ArrayList<String>();

		boolean hasCaDependency = false;

		if (comp.getComponentType().isOrExtends(NeuroMLElements.ION_CHANNEL_COMP_TYPE)) {
			mechName = comp.getID();
			blockNeuron.append("SUFFIX " + mechName + "\n");

			String species = comp.getTextParam("species");

			if (species == null || species.equals("non_specific")) {
				blockNeuron.append("NONSPECIFIC_CURRENT i\n");
				blockNeuron.append("RANGE e\n");
			} else {
				String readRevPot = "";

				if (condOption == null || (condOption.equals(ChannelConductanceOption.USE_NERNST))) {

					readRevPot = "READ e" + species;

				} else if (condOption.equals(ChannelConductanceOption.FIXED_REVERSAL_POTENTIAL)) {

					blockInitial.append("e" + species + " = " + condOption.erev + "\n\n");

				} else if (condOption.equals(ChannelConductanceOption.USE_GHK)) {

					blockFunctions.append(NRNConst.ghkFunctionDefs);
					blockUnits.append(NRNConst.ghkUnits);

				}

				if (species.indexOf("ca") >= 0) {

					blockNeuron.append("USEION " + species + " " + readRevPot + " WRITE i" + species + " VALENCE 2 ? Assuming valence = 2 (Ca ion); TODO check this!!\n");

				} else {

					blockNeuron.append("USEION " + species + " " + readRevPot + " WRITE i" + species + " VALENCE 1 ? Assuming valence = 1; TODO check this!!\n");

				}
			}

			for (Component child1 : comp.getAllChildren()) {
				if (child1.getComponentType().isOrExtends(NeuroMLElements.BASE_GATE_COMP_TYPE)) {
					// blockNeuron.append("? Checking " + child1 + "\n");
					for (Component child2 : child1.getAllChildren()) {
						// blockNeuron.append("? Checking " + child2 + "\n");
						if (child2.getComponentType().isOrExtends(NeuroMLElements.BASE_CONC_DEP_VAR_COMP_TYPE)
								|| child2.getComponentType().isOrExtends(NeuroMLElements.BASE_CONC_DEP_RATE_COMP_TYPE)) {
							hasCaDependency = true;

						}

					}
				}
			}
			if (hasCaDependency) {
				blockNeuron.append("USEION ca READ cai,cao VALENCE 2\n"); // TODO check valence
			}

			blockNeuron.append("\nRANGE gion                           ");

			if (condOption == null || condOption.equals(ChannelConductanceOption.USE_NERNST)) {

				blockNeuron.append("\nRANGE gmax                              : Will be changed when ion channel mechanism placed on cell!\n");
				blockParameter.append("\ngmax = 0  (S/cm2)                       : Will be changed when ion channel mechanism placed on cell!\n");

			} else if (condOption.equals(ChannelConductanceOption.USE_GHK)) {
				blockNeuron.append("\nRANGE permeability                      : Will be changed when ion channel mechanism placed on cell!\n");
				blockParameter.append("\npermeability = 0  (um/ms)                       : Will be changed when ion channel mechanism placed on cell!\n");

				blockNeuron.append("RANGE cai\n");
				blockNeuron.append("RANGE cao\n");

				blockAssigned.append("cai (mM)\n");
				blockAssigned.append("cao (mM)\n");

			}

			blockAssigned.append("\ngion   (S/cm2)                          : Transient conductance density of the channel");

		} else if (comp.getComponentType().isOrExtends(NeuroMLElements.CONC_MODEL_COMP_TYPE)) {
			mechName = comp.getID();
			blockNeuron.append("SUFFIX " + mechName + "\n");

			String ion = comp.getStringValue("ion");

			if (ion != null) {
				blockNeuron.append("USEION " + ion + " READ " + ion + "i, " + ion + "o, i" + ion + " WRITE " + ion + "i VALENCE 2\n"); // TODO check valence
			}

			blockNeuron.append("RANGE cai\n");
			blockNeuron.append("RANGE cao\n");

			blockAssigned.append("cai (mM)\n");
			blockAssigned.append("cao (mM)\n");

			blockAssigned.append("ica               (mA/cm2)\n");

			blockAssigned.append("diam (um)\n");

			blockAssigned.append("area (um2)\n");

			blockParameter.append(NeuroMLElements.CONC_MODEL_SURF_AREA + " (um2)\n");
			blockParameter.append(NeuroMLElements.CONC_MODEL_CA_TOT_CURR + " (nA)\n");

			ratesMethod.append(NeuroMLElements.CONC_MODEL_SURF_AREA + " = area\n\n");
			ratesMethod.append(NeuroMLElements.CONC_MODEL_CA_TOT_CURR + " = -1 * (0.01) * ica * " + NeuroMLElements.CONC_MODEL_SURF_AREA + " : To correct units...\n\n");

			// locals.add(NeuroMLElements.CONC_MODEL_SURF_AREA);
			// locals.add(NeuroMLElements.CONC_MODEL_CA_CURR_DENS);

			blockNeuron.append("GLOBAL " + NeuroMLElements.CONC_MODEL_INIT_CONC + "\n");
			blockNeuron.append("GLOBAL " + NeuroMLElements.CONC_MODEL_INIT_EXT_CONC + "\n");
			blockParameter.append(NeuroMLElements.CONC_MODEL_INIT_CONC + " (mM)\n");
			blockParameter.append(NeuroMLElements.CONC_MODEL_INIT_EXT_CONC + " (mM)\n");

			blockInitial.append(NeuroMLElements.CONC_MODEL_INIT_CONC + " = cai" + "\n");
			blockInitial.append(NeuroMLElements.CONC_MODEL_INIT_EXT_CONC + " = cao" + "\n");

		} else if (comp.getComponentType().isOrExtends(NeuroMLElements.BASE_POINT_CURR_COMP_TYPE)) {
			mechName = comp.getID();
			blockNeuron.append("POINT_PROCESS " + mechName + "\n");
			if (!comp.getComponentType().isOrExtends(NeuroMLElements.BASE_SYNAPSE_COMP_TYPE)) {
				blockNeuron.append("ELECTRODE_CURRENT i\n");
			}
		} else {
			blockNeuron.append("POINT_PROCESS " + mechName + "\n");
		}

		if (comp.getComponentType().isOrExtends(NeuroMLElements.BASE_SYNAPSE_COMP_TYPE)) {
			blockNetReceiveParams = "weight (uS)";
			blockAssigned.append("? Standard Assigned variables with baseSynapse\n");
			blockAssigned.append("v (mV)\n");
			blockAssigned.append(NRNConst.NEURON_TEMP + " (degC)\n");
			blockAssigned.append(NeuroMLElements.TEMPERATURE + " (K)\n");
		} else {
			blockNetReceiveParams = "flag";
		}

		String prefix = "";

		ArrayList<String> rangeVars = new ArrayList<String>();
		ArrayList<String> stateVars = new ArrayList<String>();

		parseStateVars(comp, prefix, rangeVars, stateVars, blockNeuron,
				blockParameter, blockAssigned, blockState, paramMappings);

		parseParameters(comp, prefix, prefix, rangeVars, stateVars,
				blockNeuron, blockParameter, paramMappings);

		if (comp.getComponentType().isOrExtends(NeuroMLElements.ION_CHANNEL_COMP_TYPE)) {
			blockAssigned.append("? Standard Assigned variables with ionChannel\n");
			blockAssigned.append("v (mV)\n");
			blockAssigned.append(NRNConst.NEURON_TEMP + " (degC)\n");
			blockAssigned.append(NeuroMLElements.TEMPERATURE + " (K)\n");

			String species = comp.getTextParam("species");

			if (species == null || species.equals("non_specific")) {
				blockAssigned.append("e (mV)\n");
				blockAssigned.append("i (mA/cm2)\n");
			} else if (species != null) {
				blockAssigned.append("e" + species + " (mV)\n");
				blockAssigned.append("i" + species + " (mA/cm2)\n");
			}
			blockAssigned.append("\n");
			if (hasCaDependency) {
				blockAssigned.append("cai (mM)\n\n");

				locals.add("caConc");
				ratesMethod.append("caConc = cai\n\n");

			}
		}

		// ratesMethod.append("? - \n");

		parseDerivedVars(comp, prefix, rangeVars, ratesMethod, blockNeuron,
				blockParameter, blockAssigned, blockBreakpoint, paramMappings);

		// ratesMethod.append("? + \n");

		if (comp.getComponentType().isOrExtends(NeuroMLElements.ION_CHANNEL_COMP_TYPE)) {

			if (condOption == null || condOption.equals(ChannelConductanceOption.USE_NERNST)) {
				blockBreakpoint.append("gion = gmax * fopen \n\n");
			} else if (condOption.equals(ChannelConductanceOption.USE_GHK)) {
				blockBreakpoint.append("gion = permeability * fopen \n");
			}
			String species = comp.getTextParam("species");

			// Only for ohmic!!
			if (species == null || species.equals("non_specific")) {
				blockBreakpoint.append("i = gion * (v - e)\n");
			} else {
				if (condOption == null || condOption.equals(ChannelConductanceOption.USE_NERNST)) {
					blockBreakpoint.append("i" + species + " = gion * (v - e" + species + ")\n");
				} else if (condOption.equals(ChannelConductanceOption.USE_GHK))  {
					blockBreakpoint.append("i" + species + " = gion * ghk(v, cai, cao)\n");
				}
			}
		} else if (comp.getComponentType().isOrExtends(NeuroMLElements.BASE_SYNAPSE_COMP_TYPE)) {
			// ratesMethod.append("i = -1 * i ? Due to different convention in synapses\n");

		}

		parseTimeDerivs(comp, prefix, locals, blockDerivative, blockBreakpoint,
				blockAssigned, ratesMethod, paramMappings);

		if (blockDerivative.length() > 0) {
			blockBreakpoint.insert(0, "SOLVE states METHOD cnexp\n\n");
		}

		ArrayList<String> regimeNames = new ArrayList<String>();
		HashMap<String, Integer> flagsVsRegimes = new HashMap<String, Integer>();

		if (comp.getComponentType().hasDynamics()) {

			int regimeFlag = 5000;
			for (Regime regime : comp.getComponentType().getDynamics().getRegimes()) {
				flagsVsRegimes.put(regime.name, regimeFlag); // fill
				regimeFlag++;
			}

			// //String elsePrefix = "";
			for (Regime regime : comp.getComponentType().getDynamics().getRegimes()) {
				String regimeStateName = NRNConst.REGIME_PREFIX + regime.name;
				regimeNames.add(regimeStateName);

				// StringBuilder test = new
						// StringBuilder(": Testing for "+regimeStateName+ "\n");
				// test.append(elsePrefix+"if ("+regimeStateName+" == 1 ) {\n");
				// elsePrefix = "else ";

				// blockNetReceive.append("if ("+regimeStateName+" == 1 && flag == "+regimeFlag+") { "
				// +
				// ": Setting watch for OnCondition in "+regimeStateName+"...\n");
				// ////////blockNetReceive.append("    WATCH (" +
				// checkForStateVarsAndNested(cond, comp, paramMappings) +
				// ") "+conditionFlag+"\n");

				for (OnCondition oc : regime.getOnConditions()) {

					String cond = checkForBinaryOperators(oc.test);

					blockNetReceive.append("\nif (flag == 1) { : Setting watch condition for " + regimeStateName + "\n");
					blockNetReceive.append("    WATCH (" + checkForStateVarsAndNested(cond, comp, paramMappings) + ") " + regimeFlag + "\n");
					blockNetReceive.append("}\n\n");

					// test.append("    if (" + checkForStateVarsAndNested(cond,
					// comp, paramMappings) + ") {\n");

					blockNetReceive.append("if (" + regimeStateName + " == 1 && flag == " + regimeFlag + ") { : Setting actions for " + regimeStateName + "\n");

					if (debug) blockNetReceive.append("\n        printf(\"+++++++ Start condition (" + oc.test + ") for " + regimeStateName + " at time: %g, v: %g\\n\", t, v)\n");

					blockNetReceive.append("\n        : State assignments\n");
					for (StateAssignment sa : oc.getStateAssignments()) {
						blockNetReceive.append("\n        " + getStateVarName(sa.getStateVariable().getName()) + " = "
								+ checkForStateVarsAndNested(sa.getValueExpression(), comp, paramMappings) + "\n");
					}
					for (Transition trans : oc.getTransitions()) {
						blockNetReceive.append("\n        : Change regime flags\n");
						blockNetReceive.append("        " + regimeStateName + " = 0\n");
						blockNetReceive.append("        " + NRNConst.REGIME_PREFIX + trans.regime + " = 1\n");
						// int flagTarget =
						// flagsVsRegimes.get(trans.getRegime());
						// test.append("    net_send(0,"+flagTarget+") : Sending to regime_"+trans.getRegime()+"\n");

						Regime targetRegime = comp.getComponentType().getDynamics().getRegimes().getByName(trans.regime);
						if (targetRegime != null) {
							blockNetReceive.append("\n        : OnEntry to " + targetRegime + "\n");
							for (OnEntry oe : targetRegime.getOnEntrys()) {
								for (StateAssignment sa : oe.getStateAssignments()) {
									blockNetReceive.append("\n        " + sa.getStateVariable().getName() + " = " 
											+ checkForStateVarsAndNested(sa.getValueExpression(), comp, paramMappings) + "\n");
								}
							}
						}
					}

					if (debug) blockNetReceive.append("\n        printf(\"+++++++ End condition (" + oc.test + ") for " + regimeStateName + " at time: %g, v: %g\\n\", t, v)\n");

					blockNetReceive.append("}\n");

				}
				// blockNetReceive.append("}\n");
				// blockBreakpoint_regimes.insert(0, test.toString()+ "\n");
				// blockBreakpoint_regimes.append(blockNetReceive.toString()+
				// "\n");
				regimeFlag++;
			}
		}

		String localsLine = "";
		if (!locals.isEmpty()) {
			localsLine = "LOCAL ";
		}
		for (String local : locals) {
			localsLine = localsLine + local;
			if (!locals.get(locals.size() - 1).equals(local)) {
				localsLine = localsLine + ", ";
			} else {
				localsLine = localsLine + "\n";
			}

		}

		ratesMethod.insert(0, localsLine);
		// blockDerivative.insert(0, localsLine);

		blockInitial.append("rates()\n");

		if (comp.getComponentType().isOrExtends(NeuroMLElements.ION_CHANNEL_COMP_TYPE)
				|| comp.getComponentType().isOrExtends(NeuroMLElements.BASE_SYNAPSE_COMP_TYPE)) {
			blockInitial.append("\n" + NeuroMLElements.TEMPERATURE + " = " + NRNConst.NEURON_TEMP + " + 273.15\n");
		}

		parseOnStart(comp, prefix, blockInitial, blockInitial_v, paramMappings);

		/*
		 * for (OnStart os :
		 * comp.getComponentClass().getDynamics().getOnStarts()) { for
		 * (StateAssignment sa : os.getStateAssignments()) {
		 * blockInitial.append("\n" +
		 * getStateVarName(sa.getStateVariable().getName()) + " = " +
		 * sa.getEvaluable() + "\n"); } }
		 */

		int conditionFlag = 1000;
		Dynamics dyn = comp.getComponentType().getDynamics();
		if (dyn != null) {

			for (OnCondition oc : dyn.getOnConditions()) {

				String cond = checkForBinaryOperators(oc.test);

				boolean resetVoltage = false;
				for (StateAssignment sa : oc.getStateAssignments()) {
					resetVoltage = resetVoltage || sa.getStateVariable().getName().equals(NRNConst.NEURON_VOLTAGE);
				}

				if (!resetVoltage) {
					blockBreakpoint.append("if (" + checkForStateVarsAndNested(cond, comp, paramMappings) + ") {");
					for (StateAssignment sa : oc.getStateAssignments()) {
						blockBreakpoint.append("\n    " + getStateVarName(sa.getStateVariable().getName()) + " = " + checkForStateVarsAndNested(sa.getValueExpression(), comp, paramMappings) + " ??\n"); 
					}
					blockBreakpoint.append("}\n\n");
				} else {

					blockNetReceive.append("\nif (flag == 1) { : Setting watch for top level OnCondition...\n");
					blockNetReceive.append("    WATCH (" + checkForStateVarsAndNested(cond, comp, paramMappings) + ") " + conditionFlag + "\n");

					blockNetReceive.append("}\n");
					blockNetReceive.append("if (flag == " + conditionFlag + ") {\n");
					if (debug) blockNetReceive.append("    printf(\"Condition (" + checkForStateVarsAndNested(cond, comp, paramMappings) + "), " + conditionFlag
							+ ", satisfied at time: %g, v: %g\\n\", t, v)\n");
					for (StateAssignment sa : oc.getStateAssignments()) {
						blockNetReceive.append("\n    " + sa.getStateVariable().getName() + " = " + checkForStateVarsAndNested(sa.getValueExpression(), comp, paramMappings) + "\n");
					}
					blockNetReceive.append("}\n");

				}
				conditionFlag++;
			}
		}

		parseOnEvent(comp, blockNetReceive, paramMappings);

		if (comp.getComponentType().isOrExtends(NeuroMLElements.CONC_MODEL_COMP_TYPE)
				&& comp.getComponentType().getDynamics().getTimeDerivatives().isEmpty()) {
			blockBreakpoint.append("\ncai = " + NeuroMLElements.CONC_MODEL_CONC_STATE_VAR + "\n\n");
		}

		//              if (comp.getComponentType().hasDynamics()) {
		//                      for (Regime regime : comp.getComponentType().getDynamics()
		//                                      .getRegimes()) {
		//                              String regimeStateName = "regime_" + regime.name;
		//                              blockNetReceive.append(": Conditions for " + regimeStateName);
		//                              int regimeFlag = flagsVsRegimes.get(regime.name);
		//                              blockNetReceive.append("if (flag == " + regimeFlag
		//                                              + ") { : Entry into " + regimeStateName + "\n");
		//                              for (String r : regimeNames) {
		//                                      blockNetReceive.append("    " + r + " = "
		//                                                      + (r.equals(regimeStateName) ? 1 : 0) + "\n");
		//                              }
		//                              for (OnEntry oe : regime.getOnEntrys()) {
		//
		//                                      for (StateAssignment sa : oe.getStateAssignments()) {
		//                                              blockNetReceive.append("\n    "
		//                                                              + getStateVarName(sa.getStateVariable()
		//                                                                              .getName())
		//                                                              + " = "
		//                                                              + checkForStateVarsAndNested(sa.getEvaluable()
		//                                                                              .toString(), comp, paramMappings)
		//                                                              + "\n");
		//                                      }
		//                              }
		//                              blockNetReceive.append("}\n");
		//                      }
		//              }

		if (blockInitial_v.toString().trim().length() > 0) {
			blockNetReceive.append("if (flag == 1) { : Set initial states\n");
			blockNetReceive.append(blockInitial_v.toString());
			blockNetReceive.append("}\n");
		}

		if (dyn != null) {
			for (StateVariable sv : dyn.getStateVariables()) {

				if (sv.getName().equals(NRNConst.NEURON_VOLTAGE)) {
					// blockBreakpoint.append("\ni = " + HIGH_CONDUCTANCE_PARAM
					// + "*(v-" + getStateVarName(NEURON_VOLTAGE) +
					// ") ? To ensure v of section rapidly follows " +
					// getStateVarName(sv.getName()));

					blockBreakpoint.append("\n" + NRNConst.V_COPY_PREFIX
							+ NRNConst.NEURON_VOLTAGE + " = "
							+ NRNConst.NEURON_VOLTAGE);

					if (comp.getComponentType().isOrExtends(NeuroMLElements.BASE_CELL_CAP_COMP_TYPE)) {
						// blockBreakpoint.append("\ni = -1 * " +
						// ABSTRACT_CELL_COMP_TYPE_CAP__I_MEMB + "");
						blockBreakpoint.append("\ni = " + getStateVarName(NRNConst.NEURON_VOLTAGE) + " * C");
					} else {
						blockBreakpoint.append("\ni = " + getStateVarName(NRNConst.NEURON_VOLTAGE) + "");
					}
				}
			}
		}

		writeModBlock(mod, "NEURON", blockNeuron.toString());

		writeModBlock(mod, "UNITS", blockUnits.toString());

		writeModBlock(mod, "PARAMETER", blockParameter.toString());

		if (blockAssigned.length() > 0) {
			writeModBlock(mod, "ASSIGNED", blockAssigned.toString());
		}

		writeModBlock(mod, "STATE", blockState.toString());
		writeModBlock(mod, "INITIAL", blockInitial.toString());

		if (blockDerivative.length() == 0) {
			blockBreakpoint.insert(0, "rates()\n");
		}

		if (dyn != null) {
			for (Regime regime : dyn.getRegimes()) {
				String reg = "regime_" + regime.getName();
				if (debug) blockBreakpoint.insert(0, "printf(\"     " + reg + ": %g\\n\", " + reg + ")\n");
			}
		}
		if (debug) blockBreakpoint.insert(0, "printf(\"+++++++ Entering BREAKPOINT in " + comp.getName() + " at time: %g, v: %g\\n\", t, v)\n");

		writeModBlock(mod, "BREAKPOINT", blockBreakpoint_regimes.toString() + "\n" + blockBreakpoint.toString());

		if (blockNetReceive.length() > 0) {
			writeModBlock(mod, "NET_RECEIVE(" + blockNetReceiveParams + ")", blockNetReceive.toString());
		}

		if (blockDerivative.length() > 0) {
			blockDerivative.insert(0, "rates()\n");
			writeModBlock(mod, "DERIVATIVE states", blockDerivative.toString());
		}

		writeModBlock(mod, "PROCEDURE rates()", ratesMethod.toString());

		// for (String compK : paramMappings.keySet()) {
		// E.info("  Maps for "+compK);
		// //for (String orig : paramMappings.get(compK).keySet()) {
		// E.info("      "+orig+" -> "+paramMappings.get(compK).get(orig));
		// //}
		// }
		// System.out.println("----  paramMappings: "+paramMappings);

		if (blockFunctions.length() > 0) {
			mod.append(blockFunctions.toString());
		}

		return mod.toString();
	}

	private static void parseOnStart(Component comp, String prefix,
			StringBuilder blockInitial, StringBuilder blockInitial_v,
			HashMap<String, HashMap<String, String>> paramMappings)
					throws ContentError {

		HashMap<String, String> paramMappingsComp = paramMappings.get(comp.getUniqueID());

		if (comp.getComponentType().hasDynamics()) {

			for (Regime regime : comp.getComponentType().getDynamics().getRegimes()) {
				String regimeStateName = NRNConst.REGIME_PREFIX + regime.name;
				if (regime.initial != null && regime.initial.equals("true")) {
					blockInitial.append("\n" + regimeStateName + " = 1\n");
				} else {
					blockInitial.append("\n" + regimeStateName + " = 0\n");
				}

			}

			for (OnStart os : comp.getComponentType().getDynamics().getOnStarts()) {
				for (StateAssignment sa : os.getStateAssignments()) {
					String var = getStateVarName(sa.getStateVariable().getName());

					if (paramMappingsComp.containsKey(var)) {
						var = paramMappingsComp.get(var);
					}

					if (sa.getStateVariable().getName().equals(NRNConst.NEURON_VOLTAGE)) {
						var = sa.getStateVariable().getName();

						blockInitial.append("\nnet_send(0, 1) : go to NET_RECEIVE block, flag 1, for initial state\n");
								blockInitial_v.append("\n    " + var + " = " + checkForStateVarsAndNested(sa.getValueExpression(), comp, paramMappings) + "\n");

					} else {

						blockInitial.append("\n" + var + " = " + checkForStateVarsAndNested(sa.getValueExpression(), comp, paramMappings) + "\n");
					}
				}
			}
		}
		for (Component childComp : comp.getAllChildren()) {
			String prefixNew = prefix + childComp.getID() + "_";
			if (childComp.getID() == null) {
				prefixNew = prefix + childComp.getName() + "_";
			}
			parseOnStart(childComp, prefixNew, blockInitial, blockInitial_v, paramMappings);
		}

	}

	private static void parseOnEvent(Component comp,
			StringBuilder blockNetReceive,
			HashMap<String, HashMap<String, String>> paramMappings)
					throws ContentError {
		// Add appropriate state discontinuities for synaptic events in
		// NET_RECEIVE block. Do this for all child elements as well, in case
		// the spike event is meant to be relayed down from the parent synaptic
		// mechanism to a child plasticityMechanism.
		if (comp.getComponentType().getDynamics() != null) {
			for (OnEvent oe : comp.getComponentType().getDynamics().getOnEvents()) {
				if (oe.getPortName().equals(NeuroMLElements.SYNAPSE_PORT_IN)) {
					for (StateAssignment sa : oe.getStateAssignments()) {
						blockNetReceive.append("state_discontinuity(" + checkForStateVarsAndNested(sa.getStateVariable().getName(), comp, paramMappings) + ", " + checkForStateVarsAndNested(sa.getValueExpression(), comp, paramMappings) + ")\n"); }
				}
			}
		}
		for (Component childComp : comp.getAllChildren()) {
			if (childComp.getComponentType().isOrExtends(
					NeuroMLElements.BASE_PLASTICITY_MECHANISM_COMP_TYPE)) {
				parseOnEvent(childComp, blockNetReceive, paramMappings);
			}
		}
	}

	private static void parseParameters(Component comp,
			String prefix,
			String prefixParent,
			ArrayList<String> rangeVars,
			ArrayList<String> stateVars,
			StringBuilder blockNeuron,
			StringBuilder blockParameter,
			HashMap<String, HashMap<String, String>> paramMappings) {

		HashMap<String, String> paramMappingsComp = paramMappings.get(comp
				.getUniqueID());

		if (paramMappingsComp == null) {
			paramMappingsComp = new HashMap<String, String>();
			paramMappings.put(comp.getUniqueID(), paramMappingsComp);
		}

		for (ParamValue pv : comp.getParamValues()) {
			String mappedName = prefix + pv.getName();
			rangeVars.add(mappedName);
			paramMappingsComp.put(pv.getName(), mappedName);

			String range = "RANGE " + mappedName;
			while (range.length() < NRNConst.commentOffset) {
				range = range + " ";
			}

			blockNeuron.append(range + ": parameter\n");
			float val = convertToNeuronUnits((float) pv.getDoubleValue(), pv.getDimensionName());
			String valS = val + "";
			if ((int) val == val) {
				valS = (int) val + "";
			}
			blockParameter.append("\n" + mappedName + " = " + valS + " " + getNeuronUnit(pv.getDimensionName()));
		}

		for (Exposure exp : comp.getComponentType().getExposures()) {
			String mappedName = prefix + exp.getName();

			if (!rangeVars.contains(mappedName)
					&& !stateVars.contains(mappedName)
					&& !exp.getName().equals(NRNConst.NEURON_VOLTAGE)) {
				rangeVars.add(mappedName);
				paramMappingsComp.put(exp.getName(), mappedName);

				String range = "\nRANGE " + mappedName;
				while (range.length() < NRNConst.commentOffset) {
					range = range + " ";
				}
				blockNeuron.append(range + " : exposure\n");

				if (comp.getComponentType().isOrExtends(NeuroMLElements.BASE_POINT_CURR_COMP_TYPE)
						&& exp.getName().equals(NeuroMLElements.POINT_CURR_CURRENT)) {
					blockNeuron.append("\n\nNONSPECIFIC_CURRENT " + NeuroMLElements.POINT_CURR_CURRENT + " \n");
				}
			}
		}

		for (Requirement req : comp.getComponentType().getRequirements()) {
			String mappedName = prefixParent + req.getName();
			if (!req.getName().equals("v")
					&& !req.getName().equals("temperature")
					&& !req.getName().equals("caConc")) {
				// Make the assumption that the requirement is in the parent...
				paramMappingsComp.put(req.getName(), mappedName);
			}

		}

		for (Component childComp : comp.getAllChildren()) {
			String prefixNew = prefix + childComp.getID() + "_";
			if (childComp.getID() == null) {
				prefixNew = prefix + childComp.getName() + "_";
			}
			parseParameters(childComp, prefixNew, prefix, rangeVars, stateVars,
					blockNeuron, blockParameter, paramMappings);


			//                      HashMap<String, String> childMaps = paramMappings.get(childComp.getID());
			//                      for (String mapped : childMaps.keySet()) {
			//                              if (!paramMappingsComp.containsKey(mapped)) {
			//                                      paramMappingsComp.put(mapped, childMaps.get(mapped));
			//                              }
			//                      }

		}

		if (comp.getComponentType().isOrExtends(NeuroMLElements.BASE_CELL_COMP_TYPE)) {
			blockNeuron.append("\nRANGE " + NRNConst.V_COPY_PREFIX + NRNConst.NEURON_VOLTAGE + "                           : copy of v on section\n");
		}

	}

	private static void parseStateVars(Component comp,
			String prefix,
			ArrayList<String> rangeVars,
			ArrayList<String> stateVars,
			StringBuilder blockNeuron,
			StringBuilder blockParameter,
			StringBuilder blockAssigned,
			StringBuilder blockState,
			HashMap<String, HashMap<String, String>> paramMappings)
					throws ContentError {

		HashMap<String, String> paramMappingsComp = paramMappings.get(comp.getUniqueID());

		if (paramMappingsComp == null) {
			paramMappingsComp = new HashMap<String, String>();
			paramMappings.put(comp.getUniqueID(), paramMappingsComp);
		}

		if (comp.getComponentType().hasDynamics()) {

			for (Regime regime : comp.getComponentType().getDynamics().getRegimes()) {
				String regimeStateName = NRNConst.REGIME_PREFIX + regime.name;
				stateVars.add(regimeStateName);
				blockState.append(regimeStateName + " (1)\n");
			}

			for (StateVariable sv : comp.getComponentType().getDynamics().getStateVariables()) {

				String svName = prefix + getStateVarName(sv.getName());
				stateVars.add(svName);
				String dim = getNeuronUnit(sv.getDimension().getName());

				if (!svName.equals(NRNConst.NEURON_VOLTAGE)
						&& !getStateVarName(sv.getName()).equals(getStateVarName(NRNConst.NEURON_VOLTAGE))) {
					paramMappingsComp.put(getStateVarName(sv.getName()), svName);
				}

				if (sv.getName().equals(NRNConst.NEURON_VOLTAGE)) {
					blockNeuron.append("\n\nNONSPECIFIC_CURRENT i                    : To ensure v of section follows " + svName);
					// //blockNeuron.append("\nRANGE " + HIGH_CONDUCTANCE_PARAM
							// +
							// "                  : High conductance for above current");
					// //blockParameter.append("\n\n" + HIGH_CONDUCTANCE_PARAM +
					// " = 1000 (S/cm2)");
					blockAssigned.append("v (mV)\n");
					blockAssigned.append("i (mA/cm2)\n\n");

					blockAssigned.append(NRNConst.V_COPY_PREFIX + NRNConst.NEURON_VOLTAGE + " (mV)\n\n");

					dim = "(nA)";
				}

				blockState.append(svName + " " + dim + "\n");
			}
		}

		for (Component childComp : comp.getAllChildren()) {
			String prefixNew = prefix + childComp.getID() + "_";
			if (childComp.getID() == null) {
				prefixNew = prefix + childComp.getName() + "_";
			}
			parseStateVars(childComp, prefixNew, rangeVars, stateVars,
					blockNeuron, blockParameter, blockAssigned, blockState,
					paramMappings);
		}
	}

	private static void parseTimeDerivs(Component comp,
			String prefix,
			ArrayList<String> locals, 
			StringBuilder blockDerivative,
			StringBuilder blockBreakpoint,
			StringBuilder blockAssigned,
			StringBuilder ratesMethod,
			HashMap<String, HashMap<String, String>> paramMappings)
					throws ContentError {

		StringBuilder ratesMethodFinal = new StringBuilder();

		if (comp.getComponentType().hasDynamics()) {

			HashMap<String, String> rateNameVsRateExpr = new HashMap<String, String>();

			for (TimeDerivative td : comp.getComponentType().getDynamics().getTimeDerivatives()) {

				String rateName = NRNConst.RATE_PREFIX + prefix + td.getStateVariable().getName();
				String rateUnits = getDerivativeUnit(td.getStateVariable().getDimension().getName());

				blockAssigned.append(rateName + " " + rateUnits + "\n");

				// ratesMethod.append(rateName + " = " +
						// checkForStateVarsAndNested(td.getEvaluable().toString(),
				// comp, paramMappings) + " ? \n");
				String rateExpr = checkForStateVarsAndNested(td.getValueExpression(), comp, paramMappings);
				rateNameVsRateExpr.put(rateName, rateExpr);

				if (!td.getStateVariable().getName().equals(NRNConst.NEURON_VOLTAGE)) {

					String stateVarToUse = getStateVarName(td.getStateVariable().getName());

					String line = prefix + stateVarToUse + "' = " + rateName;

					if (comp.getComponentType().isOrExtends(NeuroMLElements.CONC_MODEL_COMP_TYPE)
							&& td.getStateVariable().getName().equals(NeuroMLElements.CONC_MODEL_CONC_STATE_VAR)) {
						line = line + "\ncai = " + td.getStateVariable().getName();
					}

					if (blockDerivative.toString().indexOf(line) < 0)
						blockDerivative.append(line + " \n");

				} else {
					ratesMethodFinal.append(prefix + getStateVarName(td.getStateVariable().getName()) + " = -1 * " + rateName + "\n");
				}

			}

			for (Regime regime : comp.getComponentType().getDynamics().getRegimes()) {

				if (regime.getTimeDerivatives().isEmpty()) {
					// need to hold voltage fixed

					for (OnEntry oe : regime.getOnEntrys()) {
						for (StateAssignment sa : oe.getStateAssignments()) {

							if (sa.getStateVariable().getName().equals(NRNConst.NEURON_VOLTAGE)) {
								String rateName = NRNConst.RATE_PREFIX + prefix + sa.getStateVariable().getName();

								if (!rateNameVsRateExpr.containsKey(rateName)) {
									rateNameVsRateExpr.put(rateName, "0");
								}

								String rateExprPart = rateNameVsRateExpr.get(rateName);

								String rateUnits = getDerivativeUnit(sa.getStateVariable().getDimension().getName());
								if (blockAssigned.indexOf("\n" + rateName + " " + rateUnits + "\n") < 0) {
									blockAssigned.append("\n" + rateName + " " + rateUnits + "\n");
								}
								rateExprPart = rateExprPart + " + " + NRNConst.REGIME_PREFIX + regime.getName() + " * (10000000 * (" + sa.getValueExpression() + " - " + NRNConst.NEURON_VOLTAGE + "))";

								rateNameVsRateExpr.put(rateName, rateExprPart);
							}
						}
					}
				}
				for (TimeDerivative td : regime.getTimeDerivatives()) {
					String rateName = NRNConst.RATE_PREFIX + prefix + td.getStateVariable().getName();
					String rateUnits = getDerivativeUnit(td.getStateVariable().getDimension().getName());
					if (!rateNameVsRateExpr.containsKey(rateName)) {
						rateNameVsRateExpr.put(rateName, "0");
					}

					String rateExprPart = rateNameVsRateExpr.get(rateName);
					if (blockAssigned.indexOf("\n" + rateName + " " + rateUnits + "\n") < 0) {
						blockAssigned.append("\n" + rateName + " " + rateUnits + "\n");
					}
					rateExprPart = rateExprPart + " + " + NRNConst.REGIME_PREFIX + regime.getName() + " * (" + checkForStateVarsAndNested(td.getValueExpression(), comp, paramMappings) + ")";

					rateNameVsRateExpr.put(rateName, rateExprPart);

					if (!td.getStateVariable().getName().equals(NRNConst.NEURON_VOLTAGE)) {
						String line = prefix + getStateVarName(td.getStateVariable().getName()) + "' = " + rateName;

						if (blockDerivative.toString().indexOf(line) < 0)
							blockDerivative.append(line + " \n");

					} else {
						ratesMethodFinal.append(prefix + getStateVarName(td.getStateVariable().getName()) + " = -1 * " + rateName + "\n"); // //
					}
				}
			}

			for (String rateName : rateNameVsRateExpr.keySet()) {
				String rateExpr = rateNameVsRateExpr.get(rateName);
				// ratesMethod.insert(0,rateName + " = " + rateExpr + " \n");
				ratesMethod.append(rateName + " = " + rateExpr + " \n");
			}

			ratesMethod.append("\n" + ratesMethodFinal + " \n");

		}

		for (Component childComp : comp.getAllChildren()) {
			String prefixNew = prefix + childComp.getID() + "_";
			if (childComp.getID() == null) {
				prefixNew = prefix + childComp.getName() + "_";
			}
			parseTimeDerivs(childComp, prefixNew, locals, blockDerivative,
					blockBreakpoint, blockAssigned, ratesMethod, paramMappings);
		}
	}

	private static void parseDerivedVars(Component comp,
			String prefix,
			ArrayList<String> rangeVars,
			StringBuilder ratesMethod,
			StringBuilder blockNeuron,
			StringBuilder blockParameter,
			StringBuilder blockAssigned,
			StringBuilder blockBreakpoint,
			HashMap<String, HashMap<String, String>> paramMappings)
					throws ContentError {

		HashMap<String, String> paramMappingsComp = paramMappings.get(comp.getUniqueID());
		if (paramMappingsComp == null) {
			paramMappingsComp = new HashMap<String, String>();
			paramMappings.put(comp.getUniqueID(), paramMappingsComp);
		}

		for (Component childComp : comp.getAllChildren()) {
			String prefixNew = prefix + childComp.getID() + "_";
			if (childComp.getID() == null) {
				prefixNew = prefix + childComp.getName() + "_";
			}
			parseDerivedVars(childComp, prefixNew, rangeVars, ratesMethod,
					blockNeuron, blockParameter, blockAssigned,
					blockBreakpoint, paramMappings);
		}
		// ratesMethod.append("? Looking at"+comp+"\n");
		if (comp.getComponentType().hasDynamics()) {

			StringBuilder blockForEqns = ratesMethod;
			if (comp.getComponentType().isOrExtends(NeuroMLElements.ION_CHANNEL_COMP_TYPE)) {
				blockForEqns = blockBreakpoint;
			}

			for (DerivedVariable dv : comp.getComponentType().getDynamics().getDerivedVariables()) {

				StringBuilder block = new StringBuilder();

				String mappedName = prefix + dv.getName();
				if (!rangeVars.contains(mappedName)) {
					rangeVars.add(mappedName);

					String range = "RANGE " + mappedName;
					while (range.length() < NRNConst.commentOffset) {
						range = range + " ";
					}

					blockNeuron.append(range + ": derived variable\n");
					paramMappingsComp.put(dv.getName(), mappedName);
				}

				String assig = "\n" + prefix + dv.getName() + " " + getNeuronUnit(dv.dimension);
				while (assig.length() < NRNConst.commentOffset) {
					assig = assig + " ";
				}

				blockAssigned.append(assig + ": derived variable\n");

				if (dv.getValueExpression() != null) {

					String rate = checkForStateVarsAndNested(dv.getValueExpression(), comp, paramMappings);

					String synFactor = "";
					if (comp.getComponentType().isOrExtends(NeuroMLElements.BASE_POINT_CURR_COMP_TYPE)
							&& dv.getName().equals(NeuroMLElements.POINT_CURR_CURRENT)) {
						// since synapse currents differ in sign from NEURON
						synFactor = "-1 * ";
					}

					block.append(prefix + dv.getName() + " = " + synFactor + rate + " ? evaluable\n");

				} else {
					String firstChild = dv.getPath().substring(0, dv.getPath().indexOf("/"));
					if (firstChild.indexOf("[") >= 0) {
						firstChild = firstChild.substring(0, firstChild.indexOf("["));
					}

					Component child = null;

					try {
						child = comp.getChild(firstChild);
					} catch (ContentError ce) {
						E.info("No child of " + firstChild);// do nothing...
					}

					if (child == null) {
						ArrayList<Component> children = comp.getChildrenAL(firstChild);
						if (children.size() > 0) {
							child = children.get(0);
						}
					}

					block.append("? DerivedVariable is based on path: " + dv.getPath() + ", on: " + comp + ", from " + firstChild + "; " + child + "\n");

					if (child == null && dv.getPath().indexOf("synapse") < 0) {
						String alt = "???";
						if (dv.getReduce().equals("multiply")) {
							alt = "1";
						} else if (dv.getReduce().equals("add")) {
							alt = "0";
						}
						block.append("? Path not present in component, using factor: " + alt + "\n\n");
						String rate = checkForStateVarsAndNested(alt, comp, paramMappings);
						block.append(prefix + dv.getName() + " = " + rate + " \n\n");
					} else {
						String localVar = dv.getPath().replaceAll("/", "_");
						String globalVar = prefix + dv.getPath().replaceAll("/", "_");
						// String var0 = var;

						String eqn = globalVar;
						if (globalVar.indexOf("[*]") >= 0
								&& globalVar.indexOf("syn") >= 0) {
							eqn = "0 ? Was: " + localVar + " but insertion of currents from external attachments not yet supported";
						} else if (localVar.indexOf("[*]") >= 0) {
							String children = localVar.substring(0, localVar.indexOf("[*]"));
							String path = localVar.substring(localVar.indexOf("[*]_") + 4);
							String reduce = dv.getReduce();
							String op = null;
							if (reduce.equals("multiply")) {
								op = " * ";
							}
							if (reduce.equals("add")) {
								op = " + ";
							}
							eqn = "";

							for (Component childComp : comp.getChildrenAL(children)) {
								// var = var + childComp.getID()+" --";
								if (eqn.length() > 0) {
									eqn = eqn + op;
								}
								eqn = eqn + childComp.getID() + "_" + path;
							}
							eqn = eqn + " ? " + reduce + " applied to all instances of " + path + " in: <" + children + "> (" + comp.getChildrenAL(children) + ")" + " c2 (" + comp.getAllChildren() + ")";
						}
						block.append(prefix + dv.getName() + " = " + eqn + " ? path based\n\n");
					}
				}
				// blockForEqns.insert(0, block);
				blockForEqns.append(block);
			}

			for (ConditionalDerivedVariable cdv : comp.getComponentType()
					.getDynamics().getConditionalDerivedVariables()) {

				StringBuilder block = new StringBuilder();

				String mappedName = prefix + cdv.getName();
				if (!rangeVars.contains(mappedName)) {
					rangeVars.add(mappedName);

					String range = "\nRANGE " + mappedName;
					while (range.length() < NRNConst.commentOffset) {
						range = range + " ";
					}

					blockNeuron.append(range + ": conditional derived var\n");
					paramMappingsComp.put(cdv.getName(), mappedName);
				}

				String assig = "\n" + prefix + cdv.getName() + " " + getNeuronUnit(cdv.dimension);
				while (assig.length() < NRNConst.commentOffset) {
					assig = assig + " ";
				}

				blockAssigned.append(assig + ": conditional derived var...\n");

				for (Case c : cdv.cases) {

					String rate = checkForStateVarsAndNested(c.getValueExpression(), comp, paramMappings);

					String cond = "\n} else ";
				if (c.condition != null) {
					String cond_ = checkForStateVarsAndNested(c.condition, comp, paramMappings);
					cond = "if (" + cond_ + ") ";
					if (block.length() != 0)
						cond = "else " + cond;
				}

				block.append(cond + " { \n    " + prefix + cdv.getName() + " = " + rate + " ? evaluable cdv");
				}

				blockForEqns.append(block + "\n}\n\n");

			}

		}

	}

	private static String getNeuronUnit(String dimensionName) {

		if (dimensionName == null) {
			return ": no units???";
		}

		if (dimensionName.equals("voltage")) {
			return "(mV)";
		} else if (dimensionName.equals("per_voltage")) {
			return "(/mV)";
		} else if (dimensionName.equals("conductance")) {
			return "(uS)";
		} else if (dimensionName.equals("capacitance")) {
			return "(microfarads)";
		} else if (dimensionName.equals("time")) {
			return "(ms)";
		} else if (dimensionName.equals("per_time")) {
			return "(kHz)";
		} else if (dimensionName.equals("current")) {
			return "(nA)";
		} else if (dimensionName.equals("length")) {
			return "(um)";
		} else if (dimensionName.equals("area")) {
			return "(um2)";
		} else if (dimensionName.equals("volume")) {
			return "(um3)";
		} else if (dimensionName.equals("concentration")) {
			return "(mM)";
		} else if (dimensionName.equals("charge_per_mole")) {
			return "(coulomb)";
		} else if (dimensionName.equals("temperature")) {
			return "(K)";
		} else if (dimensionName.equals("idealGasConstantDims")) {
			return "(millijoule / K)";
		} else if (dimensionName.equals("rho_factor")) {
			return "(mM m2 /A /s)";
		} else if (dimensionName.equals(Dimension.NO_DIMENSION)) {
			return "";
		} else {
			return "? Don't know units for : (" + dimensionName + ")";
		}
	}

	private static float convertToNeuronUnits(float val, String dimensionName) {
		float newVal = val * getNeuronUnitFactor(dimensionName);
		return newVal;
	}

	private static float getNeuronUnitFactor(String dimensionName) {

		if (dimensionName.equals("voltage")) {
			return 1000f;
		} else if (dimensionName.equals("per_voltage")) {
			return 0.001f;
		} else if (dimensionName.equals("conductance")) {
			return 1000000f;
		} else if (dimensionName.equals("capacitance")) {
			return 1e6f;
		} else if (dimensionName.equals("per_time")) {
			return 0.001f;
		} else if (dimensionName.equals("current")) {
			return 1e9f;
		} else if (dimensionName.equals("time")) {
			return 1000f;
		} else if (dimensionName.equals("length")) {
			return 1000000f;
		} else if (dimensionName.equals("area")) {
			return 1e12f;
		} else if (dimensionName.equals("volume")) {
			return 1e18f;
		} else if (dimensionName.equals("concentration")) {
			return 1f;
		} else if (dimensionName.equals("charge_per_mole")) {
			return 1f;
		} else if (dimensionName.equals("idealGasConstantDims")) {
			return 1000f;
		} else if (dimensionName.equals("rho_factor")) {
			return 1f;
		}
		return 1f;
	}

	private static String getDerivativeUnit(String dimensionName) {
		String unit = getNeuronUnit(dimensionName);
		if (unit.equals("")) {
			return "(/ms)";
		} else {
			return unit.replaceAll("\\)", "/ms)");
		}
	}

	public static void writeModBlock(StringBuilder main, String blockName,
			String contents) {
		contents = contents.replaceAll("\n", "\n    ");
		if (!contents.endsWith("\n")) {
			contents = contents + "\n";
		}
		main.append(blockName + " {\n    " + contents + "}\n\n");
	}

	public class CompInfo {

		StringBuilder params = new StringBuilder();
		StringBuilder eqns = new StringBuilder();
		StringBuilder initInfo = new StringBuilder();
	}

	public static void main(String[] args) throws Exception {

		E.setDebug(false);
		ArrayList<File> nml2Channels = new ArrayList<File>();

		// nml2Channels.add(new
				// File("../nCexamples/Ex10_NeuroML2/cellMechanisms/IzhBurst/IzhBurst.nml"));
		nml2Channels.add(new File("../lemspaper/tidyExamples/test/HH_cell.nml"));

		// File expDir = new File("src/test/resources/tmp");
		// for (File f : expDir.listFiles()) {
		// f.delete();
		// }

		File lemsFile = new File("../lemspaper/tidyExamples/test/Fig_HH.xml");
		lemsFile = new File("../NeuroML2/NeuroML2CoreTypes/LEMS_NML2_Ex5_DetCell.xml");
		lemsFile = new File("../neuroConstruct/osb/invertebrate/lobster/PyloricNetwork/neuroConstruct/generatedNeuroML2/LEMS_PyloricPacemakerNetwork.xml");

		// lemsFile = new
		// File("../neuroConstruct/osb/invertebrate/celegans/muscle_model/NeuroML2/LEMS_Figure2A.xml");
		lemsFile = new File("../neuroConstruct/osb/cerebral_cortex/networks/ACnet2/neuroConstruct/generatedNeuroML2/LEMS_ACnet2.xml");
		lemsFile = new File("../NeuroML2/NeuroML2CoreTypes/LEMS_NML2_Ex9_FN.xml");
		lemsFile = new File("src/test/resources/BIOMD0000000185_LEMS.xml"); lemsFile = new File(
				"../neuroConstruct/osb/cerebellum/cerebellar_granule_cell/GranuleCell/neuroConstruct/generatedNeuroML2/LEMS_GranuleCell.xml");
		lemsFile = new File("../org.neuroml.import/src/test/resources/Simple3Species_LEMS.xml");
		lemsFile = new File("../neuroConstruct/osb/showcase/neuroConstructShowcase/Ex4_HHcell/generatedNeuroML2/LEMS_Ex4_HHcell.xml");
		lemsFile = new File("../neuroConstruct/osb/invertebrate/lobster/PyloricNetwork/neuroConstruct/generatedNeuroML2/LEMS_PyloricPacemakerNetwork.xml");

		Lems lems = Utils.readLemsNeuroMLFile(lemsFile).getLems();
		File mainFile = new File(lemsFile.getParentFile(), lemsFile.getName().replaceAll(".xml", "_nrn.py"));

		NeuronWriter nw = new NeuronWriter(lems);
		ArrayList<File> ff = nw.generateMainScriptAndMods(mainFile);
		for (File f : ff) {
			System.out.println("Generated: " + f.getAbsolutePath());
		}

		//              for (File nml2Channel : nml2Channels) {
		//                      String nml2Content = FileUtil.readStringFromFile(nml2Channel);
		//                      // System.out.println("nml2Content: "+nml2Content); String lemsified
		//                      // =
		//                      NeuroMLConverter.convertNeuroML2ToLems(nml2Content);
		//                      // System.out.println("lemsified: "+lemsified);
		//
		//                      Lems lems = Utils.readLemsNeuroMLFile(lemsified).getLems();
		//
		//                      for (Component comp : lems.components.getContents()) {
		//                              E.info("Component: " + comp);
		//                              E.info("baseIonChannel: "
		//                                              + comp.getComponentType().isOrExtends(
		//                                                              NeuroMLElements.ION_CHANNEL_COMP_TYPE));
		//                              E.info("baseCell: "
		//                                              + comp.getComponentType().isOrExtends(
		//                                                              NeuroMLElements.BASE_CELL_COMP_TYPE));
		//                              E.info("concentrationModel: "
		//                                              + comp.getComponentType().isOrExtends(
		//                                                              NeuroMLElements.CONC_MODEL_COMP_TYPE));
		//                              E.info("basePointCurrent: "
		//                                              + comp.getComponentType().isOrExtends(
		//                                                              NeuroMLElements.BASE_POINT_CURR_COMP_TYPE));
		//
		//                              if (comp.getComponentType().isOrExtends(
		//                                              NeuroMLElements.ION_CHANNEL_COMP_TYPE)
		//                                              || comp.getComponentType().isOrExtends(
		//                                                              NeuroMLElements.BASE_CELL_COMP_TYPE)
		//                                              || comp.getComponentType().isOrExtends(
		//                                                              NeuroMLElements.CONC_MODEL_COMP_TYPE)
		//                                              || comp.getComponentType().isOrExtends(
		//                                                              NeuroMLElements.BASE_POINT_CURR_COMP_TYPE)) {
		//                                      E.info(comp + " is an "
		//                                                      + NeuroMLElements.ION_CHANNEL_COMP_TYPE + " or  "
		//                                                      + NeuroMLElements.BASE_CELL_COMP_TYPE + " or  "
		//                                                      + NeuroMLElements.CONC_MODEL_COMP_TYPE + " or  "
		//                                                      + NeuroMLElements.BASE_POINT_CURR_COMP_TYPE);
		//                                      String mod = generateModFile(comp);
		//
		//                                      File expFile = new File(expDir, comp.getID() + ".mod");
		//
		//                                      E.info("\n----------------------------------------------------   \n");
		//                                      E.info(mod);
		//
		//                                      FileUtil.writeStringToFile(mod, expFile);
		//                                      E.info("Exported file to: " + expFile.getCanonicalPath());
		//
		//                              }
		//
		//                      }
		//              }
	}
}

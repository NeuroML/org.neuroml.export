/**
 *
 */
package org.neuroml.export.neuron;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.lemsml.jlems.core.sim.LEMSException;
import org.lemsml.jlems.io.util.FileUtil;
import org.neuroml.export.utils.Utils;
import org.neuroml.model.Annotation;
import org.neuroml.model.BiophysicalProperties;
import org.neuroml.model.Cell;
import org.neuroml.model.ChannelDensity;
import org.neuroml.model.ChannelDensityGHK;
import org.neuroml.model.ChannelDensityNernst;
import org.neuroml.model.ChannelDensityNonUniform;
import org.neuroml.model.ChannelDensityNonUniformNernst;
import org.neuroml.model.InhomogeneousParameter;
import org.neuroml.model.InitMembPotential;
import org.neuroml.model.IntracellularProperties;
import org.neuroml.model.Member;
import org.neuroml.model.MembraneProperties;
import org.neuroml.model.Morphology;
import org.neuroml.model.NeuroMLDocument;
import org.neuroml.model.Point3DWithDiam;
import org.neuroml.model.Resistivity;
import org.neuroml.model.Segment;
import org.neuroml.model.SegmentGroup;
import org.neuroml.model.Species;
import org.neuroml.model.SpecificCapacitance;
import org.neuroml.model.VariableParameter;
import org.neuroml.model.util.CellUtils;
import org.neuroml.model.util.NeuroMLConverter;
import org.neuroml.model.util.NeuroMLException;
import org.w3c.dom.Element;

/**
 * @author boris
 * 
 */
public class JSONCellSerializer
{

	public static String cellToJson(Cell cell, NeuronWriter.SupportedUnits units) throws LEMSException, NeuroMLException
	{

		NamingHelper nh = new NamingHelper(cell);
		JsonFactory f = new JsonFactory();
		StringWriter sw = new StringWriter();
		try
		{
			JsonGenerator g = f.createJsonGenerator(sw);
			g.useDefaultPrettyPrinter();
			g.writeStartObject();

			g.writeStringField("id", cell.getId());
			if(cell.getNotes() != null && cell.getNotes().length() > 0)
			{
				g.writeStringField("notes", cell.getNotes());
			}

			g.writeArrayFieldStart("sections");

			Morphology morph = cell.getMorphology();

			HashMap<Integer, Segment> idsVsSegments = CellUtils.getIdsVsSegments(cell);
			HashMap<Integer, String> idsVsNames = new HashMap<Integer, String>();
			HashMap<SegmentGroup, ArrayList<Integer>> sgVsSegId = CellUtils.getSegmentGroupsVsSegIds(cell);
			HashMap<String, SegmentGroup> namesVsSegmentGroups = CellUtils.getNamesVsSegmentGroups(cell);

			boolean foundNeuroLexFlags = false;

			for(SegmentGroup grp : sgVsSegId.keySet())
			{
				if(CellUtils.isUnbranchedNonOverlapping(grp))
				{
					foundNeuroLexFlags = true;
					ArrayList<Integer> segsHere = sgVsSegId.get(grp);

					g.writeStartObject();
					g.writeStringField("name", grp.getId());
					g.writeStringField("id", grp.getId());

					g.writeArrayFieldStart("points3d");
					String comments = null;

					int lastSegId = -1;
					for(int segId : segsHere)
					{

						Segment seg = idsVsSegments.get(segId);
						Segment parentSegment = null;
						if(seg.getParent() != null && seg.getParent().getSegment() != null)
						{
							parentSegment = idsVsSegments.get(seg.getParent().getSegment());

						}
						// System.out.println("  Next segment: "+seg.getId()+", parent: "+ (parentSegment!=null?parentSegment.getId():"<none>")+", lastSegId: "+lastSegId);

						Point3DWithDiam prox = null;

						if(parentSegment == null || !segsHere.contains(parentSegment.getId()))
						{
							prox = seg.getProximal();
							g.writeString(getPt3dString(prox));
						}
						else
						{
							if(parentSegment.getId() != lastSegId)
							{
								throw new NeuroMLException("Error in (ordering of?) set of segment ids for segmentGroup: " + grp + "\n" + "segsHere: " + segsHere + "\n" + "expecting parent: "
										+ lastSegId + "\n" + "for segment: " + seg);
							}
						}
						Point3DWithDiam dist = seg.getDistal();

						if(prox != null && dist.getX() == prox.getX() && dist.getY() == prox.getY() && dist.getZ() == prox.getZ() && dist.getDiameter() == prox.getDiameter())
						{
							comments = "Section in NeuroML is spherical, so using cylindrical section along Y axis for NEURON";
							dist.setY(dist.getY() + dist.getDiameter());
						}

						g.writeString(getPt3dString(dist));
						lastSegId = seg.getId();
					}
					g.writeEndArray(); // points3d

					Segment firstSeg = idsVsSegments.get(segsHere.get(0));

					if(firstSeg.getParent() != null)
					{
						double fract = firstSeg.getParent().getFractionAlong();

						if(Math.abs(fract - 1) < 1e-4)
						{
							fract = 1;
						}

						if(fract != 0 && fract != 1)
						{
							throw new NeuroMLException("Cannot yet handle fractionAlong being neither 0 or 1, it's " + fract + " in seg " + firstSeg + "...");
						}
						String parentSection = null;
						int parentId = (int) firstSeg.getParent().getSegment();

						// System.out.println("\n\nfirstSeg: "+firstSeg+ "segsHere: "+segsHere);
						// System.out.println("parentId: "+parentId);
						for(SegmentGroup sgPar : sgVsSegId.keySet())
						{
							if(parentSection == null)
							{
								// System.out.println("parent? SG "+sgPar.getId()+" ("+sgPar.getNeuroLexId()+", "+CellUtils.isUnbranchedNonOverlapping(sgPar)+"): "+sgVsSegId.get(sgPar));
								ArrayList<Integer> segsPar = sgVsSegId.get(sgPar);
								if(CellUtils.isUnbranchedNonOverlapping(sgPar) && segsPar.contains(parentId))
								{
									if(fract == 1 && segsPar.get(segsPar.size() - 1) == parentId)
									{
										parentSection = sgPar.getId();
									}
									else if(fract == 0 && segsPar.get(0) == parentId)
									{
										parentSection = sgPar.getId();
									}
									else
									{
										parentSection = sgPar.getId();
										// TODO: fix this!!!
										fract = 0.5555555555555555;
										comments = "ERROR in fraction along!! Need to work out fraction along SECTION from info about connection to a SEGMENT which is not at start or finish!!";
									}
								}
							}
						}
						g.writeStringField("parent", parentSection);

						g.writeNumberField("fractionAlong", fract);
					}

					// TODO: make this a more generic function & use string fields
					if(grp.getAnnotation() != null)
					{
						Annotation annot = grp.getAnnotation();
						for(Element prop : annot.getAny())
						{
							if(prop != null && prop.getNodeName().equals("property") && prop.hasAttribute("tag") && prop.getAttribute("tag").equals("numberInternalDivisions")
									&& prop.hasAttribute("value"))
							{

								g.writeNumberField("numberInternalDivisions", Integer.parseInt(prop.getAttribute("value")));
							}
						}
					}

					if(comments != null)
					{
						g.writeStringField("comments", comments);
					}

					g.writeEndObject();

				}
			}

			if(!foundNeuroLexFlags)
			{

				for(Segment seg : morph.getSegment())
				{
					// System.out.println("Segment: "+seg.getId()+", parent: "+seg.getParent());
					g.writeStartObject();
					String name = nh.getNrnSectionName(seg);
					idsVsNames.put(seg.getId(), name);
					// g.writeObjectFieldStart(name);
					g.writeStringField("name", name);
					g.writeStringField("id", seg.getId() + "");
					Segment parentSegment = null;
					if(seg.getParent() != null && seg.getParent().getSegment() != null)
					{
						parentSegment = idsVsSegments.get(seg.getParent().getSegment());
						String parent = idsVsNames.get(seg.getParent().getSegment());
						g.writeStringField("parent", parent);
						g.writeNumberField("fractionAlong", seg.getParent().getFractionAlong());
					}
					String comments = null;
					g.writeArrayFieldStart("points3d");
					Point3DWithDiam p0 = seg.getDistal();
					g.writeString(getPt3dString(p0));
					Point3DWithDiam p1 = seg.getProximal();
					if(p1 == null)
					{
						p1 = parentSegment.getDistal();
					}
					if(p0.getX() == p1.getX() && p0.getY() == p1.getY() && p0.getZ() == p1.getZ() && p0.getDiameter() == p1.getDiameter())
					{
						comments = "Section in NeuroML is spherical, so using cylindrical section along Y axis in NEURON";
						p1.setY(p1.getY() + p0.getDiameter());
					}
					g.writeString(getPt3dString(p1));
					g.writeEndArray();
					if(comments != null)
					{
						g.writeStringField("comments", comments);
					}

					// g.writeEndObject();
					g.writeEndObject();
				}
			}

			g.writeEndArray();

			g.writeArrayFieldStart("groups");
			boolean foundAll = false;
			for(SegmentGroup grp : morph.getSegmentGroup())
			{
				if(!(foundNeuroLexFlags && CellUtils.isUnbranchedNonOverlapping(grp)))
				{
					g.writeStartObject();
					g.writeStringField("name", grp.getId());
					foundAll = foundAll || grp.getId().equals("all");
					if(!grp.getMember().isEmpty())
					{
						g.writeArrayFieldStart("segments");
						for(Member m : grp.getMember())
						{
							g.writeString(idsVsNames.get(m.getSegment()));
						}
						g.writeEndArray();
					}
					if(!grp.getInclude().isEmpty())
					{
						g.writeArrayFieldStart("groups");
						for(org.neuroml.model.Include inc : grp.getInclude())
						{
							boolean isSection = CellUtils.isUnbranchedNonOverlapping(namesVsSegmentGroups.get(inc.getSegmentGroup()));
							if(!isSection)
							{
								g.writeString(inc.getSegmentGroup());
							}
						}
						g.writeEndArray();
						g.writeArrayFieldStart("sections");
						for(org.neuroml.model.Include inc : grp.getInclude())
						{
							boolean isSection = CellUtils.isUnbranchedNonOverlapping(namesVsSegmentGroups.get(inc.getSegmentGroup()));
							if(isSection)
							{
								g.writeString(inc.getSegmentGroup());
							}
						}
						g.writeEndArray();
					}
					// System.out.println("+++ " +grp.getInhomogeneousParameter());
					// System.out.println("--  " +ChannelDensity.class.getSimpleName());
					if(!grp.getInhomogeneousParameter().isEmpty())
					{
						g.writeArrayFieldStart("inhomogeneousParameters");
						for(InhomogeneousParameter ih : grp.getInhomogeneousParameter())
						{
							g.writeStartObject();
							g.writeStringField("id", ih.getId());
							g.writeStringField("variable", ih.getVariable());
							g.writeStringField("metric", ih.getMetric().value());
							if(ih.getProximal() != null)
							{
								g.writeStringField("proximalTranslationStart", ih.getProximal().getTranslationStart() + "");
							}
							if(ih.getDistal() != null)
							{
								g.writeStringField("distalNormalizationEnd", ih.getDistal().getNormalizationEnd() + "");
							}
							g.writeEndObject();
						}
						g.writeEndArray();
					}

					g.writeEndObject();
				}
			}
			if(!foundAll)
			{
				g.writeStartObject();
				g.writeStringField("name", "all");
				g.writeArrayFieldStart("sections");
				for(Segment seg : morph.getSegment())
				{
					String name = nh.getNrnSectionName(seg);
					g.writeString(name);
				}
				g.writeEndArray();
				g.writeEndObject();

			}
			g.writeEndArray();

			BiophysicalProperties bp = cell.getBiophysicalProperties();
			g.writeArrayFieldStart("specificCapacitance");
			MembraneProperties mp = bp.getMembraneProperties();
			for(SpecificCapacitance sc : mp.getSpecificCapacitance())
			{
				g.writeStartObject();
				String group = sc.getSegmentGroup() == null ? "all" : sc.getSegmentGroup();
				g.writeStringField("group", group);
				float value = Utils.getMagnitudeInSI(sc.getValue()) * units.specCapFactor;
				g.writeStringField("value", NeuronWriter.formatDefault(value));
				g.writeEndObject();

			}
			g.writeEndArray();

			g.writeArrayFieldStart("initMembPotential");

			for(InitMembPotential imp : mp.getInitMembPotential())
			{
				g.writeStartObject();
				String group = imp.getSegmentGroup() == null ? "all" : imp.getSegmentGroup();
				g.writeStringField("group", group);
				float value = Utils.getMagnitudeInSI(imp.getValue()) * units.voltageFactor;
				g.writeStringField("value", NeuronWriter.formatDefault(value));
				g.writeEndObject();

			}
			g.writeEndArray();

			g.writeArrayFieldStart("resistivity");
			IntracellularProperties ip = bp.getIntracellularProperties();
			for(Resistivity res : ip.getResistivity())
			{
				g.writeStartObject();
				String group = res.getSegmentGroup() == null ? "all" : res.getSegmentGroup();
				g.writeStringField("group", group);

				float value = Utils.getMagnitudeInSI(res.getValue()) * units.resistivityFactor;
				g.writeStringField("value", NeuronWriter.formatDefault(value));
				g.writeEndObject();

			}
			g.writeEndArray();

			g.writeArrayFieldStart("channelDensity");
			for(ChannelDensity cd : mp.getChannelDensity())
			{
				g.writeStartObject();
				g.writeStringField("id", cd.getId());

				g.writeStringField("ionChannel", NRNUtils.getSafeName(cd.getIonChannel()));

				if(cd.getIon() != null)
				{
					g.writeStringField("ion", cd.getIon());
				}
				else
				{
					g.writeStringField("ion", "non_specific");
				}

				String group = cd.getSegmentGroup() == null ? "all" : cd.getSegmentGroup();
				g.writeStringField("group", group);

				float valueCondDens = Utils.getMagnitudeInSI(cd.getCondDensity()) * units.condDensFactor;
				g.writeStringField("condDens", NeuronWriter.formatDefault(valueCondDens));

				float valueErev = Utils.getMagnitudeInSI(cd.getErev()) * units.voltageFactor;
				g.writeStringField("erev", NeuronWriter.formatDefault(valueErev));

				g.writeEndObject();
			}
			for(ChannelDensityNonUniform cd : mp.getChannelDensityNonUniform())
			{
				g.writeStartObject();
				g.writeStringField("id", cd.getId());

				g.writeStringField("ionChannel", NRNUtils.getSafeName(cd.getIonChannel()));

				for(VariableParameter vp : cd.getVariableParameter())
				{
					if(vp.getParameter().equals("condDensity"))
					{
						g.writeStringField("group", vp.getSegmentGroup());
						g.writeStringField("inhomogeneousParameter", vp.getInhomogeneousValue().getInhomogeneousParameter());

						String convFactor = units.condDensFactor + " * ";
						g.writeStringField("inhomogeneousValue", convFactor + vp.getInhomogeneousValue().getValue());
						g.writeStringField("comment", "Conversion factor of:  (" + convFactor + ") added");
					}
				}

				if(cd.getIon() != null)
				{
					g.writeStringField("ion", cd.getIon());
				}
				else
				{
					g.writeStringField("ion", "non_specific");
				}

				float valueErev = Utils.getMagnitudeInSI(cd.getErev()) * units.voltageFactor;
				g.writeStringField("erev", NeuronWriter.formatDefault(valueErev));

				g.writeEndObject();
			}
			for(ChannelDensityNernst cdn : mp.getChannelDensityNernst())
			{
				g.writeStartObject();
				g.writeStringField("id", cdn.getId());
				g.writeStringField("ionChannel", NRNUtils.getSafeName(cdn.getIonChannel()));
				g.writeStringField("ion", cdn.getIon());

				String group = cdn.getSegmentGroup() == null ? "all" : cdn.getSegmentGroup();
				g.writeStringField("group", group);

				float valueCondDens = Utils.getMagnitudeInSI(cdn.getCondDensity()) * units.condDensFactor;
				g.writeStringField("condDens", NeuronWriter.formatDefault(valueCondDens));

				g.writeStringField("erev", "calculated_by_Nernst_equation");

				g.writeEndObject();
			}

			for(ChannelDensityNonUniformNernst cdnn : mp.getChannelDensityNonUniformNernst())
			{
				g.writeStartObject();
				g.writeStringField("id", cdnn.getId());
				g.writeStringField("ionChannel", NRNUtils.getSafeName(cdnn.getIonChannel()));
				g.writeStringField("ion", cdnn.getIon());

				for(VariableParameter vp : cdnn.getVariableParameter())
				{
					if(vp.getParameter().equals("condDensity"))
					{
						g.writeStringField("group", vp.getSegmentGroup());
						g.writeStringField("inhomogeneousParameter", vp.getInhomogeneousValue().getInhomogeneousParameter());

						String convFactor = units.condDensFactor + " * ";
						g.writeStringField("inhomogeneousValue", convFactor + vp.getInhomogeneousValue().getValue());
						g.writeStringField("comment", "Conversion factor of:  (" + convFactor + ") added");
					}
				}

				g.writeStringField("erev", "calculated_by_Nernst_equation");

				g.writeEndObject();
			}
			for(ChannelDensityGHK cdg : mp.getChannelDensityGHK())
			{
				g.writeStartObject();
				g.writeStringField("id", cdg.getId());
				g.writeStringField("ionChannel", NRNUtils.getSafeName(cdg.getIonChannel()));

				g.writeStringField("ion", cdg.getIon());

				String group = cdg.getSegmentGroup() == null ? "all" : cdg.getSegmentGroup();
				g.writeStringField("group", group);

				float valuePermeab = Utils.getMagnitudeInSI(cdg.getPermeability()) * units.permeabilityFactor;
				g.writeStringField("permeability", NeuronWriter.formatDefault(valuePermeab));

				g.writeStringField("erev", "calculated_by_GHK_equation");

				g.writeEndObject();
			}

			g.writeEndArray();

			g.writeArrayFieldStart("species");
			for(Species sp : ip.getSpecies())
			{
				g.writeStartObject();
				g.writeStringField("id", sp.getId());
				g.writeStringField("ion", sp.getIon());
				g.writeStringField("concentrationModel", sp.getConcentrationModel());

				String group = sp.getSegmentGroup() == null ? "all" : sp.getSegmentGroup();
				g.writeStringField("group", group);

				float initConc = Utils.getMagnitudeInSI(sp.getInitialConcentration()) * units.concentrationFactor;
				g.writeStringField("initialConcentration", NeuronWriter.formatDefault(initConc));
				float initExtConc = Utils.getMagnitudeInSI(sp.getInitialExtConcentration()) * units.concentrationFactor;
				g.writeStringField("initialExtConcentration", NeuronWriter.formatDefault(initExtConc));

				g.writeEndObject();
			}
			g.writeEndArray();

			g.writeEndObject();
			g.close();
		}
		catch(IOException ex)
		{
			throw new NeuroMLException("Problem converting Cell to JSON format", ex);
		}

		// System.out.println(sw.toString());
		return sw.toString();

	}

	private static String getPt3dString(Point3DWithDiam p0)
	{
		return String.format("%g, %g, %g, %g", p0.getX(), p0.getY(), p0.getZ(), p0.getDiameter());
	}

	public static void main(String[] args) throws Exception
	{
		NeuroMLConverter conv = new NeuroMLConverter();
		// String test = "/home/padraig/neuroConstruct/osb/cerebral_cortex/networks/ACnet2/neuroConstruct/generatedNeuroML2/bask.cell.nml";
		// String test = "/home/padraig/neuroConstruct/osb/hippocampus/networks/nc_superdeep/neuroConstruct/generatedNeuroML2/pvbasketcell.cell.nml";
		ArrayList<String> tests = new ArrayList<String>();

		tests.add("/home/padraig/neuroConstruct/osb/cerebral_cortex/networks/ACnet2/neuroConstruct/generatedNeuroML2/pyr_4_sym.cell.nml");
		tests.add("/home/padraig/neuroConstruct/osb/cerebral_cortex/networks/ACnet2/neuroConstruct/generatedNeuroML2/bask_soma.cell.nml");
		tests.add("/home/padraig/neuroConstruct/osb/hippocampus/networks/nc_superdeep/neuroConstruct/generatedNeuroML2/pvbasketcell.cell.nml");
		// tests.add("/home/padraig/neuroConstruct/testProjects/TestMorphs/generatedNeuroML2/SampleCell_ca.cell.nml");
		tests.add("/home/padraig/neuroConstruct/osb/cerebral_cortex/neocortical_pyramidal_neuron/L5bPyrCellHayEtAl2011/neuroConstruct/generatedNeuroML2/L5PC.cell.nml");
		/**/
		tests.add("/home/padraig/neuroConstruct/osb/invertebrate/celegans/muscle_model/NeuroML2/SingleCompMuscle.cell.nml");
		tests.add("/home/padraig/NeuroML2/examples/NML2_SingleCompHHCell.nml");

		for(String test : tests)
		{
			NeuroMLDocument nml2 = conv.loadNeuroML(new File(test));

			Cell cell = nml2.getCell().get(0);
			System.out.println("cell: " + cell.getId());

			String json = cellToJson(cell, NeuronWriter.SupportedUnits.NEURON);
			// System.out.println(json);
			String ref = test.substring(test.lastIndexOf("/")).split("\\.")[0];
			File f = new File("../temp/" + ref + ".json");
			FileUtil.writeStringToFile(json, f);
			System.out.println("Written to: " + f.getCanonicalPath());
		}
	}

}

package org.neuroml.export.info;

import java.util.ArrayList;
import java.util.List;

import org.neuroml.export.info.model.ChannelInfoExtractor;
import org.neuroml.export.info.model.InfoNode;
import org.neuroml.model.BaseCell;
import org.neuroml.model.Cell;
import org.neuroml.model.IonChannel;
import org.neuroml.model.Morphology;
import org.neuroml.model.Network;
import org.neuroml.model.NeuroMLDocument;
import org.neuroml.model.Population;
import org.neuroml.model.Projection;
import org.neuroml.model.Standalone;

/**
 * @author matteocantarelli
 * 
 */
public class InfoTreeCreator
{

	/**
	 * @return
	 */
	public static InfoNode createInfoTree(final NeuroMLDocument nmlDocument)
	{
		InfoNode infoRoot = new InfoNode();

		for(Cell cell : nmlDocument.getCell())
		{
			InfoNode cellProps = new InfoNode();

			cellProps.put("ID", cell.getId());
			if(cell.getNotes() != null && cell.getNotes().length() > 0) cellProps.put("Description", formatNotes(cell.getNotes()));
			Morphology morph = cell.getMorphology();
			cellProps.put("Number of segments", morph.getSegment().size());

			infoRoot.put("Cell " + cell.getId(), cellProps);
		}
		for(IonChannel chan : nmlDocument.getIonChannel())
		{
			InfoNode chanProps = new InfoNode();
			ChannelInfoExtractor cinfo = new ChannelInfoExtractor(chan);

			chanProps.put("ID", chan.getId());
			
			chanProps.put("Gates", cinfo.getGates());

			if(chan.getNotes() != null && chan.getNotes().length() > 0) chanProps.put("Description", formatNotes(chan.getNotes()));

			infoRoot.put("Ion Channel " + chan.getId(), chanProps);
		}
		for(Network element : nmlDocument.getNetwork())
		{
			InfoNode elementProps = new InfoNode();

			elementProps.put("ID", element.getId());
			if(element.getNotes() != null && element.getNotes().length() > 0) elementProps.put("Description", formatNotes(element.getNotes()));

			elementProps.put("Number of populations", element.getPopulation().size());

			for(Population sub : element.getPopulation())
			{
				InfoNode subProps = new InfoNode();

				subProps.put("ID", sub.getId());
				if(sub.getNotes() != null && sub.getNotes().length() > 0) subProps.put("Description", formatNotes(sub.getNotes()));

				if(sub.getSize() != null) subProps.put("Size", sub.getSize());
				else if(!sub.getInstance().isEmpty())
				{
					subProps.put("Size (number of instances)", sub.getInstance().size());
				}

				elementProps.put("Population " + sub.getId(), subProps);
			}

			elementProps.put("Number of projections", element.getProjection().size());

			for(Projection sub : element.getProjection())
			{
				InfoNode subProps = new InfoNode();

				subProps.put("ID", sub.getId());
				subProps.put("Presynaptic population", sub.getPresynapticPopulation());
				subProps.put("Postsynaptic population", sub.getPostsynapticPopulation());

				elementProps.put("Projection " + sub.getId(), subProps);
			}

			infoRoot.put("Network " + element.getId(), elementProps);
		}

		// Testing...
		List<BaseCell> remainder = new ArrayList<BaseCell>();
		remainder.addAll(nmlDocument.getIafRefCell());
		remainder.addAll(nmlDocument.getAdExIaFCell());

		for(Object obj : remainder)
		{
			Standalone element = (Standalone) obj;
			InfoNode elementProps = new InfoNode();
			elementProps.put("ID", element.getId());
			if(element.getNotes() != null && element.getNotes().length() > 0) elementProps.put("Description", formatNotes(element.getNotes()));

			infoRoot.put("Element " + element.getId(), elementProps);

		}

		return infoRoot;
	}
	
	/**
	 * @param notes
	 * @return
	 */
	private static String formatNotes(String notes)
	{
		String formatted = notes.replaceAll("\n", " ");
		while(formatted.indexOf("  ") >= 0)
			formatted = formatted.replaceAll("  ", " ");
		return formatted;
	}

}

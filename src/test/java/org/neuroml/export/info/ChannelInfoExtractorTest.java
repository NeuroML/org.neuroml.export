/**
 * 
 */
package org.neuroml.export.info;

import javax.xml.bind.JAXBException;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.lemsml.jlems.core.sim.ContentError;
import org.lemsml.jlems.io.util.JUtil;
import org.neuroml.export.info.model.ChannelInfoExtractor;
import org.neuroml.export.utils.Utils;
import org.neuroml.model.IonChannel;
import org.neuroml.model.NeuroMLDocument;
import org.neuroml.model.util.NeuroMLConverter;
import org.neuroml.model.util.NeuroMLException;

/**
 * @author boris
 *
 */
public class ChannelInfoExtractorTest extends TestCase {

	/**
	 * Test method for {@link org.neuroml.export.info.model.ChannelInfoExtractor#getGates()}.
	 * @throws ContentError 
	 * @throws JAXBException 
	 * @throws NeuroMLException 
	 */
	public void testGetGates() throws ContentError, JAXBException, NeuroMLException {
		String content = JUtil.getRelativeResource(this.getClass(), Utils.NEUROML_EXAMPLES_RESOURCES_DIR+"/PyloricNetwork_KChannel.nml");
		//String content = JUtil.getRelativeResource(this.getClass(), Main.getNeuroMLExamplesResourcesDir()+"/KCond_NML2.nml");
		NeuroMLConverter nmlc = new NeuroMLConverter();
    	NeuroMLDocument nmlDocument = nmlc.loadNeuroML(content);
    	
    	IonChannel chan = nmlDocument.getIonChannel().get(0);
		ChannelInfoExtractor cie = new ChannelInfoExtractor(chan);	
		//Assert.assertEquals(, cie.getGates().get());
	}

}

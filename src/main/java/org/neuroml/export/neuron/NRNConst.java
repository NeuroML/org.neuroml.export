/**
 * 
 */
package org.neuroml.export.neuron;

import java.util.HashSet;
import java.util.Set;

/**
 * @author boris
 *
 */
public class NRNConst {

	final static String NEURON_FORMAT = "NEURON";
	final static String NEURON_VOLTAGE = "v";
	final static String NEURON_TEMP = "celsius";
	final static String RESERVED_STATE_SUFFIX = "I";
	final static String RATE_PREFIX = "rate_";
	final static String REGIME_PREFIX = "regime_";
	final static String V_COPY_PREFIX = "copy_";
	final static String DUMMY_POPULATION_PREFIX = "population_";
	
    final static String comm = "# ";
    final static String commPre = "'''";
    final static String commPost = "'''";


    static final int commentOffset = 40;
	static final String cellTemplateFile = "neuron/cell.vm";

	static final String generalUnits = 
			"\n(nA) = (nanoamp)\n" + 
			"(uA) = (microamp)\n" + 
			"(mA) = (milliamp)\n" + 
			"(A) = (amp)\n" + 
			"(mV) = (millivolt)\n" + 
			"(mS) = (millisiemens)\n" + 
			"(uS) = (microsiemens)\n" + 
			"(molar) = (1/liter)\n" + 
			"(kHz) = (kilohertz)\n" + 
			"(mM) = (millimolar)\n" + 
			"(um) = (micrometer)\n" + 
			"(S) = (siemens)\n"; 
	
	static final String ghkUnits = 
			": bypass nrn default faraday const\n" + 
			"FARADAY = 96485.3 (coulomb)\n" + 
			"R = (k-mole) (joule/degC)\n";
	
	static final String ghkFunctionDefs =
			"\nFUNCTION ghk(v(mV), ci(mM), co(mM)) (.001 coul/cm3) {\n" + 
			"        LOCAL z, eci, eco\n" + 
			"        z = (1e-3)*2*FARADAY*v/(R*(celsius+273.15))\n" + 
			"        eco = co*efun(z)\n" + 
			"        eci = ci*efun(-z)\n" + 
			"        :high cao charge moves inward\n" + 
			"        :negative potential charge moves inward\n" + 
			"        ghk = (.001)*2*FARADAY*(eci - eco)\n" + 
			"}\n" + 
			"\n" + 
			"FUNCTION efun(z) {\n" + 
			"        if (fabs(z) < 1e-4) {\n" + 
			"                efun = 1 - z/2\n" + 
			"        }else{\n" + 
			"                efun = z/(exp(z) - 1)\n" + 
			"        }\n" + 
			"}\n";

	// TODO Add more keywords / builtin mechanisms
	static final Set<String> NRNKeywords = new HashSet<String>() {{
		add("IClamp");
	}};
	
	
	public static String getSafeName(String id) {
		String prefix = "";

		if (NRNKeywords.contains(id)) {
			prefix = "my";
		}

		return prefix + id;
	}
}

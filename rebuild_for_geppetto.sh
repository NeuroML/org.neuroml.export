set -e

#### Script to rebuild libraries with modified dependencies required for use with Geppetto, e.g. https://github.com/openworm/org.geppetto.model.neuroml
## See https://github.com/NeuroML/org.neuroml.model.injectingplugin/blob/172771ec4f528a740bf68f6443e8b47cdf25cd25/pom.xml#L15
 
cd ../org.neuroml.model.injectingplugin/
mvn clean install -Dgeppetto

cd ../org.neuroml1.model
mvn clean install -Dgeppetto

cd ../NeuroML2
mvn clean install -Dgeppetto

cd ../org.neuroml.model
mvn clean install -Dgeppetto

cd ../jLEMS
mvn clean install 

cd ../org.neuroml.export
mvn clean install -Dgeppetto





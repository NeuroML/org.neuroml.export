Export from NeuroML & LEMS
==========================

Package to allow models in NeuroML 2 & LEMS format to be exported in various formats.
Based on earlier code at https://sourceforge.net/apps/trac/neuroml/browser/NeuroML2/src/org/neuroml/exporters 
and template based codegen from https://github.com/borismarin/som-codegen

Current targets: NEURON, SBML, Brian, XPP, Matlab, SED-ML, C/Sundials, Modelica

The simplest way to get access to this functionality is to install jNeuroML: https://github.com/NeuroML/jNeuroML
and find the range of export formats currently supported with:

    jnml -help
    
    
[![Build Status](https://travis-ci.org/NeuroML/org.neuroml.export.png)](https://travis-ci.org/NeuroML/org.neuroml.export)




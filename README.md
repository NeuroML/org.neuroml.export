Export from NeuroML & LEMS
==========================

[![Travis CI](https://travis-ci.com/NeuroML/org.neuroml.export.svg?branch=master)](https://travis-ci.com/NeuroML/org.neuroml.export)
[![GitHub](https://img.shields.io/github/license/NeuroML/org.neuroml.export)](https://github.com/NeuroML/org.neuroml.export/blob/master/LICENSE.lesser)
[![GitHub pull requests](https://img.shields.io/github/issues-pr/NeuroML/org.neuroml.export)](https://github.com/NeuroML/org.neuroml.export/pulls)
[![GitHub issues](https://img.shields.io/github/issues/NeuroML/org.neuroml.export)](https://github.com/NeuroML/org.neuroml.export/issues)
[![GitHub Org's stars](https://img.shields.io/github/stars/NeuroML?style=social)](https://github.com/NeuroML)
[![Twitter Follow](https://img.shields.io/twitter/follow/NeuroML?style=social)](https://twitter.com/NeuroML)
[![Gitter](https://badges.gitter.im/NeuroML/community.svg)](https://gitter.im/NeuroML/community?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)

Package to allow models in NeuroML 2 & LEMS format to be exported in various formats.
Based on earlier code at https://sourceforge.net/apps/trac/neuroml/browser/NeuroML2/src/org/neuroml/exporters 
and template based codegen from https://github.com/borismarin/som-codegen

Current targets: NEURON, SBML, Brian, XPP, Matlab, SED-ML, C/Sundials, Modelica

The simplest way to get access to this functionality is to install jNeuroML: https://github.com/NeuroML/jNeuroML
and find the range of export formats currently supported with:

    jnml -help
        
This code is distributed under the terms of the GNU Lesser General Public License.

The API documentation can be found [here](http://neuroml.github.io/org.neuroml.export/)

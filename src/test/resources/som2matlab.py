#!/usr/bin/env python
import sys
import json
import collections
import airspeed
airspeed.IDX_BASE = 1

if 3 != len(sys.argv):
    print "USAGE:", sys.argv[0], "model_definition.json template.vm"
    sys.exit(1)
    

with open(sys.argv[1]) as f:
    m = f.read()

m = m.replace('**', '.^')

#IMPORTANT: USE ORDERED DICT TO PRESERVE VAR ORDERING
# also making it work with velocity/java by creating a method:
class kludgeDict(collections.OrderedDict):
    def keySet(self):
        return self.keys()


model = json.JSONDecoder(object_pairs_hook=kludgeDict).decode(m)

with open(sys.argv[2]) as f:
    templ = airspeed.Template(f.read())

print templ.merge(model)










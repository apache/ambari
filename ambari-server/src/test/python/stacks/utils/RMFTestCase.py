#!/usr/bin/env python

'''
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
'''
__all__ = ["RMFTestCase", "Template", "StaticFile", "InlineTemplate"]

from unittest import TestCase
import json
import os
import imp
import sys
import pprint
from mock.mock import MagicMock, patch
from resource_management.core.environment import Environment
from resource_management.libraries.script.config_dictionary import ConfigDictionary
from resource_management.libraries.script.script import Script

class RMFTestCase(TestCase):
  def executeScript(self, path, classname=None, command=None, config_file=None):
    src_dir = RMFTestCase._getSrcFolder()
    stack_version = path.split(os.sep)[0]
    stacks_path = os.path.join(src_dir,"main/resources/stacks/HDP")
    configs_path = os.path.join(src_dir, "test/python/stacks/", stack_version, "configs")
    script_path = os.path.join(stacks_path, path)
    config_file_path = os.path.join(configs_path, config_file)
    
    try:
      with open(config_file_path, "r") as f:
        self.config_dict = ConfigDictionary(json.load(f))
    except IOError:
      raise RuntimeError("Can not read config file: "+ config_file_path)
    
    # append basedir to PYTHONPATH
    scriptsdir = os.path.dirname(script_path)
    basedir = os.path.dirname(scriptsdir)
    sys.path.append(scriptsdir)
    
    # get method to execute
    try:
      script_module = imp.load_source(classname, script_path)
    except IOError:
      raise RuntimeError("Cannot load class %s from %s",classname, path)
    
    script_class_inst = RMFTestCase._get_attr(script_module, classname)()
    method = RMFTestCase._get_attr(script_class_inst, command)
    
    # Reload params import, otherwise it won't change properties during next import
    if 'params' in sys.modules:  
      del(sys.modules["params"]) 
    
    # run
    with Environment(basedir, test_mode=True) as RMFTestCase.env:
      with patch.object(Script, 'install_packages', return_value=MagicMock()):
        with patch.object(Script, 'get_config', return_value=self.config_dict):
          method(RMFTestCase.env)
  
  def getConfig(self):
    return self.config_dict
          
  @staticmethod
  def _getSrcFolder():
    return os.path.join(os.path.abspath(os.path.dirname(__file__)),"../../../../")
      
  @staticmethod
  def _get_attr(module, attr):
    module_methods = dir(module)
    if not attr in module_methods:
      raise RuntimeError("'{0}' has no attribute '{1}'".format(module, attr))
    method = getattr(module, attr)
    return method
  
  def _ppformat(self, val):
    if isinstance(val, dict):
      return "self.getConfig()['configurations']['?']"
    
    val = pprint.pformat(val)
    
    if val.startswith("u'") or val.startswith('u"'):
      return val[1:]
    
    return val
  
  def printResources(self):
    for resource in RMFTestCase.env.resource_list:
      print "'{0}', {1},".format(resource.__class__.__name__, self._ppformat(resource.name))
      for k,v in resource.arguments.iteritems():
        print "  {0} = {1},".format(k, self._ppformat(v))
      print
  
  def assertResourceCalled(self, resource_type, name, **kwargs):
    resource = RMFTestCase.env.resource_list.pop(0)
    self.assertEquals(resource_type, resource.__class__.__name__)
    self.assertEquals(name, resource.name)
    self.assertEquals(kwargs, resource.arguments)
    
  def assertNoMoreResources(self):
    self.assertEquals(len(RMFTestCase.env.resource_list), 0, "There was other resources executed!")
    
  def assertResourceCalledByIndex(self, index, resource_type, name, **kwargs):
    resource = RMFTestCase.env.resource_list[index]
    self.assertEquals(resource_type, resource.__class__.__name__)
    self.assertEquals(name, resource.name)
    self.assertEquals(kwargs, resource.arguments)
    
# HACK: This is used to check Templates, StaticFile, InlineTemplate in testcases    
def Template(name, **kwargs):
  with RMFTestCase.env:
    from resource_management.core.source import Template
    return Template(name, kwargs)
  
def StaticFile(name, **kwargs):
  with RMFTestCase.env:
    from resource_management.core.source import StaticFile
    return StaticFile(name, kwargs)
  
def InlineTemplate(name, **kwargs):
  with RMFTestCase.env:
    from resource_management.core.source import InlineTemplate
    return InlineTemplate(name, kwargs)


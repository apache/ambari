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
__all__ = ["RMFTestCase", "Template", "StaticFile", "InlineTemplate", "UnknownConfigurationMock"]

from unittest import TestCase
import json
import os
import imp
import sys
import pprint
from mock.mock import MagicMock, patch
import platform

with patch("platform.linux_distribution", return_value = ('Suse','11','Final')):
  from resource_management.core.environment import Environment
  from resource_management.libraries.script.config_dictionary import ConfigDictionary
  from resource_management.libraries.script.script import Script
  from resource_management.libraries.script.config_dictionary import UnknownConfiguration


PATH_TO_STACKS = os.path.normpath("main/resources/stacks/HDP")
PATH_TO_STACK_TESTS = os.path.normpath("test/python/stacks/")

class RMFTestCase(TestCase):
  def executeScript(self, path, classname=None, command=None, config_file=None,
                    config_dict=None,
                    # common mocks for all the scripts
                    config_overrides = None,
                    shell_mock_value = (0, "OK."), 
                    os_type=('Suse','11','Final'),
                    kinit_path_local="/usr/bin/kinit"
                    ):
    norm_path = os.path.normpath(path)
    src_dir = RMFTestCase._getSrcFolder()
    stack_version = norm_path.split(os.sep)[0]
    stacks_path = os.path.join(src_dir, PATH_TO_STACKS)
    configs_path = os.path.join(src_dir, PATH_TO_STACK_TESTS, stack_version, "configs")
    script_path = os.path.join(stacks_path, norm_path)
    if config_file is not None and config_dict is None:
      config_file_path = os.path.join(configs_path, config_file)
      try:
        with open(config_file_path, "r") as f:
          self.config_dict = json.load(f)
      except IOError:
        raise RuntimeError("Can not read config file: "+ config_file_path)
    elif config_dict is not None and config_file is None:
      self.config_dict = config_dict
    else:
      raise RuntimeError("Please specify either config_file_path or config_dict parameter")

    if config_overrides:
      for key, value in config_overrides.iteritems():
        self.config_dict[key] = value

    self.config_dict = ConfigDictionary(self.config_dict)

    # append basedir to PYTHONPATH
    scriptsdir = os.path.dirname(script_path)
    basedir = os.path.dirname(scriptsdir)
    sys.path.append(scriptsdir)
    
    # get method to execute
    try:
      with patch.object(platform, 'linux_distribution', return_value=os_type):
        script_module = imp.load_source(classname, script_path)
    except IOError:
      raise RuntimeError("Cannot load class %s from %s",classname, norm_path)
    
    script_class_inst = RMFTestCase._get_attr(script_module, classname)()
    method = RMFTestCase._get_attr(script_class_inst, command)
    
    # Reload params import, otherwise it won't change properties during next import
    if 'params' in sys.modules:  
      del(sys.modules["params"]) 
    
    # run
    with Environment(basedir, test_mode=True) as RMFTestCase.env:
      with patch('resource_management.core.shell.checked_call', return_value=shell_mock_value): # we must always mock any shell calls
        with patch.object(Script, 'get_config', return_value=self.config_dict): # mocking configurations
          with patch.object(Script, 'get_tmp_dir', return_value="/tmp"):
            with patch.object(Script, 'install_packages'):
              with patch('resource_management.libraries.functions.get_kinit_path', return_value=kinit_path_local):
                with patch.object(platform, 'linux_distribution', return_value=os_type):
                  method(RMFTestCase.env)
    sys.path.remove(scriptsdir)
  
  def getConfig(self):
    return self.config_dict
          
  @staticmethod
  def _getSrcFolder():
    return os.path.join(os.path.abspath(os.path.dirname(__file__)),os.path.normpath("../../../../"))
      
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

  def reindent(self, s, numSpaces):
    return "\n".join((numSpaces * " ") + i for i in s.splitlines())

  def printResources(self, intendation=4):
    print
    for resource in RMFTestCase.env.resource_list:
      s = "'{0}', {1},".format(
        resource.__class__.__name__, self._ppformat(resource.name))
      has_arguments = False
      for k,v in resource.arguments.iteritems():
        has_arguments = True
        # correctly output octal mode numbers
        if k == 'mode' and isinstance( v, int ):
          val = oct(v)
        elif  isinstance( v, UnknownConfiguration):
          val = "UnknownConfigurationMock()"
        else:
          val = self._ppformat(v)
        # If value is multiline, format it
        if "\n" in val:
          lines = val.splitlines()
          firstLine = lines[0]
          nextlines = "\n".join(lines [1:])
          nextlines = self.reindent(nextlines, 2)
          val = "\n".join([firstLine, nextlines])
        param_str="{0} = {1},".format(k, val)
        s+="\n" + self.reindent(param_str, intendation)
      # Decide whether we want bracket to be at next line
      if has_arguments:
        before_bracket = "\n"
      else:
        before_bracket = ""
      # Add assertion
      s = "self.assertResourceCalled({0}{1})".format(s, before_bracket)
      # Intendation
      s = self.reindent(s, intendation)
      print s
    print(self.reindent("self.assertNoMoreResources()", intendation))
  
  def assertResourceCalled(self, resource_type, name, **kwargs):
    with patch.object(UnknownConfiguration, '__getattr__', return_value=lambda: "UnknownConfiguration()"): 
      self.assertNotEqual(len(RMFTestCase.env.resource_list), 0, "There was no more resources executed!")
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
    return Template(name, **kwargs)
  
def StaticFile(name, **kwargs):
  with RMFTestCase.env:
    from resource_management.core.source import StaticFile
    return StaticFile(name, **kwargs)
  
def InlineTemplate(name, **kwargs):
  with RMFTestCase.env:
    from resource_management.core.source import InlineTemplate
    return InlineTemplate(name, **kwargs)


class UnknownConfigurationMock():
  def __eq__(self, other):
    return isinstance(other, UnknownConfiguration)

  def __ne__(self, other):
    return not self.__eq__(other)
  
  def __repr__(self):
    return "UnknownConfigurationMock()"


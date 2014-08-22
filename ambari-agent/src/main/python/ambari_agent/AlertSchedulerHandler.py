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

'''
http://apscheduler.readthedocs.org/en/v2.1.2
'''
import json
import logging
import os
import sys
import time
import traceback
from apscheduler.scheduler import Scheduler
from alerts.collector import AlertCollector
from alerts.port_alert import PortAlert


logger = logging.getLogger()

class AlertSchedulerHandler():
  make_cachedir = True

  FILENAME = 'definitions.json'
  TYPE_PORT = 'PORT'

  def __init__(self, cachedir, in_minutes=True):
    self.cachedir = cachedir
    
    if not os.path.exists(cachedir) and AlertSchedulerHandler.make_cachedir:
      try:
        os.makedirs(cachedir)
      except:
        logger.critical("Could not create the cache directory {0}".format(cachedir))
        pass
    
    config = {
      'threadpool.core_threads': 3,
      'coalesce': True,
      'standalone': False
    }

    self.__scheduler = Scheduler(config)
    self.__in_minutes = in_minutes
    self.__loaded = False
    self.__collector = AlertCollector()
    self.__config_maps = {}
          
  def update_definitions(self, alert_commands, refresh_jobs=False):
    ''' updates the persisted definitions and restarts the scheduler '''
    
    with open(os.path.join(self.cachedir, self.FILENAME), 'w') as f:
      json.dump(alert_commands, f, indent=2)
    
    if refresh_jobs:
      self.__scheduler.shutdown(wait=False)
      self.__loaded = False
      self.start()
      
  def __make_function(self, alert_def):
    return lambda: alert_def.collect()
    
  def start(self):
    ''' loads definitions from file and starts the scheduler '''
    if not self.__loaded:
      alert_callables = self.__load_definitions()
      
      for _callable in alert_callables:
        if self.__in_minutes:
          self.__scheduler.add_interval_job(self.__make_function(_callable),
            minutes=_callable.interval())
        else:
          self.__scheduler.add_interval_job(self.__make_function(_callable),
            seconds=_callable.interval())
      self.__loaded = True
      
    if not self.__scheduler is None:
      self.__scheduler.start()
    
  def stop(self):
    if not self.__scheduler is None:
      self.__scheduler.shutdown(wait=False)
      self.__scheduler = None
      
  def collector(self):
    ''' gets the collector for reporting to the server '''
    return self.__collector
      
  def __load_definitions(self):
    ''' loads all alert commands from the file.  all clusters are stored in one file '''
    definitions = []
    
    all_commands = None
    try:
      with open(os.path.join(self.cachedir, self.FILENAME)) as fp:
        all_commands = json.load(fp)
    except:
      if (logger.isEnabledFor(logging.DEBUG)):
        traceback.print_exc()
      return definitions
    
    for command_json in all_commands:
      clusterName = '' if not 'clusterName' in command_json else command_json['clusterName']
      hostName = '' if not 'hostName' in command_json else command_json['hostName']

      configmap = None
      # each cluster gets a map of key/value pairs of substitution values
      self.__config_maps[clusterName] = {} 
      if 'configurations' in command_json:
        configmap = command_json['configurations']

      for definition in command_json['alertDefinitions']:
        obj = self.__json_to_callable(definition)
        if obj is None:
          continue
          
        obj.set_cluster(clusterName, hostName)

        # get the config values for the alerts 'lookup keys',
        # eg: hdfs-site/dfs.namenode.http-address : host_and_port        
        vals = self.__find_config_values(configmap, obj.get_lookup_keys())
        self.__config_maps[clusterName].update(vals)

        obj.set_helpers(self.__collector, self.__config_maps[clusterName])

        definitions.append(obj)
      
    return definitions

  def __json_to_callable(self, json_definition):
    '''
    converts the json that represents all aspects of a definition
    and makes an object that extends BaseAlert that is used for individual
    '''
    source = json_definition['source']
    source_type = source.get('type', '')

    if logger.isEnabledFor(logging.DEBUG):
      logger.debug("Creating job type {0} with {1}".format(source_type, str(json_definition)))
    
    alert = None

    if source_type == 'METRIC':
      pass
    elif source_type == 'PORT':
      alert = PortAlert(json_definition, source)
    elif type == 'SCRIPT':
      pass

    return alert
    
  def __find_config_values(self, configmap, obj_keylist):
    ''' finds templated values in the configuration map provided  by the server '''
    if configmap is None:
      return {}
    
    result = {}
    
    for key in obj_keylist:
      try:
        obj = configmap
        for layer in key.split('/'):
          obj = obj[layer]
        result[key] = obj
      except KeyError: # the nested key is missing somewhere
        pass
        
    return result
    
  def update_configurations(self, commands):
    '''
    when an execution command comes in, update any necessary values.
    status commands do not contain useful configurations
    '''
    for command in commands:
      clusterName = command['clusterName']
      if not clusterName in self.__config_maps:
        continue
        
      if 'configurations' in command:
        configmap = command['configurations']
        keylist = self.__config_maps[clusterName].keys()
        vals = self.__find_config_values(configmap, keylist)
        self.__config_maps[clusterName].update(vals)  

def main():
  args = list(sys.argv)
  del args[0]

  try:
    logger.setLevel(logger.debug)
  except TypeError:
    logger.setLevel(12)
    
  ash = AlertSchedulerHandler(args[0], False)
  ash.start()
  
  i = 0
  try:
    while i < 10:
      time.sleep(1)
      i += 1
  except KeyboardInterrupt:
    pass
    
  print str(ash.collector().alerts())
      
  ash.stop()
    
if __name__ == "__main__":
  main()
  
      

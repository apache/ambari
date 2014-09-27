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
from alerts.metric_alert import MetricAlert
from alerts.port_alert import PortAlert
from alerts.script_alert import ScriptAlert


logger = logging.getLogger()

class AlertSchedulerHandler():
  make_cachedir = True

  FILENAME = 'definitions.json'
  TYPE_PORT = 'PORT'
  TYPE_METRIC = 'METRIC'
  TYPE_SCRIPT = 'SCRIPT'

  APS_CONFIG = { 
    'threadpool.core_threads': 3,
    'coalesce': True,
    'standalone': False
  }

  def __init__(self, cachedir, stacks_dir, in_minutes=True):
    self.cachedir = cachedir
    self.stacks_dir = stacks_dir
    
    if not os.path.exists(cachedir) and AlertSchedulerHandler.make_cachedir:
      try:
        os.makedirs(cachedir)
      except:
        logger.critical("Could not create the cache directory {0}".format(cachedir))
        pass

    self._collector = AlertCollector()
    self.__scheduler = Scheduler(AlertSchedulerHandler.APS_CONFIG)
    self.__in_minutes = in_minutes
    self.__config_maps = {}

          
  def update_definitions(self, alert_commands, reschedule_jobs=False):
    ''' updates the persisted definitions and restarts the scheduler '''
    
    with open(os.path.join(self.cachedir, self.FILENAME), 'w') as f:
      json.dump(alert_commands, f, indent=2)
    
    if reschedule_jobs:
      self.reschedule()

      
  def __make_function(self, alert_def):
    return lambda: alert_def.collect()

    
  def start(self):
    ''' loads definitions from file and starts the scheduler '''

    if self.__scheduler is None:
      return

    if self.__scheduler.running:
      self.__scheduler.shutdown(wait=False)
      self.__scheduler = Scheduler(AlertSchedulerHandler.APS_CONFIG)

    alert_callables = self.__load_definitions()
      
    # schedule each definition
    for _callable in alert_callables:
      self.schedule_definition(_callable)
      
    logger.debug("Starting scheduler {0}; currently running: {1}".format(
      str(self.__scheduler), str(self.__scheduler.running)))

    self.__scheduler.start()

    
  def stop(self):
    if not self.__scheduler is None:
      self.__scheduler.shutdown(wait=False)
      self.__scheduler = Scheduler(AlertSchedulerHandler.APS_CONFIG)


  def reschedule(self):
    '''
    Removes jobs that are scheduled where their UUID no longer is valid. 
    Schedules jobs where the definition UUID is not currently scheduled.
    '''
    jobs_scheduled = 0
    jobs_removed = 0
    
    definitions = self.__load_definitions()
    scheduled_jobs = self.__scheduler.get_jobs()
    
    # for every scheduled job, see if its UUID is still valid
    for scheduled_job in scheduled_jobs:
      uuid_valid = False
      
      for definition in definitions:
        definition_uuid = definition.get_uuid()
        if scheduled_job.name == definition_uuid:
          uuid_valid = True
          break
      
      # jobs without valid UUIDs should be unscheduled
      if uuid_valid == False:
        jobs_removed += 1
        logger.info("Unscheduling {0}".format(scheduled_job.name))
        self._collector.remove_by_uuid(scheduled_job.name)
        self.__scheduler.unschedule_job(scheduled_job)
      
    # for every definition, determine if there is a scheduled job
    for definition in definitions:
      definition_scheduled = False
      for scheduled_job in scheduled_jobs:
        definition_uuid = definition.get_uuid()
        if definition_uuid == scheduled_job.name:
          definition_scheduled = True
          break
      
      # if no jobs are found with the definitions UUID, schedule it
      if definition_scheduled == False:
        jobs_scheduled += 1
        self.schedule_definition(definition)
  
    logger.info("Alert Reschedule Summary: {0} rescheduled, {1} unscheduled".format(
        str(jobs_scheduled), str(jobs_removed)))


  def collector(self):
    ''' gets the collector for reporting to the server '''
    return self._collector
  
      
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

        obj.set_helpers(self._collector, self.__config_maps[clusterName])

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

    if source_type == AlertSchedulerHandler.TYPE_METRIC:
      alert = MetricAlert(json_definition, source)
    elif source_type == AlertSchedulerHandler.TYPE_PORT:
      alert = PortAlert(json_definition, source)
    elif source_type == AlertSchedulerHandler.TYPE_SCRIPT:
      source['stacks_dir'] = self.stacks_dir
      alert = ScriptAlert(json_definition, source)

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
        

  def schedule_definition(self,definition):
    '''
    Schedule a definition (callable). Scheduled jobs are given the UUID
    as their name so that they can be identified later on.
    <p/>
    This function can be called with a definition that is disabled; it will
    simply NOOP.
    '''
    # NOOP if the definition is disabled; don't schedule it
    if definition.is_enabled() == False:
      logger.info("The alert {0} with UUID {1} is disabled and will not be scheduled".format(
          definition.get_name(),definition.get_uuid()))
      return
    
    job = None

    if self.__in_minutes:
      job = self.__scheduler.add_interval_job(self.__make_function(definition),
        minutes=definition.interval())
    else:
      job = self.__scheduler.add_interval_job(self.__make_function(definition),
        seconds=definition.interval())
    
    # although the documentation states that Job(kwargs) takes a name 
    # key/value pair, it does not actually set the name; do it manually
    if job is not None:
      job.name = definition.get_uuid()
      
    logger.info("Scheduling {0} with UUID {1}".format(
      definition.get_name(), definition.get_uuid()))
  
  def get_job_count(self):
    '''
    Gets the number of jobs currently scheduled. This is mainly used for
    test verification of scheduling
    '''
    if self.__scheduler is None:
      return 0
    
    return len(self.__scheduler.get_jobs())   

def main():
  args = list(sys.argv)
  del args[0]

  try:
    logger.setLevel(logging.DEBUG)
  except TypeError:
    logger.setLevel(12)
    
  ch = logging.StreamHandler()
  ch.setLevel(logger.level)
  logger.addHandler(ch)
    
  ash = AlertSchedulerHandler(args[0], args[1], False)
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
  
      

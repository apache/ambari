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
import glob
import json
import logging
import os
import sys
import time
from apscheduler.scheduler import Scheduler
from alerts.collector import AlertCollector
from alerts.port_alert import PortAlert


logger = logging.getLogger()

class AlertSchedulerHandler():
  make_cachedir = True

  def __init__(self, cachedir, in_minutes=True):
    self.cachedir = cachedir
    
    if not os.path.exists(cachedir) and AlertSchedulerHandler.make_cachedir:
      try:
        os.makedirs(cachedir)
      except:
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
          
  def update_definitions(self, alert_commands, refresh_jobs=False):
    for command in alert_commands:
      with open(os.path.join(self.cachedir, command['clusterName'] + '.def'), 'w') as f:
        json.dump(command, f, indent=2)
    
    if refresh_jobs:
      self.__scheduler.shutdown(wait=False)
      self.__loaded = False
      self.start()
      
  def __make_function(self, alert_def):
    return lambda: alert_def.collect()
    
  def start(self):
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
    return self.__collector
      
  def __load_definitions(self):
    definitions = []
    try:
      for deffile in glob.glob(os.path.join(self.cachedir, '*.def')):
        with open(deffile, 'r') as f:
          command_json = json.load(f)

          for definition in command_json['alertDefinitions']:
            obj = self.__json_to_callable(definition)
              
            if obj is not None:
              obj.set_cluster(
                '' if not 'clusterName' in command_json else command_json['clusterName'],
                '' if not 'hostName' in command_json else command_json['hostName'])

              definitions.append(obj)
      
    except:
      import traceback
      traceback.print_exc()
      pass
    return definitions

  def __json_to_callable(self, json_definition):
    source = json_definition['source']
    source_type = source.get('type', '')

    alert = None

    if source_type == 'METRIC':
      pass
    elif source_type == 'PORT':
      alert = PortAlert(self.__collector, json_definition, source)
    elif type == 'SCRIPT':
      pass

    return alert
    
  def __json_to_meta(self, json_definition):
    pass

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
  
      

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
from apscheduler.scheduler import Scheduler
from alerts.port_alert import PortAlert
import json
import logging
import sys
import time

logger = logging.getLogger()

class AlertSchedulerHandler():

  def __init__(self, filename, in_minutes=True):
    self.filename = filename
    
    config = {
      'threadpool.core_threads': 3,
      'coalesce': True,
      'standalone': False
    }

    self.scheduler = Scheduler(config)

    alert_callables = self.__load_alerts()

    for _callable in alert_callables:
      if in_minutes:
        self.scheduler.add_interval_job(self.__make_function(_callable),
          minutes=_callable.interval())
      else:
        self.scheduler.add_interval_job(self.__make_function(_callable),
          seconds=_callable.interval())
      
  def __make_function(self, alert_def):
    return lambda: alert_def.collect()

  def start(self):
    if not self.scheduler is None:
      self.scheduler.start()
    
  def stop(self):
    if not self.scheduler is None:
      self.scheduler.shutdown(wait=False)
      self.scheduler = None
      
  def __load_alerts(self):
    definitions = []
    try:
      # FIXME make location configurable
      with open(self.filename) as fp: 
        cluster_defs = json.load(fp)
        for deflist in cluster_defs.values():
          for definition in deflist:
            obj = self.__json_to_callable(definition)
            if obj is not None:
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
      alert = PortAlert(json_definition, source)
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
  ash.stop()
    
if __name__ == "__main__":
  main()
  
      

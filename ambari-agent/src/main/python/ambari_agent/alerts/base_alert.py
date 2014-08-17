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

import logging

logger = logging.getLogger()

class BaseAlert(object):
  RESULT_OK = 'OK'
  RESULT_WARNING = 'WARNING'
  RESULT_CRITICAL = 'CRITICAL'
  RESULT_UNKNOWN = 'UNKNOWN'
  
  def __init__(self, alert_meta, alert_source_meta):
    self.alert_meta = alert_meta
    self.alert_source_meta = alert_source_meta
    
  def interval(self):
    if not self.alert_meta.has_key('interval'):
      return 1
    else:
      return self.alert_meta['interval']
  
  def collect(self):
    res = (BaseAlert.RESULT_UNKNOWN, [])
    try:
      res = self._collect()
    except Exception as e:
      res = (BaseAlert.RESULT_CRITICAL, [str(e)])

    res_base_text = self.alert_source_meta['reporting'][res[0].lower()]['text']
      
    data = {}
    data['name'] = self._find_value('name')
    data['state'] = res[0]
    data['text'] = res_base_text.format(*res[1])
    # data['cluster'] = self._find_value('cluster') # not sure how to get this yet
    data['service'] = self._find_value('service')
    data['component'] = self._find_value('component')
    
    print str(data)
  
  def _find_value(self, meta_key):
    if self.alert_meta.has_key(meta_key):
      return self.alert_meta[meta_key]
    else:
      return None
  
  def _collect(self):
    '''
    Low level function to collect alert data.  The result is a tuple as:
    res[0] = the result code
    res[1] = the list of arguments supplied to the reporting text for the result code
    '''  
    raise NotImplementedError
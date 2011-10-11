#!/usr/bin/env python2.6

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
import logging.handlers
import Queue
import ActionQueue

logger = logging.getLogger()

class ActionResults:
  global r

  # Build action results list from memory queue
  def build(self):
    results = []
    while not ActionQueue.r.empty():
      result = { 
                 'clusterId': 'unknown',
                 'id' : 'action-001',
                 'kind' : 'STOP_ACTION',
                 'commandResults' : [],
                 'cleanUpCommandResults' : [],
                 'serverName' : 'hadoop.datanode'
               }
      results.append(result)
    logger.info(results)
    return results

def main(argv=None):
  ar = ActionResults()
  print ar.build()

if __name__ == '__main__':
  main()

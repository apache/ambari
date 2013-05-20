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

import optparse
import urllib2, urllib
import json

RESOURCEMANAGER = 'rm'
HISTORYSERVER ='hs'

STARTED_STATE = 'STARTED'

def validate(component, path, port):

  try:
    url = 'http://localhost:' + str(port) + path
    opener = urllib2.build_opener()
    urllib2.install_opener(opener)
    request = urllib2.Request(url)
    handler = urllib2.urlopen(request)
    response = json.loads(handler.read())
    is_valid = validateResponse(component, response)
    if is_valid:
      exit(0)
    else:
      exit(1)
  except Exception as e:
    print 'Error checking status of component', e
    exit(1)


def validateResponse(component, response):
  try:
    if component == RESOURCEMANAGER:
      rm_state = response['clusterInfo']['state']
      if rm_state == STARTED_STATE:
        return True
      else:
        return False
    elif component == HISTORYSERVER:
      hs_start_time = response['historyInfo']['startedOn']
      if hs_start_time > 0:
        return True
      else:
        return False
    else:
      return False
  except Exception as e:
    print 'Error validation of response', e
    return False

#
# Main.
#
def main():
  parser = optparse.OptionParser(usage="usage: %prog [options] component ")
  parser.add_option("-p", "--port", dest="port", help="Port for rest api of desired component")


  (options, args) = parser.parse_args()

  component = args[0]
  
  port = options.port
  
  if component == RESOURCEMANAGER:
    path = '/ws/v1/cluster/info'
  elif component == HISTORYSERVER:
    path = '/ws/v1/history/info'
  else:
    parser.error("Invalid component")

  validate(component, path, port)

if __name__ == "__main__":
  main()

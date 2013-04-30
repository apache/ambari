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
NODEMANAGER = 'nm'

STARTED_STATE = 'STARTED'

def validate(path, port):

  try:
    url = 'http://localhost:' + str(port) + path
    opener = urllib2.build_opener()
    urllib2.install_opener(opener)
    request = urllib2.Request(url)
    handler = urllib2.urlopen(request)
    cluster_info = json.loads(handler.read())
    component_state = cluster_info['clusterInfo']['state']
  
    if component_state == STARTED_STATE:
      print 'Component''s state is: ' + str(component_state)
      exit(0)
    else:
      print 'Component''s state is not' + STARTED_STATE
      exit(1)
  except Exception as e:
    print 'Error checking status of component', e
    exit(1)

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
  elif component == NODEMANAGER:
    path = '/ws/v1/node/info'
  else:
    parser.error("Invalid component")

  validate(path, port)

if __name__ == "__main__":
  main()

#!/usr/bin/env python
#
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
#

import os
import optparse
import json
import traceback

def main():

  parser = optparse.OptionParser()

  parser.add_option("-H", "--host", dest="host", default="localhost", help="NameNode host")
  parser.add_option("-n", "--name", dest="alert_name", help="Alert name to check")
  parser.add_option("-f", "--file", dest="alert_file", help="File containing the alert structure")

  (options, args) = parser.parse_args()

  if options.alert_name is None:
    print "Alert name is required (--name or -n)"
    exit(-1)

  if options.alert_file is None:
    print "Alert file is required (--file or -f)"
    exit(-1)

  if not os.path.exists(options.alert_file):
    print "Status is unreported"
    exit(3)

  try:
    with open(options.alert_file, 'r') as f:
      data = json.load(f)

      found = False
      buf = ''

      for_hosts = data[options.alert_name]
      if for_hosts.has_key(options.host):
        for host_entry in for_hosts[options.host]:
          alert_state = host_entry['state']
          alert_text = host_entry['text']
          if alert_state == 'CRITICAL':
            print str(alert_text)
            exit(2)
          elif alert_state == 'WARNING':
            print str(alert_text)
            exit(1)
          else:
            if found:
              buf = buf + ', '
            buf = buf + alert_text
            found = True

      if not found:
        print "Status is not reported"
        exit(3)
      else:
        print buf
        exit(0)
      
  except Exception:
    traceback.print_exc()
    exit(3)

if __name__ == "__main__":
  main()


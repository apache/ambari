#!/usr/bin/env python
"""
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

Ambari Agent

"""

import json
import sys
#import traceback
from resource_management import *


class NagiosIgnore(Script):
  def actionexecute(self, env):
    config = Script.get_config()

    ignores = None

    structured_output_example = {
      'result': 'Ignore table updated.'
    }

    try:
      if (config.has_key('passiveInfo')):
        ignores = config['passiveInfo']
      else:
        structured_output_example['result'] = "Key 'passiveInfo' not found, skipping"
        Logger.info("Key 'passiveInfo' was not found, skipping")
        self.put_structured_out(structured_output_example)
    except Exception:
      structured_output_example['result'] = "Error accessing passiveInfo"
      self.put_structured_out(structured_output_example)
      Logger.debug("Error accessing passiveInfo")
      return

    if ignores is None:
      Logger.info("Nothing to do - maintenance info was not provided")
      return
    
    new_file_entries = []

    if ignores is not None:
      for define in ignores:
        try:
          host = str(define['host'])
          service = str(define['service'])
          component = str(define['component'])
          key = host + " " + service + " " + component
          Logger.info("found entry for host=" + host +
            ", service=" + service +
            ", component=" + component)

          new_file_entries.append(key)
        except KeyError:
          Logger.debug("Could not load host, service, or component for " + str(define))
          pass

    writeFile(new_file_entries)

    self.put_structured_out(structured_output_example)

def writeFile(entries):
  buf = ""
  for entry in entries:
    buf += entry + "\n"

  f = None
  try:
    f = open('/var/nagios/ignore.dat', 'w')
    f.write(buf)
    if 0 == len(entries):
      Logger.info("Cleared all entries from '/var/nagios/ignore.dat'")
    elif 1 == len(entries):
      Logger.info("Persisted '/var/nagios/ignore.dat' with 1 entry")
    else:
      Logger.info("Persisted '/var/nagios/ignore.dat' with " + str(len(entries)) + " entries")
  except:
    Logger.info("Could not open '/var/nagios/ignore.dat' to update")
    pass
  finally:
    if f is not None:
      f.close()

if __name__ == "__main__":
  NagiosIgnore().execute()

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
        self.put_structured_out(structured_output_example)
    except Exception:
      structured_output_example['result'] = "Error accessing passiveInfo"
      self.put_structured_out(structured_output_example)
      return

    if ignores is None:
      return
    
    new_file_entries = []

    if ignores is not None:
      for define in ignores:
        try:
          host = str(define['host'])
          service = str(define['service'])
          component = str(define['component'])
          key = host + " " + service + " " + component

          new_file_entries.append(key)
        except KeyError:
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
  except:
    pass
  finally:
    if f is not None:
      f.close()

if __name__ == "__main__":
  NagiosIgnore().execute()

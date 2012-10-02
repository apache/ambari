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

import optparse
import argparse
import sys
import subprocess

SETUP_ACTION = "setup"
START_ACTION = "start"
STOP_ACTION = "stop"
SETUP_CMD = "sudo -u postgres psql -f setup_db.sql"


def setup():
  command = SETUP_CMD
  process = subprocess.Popen(command.split(' '),
                             stdout=subprocess.PIPE,
                             stderr=subprocess.PIPE)
  process.communicate()

def main():
  parser = argparse.ArgumentParser()
  parser.add_argument("action", help="action to perform with ambari server")
  parser.add_argument('-postgreuser', '--postgreuser', default='root',
                      help="User in postgresql to run init scripts")
  parser.add_argument('-postgrepass', '--postgrepass')
  args = parser.parse_args()

  action = args.action
  if action == SETUP_ACTION:
    setup()
  elif action == START_ACTION:
    print START_ACTION
  elif action == STOP_ACTION:
    print STOP_ACTION
  else:
    print "Incorrect action"
    sys.exit(2)
    

    
    
if __name__ == "__main__":
  main()
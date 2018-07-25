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
"""

import os
import sys

AMBARI_AGENT_HOST_DIR = "AMBARI_AGENT_HOST_DIR"

SYMLINKS_TXT = "symlinks.txt"
VERSIONS_TXT = "versions.txt"

# main method to parse arguments from user and start work
def main():
  if len(sys.argv) <= 1:
    sys.exit(-1)

  args = sys.argv[1:]

  do_work(args)


def extrakt_var_from_pythonpath(name):
  PATH = os.environ['PATH']
  paths = PATH.split(':')
  var = ''
  for item in paths:
    if item.startswith(name):
      var = item.replace(name, '')
      break
  return var


def print_versions(args):
  dest = versions_file_destination()

  with open(dest, 'r') as f:
    for line in f:
      print line


def versions_file_destination():
  agent_host_dir = extrakt_var_from_pythonpath(AMBARI_AGENT_HOST_DIR)
  dest = os.path.join(agent_host_dir, VERSIONS_TXT)
  if not os.path.exists(dest):
    open(dest, 'w').close()
  return dest


def print_status(args):
  dest = symlinks_file_destination()

  with open(dest, 'r') as f:
    if len(args) >= 2:
      for line in f:
        if args[1] in line:
          print line
          pass

    for line in f:
      print line


def set_version(args):
  dest = symlinks_file_destination()

  line_template = "{0} - {1}\n"
  result = ""
  with open(dest, 'r') as f:

    if len(args) >= 3:
      if args[1] != "all":
        seted = False
        for line in f:
          if args[1] in line:
            compinfo = str.split(line)
            result += line_template.format(compinfo[0], args[2])
            seted = True
          else:
            result += line
        if seted != True:
          result += line_template.format(args[1], args[2])
      else:
        for line in f:
          compinfo = str.split(line)
          result += line_template.format(compinfo[0], args[2])

  with open(dest, 'w') as f:
    f.write(result)


def symlinks_file_destination():
  agent_host_dir = extrakt_var_from_pythonpath(AMBARI_AGENT_HOST_DIR)
  dest = os.path.join(agent_host_dir, SYMLINKS_TXT)
  if not os.path.exists(dest):
    open(dest, 'w').close()
  return dest


def install_version(args):
  dest = versions_file_destination()
  installed = False
  with open(dest, 'r') as f:
    for line in f:
      if args[1] in line:
        installed = True
        break
  if not installed:
    with open(dest, 'a') as f:
      if args[1]:
        f.write(args[1] + "\n")

def deploy_cluster(args):
  dest = versions_file_destination()
  with open(dest, 'w') as f:
    if args[1]:
      f.write(args[1] + "\n")

def do_work(args):
  """
  Check that all required args are passed in. If so, perform required action.
  :param args:
  """
  if not args[0] or args[0] == "status":
    print_status(args)
  elif args[0] == "versions":
    print_versions(args)
  elif args[0] == "set":
    set_version(args)
  elif args[0] == "install":
    install_version(args)
  elif args[0] == "deploy_cluster":
    deploy_cluster(args)



if __name__ == "__main__":
  main()
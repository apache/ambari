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

import os
import sys
import subprocess
import signal
from Controller import AGENT_AUTO_RESTART_EXIT_CODE

if os.environ.has_key("PYTHON_BIN"):
  AGENT_SCRIPT = os.path.join(os.environ["PYTHON_BIN"],"site-packages/ambari_agent/main.py")
else:
  AGENT_SCRIPT = "/usr/lib/python2.6/site-packages/ambari_agent/main.py"
if os.environ.has_key("AMBARI_PID_DIR"):
  AGENT_PID_FILE = os.path.join(os.environ["AMBARI_PID_DIR"],"ambari-agent.pid")
else:
  AGENT_PID_FILE = "/var/run/ambari-agent/ambari-agent.pid"
# AGENT_AUTO_RESTART_EXIT_CODE = 77 is exit code which we return when restart_agent() is called
status = AGENT_AUTO_RESTART_EXIT_CODE

def main():
  global status

  if (os.environ.has_key("PYTHON")):
    PYTHON = os.environ["PYTHON"]
  else:
    print("Key 'PYTHON' is not defined in environment variables")
    sys.exit(1)

  args = list(sys.argv)
  del args[0]

  mergedArgs = [PYTHON, AGENT_SCRIPT] + args

  try:
    while status == AGENT_AUTO_RESTART_EXIT_CODE:
      mainProcess = subprocess.Popen(mergedArgs)
      mainProcess.communicate()
      status = mainProcess.returncode
      if os.path.isfile(AGENT_PID_FILE) and status == AGENT_AUTO_RESTART_EXIT_CODE:
        os.remove(AGENT_PID_FILE)
  finally:
    os.killpg(0, signal.SIGKILL)

if __name__ == "__main__":
    main()
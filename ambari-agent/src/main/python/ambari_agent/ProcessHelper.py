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

import os
import logging
import traceback
from shell import getTempFiles

logger = logging.getLogger()


if 'AMBARI_PID_DIR' in os.environ:
    pidfile = os.environ['AMBARI_PID_DIR'] + "/ambari-agent.pid"
else:
    pidfile = "/var/run/ambari-agent/ambari-agent.pid"


def stopAgent():
  try:
    os.unlink(pidfile)
  except Exception:
    logger.warn("Unable to remove: "+pidfile)
    traceback.print_exc()

  tempFiles = getTempFiles()
  for tempFile in tempFiles:
    if os.path.exists(tempFile):
      try:
          os.unlink(tempFile)
      except Exception:
          traceback.print_exc()
          logger.warn("Unable to remove: "+tempFile)
  os._exit(0)
  pass
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
import logging
import traceback
import sys
from ambari_commons.shell import getTempFiles

logger = logging.getLogger()

if 'AMBARI_PID_DIR' in os.environ:
    piddir = os.environ['AMBARI_PID_DIR']
else:
    piddir = "/var/run/ambari-agent"

pidfile = os.path.join(piddir, "ambari-agent.pid")


def _clean():
  logger.info("Removing pid file")
  try:
    os.unlink(pidfile)
  except Exception as ex:
    traceback.print_exc()
    logger.warn("Unable to remove pid file: %s", ex)

  logger.info("Removing temp files")
  for f in getTempFiles():
    if os.path.exists(f):
      try:
        os.unlink(f)
      except Exception as ex:
        traceback.print_exc()
        logger.warn("Unable to remove: %s, %s", f, ex)


def stopAgent():
  _clean()
  sys.exit(0)


def restartAgent():
  _clean()

  executable = sys.executable
  args = sys.argv[:]
  args.insert(0, executable)

  logger.info("Restarting self: %s %s", executable, args)

  os.execvp(executable, args)



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

import logging
import os
import sys
import signal
from ambari_commons.os_utils import remove_file

from core.controller import Controller
from core.config_reader import Configuration, PID_OUT_FILE, SERVER_LOG_FILE, SERVER_OUT_FILE
from core.stop_handler import bind_signal_handlers


logger = logging.getLogger()


def save_pid(pid, pidfile):
  """
    Save pid to pidfile.
  """
  try:
    pfile = open(pidfile, "w")
    pfile.write("%s\n" % pid)
  except IOError:
    pass
  finally:
    try:
      pfile.close()
    except:
      pass


def main(argv=None):
  # Allow Ctrl-C
  stop_handler = bind_signal_handlers()

  server_process_main(stop_handler)

def server_process_main(stop_handler, scmStatus=None):
  if scmStatus is not None:
    scmStatus.reportStartPending()

  config = Configuration()
  _init_logging(config)
  controller = Controller(config, stop_handler)

  logger.info('Starting Server RPC Thread: %s' % ' '.join(sys.argv))
  controller.start()

  print "Server out at: " + SERVER_OUT_FILE
  print "Server log at: " + SERVER_LOG_FILE

  save_pid(os.getpid(), PID_OUT_FILE)

  if scmStatus is not None:
    scmStatus.reportStarted()

  # For some reason this is needed to catch system signals like SIGTERM
  # TODO fix if possible
  signal.pause()

  #The controller thread finishes when the stop event is signaled
  controller.join()

  remove_file(PID_OUT_FILE)
  pass

def _init_logging(config):
  _levels = {
          'DEBUG': logging.DEBUG,
          'INFO': logging.INFO,
          'WARNING': logging.WARNING,
          'ERROR': logging.ERROR,
          'CRITICAL': logging.CRITICAL,
          'NOTSET' : logging.NOTSET
          }
  level = logging.INFO
  if config.get_log_level() in _levels:
    level = _levels.get(config.get_log_level())
  logger.setLevel(level)
  formatter = logging.Formatter("%(asctime)s [%(levelname)s] %(filename)s:%(lineno)d - %(message)s")
  stream_handler = logging.StreamHandler()
  stream_handler.setFormatter(formatter)
  logger.addHandler(stream_handler)
  

if __name__ == '__main__':
  main()


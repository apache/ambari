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
import logging.handlers
import code
import signal
import sys, traceback
import os
import time
import ConfigParser
from createDaemon import createDaemon
from Controller import Controller

logger = logging.getLogger()

if 'AMBARI_PID_DIR' in os.environ:
  pidfile = os.environ['AMBARI_PID_DIR'] + "/ambari-agent.pid"
else:
  pidfile = "/var/run/ambari/ambari-agent.pid"

if 'AMBARI_LOG_DIR' in os.environ:
  logfile = os.environ['AMBARI_LOG_DIR'] + "/ambari-agent.log"
else:
  logfile = "/var/log/ambari/ambari-agent.log"

def signal_handler(signum, frame):
  logger.info('signal received, exiting.')
  os.unlink(pidfile)
  os._exit(0)

def debug(sig, frame):
    """Interrupt running process, and provide a python prompt for
    interactive debugging."""
    d={'_frame':frame}         # Allow access to frame object.
    d.update(frame.f_globals)  # Unless shadowed by global
    d.update(frame.f_locals)

    message  = "Signal recieved : entering python shell.\nTraceback:\n"
    message += ''.join(traceback.format_stack(frame))
    logger.info(message)
      
def main():
  signal.signal(signal.SIGINT, signal_handler)
  signal.signal(signal.SIGTERM, signal_handler)
  signal.signal(signal.SIGUSR1, debug)
  if (len(sys.argv) >1) and sys.argv[1]=='stop':
    # stop existing Ambari agent
    try:
      f = open(pidfile, 'r')
      pid = f.read()
      pid = int(pid)
      f.close()
      os.kill(pid, signal.SIGTERM)
      time.sleep(5)
      if os.path.exists(pidfile):
        raise Exception("PID file still exists.")
      os._exit(0)
    except Exception, err:
      os.kill(pid, signal.SIGKILL)
      os._exit(1)

  # Check if there is another instance running
  if os.path.isfile(pidfile):
    print("%s already exists, exiting" % pidfile)
    sys.exit(1)
  else:
    # Daemonize current instance of Ambari Agent
    retCode = createDaemon()
    pid = str(os.getpid())
    file(pidfile, 'w').write(pid)

  logger.setLevel(logging.DEBUG)
  formatter = logging.Formatter("%(asctime)s %(filename)s:%(lineno)d - %(message)s")
  rotateLog = logging.handlers.RotatingFileHandler(logfile, "a", 10000000, 10)
  rotateLog.setFormatter(formatter)
  logger.addHandler(rotateLog)
  credential = None

  # Check for ambari configuration file.
  if(os.path.exists('/etc/ambari/ambari.ini')):
    config = ConfigParser.RawConfigParser()
    config.read('/etc/ambari/ambari.ini')
    try:
      credential = config.get('controller', 'user')+":"+config.get('controller', 'password')
      controllerUrl = config.get('controller', 'url')
    except Exception, err:
      credential = None
      controllerUrl = "http://localhost:4080"
  else:
    credential = None
    controllerUrl = "http://localhost:4080"
  logger.info("Connecting to controller at:"+controllerUrl)

  # Launch Controller communication
  controller = Controller(controllerUrl) 
  controller.start()
  controller.run()
  logger.info("finished")
    
if __name__ == "__main__":
  main()

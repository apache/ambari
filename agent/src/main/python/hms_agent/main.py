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
from mimerender import mimerender
import mimeparse
from Runner import Runner
import code
import signal
import simplejson
import sys, traceback
import web
import os
import time
import ConfigParser
from PackageHandler import PackageHandler
from DaemonHandler import DaemonHandler
from ShellHandler import ShellHandler
from ZooKeeperCommunicator import ZooKeeperCommunicator
from createDaemon import createDaemon
from Zeroconf import dnsResolver

logger = logging.getLogger()

urls = (
    '/package/info/(.*)', 'PackageHandler',
    '/package/(.*)', 'PackageHandler',
    '/daemon/status/(.*)', 'DaemonHandler',
    '/daemon/(.*)', 'DaemonHandler',
    '/shell/(.*)', 'ShellHandler'
)
app = web.application(urls, globals())

if 'HMS_PID_DIR' in os.environ:
  pidfile = os.environ['HMS_PID_DIR'] + "/hms-agent.pid"
else:
  pidfile = "/var/run/hms/hms-agent.pid"    

if 'HMS_LOG_DIR' in os.environ:
  logfile = os.environ['HMS_LOG_DIR'] + "/hms-agent.log"
else:
  logfile = "/var/log/hms/hms-agent.log"

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
      traceback.print_exc(file=sys.stdout)
      os._exit(1)
  if os.path.isfile(pidfile):
    print("%s already exists, exiting" % pidfile)
    sys.exit(1)
  else:
    retCode = createDaemon()
    pid = str(os.getpid())
    file(pidfile, 'w').write(pid)
  logger.setLevel(logging.DEBUG)
  formatter = logging.Formatter("%(asctime)s %(filename)s:%(lineno)d - %(message)s")
  rotateLog = logging.handlers.RotatingFileHandler(logfile, "a", 10000000, 10)
  rotateLog.setFormatter(formatter)
  logger.addHandler(rotateLog)
  zeroconf = dnsResolver()
  credential = None
  if(os.path.exists('/etc/hms/hms.ini')):
    config = ConfigParser.RawConfigParser()
    config.read('/etc/hms/hms.ini')
    zkservers = config.get('zookeeper', 'quorum')
    try:
      credential = config.get('zookeeper', 'user')+":"+config.get('zookeeper', 'password')
    except Exception, err:
      credential = None
  else:
    zkservers = ""
  while zkservers=="":
    zkservers = zeroconf.find('_zookeeper._tcp')
    if zkservers=="":
      logger.warn("Unable to locate zookeeper, sleeping 30 seconds")
      loop = 0
      while loop < 10:
        time.sleep(3)
        loop = loop + 1
  logger.info("Connecting to "+zkservers+".")
  zc = ZooKeeperCommunicator(zkservers, credential)
  zc.start()
  zc.run()
    
if __name__ == "__main__":
  main()

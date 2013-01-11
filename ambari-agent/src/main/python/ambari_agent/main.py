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

import logging
import logging.handlers
import code
import signal
from optparse import OptionParser
import sys, traceback
import os
import time
import ConfigParser
import ProcessHelper
from createDaemon import createDaemon
from Controller import Controller
from shell import killstaleprocesses
import AmbariConfig
from security import CertificateManager
from NetUtil import NetUtil

logger = logging.getLogger()
agentPid = os.getpid()

if 'AMBARI_LOG_DIR' in os.environ:
  logfile = os.environ['AMBARI_LOG_DIR'] + "/ambari-agent.log"
else:
  logfile = "/var/log/ambari-agent/ambari-agent.log"

def signal_handler(signum, frame):
  #we want the handler to run only for the agent process and not
  #for the children (e.g. namenode, etc.)
  if (os.getpid() != agentPid):
    os._exit(0)
  logger.info('signal received, exiting.')
  ProcessHelper.stopAgent()

def debug(sig, frame):
    """Interrupt running process, and provide a python prompt for
    interactive debugging."""
    d={'_frame':frame}         # Allow access to frame object.
    d.update(frame.f_globals)  # Unless shadowed by global
    d.update(frame.f_locals)

    message  = "Signal received : entering python shell.\nTraceback:\n"
    message += ''.join(traceback.format_stack(frame))
    logger.info(message)




def main():
  global config
  parser = OptionParser()
  parser.add_option("-v", "--verbose", dest="verbose", action="store_true", help="verbose log output", default=False)
  (options, args) = parser.parse_args()

  formatstr = "%(levelname)s %(asctime)s %(filename)s:%(lineno)d - %(message)s"
  formatter = logging.Formatter(formatstr)
  rotateLog = logging.handlers.RotatingFileHandler(logfile, "a", 10000000, 25)
  rotateLog.setFormatter(formatter)
  logger.addHandler(rotateLog)

  if options.verbose:
    logging.basicConfig(format=formatstr, level=logging.DEBUG, filename=logfile)
    logger.setLevel(logging.DEBUG)
  else:
    logging.basicConfig(format=formatstr, level=logging.INFO, filename=logfile)
    logger.setLevel(logging.INFO)

  logger.debug("loglevel=logging.DEBUG")

  default_cfg = { 'agent' : { 'prefix' : '/home/ambari' } }
  config = ConfigParser.RawConfigParser(default_cfg)
  signal.signal(signal.SIGINT, signal_handler)
  signal.signal(signal.SIGTERM, signal_handler)
  signal.signal(signal.SIGUSR1, debug)
  if (len(sys.argv) >1) and sys.argv[1]=='stop':
    # stop existing Ambari agent
    pid = -1
    try:
      f = open(ProcessHelper.pidfile, 'r')
      pid = f.read()
      pid = int(pid)
      f.close()
      os.kill(pid, signal.SIGTERM)
      time.sleep(5)
      if os.path.exists(ProcessHelper.pidfile):
        raise Exception("PID file still exists.")
      os._exit(0)
    except Exception, err:
      if pid == -1:
        print ("Agent process is not running")
      else:
        os.kill(pid, signal.SIGKILL)
      os._exit(1)

  # Check if there is another instance running
  if os.path.isfile(ProcessHelper.pidfile):
    print("%s already exists, exiting" % ProcessHelper.pidfile)
    sys.exit(1)
  else:
    # Daemonize current instance of Ambari Agent
    #retCode = createDaemon()
    pid = str(os.getpid())
    file(ProcessHelper.pidfile, 'w').write(pid)

  credential = None

  # Check for ambari configuration file.
  try:
    config = AmbariConfig.config
    if os.path.exists('/etc/ambari-agent/conf/ambari-agent.ini'):
      config.read('/etc/ambari-agent/conf/ambari-agent.ini')
      AmbariConfig.setConfig(config)
    else:
      raise Exception("No config found, use default")
  except Exception, err:
    logger.warn(err)

  killstaleprocesses()

  server_url = 'https://' + config.get('server', 'hostname') + ':' + config.get('server', 'url_port')
  print("Connecting to the server at " + server_url + "...")
  logger.info('Connecting to the server at: ' + server_url)

  # Wait until server is reachable
  netutil = NetUtil()
  netutil.try_to_connect(server_url, -1, logger)

  #Initiate security
  """ Check if security is enable if not then disable it"""
  logger.info("Creating certs")
  certMan = CertificateManager(config)
  certMan.initSecurity()
  
  # Launch Controller communication
  controller = Controller(config)
  controller.start()
  # TODO: is run() call necessary?
  controller.run()
  logger.info("finished")
    
if __name__ == "__main__":
  main()

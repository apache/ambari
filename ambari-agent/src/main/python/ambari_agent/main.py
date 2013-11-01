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
import signal
from optparse import OptionParser
import sys
import traceback
import os
import time
import ConfigParser
import ProcessHelper
from Controller import Controller
import AmbariConfig
from security import CertificateManager
from NetUtil import NetUtil
from PingPortListener import PingPortListener
import security
import hostname
from DataCleaner import DataCleaner

logger = logging.getLogger()
formatstr = "%(levelname)s %(asctime)s %(filename)s:%(lineno)d - %(message)s"
agentPid = os.getpid()
configFile = "/etc/ambari-agent/conf/ambari-agent.ini"

if 'AMBARI_LOG_DIR' in os.environ:
  logfile = os.environ['AMBARI_LOG_DIR'] + "/ambari-agent.log"
else:
  logfile = "/var/log/ambari-agent/ambari-agent.log"

def signal_handler(signum, frame):
  #we want the handler to run only for the agent process and not
  #for the children (e.g. namenode, etc.)
  if os.getpid() != agentPid:
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


def setup_logging(verbose):
  formatter = logging.Formatter(formatstr)
  rotateLog = logging.handlers.RotatingFileHandler(logfile, "a", 10000000, 25)
  rotateLog.setFormatter(formatter)
  logger.addHandler(rotateLog)

  if verbose:
    logging.basicConfig(format=formatstr, level=logging.DEBUG, filename=logfile)
    logger.setLevel(logging.DEBUG)
    logger.info("loglevel=logging.DEBUG")
  else:
    logging.basicConfig(format=formatstr, level=logging.INFO, filename=logfile)
    logger.setLevel(logging.INFO)
    logger.info("loglevel=logging.INFO")


def update_log_level(config):
  # Setting loglevel based on config file
  try:
    loglevel = config.get('agent', 'loglevel')
    if loglevel is not None:
      if loglevel == 'DEBUG':
        logging.basicConfig(format=formatstr, level=logging.DEBUG, filename=logfile)
        logger.setLevel(logging.DEBUG)
        logger.info("Newloglevel=logging.DEBUG")
      else:
        logging.basicConfig(format=formatstr, level=logging.INFO, filename=logfile)
        logger.setLevel(logging.INFO)
        logger.debug("Newloglevel=logging.INFO")
  except Exception, err:
    logger.info("Default loglevel=DEBUG")


def bind_signal_handlers():
  signal.signal(signal.SIGINT, signal_handler)
  signal.signal(signal.SIGTERM, signal_handler)
  signal.signal(signal.SIGUSR1, debug)


def resolve_ambari_config():
  try:
    config = AmbariConfig.config
    if os.path.exists(configFile):
      config.read(configFile)
      AmbariConfig.setConfig(config)
    else:
      raise Exception("No config found, use default")

  except Exception, err:
    logger.warn(err)
  return config


def perform_prestart_checks(expected_hostname):
  # Check if current hostname is equal to expected one (got from the server
  # during bootstrap.
  if expected_hostname is not None:
    current_hostname = hostname.hostname()
    if current_hostname != expected_hostname:
      print("Determined hostname does not match expected. Please check agent "
            "log for details")
      msg = "Ambari agent machine hostname ({0}) does not match expected ambari " \
            "server hostname ({1}). Aborting registration. Please check hostname, " \
            "hostname -f and /etc/hosts file to confirm your " \
            "hostname is setup correctly".format(current_hostname, expected_hostname)
      logger.error(msg)
      sys.exit(1)
  # Check if there is another instance running
  if os.path.isfile(ProcessHelper.pidfile):
    print("%s already exists, exiting" % ProcessHelper.pidfile)
    sys.exit(1)
  # check if ambari prefix exists
  elif not os.path.isdir(config.get("agent", "prefix")):
    msg = "Ambari prefix dir %s does not exists, can't continue" \
          % config.get("agent", "prefix")
    logger.error(msg)
    print(msg)
    sys.exit(1)


def daemonize():
  # Daemonize current instance of Ambari Agent
  # Currently daemonization is done via /usr/sbin/ambari-agent script (nohup)
  # and agent only dumps self pid to file
  pid = str(os.getpid())
  file(ProcessHelper.pidfile, 'w').write(pid)


def stop_agent():
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


def main():
  global config
  parser = OptionParser()
  parser.add_option("-v", "--verbose", dest="verbose", action="store_true", help="verbose log output", default=False)
  parser.add_option("-e", "--expected-hostname", dest="expected_hostname", action="store",
                    help="expected hostname of current host. If hostname differs, agent will fail", default=None)
  (options, args) = parser.parse_args()

  expected_hostname = options.expected_hostname

  setup_logging(options.verbose)

  default_cfg = { 'agent' : { 'prefix' : '/home/ambari' } }
  config = ConfigParser.RawConfigParser(default_cfg)
  bind_signal_handlers()

  if (len(sys.argv) >1) and sys.argv[1]=='stop':
    stop_agent()

  # Check for ambari configuration file.
  config = resolve_ambari_config()

  # Starting data cleanup daemon
  data_cleaner = None
  if int(config.get('agent','data_cleanup_interval')) > 0:
    data_cleaner = DataCleaner(config)
    data_cleaner.start()

  perform_prestart_checks(expected_hostname)
  daemonize()

  # Starting ping port listener
  ping_port_listener = PingPortListener(config)
  ping_port_listener.start()

  update_log_level(config)

  server_url = 'https://' + config.get('server', 'hostname') + ':' + config.get('server', 'url_port')
  print("Connecting to the server at " + server_url + "...")
  logger.info('Connecting to the server at: ' + server_url)

  # Wait until server is reachable
  netutil = NetUtil()
  netutil.try_to_connect(server_url, -1, logger)

  # Launch Controller communication
  controller = Controller(config)
  controller.start()
  controller.join()
  stop_agent()
  logger.info("finished")

if __name__ == "__main__":
  main()

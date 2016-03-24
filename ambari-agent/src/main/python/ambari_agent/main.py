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

import logging.handlers
import logging.config
import signal
from optparse import OptionParser
import sys
import traceback
import getpass
import os
import time
import platform
import ConfigParser
import ProcessHelper
from logging.handlers import SysLogHandler
from Controller import Controller
import AmbariConfig
from NetUtil import NetUtil
from PingPortListener import PingPortListener
import hostname
from DataCleaner import DataCleaner
from ExitHelper import ExitHelper
import socket
from ambari_commons import OSConst, OSCheck
from ambari_commons.shell import shellRunner
from ambari_commons import shell
import HeartbeatHandlers
from HeartbeatHandlers import bind_signal_handlers
from ambari_commons.constants import AMBARI_SUDO_BINARY
from resource_management.core.logger import Logger
logger = logging.getLogger()
alerts_logger = logging.getLogger('ambari_alerts')

formatstr = "%(levelname)s %(asctime)s %(filename)s:%(lineno)d - %(message)s"
agentPid = os.getpid()
config = AmbariConfig.AmbariConfig()
configFile = config.getConfigFile()
two_way_ssl_property = config.TWO_WAY_SSL_PROPERTY

IS_LINUX = platform.system() == "Linux"
SYSLOG_FORMAT_STRING = ' ambari_agent - %(filename)s - [%(process)d] - %(name)s - %(levelname)s - %(message)s'
SYSLOG_FORMATTER = logging.Formatter(SYSLOG_FORMAT_STRING)

GRACEFUL_STOP_TRIES = 10
GRACEFUL_STOP_TRIES_SLEEP = 3


def setup_logging(logger, filename, logging_level):
  formatter = logging.Formatter(formatstr)
  rotateLog = logging.handlers.RotatingFileHandler(filename, "a", 10000000, 25)
  rotateLog.setFormatter(formatter)
  logger.addHandler(rotateLog)

  logging.basicConfig(format=formatstr, level=logging_level, filename=filename)
  logger.setLevel(logging_level)
  logger.info("loglevel=logging.{0}".format(logging._levelNames[logging_level]))
    
  global is_logger_setup
  is_logger_setup = True

def add_syslog_handler(logger):
    
  syslog_enabled = config.has_option("logging","syslog_enabled") and (int(config.get("logging","syslog_enabled")) == 1)
      
  #add syslog handler if we are on linux and syslog is enabled in ambari config
  if syslog_enabled and IS_LINUX:
    logger.info("Adding syslog handler to ambari agent logger")
    syslog_handler = SysLogHandler(address="/dev/log",
                                   facility=SysLogHandler.LOG_LOCAL1)
        
    syslog_handler.setFormatter(SYSLOG_FORMATTER)
    logger.addHandler(syslog_handler)
    
def update_log_level(config):
  # Setting loglevel based on config file
  global logger
  log_cfg_file = os.path.join(os.path.dirname(AmbariConfig.AmbariConfig.getConfigFile()), "logging.conf")
  if os.path.exists(log_cfg_file):
    logging.config.fileConfig(log_cfg_file)
    # create logger
    logger = logging.getLogger(__name__)
    logger.info("Logging configured by " + log_cfg_file)
  else:  
    try:
      loglevel = config.get('agent', 'loglevel')
      if loglevel is not None:
        if loglevel == 'DEBUG':
          logging.basicConfig(format=formatstr, level=logging.DEBUG, filename=AmbariConfig.AmbariConfig.getLogFile())
          logger.setLevel(logging.DEBUG)
          logger.info("Newloglevel=logging.DEBUG")
        else:
          logging.basicConfig(format=formatstr, level=logging.INFO, filename=AmbariConfig.AmbariConfig.getLogFile())
          logger.setLevel(logging.INFO)
          logger.debug("Newloglevel=logging.INFO")
    except Exception, err:
      logger.info("Default loglevel=DEBUG")


#  ToDo: move that function inside AmbariConfig
def resolve_ambari_config():
  global config
  configPath = os.path.abspath(AmbariConfig.AmbariConfig.getConfigFile())

  try:
    if os.path.exists(configPath):
      config.read(configPath)
    else:
      raise Exception("No config found at {0}, use default".format(configPath))

  except Exception, err:
    logger.warn(err)


def perform_prestart_checks(expected_hostname):
  # Check if current hostname is equal to expected one (got from the server
  # during bootstrap.
  global config

  if expected_hostname is not None:
    current_hostname = hostname.hostname(config)
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
  if os.path.isfile(ProcessHelper.pidfile) and not OSCheck.get_os_family() == OSConst.WINSRV_FAMILY:
    print("%s already exists, exiting" % ProcessHelper.pidfile)
    sys.exit(1)
  # check if ambari prefix exists
  elif config.has_option('agent', 'prefix') and not os.path.isdir(os.path.abspath(config.get('agent', 'prefix'))):
    msg = "Ambari prefix dir %s does not exists, can't continue" \
          % config.get("agent", "prefix")
    logger.error(msg)
    print(msg)
    sys.exit(1)
  elif not config.has_option('agent', 'prefix'):
    msg = "Ambari prefix dir %s not configured, can't continue"
    logger.error(msg)
    print(msg)
    sys.exit(1)


def daemonize():
  pid = str(os.getpid())
  file(ProcessHelper.pidfile, 'w').write(pid)

def stop_agent():
# stop existing Ambari agent
  pid = -1
  runner = shellRunner()
  try:
    with open(ProcessHelper.pidfile, 'r') as f:
      pid = f.read()
    pid = int(pid)
    
    runner.run([AMBARI_SUDO_BINARY, 'kill', '-15', str(pid)])
    for i in range(GRACEFUL_STOP_TRIES):
      result = runner.run([AMBARI_SUDO_BINARY, 'kill', '-0', str(pid)])
      if result['exitCode'] != 0:
        logger.info("Agent died gracefully, exiting.")
        sys.exit(0)
      time.sleep(GRACEFUL_STOP_TRIES_SLEEP)
    logger.info("Agent not going to die gracefully, going to execute kill -9")
    raise Exception("Agent is running")
  except Exception, err:
    #raise
    if pid == -1:
      print ("Agent process is not running")
    else:
      res = runner.run([AMBARI_SUDO_BINARY, 'kill', '-9', str(pid)])
      if res['exitCode'] != 0:
        raise Exception("Error while performing agent stop. " + res['error'] + res['output'])
    sys.exit(1)

def reset_agent(options):
  try:
    # update agent config file
    agent_config = ConfigParser.ConfigParser()
    agent_config.read(configFile)
    server_host = agent_config.get('server', 'hostname')
    new_host = options[2]
    if new_host is not None and server_host != new_host:
      print "Updating server host from " + server_host + " to " + new_host
      agent_config.set('server', 'hostname', new_host)
      with (open(configFile, "wb")) as new_agent_config:
        agent_config.write(new_agent_config)

    # clear agent certs
    agent_keysdir = agent_config.get('security', 'keysdir')
    print "Removing Agent certificates..."
    for root, dirs, files in os.walk(agent_keysdir, topdown=False):
      for name in files:
        os.remove(os.path.join(root, name))
      for name in dirs:
        os.rmdir(os.path.join(root, name))
  except Exception, err:
    print("A problem occurred while trying to reset the agent: " + str(err))
    sys.exit(1)

  sys.exit(0)

# event - event, that will be passed to Controller and NetUtil to make able to interrupt loops form outside process
# we need this for windows os, where no sigterm available
def main(heartbeat_stop_callback=None):
  global config
  parser = OptionParser()
  parser.add_option("-v", "--verbose", dest="verbose", action="store_true", help="verbose log output", default=False)
  parser.add_option("-e", "--expected-hostname", dest="expected_hostname", action="store",
                    help="expected hostname of current host. If hostname differs, agent will fail", default=None)
  (options, args) = parser.parse_args()

  expected_hostname = options.expected_hostname

  current_user = getpass.getuser()
  
  logging_level = logging.DEBUG if options.verbose else logging.INFO
  setup_logging(logger, AmbariConfig.AmbariConfig.getLogFile(), logging_level)
  setup_logging(alerts_logger, AmbariConfig.AmbariConfig.getAlertsLogFile(), logging_level)
  Logger.initialize_logger('resource_management', logging_level=logging_level)

  default_cfg = {'agent': {'prefix': '/home/ambari'}}
  config.load(default_cfg)

  if (len(sys.argv) > 1) and sys.argv[1] == 'stop':
    stop_agent()

  if (len(sys.argv) > 2) and sys.argv[1] == 'reset':
    reset_agent(sys.argv)

  # Check for ambari configuration file.
  resolve_ambari_config()
  
  # Add syslog hanlder based on ambari config file
  add_syslog_handler(logger)

  # Starting data cleanup daemon
  data_cleaner = None
  if config.has_option('agent', 'data_cleanup_interval') and int(config.get('agent','data_cleanup_interval')) > 0:
    data_cleaner = DataCleaner(config)
    data_cleaner.start()

  perform_prestart_checks(expected_hostname)

  # Starting ping port listener
  try:
    #This acts as a single process machine-wide lock (albeit incomplete, since
    # we still need an extra file to track the Agent PID)
    ping_port_listener = PingPortListener(config)
  except Exception as ex:
    err_message = "Failed to start ping port listener of: " + str(ex)
    logger.error(err_message)
    sys.stderr.write(err_message)
    sys.exit(1)
  ping_port_listener.start()

  update_log_level(config)

  server_hostname = hostname.server_hostname(config)
  server_url = config.get_api_url()

  if not OSCheck.get_os_family() == OSConst.WINSRV_FAMILY:
    daemonize()

  try:
    server_ip = socket.gethostbyname(server_hostname)
    logger.info('Connecting to Ambari server at %s (%s)', server_url, server_ip)
  except socket.error:
    logger.warn("Unable to determine the IP address of the Ambari server '%s'", server_hostname)

  # Wait until server is reachable
  netutil = NetUtil(heartbeat_stop_callback)
  retries, connected = netutil.try_to_connect(server_url, -1, logger)
  # Ambari Agent was stopped using stop event
  if connected:
    # Launch Controller communication
    controller = Controller(config, heartbeat_stop_callback)
    controller.start()
    
    # controller.join() is not appropriate here as it blocks the signal handlers for stop etc.
    while controller.is_alive():
      time.sleep(0.1)

  ExitHelper().exit(0)

if __name__ == "__main__":
  is_logger_setup = False
  try:
    heartbeat_stop_callback = bind_signal_handlers(agentPid)
  
    main(heartbeat_stop_callback)
  except SystemExit as e:
    raise e
  except BaseException as e:
    if is_logger_setup:
      logger.exception("Exiting with exception:" + e)
  raise

#!/usr/bin/env python
#
#  Licensed to the Apache Software Foundation (ASF) under one
#  or more contributor license agreements.  See the NOTICE file
#  distributed with this work for additional information
#  regarding copyright ownership.  The ASF licenses this file
#  to you under the Apache License, Version 2.0 (the
#  "License"); you may not use this file except in compliance
#  with the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.


import logging
import logging.handlers
import sys
import signal
import json
import time
import pdb
import os
import stat
import cmd
import string
import bz2
import datetime
# import traceback
import getpass
import argparse
import readline
import ConfigParser
import StringIO
import subprocess
import textwrap

import utils.displayutils
import utils.osutils
import utils.pluginutils
from ambari_client.ambari_api import AmbariClient


configFile = "/etc/ambari-shell/conf/ambari-shell.ini"


formatstr = "%(levelname)s %(asctime)s %(filename)s:%(lineno)d - %(message)s"
LOG_MAX_BYTES = 10000000
LOG_BACKUP = 2

SHELL_CONFIG = {
    'clustername': None,
    'hostname': 'localhost',
    'port': 8080,
    'username': 'admin',
    'password': 'admin',
    'display_type': 'table',
    'client': None,
    'configFile': configFile}

########################################################################
#
# Utility methods
#
########################################################################


def exit_gracefully(signum, frame):
    # restore the original signal handler as otherwise evil things will happen
    # in raw_input when CTRL+C is pressed, and our signal handler is not
    # re-entrant
    signal.signal(signal.SIGINT, original_sigint)
    print "\nExiting"
    sys.exit(1)

    # restore the exit gracefully handler here
    signal.signal(signal.SIGINT, exit_gracefully)


def exit_gracefully1(signum, frame):
    # restore the original signal handler as otherwise evil things will happen
    # in raw_input when CTRL+C is pressed, and our signal handler is not
    # re-entrant
    signal.signal(signal.SIGQUIT, original_sigint)
    print "\nExiting"
    sys.exit(1)

    # restore the exit gracefully handler here
    signal.signal(signal.SIGQUIT, exit_gracefully)


def resolve_config():
    try:
        config = ConfigParser.RawConfigParser()
        if os.path.exists(configFile):
            print "looking  from " + configFile
            config.read(configFile)
        else:
            raise Exception("No config found")
    except Exception as err:
        logging.warn(err)
    print "found " + configFile
    return config


def get_log_level(loglevel):
    loglev = loglevel.upper()
    if loglev == "DEBUG":
        return logging.DEBUG
    elif loglev == "INFO":
        return logging.INFO
    elif loglev == "WARNING":
        return logging.WARNING
    elif loglev == "CRITICAL":
        return logging.CRITICAL
    elif loglev == "ERROR":
        return logging.ERROR
    elif loglev == "FATAL":
        return logging.FATAL
    else:
        return logging.NOTSET


def setup_logging(loglevel, logPath="./"):
    try:
        logging.root
        curTimestamp = str(datetime.datetime.now())
        ct = curTimestamp.split('.')[0]
        curTime = ct.replace(':', '-')
        datee = curTime.split(' ')[0]
        timee = curTime.split(' ')[1]
        # Set Log directory and log file name. Each run generates a new log
        # file

        logFile = logPath + 'ambaricli_' + datee + "_" + timee + '.log'
        fh = open(logFile, 'w')
        fh.write('*****************************************************\n')
        fh.write('                Amabri Python CLI Log\n')
        t = '                Timestamp: ' + ct + '\n'
        fh.write(t)
        fh.write('*****************************************************\n\n\n')
        fh.close()
        # Set the config for logging
        logging.basicConfig(
            filename=logFile,
            format='%(asctime)s : %(levelname)s: %(message)s',
            level=get_log_level(loglevel))
    except IOError as e:
        errStr = "  I/O error({0}): {1}".format(e.errno, e.strerror)
        print errStr
        sys.exit(1)
    except Exception as exception:
        print exception
        sys.exit(1)


def getLogLevel(configg):
    loglevel = "debug"
    try:
        loglevel = configg.get('python_shell', 'loglevel')
    except Exception:
        logging.error("No loglevel found ")
        return loglevel
    return loglevel


def getLogPath(configg):
    logPath = "./"
    try:
        logPath = configg.get('python_shell', 'log_folder')
    except Exception:
        logging.error("No log_folder found")
        return logPath
    return logPath


def getPluginPath(configg):
    cliplugin_path = "./"
    try:
        cliplugin_path = configg.get('python_shell', 'cliplugin_folder')
    except Exception:
        logging.error("No cliplugin_folder found")
        return cliplugin_path
    return cliplugin_path


def getDefaultPluginPath(configg):
    cliplugin_path = "./"
    try:
        cliplugin_path = configg.get('python_shell', 'default_plugin_folder')
    except Exception:
        logging.error("No default_plugin_folder found")
        return cliplugin_path
    return cliplugin_path


def getCommandsDict(ppath, cliplugin_path):
    default_dictt = utils.pluginutils.getPlugins(ppath)
    logging.debug("pluginutils returned default plugins >> %s ", default_dictt)
    dictt = utils.pluginutils.getPlugins(cliplugin_path)
    logging.debug("pluginutils returned >> %s ", dictt)
    if(not set(default_dictt).isdisjoint(set(dictt))):
        common_commands = set(default_dictt).intersection(set(dictt))
        common_commands = " & ".join(str(x) for x in common_commands)
        logging.error(
            "ERROR :plugins folder has duplicate commands already present in default commands")
        logging.error(common_commands)
        print "ERROR :plugins folder has duplicate command already present in default commands"
        print "pls remove following commands from plugin folder >" + str(common_commands)
        sys.exit(1)
    default_dictt.update(dictt)
    return default_dictt


class CmdBase(cmd.Cmd):

    """CLI .
    """

    intro = utils.displayutils.shellBanner()
    prompt = 'ambari>'
    http_proxy = ''
    https_proxy = ''
    # headers
    doc_header = "Commands"
    undoc_header = "Other Commands"

    def __init__(self):
        cmd.Cmd.__init__(self)

    def do_EOF(self, line):
        logging.info("====== do_EOF ======")
        return True

    def do_exit(self, line):
        logging.info("====== do_exit ======")
        return True

    def postloop(self):
        logging.info("====== exit ======")

#    def parseline(self, line):
#        print 'parseline(%s) =>' % line,
#        ret = cmd.Cmd.parseline(self, line)
#        print ret
#        return ret

    def emptyline(self):
        # print 'emptyline()'
        # return cmd.Cmd.emptyline(self)
        return

    def default(self, line):
        """Called on an input line when the command prefix is not recognized.

        If this method is not overridden, it prints an error message and
        returns.

        """
        self.stdout.write(
            '*** Unknown command *** : %s \n type "help" to list all commands \n' %
            line)


class AmbariShell(CmdBase):

    COMPONENTS = {}
    SERVICES = None
    CLUSTERS = None

    def __init__(self):
        CmdBase.__init__(self)
        self.config = None
        self.global_shell_config = SHELL_CONFIG

    def preloop(self):
        "Checks if the cluster was pre-defined"
        self._set_prompt()

    def _set_prompt(self):
        if self.global_shell_config['clustername']:
            self.prompt = "ambari-" + \
                str(self.global_shell_config['clustername']) + ">"
            logging.debug("found a cluster >" +
                          str(self.global_shell_config['clustername']))
        else:
            self.prompt = 'ambari>'

    def postcmd(self, stop, line):
        # print 'postcmd(%s, %s)' % (stop, line)
        self._set_prompt()
        return cmd.Cmd.postcmd(self, stop, line)

    def setConfig(self, customConfig):
        self.config = customConfig

    # core code should begin here

    def generate_output(self, headers, rows):
        if self.global_shell_config['display_type'] == "table":
            print utils.displayutils.display_table(headers, rows)

        if self.global_shell_config['display_type'] == "csv":
            utils.displayutils.createCSV(headers, rows)

        if self.global_shell_config['display_type'] == "xml":
            print utils.displayutils.createXML(headers, rows)

#
# The "main" function


def main():
    parser = argparse.ArgumentParser(description='Ambari CLI')
    parser.add_argument('-H', '--host', action='store', dest='host')
    parser.add_argument(
        '-p',
        '--port',
        action='store',
        dest='port',
        type=int,
        default=8080)
    parser.add_argument(
        '-u',
        '--user',
        action='store',
        dest='user',
        default='admin')
    parser.add_argument(
        '-c',
        '--clustername',
        action='store',
        dest='clustername')
    parser.add_argument(
        '--password',
        action='store',
        dest='password',
        default='admin')
    parser.add_argument(
        '-d',
        '--display_type',
        action='store',
        dest='display_type',
        default='table')
    parser.add_argument('-r', '--run', action='store', dest='run')
    args = parser.parse_args()

    # Check if a username was suplied, if not, prompt the user
    if not args.host:
        args.host = raw_input("Enter Ambari Server host: ")

    if args.host:
        SHELL_CONFIG['hostname'] = args.host
    if args.clustername:
        SHELL_CONFIG['clustername'] = args.clustername
    if args.display_type:
        SHELL_CONFIG['display_type'] = args.display_type

    headers_dict = {'X-Requested-By': 'mycompany'}
    client = AmbariClient(
        SHELL_CONFIG['hostname'],
        SHELL_CONFIG['port'],
        SHELL_CONFIG['username'],
        SHELL_CONFIG['password'],
        version=1,
        http_header=headers_dict)
    SHELL_CONFIG['client'] = client

    # do some plumbing
    config = resolve_config()
    logPath = getLogPath(config)
    loglevel = getLogLevel(config)
    cliplugin_path = getPluginPath(config)
    dpath = getDefaultPluginPath(config)
    setup_logging(loglevel, logPath)
    # get ready to create a shell
    utils.osutils.doclearScreen()
    shell = AmbariShell()
    logging.info("cliplugin_folder =  %s", getPluginPath(config))
    logging.info("SHELL_CONFIG =  %s", str(SHELL_CONFIG))
    # Get all commands

    commands_dictt = getCommandsDict(dpath, cliplugin_path)
    for k, v in commands_dictt.items():
        setattr(AmbariShell, str(k), v)
    shell.setConfig(config)

    # Check if user is attempting non-interactive shell
    if args.run:
        print args.run
        for command in args.run.split(';'):
            shell.onecmd(command)
            sys.exit(0)
    else:
        try:
            shell.cmdloop()
        except KeyboardInterrupt:
            sys.stdout.write("\n")
            sys.exit(0)
    logging.info("finished")


if __name__ == '__main__':
    original_sigint = signal.getsignal(signal.SIGINT)
    signal.signal(signal.SIGINT, exit_gracefully)
    signal.signal(signal.SIGQUIT, exit_gracefully1)
    signal.signal(signal.SIGTERM, exit_gracefully1)
    main()

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

from unittest import TestCase
from ambari_agent.PuppetExecutor import PuppetExecutor
from pprint import pformat
import socket
import os
import sys
import logging
from AmbariConfig import AmbariConfig
import tempfile

FILEPATH="runme.pp"
logger = logging.getLogger()

class TestPuppetExecutor(TestCase):

  def test_run(self):
    """
    Used to run arbitrary puppet manifest. Test tries to find puppet manifest 'runme.pp' and runs it.
    Test does not make any assertions
    """
    if not os.path.isfile(FILEPATH):
      return

    logger.info("***** RUNNING " + FILEPATH + " *****")
    cwd = os.getcwd()
    puppetexecutor = PuppetExecutor(cwd, "/x", "/y", "/tmp", AmbariConfig().getConfig())
    result = {}
    puppetEnv = os.environ
    _, tmpoutfile = tempfile.mkstemp()
    _, tmperrfile = tempfile.mkstemp()
    result = puppetexecutor.runPuppetFile(FILEPATH, result, puppetEnv, tmpoutfile, tmperrfile)
    logger.info("*** Puppet output: " + str(result['stdout']))
    logger.info("*** Puppet errors: " + str(result['stderr']))
    logger.info("*** Puppet retcode: " + str(result['exitcode']))
    logger.info("****** DONE *****")



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
import json
import os.path
import logging
import subprocess
from manifestGenerator import generateManifest
from RepoInstaller import RepoInstaller
import pprint, threading
from Grep import Grep
from threading import Thread
import shell
import traceback
from Grep import Grep
from StackVersionsFileHandler import StackVersionsFileHandler
import re, json

logger = logging.getLogger()
grep = Grep()

class UpgradeExecutor:

  """ Class that performs the StackVersion stack upgrade"""

  SCRIPT_DIRS = [
    'pre-upgrade.d',
    'upgrade.d',
    'post-upgrade.d'
  ]

  NAME_PARSING_FAILED_CODE = 999

  def __init__(self, pythonExecutor, puppetExecutor, config):
    self.pythonExecutor = pythonExecutor
    self.puppetExecutor = puppetExecutor
    self.stacksDir = config.get('stack', 'upgradeScriptsDir')
    self.config = config
    versionsFileDir = config.get('agent', 'prefix')
    self.versionsHandler = StackVersionsFileHandler(versionsFileDir)


  def perform_stack_upgrade(self, command, tmpout, tmperr):
    logger.info("Performing stack upgrade")
    params = command['commandParams']
    srcStack = params['source_stack_version']
    tgtStack = params['target_stack_version']
    component = command['role']

    srcStackTuple = self.split_stack_version(srcStack)
    tgtStackTuple = self.split_stack_version(tgtStack)

    if srcStackTuple is None or tgtStackTuple is None:
      errorstr = "Source (%s) or target (%s) version does not match pattern \
      <Name>-<Version>" % (srcStack, tgtStack)
      logger.info(errorstr)
      result = {
        'exitcode' : 1,
        'stdout'   : 'None',
        'stderr'   : errorstr
      }
    elif srcStack != tgtStack:
      paramTuple = sum((srcStackTuple, tgtStackTuple), ())
      upgradeId = "%s-%s.%s_%s-%s.%s" % paramTuple
      # Check stack version (do we need upgrade?)
      basedir = os.path.join(self.stacksDir, upgradeId, component)
      if not os.path.isdir(basedir):
        errorstr = "Upgrade %s is not supported (dir %s does not exist)" \
                   % (upgradeId, basedir)
        logger.error(errorstr)
        result = {
          'exitcode' : 1,
          'stdout'   : errorstr,
          'stderr'   : errorstr
        }
      else:
        result = {
          'exitcode' : 0,
          'stdout'   : '',
          'stderr'   : ''
        }
        for dir in self.SCRIPT_DIRS:
          if result['exitcode'] != 0:
            break
          tmpRes = self.execute_dir(command, basedir, dir, tmpout, tmperr)

          result = {
            'exitcode' : result['exitcode'] or tmpRes['exitcode'],
            'stdout'   : "%s\n%s" % (result['stdout'], tmpRes['stdout']),
            'stderr'   : "%s\n%s" % (result['stderr'], tmpRes['stderr']),
          }

        if result['exitcode'] == 0:
          logger.info("Upgrade %s successfully finished" % upgradeId)
          self.versionsHandler.write_stack_version(component, tgtStack)
    else:
      infostr = "target_stack_version (%s) matches current stack version" \
          " for component %s, nothing to do" % (tgtStack, component)
      logger.info(infostr)
      result = {
        'exitcode' : 0,
        'stdout'   : infostr,
        'stderr'   : 'None'
      }
    result = {
      'exitcode' : result['exitcode'],
      'stdout'   : grep.tail(result['stdout'], grep.OUTPUT_LAST_LINES),
      'stderr'   : grep.tail(result['stderr'], grep.OUTPUT_LAST_LINES)
    }
    return result


  def get_key_func(self, name):
    """
    Returns a number from filenames like 70-foobar.* or 999 for not matching
    filenames
    """
    parts = name.split('-', 1)
    if not parts or not parts[0].isdigit():
      logger.warn("Can't parse script filename number %s" % name)
      return self.NAME_PARSING_FAILED_CODE # unknown element will be placed to the end of list
    return int(parts[0])


  def split_stack_version(self, verstr):
    verdict = json.loads(verstr)
    stack_name = verdict["stackName"].strip()

    matchObj = re.match( r'(\d+).(\d+)', verdict["stackVersion"].strip(), re.M|re.I)
    stack_major_ver = matchObj.group(1)
    stack_minor_ver = matchObj.group(2)
    if matchObj:
      return stack_name, stack_major_ver, stack_minor_ver
    else:
      return None


  def execute_dir(self, command, basedir, dir, tmpout, tmperr):
    """
    Executes *.py and *.pp files located in a given directory.
    Files a executed in a numeric sorting order.
    """
    dirpath = os.path.join(basedir, dir)
    logger.info("Executing %s" % dirpath)
    if not os.path.isdir(dirpath):
      warnstr = "Script directory %s does not exist, skipping" % dirpath
      logger.warn(warnstr)
      result = {
        'exitcode' : 0,
        'stdout'   : warnstr,
        'stderr'   : 'None'
      }
      return result
    fileList=os.listdir(dirpath)
    fileList.sort(key = self.get_key_func)
    formattedResult = {
      'exitcode' : 0,
      'stdout'   : '',
      'stderr'   : ''
    }
    for filename in fileList:
      prevcode = formattedResult['exitcode']
      if prevcode != 0 or self.get_key_func(filename) == self.NAME_PARSING_FAILED_CODE:
        break
      filepath = os.path.join(dirpath, filename)
      if filename.endswith(".pp"):
        logger.info("Running puppet file %s" % filepath)
        result = self.puppetExecutor.just_run_one_file(command, filepath,
                                                                tmpout, tmperr)
      elif filename.endswith(".py"):
        logger.info("Running python file %s" % filepath)
        result = self.pythonExecutor.run_file(command, filepath, tmpout, tmperr)
      elif filename.endswith(".pyc"):
        pass # skipping compiled files
      else:
        warnstr = "Unrecognized file type, skipping: %s" % filepath
        logger.warn(warnstr)
        result = {
          'exitcode' : 0,
          'stdout'   : warnstr,
          'stderr'   : 'None'
        }
      formattedResult = {
        'exitcode' : prevcode or result['exitcode'],
        'stdout'   : "%s\n%s" % (formattedResult['stdout'], result['stdout']),
        'stderr'   : "%s\n%s" % (formattedResult['stderr'], result['stderr']),
      }
    logger.debug("Result of %s: \n %s" % (dirpath, pprint.pformat(formattedResult)))
    return formattedResult







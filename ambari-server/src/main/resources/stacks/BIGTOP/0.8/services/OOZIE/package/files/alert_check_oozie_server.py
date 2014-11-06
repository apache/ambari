#!/usr/bin/env python

"""
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
"""

import subprocess
from subprocess import CalledProcessError

RESULT_CODE_OK = 'OK'
RESULT_CODE_CRITICAL = 'CRITICAL'
RESULT_CODE_UNKNOWN = 'UNKNOWN'

OOZIE_URL_KEY = '{{oozie-site/oozie.base.url}}'

def get_tokens():
  """
  Returns a tuple of tokens in the format {{site/property}} that will be used
  to build the dictionary passed into execute
  """
  return (OOZIE_URL_KEY)
  

def execute(parameters=None, host_name=None):
  """
  Returns a tuple containing the result code and a pre-formatted result label

  Keyword arguments:
  parameters (dictionary): a mapping of parameter key to value
  host_name (string): the name of this host where the alert is running
  """

  if parameters is None:
    return (RESULT_CODE_UNKNOWN, ['There were no parameters supplied to the script.'])

  oozie_url = None
  if OOZIE_URL_KEY in parameters:
    oozie_url = parameters[OOZIE_URL_KEY]

  if oozie_url is None:
    return (RESULT_CODE_UNKNOWN, ['The Oozie URL is a required parameter.'])

  try:
    # oozie admin -oozie http://server:11000/oozie -status
    oozie_process = subprocess.Popen(['oozie', 'admin', '-oozie',
      oozie_url, '-status'], stderr=subprocess.PIPE, stdout=subprocess.PIPE)

    oozie_output, oozie_error = oozie_process.communicate()
    oozie_return_code = oozie_process.returncode

    if oozie_return_code == 0:
      # strip trailing newlines
      oozie_output = str(oozie_output).strip('\n')
      return (RESULT_CODE_OK, [oozie_output])
    else:
      oozie_error = str(oozie_error).strip('\n')
      return (RESULT_CODE_CRITICAL, [oozie_error])

  except CalledProcessError, cpe:
    return (RESULT_CODE_CRITICAL, [str(cpe)])

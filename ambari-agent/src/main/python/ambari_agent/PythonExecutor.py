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
import os.path
import logging
import subprocess
import pprint, threading
from Grep import Grep
from threading import Thread
import shell
import traceback

logger = logging.getLogger()

class PythonExecutor:

  def __init__(self):
    pass

  def run_file(self, name, stdout, stderr):
    """
    Executes the file specified in a separate subprocess.
    Method returns only when the subprocess is finished or timeout is exceeded
    """
    # TODO: implement
    logger.warn("TODO: Python file execution is not supported yet")
    pass

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
import os
import sys
import platform

LOG = logging.getLogger(__name__)


def clearScreen(operatingSys):
    """
    Function to clear the screen
    Input   : OS
    """
    logging.info('Entering..')

    if operatingSys == 'Windows':
        cmdClear = 'CLS'
    elif operatingSys == 'Linux':
        cmdClear = 'clear'
    elif operatingSys == 'Darwin':
        cmdClear = 'clear'
    logging.debug('Running command : %s', cmdClear)
    os.system(cmdClear)
    logging.info('Exiting..')


def getOperatingSystem():
    logging.info('Entering..')
    operatingSys = platform.system()
    # sprint operatingSys
    if not operatingSys:
        logging.error('Operating system is NULL.')
        return False, ''
    else:
        logging.debug('Got operating system : %s', operatingSys)
        logging.info('Exiting..')
        return True, operatingSys


def doclearScreen():
    # Determine the OS
    result, operatingSys = getOperatingSystem()
    if not result:
        logging.error('Failed to determine Operating System. Exiting.')
        sys.exit(1)
    # clear the Screen
    clearScreen(operatingSys)


if __name__ == '__main__':
    pass

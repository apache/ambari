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

import core
from core.controller import Controller
from core.config_reader import Configuration
import logging
import signal
import sys

logger = logging.getLogger()

def main(argv=None):
  # Allow Ctrl-C
  signal.signal(signal.SIGINT, signal.SIG_DFL)

  config = Configuration()
  controller = Controller(config)
  
  _init_logging(config)
  
  logger.info('Starting Server RPC Thread: %s' % ' '.join(sys.argv))
  controller.start()
  controller.start_emitter()

def _init_logging(config):
  _levels = {
          'DEBUG': logging.DEBUG,
          'INFO': logging.INFO,
          'WARNING': logging.WARNING,
          'ERROR': logging.ERROR,
          'CRITICAL': logging.CRITICAL,
          'NOTSET' : logging.NOTSET
          }
  level = logging.INFO
  if config.get_log_level() in _levels:
    level = _levels.get(config.get_log_level())
  logger.setLevel(level)
  formatter = logging.Formatter("%(asctime)s [%(levelname)s] %(filename)s:%(lineno)d - %(message)s")
  stream_handler = logging.StreamHandler()
  stream_handler.setFormatter(formatter)
  logger.addHandler(stream_handler)
  

if __name__ == '__main__':
  main()


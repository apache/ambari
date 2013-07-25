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

import AmbariConfig
import threading
import os
import time
import re
import logging

logger = logging.getLogger()

class DataCleaner(threading.Thread):
  FILE_NAME_PATTERN = 'errors-\d+.txt|output-\d+.txt|site-\d+.pp'

  def __init__(self, config):
    threading.Thread.__init__(self)
    self.daemon = True
    logger.info('Data cleanup thread started')
    self.config = config
    self.file_max_age = int(config.get('agent','data_cleanup_max_age'))
    if self.file_max_age < 86400:
      logger.warn('The minimum value allowed for data_cleanup_max_age is 1 '
                  'day. Setting data_cleanup_max_age to 86400.')
      self.file_max_age = 86400
    self.cleanup_interval = int(config.get('agent','data_cleanup_interval'))
    if self.cleanup_interval < 3600:
      logger.warn('The minimum value allowed for data_cleanup_interval is 1 '
                  'hour. Setting data_cleanup_interval to 3600.')
      self.file_max_age = 3600

    self.data_dir = config.get('agent','prefix')
    self.compiled_pattern = re.compile(self.FILE_NAME_PATTERN)
    self.stopped = False

  def __del__(self):
    logger.info('Data cleanup thread killed.')

  def cleanup(self):
    for root, dirs, files in os.walk(self.data_dir):
      for f in files:
        file_path = os.path.join(root, f)
        if self.compiled_pattern.match(f):
          try:
            if time.time() - os.path.getmtime(file_path) > self.file_max_age:
              os.remove(os.path.join(file_path))
              logger.debug('Removed file: ' + file_path)
          except Exception:
            logger.error('Error when removing file: ' + file_path)


  def run(self):
    while not self.stopped:
      logger.info('Data cleanup started')
      self.cleanup()
      logger.info('Data cleanup finished')
      time.sleep(self.cleanup_interval)


def main():
  data_cleaner = DataCleaner(AmbariConfig.config)
  data_cleaner.start()
  data_cleaner.join()


if __name__ == "__main__":
  main()

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

import sys
import logging
import AmbariConfig
import threading
import socket

logger = logging.getLogger()

class PingPortListener(threading.Thread):


  def __init__(self, config):
    threading.Thread.__init__(self)
    self.daemon = True
    self.running = True
    self.config = config
    self.host = '0.0.0.0'
    self.port = int(self.config.get('agent','ping_port'))
    try:
      self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
      self.socket.bind((self.host, self.port))
      self.socket.listen(1)
    except Exception as ex:
      logger.error("Failed to start ping port listener of:" + str(ex));
      sys.exit(1)
    else:
      config.set('agent','current_ping_port',str(self.socket.getsockname()[1]))
      logger.info("Ping port listener started on port: " + str(self.socket.getsockname()[1]))


  def __del__(self):
    logger.info("Ping port listener killed")


  def run(self):
    while  self.running:
      try:
        conn, addr = self.socket.accept()
        conn.send("OK")
        conn.close()
      except Exception as ex:
        logger.error("Failed in Ping port listener because of:" + str(ex));
        sys.exit(1)
  pass

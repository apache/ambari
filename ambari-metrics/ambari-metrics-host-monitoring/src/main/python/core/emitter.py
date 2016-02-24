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

import logging
import threading

from security import CachedHTTPSConnection, CachedHTTPConnection

logger = logging.getLogger()

class Emitter(threading.Thread):
  AMS_METRICS_POST_URL = "/ws/v1/timeline/metrics/"
  RETRY_SLEEP_INTERVAL = 5
  MAX_RETRY_COUNT = 3
  """
  Wake up every send interval seconds and empty the application metric map.
  """
  def __init__(self, config, application_metric_map, stop_handler):
    threading.Thread.__init__(self)
    logger.debug('Initializing Emitter thread.')
    self.lock = threading.Lock()
    self.send_interval = config.get_send_interval()
    self._stop_handler = stop_handler
    self.application_metric_map = application_metric_map
    timeout = int(self.send_interval - 10)
    if config.is_server_https_enabled():
      self.connection = CachedHTTPSConnection(config.get_server_host(),
                                              config.get_server_port(),
                                              timeout=timeout,
                                              ca_certs=config.get_ca_certs())
    else:
      self.connection = CachedHTTPConnection(config.get_server_host(),
                                             config.get_server_port(),
                                             timeout=timeout)

  def run(self):
    logger.info('Running Emitter thread: %s' % threading.currentThread().getName())
    while True:
      try:
        self.submit_metrics()
      except Exception, e:
        logger.warn('Unable to emit events. %s' % str(e))
      pass
      #Wait for the service stop event instead of sleeping blindly
      if 0 == self._stop_handler.wait(self.send_interval):
        logger.info('Shutting down Emitter thread')
        return
    pass

  def submit_metrics(self):
    retry_count = 0
    # This call will acquire lock on the map and clear contents before returning
    # After configured number of retries the data will not be sent to the
    # collector
    json_data = self.application_metric_map.flatten(None, True)
    if json_data is None:
      logger.info("Nothing to emit, resume waiting.")
      return
    pass

    response = None
    while retry_count < self.MAX_RETRY_COUNT:
      try:
        response = self.push_metrics(json_data)
      except Exception, e:
        logger.warn('Error sending metrics to server. %s' % str(e))
      pass

      if response and response.status == 200:
        retry_count = self.MAX_RETRY_COUNT
      else:
        logger.warn("Retrying after {0} ...".format(self.RETRY_SLEEP_INTERVAL))
        retry_count += 1
        #Wait for the service stop event instead of sleeping blindly
        if 0 == self._stop_handler.wait(self.RETRY_SLEEP_INTERVAL):
          return
      pass
    pass

  def push_metrics(self, data):
    headers = {"Content-Type" : "application/json",
               "Accept" : "*/*",
               "Connection":" Keep-Alive"}
    logger.debug("message to sent: %s" % data)
    self.connection.request("POST", self.AMS_METRICS_POST_URL, data, headers)
    response = self.connection.getresponse()
    if response:
      logger.debug("POST response from server: retcode = {0}, reason = {1}"
                   .format(response.status, response.reason))
      logger.debug(str(response.read()))

    return response

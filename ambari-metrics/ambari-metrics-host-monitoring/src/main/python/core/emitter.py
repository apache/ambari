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
from spnego_kerberos_auth import SPNEGOKerberosAuth

from security import CachedHTTPSConnection, CachedHTTPConnection

logger = logging.getLogger()

class Emitter(threading.Thread):
  AMS_METRICS_POST_URL = "/ws/v1/timeline/metrics/"
  RETRY_SLEEP_INTERVAL = 5
  MAX_RETRY_COUNT = 3
  cookie_cached = {}
  kinit_cmd = None
  klist_cmd = None
  spnego_krb_auth = None
  """
  Wake up every send interval seconds and empty the application metric map.
  """
  def __init__(self, config, application_metric_map, stop_handler):
    threading.Thread.__init__(self)
    logger.debug('Initializing Emitter thread.')
    self.lock = threading.Lock()
    self.send_interval = config.get_send_interval()
    self.kinit_cmd = config.get_kinit_cmd()
    if self.kinit_cmd:
      logger.debug(self.kinit_cmd)
    self.klist_cmd = config.get_klist_cmd()
    self._stop_handler = stop_handler
    self.application_metric_map = application_metric_map
    # TODO verify certificate
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
        self.cookie_cached = {}
      pass
      #Wait for the service stop event instead of sleeping blindly
      if 0 == self._stop_handler.wait(self.send_interval):
        logger.info('Shutting down Emitter thread')
        return
    pass

  def submit_metrics(self):
    # This call will acquire lock on the map and clear contents before returning
    # After configured number of retries the data will not be sent to the
    # collector
    json_data = self.application_metric_map.flatten(None, True)
    if json_data is None:
      logger.info("Nothing to emit, resume waiting.")
      return
    pass
    self.push_metrics(json_data)

  # TODO verify certificate
  def push_metrics(self, data):
    headers = {"Content-Type" : "application/json",
               "Accept" : "*/*",
               "Connection":" Keep-Alive"}
    logger.debug("message to sent: %s" % data)
    try:
      if self.cookie_cached[self.connection.host]:
        headers["Cookie"] = self.cookie_cached[self.connection.host]
        logger.debug("Cookie: %s" % self.cookie_cached[self.connection.host])
    except Exception, e:
      self.cookie_cached = {}
    pass

    retry_count = 0
    while retry_count < self.MAX_RETRY_COUNT:
      response = self.get_response_from_submission(self.connection, data, headers)
      if response:
        if response.status == 200:
          return
        if response.status == 401 or response.status == 403:
          self.cookie_cached = {}
          auth_header = response.getheader('www-authenticate', None)
          if auth_header == None:
            logger.warn('www-authenticate header not found')
          else:
            self.spnego_krb_auth = SPNEGOKerberosAuth()
            if self.spnego_krb_auth.get_negotiate_value(auth_header) == '':
              response = self.spnego_krb_auth.authenticate_handshake(self.connection, "POST", self.AMS_METRICS_POST_URL, data, headers, self.kinit_cmd, self.klist_cmd)
              if response:
                logger.debug("response from authenticate_client: retcode = {0}, reason = {1}"
                             .format(response.status, response.reason))
                logger.debug(str(response.read()))
                if response.status == 200:
                  logger.debug("response headers: {0}".format(response.getheaders()))
                  logger.debug("cookie_cached: %s" % self.cookie_cached)
                  set_cookie_header = response.getheader('set-cookie', None)
                  if set_cookie_header and self.spnego_krb_auth:
                    set_cookie_val = self.spnego_krb_auth.get_hadoop_auth_cookie(set_cookie_header)
                    logger.debug("set_cookie: %s" % set_cookie_val)
                    if set_cookie_val:
                      self.cookie_cached[self.connection.host] = set_cookie_val
                  return
      pass
      logger.warn("Retrying after {0} ...".format(self.RETRY_SLEEP_INTERVAL))
      retry_count += 1
      #Wait for the service stop event instead of sleeping blindly
      if 0 == self._stop_handler.wait(self.RETRY_SLEEP_INTERVAL):
        return
      pass

  def get_response_from_submission(self, connection, data, headers):
    try:
      connection.request("POST", self.AMS_METRICS_POST_URL, data, headers)
      response = connection.getresponse()
      if response:
        logger.debug("POST response from server: retcode = {0}, reason = {1}"
                     .format(response.status, response.reason))
        logger.debug(str(response.read()))
      return response
    except Exception, e:
      logger.warn('Error sending metrics to server. %s' % str(e))
      self.cookie_cached = {}
      return None
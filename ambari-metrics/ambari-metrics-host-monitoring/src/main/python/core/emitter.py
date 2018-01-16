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
from blacklisted_set import BlacklistedSet
from config_reader import ROUND_ROBIN_FAILOVER_STRATEGY
from spnego_kerberos_auth import SPNEGOKerberosAuth

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
    self.hostname = config.get_hostname_config()
    self.hostname_hash = self.compute_hash(self.hostname)
    self._stop_handler = stop_handler
    self.application_metric_map = application_metric_map
    self.collector_port = config.get_server_port()
    self.all_metrics_collector_hosts = config.get_metrics_collector_hosts_as_list()
    self.is_collector_https_enabled = config.is_collector_https_enabled()
    self.collector_protocol = "https" if self.is_collector_https_enabled else "http"
    self.set_instanceid = config.is_set_instanceid()
    self.instanceid = config.get_instanceid()
    self.is_inmemory_aggregation_enabled = config.is_inmemory_aggregation_enabled()

    if self.is_inmemory_aggregation_enabled:
      self.inmemory_aggregation_port = config.get_inmemory_aggregation_port()
      self.inmemory_aggregation_protocol = config.get_inmemory_aggregation_protocol()
      if self.inmemory_aggregation_protocol == "https":
        self.ca_certs = config.get_ca_certs()

    if self.is_collector_https_enabled:
      self.ca_certs = config.get_ca_certs()

    # TimedRoundRobinSet
    if config.get_failover_strategy() == ROUND_ROBIN_FAILOVER_STRATEGY:
      self.active_collector_hosts = BlacklistedSet(self.all_metrics_collector_hosts, float(config.get_failover_strategy_blacklisted_interval_seconds()))
    else:
      raise Exception(-1, "Uknown failover strategy {0}".format(config.get_failover_strategy()))

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
    json_data = self.application_metric_map.flatten(None, True, set_instanceid=self.set_instanceid, instanceid=self.instanceid)
    if json_data is None:
      logger.info("Nothing to emit, resume waiting.")
      return
    pass
    self.push_metrics(json_data)

  def push_metrics(self, data):
    success = False
    if self.is_inmemory_aggregation_enabled:
      success = self.try_with_collector(self.inmemory_aggregation_protocol, "localhost", self.inmemory_aggregation_port, data)
      if not success:
        logger.warning("Failed to submit metrics to local aggregator. Trying to post them to collector...")
    while not success and self.active_collector_hosts.get_actual_size() > 0:
      collector_host = self.get_collector_host_shard()
      success = self.try_with_collector(self.collector_protocol, collector_host, self.collector_port, data)
    pass

    if not success:
      logger.info('No valid collectors found...')
      for collector_host in self.active_collector_hosts:
        success = self.try_with_collector(self.collector_protocol, collector_host, self.ollector_port, data)
        if success:
          break
      pass

  def try_with_collector(self, collector_protocol, collector_host, collector_port, data):
    headers = {"Content-Type" : "application/json", "Accept" : "*/*"}
    connection = self.get_connection(collector_protocol, collector_host, collector_port)
    logger.debug("message to send: %s" % data)

    try:
      if self.cookie_cached[connection.host]:
        headers["Cookie"] = self.cookie_cached[connection.host]
        logger.debug("Cookie: %s" % self.cookie_cached[connection.host])
    except Exception, e:
      self.cookie_cached = {}
    pass

    retry_count = 0
    while retry_count < self.MAX_RETRY_COUNT:
      response = self.get_response_from_submission(connection, data, headers)
      if response:
        if response.status == 200:
          return True
        if response.status == 401:
          self.cookie_cached = {}
          auth_header = response.getheader('www-authenticate', None)
          if auth_header == None:
              logger.warn('www-authenticate header not found')
          else:
            self.spnego_krb_auth = SPNEGOKerberosAuth()
            if self.spnego_krb_auth.get_negotiate_value(auth_header) == '':
              response = self.spnego_krb_auth.authenticate_handshake(connection, "POST", self.AMS_METRICS_POST_URL, data, headers, self.kinit_cmd, self.klist_cmd)
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
                      self.cookie_cached[connection.host] = set_cookie_val
                  return True
      #No response or failed
      logger.warn("Retrying after {0} ...".format(self.RETRY_SLEEP_INTERVAL))
      retry_count += 1
      #Wait for the service stop event instead of sleeping blindly
      if 0 == self._stop_handler.wait(self.RETRY_SLEEP_INTERVAL):
        return True
    pass

    if retry_count >= self.MAX_RETRY_COUNT:
      self.active_collector_hosts.blacklist(collector_host)
      logger.warn("Metric collector host {0} was blacklisted.".format(collector_host))
      return False

  def get_connection(self, protocol, host, port):
    timeout = int(self.send_interval - 10)
    if protocol == "https":
      connection = CachedHTTPSConnection(host,
                                         port,
                                         timeout=timeout,
                                         ca_certs=self.ca_certs)
    else:
      connection = CachedHTTPConnection(host,
                                        port,
                                        timeout=timeout)
    return connection

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

  def get_collector_host_shard(self):
    size = self.active_collector_hosts.get_actual_size()
    index = self.hostname_hash % size
    index = index if index >= 0 else index + size
    hostname = self.active_collector_hosts.get_item_at_index(index)
    logger.info('Calculated collector shard based on hostname : %s' % hostname)
    return hostname

  def compute_hash(self, hostname):
    hash = 11987
    length = len(hostname)
    for i in xrange(0, length - 1):
      hash = 31*hash + ord(hostname[i])
    return hash





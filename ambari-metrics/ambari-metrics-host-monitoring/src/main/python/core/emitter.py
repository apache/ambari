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
    self.hostname = config.get_hostname_config()
    self.hostname_hash = self.compute_hash(self.hostname)
    self._stop_handler = stop_handler
    self.application_metric_map = application_metric_map
    self.collector_port = config.get_server_port()
    self.all_metrics_collector_hosts = config.get_metrics_collector_hosts()
    self.is_server_https_enabled = config.is_server_https_enabled()
    self.set_instanceid = config.is_set_instanceid()
    self.instanceid = config.get_instanceid()

    if self.is_server_https_enabled:
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
    while self.active_collector_hosts.get_actual_size() > 0:
      collector_host = self.get_collector_host_shard()
      success = self.try_with_collector_host(collector_host, data)
      if success:
        break
    pass

    if not success:
      logger.info('No valid collectors found...')
      for collector_host in self.active_collector_hosts:
        success = self.try_with_collector_host(collector_host, data)
      pass

  def try_with_collector_host(self, collector_host, data):
    headers = {"Content-Type" : "application/json", "Accept" : "*/*"}
    connection = self.get_connection(collector_host)
    logger.debug("message to send: %s" % data)
    retry_count = 0
    while retry_count < self.MAX_RETRY_COUNT:
      response = self.get_response_from_submission(connection, data, headers)
      if response and response.status == 200:
        return True
      else:
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

  def get_connection(self, collector_host):
    timeout = int(self.send_interval - 10)
    if self.is_server_https_enabled:
      connection = CachedHTTPSConnection(collector_host,
                                         self.collector_port,
                                         timeout=timeout,
                                         ca_certs=self.ca_certs)
    else:
      connection = CachedHTTPConnection(collector_host,
                                        self.collector_port,
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




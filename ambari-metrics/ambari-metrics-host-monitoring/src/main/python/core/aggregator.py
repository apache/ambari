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

import threading
import subprocess
import logging
import urllib2

logger = logging.getLogger()
class Aggregator(threading.Thread):
  def __init__(self, config, stop_handler):
    threading.Thread.__init__(self)
    self._config = config
    self._stop_handler = stop_handler
    self._aggregator_process = None
    self._sleep_interval = config.get_collector_sleep_interval()
    self.stopped = False

  def run(self):
    java_home = self._config.get_java_home()
    collector_hosts = self._config.get_metrics_collector_hosts_as_string()
    jvm_agrs = self._config.get_aggregator_jvm_agrs()
    config_dir = self._config.get_config_dir()
    class_name = "org.apache.hadoop.metrics2.host.aggregator.AggregatorApplication"
    ams_log_file = "ambari-metrics-aggregator.log"
    additional_classpath = ':{0}'.format(config_dir)
    ams_log_dir = self._config.ams_monitor_log_dir()
    hostname = self._config.get_hostname_config()
    logger.info('Starting Aggregator thread.')
    cmd = "{0}/bin/java {1} -Dams.log.dir={2} -Dams.log.file={3} -cp /var/lib/ambari-metrics-monitor/lib/*{4} {5} {6} {7}"\
      .format(java_home, jvm_agrs, ams_log_dir, ams_log_file, additional_classpath, class_name, hostname, collector_hosts)

    logger.info("Executing : {0}".format(cmd))

    self._aggregator_process = subprocess.Popen([cmd], stdout = None, stderr = None, shell = True)
    while not self.stopped:
      if 0 == self._stop_handler.wait(self._sleep_interval):
        break
    pass
    self.stop()

  def stop(self):
    self.stopped = True
    if self._aggregator_process :
      logger.info('Stopping Aggregator thread.')
      self._aggregator_process.terminate()
      self._aggregator_process = None

class AggregatorWatchdog(threading.Thread):
  SLEEP_TIME = 30
  CONNECTION_TIMEOUT = 5
  AMS_AGGREGATOR_METRICS_CHECK_URL = "/ws/v1/timeline/metrics/"
  def __init__(self, config, stop_handler):
    threading.Thread.__init__(self)
    self._config = config
    self._stop_handler = stop_handler
    self.URL = 'http://localhost:' + self._config.get_inmemory_aggregation_port() + self.AMS_AGGREGATOR_METRICS_CHECK_URL
    self._is_ok = threading.Event()
    self.set_is_ok(True)
    self.stopped = False

  def run(self):
    logger.info('Starting Aggregator Watchdog thread.')
    while not self.stopped:
      if 0 == self._stop_handler.wait(self.SLEEP_TIME):
        break
      try:
        conn = urllib2.urlopen(self.URL, timeout=self.CONNECTION_TIMEOUT)
        self.set_is_ok(True)
      except (KeyboardInterrupt, SystemExit):
        raise
      except Exception, e:
        self.set_is_ok(False)
        continue
      if conn.code != 200:
        self.set_is_ok(False)
        continue
      conn.close()

  def is_ok(self):
    return self._is_ok.is_set()

  def set_is_ok(self, value):
    if value == False and self.is_ok() != value:
      logger.warning("Watcher couldn't connect to aggregator.")
      self._is_ok.clear()
    else:
      self._is_ok.set()


  def stop(self):
    logger.info('Stopping watcher thread.')
    self.stopped = True



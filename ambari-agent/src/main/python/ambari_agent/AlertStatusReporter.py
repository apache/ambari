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
from ambari_stomp.adapter.websocket import ConnectionIsAlreadyClosed
from ambari_agent import Constants

logger = logging.getLogger(__name__)

class AlertStatusReporter(threading.Thread):
  def __init__(self, initializer_module):
    self.initializer_module = initializer_module
    self.collector = initializer_module.alert_scheduler_handler.collector()
    self.stop_event = initializer_module.stop_event
    self.alert_reports_interval = initializer_module.alert_reports_interval
    threading.Thread.__init__(self)

  def run(self):
    """
    Run an endless loop which reports all the alert statuses got from collector
    """
    if self.alert_reports_interval == 0:
      logger.warn("AlertStatusReporter is turned off. Some functionality might not work correctly.")
      return

    while not self.stop_event.is_set():
      try:
        if self.initializer_module.is_registered:
          alerts = self.collector.alerts()
          if alerts:
            self.initializer_module.connection.send(message=alerts, destination=Constants.ALERTS_STATUS_REPORTS_ENDPOINT)
      except ConnectionIsAlreadyClosed: # server and agent disconnected during sending data. Not an issue
        pass
      except:
        logger.exception("Exception in AlertStatusReporter. Re-running it")

      self.stop_event.wait(self.alert_reports_interval)

    logger.info("AlertStatusReporter has successfully finished")

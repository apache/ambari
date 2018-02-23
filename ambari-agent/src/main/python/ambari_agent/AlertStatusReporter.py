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
from collections import defaultdict
from ambari_stomp.adapter.websocket import ConnectionIsAlreadyClosed
from ambari_agent import Constants

logger = logging.getLogger(__name__)

class AlertStatusReporter(threading.Thread):
  """
  Thread sends alert reports to server. The report is only sent if its 'text' or 'state' has changed.
  This is done to reduce bandwidth usage on large clusters and number of operations (DB and others) done by server.
  """
  FIELDS_CHANGED_RESEND_ALERT = ['text', 'state']

  def __init__(self, initializer_module):
    self.initializer_module = initializer_module
    self.collector = initializer_module.alert_scheduler_handler.collector()
    self.stop_event = initializer_module.stop_event
    self.alert_reports_interval = initializer_module.config.alert_reports_interval
    self.stale_alerts_monitor = initializer_module.stale_alerts_monitor
    self.reported_alerts = defaultdict(lambda:defaultdict(lambda:[]))
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
          self.stale_alerts_monitor.save_executed_alerts(alerts)

          changed_alerts = self.get_changed_alerts(alerts)

          if changed_alerts and self.initializer_module.is_registered:
            self.initializer_module.connection.send(message=changed_alerts, destination=Constants.ALERTS_STATUS_REPORTS_ENDPOINT)
            self.save_results(changed_alerts)
      except ConnectionIsAlreadyClosed: # server and agent disconnected during sending data. Not an issue
        pass
      except:
        logger.exception("Exception in AlertStatusReporter. Re-running it")

      self.stop_event.wait(self.alert_reports_interval)

    logger.info("AlertStatusReporter has successfully finished")

  def save_results(self, alerts):
    """
    Save alert reports which were synced to server
    """
    for alert in alerts:
      cluster_id = alert['clusterId']
      alert_name = alert['name']

      self.reported_alerts[cluster_id][alert_name] = [alert[field] for field in self.FIELDS_CHANGED_RESEND_ALERT]

  def get_changed_alerts(self, alerts):
    """
    Get alert reports, which changed since last successful report to server
    """
    changed_alerts = []
    for alert in alerts:
      cluster_id = alert['clusterId']
      alert_name = alert['name']

      if [alert[field] for field in self.FIELDS_CHANGED_RESEND_ALERT] != self.reported_alerts[cluster_id][alert_name]:
        changed_alerts.append(alert)

    return changed_alerts

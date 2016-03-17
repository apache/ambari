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

import ConfigParser
import StringIO
import json
import os
from ambari_commons import OSConst
from ambari_commons.os_family_impl import OsFamilyImpl


#
# Abstraction for OS-dependent configuration defaults
#
class ConfigDefaults(object):
  def get_config_file_path(self):
    pass
  def get_metric_file_path(self):
    pass
  def get_ca_certs_file_path(self):
    pass

@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class ConfigDefaultsWindows(ConfigDefaults):
  def __init__(self):
    self._CONFIG_FILE_PATH = "conf\\metric_monitor.ini"
    self._METRIC_FILE_PATH = "conf\\metric_groups.conf"
    self._METRIC_FILE_PATH = "conf\\ca.pem"
    pass

  def get_config_file_path(self):
    return self._CONFIG_FILE_PATH
  def get_metric_file_path(self):
    return self._METRIC_FILE_PATH
  def get_ca_certs_file_path(self):
    return self._CA_CERTS_FILE_PATH

@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class ConfigDefaultsLinux(ConfigDefaults):
  def __init__(self):
    self._CONFIG_FILE_PATH = "/etc/ambari-metrics-monitor/conf/metric_monitor.ini"
    self._METRIC_FILE_PATH = "/etc/ambari-metrics-monitor/conf/metric_groups.conf"
    self._CA_CERTS_FILE_PATH = "/etc/ambari-metrics-monitor/conf/ca.pem"
    pass

  def get_config_file_path(self):
    return self._CONFIG_FILE_PATH
  def get_metric_file_path(self):
    return self._METRIC_FILE_PATH
  def get_ca_certs_file_path(self):
    return self._CA_CERTS_FILE_PATH

configDefaults = ConfigDefaults()

config = ConfigParser.RawConfigParser()

CONFIG_FILE_PATH = configDefaults.get_config_file_path()
METRIC_FILE_PATH = configDefaults.get_metric_file_path()
CA_CERTS_FILE_PATH = configDefaults.get_ca_certs_file_path()

OUT_DIR = os.path.join(os.sep, "var", "log", "ambari-metrics-host-monitoring")
SERVER_OUT_FILE = OUT_DIR + os.sep + "ambari-metrics-host-monitoring.out"
SERVER_LOG_FILE = OUT_DIR + os.sep + "ambari-metrics-host-monitoring.log"

PID_DIR = os.path.join(os.sep, "var", "run", "ambari-metrics-host-monitoring")
PID_OUT_FILE = PID_DIR + os.sep + "ambari-metrics-host-monitoring.pid"
EXITCODE_OUT_FILE = PID_DIR + os.sep + "ambari-metrics-host-monitoring.exitcode"

SERVICE_USERNAME_KEY = "TMP_AMHM_USERNAME"
SERVICE_PASSWORD_KEY = "TMP_AMHM_PASSWORD"

SETUP_ACTION = "setup"
START_ACTION = "start"
STOP_ACTION = "stop"
RESTART_ACTION = "restart"
STATUS_ACTION = "status"

AMBARI_AGENT_CONF = '/etc/ambari-agent/conf/ambari-agent.ini'

config_content = """
[default]
debug_level = INFO
enable_time_threshold = false
enable_value_threshold = false

[emitter]
send_interval = 60

[collector]
collector_sleep_interval = 5
max_queue_size = 5000
host = localhost
port = 6188
https_enabled = false
"""

metric_group_info = """
{
   "host_metric_groups": {
      "cpu_info": {
         "collect_every": "15",
         "metrics": [
            {
               "name": "cpu_user",
               "value_threshold": "1.0"
            }
         ]
      },
      "disk_info": {
         "collect_every": "30",
         "metrics": [
            {
               "name": "disk_free",
               "value_threshold": "5.0"
            }
         ]
      },
      "network_info": {
         "collect_every": "20",
         "metrics": [
            {
               "name": "bytes_out",
               "value_threshold": "128"
            }
         ]
      }
   },
   "process_metric_groups": {
      "": {
         "collect_every": "15",
         "metrics": []
      }
   }
}
"""


class Configuration:

  def __init__(self):
    global config_content
    self.config = ConfigParser.RawConfigParser()
    if os.path.exists(CONFIG_FILE_PATH):
      self.config.read(CONFIG_FILE_PATH)
    else:
      self.config.readfp(StringIO.StringIO(config_content))
    pass
    if os.path.exists(METRIC_FILE_PATH):
      with open(METRIC_FILE_PATH, 'r') as f:
        self.metric_groups = json.load(f)
    else:
      print 'No metric configs found at {0}'.format(METRIC_FILE_PATH)
      self.metric_groups = \
      {
        'host_metric_groups': [],
        'process_metric_groups': []
      }
    pass
    self._ca_cert_file_path = CA_CERTS_FILE_PATH
    self.hostname_script = None
    ambari_agent_config = ConfigParser.RawConfigParser()
    if os.path.exists(AMBARI_AGENT_CONF):
      try:
        ambari_agent_config.read(AMBARI_AGENT_CONF)
        self.hostname_script = ambari_agent_config.get('agent', 'hostname_script')
      except:
        # No hostname script identified in the ambari agent conf
        pass
    pass

  def getConfig(self):
    return self.config

  def getMetricGroupConfig(self):
    return self.metric_groups

  def get(self, section, key, default=None):
    try:
      value = str(self.config.get(section, key)).strip()
    except:
      return default
    return value

  def get_send_interval(self):
    return int(self.get("emitter", "send_interval", 60))

  def get_collector_sleep_interval(self):
    return int(self.get("collector", "collector_sleep_interval", 10))

  def get_hostname_config(self):
    return self.get("default", "hostname", None)

  def get_hostname_script(self):
    if self.hostname_script:
      return self.hostname_script
    else:
      return self.get("default", "hostname_script")

  def get_log_level(self):
    return self.get("default", "debug_level", "INFO")

  def get_max_queue_size(self):
    return int(self.get("collector", "max_queue_size", 5000))

  def is_server_https_enabled(self):
    return "true" == str(self.get("collector", "https_enabled")).lower()

  def get_server_host(self):
    return self.get("collector", "host")

  def get_server_port(self):
    try:
      return int(self.get("collector", "port"))
    except:
      return 6188

  def get_ca_certs(self):
    return self._ca_cert_file_path
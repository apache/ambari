#!/usr/bin/env python
"""
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

"""

from resource_management.core.logger import Logger
from resource_management.core.base import Fail
from resource_management import Script
from resource_management import Template
from resource_management.libraries.functions.curl_krb_request import curl_krb_request

from ambari_commons import OSConst
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from ambari_commons.parallel_processing import PrallelProcessResult, execute_in_parallel, SUCCESS

import httplib
import ambari_commons.network as network
import urllib
import ambari_simplejson as json # simplejson is much faster comparing to Python 2.6 json module and has the same functions set.
import os
import random
import time
import socket


class AMSServiceCheck(Script):
  AMS_METRICS_POST_URL = "/ws/v1/timeline/metrics/"
  AMS_METRICS_GET_URL = "/ws/v1/timeline/metrics?%s"
  AMS_CONNECT_TRIES = 10
  AMS_CONNECT_TIMEOUT = 10
  AMS_READ_TRIES = 5
  AMS_READ_TIMEOUT = 10

  @OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
  def service_check(self, env):
    from resource_management.libraries.functions.windows_service_utils import check_windows_service_exists
    import params

    env.set_params(params)

    #Just check that the services were correctly installed
    #Check the monitor on all hosts
    Logger.info("Metrics Monitor service check was started.")
    if not check_windows_service_exists(params.ams_monitor_win_service_name):
      raise Fail("Metrics Monitor service was not properly installed. Check the logs and retry the installation.")
    #Check the collector only where installed
    if params.ams_collector_home_dir and os.path.isdir(params.ams_collector_home_dir):
      Logger.info("Metrics Collector service check was started.")
      if not check_windows_service_exists(params.ams_collector_win_service_name):
        raise Fail("Metrics Collector service was not properly installed. Check the logs and retry the installation.")

  def service_check_for_single_host(self, metric_collector_host, params):
    random_value1 = random.random()

    current_time = int(time.time()) * 1000
    metric_json = Template('smoketest_metrics.json.j2', hostname=params.hostname, random1=random_value1,
                           current_time=current_time).get_content()
    try:
      if is_spnego_enabled(params):
        header= 'Content-Type: application/json'
        method = 'POST'
        tmp_dir = Script.get_tmp_dir()

        protocol = "http"
        if not callable(params.metric_collector_https_enabled):
          if params.metric_collector_https_enabled:
            protocol = "https"
        port = str(params.metric_collector_port)
        uri = '{0}://{1}:{2}{3}'.format(
        protocol, metric_collector_host, port, self.AMS_METRICS_POST_URL)

        call_curl_krb_request(tmp_dir, params.smoke_user_keytab, params.smoke_user_princ, uri, params.kinit_path_local, params.smoke_user,
                              self.AMS_CONNECT_TIMEOUT, method, metric_json, header, tries = self.AMS_CONNECT_TRIES)
      else :
        headers = {"Content-type": "application/json"}
        ca_certs = os.path.join(params.ams_monitor_conf_dir,
                                params.metric_truststore_ca_certs)
        post_metrics_to_collector(self.AMS_METRICS_POST_URL, metric_collector_host, params.metric_collector_port, params.metric_collector_https_enabled,
                                  metric_json, headers, ca_certs, self.AMS_CONNECT_TRIES, self.AMS_CONNECT_TIMEOUT)

      get_metrics_parameters = {
        "metricNames": "AMBARI_METRICS.SmokeTest.FakeMetric",
        "appId": "amssmoketestfake",
        "hostname": params.hostname,
        "startTime": current_time - 60000,
        "endTime": current_time + 61000,
        "precision": "seconds",
        "grouped": "false",
      }
      encoded_get_metrics_parameters = urllib.urlencode(get_metrics_parameters)

      if is_spnego_enabled(params):
        method = 'GET'
        uri = '{0}://{1}:{2}{3}'.format(
        protocol, metric_collector_host, port, self.AMS_METRICS_GET_URL % encoded_get_metrics_parameters)

        call_curl_krb_request(tmp_dir, params.smoke_user_keytab, params.smoke_user_princ, uri, params.kinit_path_local, params.smoke_user,
                              self.AMS_READ_TIMEOUT, method, tries = self.AMS_READ_TRIES, current_time = current_time, random_value = random_value1)
      else:
        Logger.info("Connecting (GET) to %s:%s%s" % (metric_collector_host,
                                                     params.metric_collector_port,
                                                     self.AMS_METRICS_GET_URL % encoded_get_metrics_parameters))
        for i in xrange(0, self.AMS_READ_TRIES):
          conn = network.get_http_connection(
            metric_collector_host,
            int(params.metric_collector_port),
            params.metric_collector_https_enabled,
            ca_certs,
            ssl_version=Script.get_force_https_protocol_value()
          )
          conn.request("GET", self.AMS_METRICS_GET_URL % encoded_get_metrics_parameters)
          response = conn.getresponse()
          Logger.info("Http response for host %s : %s %s" % (metric_collector_host, response.status, response.reason))

          data = response.read()
          Logger.info("Http data: %s" % data)
          conn.close()

          if response.status == 200:
            Logger.info("Metrics were retrieved from host %s" % metric_collector_host)
          else:
            raise Fail("Metrics were not retrieved from host %s. GET request status: %s %s \n%s" %
                       (metric_collector_host, response.status, response.reason, data))
          data_json = json.loads(data)

          def floats_eq(f1, f2, delta):
            return abs(f1-f2) < delta

          values_are_present = False
          for metrics_data in data_json["metrics"]:
            if (str(current_time) in metrics_data["metrics"] and str(current_time + 1000) in metrics_data["metrics"]
                and floats_eq(metrics_data["metrics"][str(current_time)], random_value1, 0.0000001)
                and floats_eq(metrics_data["metrics"][str(current_time + 1000)], current_time, 1)):
              Logger.info("Values %s and %s were found in the response from host %s." % (metric_collector_host, random_value1, current_time))
              values_are_present = True
              break
              pass

          if not values_are_present:
            if i < self.AMS_READ_TRIES - 1:  #range/xrange returns items from start to end-1
              Logger.info("Values weren't stored yet. Retrying in %s seconds."
                          % (self.AMS_READ_TIMEOUT))
              time.sleep(self.AMS_READ_TIMEOUT)
            else:
              raise Fail("Values %s and %s were not found in the response." % (random_value1, current_time))
          else:
            break
            pass
    except Fail as ex:
      Logger.warning("Ambari Metrics service check failed on collector host %s. Reason : %s" % (metric_collector_host, str(ex)))
      raise Fail("Ambari Metrics service check failed on collector host %s. Reason : %s" % (metric_collector_host, str(ex)))

  @OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
  def service_check(self, env):
    import params

    Logger.info("Ambari Metrics service check was started.")
    env.set_params(params)

    results = execute_in_parallel(self.service_check_for_single_host, params.ams_collector_hosts.split(','), params)

    for host in str(params.ams_collector_hosts).split(","):
      if host in results:
        if results[host].status == SUCCESS:
          Logger.info("Ambari Metrics service check passed on host " + host)
          return
        else:
          Logger.warning(results[host].result)
    raise Fail("All metrics collectors are unavailable.")

def is_spnego_enabled(params):
  return params.security_enabled \
      and 'core-site' in params.config['configurations'] \
      and 'hadoop.http.authentication.type' in params.config['configurations']['core-site'] \
      and params.config['configurations']['core-site']['hadoop.http.authentication.type'] == "kerberos" \
      and 'hadoop.http.filter.initializers' in params.config['configurations']['core-site'] \
      and params.config['configurations']['core-site']['hadoop.http.filter.initializers'] == "org.apache.hadoop.security.AuthenticationFilterInitializer"

def call_curl_krb_request(tmp_dir, user_keytab, user_princ, uri, kinit_path, user,
                          connection_timeout, method='GET', metric_json='', header='', tries = 1, current_time = 0, random_value = 0):
  if method == 'POST':
    Logger.info("Generated metrics for %s:\n%s" % (uri, metric_json))

  for i in xrange(0, tries):
    try:
      Logger.info("Connecting (%s) to %s" % (method, uri));

      response = None
      errmsg = None
      time_millis = 0

      response, errmsg, time_millis = curl_krb_request(tmp_dir, user_keytab, user_princ, uri, 'ams_service_check',
                                                       kinit_path, False, "AMS Service Check", user,
                                                       connection_timeout=connection_timeout, kinit_timer_ms=0,
                                                       method=method, body=metric_json, header=header)
    except Exception, exception:
      if i < tries - 1:  #range/xrange returns items from start to end-1
        time.sleep(connection_timeout)
        Logger.info("Connection failed for %s. Next retry in %s seconds."
                    % (uri, connection_timeout))
        continue
      else:
        raise Fail("Unable to {0} metrics on: {1}. Exception: {2}".format(method, uri, str(exception)))
    finally:
      if not response:
        Logger.error("Unable to {0} metrics on: {1}.  Error: {2}".format(method, uri, errmsg))
      else:
        Logger.info("%s response from %s: %s, errmsg: %s" % (method, uri, response, errmsg));
        try:
          response.close()
        except:
          Logger.debug("Unable to close {0} connection to {1}".format(method, uri))

    if method == 'GET':
      data_json = json.loads(response)

      def floats_eq(f1, f2, delta):
        return abs(f1-f2) < delta

      values_are_present = False
      for metrics_data in data_json["metrics"]:
        if (str(current_time) in metrics_data["metrics"] and str(current_time + 1000) in metrics_data["metrics"]
            and floats_eq(metrics_data["metrics"][str(current_time)], random_value, 0.0000001)
            and floats_eq(metrics_data["metrics"][str(current_time + 1000)], current_time, 1)):
          Logger.info("Values %s and %s were found in the response from %s." % (uri, random_value, current_time))
          values_are_present = True
          break
          pass

      if not values_are_present:
        if i < tries - 1:  #range/xrange returns items from start to end-1
          Logger.info("Values weren't stored yet. Retrying in %s seconds."
                      % (tries))
          time.sleep(connection_timeout)
        else:
          raise Fail("Values %s and %s were not found in the response." % (random_value, current_time))
      else:
        break
        pass
    else:
      break

def post_metrics_to_collector(ams_metrics_post_url, metric_collector_host, metric_collector_port, metric_collector_https_enabled,
                              metric_json, headers, ca_certs, tries = 1, connect_timeout = 10):
  for i in xrange(0, tries):
    try:
      Logger.info("Generated metrics for host %s :\n%s" % (metric_collector_host, metric_json))

      Logger.info("Connecting (POST) to %s:%s%s" % (metric_collector_host,
                                                    metric_collector_port,
                                                    ams_metrics_post_url))
      conn = network.get_http_connection(
        metric_collector_host,
        int(metric_collector_port),
        metric_collector_https_enabled,
        ca_certs,
        ssl_version=Script.get_force_https_protocol_value()
      )
      conn.request("POST", ams_metrics_post_url, metric_json, headers)

      response = conn.getresponse()
      Logger.info("Http response for host %s: %s %s" % (metric_collector_host, response.status, response.reason))
    except (httplib.HTTPException, socket.error) as ex:
      if i < tries - 1:  #range/xrange returns items from start to end-1
        time.sleep(connect_timeout)
        Logger.info("Connection failed for host %s. Next retry in %s seconds."
                    % (metric_collector_host, connect_timeout))
        continue
      else:
        raise Fail("Metrics were not saved. Connection failed.")

    data = response.read()
    Logger.info("Http data: %s" % data)
    conn.close()

    if response.status == 200:
      Logger.info("Metrics were saved.")
      break
    else:
      Logger.info("Metrics were not saved.")
      if i < tries - 1:  #range/xrange returns items from start to end-1
        time.sleep(tries)
        Logger.info("Next retry in %s seconds."
                    % (tries))
      else:
        raise Fail("Metrics were not saved. POST request status: %s %s \n%s" %
                   (response.status, response.reason, data))
if __name__ == "__main__":
  AMSServiceCheck().execute()


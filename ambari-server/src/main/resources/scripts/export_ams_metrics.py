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

import optparse
import sys
import os
import logging
import urllib2
import json
import datetime
import time
import re
import copy
from flask import Flask, Response, jsonify, request, abort
from flask.ext.cors import CORS
from flask_restful import Resource, Api, reqparse



class Params:

  AMS_HOSTNAME = 'localhost'
  AMS_PORT = '6188'
  AMS_APP_ID = None
  AMS_APP_ID_FORMATTED = None
  HOSTS_FILE = None
  METRICS_FILE = None
  OUT_DIR = None
  PRECISION = 'minutes'
  START_TIME = None
  END_TIME = None
  METRICS = []
  HOSTS = []
  METRICS_METADATA = {}
  FLASK_SERVER_NAME = None
  METRICS_FOR_HOSTS = {}
  HOSTS_WITH_COMPONENTS = {}

  @staticmethod
  def get_collector_uri(metricNames, hostname=None):
    if hostname:
      return 'http://{0}:{1}/ws/v1/timeline/metrics?metricNames={2}&hostname={3}&appId={4}&startTime={5}&endTime={6}&precision={7}' \
        .format(Params.AMS_HOSTNAME, Params.AMS_PORT, metricNames, hostname, Params.AMS_APP_ID,
                Params.START_TIME, Params.END_TIME, Params.PRECISION)
    else:
      return 'http://{0}:{1}/ws/v1/timeline/metrics?metricNames={2}&appId={3}&startTime={4}&endTime={5}&precision={6}' \
        .format(Params.AMS_HOSTNAME, Params.AMS_PORT, metricNames, Params.AMS_APP_ID, Params.START_TIME,
                Params.END_TIME, Params.PRECISION)

class Utils:

  @staticmethod
  def setup_logger(verbose, log_file):

    global logger
    logger = logging.getLogger('AmbariMetricsExport')
    formatter = logging.Formatter('%(asctime)s %(levelname)s %(message)s')
    if log_file:
      filehandler = logging.FileHandler(log_file)
    consolehandler = logging.StreamHandler()
    filehandler.setFormatter(formatter)
    consolehandler.setFormatter(formatter)
    logger.addHandler(filehandler)
    logger.addHandler(consolehandler)

    # set verbose
    if verbose:
      # logging.basicConfig(level=logging.DEBUG)
      logger.setLevel(logging.DEBUG)
    else:
      # logging.basicConfig(level=logging.INFO)
      logger.setLevel(logging.INFO)

  @staticmethod
  def get_data_from_url(collector_uri):
    req = urllib2.Request(collector_uri)
    connection = None
    try:
      connection = urllib2.urlopen(req)
    except Exception as e:
      logger.error('Error on metrics GET request: %s' % collector_uri)
      logger.error(str(e))
    # Validate json before dumping
    response_data = None
    if connection:
      try:
        response_data = json.loads(connection.read())
      except Exception as e:
        logger.warn('Error parsing json data returned from URI: %s' % collector_uri)
        logger.debug(str(e))

    return response_data

  @staticmethod
  def get_epoch(input):
    if (len(input) == 13):
      return int(input)
    elif (len(input) == 20):
      return int(time.mktime(datetime.datetime.strptime(input, '%Y-%m-%dT%H:%M:%SZ').timetuple()) * 1000)
    else:
      return -1


class AmsMetricsProcessor:

  @staticmethod
  def get_metrics_for_host(metrics, host=None):
    metrics_result = {}
    for metric in metrics:
      uri = Params.get_collector_uri(metric, host)
      logger.debug('Request URI: %s' % str(uri))
      metrics_dict = Utils.get_data_from_url(uri)
      if metrics_dict and "metrics" in metrics_dict and metrics_dict["metrics"]:
        metrics_result[metric] = metrics_dict
      else:
        logger.debug("No found metric {0} on host {1}".format(metric, host))

    return metrics_result

  @staticmethod
  def get_metrics_metadata():

    app_metrics_metadata = []
    for metric in Params.METRICS:
      app_metrics_metadata.append({"metricname": metric, "seriesStartTime": Params.START_TIME, "supportsAggregation": "true", "type": "UNDEFINED"})
      logger.debug("Adding {0} to metadata".format(metric))

    return {Params.AMS_APP_ID : app_metrics_metadata}

  @staticmethod
  def get_hosts_with_components():
    hosts_with_components = {}
    for host in Params.HOSTS:
      hosts_with_components[host] = [Params.AMS_APP_ID]
    return hosts_with_components

  @staticmethod
  def export_ams_metrics():

    if not os.path.exists(Params.METRICS_FILE):
      logger.error('Metrics file is required.')
      sys.exit(1)
    logger.info('Reading metrics file.')
    with open(Params.METRICS_FILE, 'r') as file:
      for line in file:
        Params.METRICS.append(line.strip())
    logger.info('Reading hosts file.')

    if Params.HOSTS_FILE and os.path.exists(Params.HOSTS_FILE):
      with open(Params.HOSTS_FILE, 'r') as file:
        for line in file:
          Params.HOSTS.append(line.strip())
    else:
      logger.info('No hosts file found, aggregate metrics will be exported.')
    hosts_metrics = {}
    if Params.HOSTS:
      for host in Params.HOSTS:
        hosts_metrics[host] = AmsMetricsProcessor.get_metrics_for_host(Params.METRICS, host)
      return hosts_metrics
    else:
      return AmsMetricsProcessor.get_metrics_for_host(Params.METRICS, None)

  def process(self):
    self.metrics_for_hosts = self.export_ams_metrics()
    self.metrics_metadata = self.get_metrics_metadata()
    self.hosts_with_components = self.get_hosts_with_components()


class FlaskServer():
  def __init__ (self, ams_metrics_processor):
    self.ams_metrics_processor = ams_metrics_processor
    app = Flask(__name__)
    api = Api(app)
    cors = CORS(app)

    api.add_resource(HostsResource, '/ws/v1/timeline/metrics/hosts', resource_class_kwargs={'ams_metrics_processor': self.ams_metrics_processor})
    api.add_resource(MetadataResource, '/ws/v1/timeline/metrics/metadata', resource_class_kwargs={'ams_metrics_processor': self.ams_metrics_processor})
    api.add_resource(MetricsResource, '/ws/v1/timeline/metrics', resource_class_kwargs={'ams_metrics_processor': self.ams_metrics_processor})

    logger.info("Start Flask server. Server URL = " + Params.FLASK_SERVER_NAME + ":5000")

    app.run(debug=True,
            host=Params.FLASK_SERVER_NAME,
            port=5000)

class MetadataResource(Resource):
  def __init__ (self, ams_metrics_processor):
    self.ams_metrics_processor = ams_metrics_processor

  def get(self):
    if self.ams_metrics_processor.metrics_metadata:
      return jsonify(self.ams_metrics_processor.metrics_metadata)
    else:
      abort(404)

class HostsResource(Resource):
  def __init__ (self, ams_metrics_processor):
    self.ams_metrics_processor = ams_metrics_processor

  def get(self):
    if self.ams_metrics_processor.hosts_with_components:
      return jsonify(self.ams_metrics_processor.hosts_with_components)
    else:
      abort(404)

class MetricsResource(Resource):
  def __init__ (self, ams_metrics_processor):
    self.ams_metrics_processor = ams_metrics_processor

  def get(self):
    args = request.args
    separator = "._"
    if not "metricNames" in args:
      logger.error("Bad request")
      abort(404)
    metric_name, operation = args["metricNames"].split(separator, 1)
    metric_dict = {"metrics" : []}

    if "hostname" in args and args["hostname"] != "":
      host_names = args["hostname"].split(",")
      metric_dict = {"metrics" : []}
      for host_name in host_names:
        if metric_name in self.ams_metrics_processor.metrics_for_hosts[host_name]:
          metric_dict["metrics"].append(self.ams_metrics_processor.metrics_for_hosts[host_name][metric_name]["metrics"][0])
        else:
          continue
    else:
      for host in self.ams_metrics_processor.metrics_for_hosts:
        for metric in self.ams_metrics_processor.metrics_for_hosts[host]:
          if metric_name == metric:
            metric_dict = self.ams_metrics_processor.metrics_for_hosts[host][metric_name]
            break

    if metric_dict:
      metrics_json_new = copy.copy(metric_dict)
      for i in range (0, len(metrics_json_new["metrics"])):
        metrics_json_new["metrics"][i]["metricname"] += separator + operation
      return jsonify(metrics_json_new)
    else :
      abort(404)
      return

#
# Main.
#
def main():
  parser = optparse.OptionParser(usage="usage: %prog [options]")
  parser.set_description('This python program is a Ambari thin client and '
                         'supports export of ambari metric data for an app '
                         'from Ambari Metrics Service to a output dir. '
                         'The metrics will be exported to a file with name of '
                         'the metric and in a directory with the name as the '
                         'hostname under the output dir.')

  d = datetime.datetime.now()
  time_suffix = '{0}-{1}-{2}-{3}-{4}-{5}'.format(d.year, d.month, d.day,
                                                 d.hour, d.minute, d.second)
  print 'Time: %s' % time_suffix
  logfile = os.path.join('/tmp', 'ambari_metrics_export.out')

  parser.add_option("-v", "--verbose", dest="verbose", action="store_false",
                    default=False, help="output verbosity.")
  parser.add_option("-s", "--host", dest="server_hostname",
                    help="AMS host name.")
  parser.add_option("-p", "--port", dest="server_port",
                    default="6188", help="AMS port. [default: 6188]")
  parser.add_option("-a", "--app-id", dest="app_id",
                    help="AMS app id.")
  parser.add_option("-m", "--metrics-file", dest="metrics_file",
                    help="Metrics file with metric names to query. New line separated.")
  parser.add_option("-f", "--host-file", dest="hosts_file",
                    help="Host file with hostnames to query. New line separated.")
  parser.add_option("-l", "--logfile", dest="log_file", default=logfile,
                    metavar='FILE', help="Log file. [default: %s]" % logfile)
  parser.add_option("-r", "--precision", dest="precision",
                    default='minutes', help="AMS API precision, default = minutes.")
  parser.add_option("-b", "--start_time", dest="start_time",
                    help="Start time in milliseconds since epoch or UTC timestamp in YYYY-MM-DDTHH:mm:ssZ format.")
  parser.add_option("-e", "--end_time", dest="end_time",
                    help="End time in milliseconds since epoch or UTC timestamp in YYYY-MM-DDTHH:mm:ssZ format.")
  parser.add_option("-n", "--flask-server_name", dest="server_name",
                    help="Flask server name, default = 127.0.0.1", default="127.0.0.1")

  (options, args) = parser.parse_args()

  Params.AMS_HOSTNAME = options.server_hostname

  Params.AMS_PORT = options.server_port

  Params.AMS_APP_ID = options.app_id

  if Params.AMS_APP_ID != "HOST":
    Params.AMS_APP_ID = Params.AMS_APP_ID.lower()

  Params.METRICS_FILE = options.metrics_file

  Params.HOSTS_FILE = options.hosts_file

  Params.PRECISION = options.precision

  Params.FLASK_SERVER_NAME = options.server_name

  Utils.setup_logger(options.verbose, options.log_file)

  Params.START_TIME = Utils.get_epoch(options.start_time)

  if Params.START_TIME == -1:
    logger.warn('No start time provided, or it is in the wrong format. Please '
                'provide milliseconds since epoch or a value in YYYY-MM-DDTHH:mm:ssZ format')
    logger.info('Aborting...')
    sys.exit(1)

  Params.END_TIME = Utils.get_epoch(options.end_time)

  if Params.END_TIME == -1:
    logger.warn('No end time provided, or it is in the wrong format. Please '
                'provide milliseconds since epoch or a value in YYYY-MM-DDTHH:mm:ssZ format')
    logger.info('Aborting...')
    sys.exit(1)

  ams_metrics_processor = AmsMetricsProcessor()
  ams_metrics_processor.process()
  FlaskServer(ams_metrics_processor)


if __name__ == "__main__":
  try:
    main()
  except (KeyboardInterrupt, EOFError):
    print("\nAborting ... Keyboard Interrupt.")
    sys.exit(1)

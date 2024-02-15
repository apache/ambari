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

import urllib2
import signal
import sys
import optparse
import time

# http://162.216.148.45:6188/ws/v1/timeline/metrics?
# metricNames=rpc.rpc.RpcAuthenticationSuccesses
# &appId=nodemanager&hostname=local.0&startTime=1414152029&endTime=1414155629

AMS_URL = "http://{0}:6188/ws/v1/timeline/metrics?metricNames={1}&appid={" \
          "2}&hostname={3}"

# in fact it can be list automatically generated from ambari
# UI queries
host_metrics = {
  'cpu': ['cpu_user', 'cpu_wio', 'cpu_nice', 'cpu_aidle', 'cpu_system', 'cpu_idle'],
  'disk': ['disk_total', 'disk_free'],
  'load': ['load_one', 'load_fifteen', 'load_five'],
  'mem': ['swap_free', 'mem_shared', 'mem_free', 'mem_cached', 'mem_buffers'],
  'network': ['bytes_in', 'bytes_out', 'pkts_in', 'pkts_out'],
  'process': ['proc_total', 'proc_run']
}

# HDFS_SERVICE
namenode_metrics = {
  'dfs.Capacity': ['dfs.FSNamesystem.CapacityRemainingGB',
                   'dfs.FSNamesystem.CapacityUsedGB',
                   'dfs.FSNamesystem.CapacityTotalGB'],
  'dfs.Replication': ['dfs.FSNamesystem.PendingReplicationBlocks',
                      'dfs.FSNamesystem.UnderReplicatedBlocks'],
  'dfs.File': ['dfs.namenode.FileInfoOps', 'dfs.namenode.CreateFileOps'],
  'jvm.gc': ['jvm.JvmMetrics.GcTimeMillis'],
  'jvm.mem': ['jvm.JvmMetrics.MemNonHeapUsedM',
              'jvm.JvmMetrics.MemNonHeapCommittedM',
              'jvm.JvmMetrics.MemHeapUsedM',
              'jvm.JvmMetrics.MemHeapCommittedM'],
  'jvm.thread': ['jvm.JvmMetrics.ThreadsRunnable',
                 'jvm.JvmMetrics.ThreadsBlocked',
                 'jvm.JvmMetrics.ThreadsWaiting',
                 'jvm.JvmMetrics.ThreadsTimedWaiting'],
  'rpc': ['rpc.rpc.RpcQueueTimeAvgTime']
}

all_metrics = {
  'HOST': host_metrics,
  'namenode': namenode_metrics
}

all_metrics_times = {}


# hostnames = ['EPPLKRAW0101.0']  # 'local.0'
# metrics_test_host = '162.216.150.247' # metricstest-100
# metrics_test_host = '162.216.148.45'    # br-3
# start_time = int(time.time())           # 1414425208


def main(argv=None):
  # Allow Ctrl-C
  signal.signal(signal.SIGINT, signal_handler)

  parser = optparse.OptionParser()

  parser.add_option("-H", "--host", dest="host",
                    help="Ambari Metrics host")
  parser.add_option("-t", "--starttime", dest="start_time_secs",
                    default=int(time.time()),
                    help="start time in seconds, default value is current time")
  parser.add_option("-n", "--nodes", dest="node_names",
                    help="nodes from cluster, used as a param to query for")
  (options, args) = parser.parse_args()

  if options.host is None:
    print "Ambari Metrics host name is required (--host or -h)"
    exit(-1)

  if options.node_names is None:
    print "cluster nodes are required (--nodes or -n)"
    exit(3)

  global start_time_secs, metrics_test_host, hostnames

  metrics_test_host = options.host
  start_time_secs = int(options.start_time_secs)
  hostnames = [options.node_names]

  while True:
    run()
    time.sleep(15)
    start_time_secs += 15


def signal_handler(signal, frame):
  print('Exiting, Ctrl+C press detected!')
  print_all_metrics(all_metrics_times)
  sys.exit(0)


def run():
  hostname = ','.join(hostnames)
  qs = QuerySender(metrics_test_host, True)
  for metric_name in all_metrics:
    print
    print 'Querying for ' + metric_name + ' metrics'
    current_time_secs = start_time_secs
    qs.query_all_app_metrics(hostname, metric_name,
                             all_metrics[metric_name],
                             current_time_secs)


def add_query_metrics_for_app_id(app_id, metric_timing):
  if not app_id in all_metrics_times:
    all_metrics_times[app_id] = []
  all_metrics_times[app_id].append(metric_timing)


def print_all_metrics(metrics):
  print 'Metrics Summary'
  for app_id in sorted(metrics):
    first = True
    for single_query_metrics in metrics[app_id]:
      print_app_metrics(app_id, single_query_metrics, first)
      first = False


def print_app_metrics(app_id, metric_timing, header=False):
  #header
  if header:
    print app_id + ': ' + ','.join(sorted(metric_timing.keys()))
  #vals
  print app_id + ':',
  for key in sorted(metric_timing):
    print '%.3f,' % metric_timing[key],
  print


class QuerySender:
  def __init__(self, metrics_address, print_responses=False):
    self.metrics_address = metrics_address
    self.print_responses = print_responses

  def query_all_app_metrics(self, hostname, app_id, metrics, current_time_secs):
    metric_timing = {}
    for key in metrics:
      print 'Getting metrics for', key
      query_time = time.time()

      metric_names = ','.join(metrics[key])
      self.query(hostname, app_id, metric_names, current_time_secs)
      query_time_elapsed = time.time() - query_time

      print 'Query for "%s" took %s' % (key, query_time_elapsed)
      metric_timing[key] = query_time_elapsed

    add_query_metrics_for_app_id(app_id, metric_timing)
    if self.print_responses:
      print_app_metrics(app_id, metric_timing)

  def query(self, hostname, app_id, metric_names, current_time_secs):
    url = self.create_url(hostname, metric_names, app_id, current_time_secs)
    print url
    response = self.send(url)
    if self.print_responses:
      print response
    pass

  def send(self, url):
    request = urllib2.Request(url)
    try:
      response = urllib2.urlopen(request, timeout=int(30))
      response = response.read()
      return response

    except urllib2.URLError as e:
      print e.reason

  def create_url(self, hostname, metric_names, app_id, current_time_secs):
    server = AMS_URL.format(self.metrics_address,
                            metric_names,
                            app_id,
                            hostname)
    t = current_time_secs
    server += '&startTime=%s&endTime=%s' % (t, t + 3600)
    return server


if __name__ == '__main__':
  main()

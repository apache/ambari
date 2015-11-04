# !/usr/bin/env python
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

import os
import sys
import re
import math
import collections
import ast

metric_filename_ext = '.txt'
# 5 regions for higher order aggregate tables
other_region_static_count = 6
# Max equidistant points to return per service
max_equidistant_points = 50

b_bytes = 1
k_bytes = 1 << 10  # 1024
m_bytes = 1 << 20  # 1024^2
g_bytes = 1 << 30  # 1024^3
t_bytes = 1 << 40  # 1024^4
p_bytes = 1 << 50  # 1024^5

def to_number(s):
  try:
    return int(re.sub("\D", "", s))
  except ValueError:
    return None

def format_Xmx_size_to_bytes(value, default='b'):
  strvalue = str(value).lower()
  if len(strvalue) == 0:
    return 0
  modifier = strvalue[-1]

  if modifier == ' ' or modifier in "0123456789":
    modifier = default

  m = {
    modifier == 'b': b_bytes,
    modifier == 'k': k_bytes,
    modifier == 'm': m_bytes,
    modifier == 'g': g_bytes,
    modifier == 't': t_bytes,
    modifier == 'p': p_bytes
    } [1]
  return to_number(strvalue) * m

# Class that takes AMS HBase configs as input and determines the Region
# pre-splits based on selected services also passed as a parameter to the class.
class FindSplitPointsForAMSRegions():

  def __init__(self, ams_hbase_site, ams_hbase_env, serviceMetricsDir,
               operation_mode = 'embedded', services = None):
    self.ams_hbase_site = ams_hbase_site
    self.ams_hbase_env = ams_hbase_env
    self.serviceMetricsDir = serviceMetricsDir
    self.services = services
    self.mode = operation_mode
    # Add host metrics if not present as input
    if self.services and 'HOST' not in self.services:
      self.services.append('HOST')

    # Initialize before user
    self.initialize()

  def initialize(self):
    # calculate regions based on available memory
    self.initialize_region_counts()
    self.initialize_ordered_set_of_metrics()

  def initialize_region_counts(self):
    try:
      xmx_master_bytes = format_Xmx_size_to_bytes(self.ams_hbase_env['hbase_master_heapsize'], 'm')
      xmx_region_bytes = 0
      if "hbase_regionserver_heapsize" in self.ams_hbase_env:
        xmx_region_bytes = format_Xmx_size_to_bytes(self.ams_hbase_env['hbase_regionserver_heapsize'], 'm')
      xmx_bytes = xmx_master_bytes + xmx_region_bytes
      if self.mode == 'distributed':
        xmx_bytes = xmx_region_bytes

      memstore_max_mem = float(self.ams_hbase_site['hbase.regionserver.global.memstore.lowerLimit']) * xmx_bytes
      memstore_flush_size = format_Xmx_size_to_bytes(self.ams_hbase_site['hbase.hregion.memstore.flush.size'])

      max_inmemory_regions = (memstore_max_mem / memstore_flush_size) - other_region_static_count
      print 'max_inmemory_regions: %s' % max_inmemory_regions

      if max_inmemory_regions > 2:
        # Lets say total = 12, so we have 7 regions to allocate between
        # METRIC_RECORD and METRIC_AGGREGATE tables, desired = (5, 2)
        self.desired_precision_region_count = int(math.floor(0.8 * max_inmemory_regions))
        self.desired_aggregate_region_count = int(max_inmemory_regions - self.desired_precision_region_count)
      else:
        self.desired_precision_region_count = 1
        self.desired_aggregate_region_count = 1

    except:
      print('Bad config settings, could not calculate max regions available.')
    pass

  def initialize_ordered_set_of_metrics(self):
    onlyServicefiles = [ f for f in os.listdir(self.serviceMetricsDir) if
                  os.path.isfile(os.path.join(self.serviceMetricsDir, f)) ]

    metrics = set()

    for file in onlyServicefiles:
      # Process for services selected at deploy time or all services if
      # services arg is not passed
      if self.services is None or file.rstrip(metric_filename_ext) in self.services:
        print 'Processing file: %s' % os.path.join(self.serviceMetricsDir, file)
        service_metrics = set()
        with open(os.path.join(self.serviceMetricsDir, file), 'r') as f:
          for metric in f:
            service_metrics.add(metric.strip())
          pass
        pass
        metrics.update(self.find_equidistant_metrics(list(sorted(service_metrics))))
      pass
    pass

    self.metrics = sorted(metrics)
    print 'metrics length: %s' % len(self.metrics)

  # Pick 50 metric points for each service that are equidistant from
  # each other for a service
  def find_equidistant_metrics(self, service_metrics):
    equi_metrics = []
    idx = len(service_metrics) / max_equidistant_points
    if idx == 0:
      return service_metrics
    pass

    index = idx
    for i in range(0, max_equidistant_points - 1):
      equi_metrics.append(service_metrics[index - 1])
      index += idx
    pass

    return equi_metrics

  def get_split_points(self):
    split_points = collections.namedtuple('SplitPoints', [ 'precision', 'aggregate' ])
    split_points.precision = []
    split_points.aggregate = []

    metric_list = list(self.metrics)
    metrics_total = len(metric_list)

    if self.desired_precision_region_count > 1:
      idx = int(math.ceil(metrics_total / self.desired_precision_region_count))
      index = idx
      for i in range(0, self.desired_precision_region_count - 1):
        if index < metrics_total - 1:
          split_points.precision.append(metric_list[index])
          index += idx

    if self.desired_aggregate_region_count > 1:
      idx = int(math.ceil(metrics_total / self.desired_aggregate_region_count))
      index = idx
      for i in range(0, self.desired_aggregate_region_count - 1):
        if index < metrics_total - 1:
          split_points.aggregate.append(metric_list[index])
          index += idx

    return split_points
  pass

def main(argv = None):
  scriptDir = os.path.realpath(os.path.dirname(argv[0]))
  serviceMetricsDir = os.path.join(scriptDir, os.pardir, 'files', 'service-metrics')

  if os.path.exists(serviceMetricsDir):
    onlyargs = argv[1:]
    if len(onlyargs) < 3:
      sys.stderr.write("Usage: dict(ams-hbase-site) dict(ams-hbase-env) list(services)\n")
      sys.exit(2)
    pass

    ams_hbase_site = None
    ams_hbase_env = None
    services = None
    try:
      ams_hbase_site = ast.literal_eval(str(onlyargs[0]))
      ams_hbase_env = ast.literal_eval(str(onlyargs[1]))
      services = onlyargs[2]
      if services:
        services = str(services).split(',')
      pass
    except Exception, ex:
      sys.stderr.write(str(ex))
      sys.stderr.write("\nUsage: Expected items not found in input. Found "
                      " ams-hbase-site => {0}, ams-hbase-env => {1},"
                      " services => {2}".format(ams_hbase_site, ams_hbase_env, services))
      sys.exit(2)

    print '--------- AMS Regions Split point finder ---------'
    print 'Services: %s' % services

    mode = 'distributed' if 'hbase.rootdir' in ams_hbase_site and \
                            'hdfs' in ams_hbase_site['hbase.rootdir'] else \
                            'embedded'

    split_point_finder = FindSplitPointsForAMSRegions(
      ams_hbase_site, ams_hbase_env, serviceMetricsDir, mode, services)

    result = split_point_finder.get_split_points()
    print 'Split points for precision table : %s' % len(result.precision)
    print 'precision: %s' % str(result.precision)
    print 'Split points for aggregate table : %s' % len(result.aggregate)
    print 'aggregate: %s' % str(result.aggregate)

    return 0

  else:
    print 'Cannot find service metrics dir in %s' % scriptDir

if __name__ == '__main__':
  main(sys.argv)

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

import cgi
import os
import rrdtool
import sys
import time

# place this script in /var/www/cgi-bin of the Ganglia collector
# requires 'yum install rrdtool-python' on the Ganglia collector


def printMetric(clusterName, hostName, metricName, file, cf, start, end, resolution, pointInTime):
  if clusterName.endswith("rrds"):
    clusterName = ""

  args = [file, cf]

  if start is not None:
    args.extend(["-s", start])

  if end is not None:
    args.extend(["-e", end])

  if resolution is not None:
    args.extend(["-r", resolution])

  rrdMetric = rrdtool.fetch(args)
  # ds_name
  sys.stdout.write(rrdMetric[1][0])
  sys.stdout.write("\n")

  sys.stdout.write(clusterName)
  sys.stdout.write("\n")
  sys.stdout.write(hostName)
  sys.stdout.write("\n")
  sys.stdout.write(metricName)
  sys.stdout.write("\n")

  # write time
  sys.stdout.write(str(rrdMetric[0][0]))
  sys.stdout.write("\n")
  # write step
  sys.stdout.write(str(rrdMetric[0][2]))
  sys.stdout.write("\n")

  if not pointInTime:
    for tuple in rrdMetric[2]:
      if tuple[0] is not None:
        sys.stdout.write(str(tuple[0]))
        sys.stdout.write("\n")
  else:
    value = None
    idx   = -1
    tuple = rrdMetric[2]
    tupleLastIdx = len(tuple) * -1

    while value is None and idx >= tupleLastIdx:
      value = tuple[idx][0]
      idx-=1

    if value is not None:
      sys.stdout.write(str(value))
      sys.stdout.write("\n")

  sys.stdout.write("[AMBARI_DP_END]\n")
  return

def stripList(l):
  return([x.strip() for x in l])

sys.stdout.write("Content-type: text/plain\n\n")

# write start time
sys.stdout.write(str(time.mktime(time.gmtime())))
sys.stdout.write("\n")

queryString = dict(cgi.parse_qsl(os.environ['QUERY_STRING']));

if "m" in queryString:
  metricParts = queryString["m"].split(",")
else:
  metricParts = [""]
metricParts = stripList(metricParts)

hostParts = []
if "h" in queryString:
  hostParts = queryString["h"].split(",")
hostParts = stripList(hostParts)

if "c" in queryString:
  clusterParts = queryString["c"].split(",")
else:
  clusterParts = [""]
clusterParts = stripList(clusterParts)

if "p" in queryString:
  rrdPath = queryString["p"]
else:
  rrdPath = "/var/lib/ganglia/rrds/"

start = None
if "s" in queryString:
  start = queryString["s"]

end = None
if "e" in queryString:
  end = queryString["e"]

resolution = None
if "r" in queryString:
  resolution = queryString["r"]

if "cf" in queryString:
  cf = queryString["cf"]
else:
  cf = "AVERAGE"

if "pt" in queryString:
  pointInTime = True
else:
  pointInTime = False

for cluster in clusterParts:
  for path, dirs, files in os.walk(rrdPath + cluster):
    pathParts = path.split("/")
    if len(hostParts) == 0 or pathParts[-1] in hostParts:
      for file in files:
        for metric in metricParts:
          if file.endswith(metric + ".rrd"):

            printMetric(pathParts[-2], pathParts[-1], file[:-4],
                os.path.join(path, file), cf, start, end, resolution, pointInTime)

sys.stdout.write("[AMBARI_END]\n")
# write end time
sys.stdout.write(str(time.mktime(time.gmtime())))
sys.stdout.write("\n")

sys.stdout.flush

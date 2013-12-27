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
import re
import urlparse

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
    valueCount = 0
    lastValue = None

    for tuple in rrdMetric[2]:

      thisValue = tuple[0]

      if valueCount > 0 and thisValue == lastValue:
        valueCount += 1
      else:
        if valueCount > 1:
          sys.stdout.write("[~r]")
          sys.stdout.write(str(valueCount))
          sys.stdout.write("\n")

        if thisValue is None:
          sys.stdout.write("[~n]\n")
        else:
          sys.stdout.write(str(thisValue))
          sys.stdout.write("\n")

        valueCount = 1
        lastValue = thisValue
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

  sys.stdout.write("[~EOM]\n")
  return

def stripList(l):
  return([x.strip() for x in l])

sys.stdout.write("Content-type: text/plain\n\n")

# write start time
sys.stdout.write(str(time.mktime(time.gmtime())))
sys.stdout.write("\n")

requestMethod = os.environ['REQUEST_METHOD']

if requestMethod == 'POST':
  postData = sys.stdin.readline()
  queryString = cgi.parse_qs(postData)
  queryString = dict((k, v[0]) for k, v in queryString.items())
elif requestMethod == 'GET':
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

def _walk(*args, **kwargs):

  for root,dirs,files in os.walk(*args, **kwargs):
    for dir in dirs:
      qualified_dir = os.path.join(root,dir)
      if os.path.islink(qualified_dir):
        for x in os.walk(qualified_dir, **kwargs):
          yield x
    yield (root, dirs, files)


for cluster in clusterParts:
  for path, dirs, files in _walk(rrdPath + cluster):
    pathParts = path.split("/")
    #Process only path which contains files. If no host parameter passed - process all hosts folders and summary info
    #If host parameter passed - process only this host folder
    if len(files) > 0 and (len(hostParts) == 0 or pathParts[-1] in hostParts):
      for metric in metricParts:
        file = metric + ".rrd"
        fileFullPath = os.path.join(path, file)
        if os.path.exists(fileFullPath):
          #Exact name of metric
          printMetric(pathParts[-2], pathParts[-1], file[:-4], os.path.join(path, file), cf, start, end, resolution, pointInTime)
        else:
          #Regex as metric name
          metricRegex = metric + '\.rrd$'
          p = re.compile(metricRegex)
          matchedFiles = filter(p.match, files)
          for matchedFile in matchedFiles:
            printMetric(pathParts[-2], pathParts[-1], matchedFile[:-4], os.path.join(path, matchedFile), cf, start, end, resolution, pointInTime)


sys.stdout.write("[~EOF]\n")
# write end time
sys.stdout.write(str(time.mktime(time.gmtime())))
sys.stdout.write("\n")

sys.stdout.flush
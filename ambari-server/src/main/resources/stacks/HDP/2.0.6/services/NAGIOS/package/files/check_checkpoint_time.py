#!/usr/bin/env python
#
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
#

import os
import optparse
import time
import urllib2
import json

CRIT_MESSAGE = "CRITICAL: Last checkpoint time is below acceptable. Checkpoint was done {h}h. {m}m. ago"
WARNING_MESSAGE = "WARNING: Last checkpoint time is below acceptable. Checkpoint was done {h}h. {m}m. ago"
OK_MESSAGE = "OK: Last checkpoint time"
WARNING_JMX_MESSAGE = "WARNING: NameNode JMX not accessible"

def main():
  current_time = int(round(time.time() * 1000))

  parser = optparse.OptionParser()

  parser.add_option("-H", "--host", dest="host",
                    default="localhost", help="NameNode host")
  parser.add_option("-p", "--port", dest="port",
                    default="50070", help="NameNode jmx port")
  parser.add_option("-s", "--ssl-enabled", dest="is_ssl_enabled",
                    default=False, help="SSL Enabled")  
  parser.add_option("-w", "--warning", dest="warning",
                    default="200", help="Percent for warning alert")
  parser.add_option("-c", "--critical", dest="crit",
                    default="200", help="Percent for critical alert")
  parser.add_option("-t", "--period", dest="period",
                    default="21600", help="Period time")
  parser.add_option("-x", "--txns", dest="txns",
                    default="1000000",
                    help="CheckpointNode will create a checkpoint of the namespace every 'dfs.namenode.checkpoint.txns'")
  
  (options, args) = parser.parse_args()

  scheme = "http"
  if options.is_ssl_enabled == "true":
    scheme = "https"

  host = get_available_nn_host(options,scheme)

  last_checkpoint_time_qry = "{scheme}://{host}:{port}/jmx?qry=Hadoop:service=NameNode,name=FSNamesystem".format(
      scheme=scheme, host=host, port=options.port)

  print last_checkpoint_time_qry
    
  last_checkpoint_time = int(get_value_from_jmx(last_checkpoint_time_qry,"LastCheckpointTime"))

  journal_transaction_info_qry = "{scheme}://{host}:{port}/jmx?qry=Hadoop:service=NameNode,name=NameNodeInfo".format(
      scheme=scheme, host=host, port=options.port)
  
  journal_transaction_info = get_value_from_jmx(journal_transaction_info_qry,"JournalTransactionInfo")
  journal_transaction_info_dict = json.loads(journal_transaction_info)

  last_txid = int(journal_transaction_info_dict['LastAppliedOrWrittenTxId'])
  most_txid = int(journal_transaction_info_dict['MostRecentCheckpointTxId'])

  delta = (current_time - last_checkpoint_time)/1000

  if ((last_txid - most_txid) > int(options.txns)) and (float(delta) / int(options.period)*100 >= int(options.crit)):
    print CRIT_MESSAGE.format(h=get_time(delta)['h'], m=get_time(delta)['m'])
    exit(2)
  elif ((last_txid - most_txid) > int(options.txns)) and (float(delta) / int(options.period)*100 >= int(options.warning)):
    print WARNING_MESSAGE.format(h=get_time(delta)['h'], m=get_time(delta)['m'])
    exit(1)
  else:
    print OK_MESSAGE
    exit(0)


def get_time(delta):
  h = int(delta/3600)
  m = int((delta % 3600)/60)
  return {'h':h, 'm':m}


def get_value_from_jmx(qry, property):
  try:
    response = urllib2.urlopen(qry)
    data=response.read()
  except Exception:
    print WARNING_JMX_MESSAGE
    exit(1)

  data_dict = json.loads(data)
  return data_dict["beans"][0][property]


def get_available_nn_host(options, scheme):
  nn_hosts = options.host.split(" ")
  for nn_host in nn_hosts:
    try:
      urllib2.urlopen("{scheme}://{host}:{port}/jmx".format(scheme=scheme, host=nn_host, port=options.port))
      return nn_host
    except Exception:
      pass
  print WARNING_JMX_MESSAGE
  exit(1)


if __name__ == "__main__":
  main()
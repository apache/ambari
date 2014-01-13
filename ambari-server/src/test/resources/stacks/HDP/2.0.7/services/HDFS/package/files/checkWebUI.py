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
import httplib

#
# Main.
#
def main():
  parser = optparse.OptionParser(usage="usage: %prog [options] component ")
  parser.add_option("-m", "--hosts", dest="hosts", help="Comma separated hosts list for WEB UI to check it availability")
  parser.add_option("-p", "--port", dest="port", help="Port of WEB UI to check it availability")

  (options, args) = parser.parse_args()
  
  hosts = options.hosts.split(',')
  port = options.port

  for host in hosts:
    try:
      conn = httplib.HTTPConnection(host, port)
      # This can be modified to get a partial url part to be sent with request
      conn.request("GET", "/")
      httpCode = conn.getresponse().status
      conn.close()
    except Exception:
      httpCode = 404

    if httpCode != 200:
      print "Cannot access WEB UI on: http://" + host + ":" + port
      exit(1)
      

if __name__ == "__main__":
  main()

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
import optparse
import socket

#
# Main.
#
def main():
  parser = optparse.OptionParser(usage="usage: %prog [options]")
  parser.add_option("-p", "--port", dest="port", help="Port for Knox process")
  parser.add_option("-n", "--hostname", dest="hostname", help="Hostname of Knox Gateway component")

  (options, args) = parser.parse_args()
  timeout_seconds = 5
  try:
    s = socket.create_connection((options.hostname, int(options.port)),timeout=timeout_seconds)
    print "Successfully connected to %s on port %s" % (options.hostname, options.port)
    s.close()
  except socket.error, e:
    print "Connection to %s on port %s failed: %s" % (options.hostname, options.port, e)
    exit(1)

if __name__ == "__main__":
  main()


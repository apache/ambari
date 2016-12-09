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
import socket
import ssl

class TLS1HTTPSConnection(httplib.HTTPSConnection):
  """
  Some of python implementations does not work correctly with sslv3 but trying to use it, we need to change protocol to
  tls1.
  """
  def __init__(self, host, port, **kwargs):
    httplib.HTTPSConnection.__init__(self, host, port, **kwargs)

  def connect(self):
    sock = socket.create_connection((self.host, self.port), self.timeout)
    if getattr(self, '_tunnel_host', None):
      self.sock = sock
      self._tunnel()
    self.sock = ssl.wrap_socket(sock, self.key_file, self.cert_file, ssl_version=ssl.PROTOCOL_TLSv1)

def make_connection(host, port, https):
  try:
    conn = httplib.HTTPConnection(host, port) if not https else httplib.HTTPSConnection(host, port)
    conn.request("GET", "/")
    return conn.getresponse().status
  except ssl.SSLError:
    # got ssl error, lets try to use TLS1 protocol, maybe it will work
    try:
      tls1_conn = TLS1HTTPSConnection(host, port)
      tls1_conn.request("GET", "/")
      return tls1_conn.getresponse().status
    except Exception as e:
      print e
    finally:
      tls1_conn.close()
  except Exception as e:
    print e
  finally:
    conn.close()
#
# Main.
#
def main():
  parser = optparse.OptionParser(usage="usage: %prog [options] component ")
  parser.add_option("-m", "--hosts", dest="hosts", help="Comma separated hosts list for WEB UI to check it availability")
  parser.add_option("-p", "--port", dest="port", help="Port of WEB UI to check it availability")
  parser.add_option("-s", "--https", dest="https", help="\"True\" if value of dfs.http.policy is \"HTTPS_ONLY\"")

  (options, args) = parser.parse_args()
  
  hosts = options.hosts.split(',')
  port = options.port
  https = options.https

  for host in hosts:
    httpCode = make_connection(host, port, https.lower() == "true")

    if httpCode != 200:
      print "Cannot access WEB UI on: http://" + host + ":" + port if not https.lower() == "true" else "Cannot access WEB UI on: https://" + host + ":" + port
      exit(1)

if __name__ == "__main__":
  main()

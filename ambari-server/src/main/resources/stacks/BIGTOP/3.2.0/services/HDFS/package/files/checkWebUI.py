#!/usr/bin/env python3

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
import http.client
import ssl


class ForcedProtocolHTTPSConnection(http.client.HTTPSConnection):
  """
  Some of python implementations does not work correctly with sslv3 but trying to use it, we need to change protocol to
  tls1.
  """

  def __init__(self, host, port, force_protocol, **kwargs):
    self.force_protocol = force_protocol
    context = ssl.SSLContext(getattr(ssl, self.force_protocol))
    context.check_hostname = False
    context.verify_mode = ssl.CERT_NONE
    super(ForcedProtocolHTTPSConnection, self).__init__(
      host, port, context=context, **kwargs
    )


def make_connection(host, port, https, force_protocol=None):
  conn = None
  fallback_conn = None
  try:
    conn = (
      http.client.HTTPConnection(host, port)
      if not https
      else http.client.HTTPSConnection(host, port)
    )
    conn.request("GET", "/")
    return conn.getresponse().status
  except ssl.SSLError:
    # got ssl error, lets try to use TLS1 protocol, maybe it will work
    try:
      fallback_conn = ForcedProtocolHTTPSConnection(host, port, force_protocol)
      fallback_conn.request("GET", "/")
      return fallback_conn.getresponse().status
    except Exception as e:
      print(e)
    finally:
      if fallback_conn is not None:
        fallback_conn.close()
  except Exception as e:
    print(e)
  finally:
    if conn is not None:
      conn.close()


#
# Main.
#
def main():
  parser = optparse.OptionParser(usage="usage: %prog [options] component ")
  parser.add_option(
    "-m",
    "--hosts",
    dest="hosts",
    help="Comma separated hosts list for WEB UI to check it availability",
  )
  parser.add_option(
    "-p", "--port", dest="port", help="Port of WEB UI to check it availability"
  )
  parser.add_option(
    "-s",
    "--https",
    dest="https",
    help='"True" if value of dfs.http.policy is "HTTPS_ONLY"',
  )
  parser.add_option(
    "-o",
    "--protocol",
    dest="protocol",
    help="Protocol to use when executing https request",
  )

  (options, args) = parser.parse_args()

  hosts = options.hosts.split(",")
  port = options.port
  https = options.https
  protocol = options.protocol

  for host in hosts:
    httpCode = make_connection(host, port, https.lower() == "true", protocol)

    if httpCode != 200 and httpCode != 302:
      print(
        "Cannot access WEB UI on: http://" + host + ":" + port
        if not https.lower() == "true"
        else "Cannot access WEB UI on: https://" + host + ":" + port
      )
      exit(1)


if __name__ == "__main__":
  main()

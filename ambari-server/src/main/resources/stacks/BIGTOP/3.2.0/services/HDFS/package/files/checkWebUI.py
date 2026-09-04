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

import argparse
import http.client
import math

from ambari_commons.inet_utils import create_ssl_context


def positive_timeout(value):
  try:
    timeout = float(value)
  except (TypeError, ValueError) as error:
    raise argparse.ArgumentTypeError("timeout must be a number") from error
  if not math.isfinite(timeout) or timeout <= 0:
    raise argparse.ArgumentTypeError("timeout must be a positive finite number")
  return timeout


def make_connection(host, port, https, timeout, force_protocol=None):
  conn = None
  try:
    if https:
      context = create_ssl_context(force_protocol or "PROTOCOL_TLS_CLIENT")
      conn = http.client.HTTPSConnection(
        host, port, timeout=timeout, context=context
      )
    else:
      conn = http.client.HTTPConnection(host, port, timeout=timeout)
    conn.request("GET", "/")
    return conn.getresponse().status
  except Exception as e:
    print(e)
  finally:
    if conn is not None:
      conn.close()


#
# Main.
#
def main():
  parser = argparse.ArgumentParser(usage="usage: %(prog)s [options] component ")
  parser.add_argument(
    "-m",
    "--hosts",
    dest="hosts",
    required=True,
    help="Comma separated hosts list for WEB UI to check it availability",
  )
  parser.add_argument(
    "-p",
    "--port",
    dest="port",
    required=True,
    type=int,
    choices=range(1, 65536),
    metavar="PORT",
    help="Port of WEB UI to check it availability",
  )
  parser.add_argument(
    "-s",
    "--https",
    dest="https",
    required=True,
    choices=("true", "false", "True", "False"),
    help='"True" if value of dfs.http.policy is "HTTPS_ONLY"',
  )
  parser.add_argument(
    "-o",
    "--protocol",
    dest="protocol",
    help="Protocol to use when executing https request",
  )
  parser.add_argument(
    "-t",
    "--timeout",
    dest="timeout",
    type=positive_timeout,
    default=10,
    help="Per-host connection timeout in seconds",
  )

  parser.add_argument("component", nargs="?", help=argparse.SUPPRESS)
  options = parser.parse_args()

  hosts = [host.strip() for host in options.hosts.split(",") if host.strip()]
  if not hosts:
    parser.error("at least one WEB UI host is required")
  port = options.port
  https = options.https
  protocol = options.protocol

  for host in hosts:
    httpCode = make_connection(
      host, port, https.lower() == "true", options.timeout, protocol
    )

    if httpCode != 200 and httpCode != 302:
      print(
        "Cannot access WEB UI on: http://" + host + ":" + str(port)
        if not https.lower() == "true"
        else "Cannot access WEB UI on: https://" + host + ":" + str(port)
      )
      raise SystemExit(1)


if __name__ == "__main__":
  main()

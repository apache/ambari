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

def get_property_value(dictionary, property_name, null_value=None):
  return dictionary[property_name] if property_name in dictionary else null_value

def get_unstructured_data(dictionary, property_name):
  prefix = property_name + '/'
  prefix_len = len(prefix)
  return dict((k[prefix_len:], v) for k, v in dictionary.iteritems() if k.startswith(prefix))

def split_host_and_port(host):
  """
  Splits a string into its host and port components

  :param host: a string matching the following patern: <host name | ip address>[:port]
  :return: a Dictionary containing 'host' and 'port' entries for the input value
  """

  if host is None:
    host_and_port = None
  else:
    host_and_port = {}
    parts = host.split(":")

    if parts is not None:
      length = len(parts)

      if length > 0:
        host_and_port['host'] = parts[0]

        if length > 1:
          host_and_port['port'] = int(parts[1])

  return host_and_port

def set_port(host, port):
  """
  Sets the port for a host specification, potentially replacing an existing port declaration

  :param host: a string matching the following pattern: <host name | ip address>[:port]
  :param port: a string or integer declaring the (new) port
  :return: a string declaring the new host/port specification
  """
  if port is None:
    return host
  else:
    host_and_port = split_host_and_port(host)

    if (host_and_port is not None) and ('host' in host_and_port):
      return "%s:%s" % (host_and_port['host'], port)
    else:
      return host

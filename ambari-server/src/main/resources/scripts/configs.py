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

import sys
import urllib2
import time
import json
import base64

HTTP_PROTOCOL = 'http'
HTTPS_PROTOCOL = 'https'

SET_ACTION = 'set'
GET_ACTION = 'get'
DELETE_ACTION = 'delete'

GET_REQUEST_TYPE = 'GET'
PUT_REQUEST_TYPE = 'PUT'

# JSON Keywords
PROPERTIES = 'properties'
ATTRIBUTES = 'properties_attributes'
CLUSTERS = 'Clusters'
DESIRED_CONFIGS = 'desired_configs'
TYPE = 'type'
TAG = 'tag'
ITEMS = 'items'
TAG_PREFIX = 'version'

CLUSTERS_URL = '/api/v1/clusters/{0}'
DESIRED_CONFIGS_URL = CLUSTERS_URL + '?fields=Clusters/desired_configs'
CONFIGURATION_URL = CLUSTERS_URL + '/configurations?type={1}&tag={2}'

FILE_FORMAT = \
"""
"properties": {
  "key1": "value1"
  "key2": "value2"
},
"properties_attributes": {
  "attribute": {
    "key1": "value1"
    "key2": "value2"
  }
}
"""

class UsageException(Exception):
  pass

def api_accessor(host, login, password, protocol, port):
  def do_request(api_url, request_type=GET_REQUEST_TYPE, request_body=''):
    try:
      url = '{0}://{1}:{2}{3}'.format(protocol, host, port, api_url)
      admin_auth = base64.encodestring('%s:%s' % (login, password)).replace('\n', '')
      request = urllib2.Request(url)
      request.add_header('Authorization', 'Basic %s' % admin_auth)
      request.add_header('X-Requested-By', 'ambari')
      request.add_data(request_body)
      request.get_method = lambda: request_type
      response = urllib2.urlopen(request)
      response_body = response.read()
    except Exception as exc:
      raise Exception('Problem with accessing api. Reason: {0}'.format(exc))
    return response_body
  return do_request

def get_config_tag(cluster, config_type, accessor):
  response = accessor(DESIRED_CONFIGS_URL.format(cluster))
  try:
    desired_tags = json.loads(response)
    current_config_tag = desired_tags[CLUSTERS][DESIRED_CONFIGS][config_type][TAG]
  except Exception as exc:
    raise Exception('"{0}" not found in server response. Response:\n{1}'.format(config_type, response))
  return current_config_tag

def create_new_desired_config(cluster, config_type, properties, attributes, accessor):
  new_tag = TAG_PREFIX + str(int(time.time() * 1000000))
  new_config = {
    CLUSTERS: {
      DESIRED_CONFIGS: {
        TYPE: config_type,
        TAG: new_tag,
        PROPERTIES: properties
      }
    }
  }
  if len(attributes.keys()) > 0:
    new_config[CLUSTERS][DESIRED_CONFIGS][ATTRIBUTES] = attributes
  request_body = json.dumps(new_config)
  new_file = 'doSet_{0}.json'.format(new_tag)
  print '### PUTting json into: {0}'.format(new_file)
  output_to_file(new_file)(new_config)
  accessor(CLUSTERS_URL.format(cluster), PUT_REQUEST_TYPE, request_body)
  print '### NEW Site:{0}, Tag:{1}'.format(config_type, new_tag)

def get_current_config(cluster, config_type, accessor):
  config_tag = get_config_tag(cluster, config_type, accessor)
  print "### on (Site:{0}, Tag:{1})".format(config_type, config_tag)
  response = accessor(CONFIGURATION_URL.format(cluster, config_type, config_tag))
  config_by_tag = json.loads(response)
  current_config = config_by_tag[ITEMS][0]
  return current_config[PROPERTIES], current_config.get(ATTRIBUTES, {})

def update_config(cluster, config_type, config_updater, accessor):
  properties, attributes = config_updater(cluster, config_type, accessor)
  create_new_desired_config(cluster, config_type, properties, attributes, accessor)

def update_specific_property(config_name, config_value):
  def update(cluster, config_type, accessor):
    properties, attributes = get_current_config(cluster, config_type, accessor)
    properties[config_name] = config_value
    return properties, attributes
  return update

def update_from_file(config_file):
  def update(cluster, config_type, accessor):
    try:
      with open(config_file) as in_file:
        file_content = in_file.read()
    except Exception as e:
      raise Exception('Cannot find file "{0}" to PUT'.format(config_file))
    try:
      file_properties = json.loads('{' + file_content + '}')
    except Exception as e:
      raise Exception('File "{0}" should be in the following JSON format ("properties_attributes" is optional):\n{1}'.format(config_file, FILE_FORMAT))
    new_properties = file_properties.get(PROPERTIES, {})
    new_attributes = file_properties.get(ATTRIBUTES, {})
    print '### PUTting file: "{0}"'.format(config_file)
    return new_properties, new_attributes
  return update

def delete_specific_property(config_name):
  def update(cluster, config_type, accessor):
    properties, attributes = get_current_config(cluster, config_type, accessor)
    properties.pop(config_name, None)
    for attribute_values in attributes.values():
      attribute_values.pop(config_name, None)
    return properties, attributes
  return update

def format_json(dictionary, tab_level=0):
  output = ''
  tab = ' ' * 2 * tab_level
  for key, value in dictionary.iteritems():
    output += ',\n{0}"{1}": '.format(tab, key)
    if isinstance(value, dict):
      output += '{\n' + format_json(value, tab_level + 1) + tab + '}'
    else:
      output += '"{0}"'.format(value)
  output += '\n'
  return output[2:]

def output_to_file(filename):
  def output(config):
    with open(filename, 'w') as out_file:
      out_file.write(format_json(config))
  return output

def output_to_console(config):
  print format_json(config)

def get_config(cluster, config_type, accessor, output):
  properties, attributes = get_current_config(cluster, config_type, accessor)
  config = {PROPERTIES: properties}
  if len(attributes.keys()) > 0:
    config[ATTRIBUTES] = attributes
  output(config)

def set_properties(cluster, config_type, args, accessor):
  print '### Performing "set" content:'
  if len(args) == 0:
    raise UsageException("Not enough arguments. Expected config key and value or filename.")

  if len(args) == 1:
    config_file = args[0]
    updater = update_from_file(config_file)
    print '### from file "{0}"'.format(config_file)
  else:
    config_name = args[0]
    config_value = args[1]
    updater = update_specific_property(config_name, config_value)
    print '### new property - "{0}":"{1}"'.format(config_name, config_value)
  update_config(cluster, config_type, updater, accessor)

def delete_properties(cluster, config_type, args, accessor):
  print '### Performing "delete":'
  if len(args) == 0:
    raise UsageException("Not enough arguments. Expected config key.")

  config_name = args[0]
  print '### on property "{0}"'.format(config_name)
  update_config(cluster, config_type, delete_specific_property(config_name), accessor)

def get_properties(cluster, config_type, args, accessor):
  print '### Performing "get" content:'
  if len(args) > 0:
    filename = args[0]
    output = output_to_file(filename)
    print '### to file "{0}"'.format(filename)
  else:
    output = output_to_console
  get_config(cluster, config_type, accessor, output)

def main():
  if len(sys.argv) < 9:
    raise UsageException('Not enough arguments.')
  args = sys.argv[1:]
  user = args[0]
  password = args[1]
  port = args[2]
  protocol = args[3]
  action = args[4]
  host = args[5]
  cluster = args[6]
  config_type = args[7]
  action_args = args[8:]
  accessor = api_accessor(host, user, password, protocol, port)
  if action == SET_ACTION:
    set_properties(cluster, config_type, action_args, accessor)
  elif action == GET_ACTION:
    get_properties(cluster, config_type, action_args, accessor)
  elif action == DELETE_ACTION:
    delete_properties(cluster, config_type, action_args, accessor)
  else:
    raise UsageException('Action "{0}" is not supported. Supported actions: "get", "set", "delete".'.format(action))

if __name__ == "__main__":
  try:
    main()
  except UsageException as usage_exc:
    print '[ERROR]   {0}'.format(usage_exc)
    sys.exit(2)
  except Exception as exc:
    for line in str(exc).split('\n'):
      print '[ERROR]   {0}'.format(line)
    sys.exit(1)

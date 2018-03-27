#!/usr/bin/python

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
import json
import base64
import optparse

HTTP_PROTOCOL = 'http'
HTTPS_PROTOCOL = 'https'

SOLR_SERVICE_NAME = 'AMBARI_INFRA_SOLR'

SOLR_COMPONENT_NAME ='INFRA_SOLR'

CLUSTERS_URL = '/api/v1/clusters/{0}'

GET_HOSTS_COMPONENTS_URL = '/services/{0}/components/{1}?fields=host_components'

REQUESTS_API_URL = '/requests'

def api_accessor(host, username, password, protocol, port):
  def do_request(api_url, request_type, request_body=''):
    try:
      url = '{0}://{1}:{2}{3}'.format(protocol, host, port, api_url)
      print 'Execute {0} {1}'.format(request_type, url)
      if request_body:
        print 'Request body: {0}'.format(request_body)
      admin_auth = base64.encodestring('%s:%s' % (username, password)).replace('\n', '')
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

def read_json(json_file):
  with open(json_file) as data_file:
    data = json.load(data_file)
  return data

def get_json(accessor, url):
  response = accessor(url, 'GET')
  print 'GET ' + url + ' response: '
  print '----------------------------'
  print response
  json_resp = json.loads(response)
  return json_resp

def post_json(accessor, url, request_body):
  response = accessor(url, 'POST', json.dumps(request_body))
  print 'POST ' + url + ' response: '
  print '----------------------------'
  print response
  json_resp = json.loads(response)
  return json_resp

def get_component_hosts(host_components_json):
  hosts = []
  if "host_components" in host_components_json and len(host_components_json['host_components']) > 0:
    for host_component in host_components_json['host_components']:
      if 'HostRoles' in host_component:
        hosts.append(host_component['HostRoles']['host_name'])
  return hosts

def create_command_request(command, parameters, hosts, cluster, context):
  request = {}
  request_info = {}
  request_info["context"] = context
  request_info["command"] = command
  request_info["parameters"] = parameters

  operation_level = {}
  operation_level["level"] = "HOST_COMPONENT"
  operation_level["cluster_name"] = cluster

  request_info["operation_level"] = operation_level
  request["RequestInfo"] = request_info

  resource_filter = {}
  resource_filter["service_name"] = SOLR_SERVICE_NAME
  resource_filter["component_name"] = SOLR_COMPONENT_NAME
  resource_filter["hosts"] = ','.join(hosts)

  resource_filters = []
  resource_filters.append(resource_filter)
  request["Requests/resource_filters"] = resource_filters
  return request

def fill_parameters(options):
  params = {}
  if options.collection:
    params['solr_collection'] = options.collection
  if options.index_location:
    params['solr_index_location'] = options.index_location
  if options.backup_name:
    params['solr_backup_name'] = options.backup_name
  if options.index_version:
    params['solr_index_version'] = options.index_version
  if options.force:
    params['solr_index_upgrade_force'] = options.force
  if options.async:
    params['solr_request_async'] = options.async
  if options.request_tries:
    params['solr_request_tries'] = options.request_tries
  if options.request_time_interval:
    params['solr_request_time_interval'] = options.request_time_interval
  if options.disable_solr_host_check:
    params['solr_check_hosts'] = False
  if options.core_filter:
    params['solr_core_filter'] = options.core_filter

  return params

def validte_common_options(options, parser):
  if not options.index_location:
    parser.print_help()
    print 'index-location option is required'
    sys.exit(1)

  if not options.collection:
    parser.print_help()
    print 'collection option is required'
    sys.exit(1)

def get_solr_hosts(options, accessor):
  if options.solr_hosts:
    component_hosts = options.solr_hosts.split(",")
  else:
    host_components_json = get_json(accessor, CLUSTERS_URL.format(options.cluster) + GET_HOSTS_COMPONENTS_URL.format(SOLR_SERVICE_NAME, SOLR_COMPONENT_NAME))
    component_hosts = get_component_hosts(host_components_json)
  return component_hosts

def restore(options, accessor, parser):
  """
  Send restore solr collection custom command request to ambari-server
  """
  validte_common_options(options, parser)
  if not options.backup_name:
    parser.print_help()
    print 'backup-name option is required'
    sys.exit(1)
  component_hosts = get_solr_hosts(options, accessor)
  parameters = fill_parameters(options)

  cmd_request = create_command_request("RESTORE", parameters, component_hosts, options.cluster, 'Restore Solr Collection: ' + options.collection)
  post_json(accessor, CLUSTERS_URL.format(options.cluster) + REQUESTS_API_URL, cmd_request)

def migrate(options, accessor, parser):
  """
  Send migrate lucene index custom command request to ambari-server
  """
  validte_common_options(options, parser)
  component_hosts = get_solr_hosts(options, accessor)
  parameters = fill_parameters(options)

  cmd_request = create_command_request("MIGRATE", parameters, component_hosts, options.cluster, 'Migrating Solr Collection: ' + options.collection)
  post_json(accessor, CLUSTERS_URL.format(options.cluster) + REQUESTS_API_URL, cmd_request)

def backup(options, accessor, parser):
  """
  Send backup solr collection custom command request to ambari-server
  """
  validte_common_options(options, parser)
  if not options.backup_name:
    parser.print_help()
    print 'backup-name option is required'
    sys.exit(1)
  component_hosts = get_solr_hosts(options, accessor)
  parameters = fill_parameters(options)

  cmd_request = create_command_request("BACKUP", parameters, component_hosts, options.cluster, 'Backup Solr Collection: ' + options.collection)
  post_json(accessor, CLUSTERS_URL.format(options.cluster) + REQUESTS_API_URL, cmd_request)

if __name__=="__main__":
  parser = optparse.OptionParser("usage: %prog [options]")
  parser.add_option("-H", "--host", dest="host", default="localhost", type="string", help="hostname for ambari server")
  parser.add_option("-P", "--port", dest="port", default=8080, type="int", help="port number for ambari server")
  parser.add_option("-c", "--cluster", dest="cluster", type="string", help="name cluster")
  parser.add_option("-s", "--ssl", dest="ssl", action="store_true", help="use if ambari server using https")
  parser.add_option("-u", "--username", dest="username", default="admin", type="string", help="username for accessing ambari server")
  parser.add_option("-p", "--password", dest="password", default="admin", type="string", help="password for accessing ambari server")

  parser.add_option("-a", "--action", dest="action", type="string", help="backup | restore | migrate ")
  parser.add_option("-f", "--force", dest="force", default=False, action="store_true", help="force index upgrade even if it's the right version")
  parser.add_option("--index-location", dest="index_location", type="string", help="location of the index backups")
  parser.add_option("--backup-name", dest="backup_name", type="string", help="backup name of the index")
  parser.add_option("--collection", dest="collection", type="string", help="solr collection")

  parser.add_option("--version", dest="index_version", type="string", default="6.6.2", help="lucene index version for migration (6.6.2 or 7.2.1)")
  parser.add_option("--request-tries", dest="request_tries", type="int", help="number of tries for BACKUP/RESTORE status api calls in the request")
  parser.add_option("--request-time-interval", dest="request_time_interval", type="int", help="time interval between BACKUP/RESTORE status api calls in the request")
  parser.add_option("--request-async", dest="async", action="store_true", default=False, help="skip BACKUP/RESTORE status api calls from the command")
  parser.add_option("--shared-fs", dest="shared_fs", action="store_true", default=False, help="shared fs for storing backup (will create index location to <path><hostname>)")
  parser.add_option("--solr-hosts", dest="solr_hosts", type="string", help="comma separated list of solr hosts")
  parser.add_option("--disable-solr-host-check", dest="disable_solr_host_check", action="store_true", default=False, help="Disable to check solr hosts are good for the collection backups")
  parser.add_option("--core-filter", dest="core_filter", default=None, type="string", help="core filter for replica folders")

  (options, args) = parser.parse_args()

  protocol = 'https' if options.ssl else 'http'

  accessor = api_accessor(options.host, options.username, options.password, protocol, options.port)

  print 'Inputs: ' + str(options)
  if options.action.lower() == 'backup':
    backup(options, accessor, parser)
  elif options.action.lower() == 'restore':
    restore(options, accessor, parser)
  elif options.action.lower() == 'migrate':
    migrate(options, accessor, parser)
  else:
    parser.print_help()
    print 'action option is wrong or missing'


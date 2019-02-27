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

import ConfigParser
import base64
import copy
import glob
import json
import logging
import optparse
import os
import socket
import ssl
import sys
import time
import traceback
import urllib2
from datetime import datetime, timedelta
from random import randrange, randint
from subprocess import Popen, PIPE

import solrDataManager as solr_data_manager

HTTP_PROTOCOL = 'http'
HTTPS_PROTOCOL = 'https'

AMBARI_SUDO = "/var/lib/ambari-agent/ambari-sudo.sh"

SOLR_SERVICE_NAME = 'AMBARI_INFRA_SOLR'

SOLR_COMPONENT_NAME ='INFRA_SOLR'

LOGSEARCH_SERVICE_NAME = 'LOGSEARCH'

LOGSEARCH_SERVER_COMPONENT_NAME ='LOGSEARCH_SERVER'
LOGSEARCH_LOGFEEDER_COMPONENT_NAME ='LOGSEARCH_LOGFEEDER'

RANGER_SERVICE_NAME = "RANGER"
RANGER_ADMIN_COMPONENT_NAME = "RANGER_ADMIN"

ATLAS_SERVICE_NAME = "ATLAS"
ATLAS_SERVER_COMPONENT_NAME = "ATLAS_SERVER"

CLUSTERS_URL = '/api/v1/clusters/{0}'

GET_HOSTS_COMPONENTS_URL = '/services/{0}/components/{1}?fields=host_components'

REQUESTS_API_URL = '/requests'
BATCH_REQUEST_API_URL = "/api/v1/clusters/{0}/request_schedules"
GET_ACTUAL_CONFIG_URL = '/configurations/service_config_versions?service_name={0}&is_current=true'
CREATE_CONFIGURATIONS_URL = '/configurations'

LIST_SOLR_COLLECTION_URL = '{0}/admin/collections?action=LIST&wt=json'
CREATE_SOLR_COLLECTION_URL = '{0}/admin/collections?action=CREATE&name={1}&collection.configName={2}&numShards={3}&replicationFactor={4}&maxShardsPerNode={5}&wt=json'
DELETE_SOLR_COLLECTION_URL = '{0}/admin/collections?action=DELETE&name={1}&wt=json&async={2}'
RELOAD_SOLR_COLLECTION_URL = '{0}/admin/collections?action=RELOAD&name={1}&wt=json'
REQUEST_STATUS_SOLR_COLLECTION_URL = '{0}/admin/collections?action=REQUESTSTATUS&requestid={1}&wt=json'
CORE_DETAILS_URL = '{0}replication?command=details&wt=json'

INFRA_SOLR_CLIENT_BASE_PATH = '/usr/lib/ambari-infra-solr-client/'
RANGER_NEW_SCHEMA = 'migrate/managed-schema'
SOLR_CLOUD_CLI_SCRIPT = 'solrCloudCli.sh'
COLLECTIONS_DATA_JSON_LOCATION = INFRA_SOLR_CLIENT_BASE_PATH + "migrate/data/{0}"

logger = logging.getLogger()
handler = logging.StreamHandler()
formatter = logging.Formatter("%(asctime)s - %(message)s")
handler.setFormatter(formatter)
logger.addHandler(handler)

class colors:
  OKGREEN = '\033[92m'
  WARNING = '\033[38;5;214m'
  FAIL = '\033[91m'
  ENDC = '\033[0m'

def api_accessor(host, username, password, protocol, port):
  def do_request(api_url, request_type, request_body=''):
    try:
      url = '{0}://{1}:{2}{3}'.format(protocol, host, port, api_url)
      logger.debug('Execute {0} {1}'.format(request_type, url))
      if request_body:
        logger.debug('Request body: {0}'.format(request_body))
      admin_auth = base64.encodestring('%s:%s' % (username, password)).replace('\n', '')
      request = urllib2.Request(url)
      request.add_header('Authorization', 'Basic %s' % admin_auth)
      request.add_header('X-Requested-By', 'ambari')
      request.add_data(request_body)
      request.get_method = lambda: request_type
      response = None
      if protocol == 'https':
        ctx = ssl.create_default_context()
        ctx.check_hostname = False
        ctx.verify_mode = ssl.CERT_NONE
        response = urllib2.urlopen(request, context=ctx)
      else:
        response = urllib2.urlopen(request)
      response_body = response.read()
    except Exception as exc:
      raise Exception('Problem with accessing api. Reason: {0}'.format(exc))
    return response_body
  return do_request

def set_log_level(verbose):
  if verbose:
    logger.setLevel(logging.DEBUG)
  else:
    logger.setLevel(logging.INFO)

def retry(func, *args, **kwargs):
  retry_count = kwargs.pop("count", 10)
  delay = kwargs.pop("delay", 5)
  context = kwargs.pop("context", "")
  for r in range(retry_count):
    try:
      result = func(*args, **kwargs)
      if result is not None: return result
    except Exception as e:
      logger.error("Error occurred during {0} operation: {1}".format(context, str(traceback.format_exc())))
    logger.info("\n{0}: waiting for {1} seconds before retyring again (retry count: {2})".format(context, delay, r+1))
    time.sleep(delay)
  print '{0} operation {1}FAILED{2}'.format(context, colors.FAIL, colors.ENDC)
  sys.exit(1)

def get_keytab_and_principal(config):
  kerberos_enabled = 'false'
  keytab=None
  principal=None
  if config.has_section('cluster') and config.has_option('cluster', 'kerberos_enabled'):
    kerberos_enabled=config.get('cluster', 'kerberos_enabled')

  if config.has_section('infra_solr'):
    if config.has_option('infra_solr', 'user'):
      user=config.get('infra_solr', 'user')
    if kerberos_enabled == 'true':
      if config.has_option('infra_solr', 'keytab'):
        keytab=config.get('infra_solr', 'keytab')
      if config.has_option('infra_solr', 'principal'):
        principal=config.get('infra_solr', 'principal')
  return keytab, principal

def create_solr_api_request_command(request_url, config, output=None):
  user='infra-solr'
  if config.has_section('infra_solr'):
    if config.has_option('infra_solr', 'user'):
      user=config.get('infra_solr', 'user')
  kerberos_enabled='false'
  if config.has_section('cluster') and config.has_option('cluster', 'kerberos_enabled'):
    kerberos_enabled=config.get('cluster', 'kerberos_enabled')
  keytab, principal=get_keytab_and_principal(config)
  use_infra_solr_user="sudo -u {0}".format(user)
  curl_prefix = "curl -k"
  if output is not None:
    curl_prefix+=" -o {0}".format(output)
  api_cmd = '{0} kinit -kt {1} {2} && {3} {4} --negotiate -u : "{5}"'.format(use_infra_solr_user, keytab, principal, use_infra_solr_user, curl_prefix, request_url) \
    if kerberos_enabled == 'true' else '{0} {1} "{2}"'.format(use_infra_solr_user, curl_prefix, request_url)
  logger.debug("Solr API command: {0}".format(api_cmd))
  return api_cmd

def create_infra_solr_client_command(options, config, command, appendZnode=False):
  user='infra-solr'
  kerberos_enabled='false'
  infra_solr_cli_opts = ''
  java_home=None
  jaasOption=None
  zkConnectString=None
  if config.has_section('cluster') and config.has_option('cluster', 'kerberos_enabled'):
    kerberos_enabled=config.get('cluster', 'kerberos_enabled')
  if config.has_section('infra_solr'):
    if config.has_option('infra_solr', 'user'):
      user=config.get('infra_solr', 'user')
    if config.has_option('infra_solr', 'external_zk_connect_string'):
      zkConnectString=config.get('infra_solr', 'external_zk_connect_string')
    elif config.has_option('infra_solr', 'zk_connect_string'):
      zkConnectString=config.get('infra_solr', 'zk_connect_string')
  if kerberos_enabled == 'true':
    zk_principal_user = config.get('infra_solr', 'zk_principal_user') if config.has_option('infra_solr', 'zk_principal_user') else 'zookeeper'
    infra_solr_cli_opts= '-Dzookeeper.sasl.client=true -Dzookeeper.sasl.client.username={0} -Dzookeeper.sasl.clientconfig=Client'.format(zk_principal_user)
    jaasOption=" --jaas-file /etc/ambari-infra-solr/conf/infra_solr_jaas.conf"
    command+=jaasOption
  if config.has_section('local') and config.has_option('local', 'java_home'):
    java_home=config.get('local', 'java_home')
  if not java_home:
    raise Exception("'local' section or 'java_home' is missing (or empty) from the configuration")
  if not zkConnectString:
    raise Exception("'zk_connect_string' section or 'external_zk_connect_string' is missing (or empty) from the configuration")
  if appendZnode:
    if config.has_option('infra_solr', 'znode'):
      znode_to_append=config.get('infra_solr', 'znode')
      zkConnectString+="{0}".format(znode_to_append)
    else:
      raise Exception("'znode' option is required for infra_solr section")

  set_java_home_= 'JAVA_HOME={0}'.format(java_home)
  set_infra_solr_cli_opts = ' INFRA_SOLR_CLI_OPTS="{0}"'.format(infra_solr_cli_opts) if infra_solr_cli_opts != '' else ''
  solr_cli_cmd = '{0} {1}{2} /usr/lib/ambari-infra-solr-client/solrCloudCli.sh --zookeeper-connect-string {3} {4}'\
    .format(AMBARI_SUDO, set_java_home_, set_infra_solr_cli_opts, zkConnectString, command)

  return solr_cli_cmd

def get_random_solr_url(solr_urls, options = None):
  random_index = randrange(0, len(solr_urls))
  result = solr_urls[random_index]
  logger.debug("Use {0} solr address for next request.".format(result))
  return result

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
  logger.debug('GET ' + url + ' response: ')
  logger.debug('----------------------------')
  logger.debug(str(response))
  json_resp = json.loads(response)
  return json_resp

def post_json(accessor, url, request_body):
  response = accessor(url, 'POST', json.dumps(request_body))
  logger.debug('POST ' + url + ' response: ')
  logger.debug( '----------------------------')
  logger.debug(str(response))
  json_resp = json.loads(response)
  return json_resp

def get_component_hosts(host_components_json):
  hosts = []
  if "host_components" in host_components_json and len(host_components_json['host_components']) > 0:
    for host_component in host_components_json['host_components']:
      if 'HostRoles' in host_component:
        hosts.append(host_component['HostRoles']['host_name'])
  return hosts

def create_batch_command(command, hosts, cluster, service_name, component_name, interval_seconds, fault_tolerance, context):
  request_schedules = []
  request_schedule = {}
  batch = []
  requests = []
  order_id = 1
  all = len(hosts)
  for host in hosts:
    request = {}
    request['order_id'] = order_id
    request['type'] = 'POST'
    request['uri'] = "/clusters/{0}/requests".format(cluster)
    request_body_info = {}
    request_info = {}
    request_info["context"] = context + " ({0} of {1})".format(order_id, all)
    request_info["command"] = command

    order_id = order_id + 1

    resource_filter = {}
    resource_filter["service_name"] = service_name
    resource_filter["component_name"] = component_name
    resource_filter["hosts"] = host

    resource_filters = []
    resource_filters.append(resource_filter)
    request_body_info["Requests/resource_filters"] = resource_filters
    request_body_info['RequestInfo'] = request_info

    request['RequestBodyInfo'] = request_body_info
    requests.append(request)
  batch_requests_item = {}
  batch_requests_item['requests'] = requests
  batch.append(batch_requests_item)
  batch_settings_item = {}
  batch_settings = {}
  batch_settings['batch_separation_in_seconds'] = interval_seconds
  batch_settings['task_failure_tolerance'] = fault_tolerance
  batch_settings_item['batch_settings'] = batch_settings
  batch.append(batch_settings_item)
  request_schedule['batch'] = batch

  request_schedule_item = {}
  request_schedule_item['RequestSchedule'] = request_schedule
  request_schedules.append(request_schedule_item)

  return request_schedules

def create_command_request(command, parameters, hosts, cluster, context, service=SOLR_SERVICE_NAME, component=SOLR_COMPONENT_NAME):
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
  resource_filter["service_name"] = service
  resource_filter["component_name"] = component
  resource_filter["hosts"] = ','.join(hosts)

  resource_filters = []
  resource_filters.append(resource_filter)
  request["Requests/resource_filters"] = resource_filters
  return request

def fill_params_for_backup(params, collection):
  collections_data = get_collections_data(COLLECTIONS_DATA_JSON_LOCATION.format("backup_collections.json"))
  if collection in collections_data and 'leaderHostCoreMap' in collections_data[collection]:
    params["solr_backup_host_cores_map"] = json.dumps(collections_data[collection]['leaderHostCoreMap'])
  if collection in collections_data and 'leaderCoreHostMap' in collections_data[collection]:
    params["solr_backup_core_host_map"] = json.dumps(collections_data[collection]['leaderCoreHostMap'])
  return params

def fill_params_for_restore(params, original_collection, collection, config_set):
  backup_collections_data = get_collections_data(COLLECTIONS_DATA_JSON_LOCATION.format("backup_collections.json"))
  if original_collection in backup_collections_data and 'leaderHostCoreMap' in backup_collections_data[original_collection]:
    params["solr_backup_host_cores_map"] = json.dumps(backup_collections_data[original_collection]['leaderHostCoreMap'])
  if original_collection in backup_collections_data and 'leaderCoreHostMap' in backup_collections_data[original_collection]:
    params["solr_backup_core_host_map"] = json.dumps(backup_collections_data[original_collection]['leaderCoreHostMap'])

  collections_data = get_collections_data(COLLECTIONS_DATA_JSON_LOCATION.format("restore_collections.json"))
  if collection in collections_data and 'leaderHostCoreMap' in collections_data[collection]:
    params["solr_restore_host_cores_map"] = json.dumps(collections_data[collection]['leaderHostCoreMap'])
  if collection in collections_data and 'leaderCoreHostMap' in collections_data[collection]:
    params["solr_restore_core_host_map"] = json.dumps(collections_data[collection]['leaderCoreHostMap'])
  if collection in collections_data and 'leaderSolrCoreDataMap' in collections_data[collection]:
    params["solr_restore_core_data"] = json.dumps(collections_data[collection]['leaderSolrCoreDataMap'])
  if config_set:
    params["solr_restore_config_set"] = config_set

  return params

def fill_parameters(options, config, collection, index_location, hdfs_path=None, shards=None):
  params = {}
  if collection:
    params['solr_collection'] = collection
    params['solr_backup_name'] = collection
  if index_location:
    params['solr_index_location'] = index_location
  if options.index_version:
    params['solr_index_version'] = options.index_version
  if options.force:
    params['solr_index_upgrade_force'] = options.force
  if options.async:
    params['solr_request_async'] = options.request_async
  if options.request_tries:
    params['solr_request_tries'] = options.request_tries
  if options.request_time_interval:
    params['solr_request_time_interval'] = options.request_time_interval
  if options.disable_solr_host_check:
    params['solr_check_hosts'] = False
  if options.core_filter:
    params['solr_core_filter'] = options.core_filter
  if options.core_filter:
    params['solr_skip_cores'] = options.skip_cores
  if shards:
    params['solr_shards'] = shards
  if options.shared_drive:
    params['solr_shared_fs'] = True
  elif config.has_section('local') and config.has_option('local', 'shared_drive') and config.get('local', 'shared_drive') == 'true':
    params['solr_shared_fs'] = True
  if hdfs_path:
    params['solr_hdfs_path'] = hdfs_path
  if options.keep_backup:
    params['solr_keep_backup'] = True
  return params

def validte_common_options(options, parser, config):
  if not options.index_location:
    parser.print_help()
    print 'index-location option is required'
    sys.exit(1)

  if not options.collection:
    parser.print_help()
    print 'collection option is required'
    sys.exit(1)

def get_service_components(options, accessor, cluster, service, component):
  host_components_json = get_json(accessor, CLUSTERS_URL.format(cluster) + GET_HOSTS_COMPONENTS_URL.format(service, component))
  component_hosts = get_component_hosts(host_components_json)
  return component_hosts

def get_solr_hosts(options, accessor, cluster):
  component_hosts = get_service_components(options, accessor, cluster, SOLR_SERVICE_NAME, SOLR_COMPONENT_NAME)

  if options.include_solr_hosts:
    new_component_hosts = []
    include_solr_hosts_list = options.include_solr_hosts.split(',')
    for include_host in include_solr_hosts_list:
      if include_host in component_hosts:
        new_component_hosts.append(include_host)
    component_hosts = new_component_hosts
  if options.exclude_solr_hosts:
    exclude_solr_hosts_list = options.exclude_solr_hosts.split(',')
    for exclude_host in exclude_solr_hosts_list:
      if exclude_host in component_hosts:
        component_hosts.remove(exclude_host)
  return component_hosts

def restore(options, accessor, parser, config, original_collection, collection, config_set, index_location, hdfs_path, shards):
  """
  Send restore solr collection custom command request to ambari-server
  """
  cluster = config.get('ambari_server', 'cluster')

  component_hosts = get_solr_hosts(options, accessor, cluster)
  parameters = fill_parameters(options, config, collection, index_location, hdfs_path, shards)
  parameters = fill_params_for_restore(parameters, original_collection, collection, config_set)

  cmd_request = create_command_request("RESTORE", parameters, component_hosts, cluster, 'Restore Solr Collection: ' + collection)
  return post_json(accessor, CLUSTERS_URL.format(cluster) + REQUESTS_API_URL, cmd_request)

def migrate(options, accessor, parser, config, collection, index_location):
  """
  Send migrate lucene index custom command request to ambari-server
  """
  cluster = config.get('ambari_server', 'cluster')

  component_hosts = get_solr_hosts(options, accessor, cluster)
  parameters = fill_parameters(options, config, collection, index_location)

  cmd_request = create_command_request("MIGRATE", parameters, component_hosts, cluster, 'Migrating Solr Collection: ' + collection)
  return post_json(accessor, CLUSTERS_URL.format(cluster) + REQUESTS_API_URL, cmd_request)

def backup(options, accessor, parser, config, collection, index_location):
  """
  Send backup solr collection custom command request to ambari-server
  """
  cluster = config.get('ambari_server', 'cluster')

  component_hosts = get_solr_hosts(options, accessor, cluster)
  parameters = fill_parameters(options, config, collection, index_location)

  parameters = fill_params_for_backup(parameters, collection)

  cmd_request = create_command_request("BACKUP", parameters, component_hosts, cluster, 'Backup Solr Collection: ' + collection)
  return post_json(accessor, CLUSTERS_URL.format(cluster) + REQUESTS_API_URL, cmd_request)


def upgrade_solr_instances(options, accessor, parser, config):
  """
  Upgrade (remove & re-install) infra solr instances
  """
  cluster = config.get('ambari_server', 'cluster')
  solr_instance_hosts = get_service_components(options, accessor, cluster, "AMBARI_INFRA_SOLR", "INFRA_SOLR")

  context = "Upgrade Solr Instances"
  sys.stdout.write("Sending upgrade request: [{0}] ".format(context))
  sys.stdout.flush()

  cmd_request = create_command_request("UPGRADE_SOLR_INSTANCE", {}, solr_instance_hosts, cluster, context)
  response = post_json(accessor, CLUSTERS_URL.format(cluster) + REQUESTS_API_URL, cmd_request)
  request_id = get_request_id(response)
  sys.stdout.write(colors.OKGREEN + 'DONE\n' + colors.ENDC)
  sys.stdout.flush()
  print 'Upgrade command request id: {0}'.format(request_id)
  if options.async:
    print "Upgrade request sent to Ambari server. Check Ambari UI about the results."
    sys.exit(0)
  else:
    sys.stdout.write("Start monitoring Ambari request with id {0} ...".format(request_id))
    sys.stdout.flush()
    cluster = config.get('ambari_server', 'cluster')
    monitor_request(options, accessor, cluster, request_id, context)
    print "{0}... {1} DONE{2}".format(context, colors.OKGREEN, colors.ENDC)

def upgrade_solr_clients(options, accessor, parser, config):
  """
  Upgrade (remove & re-install) infra solr clients
  """
  cluster = config.get('ambari_server', 'cluster')
  solr_client_hosts = get_service_components(options, accessor, cluster, "AMBARI_INFRA_SOLR", "INFRA_SOLR_CLIENT")

  fqdn = socket.getfqdn()
  if fqdn in solr_client_hosts:
    solr_client_hosts.remove(fqdn)
  host = socket.gethostname()
  if host in solr_client_hosts:
    solr_client_hosts.remove(host)
  context = "Upgrade Solr Clients"
  sys.stdout.write("Sending upgrade request: [{0}] ".format(context))
  sys.stdout.flush()

  cmd_request = create_command_request("UPGRADE_SOLR_CLIENT", {}, solr_client_hosts, cluster, context, component="INFRA_SOLR_CLIENT")
  response = post_json(accessor, CLUSTERS_URL.format(cluster) + REQUESTS_API_URL, cmd_request)
  request_id = get_request_id(response)
  sys.stdout.write(colors.OKGREEN + 'DONE\n' + colors.ENDC)
  sys.stdout.flush()
  print 'Upgrade command request id: {0}'.format(request_id)
  if options.async:
    print "Upgrade request sent to Ambari server. Check Ambari UI about the results."
    sys.exit(0)
  else:
    sys.stdout.write("Start monitoring Ambari request with id {0} ...".format(request_id))
    sys.stdout.flush()
    cluster = config.get('ambari_server', 'cluster')
    monitor_request(options, accessor, cluster, request_id, context)
    print "{0}... {1}DONE{2}".format(context, colors.OKGREEN, colors.ENDC)

def upgrade_logfeeders(options, accessor, parser, config):
  """
  Upgrade (remove & re-install) logfeeders
  """
  cluster = config.get('ambari_server', 'cluster')
  logfeeder_hosts = get_service_components(options, accessor, cluster, "LOGSEARCH", "LOGSEARCH_LOGFEEDER")

  context = "Upgrade Log Feeders"
  sys.stdout.write("Sending upgrade request: [{0}] ".format(context))
  sys.stdout.flush()

  cmd_request = create_command_request("UPGRADE_LOGFEEDER", {}, logfeeder_hosts, cluster, context, service="LOGSEARCH", component="LOGSEARCH_LOGFEEDER")
  response = post_json(accessor, CLUSTERS_URL.format(cluster) + REQUESTS_API_URL, cmd_request)
  request_id = get_request_id(response)
  sys.stdout.write(colors.OKGREEN + 'DONE\n' + colors.ENDC)
  sys.stdout.flush()
  print 'Upgrade command request id: {0}'.format(request_id)
  if options.async:
    print "Upgrade request sent to Ambari server. Check Ambari UI about the results."
    sys.exit(0)
  else:
    sys.stdout.write("Start monitoring Ambari request with id {0} ...".format(request_id))
    sys.stdout.flush()
    cluster = config.get('ambari_server', 'cluster')
    monitor_request(options, accessor, cluster, request_id, context)
    print "{0}... {1} DONE{2}".format(context, colors.OKGREEN, colors.ENDC)

def upgrade_logsearch_portal(options, accessor, parser, config):
  """
  Upgrade (remove & re-install) logsearch server instances
  """
  cluster = config.get('ambari_server', 'cluster')
  logsearch_portal_hosts = get_service_components(options, accessor, cluster, "LOGSEARCH", "LOGSEARCH_SERVER")

  context = "Upgrade Log Search Portal"
  sys.stdout.write("Sending upgrade request: [{0}] ".format(context))
  sys.stdout.flush()

  cmd_request = create_command_request("UPGRADE_LOGSEARCH_PORTAL", {}, logsearch_portal_hosts, cluster, context, service="LOGSEARCH", component="LOGSEARCH_SERVER")
  response = post_json(accessor, CLUSTERS_URL.format(cluster) + REQUESTS_API_URL, cmd_request)
  request_id = get_request_id(response)
  sys.stdout.write(colors.OKGREEN + 'DONE\n' + colors.ENDC)
  sys.stdout.flush()
  print 'Upgrade command request id: {0}'.format(request_id)
  if options.async:
    print "Upgrade request sent to Ambari server. Check Ambari UI about the results."
    sys.exit(0)
  else:
    sys.stdout.write("Start monitoring Ambari request with id {0} ...".format(request_id))
    sys.stdout.flush()
    cluster = config.get('ambari_server', 'cluster')
    monitor_request(options, accessor, cluster, request_id, context)
    print "{0}... {1} DONE{2}".format(context, colors.OKGREEN, colors.ENDC)

def service_components_command(options, accessor, parser, config, service, component, command, command_str):
  """
  Run command on service components
  """
  cluster = config.get('ambari_server', 'cluster')
  service_components = get_service_components(options, accessor, cluster, service, component)

  context = "{0} {1}".format(command_str, component)
  sys.stdout.write("Sending '{0}' request: [{1}] ".format(command, context))
  sys.stdout.flush()

  cmd_request = create_command_request(command, {}, service_components, cluster, context, service=service, component=component)
  response = post_json(accessor, CLUSTERS_URL.format(cluster) + REQUESTS_API_URL, cmd_request)
  request_id = get_request_id(response)
  sys.stdout.write(colors.OKGREEN + 'DONE\n' + colors.ENDC)
  sys.stdout.flush()
  print '{0} command request id: {1}'.format(command_str, request_id)
  if options.async:
    print "{0} request sent to Ambari server. Check Ambari UI about the results.".format(command_str)
    sys.exit(0)
  else:
    sys.stdout.write("Start monitoring Ambari request with id {0} ...".format(request_id))
    sys.stdout.flush()
    cluster = config.get('ambari_server', 'cluster')
    monitor_request(options, accessor, cluster, request_id, context)
    print "{0}... {1} DONE{2}".format(context, colors.OKGREEN, colors.ENDC)

def monitor_request(options, accessor, cluster, request_id, context):
  while True:
    request_response=get_json(accessor, "/api/v1/clusters/{0}{1}/{2}".format(cluster, REQUESTS_API_URL, request_id))
    if 'Requests' in request_response and 'request_status' in request_response['Requests']:
      request_status = request_response['Requests']['request_status']
      logger.debug("\nMonitoring '{0}' request (id: '{1}') status is {2}".format(context, request_id, request_status))
      if request_status in ['FAILED', 'TIMEDOUT', 'ABORTED', 'COMPLETED', 'SKIPPED_FAILED']:
        if request_status == 'COMPLETED':
          print "\nRequest (id: {0}) {1}COMPLETED{2}".format(request_id, colors.OKGREEN, colors.ENDC)
          time.sleep(4)
        else:
          print "\nRequest (id: {0}) {1}FAILED{2} (checkout Ambari UI about the failed tasks)\n".format(request_id, colors.FAIL, colors.ENDC)
          sys.exit(1)
        break
      else:
        if not options.verbose:
          sys.stdout.write(".")
          sys.stdout.flush()
        logger.debug("Sleep 5 seconds ...")
        time.sleep(5)
    else:
      print "'Requests' or 'request_status' cannot be found in JSON response: {0}".format(request_response)
      sys.exit(1)

def get_request_id(json_response):
  if "Requests" in json_response:
    if "id" in json_response['Requests']:
      return json_response['Requests']['id']
  raise Exception("Cannot access request id from Ambari response: {0}".format(json_response))

def filter_collections(options, collections):
  if options.collection is not None:
    filtered_collections = []
    if options.collection in collections:
      filtered_collections.append(options.collection)
    return filtered_collections
  else:
    return collections

def get_infra_solr_props(config, accessor):
  cluster = config.get('ambari_server', 'cluster')
  service_configs = get_json(accessor, CLUSTERS_URL.format(cluster) + GET_ACTUAL_CONFIG_URL.format(SOLR_SERVICE_NAME))
  infra_solr_props = {}
  infra_solr_env_properties = {}
  infra_solr_security_json_properties = {}
  if 'items' in service_configs and len(service_configs['items']) > 0:
    if 'configurations' in service_configs['items'][0]:
      for config in service_configs['items'][0]['configurations']:
        if 'type' in config and config['type'] == 'infra-solr-env':
          infra_solr_env_properties = config['properties']
        if 'type' in config and config['type'] == 'infra-solr-security-json':
          infra_solr_security_json_properties = config['properties']
  infra_solr_props['infra-solr-env'] = infra_solr_env_properties
  infra_solr_props['infra-solr-security-json'] = infra_solr_security_json_properties
  return infra_solr_props

def insert_string_before(full_str, sub_str, insert_str):
  idx = full_str.index(sub_str)
  return full_str[:idx] + insert_str + full_str[idx:]

def set_solr_security_management(infra_solr_props, accessor, enable = True):
  security_props =  infra_solr_props['infra-solr-security-json']
  check_value = "false" if enable else "true"
  set_value = "true" if enable else "false"
  turn_status = "on" if enable else "off"
  if 'infra_solr_security_manually_managed' in security_props and security_props['infra_solr_security_manually_managed'] == check_value:
    security_props['infra_solr_security_manually_managed'] = set_value
    post_configuration = create_configs('infra-solr-security-json', security_props, 'Turn {0} security.json manaul management by migrationHelper.py'.format(turn_status))
    apply_configs(config, accessor, post_configuration)
  else:
    print "Configuration 'infra-solr-security-json/infra_solr_security_manually_managed' has already set to '{0}'".format(set_value)

def set_solr_name_rules(infra_solr_props, accessor, add = False):
  """
  Set name rules in infra-solr-env/content if not set in add mode, in non-add mode, remove it if exists
  :param add: solr kerb name rules needs to be added (if false, it needs to be removed)
  """
  infra_solr_env_props =  infra_solr_props['infra-solr-env']
  name_rules_param = "SOLR_KERB_NAME_RULES=\"{{infra_solr_kerberos_name_rules}}\"\n"

  if 'content' in infra_solr_env_props and (name_rules_param not in infra_solr_env_props['content']) is add:
    if add:
      print "Adding 'SOLR_KERB_NAME_RULES' to 'infra-solr-env/content'"
      new_content = insert_string_before(infra_solr_env_props['content'], "SOLR_KERB_KEYTAB", name_rules_param)
      infra_solr_env_props['content'] = new_content
      post_configuration = create_configs('infra-solr-env', infra_solr_env_props, 'Add "SOLR_KERB_NAME_RULES" by migrationHelper.py')
      apply_configs(config, accessor, post_configuration)
    else:
      print "Removing 'SOLR_KERB_NAME_RULES' from 'infra-solr-env/content'"
      new_content = infra_solr_env_props['content'].replace(name_rules_param, '')
      infra_solr_env_props['content'] = new_content
      post_configuration = create_configs('infra-solr-env', infra_solr_env_props, 'Remove "SOLR_KERB_NAME_RULES" by migrationHelper.py')
      apply_configs(config, accessor, post_configuration)
  else:
    if add:
      print "'SOLR_KERB_NAME_RULES' has already set in configuration 'infra-solr-env/content'"
    else:
      print "Configuration 'infra-solr-env/content' does not contain 'SOLR_KERB_NAME_RULES'"

def apply_configs(config, accessor, post_configuration):
  cluster = config.get('ambari_server', 'cluster')
  desired_configs_post_body = {}
  desired_configs_post_body["Clusters"] = {}
  desired_configs_post_body["Clusters"]["desired_configs"] = post_configuration
  accessor(CLUSTERS_URL.format(cluster), 'PUT', json.dumps(desired_configs_post_body))

def create_configs(config_type, properties, context):
  configs_for_posts = {}
  configuration = {}
  configuration['type'] = config_type
  configuration['tag'] = "version" + str(int(round(time.time() * 1000)))
  configuration['properties'] = properties
  configuration['service_config_version_note'] = context
  configs_for_posts[config_type] = configuration
  return configs_for_posts

def common_data(list1, list2):
  common_data = []
  for x in list1:
    for y in list2:
      if x == y:
        common_data.append(x)
  return common_data

def filter_solr_hosts_if_match_any(splitted_solr_hosts, collection, collections_json):
  """
  Return common hosts if there is any match with the collection related hosts, if not then filter won't apply (e.g.: won't filter with IPs in host names)
  """
  collection_related_hosts = []
  all_collection_data = get_collections_data(collections_json)
  if collection in all_collection_data:
    collection_data = all_collection_data[collection]
    if 'shards' in collection_data:
      for shard in collection_data['shards']:
        if 'replicas' in collection_data['shards'][shard]:
          for replica in collection_data['shards'][shard]['replicas']:
            nodeName = collection_data['shards'][shard]['replicas'][replica]['nodeName']
            hostName = nodeName.split(":")[0]
            if hostName not in collection_related_hosts:
              collection_related_hosts.append(hostName)
  common_list = common_data(splitted_solr_hosts, collection_related_hosts)
  return common_list if common_list else splitted_solr_hosts

def get_solr_urls(options, config, collection, collections_json):
  solr_urls = []
  solr_hosts = None
  solr_port = "8886"
  solr_protocol = "http"
  if config.has_section("infra_solr") and config.has_option("infra_solr", "port"):
    solr_port = config.get('infra_solr', 'port')
  if config.has_section("infra_solr") and config.has_option("infra_solr", "protocol"):
    solr_protocol = config.get('infra_solr', 'protocol')
  if config.has_section('infra_solr') and config.has_option('infra_solr', 'hosts'):
    solr_hosts = config.get('infra_solr', 'hosts')

  splitted_solr_hosts = solr_hosts.split(',')
  splitted_solr_hosts = filter_solr_hosts_if_match_any(splitted_solr_hosts, collection, collections_json)
  if options.include_solr_hosts:
    # keep only included ones, do not override any
    include_solr_hosts_list = options.include_solr_hosts.split(',')
    new_splitted_hosts = []
    for host in splitted_solr_hosts:
      if any(inc_solr_host in host for inc_solr_host in include_solr_hosts_list):
        new_splitted_hosts.append(host)
    splitted_solr_hosts = new_splitted_hosts

  if options.exclude_solr_hosts:
    exclude_solr_hosts_list = options.exclude_solr_hosts.split(',')
    hosts_to_exclude = []
    for host in splitted_solr_hosts:
      if any(exc_solr_host in host for exc_solr_host in exclude_solr_hosts_list):
        hosts_to_exclude.append(host)
    for excluded_url in hosts_to_exclude:
      splitted_solr_hosts.remove(excluded_url)

  for solr_host in splitted_solr_hosts:
    solr_addr = "{0}://{1}:{2}/solr".format(solr_protocol, solr_host, solr_port)
    solr_urls.append(solr_addr)

  return solr_urls

def get_input_output_solr_url(src_solr_urls, target_solr_urls):
  """
  Choose random solr urls for the source and target collections, prefer localhost and common urls
  """
  def intersect(a, b):
    return list(set(a) & set(b))
  input_solr_urls = src_solr_urls
  output_solr_urls = target_solr_urls
  hostname = socket.getfqdn()
  if any(hostname in s for s in input_solr_urls):
    input_solr_urls = filter(lambda x: hostname in x, input_solr_urls)
  if any(hostname in s for s in output_solr_urls):
    output_solr_urls = filter(lambda x: hostname in x, output_solr_urls)
  common_url_list = intersect(input_solr_urls, output_solr_urls)
  if common_url_list:
    input_solr_urls = common_url_list
    output_solr_urls = common_url_list

  return get_random_solr_url(input_solr_urls), get_random_solr_url(output_solr_urls)

def is_atlas_available(config, service_filter):
  return 'ATLAS' in service_filter and config.has_section('atlas_collections') \
    and config.has_option('atlas_collections', 'enabled') and config.get('atlas_collections', 'enabled') == 'true'

def is_ranger_available(config, service_filter):
  return 'RANGER' in service_filter and config.has_section('ranger_collection') \
    and config.has_option('ranger_collection', 'enabled') and config.get('ranger_collection', 'enabled') == 'true'

def is_logsearch_available(config, service_filter):
  return 'LOGSEARCH' in service_filter and config.has_section('logsearch_collections') \
    and config.has_option('logsearch_collections', 'enabled') and config.get('logsearch_collections', 'enabled') == 'true'

def monitor_solr_async_request(options, config, status_request, request_id):
  request_status_json_cmd=create_solr_api_request_command(status_request, config)
  logger.debug("Solr request: {0}".format(status_request))
  async_request_success_msg = "Async Solr request (id: {0}) {1}COMPLETED{2}".format(request_id, colors.OKGREEN, colors.ENDC)
  async_request_timeout_msg = "Async Solr request (id: {0}) {1}FAILED{2}".format(request_id, colors.FAIL, colors.ENDC)
  async_request_fail_msg = "\nAsync Solr request (id: {0}) {1}TIMED OUT{2} (increase --solr-async-request-tries if required, default is 400)".format(request_id, colors.FAIL, colors.ENDC)
  max_tries = options.solr_async_request_tries if options.solr_async_request_tries else 400
  tries = 0
  sys.stdout.write("Start monitoring Solr request with id {0} ...".format(request_id))
  sys.stdout.flush()
  async_request_finished = False
  async_request_failed = False
  async_request_timed_out = False
  while not async_request_finished:
    tries = tries + 1
    process = Popen(request_status_json_cmd, stdout=PIPE, stderr=PIPE, shell=True)
    out, err = process.communicate()
    if process.returncode != 0:
      raise Exception("{0} command failed: {1}".format(request_status_json_cmd, str(err)))
    else:
      response=json.loads(str(out))
      logger.debug(response)
      if 'status' in response:
        async_state=response['status']['state']
        async_msg=response['status']['msg']
        if async_state == "completed":
          async_request_finished = True
          sys.stdout.write("\nSolr response message: {0}\n".format(async_msg))
          sys.stdout.flush()
        elif async_state == "failed":
          async_request_finished = True
          async_request_failed = True
          sys.stdout.write("\nSolr response message: {0}\n".format(async_msg))
          sys.stdout.flush()
        else:
          if not options.verbose:
            sys.stdout.write(".")
            sys.stdout.flush()
          logger.debug(str(async_msg))
          logger.debug("Sleep 5 seconds ...")
          time.sleep(5)
      else:
        raise Exception("The 'status' field is missing from the response: {0}".format(response))
    if tries == max_tries:
      async_request_finished = True
      async_request_timed_out = True

  if async_request_failed:
    if async_request_timed_out:
      print async_request_timeout_msg
      sys.exit(1)
    else:
      print async_request_fail_msg
      sys.exit(1)
  else:
    print async_request_success_msg
    return request_id


def delete_collection(options, config, collection, solr_urls, response_data_map):
  async_id = str(randint(1000,100000))
  solr_url = get_random_solr_url(solr_urls, options)
  request = DELETE_SOLR_COLLECTION_URL.format(solr_url, collection, async_id)
  logger.debug("Solr request: {0}".format(request))
  delete_collection_json_cmd=create_solr_api_request_command(request, config)
  process = Popen(delete_collection_json_cmd, stdout=PIPE, stderr=PIPE, shell=True)
  out, err = process.communicate()
  if process.returncode != 0:
    raise Exception("{0} command failed: {1}".format(delete_collection_json_cmd, str(err)))
  response=json.loads(str(out))
  if 'requestid' in response:
    print 'Deleting collection {0} request sent. {1}DONE{2}'.format(collection, colors.OKGREEN, colors.ENDC)
    response_data_map['request_id']=response['requestid']
    response_data_map['status_request']=REQUEST_STATUS_SOLR_COLLECTION_URL.format(solr_url, response['requestid'])
    return collection
  else:
    raise Exception("DELETE collection ('{0}') failed. Response: {1}".format(collection, str(out)))

def create_collection(options, config, solr_urls, collection, config_set, shards, replica, max_shards_per_node):
  request = CREATE_SOLR_COLLECTION_URL.format(get_random_solr_url(solr_urls, options), collection, config_set, shards, replica, max_shards_per_node)
  logger.debug("Solr request: {0}".format(request))
  create_collection_json_cmd=create_solr_api_request_command(request, config)
  process = Popen(create_collection_json_cmd, stdout=PIPE, stderr=PIPE, shell=True)
  out, err = process.communicate()
  if process.returncode != 0:
    raise Exception("{0} command failed: {1}".format(create_collection_json_cmd, str(err)))
  response=json.loads(str(out))
  if 'success' in response:
    print 'Creating collection {0} was {1}SUCCESSFUL{2}'.format(collection, colors.OKGREEN, colors.ENDC)
    return collection
  else:
    raise Exception("CREATE collection ('{0}') failed. ({1}) Response: {1}".format(collection, str(out)))

def reload_collection(options, config, solr_urls, collection):
  request = RELOAD_SOLR_COLLECTION_URL.format(get_random_solr_url(solr_urls, options), collection)
  logger.debug("Solr request: {0}".format(request))
  reload_collection_json_cmd=create_solr_api_request_command(request, config)
  process = Popen(reload_collection_json_cmd, stdout=PIPE, stderr=PIPE, shell=True)
  out, err = process.communicate()
  if process.returncode != 0:
    raise Exception("{0} command failed: {1}".format(reload_collection_json_cmd, str(err)))
  response=json.loads(str(out))
  if 'success' in response:
    print 'Reloading collection {0} was {1}SUCCESSFUL{2}'.format(collection, colors.OKGREEN, colors.ENDC)
    return collection
  else:
    raise Exception("RELOAD collection ('{0}') failed. ({1}) Response: {1}".format(collection, str(out)))

def human_size(size_bytes):
  if size_bytes == 1:
    return "1 byte"
  suffixes_table = [('bytes',0),('KB',2),('MB',2),('GB',2),('TB',2), ('PB',2)]
  num = float(size_bytes)
  for suffix, precision in suffixes_table:
    if num < 1024.0:
      break
    num /= 1024.0
  if precision == 0:
    formatted_size = "%d" % num
  else:
    formatted_size = str(round(num, ndigits=precision))
  return "%s %s" % (formatted_size, suffix)

def parse_size(human_size):
  import locale
  units = {"bytes": 1, "KB": 1024, "MB": 1024**2, "GB": 1024**3, "TB": 1024**4 }
  number, unit = [string.strip() for string in human_size.split()]
  locale.setlocale(locale.LC_ALL,'')
  return int(locale.atof(number)*units[unit])

def get_replica_index_size(config, core_url, replica):
  request = CORE_DETAILS_URL.format(core_url)
  logger.debug("Solr request: {0}".format(request))
  get_core_detaul_json_cmd=create_solr_api_request_command(request, config)
  process = Popen(get_core_detaul_json_cmd, stdout=PIPE, stderr=PIPE, shell=True)
  out, err = process.communicate()
  if process.returncode != 0:
    raise Exception("{0} command failed: {1}".format(get_core_detaul_json_cmd, str(err)))
  response=json.loads(str(out))
  if 'details' in response:
    if 'indexSize' in response['details']:
      return response['details']['indexSize']
    else:
      raise Exception("Not found 'indexSize' in core details ('{0}'). Response: {1}".format(replica, str(out)))
  else:
    raise Exception("GET core details ('{0}') failed. Response: {1}".format(replica, str(out)))

def delete_znode(options, config, znode):
  solr_cli_command=create_infra_solr_client_command(options, config, '--delete-znode --znode {0}'.format(znode))
  logger.debug("Solr cli command: {0}".format(solr_cli_command))
  sys.stdout.write('Deleting znode {0} ... '.format(znode))
  sys.stdout.flush()
  process = Popen(solr_cli_command, stdout=PIPE, stderr=PIPE, shell=True)
  out, err = process.communicate()
  if process.returncode != 0:
    sys.stdout.write(colors.FAIL + 'FAILED\n' + colors.ENDC)
    sys.stdout.flush()
    raise Exception("{0} command failed: {1}".format(solr_cli_command, str(err)))
  sys.stdout.write(colors.OKGREEN + 'DONE\n' + colors.ENDC)
  sys.stdout.flush()
  logger.debug(str(out))

def copy_znode(options, config, copy_src, copy_dest, copy_from_local=False, copy_to_local=False):
  solr_cli_command=create_infra_solr_client_command(options, config, '--transfer-znode --copy-src {0} --copy-dest {1}'.format(copy_src, copy_dest))
  if copy_from_local:
    solr_cli_command+=" --transfer-mode copyFromLocal"
  elif copy_to_local:
    solr_cli_command+=" --transfer-mode copyToLocal"
  logger.debug("Solr cli command: {0}".format(solr_cli_command))
  sys.stdout.write('Transferring data from {0} to {1} ... '.format(copy_src, copy_dest))
  sys.stdout.flush()
  process = Popen(solr_cli_command, stdout=PIPE, stderr=PIPE, shell=True)
  out, err = process.communicate()
  if process.returncode != 0:
    sys.stdout.write(colors.FAIL + 'FAILED\n' + colors.ENDC)
    sys.stdout.flush()
    raise Exception("{0} command failed: {1}".format(solr_cli_command, str(err)))
  sys.stdout.write(colors.OKGREEN + 'DONE\n' + colors.ENDC)
  sys.stdout.flush()
  logger.debug(str(out))

def list_collections(options, config, output_file, include_number_of_docs=False):
  dump_json_files_list=[]
  skip_dump=False
  if options.skip_json_dump_files:
    dump_json_files_list=options.skip_json_dump_files.split(',')
  if dump_json_files_list:
    for dump_json_file in dump_json_files_list:
      if output_file.endswith(dump_json_file):
        skip_dump=True
  if skip_dump:
    print 'Skipping collection dump file generation: {0}'.format(output_file)
    if not os.path.exists(output_file):
      print "{0}FAIL{1}: Collection dump file '{2}' does not exist.".format(colors.FAIL, colors.ENDC, output_file)
      sys.exit(1)
  else:
    command_suffix = '--dump-collections --output {0}'.format(output_file)
    if include_number_of_docs:
      command_suffix+=' --include-doc-number'
    solr_cli_command=create_infra_solr_client_command(options, config, command_suffix, appendZnode=True)
    logger.debug("Solr cli command: {0}".format(solr_cli_command))
    sys.stdout.write('Dumping collections data to {0} ... '.format(output_file))
    sys.stdout.flush()
    process = Popen(solr_cli_command, stdout=PIPE, stderr=PIPE, shell=True)
    out, err = process.communicate()
    if process.returncode != 0:
      sys.stdout.write(colors.FAIL + 'FAILED\n' + colors.ENDC)
      sys.stdout.flush()
      raise Exception("{0} command failed: {1}".format(solr_cli_command, str(err)))
    sys.stdout.write(colors.OKGREEN + 'DONE\n' + colors.ENDC)
    sys.stdout.flush()
    logger.debug(str(out))
  collections_data = get_collections_data(output_file)
  return collections_data.keys() if collections_data is not None else []

def get_collections_data(output_file):
  return read_json(output_file)

def get_collection_data(collections_data, collection):
  return collections_data[collection] if collection in collections_data else None

def delete_logsearch_collections(options, config, collections_json_location, collections):
  service_logs_collection = config.get('logsearch_collections', 'hadoop_logs_collection_name')
  audit_logs_collection = config.get('logsearch_collections', 'audit_logs_collection_name')
  history_collection = config.get('logsearch_collections', 'history_collection_name')
  if service_logs_collection in collections:
    solr_urls = get_solr_urls(options, config, service_logs_collection, collections_json_location)
    response_map={}
    retry(delete_collection, options, config, service_logs_collection, solr_urls, response_map, context='[Delete {0} collection]'.format(service_logs_collection))
    retry(monitor_solr_async_request, options, config, response_map['status_request'], response_map['request_id'],
          context="[Monitor Solr async request, id: {0}]".format(response_map['request_id']))
  else:
    print 'Collection {0} does not exist or filtered out. Skipping delete operation.'.format(service_logs_collection)
  if audit_logs_collection in collections:
    solr_urls = get_solr_urls(options, config, audit_logs_collection, collections_json_location)
    response_map={}
    retry(delete_collection, options, config, audit_logs_collection, solr_urls, response_map, context='[Delete {0} collection]'.format(audit_logs_collection))
    retry(monitor_solr_async_request, options, config, response_map['status_request'], response_map['request_id'],
          context="[Monitor Solr async request, id: {0}]".format(response_map['request_id']))
  else:
    print 'Collection {0} does not exist or filtered out. Skipping delete operation.'.format(audit_logs_collection)
  if history_collection in collections:
    solr_urls = get_solr_urls(options, config, history_collection, collections_json_location)
    response_map={}
    retry(delete_collection, options, config, history_collection, solr_urls, response_map, context='[Delete {0} collection]'.format(history_collection))
    retry(monitor_solr_async_request, options, config, response_map['status_request'], response_map['request_id'],
          context="[Monitor Solr async request, id: {0}]".format(response_map['request_id']))
  else:
    print 'Collection {0} does not exist or filtered out. Skipping delete operation.'.format(history_collection)

def delete_atlas_collections(options, config, collections_json_location, collections):
  fulltext_collection = config.get('atlas_collections', 'fulltext_index_name')
  edge_index_collection = config.get('atlas_collections', 'edge_index_name')
  vertex_index_collection = config.get('atlas_collections', 'vertex_index_name')
  if fulltext_collection in collections:
    solr_urls = get_solr_urls(options, config, fulltext_collection, collections_json_location)
    response_map={}
    retry(delete_collection, options, config, fulltext_collection, solr_urls, response_map, context='[Delete {0} collection]'.format(fulltext_collection))
    retry(monitor_solr_async_request, options, config, response_map['status_request'], response_map['request_id'],
          context="[Monitor Solr async request, id: {0}]".format(response_map['request_id']))
  else:
    print 'Collection {0} does not exist or filtered out. Skipping delete operation.'.format(fulltext_collection)
  if edge_index_collection in collections:
    solr_urls = get_solr_urls(options, config, edge_index_collection, collections_json_location)
    response_map={}
    retry(delete_collection, options, config, edge_index_collection, solr_urls, response_map, context='[Delete {0} collection]'.format(edge_index_collection))
    retry(monitor_solr_async_request, options, config, response_map['status_request'], response_map['request_id'],
          context="[Monitor Solr async request, id: {0}]".format(response_map['request_id']))
  else:
    print 'Collection {0} does not exist or filtered out. Skipping delete operation.'.format(edge_index_collection)
  if vertex_index_collection in collections:
    solr_urls = get_solr_urls(options, config, vertex_index_collection, collections_json_location)
    response_map={}
    retry(delete_collection, options, config, vertex_index_collection, solr_urls, response_map, context='[Delete {0} collection]'.format(vertex_index_collection))
    retry(monitor_solr_async_request, options, config, response_map['status_request'], response_map['request_id'],
          context="[Monitor Solr async request, id: {0}]".format(response_map['request_id']))
  else:
    print 'Collection {0} does not exist or filtered out. Skipping delete operation.'.format(vertex_index_collection)

def delete_ranger_collection(options, config, collections_json_location, collections):
  ranger_collection_name = config.get('ranger_collection', 'ranger_collection_name')
  if ranger_collection_name in collections:
    solr_urls = get_solr_urls(options, config, ranger_collection_name, collections_json_location)
    response_map={}
    retry(delete_collection, options, config, ranger_collection_name, solr_urls, response_map, context='[Delete {0} collection]'.format(ranger_collection_name))
    retry(monitor_solr_async_request, options, config, response_map['status_request'], response_map['request_id'],
          context="[Monitor Solr async request, id: {0}]".format(response_map['request_id']))
  else:
    print 'Collection {0} does not exist or filtered out. Skipping delete operation'.format(ranger_collection_name)

def delete_collections(options, config, service_filter):
  collections_json_location = COLLECTIONS_DATA_JSON_LOCATION.format("delete_collections.json")
  collections=list_collections(options, config, collections_json_location)
  collections=filter_collections(options, collections)
  if is_ranger_available(config, service_filter):
    delete_ranger_collection(options, config, collections_json_location, collections)
  if is_atlas_available(config, service_filter):
    delete_atlas_collections(options, config, collections_json_location, collections)
  if is_logsearch_available(config, service_filter):
    delete_logsearch_collections(options, config, collections_json_location, collections)

def upgrade_ranger_schema(options, config, service_filter):
  solr_znode='/infra-solr'
  if is_ranger_available(config, service_filter):
    if config.has_section('infra_solr') and config.has_option('infra_solr', 'znode'):
      solr_znode=config.get('infra_solr', 'znode')
    ranger_config_set_name = config.get('ranger_collection', 'ranger_config_set_name')
    copy_znode(options, config, "{0}{1}".format(INFRA_SOLR_CLIENT_BASE_PATH, RANGER_NEW_SCHEMA),
               "{0}/configs/{1}/managed-schema".format(solr_znode, ranger_config_set_name), copy_from_local=True)

def backup_ranger_configs(options, config, service_filter):
  solr_znode='/infra-solr'
  if is_ranger_available(config, service_filter):
    if config.has_section('infra_solr') and config.has_option('infra_solr', 'znode'):
      solr_znode=config.get('infra_solr', 'znode')
    ranger_config_set_name = config.get('ranger_collection', 'ranger_config_set_name')
    backup_ranger_config_set_name = config.get('ranger_collection', 'backup_ranger_config_set_name')
    copy_znode(options, config, "{0}/configs/{1}".format(solr_znode, ranger_config_set_name),
               "{0}/configs/{1}".format(solr_znode, backup_ranger_config_set_name))

def upgrade_ranger_solrconfig_xml(options, config, service_filter):
  solr_znode='/infra-solr'
  if is_ranger_available(config, service_filter):
    if config.has_section('infra_solr') and config.has_option('infra_solr', 'znode'):
      solr_znode=config.get('infra_solr', 'znode')
    ranger_config_set_name = config.get('ranger_collection', 'ranger_config_set_name')
    backup_ranger_config_set_name = config.get('ranger_collection', 'backup_ranger_config_set_name')
    copy_znode(options, config, "{0}/configs/{1}/solrconfig.xml".format(solr_znode, ranger_config_set_name),
               "{0}/configs/{1}/solrconfig.xml".format(solr_znode, backup_ranger_config_set_name))

def evaluate_check_shard_result(collection, result, skip_index_size = False):
  evaluate_result = {}
  active_shards = result['active_shards']
  all_shards = result['all_shards']
  warnings = 0
  print 30 * "-"
  print "Number of shards: {0}".format(str(len(all_shards)))
  for shard in all_shards:
    if shard in active_shards:
      print "{0}OK{1}: Found active leader replica for {2}" \
        .format(colors.OKGREEN, colors.ENDC, shard)
    else:
      warnings=warnings+1
      print "{0}WARNING{1}: Not found any active leader replicas for {2}, migration will probably fail, fix or delete the shard if it is possible." \
        .format(colors.WARNING, colors.ENDC, shard)

  if not skip_index_size:
    index_size_map = result['index_size_map']
    host_index_size_map = result['host_index_size_map']
    if index_size_map:
      print "Index size per shard for {0}:".format(collection)
      for shard in index_size_map:
        print " - {0}: {1}".format(shard, human_size(index_size_map[shard]))
    if host_index_size_map:
      print "Index size per host for {0} (consider this for backup): ".format(collection)
      for host in host_index_size_map:
        print " - {0}: {1}".format(host, human_size(host_index_size_map[host]))
      evaluate_result['host_index_size_map'] = host_index_size_map
  print 30 * "-"
  evaluate_result['warnings'] = warnings
  return evaluate_result

def check_shard_for_collection(config, collection, skip_index_size = False):
  result = {}
  active_shards = []
  all_shards = []
  index_size_map = {}
  host_index_size_map = {}
  collections_data = get_collections_data(COLLECTIONS_DATA_JSON_LOCATION.format("check_collections.json"))
  print "Checking available shards for '{0}' collection...".format(collection)
  if collection in collections_data:
    collection_details = collections_data[collection]
    if 'shards' in collection_details:
      for shard in collection_details['shards']:
        all_shards.append(shard)
        if 'replicas' in collection_details['shards'][shard]:
          for replica in collection_details['shards'][shard]['replicas']:
            if 'state' in collection_details['shards'][shard]['replicas'][replica] \
              and collection_details['shards'][shard]['replicas'][replica]['state'].lower() == 'active' \
              and 'leader' in collection_details['shards'][shard]['replicas'][replica]['properties'] \
              and collection_details['shards'][shard]['replicas'][replica]['properties']['leader'] == 'true' :
              logger.debug("Found active shard for {0} (collection: {1})".format(shard, collection))
              active_shards.append(shard)
              if not skip_index_size:
                core_url = collection_details['shards'][shard]['replicas'][replica]['coreUrl']
                core_name = collection_details['shards'][shard]['replicas'][replica]['coreName']
                node_name = collection_details['shards'][shard]['replicas'][replica]['nodeName']
                hostname = node_name.split(":")[0]
                index_size = get_replica_index_size(config, core_url, core_name)
                index_bytes = parse_size(index_size)
                if hostname in host_index_size_map:
                  last_value = host_index_size_map[hostname]
                  host_index_size_map[hostname] = last_value + index_bytes
                else:
                  host_index_size_map[hostname] = index_bytes
                index_size_map[shard] = index_bytes
  result['active_shards'] = active_shards
  result['all_shards'] = all_shards
  if not skip_index_size:
    result['index_size_map'] = index_size_map
    result['host_index_size_map'] = host_index_size_map

  return result

def generate_core_pairs(original_collection, collection, config, options):
  core_pairs_data={}

  original_cores={}
  original_collections_data = get_collections_data(COLLECTIONS_DATA_JSON_LOCATION.format("backup_collections.json"))
  if original_collection in original_collections_data and 'leaderCoreHostMap' in original_collections_data[original_collection]:
    original_cores = original_collections_data[original_collection]['leaderCoreHostMap']

  sorted_original_cores=[]
  for key in sorted(original_cores):
    sorted_original_cores.append((key, original_cores[key]))

  new_cores={}
  collections_data = get_collections_data(COLLECTIONS_DATA_JSON_LOCATION.format("restore_collections.json"))
  if collection in collections_data and 'leaderCoreHostMap' in collections_data[collection]:
    new_cores = collections_data[collection]['leaderCoreHostMap']

  sorted_new_cores=[]
  for key in sorted(new_cores):
    sorted_new_cores.append((key, new_cores[key]))

  if len(new_cores) < len(original_cores):
    raise Exception("Old collection core size is: " + str(len(new_cores)) +
                    ". You will need at least: " + str(len(original_cores)))
  else:
    for index, original_core_data in enumerate(sorted_original_cores):
      core_pairs_data[sorted_new_cores[index][0]]=original_core_data[0]
    with open(COLLECTIONS_DATA_JSON_LOCATION.format(collection + "/restore_core_pairs.json"), 'w') as outfile:
      json.dump(core_pairs_data, outfile)
    return core_pairs_data

def get_number_of_docs_map(collection_dump_file):
  collections_data = get_collections_data(collection_dump_file)
  doc_num_map={}
  for collection in collections_data:
    number_of_docs=collections_data[collection]['numberOfDocs']
    doc_num_map[collection]=number_of_docs
  return doc_num_map

def is_collection_empty(docs_map, collection):
  result = False
  if collection in docs_map:
    num_docs=docs_map[collection]
    if num_docs == -1:
      print "Number of documents: -1. That means the number of docs was not provided in the collection dump."
    elif num_docs == 0:
      result = True
  return result

def update_state_json(original_collection, collection, config, options):
  solr_znode='/infra-solr'
  if config.has_section('infra_solr') and config.has_option('infra_solr', 'znode'):
    solr_znode=config.get('infra_solr', 'znode')
  coll_data_dir = "{0}migrate/data/{1}".format(INFRA_SOLR_CLIENT_BASE_PATH, collection)
  if not os.path.exists(coll_data_dir):
    os.makedirs(coll_data_dir)

  copy_znode(options, config, "{0}/collections/{1}/state.json".format(solr_znode, collection), "{0}/state.json".format(coll_data_dir), copy_to_local=True)
  copy_znode(options, config, "{0}/restore_metadata/{1}".format(solr_znode, collection), "{0}".format(coll_data_dir), copy_to_local=True)

  json_file_list=glob.glob("{0}/*.json".format(coll_data_dir))
  logger.debug("Downloaded json files list: {0}".format(str(json_file_list)))

  cores_data_json_list = [k for k in json_file_list if 'state.json' not in k and 'new_state.json' not in k and 'restore_core_pairs.json' not in k]
  state_json_list = [k for k in json_file_list if '/state.json' in k]

  if not cores_data_json_list:
    raise Exception('Cannot find any downloaded restore core metadata for {0}'.format(collection))
  if not state_json_list:
    raise Exception('Cannot find any downloaded restore collection state metadata for {0}'.format(collection))

  core_pairs = generate_core_pairs(original_collection, collection, config, options)
  cores_to_skip = []
  logger.debug("Generated core pairs: {0}".format(str(core_pairs)))
  if options.skip_cores:
    cores_to_skip = options.skip_cores.split(',')
  logger.debug("Cores to skip: {0}".format(str(cores_to_skip)))

  state_json_file=state_json_list[0]
  state_data = read_json(state_json_file)
  core_json_data=[]

  for core_data_json_file in cores_data_json_list:
    core_json_data.append(read_json(core_data_json_file))

  logger.debug("collection data content: {0}".format(str(state_data)))
  core_details={}
  for core in core_json_data:
    core_details[core['core_node']]=core
  logger.debug("core data contents: {0}".format(str(core_details)))

  collection_data = state_data[collection]
  shards = collection_data['shards']
  new_state_json_data=copy.deepcopy(state_data)

  for shard in shards:
    replicas = shards[shard]['replicas']
    for replica in replicas:
      core_data = replicas[replica]
      core = core_data['core']
      base_url = core_data['base_url']
      node_name = core_data['node_name']
      data_dir = core_data['dataDir'] if 'dataDir' in core_data else None
      ulog_dir = core_data['ulogDir'] if 'ulogDir' in core_data else None

      if cores_to_skip and (core in cores_to_skip or (core in core_pairs and core_pairs[core] in cores_to_skip)):
        print "Skipping core '{0}' as it is in skip-cores list (or its original pair: '{1}')".format(core, core_pairs[core])
      elif replica in core_details:
        old_core_node=core_details[replica]['core_node']
        new_core_node=core_details[replica]['new_core_node']

        new_state_core = copy.deepcopy(state_data[collection]['shards'][shard]['replicas'][replica])
        new_state_json_data[collection]['shards'][shard]['replicas'][new_core_node]=new_state_core
        if old_core_node != new_core_node:
          if old_core_node in new_state_json_data[collection]['shards'][shard]['replicas']:
            del new_state_json_data[collection]['shards'][shard]['replicas'][old_core_node]
          if data_dir:
            new_state_json_data[collection]['shards'][shard]['replicas'][new_core_node]['dataDir']=data_dir.replace(old_core_node, new_core_node)
          if ulog_dir:
            new_state_json_data[collection]['shards'][shard]['replicas'][new_core_node]['ulogDir']=ulog_dir.replace(old_core_node, new_core_node)
        old_host=core_details[replica]['old_host']
        new_host=core_details[replica]['new_host']
        if old_host != new_host and old_core_node != new_core_node:
          new_state_json_data[collection]['shards'][shard]['replicas'][new_core_node]['base_url']=base_url.replace(old_host, new_host)
          new_state_json_data[collection]['shards'][shard]['replicas'][new_core_node]['node_name']=node_name.replace(old_host, new_host)
        elif old_host != new_host:
          new_state_json_data[collection]['shards'][shard]['replicas'][replica]['base_url']=base_url.replace(old_host, new_host)
          new_state_json_data[collection]['shards'][shard]['replicas'][replica]['node_name']=node_name.replace(old_host, new_host)

  with open("{0}/new_state.json".format(coll_data_dir), 'w') as outfile:
    json.dump(new_state_json_data, outfile)

  copy_znode(options, config, "{0}/new_state.json".format(coll_data_dir), "{0}/collections/{1}/state.json".format(solr_znode, collection), copy_from_local=True)

def delete_znodes(options, config, service_filter):
  solr_znode='/infra-solr'
  if is_logsearch_available(config, service_filter):
    if config.has_section('infra_solr') and config.has_option('infra_solr', 'znode'):
      solr_znode=config.get('infra_solr', 'znode')
    delete_znode(options, config, "{0}/configs/hadoop_logs".format(solr_znode))
    delete_znode(options, config, "{0}/configs/audit_logs".format(solr_znode))
    delete_znode(options, config, "{0}/configs/history".format(solr_znode))

def do_backup_request(options, accessor, parser, config, collection, index_location):
  sys.stdout.write("Sending backup collection request ('{0}') to Ambari to process (backup destination: '{1}')..."
                   .format(collection, index_location))
  sys.stdout.flush()
  response = backup(options, accessor, parser, config, collection, index_location)
  request_id = get_request_id(response)
  sys.stdout.write(colors.OKGREEN + 'DONE\n' + colors.ENDC)
  sys.stdout.flush()
  print 'Backup command request id: {0}'.format(request_id)
  if options.async:
    print "Backup {0} collection request sent to Ambari server. Check Ambari UI about the results.".format(collection)
    sys.exit(0)
  else:
    sys.stdout.write("Start monitoring Ambari request with id {0} ...".format(request_id))
    sys.stdout.flush()
    cluster = config.get('ambari_server', 'cluster')
    monitor_request(options, accessor, cluster, request_id, 'Backup Solr collection: ' + collection)
    print "Backup collection '{0}'... {1}DONE{2}".format(collection, colors.OKGREEN, colors.ENDC)

def do_migrate_request(options, accessor, parser, config, collection, index_location):
  sys.stdout.write("Sending migrate collection request ('{0}') to Ambari to process (migrate folder: '{1}')..."
                   .format(collection, index_location))
  sys.stdout.flush()
  response = migrate(options, accessor, parser, config, collection, index_location)
  request_id = get_request_id(response)
  sys.stdout.write(colors.OKGREEN + 'DONE\n' + colors.ENDC)
  sys.stdout.flush()
  print 'Migrate command request id: {0}'.format(request_id)
  if options.async:
    print "Migrate {0} collection index request sent to Ambari server. Check Ambari UI about the results.".format(collection)
    sys.exit(0)
  else:
    sys.stdout.write("Start monitoring Ambari request with id {0} ...".format(request_id))
    sys.stdout.flush()
    cluster = config.get('ambari_server', 'cluster')
    monitor_request(options, accessor, cluster, request_id, 'Migrate Solr collection index: ' + collection)
    print "Migrate index '{0}'... {1}DONE{2}".format(collection, colors.OKGREEN, colors.ENDC)

def do_restore_request(options, accessor, parser, config, original_collection, collection, config_set, index_location, shards, hdfs_path):
  sys.stdout.write("Sending restore collection request ('{0}') to Ambari to process (backup location: '{1}')..."
                   .format(collection, index_location))
  sys.stdout.flush()
  response = restore(options, accessor, parser, config, original_collection, collection, config_set, index_location, hdfs_path, shards)
  request_id = get_request_id(response)
  sys.stdout.write(colors.OKGREEN + 'DONE\n' + colors.ENDC)
  sys.stdout.flush()
  print 'Restore command request id: {0}'.format(request_id)
  if options.async:
    print "Restore {0} collection request sent to Ambari server. Check Ambari UI about the results.".format(collection)
    sys.exit(0)
  else:
    sys.stdout.write("Start monitoring Ambari request with id {0} ...".format(request_id))
    sys.stdout.flush()
    cluster = config.get('ambari_server', 'cluster')
    monitor_request(options, accessor, cluster, request_id, 'Restore Solr collection: ' + collection)
    print "Restoring collection '{0}'... {1}DONE{2}".format(collection, colors.OKGREEN, colors.ENDC)

def get_ranger_index_location(collection, config, options):
  ranger_index_location = None
  if options.index_location:
    ranger_index_location = os.path.join(options.index_location, "ranger")
  elif options.ranger_index_location:
    ranger_index_location = options.ranger_index_location
  elif config.has_option('ranger_collection', 'backup_path'):
    ranger_index_location = config.get('ranger_collection', 'backup_path')
  else:
    print "'backup_path'is missing from config file and --index-location or --ranger-index-location options are missing as well. Backup collection {0} {1}FAILED{2}." \
      .format(collection, colors.FAIL, colors.ENDC)
    sys.exit(1)
  return ranger_index_location

def get_atlas_index_location(collection, config, options):
  atlas_index_location = None
  if options.index_location:
    atlas_index_location = os.path.join(options.index_location, "atlas", collection)
  elif options.ranger_index_location:
    atlas_index_location = os.path.join(options.atlas_index_location, collection)
  elif config.has_option('atlas_collections', 'backup_path'):
    atlas_index_location = os.path.join(config.get('atlas_collections', 'backup_path'), collection)
  else:
    print "'backup_path'is missing from config file and --index-location or --atlas-index-location options are missing as well. Backup collection {0} {1}FAILED{2}." \
      .format(collection, colors.FAIL, colors.ENDC)
    sys.exit(1)
  return atlas_index_location

def backup_collections(options, accessor, parser, config, service_filter):
  collections=list_collections(options, config, COLLECTIONS_DATA_JSON_LOCATION.format("backup_collections.json"), include_number_of_docs=True)
  collections=filter_collections(options, collections)
  num_docs_map = get_number_of_docs_map(COLLECTIONS_DATA_JSON_LOCATION.format("backup_collections.json"))
  if is_ranger_available(config, service_filter):
    collection_name = config.get('ranger_collection', 'ranger_collection_name')
    if collection_name in collections:
      if is_collection_empty(num_docs_map, collection_name):
        print "Collection '{0}' is empty. Backup is not required.".format(collection_name)
      else:
        ranger_index_location=get_ranger_index_location(collection_name, config, options)
        do_backup_request(options, accessor, parser, config, collection_name, ranger_index_location)
    else:
      print 'Collection {0} does not exist or filtered out. Skipping backup operation.'.format(collection_name)
  if is_atlas_available(config, service_filter):
    fulltext_index_collection = config.get('atlas_collections', 'fulltext_index_name')
    if fulltext_index_collection in collections:
      if is_collection_empty(num_docs_map, fulltext_index_collection):
        print "Collection '{0}' is empty. Backup is not required.".format(fulltext_index_collection)
      else:
        fulltext_index_location = get_atlas_index_location(fulltext_index_collection, config, options)
        do_backup_request(options, accessor, parser, config, fulltext_index_collection, fulltext_index_location)
    else:
      print 'Collection {0} does not exist or filtered out. Skipping backup operation.'.format(fulltext_index_collection)
    vertex_index_collection = config.get('atlas_collections', 'vertex_index_name')
    if vertex_index_collection in collections:
      if is_collection_empty(num_docs_map, vertex_index_collection):
        print "Collection '{0}' is empty. Backup is not required.".format(vertex_index_collection)
      else:
        vertex_index_location = get_atlas_index_location(vertex_index_collection, config, options)
        do_backup_request(options, accessor, parser, config, vertex_index_collection, vertex_index_location)
    else:
      print 'Collection {0} does not exist or filtered out. Skipping backup operation.'.format(vertex_index_collection)
    edge_index_collection = config.get('atlas_collections', 'edge_index_name')
    if edge_index_collection in collections:
      if is_collection_empty(num_docs_map, edge_index_collection):
        print "Collection '{0}' is empty. Backup is not required.".format(edge_index_collection)
      else:
        edge_index_location = get_atlas_index_location(edge_index_collection, config, options)
        do_backup_request(options, accessor, parser, config, edge_index_collection, edge_index_location)
    else:
      print 'Collection {0} does not exist or filtered out. Skipping backup operation.'.format(edge_index_collection)

def migrate_snapshots(options, accessor, parser, config, service_filter):
  if is_ranger_available(config, service_filter):
    collection_name = config.get('ranger_collection', 'ranger_collection_name')
    if options.collection is None or options.collection == collection_name:
      ranger_index_location=get_ranger_index_location(collection_name, config, options)
      do_migrate_request(options, accessor, parser, config, collection_name, ranger_index_location)
    else:
      print "Collection '{0}' backup index has filtered out. Skipping migrate operation.".format(collection_name)
  if is_atlas_available(config, service_filter):
    fulltext_index_collection = config.get('atlas_collections', 'fulltext_index_name')
    if options.collection is None or options.collection == fulltext_index_collection:
      fulltext_index_location=get_atlas_index_location(fulltext_index_collection, config, options)
      do_migrate_request(options, accessor, parser, config, fulltext_index_collection, fulltext_index_location)
    else:
      print "Collection '{0}' backup index has filtered out. Skipping migrate operation.".format(fulltext_index_collection)
    vertex_index_collection = config.get('atlas_collections', 'vertex_index_name')
    if options.collection is None or options.collection == vertex_index_collection:
      vertex_index_location=get_atlas_index_location(vertex_index_collection, config, options)
      do_migrate_request(options, accessor, parser, config, vertex_index_collection, vertex_index_location)
    else:
      print "Collection '{0}' backup index has filtered out. Skipping migrate operation.".format(vertex_index_collection)
    edge_index_collection = config.get('atlas_collections', 'edge_index_name')
    if options.collection is None or options.collection == edge_index_collection:
      edge_index_location=get_atlas_index_location(edge_index_collection, config, options)
      do_migrate_request(options, accessor, parser, config, edge_index_collection, edge_index_location)
    else:
      print "Collection '{0}' backup index has filtered out. Skipping migrate operation.".format(edge_index_collection)

def create_backup_collections(options, accessor, parser, config, service_filter):
  collections_json_location = COLLECTIONS_DATA_JSON_LOCATION.format("before_restore_collections.json")
  num_docs_map = get_number_of_docs_map(COLLECTIONS_DATA_JSON_LOCATION.format("backup_collections.json"))
  collections=list_collections(options, config, collections_json_location)
  replica_number = "1" # hard coded
  if is_ranger_available(config, service_filter):
    original_ranger_collection = config.get('ranger_collection', 'ranger_collection_name')
    backup_ranger_collection = config.get('ranger_collection', 'backup_ranger_collection_name')
    if original_ranger_collection in collections:
      if is_collection_empty(num_docs_map, original_ranger_collection):
        print "Collection '{0}' was empty during backup. It won't need a backup collection.".format(original_ranger_collection)
      else:
        if backup_ranger_collection not in collections:
          if options.collection is not None and options.collection != backup_ranger_collection:
            print "Collection {0} has filtered out. Skipping create operation.".format(backup_ranger_collection)
          else:
            solr_urls = get_solr_urls(options, config, backup_ranger_collection, collections_json_location)
            backup_ranger_config_set = config.get('ranger_collection', 'backup_ranger_config_set_name')
            backup_ranger_shards = config.get('ranger_collection', 'ranger_collection_shards')
            backup_ranger_max_shards = config.get('ranger_collection', 'ranger_collection_max_shards_per_node')
            retry(create_collection, options, config, solr_urls, backup_ranger_collection, backup_ranger_config_set,
                        backup_ranger_shards, replica_number, backup_ranger_max_shards, context="[Create Solr Collections]")
        else:
          print "Collection {0} has already exist. Skipping create operation.".format(backup_ranger_collection)
  if is_atlas_available(config, service_filter):
    backup_atlas_config_set = config.get('atlas_collections', 'config_set')
    backup_fulltext_index_name = config.get('atlas_collections', 'backup_fulltext_index_name')
    original_fulltext_index_name = config.get('atlas_collections', 'fulltext_index_name')
    if original_fulltext_index_name in collections:
      if is_collection_empty(num_docs_map, original_fulltext_index_name):
        print "Collection '{0}' was empty during backup. It won't need a backup collection.".format(original_fulltext_index_name)
      else:
        if backup_fulltext_index_name not in collections:
          if options.collection is not None and options.collection != backup_fulltext_index_name:
            print "Collection {0} has filtered out. Skipping create operation.".format(backup_fulltext_index_name)
          else:
            solr_urls = get_solr_urls(options, config, backup_fulltext_index_name, collections_json_location)
            backup_fulltext_index_shards = config.get('atlas_collections', 'fulltext_index_shards')
            backup_fulltext_index_max_shards = config.get('atlas_collections', 'fulltext_index_max_shards_per_node')
            retry(create_collection, options, config, solr_urls, backup_fulltext_index_name, backup_atlas_config_set,
                        backup_fulltext_index_shards, replica_number, backup_fulltext_index_max_shards, context="[Create Solr Collections]")
        else:
          print "Collection {0} has already exist. Skipping create operation.".format(backup_fulltext_index_name)

    backup_edge_index_name = config.get('atlas_collections', 'backup_edge_index_name')
    original_edge_index_name = config.get('atlas_collections', 'edge_index_name')
    if original_edge_index_name in collections:
      if is_collection_empty(num_docs_map, original_edge_index_name):
        print "Collection '{0}' was empty during backup. It won't need a backup collection.".format(original_edge_index_name)
      else:
        if backup_edge_index_name not in collections:
          if options.collection is not None and options.collection != backup_edge_index_name:
            print "Collection {0} has filtered out. Skipping create operation.".format(backup_edge_index_name)
          else:
            solr_urls = get_solr_urls(options, config, backup_edge_index_name, collections_json_location)
            backup_edge_index_shards = config.get('atlas_collections', 'edge_index_shards')
            backup_edge_index_max_shards = config.get('atlas_collections', 'edge_index_max_shards_per_node')
            retry(create_collection, options, config, solr_urls, backup_edge_index_name, backup_atlas_config_set,
                        backup_edge_index_shards, replica_number, backup_edge_index_max_shards, context="[Create Solr Collections]")
        else:
          print "Collection {0} has already exist. Skipping create operation.".format(backup_edge_index_name)

    backup_vertex_index_name = config.get('atlas_collections', 'backup_vertex_index_name')
    original_vertex_index_name = config.get('atlas_collections', 'vertex_index_name')
    if original_vertex_index_name in collections:
      if is_collection_empty(num_docs_map, original_vertex_index_name):
        print "Collection '{0}' was empty during backup. It won't need a backup collection.".format(original_vertex_index_name)
      else:
        if backup_vertex_index_name not in collections:
          if options.collection is not None and options.collection != backup_vertex_index_name:
            print "Collection {0} has filtered out. Skipping create operation.".format(backup_vertex_index_name)
          else:
            solr_urls = get_solr_urls(options, config, backup_vertex_index_name, collections_json_location)
            backup_vertex_index_shards = config.get('atlas_collections', 'vertex_index_shards')
            backup_vertex_index_max_shards = config.get('atlas_collections', 'vertex_index_max_shards_per_node')
            retry(create_collection, options, config, solr_urls, backup_vertex_index_name, backup_atlas_config_set,
                        backup_vertex_index_shards, replica_number, backup_vertex_index_max_shards, context="[Create Solr Collections]")
        else:
          print "Collection {0} has already exist. Skipping create operation.".format(backup_fulltext_index_name)

def restore_collections(options, accessor, parser, config, service_filter):
  collections=list_collections(options, config, COLLECTIONS_DATA_JSON_LOCATION.format("restore_collections.json"))
  collections=filter_collections(options, collections)
  if 'RANGER' in service_filter and config.has_section('ranger_collection') and config.has_option('ranger_collection', 'enabled') \
    and config.get('ranger_collection', 'enabled') == 'true':
    collection_name = config.get('ranger_collection', 'ranger_collection_name')
    backup_ranger_collection = config.get('ranger_collection', 'backup_ranger_collection_name')
    backup_ranger_config_set_name = config.get('ranger_collection', 'backup_ranger_config_set_name')

    hdfs_base_path = None
    if options.ranger_hdfs_base_path:
      hdfs_base_path = options.ranger_hdfs_base_path
    elif options.hdfs_base_path:
      hdfs_base_path = options.hdfs_base_path
    elif config.has_option('ranger_collection', 'hdfs_base_path'):
      hdfs_base_path = config.get('ranger_collection', 'hdfs_base_path')
    if backup_ranger_collection in collections:
      backup_ranger_shards = config.get('ranger_collection', 'ranger_collection_shards')
      ranger_index_location=get_ranger_index_location(collection_name, config, options)
      do_restore_request(options, accessor, parser, config, collection_name, backup_ranger_collection, backup_ranger_config_set_name, ranger_index_location, backup_ranger_shards, hdfs_base_path)
    else:
      print "Collection '{0}' does not exist or filtered out. Skipping restore operation.".format(backup_ranger_collection)

  if is_atlas_available(config, service_filter):
    hdfs_base_path = None
    if options.ranger_hdfs_base_path:
      hdfs_base_path = options.atlas_hdfs_base_path
    elif options.hdfs_base_path:
      hdfs_base_path = options.hdfs_base_path
    elif config.has_option('atlas_collections', 'hdfs_base_path'):
      hdfs_base_path = config.get('atlas_collections', 'hdfs_base_path')
    atlas_config_set = config.get('atlas_collections', 'config_set')

    fulltext_index_collection = config.get('atlas_collections', 'fulltext_index_name')
    backup_fulltext_index_name = config.get('atlas_collections', 'backup_fulltext_index_name')
    if backup_fulltext_index_name in collections:
      backup_fulltext_index_shards = config.get('atlas_collections', 'fulltext_index_shards')
      fulltext_index_location=get_atlas_index_location(fulltext_index_collection, config, options)
      do_restore_request(options, accessor, parser, config, fulltext_index_collection, backup_fulltext_index_name, atlas_config_set, fulltext_index_location, backup_fulltext_index_shards, hdfs_base_path)
    else:
      print "Collection '{0}' does not exist or filtered out. Skipping restore operation.".format(fulltext_index_collection)

    edge_index_collection = config.get('atlas_collections', 'edge_index_name')
    backup_edge_index_name = config.get('atlas_collections', 'backup_edge_index_name')
    if backup_edge_index_name in collections:
      backup_edge_index_shards = config.get('atlas_collections', 'edge_index_shards')
      edge_index_location=get_atlas_index_location(edge_index_collection, config, options)
      do_restore_request(options, accessor, parser, config, edge_index_collection, backup_edge_index_name, atlas_config_set, edge_index_location, backup_edge_index_shards, hdfs_base_path)
    else:
      print "Collection '{0}' does not exist or filtered out. Skipping restore operation.".format(edge_index_collection)

    vertex_index_collection = config.get('atlas_collections', 'vertex_index_name')
    backup_vertex_index_name = config.get('atlas_collections', 'backup_vertex_index_name')
    if backup_vertex_index_name in collections:
      backup_vertex_index_shards = config.get('atlas_collections', 'vertex_index_shards')
      vertex_index_location=get_atlas_index_location(vertex_index_collection, config, options)
      do_restore_request(options, accessor, parser, config, vertex_index_collection, backup_vertex_index_name, atlas_config_set, vertex_index_location, backup_vertex_index_shards, hdfs_base_path)
    else:
      print "Collection '{0}' does not exist or filtered out. Skipping restore operation.".format(vertex_index_collection)

def reload_collections(options, accessor, parser, config, service_filter):
  collections_json_location = COLLECTIONS_DATA_JSON_LOCATION.format("reload_collections.json")
  collections=list_collections(options, config, collections_json_location)
  collections=filter_collections(options, collections)
  if is_ranger_available(config, service_filter):
    backup_ranger_collection = config.get('ranger_collection', 'backup_ranger_collection_name')
    if backup_ranger_collection in collections:
      solr_urls = get_solr_urls(options, config, backup_ranger_collection, collections_json_location)
      retry(reload_collection, options, config, solr_urls, backup_ranger_collection, context="[Reload Solr Collections]")
    else:
      print "Collection '{0}' does not exist or filtered out. Skipping reload operation.".format(backup_ranger_collection)
  if is_atlas_available(config, service_filter):
    backup_fulltext_index_name = config.get('atlas_collections', 'backup_fulltext_index_name')
    if backup_fulltext_index_name in collections:
      solr_urls = get_solr_urls(options, config, backup_fulltext_index_name, collections_json_location)
      retry(reload_collection, options, config, solr_urls, backup_fulltext_index_name, context="[Reload Solr Collections]")
    else:
      print "Collection '{0}' does not exist or filtered out. Skipping reload operation.".format(backup_fulltext_index_name)
    backup_edge_index_name = config.get('atlas_collections', 'backup_edge_index_name')
    if backup_edge_index_name in collections:
      solr_urls = get_solr_urls(options, config, backup_edge_index_name, collections_json_location)
      retry(reload_collection, options, config, solr_urls, backup_edge_index_name, context="[Reload Solr Collections]")
    else:
      print "Collection '{0}' does not exist or filtered out. Skipping reload operation.".format(backup_edge_index_name)
    backup_vertex_index_name = config.get('atlas_collections', 'backup_vertex_index_name')
    if backup_vertex_index_name in collections:
      solr_urls = get_solr_urls(options, config, backup_vertex_index_name, collections_json_location)
      retry(reload_collection, options, config, solr_urls, backup_vertex_index_name, context="[Reload Solr Collections]")
    else:
      print "Collection '{0}' does not exist or filtered out. Skipping reload operation.".format(backup_fulltext_index_name)

def validate_ini_file(options, parser):
  if options.ini_file is None:
    parser.print_help()
    print 'ini-file option is missing'
    sys.exit(1)
  elif not os.path.isfile(options.ini_file):
    parser.print_help()
    print 'ini file ({0}) does not exist'.format(options.ini_file)
    sys.exit(1)

def rolling_restart(options, accessor, parser, config, service_name, component_name, context):
  cluster = config.get('ambari_server', 'cluster')
  component_hosts = get_solr_hosts(options, accessor, cluster)
  interval_secs = options.batch_interval
  fault_tolerance = options.batch_fault_tolerance
  request_body = create_batch_command("RESTART", component_hosts, cluster, service_name, component_name, interval_secs, fault_tolerance, "Rolling restart Infra Solr Instances")
  post_json(accessor, BATCH_REQUEST_API_URL.format(cluster), request_body)
  print "{0} request sent. (check Ambari UI about the requests)".format(context)

def update_state_jsons(options, accessor, parser, config, service_filter):
  collections=list_collections(options, config, COLLECTIONS_DATA_JSON_LOCATION.format("collections.json"))
  collections=filter_collections(options, collections)
  if is_ranger_available(config, service_filter):
    original_ranger_collection = config.get('ranger_collection', 'ranger_collection_name')
    backup_ranger_collection = config.get('ranger_collection', 'backup_ranger_collection_name')
    if backup_ranger_collection in collections:
      update_state_json(original_ranger_collection, backup_ranger_collection, config, options)
    else:
      print "Collection '{0}' does not exist or filtered out. Skipping update collection state operation.".format(backup_ranger_collection)
  if is_atlas_available(config, service_filter):
    original_fulltext_index_name = config.get('atlas_collections', 'fulltext_index_name')
    backup_fulltext_index_name = config.get('atlas_collections', 'backup_fulltext_index_name')
    if backup_fulltext_index_name in collections:
      update_state_json(original_fulltext_index_name, backup_fulltext_index_name, config, options)
    else:
      print "Collection '{0}' does not exist or filtered out. Skipping update collection state operation.".format(backup_fulltext_index_name)
    original_edge_index_name = config.get('atlas_collections', 'edge_index_name')
    backup_edge_index_name = config.get('atlas_collections', 'backup_edge_index_name')
    if backup_edge_index_name in collections:
      update_state_json(original_edge_index_name, backup_edge_index_name, config, options)
    else:
      print "Collection '{0}' does not exist or filtered out. Skipping update collection state operation.".format(backup_edge_index_name)
    original_vertex_index_name = config.get('atlas_collections', 'vertex_index_name')
    backup_vertex_index_name = config.get('atlas_collections', 'backup_vertex_index_name')
    if backup_vertex_index_name in collections:
      update_state_json(original_vertex_index_name, backup_vertex_index_name, config, options)
    else:
      print "Collection '{0}' does not exist or filtered out. Skipping update collection state operation.".format(backup_fulltext_index_name)

def set_solr_authorization(options, accessor, parser, config, enable_authorization, fix_kerberos_config = False):
  solr_znode='/infra-solr'
  if config.has_section('infra_solr') and config.has_option('infra_solr', 'znode'):
    solr_znode=config.get('infra_solr', 'znode')
  kerberos_enabled='false'
  if config.has_section('cluster') and config.has_option('cluster', 'kerberos_enabled'):
    kerberos_enabled=config.get('cluster', 'kerberos_enabled')
  if kerberos_enabled == 'true':
    infra_solr_props = get_infra_solr_props(config, accessor)
    if enable_authorization:
      print "Enable Solr security.json management by Ambari ... "
      set_solr_security_management(infra_solr_props, accessor, enable = False)
      if fix_kerberos_config:
        set_solr_name_rules(infra_solr_props, accessor, False)
    else:
      print "Disable Solr authorization by uploading a new security.json and turn on security.json management by Ambari..."
      set_solr_security_management(infra_solr_props, accessor, enable = True)
      copy_znode(options, config, COLLECTIONS_DATA_JSON_LOCATION.format("security-without-authr.json"),
             "{0}/security.json".format(solr_znode), copy_from_local=True)
      if fix_kerberos_config:
        set_solr_name_rules(infra_solr_props, accessor, True)
  else:
    if fix_kerberos_config:
      print "Security is not enabled. Skipping enable/disable Solr authorization + fix infra-solr-env kerberos config operation."
    else:
      print "Security is not enabled. Skipping enable/disable Solr authorization operation."

def summarize_shard_check_result(check_results, skip_warnings = False, skip_index_size = False):
  warnings = 0
  index_size_per_host = {}
  for collection in check_results:
    warnings=warnings+check_results[collection]['warnings']
    if not skip_index_size and 'host_index_size_map' in check_results[collection]:
      host_index_size_map = check_results[collection]['host_index_size_map']
      for host in host_index_size_map:
        if host in index_size_per_host:
          last_value=index_size_per_host[host]
          index_size_per_host[host]=last_value+host_index_size_map[host]
        else:
          index_size_per_host[host]=host_index_size_map[host]
      pass
  if not skip_index_size and index_size_per_host:
    print "Full index size per hosts: (consider this for backup)"
    for host in index_size_per_host:
      print " - {0}: {1}".format(host, human_size(index_size_per_host[host]))

  print "All warnings: {0}".format(warnings)
  if warnings != 0 and not skip_warnings:
    print "Check shards - {0}FAILED{1} (warnings: {2}, fix warnings or use --skip-warnings flag to PASS) ".format(colors.FAIL, colors.ENDC, warnings)
    sys.exit(1)
  else:
    print "Check shards - {0}PASSED{1}".format(colors.OKGREEN, colors.ENDC)

def check_shards(options, accessor, parser, config, backup_shards = False):
  collections=list_collections(options, config, COLLECTIONS_DATA_JSON_LOCATION.format("check_collections.json"))
  collections=filter_collections(options, collections)
  check_results={}
  if is_ranger_available(config, service_filter):
    ranger_collection = config.get('ranger_collection', 'backup_ranger_collection_name') if backup_shards \
      else config.get('ranger_collection', 'ranger_collection_name')
    if ranger_collection in collections:
      ranger_collection_details = check_shard_for_collection(config, ranger_collection, options.skip_index_size)
      check_results[ranger_collection]=evaluate_check_shard_result(ranger_collection, ranger_collection_details, options.skip_index_size)
    else:
      print "Collection '{0}' does not exist or filtered out. Skipping check collection operation.".format(ranger_collection)
  if is_atlas_available(config, service_filter):
    fulltext_index_name = config.get('atlas_collections', 'backup_fulltext_index_name') if backup_shards \
      else config.get('atlas_collections', 'fulltext_index_name')
    if fulltext_index_name in collections:
      fulltext_collection_details = check_shard_for_collection(config, fulltext_index_name, options.skip_index_size)
      check_results[fulltext_index_name]=evaluate_check_shard_result(fulltext_index_name, fulltext_collection_details, options.skip_index_size)
    else:
      print "Collection '{0}' does not exist or filtered out. Skipping check collection operation.".format(fulltext_index_name)
    edge_index_name = config.get('atlas_collections', 'backup_edge_index_name') if backup_shards \
      else config.get('atlas_collections', 'edge_index_name')
    if edge_index_name in collections:
      edge_collection_details = check_shard_for_collection(config, edge_index_name, options.skip_index_size)
      check_results[edge_index_name]=evaluate_check_shard_result(edge_index_name, edge_collection_details, options.skip_index_size)
    else:
      print "Collection '{0}' does not exist or filtered out. Skipping check collection operation.".format(edge_index_name)
    vertex_index_name = config.get('atlas_collections', 'backup_vertex_index_name') if backup_shards \
      else config.get('atlas_collections', 'vertex_index_name')
    if vertex_index_name in collections:
      vertex_collection_details = check_shard_for_collection(config, vertex_index_name, options.skip_index_size)
      check_results[vertex_index_name]=evaluate_check_shard_result(vertex_index_name, vertex_collection_details, options.skip_index_size)
    else:
      print "Collection '{0}' does not exist or filtered out. Skipping check collection operation.".format(fulltext_index_name)
    summarize_shard_check_result(check_results, options.skip_warnings, options.skip_index_size)

def check_docs(options, accessor, parser, config):
  collections=list_collections(options, config, COLLECTIONS_DATA_JSON_LOCATION.format("check_docs_collections.json"), include_number_of_docs=True)
  if collections:
    print "Get the number of documents per collections ..."
    docs_map = get_number_of_docs_map(COLLECTIONS_DATA_JSON_LOCATION.format("check_docs_collections.json"))
    for collection_docs_data in docs_map:
      print "Collection: '{0}' - Number of docs: {1}".format(collection_docs_data, docs_map[collection_docs_data])
  else:
    print "Check number of documents - Not found any collections."

def run_solr_data_manager_on_collection(options, config, collections, src_collection, target_collection,
                                        collections_json_location, num_docs, skip_date_usage = True):
  if target_collection in collections and src_collection in collections:
    source_solr_urls = get_solr_urls(options, config, src_collection, collections_json_location)
    target_solr_urls = get_solr_urls(options, config, target_collection, collections_json_location)
    if is_collection_empty(num_docs, src_collection):
      print "Collection '{0}' is empty. Skipping transport data operation.".format(target_collection)
    else:
      src_solr_url, target_solr_url = get_input_output_solr_url(source_solr_urls, target_solr_urls)
      keytab, principal = get_keytab_and_principal(config)
      date_format = "%Y-%m-%dT%H:%M:%S.%fZ"
      d = datetime.now() + timedelta(days=365)
      end = d.strftime(date_format)
      print "Running solrDataManager.py (solr input collection: {0}, solr output collection: {1})"\
        .format(src_collection, target_collection)
      solr_data_manager.verbose = options.verbose
      solr_data_manager.set_log_level(True)
      solr_data_manager.save("archive", src_solr_url, src_collection, "evtTime", "id", end,
                             options.transport_read_block_size, options.transport_write_block_size,
                             False, None, None, keytab, principal, False, "none", None, None, None,
                             None, None, None, None, None, target_collection,
                             target_solr_url, "_version_", skip_date_usage)
  else:
    print "Collection '{0}' or {1} does not exist or filtered out. Skipping transport data operation.".format(target_collection, src_collection)

def transfer_old_data(options, accessor, parser, config):
  collections_json_location = COLLECTIONS_DATA_JSON_LOCATION.format("transport_collections.json")
  collections=list_collections(options, config, collections_json_location, include_number_of_docs=True)
  collections=filter_collections(options, collections)
  docs_map = get_number_of_docs_map(collections_json_location) if collections else {}
  if is_ranger_available(config, service_filter):
    original_ranger_collection = config.get('ranger_collection', 'ranger_collection_name')
    backup_ranger_collection = config.get('ranger_collection', 'backup_ranger_collection_name')
    run_solr_data_manager_on_collection(options, config, collections, backup_ranger_collection,
                                        original_ranger_collection, collections_json_location, docs_map, skip_date_usage=False)
  if is_atlas_available(config, service_filter):
    original_fulltext_index_name = config.get('atlas_collections', 'fulltext_index_name')
    backup_fulltext_index_name = config.get('atlas_collections', 'backup_fulltext_index_name')
    run_solr_data_manager_on_collection(options, config, collections, backup_fulltext_index_name,
                                        original_fulltext_index_name, collections_json_location, docs_map)

    original_edge_index_name = config.get('atlas_collections', 'edge_index_name')
    backup_edge_index_name = config.get('atlas_collections', 'backup_edge_index_name')
    run_solr_data_manager_on_collection(options, config, collections, backup_edge_index_name,
                                        original_edge_index_name, collections_json_location, docs_map)

    original_vertex_index_name = config.get('atlas_collections', 'vertex_index_name')
    backup_vertex_index_name = config.get('atlas_collections', 'backup_vertex_index_name')
    run_solr_data_manager_on_collection(options, config, collections, backup_vertex_index_name,
                                        original_vertex_index_name, collections_json_location, docs_map)


if __name__=="__main__":
  parser = optparse.OptionParser("usage: %prog [options]")

  parser.add_option("-a", "--action", dest="action", type="string", help="delete-collections | backup | cleanup-znodes | backup-and-cleanup | migrate | restore |' \
              ' rolling-restart-solr | rolling-restart-atlas | rolling-restart-ranger | check-shards | check-backup-shards | enable-solr-authorization | disable-solr-authorization |'\
              ' fix-solr5-kerberos-config | fix-solr7-kerberos-config | upgrade-solr-clients | upgrade-solr-instances | upgrade-logsearch-portal | upgrade-logfeeders | stop-logsearch |'\
              ' restart-solr |restart-logsearch | restart-ranger | restart-atlas | transport-old-data")
  parser.add_option("-i", "--ini-file", dest="ini_file", type="string", help="Config ini file to parse (required)")
  parser.add_option("-f", "--force", dest="force", default=False, action="store_true", help="force index upgrade even if it's the right version")
  parser.add_option("-v", "--verbose", dest="verbose", action="store_true", help="use for verbose logging")
  parser.add_option("-s", "--service-filter", dest="service_filter", default=None, type="string", help="run commands only selected services (comma separated: LOGSEARCH,ATLAS,RANGER)")
  parser.add_option("-c", "--collection", dest="collection", default=None, type="string", help="selected collection to run an operation")
  parser.add_option("--async", dest="async", action="store_true", default=False, help="async Ambari operations (backup | restore | migrate)")
  parser.add_option("--index-location", dest="index_location", type="string", help="location of the index backups. add ranger/atlas prefix after the path. required only if no backup path in the ini file")
  parser.add_option("--atlas-index-location", dest="atlas_index_location", type="string", help="location of the index backups (for atlas). required only if no backup path in the ini file")
  parser.add_option("--ranger-index-location", dest="ranger_index_location", type="string", help="location of the index backups (for ranger). required only if no backup path in the ini file")

  parser.add_option("--version", dest="index_version", type="string", default="6.6.2", help="lucene index version for migration (6.6.2 or 7.7.0)")
  parser.add_option("--solr-async-request-tries", dest="solr_async_request_tries", type="int", default=400,  help="number of max tries for async Solr requests (e.g.: delete operation)")
  parser.add_option("--request-tries", dest="request_tries", type="int", help="number of tries for BACKUP/RESTORE status api calls in the request")
  parser.add_option("--request-time-interval", dest="request_time_interval", type="int", help="time interval between BACKUP/RESTORE status api calls in the request")
  parser.add_option("--request-async", dest="request_async", action="store_true", default=False, help="skip BACKUP/RESTORE status api calls from the command")
  parser.add_option("--transport-read-block-size", dest="transport_read_block_size", type="string", help="block size to use for reading from solr during transport",default=10000)
  parser.add_option("--transport-write-block-size", dest="transport_write_block_size", type="string", help="number of records in the output files during transport", default=100000)
  parser.add_option("--include-solr-hosts", dest="include_solr_hosts", type="string", help="comma separated list of included solr hosts")
  parser.add_option("--exclude-solr-hosts", dest="exclude_solr_hosts", type="string", help="comma separated list of excluded solr hosts")
  parser.add_option("--disable-solr-host-check", dest="disable_solr_host_check", action="store_true", default=False, help="Disable to check solr hosts are good for the collection backups")
  parser.add_option("--core-filter", dest="core_filter", default=None, type="string", help="core filter for replica folders")
  parser.add_option("--skip-cores", dest="skip_cores", default=None, type="string", help="specific cores to skip (comma separated)")
  parser.add_option("--hdfs-base-path", dest="hdfs_base_path", default=None, type="string", help="hdfs base path where the collections are located (e.g.: /user/infrasolr). Use if both atlas and ranger collections are on hdfs.")
  parser.add_option("--ranger-hdfs-base-path", dest="ranger_hdfs_base_path", default=None, type="string", help="hdfs base path where the ranger collection is located (e.g.: /user/infra-solr). Use if only ranger collection is on hdfs.")
  parser.add_option("--atlas-hdfs-base-path", dest="atlas_hdfs_base_path", default=None, type="string", help="hdfs base path where the atlas collections are located (e.g.: /user/infra-solr). Use if only atlas collections are on hdfs.")
  parser.add_option("--keep-backup", dest="keep_backup", default=False, action="store_true", help="If it is turned on, Snapshot Solr data will not be deleted from the filesystem during restore.")
  parser.add_option("--batch-interval", dest="batch_interval", type="int", default=60 ,help="batch time interval (seconds) between requests (for restarting INFRA SOLR, default: 60)")
  parser.add_option("--batch-fault-tolerance", dest="batch_fault_tolerance", type="int", default=0 ,help="fault tolerance of tasks for batch request (for restarting INFRA SOLR, default: 0)")
  parser.add_option("--shared-drive", dest="shared_drive", default=False, action="store_true", help="Use if the backup location is shared between hosts. (override config from config ini file)")
  parser.add_option("--skip-json-dump-files", dest="skip_json_dump_files", type="string", help="comma separated list of files that won't be download during collection dump (could be useful if it is required to change something in manually in the already downloaded file)")
  parser.add_option("--skip-index-size", dest="skip_index_size", default=False, action="store_true", help="Skip index size check for check-shards or check-backup-shards")
  parser.add_option("--skip-warnings", dest="skip_warnings", default=False, action="store_true", help="Pass check-shards or check-backup-shards even if there are warnings")
  (options, args) = parser.parse_args()

  set_log_level(options.verbose)

  if options.verbose:
    print "Run command with args: {0}".format(str(sys.argv))

  validate_ini_file(options, parser)

  config = ConfigParser.RawConfigParser()
  config.read(options.ini_file)

  command_start_time = time.time()

  service_filter=options.service_filter.upper().split(',') if options.service_filter is not None else ['LOGSEARCH', 'ATLAS', 'RANGER']

  if options.action is None:
     parser.print_help()
     print 'action option is missing'
     sys.exit(1)
  else:
    if config.has_section('ambari_server'):
      host = config.get('ambari_server', 'host')
      port = config.get('ambari_server', 'port')
      protocol = config.get('ambari_server', 'protocol')
      username = config.get('ambari_server', 'username')
      password = config.get('ambari_server', 'password')
      accessor = api_accessor(host, username, password, protocol, port)

      if config.has_section('infra_solr') and config.has_option('infra_solr', 'hosts'):
        local_host = socket.getfqdn()
        solr_hosts = config.get('infra_solr', 'hosts')
        if solr_hosts and local_host not in solr_hosts.split(","):
          print "{0}WARNING{1}: Host '{2}' is not found in Infra Solr hosts ({3}). Migration commands won't work from here." \
            .format(colors.WARNING, colors.ENDC, local_host, solr_hosts)
      if options.action.lower() == 'backup':
        backup_ranger_configs(options, config, service_filter)
        backup_collections(options, accessor, parser, config, service_filter)
      elif options.action.lower() == 'delete-collections':
        delete_collections(options, config, service_filter)
        delete_znodes(options, config, service_filter)
        upgrade_ranger_schema(options, config, service_filter)
      elif options.action.lower() == 'cleanup-znodes':
        delete_znodes(options, config, service_filter)
        upgrade_ranger_schema(options, config, service_filter)
      elif options.action.lower() == 'backup-and-cleanup':
        backup_ranger_configs(options, config, service_filter)
        backup_collections(options, accessor, parser, config, service_filter)
        delete_collections(options, config, service_filter)
        delete_znodes(options, config, service_filter)
        upgrade_ranger_schema(options, config, service_filter)
      elif options.action.lower() == 'restore':
        upgrade_ranger_solrconfig_xml(options, config, service_filter)
        create_backup_collections(options, accessor, parser, config, service_filter)
        restore_collections(options, accessor, parser, config, service_filter)
        update_state_jsons(options, accessor, parser, config, service_filter)
      elif options.action.lower() == 'update-collection-state':
        update_state_jsons(options, accessor, parser, config, service_filter)
      elif options.action.lower() == 'reload':
        reload_collections(options, accessor, parser, config, service_filter)
      elif options.action.lower() == 'migrate':
        migrate_snapshots(options, accessor, parser, config, service_filter)
      elif options.action.lower() == 'upgrade-solr-clients':
        upgrade_solr_clients(options, accessor, parser, config)
      elif options.action.lower() == 'upgrade-solr-instances':
        upgrade_solr_instances(options, accessor, parser, config)
      elif options.action.lower() == 'upgrade-logsearch-portal':
        if is_logsearch_available(config, service_filter):
          upgrade_logsearch_portal(options, accessor, parser, config)
        else:
          print "LOGSEARCH service has not found in the config or filtered out."
      elif options.action.lower() == 'upgrade-logfeeders':
        if is_logsearch_available(config, service_filter):
          upgrade_logfeeders(options, accessor, parser, config)
        else:
          print "LOGSEARCH service has not found in the config or filtered out."
      elif options.action.lower() == 'stop-logsearch':
        if is_logsearch_available(config, service_filter):
          service_components_command(options, accessor, parser, config, LOGSEARCH_SERVICE_NAME, LOGSEARCH_SERVER_COMPONENT_NAME, "STOP", "Stop")
          service_components_command(options, accessor, parser, config, LOGSEARCH_SERVICE_NAME, LOGSEARCH_LOGFEEDER_COMPONENT_NAME, "STOP", "Stop")
        else:
          print "LOGSEARCH service has not found in the config or filtered out."
      elif options.action.lower() == 'restart-solr':
        service_components_command(options, accessor, parser, config, SOLR_SERVICE_NAME, SOLR_COMPONENT_NAME, "RESTART", "Restart")
      elif options.action.lower() == 'restart-logsearch':
        if is_logsearch_available(config, service_filter):
          service_components_command(options, accessor, parser, config, LOGSEARCH_SERVICE_NAME, LOGSEARCH_SERVER_COMPONENT_NAME, "RESTART", "Restart")
          service_components_command(options, accessor, parser, config, LOGSEARCH_SERVICE_NAME, LOGSEARCH_LOGFEEDER_COMPONENT_NAME, "RESTART", "Restart")
        else:
          print "LOGSEARCH service has not found in the config or filtered out."
      elif options.action.lower() == 'restart-atlas':
        if is_atlas_available(config, service_filter):
          service_components_command(options, accessor, parser, config, ATLAS_SERVICE_NAME, ATLAS_SERVER_COMPONENT_NAME, "RESTART", "Restart")
        else:
          print "ATLAS service has not found in the config or filtered out."
      elif options.action.lower() == 'restart-ranger':
        if is_ranger_available(config, service_filter):
          service_components_command(options, accessor, parser, config, RANGER_SERVICE_NAME, RANGER_ADMIN_COMPONENT_NAME, "RESTART", "Restart")
        else:
          print "RANGER service has not found in the config or filtered out."
      elif options.action.lower() == 'rolling-restart-ranger':
        if is_ranger_available(config, service_filter):
          rolling_restart(options, accessor, parser, config, RANGER_SERVICE_NAME, RANGER_ADMIN_COMPONENT_NAME, "Rolling Restart Ranger Admin Instances")
        else:
          print "RANGER service has not found in the config or filtered out."
      elif options.action.lower() == 'rolling-restart-atlas':
        if is_atlas_available(config, service_filter):
          rolling_restart(options, accessor, parser, config, ATLAS_SERVICE_NAME, ATLAS_SERVER_COMPONENT_NAME, "Rolling Restart Atlas Server Instances")
        else:
          print "ATLAS service has not found in the config or filtered out."
      elif options.action.lower() == 'rolling-restart-solr':
        rolling_restart(options, accessor, parser, config, SOLR_SERVICE_NAME, SOLR_COMPONENT_NAME, "Rolling Restart Infra Solr Instances")
      elif options.action.lower() == 'enable-solr-authorization':
        set_solr_authorization(options, accessor, parser, config, True)
      elif options.action.lower() == 'disable-solr-authorization':
        set_solr_authorization(options, accessor, parser, config, False)
      elif options.action.lower() == 'fix-solr5-kerberos-config':
        set_solr_authorization(options, accessor, parser, config, False, True)
      elif options.action.lower() == 'fix-solr7-kerberos-config':
        set_solr_authorization(options, accessor, parser, config, True, True)
      elif options.action.lower() == 'check-shards':
        check_shards(options, accessor, parser, config)
      elif options.action.lower() == 'check-backup-shards':
        check_shards(options, accessor, parser, config, backup_shards=True)
      elif options.action.lower() == 'check-docs':
        check_docs(options, accessor, parser, config)
      elif options.action.lower() == 'transport-old-data':
        check_docs(options, accessor, parser, config)
        transfer_old_data(options, accessor, parser, config)
        check_docs(options, accessor, parser, config)
      else:
        parser.print_help()
        print 'action option is invalid (available actions: delete-collections | backup | cleanup-znodes | backup-and-cleanup | migrate | restore |' \
              ' rolling-restart-solr | rolling-restart-ranger | rolling-restart-atlas | check-shards | check-backup-shards | check-docs | enable-solr-authorization |'\
              ' disable-solr-authorization | fix-solr5-kerberos-config | fix-solr7-kerberos-config | upgrade-solr-clients | upgrade-solr-instances | upgrade-logsearch-portal |' \
              ' upgrade-logfeeders | stop-logsearch | restart-solr |' \
              ' restart-logsearch | restart-ranger | restart-atlas | transport-old-data )'
        sys.exit(1)
      command_elapsed_time = time.time() - command_start_time
      time_to_print = time.strftime("%H:%M:%S", time.gmtime(command_elapsed_time))
      print 30 * "-"
      print "Command elapsed time: {0}".format(time_to_print)
      print 30 * "-"
      print "Migration helper command {0}FINISHED{1}".format(colors.OKGREEN, colors.ENDC)

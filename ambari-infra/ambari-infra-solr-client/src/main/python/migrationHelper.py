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

import logging
import os
import sys
import urllib2
import json
import base64
import optparse
import time
import traceback
import ConfigParser

from random import randrange
from subprocess import Popen, PIPE

HTTP_PROTOCOL = 'http'
HTTPS_PROTOCOL = 'https'

AMBARI_SUDO = "/var/lib/ambari-agent/ambari-sudo.sh"

SOLR_SERVICE_NAME = 'AMBARI_INFRA_SOLR'

SOLR_COMPONENT_NAME ='INFRA_SOLR'

CLUSTERS_URL = '/api/v1/clusters/{0}'

GET_HOSTS_COMPONENTS_URL = '/services/{0}/components/{1}?fields=host_components'

REQUESTS_API_URL = '/requests'
BATCH_REQUEST_API_URL = "/api/v1/clusters/{0}/request_schedules"

LIST_SOLR_COLLECTION_URL = '{0}/admin/collections?action=LIST&wt=json'
CREATE_SOLR_COLLECTION_URL = '{0}/admin/collections?action=CREATE&name={1}&collection.configName={2}&numShards={3}&replicationFactor={4}&maxShardsPerNode={5}&wt=json'
DELETE_SOLR_COLLECTION_URL = '{0}/admin/collections?action=DELETE&name={1}&wt=json'
RELOAD_SOLR_COLLECTION_URL = '{0}/admin/collections?action=RELOAD&name={1}&wt=json'

INFRA_SOLR_CLIENT_BASE_PATH = '/usr/lib/ambari-infra-solr-client/'
RANGER_NEW_SCHEMA = 'migrate/managed-schema'
SOLR_CLOUD_CLI_SCRIPT = 'solrCloudCli.sh'

logger = logging.getLogger()
handler = logging.StreamHandler()
formatter = logging.Formatter("%(asctime)s - %(message)s")
handler.setFormatter(formatter)
logger.addHandler(handler)

class colors:
  OKGREEN = '\033[92m'
  WARNING = '\033[93m'
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
  exit(1)

def create_solr_api_request_command(request_url, config, output=None):
  user='infra-solr'
  kerberos_enabled='false'
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

  use_infra_solr_user="sudo -u {0}".format(user)
  curl_prefix = "curl -k"
  if output is not None:
    curl_prefix+=" -o {0}".format(output)
  api_cmd = '{0} kinit -kt {1} {2} && {3} {4} --negotiate -u : "{5}"'.format(use_infra_solr_user, keytab, principal, use_infra_solr_user, curl_prefix, request_url) \
    if kerberos_enabled == 'true' else '{0} {1} "{2}"'.format(use_infra_solr_user, curl_prefix, request_url)
  logger.debug("Solr API command: {0}".format(api_cmd))
  return api_cmd

def create_infra_solr_client_command(options, config, command):
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
  set_java_home_= 'JAVA_HOME={0}'.format(java_home)
  set_infra_solr_cli_opts = ' INFRA_SOLR_CLI_OPTS="{0}"'.format(infra_solr_cli_opts) if infra_solr_cli_opts != '' else ''
  solr_cli_cmd = '{0} {1}{2} /usr/lib/ambari-infra-solr-client/solrCloudCli.sh --zookeeper-connect-string {3} {4}'\
    .format(AMBARI_SUDO, set_java_home_, set_infra_solr_cli_opts, zkConnectString, command)

  return solr_cli_cmd

def get_random_solr_url(solr_urls): # TODO: use Solr host filter
  splitted_solr_urls = solr_urls.split(',')
  random_index = randrange(0, len(splitted_solr_urls))
  result = splitted_solr_urls[random_index]
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

def fill_parameters(options, collection, index_location, shards=None):
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
  if options.solr_hdfs_path: # TODO get from ini file + had=ndle shared_fs value
    params['solr_hdfs_path'] = options.solr_hdfs_path
  if options.solr_keep_backup:
    params['solr_keep_backup'] = True
  if options.skip_generate_restore_host_cores:
    params['solr_skip_generate_restore_host_cores'] = True
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

def get_solr_hosts(options, accessor, cluster):
  if options.solr_hosts:
    component_hosts = options.solr_hosts.split(",")
  else:
    host_components_json = get_json(accessor, CLUSTERS_URL.format(cluster) + GET_HOSTS_COMPONENTS_URL.format(SOLR_SERVICE_NAME, SOLR_COMPONENT_NAME))
    component_hosts = get_component_hosts(host_components_json)
  return component_hosts

def restore(options, accessor, parser, config, collection, index_location, shards):
  """
  Send restore solr collection custom command request to ambari-server
  """
  cluster = config.get('ambari_server', 'cluster')

  component_hosts = get_solr_hosts(options, accessor, cluster)
  parameters = fill_parameters(options, collection, index_location, shards)

  cmd_request = create_command_request("RESTORE", parameters, component_hosts, cluster, 'Restore Solr Collection: ' + collection)
  return post_json(accessor, CLUSTERS_URL.format(cluster) + REQUESTS_API_URL, cmd_request)

def migrate(options, accessor, parser, config, collection, index_location):
  """
  Send migrate lucene index custom command request to ambari-server
  """
  cluster = config.get('ambari_server', 'cluster')

  component_hosts = get_solr_hosts(options, accessor, cluster)
  parameters = fill_parameters(options, collection, index_location)

  cmd_request = create_command_request("MIGRATE", parameters, component_hosts, cluster, 'Migrating Solr Collection: ' + collection)
  return post_json(accessor, CLUSTERS_URL.format(cluster) + REQUESTS_API_URL, cmd_request)

def backup(options, accessor, parser, config, collection, index_location):
  """
  Send backup solr collection custom command request to ambari-server
  """
  cluster = config.get('ambari_server', 'cluster')

  component_hosts = get_solr_hosts(options, accessor, cluster)
  parameters = fill_parameters(options, collection, index_location)

  cmd_request = create_command_request("BACKUP", parameters, component_hosts, cluster, 'Backup Solr Collection: ' + collection)
  return post_json(accessor, CLUSTERS_URL.format(cluster) + REQUESTS_API_URL, cmd_request)

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

def filter_collection(options, collections):
  if options.collection is not None:
    filtered_collections = []
    if options.collection in collections:
      filtered_collections.append(options.collection)
    return filtered_collections
  else:
    return collections

def get_solr_urls(config):
  solr_urls = None
  if config.has_section('infra_solr') and config.has_option('infra_solr', 'urls'):
    return config.get('infra_solr', 'urls')
  return solr_urls

def delete_collection(options, config, collection, solr_urls):
  request = DELETE_SOLR_COLLECTION_URL.format(get_random_solr_url(solr_urls), collection)
  logger.debug("Solr request: {0}".format(request))
  delete_collection_json_cmd=create_solr_api_request_command(request, config)
  process = Popen(delete_collection_json_cmd, stdout=PIPE, stderr=PIPE, shell=True)
  out, err = process.communicate()
  if process.returncode != 0:
    raise Exception("{0} command failed: {1}".format(delete_collection_json_cmd, str(err)))
  response=json.loads(str(out))
  if 'success' in response:
    print 'Deleting collection {0} was {1}SUCCESSFUL{2}'.format(collection, colors.OKGREEN, colors.ENDC)
    return collection
  else:
    raise Exception("DELETE collection ('{0}') failed. Response: {1}".format(collection, str(out)))

def list_collections(options, config, solr_urls):
  request = LIST_SOLR_COLLECTION_URL.format(get_random_solr_url(solr_urls))
  logger.debug("Solr request: {0}".format(request))
  list_collection_json_cmd=create_solr_api_request_command(request, config)
  process = Popen(list_collection_json_cmd, stdout=PIPE, stderr=PIPE, shell=True)
  out, err = process.communicate()
  if process.returncode != 0:
    raise Exception("{0} command failed: {1}".format(list_collection_json_cmd, str(err)))
  response=json.loads(str(out))
  if 'collections' in response:
    return response['collections']
  else:
    raise Exception("LIST collections failed ({0}). Response: {1}".format(request, str(out)))

def create_collection(options, config, solr_urls, collection, config_set, shards, replica, max_shards_per_node):
  request = CREATE_SOLR_COLLECTION_URL.format(get_random_solr_url(solr_urls), collection, config_set, shards, replica, max_shards_per_node)
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
  request = RELOAD_SOLR_COLLECTION_URL.format(get_random_solr_url(solr_urls), collection)
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

def delete_logsearch_collections(options, config, solr_urls, collections):
  if config.has_section('logsearch_collections'):
    if config.has_option('logsearch_collections', 'enabled') and config.get('logsearch_collections', 'enabled') == 'true':
      service_logs_collection = config.get('logsearch_collections', 'hadoop_logs_collection_name')
      audit_logs_collection = config.get('logsearch_collections', 'audit_logs_collection_name')
      history_collection = config.get('logsearch_collections', 'history_collection_name')
      if service_logs_collection in collections:
        retry(delete_collection, options, config, service_logs_collection, solr_urls, context='[Delete {0} collection]'.format(service_logs_collection))
      else:
        print 'Collection {0} does not exist or filtered out. Skipping delete operation.'.format(service_logs_collection)
      if audit_logs_collection in collections:
        retry(delete_collection, options, config, audit_logs_collection, solr_urls, context='[Delete {0} collection]'.format(audit_logs_collection))
      else:
        print 'Collection {0} does not exist or filtered out. Skipping delete operation.'.format(audit_logs_collection)
      if history_collection in collections:
        retry(delete_collection, options, config, history_collection, solr_urls, context='[Delete {0} collection]'.format(history_collection))
      else:
        print 'Collection {0} does not exist or filtered out. Skipping delete operation.'.format(history_collection)

def delete_atlas_collections(options, config, solr_urls, collections):
  if config.has_section('atlas_collections'):
    if config.has_option('atlas_collections', 'enabled') and config.get('atlas_collections', 'enabled') == 'true':
      fulltext_collection = config.get('atlas_collections', 'fulltext_index_name')
      edge_index_collection = config.get('atlas_collections', 'edge_index_name')
      vertex_index_collection = config.get('atlas_collections', 'vertex_index_name')
      if fulltext_collection in collections:
        retry(delete_collection, options, config, fulltext_collection, solr_urls, context='[Delete {0} collection]'.format(fulltext_collection))
      else:
        print 'Collection {0} does not exist or filtered out. Skipping delete operation.'.format(fulltext_collection)
      if edge_index_collection in collections:
        retry(delete_collection, options, config, edge_index_collection, solr_urls, context='[Delete {0} collection]'.format(edge_index_collection))
      else:
        print 'Collection {0} does not exist or filtered out. Skipping delete operation.'.format(edge_index_collection)
      if vertex_index_collection in collections:
        retry(delete_collection, options, config, vertex_index_collection, solr_urls, context='[Delete {0} collection]'.format(vertex_index_collection))
      else:
        print 'Collection {0} does not exist or filtered out. Skipping delete operation.'.format(vertex_index_collection)

def delete_ranger_collection(options, config, solr_urls, collections):
  if config.has_section('ranger_collection'):
    if config.has_option('ranger_collection', 'enabled') and config.get('ranger_collection', 'enabled') == 'true':
      ranger_collection_name = config.get('ranger_collection', 'ranger_collection_name')
      if ranger_collection_name in collections:
        retry(delete_collection, options, config, ranger_collection_name, solr_urls, context='[Delete {0} collection]'.format(ranger_collection_name))
      else:
        print 'Collection {0} does not exist or filtered out. Skipping delete operation'.format(ranger_collection_name)

def delete_collections(options, config, service_filter):
  solr_urls = get_solr_urls(config)
  collections=retry(list_collections, options, config, solr_urls, context="[List Solr Collections]")
  collections=filter_collection(options, collections)
  if 'RANGER' in service_filter:
    delete_ranger_collection(options, config, solr_urls, collections)
  if 'ATLAS' in service_filter:
    delete_atlas_collections(options, config, solr_urls, collections)
  if 'LOGSEARCH' in service_filter:
    delete_logsearch_collections(options, config, solr_urls, collections)

def upgrade_ranger_schema(options, config, service_filter):
  solr_znode='/infra-solr'
  if 'RANGER' in service_filter and config.has_option('ranger_collection', 'enabled') \
    and config.get('ranger_collection', 'enabled') == 'true':
    if config.has_section('infra_solr') and config.has_option('infra_solr', 'znode'):
      solr_znode=config.get('infra_solr', 'znode')
    ranger_config_set_name = config.get('ranger_collection', 'ranger_config_set_name')
    copy_znode(options, config, "{0}{1}".format(INFRA_SOLR_CLIENT_BASE_PATH, RANGER_NEW_SCHEMA),
               "{0}/configs/{1}/managed-schema".format(solr_znode, ranger_config_set_name), copy_from_local=True)

def backup_ranger_configs(options, config, service_filter):
  solr_znode='/infra-solr'
  if 'RANGER' in service_filter and config.has_option('ranger_collection', 'enabled') \
    and config.get('ranger_collection', 'enabled') == 'true':
    if config.has_section('infra_solr') and config.has_option('infra_solr', 'znode'):
      solr_znode=config.get('infra_solr', 'znode')
    ranger_config_set_name = config.get('ranger_collection', 'ranger_config_set_name')
    backup_ranger_config_set_name = config.get('ranger_collection', 'backup_ranger_config_set_name')
    copy_znode(options, config, "{0}/configs/{1}".format(solr_znode, ranger_config_set_name),
               "{0}/configs/{1}".format(solr_znode, backup_ranger_config_set_name))

def upgrade_ranger_solrconfig_xml(options, config, service_filter):
  solr_znode='/infra-solr'
  if 'RANGER' in service_filter and config.has_option('ranger_collection', 'enabled') \
    and config.get('ranger_collection', 'enabled') == 'true':
    if config.has_section('infra_solr') and config.has_option('infra_solr', 'znode'):
      solr_znode=config.get('infra_solr', 'znode')
    ranger_config_set_name = config.get('ranger_collection', 'ranger_config_set_name')
    backup_ranger_config_set_name = config.get('ranger_collection', 'backup_ranger_config_set_name')
    copy_znode(options, config, "{0}/configs/solrconfig.xml".format(solr_znode, ranger_config_set_name),
               "{0}/configs/solrconfig.xml".format(solr_znode, backup_ranger_config_set_name))

def delete_znodes(options, config, service_filter):
  solr_znode='/infra-solr'
  if 'LOGSEARCH' in service_filter and config.has_option('logsearch_collections', 'enabled') \
    and config.get('logsearch_collections', 'enabled') == 'true':
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

def do_restore_request(options, accessor, parser, config, collection, index_location, shards):
  sys.stdout.write("Sending restore collection request ('{0}') to Ambari to process (backup location: '{1}')..."
                   .format(collection, index_location))
  sys.stdout.flush()
  response = restore(options, accessor, parser, config, collection, index_location, shards)
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
  solr_urls = get_solr_urls(config)
  collections=retry(list_collections, options, config, solr_urls, context="[List Solr Collections]")
  collections=filter_collection(options, collections)
  if 'RANGER' in service_filter and config.has_section('ranger_collection') and config.has_option('ranger_collection', 'enabled') \
    and config.get('ranger_collection', 'enabled') == 'true':
    collection_name = config.get('ranger_collection', 'ranger_collection_name')
    if collection_name in collections:
      ranger_index_location=get_ranger_index_location(collection_name, config, options)
      do_backup_request(options, accessor, parser, config, collection_name, ranger_index_location)
    else:
      print 'Collection {0} does not exist or filtered out. Skipping backup operation.'.format(collection_name)
  if 'ATLAS' in service_filter and config.has_section('atlas_collections') \
    and config.has_option('atlas_collections', 'enabled') and config.get('atlas_collections', 'enabled') == 'true':
    fulltext_index_collection = config.get('atlas_collections', 'fulltext_index_name')
    if fulltext_index_collection in collections:
      fulltext_index_location = get_atlas_index_location(fulltext_index_collection, config, options)
      do_backup_request(options, accessor, parser, config, fulltext_index_collection, fulltext_index_location)
    else:
      print 'Collection {0} does not exist or filtered out. Skipping backup operation.'.format(fulltext_index_collection)
    vertex_index_collection = config.get('atlas_collections', 'vertex_index_name')
    if vertex_index_collection in collections:
      vertex_index_location = get_atlas_index_location(vertex_index_collection, config, options)
      do_backup_request(options, accessor, parser, config, vertex_index_collection, vertex_index_location)
    else:
      print 'Collection {0} does not exist or filtered out. Skipping backup operation.'.format(vertex_index_collection)
    edge_index_collection = config.get('atlas_collections', 'edge_index_name')
    if edge_index_collection in collections:
      edge_index_location = get_atlas_index_location(vertex_index_collection, config, options)
      do_backup_request(options, accessor, parser, config, edge_index_collection, edge_index_location)
    else:
      print 'Collection {0} does not exist or filtered out. Skipping backup operation.'.format(edge_index_collection)

def migrate_snapshots(options, accessor, parser, config, service_filter):
  if 'RANGER' in service_filter and config.has_section('ranger_collection') and config.has_option('ranger_collection', 'enabled') \
    and config.get('ranger_collection', 'enabled') == 'true':
    collection_name = config.get('ranger_collection', 'ranger_collection_name')
    if options.collection is None or options.collection == collection_name:
      ranger_index_location=get_ranger_index_location(collection_name, config, options)
      do_migrate_request(options, accessor, parser, config, collection_name, ranger_index_location)
    else:
      print "Collection ('{0}') backup index has filtered out. Skipping migrate operation.".format(collection_name)
  if 'ATLAS' in service_filter and config.has_section('atlas_collections') \
    and config.has_option('atlas_collections', 'enabled') and config.get('atlas_collections', 'enabled') == 'true':
    fulltext_index_collection = config.get('atlas_collections', 'fulltext_index_name')
    if options.collection is None or options.collection == fulltext_index_collection:
      fulltext_index_location=get_atlas_index_location(fulltext_index_collection, config, options)
      do_migrate_request(options, accessor, parser, config, fulltext_index_collection, fulltext_index_location)
    else:
      print "Collection ('{0}') backup index has filtered out. Skipping migrate operation.".format(fulltext_index_collection)
    vertex_index_collection = config.get('atlas_collections', 'vertex_index_name')
    if options.collection is None or options.collection == vertex_index_collection:
      vertex_index_location=get_atlas_index_location(vertex_index_collection, config, options)
      do_migrate_request(options, accessor, parser, config, fulltext_index_collection, vertex_index_location)
    else:
      print "Collection ('{0}') backup index has filtered out. Skipping migrate operation.".format(vertex_index_collection)
    edge_index_collection = config.get('atlas_collections', 'edge_index_name')
    if options.collection is None or options.collection == edge_index_collection:
      edge_index_location=get_atlas_index_location(edge_index_collection, config, options)
      do_migrate_request(options, accessor, parser, config, edge_index_collection, edge_index_location)
    else:
      print "Collection ('{0}') backup index has filtered out. Skipping migrate operation.".format(edge_index_collection)

def create_backup_collections(options, accessor, parser, config, service_filter):
  solr_urls = get_solr_urls(config)
  collections=retry(list_collections, options, config, solr_urls, context="[List Solr Collections]")
  replica_number = "1" # hard coded
  if 'RANGER' in service_filter and config.has_section('ranger_collection') and config.has_option('ranger_collection', 'enabled') \
    and config.get('ranger_collection', 'enabled') == 'true':
    backup_ranger_collection = config.get('ranger_collection', 'backup_ranger_collection_name')
    if backup_ranger_collection not in collections:
      if options.collection is not None and options.collection != backup_ranger_collection:
        print "Collection {0} has filtered out. Skipping create operation.".format(backup_ranger_collection)
      else:
        backup_ranger_config_set = config.get('ranger_collection', 'backup_ranger_config_set_name')
        backup_ranger_shards = config.get('ranger_collection', 'ranger_collection_shards')
        backup_ranger_max_shards = config.get('ranger_collection', 'ranger_collection_max_shards_per_node')
        retry(create_collection, options, config, solr_urls, backup_ranger_collection, backup_ranger_config_set,
                        backup_ranger_shards, replica_number, backup_ranger_max_shards, context="[Create Solr Collections]")
    else:
      print "Collection {0} has already exist. Skipping create operation.".format(backup_ranger_collection)
  if 'ATLAS' in service_filter and config.has_section('atlas_collections') \
    and config.has_option('atlas_collections', 'enabled') and config.get('atlas_collections', 'enabled') == 'true':
    backup_atlas_config_set = config.get('atlas_collections', 'config_set')
    backup_fulltext_index_name = config.get('atlas_collections', 'backup_fulltext_index_name')
    if backup_fulltext_index_name not in collections:
      if options.collection is not None and options.collection != backup_fulltext_index_name:
        print "Collection {0} has filtered out. Skipping create operation.".format(backup_fulltext_index_name)
      else:
        backup_fulltext_index_shards = config.get('atlas_collections', 'fulltext_index_shards')
        backup_fulltext_index_max_shards = config.get('atlas_collections', 'fulltext_index_max_shards_per_node')
        retry(create_collection, options, config, solr_urls, backup_fulltext_index_name, backup_atlas_config_set,
                        backup_fulltext_index_shards, replica_number, backup_fulltext_index_max_shards, context="[Create Solr Collections]")
    else:
      print "Collection {0} has already exist. Skipping create operation.".format(backup_fulltext_index_name)
    backup_edge_index_name = config.get('atlas_collections', 'backup_edge_index_name')
    if backup_edge_index_name not in collections:
      if options.collection is not None and options.collection != backup_edge_index_name:
        print "Collection {0} has filtered out. Skipping create operation.".format(backup_edge_index_name)
      else:
        backup_edge_index_shards = config.get('atlas_collections', 'edge_index_shards')
        backup_edge_index_max_shards = config.get('atlas_collections', 'edge_index_max_shards_per_node')
        retry(create_collection, options, config, solr_urls, backup_edge_index_name, backup_atlas_config_set,
                        backup_edge_index_shards, replica_number, backup_edge_index_max_shards, context="[Create Solr Collections]")
    else:
      print "Collection {0} has already exist. Skipping create operation.".format(backup_edge_index_name)
    backup_vertex_index_name = config.get('atlas_collections', 'backup_vertex_index_name')
    if backup_vertex_index_name not in collections:
      if options.collection is not None and options.collection != backup_vertex_index_name:
        print "Collection {0} has filtered out. Skipping create operation.".format(backup_vertex_index_name)
      else:
        backup_vertex_index_shards = config.get('atlas_collections', 'vertex_index_shards')
        backup_vertex_index_max_shards = config.get('atlas_collections', 'vertex_index_max_shards_per_node')
        retry(create_collection, options, config, solr_urls, backup_vertex_index_name, backup_atlas_config_set,
                        backup_vertex_index_shards, replica_number, backup_vertex_index_max_shards, context="[Create Solr Collections]")
    else:
      print "Collection {0} has already exist. Skipping create operation.".format(backup_fulltext_index_name)

def restore_collections(options, accessor, parser, config, service_filter):
  solr_urls = get_solr_urls(config)
  collections=retry(list_collections, options, config, solr_urls, context="[List Solr Collections]")
  collections=filter_collection(options, collections)
  if 'RANGER' in service_filter and config.has_section('ranger_collection') and config.has_option('ranger_collection', 'enabled') \
    and config.get('ranger_collection', 'enabled') == 'true':
    collection_name = config.get('ranger_collection', 'ranger_collection_name')
    backup_ranger_collection = config.get('ranger_collection', 'backup_ranger_collection_name')
    if backup_ranger_collection in collections:
      backup_ranger_shards = config.get('ranger_collection', 'ranger_collection_shards')
      ranger_index_location=get_ranger_index_location(collection_name, config, options)
      do_restore_request(options, accessor, parser, config, backup_ranger_collection, ranger_index_location, backup_ranger_shards)
    else:
      print "Collection ('{0}') does not exist or filtered out. Skipping restore operation.".format(backup_ranger_collection)

  if 'ATLAS' in service_filter and config.has_section('atlas_collections') \
    and config.has_option('atlas_collections', 'enabled') and config.get('atlas_collections', 'enabled') == 'true':

    fulltext_index_collection = config.get('atlas_collections', 'fulltext_index_name')
    backup_fulltext_index_name = config.get('atlas_collections', 'backup_fulltext_index_name')
    if backup_fulltext_index_name in collections:
      backup_fulltext_index_shards = config.get('atlas_collections', 'fulltext_index_shards')
      fulltext_index_location=get_atlas_index_location(fulltext_index_collection, config, options)
      do_restore_request(options, accessor, parser, config, backup_fulltext_index_name, fulltext_index_location, backup_fulltext_index_shards)
    else:
      print "Collection ('{0}') does not exist or filtered out. Skipping restore operation.".format(fulltext_index_collection)

    edge_index_collection = config.get('atlas_collections', 'edge_index_name')
    backup_edge_index_name = config.get('atlas_collections', 'backup_edge_index_name')
    if backup_edge_index_name in collections:
      backup_edge_index_shards = config.get('atlas_collections', 'edge_index_shards')
      edge_index_location=get_atlas_index_location(edge_index_collection, config, options)
      do_restore_request(options, accessor, parser, config, backup_edge_index_name, edge_index_location, backup_edge_index_shards)
    else:
      print "Collection ('{0}') does not exist or filtered out. Skipping restore operation.".format(edge_index_collection)

    vertex_index_collection = config.get('atlas_collections', 'vertex_index_name')
    backup_vertex_index_name = config.get('atlas_collections', 'backup_vertex_index_name')
    if backup_vertex_index_name in collections:
      backup_vertex_index_shards = config.get('atlas_collections', 'vertex_index_shards')
      vertex_index_location=get_atlas_index_location(vertex_index_collection, config, options)
      do_restore_request(options, accessor, parser, config, backup_vertex_index_name, vertex_index_location, backup_vertex_index_shards)
    else:
      print "Collection ('{0}') does not exist or filtered out. Skipping restore operation.".format(vertex_index_collection)

def reload_collections(options, accessor, parser, config, service_filter):
  solr_urls = get_solr_urls(config)
  collections=retry(list_collections, options, config, solr_urls, context="[List Solr Collections]")
  collections=filter_collection(options, collections)
  if 'RANGER' in service_filter and config.has_section('ranger_collection') and config.has_option('ranger_collection', 'enabled') \
    and config.get('ranger_collection', 'enabled') == 'true':
    backup_ranger_collection = config.get('ranger_collection', 'backup_ranger_collection_name')
    if backup_ranger_collection in collections:
      retry(reload_collection, options, config, solr_urls, backup_ranger_collection, context="[Reload Solr Collections]")
    else:
      print "Collection ('{0}') does not exist or filtered out. Skipping reload operation.".format(backup_ranger_collection)
  if 'ATLAS' in service_filter and config.has_section('atlas_collections') \
    and config.has_option('atlas_collections', 'enabled') and config.get('atlas_collections', 'enabled') == 'true':
    backup_fulltext_index_name = config.get('atlas_collections', 'backup_fulltext_index_name')
    if backup_fulltext_index_name in collections:
      retry(reload_collection, options, config, solr_urls, backup_fulltext_index_name, context="[Reload Solr Collections]")
    else:
      print "Collection ('{0}') does not exist or filtered out. Skipping reload operation.".format(backup_fulltext_index_name)
    backup_edge_index_name = config.get('atlas_collections', 'backup_edge_index_name')
    if backup_edge_index_name in collections:
      retry(reload_collection, options, config, solr_urls, backup_edge_index_name, context="[Reload Solr Collections]")
    else:
      print "Collection ('{0}') does not exist or filtered out. Skipping reload operation.".format(backup_edge_index_name)
    backup_vertex_index_name = config.get('atlas_collections', 'backup_vertex_index_name')
    if backup_vertex_index_name in collections:
      retry(reload_collection, options, config, solr_urls, backup_vertex_index_name, context="[Reload Solr Collections]")
    else:
      print "Collection ('{0}') does not exist or filtered out. Skipping reload operation.".format(backup_fulltext_index_name)

def validate_ini_file(options, parser):
  if options.ini_file is None:
    parser.print_help()
    print 'ini-file option is missing'
    sys.exit(1)
  elif not os.path.isfile(options.ini_file):
    parser.print_help()
    print 'ini file ({0}) does not exist'.format(options.ini_file)
    sys.exit(1)

def rolling_restart_solr(options, accessor, parser, config):
  cluster = config.get('ambari_server', 'cluster')
  component_hosts = get_solr_hosts(options, accessor, cluster)
  interval_secs = options.batch_interval
  fault_tolerance = options.batch_fault_tolerance
  request_body = create_batch_command("RESTART", component_hosts, cluster, "AMBARI_INFRA_SOLR", "INFRA_SOLR", interval_secs, fault_tolerance, "Rolling restart Infra Solr Instances")
  post_json(accessor, BATCH_REQUEST_API_URL.format(cluster), request_body)
  print "Rolling Restart Infra Solr Instances request sent. (check Ambari UI about the requests)"

if __name__=="__main__":
  parser = optparse.OptionParser("usage: %prog [options]")

  parser.add_option("-a", "--action", dest="action", type="string", help="delete-collections | backup | cleanup-znodes | backup-and-cleanup | migrate | restore | rolling-restart-solr")
  parser.add_option("-i", "--ini-file", dest="ini_file", type="string", help="Config ini file to parse (required)")
  parser.add_option("-f", "--force", dest="force", default=False, action="store_true", help="force index upgrade even if it's the right version")
  parser.add_option("-v", "--verbose", dest="verbose", action="store_true", help="use for verbose logging")
  parser.add_option("-s", "--service-filter", dest="service_filter", default=None, type="string", help="run commands only selected services (comma separated: LOGSEARCH,ATLAS,RANGER)")
  parser.add_option("-c", "--collection", dest="collection", default=None, type="string", help="selected collection to run an operation")
  parser.add_option("--async", dest="async", action="store_true", default=False, help="async Ambari operations (backup | restore | migrate)")
  parser.add_option("--index-location", dest="index_location", type="string", help="location of the index backups. add ranger/atlas prefix after the path. required only if no backup path in the ini file")
  parser.add_option("--atlas-index-location", dest="atlas_index_location", type="string", help="location of the index backups (for atlas). required only if no backup path in the ini file")
  parser.add_option("--ranger-index-location", dest="ranger_index_location", type="string", help="location of the index backups (for ranger). required only if no backup path in the ini file")

  parser.add_option("--version", dest="index_version", type="string", default="6.6.2", help="lucene index version for migration (6.6.2 or 7.3.1)")
  parser.add_option("--request-tries", dest="request_tries", type="int", help="number of tries for BACKUP/RESTORE status api calls in the request")
  parser.add_option("--request-time-interval", dest="request_time_interval", type="int", help="time interval between BACKUP/RESTORE status api calls in the request")
  parser.add_option("--request-async", dest="request_async", action="store_true", default=False, help="skip BACKUP/RESTORE status api calls from the command")
  parser.add_option("--shared-fs", dest="shared_fs", action="store_true", default=False, help="shared fs for storing backup (will create index location to <path><hostname>)")
  parser.add_option("--solr-hosts", dest="solr_hosts", type="string", help="comma separated list of solr hosts")
  parser.add_option("--disable-solr-host-check", dest="disable_solr_host_check", action="store_true", default=False, help="Disable to check solr hosts are good for the collection backups")
  parser.add_option("--core-filter", dest="core_filter", default=None, type="string", help="core filter for replica folders")
  parser.add_option("--skip-cores", dest="skip_cores", default=None, type="string", help="specific cores to skip (comma separated)")
  parser.add_option("--skip-generate-restore-host-cores", dest="skip_generate_restore_host_cores", default=False, action="store_true", help="Skip the generation of restore_host_cores.json, just read the file itself, can be useful if command failed at some point.")
  parser.add_option("--solr-hdfs-path", dest="solr_hdfs_path", type="string", default=None, help="Base path of Solr (where collections are located) if HDFS is used (like /user/infra-solr)")
  parser.add_option("--solr-keep-backup", dest="solr_keep_backup", default=False, action="store_true", help="If it is turned on, Snapshot Solr data will not be deleted from the filesystem during restore.")
  parser.add_option("--batch-interval", dest="batch_interval", type="int", default=60 ,help="batch time interval (seconds) between requests (for restarting INFRA SOLR, default: 60)")
  parser.add_option("--batch-fault-tolerance", dest="batch_fault_tolerance", type="int", default=0 ,help="fault tolerance of tasks for batch request (for restarting INFRA SOLR, default: 0)")
  (options, args) = parser.parse_args()

  set_log_level(options.verbose)

  validate_ini_file(options, parser)

  config = ConfigParser.RawConfigParser()
  config.read(options.ini_file)

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
        reload_collections(options, accessor, parser, config, service_filter)
      elif options.action.lower() == 'migrate':
        migrate_snapshots(options, accessor, parser, config, service_filter)
      elif options.action.lower() == 'rolling-restart-solr':
        rolling_restart_solr(options, accessor, parser, config)
      else:
        parser.print_help()
        print 'action option is invalid (available actions: delete-collections | backup | cleanup-znodes | backup-and-cleanup | migrate | restore | rolling-restart-solr)'
        sys.exit(1)

      print "Migration helper command {0}FINISHED{1}".format(colors.OKGREEN, colors.ENDC)
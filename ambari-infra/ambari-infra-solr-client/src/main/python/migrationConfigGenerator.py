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

import os
import socket
import signal
import sys
import time
import traceback
import urllib2, ssl
import logging
import json
import base64
import optparse
import ConfigParser
from subprocess import Popen, PIPE
from random import randrange

SOLR_SERVICE_NAME = 'AMBARI_INFRA_SOLR'
SOLR_COMPONENT_NAME ='INFRA_SOLR'

ATLAS_SERVICE_NAME = 'ATLAS'

RANGER_SERVICE_NAME = 'RANGER'
RANGER_COMPONENT_NAME = 'RANGER_ADMIN'

ZOOKEEPER_SERVICE_NAME = 'ZOOKEEPER'
ZOOKEEPER_COMPONENT_NAME ='ZOOKEEPER_SERVER'

CLUSTERS_URL = '/api/v1/clusters/{0}'
BLUEPRINT_CONFIG_URL = '?format=blueprint'
GET_SERVICES_URL = '/services/{0}'
GET_HOSTS_COMPONENTS_URL = '/services/{0}/components/{1}?fields=host_components'

GET_STATE_JSON_URL = '{0}/admin/zookeeper?wt=json&detail=true&path=%2Fclusterstate.json&view=graph'

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

def create_solr_api_request_command(request_url, user='infra-solr', kerberos_enabled='false', keytab=None, principal=None, output=None):
  use_infra_solr_user="sudo -u {0}".format(user)
  curl_prefix = "curl -k"
  if output is not None:
    curl_prefix+=" -o {0}".format(output)
  api_cmd = '{0} kinit -kt {1} {2} && {3} {4} --negotiate -u : "{5}"'.format(use_infra_solr_user, keytab, principal, use_infra_solr_user, curl_prefix, request_url) \
    if kerberos_enabled == 'true' else '{0} {1} "{2}"'.format(use_infra_solr_user, curl_prefix, request_url)
  return api_cmd

def get_random_solr_url(solr_urls):
  splitted_solr_urls = solr_urls.split(',')
  random_index = randrange(0, len(splitted_solr_urls))
  result = splitted_solr_urls[random_index]
  logger.debug("Use {0} for request.".format(result))
  return result

def retry(func, *args, **kwargs):
  retry_count = kwargs.pop("count", 10)
  delay = kwargs.pop("delay", 5)
  context = kwargs.pop("context", "")
  for r in range(retry_count):
    try:
      result = func(*args, **kwargs)
      if result is not None: return result
    except Exception as e:
      logger.debug("Error occurred during {0} operation: {1}".format(context, str(traceback.format_exc())))
    logger.info("{0}: waiting for {1} seconds before retyring again (retry count: {2})".format(context, delay, r+1))
    time.sleep(delay)

def get_shard_numbers_per_collections(state_json_data):
  collection_shard_map={}
  for key,val in state_json_data.iteritems():
    if 'shards' in val:
      shard_count=len(val['shards'])
      collection_shard_map[key]=shard_count
  return collection_shard_map

def get_max_shards_for_collections(state_json_data):
  collection_max_shard_map={}
  for key,val in state_json_data.iteritems():
    if 'maxShardsPerNode' in val:
      collection_max_shard_map[key]=val['maxShardsPerNode']
  return collection_max_shard_map

def get_state_json_map(solr_urls, user='infra-solr', kerberos_enabled='false', keytab=None, principal=None):
  state_json_data={}
  request = GET_STATE_JSON_URL.format(get_random_solr_url(solr_urls))
  get_state_json_cmd=create_solr_api_request_command(request, user, kerberos_enabled, keytab, principal)
  process = Popen(get_state_json_cmd, stdout=PIPE, stderr=PIPE, shell=True)
  out, err = process.communicate()
  if process.returncode != 0:
    logger.error(str(err))
  response=json.loads(str(out))
  if 'znode' in response:
    if 'data' in response['znode']:
      state_json_data=json.loads(response['znode']['data'])
  return state_json_data

def read_json(json_file):
  with open(json_file) as data_file:
    data = json.load(data_file)
  return data

def get_json(accessor, url):
  response = accessor(url, 'GET')
  logger.debug('GET ' + url + ' response: ')
  logger.debug('----------------------------')
  logger.debug(response)
  json_resp = json.loads(response)
  return json_resp

def post_json(accessor, url, request_body):
  response = accessor(url, 'POST', json.dumps(request_body))
  logger.debug('POST ' + url + ' response: ')
  logger.debug('----------------------------')
  logger.debug(response)
  json_resp = json.loads(response)
  return json_resp

def get_component_hosts(host_components_json):
  hosts = []
  if "host_components" in host_components_json and len(host_components_json['host_components']) > 0:
    for host_component in host_components_json['host_components']:
      if 'HostRoles' in host_component:
        hosts.append(host_component['HostRoles']['host_name'])
  return hosts

def get_solr_hosts(options, accessor):
  host_components_json = get_json(accessor, CLUSTERS_URL.format(options.cluster) + GET_HOSTS_COMPONENTS_URL.format(SOLR_SERVICE_NAME, SOLR_COMPONENT_NAME))
  component_hosts = get_component_hosts(host_components_json)
  return component_hosts

def get_zookeeper_server_hosts(options, accessor):
  host_components_json = get_json(accessor, CLUSTERS_URL.format(options.cluster) + GET_HOSTS_COMPONENTS_URL.format(ZOOKEEPER_SERVICE_NAME, ZOOKEEPER_COMPONENT_NAME))
  component_hosts = get_component_hosts(host_components_json)
  return component_hosts

def get_cluster_configs(blueprint):
  result = []
  if 'configurations' in blueprint:
    result = blueprint['configurations']
  return result

def get_config_props(cluster_config, config_type):
  props={}
  for config in cluster_config:
    if config_type in config and 'properties' in config[config_type]:
      props=config[config_type]['properties']
  return props

def is_security_enabled(cluster_config):
  result = 'false'
  cluster_env_props=get_config_props(cluster_config, 'cluster-env')
  if cluster_env_props and 'security_enabled' in cluster_env_props and cluster_env_props['security_enabled'] == 'true':
    result = 'true'
  return result

def set_log_level(verbose):
  if verbose:
    logger.setLevel(logging.DEBUG)
  else:
    logger.setLevel(logging.INFO)

def get_solr_env_props(cluster_config):
  return get_config_props(cluster_config, 'infra-solr-env')

def get_solr_urls(cluster_config, solr_hosts, solr_protocol):
  infra_solr_env_props = get_solr_env_props(cluster_config)

  solr_port = infra_solr_env_props['infra_solr_port'] if 'infra_solr_port' in infra_solr_env_props else '8886'
  solr_addr_list = []
  for solr_host in solr_hosts:
    solr_addr = "{0}://{1}:{2}/solr".format(solr_protocol, solr_host, solr_port)
    solr_addr_list.append(solr_addr)

  return ','.join(solr_addr_list)

def get_solr_protocol(cluster_config):
  infra_solr_env_props = get_solr_env_props(cluster_config)
  return 'https' if 'infra_solr_ssl_enabled' in infra_solr_env_props and infra_solr_env_props['infra_solr_ssl_enabled'] == 'true' else 'http'

def get_zookeeper_connection_string(cluster_config, zookeeper_hosts):
  client_port = "2181"
  zoo_cfg_props=get_config_props(cluster_config, 'zoo.cfg')
  if zoo_cfg_props and 'clientPort' in zoo_cfg_props:
    client_port = zoo_cfg_props['clientPort']

  zookeeper_addr_list = []
  for zookeeper_host in zookeeper_hosts:
    zookeeper_addr = zookeeper_host + ":" + client_port
    zookeeper_addr_list.append(zookeeper_addr)

  return ','.join(zookeeper_addr_list)

def get_solr_znode(cluster_config):
  infra_solr_env_props = get_solr_env_props(cluster_config)
  return infra_solr_env_props['infra_solr_znode'] if 'infra_solr_znode' in infra_solr_env_props else '/infra-solr'

def get_installed_components(blueprint):
  components = []
  if 'host_groups' in blueprint:
    for host_group in blueprint['host_groups']:
      if 'components' in host_group:
        for component in host_group['components']:
          if 'name' in component:
            if component['name'] not in components:
              components.append(component['name'])
  return components

def generate_ambari_solr_migration_ini_file(options, accessor, protocol):

  print "Start generating config file: {0} ...".format(options.ini_file)

  config = ConfigParser.RawConfigParser()

  config.add_section('ambari_server')
  config.set('ambari_server', 'host', options.host)
  config.set('ambari_server', 'port', options.port)
  config.set('ambari_server', 'cluster', options.cluster)
  config.set('ambari_server', 'protocol', protocol)
  config.set('ambari_server', 'username', options.username)
  config.set('ambari_server', 'password', options.password)

  print "Get Ambari cluster details ..."
  blueprint = get_json(accessor, CLUSTERS_URL.format(options.cluster) + BLUEPRINT_CONFIG_URL)
  installed_components = get_installed_components(blueprint)

  print "Set JAVA_HOME: {0}".format(options.java_home)
  host = socket.getfqdn()

  cluster_config = get_cluster_configs(blueprint)
  solr_hosts = get_solr_hosts(options, accessor)

  if solr_hosts and host not in solr_hosts:
    print "{0}WARNING{1}: Host '{2}' is not found in Infra Solr hosts ({3}). Migration commands won't work from here."\
      .format(colors.WARNING, colors.ENDC, host, ','.join(solr_hosts))

  zookeeper_hosts = get_zookeeper_server_hosts(options, accessor)

  security_enabled = is_security_enabled(cluster_config)
  zk_connect_string = get_zookeeper_connection_string(cluster_config, zookeeper_hosts)
  if zk_connect_string:
    print "Service detected: " + colors.OKGREEN + "ZOOKEEPER" + colors.ENDC
    print "Zookeeper connection string: {0}".format(str(zk_connect_string))
  solr_protocol = get_solr_protocol(cluster_config)
  solr_urls = get_solr_urls(cluster_config, solr_hosts, solr_protocol)
  if solr_urls:
    print "Service detected: " + colors.OKGREEN + "AMBARI_INFRA_SOLR" + colors.ENDC
  solr_znode = get_solr_znode(cluster_config)
  if solr_znode:
    print "Infra Solr znode: {0}".format(solr_znode)
  infra_solr_env_props = get_config_props(cluster_config, 'infra-solr-env')

  infra_solr_user = infra_solr_env_props['infra_solr_user'] if 'infra_solr_user' in infra_solr_env_props else 'infra-solr'
  infra_solr_kerberos_keytab = infra_solr_env_props['infra_solr_kerberos_keytab'] if 'infra_solr_kerberos_keytab' in infra_solr_env_props else '/etc/security/keytabs/ambari-infra-solr.service.keytab'
  infra_solr_kerberos_principal_config = infra_solr_env_props['infra_solr_kerberos_principal'] if 'infra_solr_kerberos_principal' in infra_solr_env_props else 'infra-solr'
  infra_solr_kerberos_principal = "infra-solr/" + host
  if '/' in infra_solr_kerberos_principal_config:
    infra_solr_kerberos_principal = infra_solr_kerberos_principal_config.replace('_HOST',host)
  else:
    infra_solr_kerberos_principal = infra_solr_kerberos_principal_config + "/" + host
  infra_solr_port = infra_solr_env_props['infra_solr_port'] if 'infra_solr_port' in infra_solr_env_props else '8886'

  config.add_section('local')
  config.set('local', 'java_home', options.java_home)
  config.set('local', 'hostname', host)
  if options.shared_drive:
    config.set('local', 'shared_drive', 'true')
  else:
    config.set('local', 'shared_drive', 'false')

  config.add_section('cluster')
  config.set('cluster', 'kerberos_enabled', security_enabled)

  config.add_section('infra_solr')
  config.set('infra_solr', 'protocol', solr_protocol)
  config.set('infra_solr', 'hosts', ','.join(solr_hosts))
  config.set('infra_solr', 'zk_connect_string', zk_connect_string)
  config.set('infra_solr', 'znode', solr_znode)
  config.set('infra_solr', 'user', infra_solr_user)
  config.set('infra_solr', 'port', infra_solr_port)
  if security_enabled == 'true':
    config.set('infra_solr', 'keytab', infra_solr_kerberos_keytab)
    config.set('infra_solr', 'principal', infra_solr_kerberos_principal)
    zookeeper_env_props = get_config_props(cluster_config, 'zookeeper-env')
    zookeeper_principal_name = zookeeper_env_props['zookeeper_principal_name'] if 'zookeeper_principal_name' in zookeeper_env_props else "zookeeper/_HOST@EXAMPLE.COM"
    zk_principal_user = zookeeper_principal_name.split("/")[0]
    default_zk_quorum = "{zookeeper_quorum}"
    external_zk_connection_string = infra_solr_env_props['infra_solr_zookeeper_quorum'] if 'infra_solr_zookeeper_quorum' in infra_solr_env_props else default_zk_quorum
    if default_zk_quorum != external_zk_connection_string:
      print "Found external zk connection string: {0}".format(external_zk_connection_string)
      config.set('infra_solr', 'external_zk_connect_string', external_zk_connection_string)
    config.set('infra_solr', 'zk_principal_user', zk_principal_user)

  state_json_map = retry(get_state_json_map, solr_urls, infra_solr_user, security_enabled, infra_solr_kerberos_keytab, infra_solr_kerberos_principal, count=options.retry, delay=options.delay, context="Get clusterstate.json")
  coll_shard_map=get_shard_numbers_per_collections(state_json_map)
  max_shards_map=get_max_shards_for_collections(state_json_map)

  config.add_section('ranger_collection')
  if "RANGER_ADMIN" in installed_components and not options.skip_ranger:
    print "Service detected: " + colors.OKGREEN + "RANGER" + colors.ENDC
    ranger_env_props = get_config_props(cluster_config, 'ranger-env')
    if "is_solrCloud_enabled" in ranger_env_props and ranger_env_props['is_solrCloud_enabled'] == 'true':
      if "is_external_solrCloud_enabled" in ranger_env_props and ranger_env_props['is_external_solrCloud_enabled'] == 'true' and not options.force_ranger:
        config.set('ranger_collection', 'enabled', 'false')
      else:
        config.set('ranger_collection', 'enabled', 'true')
        ranger_config_set = ranger_env_props['ranger_solr_config_set'] if ranger_env_props and 'ranger_solr_config_set' in ranger_env_props else 'ranger_audits'
        ranger_collection_name = ranger_env_props['ranger_solr_collection_name'] if ranger_env_props and 'ranger_solr_collection_name' in ranger_env_props else 'ranger_audits'
        config.set('ranger_collection', 'ranger_config_set_name', ranger_config_set)
        config.set('ranger_collection', 'ranger_collection_name', ranger_collection_name)
        if ranger_collection_name in coll_shard_map:
          config.set('ranger_collection', 'ranger_collection_shards', coll_shard_map[ranger_collection_name])
        if ranger_collection_name in max_shards_map:
           config.set('ranger_collection', 'ranger_collection_max_shards_per_node', max_shards_map[ranger_collection_name])
        config.set('ranger_collection', 'backup_ranger_config_set_name', 'old_ranger_audits')
        config.set('ranger_collection', 'backup_ranger_collection_name', 'old_ranger_audits')
        print 'Ranger Solr collection: ' + ranger_collection_name
        ranger_backup_path = None
        if options.backup_base_path:
          ranger_backup_path = os.path.join(options.backup_base_path, "ranger")
        elif options.backup_ranger_base_path:
          ranger_backup_path = options.backup_ranger_base_path
        if ranger_backup_path is not None:
          config.set('ranger_collection', 'backup_path', ranger_backup_path)
          print 'Ranger backup path: ' + ranger_backup_path
        if options.ranger_hdfs_base_path:
          config.set('ranger_collection', 'hdfs_base_path', options.ranger_hdfs_base_path)
        elif options.hdfs_base_path:
          config.set('ranger_collection', 'hdfs_base_path', options.hdfs_base_path)
    else:
      config.set('ranger_collection', 'enabled', 'false')
  else:
    config.set('ranger_collection', 'enabled', 'false')

  config.add_section('atlas_collections')
  if "ATLAS_SERVER" in installed_components and not options.skip_atlas:
    print "Service detected: " + colors.OKGREEN + "ATLAS" + colors.ENDC
    config.set('atlas_collections', 'enabled', 'true')
    config.set('atlas_collections', 'config_set', 'atlas_configs')
    config.set('atlas_collections', 'fulltext_index_name', 'fulltext_index')
    config.set('atlas_collections', 'backup_fulltext_index_name', 'old_fulltext_index')
    if 'fulltext_index' in coll_shard_map:
      config.set('atlas_collections', 'fulltext_index_shards', coll_shard_map['fulltext_index'])
    if 'fulltext_index' in max_shards_map:
      config.set('atlas_collections', 'fulltext_index_max_shards_per_node', max_shards_map['fulltext_index'])
    config.set('atlas_collections', 'edge_index_name', 'edge_index')
    config.set('atlas_collections', 'backup_edge_index_name', 'old_edge_index')
    if 'edge_index' in coll_shard_map:
      config.set('atlas_collections', 'edge_index_shards', coll_shard_map['edge_index'])
    if 'edge_index' in max_shards_map:
      config.set('atlas_collections', 'edge_index_max_shards_per_node', max_shards_map['edge_index'])
    config.set('atlas_collections', 'vertex_index_name', 'vertex_index')
    config.set('atlas_collections', 'backup_vertex_index_name', 'old_vertex_index')
    if 'vertex_index' in coll_shard_map:
      config.set('atlas_collections', 'vertex_index_shards', coll_shard_map['vertex_index'])
    if 'vertex_index' in max_shards_map:
      config.set('atlas_collections', 'vertex_index_max_shards_per_node', max_shards_map['vertex_index'])
    print 'Atlas Solr collections: fulltext_index, edge_index, vertex_index'
    atlas_backup_path = None
    if options.backup_base_path:
      atlas_backup_path = os.path.join(options.backup_base_path, "atlas")
    elif options.backup_ranger_base_path:
      atlas_backup_path = options.backup_atlas_base_path
    if atlas_backup_path is not None:
      config.set('atlas_collections', 'backup_path', atlas_backup_path)
      print 'Atlas backup path: ' + atlas_backup_path
    if options.atlas_hdfs_base_path:
      config.set('atlas_collections', 'hdfs_base_path', options.atlas_hdfs_base_path)
    elif options.hdfs_base_path:
      config.set('atlas_collections', 'hdfs_base_path', options.hdfs_base_path)
  else:
    config.set('atlas_collections', 'enabled', 'false')

  config.add_section('logsearch_collections')
  if "LOGSEARCH_SERVER" in installed_components:
    print "Service detected: " + colors.OKGREEN + "LOGSEARCH" + colors.ENDC

    logsearch_props = get_config_props(cluster_config, 'logsearch-properties')

    logsearch_hadoop_logs_coll_name = logsearch_props['logsearch.solr.collection.service.logs'] if logsearch_props and 'logsearch.solr.collection.service.logs' in logsearch_props else 'hadoop_logs'
    logsearch_audit_logs_coll_name = logsearch_props['logsearch.solr.collection.audit.logs'] if logsearch_props and 'logsearch.solr.collection.audit.logs' in logsearch_props else 'audit_logs'

    config.set('logsearch_collections', 'enabled', 'true')
    config.set('logsearch_collections', 'hadoop_logs_collection_name', logsearch_hadoop_logs_coll_name)
    config.set('logsearch_collections', 'audit_logs_collection_name', logsearch_audit_logs_coll_name)
    config.set('logsearch_collections', 'history_collection_name', 'history')
    print 'Log Search Solr collections: {0}, {1}, history'.format(logsearch_hadoop_logs_coll_name, logsearch_audit_logs_coll_name)
  else:
    config.set('logsearch_collections', 'enabled', 'false')

  if security_enabled == 'true':
    print "Kerberos: enabled"
  else:
    print "Kerberos: disabled"

  with open(options.ini_file, 'w') as f:
    config.write(f)

  print "Config file generation has finished " + colors.OKGREEN + "successfully" + colors.ENDC

def validate_inputs(options):
  errors=[]
  if not options.host:
    errors.append("Option is empty or missing: host")
  if not options.port:
    errors.append("Option is empty or missing: port")
  if not options.cluster:
    errors.append("Option is empty or missing: cluster")
  if not options.username:
    errors.append("Option is empty or missing: username")
  if not options.password:
    errors.append("Option is empty or missing: password")
  if not options.java_home:
    errors.append("Option is empty or missing: java-home")
  elif not os.path.isdir(options.java_home):
    errors.append("java-home directory does not exist ({0})".format(options.java_home))
  return errors

if __name__=="__main__":
  try:
    parser = optparse.OptionParser("usage: %prog [options]")
    parser.add_option("-H", "--host", dest="host", type="string", help="hostname for ambari server")
    parser.add_option("-P", "--port", dest="port", type="int", help="port number for ambari server")
    parser.add_option("-c", "--cluster", dest="cluster", type="string", help="name cluster")
    parser.add_option("-f", "--force-ranger", dest="force_ranger", default=False, action="store_true", help="force to get Ranger details - can be useful if Ranger is configured to use external Solr (but points to internal Sols)")
    parser.add_option("-s", "--ssl", dest="ssl", action="store_true", help="use if ambari server using https")
    parser.add_option("-v", "--verbose", dest="verbose", action="store_true", help="use for verbose logging")
    parser.add_option("-u", "--username", dest="username", type="string", help="username for accessing ambari server")
    parser.add_option("-p", "--password", dest="password", type="string", help="password for accessing ambari server")
    parser.add_option("-j", "--java-home", dest="java_home", type="string", help="local java_home location")
    parser.add_option("-i", "--ini-file", dest="ini_file", default="ambari_solr_migration.ini", type="string", help="Filename of the generated ini file for migration (default: ambari_solr_migration.ini)")
    parser.add_option("--backup-base-path", dest="backup_base_path", default=None, type="string", help="base path for backup, e.g.: /tmp/backup, then /tmp/backup/ranger/ and /tmp/backup/atlas/ folders will be generated")
    parser.add_option("--backup-ranger-base-path", dest="backup_ranger_base_path", default=None, type="string", help="base path for ranger backup (override backup-base-path for ranger), e.g.: /tmp/backup/ranger")
    parser.add_option("--backup-atlas-base-path", dest="backup_atlas_base_path", default=None, type="string", help="base path for atlas backup (override backup-base-path for atlas), e.g.: /tmp/backup/atlas")
    parser.add_option("--hdfs-base-path", dest="hdfs_base_path", default=None, type="string", help="hdfs base path where the collections are located (e.g.: /user/infrasolr). Use if both atlas and ranger collections are on hdfs.")
    parser.add_option("--ranger-hdfs-base-path", dest="ranger_hdfs_base_path", default=None, type="string", help="hdfs base path where the ranger collection is located (e.g.: /user/infra-solr). Use if only ranger collection is on hdfs.")
    parser.add_option("--atlas-hdfs-base-path", dest="atlas_hdfs_base_path", default=None, type="string", help="hdfs base path where the atlas collections are located (e.g.: /user/infra-solr). Use if only atlas collections are on hdfs.")
    parser.add_option("--skip-atlas", dest="skip_atlas", action="store_true", default=False, help="skip to gather Atlas service details")
    parser.add_option("--skip-ranger", dest="skip_ranger", action="store_true", default=False, help="skip to gather Ranger service details")
    parser.add_option("--retry", dest="retry", type="int", default=10, help="number of retries during accessing random solr urls")
    parser.add_option("--delay", dest="delay", type="int", default=5, help="delay (seconds) between retries during accessing random solr urls")
    parser.add_option("--shared-drive", dest="shared_drive", default=False, action="store_true", help="Use if the backup location is shared between hosts.")

    (options, args) = parser.parse_args()

    set_log_level(options.verbose)
    errors = validate_inputs(options)

    if errors:
      print 'Errors'
      for error in errors:
        print '- {0}'.format(error)
      print ''
      parser.print_help()
    else:
      protocol = 'https' if options.ssl else 'http'
      accessor = api_accessor(options.host, options.username, options.password, protocol, options.port)
      try:
        generate_ambari_solr_migration_ini_file(options, accessor, protocol)
      except Exception as exc:
        print traceback.format_exc()
        print 'Config file generation ' + colors.FAIL + 'failed' + colors.ENDC
  except KeyboardInterrupt:
    print
    sys.exit(128 + signal.SIGINT)
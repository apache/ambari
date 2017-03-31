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

import logging
import json
import socket
import urllib2
import urllib

from resource_management.core import shell
from resource_management.libraries.functions.curl_krb_request import curl_krb_request
from resource_management.libraries.functions.get_kinit_path import get_kinit_path
from resource_management.libraries.functions.namenode_ha_utils import get_active_namenode
from resource_management.libraries.script.config_dictionary import ConfigDictionary
from resource_management.core.environment import Environment

CLUSTER_ENV_SECURITY = '{{cluster-env/security_enabled}}'
ACTING_USER = 'pxf'
KEYTAB_FILE = '{{pxf-site/pxf.service.kerberos.keytab}}'
PRINCIPAL_NAME = '{{pxf-site/pxf.service.kerberos.principal}}'
HDFS_SITE = '{{hdfs-site}}'


RESULT_STATE_OK = 'OK'
RESULT_STATE_WARNING = 'WARNING'

PXF_PORT = 51200

logger = logging.getLogger('ambari_alerts')

commonPXFHeaders = {
    "X-GP-SEGMENT-COUNT": "1",
    "X-GP-URL-PORT": PXF_PORT,
    "X-GP-SEGMENT-ID": "-1",
    "X-GP-HAS-FILTER": "0",
    "Accept": "application/json",
    "X-GP-ALIGNMENT": "8",
    "X-GP-ATTRS": "0",
    "X-GP-FORMAT": "TEXT",
    "X-GP-URL-HOST": "localhost"
  }


def get_tokens():
  return (CLUSTER_ENV_SECURITY,
          ACTING_USER,
          KEYTAB_FILE,
          PRINCIPAL_NAME,
          HDFS_SITE)

def _get_delegation_token(namenode_address, user, keytab, principal, kinit_path):
  """
  Gets the kerberos delegation token from name node
  """
  url = namenode_address + "/webhdfs/v1/?op=GETDELEGATIONTOKEN"
  logger.info("Getting delegation token from {0} for PXF".format(url))
  response, _, _  = curl_krb_request(Environment.get_instance().tmp_dir,
                                     keytab,
                                     principal,
                                     url,
                                     "get_delegation_token",
                                     kinit_path,
                                     False,
                                     "Delegation Token",
                                     user)
  json_response = json.loads(response)
  if json_response['Token'] and json_response['Token']['urlString']:
    return json_response['Token']['urlString']

  msg = "Unable to get delegation token for PXF"
  logger.error(msg)
  raise Exception(msg)

def _makeHTTPCall(url, header={}, body=None):
  # timeout in seconds
  timeout = 10
  socket.setdefaulttimeout(timeout)

  try:
    data = None
    if body:
      data = urllib.urlencode(body)
    req = urllib2.Request(url, data, header)

    response = urllib2.urlopen(req)
    responseContent = response.read()
    return responseContent
  except urllib2.URLError as e:
    if hasattr(e, 'reason'):
      logger.error( 'Reason: ' + str(e.reason))
    if hasattr(e, 'code'):
      logger.error('Error code: ' + str(e.code))
    raise e


def _get_pxf_protocol_version(base_url):
  """
  Gets the pxf protocol version number
  """
  logger.info("Fetching PXF protocol version")
  url = base_url + "ProtocolVersion"
  try:
    response = _makeHTTPCall(url)
  except Exception as e:
    raise Exception("URL: " + url + " is not accessible. " + str(e.reason))

  logger.info(response)
  # Sample response: 'PXF protocol version v14'
  if response:
    import re
    # Extract the v14 from the output
    match =  re.search('.*(v\d*).*', response)
    if match:
       return match.group(1)

  raise Exception("version could not be found in response " + response)


def _ensure_kerberos_authentication(user, principal, keytab_file, kinit_path):
  kinit_path_local = get_kinit_path(kinit_path)
  shell.checked_call("{0} -kt {1} {2} > /dev/null".format(kinit_path_local, keytab_file, principal),
                     user=user)

def execute(configurations={}, parameters={}, host_name=None):
  BASE_URL = "http://{0}:{1}/pxf/".format(host_name, PXF_PORT)
  try:
    # Get delegation token if security is enabled
    if CLUSTER_ENV_SECURITY in configurations and configurations[CLUSTER_ENV_SECURITY].lower() == "true":
      resolved_principal = configurations[PRINCIPAL_NAME]
      if resolved_principal is not None:
        resolved_principal = resolved_principal.replace('_HOST', host_name)

      if 'dfs.nameservices' in configurations[HDFS_SITE]:
        if configurations[CLUSTER_ENV_SECURITY]:
          _ensure_kerberos_authentication(configurations[ACTING_USER], resolved_principal, configurations[KEYTAB_FILE], None)
        namenode_address = get_active_namenode(ConfigDictionary(configurations[HDFS_SITE]), configurations[CLUSTER_ENV_SECURITY], configurations[ACTING_USER])[1]
      else:
        namenode_address = configurations[HDFS_SITE]['dfs.namenode.http-address']

      token = _get_delegation_token(namenode_address,
                                    configurations[ACTING_USER],
                                    configurations[KEYTAB_FILE],
                                    resolved_principal,
                                    None)
      commonPXFHeaders.update({"X-GP-TOKEN": token})

    if _get_pxf_protocol_version(BASE_URL).startswith("v"):
      return (RESULT_STATE_OK, ['PXF is functional'])

    message = "Unable to determine PXF version"
    logger.exception(message)
    raise Exception(message)

  except Exception as e:
    message = 'PXF is not functional on host, {0}: {1}'.format(host_name, e)
    logger.exception(message)
    return (RESULT_STATE_WARNING, [message])


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
import time
import subprocess
import os
from  tempfile import gettempdir
from alerts.base_alert import BaseAlert
from collections import namedtuple
from resource_management.libraries.functions.get_port_from_url import get_port_from_url
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.functions import get_klist_path
from ambari_commons import OSCheck
from ambari_commons.inet_utils import resolve_address

# hashlib is supplied as of Python 2.5 as the replacement interface for md5
# and other secure hashes.  In 2.6, md5 is deprecated.  Import hashlib if
# available, avoiding a deprecation warning under 2.6.  Import md5 otherwise,
# preserving 2.4 compatibility.
try:
  import hashlib
  _md5 = hashlib.md5
except ImportError:
  import md5
  _md5 = md5.new

logger = logging.getLogger()

CURL_CONNECTION_TIMEOUT = '20'

class WebAlert(BaseAlert):
  
  def __init__(self, alert_meta, alert_source_meta, config):
    super(WebAlert, self).__init__(alert_meta, alert_source_meta)
    
    # extract any lookup keys from the URI structure
    self.uri_property_keys = None
    if 'uri' in alert_source_meta:
      uri = alert_source_meta['uri']
      self.uri_property_keys = self._lookup_uri_property_keys(uri)

    self.config = config


  def _collect(self):
    if self.uri_property_keys is None:
      raise Exception("Could not determine result. URL(s) were not defined.")

    # use the URI lookup keys to get a final URI value to query
    alert_uri = self._get_uri_from_structure(self.uri_property_keys)      

    logger.debug("[Alert][{0}] Calculated web URI to be {1} (ssl={2})".format(
      self.get_name(), alert_uri.uri, str(alert_uri.is_ssl_enabled)))

    url = self._build_web_query(alert_uri)
    web_response = self._make_web_request(url)
    status_code = web_response.status_code
    time_seconds = web_response.time_millis / 1000
    error_message = web_response.error_msg

    if status_code == 0:
      return (self.RESULT_CRITICAL, [status_code, url, time_seconds, error_message])
    
    if status_code < 400:
      return (self.RESULT_OK, [status_code, url, time_seconds])
    
    return (self.RESULT_WARNING, [status_code, url, time_seconds])


  def _build_web_query(self, alert_uri):
    """
    Builds a URL out of the URI structure. If the URI is already a URL of
    the form http[s]:// then this will return the URI as the URL; otherwise,
    it will build the URL from the URI structure's elements
    """
    # shortcut if the supplied URI starts with the information needed
    string_uri = str(alert_uri.uri)
    if string_uri.startswith('http://') or string_uri.startswith('https://'):
      return alert_uri.uri

    # start building the URL manually
    host = BaseAlert.get_host_from_url(alert_uri.uri)
    if host is None:
      host = self.host_name

    # maybe slightly realistic
    port = 80
    if alert_uri.is_ssl_enabled is True:
      port = 443

    # extract the port
    try:
      port = int(get_port_from_url(alert_uri.uri))
    except:
      pass

    scheme = 'http'
    if alert_uri.is_ssl_enabled is True:
      scheme = 'https'
    if OSCheck.is_windows_family():
      # on windows 0.0.0.0 is invalid address to connect but on linux it resolved to 127.0.0.1
      host = resolve_address(host)
    return "{0}://{1}:{2}".format(scheme, host, str(port))


  def _make_web_request(self, url):
    """
    Makes an http(s) request to a web resource and returns the http code. If
    there was an error making the request, return 0 for the status code.
    """    
    WebResponse = namedtuple('WebResponse', 'status_code time_millis error_msg')
    
    time_millis = 0
    
    try:
      kerberos_keytab = None
      kerberos_principal = None

      if self.uri_property_keys.kerberos_principal is not None:
        kerberos_principal = self._get_configuration_value(
          self.uri_property_keys.kerberos_principal)

        if kerberos_principal is not None:
          # substitute _HOST in kerberos principal with actual fqdn
          kerberos_principal = kerberos_principal.replace('_HOST', self.host_name)

      if self.uri_property_keys.kerberos_keytab is not None:
        kerberos_keytab = self._get_configuration_value(self.uri_property_keys.kerberos_keytab)

      if kerberos_principal is not None and kerberos_keytab is not None:
        # Create the kerberos credentials cache (ccache) file and set it in the environment to use
        # when executing curl. Use the md5 hash of the combination of the principal and keytab file
        # to generate a (relatively) unique cache filename so that we can use it as needed.
        tmp_dir = self.config.get('agent', 'tmp_dir')
        if tmp_dir is None:
          tmp_dir = gettempdir()

        ccache_file_name = _md5("{0}|{1}".format(kerberos_principal, kerberos_keytab)).hexdigest()
        ccache_file_path = "{0}{1}web_alert_cc_{2}".format(tmp_dir, os.sep, ccache_file_name)
        kerberos_env = {'KRB5CCNAME': ccache_file_path}

        # If there are no tickets in the cache or they are expired, perform a kinit, else use what
        # is in the cache
        klist_path_local = get_klist_path()

        if os.system("{0} -s {1}".format(klist_path_local, ccache_file_path)) != 0:
          kinit_path_local = get_kinit_path()
          logger.debug("[Alert][{0}] Enabling Kerberos authentication via GSSAPI using ccache at {1}."
                       .format(self.get_name(), ccache_file_path))
          os.system("{0} -l 5m -c {1} -kt {2} {3} > /dev/null".format(kinit_path_local, ccache_file_path, kerberos_keytab, kerberos_principal))
        else:
          logger.debug("[Alert][{0}] Kerberos authentication via GSSAPI already enabled using ccache at {1}."
                       .format(self.get_name(), ccache_file_path))
      else:
        kerberos_env = None

      # substitute 0.0.0.0 in url with actual fqdn
      url = url.replace('0.0.0.0', self.host_name)
      start_time = time.time()
      curl = subprocess.Popen(['curl', '--negotiate', '-u', ':', '-sL', '-w',
        '%{http_code}', url, '--connect-timeout', CURL_CONNECTION_TIMEOUT,
        '-o', '/dev/null'], stdout=subprocess.PIPE, stderr=subprocess.PIPE, env=kerberos_env)

      out, err = curl.communicate()

      if err != '':
        raise Exception(err)

      response_code = int(out)
      time_millis = time.time() - start_time
    except Exception, exc:
      if logger.isEnabledFor(logging.DEBUG):
        logger.exception("[Alert][{0}] Unable to make a web request.".format(self.get_name()))

      return WebResponse(status_code=0, time_millis=0, error_msg=str(exc))

    return WebResponse(status_code=response_code, time_millis=time_millis, error_msg=None)


  def _get_reporting_text(self, state):
    '''
    Gets the default reporting text to use when the alert definition does not
    contain any.
    :param state: the state of the alert in uppercase (such as OK, WARNING, etc)
    :return:  the parameterized text
    '''
    if state == self.RESULT_CRITICAL:
      return 'Connection failed to {1}'

    return 'HTTP {0} response in {2:.4f} seconds'
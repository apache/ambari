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
import urllib2
from alerts.base_alert import BaseAlert
from collections import namedtuple
from resource_management.libraries.functions.get_port_from_url import get_port_from_url

logger = logging.getLogger()

class WebAlert(BaseAlert):
  
  def __init__(self, alert_meta, alert_source_meta):
    super(WebAlert, self).__init__(alert_meta, alert_source_meta)
    
    # extract any lookup keys from the URI structure
    self.uri_property_keys = None
    if 'uri' in alert_source_meta:
      uri = alert_source_meta['uri']
      self.uri_property_keys = self._lookup_uri_property_keys(uri)

      
  def _collect(self):
    if self.uri_property_keys is None:
      raise Exception("Could not determine result. URL(s) were not defined.")

    # use the URI lookup keys to get a final URI value to query
    alert_uri = self._get_uri_from_structure(self.uri_property_keys)      

    logger.debug("Calculated web URI to be {0} (ssl={1})".format(alert_uri.uri, 
        str(alert_uri.is_ssl_enabled)))

    url = self._build_web_query(alert_uri)
    web_response = self._make_web_request(url)
    status_code = web_response.status_code
    time_seconds = web_response.time_millis / 1000

    if status_code == 0:
      return (self.RESULT_CRITICAL, [status_code, url, time_seconds])
    
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

    return "{0}://{1}:{2}".format(scheme, host, str(port))


  def _make_web_request(self, url):
    """
    Makes an http(s) request to a web resource and returns the http code. If
    there was an error making the request, return 0 for the status code.
    """    
    WebResponse = namedtuple('WebResponse', 'status_code time_millis')
    
    time_millis = 0
    
    try:
      start_time = time.time()      
      response = urllib2.urlopen(url)
      time_millis = time.time() - start_time
    except:
      if logger.isEnabledFor(logging.DEBUG):
        logger.exception("Unable to make a web request.")
      
      return WebResponse(status_code=0, time_millis=0)
    
    return WebResponse(status_code=response.getcode(), time_millis=time_millis) 
  
  
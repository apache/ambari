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

import logging
import urllib2
from alerts.base_alert import BaseAlert
from resource_management.libraries.functions.get_port_from_url import get_port_from_url

logger = logging.getLogger()

class WebAlert(BaseAlert):
  
  def __init__(self, alert_meta, alert_source_meta):
    super(WebAlert, self).__init__(alert_meta, alert_source_meta)
    
    # extract any lookup keys from the URI structure
    self.uri_property_keys = self._lookup_uri_property_keys(alert_source_meta['uri'])

      
  def _collect(self):
    if self.uri_property_keys is None:
      raise Exception("Could not determine result. URL(s) were not defined.")

    # use the URI lookup keys to get a final URI value to query
    alert_uri = self._get_uri_from_structure(self.uri_property_keys)      

    logger.debug("Calculated web URI to be {0} (ssl={1})".format(alert_uri.uri, 
        str(alert_uri.is_ssl_enabled)))

    host = BaseAlert.get_host_from_url(alert_uri.uri)
    if host is None:
      host = self.host_name

    # maybe slightly realistic
    port = 80 
    if alert_uri.is_ssl_enabled:
      port = 443
      
    try:      
      port = int(get_port_from_url(alert_uri.uri))
    except:
      pass

    status_code = self._make_web_request(host, port, alert_uri.is_ssl_enabled)

    if status_code == 0:
      return (self.RESULT_CRITICAL, [status_code, host, port])
    
    if status_code <= 401:
      return (self.RESULT_OK, [status_code, host, port])
    
    return (self.RESULT_WARNING, [status_code, host, port])


  def _make_web_request(self, host, port, ssl):
    '''
    Makes an http(s) request to a web resource and returns the http code. If
    there was an error making the request, return 0 for the status code.
    '''    
    url = "{0}://{1}:{2}".format(
        "https" if ssl else "http", host, str(port))
    
    try:
      response = urllib2.urlopen(url)
    except:
      if logger.isEnabledFor(logging.DEBUG):
        logger.exception("Unable to make a web request.")
      
      return 0
    
    return response.getcode()
  
  
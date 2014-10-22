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
import re
import time
from collections import namedtuple

logger = logging.getLogger()

class BaseAlert(object):
  RESULT_OK = 'OK'
  RESULT_WARNING = 'WARNING'
  RESULT_CRITICAL = 'CRITICAL'
  RESULT_UNKNOWN = 'UNKNOWN'
  
  def __init__(self, alert_meta, alert_source_meta):
    self.alert_meta = alert_meta
    self.alert_source_meta = alert_source_meta
    self.cluster = ''
    self.host_name = ''
    self._lookup_keys = []
    
    
  def interval(self):
    """ gets the defined interval this check should run """
    if not self.alert_meta.has_key('interval'):
      return 1
    else:
      interval = self.alert_meta['interval']
      return 1 if interval < 1 else interval


  def is_enabled(self):
    """
    gets whether the definition is enabled
    """
    return self.alert_meta['enabled']
  

  def get_name(self):
    """
    gets the unique name of the alert definition
    """
    return self.alert_meta['name']


  def get_uuid(self):
    """
    gets the unique has of the alert definition
    """
    return self.alert_meta['uuid']


  def set_helpers(self, collector, value_dict):
    """ sets helper objects for alerts without having to use them in a constructor """
    self.collector = collector
    self.config_value_dict = value_dict


  def set_cluster(self, cluster, host):
    """ sets cluster information for the alert """
    self.cluster = cluster
    self.host_name = host


  def collect(self):
    """ method used for collection.  defers to _collect() """
    
    res = (BaseAlert.RESULT_UNKNOWN, [])
    res_base_text = "Unknown {0}"
    
    try:
      res = self._collect()
      res_base_text = self.alert_source_meta['reporting'][res[0].lower()]['text']
    except Exception as e:
      message = "Unable to run alert {0}".format(str(self.alert_meta['name']))
      
      # print the exception if in DEBUG, otherwise just log the warning
      if logger.isEnabledFor(logging.DEBUG):
        logger.exception(message)
      else:
        logger.warning(message)

      res = (BaseAlert.RESULT_UNKNOWN, [str(e)])
      res_base_text = "Unknown {0}"
    
    
    if logger.isEnabledFor(logging.DEBUG):
      logger.debug("debug alert result: {0}".format(str(res)))
      
    data = {}
    data['name'] = self._find_value('name')
    data['label'] = self._find_value('label')
    data['state'] = res[0]
    data['text'] = res_base_text.format(*res[1])
    data['cluster'] = self.cluster
    data['service'] = self._find_value('serviceName')
    data['component'] = self._find_value('componentName')
    data['timestamp'] = long(time.time() * 1000)
    data['uuid'] = self._find_value('uuid')
    data['enabled'] = self._find_value('enabled')

    if logger.isEnabledFor(logging.DEBUG):
      logger.debug("debug alert text: {0}".format(data['text']))
    
    self.collector.put(self.cluster, data)


  def _find_value(self, meta_key):
    """ safe way to get a value when outputting result json.  will not throw an exception """
    if self.alert_meta.has_key(meta_key):
      return self.alert_meta[meta_key]
    else:
      return None


  def get_lookup_keys(self):
    """ returns a list of lookup keys found for this alert """
    return self._lookup_keys


  def _find_lookup_property(self, key):
    """
    check if the supplied key is parameterized and appends the extracted key
    to the array of keys
    """
    keys = re.findall("{{([\S]+)}}", key)
    
    if len(keys) > 0:
      logger.debug("Found parameterized key {0} for {1}".format(str(keys), str(self)))
      self._lookup_keys.append(keys[0])
      return keys[0]
      
    return key


  def _lookup_property_value(self, key):
    """
    in the case of specifying a configuration path, lookup that path's value
    """
    if not key in self._lookup_keys:
      return key

    if key in self.config_value_dict:
      return self.config_value_dict[key]
    else:
      return None

    
  def _lookup_uri_property_keys(self, uri_structure):
    """
    Loads the configuration lookup keys that the URI structure needs. This
    will return a named tuple that contains the keys needed to lookup
    parameterized URI values from the URI structure. The URI structure looks 
    something like:
    
    "uri":{ 
      "http": foo,
      "https": bar,
      ...
    }
    """
    
    if uri_structure is None:
      return None
    
    http_key = None
    https_key = None
    https_property_key = None
    https_property_value_key = None
    default_port = None
    
    if 'http' in uri_structure:
      http_key = self._find_lookup_property(uri_structure['http'])
    
    if 'https' in uri_structure:
      https_key = self._find_lookup_property(uri_structure['https'])
      
    if 'https_property' in uri_structure:
      https_property_key = self._find_lookup_property(uri_structure['https_property'])
      
    if 'https_property_value' in uri_structure:
      https_property_value_key = uri_structure['https_property_value']

    if 'default_port' in uri_structure:
      default_port = uri_structure['default_port']

    AlertUriLookupKeys = namedtuple('AlertUriLookupKeys', 
        'http https https_property https_property_value default_port')
    
    alert_uri_lookup_keys = AlertUriLookupKeys(http=http_key, https=https_key, 
        https_property=https_property_key, 
        https_property_value=https_property_value_key, default_port=default_port)
    
    return alert_uri_lookup_keys

    
  def _get_uri_from_structure(self, alert_uri_lookup_keys):
    """
    Gets the URI to use by examining the URI structure from the definition.
    This will return a named tuple that has the uri and the SSL flag. The
    URI structure looks something like:
    
    "uri":{ 
      "http": foo,
      "https": bar,
      ...
    }
    """
    
    if alert_uri_lookup_keys is None:
      return None
    
    http_uri = None
    https_uri = None
    https_property = None
    https_property_value = None

    # create a named tuple to return both the concrete URI and SSL flag
    AlertUri = namedtuple('AlertUri', 'uri is_ssl_enabled')
    
    # attempt to parse and parameterize the various URIs; properties that
    # do not exist int he lookup map are returned as None
    if alert_uri_lookup_keys.http is not None:
      http_uri = self._lookup_property_value(alert_uri_lookup_keys.http)
    
    if alert_uri_lookup_keys.https is not None:
      https_uri = self._lookup_property_value(alert_uri_lookup_keys.https)

    if alert_uri_lookup_keys.https_property is not None:
      https_property = self._lookup_property_value(alert_uri_lookup_keys.https_property)

    if alert_uri_lookup_keys.https_property_value is not None:
      https_property_value = self._lookup_property_value(alert_uri_lookup_keys.https_property_value)

    # without a URI, there's no way to create the structure we need - return
    # the default port if specified, otherwise throw an exception
    if http_uri is None and https_uri is None:
      if alert_uri_lookup_keys.default_port is not None:
        alert_uri = AlertUri(uri=alert_uri_lookup_keys.default_port, is_ssl_enabled=False)
        return alert_uri
      else:
        raise Exception("Could not determine result. Either the http or https URI must be specified.")

    # start out assuming plaintext
    uri = http_uri
    is_ssl_enabled = False
    
    if https_uri is not None:
      # https without http implies SSL
      if http_uri is None:
        is_ssl_enabled = True
        uri = https_uri
      elif https_property is not None and https_property == https_property_value:
        is_ssl_enabled = True
        uri = https_uri
    
    alert_uri = AlertUri(uri=uri, is_ssl_enabled=is_ssl_enabled)
    return alert_uri


  def _collect(self):
    """
    Low level function to collect alert data.  The result is a tuple as:
    res[0] = the result code
    res[1] = the list of arguments supplied to the reporting text for the result code
    """  
    raise NotImplementedError

  """
  See RFC3986, Appendix B
  Tested on the following cases:
    "192.168.54.1"
    "192.168.54.2:7661
    "hdfs://192.168.54.3/foo/bar"
    "ftp://192.168.54.4:7842/foo/bar"

    Returns None if only a port is passsed in
  """
  @staticmethod
  def get_host_from_url(uri):
    if uri is None:
      return None
    
    # if not a string, return None
    if not isinstance(uri, basestring):
      return None    
        
    # RFC3986, Appendix B
    parts = re.findall('^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\?([^#]*))?(#(.*))?', uri)

    # index of parts
    # scheme    = 1
    # authority = 3
    # path      = 4
    # query     = 6
    # fragment  = 8

    host_and_port = uri
    if 0 == len(parts[0][1]):
      host_and_port = parts[0][4]
    elif 0 == len(parts[0][2]):
      host_and_port = parts[0][1]
    elif parts[0][2].startswith("//"):
      host_and_port = parts[0][3]

    if -1 == host_and_port.find(':'):
      if host_and_port.isdigit():
        return None    
      
      return host_and_port
    else:
      return host_and_port.split(':')[0]

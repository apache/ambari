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
import httplib
import os

logger = logging.getLogger()
try:
  import kerberos
except ImportError:
  import krberr as kerberos
  logger.warn('import kerberos exception: %s' % str(ImportError))
pass

class SPNEGOKerberosAuth:
  def __init__(self):
    self.krb_context = None

  def authenticate_handshake (self, connection, method, service_url, body, headers, kinit_cmd, klist_cmd):
    # kinit to ensure ticket valid
    self.execute_kinit(kinit_cmd, klist_cmd)

    try:
      # Authenticate the client request
      response = self.authenticate_client(connection, method, service_url, body, headers)

      # Authenticate the response from the server
      if response:
        self.authenticate_server(response)
      return response
    finally:
      # Clean the client context after the handshake
      self.clean_client_context()
    pass

  def execute_kinit(self, kinit_cmd, klist_cmd):
    exit_status = os.system(kinit_cmd)
    logger.debug("kinit exit_status: {0}".format(exit_status))
    logger.debug(os.system(klist_cmd))
    return exit_status

  def authenticate_client(self, connection, method, service_url, body, headers):
    service = "HTTP@%s" % connection.host.lower()
    logger.debug("connection: %s", connection)
    logger.debug("service: %s", service)

    auth_header = self.get_authorization_header(service)
    logger.debug("Authorization: %s" % auth_header)

    # Send 2nd HTTP request with authorization header
    headers['Authorization'] = auth_header
    try:
      connection.request(method, service_url, body, headers)
      response = connection.getresponse()
    except Exception, e:
      logger.warn('2nd HTTP request exception from server: %s' % str(e))
      return None
    pass
    if response:
      logger.debug("2nd HTTP response from server: retcode = {0}, reason = {1}"
                    .format(response.status, response.reason))
      logger.debug(str(response.read()))
      logger.debug("response headers: {0}".format(response.getheaders()))
    return response

  def get_authorization_header(self, service):
    # Initialize the context object for client-side authentication with a service principal
    try:
      result, self.krb_context = kerberos.authGSSClientInit(service)
      if result == -1:
        logger.warn('authGSSClientInit result: {0}'.format(result))
        return None
    except kerberos.GSSError, e:
      logger.warn('authGSSClientInit exception: %s' % str(e))
      return None
    pass

    # Process the first client-side step with the context
    try:
      result = kerberos.authGSSClientStep(self.krb_context, "")
      if result == -1:
        logger.warn('authGSSClientStep result for authenticate client: {0}'.format(result))
        return None
    except kerberos.GSSError, e:
      logger.warn('authGSSClientStep exception for authenticate client: %s' % str(e))
      return None
    pass

    # Get the client response from the first client-side step
    try:
      negotiate_value = kerberos.authGSSClientResponse(self.krb_context)
      logger.debug("authGSSClientResponse response:{0}".format(negotiate_value))
    except kerberos.GSSError, e:
      logger.warn('authGSSClientResponse exception: %s' % str(e))
      return None
    pass

    # Build the authorization header
    return "Negotiate %s" % negotiate_value

  def authenticate_server(self, response):
    auth_header = response.getheader('www-authenticate', None)
    negotiate_value = self.get_negotiate_value(auth_header)
    if negotiate_value == None:
      logger.warn('www-authenticate header not found')

    # Process the client-side step with the context and the negotiate value from 2nd HTTP response
    try:
      result = kerberos.authGSSClientStep(self.krb_context, negotiate_value)
      if result == -1:
        logger.warn('authGSSClientStep result for authenticate server: {0}'.format(result))
    except kerberos.GSSError, e:
      logger.warn('authGSSClientStep exception for authenticate server: %s' % str(e))
      result = -1
    pass
    return result

  def clean_client_context(self):
    # Destroy the context for client-side authentication
    try:
      result = kerberos.authGSSClientClean(self.krb_context)
      logger.debug("authGSSClientClean result:{0}".format(result))
    except kerberos.GSSError, e:
      logger.warn('authGSSClientClean exception: %s' % str(e))
      result = -1
    pass
    return result

  def get_hadoop_auth_cookie(self, set_cookie_header):
    if set_cookie_header:
      for field in set_cookie_header.split(";"):
        if field.startswith('hadoop.auth='):
          return field
      else:
        return None
    return None

  def get_negotiate_value(self, auth_header):
    if auth_header:
      for field in auth_header.split(","):
        key, __, value = field.strip().partition(" ")
        if key.lower() == "negotiate":
          return value.strip()
      else:
        return None
    return None

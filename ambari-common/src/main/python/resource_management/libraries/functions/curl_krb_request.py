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

Ambari Agent

"""

__all__ = ["curl_krb_request"]
import logging
import os
import time
import subprocess

from resource_management.core import shell
from resource_management.core.exceptions import Fail
from get_kinit_path import get_kinit_path
from get_klist_path import get_klist_path
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

CONNECTION_TIMEOUT = 10
MAX_TIMEOUT = 12

logger = logging.getLogger()


def curl_krb_request(tmp_dir, keytab, principal, url, cache_file_prefix, krb_exec_search_paths,
                     return_only_http_code, alert_name, user):
  import uuid
  # Create the kerberos credentials cache (ccache) file and set it in the environment to use
  # when executing curl. Use the md5 hash of the combination of the principal and keytab file
  # to generate a (relatively) unique cache filename so that we can use it as needed.
  ccache_file_name = _md5("{0}|{1}".format(principal, keytab)).hexdigest()
  ccache_file_path = "{0}{1}{2}_cc_{3}".format(tmp_dir, os.sep, cache_file_prefix, ccache_file_name)
  kerberos_env = {'KRB5CCNAME': ccache_file_path}

  # If there are no tickets in the cache or they are expired, perform a kinit, else use what
  # is in the cache
  if krb_exec_search_paths:
    klist_path_local = get_klist_path(krb_exec_search_paths)
  else:
    klist_path_local = get_klist_path()

  if shell.call("{0} -s {1}".format(klist_path_local, ccache_file_path), user=user)[0] != 0:
    if krb_exec_search_paths:
      kinit_path_local = get_kinit_path(krb_exec_search_paths)
    else:
      kinit_path_local = get_kinit_path()
    logger.debug("[Alert][{0}] Enabling Kerberos authentication via GSSAPI using ccache at {1}.".format(
      alert_name, ccache_file_path))

    shell.checked_call("{0} -l 5m -c {1} -kt {2} {3} > /dev/null".format(kinit_path_local, ccache_file_path, keytab, principal), user=user)
  else:
    logger.debug("[Alert][{0}] Kerberos authentication via GSSAPI already enabled using ccache at {1}.".format(
      alert_name, ccache_file_path))

  # check if cookies dir exists, if not then create it
  cookies_dir = os.path.join(tmp_dir, "cookies")

  if not os.path.exists(cookies_dir):
    os.makedirs(cookies_dir)

  cookie_file_name = str(uuid.uuid4())
  cookie_file = os.path.join(cookies_dir, cookie_file_name)

  start_time = time.time()
  error_msg = None
  try:
    if return_only_http_code:
      _, curl_stdout, curl_stderr = shell.checked_call(['curl', '-k', '--negotiate', '-u', ':', '-b', cookie_file, '-c', cookie_file, '-w',
                             '%{http_code}', url, '--connect-timeout', str(CONNECTION_TIMEOUT), '--max-time', str(MAX_TIMEOUT), '-o', '/dev/null'],
                             stderr=subprocess.PIPE, env=kerberos_env, user=user)
    else:
      # returns response body
      _, curl_stdout, curl_stderr = shell.checked_call(['curl', '-k', '--negotiate', '-u', ':', '-b', cookie_file, '-c', cookie_file,
                             url, '--connect-timeout', str(CONNECTION_TIMEOUT), '--max-time', str(MAX_TIMEOUT)],
                             stderr=subprocess.PIPE, env=kerberos_env, user=user)
  except Fail:
    if logger.isEnabledFor(logging.DEBUG):
      logger.exception("[Alert][{0}] Unable to make a web request.".format(alert_name))
    raise
  finally:
    if os.path.isfile(cookie_file):
      os.remove(cookie_file)

  # empty quotes evaluates to false
  if curl_stderr:
    error_msg = curl_stderr

  time_millis = time.time() - start_time

  # empty quotes evaluates to false
  if curl_stdout:
    if return_only_http_code:
      return (int(curl_stdout), error_msg, time_millis)
    else:
      return (curl_stdout, error_msg, time_millis)

  logger.debug("[Alert][{0}] Curl response is empty! Please take a look at error message: ".
               format(alert_name, str(error_msg)))
  return ("", error_msg, time_millis)

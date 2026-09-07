#!/usr/bin/env python3
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
import math
import re
import time
from contextlib import nullcontext

from .get_kinit_path import get_kinit_path
from resource_management.core.exceptions import Fail
from resource_management.libraries.functions.get_user_call_output import (
  get_user_call_output,
)
from resource_management.libraries.functions.private_kerberos_cache import (
  PrivateKerberosCache,
)
from resource_management.libraries.functions.private_temporary_file import (
  private_temporary_file,
)

CONNECTION_TIMEOUT_DEFAULT = 10
MAX_TIMEOUT_DEFAULT = CONNECTION_TIMEOUT_DEFAULT + 2

logger = logging.getLogger()

# the default time in between forced kinit calls (4 hours)
DEFAULT_KERBEROS_KINIT_TIMER_MS = 14400000

# a parameter which can be used to pass around the above timout value
KERBEROS_KINIT_TIMER_PARAMETER = "kerberos.kinit.timer"


def is_kinit_refresh_required(current_time, last_kinit_time, kinit_timer_ms):
  if not last_kinit_time:
    return True
  elapsed_ms = max(0, current_time - last_kinit_time) * 1000
  return elapsed_ms >= max(0, int(kinit_timer_ms))


def curl_krb_request(
  tmp_dir,
  keytab,
  principal,
  url,
  cache_file_prefix,
  krb_exec_search_paths,
  return_only_http_code,
  caller_label,
  user,
  connection_timeout=CONNECTION_TIMEOUT_DEFAULT,
  ca_certs=None,
  kinit_timer_ms=DEFAULT_KERBEROS_KINIT_TIMER_MS,
  method="",
  body="",
  header="",
  use_system_proxy_settings=True,
  follow_redirects=True,
  fail_on_http_error=False,
  max_response_bytes=None,
  verify_ssl=True,
):
  """
  Makes a curl request using a private, per-request Kerberos credential cache.

  :param tmp_dir: the directory to use for storing the local kerberos cache for this request.
  :param keytab: the location of the keytab to use when performing a kinit
  :param principal: the principal to use when performing a kinit
  :param url: the URL to request
  :param cache_file_prefix: an identifier used to build the unique cache name for this request.
                            This ensures that multiple requests can use the same cache.
  :param krb_exec_search_paths: the search path to use for invoking kerberos binaries
  :param return_only_http_code: True to return only the HTTP code, False to return GET content
  :param caller_label: an identifier to give context into the caller of this module (used for logging)
  :param user: the user to invoke the curl command as
  :param connection_timeout: if specified, a connection timeout for curl (default 10 seconds)
  :param ca_certs: path to certificates
  :param kinit_timer_ms: retained for compatibility; private caches always obtain a fresh ticket.
  :return:
  """

  # backward compatibility with old code and management packs, etc. All new code need pass ca_certs explicitly
  if ca_certs is None:
    try:
      from ambari_agent.AmbariConfig import AmbariConfig

      ca_certs = AmbariConfig.get_resolved_config().get_ca_cert_file_path()
    except Exception:
      pass

  try:
    requested_timeout = float(connection_timeout)
  except (TypeError, ValueError) as error:
    raise Fail(f"Invalid curl connection timeout: {connection_timeout!r}") from error
  if not math.isfinite(requested_timeout) or requested_timeout <= 0:
    raise Fail("Curl connection timeout must be a positive finite number")
  connection_timeout = max(1, math.ceil(requested_timeout))
  maximum_timeout = connection_timeout + 2

  ssl_options = ["--cacert", ca_certs] if ca_certs else ([] if verify_ssl else ["-k"])
  proxy_options = [] if use_system_proxy_settings else ["--noproxy", "*"]
  transfer_options = []
  if follow_redirects:
    transfer_options.append("--location")
  if fail_on_http_error:
    transfer_options.append("--fail")
  if max_response_bytes is not None:
    transfer_options.extend(["--max-filesize", str(max_response_bytes)])
  safe_prefix = re.sub(r"[^A-Za-z0-9_.-]", "_", str(cache_file_prefix))[:64]
  cache_prefix = f"ambari-curl-{safe_prefix or 'request'}-"
  kinit_path_local = get_kinit_path(krb_exec_search_paths)

  with PrivateKerberosCache(
    user,
    temp_dir=tmp_dir,
    prefix=cache_prefix,
  ) as kerberos_cache:
    kerberos_cache.kinit(kinit_path_local, keytab, principal)
    cookie_file = f"{kerberos_cache.cache_dir}/cookies"
    body_context = (
      private_temporary_file(
        body,
        user,
        temp_dir=kerberos_cache.cache_dir,
        prefix="ambari-curl-body-",
      )
      if body
      else nullcontext(None)
    )

    with body_context as request_body_file:
      curl_command = (
        ["curl", "--silent", "--show-error"]
        + transfer_options
        + ssl_options
        + proxy_options
        + [
          "--negotiate",
          "-u",
          ":",
          "-b",
          cookie_file,
          "-c",
          cookie_file,
        ]
      )
      if return_only_http_code:
        curl_command.extend(["-w", "%{http_code}", "-o", "/dev/null"])
      curl_command.extend(
        [
          "--connect-timeout",
          str(connection_timeout),
          "--max-time",
          str(maximum_timeout),
        ]
      )
      if method:
        if header:
          curl_command.extend(["-H", header])
        curl_command.extend(["-X", method])
        if request_body_file is not None:
          curl_command.extend(["--data-binary", f"@{request_body_file}"])
      curl_command.append(url)

      start_time = time.time()
      try:
        _, curl_stdout, curl_stderr = get_user_call_output(
          curl_command,
          user=user,
          env=kerberos_cache.environment,
          quiet=True,
        )
      except Fail:
        logger.debug("Unable to make a curl request for %s.", caller_label)
        raise Fail(f"Unable to make a curl request for {caller_label}.") from None
      elapsed_time = time.time() - start_time

  error_msg = curl_stderr or None
  if curl_stdout:
    if return_only_http_code:
      return (int(curl_stdout), error_msg, elapsed_time)
    return (curl_stdout, error_msg, elapsed_time)

  logger.debug(
    "The curl response for %s is empty; standard error = %s",
    caller_label,
    str(error_msg),
  )

  return ("", error_msg, elapsed_time)

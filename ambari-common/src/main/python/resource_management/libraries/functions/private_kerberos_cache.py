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
"""

import os
import tempfile

from resource_management.core.exceptions import Fail
from resource_management.core.resources.system import Directory, Execute, File


class PrivateKerberosCache:
  def __init__(self, user, group=None, temp_dir="/tmp", prefix="ambari-krb5-"):
    if not user:
      raise Fail("A service user is required for a private Kerberos cache")
    self.user = user
    self.group = group
    self.temp_dir = temp_dir
    self.prefix = prefix
    self.cache_root = None
    self.cache_dir = None
    self.cache_path = None

  @property
  def cache_name(self):
    if self.cache_path is None:
      raise Fail("Kerberos cache has not been created")
    return f"FILE:{self.cache_path}"

  @property
  def environment(self):
    return {"KRB5CCNAME": self.cache_name}

  def merge_environment(self, environment=None):
    merged = dict(environment or {})
    merged.update(self.environment)
    return merged

  def kinit(self, kinit_path, keytab, principal, timeout=30):
    if not kinit_path or not keytab or not principal:
      raise Fail("Kerberos executable, keytab, and principal are required")
    Execute(
      (kinit_path, "-c", self.cache_name, "-kt", keytab, principal),
      user=self.user,
      environment=self.environment,
      timeout=timeout,
    )

  def __enter__(self):
    try:
      self.cache_root = tempfile.mkdtemp(prefix=self.prefix, dir=self.temp_dir)
      self.cache_dir = os.path.join(self.cache_root, "cache")
      self.cache_path = os.path.join(self.cache_dir, "krb5cc")
      Directory(
        self.cache_root,
        owner="root",
        group="root",
        mode=0o711,
      )
      cache_directory_options = {
        "owner": self.user,
        "mode": 0o700,
      }
      if self.group:
        cache_directory_options["group"] = self.group
      Directory(self.cache_dir, **cache_directory_options)
    except Exception as setup_error:
      if self.cache_root is not None:
        try:
          Directory(self.cache_root, action="delete")
        except Exception as cleanup_error:
          raise Fail(
            f"Could not configure private Kerberos cache: {setup_error}; "
            f"rollback also failed: {cleanup_error}"
          ) from setup_error
      raise
    return self

  def __exit__(self, exception_type, exception, traceback):
    cleanup_errors = []
    for resource, path in (
      (File, self.cache_path),
      (Directory, self.cache_dir),
      (Directory, self.cache_root),
    ):
      if path is None:
        continue
      try:
        resource(path, action="delete")
      except Exception as error:
        cleanup_errors.append(error)

    if cleanup_errors:
      cleanup_details = "; ".join(str(error) for error in cleanup_errors)
      if exception is not None:
        raise Fail(
          f"Operation failed: {exception}; private Kerberos cache cleanup also "
          f"failed: {cleanup_details}"
        ) from exception
      raise Fail(
        f"Could not remove private Kerberos cache: {cleanup_details}"
      ) from cleanup_errors[0]
    return False

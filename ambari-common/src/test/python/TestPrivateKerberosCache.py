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

import unittest
from unittest.mock import call, patch

from resource_management.core.exceptions import Fail
from resource_management.libraries.functions import private_kerberos_cache


class TestPrivateKerberosCache(unittest.TestCase):
  def test_each_context_uses_a_unique_private_cache_and_structured_kinit(self):
    roots = ("/tmp/ambari-krb5-first", "/tmp/ambari-krb5-second")
    with (
      patch.object(
        private_kerberos_cache.tempfile, "mkdtemp", side_effect=roots
      ),
      patch.object(private_kerberos_cache, "Directory") as directory,
      patch.object(private_kerberos_cache, "File"),
      patch.object(private_kerberos_cache, "Execute") as execute,
    ):
      cache_names = []
      for _ in roots:
        with private_kerberos_cache.PrivateKerberosCache(
          "service", "hadoop"
        ) as cache:
          cache_names.append(cache.cache_name)
          cache.kinit(
            "/usr/bin/kinit",
            "/etc/security/keytabs/service keytab",
            "service/host@REALM;$(id)",
          )

    self.assertEqual(
      [
        "FILE:/tmp/ambari-krb5-first/cache/krb5cc",
        "FILE:/tmp/ambari-krb5-second/cache/krb5cc",
      ],
      cache_names,
    )
    self.assertEqual(
      [
        call(
          (
            "/usr/bin/kinit",
            "-c",
            cache_name,
            "-kt",
            "/etc/security/keytabs/service keytab",
            "service/host@REALM;$(id)",
          ),
          user="service",
          environment={"KRB5CCNAME": cache_name},
          timeout=30,
        )
        for cache_name in cache_names
      ],
      execute.call_args_list,
    )
    self.assertIn(
      call(
        "/tmp/ambari-krb5-first",
        owner="root",
        group="root",
        mode=0o711,
      ),
      directory.call_args_list,
    )
    self.assertIn(
      call(
        "/tmp/ambari-krb5-first/cache",
        owner="service",
        mode=0o700,
        group="hadoop",
      ),
      directory.call_args_list,
    )

  def test_operation_and_cleanup_failures_are_both_reported(self):
    with (
      patch.object(
        private_kerberos_cache.tempfile,
        "mkdtemp",
        return_value="/tmp/ambari-krb5-failure",
      ),
      patch.object(private_kerberos_cache, "Directory"),
      patch.object(
        private_kerberos_cache,
        "File",
        side_effect=Fail("cache file cleanup failed"),
      ),
    ):
      with self.assertRaisesRegex(
        Fail, "primary operation failed.*cache file cleanup failed"
      ) as raised:
        with private_kerberos_cache.PrivateKerberosCache("service"):
          raise Fail("primary operation failed")

    self.assertIsInstance(raised.exception.__cause__, Fail)

  def test_setup_failure_rolls_back_the_outer_directory(self):
    def directory(path, **kwargs):
      if path.endswith("/cache"):
        raise Fail("inner directory failed")

    with (
      patch.object(
        private_kerberos_cache.tempfile,
        "mkdtemp",
        return_value="/tmp/ambari-krb5-rollback",
      ),
      patch.object(
        private_kerberos_cache, "Directory", side_effect=directory
      ) as directory_resource,
    ):
      with self.assertRaisesRegex(Fail, "inner directory failed"):
        with private_kerberos_cache.PrivateKerberosCache("service"):
          pass

    directory_resource.assert_called_with(
      "/tmp/ambari-krb5-rollback", action="delete"
    )


if __name__ == "__main__":
  unittest.main()

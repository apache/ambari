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

from types import SimpleNamespace
import unittest
from unittest.mock import MagicMock, patch

from resource_management.libraries.providers import hdfs_resource


class TestHdfsResourceKerberos(unittest.TestCase):
  def test_provider_creates_and_initializes_private_cache(self):
    provider = object.__new__(hdfs_resource.HdfsResourceProvider)
    provider.resource = SimpleNamespace(
      security_enabled=True,
      user="hdfs",
      kinit_path_local="/usr/bin/kinit",
      keytab="/etc/security/keytabs/hdfs keytab",
      principal_name="hdfs/host@REALM;$(id)",
    )
    cache = MagicMock()
    with patch.object(hdfs_resource, "PrivateKerberosCache") as cache_class:
      provider.kerberos_cache()
    cache_class.assert_called_once_with(
      "hdfs", prefix="ambari-hdfs-resource-"
    )

    provider.kinit(cache)
    cache.kinit.assert_called_once_with(
      "/usr/bin/kinit",
      "/etc/security/keytabs/hdfs keytab",
      "hdfs/host@REALM;$(id)",
    )

  def test_jar_executor_passes_private_cache_to_hadoop_process(self):
    environment = SimpleNamespace(config={"hdfs_files": [{"target": "/apps"}]})
    cache = MagicMock(environment={"KRB5CCNAME": "FILE:/tmp/private/krb5cc"})
    cache_context = MagicMock()
    cache_context.__enter__.return_value = cache
    main_resource = MagicMock()
    main_resource.resource = SimpleNamespace(
      user="hdfs",
      hadoop_bin_dir="/usr/bin",
      hadoop_conf_dir="/etc/hadoop/conf",
      logoutput=True,
    )
    main_resource.kerberos_cache.return_value = cache_context

    with patch.object(
        hdfs_resource.Environment, "get_instance", return_value=environment
      ), \
      patch.object(hdfs_resource.time, "time", return_value=123.0), \
      patch.object(hdfs_resource, "File"), \
      patch.object(hdfs_resource, "Execute") as execute:
      hdfs_resource.HdfsResourceJar().action_execute(main_resource)

    main_resource.kinit.assert_called_once_with(cache)
    execute.assert_called_once_with(
      (
        "hadoop",
        "--config",
        "/etc/hadoop/conf",
        "jar",
        hdfs_resource.JAR_PATH,
        "/var/lib/ambari-agent/tmp/hdfs_resources_123.0.json",
      ),
      user="hdfs",
      path=["/usr/bin"],
      logoutput=True,
      sudo=False,
      environment={"KRB5CCNAME": "FILE:/tmp/private/krb5cc"},
    )

  def test_webhdfs_curl_receives_private_cache_environment(self):
    util = object.__new__(hdfs_resource.WebHDFSUtil)
    util.address = "https://namenode:9871"
    util.run_user = "hdfs"
    util.security_enabled = True
    util.logoutput = True
    util.environment = {"KRB5CCNAME": "FILE:/tmp/private/krb5cc"}
    with patch.object(
      hdfs_resource,
      "get_user_call_output",
      return_value=(0, "{}200", ""),
    ) as call_output:
      self.assertEqual(
        {},
        util._run_command("/apps", "GETFILESTATUS", method="GET"),
      )

    call_output.assert_called_once()
    self.assertEqual(
      {"KRB5CCNAME": "FILE:/tmp/private/krb5cc"},
      call_output.call_args.kwargs["env"],
    )


if __name__ == "__main__":
  unittest.main()

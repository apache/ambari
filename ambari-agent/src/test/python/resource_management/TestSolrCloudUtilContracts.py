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

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
"""

from unittest import TestCase
from unittest.mock import MagicMock, patch

from resource_management.core.exceptions import ExecutionFailed, Fail
from resource_management.libraries.functions import solr_cloud_util


class TestSolrCloudUtilContracts(TestCase):
  def test_znode_commands_use_structured_argv(self):
    with patch.object(solr_cloud_util, "Execute") as execute:
      solr_cloud_util.create_znode(
        "zk1.example:2181,zk2.example:2181",
        "/infra-solr",
        "/usr/lib/jvm/java-17",
        retry=3,
        interval=2,
      )

    command = execute.call_args.args[0]
    self.assertIsInstance(command, tuple)
    self.assertEqual("ambari-sudo.sh", command[0])
    self.assertIn("--create-znode", command)
    self.assertEqual("3", command[command.index("--retry") + 1])

  def test_client_setup_does_not_rewrite_packaged_cli(self):
    with (
      patch.object(solr_cloud_util, "Directory") as directory,
      patch.object(solr_cloud_util, "File") as file_resource,
    ):
      solr_cloud_util.setup_solr_client(
        {},
        custom_log4j=False,
        user="infra-solr",
        group="hadoop",
      )

    directory.assert_called_once_with(
      "/var/log/ambari-infra-solr-client",
      mode=0o750,
      create_parents=True,
      owner="infra-solr",
      group="hadoop",
    )
    managed_paths = [call.args[0] for call in file_resource.call_args_list]
    self.assertNotIn(
      "/usr/lib/ambari-infra-solr-client/solrCloudCli.sh", managed_paths
    )
    self.assertEqual(
      0o640,
      file_resource.call_args_list[-1].kwargs["mode"],
    )

  def test_upload_failure_always_removes_temporary_config(self):
    with (
      patch.object(
        solr_cloud_util,
        "Execute",
        side_effect=(
          ExecutionFailed("missing", 1, ""),
          ExecutionFailed("upload", 1, ""),
        ),
      ),
      patch.object(solr_cloud_util, "Directory") as directory,
    ):
      with self.assertRaises(ExecutionFailed):
        solr_cloud_util.upload_configuration_to_zk(
          "zk1.example:2181",
          "/infra-solr",
          "ranger_audits",
          "/etc/ranger/audit-solr",
          "/var/lib/ambari-agent/tmp",
          "/usr/lib/jvm/java-17",
        )

    directory.assert_called_once()
    self.assertEqual("delete", directory.call_args.kwargs["action"])

  def test_role_update_uses_private_cache_and_verified_tls(self):
    config = {
      "agentLevelParams": {"hostname": "solr1.example"},
      "clusterHostInfo": {"infra_solr_hosts": ["solr1.example"]},
      "configurations": {
        "cluster-env": {"security_enabled": True, "user_group": "hadoop"},
        "infra-solr-env": {
          "infra_solr_ssl_enabled": True,
          "infra_solr_port": "8886",
          "infra_solr_user": "infra-solr",
          "infra_solr_kerberos_keytab": "/etc/security/keytabs/infra-solr.keytab",
          "infra_solr_kerberos_principal": "infra-solr/_HOST@EXAMPLE.COM",
        },
        "kerberos-env": {"realm": "EXAMPLE.COM"},
      },
    }
    cache = MagicMock()
    cache.environment = {"KRB5CCNAME": "FILE:/private/cache"}
    cache_context = MagicMock()
    cache_context.__enter__.return_value = cache

    with (
      patch.object(solr_cloud_util, "get_kinit_path", return_value="/usr/bin/kinit"),
      patch.object(solr_cloud_util.Script, "get_tmp_dir", return_value="/agent/tmp"),
      patch.object(
        solr_cloud_util,
        "PrivateKerberosCache",
        return_value=cache_context,
      ) as private_cache,
      patch.object(solr_cloud_util, "Execute") as execute,
    ):
      solr_cloud_util.add_solr_roles(
        config,
        roles=["ranger_audit_user"],
        new_service_principals=["hdfs/_HOST@EXAMPLE.COM"],
      )

    private_cache.assert_called_once_with(
      "infra-solr", "hadoop", "/agent/tmp", "ambari-infra-solr-role-"
    )
    cache.kinit.assert_called_once_with(
      "/usr/bin/kinit",
      "/etc/security/keytabs/infra-solr.keytab",
      "infra-solr/solr1.example@EXAMPLE.COM",
    )
    command = execute.call_args.args[0]
    self.assertIsInstance(command, tuple)
    self.assertIn("--fail", command)
    self.assertNotIn("-k", command)
    self.assertEqual(cache.environment, execute.call_args.kwargs["environment"])

  def test_managed_paths_and_options_fail_before_execute(self):
    with patch.object(solr_cloud_util, "Execute") as execute:
      with self.assertRaises(Fail):
        solr_cloud_util.create_znode(
          "zk1.example:2181",
          "/infra-solr",
          "/",
        )
    execute.assert_not_called()

  def test_solr_json_post_uses_private_cache_and_propagates_failure(self):
    cache = MagicMock()
    cache.environment = {"KRB5CCNAME": "FILE:/private/api-cache"}
    cache_context = MagicMock()
    cache_context.__enter__.return_value = cache
    failure = ExecutionFailed("schema update failed", 22, "")
    with (
      patch.object(solr_cloud_util.Script, "get_tmp_dir", return_value="/agent/tmp"),
      patch.object(
        solr_cloud_util,
        "PrivateKerberosCache",
        return_value=cache_context,
      ) as private_cache,
      patch.object(solr_cloud_util, "Execute", side_effect=failure) as execute,
      self.assertRaises(ExecutionFailed),
    ):
      solr_cloud_util.post_json_to_solr(
        host="solr1.example",
        port=8886,
        collection="ranger_audits",
        endpoint="schema",
        payload={"add-field": {"name": "zoneName"}},
        user="ranger",
        group="hadoop",
        use_ssl=True,
        security_enabled=True,
        kinit_path="/usr/bin/kinit",
        keytab="/etc/security/keytabs/ranger.keytab",
        principal="rangeradmin/solr1.example@EXAMPLE.COM",
      )

    private_cache.assert_called_once_with(
      "ranger", "hadoop", "/agent/tmp", "ambari-solr-api-"
    )
    cache.kinit.assert_called_once()
    command = execute.call_args.args[0]
    self.assertIsInstance(command, tuple)
    self.assertIn("--fail", command)
    self.assertNotIn("-k", command)
    self.assertEqual(cache.environment, execute.call_args.kwargs["environment"])

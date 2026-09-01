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

import kerberos_utils
from resource_management.core.exceptions import Fail
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.script.script import Script


config = Script.get_config()
configurations = config.get("configurations", {})
cluster_env = configurations.get("cluster-env", {})
kerberos_env = configurations.get("kerberos-env", {})
krb5_conf = configurations.get("krb5-conf", {})
command_params = config.get("commandParams", {})

cluster_level_params = config.get("clusterLevelParams", {})
stack_name = cluster_level_params.get("stack_name")
stack_version = cluster_level_params.get("stack_version")
kerberos_utils.validate_bigtop_stack(stack_name, stack_version)

hostname = kerberos_utils.validate_host(
  config["agentLevelParams"]["hostname"], "Ambari agent host"
)
default_group = cluster_env.get("user_group") or cluster_env.get("user-group")

manage_identities = kerberos_utils.as_bool(
  kerberos_env.get("manage_identities", True),
  "kerberos-env/manage_identities",
)
manage_krb5_conf = kerberos_utils.as_bool(
  krb5_conf.get("manage_krb5_conf", True),
  "krb5-conf/manage_krb5_conf",
)
force_tcp = kerberos_utils.as_bool(
  krb5_conf.get("force_tcp", False),
  "krb5-conf/force_tcp",
)

realm = kerberos_utils.validate_realm(
  krb5_conf.get("realm", kerberos_env.get("realm"))
)
domains = kerberos_utils.validate_domains(krb5_conf.get("domains", ""))
kdc_hosts = kerberos_utils.validate_endpoints(
  kerberos_env.get("kdc_hosts"), "kerberos-env/kdc_hosts", required=True
)
master_kdc = kerberos_utils.validate_endpoint(
  kerberos_env.get("master_kdc"), "kerberos-env/master_kdc"
)
admin_server_host = kerberos_utils.validate_endpoint(
  kerberos_env.get("admin_server_host"),
  "kerberos-env/admin_server_host",
)
if not admin_server_host:
  admin_server_host = kerberos_utils.endpoint_without_port(
    kdc_hosts.split(",", 1)[0], "kerberos-env/kdc_hosts"
  )

krb5_conf_dir = str(krb5_conf.get("conf_dir", "/etc"))
krb5_conf_file = "krb5.conf"
krb5_conf_path = os.path.join(krb5_conf_dir, krb5_conf_file)
krb5_conf_template = krb5_conf.get("content")
if manage_krb5_conf and (
  not isinstance(krb5_conf_template, str) or not krb5_conf_template.strip()
):
  raise Fail("krb5-conf/content must contain the managed krb5.conf template")

smoke_user = cluster_env.get("smokeuser", "ambari-qa")
smoke_test_principal = cluster_env.get("smokeuser_principal_name")
smoke_test_keytab_file = cluster_env.get("smokeuser_keytab")
if manage_identities:
  smoke_test_principal = command_params.get(
    "principal_name", smoke_test_principal
  )
  smoke_test_keytab_file = command_params.get(
    "keytab_file", smoke_test_keytab_file
  )

kerberos_command_params = config.get("kerberosCommandParams")
kinit_path_local = get_kinit_path(
  kerberos_env.get("executable_search_paths")
)
service_check_timeout = 60

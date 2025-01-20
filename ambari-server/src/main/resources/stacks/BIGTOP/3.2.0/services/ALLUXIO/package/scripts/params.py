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

import socket
import os
from urllib.parse import urlparse

from ambari_commons.constants import AMBARI_SUDO_BINARY
from resource_management import *
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.constants import StackFeature
from resource_management.libraries.functions import conf_select, stack_select
from resource_management.libraries.functions.version import (
  format_stack_version,
  get_major_version,
)
from resource_management.libraries.functions.copy_tarball import (
  get_sysprep_skip_copy_tarballs_hdfs,
)
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.functions.get_not_managed_resources import (
  get_not_managed_resources,
)
from resource_management.libraries.resources.hdfs_resource import HdfsResource
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions.copy_tarball import get_current_version
from resource_management.libraries.functions.stack_features import (
  check_stack_feature,
  get_stack_feature_version,
)
from resource_management.libraries.functions import StackFeature


config = Script.get_config()

java_home = config["ambariLevelParams"]["java_home"]
stack_root = Script.get_stack_root()

config = Script.get_config()
tmp_dir = Script.get_tmp_dir()
sudo = AMBARI_SUDO_BINARY
fqdn = socket.getfqdn().lower()

retryAble = default("/commandParams/command_retry_enabled", False)

cluster_name = config["clusterName"]
stack_name = default("/clusterLevelParams/stack_name", None)
stack_root = Script.get_stack_root()
# 3.2
stack_version_unformatted = config["clusterLevelParams"]["stack_version"]
# 3.2.0.0
stack_version_formatted = format_stack_version(stack_version_unformatted)
# 3.2
major_stack_version = get_major_version(stack_version_formatted)

# 3.2.1.0-001
effective_version = get_current_version(service="ALLUXIO")

sysprep_skip_copy_tarballs_hdfs = get_sysprep_skip_copy_tarballs_hdfs()

# New Cluster Stack Version that is defined during the RESTART of a Stack Upgrade
version = default("/commandParams/version", None)

hadoop_conf_dir = conf_select.get_hadoop_conf_dir()
hadoop_bin_dir = stack_select.get_hadoop_dir("bin")

if stack_version_formatted and check_stack_feature(
  StackFeature.ROLLING_UPGRADE, stack_version_formatted
):
  hadoop_home = stack_select.get_hadoop_dir("home")

hdfs_user = config["configurations"]["hadoop-env"]["hdfs_user"]
hdfs_principal_name = config["configurations"]["hadoop-env"]["hdfs_principal_name"]
hdfs_user_keytab = config["configurations"]["hadoop-env"]["hdfs_user_keytab"]
user_group = config["configurations"]["cluster-env"]["user_group"]


component_directory = "alluxio"
alluxio_home = format("{stack_root}/current/{component_directory}")
alluxio_conf_dir = format("{stack_root}/current/{component_directory}/conf")

alluxio_user = config["configurations"]["alluxio-env"]["alluxio_user"]
alluxio_group = config["configurations"]["alluxio-env"]["alluxio_group"]
alluxio_pid_dir = config["configurations"]["alluxio-env"]["alluxio_pid_dir"]
alluxio_log_dir = config["configurations"]["alluxio-env"]["alluxio_log_dir"]
alluxio_work_dir = format("{alluxio_pid_dir}/work")

alluxio_journal_dir = format("{stack_root}/current/{component_directory}/journal")

host_name = config["agentLevelParams"]["hostname"]

# alluxio masters address
alluxio_masters = config["clusterHostInfo"]["alluxio_master_hosts"]
alluxio_masters_str = "\n".join(alluxio_masters)
alluxio_master_host = host_name

masters_journal_port = "19200"
master_embedded_journal_addresses = ""
master_embedded_journal_addresses_config = ""
# get comma separated lists of masters_journal_host hosts from alluxio_masters
if len(alluxio_masters) > 1:
  alluxio_master_host = host_name
  index = 0
  for host in alluxio_masters:
    masters_journal_host = host
    if masters_journal_port is not None:
      masters_journal_host = host + ":" + str(masters_journal_port)

    master_embedded_journal_addresses += masters_journal_host
    index += 1
    if index < len(alluxio_masters):
      master_embedded_journal_addresses += ","
  master_embedded_journal_addresses_config = (
    "alluxio.master.embedded.journal.addresses=" + master_embedded_journal_addresses
  )
elif len(alluxio_masters) == 1:
  alluxio_master_host = alluxio_masters[0]
print(alluxio_master_host)
print(master_embedded_journal_addresses_config)


# alluxio.underfs.address
alluxio_master_metastore_dir = config["configurations"]["alluxio-site-properties"][
  "alluxio.master.metastore.dir"
]
if alluxio_master_metastore_dir is None:
  alluxio_master_metastore_dir = "/usr/hdp/current/alluxio/metastore"
alluxio_master_metastore_formatted = False
if (
  os.path.exists(alluxio_master_metastore_dir)
  and os.path.isdir(alluxio_master_metastore_dir)
  and len(os.listdir(alluxio_master_metastore_dir)) > 0
):
  alluxio_master_metastore_formatted = True


alluxio_master_rpc_port = config["configurations"]["alluxio-site-properties"][
  "alluxio.master.rpc.port"
]
alluxio_master_web_port = config["configurations"]["alluxio-site-properties"][
  "alluxio.master.web.port"
]

alluxio_worker_rpc_port = config["configurations"]["alluxio-site-properties"][
  "alluxio.worker.rpc.port"
]
alluxio_worker_web_port = config["configurations"]["alluxio-site-properties"][
  "alluxio.worker.web.port"
]

# alluxio workers address
alluxio_workers = config["clusterHostInfo"]["alluxio_worker_hosts"]
alluxio_workers_str = "\n".join(alluxio_workers)

# alluxio worker memory alotment
worker_mem = config["configurations"]["alluxio-site-properties"][
  "alluxio.worker.memory"
]

# Find current stack and version to push agent files to
stack_name = default("/hostLevelParams/stack_name", None)
stack_version = config["hostLevelParams"]["stack_version"]

# hadoop params
namenode_address = None
if "dfs.namenode.rpc-address" in config["configurations"]["hdfs-site"]:
  namenode_rpcaddress = config["configurations"]["hdfs-site"][
    "dfs.namenode.rpc-address"
  ]
  namenode_address = format("hdfs://{namenode_rpcaddress}")
else:
  namenode_address = config["configurations"]["core-site"]["fs.defaultFS"]

# alluxio underfs address
underfs_hdfs_addr = (
  namenode_address
  + config["configurations"]["alluxio-site-properties"]["alluxio.underfs.hdfs.address"]
)


# alluxio hdd dirs


alluxio_site_properties = config["configurations"]["alluxio-site-properties"]["content"]
alluxio_env_sh = config["configurations"]["alluxio-env"]["content"]

alluxio_log4j2_properties = config["configurations"]["alluxio-log4j-properties"][
  "content"
]

alluxio_hdfs_user_dir = format("/user/{alluxio_user}")

smoke_user = config["configurations"]["cluster-env"]["smokeuser"]

alluxio_authentication = "SIMPLE"

# security_enabled
security_enabled = default("/configurations/cluster-env/security_enabled", None)
kinit_path_local = get_kinit_path(
  default("/configurations/kerberos-env/executable_search_paths", None)
)

if security_enabled:
  alluxio_authentication = "KERBEROS"
  alluxio_kerberos_keytab = config["configurations"]["alluxio-env"]["alluxio_keytab"]
  alluxio_kerberos_principal = config["configurations"]["alluxio-env"][
    "alluxio_principal"
  ]
  smoke_user_keytab = config["configurations"]["cluster-env"]["smokeuser_keytab"]
  smokeuser_principal = config["configurations"]["cluster-env"][
    "smokeuser_principal_name"
  ]
  alluxio_service_kerberos_keytab = config["configurations"]["alluxio-env"][
    "alluxio_service_keytab"
  ]
  alluxio_serive_kerberos_principal = config["configurations"]["alluxio-env"][
    "alluxio_service_principal"
  ]


# for create_hdfs_directory
default_fs = config["configurations"]["core-site"]["fs.defaultFS"]
hdfs_site = config["configurations"]["hdfs-site"]
hdfs_resource_ignore_file = "/var/lib/ambari-agent/data/.hdfs_resource_ignore"

dfs_type = default("/clusterLevelParams/dfs_type", "")

import functools

# create partial functions with common arguments for every HdfsResource call
# to create/delete hdfs directory/file/copyfromlocal we need to call params.HdfsResource in code
HdfsResource = functools.partial(
  HdfsResource,
  user=hdfs_user,
  hdfs_resource_ignore_file=hdfs_resource_ignore_file,
  security_enabled=security_enabled,
  keytab=hdfs_user_keytab,
  kinit_path_local=kinit_path_local,
  hadoop_bin_dir=hadoop_bin_dir,
  hadoop_conf_dir=hadoop_conf_dir,
  principal_name=hdfs_principal_name,
  hdfs_site=hdfs_site,
  default_fs=default_fs,
  immutable_paths=get_not_managed_resources(),
  dfs_type=dfs_type,
)


# command
alluxio_master_pid_file = format("{alluxio_pid_dir}/{alluxio_user}-master.pid")
alluxio_master_pid_cmd = (
  "echo `ps -A -o pid,command | grep -i \"[j]ava\" | grep alluxio.master.AlluxioMaster | awk '{print $1; exit}'`> "
  + alluxio_master_pid_file
)
alluxio_master_start_cmd = format(
  "rm -fr {alluxio_master_pid_file} && {alluxio_home}/bin/alluxio-start.sh -a master"
)
alluxio_master_stop_cmd = format(
  "{alluxio_home}/bin/alluxio-stop.sh master && rm -fr {alluxio_master_pid_file}"
)
alluxio_master_format = format("{alluxio_home}/bin/alluxio formatMaster")

alluxio_worker_pid_file = format("{alluxio_pid_dir}/{alluxio_user}-worker.pid")
alluxio_worker_pid_cmd = (
  "echo `ps -A -o pid,command | grep -i \"[j]ava\" | grep alluxio.worker.AlluxioWorker | awk '{print $1; exit}'`> "
  + alluxio_worker_pid_file
)
alluxio_worker_start_cmd = format(
  "rm -fr {alluxio_worker_pid_file} && {alluxio_home}/bin/alluxio-start.sh worker"
)
alluxio_worker_stop_cmd = format(
  "{alluxio_home}/bin/alluxio-stop.sh worker  && rm -fr {alluxio_worker_pid_file}"
)

sudo = AMBARI_SUDO_BINARY
alluxio_worker_mount_cmd = format("{alluxio_home}/bin/alluxio-mount.sh Mount")

alluxio_test_cmd = format("{alluxio_home}/bin/alluxio runTests")

if security_enabled:
  kinit_principal = alluxio_serive_kerberos_principal.replace("_HOST", host_name)
  alluxio_kinit_cmd = format(
    "kinit -kt {alluxio_service_kerberos_keytab} {kinit_principal}"
  )
  alluxio_master_start_cmd = alluxio_kinit_cmd + " && " + alluxio_master_start_cmd
  alluxio_worker_start_cmd = alluxio_kinit_cmd + " && " + alluxio_worker_start_cmd

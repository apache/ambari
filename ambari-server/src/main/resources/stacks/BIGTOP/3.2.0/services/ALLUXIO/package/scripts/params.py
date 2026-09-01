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

import functools
import os

from resource_management.core.shell import quote_bash_args
from resource_management.libraries.functions import (
  conf_select,
  get_kinit_path,
  stack_select,
)
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.get_not_managed_resources import (
  get_not_managed_resources,
)
from resource_management.libraries.resources.hdfs_resource import HdfsResource
from resource_management.libraries.script.script import Script

from alluxio_utils import resolve_master_metastore_dir


config = Script.get_config()

java_home = config["ambariLevelParams"]["java_home"]
stack_root = Script.get_stack_root()

hadoop_conf_dir = conf_select.get_hadoop_conf_dir()
hadoop_bin_dir = stack_select.get_hadoop_dir("bin")

hadoop_home = stack_select.get_hadoop_dir("home")

hdfs_user = config["configurations"]["hadoop-env"]["hdfs_user"]
hdfs_principal_name = config["configurations"]["hadoop-env"]["hdfs_principal_name"]
hdfs_user_keytab = config["configurations"]["hadoop-env"]["hdfs_user_keytab"]


component_directory = "alluxio"
alluxio_home = format("{stack_root}/current/{component_directory}")
alluxio_conf_dir = format("{stack_root}/current/{component_directory}/conf")
alluxio_data_dir = "/var/lib/alluxio"

alluxio_user = config["configurations"]["alluxio-env"]["alluxio_user"]
alluxio_group = config["configurations"]["alluxio-env"]["alluxio_group"]
alluxio_pid_dir = config["configurations"]["alluxio-env"]["alluxio_pid_dir"]
alluxio_log_dir = config["configurations"]["alluxio-env"]["alluxio_log_dir"]

alluxio_journal_dir = os.path.join(alluxio_data_dir, "journal")

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


# alluxio.underfs.address
alluxio_master_metastore_dir = resolve_master_metastore_dir(
  config["configurations"].get("alluxio-site-properties", {}), alluxio_data_dir
)

java_home_shell = quote_bash_args(str(java_home))
alluxio_native_library_option_shell = quote_bash_args(
  "-Djava.library.path=" + os.path.join(hadoop_home, "lib", "native")
)


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
  alluxio_service_kerberos_keytab = config["configurations"]["alluxio-env"][
    "alluxio_service_keytab"
  ]
  alluxio_service_kerberos_principal = config["configurations"]["alluxio-env"][
    "alluxio_service_principal"
  ]


# for create_hdfs_directory
default_fs = config["configurations"]["core-site"]["fs.defaultFS"]
hdfs_site = config["configurations"]["hdfs-site"]
hdfs_resource_ignore_file = "/var/lib/ambari-agent/data/.hdfs_resource_ignore"

dfs_type = default("/clusterLevelParams/dfs_type", "")

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
alluxio_master_process_class = "alluxio.master.AlluxioMaster"
alluxio_master_pid_file = format("{alluxio_pid_dir}/{alluxio_user}-master.pid")
alluxio_master_start_cmd = (
  os.path.join(alluxio_home, "bin", "alluxio-start.sh"),
  "-a",
  "-N",
  "master",
)

alluxio_worker_process_class = "alluxio.worker.AlluxioWorker"
alluxio_worker_pid_file = format("{alluxio_pid_dir}/{alluxio_user}-worker.pid")
alluxio_worker_start_cmd = (
  os.path.join(alluxio_home, "bin", "alluxio-start.sh"),
  "-a",
  "-N",
  "worker",
  "NoMount",
)

alluxio_worker_mount_cmd = (
  os.path.join(alluxio_home, "bin", "alluxio-mount.sh"),
  "Mount",
)

alluxio_test_cmd = (os.path.join(alluxio_home, "bin", "alluxio"), "runTests")

if security_enabled:
  kinit_principal = alluxio_service_kerberos_principal.replace("_HOST", host_name)

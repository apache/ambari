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

from resource_management import *


def service(action=None, name=None, user=None, create_pid_dir=False,
            create_log_dir=False, keytab=None, principal=None):
  import params

  kinit_cmd = "true"
  pid_dir = format("{hadoop_pid_dir_prefix}/{user}")
  pid_file = format("{pid_dir}/hadoop-{user}-{name}.pid")
  log_dir = format("{hdfs_log_dir_prefix}/{user}")
  hadoop_daemon = format(
    "{ulimit_cmd} export HADOOP_LIBEXEC_DIR={hadoop_libexec_dir} && "
    "{hadoop_bin}/hadoop-daemon.sh")
  cmd = format("{hadoop_daemon} --config {hadoop_conf_dir}")

  if create_pid_dir:
    Directory(pid_dir,
              owner=user,
              recursive=True)
  if create_log_dir:
    Directory(log_dir,
              owner=user,
              recursive=True)

  if params.security_enabled and name != "zkfc":
    principal_replaced = principal.replace("_HOST", params.hostname)
    kinit_cmd = format("kinit -kt {keytab} {principal_replaced}")

    if name == "datanode":
      user = "root"
      pid_file = format(
        "{hadoop_pid_dir_prefix}/{hdfs_user}/hadoop-{hdfs_user}-{name}.pid")

  daemon_cmd = format("{cmd} {action} {name}")

  service_is_up = format(
    "ls {pid_file} >/dev/null 2>&1 &&"
    " ps `cat {pid_file}` >/dev/null 2>&1") if action == "start" else None

  Execute(kinit_cmd)
  Execute(daemon_cmd,
          user = user,
          not_if=service_is_up
  )
  if action == "stop":
    File(pid_file,
         action="delete",
         ignore_failures=True
    )


def hdfs_directory(name=None, owner=None, group=None,
                   mode=None, recursive_chown=False, recursive_chmod=False):
  import params

  dir_exists = format("hadoop fs -ls {name} >/dev/null 2>&1")
  namenode_safe_mode_off = "hadoop dfsadmin -safemode get|grep 'Safe mode is OFF'"

  stub_dir = params.namenode_dirs_created_stub_dir
  stub_filename = params.namenode_dirs_stub_filename
  dir_absent_in_stub = format(
    "grep -q '^{name}$' {stub_dir}/{stub_filename} > /dev/null 2>&1; test $? -ne 0")
  record_dir_in_stub = format("echo '{name}' >> {stub_dir}/{stub_filename}")
  tries = 30
  try_sleep = 10
  dfs_check_nn_status_cmd = "true"

  if params.dfs_ha_enabled:
    namenode_id = params.namenode_id
    dfs_check_nn_status_cmd = format(
      "hdfs haadmin -getServiceState $namenode_id | grep active > /dev/null")

  #if params.stack_version[0] == "2":
  mkdir_cmd = format("fs -mkdir -p {name}")
  #else:
  #  mkdir_cmd = format("fs -mkdir {name}")

  if params.security_enabled:
    Execute(format("kinit -kt {hdfs_user_keytab} {hdfs_user}"),
            user = params.hdfs_user)
  ExecuteHadoop(mkdir_cmd,
                try_sleep=try_sleep,
                tries=tries,
                not_if=format(
                  "! {dir_absent_in_stub} && {dfs_check_nn_status_cmd} && "
                  "{dir_exists} && ! {namenode_safe_mode_off}"),
                only_if=format(
                  "{dir_absent_in_stub} && {dfs_check_nn_status_cmd} && "
                  "! {dir_exists}"),
                conf_dir=params.hadoop_conf_dir,
                user=params.hdfs_user
  )
  Execute(record_dir_in_stub,
          user=params.hdfs_user,
          only_if=format("{dir_absent_in_stub}")
  )

  recursive = "-R" if recursive_chown else ""
  perm_cmds = []

  if owner:
    chown = owner
    if group:
      chown = format("{owner}:{group}")
    perm_cmds.append(format("fs -chown {recursive} {chown} {name}"))
  if mode:
    perm_cmds.append(format("fs -chmod {recursive} {mode} {name}"))
  for cmd in perm_cmds:
    ExecuteHadoop(cmd,
                  user=params.hdfs_user,
                  only_if=format("! {dir_absent_in_stub} && {dfs_check_nn_status_cmd} && {namenode_safe_mode_off} && {dir_exists}"),
                  try_sleep=try_sleep,
                  tries=tries,
                  conf_dir=params.hadoop_conf_dir
    )




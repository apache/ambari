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

import shlex

from resource_management.core import shell
from resource_management.core.exceptions import Fail
from resource_management.core.resources.system import Directory, Execute, File
from resource_management.core.source import Template
from resource_management.libraries.functions.check_process_status import (
  check_process_status,
)
from resource_management.libraries.script.script import Script


class VictoriaMetrics(Script):
  def install(self, env):
    self.install_packages(env)

  def configure(self, env):
    import params

    env.set_params(params)
    Directory(
      [
        params.victoriametrics_pid_dir,
        params.victoriametrics_log_dir,
        params.victoriametrics_config_dir,
      ]
      + params.data_dirs,
      owner=params.victoriametrics_user,
      group=params.victoriametrics_group,
      mode=0o750,
      create_parents=True,
    )

    if params.role == "VMAGENT":
      File(
        params.promscrape_config_file,
        owner=params.victoriametrics_user,
        group=params.victoriametrics_group,
        mode=0o600,
        content=Template("promscrape.yml.j2"),
      )
      File(
        params.ambari_sd_password_file,
        owner=params.victoriametrics_user,
        group=params.victoriametrics_group,
        mode=0o600,
        content=params.ambari_sd_password,
      )
      if params.require_authentication and params.remote_write_uses_vmauth:
        File(
          params.remote_write_password_file,
          owner=params.victoriametrics_user,
          group=params.victoriametrics_group,
          mode=0o600,
          content=params.api_password,
        )
      else:
        File(params.remote_write_password_file, action="delete")

    if params.role == "VMAUTH":
      File(
        params.vmauth_config_file,
        owner=params.victoriametrics_user,
        group=params.victoriametrics_group,
        mode=0o600,
        content=Template("vmauth.yml.j2"),
      )

  def start(self, env):
    import params

    env.set_params(params)
    self.configure(env)
    self._validate_start(params)

    arguments = self._component_arguments(params)
    if params.component_extra_args:
      arguments.extend(shlex.split(params.component_extra_args))

    Execute(
      shell.string_cmd_from_args_list(["test", "-x", params.binary_path]),
      user=params.victoriametrics_user,
    )
    process_command = shell.string_cmd_from_args_list(
      [params.binary_path] + arguments
    )
    start_command = "nohup {0} > {1} 2>&1 < /dev/null & echo $! > {2}".format(
      process_command,
      shell.quote_bash_args(params.log_file),
      shell.quote_bash_args(params.pid_file),
    )
    Execute(
      start_command,
      user=params.victoriametrics_user,
      not_if=self._process_exists_command(params.pid_file),
    )

  def stop(self, env):
    import params

    env.set_params(params)
    process_exists = self._process_exists_command(params.pid_file)
    Execute(
      "kill $(cat {0})".format(shell.quote_bash_args(params.pid_file)),
      user=params.victoriametrics_user,
      only_if=process_exists,
    )
    Execute(
      "kill -9 $(cat {0})".format(shell.quote_bash_args(params.pid_file)),
      user=params.victoriametrics_user,
      only_if=process_exists,
      not_if="sleep 5; ! ({0})".format(process_exists),
      ignore_failures=True,
    )
    Execute(
      "! ({0})".format(process_exists),
      user=params.victoriametrics_user,
      tries=20,
      try_sleep=3,
    )
    File(params.pid_file, action="delete")

  def restart(self, env):
    self.stop(env)
    self.start(env)

  def status(self, env):
    import status_params

    env.set_params(status_params)
    check_process_status(status_params.pid_file)

  @staticmethod
  def _process_exists_command(pid_file):
    quoted_pid_file = shell.quote_bash_args(pid_file)
    return "test -f {0} && kill -0 $(cat {0}) >/dev/null 2>&1".format(
      quoted_pid_file
    )

  @staticmethod
  def _validate_start(params):
    if params.role in ("VMINSERT", "VMSELECT") and not params.vmstorage_hosts:
      raise Fail("{0} requires at least one VMSTORAGE host".format(params.role))
    if params.role == "VMAGENT":
      if not 1 <= params.vmagent_replication_factor <= 2:
        raise Fail(
          "VMAGENT scrape replication factor must be between 1 and 2"
        )
      if params.vmagent_replication_factor > params.vmagent_members_count:
        raise Fail(
          "VMAGENT scrape replication factor cannot exceed the VMAGENT member count"
        )
      if not params.ambari_sd_username or not params.ambari_sd_password:
        raise Fail("VMAGENT requires an Ambari HTTP SD identity")
      if not params.remote_write_url:
        raise Fail("VMAGENT cannot derive a remote-write URL from the topology")
      if params.require_authentication and params.remote_write_uses_vmauth and (
        not params.api_username or not params.api_password
      ):
        raise Fail("VMAUTH authentication requires both API user and password")
    if params.role == "VMAUTH":
      if params.require_authentication and (
        not params.api_username or not params.api_password
      ):
        raise Fail("VMAUTH authentication requires both API user and password")
      if params.deployment_mode == "single" and not params.server_hosts:
        raise Fail("VMAUTH requires a VICTORIAMETRICS_SERVER host in single mode")
      if params.deployment_mode == "cluster" and (
        not params.vminsert_hosts or not params.vmselect_hosts
      ):
        raise Fail("VMAUTH requires VMINSERT and VMSELECT hosts in cluster mode")

  @staticmethod
  def _component_arguments(params):
    arguments = [
      "-httpListenAddr=0.0.0.0:{0}".format(params.http_port),
      "-loggerOutput=stdout",
    ]
    if params.role == "VICTORIAMETRICS_SERVER":
      arguments.extend(
        [
          "-storageDataPath={0}".format(params.server_data_dir),
          "-retentionPeriod={0}".format(params.retention_period),
        ]
      )
      if params.vmagent_replication_factor > 1:
        arguments.append(
          "-dedup.minScrapeInterval={0}".format(
            params.effective_dedup_min_scrape_interval
          )
        )
    elif params.role == "VMSTORAGE":
      arguments.extend(
        [
          "-storageDataPath={0}".format(params.vmstorage_data_dir),
          "-retentionPeriod={0}".format(params.retention_period),
          "-vminsertAddr=0.0.0.0:{0}".format(params.vmstorage_vminsert_port),
          "-vmselectAddr=0.0.0.0:{0}".format(params.vmstorage_vmselect_port),
        ]
      )
      if params.deduplication_enabled:
        arguments.append(
          "-dedup.minScrapeInterval={0}".format(
            params.effective_dedup_min_scrape_interval
          )
        )
    elif params.role == "VMINSERT":
      storage_nodes = [
        "{0}:{1}".format(host, params.vmstorage_vminsert_port)
        for host in params.vmstorage_hosts
      ]
      arguments.extend(
        [
          "-storageNode={0}".format(",".join(storage_nodes)),
          "-replicationFactor={0}".format(params.replication_factor),
        ]
      )
    elif params.role == "VMSELECT":
      storage_nodes = [
        "{0}:{1}".format(host, params.vmstorage_vmselect_port)
        for host in params.vmstorage_hosts
      ]
      arguments.extend(
        [
          "-storageNode={0}".format(",".join(storage_nodes)),
          "-cacheDataPath={0}".format(params.vmselect_cache_dir),
          "-replicationFactor={0}".format(params.replication_factor),
        ]
      )
      if params.deduplication_enabled:
        arguments.append(
          "-dedup.minScrapeInterval={0}".format(
            params.effective_dedup_min_scrape_interval
          )
        )
    elif params.role == "VMAGENT":
      arguments.extend(
        [
          "-promscrape.config={0}".format(params.promscrape_config_file),
          "-promscrape.httpSDCheckInterval={0}".format(
            params.http_sd_refresh_interval
          ),
          "-promscrape.cluster.membersCount={0}".format(
            params.vmagent_members_count
          ),
          "-promscrape.cluster.memberNum={0}".format(params.vmagent_member_num),
          "-promscrape.cluster.replicationFactor={0}".format(
            params.vmagent_replication_factor
          ),
          "-remoteWrite.url={0}".format(params.remote_write_url),
          "-remoteWrite.tmpDataPath={0}".format(params.vmagent_data_dir),
          "-remoteWrite.maxDiskUsagePerURL={0}".format(
            params.remote_write_max_disk_usage
          ),
        ]
      )
      if params.require_authentication and params.remote_write_uses_vmauth:
        arguments.extend(
          [
            "-remoteWrite.basicAuth.username={0}".format(params.api_username),
            "-remoteWrite.basicAuth.passwordFile={0}".format(
              params.remote_write_password_file
            ),
          ]
        )
    elif params.role == "VMAUTH":
      arguments.append("-auth.config={0}".format(params.vmauth_config_file))
    return arguments


if __name__ == "__main__":
  VictoriaMetrics().execute()

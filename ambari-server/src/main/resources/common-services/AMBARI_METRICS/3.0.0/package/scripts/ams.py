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

from resource_management.core.exceptions import Fail
from resource_management.core.resources.system import Directory, Execute, File
from resource_management.libraries.resources.xml_config import XmlConfig
from resource_management.libraries.resources.template_config import TemplateConfig
from resource_management.core.source import InlineTemplate, StaticFile, Template
from resource_management.libraries.functions.format import format
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
import os


@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def ams(name=None, action=None):
  import params

  if name == "collector":
    Directory(
      params.ams_collector_conf_dir,
      owner=params.ams_user,
      group=params.user_group,
      create_parents=True,
    )

    Directory(
      params.ams_checkpoint_dir,
      owner=params.ams_user,
      group=params.user_group,
      create_parents=True,
      mode=0o750,
    )

    new_ams_site = {}
    new_ams_site.update(params.config["configurations"]["ams-site"])
    if params.clusterHostInfoDict:
      master_components = []
      slave_components = []
      components = dict(params.clusterHostInfoDict).keys()
      known_slave_components = [
        "nodemanager",
        "metrics_monitor",
        "datanode",
        "hbase_regionserver",
      ]
      for component in components:
        if component and component.endswith("_hosts"):
          component_name = component[:-6]
        elif component and component.endswith("_host"):
          component_name = component[:-5]
        else:
          continue
        if component_name in known_slave_components:
          slave_components.insert(0, component_name)
        else:
          master_components.insert(0, component_name)

      if slave_components:
        new_ams_site["timeline.metrics.initial.configured.slave.components"] = ",".join(
          slave_components
        )
      if master_components:
        if "ambari_server" not in master_components:
          master_components.insert(0, "ambari_server")
        master_components = sorted(master_components)
        new_ams_site["timeline.metrics.initial.configured.master.components"] = (
          ",".join(master_components)
        )

    hbase_total_heapsize_with_trailing_m = params.hbase_heapsize
    hbase_total_heapsize = int(hbase_total_heapsize_with_trailing_m[:-1]) * 1024 * 1024
    new_ams_site["hbase_total_heapsize"] = hbase_total_heapsize

    XmlConfig(
      "ams-site.xml",
      conf_dir=params.ams_collector_conf_dir,
      configurations=new_ams_site,
      configuration_attributes=params.config["configurationAttributes"]["ams-site"],
      owner=params.ams_user,
      group=params.user_group,
    )

    XmlConfig(
      "ssl-server.xml",
      conf_dir=params.ams_collector_conf_dir,
      configurations=params.config["configurations"]["ams-ssl-server"],
      configuration_attributes=params.config["configurationAttributes"][
        "ams-ssl-server"
      ],
      owner=params.ams_user,
      group=params.user_group,
    )

    merged_ams_hbase_site = {}
    merged_ams_hbase_site.update(params.config["configurations"]["ams-hbase-site"])
    if params.security_enabled:
      merged_ams_hbase_site.update(
        params.config["configurations"]["ams-hbase-security-site"]
      )

    # Add phoenix client side overrides
    merged_ams_hbase_site["phoenix.query.maxGlobalMemoryPercentage"] = str(
      params.phoenix_max_global_mem_percent
    )
    merged_ams_hbase_site["phoenix.spool.directory"] = params.phoenix_client_spool_dir

    XmlConfig(
      "hbase-site.xml",
      conf_dir=params.ams_collector_conf_dir,
      configurations=merged_ams_hbase_site,
      configuration_attributes=params.config["configurationAttributes"][
        "ams-hbase-site"
      ],
      owner=params.ams_user,
      group=params.user_group,
    )

    if params.security_enabled:
      TemplateConfig(
        os.path.join(params.hbase_conf_dir, "ams_collector_jaas.conf"),
        owner=params.ams_user,
        template_tag=None,
      )

    if params.log4j_props is not None:
      File(
        format("{params.ams_collector_conf_dir}/log4j.properties"),
        mode=0o644,
        group=params.user_group,
        owner=params.ams_user,
        content=InlineTemplate(params.log4j_props),
      )

    File(
      format("{ams_collector_conf_dir}/ams-env.sh"),
      owner=params.ams_user,
      content=InlineTemplate(params.ams_env_sh_template),
    )

    Directory(
      params.ams_collector_log_dir,
      owner=params.ams_user,
      group=params.user_group,
      create_parents=True,
      mode=0o750,
    )

    Directory(
      params.ams_collector_pid_dir,
      owner=params.ams_user,
      group=params.user_group,
      create_parents=True,
      mode=0o750,
    )

    # Hack to allow native HBase libs to be included for embedded hbase
    File(
      os.path.join(params.ams_hbase_home_dir, "bin", "hadoop"),
      owner=params.ams_user,
      mode=0o755,
    )

    # On some OS this folder could be not exists, so we will create it before pushing there files
    Directory(params.limits_conf_dir, create_parents=True, owner="root", group="root")

    # Setting up security limits
    File(
      os.path.join(params.limits_conf_dir, "ams.conf"),
      owner="root",
      group="root",
      mode=0o644,
      content=Template("ams.conf.j2"),
    )

    # Phoenix spool file dir if not /tmp
    if not os.path.exists(params.phoenix_client_spool_dir):
      Directory(
        params.phoenix_client_spool_dir,
        owner=params.ams_user,
        group=params.user_group,
        create_parents=True,
        mode=0o750,
      )

    if not params.is_local_fs_rootdir and params.is_ams_distributed:
      # Configuration needed to support NN HA
      XmlConfig(
        "hdfs-site.xml",
        conf_dir=params.ams_collector_conf_dir,
        configurations=params.config["configurations"]["hdfs-site"],
        configuration_attributes=params.config["configurationAttributes"]["hdfs-site"],
        owner=params.ams_user,
        group=params.user_group,
        mode=0o644,
      )

      XmlConfig(
        "hdfs-site.xml",
        conf_dir=params.hbase_conf_dir,
        configurations=params.config["configurations"]["hdfs-site"],
        configuration_attributes=params.config["configurationAttributes"]["hdfs-site"],
        owner=params.ams_user,
        group=params.user_group,
        mode=0o644,
      )

      truncated_core_site = {}
      truncated_core_site.update(params.config["configurations"]["core-site"])
      if is_spnego_enabled(params):
        truncated_core_site.pop("hadoop.http.authentication.type", None)
        truncated_core_site.pop("hadoop.http.filter.initializers", None)

      XmlConfig(
        "core-site.xml",
        conf_dir=params.ams_collector_conf_dir,
        configurations=truncated_core_site,
        configuration_attributes=params.config["configurationAttributes"]["core-site"],
        owner=params.ams_user,
        group=params.user_group,
        mode=0o644,
      )

      XmlConfig(
        "core-site.xml",
        conf_dir=params.hbase_conf_dir,
        configurations=truncated_core_site,
        configuration_attributes=params.config["configurationAttributes"]["core-site"],
        owner=params.ams_user,
        group=params.user_group,
        mode=0o644,
      )

    if params.metric_collector_https_enabled:
      export_ca_certs(params.ams_collector_conf_dir)

  elif name == "monitor":
    Directory(
      params.ams_monitor_conf_dir,
      owner=params.ams_user,
      group=params.user_group,
      create_parents=True,
    )

    Directory(
      params.ams_monitor_log_dir,
      owner=params.ams_user,
      group=params.user_group,
      mode=0o750,
      create_parents=True,
    )

    if params.host_in_memory_aggregation and params.log4j_props is not None:
      File(
        format("{params.ams_monitor_conf_dir}/log4j.properties"),
        mode=0o644,
        group=params.user_group,
        owner=params.ams_user,
        content=InlineTemplate(params.log4j_props),
      )

      XmlConfig(
        "ams-site.xml",
        conf_dir=params.ams_monitor_conf_dir,
        configurations=params.config["configurations"]["ams-site"],
        configuration_attributes=params.config["configurationAttributes"]["ams-site"],
        owner=params.ams_user,
        group=params.user_group,
      )
      XmlConfig(
        "ssl-server.xml",
        conf_dir=params.ams_monitor_conf_dir,
        configurations=params.config["configurations"]["ams-ssl-server"],
        configuration_attributes=params.config["configurationAttributes"][
          "ams-ssl-server"
        ],
        owner=params.ams_user,
        group=params.user_group,
      )

    Directory(
      params.ams_monitor_pid_dir,
      owner=params.ams_user,
      group=params.user_group,
      mode=0o750,
      create_parents=True,
    )

    TemplateConfig(
      format("{ams_monitor_conf_dir}/metric_monitor.ini"),
      owner=params.ams_user,
      group=params.user_group,
      template_tag=None,
    )

    TemplateConfig(
      format("{ams_monitor_conf_dir}/metric_groups.conf"),
      owner=params.ams_user,
      group=params.user_group,
      template_tag=None,
    )

    File(
      format("{ams_monitor_conf_dir}/ams-env.sh"),
      owner=params.ams_user,
      content=InlineTemplate(params.ams_env_sh_template),
    )

    if params.metric_collector_https_enabled or params.is_aggregation_https_enabled:
      export_ca_certs(params.ams_monitor_conf_dir)

    # On some OS this folder could be not exists, so we will create it before pushing there files
    Directory(params.limits_conf_dir, create_parents=True, owner="root", group="root")

    # Setting up security limits
    File(
      os.path.join(params.limits_conf_dir, "ams.conf"),
      owner="root",
      group="root",
      mode=0o644,
      content=Template("ams.conf.j2"),
    )

  elif name == "grafana":
    ams_grafana_directories = (
      params.ams_grafana_conf_dir,
      params.ams_grafana_log_dir,
    )
    private_grafana_directories = (
      params.ams_grafana_data_dir,
      params.ams_grafana_pid_dir,
    )

    for ams_grafana_directory in ams_grafana_directories:
      Directory(
        ams_grafana_directory,
        owner=params.ams_user,
        group=params.user_group,
        mode=0o755,
        create_parents=True,
      )

    for ams_grafana_directory in private_grafana_directories:
      Directory(
        ams_grafana_directory,
        owner=params.ams_user,
        group=params.user_group,
        mode=0o750,
        create_parents=True,
      )

    File(
      format("{ams_grafana_conf_dir}/ams-grafana-env.sh"),
      owner=params.ams_user,
      group=params.user_group,
      content=InlineTemplate(params.ams_grafana_env_sh_template),
    )

    File(
      format("{ams_grafana_conf_dir}/ams-grafana.ini"),
      owner=params.ams_user,
      group=params.user_group,
      content=InlineTemplate(params.ams_grafana_ini_template),
      mode=0o600,
    )

    if params.metric_collector_https_enabled:
      export_ca_certs(params.ams_grafana_conf_dir)

  else:
    raise Fail(f"Unsupported Ambari Metrics component: {name}")


def is_spnego_enabled(params):
  if (
    "core-site" in params.config["configurations"]
    and "hadoop.http.authentication.type"
    in params.config["configurations"]["core-site"]
    and params.config["configurations"]["core-site"]["hadoop.http.authentication.type"]
    == "kerberos"
    and "hadoop.http.filter.initializers"
    in params.config["configurations"]["core-site"]
    and "org.apache.hadoop.security.AuthenticationFilterInitializer"
    in params.config["configurations"]["core-site"]["hadoop.http.filter.initializers"]
  ):
    return True
  return False


def export_ca_certs(dir_path):
  import params
  import tempfile

  ca_certs_path = os.path.join(dir_path, params.metric_truststore_ca_certs)
  truststore = params.metric_truststore_path
  if not os.path.isfile(truststore) or os.path.islink(truststore):
    raise Fail("Metrics truststore must be a regular file and must not be a symlink")

  tmpdir = tempfile.mkdtemp(prefix="ams-truststore-")
  truststore_p12 = os.path.join(tmpdir, "truststore.p12")
  exported_certs = os.path.join(tmpdir, "ca.pem")
  secret_environment = {"AMS_TRUSTSTORE_PASSWORD": params.metric_truststore_password}

  try:
    if params.metric_truststore_type.lower() == "jks":
      Execute(
        (
          os.path.join(params.java64_home, "bin", "keytool"),
          "-importkeystore",
          "-noprompt",
          "-srckeystore",
          truststore,
          "-destkeystore",
          truststore_p12,
          "-deststoretype",
          "PKCS12",
          "-srcstorepass:env",
          "AMS_TRUSTSTORE_PASSWORD",
          "-deststorepass:env",
          "AMS_TRUSTSTORE_PASSWORD",
        ),
        sudo=True,
        environment=secret_environment,
        timeout=60,
      )
      truststore = truststore_p12

    Execute(
      (
        "openssl",
        "pkcs12",
        "-in",
        truststore,
        "-out",
        exported_certs,
        "-cacerts",
        "-nokeys",
        "-passin",
        "env:AMS_TRUSTSTORE_PASSWORD",
      ),
      sudo=True,
      environment=secret_environment,
      timeout=60,
    )
    File(
      ca_certs_path,
      content=StaticFile(exported_certs),
      owner=params.ams_user,
      group=params.user_group,
      mode=0o644,
    )
  finally:
    Directory(tmpdir, action="delete")

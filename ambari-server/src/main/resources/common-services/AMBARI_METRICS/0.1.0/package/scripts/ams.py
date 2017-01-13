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

from resource_management.core.resources.system import Directory, Execute, File
from resource_management.libraries.resources.xml_config import XmlConfig
from resource_management.libraries.resources.template_config import TemplateConfig
from resource_management.core.resources.service import ServiceConfig
from resource_management.core.source import InlineTemplate, Template
from resource_management.libraries.functions.format import format
from ambari_commons import OSConst
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from ambari_commons.str_utils import compress_backslashes
import glob
import os

@OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
def ams(name=None):
  import params
  if name == 'collector':
    if not check_windows_service_exists(params.ams_collector_win_service_name):
      Execute(format("cmd /C cd {ams_collector_home_dir} & ambari-metrics-collector.cmd setup"))

    Directory(params.ams_collector_conf_dir,
              owner=params.ams_user,
              create_parents = True
    )

    Directory(params.ams_checkpoint_dir,
              owner=params.ams_user,
              create_parents = True
    )

    XmlConfig("ams-site.xml",
              conf_dir=params.ams_collector_conf_dir,
              configurations=params.config['configurations']['ams-site'],
              configuration_attributes=params.config['configuration_attributes']['ams-site'],
              owner=params.ams_user,
    )

    merged_ams_hbase_site = {}
    merged_ams_hbase_site.update(params.config['configurations']['ams-hbase-site'])
    if params.security_enabled:
      merged_ams_hbase_site.update(params.config['configurations']['ams-hbase-security-site'])

    XmlConfig( "hbase-site.xml",
               conf_dir = params.ams_collector_conf_dir,
               configurations = merged_ams_hbase_site,
               configuration_attributes=params.config['configuration_attributes']['ams-hbase-site'],
               owner = params.ams_user,
    )

    if (params.log4j_props != None):
      File(os.path.join(params.ams_collector_conf_dir, "log4j.properties"),
           owner=params.ams_user,
           content=params.log4j_props
      )

    File(os.path.join(params.ams_collector_conf_dir, "ams-env.cmd"),
         owner=params.ams_user,
         content=InlineTemplate(params.ams_env_sh_template)
    )

    ServiceConfig(params.ams_collector_win_service_name,
                  action="change_user",
                  username = params.ams_user,
                  password = Script.get_password(params.ams_user))

    if not params.is_local_fs_rootdir:
      # Configuration needed to support NN HA
      XmlConfig("hdfs-site.xml",
            conf_dir=params.ams_collector_conf_dir,
            configurations=params.config['configurations']['hdfs-site'],
            configuration_attributes=params.config['configuration_attributes']['hdfs-site'],
            owner=params.ams_user,
            group=params.user_group,
            mode=0644
      )

      XmlConfig("hdfs-site.xml",
            conf_dir=params.hbase_conf_dir,
            configurations=params.config['configurations']['hdfs-site'],
            configuration_attributes=params.config['configuration_attributes']['hdfs-site'],
            owner=params.ams_user,
            group=params.user_group,
            mode=0644
      )

      XmlConfig("core-site.xml",
                conf_dir=params.ams_collector_conf_dir,
                configurations=params.config['configurations']['core-site'],
                configuration_attributes=params.config['configuration_attributes']['core-site'],
                owner=params.ams_user,
                group=params.user_group,
                mode=0644
      )

      XmlConfig("core-site.xml",
                conf_dir=params.hbase_conf_dir,
                configurations=params.config['configurations']['core-site'],
                configuration_attributes=params.config['configuration_attributes']['core-site'],
                owner=params.ams_user,
                group=params.user_group,
                mode=0644
      )

    else:
      ServiceConfig(params.ams_embedded_hbase_win_service_name,
                    action="change_user",
                    username = params.ams_user,
                    password = Script.get_password(params.ams_user))
      # creating symbolic links on ams jars to make them available to services
      links_pairs = [
        ("%COLLECTOR_HOME%\\hbase\\lib\\ambari-metrics-hadoop-sink-with-common.jar",
         "%SINK_HOME%\\hadoop-sink\\ambari-metrics-hadoop-sink-with-common-*.jar"),
        ]
      for link_pair in links_pairs:
        link, target = link_pair
        real_link = os.path.expandvars(link)
        target = compress_backslashes(glob.glob(os.path.expandvars(target))[0])
        if not os.path.exists(real_link):
          #TODO check the symlink destination too. Broken in Python 2.x on Windows.
          Execute('cmd /c mklink "{0}" "{1}"'.format(real_link, target))
    pass

  elif name == 'monitor':
    if not check_windows_service_exists(params.ams_monitor_win_service_name):
      Execute(format("cmd /C cd {ams_monitor_home_dir} & ambari-metrics-monitor.cmd setup"))

    # creating symbolic links on ams jars to make them available to services
    links_pairs = [
      ("%HADOOP_HOME%\\share\\hadoop\\common\\lib\\ambari-metrics-hadoop-sink-with-common.jar",
       "%SINK_HOME%\\hadoop-sink\\ambari-metrics-hadoop-sink-with-common-*.jar"),
      ("%HBASE_HOME%\\lib\\ambari-metrics-hadoop-sink-with-common.jar",
       "%SINK_HOME%\\hadoop-sink\\ambari-metrics-hadoop-sink-with-common-*.jar"),
    ]
    for link_pair in links_pairs:
      link, target = link_pair
      real_link = os.path.expandvars(link)
      target = compress_backslashes(glob.glob(os.path.expandvars(target))[0])
      if not os.path.exists(real_link):
        #TODO check the symlink destination too. Broken in Python 2.x on Windows.
        Execute('cmd /c mklink "{0}" "{1}"'.format(real_link, target))

    Directory(params.ams_monitor_conf_dir,
              owner=params.ams_user,
              create_parents = True
    )

    TemplateConfig(
      os.path.join(params.ams_monitor_conf_dir, "metric_monitor.ini"),
      owner=params.ams_user,
      template_tag=None
    )

    TemplateConfig(
      os.path.join(params.ams_monitor_conf_dir, "metric_groups.conf"),
      owner=params.ams_user,
      template_tag=None
    )

    ServiceConfig(params.ams_monitor_win_service_name,
                  action="change_user",
                  username = params.ams_user,
                  password = Script.get_password(params.ams_user))


@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def ams(name=None, action=None):
  import params

  if name == 'collector':
    Directory(params.ams_collector_conf_dir,
              owner=params.ams_user,
              group=params.user_group,
              create_parents = True,
              recursive_ownership = True,
    )
    
    Directory(params.ams_checkpoint_dir,
              owner=params.ams_user,
              group=params.user_group,
              cd_access="a",
              create_parents = True,
              recursive_ownership = True
    )

    XmlConfig("ams-site.xml",
              conf_dir=params.ams_collector_conf_dir,
              configurations=params.config['configurations']['ams-site'],
              configuration_attributes=params.config['configuration_attributes']['ams-site'],
              owner=params.ams_user,
              group=params.user_group
    )

    XmlConfig("ssl-server.xml",
              conf_dir=params.ams_collector_conf_dir,
              configurations=params.config['configurations']['ams-ssl-server'],
              configuration_attributes=params.config['configuration_attributes']['ams-ssl-server'],
              owner=params.ams_user,
              group=params.user_group
    )

    merged_ams_hbase_site = {}
    merged_ams_hbase_site.update(params.config['configurations']['ams-hbase-site'])
    if params.security_enabled:
      merged_ams_hbase_site.update(params.config['configurations']['ams-hbase-security-site'])

    # Add phoenix client side overrides
    merged_ams_hbase_site['phoenix.query.maxGlobalMemoryPercentage'] = str(params.phoenix_max_global_mem_percent)
    merged_ams_hbase_site['phoenix.spool.directory'] = params.phoenix_client_spool_dir

    XmlConfig( "hbase-site.xml",
               conf_dir = params.ams_collector_conf_dir,
               configurations = merged_ams_hbase_site,
               configuration_attributes=params.config['configuration_attributes']['ams-hbase-site'],
               owner = params.ams_user,
               group = params.user_group
    )

    if params.security_enabled:
      TemplateConfig(os.path.join(params.hbase_conf_dir, "ams_collector_jaas.conf"),
                     owner = params.ams_user,
                     template_tag = None)

    if (params.log4j_props != None):
      File(format("{params.ams_collector_conf_dir}/log4j.properties"),
           mode=0644,
           group=params.user_group,
           owner=params.ams_user,
           content=InlineTemplate(params.log4j_props)
      )

    File(format("{ams_collector_conf_dir}/ams-env.sh"),
         owner=params.ams_user,
         content=InlineTemplate(params.ams_env_sh_template)
    )

    Directory(params.ams_collector_log_dir,
              owner=params.ams_user,
              group=params.user_group,
              cd_access="a",
              create_parents = True,
              mode=0755,
    )

    Directory(params.ams_collector_pid_dir,
              owner=params.ams_user,
              group=params.user_group,
              cd_access="a",
              create_parents = True,
              mode=0755,
    )

    # Hack to allow native HBase libs to be included for embedded hbase
    File(os.path.join(params.ams_hbase_home_dir, "bin", "hadoop"),
         owner=params.ams_user,
         mode=0755
    )

    # On some OS this folder could be not exists, so we will create it before pushing there files
    Directory(params.limits_conf_dir,
              create_parents = True,
              owner='root',
              group='root'
    )

    # Setting up security limits
    File(os.path.join(params.limits_conf_dir, 'ams.conf'),
         owner='root',
         group='root',
         mode=0644,
         content=Template("ams.conf.j2")
    )

    # Phoenix spool file dir if not /tmp
    if not os.path.exists(params.phoenix_client_spool_dir):
      Directory(params.phoenix_client_spool_dir,
                owner=params.ams_user,
                mode = 0755,
                group=params.user_group,
                cd_access="a",
                create_parents = True
      )
    pass

    if not params.is_local_fs_rootdir and params.is_ams_distributed:
      # Configuration needed to support NN HA
      XmlConfig("hdfs-site.xml",
            conf_dir=params.ams_collector_conf_dir,
            configurations=params.config['configurations']['hdfs-site'],
            configuration_attributes=params.config['configuration_attributes']['hdfs-site'],
            owner=params.ams_user,
            group=params.user_group,
            mode=0644
      )

      XmlConfig("hdfs-site.xml",
            conf_dir=params.hbase_conf_dir,
            configurations=params.config['configurations']['hdfs-site'],
            configuration_attributes=params.config['configuration_attributes']['hdfs-site'],
            owner=params.ams_user,
            group=params.user_group,
            mode=0644
      )

      # Remove spnego configs from core-site, since AMS does not support spnego (AMBARI-14384)
      truncated_core_site = {}
      truncated_core_site.update(params.config['configurations']['core-site'])
      if 'core-site' in params.config['configurations']:
        if 'hadoop.http.authentication.type' in params.config['configurations']['core-site']:
          truncated_core_site.pop('hadoop.http.authentication.type')
        if 'hadoop.http.filter.initializers' in params.config['configurations']['core-site']:
          truncated_core_site.pop('hadoop.http.filter.initializers')

      XmlConfig("core-site.xml",
                conf_dir=params.ams_collector_conf_dir,
                configurations=truncated_core_site,
                configuration_attributes=params.config['configuration_attributes']['core-site'],
                owner=params.ams_user,
                group=params.user_group,
                mode=0644
      )

      XmlConfig("core-site.xml",
                conf_dir=params.hbase_conf_dir,
                configurations=truncated_core_site,
                configuration_attributes=params.config['configuration_attributes']['core-site'],
                owner=params.ams_user,
                group=params.user_group,
                mode=0644
      )

    if params.metric_collector_https_enabled:
      export_ca_certs(params.ams_collector_conf_dir)

    pass

  elif name == 'monitor':
    Directory(params.ams_monitor_conf_dir,
              owner=params.ams_user,
              group=params.user_group,
              create_parents = True
    )

    Directory(params.ams_monitor_log_dir,
              owner=params.ams_user,
              group=params.user_group,
              mode=0755,
              create_parents = True
    )

    Execute(format("{sudo} chown -R {ams_user}:{user_group} {ams_monitor_log_dir}")
            )

    Directory(params.ams_monitor_pid_dir,
              owner=params.ams_user,
              group=params.user_group,
              cd_access="a",
              mode=0755,
              create_parents = True
    )

    Directory(format("{ams_monitor_dir}/psutil/build"),
              owner=params.ams_user,
              group=params.user_group,
              cd_access="a",
              create_parents = True)

    Execute(format("{sudo} chown -R {ams_user}:{user_group} {ams_monitor_dir}")
    )

    TemplateConfig(
      format("{ams_monitor_conf_dir}/metric_monitor.ini"),
      owner=params.ams_user,
      group=params.user_group,
      template_tag=None
    )

    TemplateConfig(
      format("{ams_monitor_conf_dir}/metric_groups.conf"),
      owner=params.ams_user,
      group=params.user_group,
      template_tag=None
    )

    File(format("{ams_monitor_conf_dir}/ams-env.sh"),
         owner=params.ams_user,
         content=InlineTemplate(params.ams_env_sh_template)
    )

    if params.metric_collector_https_enabled:
      export_ca_certs(params.ams_monitor_conf_dir)

    pass
  elif name == 'grafana':

    ams_grafana_directories = [
                              params.ams_grafana_conf_dir,
                              params.ams_grafana_log_dir,
                              params.ams_grafana_data_dir,
                              params.ams_grafana_pid_dir
                              ]

    for ams_grafana_directory in ams_grafana_directories:
      Directory(ams_grafana_directory,
                owner=params.ams_user,
                group=params.user_group,
                mode=0755,
                recursive_ownership = True
                )

    File(format("{ams_grafana_conf_dir}/ams-grafana-env.sh"),
         owner=params.ams_user,
         group=params.user_group,
         content=InlineTemplate(params.ams_grafana_env_sh_template)
         )

    File(format("{ams_grafana_conf_dir}/ams-grafana.ini"),
         owner=params.ams_user,
         group=params.user_group,
         content=InlineTemplate(params.ams_grafana_ini_template),
         mode=0600
         )

    if action != 'stop':
      for dir in ams_grafana_directories:
        Execute(('chown', '-R', params.ams_user, dir),
                sudo=True
                )

    if params.metric_collector_https_enabled:
      export_ca_certs(params.ams_grafana_conf_dir)

    pass

def export_ca_certs(dir_path):
  # export ca certificates on every restart to handle changed truststore content

  import params
  import tempfile

  ca_certs_path = os.path.join(dir_path, params.metric_truststore_ca_certs)
  truststore = params.metric_truststore_path

  tmpdir = tempfile.mkdtemp()
  truststore_p12 = os.path.join(tmpdir,'truststore.p12')

  if (params.metric_truststore_type.lower() == 'jks'):
    # Convert truststore from JKS to PKCS12
    cmd = format("{sudo} {java64_home}/bin/keytool -importkeystore -srckeystore {metric_truststore_path} -destkeystore {truststore_p12} -srcalias {metric_truststore_alias} -deststoretype PKCS12 -srcstorepass {metric_truststore_password} -deststorepass {metric_truststore_password}")
    Execute(cmd,
    )
    truststore = truststore_p12

  # Export all CA certificates from the truststore to the conf directory
  cmd = format("{sudo} openssl pkcs12 -in {truststore} -out {ca_certs_path} -cacerts -nokeys -passin pass:{metric_truststore_password}")
  Execute(cmd,
  )
  Execute(('chown', params.ams_user, ca_certs_path),
          sudo=True
  )
  Execute(format('{sudo} rm -rf {tmpdir}')
  )


  pass

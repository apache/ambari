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

from resource_management import *
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
              create_parents=True
    )

    Directory(params.ams_checkpoint_dir,
              owner=params.ams_user,
              create_parents=True
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
              create_parents=True
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
def ams(name=None):
  import params

  if name == 'collector':
    Directory(params.ams_collector_conf_dir,
              owner=params.ams_user,
              group=params.user_group,
              create_parents=True
    )

    Execute(('chown', '-R', params.ams_user, params.ams_collector_conf_dir),
            sudo=True
            )

    Directory(params.ams_checkpoint_dir,
              owner=params.ams_user,
              group=params.user_group,
              cd_access="a",
              create_parents=True
    )

    Execute(('chown', '-R', params.ams_user, params.ams_checkpoint_dir),
            sudo=True
            )

    XmlConfig("ams-site.xml",
              conf_dir=params.ams_collector_conf_dir,
              configurations=params.config['configurations']['ams-site'],
              configuration_attributes=params.config['configuration_attributes']['ams-site'],
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
           content=params.log4j_props
      )

    File(format("{ams_collector_conf_dir}/ams-env.sh"),
         owner=params.ams_user,
         content=InlineTemplate(params.ams_env_sh_template)
    )

    Directory(params.ams_collector_log_dir,
              owner=params.ams_user,
              group=params.user_group,
              cd_access="a",
              create_parents=True,
              mode=0755,
    )

    Directory(params.ams_collector_pid_dir,
              owner=params.ams_user,
              group=params.user_group,
              cd_access="a",
              create_parents=True,
              mode=0755,
    )

    # Hack to allow native HBase libs to be included for embedded hbase
    File(os.path.join(params.ams_hbase_home_dir, "bin", "hadoop"),
         owner=params.ams_user,
         mode=0755
    )

    # On some OS this folder could be not exists, so we will create it before pushing there files
    Directory(params.limits_conf_dir,
              create_parents=True,
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
                create_parents=True
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

    pass

  elif name == 'monitor':
    Directory(params.ams_monitor_conf_dir,
              owner=params.ams_user,
              group=params.user_group,
              create_parents=True
    )

    Directory(params.ams_monitor_log_dir,
              owner=params.ams_user,
              group=params.user_group,
              mode=0755,
              create_parents=True
    )

    Directory(params.ams_monitor_pid_dir,
              owner=params.ams_user,
              group=params.user_group,
              mode=0755,
              create_parents=True
    )

    Directory(format("{ams_monitor_dir}/psutil/build"),
              owner=params.ams_user,
              group=params.user_group,
              cd_access="a",
              create_parents=True)

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

    # TODO
    pass

  pass

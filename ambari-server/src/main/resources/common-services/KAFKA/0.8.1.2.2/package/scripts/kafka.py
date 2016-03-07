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
import collections
import os

from resource_management.libraries.functions.version import format_hdp_stack_version, compare_versions
from resource_management.libraries.resources.properties_file import PropertiesFile
from resource_management.libraries.resources.template_config import TemplateConfig
from resource_management.core.resources.system import Directory, Execute, File, Link
from resource_management.core.source import StaticFile, Template, InlineTemplate
from resource_management.libraries.functions import format


from resource_management.core.logger import Logger


def kafka(upgrade_type=None):
    import params
    ensure_base_directories()

    kafka_server_config = mutable_config_dict(params.config['configurations']['kafka-broker'])
    # This still has an issue of hostnames being alphabetically out-of-order for broker.id in HDP-2.2.
    # Starting in HDP 2.3, Kafka handles the generation of broker.id so Ambari doesn't have to.

    effective_version = params.hdp_stack_version if upgrade_type is None else format_hdp_stack_version(params.version)
    Logger.info(format("Effective stack version: {effective_version}"))

    if effective_version is not None and effective_version != "" and compare_versions(effective_version, '2.2.0.0') >= 0 and compare_versions(effective_version, '2.3.0.0') < 0:
      if len(params.kafka_hosts) > 0 and params.hostname in params.kafka_hosts:
        brokerid = str(sorted(params.kafka_hosts).index(params.hostname))
        kafka_server_config['broker.id'] = brokerid
        Logger.info(format("Calculating broker.id as {brokerid}"))

    # listeners and advertised.listeners are only added in 2.3.0.0 onwards.
    if effective_version is not None and effective_version != "" and compare_versions(effective_version, '2.3.0.0') >= 0:
      listeners = kafka_server_config['listeners'].replace("localhost", params.hostname)
      Logger.info(format("Kafka listeners: {listeners}"))

      if params.security_enabled and params.kafka_kerberos_enabled:
        Logger.info("Kafka kerberos security is enabled.")
        if "SASL" not in listeners:
          listeners = listeners.replace("PLAINTEXT", "PLAINTEXTSASL")

        kafka_server_config['listeners'] = listeners
        kafka_server_config['advertised.listeners'] = listeners
        Logger.info(format("Kafka advertised listeners: {listeners}"))
      else:
        kafka_server_config['listeners'] = listeners

        if 'advertised.listeners' in kafka_server_config:
          advertised_listeners = kafka_server_config['advertised.listeners'].replace("localhost", params.hostname)
          kafka_server_config['advertised.listeners'] = advertised_listeners
          Logger.info(format("Kafka advertised listeners: {advertised_listeners}"))
    else:
      kafka_server_config['host.name'] = params.hostname

    if params.has_metric_collector:
      kafka_server_config['kafka.timeline.metrics.host'] = params.metric_collector_host
      kafka_server_config['kafka.timeline.metrics.port'] = params.metric_collector_port
      kafka_server_config['kafka.timeline.metrics.protocol'] = params.metric_collector_protocol
      kafka_server_config['kafka.timeline.metrics.truststore.path'] = params.metric_truststore_path
      kafka_server_config['kafka.timeline.metrics.truststore.type'] = params.metric_truststore_type
      kafka_server_config['kafka.timeline.metrics.truststore.password'] = params.metric_truststore_password

    kafka_data_dir = kafka_server_config['log.dirs']
    kafka_data_dirs = filter(None, kafka_data_dir.split(","))
    Directory(kafka_data_dirs[:],  # Todo: remove list copy when AMBARI-14373 is fixed
              mode=0755,
              cd_access='a',
              owner=params.kafka_user,
              group=params.user_group,
              recursive=True)
    set_dir_ownership(kafka_data_dirs)

    PropertiesFile("server.properties",
                      dir=params.conf_dir,
                      properties=kafka_server_config,
                      owner=params.kafka_user,
                      group=params.user_group,
    )

    File(format("{conf_dir}/kafka-env.sh"),
          owner=params.kafka_user,
          content=InlineTemplate(params.kafka_env_sh_template)
     )

    if (params.log4j_props != None):
        File(format("{conf_dir}/log4j.properties"),
             mode=0644,
             group=params.user_group,
             owner=params.kafka_user,
             content=params.log4j_props
         )

    if params.security_enabled and params.kafka_kerberos_enabled:
        TemplateConfig(format("{conf_dir}/kafka_jaas.conf"),
                         owner=params.kafka_user)

        TemplateConfig(format("{conf_dir}/kafka_client_jaas.conf"),
                       owner=params.kafka_user)

    # On some OS this folder could be not exists, so we will create it before pushing there files
    Directory(params.limits_conf_dir,
              recursive=True,
              owner='root',
              group='root'
    )

    File(os.path.join(params.limits_conf_dir, 'kafka.conf'),
         owner='root',
         group='root',
         mode=0644,
         content=Template("kafka.conf.j2")
    )

    File(os.path.join(params.conf_dir, 'tools-log4j.properties'),
         owner='root',
         group='root',
         mode=0644,
         content=Template("tools-log4j.properties.j2")
         )

    setup_symlink(params.kafka_managed_pid_dir, params.kafka_pid_dir)
    setup_symlink(params.kafka_managed_log_dir, params.kafka_log_dir)


def mutable_config_dict(kafka_broker_config):
    kafka_server_config = {}
    for key, value in kafka_broker_config.iteritems():
        kafka_server_config[key] = value
    return kafka_server_config


# Used to workaround the hardcoded pid/log dir used on the kafka bash process launcher
def setup_symlink(kafka_managed_dir, kafka_ambari_managed_dir):
  import params
  backup_folder_path = None
  backup_folder_suffix = "_tmp"
  if kafka_ambari_managed_dir != kafka_managed_dir:
    if os.path.exists(kafka_managed_dir) and not os.path.islink(kafka_managed_dir):

      # Backup existing data before delete if config is changed repeatedly to/from default location at any point in time time, as there may be relevant contents (historic logs)
      backup_folder_path = backup_dir_contents(kafka_managed_dir, backup_folder_suffix)

      Directory(kafka_managed_dir,
                action="delete",
                recursive=True)

    elif os.path.islink(kafka_managed_dir) and os.path.realpath(kafka_managed_dir) != kafka_ambari_managed_dir:
      Link(kafka_managed_dir,
           action="delete")

    if not os.path.islink(kafka_managed_dir):
      Link(kafka_managed_dir,
           to=kafka_ambari_managed_dir)

  elif os.path.islink(kafka_managed_dir): # If config is changed and coincides with the kafka managed dir, remove the symlink and physically create the folder
    Link(kafka_managed_dir,
         action="delete")

    Directory(kafka_managed_dir,
              mode=0755,
              cd_access='a',
              owner=params.kafka_user,
              group=params.user_group,
              recursive=True)
    set_dir_ownership(kafka_managed_dir)

  if backup_folder_path:
    # Restore backed up files to current relevant dirs if needed - will be triggered only when changing to/from default path;
    for file in os.listdir(backup_folder_path):
      File(os.path.join(kafka_managed_dir,file),
           owner=params.kafka_user,
           content = StaticFile(os.path.join(backup_folder_path,file)))

    # Clean up backed up folder
    Directory(backup_folder_path,
              action="delete",
              recursive=True)


# Uses agent temp dir to store backup files
def backup_dir_contents(dir_path, backup_folder_suffix):
  import params
  backup_destination_path = params.tmp_dir + os.path.normpath(dir_path)+backup_folder_suffix
  Directory(backup_destination_path,
            mode=0755,
            cd_access='a',
            owner=params.kafka_user,
            group=params.user_group,
            recursive=True
  )
  set_dir_ownership(backup_destination_path)
  # Safely copy top-level contents to backup folder
  for file in os.listdir(dir_path):
    File(os.path.join(backup_destination_path, file),
         owner=params.kafka_user,
         content = StaticFile(os.path.join(dir_path,file)))

  return backup_destination_path


def ensure_base_directories():
  """
  Make basic Kafka directories, and make sure that their ownership is correct
  """
  import params
  base_dirs = [params.kafka_log_dir, params.kafka_pid_dir, params.conf_dir]
  Directory(base_dirs[:],  # Todo: remove list copy when AMBARI-14373 is fixed
            mode=0755,
            cd_access='a',
            owner=params.kafka_user,
            group=params.user_group,
            recursive=True
            )
  set_dir_ownership(base_dirs)


def set_dir_ownership(targets):
  import params
  if isinstance(targets, collections.Iterable):
    directories = targets
  else:  # If target is a single object, convert it to list
    directories = [targets]
  for directory in directories:
    # If path is empty or a single slash,
    # may corrupt filesystem permissions
    if len(directory) > 1:
      Execute(('chown', '-R', format("{kafka_user}:{user_group}"), directory),
            sudo=True)
    else:
      Logger.warning("Permissions for the folder \"%s\" were not updated due to "
            "empty path passed: " % directory)

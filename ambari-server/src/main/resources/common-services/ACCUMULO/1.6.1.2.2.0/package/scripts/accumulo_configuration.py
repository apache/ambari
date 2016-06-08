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

import os
from resource_management.core.resources.system import Directory, Execute, File
from resource_management.core.source import InlineTemplate, StaticFile
from resource_management.core.shell import as_user
from resource_management.libraries.functions.format import format
from resource_management.libraries.resources.properties_file import PropertiesFile
from resource_management.libraries.resources.template_config import TemplateConfig
from resource_management.libraries.resources.xml_config import XmlConfig

def setup_conf_dir(name=None): # 'master' or 'tserver' or 'monitor' or 'gc' or 'tracer' or 'client'
  import params

  # create the conf directory
  Directory( params.conf_dir,
      mode=0755,
      owner = params.accumulo_user,
      group = params.user_group,
      create_parents = True
  )

  if name == 'client':
    dest_conf_dir = params.conf_dir

    # create a site file for client processes
    configs = {}
    configs.update(params.config['configurations']['accumulo-site'])
    if "instance.secret" in configs:
      configs.pop("instance.secret")
    if "trace.token.property.password" in configs:
      configs.pop("trace.token.property.password")
    XmlConfig("accumulo-site.xml",
              conf_dir = dest_conf_dir,
              configurations = configs,
              configuration_attributes=params.config['configuration_attributes']['accumulo-site'],
              owner = params.accumulo_user,
              group = params.user_group,
              mode = 0644
    )

    # create env file
    File(format("{dest_conf_dir}/accumulo-env.sh"),
         mode=0644,
         group=params.user_group,
         owner=params.accumulo_user,
         content=InlineTemplate(params.env_sh_template)
    )
  else:
    dest_conf_dir = params.server_conf_dir
    # create server conf directory
    Directory( params.server_conf_dir,
               mode=0700,
               owner = params.accumulo_user,
               group = params.user_group,
               create_parents = True
    )
    # create a site file for server processes
    configs = {}
    configs.update(params.config['configurations']['accumulo-site'])
    configs["instance.secret"] = str(params.config['configurations']['accumulo-env']['instance_secret'])
    configs["trace.token.property.password"] = str(params.trace_password)
    XmlConfig( "accumulo-site.xml",
               conf_dir = dest_conf_dir,
               configurations = configs,
               configuration_attributes=params.config['configuration_attributes']['accumulo-site'],
               owner = params.accumulo_user,
               group = params.user_group,
               mode = 0600
    )

    # create pid dir
    Directory( params.pid_dir,
               owner = params.accumulo_user,
               group = params.user_group,
               create_parents = True,
               cd_access = "a",
               mode = 0755,
    )

    # create log dir
    Directory (params.log_dir,
               owner = params.accumulo_user,
               group = params.user_group,
               create_parents = True,
               cd_access = "a",
               mode = 0755,
    )

    # create env file
    File(format("{dest_conf_dir}/accumulo-env.sh"),
         mode=0644,
         group=params.user_group,
         owner=params.accumulo_user,
         content=InlineTemplate(params.server_env_sh_template)
    )

  # create client.conf file
  configs = {}
  if 'client' in params.config['configurations']:
    configs.update(params.config['configurations']['client'])
  configs["instance.name"] = params.instance_name
  configs["instance.zookeeper.host"] = params.config['configurations']['accumulo-site']['instance.zookeeper.host']
  copy_site_property(configs, 'instance.rpc.sasl.enabled')
  copy_site_property(configs, 'rpc.sasl.qop')
  copy_site_property(configs, 'rpc.useJsse')
  copy_site_property(configs, 'instance.rpc.ssl.clientAuth')
  copy_site_property(configs, 'instance.rpc.ssl.enabled')
  copy_site_property(configs, 'instance.zookeeper.timeout')
  copy_site_property(configs, 'trace.span.receivers')
  copy_site_property(configs, 'trace.zookeeper.path')
  for key,value in params.config['configurations']['accumulo-site'].iteritems():
    if key.startswith("trace.span.receiver."):
      configs[key] = value
  PropertiesFile(format("{dest_conf_dir}/client.conf"),
                 properties = configs,
                 owner = params.accumulo_user,
                 group = params.user_group
  )

  # create log4j.properties files
  if (params.log4j_props != None):
    File(format("{dest_conf_dir}/log4j.properties"),
         mode=0644,
         group=params.user_group,
         owner=params.accumulo_user,
         content=params.log4j_props
    )
  else:
    File(format("{dest_conf_dir}/log4j.properties"),
         mode=0644,
         group=params.user_group,
         owner=params.hbase_user
    )

  # create logging configuration files
  accumulo_TemplateConfig("auditLog.xml", dest_conf_dir)
  accumulo_TemplateConfig("generic_logger.xml", dest_conf_dir)
  accumulo_TemplateConfig("monitor_logger.xml", dest_conf_dir)
  accumulo_StaticFile("accumulo-metrics.xml", dest_conf_dir)

  # create host files
  accumulo_TemplateConfig("tracers", dest_conf_dir)
  accumulo_TemplateConfig("gc", dest_conf_dir)
  accumulo_TemplateConfig("monitor", dest_conf_dir)
  accumulo_TemplateConfig("slaves", dest_conf_dir)
  accumulo_TemplateConfig("masters", dest_conf_dir)

  # metrics configuration
  if params.has_metric_collector:
    accumulo_TemplateConfig( "hadoop-metrics2-accumulo.properties", dest_conf_dir)

  # other server setup
  if name == 'master':
    params.HdfsResource(format("/user/{params.accumulo_user}"),
                         type="directory",
                         action="create_on_execute",
                         owner=params.accumulo_user,
                         mode=0700
    )
    params.HdfsResource(format("{params.parent_dir}"),
                         type="directory",
                         action="create_on_execute",
                         owner=params.accumulo_user,
                         mode=0700
    )
    params.HdfsResource(None, action="execute")
    if params.security_enabled and params.has_secure_user_auth:
      Execute( format("{params.kinit_cmd} "
                      "{params.daemon_script} init "
                      "--user {params.accumulo_principal_name} "
                      "--instance-name {params.instance_name} "
                      "--clear-instance-name "
                      ">{params.log_dir}/accumulo-init.out "
                      "2>{params.log_dir}/accumulo-init.err"),
               not_if=as_user(format("{params.kinit_cmd} "
                                     "{params.hadoop_bin_dir}/hadoop --config "
                                     "{params.hadoop_conf_dir} fs -stat "
                                     "{params.instance_volumes}"),
                              params.accumulo_user),
               logoutput=True,
               user=params.accumulo_user)
    else:
      passfile = format("{params.exec_tmp_dir}/pass")
      try:
        File(passfile,
             mode=0600,
             group=params.user_group,
             owner=params.accumulo_user,
             content=InlineTemplate('{{root_password}}\n'
                                    '{{root_password}}\n\n')
        )
        Execute( format("cat {passfile} | {params.daemon_script} init "
                        "--instance-name {params.instance_name} "
                        "--clear-instance-name "
                        ">{params.log_dir}/accumulo-init.out "
                        "2>{params.log_dir}/accumulo-init.err"),
                 not_if=as_user(format("{params.kinit_cmd} "
                                       "{params.hadoop_bin_dir}/hadoop --config "
                                       "{params.hadoop_conf_dir} fs -stat "
                                       "{params.instance_volumes}"),
                                params.accumulo_user),
                 logoutput=True,
                 user=params.accumulo_user)
      finally:
        File(passfile, action = "delete")

  if name == 'tracer':
    if params.security_enabled and params.has_secure_user_auth:
      Execute( format("{params.kinit_cmd} "
                      "{params.daemon_script} init --reset-security "
                      "--user {params.accumulo_principal_name} "
                      "--password NA "
                      ">{params.log_dir}/accumulo-reset.out "
                      "2>{params.log_dir}/accumulo-reset.err"),
               not_if=as_user(format("{params.kinit_cmd} "
                                     "{params.daemon_script} shell -e "
                                     "\"userpermissions -u "
                                     "{params.accumulo_principal_name}\" | "
                                     "grep System.CREATE_TABLE"),
                              params.accumulo_user),
               user=params.accumulo_user)
      create_user(params.smokeuser_principal, params.smoke_test_password)
    else:
      # do not try to reset security in nonsecure mode, for now
      # Execute( format("{params.daemon_script} init --reset-security "
      #                 "--user root "
      #                 ">{params.log_dir}/accumulo-reset.out "
      #                 "2>{params.log_dir}/accumulo-reset.err"),
      #          not_if=as_user(format("cat {rpassfile} | "
      #                                "{params.daemon_script} shell -e "
      #                                "\"userpermissions -u root\" | "
      #                                "grep System.CREATE_TABLE"),
      #                         params.accumulo_user),
      #          user=params.accumulo_user)
      create_user(params.smoke_test_user, params.smoke_test_password)
    create_user(params.trace_user, params.trace_password)
    rpassfile = format("{params.exec_tmp_dir}/pass0")
    cmdfile = format("{params.exec_tmp_dir}/resetcmds")
    try:
      File(cmdfile,
           mode=0600,
           group=params.user_group,
           owner=params.accumulo_user,
           content=InlineTemplate('grant -t trace -u {{trace_user}} Table.ALTER_TABLE\n'
                                  'grant -t trace -u {{trace_user}} Table.READ\n'
                                  'grant -t trace -u {{trace_user}} Table.WRITE\n\n')
      )
      if params.security_enabled and params.has_secure_user_auth:
        Execute( format("{params.kinit_cmd} {params.daemon_script} shell -f "
                        "{cmdfile}"),
                 only_if=as_user(format("{params.kinit_cmd} "
                                        "{params.daemon_script} shell "
                                        "-e \"table trace\""),
                                 params.accumulo_user),
                 not_if=as_user(format("{params.kinit_cmd} "
                                       "{params.daemon_script} shell "
                                       "-e \"userpermissions -u "
                                       "{params.trace_user} | "
                                       "grep Table.READ | grep trace"),
                                params.accumulo_user),
                 user=params.accumulo_user)
      else:
        File(rpassfile,
             mode=0600,
             group=params.user_group,
             owner=params.accumulo_user,
             content=InlineTemplate('{{root_password}}\n\n')
        )
        Execute( format("cat {rpassfile} | {params.daemon_script} shell -f "
                        "{cmdfile} -u root"),
                 only_if=as_user(format("cat {rpassfile} | "
                                       "{params.daemon_script} shell -u root "
                                       "-e \"table trace\""),
                                params.accumulo_user),
                 not_if=as_user(format("cat {rpassfile} | "
                                       "{params.daemon_script} shell -u root "
                                       "-e \"userpermissions -u "
                                       "{params.trace_user} | "
                                       "grep Table.READ | grep trace"),
                                params.accumulo_user),
                 user=params.accumulo_user)
    finally:
      try_remove(rpassfile)
      try_remove(cmdfile)

def copy_site_property(configs, name):
  import params
  if name in params.config['configurations']['accumulo-site']:
    configs[name] = params.config['configurations']['accumulo-site'][name]

def create_user(user, password):
  import params
  rpassfile = format("{params.exec_tmp_dir}/pass0")
  passfile = format("{params.exec_tmp_dir}/pass")
  cmdfile = format("{params.exec_tmp_dir}/cmds")
  try:
    File(cmdfile,
         mode=0600,
         group=params.user_group,
         owner=params.accumulo_user,
         content=InlineTemplate(format("createuser {user}\n"
                                       "grant -s System.CREATE_TABLE -u {user}\n\n"))
    )
    if params.security_enabled and params.has_secure_user_auth:
      Execute( format("{params.kinit_cmd} {params.daemon_script} shell -f "
                      "{cmdfile}"),
               not_if=as_user(format("{params.kinit_cmd} "
                                     "{params.daemon_script} shell "
                                     "-e \"userpermissions -u {user}\""),
                              params.accumulo_user),
               user=params.accumulo_user)
    else:
      File(rpassfile,
           mode=0600,
           group=params.user_group,
           owner=params.accumulo_user,
           content=InlineTemplate('{{root_password}}\n\n')
      )
      File(passfile,
           mode=0600,
           group=params.user_group,
           owner=params.accumulo_user,
           content=InlineTemplate(format("{params.root_password}\n"
                                         "{password}\n"
                                         "{password}\n\n"))
      )
      Execute( format("cat {passfile} | {params.daemon_script} shell -u root "
                      "-f {cmdfile}"),
               not_if=as_user(format("cat {rpassfile} | "
                                     "{params.daemon_script} shell -u root "
                                     "-e \"userpermissions -u {user}\""),
                              params.accumulo_user),
               user=params.accumulo_user)
  finally:
    try_remove(rpassfile)
    try_remove(passfile)
    try_remove(cmdfile)

def try_remove(file):
  try:
    os.remove(file)
  except:
    pass

# create file 'name' from template
def accumulo_TemplateConfig(name, dest_conf_dir, tag=None):
  import params

  TemplateConfig( format("{dest_conf_dir}/{name}"),
      owner = params.accumulo_user,
      group = params.user_group,
      template_tag = tag
  )

# create static file 'name'
def accumulo_StaticFile(name, dest_conf_dir):
  import params

  File(format("{dest_conf_dir}/{name}"),
    mode=0644,
    group=params.user_group,
    owner=params.accumulo_user,
    content=StaticFile(name)
  )

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

from resource_management import *

def setup_java():
  """
  Installs jdk using specific params, that comes from ambari-server
  """
  import params

  jdk_curl_target = format("{artifact_dir}/{jdk_name}")
  java_dir = os.path.dirname(params.java_home)
  java_exec = format("{java_home}/bin/java")
  
  if not params.jdk_name:
    return
  
  Execute(format("mkdir -p {artifact_dir} ; curl -kf --retry 10 {jdk_location}/{jdk_name} -o {jdk_curl_target}"),
          path = ["/bin","/usr/bin/"],
          not_if = format("test -e {java_exec}"))

  if params.jdk_name.endswith(".bin"):
    install_cmd = format("mkdir -p {java_dir} ; chmod +x {jdk_curl_target}; cd {java_dir} ; echo A | {jdk_curl_target} -noregister > /dev/null 2>&1")
  elif params.jdk_name.endswith(".gz"):
    install_cmd = format("mkdir -p {java_dir} ; cd {java_dir} ; tar -xf {jdk_curl_target} > /dev/null 2>&1")
  
  Execute(install_cmd,
          path = ["/bin","/usr/bin/"],
          not_if = format("test -e {java_exec}")
  )
  jce_curl_target = format("{artifact_dir}/{jce_policy_zip}")
  download_jce = format("mkdir -p {artifact_dir}; curl -kf --retry 10 {jce_location}/{jce_policy_zip} -o {jce_curl_target}")
  Execute( download_jce,
        path = ["/bin","/usr/bin/"],
        not_if =format("test -e {jce_curl_target}"),
        ignore_failures = True
  )
  
  if params.security_enabled:
    security_dir = format("{java_home}/jre/lib/security")
    extract_cmd = format("rm -f local_policy.jar; rm -f US_export_policy.jar; unzip -o -j -q {jce_curl_target}")
    Execute(extract_cmd,
          only_if = format("test -e {security_dir} && test -f {jce_curl_target}"),
          cwd  = security_dir,
          path = ['/bin/','/usr/bin']
    )

def setup_hadoop():
  """
  Setup hadoop files and directories
  """
  import params

  File(os.path.join(params.snmp_conf_dir, 'snmpd.conf'),
       content=Template("snmpd.conf.j2"))
  Service("snmpd",
          action = "restart")

  Execute("/bin/echo 0 > /selinux/enforce",
          only_if="test -f /selinux/enforce"
  )

  install_snappy()

  #directories
  Directory(params.hadoop_conf_dir,
            recursive=True,
            owner='root',
            group='root'
  )
  Directory(params.hdfs_log_dir_prefix,
            recursive=True,
            owner='root',
            group='root'
  )
  Directory(params.hadoop_pid_dir_prefix,
            recursive=True,
            owner='root',
            group='root'
  )

  #files
  File(os.path.join(params.limits_conf_dir, 'hdfs.conf'),
       owner='root',
       group='root',
       mode=0644,
       content=Template("hdfs.conf.j2")
  )
  if params.security_enabled:
    File(os.path.join(params.hadoop_bin, "task-controller"),
         owner="root",
         group=params.mapred_tt_group,
         mode=06050
    )
    tc_mode = 0644
    tc_owner = "root"
  else:
    tc_mode = None
    tc_owner = params.hdfs_user

  if tc_mode:
    File(os.path.join(params.hadoop_conf_dir, 'taskcontroller.cfg'),
         owner = tc_owner,
         mode = tc_mode,
         group = params.mapred_tt_group,
         content=Template("taskcontroller.cfg.j2")
    )
  else:
    File(os.path.join(params.hadoop_conf_dir, 'taskcontroller.cfg'),
         owner=tc_owner,
         content=Template("taskcontroller.cfg.j2")
    )
  for file in ['hadoop-env.sh', 'commons-logging.properties', 'slaves']:
    File(os.path.join(params.hadoop_conf_dir, file),
         owner=tc_owner,
         content=Template(file + ".j2")
    )

  health_check_template = "health_check" #for stack 1 use 'health_check'
  File(os.path.join(params.hadoop_conf_dir, "health_check"),
       owner=tc_owner,
       content=Template(health_check_template + ".j2")
  )

  log4j_filename = os.path.join(params.hadoop_conf_dir, "log4j.properties")
  if (params.log4j_props != None):
    File(log4j_filename,
         mode=0644,
         group=params.user_group,
         owner=params.hdfs_user,
         content=params.log4j_props
    )
  elif (os.path.exists(format("{params.hadoop_conf_dir}/log4j.properties"))):
    File(log4j_filename,
         mode=0644,
         group=params.user_group,
         owner=params.hdfs_user,
    )

  File(os.path.join(params.hadoop_conf_dir, "hadoop-metrics2.properties"),
       owner=params.hdfs_user,
       content=Template("hadoop-metrics2.properties.j2")
  )

  db_driver_dload_cmd = ""
  if params.server_db_name == 'oracle' and params.oracle_driver_url != "":
    db_driver_dload_cmd = format(
      "curl -kf --retry 5 {oracle_driver_url} -o {hadoop_lib_home}/{db_driver_filename}")
  elif params.server_db_name == 'mysql' and params.mysql_driver_url != "":
    db_driver_dload_cmd = format(
      "curl -kf --retry 5 {mysql_driver_url} -o {hadoop_lib_home}/{db_driver_filename}")

  if db_driver_dload_cmd:
    Execute(db_driver_dload_cmd,
            not_if =format("test -e {hadoop_lib_home}/{db_driver_filename}")
    )


def setup_configs():
  """
  Creates configs for services DHFS mapred
  """
  import params

  if "mapred-queue-acls" in params.config['configurations']:
    XmlConfig("mapred-queue-acls.xml",
              conf_dir=params.hadoop_conf_dir,
              configurations=params.config['configurations'][
                'mapred-queue-acls'],
              owner=params.mapred_user,
              group=params.user_group
    )
  elif os.path.exists(
      os.path.join(params.hadoop_conf_dir, "mapred-queue-acls.xml")):
    File(os.path.join(params.hadoop_conf_dir, "mapred-queue-acls.xml"),
         owner=params.mapred_user,
         group=params.user_group
    )

  if "hadoop-policy" in params.config['configurations']:
    XmlConfig("hadoop-policy.xml",
              conf_dir=params.hadoop_conf_dir,
              configurations=params.config['configurations']['hadoop-policy'],
              owner=params.hdfs_user,
              group=params.user_group
    )

  XmlConfig("core-site.xml",
            conf_dir=params.hadoop_conf_dir,
            configurations=params.config['configurations']['core-site'],
            owner=params.hdfs_user,
            group=params.user_group
  )

  if "mapred-site" in params.config['configurations']:
    XmlConfig("mapred-site.xml",
              conf_dir=params.hadoop_conf_dir,
              configurations=params.config['configurations']['mapred-site'],
              owner=params.mapred_user,
              group=params.user_group
    )

  File(params.task_log4j_properties_location,
       content=StaticFile("task-log4j.properties"),
       mode=0755
  )

  if "capacity-scheduler" in params.config['configurations']:
    XmlConfig("capacity-scheduler.xml",
              conf_dir=params.hadoop_conf_dir,
              configurations=params.config['configurations'][
                'capacity-scheduler'],
              owner=params.hdfs_user,
              group=params.user_group
    )

  XmlConfig("hdfs-site.xml",
            conf_dir=params.hadoop_conf_dir,
            configurations=params.config['configurations']['hdfs-site'],
            owner=params.hdfs_user,
            group=params.user_group
  )

  # if params.stack_version[0] == "1":
  Link('/usr/lib/hadoop/lib/hadoop-tools.jar',
       to = '/usr/lib/hadoop/hadoop-tools.jar'
  )

  if os.path.exists(os.path.join(params.hadoop_conf_dir, 'configuration.xsl')):
    File(os.path.join(params.hadoop_conf_dir, 'configuration.xsl'),
         owner=params.hdfs_user,
         group=params.user_group
    )
  if os.path.exists(os.path.join(params.hadoop_conf_dir, 'fair-scheduler.xml')):
    File(os.path.join(params.hadoop_conf_dir, 'fair-scheduler.xml'),
         owner=params.mapred_user,
         group=params.user_group
    )
  if os.path.exists(os.path.join(params.hadoop_conf_dir, 'masters')):
    File(os.path.join(params.hadoop_conf_dir, 'masters'),
              owner=params.hdfs_user,
              group=params.user_group
    )
  if os.path.exists(
      os.path.join(params.hadoop_conf_dir, 'ssl-client.xml.example')):
    File(os.path.join(params.hadoop_conf_dir, 'ssl-client.xml.example'),
         owner=params.mapred_user,
         group=params.user_group
    )
  if os.path.exists(
      os.path.join(params.hadoop_conf_dir, 'ssl-server.xml.example')):
    File(os.path.join(params.hadoop_conf_dir, 'ssl-server.xml.example'),
         owner=params.mapred_user,
         group=params.user_group
    )

  # generate_include_file()

def generate_include_file():
  import params

  if params.dfs_hosts and params.has_slaves:
    include_hosts_list = params.slave_hosts
    File(params.dfs_hosts,
         content=Template("include_hosts_list.j2"),
         owner=params.hdfs_user,
         group=params.user_group
    )


def install_snappy():
  import params

  snappy_so = "libsnappy.so"
  so_target_dir_x86 = format("{hadoop_lib_home}/native/Linux-i386-32")
  so_target_dir_x64 = format("{hadoop_lib_home}/native/Linux-amd64-64")
  so_target_x86 = format("{so_target_dir_x86}/{snappy_so}")
  so_target_x64 = format("{so_target_dir_x64}/{snappy_so}")
  so_src_dir_x86 = format("{hadoop_home}/lib")
  so_src_dir_x64 = format("{hadoop_home}/lib64")
  so_src_x86 = format("{so_src_dir_x86}/{snappy_so}")
  so_src_x64 = format("{so_src_dir_x64}/{snappy_so}")
  Execute(
    format("mkdir -p {so_target_dir_x86}; ln -sf {so_src_x86} {so_target_x86}"))
  Execute(
    format("mkdir -p {so_target_dir_x64}; ln -sf {so_src_x64} {so_target_x64}"))


def create_javahome_symlink():
  if os.path.exists("/usr/jdk/jdk1.6.0_31") and not os.path.exists("/usr/jdk64/jdk1.6.0_31"):
    Execute("mkdir -p /usr/jdk64/")
    Execute("ln -s /usr/jdk/jdk1.6.0_31 /usr/jdk64/jdk1.6.0_31")

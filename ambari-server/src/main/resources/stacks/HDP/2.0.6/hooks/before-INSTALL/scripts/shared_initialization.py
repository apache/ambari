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

def setup_users():
  """
  Creates users before cluster installation
  """
  import params

  Group(params.user_group)
  Group(params.smoke_user_group)
  Group(params.proxyuser_group)
  User(params.smoke_user,
       gid=params.user_group,
       groups=[params.proxyuser_group]
  )
  smoke_user_dirs = format(
    "/tmp/hadoop-{smoke_user},/tmp/hsperfdata_{smoke_user},/home/{smoke_user},/tmp/{smoke_user},/tmp/sqoop-{smoke_user}")
  set_uid(params.smoke_user, smoke_user_dirs)

  if params.has_hbase_masters:
    User(params.hbase_user,
         gid = params.user_group,
         groups=[params.user_group])
    hbase_user_dirs = format(
      "/home/{hbase_user},/tmp/{hbase_user},/usr/bin/{hbase_user},/var/log/{hbase_user},{hbase_tmp_dir}")
    set_uid(params.hbase_user, hbase_user_dirs)

  if params.has_nagios:
    Group(params.nagios_group)
    User(params.nagios_user,
         gid=params.nagios_group)

  if params.has_oozie_server:
    User(params.oozie_user,
         gid = params.user_group)

  if params.has_hcat_server_host:
    User(params.webhcat_user,
         gid = params.user_group)
    User(params.hcat_user,
         gid = params.user_group)

  if params.has_hive_server_host:
    User(params.hive_user,
         gid = params.user_group)

  if params.has_resourcemanager:
    User(params.yarn_user,
         gid = params.user_group)

  if params.has_ganglia_server:
    Group(params.gmetad_user)
    Group(params.gmond_user)
    User(params.gmond_user,
         gid=params.user_group,
        groups=[params.gmond_user])
    User(params.gmetad_user,
         gid=params.user_group,
        groups=[params.gmetad_user])

  User(params.hdfs_user,
        gid=params.user_group,
        groups=[params.user_group]
  )
  User(params.mapred_user,
       gid=params.user_group,
       groups=[params.user_group]
  )
  if params.has_zk_host:
    User(params.zk_user,
         gid=params.user_group)

  if params.has_storm_server:
    User(params.storm_user,
         gid=params.user_group,
         groups=[params.user_group]
    )

  if params.has_falcon_server:
    User(params.falcon_user,
         gid=params.user_group,
         groups=[params.user_group]
    )
    
  if params.has_tez:  
    User(params.tez_user,
      gid=params.user_group,
      groups=[params.proxyuser_group]
  )

def set_uid(user, user_dirs):
  """
  user_dirs - comma separated directories
  """
  File("/tmp/changeUid.sh",
       content=StaticFile("changeToSecureUid.sh"),
       mode=0555)
  Execute(format("/tmp/changeUid.sh {user} {user_dirs} 2>/dev/null"),
          not_if = format("test $(id -u {user}) -gt 1000"))

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

  Execute(format("mkdir -p {artifact_dir} ; '\
  curl --noproxy {ambari_server_hostname} -kf \
  --retry 10 {jdk_location}/{jdk_name} -o {jdk_curl_target}"),
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

  if params.jce_policy_zip is not None:
    jce_curl_target = format("{artifact_dir}/{jce_policy_zip}")
    download_jce = format("mkdir -p {artifact_dir}; \
    curl --noproxy {ambari_server_hostname} -kf --retry 10 \
    {jce_location}/{jce_policy_zip} -o {jce_curl_target}")
    Execute( download_jce,
             path = ["/bin","/usr/bin/"],
             not_if =format("test -e {jce_curl_target}"),
             ignore_failures = True
    )
  elif params.security_enabled:
    # Something weird is happening
    raise Fail("Security is enabled, but JCE policy zip is not specified.")

  if params.security_enabled:
    security_dir = format("{java_home}/jre/lib/security")
    extract_cmd = format("rm -f local_policy.jar; rm -f US_export_policy.jar; unzip -o -j -q {jce_curl_target}")
    Execute(extract_cmd,
            only_if = format("test -e {security_dir} && test -f {jce_curl_target}"),
            cwd  = security_dir,
            path = ['/bin/','/usr/bin']
    )

def install_packages():
  packages = {"redhat": ["net-snmp-utils", "net-snmp"],
              "suse": ["net-snmp"],
              "debian": ["snmp", "snmpd"],
              "all": ["unzip", "curl"]
              }
  
  Package(packages['all'])
  Package(packages[System.get_instance().os_family])

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
import sys
os.environ["PATH"] += os.pathsep + "/var/lib/ambari-agent"
sys.path.append("/usr/lib/python2.6/site-packages")

import glob
from logging import thread
import re
import hashlib
import tempfile
import time
import functools
from xml.dom import minidom
from xml.dom.minidom import parseString
 
from resource_management import *
from resource_management.core import shell
from resource_management.core.base import Resource, ForcedListArgument, ResourceArgument, BooleanArgument
from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger
from resource_management.core.resources.system import Execute
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.format import format
from resource_management.libraries.resources.hdfs_resource import HdfsResource
from resource_management.libraries.resources.execute_hadoop import ExecuteHadoop
from resource_management import Script
 
"""
This file provides helper methods needed for the versioning of RPMs. Specifically, it does dynamic variable
interpretation to replace strings like {{ hdp_stack_version }}  where the value of the
variables cannot be determined ahead of time, but rather, depends on what files are found.
 
It assumes that {{ hdp_stack_version }} is constructed as ${major.minor.patch.rev}-${build_number}
E.g., 998.2.2.1.0-998
Please note that "-${build_number}" is optional.
"""

def getPropertyValueFromConfigXMLFile(xmlfile, name, defaultValue=None):
  xmldoc = minidom.parse(xmlfile)
  propNodes = [node.parentNode for node in xmldoc.getElementsByTagName("name") if node.childNodes[0].nodeValue == name]
  if len(propNodes) > 0:
    for node in propNodes[-1].childNodes:
      if node.nodeName == "value":
        if len(node.childNodes) > 0:
          return node.childNodes[0].nodeValue
        else:
          return defaultValue
  return defaultValue

def get_fs_root(fsdefaultName=None):
  fsdefaultName = "fake"
   
  while (not fsdefaultName.startswith("wasb://")):
    fsdefaultName =  getPropertyValueFromConfigXMLFile("/etc/hadoop/conf/core-site.xml", "fs.defaultFS")
    if fsdefaultName is None:
      fsdefaultName = "fake"
    print "Waiting to read appropriate value of fs.defaultFS from /etc/hadoop/conf/core-site.xml ..."
    time.sleep(10)
    pass

  print "Returning fs.defaultFS -> " + fsdefaultName
  return fsdefaultName
 
# These values must be the suffix of the properties in cluster-env.xml
TAR_SOURCE_SUFFIX = "_tar_source"
TAR_DESTINATION_FOLDER_SUFFIX = "_tar_destination_folder"

class params:
  hdfs_user = "hdfs"
  mapred_user ="mapred"
  hadoop_bin_dir="/usr/hdp/current/hadoop-client/bin"
  hadoop_conf_dir = "/etc/hadoop/conf"
  user_group = "hadoop"
  security_enabled = False
  oozie_user = "oozie"
  execute_path = "/usr/hdp/current/hadoop-client/bin"
  ambari_libs_dir = "/var/lib/ambari-agent/lib"
  hdfs_site = ConfigDictionary({'dfs.webhdfs.enabled':False, 
  })
  fs_default = get_fs_root()
  oozie_env_sh_template = \
'''
#!/bin/bash

export OOZIE_CONFIG=${OOZIE_CONFIG:-/usr/hdp/current/oozie/conf}
export OOZIE_DATA=${OOZIE_DATA:-/var/lib/oozie/data}
export OOZIE_LOG=${OOZIE_LOG:-/var/log/oozie}
export CATALINA_BASE=${CATALINA_BASE:-/usr/hdp/current/oozie-server/oozie-server}
export CATALINA_TMPDIR=${CATALINA_TMPDIR:-/var/tmp/oozie}
export CATALINA_PID=${CATALINA_PID:-/var/run/oozie/oozie.pid}
export OOZIE_CATALINA_HOME=/usr/lib/bigtop-tomcat
'''
  
  HdfsResource = functools.partial(
    HdfsResource,
    user=hdfs_user,
    security_enabled = False,
    keytab = None,
    kinit_path_local = None,
    hadoop_bin_dir = hadoop_bin_dir,
    hadoop_conf_dir = hadoop_conf_dir,
    principal_name = None,
    hdfs_site = hdfs_site,
    default_fs = fs_default
  )
 
def _copy_files(source_and_dest_pairs, file_owner, group_owner, kinit_if_needed):
  """
  :param source_and_dest_pairs: List of tuples (x, y), where x is the source file in the local file system,
  and y is the destination file path in HDFS
  :param file_owner: Owner to set for the file copied to HDFS (typically hdfs account)
  :param group_owner: Owning group to set for the file copied to HDFS (typically hadoop group)
  :param kinit_if_needed: kinit command if it is needed, otherwise an empty string
  :return: Returns 0 if at least one file was copied and no exceptions occurred, and 1 otherwise.
 
  Must kinit before calling this function.
  """
 
  for (source, destination) in source_and_dest_pairs:
    params.HdfsResource(destination,
                  action="create_on_execute",
                  type = 'file',
                  mode=0444,
                  owner=file_owner,
                  group=group_owner,
                  source=source,
    )
 
 
def copy_tarballs_to_hdfs(source, dest, hdp_select_component_name, component_user, file_owner, group_owner):
  """
  :param tarball_prefix: Prefix of the tarball must be one of tez, hive, mr, pig
  :param hdp_select_component_name: Component name to get the status to determine the version
  :param component_user: User that will execute the Hadoop commands
  :param file_owner: Owner of the files copied to HDFS (typically hdfs account)
  :param group_owner: Group owner of the files copied to HDFS (typically hadoop group)
  :return: Returns 0 on success, 1 if no files were copied, and in some cases may raise an exception.
 
  In order to call this function, params.py must have all of the following,
  hdp_stack_version, kinit_path_local, security_enabled, hdfs_user, hdfs_principal_name, hdfs_user_keytab,
  hadoop_bin_dir, hadoop_conf_dir, and HdfsDirectory as a partial function.
  """
 
  component_tar_source_file, component_tar_destination_folder = source, dest
 
  if not os.path.exists(component_tar_source_file):
    Logger.warning("Could not find file: %s" % str(component_tar_source_file))
    return 1
 
  # Ubuntu returns: "stdin: is not a tty", as subprocess output.
  tmpfile = tempfile.NamedTemporaryFile()
  out = None
  with open(tmpfile.name, 'r+') as file:
    get_hdp_version_cmd = '/usr/bin/hdp-select status %s > %s' % (hdp_select_component_name, tmpfile.name)
    code, stdoutdata = shell.call(get_hdp_version_cmd)
    out = file.read()
  pass
  if code != 0 or out is None:
    Logger.warning("Could not verify HDP version by calling '%s'. Return Code: %s, Output: %s." %
                   (get_hdp_version_cmd, str(code), str(out)))
    return 1
 
  matches = re.findall(r"([\d\.]+\-\d+)", out)
  hdp_version = matches[0] if matches and len(matches) > 0 else None
 
  if not hdp_version:
    Logger.error("Could not parse HDP version from output of hdp-select: %s" % str(out))
    return 1
 
  file_name = os.path.basename(component_tar_source_file)
  destination_file = os.path.join(component_tar_destination_folder, file_name)
  destination_file = destination_file.replace("{{ hdp_stack_version }}", hdp_version)
 

  kinit_if_needed = ""
  if params.security_enabled:
    kinit_if_needed = format("{kinit_path_local} -kt {hdfs_user_keytab} {hdfs_principal_name};")
 
  if kinit_if_needed:
    Execute(kinit_if_needed,
            user=component_user,
            path='/bin'
    )
 
  source_and_dest_pairs = [(component_tar_source_file, destination_file), ]
  return _copy_files(source_and_dest_pairs, file_owner, group_owner, kinit_if_needed)

# See if hdfs path prefix is provided on the command line. If yes, use that value, if no
# use empty string as default.
hdfs_path_prefix = ""
if len(sys.argv) == 2:
    hdfs_path_prefix = sys.argv[1]

hadoop_conf_dir = params.hadoop_conf_dir
 
oozie_libext_dir = "/usr/hdp/current/oozie-server/libext"
oozie_home="/usr/hdp/current/oozie-server"
oozie_setup_sh="/usr/hdp/current/oozie-server/bin/oozie-setup.sh"
oozie_tmp_dir = "/var/tmp/oozie"
configure_cmds = []
configure_cmds.append(('tar','-xvf', oozie_home + '/oozie-sharelib.tar.gz','-C', oozie_home))
configure_cmds.append(('cp', "/usr/share/HDP-oozie/ext-2.2.zip", "/usr/hdp/current/oozie-server/libext"))
configure_cmds.append(('chown', 'oozie:hadoop', oozie_libext_dir + "/ext-2.2.zip"))
 
no_op_test = "ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps -p `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1"

with Environment() as env:
  env.set_params(params)

  File("/etc/oozie/conf/oozie-env.sh",
       owner=params.oozie_user,
       content=params.oozie_env_sh_template
  )

  hashcode_file = format("{oozie_home}/.hashcode")
  hashcode = hashlib.md5(format('{oozie_home}/oozie-sharelib.tar.gz')).hexdigest()
  skip_recreate_sharelib = format("test -f {hashcode_file} && test -d {oozie_home}/share && [[ `cat {hashcode_file}` == '{hashcode}' ]]")

  Execute( configure_cmds,
           not_if  = format("{no_op_test} || {skip_recreate_sharelib}"), 
           sudo = True,
           )
  Execute(format("cd {oozie_tmp_dir} && {oozie_setup_sh} prepare-war"),
    user = params.oozie_user,
    not_if  = format("{no_op_test} || {skip_recreate_sharelib}")
  )
  File(hashcode_file,
       content = hashcode,
       mode = 0644,
  )

  oozie_shared_lib = format("/usr/hdp/current/oozie-server/share")
  oozie_user = 'oozie'
  oozie_hdfs_user_dir = format("{hdfs_path_prefix}/user/{oozie_user}")
  kinit_if_needed = ''

  params.HdfsResource(format("{oozie_hdfs_user_dir}/share/"),
    action="delete_on_execute",
    type = 'directory'
  )
    
  params.HdfsResource(format("{oozie_hdfs_user_dir}/share"),
    action="create_on_execute",
    type = 'directory',
    mode=0755,
    recursive_chmod = True,
    owner=oozie_user,
    source = oozie_shared_lib,
  )

  print "Copying tarballs..."
  copy_tarballs_to_hdfs("/usr/hdp/current/hadoop-client/mapreduce.tar.gz", hdfs_path_prefix+"/hdp/apps/{{ hdp_stack_version }}/mapreduce/", 'hadoop-mapreduce-historyserver', params.mapred_user, params.hdfs_user, params.user_group)
  copy_tarballs_to_hdfs("/usr/hdp/current/tez-client/lib/tez.tar.gz", hdfs_path_prefix+"/hdp/apps/{{ hdp_stack_version }}/tez/", 'hadoop-mapreduce-historyserver', params.mapred_user, params.hdfs_user, params.user_group)
  copy_tarballs_to_hdfs("/usr/hdp/current/hive-client/hive.tar.gz", hdfs_path_prefix+"/hdp/apps/{{ hdp_stack_version }}/hive/", 'hadoop-mapreduce-historyserver', params.mapred_user, params.hdfs_user, params.user_group)
  copy_tarballs_to_hdfs("/usr/hdp/current/pig-client/pig.tar.gz", hdfs_path_prefix+"/hdp/apps/{{ hdp_stack_version }}/pig/", 'hadoop-mapreduce-historyserver', params.mapred_user, params.hdfs_user, params.user_group)
  copy_tarballs_to_hdfs("/usr/hdp/current/hadoop-mapreduce-client/hadoop-streaming.jar", hdfs_path_prefix+"/hdp/apps/{{ hdp_stack_version }}/mapreduce/", 'hadoop-mapreduce-historyserver', params.mapred_user, params.hdfs_user, params.user_group)
  copy_tarballs_to_hdfs("/usr/hdp/current/sqoop-client/sqoop.tar.gz", hdfs_path_prefix+"/hdp/apps/{{ hdp_stack_version }}/sqoop/", 'hadoop-mapreduce-historyserver', params.mapred_user, params.hdfs_user, params.user_group)

  
  # jar shouldn't be used before (read comment below)
  File(format("{ambari_libs_dir}/fast-hdfs-resource.jar"),
       mode=0644,
       content=StaticFile("/var/lib/ambari-agent/cache/stacks/HDP/2.0.6/hooks/before-START/files/fast-hdfs-resource.jar")
  )
  # Create everything in one jar call (this is fast).
  # (! Before everything should be executed with action="create_on_execute/delete_on_execute" for this time-optimization to work)
  params.HdfsResource(None, 
               logoutput=True,
               action="execute"
  )
  print "Completed tarball copy. Ambari preupload script completed."

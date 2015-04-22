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
# Before running the script please add a proper pythopath, by executing:
# export PYTHONPATH=/usr/lib/python2.6/site-packages 
import glob
from logging import thread
import sys
import os
import re
import tempfile
import time
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
from resource_management.libraries.resources.copy_from_local import CopyFromLocal
from resource_management.libraries.resources.execute_hadoop import ExecuteHadoop
from resource_management import Script
 
 
__all__ = ["copy_tarballs_to_hdfs", ]
 
"""
This file provides helper methods needed for the versioning of RPMs. Specifically, it does dynamic variable
interpretation to replace strings like {{ hdp_stack_version }}  where the value of the
variables cannot be determined ahead of time, but rather, depends on what files are found.
 
It assumes that {{ hdp_stack_version }} is constructed as ${major.minor.patch.rev}-${build_number}
E.g., 998.2.2.1.0-998
Please note that "-${build_number}" is optional.
"""
 
# These values must be the suffix of the properties in cluster-env.xml
TAR_SOURCE_SUFFIX = "_tar_source"
TAR_DESTINATION_FOLDER_SUFFIX = "_tar_destination_folder"
class params():
  hdfs_user = "hdfs"
  mapred_user ="mapred"
  hadoop_bin_dir="/usr/hdp/current/hadoop-client/bin"
  hadoop_conf_dir = "/etc/hadoop/conf"
  user_group = "hadoop"
  security_enabled = False
  oozie_user = "oozie"
  execute_path = "/usr/hdp/current/hadoop-client/bin"
 
params = params()
 
 
class HdfsDirectory(Resource):
  action = ForcedListArgument()
 
  dir_name = ResourceArgument(default=lambda obj: obj.name)
  owner = ResourceArgument()
  group = ResourceArgument()
  mode = ResourceArgument()
  recursive_chown = BooleanArgument(default=False)
  recursive_chmod = BooleanArgument(default=False)
 
  conf_dir = ResourceArgument()
  security_enabled = BooleanArgument(default=False)
  keytab = ResourceArgument()
  kinit_path_local = ResourceArgument()
  hdfs_user = ResourceArgument()
  bin_dir = ResourceArgument(default="")
 
  #action 'create' immediately creates all pending directory in efficient manner
  #action 'create_delayed' add directory to list of pending directories
  actions = Resource.actions + ["create","create_delayed"]
 
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
 
  return_value = 1
  if source_and_dest_pairs and len(source_and_dest_pairs) > 0:
    return_value = 0
    for (source, destination) in source_and_dest_pairs:
      try:
        destination_dir = os.path.dirname(destination)
 
        HdfsDirectory(destination_dir,
                      action="create",
                      owner=file_owner,
                      mode=0555,
                      conf_dir=params.hadoop_conf_dir,
                      hdfs_user=params.hdfs_user,
        )
 
        CopyFromLocal(source,
                      mode=0444,
                      owner=file_owner,
                      group=group_owner,
                      dest_dir=destination_dir,
                      kinnit_if_needed=kinit_if_needed,
                      hdfs_user=params.hdfs_user,
                      hadoop_bin_dir=params.hadoop_bin_dir,
                      hadoop_conf_dir=params.hadoop_conf_dir
        )
      except:
        return_value = 1
  return return_value
 
 
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

  #Check if destination folder already exists
  does_hdfs_dir_exist = False
  does_hdfs_file_exist_cmd = "fs -ls %s" % os.path.dirname(destination_file)
  try:
    ExecuteHadoop(does_hdfs_file_exist_cmd,
                  user=component_user,
                  logoutput=True,
                  conf_dir=params.hadoop_conf_dir,
                  bin_dir=params.hadoop_bin_dir
    )
    does_hdfs_dir_exist = True
  except Fail:
    pass

  does_hdfs_file_exist_cmd = "fs -ls %s" % destination_file
  does_hdfs_file_exist = False
  try:
    ExecuteHadoop(does_hdfs_file_exist_cmd,
                  user=component_user,
                  logoutput=True,
                  conf_dir=params.hadoop_conf_dir,
                  bin_dir=params.hadoop_bin_dir
    )
    does_hdfs_file_exist = True
  except Fail:
    pass
 
  if not does_hdfs_file_exist and not does_hdfs_dir_exist:
    source_and_dest_pairs = [(component_tar_source_file, destination_file), ]
    return _copy_files(source_and_dest_pairs, file_owner, group_owner, kinit_if_needed)
  return 1
 
def getPropertyValueFromConfigXMLFile(xmlfile, name, defaultValue=None):
  xmldoc = minidom.parse(xmlfile)
  propNodes = [node.parentNode for node in xmldoc.getElementsByTagName("name") if node.childNodes[0].nodeValue == name]
  if len(propNodes) > 0:
    for node in propNodes[-1].childNodes:
      if node.nodeName == "value":
        if len(node.childNodes) > 0:
          return node.childNodes[0].nodeValue
        else:
          return ''
  return defaultValue

# See if hdfs path prefix is provided on the command line. If yes, use that value, if no
# use empty string as default.
hdfs_path_prefix = ""
if len(sys.argv) == 2:
    hdfs_path_prefix = sys.argv[1]

hadoop_conf_dir = params.hadoop_conf_dir
fsdefaultName =  getPropertyValueFromConfigXMLFile("/etc/hadoop/conf/core-site.xml", "fs.defaultFS")
if fsdefaultName is None:
  fsdefaultName = "fake"
 
while (not fsdefaultName.startswith("wasb://")):
  fsdefaultName =  getPropertyValueFromConfigXMLFile("/etc/hadoop/conf/core-site.xml", "fs.defaultFS")
  if fsdefaultName is None:
    fsdefaultName = "fake"
  time.sleep(10)
 
fs_root = fsdefaultName
 
oozie_libext_dir = "/usr/hdp/current/oozie-server/libext"
oozie_home="/usr/hdp/current/oozie-server"
configure_cmds = []
configure_cmds.append(('tar','-xvf', oozie_home + '/oozie-sharelib.tar.gz','-C', oozie_home))
configure_cmds.append(('cp', "/usr/share/HDP-oozie/ext-2.2.zip", "/usr/hdp/current/oozie-server/libext"))
configure_cmds.append(('chown', 'oozie:hadoop', oozie_libext_dir + "/ext-2.2.zip"))
 
no_op_test = "ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps -p `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1"
with Environment() as env:
  Execute( configure_cmds,
           not_if  = no_op_test,
           sudo = True,
           )

  oozie_shared_lib = format("/usr/hdp/current/oozie-server/share")
  oozie_user = 'oozie'
  oozie_hdfs_user_dir = format("{hdfs_path_prefix}/user/{oozie_user}")
  kinit_if_needed = ''

  #Ideally, we would want to run: put_shared_lib_to_hdfs_cmd = format("{oozie_setup_sh} sharelib create -fs {fs_root} -locallib {oozie_shared_lib}")
  #However given that oozie_setup_sh does not support an arbitrary hdfs path prefix, we are simulating the same command below
  put_shared_lib_to_hdfs_cmd = format("hadoop --config {hadoop_conf_dir} dfs -copyFromLocal {oozie_shared_lib}/lib/** {oozie_hdfs_user_dir}/share/lib/lib_20150212065327")

  oozie_cmd = format("{put_shared_lib_to_hdfs_cmd} ; hadoop --config {hadoop_conf_dir} dfs -chmod -R 755 {oozie_hdfs_user_dir}/share")

  #Check if destination folder already exists
  does_hdfs_file_exist_cmd = "fs -ls %s" % format("{oozie_hdfs_user_dir}/share")
  try:
    ExecuteHadoop(does_hdfs_file_exist_cmd,
                  user=oozie_user,
                  logoutput=True,
                  conf_dir=params.hadoop_conf_dir,
                  bin_dir=params.hadoop_bin_dir
    )
  except Fail:
    #If dir does not exist create it and put files there
    HdfsDirectory(format("{oozie_hdfs_user_dir}/share/lib/lib_20150212065327"),
                  action="create",
                  owner=oozie_user,
                  mode=0555,
                  conf_dir=params.hadoop_conf_dir,
                  hdfs_user=params.hdfs_user,
                  )
    Execute( oozie_cmd, user = params.oozie_user, not_if = None,
             path = params.execute_path )

  copy_tarballs_to_hdfs("/usr/hdp/current/hadoop-client/mapreduce.tar.gz", hdfs_path_prefix+"/hdp/apps/{{ hdp_stack_version }}/mapreduce/", 'hadoop-mapreduce-historyserver', params.mapred_user, params.hdfs_user, params.user_group)
  copy_tarballs_to_hdfs("/usr/hdp/current/tez-client/lib/tez.tar.gz", hdfs_path_prefix+"/hdp/apps/{{ hdp_stack_version }}/tez/", 'hadoop-mapreduce-historyserver', params.mapred_user, params.hdfs_user, params.user_group)
  copy_tarballs_to_hdfs("/usr/hdp/current/hive-client/hive.tar.gz", hdfs_path_prefix+"/hdp/apps/{{ hdp_stack_version }}/hive/", 'hadoop-mapreduce-historyserver', params.mapred_user, params.hdfs_user, params.user_group)
  copy_tarballs_to_hdfs("/usr/hdp/current/pig-client/pig.tar.gz", hdfs_path_prefix+"/hdp/apps/{{ hdp_stack_version }}/pig/", 'hadoop-mapreduce-historyserver', params.mapred_user, params.hdfs_user, params.user_group)
  copy_tarballs_to_hdfs("/usr/hdp/current/hadoop-mapreduce-client/hadoop-streaming.jar", hdfs_path_prefix+"/hdp/apps/{{ hdp_stack_version }}/mapreduce/", 'hadoop-mapreduce-historyserver', params.mapred_user, params.hdfs_user, params.user_group)
  copy_tarballs_to_hdfs("/usr/hdp/current/sqoop-client/sqoop.tar.gz", hdfs_path_prefix+"/hdp/apps/{{ hdp_stack_version }}/sqoop/", 'hadoop-mapreduce-historyserver', params.mapred_user, params.hdfs_user, params.user_group)

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
from optparse import OptionParser
os.environ["PATH"] += os.pathsep + "/var/lib/ambari-agent"
sys.path.append("/usr/lib/python2.6/site-packages")

import glob
from logging import thread
import re
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
from resource_management.core.resources.system import Execute, Directory
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.oozie_prepare_war import prepare_war
from resource_management.libraries.resources.hdfs_resource import HdfsResource
from resource_management.libraries.resources.execute_hadoop import ExecuteHadoop
from resource_management import Script

SQL_DRIVER_PATH = "/var/lib/ambari-server/resources/sqljdbc41.jar"
 
"""
This file provides helper methods needed for the versioning of RPMs. Specifically, it does dynamic variable
interpretation to replace strings like {{ stack_version_formatted }}  where the value of the
variables cannot be determined ahead of time, but rather, depends on what files are found.
 
It assumes that {{ stack_version_formatted }} is constructed as ${major.minor.patch.rev}-${build_number}
E.g., 998.2.2.1.0-998
Please note that "-${build_number}" is optional.
"""

with Environment() as env:
  def get_stack_version():
    if not options.hdp_version:
      # Ubuntu returns: "stdin: is not a tty", as subprocess output.
      tmpfile = tempfile.NamedTemporaryFile()
      out = None
      with open(tmpfile.name, 'r+') as file:
        get_stack_version_cmd = '/usr/bin/hdp-select status %s > %s' % ('hadoop-mapreduce-historyserver', tmpfile.name)
        code, stdoutdata = shell.call(get_stack_version_cmd)
        out = file.read()
      pass
      if code != 0 or out is None:
        Logger.warning("Could not verify HDP version by calling '%s'. Return Code: %s, Output: %s." %
                       (get_stack_version_cmd, str(code), str(out)))
        return 1
     
      matches = re.findall(r"([\d\.]+\-\d+)", out)
      stack_version = matches[0] if matches and len(matches) > 0 else None
     
      if not stack_version:
        Logger.error("Could not parse HDP version from output of hdp-select: %s" % str(out))
        return 1
    else:
      stack_version = options.hdp_version
      
    return stack_version
  
  parser = OptionParser()
  parser.add_option("-v", "--hdp-version", dest="hdp_version", default="",
                    help="hdp-version used in path of tarballs")
  parser.add_option("-u", "--upgrade", dest="upgrade", action="store_true",
                    help="flag to indicate script is being run for upgrade", default=False)  
  (options, args) = parser.parse_args()

  
  # See if hdfs path prefix is provided on the command line. If yes, use that value, if no
  # use empty string as default.
  hdfs_path_prefix = ""
  if len(args) > 0:
    hdfs_path_prefix = args[0]
  
  stack_version = get_stack_version()
  
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
     
    while True:
      fsdefaultName =  getPropertyValueFromConfigXMLFile("/etc/hadoop/conf/core-site.xml", "fs.defaultFS")
  
      if fsdefaultName and fsdefaultName.startswith("wasb://"):
        break
        
      print "Waiting to read appropriate value of fs.defaultFS from /etc/hadoop/conf/core-site.xml ..."
      time.sleep(10)
      pass
  
    print "Returning fs.defaultFS -> " + fsdefaultName
    return fsdefaultName
   
  # These values must be the suffix of the properties in cluster-env.xml
  TAR_SOURCE_SUFFIX = "_tar_source"
  TAR_DESTINATION_FOLDER_SUFFIX = "_tar_destination_folder"
  
  class params:
    hdfs_path_prefix = hdfs_path_prefix
    hdfs_user = "hdfs"
    mapred_user ="mapred"
    hadoop_bin_dir="/usr/hdp/" + stack_version + "/hadoop/bin"
    hadoop_conf_dir = "/etc/hadoop/conf"
    user_group = "hadoop"
    security_enabled = False
    oozie_user = "oozie"
    execute_path = "/usr/hdp/" + stack_version + "/hadoop/bin"
    ambari_libs_dir = "/var/lib/ambari-agent/lib"
    hdfs_site = ConfigDictionary({'dfs.webhdfs.enabled':False, 
    })
    fs_default = get_fs_root()
    oozie_secure = ''
    oozie_home="/usr/hdp/" + stack_version + "/oozie"
    oozie_setup_sh=format("/usr/hdp/" + stack_version + "/oozie/bin/oozie-setup.sh")
    oozie_setup_sh_current="/usr/hdp/current/oozie-server/bin/oozie-setup.sh"
    oozie_tmp_dir = "/var/tmp/oozie"
    oozie_libext_dir = "/usr/hdp/" + stack_version + "/oozie/libext"
    oozie_env_sh_template = \
  '''
  #!/bin/bash
  
  export OOZIE_CONFIG=${{OOZIE_CONFIG:-/usr/hdp/{0}/oozie/conf}}
  export OOZIE_DATA=${{OOZIE_DATA:-/var/lib/oozie/data}}
  export OOZIE_LOG=${{OOZIE_LOG:-/var/log/oozie}}
  export CATALINA_BASE=${{CATALINA_BASE:-/usr/hdp/{0}/oozie/oozie-server}}
  export CATALINA_TMPDIR=${{CATALINA_TMPDIR:-/var/tmp/oozie}}
  export CATALINA_PID=${{CATALINA_PID:-/var/run/oozie/oozie.pid}}
  export OOZIE_CATALINA_HOME=/usr/lib/bigtop-tomcat
  '''.format(stack_version)
    
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
      default_fs = fs_default,
      hdfs_resource_ignore_file = "/var/lib/ambari-agent/data/.hdfs_resource_ignore",
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
   
   
  def copy_tarballs_to_hdfs(source, dest, stack_select_component_name, component_user, file_owner, group_owner):
    """
    :param tarball_prefix: Prefix of the tarball must be one of tez, hive, mr, pig
    :param stack_select_component_name: Component name to get the status to determine the version
    :param component_user: User that will execute the Hadoop commands
    :param file_owner: Owner of the files copied to HDFS (typically hdfs account)
    :param group_owner: Group owner of the files copied to HDFS (typically hadoop group)
    :return: Returns 0 on success, 1 if no files were copied, and in some cases may raise an exception.
   
    In order to call this function, params.py must have all of the following,
    stack_version_formatted, kinit_path_local, security_enabled, hdfs_user, hdfs_principal_name, hdfs_user_keytab,
    hadoop_bin_dir, hadoop_conf_dir, and HdfsDirectory as a partial function.
    """
   
    component_tar_source_file, component_tar_destination_folder = source, dest
   
    if not os.path.exists(component_tar_source_file):
      Logger.warning("Could not find file: %s" % str(component_tar_source_file))
      return 1
   
  
   
    file_name = os.path.basename(component_tar_source_file)
    destination_file = os.path.join(component_tar_destination_folder, file_name)
    destination_file = destination_file.replace("{{ stack_version_formatted }}", stack_version)
   
  
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
  
  def createHdfsResources():
    print "Creating hdfs directories..."
    params.HdfsResource(format('{hdfs_path_prefix}/atshistory'), user='hdfs', change_permissions_for_parents=True, owner='yarn', group='hadoop', type='directory', action= ['create_on_execute'], mode=0755)
    params.HdfsResource(format('{hdfs_path_prefix}/user/hcat'), owner='hcat', type='directory', action=['create_on_execute'], mode=0755)
    params.HdfsResource(format('{hdfs_path_prefix}/hive/warehouse'), owner='hive', type='directory', action=['create_on_execute'], mode=0777)
    params.HdfsResource(format('{hdfs_path_prefix}/user/hive'), owner='hive', type='directory', action=['create_on_execute'], mode=0755)
    params.HdfsResource(format('{hdfs_path_prefix}/tmp'), mode=0777, action=['create_on_execute'], type='directory', owner='hdfs')
    params.HdfsResource(format('{hdfs_path_prefix}/user/ambari-qa'), type='directory', action=['create_on_execute'], mode=0770)
    params.HdfsResource(format('{hdfs_path_prefix}/user/oozie'), owner='oozie', type='directory', action=['create_on_execute'], mode=0775)
    params.HdfsResource(format('{hdfs_path_prefix}/app-logs'), recursive_chmod=True, owner='yarn', group='hadoop', type='directory', action=['create_on_execute'], mode=0777)
    params.HdfsResource(format('{hdfs_path_prefix}/tmp/entity-file-history/active'), owner='yarn', group='hadoop', type='directory', action=['create_on_execute'])
    params.HdfsResource(format('{hdfs_path_prefix}/mapred'), owner='mapred', type='directory', action=['create_on_execute'])
    params.HdfsResource(format('{hdfs_path_prefix}/mapred/system'), owner='hdfs', type='directory', action=['create_on_execute'])
    params.HdfsResource(format('{hdfs_path_prefix}/mr-history/done'), change_permissions_for_parents=True, owner='mapred', group='hadoop', type='directory', action=['create_on_execute'], mode=0777)
    params.HdfsResource(format('{hdfs_path_prefix}/atshistory/done'), owner='yarn', group='hadoop', type='directory', action=['create_on_execute'], mode=0700)
    params.HdfsResource(format('{hdfs_path_prefix}/atshistory/active'), owner='yarn', group='hadoop', type='directory', action=['create_on_execute'], mode=01777)
    params.HdfsResource(format('{hdfs_path_prefix}/ams/hbase'), owner='ams', type='directory', action=['create_on_execute'], mode=0775)
    params.HdfsResource(format('{hdfs_path_prefix}/amshbase/staging'), owner='ams', type='directory', action=['create_on_execute'], mode=0711)
    params.HdfsResource(format('{hdfs_path_prefix}/user/ams/hbase'), owner='ams', type='directory', action=['create_on_execute'], mode=0775)
    params.HdfsResource(format('{hdfs_path_prefix}/hdp'), owner='hdfs', type='directory', action=['create_on_execute'], mode=0755)
    params.HdfsResource(format('{hdfs_path_prefix}/user/spark'), owner='spark', group='hadoop', type='directory', action=['create_on_execute'], mode=0775)
    params.HdfsResource(format('{hdfs_path_prefix}/user/livy'), owner='livy', group='hadoop', type='directory', action=['create_on_execute'], mode=0775)
    params.HdfsResource(format('{hdfs_path_prefix}/hdp/spark-events'), owner='spark', group='hadoop', type='directory', action=['create_on_execute'], mode=0777)
    params.HdfsResource(format('{hdfs_path_prefix}/hdp/spark2-events'), owner='spark', group='hadoop', type='directory', action=['create_on_execute'], mode=0777)
    params.HdfsResource(format('{hdfs_path_prefix}/hbase'), owner='hbase', type='directory', action=['create_on_execute'])
    params.HdfsResource(format('{hdfs_path_prefix}/apps/hbase/staging'), owner='hbase', type='directory', action=['create_on_execute'], mode=0711)
    params.HdfsResource(format('{hdfs_path_prefix}/user/hbase'), owner='hbase', type='directory', action=['create_on_execute'], mode=0755)
    params.HdfsResource(format('{hdfs_path_prefix}/apps/zeppelin'), owner='zeppelin', group='hadoop', type='directory', action=['create_on_execute'])
    params.HdfsResource(format('{hdfs_path_prefix}/user/zeppelin'), owner='zeppelin', group='hadoop', type='directory', action=['create_on_execute'])
    params.HdfsResource(format('{hdfs_path_prefix}/user/zeppelin/test'), owner='zeppelin', group='hadoop', type='directory', action=['create_on_execute'])

  def copy_zeppelin_dependencies_to_hdfs(file_pattern):
    spark_deps_full_path = glob.glob(file_pattern)
    if spark_deps_full_path and os.path.exists(spark_deps_full_path[0]):
      copy_tarballs_to_hdfs(spark_deps_full_path[0], hdfs_path_prefix+'/apps/zeppelin/', 'hadoop-mapreduce-historyserver', params.hdfs_user, 'zeppelin', 'zeppelin')
    else:
      Logger.info('zeppelin-spark-dependencies not found at %s.' % file_pattern)

  def putCreatedHdfsResourcesToIgnore(env):
    if not 'hdfs_files' in env.config:
      Logger.info("Not creating .hdfs_resource_ignore as no resources to use.")
      return
    
    file_content = ""
    for file in env.config['hdfs_files']:
      if not file['target'].startswith(hdfs_path_prefix):
        raise Exception("Something created outside hdfs_path_prefix!")
      file_content += file['target'][len(hdfs_path_prefix):]
      file_content += "\n"
      
    with open("/var/lib/ambari-agent/data/.hdfs_resource_ignore", "a+") as fp:
      fp.write(file_content)
      
  def putSQLDriverToOozieShared():
    params.HdfsResource(hdfs_path_prefix + '/user/oozie/share/lib/sqoop/{0}'.format(os.path.basename(SQL_DRIVER_PATH)),
                        owner='hdfs', type='file', action=['create_on_execute'], mode=0644, source=SQL_DRIVER_PATH)
      
  env.set_params(params)
  hadoop_conf_dir = params.hadoop_conf_dir
   
  oozie_libext_dir = params.oozie_libext_dir
  sql_driver_filename = os.path.basename(SQL_DRIVER_PATH)
  oozie_home=params.oozie_home
  configure_cmds = []
  configure_cmds.append(('tar','-xvf', oozie_home + '/oozie-sharelib.tar.gz','-C', oozie_home))
  configure_cmds.append(('cp', "/usr/share/HDP-oozie/ext-2.2.zip", SQL_DRIVER_PATH, oozie_libext_dir))
  configure_cmds.append(('chown', 'oozie:hadoop', oozie_libext_dir + "/ext-2.2.zip", oozie_libext_dir + "/" + sql_driver_filename))
   
  no_op_test = "ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps -p `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1"

  File("/etc/oozie/conf/oozie-env.sh",
       owner=params.oozie_user,
       content=params.oozie_env_sh_template
  )

  hashcode_file = format("{oozie_home}/.hashcode")
  skip_recreate_sharelib = format("test -f {hashcode_file} && test -d {oozie_home}/share")

  Execute( configure_cmds,
           not_if  = format("{no_op_test} || {skip_recreate_sharelib}"), 
           sudo = True,
           )
  
  File(hashcode_file,
       mode = 0644,
  )
  
  prepare_war(params)

  oozie_shared_lib = format("/usr/hdp/{stack_version}/oozie/share")
  oozie_user = 'oozie'
  oozie_hdfs_user_dir = format("{hdfs_path_prefix}/user/{oozie_user}")
  kinit_if_needed = ''

  if options.upgrade:
    Logger.info("Skipping uploading oozie shared lib during upgrade")
  else:
    params.HdfsResource(format("{oozie_hdfs_user_dir}/share/"),
      action="delete_on_execute",
      type = 'directory'
    )

    spark_client_dir = format("/usr/hdp/{stack_version}/spark")

    if os.path.exists(spark_client_dir):
      try:
        # Rename /usr/hdp/{stack_version}/oozie/share/lib/spark to spark-orig
        if not os.path.exists(format("{oozie_shared_lib}/lib/spark-orig")):
          Execute(("mv",
                   format("{oozie_shared_lib}/lib/spark"),
                   format("{oozie_shared_lib}/lib/spark-orig")),
                   sudo=True)

        # Create /usr/hdp/{stack_version}/oozie/share/lib/spark
        if not os.path.exists(format("{oozie_shared_lib}/lib/spark")):
          Execute(('mkdir', format('{oozie_shared_lib}/lib/spark')),
                sudo=True)

        # Copy oozie-sharelib-spark from /usr/hdp/{stack_version}/oozie/share/lib/spark-orig to spark
        Execute(format("cp -f {oozie_shared_lib}/lib/spark-orig/oozie-sharelib-spark*.jar {oozie_shared_lib}/lib/spark"))

        # Copy /usr/hdp/{stack_version}/spark-client/*.jar except spark-examples*.jar
        Execute(format("cp -P {spark_client_dir}/lib/*.jar {oozie_shared_lib}/lib/spark"))
        Execute(format("find {oozie_shared_lib}/lib/spark/ -type l -delete"))
        try:
          Execute(format("rm -f {oozie_shared_lib}/lib/spark/spark-examples*.jar"))
        except:
          print "No spark-examples jar files found in Spark client lib."

        # Copy /usr/hdp/{stack_version}/spark-client/python/lib/*.zip & *.jar to /usr/hdp/{stack_version}/oozie/share/lib/spark
        Execute(format("cp -f {spark_client_dir}/python/lib/*.zip {oozie_shared_lib}/lib/spark"))

        try:
          Execute(format("cp -f {spark_client_dir}/python/lib/*.jar {oozie_shared_lib}/lib/spark"))
        except:
          print "No jar files found in Spark client python lib."

        Execute(("chmod", "-R", "0755", format('{oozie_shared_lib}/lib/spark')),
                sudo=True)

        # Skipping this step since it might cause issues to automated scripts that rely on hdfs://user/oozie/share/lib
        # Rename /usr/hdp/{stack_version}/oozie/share/lib to lib_ts
        # millis = int(round(time.time() * 1000))
        # Execute(("mv",
        #          format("{oozie_shared_lib}/lib"),
        #          format("{oozie_shared_lib}/lib_{millis}")),
        #         sudo=True)
      except Exception, e:
        print 'Exception occurred while preparing oozie share lib: '+ repr(e)

    params.HdfsResource(format("{oozie_hdfs_user_dir}/share"),
      action="create_on_execute",
      type = 'directory',
      mode=0755,
      recursive_chmod = True,
      owner=oozie_user,
      source = oozie_shared_lib,
    )

  print "Copying tarballs..."
  # TODO, these shouldn't hardcode the stack root or destination stack name.
  copy_tarballs_to_hdfs(format("/usr/hdp/{stack_version}/hadoop/mapreduce.tar.gz"), hdfs_path_prefix+"/hdp/apps/{{ stack_version_formatted }}/mapreduce/", 'hadoop-mapreduce-historyserver', params.mapred_user, params.hdfs_user, params.user_group)
  copy_tarballs_to_hdfs(format("/usr/hdp/{stack_version}/tez/lib/tez.tar.gz"), hdfs_path_prefix+"/hdp/apps/{{ stack_version_formatted }}/tez/", 'hadoop-mapreduce-historyserver', params.mapred_user, params.hdfs_user, params.user_group)
  copy_tarballs_to_hdfs(format("/usr/hdp/{stack_version}/hive/hive.tar.gz"), hdfs_path_prefix+"/hdp/apps/{{ stack_version_formatted }}/hive/", 'hadoop-mapreduce-historyserver', params.mapred_user, params.hdfs_user, params.user_group)

  # Needed by Hive Interactive
  copy_tarballs_to_hdfs(format("/usr/hdp/{stack_version}/tez_hive2/lib/tez.tar.gz"), hdfs_path_prefix+"/hdp/apps/{{ stack_version_formatted }}/tez_hive2/", 'hadoop-mapreduce-historyserver', params.mapred_user, params.hdfs_user, params.user_group)

  copy_tarballs_to_hdfs(format("/usr/hdp/{stack_version}/pig/pig.tar.gz"), hdfs_path_prefix+"/hdp/apps/{{ stack_version_formatted }}/pig/", 'hadoop-mapreduce-historyserver', params.mapred_user, params.hdfs_user, params.user_group)
  copy_tarballs_to_hdfs(format("/usr/hdp/{stack_version}/hadoop-mapreduce/hadoop-streaming.jar"), hdfs_path_prefix+"/hdp/apps/{{ stack_version_formatted }}/mapreduce/", 'hadoop-mapreduce-historyserver', params.mapred_user, params.hdfs_user, params.user_group)
  copy_tarballs_to_hdfs(format("/usr/hdp/{stack_version}/sqoop/sqoop.tar.gz"), hdfs_path_prefix+"/hdp/apps/{{ stack_version_formatted }}/sqoop/", 'hadoop-mapreduce-historyserver', params.mapred_user, params.hdfs_user, params.user_group)
  
  createHdfsResources()
  copy_zeppelin_dependencies_to_hdfs(format("/usr/hdp/{stack_version}/zeppelin/interpreter/spark/dep/zeppelin-spark-dependencies*.jar"))
  putSQLDriverToOozieShared()
  putCreatedHdfsResourcesToIgnore(env)

  # jar shouldn't be used before (read comment below)
  File(format("{ambari_libs_dir}/fast-hdfs-resource.jar"),
       mode=0644,
       content=StaticFile("/var/lib/ambari-agent/cache/stacks/HDP/2.0.6/hooks/before-START/files/fast-hdfs-resource.jar")
  )
  # Create everything in one jar call (this is fast).
  # (! Before everything should be executed with action="create_on_execute/delete_on_execute" for this time-optimization to work)
  try:
    params.HdfsResource(None, 
                 logoutput=True,
                 action="execute"
    )
  except:
    os.remove("/var/lib/ambari-agent/data/.hdfs_resource_ignore")
    raise
  print "Completed tarball copy."

  if not options.upgrade:
    print "Executing stack-selector-tool for stack {0} ...".format(stack_version)
    Execute(
      ('/usr/bin/hdp-select', 'set', 'all', stack_version),
      sudo = True
    )

  print "Ambari preupload script completed."
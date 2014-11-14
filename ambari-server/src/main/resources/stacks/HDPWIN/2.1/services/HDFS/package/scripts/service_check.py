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
from resource_management.libraries import functions

class HdfsServiceCheck(Script):
  def service_check(self, env):
    import params
    env.set_params(params)

    unique = functions.get_unique_id_and_date()

    #Hadoop uses POSIX-style paths, separator is always /
    dir = '/tmp'
    tmp_file = dir + '/' + unique

    #commands for execution
    hadoop_cmd = "cmd /C %s" % (os.path.join(params.hadoop_home, "bin", "hadoop.cmd"))
    create_dir_cmd = "%s fs -mkdir %s" % (hadoop_cmd, dir)
    own_dir = "%s fs -chmod 777 %s" % (hadoop_cmd, dir)
    test_dir_exists = "%s fs -test -e %s" % (hadoop_cmd, dir)
    cleanup_cmd = "%s fs -rm %s" % (hadoop_cmd, tmp_file)
    create_file_cmd = "%s fs -put %s %s" % (hadoop_cmd, os.path.join(params.hadoop_conf_dir, "core-site.xml"), tmp_file)
    test_cmd = "%s fs -test -e %s" % (hadoop_cmd, tmp_file)

    hdfs_cmd = "cmd /C %s" % (os.path.join(params.hadoop_home, "bin", "hdfs.cmd"))
    safemode_command = "%s dfsadmin -safemode get | %s OFF" % (hdfs_cmd, params.grep_exe)

    Execute(safemode_command, logoutput=True, try_sleep=3, tries=20)
    Execute(create_dir_cmd, user=params.hdfs_user,logoutput=True, ignore_failures=True)
    Execute(own_dir, user=params.hdfs_user,logoutput=True)
    Execute(test_dir_exists, user=params.hdfs_user,logoutput=True)
    Execute(create_file_cmd, user=params.hdfs_user,logoutput=True)
    Execute(test_cmd, user=params.hdfs_user,logoutput=True)
    Execute(cleanup_cmd, user=params.hdfs_user,logoutput=True)

if __name__ == "__main__":
  HdfsServiceCheck().execute()

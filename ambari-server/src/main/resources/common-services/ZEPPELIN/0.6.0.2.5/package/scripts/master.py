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

import glob
import grp
import os
import pwd
import sys
from resource_management.core.resources import Directory
from resource_management.core.resources.system import Execute, File
from resource_management.core.source import InlineTemplate
from resource_management.libraries import XmlConfig
from resource_management.libraries.functions.check_process_status import check_process_status
from resource_management.libraries.functions.format import format
from resource_management.libraries.script.script import Script

class Master(Script):
  def install(self, env):
    import params
    env.set_params(params)

    Execute('chmod +x ' + params.service_packagedir + "/scripts/setup_snapshot.sh")

    # Create user and group if they don't exist
    self.create_linux_user(params.zeppelin_user, params.zeppelin_group)
    self.install_packages(env)

    Execute('chown -R ' + params.zeppelin_user + ':' + params.zeppelin_group + ' ' + params.zeppelin_dir)

    # create the log, pid, zeppelin dirs
    Directory([params.zeppelin_pid_dir, params.zeppelin_log_dir, params.zeppelin_dir],
              owner=params.zeppelin_user,
              group=params.zeppelin_group,
              cd_access="a",
              mode=0755
              )

    Execute('echo spark_version:' + params.spark_version + ' detected for spark_home: '
            + params.spark_home + ' >> ' + params.zeppelin_log_file, user=params.zeppelin_user)

    # update the configs specified by user
    self.configure(env)

    # run setup_snapshot.sh
    Execute(format("{service_packagedir}/scripts/setup_snapshot.sh {zeppelin_dir} "
                   "{hive_metastore_host} {hive_metastore_port} {hive_server_port} "
                   "{zeppelin_host} {zeppelin_port} {setup_view} {service_packagedir} "
                   "{java64_home} >> {zeppelin_log_file}"),
            user=params.zeppelin_user)

  def create_linux_user(self, user, group):
    try:
      pwd.getpwnam(user)
    except KeyError:
      Execute('adduser ' + user)
    try:
      grp.getgrnam(group)
    except KeyError:
      Execute('groupadd ' + group)

  def create_zeppelin_dir(self, params):
    params.HdfsResource(format("/user/{zeppelin_user}"),
                        type="directory",
                        action="create_on_execute",
                        owner=params.zeppelin_user,
                        recursive_chown=True,
                        recursive_chmod=True
                        )
    params.HdfsResource(format("/user/{zeppelin_user}/test"),
                        type="directory",
                        action="create_on_execute",
                        owner=params.zeppelin_user,
                        recursive_chown=True,
                        recursive_chmod=True
                        )
    params.HdfsResource(format("/apps/zeppelin"),
                        type="directory",
                        action="create_on_execute",
                        owner=params.zeppelin_user,
                        recursive_chown=True,
                        recursive_chmod=True
                        )

    spark_deps_full_path = glob.glob(params.zeppelin_dir + '/interpreter/spark/dep/zeppelin-spark-dependencies-*.jar')[0]
    spark_dep_file_name = os.path.basename(spark_deps_full_path);

    params.HdfsResource(params.spark_jar_dir + "/" + spark_dep_file_name,
                        type="file",
                        action="create_on_execute",
                        source=spark_deps_full_path,
                        group=params.zeppelin_group,
                        owner=params.zeppelin_user,
                        mode=0444,
                        replace_existing_files=True,
                        )

    params.HdfsResource(None, action="execute")

  def configure(self, env):
    import params
    import status_params
    env.set_params(params)
    env.set_params(status_params)

    # write out zeppelin-site.xml
    XmlConfig("zeppelin-site.xml",
              conf_dir=params.conf_dir,
              configurations=params.config['configurations']['zeppelin-config'],
              owner=params.zeppelin_user,
              group=params.zeppelin_group
              )
    # write out zeppelin-env.sh
    env_content = InlineTemplate(params.zeppelin_env_content)
    File(format("{params.conf_dir}/zeppelin-env.sh"), content=env_content,
         owner=params.zeppelin_user, group=params.zeppelin_group)  # , mode=0777)

  def stop(self, env):
    import params
    Execute(params.zeppelin_dir + '/bin/zeppelin-daemon.sh stop >> ' + params.zeppelin_log_file,
            user=params.zeppelin_user)

  def start(self, env):
    import params
    import status_params
    self.configure(env)

    if glob.glob(
            params.zeppelin_dir + '/interpreter/spark/dep/zeppelin-spark-dependencies-*.jar') and os.path.exists(
      glob.glob(params.zeppelin_dir + '/interpreter/spark/dep/zeppelin-spark-dependencies-*.jar')[0]):
      self.create_zeppelin_dir(params)

    Execute(params.zeppelin_dir + '/bin/zeppelin-daemon.sh start >> '
            + params.zeppelin_log_file, user=params.zeppelin_user)
    pidfile = glob.glob(status_params.zeppelin_pid_dir
                        + '/zeppelin-' + params.zeppelin_user + '*.pid')[0]
    Execute('echo pid file is: ' + pidfile, user=params.zeppelin_user)
    contents = open(pidfile).read()
    Execute('echo pid is ' + contents, user=params.zeppelin_user)

    # if first_setup:
    import time
    time.sleep(20)
    self.update_zeppelin_interpreter()

  def status(self, env):
    import status_params
    env.set_params(status_params)

    pid_file = glob.glob(status_params.zeppelin_pid_dir + '/zeppelin-'
                         + status_params.zeppelin_user + '*.pid')[0]
    check_process_status(pid_file)

  def update_zeppelin_interpreter(self):
    import params
    import json, urllib2
    zeppelin_int_url = 'http://' + params.zeppelin_host + ':' + str(
      params.zeppelin_port) + '/api/interpreter/setting/'

    # fetch current interpreter settings for spark, hive, phoenix
    data = json.load(urllib2.urlopen(zeppelin_int_url))
    print data
    for body in data['body']:
      if body['group'] == 'spark':
        sparkbody = body
      elif body['group'] == 'hive':
        hivebody = body
      elif body['group'] == 'phoenix':
        phoenixbody = body

    # if hive installed, update hive settings and post to hive interpreter
    if (params.hive_server_host):
      hivebody['properties']['hive.hiveserver2.url'] = 'jdbc:hive2://' \
                                                       + params.hive_server_host \
                                                       + ':' + params.hive_server_port
      self.post_request(zeppelin_int_url + hivebody['id'], hivebody)

    # if hbase installed, update hbase settings and post to phoenix interpreter
    if (params.zookeeper_znode_parent and params.hbase_zookeeper_quorum):
      phoenixbody['properties'][
        'phoenix.jdbc.url'] = "jdbc:phoenix:" + params.hbase_zookeeper_quorum + ':' \
                              + params.zookeeper_znode_parent
      self.post_request(zeppelin_int_url + phoenixbody['id'], phoenixbody)

  def post_request(self, url, body):
    import json, urllib2
    encoded_body = json.dumps(body)
    req = urllib2.Request(str(url), encoded_body)
    req.get_method = lambda: 'PUT'
    try:
      response = urllib2.urlopen(req, encoded_body).read()
    except urllib2.HTTPError, error:
      print 'Exception: ' + error.read()

    jsonresp = json.loads(response.decode('utf-8'))
    print jsonresp['status']


if __name__ == "__main__":
  Master().execute()

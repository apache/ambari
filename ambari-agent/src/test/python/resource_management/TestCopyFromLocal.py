'''
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
'''

from unittest import TestCase
from mock.mock import patch
from resource_management import *

@patch.object(System, "os_family", new = 'redhat')
class TestCopyFromLocal(TestCase):

  @patch("resource_management.libraries.providers.execute_hadoop.ExecuteHadoopProvider")
  def test_run_default_args(self, execute_hadoop_mock):
    with Environment() as env:
      CopyFromLocal('/user/testdir/*.files',
        owner='user1',
        dest_dir='/apps/test/',
        kinnit_if_needed='',
        hdfs_user='hdfs'
      )
      self.assertEqual(execute_hadoop_mock.call_count, 2)
      call_arg_list = execute_hadoop_mock.call_args_list
      self.assertEqual('fs -copyFromLocal /user/testdir/*.files /apps/test/',
                       call_arg_list[0][0][0].command)
      self.assertEquals({'not_if': "su - user1 -c ' export PATH=$PATH:/usr/bin ; hadoop fs -ls /apps/test//*.files' >/dev/null 2>&1", 'user': 'user1', 'bin_dir': '/usr/bin', 'conf_dir': '/etc/hadoop/conf'},
                        call_arg_list[0][0][0].arguments)
      self.assertEquals('fs -chown user1 /apps/test//*.files', call_arg_list[1][0][0].command)
      self.assertEquals({'user': 'hdfs', 'bin_dir': '/usr/bin', 'conf_dir': '/etc/hadoop/conf'}, call_arg_list[1][0][0].arguments)


  @patch("resource_management.libraries.providers.execute_hadoop.ExecuteHadoopProvider")
  def test_run_with_chmod(self, execute_hadoop_mock):
    with Environment() as env:
      CopyFromLocal('/user/testdir/*.files',
        mode=0655,
        owner='user1',
        group='hdfs',
        dest_dir='/apps/test/',
        kinnit_if_needed='',
        hdfs_user='hdfs'
      )
      self.assertEqual(execute_hadoop_mock.call_count, 3)
      call_arg_list = execute_hadoop_mock.call_args_list
      self.assertEqual('fs -copyFromLocal /user/testdir/*.files /apps/test/',
                       call_arg_list[0][0][0].command)
      self.assertEquals({'not_if': "su - user1 -c ' export PATH=$PATH:/usr/bin ; hadoop fs -ls /apps/test//*.files' >/dev/null 2>&1", 'user': 'user1', 'bin_dir': '/usr/bin', 'conf_dir': '/etc/hadoop/conf'},
                        call_arg_list[0][0][0].arguments)
      self.assertEquals('fs -chown user1:hdfs /apps/test//*.files', call_arg_list[1][0][0].command)
      self.assertEquals({'user': 'hdfs', 'bin_dir': '/usr/bin', 'conf_dir': '/etc/hadoop/conf'}, call_arg_list[1][0][0].arguments)



#!/usr/bin/env python

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

from mock.mock import MagicMock, patch
from stacks.utils.RMFTestCase import *


class TestNagiosServer(RMFTestCase):
  def test_configure_default(self):
    self.executeScript("1.3.2/services/NAGIOS/package/scripts/nagios_server.py",
                       classname="NagiosServer",
                       command="configure",
                       config_file="default.json"
    )
    self.assert_configure_default()
    self.assertNoMoreResources()


  def test_start_default(self):
    self.executeScript(
      "1.3.2/services/NAGIOS/package/scripts/nagios_service.py",
      classname="NagiosServer",
      command="start",
      config_file="default.json"
    )
    self.assert_configure_default()
    self.assertResourceCalled('Execute', 'service nagios start',
                              path=['/usr/local/bin/:/bin/:/sbin/']
    )
    self.assertResourceCalled('MonitorWebserver', 'restart',
    )
    self.assertNoMoreResources()


  @patch('os.path.isfile')
  def test_stop_default(self, os_path_isfile_mock):
    src_dir = RMFTestCase._getSrcFolder()    
    os_path_isfile_mock.side_effect = [False, True]
       
    self.executeScript(
      "1.3.2/services/NAGIOS/package/scripts/nagios_service.py",
      classname="NagiosServer",
      command="stop",
      config_file="default.json"
    )
    
    self.assertResourceCalled('Execute','service nagios stop', path=['/usr/local/bin/:/bin/:/sbin/'])
    self.assertResourceCalled('Execute','rm -f /var/run/nagios/nagios.pid')
    self.assertResourceCalled('MonitorWebserver', 'restart')
    
    self.assertNoMoreResources()


  def assert_configure_default(self):
    self.assertResourceCalled('File', '/etc/apache2/conf.d/nagios.conf',
                              owner='nagios',
                              group='nagios',
                              content=Template("nagios.conf.j2"),
                              mode=0644
    )
    self.assertResourceCalled('Directory', '/etc/nagios',
                              owner='nagios',
                              group='nagios',
    )
    self.assertResourceCalled('Directory', '/usr/lib64/nagios/plugins'
    )
    self.assertResourceCalled('Directory', '/etc/nagios/objects'
    )
    self.assertResourceCalled('Directory', '/var/run/nagios',
                              owner='nagios',
                              group='nagios',
                              mode=0755,
                              recursive=True
    )
    self.assertResourceCalled('Directory', '/var/nagios',
                              owner='nagios',
                              group='nagios',
                              recursive=True
    )
    self.assertResourceCalled('Directory', '/var/nagios/spool/checkresults',
                              owner='nagios',
                              group='nagios',
                              recursive=True
    )
    self.assertResourceCalled('Directory', '/var/nagios/rw',
                              owner='nagios',
                              group='nagios',
                              recursive=True
    )
    self.assertResourceCalled('Directory', '/var/log/nagios',
                              owner='nagios',
                              group='nagios',
                              mode=0755
    )
    self.assertResourceCalled('Directory', '/var/log/nagios/archives',
                              owner='nagios',
                              group='nagios',
                              mode=0755
    )
    self.assertResourceCalled('TemplateConfig', '/etc/nagios/nagios.cfg',
                              owner='nagios',
                              group='nagios',
                              mode=None
    )
    self.assertResourceCalled('TemplateConfig', '/etc/nagios/resource.cfg',
                              owner='nagios',
                              group='nagios',
                              mode=None
    )
    self.assertResourceCalled('TemplateConfig',
                              '/etc/nagios/objects/hadoop-hosts.cfg',
                              owner='nagios',
                              group='hadoop',
                              mode=None
    )
    self.assertResourceCalled('TemplateConfig',
                              '/etc/nagios/objects/hadoop-hostgroups.cfg',
                              owner='nagios',
                              group='hadoop',
                              mode=None
    )
    self.assertResourceCalled('TemplateConfig',
                              '/etc/nagios/objects/hadoop-servicegroups.cfg',
                              owner='nagios',
                              group='hadoop',
                              mode=None
    )
    self.assertResourceCalled('TemplateConfig',
                              '/etc/nagios/objects/hadoop-services.cfg',
                              owner='nagios',
                              group='hadoop',
                              mode=None
    )
    self.assertResourceCalled('TemplateConfig',
                              '/etc/nagios/objects/hadoop-commands.cfg',
                              owner='nagios',
                              group='hadoop',
                              mode=None
    )
    self.assertResourceCalled('TemplateConfig',
                              '/etc/nagios/objects/contacts.cfg',
                              owner='nagios',
                              group='hadoop',
                              mode=None
    )
    self.assertResourceCalled('File', '/usr/lib64/nagios/plugins/check_cpu.pl',
                              content=StaticFile('check_cpu.pl'),
                              mode=0755
    )
    self.assertResourceCalled('File', '/usr/lib64/nagios/plugins/check_cpu.php',
                              content=StaticFile('check_cpu.php'),
                              mode=0755
    )
    self.assertResourceCalled('File', '/usr/lib64/nagios/plugins/check_cpu_ha.php',
                              content=StaticFile('check_cpu_ha.php'),
                              mode=0755
    )
    self.assertResourceCalled('File',
                              '/usr/lib64/nagios/plugins/check_datanode_storage.php',
                              content=StaticFile('check_datanode_storage.php'),
                              mode=0755
    )
    self.assertResourceCalled('File',
                              '/usr/lib64/nagios/plugins/check_aggregate.php',
                              content=StaticFile('check_aggregate.php'),
                              mode=0755
    )
    self.assertResourceCalled('File',
                              '/usr/lib64/nagios/plugins/check_hdfs_blocks.php',
                              content=StaticFile('check_hdfs_blocks.php'),
                              mode=0755
    )
    self.assertResourceCalled('File',
                              '/usr/lib64/nagios/plugins/check_hdfs_capacity.php',
                              content=StaticFile('check_hdfs_capacity.php'),
                              mode=0755
    )
    self.assertResourceCalled('File',
                              '/usr/lib64/nagios/plugins/check_rpcq_latency.php',
                              content=StaticFile('check_rpcq_latency.php'),
                              mode=0755
    )
    self.assertResourceCalled('File',
                              '/usr/lib64/nagios/plugins/check_webui.sh',
                              content=StaticFile('check_webui.sh'),
                              mode=0755
    )
    self.assertResourceCalled('File',
                              '/usr/lib64/nagios/plugins/check_name_dir_status.php',
                              content=StaticFile('check_name_dir_status.php'),
                              mode=0755
    )
    self.assertResourceCalled('File',
                              '/usr/lib64/nagios/plugins/check_oozie_status.sh',
                              content=StaticFile('check_oozie_status.sh'),
                              mode=0755
    )
    self.assertResourceCalled('File',
                              '/usr/lib64/nagios/plugins/check_templeton_status.sh',
                              content=StaticFile('check_templeton_status.sh'),
                              mode=0755
    )
    self.assertResourceCalled('File',
                              '/usr/lib64/nagios/plugins/check_hive_metastore_status.sh',
                              content=StaticFile(
                                'check_hive_metastore_status.sh'),
                              mode=0755
    )
    self.assertResourceCalled('File',
                              '/usr/lib64/nagios/plugins/check_hue_status.sh',
                              content=StaticFile('check_hue_status.sh'),
                              mode=0755
    )
    self.assertResourceCalled('File',
                              '/usr/lib64/nagios/plugins/check_mapred_local_dir_used.sh',
                              content=StaticFile(
                                'check_mapred_local_dir_used.sh'),
                              mode=0755
    )
    self.assertResourceCalled('File',
                              '/usr/lib64/nagios/plugins/check_nodemanager_health.sh',
                              content=StaticFile('check_nodemanager_health.sh'),
                              mode=0755
    )
    self.assertResourceCalled('File',
                              '/usr/lib64/nagios/plugins/check_namenodes_ha.sh',
                              content=StaticFile('check_namenodes_ha.sh'),
                              mode=0755
    )
    self.assertResourceCalled('File',
                              '/usr/lib64/nagios/plugins/hdp_nagios_init.php',
                              content=StaticFile('hdp_nagios_init.php'),
                              mode=0755
    )
    self.assertResourceCalled('File',
                              '/usr/lib64/nagios/plugins/mm_wrapper.py',
                              content=StaticFile('mm_wrapper.py'),
                              mode=0755
    )
    self.assertResourceCalled('File',
                              '/usr/lib64/nagios/plugins/check_hive_thrift_port.py',
                              content=StaticFile('check_hive_thrift_port.py'),
                              mode=0755
    )
    self.assertResourceCalled('Execute',
                              'htpasswd2 -c -b  /etc/nagios/htpasswd.users nagiosadmin \'!`"\'"\'"\' 1\''
    )

    self.assertResourceCalled('File', '/etc/nagios/htpasswd.users',
                              owner='nagios',
                              group='nagios',
                              mode=0640
    )
    self.assertResourceCalled('Execute', 'usermod -G nagios wwwrun'
    )

    self.assertResourceCalled('File', '/etc/nagios/command.cfg',
                              owner='nagios',
                              group='nagios'
    )
    self.assertResourceCalled('File', '/var/nagios/ignore.dat',
                              owner='nagios',
                              group='nagios',
                              mode=0664
    )

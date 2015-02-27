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

from stacks.utils.RMFTestCase import *


class TestGangliaMonitor(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "GANGLIA/3.5.0/package"
  STACK_VERSION = "2.0.6"
  
  def test_configure_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ganglia_monitor.py",
                       classname="GangliaMonitor",
                       command="configure",
                       config_file="default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()
    self.assert_gmond_master_conf_generated()
    self.assertNoMoreResources()


  def test_configure_non_gmetad_node(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ganglia_monitor.py",
                       classname="GangliaMonitor",
                       command="configure",
                       config_file="default.non_gmetad_host.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()
    self.assertNoMoreResources()


  def test_start_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ganglia_monitor.py",
                       classname="GangliaMonitor",
                       command="start",
                       config_file="default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()
    self.assert_gmond_master_conf_generated()
    self.assertResourceCalled('Execute', 'ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E service hdp-gmond start >> /tmp/gmond.log  2>&1 ; /bin/ps auwx | /bin/grep [g]mond  >> /tmp/gmond.log  2>&1',
        path = ['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'],
    )
    self.assertNoMoreResources()


  def test_stop_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ganglia_monitor.py",
                       classname="GangliaMonitor",
                       command="stop",
                       config_file="default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Execute', 'ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E service hdp-gmond stop >> /tmp/gmond.log  2>&1 ; /bin/ps auwx | /bin/grep [g]mond  >> /tmp/gmond.log  2>&1',
        path = ['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'],
    )
    self.assertNoMoreResources()


  def test_install_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ganglia_monitor.py",
                       classname="GangliaMonitor",
                       command="install",
                       config_file="default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()
    self.assert_gmond_master_conf_generated()
    self.assertResourceCalled('Execute', ('chkconfig', 'gmond', 'off'),
        path = ['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'],
        sudo = True,
    )
    self.assertResourceCalled('Execute', ('chkconfig', 'gmetad', 'off'),
        path = ['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'],
        sudo = True,
    )
    self.assertNoMoreResources()


  def assert_configure_default(self):
    self.assertResourceCalled('Directory', '/etc/ganglia/hdp',
                              owner = 'root',
                              group = 'hadoop',
                              recursive = True,
                              )
    self.assertResourceCalled('Directory', '/usr/libexec/hdp/ganglia',
                              owner = 'root',
                              group = 'root',
                              recursive = True,
                              )
    self.assertResourceCalled('File', '/etc/init.d/hdp-gmetad',
                              content = StaticFile('gmetad.init'),
                              mode = 0755,
                              )
    self.assertResourceCalled('File', '/etc/init.d/hdp-gmond',
                              content = StaticFile('gmond.init'),
                              mode = 0755,
                              )
    self.assertResourceCalled('File', '/usr/libexec/hdp/ganglia/checkGmond.sh',
                              content = StaticFile('checkGmond.sh'),
                              mode = 0755,
                              )
    self.assertResourceCalled('File', '/usr/libexec/hdp/ganglia/checkRrdcached.sh',
                              content = StaticFile('checkRrdcached.sh'),
                              mode = 0755,
                              )
    self.assertResourceCalled('File', '/usr/libexec/hdp/ganglia/gmetadLib.sh',
                              content = StaticFile('gmetadLib.sh'),
                              mode = 0755,
                              )
    self.assertResourceCalled('File', '/usr/libexec/hdp/ganglia/gmondLib.sh',
                              content = StaticFile('gmondLib.sh'),
                              mode = 0755,
                              )
    self.assertResourceCalled('File', '/usr/libexec/hdp/ganglia/rrdcachedLib.sh',
                              content = StaticFile('rrdcachedLib.sh'),
                              mode = 0755,
                              )
    self.assertResourceCalled('File', '/usr/libexec/hdp/ganglia/setupGanglia.sh',
                              content = StaticFile('setupGanglia.sh'),
                              mode = 0755,
                              )
    self.assertResourceCalled('File', '/usr/libexec/hdp/ganglia/startGmetad.sh',
                              content = StaticFile('startGmetad.sh'),
                              mode = 0755,
                              )
    self.assertResourceCalled('File', '/usr/libexec/hdp/ganglia/startGmond.sh',
                              content = StaticFile('startGmond.sh'),
                              mode = 0755,
                              )
    self.assertResourceCalled('File', '/usr/libexec/hdp/ganglia/startRrdcached.sh',
                              content = StaticFile('startRrdcached.sh'),
                              mode = 0755,
                              )
    self.assertResourceCalled('File', '/usr/libexec/hdp/ganglia/stopGmetad.sh',
                              content = StaticFile('stopGmetad.sh'),
                              mode = 0755,
                              )
    self.assertResourceCalled('File', '/usr/libexec/hdp/ganglia/stopGmond.sh',
                              content = StaticFile('stopGmond.sh'),
                              mode = 0755,
                              )
    self.assertResourceCalled('File', '/usr/libexec/hdp/ganglia/stopRrdcached.sh',
                              content = StaticFile('stopRrdcached.sh'),
                              mode = 0755,
                              )
    self.assertResourceCalled('File', '/usr/libexec/hdp/ganglia/teardownGanglia.sh',
                              content = StaticFile('teardownGanglia.sh'),
                              mode = 0755,
                              )
    self.assertResourceCalled('TemplateConfig', '/usr/libexec/hdp/ganglia/gangliaClusters.conf',
                              owner = 'root',
                              template_tag = None,
                              group = 'root',
                              mode = 0755,
                              )
    self.assertResourceCalled('TemplateConfig', '/usr/libexec/hdp/ganglia/gangliaEnv.sh',
                              owner = 'root',
                              template_tag = None,
                              group = 'root',
                              mode = 0755,
                              )
    self.assertResourceCalled('TemplateConfig', '/usr/libexec/hdp/ganglia/gangliaLib.sh',
                              owner = 'root',
                              template_tag = None,
                              group = 'root',
                              mode = 0755,
                              )
    self.assertResourceCalled('Execute', '/usr/libexec/hdp/ganglia/setupGanglia.sh -c HDPSlaves -o root -g hadoop',
                              path = ['/usr/libexec/hdp/ganglia',
                                      '/usr/sbin',
                                      '/sbin:/usr/local/bin',
                                      '/bin',
                                      '/usr/bin'],
                              )
    self.assertResourceCalled('Directory', '/etc/ganglia/conf.d',
                              owner = 'root',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/ganglia/conf.d/modgstatus.conf',
                              owner = 'root',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/ganglia/conf.d/multicpu.conf',
                              owner = 'root',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/ganglia/gmond.conf',
                              owner = 'root',
                              group = 'hadoop',
                              )


  def assert_gmond_master_conf_generated(self):
    self.assertResourceCalled('Execute', '/usr/libexec/hdp/ganglia/setupGanglia.sh -c HDPNameNode -m -o root -g hadoop',
        path = ['/usr/libexec/hdp/ganglia',
           '/usr/sbin',
           '/sbin:/usr/local/bin',
           '/bin',
           '/usr/bin'],
    )
    self.assertResourceCalled('Execute', '/usr/libexec/hdp/ganglia/setupGanglia.sh -c HDPHBaseMaster -m -o root -g hadoop',
        path = ['/usr/libexec/hdp/ganglia',
           '/usr/sbin',
           '/sbin:/usr/local/bin',
           '/bin',
           '/usr/bin'],
    )
    self.assertResourceCalled('Execute', '/usr/libexec/hdp/ganglia/setupGanglia.sh -c HDPResourceManager -m -o root -g hadoop',
        path = ['/usr/libexec/hdp/ganglia',
           '/usr/sbin',
           '/sbin:/usr/local/bin',
           '/bin',
           '/usr/bin'],
    )
    self.assertResourceCalled('Execute', '/usr/libexec/hdp/ganglia/setupGanglia.sh -c HDPNodeManager -m -o root -g hadoop',
        path = ['/usr/libexec/hdp/ganglia',
           '/usr/sbin',
           '/sbin:/usr/local/bin',
           '/bin',
           '/usr/bin'],
    )
    self.assertResourceCalled('Execute', '/usr/libexec/hdp/ganglia/setupGanglia.sh -c HDPHistoryServer -m -o root -g hadoop',
        path = ['/usr/libexec/hdp/ganglia',
           '/usr/sbin',
           '/sbin:/usr/local/bin',
           '/bin',
           '/usr/bin'],
    )
    self.assertResourceCalled('Execute', '/usr/libexec/hdp/ganglia/setupGanglia.sh -c HDPDataNode -m -o root -g hadoop',
        path = ['/usr/libexec/hdp/ganglia',
           '/usr/sbin',
           '/sbin:/usr/local/bin',
           '/bin',
           '/usr/bin'],
    )
    self.assertResourceCalled('Execute', '/usr/libexec/hdp/ganglia/setupGanglia.sh -c HDPHBaseRegionServer -m -o root -g hadoop',
        path = ['/usr/libexec/hdp/ganglia',
           '/usr/sbin',
           '/sbin:/usr/local/bin',
           '/bin',
           '/usr/bin'],
    )
    self.assertResourceCalled('Execute', '/usr/libexec/hdp/ganglia/setupGanglia.sh -c HDPNimbus -m -o root -g hadoop',
        path = ['/usr/libexec/hdp/ganglia',
           '/usr/sbin',
           '/sbin:/usr/local/bin',
           '/bin',
           '/usr/bin'],
    )
    self.assertResourceCalled('Execute', '/usr/libexec/hdp/ganglia/setupGanglia.sh -c HDPSupervisor -m -o root -g hadoop',
        path = ['/usr/libexec/hdp/ganglia',
           '/usr/sbin',
           '/sbin:/usr/local/bin',
           '/bin',
           '/usr/bin'],
    )
    self.assertResourceCalled('Execute', '/usr/libexec/hdp/ganglia/setupGanglia.sh -c HDPSlaves -m -o root -g hadoop',
        path = ['/usr/libexec/hdp/ganglia',
           '/usr/sbin',
           '/sbin:/usr/local/bin',
           '/bin',
           '/usr/bin'],
    )

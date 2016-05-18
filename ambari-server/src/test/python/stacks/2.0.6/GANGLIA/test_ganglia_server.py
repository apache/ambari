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
from mock.mock import MagicMock, call, patch

from only_for_platform import not_for_platform, PLATFORM_WINDOWS

@not_for_platform(PLATFORM_WINDOWS)
class TestGangliaServer(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "GANGLIA/3.5.0/package"
  STACK_VERSION = "2.0.6"
  
  def test_configure_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ganglia_server.py",
                     classname="GangliaServer",
                     command="configure",
                     config_file="default.json",
                     stack_version = self.STACK_VERSION,
                     target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()
    self.assertNoMoreResources()

  def test_start_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ganglia_server.py",
                       classname="GangliaServer",
                       command="start",
                       config_file="default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()
    self.assertResourceCalled('Execute', 'ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E service hdp-gmetad start >> /tmp/gmetad.log  2>&1 ; /bin/ps auwx | /bin/grep [g]metad  >> /tmp/gmetad.log  2>&1',
        path = ['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'],
    )
    self.assertResourceCalled('MonitorWebserver', 'restart',
                              )
    self.assertNoMoreResources()

  def test_stop_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ganglia_server.py",
                       classname="GangliaServer",
                       command="stop",
                       config_file="default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Execute', 'ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E service hdp-gmetad stop >> /tmp/gmetad.log  2>&1 ; /bin/ps auwx | /bin/grep [g]metad  >> /tmp/gmetad.log  2>&1',
        path = ['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'],
    )
    self.assertResourceCalled('MonitorWebserver', 'restart',
                              )
    self.assertNoMoreResources()

  def test_install_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ganglia_server.py",
                       classname="GangliaServer",
                       command="install",
                       config_file="default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()

  def assert_configure_default(self):
    self.assertResourceCalled('Directory', '/usr/libexec/hdp/ganglia',
        owner = 'root',
        group = 'root',
        create_parents = True,
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
    self.assertResourceCalled('Execute', '/usr/libexec/hdp/ganglia/setupGanglia.sh -t -o root -g hadoop',
        path = ['/usr/libexec/hdp/ganglia',
           '/usr/sbin',
           '/sbin:/usr/local/bin',
           '/bin',
           '/usr/bin'],
    )
    self.assertResourceCalled('Directory', '/var/run/ganglia',
        mode=0755,
        create_parents = True
    )
    self.assertResourceCalled('Directory', '/var/lib/ganglia-web/dwoo',
        owner = 'wwwrun',
        create_parents = True,
        recursive_ownership = True,
        mode = 0755,
    )
    self.assertResourceCalled('Directory', '/srv/www/cgi-bin',
        create_parents = True,
    )
    self.assertResourceCalled('TemplateConfig', '/srv/www/cgi-bin/rrd.py',
                              owner = "root",
                              group = "root",
                              mode = 0755,
                              )
    self.assertResourceCalled('Directory', '/var/lib/ganglia/rrds',
                              owner = 'nobody',
                              group = 'nobody',
                              create_parents = True,
                              mode = 0755,
                              )
    self.assertResourceCalled('File', '/etc/apache2/conf.d/ganglia.conf',
                              content = Template('ganglia.conf.j2'),
                              mode = 0644,
                              )
    self.assertResourceCalled('File', '/etc/ganglia/gmetad.conf',
        owner = 'root',
        group = 'hadoop',
    )

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

@patch("os.path.exists", new = MagicMock(return_value=False))
@patch("os.path.islink", new = MagicMock(return_value=False))
class TestGangliaServer(RMFTestCase):

  def test_configure_default(self):
    self.executeScript("1.3.2/services/GANGLIA/package/scripts/ganglia_server.py",
                       classname="GangliaServer",
                       command="configure",
                       config_file="default.json",
    )
    self.assert_configure_default()
    self.assertNoMoreResources()

  def test_start_default(self):
    self.executeScript("1.3.2/services/GANGLIA/package/scripts/ganglia_server.py",
                       classname="GangliaServer",
                       command="start",
                       config_file="default.json",
    )
    self.assert_configure_default()
    self.assertResourceCalled('Execute', 'service hdp-gmetad start >> /tmp/gmetad.log  2>&1 ; /bin/ps auwx | /bin/grep [g]metad  >> /tmp/gmetad.log  2>&1',
                              path = ['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'],
                              )
    self.assertResourceCalled('MonitorWebserver', 'restart',
                              )
    self.assertNoMoreResources()

  def test_stop_default(self):
    self.executeScript("1.3.2/services/GANGLIA/package/scripts/ganglia_server.py",
                       classname="GangliaServer",
                       command="stop",
                       config_file="default.json",
    )
    self.assertResourceCalled('Execute', 'service hdp-gmetad stop >> /tmp/gmetad.log  2>&1 ; /bin/ps auwx | /bin/grep [g]metad  >> /tmp/gmetad.log  2>&1',
                              path = ['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'],
                              )
    self.assertResourceCalled('MonitorWebserver', 'restart',
                              )
    self.assertNoMoreResources()


  def test_install_default(self):
    self.executeScript("1.3.2/services/GANGLIA/package/scripts/ganglia_server.py",
                       classname="GangliaServer",
                       command="install",
                       config_file="default.json",
    )
    self.test_configure_default()

  def assert_configure_default(self):
    self.assertResourceCalled('Group', 'hadoop')
    self.assertResourceCalled('Group', 'nobody')
    self.assertResourceCalled('Group', 'nobody')
    self.assertResourceCalled('User', 'nobody',
        groups = ['nobody'],
    )
    self.assertResourceCalled('User', 'nobody',
        groups = ['nobody'],
    )
    self.assertResourceCalled('Directory', '/usr/libexec/hdp/ganglia',
        owner = 'root',
        group = 'root',
        recursive = True,
    )
    self.assertResourceCalled('File', '/etc/init.d/hdp-gmetad',
        content = StaticFile('gmetad.init'),
        mode = 0o755,
    )
    self.assertResourceCalled('File', '/etc/init.d/hdp-gmond',
        content = StaticFile('gmond.init'),
        mode = 0o755,
    )
    self.assertResourceCalled('File', '/usr/libexec/hdp/ganglia/checkGmond.sh',
        content = StaticFile('checkGmond.sh'),
        mode = 0o755,
    )
    self.assertResourceCalled('File', '/usr/libexec/hdp/ganglia/checkRrdcached.sh',
        content = StaticFile('checkRrdcached.sh'),
        mode = 0o755,
    )
    self.assertResourceCalled('File', '/usr/libexec/hdp/ganglia/gmetadLib.sh',
        content = StaticFile('gmetadLib.sh'),
        mode = 0o755,
    )
    self.assertResourceCalled('File', '/usr/libexec/hdp/ganglia/gmondLib.sh',
        content = StaticFile('gmondLib.sh'),
        mode = 0o755,
    )
    self.assertResourceCalled('File', '/usr/libexec/hdp/ganglia/rrdcachedLib.sh',
        content = StaticFile('rrdcachedLib.sh'),
        mode = 0o755,
    )
    self.assertResourceCalled('File', '/usr/libexec/hdp/ganglia/setupGanglia.sh',
        content = StaticFile('setupGanglia.sh'),
        mode = 0o755,
    )
    self.assertResourceCalled('File', '/usr/libexec/hdp/ganglia/startGmetad.sh',
        content = StaticFile('startGmetad.sh'),
        mode = 0o755,
    )
    self.assertResourceCalled('File', '/usr/libexec/hdp/ganglia/startGmond.sh',
        content = StaticFile('startGmond.sh'),
        mode = 0o755,
    )
    self.assertResourceCalled('File', '/usr/libexec/hdp/ganglia/startRrdcached.sh',
        content = StaticFile('startRrdcached.sh'),
        mode = 0o755,
    )
    self.assertResourceCalled('File', '/usr/libexec/hdp/ganglia/stopGmetad.sh',
        content = StaticFile('stopGmetad.sh'),
        mode = 0o755,
    )
    self.assertResourceCalled('File', '/usr/libexec/hdp/ganglia/stopGmond.sh',
        content = StaticFile('stopGmond.sh'),
        mode = 0o755,
    )
    self.assertResourceCalled('File', '/usr/libexec/hdp/ganglia/stopRrdcached.sh',
        content = StaticFile('stopRrdcached.sh'),
        mode = 0o755,
    )
    self.assertResourceCalled('File', '/usr/libexec/hdp/ganglia/teardownGanglia.sh',
        content = StaticFile('teardownGanglia.sh'),
        mode = 0o755,
    )
    self.assertResourceCalled('TemplateConfig', '/usr/libexec/hdp/ganglia/gangliaClusters.conf',
        owner = 'root',
        template_tag = None,
        group = 'root',
        mode = 0o755,
    )
    self.assertResourceCalled('TemplateConfig', '/usr/libexec/hdp/ganglia/gangliaEnv.sh',
        owner = 'root',
        template_tag = None,
        group = 'root',
        mode = 0o755,
    )
    self.assertResourceCalled('TemplateConfig', '/usr/libexec/hdp/ganglia/gangliaLib.sh',
        owner = 'root',
        template_tag = None,
        group = 'root',
        mode = 0o755,
    )
    self.assertResourceCalled('Execute', '/usr/libexec/hdp/ganglia/setupGanglia.sh -t -o root -g hadoop',
        path = ['/usr/libexec/hdp/ganglia',
           '/usr/sbin',
           '/sbin:/usr/local/bin',
           '/bin',
           '/usr/bin'],
    )
    self.assertResourceCalled('Directory', '/var/lib/ganglia/dwoo',
        owner = 'nobody',
        recursive = True,
        mode = 0o777,
    )
    self.assertResourceCalled('Directory', '/srv/www/cgi-bin',
        recursive = True,
    )
    self.assertResourceCalled('File', '/srv/www/cgi-bin/rrd.py',
        content = StaticFile('rrd.py'),
        mode = 0o755,
    )
    self.assertResourceCalled('Directory', '/var/lib/ganglia/rrds',
        action = ['delete'],
    )
    self.assertResourceCalled('Directory', '/var/lib/ganglia/rrds',
        owner = 'nobody',
        group = 'nobody',
        recursive = True,
        mode = 0o755,
    )
    self.assertResourceCalled('File', '/etc/ganglia/gmetad.conf',
        owner = 'root',
        group = 'hadoop',
    )

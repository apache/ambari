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

  def test_configure_default(self):
    self.executeScript("1.3.2/services/GANGLIA/package/scripts/ganglia_monitor.py",
                       classname="GangliaMonitor",
                       command="configure",
                       config_file="default.json"
                      )
    self.assertResourceCalled('Group', 'hadoop',
                              )
    self.assertResourceCalled('Group', 'nobody',
                              )
    self.assertResourceCalled('Group', 'nobody',
                              )
    self.assertResourceCalled('User', 'nobody',
                              groups = [u'nobody'],
                              )
    self.assertResourceCalled('User', 'nobody',
                              groups = [u'nobody'],
                              )
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
    self.assertResourceCalled('Execute', '/usr/libexec/hdp/ganglia/setupGanglia.sh -c HDPJobTracker -o root -g hadoop',
                              path = ['/usr/libexec/hdp/ganglia',
                                      '/usr/sbin',
                                      '/sbin:/usr/local/bin',
                                      '/bin',
                                      '/usr/bin'],
                              )
    self.assertResourceCalled('Execute', '/usr/libexec/hdp/ganglia/setupGanglia.sh -c HDPHistoryServer -o root -g hadoop',
                              path = ['/usr/libexec/hdp/ganglia',
                                      '/usr/sbin',
                                      '/sbin:/usr/local/bin',
                                      '/bin',
                                      '/usr/bin'],
                              )
    self.assertResourceCalled('Execute', '/usr/libexec/hdp/ganglia/setupGanglia.sh -c HDPDataNode -o root -g hadoop',
                              path = ['/usr/libexec/hdp/ganglia',
                                      '/usr/sbin',
                                      '/sbin:/usr/local/bin',
                                      '/bin',
                                      '/usr/bin'],
                              )
    self.assertResourceCalled('Execute', '/usr/libexec/hdp/ganglia/setupGanglia.sh -c HDPTaskTracker -o root -g hadoop',
                              path = ['/usr/libexec/hdp/ganglia',
                                      '/usr/sbin',
                                      '/sbin:/usr/local/bin',
                                      '/bin',
                                      '/usr/bin'],
                              )
    self.assertResourceCalled('Execute', '/usr/libexec/hdp/ganglia/setupGanglia.sh -c HDPHBaseRegionServer -o root -g hadoop',
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
    self.assertNoMoreResources()

  def test_start_default(self):
    self.executeScript("1.3.2/services/GANGLIA/package/scripts/ganglia_monitor.py",
                       classname="GangliaMonitor",
                       command="start",
                       config_file="default.json"
    )
    self.assertResourceCalled(
        'Group', 'hadoop',
        )
    self.assertResourceCalled(
        'Group', 'nobody',
        )
    self.assertResourceCalled(
        'Group', 'nobody',
        )
    self.assertResourceCalled(
        'User', 'nobody',
        groups = [u'nobody'],
        )
    self.assertResourceCalled(
        'User', 'nobody',
        groups = [u'nobody'],
        )
    self.assertResourceCalled(
        'Directory', '/etc/ganglia/hdp',
        owner = 'root',
        group = 'hadoop',
        recursive = True,
        )
    self.assertResourceCalled(
        'Directory', '/usr/libexec/hdp/ganglia',
        owner = 'root',
        group = 'root',
        recursive = True,
        )
    self.assertResourceCalled(
        'File', '/etc/init.d/hdp-gmetad',
        content = StaticFile('gmetad.init'),
        mode = 0755,
        )
    self.assertResourceCalled(
        'File', '/etc/init.d/hdp-gmond',
        content = StaticFile('gmond.init'),
        mode = 0755,
        )
    self.assertResourceCalled(
        'File', '/usr/libexec/hdp/ganglia/checkGmond.sh',
        content = StaticFile('checkGmond.sh'),
        mode = 0755,
        )
    self.assertResourceCalled(
        'File', '/usr/libexec/hdp/ganglia/checkRrdcached.sh',
        content = StaticFile('checkRrdcached.sh'),
        mode = 0755,
        )
    self.assertResourceCalled(
        'File', '/usr/libexec/hdp/ganglia/gmetadLib.sh',
        content = StaticFile('gmetadLib.sh'),
        mode = 0755,
        )
    self.assertResourceCalled(
        'File', '/usr/libexec/hdp/ganglia/gmondLib.sh',
        content = StaticFile('gmondLib.sh'),
        mode = 0755,
        )
    self.assertResourceCalled(
        'File', '/usr/libexec/hdp/ganglia/rrdcachedLib.sh',
        content = StaticFile('rrdcachedLib.sh'),
        mode = 0755,
        )
    self.assertResourceCalled(
        'File', '/usr/libexec/hdp/ganglia/setupGanglia.sh',
        content = StaticFile('setupGanglia.sh'),
        mode = 0755,
        )
    self.assertResourceCalled(
        'File', '/usr/libexec/hdp/ganglia/startGmetad.sh',
        content = StaticFile('startGmetad.sh'),
        mode = 0755,
        )
    self.assertResourceCalled(
        'File', '/usr/libexec/hdp/ganglia/startGmond.sh',
        content = StaticFile('startGmond.sh'),
        mode = 0755,
        )
    self.assertResourceCalled(
        'File', '/usr/libexec/hdp/ganglia/startRrdcached.sh',
        content = StaticFile('startRrdcached.sh'),
        mode = 0755,
        )
    self.assertResourceCalled(
        'File', '/usr/libexec/hdp/ganglia/stopGmetad.sh',
        content = StaticFile('stopGmetad.sh'),
        mode = 0755,
        )
    self.assertResourceCalled(
        'File', '/usr/libexec/hdp/ganglia/stopGmond.sh',
        content = StaticFile('stopGmond.sh'),
        mode = 0755,
        )
    self.assertResourceCalled(
        'File', '/usr/libexec/hdp/ganglia/stopRrdcached.sh',
        content = StaticFile('stopRrdcached.sh'),
        mode = 0755,
        )
    self.assertResourceCalled(
        'File', '/usr/libexec/hdp/ganglia/teardownGanglia.sh',
        content = StaticFile('teardownGanglia.sh'),
        mode = 0755,
        )
    self.assertResourceCalled(
        'TemplateConfig', '/usr/libexec/hdp/ganglia/gangliaClusters.conf',
        owner = 'root',
        template_tag = None,
        group = 'root',
        mode = 0755,
        )
    self.assertResourceCalled(
        'TemplateConfig', '/usr/libexec/hdp/ganglia/gangliaEnv.sh',
        owner = 'root',
        template_tag = None,
        group = 'root',
        mode = 0755,
        )
    self.assertResourceCalled(
        'TemplateConfig', '/usr/libexec/hdp/ganglia/gangliaLib.sh',
        owner = 'root',
        template_tag = None,
        group = 'root',
        mode = 0755,
        )
    self.assertResourceCalled(
        'Execute', '/usr/libexec/hdp/ganglia/setupGanglia.sh -c HDPJobTracker -o root -g hadoop',
        path = ['/usr/libexec/hdp/ganglia',
                '/usr/sbin',
                '/sbin:/usr/local/bin',
                '/bin',
                '/usr/bin'],
        )
    self.assertResourceCalled(
        'Execute', '/usr/libexec/hdp/ganglia/setupGanglia.sh -c HDPHistoryServer -o root -g hadoop',
        path = ['/usr/libexec/hdp/ganglia',
                '/usr/sbin',
                '/sbin:/usr/local/bin',
                '/bin',
                '/usr/bin'],
        )
    self.assertResourceCalled(
        'Execute', '/usr/libexec/hdp/ganglia/setupGanglia.sh -c HDPDataNode -o root -g hadoop',
        path = ['/usr/libexec/hdp/ganglia',
                '/usr/sbin',
                '/sbin:/usr/local/bin',
                '/bin',
                '/usr/bin'],
        )
    self.assertResourceCalled(
        'Execute', '/usr/libexec/hdp/ganglia/setupGanglia.sh -c HDPTaskTracker -o root -g hadoop',
        path = ['/usr/libexec/hdp/ganglia',
                '/usr/sbin',
                '/sbin:/usr/local/bin',
                '/bin',
                '/usr/bin'],
        )
    self.assertResourceCalled(
        'Execute', '/usr/libexec/hdp/ganglia/setupGanglia.sh -c HDPHBaseRegionServer -o root -g hadoop',
        path = ['/usr/libexec/hdp/ganglia',
                '/usr/sbin',
                '/sbin:/usr/local/bin',
                '/bin',
                '/usr/bin'],
        )
    self.assertResourceCalled(
        'Directory', '/etc/ganglia/conf.d',
        owner = 'root',
        group = 'hadoop',
        )
    self.assertResourceCalled(
        'File', '/etc/ganglia/conf.d/modgstatus.conf',
        owner = 'root',
        group = 'hadoop',
        )
    self.assertResourceCalled(
        'File', '/etc/ganglia/conf.d/multicpu.conf',
        owner = 'root',
        group = 'hadoop',
        )
    self.assertResourceCalled(
        'File', '/etc/ganglia/gmond.conf',
        owner = 'root',
        group = 'hadoop',
        )
    self.assertResourceCalled(
        'Execute', 'service hdp-gmond start >> /tmp/gmond.log  2>&1 ; /bin/ps auwx | /bin/grep [g]mond  >> /tmp/gmond.log  2>&1',
        path = ['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'],
        )
    self.assertNoMoreResources()


  def test_stop_default(self):
    self.executeScript("1.3.2/services/GANGLIA/package/scripts/ganglia_monitor.py",
                       classname="GangliaMonitor",
                       command="stop",
                       config_file="default.json"
    )
    self.assertResourceCalled('Execute', 'service hdp-gmond stop >> /tmp/gmond.log  2>&1 ; /bin/ps auwx | /bin/grep [g]mond  >> /tmp/gmond.log  2>&1',
                              path = ['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'],
                              )
    self.assertNoMoreResources()


  def test_install_default(self):
    self.executeScript("1.3.2/services/GANGLIA/package/scripts/ganglia_monitor.py",
                       classname="GangliaMonitor",
                       command="install",
                       config_file="default.json"
    )
    self.assertResourceCalled('Group', 'hadoop',)

    self.assertResourceCalled('Group', 'nobody',)

    self.assertResourceCalled('Group', 'nobody',)

    self.assertResourceCalled('User', 'nobody',
                              groups = [u'nobody'],)

    self.assertResourceCalled('User', 'nobody',
                              groups = [u'nobody'],)

    self.assertResourceCalled('Directory', '/etc/ganglia/hdp',
                              owner = 'root',
                              group = 'hadoop',
                              recursive = True,)

    self.assertResourceCalled('Directory', '/usr/libexec/hdp/ganglia',
                              owner = 'root',
                              group = 'root',
                              recursive = True,)

    self.assertResourceCalled('File', '/etc/init.d/hdp-gmetad',
                              content = StaticFile('gmetad.init'),
                              mode = 0755,)

    self.assertResourceCalled('File', '/etc/init.d/hdp-gmond',
                              content = StaticFile('gmond.init'),
                              mode = 0755,)

    self.assertResourceCalled('File', '/usr/libexec/hdp/ganglia/checkGmond.sh',
                              content = StaticFile('checkGmond.sh'),
                              mode = 0755,)

    self.assertResourceCalled('File', '/usr/libexec/hdp/ganglia/checkRrdcached.sh',
                              content = StaticFile('checkRrdcached.sh'),
                              mode = 0755,)

    self.assertResourceCalled('File', '/usr/libexec/hdp/ganglia/gmetadLib.sh',
                              content = StaticFile('gmetadLib.sh'),
                              mode = 0755,)

    self.assertResourceCalled('File', '/usr/libexec/hdp/ganglia/gmondLib.sh',
                              content = StaticFile('gmondLib.sh'),
                              mode = 0755,)

    self.assertResourceCalled('File', '/usr/libexec/hdp/ganglia/rrdcachedLib.sh',
                              content = StaticFile('rrdcachedLib.sh'),
                              mode = 0755,)

    self.assertResourceCalled('File', '/usr/libexec/hdp/ganglia/setupGanglia.sh',
                              content = StaticFile('setupGanglia.sh'),
                              mode = 0755,)

    self.assertResourceCalled('File', '/usr/libexec/hdp/ganglia/startGmetad.sh',
                              content = StaticFile('startGmetad.sh'),
                              mode = 0755,)

    self.assertResourceCalled('File', '/usr/libexec/hdp/ganglia/startGmond.sh',
                              content = StaticFile('startGmond.sh'),
                              mode = 0755,)

    self.assertResourceCalled('File', '/usr/libexec/hdp/ganglia/startRrdcached.sh',
                              content = StaticFile('startRrdcached.sh'),
                              mode = 0755,)

    self.assertResourceCalled('File', '/usr/libexec/hdp/ganglia/stopGmetad.sh',
                              content = StaticFile('stopGmetad.sh'),
                              mode = 0755,)

    self.assertResourceCalled('File', '/usr/libexec/hdp/ganglia/stopGmond.sh',
                              content = StaticFile('stopGmond.sh'),
                              mode = 0755,)

    self.assertResourceCalled('File', '/usr/libexec/hdp/ganglia/stopRrdcached.sh',
                              content = StaticFile('stopRrdcached.sh'),
                              mode = 0755,)

    self.assertResourceCalled('File', '/usr/libexec/hdp/ganglia/teardownGanglia.sh',
                              content = StaticFile('teardownGanglia.sh'),
                              mode = 0755,)

    self.assertResourceCalled('TemplateConfig', '/usr/libexec/hdp/ganglia/gangliaClusters.conf',
                              owner = 'root',
                              template_tag = None,
                              group = 'root',
                              mode = 0755,)

    self.assertResourceCalled('TemplateConfig', '/usr/libexec/hdp/ganglia/gangliaEnv.sh',
                              owner = 'root',
                              template_tag = None,
                              group = 'root',
                              mode = 0755,)

    self.assertResourceCalled('TemplateConfig', '/usr/libexec/hdp/ganglia/gangliaLib.sh',
                              owner = 'root',
                              template_tag = None,
                              group = 'root',
                              mode = 0755,)

    self.assertResourceCalled('Execute', '/usr/libexec/hdp/ganglia/setupGanglia.sh -c HDPJobTracker -o root -g hadoop',
                              path = ['/usr/libexec/hdp/ganglia',
                                      '/usr/sbin',
                                      '/sbin:/usr/local/bin',
                                      '/bin',
                                      '/usr/bin'],)

    self.assertResourceCalled('Execute', '/usr/libexec/hdp/ganglia/setupGanglia.sh -c HDPHistoryServer -o root -g hadoop',
                              path = ['/usr/libexec/hdp/ganglia',
                                      '/usr/sbin',
                                      '/sbin:/usr/local/bin',
                                      '/bin',
                                      '/usr/bin'],)

    self.assertResourceCalled('Execute', '/usr/libexec/hdp/ganglia/setupGanglia.sh -c HDPDataNode -o root -g hadoop',
                              path = ['/usr/libexec/hdp/ganglia',
                                      '/usr/sbin',
                                      '/sbin:/usr/local/bin',
                                      '/bin',
                                      '/usr/bin'],)

    self.assertResourceCalled('Execute', '/usr/libexec/hdp/ganglia/setupGanglia.sh -c HDPTaskTracker -o root -g hadoop',
                              path = ['/usr/libexec/hdp/ganglia',
                                      '/usr/sbin',
                                      '/sbin:/usr/local/bin',
                                      '/bin',
                                      '/usr/bin'],)

    self.assertResourceCalled('Execute', '/usr/libexec/hdp/ganglia/setupGanglia.sh -c HDPHBaseRegionServer -o root -g hadoop',
                              path = ['/usr/libexec/hdp/ganglia',
                                      '/usr/sbin',
                                      '/sbin:/usr/local/bin',
                                      '/bin',
                                      '/usr/bin'],)

    self.assertResourceCalled('Directory', '/etc/ganglia/conf.d',
                              owner = 'root',
                              group = 'hadoop',)

    self.assertResourceCalled('File', '/etc/ganglia/conf.d/modgstatus.conf',
                              owner = 'root',
                              group = 'hadoop',)

    self.assertResourceCalled('File', '/etc/ganglia/conf.d/multicpu.conf',
                              owner = 'root',
                              group = 'hadoop',)

    self.assertResourceCalled('File', '/etc/ganglia/gmond.conf',
                              owner = 'root',
                              group = 'hadoop',)

    self.assertResourceCalled('Execute', 'chkconfig gmond off',
                              path = ['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'],)

    self.assertResourceCalled('Execute', 'chkconfig gmetad off',
                              path = ['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'],)
    self.assertNoMoreResources()
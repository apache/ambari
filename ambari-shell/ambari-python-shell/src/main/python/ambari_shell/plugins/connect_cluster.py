#!/usr/bin/env python
#
#  Licensed to the Apache Software Foundation (ASF) under one
#  or more contributor license agreements.  See the NOTICE file
#  distributed with this work for additional information
#  regarding copyright ownership.  The ASF licenses this file
#  to you under the Apache License, Version 2.0 (the
#  "License"); you may not use this file except in compliance
#  with the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
import logging
import textwrap

LOG = logging.getLogger(__name__)


def do_connect_cluster(self, cluster):

    if not cluster:
        self.help_connect_cluster()
        return None

    if not self.CLUSTERS:
        clusters = [str(c.cluster_name).encode('ascii', 'ignore')
                    for c in self.global_shell_config['client'].get_all_clusters()]
        self.CLUSTERS = clusters

    if cluster not in self.CLUSTERS:
        print "ERROR ! cluster %s not found " % str(cluster)
        print "        valid clusters are " + str(self.CLUSTERS)
        return None
    if self.global_shell_config['clustername']:
        LOG.debug("old cluster = " + self.global_shell_config['clustername'])
        self.global_shell_config['clustername'] = str(cluster)
    else:
        self.global_shell_config['clustername'] = cluster
    self.prompt = "ambari-" + \
        str(self.global_shell_config['clustername']) + ">"


def help_connect_cluster(self):
    print '\n'.join([' Usage:', '     connect_cluster <cluster_name>'])


def do_disconnect_cluster(self, line):

    if self.global_shell_config['clustername']:
        LOG.debug("old cluster = " + self.global_shell_config['clustername'])
    self.global_shell_config['clustername'] = None
    self.prompt = "ambari>"


def help_disconnect_cluster(self):
    print '\n'.join([' Usage:', '     disconnect_cluster'])


def complete_connect_cluster(self, pattern, line, start_index, end_index):
    if not self.CLUSTERS:
        clusters = [
            (c.cluster_name).encode(
                'ascii',
                'ignore') for c in self.global_shell_config['client'].get_all_clusters()]
        self.CLUSTERS = clusters

    LOG.debug(
        ("self.CLUSTERS = %s pattern = %s ") %
        (str(
            self.CLUSTERS),
            pattern))
    if pattern:
        return [
            c for c in self.CLUSTERS if c.startswith(pattern)]
    else:
        return self.CLUSTERS

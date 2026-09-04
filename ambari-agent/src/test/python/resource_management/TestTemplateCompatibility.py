#!/usr/bin/env python3

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

from pathlib import Path
from unittest import TestCase

from resource_management.core.environment import Environment
from resource_management.core.source import Template


REPOSITORY_ROOT = Path(__file__).resolve().parents[5]
BIGTOP_SERVICES = (
  REPOSITORY_ROOT
  / "ambari-server/src/main/resources/stacks/BIGTOP/3.2.0/services"
)


class TestTemplateCompatibility(TestCase):
  def render(self, package_directory, template_name, **context):
    with Environment(str(package_directory)) as environment:
      environment.set_params(context)
      return Template(template_name).get_content().strip()

  def test_hdfs_kerberos_template_golden_output(self):
    rendered = self.render(
      BIGTOP_SERVICES / "HDFS/package",
      "hdfs_nn_jaas.conf.j2",
      nn_keytab="/etc/security/keytabs/nn.service.keytab",
      nn_principal_name="nn/node.example@EXAMPLE.COM",
    )

    self.assertEqual(
      """com.sun.security.jgss.krb5.initiate {
    com.sun.security.auth.module.Krb5LoginModule required
    renewTGT=false
    doNotPrompt=true
    useKeyTab=true
    keyTab=\"/etc/security/keytabs/nn.service.keytab\"
    principal=\"nn/node.example@EXAMPLE.COM\"
    storeKey=true
    useTicketCache=false;
};""",
      rendered,
    )

  def test_yarn_template_golden_output(self):
    rendered = self.render(
      BIGTOP_SERVICES / "YARN/package",
      "yarn_jaas.conf.j2",
      rm_keytab_jaas="/etc/security/keytabs/rm.service.keytab",
      rm_principal_name_jaas="rm/node.example@EXAMPLE.COM",
    )

    self.assertEqual(
      """Client {
  com.sun.security.auth.module.Krb5LoginModule required
  useKeyTab=true
  storeKey=true
  useTicketCache=false
  keyTab=\"/etc/security/keytabs/rm.service.keytab\"
  principal=\"rm/node.example@EXAMPLE.COM\";
};
com.sun.security.jgss.krb5.initiate {
  com.sun.security.auth.module.Krb5LoginModule required
  renewTGT=false
  doNotPrompt=true
  useKeyTab=true
  keyTab=\"/etc/security/keytabs/rm.service.keytab\"
  principal=\"rm/node.example@EXAMPLE.COM\"
  storeKey=true
  useTicketCache=false;
};""",
      rendered,
    )

  def test_hbase_template_golden_output(self):
    rendered = self.render(
      BIGTOP_SERVICES / "HBASE/package",
      "hbase_master_jaas.conf.j2",
      master_keytab_path="/etc/security/keytabs/hbase.service.keytab",
      master_jaas_princ="hbase/node.example@EXAMPLE.COM",
    )

    self.assertEqual(
      """Client {
com.sun.security.auth.module.Krb5LoginModule required
useKeyTab=true
storeKey=true
useTicketCache=false
keyTab=\"/etc/security/keytabs/hbase.service.keytab\"
principal=\"hbase/node.example@EXAMPLE.COM\";
};
com.sun.security.jgss.krb5.initiate {
com.sun.security.auth.module.Krb5LoginModule required
renewTGT=false
doNotPrompt=true
useKeyTab=true
storeKey=true
useTicketCache=false
keyTab=\"/etc/security/keytabs/hbase.service.keytab\"
principal=\"hbase/node.example@EXAMPLE.COM\";
};""",
      rendered,
    )

  def test_hive_template_golden_output(self):
    rendered = self.render(
      BIGTOP_SERVICES / "HIVE/package",
      "zkmigrator_jaas.conf.j2",
      hive_keytab="/etc/security/keytabs/hive.service.keytab",
      hive_principal="hive/node.example@EXAMPLE.COM",
    )

    self.assertEqual(
      """Client {
  com.sun.security.auth.module.Krb5LoginModule required
  useKeyTab=true
  storeKey=true
  useTicketCache=false
  keyTab=\"/etc/security/keytabs/hive.service.keytab\"
  principal=\"hive/node.example@EXAMPLE.COM\";
};""",
      rendered,
    )

  def test_kafka_conditional_template_golden_output(self):
    rendered = self.render(
      BIGTOP_SERVICES / "KAFKA/package",
      "kafka_client_jaas.conf.j2",
      kerberos_security_enabled=True,
      kafka_kerberos_credentials_enabled=True,
      kafka_bare_jaas_principal_jaas="kafka",
    )

    self.assertEqual(
      """KafkaClient {
   com.sun.security.auth.module.Krb5LoginModule required
   useTicketCache=true
   renewTicket=true
   serviceName=\"kafka\";
};""",
      rendered,
    )

  def test_ranger_template_golden_output(self):
    ranger_package = (
      REPOSITORY_ROOT
      / "ambari-server/src/main/resources/stacks/BIGTOP/3.3.0/services/RANGER/package"
    )
    rendered = self.render(
      ranger_package,
      "ranger_solr_jaas_conf.j2",
      solr_kerberos_keytab="/etc/security/keytabs/solr.service.keytab",
      solr_kerberos_principal="solr/node.example@EXAMPLE.COM",
    )

    self.assertEqual(
      """Client {
  com.sun.security.auth.module.Krb5LoginModule required
  useKeyTab=true
  storeKey=true
  useTicketCache=false
  keyTab=\"/etc/security/keytabs/solr.service.keytab\"
  principal=\"solr/node.example@EXAMPLE.COM\";
};""",
      rendered,
    )

  def test_generic_kerberos_template_golden_output(self):
    rendered = self.render(
      BIGTOP_SERVICES / "ZOOKEEPER/package",
      "zookeeper_client_jaas.conf.j2",
    )

    self.assertEqual(
      """Client {
  com.sun.security.auth.module.Krb5LoginModule required
  useKeyTab=false
  useTicketCache=true
  doNotPrompt=true
  renewTGT=true;
};""",
      rendered,
    )

  def test_ha_topology_template_golden_output(self):
    hook_package = (
      REPOSITORY_ROOT
      / "ambari-server/src/main/resources/stack-hooks/before-START"
    )
    rendered = self.render(
      hook_package,
      "topology_mappings.data.j2",
      all_hosts=["nn-a", "nn-b", "worker-a"],
      slave_hosts=["worker-a"],
      nm_hosts=["worker-a"],
      all_racks=["/rack-a", "/rack-b", "/rack-c"],
      all_ipv4_ips=["10.0.0.1", "10.0.0.2", "10.0.0.3"],
    )

    self.assertEqual(
      """[network_topology]
worker-a=/rack-c
10.0.0.3=/rack-c""",
      rendered,
    )

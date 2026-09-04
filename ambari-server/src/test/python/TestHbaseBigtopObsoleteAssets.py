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
import unittest


HBASE = (
  Path(__file__).resolve().parents[2]
  / "main/resources/stacks/BIGTOP/3.2.0/services/HBASE"
)


class TestHbaseObsoleteAssets(unittest.TestCase):
  def test_no_vendored_ruby_or_shell_workflow_remains(self):
    package_dir = HBASE / "package"
    vendored_workflows = sorted(
      path.relative_to(HBASE).as_posix()
      for pattern in ("*.rb", "*.sh")
      for path in package_dir.rglob(pattern)
    )
    self.assertEqual([], vendored_workflows)

  def test_deprecated_custom_workflow_assets_are_removed(self):
    removed = (
      "package/files/draining_servers.rb",
      "package/files/draining_servers2.rb",
      "package/files/hbase-smoke-cleanup.sh",
      "package/files/hbaseSmokeVerify.sh",
      "package/scripts/hbase_upgrade.py",
      "package/templates/hbase-smoke.sh.j2",
      "package/templates/hbase_grant_permissions.j2",
      "package/templates/hbase_queryserver_jaas.conf.j2",
    )
    for relative_path in removed:
      with self.subTest(path=relative_path):
        self.assertFalse((HBASE / relative_path).exists())

  def test_unmanaged_atlas_hook_contract_is_removed(self):
    owned_sources = (
      "configuration/hbase-env.xml",
      "configuration/hbase-site.xml",
      "package/scripts/hbase_master.py",
      "package/scripts/params_linux.py",
    )
    for relative_path in owned_sources:
      with self.subTest(path=relative_path):
        self.assertNotIn("atlas", (HBASE / relative_path).read_text().lower())

    advisor = (HBASE / "service_advisor.py").read_text()
    self.assertNotIn("recommendAtlasHook", advisor)
    self.assertIn("removeObsoleteAtlasHook", advisor)
    self.assertIn("org.apache.atlas.hbase.hook.HBaseAtlasCoprocessor", advisor)

  def test_hbase_2_6_deprecated_configuration_is_removed(self):
    deprecated_properties = (
      "hbase.bulkload.staging.dir",
      "hbase.bucketcache.percentage.in.combinedcache",
    )
    owned_sources = (
      "configuration/hbase-site.xml",
      "kerberos.json",
      "themes/directories.json",
      "package/scripts/hbase.py",
      "package/scripts/params_linux.py",
    )
    for deprecated_property in deprecated_properties:
      for relative_path in owned_sources:
        with self.subTest(
          deprecated_property=deprecated_property, path=relative_path
        ):
          self.assertNotIn(
            deprecated_property, (HBASE / relative_path).read_text()
          )

    advisor = (HBASE / "service_advisor.py").read_text()
    self.assertIn(
      '"hbase.bucketcache.percentage.in.combinedcache", "delete", "true"',
      advisor,
    )

    hbase_source = (HBASE / "package/scripts/hbase.py").read_text()
    params_source = (HBASE / "package/scripts/params_linux.py").read_text()
    self.assertNotIn("PHOENIX_CORE_HDFS_SITE_REQUIRED", hbase_source)
    self.assertNotIn("mount_table_xml_inclusion_file_full_path", params_source)
    self.assertNotIn("mount_table_content", params_source)


if __name__ == "__main__":
  unittest.main()

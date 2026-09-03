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

import pathlib
import unittest


HIVE_ENV = (
  pathlib.Path(__file__).parents[2]
  / "main/resources/stacks/BIGTOP/3.2.0/services/HIVE/configuration/hive-env.xml"
)


class TestHiveBigtop(unittest.TestCase):
  def test_java8_override_does_not_receive_jdk17_gc_flags(self):
    content = HIVE_ENV.read_text(encoding="utf-8")

    self.assertEqual(content.count('grep -q \'version "17\''), 2)
    self.assertEqual(content.count("-Xlog:gc*:file="), 2)
    self.assertEqual(content.count('export HADOOP_OPTS="${HADOOP_OPTS:-} -Dhive.log.dir='), 2)

    for service_log in ("hivemetastore-gc.log", "hiveserver2-gc.log"):
      option = content.index("-Xlog:gc*:file=" + "{{hive_log_dir}}/" + service_log)
      java17_guard = content.rfind('grep -q \'version "17\'', 0, option)
      self.assertGreater(java17_guard, content.rfind("fi", 0, option))


if __name__ == "__main__":
  unittest.main()

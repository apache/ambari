#!/usr/bin/env python3

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import importlib.util
from pathlib import Path
import tempfile
from unittest import TestCase
from unittest.mock import patch


MODULE_PATH = (
  Path(__file__).resolve().parents[2]
  / "main/resources/stacks/HDF/2.0/hooks/before-START/files/topology_script.py"
)
SPEC = importlib.util.spec_from_file_location("hdf_topology_script", MODULE_PATH)
topology_script = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(topology_script)


class TestTopologyScript(TestCase):
  def test_loads_mapping_and_resolves_multiple_hosts(self):
    with tempfile.TemporaryDirectory() as directory:
      mapping_path = Path(directory) / "topology_mappings.data"
      mapping_path.write_text(
        "[network_topology]\n"
        "host-a=/rack-a\n"
        "10.0.0.2=/rack-b\n",
        encoding="utf-8",
      )
      script = topology_script.TopologyScript()

      with patch.object(topology_script, "DATA_FILE_NAME", str(mapping_path)):
        rack_map = script.load_rack_map()

      self.assertEqual(
        "/rack-a /rack-b",
        script.get_racks(
          rack_map,
          ["topology_script.py", "host-a", "10.0.0.2:50010"],
        ),
      )

  def test_missing_mapping_section_falls_back_to_default_rack(self):
    with tempfile.TemporaryDirectory() as directory:
      mapping_path = Path(directory) / "topology_mappings.data"
      mapping_path.write_text("[other]\nhost-a=/rack-a\n", encoding="utf-8")
      script = topology_script.TopologyScript()

      with patch.object(topology_script, "DATA_FILE_NAME", str(mapping_path)):
        rack_map = script.load_rack_map()

      self.assertEqual({}, rack_map)
      self.assertEqual(
        topology_script.DEFAULT_RACK,
        script.lookup_by_hostname_or_ip("unknown-host", rack_map),
      )

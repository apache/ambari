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

import os
import re
from pathlib import Path
from xml.etree import ElementTree

from setuptools import find_packages, setup


def get_version():
  version = os.environ.get("AMBARI_VERSION")
  if version is None:
    root_pom = Path(__file__).resolve().parents[4] / "pom.xml"
    if root_pom.is_file():
      namespace = "http://maven.apache.org/POM/4.0.0"
      root = ElementTree.parse(root_pom).getroot()
      version = root.findtext(f"{{{namespace}}}properties/{{{namespace}}}revision")
  version = version or "3.1.0.0.dev0"
  if version.endswith("-SNAPSHOT"):
    version = version[: -len("-SNAPSHOT")] + ".dev0"
  if not re.fullmatch(r"[0-9]+(?:\.[0-9]+)*(?:\.dev[0-9]+)?", version):
    raise ValueError(f"Unsupported Ambari Agent package version: {version}")
  return version

setup(
  version=get_version(),
  packages=find_packages(),
)

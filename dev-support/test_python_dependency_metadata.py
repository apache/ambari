#!/usr/bin/env python3

# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

import importlib.util
from pathlib import Path
import tempfile
import unittest


SCRIPT_PATH = Path(__file__).with_name("check_python_dependency_metadata.py")
SPEC = importlib.util.spec_from_file_location("check_python_dependency_metadata", SCRIPT_PATH)
metadata_check = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(metadata_check)


class PythonDependencyMetadataTest(unittest.TestCase):
  def setUp(self):
    self.temporary_directory = tempfile.TemporaryDirectory()
    self.repository = Path(self.temporary_directory.name)
    common = self.repository / "ambari-common/src/main/python"
    agent = self.repository / "ambari-agent/src/main/python"
    server = self.repository / "ambari-server/src/main/python"
    common.mkdir(parents=True)
    agent.mkdir(parents=True)
    server.mkdir(parents=True)
    (common / "requirements.in").write_text("base==1.0\n", encoding="utf-8")
    (agent / "requirements.in").write_text(
      "-r ../../../../ambari-common/src/main/python/requirements.in\nagent==2.0\n",
      encoding="utf-8",
    )
    (server / "requirements.in").write_text(
      "-r ../../../../ambari-common/src/main/python/requirements.in\nserver==3.0\n",
      encoding="utf-8",
    )
    (agent / "requirements-sdist.in").write_text(
      "docopt==0.6.2\n", encoding="utf-8"
    )
    (common / "requirements.lock").write_text(
      "base==1.0 --hash=sha256:" + "a" * 64 + "\n", encoding="utf-8"
    )
    (agent / "requirements.lock").write_text(
      "base==1.0 --hash=sha256:" + "a" * 64 + "\n"
      "agent==2.0 --hash=sha256:" + "a" * 64 + "\n",
      encoding="utf-8",
    )
    (agent / "requirements-sdist.lock").write_text(
      "docopt==0.6.2 --hash=sha256:" + "a" * 64 + "\n",
      encoding="utf-8",
    )
    (server / "requirements.lock").write_text(
      "base==1.0 --hash=sha256:" + "a" * 64 + "\n"
      "server==3.0 --hash=sha256:" + "a" * 64 + "\n",
      encoding="utf-8",
    )
    (self.repository / "requirements-build.in").write_text(
      "setuptools==4.0\nbuild==5.0\n", encoding="utf-8"
    )
    (self.repository / "requirements-tooling.txt").write_text(
      "ruff==6.0 \\\n  --hash=sha256:" + "a" * 64 + "\n",
      encoding="utf-8",
    )
    (self.repository / "requirements-build.lock").write_text(
      "setuptools==4.0 --hash=sha256:" + "a" * 64 + "\n"
      "build==5.0 --hash=sha256:" + "a" * 64 + "\n",
      encoding="utf-8",
    )
    (self.repository / "setup.py").write_text(
      "from setuptools import setup\nsetup()\n",
      encoding="utf-8",
    )
    (agent / "setup.py").write_text(
      "from setuptools import setup\nsetup()\n",
      encoding="utf-8",
    )
    (self.repository / "pyproject.toml").write_text(
      "[build-system]\nrequires=['setuptools==4.0']\n"
      "[project]\nrequires-python='>=3.9.2'\ndependencies=['base==1.0']\n"
      "[project.optional-dependencies]\n"
      "agent=['agent==2.0']\nserver=['server==3.0']\n"
      "tooling=['ruff==6.0']\n"
      "[tool.ruff]\nexclude=[]\n",
      encoding="utf-8",
    )
    (agent / "pyproject.toml").write_text(
      "[build-system]\nrequires=['setuptools==4.0']\n"
      "[project]\nrequires-python='>=3.9.2'\n"
      "dependencies=['base==1.0', 'agent==2.0']\n"
      "[project.scripts]\nambari-agent='ambari_agent.AmbariAgent:main'\n",
      encoding="utf-8",
    )

  def tearDown(self):
    self.temporary_directory.cleanup()

  def test_consistent_metadata_is_accepted(self):
    self.assertEqual([], metadata_check.audit(self.repository))

  def test_setup_runtime_dependency_duplication_is_reported(self):
    setup_path = self.repository / "ambari-agent/src/main/python/setup.py"
    setup_path.write_text(
      "from setuptools import setup\nsetup(install_requires=['agent==2.0'])\n",
      encoding="utf-8",
    )

    self.assertEqual(
      ["Agent setup.py must not duplicate pyproject runtime dependencies"],
      metadata_check.audit(self.repository),
    )

  def test_ruff_cannot_exclude_contrib_sources(self):
    pyproject = self.repository / "pyproject.toml"
    pyproject.write_text(
      pyproject.read_text(encoding="utf-8").replace(
        "[tool.ruff]\nexclude=[]\n", "[tool.ruff]\nexclude=['contrib']\n"
      ),
      encoding="utf-8",
    )

    self.assertIn(
      "Ruff must scan contrib Python sources",
      metadata_check.audit(self.repository),
    )

  def test_missing_manifest_pin_in_lock_is_reported(self):
    lock_path = self.repository / "ambari-agent/src/main/python/requirements.lock"
    lock_path.write_text(
      "base==1.0 --hash=sha256:" + "a" * 64 + "\n", encoding="utf-8"
    )

    self.assertEqual(
      ["Agent lock omits manifest pins: agent==2.0"],
      metadata_check.audit(self.repository),
    )

  def test_lock_entry_without_hash_is_rejected(self):
    lock_path = self.repository / "requirements-build.lock"
    lock_path.write_text("setuptools==4.0\nbuild==5.0\n", encoding="utf-8")

    with self.assertRaisesRegex(metadata_check.MetadataError, "SHA-256"):
      metadata_check.audit(self.repository)

  def test_maven_sdist_execution_requires_locked_build_environment(self):
    agent_pom = self.repository / "ambari-agent/pom.xml"
    agent_pom.write_text(
      """<project xmlns="http://maven.apache.org/POM/4.0.0">
  <build><plugins><plugin><artifactId>exec-maven-plugin</artifactId><executions>
    <execution><id>bundle-python-sdist-dependencies</id>
      <phase>generate-resources</phase>
      <configuration><arguments>
        <argument>--no-build-isolation</argument>
        <argument>--no-binary=:all:</argument>
        <argument>--no-deps</argument>
        <argument>--require-hashes</argument>
        <argument>${project.basedir}/src/main/python/requirements-sdist.lock</argument>
      </arguments></configuration>
    </execution>
  </executions></plugin></plugins></build>
</project>
""",
      encoding="utf-8",
    )

    errors = metadata_check.audit(self.repository)

    self.assertIn(
      "Agent Maven execution bundle-python-sdist-dependencies requires "
      "PYTHONPATH=${python.build.dependencies.dir}",
      errors,
    )
    self.assertIn(
      "Agent Maven execution bundle-python-sdist-dependencies omits arguments: "
      "--use-pep517",
      errors,
    )

  def test_blank_default_wheelhouse_is_rejected(self):
    (self.repository / "pom.xml").write_text(
      """<project xmlns="http://maven.apache.org/POM/4.0.0">
  <properties><python.wheelhouse></python.wheelhouse></properties>
</project>
""",
      encoding="utf-8",
    )
    errors = []

    metadata_check._validate_maven_contracts(self.repository, errors)

    self.assertIn(
      "root Maven property python.wheelhouse requires a non-empty "
      "online-build default",
      errors,
    )

  def test_assembly_cannot_strip_locked_wheel_content(self):
    for excluded_directory in ("doc", "docs", "examples", "test", "tests"):
      with self.subTest(excluded_directory=excluded_directory):
        assembly = self.repository / "ambari-agent/src/packages/tarball/all.xml"
        assembly.parent.mkdir(parents=True, exist_ok=True)
        assembly.write_text(
          f"""<assembly xmlns="http://maven.apache.org/plugins/maven-assembly-plugin/assembly/1.1.1">
  <fileSets><fileSet>
    <directory>${{python.dependencies.dir}}</directory>
    <excludes><exclude>**/{excluded_directory}/**</exclude></excludes>
  </fileSet></fileSets>
</assembly>
""",
          encoding="utf-8",
        )

        errors = metadata_check.audit(self.repository)

        self.assertIn(
          "Agent assembly must preserve locked wheel contents: "
          f"**/{excluded_directory}/**",
          errors,
        )

  def test_product_source_assembly_must_exclude_generated_bytecode(self):
    assembly = self.repository / "ambari-agent/src/packages/tarball/all.xml"
    assembly.parent.mkdir(parents=True, exist_ok=True)
    assembly.write_text(
      """<assembly xmlns="http://maven.apache.org/plugins/maven-assembly-plugin/assembly/1.1.1">
  <fileSets><fileSet>
    <directory>src/main/python/ambari_agent</directory>
  </fileSet></fileSets>
</assembly>
""",
      encoding="utf-8",
    )

    errors = metadata_check.audit(self.repository)

    self.assertIn(
      "Agent assembly source src/main/python/ambari_agent omits bytecode "
      "excludes: **/*.pyc,**/*.pyo,**/__pycache__/**",
      errors,
    )

  def test_python_test_cannot_write_unowned_bytecode_into_dependencies(self):
    agent_pom = self.repository / "ambari-agent/pom.xml"
    agent_pom.write_text(
      """<project xmlns="http://maven.apache.org/POM/4.0.0">
  <build><plugins><plugin><artifactId>exec-maven-plugin</artifactId><executions>
    <execution><id>python-test</id><phase>test</phase>
      <configuration><environmentVariables>
        <PYTHONPATH>${python.dependencies.dir}</PYTHONPATH>
      </environmentVariables></configuration>
    </execution>
  </executions></plugin></plugins></build>
</project>
""",
      encoding="utf-8",
    )

    errors = metadata_check.audit(self.repository)

    self.assertIn(
      "Agent Maven execution python-test requires PYTHONDONTWRITEBYTECODE=1",
      errors,
    )


if __name__ == "__main__":
  unittest.main()

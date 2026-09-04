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

"""Verify that Python dependency declarations match their requirements inputs."""

import argparse
import ast
from pathlib import Path
import re
import sys
from xml.etree import ElementTree

try:
  import tomllib
except ImportError:
  import tomli as tomllib


REQUIREMENT = re.compile(
  r"^(?P<name>[A-Za-z0-9][A-Za-z0-9_.-]*)==(?P<version>[^;\s]+)"
  r"(?:\s*;\s*(?P<marker>.+))?$"
)
SHA256_HASH = re.compile(r"(?:^|\s)--hash=sha256:[0-9a-fA-F]{64}(?=\s|$)")


class MetadataError(RuntimeError):
  pass


MAVEN_NAMESPACE = "http://maven.apache.org/POM/4.0.0"
BYTECODE_EXCLUDES = {"**/__pycache__/**", "**/*.pyc", "**/*.pyo"}
PRODUCT_PYTHON_SOURCE_DIRECTORIES = {
  "Agent": {
    "src/main/python/ambari_agent",
    "${project.basedir}/../ambari-common/src/main/python/ambari_commons",
    "${resourceManagementSrcLocation}",
    "src/examples",
    "${target.cache.dir}",
    "${pluggableStackDefinitionOutput}/custom_actions",
  },
  "Server": {
    "${project.basedir}/../ambari-common/src/main/python/ambari_commons",
    "${resourceManagementSrcLocation}",
    "src/main/resources/scripts",
    "${commonServicesSrcLocation}",
    "${stackHooksLocation}",
    "${stacksSrcLocation}",
    "src/main/python/ambari_server",
    "${customActionsRoot}",
    "src/main/resources/host_scripts",
  },
}


def _normalize_requirement(requirement):
  requirement = requirement.split("--hash=", 1)[0].strip()
  match = REQUIREMENT.fullmatch(requirement)
  if match is None:
    raise MetadataError(f"Dependency is not exactly pinned: {requirement}")
  name = re.sub(r"[-_.]+", "-", match.group("name")).lower()
  normalized = f"{name}=={match.group('version')}"
  if match.group("marker"):
    normalized += "; " + match.group("marker").strip().replace("'", '"')
  return normalized


def read_requirements(
  path, include_references=True, _seen=None, require_hashes=False
):
  path = Path(path).resolve()
  seen = set() if _seen is None else _seen
  if path in seen:
    raise MetadataError(f"Recursive requirements include: {path}")
  seen.add(path)
  requirements = set()
  logical_line = ""
  try:
    lines = path.read_text(encoding="utf-8").splitlines()
    for raw_line in lines:
      line = raw_line.split("#", 1)[0].strip()
      if not line:
        continue
      logical_line += line[:-1].rstrip() + " " if line.endswith("\\") else line
      if line.endswith("\\"):
        continue
      line = logical_line.strip()
      logical_line = ""
      if line.startswith("-r "):
        if include_references:
          requirements.update(
            read_requirements(
              path.parent / line[3:].strip(), True, seen, require_hashes
            )
          )
        continue
      if require_hashes:
        raw_hashes = re.findall(r"(?:^|\s)--hash=([^\s]+)", line)
        valid_hashes = SHA256_HASH.findall(line)
        if not valid_hashes or len(raw_hashes) != len(valid_hashes):
          raise MetadataError(f"Lock entry has no valid SHA-256 hash: {path}")
      requirements.add(_normalize_requirement(line))
    if logical_line:
      raise MetadataError(f"Dangling requirements continuation: {path}")
  finally:
    seen.remove(path)
  return requirements


def _validate_setup_contract(path, errors, label):
  tree = ast.parse(Path(path).read_text(encoding="utf-8"), filename=str(path))
  setup_calls = [
    node
    for node in ast.walk(tree)
    if isinstance(node, ast.Call)
    and isinstance(node.func, ast.Name)
    and node.func.id == "setup"
  ]
  if len(setup_calls) != 1:
    raise MetadataError(f"Expected one setup() call in {path}")
  for keyword in setup_calls[0].keywords:
    if keyword.arg == "install_requires":
      errors.append(
        f"{label} setup.py must not duplicate pyproject runtime dependencies"
      )


def _toml_requirements(values):
  return {_normalize_requirement(value) for value in values}


def _compare(errors, label, actual, expected):
  if actual == expected:
    return
  missing = sorted(expected - actual)
  unexpected = sorted(actual - expected)
  details = []
  if missing:
    details.append("missing=" + ",".join(missing))
  if unexpected:
    details.append("unexpected=" + ",".join(unexpected))
  errors.append(f"{label}: {'; '.join(details)}")


def _maven_executions(path):
  root = ElementTree.parse(path).getroot()
  namespace = {"m": MAVEN_NAMESPACE}
  executions = {}
  for plugin in root.findall(".//m:plugin", namespace):
    if plugin.findtext("m:artifactId", namespaces=namespace) != "exec-maven-plugin":
      continue
    for execution in plugin.findall("m:executions/m:execution", namespace):
      execution_id = execution.findtext("m:id", namespaces=namespace)
      if not execution_id:
        continue
      arguments = [
        argument.text or ""
        for argument in execution.findall(
          "m:configuration/m:arguments/m:argument", namespace
        )
      ]
      environment = {
        child.tag.rsplit("}", 1)[-1]: child.text or ""
        for child in execution.findall(
          "m:configuration/m:environmentVariables/*", namespace
        )
      }
      executions[execution_id] = {
        "arguments": arguments,
        "environment": environment,
        "phase": execution.findtext("m:phase", namespaces=namespace),
      }
  return executions


def _maven_properties(path):
  root = ElementTree.parse(path).getroot()
  namespace = {"m": MAVEN_NAMESPACE}
  properties = root.find("m:properties", namespace)
  if properties is None:
    return {}
  return {
    child.tag.rsplit("}", 1)[-1]: child.text or "" for child in properties
  }


def _require_maven_execution(
  errors,
  component,
  executions,
  execution_id,
  required_arguments=(),
  required_environment=None,
  phase=None,
):
  execution = executions.get(execution_id)
  if execution is None:
    errors.append(f"{component} Maven execution is missing: {execution_id}")
    return
  missing_arguments = [
    argument
    for argument in required_arguments
    if argument not in execution["arguments"]
  ]
  if missing_arguments:
    errors.append(
      f"{component} Maven execution {execution_id} omits arguments: "
      + ",".join(missing_arguments)
    )
  for name, value in (required_environment or {}).items():
    if execution["environment"].get(name) != value:
      errors.append(
        f"{component} Maven execution {execution_id} requires {name}={value}"
      )
  if phase is not None and execution["phase"] != phase:
    errors.append(
      f"{component} Maven execution {execution_id} must run in {phase}, "
      f"found {execution['phase']}"
    )


def _validate_maven_contracts(repository, errors):
  root_pom = repository / "pom.xml"
  if root_pom.is_file():
    properties = _maven_properties(root_pom)
    if not properties.get("python.wheelhouse", "").strip():
      errors.append(
        "root Maven property python.wheelhouse requires a non-empty online-build default"
      )

  agent_pom = repository / "ambari-agent/pom.xml"
  if agent_pom.is_file():
    executions = _maven_executions(agent_pom)
    _require_maven_execution(
      errors,
      "Agent",
      executions,
      "bundle-python-build-dependencies",
      (
        "--no-deps",
        "--only-binary=:all:",
        "--require-hashes",
        "${python.build.dependencies.dir}",
        "${maven.multiModuleProjectDirectory}/requirements-build.lock",
      ),
      phase="generate-resources",
    )
    _require_maven_execution(
      errors,
      "Agent",
      executions,
      "bundle-python-binary-dependencies",
      (
        "--no-deps",
        "--only-binary=:all:",
        "--platform",
        "${python.wheel.platform}",
        "--python-version",
        "${python.wheel.version}",
        "--abi",
        "${python.wheel.abi}",
        "--require-hashes",
        "${project.basedir}/src/main/python/requirements.lock",
      ),
      phase="generate-resources",
    )
    _require_maven_execution(
      errors,
      "Agent",
      executions,
      "bundle-python-sdist-dependencies",
      (
        "--no-build-isolation",
        "--use-pep517",
        "--no-binary=:all:",
        "--no-deps",
        "--require-hashes",
        "${project.basedir}/src/main/python/requirements-sdist.lock",
      ),
      {
        "PYTHONNOUSERSITE": "1",
        "PYTHONPATH": "${python.build.dependencies.dir}",
      },
      "generate-resources",
    )
    _require_maven_execution(
      errors,
      "Agent",
      executions,
      "normalize-python-dependencies",
      ("${python.dependencies.dir}",),
      phase="generate-resources",
    )
    _require_maven_execution(
      errors,
      "Agent",
      executions,
      "audit-python-dependencies",
      (
        "${project.basedir}/src/main/python/requirements.lock",
        "${project.basedir}/src/main/python/requirements-sdist.lock",
        "${project.basedir}/src/main/python/requirements.in",
        "${project.basedir}/src/main/python/requirements-sdist.in",
      ),
      phase="prepare-package",
    )
    _require_maven_execution(
      errors,
      "Agent",
      executions,
      "python-test",
      required_environment={"PYTHONDONTWRITEBYTECODE": "1"},
      phase="test",
    )

  server_pom = repository / "ambari-server/pom.xml"
  if server_pom.is_file():
    executions = _maven_executions(server_pom)
    _require_maven_execution(
      errors,
      "Server",
      executions,
      "bundle-python-dependencies",
      (
        "--no-deps",
        "--only-binary=:all:",
        "--platform",
        "${python.wheel.platform}",
        "--python-version",
        "${python.wheel.version}",
        "--abi",
        "${python.wheel.abi}",
        "--require-hashes",
        "${project.basedir}/src/main/python/requirements.lock",
      ),
      phase="generate-resources",
    )
    _require_maven_execution(
      errors,
      "Server",
      executions,
      "normalize-python-dependencies",
      ("${python.dependencies.dir}",),
      phase="generate-resources",
    )
    _require_maven_execution(
      errors,
      "Server",
      executions,
      "audit-python-dependencies",
      (
        "${project.basedir}/src/main/python/requirements.lock",
        "${project.basedir}/src/main/python/requirements.in",
      ),
      phase="prepare-package",
    )
    _require_maven_execution(
      errors,
      "Server",
      executions,
      "python-test",
      required_environment={"PYTHONDONTWRITEBYTECODE": "1"},
      phase="test",
    )


def _validate_assembly_contracts(repository, errors):
  assemblies = (
    ("Agent", repository / "ambari-agent/src/packages/tarball/all.xml"),
    ("Server", repository / "ambari-server/src/main/assemblies/server.xml"),
  )
  forbidden_excludes = {
    "**/doc/**",
    "**/docs/**",
    "**/examples/**",
    "**/test/**",
    "**/tests/**",
  }
  for component, assembly_path in assemblies:
    if not assembly_path.is_file():
      continue
    root = ElementTree.parse(assembly_path).getroot()
    for file_set in root.iter():
      if file_set.tag.rsplit("}", 1)[-1] != "fileSet":
        continue
      directory = next(
        (
          child.text or ""
          for child in file_set
          if child.tag.rsplit("}", 1)[-1] == "directory"
        ),
        "",
      )
      excludes = {
        element.text or ""
        for element in file_set.iter()
        if element.tag.rsplit("}", 1)[-1] == "exclude"
      }
      if "${python.dependencies.dir}" in directory:
        forbidden = sorted(excludes & forbidden_excludes)
        if forbidden:
          errors.append(
            f"{component} assembly must preserve locked wheel contents: "
            + ",".join(forbidden)
          )
      if directory in PRODUCT_PYTHON_SOURCE_DIRECTORIES[component]:
        missing = sorted(BYTECODE_EXCLUDES - excludes)
        if missing:
          errors.append(
            f"{component} assembly source {directory} omits bytecode excludes: "
            + ",".join(missing)
          )


def audit(repository):
  repository = Path(repository).resolve()
  common_path = repository / "ambari-common/src/main/python/requirements.in"
  agent_path = repository / "ambari-agent/src/main/python/requirements.in"
  server_path = repository / "ambari-server/src/main/python/requirements.in"
  build_path = repository / "requirements-build.in"
  tooling_path = repository / "requirements-tooling.txt"

  common = read_requirements(common_path)
  agent = read_requirements(agent_path)
  agent_direct = read_requirements(agent_path, include_references=False)
  server_direct = read_requirements(server_path, include_references=False)
  agent_sdist_path = repository / "ambari-agent/src/main/python/requirements-sdist.in"
  build = read_requirements(build_path)
  tooling = read_requirements(tooling_path, require_hashes=True)
  project = tomllib.loads((repository / "pyproject.toml").read_text(encoding="utf-8"))
  agent_project = tomllib.loads(
    (repository / "ambari-agent/src/main/python/pyproject.toml").read_text(
      encoding="utf-8"
    )
  )

  errors = []
  _validate_setup_contract(repository / "setup.py", errors, "root")
  _validate_setup_contract(
    repository / "ambari-agent/src/main/python/setup.py", errors, "Agent"
  )
  _compare(
    errors,
    "pyproject common dependencies",
    _toml_requirements(project["project"]["dependencies"]),
    common,
  )
  optional = project["project"]["optional-dependencies"]
  ruff_excludes = set(project.get("tool", {}).get("ruff", {}).get("exclude", ()))
  if "contrib" in ruff_excludes:
    errors.append("Ruff must scan contrib Python sources")
  _compare(
    errors,
    "pyproject Agent dependencies",
    _toml_requirements(optional["agent"]),
    agent_direct,
  )
  _compare(
    errors,
    "pyproject Server dependencies",
    _toml_requirements(optional["server"]),
    server_direct,
  )
  _compare(
    errors,
    "pyproject tooling dependencies",
    _toml_requirements(optional["tooling"]),
    tooling,
  )
  _compare(
    errors,
    "Agent pyproject dependencies",
    _toml_requirements(agent_project["project"]["dependencies"]),
    agent,
  )
  for label, metadata in (("root", project), ("Agent", agent_project)):
    if metadata["project"].get("requires-python") != ">=3.9.2":
      errors.append(f"{label} pyproject requires-python must be >=3.9.2")
    build_backend = _toml_requirements(metadata["build-system"]["requires"])
    missing_build_requirements = build_backend - build
    if missing_build_requirements:
      errors.append(
        f"{label} pyproject build backend is absent from requirements-build.in: "
        + ",".join(sorted(missing_build_requirements))
      )
  if agent_project["project"].get("scripts", {}).get("ambari-agent") != (
    "ambari_agent.AmbariAgent:main"
  ):
    errors.append("Agent pyproject must declare the ambari-agent console script")
  lock_pairs = (
    ("Common", common_path, common_path.with_suffix(".lock")),
    ("Agent", agent_path, agent_path.with_suffix(".lock")),
    ("Agent sdist", agent_sdist_path, agent_sdist_path.with_suffix(".lock")),
    ("Server", server_path, server_path.with_suffix(".lock")),
    ("Build", build_path, repository / "requirements-build.lock"),
  )
  for label, manifest_path, lock_path in lock_pairs:
    manifest = read_requirements(manifest_path)
    locked = read_requirements(lock_path, require_hashes=True)
    missing = sorted(manifest - locked)
    if missing:
      errors.append(f"{label} lock omits manifest pins: {','.join(missing)}")
  _validate_maven_contracts(repository, errors)
  _validate_assembly_contracts(repository, errors)
  return errors


def main(argv=None):
  parser = argparse.ArgumentParser(description=__doc__)
  parser.add_argument("repository", nargs="?", default=Path(__file__).parents[1])
  arguments = parser.parse_args(argv)
  try:
    errors = audit(arguments.repository)
  except (KeyError, MetadataError, OSError, SyntaxError, ValueError) as exception:
    parser.exit(1, f"Python dependency metadata check failed: {exception}\n")
  if errors:
    parser.exit(1, "Python dependency metadata differs:\n- " + "\n- ".join(errors) + "\n")
  print("Python dependency metadata is consistent")
  return 0


if __name__ == "__main__":
  sys.exit(main())

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

"""Audit the Python dependency tree embedded in Ambari packages."""

import argparse
import base64
import csv
from email.parser import Parser
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import re
import struct
import sys
import tempfile


LOCK_ENTRY = re.compile(
  r"^([A-Za-z0-9][A-Za-z0-9._-]*)==([^\s\\;]+)(?:\s|;|$)"
)
LOCK_HASH = re.compile(r"(?:^|\s)--hash=sha256:([0-9a-fA-F]{64})(?=\s|$)")
REQUIREMENT = re.compile(
  r"^\s*([A-Za-z0-9][A-Za-z0-9._-]*)(?:\[[^]]*\])?\s*"
  r"(?:\(([^)]*)\)|([^;\s][^;]*?))?\s*(?:;\s*(.*))?$"
)
MARKER_TOKEN = re.compile(
  r"\s*(not\s+in\b|===|~=|==|!=|<=|>=|<|>|in\b|and\b|or\b|\(|\)|"
  r"[A-Za-z_][A-Za-z0-9_]*|'[^']*'|\"[^\"]*\")"
)
NATIVE_SUFFIXES = (".so", ".pyd", ".dylib")
FORBIDDEN_DISTRIBUTIONS = {
  "coilmq",
  "mock",
  "simplejson",
  "pyaes",
  "pbkdf2",
  "ws4py",
}
LICENSE_CONCLUSIONS = {
  "apscheduler": "MIT",
  "cffi": "MIT-0",
  "cryptography": "Apache-2.0",
  "distro": "Apache-2.0",
  "docopt": "MIT",
  "javaproperties": "MIT",
  "jinja2": "BSD-3-Clause",
  "markupsafe": "BSD-3-Clause",
  "pycparser": "BSD-3-Clause",
  "pyyaml": "MIT",
  "stomp-py": "Apache-2.0",
  "typing-extensions": "PSF-2.0",
  "tzlocal": "MIT",
  "websocket-client": "Apache-2.0",
}
LICENSE_SEARCH_NAMES = {
  "stomp-py": ("stomp.py",),
  "typing-extensions": ("typing_extensions", "typing-extensions"),
  "websocket-client": ("websocket-client",),
}
LICENSE_DECLARATIONS = {
  "apscheduler": ("MIT",),
  "cffi": ("MIT",),
  "cryptography": ("Apache-2.0 OR BSD-3-Clause",),
  "distro": ("Apache License, Version 2.0", "Apache-2.0"),
  "docopt": ("MIT",),
  "javaproperties": ("MIT",),
  "jinja2": ("BSD-3-Clause",),
  "markupsafe": ("BSD-3-Clause",),
  "pycparser": ("BSD-3-Clause",),
  "pyyaml": ("MIT",),
  "stomp-py": ("Apache-2.0", "Apache Software License"),
  "typing-extensions": ("PSF-2.0",),
  "tzlocal": ("MIT",),
  "websocket-client": ("Apache-2.0",),
}
LICENSE_EVIDENCE_MARKERS = {
  "Apache-2.0": ("apache-2.0", "apache license"),
  "BSD-3-Clause": ("bsd-3-clause", "redistribution and use in source and binary forms"),
  "MIT": ("mit", "permission is hereby granted, free of charge"),
  "MIT-0": ("mit no attribution",),
  "PSF-2.0": ("psf-2.0", "python software foundation license version 2"),
}
ROOT_LICENSE_MARKERS = {
  "Apache-2.0": "apache license",
  "BSD-3-Clause": "bsd 3-clause license",
  "MIT": "permission is hereby granted, free of charge",
  "MIT-0": "mit no attribution license",
  "PSF-2.0": "python software foundation license version 2",
}
ELF_MACHINES = {
  "manylinux2014_x86_64": 62,
  "manylinux2014_aarch64": 183,
}


class AuditError(RuntimeError):
  pass


def normalize_name(name):
  return re.sub(r"[-_.]+", "-", name).lower()


def _logical_lock_lines(path):
  parts = []
  start_line = None
  for line_number, raw_line in enumerate(
    Path(path).read_text(encoding="utf-8").splitlines(), start=1
  ):
    line = raw_line.strip()
    if not line or line.startswith("#"):
      continue
    if start_line is None:
      start_line = line_number
    continued = line.endswith("\\")
    parts.append(line[:-1].rstrip() if continued else line)
    if not continued:
      yield start_line, " ".join(parts)
      parts = []
      start_line = None
  if parts:
    raise AuditError(f"Dangling lock-file continuation: {path}:{start_line}")


def parse_locks(paths):
  expected = {}
  for path in paths:
    entries = list(_logical_lock_lines(path))
    if not entries:
      raise AuditError(f"Lock file contains no pinned distributions: {path}")
    for line_number, entry in entries:
      match = LOCK_ENTRY.match(entry)
      if match is None:
        raise AuditError(f"Lock entry is not pinned: {path}:{line_number}")
      raw_hashes = re.findall(r"(?:^|\s)--hash=([^\s]+)", entry)
      hashes = LOCK_HASH.findall(entry)
      if not hashes:
        raise AuditError(f"Lock entry has no SHA-256 hash: {path}:{line_number}")
      if len(raw_hashes) != len(hashes):
        raise AuditError(f"Lock entry has a malformed hash: {path}:{line_number}")
      raw_name, version = match.groups()
      name = normalize_name(raw_name)
      previous = expected.get(name)
      if previous is not None and previous != version:
        raise AuditError(
          f"Conflicting lock versions for {name}: {previous} and {version}"
        )
      expected[name] = version
  return expected


def parse_manifests(paths, _seen=None):
  expected = {}
  seen = set() if _seen is None else _seen
  for raw_path in paths:
    path = Path(raw_path).resolve()
    if path in seen:
      raise AuditError(f"Recursive requirements include: {path}")
    seen.add(path)
    try:
      for line_number, entry in _logical_lock_lines(path):
        if entry.startswith("-r "):
          nested = parse_manifests([path.parent / entry[3:].strip()], seen)
          for name, version in nested.items():
            previous = expected.get(name)
            if previous is not None and previous != version:
              raise AuditError(
                f"Conflicting manifest versions for {name}: {previous} and {version}"
              )
            expected[name] = version
          continue
        match = LOCK_ENTRY.match(entry)
        if match is None:
          raise AuditError(f"Manifest entry is not pinned: {path}:{line_number}")
        raw_name, version = match.groups()
        name = normalize_name(raw_name)
        previous = expected.get(name)
        if previous is not None and previous != version:
          raise AuditError(
            f"Conflicting manifest versions for {name}: {previous} and {version}"
          )
        expected[name] = version
    finally:
      seen.remove(path)
  return expected


def _version_parts(value):
  result = []
  for part in re.findall(r"\d+|[A-Za-z]+", value):
    result.append(int(part) if part.isdigit() else part.lower())
  return tuple(result)


def _compare(left, operator, right, version_comparison=False):
  if operator in ("in", "not in"):
    result = left in right
    return not result if operator == "not in" else result

  if version_comparison:
    left = _version_parts(left)
    right = _version_parts(right)
  if operator in ("==", "==="):
    return left == right
  if operator == "!=":
    return left != right
  if operator == "<":
    return left < right
  if operator == "<=":
    return left <= right
  if operator == ">":
    return left > right
  if operator == ">=":
    return left >= right
  if operator == "~=":
    prefix_length = max(1, len(right) - 1)
    return left >= right and left[:prefix_length] == right[:prefix_length]
  raise AuditError(f"Unsupported comparison operator: {operator}")


class MarkerEvaluator:
  def __init__(self, marker, environment):
    self.environment = environment
    self.tokens = self._tokenize(marker)
    self.position = 0

  @staticmethod
  def _tokenize(marker):
    tokens = []
    position = 0
    while position < len(marker):
      match = MARKER_TOKEN.match(marker, position)
      if match is None:
        raise AuditError(f"Unsupported environment marker: {marker}")
      tokens.append(match.group(1))
      position = match.end()
    return tokens

  def evaluate(self):
    result = self._parse_or()
    if self.position != len(self.tokens):
      raise AuditError("Unexpected trailing environment marker tokens")
    return result

  def _accept(self, value):
    if self.position < len(self.tokens) and self.tokens[self.position] == value:
      self.position += 1
      return True
    return False

  def _parse_or(self):
    result = self._parse_and()
    while self._accept("or"):
      other = self._parse_and()
      result = result or other
    return result

  def _parse_and(self):
    result = self._parse_atom()
    while self._accept("and"):
      other = self._parse_atom()
      result = result and other
    return result

  def _parse_atom(self):
    if self._accept("("):
      result = self._parse_or()
      if not self._accept(")"):
        raise AuditError("Unbalanced environment marker parentheses")
      return result

    if self.position + 2 >= len(self.tokens):
      raise AuditError("Incomplete environment marker comparison")
    left_token, operator, right_token = self.tokens[self.position : self.position + 3]
    self.position += 3
    left, left_is_version = self._resolve(left_token)
    right, right_is_version = self._resolve(right_token)
    return _compare(left, operator, right, left_is_version or right_is_version)

  def _resolve(self, token):
    if token[:1] in ("'", '"'):
      return token[1:-1], False
    if token not in self.environment:
      raise AuditError(f"Unknown environment marker variable: {token}")
    return self.environment[token], token in ("python_version", "python_full_version")


def _requirement_parts(requirement):
  match = REQUIREMENT.match(requirement)
  if match is None:
    raise AuditError(f"Unsupported Requires-Dist value: {requirement}")
  name, parenthesized, unparenthesized, marker = match.groups()
  return normalize_name(name), (parenthesized or unparenthesized or "").strip(), marker


def _satisfies(version, specifier):
  for clause in filter(None, (part.strip() for part in specifier.split(","))):
    match = re.match(r"^(===|~=|==|!=|<=|>=|<|>)\s*([^\s]+)$", clause)
    if match is None:
      raise AuditError(f"Unsupported version specifier: {specifier}")
    operator, expected = match.groups()
    if operator in ("==", "!=") and expected.endswith(".*"):
      matches = version == expected[:-2] or version.startswith(expected[:-1])
      if matches != (operator == "=="):
        return False
    elif not _compare(version, operator, expected, True):
      return False
  return True


def _read_metadata(root):
  distributions = {}
  for metadata_path in sorted(root.glob("*.dist-info/METADATA")):
    metadata = Parser().parsestr(metadata_path.read_text(encoding="utf-8"))
    raw_name = metadata.get("Name")
    version = metadata.get("Version")
    if not raw_name or not version:
      raise AuditError(f"Incomplete distribution metadata: {metadata_path}")
    name = normalize_name(raw_name)
    if name in distributions:
      raise AuditError(f"Duplicate distribution metadata for {name}")
    distributions[name] = {
      "metadata": metadata,
      "metadata_path": metadata_path,
      "name": raw_name,
      "version": version,
    }
  return distributions


def _target_environment(python_version, platform):
  short_version = ".".join(python_version.split(".")[:2])
  machine = "aarch64" if platform.endswith("aarch64") else "x86_64"
  return {
    "extra": "",
    "implementation_name": "cpython",
    "implementation_version": python_version,
    "os_name": "posix",
    "platform_machine": machine,
    "platform_python_implementation": "CPython",
    "platform_release": "",
    "platform_system": "Linux",
    "platform_version": "",
    "python_full_version": python_version,
    "python_version": short_version,
    "sys_platform": "linux",
  }


def _validate_dependency_closure(distributions, environment):
  for name, distribution in distributions.items():
    for requirement in distribution["metadata"].get_all("Requires-Dist", []):
      dependency, specifier, marker = _requirement_parts(requirement)
      if marker and not MarkerEvaluator(marker, environment).evaluate():
        continue
      installed = distributions.get(dependency)
      if installed is None:
        raise AuditError(f"{name} requires missing distribution {dependency}")
      if specifier and not _satisfies(installed["version"], specifier):
        raise AuditError(
          f"{name} requires {dependency}{specifier}, found {installed['version']}"
        )


def _validate_reachable_closure(distributions, roots, environment):
  pending = list(roots)
  reachable = set()
  while pending:
    name = pending.pop()
    if name in reachable:
      continue
    distribution = distributions.get(name)
    if distribution is None:
      raise AuditError(f"Manifest requires missing distribution {name}")
    reachable.add(name)
    for requirement in distribution["metadata"].get_all("Requires-Dist", []):
      dependency, _, marker = _requirement_parts(requirement)
      if marker and not MarkerEvaluator(marker, environment).evaluate():
        continue
      pending.append(dependency)

  unreachable = sorted(set(distributions) - reachable)
  if unreachable:
    raise AuditError(
      "Locked distributions are not reachable from runtime manifests: "
      + ", ".join(unreachable)
    )


def _compatible_platform_tags(platform):
  tags = {platform, "any"}
  match = re.fullmatch(r"manylinux2014_(x86_64|aarch64)", platform)
  if match:
    tags.add(f"manylinux_2_17_{match.group(1)}")
  return tags


def _python_abi_tag_is_compatible(python_tag, wheel_abi, target_abi):
  target = re.fullmatch(r"cp(\d)(\d+)", target_abi)
  if target is None:
    raise AuditError(f"Unsupported target Python ABI: {target_abi}")
  target_major, target_minor = (int(part) for part in target.groups())
  target_digits = f"{target_major}{target_minor}"

  if wheel_abi == "none":
    return python_tag in ("py3", f"py{target_digits}", target_abi)
  if wheel_abi == target_abi:
    return python_tag == target_abi
  if wheel_abi != "abi3":
    return False

  stable_tag = re.fullmatch(r"cp(\d)(\d+)", python_tag)
  if stable_tag is None:
    return False
  stable_major, stable_minor = (int(part) for part in stable_tag.groups())
  return stable_major == target_major and stable_minor <= target_minor


def _validate_wheel_tags(distributions, platform, abi):
  compatible_platforms = _compatible_platform_tags(platform)
  for name, distribution in distributions.items():
    wheel_path = distribution["metadata_path"].with_name("WHEEL")
    if not wheel_path.is_file():
      raise AuditError(f"Missing WHEEL metadata for distribution {name}")
    wheel_metadata = Parser().parsestr(wheel_path.read_text(encoding="utf-8"))
    tags = wheel_metadata.get_all("Tag", [])
    if not tags:
      raise AuditError(f"Distribution {name} declares no wheel compatibility tag")

    compatible = False
    for tag in tags:
      parts = tag.split("-")
      if len(parts) != 3:
        raise AuditError(f"Malformed wheel compatibility tag for {name}: {tag}")
      python_tags, wheel_abis, platform_tags = (
        set(part.split(".")) for part in parts
      )
      if not compatible_platforms.intersection(platform_tags):
        continue
      if any(
        _python_abi_tag_is_compatible(python_tag, wheel_abi, abi)
        for python_tag in python_tags
        for wheel_abi in wheel_abis
      ):
        compatible = True
        break

    if not compatible:
      raise AuditError(
        f"Distribution {name} wheel tags are incompatible with {abi}/{platform}: "
        + ", ".join(tags)
      )


def _record_path(root, raw_path):
  pure_path = PurePosixPath(raw_path)
  if pure_path.is_absolute() or ".." in pure_path.parts:
    raise AuditError(f"Distribution RECORD escapes dependency root: {raw_path}")
  path = root.joinpath(*pure_path.parts)
  try:
    path.resolve(strict=False).relative_to(root.resolve())
  except ValueError as exception:
    raise AuditError(f"Distribution RECORD escapes dependency root: {raw_path}") from exception
  return path


def _validate_records(root, distributions):
  owned_paths = set()
  for name, distribution in distributions.items():
    record_path = distribution["metadata_path"].with_name("RECORD")
    if not record_path.is_file():
      raise AuditError(f"Missing RECORD for distribution {name}")
    with record_path.open(newline="", encoding="utf-8") as record_file:
      for raw_path, digest, raw_size in csv.reader(record_file):
        path = _record_path(root, raw_path)
        relative = path.relative_to(root).as_posix()
        if relative in owned_paths:
          raise AuditError(f"RECORD path has multiple owners: {relative}")
        owned_paths.add(relative)
        if not path.is_file():
          raise AuditError(f"RECORD path is missing: {relative}")
        if raw_size and path.stat().st_size != int(raw_size):
          raise AuditError(f"RECORD size mismatch: {relative}")
        if digest:
          algorithm, separator, encoded_digest = digest.partition("=")
          if separator != "=" or algorithm != "sha256":
            raise AuditError(f"Unsupported RECORD digest for {relative}: {digest}")
          actual = base64.urlsafe_b64encode(hashlib.sha256(path.read_bytes()).digest())
          actual = actual.rstrip(b"=").decode("ascii")
          if actual != encoded_digest:
            raise AuditError(f"RECORD digest mismatch: {relative}")

  actual_paths = {
    path.relative_to(root).as_posix()
    for path in root.rglob("*")
    if path.is_file() or path.is_symlink()
  }
  unowned = sorted(actual_paths - owned_paths)
  if unowned:
    raise AuditError(f"Files without distribution RECORD ownership: {', '.join(unowned)}")


def _elf_machine(path):
  with path.open("rb") as stream:
    header = stream.read(20)
  if len(header) < 20 or header[:4] != b"\x7fELF":
    raise AuditError(f"Native extension is not ELF: {path}")
  if header[5] == 1:
    byte_order = "<"
  elif header[5] == 2:
    byte_order = ">"
  else:
    raise AuditError(f"Native extension has invalid ELF byte order: {path}")
  return struct.unpack(f"{byte_order}H", header[18:20])[0]


def _validate_tree(root, platform, abi):
  expected_machine = ELF_MACHINES.get(platform)
  if expected_machine is None:
    raise AuditError(f"Unsupported target wheel platform: {platform}")
  expected_python = abi.removeprefix("cp")
  native_files = []

  for path in sorted(root.rglob("*")):
    relative = path.relative_to(root)
    if path.is_symlink():
      raise AuditError(f"Symlinks are not allowed in dependency tree: {relative}")
    if path.is_file() and path.suffix in (".pyc", ".pyo"):
      raise AuditError(f"Precompiled Python bytecode is not allowed: {relative}")
    if not path.is_file():
      continue
    if path.name == "_posixsubprocess.so":
      raise AuditError("Interpreter-private _posixsubprocess.so is bundled")
    if path.suffix.lower() in (".pyd", ".dylib"):
      raise AuditError(f"Non-Linux native extension is bundled: {relative}")
    if ".so" not in path.name:
      continue

    match = re.search(r"\.cpython-(\d+)-", path.name)
    if match and match.group(1) != expected_python:
      raise AuditError(
        f"Native extension {relative} targets CPython {match.group(1)}, expected {abi}"
      )
    if re.search(r"\.(?:cpython-2\d|pypy|jython)", path.name, re.IGNORECASE):
      raise AuditError(f"Unsupported Python ABI extension is bundled: {relative}")
    machine = _elf_machine(path)
    if machine != expected_machine:
      raise AuditError(
        f"Native extension {relative} has ELF machine {machine}, expected {expected_machine}"
      )
    native_files.append(relative.as_posix())
  return native_files


def _validate_licenses(distributions, license_path):
  license_text = Path(license_path).read_text(encoding="utf-8").lower()
  conclusions = {}
  for name, distribution in distributions.items():
    conclusion = LICENSE_CONCLUSIONS.get(name)
    if conclusion is None:
      raise AuditError(f"Distribution {name} has no reviewed license conclusion")
    search_names = LICENSE_SEARCH_NAMES.get(name, (distribution["name"], name))
    if not any(candidate.lower() in license_text for candidate in search_names):
      raise AuditError(f"Root LICENSE does not identify bundled distribution {name}")
    metadata = distribution["metadata"]
    licenses_dir = distribution["metadata_path"].parent / "licenses"
    license_files = (
      [path for path in licenses_dir.rglob("*") if path.is_file()]
      if licenses_dir.is_dir()
      else []
    )
    declared = metadata.get("License-Expression") or metadata.get("License")
    if declared and declared not in LICENSE_DECLARATIONS[name]:
      raise AuditError(
        f"Distribution {name} declares unexpected license {declared!r}; "
        f"reviewed={LICENSE_DECLARATIONS[name]}"
      )
    if not declared and not license_files:
      raise AuditError(f"Distribution {name} contains no license metadata or license file")
    evidence = "\n".join(
      [declared or ""]
      + [path.read_text(encoding="utf-8", errors="replace") for path in license_files]
    ).lower()
    markers = LICENSE_EVIDENCE_MARKERS[conclusion]
    if not any(marker in evidence for marker in markers):
      raise AuditError(
        f"Distribution {name} license evidence does not support reviewed "
        f"conclusion {conclusion}"
      )
    root_marker = ROOT_LICENSE_MARKERS[conclusion]
    if root_marker not in license_text:
      raise AuditError(
        f"Root LICENSE lacks {conclusion} text required by distribution {name}"
      )
    conclusions[name] = conclusion
  return conclusions


def _write_json_atomic(path, value):
  output = Path(path)
  output.parent.mkdir(parents=True, exist_ok=True)
  descriptor, temporary_name = tempfile.mkstemp(
    dir=output.parent, prefix=f".{output.name}.", suffix=".tmp"
  )
  try:
    with os.fdopen(descriptor, "w", encoding="utf-8") as stream:
      json.dump(value, stream, indent=2, sort_keys=True)
      stream.write("\n")
      stream.flush()
      os.fsync(stream.fileno())
    os.replace(temporary_name, output)
  except Exception:
    try:
      os.unlink(temporary_name)
    except FileNotFoundError:
      pass
    raise


def audit(
  root,
  lock_paths,
  license_path,
  platform,
  python_version,
  abi,
  project_version,
  manifest_paths=None,
):
  root = Path(root)
  if not root.is_dir():
    raise AuditError(f"Dependency root does not exist: {root}")
  expected = parse_locks(lock_paths)
  forbidden = sorted(FORBIDDEN_DISTRIBUTIONS.intersection(expected))
  if forbidden:
    raise AuditError(f"Forbidden runtime distributions are locked: {', '.join(forbidden)}")

  distributions = _read_metadata(root)
  installed = {name: value["version"] for name, value in distributions.items()}
  if installed != expected:
    missing = sorted(set(expected) - set(installed))
    unexpected = sorted(set(installed) - set(expected))
    mismatched = sorted(
      name
      for name in set(expected).intersection(installed)
      if expected[name] != installed[name]
    )
    details = []
    if missing:
      details.append(f"missing={','.join(missing)}")
    if unexpected:
      details.append(f"unexpected={','.join(unexpected)}")
    if mismatched:
      details.append(
        "versions="
        + ",".join(
          f"{name}:{installed[name]}!={expected[name]}" for name in mismatched
        )
      )
    raise AuditError("Installed distributions differ from locks: " + "; ".join(details))

  environment = _target_environment(python_version, platform)
  _validate_dependency_closure(distributions, environment)
  if manifest_paths:
    manifest = parse_manifests(manifest_paths)
    manifest_mismatches = sorted(
      name
      for name, version in manifest.items()
      if expected.get(name) != version
    )
    if manifest_mismatches:
      raise AuditError(
        "Runtime manifest versions differ from locks: "
        + ", ".join(manifest_mismatches)
      )
    _validate_reachable_closure(distributions, manifest, environment)
  _validate_wheel_tags(distributions, platform, abi)
  _validate_records(root, distributions)
  native_files = _validate_tree(root, platform, abi)
  licenses = _validate_licenses(distributions, license_path)

  components = []
  for name in sorted(distributions):
    distribution = distributions[name]
    components.append(
      {
        "licenseConcluded": licenses[name],
        "licenseDeclared": distribution["metadata"].get("License-Expression")
        or distribution["metadata"].get("License")
        or "SEE-DIST-INFO-LICENSES",
        "name": distribution["name"],
        "requiresDist": distribution["metadata"].get_all("Requires-Dist", []),
        "version": distribution["version"],
      }
    )
  return {
    "abi": abi,
    "components": components,
    "format": "ambari-python-runtime-sbom-v1",
    "nativeFiles": native_files,
    "platform": platform,
    "projectVersion": project_version,
    "pythonVersion": python_version,
  }


def main(argv=None):
  parser = argparse.ArgumentParser(description=__doc__)
  parser.add_argument("--root", required=True)
  parser.add_argument("--lock", action="append", required=True)
  parser.add_argument("--manifest", action="append", required=True)
  parser.add_argument("--license", required=True)
  parser.add_argument("--platform", required=True)
  parser.add_argument("--python-version", required=True)
  parser.add_argument("--abi", required=True)
  parser.add_argument("--project-version", required=True)
  parser.add_argument("--sbom", required=True)
  arguments = parser.parse_args(argv)
  try:
    sbom = audit(
      arguments.root,
      arguments.lock,
      arguments.license,
      arguments.platform,
      arguments.python_version,
      arguments.abi,
      arguments.project_version,
      arguments.manifest,
    )
    _write_json_atomic(arguments.sbom, sbom)
  except (AuditError, OSError, UnicodeError, ValueError) as exception:
    parser.exit(1, f"Python dependency artifact audit failed: {exception}\n")
  print(
    f"Audited {len(sbom['components'])} Python distributions and "
    f"{len(sbom['nativeFiles'])} native extensions"
  )
  return 0


if __name__ == "__main__":
  sys.exit(main())

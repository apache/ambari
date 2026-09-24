"""
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements. See the NOTICE file
distributed with this work for additional information
regarding copyright ownership. The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
"""

import json
import re
from importlib.resources import files

from jsonschema import Draft202012Validator

SCHEMA_VERSION = 1
MAX_MANIFEST_BYTES = 1024 * 1024
DIGEST = re.compile(r"[a-f0-9]{64}")
VERSION = re.compile(r"(?:0|[1-9][0-9]{0,9})(?:\.(?:0|[1-9][0-9]{0,9})){0,4}")


class ContractError(Exception):
    def __init__(self, code, message, details=None):
        super().__init__(message)
        self.code = code
        self.details = details or {}


def unique_keys(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ContractError("INVALID_JSON", "Duplicate JSON object key")
        result[key] = value
    return result


def read_json(content):
    def invalid_constant(_value):
        raise ContractError("INVALID_JSON", "Non-finite JSON number")

    try:
        return json.loads(content, object_pairs_hook=unique_keys, parse_constant=invalid_constant)
    except (ValueError, UnicodeError) as error:
        raise ContractError("INVALID_JSON", "Invalid JSON document") from error


def canonical(value):
    return json.dumps(value, sort_keys=True, separators=(",", ":"), ensure_ascii=False, allow_nan=False)


def compare_versions(left, right):
    if not isinstance(left, str) or not isinstance(right, str) or not VERSION.fullmatch(left) or not VERSION.fullmatch(right):
        raise ContractError("INVALID_VERSION", "Expected one to five numeric version components")
    a = tuple(map(int, left.split(".")))
    b = tuple(map(int, right.split(".")))
    width = max(len(a), len(b))
    a += (0,) * (width - len(a))
    b += (0,) * (width - len(b))
    return (a > b) - (a < b)


def validate_manifest(content):
    if len(content.encode("utf-8") if isinstance(content, str) else content) > MAX_MANIFEST_BYTES:
        raise ContractError("INVALID_MANIFEST", "Manifest exceeds the size limit")
    manifest = read_json(content)
    if not isinstance(manifest, dict) or type(manifest.get("schema_version")) is not int or manifest["schema_version"] != 1:
        raise ContractError("UNSUPPORTED_SCHEMA", "Manifest schema_version must be the integer 1")
    schema = read_json(files("ambari_mpack").joinpath("schemas/manifest-v1.schema.json").read_text(encoding="utf-8"))
    error = next(Draft202012Validator(schema).iter_errors(manifest), None)
    if error is not None:
        raise ContractError("INVALID_MANIFEST", "Manifest does not satisfy the published schema",
                            {"path": list(error.absolute_path), "rule": error.validator})
    artifacts = manifest["artifacts"]
    if len({artifact["name"] for artifact in artifacts}) != len(artifacts):
        raise ContractError("INVALID_MANIFEST", "Duplicate artifact name")
    directories = []
    for artifact in artifacts:
        source = artifact["source_dir"]
        if any(source == other or source.startswith(other + "/") or other.startswith(source + "/")
               for other in directories):
            raise ContractError("INVALID_MANIFEST", "Artifact source directories overlap")
        directories.append(source)
        targets = set()
        for mapping in artifact.get("service_versions_map", []):
            for target in mapping["applicable_stacks"]:
                key = (mapping["service_name"], target["stack_name"], target["stack_version"])
                if key in targets:
                    raise ContractError("INVALID_MANIFEST", "Duplicate addon service target")
                targets.add(key)
    names = set()
    for dependency in manifest.get("dependencies", []):
        if dependency["name"] == manifest["name"] or dependency["name"] in names:
            raise ContractError("INVALID_MANIFEST", "Self or duplicate dependency")
        names.add(dependency["name"])
        if "min_version" in dependency and "max_version" in dependency:
            if compare_versions(dependency["min_version"], dependency["max_version"]) > 0:
                raise ContractError("INVALID_MANIFEST", "Dependency version range is empty")
    phases = set()
    for hook in manifest.get("hooks", []):
        if hook["name"] in phases or type(hook["timeout_seconds"]) is not int:
            raise ContractError("INVALID_MANIFEST", "Duplicate hook phase or non-integer timeout")
        phases.add(hook["name"])
    prerequisites = manifest.get("prerequisites", {})
    if "min_ambari_version" in prerequisites and "max_ambari_version" in prerequisites:
        if compare_versions(prerequisites["min_ambari_version"], prerequisites["max_ambari_version"]) > 0:
            raise ContractError("INVALID_MANIFEST", "Ambari version range is empty")
    stacks = [(target["stack_name"], target["stack_version"])
              for target in prerequisites.get("min_stack_versions", [])]
    if len(set(stacks)) != len(stacks):
        raise ContractError("INVALID_MANIFEST", "Duplicate stack prerequisite")
    return manifest

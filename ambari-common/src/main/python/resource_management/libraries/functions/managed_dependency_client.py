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

import json
import subprocess

from resource_management.core.signal_utils import TerminateStrategy
from resource_management.libraries.functions.managed_dependency import ManagedDependencyFailure


CLIENT_HELPER_JAR = "/var/lib/ambari-agent/tools/zkmigrator.jar"
CLIENT_HELPER_CLASS = "org.apache.ambari.tools.hadoop.ManagedDependencyClient"
ERROR_CODES = {
  "DEPENDENCY_AUTHORIZATION_FAILED",
  "DEPENDENCY_DATA_INTEGRITY_FAILED",
  "DEPENDENCY_DATANODE_READ_WRITE_FAILED",
  "DEPENDENCY_PROBE_CLEANUP_FAILED",
  "DEPENDENCY_CLIENT_OBSERVATION_FAILED",
}
RESULT_FIELDS = {
  "VERSION": {"kind": str, "version": str},
  "ACL": {"entries": list},
  "COUNT": {"empty": bool},
  "MKDIR": {"created": bool},
  "SET_OWNER": {"applied": bool},
  "SET_PERMISSION": {"applied": bool},
  "READ_MARKER": {"marker": dict},
  "WRITE_MARKER": {"written": bool},
  "PROBE": {"readWriteVerified": bool, "bytes": int, "cleaned": bool},
}


def observe_client(call, executable, config_dir, command, operation, payload,
                   user, environment, timeout=60, error_code="DEPENDENCY_CLIENT_OBSERVATION_FAILED"):
  identity = dict(command.envelope)
  arguments = (executable, "--config", config_dir, CLIENT_HELPER_CLASS,
               operation, config_dir, json.dumps(identity, sort_keys=True),
               json.dumps(payload, sort_keys=True))
  # Use the selected distribution's launcher so its SDK precedes the Agent helper.
  environment = {**(environment or {}), "HADOOP_CLASSPATH": CLIENT_HELPER_JAR,
                 "HBASE_CLASSPATH": CLIENT_HELPER_JAR}
  try:
    code, output = call(arguments, user=user, env=environment, stderr=subprocess.PIPE,
                        timeout=timeout, timeout_kill_strategy=TerminateStrategy.KILL_PROCESS_GROUP,
                        shell=False, quiet=True)[:2]
    if code != 0 or not isinstance(output, str) or len(output.encode("utf-8")) > 32 * 1024:
      raise ValueError("Invalid client process result")
    result = json.loads(output)
    if (not isinstance(result, dict) or type(result.get("schemaVersion")) is not int
        or result["schemaVersion"] != 1 or json.dumps(result.get("identity"), sort_keys=True) != json.dumps(identity, sort_keys=True)
        or result.get("operation") != operation):
      raise ValueError("Invalid client response identity or schema")
    if result.get("status") == "FAILED":
      failure_code = result.get("errorCode")
      if failure_code not in ERROR_CODES:
        raise ValueError("Invalid client error code")
      raise ManagedDependencyFailure(
        error_code if failure_code == "DEPENDENCY_CLIENT_OBSERVATION_FAILED" else failure_code,
        "The managed dependency client API could not complete the observation")
    if result.get("status") != "SUCCEEDED" or "errorCode" in result:
      raise ValueError("Invalid client response status")
    facts = result.get("result")
    fields = RESULT_FIELDS.get(operation)
    if operation == "STAT":
      if not isinstance(facts, dict) or type(facts.get("exists")) is not bool:
        raise ValueError("Invalid client file status")
      fields = {"exists": bool}
      if facts["exists"]:
        fields = {**fields, "type": str, "owner": str, "group": str, "mode": int}
    if fields is None or not isinstance(facts, dict) or set(facts) != set(fields):
      raise ValueError("Invalid client observation fields")
    if any(type(facts[key]) is not expected for key, expected in fields.items()):
      raise ValueError("Invalid client observation types")
    if operation == "STAT" and facts["exists"]:
      if facts["type"] not in {"DIRECTORY", "FILE", "SYMLINK"} or not 0 <= facts["mode"] <= 0o7777:
        raise ValueError("Invalid file type or permission bits")
    if operation == "ACL":
      for entry in facts["entries"]:
        if (not isinstance(entry, dict) or set(entry) != {"scope", "type", "name", "permission"}
            or entry["scope"] not in {"ACCESS", "DEFAULT"}
            or entry["type"] not in {"USER", "GROUP", "MASK", "OTHER"}
            or type(entry["name"]) is not str or type(entry["permission"]) is not int
            or not 0 <= entry["permission"] <= 7):
          raise ValueError("Invalid typed ACL entry")
    return facts
  except ManagedDependencyFailure:
    raise
  except Exception as error:
    raise ManagedDependencyFailure(error_code,
      "The managed dependency client returned no valid structured observation") from error

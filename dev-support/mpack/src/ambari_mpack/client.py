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

import base64
import hashlib
import json
import ssl
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid
from pathlib import Path

from .contract import ContractError, DIGEST, canonical, read_json

PHASES = {"ACCEPTED", "PREPARING", "WAITING_MAINTENANCE", "WAITING_RESTART",
          "PUBLISHING", "SUCCEEDED", "FAILED", "CANCELLING", "CANCELLED", "RECOVERY_REQUIRED"}


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, request, response, code, message, headers, new_url):
        return None


class Client:
    def __init__(self, server, username, password, timeout=60, ca_file=None):
        parsed = urllib.parse.urlsplit(server)
        if parsed.scheme not in {"http", "https"} or not parsed.netloc or parsed.username or parsed.password or parsed.query or parsed.fragment:
            raise ContractError("INVALID_SERVER", "Specify an HTTP server URL without credentials, query, or fragment")
        self.server = server.rstrip("/")
        if not self.server.endswith("/api/v1"):
            self.server += "/api/v1"
        self.timeout = timeout
        self.context = ssl.create_default_context(cafile=ca_file)
        self.opener = urllib.request.build_opener(NoRedirect(), urllib.request.HTTPSHandler(context=self.context))
        credential = base64.b64encode((username + ":" + password).encode("utf-8")).decode("ascii")
        self.headers = {"Authorization": "Basic " + credential, "X-Requested-By": "ambari-mpack",
                        "Accept": "application/json"}

    def request(self, method, path, value=None, headers=None, stream=None):
        request_headers = dict(self.headers)
        request_headers.update(headers or {})
        data = stream
        if value is not None:
            data = canonical(value).encode("utf-8")
            request_headers["Content-Type"] = "application/json"
        request = urllib.request.Request(self.server + "/" + path.lstrip("/"), data=data,
                                         headers=request_headers, method=method)
        try:
            with self.opener.open(request, timeout=self.timeout) as response:
                content = response.read(64 * 1024 * 1024 + 1)
                if len(content) > 64 * 1024 * 1024:
                    raise ContractError("INVALID_RESPONSE", "Server response exceeds the size limit")
                result = read_json(content)
        except urllib.error.HTTPError as error:
            content = error.read(1024 * 1024)
            try:
                envelope = read_json(content)
            except ContractError:
                envelope = {}
            if isinstance(envelope, dict) and envelope.get("schema_version") == 1 and isinstance(envelope.get("error"), dict):
                observation = envelope["error"]
                raise ContractError(str(observation.get("code", "HTTP_ERROR")),
                                    str(observation.get("message", "Request failed")),
                                    observation.get("details", {})) from error
            raise ContractError("HTTP_ERROR", "Server rejected the request", {"status": error.code}) from error
        except (urllib.error.URLError, TimeoutError, OSError) as error:
            raise ContractError("CONNECTION_UNRESOLVED", "The request outcome must be reconciled using its submission identity") from error
        if not isinstance(result, dict) or type(result.get("schema_version")) is not int or result["schema_version"] != 1:
            raise ContractError("INVALID_RESPONSE", "Server response has no supported schema")
        return result

    def capabilities(self):
        result = self.request("GET", "mpack_capabilities")
        if not isinstance(result.get("manifest_schema_versions"), list) or 1 not in result["manifest_schema_versions"] or result.get("binding_scope") != "STACK_VERSION":
            raise ContractError("UNSUPPORTED_SCHEMA", "Server does not advertise the required mpack contract")
        return result

    def upload(self, path):
        path = Path(path)
        digest = hashlib.sha256()
        with path.open("rb") as source:
            for chunk in iter(lambda: source.read(1024 * 1024), b""):
                digest.update(chunk)
            source.seek(0)
            result = self.request("POST", "mpack_uploads",
                                  headers={"Content-Type": "application/octet-stream",
                                           "Content-Length": str(path.stat().st_size),
                                           "X-Content-SHA256": digest.hexdigest()}, stream=source)
        if result.get("archive_digest") != digest.hexdigest():
            raise ContractError("INVALID_RESPONSE", "Upload response identifies different archive content")
        return result

    def plan(self, mutation):
        result = self.request("POST", "mpack_plans", mutation)
        if not self._uuid(result.get("id")) or not isinstance(result.get("digest"), str) or not DIGEST.fullmatch(result["digest"]):
            raise ContractError("INVALID_RESPONSE", "Plan response has no exact identity")
        if result.get("mutation") != mutation:
            raise ContractError("INVALID_RESPONSE", "Server preview does not match the requested mutation")
        return result

    def submit(self, plan, key):
        result = self.request("POST", "mpack_operations",
                              {"schema_version": 1, "plan_id": plan["id"]},
                              headers={"Idempotency-Key": key})
        self.validate_operation(result)
        if result.get("plan_id") != plan["id"] or result.get("plan_digest") != plan["digest"]:
            raise ContractError("INVALID_RESPONSE", "Operation belongs to another plan")
        return result

    def service_plan(self, service_ids, cluster_id=None, maintenance=False):
        result = self.request("POST", "mpack_service_plans", {"schema_version": 1,
            "service_ids": service_ids, "cluster_id": cluster_id, "maintenance": maintenance})
        deployment = result.get("deployment")
        if (not self._uuid(result.get("id")) or not isinstance(result.get("digest"), str)
            or not DIGEST.fullmatch(result["digest"]) or not isinstance(deployment, dict)
            or deployment.get("cluster_id") != cluster_id
            or type(deployment.get("cluster_id")) is not type(cluster_id)
            or not isinstance(deployment.get("service_ids"), list)
            or any(not isinstance(value, str) for value in deployment["service_ids"])
            or sorted(deployment["service_ids"]) != sorted(service_ids)
            or not isinstance(deployment.get("service_names"), list)
            or len(deployment["service_names"]) != len(service_ids)):
            raise ContractError("INVALID_RESPONSE", "Service plan belongs to another selection or destination")
        return result

    def operation(self, operation_id):
        if not self._uuid(operation_id):
            raise ContractError("INVALID_OPERATION", "A canonical operation UUID is required")
        result = self.request("GET", "mpack_operations/" + operation_id)
        self.validate_operation(result)
        if result["id"] != operation_id:
            raise ContractError("INVALID_RESPONSE", "Server returned a different operation")
        return result

    def wait(self, operation, interval=2):
        self.validate_operation(operation)
        while operation["phase"] in {"ACCEPTED", "PREPARING", "WAITING_MAINTENANCE", "PUBLISHING", "CANCELLING"}:
            time.sleep(interval)
            operation = self.operation(operation["id"])
        return operation

    @staticmethod
    def _uuid(value):
        try:
            return isinstance(value, str) and str(uuid.UUID(value)) == value
        except (ValueError, AttributeError):
            return False

    @classmethod
    def validate_operation(cls, operation):
        if not isinstance(operation, dict) or type(operation.get("schema_version")) is not int or operation["schema_version"] != 1 or not cls._uuid(operation.get("id")):
            raise ContractError("INVALID_RESPONSE", "Operation identity is missing or invalid")
        if operation.get("phase") not in PHASES or type(operation.get("generation")) is not int or operation["generation"] < 0:
            raise ContractError("INVALID_RESPONSE", "Operation phase or generation is invalid")
        if not cls._uuid(operation.get("plan_id")) or not isinstance(operation.get("plan_digest"), str) or not DIGEST.fullmatch(operation["plan_digest"]):
            raise ContractError("INVALID_RESPONSE", "Operation plan lineage is invalid")
        hooks = operation.get("hooks")
        if not isinstance(hooks, dict):
            raise ContractError("INVALID_RESPONSE", "Operation hook observations are missing")
        history = operation.get("hook_history")
        if not isinstance(history, list):
            raise ContractError("INVALID_RESPONSE", "Operation hook history is missing")
        for receipt in list(hooks.values()) + history:
            if not isinstance(receipt, dict) or receipt.get("schema_version") != 1 or receipt.get("operation_id") != operation["id"] or receipt.get("plan_digest") != operation["plan_digest"]:
                raise ContractError("INVALID_RESPONSE", "Hook receipt belongs to another operation")
            if type(receipt.get("attempt")) is not int or receipt["attempt"] < 1 or receipt.get("effect_state") not in {"APPLIED", "NOT_APPLIED", "UNKNOWN"}:
                raise ContractError("INVALID_RESPONSE", "Hook attempt or effect observation is invalid")
            if not isinstance(receipt.get("observations"), dict):
                raise ContractError("INVALID_RESPONSE", "Hook observations must be a structured object")
            if (not isinstance(receipt.get("archive_digest"), str) or not DIGEST.fullmatch(receipt["archive_digest"])
                or receipt.get("phase") not in {"before-install", "after-install", "before-upgrade",
                                                "after-upgrade", "before-uninstall", "after-uninstall"}
                or receipt.get("state") not in {"RUNNING", "APPLIED", "FAILED", "UNKNOWN"}):
                raise ContractError("INVALID_RESPONSE", "Hook resource identity, phase, or state is invalid")
        for key, receipt in hooks.items():
            if key != receipt["archive_digest"] + "/" + receipt["phase"]:
                raise ContractError("INVALID_RESPONSE", "Hook result does not match its resource identity")
        attempts = set()
        for receipt in history:
            key = receipt["archive_digest"] + "/" + receipt["phase"]
            identity = (key, receipt["attempt"])
            if identity in attempts or key in hooks and hooks[key]["attempt"] <= receipt["attempt"]:
                raise ContractError("INVALID_RESPONSE", "Hook attempt history is duplicated or foreign")
            attempts.add(identity)
        if operation["phase"] == "SUCCEEDED":
            effective = operation.get("effective_snapshot")
            if not isinstance(effective, str) or not DIGEST.fullmatch(effective) or operation.get("error_code") is not None:
                raise ContractError("INVALID_RESPONSE", "Successful operation lacks verified activation")
            if any(receipt.get("state") != "APPLIED" or receipt.get("effect_state") == "UNKNOWN" or not receipt.get("observations") for receipt in hooks.values()):
                raise ContractError("INVALID_RESPONSE", "Successful operation has an unresolved hook")
        return operation

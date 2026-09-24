"""
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements. See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License. You may obtain a copy of the License at
http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
"""

"""Live, checkpointed mpack acceptance against a dedicated disposable Server."""

import argparse
import hashlib
import json
import os
import tempfile
import time
import urllib.error
import urllib.request
import uuid
from pathlib import Path

from ambari_mpack.client import Client
from ambari_mpack.contract import ContractError, read_json


def require(condition, message):
    if not condition:
        raise RuntimeError(message)


def save(path, value):
    path.parent.mkdir(parents=True, exist_ok=True, mode=0o700)
    descriptor, temporary = tempfile.mkstemp(dir=path.parent, prefix=".evidence-")
    try:
        with os.fdopen(descriptor, "w", encoding="utf-8") as stream:
            json.dump(value, stream, sort_keys=True, indent=2, allow_nan=False)
            stream.write("\n")
            stream.flush()
            os.fsync(stream.fileno())
        os.replace(temporary, path)
    finally:
        if os.path.exists(temporary):
            os.unlink(temporary)


def ordinary(client, path, method="GET", value=None):
    headers = dict(client.headers, Accept="*/*")
    data = None
    if value is not None:
        data = json.dumps(value).encode("utf-8")
        headers["Content-Type"] = "application/json"
    with client.opener.open(urllib.request.Request(client.server + "/" + path,
                            headers=headers, data=data, method=method), timeout=60) as response:
        body = response.read(4 * 1024 * 1024 + 1)
        require(len(body) <= 4 * 1024 * 1024, "Unbounded ordinary API response")
        return read_json(body) if body else {}


def complete(client, directory, label, plan):
    receipt = directory / (label + "-submission.json")
    if receipt.exists():
        checkpoint = read_json(receipt.read_bytes())
        require(checkpoint["server"] == client.server and checkpoint["plan"] == plan,
                "Saved submission belongs to another Server or plan")
    else:
        checkpoint = {"schema_version": 1, "server": client.server, "plan": plan,
                      "idempotency_key": str(uuid.uuid4())}
        save(receipt, checkpoint)
    accepted = client.submit(plan, checkpoint["idempotency_key"])
    save(directory / (label + "-accepted.json"), accepted)
    deadline = time.monotonic() + 180
    current = accepted
    while current["phase"] in {"ACCEPTED", "PREPARING", "PUBLISHING", "CANCELLING"}:
        require(time.monotonic() < deadline, "Accepted operation is still unresolved; retain its checkpoint")
        time.sleep(1)
        current = client.operation(accepted["id"])
        for key in ("id", "plan_id", "plan_digest", "generation"):
            require(current[key] == accepted[key], "Operation lineage changed")
        save(directory / (label + "-operation.json"), current)
    require(current["phase"] == "SUCCEEDED", "Operation did not succeed: " + current["phase"])
    repeated = client.submit(plan, checkpoint["idempotency_key"])
    require(repeated["id"] == current["id"] and repeated["generation"] == current["generation"],
            "Idempotent acceptance created another operation")
    save(directory / (label + "-replayed.json"), repeated)
    return current


def run(args):
    client = Client(args.server, "admin", os.environ["AMBARI_DEPLOY_PASSWORD"])
    directory = Path(args.evidence)
    capabilities = client.capabilities()
    save(directory / "capabilities.json", capabilities)
    require(capabilities["required_agent_protocol"] == "MPACK_RESOURCES_V1", "Wrong Agent contract")
    if args.step == "preflight":
        clusters = ordinary(client, "clusters")
        hosts = ordinary(client, "hosts?fields=Hosts/host_name,Hosts/host_status,Hosts/last_heartbeat_time")
        require(clusters["items"] == [], "Pre-cluster acceptance requires no existing clusters")
        require(len(hosts["items"]) == 4, "Expected four registered acceptance hosts")
        require(all(item["Hosts"]["host_status"] == "HEALTHY" for item in hosts["items"]),
                "Some registered hosts are not healthy")
        save(directory / "hosts.json", hosts)
        save(directory / "clusters-before.json", clusters)
        schema = client.request("GET", "mpack_capabilities/manifest_schema")
        require(isinstance(schema.get("manifest_schema"), dict), "Published schema is missing")
        save(directory / "schema.json", schema)
        save(directory / "preflight.json", {"schema_version": 1, "outcome": "PASS",
                                          "host_count": len(hosts["items"])})
    elif args.step == "retire-inactive":
        require(bool(args.release_ids), "Explicit release IDs are required")
        plan_file = directory / "retire-plan.json"
        if plan_file.exists():
            plan = read_json(plan_file.read_bytes())
            require(plan["mutation"]["release_ids"] == args.release_ids, "Retirement selection changed")
        else:
            plan = client.plan({"schema_version": 1, "action": "UNINSTALL", "archive_digests": [],
                "release_ids": args.release_ids, "bindings": [], "activate": True, "maintenance": False})
            save(plan_file, plan)
        operation = complete(client, directory, "retire", plan)
        require(operation["hooks"] == {}, "Inactive package retirement executed hooks")
        releases = client.request("GET", "mpacks")
        for release_id in args.release_ids:
            matches = [item for item in releases["items"] if item["id"] == release_id]
            require(len(matches) == 1 and matches[0]["installed"] is False, "Retirement was not observed")
        save(directory / "retired-releases.json", releases)
    elif args.step == "import":
        archive = Path(args.archive)
        digest = hashlib.sha256(archive.read_bytes()).hexdigest()
        before = {path: client.request("GET", path) for path in ("mpacks", "mpack_bindings", "mpack_operations")}
        plan_file = directory / "import-plan.json"
        if plan_file.exists():
            plan = read_json(plan_file.read_bytes())
            upload = read_json((directory / "upload.json").read_bytes())
            require(upload["archive_digest"] == digest, "The pending import uses another archive")
        else:
            require(not any(item["installed"] for item in before["mpacks"]["items"]),
                    "Import fixture requires no installed package releases")
            upload = client.upload(archive)
            require(upload["bundle"] is True and len(upload["members"]) == 4, "Wrong store bundle")
            names = {member["name"] for member in upload["members"]}
            require(names == {"generic-base", "nginx", "postgresql", "kyuubi"}, "Wrong store members")
            save(directory / "upload.json", upload)
            plan = client.plan({"schema_version": 1, "action": "IMPORT",
                "archive_digests": [member["archive_digest"] for member in upload["members"]],
                "release_ids": [], "bindings": [], "activate": False, "maintenance": False})
            after = {path: client.request("GET", path) for path in before}
            require(after == before, "Upload/preview changed effective catalog state")
            save(directory / "preview-before.json", before)
            save(directory / "preview-after.json", after)
            save(plan_file, plan)
        operation = complete(client, directory, "import", plan)
        require(operation["hooks"] == {} and operation["hook_history"] == [], "Import executed hooks")
        bindings = client.request("GET", "mpack_bindings")
        baseline = read_json((directory / "preview-before.json").read_bytes())
        require(bindings["items"] == baseline["mpack_bindings"]["items"], "Import changed bindings")
        require(operation["effective_snapshot"] == plan["previous_snapshot"] == plan["candidate_snapshot"],
                "Import changed the effective definition snapshot")
        require(client.capabilities()["target_stack_versions"] == capabilities["target_stack_versions"],
                "Import activated a new stack")
        releases = client.request("GET", "mpacks")
        expected = {member["name"] + "/" + member["version"] for member in upload["members"]}
        require({item["id"] for item in releases["items"] if item["installed"]} == expected,
                "Imported release inventory differs")
        save(directory / "releases-after-import.json", releases)
        services = client.request("GET", "mpack_services")
        save(directory / "services-after-import.json", services)
        require({item["service_name"] for item in services["items"]} >= {"NGINX", "POSTGRESQL", "KYUUBI"},
                "Imported services are not discoverable")
    elif args.step == "enable-nginx":
        services = client.request("GET", "mpack_services")
        selected = [item for item in services["items"] if item["service_name"] == "NGINX"
                    and item["stack_name"] == "GENERIC" and item["stack_version"] == "1.0"]
        require(len(selected) == 1, "Nginx provider is missing or ambiguous")
        plan_file = directory / "nginx-plan.json"
        if plan_file.exists():
            plan = read_json(plan_file.read_bytes())
        else:
            plan = client.service_plan([selected[0]["id"]])
            save(plan_file, plan)
        operation = complete(client, directory, "nginx", plan)
        deployment = client.request("GET", "mpack_operations/" + operation["id"] + "/deployment")
        require(deployment["operation_id"] == operation["id"] and deployment["plan_id"] == plan["id"],
                "Deployment handoff belongs to another selection")
        require(deployment["deployment"]["service_names"] == ["NGINX"], "Unselected service leaked into deployment")
        save(directory / "nginx-deployment.json", deployment)
    elif args.step == "deploy-nginx":
        name = "mpack_nginx_20260924"
        blueprint = "mpack_nginx_20260924"
        receipt_file = directory / "nginx-install-request.json"
        if receipt_file.exists():
            receipt = read_json(receipt_file.read_bytes())
        else:
            clusters = ordinary(client, "clusters")
            require(all(item["Clusters"]["cluster_name"] != name for item in clusters["items"]),
                    "Existing cluster has no saved installation receipt; reconcile explicitly")
            definitions = {"Blueprints": {"stack_name": "GENERIC", "stack_version": "1.0"},
                "host_groups": [{"name": "nginx", "components": [{"name": "NGINX_SERVER"}], "cardinality": "1"}],
                "configurations": [{"nginx-env": {"listen_port": "8088"}}]}
            existing = ordinary(client, "blueprints")
            if any(item["Blueprints"]["blueprint_name"] == blueprint for item in existing["items"]):
                recorded = read_json((directory / "nginx-blueprint.json").read_bytes())
                require(recorded == definitions, "Existing blueprint has different recorded inputs")
            else:
                ordinary(client, "blueprints/" + blueprint, "POST", definitions)
                save(directory / "nginx-blueprint.json", definitions)
            template = {"blueprint": blueprint, "host_groups": [{"name": "nginx",
                "hosts": [{"fqdn": "worker2.bigtop.apache.org"}]}], "config_recommendation_strategy": "NEVER_APPLY"}
            attempt_file = directory / "nginx-install-submission.json"
            require(not attempt_file.exists(), "Installation submission has no receipt; reconcile before retry")
            save(attempt_file, {"schema_version": 1, "cluster": name, "template": template})
            result = ordinary(client, "clusters/" + name, "POST", template)
            require(type(result.get("Requests", {}).get("id")) is int, "Installation has no exact request ID")
            receipt = {"schema_version": 1, "cluster": name, "request_id": result["Requests"]["id"], "response": result}
            save(receipt_file, receipt)
        require(receipt["cluster"] == name, "Installation receipt belongs to another cluster")
        request_id = receipt["request_id"]
        deadline = time.monotonic() + 600
        while True:
            result = ordinary(client, "clusters/" + name + "/requests/" + str(request_id))
            require(result["Requests"]["id"] == request_id, "Foreign install request observation")
            save(directory / "nginx-install-status.json", result)
            status = result["Requests"]["request_status"]
            if status in {"COMPLETED", "FAILED", "ABORTED", "TIMEDOUT", "HOLDING", "HOLDING_FAILED", "HOLDING_TIMEDOUT"}:
                break
            require(status in {"PENDING", "IN_PROGRESS", "QUEUED"}, "Unknown request state")
            require(time.monotonic() < deadline, "Install request is still unresolved")
            time.sleep(3)
        tasks = ordinary(client, "clusters/" + name + "/requests/" + str(request_id)
                         + "/tasks?fields=Tasks/id,Tasks/status,Tasks/role,Tasks/host_name,Tasks/structured_out")
        save(directory / "nginx-install-tasks.json", tasks)
        require(status == "COMPLETED", "Installation request did not complete: " + status)
        services = ordinary(client, "clusters/" + name + "/services")
        require({item["ServiceInfo"]["service_name"] for item in services["items"]} == {"NGINX"},
                "Unselected services were installed")
        save(directory / "nginx-installed-services.json", services)
    elif args.step == "deploy-kyuubi":
        require(bool(args.rendered), "Deploy-generated artifacts are required")
        name = "mpack_kyuubi"
        receipt_file = directory / "kyuubi-install-request.json"
        require(not receipt_file.exists(), "Use the saved request to observe or recover this installation")
        require(all(item["Clusters"]["cluster_name"] != name for item in ordinary(client, "clusters")["items"]),
                "Existing Kyuubi cluster requires explicit reconciliation")
        rendered = Path(args.rendered) / "blueprint"
        blueprint = read_json((rendered / "blueprint.json").read_bytes())
        template = read_json((rendered / "cluster_template.json").read_bytes())
        template["default_password"] = os.environ["AMBARI_DEPLOY_PASSWORD"]
        template["blueprint"] = "mpack_kyuubi_20260924"
        template["config_recommendation_strategy"] = "NEVER_APPLY"
        blueprint["configurations"].extend([
            {"kyuubi-env": {"server_host": "worker4.bigtop.apache.org"}},
            {"kyuubi-defaults": {"spark.master": "local[2]", "spark.driver.memory": "1g",
                                  "spark.executor.memory": "1g", "spark.sql.shuffle.partitions": "2"}},
        ])
        for item in blueprint["configurations"]:
            if "yarn-site" in item:
                values = item["yarn-site"]
                if "properties" in values:
                    values = values["properties"]
                values.update({"yarn.nodemanager.resource.memory-mb": "1536",
                               "yarn.scheduler.minimum-allocation-mb": "256",
                               "yarn.scheduler.maximum-allocation-mb": "1536"})
        save(directory / "kyuubi-blueprint.json", blueprint)
        ordinary(client, "blueprints/" + template["blueprint"], "POST", blueprint)
        save(directory / "kyuubi-install-submission.json", {"schema_version": 1, "cluster": name,
            "template": {key: value for key, value in template.items() if key != "default_password"},
            "default_password_env": "AMBARI_DEPLOY_PASSWORD"})
        response = ordinary(client, "clusters/" + name, "POST", template)
        require(type(response.get("Requests", {}).get("id")) is int, "Kyuubi installation has no exact request ID")
        save(receipt_file, {"schema_version": 1, "cluster": name, "request_id": response["Requests"]["id"], "response": response})
        print(json.dumps({"schema_version": 1, "step": args.step, "outcome": "ACCEPTED", "request_id": response["Requests"]["id"]}))
        return
    print(json.dumps({"schema_version": 1, "step": args.step, "outcome": "PASS", "evidence": str(directory)}))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--server", required=True)
    parser.add_argument("--evidence", required=True)
    parser.add_argument("--archive")
    parser.add_argument("--rendered")
    parser.add_argument("--release-ids", nargs="+")
    parser.add_argument("--step", choices=("preflight", "import", "enable-nginx", "deploy-nginx", "deploy-kyuubi", "retire-inactive"), required=True)
    arguments = parser.parse_args()
    try:
        run(arguments)
    except (ContractError, RuntimeError, KeyError, ValueError, urllib.error.URLError) as error:
        print(json.dumps({"schema_version": 1, "step": arguments.step, "outcome": "FAIL",
                          "error_type": type(error).__name__, "message": str(error)}))
        raise SystemExit(1)

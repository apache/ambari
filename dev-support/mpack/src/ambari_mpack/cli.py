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

import argparse
import getpass
import json
import os
import sys
import tempfile
import urllib.parse
import uuid
from importlib.resources import files
from pathlib import Path

from . import __doc__ as license_header
from .builder import build, bundle, selection, validate
from .client import Client
from .contract import ContractError, canonical, compare_versions, read_json


def parser():
    root = argparse.ArgumentParser(prog="ambari-mpack")
    root.add_argument("--server", default=os.environ.get("AMBARI_SERVER_URL", "http://localhost:8080"))
    root.add_argument("--username", default=os.environ.get("AMBARI_USERNAME", "admin"))
    root.add_argument("--ca-file")
    root.add_argument("--json", action="store_true", dest="json_output")
    commands = root.add_subparsers(dest="command", required=True)
    scaffold = commands.add_parser("scaffold")
    scaffold.add_argument("name")
    scaffold.add_argument("--directory")
    validation = commands.add_parser("validate")
    validation.add_argument("directory")
    reference = commands.add_parser("export-hdfs-reference")
    reference.add_argument("--repository", required=True)
    reference.add_argument("--commit", required=True)
    reference.add_argument("--output", required=True)
    building = commands.add_parser("build")
    building.add_argument("directory", nargs="?")
    building.add_argument("--all", action="store_true")
    building.add_argument("--packs")
    building.add_argument("--profile")
    building.add_argument("--repository", default=".")
    building.add_argument("--output", default="dist")
    building.add_argument("--bundle")
    commands.add_parser("list")
    commands.add_parser("services")
    show = commands.add_parser("show")
    show.add_argument("release")
    for name in ("import", "install", "update", "uninstall", "bind", "unbind"):
        operation = commands.add_parser(name)
        operation.add_argument("target")
        operation.add_argument("--file")
        operation.add_argument("--stack")
        operation.add_argument("--store-only", action="store_true")
        operation.add_argument("--maintenance", action="store_true")
        operation.add_argument("--dry-run", action="store_true")
        operation.add_argument("--no-wait", action="store_true")
    enable = commands.add_parser("enable")
    enable.add_argument("service_ids", help="Comma-separated exact catalog service IDs")
    enable.add_argument("--cluster-id", type=int)
    enable.add_argument("--maintenance", action="store_true")
    enable.add_argument("--dry-run", action="store_true")
    enable.add_argument("--no-wait", action="store_true")
    operations = commands.add_parser("operations")
    operations.add_argument("action", choices=("list", "show", "members", "recover", "retry", "cancel", "resume"))
    operations.add_argument("identity", nargs="?")
    return root


def _emit(value, machine):
    if machine:
        print(canonical(value))
    else:
        print(json.dumps(value, indent=2, ensure_ascii=False))


def _submission_directory():
    base = Path(os.environ.get("XDG_STATE_HOME", str(Path.home() / ".local" / "state")))
    directory = base / "ambari-mpack" / "submissions"
    directory.mkdir(mode=0o700, parents=True, exist_ok=True)
    return directory


def _save_submission(value, path=None):
    directory = _submission_directory()
    path = Path(path) if path else directory / (value["submission_id"] + ".json")
    descriptor, temporary = tempfile.mkstemp(prefix=".submission-", dir=directory)
    try:
        with os.fdopen(descriptor, "w", encoding="utf-8") as output:
            output.write(canonical(value))
            output.flush()
            os.fsync(output.fileno())
        os.replace(temporary, path)
    finally:
        if os.path.exists(temporary):
            os.unlink(temporary)
    return path


def _select_release(client, identity):
    inventory = client.request("GET", "mpacks")
    if not isinstance(inventory.get("items"), list):
        raise ContractError("INVALID_RESPONSE", "Inventory has no release list")
    candidates = [release for release in inventory["items"]
                  if release.get("installed") is True and
                  (release.get("id") == identity or release.get("id", "").split("/")[0] == identity)]
    if len(candidates) != 1:
        raise ContractError("RELEASE_SELECTION_REQUIRED", "Specify an unambiguous package/version identity",
                            {"candidates": [release.get("id") for release in candidates]})
    return candidates[0]


def _binding_targets(upload, capabilities, stack):
    result = []
    for extension in upload.get("extensions", []):
        eligible = []
        for target in capabilities.get("target_stack_versions", []):
            if any(target["stack_name"] == minimum["stack_name"] and
                   compare_versions(target["stack_version"], minimum["stack_version"]) >= 0
                   for minimum in extension["minimum_stacks"]):
                if stack is None or stack == target["stack_name"] + "/" + target["stack_version"]:
                    eligible.append(target)
        if len(eligible) != 1:
            raise ContractError("BINDING_SELECTION_REQUIRED",
                                "Specify one compatible --stack NAME/VERSION, or use --store-only",
                                {"extension": extension["name"], "targets": eligible})
        result.append({"stack_name": eligible[0]["stack_name"], "stack_version": eligible[0]["stack_version"],
                       "extension_name": extension["name"], "extension_version": extension["version"]})
    return result


def _run_mutation(client, args):
    capabilities = client.capabilities()
    mutation = {"schema_version": 1, "action": args.command.upper(), "archive_digests": [],
                "release_ids": [], "bindings": [], "activate": not args.store_only,
                "maintenance": args.maintenance}
    if args.command == "import" or args.command == "install" and args.store_only:
        mutation["action"] = "IMPORT"
        mutation["activate"] = False
    if args.command in {"import", "install", "update"}:
        if args.command == "update":
            if not args.file:
                raise ContractError("INVALID_ARGUMENT", "Update requires --file ARCHIVE")
            mutation["release_ids"] = [_select_release(client, args.target)["id"]]
        uploaded = client.upload(args.target if args.command in {"import", "install"} else args.file)
        members = uploaded.get("members", [uploaded])
        mutation["archive_digests"] = [member["archive_digest"] for member in members]
        if mutation["activate"]:
            targets = {(target["stack_name"], target["stack_version"]): target
                       for target in capabilities.get("target_stack_versions", [])}
            for member in members:
                for target in member.get("stacks", []):
                    targets[(target["stack_name"], target["stack_version"])] = target
            capabilities["target_stack_versions"] = list(targets.values())
            mutation["bindings"] = [binding for member in members
                                    for binding in _binding_targets(member, capabilities, args.stack)]
    else:
        release = _select_release(client, args.target)
        mutation["release_ids"] = [release["id"]]
        if args.command in {"bind", "unbind"}:
            if args.store_only:
                raise ContractError("INVALID_ARGUMENT", "Binding operations require activation")
            provided = set()
            for contribution in release["contributions"]:
                parts = contribution["scope"].split("/")
                if len(parts) == 3 and parts[0] == "extensions":
                    provided.add((parts[1], parts[2]))
            if args.command == "unbind":
                current = client.request("GET", "mpack_bindings")
                mutation["bindings"] = [binding for binding in current["items"]
                                       if (binding["extension_name"], binding["extension_version"]) in provided
                                       and (args.stack is None or args.stack == binding["stack_name"] + "/" + binding["stack_version"])]
            else:
                if not args.stack or len(args.stack.split("/")) != 2:
                    raise ContractError("INVALID_ARGUMENT", "Bind requires --stack NAME/VERSION")
                name, version = args.stack.split("/")
                mutation["bindings"] = [{"stack_name": name, "stack_version": version,
                                         "extension_name": extension, "extension_version": extension_version}
                                        for extension, extension_version in sorted(provided)]
            if not mutation["bindings"]:
                raise ContractError("INVALID_ARGUMENT", "No binding matches the selected release and target")
    plan = client.plan(mutation)
    return _submit_plan(client, args, plan)


def _submit_plan(client, args, plan):
    if args.dry_run:
        return plan
    submission = {"schema_version": 1, "submission_id": str(uuid.uuid4()), "server": client.server,
                  "plan_id": plan["id"], "plan_digest": plan["digest"], "idempotency_key": str(uuid.uuid4()),
                  "operation_id": None}
    path = _save_submission(submission)
    try:
        operation = client.submit(plan, submission["idempotency_key"])
    except ContractError as error:
        error.details = dict(error.details, submission_id=submission["submission_id"])
        raise
    submission["operation_id"] = operation["id"]
    _save_submission(submission, path)
    return operation if args.no_wait else client.wait(operation)


def _operations(client, args):
    if args.action == "list":
        return client.request("GET", "mpack_operations")
    if not args.identity:
        raise ContractError("INVALID_ARGUMENT", "An operation or submission identity is required")
    if args.action == "show":
        return client.operation(args.identity)
    if args.action in {"recover", "retry", "cancel", "members"}:
        if not Client._uuid(args.identity):
            raise ContractError("INVALID_ARGUMENT", "A canonical operation UUID is required")
        result = client.request("GET" if args.action == "members" else "POST",
                                "mpack_operations/" + args.identity + "/" + args.action)
        if args.action == "members":
            return result
        return Client.validate_operation(result)
    if not Client._uuid(args.identity):
        raise ContractError("INVALID_ARGUMENT", "A canonical submission UUID is required")
    submission = read_json((_submission_directory() / (args.identity + ".json")).read_bytes())
    if submission.get("schema_version") != 1 or submission.get("submission_id") != args.identity or submission.get("server") != client.server:
        raise ContractError("INVALID_SUBMISSION", "Submission identity or server does not match")
    if submission.get("operation_id"):
        return client.wait(client.operation(submission["operation_id"]))
    plan = {"id": submission["plan_id"], "digest": submission["plan_digest"]}
    operation = client.submit(plan, submission["idempotency_key"])
    submission["operation_id"] = operation["id"]
    _save_submission(submission)
    return client.wait(operation)


def _scaffold(args):
    import re
    if not re.fullmatch(r"[a-z][a-z0-9_-]{0,63}", args.name):
        raise ContractError("INVALID_NAME", "Use a lowercase package identifier")
    root = Path(args.directory or ("mpacks/" + args.name))
    root.mkdir(parents=True, exist_ok=False)
    service = args.name.upper().replace("-", "_")
    manifest = {"$comment": license_header.strip(), "schema_version": 1, "type": "full-release",
                "name": args.name, "version": "1.0.0.0",
                "artifacts": [{"name": args.name + "-extension", "type": "extension-definitions",
                               "source_dir": "extensions"}],
                "dependencies": [{"name": "generic-base", "version": "1.0.0.0"}]}
    (root / "mpack.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    extension = root / "extensions" / service / "1.0"
    scripts = extension / "services" / service / "package" / "scripts"
    scripts.mkdir(parents=True)
    comment = "<!--\n" + license_header.strip() + "\n-->\n"
    (extension / "metainfo.xml").write_text(
        comment + '<metainfo><versions><active>true</active></versions><prerequisites>'
        '<min-stack-versions><stack><name>GENERIC</name><version>1.0</version></stack>'
        '</min-stack-versions></prerequisites></metainfo>\n', encoding="utf-8")
    (extension / "services" / service / "metainfo.xml").write_text(
        comment + '<metainfo><schemaVersion>2.0</schemaVersion><services><service><name>' + service +
        '</name><displayName>' + args.name + '</displayName><version>1.0</version><components><component>'
        '<name>' + service + '_SERVER</name><displayName>' + args.name + '</displayName><category>MASTER</category>'
        '<cardinality>1</cardinality><versionAdvertised>false</versionAdvertised><commandScript>'
        '<script>scripts/service.py</script><scriptType>PYTHON</scriptType><timeout>600</timeout>'
        '</commandScript></component></components></service></services></metainfo>\n', encoding="utf-8")
    (scripts / "service.py").write_text(
        '"""\n' + license_header.strip() + '\n"""\n\n'
        'from resource_management.libraries.script.script import Script\n'
        'from resource_management.core.exceptions import Fail\n\n\n'
        'class Service(Script):\n'
        '    def install(self, env):\n'
        '        raise Fail("Implement software installation and structured verification before deployment")\n\n'
        '    def configure(self, env):\n'
        '        raise Fail("Implement software configuration before deployment")\n\n\n'
        'if __name__ == "__main__":\n'
        '    Service().execute()\n', encoding="utf-8")
    for name in ("LICENSE", "NOTICE"):
        (root / name).write_text(files("ambari_mpack").joinpath("templates", name).read_text(encoding="utf-8"),
                                 encoding="utf-8")
    return {"schema_version": 1, "path": str(root), "requires_service_implementation": True}


def main(argv=None):
    args = parser().parse_args(argv)
    try:
        if args.command == "scaffold":
            result = _scaffold(args)
        elif args.command == "export-hdfs-reference":
            from .reference import export_hdfs
            result = export_hdfs(args.repository, args.commit, args.output)
        elif args.command == "validate":
            manifest, selected = validate(args.directory)
            result = {"schema_version": 1, "validation": "STATIC", "name": manifest["name"],
                      "version": manifest["version"], "files": len(selected)}
        elif args.command == "build":
            if args.directory:
                if args.all or args.packs or args.profile:
                    raise ContractError("INVALID_ARGUMENT", "Select a directory or a release selection, not both")
                paths = [Path(args.directory)]
            else:
                if not (args.all or args.packs or args.profile):
                    raise ContractError("INVALID_ARGUMENT", "Specify a directory, --all, --packs, or --profile")
                paths = selection(args.repository, args.packs.split(",") if args.packs else None, args.profile)
            archives = [build(path, args.output) for path in paths]
            result = {"schema_version": 1, "archives": archives}
            if args.bundle:
                result["bundle"] = bundle(archives, args.output, args.bundle)
        else:
            password = os.environ.get("AMBARI_PASSWORD")
            if password is None:
                if not sys.stdin.isatty():
                    raise ContractError("AUTHENTICATION_REQUIRED", "Set AMBARI_PASSWORD for non-interactive requests")
                password = getpass.getpass("Ambari password: ")
            client = Client(args.server, args.username, password, ca_file=args.ca_file)
            if args.command == "list":
                result = client.request("GET", "mpacks")
            elif args.command == "services":
                result = client.request("GET", "mpack_services")
            elif args.command == "enable":
                plan = client.service_plan(args.service_ids.split(","), args.cluster_id, args.maintenance)
                result = _submit_plan(client, args, plan)
            elif args.command == "show":
                result = dict(_select_release(client, args.release), schema_version=1)
            elif args.command == "operations":
                result = _operations(client, args)
            else:
                result = _run_mutation(client, args)
        _emit(result, args.json_output)
        if result.get("phase") in {"FAILED", "CANCELLED", "RECOVERY_REQUIRED"}:
            return 1
        if result.get("phase") == "WAITING_RESTART":
            return 3
        return 0
    except ContractError as error:
        _emit({"schema_version": 1, "error": {"code": error.code, "message": str(error),
                                              "details": error.details}}, args.json_output)
        return 2
    except KeyboardInterrupt:
        _emit({"schema_version": 1, "error": {"code": "WAIT_INTERRUPTED",
              "message": "Only local waiting stopped; the server operation continues."}}, args.json_output)
        return 130
    except (OSError, KeyError, TypeError) as error:
        _emit({"schema_version": 1, "error": {"code": "LOCAL_OR_CONTRACT_ERROR",
              "message": "Local files or contract data could not be processed",
              "details": {"exception_type": type(error).__name__}}}, args.json_output)
        return 2

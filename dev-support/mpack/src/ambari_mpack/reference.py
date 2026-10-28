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

import io
import re
import subprocess
import tarfile
from pathlib import Path
from xml.etree import ElementTree

from .builder import validate
from .contract import ContractError, canonical
from . import __doc__ as license_header


def export_hdfs(repository, commit, output):
    if not re.fullmatch(r"[0-9a-f]{40}", commit):
        raise ContractError("INVALID_SOURCE", "A complete fixed Git commit identity is required")
    repository = Path(repository).resolve()
    output = Path(output)
    if output.exists():
        raise ContractError("OUTPUT_EXISTS", "Reference export requires a new output directory")
    mappings = {
        "ambari-server/src/main/resources/stacks/BIGTOP/": "stacks/BIGTOP/",
        "ambari-server/src/main/resources/common-services/": "common-services/",
        "ambari-server/src/main/resources/stack-hooks/": "stacks/BIGTOP/hooks/",
    }
    includes = list(mappings) + ["LICENSE.txt", "NOTICE.txt",
        "ambari-server/src/main/resources/stacks/stack_advisor.py",
        "ambari-server/src/main/resources/stacks/ambari_configuration.py",
        "ambari-server/src/main/resources/stacks/service_advisor.py"]
    process = subprocess.run(["git", "-C", str(repository), "archive", "--format=tar", commit, *includes],
                             stdout=subprocess.PIPE, stderr=subprocess.DEVNULL, check=False)
    if process.returncode != 0:
        raise ContractError("INVALID_SOURCE", "The fixed commit does not contain the declared reference resources")
    output.mkdir(parents=True)
    with tarfile.open(fileobj=io.BytesIO(process.stdout), mode="r:") as archive:
        for member in archive.getmembers():
            if member.isdir():
                continue
            target = next((prefix + member.name[len(source):] for source, prefix in mappings.items()
                           if member.name.startswith(source)), None)
            if target is None:
                target = {"LICENSE.txt": "LICENSE", "NOTICE.txt": "NOTICE",
                          "ambari-server/src/main/resources/stacks/stack_advisor.py": "stacks/stack_advisor.py",
                          "ambari-server/src/main/resources/stacks/ambari_configuration.py": "stacks/ambari_configuration.py",
                          "ambari-server/src/main/resources/stacks/service_advisor.py": "stacks/service_advisor.py"}.get(member.name)
            if target is None:
                raise ContractError("INVALID_SOURCE", "Git archive contains an undeclared reference resource")
            relative = Path(target)
            if relative.is_absolute() or ".." in relative.parts:
                raise ContractError("INVALID_SOURCE", "Reference resource path leaves the export")
            destination = output / relative
            destination.parent.mkdir(parents=True, exist_ok=True)
            if member.issym():
                link = Path(member.linkname)
                if link.is_absolute() or not (destination.parent / link).resolve().is_relative_to(output.resolve()):
                    raise ContractError("INVALID_SOURCE", "Reference symlink leaves the export")
                destination.symlink_to(member.linkname)
            elif member.isfile():
                stream = archive.extractfile(member)
                if stream is None:
                    raise ContractError("INVALID_SOURCE", "Reference resource content is missing")
                destination.write_bytes(stream.read())
                destination.chmod(0o755 if member.mode & 0o111 else 0o644)
            else:
                raise ContractError("INVALID_SOURCE", "Unsupported Git archive member")
    for version in ("3.2.0", "3.3.0", "3.4.0"):
        metainfo = output / "stacks" / "BIGTOP" / version / "metainfo.xml"
        document = ElementTree.parse(metainfo)
        ElementTree.SubElement(document.getroot(), "hooksFolder").text = "stacks/BIGTOP/hooks"
        with metainfo.open("wb") as destination:
            destination.write(("<!--\n" + license_header.strip() + "\n-->\n").encode("utf-8"))
            document.write(destination, encoding="utf-8")
    manifest = {
        "$comment": license_header.strip(), "schema_version": 1, "type": "full-release",
        "name": "hdfs-reference", "version": "1.0.0.0",
        "artifacts": [{"name": "reference-platform", "type": "stack-definitions", "source_dir": "stacks"},
                      {"name": "reference-common-services", "type": "service-definitions", "source_dir": "common-services"}],
    }
    (output / "mpack.json").write_text(canonical(manifest) + "\n", encoding="utf-8")
    (output / "source.json").write_text(canonical({"schema_version": 1, "commit": commit,
        "paths": includes, "projection": "V1_PLATFORM_WITH_HDFS_AND_DECLARED_DEPENDENCIES"}) + "\n", encoding="utf-8")
    validate(output)
    return {"schema_version": 1, "path": str(output), "source_commit": commit,
            "reference_scope": "PLATFORM", "requires_isolated_definition_roots": True}

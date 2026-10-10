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

import gzip
import hashlib
import io
import os
import tarfile
import tempfile
from pathlib import Path

from .contract import ContractError, canonical, read_json, validate_manifest

GENERATED = {"archive.zip", ".hash", ".archive.sha256", ".resource-archive-digests.json", "__pycache__"}


def declared_files(root, manifest):
    root = Path(root).resolve()
    selected = {Path("mpack.json")}
    for name in ("LICENSE", "NOTICE", "README.md", "source.json"):
        if (root / name).is_file():
            selected.add(Path(name))
    for artifact in manifest["artifacts"]:
        source = root / artifact["source_dir"]
        if not source.is_dir() or not source.resolve().is_relative_to(root):
            raise ContractError("INVALID_MANIFEST", "Artifact directory is missing or outside the package")
        for directory, directories, names in os.walk(source, followlinks=False):
            for name in directories:
                if (Path(directory) / name).is_symlink():
                    raise ContractError("INVALID_MANIFEST", "Materialize directory links before building")
            directories[:] = sorted(name for name in directories if name not in GENERATED)
            for name in sorted(names):
                if name in GENERATED or name.endswith(".pyc"):
                    continue
                path = Path(directory) / name
                if not path.is_file() or not path.resolve().is_relative_to(root):
                    raise ContractError("INVALID_MANIFEST", "Artifact file leaves its package")
                selected.add(path.relative_to(root))
    for hook in manifest.get("hooks", []):
        path = root / hook["script"]
        if not path.is_file() or not path.resolve().is_relative_to(root):
            raise ContractError("INVALID_MANIFEST", "Hook script is missing or outside its package")
        selected.add(path.relative_to(root))
    return sorted(selected, key=lambda path: path.as_posix())


def validate(root):
    root = Path(root).resolve()
    manifest = validate_manifest((root / "mpack.json").read_bytes())
    selected = declared_files(root, manifest)
    return manifest, selected


def _write_tar(destination, members):
    destination = Path(destination)
    destination.parent.mkdir(parents=True, exist_ok=True)
    descriptor, temporary_name = tempfile.mkstemp(prefix=".mpack-build-", dir=destination.parent)
    temporary = Path(temporary_name)
    try:
        with os.fdopen(descriptor, "wb") as raw:
            with gzip.GzipFile(filename="", mode="wb", fileobj=raw, mtime=0) as compressed:
                with tarfile.open(mode="w", fileobj=compressed, format=tarfile.PAX_FORMAT) as archive:
                    for name, content, executable in sorted(members, key=lambda member: member[0]):
                        info = tarfile.TarInfo(name)
                        info.size = len(content)
                        info.mode = 0o755 if executable else 0o644
                        info.uid = info.gid = info.mtime = 0
                        info.uname = info.gname = ""
                        archive.addfile(info, io.BytesIO(content))
            raw.flush()
            os.fsync(raw.fileno())
        try:
            os.link(temporary, destination)
        except FileExistsError:
            if hashlib.sha256(destination.read_bytes()).digest() != hashlib.sha256(temporary.read_bytes()).digest():
                raise ContractError("RELEASE_CONFLICT", "A different archive already exists at the output path")
    finally:
        if temporary.exists():
            temporary.unlink()
    return {"path": str(destination), "digest": hashlib.sha256(destination.read_bytes()).hexdigest()}


def build(root, output):
    root = Path(root).resolve()
    manifest, selected = validate(root)
    prefix = manifest["name"] + "-" + manifest["version"]
    members = [(prefix + "/" + path.as_posix(), (root / path).read_bytes(),
                bool((root / path).stat().st_mode & 0o111)) for path in selected]
    shared_index = root / "shared-files.json"
    if shared_index.is_file():
        index = read_json(shared_index.read_bytes())
        if index.get("schema_version") != 1 or not isinstance(index.get("files"), list):
            raise ContractError("INVALID_SHARED_INPUT", "A versioned shared file index is required")
        repository = root.parent.parent.resolve()
        used = {name for name, _content, _executable in members}
        for contribution in index["files"]:
            if set(contribution) != {"source", "target", "digest"}:
                raise ContractError("INVALID_SHARED_INPUT", "Shared files require source, target, and a locked digest")
            source = (repository / contribution["source"]).resolve()
            target = Path(contribution["target"])
            if not source.is_relative_to(repository / "shared") or target.is_absolute() or any(
                part in {".", ".."} for part in target.parts):
                raise ContractError("INVALID_SHARED_INPUT", "Shared contribution leaves its declared boundary")
            name = prefix + "/" + target.as_posix()
            content = source.read_bytes()
            if hashlib.sha256(content).hexdigest() != contribution["digest"] or name in used:
                raise ContractError("INVALID_SHARED_INPUT", "Shared content differs from the lock or duplicates a resource")
            used.add(name)
            members.append((name, content, False))
    result = _write_tar(Path(output) / (prefix + ".tar.gz"), members)
    result.update(name=manifest["name"], version=manifest["version"])
    return result


def selection(repository, names=None, profile=None):
    repository = Path(repository).resolve()
    index = read_json((repository / "release.json").read_bytes())
    if not isinstance(index, dict) or index.get("schema_version") != 1 or not isinstance(index.get("packs"), dict):
        raise ContractError("INVALID_RELEASE_INDEX", "A versioned release.json package mapping is required")
    if profile:
        if not profile.replace("-", "").replace("_", "").isalnum():
            raise ContractError("INVALID_PROFILE", "Invalid profile name")
        chosen = read_json((repository / "profiles" / (profile + ".json")).read_bytes())
        if chosen.get("schema_version") != 1 or not isinstance(chosen.get("packs"), list):
            raise ContractError("INVALID_PROFILE", "A versioned profile package list is required")
        names = chosen["packs"]
    selected = names if names is not None else sorted(index["packs"])
    result = []
    for name in selected:
        entry = index["packs"].get(name)
        if not isinstance(entry, dict) or set(entry) != {"path", "version"}:
            raise ContractError("INVALID_RELEASE_INDEX", "Selected package is missing its exact path or version")
        path = (repository / entry["path"]).resolve()
        if not path.is_relative_to(repository):
            raise ContractError("INVALID_RELEASE_INDEX", "Package path leaves the repository")
        manifest, _ = validate(path)
        if manifest["name"] != name or manifest["version"] != entry["version"]:
            raise ContractError("INVALID_RELEASE_INDEX", "Release index does not match package identity")
        result.append(path)
    if not result or len(set(result)) != len(result):
        raise ContractError("INVALID_RELEASE_INDEX", "Package selection is empty or duplicated")
    return result


def bundle(archives, output, name):
    if not name.replace("-", "").replace("_", "").isalnum():
        raise ContractError("INVALID_BUNDLE", "Invalid bundle name")
    entries = []
    members = []
    for archive in archives:
        path = Path(archive["path"])
        content = path.read_bytes()
        digest = hashlib.sha256(content).hexdigest()
        if digest != archive["digest"]:
            raise ContractError("DIGEST_MISMATCH", "An archive changed after building")
        member = "mpacks/" + path.name
        entries.append({"name": archive["name"], "version": archive["version"], "path": member, "digest": digest})
        members.append((member, content, False))
    members.append(("bundle.json", canonical({"schema_version": 1, "packs": entries}).encode("utf-8"), False))
    return _write_tar(Path(output) / (name + ".bundle.tar.gz"), members)

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

import glob
import os
import posixpath
import re
import stat
import tarfile
import urllib.parse
import uuid

from resource_management.core import sudo
from resource_management.core.exceptions import Fail
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.private_kerberos_cache import (
  PrivateKerberosCache,
)
from resource_management.libraries.functions.show_logs import show_logs
from resource_management.core.resources.system import Directory, Execute, File
from resource_management.core.source import Template
from resource_management.core.logger import Logger
from resource_management.core.signal_utils import TerminateStrategy

import yarn_process_utils


validate_path_segment = yarn_process_utils.validate_path_segment


def validate_hdfs_directory(path, label, allow_top_level=False, allow_uri=False):
  if not isinstance(path, str) or not path.strip():
    raise Fail(f"{label} must be a non-empty absolute HDFS directory")
  value = path.strip()
  if any(ord(character) < 32 or ord(character) == 127 for character in value):
    raise Fail(f"{label} must not contain control characters: {path!r}")
  parsed = urllib.parse.urlsplit(value)
  if parsed.scheme:
    if (
      not allow_uri
      or parsed.scheme.lower() not in ("hdfs", "viewfs")
      or parsed.query
      or parsed.fragment
      or "@" in parsed.netloc
    ):
      raise Fail(f"{label} has an unsupported HDFS URI: {path!r}")
    raw_path = parsed.path
  else:
    if parsed.netloc or parsed.query or parsed.fragment:
      raise Fail(f"{label} has an unsupported HDFS path: {path!r}")
    raw_path = parsed.path

  if "%" in parsed.netloc or "%" in raw_path or "\" in raw_path:
    raise Fail(f"{label} must not contain encoded or backslash path data: {path!r}")

  raw_parts = [part for part in raw_path.split("/") if part]
  normalized_path = "/" + posixpath.normpath("/" + raw_path.lstrip("/")).lstrip("/")
  normalized_parts = [part for part in normalized_path.split("/") if part]
  if (
    not raw_path.startswith("/")
    or ".." in raw_parts
    or normalized_path == "/"
    or (not allow_top_level and len(normalized_parts) < 2)
  ):
    raise Fail(f"{label} is an unsafe HDFS directory: {path!r}")
  if not parsed.scheme:
    return normalized_path
  return urllib.parse.urlunsplit(
    (parsed.scheme.lower(), parsed.netloc, normalized_path, "", "")
  )


def join_hdfs_directory(base, *segments):
  parsed = urllib.parse.urlsplit(base)
  joined_path = posixpath.join(parsed.path if parsed.scheme else base, *segments)
  if not parsed.scheme:
    return joined_path
  return urllib.parse.urlunsplit(
    (parsed.scheme, parsed.netloc, joined_path, "", "")
  )


def _validate_archive_directory(path, version):
  validate_path_segment(version, "stack version")
  archive_root = "/var/lib/ambari-agent/yarn-ats-hbase"
  expected_path = os.path.join(archive_root, version)
  normalized = os.path.normpath(path)
  if normalized != expected_path:
    raise Fail(f"ATS HBase archive directory must be {expected_path}")
  current = os.sep
  for path_part in [part for part in normalized.split(os.sep) if part]:
    current = os.path.join(current, path_part)
    if not sudo.path_lexists(current):
      continue
    metadata = sudo.lstat(current)
    if sudo.path_islink(current):
      raise Fail(f"ATS HBase archive directory must not contain a symlink: {current}")
    if not stat.S_ISDIR(metadata.st_mode):
      raise Fail(f"ATS HBase archive parent must be a directory: {current}")
    if metadata.st_uid != 0 or metadata.st_mode & 0o022:
      raise Fail(
        "ATS HBase archive parent must be root-owned and non-writable: "
        f"{current}"
      )
  return normalized


def _hadoop_archive_required_members(stack_version):
  hadoop_version = stack_version.split("-", 1)[0]
  return (
    "hadoop/bin/hadoop",
    "hadoop/bin/hdfs",
    "hadoop/bin/mapred",
    "hadoop/bin/yarn",
    f"hadoop/share/hadoop/common/hadoop-common-{hadoop_version}.jar",
    f"hadoop/share/hadoop/hdfs/hadoop-hdfs-{hadoop_version}.jar",
    f"hadoop/share/hadoop/mapreduce/hadoop-mapreduce-client-core-{hadoop_version}.jar",
    f"hadoop/share/hadoop/yarn/hadoop-yarn-api-{hadoop_version}.jar",
    f"hadoop/share/hadoop/yarn/timelineservice/"
    f"hadoop-yarn-server-timelineservice-{hadoop_version}.jar",
    "hbase/bin/hbase",
  )


def _copy_directory_contents(source, destination):
  Execute(
    (
      "cp",
      "-R",
      "--preserve=mode,timestamps,links",
      "--",
      os.path.join(source, "."),
      destination,
    ),
    user="root",
    timeout=300,
    timeout_kill_strategy=TerminateStrategy.KILL_PROCESS_GROUP,
  )


def _source_identity(metadata, link_target=None):
  return (
    metadata.st_dev,
    metadata.st_ino,
    metadata.st_mode,
    metadata.st_uid,
    getattr(metadata, "st_gid", None),
    getattr(metadata, "st_size", None),
    getattr(metadata, "st_mtime_ns", None),
    link_target,
  )


def _validate_package_source_chain(path, version_lib_dir):
  normalized = os.path.normpath(path)
  if (
    not os.path.isabs(path)
    or normalized != path
    or os.path.commonpath((normalized, version_lib_dir)) != version_lib_dir
  ):
    raise Fail(f"BIGTOP package source is outside {version_lib_dir}: {path}")
  identities = {}
  current = os.sep
  for part in [part for part in normalized.split(os.sep) if part]:
    current = os.path.join(current, part)
    try:
      metadata = sudo.lstat(current)
    except Exception as error:
      raise Fail(f"BIGTOP package source is not accessible: {current}") from error
    if stat.S_ISLNK(metadata.st_mode):
      raise Fail(f"BIGTOP package source chain must not contain symlinks: {current}")
    if not stat.S_ISDIR(metadata.st_mode):
      raise Fail(f"BIGTOP package source parent must be a directory: {current}")
    if metadata.st_uid != 0 or metadata.st_mode & 0o022:
      raise Fail(
        "BIGTOP package source chain must be root-owned and non-writable: "
        f"{current}"
      )
    identities[current] = _source_identity(metadata)
  return identities


def _snapshot_package_sources(source_roots, version_lib_dir):
  version_lib_dir = os.path.normpath(version_lib_dir)
  snapshot = _validate_package_source_chain(version_lib_dir, version_lib_dir)

  def fail_walk(error):
    raise Fail(f"BIGTOP package source tree is not readable: {error}") from error

  for source_root in source_roots:
    snapshot.update(_validate_package_source_chain(source_root, version_lib_dir))
    for current_root, directory_names, file_names in os.walk(
      source_root, topdown=True, onerror=fail_walk, followlinks=False
    ):
      for name in list(directory_names) + list(file_names):
        path = os.path.join(current_root, name)
        try:
          metadata = sudo.lstat(path)
        except Exception as error:
          raise Fail(f"BIGTOP package source changed during inspection: {path}") from error
        if stat.S_ISLNK(metadata.st_mode):
          if metadata.st_uid != 0:
            raise Fail(f"BIGTOP package source symlink must be root-owned: {path}")
          try:
            link_target = os.readlink(path)
          except OSError as error:
            raise Fail(f"BIGTOP package source symlink is not readable: {path}") from error
          if name in directory_names:
            directory_names.remove(name)
        else:
          link_target = None
          if not (stat.S_ISDIR(metadata.st_mode) or stat.S_ISREG(metadata.st_mode)):
            raise Fail(f"BIGTOP package source has an unsupported file type: {path}")
          if metadata.st_uid != 0 or metadata.st_mode & 0o022:
            raise Fail(
              "BIGTOP package source entries must be root-owned and non-writable: "
              f"{path}"
            )
        snapshot[path] = _source_identity(metadata, link_target)
  return snapshot


def _require_same_package_sources(expected, source_roots, version_lib_dir):
  current = _snapshot_package_sources(source_roots, version_lib_dir)
  if current != expected:
    changed_paths = sorted(set(current).symmetric_difference(expected))
    if not changed_paths:
      changed_paths = sorted(
        path for path in current if current[path] != expected[path]
      )
    raise Fail(
      "BIGTOP package sources changed while creating the ATS HBase archive: "
      + ", ".join(changed_paths[:10])
    )


def _copy_matching_files(pattern, destination):
  matches = sorted(glob.glob(pattern))
  if not matches:
    raise Fail(f"No BIGTOP package files match {pattern}")
  for source in matches:
    source_identity = _regular_file_identity(source, "BIGTOP package file")
    Execute(
      ("cp", "--", source, destination),
      user="root",
      timeout=60,
      timeout_kill_strategy=TerminateStrategy.KILL_PROCESS_GROUP,
    )
    _require_same_file(source, source_identity, "BIGTOP package file")


def _replace_external_zookeeper_links(source_directory, destination):
  for external_jar in glob.glob(os.path.join(destination, "zookeeper*.jar")):
    File(external_jar, action="delete")
  for versioned_jar_pattern in (
    "zookeeper-[0-9]*.jar",
    "zookeeper-jute-[0-9]*.jar",
  ):
    _copy_matching_files(
      os.path.join(source_directory, versioned_jar_pattern), destination
    )


def _regular_file_identity(path, label):
  try:
    metadata = sudo.lstat(path)
  except Exception as error:
    raise Fail(f"{label} is not accessible: {path}") from error
  if not stat.S_ISREG(metadata.st_mode):
    raise Fail(f"{label} is not a regular file: {path}")
  if metadata.st_uid != 0 or metadata.st_mode & 0o022:
    raise Fail(f"{label} must be root-owned and non-writable: {path}")
  return metadata


def _require_same_file(path, expected, label):
  current = _regular_file_identity(path, label)
  if (current.st_dev, current.st_ino) != (expected.st_dev, expected.st_ino):
    raise Fail(f"{label} changed while it was being published: {path}")
  return current


def _unlink_if_same_file(path, expected):
  if not sudo.path_lexists(path):
    return False
  current = sudo.lstat(path)
  if stat.S_ISREG(current.st_mode) and (current.st_dev, current.st_ino) == (
    expected.st_dev,
    expected.st_ino,
  ):
    sudo.unlink(path)
    return True
  return False


def _validate_tar_archive(archive_path, expected_roots, required_members):
  try:
    with tarfile.open(archive_path, "r:gz") as archive:
      members = archive.getmembers()
  except (OSError, tarfile.TarError) as error:
    raise Fail(f"Archive {archive_path} is not a readable gzip tar file") from error

  members_by_name = {}
  roots = set()
  for member in members:
    name = member.name.rstrip("/")
    normalized_name = posixpath.normpath(name)
    if (
      not name
      or name.startswith("/")
      or normalized_name != name
      or normalized_name == ".."
      or normalized_name.startswith("../")
    ):
      raise Fail(f"Archive {archive_path} contains unsafe member {member.name!r}")
    if normalized_name in members_by_name:
      raise Fail(
        f"Archive {archive_path} contains duplicate member {normalized_name!r}"
      )
    members_by_name[normalized_name] = member
    roots.add(normalized_name.split("/", 1)[0])
    if not (member.isreg() or member.isdir() or member.issym() or member.islnk()):
      raise Fail(
        f"Archive {archive_path} contains unsupported member {member.name!r}"
      )
    if member.issym() or member.islnk():
      link_name = member.linkname
      if not link_name or link_name.startswith("/"):
        raise Fail(
          f"Archive {archive_path} contains unsafe link {member.name!r}"
        )
      if member.issym():
        link_target = posixpath.normpath(
          posixpath.join(posixpath.dirname(normalized_name), link_name)
        )
      else:
        link_target = posixpath.normpath(link_name)
      if link_target == ".." or link_target.startswith("../"):
        raise Fail(
          f"Archive {archive_path} contains escaping link {member.name!r}"
        )

  if roots != set(expected_roots):
    raise Fail(
      f"Archive {archive_path} has unexpected top-level entries: "
      f"{', '.join(sorted(roots)) or '<empty>'}"
    )
  missing_members = sorted(set(required_members) - set(members_by_name))
  if missing_members:
    raise Fail(
      f"Archive {archive_path} is missing required members: "
      + ", ".join(missing_members)
    )
  for required_name in required_members:
    member = members_by_name[required_name]
    if not member.isreg():
      raise Fail(
        f"Archive {archive_path} required member {required_name!r} is not a regular file"
      )
    if "/bin/" in required_name and not member.mode & 0o111:
      raise Fail(
        f"Archive {archive_path} required executable {required_name!r} is not executable"
      )


def _delete_staged_directory_or_link(path):
  if not sudo.path_lexists(path):
    return
  if sudo.path_islink(path):
    sudo.unlink(path)
  elif sudo.path_isdir(path):
    Directory(path, action="delete")
  else:
    raise Fail(f"Refusing unexpected staged HBase path type at {path}")


def hbase_service(name, action="start"):  # 'start' or 'stop'
  import params

  daemon_script = format("{yarn_hbase_bin}/hbase-daemon.sh")
  role = name
  pid_file = format("{yarn_hbase_pid_dir}/hbase-{yarn_hbase_user}-{role}.pid")

  if action == "start":
    start_attempted = False
    try:
      running = yarn_process_utils.recover_running_process(
        pid_file,
        params.yarn_hbase_user,
        role,
        params.yarn_hbase_user,
        params.user_group,
      )
      if running is not None:
        Logger.info(f"YARN ATS HBase {role} is already running as pid {running.pid}")
        return False
      start_attempted = True
      Execute(
        (daemon_script, "--config", params.yarn_hbase_conf_dir, "start", role),
        user=params.yarn_hbase_user,
        timeout=300,
        timeout_kill_strategy=TerminateStrategy.KILL_PROCESS_GROUP,
      )
      yarn_process_utils.wait_for_running_process(
        pid_file,
        params.yarn_hbase_user,
        role,
        params.yarn_hbase_user,
        params.user_group,
      )
      return True
    except Exception as error:
      secondary_errors = []
      if start_attempted:
        try:
          yarn_process_utils.stop_process(
            pid_file,
            params.yarn_hbase_user,
            role,
            params.yarn_hbase_user,
            params.user_group,
          )
        except Exception as cleanup_error:
          secondary_errors.append(
            f"failed to stop newly started HBase {role}: {cleanup_error}"
          )
      try:
        show_logs(params.yarn_hbase_log_dir, params.yarn_hbase_user)
      except Exception as log_error:
        secondary_errors.append(f"log collection failed: {log_error}")
      if secondary_errors:
        raise RuntimeError(
          f"{error}; additionally {'; '.join(secondary_errors)}"
        ) from error
      raise
  elif action == "stop":
    try:
      yarn_process_utils.stop_process(
        pid_file,
        params.yarn_hbase_user,
        role,
        params.yarn_hbase_user,
        params.user_group,
      )
    except Exception as error:
      try:
        show_logs(params.yarn_hbase_log_dir, params.yarn_hbase_user)
      except Exception as log_error:
        raise RuntimeError(
          f"{error}; additionally log collection failed: {log_error}"
        ) from error
      raise


def rollback_hbase_roles(started_roles):
  cleanup_errors = []
  for role in reversed(started_roles):
    try:
      hbase_service(role, action="stop")
    except Exception as error:
      cleanup_errors.append(f"{role}: {error}")
  return cleanup_errors


def hbase(action):
  if action == "stop":
    Logger.info("Stopping HBase daemons")
    cleanup_errors = []
    for role in ("regionserver", "master"):
      try:
        hbase_service(role, action=action)
      except Exception as error:
        cleanup_errors.append(f"{role}: {error}")
    if cleanup_errors:
      raise RuntimeError(
        "Failed to stop HBase roles: " + "; ".join(cleanup_errors)
      )
  else:
    Logger.info("Starting HBase daemons")
    started_roles = []
    try:
      if hbase_service("master", action=action):
        started_roles.append("master")
      if hbase_service("regionserver", action=action):
        started_roles.append("regionserver")
      createTables()
      return tuple(started_roles)
    except Exception as error:
      cleanup_errors = rollback_hbase_roles(started_roles)
      if cleanup_errors:
        raise Fail(
          f"{error}; additionally failed to roll back HBase roles: "
          + "; ".join(cleanup_errors)
        ) from error
      raise


def configure_hbase(env):
  import params

  env.set_params(params)
  yarn_hbase_hdfs_root_dir = validate_hdfs_directory(
    params.yarn_hbase_hdfs_root_dir,
    "yarn-hbase-site/hbase.rootdir",
    allow_uri=True,
  )
  params.HdfsResource(
    yarn_hbase_hdfs_root_dir,
    type="directory",
    action="create_on_execute",
    owner=params.yarn_hbase_user,
  )
  params.HdfsResource(None, action="execute")


def create_hbase_package():
  import params

  if not isinstance(params.version, str) or not re.fullmatch(
    r"[A-Za-z0-9][A-Za-z0-9._-]*", params.version
  ):
    raise Fail("A valid stack version is required to create the ATS HBase archive")
  archive_directory = _validate_archive_directory(
    params.yarn_hbase_user_tmp, params.version
  )
  if not sudo.path_lexists(archive_directory):
    Directory(
      archive_directory,
      owner="root",
      group="root",
      mode=0o755,
      create_parents=True,
    )

  archive_path = os.path.join(archive_directory, "hbase.tar.gz")
  required_archive_members = _hadoop_archive_required_members(params.version)
  if sudo.path_islink(archive_path):
    raise Fail(f"Refusing symbolic link at HBase archive path {archive_path}")
  if sudo.path_exists(archive_path):
    existing_identity = _regular_file_identity(
      archive_path, "Existing HBase archive"
    )
    _validate_tar_archive(
      archive_path,
      ("hadoop", "hbase"),
      required_archive_members,
    )
    _require_same_file(archive_path, existing_identity, "Existing HBase archive")
    File(
      archive_path,
      owner="root",
      group="root",
      mode=0o444,
    )
    _require_same_file(archive_path, existing_identity, "Existing HBase archive")
    Logger.info(f"Reusing existing HBase archive {archive_path}")
    return

  version_lib_dir = os.path.join(params.stack_root, params.version, "usr", "lib")
  source_hbase_dir = os.path.join(version_lib_dir, "hbase")
  source_zookeeper_lib_dir = os.path.join(version_lib_dir, "zookeeper")
  hadoop_sources = {
    "common": os.path.join(version_lib_dir, "hadoop"),
    "hdfs": os.path.join(version_lib_dir, "hadoop-hdfs"),
    "mapreduce": os.path.join(version_lib_dir, "hadoop-mapreduce"),
    "yarn": os.path.join(version_lib_dir, "hadoop-yarn"),
  }
  required_directories = (
    source_hbase_dir,
    source_zookeeper_lib_dir,
    hadoop_sources["common"],
    os.path.join(hadoop_sources["common"], "lib"),
    os.path.join(hadoop_sources["common"], "libexec"),
    os.path.join(hadoop_sources["common"], "tools", "lib"),
    hadoop_sources["hdfs"],
    os.path.join(hadoop_sources["hdfs"], "lib"),
    hadoop_sources["mapreduce"],
    hadoop_sources["yarn"],
    os.path.join(hadoop_sources["yarn"], "lib"),
    os.path.join(hadoop_sources["yarn"], "timelineservice"),
    os.path.join(hadoop_sources["yarn"], "timelineservice", "lib"),
  )
  source_roots = (
    source_hbase_dir,
    source_zookeeper_lib_dir,
    hadoop_sources["common"],
    hadoop_sources["hdfs"],
    hadoop_sources["mapreduce"],
    hadoop_sources["yarn"],
  )
  source_snapshot = _snapshot_package_sources(source_roots, version_lib_dir)
  for required_directory in required_directories:
    metadata = sudo.lstat(required_directory)
    if not stat.S_ISDIR(metadata.st_mode):
      raise Fail(
        "Cannot create ATS HBase archive; required package directory is invalid: "
        f"{required_directory}"
      )
  hadoop_version = params.version.split("-", 1)[0]
  required_source_files = (
    os.path.join(source_hbase_dir, "bin", "hbase"),
    os.path.join(hadoop_sources["common"], "bin", "hadoop"),
    os.path.join(hadoop_sources["hdfs"], "bin", "hdfs"),
    os.path.join(hadoop_sources["mapreduce"], "bin", "mapred"),
    os.path.join(hadoop_sources["yarn"], "bin", "yarn"),
    os.path.join(hadoop_sources["common"], f"hadoop-common-{hadoop_version}.jar"),
    os.path.join(hadoop_sources["hdfs"], f"hadoop-hdfs-{hadoop_version}.jar"),
    os.path.join(
      hadoop_sources["mapreduce"],
      f"hadoop-mapreduce-client-core-{hadoop_version}.jar",
    ),
    os.path.join(hadoop_sources["yarn"], f"hadoop-yarn-api-{hadoop_version}.jar"),
    os.path.join(
      hadoop_sources["yarn"],
      "timelineservice",
      f"hadoop-yarn-server-timelineservice-{hadoop_version}.jar",
    ),
  )
  for required_source_file in required_source_files:
    _regular_file_identity(required_source_file, "Required BIGTOP package file")
  hbase_lib_dir = os.path.join(source_hbase_dir, "lib")
  if not stat.S_ISDIR(sudo.lstat(hbase_lib_dir).st_mode):
    raise Fail(
      f"Cannot create ATS HBase archive; {hbase_lib_dir} is not a directory"
    )
  staging_dir = os.path.join(
    params.yarn_hbase_user_tmp, f".hbase-package-{uuid.uuid4().hex}"
  )
  staged_hbase_dir = os.path.join(staging_dir, "hbase")
  staged_hbase_lib_dir = os.path.join(staged_hbase_dir, "lib")
  staged_archive = os.path.join(staging_dir, "hbase.tar.gz")

  primary_error = None
  published_identity = None
  try:
    Directory(
      staging_dir,
      owner="root",
      group="root",
      mode=0o700,
    )
    Execute(
      (
        "cp",
        "-R",
        "--preserve=mode,timestamps,links",
        "--",
        source_hbase_dir,
        staged_hbase_dir,
      ),
      user="root",
      timeout=300,
      timeout_kill_strategy=TerminateStrategy.KILL_PROCESS_GROUP,
    )
    for staged_runtime_path in ("conf", "logs", "pids"):
      _delete_staged_directory_or_link(
        os.path.join(staged_hbase_dir, staged_runtime_path)
      )
    # BIGTOP's HBase package links these jars to absolute host paths. The
    # system-service archive carries its own Hadoop tree and must be portable.
    for external_jar_pattern in ("hadoop*.jar",):
      for external_jar in glob.glob(
        os.path.join(staged_hbase_lib_dir, external_jar_pattern)
      ):
        File(external_jar, action="delete")
    _replace_external_zookeeper_links(
      source_zookeeper_lib_dir, staged_hbase_lib_dir
    )

    staged_hadoop_dir = os.path.join(staging_dir, "hadoop")
    hadoop_destinations = {
      "bin": os.path.join(staged_hadoop_dir, "bin"),
      "libexec": os.path.join(staged_hadoop_dir, "libexec"),
      "common": os.path.join(staged_hadoop_dir, "share", "hadoop", "common"),
      "common_lib": os.path.join(
        staged_hadoop_dir, "share", "hadoop", "common", "lib"
      ),
      "hdfs": os.path.join(staged_hadoop_dir, "share", "hadoop", "hdfs"),
      "hdfs_lib": os.path.join(
        staged_hadoop_dir, "share", "hadoop", "hdfs", "lib"
      ),
      "mapreduce": os.path.join(
        staged_hadoop_dir, "share", "hadoop", "mapreduce"
      ),
      "yarn": os.path.join(staged_hadoop_dir, "share", "hadoop", "yarn"),
      "yarn_lib": os.path.join(
        staged_hadoop_dir, "share", "hadoop", "yarn", "lib"
      ),
      "timeline": os.path.join(
        staged_hadoop_dir, "share", "hadoop", "yarn", "timelineservice"
      ),
      "timeline_lib": os.path.join(
        staged_hadoop_dir,
        "share",
        "hadoop",
        "yarn",
        "timelineservice",
        "lib",
      ),
      "tools_lib": os.path.join(
        staged_hadoop_dir, "share", "hadoop", "tools", "lib"
      ),
    }
    Directory(
      list(hadoop_destinations.values()),
      owner="root",
      group="root",
      mode=0o755,
      create_parents=True,
    )
    for component, executable in (
      ("common", "hadoop"),
      ("hdfs", "hdfs"),
      ("mapreduce", "mapred"),
      ("yarn", "yarn"),
    ):
      Execute(
        (
          "cp",
          "--",
          os.path.join(hadoop_sources[component], "bin", executable),
          hadoop_destinations["bin"],
        ),
        user="root",
        timeout=60,
        timeout_kill_strategy=TerminateStrategy.KILL_PROCESS_GROUP,
      )
    _copy_directory_contents(
      os.path.join(hadoop_sources["common"], "libexec"),
      hadoop_destinations["libexec"],
    )
    _copy_matching_files(
      os.path.join(hadoop_sources["common"], "*.jar"),
      hadoop_destinations["common"],
    )
    _copy_directory_contents(
      os.path.join(hadoop_sources["common"], "lib"),
      hadoop_destinations["common_lib"],
    )
    _replace_external_zookeeper_links(
      source_zookeeper_lib_dir, hadoop_destinations["common_lib"]
    )
    _copy_directory_contents(
      os.path.join(hadoop_sources["common"], "tools", "lib"),
      hadoop_destinations["tools_lib"],
    )
    _copy_matching_files(
      os.path.join(hadoop_sources["hdfs"], "*.jar"),
      hadoop_destinations["hdfs"],
    )
    _copy_directory_contents(
      os.path.join(hadoop_sources["hdfs"], "lib"),
      hadoop_destinations["hdfs_lib"],
    )
    _copy_matching_files(
      os.path.join(hadoop_sources["mapreduce"], "*.jar"),
      hadoop_destinations["mapreduce"],
    )
    _copy_matching_files(
      os.path.join(hadoop_sources["yarn"], "*.jar"),
      hadoop_destinations["yarn"],
    )
    _copy_directory_contents(
      os.path.join(hadoop_sources["yarn"], "lib"),
      hadoop_destinations["yarn_lib"],
    )
    _copy_matching_files(
      os.path.join(hadoop_sources["yarn"], "timelineservice", "*.jar"),
      hadoop_destinations["timeline"],
    )
    _copy_directory_contents(
      os.path.join(hadoop_sources["yarn"], "timelineservice", "lib"),
      hadoop_destinations["timeline_lib"],
    )
    _require_same_package_sources(
      source_snapshot, source_roots, version_lib_dir
    )
    Execute(
      (
        "tar",
        "-czf",
        staged_archive,
        "-C",
        staging_dir,
        "hbase",
        "hadoop",
      ),
      user="root",
      timeout=300,
      timeout_kill_strategy=TerminateStrategy.KILL_PROCESS_GROUP,
    )
    _validate_tar_archive(
      staged_archive,
      ("hadoop", "hbase"),
      required_archive_members,
    )
    File(staged_archive, owner="root", group="root", mode=0o444)
    staged_identity = _regular_file_identity(staged_archive, "Staged HBase archive")
    try:
      sudo.link_exclusive(staged_archive, archive_path)
    except Exception as error:
      try:
        concurrent_identity = _regular_file_identity(
          archive_path, "Concurrently published HBase archive"
        )
      except Fail:
        raise Fail(f"Could not publish HBase archive {archive_path}: {error}") from error
      _validate_tar_archive(
        archive_path,
        ("hadoop", "hbase"),
        required_archive_members,
      )
      _require_same_file(
        archive_path,
        concurrent_identity,
        "Concurrently published HBase archive",
      )
      Logger.info(f"Another command published HBase archive {archive_path}")
    else:
      published_identity = staged_identity
      staged_after_link = _require_same_file(
        staged_archive, staged_identity, "Staged HBase archive"
      )
      published_after_link = _require_same_file(
        archive_path, staged_identity, "Published HBase archive"
      )
      if staged_after_link.st_nlink < 2 or published_after_link.st_nlink < 2:
        raise Fail(
          f"Published HBase archive {archive_path} does not have a stable hard link"
        )
    File(
      archive_path,
      owner="root",
      group="root",
      mode=0o444,
    )
    if published_identity is not None:
      _require_same_file(
        archive_path, published_identity, "Published HBase archive"
      )
  except Exception as error:
    rollback_error = None
    if published_identity is not None:
      try:
        _unlink_if_same_file(archive_path, published_identity)
      except Exception as cleanup_error:
        rollback_error = cleanup_error
    if rollback_error is not None:
      primary_error = Fail(
        f"{error}; additionally failed to roll back HBase archive publication: "
        f"{rollback_error}"
      )
      raise primary_error from error
    primary_error = error
    raise
  finally:
    try:
      Directory(staging_dir, action="delete")
    except Exception as cleanup_error:
      if primary_error is None:
        raise
      raise Fail(
        f"{primary_error}; additionally failed to remove staging directory "
        f"{staging_dir}: {cleanup_error}"
      ) from primary_error


def copy_hbase_package_to_hdfs():
  import params

  yarn_hbase_app_hdfs_path = validate_hdfs_directory(
    params.yarn_hbase_app_hdfs_path, "ATS HBase application HDFS path"
  )
  try:
    Logger.info(
      "Copying hbase tarball into hdfs path'" + yarn_hbase_app_hdfs_path + "'."
    )
    params.HdfsResource(
      yarn_hbase_app_hdfs_path,
      type="directory",
      action="create_on_execute",
      owner=params.hdfs_user,
      group=params.hdfs_user,
      mode=0o555,
    )
    params.HdfsResource(
      posixpath.join(yarn_hbase_app_hdfs_path, "hbase.tar.gz"),
      type="file",
      action="create_on_execute",
      source=format("{yarn_hbase_user_tmp}/hbase.tar.gz"),
      owner=params.hdfs_user,
      group=params.user_group,
      mode=0o444,
    )
    params.HdfsResource(None, action="execute")
  except Exception:
    Logger.error(
      "Error occurred while copying HBase archive into HDFS '"
      + params.yarn_hbase_app_hdfs_path
      + "'."
    )
    raise


def createTables():
  import params

  try:
    Logger.info("Creating HBase tables")
    Execute(("sleep", "10"), timeout=15)
    def execute_hbase_commands(environment, grant_permissions_file=None):
      Execute(
        (
          params.yarn_hbase_executable,
          "--config",
          params.yarn_hbase_conf_dir,
          *params.yarn_hbase_schema_creator_args,
        ),
        user=params.yarn_hbase_user,
        timeout=300,
        timeout_kill_strategy=TerminateStrategy.KILL_PROCESS_GROUP,
        logoutput=True,
        environment=environment,
      )
      if params.security_enabled:
        Execute(
          (
            params.yarn_hbase_executable,
            "--config",
            params.yarn_hbase_conf_dir,
            "shell",
            grant_permissions_file,
          ),
          user=params.yarn_hbase_user,
          timeout=300,
          timeout_kill_strategy=TerminateStrategy.KILL_PROCESS_GROUP,
          logoutput=True,
          environment=environment,
        )

    base_environment = {
      "HBASE_CLASSPATH_PREFIX": params.yarn_hbase_classpath_prefix
    }
    if params.security_enabled:
      with PrivateKerberosCache(
        params.yarn_hbase_user, params.user_group
      ) as cache:
        grant_permissions_file = os.path.join(
          cache.cache_dir, "hbase_grant_permissions.rb"
        )
        File(
          grant_permissions_file,
          owner=params.yarn_hbase_user,
          group=params.user_group,
          mode=0o600,
          content=Template("yarn_hbase_grant_permissions.j2"),
        )
        cache.kinit(
          params.kinit_path_local,
          params.yarn_ats_user_keytab,
          params.yarn_ats_principal_name,
        )
        execute_hbase_commands(
          cache.merge_environment(base_environment), grant_permissions_file
        )
    else:
      execute_hbase_commands(base_environment)
  except Exception:
    show_logs(params.yarn_hbase_log_dir, params.yarn_hbase_user)
    raise

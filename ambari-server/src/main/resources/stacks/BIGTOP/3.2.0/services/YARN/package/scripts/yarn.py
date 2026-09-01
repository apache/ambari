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

Ambari Agent

"""

# Python Imports
import os
import posixpath
import pwd
import stat

# Ambari Common and Resource Management Imports
from resource_management.core import sudo
from resource_management.core.exceptions import Fail
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.lzo_utils import install_lzo_if_needed
from resource_management.core.resources.system import Directory, Execute
from resource_management.core.resources.system import File
from resource_management.libraries.resources.xml_config import XmlConfig
from resource_management.core.source import InlineTemplate, Template
from resource_management.core.logger import Logger
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from resource_management.libraries.script.config_dictionary import UnknownConfiguration

from resource_management.libraries.functions.mounted_dirs_helper import (
  handle_mounted_dirs,
)
from hbase_service import (
  create_hbase_package,
  copy_hbase_package_to_hdfs,
  createTables,
  join_hdfs_directory,
  validate_hdfs_directory,
  validate_path_segment,
)

_validate_hdfs_directory = validate_hdfs_directory


def _is_trusted_var_run_alias(path):
  if path != "/var/run" or not sudo.path_islink(path):
    return False
  target = sudo.readlink(path)
  if not os.path.isabs(target):
    target = os.path.join(os.path.dirname(path), target)
  return os.path.normpath(target) == "/run" and sudo.stat(path).st_uid == 0


def _validate_local_path(path, label, minimum_parts):
  if not isinstance(path, str) or not path:
    raise Fail(f"{label} must be a non-empty absolute path")
  normalized = os.path.normpath(path)
  if path != normalized:
    raise Fail(f"{label} must be a normalized absolute path: {path!r}")
  path_parts = [part for part in normalized.split(os.sep) if part]
  if not os.path.isabs(normalized) or len(path_parts) < minimum_parts:
    raise Fail(f"{label} must not target a relative or top-level path: {path!r}")

  current = os.sep
  for path_part in path_parts:
    current = os.path.join(current, path_part)
    if (
      sudo.path_lexists(current)
      and sudo.path_islink(current)
      and not _is_trusted_var_run_alias(current)
    ):
      raise Fail(f"{label} must not contain a symbolic link: {current}")
  return normalized


def _validate_local_service_directory(path, label):
  return _validate_local_path(path, label, minimum_parts=2)


def _validate_local_service_file(path, label):
  return _validate_local_path(path, label, minimum_parts=3)


def _daemon_owned_directory(
  path,
  label,
  owner,
  group,
  mode=0o755,
  cd_access="a",
):
  normalized = _validate_local_service_directory(path, label)
  validate_path_segment(owner, f"{label} owner")
  try:
    expected_uid = pwd.getpwnam(owner).pw_uid
  except KeyError as error:
    raise Fail(f"{label} owner does not exist: {owner}") from error

  target_exists = sudo.path_lexists(normalized)
  path_parts = [part for part in normalized.split(os.sep) if part]
  current = os.sep
  closest_existing_parent = os.sep
  for path_part in path_parts:
    current = os.path.join(current, path_part)
    if not sudo.path_lexists(current):
      continue
    if sudo.path_islink(current):
      if not _is_trusted_var_run_alias(current):
        raise Fail(f"{label} must not contain a symbolic link: {current}")
    elif not stat.S_ISDIR(sudo.lstat(current).st_mode):
      raise Fail(f"{label} parent must be a directory: {current}")

    metadata = sudo.stat(current)
    is_sticky_temporary_parent = (
      target_exists
      and current in ("/tmp", "/var/tmp")
      and metadata.st_uid == 0
      and metadata.st_mode & 0o1000
    )
    if metadata.st_uid not in (0, expected_uid) or (
      metadata.st_mode & 0o022 and not is_sticky_temporary_parent
    ):
      raise Fail(
        f"{label} parent must be root-owned or owned by {owner} and "
        f"non-writable: {current}"
      )
    if current != normalized:
      closest_existing_parent = current

  if target_exists:
    metadata = sudo.stat(normalized)
    if metadata.st_uid != expected_uid:
      raise Fail(f"{label} must already be owned by {owner}: {normalized}")
    return normalized

  parent_metadata = sudo.stat(closest_existing_parent)
  if parent_metadata.st_uid != 0 or parent_metadata.st_mode & 0o022:
    raise Fail(
      f"{label} closest existing parent must be root-owned and non-writable: "
      f"{closest_existing_parent}"
    )

  Directory(
    normalized,
    owner=owner,
    group=group,
    create_parents=True,
    mode=mode,
    cd_access=cd_access,
  )
  return normalized


def _root_owned_directory(path, label, allowed_roots, group="root", mode=0o755):
  normalized = _validate_local_service_directory(path, label)
  normalized_roots = tuple(os.path.normpath(root) for root in allowed_roots)
  if not any(
    normalized != root and os.path.commonpath((normalized, root)) == root
    for root in normalized_roots
  ):
    roots = ", ".join(normalized_roots)
    raise Fail(f"{label} must be below one of the allowed roots: {roots}")

  path_parts = [part for part in normalized.split(os.sep) if part]
  current = os.sep
  target_exists = False
  for path_part in path_parts:
    current = os.path.join(current, path_part)
    if not sudo.path_lexists(current):
      continue
    target_exists = current == normalized
    if sudo.path_islink(current):
      if not _is_trusted_var_run_alias(current):
        raise Fail(f"{label} must not contain a symbolic link: {current}")
    elif not stat.S_ISDIR(sudo.lstat(current).st_mode):
      raise Fail(f"{label} parent must be a directory: {current}")
    metadata = sudo.stat(current)
    if metadata.st_uid != 0 or metadata.st_mode & 0o022:
      raise Fail(
        f"{label} parent must be root-owned and non-writable: {current}"
      )

  if not target_exists:
    Directory(
      normalized,
      owner="root",
      group=group,
      create_parents=True,
      mode=mode,
    )
  return normalized


def _validate_root_managed_config_file(path, label, config_dir, stack_root):
  normalized = os.path.normpath(path) if isinstance(path, str) else ""
  normalized_config_dir = (
    os.path.normpath(config_dir) if isinstance(config_dir, str) else ""
  )
  if normalized_config_dir != "/etc/hadoop/conf":
    raise Fail("ResourceManager host files require the BIGTOP Hadoop config directory")
  normalized_stack_root = (
    os.path.normpath(stack_root) if isinstance(stack_root, str) else ""
  )
  if (
    not normalized_stack_root
    or not os.path.isabs(normalized_stack_root)
    or normalized_stack_root != stack_root
    or normalized_stack_root == os.sep
  ):
    raise Fail("ResourceManager host files require a valid BIGTOP stack root")
  if (
    not normalized
    or path != normalized
    or os.path.dirname(normalized) != normalized_config_dir
  ):
    raise Fail(f"{label} must be a direct child of {normalized_config_dir}")

  current = os.sep
  for part in [part for part in normalized_config_dir.split(os.sep) if part]:
    current = os.path.join(current, part)
    if not sudo.path_lexists(current):
      raise Fail(f"{label} parent directory does not exist: {current}")
    metadata = sudo.lstat(current)
    if sudo.path_islink(current):
      if current != normalized_config_dir or metadata.st_uid != 0:
        raise Fail(f"{label} parent must not contain an untrusted symlink: {current}")
      continue
    if not stat.S_ISDIR(metadata.st_mode):
      raise Fail(f"{label} parent must be a directory: {current}")
    if metadata.st_uid != 0 or metadata.st_mode & 0o022:
      raise Fail(f"{label} parent must be root-owned and non-writable: {current}")

  resolved_config_dir = os.path.realpath(normalized_config_dir)
  managed_config_dir = os.path.join(
    normalized_stack_root, "current", "hadoop-client", "conf"
  )
  managed_resolved_config_dir = os.path.realpath(managed_config_dir)
  if resolved_config_dir not in (
    normalized_config_dir,
    managed_resolved_config_dir,
  ):
    raise Fail(
      f"{label} parent must resolve to the BIGTOP-managed Hadoop config directory"
    )
  current = os.sep
  for part in [part for part in resolved_config_dir.split(os.sep) if part]:
    current = os.path.join(current, part)
    if not sudo.path_lexists(current):
      raise Fail(f"{label} resolved parent directory does not exist: {current}")
    metadata = sudo.lstat(current)
    if sudo.path_islink(current) or not stat.S_ISDIR(metadata.st_mode):
      raise Fail(f"{label} resolved parent must be a real directory: {current}")
    if metadata.st_uid != 0 or metadata.st_mode & 0o022:
      raise Fail(
        f"{label} resolved parent must be root-owned and non-writable: {current}"
      )

  if sudo.path_lexists(normalized):
    metadata = sudo.lstat(normalized)
    if not stat.S_ISREG(metadata.st_mode):
      raise Fail(f"{label} must be a regular file: {normalized}")
    if metadata.st_uid != 0 or metadata.st_mode & 0o022:
      raise Fail(f"{label} must be root-owned and non-writable: {normalized}")
  return normalized


def _validated_resource_manager_host_files(
  exclude_path, config_dir, stack_root, include_path=None
):
  exclude_file = _validate_root_managed_config_file(
    exclude_path,
    "yarn.resourcemanager.nodes.exclude-path",
    config_dir,
    stack_root,
  )
  include_file = None
  if include_path:
    include_file = _validate_root_managed_config_file(
      include_path,
      "yarn.resourcemanager.nodes.include-path",
      config_dir,
      stack_root,
    )
  return exclude_file, include_file


def _configured_node_manager_directories(paths, label):
  return [
    _validate_local_service_directory(path, label)
    for path in paths
    if isinstance(path, str) and path.strip()
  ]


def _trusted_nm_security_marker(marker_file, label):
  marker_dir = os.path.dirname(marker_file)
  current = os.sep
  for path_part in [part for part in marker_dir.split(os.sep) if part]:
    current = os.path.join(current, path_part)
    if not sudo.path_lexists(current):
      continue
    metadata = sudo.lstat(current)
    if not stat.S_ISDIR(metadata.st_mode):
      raise Fail(f"{label} parent is not a directory: {current}")
    if metadata.st_uid != 0 or metadata.st_mode & 0o022:
      raise Fail(
        f"{label} parent must be a root-owned, non-writable directory: {current}"
      )

  if not sudo.path_lexists(marker_file):
    return False
  metadata = sudo.lstat(marker_file)
  if not stat.S_ISREG(metadata.st_mode):
    raise Fail(f"{label} must be a regular file")
  if metadata.st_uid != 0 or metadata.st_mode & 0o022:
    raise Fail(f"{label} must be a root-owned, non-writable regular file")
  return True


def _current_nm_security_state(marker_dir, marker_file, legacy_marker_file):
  expected_dir = "/var/lib/ambari-agent/data/yarn"
  expected_file = os.path.join(expected_dir, "nm_security_enabled")
  expected_legacy_file = "/var/lib/hadoop-yarn/nm_security_enabled"
  if marker_dir != expected_dir or marker_file != expected_file:
    raise Fail("NodeManager security marker must use the Ambari Agent state directory")
  if legacy_marker_file != expected_legacy_file:
    raise Fail("Legacy NodeManager security marker path is invalid")

  if _trusted_nm_security_marker(marker_file, "NodeManager security marker"):
    return True, False
  if _trusted_nm_security_marker(
    legacy_marker_file, "Legacy NodeManager security marker"
  ):
    return True, True
  return False, False


def _open_directory(path, dir_fd=None):
  flags = os.O_RDONLY | os.O_DIRECTORY | os.O_NOFOLLOW
  if hasattr(os, "O_CLOEXEC"):
    flags |= os.O_CLOEXEC
  return os.open(path, flags, dir_fd=dir_fd)


def _remove_directory_contents(directory_fd, filesystem_device):
  for entry in os.listdir(directory_fd):
    metadata = os.stat(entry, dir_fd=directory_fd, follow_symlinks=False)
    if stat.S_ISDIR(metadata.st_mode):
      if metadata.st_dev != filesystem_device:
        raise Fail(f"Refusing to cross a mounted directory while deleting {entry!r}")
      child_fd = _open_directory(entry, dir_fd=directory_fd)
      try:
        pinned = os.fstat(child_fd)
        if (pinned.st_dev, pinned.st_ino) != (metadata.st_dev, metadata.st_ino):
          raise Fail(f"Directory changed while preparing to delete {entry!r}")
        _remove_directory_contents(child_fd, filesystem_device)
      finally:
        os.close(child_fd)
      current = os.stat(entry, dir_fd=directory_fd, follow_symlinks=False)
      if (current.st_dev, current.st_ino) != (metadata.st_dev, metadata.st_ino):
        raise Fail(f"Directory changed while deleting {entry!r}")
      os.rmdir(entry, dir_fd=directory_fd)
    else:
      os.unlink(entry, dir_fd=directory_fd)


def _remove_local_tree_safely(path, label, expected_user):
  normalized = _validate_local_service_directory(path, label)
  validate_path_segment(expected_user, f"{label} owner")
  try:
    expected_uid = pwd.getpwnam(expected_user).pw_uid
  except KeyError as error:
    raise Fail(f"{label} owner does not exist: {expected_user}") from error
  parts = [part for part in normalized.split(os.sep) if part]
  parent_fd = _open_directory(os.sep)
  try:
    for part in parts[:-1]:
      try:
        next_fd = _open_directory(part, dir_fd=parent_fd)
      except FileNotFoundError:
        return
      os.close(parent_fd)
      parent_fd = next_fd
    leaf = parts[-1]
    try:
      metadata = os.stat(leaf, dir_fd=parent_fd, follow_symlinks=False)
    except FileNotFoundError:
      return
    if not stat.S_ISDIR(metadata.st_mode):
      raise Fail(f"{label} must be a directory and must not be a symbolic link")
    if metadata.st_uid != expected_uid:
      raise Fail(f"{label} must be owned by {expected_user}: {normalized}")
    directory_fd = _open_directory(leaf, dir_fd=parent_fd)
    try:
      pinned = os.fstat(directory_fd)
      if (
        (pinned.st_dev, pinned.st_ino) != (metadata.st_dev, metadata.st_ino)
        or pinned.st_uid != expected_uid
      ):
        raise Fail(f"{label} changed while preparing cleanup")
      _remove_directory_contents(directory_fd, metadata.st_dev)
    finally:
      os.close(directory_fd)
    current = os.stat(leaf, dir_fd=parent_fd, follow_symlinks=False)
    if (
      (current.st_dev, current.st_ino) != (metadata.st_dev, metadata.st_ino)
      or current.st_uid != expected_uid
    ):
      raise Fail(f"{label} changed while cleaning it")
    os.rmdir(leaf, dir_fd=parent_fd)
  finally:
    os.close(parent_fd)


def _create_ats_hdfs_directory(params, path, mode):
  normalized = validate_hdfs_directory(path, "ATS entity-group FS store path")
  parent_path = os.path.dirname(normalized)
  hdfs_tmp_dir = validate_hdfs_directory(
    params.hdfs_tmp_dir, "hdfs_tmp_dir", allow_top_level=True
  )
  if parent_path == hdfs_tmp_dir:
    params.HdfsResource(
      hdfs_tmp_dir,
      type="directory",
      action="create_on_execute",
      owner=params.hdfs_user,
      group=params.user_group,
      mode=0o1777,
    )
  else:
    params.HdfsResource(
      parent_path,
      type="directory",
      action="create_on_execute",
      owner=params.yarn_user,
      group=params.user_group,
      mode=0o755,
    )
  params.HdfsResource(
    normalized,
    type="directory",
    action="create_on_execute",
    owner=params.yarn_user,
    group=params.user_group,
    mode=mode,
  )


@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def yarn(name=None, config_dir=None):
  """
  :param name: Component name, apptimelineserver, nodemanager, resourcemanager, or None (defaults for client)
  :param config_dir: Which config directory to write configs to, which could be different during rolling upgrade.
  """
  import params

  install_lzo_if_needed()

  if config_dir is None:
    config_dir = params.hadoop_conf_dir
  manages_embedded_hbase = not params.use_external_hbase

  if params.yarn_nodemanager_recovery_dir:
    _daemon_owned_directory(
      InlineTemplate(params.yarn_nodemanager_recovery_dir).get_content(),
      "yarn.nodemanager.recovery.dir",
      params.yarn_user,
      params.user_group,
    )

  for path, label, allowed_roots in (
    (params.yarn_pid_dir_prefix, "yarn_pid_dir_prefix", ("/run", "/var/run")),
    (params.yarn_log_dir_prefix, "yarn_log_dir_prefix", ("/var/log",)),
    (params.mapred_pid_dir_prefix, "mapred_pid_dir_prefix", ("/run", "/var/run")),
    (params.mapred_log_dir_prefix, "mapred_log_dir_prefix", ("/var/log",)),
  ):
    _root_owned_directory(
      path, label, allowed_roots, group=params.user_group
    )
  privileged_registry_pid_dir = _root_owned_directory(
    os.path.join(params.yarn_pid_dir_prefix, "root"),
    "privileged Registry DNS PID directory",
    (params.yarn_pid_dir_prefix,),
    group=params.user_group,
    mode=0o750,
  )
  for path, label in (
    (params.yarn_pid_dir, "yarn_pid_dir"),
    (params.yarn_log_dir, "yarn_log_dir"),
  ):
    _daemon_owned_directory(
      path, label, params.yarn_user, params.user_group
    )
  for path, label in (
    (params.mapred_pid_dir, "mapred_pid_dir"),
    (params.mapred_log_dir, "mapred_log_dir"),
  ):
    _daemon_owned_directory(
      path, label, params.mapred_user, params.user_group
    )
  if manages_embedded_hbase:
    yarn_hbase_conf_dir = _validate_local_service_directory(
      params.yarn_hbase_conf_dir, "yarn_hbase_conf_dir"
    )
    Directory(
      yarn_hbase_conf_dir,
      owner="root",
      group=params.user_group,
      create_parents=True,
      cd_access="a",
    )
  # Some of these function calls depend on the directories above being created first.
  if name == "resourcemanager":
    setup_resourcemanager()
  elif name == "nodemanager":
    setup_nodemanager()
  elif name == "apptimelineserver":
    setup_ats()
  elif name == "historyserver":
    setup_historyserver()
  elif name == "apptimelinereader":
    if not params.use_external_hbase and not params.is_hbase_system_service_launch:
      setup_atsv2_hbase_directories()
      setup_atsv2_hbase_files()

  XmlConfig(
    "core-site.xml",
    conf_dir=config_dir,
    configurations=params.config["configurations"]["core-site"],
    configuration_attributes=params.config["configurationAttributes"]["core-site"],
    owner="root",
    group=params.user_group,
    mode=0o644,
  )

  # During RU, Core Masters and Slaves need hdfs-site.xml
  # TODO, instead of specifying individual configs, which is susceptible to breaking when new configs are added,
  # RU should rely on all available in <stack-root>/<version>/hadoop/conf
  XmlConfig(
    "hdfs-site.xml",
    conf_dir=config_dir,
    configurations=params.config["configurations"]["hdfs-site"],
    configuration_attributes=params.config["configurationAttributes"]["hdfs-site"],
    owner="root",
    group=params.user_group,
    mode=0o644,
  )

  XmlConfig(
    "mapred-site.xml",
    conf_dir=config_dir,
    configurations=params.config["configurations"]["mapred-site"],
    configuration_attributes=params.config["configurationAttributes"]["mapred-site"],
    owner="root",
    group=params.user_group,
    mode=0o644,
  )

  configs = {}
  configs.update(params.config["configurations"]["yarn-site"])
  configs["hadoop.registry.dns.bind-port"] = params.config["configurations"][
    "yarn-env"
  ]["registry.dns.bind-port"]
  if not params.use_external_hbase and params.is_hbase_system_service_launch:
    configs["yarn.service.framework.path"] = params.yarn_service_framework_path
  XmlConfig(
    "yarn-site.xml",
    conf_dir=config_dir,
    configurations=configs,
    configuration_attributes=params.config["configurationAttributes"]["yarn-site"],
    owner="root",
    group=params.user_group,
    mode=0o644,
  )

  XmlConfig(
    "capacity-scheduler.xml",
    conf_dir=config_dir,
    configurations=params.config["configurations"]["capacity-scheduler"],
    configuration_attributes=params.config["configurationAttributes"][
      "capacity-scheduler"
    ],
    owner="root",
    group=params.user_group,
    mode=0o644,
  )

  if manages_embedded_hbase and not isinstance(
    params.hbase_site_conf, UnknownConfiguration
  ):
    XmlConfig(
      "hbase-site.xml",
      conf_dir=params.yarn_hbase_conf_dir,
      configurations=params.hbase_site_conf,
      configuration_attributes=params.hbase_site_attributes,
      owner="root",
      group=params.user_group,
      mode=0o644,
    )

  XmlConfig(
    "resource-types.xml",
    conf_dir=config_dir,
    configurations=params.config["configurations"]["resource-types"],
    configuration_attributes=params.config["configurationAttributes"]["resource-types"],
    owner="root",
    group=params.user_group,
    mode=0o644,
  )
  File(
    format("{limits_conf_dir}/yarn.conf"),
    owner="root",
    group="root",
    mode=0o644,
    content=Template("yarn.conf.j2"),
  )

  File(
    format("{limits_conf_dir}/mapreduce.conf"),
    owner="root",
    group="root",
    mode=0o644,
    content=Template("mapreduce.conf.j2"),
  )

  File(
    os.path.join(config_dir, "yarn-env.sh"),
    owner="root",
    group=params.user_group,
    mode=0o644,
    content=InlineTemplate(params.yarn_env_sh_template),
  )

  File(
    format("{yarn_container_bin}/container-executor"),
    owner="root",
    group=params.yarn_executor_container_group,
    mode=params.container_executor_mode,
  )

  File(
    os.path.join(config_dir, "container-executor.cfg"),
    owner="root",
    group="root",
    mode=0o400,
    content=InlineTemplate(params.container_executor_cfg_template),
  )

  File(
    os.path.join(config_dir, "mapred-env.sh"),
    owner="root",
    group=params.user_group,
    mode=0o644,
    content=InlineTemplate(params.mapred_env_sh_template),
  )

  if params.security_enabled:
    File(
      os.path.join(config_dir, "yarn_jaas.conf"),
      owner="root",
      group=params.user_group,
      mode=0o640,
      content=Template("yarn_jaas.conf.j2"),
    )
    if params.has_ats or params.has_atsv2:
      File(
        os.path.join(config_dir, "yarn_ats_jaas.conf"),
        owner="root",
        group=params.user_group,
        mode=0o640,
        content=Template("yarn_ats_jaas.conf.j2"),
      )

    if params.has_registry_dns:
      File(
        os.path.join(config_dir, "yarn_registry_dns_jaas.conf"),
        owner="root",
        group=params.user_group,
        mode=0o640,
        content=Template("yarn_registry_dns_jaas.conf.j2"),
      )
    File(
      os.path.join(config_dir, "yarn_nm_jaas.conf"),
      owner="root",
      group=params.user_group,
      mode=0o640,
      content=Template("yarn_nm_jaas.conf.j2"),
    )
    if params.has_hs:
      File(
        os.path.join(config_dir, "mapred_jaas.conf"),
        owner="root",
        group=params.user_group,
        mode=0o640,
        content=Template("mapred_jaas.conf.j2"),
      )
  if "ssl-client" in params.config["configurations"]:
    XmlConfig(
      "ssl-client.xml",
      conf_dir=config_dir,
      configurations=params.config["configurations"]["ssl-client"],
      configuration_attributes=params.config["configurationAttributes"]["ssl-client"],
      owner="root",
      group=params.user_group,
      mode=0o644,
    )

    Directory(
      params.hadoop_conf_secure_dir,
      create_parents=True,
      owner="root",
      group=params.user_group,
      mode=0o755,
      cd_access="a",
    )

    XmlConfig(
      "ssl-client.xml",
      conf_dir=params.hadoop_conf_secure_dir,
      configurations=params.config["configurations"]["ssl-client"],
      configuration_attributes=params.config["configurationAttributes"]["ssl-client"],
      owner="root",
      group=params.user_group,
      mode=0o644,
    )

  if "ssl-server" in params.config["configurations"]:
    XmlConfig(
      "ssl-server.xml",
      conf_dir=config_dir,
      configurations=params.config["configurations"]["ssl-server"],
      configuration_attributes=params.config["configurationAttributes"]["ssl-server"],
      owner="root",
      group=params.user_group,
      mode=0o644,
    )
  if os.path.exists(os.path.join(config_dir, "fair-scheduler.xml")):
    File(
      os.path.join(config_dir, "fair-scheduler.xml"),
      owner="root",
      group=params.user_group,
      mode=0o644,
    )

  if os.path.exists(os.path.join(config_dir, "ssl-client.xml.example")):
    File(
      os.path.join(config_dir, "ssl-client.xml.example"),
      owner="root",
      group=params.user_group,
      mode=0o644,
    )

  if os.path.exists(os.path.join(config_dir, "ssl-server.xml.example")):
    File(
      os.path.join(config_dir, "ssl-server.xml.example"),
      owner="root",
      group=params.user_group,
      mode=0o644,
    )

  setup_atsv2_backend(name, config_dir)


def setup_historyserver():
  import params

  if params.yarn_log_aggregation_enabled:
    yarn_nm_app_log_dir = validate_hdfs_directory(
      params.yarn_nm_app_log_dir,
      "yarn.nodemanager.remote-app-log-dir",
      allow_top_level=True,
      allow_uri=True,
    )
    params.HdfsResource(
      yarn_nm_app_log_dir,
      action="create_on_execute",
      type="directory",
      owner=params.yarn_user,
      group=params.user_group,
      mode=0o1777,
    )

  intermediate_done_dir = validate_hdfs_directory(
    params.mapreduce_jobhistory_intermediate_done_dir,
    "mapreduce.jobhistory.intermediate-done-dir",
    allow_uri=True,
  )
  done_dir = validate_hdfs_directory(
    params.mapreduce_jobhistory_done_dir,
    "mapreduce.jobhistory.done-dir",
    allow_uri=True,
  )
  params.HdfsResource(
    intermediate_done_dir,
    type="directory",
    action="create_on_execute",
    owner=params.mapred_user,
    group=params.user_group,
    mode=0o1777,
  )
  params.HdfsResource(
    done_dir,
    type="directory",
    action="create_on_execute",
    owner=params.mapred_user,
    group=params.user_group,
    mode=0o770,
  )
  params.HdfsResource(None, action="execute")
  _daemon_owned_directory(
    params.jhs_leveldb_state_store_dir,
    "mapreduce.jobhistory.recovery.store.leveldb.path",
    params.mapred_user,
    params.user_group,
  )


def setup_nodemanager():
  import params

  current_nm_security_state, migrated_legacy_marker = _current_nm_security_state(
    params.nm_security_marker_dir,
    params.nm_security_marker,
    params.legacy_nm_security_marker,
  )
  toggle_nm_security = current_nm_security_state != params.security_enabled

  # First start after enabling/disabling security
  if toggle_nm_security:
    cleanup_dirs = _configured_node_manager_directories(
      params.nm_local_dirs_list, "yarn.nodemanager.local-dirs"
    ) + _configured_node_manager_directories(
      params.nm_log_dirs_list, "yarn.nodemanager.log-dirs"
    )
    for cleanup_dir in cleanup_dirs:
      _remove_local_tree_safely(
        cleanup_dir,
        "NodeManager local or log directory",
        params.yarn_user,
      )

    # If yarn.nodemanager.recovery.dir exists, remove this dir
    if params.yarn_nodemanager_recovery_dir:
      recovery_dir = _validate_local_service_directory(
        InlineTemplate(params.yarn_nodemanager_recovery_dir).get_content(),
        "yarn.nodemanager.recovery.dir",
      )
      _remove_local_tree_safely(
        recovery_dir,
        "yarn.nodemanager.recovery.dir",
        params.yarn_user,
      )

    if not params.security_enabled:
      File(params.nm_security_marker, action="delete")

  Directory(
    params.nm_security_marker_dir,
    owner="root",
    group=params.user_group,
    mode=0o750,
    create_parents=True,
  )
  if params.security_enabled:
    File(
      params.nm_security_marker,
      owner="root",
      group=params.user_group,
      mode=0o640,
      content="NodeManager security mode enabled",
    )
  if migrated_legacy_marker:
    File(params.legacy_nm_security_marker, action="delete")

  if not params.security_enabled or toggle_nm_security:
    nm_log_dirs = _configured_node_manager_directories(
      params.nm_log_dirs_list, "yarn.nodemanager.log-dirs"
    )
    nm_local_dirs = _configured_node_manager_directories(
      params.nm_local_dirs_list, "yarn.nodemanager.local-dirs"
    )
    # handle_mounted_dirs ensures that we don't create dirs which are temporary unavailable (unmounted), and intended to reside on a different mount.
    nm_log_dir_to_mount_file_content = handle_mounted_dirs(
      create_log_dir,
      ",".join(nm_log_dirs),
      params.nm_log_dir_to_mount_file,
      params,
    )
    # create a history file used by handle_mounted_dirs
    File(
      params.nm_log_dir_to_mount_file,
      owner="root",
      group=params.user_group,
      mode=0o640,
      content=nm_log_dir_to_mount_file_content,
    )
    nm_local_dir_to_mount_file_content = handle_mounted_dirs(
      create_local_dir,
      ",".join(nm_local_dirs),
      params.nm_local_dir_to_mount_file,
      params,
    )
    File(
      params.nm_local_dir_to_mount_file,
      owner="root",
      group=params.user_group,
      mode=0o644,
      content=nm_local_dir_to_mount_file_content,
    )


def setup_resourcemanager():
  import params

  exclude_file_path, include_file_path = _validated_resource_manager_host_files(
    params.exclude_file_path,
    params.hadoop_conf_dir,
    params.stack_root,
    params.include_file_path if params.include_hosts else None,
  )
  File(
    exclude_file_path,
    content=Template("exclude_hosts_list.j2"),
    owner="root",
    group=params.user_group,
    mode=0o644,
  )
  if params.include_hosts:
    File(
      include_file_path,
      content=Template("include_hosts_list.j2"),
      owner="root",
      group=params.user_group,
      mode=0o644,
    )
  if params.node_label_enable and params.node_labels_dir:
    node_labels_dir = validate_hdfs_directory(
      params.node_labels_dir, "yarn.node-labels.fs-store.root-dir"
    )
    params.HdfsResource(
      node_labels_dir,
      type="directory",
      action="create_on_execute",
      owner=params.yarn_user,
      group=params.user_group,
      mode=0o700,
    )
    params.HdfsResource(None, action="execute")


def setup_ats():
  import params

  _daemon_owned_directory(
    params.ats_leveldb_dir,
    "yarn.timeline-service.leveldb-timeline-store.path",
    params.yarn_user,
    params.user_group,
  )

  # if stack support application timeline-service state store property (timeline_state_store stack feature)
  if params.stack_supports_timeline_state_store:
    _daemon_owned_directory(
      params.ats_leveldb_state_store_dir,
      "yarn.timeline-service.leveldb-state-store.path",
      params.yarn_user,
      params.user_group,
    )
  # app timeline server 1.5 directories
  if params.entity_groupfs_store_dir:
    _create_ats_hdfs_directory(
      params,
      params.entity_groupfs_store_dir,
      params.entity_groupfs_store_dir_mode,
    )
  if params.entity_groupfs_active_dir:
    _create_ats_hdfs_directory(
      params,
      params.entity_groupfs_active_dir,
      params.entity_groupfs_active_dir_mode,
    )
  params.HdfsResource(None, action="execute")


def create_log_dir(dir_name):
  import params

  _daemon_owned_directory(
    dir_name,
    "yarn.nodemanager.log-dirs",
    params.yarn_user,
    params.user_group,
  )


def create_local_dir(dir_name):
  import params

  _daemon_owned_directory(
    dir_name,
    "yarn.nodemanager.local-dirs",
    params.yarn_user,
    params.user_group,
  )


def setup_atsv2_backend(name=None, config_dir=None):
  import params

  if name == "apptimelinereader" and not params.atsv2_backend_enabled:
    raise Fail("TIMELINE_READER requires an enabled YARN Timeline Service v2")
  if not params.atsv2_backend_enabled:
    return

  Logger.info(
    f"setup_atsv2_backend name:{name} {params.use_external_hbase} {params.hbase_within_cluster} {params.is_hbase_system_service_launch}"
  )
  if (
    name == "apptimelinereader"
    and params.use_external_hbase
    and params.hbase_within_cluster
  ):
    createTables()

  if not params.use_external_hbase and params.is_hbase_system_service_launch:
    if name == "resourcemanager":
      setup_system_services(config_dir)
    elif name == "nodemanager":
      setup_atsv2_hbase_files()


def setup_atsv2_hbase_files():
  import params

  if "yarn-hbase-policy" in params.config["configurations"]:
    XmlConfig(
      "hbase-policy.xml",
      conf_dir=params.yarn_hbase_conf_dir,
      configurations=params.config["configurations"]["yarn-hbase-policy"],
      configuration_attributes=params.config["configurationAttributes"][
        "yarn-hbase-policy"
      ],
      owner="root",
      group=params.user_group,
      mode=0o644,
    )

  File(
    os.path.join(params.yarn_hbase_conf_dir, "hbase-env.sh"),
    owner="root",
    group=params.user_group,
    mode=0o644,
    content=InlineTemplate(params.yarn_hbase_env_sh_template),
  )

  File(
    params.yarn_hbase_grant_permissions_file,
    owner="root",
    group=params.user_group,
    mode=0o640,
    content=Template("yarn_hbase_grant_permissions.j2"),
  )

  if params.yarn_hbase_log4j_props is not None:
    File(
      format("{yarn_hbase_conf_dir}/log4j.properties"),
      mode=0o644,
      group=params.user_group,
      owner="root",
      content=InlineTemplate(params.yarn_hbase_log4j_props),
    )
  elif os.path.exists(format("{yarn_hbase_conf_dir}/log4j.properties")):
    File(
      format("{yarn_hbase_conf_dir}/log4j.properties"),
      mode=0o644,
      group=params.user_group,
      owner="root",
    )
  if params.security_enabled:
    File(
      os.path.join(params.yarn_hbase_conf_dir, "yarn_hbase_master_jaas.conf"),
      owner="root",
      group=params.user_group,
      mode=0o640,
      content=Template("yarn_hbase_master_jaas.conf.j2"),
    )
    File(
      os.path.join(params.yarn_hbase_conf_dir, "yarn_hbase_regionserver_jaas.conf"),
      owner="root",
      group=params.user_group,
      mode=0o640,
      content=Template("yarn_hbase_regionserver_jaas.conf.j2"),
    )
  # Metrics properties
  if params.has_metric_collector:
    File(
      os.path.join(params.yarn_hbase_conf_dir, "hadoop-metrics2-hbase.properties"),
      owner="root",
      group=params.user_group,
      mode=0o640,
      content=Template("hadoop-metrics2-hbase.properties.j2"),
    )


def setup_atsv2_hbase_directories():
  import params

  _root_owned_directory(
    params.yarn_hbase_pid_dir_prefix,
    "yarn_hbase_pid_dir_prefix",
    ("/run", "/var/run"),
  )
  for path, label in (
    (params.yarn_hbase_pid_dir, "yarn_hbase_pid_dir"),
    (params.yarn_hbase_log_dir, "yarn_hbase_log_dir"),
  ):
    _daemon_owned_directory(
      path, label, params.yarn_hbase_user, params.user_group
    )

  yarn_hbase_tmp_dir = params.yarn_hbase_tmp_dir.replace(
    "${user.name}", params.yarn_hbase_user
  )
  _daemon_owned_directory(
    yarn_hbase_tmp_dir,
    "hbase.tmp.dir",
    params.yarn_hbase_user,
    params.user_group,
    mode=0o750,
  )


def setup_system_services(config_dir=None):
  import params

  validate_path_segment(params.version, "stack version")
  yarn_hbase_user = validate_path_segment(params.yarn_hbase_user, "ATS HBase user")
  launch_mode = validate_path_segment(
    params.yarn_system_service_launch_mode, "YARN system service launch mode"
  )
  if launch_mode not in ("sync", "async"):
    raise Fail("YARN system service launch mode must be sync or async")
  if params.rm_ha_id is not None:
    validate_path_segment(params.rm_ha_id, "ResourceManager HA ID")
  if params.rm_ha_enabled and params.rm_ha_id is None:
    raise Fail(
      "The local ResourceManager must match exactly one HA ID before publishing "
      "the YARN system service"
    )

  system_service_dir = validate_hdfs_directory(
    params.yarn_system_service_dir,
    "yarn.service.system-service.dir",
    allow_top_level=True,
    allow_uri=True,
  )
  user_dir = validate_hdfs_directory(
    join_hdfs_directory(system_service_dir, launch_mode, yarn_hbase_user),
    "YARN system service user directory",
    allow_uri=True,
  )
  yarn_hbase_user_home = validate_hdfs_directory(
    params.yarn_hbase_user_home, "ATS HBase user home"
  )
  yarn_hbase_user_version_home = validate_hdfs_directory(
    params.yarn_hbase_user_version_home, "ATS HBase version home"
  )
  yarn_hbase_hdfs_root_dir = validate_hdfs_directory(
    params.yarn_hbase_hdfs_root_dir,
    "yarn-hbase-site/hbase.rootdir",
    allow_uri=True,
  )
  yarn_service_app_hdfs_path = validate_hdfs_directory(
    params.yarn_service_app_hdfs_path, "YARN service application HDFS path"
  )
  yarn_hbase_app_hdfs_path = validate_hdfs_directory(
    params.yarn_hbase_app_hdfs_path, "ATS HBase application HDFS path"
  )
  if params.rm_ha_enabled:
    expected_suffix = f"/{params.rm_ha_id}"
    if not yarn_hbase_app_hdfs_path.rstrip("/").endswith(expected_suffix):
      raise Fail(
        "HA HBase artifacts must use a path isolated by the local "
        "ResourceManager ID"
      )
  expected_service_path = f"/bigtop/apps/{params.version}/yarn"
  if yarn_service_app_hdfs_path != expected_service_path:
    raise Fail(
      "The YARN fast-launch framework path must be stable across all roles"
    )

  setup_atsv2_hbase_files()
  if params.security_enabled:
    File(
      os.path.join(params.yarn_hbase_conf_dir, "hbase.yarnfile"),
      owner="root",
      group=params.user_group,
      mode=0o640,
      content=Template("yarn_hbase_secure.yarnfile.j2"),
    )
  else:
    File(
      os.path.join(params.yarn_hbase_conf_dir, "hbase.yarnfile"),
      owner="root",
      group=params.user_group,
      mode=0o640,
      content=Template("yarn_hbase_unsecure.yarnfile.j2"),
    )

  params.HdfsResource(
    user_dir,
    type="directory",
    action="create_on_execute",
    owner=params.yarn_user,
    group=params.user_group,
  )
  params.HdfsResource(
    yarn_hbase_user_home,
    type="directory",
    action="create_on_execute",
    owner=params.yarn_hbase_user,
    group=params.user_group,
    mode=0o770,
  )
  params.HdfsResource(
    yarn_hbase_user_version_home,
    type="directory",
    action="create_on_execute",
    owner=params.yarn_hbase_user,
    group=params.user_group,
    mode=0o770,
  )
  params.HdfsResource(
    posixpath.join(yarn_hbase_user_version_home, "core-site.xml"),
    type="file",
    action="create_on_execute",
    source=format("{config_dir}/core-site.xml"),
    owner=params.yarn_hbase_user,
    group=params.user_group,
  )
  params.HdfsResource(
    posixpath.join(yarn_hbase_user_version_home, "hbase-site.xml"),
    type="file",
    action="create_on_execute",
    source=format("{yarn_hbase_conf_dir}/hbase-site.xml"),
    owner=params.yarn_hbase_user,
    group=params.user_group,
  )
  params.HdfsResource(
    posixpath.join(yarn_hbase_user_version_home, "hbase-policy.xml"),
    type="file",
    action="create_on_execute",
    source=format("{yarn_hbase_conf_dir}/hbase-policy.xml"),
    owner=params.yarn_hbase_user,
    group=params.user_group,
  )
  if params.security_enabled:
    params.HdfsResource(
      posixpath.join(yarn_hbase_user_version_home, "hbase_grant_permissions.rb"),
      type="file",
      action="create_on_execute",
      source=params.yarn_hbase_grant_permissions_file,
      owner=params.yarn_hbase_user,
      group=params.user_group,
      mode=0o600,
    )
  params.HdfsResource(
    posixpath.join(yarn_hbase_user_version_home, "log4j.properties"),
    type="file",
    action="create_on_execute",
    source=format("{yarn_hbase_conf_dir}/log4j.properties"),
    owner=params.yarn_hbase_user,
    group=params.user_group,
  )
  if params.has_metric_collector:
    params.HdfsResource(
      posixpath.join(
        yarn_hbase_user_version_home, "hadoop-metrics2-hbase.properties"
      ),
      type="file",
      action="create_on_execute",
      source=format("{yarn_hbase_conf_dir}/hadoop-metrics2-hbase.properties"),
      owner=params.yarn_hbase_user,
      group=params.user_group,
    )
  params.HdfsResource(
    yarn_hbase_hdfs_root_dir,
    type="directory",
    action="create_on_execute",
    owner=params.yarn_hbase_user,
  )
  params.HdfsResource(
    yarn_service_app_hdfs_path,
    type="directory",
    action="create_on_execute",
    owner=params.yarn_user,
    group=params.user_group,
    mode=0o755,
  )

  params.HdfsResource(None, action="execute")

  create_hbase_package()
  copy_hbase_package_to_hdfs()

  # Publish the manifest only after every path it references is available.
  params.HdfsResource(
    join_hdfs_directory(user_dir, "hbase.yarnfile"),
    type="file",
    action="create_on_execute",
    source=format("{yarn_hbase_conf_dir}/hbase.yarnfile"),
    owner=params.yarn_user,
    group=params.user_group,
    mode=0o644,
  )
  params.HdfsResource(None, action="execute")

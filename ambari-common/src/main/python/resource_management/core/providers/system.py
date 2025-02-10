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

import re
import os
import time
import pwd
import grp
from resource_management.core import shell
from resource_management.core import sudo
from resource_management.core.base import Fail
from resource_management.core import ExecuteTimeoutException
from resource_management.core.providers import Provider
from resource_management.core.logger import Logger


def assert_not_safemode_folder(path, safemode_folders):
  if os.path.abspath(path) in safemode_folders:
    raise Fail(
      (
        "Not performing recursive operation ('recursive_ownership' or 'recursive_mode_flags') on folder '%s'"
        + " as this can damage the system. Please pass changed safemode_folders parameter to Directory resource if you really intend to do this."
      )
      % (path)
    )


def _ensure_metadata(
  path,
  user,
  group,
  mode=None,
  cd_access=None,
  recursive_ownership=False,
  recursive_mode_flags=None,
  recursion_follow_links=False,
  safemode_folders=[],
):
  user_entity = group_entity = None
  _user_entity = _group_entity = None

  if user or group:
    stat = sudo.stat(path)

  if user:
    try:
      _user_entity = pwd.getpwnam(user)
    except KeyError:
      raise Fail(f"User '{user}' doesn't exist")

    if stat.st_uid != _user_entity.pw_uid:
      user_entity = _user_entity
      Logger.info("Changing owner for %s from %d to %s" % (path, stat.st_uid, user))

  if group:
    try:
      _group_entity = grp.getgrnam(group)
    except KeyError:
      raise Fail(f"Group '{group}' doesn't exist")

    if stat.st_gid != _group_entity.gr_gid:
      group_entity = _group_entity
      Logger.info("Changing group for %s from %d to %s" % (path, stat.st_gid, group))

  if recursive_ownership:
    assert_not_safemode_folder(path, safemode_folders)
    sudo.chown_recursive(path, _user_entity, _group_entity, recursion_follow_links)

  sudo.chown(path, user_entity, group_entity)

  if recursive_mode_flags:
    if not isinstance(recursive_mode_flags, dict):
      raise Fail(
        "'recursion_follow_links' value should be a dictionary with 'f' and(or) 'd' key (for file and directory permission flags)"
      )

    regexp_to_match = f"^({'[ugoa]+[+=-][rwx]+'},)*({'[ugoa]+[+=-][rwx]+'})$"
    for key, flags in recursive_mode_flags.items():
      if key != "d" and key != "f":
        raise Fail(
          "'recursive_mode_flags' with value '%s' has unknown key '%s', only keys 'f' and 'd' are valid"
          % (str(recursive_mode_flags), str(key))
        )

      if not re.match(regexp_to_match, flags):
        raise Fail(
          "'recursive_mode_flags' found '%s', but should value format have the following format: [ugoa...][[+-=][perms...]...]."
          % (str(flags))
        )

    assert_not_safemode_folder(path, safemode_folders)
    sudo.chmod_recursive(path, recursive_mode_flags, recursion_follow_links)

  if mode:
    stat = sudo.stat(path)
    if stat.st_mode != mode:
      Logger.info(
        f"Changing permission for {path} from {stat.st_mode:o} to {mode:o}"
      )
      sudo.chmod(path, mode)

  if cd_access:
    if not re.match("^[ugoa]+$", cd_access):
      raise Fail(f"'cd_acess' value '{cd_access}' is not valid")

    dir_path = re.sub("/+", "/", path)
    while dir_path and dir_path != os.sep:
      if sudo.path_isdir(dir_path):
        sudo.chmod_extended(dir_path, cd_access + "+rx")

      dir_path = os.path.split(dir_path)[0]


class FileProvider(Provider):
  def action_create(self):
    path = self.resource.path

    if sudo.path_isdir(path):
      raise Fail(
        f"Applying {self.resource} failed, directory with name {path} exists"
      )

    dirname = os.path.dirname(path)
    if not sudo.path_isdir(dirname):
      raise Fail(
        f"Applying {self.resource} failed, parent directory {dirname} doesn't exist"
      )

    write = False
    content = self._get_content()
    if not sudo.path_exists(path):
      write = True
      reason = "it doesn't exist"
    elif self.resource.replace:
      if content is not None:
        old_content = sudo.read_file(path, encoding=self.resource.encoding)
        if content != old_content:
          write = True
          reason = "contents don't match"
          if self.resource.backup:
            self.resource.env.backup_file(path)

    owner = self.resource.owner or "root"
    group = self.resource.group or "root"

    if write:
      Logger.info(f"Writing {self.resource} because {reason}")

      def on_file_created(filename):
        _ensure_metadata(
          filename,
          owner,
          group,
          mode=self.resource.mode,
          cd_access=self.resource.cd_access,
        )
        Logger.info(f"Moving {filename} to {path}")

      sudo.create_file(
        path, content, encoding=self.resource.encoding, on_file_created=on_file_created
      )
    else:
      _ensure_metadata(
        path, owner, group, mode=self.resource.mode, cd_access=self.resource.cd_access
      )

  def action_delete(self):
    path = self.resource.path

    if sudo.path_isdir(path):
      raise Fail(
        f"Applying {self.resource} failed, {path} is directory not file!"
      )

    if sudo.path_exists(path):
      Logger.info(f"Deleting {self.resource}")
      sudo.unlink(path)

  def _get_content(self):
    content = self.resource.content
    if content is None:
      return None
    elif isinstance(content, str) or isinstance(content, bytes):
      return content
    elif hasattr(content, "__call__"):
      return content()
    raise Fail(f"Unknown source type for {self}: {content!r}")


class DirectoryProvider(Provider):
  def action_create(self):
    path = self.resource.path

    if not sudo.path_exists(path):
      Logger.info(f"Creating directory {self.resource} since it doesn't exist.")

      # dead links should be followed, else we gonna have failures on trying to create directories on top of them.
      if self.resource.follow:
        # Follow symlink until the tail.
        followed_links = set()
        while sudo.path_islink(path):
          if path in followed_links:
            raise Fail(
              "Applying %s failed, looped symbolic links found while resolving %s"
              % (self.resource, path)
            )
          followed_links.add(path)
          prev_path = path
          path = sudo.readlink(path)

          if not os.path.isabs(path):
            path = os.path.join(os.path.dirname(prev_path), path)

        if path != self.resource.path:
          Logger.info(
            f"Following the link {self.resource.path} to {path} to create the directory"
          )

      if self.resource.create_parents:
        sudo.makedirs(path, self.resource.mode or 0o755)
      else:
        dirname = os.path.dirname(path)
        if not sudo.path_isdir(dirname):
          raise Fail(
            "Applying %s failed, parent directory %s doesn't exist"
            % (self.resource, dirname)
          )

        try:
          sudo.makedir(path, self.resource.mode or 0o755)
        except Exception as ex:
          # race condition (somebody created the file before us)
          if "File exists" in str(ex):
            sudo.makedirs(path, self.resource.mode or 0o755)
          else:
            raise

    if not sudo.path_isdir(path):
      raise Fail(f"Applying {self.resource} failed, file {path} already exists")

    _ensure_metadata(
      path,
      self.resource.owner,
      self.resource.group,
      mode=self.resource.mode,
      cd_access=self.resource.cd_access,
      recursive_ownership=self.resource.recursive_ownership,
      recursive_mode_flags=self.resource.recursive_mode_flags,
      recursion_follow_links=self.resource.recursion_follow_links,
      safemode_folders=self.resource.safemode_folders,
    )

  def action_delete(self):
    path = self.resource.path
    if sudo.path_exists(path):
      if not sudo.path_isdir(path):
        raise Fail(f"Applying {self.resource} failed, {path} is not a directory")

      Logger.info(f"Removing directory {self.resource} and all its content")
      sudo.rmtree(path)


class LinkProvider(Provider):
  def action_create(self):
    path = self.resource.path

    if sudo.path_lexists(path):
      oldpath = os.path.realpath(path)
      if oldpath == self.resource.to:
        return
      if not sudo.path_islink(path):
        raise Fail(
          "%s trying to create a symlink with the same name as an existing file or directory"
          % self.resource
        )
      Logger.info(f"{self.resource} replacing old symlink to {oldpath}")
      sudo.unlink(path)

    if self.resource.hard:
      if not sudo.path_exists(self.resource.to):
        raise Fail(
          "Failed to apply %s, linking to nonexistent location %s"
          % (self.resource, self.resource.to)
        )
      if sudo.path_isdir(self.resource.to):
        raise Fail(
          "Failed to apply %s, cannot create hard link to a directory (%s)"
          % (self.resource, self.resource.to)
        )

      Logger.info(f"Creating hard {self.resource}")
      sudo.link(self.resource.to, path)
    else:
      if not sudo.path_exists(self.resource.to):
        Logger.info(f"Warning: linking to nonexistent location {self.resource.to}")

      Logger.info(f"Creating symbolic {self.resource} to {self.resource.to}")
      sudo.symlink(self.resource.to, path)

  def action_delete(self):
    path = self.resource.path
    if sudo.path_lexists(path):
      Logger.info(f"Deleting {self.resource}")
      sudo.unlink(path)


class ExecuteProvider(Provider):
  def action_run(self):
    if self.resource.creates:
      if sudo.path_exists(self.resource.creates):
        Logger.info(f"Skipping {self.resource} due to creates")
        return

    shell.checked_call(
      self.resource.command,
      logoutput=self.resource.logoutput,
      cwd=self.resource.cwd,
      env=self.resource.environment,
      user=self.resource.user,
      wait_for_finish=self.resource.wait_for_finish,
      timeout=self.resource.timeout,
      on_timeout=self.resource.on_timeout,
      path=self.resource.path,
      sudo=self.resource.sudo,
      timeout_kill_strategy=self.resource.timeout_kill_strategy,
      on_new_line=self.resource.on_new_line,
      stdout=self.resource.stdout,
      stderr=self.resource.stderr,
      tries=self.resource.tries,
      try_sleep=self.resource.try_sleep,
      returns=self.resource.returns,
    )


class ExecuteScriptProvider(Provider):
  def action_run(self):
    from tempfile import NamedTemporaryFile

    Logger.info(f"Running script {self.resource}")
    with NamedTemporaryFile(prefix="resource_management-script", bufsize=0) as tf:
      tf.write(self.resource.code)
      tf.flush()

      _ensure_metadata(tf.name, self.resource.user, self.resource.group)
      shell.call(
        [self.resource.interpreter, tf.name],
        cwd=self.resource.cwd,
        env=self.resource.environment,
        preexec_fn=_preexec_fn(self.resource),
      )

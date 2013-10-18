from __future__ import with_statement

import grp
import os
import pwd
import subprocess
from resource_management.base import Fail
from resource_management.providers import Provider


def _coerce_uid(user):
  try:
    uid = int(user)
  except ValueError:
    uid = pwd.getpwnam(user).pw_uid
  return uid


def _coerce_gid(group):
  try:
    gid = int(group)
  except ValueError:
    gid = grp.getgrnam(group).gr_gid
  return gid


def _ensure_metadata(path, user, group, mode=None, log=None):
  stat = os.stat(path)
  updated = False

  if mode:
    existing_mode = stat.st_mode & 07777
    if existing_mode != mode:
      log and log.info("Changing permission for %s from %o to %o" % (
      path, existing_mode, mode))
      os.chmod(path, mode)
      updated = True

  if user:
    uid = _coerce_uid(user)
    if stat.st_uid != uid:
      log and log.info(
        "Changing owner for %s from %d to %s" % (path, stat.st_uid, user))
      os.chown(path, uid, -1)
      updated = True

  if group:
    gid = _coerce_gid(group)
    if stat.st_gid != gid:
      log and log.info(
        "Changing group for %s from %d to %s" % (path, stat.st_gid, group))
      os.chown(path, -1, gid)
      updated = True

  return updated


class FileProvider(Provider):
  def action_create(self):
    path = self.resource.path
    write = False
    content = self._get_content()
    if not os.path.exists(path):
      write = True
      reason = "it doesn't exist"
    else:
      if content is not None:
        with open(path, "rb") as fp:
          old_content = fp.read()
        if content != old_content:
          write = True
          reason = "contents don't match"
          self.resource.env.backup_file(path)

    if write:
      self.log.info("Writing %s because %s" % (self.resource, reason))
      with open(path, "wb") as fp:
        if content:
          fp.write(content)
      self.resource.updated()

    if _ensure_metadata(self.resource.path, self.resource.owner,
                        self.resource.group, mode=self.resource.mode,
                        log=self.log):
      self.resource.updated()

  def action_delete(self):
    path = self.resource.path
    if os.path.exists(path):
      self.log.info("Deleting %s" % self.resource)
      os.unlink(path)
      self.resource.updated()

  def action_touch(self):
    path = self.resource.path
    with open(path, "a"):
      pass

  def _get_content(self):
    content = self.resource.content
    if content is None:
      return None
    elif isinstance(content, basestring):
      return content
    elif hasattr(content, "__call__"):
      return content()
    raise Fail("Unknown source type for %s: %r" % (self, content))


class DirectoryProvider(Provider):
  def action_create(self):
    path = self.resource.path
    if not os.path.exists(path):
      self.log.info("Creating directory %s" % self.resource)
      if self.resource.recursive:
        os.makedirs(path, self.resource.mode or 0755)
      else:
        os.mkdir(path, self.resource.mode or 0755)
      self.resource.updated()

    if _ensure_metadata(path, self.resource.owner, self.resource.group,
                        mode=self.resource.mode, log=self.log):
      self.resource.updated()

  def action_delete(self):
    path = self.resource.path
    if os.path.exists(path):
      self.log.info("Removing directory %s" % self.resource)
      os.rmdir(path)
      # TODO: recursive
      self.resource.updated()


class LinkProvider(Provider):
  def action_create(self):
    path = self.resource.path

    if os.path.lexists(path):
      oldpath = os.path.realpath(path)
      if oldpath == self.resource.to:
        return
      if not os.path.islink(path):
        raise Fail(
          "%s trying to create a symlink with the same name as an existing file or directory" % self)
      self.log.info("%s replacing old symlink to %s" % (self, oldpath))
      os.unlink(path)

    if self.resource.hard:
      self.log.info("Creating hard %s" % self.resource)
      os.link(self.resource.to, path)
      self.resource.updated()
    else:
      self.log.info("Creating symbolic %s" % self.resource)
      os.symlink(self.resource.to, path)
      self.resource.updated()

  def action_delete(self):
    path = self.resource.path
    if os.path.exists(path):
      self.log.info("Deleting %s" % self.resource)
      os.unlink(path)
      self.resource.updated()


def _preexec_fn(resource):
  def preexec():
    if resource.group:
      gid = _coerce_gid(resource.group)
      os.setgid(gid)
      os.setegid(gid)
    if resource.user:
      uid = _coerce_uid(resource.user)
      os.setuid(uid)
      os.seteuid(uid)

  return preexec


class ExecuteProvider(Provider):
  def action_run(self):
    if self.resource.creates:
      if os.path.exists(self.resource.creates):
        return

    self.log.info("Executing %s" % self.resource)

    ret = subprocess.call(self.resource.command, shell=True,
                          cwd=self.resource.cwd, env=self.resource.environment,
                          preexec_fn=_preexec_fn(self.resource))

    if self.resource.returns and ret not in self.resource.returns:
      raise Fail("%s failed, returned %d instead of %s" % (
      self, ret, self.resource.returns))
    self.resource.updated()


class ScriptProvider(Provider):
  def action_run(self):
    from tempfile import NamedTemporaryFile

    self.log.info("Running script %s" % self.resource)
    with NamedTemporaryFile(prefix="resource_management-script", bufsize=0) as tf:
      tf.write(self.resource.code)
      tf.flush()

      _ensure_metadata(tf.name, self.resource.user, self.resource.group)
      subprocess.call([self.resource.interpreter, tf.name],
                      cwd=self.resource.cwd, env=self.resource.environment,
                      preexec_fn=_preexec_fn(self.resource))
    self.resource.updated()

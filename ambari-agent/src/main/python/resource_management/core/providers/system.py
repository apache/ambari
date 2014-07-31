#!/usr/bin/env python
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

from __future__ import with_statement

import grp
import os
import pwd
import time
import shutil
from resource_management.core import shell
from resource_management.core.base import Fail
from resource_management.core import ExecuteTimeoutException
from resource_management.core.providers import Provider
from resource_management.core.logger import Logger


def _coerce_uid(user):
  try:
    uid = int(user)
  except ValueError:
    try:
      uid = pwd.getpwnam(user).pw_uid
    except KeyError:
      raise Fail("User %s doesn't exist." % user)
  return uid


def _coerce_gid(group):
  try:
    gid = int(group)
  except ValueError:
    try:
      gid = grp.getgrnam(group).gr_gid
    except KeyError:
      raise Fail("Group %s doesn't exist." % group)
  return gid


def _ensure_metadata(path, user, group, mode=None):
  stat = os.stat(path)

  if mode:
    existing_mode = stat.st_mode & 07777
    if existing_mode != mode:
      Logger.info("Changing permission for %s from %o to %o" % (
      path, existing_mode, mode))
      os.chmod(path, mode)

  if user:
    uid = _coerce_uid(user)
    if stat.st_uid != uid:
      Logger.info(
        "Changing owner for %s from %d to %s" % (path, stat.st_uid, user))
      os.chown(path, uid, -1)

  if group:
    gid = _coerce_gid(group)
    if stat.st_gid != gid:
      Logger.info(
        "Changing group for %s from %d to %s" % (path, stat.st_gid, group))
      os.chown(path, -1, gid)


class FileProvider(Provider):
  def action_create(self):
    path = self.resource.path
    
    if os.path.isdir(path):
      raise Fail("Applying %s failed, directory with name %s exists" % (self.resource, path))
    
    dirname = os.path.dirname(path)
    if not os.path.isdir(dirname):
      raise Fail("Applying %s failed, parent directory %s doesn't exist" % (self.resource, dirname))
    
    write = False
    content = self._get_content()
    if not os.path.exists(path):
      write = True
      reason = "it doesn't exist"
    elif self.resource.replace:
      if content is not None:
        with open(path, "rb") as fp:
          old_content = fp.read()
        if content != old_content:
          write = True
          reason = "contents don't match"
          if self.resource.backup:
            self.resource.env.backup_file(path)

    if write:
      Logger.info("Writing %s because %s" % (self.resource, reason))
      with open(path, "wb") as fp:
        if content:
          fp.write(content)

    _ensure_metadata(self.resource.path, self.resource.owner,
                        self.resource.group, mode=self.resource.mode)

  def action_delete(self):
    path = self.resource.path
    
    if os.path.isdir(path):
      raise Fail("Applying %s failed, %s is directory not file!" % (self.resource, path))
    
    if os.path.exists(path):
      Logger.info("Deleting %s" % self.resource)
      os.unlink(path)

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
      Logger.info("Creating directory %s" % self.resource)
      if self.resource.recursive:
        os.makedirs(path, self.resource.mode or 0755)
      else:
        dirname = os.path.dirname(path)
        if not os.path.isdir(dirname):
          raise Fail("Applying %s failed, parent directory %s doesn't exist" % (self.resource, dirname))
        
        os.mkdir(path, self.resource.mode or 0755)
      
    if not os.path.isdir(path):
      raise Fail("Applying %s failed, file %s already exists" % (self.resource, path))

    _ensure_metadata(path, self.resource.owner, self.resource.group,
                        mode=self.resource.mode)

  def action_delete(self):
    path = self.resource.path
    if os.path.exists(path):
      if not os.path.isdir(path):
        raise Fail("Applying %s failed, %s is not a directory" % (self.resource, path))
      
      Logger.info("Removing directory %s and all its content" % self.resource)
      shutil.rmtree(path)


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
      Logger.info("%s replacing old symlink to %s" % (self.resource, oldpath))
      os.unlink(path)
      
    if self.resource.hard:
      if not os.path.exists(self.resource.to):
        raise Fail("Failed to apply %s, linking to nonexistent location %s" % (self.resource, self.resource.to))
      if os.path.isdir(self.resource.to):
        raise Fail("Failed to apply %s, cannot create hard link to a directory (%s)" % (self.resource, self.resource.to))
      
      Logger.info("Creating hard %s" % self.resource)
      os.link(self.resource.to, path)
    else:
      if not os.path.exists(self.resource.to):
        Logger.info("Warning: linking to nonexistent location %s" % self.resource.to)
        
      Logger.info("Creating symbolic %s" % self.resource)
      os.symlink(self.resource.to, path)

  def action_delete(self):
    path = self.resource.path
    if os.path.exists(path):
      Logger.info("Deleting %s" % self.resource)
      os.unlink(path)


def _preexec_fn(resource):
  def preexec():
    if resource.group:
      gid = _coerce_gid(resource.group)
      os.setgid(gid)
      os.setegid(gid)

  return preexec


class ExecuteProvider(Provider):
  def action_run(self):
    if self.resource.creates:
      if os.path.exists(self.resource.creates):
        return

    Logger.debug("Executing %s" % self.resource)
    
    if self.resource.path != []:
      if not self.resource.environment:
        self.resource.environment = {}
      
      self.resource.environment['PATH'] = os.pathsep.join(self.resource.path) 
    
    for i in range (0, self.resource.tries):
      try:
        shell.checked_call(self.resource.command, logoutput=self.resource.logoutput,
                            cwd=self.resource.cwd, env=self.resource.environment,
                            preexec_fn=_preexec_fn(self.resource), user=self.resource.user,
                            wait_for_finish=self.resource.wait_for_finish, timeout=self.resource.timeout)
        break
      except Fail as ex:
        if i == self.resource.tries-1: # last try
          raise ex
        else:
          Logger.info("Retrying after %d seconds. Reason: %s" % (self.resource.try_sleep, str(ex)))
          time.sleep(self.resource.try_sleep)
      except ExecuteTimeoutException:
        err_msg = ("Execution of '%s' was killed due timeout after %d seconds") % (self.resource.command, self.resource.timeout)
        
        if self.resource.on_timeout:
          Logger.info("Executing '%s'. Reason: %s" % (self.resource.on_timeout, err_msg))
          shell.checked_call(self.resource.on_timeout)
        else:
          raise Fail(err_msg)
       

class ExecuteScriptProvider(Provider):
  def action_run(self):
    from tempfile import NamedTemporaryFile

    Logger.info("Running script %s" % self.resource)
    with NamedTemporaryFile(prefix="resource_management-script", bufsize=0) as tf:
      tf.write(self.resource.code)
      tf.flush()

      _ensure_metadata(tf.name, self.resource.user, self.resource.group)
      shell.call([self.resource.interpreter, tf.name],
                      cwd=self.resource.cwd, env=self.resource.environment,
                      preexec_fn=_preexec_fn(self.resource))

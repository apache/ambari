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

from contextlib import contextmanager
import grp
import os
import pwd
import re
import tempfile

from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger


_SAFE_PREFIX = re.compile(r"[A-Za-z0-9_.-]{1,64}\Z", re.ASCII)


@contextmanager
def private_temporary_file(
  content,
  owner,
  group=None,
  temp_dir="/tmp",
  prefix="ambari-secret-",
):
  if not owner:
    raise Fail("Private temporary files require an owner")
  if not isinstance(prefix, str) or _SAFE_PREFIX.fullmatch(prefix) is None:
    raise Fail("Private temporary file prefix contains unsupported characters")

  try:
    owner_record = pwd.getpwnam(owner)
    uid = owner_record.pw_uid
    gid = grp.getgrnam(group).gr_gid if group else owner_record.pw_gid
  except KeyError as error:
    raise Fail("Private temporary file owner or group does not exist") from error

  if isinstance(content, str):
    encoded_content = content.encode("utf-8")
  elif isinstance(content, bytes):
    encoded_content = content
  else:
    raise Fail("Private temporary file content must be text or bytes")

  descriptor = None
  path = None
  primary_error = None
  try:
    descriptor, path = tempfile.mkstemp(dir=temp_dir, prefix=prefix)
    os.fchmod(descriptor, 0o600)
    os.fchown(descriptor, uid, gid)
    with os.fdopen(descriptor, "wb") as stream:
      descriptor = None
      stream.write(encoded_content)
      stream.flush()
      os.fsync(stream.fileno())
    yield path
  except BaseException as error:
    primary_error = error
    raise
  finally:
    if descriptor is not None:
      try:
        os.close(descriptor)
      except OSError:
        pass
    if path is not None:
      try:
        os.unlink(path)
      except FileNotFoundError:
        pass
      except OSError as cleanup_error:
        if primary_error is not None:
          Logger.warning(
            "Could not remove private temporary file after operation failure: "
            f"{cleanup_error}"
          )
        else:
          raise Fail(
            f"Could not remove private temporary file: {cleanup_error}"
          ) from cleanup_error

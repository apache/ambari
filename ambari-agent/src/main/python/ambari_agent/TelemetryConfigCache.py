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

import copy
import json
import logging
import os
import tempfile
import threading

from ambari_agent.metrics.telemetry import (
  profile_digest,
  validate_telemetry_bundle,
)


logger = logging.getLogger(__name__)


class TelemetryConfigCache:
  ASSIGNMENT_FILE = "telemetry_assignment.json"
  PROFILES_DIRECTORY = "telemetry_profiles"

  def __init__(self, cache_dir):
    self.cache_dir = cache_dir
    self.assignment_file = os.path.join(cache_dir, self.ASSIGNMENT_FILE)
    self.profiles_dir = os.path.join(cache_dir, self.PROFILES_DIRECTORY)
    self._lock = threading.RLock()
    self._assignment = {"schemaVersion": 1, "targets": []}
    self._profiles = {}
    self.hash = None
    self.last_reload_successful = True
    self._load()

  def _load(self):
    if not os.path.isfile(self.assignment_file):
      return

    try:
      with open(self.assignment_file, "r", encoding="utf-8") as stream:
        envelope = json.load(stream)

      assignment = envelope["assignment"]
      cache_hash = envelope["hash"]
      if not isinstance(cache_hash, str) or not cache_hash:
        raise ValueError("Persisted telemetry assignment hash is invalid")
      profiles = self._load_referenced_profiles(assignment)
      validate_telemetry_bundle(assignment, profiles)
    except Exception:
      self.last_reload_successful = False
      logger.exception("Unable to load the persisted telemetry assignment")
      return

    with self._lock:
      self._assignment = assignment
      self._profiles = profiles
      self.hash = cache_hash

  def _load_referenced_profiles(self, assignment):
    profiles = {}
    for target in assignment.get("targets", []):
      digest = target.get("profileHash")
      if not digest or digest in profiles:
        continue
      path = self._profile_path(digest)
      with open(path, "r", encoding="utf-8") as stream:
        profile = json.load(stream)
      if profile_digest(profile) != digest:
        raise ValueError('Telemetry profile digest does not match "{}"'.format(digest))
      profiles[digest] = profile
    return profiles

  def update(self, assignment, profiles, cache_hash):
    if not isinstance(cache_hash, str) or not cache_hash:
      raise ValueError("Telemetry assignment hash must be a non-empty string")
    if not isinstance(profiles, dict):
      raise ValueError("Telemetry profiles must be keyed by their SHA-256 digest")

    with self._lock:
      candidate_profiles = copy.deepcopy(self._profiles)
      try:
        for digest, profile in profiles.items():
          if profile_digest(profile) != digest:
            raise ValueError(
              'Telemetry profile digest does not match "{}"'.format(digest)
            )
          candidate_profiles[digest] = profile

        validate_telemetry_bundle(assignment, candidate_profiles)
        referenced_profiles = {
          target["profileHash"]: candidate_profiles[target["profileHash"]]
          for target in assignment["targets"]
          if target["format"] == "jmx_json"
        }
        self._persist_profiles(profiles)
        self._atomic_json_write(
          self.assignment_file,
          {"hash": cache_hash, "assignment": assignment},
        )
      except Exception:
        self.last_reload_successful = False
        raise

      self._assignment = copy.deepcopy(assignment)
      self._profiles = copy.deepcopy(referenced_profiles)
      self.hash = cache_hash
      self.last_reload_successful = True

  def snapshot(self):
    with self._lock:
      return {
        "assignment": copy.deepcopy(self._assignment),
        "profiles": copy.deepcopy(self._profiles),
      }

  def _persist_profiles(self, profiles):
    for digest, profile in profiles.items():
      path = self._profile_path(digest)
      if os.path.isfile(path):
        continue
      self._atomic_json_write(path, profile)

  def _profile_path(self, digest):
    prefix = "sha256:"
    if not isinstance(digest, str) or not digest.startswith(prefix):
      raise ValueError('Invalid telemetry profile digest "{}"'.format(digest))
    hex_digest = digest[len(prefix) :]
    if len(hex_digest) != 64 or any(char not in "0123456789abcdef" for char in hex_digest):
      raise ValueError('Invalid telemetry profile digest "{}"'.format(digest))
    return os.path.join(self.profiles_dir, hex_digest + ".json")

  def _atomic_json_write(self, path, value):
    directory = os.path.dirname(path)
    if not os.path.isdir(directory):
      os.makedirs(directory)

    descriptor, temporary_path = tempfile.mkstemp(prefix=".telemetry-", dir=directory)
    try:
      with os.fdopen(descriptor, "w", encoding="utf-8") as stream:
        json.dump(value, stream, indent=2, sort_keys=True)
        stream.flush()
        os.fsync(stream.fileno())
      os.replace(temporary_path, path)
    except Exception:
      try:
        os.unlink(temporary_path)
      except OSError:
        pass
      raise

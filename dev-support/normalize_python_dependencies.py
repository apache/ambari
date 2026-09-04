#!/usr/bin/env python3

# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

"""Remove unused dependency CLIs while keeping wheel RECORD files consistent."""

import argparse
import configparser
import csv
import os
from pathlib import Path, PurePosixPath
import re
import sys
import tempfile


EXTERNAL_SCRIPT = re.compile(r"^(?:\.\./)+bin/([^/]+)$")


class NormalizationError(RuntimeError):
  pass


def _declared_scripts(dist_info):
  entry_points_path = dist_info / "entry_points.txt"
  if not entry_points_path.is_file():
    return set()
  parser = configparser.ConfigParser(interpolation=None)
  parser.optionxform = str
  parser.read(entry_points_path, encoding="utf-8")
  scripts = set()
  for section in ("console_scripts", "gui_scripts"):
    if parser.has_section(section):
      scripts.update(parser.options(section))
  return scripts


def _script_name(raw_path):
  match = EXTERNAL_SCRIPT.fullmatch(raw_path)
  if match:
    return match.group(1)
  parts = PurePosixPath(raw_path).parts
  if len(parts) == 2 and parts[0] == "bin":
    return parts[1]
  return None


def _write_record_atomic(record_path, rows):
  descriptor, temporary_name = tempfile.mkstemp(
    dir=record_path.parent, prefix=f".{record_path.name}.", suffix=".tmp"
  )
  try:
    with os.fdopen(descriptor, "w", newline="", encoding="utf-8") as stream:
      csv.writer(stream).writerows(rows)
      stream.flush()
      os.fsync(stream.fileno())
    os.replace(temporary_name, record_path)
  except Exception:
    try:
      os.unlink(temporary_name)
    except FileNotFoundError:
      pass
    raise


def normalize(root):
  root = Path(root)
  if not root.is_dir():
    raise NormalizationError(f"Dependency root does not exist: {root}")

  owners = {}
  records_to_update = {}
  for dist_info in sorted(root.glob("*.dist-info")):
    record_path = dist_info / "RECORD"
    if not record_path.is_file():
      raise NormalizationError(f"Distribution has no RECORD: {dist_info.name}")
    declared_scripts = _declared_scripts(dist_info)
    retained_rows = []
    removed_rows = 0
    with record_path.open(newline="", encoding="utf-8") as stream:
      for row in csv.reader(stream):
        if len(row) != 3:
          raise NormalizationError(f"Malformed RECORD row in {record_path}: {row}")
        script_name = _script_name(row[0])
        if script_name is None:
          retained_rows.append(row)
          continue
        if script_name not in declared_scripts:
          raise NormalizationError(
            f"RECORD contains undeclared dependency script {script_name}: {record_path}"
          )
        previous_owner = owners.get(script_name)
        if previous_owner is not None:
          raise NormalizationError(
            f"Dependency script {script_name} has multiple owners: "
            f"{previous_owner.name}, {dist_info.name}"
          )
        owners[script_name] = dist_info
        removed_rows += 1
    if removed_rows:
      records_to_update[record_path] = retained_rows

  bin_directory = root / "bin"
  actual_scripts = (
    {path.name for path in bin_directory.iterdir()} if bin_directory.is_dir() else set()
  )
  if actual_scripts != set(owners):
    missing = sorted(set(owners) - actual_scripts)
    unexpected = sorted(actual_scripts - set(owners))
    details = []
    if missing:
      details.append("missing=" + ",".join(missing))
    if unexpected:
      details.append("undeclared=" + ",".join(unexpected))
    raise NormalizationError(
      "Dependency console scripts differ from RECORD metadata: " + "; ".join(details)
    )

  for script_name in sorted(owners):
    script_path = bin_directory / script_name
    if script_path.is_symlink() or not script_path.is_file():
      raise NormalizationError(f"Dependency script is not a regular file: {script_path}")
    script_path.unlink()
  if bin_directory.is_dir():
    bin_directory.rmdir()

  for record_path, rows in records_to_update.items():
    _write_record_atomic(record_path, rows)
  return sorted(owners)


def main(argv=None):
  parser = argparse.ArgumentParser(description=__doc__)
  parser.add_argument("--root", required=True)
  arguments = parser.parse_args(argv)
  try:
    removed = normalize(arguments.root)
  except (NormalizationError, OSError, UnicodeError, ValueError) as exception:
    parser.exit(1, f"Python dependency normalization failed: {exception}\n")
  print(f"Removed {len(removed)} unused Python dependency console scripts")
  return 0


if __name__ == "__main__":
  sys.exit(main())

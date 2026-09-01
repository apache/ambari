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

import importlib.util
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tarfile
import tempfile
from unittest import TestCase
from unittest.mock import call, patch


MODULE_PATH = os.path.abspath(
  os.path.join(
    os.path.dirname(__file__), "../../main/repo/install_ambari_tarball.py"
  )
)
SPEC = importlib.util.spec_from_file_location("install_ambari_tarball", MODULE_PATH)
install_ambari_tarball = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(install_ambari_tarball)


class TestInstallAmbariTarball(TestCase):
  @staticmethod
  def _shell_function(path, name):
    lines = Path(path).read_text(encoding="utf-8").splitlines()
    start = next(
      index
      for index, line in enumerate(lines)
      if line.replace(" ", "") == f"{name}(){{"
    )
    end = next(index for index in range(start + 1, len(lines)) if lines[index] == "}")
    return "\n".join(lines[start : end + 1])

  def test_dependency_property_names_include_family_and_generic_fallbacks(self):
    self.assertEqual(
      install_ambari_tarball.dependency_property_names(
        "rpm.dependency.list", "redhat", "3"
      ),
      [
        "rpm.dependency.list.redhat3",
        "rpm.dependency.list.redhat2",
        "rpm.dependency.list.redhat1",
        "rpm.dependency.list.redhat",
        "rpm.dependency.list",
      ],
    )

  def test_dependency_parser_preserves_versions_capabilities_and_alternatives(self):
    self.assertEqual(
      [
        "openssl",
        "(python39 >= 3.9.2 or python3 >= 3.9.2)",
        "python(abi) = 3.9",
      ],
      install_ambari_tarball.dependency_expressions(
        "openssl,\\nRequires: (python39 >= 3.9.2 or python3 >= 3.9.2),"
        "\\nRequires: python(abi) = 3.9"
      ),
    )
    self.assertEqual(
      ["python39 >= 3.9.2", "python3 >= 3.9.2"],
      install_ambari_tarball.rpm_dependency_alternatives(
        "(python39 >= 3.9.2 or python3 >= 3.9.2)"
      ),
    )
    self.assertEqual(
      ("python3", "<<", "3.10"),
      install_ambari_tarball.deb_dependency_parts("python3 (<< 3.10)"),
    )

  @patch.object(install_ambari_tarball.Utils, "os_call")
  @patch.object(install_ambari_tarball.Utils, "dependency_is_installed")
  def test_rpm_alternative_installs_candidate_and_rechecks_constraint(
    self, dependency_is_installed, os_call
  ):
    dependency_is_installed.side_effect = [
      install_ambari_tarball.OsCallFailure("missing"),
      install_ambari_tarball.OsCallFailure("missing"),
      True,
    ]

    install_ambari_tarball.Utils.install_dependency(
      "(python39 >= 3.9.2 or python3 >= 3.9.2)", True
    )

    os_call.assert_called_once_with(
      ["sudo", "yum", "-y", "install", "python39"]
    )

  @patch.object(install_ambari_tarball.Utils, "os_call")
  def test_deb_dependency_checks_installed_version(self, os_call):
    os_call.side_effect = ["3.9.18-1", None]

    self.assertTrue(
      install_ambari_tarball.Utils.dependency_is_installed(
        "python3 (>= 3.9.2)", False
      )
    )

    self.assertEqual(
      [
        call(
          ["dpkg-query", "-W", "-f=${Version}", "python3"], logoutput=False
        ),
        call(
          ["dpkg", "--compare-versions", "3.9.18-1", ">=", "3.9.2"],
          logoutput=False,
        ),
      ],
      os_call.call_args_list,
    )

  def test_runner_accepts_single_url_and_preserves_options(self):
    runner = install_ambari_tarball.Runner()

    runner.parse_opts(
      ["--root-folder", "/opt/ambari", "--dependencies-skip", "bundle.tar.gz"]
    )

    self.assertEqual("bundle.tar.gz", runner.url)
    self.assertEqual("/opt/ambari", runner.options.root_folder)
    self.assertTrue(runner.options.skip_dependencies)

  def test_runner_rejects_unknown_option(self):
    runner = install_ambari_tarball.Runner()

    with self.assertRaises(SystemExit) as raised:
      runner.parse_opts(["--dependecies-skip", "bundle.tar.gz"])

    self.assertEqual(2, raised.exception.code)

  def test_tar_upgrade_replaces_owned_library_and_preserves_wheel_content(self):
    with tempfile.TemporaryDirectory() as directory:
      temporary = Path(directory)
      root = temporary / "root"
      library = root / "usr/lib/ambari-agent/lib"
      (library / "ambari_jinja2").mkdir(parents=True)
      (library / "ambari_jinja2/legacy.py").write_text(
        "legacy = True\n", encoding="utf-8"
      )
      (library / "ambari_agent").mkdir()
      (library / "ambari_agent/deleted.py").write_text(
        "deleted = True\n", encoding="utf-8"
      )

      payload = temporary / "payload"
      (payload / "usr/lib/ambari-agent/lib/ambari_agent").mkdir(parents=True)
      (payload / "usr/lib/ambari-agent/lib/ambari_agent/main.py").write_text(
        "current = True\n", encoding="utf-8"
      )
      (payload / "usr/lib/ambari-agent/lib/official_dependency/docs").mkdir(
        parents=True
      )
      (payload / "usr/lib/ambari-agent/lib/official_dependency/docs/index.txt").write_text(
        "upstream documentation\n", encoding="utf-8"
      )
      archive_path = temporary / "ambari-agent.tar.gz"
      with tarfile.open(archive_path, "w:gz") as archive:
        archive.add(payload / "usr", arcname="usr")

      installer = install_ambari_tarball.TargzInstaller(
        str(archive_path), str(root), False, True
      )
      installer.archive_name = str(archive_path)
      installer.remove_replaced_library_roots()
      installer.extract_archive()

      self.assertFalse((library / "ambari_jinja2").exists())
      self.assertFalse((library / "ambari_agent/deleted.py").exists())
      self.assertTrue((library / "ambari_agent/main.py").is_file())
      self.assertTrue(
        (library / "official_dependency/docs/index.txt").is_file()
      )

  def test_install_helpers_derive_one_python_minor_from_native_extensions(self):
    repository = Path(__file__).resolve().parents[4]
    helpers = (
      (
        repository / "ambari-agent/conf/unix/install-helper.sh",
        "AMBARI_AGENT_ROOT_DIR",
      ),
      (
        repository / "ambari-server/conf/unix/install-helper.sh",
        "AMBARI_SERVER_ROOT_DIR",
      ),
    )

    with tempfile.TemporaryDirectory() as temporary_directory:
      root = Path(temporary_directory)
      library = root / "lib"
      library.mkdir()
      (library / "_cffi_backend.cpython-39-x86_64-linux-gnu.so").touch()

      for helper, root_variable in helpers:
        function = self._shell_function(helper, "required_python_version")
        result = subprocess.run(
          ["sh", "-c", f'{function}\n{root_variable}="$1" required_python_version', "sh", str(root)],
          check=False,
          capture_output=True,
          text=True,
        )
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual("3.9", result.stdout.strip())

  def test_install_helpers_reject_mixed_or_missing_native_abis(self):
    repository = Path(__file__).resolve().parents[4]
    helper = repository / "ambari-agent/conf/unix/install-helper.sh"
    function = self._shell_function(helper, "required_python_version")

    with tempfile.TemporaryDirectory() as temporary_directory:
      root = Path(temporary_directory)
      library = root / "lib"
      library.mkdir()
      for filename in (
        "_cffi_backend.cpython-39-x86_64-linux-gnu.so",
        "_speedups.cpython-310-x86_64-linux-gnu.so",
      ):
        (library / filename).touch()

      mixed = subprocess.run(
        ["sh", "-c", f'{function}\nAMBARI_AGENT_ROOT_DIR="$1" required_python_version', "sh", str(root)],
        check=False,
        capture_output=True,
        text=True,
      )
      self.assertNotEqual(0, mixed.returncode)

      for path in library.iterdir():
        path.unlink()
      missing = subprocess.run(
        ["sh", "-c", f'{function}\nAMBARI_AGENT_ROOT_DIR="$1" required_python_version', "sh", str(root)],
        check=False,
        capture_output=True,
        text=True,
      )
      self.assertNotEqual(0, missing.returncode)

  def test_service_entrypoints_derive_and_enforce_packaged_python_abi(self):
    repository = Path(__file__).resolve().parents[4]
    entrypoints = (
      (
        repository / "ambari-agent/conf/unix/ambari-agent",
        "AMBARI_AGENT_LIB_DIR",
      ),
      (
        repository / "ambari-server/sbin/ambari-server",
        "AMBARI_SERVER_LIB_DIR",
      ),
    )
    for entrypoint, library_variable in entrypoints:
      script = entrypoint.read_text(encoding="utf-8")
      function = self._shell_function(entrypoint, "required_python_version")
      self.assertIn("sys.version_info[:2] != required", script)
      for abi, expected in (("39", "3.9"), ("310", "3.10")):
        with self.subTest(entrypoint=entrypoint, abi=abi):
          with tempfile.TemporaryDirectory() as directory:
            extension = Path(directory) / f"_native.cpython-{abi}-x86_64-linux-gnu.so"
            extension.touch()
            result = subprocess.run(
              [
                "sh",
                "-c",
                f'{function}\n{library_variable}="$1" required_python_version',
                "sh",
                directory,
              ],
              check=True,
              text=True,
              capture_output=True,
            )
          self.assertEqual(expected, result.stdout.strip())

  def test_agent_package_scripts_keep_custom_root_operations_scoped(self):
    repository = Path(__file__).resolve().parents[4]
    package_scripts = (
      repository / "ambari-agent/src/main/package/rpm/preinstall.sh",
      repository / "ambari-agent/src/main/package/rpm/postinstall.sh",
      repository / "ambari-agent/src/main/package/rpm/preremove.sh",
      repository / "ambari-agent/src/main/package/rpm/postremove.sh",
      repository / "ambari-agent/src/main/package/rpm/posttrans_agent.sh",
      repository / "ambari-agent/src/main/package/deb/control/preinst",
      repository / "ambari-agent/src/main/package/deb/control/postinst",
      repository / "ambari-agent/src/main/package/deb/control/prerm",
      repository / "ambari-agent/src/main/package/deb/control/postrm",
    )

    for package_script in package_scripts:
      content = package_script.read_text(encoding="utf-8")
      self.assertIn("RPM_INSTALL_PREFIX", content, package_script)

    with tempfile.TemporaryDirectory() as temporary_directory:
      root = Path(temporary_directory)
      config = root / "etc/ambari-agent/conf/ambari-agent.ini"
      config.parent.mkdir(parents=True)
      config.write_text("[server]\nhostname=server.example\n", encoding="utf-8")

      env = os.environ.copy()
      env[install_ambari_tarball.ROOT_FOLDER_ENV_VARIABLE] = str(root)
      for preinstall in (package_scripts[0], package_scripts[5]):
        result = subprocess.run(
          ["bash", str(preinstall), "install"],
          check=False,
          capture_output=True,
          text=True,
          env=env,
        )
        self.assertEqual(0, result.returncode, result.stderr)

      self.assertFalse(config.exists())
      self.assertTrue(
        list(config.parent.glob("ambari-agent.ini.old*")),
        "preinstall backup must remain inside the custom root",
      )

  def test_agent_install_helper_and_config_upgrade_use_custom_root(self):
    repository = Path(__file__).resolve().parents[4]
    helper = repository / "ambari-agent/conf/unix/install-helper.sh"
    upgrade_script = repository / "ambari-agent/conf/unix/upgrade_agent_configs.py"

    python_version_output = subprocess.check_output(
      [
        "/usr/bin/python3",
        "-c",
        "import sys; print(f'{sys.version_info[0]}.{sys.version_info[1]}')",
      ],
      text=True,
    ).strip()
    python_abi = python_version_output.replace(".", "")
    versioned_python = Path(f"/usr/bin/python{python_version_output}")
    expected_python = str(
      versioned_python if versioned_python.is_file() else Path("/usr/bin/python3")
    )

    with tempfile.TemporaryDirectory() as temporary_directory:
      root = Path(temporary_directory)
      required_directories = (
        root / "usr/lib/ambari-agent/lib",
        root / "usr/bin",
        root / "usr/sbin",
        root / "etc/init.d",
        root / "etc/ambari-agent/conf",
        root / "var/lib/ambari-agent/tmp",
        root / "var/lib/ambari-agent/keys",
        root / "var/lib/ambari-agent/data",
      )
      for directory in required_directories:
        directory.mkdir(parents=True, exist_ok=True)

      native_extension = (
        root
        / "usr/lib/ambari-agent/lib"
        / f"_cffi_backend.cpython-{python_abi}-x86_64-linux-gnu.so"
      )
      native_extension.touch()
      shutil.copy2(
        upgrade_script, root / "var/lib/ambari-agent/upgrade_agent_configs.py"
      )
      (root / "etc/init.d/ambari-agent").write_text(
        "#!/bin/sh\nexit 0\n", encoding="utf-8"
      )

      env = os.environ.copy()
      env[install_ambari_tarball.ROOT_FOLDER_ENV_VARIABLE] = str(root)
      result = subprocess.run(
        ["sh", str(helper), "install"],
        check=False,
        capture_output=True,
        text=True,
        env=env,
      )
      self.assertEqual(0, result.returncode, result.stderr)
      wrapper = root / "usr/bin/ambari-python-wrap"
      self.assertTrue(wrapper.is_symlink())
      self.assertEqual(expected_python, os.readlink(wrapper))
      self.assertTrue((root / "usr/sbin/ambari-agent").is_symlink())
      self.assertTrue((root / "var/log/ambari-agent").is_dir())

      config = root / "etc/ambari-agent/conf/ambari-agent.ini"
      backup = root / "etc/ambari-agent/conf/ambari-agent.ini.old"
      config.write_text("[server]\nhostname=new.example\n", encoding="utf-8")
      backup.write_text("[server]\nhostname=old.example\n", encoding="utf-8")
      result = subprocess.run(
        [sys.executable, str(upgrade_script)],
        check=False,
        capture_output=True,
        text=True,
        env=env,
      )
      self.assertEqual(0, result.returncode, result.stderr)
      self.assertIn("hostname = old.example", config.read_text(encoding="utf-8"))

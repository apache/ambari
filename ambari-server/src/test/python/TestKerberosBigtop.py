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

import base64
import importlib.util
from pathlib import Path
import sys
from types import SimpleNamespace
import unittest
from unittest.mock import MagicMock, patch
from xml.etree import ElementTree

from resource_management.core.exceptions import Fail
from resource_management.libraries import functions
from resource_management.libraries.script.script import Script


KERBEROS = (
  Path(__file__).resolve().parents[2]
  / "main/resources/stacks/BIGTOP/3.2.0/services/KERBEROS"
)
SCRIPTS = KERBEROS / "package/scripts"
BIGTOP_STACKS = KERBEROS.parents[2]


def load_module(name, path, dependencies=None):
  spec = importlib.util.spec_from_file_location(name, path)
  module = importlib.util.module_from_spec(spec)
  with patch.dict(sys.modules, dependencies or {}):
    spec.loader.exec_module(module)
  return module


KERBEROS_UTILS = load_module(
  "bigtop_kerberos_utils", SCRIPTS / "kerberos_utils.py"
)
KERBEROS_CLIENT = load_module(
  "bigtop_kerberos_client",
  SCRIPTS / "kerberos_client.py",
  {"kerberos_utils": KERBEROS_UTILS},
)
KERBEROS_CHECK = load_module(
  "bigtop_kerberos_service_check",
  SCRIPTS / "service_check.py",
  {"kerberos_utils": KERBEROS_UTILS},
)


class TestKerberosInputContract(unittest.TestCase):
  def test_boolean_values_are_parsed_instead_of_using_string_truthiness(self):
    self.assertTrue(KERBEROS_UTILS.as_bool(True, "setting"))
    self.assertTrue(KERBEROS_UTILS.as_bool(" true ", "setting"))
    self.assertFalse(KERBEROS_UTILS.as_bool(False, "setting"))
    self.assertFalse(KERBEROS_UTILS.as_bool("FALSE", "setting"))
    for value in (None, 1, "yes", ""):
      with self.subTest(value=value):
        with self.assertRaises(Fail):
          KERBEROS_UTILS.as_bool(value, "setting")

  def test_stack_host_and_executable_contracts_fail_closed(self):
    self.assertEqual(
      "3.2.0", KERBEROS_UTILS.validate_bigtop_stack("BIGTOP", "3.2.0")
    )
    self.assertEqual(
      "host.example.com",
      KERBEROS_UTILS.validate_host("Host.Example.Com", "host"),
    )
    for stack_name, stack_version in (("OTHER", "3.2.0"), ("BIGTOP", "3")):
      with self.subTest(stack_name=stack_name, stack_version=stack_version):
        with self.assertRaises(Fail):
          KERBEROS_UTILS.validate_bigtop_stack(stack_name, stack_version)

    trusted = SimpleNamespace(st_uid=0, st_mode=0o100755)
    with (
      patch.object(KERBEROS_UTILS.sudo, "path_lexists", return_value=True),
      patch.object(KERBEROS_UTILS.sudo, "path_islink", return_value=False),
      patch.object(KERBEROS_UTILS.sudo, "path_isfile", return_value=True),
      patch.object(KERBEROS_UTILS.sudo, "stat", return_value=trusted),
    ):
      self.assertEqual(
        "/usr/bin/kinit",
        KERBEROS_UTILS.validate_executable("/usr/bin/kinit", "kinit"),
      )
      for mode in (0o100775, 0o100644):
        with self.subTest(mode=mode):
          with (
            patch.object(
              KERBEROS_UTILS.sudo,
              "stat",
              return_value=SimpleNamespace(st_uid=0, st_mode=mode),
            ),
            self.assertRaises(Fail),
          ):
            KERBEROS_UTILS.validate_executable("/usr/bin/kinit", "kinit")

  def test_realm_endpoints_and_domains_fail_closed(self):
    self.assertEqual("EXAMPLE.COM", KERBEROS_UTILS.validate_realm("EXAMPLE.COM"))
    self.assertEqual(
      "kdc1.example.com:88,[2001:db8::1]:88",
      KERBEROS_UTILS.validate_endpoints(
        " kdc1.example.com:88, [2001:db8::1]:88 ",
        "kdc_hosts",
        required=True,
      ),
    )
    self.assertEqual(
      ".example.com,example.com",
      KERBEROS_UTILS.validate_domains(" .example.com, example.com "),
    )
    for endpoint in (
      "",
      88,
      "host:0",
      "host:65536",
      "host..example.com:88",
      "999.999.999.999:88",
      "host;$(id):88",
      "2001:db8::1",
    ):
      with self.subTest(endpoint=endpoint):
        with self.assertRaises(Fail):
          KERBEROS_UTILS.validate_endpoints(endpoint, "kdc_hosts", required=True)
    with self.assertRaises(Fail):
      KERBEROS_UTILS.validate_endpoint(
        "kdc1.example.com,kdc2.example.com", "master_kdc"
      )
    self.assertEqual(
      "kdc1.example.com",
      KERBEROS_UTILS.endpoint_without_port("kdc1.example.com:88", "kdc_hosts"),
    )
    self.assertEqual(
      "[2001:db8::1]",
      KERBEROS_UTILS.endpoint_without_port("[2001:db8::1]:88", "kdc_hosts"),
    )
    for realm in (None, "", "EXAMPLE.COM.", "EXAMPLE.COM\n[realms]", "EXAMPLE COM"):
      with self.subTest(realm=realm):
        with self.assertRaises(Fail):
          KERBEROS_UTILS.validate_realm(realm)
    for domains in ("example..com", ".example.com,,other.example.com"):
      with self.subTest(domains=domains):
        with self.assertRaises(Fail):
          KERBEROS_UTILS.validate_domains(domains)

  def test_keytab_records_require_safe_paths_principals_and_strict_base64(self):
    record = {
      "principal": "service/_HOST@EXAMPLE.COM",
      "keytab_file_path": "/etc/security/keytabs/service.keytab",
      "keytab_content_base64": base64.b64encode(b"keytab").decode("ascii"),
      "keytab_file_owner_name": "service",
      "keytab_file_group_name": "hadoop",
      "keytab_file_owner_access": "r",
      "keytab_file_group_access": "",
    }
    with (
      patch.object(KERBEROS_UTILS.sudo, "path_lexists", return_value=False),
      patch.object(KERBEROS_UTILS.sudo, "path_islink", return_value=False),
    ):
      self.assertEqual(
        (record,),
        KERBEROS_UTILS.validate_keytab_records([record], require_content=True),
      )

      inconsistent = dict(record)
      inconsistent["keytab_file_group_access"] = "r"
      with self.assertRaisesRegex(Fail, "inconsistent content or permissions"):
        KERBEROS_UTILS.validate_keytab_records(
          [record, inconsistent], require_content=True
        )

      for field, value in (
        ("principal", "service\nother"),
        ("principal", " service/_HOST@EXAMPLE.COM"),
        ("keytab_file_path", "/etc/passwd"),
        ("keytab_file_path", "/tmp/service.keytab"),
        ("keytab_file_path", "/usr/lib/service.keytab"),
        ("keytab_file_path", "relative.keytab"),
        ("keytab_file_path", "/etc/security/keytabs/bad\tpath.keytab"),
        ("keytab_content_base64", "not base64!"),
        ("keytab_file_owner_name", "service;id"),
        ("keytab_file_owner_access", "rwx"),
      ):
        invalid = dict(record)
        invalid[field] = value
        with self.subTest(field=field, value=value):
          with self.assertRaises(Fail):
            KERBEROS_UTILS.validate_keytab_records(
              [invalid], require_content=True
            )

      oversized = dict(record)
      oversized["keytab_content_base64"] = base64.b64encode(b"12345").decode(
        "ascii"
      )
      with (
        patch.object(KERBEROS_UTILS, "_MAX_KEYTAB_BYTES", 4),
        self.assertRaisesRegex(Fail, "supported size"),
      ):
        KERBEROS_UTILS.validate_keytab_records([oversized], require_content=True)

    with (
      patch.object(KERBEROS_UTILS.sudo, "path_lexists", return_value=True),
      patch.object(KERBEROS_UTILS.sudo, "path_islink", return_value=True),
    ):
      with self.assertRaisesRegex(Fail, "symbolic link"):
        KERBEROS_UTILS.validate_keytab_records([record], require_content=True)

    with (
      patch.object(KERBEROS_UTILS.sudo, "path_lexists", return_value=True),
      patch.object(KERBEROS_UTILS.sudo, "path_islink", return_value=False),
      patch.object(KERBEROS_UTILS.sudo, "path_isfile", return_value=False),
    ):
      with self.assertRaisesRegex(Fail, "regular file"):
        KERBEROS_UTILS.validate_keytab_records([record], require_content=True)


class TestKerberosClientContract(unittest.TestCase):
  def setUp(self):
    self.client = KERBEROS_CLIENT.KerberosClient()
    self.env = MagicMock()

  def test_install_packages_false_skips_package_installation(self):
    with (
      patch.object(KERBEROS_CLIENT, "default", return_value="false"),
      patch.object(self.client, "install_packages") as install_packages,
      patch.object(self.client, "configure") as configure,
    ):
      self.client.install(self.env)
    install_packages.assert_not_called()
    configure.assert_called_once_with(self.env)

  def test_configure_honors_manage_flag_and_never_clears_shared_cache(self):
    unmanaged = SimpleNamespace(manage_krb5_conf=False)
    with (
      patch.dict(sys.modules, {"params": unmanaged}),
      patch.object(KERBEROS_CLIENT, "write_krb5_conf") as write,
    ):
      self.client.configure(self.env)
    write.assert_not_called()

    managed = SimpleNamespace(
      manage_krb5_conf=True,
      krb5_conf_path="/etc/krb5.conf",
    )
    with (
      patch.dict(sys.modules, {"params": managed}),
      patch.object(KERBEROS_UTILS, "validate_managed_file") as validate,
      patch.object(KERBEROS_CLIENT, "write_krb5_conf") as write,
    ):
      self.client.configure(self.env)
    validate.assert_called_once_with(
      "/etc/krb5.conf", "krb5.conf path", suffix="/krb5.conf"
    )
    write.assert_called_once_with(managed)
    self.assertNotIn("clear_tmp_cache", (SCRIPTS / "kerberos_client.py").read_text())

  def test_keytab_commands_validate_before_shared_helper_and_set_params(self):
    params = SimpleNamespace(
      kerberos_command_params=[{"principal": "svc/_HOST@REALM"}],
      hostname="host.example.com",
    )
    for method_name, helper_name, require_content in (
      ("set_keytab", "write_keytab_file", True),
      ("remove_keytab", "delete_keytab_file", False),
      ("check_keytabs", "find_missing_keytabs", False),
    ):
      with self.subTest(method=method_name):
        with (
          patch.dict(sys.modules, {"params": params}),
          patch.object(
            KERBEROS_UTILS, "validate_keytab_records"
          ) as validate,
          patch.object(KERBEROS_CLIENT, helper_name) as helper,
        ):
          getattr(self.client, method_name)(self.env)
        validate.assert_called_once_with(
          params.kerberos_command_params,
          **({"require_content": True} if require_content else {}),
        )
        helper.assert_called_once()
    self.assertEqual(3, self.env.set_params.call_count)


class TestKerberosServiceCheckContract(unittest.TestCase):
  def _params(self, managed=True):
    return SimpleNamespace(
      smoke_test_principal="ambari-qa@EXAMPLE.COM",
      smoke_test_keytab_file="/etc/security/keytabs/smokeuser.headless.keytab",
      smoke_user="ambari-qa",
      default_group="hadoop",
      manage_identities=managed,
      kinit_path_local="/usr/bin/kinit",
      service_check_timeout=60,
    )

  def test_service_check_uses_unique_private_cache_and_structured_kinit(self):
    params = self._params()
    cache = MagicMock()
    context = MagicMock()
    context.__enter__.return_value = cache
    with (
      patch.dict(sys.modules, {"params": params}),
      patch.object(KERBEROS_UTILS, "keytab_is_regular_file", return_value=True),
      patch.object(KERBEROS_UTILS, "validate_executable"),
      patch.object(
        KERBEROS_CHECK, "PrivateKerberosCache", return_value=context
      ) as cache_factory,
    ):
      KERBEROS_CHECK.KerberosServiceCheck().service_check(MagicMock())

    cache_factory.assert_called_once_with(
      "ambari-qa", "hadoop", prefix="ambari-kerberos-service-check-"
    )
    cache.kinit.assert_called_once_with(
      "/usr/bin/kinit",
      params.smoke_test_keytab_file,
      "ambari-qa@EXAMPLE.COM",
      timeout=60,
    )
    context.__exit__.assert_called_once()

  def test_missing_managed_credentials_fail_and_manual_credentials_skip(self):
    for managed in (True, False):
      params = self._params(managed)
      with (
        patch.dict(sys.modules, {"params": params}),
        patch.object(
          KERBEROS_UTILS, "keytab_is_regular_file", return_value=False
        ),
        patch.object(KERBEROS_CHECK, "PrivateKerberosCache") as cache_factory,
      ):
        if managed:
          with self.assertRaises(Fail):
            KERBEROS_CHECK.KerberosServiceCheck().service_check(MagicMock())
        else:
          KERBEROS_CHECK.KerberosServiceCheck().service_check(MagicMock())
      cache_factory.assert_not_called()

  def test_service_check_has_no_predictable_cache_or_shell_command(self):
    source = (SCRIPTS / "service_check.py").read_text(encoding="utf-8")
    for obsolete in ("hashlib", "sha224", "Execute(", "os.path.isfile", " -c "):
      self.assertNotIn(obsolete, source)

  def test_untrusted_kinit_fails_before_private_cache_creation(self):
    params = self._params()
    with (
      patch.dict(sys.modules, {"params": params}),
      patch.object(KERBEROS_UTILS, "keytab_is_regular_file", return_value=True),
      patch.object(
        KERBEROS_UTILS,
        "validate_executable",
        side_effect=Fail("untrusted kinit"),
      ),
      patch.object(KERBEROS_CHECK, "PrivateKerberosCache") as cache_factory,
      self.assertRaisesRegex(Fail, "untrusted kinit"),
    ):
      KERBEROS_CHECK.KerberosServiceCheck().service_check(MagicMock())
    cache_factory.assert_not_called()

  def test_kinit_failure_propagates_after_private_cache_cleanup(self):
    params = self._params()
    cache = MagicMock()
    cache.kinit.side_effect = Fail("kinit failed")
    context = MagicMock()
    context.__enter__.return_value = cache
    context.__exit__.return_value = False
    with (
      patch.dict(sys.modules, {"params": params}),
      patch.object(KERBEROS_UTILS, "keytab_is_regular_file", return_value=True),
      patch.object(KERBEROS_UTILS, "validate_executable"),
      patch.object(KERBEROS_CHECK, "PrivateKerberosCache", return_value=context),
    ):
      with self.assertRaisesRegex(Fail, "kinit failed"):
        KERBEROS_CHECK.KerberosServiceCheck().service_check(MagicMock())
    context.__exit__.assert_called_once()


class TestKerberosParamsAndMetadata(unittest.TestCase):
  def _config(self, manage_identities="false", manage_conf="false"):
    return {
      "clusterLevelParams": {
        "stack_name": "BIGTOP",
        "stack_version": "3.2.0",
      },
      "agentLevelParams": {"hostname": "host.example.com"},
      "configurations": {
        "cluster-env": {
          "smokeuser": "ambari-qa",
          "smokeuser_principal_name": "cluster@EXAMPLE.COM",
          "smokeuser_keytab": "/etc/security/keytabs/cluster.keytab",
          "user_group": "hadoop",
        },
        "kerberos-env": {
          "manage_identities": manage_identities,
          "realm": "EXAMPLE.COM",
          "kdc_hosts": " kdc1.example.com:88, kdc2.example.com ",
          "master_kdc": "",
          "admin_server_host": "admin.example.com:749",
          "encryption_types": "aes256-cts-hmac-sha1-96 aes128-cts-hmac-sha1-96",
          "executable_search_paths": "/usr/bin",
        },
        "krb5-conf": {
          "manage_krb5_conf": manage_conf,
          "force_tcp": "false",
          "conf_dir": "/etc",
          "domains": ".example.com, example.com",
          "content": "template",
        },
      },
      "commandParams": {
        "principal_name": "command@EXAMPLE.COM",
        "keytab_file": "/etc/security/keytabs/command.keytab",
      },
      "kerberosCommandParams": [],
    }

  def test_params_are_minimal_typed_and_only_use_command_override_when_managed(self):
    with (
      patch.object(Script, "get_config", return_value=self._config()),
      patch.object(functions, "get_kinit_path", return_value="/usr/bin/kinit"),
    ):
      params = load_module(
        "bigtop_kerberos_params_unmanaged",
        SCRIPTS / "params.py",
        {"kerberos_utils": KERBEROS_UTILS},
      )
    self.assertFalse(params.manage_identities)
    self.assertFalse(params.manage_krb5_conf)
    self.assertFalse(params.force_tcp)
    self.assertEqual("kdc1.example.com:88,kdc2.example.com", params.kdc_hosts)
    self.assertEqual("cluster@EXAMPLE.COM", params.smoke_test_principal)
    for obsolete in (
      "kdc_conf_path",
      "kadm5_acl_path",
      "kdb5_util_path",
      "kdamin_pid_path",
      "artifact_dir",
      "jce_policy_zip",
      "admin_password",
    ):
      self.assertFalse(hasattr(params, obsolete))

    fallback_config = self._config()
    fallback_config["configurations"]["kerberos-env"]["admin_server_host"] = ""
    with (
      patch.object(Script, "get_config", return_value=fallback_config),
      patch.object(functions, "get_kinit_path", return_value="/usr/bin/kinit"),
    ):
      fallback = load_module(
        "bigtop_kerberos_params_admin_fallback",
        SCRIPTS / "params.py",
        {"kerberos_utils": KERBEROS_UTILS},
      )
    self.assertEqual("kdc1.example.com", fallback.admin_server_host)

    config = self._config(manage_identities="true")
    with (
      patch.object(Script, "get_config", return_value=config),
      patch.object(functions, "get_kinit_path", return_value="/usr/bin/kinit"),
    ):
      managed = load_module(
        "bigtop_kerberos_params_managed",
        SCRIPTS / "params.py",
        {"kerberos_utils": KERBEROS_UTILS},
      )
    self.assertEqual("command@EXAMPLE.COM", managed.smoke_test_principal)

  def test_metadata_uses_current_supported_os_families_and_has_no_dead_status(self):
    root = ElementTree.parse(KERBEROS / "metainfo.xml").getroot()
    families = {
      node.findtext("osFamily")
      for node in root.findall("./services/service/osSpecifics/osSpecific")
    }
    self.assertIn("debian10,debian11,ubuntu20,ubuntu22", families)
    self.assertNotIn("debian7,ubuntu12,ubuntu14,ubuntu16", families)
    self.assertFalse((SCRIPTS / "status_params.py").exists())
    for stack_version in ("3.2.0", "3.3.0", "3.4.0"):
      metadata = ElementTree.parse(
        BIGTOP_STACKS / stack_version / "services/KERBEROS/metainfo.xml"
      ).getroot()
      self.assertEqual(
        "1.0.0", metadata.findtext("./services/service/version")
      )

  def test_managed_empty_krb5_template_fails_closed(self):
    config = self._config(manage_conf="true")
    config["configurations"]["krb5-conf"]["content"] = "  "
    with (
      patch.object(Script, "get_config", return_value=config),
      patch.object(functions, "get_kinit_path", return_value="/usr/bin/kinit"),
    ):
      with self.assertRaisesRegex(Fail, "krb5-conf/content"):
        load_module(
          "bigtop_kerberos_params_empty_template",
          SCRIPTS / "params.py",
          {"kerberos_utils": KERBEROS_UTILS},
        )

  def test_client_template_does_not_pin_obsolete_default_enctypes(self):
    source = (KERBEROS / "properties/krb5_conf.j2").read_text(encoding="utf-8")
    self.assertNotIn("default_tgs_enctypes", source)
    self.assertNotIn("default_tkt_enctypes", source)
    self.assertNotIn("default(kdc_host_list[0]", source)


if __name__ == "__main__":
  unittest.main()

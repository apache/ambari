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

import io
import json
import sys
import subprocess
from unittest.mock import MagicMock, patch, ANY
from unittest import mock
import unittest
import logging
import signal
import configparser
import ssl
import os
import tempfile

from ambari_commons import OSCheck
from only_for_platform import os_distro_value

with patch("ambari_commons.os_check.linux_distribution", return_value=("Suse", "11", "Final")):
  from ambari_agent import NetUtil
  from ambari_agent.security import CertificateManager
  from ambari_agent.AmbariConfig import AmbariConfig
from ambari_agent import security

aa = mock.mock_open()


class TestSecurity(unittest.TestCase):
  @patch.object(OSCheck, "os_distribution", new=MagicMock(return_value=os_distro_value))
  def setUp(self):
    # disable stdout
    out = io.StringIO()
    sys.stdout = out
    # Create config
    self.config = AmbariConfig()
    self.config.get_server_ssl_context = MagicMock(return_value=MagicMock())
    self.config.get_server_ssl_options = MagicMock(
      return_value={
        "cert_reqs": ssl.CERT_REQUIRED,
        "ca_certs": "/keys/ca.crt",
        "check_hostname": True,
      }
    )

  def tearDown(self):
    # enable stdout
    sys.stdout = sys.__stdout__

  def test_VerifiedHTTPSConnection_establish_connection_starts_through_connect(self):
    connection = MagicMock()
    verified_connection = security.VerifiedHTTPSConnection(
      "server.example", "wss://server.example/agent/stomp/v1", self.config
    )

    verified_connection.establish_connection(connection)

    connection.connect.assert_called_once_with(wait=True)
    connection.start.assert_not_called()
    connection.disconnect.assert_not_called()

  def test_VerifiedHTTPSConnection_establish_connection_disconnects_after_failure(self):
    connection = MagicMock()
    connection.connect.side_effect = RuntimeError("connection failed")
    verified_connection = security.VerifiedHTTPSConnection(
      "server.example", "wss://server.example/agent/stomp/v1", self.config
    )

    with self.assertRaisesRegex(RuntimeError, "connection failed"):
      verified_connection.establish_connection(connection)

    connection.connect.assert_called_once_with(wait=True)
    connection.start.assert_not_called()
    connection.disconnect.assert_called_once_with()

  @patch("ambari_agent.security.AmbariStompConnection")
  @patch("ambari_agent.security.CertificateManager")
  def test_enrollment_passphrase_is_retained_until_agent_certificate_exists(
    self, certificate_manager_mock, connection_mock
  ):
    self.config.isTwoWaySSLConnection = MagicMock(return_value=True)
    manager = certificate_manager_mock.return_value
    manager.getAgentKeyName.return_value = "/keys/agent.key"
    manager.getAgentCrtName.return_value = "/keys/agent.crt"
    manager.getSrvrCrtName.return_value = "/keys/ca.crt"
    connection_mock.return_value.connect.side_effect = OSError("connection reset")
    verified_connection = security.VerifiedHTTPSConnection(
      "server.example",
      "wss://server.example/agent/stomp/v1",
      self.config,
      enrollment_passphrase="one-time-secret",
    )

    with patch("ambari_agent.security.os.path.isfile", return_value=False):
      with self.assertRaisesRegex(OSError, "connection reset"):
        verified_connection.connect()

    self.assertEqual("one-time-secret", verified_connection.enrollment_passphrase)

  @patch("ambari_agent.security.AmbariStompConnection")
  @patch("ambari_agent.security.CertificateManager")
  def test_enrollment_passphrase_is_cleared_after_certificate_install(
    self, certificate_manager_mock, connection_mock
  ):
    self.config.isTwoWaySSLConnection = MagicMock(return_value=True)
    manager = certificate_manager_mock.return_value
    manager.getAgentKeyName.return_value = "/keys/agent.key"
    manager.getAgentCrtName.return_value = "/keys/agent.crt"
    manager.getSrvrCrtName.return_value = "/keys/ca.crt"
    verified_connection = security.VerifiedHTTPSConnection(
      "server.example",
      "wss://server.example/agent/stomp/v1",
      self.config,
      enrollment_passphrase="one-time-secret",
    )

    with patch("ambari_agent.security.os.path.isfile", return_value=True):
      verified_connection.connect()

    self.assertIsNone(verified_connection.enrollment_passphrase)
    self.assertIsNone(manager.enrollment_passphrase)

  ### CertificateManager ###

  @patch("ambari_agent.hostname.hostname")
  def test_getAgentKeyName(self, hostname_mock):
    hostname_mock.return_value = "dummy.hostname"
    self.config.set("security", "keysdir", "/dummy-keysdir")
    man = CertificateManager(self.config, "active_server")
    res = man.getAgentKeyName()
    self.assertEqual(res, os.path.abspath("/dummy-keysdir/dummy.hostname.key"))

  @patch("ambari_agent.hostname.hostname")
  def test_getAgentCrtName(self, hostname_mock):
    hostname_mock.return_value = "dummy.hostname"
    self.config.set("security", "keysdir", "/dummy-keysdir")
    man = CertificateManager(self.config, "active_server")
    res = man.getAgentCrtName()
    self.assertEqual(res, os.path.abspath("/dummy-keysdir/dummy.hostname.crt"))

  @patch("ambari_agent.hostname.hostname")
  def test_getAgentCrtReqName(self, hostname_mock):
    hostname_mock.return_value = "dummy.hostname"
    self.config.set("security", "keysdir", "/dummy-keysdir")
    man = CertificateManager(self.config, "active_server")
    res = man.getAgentCrtReqName()
    self.assertEqual(res, os.path.abspath("/dummy-keysdir/dummy.hostname.csr"))

  @patch("ambari_agent.hostname.hostname")
  def test_agent_certificate_paths_hash_unsafe_hostname(self, hostname_mock):
    hostname_mock.return_value = "../../outside/agent name"
    self.config.set("security", "keysdir", "/dummy-keysdir")
    man = CertificateManager(self.config, "active_server")

    key_path = man.getAgentKeyName()

    self.assertEqual(os.path.abspath("/dummy-keysdir"), os.path.dirname(key_path))
    self.assertRegex(os.path.basename(key_path), r"^agent-[0-9a-f]{64}\.key$")

  def test_getSrvrCrtName(self):
    self.config.set("security", "keysdir", "/dummy-keysdir")
    man = CertificateManager(self.config, "active_server")
    res = man.getSrvrCrtName()
    self.assertEqual(res, os.path.abspath("/dummy-keysdir/ca.crt"))

  @patch("os.path.exists")
  @patch("os.unlink")
  @patch("ssl.SSLContext")
  @patch.object(security.CertificateManager, "getAgentKeyName")
  @patch.object(security.CertificateManager, "genAgentCrtReq")
  @patch.object(security.CertificateManager, "getAgentCrtName")
  @patch.object(security.CertificateManager, "reqSignCrt")
  def test_checkCertExists(
    self,
    reqSignCrt_mock,
    getAgentCrtName_mock,
    genAgentCrtReq_mock,
    getAgentKeyName_mock,
    ssl_context_mock,
    unlink_mock,
    exists_mock,
  ):
    self.config.set("security", "keysdir", "/dummy-keysdir")
    getAgentKeyName_mock.return_value = "dummy AgentKeyName"
    getAgentCrtName_mock.return_value = "dummy AgentCrtName"
    man = CertificateManager(self.config, "active_server")

    # Case when all files exist
    exists_mock.side_effect = [True, True, True]
    man.checkCertExists()
    self.assertFalse(genAgentCrtReq_mock.called)
    self.assertFalse(reqSignCrt_mock.called)
    ssl_context_mock.return_value.load_cert_chain.assert_called_once_with(
      "dummy AgentCrtName", "dummy AgentKeyName"
    )
    ssl_context_mock.reset_mock()

    # Absent agent key
    exists_mock.side_effect = [True, False, True]
    man.checkCertExists()
    genAgentCrtReq_mock.assert_called_once_with("dummy AgentKeyName")
    reqSignCrt_mock.assert_called_once_with()
    unlink_mock.assert_called_once_with("dummy AgentCrtName")
    genAgentCrtReq_mock.reset_mock()
    reqSignCrt_mock.reset_mock()
    unlink_mock.reset_mock()

    # Absent agent cert
    exists_mock.side_effect = [True, True, False]
    man.checkCertExists()
    genAgentCrtReq_mock.assert_called_once_with(
      "dummy AgentKeyName", reuse_key=True
    )
    reqSignCrt_mock.assert_called_once_with()
    reqSignCrt_mock.reset_mock()

  def test_remove_persisted_enrollment_passphrase_preserves_other_settings(self):
    with tempfile.TemporaryDirectory() as directory:
      env_path = os.path.join(directory, "ambari-env.sh")
      with open(env_path, "w", encoding="utf-8") as stream:
        stream.write(
          "OTHER_SETTING=value\n"
          "  export AMBARI_PASSPHRASE=legacy-secret\n"
          "AMBARI_PASSPHRASE=secret\n"
        )
      self.config.set("security", "passphrase_file", env_path)

      security.remove_persisted_enrollment_passphrase(self.config)

      with open(env_path, encoding="utf-8") as stream:
        self.assertEqual("OTHER_SETTING=value\n", stream.read())
      self.assertEqual(0o600, os.stat(env_path).st_mode & 0o777)

  @patch("os.path.exists", return_value=False)
  def test_checkCertExists_rejects_missing_server_ca(self, exists_mock):
    self.config.set("security", "keysdir", "/dummy-keysdir")
    man = CertificateManager(self.config, "active_server")

    with self.assertRaisesRegex(ssl.SSLError, "Install the trusted CA"):
      man.checkCertExists()

    exists_mock.assert_called_once_with(man.getSrvrCrtName())

  def test_atomic_write_secures_existing_key_directory(self):
    with tempfile.TemporaryDirectory() as directory:
      keys_directory = os.path.join(directory, "keys")
      os.mkdir(keys_directory, 0o755)
      destination = os.path.join(keys_directory, "ca.crt")
      man = CertificateManager(self.config, "active_server")

      man._atomic_write(destination, b"certificate", 0o644)

      self.assertEqual(0o700, os.stat(keys_directory).st_mode & 0o777)
      self.assertEqual(0o644, os.stat(destination).st_mode & 0o777)

  @patch("ambari_agent.hostname.hostname", return_value="dummy-hostname")
  @patch("builtins.open", create=True, autospec=True)
  @patch("urllib.request.build_opener")
  def test_reqSignCrt_consumes_passphrase_and_atomically_writes_certificate(
    self, build_opener_mock, open_mock, hostname_mock
  ):
    self.config.set("security", "keysdir", "/dummy-keysdir")
    self.config.set("security", "passphrase_env_var_name", "DUMMY_PASSPHRASE")
    man = CertificateManager(
      self.config,
      "active_server",
      enrollment_passphrase="dummy-passphrase",
    )
    man._atomic_write = MagicMock()
    open_mock.return_value.__enter__.return_value.read.return_value = "dummy_request"
    response = MagicMock()
    response.read.return_value = b'{"result":"OK","signedCa":"dummy-crt"}'
    build_opener_mock.return_value.open.return_value.__enter__.return_value = response

    man.reqSignCrt()

    request = build_opener_mock.return_value.open.call_args.args[0]
    self.assertEqual(
      {"csr": "dummy_request", "passphrase": "dummy-passphrase"},
      json.loads(request.data.decode("utf-8")),
    )
    man._atomic_write.assert_called_once_with(
      man.getAgentCrtName(), b"dummy-crt", 0o644
    )
    self.assertIsNone(man.enrollment_passphrase)

  @patch("ambari_agent.hostname.hostname", return_value="dummy-hostname")
  @patch("builtins.open", create=True, autospec=True)
  @patch("urllib.request.build_opener")
  def test_reqSignCrt_rejects_failed_or_malformed_response_without_writing(
    self, build_opener_mock, open_mock, hostname_mock
  ):
    self.config.set("security", "keysdir", "/dummy-keysdir")
    open_mock.return_value.__enter__.return_value.read.return_value = "dummy_request"

    for response_body in (
      b'{"result":"FAIL","signedCa":"fail-crt"}',
      b"{malformed_object}",
    ):
      response = MagicMock()
      response.read.return_value = response_body
      build_opener_mock.return_value.open.return_value.__enter__.return_value = response
      man = CertificateManager(
        self.config, "active_server", enrollment_passphrase="dummy-passphrase"
      )
      man._atomic_write = MagicMock()

      with self.assertRaises(ssl.SSLError):
        man.reqSignCrt()

      man._atomic_write.assert_not_called()

  @patch("ambari_agent.hostname.hostname", return_value="dummy-hostname")
  @patch("builtins.open", create=True, autospec=True)
  @patch("urllib.request.build_opener")
  def test_reqSignCrt_retains_passphrase_after_transient_network_failure(
    self, build_opener_mock, open_mock, hostname_mock
  ):
    self.config.set("security", "keysdir", "/dummy-keysdir")
    open_mock.return_value.__enter__.return_value.read.return_value = "dummy_request"
    build_opener_mock.return_value.open.side_effect = OSError("connection reset")
    man = CertificateManager(
      self.config, "active_server", enrollment_passphrase="dummy-passphrase"
    )

    with self.assertRaisesRegex(OSError, "connection reset"):
      man.reqSignCrt()

    self.assertEqual(man.enrollment_passphrase, "dummy-passphrase")

  @patch.object(subprocess, "run")
  @patch.object(os, "chmod")
  def test_genAgentCrtReq(self, chmod_mock, run_mock):
    with tempfile.TemporaryDirectory() as keysdir:
      self.config.set("security", "keysdir", keysdir)
      man = CertificateManager(self.config, "active_server")
      man.genAgentCrtReq(os.path.join(keysdir, "hostname.key"))
      self.assertTrue(chmod_mock.called)
    run_mock.assert_called_once_with(
      ANY,
      check=True,
      stdout=subprocess.PIPE,
      stderr=subprocess.PIPE,
    )
    self.assertNotIn("shell", run_mock.call_args.kwargs)

  @patch.object(security.CertificateManager, "checkCertExists")
  def test_initSecurity(self, checkCertExists_method):
    man = CertificateManager(self.config, "active_server")
    man.initSecurity()
    self.assertTrue(checkCertExists_method.called)

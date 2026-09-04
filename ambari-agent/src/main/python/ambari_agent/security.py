#!/usr/bin/env python3

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import hashlib
import urllib.request, urllib.error, urllib.parse
import socket
import ssl
import os
import logging
import subprocess
import json
import re
import tempfile
from ambari_agent import hostname
from ambari_agent.AmbariStompConnection import AmbariStompConnection
from socket import error as socket_error

logger = logging.getLogger(__name__)


def _is_shell_variable_assignment(line, variable):
  return re.match(
    rf"(?:export\s+)?{re.escape(variable)}=", line.lstrip()
  ) is not None


def certificate_file_prefix(agent_hostname):
  if (
    len(agent_hostname) <= 200
    and not agent_hostname.startswith(".")
    and ".." not in agent_hostname
    and all(character.isalnum() or character in "._-" for character in agent_hostname)
  ):
    return agent_hostname
  return "agent-" + hashlib.sha256(agent_hostname.encode("utf-8")).hexdigest()


def remove_persisted_enrollment_passphrase(config):
  passphrase_file = os.path.abspath(
    config.get(
      "security", "passphrase_file", "/var/lib/ambari-agent/ambari-env.sh"
    )
  )
  if not os.path.isfile(passphrase_file):
    return

  passphrase_variable = config.get(
    "security", "passphrase_env_var_name", "AMBARI_PASSPHRASE"
  )
  with open(passphrase_file, "r", encoding="utf-8") as stream:
    original_lines = stream.readlines()
  retained_lines = [
    line
    for line in original_lines
    if not _is_shell_variable_assignment(line, passphrase_variable)
  ]
  if retained_lines == original_lines:
    return

  passphrase_directory = os.path.dirname(passphrase_file)
  descriptor, temporary_path = tempfile.mkstemp(
    prefix=".ambari-env-", dir=passphrase_directory, text=True
  )
  try:
    with os.fdopen(descriptor, "w", encoding="utf-8") as stream:
      stream.writelines(retained_lines)
      stream.flush()
      os.fsync(stream.fileno())
    os.chmod(temporary_path, 0o600)
    os.replace(temporary_path, passphrase_file)
    temporary_path = None
    directory_descriptor = os.open(passphrase_directory, os.O_RDONLY)
    try:
      os.fsync(directory_descriptor)
    finally:
      os.close(directory_descriptor)
  finally:
    if temporary_path is not None:
      try:
        os.unlink(temporary_path)
      except OSError:
        pass


class VerifiedHTTPSConnection:
  """Connecting using ssl wrapped sockets"""

  def __init__(self, host, connection_url, config, enrollment_passphrase=None):
    self.two_way_ssl_required = False
    self.host = host
    self.connection_url = connection_url
    self.config = config
    self.enrollment_passphrase = enrollment_passphrase

  def connect(self):
    self.two_way_ssl_required = self.config.isTwoWaySSLConnection(self.host)
    logger.debug(
      "Server two-way SSL authentication required: %s", self.two_way_ssl_required
    )
    if self.two_way_ssl_required is True:
      logger.info(
        "Server require two-way SSL authentication. Use it instead of one-way..."
      )

    logging.info(f"Connecting to {self.connection_url}")

    ssl_options = self.config.get_server_ssl_options()
    if not self.two_way_ssl_required:
      conn = AmbariStompConnection(self.connection_url, ssl_options=ssl_options)
      self.establish_connection(conn)
      remove_persisted_enrollment_passphrase(self.config)
      self.enrollment_passphrase = None
      logger.info(
        "SSL connection established. Two-way SSL authentication is "
        "turned off on the server."
      )
      return conn
    else:
      self.certMan = CertificateManager(
        self.config,
        self.host,
        enrollment_passphrase=self.enrollment_passphrase,
      )
      self.certMan.initSecurity()
      agent_key = self.certMan.getAgentKeyName()
      agent_crt = self.certMan.getAgentCrtName()
      server_crt = self.certMan.getSrvrCrtName()
      if os.path.isfile(agent_crt):
        self.enrollment_passphrase = None
        self.certMan.enrollment_passphrase = None

      ssl_options.update(
        {
          "keyfile": agent_key,
          "certfile": agent_crt,
          "cert_reqs": ssl.CERT_REQUIRED,
          "ca_certs": server_crt,
        }
      )

      conn = AmbariStompConnection(self.connection_url, ssl_options=ssl_options)

      try:
        self.establish_connection(conn)
        logger.info(
          "SSL connection established. Two-way SSL authentication "
          "completed successfully."
        )
      except ssl.SSLError:
        logger.error(
          "Two-way SSL authentication failed. Ensure that "
          "server and agent certificates were signed by the same CA "
          "and restart the agent. "
          "\nIn order to receive a new agent certificate, remove "
          "existing certificate file from keys directory. As a "
          "workaround you can turn off two-way SSL authentication in "
          "server configuration(ambari.properties) "
          "\nExiting.."
        )
        raise
      return conn

  def establish_connection(self, conn):
    """
    Create a stomp connection
    """
    try:
      conn.connect(wait=True)
    except Exception as ex:
      try:
        conn.disconnect()
      except:
        logger.exception("Exception during conn.disconnect()")

      if isinstance(ex, socket_error):
        logger.warning(f"Could not connect to {self.connection_url}. {str(ex)}")

      raise


class CertificateManager:
  def __init__(self, config, server_hostname, enrollment_passphrase=None):
    self.config = config
    self.keysdir = os.path.abspath(self.config.get("security", "keysdir"))
    self.server_crt = self.config.get("security", "server_crt")
    self.enrollment_passphrase = enrollment_passphrase
    self.server_url = (
      "https://" + server_hostname + ":" + self.config.get("server", "url_port")
    )

  def getAgentKeyName(self):
    keysdir = os.path.abspath(self.config.get("security", "keysdir"))
    return os.path.join(
      keysdir, certificate_file_prefix(hostname.hostname(self.config)) + ".key"
    )

  def getAgentCrtName(self):
    keysdir = os.path.abspath(self.config.get("security", "keysdir"))
    return os.path.join(
      keysdir, certificate_file_prefix(hostname.hostname(self.config)) + ".crt"
    )

  def getAgentCrtReqName(self):
    keysdir = os.path.abspath(self.config.get("security", "keysdir"))
    return os.path.join(
      keysdir, certificate_file_prefix(hostname.hostname(self.config)) + ".csr"
    )

  def getSrvrCrtName(self):
    return self.config.get_ca_cert_file_path()

  def checkCertExists(self):
    s = self.getSrvrCrtName()

    server_crt_exists = os.path.exists(s)

    if not server_crt_exists:
      raise ssl.SSLError(
        f"Ambari Server CA certificate does not exist at {s}. "
        "Install the trusted CA before starting the Agent."
      )
    else:
      logger.info("Server certicate exists, ok")

    agent_key = self.getAgentKeyName()
    agent_crt = self.getAgentCrtName()
    agent_key_exists = os.path.exists(agent_key)
    agent_crt_exists = os.path.exists(agent_crt)

    if agent_key_exists and agent_crt_exists:
      try:
        validation_context = ssl.SSLContext(ssl.PROTOCOL_TLS_CLIENT)
        validation_context.load_cert_chain(agent_crt, agent_key)
      except (OSError, ssl.SSLError):
        logger.error("Agent certificate and private key do not form a valid pair")
        os.unlink(agent_key)
        os.unlink(agent_crt)
        agent_key_exists = False
        agent_crt_exists = False

    csr_generated = False
    if not agent_key_exists:
      logger.info("Agent key not exists, generating request")
      if agent_crt_exists:
        os.unlink(agent_crt)
        agent_crt_exists = False
      self.genAgentCrtReq(agent_key)
      csr_generated = True
    else:
      logger.info("Agent key exists, ok")

    if not agent_crt_exists:
      logger.info("Agent certificate not exists, sending sign request")
      if not csr_generated:
        self.genAgentCrtReq(agent_key, reuse_key=True)
      self.reqSignCrt()
    else:
      logger.info("Agent certificate exists, ok")
      remove_persisted_enrollment_passphrase(self.config)

  def reqSignCrt(self):
    sign_crt_req_url = self.server_url + "/certs/" + urllib.parse.quote(
      hostname.hostname(self.config), safe=""
    )
    with open(self.getAgentCrtReqName(), encoding="utf-8") as agent_crt_req_f:
      agent_crt_req_content = agent_crt_req_f.read()
    if not self.enrollment_passphrase:
      raise RuntimeError("Ambari Agent enrollment passphrase is not available")
    register_data = {
      "csr": agent_crt_req_content,
      "passphrase": self.enrollment_passphrase,
    }
    data = json.dumps(register_data).encode("utf-8")
    proxy_handler = urllib.request.ProxyHandler({})
    https_handler = urllib.request.HTTPSHandler(
      context=self.config.get_server_ssl_context()
    )
    opener = urllib.request.build_opener(proxy_handler, https_handler)
    req = urllib.request.Request(
      sign_crt_req_url, data, {"Content-Type": "application/json"}
    )
    timeout = int(self.config.get("server", "connection_timeout", "10"))
    with opener.open(req, timeout=timeout) as response_stream:
      response = response_stream.read()
    try:
      data = json.loads(response.decode("utf-8"))
      logger.debug("Certificate signing response result=%s", data.get("result"))
    except Exception:
      logger.warning(
        "Malformed certificate signing response (bytes=%s)", len(response)
      )
      data = {"result": "ERROR"}
    result = data["result"]
    if result == "OK":
      agent_crt_content = data["signedCa"].encode("utf-8")
      self._atomic_write(self.getAgentCrtName(), agent_crt_content, 0o644)
      remove_persisted_enrollment_passphrase(self.config)
      self.enrollment_passphrase = None
      register_data["passphrase"] = None
    else:
      # Possible exception is catched higher at Controller
      logger.error(
        "Certificate signing failed."
        "\nIn order to receive a new agent"
        " certificate, remove existing certificate file from keys "
        "directory. As a workaround you can turn off two-way SSL "
        "authentication in server configuration(ambari.properties) "
        "\nExiting.."
      )
      raise ssl.SSLError

  def genAgentCrtReq(self, keyname, reuse_key=False):
    keysdir = os.path.abspath(self.config.get("security", "keysdir"))
    agent_hostname = hostname.hostname(self.config)
    os.makedirs(keysdir, mode=0o700, exist_ok=True)
    os.chmod(keysdir, 0o700)
    key_descriptor, temporary_key = tempfile.mkstemp(prefix=".agent-key-", dir=keysdir)
    os.close(key_descriptor)
    csr_descriptor, temporary_csr = tempfile.mkstemp(prefix=".agent-csr-", dir=keysdir)
    os.close(csr_descriptor)
    command = ["openssl", "req", "-new"]
    if reuse_key:
      os.unlink(temporary_key)
      temporary_key = None
      command.extend(["-key", keyname])
    else:
      command.extend(
        ["-newkey", "rsa:2048", "-nodes", "-keyout", temporary_key]
      )
    command.extend(
      ["-subj", f"/OU={agent_hostname}/", "-out", temporary_csr]
    )
    logger.info("Generating Ambari Agent certificate request with OpenSSL")
    try:
      subprocess.run(
        command,
        check=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
      )
      if temporary_key is not None:
        os.chmod(temporary_key, 0o600)
        os.replace(temporary_key, keyname)
        temporary_key = None
      os.chmod(temporary_csr, 0o600)
      os.replace(temporary_csr, self.getAgentCrtReqName())
      temporary_csr = None
      directory_descriptor = os.open(keysdir, os.O_RDONLY)
      try:
        os.fsync(directory_descriptor)
      finally:
        os.close(directory_descriptor)
    finally:
      for temporary_path in (temporary_key, temporary_csr):
        if temporary_path is not None:
          try:
            os.unlink(temporary_path)
          except OSError:
            pass

  def _atomic_write(self, path, content, mode):
    directory = os.path.dirname(path)
    os.makedirs(directory, mode=0o700, exist_ok=True)
    os.chmod(directory, 0o700)
    descriptor, temporary_path = tempfile.mkstemp(
      prefix=f".{os.path.basename(path)}-", dir=directory
    )
    try:
      with os.fdopen(descriptor, "wb") as stream:
        stream.write(content)
        stream.flush()
        os.fsync(stream.fileno())
      os.chmod(temporary_path, mode)
      os.replace(temporary_path, path)
      temporary_path = None
      directory_descriptor = os.open(os.path.dirname(path), os.O_RDONLY)
      try:
        os.fsync(directory_descriptor)
      finally:
        os.close(directory_descriptor)
    finally:
      if temporary_path is not None:
        try:
          os.unlink(temporary_path)
        except OSError:
          pass

  def initSecurity(self):
    self.checkCertExists()

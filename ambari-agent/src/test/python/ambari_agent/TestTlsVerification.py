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

import datetime
import http.client
import http.server
import ipaddress
import os
import ssl
import tempfile
import threading
from contextlib import closing
from unittest import TestCase

from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.x509.oid import ExtendedKeyUsageOID, NameOID

from ambari_agent.AmbariConfig import AmbariConfig
from ambari_agent.NetUtil import NetUtil


class _StatusHandler(http.server.BaseHTTPRequestHandler):
  def do_GET(self):
    self.send_response(200)
    self.end_headers()
    self.wfile.write(b"server-ca")

  def log_message(self, format_string, *args):
    pass


class TestTlsVerification(TestCase):
  def setUp(self):
    self.temp_dir = tempfile.TemporaryDirectory()
    self.addCleanup(self.temp_dir.cleanup)
    self.ca = self._create_ca("trusted-ca")
    self.untrusted_ca = self._create_ca("untrusted-ca")

  def test_trusted_ca_and_matching_hostname_succeed(self):
    server_cert, server_key = self._create_certificate(
      self.ca, "localhost", server_auth=True
    )

    with self._https_server(server_cert, server_key) as url:
      self.assertEqual((True, b"server-ca"), self._check_url(url, self.ca[2]))

  def test_untrusted_ca_is_rejected(self):
    server_cert, server_key = self._create_certificate(
      self.ca, "localhost", server_auth=True
    )

    with self._https_server(server_cert, server_key) as url:
      self.assertEqual((False, ""), self._check_url(url, self.untrusted_ca[2]))

  def test_hostname_mismatch_is_rejected(self):
    server_cert, server_key = self._create_certificate(
      self.ca, "wrong.example", server_auth=True
    )

    with self._https_server(server_cert, server_key) as url:
      self.assertEqual((False, ""), self._check_url(url, self.ca[2]))

  def test_expired_certificate_is_rejected(self):
    server_cert, server_key = self._create_certificate(
      self.ca, "localhost", server_auth=True, expired=True
    )

    with self._https_server(server_cert, server_key) as url:
      self.assertEqual((False, ""), self._check_url(url, self.ca[2]))

  def test_legacy_disable_setting_cannot_disable_tls_verification(self):
    config = AmbariConfig()
    config.set("security", "ca_cert_path", self.ca[2])
    config.set("security", "ssl_verify_cert", "0")

    options = config.get_server_ssl_options()
    context = config.get_server_ssl_context()

    self.assertEqual(ssl.CERT_REQUIRED, options["cert_reqs"])
    self.assertTrue(options["check_hostname"])
    self.assertEqual(self.ca[2], options["ca_certs"])
    self.assertEqual(ssl.CERT_REQUIRED, context.verify_mode)
    self.assertTrue(context.check_hostname)
    self.assertEqual(ssl.TLSVersion.TLSv1_2, context.minimum_version)

  def test_mutual_tls_validates_server_and_client_certificates(self):
    server_cert, server_key = self._create_certificate(
      self.ca, "localhost", server_auth=True
    )
    client_cert, client_key = self._create_certificate(
      self.ca, "ambari-agent", client_auth=True
    )
    client_context = ssl.create_default_context(cafile=self.ca[2])
    client_context.load_cert_chain(client_cert, client_key)

    with self._https_server(
      server_cert,
      server_key,
      client_ca=self.ca[2],
    ) as url:
      with closing(
        http.client.HTTPSConnection(
          "localhost",
          int(url.rsplit(":", 1)[1].split("/", 1)[0]),
          context=client_context,
          timeout=2,
        )
      ) as connection:
        connection.request("GET", "/ca")
        self.assertEqual(200, connection.getresponse().status)

  def test_mutual_tls_rejects_missing_client_certificate(self):
    server_cert, server_key = self._create_certificate(
      self.ca, "localhost", server_auth=True
    )
    client_context = ssl.create_default_context(cafile=self.ca[2])

    with self._https_server(
      server_cert,
      server_key,
      client_ca=self.ca[2],
    ) as url:
      with closing(
        http.client.HTTPSConnection(
          "localhost",
          int(url.rsplit(":", 1)[1].split("/", 1)[0]),
          context=client_context,
          timeout=2,
        )
      ) as connection:
        with self.assertRaises((ssl.SSLError, ConnectionError)):
          connection.request("GET", "/ca")
          connection.getresponse()

  def test_mutual_tls_rejects_client_certificate_from_untrusted_ca(self):
    server_cert, server_key = self._create_certificate(
      self.ca, "localhost", server_auth=True
    )
    client_cert, client_key = self._create_certificate(
      self.untrusted_ca, "ambari-agent", client_auth=True
    )
    client_context = ssl.create_default_context(cafile=self.ca[2])
    client_context.load_cert_chain(client_cert, client_key)

    with self._https_server(
      server_cert,
      server_key,
      client_ca=self.ca[2],
    ) as url:
      with closing(
        http.client.HTTPSConnection(
          "localhost",
          int(url.rsplit(":", 1)[1].split("/", 1)[0]),
          context=client_context,
          timeout=2,
        )
      ) as connection:
        with self.assertRaises((ssl.SSLError, ConnectionError)):
          connection.request("GET", "/ca")
          connection.getresponse()

  def _check_url(self, url, ca_path):
    config = AmbariConfig()
    config.set("security", "ca_cert_path", ca_path)
    config.set("server", "connection_timeout", "2")
    return NetUtil(config, threading.Event()).checkURL(url)

  def _create_ca(self, common_name):
    key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
    subject = x509.Name([x509.NameAttribute(NameOID.COMMON_NAME, common_name)])
    now = datetime.datetime.now(datetime.timezone.utc)
    cert = (
      x509.CertificateBuilder()
      .subject_name(subject)
      .issuer_name(subject)
      .public_key(key.public_key())
      .serial_number(x509.random_serial_number())
      .not_valid_before(now - datetime.timedelta(days=1))
      .not_valid_after(now + datetime.timedelta(days=30))
      .add_extension(x509.BasicConstraints(ca=True, path_length=None), critical=True)
      .sign(key, hashes.SHA256())
    )
    cert_path = self._write_certificate(f"{common_name}.crt", cert)
    return key, cert, cert_path

  def _create_certificate(
    self,
    ca,
    common_name,
    server_auth=False,
    client_auth=False,
    expired=False,
  ):
    key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
    subject = x509.Name([x509.NameAttribute(NameOID.COMMON_NAME, common_name)])
    now = datetime.datetime.now(datetime.timezone.utc)
    usages = []
    if server_auth:
      usages.append(ExtendedKeyUsageOID.SERVER_AUTH)
    if client_auth:
      usages.append(ExtendedKeyUsageOID.CLIENT_AUTH)
    cert = (
      x509.CertificateBuilder()
      .subject_name(subject)
      .issuer_name(ca[1].subject)
      .public_key(key.public_key())
      .serial_number(x509.random_serial_number())
      .not_valid_before(now - datetime.timedelta(days=30 if expired else 1))
      .not_valid_after(now - datetime.timedelta(days=1) if expired else now + datetime.timedelta(days=30))
      .add_extension(
        x509.SubjectAlternativeName(
          [
            x509.DNSName(common_name),
            x509.IPAddress(ipaddress.ip_address("127.0.0.1")),
          ]
        ),
        critical=False,
      )
      .add_extension(x509.ExtendedKeyUsage(usages), critical=False)
      .sign(ca[0], hashes.SHA256())
    )
    cert_path = self._write_certificate(f"{common_name}.crt", cert)
    key_path = os.path.join(self.temp_dir.name, f"{common_name}.key")
    with open(key_path, "wb") as stream:
      stream.write(
        key.private_bytes(
          serialization.Encoding.PEM,
          serialization.PrivateFormat.PKCS8,
          serialization.NoEncryption(),
        )
      )
    os.chmod(key_path, 0o600)
    return cert_path, key_path

  def _write_certificate(self, name, certificate):
    path = os.path.join(self.temp_dir.name, name)
    with open(path, "wb") as stream:
      stream.write(certificate.public_bytes(serialization.Encoding.PEM))
    return path

  def _https_server(self, certificate, key, client_ca=None):
    server = http.server.ThreadingHTTPServer(("127.0.0.1", 0), _StatusHandler)
    context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
    context.load_cert_chain(certificate, key)
    if client_ca:
      context.verify_mode = ssl.CERT_REQUIRED
      context.load_verify_locations(client_ca)
    server.socket = context.wrap_socket(server.socket, server_side=True)
    thread = threading.Thread(target=server.serve_forever, daemon=True)
    thread.start()

    class ServerContext:
      def __enter__(self):
        return f"https://localhost:{server.server_address[1]}/ca"

      def __exit__(self, exc_type, exc_value, traceback):
        server.shutdown()
        server.server_close()
        thread.join(2)

    return ServerContext()

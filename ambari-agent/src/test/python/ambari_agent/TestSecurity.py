#!/usr/bin/env python

'''
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
'''
import StringIO
import sys, subprocess

from ambari_agent import NetUtil
from ambari_agent.security import CertificateManager
from mock.mock import MagicMock, patch, ANY
import mock.mock
import unittest
from ambari_agent import ProcessHelper, main
import logging
import signal
from ambari_agent.AmbariConfig import AmbariConfig
import ConfigParser
import ssl
import os
import tempfile
from ambari_agent.Controller import Controller
from ambari_agent import security

aa = mock.mock.mock_open()
class TestSecurity(unittest.TestCase):

  def setUp(self):
    # disable stdout
    out = StringIO.StringIO()
    sys.stdout = out
    # Create config
    self.config = AmbariConfig().getConfig()
    # Instantiate CachedHTTPSConnection (skip connect() call)
    with patch.object(security.VerifiedHTTPSConnection, "connect"):
      self.cachedHTTPSConnection = security.CachedHTTPSConnection(self.config)


  def tearDown(self):
    # enable stdout
    sys.stdout = sys.__stdout__


  ### VerifiedHTTPSConnection ###

  @patch.object(security.CertificateManager, "initSecurity")
  @patch("socket.create_connection")
  @patch("ssl.wrap_socket")
  def test_VerifiedHTTPSConnection_connect(self, wrap_socket_mock,
                                           create_connection_mock,
                                            init_security_mock):
    init_security_mock.return_value = None
    self.config.set('security', 'keysdir', '/dummy-keysdir')
    connection = security.VerifiedHTTPSConnection("example.com",
      self.config.get('server', 'secured_url_port'), self.config)
    connection._tunnel_host = False
    connection.sock = None
    connection.connect()
    self.assertTrue(wrap_socket_mock.called)

  ### VerifiedHTTPSConnection with no certificates creation
  @patch.object(security.CertificateManager, "initSecurity")
  @patch("socket.create_connection")
  @patch("ssl.wrap_socket")
  def test_Verified_HTTPSConnection_non_secure_connect(self, wrap_socket_mock,
                                                    create_connection_mock,
                                                    init_security_mock):
    connection = security.VerifiedHTTPSConnection("example.com",
      self.config.get('server', 'secured_url_port'), self.config)
    connection._tunnel_host = False
    connection.sock = None
    connection.connect()
    self.assertFalse(init_security_mock.called)

  ### VerifiedHTTPSConnection with two-way SSL authentication enabled
  @patch.object(security.CertificateManager, "initSecurity")
  @patch("socket.create_connection")
  @patch("ssl.wrap_socket")
  def test_Verified_HTTPSConnection_two_way_ssl_connect(self, wrap_socket_mock,
                                                    create_connection_mock,
                                                    init_security_mock):
    wrap_socket_mock.side_effect=ssl.SSLError()
    connection = security.VerifiedHTTPSConnection("example.com",
      self.config.get('server', 'secured_url_port'), self.config)
    connection._tunnel_host = False
    connection.sock = None
    try:
      connection.connect()
    except ssl.SSLError:
      pass
    self.assertTrue(init_security_mock.called)

  ### CachedHTTPSConnection ###

  @patch.object(security.VerifiedHTTPSConnection, "connect")
  def test_CachedHTTPSConnection_connect(self, vhc_connect_mock):
    self.config.set('server', 'hostname', 'dummy.server.hostname')
    self.config.set('server', 'secured_url_port', '443')
    # Testing not connected case
    self.cachedHTTPSConnection.connected = False
    self.cachedHTTPSConnection.connect()
    self.assertTrue(vhc_connect_mock.called)
    vhc_connect_mock.reset_mock()
    # Testing already connected case
    self.cachedHTTPSConnection.connect()
    self.assertFalse(vhc_connect_mock.called)


  @patch.object(security.CachedHTTPSConnection, "connect")
  def test_forceClear(self, connect_mock):
    # Testing if httpsconn instance changed
    old = self.cachedHTTPSConnection.httpsconn
    self.cachedHTTPSConnection.forceClear()
    self.assertNotEqual(old, self.cachedHTTPSConnection.httpsconn)


  @patch.object(security.CachedHTTPSConnection, "connect")
  def test_request(self, connect_mock):
    httpsconn_mock = MagicMock(create = True)
    self.cachedHTTPSConnection.httpsconn = httpsconn_mock

    dummy_request = MagicMock(create = True)
    dummy_request.get_method.return_value = "dummy_get_method"
    dummy_request.get_full_url.return_value = "dummy_full_url"
    dummy_request.get_data.return_value = "dummy_get_data"
    dummy_request.headers = "dummy_headers"

    responce_mock = MagicMock(create = True)
    responce_mock.read.return_value = "dummy responce"
    httpsconn_mock.getresponse.return_value = responce_mock

    # Testing normal case
    responce = self.cachedHTTPSConnection.request(dummy_request)

    self.assertEqual(responce, responce_mock.read.return_value)
    httpsconn_mock.request.assert_called_once_with(
      dummy_request.get_method.return_value,
      dummy_request.get_full_url.return_value,
      dummy_request.get_data.return_value,
      dummy_request.headers)

    # Testing case of exception
    try:
      def side_eff():
        raise Exception("Dummy exception")
      httpsconn_mock.read.side_effect = side_eff
      responce = self.cachedHTTPSConnection.request(dummy_request)
      self.fail("Should raise IOError")
    except Exception, err:
      # Expected
      pass


  ### CertificateManager ###


  @patch("ambari_agent.hostname.hostname")
  def test_getAgentKeyName(self, hostname_mock):
    hostname_mock.return_value = "dummy.hostname"
    self.config.set('security', 'keysdir', '/dummy-keysdir')
    man = CertificateManager(self.config)
    res = man.getAgentKeyName()
    self.assertEquals(res, "/dummy-keysdir/dummy.hostname.key")


  @patch("ambari_agent.hostname.hostname")
  def test_getAgentCrtName(self, hostname_mock):
    hostname_mock.return_value = "dummy.hostname"
    self.config.set('security', 'keysdir', '/dummy-keysdir')
    man = CertificateManager(self.config)
    res = man.getAgentCrtName()
    self.assertEquals(res, "/dummy-keysdir/dummy.hostname.crt")


  @patch("ambari_agent.hostname.hostname")
  def test_getAgentCrtReqName(self, hostname_mock):
    hostname_mock.return_value = "dummy.hostname"
    self.config.set('security', 'keysdir', '/dummy-keysdir')
    man = CertificateManager(self.config)
    res = man.getAgentCrtReqName()
    self.assertEquals(res, "/dummy-keysdir/dummy.hostname.csr")


  def test_getSrvrCrtName(self):
    self.config.set('security', 'keysdir', '/dummy-keysdir')
    man = CertificateManager(self.config)
    res = man.getSrvrCrtName()
    self.assertEquals(res, "/dummy-keysdir/ca.crt")


  @patch("os.path.exists")
  @patch.object(security.CertificateManager, "loadSrvrCrt")
  @patch.object(security.CertificateManager, "getAgentKeyName")
  @patch.object(security.CertificateManager, "genAgentCrtReq")
  @patch.object(security.CertificateManager, "getAgentCrtName")
  @patch.object(security.CertificateManager, "reqSignCrt")
  def test_checkCertExists(self, reqSignCrt_mock, getAgentCrtName_mock,
                           genAgentCrtReq_mock, getAgentKeyName_mock,
                           loadSrvrCrt_mock, exists_mock):
    self.config.set('security', 'keysdir', '/dummy-keysdir')
    getAgentKeyName_mock.return_value = "dummy AgentKeyName"
    getAgentCrtName_mock.return_value = "dummy AgentCrtName"
    man = CertificateManager(self.config)

    # Case when all files exist
    exists_mock.side_effect = [True, True, True]
    man.checkCertExists()
    self.assertFalse(loadSrvrCrt_mock.called)
    self.assertFalse(genAgentCrtReq_mock.called)
    self.assertFalse(reqSignCrt_mock.called)

    # Absent server cert
    exists_mock.side_effect = [False, True, True]
    man.checkCertExists()
    self.assertTrue(loadSrvrCrt_mock.called)
    self.assertFalse(genAgentCrtReq_mock.called)
    self.assertFalse(reqSignCrt_mock.called)
    loadSrvrCrt_mock.reset_mock()

    # Absent agent key
    exists_mock.side_effect = [True, False, True]
    man.checkCertExists()
    self.assertFalse(loadSrvrCrt_mock.called)
    self.assertTrue(genAgentCrtReq_mock.called)
    self.assertFalse(reqSignCrt_mock.called)
    genAgentCrtReq_mock.reset_mock()

    # Absent agent cert
    exists_mock.side_effect = [True, True, False]
    man.checkCertExists()
    self.assertFalse(loadSrvrCrt_mock.called)
    self.assertFalse(genAgentCrtReq_mock.called)
    self.assertTrue(reqSignCrt_mock.called)
    reqSignCrt_mock.reset_mock()



  @patch('urllib2.urlopen')
  @patch.object(security.CertificateManager, "getSrvrCrtName")
  def test_loadSrvrCrt(self, getSrvrCrtName_mock, urlopen_mock):
    read_mock = MagicMock(create=True)
    read_mock.read.return_value = "dummy_cert"
    urlopen_mock.return_value = read_mock
    _, tmpoutfile = tempfile.mkstemp()
    getSrvrCrtName_mock.return_value = tmpoutfile

    man = CertificateManager(self.config)
    man.loadSrvrCrt()

    # Checking file contents
    saved = open(tmpoutfile, 'r').read()
    self.assertEqual(saved, read_mock.read.return_value)

    os.unlink(tmpoutfile)


  @patch("ambari_agent.hostname.hostname")
  @patch('__builtin__.open', create=True, autospec=True)
  @patch.dict('os.environ', {'DUMMY_PASSPHRASE': 'dummy-passphrase'})
  @patch('json.dumps')
  @patch('urllib2.Request')
  @patch('urllib2.urlopen')
  @patch('json.loads')
  def test_reqSignCrt(self, loads_mock, urlopen_mock, request_mock, dumps_mock, open_mock, hostname_mock):
    self.config.set('security', 'keysdir', '/dummy-keysdir')
    self.config.set('security', 'passphrase_env_var_name', 'DUMMY_PASSPHRASE')
    man = CertificateManager(self.config)
    hostname_mock.return_value = "dummy-hostname"

    open_mock.return_value.read.return_value = "dummy_request"
    urlopen_mock.return_value.read.return_value = "dummy_server_request"
    loads_mock.return_value = {
      'result': 'OK',
      'signedCa': 'dummy-crt'
    }

    # Test normal server interaction
    man.reqSignCrt()

    self.assertEqual(dumps_mock.call_args[0][0], {
      'csr'       : 'dummy_request',
      'passphrase' : 'dummy-passphrase'
    })
    self.assertEqual(open_mock.return_value.write.call_args[0][0], 'dummy-crt')

    # Test negative server reply
    dumps_mock.reset_mock()
    open_mock.return_value.write.reset_mock()
    loads_mock.return_value = {
      'result': 'FAIL',
      'signedCa': 'fail-crt'
    }

    # If certificate signing failed, then exception must be raised
    try:
      man.reqSignCrt()
      self.fail()
    except ssl.SSLError:
      pass
    self.assertFalse(open_mock.return_value.write.called)

    # Test connection fail
    dumps_mock.reset_mock()
    open_mock.return_value.write.reset_mock()

    try:
      man.reqSignCrt()
      self.fail("Expected exception here")
    except Exception, err:
      # expected
      pass




  @patch("subprocess.Popen")
  @patch("subprocess.Popen.communicate")
  def test_genAgentCrtReq(self, communicate_mock, popen_mock):
    man = CertificateManager(self.config)
    p = MagicMock(spec=subprocess.Popen)
    p.communicate = communicate_mock
    popen_mock.return_value = p
    man.genAgentCrtReq()
    self.assertTrue(popen_mock.called)
    self.assertTrue(communicate_mock.called)


  @patch.object(security.CertificateManager, "checkCertExists")
  def test_initSecurity(self, checkCertExists_method):
    man = CertificateManager(self.config)
    man.initSecurity()
    self.assertTrue(checkCertExists_method.called)


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

from unittest import TestCase
import logging
import unittest
import subprocess
from mock.mock import patch
from mock.mock import MagicMock
from mock.mock import create_autospec
from ambari_agent.HostCheckReportFileHandler import HostCheckReportFileHandler
from ambari_agent.PackagesAnalyzer import PackagesAnalyzer
from ambari_agent.HostInfo import HostInfo
from ambari_agent.Hardware import Hardware
from ambari_agent.AmbariConfig import AmbariConfig

class TestHostInfo(TestCase):

  logger = logging.getLogger()

  @patch.object(PackagesAnalyzer, 'hasZypper')
  @patch.object(PackagesAnalyzer, 'subprocessWithTimeout')
  def test_analyze_zypper_out(self, spwt_mock, hasZy_mock):
    packageAnalyzer = PackagesAnalyzer()
    stringToRead = """Refreshing service 'susecloud'.
           Loading repository data...
           Reading installed packages...

           S | Name                              | Type    | Version                | Arch   | Repository
           --+-----------------------------------+---------+------------------------+--------+----------------------
           i | ConsoleKit                        | package | 0.2.10-64.65.1         | x86_64 | SLES11-SP1-Updates
           i | gweb                              | package | 2.2.0-99               | noarch | Hortonworks Data Platform Utils Version - HDP-UTILS-1.1.0.15
           i | hadoop                            | package | 1.2.0.1.3.0.0-107      | x86_64 | HDP
           i | hadoop-libhdfs                    | package | 1.2.0.1.3.0.0-107      | x86_64 | HDP
           i | ambari-server                     | package | 1.2.4.9-1              | noarch | Ambari 1.x
           i | hdp_mon_ganglia_addons            | package | 1.2.4.9-1              | noarch | Ambari 1.x
           i | Minimal                           | pattern | 11-38.13.9             | x86_64 | SLES11-SP1"""
    result = {}
    result['out'] = stringToRead
    result['err'] = ""
    result['retCode'] = 0

    spwt_mock.return_value = result
    hasZy_mock.return_value = True
    installedPackages = []
    packageAnalyzer.allInstalledPackages(installedPackages)
    self.assertEqual(7, len(installedPackages))
    self.assertTrue(installedPackages[1][0], "gweb")
    self.assertTrue(installedPackages[3][2], "HDP")
    self.assertTrue(installedPackages[6][1], "11-38.13.9")

  def test_getReposToRemove(self):
    l1 = ["Hortonworks Data Platform Utils Version - HDP-UTILS-1.1.0.15", "Ambari 1.x", "HDP"]
    l2 = ["Ambari", "HDP-UTIL"]
    hostInfo = HostInfo()
    l3 = hostInfo.getReposToRemove(l1, l2)
    self.assertTrue(1, len(l3))
    self.assertEqual(l3[0], "HDP")

    l1 = ["AMBARI.dev-1.x", "HDP-1.3.0"]
    l3 = hostInfo.getReposToRemove(l1, l2)
    self.assertTrue(1, len(l3))
    self.assertEqual(l3[0], "HDP-1.3.0")

  def test_perform_package_analysis(self):
    packageAnalyzer = PackagesAnalyzer()
    installedPackages = [
      ["hadoop-a", "2.3", "HDP"], ["zk", "3.1", "HDP"], ["webhcat", "3.1", "HDP"],
      ["hadoop-b", "2.3", "HDP-epel"], ["epel", "3.1", "HDP-epel"], ["epel-2", "3.1", "HDP-epel"],
      ["hadoop-c", "2.3", "Ambari"], ["ambari-s", "3.1", "Ambari"],
      ["nagios", "2.3", "NAGIOS"], ["rrd", "3.1", "RRD"],
      ["keeper-1", "2.3", "NAGIOS"], ["keeper-2", "3.1", "base"],["def-def.x86", "2.2", "DEF.3"],
      ["def.1", "1.2", "NewDEF"]
    ]
    availablePackages = [
      ["hadoop-d", "2.3", "HDP"], ["zk-2", "3.1", "HDP"], ["pig", "3.1", "HDP"],
      ["epel-3", "2.3", "HDP-epel"], ["hadoop-e", "3.1", "HDP-epel"],
      ["ambari-a", "3.1", "Ambari"],
      ["keeper-3", "3.1", "base"]
    ]

    packagesToLook = ["webhcat", "hadoop", "*-def"]
    reposToIgnore = ["ambari"]
    additionalPackages = ["nagios", "rrd"]

    repos = []
    packageAnalyzer.getInstalledRepos(packagesToLook, installedPackages + availablePackages, reposToIgnore, repos)
    self.assertEqual(3, len(repos))
    expected = ["HDP", "HDP-epel", "DEF.3"]
    for repo in expected:
      self.assertTrue(repo in repos)

    packagesInstalled = packageAnalyzer.getInstalledPkgsByRepo(repos, ["epel"], installedPackages)
    self.assertEqual(5, len(packagesInstalled))
    expected = ["hadoop-a", "zk", "webhcat", "hadoop-b", "def-def.x86"]
    for repo in expected:
      self.assertTrue(repo in packagesInstalled)

    additionalPkgsInstalled = packageAnalyzer.getInstalledPkgsByNames(
        additionalPackages, installedPackages)
    self.assertEqual(2, len(additionalPkgsInstalled))
    expected = ["nagios", "rrd"]
    for additionalPkg in expected:
      self.assertTrue(additionalPkg in additionalPkgsInstalled)

    allPackages = list(set(packagesInstalled + additionalPkgsInstalled))
    self.assertEqual(7, len(allPackages))
    expected = ["hadoop-a", "zk", "webhcat", "hadoop-b", "nagios", "rrd", "def-def.x86"]
    for package in expected:
      self.assertTrue(package in allPackages)

  @patch.object(PackagesAnalyzer, 'hasZypper')
  @patch.object(PackagesAnalyzer, 'subprocessWithTimeout')
  def test_analyze_yum_output(self, subprocessWithTimeout_mock, hasZy_mock):
    packageAnalyzer = PackagesAnalyzer()
    stringToRead = """Loaded plugins: amazon-id, product-id, rhui-lb, security, subscription-manager
                      Updating certificate-based repositories.
                      Installed Packages
                      AMBARI.dev.noarch             1.x-1.el6             installed
                      PyXML.x86_64                  0.8.4-19.el6          @koji-override-0
                      Red_Hat_Enterprise_Linux-Release_Notes-6-en-US.noarch
                              3-7.el6               @koji-override-0
                      hcatalog.noarch               0.11.0.1.3.0.0-107.el6
                                                    @HDP-1.3.0
                      hesiod.x86_64                 3.1.0-19.el6          @koji-override-0/$releasever
                      hive.noarch                   0.11.0.1.3.0.0-107.el6
                                                    @HDP-1.3.0
                      oracle-server-db.x86          1.3.17-2
                                                    @Oracle-11g
                      ambari-log4j.noarch           1.2.5.9-1             @AMBARI.dev-1.x
                      libconfuse.x86_64             2.7-4.el6             @HDP-epel"""
    result = {}
    result['out'] = stringToRead
    result['err'] = ""
    result['retCode'] = 0

    subprocessWithTimeout_mock.return_value = result
    hasZy_mock.return_value = False
    installedPackages = []
    packageAnalyzer.allInstalledPackages(installedPackages)
    self.assertEqual(9, len(installedPackages))
    for package in installedPackages:
      self.assertTrue(package[0] in ["AMBARI.dev.noarch", "PyXML.x86_64", "oracle-server-db.x86",
                                 "Red_Hat_Enterprise_Linux-Release_Notes-6-en-US.noarch",
                                 "hcatalog.noarch", "hesiod.x86_64", "hive.noarch", "ambari-log4j.noarch", "libconfuse.x86_64"])
      self.assertTrue(package[1] in ["1.x-1.el6", "0.8.4-19.el6", "3-7.el6", "3.1.0-19.el6",
                                 "0.11.0.1.3.0.0-107.el6", "1.2.5.9-1", "1.3.17-2", "1.2.5.9-1", "2.7-4.el6"])
      self.assertTrue(package[2] in ["installed", "koji-override-0", "HDP-1.3.0",
                                 "koji-override-0/$releasever", "AMBARI.dev-1.x", "Oracle-11g", "HDP-epel"])

    packages = packageAnalyzer.getInstalledPkgsByNames(["AMBARI", "Red_Hat_Enterprise", "hesiod", "hive"],
                                                       installedPackages)
    self.assertEqual(4, len(packages))
    expected = ["AMBARI.dev.noarch", "Red_Hat_Enterprise_Linux-Release_Notes-6-en-US.noarch",
                                "hesiod.x86_64", "hive.noarch"]
    for package in expected:
      self.assertTrue(package in packages)

    detailedPackages = packageAnalyzer.getPackageDetails(installedPackages, packages)
    self.assertEqual(4, len(detailedPackages))
    for package in detailedPackages:
      self.assertTrue(package['version'] in ["1.x-1.el6", "3-7.el6", "3.1.0-19.el6",
                                            "0.11.0.1.3.0.0-107.el6"])
      self.assertTrue(package['repoName'] in ["installed", "koji-override-0", "HDP-1.3.0",
                                              "koji-override-0/$releasever"])
      self.assertFalse(package['repoName'] in ["AMBARI.dev-1.x"])

  @patch.object(PackagesAnalyzer, 'subprocessWithTimeout')
  def test_analyze_yum_output_err(self, subprocessWithTimeout_mock):
    packageAnalyzer = PackagesAnalyzer()

    result = {}
    result['out'] = ""
    result['err'] = ""
    result['retCode'] = 1

    subprocessWithTimeout_mock.return_value = result
    installedPackages = []
    packageAnalyzer.allInstalledPackages(installedPackages)
    self.assertEqual(installedPackages, [])


  @patch('os.path.exists')
  def test_checkFolders(self, path_mock):
    path_mock.return_value = True
    hostInfo = HostInfo()
    results = []
    existingUsers = [{'name':'a1', 'homeDir':'/home/a1'}, {'name':'b1', 'homeDir':'/home/b1'}]
    hostInfo.checkFolders(["/etc/conf", "/var/lib", "/home/"], ["a1", "b1"], existingUsers, results)
    self.assertEqual(4, len(results))
    names = [i['name'] for i in results]
    for item in ['/etc/conf/a1', '/var/lib/a1', '/etc/conf/b1', '/var/lib/b1']:
      self.assertTrue(item in names)

  @patch('os.path.exists')
  @patch('__builtin__.open')
  def test_checkUsers(self, builtins_open_mock, path_mock):
    builtins_open_mock.return_value = [
      "hdfs:x:493:502:Hadoop HDFS:/usr/lib/hadoop:/bin/bash",
      "zookeeper:x:492:502:ZooKeeper:/var/run/zookeeper:/bin/bash"]
    path_mock.side_effect = [True, False]

    hostInfo = HostInfo()
    results = []
    hostInfo.checkUsers(["zookeeper", "hdfs"], results)
    self.assertEqual(2, len(results))
    newlist = sorted(results, key=lambda k: k['name'])
    self.assertTrue(newlist[0]['name'], "hdfs")
    self.assertTrue(newlist[1]['name'], "zookeeper")
    self.assertTrue(newlist[0]['homeDir'], "/usr/lib/hadoop")
    self.assertTrue(newlist[1]['homeDir'], "/var/run/zookeeper")
    self.assertTrue(newlist[0]['status'], "Available")
    self.assertTrue(newlist[1]['status'], "Invalid home directory")


  @patch.object(HostInfo, 'get_os_type')
  @patch('os.umask')
  @patch.object(HostCheckReportFileHandler, 'writeHostCheckFile')
  @patch.object(PackagesAnalyzer, 'allAvailablePackages')
  @patch.object(PackagesAnalyzer, 'allInstalledPackages')
  @patch.object(PackagesAnalyzer, 'getPackageDetails')
  @patch.object(PackagesAnalyzer, 'getInstalledPkgsByNames')
  @patch.object(PackagesAnalyzer, 'getInstalledPkgsByRepo')
  @patch.object(PackagesAnalyzer, 'getInstalledRepos')
  @patch.object(HostInfo, 'checkUsers')
  @patch.object(HostInfo, 'checkLiveServices')
  @patch.object(HostInfo, 'javaProcs')
  @patch.object(HostInfo, 'checkFolders')
  @patch.object(HostInfo, 'etcAlternativesConf')
  @patch.object(HostInfo, 'hadoopVarRunCount')
  @patch.object(HostInfo, 'hadoopVarLogCount')
  def test_hostinfo_register_suse(self, hvlc_mock, hvrc_mock, eac_mock, cf_mock, jp_mock,
                             cls_mock, cu_mock, gir_mock, gipbr_mock, gipbn_mock,
                             gpd_mock, aip_mock, aap_mock, whcf_mock, os_umask_mock, get_os_type_mock):
    hvlc_mock.return_value = 1
    hvrc_mock.return_value = 1
    gipbr_mock.return_value = ["pkg1"]
    gipbn_mock.return_value = ["pkg2"]
    gpd_mock.return_value = ["pkg1", "pkg2"]
    get_os_type_mock.return_value = "suse"

    hostInfo = HostInfo()
    dict = {}
    hostInfo.register(dict, False, False)
    self.assertFalse(gir_mock.called)
    self.assertFalse(gpd_mock.called)
    self.assertFalse(aip_mock.called)
    self.assertFalse(aap_mock.called)
    self.assertTrue(os_umask_mock.called)
    self.assertFalse(whcf_mock.called)

    self.assertTrue(0 == len(dict['installedPackages']))
    self.assertTrue('agentTimeStampAtReporting' in dict['hostHealth'])


  @patch.object(HostInfo, 'get_os_type')
  @patch('os.umask')
  @patch.object(HostCheckReportFileHandler, 'writeHostCheckFile')
  @patch.object(PackagesAnalyzer, 'allAvailablePackages')
  @patch.object(PackagesAnalyzer, 'allInstalledPackages')
  @patch.object(PackagesAnalyzer, 'getPackageDetails')
  @patch.object(PackagesAnalyzer, 'getInstalledPkgsByNames')
  @patch.object(PackagesAnalyzer, 'getInstalledPkgsByRepo')
  @patch.object(PackagesAnalyzer, 'getInstalledRepos')
  @patch.object(HostInfo, 'checkUsers')
  @patch.object(HostInfo, 'checkLiveServices')
  @patch.object(HostInfo, 'javaProcs')
  @patch.object(HostInfo, 'checkFolders')
  @patch.object(HostInfo, 'etcAlternativesConf')
  @patch.object(HostInfo, 'hadoopVarRunCount')
  @patch.object(HostInfo, 'hadoopVarLogCount')
  @patch.object(HostInfo, 'checkIptables')
  def test_hostinfo_register(self, cit_mock, hvlc_mock, hvrc_mock, eac_mock, cf_mock, jp_mock,
                             cls_mock, cu_mock, gir_mock, gipbr_mock, gipbn_mock,
                             gpd_mock, aip_mock, aap_mock, whcf_mock, os_umask_mock, get_os_type_mock):
    cit_mock.return_value = True
    hvlc_mock.return_value = 1
    hvrc_mock.return_value = 1
    gipbr_mock.return_value = ["pkg1"]
    gipbn_mock.return_value = ["pkg2"]
    gpd_mock.return_value = ["pkg1", "pkg2"]
    get_os_type_mock.return_value = "redhat"

    hostInfo = HostInfo()
    dict = {}
    hostInfo.register(dict, True, True)
    self.verifyReturnedValues(dict)

    hostInfo.register(dict, True, False)
    self.verifyReturnedValues(dict)

    hostInfo.register(dict, False, True)
    self.verifyReturnedValues(dict)
    self.assertTrue(os_umask_mock.call_count == 2)

    hostInfo = HostInfo()
    dict = {}
    hostInfo.register(dict, False, False)
    self.assertTrue(gir_mock.called)
    self.assertTrue(gpd_mock.called)
    self.assertTrue(aip_mock.called)
    self.assertTrue(cit_mock.called)

    for existingPkg in ["pkg1", "pkg2"]:
      self.assertTrue(existingPkg in dict['installedPackages'])
    args, kwargs = gpd_mock.call_args_list[0]
    for existingPkg in ["pkg1", "pkg2"]:
      self.assertTrue(existingPkg in args[1])

  def verifyReturnedValues(self, dict):
    hostInfo = HostInfo()
    self.assertEqual(dict['alternatives'], [])
    self.assertEqual(dict['stackFoldersAndFiles'], [])
    self.assertEqual(dict['existingUsers'], [])
    self.assertEqual(dict['existingRepos'][0], hostInfo.RESULT_UNAVAILABLE)
    self.assertEqual(dict['installedPackages'], [])
    self.assertTrue(dict['iptablesIsRunning'])

  @patch("os.path.exists")
  @patch("os.path.islink")
  @patch("os.path.isdir")
  @patch("os.path.isfile")
  def test_dirType(self, os_path_isfile_mock, os_path_isdir_mock, os_path_islink_mock, os_path_exists_mock):
    host = HostInfo()

    os_path_exists_mock.return_value = False
    result = host.dirType("/home")
    self.assertEquals(result, 'not_exist')

    os_path_exists_mock.return_value = True
    os_path_islink_mock.return_value = True
    result = host.dirType("/home")
    self.assertEquals(result, 'sym_link')

    os_path_exists_mock.return_value = True
    os_path_islink_mock.return_value = False
    os_path_isdir_mock.return_value = True
    result = host.dirType("/home")
    self.assertEquals(result, 'directory')

    os_path_exists_mock.return_value = True
    os_path_islink_mock.return_value = False
    os_path_isdir_mock.return_value = False
    os_path_isfile_mock.return_value = True
    result = host.dirType("/home")
    self.assertEquals(result, 'file')

    os_path_exists_mock.return_value = True
    os_path_islink_mock.return_value = False
    os_path_isdir_mock.return_value = False
    os_path_isfile_mock.return_value = False
    result = host.dirType("/home")
    self.assertEquals(result, 'unknown')


  @patch("os.path.exists")
  @patch("glob.glob")
  def test_hadoopVarRunCount(self, glob_glob_mock, os_path_exists_mock):
    hostInfo = HostInfo()

    os_path_exists_mock.return_value = True
    glob_glob_mock.return_value = ['pid1','pid2','pid3']
    result = hostInfo.hadoopVarRunCount()
    self.assertEquals(result, 3)

    os_path_exists_mock.return_value = False
    result = hostInfo.hadoopVarRunCount()
    self.assertEquals(result, 0)


  @patch("os.path.exists")
  @patch("glob.glob")
  def test_hadoopVarLogCount(self, glob_glob_mock, os_path_exists_mock):
    hostInfo = HostInfo()

    os_path_exists_mock.return_value = True
    glob_glob_mock.return_value = ['log1','log2']
    result = hostInfo.hadoopVarLogCount()
    self.assertEquals(result, 2)

    os_path_exists_mock.return_value = False
    result = hostInfo.hadoopVarLogCount()
    self.assertEquals(result, 0)


  @patch("os.listdir", create=True, autospec=True)
  @patch("__builtin__.open", create=True, autospec=True)
  @patch("pwd.getpwuid", create=True, autospec=True)
  def test_javaProcs(self, pwd_getpwuid_mock, buitin_open_mock, os_listdir_mock):
    hostInfo = HostInfo()
    openRead = MagicMock()
    openRead.read.return_value = '/java/;/hadoop/'
    buitin_open_mock.side_effect = [openRead, ['Uid: 22']]
    pwuid = MagicMock()
    pwd_getpwuid_mock.return_value = pwuid
    pwuid.pw_name = 'user'
    os_listdir_mock.return_value = ['1']
    list = []
    hostInfo.javaProcs(list)

    self.assertEquals(list[0]['command'], '/java/;/hadoop/')
    self.assertEquals(list[0]['pid'], 1)
    self.assertTrue(list[0]['hadoop'])
    self.assertEquals(list[0]['user'], 'user')


  @patch("subprocess.Popen")
  @patch.object(Hardware, 'extractMountInfo')
  def test_osdiskAvailableSpace(self, extract_mount_info_mock, subproc_popen_mock):
    hostInfo = HostInfo()
    p = MagicMock()
    p.communicate.return_value = ['some']
    subproc_popen_mock.return_value = p
    extract_mount_info_mock.return_value = {'info' : 'info'}
    result = hostInfo.osdiskAvailableSpace('')

    self.assertTrue(result['info'], 'info')

    p.communicate.return_value = ''
    result = hostInfo.osdiskAvailableSpace('')

    self.assertEquals(result, {})


  @patch.object(HostInfo, "get_os_type")
  @patch("subprocess.Popen")
  def test_checkLiveServices(self, subproc_popen, get_os_type_method):
    hostInfo = HostInfo()
    p = MagicMock()
    p.returncode = 0
    p.communicate.return_value = ('', 'err')
    subproc_popen.return_value = p
    result = []
    get_os_type_method.return_value = 'redhat'
    hostInfo.checkLiveServices(['service1'], result)

    self.assertEquals(result[0]['status'], 'Healthy')
    self.assertEquals(result[0]['name'], 'service1')
    self.assertEquals(result[0]['desc'], '')
    self.assertEquals(str(subproc_popen.call_args_list),
                      "[call(['/sbin/service', 'service1', 'status'], stderr=-1, stdout=-1)]")

    p.returncode = 1
    p.communicate.return_value = ('out', 'err')
    result = []
    hostInfo.checkLiveServices(['service1'], result)

    self.assertEquals(result[0]['status'], 'Unhealthy')
    self.assertEquals(result[0]['name'], 'service1')
    self.assertEquals(result[0]['desc'], 'out')

    p.communicate.return_value = ('', 'err')
    result = []
    hostInfo.checkLiveServices(['service1'], result)

    self.assertEquals(result[0]['status'], 'Unhealthy')
    self.assertEquals(result[0]['name'], 'service1')
    self.assertEquals(result[0]['desc'], 'err')

    p.communicate.return_value = ('', 'err', '')
    result = []
    hostInfo.checkLiveServices(['service1'], result)

    self.assertEquals(result[0]['status'], 'Unhealthy')
    self.assertEquals(result[0]['name'], 'service1')
    self.assertTrue(len(result[0]['desc']) > 0)


  @patch("os.path.exists")
  @patch("os.listdir", create=True, autospec=True)
  @patch("os.path.islink")
  @patch("os.path.realpath")
  def test_etcAlternativesConf(self, os_path_realpath_mock, os_path_islink_mock, os_listdir_mock, os_path_exists_mock):
    hostInfo = HostInfo()
    os_path_exists_mock.return_value = False
    result = hostInfo.etcAlternativesConf('',[])

    self.assertEquals(result, [])

    os_path_exists_mock.return_value = True
    os_listdir_mock.return_value = ['config1']
    os_path_islink_mock.return_value = True
    os_path_realpath_mock.return_value = 'real_path_to_conf'
    result = []
    hostInfo.etcAlternativesConf('project',result)

    self.assertEquals(result[0]['name'], 'config1')
    self.assertEquals(result[0]['target'], 'real_path_to_conf')


  @patch("subprocess.Popen")
  def test_checkIptables(self, subproc_popen_mock):
    hostInfo = HostInfo()
    p = MagicMock()
    p.returncode = 0
    subproc_popen_mock.return_value = p
    result = hostInfo.checkIptables()

    self.assertTrue(result)

    p.returncode = 1
    result = hostInfo.checkIptables()

    self.assertFalse(result)


if __name__ == "__main__":
  unittest.main()

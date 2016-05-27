#!/usr/bin/env python
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

import sys
from resource_management import *
import shutil, tempfile, subprocess, traceback
from resource_management.core import shell

openr_location = 'http://104.196.87.250/msft-r/'
rserver_location = 'http://104.196.87.250/msft-r/'
#openr_location = '/ambari/contrib/msr/'
#rserver_location = '/ambari/contrib/msr/'

class MicrosoftR(Script):
  def install(self, env):
    print 'Install R Server'
    tmp_dir = tempfile.mkdtemp()
    print 'Using temp dir: ' + tmp_dir
    try:
      print 'Download R Open'
      if "http" in openr_location:
        subprocess.call(['wget', openr_location + 'MRO-for-MRS-8.0.0.el6.x86_64.rpm'], cwd=tmp_dir)
      else:
        shell.call('cp ' + openr_location + '/MRO-for-MRS-8.0.0.el6.x86_64.rpm ' + tmp_dir)

      print 'Install R Open'
      subprocess.call(['yum', 'install', '-y', 'MRO-for-MRS-8.0.0.el6.x86_64.rpm'], cwd=tmp_dir)

      print 'Download R Server'
      if "http" in rserver_location:
        subprocess.call(['wget', rserver_location + 'Microsoft-R-Server-8.0.0-RHEL6.tar.gz'], cwd=tmp_dir)
      else:
        shell.call('cp ' + rserver_location + 'Microsoft-R-Server-8.0.0-RHEL6.tar.gz ' + tmp_dir)

      print 'Install R Server'
      subprocess.call(['tar', '-xzvf', 'Microsoft-R-Server-8.0.0-RHEL6.tar.gz'], cwd=tmp_dir)
      subprocess.call([tmp_dir + '/rrent/install.sh', '-a', '-y', '-p', '/usr/lib64/MRO-for-MRS-8.0.0/R-3.2.2'], cwd = tmp_dir + '/rrent')

      print 'Create symlink to hadoop library'
      _, libhdfs = shell.call('find /usr/hdp/ -name libhdfs.so')
      shell.call('ln -s ' + libhdfs + ' /usr/lib64/libhdfs.so')

      print 'Create /share on hadoop' # This is not strictly required, but needed to run the example
      shell.call('sudo -u hdfs hadoop fs -mkdir -p /share')
      shell.call('sudo -u hdfs hadoop fs -chmod uog+rwx /share')

      print 'Configure R Server for the ambari-qa user'
      shell.call('sudo -u hdfs hadoop fs -mkdir -p /user/RevoShare/ambari-qa')
      shell.call('sudo -u hdfs hadoop fs -chmod uog+rwx /user/RevoShare/ambari-qa')
      shell.call('mkdir -p /var/RevoShare/ambari-qa')
      shell.call('chmod oug+rwx /var/RevoShare/ambari-qa')
      shell.call('sudo -u ambari-qa echo ". /usr/lib64/MRS-8.0/scripts/RevoHadoopEnvVars.site" >> ~ambari-qa/.bashrc')

      print 'Installed R Server'
    except Exception as ex:
      print "An error occured while installing Microsoft R"
      traceback.print_exc()
    finally:
      print 'Cleaning up'
      shutil.rmtree(tmp_dir)

  def configure(self, env):
    print 'Configure R Server. Nothing to do.'

if __name__ == "__main__":
  MicrosoftR().execute()

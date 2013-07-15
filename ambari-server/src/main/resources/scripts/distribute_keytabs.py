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

from optparse import OptionParser
import sys
import subprocess
import re
import os
import glob


def sendTarfileToHosts(hostnames,identity_file,krb5_conf):
  user = "root"
  for i in range(len(hostnames)):
    remotehost, localfile = hostnames[i]
    os.system('scp -i "%s" -oStrictHostKeyChecking=no "%s" "%s"@"%s":/' % (identity_file, localfile, user, remotehost))
    if krb5_conf:
      os.system('scp -i "%s" -oStrictHostKeyChecking=no "%s" "%s"@"%s":%s' % (identity_file, krb5_conf, user, remotehost, krb5_conf))
    sshCommand = "tar xvf /" + os.path.basename(localfile) + " -C /"
    ssh = subprocess.Popen(["ssh", "-i", "%s" % identity_file, "-oStrictHostKeyChecking=no","%s" % remotehost, sshCommand],shell=False,stdout=subprocess.PIPE,stderr=subprocess.PIPE)
    result = ssh.stdout.readlines()
    if result == []:
      error = ssh.stderr.readlines()
      print >>sys.stderr, "ERROR: %s" % error
    else:
      print result


def getHostnames(filenames,regex):
  return [(hostname.group(1),tarfile) for tarfile in filenames for hostname in (regex(tarfile),) if hostname]


def main():
  parser = OptionParser()
  parser.add_option('-d','--directory', help='Path to the Directory containing tar files',dest='dirPath', default='.')
  parser.add_option('-i','--identity-file', help='Path to the identity file',dest='identity_file', default='/tmp/ec2-keypair')
  parser.add_option('-k','--krb5-conf', help='Path to the krb5_conf file',dest='krb5_conf')
  (options, args) = parser.parse_args()
  pattern = options.dirPath + "/*.tar"
  tarfiles = glob.glob(pattern)
  hostnames_regex = re.compile("(?<=keytabs_)(.*)(?=.tar)").search
  hostnames = getHostnames(tarfiles,hostnames_regex)
  sendTarfileToHosts(hostnames,options.identity_file,options.krb5_conf)


if __name__ == '__main__':
  main()



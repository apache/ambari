#!/usr/bin/env python2
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

import os
import sys
import logging
import subprocess
from optparse import OptionParser

USAGE = "Usage: %prog [OPTION]... URL"
DESCRIPTION = "URL should point to full tar.gz location e.g.: https://public-repo-1.hortonworks.com/something/ambari-server.tar.gz"

logger = logging.getLogger("install_ambari_tarball")

PREINST_SCRIPT = "preinst"
PRERM_SCRIPT = "prerm"
POSTINST_SCRIPT = "postinst"
POSTRM_SCRIPT = "postrm"

ROOT_FOLDER_ENV_VARIABLE = "AMBARI_ROOT_FOLDER"

class Utils:
  verbose = False
  @staticmethod
  def os_call(command, logoutput=None, env={}):
    shell = not isinstance(command, list)
    print_output = logoutput==True or (logoutput==None and Utils.verbose)
    
    if not print_output:
      stdout = subprocess.PIPE
      stderr = subprocess.STDOUT
    else:
      stdout = stderr = None
    
    logger.info("Running '{0}'".format(command))
    proc = subprocess.Popen(command, shell=shell, stdout=stdout, stderr=stderr, env=env)
      
    if not print_output:
      out = proc.communicate()[0].strip('\n')
    else:
      proc.wait()
      out = None
      
    code = proc.returncode
  
    if code:
      err_msg = ("Execution of '%s'\n returned %d. %s") % (command, code, out)
      raise OsCallFailure(err_msg)
      
    return out
  
class OsCallFailure(RuntimeError):
  pass

class Installer:
  def __init__(self, archive_url, root_folder, verbose):
    splited_url = archive_url.split('/')
    self.archive_name = splited_url[-1]
    self.base_url = '/'.join(splited_url[0:-1])
    self.root_folder = root_folder
    self.verbose = verbose
    
  def download_files(self):
    for name in [ self.archive_name, PREINST_SCRIPT, PRERM_SCRIPT, POSTINST_SCRIPT, POSTRM_SCRIPT]: 
      url = "{0}/{1}".format(self.base_url, name)
      logger.info("Downloading {0}".format(url))
      Utils.os_call(["wget", "-O", name, url])
    
  def run(self):
    self.download_files()
    
    self.run_script(PRERM_SCRIPT, ["remove"]) # in case we are upgrading
    self.run_script(POSTRM_SCRIPT, ["remove"]) # in case we are upgrading
    
    self.run_script(PREINST_SCRIPT, ["install"])
    self.extract_archive()
    self.run_script(POSTINST_SCRIPT, ["configure"])
    
  def run_script(self, script_name, args):
    bash_args = []
    if self.verbose:
      bash_args.append("-x")
      
    Utils.os_call(["bash"] + bash_args + [script_name] + args, env={ROOT_FOLDER_ENV_VARIABLE: self.root_folder})
    

class TargzInstaller(Installer):
  def extract_archive(self):
    Utils.os_call(['tar','--no-same-owner', '-xvf', self.archive_name, '-C', self.root_folder+os.sep], logoutput=False)


class Runner:
  def parse_opts(self):
    parser = OptionParser(usage=USAGE, description=DESCRIPTION)
    parser.add_option("-v", "--verbose", dest="verbose", action="store_true",
                      help="sets output level to more detailed")
    parser.add_option("-r", "--root-folder", dest="root_folder", default="/",
                      help="root folder to install Ambari to. E.g.: /opt")
    
    (self.options, args) = parser.parse_args()
    
    if len(args) != 1:
      help = parser.print_help()
      sys.exit(1)
      
    self.url = args[0]
    
  @staticmethod
  def setup_logger(verbose):
    logging_level = logging.DEBUG if verbose else logging.INFO
    logger.setLevel(logging_level)

    formatter = logging.Formatter('%(asctime)s %(levelname)s %(message)s')
    stdout_handler = logging.StreamHandler(sys.stdout)
    stdout_handler.setLevel(logging_level)
    stdout_handler.setFormatter(formatter)
    logger.addHandler(stdout_handler)
    
  def run(self):
    self.parse_opts()
    Runner.setup_logger(self.options.verbose)
    Utils.verbose = self.options.verbose
    
    # TODO: check if ends with tar.gz?
    targz_installer = TargzInstaller(self.url, self.options.root_folder, self.options.verbose)
    targz_installer.run()
      
if __name__ == '__main__':
  Runner().run()
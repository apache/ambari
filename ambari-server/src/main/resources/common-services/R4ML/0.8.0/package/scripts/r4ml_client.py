#!/usr/bin/python
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

import os
from ambari_commons import subprocess32
from resource_management import *
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.core.exceptions import ClientComponentHasNoStatus
from resource_management.core.logger import Logger

class R4MLClient(Script):

  def configure(selfself, env):
    import params
    env.set_params(params)

  def pre_upgrade_restart(self, env, upgrade_type=None):
    import params

    env.set_params(params)
    if params.version and check_stack_feature(StackFeature.ROLLING_UPGRADE, params.version):
      Logger.info("Executing R4ML Client Stack Upgrade pre-restart")
      stack_select.select_packages(params.version)

  def stack_upgrade_save_new_config(self, env):
    import params
    env.set_params(params)

    conf_select_name = "r4ml"
    base_dir = os.path.dirname(os.path.dirname(os.path.realpath(__file__)))
    config_dir = self.get_config_dir_during_stack_upgrade(env, base_dir, conf_select_name)

    if config_dir:
      Logger.info("stack_upgrade_save_new_config(): Calling conf-select on %s using version %s" % (conf_select_name, str(params.version)))

      # Because this script was called from ru_execute_tasks.py which already enters an Environment with its own basedir,
      # must change it now so this function can find the Jinja Templates for the service.
      env.config.basedir = base_dir
      self.configure(env, config_dir=config_dir)

  def checkPackage(self, packages):
    try :
      checked_call("sudo yum list " + packages)
    except Exception as e:
      # ignore
      print e
      return 1
    return 0

  def setupEpelRepo(self, params):
    epel_installed = False
    import urllib
    code = 0
    try :
      code = subprocess32.call(["sudo", "which", "R"])
    except Exception as e :
      Logger.error(str(e))
    if code != 0 :
      # try to set up R repo
      code = self.checkPackage("R")
      if code != 0 :
        # R does not exist in any repo
        code = self.checkPackage("epel-release")
        if code != 0 :
          if params.epel != "" :
            # proceed to install EPEL
            try :
              urllib.urlretrieve(params.epel, "/tmp/epel.rpm")
              Execute(("yum", "install", "/tmp/epel.rpm", "-y"), sudo=True)
              epel_installed = True
            except Exception as e :
              Logger.error(str(e))
              # it is ok to fail to download as it can be an offline install case
        else :
          Execute(("yum", "install", "epel-release", "-y"), sudo=True)
          epel_installed = True

      # check another two dependencies
      code = self.checkPackage("texinfo-tex texlive-epsf")
      if code != 0 :
        # download from centos mirror
        if params.centos != "" :
          try :
            import re
            urllib.urlretrieve(params.centos, "/tmp/index")
            s = open("/tmp/index", "r").read()
            tex = re.search('texinfo-tex(.+)rpm(?=\")', s).group(0)
            epsf = re.search('texlive-epsf-svn(.+)rpm(?=\")', s).group(0)
            urllib.urlretrieve(params.centos + tex, "/tmp/tex.rpm")
            urllib.urlretrieve(params.centos + epsf, "/tmp/epsf.rpm")
            Execute(("yum", "install", "/tmp/epsf.rpm", "/tmp/tex.rpm", "-y"), sudo=True)
          except Exception as e :
            Logger.error(str(e))
        else :
          Logger.error("Dependent packages texinfo-tex and texlive-epsf are not found in any repos. Enable RedHat Optional Packages repo or install these two packages manually before retry.")
          exit(1)
      # install R now
      Execute(("yum", "install", "R", "-y"), sudo=True)
    return epel_installed

  def setupRrepo(self, params):
    import re
    if params.baseurl != "http://" :
      # assume this is a local install
      File(format(params.rrepo),
           action="delete")

      File(format(params.rrepo),
           content = StaticFile("localr.repo"),
           mode = 0644)
      Execute(("sed", "-i", "s/URLXXXX/" + re.sub('\$', '\$', re.sub('/', '\/', params.baseurl)) + "/g ", params.rrepo),
              sudo=True)
      Logger.info("Local install R from %s." %params.baseurl)
      # install R now
      Execute(("yum", "install", "R", "-y"), sudo=True)
      return False
    else :
      return self.setupEpelRepo(params)

  def install(self, env):
    import params
    env.set_params(params)

    # set up R repo
    epel_installed = self.setupRrepo(params)

    # install R and R4ML
    self.install_packages(env)

    # remove epel-release repo installed above as R has been installed
    if epel_installed :
      Execute(("yum", "remove", "epel-release", "-y"), sudo=True)
    else :
      if (os.path.exists(params.rrepo)) :
        File(format(params.rrepo),
             action="delete")

    # install several R packages that will be used by R4ML functions
    installR = params.exec_tmp_dir + "/Install.R"
    File(format(installR),
         content = StaticFile("Install.R"),
         mode = 0755)

    if (params.baseurl != "http://"):
      import re
      Execute(("sed", "-i", "s/repos=c(.*/repos=c(\"" + re.sub('\$', '\$', re.sub('/', '\/', params.baseurl)) + "\"))/g", installR), sudo=True)

    # install the dependent packages
    packages = ["R6", "uuid", "survival"]
    for pkg in packages :
      Execute(("Rscript", installR, pkg), sudo=True)

    # set up configuration file
    Directory(params.r4ml_conf_dir,
              create_parents=True,
              action="create",
              mode=0755)

    File(format("{r4ml_conf_dir}/Renviron"),
         mode=0755,
         content = InlineTemplate(params.Renviron_template))

    # install R4ML package to /usr/iop/current/r4ml-client/R/lib directory
    Directory(format(params.r4ml_home + "/R/lib"),
              action="create",
              create_parents=True,
              mode=0755)

    checked_call(format("sudo R_LIBS={spark_home}/R/lib R CMD INSTALL --install-tests --library={r4ml_home}/R/lib {r4ml_home}/R4ML_*.tar.gz"))

  def status(self, env):
    raise ClientComponentHasNoStatus()

if __name__ == "__main__":
  R4MLClient().execute()


from resource_management.providers.package import PackageProvider
from subprocess import STDOUT, PIPE, check_call

INSTALL_CMD = "/usr/bin/yum -d 0 -e 0 -y install %s"
REMOVE_CMD = "/usr/bin/yum -d 0 -e 0 -y erase %s"

class YumProvider(PackageProvider):
  def install_package(self, name):
    return 0 == check_call(INSTALL_CMD % (name),
                      shell=True, stdout=PIPE, stderr=STDOUT)

  def upgrade_package(self, name):
    return self.install_package(name)
  
  def remove_package(self, name):
    return 0 == check_call(REMOVE_CMD % (name),
                           shell=True, stdout=PIPE, stderr=STDOUT)    

from resource_management.core.providers.package import PackageProvider
from resource_management.core import shell

INSTALL_CMD = "/usr/bin/yum -d 0 -e 0 -y install %s"
REMOVE_CMD = "/usr/bin/yum -d 0 -e 0 -y erase %s"

class YumProvider(PackageProvider):
  def install_package(self, name):
    shell.checked_call(INSTALL_CMD % (name))

  def upgrade_package(self, name):
    return self.install_package(name)
  
  def remove_package(self, name):
    shell.checked_call(REMOVE_CMD % (name))    

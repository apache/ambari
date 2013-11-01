from resource_management.core.base import Fail
from resource_management.core.providers import Provider


class PackageProvider(Provider):
  def __init__(self, *args, **kwargs):
    super(PackageProvider, self).__init__(*args, **kwargs)   
  
  def install_package(self, name, version):
    raise NotImplementedError()
  def remove_package(self, name):
    raise NotImplementedError()
  def upgrade_package(self, name, version):
    raise NotImplementedError()

  def action_install(self):
    package_name = self.get_package_name_with_version()
    self.log.info("Installing package %s", package_name)
    self.install_package(package_name)

  def action_upgrade(self):
    package_name = self.get_package_name_with_version()
    self.log.info("Upgrading package %s", package_name)
    self.upgrade_package(package_name)

  def action_remove(self):
    package_name = self.get_package_name_with_version()
    self.log.info("Removing package %s", package_name)
    self.remove_package(package_name)

  def get_package_name_with_version(self):
    if self.resource.version:
      return self.resource.package_name + '-' + self.resource.version
    else:
      return self.resource.package_name
    

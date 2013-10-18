from resource_management.base import Fail
from resource_management.providers import Provider


class PackageProvider(Provider):
  def __init__(self, *args, **kwargs):
    super(PackageProvider, self).__init__(*args, **kwargs)
    self.get_current_status()

  def get_current_status(self):
    raise NotImplementedError()

  def install_package(self, name, version):
    raise NotImplementedError()

  def remove_package(self, name):
    raise NotImplementedError()

  def purge_package(self, name):
    raise NotImplementedError()

  def upgrade_package(self, name, version):
    raise NotImplementedError()

  def action_install(self):
    if self.resource.version != None and self.resource.version != self.current_version:
      install_version = self.resource.version
    elif self.current_version is None:
      install_version = self.candidate_version
    else:
      return

    if not install_version:
      raise Fail(
        "No version specified, and no candidate version available for package %s." % self.resource.package_name)

    self.log.info(
      "Install %s version %s (resource %s, current %s, candidate %s) location %s",
      self.resource.package_name, install_version, self.resource.version,
      self.current_version, self.candidate_version, self.resource.location)

    status = self.install_package(self.resource.location, install_version)
    if status:
      self.resource.updated()

  def action_upgrade(self):
    if self.current_version != self.candidate_version:
      orig_version = self.current_version or "uninstalled"
      self.log.info("Upgrading %s from version %s to %s",
                    str(self.resource), orig_version, self.candidate_version)

      status = self.upgrade_package(self.resource.location,
                                    self.candidate_version)
      if status:
        self.resource.updated()

  def action_remove(self):
    if self.current_version:
      self.log.info("Remove %s version %s", self.resource.package_name,
                    self.current_version)
      self.remove_package(self.resource.package_name)
      self.resource.updated()

  def action_purge(self):
    if self.current_version:
      self.log.info("Purging %s version %s", self.resource.package_name,
                    self.current_version)
      self.purge_package(self.resource.package_name)
      self.resource.updated()

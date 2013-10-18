from resource_management.providers.package import PackageProvider
import yum


class DummyCallback(object):
  def event(self, state, data=None):
    pass


class YumProvider(PackageProvider):
  def get_current_status(self):
    self.candidate_version = None
    self.current_version = None
    yb = yum.YumBase()
    yb.doConfigSetup()
    yb.doTsSetup()
    yb.doRpmDBSetup()
    for pkg in yb.rpmdb.returnPackages():
      if pkg.name == self.resource.package_name:
        self.current_version = pkg.version
        self.log.debug("Current version of %s is %s" % (
        self.resource.package_name, self.current_version))
    searchlist = ['name', 'version']
    args = [self.resource.package_name]
    matching = yb.searchPackages(searchlist, args)
    for po in matching:
      if po.name == self.resource.package_name:
        self.candidate_version = po.version
        self.log.debug("Candidate version of %s is %s" % (
        self.resource.package_name, self.current_version))

  def install_package(self, name, version):
    yb = yum.YumBase()
    yb.doGenericSetup()
    yb.doRepoSetup()
    #TODO: Handle locks not being available
    yb.doLock()
    yb.install(pattern=name)
    yb.buildTransaction()
    #yb.conf.setattr('assumeyes',True)
    yb.processTransaction(callback=DummyCallback())
    yb.closeRpmDB()
    yb.doUnlock()

  def upgrade_package(self, name, version):
    return self.install_package(name, version)

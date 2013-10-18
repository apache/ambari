import re
from subprocess import check_call, Popen, PIPE, STDOUT
from resource_management.providers.package import PackageProvider

VERSION_RE = re.compile(r'\S\S(.*)\/(.*)-(.*)-py(.*).egg\S')
BEST_MATCH_RE = re.compile(r'Best match: (.*) (.*)\n')


class EasyInstallProvider(PackageProvider):
  def get_current_status(self):
    proc = Popen(["python", "-c", "import %s; print %s.__path__" % (
    self.resource.package_name, self.resource.package_name)], stdout=PIPE,
                 stderr=STDOUT)
    path = proc.communicate()[0]
    if proc.wait() != 0:
      self.current_version = None
    else:
      match = VERSION_RE.search(path)
      if match:
        self.current_version = match.group(3)
      else:
        self.current_version = "unknown"

  @property
  def candidate_version(self):
    if not hasattr(self, '_candidate_version'):
      proc = Popen(
        [self.easy_install_binary_path, "-n", self.resource.package_name],
        stdout=PIPE, stderr=STDOUT)
      out = proc.communicate()[0]
      res = proc.wait()
      if res != 0:
        self.log.warning(
          "easy_install check returned a non-zero result (%d) %s" % (
          res, self.resource))
        #     self._candidate_version = None
      # else:
      match = BEST_MATCH_RE.search(out)
      if not match:
        self._candidate_version = None
      else:
        self._candidate_version = match.group(2)
    return self._candidate_version

  @property
  def easy_install_binary_path(self):
    return "easy_install"

  def install_package(self, name, version):
    check_call(
      [self.easy_install_binary_path, "-U", "%s==%s" % (name, version)],
      stdout=PIPE, stderr=STDOUT)

  def upgrade_package(self, name, version):
    self.install_package(name, version)

  def remove_package(self, name):
    check_call([self.easy_install_binary_path, "-m", name])

  def purge_package(self, name):
    self.remove_package(name)

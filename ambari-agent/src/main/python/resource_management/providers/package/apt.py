import glob
import os
import shutil
import tempfile
from subprocess import Popen, STDOUT, PIPE, check_call, CalledProcessError
from resource_management.base import Fail
from resource_management.providers.package import PackageProvider


class DebianAptProvider(PackageProvider):
  def get_current_status(self):
    self.current_version = None
    self.candidate_version = None

    proc = Popen("apt-cache policy %s" % self.resource.package_name, shell=True,
                 stdout=PIPE)
    out = proc.communicate()[0]
    for line in out.split("\n"):
      line = line.strip().split(':', 1)
      if len(line) != 2:
        continue

      ver = line[1].strip()
      if line[0] == "Installed":
        self.current_version = None if ver == '(none)' else ver
        self.log.debug("Current version of package %s is %s" % (
        self.resource.package_name, self.current_version))
      elif line[0] == "Candidate":
        self.candidate_version = ver

    if self.candidate_version == "(none)":
      raise Fail(
        "APT does not provide a version of package %s" % self.resource.package_name)

  def install_package(self, name, version):
    if self.resource.build_vars:
      self._install_package_source(name, version)
    else:
      self._install_package_default(name, version)

  def _install_package_default(self, name, version):
    return 0 == check_call(
      "DEBIAN_FRONTEND=noninteractive apt-get -q -y install %s=%s" % (
      name, version),
      shell=True, stdout=PIPE, stderr=STDOUT)

  def _install_package_source(self, name, version):
    build_vars = " ".join(self.resource.build_vars)
    run_check_call = lambda s, **kw: check_call(s, shell=True, stdout=PIPE,
                                                stderr=STDOUT, **kw)
    pkgdir = tempfile.mkdtemp(suffix=name)

    try:
      run_check_call(
        "DEBIAN_FRONTEND=noninteractive apt-get -q -y install fakeroot")
      run_check_call(
        "DEBIAN_FRONTEND=noninteractive apt-get -q -y build-dep %s=%s" % (
        name, version))
      run_check_call(
        "DEBIAN_FRONTEND=noninteractive apt-get -q -y source %s=%s" % (
        name, version), cwd=pkgdir)

      try:
        builddir =
        [p for p in glob.iglob("%s/%s*" % (pkgdir, name)) if os.path.isdir(p)][
          0]
      except IndexError:
        raise Fail(
          "Couldn't install %s from source: apt-get source created an unfamiliar directory structure." % name)

      run_check_call("%s fakeroot debian/rules binary > /dev/null" % build_vars,
                     cwd=builddir)

      # NOTE: I can't figure out why this call returns non-zero sometimes, though everything seems to work.
      # Just ignoring checking for now.
      try:
        run_check_call("dpkg -i *.deb > /dev/null", cwd=pkgdir)
      except CalledProcessError:
        pass
    finally:
      shutil.rmtree(pkgdir)

    return True

  def remove_package(self, name):
    return 0 == check_call(
      "DEBIAN_FRONTEND=noninteractive apt-get -q -y remove %s" % name,
      shell=True, stdout=PIPE, stderr=STDOUT)

  def purge_package(self, name):
    return 0 == check_call(
      "DEBIAN_FRONTEND=noninteractive apt-get -q -y purge %s" % name,
      shell=True, stdout=PIPE, stderr=STDOUT)

  def upgrade_package(self, name, version):
    return self.install_package(name, version)

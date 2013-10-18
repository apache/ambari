from subprocess import Popen, STDOUT, PIPE, check_call
from resource_management.base import Fail
from resource_management.providers.package import PackageProvider


class GentooEmergeProvider(PackageProvider):
  def get_current_status(self):
    self.current_version = None
    self.candidate_version = None

    proc = Popen("qlist --installed --exact --verbose --nocolor %s"
                 % self.resource.package_name, shell=True, stdout=PIPE)
    out = proc.communicate()[0]
    for line in out.split("\n"):
      line = line.split('/', 1)
      if len(line) != 2:
        continue
      _category, nameversion = line
      _name, version = nameversion.split('-', 1)
      self.current_version = version
      self.log.debug("Current version of package %s is %s",
                     self.resource.package_name, self.current_version)

    proc = Popen(
      "emerge --pretend --quiet --color n %s" % self.resource.package_name,
      shell=True, stdout=PIPE)
    out = proc.communicate()[0]
    for line in out.split("\n"):
      line = line.strip(' [').split(']', 1)
      if len(line) != 2:
        continue

      # kind, flag = line[0].split()
      _category, nameversion = line[1].split('/', 1)
      _name, version = nameversion.split('-', 1)
      self.candidate_version = version
      self.log.debug("Candidate version of package %s is %s",
                     self.resource.package_name, self.candidate_version)

    if self.candidate_version is None:
      raise Fail(
        "emerge does not provide a version of package %s" % self.resource.package_name)

  def install_package(self, name, version):
    return 0 == check_call("emerge --color n =%s-%s" % (name, version),
                           shell=True, stdout=PIPE, stderr=STDOUT)

  def upgrade_package(self, name, version):
    return self.install_package(name, version)

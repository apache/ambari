__all__ = ["Provider", "find_provider"]

import logging
from resource_management.exceptions import Fail


class Provider(object):
  def __init__(self, resource):
    self.log = logging.getLogger("resource_management.provider")
    self.resource = resource

  def action_nothing(self):
    pass

  def __repr__(self):
    return self.__unicode__()

  def __unicode__(self):
    return u"%s[%s]" % (self.__class__.__name__, self.resource)


PROVIDERS = dict(
  redhat=dict(
    Service="resource_management.providers.service.redhat.RedhatServiceProvider",
    Package="resource_management.providers.package.yumrpm.YumProvider",
  ),
  centos=dict(
    Service="resource_management.providers.service.redhat.RedhatServiceProvider",
    Package="resource_management.providers.package.yumrpm.YumProvider",
  ),
  suse=dict(
    Service="resource_management.providers.service.suse.SuseServiceProvider",
    Package="resource_management.providers.package.zypper.ZypperProvider",
  ),
  fedora=dict(
    Service="resource_management.providers.service.redhat.RedhatServiceProvider",
    Package="resource_management.providers.package.yumrpm.YumProvider",
  ),
  amazon=dict(
    Service="resource_management.providers.service.redhat.RedhatServiceProvider",
    Package="resource_management.providers.package.yumrpm.YumProvider",
  ),
  default=dict(
    File="resource_management.providers.system.FileProvider",
    Directory="resource_management.providers.system.DirectoryProvider",
    Link="resource_management.providers.system.LinkProvider",
    Execute="resource_management.providers.system.ExecuteProvider",
    Script="resource_management.providers.system.ScriptProvider",
    Mount="resource_management.providers.mount.MountProvider",
    User="resource_management.providers.accounts.UserProvider",
    Group="resource_management.providers.accounts.GroupProvider",
  ),
)


def find_provider(env, resource, class_path=None):
  if not class_path:
    try:
      class_path = PROVIDERS[env.system.platform][resource]
    except KeyError:
      class_path = PROVIDERS["default"][resource]

  if class_path.startswith('*'):
    cookbook, classname = class_path[1:].split('.')
    return getattr(env.cookbooks[cookbook], classname)

  try:
    mod_path, class_name = class_path.rsplit('.', 1)
  except ValueError:
    raise Fail("Unable to find provider for %s as %s" % (resource, class_path))
  mod = __import__(mod_path, {}, {}, [class_name])
  return getattr(mod, class_name)

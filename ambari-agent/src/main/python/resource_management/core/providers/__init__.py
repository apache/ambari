__all__ = ["Provider", "find_provider"]

import logging
from resource_management.core.exceptions import Fail
from resource_management.libraries.providers import PROVIDERS as LIBRARY_PROVIDERS


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
    Package="resource_management.core.providers.package.yumrpm.YumProvider",
  ),
  centos=dict(
    Package="resource_management.core.providers.package.yumrpm.YumProvider",
  ),
  suse=dict(
    Package="resource_management.core.providers.package.zypper.ZypperProvider",
  ),
  fedora=dict(
    Package="resource_management.core.providers.package.yumrpm.YumProvider",
  ),
  amazon=dict(
    Package="resource_management.core.providers.package.yumrpm.YumProvider",
  ),
  default=dict(
    File="resource_management.core.providers.system.FileProvider",
    Directory="resource_management.core.providers.system.DirectoryProvider",
    Link="resource_management.core.providers.system.LinkProvider",
    Execute="resource_management.core.providers.system.ExecuteProvider",
    Script="resource_management.core.providers.system.ScriptProvider",
    Mount="resource_management.core.providers.mount.MountProvider",
    User="resource_management.core.providers.accounts.UserProvider",
    Group="resource_management.core.providers.accounts.GroupProvider",
    Service="resource_management.core.providers.service.ServiceProvider",
  ),
)


def find_provider(env, resource, class_path=None):
  if not class_path:
    providers = [PROVIDERS, LIBRARY_PROVIDERS]
    for provider in providers:
      if resource in provider[env.system.platform]:
        class_path = provider[env.system.platform][resource]
        break
      if resource in provider["default"]:
        class_path = provider["default"][resource]
        break

  try:
    mod_path, class_name = class_path.rsplit('.', 1)
  except ValueError:
    raise Fail("Unable to find provider for %s as %s" % (resource, class_path))
  mod = __import__(mod_path, {}, {}, [class_name])
  return getattr(mod, class_name)

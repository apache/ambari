__all__ = ["DebianServiceProvider"]

from resource_management.providers.service import ServiceProvider


class DebianServiceProvider(ServiceProvider):
  def enable_runlevel(self, runlevel):
    pass

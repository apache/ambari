__all__ = ["RedhatServiceProvider"]

from resource_management.providers.service import ServiceProvider


class RedhatServiceProvider(ServiceProvider):
  def enable_runlevel(self, runlevel):
    pass

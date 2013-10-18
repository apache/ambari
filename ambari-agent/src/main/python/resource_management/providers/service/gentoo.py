__all__ = ["GentooServiceProvider"]

from resource_management.providers.service import ServiceProvider


class GentooServiceProvider(ServiceProvider):
  def enable_runlevel(self, runlevel):
    pass

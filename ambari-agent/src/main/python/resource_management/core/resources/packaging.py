__all__ = ["Package"]

from resource_management.core.base import Resource, ForcedListArgument, ResourceArgument


class Package(Resource):
  action = ForcedListArgument(default="install")
  package_name = ResourceArgument(default=lambda obj: obj.name)
  location = ResourceArgument(default=lambda obj: obj.package_name)
  version = ResourceArgument()
  actions = ["install", "upgrade", "remove"]
  build_vars = ForcedListArgument(default=[])

_all__ = ["ConfigFile"]
from resource_management.core.base import Resource, ForcedListArgument, ResourceArgument, BooleanArgument

class ConfigFile(Resource):
  action = ForcedListArgument(default="create")
  path = ResourceArgument(default=lambda obj: obj.name)
  mode = ResourceArgument()
  owner = ResourceArgument()
  group = ResourceArgument()
  template_tag = ResourceArgument()

  actions = Resource.actions + ["create"]
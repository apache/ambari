_all__ = ["XmlConfig"]
from resource_management.core.base import Resource, ForcedListArgument, ResourceArgument, BooleanArgument

class XmlConfig(Resource):
  action = ForcedListArgument(default="create")
  filename = ResourceArgument(default=lambda obj: obj.name)
  
  configurations = ResourceArgument()
  conf_dir = ResourceArgument()
  
  mode = ResourceArgument()
  owner = ResourceArgument()
  group = ResourceArgument()

  actions = Resource.actions + ["create"]
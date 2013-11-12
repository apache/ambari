_all__ = ["TemplateConfig"]
from resource_management.core.base import Resource, ForcedListArgument, ResourceArgument, BooleanArgument

class TemplateConfig(Resource):
  action = ForcedListArgument(default="create")
  path = ResourceArgument(default=lambda obj: obj.name)
  mode = ResourceArgument()
  owner = ResourceArgument()
  group = ResourceArgument()
  template_tag = ResourceArgument()
  extra_imports = ResourceArgument(default=[])

  actions = Resource.actions + ["create"]
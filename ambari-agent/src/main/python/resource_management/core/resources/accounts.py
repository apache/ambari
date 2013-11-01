__all__ = ["Group", "User"]

from resource_management.core.base import Resource, ForcedListArgument, ResourceArgument, BooleanArgument


class Group(Resource):
  action = ForcedListArgument(default="create")
  group_name = ResourceArgument(default=lambda obj: obj.name)
  gid = ResourceArgument()
  password = ResourceArgument()

  actions = Resource.actions + ["create", "remove"]


class User(Resource):
  action = ForcedListArgument(default="create")
  username = ResourceArgument(default=lambda obj: obj.name)
  comment = ResourceArgument()
  uid = ResourceArgument()
  gid = ResourceArgument()
  groups = ForcedListArgument(default=[]) # supplementary groups
  home = ResourceArgument()
  shell = ResourceArgument(default="/bin/bash")
  password = ResourceArgument()
  system = BooleanArgument(default=False)

  actions = Resource.actions + ["create", "remove"]

__all__ = ["File", "Directory", "Link", "Execute", "Script", "Mount"]

from resource_management.base import Resource, ForcedListArgument, ResourceArgument, BooleanArgument


class File(Resource):
  action = ForcedListArgument(default="create")
  path = ResourceArgument(default=lambda obj: obj.name)
  backup = ResourceArgument()
  mode = ResourceArgument()
  owner = ResourceArgument()
  group = ResourceArgument()
  content = ResourceArgument()

  actions = Resource.actions + ["create", "delete", "touch"]


class Directory(Resource):
  action = ForcedListArgument(default="create")
  path = ResourceArgument(default=lambda obj: obj.name)
  mode = ResourceArgument()
  owner = ResourceArgument()
  group = ResourceArgument()
  recursive = BooleanArgument(default=False)

  actions = Resource.actions + ["create", "delete"]


class Link(Resource):
  action = ForcedListArgument(default="create")
  path = ResourceArgument(default=lambda obj: obj.name)
  to = ResourceArgument(required=True)
  hard = BooleanArgument(default=False)

  actions = Resource.actions + ["create", "delete"]


class Execute(Resource):
  action = ForcedListArgument(default="run")
  command = ResourceArgument(default=lambda obj: obj.name)
  creates = ResourceArgument()
  cwd = ResourceArgument()
  environment = ResourceArgument()
  user = ResourceArgument()
  group = ResourceArgument()
  returns = ForcedListArgument(default=0)
  timeout = ResourceArgument()

  actions = Resource.actions + ["run"]


class Script(Resource):
  action = ForcedListArgument(default="run")
  code = ResourceArgument(required=True)
  cwd = ResourceArgument()
  environment = ResourceArgument()
  interpreter = ResourceArgument(default="/bin/bash")
  user = ResourceArgument()
  group = ResourceArgument()

  actions = Resource.actions + ["run"]


class Mount(Resource):
  action = ForcedListArgument(default="mount")
  mount_point = ResourceArgument(default=lambda obj: obj.name)
  device = ResourceArgument()
  fstype = ResourceArgument()
  options = ResourceArgument(default=["defaults"])
  dump = ResourceArgument(default=0)
  passno = ResourceArgument(default=2)

  actions = Resource.actions + ["mount", "umount", "remount", "enable",
                                "disable"]

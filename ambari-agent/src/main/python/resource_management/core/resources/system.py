__all__ = ["File", "Directory", "Link", "Execute", "ExecuteScript", "Mount"]

from resource_management.core.base import Resource, ForcedListArgument, ResourceArgument, BooleanArgument


class File(Resource):
  action = ForcedListArgument(default="create")
  path = ResourceArgument(default=lambda obj: obj.name)
  backup = ResourceArgument()
  mode = ResourceArgument()
  owner = ResourceArgument()
  group = ResourceArgument()
  content = ResourceArgument()
  # whether to replace files with different content
  replace = ResourceArgument(default=True)

  actions = Resource.actions + ["create", "delete"]


class Directory(Resource):
  action = ForcedListArgument(default="create")
  path = ResourceArgument(default=lambda obj: obj.name)
  mode = ResourceArgument()
  owner = ResourceArgument()
  group = ResourceArgument()
  recursive = BooleanArgument(default=False) # this work for 'create', 'delete' is anyway recursive

  actions = Resource.actions + ["create", "delete"]


class Link(Resource):
  action = ForcedListArgument(default="create")
  path = ResourceArgument(default=lambda obj: obj.name)
  to = ResourceArgument(required=True)
  hard = BooleanArgument(default=False)

  actions = Resource.actions + ["create", "delete"]


class Execute(Resource):
  action = ForcedListArgument(default="run")
  
  """
  Recommended:
  command = ('rm','-f','myfile')
  Not recommended:
  command = 'rm -f myfile'
  
  The first one helps to stop escaping issues
  """
  command = ResourceArgument(default=lambda obj: obj.name)
  
  creates = ResourceArgument()
  cwd = ResourceArgument()
  # this runs command with a specific env variables, env={'JAVA_HOME': '/usr/jdk'}
  environment = ResourceArgument()
  user = ResourceArgument()
  group = ResourceArgument()
  returns = ForcedListArgument(default=0)
  tries = ResourceArgument(default=1)
  try_sleep = ResourceArgument(default=0) # seconds
  path = ForcedListArgument(default=[])
  actions = Resource.actions + ["run"]
  logoutput = BooleanArgument(default=False)


class ExecuteScript(Resource):
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

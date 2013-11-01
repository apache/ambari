_all__ = ["ExecuteHadoop"]
from resource_management.core.base import Resource, ForcedListArgument, ResourceArgument, BooleanArgument

class ExecuteHadoop(Resource):
  action = ForcedListArgument(default="run")
  command = ResourceArgument(default=lambda obj: obj.name)
  kinit_override = BooleanArgument(default=False)
  tries = ResourceArgument(default=1)
  try_sleep = ResourceArgument(default=0) # seconds
  user = ResourceArgument()
  logoutput = BooleanArgument(default=False)
  
  conf_dir = ResourceArgument()
  
  security_enabled = BooleanArgument(default=False)
  keytab = ResourceArgument()
  principal = ResourceArgument()
  kinit_path_local = ResourceArgument()
  

  
  actions = Resource.actions + ["run"]
  
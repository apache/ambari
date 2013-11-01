__all__ = ["Service"]

from resource_management.core.base import Resource, ResourceArgument, ForcedListArgument


class Service(Resource):
  action = ForcedListArgument(default="start")
  service_name = ResourceArgument(default=lambda obj: obj.name)
  #enabled = ResourceArgument() # Maybe add support to put in/out autostart.
  start_command = ResourceArgument()
  stop_command = ResourceArgument()
  restart_command = ResourceArgument()
  reload_command = ResourceArgument() # reload the config file without interrupting pending operations
  status_command = ResourceArgument()

  actions = ["nothing", "start", "stop", "restart", "reload"]

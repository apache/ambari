__all__ = ["Service"]

from resource_management.base import Resource, ResourceArgument, BooleanArgument


class Service(Resource):
  service_name = ResourceArgument(default=lambda obj: obj.name)
  enabled = ResourceArgument()
  running = ResourceArgument()
  pattern = ResourceArgument()
  start_command = ResourceArgument()
  stop_command = ResourceArgument()
  restart_command = ResourceArgument()
  reload_command = ResourceArgument()
  status_command = ResourceArgument()
  supports_restart = BooleanArgument(
    default=lambda obj: bool(obj.restart_command))
  supports_reload = BooleanArgument(
    default=lambda obj: bool(obj.reload_command))
  supports_status = BooleanArgument(
    default=lambda obj: bool(obj.status_command))

  actions = ["nothing", "start", "stop", "restart", "reload"]

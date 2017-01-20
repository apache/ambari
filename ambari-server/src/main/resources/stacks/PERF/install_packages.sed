/actionexecute/{i\
  def actionexecute(self, env):\
    # Parse parameters\
    config = Script.get_config()\
    repository_version = config['roleParams']['repository_version']\
    (stack_selector_name, stack_selector_path, stack_selector_package) = stack_tools.get_stack_tool(stack_tools.STACK_SELECTOR_NAME)\
    command = 'ambari-python-wrap {0} install {1}'.format(stack_selector_path, repository_version)\
    Execute(command)\
  def actionexecute_old(self, env):
d
}
/    command_env = dict(os.environ)/{i\
    command_env = dict(os.environ)\
    command_env['PATH'] = command_env['PATH'] + ':' + "AMBARI_AGENT_HOST_DIR" + self.config.get('agent', 'prefix') + ':' + "AMBARI_AGENT_CACHE_DIR" + self.config.get('agent', 'cache_dir')
d
}
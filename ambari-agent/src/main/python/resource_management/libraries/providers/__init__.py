PROVIDERS = dict(
  redhat=dict(
  ),
  centos=dict(
  ),
  suse=dict(
  ),
  fedora=dict(
  ),
  amazon=dict(
  ),
  default=dict(
    ExecuteHadoop="resource_management.libraries.providers.execute_hadoop.ExecuteHadoopProvider",
    ConfigFile="resource_management.libraries.providers.config_file.ConfigFileProvider",
  ),
)
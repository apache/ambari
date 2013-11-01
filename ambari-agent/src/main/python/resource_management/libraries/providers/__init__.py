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
    TemplateConfig="resource_management.libraries.providers.template_config.TemplateConfigProvider",
    XmlConfig="resource_management.libraries.providers.xml_config.XmlConfigProvider"
  ),
)
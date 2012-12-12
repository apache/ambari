CREATE TABLE ambari.clusters (cluster_id BIGINT NOT NULL, cluster_info VARCHAR(255) NOT NULL, cluster_name VARCHAR(255) NOT NULL UNIQUE, desired_cluster_state VARCHAR(255) NOT NULL, PRIMARY KEY (cluster_id))
CREATE TABLE ambari.clusterconfig (version_tag VARCHAR(255) NOT NULL, type_name VARCHAR(255) NOT NULL, cluster_id BIGINT NOT NULL, config_data VARCHAR(4000) NOT NULL, create_timestamp BIGINT NOT NULL, PRIMARY KEY (version_tag, type_name, cluster_id))
CREATE TABLE ambari.clusterservices (service_name VARCHAR(255) NOT NULL, cluster_id BIGINT NOT NULL, service_enabled INTEGER NOT NULL, PRIMARY KEY (service_name, cluster_id))
CREATE TABLE ambari.clusterstate (cluster_id BIGINT NOT NULL, current_cluster_state VARCHAR(255) NOT NULL, PRIMARY KEY (cluster_id))
CREATE TABLE ambari.componentconfigmapping (cluster_id BIGINT NOT NULL, component_name VARCHAR(255) NOT NULL, config_type VARCHAR(255) NOT NULL, service_name VARCHAR(255) NOT NULL, timestamp BIGINT NOT NULL, config_tag VARCHAR(255) NOT NULL, PRIMARY KEY (cluster_id, component_name, service_name))
CREATE TABLE ambari.hostcomponentconfigmapping (cluster_id BIGINT NOT NULL, component_name VARCHAR(255) NOT NULL, config_type VARCHAR(255) NOT NULL, host_name VARCHAR(255) NOT NULL, service_name VARCHAR(255) NOT NULL, timestamp BIGINT NOT NULL, config_tag VARCHAR(255) NOT NULL, PRIMARY KEY (cluster_id, component_name, host_name, service_name))
CREATE TABLE ambari.hostcomponentdesiredstate (cluster_id BIGINT NOT NULL, component_name VARCHAR(255) NOT NULL, desired_config_version VARCHAR(255) NOT NULL, desired_stack_version VARCHAR(255) NOT NULL, desired_state INTEGER NOT NULL, host_name VARCHAR(255) NOT NULL, service_name VARCHAR(255) NOT NULL, PRIMARY KEY (cluster_id, component_name, host_name, service_name))
CREATE TABLE ambari.hostcomponentstate (cluster_id BIGINT NOT NULL, component_name VARCHAR(255) NOT NULL, current_config_version VARCHAR(255) NOT NULL, current_stack_version VARCHAR(255) NOT NULL, current_state VARCHAR(255) NOT NULL, host_name VARCHAR(255) NOT NULL, service_name VARCHAR(255) NOT NULL, PRIMARY KEY (cluster_id, component_name, host_name, service_name))
CREATE TABLE ambari.hostcomponentmapping (host_component_mapping_id INTEGER NOT NULL, cluster_id BIGINT NOT NULL, host_component_mapping_snapshot VARCHAR(255) NOT NULL, service_name VARCHAR(255) NOT NULL, PRIMARY KEY (host_component_mapping_id, cluster_id, service_name))
CREATE TABLE ambari.hosts (host_name VARCHAR(255) NOT NULL, cpu_count INTEGER NOT NULL, cpu_info VARCHAR(255) NOT NULL, discovery_status VARCHAR(255) NOT NULL, disks_info VARCHAR(2000) NOT NULL, host_attributes VARCHAR(255) NOT NULL, ipv4 VARCHAR(255), ipv6 VARCHAR(255), last_registration_time BIGINT NOT NULL, os_arch VARCHAR(255) NOT NULL, os_info VARCHAR(255) NOT NULL, os_type VARCHAR(255) NOT NULL, rack_info VARCHAR(255) NOT NULL, total_mem BIGINT NOT NULL, PRIMARY KEY (host_name))
CREATE TABLE ambari.hoststate (agent_version VARCHAR(255) NOT NULL, available_mem BIGINT NOT NULL, current_state VARCHAR(255) NOT NULL, health_status VARCHAR(255), host_name VARCHAR(255) NOT NULL, last_heartbeat_time BIGINT NOT NULL, PRIMARY KEY (host_name))
CREATE TABLE ambari.servicecomponentconfig (config_version INTEGER NOT NULL, component_name VARCHAR(255) NOT NULL, config_snapshot VARCHAR(255) NOT NULL, config_snapshot_time TIMESTAMP NOT NULL, cluster_id BIGINT NOT NULL, service_name VARCHAR(255) NOT NULL, PRIMARY KEY (config_version))
CREATE TABLE ambari.servicecomponentdesiredstate (component_name VARCHAR(255) NOT NULL, cluster_id BIGINT NOT NULL, desired_stack_version VARCHAR(255) NOT NULL, desired_state VARCHAR(255) NOT NULL, service_name VARCHAR(255) NOT NULL, PRIMARY KEY (component_name, cluster_id, service_name))
CREATE TABLE ambari.servicecomponenthostconfig (config_version INTEGER NOT NULL, component_name VARCHAR(255) NOT NULL, config_snapshot VARCHAR(255) NOT NULL, config_snapshot_time TIMESTAMP NOT NULL, cluster_id BIGINT NOT NULL, service_name VARCHAR(255) NOT NULL, host_name VARCHAR(255) NOT NULL, PRIMARY KEY (config_version))
CREATE TABLE ambari.serviceconfig (config_version INTEGER NOT NULL, config_snapshot VARCHAR(255) NOT NULL, config_snapshot_time TIMESTAMP NOT NULL, cluster_id BIGINT NOT NULL, service_name VARCHAR(255) NOT NULL, PRIMARY KEY (config_version))
CREATE TABLE ambari.serviceconfigmapping (cluster_id BIGINT NOT NULL, config_type VARCHAR(255) NOT NULL, service_name VARCHAR(255) NOT NULL, timestamp BIGINT NOT NULL, config_tag VARCHAR(255) NOT NULL, PRIMARY KEY (cluster_id, service_name))
CREATE TABLE ambari.servicedesiredstate (cluster_id BIGINT NOT NULL, desired_host_role_mapping INTEGER NOT NULL, desired_stack_version VARCHAR(255) NOT NULL, desired_state VARCHAR(255) NOT NULL, service_name VARCHAR(255) NOT NULL, PRIMARY KEY (cluster_id, service_name))
CREATE TABLE ambari.roles (role_name VARCHAR(255) NOT NULL, PRIMARY KEY (role_name))
CREATE TABLE ambari.users (ldap_user BOOLEAN NOT NULL, user_name VARCHAR(255) NOT NULL, create_time TIMESTAMP, user_password VARCHAR(255), PRIMARY KEY (ldap_user, user_name))
CREATE TABLE ambari.execution_command (command VARCHAR(32000), task_id BIGINT NOT NULL, PRIMARY KEY (task_id))
CREATE TABLE ambari.host_role_command (task_id BIGINT NOT NULL, attempt_count SMALLINT NOT NULL, command VARCHAR(255) NOT NULL, event VARCHAR(255) NOT NULL, exitcode INTEGER NOT NULL, host_name VARCHAR(255) NOT NULL, last_attempt_time BIGINT NOT NULL, request_id BIGINT NOT NULL, role VARCHAR(255), stage_id BIGINT NOT NULL, start_time BIGINT NOT NULL, status VARCHAR(255), std_error VARCHAR(32000) NOT NULL, std_out VARCHAR(32000) NOT NULL, PRIMARY KEY (task_id))
CREATE TABLE ambari.role_success_criteria (role VARCHAR(255) NOT NULL, request_id BIGINT NOT NULL, stage_id BIGINT NOT NULL, success_factor FLOAT NOT NULL, PRIMARY KEY (role, request_id, stage_id))
CREATE TABLE ambari.stage (stage_id BIGINT NOT NULL, request_id BIGINT NOT NULL, cluster_id BIGINT NOT NULL, log_info VARCHAR(255) NOT NULL, PRIMARY KEY (stage_id, request_id))
CREATE TABLE ambari.ClusterHostMapping (cluster_id BIGINT NOT NULL, host_name VARCHAR(255) NOT NULL, PRIMARY KEY (cluster_id, host_name))
CREATE TABLE ambari.user_roles (role_name VARCHAR(255) NOT NULL, user_name VARCHAR(255) NOT NULL, ldap_user BOOLEAN NOT NULL, PRIMARY KEY (role_name, user_name, ldap_user))
ALTER TABLE ambari.clusterconfig ADD CONSTRAINT FK_clusterconfig_cluster_id FOREIGN KEY (cluster_id) REFERENCES ambari.clusters (cluster_id)
ALTER TABLE ambari.clusterservices ADD CONSTRAINT FK_clusterservices_cluster_id FOREIGN KEY (cluster_id) REFERENCES ambari.clusters (cluster_id)
ALTER TABLE ambari.clusterstate ADD CONSTRAINT FK_clusterstate_cluster_id FOREIGN KEY (cluster_id) REFERENCES ambari.clusters (cluster_id)
ALTER TABLE ambari.componentconfigmapping ADD CONSTRAINT FK_componentconfigmapping_component_name FOREIGN KEY (component_name, cluster_id, service_name) REFERENCES ambari.servicecomponentdesiredstate (component_name, cluster_id, service_name)
ALTER TABLE ambari.hostcomponentconfigmapping ADD CONSTRAINT FK_hostcomponentconfigmapping_cluster_id FOREIGN KEY (cluster_id, component_name, host_name, service_name) REFERENCES ambari.hostcomponentdesiredstate (cluster_id, component_name, host_name, service_name)
ALTER TABLE ambari.hostcomponentdesiredstate ADD CONSTRAINT FK_hostcomponentdesiredstate_host_name FOREIGN KEY (host_name) REFERENCES ambari.hosts (host_name)
ALTER TABLE ambari.hostcomponentdesiredstate ADD CONSTRAINT FK_hostcomponentdesiredstate_component_name FOREIGN KEY (component_name, cluster_id, service_name) REFERENCES ambari.servicecomponentdesiredstate (component_name, cluster_id, service_name)
ALTER TABLE ambari.hostcomponentstate ADD CONSTRAINT FK_hostcomponentstate_component_name FOREIGN KEY (component_name, cluster_id, service_name) REFERENCES ambari.servicecomponentdesiredstate (component_name, cluster_id, service_name)
ALTER TABLE ambari.hostcomponentstate ADD CONSTRAINT FK_hostcomponentstate_host_name FOREIGN KEY (host_name) REFERENCES ambari.hosts (host_name)
ALTER TABLE ambari.hostcomponentmapping ADD CONSTRAINT FK_hostcomponentmapping_service_name FOREIGN KEY (service_name, cluster_id) REFERENCES ambari.clusterservices (service_name, cluster_id)
ALTER TABLE ambari.hoststate ADD CONSTRAINT FK_hoststate_host_name FOREIGN KEY (host_name) REFERENCES ambari.hosts (host_name)
ALTER TABLE ambari.servicecomponentconfig ADD CONSTRAINT FK_servicecomponentconfig_service_name FOREIGN KEY (service_name, cluster_id) REFERENCES ambari.clusterservices (service_name, cluster_id)
ALTER TABLE ambari.servicecomponentdesiredstate ADD CONSTRAINT FK_servicecomponentdesiredstate_service_name FOREIGN KEY (service_name, cluster_id) REFERENCES ambari.clusterservices (service_name, cluster_id)
ALTER TABLE ambari.servicecomponenthostconfig ADD CONSTRAINT FK_servicecomponenthostconfig_host_name FOREIGN KEY (host_name) REFERENCES ambari.hosts (host_name)
ALTER TABLE ambari.servicecomponenthostconfig ADD CONSTRAINT FK_servicecomponenthostconfig_service_name FOREIGN KEY (service_name, cluster_id) REFERENCES ambari.clusterservices (service_name, cluster_id)
ALTER TABLE ambari.serviceconfig ADD CONSTRAINT FK_serviceconfig_service_name FOREIGN KEY (service_name, cluster_id) REFERENCES ambari.clusterservices (service_name, cluster_id)
ALTER TABLE ambari.serviceconfigmapping ADD CONSTRAINT FK_serviceconfigmapping_service_name FOREIGN KEY (service_name, cluster_id) REFERENCES ambari.clusterservices (service_name, cluster_id)
ALTER TABLE ambari.servicedesiredstate ADD CONSTRAINT FK_servicedesiredstate_service_name FOREIGN KEY (service_name, cluster_id) REFERENCES ambari.clusterservices (service_name, cluster_id)
ALTER TABLE ambari.execution_command ADD CONSTRAINT FK_execution_command_task_id FOREIGN KEY (task_id) REFERENCES ambari.host_role_command (task_id)
ALTER TABLE ambari.host_role_command ADD CONSTRAINT FK_host_role_command_stage_id FOREIGN KEY (stage_id, request_id) REFERENCES ambari.stage (stage_id, request_id)
ALTER TABLE ambari.host_role_command ADD CONSTRAINT FK_host_role_command_host_name FOREIGN KEY (host_name) REFERENCES ambari.hosts (host_name)
ALTER TABLE ambari.role_success_criteria ADD CONSTRAINT FK_role_success_criteria_stage_id FOREIGN KEY (stage_id, request_id) REFERENCES ambari.stage (stage_id, request_id)
ALTER TABLE ambari.stage ADD CONSTRAINT FK_stage_cluster_id FOREIGN KEY (cluster_id) REFERENCES ambari.clusters (cluster_id)
ALTER TABLE ambari.ClusterHostMapping ADD CONSTRAINT FK_ClusterHostMapping_host_name FOREIGN KEY (host_name) REFERENCES ambari.hosts (host_name)
ALTER TABLE ambari.ClusterHostMapping ADD CONSTRAINT FK_ClusterHostMapping_cluster_id FOREIGN KEY (cluster_id) REFERENCES ambari.clusters (cluster_id)
ALTER TABLE ambari.user_roles ADD CONSTRAINT FK_user_roles_ldap_user FOREIGN KEY (ldap_user, user_name) REFERENCES ambari.users (ldap_user, user_name)
ALTER TABLE ambari.user_roles ADD CONSTRAINT FK_user_roles_role_name FOREIGN KEY (role_name) REFERENCES ambari.roles (role_name)
CREATE SEQUENCE ambari.serviceconfig_config_version_seq START WITH 1
CREATE SEQUENCE ambari.host_role_command_task_id_seq START WITH 1
CREATE SEQUENCE ambari.clusters_cluster_id_seq START WITH 1
CREATE SEQUENCE ambari.servicecomponenthostconfig_config_version_seq START WITH 1
CREATE SEQUENCE ambari.hostcomponentmapping_host_component_mapping_id_seq START WITH 1
CREATE SEQUENCE ambari.servicecomponentconfig_config_version_seq START WITH 1
CREATE SEQUENCE ambari.serviceconfig_config_version_seq START WITH 1
CREATE SEQUENCE ambari.servicecomponenthostconfig_config_version_seq START WITH 1
CREATE SEQUENCE ambari.hostcomponentmapping_host_component_mapping_id_seq START WITH 1
CREATE SEQUENCE ambari.servicecomponentconfig_config_version_seq START WITH 1
CREATE SEQUENCE ambari.clusters_cluster_id_seq START WITH 1
CREATE SEQUENCE ambari.host_role_command_task_id_seq START WITH 1

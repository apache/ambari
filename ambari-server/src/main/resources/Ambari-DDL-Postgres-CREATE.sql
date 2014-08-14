--
-- Licensed to the Apache Software Foundation (ASF) under one
-- or more contributor license agreements.  See the NOTICE file
-- distributed with this work for additional information
-- regarding copyright ownership.  The ASF licenses this file
-- to you under the Apache License, Version 2.0 (the
-- "License"); you may not use this file except in compliance
-- with the License.  You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

------create tables ang grant privileges to db user---------
CREATE TABLE clusters (cluster_id BIGINT NOT NULL, resource_id BIGINT NOT NULL, cluster_info VARCHAR(255) NOT NULL, cluster_name VARCHAR(100) NOT NULL UNIQUE, provisioning_state VARCHAR(255) NOT NULL DEFAULT 'INIT', desired_cluster_state VARCHAR(255) NOT NULL, desired_stack_version VARCHAR(255) NOT NULL, PRIMARY KEY (cluster_id));

CREATE TABLE clusterconfig (config_id BIGINT NOT NULL, version_tag VARCHAR(255) NOT NULL, version BIGINT NOT NULL, type_name VARCHAR(255) NOT NULL, cluster_id BIGINT NOT NULL, config_data VARCHAR(32000) NOT NULL, config_attributes VARCHAR(32000), create_timestamp BIGINT NOT NULL, PRIMARY KEY (config_id));

CREATE TABLE clusterconfigmapping (cluster_id BIGINT NOT NULL, type_name VARCHAR(255) NOT NULL, version_tag VARCHAR(255) NOT NULL, create_timestamp BIGINT NOT NULL, selected INTEGER NOT NULL DEFAULT 0, user_name VARCHAR(255) NOT NULL DEFAULT '_db', PRIMARY KEY (cluster_id, type_name, create_timestamp));

CREATE TABLE serviceconfig (service_config_id BIGINT NOT NULL, cluster_id BIGINT NOT NULL, service_name VARCHAR(255) NOT NULL, version BIGINT NOT NULL, create_timestamp BIGINT NOT NULL, PRIMARY KEY (service_config_id));

CREATE TABLE serviceconfigmapping (service_config_id BIGINT NOT NULL, config_id BIGINT NOT NULL, PRIMARY KEY(service_config_id, config_id));

CREATE TABLE serviceconfigapplication (apply_id BIGINT NOT NULL, service_config_id BIGINT NOT NULL, apply_timestamp BIGINT NOT NULL, user_name VARCHAR(255) NOT NULL DEFAULT '_db', note TEXT, PRIMARY KEY(apply_id));

CREATE TABLE clusterservices (service_name VARCHAR(255) NOT NULL, cluster_id BIGINT NOT NULL, service_enabled INTEGER NOT NULL, PRIMARY KEY (service_name, cluster_id));

CREATE TABLE clusterstate (cluster_id BIGINT NOT NULL, current_cluster_state VARCHAR(255) NOT NULL, current_stack_version VARCHAR(255) NOT NULL, PRIMARY KEY (cluster_id));

CREATE TABLE hostcomponentdesiredstate (cluster_id BIGINT NOT NULL, component_name VARCHAR(255) NOT NULL, desired_stack_version VARCHAR(255) NOT NULL, desired_state VARCHAR(255) NOT NULL, host_name VARCHAR(255) NOT NULL, service_name VARCHAR(255) NOT NULL, admin_state VARCHAR(32), maintenance_state VARCHAR(32) NOT NULL, restart_required SMALLINT NOT NULL DEFAULT 0, PRIMARY KEY (cluster_id, component_name, host_name, service_name));

CREATE TABLE hostcomponentstate (cluster_id BIGINT NOT NULL, component_name VARCHAR(255) NOT NULL, current_stack_version VARCHAR(255) NOT NULL, current_state VARCHAR(255) NOT NULL, host_name VARCHAR(255) NOT NULL, service_name VARCHAR(255) NOT NULL, PRIMARY KEY (cluster_id, component_name, host_name, service_name));

CREATE TABLE hosts (host_name VARCHAR(255) NOT NULL, cpu_count INTEGER NOT NULL, ph_cpu_count INTEGER, cpu_info VARCHAR(255) NOT NULL, discovery_status VARCHAR(2000) NOT NULL, host_attributes VARCHAR(20000) NOT NULL, ipv4 VARCHAR(255), ipv6 VARCHAR(255), public_host_name VARCHAR(255), last_registration_time BIGINT NOT NULL, os_arch VARCHAR(255) NOT NULL, os_info VARCHAR(1000) NOT NULL, os_type VARCHAR(255) NOT NULL, rack_info VARCHAR(255) NOT NULL, total_mem BIGINT NOT NULL, PRIMARY KEY (host_name));

CREATE TABLE hoststate (agent_version VARCHAR(255) NOT NULL, available_mem BIGINT NOT NULL, current_state VARCHAR(255) NOT NULL, health_status VARCHAR(255), host_name VARCHAR(255) NOT NULL, time_in_state BIGINT NOT NULL, maintenance_state VARCHAR(512), PRIMARY KEY (host_name));

CREATE TABLE servicecomponentdesiredstate (component_name VARCHAR(255) NOT NULL, cluster_id BIGINT NOT NULL, desired_stack_version VARCHAR(255) NOT NULL, desired_state VARCHAR(255) NOT NULL, service_name VARCHAR(255) NOT NULL, PRIMARY KEY (component_name, cluster_id, service_name));

CREATE TABLE servicedesiredstate (cluster_id BIGINT NOT NULL, desired_host_role_mapping INTEGER NOT NULL, desired_stack_version VARCHAR(255) NOT NULL, desired_state VARCHAR(255) NOT NULL, service_name VARCHAR(255) NOT NULL, maintenance_state VARCHAR(32) NOT NULL, PRIMARY KEY (cluster_id, service_name));

CREATE TABLE roles (role_name VARCHAR(255) NOT NULL, PRIMARY KEY (role_name));

CREATE TABLE users (user_id INTEGER, principal_id BIGINT NOT NULL, ldap_user INTEGER NOT NULL DEFAULT 0, user_name VARCHAR(255) NOT NULL, create_time TIMESTAMP DEFAULT NOW(), user_password VARCHAR(255), PRIMARY KEY (user_id), active INTEGER NOT NULL DEFAULT 1, UNIQUE (ldap_user, user_name));

CREATE TABLE groups (group_id INTEGER, principal_id BIGINT NOT NULL, group_name VARCHAR(255) NOT NULL, ldap_group INTEGER NOT NULL DEFAULT 0, PRIMARY KEY (group_id), UNIQUE (ldap_group, group_name));

CREATE TABLE members (member_id INTEGER, group_id INTEGER NOT NULL, user_id INTEGER NOT NULL, PRIMARY KEY (member_id), UNIQUE(group_id, user_id));

CREATE TABLE execution_command (command BYTEA, task_id BIGINT NOT NULL, PRIMARY KEY (task_id));

CREATE TABLE host_role_command (task_id BIGINT NOT NULL, attempt_count SMALLINT NOT NULL, event VARCHAR(32000) NOT NULL, exitcode INTEGER NOT NULL, host_name VARCHAR(255) NOT NULL, last_attempt_time BIGINT NOT NULL, request_id BIGINT NOT NULL, role VARCHAR(255), stage_id BIGINT NOT NULL, start_time BIGINT NOT NULL, end_time BIGINT, status VARCHAR(255), std_error BYTEA, std_out BYTEA, output_log VARCHAR(255) NULL, error_log VARCHAR(255) NULL, structured_out BYTEA, role_command VARCHAR(255), command_detail VARCHAR(255), custom_command_name VARCHAR(255), PRIMARY KEY (task_id));

CREATE TABLE role_success_criteria (role VARCHAR(255) NOT NULL, request_id BIGINT NOT NULL, stage_id BIGINT NOT NULL, success_factor FLOAT NOT NULL, PRIMARY KEY (role, request_id, stage_id));

CREATE TABLE stage (stage_id BIGINT NOT NULL, request_id BIGINT NOT NULL, cluster_id BIGINT NOT NULL, log_info VARCHAR(255) NOT NULL, request_context VARCHAR(255), cluster_host_info BYTEA NOT NULL, PRIMARY KEY (stage_id, request_id));

CREATE TABLE request (request_id BIGINT NOT NULL, cluster_id BIGINT, command_name VARCHAR(255), create_time BIGINT NOT NULL, end_time BIGINT NOT NULL, inputs BYTEA, request_context VARCHAR(255), request_type VARCHAR(255), request_schedule_id BIGINT, start_time BIGINT NOT NULL, status VARCHAR(255), PRIMARY KEY (request_id));

CREATE TABLE requestresourcefilter (filter_id BIGINT NOT NULL, request_id BIGINT NOT NULL, service_name VARCHAR(255), component_name VARCHAR(255), hosts BYTEA, PRIMARY KEY (filter_id));

CREATE TABLE requestoperationlevel (operation_level_id BIGINT NOT NULL, request_id BIGINT NOT NULL, level_name VARCHAR(255), cluster_name VARCHAR(255), service_name VARCHAR(255), host_component_name VARCHAR(255), host_name VARCHAR(255), PRIMARY KEY (operation_level_id));

CREATE TABLE ClusterHostMapping (cluster_id BIGINT NOT NULL, host_name VARCHAR(255) NOT NULL, PRIMARY KEY (cluster_id, host_name));

CREATE TABLE user_roles (role_name VARCHAR(255) NOT NULL, user_id INTEGER NOT NULL, PRIMARY KEY (role_name, user_id));

CREATE TABLE key_value_store ("key" VARCHAR(255), "value" VARCHAR, PRIMARY KEY ("key"));

CREATE TABLE hostconfigmapping (cluster_id BIGINT NOT NULL, host_name VARCHAR(255) NOT NULL, type_name VARCHAR(255) NOT NULL, version_tag VARCHAR(255) NOT NULL, service_name VARCHAR(255), create_timestamp BIGINT NOT NULL, selected INTEGER NOT NULL DEFAULT 0, user_name VARCHAR(255) NOT NULL DEFAULT '_db', PRIMARY KEY (cluster_id, host_name, type_name, create_timestamp));

CREATE TABLE metainfo ("metainfo_key" VARCHAR(255), "metainfo_value" VARCHAR, PRIMARY KEY ("metainfo_key"));

CREATE TABLE ambari_sequences (sequence_name VARCHAR(255) PRIMARY KEY, "value" BIGINT NOT NULL);

CREATE TABLE configgroup (group_id BIGINT, cluster_id BIGINT NOT NULL, group_name VARCHAR(255) NOT NULL, tag VARCHAR(1024) NOT NULL, description VARCHAR(1024), create_timestamp BIGINT NOT NULL, PRIMARY KEY(group_id));

CREATE TABLE confgroupclusterconfigmapping (config_group_id BIGINT NOT NULL, cluster_id BIGINT NOT NULL, config_type VARCHAR(255) NOT NULL, version_tag VARCHAR(255) NOT NULL, user_name VARCHAR(255) DEFAULT '_db', create_timestamp BIGINT NOT NULL, PRIMARY KEY(config_group_id, cluster_id, config_type));

CREATE TABLE configgrouphostmapping (config_group_id BIGINT NOT NULL, host_name VARCHAR(255) NOT NULL, PRIMARY KEY(config_group_id, host_name));

CREATE TABLE requestschedule (schedule_id bigint, cluster_id bigint NOT NULL, description varchar(255), status varchar(255), batch_separation_seconds smallint, batch_toleration_limit smallint, create_user varchar(255), create_timestamp bigint, update_user varchar(255), update_timestamp bigint, minutes varchar(10), hours varchar(10), days_of_month varchar(10), month varchar(10), day_of_week varchar(10), yearToSchedule varchar(10), startTime varchar(50), endTime varchar(50), last_execution_status varchar(255), PRIMARY KEY(schedule_id));

CREATE TABLE requestschedulebatchrequest (schedule_id bigint, batch_id bigint, request_id bigint, request_type varchar(255), request_uri varchar(1024), request_body BYTEA, request_status varchar(255), return_code smallint, return_message varchar(20000), PRIMARY KEY(schedule_id, batch_id));

CREATE TABLE blueprint (blueprint_name VARCHAR(255) NOT NULL, stack_name VARCHAR(255) NOT NULL, stack_version VARCHAR(255) NOT NULL, PRIMARY KEY(blueprint_name));
CREATE TABLE hostgroup (blueprint_name VARCHAR(255) NOT NULL, name VARCHAR(255) NOT NULL, cardinality VARCHAR(255) NOT NULL, PRIMARY KEY(blueprint_name, name));
CREATE TABLE hostgroup_component (blueprint_name VARCHAR(255) NOT NULL, hostgroup_name VARCHAR(255) NOT NULL, name VARCHAR(255) NOT NULL, PRIMARY KEY(blueprint_name, hostgroup_name, name));
CREATE TABLE blueprint_configuration (blueprint_name varchar(255) NOT NULL, type_name varchar(255) NOT NULL, config_data varchar(32000) NOT NULL , config_attributes varchar(32000), PRIMARY KEY(blueprint_name, type_name));
CREATE TABLE hostgroup_configuration (blueprint_name VARCHAR(255) NOT NULL, hostgroup_name VARCHAR(255) NOT NULL, type_name VARCHAR(255) NOT NULL, config_data TEXT NOT NULL, config_attributes varchar(32000), PRIMARY KEY(blueprint_name, hostgroup_name, type_name));

CREATE TABLE viewmain (view_name VARCHAR(255) NOT NULL, label VARCHAR(255), version VARCHAR(255), resource_type_id INTEGER NOT NULL, icon VARCHAR(255), icon64 VARCHAR(255), archive VARCHAR(255), mask VARCHAR(255), PRIMARY KEY(view_name));
CREATE TABLE viewinstancedata (view_instance_id BIGINT, view_name VARCHAR(255) NOT NULL, view_instance_name VARCHAR(255) NOT NULL, name VARCHAR(255) NOT NULL, user_name VARCHAR(255) NOT NULL, value VARCHAR(2000) NOT NULL, PRIMARY KEY(view_instance_id, name, user_name));
CREATE TABLE viewinstance (view_instance_id BIGINT, resource_id BIGINT NOT NULL, view_name VARCHAR(255) NOT NULL, name VARCHAR(255) NOT NULL, label VARCHAR(255), description VARCHAR(255), visible CHAR(1), icon VARCHAR(255), icon64 VARCHAR(255), xml_driven CHAR(1), PRIMARY KEY(view_instance_id));
CREATE TABLE viewinstanceproperty (view_name VARCHAR(255) NOT NULL, view_instance_name VARCHAR(255) NOT NULL, name VARCHAR(255) NOT NULL, value VARCHAR(2000) NOT NULL, PRIMARY KEY(view_name, view_instance_name, name));
CREATE TABLE viewparameter (view_name VARCHAR(255) NOT NULL, name VARCHAR(255) NOT NULL, description VARCHAR(255), required CHAR(1), masked CHAR(1), PRIMARY KEY(view_name, name));
CREATE TABLE viewresource (view_name VARCHAR(255) NOT NULL, name VARCHAR(255) NOT NULL, plural_name VARCHAR(255), id_property VARCHAR(255), subResource_names VARCHAR(255), provider VARCHAR(255), service VARCHAR(255), resource VARCHAR(255), PRIMARY KEY(view_name, name));
CREATE TABLE viewentity (id BIGINT NOT NULL, view_name VARCHAR(255) NOT NULL, view_instance_name VARCHAR(255) NOT NULL, class_name VARCHAR(255) NOT NULL, id_property VARCHAR(255), PRIMARY KEY(id));

CREATE TABLE adminresourcetype (resource_type_id INTEGER NOT NULL, resource_type_name VARCHAR(255) NOT NULL, PRIMARY KEY(resource_type_id));
CREATE TABLE adminresource (resource_id BIGINT NOT NULL, resource_type_id INTEGER NOT NULL, PRIMARY KEY(resource_id));
CREATE TABLE adminprincipaltype (principal_type_id INTEGER NOT NULL, principal_type_name VARCHAR(255) NOT NULL, PRIMARY KEY(principal_type_id));
CREATE TABLE adminprincipal (principal_id BIGINT NOT NULL, principal_type_id INTEGER NOT NULL, PRIMARY KEY(principal_id));
CREATE TABLE adminpermission (permission_id BIGINT NOT NULL, permission_name VARCHAR(255) NOT NULL, resource_type_id INTEGER NOT NULL, PRIMARY KEY(permission_id));
CREATE TABLE adminprivilege (privilege_id BIGINT, permission_id BIGINT NOT NULL, resource_id BIGINT NOT NULL, principal_id BIGINT NOT NULL, PRIMARY KEY(privilege_id));

--------altering tables by creating unique constraints----------
ALTER TABLE clusterconfig ADD CONSTRAINT UQ_config_type_tag UNIQUE (cluster_id, type_name, version_tag);
ALTER TABLE clusterconfig ADD CONSTRAINT UQ_config_type_version UNIQUE (cluster_id, type_name, version);
ALTER TABLE viewinstance ADD CONSTRAINT UQ_viewinstance_name UNIQUE (view_name, name);
ALTER TABLE viewinstance ADD CONSTRAINT UQ_viewinstance_name_id UNIQUE (view_instance_id, view_name, name);
ALTER TABLE serviceconfig ADD CONSTRAINT UQ_scv_service_version UNIQUE (cluster_id, service_name, version);
ALTER TABLE adminpermission ADD CONSTRAINT UQ_perm_name_resource_type_id UNIQUE (permission_name, resource_type_id);

--------altering tables by creating foreign keys----------
ALTER TABLE members ADD CONSTRAINT FK_members_group_id FOREIGN KEY (group_id) REFERENCES groups (group_id);
ALTER TABLE members ADD CONSTRAINT FK_members_user_id FOREIGN KEY (user_id) REFERENCES users (user_id);
ALTER TABLE clusterconfig ADD CONSTRAINT FK_clusterconfig_cluster_id FOREIGN KEY (cluster_id) REFERENCES clusters (cluster_id);
ALTER TABLE clusterservices ADD CONSTRAINT FK_clusterservices_cluster_id FOREIGN KEY (cluster_id) REFERENCES clusters (cluster_id);
ALTER TABLE clusterconfigmapping ADD CONSTRAINT clusterconfigmappingcluster_id FOREIGN KEY (cluster_id) REFERENCES clusters (cluster_id);
ALTER TABLE clusterstate ADD CONSTRAINT FK_clusterstate_cluster_id FOREIGN KEY (cluster_id) REFERENCES clusters (cluster_id);
ALTER TABLE hostcomponentdesiredstate ADD CONSTRAINT hstcmponentdesiredstatehstname FOREIGN KEY (host_name) REFERENCES hosts (host_name);
ALTER TABLE hostcomponentdesiredstate ADD CONSTRAINT hstcmpnntdesiredstatecmpnntnme FOREIGN KEY (component_name, cluster_id, service_name) REFERENCES servicecomponentdesiredstate (component_name, cluster_id, service_name);
ALTER TABLE hostcomponentstate ADD CONSTRAINT hstcomponentstatecomponentname FOREIGN KEY (component_name, cluster_id, service_name) REFERENCES servicecomponentdesiredstate (component_name, cluster_id, service_name);
ALTER TABLE hostcomponentstate ADD CONSTRAINT hostcomponentstate_host_name FOREIGN KEY (host_name) REFERENCES hosts (host_name);
ALTER TABLE hoststate ADD CONSTRAINT FK_hoststate_host_name FOREIGN KEY (host_name) REFERENCES hosts (host_name);
ALTER TABLE servicecomponentdesiredstate ADD CONSTRAINT srvccmponentdesiredstatesrvcnm FOREIGN KEY (service_name, cluster_id) REFERENCES clusterservices (service_name, cluster_id);
ALTER TABLE servicedesiredstate ADD CONSTRAINT servicedesiredstateservicename FOREIGN KEY (service_name, cluster_id) REFERENCES clusterservices (service_name, cluster_id);
ALTER TABLE execution_command ADD CONSTRAINT FK_execution_command_task_id FOREIGN KEY (task_id) REFERENCES host_role_command (task_id);
ALTER TABLE host_role_command ADD CONSTRAINT FK_host_role_command_stage_id FOREIGN KEY (stage_id, request_id) REFERENCES stage (stage_id, request_id);
ALTER TABLE host_role_command ADD CONSTRAINT FK_host_role_command_host_name FOREIGN KEY (host_name) REFERENCES hosts (host_name);
ALTER TABLE role_success_criteria ADD CONSTRAINT role_success_criteria_stage_id FOREIGN KEY (stage_id, request_id) REFERENCES stage (stage_id, request_id);
ALTER TABLE stage ADD CONSTRAINT FK_stage_request_id FOREIGN KEY (request_id) REFERENCES request (request_id);
ALTER TABLE request ADD CONSTRAINT FK_request_schedule_id FOREIGN KEY (request_schedule_id) REFERENCES requestschedule (schedule_id);
ALTER TABLE ClusterHostMapping ADD CONSTRAINT ClusterHostMapping_host_name FOREIGN KEY (cluster_id) REFERENCES clusters (cluster_id);
ALTER TABLE user_roles ADD CONSTRAINT FK_user_roles_user_id FOREIGN KEY (user_id) REFERENCES users (user_id);
ALTER TABLE user_roles ADD CONSTRAINT FK_user_roles_role_name FOREIGN KEY (role_name) REFERENCES roles (role_name);
ALTER TABLE hostconfigmapping ADD CONSTRAINT FK_hostconfmapping_cluster_id FOREIGN KEY (cluster_id) REFERENCES clusters (cluster_id);
ALTER TABLE hostconfigmapping ADD CONSTRAINT FK_hostconfmapping_host_name FOREIGN KEY (host_name) REFERENCES hosts (host_name);
ALTER TABLE configgroup ADD CONSTRAINT FK_configgroup_cluster_id FOREIGN KEY (cluster_id) REFERENCES clusters (cluster_id);
ALTER TABLE confgroupclusterconfigmapping ADD CONSTRAINT FK_confg FOREIGN KEY (version_tag, config_type, cluster_id) REFERENCES clusterconfig (version_tag, type_name, cluster_id);
ALTER TABLE confgroupclusterconfigmapping ADD CONSTRAINT FK_cgccm_gid FOREIGN KEY (config_group_id) REFERENCES configgroup (group_id);
ALTER TABLE configgrouphostmapping ADD CONSTRAINT FK_cghm_cgid FOREIGN KEY (config_group_id) REFERENCES configgroup (group_id);
ALTER TABLE configgrouphostmapping ADD CONSTRAINT FK_cghm_hname FOREIGN KEY (host_name) REFERENCES hosts (host_name);
ALTER TABLE requestschedulebatchrequest ADD CONSTRAINT FK_rsbatchrequest_schedule_id FOREIGN KEY (schedule_id) REFERENCES requestschedule (schedule_id);
ALTER TABLE hostgroup ADD CONSTRAINT FK_hg_blueprint_name FOREIGN KEY (blueprint_name) REFERENCES blueprint(blueprint_name);
ALTER TABLE hostgroup_component ADD CONSTRAINT FK_hgc_blueprint_name FOREIGN KEY (blueprint_name, hostgroup_name) REFERENCES hostgroup (blueprint_name, name);
ALTER TABLE blueprint_configuration ADD CONSTRAINT FK_cfg_blueprint_name FOREIGN KEY (blueprint_name) REFERENCES blueprint(blueprint_name);
ALTER TABLE hostgroup_configuration ADD CONSTRAINT FK_hg_cfg_bp_hg_name FOREIGN KEY (blueprint_name, hostgroup_name) REFERENCES hostgroup (blueprint_name, name);
ALTER TABLE requestresourcefilter ADD CONSTRAINT FK_reqresfilter_req_id FOREIGN KEY (request_id) REFERENCES request (request_id);
ALTER TABLE requestoperationlevel ADD CONSTRAINT FK_req_op_level_req_id FOREIGN KEY (request_id) REFERENCES request (request_id);
ALTER TABLE viewparameter ADD CONSTRAINT FK_viewparam_view_name FOREIGN KEY (view_name) REFERENCES viewmain(view_name);
ALTER TABLE viewresource ADD CONSTRAINT FK_viewres_view_name FOREIGN KEY (view_name) REFERENCES viewmain(view_name);
ALTER TABLE viewinstance ADD CONSTRAINT FK_viewinst_view_name FOREIGN KEY (view_name) REFERENCES viewmain(view_name);
ALTER TABLE viewinstanceproperty ADD CONSTRAINT FK_viewinstprop_view_name FOREIGN KEY (view_name, view_instance_name) REFERENCES viewinstance(view_name, name);
ALTER TABLE viewinstancedata ADD CONSTRAINT FK_viewinstdata_view_name FOREIGN KEY (view_instance_id, view_name, view_instance_name) REFERENCES viewinstance(view_instance_id, view_name, name);
ALTER TABLE viewentity ADD CONSTRAINT FK_viewentity_view_name FOREIGN KEY (view_name, view_instance_name) REFERENCES viewinstance(view_name, name);
ALTER TABLE adminresource ADD CONSTRAINT FK_resource_resource_type_id FOREIGN KEY (resource_type_id) REFERENCES adminresourcetype(resource_type_id);
ALTER TABLE adminprincipal ADD CONSTRAINT FK_principal_principal_type_id FOREIGN KEY (principal_type_id) REFERENCES adminprincipaltype(principal_type_id);
ALTER TABLE adminpermission ADD CONSTRAINT FK_permission_resource_type_id FOREIGN KEY (resource_type_id) REFERENCES adminresourcetype(resource_type_id);
ALTER TABLE adminprivilege ADD CONSTRAINT FK_privilege_permission_id FOREIGN KEY (permission_id) REFERENCES adminpermission(permission_id);
ALTER TABLE adminprivilege ADD CONSTRAINT FK_privilege_resource_id FOREIGN KEY (resource_id) REFERENCES adminresource(resource_id);
ALTER TABLE viewmain ADD CONSTRAINT FK_view_resource_type_id FOREIGN KEY (resource_type_id) REFERENCES adminresourcetype(resource_type_id);
ALTER TABLE viewinstance ADD CONSTRAINT FK_viewinstance_resource_id FOREIGN KEY (resource_id) REFERENCES adminresource(resource_id);
ALTER TABLE adminprivilege ADD CONSTRAINT FK_privilege_principal_id FOREIGN KEY (principal_id) REFERENCES adminprincipal(principal_id);
ALTER TABLE users ADD CONSTRAINT FK_users_principal_id FOREIGN KEY (principal_id) REFERENCES adminprincipal(principal_id);
ALTER TABLE groups ADD CONSTRAINT FK_groups_principal_id FOREIGN KEY (principal_id) REFERENCES adminprincipal(principal_id);
ALTER TABLE serviceconfigmapping ADD CONSTRAINT FK_scvm_scv FOREIGN KEY (service_config_id) REFERENCES serviceconfig(service_config_id);
ALTER TABLE serviceconfigmapping ADD CONSTRAINT FK_scvm_config FOREIGN KEY (config_id) REFERENCES clusterconfig(config_id);
ALTER TABLE serviceconfigapplication ADD CONSTRAINT FK_scva_scv FOREIGN KEY (service_config_id) REFERENCES serviceconfig(service_config_id);
ALTER TABLE clusters ADD CONSTRAINT FK_clusters_resource_id FOREIGN KEY (resource_id) REFERENCES adminresource(resource_id);

-- Alerting Framework
CREATE TABLE alert_definition (
  definition_id BIGINT NOT NULL, 
  cluster_id BIGINT NOT NULL, 
  definition_name VARCHAR(255) NOT NULL,
  service_name VARCHAR(255) NOT NULL,
  component_name VARCHAR(255),
  scope VARCHAR(255),
  enabled SMALLINT DEFAULT 1 NOT NULL,
  schedule_interval INTEGER NOT NULL,
  source_type VARCHAR(255) NOT NULL,
  alert_source TEXT NOT NULL,
  hash VARCHAR(64) NOT NULL,
  PRIMARY KEY (definition_id),
  FOREIGN KEY (cluster_id) REFERENCES clusters(cluster_id),
  CONSTRAINT uni_alert_def_name UNIQUE(cluster_id,definition_name)
);

CREATE TABLE alert_history (
  alert_id BIGINT NOT NULL,
  cluster_id BIGINT NOT NULL,
  alert_definition_id BIGINT NOT NULL,
  service_name VARCHAR(255) NOT NULL,
  component_name VARCHAR(255),
  host_name VARCHAR(255),
  alert_instance VARCHAR(255),
  alert_timestamp BIGINT NOT NULL,
  alert_label VARCHAR(1024),
  alert_state VARCHAR(255) NOT NULL,
  alert_text TEXT,
  PRIMARY KEY (alert_id),
  FOREIGN KEY (alert_definition_id) REFERENCES alert_definition(definition_id),
  FOREIGN KEY (cluster_id) REFERENCES clusters(cluster_id)
);

CREATE TABLE alert_current (
  alert_id BIGINT NOT NULL,
  definition_id BIGINT NOT NULL,
  history_id BIGINT NOT NULL UNIQUE,
  maintenance_state VARCHAR(255),
  original_timestamp BIGINT NOT NULL,
  latest_timestamp BIGINT NOT NULL,
  PRIMARY KEY (alert_id),
  FOREIGN KEY (definition_id) REFERENCES alert_definition(definition_id),
  FOREIGN KEY (history_id) REFERENCES alert_history(alert_id)
);

CREATE TABLE alert_group (
  group_id BIGINT NOT NULL,
  cluster_id BIGINT NOT NULL,
  group_name VARCHAR(255) NOT NULL,
  is_default SMALLINT NOT NULL DEFAULT 0,
  PRIMARY KEY (group_id),
  CONSTRAINT uni_alert_group_name UNIQUE(cluster_id,group_name)
);

CREATE TABLE alert_target (
  target_id BIGINT NOT NULL,
  target_name VARCHAR(255) NOT NULL UNIQUE,
  notification_type VARCHAR(64) NOT NULL,
  properties TEXT,
  description VARCHAR(1024),
  PRIMARY KEY (target_id)
);

CREATE TABLE alert_group_target (
  group_id BIGINT NOT NULL,
  target_id BIGINT NOT NULL,
  PRIMARY KEY (group_id, target_id),
  FOREIGN KEY (group_id) REFERENCES alert_group(group_id),
  FOREIGN KEY (target_id) REFERENCES alert_target(target_id)
);

CREATE TABLE alert_grouping (
  definition_id BIGINT NOT NULL,
  group_id BIGINT NOT NULL,
  PRIMARY KEY (group_id, definition_id),
  FOREIGN KEY (definition_id) REFERENCES alert_definition(definition_id),
  FOREIGN KEY (group_id) REFERENCES alert_group(group_id)
);

CREATE TABLE alert_notice (
  notification_id BIGINT NOT NULL,
  target_id BIGINT NOT NULL,
  history_id BIGINT NOT NULL,
  notify_state VARCHAR(255) NOT NULL,
  PRIMARY KEY (notification_id),
  FOREIGN KEY (target_id) REFERENCES alert_target(target_id),  
  FOREIGN KEY (history_id) REFERENCES alert_history(alert_id)
);

CREATE INDEX idx_alert_history_def_id on alert_history(alert_definition_id);
CREATE INDEX idx_alert_history_service on alert_history(service_name);
CREATE INDEX idx_alert_history_host on alert_history(host_name);
CREATE INDEX idx_alert_history_time on alert_history(alert_timestamp);
CREATE INDEX idx_alert_history_state on alert_history(alert_state);
CREATE INDEX idx_alert_group_name on alert_group(group_name);
CREATE INDEX idx_alert_notice_state on alert_notice(notify_state);

---------inserting some data-----------
BEGIN;
  INSERT INTO ambari_sequences (sequence_name, "value")
  SELECT 'cluster_id_seq', 1
  UNION ALL
  SELECT 'user_id_seq', 2
  UNION ALL
  SELECT 'group_id_seq', 1
  UNION ALL
  SELECT 'member_id_seq', 1
  UNION ALL
  SELECT 'host_role_command_id_seq', 1
  union all
  select 'configgroup_id_seq', 1
  union all
  select 'requestschedule_id_seq', 1
  union all
  select 'resourcefilter_id_seq', 1
  union all
  select 'viewentity_id_seq', 0
  union all
  select 'operation_level_id_seq', 1
  union all
  select 'view_instance_id_seq', 1
  union all
  select 'resource_type_id_seq', 4
  union all
  select 'resource_id_seq', 2
  union all
  select 'principal_type_id_seq', 3
  union all
  select 'principal_id_seq', 2
  union all
  select 'permission_id_seq', 5
  union all
  select 'privilege_id_seq', 1
  union all
  select 'alert_definition_id_seq', 0
  union all
  select 'alert_group_id_seq', 0
  union all
  select 'alert_target_id_seq', 0
  union all
  select 'alert_history_id_seq', 0
  union all
  select 'alert_notice_id_seq', 0
  union all
  select 'alert_current_id_seq', 0
  union all
  select 'config_id_seq', 1
  union all
  select 'service_config_id_seq', 1
  union  all
  select 'service_config_application_id_seq', 1;

  INSERT INTO adminresourcetype (resource_type_id, resource_type_name)
  SELECT 1, 'AMBARI'
  UNION ALL
  SELECT 2, 'CLUSTER'
  UNION ALL
  SELECT 3, 'VIEW';

  INSERT INTO adminresource (resource_id, resource_type_id)
  SELECT 1, 1;

  INSERT INTO Roles (role_name)
  SELECT 'admin'
  UNION ALL
  SELECT 'user';

  INSERT INTO adminprincipaltype (principal_type_id, principal_type_name)
  SELECT 1, 'USER'
  UNION ALL
  SELECT 2, 'GROUP';

  INSERT INTO adminprincipal (principal_id, principal_type_id)
  SELECT 1, 1;

  INSERT INTO Users (user_id, principal_id, user_name, user_password)
  SELECT 1, 1, 'admin', '538916f8943ec225d97a9a86a2c6ec0818c1cd400e09e03b660fdaaec4af29ddbb6f2b1033b81b00';

  INSERT INTO user_roles (role_name, user_id)
  SELECT 'admin', 1;

  INSERT INTO adminpermission(permission_id, permission_name, resource_type_id)
  SELECT 1, 'AMBARI.ADMIN', 1
  UNION ALL
  SELECT 2, 'CLUSTER.READ', 2
  UNION ALL
  SELECT 3, 'CLUSTER.OPERATE', 2
  UNION ALL
  SELECT 4, 'VIEW.USE', 3;

  INSERT INTO adminprivilege (privilege_id, permission_id, resource_id, principal_id)
  SELECT 1, 1, 1, 1;

  INSERT INTO metainfo (metainfo_key, metainfo_value)
  SELECT 'version', '${ambariVersion}';
COMMIT;

-- Quartz tables

CREATE TABLE qrtz_job_details
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    JOB_NAME  VARCHAR(200) NOT NULL,
    JOB_GROUP VARCHAR(200) NOT NULL,
    DESCRIPTION VARCHAR(250) NULL,
    JOB_CLASS_NAME   VARCHAR(250) NOT NULL,
    IS_DURABLE BOOL NOT NULL,
    IS_NONCONCURRENT BOOL NOT NULL,
    IS_UPDATE_DATA BOOL NOT NULL,
    REQUESTS_RECOVERY BOOL NOT NULL,
    JOB_DATA BYTEA NULL,
    PRIMARY KEY (SCHED_NAME,JOB_NAME,JOB_GROUP)
);

CREATE TABLE qrtz_triggers
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    JOB_NAME  VARCHAR(200) NOT NULL,
    JOB_GROUP VARCHAR(200) NOT NULL,
    DESCRIPTION VARCHAR(250) NULL,
    NEXT_FIRE_TIME BIGINT NULL,
    PREV_FIRE_TIME BIGINT NULL,
    PRIORITY INTEGER NULL,
    TRIGGER_STATE VARCHAR(16) NOT NULL,
    TRIGGER_TYPE VARCHAR(8) NOT NULL,
    START_TIME BIGINT NOT NULL,
    END_TIME BIGINT NULL,
    CALENDAR_NAME VARCHAR(200) NULL,
    MISFIRE_INSTR SMALLINT NULL,
    JOB_DATA BYTEA NULL,
    PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME,JOB_NAME,JOB_GROUP)
	REFERENCES QRTZ_JOB_DETAILS(SCHED_NAME,JOB_NAME,JOB_GROUP)
);

CREATE TABLE qrtz_simple_triggers
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    REPEAT_COUNT BIGINT NOT NULL,
    REPEAT_INTERVAL BIGINT NOT NULL,
    TIMES_TRIGGERED BIGINT NOT NULL,
    PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
	REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
);

CREATE TABLE qrtz_cron_triggers
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    CRON_EXPRESSION VARCHAR(120) NOT NULL,
    TIME_ZONE_ID VARCHAR(80),
    PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
	REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
);

CREATE TABLE qrtz_simprop_triggers
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    STR_PROP_1 VARCHAR(512) NULL,
    STR_PROP_2 VARCHAR(512) NULL,
    STR_PROP_3 VARCHAR(512) NULL,
    INT_PROP_1 INT NULL,
    INT_PROP_2 INT NULL,
    LONG_PROP_1 BIGINT NULL,
    LONG_PROP_2 BIGINT NULL,
    DEC_PROP_1 NUMERIC(13,4) NULL,
    DEC_PROP_2 NUMERIC(13,4) NULL,
    BOOL_PROP_1 BOOL NULL,
    BOOL_PROP_2 BOOL NULL,
    PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
    REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
);

CREATE TABLE qrtz_blob_triggers
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    BLOB_DATA BYTEA NULL,
    PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
);

CREATE TABLE qrtz_calendars
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    CALENDAR_NAME  VARCHAR(200) NOT NULL,
    CALENDAR BYTEA NOT NULL,
    PRIMARY KEY (SCHED_NAME,CALENDAR_NAME)
);


CREATE TABLE qrtz_paused_trigger_grps
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_GROUP  VARCHAR(200) NOT NULL,
    PRIMARY KEY (SCHED_NAME,TRIGGER_GROUP)
);

CREATE TABLE qrtz_fired_triggers
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    ENTRY_ID VARCHAR(95) NOT NULL,
    TRIGGER_NAME VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    INSTANCE_NAME VARCHAR(200) NOT NULL,
    FIRED_TIME BIGINT NOT NULL,
    SCHED_TIME BIGINT NOT NULL,
    PRIORITY INTEGER NOT NULL,
    STATE VARCHAR(16) NOT NULL,
    JOB_NAME VARCHAR(200) NULL,
    JOB_GROUP VARCHAR(200) NULL,
    IS_NONCONCURRENT BOOL NULL,
    REQUESTS_RECOVERY BOOL NULL,
    PRIMARY KEY (SCHED_NAME,ENTRY_ID)
);

CREATE TABLE qrtz_scheduler_state
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    INSTANCE_NAME VARCHAR(200) NOT NULL,
    LAST_CHECKIN_TIME BIGINT NOT NULL,
    CHECKIN_INTERVAL BIGINT NOT NULL,
    PRIMARY KEY (SCHED_NAME,INSTANCE_NAME)
);

CREATE TABLE qrtz_locks
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    LOCK_NAME  VARCHAR(40) NOT NULL,
    PRIMARY KEY (SCHED_NAME,LOCK_NAME)
);

create index idx_qrtz_j_req_recovery on qrtz_job_details(SCHED_NAME,REQUESTS_RECOVERY);
create index idx_qrtz_j_grp on qrtz_job_details(SCHED_NAME,JOB_GROUP);

create index idx_qrtz_t_j on qrtz_triggers(SCHED_NAME,JOB_NAME,JOB_GROUP);
create index idx_qrtz_t_jg on qrtz_triggers(SCHED_NAME,JOB_GROUP);
create index idx_qrtz_t_c on qrtz_triggers(SCHED_NAME,CALENDAR_NAME);
create index idx_qrtz_t_g on qrtz_triggers(SCHED_NAME,TRIGGER_GROUP);
create index idx_qrtz_t_state on qrtz_triggers(SCHED_NAME,TRIGGER_STATE);
create index idx_qrtz_t_n_state on qrtz_triggers(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP,TRIGGER_STATE);
create index idx_qrtz_t_n_g_state on qrtz_triggers(SCHED_NAME,TRIGGER_GROUP,TRIGGER_STATE);
create index idx_qrtz_t_next_fire_time on qrtz_triggers(SCHED_NAME,NEXT_FIRE_TIME);
create index idx_qrtz_t_nft_st on qrtz_triggers(SCHED_NAME,TRIGGER_STATE,NEXT_FIRE_TIME);
create index idx_qrtz_t_nft_misfire on qrtz_triggers(SCHED_NAME,MISFIRE_INSTR,NEXT_FIRE_TIME);
create index idx_qrtz_t_nft_st_misfire on qrtz_triggers(SCHED_NAME,MISFIRE_INSTR,NEXT_FIRE_TIME,TRIGGER_STATE);
create index idx_qrtz_t_nft_st_misfire_grp on qrtz_triggers(SCHED_NAME,MISFIRE_INSTR,NEXT_FIRE_TIME,TRIGGER_GROUP,TRIGGER_STATE);

create index idx_qrtz_ft_trig_inst_name on qrtz_fired_triggers(SCHED_NAME,INSTANCE_NAME);
create index idx_qrtz_ft_inst_job_req_rcvry on qrtz_fired_triggers(SCHED_NAME,INSTANCE_NAME,REQUESTS_RECOVERY);
create index idx_qrtz_ft_j_g on qrtz_fired_triggers(SCHED_NAME,JOB_NAME,JOB_GROUP);
create index idx_qrtz_ft_jg on qrtz_fired_triggers(SCHED_NAME,JOB_GROUP);
create index idx_qrtz_ft_t_g on qrtz_fired_triggers(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP);
create index idx_qrtz_ft_tg on qrtz_fired_triggers(SCHED_NAME,TRIGGER_GROUP);

-- ambari log4j DDL

CREATE TABLE workflow (
  workflowId       TEXT, workflowName TEXT,
  parentWorkflowId TEXT,
  workflowContext  TEXT, userName TEXT,
  startTime        BIGINT, lastUpdateTime BIGINT,
  numJobsTotal     INTEGER, numJobsCompleted INTEGER,
  inputBytes       BIGINT, outputBytes BIGINT,
  duration         BIGINT,
  PRIMARY KEY (workflowId),
  FOREIGN KEY (parentWorkflowId) REFERENCES workflow (workflowId) ON DELETE CASCADE
);

CREATE TABLE job (
  jobId        TEXT, workflowId TEXT, jobName TEXT, workflowEntityName TEXT,
  userName     TEXT, queue TEXT, acls TEXT, confPath TEXT,
  submitTime   BIGINT, launchTime BIGINT, finishTime BIGINT,
  maps         INTEGER, reduces INTEGER, status TEXT, priority TEXT,
  finishedMaps INTEGER, finishedReduces INTEGER,
  failedMaps   INTEGER, failedReduces INTEGER,
  mapsRuntime  BIGINT, reducesRuntime BIGINT,
  mapCounters  TEXT, reduceCounters TEXT, jobCounters TEXT,
  inputBytes   BIGINT, outputBytes BIGINT,
  PRIMARY KEY (jobId),
  FOREIGN KEY (workflowId) REFERENCES workflow (workflowId) ON DELETE CASCADE
);

CREATE TABLE task (
  taskId        TEXT, jobId TEXT, taskType TEXT, splits TEXT,
  startTime     BIGINT, finishTime BIGINT, status TEXT, error TEXT, counters TEXT,
  failedAttempt TEXT,
  PRIMARY KEY (taskId),
  FOREIGN KEY (jobId) REFERENCES job (jobId) ON DELETE CASCADE
);

CREATE TABLE taskAttempt (
  taskAttemptId TEXT, taskId TEXT, jobId TEXT, taskType TEXT, taskTracker TEXT,
  startTime     BIGINT, finishTime BIGINT,
  mapFinishTime BIGINT, shuffleFinishTime BIGINT, sortFinishTime BIGINT,
  locality      TEXT, avataar TEXT,
  status        TEXT, error TEXT, counters TEXT,
  inputBytes    BIGINT, outputBytes BIGINT,
  PRIMARY KEY (taskAttemptId),
  FOREIGN KEY (jobId) REFERENCES job (jobId) ON DELETE CASCADE,
  FOREIGN KEY (taskId) REFERENCES task (taskId) ON DELETE CASCADE
);

CREATE TABLE hdfsEvent (
  timestamp   BIGINT,
  userName    TEXT,
  clientIP    TEXT,
  operation   TEXT,
  srcPath     TEXT,
  dstPath     TEXT,
  permissions TEXT
);

CREATE TABLE mapreduceEvent (
  timestamp   BIGINT,
  userName    TEXT,
  clientIP    TEXT,
  operation   TEXT,
  target      TEXT,
  result      TEXT,
  description TEXT,
  permissions TEXT
);

CREATE TABLE clusterEvent (
  timestamp BIGINT,
  service   TEXT, status TEXT,
  error     TEXT, data TEXT,
  host      TEXT, rack TEXT
);

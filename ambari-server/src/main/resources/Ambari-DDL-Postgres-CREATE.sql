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
CREATE DATABASE :dbname;
\connect :dbname;

ALTER ROLE :username LOGIN ENCRYPTED PASSWORD :password;
CREATE ROLE :username LOGIN ENCRYPTED PASSWORD :password;

GRANT ALL PRIVILEGES ON DATABASE :dbname TO :username;

CREATE SCHEMA ambari AUTHORIZATION :username;
ALTER SCHEMA ambari OWNER TO :username;
ALTER ROLE :username SET search_path TO 'ambari';

------create tables ang grant privileges to db user---------
CREATE TABLE ambari.clusters (cluster_id BIGINT NOT NULL, cluster_info VARCHAR(255) NOT NULL, cluster_name VARCHAR(100) NOT NULL UNIQUE, desired_cluster_state VARCHAR(255) NOT NULL, desired_stack_version VARCHAR(255) NOT NULL, PRIMARY KEY (cluster_id));
GRANT ALL PRIVILEGES ON TABLE ambari.clusters TO :username;

CREATE TABLE ambari.clusterconfig (version_tag VARCHAR(255) NOT NULL, type_name VARCHAR(255) NOT NULL, cluster_id BIGINT NOT NULL, config_data VARCHAR(32000) NOT NULL, create_timestamp BIGINT NOT NULL, PRIMARY KEY (cluster_id, type_name, version_tag));
GRANT ALL PRIVILEGES ON TABLE ambari.clusterconfig TO :username;

CREATE TABLE ambari.clusterconfigmapping (cluster_id BIGINT NOT NULL, type_name VARCHAR(255) NOT NULL, version_tag VARCHAR(255) NOT NULL, create_timestamp BIGINT NOT NULL, selected INTEGER NOT NULL DEFAULT 0, user_name VARCHAR(255) NOT NULL DEFAULT '_db', PRIMARY KEY (cluster_id, type_name, create_timestamp));
GRANT ALL PRIVILEGES ON TABLE ambari.clusterconfigmapping TO :username;

CREATE TABLE ambari.clusterservices (service_name VARCHAR(255) NOT NULL, cluster_id BIGINT NOT NULL, service_enabled INTEGER NOT NULL, PRIMARY KEY (service_name, cluster_id));
GRANT ALL PRIVILEGES ON TABLE ambari.clusterservices TO :username;

CREATE TABLE ambari.clusterstate (cluster_id BIGINT NOT NULL, current_cluster_state VARCHAR(255) NOT NULL, current_stack_version VARCHAR(255) NOT NULL, PRIMARY KEY (cluster_id));
GRANT ALL PRIVILEGES ON TABLE ambari.clusterstate TO :username;

CREATE TABLE ambari.hostcomponentdesiredstate (cluster_id BIGINT NOT NULL, component_name VARCHAR(255) NOT NULL, desired_stack_version VARCHAR(255) NOT NULL, desired_state VARCHAR(255) NOT NULL, host_name VARCHAR(255) NOT NULL, service_name VARCHAR(255) NOT NULL, admin_state VARCHAR(32), maintenance_state VARCHAR(32) NOT NULL, PRIMARY KEY (cluster_id, component_name, host_name, service_name));
GRANT ALL PRIVILEGES ON TABLE ambari.hostcomponentdesiredstate TO :username;

CREATE TABLE ambari.hostcomponentstate (cluster_id BIGINT NOT NULL, component_name VARCHAR(255) NOT NULL, current_stack_version VARCHAR(255) NOT NULL, current_state VARCHAR(255) NOT NULL, host_name VARCHAR(255) NOT NULL, service_name VARCHAR(255) NOT NULL, PRIMARY KEY (cluster_id, component_name, host_name, service_name));
GRANT ALL PRIVILEGES ON TABLE ambari.hostcomponentstate TO :username;

CREATE TABLE ambari.hosts (host_name VARCHAR(255) NOT NULL, cpu_count INTEGER NOT NULL, ph_cpu_count INTEGER, cpu_info VARCHAR(255) NOT NULL, discovery_status VARCHAR(2000) NOT NULL, host_attributes VARCHAR(20000) NOT NULL, ipv4 VARCHAR(255), ipv6 VARCHAR(255), public_host_name VARCHAR(255), last_registration_time BIGINT NOT NULL, os_arch VARCHAR(255) NOT NULL, os_info VARCHAR(1000) NOT NULL, os_type VARCHAR(255) NOT NULL, rack_info VARCHAR(255) NOT NULL, total_mem BIGINT NOT NULL, PRIMARY KEY (host_name));
GRANT ALL PRIVILEGES ON TABLE ambari.hosts TO :username;

CREATE TABLE ambari.hoststate (agent_version VARCHAR(255) NOT NULL, available_mem BIGINT NOT NULL, current_state VARCHAR(255) NOT NULL, health_status VARCHAR(255), host_name VARCHAR(255) NOT NULL, time_in_state BIGINT NOT NULL, maintenance_state VARCHAR(512), PRIMARY KEY (host_name));
GRANT ALL PRIVILEGES ON TABLE ambari.hoststate TO :username;

CREATE TABLE ambari.servicecomponentdesiredstate (component_name VARCHAR(255) NOT NULL, cluster_id BIGINT NOT NULL, desired_stack_version VARCHAR(255) NOT NULL, desired_state VARCHAR(255) NOT NULL, service_name VARCHAR(255) NOT NULL, PRIMARY KEY (component_name, cluster_id, service_name));
GRANT ALL PRIVILEGES ON TABLE ambari.servicecomponentdesiredstate TO :username;

CREATE TABLE ambari.servicedesiredstate (cluster_id BIGINT NOT NULL, desired_host_role_mapping INTEGER NOT NULL, desired_stack_version VARCHAR(255) NOT NULL, desired_state VARCHAR(255) NOT NULL, service_name VARCHAR(255) NOT NULL, maintenance_state VARCHAR(32) NOT NULL, PRIMARY KEY (cluster_id, service_name));
GRANT ALL PRIVILEGES ON TABLE ambari.servicedesiredstate TO :username;

CREATE TABLE ambari.roles (role_name VARCHAR(255) NOT NULL, PRIMARY KEY (role_name));
GRANT ALL PRIVILEGES ON TABLE ambari.roles TO :username;

CREATE TABLE ambari.users (user_id INTEGER, ldap_user INTEGER NOT NULL DEFAULT 0, user_name VARCHAR(255) NOT NULL, create_time TIMESTAMP DEFAULT NOW(), user_password VARCHAR(255), PRIMARY KEY (user_id), UNIQUE (ldap_user, user_name));
GRANT ALL PRIVILEGES ON TABLE ambari.users TO :username;

CREATE TABLE ambari.execution_command (command BYTEA, task_id BIGINT NOT NULL, PRIMARY KEY (task_id));
GRANT ALL PRIVILEGES ON TABLE ambari.execution_command TO :username;

CREATE TABLE ambari.host_role_command (task_id BIGINT NOT NULL, attempt_count SMALLINT NOT NULL, event VARCHAR(32000) NOT NULL, exitcode INTEGER NOT NULL, host_name VARCHAR(255) NOT NULL, last_attempt_time BIGINT NOT NULL, request_id BIGINT NOT NULL, role VARCHAR(255), stage_id BIGINT NOT NULL, start_time BIGINT NOT NULL, end_time BIGINT, status VARCHAR(255), std_error BYTEA, std_out BYTEA, structured_out BYTEA, role_command VARCHAR(255), command_detail VARCHAR(255), custom_command_name VARCHAR(255), PRIMARY KEY (task_id));
GRANT ALL PRIVILEGES ON TABLE ambari.host_role_command TO :username;

CREATE TABLE ambari.role_success_criteria (role VARCHAR(255) NOT NULL, request_id BIGINT NOT NULL, stage_id BIGINT NOT NULL, success_factor FLOAT NOT NULL, PRIMARY KEY (role, request_id, stage_id));
GRANT ALL PRIVILEGES ON TABLE ambari.role_success_criteria TO :username;

CREATE TABLE ambari.stage (stage_id BIGINT NOT NULL, request_id BIGINT NOT NULL, cluster_id BIGINT NOT NULL, log_info VARCHAR(255) NOT NULL, request_context VARCHAR(255), cluster_host_info BYTEA NOT NULL, PRIMARY KEY (stage_id, request_id));
GRANT ALL PRIVILEGES ON TABLE ambari.stage TO :username;

CREATE TABLE ambari.request (request_id BIGINT NOT NULL, cluster_id BIGINT, command_name VARCHAR(255), create_time BIGINT NOT NULL, end_time BIGINT NOT NULL, inputs BYTEA, request_context VARCHAR(255), request_type VARCHAR(255), request_schedule_id BIGINT, start_time BIGINT NOT NULL, status VARCHAR(255), PRIMARY KEY (request_id));
GRANT ALL PRIVILEGES ON TABLE ambari.request TO :username;

CREATE TABLE ambari.requestresourcefilter (filter_id BIGINT NOT NULL, request_id BIGINT NOT NULL, service_name VARCHAR(255), component_name VARCHAR(255), hosts BYTEA, PRIMARY KEY (filter_id));
GRANT ALL PRIVILEGES ON TABLE ambari.requestresourcefilter TO :username;

CREATE TABLE ambari.ClusterHostMapping (cluster_id BIGINT NOT NULL, host_name VARCHAR(255) NOT NULL, PRIMARY KEY (cluster_id, host_name));
GRANT ALL PRIVILEGES ON TABLE ambari.ClusterHostMapping TO :username;

CREATE TABLE ambari.user_roles (role_name VARCHAR(255) NOT NULL, user_id INTEGER NOT NULL, PRIMARY KEY (role_name, user_id));
GRANT ALL PRIVILEGES ON TABLE ambari.user_roles TO :username;

CREATE TABLE ambari.key_value_store ("key" VARCHAR(255), "value" VARCHAR, PRIMARY KEY ("key"));
GRANT ALL PRIVILEGES ON TABLE ambari.key_value_store TO :username;

CREATE TABLE ambari.hostconfigmapping (cluster_id BIGINT NOT NULL, host_name VARCHAR(255) NOT NULL, type_name VARCHAR(255) NOT NULL, version_tag VARCHAR(255) NOT NULL, service_name VARCHAR(255), create_timestamp BIGINT NOT NULL, selected INTEGER NOT NULL DEFAULT 0, user_name VARCHAR(255) NOT NULL DEFAULT '_db', PRIMARY KEY (cluster_id, host_name, type_name, create_timestamp));
GRANT ALL PRIVILEGES ON TABLE ambari.hostconfigmapping TO :username;

CREATE TABLE ambari.metainfo ("metainfo_key" VARCHAR(255), "metainfo_value" VARCHAR, PRIMARY KEY ("metainfo_key"));
GRANT ALL PRIVILEGES ON TABLE ambari.metainfo TO :username;

CREATE TABLE ambari.ambari_sequences (sequence_name VARCHAR(255) PRIMARY KEY, "value" BIGINT NOT NULL);
GRANT ALL PRIVILEGES ON TABLE ambari.ambari_sequences TO :username;

CREATE TABLE ambari.configgroup (group_id BIGINT, cluster_id BIGINT NOT NULL, group_name VARCHAR(255) NOT NULL, tag VARCHAR(1024) NOT NULL, description VARCHAR(1024), create_timestamp BIGINT NOT NULL, PRIMARY KEY(group_id));
GRANT ALL PRIVILEGES ON TABLE ambari.configgroup TO :username;

CREATE TABLE ambari.confgroupclusterconfigmapping (config_group_id BIGINT NOT NULL, cluster_id BIGINT NOT NULL, config_type VARCHAR(255) NOT NULL, version_tag VARCHAR(255) NOT NULL, user_name VARCHAR(255) DEFAULT '_db', create_timestamp BIGINT NOT NULL, PRIMARY KEY(config_group_id, cluster_id, config_type));
GRANT ALL PRIVILEGES ON TABLE ambari.confgroupclusterconfigmapping TO :username;

CREATE TABLE ambari.configgrouphostmapping (config_group_id BIGINT NOT NULL, host_name VARCHAR(255) NOT NULL, PRIMARY KEY(config_group_id, host_name));
GRANT ALL PRIVILEGES ON TABLE ambari.configgrouphostmapping TO :username;

CREATE TABLE ambari.requestschedule (schedule_id bigint, cluster_id bigint NOT NULL, description varchar(255), status varchar(255), batch_separation_seconds smallint, batch_toleration_limit smallint, create_user varchar(255), create_timestamp bigint, update_user varchar(255), update_timestamp bigint, minutes varchar(10), hours varchar(10), days_of_month varchar(10), month varchar(10), day_of_week varchar(10), yearToSchedule varchar(10), startTime varchar(50), endTime varchar(50), last_execution_status varchar(255), PRIMARY KEY(schedule_id));
GRANT ALL PRIVILEGES ON TABLE ambari.requestschedule TO :username;

CREATE TABLE ambari.requestschedulebatchrequest (schedule_id bigint, batch_id bigint, request_id bigint, request_type varchar(255), request_uri varchar(1024), request_body BYTEA, request_status varchar(255), return_code smallint, return_message varchar(20000), PRIMARY KEY(schedule_id, batch_id));
GRANT ALL PRIVILEGES ON TABLE ambari.requestschedulebatchrequest TO :username;

CREATE TABLE ambari.blueprint (blueprint_name VARCHAR(255) NOT NULL, stack_name VARCHAR(255) NOT NULL, stack_version VARCHAR(255) NOT NULL, PRIMARY KEY(blueprint_name));
CREATE TABLE ambari.hostgroup (blueprint_name VARCHAR(255) NOT NULL, name VARCHAR(255) NOT NULL, cardinality VARCHAR(255) NOT NULL, PRIMARY KEY(blueprint_name, name));
CREATE TABLE ambari.hostgroup_component (blueprint_name VARCHAR(255) NOT NULL, hostgroup_name VARCHAR(255) NOT NULL, name VARCHAR(255) NOT NULL, PRIMARY KEY(blueprint_name, hostgroup_name, name));
CREATE TABLE ambari.blueprint_configuration (blueprint_name varchar(255) NOT NULL, type_name varchar(255) NOT NULL, config_data varchar(32000) NOT NULL , PRIMARY KEY(blueprint_name, type_name));
GRANT ALL PRIVILEGES ON TABLE ambari.blueprint TO :username;
GRANT ALL PRIVILEGES ON TABLE ambari.hostgroup TO :username;
GRANT ALL PRIVILEGES ON TABLE ambari.hostgroup_component TO :username;
GRANT ALL PRIVILEGES ON TABLE ambari.blueprint_configuration TO :username;

CREATE TABLE ambari.viewmain (view_name VARCHAR(255) NOT NULL, label VARCHAR(255), version VARCHAR(255), archive VARCHAR(255), PRIMARY KEY(view_name));
CREATE TABLE ambari.viewinstancedata (view_name VARCHAR(255) NOT NULL, view_instance_name VARCHAR(255) NOT NULL, name VARCHAR(255) NOT NULL, value VARCHAR(2000) NOT NULL, PRIMARY KEY(view_name, view_instance_name, name));
CREATE TABLE ambari.viewinstance (view_name VARCHAR(255) NOT NULL, name VARCHAR(255) NOT NULL, PRIMARY KEY(view_name, name));
CREATE TABLE ambari.viewinstanceproperty (view_name VARCHAR(255) NOT NULL, view_instance_name VARCHAR(255) NOT NULL, name VARCHAR(255) NOT NULL, value VARCHAR(2000) NOT NULL, PRIMARY KEY(view_name, view_instance_name, name));
CREATE TABLE ambari.viewparameter (view_name VARCHAR(255) NOT NULL, name VARCHAR(255) NOT NULL, description VARCHAR(255), required CHAR(1), PRIMARY KEY(view_name, name));
CREATE TABLE ambari.viewresource (view_name VARCHAR(255) NOT NULL, name VARCHAR(255) NOT NULL, plural_name VARCHAR(255), id_property VARCHAR(255), subResource_names VARCHAR(255), provider VARCHAR(255), service VARCHAR(255), resource VARCHAR(255), PRIMARY KEY(view_name, name));
GRANT ALL PRIVILEGES ON TABLE ambari.viewmain TO :username;
GRANT ALL PRIVILEGES ON TABLE ambari.viewinstancedata TO :username;
GRANT ALL PRIVILEGES ON TABLE ambari.viewinstance TO :username;
GRANT ALL PRIVILEGES ON TABLE ambari.viewinstanceproperty TO :username;
GRANT ALL PRIVILEGES ON TABLE ambari.viewparameter TO :username;
GRANT ALL PRIVILEGES ON TABLE ambari.viewresource TO :username;

--------altering tables by creating foreign keys----------
ALTER TABLE ambari.clusterconfig ADD CONSTRAINT FK_clusterconfig_cluster_id FOREIGN KEY (cluster_id) REFERENCES ambari.clusters (cluster_id);
ALTER TABLE ambari.clusterservices ADD CONSTRAINT FK_clusterservices_cluster_id FOREIGN KEY (cluster_id) REFERENCES ambari.clusters (cluster_id);
ALTER TABLE ambari.clusterconfigmapping ADD CONSTRAINT clusterconfigmappingcluster_id FOREIGN KEY (cluster_id) REFERENCES ambari.clusters (cluster_id);
ALTER TABLE ambari.clusterstate ADD CONSTRAINT FK_clusterstate_cluster_id FOREIGN KEY (cluster_id) REFERENCES ambari.clusters (cluster_id);
ALTER TABLE ambari.hostcomponentdesiredstate ADD CONSTRAINT hstcmponentdesiredstatehstname FOREIGN KEY (host_name) REFERENCES ambari.hosts (host_name);
ALTER TABLE ambari.hostcomponentdesiredstate ADD CONSTRAINT hstcmpnntdesiredstatecmpnntnme FOREIGN KEY (component_name, cluster_id, service_name) REFERENCES ambari.servicecomponentdesiredstate (component_name, cluster_id, service_name);
ALTER TABLE ambari.hostcomponentstate ADD CONSTRAINT hstcomponentstatecomponentname FOREIGN KEY (component_name, cluster_id, service_name) REFERENCES ambari.servicecomponentdesiredstate (component_name, cluster_id, service_name);
ALTER TABLE ambari.hostcomponentstate ADD CONSTRAINT hostcomponentstate_host_name FOREIGN KEY (host_name) REFERENCES ambari.hosts (host_name);
ALTER TABLE ambari.hoststate ADD CONSTRAINT FK_hoststate_host_name FOREIGN KEY (host_name) REFERENCES ambari.hosts (host_name);
ALTER TABLE ambari.servicecomponentdesiredstate ADD CONSTRAINT srvccmponentdesiredstatesrvcnm FOREIGN KEY (service_name, cluster_id) REFERENCES ambari.clusterservices (service_name, cluster_id);
ALTER TABLE ambari.servicedesiredstate ADD CONSTRAINT servicedesiredstateservicename FOREIGN KEY (service_name, cluster_id) REFERENCES ambari.clusterservices (service_name, cluster_id);
ALTER TABLE ambari.execution_command ADD CONSTRAINT FK_execution_command_task_id FOREIGN KEY (task_id) REFERENCES ambari.host_role_command (task_id);
ALTER TABLE ambari.host_role_command ADD CONSTRAINT FK_host_role_command_stage_id FOREIGN KEY (stage_id, request_id) REFERENCES ambari.stage (stage_id, request_id);
ALTER TABLE ambari.host_role_command ADD CONSTRAINT FK_host_role_command_host_name FOREIGN KEY (host_name) REFERENCES ambari.hosts (host_name);
ALTER TABLE ambari.role_success_criteria ADD CONSTRAINT role_success_criteria_stage_id FOREIGN KEY (stage_id, request_id) REFERENCES ambari.stage (stage_id, request_id);
ALTER TABLE ambari.stage ADD CONSTRAINT FK_stage_cluster_id FOREIGN KEY (cluster_id) REFERENCES ambari.clusters (cluster_id);
ALTER TABLE ambari.stage ADD CONSTRAINT FK_stage_request_id FOREIGN KEY (request_id) REFERENCES ambari.request (request_id);
ALTER TABLE ambari.request ADD CONSTRAINT FK_request_cluster_id FOREIGN KEY (cluster_id) REFERENCES ambari.clusters (cluster_id);
ALTER TABLE ambari.request ADD CONSTRAINT FK_request_schedule_id FOREIGN KEY (request_schedule_id) REFERENCES ambari.requestschedule (schedule_id);
ALTER TABLE ambari.ClusterHostMapping ADD CONSTRAINT ClusterHostMapping_cluster_id FOREIGN KEY (host_name) REFERENCES ambari.hosts (host_name);
ALTER TABLE ambari.ClusterHostMapping ADD CONSTRAINT ClusterHostMapping_host_name FOREIGN KEY (cluster_id) REFERENCES ambari.clusters (cluster_id);
ALTER TABLE ambari.user_roles ADD CONSTRAINT FK_user_roles_user_id FOREIGN KEY (user_id) REFERENCES ambari.users (user_id);
ALTER TABLE ambari.user_roles ADD CONSTRAINT FK_user_roles_role_name FOREIGN KEY (role_name) REFERENCES ambari.roles (role_name);
ALTER TABLE ambari.hostconfigmapping ADD CONSTRAINT FK_hostconfmapping_cluster_id FOREIGN KEY (cluster_id) REFERENCES ambari.clusters (cluster_id);
ALTER TABLE ambari.hostconfigmapping ADD CONSTRAINT FK_hostconfmapping_host_name FOREIGN KEY (host_name) REFERENCES ambari.hosts (host_name);
ALTER TABLE ambari.configgroup ADD CONSTRAINT FK_configgroup_cluster_id FOREIGN KEY (cluster_id) REFERENCES ambari.clusters (cluster_id);
ALTER TABLE ambari.confgroupclusterconfigmapping ADD CONSTRAINT FK_confg FOREIGN KEY (version_tag, config_type, cluster_id) REFERENCES ambari.clusterconfig (version_tag, type_name, cluster_id);
ALTER TABLE ambari.confgroupclusterconfigmapping ADD CONSTRAINT FK_cgccm_gid FOREIGN KEY (config_group_id) REFERENCES ambari.configgroup (group_id);
ALTER TABLE ambari.configgrouphostmapping ADD CONSTRAINT FK_cghm_cgid FOREIGN KEY (config_group_id) REFERENCES ambari.configgroup (group_id);
ALTER TABLE ambari.configgrouphostmapping ADD CONSTRAINT FK_cghm_hname FOREIGN KEY (host_name) REFERENCES ambari.hosts (host_name);
ALTER TABLE ambari.requestschedulebatchrequest ADD CONSTRAINT FK_rsbatchrequest_schedule_id FOREIGN KEY (schedule_id) REFERENCES ambari.requestschedule (schedule_id);
ALTER TABLE ambari.hostgroup ADD CONSTRAINT FK_hg_blueprint_name FOREIGN KEY (blueprint_name) REFERENCES ambari.blueprint(blueprint_name);
ALTER TABLE ambari.hostgroup_component ADD CONSTRAINT FK_hgc_blueprint_name FOREIGN KEY (blueprint_name, hostgroup_name) REFERENCES ambari.hostgroup (blueprint_name, name);
ALTER TABLE ambari.blueprint_configuration ADD CONSTRAINT FK_cfg_blueprint_name FOREIGN KEY (blueprint_name) REFERENCES ambari.blueprint(blueprint_name);
ALTER TABLE ambari.requestresourcefilter ADD CONSTRAINT FK_reqresfilter_req_id FOREIGN KEY (request_id) REFERENCES ambari.request (request_id);
ALTER TABLE viewparameter ADD CONSTRAINT FK_viewparam_view_name FOREIGN KEY (view_name) REFERENCES viewmain(view_name);
ALTER TABLE viewresource ADD CONSTRAINT FK_viewres_view_name FOREIGN KEY (view_name) REFERENCES viewmain(view_name);
ALTER TABLE viewinstance ADD CONSTRAINT FK_viewinst_view_name FOREIGN KEY (view_name) REFERENCES viewmain(view_name);
ALTER TABLE viewinstanceproperty ADD CONSTRAINT FK_viewinstprop_view_name FOREIGN KEY (view_name, view_instance_name) REFERENCES viewinstance(view_name, name);
ALTER TABLE viewinstancedata ADD CONSTRAINT FK_viewinstdata_view_name FOREIGN KEY (view_name, view_instance_name) REFERENCES viewinstance(view_name, name);


---------inserting some data-----------
BEGIN;
  INSERT INTO ambari.ambari_sequences (sequence_name, "value")
  SELECT 'cluster_id_seq', 1
  UNION ALL
  SELECT 'user_id_seq', 2
  UNION ALL
  SELECT 'host_role_command_id_seq', 1
  union all
  select 'configgroup_id_seq', 1
  union all
  select 'requestschedule_id_seq', 1
  union all
  select 'resourcefilter_id_seq', 1;

  INSERT INTO ambari.Roles (role_name)
  SELECT 'admin'
  UNION ALL
  SELECT 'user';

  INSERT INTO ambari.Users (user_id, user_name, user_password)
  SELECT 1, 'admin', '538916f8943ec225d97a9a86a2c6ec0818c1cd400e09e03b660fdaaec4af29ddbb6f2b1033b81b00';

  INSERT INTO ambari.user_roles (role_name, user_id)
  SELECT 'admin', 1;

  INSERT INTO ambari.metainfo (metainfo_key, metainfo_value)
  SELECT 'version', '${ambariVersion}';
COMMIT;

-- Quartz tables

CREATE TABLE ambari.qrtz_job_details
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
GRANT ALL PRIVILEGES ON TABLE ambari.qrtz_job_details TO :username;

CREATE TABLE ambari.qrtz_triggers
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
	REFERENCES ambari.QRTZ_JOB_DETAILS(SCHED_NAME,JOB_NAME,JOB_GROUP)
);
GRANT ALL PRIVILEGES ON TABLE ambari.qrtz_triggers TO :username;

CREATE TABLE ambari.qrtz_simple_triggers
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    REPEAT_COUNT BIGINT NOT NULL,
    REPEAT_INTERVAL BIGINT NOT NULL,
    TIMES_TRIGGERED BIGINT NOT NULL,
    PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
	REFERENCES ambari.QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
);
GRANT ALL PRIVILEGES ON TABLE ambari.qrtz_simple_triggers TO :username;

CREATE TABLE ambari.qrtz_cron_triggers
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    CRON_EXPRESSION VARCHAR(120) NOT NULL,
    TIME_ZONE_ID VARCHAR(80),
    PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
	REFERENCES ambari.QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
);
GRANT ALL PRIVILEGES ON TABLE ambari.qrtz_cron_triggers TO :username;

CREATE TABLE ambari.qrtz_simprop_triggers
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
    REFERENCES ambari.QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
);
GRANT ALL PRIVILEGES ON TABLE ambari.qrtz_simprop_triggers TO :username;

CREATE TABLE ambari.qrtz_blob_triggers
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_NAME VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    BLOB_DATA BYTEA NULL,
    PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
        REFERENCES ambari.QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
);
GRANT ALL PRIVILEGES ON TABLE ambari.qrtz_blob_triggers TO :username;

CREATE TABLE ambari.qrtz_calendars
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    CALENDAR_NAME  VARCHAR(200) NOT NULL,
    CALENDAR BYTEA NOT NULL,
    PRIMARY KEY (SCHED_NAME,CALENDAR_NAME)
);
GRANT ALL PRIVILEGES ON TABLE ambari.qrtz_calendars TO :username;


CREATE TABLE ambari.qrtz_paused_trigger_grps
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    TRIGGER_GROUP  VARCHAR(200) NOT NULL,
    PRIMARY KEY (SCHED_NAME,TRIGGER_GROUP)
);
GRANT ALL PRIVILEGES ON TABLE ambari.qrtz_paused_trigger_grps TO :username;

CREATE TABLE ambari.qrtz_fired_triggers
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
GRANT ALL PRIVILEGES ON TABLE ambari.qrtz_fired_triggers TO :username;

CREATE TABLE ambari.qrtz_scheduler_state
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    INSTANCE_NAME VARCHAR(200) NOT NULL,
    LAST_CHECKIN_TIME BIGINT NOT NULL,
    CHECKIN_INTERVAL BIGINT NOT NULL,
    PRIMARY KEY (SCHED_NAME,INSTANCE_NAME)
);
GRANT ALL PRIVILEGES ON TABLE ambari.qrtz_scheduler_state TO :username;

CREATE TABLE ambari.qrtz_locks
  (
    SCHED_NAME VARCHAR(120) NOT NULL,
    LOCK_NAME  VARCHAR(40) NOT NULL,
    PRIMARY KEY (SCHED_NAME,LOCK_NAME)
);
GRANT ALL PRIVILEGES ON TABLE ambari.qrtz_locks TO :username;

create index idx_qrtz_j_req_recovery on ambari.qrtz_job_details(SCHED_NAME,REQUESTS_RECOVERY);
create index idx_qrtz_j_grp on ambari.qrtz_job_details(SCHED_NAME,JOB_GROUP);

create index idx_qrtz_t_j on ambari.qrtz_triggers(SCHED_NAME,JOB_NAME,JOB_GROUP);
create index idx_qrtz_t_jg on ambari.qrtz_triggers(SCHED_NAME,JOB_GROUP);
create index idx_qrtz_t_c on ambari.qrtz_triggers(SCHED_NAME,CALENDAR_NAME);
create index idx_qrtz_t_g on ambari.qrtz_triggers(SCHED_NAME,TRIGGER_GROUP);
create index idx_qrtz_t_state on ambari.qrtz_triggers(SCHED_NAME,TRIGGER_STATE);
create index idx_qrtz_t_n_state on ambari.qrtz_triggers(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP,TRIGGER_STATE);
create index idx_qrtz_t_n_g_state on ambari.qrtz_triggers(SCHED_NAME,TRIGGER_GROUP,TRIGGER_STATE);
create index idx_qrtz_t_next_fire_time on ambari.qrtz_triggers(SCHED_NAME,NEXT_FIRE_TIME);
create index idx_qrtz_t_nft_st on ambari.qrtz_triggers(SCHED_NAME,TRIGGER_STATE,NEXT_FIRE_TIME);
create index idx_qrtz_t_nft_misfire on ambari.qrtz_triggers(SCHED_NAME,MISFIRE_INSTR,NEXT_FIRE_TIME);
create index idx_qrtz_t_nft_st_misfire on ambari.qrtz_triggers(SCHED_NAME,MISFIRE_INSTR,NEXT_FIRE_TIME,TRIGGER_STATE);
create index idx_qrtz_t_nft_st_misfire_grp on ambari.qrtz_triggers(SCHED_NAME,MISFIRE_INSTR,NEXT_FIRE_TIME,TRIGGER_GROUP,TRIGGER_STATE);

create index idx_qrtz_ft_trig_inst_name on ambari.qrtz_fired_triggers(SCHED_NAME,INSTANCE_NAME);
create index idx_qrtz_ft_inst_job_req_rcvry on ambari.qrtz_fired_triggers(SCHED_NAME,INSTANCE_NAME,REQUESTS_RECOVERY);
create index idx_qrtz_ft_j_g on ambari.qrtz_fired_triggers(SCHED_NAME,JOB_NAME,JOB_GROUP);
create index idx_qrtz_ft_jg on ambari.qrtz_fired_triggers(SCHED_NAME,JOB_GROUP);
create index idx_qrtz_ft_t_g on ambari.qrtz_fired_triggers(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP);
create index idx_qrtz_ft_tg on ambari.qrtz_fired_triggers(SCHED_NAME,TRIGGER_GROUP);

-- ambari log4j DDL

--------------------------------------------------
----------initialisation of mapred db-------------
--------------------------------------------------
CREATE DATABASE ambarirca;
\connect ambarirca;

--CREATE ROLE "mapred" LOGIN ENCRYPTED PASSWORD 'mapred';
CREATE USER "mapred" WITH PASSWORD 'mapred';
GRANT ALL PRIVILEGES ON DATABASE ambarirca TO "mapred";

------create tables ang grant privileges to db user---------
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
GRANT ALL PRIVILEGES ON TABLE workflow TO "mapred";

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
GRANT ALL PRIVILEGES ON TABLE job TO "mapred";

CREATE TABLE task (
  taskId        TEXT, jobId TEXT, taskType TEXT, splits TEXT,
  startTime     BIGINT, finishTime BIGINT, status TEXT, error TEXT, counters TEXT,
  failedAttempt TEXT,
  PRIMARY KEY (taskId),
  FOREIGN KEY (jobId) REFERENCES job (jobId) ON DELETE CASCADE
);
GRANT ALL PRIVILEGES ON TABLE task TO "mapred";

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
GRANT ALL PRIVILEGES ON TABLE taskAttempt TO "mapred";

CREATE TABLE hdfsEvent (
  timestamp   BIGINT,
  userName    TEXT,
  clientIP    TEXT,
  operation   TEXT,
  srcPath     TEXT,
  dstPath     TEXT,
  permissions TEXT
);
GRANT ALL PRIVILEGES ON TABLE hdfsEvent TO "mapred";

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
GRANT ALL PRIVILEGES ON TABLE mapreduceEvent TO "mapred";

CREATE TABLE clusterEvent (
  timestamp BIGINT,
  service   TEXT, status TEXT,
  error     TEXT, data TEXT,
  host      TEXT, rack TEXT
);
GRANT ALL PRIVILEGES ON TABLE clusterEvent TO "mapred";

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

CREATE TABLE clusters (cluster_id NUMBER(19) NOT NULL, cluster_info VARCHAR2(255) NULL, cluster_name VARCHAR2(100) NOT NULL UNIQUE, desired_cluster_state VARCHAR2(255) NULL, desired_stack_version VARCHAR2(255) NULL, PRIMARY KEY (cluster_id));
CREATE TABLE clusterconfig (version_tag VARCHAR2(255) NOT NULL, type_name VARCHAR2(255) NOT NULL, cluster_id NUMBER(19) NOT NULL, config_data CLOB NOT NULL, create_timestamp NUMBER(19) NOT NULL, PRIMARY KEY (version_tag, type_name, cluster_id));
CREATE TABLE clusterservices (service_name VARCHAR2(255) NOT NULL, cluster_id NUMBER(19) NOT NULL, service_enabled NUMBER(10) NOT NULL, PRIMARY KEY (service_name, cluster_id));
CREATE TABLE clusterstate (cluster_id NUMBER(19) NOT NULL, current_cluster_state VARCHAR2(255) NULL, current_stack_version VARCHAR2(255) NULL, PRIMARY KEY (cluster_id));
CREATE TABLE hostcomponentdesiredstate (cluster_id NUMBER(19) NOT NULL, component_name VARCHAR2(255) NOT NULL, desired_stack_version VARCHAR2(255) NULL, desired_state VARCHAR2(255) NOT NULL, host_name VARCHAR2(255) NOT NULL, service_name VARCHAR2(255) NOT NULL, admin_state VARCHAR2(32) NULL, maintenance_state VARCHAR2(32) NOT NULL, PRIMARY KEY (cluster_id, component_name, host_name, service_name));
CREATE TABLE hostcomponentstate (cluster_id NUMBER(19) NOT NULL, component_name VARCHAR2(255) NOT NULL, current_stack_version VARCHAR2(255) NOT NULL, current_state VARCHAR2(255) NOT NULL, host_name VARCHAR2(255) NOT NULL, service_name VARCHAR2(255) NOT NULL, PRIMARY KEY (cluster_id, component_name, host_name, service_name));
CREATE TABLE hosts (host_name VARCHAR2(255) NOT NULL, cpu_count INTEGER NOT NULL, cpu_info VARCHAR2(255) NULL, discovery_status VARCHAR2(2000) NULL, host_attributes CLOB NULL, ipv4 VARCHAR2(255) NULL, ipv6 VARCHAR2(255) NULL, last_registration_time INTEGER NOT NULL, os_arch VARCHAR2(255) NULL, os_info VARCHAR2(1000) NULL, os_type VARCHAR2(255) NULL, ph_cpu_count INTEGER NOT NULL, public_host_name VARCHAR2(255) NULL, rack_info VARCHAR2(255) NOT NULL, total_mem INTEGER NOT NULL, PRIMARY KEY (host_name));
CREATE TABLE hoststate (agent_version VARCHAR2(255) NULL, available_mem NUMBER(19) NOT NULL, current_state VARCHAR2(255) NOT NULL, health_status VARCHAR2(255) NULL, host_name VARCHAR2(255) NOT NULL, time_in_state NUMBER(19) NOT NULL, maintenance_state VARCHAR2(512), PRIMARY KEY (host_name));
CREATE TABLE servicecomponentdesiredstate (component_name VARCHAR2(255) NOT NULL, cluster_id NUMBER(19) NOT NULL, desired_stack_version VARCHAR2(255) NULL, desired_state VARCHAR2(255) NOT NULL, service_name VARCHAR2(255) NOT NULL, PRIMARY KEY (component_name, cluster_id, service_name));
CREATE TABLE servicedesiredstate (cluster_id NUMBER(19) NOT NULL, desired_host_role_mapping NUMBER(10) NOT NULL, desired_stack_version VARCHAR2(255) NULL, desired_state VARCHAR2(255) NOT NULL, service_name VARCHAR2(255) NOT NULL, maintenance_state VARCHAR2(32) NOT NULL, PRIMARY KEY (cluster_id, service_name));
CREATE TABLE roles (role_name VARCHAR2(255) NOT NULL, PRIMARY KEY (role_name));
CREATE TABLE users (user_id NUMBER(10) NOT NULL, create_time TIMESTAMP NULL, ldap_user NUMBER(10) DEFAULT 0, user_name VARCHAR2(255) NULL, user_password VARCHAR2(255) NULL, PRIMARY KEY (user_id));
CREATE TABLE execution_command (task_id NUMBER(19) NOT NULL, command BLOB NULL, PRIMARY KEY (task_id));
CREATE TABLE host_role_command (task_id NUMBER(19) NOT NULL, attempt_count NUMBER(5) NOT NULL, event CLOB NULL, exitcode NUMBER(10) NOT NULL, host_name VARCHAR2(255) NOT NULL, last_attempt_time NUMBER(19) NOT NULL, request_id NUMBER(19) NOT NULL, role VARCHAR2(255) NULL, role_command VARCHAR2(255) NULL, stage_id NUMBER(19) NOT NULL, start_time NUMBER(19) NOT NULL, end_time NUMBER(19), status VARCHAR2(255) NULL, std_error BLOB NULL, std_out BLOB NULL, structured_out BLOB NULL,  command_detail VARCHAR2(255) NULL, custom_command_name VARCHAR2(255) NULL, PRIMARY KEY (task_id));
CREATE TABLE role_success_criteria (role VARCHAR2(255) NOT NULL, request_id NUMBER(19) NOT NULL, stage_id NUMBER(19) NOT NULL, success_factor NUMBER(19,4) NOT NULL, PRIMARY KEY (role, request_id, stage_id));
CREATE TABLE stage (stage_id NUMBER(19) NOT NULL, request_id NUMBER(19) NOT NULL, cluster_id NUMBER(19) NULL, log_info VARCHAR2(255) NULL, request_context VARCHAR2(255) NULL, cluster_host_info BLOB NOT NULL, PRIMARY KEY (stage_id, request_id));
CREATE TABLE request (request_id NUMBER(19) NOT NULL, cluster_id NUMBER(19), request_schedule_id NUMBER(19), command_name VARCHAR(255), create_time NUMBER(19) NOT NULL, end_time NUMBER(19) NOT NULL, inputs BLOB, request_context VARCHAR(255), request_type VARCHAR(255), start_time NUMBER(19) NOT NULL, status VARCHAR(255), PRIMARY KEY (request_id));
CREATE TABLE requestresourcefilter (filter_id NUMBER(19) NOT NULL, request_id NUMBER(19) NOT NULL, service_name VARCHAR2(255), component_name VARCHAR2(255), hosts BLOB, PRIMARY KEY (filter_id));
CREATE TABLE key_value_store ("key" VARCHAR2(255) NOT NULL, "value" CLOB NULL, PRIMARY KEY ("key"));
CREATE TABLE clusterconfigmapping (type_name VARCHAR2(255) NOT NULL, create_timestamp NUMBER(19) NOT NULL, cluster_id NUMBER(19) NOT NULL, selected NUMBER(10) NOT NULL, version_tag VARCHAR2(255) NOT NULL, user_name VARCHAR(255) DEFAULT '_db', PRIMARY KEY (type_name, create_timestamp, cluster_id));
CREATE TABLE hostconfigmapping (create_timestamp NUMBER(19) NOT NULL, host_name VARCHAR2(255) NOT NULL, cluster_id NUMBER(19) NOT NULL, type_name VARCHAR2(255) NOT NULL, selected NUMBER(10) NOT NULL, service_name VARCHAR2(255) NULL, version_tag VARCHAR2(255) NOT NULL, user_name VARCHAR(255) DEFAULT '_db', PRIMARY KEY (create_timestamp, host_name, cluster_id, type_name));
CREATE TABLE metainfo ("metainfo_key" VARCHAR2(255) NOT NULL, "metainfo_value" CLOB NULL, PRIMARY KEY ("metainfo_key"));
CREATE TABLE ClusterHostMapping (cluster_id NUMBER(19) NOT NULL, host_name VARCHAR2(255) NOT NULL, PRIMARY KEY (cluster_id, host_name));
CREATE TABLE user_roles (role_name VARCHAR2(255) NOT NULL, user_id NUMBER(10) NOT NULL, PRIMARY KEY (role_name, user_id));
CREATE TABLE ambari_sequences (sequence_name VARCHAR2(50) NOT NULL, value NUMBER(38) NULL, PRIMARY KEY (sequence_name));
CREATE TABLE configgroup (group_id NUMBER(19), cluster_id NUMBER(19) NOT NULL, group_name VARCHAR2(255) NOT NULL, tag VARCHAR2(1024) NOT NULL, description VARCHAR2(1024), create_timestamp NUMBER(19) NOT NULL, PRIMARY KEY(group_id));
CREATE TABLE confgroupclusterconfigmapping (config_group_id NUMBER(19) NOT NULL, cluster_id NUMBER(19) NOT NULL, config_type VARCHAR2(255) NOT NULL, version_tag VARCHAR2(255) NOT NULL, user_name VARCHAR2(255) DEFAULT '_db', create_timestamp NUMBER(19) NOT NULL, PRIMARY KEY(config_group_id, cluster_id, config_type));
CREATE TABLE configgrouphostmapping (config_group_id NUMBER(19) NOT NULL, host_name VARCHAR2(255) NOT NULL, PRIMARY KEY(config_group_id, host_name));
CREATE TABLE requestschedule (schedule_id NUMBER(19), cluster_id NUMBER(19) NOT NULL, description VARCHAR2(255), status VARCHAR2(255), batch_separation_seconds smallint, batch_toleration_limit smallint, create_user VARCHAR2(255), create_timestamp NUMBER(19), update_user VARCHAR2(255), update_timestamp NUMBER(19), minutes VARCHAR2(10), hours VARCHAR2(10), days_of_month VARCHAR2(10), month VARCHAR2(10), day_of_week VARCHAR2(10), yearToSchedule VARCHAR2(10), startTime VARCHAR2(50), endTime VARCHAR2(50), last_execution_status VARCHAR2(255), PRIMARY KEY(schedule_id));
CREATE TABLE requestschedulebatchrequest (schedule_id NUMBER(19), batch_id NUMBER(19), request_id NUMBER(19), request_type VARCHAR2(255), request_uri VARCHAR2(1024), request_body BLOB, request_status VARCHAR2(255), return_code smallint, return_message VARCHAR2(2000), PRIMARY KEY(schedule_id, batch_id));
CREATE TABLE blueprint (blueprint_name VARCHAR2(255) NOT NULL, stack_name VARCHAR2(255) NOT NULL, stack_version VARCHAR2(255) NOT NULL, PRIMARY KEY(blueprint_name));
CREATE TABLE hostgroup (blueprint_name VARCHAR2(255) NOT NULL, name VARCHAR2(255) NOT NULL, cardinality VARCHAR2(255) NOT NULL, PRIMARY KEY(blueprint_name, name));
CREATE TABLE hostgroup_component (blueprint_name VARCHAR2(255) NOT NULL, hostgroup_name VARCHAR2(255) NOT NULL, name VARCHAR2(255) NOT NULL, PRIMARY KEY(blueprint_name, hostgroup_name, name));
CREATE TABLE blueprint_configuration (blueprint_name VARCHAR2(255) NOT NULL, type_name VARCHAR2(255) NOT NULL, config_data CLOB NOT NULL , PRIMARY KEY(blueprint_name, type_name));
CREATE TABLE viewmain (view_name VARCHAR(255) NOT NULL, label VARCHAR(255) NOT NULL, version VARCHAR(255), archive VARCHAR(255), PRIMARY KEY(view_name));
CREATE TABLE viewinstancedata (view_name VARCHAR(255) NOT NULL, view_instance_name VARCHAR(255) NOT NULL, name VARCHAR(255) NOT NULL, value VARCHAR(255) NOT NULL, PRIMARY KEY(view_name, view_instance_name, name));
CREATE TABLE viewinstance (view_name VARCHAR(255) NOT NULL, name VARCHAR(255) NOT NULL, PRIMARY KEY(view_name, name));
CREATE TABLE viewinstanceproperty (view_name VARCHAR(255) NOT NULL, view_instance_name VARCHAR(255) NOT NULL, name VARCHAR(255) NOT NULL, value VARCHAR(255) NOT NULL, PRIMARY KEY(view_name, view_instance_name, name));
CREATE TABLE viewparameter (view_name VARCHAR(255) NOT NULL, name VARCHAR(255) NOT NULL, description VARCHAR(255), required CHAR(1), PRIMARY KEY(view_name, name));
CREATE TABLE viewresource (view_name VARCHAR(255) NOT NULL, name VARCHAR(255) NOT NULL, plural_name VARCHAR(255), id_property VARCHAR(255), subResource_names VARCHAR(255), provider VARCHAR(255), service VARCHAR(255), resource VARCHAR(255), PRIMARY KEY(view_name, name));

ALTER TABLE users ADD CONSTRAINT UNQ_users_0 UNIQUE (user_name, ldap_user);
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
ALTER TABLE stage ADD CONSTRAINT FK_stage_cluster_id FOREIGN KEY (cluster_id) REFERENCES clusters (cluster_id);
ALTER TABLE stage ADD CONSTRAINT FK_stage_request_id FOREIGN KEY (request_id) REFERENCES request (request_id);
ALTER TABLE request ADD CONSTRAINT FK_request_cluster_id FOREIGN KEY (cluster_id) REFERENCES clusters (cluster_id);
ALTER TABLE request ADD CONSTRAINT FK_request_schedule_id FOREIGN KEY (request_schedule_id) REFERENCES requestschedule (schedule_id);
ALTER TABLE ClusterHostMapping ADD CONSTRAINT ClusterHostMapping_cluster_id FOREIGN KEY (cluster_id) REFERENCES clusters (cluster_id);
ALTER TABLE ClusterHostMapping ADD CONSTRAINT ClusterHostMapping_host_name FOREIGN KEY (host_name) REFERENCES hosts (host_name);
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
ALTER TABLE hostgroup_component ADD CONSTRAINT FK_hgc_blueprint_name FOREIGN KEY (blueprint_name, hostgroup_name) REFERENCES hostgroup(blueprint_name, name);
ALTER TABLE blueprint_configuration ADD CONSTRAINT FK_cfg_blueprint_name FOREIGN KEY (blueprint_name) REFERENCES blueprint(blueprint_name);
ALTER TABLE requestresourcefilter ADD CONSTRAINT FK_reqresfilter_req_id FOREIGN KEY (request_id) REFERENCES request (request_id);
ALTER TABLE viewparameter ADD CONSTRAINT FK_viewparameter_view_name FOREIGN KEY (view_name) REFERENCES viewmain(view_name);
ALTER TABLE viewresource ADD CONSTRAINT FK_viewresource_view_name FOREIGN KEY (view_name) REFERENCES viewmain(view_name);
ALTER TABLE viewinstance ADD CONSTRAINT FK_viewinstance_view_name FOREIGN KEY (view_name) REFERENCES viewmain(view_name);
ALTER TABLE viewinstanceproperty ADD CONSTRAINT FK_viewinstanceproperty_view_name FOREIGN KEY (view_name, view_instance_name) REFERENCES viewinstance(view_name, name);
ALTER TABLE viewinstancedata ADD CONSTRAINT FK_viewinstancedata_view_name FOREIGN KEY (view_name, view_instance_name) REFERENCES viewinstance(view_name, name);

INSERT INTO ambari_sequences(sequence_name, value) values ('host_role_command_id_seq', 0);
INSERT INTO ambari_sequences(sequence_name, value) values ('user_id_seq', 1);
INSERT INTO ambari_sequences(sequence_name, value) values ('cluster_id_seq', 0);
INSERT INTO ambari_sequences(sequence_name, value) values ('configgroup_id_seq', 1);
INSERT INTO ambari_sequences(sequence_name, value) values ('requestschedule_id_seq', 1);
INSERT INTO ambari_sequences(sequence_name, value) values ('resourcefilter_id_seq', 1);
INSERT INTO metainfo("metainfo_key", "metainfo_value") values ('version', '${ambariVersion}');

insert into Roles(role_name)
select 'admin' from dual
union all
select 'user' from dual;

insert into Users(user_id, user_name, user_password)
select 1,'admin','538916f8943ec225d97a9a86a2c6ec0818c1cd400e09e03b660fdaaec4af29ddbb6f2b1033b81b00' from dual;

insert into user_roles(role_name, user_id)
select 'admin',1 from dual;



commit;

-- ambari rca

CREATE TABLE workflow (
  workflowId VARCHAR2(4000), workflowName VARCHAR2(4000),
  parentWorkflowId VARCHAR2(4000),  
  workflowContext VARCHAR2(4000), userName VARCHAR2(4000),
  startTime INTEGER, lastUpdateTime INTEGER,
  numJobsTotal INTEGER, numJobsCompleted INTEGER,
  inputBytes INTEGER, outputBytes INTEGER,
  duration INTEGER,
  PRIMARY KEY (workflowId),
  FOREIGN KEY (parentWorkflowId) REFERENCES workflow(workflowId) ON DELETE CASCADE
);

CREATE TABLE job (
  jobId VARCHAR2(4000), workflowId VARCHAR2(4000), jobName VARCHAR2(4000), workflowEntityName VARCHAR2(4000),
  userName VARCHAR2(4000), queue CLOB, acls CLOB, confPath CLOB, 
  submitTime INTEGER, launchTime INTEGER, finishTime INTEGER, 
  maps INTEGER, reduces INTEGER, status VARCHAR2(4000), priority VARCHAR2(4000), 
  finishedMaps INTEGER, finishedReduces INTEGER, 
  failedMaps INTEGER, failedReduces INTEGER, 
  mapsRuntime INTEGER, reducesRuntime INTEGER,
  mapCounters VARCHAR2(4000), reduceCounters VARCHAR2(4000), jobCounters VARCHAR2(4000), 
  inputBytes INTEGER, outputBytes INTEGER,
  PRIMARY KEY(jobId),
  FOREIGN KEY(workflowId) REFERENCES workflow(workflowId) ON DELETE CASCADE
);

CREATE TABLE task (
  taskId VARCHAR2(4000), jobId VARCHAR2(4000), taskType VARCHAR2(4000), splits VARCHAR2(4000), 
  startTime INTEGER, finishTime INTEGER, status VARCHAR2(4000), error CLOB, counters VARCHAR2(4000), 
  failedAttempt VARCHAR2(4000), 
  PRIMARY KEY(taskId), 
  FOREIGN KEY(jobId) REFERENCES job(jobId) ON DELETE CASCADE
);

CREATE TABLE taskAttempt (
  taskAttemptId VARCHAR2(4000), taskId VARCHAR2(4000), jobId VARCHAR2(4000), taskType VARCHAR2(4000), taskTracker VARCHAR2(4000), 
  startTime INTEGER, finishTime INTEGER, 
  mapFinishTime INTEGER, shuffleFinishTime INTEGER, sortFinishTime INTEGER, 
  locality VARCHAR2(4000), avataar VARCHAR2(4000), 
  status VARCHAR2(4000), error CLOB, counters VARCHAR2(4000), 
  inputBytes INTEGER, outputBytes INTEGER,
  PRIMARY KEY(taskAttemptId), 
  FOREIGN KEY(jobId) REFERENCES job(jobId) ON DELETE CASCADE,
  FOREIGN KEY(taskId) REFERENCES task(taskId) ON DELETE CASCADE
); 

CREATE TABLE hdfsEvent (
  timestamp INTEGER,
  userName VARCHAR2(4000),
  clientIP VARCHAR2(4000),
  operation VARCHAR2(4000),
  srcPath CLOB,
  dstPath CLOB,
  permissions VARCHAR2(4000)
);

CREATE TABLE mapreduceEvent (
  timestamp INTEGER,
  userName VARCHAR2(4000),
  clientIP VARCHAR2(4000),
  operation VARCHAR2(4000),
  target VARCHAR2(4000),
  result CLOB,
  description CLOB,
  permissions VARCHAR2(4000)
);

CREATE TABLE clusterEvent (
  timestamp INTEGER, 
  service VARCHAR2(4000), status VARCHAR2(4000), 
  error CLOB, data CLOB , 
  host VARCHAR2(4000), rack VARCHAR2(4000)
);

-- Quartz tables

delete from qrtz_fired_triggers;
delete from qrtz_simple_triggers;
delete from qrtz_simprop_triggers;
delete from qrtz_cron_triggers;
delete from qrtz_blob_triggers;
delete from qrtz_triggers;
delete from qrtz_job_details;
delete from qrtz_calendars;
delete from qrtz_paused_trigger_grps;
delete from qrtz_locks;
delete from qrtz_scheduler_state;

drop table qrtz_calendars;
drop table qrtz_fired_triggers;
drop table qrtz_blob_triggers;
drop table qrtz_cron_triggers;
drop table qrtz_simple_triggers;
drop table qrtz_simprop_triggers;
drop table qrtz_triggers;
drop table qrtz_job_details;
drop table qrtz_paused_trigger_grps;
drop table qrtz_locks;
drop table qrtz_scheduler_state;


CREATE TABLE qrtz_job_details
  (
    SCHED_NAME VARCHAR2(120) NOT NULL,
    JOB_NAME  VARCHAR2(200) NOT NULL,
    JOB_GROUP VARCHAR2(200) NOT NULL,
    DESCRIPTION VARCHAR2(250) NULL,
    JOB_CLASS_NAME   VARCHAR2(250) NOT NULL,
    IS_DURABLE VARCHAR2(1) NOT NULL,
    IS_NONCONCURRENT VARCHAR2(1) NOT NULL,
    IS_UPDATE_DATA VARCHAR2(1) NOT NULL,
    REQUESTS_RECOVERY VARCHAR2(1) NOT NULL,
    JOB_DATA BLOB NULL,
    CONSTRAINT QRTZ_JOB_DETAILS_PK PRIMARY KEY (SCHED_NAME,JOB_NAME,JOB_GROUP)
);
CREATE TABLE qrtz_triggers
  (
    SCHED_NAME VARCHAR2(120) NOT NULL,
    TRIGGER_NAME VARCHAR2(200) NOT NULL,
    TRIGGER_GROUP VARCHAR2(200) NOT NULL,
    JOB_NAME  VARCHAR2(200) NOT NULL,
    JOB_GROUP VARCHAR2(200) NOT NULL,
    DESCRIPTION VARCHAR2(250) NULL,
    NEXT_FIRE_TIME NUMBER(13) NULL,
    PREV_FIRE_TIME NUMBER(13) NULL,
    PRIORITY NUMBER(13) NULL,
    TRIGGER_STATE VARCHAR2(16) NOT NULL,
    TRIGGER_TYPE VARCHAR2(8) NOT NULL,
    START_TIME NUMBER(13) NOT NULL,
    END_TIME NUMBER(13) NULL,
    CALENDAR_NAME VARCHAR2(200) NULL,
    MISFIRE_INSTR NUMBER(2) NULL,
    JOB_DATA BLOB NULL,
    CONSTRAINT QRTZ_TRIGGERS_PK PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
    CONSTRAINT QRTZ_TRIGGER_TO_JOBS_FK FOREIGN KEY (SCHED_NAME,JOB_NAME,JOB_GROUP)
      REFERENCES QRTZ_JOB_DETAILS(SCHED_NAME,JOB_NAME,JOB_GROUP)
);
CREATE TABLE qrtz_simple_triggers
  (
    SCHED_NAME VARCHAR2(120) NOT NULL,
    TRIGGER_NAME VARCHAR2(200) NOT NULL,
    TRIGGER_GROUP VARCHAR2(200) NOT NULL,
    REPEAT_COUNT NUMBER(7) NOT NULL,
    REPEAT_INTERVAL NUMBER(12) NOT NULL,
    TIMES_TRIGGERED NUMBER(10) NOT NULL,
    CONSTRAINT QRTZ_SIMPLE_TRIG_PK PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
    CONSTRAINT QRTZ_SIMPLE_TRIG_TO_TRIG_FK FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
	REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
);
CREATE TABLE qrtz_cron_triggers
  (
    SCHED_NAME VARCHAR2(120) NOT NULL,
    TRIGGER_NAME VARCHAR2(200) NOT NULL,
    TRIGGER_GROUP VARCHAR2(200) NOT NULL,
    CRON_EXPRESSION VARCHAR2(120) NOT NULL,
    TIME_ZONE_ID VARCHAR2(80),
    CONSTRAINT QRTZ_CRON_TRIG_PK PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
    CONSTRAINT QRTZ_CRON_TRIG_TO_TRIG_FK FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
      REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
);
CREATE TABLE qrtz_simprop_triggers
  (
    SCHED_NAME VARCHAR2(120) NOT NULL,
    TRIGGER_NAME VARCHAR2(200) NOT NULL,
    TRIGGER_GROUP VARCHAR2(200) NOT NULL,
    STR_PROP_1 VARCHAR2(512) NULL,
    STR_PROP_2 VARCHAR2(512) NULL,
    STR_PROP_3 VARCHAR2(512) NULL,
    INT_PROP_1 NUMBER(10) NULL,
    INT_PROP_2 NUMBER(10) NULL,
    LONG_PROP_1 NUMBER(13) NULL,
    LONG_PROP_2 NUMBER(13) NULL,
    DEC_PROP_1 NUMERIC(13,4) NULL,
    DEC_PROP_2 NUMERIC(13,4) NULL,
    BOOL_PROP_1 VARCHAR2(1) NULL,
    BOOL_PROP_2 VARCHAR2(1) NULL,
    CONSTRAINT QRTZ_SIMPROP_TRIG_PK PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
    CONSTRAINT QRTZ_SIMPROP_TRIG_TO_TRIG_FK FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
      REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
);
CREATE TABLE qrtz_blob_triggers
  (
    SCHED_NAME VARCHAR2(120) NOT NULL,
    TRIGGER_NAME VARCHAR2(200) NOT NULL,
    TRIGGER_GROUP VARCHAR2(200) NOT NULL,
    BLOB_DATA BLOB NULL,
    CONSTRAINT QRTZ_BLOB_TRIG_PK PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP),
    CONSTRAINT QRTZ_BLOB_TRIG_TO_TRIG_FK FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
        REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
);
CREATE TABLE qrtz_calendars
  (
    SCHED_NAME VARCHAR2(120) NOT NULL,
    CALENDAR_NAME  VARCHAR2(200) NOT NULL,
    CALENDAR BLOB NOT NULL,
    CONSTRAINT QRTZ_CALENDARS_PK PRIMARY KEY (SCHED_NAME,CALENDAR_NAME)
);
CREATE TABLE qrtz_paused_trigger_grps
  (
    SCHED_NAME VARCHAR2(120) NOT NULL,
    TRIGGER_GROUP  VARCHAR2(200) NOT NULL,
    CONSTRAINT QRTZ_PAUSED_TRIG_GRPS_PK PRIMARY KEY (SCHED_NAME,TRIGGER_GROUP)
);
CREATE TABLE qrtz_fired_triggers
  (
    SCHED_NAME VARCHAR2(120) NOT NULL,
    ENTRY_ID VARCHAR2(95) NOT NULL,
    TRIGGER_NAME VARCHAR2(200) NOT NULL,
    TRIGGER_GROUP VARCHAR2(200) NOT NULL,
    INSTANCE_NAME VARCHAR2(200) NOT NULL,
    FIRED_TIME NUMBER(13) NOT NULL,
    SCHED_TIME NUMBER(13) NOT NULL,
    PRIORITY NUMBER(13) NOT NULL,
    STATE VARCHAR2(16) NOT NULL,
    JOB_NAME VARCHAR2(200) NULL,
    JOB_GROUP VARCHAR2(200) NULL,
    IS_NONCONCURRENT VARCHAR2(1) NULL,
    REQUESTS_RECOVERY VARCHAR2(1) NULL,
    CONSTRAINT QRTZ_FIRED_TRIGGER_PK PRIMARY KEY (SCHED_NAME,ENTRY_ID)
);
CREATE TABLE qrtz_scheduler_state
  (
    SCHED_NAME VARCHAR2(120) NOT NULL,
    INSTANCE_NAME VARCHAR2(200) NOT NULL,
    LAST_CHECKIN_TIME NUMBER(13) NOT NULL,
    CHECKIN_INTERVAL NUMBER(13) NOT NULL,
    CONSTRAINT QRTZ_SCHEDULER_STATE_PK PRIMARY KEY (SCHED_NAME,INSTANCE_NAME)
);
CREATE TABLE qrtz_locks
  (
    SCHED_NAME VARCHAR2(120) NOT NULL,
    LOCK_NAME  VARCHAR2(40) NOT NULL,
    CONSTRAINT QRTZ_LOCKS_PK PRIMARY KEY (SCHED_NAME,LOCK_NAME)
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



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


-- DDL
-- add user_name column to the tables
ALTER TABLE clusterconfigmapping ADD (user_name VARCHAR2 (255) DEFAULT '_db');

ALTER TABLE hostconfigmapping ADD (user_name VARCHAR2 (255) DEFAULT '_db');

ALTER TABLE stage ADD (cluster_host_info BLOB DEFAULT NULL);

-- add decommission state
ALTER TABLE hostcomponentdesiredstate ADD (admin_state VARCHAR2 (32) DEFAULT NULL);
ALTER TABLE hostcomponentdesiredstate ADD (passive_state VARCHAR2 (32) NOT NULL DEFAULT 'ACTIVE');

-- DML
--Upgrade version to current
UPDATE metainfo SET "metainfo_value" = '${ambariVersion}' WHERE "metainfo_key" = 'version';

INSERT INTO ambari_sequences(sequence_name, value) values ('configgroup_id_seq', 1);

-- drop deprecated tables componentconfigmapping and hostcomponentconfigmapping
-- not required after Config Group implementation
--DROP TABLE componentconfigmapping;
--DROP TABLE hostcomponentconfigmapping;

-- required for Config Group implementation
CREATE TABLE configgroup (group_id NUMBER(19), cluster_id NUMBER(19) NOT NULL, group_name VARCHAR2(255) NOT NULL, tag VARCHAR2(1024) NOT NULL, description VARCHAR2(1024), create_timestamp NUMBER(19) NOT NULL, PRIMARY KEY(group_id), UNIQUE(group_name));
CREATE TABLE confgroupclusterconfigmapping (config_group_id NUMBER(19) NOT NULL, cluster_id NUMBER(19) NOT NULL, config_type VARCHAR2(255) NOT NULL, version_tag VARCHAR2(255) NOT NULL, user_name VARCHAR2(255) DEFAULT '_db', create_timestamp NUMBER(19) NOT NULL, PRIMARY KEY(config_group_id, cluster_id, config_type));
CREATE TABLE configgrouphostmapping (config_group_id NUMBER(19) NOT NULL, host_name VARCHAR2(255) NOT NULL, PRIMARY KEY(config_group_id, host_name));

ALTER TABLE configgroup ADD CONSTRAINT FK_configgroup_cluster_id FOREIGN KEY (cluster_id) REFERENCES clusters (cluster_id);
ALTER TABLE confgroupclusterconfigmapping ADD CONSTRAINT FK_confg FOREIGN KEY (version_tag, config_type, cluster_id) REFERENCES clusterconfig (version_tag, type_name, cluster_id);
ALTER TABLE confgroupclusterconfigmapping ADD CONSTRAINT FK_cgccm_gid FOREIGN KEY (config_group_id) REFERENCES configgroup (group_id);
ALTER TABLE configgrouphostmapping ADD CONSTRAINT FK_cghm_cgid FOREIGN KEY (config_group_id) REFERENCES configgroup (group_id);
ALTER TABLE configgrouphostmapping ADD CONSTRAINT FK_cghm_hname FOREIGN KEY (host_name) REFERENCES hosts (host_name);

-- Don't set not null constraint
-- ALTER TABLE stage MODIFY (cluster_host_info NOT NULL);

-- blueprint related tables
CREATE TABLE blueprint (blueprint_name VARCHAR2(255) NOT NULL, stack_name VARCHAR2(255) NOT NULL, stack_version VARCHAR2(255) NOT NULL, PRIMARY KEY(blueprint_name));
CREATE TABLE hostgroup (blueprint_name VARCHAR2(255) NOT NULL, name VARCHAR2(255) NOT NULL, cardinality VARCHAR2(255) NOT NULL, PRIMARY KEY(blueprint_name, name));
CREATE TABLE hostgroup_component (blueprint_name VARCHAR2(255) NOT NULL, hostgroup_name VARCHAR2(255) NOT NULL, name VARCHAR2(255) NOT NULL, PRIMARY KEY(blueprint_name, hostgroup_name, name));

ALTER TABLE hostgroup ADD FOREIGN KEY (blueprint_name) REFERENCES ambari.blueprint(blueprint_name);
ALTER TABLE hostgroup_component ADD FOREIGN KEY (blueprint_name, hostgroup_name) REFERENCES ambari.hostgroup(blueprint_name, name);

-- Abort all tasks in progress due to format change
UPDATE host_role_command SET status = 'ABORTED' WHERE status IN ('PENDING', 'QUEUED', 'IN_PROGRESS');

ALTER TABLE hosts DROP COLUMN disks_info;

--Added end_time and structured output support to command execution result
ALTER TABLE host_role_command ADD (end_time NUMBER(19) DEFAULT NULL);
ALTER TABLE host_role_command ADD (structured_out BLOB DEFAULT NULL);

--1.5.0 upgrade
CREATE TABLE request (request_id NUMBER(19) NOT NULL, cluster_id NUMBER(19), request_schedule_id NUMBER(19), command_name VARCHAR(255), create_time NUMBER(19) NOT NULL, end_time NUMBER(19) NOT NULL, inputs CLOB, request_context VARCHAR(255), request_type VARCHAR(255), start_time NUMBER(19) NOT NULL, status VARCHAR(255), target_component VARCHAR(255), target_hosts CLOB, target_service VARCHAR(255), PRIMARY KEY (request_id))

INSERT INTO request(request_id, cluster_id, request_context, start_time, end_time, create_time)
  SELECT DISTINCT s.request_id, s.cluster_id, s.request_context, nvl(cmd.start_time, -1), nvl(cmd.end_time, -1), -1
  FROM
    (SELECT DISTINCT request_id, cluster_id, request_context FROM stage ) s
    LEFT JOIN
    (SELECT request_id, min(start_time) as start_time, max(end_time) as end_time FROM host_role_command GROUP BY request_id) cmd
    ON s.request_id=cmd.request_id;

CREATE TABLE requestschedule (schedule_id NUMBER(19), cluster_id NUMBER(19) NOT NULL, description VARCHAR2(255), status VARCHAR2(255), batch_separation_seconds smallint, batch_toleration_limit smallint, create_user VARCHAR2(255), create_timestamp NUMBER(19), update_user VARCHAR2(255), update_timestamp NUMBER(19), minutes VARCHAR2(10), hours VARCHAR2(10), days_of_month VARCHAR2(10), month VARCHAR2(10), day_of_week VARCHAR2(10), yearToSchedule VARCHAR2(10), startTime VARCHAR2(50), endTime VARCHAR2(50), last_execution_status VARCHAR2(255), PRIMARY KEY(schedule_id));
CREATE TABLE requestschedulebatchrequest (schedule_id NUMBER(19), batch_id NUMBER(19), request_id NUMBER(19), request_type VARCHAR2(255), request_uri VARCHAR2(1024), request_body BLOB, request_status VARCHAR2(255), return_code smallint, return_message VARCHAR2(2000), PRIMARY KEY(schedule_id, batch_id));

ALTER TABLE stage ADD CONSTRAINT FK_stage_request_id FOREIGN KEY (request_id) REFERENCES request (request_id);
ALTER TABLE request ADD CONSTRAINT FK_request_cluster_id FOREIGN KEY (cluster_id) REFERENCES clusters (cluster_id);
ALTER TABLE request ADD CONSTRAINT FK_request_schedule_id FOREIGN KEY (request_schedule_id) REFERENCES requestschedule (schedule_id);
ALTER TABLE requestschedulebatchrequest ADD CONSTRAINT FK_rsbatchrequest_schedule_id FOREIGN KEY (schedule_id) REFERENCES requestschedule (schedule_id)

--quartz tables
CREATE TABLE qrtz_job_details ( SCHED_NAME VARCHAR2(120) NOT NULL, JOB_NAME  VARCHAR2(200) NOT NULL, JOB_GROUP VARCHAR2(200) NOT NULL, DESCRIPTION VARCHAR2(250) NULL, JOB_CLASS_NAME   VARCHAR2(250) NOT NULL, IS_DURABLE VARCHAR2(1) NOT NULL, IS_NONCONCURRENT VARCHAR2(1) NOT NULL, IS_UPDATE_DATA VARCHAR2(1) NOT NULL, REQUESTS_RECOVERY VARCHAR2(1) NOT NULL, JOB_DATA BLOB NULL, CONSTRAINT QRTZ_JOB_DETAILS_PK PRIMARY KEY (SCHED_NAME,JOB_NAME,JOB_GROUP) );
CREATE TABLE qrtz_triggers ( SCHED_NAME VARCHAR2(120) NOT NULL, TRIGGER_NAME VARCHAR2(200) NOT NULL, TRIGGER_GROUP VARCHAR2(200) NOT NULL, JOB_NAME  VARCHAR2(200) NOT NULL, JOB_GROUP VARCHAR2(200) NOT NULL, DESCRIPTION VARCHAR2(250) NULL, NEXT_FIRE_TIME NUMBER(13) NULL, PREV_FIRE_TIME NUMBER(13) NULL, PRIORITY NUMBER(13) NULL, TRIGGER_STATE VARCHAR2(16) NOT NULL, TRIGGER_TYPE VARCHAR2(8) NOT NULL, START_TIME NUMBER(13) NOT NULL, END_TIME NUMBER(13) NULL, CALENDAR_NAME VARCHAR2(200) NULL, MISFIRE_INSTR NUMBER(2) NULL, JOB_DATA BLOB NULL, CONSTRAINT QRTZ_TRIGGERS_PK PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP), CONSTRAINT QRTZ_TRIGGER_TO_JOBS_FK FOREIGN KEY (SCHED_NAME,JOB_NAME,JOB_GROUP) REFERENCES QRTZ_JOB_DETAILS(SCHED_NAME,JOB_NAME,JOB_GROUP) );
CREATE TABLE qrtz_simple_triggers ( SCHED_NAME VARCHAR2(120) NOT NULL, TRIGGER_NAME VARCHAR2(200) NOT NULL, TRIGGER_GROUP VARCHAR2(200) NOT NULL, REPEAT_COUNT NUMBER(7) NOT NULL, REPEAT_INTERVAL NUMBER(12) NOT NULL, TIMES_TRIGGERED NUMBER(10) NOT NULL, CONSTRAINT QRTZ_SIMPLE_TRIG_PK PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP), CONSTRAINT QRTZ_SIMPLE_TRIG_TO_TRIG_FK FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP) REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP) );
CREATE TABLE qrtz_cron_triggers ( SCHED_NAME VARCHAR2(120) NOT NULL, TRIGGER_NAME VARCHAR2(200) NOT NULL, TRIGGER_GROUP VARCHAR2(200) NOT NULL, CRON_EXPRESSION VARCHAR2(120) NOT NULL, TIME_ZONE_ID VARCHAR2(80), CONSTRAINT QRTZ_CRON_TRIG_PK PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP), CONSTRAINT QRTZ_CRON_TRIG_TO_TRIG_FK FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP) REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP) );
CREATE TABLE qrtz_simprop_triggers ( SCHED_NAME VARCHAR2(120) NOT NULL, TRIGGER_NAME VARCHAR2(200) NOT NULL, TRIGGER_GROUP VARCHAR2(200) NOT NULL, STR_PROP_1 VARCHAR2(512) NULL, STR_PROP_2 VARCHAR2(512) NULL, STR_PROP_3 VARCHAR2(512) NULL, INT_PROP_1 NUMBER(10) NULL, INT_PROP_2 NUMBER(10) NULL, LONG_PROP_1 NUMBER(13) NULL, LONG_PROP_2 NUMBER(13) NULL, DEC_PROP_1 NUMERIC(13,4) NULL, DEC_PROP_2 NUMERIC(13,4) NULL, BOOL_PROP_1 VARCHAR2(1) NULL, BOOL_PROP_2 VARCHAR2(1) NULL, CONSTRAINT QRTZ_SIMPROP_TRIG_PK PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP), CONSTRAINT QRTZ_SIMPROP_TRIG_TO_TRIG_FK FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP) REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP) );
CREATE TABLE qrtz_blob_triggers ( SCHED_NAME VARCHAR2(120) NOT NULL, TRIGGER_NAME VARCHAR2(200) NOT NULL, TRIGGER_GROUP VARCHAR2(200) NOT NULL, BLOB_DATA BLOB NULL, CONSTRAINT QRTZ_BLOB_TRIG_PK PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP), CONSTRAINT QRTZ_BLOB_TRIG_TO_TRIG_FK FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP) REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP) );
CREATE TABLE qrtz_calendars ( SCHED_NAME VARCHAR2(120) NOT NULL, CALENDAR_NAME  VARCHAR2(200) NOT NULL, CALENDAR BLOB NOT NULL, CONSTRAINT QRTZ_CALENDARS_PK PRIMARY KEY (SCHED_NAME,CALENDAR_NAME) );
CREATE TABLE qrtz_paused_trigger_grps ( SCHED_NAME VARCHAR2(120) NOT NULL, TRIGGER_GROUP  VARCHAR2(200) NOT NULL, CONSTRAINT QRTZ_PAUSED_TRIG_GRPS_PK PRIMARY KEY (SCHED_NAME,TRIGGER_GROUP) );
CREATE TABLE qrtz_fired_triggers ( SCHED_NAME VARCHAR2(120) NOT NULL, ENTRY_ID VARCHAR2(95) NOT NULL, TRIGGER_NAME VARCHAR2(200) NOT NULL, TRIGGER_GROUP VARCHAR2(200) NOT NULL, INSTANCE_NAME VARCHAR2(200) NOT NULL, FIRED_TIME NUMBER(13) NOT NULL, SCHED_TIME NUMBER(13) NOT NULL, PRIORITY NUMBER(13) NOT NULL, STATE VARCHAR2(16) NOT NULL, JOB_NAME VARCHAR2(200) NULL, JOB_GROUP VARCHAR2(200) NULL, IS_NONCONCURRENT VARCHAR2(1) NULL, REQUESTS_RECOVERY VARCHAR2(1) NULL, CONSTRAINT QRTZ_FIRED_TRIGGER_PK PRIMARY KEY (SCHED_NAME,ENTRY_ID) );
CREATE TABLE qrtz_scheduler_state ( SCHED_NAME VARCHAR2(120) NOT NULL, INSTANCE_NAME VARCHAR2(200) NOT NULL, LAST_CHECKIN_TIME NUMBER(13) NOT NULL, CHECKIN_INTERVAL NUMBER(13) NOT NULL, CONSTRAINT QRTZ_SCHEDULER_STATE_PK PRIMARY KEY (SCHED_NAME,INSTANCE_NAME) );
CREATE TABLE qrtz_locks ( SCHED_NAME VARCHAR2(120) NOT NULL, LOCK_NAME  VARCHAR2(40) NOT NULL, CONSTRAINT QRTZ_LOCKS_PK PRIMARY KEY (SCHED_NAME,LOCK_NAME) );

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

ALTER TABLE hoststate ADD (passive_state VARCHAR2(512) DEFAULT NULL);
ALTER TABLE servicedesiredstate ADD (passive_state VARCHAR2(32) NOT NULL DEFAULT 'ACTIVE');

commit;

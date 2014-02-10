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

--Upgrade version to current
UPDATE metainfo SET "metainfo_value" = '${ambariVersion}' WHERE metainfo_key = 'version';

ALTER TABLE hostcomponentdesiredstate ADD admin_state VARCHAR(32);

--1.5.0
CREATE TABLE request (request_id BIGINT NOT NULL, cluster_id BIGINT, request_schedule_id BIGINT, command_name VARCHAR(255), create_time BIGINT NOT NULL, end_time BIGINT NOT NULL, inputs LONGTEXT, request_context VARCHAR(255), request_type VARCHAR(255), start_time BIGINT NOT NULL, status VARCHAR(255), target_component VARCHAR(255), target_hosts LONGTEXT, target_service VARCHAR(255), PRIMARY KEY (request_id));
CREATE TABLE requestschedule (schedule_id bigint, cluster_id BIGINT NOT NULL, description varchar(255), status varchar(255), batch_separation_seconds smallint, batch_toleration_limit smallint, create_user varchar(255), create_timestamp bigint, update_user varchar(255), update_timestamp bigint, minutes varchar(10), hours varchar(10), days_of_month varchar(10), month varchar(10), day_of_week varchar(10), yearToSchedule varchar(10), startTime varchar(50), endTime varchar(50), last_execution_status varchar(255), PRIMARY KEY(schedule_id));
CREATE TABLE requestschedulebatchrequest (schedule_id bigint, batch_id bigint, request_id bigint, request_type varchar(255), request_uri varchar(1024), request_body LONGBLOB, request_status varchar(255), return_code smallint, return_message varchar(2000), PRIMARY KEY(schedule_id, batch_id));

insert into request(request_id, cluster_id, request_context, start_time, end_time, create_time)
  select distinct s.request_id, s.cluster_id, s.request_context, coalesce (cmd.start_time, -1), coalesce (cmd.end_time, -1), -1
  from
    (select distinct request_id, cluster_id, request_context from stage ) s
    left join
    (select request_id, min(start_time) as start_time, max(end_time) as end_time from host_role_command group by request_id) cmd
    on s.request_id=cmd.request_id;

ALTER TABLE stage ADD CONSTRAINT FK_stage_request_id FOREIGN KEY (request_id) REFERENCES request (request_id);
ALTER TABLE request ADD CONSTRAINT FK_request_cluster_id FOREIGN KEY (cluster_id) REFERENCES clusters (cluster_id);
ALTER TABLE request ADD CONSTRAINT FK_request_schedule_id FOREIGN KEY (request_schedule_id) REFERENCES requestschedule (schedule_id);
ALTER TABLE requestschedulebatchrequest ADD CONSTRAINT FK_requestschedulebatchrequest_schedule_id FOREIGN KEY (schedule_id) REFERENCES ambari.requestschedule (schedule_id);



--quartz tables
CREATE TABLE QRTZ_JOB_DETAILS ( SCHED_NAME VARCHAR(120) NOT NULL, JOB_NAME  VARCHAR(200) NOT NULL, JOB_GROUP VARCHAR(200) NOT NULL, DESCRIPTION VARCHAR(250) NULL, JOB_CLASS_NAME   VARCHAR(250) NOT NULL, IS_DURABLE VARCHAR(1) NOT NULL, IS_NONCONCURRENT VARCHAR(1) NOT NULL, IS_UPDATE_DATA VARCHAR(1) NOT NULL, REQUESTS_RECOVERY VARCHAR(1) NOT NULL, JOB_DATA BLOB NULL, PRIMARY KEY (SCHED_NAME,JOB_NAME,JOB_GROUP) );
CREATE TABLE QRTZ_TRIGGERS ( SCHED_NAME VARCHAR(120) NOT NULL, TRIGGER_NAME VARCHAR(200) NOT NULL, TRIGGER_GROUP VARCHAR(200) NOT NULL, JOB_NAME  VARCHAR(200) NOT NULL, JOB_GROUP VARCHAR(200) NOT NULL, DESCRIPTION VARCHAR(250) NULL, NEXT_FIRE_TIME BIGINT(13) NULL, PREV_FIRE_TIME BIGINT(13) NULL, PRIORITY INTEGER NULL, TRIGGER_STATE VARCHAR(16) NOT NULL, TRIGGER_TYPE VARCHAR(8) NOT NULL, START_TIME BIGINT(13) NOT NULL, END_TIME BIGINT(13) NULL, CALENDAR_NAME VARCHAR(200) NULL, MISFIRE_INSTR SMALLINT(2) NULL, JOB_DATA BLOB NULL, PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP), FOREIGN KEY (SCHED_NAME,JOB_NAME,JOB_GROUP) REFERENCES QRTZ_JOB_DETAILS(SCHED_NAME,JOB_NAME,JOB_GROUP) );
CREATE TABLE QRTZ_SIMPLE_TRIGGERS ( SCHED_NAME VARCHAR(120) NOT NULL, TRIGGER_NAME VARCHAR(200) NOT NULL, TRIGGER_GROUP VARCHAR(200) NOT NULL, REPEAT_COUNT BIGINT(7) NOT NULL, REPEAT_INTERVAL BIGINT(12) NOT NULL, TIMES_TRIGGERED BIGINT(10) NOT NULL, PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP), FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP) REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP) );
CREATE TABLE QRTZ_CRON_TRIGGERS ( SCHED_NAME VARCHAR(120) NOT NULL, TRIGGER_NAME VARCHAR(200) NOT NULL, TRIGGER_GROUP VARCHAR(200) NOT NULL, CRON_EXPRESSION VARCHAR(200) NOT NULL, TIME_ZONE_ID VARCHAR(80), PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP), FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP) REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP) );
CREATE TABLE QRTZ_SIMPROP_TRIGGERS ( SCHED_NAME VARCHAR(120) NOT NULL, TRIGGER_NAME VARCHAR(200) NOT NULL, TRIGGER_GROUP VARCHAR(200) NOT NULL, STR_PROP_1 VARCHAR(512) NULL, STR_PROP_2 VARCHAR(512) NULL, STR_PROP_3 VARCHAR(512) NULL, INT_PROP_1 INT NULL, INT_PROP_2 INT NULL, LONG_PROP_1 BIGINT NULL, LONG_PROP_2 BIGINT NULL, DEC_PROP_1 NUMERIC(13,4) NULL, DEC_PROP_2 NUMERIC(13,4) NULL, BOOL_PROP_1 VARCHAR(1) NULL, BOOL_PROP_2 VARCHAR(1) NULL, PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP), FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP) REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP) );
CREATE TABLE QRTZ_BLOB_TRIGGERS ( SCHED_NAME VARCHAR(120) NOT NULL, TRIGGER_NAME VARCHAR(200) NOT NULL, TRIGGER_GROUP VARCHAR(200) NOT NULL, BLOB_DATA BLOB NULL, PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP), FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP) REFERENCES QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP) );
CREATE TABLE QRTZ_CALENDARS ( SCHED_NAME VARCHAR(120) NOT NULL, CALENDAR_NAME  VARCHAR(200) NOT NULL, CALENDAR BLOB NOT NULL, PRIMARY KEY (SCHED_NAME,CALENDAR_NAME) );
CREATE TABLE QRTZ_PAUSED_TRIGGER_GRPS ( SCHED_NAME VARCHAR(120) NOT NULL, TRIGGER_GROUP  VARCHAR(200) NOT NULL, PRIMARY KEY (SCHED_NAME,TRIGGER_GROUP) );
CREATE TABLE QRTZ_FIRED_TRIGGERS ( SCHED_NAME VARCHAR(120) NOT NULL, ENTRY_ID VARCHAR(95) NOT NULL, TRIGGER_NAME VARCHAR(200) NOT NULL, TRIGGER_GROUP VARCHAR(200) NOT NULL, INSTANCE_NAME VARCHAR(200) NOT NULL, FIRED_TIME BIGINT(13) NOT NULL, SCHED_TIME BIGINT(13) NOT NULL, PRIORITY INTEGER NOT NULL, STATE VARCHAR(16) NOT NULL, JOB_NAME VARCHAR(200) NULL, JOB_GROUP VARCHAR(200) NULL, IS_NONCONCURRENT VARCHAR(1) NULL, REQUESTS_RECOVERY VARCHAR(1) NULL, PRIMARY KEY (SCHED_NAME,ENTRY_ID) );
CREATE TABLE QRTZ_SCHEDULER_STATE ( SCHED_NAME VARCHAR(120) NOT NULL, INSTANCE_NAME VARCHAR(200) NOT NULL, LAST_CHECKIN_TIME BIGINT(13) NOT NULL, CHECKIN_INTERVAL BIGINT(13) NOT NULL, PRIMARY KEY (SCHED_NAME,INSTANCE_NAME) );
CREATE TABLE QRTZ_LOCKS ( SCHED_NAME VARCHAR(120) NOT NULL, LOCK_NAME  VARCHAR(40) NOT NULL, PRIMARY KEY (SCHED_NAME,LOCK_NAME) );

create index idx_qrtz_j_req_recovery on QRTZ_JOB_DETAILS(SCHED_NAME,REQUESTS_RECOVERY);
create index idx_qrtz_j_grp on QRTZ_JOB_DETAILS(SCHED_NAME,JOB_GROUP);

create index idx_qrtz_t_j on QRTZ_TRIGGERS(SCHED_NAME,JOB_NAME,JOB_GROUP);
create index idx_qrtz_t_jg on QRTZ_TRIGGERS(SCHED_NAME,JOB_GROUP);
create index idx_qrtz_t_c on QRTZ_TRIGGERS(SCHED_NAME,CALENDAR_NAME);
create index idx_qrtz_t_g on QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_GROUP);
create index idx_qrtz_t_state on QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_STATE);
create index idx_qrtz_t_n_state on QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP,TRIGGER_STATE);
create index idx_qrtz_t_n_g_state on QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_GROUP,TRIGGER_STATE);
create index idx_qrtz_t_next_fire_time on QRTZ_TRIGGERS(SCHED_NAME,NEXT_FIRE_TIME);
create index idx_qrtz_t_nft_st on QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_STATE,NEXT_FIRE_TIME);
create index idx_qrtz_t_nft_misfire on QRTZ_TRIGGERS(SCHED_NAME,MISFIRE_INSTR,NEXT_FIRE_TIME);
create index idx_qrtz_t_nft_st_misfire on QRTZ_TRIGGERS(SCHED_NAME,MISFIRE_INSTR,NEXT_FIRE_TIME,TRIGGER_STATE);
create index idx_qrtz_t_nft_st_misfire_grp on QRTZ_TRIGGERS(SCHED_NAME,MISFIRE_INSTR,NEXT_FIRE_TIME,TRIGGER_GROUP,TRIGGER_STATE);

create index idx_qrtz_ft_trig_inst_name on QRTZ_FIRED_TRIGGERS(SCHED_NAME,INSTANCE_NAME);
create index idx_qrtz_ft_inst_job_req_rcvry on QRTZ_FIRED_TRIGGERS(SCHED_NAME,INSTANCE_NAME,REQUESTS_RECOVERY);
create index idx_qrtz_ft_j_g on QRTZ_FIRED_TRIGGERS(SCHED_NAME,JOB_NAME,JOB_GROUP);
create index idx_qrtz_ft_jg on QRTZ_FIRED_TRIGGERS(SCHED_NAME,JOB_GROUP);
create index idx_qrtz_ft_t_g on QRTZ_FIRED_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP);
create index idx_qrtz_ft_tg on QRTZ_FIRED_TRIGGERS(SCHED_NAME,TRIGGER_GROUP);

ALTER TABLE hostcomponentdesiredstate ADD passive_state VARCHAR(32) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE servicedesiredstate ADD passive_state VARCHAR(32) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE hoststate ADD passive_state VARCHAR(512);
ALTER TABLE host_role_command ADD command_detail VARCHAR(255);
ALTER TABLE host_role_command ADD custom_command_name VARCHAR(255);

-- blueprint related tables
CREATE TABLE blueprint (blueprint_name VARCHAR(255) NOT NULL, stack_name VARCHAR(255) NOT NULL, stack_version VARCHAR(255) NOT NULL, PRIMARY KEY(blueprint_name));
CREATE TABLE hostgroup (blueprint_name VARCHAR(255) NOT NULL, name VARCHAR(255) NOT NULL, cardinality VARCHAR(255) NOT NULL, PRIMARY KEY(blueprint_name, name));
CREATE TABLE hostgroup_component (blueprint_name VARCHAR(255) NOT NULL, hostgroup_name VARCHAR(255) NOT NULL, name VARCHAR(255) NOT NULL, PRIMARY KEY(blueprint_name, hostgroup_name, name));

ALTER TABLE hostgroup ADD FOREIGN KEY (blueprint_name) REFERENCES blueprint(blueprint_name);
ALTER TABLE hostgroup_component ADD FOREIGN KEY (blueprint_name, hostgroup_name) REFERENCES hostgroup(blueprint_name, name);

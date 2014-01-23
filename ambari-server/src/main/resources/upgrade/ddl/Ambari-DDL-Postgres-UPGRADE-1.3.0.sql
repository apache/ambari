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
\connect :dbname;

-- service to cluster level config mappings move. idempotent update
CREATE LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION update_clusterconfigmapping()
  RETURNS void AS
$_$
BEGIN

IF NOT EXISTS (
  SELECT *
  FROM   pg_catalog.pg_tables
  WHERE  schemaname = 'ambari'
  AND    tablename  = 'clusterconfigmapping'
  )
  THEN

    CREATE TABLE ambari.clusterconfigmapping (cluster_id bigint NOT NULL, type_name VARCHAR(255) NOT NULL, version_tag VARCHAR(255) NOT NULL, create_timestamp BIGINT NOT NULL, selected INTEGER NOT NULL DEFAULT 0, user_name VARCHAR(255) NOT NULL DEFAULT '_db', PRIMARY KEY (cluster_id, type_name, create_timestamp));
    ALTER TABLE ambari.clusterconfigmapping ADD CONSTRAINT FK_clusterconfigmapping_cluster_id FOREIGN KEY (cluster_id) REFERENCES ambari.clusters (cluster_id);
    INSERT INTO ambari.clusterconfigmapping(cluster_id, type_name, version_tag, create_timestamp, selected)
      (SELECT DISTINCT cluster_id, config_type, config_tag, cast(date_part('epoch', now()) as bigint), 1
        FROM ambari.serviceconfigmapping scm
        WHERE timestamp = (SELECT max(timestamp) FROM ambari.serviceconfigmapping WHERE cluster_id = scm.cluster_id AND config_type = scm.config_type));
    DELETE FROM ambari.serviceconfigmapping;

END IF;

END;
$_$ LANGUAGE plpgsql;

-- Upgrade from 1.2.0
ALTER TABLE ambari.hosts
  ADD COLUMN ph_cpu_count INTEGER,
  ALTER COLUMN disks_info TYPE VARCHAR(32000);

-- Upgrade to 1.3.0

-- setting run-time search_path for :username
ALTER SCHEMA ambari OWNER TO :username;
ALTER ROLE :username SET search_path to 'ambari';

--updating clusterstate table
ALTER TABLE ambari.clusterstate
  ADD COLUMN current_stack_version VARCHAR(255) NOT NULL;

--updating hostconfigmapping table
ALTER TABLE ambari.hostconfigmapping
  ADD COLUMN user_name VARCHAR(255) NOT NULL DEFAULT '_db';
CREATE TABLE ambari.hostconfigmapping (cluster_id bigint NOT NULL, host_name VARCHAR(255) NOT NULL, type_name VARCHAR(255) NOT NULL, version_tag VARCHAR(255) NOT NULL, service_name VARCHAR(255), create_timestamp BIGINT NOT NULL, selected INTEGER NOT NULL DEFAULT 0, PRIMARY KEY (cluster_id, host_name, type_name, create_timestamp));
GRANT ALL PRIVILEGES ON TABLE ambari.hostconfigmapping TO :username;
ALTER TABLE ambari.hostconfigmapping ADD CONSTRAINT FK_hostconfigmapping_cluster_id FOREIGN KEY (cluster_id) REFERENCES ambari.clusters (cluster_id);
ALTER TABLE ambari.hostconfigmapping ADD CONSTRAINT FK_hostconfigmapping_host_name FOREIGN KEY (host_name) REFERENCES ambari.hosts (host_name);

--updating stage table
ALTER TABLE ambari.stage ADD COLUMN request_context VARCHAR(255);
ALTER TABLE ambari.stage ADD COLUMN cluster_host_info BYTEA;

-- portability changes for MySQL/Oracle support
ALTER TABLE ambari.hostcomponentdesiredconfigmapping rename to hcdesiredconfigmapping;
ALTER TABLE ambari.users ALTER column user_id DROP DEFAULT;
ALTER TABLE ambari.users ALTER column ldap_user TYPE INTEGER USING CASE WHEN ldap_user=true THEN 1 ELSE 0 END;

--creating ambari_sequences table instead of deprecated sequences
CREATE TABLE ambari.ambari_sequences (sequence_name VARCHAR(255) PRIMARY KEY, "value" BIGINT NOT NULL);
GRANT ALL PRIVILEGES ON TABLE ambari.ambari_sequences TO :username;

INSERT INTO ambari.ambari_sequences(sequence_name, "value")
  SELECT 'cluster_id_seq', nextval('ambari.clusters_cluster_id_seq')
  UNION ALL
  SELECT 'user_id_seq', nextval('ambari.users_user_id_seq')
  UNION ALL
  SELECT 'host_role_command_id_seq', COALESCE((SELECT max(task_id) FROM ambari.host_role_command), 1) + 50
  UNION ALL
  SELECT 'configgroup_id_seq', 1;

DROP sequence ambari.host_role_command_task_id_seq;
DROP sequence ambari.users_user_id_seq;
DROP sequence ambari.clusters_cluster_id_seq;

--updating metainfo table
CREATE TABLE ambari.metainfo (metainfo_key VARCHAR(255), metainfo_value VARCHAR, PRIMARY KEY(metainfo_key));
INSERT INTO ambari.metainfo (metainfo_key, metainfo_value) SELECT 'version', '${ambariVersion}';
UPDATE ambari.metainfo SET metainfo_value = '${ambariVersion}' WHERE metainfo_key = 'version';
GRANT ALL PRIVILEGES ON TABLE ambari.metainfo TO :username;

--replacing deprecated STOP_FAILED and START_FAILED states with INSTALLED
UPDATE ambari.hostcomponentstate SET current_state = 'INSTALLED' WHERE current_state LIKE 'STOP_FAILED';
UPDATE ambari.hostcomponentstate SET current_state = 'INSTALLED' WHERE current_state LIKE 'START_FAILED';

--updating clusterconfigmapping table
ALTER TABLE ambari.clusterconfigmapping
  ADD COLUMN user_name VARCHAR(255) NOT NULL DEFAULT '_db';
SELECT update_clusterconfigmapping();
GRANT ALL PRIVILEGES ON TABLE ambari.clusterconfigmapping TO :username;

-- drop deprecated tables componentconfigmapping and hostcomponentconfigmapping
-- not required after Config Group implementation
--DROP TABLE componentconfigmapping;
--DROP TABLE hostcomponentconfigmapping;

-- required for Config Group implementation
CREATE TABLE ambari.configgroup (group_id BIGINT, cluster_id BIGINT NOT NULL, group_name VARCHAR(255) NOT NULL, tag VARCHAR(1024) NOT NULL, description VARCHAR(1024), create_timestamp BIGINT NOT NULL, PRIMARY KEY(group_id), UNIQUE(group_name));
GRANT ALL PRIVILEGES ON TABLE ambari.configgroup TO :username;

CREATE TABLE ambari.confgroupclusterconfigmapping (config_group_id BIGINT NOT NULL, cluster_id BIGINT NOT NULL, config_type VARCHAR(255) NOT NULL, version_tag VARCHAR(255) NOT NULL, user_name VARCHAR(255) DEFAULT '_db', create_timestamp BIGINT NOT NULL, PRIMARY KEY(config_group_id, cluster_id, config_type));
GRANT ALL PRIVILEGES ON TABLE ambari.confgroupclusterconfigmapping TO :username;

CREATE TABLE ambari.configgrouphostmapping (config_group_id BIGINT NOT NULL, host_name VARCHAR(255) NOT NULL, PRIMARY KEY(config_group_id, host_name));
GRANT ALL PRIVILEGES ON TABLE ambari.configgrouphostmapping TO :username;

ALTER TABLE ambari.configgroup ADD CONSTRAINT FK_configgroup_cluster_id FOREIGN KEY (cluster_id) REFERENCES ambari.clusters (cluster_id);
ALTER TABLE ambari.confgroupclusterconfigmapping ADD CONSTRAINT FK_confgroupclusterconfigmapping_config_tag FOREIGN KEY (version_tag, config_type, cluster_id) REFERENCES ambari.clusterconfig (version_tag, type_name, cluster_id);
ALTER TABLE ambari.confgroupclusterconfigmapping ADD CONSTRAINT FK_confgroupclusterconfigmapping_group_id FOREIGN KEY (config_group_id) REFERENCES ambari.configgroup (group_id);
ALTER TABLE ambari.configgrouphostmapping ADD CONSTRAINT FK_configgrouphostmapping_configgroup_id FOREIGN KEY (config_group_id) REFERENCES ambari.configgroup (group_id);
ALTER TABLE ambari.configgrouphostmapping ADD CONSTRAINT FK_configgrouphostmapping_host_name FOREIGN KEY (host_name) REFERENCES ambari.hosts (host_name);

-- add decommission state
ALTER TABLE ambari.hostcomponentdesiredstate ADD COLUMN admin_state VARCHAR(32);

--Move cluster host info for old execution commands to stage table
UPDATE ambari.stage sd
  SET 
    cluster_host_info = substring(ec.command, position('clusterHostInfo' in ec.command) + 17, position('configurations' in ec.command) - position('clusterHostInfo' in ec.command) - 19)
  FROM
    ambari.execution_command ec,
    ambari.host_role_command hrc,
    ambari.stage ss
  WHERE ec.task_id = hrc.task_id
  AND hrc.stage_id = ss.stage_id
  AND hrc.request_id = ss.request_id
  AND sd.cluster_host_info IS NULL;
  

--Set cluster_host_info column mandatory
ALTER TABLE ambari.stage ALTER COLUMN cluster_host_info SET NOT NULL;

--Compress cluster host info-----------------------------
CREATE OR REPLACE FUNCTION get_keys(p_cluster_host_info text)
  RETURNS setof text AS
$_$
DECLARE
v_r text;
BEGIN
  FOR v_r IN (SELECT substr(key_tokens,3,length(key_tokens)) AS cluster_host_info_key
  FROM regexp_split_to_table(p_cluster_host_info, E'":\[[a-z0-9":.,-]{1,}\]') AS key_tokens
   WHERE key_tokens NOT LIKE '%ambari_db_rca_%') LOOP
     RETURN NEXT v_r;
  END LOOP;
END;
$_$ LANGUAGE plpgsql;



CREATE OR REPLACE FUNCTION get_value(p_cluster_host_info text, p_param_key text)
  RETURNS text AS
$_$

DECLARE
v_param_value text;
BEGIN

	SELECT regexp_matches(p_cluster_host_info,

	 '"' || p_param_key || E'":\[["a-z0-9., ]{1,}]') into v_param_value;

	SELECT substring(v_param_value, length(p_param_key) + 9, length(v_param_value) - length(p_param_key) - 11) into v_param_value;

	RETURN v_param_value;
	
END;
$_$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION compress_cluster_host_info(p_stage_id ambari.stage.stage_id%type, p_request_id ambari.stage.request_id%type) RETURNS text AS
$_$
DECLARE

cur1 CURSOR(p_param_name text) IS 

  select a.param_key, string_to_array(get_value(cast(cluster_host_info as text), a.param_key), ',') as param_value
  from (
    select s.stage_id, request_id, get_keys(cast(cluster_host_info as text)) as param_key, s.cluster_host_info from ambari.stage s) a
      where stage_id = p_stage_id
      and request_id = p_request_id
      and (a.param_key = p_param_name or (p_param_name is null and a.param_key not in ('all_hosts', 'all_ping_ports')));

v_all_hosts text[];
v_all_ping_ports text[];
v_indexed integer[];
v_r record;
v_param_key text;
v_compressed_ping_ports text[];
v_compressed_cluster_host_info text;

BEGIN

  open cur1('all_hosts');
  fetch cur1 into v_param_key, v_all_hosts;
  close cur1;

  open cur1('all_ping_ports');
  fetch cur1 into v_param_key, v_all_ping_ports;
  close cur1;

  v_compressed_cluster_host_info = '{';

  for v_r in cur1(null) loop
    v_indexed = to_indexed_array(v_r.param_value, v_all_hosts);
    select v_compressed_cluster_host_info || '"' || v_r.param_key || '":["' || array_to_string(v_indexed, ',') || '"],'
    into v_compressed_cluster_host_info;

  end loop;

  v_compressed_ping_ports = to_mapped_indexed_array(v_all_ping_ports);

  v_compressed_cluster_host_info = v_compressed_cluster_host_info || '"all_hosts":["' || array_to_string(v_all_hosts, ',') || '"],';

  v_compressed_cluster_host_info = v_compressed_cluster_host_info || '"all_ping_ports":["' || array_to_string(v_compressed_ping_ports, ',') || '"]';

  v_compressed_cluster_host_info = v_compressed_cluster_host_info || '}';

  return v_compressed_cluster_host_info;

  
END;
$_$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION index_of(p_arr text[], p_item text)
RETURNS INT AS 
$_$
DECLARE
  v_index integer;
BEGIN

    SELECT i-1
    into v_index
      FROM generate_subscripts(p_arr, 1) AS i
     WHERE p_arr[i] = p_item
  ORDER BY i;

  RETURN v_index;
END;
$_$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION to_indexed_array(arr text[], dict_array text[])
RETURNS integer[] AS 
$_$

DECLARE

v_result integer[];
v_index_of integer;

BEGIN

  FOR i IN array_lower(arr, 1)..array_upper(arr, 1)
    LOOP
        v_index_of = index_of(dict_array, arr[i]);
        select array_append(v_result, v_index_of) into v_result;
    END LOOP;

  RETURN v_result; 
    
END;
$_$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION to_mapped_indexed_array(p_arr text[])
RETURNS text[] AS 
$_$
DECLARE
v_result text[];
v_r record;
v_curr_indexes text;
v_prev_val text;
BEGIN

  FOR v_r in (select (row_number() OVER (ORDER BY 1)) -1 AS ind, x AS val from (select unnest(p_arr) AS x) a) LOOP

    if v_r.val <> v_prev_val then
      v_result = array_append(v_result, v_curr_indexes);
      v_curr_indexes = null;
    end if;

    if v_curr_indexes is null then
      v_curr_indexes = v_r.val || ':' || v_r.ind;
    else
      v_curr_indexes = v_curr_indexes || ',' || v_r.ind;
    end if;

    v_prev_val = v_r.val;
    
  END LOOP;

  if v_curr_indexes is not null then
    v_result = array_append(v_result, v_curr_indexes);
  end if;
  
  RETURN v_result; 
    
END;
$_$ LANGUAGE plpgsql;

--Update cluster host info to compressed values
UPDATE ambari.stage s
SET cluster_host_info = (decode(replace(compress_cluster_host_info(stage_id, request_id), E'\\', E'\\\\'), 'escape'))
WHERE s.cluster_host_info LIKE '%ambari_db_rca%';

--Drop compression functions
DROP FUNCTION get_keys;
DROP FUNCTION get_value;
DROP FUNCTION compress_cluster_host_info;
DROP FUNCTION to_indexed_array;
DROP FUNCTION to_mapped_indexed_array;


ALTER TABLE ambari.hosts DROP COLUMN disks_info;

--Added end_time and structured output support to command execution result
ALTER TABLE ambari.host_role_command ADD COLUMN end_time BIGINT;
ALTER TABLE ambari.host_role_command ADD COLUMN structured_out BYTEA;

--1.5.0 upgrade

CREATE TABLE ambari.request (request_id BIGINT NOT NULL, cluster_id BIGINT, command_name VARCHAR(255), create_time BIGINT NOT NULL, end_time BIGINT NOT NULL, inputs VARCHAR(32000), request_context VARCHAR(255), request_type VARCHAR(255), request_schedule_id BIGINT, start_time BIGINT NOT NULL, status VARCHAR(255), target_component VARCHAR(255), target_hosts TEXT, target_service VARCHAR(255), PRIMARY KEY (request_id));
GRANT ALL PRIVILEGES ON TABLE ambari.request TO :username;

--insert request data
begin;
insert into ambari.request(request_id, cluster_id, request_context, start_time, end_time, create_time) (
  select distinct s.request_id, s.cluster_id, s.request_context, coalesce (cmd.start_time, -1), coalesce (cmd.end_time, -1), -1
  from
    (select distinct request_id, cluster_id, request_context from ambari.stage ) s
    left join
    (select request_id, min(start_time) as start_time, max(end_time) as end_time from ambari.host_role_command group by request_id) cmd
    on s.request_id=cmd.request_id
);

commit;

CREATE TABLE ambari.requestschedule (schedule_id bigint, cluster_id bigint NOT NULL, description varchar(255), status varchar(255), batch_separation_seconds smallint, batch_toleration_limit smallint, create_user varchar(255), create_timestamp bigint, update_user varchar(255), update_timestamp bigint, minutes varchar(10), hours varchar(10), days_of_month varchar(10), month varchar(10), day_of_week varchar(10), yearToSchedule varchar(10), startTime varchar(50), endTime varchar(50), last_execution_status varchar(255), PRIMARY KEY(schedule_id));
GRANT ALL PRIVILEGES ON TABLE ambari.requestschedule TO :username;

CREATE TABLE ambari.requestschedulebatchrequest (schedule_id bigint, batch_id bigint, request_id bigint, request_type varchar(255), request_uri varchar(1024), request_body BYTEA, request_status varchar(255), return_code smallint, return_message varchar(20000), PRIMARY KEY(schedule_id, batch_id));
GRANT ALL PRIVILEGES ON TABLE ambari.requestschedulebatchrequest TO :username;

ALTER TABLE ambari.stage ADD CONSTRAINT FK_stage_request_id FOREIGN KEY (request_id) REFERENCES ambari.request (request_id);
ALTER TABLE ambari.request ADD CONSTRAINT FK_request_cluster_id FOREIGN KEY (cluster_id) REFERENCES ambari.clusters (cluster_id);
ALTER TABLE ambari.request ADD CONSTRAINT FK_request_schedule_id FOREIGN KEY (request_schedule_id) REFERENCES ambari.requestschedule (schedule_id);

--Create quartz tables for schedule manager
CREATE TABLE ambari.qrtz_job_details ( SCHED_NAME VARCHAR(120) NOT NULL, JOB_NAME  VARCHAR(200) NOT NULL, JOB_GROUP VARCHAR(200) NOT NULL, DESCRIPTION VARCHAR(250) NULL, JOB_CLASS_NAME   VARCHAR(250) NOT NULL, IS_DURABLE BOOL NOT NULL, IS_NONCONCURRENT BOOL NOT NULL, IS_UPDATE_DATA BOOL NOT NULL, REQUESTS_RECOVERY BOOL NOT NULL, JOB_DATA BYTEA NULL, PRIMARY KEY (SCHED_NAME,JOB_NAME,JOB_GROUP) );
GRANT ALL PRIVILEGES ON TABLE ambari.qrtz_job_details TO :username;

CREATE TABLE ambari.qrtz_triggers ( SCHED_NAME VARCHAR(120) NOT NULL, TRIGGER_NAME VARCHAR(200) NOT NULL, TRIGGER_GROUP VARCHAR(200) NOT NULL, JOB_NAME  VARCHAR(200) NOT NULL, JOB_GROUP VARCHAR(200) NOT NULL, DESCRIPTION VARCHAR(250) NULL, NEXT_FIRE_TIME BIGINT NULL, PREV_FIRE_TIME BIGINT NULL, PRIORITY INTEGER NULL, TRIGGER_STATE VARCHAR(16) NOT NULL, TRIGGER_TYPE VARCHAR(8) NOT NULL, START_TIME BIGINT NOT NULL, END_TIME BIGINT NULL, CALENDAR_NAME VARCHAR(200) NULL, MISFIRE_INSTR SMALLINT NULL, JOB_DATA BYTEA NULL, PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP), FOREIGN KEY (SCHED_NAME,JOB_NAME,JOB_GROUP) REFERENCES ambari.QRTZ_JOB_DETAILS(SCHED_NAME,JOB_NAME,JOB_GROUP) );
GRANT ALL PRIVILEGES ON TABLE ambari.qrtz_triggers TO :username;

CREATE TABLE ambari.qrtz_simple_triggers ( SCHED_NAME VARCHAR(120) NOT NULL, TRIGGER_NAME VARCHAR(200) NOT NULL, TRIGGER_GROUP VARCHAR(200) NOT NULL, REPEAT_COUNT BIGINT NOT NULL, REPEAT_INTERVAL BIGINT NOT NULL, TIMES_TRIGGERED BIGINT NOT NULL, PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP), FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP) REFERENCES ambari.QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP) );
GRANT ALL PRIVILEGES ON TABLE ambari.qrtz_simple_triggers TO :username;

CREATE TABLE ambari.qrtz_cron_triggers ( SCHED_NAME VARCHAR(120) NOT NULL, TRIGGER_NAME VARCHAR(200) NOT NULL, TRIGGER_GROUP VARCHAR(200) NOT NULL, CRON_EXPRESSION VARCHAR(120) NOT NULL, TIME_ZONE_ID VARCHAR(80), PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP), FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP) REFERENCES ambari.QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP) );
GRANT ALL PRIVILEGES ON TABLE ambari.qrtz_cron_triggers TO :username;

CREATE TABLE ambari.qrtz_simprop_triggers ( SCHED_NAME VARCHAR(120) NOT NULL, TRIGGER_NAME VARCHAR(200) NOT NULL, TRIGGER_GROUP VARCHAR(200) NOT NULL, STR_PROP_1 VARCHAR(512) NULL, STR_PROP_2 VARCHAR(512) NULL, STR_PROP_3 VARCHAR(512) NULL, INT_PROP_1 INT NULL, INT_PROP_2 INT NULL, LONG_PROP_1 BIGINT NULL, LONG_PROP_2 BIGINT NULL, DEC_PROP_1 NUMERIC(13,4) NULL, DEC_PROP_2 NUMERIC(13,4) NULL, BOOL_PROP_1 BOOL NULL, BOOL_PROP_2 BOOL NULL, PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP), FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP) REFERENCES ambari.QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP) );
GRANT ALL PRIVILEGES ON TABLE ambari.qrtz_simprop_triggers TO :username;
CREATE TABLE ambari.qrtz_blob_triggers ( SCHED_NAME VARCHAR(120) NOT NULL, TRIGGER_NAME VARCHAR(200) NOT NULL, TRIGGER_GROUP VARCHAR(200) NOT NULL, BLOB_DATA BYTEA NULL, PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP), FOREIGN KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP) REFERENCES ambari.QRTZ_TRIGGERS(SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP) );
GRANT ALL PRIVILEGES ON TABLE ambari.qrtz_blob_triggers TO :username;

CREATE TABLE ambari.qrtz_calendars ( SCHED_NAME VARCHAR(120) NOT NULL, CALENDAR_NAME  VARCHAR(200) NOT NULL, CALENDAR BYTEA NOT NULL, PRIMARY KEY (SCHED_NAME,CALENDAR_NAME) );
GRANT ALL PRIVILEGES ON TABLE ambari.qrtz_calendars TO :username;


CREATE TABLE ambari.qrtz_paused_trigger_grps ( SCHED_NAME VARCHAR(120) NOT NULL, TRIGGER_GROUP  VARCHAR(200) NOT NULL, PRIMARY KEY (SCHED_NAME,TRIGGER_GROUP) );
GRANT ALL PRIVILEGES ON TABLE ambari.qrtz_paused_trigger_grps TO :username;

CREATE TABLE ambari.qrtz_fired_triggers ( SCHED_NAME VARCHAR(120) NOT NULL, ENTRY_ID VARCHAR(95) NOT NULL, TRIGGER_NAME VARCHAR(200) NOT NULL, TRIGGER_GROUP VARCHAR(200) NOT NULL, INSTANCE_NAME VARCHAR(200) NOT NULL, FIRED_TIME BIGINT NOT NULL, SCHED_TIME BIGINT NOT NULL, PRIORITY INTEGER NOT NULL, STATE VARCHAR(16) NOT NULL, JOB_NAME VARCHAR(200) NULL, JOB_GROUP VARCHAR(200) NULL, IS_NONCONCURRENT BOOL NULL, REQUESTS_RECOVERY BOOL NULL, PRIMARY KEY (SCHED_NAME,ENTRY_ID) );
GRANT ALL PRIVILEGES ON TABLE ambari.qrtz_fired_triggers TO :username;

CREATE TABLE ambari.qrtz_scheduler_state ( SCHED_NAME VARCHAR(120) NOT NULL, INSTANCE_NAME VARCHAR(200) NOT NULL, LAST_CHECKIN_TIME BIGINT NOT NULL, CHECKIN_INTERVAL BIGINT NOT NULL, PRIMARY KEY (SCHED_NAME,INSTANCE_NAME) );
GRANT ALL PRIVILEGES ON TABLE ambari.qrtz_scheduler_state TO :username;

CREATE TABLE ambari.qrtz_locks (SCHED_NAME VARCHAR(120) NOT NULL, LOCK_NAME  VARCHAR(40) NOT NULL, PRIMARY KEY (SCHED_NAME,LOCK_NAME));
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

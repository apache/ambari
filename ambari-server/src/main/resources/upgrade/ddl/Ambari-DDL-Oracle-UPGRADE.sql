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

-- DML
--Upgrade version to current
UPDATE metainfo SET "metainfo_key" = 'version', "metainfo_value" = '${ambariVersion}';

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
ALTER TABLE confgroupclusterconfigmapping ADD CONSTRAINT FK_confgroupclusterconfigmapping_config_tag FOREIGN KEY (version_tag, config_type, cluster_id) REFERENCES clusterconfig (version_tag, type_name, cluster_id);
ALTER TABLE confgroupclusterconfigmapping ADD CONSTRAINT FK_confgroupclusterconfigmapping_group_id FOREIGN KEY (config_group_id) REFERENCES configgroup (group_id);
ALTER TABLE configgrouphostmapping ADD CONSTRAINT FK_configgrouphostmapping_configgroup_id FOREIGN KEY (config_group_id) REFERENCES configgroup (group_id);
ALTER TABLE configgrouphostmapping ADD CONSTRAINT FK_configgrouphostmapping_host_name FOREIGN KEY (host_name) REFERENCES hosts (host_name);


UPDATE
  stage sd
SET
  (sd.cluster_host_info) =
  (
    SELECT DISTINCT
      (dbms_lob.substr(ec.command, dbms_lob.instr(ec.command,
      '636f6e66696775726174696f6e73')   - dbms_lob.instr(ec.command,
      '636c7573746572486f7374496e666f') - 19, dbms_lob.instr(ec.command,
      '636c7573746572486f7374496e666f') + 17) )
    FROM
      execution_command ec ,
      host_role_command hrc,
      stage ss
    WHERE
      ec.task_id       = hrc.task_id
    AND hrc.stage_id   = ss.stage_id
    AND hrc.request_id = ss.request_id
    AND ss.stage_id    = sd.stage_id
    AND ss.request_id  = sd.request_id
  );
  
ALTER TABLE stage MODIFY (cluster_host_info NOT NULL);




CREATE OR REPLACE PACKAGE compress_cluster_host_info_pkg AS
  TYPE nested_vachar2_tbl_type IS TABLE OF VARCHAR2(4000);
  FUNCTION compress_cluster_host_info(p_cluster_host_info BLOB) RETURN BLOB;
  FUNCTION get_keys(p_cluster_host_info VARCHAR2) RETURN VARCHAR2TBL PIPELINED;
  FUNCTION get_value(p_cluster_host_info BLOB, p_param_key VARCHAR2) RETURN VARCHAR2;
  FUNCTION split_to_array(p_string VARCHAR2,p_sep VARCHAR2) RETURN nested_vachar2_tbl_type;
  FUNCTION index_of(p_arr  nested_vachar2_tbl_type, p_item VARCHAR2) RETURN INTEGER;
  FUNCTION to_indexed_array(p_arr nested_vachar2_tbl_type, p_dict_arr nested_vachar2_tbl_type) RETURN nested_vachar2_tbl_type;
  FUNCTION To_mapped_indexed_array(p_arr nested_vachar2_tbl_type) RETURN nested_vachar2_tbl_type;
  FUNCTION Nested_table_to_string(p_arr nested_vachar2_tbl_type, p_sep VARCHAR2) RETURN VARCHAR2;
END compress_cluster_host_info_pkg;

/

CREATE OR REPLACE PACKAGE BODY compress_cluster_host_info_pkg AS
  c_regex_pattern CONSTANT VARCHAR2(29) := '":\[[a-zA-Z0-9@:/":.,-]{1,}\]';
  
  PROCEDURE print_nested_table(p_arr NESTED_VACHAR2_TBL_TYPE)
  AS
  BEGIN
      FOR i IN p_arr.FIRST..p_arr.LAST LOOP
          dbms_output.put_line(i
                               || ': '
                               || P_arr(i));
      END LOOP;
  END print_nested_table;
  FUNCTION Get_keys(p_cluster_host_info VARCHAR2)
  RETURN VARCHAR2TBL PIPELINED
  AS
  BEGIN
      FOR r IN (SELECT param_key
                FROM   (SELECT substr(regexp_substr(regexp_replace(
                                                    p_cluster_host_info
                                                    , c_regex_pattern
                                                            , ' '),
                                                      '[^ ]+'
                                      , 1, LEVEL), 3) AS param_key
                        FROM dual
                        CONNECT BY LEVEL <= regexp_count(regexp_replace(
                                                         p_cluster_host_info,
                                                         c_regex_pattern, ' '),
                                            '[^ ]+'))
                WHERE  param_key IS NOT NULL
                       AND NOT param_key LIKE '%ambari_db_rca%') LOOP
          PIPE ROW(r.param_key);
      END LOOP;
  END get_keys;
  FUNCTION get_value(p_cluster_host_info BLOB,
                     p_param_key         VARCHAR2)
  RETURN VARCHAR2
  AS
    v_param_value VARCHAR2(32767);
  BEGIN
      SELECT regexp_substr(utl_raw.Cast_to_varchar2(p_cluster_host_info), '"'
                                                                          ||
                   p_param_key
                                                                          ||
             '":\[["a-z0-9., ]{1,}]')
      INTO   v_param_value
      FROM   dual;

      SELECT substr(v_param_value, length(p_param_key) + 5,
                    dbms_lob.Getlength(v_param_value) - length(p_param_key) - 5)
      INTO   v_param_value
      FROM   dual;

      RETURN v_param_value;
  END get_value;
  
  FUNCTION compress_cluster_host_info (p_cluster_host_info BLOB)
  RETURN BLOB
  AS
    CURSOR cur1(
      p_param_name VARCHAR2) IS
      SELECT *
      FROM   (SELECT column_value
                            AS
                            param_name,
  compress_cluster_host_info_pkg.get_value(p_cluster_host_info, column_value) AS
  param_value
  FROM   TABLE(compress_cluster_host_info_pkg.get_keys((
                      utl_raw.cast_to_varchar2(p_cluster_host_info) )))) a
  WHERE  ( a.param_name = p_param_name
          OR ( p_param_name IS NULL
               AND a.param_name NOT IN ( 'all_hosts', 'all_ping_ports' ) ) );
  l_result                BLOB;
  l_raw                   RAW(32767);
  l_all_hosts             NESTED_VACHAR2_TBL_TYPE;
  l_all_ping_ports        NESTED_VACHAR2_TBL_TYPE;
  l_compressed_ping_ports NESTED_VACHAR2_TBL_TYPE;
  l_indexed               NESTED_VACHAR2_TBL_TYPE;
  BEGIN
    dbms_lob.createtemporary(l_result, FALSE);

    dbms_lob.OPEN(l_result, dbms_lob.lob_readwrite);

    FOR r IN cur1('all_hosts') LOOP
      l_all_hosts := split_to_array(r.param_value, ',');
    END LOOP;

    FOR r IN cur1('all_ping_ports') LOOP
      l_all_ping_ports := split_to_array(r.param_value, ',');

      dbms_output.put_line(r.param_value);
    END LOOP;

    l_compressed_ping_ports := to_mapped_indexed_array(l_all_ping_ports);

    l_raw := utl_raw.cast_to_raw('{');

    dbms_lob.writeappend(l_result, utl_raw.length(l_raw), l_raw);

    FOR r IN cur1(NULL) LOOP
      dbms_output.put_line(r.param_name);

      l_indexed := to_indexed_array(split_to_array(r.param_value, ','),
                 l_all_hosts);

      l_raw := utl_raw.cast_to_raw('"'
                                 || r.param_name
                                 || '":["'
                                 || nested_table_to_string(l_indexed, ',')
                                 || '"],');

      dbms_lob.writeappend(l_result, utl_raw.Length(l_raw), l_raw);
    END LOOP;

    l_raw := utl_raw.cast_to_raw('"all_hosts":['
                             || nested_table_to_string(l_all_hosts, ',')
                             || '],');

    dbms_lob.writeappend(l_result, utl_raw.length(l_raw), l_raw);

    l_raw := utl_raw.Cast_to_raw('"all_ping_ports":['
                             || Nested_table_to_string(
                                l_compressed_ping_ports,
                                ',')
                             || ']');

    dbms_lob.Writeappend(l_result, utl_raw.Length(l_raw), l_raw);

    l_raw := utl_raw.Cast_to_raw('}');

    dbms_lob.writeappend(l_result, utl_raw.Length(l_raw), l_raw);

    dbms_lob.CLOSE(l_result);

    RETURN l_result;
    
  END compress_cluster_host_info;

  FUNCTION split_to_array(p_string VARCHAR2,
                          p_sep    VARCHAR2)
  RETURN NESTED_VACHAR2_TBL_TYPE
  AS
    l_result NESTED_VACHAR2_TBL_TYPE;
  BEGIN
      SELECT Regexp_substr(p_string, '[^,]+', 1, LEVEL)
      BULK   COLLECT INTO l_result
      FROM   dual
      CONNECT BY Instr(p_string, p_sep, 1, LEVEL - 1) > 0;

      RETURN l_result;
  END split_to_array;
  
  FUNCTION index_of(p_arr  NESTED_VACHAR2_TBL_TYPE,
                    p_item VARCHAR2)
  RETURN INTEGER
  AS
  BEGIN
      FOR i IN p_arr.FIRST..p_arr.LAST LOOP
          IF p_arr(i) = p_item THEN
            RETURN i - 1;
          END IF;
      END LOOP;
  END index_of;
  
  FUNCTION to_indexed_array(p_arr      NESTED_VACHAR2_TBL_TYPE,
                            p_dict_arr NESTED_VACHAR2_TBL_TYPE)
  RETURN NESTED_VACHAR2_TBL_TYPE
  AS
    l_index  INTEGER;
    l_result NESTED_VACHAR2_TBL_TYPE := Nested_vachar2_tbl_type();
  BEGIN
      FOR i IN p_arr.first..p_arr.last LOOP
          l_index := Index_of(p_dict_arr, P_arr(i));

          l_result.Extend(1);

          L_result(i) := l_index;
      END LOOP;

      RETURN l_result;
  END to_indexed_array;
  FUNCTION to_mapped_indexed_array(p_arr NESTED_VACHAR2_TBL_TYPE)
  RETURN NESTED_VACHAR2_TBL_TYPE
  AS
    v_result       NESTED_VACHAR2_TBL_TYPE := Nested_vachar2_tbl_type();
    v_curr_indexes VARCHAR2(32767);
    v_prev_val     VARCHAR2(32767);
  BEGIN
      FOR i IN p_arr.first..p_arr.last LOOP
          IF P_arr(i) <> v_prev_val THEN
            v_result.extend(1);

            V_result(v_result.last) := '"'
                                       || v_curr_indexes
                                       || '"';

            v_curr_indexes := NULL;
          END IF;

          IF v_curr_indexes IS NULL THEN
            v_curr_indexes := substr(p_arr(i), 2, length(p_arr(i)) - 2)
                              || ':'
                              || to_char(i - 1);
          ELSE
            v_curr_indexes := v_curr_indexes
                              || ','
                              || to_char(i - 1);
          END IF;

          v_prev_val := p_arr(i);
      END LOOP;

      IF v_curr_indexes IS NOT NULL THEN
        v_result.extend(1);

        V_result(v_result.LAST) := '"' || v_curr_indexes || '"';
      END IF;

      RETURN v_result;
  END to_mapped_indexed_array;
  FUNCTION nested_table_to_string(p_arr NESTED_VACHAR2_TBL_TYPE,
                                  p_sep VARCHAR2)
  RETURN VARCHAR2
  AS
    v_result VARCHAR2(32767);
  BEGIN
      v_result := p_arr(1);

      FOR i IN p_arr.FIRST + 1 ..p_arr.LAST LOOP
          v_result := v_result
                      || ','
                      || p_arr(i);
      END LOOP;

      RETURN v_result;
  END nested_table_to_string;
END compress_cluster_host_info_pkg; 
/

--Compress cluster host info
UPDATE stage s
SET s.cluster_host_info = compress_cluster_host_info_pkg.compress_cluster_host_info(s.cluster_host_info)
WHERE dbms_lob.instr(cluster_host_info, utl_raw.cast_to_raw('ambari_db_rca'), 1, 1) > 0;

--Drop compression package
DROP PACKAGE compress_cluster_host_info_pkg;


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

commit;

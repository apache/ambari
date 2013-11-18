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

-- DML
--Upgrade version to current
UPDATE metainfo SET "metainfo_key" = 'version', "metainfo_value" = '${ambariVersion}';

INSERT INTO ambari_sequences(sequence_name, value) values ('configgroup_id_seq', 1);

-- drop deprecated tables componentconfigmapping and hostcomponentconfigmapping
-- not required after Config Group implementation
--DROP TABLE componentconfigmapping;
--DROP TABLE hostcomponentconfigmapping;

-- required for Config Group implementation
CREATE TABLE ambari.configgroup (group_id BIGINT, cluster_id BIGINT NOT NULL, group_name VARCHAR2(255) NOT NULL, tag VARCHAR2(1024) NOT NULL, description VARCHAR2(1024), create_timestamp BIGINT NOT NULL, PRIMARY KEY(group_id), UNIQUE(group_name));
CREATE TABLE ambari.confgroupclusterconfigmapping (config_group_id BIGINT NOT NULL, cluster_id BIGINT NOT NULL, config_type VARCHAR2(255) NOT NULL, version_tag VARCHAR2(255) NOT NULL, user_name VARCHAR2(255) DEFAULT '_db', create_timestamp BIGINT NOT NULL, PRIMARY KEY(config_group_id, cluster_id, config_type));
CREATE TABLE ambari.configgrouphostmapping (config_group_id BIGINT NOT NULL, host_name VARCHAR2(255) NOT NULL, PRIMARY KEY(config_group_id, host_name));
CREATE TABLE ambari.action (action_name VARCHAR2(255) NOT NULL, action_type VARCHAR2(255) NOT NULL, inputs VARCHAR2(1024), target_service VARCHAR2(255), target_component VARCHAR2(255), default_timeout NUMBER(10) NOT NULL, description VARCHAR2(1024), target_type VARCHAR2(255), PRIMARY KEY (action_name));


ALTER TABLE ambari.configgroup ADD CONSTRAINT FK_configgroup_cluster_id FOREIGN KEY (cluster_id) REFERENCES ambari.clusters (cluster_id);
ALTER TABLE ambari.confgroupclusterconfigmapping ADD CONSTRAINT FK_confgroupclusterconfigmapping_config_tag FOREIGN KEY (version_tag, config_type, cluster_id) REFERENCES ambari.clusterconfig (version_tag, type_name, cluster_id);
ALTER TABLE ambari.confgroupclusterconfigmapping ADD CONSTRAINT FK_confgroupclusterconfigmapping_group_id FOREIGN KEY (config_group_id) REFERENCES ambari.configgroup (group_id);
ALTER TABLE ambari.configgrouphostmapping ADD CONSTRAINT FK_configgrouphostmapping_configgroup_id FOREIGN KEY (config_group_id) REFERENCES ambari.configgroup (group_id);
ALTER TABLE ambari.configgrouphostmapping ADD CONSTRAINT FK_configgrouphostmapping_host_name FOREIGN KEY (host_name) REFERENCES ambari.hosts (host_name);


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

ALTER TABLE ambari.hosts DROP COLUMN disks_info;

commit;

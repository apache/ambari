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

ALTER TABLE ambari.configgroup ADD CONSTRAINT FK_configgroup_cluster_id FOREIGN KEY (cluster_id) REFERENCES ambari.clusters (cluster_id);
ALTER TABLE ambari.confgroupclusterconfigmapping ADD CONSTRAINT FK_confgroupclusterconfigmapping_config_tag FOREIGN KEY (version_tag, config_type, cluster_id) REFERENCES ambari.clusterconfig (version_tag, type_name, cluster_id);
ALTER TABLE ambari.confgroupclusterconfigmapping ADD CONSTRAINT FK_confgroupclusterconfigmapping_group_id FOREIGN KEY (config_group_id) REFERENCES ambari.configgroup (group_id);
ALTER TABLE ambari.configgrouphostmapping ADD CONSTRAINT FK_configgrouphostmapping_configgroup_id FOREIGN KEY (config_group_id) REFERENCES ambari.configgroup (group_id);
ALTER TABLE ambari.configgrouphostmapping ADD CONSTRAINT FK_configgrouphostmapping_host_name FOREIGN KEY (host_name) REFERENCES ambari.hosts (host_name);

commit;

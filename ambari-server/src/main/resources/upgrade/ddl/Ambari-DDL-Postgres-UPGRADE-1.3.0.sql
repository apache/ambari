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

-- required for custom action
CREATE TABLE ambari.action (action_name VARCHAR(255) NOT NULL, action_type VARCHAR(32) NOT NULL, inputs VARCHAR(1000),
target_service VARCHAR(255), target_component VARCHAR(255), default_timeout SMALLINT NOT NULL, description VARCHAR(1000), target_type VARCHAR(32), PRIMARY KEY (action_name));
GRANT ALL PRIVILEGES ON TABLE ambari.action TO :username;

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

ALTER TABLE ambari.hosts DROP COLUMN disks_info;

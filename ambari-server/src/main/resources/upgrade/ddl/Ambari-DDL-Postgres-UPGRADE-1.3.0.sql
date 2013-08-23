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

-- Upgrade from 1.2.0
ALTER TABLE ambari.hosts
  ADD COLUMN ph_cpu_count INTEGER,
  ALTER COLUMN disks_info TYPE VARCHAR(10000);

-- Upgrade to 1.3.0
ALTER TABLE ambari.clusterstate
  ADD COLUMN current_stack_version VARCHAR(255) NOT NULL;

ALTER TABLE ambari.hostconfigmapping
  ADD COLUMN user_name VARCHAR(255) NOT NULL DEFAULT '_db';
ALTER TABLE ambari.clusterconfigmapping
  ADD COLUMN user_name VARCHAR(255) NOT NULL DEFAULT '_db';

CREATE TABLE ambari.hostconfigmapping (cluster_id bigint NOT NULL, host_name VARCHAR(255) NOT NULL, type_name VARCHAR(255) NOT NULL, version_tag VARCHAR(255) NOT NULL, service_name VARCHAR(255), create_timestamp BIGINT NOT NULL, selected INTEGER NOT NULL DEFAULT 0, PRIMARY KEY (cluster_id, host_name, type_name, create_timestamp));
GRANT ALL PRIVILEGES ON TABLE ambari.hostconfigmapping TO :username;
ALTER TABLE ambari.hostconfigmapping ADD CONSTRAINT FK_hostconfigmapping_cluster_id FOREIGN KEY (cluster_id) REFERENCES ambari.clusters (cluster_id);
ALTER TABLE ambari.hostconfigmapping ADD CONSTRAINT FK_hostconfigmapping_host_name FOREIGN KEY (host_name) REFERENCES ambari.hosts (host_name);

ALTER ROLE :username SET search_path to 'ambari';

ALTER SEQUENCE ambari.host_role_command_task_id_seq INCREMENT BY 50;
SELECT nextval('ambari.host_role_command_task_id_seq');

ALTER TABLE ambari.stage ADD COLUMN request_context VARCHAR(255);SELECT nextval('ambari.host_role_command_task_id_seq');


-- portability changes for MySQL/Oracle support
alter table ambari.hostcomponentdesiredconfigmapping rename to hcdesiredconfigmapping;
alter table ambari.users alter column user_id drop default;
alter table ambari.users alter column ldap_user type INTEGER using case when ldap_user=true then 1 else 0 END;

CREATE TABLE ambari.ambari_sequences (sequence_name VARCHAR(255) PRIMARY KEY, "value" BIGINT NOT NULL);
GRANT ALL PRIVILEGES ON TABLE ambari.ambari_sequences TO :username;

insert into ambari.ambari_sequences(sequence_name, "value")
  select 'cluster_id_seq', nextval('ambari.clusters_cluster_id_seq')
  union all
  select 'user_id_seq', nextval('ambari.users_user_id_seq')
  union all
  select 'host_role_command_id_seq', nextval('ambari.host_role_command_task_id_seq');

drop sequence ambari.host_role_command_task_id_seq;
drop sequence ambari.users_user_id_seq;
drop sequence ambari.clusters_cluster_id_seq;

CREATE LANGUAGE plpgsql;

CREATE TABLE ambari.metainfo (metainfo_key VARCHAR(255), metainfo_value VARCHAR, PRIMARY KEY(metainfo_key));
INSERT INTO ambari.metainfo (metainfo_key, metainfo_value) select 'version', '${ambariVersion}';
UPDATE ambari.metainfo SET metainfo_value = '${ambariVersion}' WHERE metainfo_key = 'version';
GRANT ALL PRIVILEGES ON TABLE ambari.metainfo TO :username;

UPDATE ambari.hostcomponentstate SET current_state = 'INSTALLED' WHERE current_state like 'STOP_FAILED';
UPDATE ambari.hostcomponentstate SET current_state = 'INSTALLED' WHERE current_state like 'START_FAILED';

-- service to cluster level config mappings move. idempotent update

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

SELECT update_clusterconfigmapping();
GRANT ALL PRIVILEGES ON TABLE ambari.clusterconfigmapping TO :username;


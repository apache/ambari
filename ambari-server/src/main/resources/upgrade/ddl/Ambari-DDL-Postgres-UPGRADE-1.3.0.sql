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
\connect ambari;

ALTER TABLE ambari.clusterstate
  ADD COLUMN current_stack_version VARCHAR(255) NOT NULL;

CREATE TABLE ambari.clusterconfigmapping (cluster_id bigint NOT NULL, type_name VARCHAR(255) NOT NULL, version_tag VARCHAR(255) NOT NULL, create_timestamp BIGINT NOT NULL, selected INTEGER NOT NULL DEFAULT 0, PRIMARY KEY (cluster_id, type_name, create_timestamp));
GRANT ALL PRIVILEGES ON TABLE ambari.clusterconfigmapping TO :username;
ALTER TABLE ambari.clusterconfigmapping ADD CONSTRAINT FK_clusterconfigmapping_cluster_id FOREIGN KEY (cluster_id) REFERENCES ambari.clusters (cluster_id);

CREATE TABLE ambari.hostconfigmapping (cluster_id bigint NOT NULL, host_name VARCHAR(255) NOT NULL, type_name VARCHAR(255) NOT NULL, version_tag VARCHAR(255) NOT NULL, service_name VARCHAR(255), create_timestamp BIGINT NOT NULL, selected INTEGER NOT NULL DEFAULT 0, PRIMARY KEY (cluster_id, host_name, type_name, create_timestamp));
GRANT ALL PRIVILEGES ON TABLE ambari.hostconfigmapping TO :username;
ALTER TABLE ambari.hostconfigmapping ADD CONSTRAINT FK_hostconfigmapping_cluster_id FOREIGN KEY (cluster_id) REFERENCES ambari.clusters (cluster_id);
ALTER TABLE ambari.hostconfigmapping ADD CONSTRAINT FK_hostconfigmapping_host_name FOREIGN KEY (host_name) REFERENCES ambari.hosts (host_name);

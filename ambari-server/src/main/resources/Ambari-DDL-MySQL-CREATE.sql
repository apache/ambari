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
-- DROP DATABASE IF EXISTS `ambari`;
-- DROP USER `ambari`;

delimiter ;

# CREATE DATABASE `ambari` /*!40100 DEFAULT CHARACTER SET utf8 */;
#
# CREATE USER 'ambari' IDENTIFIED BY 'bigdata';

# USE @schema;

CREATE TABLE clusters (cluster_id BIGINT NOT NULL, cluster_info VARCHAR(255) NOT NULL, cluster_name VARCHAR(100) NOT NULL UNIQUE, desired_cluster_state VARCHAR(255) NOT NULL, desired_stack_version VARCHAR(255) NOT NULL, PRIMARY KEY (cluster_id));
CREATE TABLE clusterconfig (version_tag VARCHAR(255) NOT NULL, type_name VARCHAR(255) NOT NULL, cluster_id BIGINT NOT NULL, config_data LONGTEXT NOT NULL, create_timestamp BIGINT NOT NULL, PRIMARY KEY (version_tag, type_name, cluster_id));
CREATE TABLE clusterservices (service_name VARCHAR(255) NOT NULL, cluster_id BIGINT NOT NULL, service_enabled INTEGER NOT NULL, PRIMARY KEY (service_name, cluster_id));
CREATE TABLE clusterstate (cluster_id BIGINT NOT NULL, current_cluster_state VARCHAR(255) NOT NULL, current_stack_version VARCHAR(255) NOT NULL, PRIMARY KEY (cluster_id));
CREATE TABLE hostcomponentdesiredstate (cluster_id BIGINT NOT NULL, component_name VARCHAR(255) NOT NULL, desired_stack_version VARCHAR(255) NOT NULL, desired_state VARCHAR(255) NOT NULL, host_name VARCHAR(255) NOT NULL, service_name VARCHAR(255) NOT NULL, PRIMARY KEY (cluster_id, component_name, host_name, service_name));
CREATE TABLE hostcomponentstate (cluster_id BIGINT NOT NULL, component_name VARCHAR(255) NOT NULL, current_stack_version VARCHAR(255) NOT NULL, current_state VARCHAR(255) NOT NULL, host_name VARCHAR(255) NOT NULL, service_name VARCHAR(255) NOT NULL, PRIMARY KEY (cluster_id, component_name, host_name, service_name));
CREATE TABLE hosts (host_name VARCHAR(255) NOT NULL, cpu_count INTEGER NOT NULL, cpu_info VARCHAR(255) NOT NULL, discovery_status VARCHAR(2000) NOT NULL, host_attributes LONGTEXT, ipv4 VARCHAR(255), ipv6 VARCHAR(255), last_registration_time BIGINT NOT NULL, os_arch VARCHAR(255) NOT NULL, os_info VARCHAR(1000) NOT NULL, os_type VARCHAR(255) NOT NULL, ph_cpu_count INTEGER NOT NULL, public_host_name VARCHAR(255), rack_info VARCHAR(255) NOT NULL, total_mem BIGINT NOT NULL, PRIMARY KEY (host_name));
CREATE TABLE hoststate (agent_version VARCHAR(255) NOT NULL, available_mem BIGINT NOT NULL, current_state VARCHAR(255) NOT NULL, health_status VARCHAR(255), host_name VARCHAR(255) NOT NULL, time_in_state BIGINT NOT NULL, PRIMARY KEY (host_name));
CREATE TABLE servicecomponentdesiredstate (component_name VARCHAR(255) NOT NULL, cluster_id BIGINT NOT NULL, desired_stack_version VARCHAR(255) NOT NULL, desired_state VARCHAR(255) NOT NULL, service_name VARCHAR(255) NOT NULL, PRIMARY KEY (component_name, cluster_id, service_name));
CREATE TABLE servicedesiredstate (cluster_id BIGINT NOT NULL, desired_host_role_mapping INTEGER NOT NULL, desired_stack_version VARCHAR(255) NOT NULL, desired_state VARCHAR(255) NOT NULL, service_name VARCHAR(255) NOT NULL, PRIMARY KEY (cluster_id, service_name));
CREATE TABLE roles (role_name VARCHAR(255) NOT NULL, PRIMARY KEY (role_name));
CREATE TABLE users (user_id INTEGER NOT NULL, create_time TIMESTAMP DEFAULT NOW(), ldap_user INTEGER NOT NULL DEFAULT 0, user_name VARCHAR(255), user_password VARCHAR(255), PRIMARY KEY (user_id));
CREATE TABLE execution_command (task_id BIGINT NOT NULL, command LONGBLOB, PRIMARY KEY (task_id));
CREATE TABLE host_role_command (task_id BIGINT NOT NULL, attempt_count SMALLINT NOT NULL, event LONGTEXT NOT NULL, exitcode INTEGER NOT NULL, host_name VARCHAR(255) NOT NULL, last_attempt_time BIGINT NOT NULL, request_id BIGINT NOT NULL, role VARCHAR(255), role_command VARCHAR(255), stage_id BIGINT NOT NULL, start_time BIGINT NOT NULL, status VARCHAR(255), std_error LONGBLOB, std_out LONGBLOB, PRIMARY KEY (task_id));
CREATE TABLE role_success_criteria (role VARCHAR(255) NOT NULL, request_id BIGINT NOT NULL, stage_id BIGINT NOT NULL, success_factor DOUBLE NOT NULL, PRIMARY KEY (role, request_id, stage_id));
CREATE TABLE stage (stage_id BIGINT NOT NULL, request_id BIGINT NOT NULL, cluster_id BIGINT, log_info VARCHAR(255) NOT NULL, request_context VARCHAR(255), cluster_host_info LONGBLOB, PRIMARY KEY (stage_id, request_id));
CREATE TABLE key_value_store (`key` VARCHAR(255) NOT NULL, `value` LONGTEXT, PRIMARY KEY (`key`));
CREATE TABLE clusterconfigmapping (type_name VARCHAR(255) NOT NULL, create_timestamp BIGINT NOT NULL, cluster_id BIGINT NOT NULL, selected INTEGER NOT NULL, version_tag VARCHAR(255) NOT NULL, user_name VARCHAR(255) NOT NULL DEFAULT '_db', PRIMARY KEY (type_name, create_timestamp, cluster_id));
CREATE TABLE hostconfigmapping (create_timestamp BIGINT NOT NULL, host_name VARCHAR(255) NOT NULL, cluster_id BIGINT NOT NULL, type_name VARCHAR(255) NOT NULL, selected INTEGER NOT NULL, service_name VARCHAR(255), version_tag VARCHAR(255) NOT NULL, user_name VARCHAR(255) NOT NULL DEFAULT '_db', PRIMARY KEY (create_timestamp, host_name, cluster_id, type_name));
CREATE TABLE metainfo (`metainfo_key` VARCHAR(255) NOT NULL, `metainfo_value` LONGTEXT, PRIMARY KEY (`metainfo_key`));
CREATE TABLE ClusterHostMapping (cluster_id BIGINT NOT NULL, host_name VARCHAR(255) NOT NULL, PRIMARY KEY (cluster_id, host_name));
CREATE TABLE user_roles (role_name VARCHAR(255) NOT NULL, user_id INTEGER NOT NULL, PRIMARY KEY (role_name, user_id));
CREATE TABLE ambari_sequences (sequence_name VARCHAR(50) NOT NULL, value DECIMAL(38), PRIMARY KEY (sequence_name));


ALTER TABLE users ADD CONSTRAINT UNQ_users_0 UNIQUE (user_name, ldap_user);
ALTER TABLE clusterconfig ADD CONSTRAINT FK_clusterconfig_cluster_id FOREIGN KEY (cluster_id) REFERENCES clusters (cluster_id);
ALTER TABLE clusterservices ADD CONSTRAINT FK_clusterservices_cluster_id FOREIGN KEY (cluster_id) REFERENCES clusters (cluster_id);
ALTER TABLE clusterstate ADD CONSTRAINT FK_clusterstate_cluster_id FOREIGN KEY (cluster_id) REFERENCES clusters (cluster_id);
ALTER TABLE hostcomponentdesiredstate ADD CONSTRAINT FK_hostcomponentdesiredstate_host_name FOREIGN KEY (host_name) REFERENCES hosts (host_name);
ALTER TABLE hostcomponentdesiredstate ADD CONSTRAINT FK_hostcomponentdesiredstate_component_name FOREIGN KEY (component_name, cluster_id, service_name) REFERENCES servicecomponentdesiredstate (component_name, cluster_id, service_name);
ALTER TABLE hostcomponentstate ADD CONSTRAINT FK_hostcomponentstate_component_name FOREIGN KEY (component_name, cluster_id, service_name) REFERENCES servicecomponentdesiredstate (component_name, cluster_id, service_name);
ALTER TABLE hostcomponentstate ADD CONSTRAINT FK_hostcomponentstate_host_name FOREIGN KEY (host_name) REFERENCES hosts (host_name);
ALTER TABLE hoststate ADD CONSTRAINT FK_hoststate_host_name FOREIGN KEY (host_name) REFERENCES hosts (host_name);
ALTER TABLE servicecomponentdesiredstate ADD CONSTRAINT FK_servicecomponentdesiredstate_service_name FOREIGN KEY (service_name, cluster_id) REFERENCES clusterservices (service_name, cluster_id);
ALTER TABLE servicedesiredstate ADD CONSTRAINT FK_servicedesiredstate_service_name FOREIGN KEY (service_name, cluster_id) REFERENCES clusterservices (service_name, cluster_id);
ALTER TABLE execution_command ADD CONSTRAINT FK_execution_command_task_id FOREIGN KEY (task_id) REFERENCES host_role_command (task_id);
ALTER TABLE host_role_command ADD CONSTRAINT FK_host_role_command_stage_id FOREIGN KEY (stage_id, request_id) REFERENCES stage (stage_id, request_id);
ALTER TABLE host_role_command ADD CONSTRAINT FK_host_role_command_host_name FOREIGN KEY (host_name) REFERENCES hosts (host_name);
ALTER TABLE role_success_criteria ADD CONSTRAINT FK_role_success_criteria_stage_id FOREIGN KEY (stage_id, request_id) REFERENCES stage (stage_id, request_id);
ALTER TABLE stage ADD CONSTRAINT FK_stage_cluster_id FOREIGN KEY (cluster_id) REFERENCES clusters (cluster_id);
ALTER TABLE clusterconfigmapping ADD CONSTRAINT FK_clusterconfigmapping_cluster_id FOREIGN KEY (cluster_id) REFERENCES clusters (cluster_id);
ALTER TABLE ClusterHostMapping ADD CONSTRAINT FK_ClusterHostMapping_host_name FOREIGN KEY (host_name) REFERENCES hosts (host_name);
ALTER TABLE ClusterHostMapping ADD CONSTRAINT FK_ClusterHostMapping_cluster_id FOREIGN KEY (cluster_id) REFERENCES clusters (cluster_id);
ALTER TABLE user_roles ADD CONSTRAINT FK_user_roles_user_id FOREIGN KEY (user_id) REFERENCES users (user_id);
ALTER TABLE user_roles ADD CONSTRAINT FK_user_roles_role_name FOREIGN KEY (role_name) REFERENCES roles (role_name);


INSERT INTO ambari_sequences(sequence_name, value) values ('cluster_id_seq', 0);
INSERT INTO ambari_sequences(sequence_name, value) values ('host_role_command_id_seq', 0);
INSERT INTO ambari_sequences(sequence_name, value) values ('user_id_seq', 1);

insert into ambari.roles(role_name)
  select 'admin'
  union all
  select 'user';

insert into ambari.users(user_id, user_name, user_password)
  select 1,'admin','538916f8943ec225d97a9a86a2c6ec0818c1cd400e09e03b660fdaaec4af29ddbb6f2b1033b81b00';

insert into ambari.user_roles(role_name, user_id)
  select 'admin',1;

insert into ambari.metainfo(`metainfo_key`, `metainfo_value`)
  select 'version','${ambariVersion}';



CREATE TABLE workflow (
  workflowId VARCHAR(255), workflowName TEXT,
  parentWorkflowId VARCHAR(255),
  workflowContext TEXT, userName TEXT,
  startTime BIGINT, lastUpdateTime BIGINT,
  numJobsTotal INTEGER, numJobsCompleted INTEGER,
  inputBytes BIGINT, outputBytes BIGINT,
  duration BIGINT,
  PRIMARY KEY (workflowId),
  FOREIGN KEY (parentWorkflowId) REFERENCES workflow(workflowId)
);

CREATE TABLE job (
  jobId VARCHAR(255), workflowId VARCHAR(255), jobName TEXT, workflowEntityName TEXT,
  userName TEXT, queue TEXT, acls TEXT, confPath TEXT,
  submitTime BIGINT, launchTime BIGINT, finishTime BIGINT,
  maps INTEGER, reduces INTEGER, status TEXT, priority TEXT,
  finishedMaps INTEGER, finishedReduces INTEGER,
  failedMaps INTEGER, failedReduces INTEGER,
  mapsRuntime BIGINT, reducesRuntime BIGINT,
  mapCounters TEXT, reduceCounters TEXT, jobCounters TEXT,
  inputBytes BIGINT, outputBytes BIGINT,
  PRIMARY KEY(jobId),
  FOREIGN KEY(workflowId) REFERENCES workflow(workflowId)
);

CREATE TABLE task (
  taskId VARCHAR(255), jobId VARCHAR(255), taskType TEXT, splits TEXT,
  startTime BIGINT, finishTime BIGINT, status TEXT, error TEXT, counters TEXT,
  failedAttempt TEXT,
  PRIMARY KEY(taskId),
  FOREIGN KEY(jobId) REFERENCES job(jobId)
);

CREATE TABLE taskAttempt (
  taskAttemptId VARCHAR(255), taskId VARCHAR(255), jobId VARCHAR(255), taskType TEXT, taskTracker TEXT,
  startTime BIGINT, finishTime BIGINT,
  mapFinishTime BIGINT, shuffleFinishTime BIGINT, sortFinishTime BIGINT,
  locality TEXT, avataar TEXT,
  status TEXT, error TEXT, counters TEXT,
  inputBytes BIGINT, outputBytes BIGINT,
  PRIMARY KEY(taskAttemptId),
  FOREIGN KEY(jobId) REFERENCES job(jobId),
  FOREIGN KEY(taskId) REFERENCES task(taskId)
);

CREATE TABLE hdfsEvent (
  timestamp BIGINT,
  userName TEXT,
  clientIP TEXT,
  operation TEXT,
  srcPath TEXT,
  dstPath TEXT,
  permissions TEXT
);

CREATE TABLE mapreduceEvent (
  timestamp BIGINT,
  userName TEXT,
  clientIP TEXT,
  operation TEXT,
  target TEXT,
  result TEXT,
  description TEXT,
  permissions TEXT
);

CREATE TABLE clusterEvent (
  timestamp BIGINT,
  service TEXT, status TEXT,
  error TEXT, data TEXT ,
  host TEXT, rack TEXT
);





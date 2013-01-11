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
CREATE DATABASE ambari;

\connect ambari;

CREATE ROLE :username LOGIN ENCRYPTED PASSWORD :password;

CREATE SCHEMA ambari
  AUTHORIZATION :username;

CREATE TABLE ambari.clusters (cluster_id BIGINT NOT NULL, cluster_info VARCHAR(255) NOT NULL, cluster_name VARCHAR(100) NOT NULL UNIQUE, desired_cluster_state VARCHAR(255) NOT NULL, desired_stack_version VARCHAR(255) NOT NULL, PRIMARY KEY (cluster_id));

GRANT ALL PRIVILEGES ON TABLE ambari.clusters TO :username;

CREATE TABLE ambari.clusterconfig (version_tag VARCHAR(255) NOT NULL, type_name VARCHAR(255) NOT NULL, cluster_id BIGINT NOT NULL, config_data VARCHAR(32000) NOT NULL, create_timestamp BIGINT NOT NULL, PRIMARY KEY (cluster_id, type_name, version_tag));

GRANT ALL PRIVILEGES ON TABLE ambari.clusterconfig TO :username;

CREATE TABLE ambari.clusterservices (service_name VARCHAR(255) NOT NULL, cluster_id BIGINT NOT NULL, service_enabled INTEGER NOT NULL, PRIMARY KEY (service_name, cluster_id));

GRANT ALL PRIVILEGES ON TABLE ambari.clusterservices TO :username;

CREATE TABLE ambari.clusterstate (cluster_id BIGINT NOT NULL, current_cluster_state VARCHAR(255) NOT NULL, PRIMARY KEY (cluster_id));

GRANT ALL PRIVILEGES ON TABLE ambari.clusterstate TO :username;

CREATE TABLE ambari.componentconfigmapping (cluster_id BIGINT NOT NULL, component_name VARCHAR(255) NOT NULL, service_name VARCHAR(255) NOT NULL, config_type VARCHAR(255) NOT NULL, timestamp BIGINT NOT NULL, config_tag VARCHAR(255) NOT NULL, PRIMARY KEY (cluster_id, component_name, service_name, config_type));

GRANT ALL PRIVILEGES ON TABLE ambari.componentconfigmapping TO :username;

CREATE TABLE ambari.hostcomponentconfigmapping (cluster_id BIGINT NOT NULL, component_name VARCHAR(255) NOT NULL, host_name VARCHAR(255) NOT NULL, service_name VARCHAR(255) NOT NULL, config_type VARCHAR(255) NOT NULL, timestamp BIGINT NOT NULL, config_tag VARCHAR(255) NOT NULL, PRIMARY KEY (cluster_id, component_name, host_name, service_name, config_type));

GRANT ALL PRIVILEGES ON TABLE ambari.hostcomponentconfigmapping TO :username;

CREATE TABLE ambari.hostcomponentdesiredconfigmapping (cluster_id BIGINT NOT NULL, component_name VARCHAR(255) NOT NULL, host_name VARCHAR(255) NOT NULL, service_name VARCHAR(255) NOT NULL, config_type VARCHAR(255) NOT NULL, timestamp BIGINT NOT NULL, config_tag VARCHAR(255) NOT NULL, PRIMARY KEY (cluster_id, component_name, host_name, service_name, config_type));

GRANT ALL PRIVILEGES ON TABLE ambari.hostcomponentdesiredconfigmapping TO :username;

CREATE TABLE ambari.hostcomponentdesiredstate (cluster_id BIGINT NOT NULL, component_name VARCHAR(255) NOT NULL, desired_stack_version VARCHAR(255) NOT NULL, desired_state VARCHAR(255) NOT NULL, host_name VARCHAR(255) NOT NULL, service_name VARCHAR(255) NOT NULL, PRIMARY KEY (cluster_id, component_name, host_name, service_name));

GRANT ALL PRIVILEGES ON TABLE ambari.hostcomponentdesiredstate TO :username;

CREATE TABLE ambari.hostcomponentstate (cluster_id BIGINT NOT NULL, component_name VARCHAR(255) NOT NULL, current_stack_version VARCHAR(255) NOT NULL, current_state VARCHAR(255) NOT NULL, host_name VARCHAR(255) NOT NULL, service_name VARCHAR(255) NOT NULL, PRIMARY KEY (cluster_id, component_name, host_name, service_name));

GRANT ALL PRIVILEGES ON TABLE ambari.hostcomponentstate TO :username;

CREATE TABLE ambari.hosts (host_name VARCHAR(255) NOT NULL, cpu_count INTEGER NOT NULL, cpu_info VARCHAR(255) NOT NULL, discovery_status VARCHAR(2000) NOT NULL, disks_info VARCHAR(2000) NOT NULL, host_attributes VARCHAR(20000) NOT NULL, ipv4 VARCHAR(255), ipv6 VARCHAR(255), public_host_name VARCHAR(255), last_registration_time BIGINT NOT NULL, os_arch VARCHAR(255) NOT NULL, os_info VARCHAR(1000) NOT NULL, os_type VARCHAR(255) NOT NULL, rack_info VARCHAR(255) NOT NULL, total_mem BIGINT NOT NULL, PRIMARY KEY (host_name));

GRANT ALL PRIVILEGES ON TABLE ambari.hosts TO :username;

CREATE TABLE ambari.hoststate (agent_version VARCHAR(255) NOT NULL, available_mem BIGINT NOT NULL, current_state VARCHAR(255) NOT NULL, health_status VARCHAR(255), host_name VARCHAR(255) NOT NULL, time_in_state BIGINT NOT NULL,  PRIMARY KEY (host_name));

GRANT ALL PRIVILEGES ON TABLE ambari.hoststate TO :username;

CREATE TABLE ambari.servicecomponentdesiredstate (component_name VARCHAR(255) NOT NULL, cluster_id BIGINT NOT NULL, desired_stack_version VARCHAR(255) NOT NULL, desired_state VARCHAR(255) NOT NULL, service_name VARCHAR(255) NOT NULL, PRIMARY KEY (component_name, cluster_id, service_name));

GRANT ALL PRIVILEGES ON TABLE ambari.servicecomponentdesiredstate TO :username;

CREATE TABLE ambari.serviceconfigmapping (cluster_id BIGINT NOT NULL, service_name VARCHAR(255) NOT NULL, config_type VARCHAR(255) NOT NULL, timestamp BIGINT NOT NULL, config_tag VARCHAR(255) NOT NULL, PRIMARY KEY (cluster_id, service_name, config_type));

GRANT ALL PRIVILEGES ON TABLE ambari.serviceconfigmapping TO :username;

CREATE TABLE ambari.servicedesiredstate (cluster_id BIGINT NOT NULL, desired_host_role_mapping INTEGER NOT NULL, desired_stack_version VARCHAR(255) NOT NULL, desired_state VARCHAR(255) NOT NULL, service_name VARCHAR(255) NOT NULL, PRIMARY KEY (cluster_id, service_name));

GRANT ALL PRIVILEGES ON TABLE ambari.servicedesiredstate TO :username;

CREATE TABLE ambari.roles (role_name VARCHAR(255) NOT NULL, PRIMARY KEY (role_name));

GRANT ALL PRIVILEGES ON TABLE ambari.roles TO :username;

CREATE TABLE ambari.users (user_id SERIAL, ldap_user BOOLEAN NOT NULL, user_name VARCHAR(255) NOT NULL, create_time TIMESTAMP DEFAULT NOW(), user_password VARCHAR(255), PRIMARY KEY (user_id), UNIQUE (ldap_user, user_name));
GRANT ALL PRIVILEGES ON TABLE ambari.users TO :username;

CREATE TABLE ambari.execution_command (command bytea, task_id BIGINT NOT NULL, PRIMARY KEY (task_id));
GRANT ALL PRIVILEGES ON TABLE ambari.execution_command TO :username;

CREATE TABLE ambari.host_role_command (task_id BIGINT NOT NULL, attempt_count SMALLINT NOT NULL, event VARCHAR(32000) NOT NULL, exitcode INTEGER NOT NULL, host_name VARCHAR(255) NOT NULL, last_attempt_time BIGINT NOT NULL, request_id BIGINT NOT NULL, role VARCHAR(255), stage_id BIGINT NOT NULL, start_time BIGINT NOT NULL, status VARCHAR(255), std_error bytea, std_out bytea, role_command VARCHAR(255), PRIMARY KEY (task_id));
GRANT ALL PRIVILEGES ON TABLE ambari.host_role_command TO :username;

CREATE TABLE ambari.role_success_criteria (role VARCHAR(255) NOT NULL, request_id BIGINT NOT NULL, stage_id BIGINT NOT NULL, success_factor FLOAT NOT NULL, PRIMARY KEY (role, request_id, stage_id));
GRANT ALL PRIVILEGES ON TABLE ambari.role_success_criteria TO :username;

CREATE TABLE ambari.stage (stage_id BIGINT NOT NULL, request_id BIGINT NOT NULL, cluster_id BIGINT NOT NULL, log_info VARCHAR(255) NOT NULL, PRIMARY KEY (stage_id, request_id));
GRANT ALL PRIVILEGES ON TABLE ambari.stage TO :username;

CREATE TABLE ambari.ClusterHostMapping (cluster_id BIGINT NOT NULL, host_name VARCHAR(255) NOT NULL, PRIMARY KEY (cluster_id, host_name));
GRANT ALL PRIVILEGES ON TABLE ambari.ClusterHostMapping TO :username;

CREATE TABLE ambari.user_roles (role_name VARCHAR(255) NOT NULL, user_id INTEGER NOT NULL, PRIMARY KEY (role_name, user_id));
GRANT ALL PRIVILEGES ON TABLE ambari.user_roles TO :username;

CREATE TABLE ambari.key_value_store ("key" VARCHAR(255), "value" VARCHAR, PRIMARY KEY("key"));
GRANT ALL PRIVILEGES ON TABLE ambari.key_value_store TO :username;

ALTER TABLE ambari.clusterconfig ADD CONSTRAINT FK_clusterconfig_cluster_id FOREIGN KEY (cluster_id) REFERENCES ambari.clusters (cluster_id);
ALTER TABLE ambari.clusterservices ADD CONSTRAINT FK_clusterservices_cluster_id FOREIGN KEY (cluster_id) REFERENCES ambari.clusters (cluster_id);
ALTER TABLE ambari.clusterstate ADD CONSTRAINT FK_clusterstate_cluster_id FOREIGN KEY (cluster_id) REFERENCES ambari.clusters (cluster_id);
ALTER TABLE ambari.componentconfigmapping ADD CONSTRAINT FK_componentconfigmapping_config_tag FOREIGN KEY (config_tag, config_type, cluster_id) REFERENCES ambari.clusterconfig (version_tag, type_name, cluster_id);
ALTER TABLE ambari.componentconfigmapping ADD CONSTRAINT FK_componentconfigmapping_component_name FOREIGN KEY (component_name, cluster_id, service_name) REFERENCES ambari.servicecomponentdesiredstate (component_name, cluster_id, service_name);
ALTER TABLE ambari.hostcomponentconfigmapping ADD CONSTRAINT FK_hostcomponentconfigmapping_config_tag FOREIGN KEY (config_tag, config_type, cluster_id) REFERENCES ambari.clusterconfig (version_tag, type_name, cluster_id);
ALTER TABLE ambari.hostcomponentconfigmapping ADD CONSTRAINT FK_hostcomponentconfigmapping_cluster_id FOREIGN KEY (cluster_id, component_name, host_name, service_name) REFERENCES ambari.hostcomponentstate (cluster_id, component_name, host_name, service_name);
ALTER TABLE ambari.hostcomponentdesiredconfigmapping ADD CONSTRAINT FK_hostcomponentdesiredconfigmapping_config_tag FOREIGN KEY (config_tag, config_type, cluster_id) REFERENCES ambari.clusterconfig (version_tag, type_name, cluster_id);
ALTER TABLE ambari.hostcomponentdesiredconfigmapping ADD CONSTRAINT FK_hostcomponentdesiredconfigmapping_cluster_id FOREIGN KEY (cluster_id, component_name, host_name, service_name) REFERENCES ambari.hostcomponentdesiredstate (cluster_id, component_name, host_name, service_name);
ALTER TABLE ambari.hostcomponentdesiredstate ADD CONSTRAINT FK_hostcomponentdesiredstate_host_name FOREIGN KEY (host_name) REFERENCES ambari.hosts (host_name);
ALTER TABLE ambari.hostcomponentdesiredstate ADD CONSTRAINT FK_hostcomponentdesiredstate_component_name FOREIGN KEY (component_name, cluster_id, service_name) REFERENCES ambari.servicecomponentdesiredstate (component_name, cluster_id, service_name);
ALTER TABLE ambari.hostcomponentstate ADD CONSTRAINT FK_hostcomponentstate_component_name FOREIGN KEY (component_name, cluster_id, service_name) REFERENCES ambari.servicecomponentdesiredstate (component_name, cluster_id, service_name);
ALTER TABLE ambari.hostcomponentstate ADD CONSTRAINT FK_hostcomponentstate_host_name FOREIGN KEY (host_name) REFERENCES ambari.hosts (host_name);
ALTER TABLE ambari.hoststate ADD CONSTRAINT FK_hoststate_host_name FOREIGN KEY (host_name) REFERENCES ambari.hosts (host_name);
ALTER TABLE ambari.servicecomponentdesiredstate ADD CONSTRAINT FK_servicecomponentdesiredstate_service_name FOREIGN KEY (service_name, cluster_id) REFERENCES ambari.clusterservices (service_name, cluster_id);
ALTER TABLE ambari.serviceconfigmapping ADD CONSTRAINT FK_serviceconfigmapping_config_tag FOREIGN KEY (config_tag, config_type, cluster_id) REFERENCES ambari.clusterconfig (version_tag, type_name, cluster_id);
ALTER TABLE ambari.serviceconfigmapping ADD CONSTRAINT FK_serviceconfigmapping_service_name FOREIGN KEY (service_name, cluster_id) REFERENCES ambari.clusterservices (service_name, cluster_id);
ALTER TABLE ambari.servicedesiredstate ADD CONSTRAINT FK_servicedesiredstate_service_name FOREIGN KEY (service_name, cluster_id) REFERENCES ambari.clusterservices (service_name, cluster_id);
ALTER TABLE ambari.execution_command ADD CONSTRAINT FK_execution_command_task_id FOREIGN KEY (task_id) REFERENCES ambari.host_role_command (task_id);
ALTER TABLE ambari.host_role_command ADD CONSTRAINT FK_host_role_command_stage_id FOREIGN KEY (stage_id, request_id) REFERENCES ambari.stage (stage_id, request_id);
ALTER TABLE ambari.host_role_command ADD CONSTRAINT FK_host_role_command_host_name FOREIGN KEY (host_name) REFERENCES ambari.hosts (host_name);
ALTER TABLE ambari.role_success_criteria ADD CONSTRAINT FK_role_success_criteria_stage_id FOREIGN KEY (stage_id, request_id) REFERENCES ambari.stage (stage_id, request_id);
ALTER TABLE ambari.stage ADD CONSTRAINT FK_stage_cluster_id FOREIGN KEY (cluster_id) REFERENCES ambari.clusters (cluster_id);
ALTER TABLE ambari.ClusterHostMapping ADD CONSTRAINT FK_ClusterHostMapping_host_name FOREIGN KEY (host_name) REFERENCES ambari.hosts (host_name);
ALTER TABLE ambari.ClusterHostMapping ADD CONSTRAINT FK_ClusterHostMapping_cluster_id FOREIGN KEY (cluster_id) REFERENCES ambari.clusters (cluster_id);
ALTER TABLE ambari.user_roles ADD CONSTRAINT FK_user_roles_user_id FOREIGN KEY (user_id) REFERENCES ambari.users (user_id);
ALTER TABLE ambari.user_roles ADD CONSTRAINT FK_user_roles_role_name FOREIGN KEY (role_name) REFERENCES ambari.roles (role_name);

CREATE SEQUENCE ambari.host_role_command_task_id_seq START WITH 1;

GRANT ALL PRIVILEGES ON TABLE ambari.host_role_command_task_id_seq TO :username;

GRANT ALL PRIVILEGES ON TABLE ambari.users_user_id_seq TO :username;

CREATE SEQUENCE ambari.clusters_cluster_id_seq START WITH 1;

GRANT ALL PRIVILEGES ON TABLE ambari.clusters_cluster_id_seq TO :username;

BEGIN;

insert into ambari.Roles(role_name)
select 'admin'
union all
select 'user';

insert into ambari.Users(user_name, user_password, ldap_user)
select 'admin','538916f8943ec225d97a9a86a2c6ec0818c1cd400e09e03b660fdaaec4af29ddbb6f2b1033b81b00', FALSE;

insert into ambari.user_roles(role_name, user_id)
select 'admin',(select user_id from ambari.users where user_name='admin' and ldap_user=false);

COMMIT;

-- ambari log4j DDL

CREATE DATABASE ambarirca;

\connect ambarirca;

CREATE USER "mapred" WITH PASSWORD 'mapred';

GRANT ALL PRIVILEGES ON DATABASE ambarirca TO "mapred";

CREATE TABLE workflow (
  workflowId TEXT, workflowName TEXT,
  parentWorkflowId TEXT,  
  workflowContext TEXT, userName TEXT,
  startTime BIGINT, lastUpdateTime BIGINT,
  numJobsTotal INTEGER, numJobsCompleted INTEGER,
  inputBytes BIGINT, outputBytes BIGINT,
  duration BIGINT,
  PRIMARY KEY (workflowId),
  FOREIGN KEY (parentWorkflowId) REFERENCES workflow(workflowId)
);

GRANT ALL PRIVILEGES ON TABLE workflow TO "mapred";

CREATE TABLE job (
  jobId TEXT, workflowId TEXT, jobName TEXT, workflowEntityName TEXT,
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

GRANT ALL PRIVILEGES ON TABLE job TO "mapred";

CREATE TABLE task (
  taskId TEXT, jobId TEXT, taskType TEXT, splits TEXT, 
  startTime BIGINT, finishTime BIGINT, status TEXT, error TEXT, counters TEXT, 
  failedAttempt TEXT, 
  PRIMARY KEY(taskId), 
  FOREIGN KEY(jobId) REFERENCES job(jobId)
);

GRANT ALL PRIVILEGES ON TABLE task TO "mapred";

CREATE TABLE taskAttempt (
  taskAttemptId TEXT, taskId TEXT, jobId TEXT, taskType TEXT, taskTracker TEXT, 
  startTime BIGINT, finishTime BIGINT, 
  mapFinishTime BIGINT, shuffleFinishTime BIGINT, sortFinishTime BIGINT, 
  locality TEXT, avataar TEXT, 
  status TEXT, error TEXT, counters TEXT, 
  inputBytes BIGINT, outputBytes BIGINT,
  PRIMARY KEY(taskAttemptId), 
  FOREIGN KEY(jobId) REFERENCES job(jobId), 
  FOREIGN KEY(taskId) REFERENCES task(taskId)
); 

GRANT ALL PRIVILEGES ON TABLE taskAttempt TO "mapred";

CREATE TABLE hdfsEvent (
  timestamp BIGINT,
  userName TEXT,
  clientIP TEXT,
  operation TEXT,
  srcPath TEXT,
  dstPath TEXT,
  permissions TEXT
);

GRANT ALL PRIVILEGES ON TABLE hdfsEvent TO "mapred";

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

GRANT ALL PRIVILEGES ON TABLE mapreduceEvent TO "mapred";

CREATE TABLE clusterEvent (
  timestamp BIGINT, 
  service TEXT, status TEXT, 
  error TEXT, data TEXT , 
  host TEXT, rack TEXT
);

GRANT ALL PRIVILEGES ON TABLE clusterEvent TO "mapred";


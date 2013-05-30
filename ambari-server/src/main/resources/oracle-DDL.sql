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
-- PL/SQL script
DROP USER &1 CASCADE;
CREATE USER &1 IDENTIFIED BY &2 DEFAULT TABLESPACE "USERS" TEMPORARY TABLESPACE "TEMP";

GRANT UNLIMITED TABLESPACE TO &1;
GRANT CREATE SESSION TO &1;
GRANT CREATE TABLE TO &1;

ALTER SESSION SET CURRENT_SCHEMA= &1;

CREATE TABLE clusters (cluster_id NUMBER(19) NOT NULL, cluster_info VARCHAR2(255) NULL, cluster_name VARCHAR2(100) NOT NULL UNIQUE, desired_cluster_state VARCHAR2(255) NULL, desired_stack_version VARCHAR2(255) NULL, PRIMARY KEY (cluster_id));
CREATE TABLE clusterconfig (version_tag VARCHAR2(255) NOT NULL, type_name VARCHAR2(255) NOT NULL, cluster_id NUMBER(19) NOT NULL, config_data CLOB NOT NULL, create_timestamp NUMBER(19) NOT NULL, PRIMARY KEY (version_tag, type_name, cluster_id));
CREATE TABLE clusterservices (service_name VARCHAR2(255) NOT NULL, cluster_id NUMBER(19) NOT NULL, service_enabled NUMBER(10) NOT NULL, PRIMARY KEY (service_name, cluster_id));
CREATE TABLE clusterstate (cluster_id NUMBER(19) NOT NULL, current_cluster_state VARCHAR2(255) NULL, current_stack_version VARCHAR2(255) NULL, PRIMARY KEY (cluster_id));
CREATE TABLE componentconfigmapping (config_type VARCHAR2(255) NOT NULL, cluster_id NUMBER(19) NOT NULL, component_name VARCHAR2(255) NOT NULL, config_tag VARCHAR2(255) NOT NULL, service_name VARCHAR2(255) NOT NULL, timestamp NUMBER(19) NOT NULL, PRIMARY KEY (config_type, cluster_id, component_name, service_name));
CREATE TABLE hostcomponentconfigmapping (config_type VARCHAR2(255) NOT NULL, cluster_id NUMBER(19) NOT NULL, component_name VARCHAR2(255) NOT NULL, config_tag VARCHAR2(255) NOT NULL, host_name VARCHAR2(255) NOT NULL, service_name VARCHAR2(255) NOT NULL, timestamp NUMBER(19) NOT NULL, PRIMARY KEY (config_type, cluster_id, component_name, host_name, service_name));
CREATE TABLE hcdesiredconfigmapping (config_type VARCHAR2(255) NOT NULL, cluster_id NUMBER(19) NOT NULL, component_name VARCHAR2(255) NOT NULL, config_tag VARCHAR2(255) NOT NULL, host_name VARCHAR2(255) NOT NULL, service_name VARCHAR2(255) NOT NULL, timestamp NUMBER(19) NOT NULL, PRIMARY KEY (config_type, cluster_id, component_name, host_name, service_name));
CREATE TABLE hostcomponentdesiredstate (cluster_id NUMBER(19) NOT NULL, component_name VARCHAR2(255) NOT NULL, desired_stack_version VARCHAR2(255) NULL, desired_state VARCHAR2(255) NOT NULL, host_name VARCHAR2(255) NOT NULL, service_name VARCHAR2(255) NOT NULL, PRIMARY KEY (cluster_id, component_name, host_name, service_name));
CREATE TABLE hostcomponentstate (cluster_id NUMBER(19) NOT NULL, component_name VARCHAR2(255) NOT NULL, current_stack_version VARCHAR2(255) NOT NULL, current_state VARCHAR2(255) NOT NULL, host_name VARCHAR2(255) NOT NULL, service_name VARCHAR2(255) NOT NULL, PRIMARY KEY (cluster_id, component_name, host_name, service_name));
CREATE TABLE hosts (host_name VARCHAR2(255) NOT NULL, cpu_count INTEGER NOT NULL, cpu_info VARCHAR2(255) NULL, discovery_status VARCHAR2(2000) NULL, disks_info CLOB NOT NULL, host_attributes CLOB NULL, ipv4 VARCHAR2(255) NULL, ipv6 VARCHAR2(255) NULL, last_registration_time INTEGER NOT NULL, os_arch VARCHAR2(255) NULL, os_info VARCHAR2(1000) NULL, os_type VARCHAR2(255) NULL, ph_cpu_count INTEGER NOT NULL, public_host_name VARCHAR2(255) NULL, rack_info VARCHAR2(255) NOT NULL, total_mem INTEGER NOT NULL, PRIMARY KEY (host_name));
CREATE TABLE hoststate (agent_version VARCHAR2(255) NULL, available_mem NUMBER(19) NOT NULL, current_state VARCHAR2(255) NOT NULL, health_status VARCHAR2(255) NULL, host_name VARCHAR2(255) NOT NULL, time_in_state NUMBER(19) NOT NULL, PRIMARY KEY (host_name));
CREATE TABLE servicecomponentdesiredstate (component_name VARCHAR2(255) NOT NULL, cluster_id NUMBER(19) NOT NULL, desired_stack_version VARCHAR2(255) NULL, desired_state VARCHAR2(255) NOT NULL, service_name VARCHAR2(255) NOT NULL, PRIMARY KEY (component_name, cluster_id, service_name));
CREATE TABLE serviceconfigmapping (config_type VARCHAR2(255) NOT NULL, cluster_id NUMBER(19) NOT NULL, config_tag VARCHAR2(255) NOT NULL, service_name VARCHAR2(255) NOT NULL, timestamp NUMBER(19) NOT NULL, PRIMARY KEY (config_type, cluster_id, service_name));
CREATE TABLE servicedesiredstate (cluster_id NUMBER(19) NOT NULL, desired_host_role_mapping NUMBER(10) NOT NULL, desired_stack_version VARCHAR2(255) NULL, desired_state VARCHAR2(255) NOT NULL, service_name VARCHAR2(255) NOT NULL, PRIMARY KEY (cluster_id, service_name));
CREATE TABLE roles (role_name VARCHAR2(255) NOT NULL, PRIMARY KEY (role_name));
CREATE TABLE users (user_id NUMBER(10) NOT NULL, create_time TIMESTAMP NULL, ldap_user NUMBER(10) DEFAULT 0, user_name VARCHAR2(255) NULL, user_password VARCHAR2(255) NULL, PRIMARY KEY (user_id));
CREATE TABLE execution_command (task_id NUMBER(19) NOT NULL, command BLOB NULL, PRIMARY KEY (task_id));
CREATE TABLE host_role_command (task_id NUMBER(19) NOT NULL, attempt_count NUMBER(5) NOT NULL, event CLOB NULL, exitcode NUMBER(10) NOT NULL, host_name VARCHAR2(255) NOT NULL, last_attempt_time NUMBER(19) NOT NULL, request_id NUMBER(19) NOT NULL, role VARCHAR2(255) NULL, role_command VARCHAR2(255) NULL, stage_id NUMBER(19) NOT NULL, start_time NUMBER(19) NOT NULL, status VARCHAR2(255) NULL, std_error BLOB NULL, std_out BLOB NULL, PRIMARY KEY (task_id));
CREATE TABLE role_success_criteria (role VARCHAR2(255) NOT NULL, request_id NUMBER(19) NOT NULL, stage_id NUMBER(19) NOT NULL, success_factor NUMBER(19,4) NOT NULL, PRIMARY KEY (role, request_id, stage_id));
CREATE TABLE stage (stage_id NUMBER(19) NOT NULL, request_id NUMBER(19) NOT NULL, cluster_id NUMBER(19) NULL, log_info VARCHAR2(255) NULL, request_context VARCHAR2(255) NULL, PRIMARY KEY (stage_id, request_id));
CREATE TABLE key_value_store ("key" VARCHAR2(255) NOT NULL, "value" CLOB NULL, PRIMARY KEY ("key"));
CREATE TABLE clusterconfigmapping (type_name VARCHAR2(255) NOT NULL, create_timestamp NUMBER(19) NOT NULL, cluster_id NUMBER(19) NOT NULL, selected NUMBER(10) NOT NULL, version_tag VARCHAR2(255) NOT NULL, user_name VARCHAR(255) DEFAULT '_db', PRIMARY KEY (type_name, create_timestamp, cluster_id));
CREATE TABLE hostconfigmapping (create_timestamp NUMBER(19) NOT NULL, host_name VARCHAR2(255) NOT NULL, cluster_id NUMBER(19) NOT NULL, type_name VARCHAR2(255) NOT NULL, selected NUMBER(10) NOT NULL, service_name VARCHAR2(255) NULL, version_tag VARCHAR2(255) NOT NULL, user_name VARCHAR(255) DEFAULT '_db', PRIMARY KEY (create_timestamp, host_name, cluster_id, type_name));
CREATE TABLE metainfo ("metainfo_key" VARCHAR2(255) NOT NULL, "metainfo_value" CLOB NULL, PRIMARY KEY ("metainfo_key"));
CREATE TABLE ClusterHostMapping (cluster_id NUMBER(19) NOT NULL, host_name VARCHAR2(255) NOT NULL, PRIMARY KEY (cluster_id, host_name));
CREATE TABLE user_roles (role_name VARCHAR2(255) NOT NULL, user_id NUMBER(10) NOT NULL, PRIMARY KEY (role_name, user_id));
CREATE TABLE ambari_sequences (sequence_name VARCHAR2(50) NOT NULL, value NUMBER(38) NULL, PRIMARY KEY (sequence_name));


ALTER TABLE users ADD CONSTRAINT UNQ_users_0 UNIQUE (user_name, ldap_user);
ALTER TABLE clusterconfig ADD CONSTRAINT FK_clusterconfig_cluster_id FOREIGN KEY (cluster_id) REFERENCES clusters (cluster_id);
ALTER TABLE clusterservices ADD CONSTRAINT FK_clusterservices_cluster_id FOREIGN KEY (cluster_id) REFERENCES clusters (cluster_id);
ALTER TABLE clusterstate ADD CONSTRAINT FK_clusterstate_cluster_id FOREIGN KEY (cluster_id) REFERENCES clusters (cluster_id);
ALTER TABLE componentconfigmapping ADD CONSTRAINT cmponentconfigmappingconfigtag FOREIGN KEY (config_tag, config_type, cluster_id) REFERENCES clusterconfig (version_tag, type_name, cluster_id);
ALTER TABLE componentconfigmapping ADD CONSTRAINT cmpnntconfigmappingcmpnentname FOREIGN KEY (component_name, cluster_id, service_name) REFERENCES servicecomponentdesiredstate (component_name, cluster_id, service_name);
ALTER TABLE hostcomponentconfigmapping ADD CONSTRAINT hstcmponentconfigmappingclstrd FOREIGN KEY (cluster_id, component_name, host_name, service_name) REFERENCES hostcomponentstate (cluster_id, component_name, host_name, service_name);
ALTER TABLE hostcomponentconfigmapping ADD CONSTRAINT hstcmponentconfigmappingcnfgtg FOREIGN KEY (config_tag, config_type, cluster_id) REFERENCES clusterconfig (version_tag, type_name, cluster_id);
ALTER TABLE hcdesiredconfigmapping ADD CONSTRAINT hcdesiredconfigmappingcnfigtag FOREIGN KEY (config_tag, config_type, cluster_id) REFERENCES clusterconfig (version_tag, type_name, cluster_id);
ALTER TABLE hcdesiredconfigmapping ADD CONSTRAINT hcdesiredconfigmappingclsterid FOREIGN KEY (cluster_id, component_name, host_name, service_name) REFERENCES hostcomponentdesiredstate (cluster_id, component_name, host_name, service_name);
ALTER TABLE hostcomponentdesiredstate ADD CONSTRAINT hstcmponentdesiredstatehstname FOREIGN KEY (host_name) REFERENCES hosts (host_name);
ALTER TABLE hostcomponentdesiredstate ADD CONSTRAINT hstcmpnntdesiredstatecmpnntnme FOREIGN KEY (component_name, cluster_id, service_name) REFERENCES servicecomponentdesiredstate (component_name, cluster_id, service_name);
ALTER TABLE hostcomponentstate ADD CONSTRAINT hstcomponentstatecomponentname FOREIGN KEY (component_name, cluster_id, service_name) REFERENCES servicecomponentdesiredstate (component_name, cluster_id, service_name);
ALTER TABLE hostcomponentstate ADD CONSTRAINT hostcomponentstate_host_name FOREIGN KEY (host_name) REFERENCES hosts (host_name);
ALTER TABLE hoststate ADD CONSTRAINT FK_hoststate_host_name FOREIGN KEY (host_name) REFERENCES hosts (host_name);
ALTER TABLE servicecomponentdesiredstate ADD CONSTRAINT srvccmponentdesiredstatesrvcnm FOREIGN KEY (service_name, cluster_id) REFERENCES clusterservices (service_name, cluster_id);
ALTER TABLE serviceconfigmapping ADD CONSTRAINT srviceconfigmappingservicename FOREIGN KEY (service_name, cluster_id) REFERENCES clusterservices (service_name, cluster_id);
ALTER TABLE serviceconfigmapping ADD CONSTRAINT serviceconfigmappingconfig_tag FOREIGN KEY (config_tag, config_type, cluster_id) REFERENCES clusterconfig (version_tag, type_name, cluster_id);
ALTER TABLE servicedesiredstate ADD CONSTRAINT servicedesiredstateservicename FOREIGN KEY (service_name, cluster_id) REFERENCES clusterservices (service_name, cluster_id);
ALTER TABLE execution_command ADD CONSTRAINT FK_execution_command_task_id FOREIGN KEY (task_id) REFERENCES host_role_command (task_id);
ALTER TABLE host_role_command ADD CONSTRAINT FK_host_role_command_stage_id FOREIGN KEY (stage_id, request_id) REFERENCES stage (stage_id, request_id);
ALTER TABLE host_role_command ADD CONSTRAINT FK_host_role_command_host_name FOREIGN KEY (host_name) REFERENCES hosts (host_name);
ALTER TABLE role_success_criteria ADD CONSTRAINT role_success_criteria_stage_id FOREIGN KEY (stage_id, request_id) REFERENCES stage (stage_id, request_id);
ALTER TABLE stage ADD CONSTRAINT FK_stage_cluster_id FOREIGN KEY (cluster_id) REFERENCES clusters (cluster_id);
ALTER TABLE clusterconfigmapping ADD CONSTRAINT clusterconfigmappingcluster_id FOREIGN KEY (cluster_id) REFERENCES clusters (cluster_id);
ALTER TABLE ClusterHostMapping ADD CONSTRAINT ClusterHostMapping_cluster_id FOREIGN KEY (cluster_id) REFERENCES clusters (cluster_id);
ALTER TABLE ClusterHostMapping ADD CONSTRAINT ClusterHostMapping_host_name FOREIGN KEY (host_name) REFERENCES hosts (host_name);
ALTER TABLE user_roles ADD CONSTRAINT FK_user_roles_user_id FOREIGN KEY (user_id) REFERENCES users (user_id);
ALTER TABLE user_roles ADD CONSTRAINT FK_user_roles_role_name FOREIGN KEY (role_name) REFERENCES roles (role_name);


GRANT ALL ON clusters TO &1;
GRANT ALL ON clusterconfig TO &1;
GRANT ALL ON clusterconfigmapping TO &1;
GRANT ALL ON clusterservices TO &1;
GRANT ALL ON clusterstate TO &1;
GRANT ALL ON componentconfigmapping TO &1;
GRANT ALL ON hostcomponentconfigmapping TO &1;
GRANT ALL ON hcdesiredconfigmapping TO &1;
GRANT ALL ON hostcomponentdesiredstate TO &1;
GRANT ALL ON hostcomponentstate TO &1;
GRANT ALL ON hosts TO &1;
GRANT ALL ON hoststate TO &1;
GRANT ALL ON servicecomponentdesiredstate TO &1;
GRANT ALL ON serviceconfigmapping TO &1;
GRANT ALL ON servicedesiredstate TO &1;
GRANT ALL ON roles TO &1;
GRANT ALL ON users TO &1;
GRANT ALL ON execution_command TO &1;
GRANT ALL ON host_role_command TO &1;
GRANT ALL ON role_success_criteria TO &1;
GRANT ALL ON stage TO &1;
GRANT ALL ON ClusterHostMapping TO &1;
GRANT ALL ON user_roles TO &1;
GRANT ALL ON key_value_store TO &1;
GRANT ALL ON hostconfigmapping TO &1;
GRANT ALL ON ambari_sequences TO &1;
GRANT ALL ON metainfo TO &1;


INSERT INTO ambari_sequences(sequence_name, value) values ('host_role_command_id_seq', 0);
INSERT INTO ambari_sequences(sequence_name, value) values ('user_id_seq', 1);
INSERT INTO ambari_sequences(sequence_name, value) values ('cluster_id_seq', 0);
INSERT INTO metainfo("metainfo_key", "metainfo_value") values ('version', '${ambariVersion}');

insert into Roles(role_name)
select 'admin' from dual
union all
select 'user' from dual;

insert into Users(user_id, user_name, user_password)
select 1,'admin','538916f8943ec225d97a9a86a2c6ec0818c1cd400e09e03b660fdaaec4af29ddbb6f2b1033b81b00' from dual;

insert into user_roles(role_name, user_id)
select 'admin',1 from dual;



commit;

-- ambari rca

CREATE TABLE workflow (
  workflowId VARCHAR2(4000), workflowName VARCHAR2(4000),
  parentWorkflowId VARCHAR2(4000),  
  workflowContext VARCHAR2(4000), userName VARCHAR2(4000),
  startTime INTEGER, lastUpdateTime INTEGER,
  numJobsTotal INTEGER, numJobsCompleted INTEGER,
  inputBytes INTEGER, outputBytes INTEGER,
  duration INTEGER,
  PRIMARY KEY (workflowId),
  FOREIGN KEY (parentWorkflowId) REFERENCES workflow(workflowId)
);

GRANT ALL ON workflow TO &1;

CREATE TABLE job (
  jobId VARCHAR2(4000), workflowId VARCHAR2(4000), jobName VARCHAR2(4000), workflowEntityName VARCHAR2(4000),
  userName VARCHAR2(4000), queue CLOB, acls CLOB, confPath CLOB, 
  submitTime INTEGER, launchTime INTEGER, finishTime INTEGER, 
  maps INTEGER, reduces INTEGER, status VARCHAR2(4000), priority VARCHAR2(4000), 
  finishedMaps INTEGER, finishedReduces INTEGER, 
  failedMaps INTEGER, failedReduces INTEGER, 
  mapsRuntime INTEGER, reducesRuntime INTEGER,
  mapCounters VARCHAR2(4000), reduceCounters VARCHAR2(4000), jobCounters VARCHAR2(4000), 
  inputBytes INTEGER, outputBytes INTEGER,
  PRIMARY KEY(jobId),
  FOREIGN KEY(workflowId) REFERENCES workflow(workflowId)
);

GRANT ALL ON job TO &1;

CREATE TABLE task (
  taskId VARCHAR2(4000), jobId VARCHAR2(4000), taskType VARCHAR2(4000), splits VARCHAR2(4000), 
  startTime INTEGER, finishTime INTEGER, status VARCHAR2(4000), error CLOB, counters VARCHAR2(4000), 
  failedAttempt VARCHAR2(4000), 
  PRIMARY KEY(taskId), 
  FOREIGN KEY(jobId) REFERENCES job(jobId)
);

GRANT ALL ON task TO &1;

CREATE TABLE taskAttempt (
  taskAttemptId VARCHAR2(4000), taskId VARCHAR2(4000), jobId VARCHAR2(4000), taskType VARCHAR2(4000), taskTracker VARCHAR2(4000), 
  startTime INTEGER, finishTime INTEGER, 
  mapFinishTime INTEGER, shuffleFinishTime INTEGER, sortFinishTime INTEGER, 
  locality VARCHAR2(4000), avataar VARCHAR2(4000), 
  status VARCHAR2(4000), error CLOB, counters VARCHAR2(4000), 
  inputBytes INTEGER, outputBytes INTEGER,
  PRIMARY KEY(taskAttemptId), 
  FOREIGN KEY(jobId) REFERENCES job(jobId), 
  FOREIGN KEY(taskId) REFERENCES task(taskId)
); 

GRANT ALL ON taskAttempt TO &1;

CREATE TABLE hdfsEvent (
  timestamp INTEGER,
  userName VARCHAR2(4000),
  clientIP VARCHAR2(4000),
  operation VARCHAR2(4000),
  srcPath CLOB,
  dstPath CLOB,
  permissions VARCHAR2(4000)
);

GRANT ALL ON hdfsEvent TO &1;

CREATE TABLE mapreduceEvent (
  timestamp INTEGER,
  userName VARCHAR2(4000),
  clientIP VARCHAR2(4000),
  operation VARCHAR2(4000),
  target VARCHAR2(4000),
  result CLOB,
  description CLOB,
  permissions VARCHAR2(4000)
);

GRANT ALL ON mapreduceEvent TO &1;

CREATE TABLE clusterEvent (
  timestamp INTEGER, 
  service VARCHAR2(4000), status VARCHAR2(4000), 
  error CLOB, data CLOB , 
  host VARCHAR2(4000), rack VARCHAR2(4000)
);

GRANT ALL ON clusterEvent TO &1;


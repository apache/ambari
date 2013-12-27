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

DROP SCHEMA IF EXISTS ambari CASCADE;

DROP ROLE IF EXISTS "ambari-server";

CREATE ROLE "ambari-server" LOGIN ENCRYPTED PASSWORD 'bigdata';

CREATE SCHEMA ambari
  AUTHORIZATION "ambari-server";

COMMENT ON SCHEMA ambari
  IS 'test schema';

SET search_path TO ambari;

/* Table for storing user information*/
CREATE TABLE Users
(
user_name VARCHAR,
user_password VARCHAR,
ldap_user boolean DEFAULT FALSE NOT NULL,
create_time TIMESTAMP DEFAULT now() NOT NULL,
PRIMARY KEY(user_name, ldap_user)
);

/*Table for storing roles list - can be dropped out if list of roles is predefined and limited on upper layer*/
CREATE TABLE Roles
(
role_name VARCHAR PRIMARY KEY
);

/*Users - Roles mapping table*/
CREATE TABLE user_roles
(
user_name VARCHAR,
ldap_user boolean default false,
role_name VARCHAR references Roles(role_name),
PRIMARY KEY(user_name, ldap_user, role_name),
FOREIGN KEY(user_name, ldap_user) REFERENCES Users(user_name, ldap_user)
);

/* Overall clusters table - all created/managed clusters */
CREATE TABLE Clusters
(
cluster_id BIGSERIAL,
cluster_name VARCHAR(100) UNIQUE NOT NULL,
desired_cluster_state VARCHAR DEFAULT '' NOT NULL,
cluster_info VARCHAR DEFAULT '' NOT NULL,
PRIMARY KEY (cluster_id)
);

/* All hosts for all clusters */
CREATE TABLE Hosts
(
host_name VARCHAR NOT NULL,
ipv4 VARCHAR UNIQUE,
ipv6 VARCHAR UNIQUE,
total_mem BIGINT DEFAULT '0' NOT NULL,
cpu_count INTEGER DEFAULT '0' NOT NULL,
ph_cpu_count INTEGER DEFAULT '0' NOT NULL,
cpu_info VARCHAR DEFAULT '' NOT NULL,
os_arch VARCHAR DEFAULT '' NOT NULL,
os_info VARCHAR DEFAULT '' NOT NULL,
os_type VARCHAR DEFAULT '' NOT NULL,
discovery_status VARCHAR DEFAULT '' NOT NULL,
last_registration_time BIGINT DEFAULT '0' NOT NULL,
rack_info VARCHAR DEFAULT '/default-rack' NOT NULL,
host_attributes VARCHAR DEFAULT '' NOT NULL,
PRIMARY KEY (host_name)
);

/* Cluster Hosts mapping table */
CREATE TABLE ClusterHostMapping
(
  cluster_id BIGINT references Clusters(cluster_id),
  host_name VARCHAR references Hosts(host_name),
  PRIMARY KEY(cluster_id, host_name)
);

CREATE TABLE ClusterServices
(
cluster_id BIGINT NOT NULL references Clusters(cluster_id),
service_name VARCHAR,
service_enabled INTEGER DEFAULT '0' NOT NULL,
PRIMARY KEY (cluster_id,service_name)
);

/* Configs at a service level */
/* This will be used in most scenarios for homogenous clusters */
/* Snapshot is a blob for all properties and their values. There is no separate row for each property */
/* A special service called AMBARI or GLOBAL can be leveraged for global level configs */
CREATE TABLE ServiceConfig
(
config_version SERIAL /*INTEGER NOT NULL AUTO_INCREMENT*/,
cluster_id BIGINT NOT NULL,
service_name VARCHAR NOT NULL,
config_snapshot VARCHAR DEFAULT '' NOT NULL,
config_snapshot_time timestamp NOT NULL,
PRIMARY KEY (config_version),
FOREIGN KEY (cluster_id, service_name) REFERENCES ClusterServices(cluster_id, service_name)
);

/* Configs that are overridden at the component level */
/* Combination of serviceconfig and servicecomponentconfig table 
    defines the config for a given component. 
    Absence of an entry implies the component’s configs are same as that of the overall service config */
CREATE TABLE ServiceComponentConfig
(
config_version SERIAL /*INTEGER NOT NULL AUTO_INCREMENT*/,
cluster_id BIGINT NOT NULL,
service_name VARCHAR NOT NULL,
component_name VARCHAR NOT NULL,
config_snapshot VARCHAR DEFAULT '' NOT NULL,
config_snapshot_time timestamp NOT NULL,
PRIMARY KEY (config_version),
FOREIGN KEY (cluster_id, service_name) REFERENCES ClusterServices(cluster_id, service_name)
);

/* For overridding configs on a per host level for heterogenous clusters */
CREATE TABLE ServiceComponentHostConfig
(
config_version SERIAL /*INTEGER NOT NULL AUTO_INCREMENT*/,
cluster_id BIGINT NOT NULL,
service_name VARCHAR NOT NULL,
component_name VARCHAR NOT NULL,
host_name VARCHAR NOT NULL references Hosts(host_name),
config_snapshot VARCHAR DEFAULT '' NOT NULL,
config_snapshot_time timestamp NOT NULL,
PRIMARY KEY (config_version),
FOREIGN KEY (cluster_id, service_name) REFERENCES ClusterServices(cluster_id, service_name)
);

CREATE TABLE ServiceDesiredState
(
cluster_id BIGINT,
service_name VARCHAR DEFAULT '' NOT NULL,
desired_state VARCHAR DEFAULT '' NOT NULL,
desired_host_role_mapping INTEGER DEFAULT '0' NOT NULL,
desired_stack_version VARCHAR DEFAULT '' NOT NULL,
PRIMARY KEY (cluster_id, service_name),
FOREIGN KEY (cluster_id, service_name) REFERENCES ClusterServices(cluster_id, service_name)
);

CREATE TABLE HostComponentMapping /*HostRoleMapping*/
(
cluster_id BIGINT,
service_name VARCHAR DEFAULT '' NOT NULL,
host_component_mapping_id SERIAL /*INTEGER NOT NULL AUTO_INCREMENT*/,
host_component_mapping_snapshot VARCHAR DEFAULT '' NOT NULL,
PRIMARY KEY (cluster_id, service_name, host_component_mapping_id),
FOREIGN KEY (cluster_id, service_name) REFERENCES ClusterServices(cluster_id, service_name)
);


CREATE TABLE ClusterState
(
cluster_id BIGINT NOT NULL references Clusters(cluster_id),
current_cluster_state VARCHAR DEFAULT '' NOT NULL,
current_stack_version VARCHAR DEFAULT '' NOT NULL,
PRIMARY KEY (cluster_id)
);

CREATE TABLE HostState
(
/*cluster_id INTEGER references Clusters(cluster_id),*/
host_name VARCHAR NOT NULL references Hosts(host_name),
available_mem INTEGER DEFAULT '0' NOT NULL,
last_heartbeat_time INTEGER DEFAULT '0' NOT NULL,
time_in_state INTEGER DEFAULT '0' NOT NULL,
agent_version VARCHAR DEFAULT '' NOT NULL,
health_status VARCHAR,
current_state VARCHAR DEFAULT '' NOT NULL,
PRIMARY KEY (host_name)
);


CREATE TABLE ServiceComponentDesiredState
(
cluster_id BIGINT references Clusters(cluster_id),
service_name VARCHAR DEFAULT '' NOT NULL,
component_name VARCHAR DEFAULT '' NOT NULL,
desired_state VARCHAR DEFAULT '' NOT NULL,
desired_stack_version VARCHAR DEFAULT '' NOT NULL,
PRIMARY KEY (cluster_id,service_name,component_name),
FOREIGN KEY (cluster_id, service_name) REFERENCES ClusterServices(cluster_id, service_name)
);


CREATE TABLE HostComponentState
(
cluster_id BIGINT,
service_name VARCHAR DEFAULT '' NOT NULL,
host_name VARCHAR DEFAULT '' NOT NULL references Hosts(host_name),
component_name VARCHAR DEFAULT '' NOT NULL,
current_state VARCHAR DEFAULT '' NOT NULL,
current_config_version VARCHAR DEFAULT '' NOT NULL,
current_stack_version VARCHAR DEFAULT '' NOT NULL,
PRIMARY KEY (cluster_id, service_name, host_name, component_name),
FOREIGN KEY (cluster_id, service_name, component_name) REFERENCES ServiceComponentDesiredState(cluster_id, service_name, component_name)
);

CREATE TABLE HostComponentDesiredState
(
cluster_id BIGINT,
service_name VARCHAR DEFAULT '' NOT NULL,
host_name VARCHAR NOT NULL references Hosts(host_name),
component_name VARCHAR DEFAULT '' NOT NULL,
desired_state VARCHAR DEFAULT '' NOT NULL,
desired_config_version VARCHAR DEFAULT '' NOT NULL, /* desired config version defines a combined version of service/component/node-component config versions */
desired_stack_version VARCHAR DEFAULT '' NOT NULL,
PRIMARY KEY (cluster_id,host_name,component_name),
FOREIGN KEY (cluster_id, service_name, component_name) REFERENCES ServiceComponentDesiredState(cluster_id, service_name, component_name)
);

CREATE TABLE STAGE
(
   cluster_id BIGINT references Clusters(cluster_id),
   request_id BIGINT DEFAULT '0',
   stage_id BIGINT DEFAULT '0' NOT NULL,
   log_info VARCHAR DEFAULT '' NOT NULL,
   PRIMARY KEY (request_id, stage_id)
);

CREATE TABLE HOST_ROLE_COMMAND
(
   task_id BIGSERIAL NOT NULL,
   request_id BIGINT NOT NULL,
   stage_id BIGINT NOT NULL,
   host_name VARCHAR DEFAULT '' NOT NULL references Hosts(host_name),
   role VARCHAR DEFAULT '' NOT NULL,
   command VARCHAR DEFAULT '' NOT NULL,
   event VARCHAR DEFAULT '' NOT NULL, /** Refer to ServiceComponentHostEventType.java */
   exitCode INTEGER DEFAULT '0' NOT NULL,
   status VARCHAR DEFAULT '' NOT NULL, /** PENDING, QUEUED, IN_PROGRESS, COMPLETED, FAILED, TIMEDOUT, ABORTED **/
   std_error VARCHAR DEFAULT '' NOT NULL,
   std_out VARCHAR DEFAULT '' NOT NULL,
   start_time BIGINT DEFAULT -1 NOT NULL,
   last_attempt_time BIGINT DEFAULT -1 NOT NULL,
   attempt_count SMALLINT DEFAULT 0 NOT NULL,
   PRIMARY KEY (task_id),
   FOREIGN KEY (request_id, stage_id) REFERENCES STAGE(request_id, stage_id)
);

CREATE TABLE EXECUTION_COMMAND
(
   task_id BIGINT DEFAULT '0' NOT NULL references HOST_ROLE_COMMAND(task_id),
   command VARCHAR NOT NULL, /** Serialized ExecutionCommand **/
   PRIMARY KEY(task_id)
);


CREATE TABLE ROLE_SUCCESS_CRITERIA
(
   request_id BIGINT NOT NULL,
   stage_id BIGINT NOT NULL,
   role VARCHAR DEFAULT '' NOT NULL,
   success_factor FLOAT DEFAULT 1,
   PRIMARY KEY(role, request_id, stage_id),
   FOREIGN KEY (request_id, stage_id) REFERENCES STAGE(request_id, stage_id)
);

--CREATE TABLE ActionStatus 
--(
--cluster_id INTEGER references Clusters(cluster_id),
--host_name VARCHAR DEFAULT '' NOT NULL references Hosts(host_name),
--role VARCHAR DEFAULT '' NOT NULL,
--request_id INTEGER DEFAULT '0' NOT NULL,
--stage_id INTEGER DEFAULT '0' NOT NULL,
--event VARCHAR DEFAULT '' NOT NULL,
--task_id INTEGER DEFAULT '0' NOT NULL,
--status VARCHAR DEFAULT '' NOT NULL, /* PENDING, QUEUED, COMPLETED, FAILED,, ABORTED */ 
--log_info VARCHAR DEFAULT '' NOT NULL,
--continue_criteria bytea /*BLOB*/ DEFAULT '' NOT NULL, /* Define continuation criteria for moving to next stage */
--PRIMARY KEY (cluster_id, host_name, role, request_id, stage_id)
--);


GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA ambari TO "ambari-server";
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA ambari TO "ambari-server";

BEGIN;

insert into Roles(role_name) 
select 'admin'
union all
select 'user';

insert into Users(user_name, user_password)
select 'administrator','538916f8943ec225d97a9a86a2c6ec0818c1cd400e09e03b660fdaaec4af29ddbb6f2b1033b81b00'
union all
select 'test','d2f5da28bf8353e836fbae0a7f586b9cbda03f590910998957383371fbacba7e4088394991305ef8';

insert into user_roles(user_name,role_name)
select 'test','user'
union all
select 'administrator','admin';

COMMIT;
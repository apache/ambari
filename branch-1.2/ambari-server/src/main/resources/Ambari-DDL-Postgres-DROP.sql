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
\connect ambari
ALTER TABLE ambari.clusterconfig DROP CONSTRAINT FK_clusterconfig_cluster_id;
ALTER TABLE ambari.clusterservices DROP CONSTRAINT FK_clusterservices_cluster_id;
ALTER TABLE ambari.clusterstate DROP CONSTRAINT FK_clusterstate_cluster_id;
ALTER TABLE ambari.componentconfigmapping DROP CONSTRAINT FK_componentconfigmapping_component_name;
ALTER TABLE ambari.hostcomponentconfigmapping DROP CONSTRAINT FK_hostcomponentconfigmapping_cluster_id;
ALTER TABLE ambari.hostcomponentdesiredconfigmapping DROP CONSTRAINT FK_hostcomponentdesiredconfigmapping_config_tag;
ALTER TABLE ambari.hostcomponentdesiredconfigmapping DROP CONSTRAINT FK_hostcomponentdesiredconfigmapping_cluster_id;
ALTER TABLE ambari.hostcomponentdesiredstate DROP CONSTRAINT FK_hostcomponentdesiredstate_host_name;
ALTER TABLE ambari.hostcomponentdesiredstate DROP CONSTRAINT FK_hostcomponentdesiredstate_component_name;
ALTER TABLE ambari.hostcomponentstate DROP CONSTRAINT FK_hostcomponentstate_component_name;
ALTER TABLE ambari.hostcomponentstate DROP CONSTRAINT FK_hostcomponentstate_host_name;
ALTER TABLE ambari.hoststate DROP CONSTRAINT FK_hoststate_host_name;
ALTER TABLE ambari.servicecomponentdesiredstate DROP CONSTRAINT FK_servicecomponentdesiredstate_service_name;
ALTER TABLE ambari.serviceconfigmapping DROP CONSTRAINT FK_serviceconfigmapping_service_name;
ALTER TABLE ambari.servicedesiredstate DROP CONSTRAINT FK_servicedesiredstate_service_name;
ALTER TABLE ambari.execution_command DROP CONSTRAINT FK_execution_command_task_id;
ALTER TABLE ambari.host_role_command DROP CONSTRAINT FK_host_role_command_stage_id;
ALTER TABLE ambari.host_role_command DROP CONSTRAINT FK_host_role_command_host_name;
ALTER TABLE ambari.role_success_criteria DROP CONSTRAINT FK_role_success_criteria_stage_id;
ALTER TABLE ambari.stage DROP CONSTRAINT FK_stage_cluster_id;
ALTER TABLE ambari.ClusterHostMapping DROP CONSTRAINT FK_ClusterHostMapping_host_name;
ALTER TABLE ambari.ClusterHostMapping DROP CONSTRAINT FK_ClusterHostMapping_cluster_id;
ALTER TABLE ambari.user_roles DROP CONSTRAINT FK_user_roles_ldap_user;
ALTER TABLE ambari.user_roles DROP CONSTRAINT FK_user_roles_role_name;
DROP TABLE ambari.clusters CASCADE;
DROP TABLE ambari.clusterservices CASCADE;
DROP TABLE ambari.clusterstate CASCADE;
DROP TABLE ambari.componentconfigmapping CASCADE;
DROP TABLE ambari.hostcomponentconfigmapping CASCADE;
DROP TABLE ambari.hostcomponentdesiredconfigmapping CASCADE;
DROP TABLE ambari.hostcomponentdesiredstate CASCADE;
DROP TABLE ambari.hostcomponentstate CASCADE;
DROP TABLE ambari.hosts CASCADE;
DROP TABLE ambari.hoststate CASCADE;
DROP TABLE ambari.servicecomponentdesiredstate CASCADE;
DROP TABLE ambari.serviceconfigmapping CASCADE;
DROP TABLE ambari.servicedesiredstate CASCADE;
DROP TABLE ambari.roles CASCADE;
DROP TABLE ambari.users CASCADE;
DROP TABLE ambari.execution_command CASCADE;
DROP TABLE ambari.host_role_command CASCADE;
DROP TABLE ambari.role_success_criteria CASCADE;
DROP TABLE ambari.stage CASCADE;
DROP TABLE ambari.ClusterHostMapping CASCADE;
DROP TABLE ambari.clusterconfig CASCADE;
DROP TABLE ambari.user_roles CASCADE;
DROP TABLE ambari.key_value_store CASCADE;
DROP SEQUENCE ambari.host_role_command_task_id_seq;
DROP SEQUENCE ambari.clusters_cluster_id_seq;

\connect ambarirca
ALTER TABLE job DROP CONSTRAINT job_workflowid_fkey;
ALTER TABLE task DROP CONSTRAINT task_jobid_fkey;
ALTER TABLE taskattempt DROP CONSTRAINT taskattempt_jobid_fkey;
ALTER TABLE taskattempt DROP CONSTRAINT taskattempt_taskid_fkey;
DROP TABLE workflow;
DROP TABLE job;
DROP TABLE task;
DROP TABLE taskAttempt;
DROP TABLE hdfsEvent;
DROP TABLE mapreduceEvent;
DROP TABLE clusterEvent;

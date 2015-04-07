/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

/*
Schema purge script for $(AMBARIDBNAME)

Use this script in sqlcmd mode, setting the environment variables like this:
set AMBARIDBNAME=ambari

sqlcmd -S localhost\SQLEXPRESS -i C:\app\ambari-server-1.3.0-SNAPSHOT\resources\Ambari-DDL-SQLServer-DROP.sql
*/

USE [$(AMBARIDBNAME)];

IF OBJECT_ID ('trigger_task_delete','TR') IS NOT NULL DROP TRIGGER trigger_task_delete;
GO
IF OBJECT_ID ('trigger_job_delete','TR') IS NOT NULL DROP TRIGGER trigger_job_delete;
GO
IF OBJECT_ID ('trigger_workflow_delete','TR') IS NOT NULL DROP TRIGGER trigger_workflow_delete;
GO
IF OBJECT_ID('clusterEvent', 'U') IS NOT NULL DROP TABLE clusterEvent
GO
IF OBJECT_ID('mapreduceEvent', 'U') IS NOT NULL DROP TABLE mapreduceEvent
GO
IF OBJECT_ID('hdfsEvent', 'U') IS NOT NULL DROP TABLE hdfsEvent
GO
IF OBJECT_ID('taskAttempt', 'U') IS NOT NULL DROP TABLE taskAttempt
GO
IF OBJECT_ID('task', 'U') IS NOT NULL DROP TABLE task
GO
IF OBJECT_ID('job', 'U') IS NOT NULL DROP TABLE job
GO
IF OBJECT_ID('workflow', 'U') IS NOT NULL DROP TABLE workflow
GO
IF OBJECT_ID('qrtz_locks', 'U') IS NOT NULL DROP TABLE qrtz_locks
GO
IF OBJECT_ID('qrtz_scheduler_state', 'U') IS NOT NULL DROP TABLE qrtz_scheduler_state
GO
IF OBJECT_ID('qrtz_fired_triggers', 'U') IS NOT NULL DROP TABLE qrtz_fired_triggers
GO
IF OBJECT_ID('qrtz_paused_trigger_grps', 'U') IS NOT NULL DROP TABLE qrtz_paused_trigger_grps
GO
IF OBJECT_ID('qrtz_calendars', 'U') IS NOT NULL DROP TABLE qrtz_calendars
GO
IF OBJECT_ID('qrtz_blob_triggers', 'U') IS NOT NULL DROP TABLE qrtz_blob_triggers
GO
IF OBJECT_ID('qrtz_simprop_triggers', 'U') IS NOT NULL DROP TABLE qrtz_simprop_triggers
GO
IF OBJECT_ID('qrtz_cron_triggers', 'U') IS NOT NULL DROP TABLE qrtz_cron_triggers
GO
IF OBJECT_ID('qrtz_simple_triggers', 'U') IS NOT NULL DROP TABLE qrtz_simple_triggers
GO
IF OBJECT_ID('qrtz_triggers', 'U') IS NOT NULL DROP TABLE qrtz_triggers
GO
IF OBJECT_ID('qrtz_job_details', 'U') IS NOT NULL DROP TABLE qrtz_job_details
GO
IF OBJECT_ID('viewentity', 'U') IS NOT NULL DROP TABLE viewentity
GO
IF OBJECT_ID('viewresource', 'U') IS NOT NULL DROP TABLE viewresource
GO
IF OBJECT_ID('viewparameter', 'U') IS NOT NULL DROP TABLE viewparameter
GO
IF OBJECT_ID('viewinstanceproperty', 'U') IS NOT NULL DROP TABLE viewinstanceproperty
GO
IF OBJECT_ID('viewinstancedata', 'U') IS NOT NULL DROP TABLE viewinstancedata
GO
IF OBJECT_ID('viewinstance', 'U') IS NOT NULL DROP TABLE viewinstance
GO
IF OBJECT_ID('viewmain', 'U') IS NOT NULL DROP TABLE viewmain
GO
IF OBJECT_ID('hostgroup_configuration', 'U') IS NOT NULL DROP TABLE hostgroup_configuration
GO
IF OBJECT_ID('blueprint_configuration', 'U') IS NOT NULL DROP TABLE blueprint_configuration
GO
IF OBJECT_ID('hostgroup_component', 'U') IS NOT NULL DROP TABLE hostgroup_component
GO
IF OBJECT_ID('hostgroup', 'U') IS NOT NULL DROP TABLE hostgroup
GO
IF OBJECT_ID('blueprint', 'U') IS NOT NULL DROP TABLE blueprint
GO
IF OBJECT_ID('configgrouphostmapping', 'U') IS NOT NULL DROP TABLE configgrouphostmapping
GO
IF OBJECT_ID('confgroupclusterconfigmapping', 'U') IS NOT NULL DROP TABLE confgroupclusterconfigmapping
GO
IF OBJECT_ID('configgroup', 'U') IS NOT NULL DROP TABLE configgroup
GO
IF OBJECT_ID('kerberos_principal_host', 'U') IS NOT NULL DROP TABLE kerberos_principal_host
GO
IF OBJECT_ID('kerberos_principal', 'U') IS NOT NULL DROP TABLE kerberos_principal
GO
IF OBJECT_ID('ambari_sequences', 'U') IS NOT NULL DROP TABLE ambari_sequences
GO
IF OBJECT_ID('metainfo', 'U') IS NOT NULL DROP TABLE metainfo
GO
IF OBJECT_ID('hostconfigmapping', 'U') IS NOT NULL DROP TABLE hostconfigmapping
GO
IF OBJECT_ID('key_value_store', 'U') IS NOT NULL DROP TABLE key_value_store
GO
IF OBJECT_ID('user_roles', 'U') IS NOT NULL DROP TABLE user_roles
GO
IF OBJECT_ID('ClusterHostMapping', 'U') IS NOT NULL DROP TABLE ClusterHostMapping
GO
IF OBJECT_ID('role_success_criteria', 'U') IS NOT NULL DROP TABLE role_success_criteria
GO
IF OBJECT_ID('execution_command', 'U') IS NOT NULL DROP TABLE execution_command
GO
IF OBJECT_ID('host_role_command', 'U') IS NOT NULL DROP TABLE host_role_command
GO
IF OBJECT_ID('members', 'U') IS NOT NULL DROP TABLE members
GO
IF OBJECT_ID('groups', 'U') IS NOT NULL DROP TABLE groups
GO
IF OBJECT_ID('users', 'U') IS NOT NULL DROP TABLE users
GO
IF OBJECT_ID('roles', 'U') IS NOT NULL DROP TABLE roles
GO
IF OBJECT_ID('stage', 'U') IS NOT NULL DROP TABLE stage
GO
IF OBJECT_ID('upgrade_item', 'U') IS NOT NULL DROP TABLE upgrade_item
GO
IF OBJECT_ID('upgrade_group', 'U') IS NOT NULL DROP TABLE upgrade_group
GO
IF OBJECT_ID('upgrade', 'U') IS NOT NULL DROP TABLE upgrade
GO
IF OBJECT_ID('requestoperationlevel', 'U') IS NOT NULL DROP TABLE requestoperationlevel
GO
IF OBJECT_ID('requestresourcefilter', 'U') IS NOT NULL DROP TABLE requestresourcefilter
GO
IF OBJECT_ID('requestschedulebatchrequest', 'U') IS NOT NULL DROP TABLE requestschedulebatchrequest
GO
IF OBJECT_ID('request', 'U') IS NOT NULL DROP TABLE request
GO
IF OBJECT_ID('requestschedule', 'U') IS NOT NULL DROP TABLE requestschedule
GO
IF OBJECT_ID('hoststate', 'U') IS NOT NULL DROP TABLE hoststate
GO
IF OBJECT_ID('hostcomponentdesiredstate', 'U') IS NOT NULL DROP TABLE hostcomponentdesiredstate
GO
IF OBJECT_ID('hostcomponentstate', 'U') IS NOT NULL DROP TABLE hostcomponentstate
GO
IF OBJECT_ID('host_version', 'U') IS NOT NULL DROP TABLE host_version
GO
IF OBJECT_ID('hosts', 'U') IS NOT NULL DROP TABLE hosts
GO
IF OBJECT_ID('servicedesiredstate', 'U') IS NOT NULL DROP TABLE servicedesiredstate
GO
IF OBJECT_ID('servicecomponentdesiredstate', 'U') IS NOT NULL DROP TABLE servicecomponentdesiredstate
GO
IF OBJECT_ID('clusterstate', 'U') IS NOT NULL DROP TABLE clusterstate
GO
IF OBJECT_ID('clusterservices', 'U') IS NOT NULL DROP TABLE clusterservices
GO
IF OBJECT_ID('clusterconfigmapping', 'U') IS NOT NULL DROP TABLE clusterconfigmapping
GO
IF OBJECT_ID('alert_notice', 'U') IS NOT NULL DROP TABLE alert_notice
GO
IF OBJECT_ID('alert_grouping', 'U') IS NOT NULL DROP TABLE alert_grouping
GO
IF OBJECT_ID('alert_group_target', 'U') IS NOT NULL DROP TABLE alert_group_target
GO
IF OBJECT_ID('alert_target_states', 'U') IS NOT NULL DROP TABLE alert_target_states
GO
IF OBJECT_ID('alert_target', 'U') IS NOT NULL DROP TABLE alert_target
GO
IF OBJECT_ID('alert_group', 'U') IS NOT NULL DROP TABLE alert_group
GO
IF OBJECT_ID('alert_current', 'U') IS NOT NULL DROP TABLE alert_current
GO
IF OBJECT_ID('alert_history', 'U') IS NOT NULL DROP TABLE alert_history
GO
IF OBJECT_ID('alert_definition', 'U') IS NOT NULL DROP TABLE alert_definition
GO
IF OBJECT_ID('serviceconfighosts', 'U') IS NOT NULL DROP TABLE serviceconfighosts
GO
IF OBJECT_ID('serviceconfigmapping', 'U') IS NOT NULL DROP TABLE serviceconfigmapping
GO
IF OBJECT_ID('serviceconfig', 'U') IS NOT NULL DROP TABLE serviceconfig
GO
IF OBJECT_ID('clusterconfig', 'U') IS NOT NULL DROP TABLE clusterconfig
GO
IF OBJECT_ID('cluster_version', 'U') IS NOT NULL DROP TABLE cluster_version
GO
IF OBJECT_ID('adminprivilege', 'U') IS NOT NULL DROP TABLE adminprivilege
GO
IF OBJECT_ID('adminpermission', 'U') IS NOT NULL DROP TABLE adminpermission
GO
IF OBJECT_ID('clusters', 'U') IS NOT NULL DROP TABLE clusters
GO
IF OBJECT_ID('adminresource', 'U') IS NOT NULL DROP TABLE adminresource
GO
IF OBJECT_ID('adminresourcetype', 'U') IS NOT NULL DROP TABLE adminresourcetype
GO
IF OBJECT_ID('adminprincipal', 'U') IS NOT NULL DROP TABLE adminprincipal
GO
IF OBJECT_ID('adminprincipaltype', 'U') IS NOT NULL DROP TABLE adminprincipaltype
GO
IF OBJECT_ID('repo_version', 'U') IS NOT NULL DROP TABLE repo_version
GO
IF OBJECT_ID('artifact', 'U') IS NOT NULL DROP TABLE artifact
GO
IF OBJECT_ID('stack', 'U') IS NOT NULL DROP TABLE stack
GO
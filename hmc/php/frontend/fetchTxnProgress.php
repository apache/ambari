<?php

include_once '../util/Logger.php';
include_once "../conf/Config.inc";
include_once "../orchestrator/HMC.php";

$dbPath = $GLOBALS["DB_PATH"];

$clusterName = $_GET['clusterName'];
$txnId = $_GET['txnId'];

//REZXXX $dummyDeployProgressData = array(
//REZXXX   // Sample 0
//REZXXX   array(
//REZXXX     'result' => 0,
//REZXXX     'error' => '',
//REZXXX     'processRunning' => 1,
//REZXXX     'subTxns' => array(
//REZXXX       array(
//REZXXX         'subTxnId' => 1,
//REZXXX         'parentSubTxnId' => 0,
//REZXXX         'state' => 'INSTALLING',
//REZXXX         'description' => 'orchestratortestcluster-INSTALLING',
//REZXXX         'progress' => 'IN_PROGRESS',
//REZXXX         'subTxnType' => 'CLUSTER',
//REZXXX         'rank' => 0
//REZXXX       ),
//REZXXX       array(
//REZXXX         'subTxnId' => 14,
//REZXXX         'parentSubTxnId' => 13,
//REZXXX         'state' => 'PENDING',
//REZXXX         'description' => 'HDFS-STARTING',
//REZXXX         'progress' => 'PENDING',
//REZXXX         'subTxnType' => 'SERVICE',
//REZXXX         'rank' => 1
//REZXXX       ),
//REZXXX       array(
//REZXXX         'subTxnId' => 16,
//REZXXX         'parentSubTxnId' => 15,
//REZXXX         'state' => 'PENDING',
//REZXXX         'description' => 'NAMENODE-STARTING',
//REZXXX         'progress' => 'PENDING',
//REZXXX         'subTxnType' => 'SERVICECOMPONENT',
//REZXXX         'rank' => 2
//REZXXX       ),
//REZXXX       array(
//REZXXX         'subTxnId' => 15,
//REZXXX         'parentSubTxnId' => 14,
//REZXXX         'state' => 'PENDING',
//REZXXX         'description' => 'DATANODE-STARTING',
//REZXXX         'progress' => 'PENDING',
//REZXXX         'subTxnType' => 'SERVICECOMPONENT',
//REZXXX         'rank' => 3
//REZXXX       ),
//REZXXX       array(
//REZXXX         'subTxnId' => 18,
//REZXXX         'parentSubTxnId' => 14,
//REZXXX         'state' => 'PENDING',
//REZXXX         'description' => 'SNAMENODE-STARTING',
//REZXXX         'progress' => 'PENDING',
//REZXXX         'subTxnType' => 'SERVICECOMPONENT',
//REZXXX         'rank' => 4
//REZXXX       ),
//REZXXX       array(
//REZXXX         'subTxnId' => 20,
//REZXXX         'parentSubTxnId' => 13,
//REZXXX         'state' => 'PENDING',
//REZXXX         'description' => 'MAPREDUCE-STARTING',
//REZXXX         'progress' => 'PENDING',
//REZXXX         'subTxnType' => 'SERVICE',
//REZXXX         'rank' => 5
//REZXXX       ),
//REZXXX       array(
//REZXXX         'subTxnId' => 22,
//REZXXX         'parentSubTxnId' => 20,
//REZXXX         'state' => 'PENDING',
//REZXXX         'description' => 'JOBTRACKER-STARTING',
//REZXXX         'progress' => 'PENDING',
//REZXXX         'subTxnType' => 'SERVICECOMPONENT',
//REZXXX         'rank' => 6
//REZXXX       ),
//REZXXX       array(
//REZXXX         'subTxnId' => 23,
//REZXXX         'parentSubTxnId' => 20,
//REZXXX         'state' => 'PENDING',
//REZXXX         'description' => 'TASKTRACKER-STARTING',
//REZXXX         'progress' => 'PENDING',
//REZXXX         'subTxnType' => 'SERVICECOMPONENT',
//REZXXX         'rank' => 7
//REZXXX       )
//REZXXX     )
//REZXXX   ),
//REZXXX   // Sample 1
//REZXXX   array(
//REZXXX     'result' => 0,
//REZXXX     'error' => '',
//REZXXX     'processRunning' => 1,
//REZXXX     'subTxns' => array(
//REZXXX       array(
//REZXXX         'subTxnId' => 1,
//REZXXX         'parentSubTxnId' => 0,
//REZXXX         'state' => 'INSTALLED',
//REZXXX         'description' => 'orchestratortestcluster-INSTALLED',
//REZXXX         'progress' => 'COMPLETED',
//REZXXX         'subTxnType' => 'CLUSTER',
//REZXXX         'rank' => 0
//REZXXX       ),
//REZXXX       array(
//REZXXX         'subTxnId' => 14,
//REZXXX         'parentSubTxnId' => 13,
//REZXXX         'state' => 'STARTING',
//REZXXX         'description' => 'HDFS-STARTING',
//REZXXX         'progress' => 'IN_PROGRESS',
//REZXXX         'subTxnType' => 'SERVICE',
//REZXXX         'rank' => 1
//REZXXX       ),
//REZXXX       array(
//REZXXX         'subTxnId' => 16,
//REZXXX         'parentSubTxnId' => 15,
//REZXXX         'state' => 'STARTING',
//REZXXX         'description' => 'NAMENODE-STARTING',
//REZXXX         'progress' => 'IN_PROGRESS',
//REZXXX         'subTxnType' => 'SERVICECOMPONENT',
//REZXXX         'rank' => 2
//REZXXX       ),
//REZXXX       array(
//REZXXX         'subTxnId' => 15,
//REZXXX         'parentSubTxnId' => 14,
//REZXXX         'state' => 'STARTING',
//REZXXX         'description' => 'DATANODE-STARTING',
//REZXXX         'progress' => 'IN_PROGRESS',
//REZXXX         'subTxnType' => 'SERVICECOMPONENT',
//REZXXX         'rank' => 3
//REZXXX       ),
//REZXXX       array(
//REZXXX         'subTxnId' => 18,
//REZXXX         'parentSubTxnId' => 14,
//REZXXX         'state' => 'PENDING',
//REZXXX         'description' => 'SNAMENODE-STARTING',
//REZXXX         'progress' => 'PENDING',
//REZXXX         'subTxnType' => 'SERVICECOMPONENT',
//REZXXX         'rank' => 4
//REZXXX       ),
//REZXXX       array(
//REZXXX         'subTxnId' => 20,
//REZXXX         'parentSubTxnId' => 13,
//REZXXX         'state' => 'PENDING',
//REZXXX         'description' => 'MAPREDUCE-STARTING',
//REZXXX         'progress' => 'PENDING',
//REZXXX         'subTxnType' => 'SERVICE',
//REZXXX         'rank' => 5
//REZXXX       ),
//REZXXX       array(
//REZXXX         'subTxnId' => 22,
//REZXXX         'parentSubTxnId' => 20,
//REZXXX         'state' => 'PENDING',
//REZXXX         'description' => 'JOBTRACKER-STARTING',
//REZXXX         'progress' => 'PENDING',
//REZXXX         'subTxnType' => 'SERVICECOMPONENT',
//REZXXX         'rank' => 6
//REZXXX       ),
//REZXXX       array(
//REZXXX         'subTxnId' => 23,
//REZXXX         'parentSubTxnId' => 20,
//REZXXX         'state' => 'PENDING',
//REZXXX         'description' => 'TASKTRACKER-STARTING',
//REZXXX         'progress' => 'PENDING',
//REZXXX         'subTxnType' => 'SERVICECOMPONENT',
//REZXXX         'rank' => 7
//REZXXX       )
//REZXXX     )
//REZXXX   ),
//REZXXX   // Sample 2
//REZXXX   array(
//REZXXX     'result' => 0,
//REZXXX     'error' => '',
//REZXXX     'processRunning' => 1,
//REZXXX     'subTxns' => array(
//REZXXX       array(
//REZXXX         'subTxnId' => 1,
//REZXXX         'parentSubTxnId' => 0,
//REZXXX         'state' => 'INSTALLED',
//REZXXX         'description' => 'orchestratortestcluster-INSTALLED',
//REZXXX         'progress' => 'COMPLETED',
//REZXXX         'subTxnType' => 'CLUSTER',
//REZXXX         'rank' => 0
//REZXXX       ),
//REZXXX       array(
//REZXXX         'subTxnId' => 14,
//REZXXX         'parentSubTxnId' => 13,
//REZXXX         'state' => 'STARTING',
//REZXXX         'description' => 'HDFS-STARTING',
//REZXXX         'progress' => 'IN_PROGRESS',
//REZXXX         'subTxnType' => 'SERVICE',
//REZXXX         'rank' => 1
//REZXXX       ),
//REZXXX       array(
//REZXXX         'subTxnId' => 16,
//REZXXX         'parentSubTxnId' => 15,
//REZXXX         'state' => 'STARTED',
//REZXXX         'description' => 'NAMENODE-STARTED',
//REZXXX         'progress' => 'COMPLETED',
//REZXXX         'subTxnType' => 'SERVICECOMPONENT',
//REZXXX         'rank' => 2
//REZXXX       ),
//REZXXX       array(
//REZXXX         'subTxnId' => 15,
//REZXXX         'parentSubTxnId' => 14,
//REZXXX         'state' => 'STARTING',
//REZXXX         'description' => 'DATANODE-STARTING',
//REZXXX         'progress' => 'IN_PROGRESS',
//REZXXX         'subTxnType' => 'SERVICECOMPONENT',
//REZXXX         'rank' => 3
//REZXXX       ),
//REZXXX       array(
//REZXXX         'subTxnId' => 18,
//REZXXX         'parentSubTxnId' => 14,
//REZXXX         'state' => 'PENDING',
//REZXXX         'description' => 'SNAMENODE-STARTING',
//REZXXX         'progress' => 'PENDING',
//REZXXX         'subTxnType' => 'SERVICECOMPONENT',
//REZXXX         'rank' => 4
//REZXXX       ),
//REZXXX       array(
//REZXXX         'subTxnId' => 20,
//REZXXX         'parentSubTxnId' => 13,
//REZXXX         'state' => 'PENDING',
//REZXXX         'description' => 'MAPREDUCE-STARTING',
//REZXXX         'progress' => 'PENDING',
//REZXXX         'subTxnType' => 'SERVICE',
//REZXXX         'rank' => 5
//REZXXX       ),
//REZXXX       array(
//REZXXX         'subTxnId' => 22,
//REZXXX         'parentSubTxnId' => 20,
//REZXXX         'state' => 'PENDING',
//REZXXX         'description' => 'JOBTRACKER-STARTING',
//REZXXX         'progress' => 'PENDING',
//REZXXX         'subTxnType' => 'SERVICECOMPONENT',
//REZXXX         'rank' => 6
//REZXXX       ),
//REZXXX       array(
//REZXXX         'subTxnId' => 23,
//REZXXX         'parentSubTxnId' => 20,
//REZXXX         'state' => 'PENDING',
//REZXXX         'description' => 'TASKTRACKER-STARTING',
//REZXXX         'progress' => 'PENDING',
//REZXXX         'subTxnType' => 'SERVICECOMPONENT',
//REZXXX         'rank' => 7
//REZXXX       )
//REZXXX     )
//REZXXX   ),
//REZXXX   // Sample 3
//REZXXX   array(
//REZXXX     'result' => 0,
//REZXXX     'error' => '',
//REZXXX     'processRunning' => 1,
//REZXXX     'subTxns' => array(
//REZXXX       array(
//REZXXX         'subTxnId' => 1,
//REZXXX         'parentSubTxnId' => 0,
//REZXXX         'state' => 'INSTALLED',
//REZXXX         'description' => 'orchestratortestcluster-INSTALLED',
//REZXXX         'progress' => 'COMPLETED',
//REZXXX         'subTxnType' => 'CLUSTER',
//REZXXX         'rank' => 0
//REZXXX       ),
//REZXXX       array(
//REZXXX         'subTxnId' => 14,
//REZXXX         'parentSubTxnId' => 13,
//REZXXX         'state' => 'STARTING',
//REZXXX         'description' => 'HDFS-STARTING',
//REZXXX         'progress' => 'IN_PROGRESS',
//REZXXX         'subTxnType' => 'SERVICE',
//REZXXX         'rank' => 1
//REZXXX       ),
//REZXXX       array(
//REZXXX         'subTxnId' => 16,
//REZXXX         'parentSubTxnId' => 15,
//REZXXX         'state' => 'STARTED',
//REZXXX         'description' => 'NAMENODE-STARTED',
//REZXXX         'progress' => 'COMPLETED',
//REZXXX         'subTxnType' => 'SERVICECOMPONENT',
//REZXXX         'rank' => 2
//REZXXX       ),
//REZXXX       array(
//REZXXX         'subTxnId' => 15,
//REZXXX         'parentSubTxnId' => 14,
//REZXXX         'state' => 'STARTED',
//REZXXX         'description' => 'DATANODE-STARTED',
//REZXXX         'progress' => 'COMPLETED',
//REZXXX         'subTxnType' => 'SERVICECOMPONENT',
//REZXXX         'rank' => 3
//REZXXX       ),
//REZXXX       array(
//REZXXX         'subTxnId' => 18,
//REZXXX         'parentSubTxnId' => 14,
//REZXXX         'state' => 'STARTING',
//REZXXX         'description' => 'SNAMENODE-STARTING',
//REZXXX         'progress' => 'IN_PROGRESS',
//REZXXX         'subTxnType' => 'SERVICECOMPONENT',
//REZXXX         'rank' => 4
//REZXXX       ),
//REZXXX       array(
//REZXXX         'subTxnId' => 20,
//REZXXX         'parentSubTxnId' => 13,
//REZXXX         'state' => 'PENDING',
//REZXXX         'description' => 'MAPREDUCE-STARTING',
//REZXXX         'progress' => 'PENDING',
//REZXXX         'subTxnType' => 'SERVICE',
//REZXXX         'rank' => 5
//REZXXX       ),
//REZXXX       array(
//REZXXX         'subTxnId' => 22,
//REZXXX         'parentSubTxnId' => 20,
//REZXXX         'state' => 'PENDING',
//REZXXX         'description' => 'JOBTRACKER-STARTING',
//REZXXX         'progress' => 'PENDING',
//REZXXX         'subTxnType' => 'SERVICECOMPONENT',
//REZXXX         'rank' => 6
//REZXXX       ),
//REZXXX       array(
//REZXXX         'subTxnId' => 23,
//REZXXX         'parentSubTxnId' => 20,
//REZXXX         'state' => 'PENDING',
//REZXXX         'description' => 'TASKTRACKER-STARTING',
//REZXXX         'progress' => 'PENDING',
//REZXXX         'subTxnType' => 'SERVICECOMPONENT',
//REZXXX         'rank' => 7
//REZXXX       )
//REZXXX     )
//REZXXX   ),
//REZXXX   // Sample 4
//REZXXX   array(
//REZXXX     'result' => 0,
//REZXXX     'error' => '',
//REZXXX     'processRunning' => 1,
//REZXXX     'subTxns' => array(
//REZXXX       array(
//REZXXX         'subTxnId' => 1,
//REZXXX         'parentSubTxnId' => 0,
//REZXXX         'state' => 'INSTALLED',
//REZXXX         'description' => 'orchestratortestcluster-INSTALLED',
//REZXXX         'progress' => 'COMPLETED',
//REZXXX         'subTxnType' => 'CLUSTER',
//REZXXX         'rank' => 0
//REZXXX       ),
//REZXXX       array(
//REZXXX         'subTxnId' => 14,
//REZXXX         'parentSubTxnId' => 13,
//REZXXX         'state' => 'STARTED',
//REZXXX         'description' => 'HDFS-STARTED',
//REZXXX         'progress' => 'COMPLETED',
//REZXXX         'subTxnType' => 'SERVICE',
//REZXXX         'rank' => 1
//REZXXX       ),
//REZXXX       array(
//REZXXX         'subTxnId' => 16,
//REZXXX         'parentSubTxnId' => 15,
//REZXXX         'state' => 'STARTED',
//REZXXX         'description' => 'NAMENODE-STARTED',
//REZXXX         'progress' => 'COMPLETED',
//REZXXX         'subTxnType' => 'SERVICECOMPONENT',
//REZXXX         'rank' => 2
//REZXXX       ),
//REZXXX       array(
//REZXXX         'subTxnId' => 15,
//REZXXX         'parentSubTxnId' => 14,
//REZXXX         'state' => 'STARTED',
//REZXXX         'description' => 'DATANODE-STARTED',
//REZXXX         'progress' => 'COMPLETED',
//REZXXX         'subTxnType' => 'SERVICECOMPONENT',
//REZXXX         'rank' => 3
//REZXXX       ),
//REZXXX       array(
//REZXXX         'subTxnId' => 18,
//REZXXX         'parentSubTxnId' => 14,
//REZXXX         'state' => 'STARTED',
//REZXXX         'description' => 'SNAMENODE-STARTED',
//REZXXX         'progress' => 'COMPLETED',
//REZXXX         'subTxnType' => 'SERVICECOMPONENT',
//REZXXX         'rank' => 4
//REZXXX       ),
//REZXXX       array(
//REZXXX         'subTxnId' => 20,
//REZXXX         'parentSubTxnId' => 13,
//REZXXX         'state' => 'STARTING',
//REZXXX         'description' => 'MAPREDUCE-STARTING',
//REZXXX         'progress' => 'IN_PROGRESS',
//REZXXX         'subTxnType' => 'SERVICE',
//REZXXX         'rank' => 5
//REZXXX       ),
//REZXXX       array(
//REZXXX         'subTxnId' => 22,
//REZXXX         'parentSubTxnId' => 20,
//REZXXX         'state' => 'STARTING',
//REZXXX         'description' => 'JOBTRACKER-STARTING',
//REZXXX         'progress' => 'IN_PROGRESS',
//REZXXX         'subTxnType' => 'SERVICECOMPONENT',
//REZXXX         'rank' => 6
//REZXXX       ),
//REZXXX       array(
//REZXXX         'subTxnId' => 23,
//REZXXX         'parentSubTxnId' => 20,
//REZXXX         'state' => 'PENDING',
//REZXXX         'description' => 'TASKTRACKER-STARTING',
//REZXXX         'progress' => 'PENDING',
//REZXXX         'subTxnType' => 'SERVICECOMPONENT',
//REZXXX         'rank' => 7
//REZXXX       )
//REZXXX     )
//REZXXX   ),
//REZXXX   // Sample 5
//REZXXX   array(
//REZXXX     'result' => 0,
//REZXXX     'error' => '',
//REZXXX     'processRunning' => 1,
//REZXXX     'subTxns' => array(
//REZXXX       array(
//REZXXX         'subTxnId' => 1,
//REZXXX         'parentSubTxnId' => 0,
//REZXXX         'state' => 'INSTALLED',
//REZXXX         'description' => 'orchestratortestcluster-INSTALLED',
//REZXXX         'progress' => 'COMPLETED',
//REZXXX         'subTxnType' => 'CLUSTER',
//REZXXX         'rank' => 0
//REZXXX       ),
//REZXXX       array(
//REZXXX         'subTxnId' => 14,
//REZXXX         'parentSubTxnId' => 13,
//REZXXX         'state' => 'STARTED',
//REZXXX         'description' => 'HDFS-STARTED',
//REZXXX         'progress' => 'COMPLETED',
//REZXXX         'subTxnType' => 'SERVICE',
//REZXXX         'rank' => 1
//REZXXX       ),
//REZXXX       array(
//REZXXX         'subTxnId' => 16,
//REZXXX         'parentSubTxnId' => 15,
//REZXXX         'state' => 'STARTED',
//REZXXX         'description' => 'NAMENODE-STARTED',
//REZXXX         'progress' => 'COMPLETED',
//REZXXX         'subTxnType' => 'SERVICECOMPONENT',
//REZXXX         'rank' => 2
//REZXXX       ),
//REZXXX       array(
//REZXXX         'subTxnId' => 15,
//REZXXX         'parentSubTxnId' => 14,
//REZXXX         'state' => 'STARTED',
//REZXXX         'description' => 'DATANODE-STARTED',
//REZXXX         'progress' => 'COMPLETED',
//REZXXX         'subTxnType' => 'SERVICECOMPONENT',
//REZXXX         'rank' => 3
//REZXXX       ),
//REZXXX       array(
//REZXXX         'subTxnId' => 18,
//REZXXX         'parentSubTxnId' => 14,
//REZXXX         'state' => 'STARTED',
//REZXXX         'description' => 'SNAMENODE-STARTED',
//REZXXX         'progress' => 'COMPLETED',
//REZXXX         'subTxnType' => 'SERVICECOMPONENT',
//REZXXX         'rank' => 4
//REZXXX       ),
//REZXXX       array(
//REZXXX         'subTxnId' => 20,
//REZXXX         'parentSubTxnId' => 13,
//REZXXX         'state' => 'STARTING',
//REZXXX         'description' => 'MAPREDUCE-STARTING',
//REZXXX         'progress' => 'IN_PROGRESS',
//REZXXX         'subTxnType' => 'SERVICE',
//REZXXX         'rank' => 5
//REZXXX       ),
//REZXXX       array(
//REZXXX         'subTxnId' => 22,
//REZXXX         'parentSubTxnId' => 20,
//REZXXX         'state' => 'STARTED',
//REZXXX         'description' => 'JOBTRACKER-STARTED',
//REZXXX         'progress' => 'COMPLETED',
//REZXXX         'subTxnType' => 'SERVICECOMPONENT',
//REZXXX         'rank' => 6
//REZXXX       ),
//REZXXX       array(
//REZXXX         'subTxnId' => 23,
//REZXXX         'parentSubTxnId' => 20,
//REZXXX         'state' => 'STARTING',
//REZXXX         'description' => 'TASKTRACKER-STARTING',
//REZXXX         'progress' => 'IN_PROGRESS',
//REZXXX         'subTxnType' => 'SERVICECOMPONENT',
//REZXXX         'rank' => 7
//REZXXX       )
//REZXXX     )
//REZXXX   ),
//REZXXX   // Sample 6
//REZXXX   array(
//REZXXX     'result' => 0,
//REZXXX     'error' => '',
//REZXXX     'processRunning' => 0,
//REZXXX     'subTxns' => array(
//REZXXX       array(
//REZXXX         'subTxnId' => 1,
//REZXXX         'parentSubTxnId' => 0,
//REZXXX         'state' => 'INSTALLED',
//REZXXX         'description' => 'orchestratortestcluster-INSTALLED',
//REZXXX         'progress' => 'COMPLETED',
//REZXXX         'subTxnType' => 'CLUSTER',
//REZXXX         'rank' => 0
//REZXXX       ),
//REZXXX       array(
//REZXXX         'subTxnId' => 14,
//REZXXX         'parentSubTxnId' => 13,
//REZXXX         'state' => 'STARTED',
//REZXXX         'description' => 'HDFS-STARTED',
//REZXXX         'progress' => 'COMPLETED',
//REZXXX         'subTxnType' => 'SERVICE',
//REZXXX         'rank' => 1
//REZXXX       ),
//REZXXX       array(
//REZXXX         'subTxnId' => 16,
//REZXXX         'parentSubTxnId' => 15,
//REZXXX         'state' => 'STARTED',
//REZXXX         'description' => 'NAMENODE-STARTED',
//REZXXX         'progress' => 'COMPLETED',
//REZXXX         'subTxnType' => 'SERVICECOMPONENT',
//REZXXX         'rank' => 2
//REZXXX       ),
//REZXXX       array(
//REZXXX         'subTxnId' => 15,
//REZXXX         'parentSubTxnId' => 14,
//REZXXX         'state' => 'STARTED',
//REZXXX         'description' => 'DATANODE-STARTED',
//REZXXX         'progress' => 'COMPLETED',
//REZXXX         'subTxnType' => 'SERVICECOMPONENT',
//REZXXX         'rank' => 3
//REZXXX       ),
//REZXXX       array(
//REZXXX         'subTxnId' => 18,
//REZXXX         'parentSubTxnId' => 14,
//REZXXX         'state' => 'STARTED',
//REZXXX         'description' => 'SNAMENODE-STARTED',
//REZXXX         'progress' => 'COMPLETED',
//REZXXX         'subTxnType' => 'SERVICECOMPONENT',
//REZXXX         'rank' => 4
//REZXXX       ),
//REZXXX       array(
//REZXXX         'subTxnId' => 20,
//REZXXX         'parentSubTxnId' => 13,
//REZXXX         'state' => 'STARTED',
//REZXXX         'description' => 'MAPREDUCE-STARTED',
//REZXXX         'progress' => 'COMPLETED',
//REZXXX         'subTxnType' => 'SERVICE',
//REZXXX         'rank' => 5
//REZXXX       ),
//REZXXX       array(
//REZXXX         'subTxnId' => 22,
//REZXXX         'parentSubTxnId' => 20,
//REZXXX         'state' => 'STARTED',
//REZXXX         'description' => 'JOBTRACKER-STARTED',
//REZXXX         'progress' => 'COMPLETED',
//REZXXX         'subTxnType' => 'SERVICECOMPONENT',
//REZXXX         'rank' => 6
//REZXXX       ),
//REZXXX       array(
//REZXXX         'subTxnId' => 23,
//REZXXX         'parentSubTxnId' => 20,
//REZXXX         'state' => 'STARTED',
//REZXXX         'description' => 'TASKTRACKER-STARTED',
//REZXXX         'progress' => 'COMPLETED',
//REZXXX         'subTxnType' => 'SERVICECOMPONENT',
//REZXXX         'rank' => 7
//REZXXX       )
//REZXXX     )
//REZXXX   )
//REZXXX );
//REZXXX
//REZXXX define('LAST_PROGRESS_STATE_INDEX_FILE', '/tmp/rezDeployProgressStateIndex' . $txnId);
//REZXXX
//REZXXX function fetchLastProgressStateIndex()
//REZXXX {
//REZXXX   $lastProgressStateIndex = 0;
//REZXXX
//REZXXX   if( file_exists(LAST_PROGRESS_STATE_INDEX_FILE) )
//REZXXX   {
//REZXXX     $lastProgressStateIndex = trim( file_get_contents(LAST_PROGRESS_STATE_INDEX_FILE) );
//REZXXX   }
//REZXXX
//REZXXX   return $lastProgressStateIndex;
//REZXXX }
//REZXXX
//REZXXX function storeLastProgressStateIndex( $latestProgressStateIndex )
//REZXXX {
//REZXXX   file_put_contents(LAST_PROGRESS_STATE_INDEX_FILE, $latestProgressStateIndex);
//REZXXX }

function fetchTxnProgress( $txnId )
{
  global $dbPath;
  global $clusterName;

  $hmc = new HMC($dbPath, $clusterName);

  $progress = $hmc->getProgress($txnId);

//REZXXX  global $dummyDeployProgressData;
//REZXXX
//REZXXX  $lastProgressStateIndex = fetchLastProgressStateIndex();
//REZXXX
//REZXXX  $progress = $dummyDeployProgressData[$lastProgressStateIndex];
//REZXXX
//REZXXX  $currentProgressStateIndex = $lastProgressStateIndex;
//REZXXX
//REZXXX  /* Progress to the next state only if we haven't already reached the end.
//REZXXX   *
//REZXXX   * We expect callers to stop to call this webservice once this condition is
//REZXXX   * reached in any case, but let's be safe all the same.
//REZXXX   */
//REZXXX  if( $lastProgressStateIndex < count($dummyDeployProgressData) )
//REZXXX  {
//REZXXX    /* Randomize the rate of our progress, in steps of 1. */
//REZXXX    $currentProgressStateIndex = (rand() % 2) ? ($lastProgressStateIndex + 1) : ($lastProgressStateIndex);
//REZXXX
//REZXXX    /* Update our disk cookie. */
//REZXXX    storeLastProgressStateIndex( $currentProgressStateIndex );
//REZXXX  }

  return $progress;
}

function sortProgressStatesByRank( $first, $second )
{
  if( $first['rank'] == $second['rank'] )
  {
    return 0;
  }

  return ($first['rank'] < $second['rank']) ? -1 : 1;
}

$progress = fetchTxnProgress($txnId);

// TODO XXX Check for $progress['result'] and $progress['error'] here, before proceeding.

/* Tack on some additional state to make life on the frontend easier. */
$progress['encounteredError'] = false;

/* Marker to keep track of whether at least one subTxn has been kicked off. */
$atLeastOneSubTxnInProgress = false;

/* Sort the subTxns array inside $progress by rank, and then remove all notion
 * of rank from the sorted array we're going to return.
 */
usort( $progress['subTxns'], 'sortProgressStatesByRank' );

foreach( $progress['subTxns'] as &$progressSubTxn )
{
  unset( $progressSubTxn['rank'] );

  /* Any one subTxn failing means we want the frontend to bail. */
  if( $progressSubTxn['progress'] == 'FAILED' )
  {
    $progress['encounteredError'] = true;
  }
  /* We need to make sure at least one subTxn is not pending before
   * sending a progress report back to the frontend - if not, the
   * progress states aren't yet finalized and will change across
   * invocations to this webservice, so we prefer to wait before
   * showing anything.
   */
  if( $progressSubTxn['progress'] != 'PENDING' )
  {
    $atLeastOneSubTxnInProgress = true;
  }
}

/* Clean up some more remnants that we don't need on the frontend. */
unset( $progress['result'] );
unset( $progress['error'] );

/* Create the output data... */
$jsonOutput = array(
    'clusterName' => $clusterName,
    'txnId' => $txnId,
    'progress' => $atLeastOneSubTxnInProgress ? $progress : null );

/* ...and spit it out. */
header("Content-type: application/json");

print (json_encode($jsonOutput));

?>

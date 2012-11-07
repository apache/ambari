#!/usr/bin/python

import sys
sys.path.append('../src')

import sys_logger

tests_passed = 0
tests_failed = 0
def test_log_tvi_msg(msg):
    global tests_passed, tests_failed
    if msg == expected_log_msg:
        print 'Test Passed'
        tests_passed += 1
    else:
        print '*** TEST FAILED ***'
        print 'Expected MSG: {0}'.format(expected_log_msg)
        print 'Actual MSG  : {0}'.format(msg)
        tests_failed += 1

sys_logger.log_tvi_msg = test_log_tvi_msg

def test(tvi_rule, expected_msg, arg1, arg2, arg3, arg4, arg5):
    sys.stdout.write(tvi_rule + ': ')
    global expected_log_msg
    expected_log_msg = expected_msg
    sys_logger.generate_tvi_log_msg(arg1, arg2, arg3, arg4, arg5)

def summary():
    total_tests = tests_passed + tests_failed
    print '\nTests Run: {0}'.format(total_tests)
    print 'Passed: {0}, Failed: {1}'.format(tests_passed, tests_failed)
    if not tests_failed:
        print 'SUCCESS! All tests pass.'


# Hadoop_Host_Down
test('Hadoop_Host_Down',
     'Critical: Hadoop: host_down# Event Host=MY_HOST(CRITICAL), PING FAILED - Packet loss = 100%, RTA = 0.00 ms',
     'HARD', '1', 'CRITICAL', 'Host::Ping', 'Event Host=MY_HOST(CRITICAL), PING FAILED - Packet loss = 100%, RTA = 0.00 ms')

test('Hadoop_Host_Down:OK',
    'OK: Hadoop: host_down_ok# Event Host=MY_HOST(OK), PING SUCCESS - Packet loss = 0%, RTA = 1.00 ms',
    'HARD', '1', 'OK', 'Host::Ping', 'Event Host=MY_HOST(OK), PING SUCCESS - Packet loss = 0%, RTA = 1.00 ms')

# Hadoop_Master_Daemon_CPU_Utilization
test('Hadoop_Master_Daemon_CPU_Utilization',
     'Critical: Hadoop: master_cpu_utilization# Event Host=MY_HOST Service Description=HBASEMASTER::HBaseMaster CPU utilization(CRITICAL), 4 CPU, average load 2.5%  200%',
     'HARD', '1', 'CRITICAL', 'HBASEMASTER::HBaseMaster CPU utilization',
     'Event Host=MY_HOST Service Description=HBASEMASTER::HBaseMaster CPU utilization(CRITICAL), 4 CPU, average load 2.5%  200%')

test('Hadoop_Master_Daemon_CPU_Utilization:Degraded',
    'Degraded: Hadoop: master_cpu_utilization# Event Host=MY_HOST Service Description=HBASEMASTER::HBaseMaster CPU utilization(CRITICAL), 4 CPU, average load 2.5%  200%',
    'HARD', '1', 'WARNING', 'HBASEMASTER::HBaseMaster CPU utilization',
    'Event Host=MY_HOST Service Description=HBASEMASTER::HBaseMaster CPU utilization(CRITICAL), 4 CPU, average load 2.5%  200%')

test('Hadoop_Master_Daemon_CPU_Utilization:OK',
    'OK: Hadoop: master_cpu_utilization_ok# Event Host=MY_HOST Service Description=HBASEMASTER::HBaseMaster CPU utilization(OK), 4 CPU, average load 2.5%  200%',
    'HARD', '1', 'OK', 'HBASEMASTER::HBaseMaster CPU utilization',
    'Event Host=MY_HOST Service Description=HBASEMASTER::HBaseMaster CPU utilization(OK), 4 CPU, average load 2.5%  200%')

# Hadoop_HDFS_Percent_Capacity
test('Hadoop_HDFS_Percent_Capacity',
     'Critical: Hadoop: hdfs_percent_capacity# Event Host=MY_HOST Service Description=HDFS::HDFS Capacity utilization(CRITICAL),DFSUsedGB:0.1, DFSTotalGB:1568.7',
     'HARD', '1', 'CRITICAL', 'HDFS::HDFS Capacity utilization',
     'Event Host=MY_HOST Service Description=HDFS::HDFS Capacity utilization(CRITICAL),DFSUsedGB:0.1, DFSTotalGB:1568.7')

test('Hadoop_HDFS_Percent_Capacity:OK',
    'OK: Hadoop: hdfs_percent_capacity_ok# Event Host=MY_HOST Service Description=HDFS::HDFS Capacity utilization(OK),DFSUsedGB:0.1, DFSTotalGB:1568.7',
    'HARD', '1', 'OK', 'HDFS::HDFS Capacity utilization',
    'Event Host=MY_HOST Service Description=HDFS::HDFS Capacity utilization(OK),DFSUsedGB:0.1, DFSTotalGB:1568.7')

# Hadoop_HDFS_Corrupt_Missing_Blocks
test('Hadoop_HDFS_Corrupt_Missing_Blocks',
     'Critical: Hadoop: hdfs_block# Event Host=MY_HOST Service Description=HDFS::Corrupt/Missing blocks(CRITICAL), corrupt_blocks:0, missing_blocks:0, total_blocks:147',
     'HARD', '1', 'CRITICAL', 'HDFS::Corrupt/Missing blocks',
     'Event Host=MY_HOST Service Description=HDFS::Corrupt/Missing blocks(CRITICAL), corrupt_blocks:0, missing_blocks:0, total_blocks:147')

test('Hadoop_HDFS_Corrupt_Missing_Blocks:OK',
    'OK: Hadoop: hdfs_block_ok# Event Host=MY_HOST Service Description=HDFS::Corrupt/Missing blocks(OK), corrupt_blocks:0, missing_blocks:0, total_blocks:147',
    'HARD', '1', 'OK', 'HDFS::Corrupt/Missing blocks',
    'Event Host=MY_HOST Service Description=HDFS::Corrupt/Missing blocks(OK), corrupt_blocks:0, missing_blocks:0, total_blocks:147')

# Hadoop_NameNode_Edit_Log_Dir_Write
test('Hadoop_NameNode_Edit_Log_Dir_Write',
     'Critical: Hadoop: namenode_edit_log_write# SERVICE MSG',
     'HARD', '1', 'CRITICAL', 'NAMENODE::Namenode Edit logs directory status', 'SERVICE MSG')

test('Hadoop_NameNode_Edit_Log_Dir_Write:OK',
    'OK: Hadoop: namenode_edit_log_write_ok# SERVICE MSG',
    'HARD', '1', 'OK', 'NAMENODE::Namenode Edit logs directory status', 'SERVICE MSG')

# Hadoop_DataNode_Down
test('Hadoop_DataNode_Down',
     'Critical: Hadoop: datanode_down# SERVICE MSG',
     'HARD', '1', 'CRITICAL', 'HDFS::Percent DataNodes down','SERVICE MSG')

test('Hadoop_DataNode_Down:OK',
    'OK: Hadoop: datanode_down_ok# SERVICE MSG',
    'HARD', '1', 'OK', 'HDFS::Percent DataNodes down','SERVICE MSG')

# Hadoop_DataNode_Process_Down
test('Hadoop_DataNode_Process_Down',
     'Critical: Hadoop: datanode_process_down# SERVICE MSG',
     'HARD', '1', 'CRITICAL', 'DATANODE::Process down', 'SERVICE MSG')

test('Hadoop_DataNode_Process_Down:OK',
    'OK: Hadoop: datanode_process_down_ok# SERVICE MSG',
    'HARD', '1', 'OK', 'DATANODE::Process down', 'SERVICE MSG')

# Hadoop_Percent_DataNodes_Storage_Full
test('Hadoop_Percent_DataNodes_Storage_Full',
     'Critical: Hadoop: datanodes_percent_storage_full# SERVICE MSG',
     'HARD', '1', 'CRITICAL', 'HDFS::Percent DataNodes storage full', 'SERVICE MSG')

test('Hadoop_Percent_DataNodes_Storage_Full:OK',
    'OK: Hadoop: datanodes_percent_storage_full_ok# SERVICE MSG',
    'HARD', '1', 'OK', 'HDFS::Percent DataNodes storage full', 'SERVICE MSG')

# Hadoop_NameNode_Process_Down
test('Hadoop_NameNode_Process_Down:CRITICAL',
     'Fatal: Hadoop: namenode_process_down# SERVICE MSG',
     'HARD', '1', 'CRITICAL', 'NAMENODE::Namenode Process down', 'SERVICE MSG')

test('Hadoop_NameNode_Process_Down:WARNING',
    'Fatal: Hadoop: namenode_process_down# SERVICE MSG',
    'HARD', '1', 'WARNING', 'NAMENODE::Namenode Process down', 'SERVICE MSG')

test('Hadoop_NameNode_Process_Down:UNKNOWN',
    'Fatal: Hadoop: namenode_process_down# SERVICE MSG',
    'HARD', '1', 'UNKNOWN', 'NAMENODE::Namenode Process down', 'SERVICE MSG')

test('Hadoop_NameNode_Process_Down:OK',
    'OK: Hadoop: namenode_process_down_ok# SERVICE MSG',
    'HARD', '1', 'OK', 'NAMENODE::Namenode Process down', 'SERVICE MSG')

# Hadoop_Secondary_NameNode_Process_Down
test('Hadoop_Secondary_NameNode_Process_Down',
    'Critical: Hadoop: secondary_namenode_process_down# SERVICE MSG',
    'HARD', '1', 'CRITICAL', 'NAMENODE::Secondary Namenode Process down', 'SERVICE MSG')

test('Hadoop_Secondary_NameNode_Process_Down:OK',
    'OK: Hadoop: secondary_namenode_process_down_ok# SERVICE MSG',
    'HARD', '1', 'OK', 'NAMENODE::Secondary Namenode Process down', 'SERVICE MSG')

# Hadoop_NameNode_RPC_Latency
test('Hadoop_NameNode_RPC_Latency',
     'Critical: Hadoop: namenode_rpc_latency# SERVICE MSG',
     'HARD', '1', 'CRITICAL', 'HDFS::Namenode RPC Latency', 'SERVICE MSG')

test('Hadoop_NameNode_RPC_Latency:Degraded',
    'Degraded: Hadoop: namenode_rpc_latency# SERVICE MSG',
    'HARD', '1', 'WARNING', 'HDFS::Namenode RPC Latency', 'SERVICE MSG')

test('Hadoop_NameNode_RPC_Latency:OK',
    'OK: Hadoop: namenode_rpc_latency_ok# SERVICE MSG',
    'HARD', '1', 'OK', 'HDFS::Namenode RPC Latency', 'SERVICE MSG')

# Hadoop_DataNodes_Storage_Full
test('Hadoop_DataNodes_Storage_Full',
     'Critical: Hadoop: datanodes_storage_full# SERVICE MSG',
     'HARD', '1', 'CRITICAL', 'DATANODE::Storage full', 'SERVICE MSG')

test('Hadoop_DataNodes_Storage_Full:OK',
    'OK: Hadoop: datanodes_storage_full_ok# SERVICE MSG',
    'HARD', '1', 'OK', 'DATANODE::Storage full', 'SERVICE MSG')

# Hadoop_JobTracker_Process_Down
test('Hadoop_JobTracker_Process_Down',
     'Critical: Hadoop: jobtracker_process_down# SERVICE MSG',
     'HARD', '1', 'CRITICAL', 'JOBTRACKER::Jobtracker Process down', 'SERVICE MSG')

test('Hadoop_JobTracker_Process_Down:OK',
    'OK: Hadoop: jobtracker_process_down_ok# SERVICE MSG',
    'HARD', '1', 'OK', 'JOBTRACKER::Jobtracker Process down', 'SERVICE MSG')

# Hadoop_JobTracker_RPC_Latency
test('Hadoop_JobTracker_RPC_Latency',
     'Critical: Hadoop: jobtracker_rpc_latency# SERVICE MSG',
     'HARD', '1', 'CRITICAL', 'MAPREDUCE::JobTracker RPC Latency', 'SERVICE MSG')

test('Hadoop_JobTracker_RPC_Latency:Degraded',
    'Degraded: Hadoop: jobtracker_rpc_latency# SERVICE MSG',
    'HARD', '1', 'WARNING', 'MAPREDUCE::JobTracker RPC Latency', 'SERVICE MSG')

test('Hadoop_JobTracker_RPC_Latency:OK',
    'OK: Hadoop: jobtracker_rpc_latency_ok# SERVICE MSG',
    'HARD', '1', 'OK', 'MAPREDUCE::JobTracker RPC Latency', 'SERVICE MSG')

# Hadoop_JobTracker_CPU_Utilization
test('Hadoop_JobTracker_CPU_Utilization',
    'Critical: Hadoop: jobtracker_cpu_utilization# SERVICE MSG',
    'HARD', '1', 'CRITICAL', 'JOBTRACKER::Jobtracker CPU utilization', 'SERVICE MSG')

test('Hadoop_JobTracker_CPU_Utilization:Degraded',
    'Degraded: Hadoop: jobtracker_cpu_utilization# SERVICE MSG',
    'HARD', '1', 'WARNING', 'JOBTRACKER::Jobtracker CPU utilization', 'SERVICE MSG')

test('Hadoop_JobTracker_CPU_Utilization:OK',
    'OK: Hadoop: jobtracker_cpu_utilization_ok# SERVICE MSG',
    'HARD', '1', 'OK', 'JOBTRACKER::Jobtracker CPU utilization', 'SERVICE MSG')

# Hadoop_TaskTracker_Down
test('Hadoop_TaskTracker_Down',
     'Critical: Hadoop: tasktrackers_down# SERVICE MSG',
     'HARD', '1', 'CRITICAL', 'MAPREDUCE::Percent TaskTrackers down', 'SERVICE MSG')

test('Hadoop_TaskTracker_Down:OK',
    'OK: Hadoop: tasktrackers_down_ok# SERVICE MSG',
    'HARD', '1', 'OK', 'MAPREDUCE::Percent TaskTrackers down', 'SERVICE MSG')

# Hadoop_TaskTracker_Process_Down
test('Hadoop_TaskTracker_Process_Down',
     'Critical: Hadoop: tasktracker_process_down# SERVICE MSG',
     'HARD', '1', 'CRITICAL', 'TASKTRACKER::Process down', 'SERVICE MSG')

test('Hadoop_TaskTracker_Process_Down:OK',
    'OK: Hadoop: tasktracker_process_down_ok# SERVICE MSG',
    'HARD', '1', 'OK', 'TASKTRACKER::Process down', 'SERVICE MSG')

# Hadoop_HBaseMaster_Process_Down
test('Hadoop_HBaseMaster_Process_Down',
     'Critical: Hadoop: hbasemaster_process_down# SERVICE MSG',
     'HARD', '1', 'CRITICAL', 'HBASEMASTER::HBaseMaster Process down', 'SERVICE MSG')

test('Hadoop_HBaseMaster_Process_Down:OK',
    'OK: Hadoop: hbasemaster_process_down_ok# SERVICE MSG',
    'HARD', '1', 'OK', 'HBASEMASTER::HBaseMaster Process down', 'SERVICE MSG')

# Hadoop_RegionServer_Process_Down
test('Hadoop_RegionServer_Process_Down',
     'Critical: Hadoop: regionserver_process_down# SERVICE MSG',
     'HARD', '1', 'CRITICAL', 'REGIONSERVER::Process down', 'SERVICE MSG')

test('Hadoop_RegionServer_Process_Down:OK',
    'OK: Hadoop: regionserver_process_down_ok# SERVICE MSG',
    'HARD', '1', 'OK', 'REGIONSERVER::Process down', 'SERVICE MSG')

# Hadoop_RegionServer_Down
test('Hadoop_RegionServer_Down',
     'Critical: Hadoop: regionservers_down# SERVICE MSG',
     'HARD', '1', 'CRITICAL', 'HBASE::Percent region servers down', 'SERVICE MSG')

test('Hadoop_RegionServer_Down:OK',
    'OK: Hadoop: regionservers_down_ok# SERVICE MSG',
    'HARD', '1', 'OK', 'HBASE::Percent region servers down', 'SERVICE MSG')

# Hadoop_Hive_Metastore_Process_Down
test('Hadoop_Hive_Metastore_Process_Down',
     'Critical: Hadoop: hive_metastore_process_down# SERVICE MSG',
     'HARD', '1', 'CRITICAL', 'HIVE-METASTORE::HIVE-METASTORE status check', 'SERVICE MSG')

test('Hadoop_Hive_Metastore_Process_Down:OK',
    'OK: Hadoop: hive_metastore_process_down_ok# SERVICE MSG',
    'HARD', '1', 'OK', 'HIVE-METASTORE::HIVE-METASTORE status check', 'SERVICE MSG')

# Hadoop_Zookeeper_Down
test('Hadoop_Zookeeper_Down',
     'Critical: Hadoop: zookeepers_down# SERVICE MSG',
     'HARD', '1', 'CRITICAL', 'ZOOKEEPER::Percent zookeeper servers down', 'SERVICE MSG')

test('Hadoop_Zookeeper_Down:OK',
    'OK: Hadoop: zookeepers_down_ok# SERVICE MSG',
    'HARD', '1', 'OK', 'ZOOKEEPER::Percent zookeeper servers down', 'SERVICE MSG')

# Hadoop_Zookeeper_Process_Down
test('Hadoop_Zookeeper_Process_Down',
     'Critical: Hadoop: zookeeper_process_down# SERVICE MSG',
     'HARD', '1', 'CRITICAL', 'ZKSERVERS::ZKSERVERS Process down', 'SERVICE MSG')

test('Hadoop_Zookeeper_Process_Down:OK',
    'OK: Hadoop: zookeeper_process_down_ok# SERVICE MSG',
    'HARD', '1', 'OK', 'ZKSERVERS::ZKSERVERS Process down', 'SERVICE MSG')

# Hadoop_Oozie_Down
test('Hadoop_Oozie_Down',
     'Critical: Hadoop: oozie_down# SERVICE MSG',
     'HARD', '1', 'CRITICAL', 'OOZIE::Oozie status check', 'SERVICE MSG')

test('Hadoop_Oozie_Down:OK',
    'OK: Hadoop: oozie_down_ok# SERVICE MSG',
    'HARD', '1', 'OK', 'OOZIE::Oozie status check', 'SERVICE MSG')

# Hadoop_Templeton_Down
test('Hadoop_Templeton_Down',
     'Critical: Hadoop: templeton_down# SERVICE MSG',
     'HARD', '1', 'CRITICAL', 'TEMPLETON::Templeton status check', 'SERVICE MSG')

test('Hadoop_Templeton_Down:OK',
    'OK: Hadoop: templeton_down_ok# SERVICE MSG',
    'HARD', '1', 'OK', 'TEMPLETON::Templeton status check', 'SERVICE MSG')

# Hadoop_Puppet_Down
test('Hadoop_Puppet_Down',
     'Critical: Hadoop: puppet_down# SERVICE MSG',
     'HARD', '1', 'CRITICAL', 'PUPPET::Puppet agent down', 'SERVICE MSG')

test('Hadoop_Puppet_Down:OK',
    'OK: Hadoop: puppet_down_ok# SERVICE MSG',
    'HARD', '1', 'OK', 'PUPPET::Puppet agent down', 'SERVICE MSG')

# Hadoop_Nagios_Status_Log_Stale
test('Hadoop_Nagios_Status_Log_Stale',
     'Critical: Hadoop: nagios_status_log_stale# SERVICE MSG',
     'HARD', '1', 'CRITICAL', 'NAGIOS::Nagios status log staleness', 'SERVICE MSG')

test('Hadoop_Nagios_Status_Log_Stale:OK',
    'OK: Hadoop: nagios_status_log_stale_ok# SERVICE MSG',
    'HARD', '1', 'OK', 'NAGIOS::Nagios status log staleness', 'SERVICE MSG')

# Hadoop_Ganglia_Process_Down
test('Hadoop_Ganglia_Process_Down',
     'Critical: Hadoop: ganglia_process_down# SERVICE MSG',
     'HARD', '1', 'CRITICAL', 'GANGLIA::Ganglia [gmetad] Process down', 'SERVICE MSG')

test('Hadoop_Ganglia_Process_Down:OK',
    'OK: Hadoop: ganglia_process_down_ok# SERVICE MSG',
    'HARD', '1', 'OK', 'GANGLIA::Ganglia [gmetad] Process down', 'SERVICE MSG')

# Hadoop_Ganglia_Collector_Process_Down
test('Hadoop_Ganglia_Collector_Process_Down',
     'Critical: Hadoop: ganglia_collector_process_down# SERVICE MSG',
     'HARD', '1', 'CRITICAL', 'GANGLIA::Ganglia collector [gmond] Process down alert for hbasemaster', 'SERVICE MSG')

test('Hadoop_Ganglia_Collector_Process_Down:OK',
    'OK: Hadoop: ganglia_collector_process_down_ok# SERVICE MSG',
    'HARD', '1', 'OK', 'GANGLIA::Ganglia collector [gmond] Process down alert for hbasemaster', 'SERVICE MSG')

# Hadoop_Ganglia_Collector_Process_Down
test('Hadoop_Ganglia_Collector_Process_Down',
     'Critical: Hadoop: ganglia_collector_process_down# SERVICE MSG',
     'HARD', '1', 'CRITICAL', 'GANGLIA::Ganglia collector [gmond] Process down alert for jobtracker', 'SERVICE MSG')

test('Hadoop_Ganglia_Collector_Process_Down:OK',
    'OK: Hadoop: ganglia_collector_process_down_ok# SERVICE MSG',
    'HARD', '1', 'OK', 'GANGLIA::Ganglia collector [gmond] Process down alert for jobtracker', 'SERVICE MSG')

# Hadoop_Ganglia_Collector_Process_Down
test('Hadoop_Ganglia_Collector_Process_Down',
     'Critical: Hadoop: ganglia_collector_process_down# SERVICE MSG',
     'HARD', '1', 'CRITICAL', 'GANGLIA::Ganglia collector [gmond] Process down alert for namenode', 'SERVICE MSG')

test('Hadoop_Ganglia_Collector_Process_Down:OK',
    'OK: Hadoop: ganglia_collector_process_down_ok# SERVICE MSG',
    'HARD', '1', 'OK', 'GANGLIA::Ganglia collector [gmond] Process down alert for namenode', 'SERVICE MSG')

# Hadoop_Ganglia_Collector_Process_Down
test('Hadoop_Ganglia_Collector_Process_Down',
     'Critical: Hadoop: ganglia_collector_process_down# SERVICE MSG',
     'HARD', '1', 'CRITICAL', 'GANGLIA::Ganglia collector [gmond] Process down alert for slaves', 'SERVICE MSG')

test('Hadoop_Ganglia_Collector_Process_Down:OK',
    'OK: Hadoop: ganglia_collector_process_down_ok# SERVICE MSG',
    'HARD', '1', 'OK', 'GANGLIA::Ganglia collector [gmond] Process down alert for slaves', 'SERVICE MSG')

# Hadoop_UNKNOWN_MSG
test('Hadoop_UNKNOWN_MSG',
     'Critical: Hadoop: HADOOP_UNKNOWN_MSG# SERVICE MSG',
     'HARD', '1', 'CRITICAL', 'ANY UNKNOWN SERVICE', 'SERVICE MSG')

# HBase UI Down
test('Hadoop_HBase_UI_Down',
    'Critical: Hadoop: hbase_ui_down# SERVICE MSG',
    'HARD', '1', 'CRITICAL', 'HBASEMASTER::HBase Web UI down', 'SERVICE MSG')

test('Hadoop_HBase_UI_Down:OK',
    'OK: Hadoop: hbase_ui_down_ok# SERVICE MSG',
    'HARD', '1', 'OK', 'HBASEMASTER::HBase Web UI down', 'SERVICE MSG')

# Namenode UI Down
test('Hadoop_NameNode_UI_Down',
    'Critical: Hadoop: namenode_ui_down# SERVICE MSG',
    'HARD', '1', 'CRITICAL', 'NAMENODE::Namenode Web UI down', 'SERVICE MSG')

test('Hadoop_NameNode_UI_Down:OK',
    'OK: Hadoop: namenode_ui_down_ok# SERVICE MSG',
    'HARD', '1', 'OK', 'NAMENODE::Namenode Web UI down', 'SERVICE MSG')

# JobHistory UI Down
test('Hadoop_JobHistory_UI_Down',
    'Critical: Hadoop: jobhistory_ui_down# SERVICE MSG',
    'HARD', '1', 'CRITICAL', 'JOBTRACKER::JobHistory Web UI down', 'SERVICE MSG')

test('Hadoop_JobHistory_UI_Down:OK',
    'OK: Hadoop: jobhistory_ui_down_ok# SERVICE MSG',
    'HARD', '1', 'OK', 'JOBTRACKER::JobHistory Web UI down', 'SERVICE MSG')

# JobTracker UI Down
test('Hadoop_JobTracker_UI_Down',
    'Critical: Hadoop: jobtracker_ui_down# SERVICE MSG',
    'HARD', '1', 'CRITICAL', 'JOBTRACKER::JobTracker Web UI down', 'SERVICE MSG')

test('Hadoop_JobTracker_UI_Down:OK',
    'OK: Hadoop: jobtracker_ui_down_ok# SERVICE MSG',
    'HARD', '1', 'OK', 'JOBTRACKER::JobTracker Web UI down', 'SERVICE MSG')

summary()


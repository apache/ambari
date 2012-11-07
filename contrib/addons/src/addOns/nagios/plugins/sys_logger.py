#!/usr/bin/python
import sys
import syslog

# dictionary of state->severity mappings
severities = {'UP':'OK', 'DOWN':'Critical', 'UNREACHABLE':'Critical', 'OK':'OK',
              'WARNING':'Warning', 'UNKNOWN':'Warning', 'CRITICAL':'Critical'}

# List of services which can result in events at the Degraded severity
degraded_alert_services = ['HBASEMASTER::HBaseMaster CPU utilization',
                           'HDFS::Namenode RPC Latency',
                           'MAPREDUCE::JobTracker RPC Latency',
                           'JOBTRACKER::Jobtracker CPU utilization']

# List of services which can result in events at the Fatal severity
fatal_alert_services = ['NAMENODE::Namenode Process down']

# dictionary of service->msg_id mappings
msg_ids = {'Host::Ping':'host_down', 'HBASEMASTER::HBaseMaster CPU utilization':'master_cpu_utilization',
           'HDFS::HDFS Capacity utilization':'hdfs_percent_capacity', 'HDFS::Corrupt/Missing blocks':'hdfs_block',
           'NAMENODE::Namenode Edit logs directory status':'namenode_edit_log_write', 'HDFS::Percent DataNodes down':'datanode_down',
           'DATANODE::Process down':'datanode_process_down', 'HDFS::Percent DataNodes storage full':'datanodes_percent_storage_full',
           'NAMENODE::Namenode Process down':'namenode_process_down', 'HDFS::Namenode RPC Latency':'namenode_rpc_latency',
           'DATANODE::Storage full':'datanodes_storage_full', 'JOBTRACKER::Jobtracker Process down':'jobtracker_process_down',
           'MAPREDUCE::JobTracker RPC Latency':'jobtracker_rpc_latency', 'MAPREDUCE::Percent TaskTrackers down':'tasktrackers_down',
           'TASKTRACKER::Process down':'tasktracker_process_down', 'HBASEMASTER::HBaseMaster Process down':'hbasemaster_process_down',
           'REGIONSERVER::Process down':'regionserver_process_down', 'HBASE::Percent region servers down':'regionservers_down',
           'HIVE-METASTORE::HIVE-METASTORE status check':'hive_metastore_process_down', 'ZOOKEEPER::Percent zookeeper servers down':'zookeepers_down',
           'ZKSERVERS::ZKSERVERS Process down':'zookeeper_process_down', 'OOZIE::Oozie status check':'oozie_down',
           'TEMPLETON::Templeton status check':'templeton_down', 'PUPPET::Puppet agent down':'puppet_down',
           'NAGIOS::Nagios status log staleness':'nagios_status_log_stale', 'GANGLIA::Ganglia [gmetad] Process down':'ganglia_process_down',
           'GANGLIA::Ganglia collector [gmond] Process down alert for hbasemaster':'ganglia_collector_process_down',
           'GANGLIA::Ganglia collector [gmond] Process down alert for jobtracker':'ganglia_collector_process_down',
           'GANGLIA::Ganglia collector [gmond] Process down alert for namenode':'ganglia_collector_process_down',
           'GANGLIA::Ganglia collector [gmond] Process down alert for slaves':'ganglia_collector_process_down',
           'NAMENODE::Secondary Namenode Process down':'secondary_namenode_process_down',
           'JOBTRACKER::Jobtracker CPU utilization':'jobtracker_cpu_utilization',
           'HBASEMASTER::HBase Web UI down':'hbase_ui_down', 'NAMENODE::Namenode Web UI down':'namenode_ui_down',
           'JOBTRACKER::JobHistory Web UI down':'jobhistory_ui_down', 'JOBTRACKER::JobTracker Web UI down':'jobtracker_ui_down'}


# Determine the severity of the TVI alert based on the Nagios alert state.
def determine_severity(state, service):
    if severities.has_key(state):
        severity = severities[state]
    else: severity = 'Warning'

    # For some alerts, warning should be converted to Degraded
    if severity == 'Warning' and service in degraded_alert_services:
        severity = 'Degraded'
    elif severity != 'OK' and service in fatal_alert_services:
        severity = 'Fatal'

    return severity


# Determine the msg id for the TVI alert from based on the service which generates the Nagios alert.
# The msg id is used to correlate a log msg to a TVI rule.
def determine_msg_id(service, severity):
    if msg_ids.has_key(service):
        msg_id = msg_ids[service]
        if severity == 'OK':
            msg_id = '{0}_ok'.format(msg_id)

        return msg_id
    else: return 'HADOOP_UNKNOWN_MSG'


# Determine the domain.  Currently the domain is always 'Hadoop'.
def determine_domain():
    return 'Hadoop'


# log the TVI msg to the syslog
def log_tvi_msg(msg):
    syslog.openlog('Hadoop', syslog.LOG_PID)
    syslog.syslog(msg)


# generate a tvi log msg from a Hadoop alert
def generate_tvi_log_msg(alert_type, attempt, state, service, msg):
    # Determine the TVI msg contents
    severity = determine_severity(state, service)  # The TVI alert severity.
    domain   = determine_domain()                  # The domain specified in the TVI alert.
    msg_id   = determine_msg_id(service, severity) # The msg_id used to correlate to a TVI rule.

    # Only log HARD alerts
    if alert_type == 'HARD':
        # Format and log msg
        log_tvi_msg('{0}: {1}: {2}# {3}'.format(severity, domain, msg_id, msg))


# main method which is called when invoked on the command line
def main():
    generate_tvi_log_msg(sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4], sys.argv[5])


# run the main method
if __name__ == '__main__':
    main()
    sys.exit(0)
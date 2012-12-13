ambari-log4j
============

log4j add-ons for Apache Ambari.

1. MapReduce JobHistory

JobHistoryAppender allows for all job statistics to be saved into a
DataBase via JDBC.

Create a postgres DB of name ambari and run the table creation commands
from src/main/resources/ambari.schema.

Build by running mvn clean package.  Copy the resulting ambari-log4j jar
from the target directory into the hadoop lib directory for the
JobTracker.  Also copy postgresql-9.1-902.jdbc4.jar into the hadoop lib 
directory for the JobTracker.

Add the following to your log4j.properties for the JobTracker, setting
<username> and <password> as appropriate for your postgres DB.  Then,
when you start the JobTracker, use the following command:
HADOOP_OPTS="$HADOOP_OPTS -Dambari.jobhistory.logger=DEBUG,JHA" bin/hadoop-daemon.sh start jobtracker

----
#
# JobHistory logger 
#

ambari.jobhistory.database=jdbc:postgresql://localhost:5432/ambari
ambari.jobhistory.driver=org.postgresql.Driver
ambari.jobhistory.user=<username>
ambari.jobhistory.password=<password>
ambari.jobhistory.logger=${hadoop.root.logger}

log4j.appender.JHA=org.apache.ambari.log4j.hadoop.mapreduce.jobhistory.JobHistoryAppender
log4j.appender.JHA.database=${ambari.jobhistory.database}
log4j.appender.JHA.driver=${ambari.jobhistory.driver}
log4j.appender.JHA.user=${ambari.jobhistory.user}
log4j.appender.JHA.password=${ambari.jobhistory.password}

log4j.logger.org.apache.hadoop.mapred.JobHistory$JobHistoryLogger=${ambari.jobhistory.logger}
log4j.additivity.org.apache.hadoop.mapred.JobHistory$JobHistoryLogger=true

----


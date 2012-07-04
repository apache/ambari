Ambari
======

Apache Ambari is a web application for installing, managing, and monitoring
Apache Hadoop clusters.
The set of Hadoop components that are currently supported by Ambari includes:

    Apache HBase
    Apache HCatalog
    Apache Hadoop HDFS
    Apache Hive
    Apache Hadoop MapReduce
    Apache Oozie
    Apache Pig
    Apache Sqoop
    Apache Templeton
    Apache Zookeeper

Ambari's primary audience is system administrators responsible for managing
Hadoop clusters.

Ambari allows them to:

    Easily Install a Hadoop Cluster
        Ambari provides an easy-to-use, step-by-step wizard for installing
        Hadoop services across any number of nodes.
        Ambari leverages Puppet to perform installation and configuration of
        Hadoop services for the cluster.

    Manage a Hadoop Cluster
        Ambari provides central management for starting, stopping, and
        reconfiguring Hadoop services across the entire cluster.

    Monitor a Hadoop Cluster
        Ambari provides a dashboard for monitoring health and status of the
        Hadoop cluster. Ambari leverages Ganglia to render graphs.
        Ambari sends email alerts when your attention is needed (e.g., a node
        goes down, remaining disk space is low, etc).
        Ambari leverages Nagios to monitor and trigger alerts.

In the future, Ambari will allow third-party tool developers to integrate
Hadoop cluster management and monitoring capabilities via its RESTful interface.


Resources
=========

 - Ambari project website:    http://incubator.apache.org/ambari
 - Ambari installation guide: http://incubator.apache.org/ambari/install.html

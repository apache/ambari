# Ambari Rolling Upgrade Magician


**Script**: ru_magician.py

## Licenses Used:
* MIT License
* GNU Lesser General Public License v1.3

This script is provided as is with no guarantees.

## Pre-reqs
* Apache Ambari 2.0 or higher
* MySQL or Postgres database, and appropriate python package to connect to the database

## Tested with
* Apache Ambari 2.0 and HDP 2.2
* Apache Ambari 2.1 and HDP 2.2 and 2.3

## Description:
This script is meant to be used on clusters deployed with Ambari and using Ambari version 2.0.0 or higher, in order
to identify any inconsistencies in the database related to the version of components for the HDP stack.
The script will pinpoint problems, and ask the user to take correct action to update their database.
As of this version, this script only supports MySQL and Postgres.

It handles the following cases:

1. On a newly installed cluster, ensures that there is at least one cluster version whose state is CURRENT. If not, will advise the user to restart services.

2. If the user has Registered and Installed repositories, check that each one has a unique version and display name. Further, if any are stuck in an INSTALLING state, will let the user take three potential actions: leave as is, force to INSTALLED, force to INSTALL_FAILED (in order to retry).

3. If the user has Registered and Installed repositories, and one cluster_version is already in an UPGRADING state, perhaps because hdp-select changed the symlinks and a component was restarted, or the user inadvertently started a manual upgrade, will allow the user to force it back to INSTALLED.

4. If the user in the in the middle of an upgrade, and they want to force one of the versions are CURRENT, it will update all database records accordingly, and set the previously CURRENT version to INSTALLED.

## Instructions to run:
* Install Python 2.6 or higher
* Ensure that /etc/ambari-server/conf/ambari.properties is accessible
* Stop ambari-server process
* If using MySQL database, will have to install MySQL database package for python. E.g., yum install MySQL-python
* If using Postgres databse, will have to install Postgres database package for python. E.g., yum install python-psycopg2
* From the Ambari Server host, run the file as, python ru_magician.py

Ambari
======

 - Ambari provides the GUI frontend that would help users install, manage and
monitor the Hadoop cluster.
 - It uses CSS, Javascript, and HTML on client side. We use the existing proven
open-source technologies in the backend e.g. PHP, Puppet for deploying and
configuring the Hadoop services, Nagios & Ganglia for monitoring alerts and
 metrics graphs respectively.

Getting Ambari
==============

The source can be checked out anonymously from SVN with this command:

  $ svn checkout http://svn.apache.org/repos/asf/incubator/ambari/trunk ambari

How to build Ambari
===================

You need to build a AMBARI rpm from the source code that you checkout above:

 $ yum install rpm-build

 $ cd hmc; // The new directory structure

 $ cd package/rpm

 $ ./create_hmc_rpm.sh

When it succeeds, you will find two RPMs inside the build directory package/rpm/build/rpmbuild/RPMS/noarch/:

 1) ambari-1.0.0-1.noarch.rpm

 2) ambari-agent-1.0.0-1.noarch.rpm

Steps to run before you use Ambari for installing Hadoop on your cluster
========================================================================

The node on which you will run the Ambari web-server will be henceforth referred
to as Ambari master and the nodes which will be used to run Hadoop software will
be called as cluster nodes.

 1) Set up password-less SSH for root to all your cluster nodes.

    - Make sure you copy root's ssh public keys to all the cluster hosts.

    - You will have to copy the ssh private key to the Ambari node for installing
Hadoop on your cluster nodes from the UI.

 Make sure you copy root's ssh public keys to all the cluster hosts.
 You will have to copy the ssh private key to your desktop for later use in the UI.

 2) Install the above built ambari-agent-1.0.0-1.noarch.rpm on each of the nodes by running

    $ sudo rpm -Uvh ambari-agent-1.0.0-1.noarch.rpm

 3) Stop ip-tables on your Ambari master:

    $ sudo service iptables stop

How to install Ambari
=====================

Ambari has few external runtime dependencies, most important of which are

 1) puppet

 2) php

 3) ruby

To simplify the installation of dependencies, you need to enable the following
yum repos. For that, you can simply install the corresponding RPMs.

 1) EPEL repo

 2) Puppet passenger

 3) Hadoop repo

So, you should do the following at the command line:

 $ sudo rpm -Uvh http://download.fedoraproject.org/pub/epel/5/[i386|x86_64]/epel-release-5-4.noarch.rpm 

 $ sudo rpm -Uvh http://passenger.stealthymonkeys.com/rhel/5/passenger-release.noarch.rpm 

 $ sudo rpm -Uvh http://public-repo-1.hortonworks.com/HDP-1.0.13/repos/centos5/hdp-release-1.0.13-1.el5.noarch.rpm

Now, to install ambari-rpms

 $ sudo yum install php-pecl-json (version 1.2.1 compatible with php-5.1 or php-5.2)
 
 $ php -m | grep posix

   If the posix module is not found, run 'sudo yum install php-process'

 $ sudo rpm -iv ambari-1.0.0-1.noarch.rpm 

Hadoop required JDK, so you can download it from the Oracle website
http://download.oracle.com/otn-pub/java/jdk/6u26-b03 . Ambari needs the following
two files to be available on the Ambari master node under /var/run/hmc/downloads/
:

  1) jdk-6u26-linux-x64.bin

  2) jdk-6u26-linux-i586.bin

How to run Ambari
=================

 $ sudo service ambari start

You have installed ambari by now. You can start deploying Apache Hadoop and
ecosystem components on your cluster nodes by visiting:

 http://AMBARIMASTER/hmc/html/index.php

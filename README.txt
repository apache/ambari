How to use HMC:

The rpm has a dependency on puppet. For now (until we have yum repo for HMC and puppet), to install puppet on the install host, you will have to add the following repo.

edit file  /etc/yum.repos.d/puppet.repo as root
to contain:
[Puppet]
name=Puppet
baseurl = http://yum.puppetlabs.com/el/5/products/x86_64/
enabled=1
gpgcheck=0

yum install rpm-build

cd hmc; // The new directory structure

cd package/rpm
./create_hmc_rpm.sh

To install the rpm:
sudo yum install php 5.1.6
sudo yum install pdsh pdsh-rcmd-exec pdsh-rcmd-ssh
sudo yum install httpd-devel mod_ssl
sudo yum install puppet-2.7.9
sudo yum install php-pdo-5.1.6
sudo yum install php-pecl-json-1.2.1
sudo yum install ruby-devel rubygems
sudo rpm -Uvh http://passenger.stealthymonkeys.com/rhel/5/passenger-release.noarch.rpm
sudo yum install mod_passenger
sudo rpm -iv  build/rpmbuild/RPMS/noarch/hmc-0.0.1-2.noarch.rpm

To Run:

Before starting, HMC requires java 32 bit and 64 bit to be available on the HMC host at /var/run/hmc/downloads/.

sudo service hmc  start

Visit:

 http://HOSTNAME/hmc/html/index.php

 to get started.

 Make sure you copy root's ssh public keys to all the cluster hosts.
 You will have to copy the ssh private key to your desktop for later use in the UI.


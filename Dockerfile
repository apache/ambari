#
# This Dockerfile is used to create an environment to build and release Ambari RPMs
#

FROM centos:6

#install rpmbuild
RUN yum -y install rpm-build

# add epel extra repo for install apache-maven in centos6
RUN curl https://repos.fedorapeople.org/repos/dchen/apache-maven/epel-apache-maven.repo | \
    sed 's/$releasever/6/g' > /etc/yum.repos.d/epel-apache-maven.repo
RUN yum -y install apache-maven

# install python setuptools
RUN wget https://pypi.python.org/packages/2.7/s/setuptools/setuptools-0.6c11-py2.7.egg#md5=fe1f997bc722265116870bc7919059ea .
RUN sh setuptools-0.6c11-py2.7.egg

# install gcc packages
RUN yum -y groupinstall "Development Tools"
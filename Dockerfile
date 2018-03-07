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
RUN yum -y install wget
RUN wget https://pypi.python.org/packages/2.6/s/setuptools/setuptools-0.6c11-py2.6.egg
RUN sh setuptools-0.6c11-py2.6.egg

# install gcc packages
RUN yum -y groupinstall "Development Tools"
RUN yum -y install epel-release
RUN yum -y install python-devel python-pip python-setuptools
RUN pip install awscli

#Install openjdk-devel
RUN yum install -y java-1.7.0-openjdk-devel

#Install expect to help us sign RPMs without interactive prompts
RUN yum install -y expect
#For publishing RPM repos
RUN yum install -y createrepo
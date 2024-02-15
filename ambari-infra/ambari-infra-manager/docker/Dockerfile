#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.

FROM centos:centos6

RUN echo root:changeme | chpasswd

RUN yum clean all -y && yum update -y
RUN yum -y install vim wget rpm-build sudo which telnet tar openssh-server openssh-clients ntp git httpd lsof
RUN rpm -e --nodeps --justdb glibc-common
RUN yum -y install glibc-common

ENV HOME /root

#Install JAVA
ENV JAVA_VERSION 11.0.1
ENV BUILD_VERSION 13

RUN wget --no-check-certificate --no-cookies --header "Cookie:oraclelicense=accept-securebackup-cookie" http://download.oracle.com/otn-pub/java/jdk/$JAVA_VERSION+$BUILD_VERSION/90cf5d8f270a4347a95050320eef3fb7/jdk-${JAVA_VERSION}_linux-x64_bin.rpm -O jdk-11-linux-x64.rpm
RUN rpm -ivh jdk-11-linux-x64.rpm
ENV JAVA_HOME /usr/java/default/

#Install Maven
RUN mkdir -p /opt/maven
WORKDIR /opt/maven
RUN wget http://archive.apache.org/dist/maven/maven-3/3.3.1/binaries/apache-maven-3.3.1-bin.tar.gz
RUN tar -xvzf /opt/maven/apache-maven-3.3.1-bin.tar.gz
RUN rm -rf /opt/maven/apache-maven-3.3.1-bin.tar.gz

ENV M2_HOME /opt/maven/apache-maven-3.3.1
ENV MAVEN_OPTS -Xmx2048m
ENV PATH $PATH:$JAVA_HOME/bin:$M2_HOME/bin

# SSH key
RUN ssh-keygen -f /root/.ssh/id_rsa -t rsa -N ''
RUN cat /root/.ssh/id_rsa.pub > /root/.ssh/authorized_keys
RUN chmod 600 /root/.ssh/authorized_keys
RUN sed -ri 's/UsePAM yes/UsePAM no/g' /etc/ssh/sshd_config

ADD bin/start.sh /root/start.sh
RUN chmod +x /root/start.sh

WORKDIR /root
CMD /root/start.sh
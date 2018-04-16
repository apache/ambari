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


RUN yum clean all -y && yum update -y
RUN yum -y install vim wget rpm-build sudo which telnet tar openssh-server openssh-clients ntp git httpd lsof

ENV HOME /root

#Install JAVA
ENV JAVA_VERSION 8u131
ENV BUILD_VERSION b11
RUN wget --no-check-certificate --no-cookies --header "Cookie:oraclelicense=accept-securebackup-cookie" http://download.oracle.com/otn-pub/java/jdk/$JAVA_VERSION-$BUILD_VERSION/d54c1d3a095b4ff2b6607d096fa80163/jdk-$JAVA_VERSION-linux-x64.rpm -O jdk-8-linux-x64.rpm
RUN rpm -ivh jdk-8-linux-x64.rpm
ENV JAVA_HOME /usr/java/default/


#Install Solr
ADD ambari-infra-solr-2.0.0.0-SNAPSHOT.noarch.rpm /root/ambari-infra-solr.rpm
RUN rpm -ivh /root/ambari-infra-solr.rpm

RUN mkdir -p /root/solr_index/data
ENV SOLR_HOME /root/solr_index/data
ADD solr.xml /root/solr_index/data/solr.xml

ENV PATH $PATH:$JAVA_HOME/bin:/usr/lib/ambari-infra-solr/bin

#Enable G1 GC
#ENV GC_TUNE="-XX:+UseG1GC -XX:+PerfDisableSharedMem -XX:+ParallelRefProcEnabled -XX:G1HeapRegionSize=3m -XX:MaxGCPauseMillis=250 -XX:InitiatingHeapOccupancyPercent=75 -XX:+UseLargePages -XX:+AggressiveOpts"

# Start in debug mode
#ENV SOLR_OPTS -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005

WORKDIR /root
CMD /usr/lib/ambari-infra-solr/bin/solr start -force -f

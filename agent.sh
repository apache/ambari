#!/bin/bash
yum install wget -y
wget -O /etc/yum.repos.d/ambari.repo http://10.240.0.30/ambari.repo
yum clean all; yum install krb5-workstation git ambari-agent -y
mkdir /home ; cd /home; git clone https://github.com/apache/ambari.git ; cd ambari ; git checkout branch-2.5
cp -r /home/ambari/ambari-server/src/main/resources/stacks/PERF /var/lib/ambari-agent/cache/stacks/PERF
sed -i -f /var/lib/ambari-agent/cache/stacks/PERF/PythonExecutor.sed /usr/lib/ambari-agent/lib/ambari_agent/PythonExecutor.py
sed -i -f /var/lib/ambari-agent/cache/stacks/PERF/check_host.sed /var/lib/ambari-agent/cache/custom_actions/scripts/check_host.py
sed -i -e 's/hostname=localhost/hostname=perf-server-test-perf-1.c.pramod-thangali.internal/g' /etc/ambari-agent/conf/ambari-agent.ini
sed -i -e 's/agent]/agent]\nhostname_script=foo\npublic_hostname_script=foo\n/1' /etc/ambari-agent/conf/ambari-agent.ini
wget http://10.240.0.30/agent-multiplier.py ; python /home/ambari/ambari-agent/conf/unix/agent-multiplier.py start
exit 0
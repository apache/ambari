#!/bin/bash
yum install wget -y
wget -O /etc/yum.repos.d/ambari.repo http://10.240.0.30/ambari.repo
yum clean all; yum install ambari-server -y
sed -i -f /home/ambari/ambari-server/src/main/resources/stacks/PERF/install_packages.sed /var/lib/ambari-server/resources/custom_actions/scripts/install_packages.py
sed -i -f /home/ambari/ambari-server/src/main/resources/stacks/PERF/install_packages.sed /var/lib/ambari-agent/cache/custom_actions/scripts/install_packages.py


cd /; wget http://central.maven.org/maven2/mysql/mysql-connector-java/5.1.40/mysql-connector-java-5.1.40.jar;
mkdir /usr/share/java; chmod 777 /usr/share/java;cp mysql-connector-java-5.1.40.jar /usr/share/java/; chmod 777 /usr/share/java/mysql-connector-java-5.1.40.jar;
ln -s /usr/share/java/mysql-connector-java-5.1.40.jar /usr/share/java/mysql-connector-java.jar;
cd /etc/yum.repos.d/; wget http://repo.mysql.com/mysql-community-release-el6-5.noarch.rpm; rpm -ivh mysql-community-release-el6-5.noarch.rpm;yum clean all; yum install mysql-server -y
sed -i -e 's/mysqld]/mysqld]\nmax_allowed_packet=1024M\njoin_buffer_size=512M\nsort_buffer_size=128M\nread_rnd_buffer_size=128M\ninnodb_buffer_pool_size=16G\ninnodb_file_io_threads=16\ninnodb_thread_concurrency=32\nkey_buffer_size=16G\nquery_cache_limit=16M\nquery_cache_size=512M\nthread_cache_size=128\ninnodb_log_buffer_size=512M/1' /etc/my.cnf
service mysqld start
mysql -uroot -e "CREATE DATABASE ambari;"
mysql -uroot -e "SOURCE /var/lib/ambari-server/resources/Ambari-DDL-MySQL-CREATE.sql;" ambari
mysql -uroot -e "CREATE USER 'ambari'@'%' IDENTIFIED BY 'bigdata';"
mysql -uroot -e "GRANT ALL PRIVILEGES ON *.* TO 'ambari'@'%%';"
mysql -uroot -e "CREATE USER 'ambari'@'localhost' IDENTIFIED BY 'bigdata';"
mysql -uroot -e "GRANT ALL PRIVILEGES ON *.* TO 'ambari'@'localhost';"
mysql -uroot -e "CREATE USER 'ambari'@'perf-server-test-perf-1.c.pramod-thangali.internal' IDENTIFIED BY 'bigdata';"
mysql -uroot -e "GRANT ALL PRIVILEGES ON *.* TO 'ambari'@'perf-server-test-perf-1.c.pramod-thangali.internal';"
mysql -uroot -e "FLUSH PRIVILEGES;"


ambari-server setup -s
ambari-server setup --database mysql --jdbc-db=mysql --jdbc-driver=/usr/share/java/mysql-connector-java.jar --databasehost=localhost --databaseport=3306 --databasename=ambari --databaseusername=ambari --databasepassword=bigdata
sed -i -e 's/=postgres/=mysql/g' /etc/ambari-server/conf/ambari.properties
sed -i -e 's/server.persistence.type=local/server.persistence.type=remote/g' /etc/ambari-server/conf/ambari.properties
sed -i -e 's/local.database.user=postgres//g' /etc/ambari-server/conf/ambari.properties
sed -i -e 's/server.jdbc.postgres.schema=ambari//g' /etc/ambari-server/conf/ambari.properties
sed -i -e 's/agent.threadpool.size.max=25/agent.threadpool.size.max=100/g' /etc/ambari-server/conf/ambari.properties
sed -i -e 's/client.threadpool.size.max=25/client.threadpool.size.max=65/g' /etc/ambari-server/conf/ambari.properties
sed -i -e 's/false/true/g' /var/lib/ambari-server/resources/stacks/PERF/1.0/metainfo.xml
sed -i -e 's/false/true/g' /var/lib/ambari-server/resources/stacks/PERF/2.0/metainfo.xml
sed -i -e 's/-Xmx2048m/-Xmx16384m/g' /var/lib/ambari-server/ambari-env.sh

echo 'server.jdbc.driver=com.mysql.jdbc.Driver' >> /etc/ambari-server/conf/ambari.properties
echo 'server.jdbc.rca.url=jdbc:mysql://perf-server-test-perf-1.c.pramod-thangali.internal:3306/ambari' >> /etc/ambari-server/conf/ambari.properties
echo 'server.jdbc.rca.driver=com.mysql.jdbc.Driver' >> /etc/ambari-server/conf/ambari.properties
echo 'server.jdbc.url=jdbc:mysql://perf-server-test-perf-1.c.pramod-thangali.internal:3306/ambari' >> /etc/ambari-server/conf/ambari.properties
echo 'server.jdbc.port=3306' >> /etc/ambari-server/conf/ambari.properties
echo 'server.jdbc.hostname=localhost' >> /etc/ambari-server/conf/ambari.properties
echo 'server.jdbc.driver.path=/usr/share/java/mysql-connector-java.jar' >> /etc/ambari-server/conf/ambari.properties
echo 'alerts.cache.enabled=true' >> /etc/ambari-server/conf/ambari.properties
echo 'alerts.cache.size=100000' >> /etc/ambari-server/conf/ambari.properties
echo 'alerts.execution.scheduler.maxThreads=4' >> /etc/ambari-server/conf/ambari.properties
echo 'security.temporary.keystore.retention.minutes=180' >> /etc/ambari-server/conf/ambari.properties
echo 'stack.hooks.folder=stacks/PERF/1.0/hooks' >> /etc/ambari-server/conf/ambari.properties

ambari-server start --skip-database-check
exit 0
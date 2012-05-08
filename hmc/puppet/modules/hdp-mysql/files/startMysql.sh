#!/bin/sh

mysqldbuser=$1
mysqldbpasswd=$2
mysqldbhost=$3

echo "Adding user $mysqldbuser@$mysqldbhost"
echo "CREATE USER '$mysqldbuser'@'$mysqldbhost' IDENTIFIED BY '$mysqldbpasswd';" | mysql -u root
echo "GRANT ALL PRIVILEGES ON *.* TO '$mysqldbuser'@'$mysqldbhost';" | mysql -u root
echo "flush privileges;" | mysql -u root

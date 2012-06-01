#!/bin/sh

dbFile="/var/db/hmc/data/data.db"

if [[ "x" != "x$1" ]]; then
  dbFile=$1
fi

if [[ ! -f ${dbFile} ]]; then
  echo "DB file ${dbFile} does not exist";
  exit 1
fi

while true; do
  read -p "Are you really sure you want to wipe out the DB at ${dbFile}? (y/n)" yn  
  case $yn in
    [Yy]* ) break;;
    [Nn]* ) echo "User aborted script. Exiting without cleaning up DB"; exit 0;;
    * ) echo "Please answer y or n.";;
  esac
done

echo "Deleting data from DB ${dbFile}, restoring to clean state"

sqlite3 ${dbFile} "Delete FROM Clusters;"
sqlite3 ${dbFile} "DELETE FROM ServiceInfo;"
sqlite3 ${dbFile} "DELETE FROM ServiceComponentInfo;"
sqlite3 ${dbFile} "DELETE FROM ServiceConfig;"
sqlite3 ${dbFile} "DELETE FROM Hosts;"
sqlite3 ${dbFile} "DELETE FROM HostRoles;"
sqlite3 ${dbFile} "DELETE FROM HostRoleConfig;"
sqlite3 ${dbFile} "DELETE FROM ConfigHistory;"
sqlite3 ${dbFile} "DELETE FROM TransactionStatus;"
sqlite3 ${dbFile} "DELETE FROM SubTransactionStatus;"

exit 0;
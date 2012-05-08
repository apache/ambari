export tablename=$1
echo "CREATE EXTERNAL TABLE IF NOT EXISTS ${tablename} ( foo INT, bar STRING );" | hive
echo "DESCRIBE ${tablename};" | hive

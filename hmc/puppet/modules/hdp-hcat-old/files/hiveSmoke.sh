echo 'CREATE EXTERNAL TABLE IF NOT EXISTS hivesmoke ( foo INT, bar STRING );' | hive
echo 'DESCRIBE hivesmoke;' | hive

export tablename=$1
hcat -e "show tables"
hcat -e "drop table IF EXISTS ${tablename}"
hcat -e "create table ${tablename} ( id INT, name string ) stored as rcfile ;"

hcat -e 'show tables'
hcat -e 'drop table IF EXISTS hcatsmoke'
hcat -e 'create table hcatsmoke ( id INT, name string ) stored as rcfile ;'

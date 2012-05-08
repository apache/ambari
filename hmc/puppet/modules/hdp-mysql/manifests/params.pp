class hdp-mysql::params() inherits hdp-hive::params
{
   $db_name = "$hdp-hive::params::hive_database_name"
   $db_user = $hdp-hive::params::hive_metastore_user_name
   $db_pw = $hdp-hive::params::hive_metastore_user_passwd
}

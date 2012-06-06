class hdp-hadoop::namenode::format(
  $force = false
)
{
  $mark_file = $hdp-hadoop::params::namenode_formatted_mark_file
  $dfs_name_dir = $hdp-hadoop::params::dfs_name_dir
  $hdfs_user = $hdp::params::hdfs_user
  $hadoop_conf_dir = $hdp-hadoop::params::conf_dir

  if ($force == true) {
      hdp-hadoop::exec-hadoop { 'namenode -format' :
      command => 'namenode -format',
      notify  => Hdp::Exec['set namenode mark']
    }
  } else {
      file { '/tmp/checkForFormat.sh':
      ensure => present,
      source => "puppet:///modules/hdp-hadoop/checkForFormat.sh",
      mode => '0755'
    }

    exec { '/tmp/checkForFormat.sh':
      command   => "sh /tmp/checkForFormat.sh ${hdfs_user} ${hadoop_conf_dir} ${mark_file} ${dfs_name_dir} ",
      unless   => "test -f ${mark_file}",
      require   => File['/tmp/checkForFormat.sh'],
      path      => '/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
      logoutput => "true",
      notify   => Hdp::Exec['set namenode mark']
    }
  }

  hdp::exec { 'set namenode mark' :
    command     => "touch ${mark_file}",
    refreshonly => true
  }
}

class hdp-hadoop::namenode::format(
  $force = false
)
{
  $mark_file = $hdp-hadoop::params::namenode_formatted_mark_file
  if ($force == true) {
      hdp-hadoop::exec-hadoop { 'namenode -format' :
      command => 'namenode -format',
      notify  => Hdp::Exec['set namenode mark']
    }
  } else {
    hdp-hadoop::exec-hadoop { 'namenode -format' :
      command  => 'namenode -format',
      echo_yes => true,
      unless   => "test -f ${mark_file}",
      notify   => Hdp::Exec['set namenode mark']
    }
  }

  hdp::exec { 'set namenode mark' :
    command     => "touch ${mark_file}",
    refreshonly => true
  }
}

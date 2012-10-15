#TODO: these scripts called shoudl be converetd to native puppet
define hdp2-ganglia::config::generate_monitor(
  $ganglia_service,
  $role
)
{
  $shell_cmds_dir = $hdp2-ganglia::params::ganglia_shell_cmds_dir
  $cmd = $ganglia_service ? {
    'gmond'  => $role ? {
      'server' => "${shell_cmds_dir}/setupGanglia.sh -c ${name} -m",
       default =>  "${shell_cmds_dir}/setupGanglia.sh -c ${name}"
    },
    'gmetad' => "${shell_cmds_dir}/setupGanglia.sh -t",
     default => hdp_fail("Unexpected ganglia service: ${$ganglia_service}")	
  }

  #TODO: put in test condition
  hdp::exec { $cmd:
    command => $cmd
 }
}

define hdp-ganglia::config::generate_monitor(
  $ganglia_service
)
{
  $shell_cmds_dir = $hdp-ganglia::params::ganglia_shell_cmds_dir

  $cmd = $ganglia_service ? {
    'gmond'  => "${shell_cmds_dir}/setupGanglia.sh -c ${name}",
     default => hdp_fail("Unexpected ganglia service: ${$ganglia_service}")	
  }

  #TODO: put in test condition
  hdp::exec { $cmd:
    command => $cmd
 }
}
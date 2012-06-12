#TODO: unset should br changed to undef; just to be consistent
define hdp-hadoop::hdfs::directory(
  $service_state = 'running',
  $owner = unset,
  $group = unset,
  $recursive_chown = false,
  $mode = undef,
  $recursive_chmod = false
) 
{
 
  if ($service_state == 'running') {
    $mkdir_cmd = "fs -mkdir ${name}"
    hdp-hadoop::exec-hadoop { $mkdir_cmd:
      command => $mkdir_cmd,
      unless => "hadoop fs -ls ${name} >/dev/null 2>&1"
    }
    if ($owner == unset) {
      $chown = ""
    } else {
      if ($group == unset) {
        $chown = $owner
      } else {
        $chown = "${owner}:${group}"
     } 
    }  
 
    if (chown != "") {
      #TODO: see if there is a good 'unless test'
      if ($recursive_chown == true) {
        $chown_cmd = "fs -chown -R ${chown} ${name}"
      } else {
        $chown_cmd = "fs -chown ${chown} ${name}"
      }
      hdp-hadoop::exec-hadoop {$chown_cmd :
        command => $chown_cmd
      }
      Hdp-hadoop::Exec-hadoop[$mkdir_cmd] -> Hdp-hadoop::Exec-hadoop[$chown_cmd]
    }
  
    if ($mode != undef) {
      #TODO: see if there is a good 'unless test'
      if ($recursive_mode == true) {
        $chmod_cmd = "fs -chmod -R ${mode} ${name}"
      } else {
        $chmod_cmd = "fs -chmod ${mode} ${name}"
      }
      hdp-hadoop::exec-hadoop {$chmod_cmd :
        command => $chmod_cmd
      }
      Hdp-hadoop::Exec-hadoop[$mkdir_cmd] -> Hdp-hadoop::Exec-hadoop[$chmod_cmd]
    }
  }       
}

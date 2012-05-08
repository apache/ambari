define hdp-hadoop::hdfs::copyfromlocal(
  $service_state,
  $owner = unset,
  $group = unset,
  $recursive_chown = false,
  $mode = undef,
  $recursive_chmod = false,
  $dest_dir = undef 
) 
{
 
  if ($service_state == 'running') {
    $copy_cmd = "fs -copyFromLocal ${name} ${dest_dir}"
    hdp-hadoop::exec-hadoop { $copy_cmd:
      command => $copy_cmd,
      unless => "hadoop fs -ls ${dest_dir} >/dev/null 2>&1"
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
        $chown_cmd = "fs -chown -R ${chown} ${dest_dir}"
      } else {
        $chown_cmd = "fs -chown ${chown} ${dest_dir}"
      }
      hdp-hadoop::exec-hadoop {$chown_cmd :
        command => $chown_cmd
      }
      Hdp-hadoop::Exec-hadoop[$copy_cmd] -> Hdp-hadoop::Exec-hadoop[$chown_cmd]
    }
  
    if ($mode != undef) {
      #TODO: see if there is a good 'unless test'
      if ($recursive_mode == true) {
        $chmod_cmd = "fs -chmod -R ${mode} ${dest_dir}"
      } else {
        $chmod_cmd = "fs -chmod ${mode} ${dest_dir}"
      }
      hdp-hadoop::exec-hadoop {$chmod_cmd :
        command => $chmod_cmd
      }
      Hdp-hadoop::Exec-hadoop[$copy_cmd] -> Hdp-hadoop::Exec-hadoop[$chmod_cmd]
    }
  }       
}

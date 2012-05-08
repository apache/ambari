class hdp::testing_env_patch()
{
  $cmd = "mkdir /tmp/repos; mv /etc/yum.repos.d/* /tmp/repos"
  $repo_target = "/etc/yum.repos.d/${hdp::params::hdp_yum_repo}"

  anchor { 'hdp::testing_env_patch::begin' :}
  class{ 'hdp::iptables': 
    ensure => stopped,
    require => Anchor['hdp::testing_env_patch::begin']
  }
  exec { '/bin/echo 0 > /selinux/enforce':
    require => Class['hdp::iptables']
  }
  hdp::testing_env_patch::packages { 'common' :
    require => Exec['/bin/echo 0 > /selinux/enforce']
  }
  hdp::exec { $cmd :
    command => $cmd,
    unless => "test -e ${repo_target}",
    require => Hdp::Testing_env_patch::Packages['common']
  }  
  anchor { 'hdp::testing_env_patch::end' :
    require => Exec[$cmd]
  }
}

define hdp::testing_env_patch::packages(
  $needed = false)
{
 if ($needed == true) {
   package { ['perl-Digest-HMAC','perl-Socket6','perl-Crypt-DES','xorg-x11-fonts-Type1','libdbi'] :} 
 }
}
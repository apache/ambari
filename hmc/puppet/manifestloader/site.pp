class manifestloader () {
    anchor { 'manifestloader::begin': }
    file { '/etc/puppet/agent/site.pp':
      ensure => present,
      source => "puppet:///modules/catalog/site.pp",  
      mode => '0755',
      require => Anchor['manifestloader::begin'],      
      notify => Exec['rm_puppet_apply_log']
    }

    exec {'rm_puppet_apply_log':
      command   => "rm -f /var/log/puppet_apply.log",
      path      => '/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
      notify    => Exec['puppet_apply'],
      require   => File['/etc/puppet/agent/site.pp'],
      logoutput => "true"
    }

    exec { 'puppet_apply':
      command   => "puppet apply --confdir=/etc/puppet/agent --logdest=/var/log/puppet_apply.log --debug --autoflush /etc/puppet/agent/site.pp",
      timeout   => 1200,
      path      => '/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
      require   => Exec['rm_puppet_apply_log'],
      notify    => Exec['append'],
      logoutput => "true"
    }

    exec { 'append':
      command   => "cat /var/log/puppet_apply.log",
      path      => '/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
      require   => Exec['puppet_apply'],
      notify   => Anchor['manifestloader::end'],
      logoutput => "true"
    }
    anchor { 'manifestloader::end': }
}

node default {
 stage{1 :}
 class {'manifestloader': stage => 1}
}


class manifestloader () {
    file { '/etc/puppet/agent/site.pp':
      ensure => present,
      source => "puppet:///modules/catalog/site.pp",  
      mode => '0755',
    }

    exec {'rm_puppet_apply_log':
      command   => "rm -f /var/log/puppet_apply.log",
      path      => '/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
    }

    exec { 'puppet_apply':
      command   => "sh /etc/puppet/agent/modules/puppetApply.sh",
      timeout   => 1200,
      path      => '/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
      logoutput => "true"
    }

    File['/etc/puppet/agent/site.pp'] -> Exec['rm_puppet_apply_log'] -> Exec['puppet_apply']
}

node default {
 stage{1 :}
 class {'manifestloader': stage => 1}
}


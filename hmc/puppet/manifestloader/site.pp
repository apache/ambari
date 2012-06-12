class manifestloader () {
    file { '/etc/puppet/agent/modules.tgz':
      ensure => present,
      source => "puppet:///modules/catalog/modules.tgz",  
      mode => '0755',
    }

    exec { 'untar_modules':
      command => "rm -rf /etc/puppet/agent/modules ; tar zxf /etc/puppet/agent/modules.tgz -C /etc/puppet/agent/ --strip-components 3",
      path    => '/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'
    } 

    exec { 'puppet_apply':
      command   => "sh /etc/puppet/agent/modules/puppetApply.sh",
      timeout   => 1200,
      path      => '/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
      logoutput => "true"
    }

    File['/etc/puppet/agent/modules.tgz'] -> Exec['untar_modules'] -> Exec['puppet_apply']
}

node default {
 stage{1 :}
 class {'manifestloader': stage => 1}
}


file { '/tmp/dansfile':
  ensure => present
}->
append_line { 'dans_line':
  line => 'dan is awesome',
  #path => '/tmp/dansfile',
}

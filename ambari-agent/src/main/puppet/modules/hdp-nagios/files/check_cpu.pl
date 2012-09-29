#!/usr/bin/perl -w 
#
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
#
use strict;
use Net::SNMP;
use Getopt::Long;

# Variable
my $base_proc = "1.3.6.1.2.1.25.3.3.1";   
my $proc_load = "1.3.6.1.2.1.25.3.3.1.2"; 
my $o_host = 	undef;
my $o_community = undef;
my $o_warn=	undef;
my $o_crit=	undef;
my $o_timeout = 15;
my $o_port = 161;

sub Usage {
    print "Usage: $0 -H <host> -C <snmp_community> -w <warn level> -c <crit level>\n";
}

Getopt::Long::Configure ("bundling");
GetOptions(
  'H:s'   => \$o_host,	
  'C:s'   => \$o_community,	
  'c:s'   => \$o_crit,        
  'w:s'   => \$o_warn
          );
if (!defined $o_host || !defined $o_community || !defined $o_crit || !defined $o_warn) {
  Usage();
  exit 3;
}
$o_warn =~ s/\%//g; 
$o_crit =~ s/\%//g;
alarm ($o_timeout);
$SIG{'ALRM'} = sub {
 print "Unable to contact host: $o_host\n";
 exit 3;
};

# Connect to host
my ($session,$error);
($session, $error) = Net::SNMP->session(
		-hostname  => $o_host,
		-community => $o_community,
		-port      => $o_port,
		-timeout   => $o_timeout
	  );
if (!defined($session)) {
   printf("Error opening session: %s.\n", $error);
   exit 3;
}

my $exit_val=undef;
my $resultat =  (Net::SNMP->VERSION < 4) ?
	  $session->get_table($base_proc)
	: $session->get_table(Baseoid => $base_proc);

if (!defined($resultat)) {
   printf("ERROR: Description table : %s.\n", $session->error);
   $session->close;
   exit 3;
}

$session->close;

my ($cpu_used,$ncpu)=(0,0);
foreach my $key ( keys %$resultat) {
  if ($key =~ /$proc_load/) {
    $cpu_used += $$resultat{$key};
    $ncpu++;
  }
}

if ($ncpu==0) {
  print "Can't find CPU usage information : UNKNOWN\n";
  exit 3;
}

$cpu_used /= $ncpu;

print "$ncpu CPU, ", $ncpu==1 ? "load" : "average load";
printf(" %.1f%%",$cpu_used);
$exit_val=0;

if ($cpu_used > $o_crit) {
 print " > $o_crit% : CRITICAL\n";
 $exit_val=2;
} else {
  if ($cpu_used > $o_warn) {
   print " > $o_warn% : WARNING\n";
   $exit_val=1;
  }
}
print " < $o_warn% : OK\n" if ($exit_val eq 0);
exit $exit_val;

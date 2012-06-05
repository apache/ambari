puppet apply --confdir=/etc/puppet/agent --logdest=console --debug --autoflush --detailed-exitcodes /etc/puppet/agent/site.pp
ret=$?
if [ "$ret" == "0" ] || [ "$ret" == "2" ]; then
  exit 0
else
  exit 1 
fi

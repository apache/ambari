puppet apply --confdir=/etc/puppet/agent --logdest=/var/log/puppet_apply.log --debug --autoflush --detailed-exitcodes /etc/puppet/agent/site.pp
ret=$?
cat /var/log/puppet_apply.log
if [ "$ret" == "0" ] || [ "$ret" == "2" ]; then
  exit 0
else
  exit 1 
fi

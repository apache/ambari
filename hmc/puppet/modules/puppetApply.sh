rm -f /var/log/puppet_apply.log
puppet apply --confdir=/etc/puppet/agent --logdest=/var/log/puppet_apply.log --debug --autoflush --detailed-exitcodes /etc/puppet/agent/modules/catalog/files/site.pp  >> /var/log/puppet_apply.log  2>&1
ret=$?
cat /var/log/puppet_apply.log
if [ "$ret" == "0" ] || [ "$ret" == "2" ]; then
  exit 0
else
  exit 1 
fi

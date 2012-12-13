getent group puppet >/dev/null || groupadd -r puppet
getent passwd puppet >/dev/null || /usr/sbin/useradd -g puppet puppet

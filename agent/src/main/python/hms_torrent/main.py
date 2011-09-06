#!/usr/bin/env python

import os, errno
import logging
import logging.handlers
from hms_agent.shell import shellRunner
import threading
import sys
import time
import signal
#from config import Config

logger = logging.getLogger()

def mkdir_p(path):
    try:
        os.makedirs(path)
    except OSError, exc:
        if exc.errno == errno.EEXIST:
            pass
        else: 
            raise

def main():
    logger.setLevel(logging.DEBUG)
    formatter = logging.Formatter("%(asctime)s %(filename)s:%(lineno)d - %(message)s")
    stream_handler = logging.StreamHandler()
    stream_handler.setFormatter(formatter)
    logger.addHandler(stream_handler)
    try:
#        try:
#            f = file('/etc/hms/agent.cfg')
#            cfg = Config(f)
#            if cfg.hmsPrefix != None:
#                hmsPrefix = cfg.hmsPrefix
#            else:
#                hmsPrefix = '/opt/hms'
#        except Exception, cfErr:
        hmsPrefix = '/home/hms'
        time.sleep(15)
        workdir = hmsPrefix + '/var/tmp'
        if not os.path.exists(workdir):
          mkdir_p(workdir)
          
        tracker = workdir + '/tracker'
        f = open(tracker, 'w')
        f.write(str(0))
        f.close()
    except Exception, err:
        logger.exception(str(err))
    
if __name__ == "__main__":
    main()

#!/usr/bin/python

'''
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
'''

import string
from shell import shellRunner
import os
import sys
import time
import bencode
import traceback
import urllib
import logging
import logging.handlers
import shutil
import struct, fcntl
import Queue

logger = logging.getLogger()
q = Queue.Queue()

class packageRunner:
    hmsPrefix = '/home/hms'
    softwarePrefix = '/home/hms/apps'
    downloadDir = '/home/hms/var/cache/downloads/'

    def install(self, packages, dryRun):
        try:
            for package in packages:
                packageName=package['name']
                if string.find(packageName, ".torrent")>0:
                    self.torrentInstall(packageName, dryRun)
                elif string.find(packageName, ".tar.gz")>0 or string.find(packageName, ".tgz")>0:
                    packageName = self.tarballDownload(packageName)
                    self.tarballInstall(packageName)
                elif string.find(packageName, ".rpm")>0 and (string.find(packageName, "http://")==0 or string.find(packageName, "https://")==0):
                    rpmName = self.rpmDownload(packageName)
                    list = [ rpmName ]
                    test = self.rpmInstall(list)
                    if test['exit_code']!=0:
                        raise Exception(test['error'])
                else:
                    self.yumInstall(packageName, dryRun)
            result = {'exit_code': 0, 'output': 'Install Successfully', 'error': ''}
        except Exception, err:
            logger.exception(str(err))
            result = {'exit_code': 1, 'output': packageName+" installation failed", 'error': str(err)}
        return result
    
    def remove(self, packages, dryRun):
        try:
            for package in packages:
                packageName=package['name']
                if string.find(packageName, ".tar.gz")>0 or string.find(package, ".tgz")>0:
                    self.tarballRemove(packageName, dryRun)
                else:
                    self.yumRemove(packageName, dryRun)
            result = {'exit_code': 0, 'output': 'Remove Successfully', 'error': ''}
        except Exception:
            traceback.print_exc(file=sys.stdout)
            result = {'exit_code': 1, 'output': packageName+" remove failed", 'error': package+" remove failed"}
        return result
        
    def info(self, packages):
        code = 0
        try:
            for package in packages:
                packageName=package['name']
                if string.find(packageName, ".tar.gz")>0 or string.find(package, ".tgz")>0:
                    result = self.tarballInfo(packageName)
                else:
                    result = self.yumInfo(packageName)
                if result['exit_code']!=0:
                    code = result['exit_code']
        except Exception:
            traceback.print_exc(file=sys.stdout)
            result = {'exit_code': 1, 'output': packageName+"", 'error': package+""}
        return {'exit_code': code, 'output': '', 'error': ''}

    def rpmDownload(self, package):
        if string.find(package, "http://")==0 or string.find(package, "https://")==0:
            urllib.urlretrieve(package, packageRunner.downloadDir+os.path.basename(package))
            tFile = packageRunner.downloadDir+os.path.basename(package)
            return tFile
        else:
            raise Exception('Unable to download '+package)

    def torrentFileListGenerator(self, info):
        fileList = []
        """Yield pieces from download file(s)."""
        piece_length = info['piece length']
        if 'files' in info: # yield pieces from a multi-file torrent
            piece = ""
            for file_info in info['files']:
                path = os.sep.join([info['name']] + file_info['path'])
                fileList.append(path)
        else: # yield pieces from a single file torrent
            path = info['name']
            fileList.append(path)
        return fileList

    def torrentInfo(self, torrentReference):
        torrentFile = open(torrentReference, "rb")
        metainfo = bencode.bdecode(torrentFile.read())
        info = metainfo['info']
        return self.torrentFileListGenerator(info)
    
    def torrentDownload(self, package):
        sh = shellRunner()
        startTime = time.time()
        script = ['transmission-daemon', '-y', '-O', '-M', '-w', packageRunner.downloadDir, '-g', packageRunner.hmsPrefix+'/var/cache/config']
        result = sh.run(script)
        for wait in [ 1, 1, 2, 2, 5 ]:
            script = ['transmission-remote', '-l']
            result = sh.run(script)
            if result['exit_code']==0:
                break
            time.sleep(wait)

        if result['exit_code']!=0:
            raise Exception('Unable to start transmission-daemon, exit_code:'+str(result['exit_code']))
        script = ['transmission-remote', '-a', package, '--torrent-done-script', '/usr/bin/hms-torrent-callback']
        result = sh.run(script)
        if result['exit_code']!=0:
            raise Exception('Unable to issue transmission-remote command')
        trackerComplete = packageRunner.hmsPrefix+'/var/tmp/tracker'
        while True:
            if os.path.exists(trackerComplete):
                break
            script = ['transmission-remote', '-t', '1', '--reannounce']
            sh.run(script)
            time.sleep(5)
        endTime = time.time()
        duration = endTime - startTime
        os.remove(trackerComplete)
        code = 0
        script = [ 'transmission-remote', '-t', '1', '-r' ]
        result = sh.run(script)
        if result['exit_code']!=0:
            code = result['exit_code']
            logger.warn('Can not remove torrent, output: '+result['output']+' error: '+result['error'])
        script = [ 'transmission-remote', '--exit' ]
        result = sh.run(script)
        if result['exit_code']!=0:
            code = result['exit_code']
            logger.warn('Can not shutdown torrent client, output: '+result['output']+' error: '+result['error'])
        output = "%s downloaded duration: %d seconds." % (package, duration)
        while True:
            """ Make sure transmission-daemon is properly terminated """
            script = [ 'transmission-remote', '-l' ]
            result = sh.run(script)
            if result['exit_code']!=0:
                break
        return {'exit_code': code, 'output': output, 'error': ''}
    
    def torrentInstall(self, package, dryRun):
        if string.find(package, "http://")==0:
            urllib.urlretrieve(package, packageRunner.downloadDir+os.path.basename(package))
            tFile = packageRunner.downloadDir+os.path.basename(package)
        else:
            tFile = package
        q.put(tFile)
        while not q.empty():
          self.torrentDownload(q.get())
        fileList = self.torrentInfo(tFile)
        try:
            list = []
            for p in fileList:
                if dryRun=='true':
                    continue
                if string.find(p, ".tar.gz")>0:
                    result = self.tarballInstall(p)
                elif string.find(p, ".rpm")>0:
                    list.append(packageRunner.downloadDir+p)
                else:
                    ''' Install fails if the file type is unknown. '''
                    list = []
                    result = []
                    result['exit_code']=1
                    break
            if len(list)>0:
                result = self.rpmInstall(list)
            if result['exit_code']!=0:
                raise Exception(p+" install error.")
        except Exception, err:
            return {'exit_code': 1, 'output': '', 'error': str(err)}
        return {'exit_code': 0, 'output': 'install completed.', 'error': ''}

    def tarballDownload(self, package):
        urllib.urlretrieve(package, packageRunner.downloadDir+os.path.basename(package))
        return os.path.basename(package)
        
    def tarballInfo(self, package):
        sh = shellRunner()
        script = [ 'cat', packageRunner.hmsPrefix+'/var/repos/'+package+'/info' ]
        return sh.run(script)

    def tarballInstall(self, package):
        sh = shellRunner()
        src = packageRunner.hmsPrefix+'/var/cache/downloads/'+package
        script = [ 'tar', 'fxz', src, '-C', packageRunner.softwarePrefix ]
        result = sh.run(script)
        if result['exit_code']!=0:
            err = 'Tarball decompress error, exit code: %d' % result['exit_code']
            raise Exception(err)
#        script = [ packageRunner.hmsPrefix+'/var/repos/'+package+'/preinstall' ]
#        result = sh.run(script)
#        if result['exit_code']!=0:
#            err = 'Preinstall script exit code: %d' % result['exit_code']
#            raise Exception(err)
#        script = [ 'tar', 'fxz', package, '-C', softwarePrefix ]
#        result = sh.run(script)
#        if result['exit_code']!=0:
#            err = 'Tarball decompress error, exit code: %d' % result['exit_code']
#            raise Exception(err)
#        script = [ packageRunner.hmsPrefix+'/var/repos'+package+'/postinstall' ]
        return result
        
    def tarballRemove(self, package, dryRun):
        sh = shellRunner()
        package = os.path.basename(package)
        if string.find(package, '.tgz')!=-1:
            package = package[:-4]            
        elif string.find(package, '.tar.gz')!=-1:
            package = package[:-7]
        src = packageRunner.softwarePrefix+'/'+package
        try:
            if dryRun!='true':
                shutil.rmtree(src)
            else:
                if os.path.exists(src)!=True:
                    err = packageRunner.softwarePrefix+'/'+package+' does not exist.'
                    raise Exception(err)
            result = {'exit_code': 0, 'output': package+' deleted', 'error': ''}
        except Exception, err:
            result = {'exit_code': 1, 'output': 'Error in deleting '+package, 'error': str(err)}
#        script = [ packageRunner.hmsPrefix+'/var/repos/'+package+'/prerm' ]
#        result = sh.run(script)
#        if result['exit_code']!=0:
#            err = 'Pre-remove script exit code: %d' % result['exit_code']
#            raise Exception(err)
#        script = [ packageRunner.hmsPrefix+'/var/repos/'+package+'/postrm' ]
        return result
    
    def rpmInstall(self, packages):
        sh = shellRunner()
        list = ' '.join([str(x) for x in packages])
        script = ['rpm', '-U', '--replacepkgs', list]
        return sh.run(script)
        
    def yumInstall(self, package, dryRun):
        sh = shellRunner()
        if dryRun=='true':
            script = [ 'yum', 'install', '-y', '--downloadonly', package ]
        else:
            script = [ 'yum', 'install', '-y', package ]
        return sh.run(script)
    
    def yumRemove(self, package, dryRun):
        sh = shellRunner()
        if dryRun=='true':
            script = [ 'rpm', '-e', '--test', package ]
        else:
            script = [ 'yum', 'erase', '-y', package ]
        return sh.run(script)
    
    def yumInfo(self, package):
        sh = shellRunner()
        script = [ 'yum', 'info', package ]
        return sh.run(script)

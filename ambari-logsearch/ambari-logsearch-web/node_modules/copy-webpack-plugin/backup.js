
            /*
            file.webpackTo = loaderUtils.interpolateName(
                {resourcePath: file.relativeFrom},
                file.webpackTo,
                {content});
            */
            
        xit('can use a glob to move multiple files to a non-root directory with name, hash and ext', (done) => {
            runEmit({
                expectedAssetKeys: [
                    'nested/file-22af64.txt',
                    'nested/directory/directoryfile-22af64.txt',
                    'nested/directory/nested/nestedfile-d41d8c.txt'
                ],
                patterns: [{
                    from: '**/*',
                    to: '[name]'
                }]
            })
            .then(done)
            .catch(done);
        });


        xit('allows pattern to contain name, hash or ext', (done) => {
            runEmit({
                expectedAssetKeys: [
                    'directory/directoryfile-22af64.txt'
                ],
                patterns: [{
                    from: 'directory/directoryfile.txt',
                    to: 'directory/[name]-[hash:6].[ext]'
                }]
            })
            .then(done)
            .catch(done);
        });
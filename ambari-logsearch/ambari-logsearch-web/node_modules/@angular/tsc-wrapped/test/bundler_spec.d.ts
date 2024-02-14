import { MetadataBundlerHost } from '../src/bundler';
import { MetadataCollector } from '../src/collector';
import { ModuleMetadata } from '../src/schema';
import { Directory } from './typescript.mocks';
export declare class MockStringBundlerHost implements MetadataBundlerHost {
    private dirName;
    private directory;
    collector: MetadataCollector;
    constructor(dirName: string, directory: Directory);
    getMetadataFor(moduleName: string): ModuleMetadata;
}
export declare const SIMPLE_LIBRARY: {
    'lib': {
        'index.ts': string;
        'src': {
            'index.ts': string;
            'one.ts': string;
            'two': {
                'index.ts': string;
            };
        };
    };
};

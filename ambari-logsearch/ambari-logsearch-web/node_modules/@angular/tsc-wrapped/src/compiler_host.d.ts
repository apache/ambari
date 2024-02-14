import * as ts from 'typescript';
import NgOptions from './options';
export declare function formatDiagnostics(d: ts.Diagnostic[]): string;
/**
 * Implementation of CompilerHost that forwards all methods to another instance.
 * Useful for partial implementations to override only methods they care about.
 */
export declare abstract class DelegatingHost implements ts.CompilerHost {
    protected delegate: ts.CompilerHost;
    constructor(delegate: ts.CompilerHost);
    getSourceFile: (fileName: string, languageVersion: ts.ScriptTarget, onError?: (message: string) => void) => ts.SourceFile;
    getCancellationToken: () => ts.CancellationToken;
    getDefaultLibFileName: (options: ts.CompilerOptions) => string;
    getDefaultLibLocation: () => string;
    writeFile: ts.WriteFileCallback;
    getCurrentDirectory: () => string;
    getDirectories: (path: string) => string[];
    getCanonicalFileName: (fileName: string) => string;
    useCaseSensitiveFileNames: () => boolean;
    getNewLine: () => string;
    fileExists: (fileName: string) => boolean;
    readFile: (fileName: string) => string;
    trace: (s: string) => void;
    directoryExists: (directoryName: string) => boolean;
}
export declare class MetadataWriterHost extends DelegatingHost {
    private ngOptions;
    private emitAllFiles;
    private metadataCollector;
    private metadataCollector1;
    constructor(delegate: ts.CompilerHost, ngOptions: NgOptions, emitAllFiles: boolean);
    private writeMetadata(emitFilePath, sourceFile);
    writeFile: ts.WriteFileCallback;
}
export declare class SyntheticIndexHost extends DelegatingHost {
    private normalSyntheticIndexName;
    private indexContent;
    private indexMetadata;
    constructor(delegate: ts.CompilerHost, syntheticIndex: {
        name: string;
        content: string;
        metadata: string;
    });
    fileExists: (fileName: string) => boolean;
    readFile: (fileName: string) => string;
    getSourceFile: (fileName: string, languageVersion: ts.ScriptTarget, onError?: (message: string) => void) => ts.SourceFile;
    writeFile: ts.WriteFileCallback;
}

import { CompilerHost, CompilerOptions, Program } from './api';
export declare function createProgram({rootNames, options, host, oldProgram}: {
    rootNames: string[];
    options: CompilerOptions;
    host: CompilerHost;
    oldProgram?: Program;
}): Program;

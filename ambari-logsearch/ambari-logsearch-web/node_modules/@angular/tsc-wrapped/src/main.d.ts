import * as ts from 'typescript';
import { CliOptions } from './cli_options';
import NgOptions from './options';
import { VinylFile } from './vinyl_file';
export { UserError } from './tsc';
export interface CodegenExtension {
    /**
     * Returns the generated file names.
     */
    (ngOptions: NgOptions, cliOptions: CliOptions, program: ts.Program, host: ts.CompilerHost): Promise<string[]>;
}
export declare function createBundleIndexHost(ngOptions: NgOptions, rootFiles: string[], host: ts.CompilerHost): {
    host: ts.CompilerHost;
    indexName?: string;
    errors?: ts.Diagnostic[];
};
export declare function main(project: string | VinylFile, cliOptions: CliOptions, codegen?: CodegenExtension, options?: ts.CompilerOptions): Promise<any>;

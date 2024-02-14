import { AotCompiler } from './compiler';
import { AotCompilerHost } from './compiler_host';
import { AotCompilerOptions } from './compiler_options';
import { StaticReflector } from './static_reflector';
/**
 * Creates a new AotCompiler based on options and a host.
 */
export declare function createAotCompiler(compilerHost: AotCompilerHost, options: AotCompilerOptions): {
    compiler: AotCompiler;
    reflector: StaticReflector;
};

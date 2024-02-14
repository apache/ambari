import { CompileNgModuleMetadata, CompileProviderMetadata } from './compile_metadata';
import { CompileReflector } from './compile_reflector';
import { OutputContext } from './util';
export declare class NgModuleCompileResult {
    ngModuleFactoryVar: string;
    constructor(ngModuleFactoryVar: string);
}
export declare class NgModuleCompiler {
    private reflector;
    constructor(reflector: CompileReflector);
    compile(ctx: OutputContext, ngModuleMeta: CompileNgModuleMetadata, extraProviders: CompileProviderMetadata[]): NgModuleCompileResult;
    createStub(ctx: OutputContext, ngModuleReference: any): void;
    private _createNgModuleFactory(ctx, reference, value);
}

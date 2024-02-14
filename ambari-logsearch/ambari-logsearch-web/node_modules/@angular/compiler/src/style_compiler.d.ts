import { CompileDirectiveMetadata, CompileStylesheetMetadata } from './compile_metadata';
import { UrlResolver } from './url_resolver';
import { OutputContext } from './util';
export declare class StylesCompileDependency {
    name: string;
    moduleUrl: string;
    setValue: (value: any) => void;
    constructor(name: string, moduleUrl: string, setValue: (value: any) => void);
}
export declare class CompiledStylesheet {
    outputCtx: OutputContext;
    stylesVar: string;
    dependencies: StylesCompileDependency[];
    isShimmed: boolean;
    meta: CompileStylesheetMetadata;
    constructor(outputCtx: OutputContext, stylesVar: string, dependencies: StylesCompileDependency[], isShimmed: boolean, meta: CompileStylesheetMetadata);
}
export declare class StyleCompiler {
    private _urlResolver;
    private _shadowCss;
    constructor(_urlResolver: UrlResolver);
    compileComponent(outputCtx: OutputContext, comp: CompileDirectiveMetadata): CompiledStylesheet;
    compileStyles(outputCtx: OutputContext, comp: CompileDirectiveMetadata, stylesheet: CompileStylesheetMetadata): CompiledStylesheet;
    needsStyleShim(comp: CompileDirectiveMetadata): boolean;
    private _compileStyles(outputCtx, comp, stylesheet, isComponentStylesheet);
    private _shimIfNeeded(style, shim);
}

import { CompileDirectiveMetadata, CompilePipeSummary } from '../compile_metadata';
import { CompileReflector } from '../compile_reflector';
import { CompilerConfig } from '../config';
import * as o from '../output/output_ast';
import { ElementSchemaRegistry } from '../schema/element_schema_registry';
import { TemplateAst } from '../template_parser/template_ast';
import { OutputContext } from '../util';
export declare class ViewCompileResult {
    viewClassVar: string;
    rendererTypeVar: string;
    constructor(viewClassVar: string, rendererTypeVar: string);
}
export declare class ViewCompiler {
    private _config;
    private _reflector;
    private _schemaRegistry;
    constructor(_config: CompilerConfig, _reflector: CompileReflector, _schemaRegistry: ElementSchemaRegistry);
    compileComponent(outputCtx: OutputContext, component: CompileDirectiveMetadata, template: TemplateAst[], styles: o.Expression, usedPipes: CompilePipeSummary[]): ViewCompileResult;
}

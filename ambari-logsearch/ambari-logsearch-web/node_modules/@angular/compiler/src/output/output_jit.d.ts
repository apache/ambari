import { EmitterVisitorContext } from './abstract_emitter';
import { AbstractJsEmitterVisitor } from './abstract_js_emitter';
import * as o from './output_ast';
export declare function jitStatements(sourceUrl: string, statements: o.Statement[]): {
    [key: string]: any;
};
export declare class JitEmitterVisitor extends AbstractJsEmitterVisitor {
    private _evalArgNames;
    private _evalArgValues;
    private _evalExportedVars;
    createReturnStmt(ctx: EmitterVisitorContext): void;
    getArgs(): {
        [key: string]: any;
    };
    visitExternalExpr(ast: o.ExternalExpr, ctx: EmitterVisitorContext): any;
    visitDeclareVarStmt(stmt: o.DeclareVarStmt, ctx: EmitterVisitorContext): any;
    visitDeclareFunctionStmt(stmt: o.DeclareFunctionStmt, ctx: EmitterVisitorContext): any;
    visitDeclareClassStmt(stmt: o.ClassStmt, ctx: EmitterVisitorContext): any;
}

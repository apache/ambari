export interface FileMap<T> { [fileName: string]: T; }

/** A class that maintains the module dependency graph of output JS files. */
export class ModulesManifest {
  /** Map of googmodule module name to file name */
  private moduleToFileName: FileMap<string> = {};
  /** Map of file name to arrays of imported googmodule module names */
  private referencedModules: FileMap<string[]> = {};

  addModule(fileName: string, module: string): void {
    this.moduleToFileName[module] = fileName;
    this.referencedModules[fileName] = [];
  }

  addReferencedModule(fileName: string, resolvedModule: string): void {
    this.referencedModules[fileName].push(resolvedModule);
  }

  get modules(): string[] {
    return Object.keys(this.moduleToFileName);
  }

  getFileNameFromModule(module: string): string {
    return this.moduleToFileName[module];
  }

  get fileNames(): string[] {
    return Object.keys(this.referencedModules);
  }

  getReferencedModules(fileName: string): string[] {
    return this.referencedModules[fileName];
  }
}

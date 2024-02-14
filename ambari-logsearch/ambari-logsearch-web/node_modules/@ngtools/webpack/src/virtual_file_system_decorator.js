"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
class VirtualFileSystemDecorator {
    constructor(_inputFileSystem, _webpackCompilerHost) {
        this._inputFileSystem = _inputFileSystem;
        this._webpackCompilerHost = _webpackCompilerHost;
    }
    _readFileSync(path) {
        if (this._webpackCompilerHost.fileExists(path, false)) {
            return this._webpackCompilerHost.readFile(path);
        }
        return null;
    }
    _statSync(path) {
        if (this._webpackCompilerHost.fileExists(path, false)
            || this._webpackCompilerHost.directoryExists(path, false)) {
            return this._webpackCompilerHost.stat(path);
        }
        return null;
    }
    _readDirSync(path) {
        if (this._webpackCompilerHost.directoryExists(path, false)) {
            const dirs = this._webpackCompilerHost.getDirectories(path);
            const files = this._webpackCompilerHost.getFiles(path);
            return files.concat(dirs);
        }
        return null;
    }
    stat(path, callback) {
        const result = this._statSync(path);
        if (result) {
            callback(null, result);
        }
        else {
            this._inputFileSystem.stat(path, callback);
        }
    }
    readdir(path, callback) {
        const result = this._readDirSync(path);
        if (result) {
            callback(null, result);
        }
        else {
            this._inputFileSystem.readdir(path, callback);
        }
    }
    readFile(path, callback) {
        const result = this._readFileSync(path);
        if (result) {
            callback(null, result);
        }
        else {
            this._inputFileSystem.readFile(path, callback);
        }
    }
    readJson(path, callback) {
        this._inputFileSystem.readJson(path, callback);
    }
    readlink(path, callback) {
        this._inputFileSystem.readlink(path, callback);
    }
    statSync(path) {
        const result = this._statSync(path);
        return result || this._inputFileSystem.statSync(path);
    }
    readdirSync(path) {
        const result = this._readDirSync(path);
        return result || this._inputFileSystem.readdirSync(path);
    }
    readFileSync(path) {
        const result = this._readFileSync(path);
        return result || this._inputFileSystem.readFileSync(path);
    }
    readJsonSync(path) {
        return this._inputFileSystem.readJsonSync(path);
    }
    readlinkSync(path) {
        return this._inputFileSystem.readlinkSync(path);
    }
    purge(changes) {
        if (typeof changes === 'string') {
            this._webpackCompilerHost.invalidate(changes);
        }
        else if (Array.isArray(changes)) {
            changes.forEach((fileName) => this._webpackCompilerHost.invalidate(fileName));
        }
        if (this._inputFileSystem.purge) {
            this._inputFileSystem.purge(changes);
        }
    }
}
exports.VirtualFileSystemDecorator = VirtualFileSystemDecorator;
//# sourceMappingURL=/users/hansl/sources/angular-cli/src/virtual_file_system_decorator.js.map
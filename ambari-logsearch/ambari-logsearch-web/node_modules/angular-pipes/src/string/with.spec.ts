// idea from https://github.com/a8m/angular-filter

import {WithPipe} from './with.pipe';

describe('WithPipe', () => {

    let pipe: WithPipe;
    let text = 'The Flash Reverse';

    beforeEach(() => {
        pipe = new WithPipe();
    });

    it('Must return string', () => expect(pipe.transform(text)).toEqual(text));
    it('Must return string #2', () => expect(pipe.transform(text,'','')).toEqual(text));
    it('Must return string #3', () => expect(pipe.transform(text,'','',true)).toEqual(text));
    it('Must return string #4', () => expect(pipe.transform(text,null,null)).toEqual(text));
    it('Must return string #5', () => expect(pipe.transform(text,null,null,true)).toEqual(text));

    it('Must return boolean true for starts #1', () => expect(pipe.transform(text,'the',null)).toEqual(true));
    it('Must return boolean true for starts #2', () => expect(pipe.transform(text,'The',null)).toEqual(true));
    it('Must return boolean true for starts #3', () => expect(pipe.transform(text,'The',null,true)).toEqual(true));
    it('Must return boolean false for starts #4', () => expect(pipe.transform(text,'the',null,true)).toEqual(false));
    it('Must return boolean false for starts #5', () => expect(pipe.transform(text,'Then',null,true)).toEqual(false));
    it('Must return boolean false for starts #6', () => expect(pipe.transform(text,'Then',null)).toEqual(false));

    it('Must return boolean true for ends #1', () => expect(pipe.transform(text,null,'Reverse')).toEqual(true));
    it('Must return boolean true for ends #2', () => expect(pipe.transform(text,null,'reverse')).toEqual(true));
    it('Must return boolean true for ends #3', () => expect(pipe.transform(text,null,'Reverse',true)).toEqual(true));
    it('Must return boolean false for ends #4', () => expect(pipe.transform(text,null,'reverse',true)).toEqual(false));
    it('Must return boolean false for ends #5', () => expect(pipe.transform(text,null,'Reverbe',true)).toEqual(false));
    it('Must return boolean false for ends #6', () => expect(pipe.transform(text,null,'Reverbe')).toEqual(false));

    it('Must return boolean true for start & ends #1', () => expect(pipe.transform(text,'The','Reverse')).toEqual(true));
    it('Must return boolean true for start & ends #2', () => expect(pipe.transform(text,'the','reverse')).toEqual(true));
    it('Must return boolean true for start & ends #3', () => expect(pipe.transform(text,'The','Reverse',true)).toEqual(true));

    it('Must return boolean false for start & ends #4', () => expect(pipe.transform(text,'the','reverse',true)).toEqual(false));
    it('Must return boolean false for start & ends #5', () => expect(pipe.transform(text,'Then','Reverbe',true)).toEqual(false));
    it('Must return boolean false for start & ends #6', () => expect(pipe.transform(text,'Then','Reverbe')).toEqual(false));

});
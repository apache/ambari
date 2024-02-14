// idea from https://github.com/a8m/angular-filter

import { WrapPipe } from './wrap.pipe';

describe('WrapPipe', () => {

    let pipe: WrapPipe;

    beforeEach(() => {
        pipe = new WrapPipe();
    });

    it('Must return string with wrap', () => {
        expect(pipe.transform('zombie','/')).toEqual('/zombie/');
    });

    it('Must return string with wrap & ends', () => {
        expect(pipe.transform('zombie','{{','}}')).toEqual('{{zombie}}');
    });

});
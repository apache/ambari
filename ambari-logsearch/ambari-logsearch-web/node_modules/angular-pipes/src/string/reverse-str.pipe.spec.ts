import { ReverseStrPipe } from './reverse-str.pipe';



describe('ReverseStrPipe', () => {

    let pipe: ReverseStrPipe;

    beforeEach(() => {
       pipe = new ReverseStrPipe();
    });

    it('Should returned the reversed string', () => {

       expect(pipe.transform('abcd')).toEqual('dcba');
    });

    it('Should returned the reversed string #2', () => {

       expect(pipe.transform('abcd aa')).toEqual('aa dcba');
    });

    it('Should returned the reversed string #3', () => {

       expect(pipe.transform('hello world')).toEqual('dlrow olleh');
    });

    it('Should returned the reversed string #4', () => {

       expect(pipe.transform('hello world')).toEqual('dlrow olleh');
    });

    it('Should return the reversed string #5', () => {

       expect(pipe.transform('foo 𝌆 bar mañana mañana')).toEqual('anañam anañam rab 𝌆 oof');
    });

    it('Should return the value unchanged', () => {

       expect(pipe.transform('a')).toEqual('a');
    });

    it('Should return the value unchanged #2', () => {

       expect(pipe.transform(null)).toEqual(null);
    });

});
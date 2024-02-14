import { CapitalizePipe } from './capitalize.pipe';
        

describe('CapitalizePipe', () => {
    
    let pipe: CapitalizePipe;
    
    beforeEach(() => {
       pipe = new CapitalizePipe(); 
    });
    
    it('Should returned the capitalized string', () => {
       
       expect(pipe.transform('abcd', false)).toEqual('Abcd');
    });
    
    it('Should returned the capitalized string #2', () => {
       
       expect(pipe.transform('abcd aa', false)).toEqual('Abcd aa');
    });
    
    it('Should returned the capitalized string #3', () => {
       
       expect(pipe.transform('abCD aA', false)).toEqual('Abcd aa');
    });
    
    it('Should returned the capitalized string #4', () => {
       
       expect(pipe.transform('abCD aA', true)).toEqual('Abcd Aa');
    });
    
    it('Should return the value unchanged', () => {
       
       expect(pipe.transform(1, null)).toEqual(1); 
    });
   
});
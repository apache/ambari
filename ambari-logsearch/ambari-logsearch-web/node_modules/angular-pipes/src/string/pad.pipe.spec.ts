import { PadPipe } from './pad.pipe';
        


describe('PadPipe', () => {
    
    let pipe: PadPipe;
    
    beforeEach(() => {
       pipe = new PadPipe(); 
    });
    
    
    it('Should return pad string', () => {
       
        const value = 'aaa';
        const result = pipe.transform(value, 4);
        expect(result).toEqual(' aaa');
        expect(value).toEqual('aaa');
    });
    
    it('Should return pad string', () => {
       
        const result = pipe.transform('aaa', 5);
        expect(result).toEqual(' aaa ');
    });
    
    it('Should return pad string', () => {
       
        const result = pipe.transform('aaa', 5, 'b');
        expect(result).toEqual('baaab');
    });
    
    
    it('Should return pad string', () => {
       
        const result = pipe.transform('aaa', 5, 'bb');
        expect(result).toEqual('bbaaa');
    });
    
    it('Should return pad string', () => {
       
        const result = pipe.transform('aaa', 2);
        expect(result).toEqual('aaa');
    });
    
    
    it('Should return pad string', () => {
       
        const result = pipe.transform('aaa', 4, 'bbb');
        expect(result).toEqual('aaa');
    });
    
    it('Should return the value unchanged', () => {
       
       expect(pipe.transform(1, null, null)).toEqual(1); 
    });
    
   
});
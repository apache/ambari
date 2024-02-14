import { LeftPadPipe } from './left-pad.pipe';
        


describe('LeftPadPipe', () => {
    
    let pipe: LeftPadPipe;
    
    beforeEach(() => {
       pipe = new LeftPadPipe(); 
    });
    
    
    it('Should return left pad string', () => {
       
        const result = pipe.transform('aaa', 4);
        expect(result).toEqual(' aaa');
    });
    
    it('Should return left pad string', () => {
       
        const result = pipe.transform('aaa', 5);
        expect(result).toEqual('  aaa');
    });
    
    it('Should return left pad string', () => {
       
        const result = pipe.transform('aaa', 5, 'b');
        expect(result).toEqual('bbaaa');
    });
    
    
    it('Should return left pad string', () => {
       
        const result = pipe.transform('aaa', 5, 'bb');
        expect(result).toEqual('bbaaa');
    });
    
    it('Should return left pad string', () => {
       
        const result = pipe.transform('aaa', 2);
        expect(result).toEqual('aaa');
    });
    
    
    it('Should return left pad string', () => {
       
        const result = pipe.transform('aaa', 4, 'bbb');
        expect(result).toEqual('aaa');
    });
    
    it('Should return the value unchanged', () => {
       
       expect(pipe.transform(1, null)).toEqual(1); 
    });
   
});
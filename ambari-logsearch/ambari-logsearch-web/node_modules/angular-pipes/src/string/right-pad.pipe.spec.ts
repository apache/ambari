import { RightPadPipe } from './right-pad.pipe';
        


describe('RightPadPipe', () => {
    
    let pipe: RightPadPipe;
    
    beforeEach(() => {
       pipe = new RightPadPipe(); 
    });
    
    
    it('Should return right pad string', () => {
       
        const result = pipe.transform('aaa', 4);
        expect(result).toEqual('aaa ');
    });
    
    it('Should return right pad string', () => {
       
        const result = pipe.transform('aaa', 5);
        expect(result).toEqual('aaa  ');
    });
    
    it('Should return right pad string', () => {
       
        const result = pipe.transform('aaa', 5, 'b');
        expect(result).toEqual('aaabb');
    });
    
    
    it('Should return right pad string', () => {
       
        const result = pipe.transform('aaa', 5, 'bb');
        expect(result).toEqual('aaabb');
    });
    
    it('Should return right pad string', () => {
       
        const result = pipe.transform('aaa', 2);
        expect(result).toEqual('aaa');
    });
    
    
    it('Should return right pad string', () => {
       
        const result = pipe.transform('aaa', 4, 'bbb');
        expect(result).toEqual('aaa');
    });
    
    it('Should return the value unchanged', () => {
       
       expect(pipe.transform(1, null, null)).toEqual(1); 
    });
    
   
});
'''
 Register와 그에 해당하는 번호를 저장하는 Table이다.
 하나만 생성된다.
 '''
class RegisterTable : 

    def __init__(self) :
        self.registerMap = {} # register 이름-번호를 저장하는 딕셔너리 자료형
        self.registerMap['A'] = 0 # self를 쓰지 않으면 에러
        self.registerMap['X'] = 1
        self.registerMap['L'] = 2
        self.registerMap['B'] = 3
        self.registerMap['S'] = 4
        self.registerMap['T'] = 5
        self.registerMap['F'] = 6
        self.registerMap['PC'] = 8
        self.registerMap['SW'] = 9
    '''
	 인자에 해당하는 이름의 register의 번호를 알려준다.
	 @param name : 번호를 찾을 register의 이름
	 @return name이 null이면 0을 리턴한다.
	 name이 null이 아닌 경우, Map을 찾아 register의 번호를 리턴한다.
	 없는 register의 경우 -1을 리턴한다.
    '''
    def getNumber(self, name) :
        if name == None : 
            return 0
        
        if name in self.registerMap : 
            return self.registerMap[name]
        else :
            return -1;
        



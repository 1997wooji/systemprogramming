'''
명령어 하나하나의 구체적인 정보는 Instruction클래스에 담긴다.
instruction과 관련된 정보들을 저장하고 기초적인 연산을 수행한다.
'''
class Instruction : 
    '''
    클래스를 선언하면서 일반문자열을 즉시 구조에 맞게 파싱한다.
	@param line : instruction 명세파일로부터 한줄씩 가져온 문자열
    '''
    def __init__(self, line) : 
        self.mnemonic = None  #명령어 이름을 저장
        self.opcode = None #명령어 opcode 숫자를 저장
        self.numberOfOperand = None #명령어의 operand 개수를 저장
        self.form = None # format이 있어서 form으로 수정
        # instruction이 몇 바이트 명령어인지 저장. 이후 편의성을 위함
        self.parsing(line)

    '''
    일반 문자열을 파싱하여 instruction 정보를 파악하고 저장한다.
    @param line : instruction 명세파일로부터 한줄씩 가져온 문자열
    '''
    def parsing(self, line) : 
        arr = line.split("/") # python은 split으로 문자열을 나눈다
        self.form = int(arr[0])
        self.opcode = int(arr[1],16) # hex
        self.numberOfOperand = int(arr[2]) #문자를 숫자로 변환



'''
모든 instruction의 정보를 관리하는 클래스. instruction data들을 저장한다
또한 instruction 관련 연산, 예를 들면 목록을 구축하는 함수, 관련 정보를 제공하는 함수 등을 제공 한다.
'''
class InstTable : 


    '''
    클래스 초기화. 파싱을 동시에 처리한다.
	@param instFile : instuction에 대한 명세가 저장된 파일 이름
    '''
    def __init__(self,instFile) : 
        '''
        inst.data 파일을 불러와 저장하는 공간.
	    명령어의 이름을 집어넣으면 해당하는 Instruction의 정보들을 리턴할 수 있다.
        '''
        self.instMap = {} # None으로 하면 NoneType에 값을 넣을 수 없다고 나오게 된다. 주의.
        self.openFile(instFile)

    '''
	입력받은 이름의 파일을 열고 해당 내용을 파싱하여 instMap에 저장한다.
	기계어 목록파일 형식은 자유롭게 구현한다. 다음과 같이 inst.data에 저장되어있다.
	@param fileName : instuction에 대한 명세가 저장된 파일 이름
	    ========================================================
	 		   이름-형식/기계어 코드/오퍼랜드의 갯수/NULL
		=========================================================	
	이름이 key, 나머지는 Instruction으로 생성되어 value로 저장된다.   	
    '''
    def openFile(self,fileName) : 
        file = open('./' + fileName, 'r') # 읽기 모드로 open
        line = None
        while True : 
            line = file.readline()
            if not line : # 더이상 읽을 것이 없으면 빈 문자열 ''을 리턴한다
                break # None, 빈문자열 '' == False로 본다
            line = line[:-1] # 개행 제거
            arr = line.split("-")
            self.instMap[arr[0]]=Instruction(arr[1]) # 정의하는 class가 위에 있어야함
        file.close()


    '''
    해당 명령어가 몇 byte짜리 명령어인지 byte수를 리턴하는 함수
	@param mnemonic : byte를 찾을 명령어
	@return 명령어가 소요하는 byte. 명령어가 아닐 경우 -1을 리턴한다.
    '''
    def searchByte(self, mnemonic) : 
        if mnemonic[0] == '+' : 
            return 4
        else : 
            if mnemonic in self.instMap :  # 해당 key가 dictionary 안에 있는지
                return self.instMap[mnemonic].form # class 멤버 참존
            else : 
                return -1
    '''
    해당 명령어가 몇개의 operand를 갖고 있는지 operand 수를 리턴하는 함수
	@param mnemonic : operand 개수를 찾을 명령어
	@return 명령어가 갖는 operand의 개수, 명령어가 아닐 경우 -1을 리턴한다.
    '''
    def searchNumberOfOperand(self, mnemonic) : 
        if mnemonic[0] == '+' : 
            mnemonic = mnemonic[1:] # substring을 다음과 같이 쓴다
    
        if mnemonic in self.instMap :  # 해당 key가 dictionary 안에 있는지
            return self.instMap[mnemonic].numberOfOperand # class 멤버 참존
        else : 
            return -1

    '''
    해당 명령어의 opcode를 찾는 함수
	@param mnemonic : opcode를 찾을 명령어
	@return 명령어의 opcode, 명령어가 아닐 경우 -1을 리턴한다.
    '''
    def searchOpcode(self, mnemonic) : 
        if mnemonic[0] == '+' : 
            mnemonic = mnemonic[1:]
    
        if mnemonic in self.instMap :  # 해당 key가 dictionary 안에 있는지
            return self.instMap[mnemonic].opcode # class 멤버 참존
        else : 
            return -1

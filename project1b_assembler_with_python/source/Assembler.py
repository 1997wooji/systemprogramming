import RegisterTable
import LiteralTable
import InstTable
import SymbolTable
import TokenTable

# python은 꼭 self 붙여줄 것! 변수 선언을 미리 하는 게 아니라 그때그때 하기 때문에 self를 안붙이면 새로운 변수로 받아들임

'''
Assembler : 
이 프로그램은 SIC/XE 머신을 위한 Assembler 프로그램의 메인 루틴이다.
프로그램의 수행 작업은 다음과 같다. 
1) 처음 시작하면 Instruction 명세를 읽어들여서 assembler를 세팅한다. 
2) 사용자가 작성한 input 파일을 읽어들인 후 저장한다. 
3) input 파일의 문장들을 단어별로 분할하고 의미를 파악해서 정리한다. (pass1) 
4) 분석된 내용을 바탕으로 컴퓨터가 사용할 수 있는 object code를 생성한다. (pass2) 
'''
class Assembler :
    MAX_LENGTH = 30 # //Text Record 최대

    '''
	클래스 초기화. instruction Table을 초기화와 동시에 세팅한다.
	@param instFile : instruction 명세를 작성한 파일 이름. 
    '''
    def __init__(self,instFile) : 

        self.lineList = [] # 읽어들인 input 파일의 내용을 한 줄 씩 저장하는 공간.
        self.symtabList = [] # 프로그램의 section별로 symbol table을 저장하는 공간
        self.literaltabList = [] # 프로그램의 section별로 literal table을 저장하는 공간
        self.TokenList = [] # 프로그램의 section별로 프로그램을 저장하는 공간

        # Token, 또는 지시어에 따라 만들어진 오브젝트 코드들을 출력 형태로 저장하는 공간.  
        # 필요한 경우 String 대신 별도의 클래스를 선언하여 ArrayList를 교체해도 무방함.
        self.codeList = []
        self.locctr = None # location counter

        '''
        파이썬은 C언어처럼 *을 붙여서 역참조하여 원본값을 바꾸는 일은 불가능 하다.
        그 역할을 위한 변수들
        '''
        # pass2에서 쓰일 변수
        self.textRecordLength = 0 # textRecord 길이
        self.textRecord = None # object code를 누적하는 buffer
        self.startLocation = None # text record 시작 location을 저장하는 데에 쓰인다
        self.instTable = InstTable.InstTable(instFile) # instruction 명세를 저장한 공간
        # Python은 .py file이 패키지인 셈이다. 따라서 class를 쓰려면 package.class로 참조해야 한다.
        self.registerTable = RegisterTable.RegisterTable() # 레지스터 정보를 저장하는 공간
    
    '''
	inputFile을 읽어들여서 lineList에 저장한다.
	@param inputFile : input 파일 이름.
	'''
    def loadInputFile(self,inputFile) : 
        file = open('./' + inputFile, 'r') # 읽기 모드로 open
        line = None
        while True : 
            line = file.readline() # python의 readline은 개행까지 읽어들인다
            line = line[:-1] # 개행 제거
            if not line : # 더이상 읽을 것이 없으면 빈 문자열 ''을 리턴한다
                break # None, 빈문자열 '' == False로 본다
            self.lineList.append(line)
        file.close()

    '''
    pass1 과정을 수행한다.
	 1) 프로그램 소스를 스캔하여 토큰단위로 분리한 뒤 토큰테이블 생성
	 2) label을 symbolTable에 정리 
	주의사항 : SymbolTable과 TokenTable은 프로그램의 section별로 하나씩 선언되어야 한다.
    '''
    def pass1(self) : 
        self.locctr=0
        section = 0
        tokenIndex = 0
        
        # 먼저 main program COPY에 대한 section별 table을 만든다
        self.symtabList.append(SymbolTable.SymbolTable())
        self.literaltabList.append(LiteralTable.LiteralTable())
        self.TokenList.append(TokenTable.TokenTable(self.symtabList[section], self.literaltabList[section],self.instTable, self.registerTable))
        # python은 생성시 new keyword가 없다

        for line in self.lineList : 
            if line[0] == '.' : 
                continue # 주석일 경우 건너뛴다.

            self.TokenList[section].putToken(line)
            token = self.TokenList[section].getToken(tokenIndex)

            if token.operator=='CSECT' : 
                # CSECT인 경우
                # 원래는 방금 전 넣었던 Token을 삭제하려 했으나
                # 나중에 section이 끝나는 point를 알기 위해 END로 operator를 고쳐준다.
                # section별로 하나씩 END point가 생긴다.
                # 새로운 section에 맞는 table들 생성 후 다시 CESCT를 Token으로 넣기
                self.TokenList[section].getToken(tokenIndex).operator = 'END'
                self.TokenList[section].programLength = self.locctr # //program길이 추가 
                section += 1 # section이 늘어남
                tokenIndex = 0 # 초기화
                self.locctr = 0 # 초기화

                self.symtabList.append(SymbolTable.SymbolTable())
                self.literaltabList.append(LiteralTable.LiteralTable())
                self.TokenList.append(TokenTable.TokenTable(self.symtabList[section],self.literaltabList[section],self.instTable,self.registerTable))
                self.TokenList[section].putToken(line)
            
            token = self.TokenList[section].getToken(tokenIndex) # 최근 추가한 token에 대해
            token.location = self.locctr # 현재 주소를 Token의 location에 할당(추후 pass2에서 쓰인다)

            # label symtab에 추가
            if token.label != None and token.label != '' :
                self.symtabList[section].putSymbol(token.label,self.locctr)

            # operator의 경우
            if self.instTable.searchByte(token.operator) > 0 : 
                token.byteSize = self.instTable.searchByte(token.operator) # token byteSize 설정
                self.locctr += self.instTable.searchByte(token.operator) # location counter에 누적한다.
            else : 
                # directivies인 경우
                if token.operator == 'EQU' : 
                    # label에 할당된 주소를 고쳐줘야할 수도 있음.
                    # 이미 symtab의 해당 label은 location에 현재 주소가 잘 할당된 상태.
                    # operand가 * 인 경우는 locctr을 넣으면 되지만, 아닐 경우 따로 값을 구해야 한다.
                    # - 연산에 대해서만 처리가 가능하다.
                    # EQU MAXLEN, EQU BUFEND-BUFFER는 가능하지만, EQU 4096이나, +의 경우에는 불가능하다

                    if token.operand[0][0] != '*' :
                        arr = token.operand[0].split('-')
                        EQUValue = self.symtabList[section].search(arr[0]) # symbol table에 들어갈 EQU 값
                        if len(arr) > 1 : # 빼야할 게 있으면
                            EQUValue -= self.symtabList[section].search(arr[1])
                        self.symtabList[section].modifySymbol(token.label, EQUValue)

                elif token.operator == 'RESW' : 
                    self.locctr += int(token.operand[0])*3 # 할당 개수*3byte 차지한다
                elif token.operator == 'RESB' : 
                    self.locctr += int(token.operand[0]) # 할당 개수*1byte 차지한다
                elif token.operator == 'WORD' : 
                    token.byteSize = 3 # byteSize설정
                    self.locctr += 3 # 3byte만 사용한다
                elif token.operator == 'BYTE' : 
                    if token.operand[0][0] == 'X' : 
                        # X인 경우
                        arr = token.operand[0].split("'")
                        token.byteSize = len(arr[1])/2 # byteSize설정
                        self.locctr += len(arr[1])/2 # hex이므로 2로 나눈다
                    else : 
                        # C인 경우
                        arr = token.operand[0].split("'")
                        token.byteSize = len(arr[1])
                        self.locctr += len(arr[1]) # char이므로 문자 길이 그대로 byte를 소요한다
                elif token.operator == 'LTORG' : 
                    # literal들에 대해 주소 할당, locctr update
                    self.locctr = self.literaltabList[section].addAddLiteral(self.locctr)
                elif token.operator == 'END' : 
                    # 할당하지 않은 literal들에 대해 주소 할당, locctr update
                    self.locctr = self.literaltabList[section].addAddLiteral(self.locctr)
                    self.TokenList[section].programLength = self.locctr # program length추가
                    section += 1 # section 증가

            # pass1에서 EXTDEF, EXTREF는 넘어간다.
            # Literal 처리
            if len(token.operand) > 0 and token.operand[0] != '' and token.operand[0][0] == '=' :
                arr = token.operand[0].split("'") # =C' 혹은 =X' 분리

                if token.operand[0][1] == 'X' : 
                    # X인 경우
                    self.literaltabList[section].putLiteral(arr[1],-1)
                else : 
                    # C인 경우
                    self.literaltabList[section].putLiteral(arr[1],-2)

            tokenIndex += 1 # tokenIndex 증가



    '''
    작성된 SymbolTable들을 출력형태에 맞게 출력한다.
	@param fileName : 저장되는 파일 이름
	null인자로 들어올 경우 콘솔로 출력한다
    '''
    def printSymbolTable(self, fileName) : 
        if fileName == None : 
            # console 출력
            print('------------* Symbol Table *------------')
            for st in self.symtabList :
                i=0
                for line in st.symbolList : 
                    print('%s\t%X'%(line, int(st.locationList[i])))
                    i += 1

        else : 
            # file 출력
            file = open('./' + fileName, 'w') # 없으면 새로운 파일 자동 생성!
            
            for st in self.symtabList :
                i=0
                for line in st.symbolList : 
                    buf = line +'\t'+ '%X'%(int(st.locationList[i])) + '\n'
                    file.write(buf)
                    i += 1

            file.close()



    '''
    작성된 LiteralTable들을 출력형태에 맞게 출력한다.
	@param fileName : 저장되는 파일 이름
	null인자로 들어올 경우 콘솔로 출력한다
    '''
    def printLiteralTable(self, fileName) : 
        if fileName == None : 
            # console 출력
            print('------------* Literal Table *------------')
            for lt in self.literaltabList :
                i=0
                for line in lt.literalList : 
                    print('%s\t%X'%(line, int(lt.locationList[i])))
                    i += 1

        else : 
            # file 출력
            file = open('./' + fileName, 'w') # 없으면 새로운 파일 자동 생성!
            
            for lt in self.literaltabList :
                i=0
                for line in lt.literalList :
                    buf = line + '\t' +'%X'%(int(lt.locationList[i])) + '\n'
                    file.write(buf)
                    i += 1

            file.close()



    '''
    pass2 과정을 수행한다.
	 1) 분석된 내용을 바탕으로 object code를 생성하여 codeList에 저장.
    '''
    def pass2(self) :
        section = 0 #section index

        for tt in self.TokenList : 
            self.textRecord = ''
            literalRecord = False  # 해당 section의 literal이 이미 record 되었는지 확인하기 위한 변수 

            for index in range(len(tt.tokenList)) : # 들어있는 token만큼
                t = tt.tokenList[index] 

                if t.operator=='START' or t.operator=='CSECT' or t.operator == 'EXTREF' or t.operator == 'EXTDEF': 
                    # 이 경우 바로 object code를 받아 list에 저장하면 된다.
                    tt.makeObjectCode(index)
                    objectCode = tt.getObjectCode(index)
                    self.codeList.append(objectCode)
                elif t.operator == 'END' : 
                    #만약 LTORG가 없어 literal이 적히지 않았다면, Literal record를 추가한다
                    if not literalRecord : 
                        self.addLiteralRecord(section)
                    # Text Record가 남아있을 수 있으므로 먼저 끝낸다
                    self.finTextRecord()
                    # section을 이동하기 전, Modify Record 추가
                    self.addModifyRecord(section);
                    #  E record 추가
                    if section == 0 : 
                        # main program일경우
                        self.codeList.append('E%06X'%(0))
                        # 원래는 START address를 저장했어야 하는데
                        # 어디에 loading될지 모르기 때문에 웬만하면 시작 주소가 0이므로 0으로 처리하였다.
                    else : 
                        self.codeList.append('E')
                elif t.operator == 'RESW' : 
                    # TextRecord 끝내기
                    self.finTextRecord()
                elif t.operator == 'RESB' : 
                    # TextRecord 끝내기
                    self.finTextRecord()
                elif t.operator == 'LTORG' : 
                    # Literal들을 추가한다
                    self.addLiteralRecord(section)
                    literalRecord = True
                elif t.operator == "EQU" : 
                     # 아무것도 하지 않는다 (pass로 표현)
                    pass
                else : 
                    # 나머지 instruction, word, byte의 경우
                    # 바로 codeList에 추가하면 안된다. TextRecord이므로 더해야함
                    tt.makeObjectCode(index)
                    objectCode = tt.getObjectCode(index)
                    self.addTextRecord(objectCode, t.byteSize, t.location)
                
            section += 1 # section 이동


    '''
    작성된 codeList를 출력형태에 맞게 출력한다.
	@param fileName : 저장되는 파일 이름
	fileName이 null일 경우, 콘솔에 출력한다
    '''
    def printObjectCode(self,fileName) : 
        if fileName == None : 
            # console 출력
            for line in self.codeList : 
                print(line)
        else : 
            # file 출력
            file = open('./' + fileName, 'w') # 없으면 새로운 파일 자동 생성!
            
            for line in self.codeList : 
                file.write(line+'\n')

            file.close()


    '''
    object code를 Text record에 누적시킨다
	만약 record 길이가 30을 넘어가면 현재 Text record는 끝내고, 새로운 record를 만들어 넣는다.
	@param objectCode : 추가할 object code
	@param byteSize : 추가할 object code의 길이
    '''
    def addTextRecord(self,objectCode,byteSize,location) : 
        if self.textRecordLength ==0 : 
            # 만약 record가 시작되지 않은 경우, 새로 시작
            self.startLocation = location
            self.textRecord = objectCode
            self.textRecordLength = byteSize
        elif (self.textRecordLength + byteSize) > self.MAX_LENGTH : 
            # 길이가 30byte를 넘어가는 경우, 현재 record를 끝내고 새로 시작
            self.finTextRecord()
            self.startLocation=location
            self.textRecord = objectCode
            self.textRecordLength = byteSize
        else : 
            # 길이가 30byte를 넘어가지 않는 경우 누적만 시킨다
            self.textRecord += objectCode
            self.textRecordLength += byteSize #length도 누적시킨다


    '''
    현재 Text record를 기록하고 끝낸다
    '''
    def finTextRecord(self) : 
        if self.textRecordLength == 0 : 
            return # record가 시작되지 않은 경우 끝낼 것도 없으므로 그냥 돌아간다
        else : 
            # 끝낼 record가 있는 경우
            self.textRecord = 'T%06X%02X'%(int(self.startLocation), int(self.textRecordLength)) + self.textRecord
            # //시작주소, 길이, 지금까지 누적한 Record들을 한 번에 Text Record로 만든다.
            self.codeList.append(self.textRecord)
            self.textRecord = ''
            self.textRecordLength=0

    '''
    해당 section의 Modify Record를 모두 object code list에 추가한다
	@param section : 해당 section index
    '''
    def addModifyRecord(self, section) : 
        for m in self.TokenList[section].modifyList : 
            self.codeList.append(m.objectCode)

    '''
    해당 section의 Literal을 모두 text record에 추가한다
	@param section : 해당 section index
    '''
    def addLiteralRecord(self, section) : 
        index=0 # literal List index
        byteSize = None
        line = ''

        for literal in self.literaltabList[section].literalList : 
            if self.literaltabList[section].charOrHexList[index] == -1 : 
                # X인 경우
                byteSize = len(literal)/2
                line = literal
            else :
                # C인 경우
                byteSize = len(literal)
                for i in range(byteSize) : 
                    # 해당 literal 각각을 ASCII code로 변경한다
                    # Character.getNumericValue(c)는 unicode로 변경된다
                    # ASCII code로 바꾸기 위해 int로 casting 한다.
                    line += '%X'%(ord(literal[i])) # ord : 문자에 대응하는 ascii code return
            # //Literal이 Text Record의 시작이 될 수도 있으므로 location이 필요하다.
            self.addTextRecord(line,byteSize,self.literaltabList[section].locationList[index])
            index += 1



'''
어셈블러의 메인 루틴
'''
def main() : 
    assembler = Assembler('inst.data')
    assembler.loadInputFile('input.txt')
    assembler.pass1()
    assembler.printSymbolTable('symtab_20160458.txt')
    assembler.printLiteralTable('literaltab_20160458.txt')
    assembler.pass2()
    assembler.printObjectCode('output_20160458.txt')
    # python의 메모리 할당 해제는 자동으로 이루어진다.

if __name__ == '__main__' : 
    main()
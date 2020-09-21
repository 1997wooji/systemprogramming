import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;


/**
 * Assembler : 
 * 이 프로그램은 SIC/XE 머신을 위한 Assembler 프로그램의 메인 루틴이다.
 * 프로그램의 수행 작업은 다음과 같다. 
 * 1) 처음 시작하면 Instruction 명세를 읽어들여서 assembler를 세팅한다. 
 * 2) 사용자가 작성한 input 파일을 읽어들인 후 저장한다. 
 * 3) input 파일의 문장들을 단어별로 분할하고 의미를 파악해서 정리한다. (pass1) 
 * 4) 분석된 내용을 바탕으로 컴퓨터가 사용할 수 있는 object code를 생성한다. (pass2) 
 * 
 * 
 * 작성중의 유의사항 : 
 *  1) 새로운 클래스, 새로운 변수, 새로운 함수 선언은 얼마든지 허용됨. 단, 기존의 변수와 함수들을 삭제하거나 완전히 대체하는 것은 안된다.
 *  2) 마찬가지로 작성된 코드를 삭제하지 않으면 필요에 따라 예외처리, 인터페이스 또는 상속 사용 또한 허용됨.
 *  3) 모든 void 타입의 리턴값은 유저의 필요에 따라 다른 리턴 타입으로 변경 가능.
 *  4) 파일, 또는 콘솔창에 한글을 출력시키지 말 것. (채점상의 이유. 주석에 포함된 한글은 상관 없음)
 * 
 *     
 *  + 제공하는 프로그램 구조의 개선방법을 제안하고 싶은 분들은 보고서의 결론 뒷부분에 첨부 바랍니다. 내용에 따라 가산점이 있을 수 있습니다.
 */
public class Assembler {
	public static final int MAX_LENGTH=30; //Text Record 최대
	
	/** instruction 명세를 저장한 공간 */
	InstTable instTable;
	/** 읽어들인 input 파일의 내용을 한 줄 씩 저장하는 공간. */
	ArrayList<String> lineList;
	/** 프로그램의 section별로 symbol table을 저장하는 공간*/
	ArrayList<SymbolTable> symtabList;
	/** 프로그램의 section별로 literal table을 저장하는 공간*/
	ArrayList<LiteralTable> literaltabList;
	/** 프로그램의 section별로 프로그램을 저장하는 공간*/
	ArrayList<TokenTable> TokenList;
	/** 
	 * Token, 또는 지시어에 따라 만들어진 오브젝트 코드들을 출력 형태로 저장하는 공간.   
	 * 필요한 경우 String 대신 별도의 클래스를 선언하여 ArrayList를 교체해도 무방함.
	 */
	ArrayList<String> codeList;
	
	/** 레지스터 정보를 저장하는 공간 */
	RegisterTable registerTable;
	
	int locctr; //location counter
	
	/** pass2에서 쓰일 변수*/
	int textRecordLength; // textRecord 길이와
	String textRecord; // object code를 누적하는 buffer
	int startLocation; // text record 시작 location을 저장하는 데에 쓰인다
	
	/**
	 * 클래스 초기화. instruction Table을 초기화와 동시에 세팅한다.
	 * 
	 * @param instFile : instruction 명세를 작성한 파일 이름. 
	 */
	public Assembler(String instFile) {
		instTable = new InstTable(instFile);
		lineList = new ArrayList<String>();
		symtabList = new ArrayList<SymbolTable>();
		literaltabList = new ArrayList<LiteralTable>();
		TokenList = new ArrayList<TokenTable>();
		codeList = new ArrayList<String>();
		registerTable = new RegisterTable();
	}

	/** 
	 * 어셈블러의 메인 루틴
	 */
	public static void main(String[] args) {
		
		Assembler assembler = new Assembler("inst.data");
		
		try {
			assembler.loadInputFile("input.txt");
			assembler.pass1();
			assembler.printSymbolTable("symtab_20160458.txt");
			assembler.printLiteralTable("literaltab_20160458.txt");
			assembler.pass2();
			assembler.printObjectCode("output_20160458.txt");
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		//메모리 해제는 garbage collector의 대상이 되어 해제됨
	}

	/**
	 * inputFile을 읽어들여서 lineList에 저장한다.
	 * @param inputFile : input 파일 이름.
	 */
	private void loadInputFile(String inputFile) throws IOException {
		File file = new File("./"+inputFile);
		
		if(!file.exists())
			throw new IOException("there is no file");
		
		BufferedReader br=new BufferedReader(new FileReader(file));
		String line;
		while((line = br.readLine())!=null){
			lineList.add(new String(line));
		}
		br.close();
		
	}

	/** 
	 * pass1 과정을 수행한다.
	 *   1) 프로그램 소스를 스캔하여 토큰단위로 분리한 뒤 토큰테이블 생성
	 *   2) label을 symbolTable에 정리
	 *    
	 *    주의사항 : SymbolTable과 TokenTable은 프로그램의 section별로 하나씩 선언되어야 한다.
	 */
	private void pass1() throws Exception {
		
		locctr=0;
		int section=0;
		int tokenIndex=0;
		Token token; //package 같으므로 사용 가능 
		int byteSize;
		
		//먼저 main program COPY에 대한 section별 table을 만든다
		symtabList.add(new SymbolTable());
		literaltabList.add(new LiteralTable());
		TokenList.add(new TokenTable(symtabList.get(section), literaltabList.get(section),instTable,registerTable));
		
		for (String line : lineList){
			
			if(line.charAt(0)=='.') //주석일 경우 건너뛴다.
				continue;
			 

			TokenList.get(section).putToken(line);
			token = TokenList.get(section).getToken(tokenIndex);
			
			if(token.operator.equals("CSECT")){
				//CSECT인 경우
				// 원래는 방금 전 넣었던 Token을 삭제하려 했으나
				// 나중에 section이 끝나는 point를 알기 위해 END로 operator를 고쳐준다.
				// section별로 하나씩 END point가 생긴다.
				// 새로운 section에 맞는 table들 생성 후 다시 CESCT를 Token으로 넣기
				TokenList.get(section).getToken(tokenIndex).operator=new String("END");
				TokenList.get(section).programLength=locctr; //program길이 추가
				
				section ++; // section이 늘어남
				tokenIndex=0; //초기화
				locctr=0; //초기화
				
				symtabList.add(new SymbolTable());
				literaltabList.add(new LiteralTable());
				TokenList.add(new TokenTable(symtabList.get(section), literaltabList.get(section),instTable,registerTable));
				TokenList.get(section).putToken(line);
			}
			
			token = TokenList.get(section).getToken(tokenIndex);//최근 추가한 token에 대해
			token.location=locctr; //현재 주소를 Token의 location에 할당(추후 pass2에서 쓰인다)
			
			/** label symtab에 추가 */
			if(token.label!=null){
				symtabList.get(section).putSymbol(token.label, locctr);
			}
			
			/** operator인 경우 */
			if((byteSize = instTable.searchByte(token.operator))>0){
				
				token.byteSize=byteSize; //token byteSize 설정
				locctr += byteSize; //location counter에 누적한다.
				
			}else{
				/** directivies인 경우 */
				if(token.operator.equals("EQU")){
					// label에 할당된 주소를 고쳐줘야할 수도 있음.
					// 이미 symtab의 해당 label은 location에 현재 주소가 잘 할당된 상태.
					// operand가 * 인 경우는 locctr을 넣으면 되지만, 아닐 경우 따로 값을 구해야 한다.
					// - 연산에 대해서만 처리가 가능하다.
					// EQU MAXLEN, EQU BUFEND-BUFFER는 가능하지만, EQU 4096이나, +의 경우에는 불가능하다
					if(token.operand[0].charAt(0)!='*'){
						int EQUValue = 0; //symbol table에 들어갈 EQU 값
						StringTokenizer str = new StringTokenizer(token.operand[0],"-");
						EQUValue = symtabList.get(section).search(str.nextToken());
						if(str.hasMoreTokens()){
							EQUValue -= symtabList.get(section).search(str.nextToken());
						}
						
						symtabList.get(section).modifySymbol(token.label, EQUValue);
						}
					
				}else if(token.operator.equals("RESW")){
					locctr += Integer.parseInt(token.operand[0]) * 3; //할당 개수*3byte 차지한다
					
				}else if(token.operator.equals("RESB")){
					locctr += Integer.parseInt(token.operand[0]);  //할당 개수*1byte 차지한다
					
				}else if(token.operator.equals("WORD")){
					token.byteSize=3; //byteSize설정
					locctr += 3; //3byte만 사용한다
					
				}else if(token.operator.equals("BYTE")){
					if(token.operand[0].charAt(0)=='X'){
						//X인 경우
						StringTokenizer str = new StringTokenizer(token.operand[0],"'");
						str.nextToken(); //X걸러내고
						byteSize = str.nextToken().length()/2;
						token.byteSize=byteSize; //byteSize설정
						locctr += byteSize; //hex이므로 2로 나눈다
					}else{
						//C인 경우
						StringTokenizer str = new StringTokenizer(token.operand[0],"'");
						str.nextToken(); //C걸러내고
						byteSize = str.nextToken().length();
						token.byteSize=byteSize; //byteSize설정
						locctr += byteSize; //char이므로 문자 길이 그대로 byte를 소요한다
					}
					
				}else if(token.operator.equals("LTORG")){
					// literal들에 대해 주소 할당, locctr update
					locctr = literaltabList.get(section).addAddLiteral(locctr);
				}else if(token.operator.equals("END")){
					// 할당하지 않은 literal들에 대해 주소 할당, locctr update
					locctr = literaltabList.get(section).addAddLiteral(locctr);
					TokenList.get(section).programLength=locctr; //program length추가
					section++;
				}
				
			}
			//pass1에서 EXTREF, EXTDEF는 넘어간다.
			
			/** Literal 처리 */
			if(token.operand!=null && token.operand[0]!=null && token.operand[0].charAt(0)=='='){
				StringTokenizer str = new StringTokenizer(token.operand[0], "'"); //=C'EOF'에서 EOF만 빼내기 위해
				str.nextToken(); //=C' 혹은 =X' 분리
				
				if(token.operand[0].charAt(1)=='X'){
					//X인 경우
					literaltabList.get(section).putLiteral(str.nextToken(),-1);
				}else{
					//C인 경우
					literaltabList.get(section).putLiteral(str.nextToken(),-2);
				}
				
			}
			
			tokenIndex++;
			
		}
		
	}
	
	/**
	 * 작성된 SymbolTable들을 출력형태에 맞게 출력한다.
	 * @param fileName : 저장되는 파일 이름
	 * null인자로 들어올 경우 콘솔로 출력한다
	 */
	private void printSymbolTable(String fileName) throws IOException {
		
		if(fileName==null){
			//콘솔 출력
			System.out.println("--------* Symbol Table *----------");
			for(SymbolTable st : symtabList){
				int i=0;
				for(String str : st.symbolList){
					
					System.out.format("%s\t%X\n",str,st.locationList.get(i));
					i++;
				}
			}
		}else{
			//File 출력
			File file = new File("./"+fileName);
			String buffer;
			
			if(!file.exists())
				file.createNewFile();
			
			FileWriter fw = new FileWriter(file);
			
			for(SymbolTable st : symtabList){
				
				int i=0;
				
				for(String str :  st.symbolList){
					
					buffer = str+"\t"+Integer.toHexString(st.locationList.get(i)).toUpperCase()+"\n";;
					fw.write(buffer);
					i++;
				}
			}
			
			fw.flush(); //혹시 적히지 않은 것이 있다면 흘려보냄
			fw.close();
		}
		
	}

	
	/**
	 * 작성된 LiteralTable들을 출력형태에 맞게 출력한다.
	 * @param fileName : 저장되는 파일 이름
	 * null인자로 들어올 경우 콘솔로 출력한다
	 */
	private void printLiteralTable(String fileName) throws IOException {
		
		if(fileName==null){
			//콘솔 출력
			System.out.println("--------* Literal Table *----------");
			for(LiteralTable lt : literaltabList){
				
				int i=0;
				
				for(String str :  lt.literalList){
					
					System.out.format("%s\t%X\n",str,lt.locationList.get(i));
					i++;
				}
				
			}
		}else{
			//File 출력
			File file = new File("./"+fileName);
			String buffer;
			
			if(!file.exists())
				file.createNewFile();
			
			FileWriter fw = new FileWriter(file);
			
			for(LiteralTable lt : literaltabList){
				
				int i=0;
				
				for(String str :  lt.literalList){
					
					buffer = str+"\t"+Integer.toHexString(lt.locationList.get(i)).toUpperCase()+"\n";
					fw.write(buffer);
					//fw.flush();
					i++;
				}
			}
			fw.flush(); //혹시 적히지 않은 것이 있다면 흘려보냄
			fw.close();
			
			
			
		}
	}

	/**
	 * pass2 과정을 수행한다.
	 *   1) 분석된 내용을 바탕으로 object code를 생성하여 codeList에 저장.
	 */
	private void pass2() {

		String objectCode; // 단 한 명령어의 object code 임시 저장소
		int section = 0; // section Index
		boolean literalRecord = false; // 해당 section의 literal이 이미 record 되었는지 확인하기 위한 변수 
		Token t;
		
		for(TokenTable tt : TokenList){
			
			textRecord = new String("");
			literalRecord = false;
			for(int index=0 ; index < tt.tokenList.size() ; index++){
				
				t = tt.tokenList.get(index);
				
				if(t.operator.equals("START") || t.operator.equals("CSECT") || t.operator.equals("EXTDEF") || t.operator.equals("EXTREF")){
					// 이 경우 바로 object code를 받아 list에 저장하면 된다.
					tt.makeObjectCode(index);
					objectCode = tt.getObjectCode(index);
					codeList.add(new String(objectCode));
					
				}else if(t.operator.equals("END")){
					// 만약 LTORG가 없어 literal이 적히지 않았다면, Literal record를 추가한다
					if(!literalRecord)
						addLiteralRecord(section);
					// Text Record가 남아있을 수 있으므로 먼저 끝낸다
					finTextRecord();
					// section을 이동하기 전, Modify Record 추가
					addModifyRecord(section);
					// E record 추가
					if(section==0){
						//main program일경우
						codeList.add(String.format("E%06X", 0));
						// 원래는 START address를 저장했어야 하는데
						// 어디에 loading될지 모르기 때문에 웬만하면 시작 주소가 0이므로 0으로 처리하였다.
					}else{
						codeList.add(new String("E")); //아닌 경우, E만 추가한다
					}
					
				}else if(t.operator.equals("RESW")){
					// TextRecord 끝내기
					finTextRecord();
					
				}else if(t.operator.equals("RESB")){
					// TextRecord 끝내기
					finTextRecord();
					
				}else if(t.operator.equals("LTORG")){
					//Literal들을 추가한다
					addLiteralRecord(section);
					literalRecord = true;
					
				}else if(t.operator.equals("EQU")){
					//아무것도 하지 않는다
					
				}else {
					// 나머지 instruction, word, byte의 경우
					// 바로 codeList에 추가하면 안된다. TextRecord이므로 더해야함
					tt.makeObjectCode(index);
					objectCode = tt.getObjectCode(index);
					addTextRecord(objectCode, t.byteSize,t.location);
				}
				
			}
			section++; //section 이동
		}
		
	}
	
	/**
	 * 작성된 codeList를 출력형태에 맞게 출력한다.
	 * @param fileName : 저장되는 파일 이름
	 * fileName이 null일 경우, 콘솔에 출력한다
	 */
	private void printObjectCode(String fileName) throws IOException {
		
		if(fileName==null){
			//console 출력
			for(String str : codeList){
				System.out.println(str);
			}
			
		}else{
			//File 출력
			
			File file = new File("./"+fileName);
			
			if(!file.exists())
				file.createNewFile();
			
			FileWriter fw = new FileWriter(file);
			
			for(String str : codeList){
				fw.write(str+"\n");
			}
			fw.flush(); //혹시 적히지 않은 것이 있다면 흘려보냄
			fw.close();
		}
		
	}
	
	/**
	 * object code를 Text record에 누적시킨다
	 * 만약 record 길이가 30을 넘어가면 현재 Text record는 끝내고, 새로운 record를 만들어 넣는다.
	 * @param objectCode : 추가할 object code
	 * @param byteSize : 추가할 object code의 길이
	 */
	private void addTextRecord(String objectCode, int byteSize, int location){
		

		if(textRecord.length()==0){
			//만약 record가 시작되지 않은 경우, 새로 시작
			startLocation = location; //시작 주소를 저장한다
			textRecord += objectCode;
			textRecordLength = byteSize;
			
		}else if( (textRecordLength + byteSize) > MAX_LENGTH){
			//길이가 30byte를 넘어가는 경우, 현재 record를 끝내고 새로 시작
			finTextRecord();
			startLocation = location;
			textRecord += objectCode;
			textRecordLength = byteSize;
			
		}else{
			//길이가 30byte를 넘어가지 않는 경우 누적만 시킨다
			textRecord += objectCode;
			textRecordLength += byteSize; //length도 누적시킨다
			
		}
		
		
	}
	
	/**
	 * 현재 Text record를 기록하고 끝낸다.
	 */
	private void finTextRecord(){

		if(textRecord.length()==0){ //record가 시작되지 않은 경우 끝낼 것도 없으므로 그냥 돌아간다
			return; 
		}else{ //끝낼 record가 있는 경우
			textRecord = String.format("T%06X%02X",startLocation,textRecordLength) + textRecord;
			//시작주소, 길이, 지금까지 누적한 Record들을 한 번에 Text Record로 만든다.
			codeList.add(new String(textRecord));
			textRecord = new String(""); //새로 시작하기 위해 만들어준다
			textRecordLength = 0; //textRecord와 Length reset
		}
	}
	
	/**
	 * 해당 section의 Modify Record를 모두 object code list에 추가한다
	 * @param section : 해당 section index
	 */
	private void addModifyRecord(int section){
		
		for(Modify m : TokenList.get(section).modifyList){
			codeList.add(new String(m.objectCode));
		}
		
	}
	
	/**
	 * 해당 section의 Literal을 모두 text record에 추가한다
	 * @param section : 해당 section index
	 */
	private void addLiteralRecord(int section){
		int index=0; //literal List index
		int byteSize;
		String str=new String("");
		
		for(String literal : TokenList.get(section).literalTab.literalList){
			
			if(TokenList.get(section).literalTab.charOrHexList.get(index)==-1){
				//X인 경우
				byteSize=literal.length()/2;
				str = literal;
			}else{
				//C인 경우
				byteSize=literal.length();
				
				for(int i=0 ; i<byteSize ; i++){
					 //해당 literal 각각을 ASCII code로 변경한다
					//Character.getNumericValue(c)는 unicode로 변경된다
					//ASCII code로 바꾸기 위해 int로 casting 한다.
					str += String.format("%X", (int)literal.charAt(i));
				}
			}
			//Literal이 Text Record의 시작이 될 수도 있으므로 location이 필요하다.
			addTextRecord(str,byteSize,TokenList.get(section).literalTab.locationList.get(index));
			index++;
		}
	}
	
}


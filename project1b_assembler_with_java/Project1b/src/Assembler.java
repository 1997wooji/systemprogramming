import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;


/**
 * Assembler : 
 * �� ���α׷��� SIC/XE �ӽ��� ���� Assembler ���α׷��� ���� ��ƾ�̴�.
 * ���α׷��� ���� �۾��� ������ ����. 
 * 1) ó�� �����ϸ� Instruction ���� �о�鿩�� assembler�� �����Ѵ�. 
 * 2) ����ڰ� �ۼ��� input ������ �о���� �� �����Ѵ�. 
 * 3) input ������ ������� �ܾ�� �����ϰ� �ǹ̸� �ľ��ؼ� �����Ѵ�. (pass1) 
 * 4) �м��� ������ �������� ��ǻ�Ͱ� ����� �� �ִ� object code�� �����Ѵ�. (pass2) 
 * 
 * 
 * �ۼ����� ���ǻ��� : 
 *  1) ���ο� Ŭ����, ���ο� ����, ���ο� �Լ� ������ �󸶵��� ����. ��, ������ ������ �Լ����� �����ϰų� ������ ��ü�ϴ� ���� �ȵȴ�.
 *  2) ���������� �ۼ��� �ڵ带 �������� ������ �ʿ信 ���� ����ó��, �������̽� �Ǵ� ��� ��� ���� ����.
 *  3) ��� void Ÿ���� ���ϰ��� ������ �ʿ信 ���� �ٸ� ���� Ÿ������ ���� ����.
 *  4) ����, �Ǵ� �ܼ�â�� �ѱ��� ��½�Ű�� �� ��. (ä������ ����. �ּ��� ���Ե� �ѱ��� ��� ����)
 * 
 *     
 *  + �����ϴ� ���α׷� ������ ��������� �����ϰ� ���� �е��� ������ ��� �޺κп� ÷�� �ٶ��ϴ�. ���뿡 ���� �������� ���� �� �ֽ��ϴ�.
 */
public class Assembler {
	public static final int MAX_LENGTH=30; //Text Record �ִ�
	
	/** instruction ���� ������ ���� */
	InstTable instTable;
	/** �о���� input ������ ������ �� �� �� �����ϴ� ����. */
	ArrayList<String> lineList;
	/** ���α׷��� section���� symbol table�� �����ϴ� ����*/
	ArrayList<SymbolTable> symtabList;
	/** ���α׷��� section���� literal table�� �����ϴ� ����*/
	ArrayList<LiteralTable> literaltabList;
	/** ���α׷��� section���� ���α׷��� �����ϴ� ����*/
	ArrayList<TokenTable> TokenList;
	/** 
	 * Token, �Ǵ� ���þ ���� ������� ������Ʈ �ڵ���� ��� ���·� �����ϴ� ����.   
	 * �ʿ��� ��� String ��� ������ Ŭ������ �����Ͽ� ArrayList�� ��ü�ص� ������.
	 */
	ArrayList<String> codeList;
	
	/** �������� ������ �����ϴ� ���� */
	RegisterTable registerTable;
	
	int locctr; //location counter
	
	/** pass2���� ���� ����*/
	int textRecordLength; // textRecord ���̿�
	String textRecord; // object code�� �����ϴ� buffer
	int startLocation; // text record ���� location�� �����ϴ� ���� ���δ�
	
	/**
	 * Ŭ���� �ʱ�ȭ. instruction Table�� �ʱ�ȭ�� ���ÿ� �����Ѵ�.
	 * 
	 * @param instFile : instruction ���� �ۼ��� ���� �̸�. 
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
	 * ������� ���� ��ƾ
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
		
		//�޸� ������ garbage collector�� ����� �Ǿ� ������
	}

	/**
	 * inputFile�� �о�鿩�� lineList�� �����Ѵ�.
	 * @param inputFile : input ���� �̸�.
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
	 * pass1 ������ �����Ѵ�.
	 *   1) ���α׷� �ҽ��� ��ĵ�Ͽ� ��ū������ �и��� �� ��ū���̺� ����
	 *   2) label�� symbolTable�� ����
	 *    
	 *    ���ǻ��� : SymbolTable�� TokenTable�� ���α׷��� section���� �ϳ��� ����Ǿ�� �Ѵ�.
	 */
	private void pass1() throws Exception {
		
		locctr=0;
		int section=0;
		int tokenIndex=0;
		Token token; //package �����Ƿ� ��� ���� 
		int byteSize;
		
		//���� main program COPY�� ���� section�� table�� �����
		symtabList.add(new SymbolTable());
		literaltabList.add(new LiteralTable());
		TokenList.add(new TokenTable(symtabList.get(section), literaltabList.get(section),instTable,registerTable));
		
		for (String line : lineList){
			
			if(line.charAt(0)=='.') //�ּ��� ��� �ǳʶڴ�.
				continue;
			 

			TokenList.get(section).putToken(line);
			token = TokenList.get(section).getToken(tokenIndex);
			
			if(token.operator.equals("CSECT")){
				//CSECT�� ���
				// ������ ��� �� �־��� Token�� �����Ϸ� ������
				// ���߿� section�� ������ point�� �˱� ���� END�� operator�� �����ش�.
				// section���� �ϳ��� END point�� �����.
				// ���ο� section�� �´� table�� ���� �� �ٽ� CESCT�� Token���� �ֱ�
				TokenList.get(section).getToken(tokenIndex).operator=new String("END");
				TokenList.get(section).programLength=locctr; //program���� �߰�
				
				section ++; // section�� �þ
				tokenIndex=0; //�ʱ�ȭ
				locctr=0; //�ʱ�ȭ
				
				symtabList.add(new SymbolTable());
				literaltabList.add(new LiteralTable());
				TokenList.add(new TokenTable(symtabList.get(section), literaltabList.get(section),instTable,registerTable));
				TokenList.get(section).putToken(line);
			}
			
			token = TokenList.get(section).getToken(tokenIndex);//�ֱ� �߰��� token�� ����
			token.location=locctr; //���� �ּҸ� Token�� location�� �Ҵ�(���� pass2���� ���δ�)
			
			/** label symtab�� �߰� */
			if(token.label!=null){
				symtabList.get(section).putSymbol(token.label, locctr);
			}
			
			/** operator�� ��� */
			if((byteSize = instTable.searchByte(token.operator))>0){
				
				token.byteSize=byteSize; //token byteSize ����
				locctr += byteSize; //location counter�� �����Ѵ�.
				
			}else{
				/** directivies�� ��� */
				if(token.operator.equals("EQU")){
					// label�� �Ҵ�� �ּҸ� ��������� ���� ����.
					// �̹� symtab�� �ش� label�� location�� ���� �ּҰ� �� �Ҵ�� ����.
					// operand�� * �� ���� locctr�� ������ ������, �ƴ� ��� ���� ���� ���ؾ� �Ѵ�.
					// - ���꿡 ���ؼ��� ó���� �����ϴ�.
					// EQU MAXLEN, EQU BUFEND-BUFFER�� ����������, EQU 4096�̳�, +�� ��쿡�� �Ұ����ϴ�
					if(token.operand[0].charAt(0)!='*'){
						int EQUValue = 0; //symbol table�� �� EQU ��
						StringTokenizer str = new StringTokenizer(token.operand[0],"-");
						EQUValue = symtabList.get(section).search(str.nextToken());
						if(str.hasMoreTokens()){
							EQUValue -= symtabList.get(section).search(str.nextToken());
						}
						
						symtabList.get(section).modifySymbol(token.label, EQUValue);
						}
					
				}else if(token.operator.equals("RESW")){
					locctr += Integer.parseInt(token.operand[0]) * 3; //�Ҵ� ����*3byte �����Ѵ�
					
				}else if(token.operator.equals("RESB")){
					locctr += Integer.parseInt(token.operand[0]);  //�Ҵ� ����*1byte �����Ѵ�
					
				}else if(token.operator.equals("WORD")){
					token.byteSize=3; //byteSize����
					locctr += 3; //3byte�� ����Ѵ�
					
				}else if(token.operator.equals("BYTE")){
					if(token.operand[0].charAt(0)=='X'){
						//X�� ���
						StringTokenizer str = new StringTokenizer(token.operand[0],"'");
						str.nextToken(); //X�ɷ�����
						byteSize = str.nextToken().length()/2;
						token.byteSize=byteSize; //byteSize����
						locctr += byteSize; //hex�̹Ƿ� 2�� ������
					}else{
						//C�� ���
						StringTokenizer str = new StringTokenizer(token.operand[0],"'");
						str.nextToken(); //C�ɷ�����
						byteSize = str.nextToken().length();
						token.byteSize=byteSize; //byteSize����
						locctr += byteSize; //char�̹Ƿ� ���� ���� �״�� byte�� �ҿ��Ѵ�
					}
					
				}else if(token.operator.equals("LTORG")){
					// literal�鿡 ���� �ּ� �Ҵ�, locctr update
					locctr = literaltabList.get(section).addAddLiteral(locctr);
				}else if(token.operator.equals("END")){
					// �Ҵ����� ���� literal�鿡 ���� �ּ� �Ҵ�, locctr update
					locctr = literaltabList.get(section).addAddLiteral(locctr);
					TokenList.get(section).programLength=locctr; //program length�߰�
					section++;
				}
				
			}
			//pass1���� EXTREF, EXTDEF�� �Ѿ��.
			
			/** Literal ó�� */
			if(token.operand!=null && token.operand[0]!=null && token.operand[0].charAt(0)=='='){
				StringTokenizer str = new StringTokenizer(token.operand[0], "'"); //=C'EOF'���� EOF�� ������ ����
				str.nextToken(); //=C' Ȥ�� =X' �и�
				
				if(token.operand[0].charAt(1)=='X'){
					//X�� ���
					literaltabList.get(section).putLiteral(str.nextToken(),-1);
				}else{
					//C�� ���
					literaltabList.get(section).putLiteral(str.nextToken(),-2);
				}
				
			}
			
			tokenIndex++;
			
		}
		
	}
	
	/**
	 * �ۼ��� SymbolTable���� ������¿� �°� ����Ѵ�.
	 * @param fileName : ����Ǵ� ���� �̸�
	 * null���ڷ� ���� ��� �ַܼ� ����Ѵ�
	 */
	private void printSymbolTable(String fileName) throws IOException {
		
		if(fileName==null){
			//�ܼ� ���
			System.out.println("--------* Symbol Table *----------");
			for(SymbolTable st : symtabList){
				int i=0;
				for(String str : st.symbolList){
					
					System.out.format("%s\t%X\n",str,st.locationList.get(i));
					i++;
				}
			}
		}else{
			//File ���
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
			
			fw.flush(); //Ȥ�� ������ ���� ���� �ִٸ� �������
			fw.close();
		}
		
	}

	
	/**
	 * �ۼ��� LiteralTable���� ������¿� �°� ����Ѵ�.
	 * @param fileName : ����Ǵ� ���� �̸�
	 * null���ڷ� ���� ��� �ַܼ� ����Ѵ�
	 */
	private void printLiteralTable(String fileName) throws IOException {
		
		if(fileName==null){
			//�ܼ� ���
			System.out.println("--------* Literal Table *----------");
			for(LiteralTable lt : literaltabList){
				
				int i=0;
				
				for(String str :  lt.literalList){
					
					System.out.format("%s\t%X\n",str,lt.locationList.get(i));
					i++;
				}
				
			}
		}else{
			//File ���
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
			fw.flush(); //Ȥ�� ������ ���� ���� �ִٸ� �������
			fw.close();
			
			
			
		}
	}

	/**
	 * pass2 ������ �����Ѵ�.
	 *   1) �м��� ������ �������� object code�� �����Ͽ� codeList�� ����.
	 */
	private void pass2() {

		String objectCode; // �� �� ��ɾ��� object code �ӽ� �����
		int section = 0; // section Index
		boolean literalRecord = false; // �ش� section�� literal�� �̹� record �Ǿ����� Ȯ���ϱ� ���� ���� 
		Token t;
		
		for(TokenTable tt : TokenList){
			
			textRecord = new String("");
			literalRecord = false;
			for(int index=0 ; index < tt.tokenList.size() ; index++){
				
				t = tt.tokenList.get(index);
				
				if(t.operator.equals("START") || t.operator.equals("CSECT") || t.operator.equals("EXTDEF") || t.operator.equals("EXTREF")){
					// �� ��� �ٷ� object code�� �޾� list�� �����ϸ� �ȴ�.
					tt.makeObjectCode(index);
					objectCode = tt.getObjectCode(index);
					codeList.add(new String(objectCode));
					
				}else if(t.operator.equals("END")){
					// ���� LTORG�� ���� literal�� ������ �ʾҴٸ�, Literal record�� �߰��Ѵ�
					if(!literalRecord)
						addLiteralRecord(section);
					// Text Record�� �������� �� �����Ƿ� ���� ������
					finTextRecord();
					// section�� �̵��ϱ� ��, Modify Record �߰�
					addModifyRecord(section);
					// E record �߰�
					if(section==0){
						//main program�ϰ��
						codeList.add(String.format("E%06X", 0));
						// ������ START address�� �����߾�� �ϴµ�
						// ��� loading���� �𸣱� ������ �����ϸ� ���� �ּҰ� 0�̹Ƿ� 0���� ó���Ͽ���.
					}else{
						codeList.add(new String("E")); //�ƴ� ���, E�� �߰��Ѵ�
					}
					
				}else if(t.operator.equals("RESW")){
					// TextRecord ������
					finTextRecord();
					
				}else if(t.operator.equals("RESB")){
					// TextRecord ������
					finTextRecord();
					
				}else if(t.operator.equals("LTORG")){
					//Literal���� �߰��Ѵ�
					addLiteralRecord(section);
					literalRecord = true;
					
				}else if(t.operator.equals("EQU")){
					//�ƹ��͵� ���� �ʴ´�
					
				}else {
					// ������ instruction, word, byte�� ���
					// �ٷ� codeList�� �߰��ϸ� �ȵȴ�. TextRecord�̹Ƿ� ���ؾ���
					tt.makeObjectCode(index);
					objectCode = tt.getObjectCode(index);
					addTextRecord(objectCode, t.byteSize,t.location);
				}
				
			}
			section++; //section �̵�
		}
		
	}
	
	/**
	 * �ۼ��� codeList�� ������¿� �°� ����Ѵ�.
	 * @param fileName : ����Ǵ� ���� �̸�
	 * fileName�� null�� ���, �ֿܼ� ����Ѵ�
	 */
	private void printObjectCode(String fileName) throws IOException {
		
		if(fileName==null){
			//console ���
			for(String str : codeList){
				System.out.println(str);
			}
			
		}else{
			//File ���
			
			File file = new File("./"+fileName);
			
			if(!file.exists())
				file.createNewFile();
			
			FileWriter fw = new FileWriter(file);
			
			for(String str : codeList){
				fw.write(str+"\n");
			}
			fw.flush(); //Ȥ�� ������ ���� ���� �ִٸ� �������
			fw.close();
		}
		
	}
	
	/**
	 * object code�� Text record�� ������Ų��
	 * ���� record ���̰� 30�� �Ѿ�� ���� Text record�� ������, ���ο� record�� ����� �ִ´�.
	 * @param objectCode : �߰��� object code
	 * @param byteSize : �߰��� object code�� ����
	 */
	private void addTextRecord(String objectCode, int byteSize, int location){
		

		if(textRecord.length()==0){
			//���� record�� ���۵��� ���� ���, ���� ����
			startLocation = location; //���� �ּҸ� �����Ѵ�
			textRecord += objectCode;
			textRecordLength = byteSize;
			
		}else if( (textRecordLength + byteSize) > MAX_LENGTH){
			//���̰� 30byte�� �Ѿ�� ���, ���� record�� ������ ���� ����
			finTextRecord();
			startLocation = location;
			textRecord += objectCode;
			textRecordLength = byteSize;
			
		}else{
			//���̰� 30byte�� �Ѿ�� �ʴ� ��� ������ ��Ų��
			textRecord += objectCode;
			textRecordLength += byteSize; //length�� ������Ų��
			
		}
		
		
	}
	
	/**
	 * ���� Text record�� ����ϰ� ������.
	 */
	private void finTextRecord(){

		if(textRecord.length()==0){ //record�� ���۵��� ���� ��� ���� �͵� �����Ƿ� �׳� ���ư���
			return; 
		}else{ //���� record�� �ִ� ���
			textRecord = String.format("T%06X%02X",startLocation,textRecordLength) + textRecord;
			//�����ּ�, ����, ���ݱ��� ������ Record���� �� ���� Text Record�� �����.
			codeList.add(new String(textRecord));
			textRecord = new String(""); //���� �����ϱ� ���� ������ش�
			textRecordLength = 0; //textRecord�� Length reset
		}
	}
	
	/**
	 * �ش� section�� Modify Record�� ��� object code list�� �߰��Ѵ�
	 * @param section : �ش� section index
	 */
	private void addModifyRecord(int section){
		
		for(Modify m : TokenList.get(section).modifyList){
			codeList.add(new String(m.objectCode));
		}
		
	}
	
	/**
	 * �ش� section�� Literal�� ��� text record�� �߰��Ѵ�
	 * @param section : �ش� section index
	 */
	private void addLiteralRecord(int section){
		int index=0; //literal List index
		int byteSize;
		String str=new String("");
		
		for(String literal : TokenList.get(section).literalTab.literalList){
			
			if(TokenList.get(section).literalTab.charOrHexList.get(index)==-1){
				//X�� ���
				byteSize=literal.length()/2;
				str = literal;
			}else{
				//C�� ���
				byteSize=literal.length();
				
				for(int i=0 ; i<byteSize ; i++){
					 //�ش� literal ������ ASCII code�� �����Ѵ�
					//Character.getNumericValue(c)�� unicode�� ����ȴ�
					//ASCII code�� �ٲٱ� ���� int�� casting �Ѵ�.
					str += String.format("%X", (int)literal.charAt(i));
				}
			}
			//Literal�� Text Record�� ������ �� ���� �����Ƿ� location�� �ʿ��ϴ�.
			addTextRecord(str,byteSize,TokenList.get(section).literalTab.locationList.get(index));
			index++;
		}
	}
	
}


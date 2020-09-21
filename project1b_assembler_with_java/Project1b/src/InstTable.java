import java.util.HashMap;
import java.util.StringTokenizer;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * 모든 instruction의 정보를 관리하는 클래스. instruction data들을 저장한다
 * 또한 instruction 관련 연산, 예를 들면 목록을 구축하는 함수, 관련 정보를 제공하는 함수 등을 제공 한다.
 */
public class InstTable {
	/** 
	 * inst.data 파일을 불러와 저장하는 공간.
	 *  명령어의 이름을 집어넣으면 해당하는 Instruction의 정보들을 리턴할 수 있다.
	 */
	HashMap<String, Instruction> instMap;
	
	/**
	 * 클래스 초기화. 파싱을 동시에 처리한다.
	 * @param instFile : instuction에 대한 명세가 저장된 파일 이름
	 */
	public InstTable(String instFile) {
		instMap = new HashMap<String, Instruction>();
		
		try {
			openFile(instFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 입력받은 이름의 파일을 열고 해당 내용을 파싱하여 instMap에 저장한다.
	 * 기계어 목록파일 형식은 자유롭게 구현한다. 다음과 같이 inst.data에 저장되어있다.
	 * @param fileName : instuction에 대한 명세가 저장된 파일 이름
	 *	========================================================
	 *		   이름-형식/기계어 코드/오퍼랜드의 갯수/NULL
	 *	=========================================================	
	 * 이름이 key, 나머지는 Instruction으로 생성되어 value로 저장된다.   	
	 */
	public void openFile(String fileName) throws IOException {
		
		File file = new File("./"+fileName);
		
		if(!file.exists())
			throw new IOException("there is no file");
		
		BufferedReader br=new BufferedReader(new FileReader(file));
		String line;
		StringTokenizer str;
		while((line = br.readLine())!=null){
			str = new StringTokenizer(line,"-");
			instMap.put(str.nextToken(), new Instruction(str.nextToken()));
		}
		
		br.close();

	}
	
	/**
	 * 해당 명령어가 몇 byte짜리 명령어인지 byte수를 리턴하는 함수
	 * @param mnemonic : byte를 찾을 명령어
	 * @return 명령어가 소요하는 byte. 명령어가 아닐 경우 -1을 리턴한다.
	 */
	public int searchByte(String mnemonic){
		
		Instruction value;
		
		if(mnemonic.charAt(0)=='+')
			return 4;
		else
			value=instMap.get(mnemonic);
		
		if(value==null)
			return -1;
		
		return value.format;
	}
	
	/**
	 * 해당 명령어가 몇개의 operand를 갖고 있는지 operand 수를 리턴하는 함수
	 * @param mnemonic : operand 개수를 찾을 명령어
	 * @return 명령어가 갖는 operand의 개수, 명령어가 아닐 경우 -1을 리턴한다.
	 */
	public int searchNumberOfOperand(String mnemonic){
		
		Instruction value;
		
		if(mnemonic.charAt(0)=='+')
			value=instMap.get(mnemonic.substring(1));
		else
			value=instMap.get(mnemonic);
		
		if(value==null)
			return -1;
		
		return value.numberOfOperand;
	}
	
	/**
	 * 해당 명령어의 opcode를 찾는 함수
	 * @param mnemonic : opcode를 찾을 명령어
	 * @return 명령어의 opcode, 명령어가 아닐 경우 -1을 리턴한다.
	 */
	public int searchOpcode(String mnemonic){
		
		Instruction value;
		
		if(mnemonic.charAt(0)=='+')
			value=instMap.get(mnemonic.substring(1));
		else
			value=instMap.get(mnemonic);
		
		if(value==null)
			return -1;
		
		return value.opcode;
		
	}

}
/**
 * 명령어 하나하나의 구체적인 정보는 Instruction클래스에 담긴다.
 * instruction과 관련된 정보들을 저장하고 기초적인 연산을 수행한다.
 */
class Instruction {
	/* 
	 * 각자의 inst.data 파일에 맞게 저장하는 변수를 선언한다.
	 *  
	 * ex)
	 * String instruction;
	 * int opcode;
	 * int numberOfOperand;
	 * String comment;
	 */
	
	String mnemonic; //명령어 이름을 저장
	int opcode; //명령어 opcode 숫자를 저장
	int numberOfOperand; //명령어의 operand 개수를 저장
	
	/** instruction이 몇 바이트 명령어인지 저장. 이후 편의성을 위함 */
	int format;
	
	/**
	 * 클래스를 선언하면서 일반문자열을 즉시 구조에 맞게 파싱한다.
	 * @param line : instruction 명세파일로부터 한줄씩 가져온 문자열
	 */
	public Instruction(String line) {
		parsing(line);
	}
	
	/**
	 * 일반 문자열을 파싱하여 instruction 정보를 파악하고 저장한다.
	 * @param line : instruction 명세파일로부터 한줄씩 가져온 문자열
	 */
	public void parsing(String line) {
		
		StringTokenizer str = new StringTokenizer(line,"/");
		
		format = Integer.parseInt(str.nextToken());
		opcode = Integer.parseInt(str.nextToken(),16); // Hex로 받음
		numberOfOperand = Integer.parseInt(str.nextToken());
		
	}
	
	
}

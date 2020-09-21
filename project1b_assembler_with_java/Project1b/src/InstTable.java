import java.util.HashMap;
import java.util.StringTokenizer;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * ��� instruction�� ������ �����ϴ� Ŭ����. instruction data���� �����Ѵ�
 * ���� instruction ���� ����, ���� ��� ����� �����ϴ� �Լ�, ���� ������ �����ϴ� �Լ� ���� ���� �Ѵ�.
 */
public class InstTable {
	/** 
	 * inst.data ������ �ҷ��� �����ϴ� ����.
	 *  ��ɾ��� �̸��� ��������� �ش��ϴ� Instruction�� �������� ������ �� �ִ�.
	 */
	HashMap<String, Instruction> instMap;
	
	/**
	 * Ŭ���� �ʱ�ȭ. �Ľ��� ���ÿ� ó���Ѵ�.
	 * @param instFile : instuction�� ���� ���� ����� ���� �̸�
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
	 * �Է¹��� �̸��� ������ ���� �ش� ������ �Ľ��Ͽ� instMap�� �����Ѵ�.
	 * ���� ������� ������ �����Ӱ� �����Ѵ�. ������ ���� inst.data�� ����Ǿ��ִ�.
	 * @param fileName : instuction�� ���� ���� ����� ���� �̸�
	 *	========================================================
	 *		   �̸�-����/���� �ڵ�/���۷����� ����/NULL
	 *	=========================================================	
	 * �̸��� key, �������� Instruction���� �����Ǿ� value�� ����ȴ�.   	
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
	 * �ش� ��ɾ �� byte¥�� ��ɾ����� byte���� �����ϴ� �Լ�
	 * @param mnemonic : byte�� ã�� ��ɾ�
	 * @return ��ɾ �ҿ��ϴ� byte. ��ɾ �ƴ� ��� -1�� �����Ѵ�.
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
	 * �ش� ��ɾ ��� operand�� ���� �ִ��� operand ���� �����ϴ� �Լ�
	 * @param mnemonic : operand ������ ã�� ��ɾ�
	 * @return ��ɾ ���� operand�� ����, ��ɾ �ƴ� ��� -1�� �����Ѵ�.
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
	 * �ش� ��ɾ��� opcode�� ã�� �Լ�
	 * @param mnemonic : opcode�� ã�� ��ɾ�
	 * @return ��ɾ��� opcode, ��ɾ �ƴ� ��� -1�� �����Ѵ�.
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
 * ��ɾ� �ϳ��ϳ��� ��ü���� ������ InstructionŬ������ ����.
 * instruction�� ���õ� �������� �����ϰ� �������� ������ �����Ѵ�.
 */
class Instruction {
	/* 
	 * ������ inst.data ���Ͽ� �°� �����ϴ� ������ �����Ѵ�.
	 *  
	 * ex)
	 * String instruction;
	 * int opcode;
	 * int numberOfOperand;
	 * String comment;
	 */
	
	String mnemonic; //��ɾ� �̸��� ����
	int opcode; //��ɾ� opcode ���ڸ� ����
	int numberOfOperand; //��ɾ��� operand ������ ����
	
	/** instruction�� �� ����Ʈ ��ɾ����� ����. ���� ���Ǽ��� ���� */
	int format;
	
	/**
	 * Ŭ������ �����ϸ鼭 �Ϲݹ��ڿ��� ��� ������ �°� �Ľ��Ѵ�.
	 * @param line : instruction �����Ϸκ��� ���پ� ������ ���ڿ�
	 */
	public Instruction(String line) {
		parsing(line);
	}
	
	/**
	 * �Ϲ� ���ڿ��� �Ľ��Ͽ� instruction ������ �ľ��ϰ� �����Ѵ�.
	 * @param line : instruction �����Ϸκ��� ���پ� ������ ���ڿ�
	 */
	public void parsing(String line) {
		
		StringTokenizer str = new StringTokenizer(line,"/");
		
		format = Integer.parseInt(str.nextToken());
		opcode = Integer.parseInt(str.nextToken(),16); // Hex�� ����
		numberOfOperand = Integer.parseInt(str.nextToken());
		
	}
	
	
}

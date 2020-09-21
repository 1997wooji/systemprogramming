
package SP20_simulator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.io.FileReader;
import java.io.FileWriter;



/**
 * ResourceManager�� ��ǻ���� ���� ���ҽ����� �����ϰ� �����ϴ� Ŭ�����̴�.
 * ũ�� �װ����� ���� �ڿ� ������ �����ϰ�, �̸� ������ �� �ִ� �Լ����� �����Ѵ�.
 * 
 * 1) ������� ���� �ܺ� ��ġ �Ǵ� device
 * 2) ���α׷� �ε� �� ������ ���� �޸� ����. ���⼭�� 64KB�� �ִ밪���� ��´�.
 * 3) ������ �����ϴµ� ����ϴ� �������� ����.
 * 4) SYMTAB �� simulator�� ���� �������� ���Ǵ� �����͵��� ���� ������. 
 * 
 * 2���� simulator������ ����Ǵ� ���α׷��� ���� �޸𸮰����� �ݸ�,
 * 4���� simulator�� ������ ���� �޸� �����̶�� ������ ���̰� �ִ�.
 */
public class ResourceManager{
	/**
	 * ����̽��� ���� ����� ��ġ���� �ǹ� ������ ���⼭�� ���Ϸ� ����̽��� ��ü�Ѵ�.
	 * ��, 'F1'�̶�� ����̽��� 'F1'�̶�� �̸��� ������ �ǹ��Ѵ�.
	 * deviceManager�� ����̽��� �̸��� �Է¹޾��� �� �ش� �̸��� ���� ����� ���� Ŭ������ �����ϴ� ������ �Ѵ�.
	 * ���� ���, 'A1'�̶�� ����̽����� ������ read���� ������ ���, hashMap�� <"A1", scanner(A1)> ���� �������μ� �̸� ������ �� �ִ�.
	 * ������ ���·� ����ϴ� �� ���� ����Ѵ�.
	 * ���� ��� key������ String��� Integer�� ����� �� �ִ�.
	 * ���� ������� ���� ����ϴ� stream ���� �������� ����, �����Ѵ�.
	 * �̰͵� �����ϸ� �˾Ƽ� �����ؼ� ����ص� �������ϴ�.
	 */
	HashMap<String,Object> deviceManager = new HashMap<String,Object>();
	char[] memory = new char[65536]; // String���� �����ؼ� ����Ͽ��� ������. 64KB
	int[] register = new int[10]; //regsiter 10��
	double register_F;
	int subReturnAddr; //stack ��ſ� ����, sub routine�� �����ؼ� �θ� �� L reg���� �����س���
	
	SymbolTable symtabList; //section�̸� - �ּ�, label-�ּҸ� �����Ѵ�
	ArrayList<Integer> sectionLength; //section�� length�� �����Ѵ�
	String programName; //�� program �̸�
	int programLength; //������ �� ��ģ program �� ����
	int programStartAddress; //program ���� �ּ�
	int addrOfFirstInst; //E record���� ������ ù ���� �ּ�

	boolean setName; //prgram �̸� �����ߴ°�?
	boolean setAddress; //���� �ּ� �����ߴ°�?
	
    String rsusingDeviceName; //���� �����ִ� device�� �̸�
	int rstargetAddr; //���� ��ɾ��� target address�� ����
	int rsstAddrMemory; //���� ��ɾ��� ���� �ּҸ� ����
	
	public ResourceManager(){
		symtabList = new SymbolTable();
		sectionLength = new ArrayList<Integer>();
		programLength = 0;
		setName = false;
		setAddress = false;
	}

	/**
	 * �޸�, �������͵� ���� ���ҽ����� �ʱ�ȭ�Ѵ�.
	 */
	public void initializeResource(){
		//��� ������ �����Ⱚ�� �������� �ʵ��� �ʱ�ȭ
		
		for(int i=0 ; i<65536 ; i++){
			memory[i]=0;
		}
		
		for(int i=0 ; i<10 ; i++){
			register[i]=0;
		}
		
		register[2]=-1; //���� return address�� -1�ֱ�(���� �ñ׳�)
		
	}
	
	/**
	 * deviceManager�� �����ϰ� �ִ� ���� ����� stream���� ���� �����Ű�� ����.
	 * ���α׷��� �����ϰų� ������ ���� �� ȣ���Ѵ�.
	 */
	public void closeDevice() throws IOException {
		Iterator<Object> iter=deviceManager.values().iterator();
		while(iter.hasNext()){
			Object i = iter.next();
			if(i.getClass().getSimpleName().equals("FileReader")){
				((FileReader)i).close(); //read�� write�� ������ �� ���, close�� ���־�� �Ѵ�.
			}else{
				//FileWriter
				((FileWriter)i).flush();
				((FileWriter)i).close();
			}
		}
		
		deviceManager.clear(); //��� device�� �����.
	}
	
	/**
	 * ����̽��� ����� �� �ִ� ��Ȳ���� üũ. TD��ɾ ������� �� ȣ��Ǵ� �Լ�.
	 * ����� stream�� ���� deviceManager�� ���� ������Ų��.
	 * @param devName Ȯ���ϰ��� �ϴ� ����̽��� ��ȣ,�Ǵ� �̸�
	 */
	public void testDevice(String devName) throws IOException {
		
		if(!deviceManager.containsKey(devName)){
			//device�� ���ٸ� ���� ������ �ִ´�
			File file = new File("./"+devName);
			if(!file.exists())
				file.createNewFile();
			
			deviceManager.put(devName, file); //���� file�� �־���´�.
		}		
	}

	/**
	 * ����̽��κ��� ���ϴ� ������ŭ�� ���ڸ� �о���δ�. RD��ɾ ������� �� ȣ��Ǵ� �Լ�.
	 * @param devName ����̽��� �̸�
	 * @param num �������� ������ ����
	 * @return ������ ������
	 */
	public char[] readDevice(String devName, int num) throws IOException{
		
		Object o = deviceManager.get(devName);
		FileReader fr;
		if(o.getClass().getSimpleName().equals("File")){
			//���� read ���� ������ ���� �ʾ��� ��.
			//��� ���� �־���´�.
			fr = new FileReader((File)o);
			deviceManager.replace(devName, fr);
			o = fr;
		}
		fr = (FileReader)o;
		char[] cbuf = new char[num];
		if(fr.read(cbuf)==-1)
			return null;
		return cbuf;
		
	}

	/**
	 * ����̽��� ���ϴ� ���� ��ŭ�� ���ڸ� ����Ѵ�. WD��ɾ ������� �� ȣ��Ǵ� �Լ�.
	 * @param devName ����̽��� �̸�
	 * @param data ������ ������
	 * @param num ������ ������ ����
	 */
	public void writeDevice(String devName, char[] data, int num) throws IOException{
		Object o = deviceManager.get(devName);
		FileWriter fw;
		if(o.getClass().getSimpleName().equals("File")){
			//���� read ���� ������ ���� �ʾ��� ��.
			//��� ���� �־���´�.
			fw = new FileWriter((File)o);
			deviceManager.replace(devName, fw);
			o = fw;
		}
		fw = (FileWriter)o;
		fw.write(data);
	}
	
	/**
	 * �޸��� Ư�� ��ġ���� ���ϴ� ������ŭ�� ���ڸ� �����´�.
	 * @param location �޸� ���� ��ġ �ε���
	 * @param num ������ ����
	 * @return �������� ������
	 */
	public char[] getMemory(int location, int num){
		char[] arr = new char[num];
		for(int i=0;i<num;i++){
			arr[i]=memory[location+i];
		}
		return arr;
		
	}

	/**
	 * �޸��� Ư�� ��ġ�� ���ϴ� ������ŭ�� �����͸� �����Ѵ�. 
	 * @param locate ���� ��ġ �ε���
	 * @param data �����Ϸ��� ������
	 * @param num �����ϴ� �������� ����
	 */
	public void setMemory(int locate, char[] data, int num){
		for(int i=0;i<num;i++){
			memory[locate+i]=data[i];
		}
	}

	/**
	 * ��ȣ�� �ش��ϴ� �������Ͱ� ���� ��� �ִ� ���� �����Ѵ�. �������Ͱ� ��� �ִ� ���� ���ڿ��� �ƴԿ� �����Ѵ�.
	 * @param regNum �������� �з���ȣ
	 * @return �������Ͱ� ������ ��
	 */
	public int getRegister(int regNum){
		return register[regNum];
	}

	/**
	 * ��ȣ�� �ش��ϴ� �������Ϳ� ���ο� ���� �Է��Ѵ�. �������Ͱ� ��� �ִ� ���� ���ڿ��� �ƴԿ� �����Ѵ�.
	 * @param regNum ���������� �з���ȣ
	 * @param value �������Ϳ� ����ִ� ��
	 */
	public void setRegister(int regNum, int value){
		register[regNum]=value;
	}

	/**
	 * �ַ� �������Ϳ� �޸𸮰��� ������ ��ȯ���� ���ȴ�. int���� char[]���·� �����Ѵ�.
	 * @param data
	 * @return
	 */
	public char[] intToChar(int data){
		/**���� ������� 06�ȿ� ���� �����Ƿ� 3byte�ȿ� ������ �����ؾ���**/
		if(data<0){
			data &= (int)Math.pow(2, 24) -1;//FFFFFF(3byte)�� ����
		}
		String str = String.format("%06X",data); //������ 3byte ��ȯ
		char[] buf = new char[3];
		buf[0]=(char)Integer.parseInt(str.substring(0,2),16);
		buf[1]=(char)Integer.parseInt(str.substring(2,4),16);
		buf[2]=(char)Integer.parseInt(str.substring(4,6),16);
		return buf;
	}

	/**
	 * �ַ� �������Ϳ� �޸𸮰��� ������ ��ȯ���� ���ȴ�. char[]���� int���·� �����Ѵ�.
	 * @param data
	 * @return
	 */
	public int byteToInt(char[] data){ //char[]�� �����ߴ�
		String str = "";
		for(char i : data){
			str+=String.format("%02X", (int)i); //�̷��� 02�� ���־�� ���̰� �°� �� �����ȴ�!
			//Integer.toHexString�� �ϸ� 00�� �ȳ����� 0�� ���ͼ� �ٸ� ���� �ǹ���..��
		}
		//���� ���� F�� �����ε� ������ ����ǵ��� ó���������
		//�׳� FFFFFF�� -1�̾ƴ϶� +1677...�� ����
		//�� ��� FF�� ���ٿ��ش�.
		if(data.length>=3 && str.charAt(0)=='F')
			str = "FF"+str;
		return (int)Long.parseLong(str,16);
		//FFFFFFFF�� parseInt�� ���� ������ ����. Long���� ���־�� -1�� ����� ���´�.
	}
}
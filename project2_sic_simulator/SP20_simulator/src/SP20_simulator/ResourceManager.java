
package SP20_simulator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.io.FileReader;
import java.io.FileWriter;



/**
 * ResourceManager는 컴퓨터의 가상 리소스들을 선언하고 관리하는 클래스이다.
 * 크게 네가지의 가상 자원 공간을 선언하고, 이를 관리할 수 있는 함수들을 제공한다.
 * 
 * 1) 입출력을 위한 외부 장치 또는 device
 * 2) 프로그램 로드 및 실행을 위한 메모리 공간. 여기서는 64KB를 최대값으로 잡는다.
 * 3) 연산을 수행하는데 사용하는 레지스터 공간.
 * 4) SYMTAB 등 simulator의 실행 과정에서 사용되는 데이터들을 위한 변수들. 
 * 
 * 2번은 simulator위에서 실행되는 프로그램을 위한 메모리공간인 반면,
 * 4번은 simulator의 실행을 위한 메모리 공간이라는 점에서 차이가 있다.
 */
public class ResourceManager{
	/**
	 * 디바이스는 원래 입출력 장치들을 의미 하지만 여기서는 파일로 디바이스를 대체한다.
	 * 즉, 'F1'이라는 디바이스는 'F1'이라는 이름의 파일을 의미한다.
	 * deviceManager는 디바이스의 이름을 입력받았을 때 해당 이름의 파일 입출력 관리 클래스를 리턴하는 역할을 한다.
	 * 예를 들어, 'A1'이라는 디바이스에서 파일을 read모드로 열었을 경우, hashMap에 <"A1", scanner(A1)> 등을 넣음으로서 이를 관리할 수 있다.
	 * 변형된 형태로 사용하는 것 역시 허용한다.
	 * 예를 들면 key값으로 String대신 Integer를 사용할 수 있다.
	 * 파일 입출력을 위해 사용하는 stream 역시 자유로이 선택, 구현한다.
	 * 이것도 복잡하면 알아서 구현해서 사용해도 괜찮습니다.
	 */
	HashMap<String,Object> deviceManager = new HashMap<String,Object>();
	char[] memory = new char[65536]; // String으로 수정해서 사용하여도 무방함. 64KB
	int[] register = new int[10]; //regsiter 10개
	double register_F;
	int subReturnAddr; //stack 대신에 쓰는, sub routine을 누적해서 부를 때 L reg값을 저장해놓음
	
	SymbolTable symtabList; //section이름 - 주소, label-주소를 저장한다
	ArrayList<Integer> sectionLength; //section별 length를 저장한다
	String programName; //주 program 이름
	int programLength; //섹션을 다 합친 program 총 길이
	int programStartAddress; //program 시작 주소
	int addrOfFirstInst; //E record에서 나오는 첫 시작 주소

	boolean setName; //prgram 이름 세팅했는가?
	boolean setAddress; //시작 주소 세팅했는가?
	
    String rsusingDeviceName; //현재 쓰고있는 device의 이름
	int rstargetAddr; //현재 명령어의 target address를 저장
	int rsstAddrMemory; //현재 명령어의 시작 주소를 저장
	
	public ResourceManager(){
		symtabList = new SymbolTable();
		sectionLength = new ArrayList<Integer>();
		programLength = 0;
		setName = false;
		setAddress = false;
	}

	/**
	 * 메모리, 레지스터등 가상 리소스들을 초기화한다.
	 */
	public void initializeResource(){
		//멤버 변수에 쓰레기값이 남아있지 않도록 초기화
		
		for(int i=0 ; i<65536 ; i++){
			memory[i]=0;
		}
		
		for(int i=0 ; i<10 ; i++){
			register[i]=0;
		}
		
		register[2]=-1; //최종 return address에 -1넣기(종료 시그널)
		
	}
	
	/**
	 * deviceManager가 관리하고 있는 파일 입출력 stream들을 전부 종료시키는 역할.
	 * 프로그램을 종료하거나 연결을 끊을 때 호출한다.
	 */
	public void closeDevice() throws IOException {
		Iterator<Object> iter=deviceManager.values().iterator();
		while(iter.hasNext()){
			Object i = iter.next();
			if(i.getClass().getSimpleName().equals("FileReader")){
				((FileReader)i).close(); //read나 write로 파일을 연 경우, close를 해주어야 한다.
			}else{
				//FileWriter
				((FileWriter)i).flush();
				((FileWriter)i).close();
			}
		}
		
		deviceManager.clear(); //모든 device를 지운다.
	}
	
	/**
	 * 디바이스를 사용할 수 있는 상황인지 체크. TD명령어를 사용했을 때 호출되는 함수.
	 * 입출력 stream을 열고 deviceManager를 통해 관리시킨다.
	 * @param devName 확인하고자 하는 디바이스의 번호,또는 이름
	 */
	public void testDevice(String devName) throws IOException {
		
		if(!deviceManager.containsKey(devName)){
			//device가 없다면 새로 생성해 넣는다
			File file = new File("./"+devName);
			if(!file.exists())
				file.createNewFile();
			
			deviceManager.put(devName, file); //먼저 file을 넣어놓는다.
		}		
	}

	/**
	 * 디바이스로부터 원하는 개수만큼의 글자를 읽어들인다. RD명령어를 사용했을 때 호출되는 함수.
	 * @param devName 디바이스의 이름
	 * @param num 가져오는 글자의 개수
	 * @return 가져온 데이터
	 */
	public char[] readDevice(String devName, int num) throws IOException{
		
		Object o = deviceManager.get(devName);
		FileReader fr;
		if(o.getClass().getSimpleName().equals("File")){
			//아직 read 모드로 파일을 열지 않았을 때.
			//열어서 새로 넣어놓는다.
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
	 * 디바이스로 원하는 개수 만큼의 글자를 출력한다. WD명령어를 사용했을 때 호출되는 함수.
	 * @param devName 디바이스의 이름
	 * @param data 보내는 데이터
	 * @param num 보내는 글자의 개수
	 */
	public void writeDevice(String devName, char[] data, int num) throws IOException{
		Object o = deviceManager.get(devName);
		FileWriter fw;
		if(o.getClass().getSimpleName().equals("File")){
			//아직 read 모드로 파일을 열지 않았을 때.
			//열어서 새로 넣어놓는다.
			fw = new FileWriter((File)o);
			deviceManager.replace(devName, fw);
			o = fw;
		}
		fw = (FileWriter)o;
		fw.write(data);
	}
	
	/**
	 * 메모리의 특정 위치에서 원하는 개수만큼의 글자를 가져온다.
	 * @param location 메모리 접근 위치 인덱스
	 * @param num 데이터 개수
	 * @return 가져오는 데이터
	 */
	public char[] getMemory(int location, int num){
		char[] arr = new char[num];
		for(int i=0;i<num;i++){
			arr[i]=memory[location+i];
		}
		return arr;
		
	}

	/**
	 * 메모리의 특정 위치에 원하는 개수만큼의 데이터를 저장한다. 
	 * @param locate 접근 위치 인덱스
	 * @param data 저장하려는 데이터
	 * @param num 저장하는 데이터의 개수
	 */
	public void setMemory(int locate, char[] data, int num){
		for(int i=0;i<num;i++){
			memory[locate+i]=data[i];
		}
	}

	/**
	 * 번호에 해당하는 레지스터가 현재 들고 있는 값을 리턴한다. 레지스터가 들고 있는 값은 문자열이 아님에 주의한다.
	 * @param regNum 레지스터 분류번호
	 * @return 레지스터가 소지한 값
	 */
	public int getRegister(int regNum){
		return register[regNum];
	}

	/**
	 * 번호에 해당하는 레지스터에 새로운 값을 입력한다. 레지스터가 들고 있는 값은 문자열이 아님에 주의한다.
	 * @param regNum 레지스터의 분류번호
	 * @param value 레지스터에 집어넣는 값
	 */
	public void setRegister(int regNum, int value){
		register[regNum]=value;
	}

	/**
	 * 주로 레지스터와 메모리간의 데이터 교환에서 사용된다. int값을 char[]형태로 변경한다.
	 * @param data
	 * @return
	 */
	public char[] intToChar(int data){
		/**만약 음수라면 06안에 들어가지 않으므로 3byte안에 들어가도록 설정해야함**/
		if(data<0){
			data &= (int)Math.pow(2, 24) -1;//FFFFFF(3byte)만 남게
		}
		String str = String.format("%06X",data); //무조건 3byte 반환
		char[] buf = new char[3];
		buf[0]=(char)Integer.parseInt(str.substring(0,2),16);
		buf[1]=(char)Integer.parseInt(str.substring(2,4),16);
		buf[2]=(char)Integer.parseInt(str.substring(4,6),16);
		return buf;
	}

	/**
	 * 주로 레지스터와 메모리간의 데이터 교환에서 사용된다. char[]값을 int형태로 변경한다.
	 * @param data
	 * @return
	 */
	public int byteToInt(char[] data){ //char[]로 변경했다
		String str = "";
		for(char i : data){
			str+=String.format("%02X", (int)i); //이렇게 02로 해주어야 길이가 맞게 잘 변형된다!
			//Integer.toHexString을 하면 00이 안나오고 0이 나와서 다른 수가 되버림..ㅠ
		}
		//만약 앞이 F면 음수인데 음수러 변경되도록 처리해줘야함
		//그냥 FFFFFF는 -1이아니라 +1677...로 나옴
		//이 경우 FF를 덧붙여준다.
		if(data.length>=3 && str.charAt(0)=='F')
			str = "FF"+str;
		return (int)Long.parseLong(str,16);
		//FFFFFFFF은 parseInt를 쓰면 에러가 난다. Long으로 해주어야 -1이 제대로 나온다.
	}
}
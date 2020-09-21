import java.util.HashMap;

/**
 * Register와 그에 해당하는 번호를 저장하는 Table이다.
 * 하나만 생성된다.
 */
public class RegisterTable {
	
	HashMap<String, Integer> registerMap;
	
	public RegisterTable(){
		
		registerMap = new HashMap<String,Integer>();
		
		//registerMap에 register이름 - 매칭 번호를 차례로 넣는다.
		registerMap.put("A", 0);
		registerMap.put("X", 1);
		registerMap.put("L", 2);
		registerMap.put("B", 3);
		registerMap.put("S", 4);
		registerMap.put("T", 5);
		registerMap.put("F", 6);
		registerMap.put("PC", 8);
		registerMap.put("SW", 9);
		
	}
	
	/**
	 * 인자에 해당하는 이름의 register의 번호를 알려준다.
	 * @param name : 번호를 찾을 register의 이름
	 * @return name이 null이면 0을 리턴한다.
	 * name이 null이 아닌 경우, Map을 찾아 register의 번호를 리턴한다.
	 * 없는 register의 경우 -1을 리턴한다.
	 */
	public int getNumber(String name){
		
		if(name==null)
			return 0;
		
		Integer number = registerMap.get(name);
		
		if(number==null)
			return -1;
		
		return number;
	}

}

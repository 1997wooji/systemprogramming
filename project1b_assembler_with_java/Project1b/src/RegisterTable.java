import java.util.HashMap;

/**
 * Register�� �׿� �ش��ϴ� ��ȣ�� �����ϴ� Table�̴�.
 * �ϳ��� �����ȴ�.
 */
public class RegisterTable {
	
	HashMap<String, Integer> registerMap;
	
	public RegisterTable(){
		
		registerMap = new HashMap<String,Integer>();
		
		//registerMap�� register�̸� - ��Ī ��ȣ�� ���ʷ� �ִ´�.
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
	 * ���ڿ� �ش��ϴ� �̸��� register�� ��ȣ�� �˷��ش�.
	 * @param name : ��ȣ�� ã�� register�� �̸�
	 * @return name�� null�̸� 0�� �����Ѵ�.
	 * name�� null�� �ƴ� ���, Map�� ã�� register�� ��ȣ�� �����Ѵ�.
	 * ���� register�� ��� -1�� �����Ѵ�.
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

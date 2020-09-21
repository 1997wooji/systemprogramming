import java.util.ArrayList;

/**
 * literal�� ���õ� �����Ϳ� ������ �����Ѵ�.
 * section ���� �ϳ��� �ν��Ͻ��� �Ҵ��Ѵ�.
 */
public class LiteralTable {
	ArrayList<String> literalList;
	ArrayList<Integer> locationList;
	ArrayList<Integer> charOrHexList; //�ش� literal�� char���� hex���� �����Ѵ� 
	
	public LiteralTable(){
		literalList=new ArrayList<String>();
		locationList=new ArrayList<Integer>();
		charOrHexList=new ArrayList<Integer>();
	}
	
	/**
	 * ���ο� Literal�� table�� �߰��Ѵ�.
	 * @param literal : ���� �߰��Ǵ� literal�� label
	 * @param location : �ش� literal�� ������ �ּҰ�
	 * *������ ���� X�� ��� location�� -1, C�� ��� location�� -2�� �ִ´�*
	 * charOrHex�� ���������� ������ ���� X�� -1, C�� -2�� �ִ´�
	 * ���� : ���� �ߺ��� literal�� putLiteral�� ���ؼ� �Էµȴٸ� �̴� ���α׷� �ڵ忡 ������ ������ ��Ÿ����. 
	 * ��Ī�Ǵ� �ּҰ��� ������ modifyLiteral()�� ���ؼ� �̷������ �Ѵ�.
	 */
	public void putLiteral(String literal, int location) {
		
		if(literalList.indexOf(literal)>=0) //�̹� �ش� literal�� �߰��Ǿ� �ִٸ� �߰��� �ʿ䰡 ����
			return;
		
		literalList.add(new String(literal));
		locationList.add(new Integer(location));
		charOrHexList.add(new Integer(location));
	}
	
	/**
	 * ������ �����ϴ� literal ���� ���ؼ� ����Ű�� �ּҰ��� �����Ѵ�.
	 * @param literal : ������ ���ϴ� literal�� label
	 * @param newLocation : ���� �ٲٰ��� �ϴ� �ּҰ�
	 */
	public void modifyLiteral(String literal, int newLocation) {
		int index = literalList.indexOf(literal);
		locationList.set(index, new Integer(newLocation));
	}
	
	/**
	 * ���ڷ� ���޵� literal�� � �ּҸ� ��Ī�ϴ��� �˷��ش�. 
	 * @param literal : �˻��� ���ϴ� literal�� label
	 * @return literal�� ������ �ִ� �ּҰ�. �ش� literal�� ���� ��� -1 ����
	 */
	public int search(String literal) {
		int address = 0;
		int index = literalList.indexOf(literal);
		
		if(index<0)
			address = -1;
		else
			address = locationList.get(index);
		
		return address;
	}
	
	/**
	 * �ش� ������ ��� 
	 * @param locctr : literal�� �ּҸ� �߰��ϱ� ���� locctr�� ���ڷ� ����´�
	 * @return ��� literal�� �ּ� �Ҵ��� ���� �� pass1���� locctr��
	 * update ���ֱ� ���� ������ locctr�� return�Ѵ�
	 */
	public int addAddLiteral(int locctr){
		
		int index = 0;
		int byteSize = 0;
		
		if(locationList.get(index)>0){
			return locctr;
			// ���� �̹� �ּҰ� �Ҵ�Ǿ��ٸ� ���ư���
			// LTORG, Ȥ�� END�� ������ �ѹ��� �� section�� ��� literal�� �Ҵ��Ѵ�.
			// ���� �̹� �ϳ��� �ּ� �Ҵ��� �Ǿ��ٸ�, ��� �ּҰ� �Ҵ�� ���̹Ƿ� ���ư���.
		}
		
		/** �Ҵ� �ȵ� literal ��� �Ҵ�*/
		for(String literal : literalList){
			
			if(charOrHexList.get(index)==-1){
				//X�� ���
				byteSize=literal.length()/2;
			}else{
				//C�� ���
				byteSize=literal.length();
			}
			this.modifyLiteral(literal, locctr); //�ش� literal �ּ� ����
			locctr+=byteSize;
			index++;
		}
		
		return locctr; //locctr�� ��ȯ�Ͽ� assember������ locctr��  ������Ʈ�Ѵ�.
		
	}
	
	
}

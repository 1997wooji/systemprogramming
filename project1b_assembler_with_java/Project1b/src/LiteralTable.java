import java.util.ArrayList;

/**
 * literal과 관련된 데이터와 연산을 소유한다.
 * section 별로 하나씩 인스턴스를 할당한다.
 */
public class LiteralTable {
	ArrayList<String> literalList;
	ArrayList<Integer> locationList;
	ArrayList<Integer> charOrHexList; //해당 literal이 char인지 hex인지 저장한다 
	
	public LiteralTable(){
		literalList=new ArrayList<String>();
		locationList=new ArrayList<Integer>();
		charOrHexList=new ArrayList<Integer>();
	}
	
	/**
	 * 새로운 Literal을 table에 추가한다.
	 * @param literal : 새로 추가되는 literal의 label
	 * @param location : 해당 literal이 가지는 주소값
	 * *구분을 위해 X인 경우 location은 -1, C인 경우 location은 -2로 넣는다*
	 * charOrHex도 마찬가지로 구분을 위해 X는 -1, C는 -2로 넣는다
	 * 주의 : 만약 중복된 literal이 putLiteral을 통해서 입력된다면 이는 프로그램 코드에 문제가 있음을 나타낸다. 
	 * 매칭되는 주소값의 변경은 modifyLiteral()을 통해서 이루어져야 한다.
	 */
	public void putLiteral(String literal, int location) {
		
		if(literalList.indexOf(literal)>=0) //이미 해당 literal이 추가되어 있다면 추가할 필요가 없음
			return;
		
		literalList.add(new String(literal));
		locationList.add(new Integer(location));
		charOrHexList.add(new Integer(location));
	}
	
	/**
	 * 기존에 존재하는 literal 값에 대해서 가리키는 주소값을 변경한다.
	 * @param literal : 변경을 원하는 literal의 label
	 * @param newLocation : 새로 바꾸고자 하는 주소값
	 */
	public void modifyLiteral(String literal, int newLocation) {
		int index = literalList.indexOf(literal);
		locationList.set(index, new Integer(newLocation));
	}
	
	/**
	 * 인자로 전달된 literal이 어떤 주소를 지칭하는지 알려준다. 
	 * @param literal : 검색을 원하는 literal의 label
	 * @return literal이 가지고 있는 주소값. 해당 literal이 없을 경우 -1 리턴
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
	 * 해당 섹션의 모든 
	 * @param locctr : literal에 주소를 추가하기 위해 locctr을 인자로 갖고온다
	 * @return 모든 literal에 주소 할당이 끝난 후 pass1에서 locctr을
	 * update 해주기 위해 누적된 locctr을 return한다
	 */
	public int addAddLiteral(int locctr){
		
		int index = 0;
		int byteSize = 0;
		
		if(locationList.get(index)>0){
			return locctr;
			// 만약 이미 주소가 할당되었다면 돌아가기
			// LTORG, 혹은 END를 만나면 한번에 그 section의 모든 literal을 할당한다.
			// 따라서 이미 하나라도 주소 할당이 되었다면, 모두 주소가 할당된 것이므로 돌아간다.
		}
		
		/** 할당 안된 literal 모두 할당*/
		for(String literal : literalList){
			
			if(charOrHexList.get(index)==-1){
				//X인 경우
				byteSize=literal.length()/2;
			}else{
				//C인 경우
				byteSize=literal.length();
			}
			this.modifyLiteral(literal, locctr); //해당 literal 주소 수정
			locctr+=byteSize;
			index++;
		}
		
		return locctr; //locctr을 반환하여 assember에서의 locctr을  업데이트한다.
		
	}
	
	
}

/*
 * 화일명 : my_assembler_20160458.c 
 * 설  명 : 이 프로그램은 SIC/XE 머신을 위한 간단한 Assembler 프로그램의 메인루틴으로,
 * 입력된 파일의 코드 중, 명령어에 해당하는 OPCODE를 찾아 출력한다.
 * 파일 내에서 사용되는 문자열 "00000000"에는 자신의 학번을 기입한다.
 */
#pragma warning( disable:4996 )
#define MAX_TEXT 30 //text record의 최대 byte (60column)
#define MAX_COL 9 //하나의 object code가 가질 수 있는 최대 column 수

/*
 * 프로그램의 헤더를 정의한다.
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <math.h>

// 파일명의 "00000000"은 자신의 학번으로 변경할 것.
#include "my_assembler_20160458.h"

/* ----------------------------------------------------------------------------------
 * 설명 : 사용자로 부터 어셈블리 파일을 받아서 명령어의 OPCODE를 찾아 출력한다.
 * 매계 : 실행 파일, 어셈블리 파일 
 * 반환 : 성공 = 0, 실패 = < 0 
 * 주의 : 현재 어셈블리 프로그램의 리스트 파일을 생성하는 루틴은 만들지 않았다. 
 *		   또한 중간파일을 생성하지 않는다. 
 * ----------------------------------------------------------------------------------
 */
int main(int args, char *arg[])
{
	
	if (init_my_assembler() < 0)
	{
		printf("init_my_assembler: 프로그램 초기화에 실패 했습니다.\n");
		return -1;
	}
	
	if (assem_pass1() < 0)
	{
		printf("assem_pass1: 패스1 과정에서 실패하였습니다.  \n");
		return -1;
	}
	//make_opcode_output("output_20160458.txt");


	make_symtab_output("symtab_20160458.txt");
	make_literaltab_output("literaltab_20160458.txt");
	if (assem_pass2() < 0)
	{
		printf("assem_pass2: 패스2 과정에서 실패하였습니다.  \n");
		return -1;
	}

	make_objectcode_output("output_20160458.txt");
	
	data_free();

	return 0;
}

/* ----------------------------------------------------------------------------------
 * 설명 : 프로그램 초기화를 위한 자료구조 생성 및 파일을 읽는 함수이다. 
 * 매계 : 없음
 * 반환 : 정상종료 = 0 , 에러 발생 = -1
 * 주의 : 각각의 명령어 테이블을 내부에 선언하지 않고 관리를 용이하게 하기 
 *		  위해서 파일 단위로 관리하여 프로그램 초기화를 통해 정보를 읽어 올 수 있도록
 *		  구현하였다. 
 * ----------------------------------------------------------------------------------
 */
int init_my_assembler(void)
{
	int result;

	init_register_number(); //register table도 초기화 시킨다.

	if ((result = init_inst_file("inst.data")) < 0)
		return -1;
	if ((result = init_input_file("input.txt")) < 0)
		return -1;
	return result;
}

/* ----------------------------------------------------------------------------------
 * 설명 : 머신을 위한 기계 코드목록 파일을 읽어 기계어 목록 테이블(inst_table)을 
 *        생성하는 함수이다. 
 * 매계 : 기계어 목록 파일
 * 반환 : 정상종료 = 0 , 에러 < 0 
 * 주의 : 기계어 목록파일 형식은 자유롭게 구현한다. 다음과 같이 inst.data에 저장되어있다.
 *	===============================================================================
 *		   이름/형식/기계어 코드/오퍼랜드의 갯수/NULL
 *	===============================================================================	   	
 * ----------------------------------------------------------------------------------
 */
int init_inst_file(char *inst_file)
{
	FILE *file;
	int errno;
	char line_buf[MAX_COUNT]; // 한줄씩 읽을 때 저장하는 버퍼
	char * split; // 분리한 문장을 가리키는 ptr

	if ((file = fopen(inst_file, "rt")) == NULL) //file open이 되지 않을 경우 error
		errno = -1;
	else {

		while (fgets(line_buf, MAX_COUNT-1, file) > 0 && inst_index < MAX_INST && line_buf != NULL) { 
			//정상적으로 file open이 되었을 경우. 한줄씩 읽어들인다.
			//buffer에 아무것도 없지 않고, index가 MAX를 넘지 않을때까지 읽어들인다.

			inst_table[inst_index] = malloc(sizeof(inst));

			// '/'가 token이므로 해당 문자를 이용하여 문자열을 분리한다.
			// 분리 후, 해당하는 정보에 저장한다.
			split=strtok(line_buf, "/");
			inst_table[inst_index]->mnemonic = malloc(strlen(split) + 1); //'\0'저장을 위해 +1
			strcpy(inst_table[inst_index]->mnemonic, split); //깊은 복사를 해야한다

			split = strtok(NULL, "/");
			inst_table[inst_index]->format = malloc(strlen(split) + 1);
			strcpy(inst_table[inst_index]->format, split);

			split = strtok(NULL, "/");
			sscanf(split, "%X", &(inst_table[inst_index]->opcode)); //opcode(str)를 hex(int)로 고쳐 저장한다

			split = strtok(NULL, "/");
			inst_table[inst_index]->n_operand= malloc(strlen(split) + 1);
			strcpy(inst_table[inst_index]->n_operand, split);

			inst_index++; //하나의 index가 꽉 찼으므로 올려준다.
		}

		fclose(file); //file을 꼭 닫아준다.
		errno = 0;
	}

	return errno;
}

/* ----------------------------------------------------------------------------------
 * 설명 : 어셈블리 할 소스코드를 읽어 소스코드 테이블(input_data)를 생성하는 함수이다. 
 * 매계 : 어셈블리할 소스파일명
 * 반환 : 정상종료 = 0 , 에러 < 0  
 * 주의 : 라인단위로 저장한다.	
 * ----------------------------------------------------------------------------------
 */
int init_input_file(char *input_file)
{

	FILE *file;
	int errno;
	char line_buf[MAX_COUNT]; // 한줄씩 읽은 문장을 저장하는 버퍼

	if ((file = fopen(input_file, "rt")) == NULL)
		errno = -1; // file open error시 error flag 설정
	else {

		while ( fgets(line_buf, MAX_COUNT-1, file) > 0 && line_num < MAX_LINES && line_buf != NULL) {
			// input data에서 한줄씩 읽어들인다.
			
			line_buf[strlen(line_buf) - 1] = '\0'; //개행문자를 제거한다. 추후 token에 개행이 들어가는 것을 막기 위함이다.

			input_data[line_num] = malloc(strlen(line_buf) + 1); 
			strcpy(input_data[line_num], line_buf); //해당 문자열만큼 공간을 잡고 깊은 복사를 한다.
			line_num++;
		}

		fclose(file);
		errno = 0; //정상 종료시 error flag가 setting되지 않는다.
	}

	return errno;
}

/* ----------------------------------------------------------------------------------
 * 설명 : 소스 코드를 읽어와 토큰단위로 분석하고 토큰 테이블을 작성하는 함수이다. 
 *        패스 1로 부터 호출된다. 
 * 매계 : 파싱을 원하는 문자열  
 * 반환 : 정상종료 = 0 , 에러 < 0 
 * 주의 : my_assembler 프로그램에서는 라인단위로 토큰 및 오브젝트 관리를 하고 있다. 
 *		  (+)operand가 3개인 경우를 고려하지 못했다.
 * ----------------------------------------------------------------------------------
 */
int token_parsing(char *str) 
{
	if (str == NULL)
		return -1;

	int i=0, n_operand;
	char * split = strtok(str, "\t"); // input data에서는 tab이 token이다.
	token_table[token_line] = malloc(sizeof(token)); //먼저 token만큼 동적 할당한다.

	init_token(token_line); //해당 token의 멤버들이 쓰레기 값을 갖지 않도록 초기화한다.

	if (str[0] != '\t'){
		token_table[token_line]->label = malloc(strlen(split) + 1);
		strcpy(token_table[token_line]->label, split); // Label이 있는 경우 Label을 저장한다.
		split = strtok(NULL, "\t");
	}
	
	token_table[token_line]->operator_sym = malloc(strlen(split) + 1);
	strcpy(token_table[token_line]->operator_sym, split); //opeartor를 저장한다.

	n_operand = search_n_operand(token_table[token_line]->operator_sym);
	
	//operand의 개수를 검색한다. 아래의 처리에서 이용된다.

	if (split != NULL) {
		if (n_operand > 0) {

			if (n_operand == 1) { //operand가 하나인 경우
				split = strtok(NULL, "\t"); //뒤에 ,가 없을 것이므로 tab앞까지가 operand이다.
				token_table[token_line]->operand[i] = malloc(strlen(split) + 1);
				strcpy(token_table[token_line]->operand[i], split); //깊은 복사를 한다.
			}
			else { //operand가 2개 이상일 때
				split = strtok(NULL, ",");
				// ","으로 문장을 분리한다.
				
				while (split != NULL && i < n_operand) { //operand 개수만큼 돈다.
					token_table[token_line]->operand[i] = malloc(strlen(split) + 1); 
					strcpy(token_table[token_line]->operand[i], split); // operand에 저장한다.
					i++;

					if (i >= n_operand)
						break; //token을 또 분리하기 전에 나간다.
							   //operand2인 상태에서 token으로 분리해버리면 comment로 가기 때문이다.

					split = strtok(NULL, "\t");
					//comment 앞에서 operand를 읽기 위해 tab으로 분리한다.
					//이렇게 하지 않으면 comment가 같이 딸려온다.
					//대신 operand가 3개일 때는 처리하지 못했다.
				}

			}
		}
		else if (n_operand < 0) {
			// operand 개수가 -1인 경우 : 일단 지시자라고 생각을 한다.
			// 지시자도 뒤에 (START, EXTDEF 등) 인수가 있을 수 있다.
			// 없는 경우도 있기 때문에 꼭 NULL인지 체크가 필요하다.

			split = strtok(NULL, "\t");

			if (split != NULL) { //있는 경우, operand에다 넣어준다.
				token_table[token_line]->operand[i] = malloc(strlen(split) + 1);
				strcpy(token_table[token_line]->operand[i], split);
			}
		}
	}

	// comment 저장하는 부분
	// operand가 없는 경우, (=0인 경우) 바로 comment로 넘어가게 된다.
	split = strtok(NULL, "\t");

	if (split != NULL) { 
		token_table[token_line]->comment = malloc(strlen(split)+1);
		strcpy(token_table[token_line]->comment, split); //comment가 있다면, 저장한다.
	}
	
	token_line++; // 마찬가지로 index를 올려준다.

	return 0;
}

/* ----------------------------------------------------------------------------------
 * 설명 : 입력 문자열이 기계어 코드인지를 검사하는 함수이다. 
 * 매계 : 토큰 단위로 구분된 문자열 
 * 반환 : 정상종료 = 기계어 테이블 인덱스, 에러 < 0 	
 * ----------------------------------------------------------------------------------
 */
int search_opcode(char *str)
{

	int i = 0;

	if (str[0] == '+') { // 4형식인 경우
		str++; // str이 + 다음 것을 가리키도록 한다.
	}
	
	while (i < inst_index) {

		if (strcmp(str, inst_table[i]->mnemonic) == 0) {
			return i; // 알맞은 문자가 있을 경우, index를 리턴한다.
		}

		i++;
	}

	return -1;

}

/* ----------------------------------------------------------------------------------
* 설명 : 어셈블리 코드를 위한 패스1과정을 수행하는 함수이다.
*		   1. 프로그램 소스를 스캔하여 해당하는 토큰단위로 분리하여 프로그램 라인별 토큰
*		   테이블을 생성한다. 
*		   2. 프로그램 및 섹션의 크기를 구하고 모든 문장에 address를 할당한다.
*		   3. 지시자가 나오면 처리한다.
*		   4. SYMTAB과 LITTAB을 만든다.
* 매계 : 없음
* 반환 : 정상 종료 = 0 , 에러 = < 0
* 주의 : 현재 초기 버전에서는 에러에 대한 검사를 하지 않고 넘어간 상태이다.
*		 따라서 에러에 대한 검사 루틴을 추가해야 한다.
* -----------------------------------------------------------------------------------
*/
static int assem_pass1(void)
{

	int i = 0;
	int byte = 0;

	while (i < line_num) { //input_data line_num까지

		if (sizeof(input_data[i]) == 0 || input_data[i][0] == '.' ) {
			i++;
			continue; //혹시나 문장이 빈 경우, 주석인 경우 넘어간다.
		}
		
		if (token_parsing(input_data[i]) < 0) { //token_parsing 호출
			return -1; //error인 경우
		}

		token_table[token_line - 1]->nixbpe = 0; //쓰레기값 방지

		if (token_table[token_line - 1]->label != NULL) { //label(symbol)이 있는 경우
			//parsing을 하고 왔기 때문에 token line-1을 해줘야 방금 추가한 token으로 간다

			if (symbol_address(token_table[token_line - 1]->label,cs_index)>=0) {
				//해당 symbol이 있다면 중복된 symbol이 있는 것이므로
				return -1;
			}
			//symbol table 만듦
			strcpy(sym_table[sym_index].symbol, token_table[token_line - 1]->label);
			sym_table[sym_index].addr = locctr; //CSECT인 경우 밑에서 따로 처리를 또 해준다.
			sym_index++;
		}

		if ((byte = search_n_byte(token_table[token_line - 1]->operator_sym)) > 0) {
			//opcode일 경우

			locctr += byte; //locctr 증가

			//nixbpe에서 nix,e는 설정 가능 (bp는 나중에 pass2에서 설정)
			if (token_table[token_line - 1]->operator_sym[0] == '+') {
				//4형식인 경우, e=1 설정(bit or)
				token_table[token_line - 1]->nixbpe |= 1;
			}

			if (token_table[token_line - 1]->operand[0] != NULL && token_table[token_line - 1]->operand[0][0] == '@') {
				// indirect, n=1 설정
				token_table[token_line - 1]->nixbpe |= 32;
			}
			else if(token_table[token_line - 1]->operand[0] != NULL && token_table[token_line - 1]->operand[0][0] == '#') {
				// immidiate, i=1 설정
				token_table[token_line - 1]->nixbpe |= 16;
			}
			else {
				//direct address이거나, operand가 없더라도 SIC/XE이므로 n, i 둘다 1 설정
				//2형식 data도 nixbpe를 갖게 되긴 한다.
				token_table[token_line - 1]->nixbpe |= 48;
			}

			if (token_table[token_line - 1]->operand[0] != NULL) {
				char * str = strtok(token_table[token_line - 1]->operand[0], ",");
				//X가 있는 경우를 위해 한 번 strtok를 한다. 만약 ,이 없다면 operand가 그대로 들어감
				str = strtok(NULL, ","); // 다시 한 번 token 분리. ,이 없다면 NULL이 나온다
				//strtok는 잘리는 문자가 잘린 상태 그대로 있기 때문에
				//이후 opearnd[0]는 ,X없는 operand를 가지게 된다

				if (str != NULL && str[0]=='X') { // x=1로 만들어준다
					token_table[token_line - 1]->nixbpe |= 8;
				}
			}

		}
		else { //directives인 경우

			if (strcmp(token_table[token_line - 1]->operator_sym, "CSECT") == 0) { //CSECT인 경우
				cs_length_table[cs_index] = locctr; //program length를 저장한다. (현재 locctr)
				//여기서는 start address를 따로 저장하지 않는다. 항상 0이라고 가정.
				cs_index++; //control section이 새로 생긴다
				locctr = 0; //locctr 초기화
				sym_table[sym_index-1].addr = locctr; //sym_tab의 label의 address 값을 다시 고친다.
			}
			else if (strcmp(token_table[token_line - 1]->operator_sym, "EQU") == 0) {
				if (token_table[token_line - 1]->operand[0][0] != '*') {
					// * 이면 그냥 현재 locctr을 넣으면 되지만 아닐 경우, 따로 값을 구해 저장해야한다
					//일단 연산을 해야할 경우, -에 대해서만 구분하였다
					// EQU MAXLEN, EQU BUFEND-BUFFER는 가능하지만, EQU 4096이나, +인 경우는 불가능하다.
					char * str = malloc(strlen(token_table[token_line - 1]->operand[0]) + 1);
					strcpy(str, token_table[token_line - 1]->operand[0]);
					//strtok를 쓰면 operand[0] 자체도 잘린 채로 저장되기 때문에
					//나중에 pass2에서도 전체가 쓰이는 것들은 따로 복사해 사용한다.
					char * sub_str= strtok(str, "-"); // -가 있는 경우
					sym_table[sym_index-1].addr = symbol_address(sub_str,cs_index);
					//현재 section에 대해서만 찾는다 변수가 겹치더라도 가능하다
					
					sub_str = strtok(NULL, "-");
					if(str!=NULL) //만약 - 뒤에 변수가 있다면
						sym_table[sym_index - 1].addr -= symbol_address(sub_str, 0); // 마이너스를 계산해 저장한다

					free(str); //동적할당한 str은 다시 메모리 free한다
				}
				
			}
			else if (strcmp(token_table[token_line - 1]->operator_sym, "RESW") == 0) {
				locctr += atoi(token_table[token_line - 1]->operand[0]) * 3; //operand*3만큼 할당
			}
			else if (strcmp(token_table[token_line - 1]->operator_sym, "RESB") == 0) {
				locctr += atoi(token_table[token_line - 1]->operand[0]); //operand만큼 할당
			}
			else if (strcmp(token_table[token_line - 1]->operator_sym, "WORD") == 0) {
				locctr += 3; //word이므로 3만 더한다
			}
			else if (strcmp(token_table[token_line - 1]->operator_sym, "BYTE") == 0) {
				if (token_table[token_line - 1]->operand[0][0] == 'X') { //HEX인 경우
					//X''를 빼고 2column당 1byte이므로 2로 나눔 (BYTE X'F1')
					locctr += (strlen(token_table[token_line - 1]->operand[0])-3)/2; //strlen안에서 -3하면 어캄;
				}
				else if(token_table[token_line - 1]->operand[0][0] == 'C'){ //Char인경우
					//operand length만큼 할당 (C''를 뺌) (BYTE C'EOF')
					locctr += strlen(token_table[token_line - 1]->operand[0])-3;
				}
			}
			else if (strcmp(token_table[token_line - 1]->operator_sym, "LTORG") == 0) {
				//LTORG를 만났으니 저장만 해놨던 literal들에 대해 진짜 주소를 할당한다.
				add_literal_addr();
			}
			else if (strcmp(token_table[token_line - 1]->operator_sym, "END") == 0) {
				add_literal_addr(); //끝내기 전 주소가 할당 안된 literal들에 대해 주소 할당
				cs_length_table[cs_index] = locctr;// program length 저장
				cs_index++;

			}
			//pass1에서 EXTDEF, EXTREF는 넘어간다.

		}

		if (token_table[token_line - 1]->operand[0] != NULL && token_table[token_line - 1]->operand[0][0]=='=') {
			//operand가 literal인 경우

			char * str = malloc(strlen(token_table[token_line - 1]->operand[0])+1);
			strcpy(str, token_table[token_line - 1]->operand[0]); //나중에 또 쓰기위한 깊은 복사
			//이렇게 하지 않으면 operand[0]에 =C나 =X만 남아있게 된다.
			char * sub_str = strtok(str, "'");// '로 구분
			sub_str = strtok(NULL, "'"); //X'F1'에서 F1을 가리킴

			if (search_literal_table(sub_str) < 0) { //literal table에 있는지 검사하고 없다면 추가
				strcpy(literal_table[literal_index].literal, sub_str);
				literal_table[literal_index].format= token_table[token_line - 1]->operand[0][1];
				//literal이 C인지 X인지 형식을 추가한다
				literal_table[literal_index].section = cs_index; //현재 control section index를 넣는다.
				literal_table[literal_index].addr = 0; //주소는 모르므로 0으로 할당한다
				literal_index++;
			}
			
			free(str);

		}

		i++;
		
	}

	return 0;

}

/* ----------------------------------------------------------------------------------
* 설명 : 입력된 문자열의 이름을 가진 파일에 프로그램의 결과를 저장하는 함수이다.
*        여기서 출력되는 내용은 명령어 옆에 OPCODE가 기록된 표(과제 5번) 이다.
* 매계 : 생성할 오브젝트 파일명
* 주의 : 만약 인자로 NULL값이 들어온다면 프로그램의 결과를 표준출력으로 보내어
*        화면에 출력해준다.
*        또한 과제 5번에서만 쓰이는 함수이므로 이후의 프로젝트에서는 사용되지 않는다.
* -----------------------------------------------------------------------------------
*/
void make_opcode_output(char *file_name)
{
	FILE* file;
	int opcode_index;
	int j;
	char line[MAX_COUNT]; //출력 line을 저장하는 버퍼

	if (file_name == NULL) {
		// 인자로 NULL값이 들어올 경우.
		// 콘솔에 결과를 출력한다.

		for (int i = 0; i < token_line; i++) {

			if (token_table[i]->label != NULL) {
				printf("%s" , token_table[i]->label);
			}
			printf("\t%s", token_table[i]->operator_sym);

			j = 0;
			if (token_table[i]->operand[j]) {
				printf("\t");
			}
			while (token_table[i]->operand[j] != NULL && j < MAX_OPERAND) {
				printf("%s",token_table[i]->operand[j]);
				j++;
				if (token_table[i]->operand[j] != NULL) {
					printf(",");
				}
			}

			if((opcode_index = search_opcode(token_table[i]->operator_sym)) >= 0) {
				printf("\t\t\t%X", inst_table[opcode_index]->opcode);
			}

			printf("\n");

		}

		return;
	}

	// filename이 NULL이 아닐경우
	if ((file = fopen(file_name, "w+t")) == NULL)
		return;
	else { // file이 정상적으로 열렸을 경우

		for (int i = 0; i < token_line; i++) {

			line[0] = '\0'; //수월하게 문장들을 붙이기 위해 [0]번에 \0을 넣어놓는다.

			if (token_table[i]->label != NULL) {
				strcat(line, token_table[i]->label); //label이 있는 경우, label을 붙여넣는다.
			}
			strcat(line, "\t");
			strcat(line, token_table[i]->operator_sym); //operator를 붙여 넣는다.
			strcat(line, "\t");

			j = 0;
			while ( token_table[i]->operand[j] != NULL && j < MAX_OPERAND) {

				if (j > 0) {
					strcat(line, ",");
				}

				strcat(line, token_table[i]->operand[j]); //operand들이 NULL이 아닌 경우, 붙여넣는다.
				j++;
			}

			if ( (opcode_index = search_opcode(token_table[i]->operator_sym)) >= 0) {
				// 지시자가 아니라 존재하는 기계어 코드였다면, opcode도 붙여 넣는다.

				strcat(line, "\t\t");
				char buf[3];
				sprintf(buf, "%2X", inst_table[opcode_index]->opcode);
				strcat(line, buf);
			}

			strcat(line, "\n"); //개행을 위한 문자 추가
			fwrite(line, strlen(line), 1, file); 
			// file에 한 줄씩 적는다.
		}

		fclose(file);
	}
	
}


/* ----------------------------------------------------------------------------------
* 설명 : 명령어의 operand 개수를 찾는 데 쓰이는 함수이다.
* 매계 : operator name
* 반환 : operand 개수, 없는 operator name일 경우 -1 반환
* -----------------------------------------------------------------------------------
*/
int search_n_operand(char *str) {

	int index=search_opcode(str);

	if (index < 0)
		return -1;
	else
		return atoi(inst_table[index]->n_operand);
}

/* ----------------------------------------------------------------------------------
* 설명 : 동적 할당한 메모리를 해제하는 작업을 하는 함수이다.
* 매계 : 없음
* 반환 : 없음
* -----------------------------------------------------------------------------------
*/
void data_free() {

	// input_data 해제
	for (int i = 0; i < line_num; i++) {
		free(input_data[i]);
	}

	// token_table 해제
	for (int i = 0; i < token_line; i++) {

		if (token_table[i]->label != NULL)
			free(token_table[i]->label);

		for (int j = 0; j < MAX_OPERAND; j++) {
			if(token_table[i]->operand[j]!=NULL)
				free(token_table[i]->operand[j]);
		}

		if (token_table[i]->comment != NULL)
			free(token_table[i]->comment);

		free(token_table[i]->operator_sym);
		free(token_table[i]);
	}

	//inst_table 해제
	//token_table이 search_opcode를 쓰기 때문에 나중에 해제해주어야 한다.
	for (int i = 0; i < inst_index; i++) {
		free(inst_table[i]->format);
		free(inst_table[i]->mnemonic);
		free(inst_table[i]->n_operand);
		//opcode는 int이므로 따로 해제할 필요 없다.
		free(inst_table[i]);
	}
	// symtab, literal tab, object_code는 동적 할당이 아니므로 해제할 필요 없다.
	
}

/* ----------------------------------------------------------------------------------
* 설명 : 처음 만든 token의 멤버들을 NULL로 초기화해주는 함수
* 매계 : 초기화할 token을 가리키는 token_table의 index
* 반환 : 없음
* -----------------------------------------------------------------------------------
*/
void init_token(int index) { // 쓰레기 값이 들어가는 것을 모두 NULL로 초기화 해준다.

	token_table[index]->label = NULL;
	token_table[index]->operator_sym = NULL;
	token_table[index]->comment = NULL;

	for (int i = 0; i < MAX_OPERAND; i++) {
		token_table[index]->operand[i] = NULL;
	}
}

/* ----------------------------------------------------------------------------------
* 설명 : 입력된 문자열의 이름을 가진 파일에 프로그램의 결과를 저장하는 함수이다.
*        여기서 출력되는 내용은 SYMBOL별 주소값이 저장된 TABLE이다.
* 매계 : 생성할 오브젝트 파일명
* 반환 : 없음
* 주의 : 만약 인자로 NULL값이 들어온다면 프로그램의 결과를 표준출력으로 보내어
*        화면에 출력해준다.
* -----------------------------------------------------------------------------------
*/
void make_symtab_output(char *file_name)
{

	FILE* file;
	char line[MAX_COUNT]; //출력 line을 저장하는 버퍼
	char hexaddr[MAX_COL]; //hex 주소를 저장할 버퍼

	if (file_name == NULL) {
		// 인자로 NULL값이 들어올 경우.
		// 콘솔에 결과를 출력한다.

		printf("************* symtab_output *************\n");

		for (int i = 0; i < sym_index; i++) {

			printf("%s\t%X\n", sym_table[i].symbol, sym_table[i].addr); //16진수로 출력

		}

		return;
	}

	// filename이 NULL이 아닐경우
	if ((file = fopen(file_name, "w+t")) == NULL)
		return;
	else { // file이 정상적으로 열렸을 경우

		for (int i = 0; i < sym_index; i++) {

			line[0] = '\0'; 
			strcat(line, sym_table[i].symbol);
			strcat(line, "\t");
			sprintf(hexaddr, "%X", sym_table[i].addr);
			strcat(line, hexaddr); //address hex로 변환
			strcat(line, "\n"); 
			fwrite(line, strlen(line), 1, file);
			// file에 한 줄씩 적는다.
		}

		fclose(file);
	}

}

/* ----------------------------------------------------------------------------------
* 설명 : 입력된 문자열의 이름을 가진 파일에 프로그램의 결과를 저장하는 함수이다.
*        여기서 출력되는 내용은 LITERAL별 주소값이 저장된 TABLE이다.
* 매계 : 생성할 오브젝트 파일명
* 반환 : 없음
* 주의 : 만약 인자로 NULL값이 들어온다면 프로그램의 결과를 표준출력으로 보내어
*        화면에 출력해준다.
* -----------------------------------------------------------------------------------
*/
void make_literaltab_output(char *file_name)
{

	FILE* file;
	char line[MAX_COUNT]; //출력 line을 저장하는 버퍼
	char hexaddr[MAX_COL]; //hex 주소를 저장할 버퍼

	if (file_name == NULL) {
		// 인자로 NULL값이 들어올 경우.
		// 콘솔에 결과를 출력한다.
		printf("\n************* literaltab_output *************\n");

		for (int i = 0; i < literal_index ; i++) {

			printf("%s\t%X\n", literal_table[i].literal, literal_table[i].addr); //16진수로 출력

		}

		return;
	}

	// filename이 NULL이 아닐경우
	if ((file = fopen(file_name, "w+t")) == NULL)
		return;
	else { // file이 정상적으로 열렸을 경우

		for (int i = 0; i < literal_index; i++) {

			line[0] = '\0';
			strcat(line, literal_table[i].literal);
			strcat(line, "\t");
			sprintf(hexaddr, "%X", literal_table[i].addr);
			strcat(line, hexaddr); //hex로 변환해야함;;
			strcat(line, "\n");
			fwrite(line, strlen(line), 1, file);
			// file에 한 줄씩 적는다.
		}

		fclose(file);
	}

}

/* ----------------------------------------------------------------------------------
* 설명 : 어셈블리 코드를 기계어 코드로 바꾸기 위한 패스2 과정을 수행하는 함수이다.
*		패스 2에서는 프로그램을 기계어로 바꾸는 작업은 라인 단위로 수행된다.
*		다음과 같은 작업이 수행되어 진다.
*		1. 실제로 해당 어셈블리 명령어를 기계어로 바꾸는 작업을 수행한다.
* 매계 : 없음
* 반환 : 정상종료 = 0, 에러발생 = < 0
* -----------------------------------------------------------------------------------
*/
static int assem_pass2(void)
{ 
	int i = 0;
	int byte = 0; //현재 명령어의 byte를 저장하는 변수
	int pc = 0; //pc값을 저장하는 변수
	int text_length = 0; //현재 text record에 얼만큼 썼는지 나타내는 변수
	int current_cs = 0; //현재 current section index 변수
	char buf[MAX_COUNT]; //최종 text record를 저장하는 변수
	char record[100]; //T, length 뒤 object code들만을 위한 buffer
	char object[MAX_COL + 1]; //2~4byte의 object code 하나(opcode+nixbpe+disp)만을 담기 위한 buffer
	int opcode; //opcode를 저장하는 변수
	int disp = 0; //object code에서 disp부분
	int op_index = 0; //해당 opcode가 존재하는 table index를 저장하는 변수
	int cur_literal = 0; //현재 object code에 record한 literal table index
	int cur_modify = 0; //현재 object code에 record한 modify table index
	int start_addr = 0; //프로그램 시작 주소를 저장하는 변수

	while (i < token_line) { //token 단위로 읽어 object code를 만든다.

		//Header 씀(CESCT, START인 경우)
		if (strcmp(token_table[i]->operator_sym, "START") == 0) {
			locctr = 0; //locctr 초기화
			pc = 0; //pc 초기화
			start_addr = atoi(token_table[i]->operand[0]); //start address 저장
			sprintf(buf,"H%-6s%06X%06X",token_table[i]->label,start_addr,cs_length_table[current_cs]);
			//H record를 쓴다. (program name왼쪽정렬, 시작주소와 section length를 적음)
			strcpy(object_code[object_index], buf);
			object_index++;
		}
		else if(strcmp(token_table[i]->operator_sym, "CSECT") == 0){
			//끝나지 않은 record가 있을 수 있으므로 record를 먼저 끝낸다
			fin_text_record(buf, record, &text_length);
			//section을 끝내기 전에 Modify record 쓰기
			while (cur_modify < m_index && modify_table[cur_modify].section == current_cs) {
				sprintf(buf, "M%06X%02X%c%s", modify_table[cur_modify].addr, modify_table[cur_modify].length, modify_table[cur_modify].plus, modify_table[cur_modify].name);
				strcat(object_code[object_index], buf);
				object_index++;
				cur_modify++;
			}

			//section을끝내기 전에 END record를 작성한다
			if (current_cs == 0) { //주 프로그램일 경우, END record에 첫 시작주소를 적어야한다
				sprintf(buf, "E%06X", start_addr);
				strcpy(object_code[object_index], buf);
			}
			else {
				strcpy(object_code[object_index], "E"); //subprogram일 경우, E만 쓰고 끝낸다
			}
			object_index++;

			locctr = 0; //CSECT시작, locctr 초기화
			pc = 0; //pc 초기화
			current_cs++; //새 section이 시작되었으므로 current_section도 하나 증가시킨다.
			sprintf(buf, "H%-6s%06X%06X", token_table[i]->label, 0, cs_length_table[current_cs]);
			//CSECT는 0이 시작주소이므로 0을 시작주소로 적는다
			strcpy(object_code[object_index], buf);
			object_index++;
		}
		else if (strcmp(token_table[i]->operator_sym, "EXTDEF") == 0) {
			//D record 작성
			object_code[object_index][0] = 'D';
			char * str = strtok(token_table[i]->operand[0],","); // , 로 구분한다

			while (str != NULL) {
				sprintf(buf, "%s%06X",str, symbol_address(str,current_cs));
				strcat(object_code[object_index], buf); //밖으로 내보낼 symbol들과 그 주소를 차례로 적는다.
				str = strtok(NULL, ",");
			}
			object_index++;

		}
		else if (strcmp(token_table[i]->operator_sym, "EXTREF") == 0) {
			//R record 작성
			object_code[object_index][0] = 'R';
			char * str = strtok(token_table[i]->operand[0], ","); // , 로 구분한다

			while (str != NULL) {
				sprintf(buf, "%-6s",str);
				strcat(object_code[object_index], buf); //외부 주소는 모르므로 이름만 적는다
				str = strtok(NULL, ",");
			}

			object_index++;

		}
		else if (strcmp(token_table[i]->operator_sym, "END") == 0) {
			//program이 끝이난 경우
			//끝나기 전 적지 않은 literal record들을 적는다
			while (cur_literal < literal_index && literal_table[cur_literal].section == current_cs) {
				// 현재 section에 대해서만
				if (literal_table[cur_literal].format == 'C') {
					//Char인 경우
					char * object_ptr = object;
					int size = strlen(literal_table[cur_literal].literal);
					for (int j = 0; j < size; j++) {
						//길이만큼 문자 하나하나의 ASCII code를 HEX로 적는다.
						sprintf(object_ptr, "%X", literal_table[cur_literal].literal[j]);
						object_ptr += 2;
					}
					byte = strlen(literal_table[cur_literal].literal); //Char byte
				}
				else { //X인 경우
					sprintf(object, "%s", literal_table[cur_literal].literal); //HEX 문자 그대로 저장
					byte = strlen(literal_table[cur_literal].literal) / 2; //HEX byte
				}
				add_text_record(buf, record, object, &text_length, byte);
				cur_literal++;
			}//end of while
			fin_text_record(buf, record, &text_length); //literal record 종료
			
			//Modify record 쓰기
			while (cur_modify < m_index && modify_table[cur_modify].section == current_cs) {
				//index 비교해주지 않으면 계속 참조하게 된다.
				//현재 section에 있는 modify record만 적는다
				sprintf(buf, "M%06X%02X%c%s", modify_table[cur_modify].addr, modify_table[cur_modify].length, modify_table[cur_modify].plus, modify_table[cur_modify].name);
				strcat(object_code[object_index], buf);
				object_index++;
				cur_modify++;
			}

			//그리고 E record를 쓴다.
			strcpy(object_code[object_index], "E");
			object_index++;
		}
		else {
			//Word,Byte,나머지 operator인 경우에는 Text record 씀
			//text record의 경우 text_length를 이용해 길이를 저장해 놓는다

			if ((byte = search_n_byte(token_table[i]->operator_sym)) > 0) {
				// opcode일 경우
				//nixbpe에서 b,p에 대한 값을 설정해주어야 한다
				//이 예제에서 b인 경우가 없어서 b=1이되는 경우는 없다

				if (token_table[i]->nixbpe & 1) {
					//4형식인 경우
					pc += 4; //pc추가
					op_index = search_opcode(token_table[i]->operator_sym);
					opcode = inst_table[op_index]->opcode;
					opcode = opcode << 4; //nixbpe들어갈 자리 만들기 위해 left shift한다
					opcode |= token_table[i]->nixbpe; //bit or로 nixbpe를 추가한다

					//strtok로 ,X가 있는 경우 이미 분리시켰으므로 처리해줄 필요가 없다.				
					disp = symbol_address(token_table[i]->operand[0], current_cs); //operand symbol 찾기

					if (disp < 0) {
						// symtab에 없는 경우
						disp = 0;
						//modify table에 unit을 추가한다
						add_modify_unit(locctr+1,5,'+', token_table[i]->operand[0],current_cs);
						//고칠 곳의 주소는 locctr+1 고칠 길이는 5로 설정한다

					}
					else {
						// symtab에 있는 경우
						opcode |= 2; //nixbpe에서 p=1로 만들어줌(사실 이부분에서 오류 검사를 해야한다)
						disp -= pc; //pc를 빼서 disp를 설정한다
					}

					if (disp < 0) {
						//disp가 음수일 경우! 하위 5column만 필요하므로 나머지는 0으로 만들어줘야 에러가 나지 않는다.
						//5byte 빼고는다 0으로 (bit and)
						disp &= (int)pow(2,20) -1 ;
					}

					sprintf(object,"%03X%05X", opcode,disp);
					add_text_record(buf, record, object, &text_length, 4); //해당 record를 추가한다
					//여기서 locctr은 위의 함수에서 증가한다



				}
				else if (byte==2) {
					//2형식인 경우
					pc += 2;
					op_index = search_opcode(token_table[i]->operator_sym);
					opcode = inst_table[op_index]->opcode;
					//2형식은 nixbpe를 사용하지 않는다.
					sprintf(object, "%02X%X%X", opcode, search_register(token_table[i]->operand[0]), search_register(token_table[i]->operand[1]));
					//register 번호를 찾아와 넣는다
					add_text_record(buf, record, object, &text_length, 2); //record 추가
				}
				else if (search_n_operand(token_table[i]->operator_sym) == 1) {
					//3형식이지만 operand가 하나인 경우
					pc += 3;
					op_index = search_opcode(token_table[i]->operator_sym);
					opcode = inst_table[op_index]->opcode;
					opcode = opcode << 4;
					opcode |= token_table[i]->nixbpe;

					char * str = token_table[i]->operand[0];

					if ((token_table[i]->nixbpe & 48) == 48) {
						//16 & 48 하면 16으로 출력됨. ==로 n,i 둘다 1인지 확인해야 한다	
						//n=1, i=1인 경우

						if (token_table[i]->operand[0][0] == '=') {
							//operand가 literal인 경우
							char * sub_str = strtok(str, "'"); // '로 구분
							sub_str=strtok(NULL, "'"); //X'', C''을 뗀다
							
							int index = search_literal_table(sub_str);
							disp = literal_table[index].addr; //literal의 address를 받아온다
							disp -= pc; //disp를 계산한다
							opcode |= 2; //pc를 빼서 disp를 계산했으므로 p=1로 만들어준다.
						}
						else {
							//평범한 symbol의 경우
							//4형식이 아니므로 operand가 symtab에 있어야 한다
							disp = symbol_address(str, current_cs); //현재 section의 symbol 주소를 찾아온다
							disp -= pc; //disp 계산
							opcode |= 2; //p=1로 만들어준다.
						}
					
					}
					else if (token_table[i]->nixbpe & 16) {
						//i만1, immediate
						// # 분해
						char * sub_str = strtok(str, "#");
						//바로 #뒤의 숫자들이 들어간다.
						disp = atoi(sub_str); //string to int
						// p를 1로 만들어주지 않는다. 진짜 상수이기 때문에.
						// ** #MAXLEN 같은 경우는 사용할 수 없다. **

					}
					else {
						// n만 1, indirect
						// n=0, i=0인 경우도 여기 포함되지만 처리되지는 않음
						// @뒤의 값을 symtab에서 찾아 object 만듦
						char * sub_str = strtok(str, "@");
						disp = symbol_address(sub_str, current_cs);
						disp -= pc;
						opcode |= 2; //상대주소이므로 p=1로 만들어준다
					}

					if (disp < 0) {
						//만약 disp가 음수인 경우, 하위 3column만 들어갈 수 있도록 bit and를 한다
						disp &= (int)pow(2, 12) - 1;
					}
					sprintf(object, "%03X%03X", opcode, disp); //추가할 record 만들기
					add_text_record(buf, record, object, &text_length, 3); //record 추가

				}
				else {
					//3형식이지만 operand가 없는 경우 (1형식인 경우도 여기 들어가지만 처리는 못한다)
					pc += 3;
					op_index = search_opcode(token_table[i]->operator_sym);
					opcode = inst_table[op_index]->opcode;
					opcode = opcode << 4;
					opcode |= token_table[i]->nixbpe; //pc는 여전히 0이다
					sprintf(object, "%03X%03X", opcode, 0); //disp 부분은 0으로 처리해준다
					add_text_record(buf, record, object, &text_length, 3); //record 추가
				}
				
			}
			else { //지시자인 경우
			
				if (strcmp(token_table[i]->operator_sym, "RESW") == 0) {
					//text record가 끝나지 않았다면 끝내야한다
					fin_text_record(buf, record, &text_length);
					locctr += atoi(token_table[i]->operand[0]) * 3; //locctr만 증가시킨다
				}
				else if (strcmp(token_table[i]->operator_sym, "RESB") == 0) {
					//text record가 끝나지 않았다면 끝내한다
					fin_text_record(buf, record, &text_length);
					locctr += atoi(token_table[i]->operand[0]);
				}
				else if (strcmp(token_table[i]->operator_sym, "WORD") == 0) {
					// 상수,symbol하나,"-"가 있는 경우에서만 처리할 수 있다.
					// +가 있다면 처리하지 못한다
					byte = 3;
					pc += byte;
					char * str = strtok(token_table[i]->operand[0], "-");
					disp = atoi(str); //WORD 뒤에 상수가 들어간 경우

					if (disp == 0) {
						disp = symbol_address(str, current_cs);
						//상수가 아닌 경우, symbol이므로 주소를 찾아온다
					}

					if (disp < 0) {
						//symtab에 없는 경우
						//modify unit을 추가
						add_modify_unit(locctr, 6, '+', str, current_cs);
						disp = 0;
						//WORD이므로 고칠 곳의 주소는 locctr그 자체, 고칠 곳의 길이는 6이다.
					}
					
					str = strtok(NULL, "-");
					if (str != NULL) {
						int sub_disp = symbol_address(str, current_cs);

						if (sub_disp < 0) {
							//symtab에 없는 경우, 똑같이 modify unit을 추가한다
							add_modify_unit(locctr, 6, '-', str, current_cs);
							//마이너스 뒤이므로 '-'를 넣어주어야 한다
						}
						else {
							disp -= sub_disp; //빼는 곳의 주소가 나왔을 경우 target address를 만들기 위해 뺀다!
						}
					}
					if (disp < 0) {
						//disp가 음수인경우 3column만 빼고 다 0으로 만든다
						disp &= (int)pow(2, 12) - 1;
					}
					sprintf(object, "%06X", disp);
					add_text_record(buf, record, object, &text_length, 3);
				}
				else if (strcmp(token_table[i]->operator_sym, "BYTE") == 0) {
					//BYTE는 -, +에 대한 처리를 따로 해주지 않았다

					char * str = strtok(token_table[i]->operand[0], "'");
					str = strtok(NULL, "'"); //C'' 또는 X''에서 분리
					
					if (token_table[i]->operand[0][0] == 'C') {

						char * object_ptr = object;
						int size = strlen(str);

						for (int j = 0; j < size; j++) {
							object_ptr += sprintf(object_ptr, "%X", str[j]);
							//한 byte마다 ascii code(hex)로 적음
						}

						byte = size;
					}
					else { //X인 경우
						sprintf(object, "%02s",str); //byte이므로 그대로 적음
						byte = strlen(str) / 2;
					}
					add_text_record(buf, record, object, &text_length, byte);

				}
				else if (strcmp(token_table[i]->operator_sym, "LTORG") == 0) {
					//저장한 literal들을 text record에 적어야한다
					//현재 section에 대한 literal들만 적는다
	
					while(literal_table[cur_literal].section == current_cs){

						if (cur_literal <literal_index && literal_table[cur_literal].format == 'C') {

							char * object_ptr = object;
							int size = strlen(literal_table[cur_literal].literal);
							for (int j = 0; j < size; j++) {
								sprintf(object_ptr, "%2X", literal_table[cur_literal].literal[j]);
								object_ptr += 2; //문자를 하나하나 ASCII code(HEX)로 적는다
							}
							byte = strlen(literal_table[cur_literal].literal);
						}
						else { //X인 경우
							sprintf(object, "%s", literal_table[cur_literal].literal);
							byte = strlen(literal_table[cur_literal].literal) / 2;
						}
						add_text_record(buf, record, object, &text_length, byte);
						cur_literal++;
					}//end of while
					fin_text_record(buf, record, &text_length); // literal record를 object code에 적고 끝낸다
				}

			}
			

		}
		i++;

	}

	return 0;
}

/* ----------------------------------------------------------------------------------
* 설명 : 입력된 문자열의 이름을 가진 파일에 프로그램의 결과를 저장하는 함수이다.
*        여기서 출력되는 내용은 object code (프로젝트 1번) 이다.
* 매계 : 생성할 오브젝트 파일명
* 반환 : 없음
* 주의 : 만약 인자로 NULL값이 들어온다면 프로그램의 결과를 표준출력으로 보내어
*        화면에 출력해준다.
* -----------------------------------------------------------------------------------
*/
void make_objectcode_output(char *file_name)
{
	FILE* file;

	if (file_name == NULL) {
		// 인자로 NULL값이 들어올 경우.
		// 콘솔에 결과를 출력한다.
		printf("\n************* Object Code *************\n");
		for (int i = 0; i < object_index; i++) {
			printf("%s\n", object_code[i]);
		}

		return;
	}

	// filename이 NULL이 아닐경우
	if ((file = fopen(file_name, "w+t")) == NULL)
		return;
	else { // file이 정상적으로 열렸을 경우

		for (int i = 0; i < object_index; i++) {
			fwrite(object_code[i], strlen(object_code[i]), 1, file);
			fwrite("\n", strlen("\n"), 1, file);
			// file에 한 줄씩 적는다.
		}

		fclose(file);
	}
}

/* ----------------------------------------------------------------------------------
* 설명 : 명령어의 형식을 찾아 몇 바이트가 소요되는지 반환하는 함수이다
* 매계 : operator name
* 반환 : 몇 byte를 쓰는지 return, opcode가 아닐 경우 0 반환
* -----------------------------------------------------------------------------------
*/
int search_n_byte(char *str) {

	if (str[0] == '+') { // 4형식인 경우
		return 4; //4byte를 return
	}
	
	//아닌 경우 opcode를 찾아 format(byte수)를 리턴
	int index = search_opcode(str);

	if (index < 0)
		return 0; //directives일 경우 0을 반환
	else
		return atoi(inst_table[index]->format);

}

/* ----------------------------------------------------------------------------------
* 설명 : literal이 LITTAB에 존재하는지 검사해주는 함수
* 매계 : literal string
* 반환 : literal이 존재하면 index를 반환, 없을 경우 -1 반환
* -----------------------------------------------------------------------------------
*/
int search_literal_table(char *str) {

	for (int i = 0; i < literal_index; i++) {

		if (strcmp(literal_table[i].literal, str) == 0) {
			return i;
		}
	}

	return -1; //해당 literal이 존재하지 않는 경우
}

/* ----------------------------------------------------------------------------------
* 설명 : register정보를 담고있는 register_table을 초기화한다.
* 매계 : 없음
* 반환 : 없음
* -----------------------------------------------------------------------------------
*/
void init_register_number() {
	register_table[regi_index].name = "A";
	register_table[regi_index].number = 0;
	regi_index++;
	register_table[regi_index].name = "X";
	register_table[regi_index].number = 1;
	regi_index++;
	register_table[regi_index].name = "L";
	register_table[regi_index].number = 2;
	regi_index++;
	register_table[regi_index].name = "B";
	register_table[regi_index].number = 3;
	regi_index++;
	register_table[regi_index].name = "S";
	register_table[regi_index].number = 4;
	regi_index++;
	register_table[regi_index].name = "T";
	register_table[regi_index].number = 5;
	regi_index++;
	register_table[regi_index].name = "F";
	register_table[regi_index].number = 6;
	regi_index++;
	register_table[regi_index].name = "PC";
	register_table[regi_index].number = 8;
	regi_index++;
	register_table[regi_index].name = "SW";
	register_table[regi_index].number = 9;
	regi_index++;
}

/* ----------------------------------------------------------------------------------
* 설명 : register에 해당하는 숫자를 반환하는 함수
* 매계 : register(X,A.. 등)
* 반환 : 찾으면 register에 해당하는 숫자를 반환한다. 없는 register이면 -1을 반환한다
* 주의 : NULL이면 0을 반환한다
* -----------------------------------------------------------------------------------
*/
int search_register(char* x) {

	if (x == NULL)
		return 0;

	for (int i = 0; i < regi_index; i++) {
		if (strcmp(register_table[i].name,x)==0) {
			return register_table[i].number;
		}
	}

	return -1;
}

/* ----------------------------------------------------------------------------------
* 설명 : literal이 table의 원소들 중 주소 할당이 안된 리터럴에 주소를 할당
		 또한 literal의 길이를 검사해서 locctr을 자동으로 증가시킨다
* 매계 : 없음
* 반환 : 없음
* -----------------------------------------------------------------------------------
*/
void add_literal_addr() {

	for (int i = 0; i < literal_index; i++) {
		if (literal_table[i].addr == 0) {
			//주소 할당이 안된 literal일 경우 주소를 할당한다
			literal_table[i].addr = locctr;

			if (literal_table[i].format == 'C') { //'EOF'->strlen만큼 추가
				locctr += strlen(literal_table[i].literal);
			}
			else { //HEX(X)인 경우
				// 'F1' -> strlen/2만큼 추가
				locctr += strlen(literal_table[i].literal) / 2;
			}
		}
	}

}

/* ----------------------------------------------------------------------------------
* 설명 : symbol 이름을 주면 해당하는 control section에서의 address를 반환하는 함수
*		 control section이 다르면 symbol이 겹쳐도 SYMTAB에 들어가기 때문에
*		 그것을 구분해줄 수 있는 변수가 필요하다.
* 매계 : symbol이름, symbol을 찾을 control section
* 반환 : control section에서의 address, 해당 symbol이 없을 경우 -1 반환
* -----------------------------------------------------------------------------------
*/
int symbol_address(char *str, int cs) {

	int section=-2;
	//addr가 0이 나올때마다 section은 하나씩 증가함.
	//초반에 program 이름(COPY) 말하고, FIRST가 있기 때문에 처음 section을 -2로 잡았다.
	//symbol_unit에 section을 추가하는 것이 나았을 것 같다.

	for (int i = 0; i < sym_index; i++) {
		if (sym_table[i].addr == 0) {
			section++;
		}

		if (strcmp(str, sym_table[i].symbol) == 0 && section==cs) {
			return sym_table[i].addr; //현재 section에서의 symbol을 찾을 경우 주소 반환
		}
	}
	return -1;
}

/* ----------------------------------------------------------------------------------
* 설명 : instruction code를 text record에 추가하는 함수
*		추가하려는 레코드가 MAX_TEXT를 넘을 경우, 이전 텍스트는 object code에 적고
*		새 레코드를 만든다. 만약 넘지 않을 경우, 기존 레코드에 추가하려는 레코드를 덧붙이는
*		작업만 한다. 또한 아직 record 작성을 시작하지 않았을 경우, 새 레코드를 만들어준다.
*		locctr 또한 알아서 더한다.
* 매계 : buf(최종 text record 한 줄이 될 buffer)
*		record(현재 record의 instruction code들이 들어있는 buffer),
*		object(추가할 instruction code가 들어있는 buffer),
*		tot_length_ptr(현재 text record의 길이를 가진 변수의 pointer),
*		byte(추가하려는 instruction code의 byte)
* 반환 : 성공시 1을 반환한다.
* -----------------------------------------------------------------------------------
*/
int add_text_record(char *buf, char *record, char *object, int *tot_length_ptr, int byte) {

	if (*tot_length_ptr==0) {
		//이번에 새로운 text record를 적기 시작하는 경우
		object_code[object_index][0] = 'T';
		sprintf(buf, "%06X", locctr);
		strcat(object_code[object_index], buf); //시작주소(현재 locctr)을 추가한다
		strcpy(record, object); //record에 현재 instruction code를 넣어준다
		//strcat하면 쓰레기값 뒤에 추가되므로 먼저 초기화 해야함!
		*tot_length_ptr = byte; //text_length를 변경해준다
		locctr += byte; //locctr 또한 증가시킨다

		return 1;
	}

	if ((*tot_length_ptr + byte) > MAX_TEXT) {
		//만약 현재 코드를 추가하면 text record 최대 허용 길이를 넘기는 경우
		fin_text_record(buf, record, tot_length_ptr); //지금까지의 text record는 object code에 적는다
		object_code[object_index][0] = 'T'; //새로운 record를 만든다
		sprintf(buf, "%06X", locctr);
		strcat(object_code[object_index], buf);
		*tot_length_ptr = byte; //text_length를 변경해준다
		strcpy(record, object); //record에 추가할 record를 새로 쓴다(record 새로 시작)
		locctr += byte;
	}
	else {
		//추가해도 이상 없는 경우
		strcat(record, object); //현재 record에 추가할 레코드를 추가한다.
		*tot_length_ptr = *tot_length_ptr + byte; //text_length도 추가할 record의 byte만큼 증가한다
		locctr += byte; //locctr도 증가시킨다.
	}
	
	return 1;
}

/* ----------------------------------------------------------------------------------
* 설명 : 지금까지 적은 record를 object_code에 기록하고 끝내는 함수
*		 만약 현재 record가 시작되지 않은 상태라면 그냥 돌아간다.
* 매계 : buf(최종 text record 한 줄이 될 buffer),
*		record(현재 record의 instruction code들이 들어있는 buffer),
*		tot_length_ptr(현재 text record의 길이를 가진 변수의 pointer)
* -----------------------------------------------------------------------------------
*/
void fin_text_record(char *buf, char *record, int *tot_length_ptr) {

	if (*tot_length_ptr == 0) {
		//record가 시작되지 않은 경우 끝낼 것이 없으므로 돌아간다
		return;
	}
	//끝낼 record가 있는 경우
	sprintf(buf, "%02X%s", *tot_length_ptr,record); //buffer에 length와 최종 record들을 추가한다
	strcat(object_code[object_index], buf); //length + records를 object_code에 적는다
	//시작주소는 이미 들어가있었다
	object_index++; //index를 증가시킨다
	*tot_length_ptr = 0; //text length를 reset한다
	record[0] = '\0'; //record도 reset시킨다
}

/* ----------------------------------------------------------------------------------
* 설명 : modify table에 modify unit을 하나 추가하는 함수
* 매계 : addr(수정할 주소), length(수정할 길이), plus(해당 주소를 더하는지 빼는지 기호),
*		 name(더하거나 뺄 symbol의 이름), section(수정하는 record가 어느 section인지)
* 반환 : 없음
* -----------------------------------------------------------------------------------
*/
void add_modify_unit(int addr, int length, char plus, char * name,int section) {
	modify_table[m_index].addr=addr;
	modify_table[m_index].length=length;
	modify_table[m_index].plus=plus;
	strcpy(modify_table[m_index].name,name);
	modify_table[m_index].section = section;
	m_index++;
}

/*
 * ȭ�ϸ� : my_assembler_20160458.c 
 * ��  �� : �� ���α׷��� SIC/XE �ӽ��� ���� ������ Assembler ���α׷��� ���η�ƾ����,
 * �Էµ� ������ �ڵ� ��, ��ɾ �ش��ϴ� OPCODE�� ã�� ����Ѵ�.
 * ���� ������ ���Ǵ� ���ڿ� "00000000"���� �ڽ��� �й��� �����Ѵ�.
 */
#pragma warning( disable:4996 )
#define MAX_TEXT 30 //text record�� �ִ� byte (60column)
#define MAX_COL 9 //�ϳ��� object code�� ���� �� �ִ� �ִ� column ��

/*
 * ���α׷��� ����� �����Ѵ�.
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <math.h>

// ���ϸ��� "00000000"�� �ڽ��� �й����� ������ ��.
#include "my_assembler_20160458.h"

/* ----------------------------------------------------------------------------------
 * ���� : ����ڷ� ���� ����� ������ �޾Ƽ� ��ɾ��� OPCODE�� ã�� ����Ѵ�.
 * �Ű� : ���� ����, ����� ���� 
 * ��ȯ : ���� = 0, ���� = < 0 
 * ���� : ���� ����� ���α׷��� ����Ʈ ������ �����ϴ� ��ƾ�� ������ �ʾҴ�. 
 *		   ���� �߰������� �������� �ʴ´�. 
 * ----------------------------------------------------------------------------------
 */
int main(int args, char *arg[])
{
	
	if (init_my_assembler() < 0)
	{
		printf("init_my_assembler: ���α׷� �ʱ�ȭ�� ���� �߽��ϴ�.\n");
		return -1;
	}
	
	if (assem_pass1() < 0)
	{
		printf("assem_pass1: �н�1 �������� �����Ͽ����ϴ�.  \n");
		return -1;
	}
	//make_opcode_output("output_20160458.txt");


	make_symtab_output("symtab_20160458.txt");
	make_literaltab_output("literaltab_20160458.txt");
	if (assem_pass2() < 0)
	{
		printf("assem_pass2: �н�2 �������� �����Ͽ����ϴ�.  \n");
		return -1;
	}

	make_objectcode_output("output_20160458.txt");
	
	data_free();

	return 0;
}

/* ----------------------------------------------------------------------------------
 * ���� : ���α׷� �ʱ�ȭ�� ���� �ڷᱸ�� ���� �� ������ �д� �Լ��̴�. 
 * �Ű� : ����
 * ��ȯ : �������� = 0 , ���� �߻� = -1
 * ���� : ������ ��ɾ� ���̺��� ���ο� �������� �ʰ� ������ �����ϰ� �ϱ� 
 *		  ���ؼ� ���� ������ �����Ͽ� ���α׷� �ʱ�ȭ�� ���� ������ �о� �� �� �ֵ���
 *		  �����Ͽ���. 
 * ----------------------------------------------------------------------------------
 */
int init_my_assembler(void)
{
	int result;

	init_register_number(); //register table�� �ʱ�ȭ ��Ų��.

	if ((result = init_inst_file("inst.data")) < 0)
		return -1;
	if ((result = init_input_file("input.txt")) < 0)
		return -1;
	return result;
}

/* ----------------------------------------------------------------------------------
 * ���� : �ӽ��� ���� ��� �ڵ��� ������ �о� ���� ��� ���̺�(inst_table)�� 
 *        �����ϴ� �Լ��̴�. 
 * �Ű� : ���� ��� ����
 * ��ȯ : �������� = 0 , ���� < 0 
 * ���� : ���� ������� ������ �����Ӱ� �����Ѵ�. ������ ���� inst.data�� ����Ǿ��ִ�.
 *	===============================================================================
 *		   �̸�/����/���� �ڵ�/���۷����� ����/NULL
 *	===============================================================================	   	
 * ----------------------------------------------------------------------------------
 */
int init_inst_file(char *inst_file)
{
	FILE *file;
	int errno;
	char line_buf[MAX_COUNT]; // ���پ� ���� �� �����ϴ� ����
	char * split; // �и��� ������ ����Ű�� ptr

	if ((file = fopen(inst_file, "rt")) == NULL) //file open�� ���� ���� ��� error
		errno = -1;
	else {

		while (fgets(line_buf, MAX_COUNT-1, file) > 0 && inst_index < MAX_INST && line_buf != NULL) { 
			//���������� file open�� �Ǿ��� ���. ���پ� �о���δ�.
			//buffer�� �ƹ��͵� ���� �ʰ�, index�� MAX�� ���� ���������� �о���δ�.

			inst_table[inst_index] = malloc(sizeof(inst));

			// '/'�� token�̹Ƿ� �ش� ���ڸ� �̿��Ͽ� ���ڿ��� �и��Ѵ�.
			// �и� ��, �ش��ϴ� ������ �����Ѵ�.
			split=strtok(line_buf, "/");
			inst_table[inst_index]->mnemonic = malloc(strlen(split) + 1); //'\0'������ ���� +1
			strcpy(inst_table[inst_index]->mnemonic, split); //���� ���縦 �ؾ��Ѵ�

			split = strtok(NULL, "/");
			inst_table[inst_index]->format = malloc(strlen(split) + 1);
			strcpy(inst_table[inst_index]->format, split);

			split = strtok(NULL, "/");
			sscanf(split, "%X", &(inst_table[inst_index]->opcode)); //opcode(str)�� hex(int)�� ���� �����Ѵ�

			split = strtok(NULL, "/");
			inst_table[inst_index]->n_operand= malloc(strlen(split) + 1);
			strcpy(inst_table[inst_index]->n_operand, split);

			inst_index++; //�ϳ��� index�� �� á���Ƿ� �÷��ش�.
		}

		fclose(file); //file�� �� �ݾ��ش�.
		errno = 0;
	}

	return errno;
}

/* ----------------------------------------------------------------------------------
 * ���� : ����� �� �ҽ��ڵ带 �о� �ҽ��ڵ� ���̺�(input_data)�� �����ϴ� �Լ��̴�. 
 * �Ű� : ������� �ҽ����ϸ�
 * ��ȯ : �������� = 0 , ���� < 0  
 * ���� : ���δ����� �����Ѵ�.	
 * ----------------------------------------------------------------------------------
 */
int init_input_file(char *input_file)
{

	FILE *file;
	int errno;
	char line_buf[MAX_COUNT]; // ���پ� ���� ������ �����ϴ� ����

	if ((file = fopen(input_file, "rt")) == NULL)
		errno = -1; // file open error�� error flag ����
	else {

		while ( fgets(line_buf, MAX_COUNT-1, file) > 0 && line_num < MAX_LINES && line_buf != NULL) {
			// input data���� ���پ� �о���δ�.
			
			line_buf[strlen(line_buf) - 1] = '\0'; //���๮�ڸ� �����Ѵ�. ���� token�� ������ ���� ���� ���� �����̴�.

			input_data[line_num] = malloc(strlen(line_buf) + 1); 
			strcpy(input_data[line_num], line_buf); //�ش� ���ڿ���ŭ ������ ��� ���� ���縦 �Ѵ�.
			line_num++;
		}

		fclose(file);
		errno = 0; //���� ����� error flag�� setting���� �ʴ´�.
	}

	return errno;
}

/* ----------------------------------------------------------------------------------
 * ���� : �ҽ� �ڵ带 �о�� ��ū������ �м��ϰ� ��ū ���̺��� �ۼ��ϴ� �Լ��̴�. 
 *        �н� 1�� ���� ȣ��ȴ�. 
 * �Ű� : �Ľ��� ���ϴ� ���ڿ�  
 * ��ȯ : �������� = 0 , ���� < 0 
 * ���� : my_assembler ���α׷������� ���δ����� ��ū �� ������Ʈ ������ �ϰ� �ִ�. 
 *		  (+)operand�� 3���� ��츦 ������� ���ߴ�.
 * ----------------------------------------------------------------------------------
 */
int token_parsing(char *str) 
{
	if (str == NULL)
		return -1;

	int i=0, n_operand;
	char * split = strtok(str, "\t"); // input data������ tab�� token�̴�.
	token_table[token_line] = malloc(sizeof(token)); //���� token��ŭ ���� �Ҵ��Ѵ�.

	init_token(token_line); //�ش� token�� ������� ������ ���� ���� �ʵ��� �ʱ�ȭ�Ѵ�.

	if (str[0] != '\t'){
		token_table[token_line]->label = malloc(strlen(split) + 1);
		strcpy(token_table[token_line]->label, split); // Label�� �ִ� ��� Label�� �����Ѵ�.
		split = strtok(NULL, "\t");
	}
	
	token_table[token_line]->operator_sym = malloc(strlen(split) + 1);
	strcpy(token_table[token_line]->operator_sym, split); //opeartor�� �����Ѵ�.

	n_operand = search_n_operand(token_table[token_line]->operator_sym);
	
	//operand�� ������ �˻��Ѵ�. �Ʒ��� ó������ �̿�ȴ�.

	if (split != NULL) {
		if (n_operand > 0) {

			if (n_operand == 1) { //operand�� �ϳ��� ���
				split = strtok(NULL, "\t"); //�ڿ� ,�� ���� ���̹Ƿ� tab�ձ����� operand�̴�.
				token_table[token_line]->operand[i] = malloc(strlen(split) + 1);
				strcpy(token_table[token_line]->operand[i], split); //���� ���縦 �Ѵ�.
			}
			else { //operand�� 2�� �̻��� ��
				split = strtok(NULL, ",");
				// ","���� ������ �и��Ѵ�.
				
				while (split != NULL && i < n_operand) { //operand ������ŭ ����.
					token_table[token_line]->operand[i] = malloc(strlen(split) + 1); 
					strcpy(token_table[token_line]->operand[i], split); // operand�� �����Ѵ�.
					i++;

					if (i >= n_operand)
						break; //token�� �� �и��ϱ� ���� ������.
							   //operand2�� ���¿��� token���� �и��ع����� comment�� ���� �����̴�.

					split = strtok(NULL, "\t");
					//comment �տ��� operand�� �б� ���� tab���� �и��Ѵ�.
					//�̷��� ���� ������ comment�� ���� �����´�.
					//��� operand�� 3���� ���� ó������ ���ߴ�.
				}

			}
		}
		else if (n_operand < 0) {
			// operand ������ -1�� ��� : �ϴ� �����ڶ�� ������ �Ѵ�.
			// �����ڵ� �ڿ� (START, EXTDEF ��) �μ��� ���� �� �ִ�.
			// ���� ��쵵 �ֱ� ������ �� NULL���� üũ�� �ʿ��ϴ�.

			split = strtok(NULL, "\t");

			if (split != NULL) { //�ִ� ���, operand���� �־��ش�.
				token_table[token_line]->operand[i] = malloc(strlen(split) + 1);
				strcpy(token_table[token_line]->operand[i], split);
			}
		}
	}

	// comment �����ϴ� �κ�
	// operand�� ���� ���, (=0�� ���) �ٷ� comment�� �Ѿ�� �ȴ�.
	split = strtok(NULL, "\t");

	if (split != NULL) { 
		token_table[token_line]->comment = malloc(strlen(split)+1);
		strcpy(token_table[token_line]->comment, split); //comment�� �ִٸ�, �����Ѵ�.
	}
	
	token_line++; // ���������� index�� �÷��ش�.

	return 0;
}

/* ----------------------------------------------------------------------------------
 * ���� : �Է� ���ڿ��� ���� �ڵ������� �˻��ϴ� �Լ��̴�. 
 * �Ű� : ��ū ������ ���е� ���ڿ� 
 * ��ȯ : �������� = ���� ���̺� �ε���, ���� < 0 	
 * ----------------------------------------------------------------------------------
 */
int search_opcode(char *str)
{

	int i = 0;

	if (str[0] == '+') { // 4������ ���
		str++; // str�� + ���� ���� ����Ű���� �Ѵ�.
	}
	
	while (i < inst_index) {

		if (strcmp(str, inst_table[i]->mnemonic) == 0) {
			return i; // �˸��� ���ڰ� ���� ���, index�� �����Ѵ�.
		}

		i++;
	}

	return -1;

}

/* ----------------------------------------------------------------------------------
* ���� : ����� �ڵ带 ���� �н�1������ �����ϴ� �Լ��̴�.
*		   1. ���α׷� �ҽ��� ��ĵ�Ͽ� �ش��ϴ� ��ū������ �и��Ͽ� ���α׷� ���κ� ��ū
*		   ���̺��� �����Ѵ�. 
*		   2. ���α׷� �� ������ ũ�⸦ ���ϰ� ��� ���忡 address�� �Ҵ��Ѵ�.
*		   3. �����ڰ� ������ ó���Ѵ�.
*		   4. SYMTAB�� LITTAB�� �����.
* �Ű� : ����
* ��ȯ : ���� ���� = 0 , ���� = < 0
* ���� : ���� �ʱ� ���������� ������ ���� �˻縦 ���� �ʰ� �Ѿ �����̴�.
*		 ���� ������ ���� �˻� ��ƾ�� �߰��ؾ� �Ѵ�.
* -----------------------------------------------------------------------------------
*/
static int assem_pass1(void)
{

	int i = 0;
	int byte = 0;

	while (i < line_num) { //input_data line_num����

		if (sizeof(input_data[i]) == 0 || input_data[i][0] == '.' ) {
			i++;
			continue; //Ȥ�ó� ������ �� ���, �ּ��� ��� �Ѿ��.
		}
		
		if (token_parsing(input_data[i]) < 0) { //token_parsing ȣ��
			return -1; //error�� ���
		}

		token_table[token_line - 1]->nixbpe = 0; //�����Ⱚ ����

		if (token_table[token_line - 1]->label != NULL) { //label(symbol)�� �ִ� ���
			//parsing�� �ϰ� �Ա� ������ token line-1�� ����� ��� �߰��� token���� ����

			if (symbol_address(token_table[token_line - 1]->label,cs_index)>=0) {
				//�ش� symbol�� �ִٸ� �ߺ��� symbol�� �ִ� ���̹Ƿ�
				return -1;
			}
			//symbol table ����
			strcpy(sym_table[sym_index].symbol, token_table[token_line - 1]->label);
			sym_table[sym_index].addr = locctr; //CSECT�� ��� �ؿ��� ���� ó���� �� ���ش�.
			sym_index++;
		}

		if ((byte = search_n_byte(token_table[token_line - 1]->operator_sym)) > 0) {
			//opcode�� ���

			locctr += byte; //locctr ����

			//nixbpe���� nix,e�� ���� ���� (bp�� ���߿� pass2���� ����)
			if (token_table[token_line - 1]->operator_sym[0] == '+') {
				//4������ ���, e=1 ����(bit or)
				token_table[token_line - 1]->nixbpe |= 1;
			}

			if (token_table[token_line - 1]->operand[0] != NULL && token_table[token_line - 1]->operand[0][0] == '@') {
				// indirect, n=1 ����
				token_table[token_line - 1]->nixbpe |= 32;
			}
			else if(token_table[token_line - 1]->operand[0] != NULL && token_table[token_line - 1]->operand[0][0] == '#') {
				// immidiate, i=1 ����
				token_table[token_line - 1]->nixbpe |= 16;
			}
			else {
				//direct address�̰ų�, operand�� ������ SIC/XE�̹Ƿ� n, i �Ѵ� 1 ����
				//2���� data�� nixbpe�� ���� �Ǳ� �Ѵ�.
				token_table[token_line - 1]->nixbpe |= 48;
			}

			if (token_table[token_line - 1]->operand[0] != NULL) {
				char * str = strtok(token_table[token_line - 1]->operand[0], ",");
				//X�� �ִ� ��츦 ���� �� �� strtok�� �Ѵ�. ���� ,�� ���ٸ� operand�� �״�� ��
				str = strtok(NULL, ","); // �ٽ� �� �� token �и�. ,�� ���ٸ� NULL�� ���´�
				//strtok�� �߸��� ���ڰ� �߸� ���� �״�� �ֱ� ������
				//���� opearnd[0]�� ,X���� operand�� ������ �ȴ�

				if (str != NULL && str[0]=='X') { // x=1�� ������ش�
					token_table[token_line - 1]->nixbpe |= 8;
				}
			}

		}
		else { //directives�� ���

			if (strcmp(token_table[token_line - 1]->operator_sym, "CSECT") == 0) { //CSECT�� ���
				cs_length_table[cs_index] = locctr; //program length�� �����Ѵ�. (���� locctr)
				//���⼭�� start address�� ���� �������� �ʴ´�. �׻� 0�̶�� ����.
				cs_index++; //control section�� ���� �����
				locctr = 0; //locctr �ʱ�ȭ
				sym_table[sym_index-1].addr = locctr; //sym_tab�� label�� address ���� �ٽ� ��ģ��.
			}
			else if (strcmp(token_table[token_line - 1]->operator_sym, "EQU") == 0) {
				if (token_table[token_line - 1]->operand[0][0] != '*') {
					// * �̸� �׳� ���� locctr�� ������ ������ �ƴ� ���, ���� ���� ���� �����ؾ��Ѵ�
					//�ϴ� ������ �ؾ��� ���, -�� ���ؼ��� �����Ͽ���
					// EQU MAXLEN, EQU BUFEND-BUFFER�� ����������, EQU 4096�̳�, +�� ���� �Ұ����ϴ�.
					char * str = malloc(strlen(token_table[token_line - 1]->operand[0]) + 1);
					strcpy(str, token_table[token_line - 1]->operand[0]);
					//strtok�� ���� operand[0] ��ü�� �߸� ä�� ����Ǳ� ������
					//���߿� pass2������ ��ü�� ���̴� �͵��� ���� ������ ����Ѵ�.
					char * sub_str= strtok(str, "-"); // -�� �ִ� ���
					sym_table[sym_index-1].addr = symbol_address(sub_str,cs_index);
					//���� section�� ���ؼ��� ã�´� ������ ��ġ���� �����ϴ�
					
					sub_str = strtok(NULL, "-");
					if(str!=NULL) //���� - �ڿ� ������ �ִٸ�
						sym_table[sym_index - 1].addr -= symbol_address(sub_str, 0); // ���̳ʽ��� ����� �����Ѵ�

					free(str); //�����Ҵ��� str�� �ٽ� �޸� free�Ѵ�
				}
				
			}
			else if (strcmp(token_table[token_line - 1]->operator_sym, "RESW") == 0) {
				locctr += atoi(token_table[token_line - 1]->operand[0]) * 3; //operand*3��ŭ �Ҵ�
			}
			else if (strcmp(token_table[token_line - 1]->operator_sym, "RESB") == 0) {
				locctr += atoi(token_table[token_line - 1]->operand[0]); //operand��ŭ �Ҵ�
			}
			else if (strcmp(token_table[token_line - 1]->operator_sym, "WORD") == 0) {
				locctr += 3; //word�̹Ƿ� 3�� ���Ѵ�
			}
			else if (strcmp(token_table[token_line - 1]->operator_sym, "BYTE") == 0) {
				if (token_table[token_line - 1]->operand[0][0] == 'X') { //HEX�� ���
					//X''�� ���� 2column�� 1byte�̹Ƿ� 2�� ���� (BYTE X'F1')
					locctr += (strlen(token_table[token_line - 1]->operand[0])-3)/2; //strlen�ȿ��� -3�ϸ� ��į;
				}
				else if(token_table[token_line - 1]->operand[0][0] == 'C'){ //Char�ΰ��
					//operand length��ŭ �Ҵ� (C''�� ��) (BYTE C'EOF')
					locctr += strlen(token_table[token_line - 1]->operand[0])-3;
				}
			}
			else if (strcmp(token_table[token_line - 1]->operator_sym, "LTORG") == 0) {
				//LTORG�� �������� ���常 �س��� literal�鿡 ���� ��¥ �ּҸ� �Ҵ��Ѵ�.
				add_literal_addr();
			}
			else if (strcmp(token_table[token_line - 1]->operator_sym, "END") == 0) {
				add_literal_addr(); //������ �� �ּҰ� �Ҵ� �ȵ� literal�鿡 ���� �ּ� �Ҵ�
				cs_length_table[cs_index] = locctr;// program length ����
				cs_index++;

			}
			//pass1���� EXTDEF, EXTREF�� �Ѿ��.

		}

		if (token_table[token_line - 1]->operand[0] != NULL && token_table[token_line - 1]->operand[0][0]=='=') {
			//operand�� literal�� ���

			char * str = malloc(strlen(token_table[token_line - 1]->operand[0])+1);
			strcpy(str, token_table[token_line - 1]->operand[0]); //���߿� �� �������� ���� ����
			//�̷��� ���� ������ operand[0]�� =C�� =X�� �����ְ� �ȴ�.
			char * sub_str = strtok(str, "'");// '�� ����
			sub_str = strtok(NULL, "'"); //X'F1'���� F1�� ����Ŵ

			if (search_literal_table(sub_str) < 0) { //literal table�� �ִ��� �˻��ϰ� ���ٸ� �߰�
				strcpy(literal_table[literal_index].literal, sub_str);
				literal_table[literal_index].format= token_table[token_line - 1]->operand[0][1];
				//literal�� C���� X���� ������ �߰��Ѵ�
				literal_table[literal_index].section = cs_index; //���� control section index�� �ִ´�.
				literal_table[literal_index].addr = 0; //�ּҴ� �𸣹Ƿ� 0���� �Ҵ��Ѵ�
				literal_index++;
			}
			
			free(str);

		}

		i++;
		
	}

	return 0;

}

/* ----------------------------------------------------------------------------------
* ���� : �Էµ� ���ڿ��� �̸��� ���� ���Ͽ� ���α׷��� ����� �����ϴ� �Լ��̴�.
*        ���⼭ ��µǴ� ������ ��ɾ� ���� OPCODE�� ��ϵ� ǥ(���� 5��) �̴�.
* �Ű� : ������ ������Ʈ ���ϸ�
* ���� : ���� ���ڷ� NULL���� ���´ٸ� ���α׷��� ����� ǥ��������� ������
*        ȭ�鿡 ������ش�.
*        ���� ���� 5�������� ���̴� �Լ��̹Ƿ� ������ ������Ʈ������ ������ �ʴ´�.
* -----------------------------------------------------------------------------------
*/
void make_opcode_output(char *file_name)
{
	FILE* file;
	int opcode_index;
	int j;
	char line[MAX_COUNT]; //��� line�� �����ϴ� ����

	if (file_name == NULL) {
		// ���ڷ� NULL���� ���� ���.
		// �ֿܼ� ����� ����Ѵ�.

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

	// filename�� NULL�� �ƴҰ��
	if ((file = fopen(file_name, "w+t")) == NULL)
		return;
	else { // file�� ���������� ������ ���

		for (int i = 0; i < token_line; i++) {

			line[0] = '\0'; //�����ϰ� ������� ���̱� ���� [0]���� \0�� �־���´�.

			if (token_table[i]->label != NULL) {
				strcat(line, token_table[i]->label); //label�� �ִ� ���, label�� �ٿ��ִ´�.
			}
			strcat(line, "\t");
			strcat(line, token_table[i]->operator_sym); //operator�� �ٿ� �ִ´�.
			strcat(line, "\t");

			j = 0;
			while ( token_table[i]->operand[j] != NULL && j < MAX_OPERAND) {

				if (j > 0) {
					strcat(line, ",");
				}

				strcat(line, token_table[i]->operand[j]); //operand���� NULL�� �ƴ� ���, �ٿ��ִ´�.
				j++;
			}

			if ( (opcode_index = search_opcode(token_table[i]->operator_sym)) >= 0) {
				// �����ڰ� �ƴ϶� �����ϴ� ���� �ڵ忴�ٸ�, opcode�� �ٿ� �ִ´�.

				strcat(line, "\t\t");
				char buf[3];
				sprintf(buf, "%2X", inst_table[opcode_index]->opcode);
				strcat(line, buf);
			}

			strcat(line, "\n"); //������ ���� ���� �߰�
			fwrite(line, strlen(line), 1, file); 
			// file�� �� �پ� ���´�.
		}

		fclose(file);
	}
	
}


/* ----------------------------------------------------------------------------------
* ���� : ��ɾ��� operand ������ ã�� �� ���̴� �Լ��̴�.
* �Ű� : operator name
* ��ȯ : operand ����, ���� operator name�� ��� -1 ��ȯ
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
* ���� : ���� �Ҵ��� �޸𸮸� �����ϴ� �۾��� �ϴ� �Լ��̴�.
* �Ű� : ����
* ��ȯ : ����
* -----------------------------------------------------------------------------------
*/
void data_free() {

	// input_data ����
	for (int i = 0; i < line_num; i++) {
		free(input_data[i]);
	}

	// token_table ����
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

	//inst_table ����
	//token_table�� search_opcode�� ���� ������ ���߿� �������־�� �Ѵ�.
	for (int i = 0; i < inst_index; i++) {
		free(inst_table[i]->format);
		free(inst_table[i]->mnemonic);
		free(inst_table[i]->n_operand);
		//opcode�� int�̹Ƿ� ���� ������ �ʿ� ����.
		free(inst_table[i]);
	}
	// symtab, literal tab, object_code�� ���� �Ҵ��� �ƴϹǷ� ������ �ʿ� ����.
	
}

/* ----------------------------------------------------------------------------------
* ���� : ó�� ���� token�� ������� NULL�� �ʱ�ȭ���ִ� �Լ�
* �Ű� : �ʱ�ȭ�� token�� ����Ű�� token_table�� index
* ��ȯ : ����
* -----------------------------------------------------------------------------------
*/
void init_token(int index) { // ������ ���� ���� ���� ��� NULL�� �ʱ�ȭ ���ش�.

	token_table[index]->label = NULL;
	token_table[index]->operator_sym = NULL;
	token_table[index]->comment = NULL;

	for (int i = 0; i < MAX_OPERAND; i++) {
		token_table[index]->operand[i] = NULL;
	}
}

/* ----------------------------------------------------------------------------------
* ���� : �Էµ� ���ڿ��� �̸��� ���� ���Ͽ� ���α׷��� ����� �����ϴ� �Լ��̴�.
*        ���⼭ ��µǴ� ������ SYMBOL�� �ּҰ��� ����� TABLE�̴�.
* �Ű� : ������ ������Ʈ ���ϸ�
* ��ȯ : ����
* ���� : ���� ���ڷ� NULL���� ���´ٸ� ���α׷��� ����� ǥ��������� ������
*        ȭ�鿡 ������ش�.
* -----------------------------------------------------------------------------------
*/
void make_symtab_output(char *file_name)
{

	FILE* file;
	char line[MAX_COUNT]; //��� line�� �����ϴ� ����
	char hexaddr[MAX_COL]; //hex �ּҸ� ������ ����

	if (file_name == NULL) {
		// ���ڷ� NULL���� ���� ���.
		// �ֿܼ� ����� ����Ѵ�.

		printf("************* symtab_output *************\n");

		for (int i = 0; i < sym_index; i++) {

			printf("%s\t%X\n", sym_table[i].symbol, sym_table[i].addr); //16������ ���

		}

		return;
	}

	// filename�� NULL�� �ƴҰ��
	if ((file = fopen(file_name, "w+t")) == NULL)
		return;
	else { // file�� ���������� ������ ���

		for (int i = 0; i < sym_index; i++) {

			line[0] = '\0'; 
			strcat(line, sym_table[i].symbol);
			strcat(line, "\t");
			sprintf(hexaddr, "%X", sym_table[i].addr);
			strcat(line, hexaddr); //address hex�� ��ȯ
			strcat(line, "\n"); 
			fwrite(line, strlen(line), 1, file);
			// file�� �� �پ� ���´�.
		}

		fclose(file);
	}

}

/* ----------------------------------------------------------------------------------
* ���� : �Էµ� ���ڿ��� �̸��� ���� ���Ͽ� ���α׷��� ����� �����ϴ� �Լ��̴�.
*        ���⼭ ��µǴ� ������ LITERAL�� �ּҰ��� ����� TABLE�̴�.
* �Ű� : ������ ������Ʈ ���ϸ�
* ��ȯ : ����
* ���� : ���� ���ڷ� NULL���� ���´ٸ� ���α׷��� ����� ǥ��������� ������
*        ȭ�鿡 ������ش�.
* -----------------------------------------------------------------------------------
*/
void make_literaltab_output(char *file_name)
{

	FILE* file;
	char line[MAX_COUNT]; //��� line�� �����ϴ� ����
	char hexaddr[MAX_COL]; //hex �ּҸ� ������ ����

	if (file_name == NULL) {
		// ���ڷ� NULL���� ���� ���.
		// �ֿܼ� ����� ����Ѵ�.
		printf("\n************* literaltab_output *************\n");

		for (int i = 0; i < literal_index ; i++) {

			printf("%s\t%X\n", literal_table[i].literal, literal_table[i].addr); //16������ ���

		}

		return;
	}

	// filename�� NULL�� �ƴҰ��
	if ((file = fopen(file_name, "w+t")) == NULL)
		return;
	else { // file�� ���������� ������ ���

		for (int i = 0; i < literal_index; i++) {

			line[0] = '\0';
			strcat(line, literal_table[i].literal);
			strcat(line, "\t");
			sprintf(hexaddr, "%X", literal_table[i].addr);
			strcat(line, hexaddr); //hex�� ��ȯ�ؾ���;;
			strcat(line, "\n");
			fwrite(line, strlen(line), 1, file);
			// file�� �� �پ� ���´�.
		}

		fclose(file);
	}

}

/* ----------------------------------------------------------------------------------
* ���� : ����� �ڵ带 ���� �ڵ�� �ٲٱ� ���� �н�2 ������ �����ϴ� �Լ��̴�.
*		�н� 2������ ���α׷��� ����� �ٲٴ� �۾��� ���� ������ ����ȴ�.
*		������ ���� �۾��� ����Ǿ� ����.
*		1. ������ �ش� ����� ��ɾ ����� �ٲٴ� �۾��� �����Ѵ�.
* �Ű� : ����
* ��ȯ : �������� = 0, �����߻� = < 0
* -----------------------------------------------------------------------------------
*/
static int assem_pass2(void)
{ 
	int i = 0;
	int byte = 0; //���� ��ɾ��� byte�� �����ϴ� ����
	int pc = 0; //pc���� �����ϴ� ����
	int text_length = 0; //���� text record�� ��ŭ ����� ��Ÿ���� ����
	int current_cs = 0; //���� current section index ����
	char buf[MAX_COUNT]; //���� text record�� �����ϴ� ����
	char record[100]; //T, length �� object code�鸸�� ���� buffer
	char object[MAX_COL + 1]; //2~4byte�� object code �ϳ�(opcode+nixbpe+disp)���� ��� ���� buffer
	int opcode; //opcode�� �����ϴ� ����
	int disp = 0; //object code���� disp�κ�
	int op_index = 0; //�ش� opcode�� �����ϴ� table index�� �����ϴ� ����
	int cur_literal = 0; //���� object code�� record�� literal table index
	int cur_modify = 0; //���� object code�� record�� modify table index
	int start_addr = 0; //���α׷� ���� �ּҸ� �����ϴ� ����

	while (i < token_line) { //token ������ �о� object code�� �����.

		//Header ��(CESCT, START�� ���)
		if (strcmp(token_table[i]->operator_sym, "START") == 0) {
			locctr = 0; //locctr �ʱ�ȭ
			pc = 0; //pc �ʱ�ȭ
			start_addr = atoi(token_table[i]->operand[0]); //start address ����
			sprintf(buf,"H%-6s%06X%06X",token_table[i]->label,start_addr,cs_length_table[current_cs]);
			//H record�� ����. (program name��������, �����ּҿ� section length�� ����)
			strcpy(object_code[object_index], buf);
			object_index++;
		}
		else if(strcmp(token_table[i]->operator_sym, "CSECT") == 0){
			//������ ���� record�� ���� �� �����Ƿ� record�� ���� ������
			fin_text_record(buf, record, &text_length);
			//section�� ������ ���� Modify record ����
			while (cur_modify < m_index && modify_table[cur_modify].section == current_cs) {
				sprintf(buf, "M%06X%02X%c%s", modify_table[cur_modify].addr, modify_table[cur_modify].length, modify_table[cur_modify].plus, modify_table[cur_modify].name);
				strcat(object_code[object_index], buf);
				object_index++;
				cur_modify++;
			}

			//section�������� ���� END record�� �ۼ��Ѵ�
			if (current_cs == 0) { //�� ���α׷��� ���, END record�� ù �����ּҸ� ������Ѵ�
				sprintf(buf, "E%06X", start_addr);
				strcpy(object_code[object_index], buf);
			}
			else {
				strcpy(object_code[object_index], "E"); //subprogram�� ���, E�� ���� ������
			}
			object_index++;

			locctr = 0; //CSECT����, locctr �ʱ�ȭ
			pc = 0; //pc �ʱ�ȭ
			current_cs++; //�� section�� ���۵Ǿ����Ƿ� current_section�� �ϳ� ������Ų��.
			sprintf(buf, "H%-6s%06X%06X", token_table[i]->label, 0, cs_length_table[current_cs]);
			//CSECT�� 0�� �����ּ��̹Ƿ� 0�� �����ּҷ� ���´�
			strcpy(object_code[object_index], buf);
			object_index++;
		}
		else if (strcmp(token_table[i]->operator_sym, "EXTDEF") == 0) {
			//D record �ۼ�
			object_code[object_index][0] = 'D';
			char * str = strtok(token_table[i]->operand[0],","); // , �� �����Ѵ�

			while (str != NULL) {
				sprintf(buf, "%s%06X",str, symbol_address(str,current_cs));
				strcat(object_code[object_index], buf); //������ ������ symbol��� �� �ּҸ� ���ʷ� ���´�.
				str = strtok(NULL, ",");
			}
			object_index++;

		}
		else if (strcmp(token_table[i]->operator_sym, "EXTREF") == 0) {
			//R record �ۼ�
			object_code[object_index][0] = 'R';
			char * str = strtok(token_table[i]->operand[0], ","); // , �� �����Ѵ�

			while (str != NULL) {
				sprintf(buf, "%-6s",str);
				strcat(object_code[object_index], buf); //�ܺ� �ּҴ� �𸣹Ƿ� �̸��� ���´�
				str = strtok(NULL, ",");
			}

			object_index++;

		}
		else if (strcmp(token_table[i]->operator_sym, "END") == 0) {
			//program�� ���̳� ���
			//������ �� ���� ���� literal record���� ���´�
			while (cur_literal < literal_index && literal_table[cur_literal].section == current_cs) {
				// ���� section�� ���ؼ���
				if (literal_table[cur_literal].format == 'C') {
					//Char�� ���
					char * object_ptr = object;
					int size = strlen(literal_table[cur_literal].literal);
					for (int j = 0; j < size; j++) {
						//���̸�ŭ ���� �ϳ��ϳ��� ASCII code�� HEX�� ���´�.
						sprintf(object_ptr, "%X", literal_table[cur_literal].literal[j]);
						object_ptr += 2;
					}
					byte = strlen(literal_table[cur_literal].literal); //Char byte
				}
				else { //X�� ���
					sprintf(object, "%s", literal_table[cur_literal].literal); //HEX ���� �״�� ����
					byte = strlen(literal_table[cur_literal].literal) / 2; //HEX byte
				}
				add_text_record(buf, record, object, &text_length, byte);
				cur_literal++;
			}//end of while
			fin_text_record(buf, record, &text_length); //literal record ����
			
			//Modify record ����
			while (cur_modify < m_index && modify_table[cur_modify].section == current_cs) {
				//index �������� ������ ��� �����ϰ� �ȴ�.
				//���� section�� �ִ� modify record�� ���´�
				sprintf(buf, "M%06X%02X%c%s", modify_table[cur_modify].addr, modify_table[cur_modify].length, modify_table[cur_modify].plus, modify_table[cur_modify].name);
				strcat(object_code[object_index], buf);
				object_index++;
				cur_modify++;
			}

			//�׸��� E record�� ����.
			strcpy(object_code[object_index], "E");
			object_index++;
		}
		else {
			//Word,Byte,������ operator�� ��쿡�� Text record ��
			//text record�� ��� text_length�� �̿��� ���̸� ������ ���´�

			if ((byte = search_n_byte(token_table[i]->operator_sym)) > 0) {
				// opcode�� ���
				//nixbpe���� b,p�� ���� ���� �������־�� �Ѵ�
				//�� �������� b�� ��찡 ��� b=1�̵Ǵ� ���� ����

				if (token_table[i]->nixbpe & 1) {
					//4������ ���
					pc += 4; //pc�߰�
					op_index = search_opcode(token_table[i]->operator_sym);
					opcode = inst_table[op_index]->opcode;
					opcode = opcode << 4; //nixbpe�� �ڸ� ����� ���� left shift�Ѵ�
					opcode |= token_table[i]->nixbpe; //bit or�� nixbpe�� �߰��Ѵ�

					//strtok�� ,X�� �ִ� ��� �̹� �и��������Ƿ� ó������ �ʿ䰡 ����.				
					disp = symbol_address(token_table[i]->operand[0], current_cs); //operand symbol ã��

					if (disp < 0) {
						// symtab�� ���� ���
						disp = 0;
						//modify table�� unit�� �߰��Ѵ�
						add_modify_unit(locctr+1,5,'+', token_table[i]->operand[0],current_cs);
						//��ĥ ���� �ּҴ� locctr+1 ��ĥ ���̴� 5�� �����Ѵ�

					}
					else {
						// symtab�� �ִ� ���
						opcode |= 2; //nixbpe���� p=1�� �������(��� �̺κп��� ���� �˻縦 �ؾ��Ѵ�)
						disp -= pc; //pc�� ���� disp�� �����Ѵ�
					}

					if (disp < 0) {
						//disp�� ������ ���! ���� 5column�� �ʿ��ϹǷ� �������� 0���� �������� ������ ���� �ʴ´�.
						//5byte ����´� 0���� (bit and)
						disp &= (int)pow(2,20) -1 ;
					}

					sprintf(object,"%03X%05X", opcode,disp);
					add_text_record(buf, record, object, &text_length, 4); //�ش� record�� �߰��Ѵ�
					//���⼭ locctr�� ���� �Լ����� �����Ѵ�



				}
				else if (byte==2) {
					//2������ ���
					pc += 2;
					op_index = search_opcode(token_table[i]->operator_sym);
					opcode = inst_table[op_index]->opcode;
					//2������ nixbpe�� ������� �ʴ´�.
					sprintf(object, "%02X%X%X", opcode, search_register(token_table[i]->operand[0]), search_register(token_table[i]->operand[1]));
					//register ��ȣ�� ã�ƿ� �ִ´�
					add_text_record(buf, record, object, &text_length, 2); //record �߰�
				}
				else if (search_n_operand(token_table[i]->operator_sym) == 1) {
					//3���������� operand�� �ϳ��� ���
					pc += 3;
					op_index = search_opcode(token_table[i]->operator_sym);
					opcode = inst_table[op_index]->opcode;
					opcode = opcode << 4;
					opcode |= token_table[i]->nixbpe;

					char * str = token_table[i]->operand[0];

					if ((token_table[i]->nixbpe & 48) == 48) {
						//16 & 48 �ϸ� 16���� ��µ�. ==�� n,i �Ѵ� 1���� Ȯ���ؾ� �Ѵ�	
						//n=1, i=1�� ���

						if (token_table[i]->operand[0][0] == '=') {
							//operand�� literal�� ���
							char * sub_str = strtok(str, "'"); // '�� ����
							sub_str=strtok(NULL, "'"); //X'', C''�� ����
							
							int index = search_literal_table(sub_str);
							disp = literal_table[index].addr; //literal�� address�� �޾ƿ´�
							disp -= pc; //disp�� ����Ѵ�
							opcode |= 2; //pc�� ���� disp�� ��������Ƿ� p=1�� ������ش�.
						}
						else {
							//����� symbol�� ���
							//4������ �ƴϹǷ� operand�� symtab�� �־�� �Ѵ�
							disp = symbol_address(str, current_cs); //���� section�� symbol �ּҸ� ã�ƿ´�
							disp -= pc; //disp ���
							opcode |= 2; //p=1�� ������ش�.
						}
					
					}
					else if (token_table[i]->nixbpe & 16) {
						//i��1, immediate
						// # ����
						char * sub_str = strtok(str, "#");
						//�ٷ� #���� ���ڵ��� ����.
						disp = atoi(sub_str); //string to int
						// p�� 1�� ��������� �ʴ´�. ��¥ ����̱� ������.
						// ** #MAXLEN ���� ���� ����� �� ����. **

					}
					else {
						// n�� 1, indirect
						// n=0, i=0�� ��쵵 ���� ���Ե����� ó�������� ����
						// @���� ���� symtab���� ã�� object ����
						char * sub_str = strtok(str, "@");
						disp = symbol_address(sub_str, current_cs);
						disp -= pc;
						opcode |= 2; //����ּ��̹Ƿ� p=1�� ������ش�
					}

					if (disp < 0) {
						//���� disp�� ������ ���, ���� 3column�� �� �� �ֵ��� bit and�� �Ѵ�
						disp &= (int)pow(2, 12) - 1;
					}
					sprintf(object, "%03X%03X", opcode, disp); //�߰��� record �����
					add_text_record(buf, record, object, &text_length, 3); //record �߰�

				}
				else {
					//3���������� operand�� ���� ��� (1������ ��쵵 ���� ������ ó���� ���Ѵ�)
					pc += 3;
					op_index = search_opcode(token_table[i]->operator_sym);
					opcode = inst_table[op_index]->opcode;
					opcode = opcode << 4;
					opcode |= token_table[i]->nixbpe; //pc�� ������ 0�̴�
					sprintf(object, "%03X%03X", opcode, 0); //disp �κ��� 0���� ó�����ش�
					add_text_record(buf, record, object, &text_length, 3); //record �߰�
				}
				
			}
			else { //�������� ���
			
				if (strcmp(token_table[i]->operator_sym, "RESW") == 0) {
					//text record�� ������ �ʾҴٸ� �������Ѵ�
					fin_text_record(buf, record, &text_length);
					locctr += atoi(token_table[i]->operand[0]) * 3; //locctr�� ������Ų��
				}
				else if (strcmp(token_table[i]->operator_sym, "RESB") == 0) {
					//text record�� ������ �ʾҴٸ� �����Ѵ�
					fin_text_record(buf, record, &text_length);
					locctr += atoi(token_table[i]->operand[0]);
				}
				else if (strcmp(token_table[i]->operator_sym, "WORD") == 0) {
					// ���,symbol�ϳ�,"-"�� �ִ� ��쿡���� ó���� �� �ִ�.
					// +�� �ִٸ� ó������ ���Ѵ�
					byte = 3;
					pc += byte;
					char * str = strtok(token_table[i]->operand[0], "-");
					disp = atoi(str); //WORD �ڿ� ����� �� ���

					if (disp == 0) {
						disp = symbol_address(str, current_cs);
						//����� �ƴ� ���, symbol�̹Ƿ� �ּҸ� ã�ƿ´�
					}

					if (disp < 0) {
						//symtab�� ���� ���
						//modify unit�� �߰�
						add_modify_unit(locctr, 6, '+', str, current_cs);
						disp = 0;
						//WORD�̹Ƿ� ��ĥ ���� �ּҴ� locctr�� ��ü, ��ĥ ���� ���̴� 6�̴�.
					}
					
					str = strtok(NULL, "-");
					if (str != NULL) {
						int sub_disp = symbol_address(str, current_cs);

						if (sub_disp < 0) {
							//symtab�� ���� ���, �Ȱ��� modify unit�� �߰��Ѵ�
							add_modify_unit(locctr, 6, '-', str, current_cs);
							//���̳ʽ� ���̹Ƿ� '-'�� �־��־�� �Ѵ�
						}
						else {
							disp -= sub_disp; //���� ���� �ּҰ� ������ ��� target address�� ����� ���� ����!
						}
					}
					if (disp < 0) {
						//disp�� �����ΰ�� 3column�� ���� �� 0���� �����
						disp &= (int)pow(2, 12) - 1;
					}
					sprintf(object, "%06X", disp);
					add_text_record(buf, record, object, &text_length, 3);
				}
				else if (strcmp(token_table[i]->operator_sym, "BYTE") == 0) {
					//BYTE�� -, +�� ���� ó���� ���� ������ �ʾҴ�

					char * str = strtok(token_table[i]->operand[0], "'");
					str = strtok(NULL, "'"); //C'' �Ǵ� X''���� �и�
					
					if (token_table[i]->operand[0][0] == 'C') {

						char * object_ptr = object;
						int size = strlen(str);

						for (int j = 0; j < size; j++) {
							object_ptr += sprintf(object_ptr, "%X", str[j]);
							//�� byte���� ascii code(hex)�� ����
						}

						byte = size;
					}
					else { //X�� ���
						sprintf(object, "%02s",str); //byte�̹Ƿ� �״�� ����
						byte = strlen(str) / 2;
					}
					add_text_record(buf, record, object, &text_length, byte);

				}
				else if (strcmp(token_table[i]->operator_sym, "LTORG") == 0) {
					//������ literal���� text record�� ������Ѵ�
					//���� section�� ���� literal�鸸 ���´�
	
					while(literal_table[cur_literal].section == current_cs){

						if (cur_literal <literal_index && literal_table[cur_literal].format == 'C') {

							char * object_ptr = object;
							int size = strlen(literal_table[cur_literal].literal);
							for (int j = 0; j < size; j++) {
								sprintf(object_ptr, "%2X", literal_table[cur_literal].literal[j]);
								object_ptr += 2; //���ڸ� �ϳ��ϳ� ASCII code(HEX)�� ���´�
							}
							byte = strlen(literal_table[cur_literal].literal);
						}
						else { //X�� ���
							sprintf(object, "%s", literal_table[cur_literal].literal);
							byte = strlen(literal_table[cur_literal].literal) / 2;
						}
						add_text_record(buf, record, object, &text_length, byte);
						cur_literal++;
					}//end of while
					fin_text_record(buf, record, &text_length); // literal record�� object code�� ���� ������
				}

			}
			

		}
		i++;

	}

	return 0;
}

/* ----------------------------------------------------------------------------------
* ���� : �Էµ� ���ڿ��� �̸��� ���� ���Ͽ� ���α׷��� ����� �����ϴ� �Լ��̴�.
*        ���⼭ ��µǴ� ������ object code (������Ʈ 1��) �̴�.
* �Ű� : ������ ������Ʈ ���ϸ�
* ��ȯ : ����
* ���� : ���� ���ڷ� NULL���� ���´ٸ� ���α׷��� ����� ǥ��������� ������
*        ȭ�鿡 ������ش�.
* -----------------------------------------------------------------------------------
*/
void make_objectcode_output(char *file_name)
{
	FILE* file;

	if (file_name == NULL) {
		// ���ڷ� NULL���� ���� ���.
		// �ֿܼ� ����� ����Ѵ�.
		printf("\n************* Object Code *************\n");
		for (int i = 0; i < object_index; i++) {
			printf("%s\n", object_code[i]);
		}

		return;
	}

	// filename�� NULL�� �ƴҰ��
	if ((file = fopen(file_name, "w+t")) == NULL)
		return;
	else { // file�� ���������� ������ ���

		for (int i = 0; i < object_index; i++) {
			fwrite(object_code[i], strlen(object_code[i]), 1, file);
			fwrite("\n", strlen("\n"), 1, file);
			// file�� �� �پ� ���´�.
		}

		fclose(file);
	}
}

/* ----------------------------------------------------------------------------------
* ���� : ��ɾ��� ������ ã�� �� ����Ʈ�� �ҿ�Ǵ��� ��ȯ�ϴ� �Լ��̴�
* �Ű� : operator name
* ��ȯ : �� byte�� ������ return, opcode�� �ƴ� ��� 0 ��ȯ
* -----------------------------------------------------------------------------------
*/
int search_n_byte(char *str) {

	if (str[0] == '+') { // 4������ ���
		return 4; //4byte�� return
	}
	
	//�ƴ� ��� opcode�� ã�� format(byte��)�� ����
	int index = search_opcode(str);

	if (index < 0)
		return 0; //directives�� ��� 0�� ��ȯ
	else
		return atoi(inst_table[index]->format);

}

/* ----------------------------------------------------------------------------------
* ���� : literal�� LITTAB�� �����ϴ��� �˻����ִ� �Լ�
* �Ű� : literal string
* ��ȯ : literal�� �����ϸ� index�� ��ȯ, ���� ��� -1 ��ȯ
* -----------------------------------------------------------------------------------
*/
int search_literal_table(char *str) {

	for (int i = 0; i < literal_index; i++) {

		if (strcmp(literal_table[i].literal, str) == 0) {
			return i;
		}
	}

	return -1; //�ش� literal�� �������� �ʴ� ���
}

/* ----------------------------------------------------------------------------------
* ���� : register������ ����ִ� register_table�� �ʱ�ȭ�Ѵ�.
* �Ű� : ����
* ��ȯ : ����
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
* ���� : register�� �ش��ϴ� ���ڸ� ��ȯ�ϴ� �Լ�
* �Ű� : register(X,A.. ��)
* ��ȯ : ã���� register�� �ش��ϴ� ���ڸ� ��ȯ�Ѵ�. ���� register�̸� -1�� ��ȯ�Ѵ�
* ���� : NULL�̸� 0�� ��ȯ�Ѵ�
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
* ���� : literal�� table�� ���ҵ� �� �ּ� �Ҵ��� �ȵ� ���ͷ��� �ּҸ� �Ҵ�
		 ���� literal�� ���̸� �˻��ؼ� locctr�� �ڵ����� ������Ų��
* �Ű� : ����
* ��ȯ : ����
* -----------------------------------------------------------------------------------
*/
void add_literal_addr() {

	for (int i = 0; i < literal_index; i++) {
		if (literal_table[i].addr == 0) {
			//�ּ� �Ҵ��� �ȵ� literal�� ��� �ּҸ� �Ҵ��Ѵ�
			literal_table[i].addr = locctr;

			if (literal_table[i].format == 'C') { //'EOF'->strlen��ŭ �߰�
				locctr += strlen(literal_table[i].literal);
			}
			else { //HEX(X)�� ���
				// 'F1' -> strlen/2��ŭ �߰�
				locctr += strlen(literal_table[i].literal) / 2;
			}
		}
	}

}

/* ----------------------------------------------------------------------------------
* ���� : symbol �̸��� �ָ� �ش��ϴ� control section������ address�� ��ȯ�ϴ� �Լ�
*		 control section�� �ٸ��� symbol�� ���ĵ� SYMTAB�� ���� ������
*		 �װ��� �������� �� �ִ� ������ �ʿ��ϴ�.
* �Ű� : symbol�̸�, symbol�� ã�� control section
* ��ȯ : control section������ address, �ش� symbol�� ���� ��� -1 ��ȯ
* -----------------------------------------------------------------------------------
*/
int symbol_address(char *str, int cs) {

	int section=-2;
	//addr�� 0�� ���ö����� section�� �ϳ��� ������.
	//�ʹݿ� program �̸�(COPY) ���ϰ�, FIRST�� �ֱ� ������ ó�� section�� -2�� ��Ҵ�.
	//symbol_unit�� section�� �߰��ϴ� ���� ������ �� ����.

	for (int i = 0; i < sym_index; i++) {
		if (sym_table[i].addr == 0) {
			section++;
		}

		if (strcmp(str, sym_table[i].symbol) == 0 && section==cs) {
			return sym_table[i].addr; //���� section������ symbol�� ã�� ��� �ּ� ��ȯ
		}
	}
	return -1;
}

/* ----------------------------------------------------------------------------------
* ���� : instruction code�� text record�� �߰��ϴ� �Լ�
*		�߰��Ϸ��� ���ڵ尡 MAX_TEXT�� ���� ���, ���� �ؽ�Ʈ�� object code�� ����
*		�� ���ڵ带 �����. ���� ���� ���� ���, ���� ���ڵ忡 �߰��Ϸ��� ���ڵ带 �����̴�
*		�۾��� �Ѵ�. ���� ���� record �ۼ��� �������� �ʾ��� ���, �� ���ڵ带 ������ش�.
*		locctr ���� �˾Ƽ� ���Ѵ�.
* �Ű� : buf(���� text record �� ���� �� buffer)
*		record(���� record�� instruction code���� ����ִ� buffer),
*		object(�߰��� instruction code�� ����ִ� buffer),
*		tot_length_ptr(���� text record�� ���̸� ���� ������ pointer),
*		byte(�߰��Ϸ��� instruction code�� byte)
* ��ȯ : ������ 1�� ��ȯ�Ѵ�.
* -----------------------------------------------------------------------------------
*/
int add_text_record(char *buf, char *record, char *object, int *tot_length_ptr, int byte) {

	if (*tot_length_ptr==0) {
		//�̹��� ���ο� text record�� ���� �����ϴ� ���
		object_code[object_index][0] = 'T';
		sprintf(buf, "%06X", locctr);
		strcat(object_code[object_index], buf); //�����ּ�(���� locctr)�� �߰��Ѵ�
		strcpy(record, object); //record�� ���� instruction code�� �־��ش�
		//strcat�ϸ� �����Ⱚ �ڿ� �߰��ǹǷ� ���� �ʱ�ȭ �ؾ���!
		*tot_length_ptr = byte; //text_length�� �������ش�
		locctr += byte; //locctr ���� ������Ų��

		return 1;
	}

	if ((*tot_length_ptr + byte) > MAX_TEXT) {
		//���� ���� �ڵ带 �߰��ϸ� text record �ִ� ��� ���̸� �ѱ�� ���
		fin_text_record(buf, record, tot_length_ptr); //���ݱ����� text record�� object code�� ���´�
		object_code[object_index][0] = 'T'; //���ο� record�� �����
		sprintf(buf, "%06X", locctr);
		strcat(object_code[object_index], buf);
		*tot_length_ptr = byte; //text_length�� �������ش�
		strcpy(record, object); //record�� �߰��� record�� ���� ����(record ���� ����)
		locctr += byte;
	}
	else {
		//�߰��ص� �̻� ���� ���
		strcat(record, object); //���� record�� �߰��� ���ڵ带 �߰��Ѵ�.
		*tot_length_ptr = *tot_length_ptr + byte; //text_length�� �߰��� record�� byte��ŭ �����Ѵ�
		locctr += byte; //locctr�� ������Ų��.
	}
	
	return 1;
}

/* ----------------------------------------------------------------------------------
* ���� : ���ݱ��� ���� record�� object_code�� ����ϰ� ������ �Լ�
*		 ���� ���� record�� ���۵��� ���� ���¶�� �׳� ���ư���.
* �Ű� : buf(���� text record �� ���� �� buffer),
*		record(���� record�� instruction code���� ����ִ� buffer),
*		tot_length_ptr(���� text record�� ���̸� ���� ������ pointer)
* -----------------------------------------------------------------------------------
*/
void fin_text_record(char *buf, char *record, int *tot_length_ptr) {

	if (*tot_length_ptr == 0) {
		//record�� ���۵��� ���� ��� ���� ���� �����Ƿ� ���ư���
		return;
	}
	//���� record�� �ִ� ���
	sprintf(buf, "%02X%s", *tot_length_ptr,record); //buffer�� length�� ���� record���� �߰��Ѵ�
	strcat(object_code[object_index], buf); //length + records�� object_code�� ���´�
	//�����ּҴ� �̹� ���־���
	object_index++; //index�� ������Ų��
	*tot_length_ptr = 0; //text length�� reset�Ѵ�
	record[0] = '\0'; //record�� reset��Ų��
}

/* ----------------------------------------------------------------------------------
* ���� : modify table�� modify unit�� �ϳ� �߰��ϴ� �Լ�
* �Ű� : addr(������ �ּ�), length(������ ����), plus(�ش� �ּҸ� ���ϴ��� ������ ��ȣ),
*		 name(���ϰų� �� symbol�� �̸�), section(�����ϴ� record�� ��� section����)
* ��ȯ : ����
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

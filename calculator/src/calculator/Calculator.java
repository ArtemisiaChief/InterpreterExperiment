package calculator;

import java.util.Scanner;

public class Calculator {

    public static void main(String[] args) {
        while(true) {
            System.out.println("请输入运算符表达式: ");
            LexicalAnalysis lexicalAnalysis = new LexicalAnalysis();
            Scanner in = new Scanner(System.in);
            String input = in.next();
//            String input = "-25 * 2";
            lexicalAnalysis.Lex(input);
        }
    }
}

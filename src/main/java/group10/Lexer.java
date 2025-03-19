package group10;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * group10.Lexer 类负责整个词法分析过程：
 * 1. 通过 group10.group10.InputHandler 逐字符读取源代码（输入处理模块）
 * 2. 利用简单的 DFA 逻辑实现词法规则的识别（词法规则解析及 DFA 执行模块）
 * 3. 调用 group10.group10.ErrorHandler 处理扫描过程中遇到的错误（错误处理模块）
 */
public class Lexer {
    private InputHandler input;
    private int currentChar;
    private int line;
    private int column;
    private List<Token> tokens;

    public Lexer(String filename) {
        try {
            input = new InputHandler(filename);
            line = 1;
            column = 0;
            currentChar = input.getNextChar();
            tokens = new ArrayList<>();
        } catch (IOException e) {
            System.err.println("无法打开文件：" + filename);
        }
    }

    /**
     * 分析文件并返回结果字符串，用于图形界面显示
     */
    public String analyzeToString() {
        StringBuilder sb = new StringBuilder();
        while (currentChar != -1) {
            // 如果遇到预处理指令（#在行首）
            if (currentChar == '#' && column == 0) {
                Token token = readPreprocessorDirective();
                tokens.add(token);
                // 可选：在 token 内容中判断是否包含 "#include"，若是，则后续处理头文件名
                continue;
            }

            // 在主循环中（例如 analyzeToString() 的 while 循环中）
            if (currentChar == '/') {
                int startLine = line;
                int startColumn = column;
                advance(); // 跳过 '/'
                if (currentChar == '/') {
                    // 单行注释：跳过直到换行
                    skipSingleLineComment();
                    continue; // 不生成任何 Token
                } else if (currentChar == '*') {
                    // 多行注释：跳过直到遇到 "*/"
                    skipMultiLineComment();
                    continue; // 不生成任何 Token
                } else {
                    // 不是注释，则 '/' 仍为操作符
                    tokens.add(new Token(TokenType.OPERATOR, "/", startLine, startColumn));
                    continue;
                }
            }

            // 如果遇到字符串字面量
            if (currentChar == '"') {
                Token token = readStringLiteral();
                tokens.add(token);
                sb.append(token).append("\n");
                continue;
            }

            // 跳过空白字符（空格、换行、制表符等）
            if (Character.isWhitespace(currentChar)) {
                skipWhitespace();
                continue;
            }

            // 识别标识符或关键字：字母或下划线开头
            if (Character.isLetter(currentChar) || currentChar == '_') {
                Token token = readIdentifier();
                tokens.add(token);
                sb.append(token).append("\n");
                continue;
            }

            // 识别数字
            if (Character.isDigit(currentChar)) {
                Token token = readNumber();
                tokens.add(token);
                sb.append(token).append("\n");
                continue;
            }

            // 识别操作符或分隔符
            if (isOperatorOrDelimiter((char) currentChar)) {
                Token token = readOperatorOrDelimiter();
                tokens.add(token);
                sb.append(token).append("\n");
                continue;
            }

            // 未识别字符，调用错误处理模块
            ErrorHandler.reportError("非法字符: " + (char) currentChar, line, column);
            tokens.add(new Token(TokenType.ERROR, String.valueOf((char) currentChar), line, column));
            sb.append("Error at line ").append(line).append(", column ").append(column)
                    .append(": ").append((char) currentChar).append("\n");
            advance();
        }
        // 添加文件结束标记
        Token eofToken = new Token(TokenType.EOF, "EOF", line, column);
        tokens.add(eofToken);
        sb.append(eofToken).append("\n");

        try {
            input.close();
        } catch (IOException e) {
            sb.append("关闭文件时出错。\n");
        }
        return sb.toString();
    }

    /**
     * 读取下一个字符，并更新行列计数
     */
    private void advance() {
        try {
            if (currentChar == '\n') {
                line++;
                column = 0;
            } else {
                column++;
            }
            currentChar = input.getNextChar();
        } catch (IOException e) {
            currentChar = -1;
        }
    }

    /**
     * 跳过所有空白字符
     */
    private void skipWhitespace() {
        while (currentChar != -1 && Character.isWhitespace(currentChar)) {
            advance();
        }
    }

    /**
     * 读取标识符或关键字（DFA状态：S0 -> S1）
     */
    private Token readIdentifier() {
        StringBuilder sb = new StringBuilder();
        int tokenLine = line;
        int tokenColumn = column;
        while (currentChar != -1 && (Character.isLetterOrDigit(currentChar) || currentChar == '_')) {
            sb.append((char) currentChar);
            advance();
        }
        String lexeme = sb.toString();
        // 简单判断关键字（可根据需要扩展）
        if (lexeme.equals("int") || lexeme.equals("return") || lexeme.equals("if") || lexeme.equals("else")) {
            return new Token(TokenType.KEYWORD, lexeme, tokenLine, tokenColumn);
        }
        return new Token(TokenType.IDENTIFIER, lexeme, tokenLine, tokenColumn);
    }

    /**
     * 读取数字常量（DFA状态：S0 -> S2）
     */
    private Token readNumber() {
        StringBuilder sb = new StringBuilder();
        int tokenLine = line;
        int tokenColumn = column;
        while (currentChar != -1 && Character.isDigit(currentChar)) {
            sb.append((char) currentChar);
            advance();
        }
        return new Token(TokenType.NUMBER, sb.toString(), tokenLine, tokenColumn);
    }

    /**
     * 读取操作符或分隔符（例如 + - * / = ; ( ) { } , < > 等）
     * 目前仅支持单字符操作符和分隔符，后续可扩展处理多字符情况（如 >=, ==, != 等）
     */
    private Token readOperatorOrDelimiter() {
        int tokenLine = line;
        int tokenColumn = column;
        char ch = (char) currentChar;
        advance(); // 跳过当前字符

        // 定义分隔符集合，如括号、分号、逗号、大括号等
        String delimiters = "();,{}[]";
        // 定义操作符集合，如 +, -, *, /, =, <, >, !, & 等
        String operators = "+-*/=<>!&|%^~"; // 根据需要扩展

        if (delimiters.indexOf(ch) != -1) {
            return new Token(TokenType.DELIMITER, String.valueOf(ch), tokenLine, tokenColumn);
        } else if (operators.indexOf(ch) != -1) {
            return new Token(TokenType.OPERATOR, String.valueOf(ch), tokenLine, tokenColumn);
        } else {
            // 若不属于以上两类，视为错误
            ErrorHandler.reportError("未知符号: " + ch, tokenLine, tokenColumn);
            return new Token(TokenType.ERROR, String.valueOf(ch), tokenLine, tokenColumn);
        }
    }


    private Token readPreprocessorDirective() {
        int tokenLine = line;
        int tokenColumn = column;
        StringBuilder sb = new StringBuilder();
        // 读取 '#' 本身
        sb.append((char) currentChar);
        advance();
        // 继续读取直到换行符结束
        while (currentChar != -1 && currentChar != '\n') {
            sb.append((char) currentChar);
            advance();
        }
        return new Token(TokenType.PREPROCESSOR, sb.toString(), tokenLine, tokenColumn);
    }

    private Token readHeaderName() {
        int tokenLine = line;
        int tokenColumn = column;
        StringBuilder sb = new StringBuilder();
        char delimiter = (char) currentChar;  // 应该是 '<' 或 '"'
        sb.append(delimiter);
        advance();
        while (currentChar != -1 && (char) currentChar != (delimiter == '<' ? '>' : delimiter)) {
            sb.append((char) currentChar);
            advance();
        }
        if (currentChar != -1) {
            sb.append((char) currentChar); // 添加结束符
            advance();
        } else {
            ErrorHandler.reportError("未闭合的头文件名称", tokenLine, tokenColumn);
        }
        return new Token(TokenType.IDENTIFIER, sb.toString(), tokenLine, tokenColumn);
    }

    private Token readStringLiteral() {
        int tokenLine = line;
        int tokenColumn = column;
        StringBuilder sb = new StringBuilder();
        // 跳过起始的双引号
        advance();
        while (currentChar != -1 && (char) currentChar != '"') {
            if (currentChar == '\\') {  // 处理转义字符
                sb.append((char) currentChar);
                advance();
                if (currentChar != -1) {
                    sb.append((char) currentChar);
                    advance();
                }
            } else {
                sb.append((char) currentChar);
                advance();
            }
        }
        if (currentChar == -1) {
            ErrorHandler.reportError("未闭合的字符串字面量", tokenLine, tokenColumn);
            return new Token(TokenType.ERROR, sb.toString(), tokenLine, tokenColumn);
        }
        // 跳过结束的双引号
        advance();
        return new Token(TokenType.STRING, sb.toString(), tokenLine, tokenColumn);
    }


    /**
     * 判断字符是否属于操作符或分隔符集合
     */
    private boolean isOperatorOrDelimiter(char ch) {
        return "+-*/=<>!;(),{}".indexOf(ch) != -1;
    }

    public void analyze() {
        System.out.println(analyzeToString());
    }

    private void skipSingleLineComment() {
        // 继续读取直到遇到换行符或文件结束
        while (currentChar != -1 && currentChar != '\n') {
            advance();
        }
    }


    private void skipMultiLineComment() {
        // 已经读取到 '*'，现在跳过注释体直到遇到 "*/"
        advance(); // 跳过 '*' 字符
        while (currentChar != -1) {
            if (currentChar == '*') {
                advance();
                if (currentChar == '/') {
                    advance(); // 跳过 '/'
                    break; // 注释结束
                }
            } else {
                advance();
            }
        }
        // 如果 currentChar == -1 仍未找到 "*/"，可以调用错误处理报告未闭合的注释（可选）
    }

}

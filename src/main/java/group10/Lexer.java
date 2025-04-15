package group10;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * group10.Lexer 类负责整个词法分析过程：
 * 1. 通过 InputHandler 逐字符读取源代码（输入处理模块）
 * 2. 利用简单的 DFA 逻辑实现词法规则的识别（词法规则解析及 DFA 执行模块）
 * 3. 调用 ErrorHandler 处理扫描过程中遇到的错误（错误处理模块）
 */
public class Lexer {
    private InputHandler input;
    private int currentChar;
    private int line;
    private int column;
//    private List<Token> tokens;

    public Lexer(String filename) {
        try {
            input = new InputHandler(filename);
            line = 1;
            column = 0;
            currentChar = input.getNextChar();
            // tokens = new ArrayList<>();
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
                // tokens.add(token);
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
                    // tokens.add(new Token(TokenType.OPERATOR, "/", startLine, startColumn));
                    continue;
                }
            }

            // 如果遇到字符串字面量
            if (currentChar == '"') {
                Token token = readStringLiteral();
                // tokens.add(token);
                appendTokenToOutput(sb, token);
                continue;
            }

            // 处理单引号
            if (currentChar == '\'') {
                Token token = readCharLiteral();
                // tokens.add(token);
                appendTokenToOutput(sb, token);
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
                // tokens.add(token);
                appendTokenToOutput(sb, token);
                continue;
            }

            // 识别数字
            if (Character.isDigit(currentChar)) {
                Token token = readNumber();
                // tokens.add(token);
                appendTokenToOutput(sb, token);
                continue;
            }

            // 识别操作符或分隔符
            if (isOperatorOrDelimiter((char) currentChar)) {
                Token token = readOperatorOrDelimiter();
                // tokens.add(token);
                appendTokenToOutput(sb, token);
                continue;
            }

            // 未识别字符，调用错误处理模块
            ErrorHandler.reportError("非法字符: " + (char) currentChar, line, column);
            // tokens.add(new Token(TokenType.ERROR, String.valueOf((char) currentChar), line, column));
            appendTokenToOutput(sb, new Token(TokenType.ERROR, String.valueOf((char) currentChar), line, column));
            advance();
        }
        // 添加文件结束标记
        Token eofToken = new Token(TokenType.EOF, "EOF", line, column);
        // // tokens.add(eofToken);
        appendTokenToOutput(sb, eofToken);

        try {
            input.close();
        } catch (IOException e) {
            sb.append("关闭文件时出错。\n");
        }
        return sb.toString();
    }

    /**
     * 将 Token 信息添加到输出字符串中，使用格式化输出
     */
    private void appendTokenToOutput(StringBuilder sb, Token token) {
        sb.append(String.format("%-20s \t %-20s \t @%d:%d\n", token.getType(), token.getLexeme(), token.getLine(), token.getColumn()));
    }

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

    private void skipWhitespace() {
        while (currentChar != -1 && Character.isWhitespace(currentChar)) {
            advance();
        }
    }

    private Token readIdentifier() {
        StringBuilder sb = new StringBuilder();
        int tokenLine = line;
        int tokenColumn = column;
        while (currentChar != -1 && (Character.isLetterOrDigit(currentChar) || currentChar == '_')) {
            sb.append((char) currentChar);
            advance();
        }
        String lexeme = sb.toString();
        if (lexeme.equals("int") || lexeme.equals("return") || lexeme.equals("if") || lexeme.equals("else")) {
            return new Token(TokenType.KEYWORD, lexeme, tokenLine, tokenColumn);
        }
        return new Token(TokenType.IDENTIFIER, lexeme, tokenLine, tokenColumn);
    }

    private Token readNumber() {
        StringBuilder sb = new StringBuilder();
        int tokenLine = line;
        int tokenColumn = column;

        while (currentChar != -1 && Character.isDigit(currentChar)) {
            sb.append((char) currentChar);
            advance();
        }

        if (currentChar == '.') {
            int dotLine = line;
            int dotColumn = column;
            advance();
            if (currentChar != -1 && Character.isDigit(currentChar)) {
                sb.append('.');
                while (currentChar != -1 && Character.isDigit(currentChar)) {
                    sb.append((char) currentChar);
                    advance();
                }
            } else {
                sb.append('.');
            }
        }

        return new Token(TokenType.NUMBER, sb.toString(), tokenLine, tokenColumn);
    }

    private Token readOperatorOrDelimiter() {
        int tokenLine = line;
        int tokenColumn = column;
        char ch = (char) currentChar;
        advance();

        // 更新分隔符集合
        String delimiters = "();,{}[].'?:&|";
        // 更新操作符集合
        String operators = "+-*/=<>!&|%^~";

        if (delimiters.indexOf(ch) != -1) {
            return new Token(TokenType.DELIMITER, String.valueOf(ch), tokenLine, tokenColumn);
        } else if (operators.indexOf(ch) != -1) {
            return new Token(TokenType.OPERATOR, String.valueOf(ch), tokenLine, tokenColumn);
        } else {
            ErrorHandler.reportError("未知符号: " + ch, tokenLine, tokenColumn);
            return new Token(TokenType.ERROR, String.valueOf(ch), tokenLine, tokenColumn);
        }
    }

    private Token readPreprocessorDirective() {
        int tokenLine = line;
        int tokenColumn = column;
        StringBuilder sb = new StringBuilder();
        sb.append((char) currentChar);
        advance();
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
        char delimiter = (char) currentChar;
        sb.append(delimiter);
        advance();
        while (currentChar != -1 && (char) currentChar != (delimiter == '<' ? '>' : delimiter)) {
            sb.append((char) currentChar);
            advance();
        }
        if (currentChar != -1) {
            sb.append((char) currentChar);
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
        advance();
        while (currentChar != -1 && (char) currentChar != '"') {
            if (currentChar == '\\') {
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
        advance();
        return new Token(TokenType.STRING, sb.toString(), tokenLine, tokenColumn);
    }

    // 处理单引号字符
    private Token readCharLiteral() {
        int tokenLine = line;
        int tokenColumn = column;
        StringBuilder sb = new StringBuilder();
        advance();
        while (currentChar != -1 && (char) currentChar != '\'') {
            if (currentChar == '\\') {
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
            ErrorHandler.reportError("未闭合的字符字面量", tokenLine, tokenColumn);
            return new Token(TokenType.ERROR, sb.toString(), tokenLine, tokenColumn);
        }
        advance();
        return new Token(TokenType.STRING, sb.toString(), tokenLine, tokenColumn);
    }

    private boolean isOperatorOrDelimiter(char ch) {
        return "+-*/=<>!;(),{}[].'?:&|".indexOf(ch) != -1;
    }

    public void analyze() {
        System.out.println(analyzeToString());
    }

    private void skipSingleLineComment() {
        while (currentChar != -1 && currentChar != '\n') {
            advance();
        }
    }

    private void skipMultiLineComment() {
        advance();
        while (currentChar != -1) {
            if (currentChar == '*') {
                advance();
                if (currentChar == '/') {
                    advance();
                    break;
                }
            } else {
                advance();
            }
        }
    }
}
/**
 * 错误处理模块，用于报告词法错误信息，如非法字符、未闭合字符串等
 */
public class ErrorHandler {
    public static void reportError(String message, int line, int column) {
        System.err.println("Error at line " + line + ", column " + column + ": " + message);
    }
}

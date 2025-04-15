package group10;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class InputHandler {
    private final BufferedReader reader;

    public InputHandler(String filename) throws IOException {
        // 指定字符编码为 UTF-8
        reader = new BufferedReader(new InputStreamReader(Files.newInputStream(Paths.get(filename)), StandardCharsets.UTF_8));
    }

    /**
     * 读取下一个字符，如果已到达文件末尾则返回-1
     */
    public int getNextChar() throws IOException {
        return reader.read();
    }

    /**
     * 关闭文件流
     */
    public void close() throws IOException {
        reader.close();
    }
}
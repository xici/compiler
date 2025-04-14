package group10;

import java.io.*;

public class InputHandler {
    private final BufferedReader reader;

    public InputHandler(String filename) throws FileNotFoundException {
        reader = new BufferedReader(new FileReader(filename));
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

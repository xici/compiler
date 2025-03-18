import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

public class LexerGUI extends JFrame {
    private JButton openButton;
    private JTextArea outputArea;

    public LexerGUI() {
        super("C语言词法分析器 - 图形化界面");

        openButton = new JButton("选择C源文件");
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(outputArea);

        openButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                chooseFileAndAnalyze();
            }
        });

        this.setLayout(new BorderLayout());
        this.add(openButton, BorderLayout.NORTH);
        this.add(scrollPane, BorderLayout.CENTER);
        this.setSize(600, 400);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
    }

    /**
     * 使用文件选择器选择 C 源文件，然后调用 Lexer 对文件进行词法分析，并将结果显示在文本区中。
     */
    private void chooseFileAndAnalyze() {
        JFileChooser fileChooser = new JFileChooser();
        int retVal = fileChooser.showOpenDialog(this);
        if (retVal == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            outputArea.setText("正在分析文件: " + selectedFile.getAbsolutePath() + "\n\n");
            // 调用词法分析器进行分析，使用新增的 analyzeToString() 方法获取分析结果
            Lexer lexer = new Lexer(selectedFile.getAbsolutePath());
            String result = lexer.analyzeToString();
            outputArea.append(result);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LexerGUI().setVisible(true));
    }
}

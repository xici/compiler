package group10;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

public class LexerGUI extends JFrame {
    private JButton openButton;
    private JTextArea outputArea;

    public LexerGUI() {
        super("C语言词法分析器");

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
     * 使用文件选择器选择 C 源文件，然后调用 group10.Lexer 对文件进行词法分析，并将结果显示在文本区中。
     */
    private void chooseFileAndAnalyze() {
        JFileChooser fileChooser = new JFileChooser();
        int retVal = fileChooser.showOpenDialog(this);
        if (retVal == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            // 校验文件类型
            if (!selectedFile.getName().toLowerCase().endsWith(".c")) {
                JOptionPane.showMessageDialog(this,
                        "请选择合法的C语言源文件（扩展名 .c）",
                        "文件类型错误",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            outputArea.setText("正在分析文件: " + selectedFile.getAbsolutePath() + "\n\n");
            Lexer lexer = new Lexer(selectedFile.getAbsolutePath());
            String result = lexer.analyzeToString();
            outputArea.append(result);
        }
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LexerGUI().setVisible(true));
    }
}

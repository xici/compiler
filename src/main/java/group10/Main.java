package group10;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        FlatLightLaf.setup();
        new LexerGUI().setVisible(true);
    }
}
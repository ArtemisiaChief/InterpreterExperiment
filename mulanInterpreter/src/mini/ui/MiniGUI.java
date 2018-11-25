/*
 * Created by Chief on Thu Nov 15 10:05:17 CST 2018
 */

package mini.ui;

import mini.component.*;
import mini.entity.Node;
import mini.entity.Token;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Chief
 */
public class MiniGUI extends JFrame {

    private File inoFile;
    private File midiFile;
    private File file;
    private boolean hasSaved = false;
    private boolean hasChanged = false;
    private boolean ctrlPressed = false;
    private boolean sPressed = false;
    private SimpleAttributeSet attributeSet;
    private SimpleAttributeSet statementAttributeSet;
    private SimpleAttributeSet durationAttributeSet;
    private SimpleAttributeSet normalAttributeSet;
    private SimpleAttributeSet commentAttributeSet;
    private SimpleAttributeSet errorAttributeSet;
    private StyledDocument inputStyledDocument;
    private Pattern statementPattern;
    private Pattern keywordPattern;
    private Pattern parenPattern;

    private LexicalAnalysis lexicalAnalysis;
    private SyntacticAnalysis syntacticAnalysis;
    private SemanticAnalysisArduino semanticAnalysisArduino;
    private SemanticAnalysisMidi semanticAnalysisMidi;
    private ArduinoCmd arduinoCmd;

    private String cmdOutput;

    public MiniGUI() {
        initComponents();
        setResizable(false);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        //样式
        attributeSet = new SimpleAttributeSet();
        statementAttributeSet = new SimpleAttributeSet();
        durationAttributeSet = new SimpleAttributeSet();
        normalAttributeSet = new SimpleAttributeSet();
        commentAttributeSet = new SimpleAttributeSet();
        errorAttributeSet = new SimpleAttributeSet();
        StyleConstants.setForeground(attributeSet, new Color(92, 101, 192));
        StyleConstants.setBold(attributeSet, true);
        StyleConstants.setForeground(statementAttributeSet, new Color(30, 80, 180));
        StyleConstants.setBold(statementAttributeSet, true);
        StyleConstants.setForeground(durationAttributeSet, new Color(111, 150, 255));
        StyleConstants.setForeground(commentAttributeSet, new Color(128, 128, 128));
        StyleConstants.setForeground(errorAttributeSet, new Color(238, 0, 1));
        inputStyledDocument = inputTextPane.getStyledDocument();
        statementPattern = Pattern.compile("\\bparagraph\\b|\\bend\\b");
        keywordPattern = Pattern.compile("\\bspeed=|\\binstrument=|\\bvolume=|\\b1=|\\bplay");
        parenPattern = Pattern.compile("<(\\s*\\{?\\s*(1|2|4|8|g|\\*)+\\s*\\}?\\s*)+>");

        //关闭窗口提示
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                //删除临时ino文件
                if (showSaveComfirm("Exist unsaved content, save before exit?")) {
                    File tempFile = new File("D:\\Just For Save");
                    File[] files = tempFile.listFiles();
                    for (File fileToDel : files) {
                        fileToDel.delete();
                    }

                    tempFile=new File("C:\\Users\\Chief\\Documents\\Arduino\\temp.ino");
                    if(tempFile.exists())
                        tempFile.delete();

                    System.exit(0);
                }
            }
        });

        //着色与补全的监听
        inputTextPane.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_UP ||
                        e.getKeyCode() == KeyEvent.VK_DOWN ||
                        e.getKeyCode() == KeyEvent.VK_LEFT ||
                        e.getKeyCode() == KeyEvent.VK_RIGHT ||
                        e.getKeyCode() == KeyEvent.VK_BACK_SPACE ||
                        e.getKeyCode() == KeyEvent.VK_SHIFT ||
                        e.getKeyCode() == KeyEvent.VK_ALT)
                    return;

                if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
                    ctrlPressed = false;
                    return;
                }

                if (e.getKeyCode() == KeyEvent.VK_S) {
                    sPressed = false;
                    return;
                }

                autoComplete();
                refreshColor();
            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                    autoRemove();
                    refreshColor();
                }

                if (e.getKeyCode() == KeyEvent.VK_CONTROL)
                    ctrlPressed = true;

                if (e.getKeyCode() == KeyEvent.VK_S)
                    sPressed = true;

                if (ctrlPressed && sPressed) {
                    sPressed = false;
                    ctrlPressed = false;
                    saveMenuItemActionPerformed(null);
                }
            }
        });

        //是否有改动的监听
        inputTextPane.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                contentChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                contentChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                contentChanged();
            }
        });

        //组件实例化
        lexicalAnalysis = new LexicalAnalysis();
        syntacticAnalysis = new SyntacticAnalysis();
        semanticAnalysisArduino = new SemanticAnalysisArduino();
        semanticAnalysisMidi = new SemanticAnalysisMidi();
        arduinoCmd = new ArduinoCmd();

        cmdOutput = "";

        //行号与滚动条
        scrollPane3.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        scrollPane3.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        String lineStr = "";
        for (int i = 1; i < 1000; i++)
            lineStr += i + "\n";
        lineTextArea.setText(lineStr);
        scrollPane1.getVerticalScrollBar().addAdjustmentListener(e -> scrollPane3.getVerticalScrollBar().setValue(scrollPane1.getVerticalScrollBar().getValue()));
    }

    //内容变动调用的函数
    private void contentChanged() {
        if (hasChanged)
            return;

        hasChanged = true;
        if (this.getTitle().lastIndexOf("(Unsaved)") == -1)
            this.setTitle(this.getTitle() + " (Unsaved)");
    }

    //内容变动之后是否保存
    private boolean showSaveComfirm(String confirm) {
        if (hasChanged) {
            int exit = JOptionPane.showConfirmDialog(null, confirm, "Confirm", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            switch (exit) {
                case JOptionPane.YES_OPTION:
                    saveMenuItemActionPerformed(null);
                    break;
                case JOptionPane.NO_OPTION:
                    break;
                case JOptionPane.CANCEL_OPTION:
                    return false;
            }
        }
        return true;
    }

    //自动删除界符
    private void autoRemove() {
        StringBuilder input = new StringBuilder(inputTextPane.getText().replace("\r", ""));
        int pos = inputTextPane.getCaretPosition();
        if (input.length() > 1 && pos < input.length() && pos > 0) {
            if ((input.charAt(pos - 1) == '(' && input.charAt(pos) == ')') ||
                    (input.charAt(pos - 1) == '[' && input.charAt(pos) == ']') ||
                    (input.charAt(pos - 1) == '<' && input.charAt(pos) == '>') ||
                    (input.charAt(pos - 1) == '{' && input.charAt(pos) == '}')) {
                input.deleteCharAt(pos);
                inputTextPane.setText(input.toString());
                inputTextPane.setCaretPosition(pos);
                return;
            }
        }
    }

    //自动补全界符与注释符号
    private void autoComplete() {
        StringBuilder input = new StringBuilder(inputTextPane.getText().replace("\r", ""));
        int pos = inputTextPane.getCaretPosition();
        if (pos > 0) {
            if (pos < input.length() && (input.substring(pos, pos + 1).equals(" ") || input.substring(pos, pos + 1).equals("\n")) || pos == input.length())
                switch (input.charAt(pos - 1)) {
                    case '(':
                        input.insert(pos, ')');
                        inputTextPane.setText(input.toString());
                        inputTextPane.setCaretPosition(pos);
                        return;
                    case '[':
                        input.insert(pos, ']');
                        inputTextPane.setText(input.toString());
                        inputTextPane.setCaretPosition(pos);
                        return;
                    case '<':
                        input.insert(pos, '>');
                        inputTextPane.setText(input.toString());
                        inputTextPane.setCaretPosition(pos);
                        return;
                    case '{':
                        input.insert(pos, '}');
                        inputTextPane.setText(input.toString());
                        inputTextPane.setCaretPosition(pos);
                        return;
                    case '*':
                        if (input.charAt(pos - 2) == '/') {
                            input.insert(inputTextPane.getCaretPosition(), "\n\n*/");
                            inputTextPane.setText(input.toString());
                            inputTextPane.setCaretPosition(pos + 1);
                        }
                        return;
                }
        }
    }

    //代码着色
    private void refreshColor() {
        String input = inputTextPane.getText().replace("\r", "");

        inputStyledDocument.setCharacterAttributes(
                0,
                input.length(),
                normalAttributeSet, true
        );

        //声明着色
        Matcher statementMatcher = statementPattern.matcher(input);
        while (statementMatcher.find()) {
            inputStyledDocument.setCharacterAttributes(
                    statementMatcher.start(),
                    statementMatcher.end() - statementMatcher.start(),
                    statementAttributeSet, true
            );
        }

        //关键字着色
        Matcher inputMatcher = keywordPattern.matcher(input);
        while (inputMatcher.find()) {
            inputStyledDocument.setCharacterAttributes(
                    inputMatcher.start(),
                    inputMatcher.end() - inputMatcher.start(),
                    attributeSet, true
            );
        }

        //节奏片段着色
        Matcher parenMatcher = parenPattern.matcher(input);
        while (parenMatcher.find()) {
            inputStyledDocument.setCharacterAttributes(
                    parenMatcher.start(),
                    parenMatcher.end() - parenMatcher.start(),
                    durationAttributeSet, true
            );
        }

        //注释着色
        for (int i = 0; i < input.length(); i++) {
            //单行注释
            if (i + 1 < input.length())
                if (input.charAt(i) == '/' && input.charAt(i + 1) == '/')
                    while (i + 1 < input.length() && input.charAt(i) != '\n') {
                        i++;
                        inputStyledDocument.setCharacterAttributes(
                                i - 1,
                                2,
                                commentAttributeSet, true
                        );
                    }

            //多行注释
            if (i + 1 < input.length() && input.charAt(i) == '/' && input.charAt(i + 1) == '*')
                while (i + 1 < input.length() && (input.charAt(i) != '*' || input.charAt(i + 1) != '/')) {
                    i++;
                    inputStyledDocument.setCharacterAttributes(
                            i - 1,
                            3,
                            commentAttributeSet, true
                    );
                }
        }
    }

    //新建文件
    private void newMenuItemActionPerformed(ActionEvent e) {
        if (showSaveComfirm("Exist unsaved content, save before new file?")) {
            hasSaved = false;
            inputTextPane.setText("");
            outputTextPane.setText("");
            hasChanged = false;
            this.setTitle("Music Interpreter - New File");
        }
    }

    //打开文件
    private void openMenuItemActionPerformed(ActionEvent e) {
        if (!showSaveComfirm("Exist unsaved content, save before open fire?"))
            return;

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Music Interpreter File", "mui");
        fileChooser.setFileFilter(filter);
        fileChooser.showOpenDialog(this);
        file = fileChooser.getSelectedFile();
        if (file == null)
            return;
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
            StringBuilder stringBuilder = new StringBuilder();
            String content;
            while ((content = bufferedReader.readLine()) != null) {
                stringBuilder.append(content);
                stringBuilder.append(System.getProperty("line.separator"));
            }
            bufferedReader.close();
            inputTextPane.setText(stringBuilder.toString());
            outputTextPane.setText("");
            refreshColor();
            hasSaved = true;
            hasChanged = false;
            this.setTitle("Music Interpreter - " + file.getName());
        } catch (FileNotFoundException e1) {
//            e1.printStackTrace();
        } catch (IOException e1) {
//            e1.printStackTrace();
        }
    }

    //保存文件
    private void saveMenuItemActionPerformed(ActionEvent e) {
        if (!hasSaved) {
            saveAsMenuItemActionPerformed(null);
        } else {
            try {
                if (!file.exists())
                    file.createNewFile();
                BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
                bufferedWriter.write(inputTextPane.getText());
                bufferedWriter.close();
                hasChanged = false;
                this.setTitle("Music Interpreter - " + file.getName());
            } catch (IOException e1) {
//                e1.printStackTrace();
            }
        }
    }

    //另存为文件
    private void saveAsMenuItemActionPerformed(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Music Interpreter File", "mui");
        fileChooser.setFileFilter(filter);
        fileChooser.showSaveDialog(this);
        if (fileChooser.getSelectedFile() == null)
            return;
        String fileStr = fileChooser.getSelectedFile().getAbsoluteFile().toString();
        if (fileStr.lastIndexOf(".mui") == -1)
            fileStr += ".mui";
        file = new File(fileStr);
        try {
            if (!file.exists())
                file.createNewFile();
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
            bufferedWriter.write(inputTextPane.getText());
            bufferedWriter.close();
            hasSaved = true;
            hasChanged = false;
            this.setTitle("Music Interpreter - " + file.getName());
        } catch (FileNotFoundException e1) {
//            e1.printStackTrace();
        } catch (IOException e1) {
//            e1.printStackTrace();
        }
    }

    //通过行号找到改行第一个字符在输入字符串中的位置
    private int getIndexByLine(int line) {
        int index = 0;
        String input = inputTextPane.getText().replace("\r", "") + "\n";

        for (int i = 0; i < line - 1; i++) {
            index = input.indexOf("\n", index + 1);
        }
        return index;
    }

    //词法分析
    private ArrayList<Token> runLex(String input, StringBuilder output) {
        ArrayList<Token> tokens = lexicalAnalysis.Lex(input);

        if (lexicalAnalysis.getError()) {
            output.append(lexicalAnalysis.getErrorInfo(tokens));
            output.append("检测到词法错误，分析停止");
            outputTextPane.setText(output.toString());
            for (int line : lexicalAnalysis.getErrorLine()) {
                inputStyledDocument.setCharacterAttributes(
                        getIndexByLine(line),
                        getIndexByLine(line + 1) - getIndexByLine(line),
                        errorAttributeSet, true
                );
            }
            return null;
        } else
            for (Token token : tokens)
                output.append(token);

        return tokens;
    }

    //语法分析
    private Node runSyn(ArrayList<Token> tokens, StringBuilder output) {
        Node AbstractSyntaxTree = syntacticAnalysis.Parse(tokens);

        if (syntacticAnalysis.getIsError()) {
            output.append(syntacticAnalysis.getErrors(AbstractSyntaxTree));
            output.append("\n检测到语法错误，分析停止\n");
            outputTextPane.setText(output.toString());
            for (int line : syntacticAnalysis.getErrorList()) {
                inputStyledDocument.setCharacterAttributes(
                        getIndexByLine(line),
                        getIndexByLine(line + 1) - getIndexByLine(line),
                        errorAttributeSet, true
                );
            }
            return null;
        } else
            output.append(AbstractSyntaxTree.print(0));

        return AbstractSyntaxTree;
    }

    //Arduino语义分析
    private String runArduinoSem(Node abstractSyntaxTree, StringBuilder output) {
        String code = semanticAnalysisArduino.ConvertToArduino(abstractSyntaxTree);

        if (semanticAnalysisArduino.getIsError()) {
            output.append(semanticAnalysisArduino.getErrors());
            output.append("\n检测到语义错误，分析停止\n");
            outputTextPane.setText(output.toString());
            for (int line : semanticAnalysisArduino.getErrorLines()) {
                inputStyledDocument.setCharacterAttributes(
                        getIndexByLine(line),
                        getIndexByLine(line + 1) - getIndexByLine(line),
                        errorAttributeSet, true
                );
            }
            return null;
        } else {
            output.append(code);
        }

        return code;
    }

    //Midi语义分析
    private String runMidiSem(Node abstractSyntaxTree, StringBuilder output) {
        String code = semanticAnalysisMidi.ConvertToMidi(abstractSyntaxTree);

        if (semanticAnalysisMidi.getIsError()) {
            output.append(semanticAnalysisMidi.getErrors());
            output.append("\n检测到语义错误，分析停止\n");
            outputTextPane.setText(output.toString());
            for (int line : semanticAnalysisMidi.getErrorLines()) {
                inputStyledDocument.setCharacterAttributes(
                        getIndexByLine(line),
                        getIndexByLine(line + 1) - getIndexByLine(line),
                        errorAttributeSet, true
                );
            }
            return null;
        } else {
            output.append(code);
        }

        return code;
    }

    //执行词法分析
    private void LexMenuItemActionPerformed(ActionEvent e) {
        StringBuilder stringBuilder = new StringBuilder();

        if (inputTextPane.getText().isEmpty())
            return;

        runLex(inputTextPane.getText(), stringBuilder);

        outputTextPane.setText(stringBuilder.toString());
    }

    //执行语法分析
    private void synMenuItemActionPerformed(ActionEvent e) {
        StringBuilder stringBuilder = new StringBuilder();


        if (inputTextPane.getText().isEmpty())
            return;

        ArrayList<Token> tokens = runLex(inputTextPane.getText(), stringBuilder);

        if (tokens == null)
            return;

        stringBuilder.append("\n=======词法分析结束======开始语法分析=======\n\n");

        runSyn(tokens, stringBuilder);

        outputTextPane.setText(stringBuilder.toString());
    }

    //执行Arduino语义分析
    private void semMenuItemActionPerformed(ActionEvent e) {
        StringBuilder stringBuilder = new StringBuilder();

        if (inputTextPane.getText().isEmpty())
            return;

        ArrayList<Token> tokens = runLex(inputTextPane.getText(), stringBuilder);

        if (tokens == null)
            return;

        stringBuilder.append("\n=======词法分析结束======开始语法分析=======\n\n");

        Node AbstractSyntaxTree = runSyn(tokens, stringBuilder);

        if (AbstractSyntaxTree == null)
            return;

        stringBuilder.append("\n=======语法分析结束======开始语义分析=======\n\n");

        runArduinoSem(AbstractSyntaxTree, stringBuilder);

        outputTextPane.setText(stringBuilder.toString());
    }

    //执行Midi语义分析
    private void sem2MenuItemActionPerformed(ActionEvent e) {
        StringBuilder stringBuilder = new StringBuilder();

        if (inputTextPane.getText().isEmpty())
            return;

        ArrayList<Token> tokens = runLex(inputTextPane.getText(), stringBuilder);

        if (tokens == null)
            return;

        stringBuilder.append("\n=======词法分析结束======开始语法分析=======\n\n");

        Node AbstractSyntaxTree = runSyn(tokens, stringBuilder);

        if (AbstractSyntaxTree == null)
            return;

        stringBuilder.append("\n=======语法分析结束======开始语义分析=======\n\n");

        runMidiSem(AbstractSyntaxTree, stringBuilder);

        outputTextPane.setText(stringBuilder.toString());
    }

    //生成Midi文件
    private void generateMidiMenuItemActionPerformed(ActionEvent e) {
        StringBuilder stringBuilder = new StringBuilder();

        if (inputTextPane.getText().isEmpty())
            return;

        ArrayList<Token> tokens = runLex(inputTextPane.getText(), stringBuilder);

        if (tokens == null)
            return;

        stringBuilder.append("\n=======词法分析结束======开始语法分析=======\n\n");

        Node AbstractSyntaxTree = runSyn(tokens, stringBuilder);

        if (AbstractSyntaxTree == null)
            return;

        stringBuilder.append("\n=======语法分析结束======开始语义分析=======\n\n");

        String code = runMidiSem(AbstractSyntaxTree, stringBuilder);

        if (code == null)
            return;

        outputTextPane.setText(code);

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Midi File", "mid");
        fileChooser.setFileFilter(filter);
        fileChooser.showSaveDialog(this);
        if (fileChooser.getSelectedFile() == null)
            return;
        String fileStr = fileChooser.getSelectedFile().getAbsoluteFile().toString();
        if (fileStr.lastIndexOf(".mid") == -1)
            fileStr += ".mid";
        midiFile = new File(fileStr);

        if(!semanticAnalysisMidi.getMidiFile().writeToFile(midiFile))
            JOptionPane.showMessageDialog(this, "目标文件被占用，无法导出", "Warning", JOptionPane.INFORMATION_MESSAGE);

    }

    //保存Arduino执行文件
    private void buildMenuItemActionPerformed(ActionEvent e) {
        StringBuilder stringBuilder = new StringBuilder();

        if (inputTextPane.getText().isEmpty())
            return;

        ArrayList<Token> tokens = runLex(inputTextPane.getText(), stringBuilder);

        if (tokens == null)
            return;

        stringBuilder.append("\n=======词法分析结束======开始语法分析=======\n\n");

        Node AbstractSyntaxTree = runSyn(tokens, stringBuilder);

        if (AbstractSyntaxTree == null)
            return;

        stringBuilder.append("\n=======语法分析结束======开始语义分析=======\n\n");

        String code = runArduinoSem(AbstractSyntaxTree, stringBuilder);

        if (code == null)
            return;

        outputTextPane.setText(code);

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Arduino File", "ino");
        fileChooser.setFileFilter(filter);
        fileChooser.showSaveDialog(this);
        if (fileChooser.getSelectedFile() == null)
            return;
        String fileStr = fileChooser.getSelectedFile().getAbsoluteFile().toString();
        if (fileStr.lastIndexOf(".ino") == -1)
            fileStr += ".ino";
        inoFile = new File(fileStr);
        try {
            if (!inoFile.exists())
                inoFile.createNewFile();
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(inoFile), "UTF-8"));
            bufferedWriter.write(code);
            bufferedWriter.close();
        } catch (FileNotFoundException e1) {
//            e1.printStackTrace();
        } catch (IOException e1) {
//            e1.printStackTrace();
        }

    }

    //读取Arduino CMD数据流
    private void readCmd() {
        compileMenuItem.setEnabled(false);
        uploadMenuItem.setEnabled(false);
        progressBar.setIndeterminate(true);
        cmdOutput = "";

        //处理输出的线程
        new Thread(() -> {
            int count = 0;
            try {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(ArduinoCmd.output, "GBK"));
                String tempStr;
                while ((tempStr = bufferedReader.readLine()) != null) {
                    count++;
                    if (count > 12) {
                        cmdOutput += tempStr + "\n";
                        outputTextPane.setText(cmdOutput);
                    }
                }
                progressBar.setIndeterminate(false);
                progressBar.setValue(100);
                compileMenuItem.setEnabled(true);
                uploadMenuItem.setEnabled(true);
            } catch (IOException IOE) {
                IOE.printStackTrace();
            } finally {
                try {
                    ArduinoCmd.output.close();
                } catch (IOException IOE) {
                    IOE.printStackTrace();
                }
            }
        }).start();

        //处理错误信息的线程
        new Thread(() -> {
            try {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(ArduinoCmd.error, "GBK"));
                String tempStr;
                while ((tempStr = bufferedReader.readLine()) != null) {
                    cmdOutput += tempStr + "\n";
                    outputTextPane.setText(cmdOutput);
                }
            } catch (IOException IOE) {
                IOE.printStackTrace();
            } finally {
                try {
                    ArduinoCmd.error.close();
                } catch (IOException IOE) {
                    IOE.printStackTrace();
                }
            }
        }).start();
    }

    //编译Arduino的十六进制文件
    private void compileMenuItemActionPerformed(ActionEvent e) {
        StringBuilder stringBuilder = new StringBuilder();

        if (inputTextPane.getText().isEmpty())
            return;

        ArrayList<Token> tokens = runLex(inputTextPane.getText(), stringBuilder);

        if (tokens == null)
            return;

        stringBuilder.append("\n=======词法分析结束======开始语法分析=======\n\n");

        Node AbstractSyntaxTree = runSyn(tokens, stringBuilder);

        if (AbstractSyntaxTree == null)
            return;

        stringBuilder.append("\n=======语法分析结束======开始语义分析=======\n\n");

        String code = runArduinoSem(AbstractSyntaxTree, stringBuilder);

        if (code == null)
            return;

        File tempFile;

        try {
            tempFile = new File("C:\\Users\\Chief\\Documents\\Arduino\\temp.ino");
            if (!tempFile.exists())
                tempFile.createNewFile();
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFile), "UTF-8"));
            bufferedWriter.write(code);
            bufferedWriter.close();

            arduinoCmd.compile(tempFile.getAbsolutePath());
            readCmd();

        } catch (IOException e1) {
//                e1.printStackTrace();
        }
    }

    //上传到Arduino
    private void uploadMenuItemActionPerformed(ActionEvent e) {
        StringBuilder stringBuilder = new StringBuilder();

        if (inputTextPane.getText().isEmpty())
            return;

        ArrayList<Token> tokens = runLex(inputTextPane.getText(), stringBuilder);

        if (tokens == null)
            return;

        stringBuilder.append("\n=======词法分析结束======开始语法分析=======\n\n");

        Node AbstractSyntaxTree = runSyn(tokens, stringBuilder);

        if (AbstractSyntaxTree == null)
            return;

        stringBuilder.append("\n=======语法分析结束======开始语义分析=======\n\n");

        String code = runArduinoSem(AbstractSyntaxTree, stringBuilder);

        if (code == null)
            return;

        File tempFile;

        try {
            tempFile = new File("C:\\Users\\Chief\\Documents\\Arduino\\temp.ino");
            if (!tempFile.exists())
                tempFile.createNewFile();
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFile), "UTF-8"));
            bufferedWriter.write(code);
            bufferedWriter.close();

            arduinoCmd.upload(tempFile.getAbsolutePath());
            readCmd();
        } catch (IOException e1) {
//                e1.printStackTrace();
        }
    }

    //直接播放Midi文件
    private void playMenuItemActionPerformed(ActionEvent e) {
        StringBuilder stringBuilder = new StringBuilder();

        if (inputTextPane.getText().isEmpty())
            return;

        ArrayList<Token> tokens = runLex(inputTextPane.getText(), stringBuilder);

        if (tokens == null)
            return;

        stringBuilder.append("\n=======词法分析结束======开始语法分析=======\n\n");

        Node AbstractSyntaxTree = runSyn(tokens, stringBuilder);

        if (AbstractSyntaxTree == null)
            return;

        stringBuilder.append("\n=======语法分析结束======开始语义分析=======\n\n");

        String code = runMidiSem(AbstractSyntaxTree, stringBuilder);

        if (code == null)
            return;

        outputTextPane.setText(code);

        Random random = new Random(System.currentTimeMillis());

        midiFile = new File("D:\\Just For Save\\" + random.nextInt(100) + ".mid");

        if (!semanticAnalysisMidi.getMidiFile().writeToFile(midiFile)) {
            JOptionPane.showMessageDialog(this, "目标文件被占用，无法导出", "Warning", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            Runtime.getRuntime().exec("rundll32 url.dll FileProtocolHandler file://"+midiFile.getAbsolutePath().replace("\\","\\\\"));
        }catch (IOException e1){
            e1.printStackTrace();
        }
    }


    //关于
    private void aboutMenuItemActionPerformed(ActionEvent e) {
        String str = "-----------------------------------------------------------\n" +
                "Music Language Interpreter\nMade By Chief, yzdxm and AsrielMao\nVersion: 0.1.0\n\n" +
                "A light weight interpreter for converting digit score       \n" +
                "to Arduino code\n" +
                "-----------------------------------------------------------";
        JOptionPane.showMessageDialog(this, str, "About", JOptionPane.INFORMATION_MESSAGE);
    }

    //展示Demo
    private void demoMenuItemActionPerformed(ActionEvent e) {
        if (!showSaveComfirm("Exist unsaved content, save before open the demo?"))
            return;

        String str = "/*\n" +
                " 欢乐颂\n" +
                " 女高音 + 女中音\n" +
                " 双声部 Version\n" +
                " */\n" +
                "\n" +
                "//女高音\n" +
                "paragraph soprano\n" +
                "instrument= 0\n" +
                "volume= 127\n" +
                "speed= 140\n" +
                "1= D\n" +
                "3345 5432 <4444 4444>\n" +
                "1123 322 <4444 4*82>\n" +
                "3345 5432 <4444 4444>\n" +
                "1123 211 <4444 4*82>\n" +
                "2231 23431 <4444 4{88}44>\n" +
                "23432 12(5) <4{88}44 {44}4>\n" +
                "33345 54342 <{44}444 44{48}8>\n" +
                "1123 211 <4444 4*82>\n" +
                "end\n" +
                "\n" +
                "//女中音\n" +
                "paragraph alto\n" +
                "instrument= 0\n" +
                "volume= 110\n" +
                "speed= 140\n" +
                "1= D\n" +
                "1123 321(5) <4444 4444>\n" +
                "(3555) 1(77) <4444 4*82>\n" +
                "1123 321(5) <4444 4444>\n" +
                "(3555) (533) <4444 4*82>\n" +
                "(77)1(5) (77)1(5) <4444 4444>\n" +
                "(7#5#5#56#45) <4444 {44}4>\n" +
                "11123 3211(5) <{44}444 44{48}8>\n" +
                "(3555 533) <4444 4*82>\n" +
                "end\n" +
                "\n" +
                "//双声部同时播放\n" +
                "play(soprano&alto)";
        inputTextPane.setText(str);
        outputTextPane.setText("");
        refreshColor();
        hasChanged = false;
        this.setTitle("Music Interpreter - Demo");
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        menuBar1 = new JMenuBar();
        fileMenu = new JMenu();
        newMenuItem = new JMenuItem();
        openMenuItem = new JMenuItem();
        saveMenuItem = new JMenuItem();
        saveAsMenuItem = new JMenuItem();
        runMenu = new JMenu();
        LexMenuItem = new JMenuItem();
        synMenuItem = new JMenuItem();
        semMenuItem = new JMenuItem();
        sem2MenuItem = new JMenuItem();
        buildMenu = new JMenu();
        buildMenuItem = new JMenuItem();
        compileMenuItem = new JMenuItem();
        uploadMenuItem = new JMenuItem();
        buildMidiMenu = new JMenu();
        generateMidiMenuItem = new JMenuItem();
        playMenuItem = new JMenuItem();
        helpMenu = new JMenu();
        demoMenuItem = new JMenuItem();
        aboutMenuItem = new JMenuItem();
        hSpacer1 = new JPanel(null);
        progressBar = new JProgressBar();
        panel1 = new JPanel();
        scrollPane3 = new JScrollPane();
        lineTextArea = new JTextArea();
        scrollPane1 = new JScrollPane();
        inputTextPane = new JTextPane();
        scrollPane2 = new JScrollPane();
        outputTextPane = new JTextPane();

        //======== this ========
        setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));
        setTitle("Music Interpreter - New File");
        setResizable(false);
        Container contentPane = getContentPane();
        contentPane.setLayout(new GridLayout());

        //======== menuBar1 ========
        {

            //======== fileMenu ========
            {
                fileMenu.setText("File");

                //---- newMenuItem ----
                newMenuItem.setText("New");
                newMenuItem.addActionListener(e -> newMenuItemActionPerformed(e));
                fileMenu.add(newMenuItem);

                //---- openMenuItem ----
                openMenuItem.setText("Open");
                openMenuItem.addActionListener(e -> openMenuItemActionPerformed(e));
                fileMenu.add(openMenuItem);

                //---- saveMenuItem ----
                saveMenuItem.setText("Save");
                saveMenuItem.addActionListener(e -> saveMenuItemActionPerformed(e));
                fileMenu.add(saveMenuItem);

                //---- saveAsMenuItem ----
                saveAsMenuItem.setText("Save As...");
                saveAsMenuItem.addActionListener(e -> saveAsMenuItemActionPerformed(e));
                fileMenu.add(saveAsMenuItem);
            }
            menuBar1.add(fileMenu);

            //======== runMenu ========
            {
                runMenu.setText("Run");

                //---- LexMenuItem ----
                LexMenuItem.setText("Lexical Analysis");
                LexMenuItem.addActionListener(e -> LexMenuItemActionPerformed(e));
                runMenu.add(LexMenuItem);

                //---- synMenuItem ----
                synMenuItem.setText("Syntactic Analysis");
                synMenuItem.addActionListener(e -> synMenuItemActionPerformed(e));
                runMenu.add(synMenuItem);

                //---- semMenuItem ----
                semMenuItem.setText("Semantic Analysis - Arduino");
                semMenuItem.addActionListener(e -> semMenuItemActionPerformed(e));
                runMenu.add(semMenuItem);

                //---- sem2MenuItem ----
                sem2MenuItem.setText("Semantic Analysis - Midi");
                sem2MenuItem.addActionListener(e -> sem2MenuItemActionPerformed(e));
                runMenu.add(sem2MenuItem);
            }
            menuBar1.add(runMenu);

            //======== buildMenu ========
            {
                buildMenu.setText("Build - Arduino");

                //---- buildMenuItem ----
                buildMenuItem.setText("Generate .ino file");
                buildMenuItem.addActionListener(e -> buildMenuItemActionPerformed(e));
                buildMenu.add(buildMenuItem);

                //---- compileMenuItem ----
                compileMenuItem.setText("Compile / Verify");
                compileMenuItem.addActionListener(e -> compileMenuItemActionPerformed(e));
                buildMenu.add(compileMenuItem);

                //---- uploadMenuItem ----
                uploadMenuItem.setText("Upload to Arduino");
                uploadMenuItem.addActionListener(e -> uploadMenuItemActionPerformed(e));
                buildMenu.add(uploadMenuItem);
            }
            menuBar1.add(buildMenu);

            //======== buildMidiMenu ========
            {
                buildMidiMenu.setText("Build - Midi");

                //---- generateMidiMenuItem ----
                generateMidiMenuItem.setText("Generate Midi File");
                generateMidiMenuItem.addActionListener(e -> generateMidiMenuItemActionPerformed(e));
                buildMidiMenu.add(generateMidiMenuItem);

                //---- playMenuItem ----
                playMenuItem.setText("Play Midi File");
                playMenuItem.addActionListener(e -> playMenuItemActionPerformed(e));
                buildMidiMenu.add(playMenuItem);
            }
            menuBar1.add(buildMidiMenu);

            //======== helpMenu ========
            {
                helpMenu.setText("Help");

                //---- demoMenuItem ----
                demoMenuItem.setText("Demo");
                demoMenuItem.addActionListener(e -> demoMenuItemActionPerformed(e));
                helpMenu.add(demoMenuItem);

                //---- aboutMenuItem ----
                aboutMenuItem.setText("About");
                aboutMenuItem.addActionListener(e -> aboutMenuItemActionPerformed(e));
                helpMenu.add(aboutMenuItem);
            }
            menuBar1.add(helpMenu);

            //---- hSpacer1 ----
            hSpacer1.setMaximumSize(new Dimension(430, 32767));
            menuBar1.add(hSpacer1);

            //---- progressBar ----
            progressBar.setMaximumSize(new Dimension(150, 20));
            progressBar.setMinimumSize(new Dimension(150, 20));
            progressBar.setPreferredSize(new Dimension(150, 20));
            progressBar.setFocusable(false);
            progressBar.setRequestFocusEnabled(false);
            menuBar1.add(progressBar);
        }
        setJMenuBar(menuBar1);

        //======== panel1 ========
        {
            panel1.setLayout(new MigLayout(
                "insets 0,hidemode 3",
                // columns
                "[fill]0" +
                "[fill]0" +
                "[fill]",
                // rows
                "[fill]"));

            //======== scrollPane3 ========
            {

                //---- lineTextArea ----
                lineTextArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
                lineTextArea.setEnabled(false);
                lineTextArea.setEditable(false);
                lineTextArea.setBorder(null);
                lineTextArea.setBackground(Color.white);
                lineTextArea.setForeground(new Color(153, 153, 153));
                scrollPane3.setViewportView(lineTextArea);
            }
            panel1.add(scrollPane3, "cell 0 0,width 40:40:40");

            //======== scrollPane1 ========
            {

                //---- inputTextPane ----
                inputTextPane.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
                inputTextPane.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
                inputTextPane.setBorder(null);
                scrollPane1.setViewportView(inputTextPane);
            }
            panel1.add(scrollPane1, "cell 1 0,width 400:400:400,height 600:600:600");

            //======== scrollPane2 ========
            {

                //---- outputTextPane ----
                outputTextPane.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
                outputTextPane.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
                outputTextPane.setBorder(null);
                scrollPane2.setViewportView(outputTextPane);
            }
            panel1.add(scrollPane2, "cell 2 0,width 460:460:460,height 600:600:600");
        }
        contentPane.add(panel1);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JMenuBar menuBar1;
    private JMenu fileMenu;
    private JMenuItem newMenuItem;
    private JMenuItem openMenuItem;
    private JMenuItem saveMenuItem;
    private JMenuItem saveAsMenuItem;
    private JMenu runMenu;
    private JMenuItem LexMenuItem;
    private JMenuItem synMenuItem;
    private JMenuItem semMenuItem;
    private JMenuItem sem2MenuItem;
    private JMenu buildMenu;
    private JMenuItem buildMenuItem;
    private JMenuItem compileMenuItem;
    private JMenuItem uploadMenuItem;
    private JMenu buildMidiMenu;
    private JMenuItem generateMidiMenuItem;
    private JMenuItem playMenuItem;
    private JMenu helpMenu;
    private JMenuItem demoMenuItem;
    private JMenuItem aboutMenuItem;
    private JPanel hSpacer1;
    private JProgressBar progressBar;
    private JPanel panel1;
    private JScrollPane scrollPane3;
    private JTextArea lineTextArea;
    private JScrollPane scrollPane1;
    private JTextPane inputTextPane;
    private JScrollPane scrollPane2;
    private JTextPane outputTextPane;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
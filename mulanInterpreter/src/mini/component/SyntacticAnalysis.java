package mini.component;

import mini.entity.Node;
import mini.entity.Token;

import java.util.ArrayList;

public class SyntacticAnalysis {

    private ArrayList<Token> tokens;
    private Node AbstractSyntaxTree;
    private int index;
    private boolean sentenceError;
    private ArrayList<Integer> errorList;

    //Start
    public Node Parse(ArrayList<Token> tokens) {
        index = 0;
        errorList = new ArrayList<>();
        this.tokens = tokens;
        AbstractSyntaxTree = new Node("root");

        boolean hadPlay = false;
        for(index = 0;index<tokens.size();index++){
            if(tokens.get(index).getSyn()==6){
                hadPlay = true;
                //break;
            }
            if(hadPlay&&tokens.get(index).getSyn()==8){
                if(index+1<tokens.size()){
                    errorList.add(tokens.get(index+1).getCount());
                    AbstractSyntaxTree.addChild(new Node("Error","Line:" + tokens.get(index+1).getCount() + "  乐谱请写再play语句之前！"));
                    return  AbstractSyntaxTree;
                }
                break;
            }
        }
        if(!hadPlay){
            errorList.add(0);
            AbstractSyntaxTree.addChild(new  Node("Error","缺少play函数！"));
            return  AbstractSyntaxTree;
        }

        index = 0;

        while (index<tokens.size() && tokens.get(index).getSyn() != 6) {
            Node paragraph = parseParagraph();
            AbstractSyntaxTree.addChild(paragraph);
        }


        if (index<tokens.size()) {
            Node execution = parseExecution();
            AbstractSyntaxTree.addChild(execution);
        }

//        if (index<tokens.size()) {
//            errorList.add(tokens.get(index).getCount());
//            AbstractSyntaxTree.addChild(new Node("Error","Line:" + tokens.get(index).getCount() + "  乐谱请写再play语句之前！"));
//        }

        return AbstractSyntaxTree;
    }

    //paragraph -> 'paragraph' identifier speed tone { sentence } 'end'
    public Node parseParagraph() {
        Node paragraph = new Node("score");
        Node terminalNode;

        //statement
        Node statement = new Node("statement");

        //paragraph
        if(tokens.get(index).getSyn()!=2){
            nextLine();
            errorList.add(tokens.get(index - 1).getCount());
            return new Node("Error", "Line: " + tokens.get(index - 1).getCount() + "  未知符号，请检查是否缺少paragraph声明");
        }
        index++;


        //identifier(段落名)
        if (tokens.get(index).getSyn() != 100) {
            if (!isAttributeIdentifier() && !isMelodyElement())
                nextLine();
            statement.addChild(new Node("Error", "Line: " + tokens.get(index - 1).getCount() + "  缺少标识符"));
            errorList.add(tokens.get(index - 1).getCount());
        } else {
            terminalNode = new Node("identifier", tokens.get(index).getContent(), tokens.get(index).getCount());
            statement.addChild(terminalNode);
            index++;
        }
        paragraph.addChild(statement);


        int tempSyn = tokens.get(index).getSyn();
        boolean hadSpeed = false, hadTone = false, hadInstrument = false, hadVolume = false;
        while (!hadReadToEnd() && !paragraphHadEnd() && !isMelodyElement()) {
            //提前遇到paragraph或play
            if(tempSyn == 2 | tempSyn == 6){
                errorList.add(tokens.get(index).getCount());
                paragraph.addChild(new Node("Error", "Line: " + tokens.get(index).getCount() + "  缺少end标识"));
                return paragraph;
            }
            switch (tempSyn) {
                case 3:
                    //speed
                    if (hadSpeed) {
                        nextLine();
                        errorList.add(tokens.get(index - 1).getCount());
                        paragraph.addChild(new Node("Error", "Line: " + tokens.get(index - 1).getCount() + "  重复定义速度"));
                        break;
                    }
                    Node speed = parseSpeed();
                    paragraph.addChild(speed);
                    hadSpeed = true;
                    break;
                case 4:
                    //tone
                    if (hadTone) {
                        nextLine();
                        errorList.add(tokens.get(index - 1).getCount());
                        paragraph.addChild(new Node("Error", "Line: " + tokens.get(index - 1).getCount() + "  重复定义调"));
                        break;
                    }
                    Node tone = parseTone();
                    paragraph.addChild(tone);
                    hadTone = true;
                    break;
                case 20:
                    //instrument
                    if (hadInstrument) {
                        nextLine();
                        errorList.add(tokens.get(index - 1).getCount());
                        paragraph.addChild(new Node("Error", "Line: " + tokens.get(index - 1).getCount() + "  重复定义乐器"));
                        break;
                    }
                    Node instrument = parseInstrument();
                    paragraph.addChild(instrument);
                    hadInstrument = true;
                    break;
                case 21:
                    //volume
                    if (hadVolume) {
                        nextLine();
                        errorList.add(tokens.get(index - 1).getCount());
                        paragraph.addChild(new Node("Error", "Line: " + tokens.get(index - 1).getCount() + "  重复定义强度"));
                        break;
                    }
                    Node volume = parseVolume();
                    paragraph.addChild(volume);
                    hadVolume = true;
                    break;
                default:
                    nextLine();
                    errorList.add(tokens.get(index-1).getCount());
                    paragraph.addChild(new Node("Error","Line: " + tokens.get(index - 1).getCount() + "  未知标识符"));
                    break;
            }

            tempSyn = tokens.get(index).getSyn();
        }
        if (!hadSpeed) {
            Node speed = parseSpeed();
            paragraph.addChild(speed);
        }
        if (!hadTone) {
            Node tone = parseTone();
            paragraph.addChild(tone);
        }
        if (!hadInstrument) {
            Node instrument = parseInstrument();
            paragraph.addChild(instrument);
        }
        if (!hadVolume) {
            Node volume = parseVolume();
            paragraph.addChild(volume);
        }


        //{ sentence }
        while (!hadReadToEnd() && (tokens.get(index).getSyn() != 5)) {
            //没遇到end就遇到play或paragraph
            if (tokens.get(index).getSyn() == 6 | tokens.get(index).getSyn() == 2) {
                paragraph.addChild(new Node("Error", "Line: " + tokens.get(index - 1).getCount() + "  缺少end标识"));
                errorList.add(tokens.get(index - 1).getCount());
                return paragraph;
            }

            sentenceError = false;

            Node sentence = parseSentence();
            paragraph.addChild(sentence);
        }

        //'end',因为上一步sentence判断遇到end才会跳出，所以这一步肯定是end
        terminalNode = new Node("end paragraph", "end", tokens.get(index).getCount());
        paragraph.addChild(terminalNode);
        index++;

        return paragraph;
    }

    //instument
    public Node parseInstrument() {
        Node instrument = new Node("instrument");
        Node terminalNode;
        if (tokens.get(index).getSyn() != 20) {
            //乐器编号
            terminalNode = new Node("instrumentValue", "0");
            instrument.addChild(terminalNode);

            return instrument;
        }

        index++;
        //乐器编号
        terminalNode = new Node("instrumentValue", tokens.get(index).getContent(),tokens.get(index).getCount());
        instrument.addChild(terminalNode);
        index++;

        return instrument;
    }

    public Node parseVolume() {
        Node volume = new Node("volume");
        Node terminalNode;
        if (tokens.get(index).getSyn() != 21) {
            //乐器编号
            terminalNode = new Node("volumeValue", "127");
            volume.addChild(terminalNode);

            return volume;
        }

        index++;
        //乐器编号
        terminalNode = new Node("volumeValue", tokens.get(index).getContent(),tokens.get(index).getCount());
        volume.addChild(terminalNode);
        index++;

        return volume;
    }


    //speed -> 'speed=' speedNum
    public Node parseSpeed() {
        Node speed = new Node("speed");
        Node terminalNode;

        //若当前token不为速度标识，设置默认速度
        if (tokens.get(index).getSyn() != 3) {
            terminalNode = new Node("speedValue", "90", tokens.get(index).getCount());
            speed.addChild(terminalNode);

            return speed;
        }

        //否则，获取速度数值
        terminalNode = new Node("speedValue", tokens.get(++index).getContent(),tokens.get(index).getCount());
        speed.addChild(terminalNode);
        index++;

        return speed;
    }

    //tone -> ([#|b] toneValue)|toneValue
    public Node parseTone() {
        //若当前token不为调性标识，设置默认调性
        if (tokens.get(index).getSyn() != 4) {
            return getTone();
        }

        //否则，获取调性
        Node tone = new Node("tonality");
        Node terminalNode;
        index++;

        //#|b
        if (tokens.get(index).getSyn() == 18 | tokens.get(index).getSyn() == 19) {
            terminalNode = new Node("lift mark", tokens.get(index).getContent(), tokens.get(index).getCount());
            tone.addChild(terminalNode);
            index++;
        }

        //tone value
        if (tokens.get(index).getSyn() != 95) {
            nextLine();
            errorList.add(tokens.get(index - 1).getCount());
            return new Node("Error", "Line: " + tokens.get(index - 1).getCount() + "  调号不正确");
        }
        terminalNode = new Node("tone value", tokens.get(index).getContent());
        tone.addChild(terminalNode);
        index++;

        return tone;
    }

    //sentence -> melody rhythm
    public Node parseSentence() {
        Node sentence = new Node("sentence");

        //melody
        Node melody = parseMelody();
        sentence.addChild(melody);
        if (sentenceError) {
            return sentence;
        }

        //rhythm
        Node rhythm = parseRhythm();
        sentence.addChild(rhythm);

        return sentence;
    }

    //melody -> { NotesInEight }
    public Node parseMelody() {
        Node melody = new Node("melody");

        int group = 0;
        int updown = 0;
        boolean doubleMeantime = true;

        while (index<tokens.size()&&(tokens.get(index).getSyn() != 13)) {
            //'(',低八度左括号
            if (tokens.get(index).getSyn() == 7) {
                if (group > 0) {
                    nextLine();
                    sentenceError = true;
                    errorList.add(tokens.get(index - 1).getCount());
                    return new Node("Error", "Line: " + tokens.get(index - 1).getCount() + "  八度转换错误");
                }
                group--;
                melody.addChild(new Node("lower left parentheses", "(", tokens.get(index).getCount()));
                index++;
                continue;
            }
            //')',低八度右括号
            if (tokens.get(index).getSyn() == 8) {
                if (group >= 0) {
                    nextLine();
                    sentenceError = true;
                    errorList.add(tokens.get(index - 1).getCount());
                    return new Node("Error", "Line: " + tokens.get(index - 1).getCount() + "  八度转换错误");
                }
                if (tokens.get(index - 1).getSyn() == 7) {
                    nextLine();
                    sentenceError = true;
                    errorList.add(tokens.get(index - 1).getCount());
                    return new Node("Error", "Line: " + tokens.get(index - 1).getCount() + "  括号内不能为空");
                }
                group++;
                melody.addChild(new Node("lower right parentheses", ")", tokens.get(index).getCount()));
                index++;
                continue;
            }
            //'['，高八度左括号
            if (tokens.get(index).getSyn() == 9) {
                if (group < 0) {
                    nextLine();
                    sentenceError = true;
                    errorList.add(tokens.get(index - 1).getCount());
                    return new Node("Error", "Line: " + tokens.get(index - 1).getCount() + "  八度转换错误");
                }
                group++;
                melody.addChild(new Node("higher left parentheses", "[", tokens.get(index).getCount()));
                index++;
                continue;
            }
            //']',高八度右括号
            if (tokens.get(index).getSyn() == 10) {
                if (group <= 0) {
                    nextLine();
                    sentenceError = true;
                    errorList.add(tokens.get(index - 1).getCount());
                    return new Node("Error", "Line: " + tokens.get(index - 1).getCount() + "  八度转换错误");
                }
                if (tokens.get(index - 1).getSyn() == 9) {
                    nextLine();
                    sentenceError = true;
                    errorList.add(tokens.get(index - 1).getCount());
                    return new Node("Error", "Line: " + tokens.get(index - 1).getCount() + "  括号内不能为空");
                }
                group--;
                melody.addChild(new Node("higher right parentheses", "]", tokens.get(index).getCount()));
                index++;
                continue;
            }

            //同时音符号
            if(tokens.get(index).getSyn() == 22){
                doubleMeantime = !doubleMeantime;
                if(doubleMeantime && (tokens.get(index-1).getSyn() == 22 || tokens.get(index-2).getSyn() == 22)){
                    nextLine();
                    sentenceError = true;
                    errorList.add(tokens.get(index - 1).getCount());
                    return new Node("Error", "Line: " + tokens.get(index - 1).getCount() + "  无意义同时音符号");
                }
                melody.addChild(new Node("meantime symbol", "|", tokens.get(index).getCount()));
                index++;
                continue;
            }

            //音符
            Node note = parseNotes();
            if (sentenceError) {
                errorList.add(tokens.get(index - 1).getCount());
                return new Node("Error", note.getContent());
            }

            melody.addChild(note);

        }//end while
        if (group != 0) {
            sentenceError = true;
            //isError = true;
            errorList.add(tokens.get(index - 1).getCount());
            nextLine();
            return new Node("Error", "Line: " + (tokens.get(index - 1).getCount()) + " 八度转换错误");
        }
        if(!doubleMeantime){
            sentenceError = true;
            //isError = true;
            errorList.add(tokens.get(index - 1).getCount());
            nextLine();
            return new Node("Error", "Line: " + (tokens.get(index - 1).getCount()) + " 同时音符号数量不正确");
        }


        if(tokens.get(index-1).getSyn()==18|tokens.get(index-1).getSyn()==19){
            sentenceError = true;
            //isError = true;
            errorList.add(tokens.get(index - 1).getCount());
            nextLine();
            return new Node("Error", "Line: " + (tokens.get(index - 1).getCount()) + " 升降号后面没有音符");
        }

        return melody;
    }

    //NotesInEight -> '(' Notes ')' | '[' Notes ']' | Notes
    public Node parseNotesInEight() {
        Node notesInEight = new Node("NotesInEight");

        switch (tokens.get(index).getSyn()) {
            case 7:
                notesInEight.addChild(new Node("lower left parentheses", "(", tokens.get(index).getCount()));
                index++;

                notesInEight.addChild(parseNotes());
                if (sentenceError)
                    return notesInEight;

                if (tokens.get(index).getSyn() != 8) {
                    nextLine();
                    sentenceError = true;
                    errorList.add(tokens.get(index - 1).getCount());
                    return new Node("Error", "Line: " + tokens.get(index - 1).getCount() + "  缺少右括号");
                }
                notesInEight.addChild(new Node("lower right parentheses", ")", tokens.get(index).getCount()));
                index++;
                break;
            case 9:
                notesInEight.addChild(new Node("lower left parentheses", "[", tokens.get(index).getCount()));
                index++;

                notesInEight.addChild(parseNotes());
                if (sentenceError)
                    return notesInEight;

                if (tokens.get(index).getSyn() != 10) {
                    nextLine();
                    sentenceError = true;
                    errorList.add(tokens.get(index - 1).getCount());
                    return new Node("Error", "Line: " + tokens.get(index - 1).getCount() + "  缺少右括号");
                }
                notesInEight.addChild(new Node("lower right parentheses", "]", tokens.get(index).getCount()));
                index++;
                break;
            default:
                notesInEight.addChild(parseNotes());
        }

        return notesInEight;
    }

    //Notes -> ([#|b] notesValue) | notesValue | 0
    public Node parseNotes() {
        Node notes;

        if (tokens.get(index).getSyn() == 2|tokens.get(index).getSyn() == 5|tokens.get(index).getSyn() == 6) {
            sentenceError = true;
            errorList.add(tokens.get(index - 1).getCount());
            return new Node("Error", "Line: " + tokens.get(index - 1).getCount() + "  缺少节奏");
        }

        //'0',休止符
        if (tokens.get(index).getSyn() == 94) {
            notes = new Node("Notes", "0", tokens.get(index).getCount());
            index++;
            return notes;
        }

        //#
        if (tokens.get(index).getSyn() == 18) {
            if(tokens.get(index-1).getSyn() == 19){
                nextLine();
                sentenceError = true;
                errorList.add(tokens.get(index - 1).getCount());
                return new Node("Error", "Line: " + tokens.get(index - 1).getCount() + "  降号后面紧跟升号");
            }
            notes = new Node("lift mark", tokens.get(index).getContent(), tokens.get(index).getCount());
            index++;

            return notes;
        }

        //b
        if (tokens.get(index).getSyn() == 19) {
            if(tokens.get(index-1).getSyn() == 18){
                nextLine();
                sentenceError = true;
                errorList.add(tokens.get(index - 1).getCount());
                return new Node("Error", "Line: " + tokens.get(index - 1).getCount() + "  升号后面紧跟降号");
            }
            notes = new Node("lift mark", tokens.get(index).getContent(), tokens.get(index).getCount());
            index++;

            return notes;
        }

        //notesValue
        if (tokens.get(index).getSyn() != 98) {
            nextLine();
            sentenceError = true;
            errorList.add(tokens.get(index - 1).getCount());
            return new Node("Error", "Line: " + tokens.get(index - 1).getCount() + "  音符不正确");
        }
        notes = new Node("notes value", tokens.get(index).getContent(), tokens.get(index).getCount());
        index++;

        return notes;
    }

    //rhythm -> '<' length '>'
    public Node parseRhythm() {
        Node rhythm = new Node("rhythm");
        Node terminalNode;

        //'<'
        if (tokens.get(index).getSyn() != 13) {
            nextLine();
            sentenceError = true;
            errorList.add(tokens.get(index - 1).getCount());
            return new Node("Error", "Line: " + tokens.get(index - 1).getCount() + "  缺少节奏");
        }
        terminalNode = new Node("left Angle brackets", "<", tokens.get(index).getCount());
        rhythm.addChild(terminalNode);
        index++;

        //length
        boolean inCurlyBraces = false;
        while (!hadReadToEnd()&&(tokens.get(index).getSyn() != 14)) {

            //'{'，连音左括号
            if (tokens.get(index).getSyn() == 11) {
                if (inCurlyBraces) {
                    nextLine();
                    sentenceError = true;
                    errorList.add(tokens.get(index - 1).getCount());
                    return new Node("Error", "Line: " + tokens.get(index - 1).getCount() + "  连音括号中出现连音括号");
                }
                inCurlyBraces = true;
                terminalNode = new Node("leftCurlyBrace", "{", tokens.get(index).getCount());
                rhythm.addChild(terminalNode);
                index++;
                continue;
            }

            //'}',连音右括号
            if (tokens.get(index).getSyn() == 12) {
                if (!inCurlyBraces) {
                    nextLine();
                    sentenceError = true;
                    errorList.add(tokens.get(index - 1).getCount());
                    return new Node("Error", "Line: " + tokens.get(index - 1).getCount() + "  缺少连音左括号");
                }
                if (tokens.get(index - 1).getSyn() == 11 | tokens.get(index - 2).getSyn() == 11) {
                    nextLine();
                    sentenceError = true;
                    errorList.add(tokens.get(index - 1).getCount());
                    return new Node("Error", "Line: " + tokens.get(index - 1).getCount() + "  无意义连音括号");
                }
                inCurlyBraces = false;
                terminalNode = new Node("rightCurlyBrace", "}", tokens.get(index).getCount());
                rhythm.addChild(terminalNode);
                index++;
                continue;
            }

            //音符长度
            if (tokens.get(index).getSyn() != 99) {
                nextLine();
                sentenceError = true;
                errorList.add(tokens.get(index - 1).getCount());
                return new Node("Error", "Line: " + tokens.get(index - 1).getCount() + "  节奏格式错误");
            }

            String len = "";
            len += tokens.get(index).getContent();
            index++;

            //附点
            if (tokens.get(index).getSyn() == 15) {
                len += "*";
                index++;
            }
            terminalNode = new Node("length", len, tokens.get(index).getCount());
            rhythm.addChild(terminalNode);
        }
        if (inCurlyBraces) {
            sentenceError = true;
            errorList.add(tokens.get(index - 1).getCount());
            nextLine();
            return new Node("Error", "Line: " + (tokens.get(index - 1).getCount()) + " 连音符号错误");
        }

        //'>'
        if (tokens.get(index).getSyn() != 14) {
            nextLine();
            sentenceError = true;
            errorList.add(tokens.get(index - 1).getCount());
            return new Node("Error", "Line: " + tokens.get(index - 1).getCount() + "  缺少右尖括号");
        }
        terminalNode = new Node("left Angle brackets", ">", tokens.get(index).getCount());
        rhythm.addChild(terminalNode);
        index++;

        return rhythm;
    }

    //get default speed if never set
    public Node getSpeed() {
        Node speed = new Node("speed");
        Node terminalNode;

        terminalNode = new Node("speed value", "90", tokens.get(index).getCount());
        speed.addChild(terminalNode);

        return speed;
    }

    //get default tone if never set
    public Node getTone() {
        Node tone = new Node("tonality");
        Node terminalNode;

        terminalNode = new Node("tone value", "C", tokens.get(index).getCount());
        tone.addChild(terminalNode);

        return tone;
    }

    //execution -> play ( playlist )
    public Node parseExecution() {
        Node execution = new Node("execution");

        //play
        if (tokens.get(index).getSyn() != 6) {
            nextLine();
            errorList.add(tokens.get(index - 1).getCount());
            return new Node("Error", "Line: " + tokens.get(index - 1).getCount() + "  缺少Play执行语句");
        }
        index++;

        //leftParentheses,(
        if (tokens.get(index).getSyn() != 7) {
            nextLine();
            errorList.add(tokens.get(index - 1).getCount());
            return new Node("Error", "Line: " + tokens.get(index - 1).getCount() + "  缺少左小括号");
        }
        index++;

        //playlist
        Node playlist = parsePlayList();
        execution.addChild(playlist);

        //rightParentheses,(
        if (tokens.get(index).getSyn() != 8) {
            nextLine();
            errorList.add(tokens.get(index - 1).getCount());
            return new Node("Error", "Line: " + tokens.get(index - 1).getCount() + "  缺少右小括号");
        }
        index++;

        return execution;
    }

    //playlist -> identifier { [&|,] identifier }
    public Node parsePlayList() {
        Node playlist = new Node("playlist");
        Node terminalNode;

        //identifier(段落名)
        if (tokens.get(index).getSyn() != 100) {
            nextLine();
            errorList.add(tokens.get(index - 1).getCount());
            return new Node("Error", "Line: " + tokens.get(index - 1).getCount() + "  缺少标识符");
        }
        terminalNode = new Node("identifier", tokens.get(index).getContent(), tokens.get(index).getCount());
        playlist.addChild(terminalNode);
        index++;

        while (!hadReadToEnd() && (tokens.get(index).getSyn() != 8)) {
            // "&" or ","
            switch (tokens.get(index).getSyn()) {
                case 16:
                    terminalNode = new Node("comma", ",", tokens.get(index).getCount());
                    break;
                case 17:
                    terminalNode = new Node("and", "&", tokens.get(index).getCount());
                    break;
                default:
                    errorList.add(tokens.get(index - 1).getCount());
                    return new Node("Error", "Line: " + tokens.get(index - 1).getCount() + "  缺少逗号或&符号");
            }
            playlist.addChild(terminalNode);
            index++;

            if (tokens.get(index).getSyn() != 100) {
                nextLine();
                errorList.add(tokens.get(index - 1).getCount());
                return new Node("Error", "Line: " + tokens.get(index - 1).getCount() + "  缺少标识符");
            }
            terminalNode = new Node("identifier", tokens.get(index).getContent(), tokens.get(index).getCount());
            playlist.addChild(terminalNode);
            index++;
        }

        return playlist;
    }


    //判断当前token是否为段落属性的标识
    public boolean isAttributeIdentifier(){
        int syn = tokens.get(index).getSyn();
        return syn == 3 | syn == 4 | syn == 20 | syn == 21;
    }

    //判断当前token是否为旋律部分的元素
    public boolean isMelodyElement(){
        int syn = tokens.get(index).getSyn();
        return (syn >= 7 && syn <= 10 ) | syn == 18 | syn == 19 | syn == 22 | syn == 94 | syn == 98;
    }

    //判断当前token是否为节奏部分的元素
    public boolean isRhythmElement(){
        int syn = tokens.get(index).getSyn();
        return (syn >= 11 && syn <= 15 ) | syn == 99;
    }

    //判断段落是否已结束
    public boolean paragraphHadEnd(){
        return tokens.get(index).getSyn() == 5;
    }

    //判断是否已经读到末尾token
    public boolean hadReadToEnd(){
        return !(index < tokens.size());
    }

    //换到下一行
    public void nextLine() {
        while (index < tokens.size() - 1 && tokens.get(index).getCount() == tokens.get(++index).getCount()) {
        }
        if(index ==  tokens.size() - 1){
            while (tokens.get(index).getCount() == tokens.get(--index).getCount()) {
            }
            index++;
        }
    }

    public String getErrors(Node curNode) {
        String errorsInfo = "";

        if (curNode.getType().equals("Error"))
            errorsInfo += curNode.getContent() + "\n";

        for (Node child : curNode.getChildren()) {
            errorsInfo += getErrors(child);
        }
        return errorsInfo;
    }

    public boolean getIsError() {
        return !errorList.isEmpty();
    }

    public ArrayList<Integer> getErrorList() {
        return errorList;
    }

}
import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Created by aarikan on 18/10/15.
 */
public class Main {
    static HashSet<String> stopWords = new HashSet<String>();
    static HashMap<Integer, String> articles = new HashMap<Integer, String>();
    static HashMap<String, String> invertedIndex = new HashMap<String, String>();
    static HashSet<Character> lettersAndDigits = new HashSet<Character>();

    public static void main(String[] args) throws IOException {
        System.out.println("Hello, World!");
        //readStopwords();
        readArticles();

    }

    /*
    reads the stopwords from stopwords.txt and fills the stopWords HashSet.
     */
    public static void readStopwords() throws IOException {
        BufferedReader read = new BufferedReader(new FileReader(new File("stopwords.txt")));
        String str;
        while ((str = read.readLine()) != null) {
            stopWords.add(str);
            System.out.println(str);
        }
        read.close();
    }

    /*
    reads all the articles in the ruters corpus and fills the articles map with index:article
     */
    public static void readArticles() throws IOException {
        int index = 0;
        for (int i = 0; i < 22; i++) { //for each of the files
            String filepath;
            if (i < 10) filepath = "reuters21578/reut2-00" + i + ".sgm";
            else filepath = "reuters21578/reut2-0" + i + ".sgm";
            BufferedReader read = new BufferedReader(new FileReader(new File(filepath)));
            String str;
            while ((str = read.readLine()) != null) {
                if (str.contains("<TEXT")) {
                    String article = str + " ";
                    String fileLine;
                    while ((fileLine = read.readLine()) != null) {
                        article += fileLine + " ";
                        if (fileLine.contains("</TEXT>")) {
                            if(article.indexOf("</BODY") == -1) {
                                System.out.println(article);
                            }
                            //articles.put(index, article.substring(article.indexOf("<BODY") + 6, article.indexOf("</BODY")));
                            index++;
                            break;
                        }
                    }
                }
            }
            read.close();
        }
    }

    /*
    takes the input, makes all lowercased.
    gets rid of the words containing all nonword characters.
    gets rid of the nonword characters at the beginning of a word.
    gets rid of the nonword characters at the end of a word.
    gets rid of any nonword character in a word (excluding digits) // 19.2 or 16,3 will pass, whereas It's okay will be its okay
     */
    public static String tokenize(String line) {
        String[] prettyTokens = line.toLowerCase().split(" "); // split the lowercase string by whitespace
        StringBuilder bigBuilder = new StringBuilder();
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < prettyTokens.length; i++) {
            String active = prettyTokens[i];

            try {
                double x = Double.parseDouble(active);
                bigBuilder.append(active);
                bigBuilder.append(" ");
            } catch (Exception e) {  // so this is not a double value
                String activeTemp = active.replace(',', '.');
                try {
                    double x = Double.parseDouble(activeTemp);
                    bigBuilder.append(activeTemp);
                    bigBuilder.append(" ");

                } catch (Exception e1) {

                    for (int j = 0; j < active.length(); j++) {
                        if (Character.isDigit(active.charAt(j)) || Character.isLetter(active.charAt(j))) {
                            stringBuilder.append(active.charAt(j));
                        }
                    }
                    bigBuilder.append(stringBuilder.toString());
                    bigBuilder.append(" ");
                    stringBuilder.setLength(0);
                }
            }
        }
        return bigBuilder.toString();

    }


}

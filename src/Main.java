import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by aarikan on 18/10/15.
 */
public class Main {
    static HashSet<String> stopWords = new HashSet<String>();
//    static HashMap<Integer, String> articles = new HashMap<Integer, String>();
    static HashMap<String, String> invertedIndex = new HashMap<String, String>();
    static HashMap<String, Integer> wordFrequency = new HashMap<String, Integer>();

    public static void main(String[] args) throws IOException {
        System.out.println("Hello, World!");
        readStopwords();
        readArticles();
    //    System.out.println("Articles: " + articles.size());
        System.out.println("invertedIndex: " + invertedIndex.size());
        System.out.println("wordFrequency: " + wordFrequency.size());
        System.out.println("actual: " + invertedIndex.get(stemmed(deleteStopWords(tokenize("World"))).split(" ")[0]));
        System.out.println("result: " + processAnd("World"));
        System.out.println("Goodbye, Cruel World!");
    }

    /*
    reads the stopwords from stopwords.txt and fills the stopWords HashSet.
     */
    public static void readStopwords() throws IOException {
        BufferedReader read = new BufferedReader(new FileReader(new File("stopwords.txt")));
        String str;
        while ((str = read.readLine()) != null) {
            stopWords.add(str);
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
                if (str.contains("<REUTERS")) {
                    String article = str + " ";
                    String tobeIndexed = "";
                    String fileLine;
                    while ((fileLine = read.readLine()) != null) {
                        article += fileLine + " ";
                        if (fileLine.contains("</REUTERS>")) { // end of the article
                            if (article.contains("</BODY>")) { // article has a body tag, normal case
                                //there's no case where an article has a body tag but not a title tag
                                tobeIndexed = article.substring(article.indexOf("<TITLE>") + 7, article.indexOf("</TITLE>")) + " ";
                                tobeIndexed += article.substring(article.indexOf("<BODY>") + 6, article.indexOf("</BODY>"));
                            } else if (article.contains("</TITLE>")) { // article has a title tag, but not a body tag
                                tobeIndexed = article.substring(article.indexOf("<TITLE>") + 7, article.indexOf("</TITLE>"));
                            } else { // no body neither title tag
                                tobeIndexed = article.substring(article.indexOf("<TEXT TYPE=\"UNPROC\">&#2;") + 24, article.indexOf("</TEXT>"));
                            }
                            String[] tokens = stemmed(deleteStopWords(tokenize(tobeIndexed))).split(" ");
                            for (int j = 0; j < tokens.length; j++) {
                                if(!tokens[j].equals("") && !tokens[j].equals(" ") && tokens[j].length() > 1) {
                                    if (invertedIndex.keySet().contains(tokens[j])) { // we've seen this token before.
                                        int newFreq = wordFrequency.get(tokens[j]) + 1;
                                        wordFrequency.put(tokens[j], newFreq);
                                        if(!invertedIndex.get(tokens[j]).contains(""+index)){ // we don't want to have the same file id appearing more than once in the postings list.
                                            invertedIndex.put(tokens[j], invertedIndex.get(tokens[j]) + "," + index);
                                        }
                                    }
                                    else { // first time we're seeing this token
                                        invertedIndex.put(tokens[j], "" + index);
                                        wordFrequency.put(tokens[j], 1);
                                    }
                                }
                            }
                       //     articles.put(index, article);
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
    gets rid of any nonword character in a word (excluding digits) // 19.2 or 16,3 will pass, whereas "It's okay" will be "its okay"
     */
    public static String tokenize(String line) {
        String[] prettyTokens = line.toLowerCase().split(" "); // split the lowercase string by whitespace
        StringBuilder bigBuilder = new StringBuilder();
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < prettyTokens.length; i++) {
            String active = prettyTokens[i];
            if(active.length() > 0) { // parseDouble thinks "1980." a double. avoid that.
                if(!Character.isDigit(active.charAt(active.length()-1)) && !Character.isLetter(active.charAt(active.length()-1))){
                    active = active.substring(0, active.length()-1);
                }
            }
            try {
                double x = Double.parseDouble(active);
                bigBuilder.append(active);
                bigBuilder.append(" ");
            } catch (Exception e) {  // so this is not a double value
                String activeTemp = active.replace(',', '.'); //try again
                try {
                    double x = Double.parseDouble(activeTemp);
                    bigBuilder.append(activeTemp);
                    bigBuilder.append(" ");

                } catch (Exception e1) {
                    if (active.contains("&lt;")) { // handle words including this
                        active = active.substring(0, active.indexOf("&lt;")) + active.substring(active.indexOf("&lt;") + 3);
                    }
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

    /*
    deletes the stop words from the given string.
     */
    public static String deleteStopWords(String article) {
        String[] tokens = article.split(" ");
        String newArticle = "";
        for (int i = 0; i < tokens.length; i++) {
            if (!stopWords.contains(tokens[i])) {
                newArticle += tokens[i] + " ";
            }
        }
        return newArticle;
    }

    /*
    takes a line as an argument, returns the stemmed version.
     */
    public static String stemmed(String str) {
        Stemmer stemmer = new Stemmer();
        String[] words = str.split(" ");
        String result = "";
        for (int j = 0; j < words.length; j++) {
            for (int i = 0; i < words[j].length(); i++) {
                stemmer.add(words[j].charAt(i));
            }
            stemmer.stem();
            result += stemmer.toString() + " ";
        }
        return result;
    }

    public static String processAnd(String userQuery){
        String[] query = stemmed(deleteStopWords(tokenize(userQuery))).split(" ");
        for (int i=0; i<query.length; i++) {

            if(query.length == 1 && invertedIndex.get(query[i]) != null){
                System.out.println("XD: " + query[i]);
                return invertedIndex.get(query[i]); // only 1 word query
            }

        }
        // no match found
        return "No match were found!\n";
    }
}

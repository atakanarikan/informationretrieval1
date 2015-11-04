import java.io.*;
import java.util.*;

/**
 * Created by aarikan on 18/10/15.
 */
public class Main {
    static HashSet<String> stopWords = new HashSet<String>();
    static HashMap<String, String> invertedIndex = new HashMap<String, String>();
    static HashMap<String, Integer> wordFrequency = new HashMap<String, Integer>();

    public static void main(String[] args) throws IOException {
        run();
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
                                if (!tokens[j].equals("") && !tokens[j].equals(" ") && tokens[j].length() > 1) {
                                    if (invertedIndex.keySet().contains(tokens[j])) { // we've seen this token before.
                                        if (!invertedIndex.get(tokens[j]).contains("" + index)) { // we don't want to have the same file id appearing more than once in the postings list.
                                            invertedIndex.put(tokens[j], invertedIndex.get(tokens[j]) + "," + index);
                                            int newFreq = wordFrequency.get(tokens[j]) + 1;
                                            wordFrequency.put(tokens[j], newFreq);
                                        }
                                    } else { // first time we're seeing this token
                                        invertedIndex.put(tokens[j], "" + index);
                                        wordFrequency.put(tokens[j], 1);
                                    }
                                }
                            }
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
            if (active.length() > 0) { // parseDouble thinks "1980." a double. avoid that.
                if (!Character.isDigit(active.charAt(active.length() - 1)) && !Character.isLetter(active.charAt(active.length() - 1))) {
                    active = active.substring(0, active.length() - 1);
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

    /*
    processes the and query given by the user.
    there are 3 cases: "A", "A AND B", "A AND B AND C AND ..."
    for the 3rd case, first processes "A AND B" then "result(A, B) AND C" and so forth.
     */
    public static String processAnd(String userQuery) {
        if (!correctQuery(userQuery)) { // there's an unknown word in the query, there's no point in processing.
            return "None!";
        }
        String[] query = stemmed(deleteStopWords(tokenize(userQuery))).split(" "); // this will get rid of the AND's in the query
        String result;
        int index = 0;
        if (query.length == 1) {
            if (invertedIndex.get(query[index]) != null) return invertedIndex.get(query[index]); // only 1 word query
            return "None!";
        } else if (query.length == 2) {
            result = and2Postings(invertedIndex.get(query[0]).split(","), invertedIndex.get(query[1]).split(","));
            if (result.equals("$$$$EMPTYRESULT$$$$")) {
                return "None!";
            }
        } else {
            String temp = "";
            for (int i = 0; i < query.length - 1; i++) {
                if (i == 0) {
                    temp = and2Postings(invertedIndex.get(query[i]).split(","), invertedIndex.get(query[i + 1]).split(","));
                    if (temp.equals("$$$$EMPTYRESULT$$$$")) { // once a couple returns empty, all the query will return empty
                        return "None!";
                    }
                } else {
                    temp = and2Postings(temp.split(","), invertedIndex.get(query[i + 1]).split(","));
                    if (temp.equals("$$$$EMPTYRESULT$$$$")) { // once a couple returns empty, all the query will return empty
                        return "None!";
                    }
                }
            }
            result = temp;
        }
        return result;
    }

    /*
    postings merge algorithm.
     */
    public static String and2Postings(String[] pl1, String[] pl2) {
        int index1 = 0;
        int index2 = 0;
        String result = "";
        while (index1 < pl1.length && index2 < pl2.length) {
            int val1 = Integer.parseInt(pl1[index1]);
            int val2 = Integer.parseInt(pl2[index2]);
            if (val1 == val2) {
                result += val1 + ",";
                index1++;
                index2++;
            } else if (val1 > val2) {
                index2++;
            } else if (val2 > val1) {
                index1++;
            }
        }
        if (result.length() > 0) {
            return result.substring(0, result.length() - 1); // post-fence
        }
        return "$$$$EMPTYRESULT$$$$";
    }

    public static String processOr(String userQuery) {
        String[] query = stemmed(deleteStopWords(tokenize(userQuery))).split(" "); // this will get rid of the OR's in the query
        String x = "";
        if(!correctQuery(userQuery)) { // an unknown word is in the query, just get rid of it from the query since it won't have any effect
            System.out.println("UNKnOWN");
            for(int i= 0; i < query.length; i++) {
                if(invertedIndex.get(query[i]) != null){
                    x += query[i] + " ";
                } else {
                }
            }
            query = x.split(" ");
        }
        String result;
        if (query.length == 1) {
            if (invertedIndex.get(query[0]) != null) return invertedIndex.get(query[0]); // only 1 word query
            return "None!";
        } else if (query.length == 2) {
            result = or2Postings(invertedIndex.get(query[0]).split(","), invertedIndex.get(query[1]).split(","));
        } else {
            String temp = "";
            for (int i = 0; i < query.length - 1; i++) {
                if (i == 0) {
                    temp = or2Postings(invertedIndex.get(query[i]).split(","), invertedIndex.get(query[i + 1]).split(","));
                } else {
                    temp = or2Postings(temp.split(","), invertedIndex.get(query[i + 1]).split(","));
                }
            }
            result = temp;
        }
        return result;
    }

    public static String or2Postings(String[] pl1, String[] pl2) {
        TreeSet<Integer> mySet = new TreeSet<Integer>();
        String result = "";
        for(int i = 0; i < pl1.length; i++){
            mySet.add(Integer.parseInt(pl1[i]));
        }
        for(int i = 0; i < pl2.length; i++){
            mySet.add(Integer.parseInt(pl2[i]));
        }
        Iterator itr = mySet.iterator();
        while(itr.hasNext()){
            result += itr.next() + ",";
        }
        if(result.length() > 0) return result.substring(0, result.length()-1);
        return "";
    }

    public static boolean correctQuery(String userQuery){
        String[] query = stemmed(deleteStopWords(tokenize(userQuery))).split(" ");
        for(int i = 0; i < query.length; i++) {
            if(!invertedIndex.containsKey(query[i]) || invertedIndex.get(query[i]) == null) {
                return false;
            }
        }
        return true;
    }

    public static void run() throws IOException {
        System.out.println("Hello, World!");
        System.out.print("Processing the stopwords... ");
        readStopwords();
        System.out.println("Done!");
        System.out.print("Processing the articles (This might take up to a minute)... ");
        readArticles();
        System.out.println("Done!");
        Scanner scan = new Scanner(System.in);
        String userQuery;
        while(true){
            System.out.println("Please enter your query(Enter \"q\" for terminating the program) : ");
            userQuery = scan.nextLine();
            if(userQuery.toLowerCase().equals("q")) {
                scan.close();
                System.out.println("Goodbye, Cruel World!");
                System.exit(0);
            }
            String queryType = "and"; // our default query is and
            String[] query = userQuery.split(" ");
            for(int i = 0; i < query.length; i++){
                if(query[i].toLowerCase().equals("or")){
                    queryType = "or";
                    break;
                }
            }
            if(queryType.equals("or")){
                System.out.println("Matched articles: " + processOr(userQuery));
            }
            else {
                System.out.println("Matched articles: " + processAnd(userQuery));
            }
        }
    }
}

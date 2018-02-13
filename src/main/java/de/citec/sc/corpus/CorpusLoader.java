/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.corpus;

import de.citec.sc.index.Language;
import de.citec.sc.syntax.DependencyParse;
import de.citec.sc.syntax.UDPipe;
import de.citec.sc.utils.FileUtil;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author sherzod
 */
public class CorpusLoader {

//    private static String outputDirectoryQALD = CorpusLoader.class.getClassLoader().getResource("qald").getPath();
//    private static String outputDirectoryWebQuestions = CorpusLoader.class.getClassLoader().getResource("webquestions").getPath();
//    private static String outputDirectorySimpleQuestions = CorpusLoader.class.getClassLoader().getResource("simplequestions").getPath();
//
//    private static final String qald6FileTrain = outputDirectoryQALD + "/qald-6-train-multilingual.json";
//    private static final String qald6FileTest = outputDirectoryQALD + "/qald-6-test-multilingual.json";
//    private static final String qaldSubset = outputDirectoryQALD + "/qald_test.xml";
//    private static final String webQuestionsTrain = outputDirectoryWebQuestions + "/WebQuestions.DBpedia.train.json";
//    private static final String webQuestionsTest = outputDirectoryWebQuestions + "/WebQuestions.DBpedia.test.json";
//    private static final String webQuestionsSubset = outputDirectoryWebQuestions + "/WebQuestions.DBpedia.subset.json";
//    private static final String simpleQuestionsTrain = outputDirectorySimpleQuestions + "/annotated_db_data_train.txt";
//    private static final String simpleQuestionsTest = outputDirectorySimpleQuestions + "/annotated_db_data_test.txt";
//    private static final String simpleQuestionsSubset = outputDirectorySimpleQuestions + "/annotated_db_data_subset.txt";
    public enum Dataset {

        qaldSubset, qald6Train, qald6Test, qald7Train, webQuestionsTrain, webQuestionsTest, webQuestionsSubset, simpleQuestionsTrain, simpleQuestionsTest, simpleQuestionsSubset, simpleQuestionsSmall
    }

    /**
     * reads QALD corpus {QALDSubset, QALD4Train, QALD4Test, QALD5Train,
     * QALD5Test} and returns corpus with documents
     *
     * @param dataset
     * @param file
     * @param includeAggregation
     * @param includeUNION
     * @param onlyDBO
     * @param isHybrid
     * @param includeYAGO
     * @return corpus
     */
    public static Set<AnnotatedDocument> load(Dataset dataset, Language.lang lang, boolean withParseTree) {

        String filePath = "";
        Set<AnnotatedDocument> documents = new HashSet<>();
        Set<DataPoint> dataPoints = new HashSet<>();

        switch (dataset.name()) {

//            case "qaldSubset":
//                filePath = qaldSubset;
//                documents = readJSONFile(filePath);
//                break;
//            case "qald6Train":
//                filePath = qald6FileTrain;
//                documents = readJSONFile(filePath);
//                break;
//            case "qald6Test":
//                filePath = qald6FileTest;
//                documents = readJSONFile(filePath);
//                break;
//            case "qald7Train":
//                filePath = qald7FileTrain;
//                documents = readJSONFile(filePath);
//                break;
//            case "webQuestionsTrain":
//                filePath = webQuestionsTrain;
//                documents = readWebQuestionsJSONFile(filePath);
//                break;
//            case "webQuestionsTest":
//                filePath = webQuestionsTest;
//                documents = readWebQuestionsJSONFile(filePath);
//                break;
//            case "webQuestionsSubset":
//                filePath = webQuestionsSubset;
//                documents = readWebQuestionsJSONFile(filePath);
//                break;
            case "simpleQuestionsTrain":
//                filePath = CorpusLoader.class.getClassLoader().getResource("simplequestions").getPath()+"/annotated_db_data_train_replaced.txt";
                filePath = "src/main/resources/simplequestions/annotated_fb_data_train.txt";
                dataPoints = readSimpleQuestions(filePath);
                break;
            case "simpleQuestionsTest":
//                filePath = simpleQuestionsTest;
//                filePath = CorpusLoader.class.getClassLoader().getResource("simplequestions").getPath()+"/annotated_db_data_test_replaced.txt";
                filePath = "src/main/resources/simplequestions/annotated_fb_data_test.txt";
                dataPoints = readSimpleQuestions(filePath);
                break;
            case "simpleQuestionsSubset":
//                filePath = simpleQuestionsSubset;
//                filePath = CorpusLoader.class.getClassLoader().getResource("simplequestions").getPath()+"/annotated_db_data_subset_replaced.txt";
                filePath = "src/main/resources/simplequestions/annotated_fb_data_subset.txt";
                dataPoints = readSimpleQuestions(filePath);
                break;
            case "simpleQuestionsSmall":
//                filePath = simpleQuestionsSubset;
//                filePath = CorpusLoader.class.getClassLoader().getResource("simplequestions").getPath()+"/annotated_db_data_subset_replaced.txt";
                filePath = "src/main/resources/simplequestions/annotated_fb_data_small.txt";
                dataPoints = readSimpleQuestions(filePath);
                break;
            default:
                System.err.println("Corpus not found!");
                System.exit(0);
        }

        System.out.println("Parsing sentences ... \n");

        for (DataPoint d1 : dataPoints) {
            String questionText = d1.getQuestion();

            if (withParseTree) {
                DependencyParse depParse = UDPipe.parse(questionText, lang);
                AnnotatedDocument document = new AnnotatedDocument(depParse, d1);

                documents.add(document);
            } else {
                
                String[] tokens = d1.getQuestion().split("\\s");
                
                Map<Integer, String> tokenMap = new HashMap<>();
                Map<Integer, Integer> nodeRelationMap = new HashMap<>();
                Map<Integer, String> postagMap = new HashMap<>();
                Map<Integer, String> edgeMap = new HashMap<>();
                
                for(int i=0; i<tokens.length; i++){
                    tokenMap.put(i, tokens[i]);
                }
                
                DependencyParse depParse = new DependencyParse(tokenMap, nodeRelationMap, edgeMap, postagMap, 0);
                
                AnnotatedDocument document = new AnnotatedDocument(depParse, d1);

                documents.add(document);
            }
        }

        return documents;
    }

    private static Set<DataPoint> readSimpleQuestions(String filePath) {
        Set<DataPoint> dataPoints = new HashSet<>();

        Set<String> content = FileUtil.readFile(filePath);

        int counter = 0;
        for (String c : content) {

            counter++;
            String[] s = c.split("\t");

            String id = counter + "";
            String question = s[3];
            
            String subject = s[0].replace("www.freebase.com/", "").replace("/", ".");
            String object = s[2].replace("www.freebase.com/", "").replace("/", ".");
            String predicate = s[1].replace("www.freebase.com/", "").replace("/", ".");
            
            String query = "SELECT ?x WHERE {<http://"+subject+"> <http://"+predicate+"> ?x .}";
            Set<String> answers = new HashSet<>();
            answers.add(object);

            DataPoint dataPoint = new DataPoint(question, query, answers, id);

            dataPoints.add(dataPoint);
        }

        return dataPoints;
    }

//    private static List<Question> readJSONFile(String filePath) {
//        ArrayList<Question> qaldQuestions = new ArrayList<>();
//
//        JSONParser parser = new JSONParser();
//
//        try {
//
//            HashMap obj = (HashMap) parser.parse(new FileReader(filePath));
//
//            JSONArray questions = (JSONArray) obj.get("questions");
//            for (int i = 0; i < questions.size(); i++) {
//                HashMap o1 = (HashMap) questions.get(i);
//
//                String hybrid = "", onlyDBO = "", aggregation = "";
//                if (o1.get("hybrid") instanceof Boolean) {
//                    Boolean b = (Boolean) o1.get("hybrid");
//                    hybrid = b.toString();
//                } else {
//                    hybrid = (String) o1.get("hybrid");
//                }
//
//                if (o1.get("onlydbo") instanceof Boolean) {
//                    Boolean b = (Boolean) o1.get("onlydbo");
//                    onlyDBO = b.toString();
//                } else {
//                    onlyDBO = (String) o1.get("onlydbo");
//                }
//
//                if (o1.get("aggregation") instanceof Boolean) {
//                    Boolean b = (Boolean) o1.get("aggregation");
//                    aggregation = b.toString();
//                } else {
//                    aggregation = (String) o1.get("aggregation");
//                }
//
//                String answerType = (String) o1.get("answertype");
//
//                String id = o1.get("id").toString();
//
//                HashMap queryTextObj = (HashMap) o1.get("query");
//                String query = (String) queryTextObj.get("sparql");
//
//                Map<Language, String> questionText = new HashMap<>();
//
//                JSONArray questionTexts = (JSONArray) o1.get("question");
//                for (Object qObject : questionTexts) {
//
//                    JSONObject englishQuestionText = (JSONObject) qObject;
//
//                    if (englishQuestionText.get("language").equals("en")) {
//                        questionText.put(Language.EN, englishQuestionText.get("string").toString());
//
//                    }
//                    if (englishQuestionText.get("language").equals("de")) {
//                        questionText.put(Language.DE, englishQuestionText.get("string").toString());
//
//                    }
//                    if (englishQuestionText.get("language").equals("es")) {
//                        questionText.put(Language.ES, englishQuestionText.get("string").toString());
//                    }
//                }
//
//                if (query != null) {
//                    if (!query.equals("")) {
//
//                        if (query.contains("UNION")) {
//                            query = removeUNION(query);
//                        }
//
//                        query = query.replace("\n", " ");
//
//                        Question q1 = new Question(questionText, query, onlyDBO, aggregation, answerType, hybrid, id);
//                        qaldQuestions.add(q1);
//                    }
//                }
//
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        return qaldQuestions;
//    }
//
//    private static List<Question> readWebQuestionsJSONFile(String filePath) {
//        ArrayList<Question> qaldQuestions = new ArrayList<>();
//
//        JSONParser parser = new JSONParser();
//
//        try {
//
//            HashMap obj = (HashMap) parser.parse(new FileReader(filePath));
//
//            JSONArray questions = (JSONArray) obj.get("questions");
//            for (int i = 0; i < questions.size(); i++) {
//                HashMap o1 = (HashMap) questions.get(i);
//
//                String hybrid = "false", onlyDBO = "true", aggregation = "false", answerType = "resource";
//
//                String question = (String) o1.get("questionText");
//                String query = (String) o1.get("query");
//
//                Map<Language, String> questionText = new HashMap<>();
//                questionText.put(Language.EN, question);
//
//                String id = (i + 1) + "";
//
//                List<String> answers = (List<String>) o1.get("answers");
//
//                Question q1 = new Question(questionText, query, onlyDBO, aggregation, answerType, hybrid, id);
//                q1.setAnswers(answers);
//
//                qaldQuestions.add(q1);
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        return qaldQuestions;
//    }
    private static String removeUNION(String q) {

        while (q.contains("UNION")) {
            String s1 = q.substring(q.indexOf("UNION"));
            String s2 = s1.substring(0, s1.indexOf("}") + 1);

            q = q.replace(s2, "");
            String tail = q.substring(q.lastIndexOf("}") + 1);
            if (!tail.trim().isEmpty()) {
                q = q.replace(tail, " ");
            }

            q = q.replace("{", " ");
            q = q.replace("}", " ");
            q = q.replace("WHERE", "WHERE { ");
            q = q + "}" + tail;
        }

        return q;
    }
}

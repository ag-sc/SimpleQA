/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.test;

import de.citec.sc.corpus.AnnotatedDocument;
import de.citec.sc.corpus.CorpusLoader;
import de.citec.sc.index.Language;
import java.util.Set;
import junit.framework.Assert;
import org.junit.Test;

/**
 *
 * @author sherzod
 */
public class CorpusTest {

    @Test
    public void test() {
        Set<AnnotatedDocument> documents = CorpusLoader.load(CorpusLoader.Dataset.simpleQuestionsSubset, Language.lang.EN, true);

        System.out.println("==========================================================");
        Assert.assertTrue(documents.size() > 0);
    }
}

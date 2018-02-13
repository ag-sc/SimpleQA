/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.template;

import de.citec.sc.corpus.AnnotatedDocument;
import de.citec.sc.variable.State;
import exceptions.UnkownTemplateRequestedException;
import templates.AbstractTemplate;
import templates.TemplateFactory;

/**
 *
 * @author sherzod
 */
public class QATemplateFactory implements TemplateFactory<AnnotatedDocument, State> {



    @Override
    public AbstractTemplate<AnnotatedDocument, State, ?> newInstance(String templateName) throws UnkownTemplateRequestedException, Exception {

        switch (templateName) {
            case "LexicalFeatureTemplate":
                return new LexicalFeatureTemplate();
            case "BaselineTemplate":
                return new BaselineTemplate();
        }

        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}

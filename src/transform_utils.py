import numpy
import re
import random
from nlp_utils import pad, get_padding_shape

predicate_tokens_re = re.compile(r"[._]+", re.UNICODE)


def collectSamplesForTrain(question, conf, res):
    text = question["text"]

    chars = list(text)
    char_indices = res.char_vocabulary.get_indices(chars)

    tokens = text.split(" ")
    token_indices = res.word_embeddings.vocabulary.get_indices(tokens)

    target_subject_uri = question["subject"]

    target_predicate_label = question["predicate"]

    subjects = []
    subject_graph_embeddings = []
    predicate_graph_embeddings = []
    predicates = []
    answer_scores = []
    subject_answer_scores = []
    predicate_answer_scores = []

    answer_pair_list = []

    triple_answer_pair_set = set()
    triple_answer_pair_list = []

    # TRAIN time
    has_correct_answer = False
    correct_subject_ngram = ''
    for i_candidate, candidate in enumerate(question["candidates"]):
        candidate_subject_uri = candidate["uri"]
        candidate_subject_label = candidate["ngram"]
        for candidate_predicate_label in candidate["predicates"]:
            answer_pair = (candidate_subject_uri, candidate_predicate_label)

            if answer_pair == (target_subject_uri, target_predicate_label):
                has_correct_answer = True
                # set the correct subject ngram
                correct_subject_ngram = candidate_subject_label
                break
        if has_correct_answer:
            break

    # collect n negative samples
    if has_correct_answer:
        for i_candidate, candidate in enumerate(question["candidates"]):
            candidate_subject_uri = candidate["uri"]
            candidate_subject_label = candidate["ngram"]

            ### if the model using NER then take samples only from the same span with the correct subject uri
            if conf.use_ner and correct_subject_ngram != candidate_subject_label:
                # skip these ones
                continue

            for candidate_predicate_label in candidate["predicates"]:
                triple_answer_pair = (candidate_subject_uri, candidate_subject_label, candidate_predicate_label)

                # collect negative triples (subject_uri, subject_ngram, predicate_uri)
                if candidate_subject_uri != target_subject_uri or candidate_predicate_label != target_predicate_label:
                    if triple_answer_pair not in triple_answer_pair_set:
                        triple_answer_pair_list.append(triple_answer_pair)
                        triple_answer_pair_set.add(triple_answer_pair)

        # shuffle and take sublist from it
        if len(triple_answer_pair_list) >= conf.negative_sample_size:
            random.shuffle(triple_answer_pair_list)
            triple_answer_pair_list = triple_answer_pair_list[0:conf.negative_sample_size]

        # add negative samples
        for candidate_subject_uri, candidate_subject_label, candidate_predicate_label in triple_answer_pair_list:
            negative_subject_chars = list(candidate_subject_label)
            negative_predicate_tokens = predicate_tokens_re.split(candidate_predicate_label)
            negative_answer_pair = (candidate_subject_uri, candidate_predicate_label)

            # add answer scores for all 3 outputs
            answer_scores.append(0.0)
            if candidate_subject_uri == target_subject_uri:
                subject_answer_scores.append(1.0)
            else:
                subject_answer_scores.append(0.0)
            if candidate_predicate_label == target_predicate_label:
                predicate_answer_scores.append(1.0)
            else:
                predicate_answer_scores.append(0.0)

            subjects.append(negative_subject_chars)
            predicates.append(negative_predicate_tokens)
            answer_pair_list.append(negative_answer_pair)

            # graph embeddings for correct subject
            if conf.use_graph_embeddings:
                subject_graph_embeddings.append(candidate_subject_uri)
                predicate_graph_embeddings.append(candidate_predicate_label)

        # add the positive sample
        positive_subject_chars = list(correct_subject_ngram)
        positive_predicate_tokens = predicate_tokens_re.split(target_predicate_label)
        positive_answer_pair = (target_subject_uri, target_predicate_label)

        # add answer scores for all 3 outputs
        answer_scores.append(1.0)
        subject_answer_scores.append(1.0)
        predicate_answer_scores.append(1.0)

        subjects.append(positive_subject_chars)
        predicates.append(positive_predicate_tokens)
        answer_pair_list.append(positive_answer_pair)

        # graph embeddings for correct subject
        if conf.use_graph_embeddings:
            subject_graph_embeddings.append(target_subject_uri)
            predicate_graph_embeddings.append(target_predicate_label)

            # convert uris to indices
            subject_graph_embeddings = list(res.graph_embeddings.vocabulary.get_indices(subject_graph_embeddings))
            predicate_graph_embeddings = list(res.graph_embeddings.vocabulary.get_indices(predicate_graph_embeddings))

        subjects = [list(res.char_vocabulary.get_indices(subject_chars)) for subject_chars in subjects]
        predicates = [list(res.word_embeddings.vocabulary.get_indices(predicate_tokens)) for predicate_tokens in
                      predicates]

    return char_indices, token_indices, subjects, predicates, answer_scores, answer_pair_list, subject_answer_scores, predicate_answer_scores, subject_graph_embeddings, predicate_graph_embeddings


def collectSamplesForTest(question, conf, res):
    text = question["text"]

    chars = list(text)
    char_indices = res.char_vocabulary.get_indices(chars)

    tokens = text.split(" ")
    token_indices = res.word_embeddings.vocabulary.get_indices(tokens)

    subjects = []
    subject_graph_embeddings = []
    predicate_graph_embeddings = []
    predicates = []
    answer_scores = []
    subject_answer_scores = []
    predicate_answer_scores = []

    answer_pair_list = []

    triple_answer_pair_set = set()
    triple_answer_pair_list = []

    # Test time
    for i_candidate, candidate in enumerate(question["candidates"]):

        ##  PRUNING
        ###   take candidate pairs from only top_k candidates during test
        if i_candidate >= conf.test_top_candidates:
            break

        candidate_subject_uri = candidate["uri"]
        candidate_subject_label = candidate["ngram"]
        for candidate_predicate_label in candidate["predicates"]:
            triple_answer_pair = (candidate_subject_uri, candidate_subject_label, candidate_predicate_label)

            if triple_answer_pair not in triple_answer_pair_set:
                triple_answer_pair_list.append(triple_answer_pair)
                triple_answer_pair_set.add(triple_answer_pair)

    ### ADD RANDOM PAIR TO RETURN STH
    if len(triple_answer_pair_list) == 0:
        triple_answer_pair = ('random_uri', 'random_ngram', 'random_predicate_uri')
        triple_answer_pair_list.append(triple_answer_pair)

    # add samples
    for candidate_subject_uri, candidate_subject_label, candidate_predicate_label in triple_answer_pair_list:
        sample_subject_chars = list(candidate_subject_label)
        sample_predicate_tokens = predicate_tokens_re.split(candidate_predicate_label)
        sample_answer_pair = (candidate_subject_uri, candidate_predicate_label)
        # add answer scores for all 3 outputs
        answer_scores.append(0.0)
        subject_answer_scores.append(0.0)
        predicate_answer_scores.append(0.0)

        subjects.append(sample_subject_chars)
        predicates.append(sample_predicate_tokens)
        answer_pair_list.append(sample_answer_pair)

        # graph embeddings for subject,predicate
        if conf.use_graph_embeddings:
            subject_graph_embeddings.append(candidate_subject_uri)
            predicate_graph_embeddings.append(candidate_predicate_label)

    subjects = [list(res.char_vocabulary.get_indices(subject_chars)) for subject_chars in subjects]
    predicates = [list(res.word_embeddings.vocabulary.get_indices(predicate_tokens)) for predicate_tokens in predicates]
    # convert uris to indices
    if conf.use_graph_embeddings:
        subject_graph_embeddings = list(res.graph_embeddings.vocabulary.get_indices(subject_graph_embeddings))
        predicate_graph_embeddings = list(res.graph_embeddings.vocabulary.get_indices(predicate_graph_embeddings))

    return char_indices, token_indices, subjects, predicates, answer_scores, answer_pair_list, subject_answer_scores, predicate_answer_scores, subject_graph_embeddings, predicate_graph_embeddings


def transform_batch_data_filtered(questions, conf, res, is_test=False, is_Predicate_Model=None):
    char_indices_batch = []
    token_indices_batch = []
    subjects_batch = []
    subject_graph_embeddings_batch = []
    predicate_graph_embeddings_batch = []
    predicates_batch = []
    answer_scores_batch = []
    subject_answer_scores_batch = []
    predicate_answer_scores_batch = []

    questions_batch = []
    answer_pair_batch = []

    for question in questions:

        if is_test:  ## TEST TIME
            char_indices, token_indices, subjects, predicates, answer_scores, answer_pair_list, subject_answer_scores, predicate_answer_scores, subject_graph_embeddings, predicate_graph_embeddings = collectSamplesForTest(
                question, conf, res)

        else:
            # TRAIN time
            char_indices, token_indices, subjects, predicates, answer_scores, answer_pair_list, subject_answer_scores, predicate_answer_scores, subject_graph_embeddings, predicate_graph_embeddings = collectSamplesForTrain(
                question, conf, res)

            ### SKIP QUESTIONS THAT DON'T HAVE CORRECT PAIR
            if len(answer_pair_list) == 0:
                continue

        ### INPUTS
        char_indices_batch.append(char_indices)
        token_indices_batch.append(token_indices)
        subjects_batch.append(subjects)
        predicates_batch.append(predicates)
        ### OUTPUTS
        answer_scores_batch.append(answer_scores)
        subject_answer_scores_batch.append(subject_answer_scores)
        predicate_answer_scores_batch.append(predicate_answer_scores)

        ### GRAPH EMBEDDINGS ###
        if conf.use_graph_embeddings:
            subject_graph_embeddings_batch.append(subject_graph_embeddings)
            predicate_graph_embeddings_batch.append(predicate_graph_embeddings)

        questions_batch.append(question)
        answer_pair_batch.append(answer_pair_list)

    char_indices_batch = pad(char_indices_batch, conf.padding_position, res.char_vocabulary.padding_index)
    token_indices_batch = pad(token_indices_batch, conf.padding_position, res.word_embeddings.vocabulary.padding_index)

    print(get_padding_shape(subjects_batch))
    subjects_batch = pad(subjects_batch, conf.padding_position, res.char_vocabulary.padding_index)
    predicates_batch = pad(predicates_batch, conf.padding_position, res.word_embeddings.vocabulary.padding_index)
    answer_scores_batch = pad(answer_scores_batch, conf.padding_position, 0.0)
    subject_answer_scores_batch = pad(subject_answer_scores_batch, conf.padding_position, 0.0)
    predicate_answer_scores_batch = pad(predicate_answer_scores_batch, conf.padding_position, 0.0)

    char_indices_batch = numpy.array(char_indices_batch)
    token_indices_batch = numpy.array(token_indices_batch)
    subjects_batch = numpy.array(subjects_batch)
    predicates_batch = numpy.array(predicates_batch)
    answer_scores_batch = numpy.array(answer_scores_batch)
    subject_answer_scores_batch = numpy.array(subject_answer_scores_batch)
    predicate_answer_scores_batch = numpy.array(predicate_answer_scores_batch)

    # pad and convert graph embeddings
    if conf.use_graph_embeddings:
        subject_graph_embeddings_batch = pad(subject_graph_embeddings_batch, conf.padding_position,
                                             res.graph_embeddings.vocabulary.padding_index)
        subject_graph_embeddings_batch = numpy.array(subject_graph_embeddings_batch)

        predicate_graph_embeddings_batch = pad(predicate_graph_embeddings_batch, conf.padding_position,
                                               res.graph_embeddings.vocabulary.padding_index)
        predicate_graph_embeddings_batch = numpy.array(predicate_graph_embeddings_batch)

    input = {}
    input["question_char_input"] = char_indices_batch
    input["question_token_input"] = token_indices_batch
    input["candidate_subject_labels_input"] = subjects_batch
    input["candidate_predicate_labels_input"] = predicates_batch

    # candidate_subject_graph_embedding_input
    if conf.use_graph_embeddings:
        input["candidate_subject_graph_embeddings_input"] = subject_graph_embeddings_batch
        input["candidate_predicate_graph_embeddings_input"] = predicate_graph_embeddings_batch

    output = {}
    output["answer_scores"] = answer_scores_batch

    if conf.use_predicate_and_subject_outputs:
        output["subject_answer_scores"] = subject_answer_scores_batch
        output["predicate_answer_scores"] = predicate_answer_scores_batch

    question_data = {}
    question_data["questions"] = questions_batch
    question_data["answer_pairs"] = answer_pair_batch

    if is_test:
        return question_data, input, output
    else:
        return input, output


def transform_batch_data_predicate_model(questions, conf, res, is_test=False):
    char_indices_batch = []
    token_indices_batch = []
    position_indices_batch = []
    predicate_graph_embeddings_batch = []

    answer_scores_batch = []
    questions_batch = []

    for question in questions:
        char_indices, token_indices, answers, position_indices, predicate_graph_embeddings = collectSamplesForPredicateModel(
            question, conf, res, is_test)

        ### happens during training when no correct subject entity
        if len(position_indices) == 0:
            continue

        ### repeat the input (question string etc.) multiple times
        if conf.predicate_model_type == "predict_as_binary":
            ## loop over each predicate_embedding
            for index, predicate_graph_embedding in enumerate(predicate_graph_embeddings):
                char_indices_batch.append(char_indices)
                token_indices_batch.append(token_indices)
                position_indices_batch.append(position_indices)
                ## add [] in to cast the embeddings as list
                predicate_graph_embeddings_batch.append([predicate_graph_embedding])

                answer_scores_batch.append(answers[index])
        else:
            ### INPUTS
            char_indices_batch.append(char_indices)
            token_indices_batch.append(token_indices)
            position_indices_batch.append(position_indices)

            ### OUTPUTS
            answer_scores_batch.append(answers)

        questions_batch.append(question)

    char_indices_batch = pad(char_indices_batch, conf.padding_position, res.char_vocabulary.padding_index)
    token_indices_batch = pad(token_indices_batch, conf.padding_position, res.word_embeddings.vocabulary.padding_index)


    if conf.padding_position == "pre":
        position_indices_batch = pad(position_indices_batch, conf.padding_position, 0)
    else:
        position_indices_batch = pad(position_indices_batch, conf.padding_position,
                                     conf.subject_position_max_distance * 2)

    char_indices_batch = numpy.array(char_indices_batch)
    token_indices_batch = numpy.array(token_indices_batch)
    position_indices_batch = numpy.array(position_indices_batch)
    answer_scores_batch = numpy.array(answer_scores_batch)

    input = {}
    input["question_char_input"] = char_indices_batch
    input["question_token_input"] = token_indices_batch

    ### add if any predicate embeddings needed to be passed as input to the respective model
    if len(predicate_graph_embeddings_batch) > 0:
        predicate_graph_embeddings_batch = numpy.array(predicate_graph_embeddings_batch)
        input["predicate_graph_embedding_input"] = predicate_graph_embeddings_batch

    input["relative_position_input"] = position_indices_batch

    output = {}
    output["answer_scores"] = answer_scores_batch

    question_data = {}
    question_data["questions"] = questions_batch

    if is_test:
        return question_data, input, output
    else:
        return input, output


def collectSamplesForPredicateModel(question, conf, res, is_test=False):
    text = question["text"]  # type:str

    char_indices = list()
    tokens = text.split(" ")

    position_indices = list()

    answers = list()
    predicate_graph_embeddings = list()

    for token in tokens:
        token_char_indices = res.char_vocabulary.get_indices(token)
        char_indices.append(token_char_indices)

    token_indices = res.word_embeddings.vocabulary.get_indices(tokens)
    target_predicate_label = question["predicate"]
    target_subject_uri = question["subject"]

    # TRAIN time
    if not is_test:
        has_correct_answer = False

        subject_start_index = len(tokens) - 2
        subject_end_index = len(tokens) - 1

        for i_candidate, candidate in enumerate(question["candidates"]):
            candidate_subject_uri = candidate["uri"]
            for candidate_predicate_label in candidate["predicates"]:
                answer_pair = (candidate_subject_uri, candidate_predicate_label)
                if answer_pair == (target_subject_uri, target_predicate_label):
                    has_correct_answer = True
                    # set the correct subject spans
                    subject_start_index = candidate["startToken"]
                    subject_end_index = candidate["endToken"]
                    break
            if has_correct_answer:
                break

        if has_correct_answer:
            ### relative distances to the subject entity
            position_indices = relative_distance_to_subject(tokens, subject_start_index, subject_end_index, conf)

            # when the model_type is binary classification
            # if conf.predicate_model_type == "predict_as_binary":
            #     answers,

    if is_test:
        subject_start_index = len(tokens) - 2
        subject_end_index = len(tokens) - 1


        for i_candidate, candidate in enumerate(question["candidates"]):
            subject_start_index = candidate["startToken"]
            subject_end_index = candidate["endToken"]
            break

        # relative position distances
        position_indices = relative_distance_to_subject(tokens, subject_start_index, subject_end_index, conf)

        # return all candidate predicates from that subject span
        # if conf.predicate_model_type == "predict_as_binary":


    if conf.predicate_model_type == "predict_all_predicates":
        # index of predicate
        answers = res.predicate_vocabulary.to_one_hot(target_predicate_label)
    elif conf.predicate_model_type == "predict_graph_embedding":
        # index of predicate embedding
        answers = res.graph_embeddings.get_vector(target_predicate_label)
    elif conf.predicate_model_type == "predict_as_binary":
        ### answers contains 0 or 1 for each predicate
        ### repeat the char_indices for each predicate_embedding that is returned
        answers, predicate_graph_embeddings= get_input_predicate_embeddings(question, res, target_subject_uri, target_predicate_label, is_test)

    return char_indices, token_indices, answers, position_indices, predicate_graph_embeddings


def relative_distance_to_subject(tokens, subject_start_index, subject_end_index, conf):
    position_indices = list()
    for position in range(len(tokens)):

        if position < subject_start_index:
            position_indices.append(max(position - subject_start_index, -1 * conf.subject_position_max_distance))
        elif position >= subject_end_index:
            position_indices.append(min(position - subject_end_index, -1 * conf.subject_position_max_distance))
        else:
            position_indices.append(0)

    position_indices = [p + conf.subject_position_max_distance for p in position_indices]

    return position_indices

def get_input_predicate_embeddings(question, res, target_subject_uri, target_predicate_label, is_test=False):
    answers = list()
    predicate_graph_embeddings = list()

    if is_test:
        all_candidate_predicates = set()

        for i_candidate, candidate in enumerate(question["candidates"]):
            candidate_predicates = candidate["predicates"]
            for p in candidate_predicates:
                all_candidate_predicates.add(p)

        predicate_graph_embeddings = list(res.graph_embeddings.vocabulary.get_indices(all_candidate_predicates))

        ## add zeros
        answers = [0] * len(all_candidate_predicates)

    else:
        for i_candidate, candidate in enumerate(question["candidates"]):
            candidate_subject_uri = candidate["uri"]
            if candidate_subject_uri != target_subject_uri:
                continue

            candidate_predicates = candidate["predicates"]
            predicate_graph_embeddings = list(
                res.graph_embeddings.vocabulary.get_indices(candidate_predicates))

            # binary classification
            for p in candidate_predicates:
                if p == target_predicate_label:
                    answers.append(1)
                else:
                    answers.append(0)
            break

    return answers, predicate_graph_embeddings
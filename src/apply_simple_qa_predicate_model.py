import json
import operator
import os
import random
from collections import defaultdict

import numpy
import os.path
from scipy import spatial

import models
from nlp_utils import BetterDict, Configuration, print_batch_shapes, BatchIterator
from transform_utils import transform_batch_data_predicate_model
from utils import load_questions, load_resources

__author__ = 'sjebbara'


def main(questions_filepath, conf):
    # conf = Configuration.load(config_filepath)
    # conf.padding_position = "pre"

    print("Configuration:")
    print(conf.description())

    questions = load_questions(questions_filepath)

    if conf.max_questions < len(questions):
        questions = random.sample(questions, conf.max_questions)

    # if conf.max_questions:
    #     questions = random.sample(questions, conf.max_questions)

    print("#Questions:", len(questions))

    res = load_resources(conf)

    print("Word Vocabulary", res.word_embeddings.vocabulary)
    print("Char Vocabulary", res.char_vocabulary)
    print("Entity Vocabulary", res.entity_vocabulary)
    print("Relation Vocabulary", res.relation_vocabulary)

    print("build model ...")
    ranking_model = None
    ranking_model = models.simple_joint_qa_predicate_model(conf, res)
    ranking_model.load_weights(os.path.join(conf.model_dirpath, "best_answer_ranking_model.hdf5"))
    ranking_model.summary()

    # ranking_model = None

    print('Starting predicting ...\n')
    output_filepath = os.path.join(conf.model_dirpath, "predicate_predicted_answers.txt")
    recall_output_filepath = os.path.join(conf.model_dirpath, "predicate_prediction_summary.txt")
    predicted_data = predict(questions, ranking_model, conf, res)

    recall_ranges = [1, 3, 5, 10, 20, 50]

    print('Computing recall values and writing predictions to the model path')
    compute_recall_and_write_predictions(predicted_data, output_filepath, recall_output_filepath, recall_ranges, conf,
                                         res)

    # print('Writing prediction summary to the model path')  #  # recall_values = recall_at_n(predicted_data, , conf, res)  # write_prediction_summary(recall_values, recall_output_filepath, conf)


def predict(questions, ranking_model, conf, res):
    question_data, input_data, output_data = transform_batch_data_predicate_model(questions, conf, res, is_test=True)
    print_batch_shapes(input_data)

    predicted_data = BetterDict(input_data)
    predicted_data.update(question_data)

    predicted_answer_scores = ranking_model.predict(input_data, batch_size=conf.batch_size)
    predicted_data.predicted_answer_scores = predicted_answer_scores

    return predicted_data


def compute_recall_and_write_predictions(predicted_data, prediction_output_filepath, recall_output_filepath, n_values,
                                         conf, res):
    instances = BatchIterator([predicted_data])

    predictions_file_writer = open(prediction_output_filepath, "w")
    recall_file_writer = open(recall_output_filepath, "w")

    instances = BatchIterator([predicted_data])
    descending_n_values = numpy.sort(n_values)[::-1]
    highest_n = descending_n_values[0]
    recall = defaultdict(float)
    n_questions = 0

    for instance in instances:
        n_questions += 1
        question = instance.questions
        target_predicate = question["predicate"]

        predicted_answer_scores = instance.predicted_answer_scores

        # get top-k predicted predicates -> top-k = highest_n
        predicted_predicates = top_predicted_predicates(predicted_answer_scores, res, conf, highest_n)

        for n in descending_n_values:
            top_pairs_at_n = set(predicted_predicates[:n])
            is_found = target_predicate in top_pairs_at_n

            recall[str(n)] += float(is_found)

        # save top-10 predicted predicates to the file
        question["predicted_predicates"] = predicted_predicates[:10]

        # remove the predicates detected during linking
        question.pop('predicates', None)

        # save each instance as a line
        predictions_file_writer.write(json.dumps(question) + "\n")

    for n in descending_n_values:
        recall[str(n)] /= n_questions
        print("Recall@{}:\t{:.3f}".format(n, recall[str(n)]))

    # save recall values to the file
    recall_file_writer.write(json.dumps({"recall": recall}))


def top_predicted_predicates(predicted_answer_scores, res, conf, top_k):
    if conf.predicate_model_type == "predict_graph_embedding":
        ### find the predicate that has the closest cos-similarity to the predicted_answer_scores
        ## get top 10
        predicted_predicates = closest_predicates(res, predicted_answer_scores, top_k)

    elif conf.predicate_model_type == "predict_all_predicates":

        sorted_predicate_indices = numpy.argsort(predicted_answer_scores)[::-1]
        predicted_predicates = [res.predicate_vocabulary.get_word(predicted_index) for predicted_index in
                                sorted_predicate_indices]
        ### get top 10
        predicted_predicates = predicted_predicates[:top_k]
    else:
        predicted_predicates = "asd"

    return predicted_predicates


def closest_predicates(res, predicted_answer_scores, top_k):
    scores = {}
    for predicate in res.graph_embeddings.vocabulary.words():

        # subject entities start with m. -> e.g. m.0fkvn
        if predicate.startswith("m."):
            continue
        predicate_vector = res.graph_embeddings.get_vector(predicate)
        similarity = 1 - spatial.distance.cosine(predicted_answer_scores, predicate_vector)
        scores[predicate] = similarity

    # sort predicates by similarity score
    sorted_predicates_tuples = sorted(scores.items(), key=operator.itemgetter(1), reverse=True)

    predicted_predicates = list()

    for predicate, score in sorted_predicates_tuples[:top_k]:
        predicted_predicates.append(predicate)

    return predicted_predicates


def write_prediction_summary(predicted_data: defaultdict, output_filepath: str, conf: Configuration):
    with open(output_filepath, "w") as f:
        f.write(json.dumps({
            "recall": predicted_data}))  # for p in predicted_data.keys():  #     f.write(str(p)+'\t'+ str(predicted_data[p])+"\n")


def recall_at_n(predicted_data, n_values, conf, res):
    instances = BatchIterator([predicted_data])
    descending_n_values = numpy.sort(n_values)[::-1]
    highest_n = descending_n_values[0]

    recall = defaultdict(float)
    n_questions = 0
    for instance in instances:
        n_questions += 1
        question = instance.questions
        target_predicate = question["predicate"]

        predicted_answer_scores = instance.predicted_answer_scores

        # get top-k predicted predicates -> top-k = highest_n
        predicted_predicates = top_predicted_predicates(predicted_answer_scores, res, conf, highest_n)

        for n in descending_n_values:
            top_pairs_at_n = set(predicted_predicates[:n])
            is_found = target_predicate in top_pairs_at_n

            recall[str(n)] += float(is_found)

    # with open(scores_filepath, "w") as f:
    #     scores = {"recall": recall}
    #     f.write(json.dumps(scores))

    for n in descending_n_values:
        recall[str(n)] /= n_questions
        print("Recall@{}:\t{:.3f}".format(n, recall[str(n)]))

    return recall


if __name__ == "__main__":

    ## RUN SCRIPT

    test_filepath = "../res/test.txt"

    for f in os.listdir("../results"):

        # skip directories without proper experiment name
        if not f.startswith("simpleQA"):
            continue

        conf_filepath = "../results/" + f + "/configuration.json"
        best_model_path = "../results/" + f + "/best_answer_ranking_model.hdf5"

        # skip experiment dirs without model files
        if not (os.path.exists(conf_filepath) and os.path.exists(best_model_path)):
            continue

        conf = Configuration.load(conf_filepath)

        # skip experiments without this name
        if conf.task_name != "simpleQA_predicate_model":
            continue

        print('Running model in: ' + f + ' with test_file:' + test_filepath)
        main(test_filepath, conf)

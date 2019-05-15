import json
import os
from collections import defaultdict
import random

import numpy

import models
from nlp_utils import BetterDict, Configuration, print_batch_shapes, BatchIterator, remove_padding
from transform_utils import transform_batch_data_filtered
from utils import load_questions, load_resources, get_data_batches, get_batches

from keras import backend as K
import tensorflow as tf

__author__ = 'sjebbara'


def main(questions_filepath, experiment_dir):
    conf_filepath = experiment_dir + "/configuration.json"
    conf = Configuration.load(conf_filepath)

    if conf.use_ner:
        print("Using NER questions!")
        questions_filepath = "../res/test_after_ner.txt"
        # take top 7 candidate entities
        conf.test_top_candidates = 7

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
    ranking_model = models.simple_joint_qa(conf, res)
    ranking_model.load_weights(os.path.join(conf.model_dirpath, "best_answer_ranking_model.hdf5"))
    ranking_model.summary()

    # ranking_model = None

    print('Starting predicting ...\n')
    output_filepath = os.path.join(conf.model_dirpath, "predicted_answers.txt")
    predicted_data_batches = predict(questions, ranking_model, conf, res)
    print('Writing predictions to the model path')
    write_prediction(predicted_data_batches, output_filepath, conf)

    print('Writing prediction summary to the model path')
    recall_output_filepath = os.path.join(conf.model_dirpath, "prediction_summary.txt")
    recall_values = recall_at_n(predicted_data_batches, [1, 3, 5, 10, 20, 50], conf)
    write_prediction_summary(recall_values, recall_output_filepath, conf)


def predict(questions, ranking_model, conf, res):
    # question_data, input_data, output_data = transform_batch_data_filtered(questions, conf, res, is_test=True)
    # print_batch_shapes(input_data)

    batches = get_batches(questions, 10)

    predicted_data_batches = []
    for question_batch in batches:
        question_data, input_data, output_data = transform_batch_data_filtered(question_batch, conf, res, is_test=True)
        predicted_data = BetterDict(input_data)
        predicted_data.update(question_data)
        print_batch_shapes(input_data)

        if conf.use_predicate_and_subject_outputs:
            predicted_answer_scores, predicted_subject_scores, predicted_predicate_scores = ranking_model.predict(
                input_data, batch_size=conf.batch_size)
            predicted_data.predicted_answer_scores = predicted_answer_scores
            predicted_data.predicted_subject_scores = predicted_subject_scores
            predicted_data.predicted_predicate_scores = predicted_predicate_scores

        else:
            predicted_answer_scores = ranking_model.predict(input_data, batch_size=conf.batch_size)

            predicted_data.predicted_answer_scores = predicted_answer_scores

        predicted_data_batches.append(predicted_data)

    return predicted_data_batches


def write_prediction(predicted_data_batches, output_filepath, conf):
    instances = BatchIterator(predicted_data_batches)

    with open(output_filepath, "w") as f:
        for instance in instances:
            question = instance.questions

            padded_answer_scores = instance.predicted_answer_scores
            answer_scores = remove_padding(padded_answer_scores, len(instance.answer_pairs),
                                           padding_position=conf.padding_position)
            predicted_answer_index = numpy.argmax(answer_scores)
            predicted_subject_uri, predicted_predicate = instance.answer_pairs[predicted_answer_index]

            question["predicted_subject"] = predicted_subject_uri
            question["predicted_predicate"] = predicted_predicate
            f.write(json.dumps(question) + "\n")


def write_prediction_summary(predicted_data: defaultdict, output_filepath: str, conf: Configuration):
    with open(output_filepath, "w") as f:
        f.write(json.dumps({
            "recall": predicted_data}))  # for p in predicted_data.keys():  #     f.write(str(p)+'\t'+ str(predicted_data[p])+"\n")


def recall_at_n(predicted_data_batches, n_values, conf):
    instances = BatchIterator(predicted_data_batches)
    descending_n_values = numpy.sort(n_values)[::-1]
    highest_n = descending_n_values[0]

    recall = defaultdict(float)
    n_questions = 0
    for instance in instances:
        n_questions += 1
        question = instance.questions
        target_subject_uri = question["subject"]
        target_predicate = question["predicate"]

        padded_answer_scores = instance.predicted_answer_scores
        answer_scores = remove_padding(padded_answer_scores, len(instance.answer_pairs),
                                       padding_position=conf.padding_position)
        predicted_answer_indices = numpy.argsort(answer_scores)[::-1]

        top_predicted_pairs = [instance.answer_pairs[i] for i in predicted_answer_indices[:highest_n]]
        for n in descending_n_values:
            top_pairs_at_n = set(top_predicted_pairs[:n])
            is_found = (target_subject_uri, target_predicate) in top_pairs_at_n

            recall[str(n)] += float(is_found)

    # with open(scores_filepath, "w") as f:
    #     scores = {"recall": recall}
    #     f.write(json.dumps(scores))

    for n in descending_n_values:
        recall[str(n)] /= n_questions
        print("Recall@{}:\t{:.3f}".format(n, recall[str(n)]))

    return recall


# if __name__ == "__main__":
#
#     ## RUN SCRIPT
#
#     test_filepath = "../res/test.txt"
#
#     for f in os.listdir("../results"):
#         conf_filepath = "../results/" + f + "/configuration.json"
#
#         conf = Configuration.load(conf_filepath)
#         if conf.use_ner:
#             test_filepath = "../res/test_after_ner.txt"
#             #take top 7 candidate entities
#             conf.test_top_candidates = 7
#
#         print('Running model in: ' + f + ' with test_file:' + test_filepath)
#         main(test_filepath, conf)
#


def _filter(experiment_dir):
    conf_filepath = experiment_dir + "/configuration.json"
    conf = Configuration.load(conf_filepath)

    # skip experiments without this name
    if conf.task_name != "simpleQA":
        return False
    else:
        return True


if __name__ == "__main__":

    ## RUN SCRIPT

    test_filepath = "../res/test.txt"

    # experiment_dirs = ["../results/" + f  for f in os.listdir("../results")]
    # experiment_dirs = filter(_filter, experiment_dirs)

    experiment_dirs = []
    experiment_dirs.append("/home/sjebbara/git/SimpleJQA/results/simpleQA_freebase_2018-03-21_10:08:40")

    for experiment_dir in experiment_dirs:
        print('Running model in: ' + experiment_dir + ' with test_file:' + test_filepath)
        main(test_filepath, experiment_dir)

import json
import random
import re
import time

import keras
import numpy

from nlp_utils import BetterDict, Embedding
from nlp_utils import Vocabulary
from transform_utils import transform_batch_data_filtered,transform_batch_data_predicate_model




class TimeHistory(keras.callbacks.Callback):
    def on_train_begin(self, logs={}):
        self.training_time_start = time.time()

    def on_epoch_begin(self, batch, logs={}):
        self.epoch_time_start = time.time()

    def on_epoch_end(self, batch, logs={}):
        epoch_seconds = time.time() - self.epoch_time_start
        training_seconds = time.time() - self.training_time_start

        eh, em, es = self._get_h_m_s(epoch_seconds)
        th, tm, ts = self._get_h_m_s(training_seconds)

        print("--- Epoch Time: {}h {} m {}s ({}) ---".format(eh, em, es, epoch_seconds))
        print("--- Total Time: {}h {} m {}s ({}) ---".format(th, tm, ts, training_seconds))

    def _get_h_m_s(self, seconds):
        em, es = divmod(seconds, 60)
        eh, em = divmod(em, 60)

        eh = int(eh)
        em = int(em)
        es = int(es)
        return eh, em, es

#
# def print_statistics(questions):
#     n_text_char = []
#     n_text_tokens = []
#     n_subject_char = []
#     n_predicate_tokens = []
#     n_candidates = []
#     n_predicates = []
#     n_target_predicates = []
#
#     n_impossible = []
#
#     for question in questions:
#         text = question["text"]
#         n_text_char.append(len(text))
#
#         tokens = text.split(" ")
#         n_text_tokens.append(len(tokens))
#
#         target_subject_uri = question["subject"]
#
#         target_predicate_label = question["predicate"]
#         target_predicate_tokens = predicate_tokens_re.split(target_predicate_label)
#
#         n_predicate_tokens.append(len(target_predicate_tokens))
#         n_candidates.append(len(question["candidates"]))
#
#         has_correct_answer = False
#         for candidate in question["candidates"]:
#             candidate_subject_uri = candidate["uri"]
#             candidate_subject_label = candidate["ngram"]
#
#             if candidate_subject_uri == target_subject_uri:
#                 n_target_predicates.append(len(candidate["predicates"]))
#
#             n_subject_char.append(len(candidate_subject_label))
#             n_predicates.append(len(candidate["predicates"]))
#
#             for candidate_predicate_label in candidate["predicates"]:
#                 candidate_predicate_tokens = predicate_tokens_re.split(candidate_predicate_label)
#                 n_predicate_tokens.append(len(candidate_predicate_tokens))
#
#                 if target_subject_uri == candidate_subject_uri and target_predicate_label == candidate_predicate_label:
#                     has_correct_answer = True
#
#         if not has_correct_answer:
#             n_impossible.append(1.)
#
#     def _print_stats(name, counts):
#         print(name, numpy.min(counts), numpy.max(counts), numpy.mean(counts))
#
#     print("#Questions:", len(questions))
#     _print_stats("n_text_char", n_text_char)
#     _print_stats("n_text_tokens", n_text_tokens)
#     _print_stats("n_candidates", n_candidates)
#     _print_stats("n_predicates", n_predicates)
#     _print_stats("n_subject_char", n_subject_char)
#     _print_stats("n_predicate_tokens", n_predicate_tokens)
#     _print_stats("n_target_predicates", n_target_predicates)
#     print("n_impossible {}/{} = {}".format(numpy.sum(n_impossible), len(questions),
#                                            numpy.sum(n_impossible) / len(questions)))
#

def load_questions(filepath):
    with open(filepath) as f:
        questions = [json.loads(line) for line in f]
    return questions


def get_batches(iterable, batch_size):
    batch = []
    for element in iterable:
        batch.append(element)

        current_batch_size = len(batch)
        if current_batch_size >= batch_size:
            yield batch
            batch = []

    if len(batch) > 0:
        yield batch


def get_data_batches(questions, conf, res):
    n_batches = int(numpy.ceil(float(len(questions)) / conf.batch_size))

    e = 0
    while True:
        # print("Create batches for epoch {}/{}".format(e + 1, conf.n_epochs))
        #random.shuffle(questions)
        #sort by characters
        questions = sorted(questions, key=lambda q: len(q["text"]))

        batches = get_batches(questions, conf.batch_size)
        for i, batch in enumerate(batches):
            # print("Create batch {}/{}".format(i + 1, n_batches))
            input_data, output_data = transform_batch_data_filtered(batch, conf, res)
            # print("Input shapes:")
            # LearningTools.print_batch_shapes(input_data)

            # print("Output shapes:")
            # LearningTools.print_batch_shapes(output_data)
            yield input_data, output_data
        e += 1

def get_data_batches_predicate_model(questions, conf, res):
    n_batches = int(numpy.ceil(float(len(questions)) / conf.batch_size))

    e = 0
    while True:
        # print("Create batches for epoch {}/{}".format(e + 1, conf.n_epochs))
        #random.shuffle(questions)
        #sort by characters
        questions = sorted(questions, key=lambda q: len(q["text"]))

        batches = get_batches(questions, conf.batch_size)
        for i, batch in enumerate(batches):
            # print("Create batch {}/{}".format(i + 1, n_batches))
            input_data, output_data = transform_batch_data_predicate_model(batch, conf, res)
            # print("Input shapes:")
            # LearningTools.print_batch_shapes(input_data)

            # print("Output shapes:")
            # LearningTools.print_batch_shapes(output_data)
            yield input_data, output_data
        e += 1

def load_word_embeddings(conf):
    embeddings = Embedding()
    embeddings.load_plain_text_file(conf.word_embedding_path, top_k=conf.top_k_vocab,
                                    has_header=True)
    embeddings.add("<pad>", 0, vector_init="zeros")
    embeddings.vocabulary.set_padding(0)
    embeddings.add("<unk>", 1, vector_init="mean")
    embeddings.vocabulary.set_unknown(1)
    return embeddings

def load_graph_embeddings(conf):
    embeddings = Embedding()
    ## based on prefix
    filePath = "../res/freebase_embedding_model_filtered_"+conf.graph_embedding_prefix+".vec"
    embeddings.load_plain_text_file(filePath)
    embeddings.add("<pad>", 0, vector_init="zeros")
    embeddings.vocabulary.set_padding(0)
    embeddings.add("<unk>", 1, vector_init="mean")
    embeddings.vocabulary.set_unknown(1)
    return embeddings


def load_char_vocab(conf):
    vocabulary = Vocabulary()
    charset = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789,.;:!?-_\"\'\\/()[]{}<>=+*$%&@#"
    vocabulary.init_from_word_list(charset)
    vocabulary.add_padding("<0>", 0)
    vocabulary.add_unknown("<?>", 1)
    return vocabulary

def load_predicates():
    predicates = list()

    filepath = '../res/trainPredicates.txt'
    with open(filepath, 'r', encoding='utf-8') as f:
        for i, line in enumerate(f):
            predicates.append(line.strip())

    vocabulary = Vocabulary()
    vocabulary.init_from_word_list(predicates)
    vocabulary.add_padding("<0>", 0)
    vocabulary.add_unknown("<?>", 1)
    return vocabulary

def load_resources(conf):
    res = BetterDict()

    word_embeddings = load_word_embeddings(conf)
    char_vocabulary = load_char_vocab(conf)

    res.word_embeddings = word_embeddings
    res.char_vocabulary = char_vocabulary

    conf.word_vocab_size = len(word_embeddings.vocabulary)
    conf.char_vocab_size = len(char_vocabulary)

    predicates = load_predicates()
    res.predicate_vocabulary = predicates
    conf.predicate_vocab_size = len(predicates)

    if conf.use_graph_embeddings:
        print('Loading graph embeddings')
        graph_embeddings = load_graph_embeddings(conf)
        res.graph_embeddings = graph_embeddings
        conf.graph_vocab_size = len(graph_embeddings.vocabulary)


    return res


def extract_entities(questions):
    entities = set()
    for q in questions:
        entities.add(q["subject"])
        for c in q["candidates"]:
            entities.add(c["uri"])
    return entities

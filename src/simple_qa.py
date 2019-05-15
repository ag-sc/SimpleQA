import os
import random
import sys
import traceback

import numpy
from keras.callbacks import ModelCheckpoint, EarlyStopping
from keras.engine import Model

import models
from nlp_utils import print_batch_shapes, get_timestamp, Configuration
from transform_utils import transform_batch_data_filtered
from utils import TimeHistory, load_questions, load_resources, get_data_batches

__author__ = 'sjebbara'


# def get_default_config(base_conf):
#     conf = Configuration()
#     conf.max_questions = 1000
#     conf.top_k_vocab = 10000
#     conf.train_size = 0.8
#
#     conf.batch_size = 10
#     conf.n_epochs = 1
#
#     conf.word_dropout = 0.5
#     conf.dropout = 0.5
#
#     # conf.entity_embedding_size = 100
#     # conf.relation_embedding_size = 100
#     conf.word_embedding_size = 300
#     conf.char_embedding_size = 20
#     conf.match_embedding_size = 100
#     conf.loss_margin = 0.5
#
#     conf.dataset = "freebase"
#     conf.task_name = "simpleQA"
#
#     if base_conf is not None:
#         # override with settings from file
#         for k, v in base_conf.items():
#             conf[k] = v
#     return conf


def _main(conf):
    conf.experiment_name = "{}_{}_{}".format(conf.task_name, conf.dataset, get_timestamp())
    conf.model_dirpath = os.path.join("../results", conf.experiment_name)

    try:
        print("Create model directory:", conf.model_dirpath)
        os.makedirs(conf.model_dirpath)
    except OSError as e:
        print(e)

    # load file paths for training and validation from config file
    train_questions = load_questions(conf.training_file_path)
    valid_questions = load_questions(conf.validation_file_path)

    if conf.max_questions < len(train_questions):
        train_questions = random.sample(train_questions, conf.max_questions)
    if conf.max_questions < len(valid_questions):
        valid_questions = random.sample(valid_questions, conf.max_questions)

    print("#Train file instances: " + str(len(train_questions)))
    print("#Valid file instances: " + str(len(valid_questions)))

    res = load_resources(conf)

    print("Word Vocabulary", res.word_embeddings.vocabulary)
    print("Char Vocabulary", res.char_vocabulary)
    if conf.use_graph_embeddings == 1:
        print("Graph Vocabulary", res.graph_embeddings.vocabulary)
    print("Entity Vocabulary", res.entity_vocabulary)
    print("Relation Vocabulary", res.relation_vocabulary)

    print("Configuration:")
    print(conf.description())

    print("build model ...")
    ranking_model = models.simple_joint_qa(conf, res)

    ranking_model.summary()

    print("Save configuration ...")
    conf.save(os.path.join(conf.model_dirpath, "configuration.json"))

    train(train_questions, valid_questions, ranking_model, conf, res)

    print("Save weights ...")
    ranking_model.save_weights(os.path.join(conf.model_dirpath, "answer_ranking_model.hdf5"), overwrite=True)
    print("Done!")


def get_callbacks(conf):
    callbacks = []
    callbacks.append(TimeHistory())

    ### GET model based on answer_scores, predicate_scores and subject scores
    if conf.use_predicate_and_subject_outputs:

        callbacks.append(ModelCheckpoint(os.path.join(conf.model_dirpath,
                                                      "answer_ranking_model_Epoch:{epoch:02d}_Acc:{val_answer_scores_acc:.2f}.hdf5"),
                                         mode="max", save_weights_only=True, monitor="val_answer_scores_acc"))
        callbacks.append(
            ModelCheckpoint(os.path.join(conf.model_dirpath, "best_subject_ranking_model.hdf5"), mode="max",
                            save_weights_only=True, save_best_only=True, monitor="val_subject_answer_scores_acc"))

        callbacks.append(
            ModelCheckpoint(os.path.join(conf.model_dirpath, "best_predicate_ranking_model.hdf5"), mode="max",
                            save_weights_only=True, save_best_only=True, monitor="val_predicate_answer_scores_acc"))

        callbacks.append(ModelCheckpoint(os.path.join(conf.model_dirpath, "best_answer_ranking_model.hdf5"), mode="max",
                                         save_weights_only=True, save_best_only=True, monitor="val_answer_scores_acc"))

        callbacks.append(EarlyStopping(monitor="val_answer_scores_acc", patience=conf.early_stopping_patience))
    else:
        ### GET model based on answer scores
        callbacks.append(ModelCheckpoint(
            os.path.join(conf.model_dirpath, "answer_ranking_model_Epoch:{epoch:02d}_Acc:{val_acc:.2f}.hdf5"),
            mode="max", save_weights_only=True, monitor="val_acc"))

        callbacks.append(ModelCheckpoint(os.path.join(conf.model_dirpath, "best_answer_ranking_model.hdf5"), mode="max",
                                         save_weights_only=True, save_best_only=True, monitor="val_acc"))

        callbacks.append(EarlyStopping(monitor="val_acc", patience=conf.early_stopping_patience))

    return callbacks


def train(train_questions, valid_questions, ranking_model, conf, res):
    assert isinstance(ranking_model, Model)

    # train_questions, val_questions = train_test_split(questions, train_size=conf.train_size)
    # print("#train data:", len(train_questions))
    # print("#val data:  ", len(val_questions))

    print("Start Training:")
    train_input_data, train_output_data = transform_batch_data_filtered(train_questions, conf, res)
    val_input_data, val_output_data = transform_batch_data_filtered(valid_questions, conf, res)

    print("Train input:")
    print_batch_shapes(train_input_data)
    print("Train output:")
    print_batch_shapes(train_output_data)
    print("Val input:")
    print_batch_shapes(val_input_data)
    print("Val output:")
    print_batch_shapes(val_output_data)

    n_batches = int(numpy.ceil(float(len(train_questions)) / conf.batch_size))
    batch_generator = get_data_batches(train_questions, conf, res)

    print(len(train_questions), conf.batch_size, n_batches, conf.n_epochs)
    ranking_model.fit_generator(batch_generator, steps_per_epoch=n_batches, epochs=conf.n_epochs,
                                validation_data=(val_input_data, val_output_data), verbose=1,
                                callbacks=get_callbacks(conf))

    return ranking_model


def test(questions, ranking_model, conf, res):
    assert isinstance(ranking_model, Model)
    input_data, output_data = transform_batch_data_filtered(questions, conf, res)
    print_batch_shapes(input_data)
    print_batch_shapes(output_data)
    predicted_answer_scores = ranking_model.predict(input_data, batch_size=conf.batch_size)
    return predicted_answer_scores


def write_prediction(input_data, predicted_answer_scores):
    pass


def grid_search(conf):
    negative_sample_size = [50]
    epochs = [100]
    use_graph_embeddings = [True]
    use_predicate_and_subject_outputs = [True, False]
    graph_embedding_prefixes = ['obj']
    use_ners = [False]

    match_embedding_sizes = [100]

    char_kernel_sizes = [3]
    token_kernel_sizes = [3]

    char_cnn_depths = [2]
    token_cnn_depths = [2]

    layer_types = ['cnn', 'rnn']

    answer_loss_weights = [0.6]
    subject_answer_loss_weights = [0.3]
    predicate_answer_loss_weights = [0.1]

    max_questions = 80000
    top_k_words_embeddings = 400000

    ### 300 dimension for word embeddings
    conf.word_embedding_size = 300
    conf.word_embedding_path = "../res/glove.6B.300d.txt"

    for negative_sample_size in negative_sample_size:
        for epoch in epochs:
            for use_graph_embedding in use_graph_embeddings:
                for graph_embedding_prefix in graph_embedding_prefixes:
                    for match_embedding_size in match_embedding_sizes:
                        for char_kernel_size in char_kernel_sizes:
                            for char_cnn_depth in char_cnn_depths:
                                for token_kernel_size in token_kernel_sizes:
                                    for token_cnn_depth in token_cnn_depths:
                                        for layer_type in layer_types:
                                            for use_ner in use_ners:

                                                # set the negative sample, graph embedding values
                                                conf.max_questions = max_questions
                                                conf.top_k_vocab = top_k_words_embeddings
                                                conf.use_predicate_and_subject_outputs = False

                                                conf.negative_sample_size = negative_sample_size
                                                conf.n_epochs = epoch
                                                conf.use_graph_embeddings = use_graph_embedding
                                                conf.graph_embedding_prefix = graph_embedding_prefix
                                                conf.match_embedding_size = match_embedding_size

                                                # kernel and cnn depth
                                                conf.question_char_kernel_size = char_kernel_size
                                                conf.question_char_cnn_depth = char_cnn_depth
                                                conf.question_token_kernel_size = token_kernel_size
                                                conf.question_token_cnn_depth = token_cnn_depth
                                                conf.predicate_token_kernel_size = token_kernel_size
                                                conf.predicate_token_cnn_depth = token_cnn_depth
                                                conf.subject_char_kernel_size = char_kernel_size
                                                conf.subject_char_cnn_depth = char_cnn_depth

                                                ## layer type
                                                conf.layer_type = layer_type
                                                conf.use_ner = use_ner

                                                for use_predicate_and_subject_output in use_predicate_and_subject_outputs:
                                                    if use_predicate_and_subject_output:
                                                        for answer_loss_weight in answer_loss_weights:
                                                            for subject_answer_loss_weight in subject_answer_loss_weights:
                                                                for predicate_answer_loss_weight in predicate_answer_loss_weights:
                                                                    # answer weight losses
                                                                    conf.answer_loss_weight = answer_loss_weight
                                                                    conf.subject_answer_loss_weight = subject_answer_loss_weight
                                                                    conf.predicate_answer_loss_weight = predicate_answer_loss_weight

                                                                    ## set the boolean flag
                                                                    conf.use_predicate_and_subject_outputs = use_predicate_and_subject_output

                                                    # train the model
                                                    main(conf)


def main(conf):
    try:
        _main(conf)
    except (MemoryError, IOError):
        raise

    except Exception as e:
        print("WARNING: Exception when performing experiment:")
        print(traceback.format_exc())


if __name__ == "__main__":
    params = sys.argv[1:]
    if len(params) == 0:
        conf_filepath = "../res/qa_configuration.json"
    else:
        conf_filepath = params[1]

    if conf_filepath is not None:
        conf = Configuration.load(conf_filepath)
    else:
        conf = None

    # grid_search(conf)

    conf.batch_size = 256
    conf.max_questions = 100000
    conf.early_stopping_patience = 15
    main(conf)

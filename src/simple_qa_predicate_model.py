import os
import random
import sys

import numpy
from keras.callbacks import ModelCheckpoint, EarlyStopping
from keras.engine import Model

import models
from nlp_utils import print_batch_shapes, get_timestamp, Configuration
from transform_utils import transform_batch_data_predicate_model
from utils import TimeHistory, load_questions, load_resources, get_data_batches_predicate_model

__author__ = 'sjebbara'


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
    ranking_model = models.simple_joint_qa_predicate_model(conf, res)

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

    ### GET model based on answer scores
    callbacks.append(ModelCheckpoint(os.path.join(conf.model_dirpath,
                                                  "answer_ranking_model_Epoch:{epoch:02d}_Acc:{" + conf.predicate_model_monitor + ":.2f}.hdf5"),
                                     mode=conf.predicate_model_callback_mode, save_weights_only=True,
                                     monitor=conf.predicate_model_monitor))

    callbacks.append(ModelCheckpoint(os.path.join(conf.model_dirpath, "best_answer_ranking_model.hdf5"),
                                     mode=conf.predicate_model_callback_mode, save_weights_only=True,
                                     save_best_only=True, monitor=conf.predicate_model_monitor))

    callbacks.append(EarlyStopping(monitor=conf.predicate_model_monitor, patience=conf.callback_patience))

    return callbacks


def train(train_questions, valid_questions, ranking_model, conf, res):
    assert isinstance(ranking_model, Model)

    # train_questions, val_questions = train_test_split(questions, train_size=conf.train_size)
    # print("#train data:", len(train_questions))
    # print("#val data:  ", len(val_questions))
    print("Val data:")
    val_input_data, val_output_data = transform_batch_data_predicate_model(valid_questions, conf, res)

    print("Val input:")
    print_batch_shapes(val_input_data)
    print("Val output:")
    print_batch_shapes(val_output_data)

    n_batches = int(numpy.ceil(float(len(train_questions)) / conf.batch_size))
    batch_generator = get_data_batches_predicate_model(train_questions, conf, res)

    print(len(train_questions), conf.batch_size, n_batches, conf.n_epochs)
    ranking_model.fit_generator(batch_generator, steps_per_epoch=n_batches, epochs=conf.n_epochs,
                                validation_data=(val_input_data, val_output_data), verbose=1,
                                callbacks=get_callbacks(conf))

    return ranking_model


def write_prediction(input_data, predicted_answer_scores):
    pass


def grid_search(base_conf):
    conf = Configuration(base_conf)
    conf.task_name = "simpleQA_predicate_model"
    conf.word_embedding_size = 300
    conf.word_embedding_path = "../res/glove.6B.300d.txt"
    conf.predicate_embedding_size = 200

    conf.max_questions = 80000
    conf.top_k_vocab = 100000
    conf.n_epochs = 100
    conf.callback_patience = 30
    conf.batch_size = 200
    conf.use_graph_embeddings = False
    conf.use_ner = True

    predicate_encoder_embedding_types = list()
    predicate_encoder_embedding_types.append("word+char+att")
    predicate_encoder_embedding_types.append("word+att")
    predicate_encoder_embedding_types.append("word")

    layer_types = list()
    layer_types.append("cnn")
    layer_types.append("rnn")

    predicate_model_types = list()

    ### model type, monitor, loss, metrics
    predicate_model_types.append(("predict_all_predicates", "val_acc", "categorical_crossentropy", "accuracy", "max"))
    predicate_model_types.append(
        ("predict_graph_embedding", "val_cosine_proximity", "cosine_proximity", "cosine_proximity", "min"))
    predicate_model_types.append(("predict_as_binary", "val_acc", "binary_crossentropy", "accuracy", "max"))

    predicate_embedding_depths = [1, 2, 4]

    for layer_type in layer_types:
        for p in predicate_encoder_embedding_types:
            for model_type, monitor, loss_function, metrics, mode in predicate_model_types:
                for predicate_embedding_depth in predicate_embedding_depths:

                    if model_type == "predict_as_binary" or model_type == "predict_graph_embedding":
                        conf.use_graph_embeddings = True

                    conf.layer_type = layer_type
                    conf.predicate_encoder_embedding_type = p
                    conf.predicate_model_type = model_type

                    conf.predicate_model_monitor = monitor
                    conf.predicate_model_loss_function = loss_function
                    conf.predicate_model_metrics = metrics
                    conf.predicate_model_callback_mode = mode
                    conf.predicate_embedding_depth = predicate_embedding_depth

                    # train
                    _main(conf)


if __name__ == "__main__":
    params = sys.argv[1:]

    if len(params) == 0:
        conf_filepath = "../res/configuration.json"
    else:
        conf_filepath = params[0]

    if conf_filepath is not None:
        conf = Configuration.load(conf_filepath)
    else:
        conf = None

    ### READ GPU count
    # if len(params) > 0:
    #     for p1, p2 in zip(params, params[1:]):
    #         if p1 == "--gpus":
    #             conf.gpus = int(p2)

    grid_search(conf)

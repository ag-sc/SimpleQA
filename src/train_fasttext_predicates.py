import json
import sys

import os
from fastText import FastText

from nlp_utils import Configuration, get_timestamp
from utils import load_questions

LABEL_PREFIX = "__label__"


def main(training_filepath, conf):
    model_output_dirpath = os.path.join("../results/fasttext_predicate_models/fastext_model_{}".format(conf.timestamp))
    os.makedirs(model_output_dirpath)
    model_output_filepath = os.path.join(model_output_dirpath, "model")
    conf.model_output_dirpath = model_output_dirpath

    conf.save(os.path.join(model_output_dirpath, "configuration.json"))

    print(conf.description())
    classifier = FastText.train_supervised(training_filepath, thread=3, dim=conf.word_embedding_size,
                                           ws=conf.context_size, epoch=conf.n_epochs, wordNgrams=conf.max_ngram_size,
                                           minn=conf.min_char_ngram_size, maxn=conf.max_char_ngram_size,
                                           label=LABEL_PREFIX)

    classifier.save_model(model_output_filepath)

    val_results_at_1 = classifier.test("../res/valid_fasttext_predicates.txt", k=1)
    val_results_at_3 = classifier.test("../res/valid_fasttext_predicates.txt", k=3)
    val_results_at_5 = classifier.test("../res/valid_fasttext_predicates.txt", k=5)
    print("Validation:")
    print(val_results_at_1)
    print(val_results_at_3)
    print(val_results_at_5)

    with open(os.path.join(model_output_dirpath, "val_scores.json"), "w") as score_file:
        r = {"recall": {"1": val_results_at_1[2], "3": val_results_at_3[2], "5": val_results_at_5[2]}}
        score_file.write(json.dumps(r))

    predict(classifier, model_output_dirpath)


def predict(classifier, model_output_dirpath):
    test_results_at_1 = classifier.test("../res/test_fasttext_predicates.txt", k=1)
    test_results_at_3 = classifier.test("../res/test_fasttext_predicates.txt", k=3)
    test_results_at_5 = classifier.test("../res/test_fasttext_predicates.txt", k=5)
    print("Test:")
    print(test_results_at_1)
    print(test_results_at_3)
    print(test_results_at_5)

    with open(os.path.join(model_output_dirpath, "prediction_summary.txt"), "w") as score_file:
        r = {"recall": {"1": test_results_at_1[2], "3": test_results_at_3[2], "5": test_results_at_5[2]}}
        score_file.write(json.dumps(r))

    with open("../res/test_fasttext_predicates.txt") as f:
        ft_questions = [line.strip().split(" ", 1)[1] for line in f]

    questions = load_questions("../res/test.txt")
    predicted_labels, predicted_probas = classifier.predict(ft_questions, k=5)

    with open(os.path.join(model_output_dirpath, "predicted_answers.txt"), "w") as output_file:
        for q, preds, probs in zip(questions, predicted_labels, predicted_probas):
            q["predictions"] = [{"predicate": predicate.strip(LABEL_PREFIX), "probability": prob} for prob, predicate in
                                zip(probs, preds)]
            output_file.write(json.dumps(q) + "\n")


def grid_search():
    for min_char_ngram_size in [3, 4, 2, 5, 0]:
        for max_char_ngram_size in [3, 4, 2, 1, 0]:
            for word_embedding_size in [50, 100, 300]:
                for n_epochs in [50]:
                    for max_ngram_size in [5, 4, 3, 2, 1]:
                        conf = Configuration()
                        conf.word_embedding_size = word_embedding_size
                        conf.context_size = 5
                        conf.n_epochs = n_epochs
                        conf.max_ngram_size = max_ngram_size
                        conf.min_char_ngram_size = min_char_ngram_size
                        conf.max_char_ngram_size = min_char_ngram_size + max_char_ngram_size
                        conf.timestamp = get_timestamp()
                        main(questions_filepath, conf)


if __name__ == "__main__":
    params = sys.argv[1:]

    questions_filepath = "../res/train_fasttext_predicates.txt"
    output_filepath = "../results/fasttext_predicate_model"

    # conf = Configuration()
    # conf.word_embedding_size = 100
    # conf.context_size = 5
    # conf.n_epochs = 1
    # conf.max_ngram_size = 1
    # conf.min_char_ngram_size = 0
    # conf.max_char_ngram_size = 0
    # conf.timestamp = get_timestamp()
    # main(questions_filepath, conf)

    grid_search()

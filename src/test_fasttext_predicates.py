# /root/fastText/fasttext supervised -input $1 -output $2 -minn 0 -maxn 0 -dim 50 -epoch 100 -neg 100 -loss ns -lr 0.2 -ws 2 -wordNgrams 1 -thread 18 -label $3
import sys

import os
from fastText import FastText

from nlp_utils import Configuration, get_timestamp


def main(questions_filepath, model_filepath):

    classifier = FastText.load_model(model_filepath)
    result = classifier.test(questions_filepath, 1)
    print(result)
    result = classifier.test(questions_filepath, 3)
    print(result)
    result = classifier.test(questions_filepath, 5)
    print(result)


if __name__ == "__main__":
    params = sys.argv[1:]

    questions_filepath = "../res/test_fasttext_predicates.txt"
    model_filepath = "/home/sjebbara/git/SimpleJQA/results/fasttext_predicate_models/fastext_model_2018-03-26_14:32:16/model"

    main(questions_filepath, model_filepath)

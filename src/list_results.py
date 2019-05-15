import json
import os
from collections import Counter
from io import open

from texttable import Texttable

from nlp_utils import Configuration


class Experiment(object):
    def __init__(self, conf, scores):
        self.conf = conf
        self.scores = scores


def get_parameters(experiments):
    params = Counter()
    for exp in experiments:
        for k, v in exp.conf.items():
            params[(str(k), str(v))] += 1

    def _filter_params(name, value, count):
        if count == len(experiments) and len(experiments) > 1:
            return False
        elif "experiment_name" in name:
            return False
        elif "dataset" in name:
            return False
        elif "path" in name:
            return False
        elif "timestamp" in name:
            return False
        elif "seed" in name:
            return False

        return True

    params = [k for ((k, v), c) in params.items() if _filter_params(k, v, c)]
    params = list(set(params))
    return params


def read_results(base_dirpath):
    experiments_dirnames = os.listdir(base_dirpath)
    experiments_dirnames = [d for d in experiments_dirnames]
    experiments = []
    for exp_dirname in experiments_dirnames:
        try:
            conf_filepath = os.path.join(base_dirpath, exp_dirname, "configuration.json")
            conf = Configuration.load(conf_filepath)

            scores_filepath = os.path.join(base_dirpath, exp_dirname, "prediction_summary.txt")
            if not os.path.isfile(scores_filepath):
                scores_filepath = os.path.join(base_dirpath, exp_dirname, "predicate_prediction_summary.txt")

            with open(scores_filepath) as f:
                scores = json.loads(f.readline())

            experiments.append(Experiment(conf, scores))

        except IOError as e:
            print(exp_dirname, "missing some files")
    return experiments


def list_results(experiments, parameter_names):
    experiments = sorted(experiments, key=lambda e: e.scores["recall"]["1"], reverse=True)
    table = Texttable(max_width=0)
    table.header(["recall@1", "recall@3", "recall@5"] + parameter_names)
    for exp in experiments:
        row = [exp.scores["recall"]["1"], exp.scores["recall"]["3"], exp.scores["recall"]["5"]] + [exp.conf.get(p, "--")
                                                                                                   for p in
                                                                                                   parameter_names]
        table.add_row(row)

    print("+++ RESULTS: +++")
    print(table.draw())


def run():
    base_dirpath = "../results"

    # parameters = ["dataset", "global_knowledge_mode", "use_character_embeddings", "sequence_embedding_size",
    #               "n_iterations", "word_dropout", "tokenization_style", "n_ensemble"]

    experiments = read_results(base_dirpath)

    parameters = get_parameters(experiments) + ["model_dirpath"]
    list_results(experiments, parameters)


if __name__ == "__main__":
    run()

import os
import sys

import apply_simple_qa


def list_experiments(base_dirpath):
    experiments_dirpaths = os.listdir(base_dirpath)
    experiments_dirpaths = [os.path.join(base_dirpath, d) for d in experiments_dirpaths]
    return experiments_dirpaths


def evaluate_experiments(test_filepath, experiments_dirpaths):
    for i, experiments_dirpath in enumerate(experiments_dirpaths):
        try:
            print("Evaluate model {}/{} at: {}".format(i + 1, len(experiments_dirpaths), experiments_dirpath))
            conf_filepath = os.path.join(experiments_dirpaths, "configuration.conf")
            apply_simple_qa.evaluate(test_filepath, conf_filepath)
        except IOError as e:
            print("Missing some files for {}".format(experiments_dirpath))


if __name__ == "__main__":
    params = sys.argv[1:]
    if len(params) == 1:
        test_filepath = params[0]
        experiments_dirpaths = [exp_dirpath.strip() for exp_dirpath in sys.stdin]

        evaluate_experiments(test_filepath, experiments_dirpaths)
    elif len(params) == 2:
        test_filepath = params[0]
        base_dirpath = params[1]
        print("Evaluate all expriments in: {}".format(base_dirpath))
        experiments_dirpaths = list_experiments(base_dirpath)

        evaluate_experiments(test_filepath, experiments_dirpaths)
    else:
        print("Arguments not understood: {}".format(params))

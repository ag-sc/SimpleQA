import sys

from utils import load_questions

LABEL_PREFIX = "__label__"


def main(questions_filepath, output_filepath):
    questions = load_questions(questions_filepath)

    with open(output_filepath, "w") as fout:
        for q in questions:
            text = q["text"]
            # subject = q["subject"]
            predicate = q["predicate"]
            # for c in q["candidates"]:
            #     if c["uri"] == subject:
            #         ngram = q["ngram"]
            fout.write(LABEL_PREFIX + predicate + " " + text + "\n")


if __name__ == "__main__":
    params = sys.argv[1:]

    questions_filepath = "../res/valid.txt"
    output_filepath = "../res/valid_fasttext_predicates.txt"
    main(questions_filepath, output_filepath)

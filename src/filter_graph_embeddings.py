def main():
    filepath = '../res/freebase_embedding_model_filtered.vec'


    f_obj = open('../res/freebase_embedding_model_filtered_obj.vec', 'w')
    f_subj = open('../res/freebase_embedding_model_filtered_subj.vec', 'w')

    with open(filepath) as f:
        for line in f:
            parts = line.strip().split(" ")
            word = ''

            word = parts[0]

            if (word.startswith("obj_")):

                line = line.replace("obj_P_", "")
                line = line.replace("obj_E_", "")

                f_obj.write(line)  # python will convert \n to os.linesep
            else:
                line = line.replace("subj_P_", "")
                line = line.replace("subj_E_", "")
                f_subj.write(line)  # python will convert \n to os.linesep

if __name__ == "__main__":

    main()



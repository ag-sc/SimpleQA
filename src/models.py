import keras.backend as K
from keras.engine import Layer
from keras.layers import Input, Embedding, Bidirectional, GRU, Convolution1D, GlobalMaxPooling1D, TimeDistributed, \
    concatenate, Dense, Activation, Dropout
from keras.models import Model
from keras.optimizers import Adam
from keras.utils.training_utils import multi_gpu_model

from custom_layers import Squeeze, BetterTimeDistributed, RepeatToMatch, ElementAt, AttentionWeightedAverage

__author__ = 'sjebbara'


def pairwise_ranking_metric(labels, diffs):
    return K.mean(diffs > 0)


def margin_ranking_loss(label, diff, margin=1.0):
    ranking_loss = K.maximum(0, margin - diff)

    return ranking_loss


def batch_margin_ranking_loss(dummy, scores, margin=1.0):
    pos_score = scores[:, :1]
    neg_scores = scores[:, 1:]

    ranking_losses = K.maximum(0.0, margin + neg_scores - pos_score)  # pos score broadcasted

    ranking_loss = K.sum(ranking_losses, axis=-1)  # todo: sum or mean?
    return ranking_loss


# def batch_pairwise_margin_ranking_loss(labels, scores, margin=1.0):
#     pos_scores = K.expand_dims(scores[(labels >= 0.5).nonzero()], dim=-1)
#     neg_scores = K.expand_dims(scores[(labels <= 0.5).nonzero()], dim=-1)
#
#     pos_tmp = K.variable(numpy.array([[margin]]))
#     pos_scores = K.concatenate((pos_scores, pos_tmp), axis=0)
#
#     neg_tmp = K.variable(numpy.array([[0.]]))
#     neg_scores = K.concatenate((neg_scores, neg_tmp), axis=0)
#
#     n_p = K.shape(pos_scores)[0]
#     n_n = K.shape(neg_scores)[0]
#     repeated_pos_scores = K.repeat(pos_scores, n_n, )
#     repeated_neg_scores = K.transpose(K.repeat(neg_scores, n_p))
#
#     ranking_losses = K.maximum(0, margin - repeated_pos_scores + repeated_neg_scores)
#
#     return K.sum(ranking_losses)


### GENERAL COMPONENTS ###
def apply_sequence_model(layer_type, input_sequence, embedding_size, depth, kernel_size, dropout):
    # if pooling == "last":
    #     return_sequences = False
    # else:
    #     return_sequences = True

    embedding_sequence = input_sequence

    for d in range(depth):
        if layer_type == "rnn":
            embedding_sequence = Bidirectional(GRU(embedding_size, activation="selu", kernel_initializer="he_normal",
                                                   return_sequences=True if d < depth - 1 else False))(
                embedding_sequence)
        elif layer_type == "cnn":
            embedding_sequence = Convolution1D(embedding_size, kernel_size=kernel_size, activation="selu",
                                               kernel_initializer="he_normal", padding="same")(embedding_sequence)
        embedding_sequence = Dropout(dropout)(embedding_sequence)

    if layer_type == "cnn":
        embedding = GlobalMaxPooling1D()(embedding_sequence)
    else:
        embedding = embedding_sequence

    return embedding


### GENERAL COMPONENTS ###
def apply_sequence_attention_model(layer_type, input_sequence, relative_position_embeddings, embedding_size, depth,
                                   kernel_size, dropout):
    # if pooling == "last":
    #     return_sequences = False
    # else:
    #     return_sequences = True

    embedding_sequence = input_sequence

    for d in range(depth):
        if layer_type == "rnn":
            forward_embedding_sequence, backward_embedding_sequence = Bidirectional(
                GRU(embedding_size, activation="selu", kernel_initializer="he_normal",
                    return_sequences=True), merge_mode=None)(
                embedding_sequence)
            embedding_sequence = concatenate([forward_embedding_sequence, backward_embedding_sequence])
        elif layer_type == "cnn":
            embedding_sequence = Convolution1D(embedding_size, kernel_size=kernel_size, activation="selu",
                                               kernel_initializer="he_normal", padding="same")(embedding_sequence)
        embedding_sequence = Dropout(dropout)(embedding_sequence)

    if layer_type == "cnn":
        summary_embedding = GlobalMaxPooling1D()(embedding_sequence)
    else:
        forward_embedding = ElementAt(-1)(forward_embedding_sequence)
        backward_embedding = ElementAt(0)(backward_embedding_sequence)

        summary_embedding = concatenate([forward_embedding, backward_embedding])

    repeated_summary_embedding = RepeatToMatch()([summary_embedding, input_sequence])
    concatenated_attention_inputs = concatenate(
        [embedding_sequence, relative_position_embeddings, repeated_summary_embedding])

    attention_embedding = AttentionWeightedAverage()([embedding_sequence, concatenated_attention_inputs])
    return attention_embedding


def get_text_encoder(vocab_size, embedding_size, kernel_size, depth, dropout, embedding_layer=None, layer_type=None):
    text_input = Input(shape=(None,), dtype='int32', name='text_input')

    if embedding_layer is None:
        embedding_layer = Embedding(input_dim=vocab_size, output_dim=embedding_size, name="word_embeddings")

    embedding_sequence = embedding_layer(text_input)

    if layer_type is None:
        layer_type = "cnn"

    text_embedding = apply_sequence_model(layer_type, input_sequence=embedding_sequence, embedding_size=embedding_size,
                                          depth=depth, kernel_size=kernel_size, dropout=dropout)

    model = Model(inputs=[text_input], outputs=[text_embedding])

    return model


### SPECIFIC COMPONENTS ###
#
# def get_question_encoder(word_vocab_size, word_embedding_size, question_embedding_size):
#     question_input = Input(shape=(None,), dtype='int32', name='question_input')
#
#     word_embedding_sequence = Embedding(input_dim=word_vocab_size, output_dim=word_embedding_size,
#                                         name="word_embeddings")(question_input)
#
#     question_embedding = apply_sequence_model("cnn", input_sequence=word_embedding_sequence,
#                                               embedding_size=question_embedding_size, depth=5, pooling="max",
#                                               kernel_size=3)
#
#     model = Model(inputs=[question_input], outputs=[question_embedding])
#     return model
#
#
# def get_predicate_label_encoder(word_vocab_size, word_embedding_size, predicate_vocab_size, predicate_embedding_size):
#     predicate_label_input = Input(shape=(None,), dtype='int32', name='predicate_label_input')
#
#     predicate_word_embedding_sequence = Embedding(input_dim=word_vocab_size, output_dim=word_embedding_size,
#                                                   name="predicate_word_embeddings")(predicate_label_input)
#
#     predicate_label_embedding = apply_sequence_model("cnn", input_sequence=predicate_word_embedding_sequence,
#                                                      embedding_size=predicate_embedding_size, depth=2, pooling="max",
#                                                      kernel_size=3)
#
#     model = Model(inputs=[predicate_label_input], outputs=[predicate_label_embedding])
#     return model
#
#
# def get_entity_label_encoder(char_vocab_size, char_embedding_size, char_kernel_size, entity_embedding_size):
#     entity_label_input = Input(shape=(None,), dtype='int32', name='predicate_hierarchy_input')
#
#     entity_label_embedding_sequence = Embedding(input_dim=char_vocab_size, output_dim=char_embedding_size,
#                                                 name="entity_char_embeddings")(entity_label_input)
#
#     entity_label_embedding = apply_sequence_model("cnn", input_sequence=entity_label_embedding_sequence,
#                                                   embedding_size=entity_embedding_size, depth=2, pooling="max",
#                                                   kernel_size=3)
#
#     model = Model(inputs=[entity_label_input], outputs=[entity_label_embedding])
#     return model
#

def simple_joint_qa(conf, res, **kwargs):
    ### setup inputs ###
    # word_vocab_size = conf., word_embedding_size, word_embedding_weights, char_vocab_size, char_embedding_size,
    # match_embedding_size,

    ### INPUT LIST####
    inputs = []

    question_char_input = Input(shape=(None,), dtype='int32', name='question_char_input')
    question_token_input = Input(shape=(None,), dtype='int32', name='question_token_input')

    candidate_subject_labels_input = Input(shape=(None, None), dtype='int32', name='candidate_subject_labels_input')
    candidate_predicate_labels_input = Input(shape=(None, None), dtype='int32', name='candidate_predicate_labels_input')

    ## add to inputs
    inputs.append(question_char_input)
    inputs.append(question_token_input)
    inputs.append(candidate_subject_labels_input)
    inputs.append(candidate_predicate_labels_input)

    ### setup encoders ###
    word_embedding_layer = Embedding(input_dim=conf.word_vocab_size, output_dim=conf.word_embedding_size,
                                     weights=[res.word_embeddings.W], name="word_embeddings")

    char_sequence_encoder = get_text_encoder(vocab_size=conf.char_vocab_size, embedding_size=conf.char_embedding_size,
                                             kernel_size=conf.question_char_kernel_size,
                                             depth=conf.question_char_cnn_depth, dropout=conf.dropout,
                                             layer_type=conf.layer_type)
    predicate_token_sequence_encoder = get_text_encoder(vocab_size=conf.word_vocab_size,
                                                        embedding_size=conf.word_embedding_size,
                                                        kernel_size=conf.predicate_token_kernel_size,
                                                        depth=conf.predicate_token_cnn_depth,
                                                        embedding_layer=word_embedding_layer, dropout=conf.dropout,
                                                        layer_type=conf.layer_type)

    question_token_sequence_encoder = get_text_encoder(vocab_size=conf.word_vocab_size,
                                                       embedding_size=conf.word_embedding_size,
                                                       kernel_size=conf.question_token_kernel_size,
                                                       depth=conf.question_token_cnn_depth,
                                                       embedding_layer=word_embedding_layer, dropout=conf.dropout,
                                                       layer_type=conf.layer_type)

    ### encode inputs ###
    question_char_embedding = char_sequence_encoder(question_char_input)
    question_token_embedding = question_token_sequence_encoder(question_token_input)

    subject_label_embeddings = BetterTimeDistributed(char_sequence_encoder)(candidate_subject_labels_input)
    predicate_label_embeddings = BetterTimeDistributed(predicate_token_sequence_encoder)(
        candidate_predicate_labels_input)

    repeated_question_char_embedding = RepeatToMatch()([question_char_embedding, subject_label_embeddings])
    repeated_question_token_embedding = RepeatToMatch()([question_token_embedding, predicate_label_embeddings])

    ### compute partial matches ###
    ## use graph embeddings
    if conf.use_graph_embeddings:
        candidate_subject_graph_embedding_input = Input(shape=(None,), dtype='int32',
                                                        name='candidate_subject_graph_embeddings_input')

        candidate_predicate_graph_embedding_input = Input(shape=(None,), dtype='int32',
                                                          name='candidate_predicate_graph_embeddings_input')

        ## add to inputs
        inputs.append(candidate_subject_graph_embedding_input)
        inputs.append(candidate_predicate_graph_embedding_input)

        graph_embedding_layer = Embedding(input_dim=conf.graph_vocab_size, output_dim=conf.graph_embedding_size,
                                          weights=[res.graph_embeddings.W], name="graph_embeddings")

        subject_graph_embeddings = graph_embedding_layer(candidate_subject_graph_embedding_input)
        predicate_graph_embeddings = graph_embedding_layer(candidate_predicate_graph_embedding_input)

        subject_match_embeddings = concatenate(
            [repeated_question_char_embedding, subject_label_embeddings, subject_graph_embeddings])

        predicate_match_embeddings = concatenate(
            [repeated_question_token_embedding, predicate_label_embeddings, predicate_graph_embeddings])

    else:
        subject_match_embeddings = concatenate([repeated_question_char_embedding, subject_label_embeddings])
        predicate_match_embeddings = concatenate([repeated_question_token_embedding, predicate_label_embeddings])

    subject_match_embeddings = BetterTimeDistributed(
        Dense(conf.match_embedding_size, activation="selu", kernel_initializer="he_normal"))(subject_match_embeddings)
    subject_match_embeddings = Dropout(conf.dropout)(subject_match_embeddings)

    predicate_match_embeddings = BetterTimeDistributed(
        Dense(conf.match_embedding_size, activation="selu", kernel_initializer="he_normal"))(predicate_match_embeddings)
    predicate_match_embeddings = Dropout(conf.dropout)(predicate_match_embeddings)

    if conf.use_predicate_and_subject_outputs:
        ### SUBJECT output
        subject_match_scores = BetterTimeDistributed(Dense(1, activation="linear"))(subject_match_embeddings)
        subject_match_scores = Squeeze()(subject_match_scores)
        subject_match_scores = Activation("sigmoid")(subject_match_scores)
        subject_answer_scores = Layer(name="subject_answer_scores")(subject_match_scores)

        ### PREDICATE output
        predicate_match_scores = BetterTimeDistributed(Dense(1, activation="linear"))(predicate_match_embeddings)
        predicate_match_scores = Squeeze()(predicate_match_scores)
        predicate_match_scores = Activation("sigmoid")(predicate_match_scores)
        predicate_answer_scores = Layer(name="predicate_answer_scores")(predicate_match_scores)

        ### compute overall matches ###
        overall_match_embedding = concatenate([subject_match_embeddings, predicate_match_embeddings])
        overall_match_embedding = BetterTimeDistributed(
            Dense(conf.match_embedding_size, activation="selu", kernel_initializer="he_normal"))(
            overall_match_embedding)
        overall_match_embedding = Dropout(conf.dropout)(overall_match_embedding)

        overall_match_scores = BetterTimeDistributed(Dense(1, activation="linear"))(overall_match_embedding)
        overall_match_scores = Squeeze()(overall_match_scores)

        overall_match_scores = Activation("softmax")(overall_match_scores)

        answer_scores = Layer(name="answer_scores")(overall_match_scores)

        #### 3 OUTPUTS -: pair, subject and predicate alone
        outputs = [answer_scores, subject_answer_scores, predicate_answer_scores]

        model = Model(inputs=inputs, outputs=outputs)
        model.compile(Adam(), {"answer_scores": "categorical_crossentropy",
                               "predicate_answer_scores": "binary_crossentropy",
                               "subject_answer_scores": "binary_crossentropy"},
                      metrics=["accuracy", "crossentropy"],
                      loss_weights={"answer_scores": conf.answer_loss_weight,
                                    "predicate_answer_scores": conf.predicate_answer_loss_weight,
                                    "subject_answer_scores": conf.subject_answer_loss_weight})

    #### SINGLE OUTPUT : CandidatePAIR Scores
    else:
        ### compute overall matches ###
        overall_match_embedding = concatenate([subject_match_embeddings, predicate_match_embeddings])
        overall_match_embedding = BetterTimeDistributed(
            Dense(conf.match_embedding_size, activation="selu", kernel_initializer="he_normal"))(
            overall_match_embedding)
        overall_match_embedding = Dropout(conf.dropout)(overall_match_embedding)

        overall_match_scores = BetterTimeDistributed(Dense(1, activation="linear"))(overall_match_embedding)
        overall_match_scores = Squeeze()(overall_match_scores)

        overall_match_scores = Activation("softmax")(overall_match_scores)

        answer_scores = Layer(name="answer_scores")(overall_match_scores)

        outputs = [answer_scores]

        model = Model(inputs=inputs, outputs=outputs)
        model.compile(Adam(), "categorical_crossentropy", metrics=["accuracy", "crossentropy"])

    return model


def simple_joint_qa_predicate_model(conf, res, **kwargs):
    ### INPUT LIST####
    inputs = []

    ### setup encoders ###
    word_embedding_layer = Embedding(input_dim=conf.word_vocab_size, output_dim=conf.word_embedding_size,
                                     weights=[res.word_embeddings.W], name="word_embeddings")



    if conf.predicate_encoder_embedding_type == "word":
        question_token_input = Input(shape=(None,), dtype='int32', name='question_token_input')
        inputs.append(question_token_input)
        token_embeddings = word_embedding_layer(question_token_input)

        resulting_embeddings = token_embeddings

    elif conf.predicate_encoder_embedding_type == "char":
        question_char_input = Input(shape=(None, None), dtype='int32', name='question_char_input')
        inputs.append(question_char_input)
        char_sequence_encoder = get_text_encoder(vocab_size=conf.char_vocab_size,
                                                 embedding_size=conf.char_embedding_size,
                                                 kernel_size=conf.question_char_kernel_size,
                                                 depth=conf.question_char_cnn_depth, dropout=conf.dropout,
                                                 layer_type=conf.layer_type)

        token_char_embeddings = BetterTimeDistributed(char_sequence_encoder)(question_char_input)

        resulting_embeddings = token_char_embeddings

    ### combination OF WORD and CHAR embeddings
    else:
        question_char_input = Input(shape=(None, None), dtype='int32', name='question_char_input')
        question_token_input = Input(shape=(None,), dtype='int32', name='question_token_input')

        ## add to inputs
        inputs.append(question_char_input)
        inputs.append(question_token_input)

        char_sequence_encoder = get_text_encoder(vocab_size=conf.char_vocab_size,
                                                 embedding_size=conf.char_embedding_size,
                                                 kernel_size=conf.question_char_kernel_size,
                                                 depth=conf.question_char_cnn_depth, dropout=conf.dropout,
                                                 layer_type=conf.layer_type)

        token_char_embeddings = BetterTimeDistributed(char_sequence_encoder)(question_char_input)

        token_embeddings = word_embedding_layer(question_token_input)

        ## concatenate
        resulting_embeddings = concatenate([token_embeddings, token_char_embeddings])

    if "att" in conf.predicate_encoder_embedding_type:
        relative_position_input = Input(shape=(None,), dtype='int32', name='relative_position_input')

        inputs.append(relative_position_input)

        relative_position_embeddings = Embedding(input_dim=conf.subject_position_max_distance * 2 + 1,
                                                 output_dim=conf.distance_embedding_size, name="distance_embeddings")(
            relative_position_input)

        text_embedding = apply_sequence_attention_model(conf.layer_type, input_sequence=resulting_embeddings,
                                                        relative_position_embeddings=relative_position_embeddings,
                                                        embedding_size=conf.predicate_embedding_size,
                                                        depth=conf.predicate_embedding_depth,
                                                        kernel_size=conf.predicate_embedding_kernel_size,
                                                        dropout=conf.dropout)
    else:
        text_embedding = apply_sequence_model(conf.layer_type, input_sequence=resulting_embeddings,
                                              embedding_size=conf.predicate_embedding_size,
                                              depth=conf.predicate_embedding_depth,
                                              kernel_size=conf.predicate_embedding_kernel_size, dropout=conf.dropout)


    ### add DROPOUT
    text_embedding = Dropout(conf.dropout)(text_embedding)

    #### Different outputs based on predicate_model_type ####
    if conf.predicate_model_type == "predict_all_predicates":
        answer_scores = Dense(conf.predicate_vocab_size, activation="softmax")(text_embedding)
        answer_scores = Layer(name="answer_scores")(answer_scores)
        # loss_function = "categorical_crossentropy"
        # metrics = "accuracy"

    elif conf.predicate_model_type == "predict_graph_embedding":
        answer_scores = Dense(conf.graph_embedding_size, activation="selu")(text_embedding)
        answer_scores = Dense(conf.graph_embedding_size, activation="linear")(answer_scores)
        answer_scores = Layer(name="answer_scores")(answer_scores)
        # loss_function = "cosine_proximity"
        # metrics = "cosine_proximity"
    else:
        #### BINARY classification for candidate predicate

        graph_embedding_layer = Embedding(input_dim=conf.graph_vocab_size, output_dim=conf.graph_embedding_size,
                                          weights=[res.graph_embeddings.W], name="graph_embeddings")

        predicate_graph_embedding_input = Input(shape=(None,), dtype='int32', name='predicate_graph_embedding_input')

        inputs.append(predicate_graph_embedding_input)

        predicate_graph_embedding = graph_embedding_layer(predicate_graph_embedding_input)
        predicate_graph_embedding = ElementAt(0)(predicate_graph_embedding)

        concatenated = concatenate([predicate_graph_embedding, text_embedding])

        concatenated_layer_output = Dense(200, activation="selu")(concatenated)
        answer_scores = Dense(1, activation="sigmoid")(concatenated_layer_output)
        answer_scores = Layer(name="answer_scores")(answer_scores)
        # loss_function = "binary_crossentropy"
        # metrics = "accuracy"

    outputs = [answer_scores]

    model = Model(inputs=inputs, outputs=outputs)

    # if conf.gpus > 1:
    #     model = multi_gpu_model(model, gpus=conf.gpus)

    model.compile(Adam(), conf.predicate_model_loss_function, metrics=[conf.predicate_model_metrics])

    return model

import datetime
import itertools
import json
import os
import pprint
import time
from collections import Counter
import codecs
import numpy
import operator


class Embedding(object):
    def __init__(self):
        self.vocabulary = None
        self.W = None

    def __len__(self):
        return len(self.vocabulary)

    def __contains__(self, word):
        return word in self.vocabulary

    def init(self, vocabulary, W, padding=None):
        assert isinstance(vocabulary, Vocabulary)
        if padding:
            padding_vector = numpy.zeros((1, W.shape[1]))
            self.W = numpy.concatenate((padding_vector, W), axis=0)
            self.vocabulary = vocabulary
            vocabulary.init_from_word_list([padding] + vocabulary.index2word)
        else:
            self.vocabulary = vocabulary
            self.W = W

    def init_from_gensim(self, w2v):
        self.vocabulary = Vocabulary()
        self.vocabulary.init_from_gensim(w2v)
        self.W = w2v.syn0

    def trim_embeddings(self, vocab_trim=None, top_k=None):
        counts = Counter()

        if vocab_trim:
            vocab_trim = set(vocab_trim)
            for w in vocab_trim:
                counts[w] = self.vocabulary.counts[w]
        else:
            vocab_trim = set()

        if top_k:
            counts.update(dict(self.vocabulary.counts.most_common(top_k)))
            vocab_trim.update(set([w for w, c in counts.items()]))

        indices = self.vocabulary.get_indices(vocab_trim)
        self.W = self.W[indices]
        self.vocabulary.init_from_word_list(vocab_trim, counts)

    def get_vector(self, word):
        return self.W[self.vocabulary.get_index(word), :]

    def get_vectors(self, words, drop_unknown=False):
        indices = self.vocabulary.get_indices(words, drop_unknown)
        if len(indices) > 0:
            return self.W[indices, :]
        else:
            return numpy.zeros((0, self.W.shape[1]))

    def save(self, dirpath, embedding_name):
        embedding_filepath = os.path.join(dirpath, embedding_name + "_W.npy")
        vocab_filepath = os.path.join(dirpath, embedding_name + "_vocab.txt")
        numpy.save(embedding_filepath, self.W)
        self.vocabulary.save(vocab_filepath)

    def save_plain_text_file(self, filepath):
        with open(filepath, "w") as f:
            for i, word in enumerate(self.vocabulary.index2word):
                vector = self.W[i]
                vector_str = " ".join(["{:.8f}".format(v) for v in vector])

                f.write(word + " " + vector_str + "\n")

    def load(self, embedding_filepath, vocab_filepath):
        self.W = numpy.load(embedding_filepath)
        self.vocabulary = Vocabulary()
        self.vocabulary.load(vocab_filepath)

    def load_plain_text_file(self, filepath, top_k=None, has_header=False):
        words = []
        word_set = set()
        vectors = []
        d = None
        with codecs.open(filepath, 'r', encoding='utf-8') as f:
            for i, line in enumerate(f):
                if not has_header or i > 0:
                    if top_k is not None and len(words) >= top_k:
                        break

                    parts = line.strip().split(" ")
                    word = ''

                    word = parts[0]

                    if word in word_set:
                        print("WARNING: Word repeated in line {}: '{}'".format(i, line))
                        continue
                    word_set.add(word)
                    vector = [float(f) for f in parts[1:]]
                    if d is None:
                        d = len(vector)

                    if len(vector) != d:
                        print("WARNING: Line {} seems to be broken: '{}'".format(i, line))
                        continue
                    words.append(word)
                    vectors.append(vector)

        vectors = numpy.array(vectors)
        vocabulary = Vocabulary()
        vocabulary.init_from_word_list(words)
        self.init(vocabulary, vectors)

    def add(self, word, index=None, vector=None, vector_init=None):
        if vector is None:
            if vector_init == "zeros":
                vector = numpy.zeros(self.W.shape[1:])
            elif vector_init == "mean":
                vector = numpy.mean(self.W, axis=0)
            elif vector_init == "uniform":
                m = numpy.mean(self.W, axis=0)
                s = numpy.std(self.W, axis=0)
                vector = numpy.random.rand(self.W.shape[1]) * s + m / 2
            elif vector_init == "normal":
                m = numpy.mean(self.W, axis=0)
                s = numpy.std(self.W, axis=0)
                vector = numpy.random.randn(self.W.shape[1]) * s + m

        vector = numpy.expand_dims(vector, axis=0)
        if index is None:
            self.vocabulary.add(word)
            self.W = numpy.append(self.W, vector, axis=0)
        else:
            self.vocabulary.add(word, index)
            self.W = numpy.concatenate((self.W[:index], vector, self.W[index:]), axis=0)

    def normalize(self):
        self.W = self.W / numpy.expand_dims(numpy.linalg.norm(self.W, axis=1), axis=1)


class Vocabulary(object):
    def __init__(self):
        self.vocab = set()
        self.counts = Counter()
        self.index2word = []
        self.word2index = dict()

    def set_padding(self, index):
        self.padding_index = index
        self.padding_word = self.get_word(index)

    def add_padding(self, word, index):
        self.add(word, index)
        self.set_padding(index)

    def set_unknown(self, index):
        self.unknown_index = index
        self.unknown_word = self.get_word(index)

    def add_unknown(self, word, index):
        self.add(word, index)
        self.set_unknown(index)

    def init_from_vocab(self, vocab):
        self.vocab = set(vocab)

        tmp_vocab = list(self.vocab)
        self.index2word, self.word2index = get_mappings(tmp_vocab)
        self.counts = Counter(self.vocab)

    def init_from_mapping(self, index2word, word2index):
        self.index2word = index2word
        self.word2index = word2index
        self.vocab = set(self.word2index.keys())
        self.counts = Counter(self.vocab)

    def init_from_word_list(self, vocab_list, counts=None):
        self.index2word, self.word2index = get_mappings(vocab_list)
        self.vocab = set(self.word2index.keys())
        if counts:
            self.counts = counts
        else:
            self.counts = Counter(self.vocab)

    def init_from_counts(self, counts):
        self.counts = counts

        self.index2word, self.word2index = get_mappings(self.counts.keys())
        self.vocab = set(self.word2index.keys())

    def init_from_gensim(self, w2v):
        self.index2word, self.word2index = get_mappings(w2v.index2word)
        self.vocab = set(self.word2index.keys())
        self.counts = Counter(dict([(w, v.count) for w, v in w2v.vocab.items()]))

    def __len__(self):
        return len(self.vocab)

    def __contains__(self, word):
        return word in self.word2index

    def __str__(self):
        s = "#Vocab: %d" % (len(self.vocab))
        if hasattr(self, "padding_word") and self.padding_word:
            s += "\n  padding: %d = '%s'" % (
                self.word2index[self.padding_word], self.index2word[self.word2index[self.padding_word]])
        if hasattr(self, "unknown_word") and self.unknown_word:
            s += "\n  unknown: %d = '%s'\n" % (
                self.word2index[self.unknown_word], self.index2word[self.word2index[self.unknown_word]])

        if len(self.index2word) <= 10:
            s += "[{}]".format(", ".join(["'{}'".format(w) for w in self.index2word]))
        else:
            s += "[{}, ... , {}]".format(", ".join(["'{}'".format(w) for w in self.index2word[:5]]),
                                         ", ".join(["'{}'".format(w) for w in self.index2word[-5:]]))

        return s

    def most_common(self, top_k=None):
        return [w for w, c in self.counts.most_common(top_k)]

    def words(self):
        return self.vocab

    def get_index(self, word):
        if word in self.word2index:
            return self.word2index[word]
        else:
            if hasattr(self, "unknown_index") and self.unknown_index is not None:
                return self.unknown_index
            else:
                raise ValueError("Unknown word '{}' received and no value specified for that.".format(word))

    def get_indices(self, words, drop_unknown=False):
        if drop_unknown or not hasattr(self, "unknown_index"):
            return values2indices(words, self.word2index)
        else:
            return values2indices(words, self.word2index, default=self.unknown_index)

    def indices(self):
        return range(len(self.index2word))

    def get_word(self, index):
        return self.index2word[index]

    def get_words(self, indices, drop_unknown=False):
        if drop_unknown or not hasattr(self, "unknown_word"):
            return indices2values(indices, self.index2word)
        else:
            return indices2values(indices, self.index2word, default=self.unknown_word)

    def to_one_hot(self, word):
        v = numpy.zeros(len(self.index2word))
        v[self.get_index(word)] = 1
        return v

    def to_k_hot(self, words):
        v = numpy.zeros(len(self.index2word))
        for i, word in enumerate(words):
            v[self.get_index(word)] = 1
        return v

    def to_bow(self, words):
        v = numpy.zeros(len(self.index2word))
        for i, word in enumerate(words):
            v[self.get_index(word)] += 1
        return v

    def to_bbow(self, words):
        v = self.to_bow(words)
        v = (v > 0).astype(int)
        return v

    def to_one_hot_sequence(self, words):
        v = numpy.zeros((len(words), len(self.index2word)))
        for i, word in enumerate(words):
            v[i, self.get_index(word)] = 1
        return v

    def from_one_hot(self, one_hot, top_k=None):
        if top_k is None:
            i = numpy.argmax(one_hot)
            word = self.get_word(i)
            return word

        else:
            # return top-k words from the vocabulary
            scores = {}
            for index, score in enumerate(one_hot):
                word = self.get_word(index)
                scores[word] = score

                # sort
                sorted(scores.items(), key=operator.itemgetter(1))
                top_k_words = list(scores.keys)[0:top_k]
                return top_k_words

    def from_k_hot(self, k_hot, threshold=0.5):
        words = set(self.get_words(i for i, x in enumerate(k_hot) if x > threshold))
        return words

    def from_bow(self, bow, threshold=0.5):
        words = []
        for i, v in enumerate(bow):
            k = int(v)
            r = v - k
            if r > threshold:
                k += 1
            words += [self.get_word(i)] * k
        return words

    def from_bbow(self, bow, threshold=0.5):
        words = set(self.get_words(i for i, x in enumerate(bow) if x > threshold))
        return words

    def save(self, filepath):
        word_count_list = [(w, self.counts[w]) for w in self.index2word]

        def to_str(x):
            w, c = x
            return w + u" " + str(c)

        write_iterable(word_count_list, filepath, to_str)

    def load(self, filepath):
        def to_object(line):
            parts = line.split(" ")
            word = parts[0]

            if len(parts) > 1:
                count = int(parts[1])
            else:
                count = 1
            return word, count

        word_count_list = load_as_list(filepath, to_object=to_object)
        self.index2word = [w for w, c in word_count_list]
        self.counts = Counter(dict(word_count_list))

        self.index2word, self.word2index = get_mappings(self.index2word)
        self.vocab = set(self.index2word)

    def add(self, word, index=None):
        if word in self.word2index:
            print("already there", word, self.word2index[word])
            return self.word2index[word]
        else:
            if index is None:
                return self.append(word)
            else:
                new_word_order = self.index2word[:index] + [word] + self.index2word[index:]
                self.counts[word] = 1
                self.init_from_word_list(new_word_order, self.counts)

                if hasattr(self, "unknown_index") and self.unknown_index is not None and index <= self.unknown_index:
                    self.unknown_index += 1
                    print("Moved {} to {}".format(self.unknown_word, self.unknown_index))
                if hasattr(self, "padding_index") and self.padding_index is not None and index <= self.padding_index:
                    self.padding_index += 1
                    print("Moved {} to {}".format(self.padding_word, self.padding_index))
                return index

    def append(self, word):
        if word in self.word2index:
            return self.word2index[word]
        else:
            self.vocab.add(word)
            index = len(self.index2word)
            self.index2word.append(word)
            self.word2index[word] = index
            self.counts[word] = 1
            return index


class BatchIterator(object):
    def __init__(self, named_batch_iterable):
        self.iterable = named_batch_iterable

    def __iter__(self):
        for batches in self.iterable:
            try_next = True
            i = 0
            while try_next:
                instance = BetterDict()
                for name, batch in batches.items():
                    if i < len(batch):
                        instance[name] = batch[i]
                    else:
                        try_next = False
                        break  # has_next = has_next and i < len(batch) - 1
                i += 1
                if try_next:
                    yield instance


class BetterDict(object):
    def __init__(self, *args, **kwargs):
        super().__init__()
        super().__setattr__('_data', dict(*args, **kwargs))

    def __getitem__(self, item):
        return self._data[item]

    def __setitem__(self, key, value):
        self._data[key] = value

    def __delitem__(self, key):
        del self._data[key]

    def __getattr__(self, item):
        return self._data[item]

    def __setattr__(self, key, value):
        self._data[key] = value

    def __repr__(self):
        return repr(self._data)

    def __str__(self):
        return str(self._data)

    def __len__(self):
        return len(self._data)

    def __contains__(self, item):
        return item in self._data

    def clear(self):
        return self._data.clear()

    def copy(self):
        return self._data.copy()

    def has_key(self, k):
        return k in self._data

    def update(self, *args, **kwargs):
        return self._data.update(*args, **kwargs)

    def keys(self):
        return self._data.keys()

    def values(self):
        return self._data.values()

    def items(self):
        return self._data.items()


class Configuration(object):
    def __init__(self, *args, **kwargs):
        super().__init__()
        super().__setattr__('_data', dict(*args, **kwargs))
        super().__setattr__('_accessed_fields', set())

    def __getitem__(self, item):
        self._accessed_fields.add(item)
        return self._data[item]

    def __setitem__(self, key, value):
        self._data[key] = value

    def __delitem__(self, key):
        del self._data[key]

    def __getattr__(self, item):
        self._accessed_fields.add(item)
        return self._data[item]

    def __setattr__(self, key, value):
        self._data[key] = value

    def __repr__(self):
        return repr(self._data)

    def __str__(self):
        return str(self._data)

    def __len__(self):
        return len(self._data)

    def __contains__(self, item):
        return item in self._data

    def clear(self):
        return self._data.clear()

    def copy(self):
        return self._data.copy()

    def has_key(self, k):
        return k in self._data

    def update(self, *args, **kwargs):
        return self._data.update(*args, **kwargs)

    def keys(self):
        return self._data.keys()

    def values(self):
        return self._data.values()

    def items(self):
        return self._data.items()

    @property
    def accessed_fields(self):
        return self._accessed_fields

    def clear_accessed_fields(self):
        self._accessed_fields.clear()

    def accessed_dict(self):
        d = {k: self[k] for k in self._accessed_fields}
        return Configuration(d)

    def _to_primitive_dict(self):
        d = dict()
        for k, v in self.items():
            if hasattr(v, "__name__"):
                d[k] = v.__name__
            else:
                d[k] = v
        return d

    def description(self):
        return pprint.pformat(self._data)

    def save(self, filepath, makedirs=False, accessed_only=False):
        if accessed_only:
            d = self.accessed_dict()._to_primitive_dict()
        else:
            d = self._to_primitive_dict()

        if makedirs:
            dirpath = os.path.dirname(filepath)
            os.makedirs(dirpath)

        with open(filepath, "w") as f:
            f.write(json.dumps(d, indent=4, sort_keys=True))

    @classmethod
    def load(cls, filepath):
        with open(filepath) as f:
            d = json.load(f)
        conf = cls(d)
        return conf


def print_batch_shapes(batches):
    for name, b in batches.items():
        if hasattr(b, "shape"):
            shape = b.shape
        else:
            shape = len(b)

        print("'{}': {}".format(name, shape))


def remove_padding(iterable, expected_length, padding_position):
    if padding_position == "pre":
        return iterable[-expected_length:]
    elif padding_position == "post":
        return iterable[:expected_length]


def get_timestamp():
    return datetime.datetime.fromtimestamp(time.time()).strftime('%Y-%m-%d_%H:%M:%S')


def pad_to_shape(X, to_shape, padding_position, value, mask=False):
    to_shape = list(to_shape)
    X_padded = []
    X_padding_mask = []
    if len(to_shape) > 1:
        for x in X:
            x_padded, padding_mask = pad_to_shape(x, to_shape[1:], padding_position, value, mask=True)
            X_padded.append(x_padded)
            X_padding_mask.append(padding_mask)
    else:
        X_padded = X
        X_padding_mask = numpy.ones((len(X)))

    X_padded = numpy.array(X_padded)
    X_padding_mask = numpy.array(X_padding_mask)

    padding_value = numpy.array(value)
    pad_shape = [to_shape[0] - len(X)] + to_shape[1:] + [1] * padding_value.ndim
    X_pad = numpy.ones(pad_shape) * padding_value
    # print X_padded.shape, pad_shape, X_pad.shape
    X_pad_mask = numpy.zeros_like(X_pad)
    # print X_padding_mask.shape, X_pad_mask.shape
    if padding_position == "pre":
        X_padded = numpy.concatenate((X_pad, X_padded), axis=0)

        X_padding_mask = numpy.concatenate((X_pad_mask, X_padding_mask), axis=0)
    elif padding_position == "post":
        X_padded = numpy.concatenate((X_padded, X_pad), axis=0)
        X_padding_mask = numpy.concatenate((X_padding_mask, X_pad_mask), axis=0)

    if mask:
        return X_padded, X_padding_mask
    else:
        return X_padded


def pad_to_shape_no_mask(X, to_shape, padding_position, value):
    to_shape = list(to_shape)
    X_padded = []
    if len(to_shape) > 1:
        for x in X:
            x_padded = pad_to_shape_no_mask(x, to_shape[1:], padding_position, value)
            X_padded.append(x_padded)
    else:
        X_padded = X

    X_padded = numpy.array(X_padded)

    if isinstance(value, str):
        padding_value = [value]
        pad_shape = [to_shape[0] - len(X)] + to_shape[1:]
    else:
        padding_value = numpy.array(value)
        pad_shape = [to_shape[0] - len(X)] + to_shape[1:] + [1] * padding_value.ndim

    X_pad = numpy.ones(pad_shape) * padding_value
    if padding_position == "pre":
        X_padded = numpy.concatenate((X_pad, X_padded), axis=0)

    elif padding_position == "post":
        X_padded = numpy.concatenate((X_padded, X_pad), axis=0)

    return X_padded


def pad(X, padding_position, value, mask=False):
    shape = get_padding_shape(X)
    # print "RAW PAD SHAPE:", shape
    value_shape = get_padding_shape(value)

    n_dim_value = len(value_shape)
    if n_dim_value > 0:
        shape = shape[:-n_dim_value]  # print "Value shape:", value_shape, n_dim_value  # print "PAD SHAPE:", shape

    if mask:
        return pad_to_shape(X, shape, padding_position, value, True)
    else:
        return pad_to_shape_no_mask(X, shape, padding_position, value)


def get_padding_shape(X):
    if isinstance(X, str):
        return []

    try:
        iter(X)
    except TypeError as e:
        return []

    # if not isinstance(X, list) and not isinstance(X, tuple):
    # 	return None
    padding_shape_x = []
    for x in X:
        padding_shape_x_new = get_padding_shape(x)

        if len(padding_shape_x_new) == 0:
            return [len(X)]
        else:
            if len(padding_shape_x) == 0:
                padding_shape_x = padding_shape_x_new
            else:
                padding_shape_x = list(
                    map(max, itertools.zip_longest(padding_shape_x, padding_shape_x_new, fillvalue=0)))
    return [len(X)] + padding_shape_x


def get_mappings(vocab):
    index2value = list(vocab)
    value2index = dict((v, i) for i, v in enumerate(index2value))
    return index2value, value2index


def values2indices(values, value2index, default=None):
    if default is None:
        vec = numpy.array([value2index[v] for v in values if v in value2index])
    else:
        vec = numpy.array([value2index[v] if v in value2index else default for v in values])

    # vec = numpy.expand_dims(vec, axis=0)
    return vec


def indices2values(indices, index2value, default=None):
    if default is None:
        val = [index2value[i] for i in indices if i < len(index2value)]
    else:
        val = [index2value[i] if i < len(index2value) else default for i in indices]

    return val


def load_as_list(filepath, to_object=None, filter=None):
    s = list()
    with open(filepath) as f:
        for line in f:
            line = line.rstrip()
            if filter is None or filter(line):
                if to_object:
                    obj = to_object(line)
                else:
                    obj = line
                s.append(obj)
    return s


def write_iterable(it, filepath, to_str=None):
    with open(filepath, "w", ) as f:
        for x in it:
            if to_str:
                x = to_str(x)
            line = x + u"\n"
            f.write(line)

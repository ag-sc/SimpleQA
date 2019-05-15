import keras.backend as K
from keras.engine import InputSpec
from keras.engine.topology import _object_list_uid, Layer
from keras.layers import Lambda, Wrapper, Permute, Reshape, Dense, RepeatVector, merge, Activation, initializers, \
    activations

__author__ = 'sjebbara'


def margin_mse(y_true, y_pred, margin=0):
    err = K.mean(K.square(y_pred - y_true), axis=-1)
    return K.switch(err < margin, 0, err)


def RepeatToMatch(axis=1, **kwargs):
    def dynamic_repeat(inputs):
        (X, other_tensor) = inputs
        n_repeat = K.shape(other_tensor)[axis]
        # return K.repeat_elements(X, rep=n_repeat, axis=axis)
        return K.repeat(X, n_repeat)

    def dynamic_repeat_shape(input_shapes):
        (shape, other_shape) = input_shapes
        output_shape = shape[:axis] + (other_shape[axis],) + shape[axis:]
        return output_shape

    return Lambda(dynamic_repeat, dynamic_repeat_shape, **kwargs)


def ElementAt(index=0, **kwargs):
    return Lambda(lambda seq: seq[:, index], output_shape=lambda shape: shape[:1] + shape[2:], **kwargs)


def Expand(axis=-1, **kwargs):
    def fn(X):
        return K.expand_dims(X, axis)

    def fn_shape(shape):
        return shape + (1,)

    return Lambda(fn, fn_shape, **kwargs)


def ElementAt(index, **kwargs):
    def fn(X):
        return X[:, index]

    def fn_shape(shape):
        return shape[:1] + shape[2:]

    return Lambda(fn, fn_shape, **kwargs)


def Squeeze(axis=-1, **kwargs):
    def fn(X):
        return K.squeeze(X, axis)

    def fn_shape(shape):
        if axis == -1:
            return shape[:axis]
        else:
            return shape[:axis] + shape[axis + 1:]

    return Lambda(fn, fn_shape, **kwargs)


def ExpandLast(**kwargs):
    def fn(X):
        return K.expand_dims(X, dim=-1)

    def fn_shape(shape):
        return shape + (1,)

    return Lambda(fn, fn_shape, **kwargs)


class BetterTimeDistributed(Wrapper):
    """This wrapper applies a layer to every temporal slice of an input.

    The input should be at least 3D, and the dimension of index one
    will be considered to be the temporal dimension.

    Consider a batch of 32 samples,
    where each sample is a sequence of 10 vectors of 16 dimensions.
    The batch input shape of the layer is then `(32, 10, 16)`,
    and the `input_shape`, not including the samples dimension, is `(10, 16)`.

    You can then use `TimeDistributed` to apply a `Dense` layer
    to each of the 10 timesteps, independently:

    ```python
        # as the first layer in a model
        model = Sequential()
        model.add(TimeDistributed(Dense(8), input_shape=(10, 16)))
        # now model.output_shape == (None, 10, 8)
    ```

    The output will then have shape `(32, 10, 8)`.

    In subsequent layers, there is no need for the `input_shape`:

    ```python
        model.add(TimeDistributed(Dense(32)))
        # now model.output_shape == (None, 10, 32)
    ```

    The output will then have shape `(32, 10, 32)`.

    `TimeDistributed` can be used with arbitrary layers, not just `Dense`,
    for instance with a `Conv2D` layer:

    ```python
        model = Sequential()
        model.add(TimeDistributed(Conv2D(64, (3, 3)),
                                  input_shape=(10, 299, 299, 3)))
    ```

    # Arguments
        layer: a layer instance.
    """

    def __init__(self, layer, n_distribution_axes=1, **kwargs):
        super(BetterTimeDistributed, self).__init__(layer, **kwargs)
        self.supports_masking = True
        self.n_distribution_axes = n_distribution_axes

    def build(self, input_shape):
        assert len(input_shape) >= 3
        self.input_spec = InputSpec(shape=input_shape)
        child_input_shape = (input_shape[0],) + input_shape[2:]
        if not self.layer.built:
            self.layer.build(child_input_shape)
            self.layer.built = True
        super(BetterTimeDistributed, self).build()

    def compute_output_shape(self, input_shape):
        child_input_shape = (input_shape[0],) + input_shape[self.n_distribution_axes + 1:]
        child_output_shape = self.layer.compute_output_shape(child_input_shape)
        distribution_axes = tuple(input_shape[1:self.n_distribution_axes + 1])
        output_shape = (child_output_shape[0],) + distribution_axes + child_output_shape[1:]
        return output_shape

    def call(self, inputs, training=None, mask=None):
        kwargs = {}
        if K.has_arg(self.layer.call, 'training'):
            kwargs['training'] = training
        uses_learning_phase = False

        # transformation in self._input_map.
        input_uid = _object_list_uid(inputs)

        actual_input_shape = K.shape(inputs)

        reshaped_axis_size = actual_input_shape[0]
        for i in range(self.n_distribution_axes):
            reshaped_axis_size *= actual_input_shape[i + 1]

        reshaped_input_shape = [reshaped_axis_size]

        in_ndims = K.ndim(inputs)
        # print("in_ndims:", in_ndims)
        # print("reshaped_axis_size:", reshaped_axis_size)

        for i in range(1 + self.n_distribution_axes, in_ndims):
            # print("i", i)
            reshaped_input_shape.append(actual_input_shape[i])

        # reshaped_input_shape = (reshaped_axis_size,) + tuple(actual_input_shape[self.n_distribution_axes + 1:])
        reshaped_input_shape = tuple(reshaped_input_shape)

        inputs = K.reshape(inputs, reshaped_input_shape)
        # inputs = K.reshape(inputs, reshaped_input_shape, ndim=len(reshaped_input_shape))

        self._input_map[input_uid] = inputs
        # (num_samples * timesteps, ...)
        y = self.layer.call(inputs, **kwargs)
        if hasattr(y, '_uses_learning_phase'):
            uses_learning_phase = y._uses_learning_phase
        # Shape: (num_samples, timesteps, ...)

        actual_output_shape = K.shape(y)

        # reshaped_output_shape = tuple(actual_input_shape[:self.n_distribution_axes + 1]) + tuple(
        #     actual_output_shape[1:])
        reshaped_output_shape = []
        for i in range(self.n_distribution_axes + 1):
            reshaped_output_shape.append(actual_input_shape[i])

        out_ndims = K.ndim(y)
        # print("out_ndims:", out_ndims)
        for i in range(1, out_ndims):
            # print("i", i)
            reshaped_output_shape.append(actual_output_shape[i])

        reshaped_output_shape = tuple(reshaped_output_shape)

        y = K.reshape(y, reshaped_output_shape)
        # y = K.reshape(y, reshaped_output_shape, ndim=len(reshaped_output_shape))

        # print("out_ndims:", K.ndim(y))
        # Apply activity regularizer if any:
        if (hasattr(self.layer, 'activity_regularizer') and self.layer.activity_regularizer is not None):
            regularization_loss = self.layer.activity_regularizer(y)
            self.add_loss(regularization_loss, inputs)

        if uses_learning_phase:
            y._uses_learning_phase = True
        return y


class AttentionWeightedAverage(Layer):
    """
    Computes a weighted average of the different channels across timesteps.
    Uses 1 parameter pr. channel to compute the attention value for a single timestep.
    """

    def __init__(self, return_attention=False, **kwargs):
        self.init = initializers.get('uniform')
        self.supports_masking = True
        self.return_attention = return_attention
        super(AttentionWeightedAverage, self).__init__(**kwargs)

    def build(self, input_shapes):
        self.input_spec = [InputSpec(ndim=3), InputSpec(ndim=3)]
        input_shape, attention_input_shape = input_shapes
        assert len(attention_input_shape) == 3

        self.W = self.add_weight(shape=(attention_input_shape[2], 1), name='{}_W'.format(self.name),
                                 initializer=self.init)
        self.trainable_weights = [self.W]
        super(AttentionWeightedAverage, self).build(attention_input_shape)

    def call(self, inputs, mask=None):
        # computes a probability distribution over the timesteps
        # uses 'max trick' for numerical stability
        # reshape is done to avoid issue with Tensorflow
        # and 1-dimensional weights
        input_sequence, attention_sequence = inputs

        logits = K.dot(attention_sequence, self.W)
        x_shape = K.shape(attention_sequence)
        logits = K.reshape(logits, (x_shape[0], x_shape[1]))
        ai = K.exp(logits - K.max(logits, axis=-1, keepdims=True))

        # masked timesteps have zero weight
        if mask is not None:
            mask = K.cast(mask, K.floatx())
            ai = ai * mask
        att_weights = ai / (K.sum(ai, axis=1, keepdims=True) + K.epsilon())
        weighted_input = input_sequence * K.expand_dims(att_weights)
        result = K.sum(weighted_input, axis=1)
        if self.return_attention:
            return [result, att_weights]
        return result

    def get_output_shape_for(self, input_shape):
        return self.compute_output_shape(input_shape)

    def compute_output_shape(self, input_shapes):
        input_shape, attention_input_shape = input_shapes

        output_len = input_shape[2]
        if self.return_attention:
            return [(input_shape[0], output_len), (input_shape[0], input_shape[1])]
        return (input_shape[0], output_len)

    def compute_mask(self, input, input_mask=None):
        if isinstance(input_mask, list):
            return [None] * len(input_mask)
        else:
            return None


class CrossAttention(Layer):
    def __init__(self, mode="bilinear", attention_embedding_size=None, inner_activation="tanh",
                 return_attention_matrix=False, initializer="glorot_uniform", **kwargs):
        self.mode = mode
        self.initializer = initializers.get(initializer)
        self.attention_embedding_size = attention_embedding_size
        self.inner_activation = activations.get(inner_activation)
        self.return_attention_matrix = return_attention_matrix
        super(CrossAttention, self).__init__(**kwargs)

    def build(self, input_shapes):
        input_sequence_shape = input_shapes[0]
        attendee_sequence_shape = input_shapes[1]

        if self.mode == "linear":
            weight_shape = (input_sequence_shape[2] + attendee_sequence_shape[2], 1)
            self.W = self.add_weight(name='W', shape=weight_shape, initializer=self.initializer, trainable=True,
                                     constraint=None)
        if self.mode == "mlp":
            weight_shape1 = (input_sequence_shape[2] + attendee_sequence_shape[2], self.attention_embedding_size)
            weight_shape2 = (self.attention_embedding_size, 1)
            self.W_inner = self.add_weight(name='W_inner', shape=weight_shape1, initializer=self.initializer,
                                           trainable=True, constraint=None)
            self.W = self.add_weight(name='W', shape=weight_shape2, initializer=self.initializer, trainable=True,
                                     constraint=None)
        elif self.mode == "bilinear":
            weight_shape = (input_sequence_shape[2], attendee_sequence_shape[2])
            self.W = self.add_weight(name='W', shape=weight_shape, initializer=self.initializer, trainable=True,
                                     constraint=None)

        super(CrossAttention, self).build(input_shapes)  # Be sure to call this somewhere!

    def repeat(self, X, n):
        Xr = K.tile(X, (1, 1, n))
        Xr = K.reshape(Xr, (K.shape(X)[0], K.shape(X)[1] * n, K.shape(X)[2]))
        return Xr

    def call(self, inputs):
        if len(inputs) == 2:
            input_key_sequence, attendee_key_sequence = inputs
            attendee_value_sequence = attendee_key_sequence
        elif len(inputs) == 3:
            input_key_sequence, attendee_key_sequence, attendee_value_sequence = inputs
        else:
            raise ValueError(
                "Layer does not understand number provided inputs: {}. Can only deal with 2 or 3 inputs.".format(
                    len(inputs)))

        B = K.shape(input_key_sequence)[0]
        N_input = K.shape(input_key_sequence)[1]
        N_attendee = K.shape(attendee_key_sequence)[1]

        if self.mode == "linear":
            repeated_input_sequence = self.repeat(input_key_sequence, N_attendee)
            repeated_attendee_sequence = K.tile(attendee_key_sequence, [1, N_input, 1])
            combined_sequence = K.concatenate([repeated_input_sequence, repeated_attendee_sequence], axis=2)

            score_sequence = K.dot(combined_sequence, self.W)
            score_sequence = K.squeeze(score_sequence, axis=-1)
        elif self.mode == "mlp":
            repeated_input_sequence = self.repeat(input_key_sequence, N_attendee)
            repeated_attendee_sequence = K.tile(attendee_key_sequence, [1, N_input, 1])
            combined_sequence = K.concatenate([repeated_input_sequence, repeated_attendee_sequence], axis=2)

            embedding_sequence = K.dot(combined_sequence, self.W_inner)
            embedding_sequence = self.inner_activation(embedding_sequence)
            score_sequence = K.dot(embedding_sequence, self.W)
            score_sequence = K.squeeze(score_sequence, axis=-1)
        elif self.mode == "bilinear":
            repeated_attendee_sequence = K.tile(attendee_key_sequence, [1, N_input, 1])

            tmp_sequence = K.dot(input_key_sequence, self.W)
            tmp_sequence = self.repeat(tmp_sequence, N_attendee)
            score_sequence = K.sum(repeated_attendee_sequence * tmp_sequence, axis=-1)
        else:
            raise ValueError("Unknown attention mode: {}".format(self.mode))

        A = K.reshape(score_sequence, (B, N_input, N_attendee))
        A = self._softmax(A)

        attended_sequence = self._apply_attention_matrix((attendee_value_sequence, A))

        if self.return_attention_matrix:
            return [attended_sequence, A]
        else:
            return attended_sequence

    def _softmax(self, X, axis=2):
        e = K.exp(X - K.max(X, axis=axis, keepdims=True))
        s = K.sum(e, axis=axis, keepdims=True)
        return e / s

    def _apply_attention_matrix(self, inputs):
        context_sequence, normalized_attention_matrix = inputs
        # normalized_attention_matrix = _softmax(attention_matrix, axis=2)  # B, S, C

        tmp_normalized_attention_matrix = K.expand_dims(normalized_attention_matrix, axis=3)  # B, S, C, 1
        context_sequence = K.expand_dims(context_sequence, axis=1)  # B, 1, C, Da

        weighted_context_sequence = tmp_normalized_attention_matrix * context_sequence  # B, S, C, Da
        attended_sequence = K.sum(weighted_context_sequence, axis=2)  # B, S, Da
        return attended_sequence

    def compute_output_shape(self, input_shapes):
        if len(input_shapes) == 2:
            input_sequence_shape, attendee_key_sequence_shape = input_shapes
            attendee_value_sequence_shape = attendee_key_sequence_shape
        elif len(input_shapes) == 3:
            input_sequence_shape, attendee_key_sequence_shape, attendee_value_sequence_shape = input_shapes
        else:
            raise ValueError(
                "Layer does not understand number provided inputs: {}. Can only deal with 2 or 3 inputs.".format(
                    len(input_shapes)))

        output_shape = (input_sequence_shape[0], input_sequence_shape[1], attendee_value_sequence_shape[2])
        attention_matrix_shape = (input_sequence_shape[0], input_sequence_shape[1], attendee_value_sequence_shape[1])

        if self.return_attention_matrix:
            return [output_shape, attention_matrix_shape]
        else:
            return output_shape

# def attention_3d_block(inputs):
#     # inputs.shape = (batch_size, time_steps, input_dim)
#     input_dim = int(inputs.shape[2])
#     a = Permute((2, 1))(inputs)
#     a = Reshape((input_dim, TIME_STEPS))(a) # this line is not useful. It's just to know which dimension is what.
#     a = Dense(TIME_STEPS, activation='softmax')(a)
#     # if SINGLE_ATTENTION_VECTOR:
#     #     a = Lambda(lambda x: K.mean(x, axis=1), name='dim_reduction')(a)
#     #     a = RepeatVector(input_dim)(a)
#     a_probs = Permute((2, 1), name='attention_vec')(a)
#     output_attention_mul = merge([inputs, a_probs], name='attention_mul', mode='mul')
#     return output_attention_mul
#
# def attention_3d_block(inputs):
#     a = BetterTimeDistributed(Dense(1))(inputs)
#     a = Squeeze(a, axis=1)
#     a_probs = Activation("softmax")(a_probs)
#     a_probs = Expand(axis=1)
#
#     output_attention_mul = merge([inputs, a_probs], name='attention_mul', mode='d')
#     return output_attention_mul
#

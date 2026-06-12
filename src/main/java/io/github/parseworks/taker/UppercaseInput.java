package io.github.parseworks.taker;

/**
 * An {@link Input} decorator that always returns uppercase characters.
 * If the wrapped input is a {@link TextInput}, this also implements {@link TextInput}.
 */
class UppercaseInput implements Input {

    private final Input delegate;

    UppercaseInput(Input delegate) {
        this.delegate = delegate;
    }

    public Input delegate() {
        return delegate;
    }

    @Override
    public CharSequence data() {
        return new CharSequence() {
            @Override
            public int length() {
                return delegate.data().length();
            }

            @Override
            public char charAt(int index) {
                return Character.toUpperCase(delegate.data().charAt(index));
            }

            @Override
            public CharSequence subSequence(int start, int end) {
                return delegate.data().subSequence(start, end).toString().toUpperCase();
            }

            @Override
            public String toString() {
                return delegate.data().toString().toUpperCase();
            }
        };
    }

    @Override
    public boolean isEof() {
        return delegate.isEof();
    }

    @Override
    public char current() {
        return Character.toUpperCase(delegate.current());
    }

    @Override
    public Input next() {
        return new UppercaseInput(delegate.next());
    }

    @Override
    public int position() {
        return delegate.position();
    }

    @Override
    public Input skip(int offset) {
        return new UppercaseInput(delegate.skip(offset));
    }

    @Override
    public boolean hasMore() {
        return delegate.hasMore();
    }

    @Override
    public String toString() {
        return "UppercaseInput{delegate=" + delegate + "}";
    }
}

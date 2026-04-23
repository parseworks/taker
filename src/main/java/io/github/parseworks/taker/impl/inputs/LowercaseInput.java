package io.github.parseworks.taker.impl.inputs;

import io.github.parseworks.taker.Input;

public class LowercaseInput implements Input {

    private final Input delegate;

    public LowercaseInput(Input delegate) {
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
                return Character.toLowerCase(delegate.data().charAt(index));
            }

            @Override
            public CharSequence subSequence(int start, int end) {
                return delegate.data().subSequence(start, end).toString().toLowerCase();
            }

            @Override
            public String toString() {
                return delegate.data().toString().toLowerCase();
            }
        };
    }

    @Override
    public boolean isEof() {
        return delegate.isEof();
    }

    @Override
    public char current() {
        return Character.toLowerCase(delegate.current());
    }

    @Override
    public Input next() {
        return new LowercaseInput(delegate.next());
    }

    @Override
    public int position() {
        return delegate.position();
    }

    @Override
    public Input skip(int offset) {
        return new LowercaseInput(delegate.skip(offset));
    }

    @Override
    public boolean hasMore() {
        return delegate.hasMore();
    }

    @Override
    public String toString() {
        return "LowercaseInput{delegate=" + delegate + "}";
    }
}

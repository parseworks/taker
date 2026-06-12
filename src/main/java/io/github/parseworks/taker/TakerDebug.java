package io.github.parseworks.taker;

final class TakerDebug {

    private static final ThreadLocal<Integer> depth = ThreadLocal.withInitial(() -> 0);

    private TakerDebug() {
    }

    static <A> Taker<A> systemOut(Taker<A> parser, String label) {
        return new Taker<>(input -> {
            int currentDepth = depth.get();
            String indent = "  ".repeat(currentDepth);
            String name = label != null ? label : "Taker";
            String snippet = getSnippet(input);

            System.out.printf("%s%s starting at pos %d: [%s]%n",
                indent, name, input.position(), snippet);

            depth.set(currentDepth + 1);
            long start = System.nanoTime();
            try {
                Result<A> result = parser.apply(input);
                long elapsed = System.nanoTime() - start;
                double ms = elapsed / 1_000_000.0;

                if (result.matches()) {
                    System.out.printf("%s%s succeeded in %.3fms with value: %s%n",
                        indent, name, ms, result.value());
                } else {
                    System.out.printf("%s%s failed in %.3fms: %s%n",
                        indent, name, ms, result.error());
                }
                return result;
            } finally {
                depth.set(currentDepth);
            }
        });
    }

    private static String getSnippet(Input input) {
        StringBuilder sb = new StringBuilder();
        Input temp = input;
        for (int i = 0; i < 20 && !temp.isEof(); i++) {
            char c = temp.current();
            if (c == '\n') sb.append("\\n");
            else if (c == '\r') sb.append("\\r");
            else if (c == '\t') sb.append("\\t");
            else sb.append(c);
            temp = temp.next();
        }
        if (!temp.isEof()) sb.append("...");
        return sb.toString();
    }
}

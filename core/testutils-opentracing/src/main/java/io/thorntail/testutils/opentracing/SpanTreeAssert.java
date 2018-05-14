package io.thorntail.testutils.opentracing;

import org.assertj.core.api.Assertions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by bob on 2/27/18.
 */
public class SpanTreeAssert {

    public SpanTreeAssert(SpanTree tree) {
        this.tree = tree;
    }

    public SpanTreeAssert hasRootSpans(int number) {
        Assertions.assertThat(tree.getRootNodes()).describedAs("root spans").hasSize(number);
        return this;
    }

    private final SpanTree tree;
}

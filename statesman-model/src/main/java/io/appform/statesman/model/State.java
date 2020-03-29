package io.appform.statesman.model;

import lombok.Value;

/**
 *
 */
@Value
public class State {
    String name;
    boolean terminal;
}

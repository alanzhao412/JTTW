package com.jttw.engine.character;

/** A key event on an ability slot, as sent from client to server. */
public enum AbilityAction {
    /** A single press, for {@code TOGGLE} abilities. */
    USE,
    /** Key went down, for {@code HELD} abilities. */
    START,
    /** Key came up, for {@code HELD} abilities. */
    STOP
}

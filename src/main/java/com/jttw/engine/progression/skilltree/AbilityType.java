package com.jttw.engine.progression.skilltree;

/**
 * How a skill-bar ability is activated by the player.
 *
 * <ul>
 *   <li>{@link #MENU}   — Pressing the key opens an interaction screen (e.g. 72 Transformations).</li>
 *   <li>{@link #TOGGLE} — One press enables; a second press (or automatic logic) disables.</li>
 *   <li>{@link #HELD}   — Active while the key is held down; deactivates on release (e.g. Ring of Fire).</li>
 *   <li>{@link #PASSIVE}— Never activated directly; grants a permanent effect.</li>
 * </ul>
 */
public enum AbilityType {
    MENU,
    TOGGLE,
    HELD,
    PASSIVE
}
package com.bedi.warcaby;

public enum MoveType {
    NONE, NORMAL, KILL;

    public String toString() {
        switch (this) {
            case NONE -> {
                return "NONE";
            }
            case NORMAL -> {
                return "NORMAL";
            }
            case KILL -> {
                return "KILL";
            }
        }
        return "NONE";
    }
}

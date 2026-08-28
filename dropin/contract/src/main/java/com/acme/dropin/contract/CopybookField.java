package com.acme.dropin.contract;

/**
 * One elementary field of a copybook record, with the byte offset it occupies
 * in the record image handed to a CICS program through the COMMAREA.
 */
public record CopybookField(String name, int level, PicClause pic, int offset) {

    public int length() {
        return pic.byteLength();
    }

    public int end() {
        return offset + length();
    }

    public boolean isFiller() {
        return "FILLER".equals(name);
    }
}

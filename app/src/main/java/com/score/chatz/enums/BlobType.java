package com.score.chatz.enums;

/**
 * Created by eranga on 10/1/16.
 */
public enum BlobType {
    TEXT(1),
    IMAGE(2),
    SOUND(3);

    private int type;

    BlobType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public static BlobType valueOfType(int state) {
        for (BlobType blobType : BlobType.values()) {
            if (blobType.type == state) {
                return blobType;
            }
        }

        return null;
    }
}

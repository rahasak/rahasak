package com.score.rahasak.enums;


public enum BlobType {
    TEXT(1),
    IMAGE(2),
    CALL(3),
    MISSED_SELFIE(4),
    MISSED_CALL(5);

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

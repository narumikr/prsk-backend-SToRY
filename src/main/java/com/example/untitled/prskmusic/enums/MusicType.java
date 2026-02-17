package com.example.untitled.prskmusic.enums;

import com.example.untitled.common.dto.ErrorDetails;
import com.example.untitled.common.exception.BadRequestException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.List;

public enum MusicType {
    ORIGINAL(0, "オリジナル"),
    THREE_D_MV(1, "3DMV"),
    TWO_D_MV(2, "2DMV");

    private final int code;
    private final String displayName;

    MusicType(int code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    @JsonValue
    public int getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static MusicType fromCode(int code) {
        for(MusicType type: values()) {
            if(type.code == code) {
                return type;
            }
        }
        throw new BadRequestException(
                "Bad Request",
                List.of(new ErrorDetails(
                        "MusicType",
                        "Invalid MusicType: " + code
                ))
        );
    }
}
package com.attacktimer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum FontTypes
{
    REGULAR("RS Regular"),
    ARIAL("Arial"),
    BOLD("Bold"),
    CAMBRIA("Cambria"),
    ROCKWELL("Rockwell"),
    SEGOE_UI("Segoe Ui"),
    TIMES_NEW_ROMAN("Times New Roman"),
    VERDANA("Verdana");

    private final String name;

    @Override
    public String toString() {
        return name;
    }
}

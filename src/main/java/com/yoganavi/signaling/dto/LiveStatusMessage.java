package com.yoganavi.signaling.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class LiveStatusMessage {

    private String liveId;
    private boolean isOnAir;
}

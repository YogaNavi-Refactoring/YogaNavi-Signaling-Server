package com.yoganavi.signaling.dto;

import lombok.Getter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SignalingDto {
    private int liveId;
    private boolean onAir;
}
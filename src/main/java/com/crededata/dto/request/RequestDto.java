package com.crededata.dto.request;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RequestDto {

    private Keys keys;
    private int skipCount;
    private int maxResultCount;

    @Data
    @Builder
    public static class Keys{
        private List<Integer> txv;
        private List<Integer> currentPage;

    }
}

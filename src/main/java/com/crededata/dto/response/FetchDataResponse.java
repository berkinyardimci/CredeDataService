package com.crededata.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FetchDataResponse {

    private String tenderRegistrationNo;
    private String natureTypeQuantity;
    private String placeToDo;
    private String tenderType;
    private String url;
}

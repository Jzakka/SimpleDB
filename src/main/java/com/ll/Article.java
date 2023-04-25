package com.ll;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Article {
    private long id;
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;
    private String title;
    private String body;

    /*
    ObjectMapper가 isBlind를 blind로 바꿔서 바인딩하려고 함
    명시적으로 매핑키를 설정하지 않으면 에러난다.
     */
    @JsonProperty("isBlind")
    private boolean isBlind;
}

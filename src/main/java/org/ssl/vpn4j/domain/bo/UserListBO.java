package org.ssl.vpn4j.domain.bo;

import lombok.Data;

import java.util.List;

@Data
public class UserListBO {
    private String keyword;

    private List<String> status;
}

package study.querydsl.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserDto {

    private String username;
    private int age;

    public UserDto(String username, int age) {
        this.username = username;
        this.age = age;
    }
}

package study.querydsl.controller;

import lombok.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.repository.MemberJpaRepository;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberJpaRepository memberJpaRepository;

    @GetMapping(value = "/v1/members")
    public JsonResult searchMemberV1(MemberSearchCondition condition) {
        List<MemberTeamDto> resultList = memberJpaRepository.searchByBuilder(condition);
        return new JsonResult(resultList.size(), resultList);
    }

    @Data
    @AllArgsConstructor
    static class JsonResult<T> {
        private int count;
        private T data;
    }
}

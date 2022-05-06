package study.querydsl.controller;

import lombok.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.repository.MemberJpaRepository;
import study.querydsl.repository.MemberRepository;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberJpaRepository memberJpaRepository;
    private final MemberRepository memberRepository;

    @GetMapping(value = "/v1/members")
    public JsonResult searchMemberV1(MemberSearchCondition condition) {
        List<MemberTeamDto> resultList = memberJpaRepository.searchByBuilder(condition);
        return new JsonResult(resultList.size(), resultList);
    }

    @GetMapping(value = "/v2/members")
    public JsonResult searchMemberV2(MemberSearchCondition condition, Pageable pageable) {
        Page<MemberTeamDto> resultList = memberRepository.searchPageSimple(condition, pageable);
        return new JsonResult(resultList.getSize(), resultList);
    }

    @GetMapping(value = "/v3/members")
    public JsonResult searchMemberV3(MemberSearchCondition condition, Pageable pageable) {
        Page<MemberTeamDto> resultList = memberRepository.searchPageComplex(condition, pageable);
        return new JsonResult(resultList.getSize(), resultList);
    }


    @Data
    @AllArgsConstructor
    static class JsonResult<T> {
        private int count;
        private T data;
    }
}

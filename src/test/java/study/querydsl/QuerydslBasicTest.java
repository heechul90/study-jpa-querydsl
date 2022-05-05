package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import java.util.List;

import static com.querydsl.jpa.JPAExpressions.*;
import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.*;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em);

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    void startJPQL() {
        //member1
        Member findMember = em.createQuery(
                "select m from Member m " +
                        " where m.name = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findMember.getName()).isEqualTo("member1");
    }

    @Test
    void startQuerydsl() {
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.name.eq("member1"))
                .fetchOne();

        assertThat(findMember.getName()).isEqualTo("member1");
    }

    @Test
    void searchTest() {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.name.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();

        assertThat(findMember.getName()).isEqualTo("member1");
        assertThat(findMember.getAge()).isEqualTo(10);
    }

    @Test
    void searchAndParamTest() {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                        member.name.eq("member1"),
                        (member.age.eq(10))
                )
                .fetchOne();

        assertThat(findMember.getName()).isEqualTo("member1");
        assertThat(findMember.getAge()).isEqualTo(10);
    }

    @Test
    void resultFetchTest() {
        /*List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();

        Member fetchOne = queryFactory
                .selectFrom(QMember.member)
                .fetchOne();

        Member fetchFirst = queryFactory
                .selectFrom(QMember.member)
                .fetchFirst();


        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .fetchResults();
        results.getTotal();
        List<Member> content = results.getResults();
         */

        long total = queryFactory
                .selectFrom(member)
                .fetchCount();
    }

    @Test
    void sortTest() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> resultList = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.name.asc().nullsLast())
                .fetch();

        Member member5 = resultList.get(0);
        Member member6 = resultList.get(1);
        Member memberNull = resultList.get(2);

        assertThat(member5.getName()).isEqualTo("member5");
        assertThat(member6.getName()).isEqualTo("member6");
        assertThat(memberNull.getName()).isNull();
    }

    @Test
    void pagingTest1() {
        List<Member> resultList = queryFactory
                .selectFrom(member)
                .orderBy(member.name.desc())
                .offset(1)
                .limit(2)
                .fetch();

        assertThat(resultList.size()).isEqualTo(2);
    }
    
    @Test
    void pagingTest2() {
        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.name.desc())
                .offset(1)
                .limit(2)
                .fetchResults();

        assertThat(queryResults.getTotal()).isEqualTo(4);
        assertThat(queryResults.getLimit()).isEqualTo(2);
        assertThat(queryResults.getOffset()).isEqualTo(1);
        assertThat(queryResults.getResults().size()).isEqualTo(2);
    }

    @Test
    void aggregation() {
        List<Tuple> resultList = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        Tuple tuple = resultList.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    @Test
    public void grroupTest() throws Exception{
        List<Tuple> resultList = queryFactory
                .select(
                        team.name,
                        member.age.avg()
                )
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = resultList.get(0);
        Tuple teamB = resultList.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    @Test
    void joinTest() {
        List<Member> resultList = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(resultList).extracting("name").containsExactly("member1", "member2");
    }

    @Test
    void thetaJoinTest() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Member> resultList = queryFactory
                .select(member)
                .from(member, team)
                .where(member.name.eq(team.name))
                .fetch();

        assertThat(resultList).extracting("name").containsExactly("teamA", "teamB");
    }

    @Test
    void joinOnFiltering() {
        List<Tuple> resultList = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : resultList) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    void joinOnNoRelationTest() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Tuple> fetch = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.name.eq(team.name))
                .fetch();

        for (Tuple tuple : fetch) {
            System.out.println("tuple = " + tuple);
        }
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    void fetchJoinNoTest() {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.name.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 미적용").isFalse();
    }

    @Test
    void fetchJoinTest() {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.name.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 미적용").isTrue();
    }

    @Test
    void subQueryTest() {

        QMember memberSub = new QMember("memberSub");

        List<Member> resultList = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(resultList).extracting("age").containsExactly(40);
    }

    @Test
    void subQueryGoeTest() {

        QMember memberSub = new QMember("memberSub");

        List<Member> resultList = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        select(memberSub.age.avg())
                        .from(memberSub)
                ))
                .fetch();

        assertThat(resultList).extracting("age").containsExactly(30, 40);
    }

    @Test
    void subQueryInTest() {

        QMember memberSub = new QMember("memberSub");

        List<Member> resultList = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        select(memberSub.age)
                        .from(memberSub)
                        .where(memberSub.age.gt(10))
                ))
                .fetch();

        assertThat(resultList).extracting("age").containsExactly(20, 30, 40);
    }

    @Test
    void selectSubQueryTest() {

        QMember memberSub = new QMember("memberSub");

        List<Tuple> resultList = queryFactory
                .select(member.name,
                        select(memberSub.age.avg())
                        .from(memberSub))
                .from(member)
                .fetch();

        for (Tuple tuple : resultList) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    void basicCaseTest() {
        List<String> resultList = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타")
                )
                .from(member)
                .fetch();

        for (String result : resultList) {
            System.out.println("result = " + result);
        }
    }

    @Test
    void complexCaseTest() {
        List<String> resultList = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타")
                )
                .from(member)
                .fetch();

        for (String result : resultList) {
            System.out.println("result = " + result);
        }
    }

    @Test
    void constantTest() {
        List<Tuple> resultList = queryFactory
                .select(member.name, Expressions.constant("A"))
                .from(member)
                .fetch();
        for (Tuple result : resultList) {
            System.out.println("result = " + result);
        }
    }

    /**
     * age 변환시 길이가 1이라서 한글자만 가져오는 이슈가 있음
     * SELECT H2VERSION(); h2 database에서 확인할 수 있음
     * 다른 데이터베이스는 모르겟음
     */
    @Test
    void concatTest() {
        //{name}_{age}
        List<String> resultList = queryFactory
                .select(member.name.concat("_").concat(member.age.stringValue()))
                .from(member)
                .fetch();
        for (String result : resultList) {
            System.out.println("result = " + result);
        }
    }

    @Test
    void concat2Test() {
        //{name}_{age}
        List<String> resultList = queryFactory
                .select(member.name.concat("_111"))
                .from(member)
                .fetch();
        for (String result : resultList) {
            System.out.println("result = " + result);
        }
    }

    @Test
    void simpleProjectionTest() {
        List<String> resultList = queryFactory
                .select(member.name)
                .from(member)
                .fetch();

        for (String result : resultList) {
            System.out.println("result = " + result);
        }
    }

    @Test
    void tupleProjectionTest() {
        List<Tuple> resultList = queryFactory
                .select(member.name, member.age)
                .from(member)
                .fetch();

        for (Tuple result : resultList) {
            String name = result.get(member.name);
            Integer age = result.get(member.age);
            System.out.println("name = " + name);
            System.out.println("age = " + age);
        }
    }

    @Test
    void findDtoByJPQL() {
        List<MemberDto> resultList = em.createQuery(
                "select new study.querydsl.dto.MemberDto(m.name, m.age) " +
                        " from Member m", MemberDto.class)
                .getResultList();

        for (MemberDto memberDto : resultList) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * setter 생성자로 값을 넣어준다
     */
    @Test
    void findDtoBySetter() {
        List<MemberDto> resultList = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.name,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto result : resultList) {
            System.out.println("result = " + result);
        }
    }

    /**
     * 필드에 그냥 바로 넣어준다.
     */
    @Test
    void findDtoByField() {
        List<MemberDto> resultList = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.name,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto result : resultList) {
            System.out.println("result = " + result);
        }
    }

    /**
     * 필드에 그냥 바로 넣어준다.(UserDto)
     */
    @Test
    void findUserDtoByField() {
        QMember memberSub = new QMember("memberSub");
        List<UserDto> resultList = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.name.as("username"),
                        ExpressionUtils.as(JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub), "age")
                        ))
                .from(member)
                .fetch();

        for (UserDto result : resultList) {
            System.out.println("result = " + result);
        }
    }

    /**
     * 생성자를 통해 넣어준다.(타입이 맞아야한다.)
     */
    @Test
    void findDtoByConstructor() {
        List<MemberDto> resultList = queryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.name,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto result : resultList) {
            System.out.println("result = " + result);
        }
    }

    /**
     * 생성자를 통해 넣어준다.(타입이 맞아야한다.)(UserDto)
     */
    @Test
    void findUserDtoByConstructor() {
        List<UserDto> resultList = queryFactory
                .select(Projections.constructor(UserDto.class,
                        member.name,
                        member.age))
                .from(member)
                .fetch();

        for (UserDto result : resultList) {
            System.out.println("result = " + result);
        }
    }

    @Test
    void findDtoByQueryProjection() {
        List<MemberDto> resultList = queryFactory
                .select(new QMemberDto(member.name, member.age))
                .from(member)
                .fetch();

        for (MemberDto result : resultList) {
            System.out.println("result = " + result);
        }
    }

    @Test
    void dynamicQueryBooleanBuilder() {
        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> resultList = searchMember1(usernameParam, ageParam);
        assertThat(resultList.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String nameCondition, Integer ageCondition) {

        //초기값 넣을수 있다.
        //BooleanBuilder builder = new BooleanBuilder(member.name.eq(nameCondition));
        BooleanBuilder builder = new BooleanBuilder();
        if (nameCondition != null) {
            builder.and(member.name.eq(nameCondition));
        }

        if (ageCondition != null) {
            builder.and(member.age.eq(ageCondition));
        }

        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    @Test
    void dynamicQueryWhereParam() {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> resultList = searchMember2(usernameParam, ageParam);
        assertThat(resultList.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String nameCondition, Integer ageCondition) {
        return queryFactory
                .selectFrom(member)
                .where(nameEq(nameCondition), ageEq(ageCondition))
                .fetch();
    }

    private BooleanExpression nameEq(String nameCondition) {
        return nameCondition != null ? member.name.eq(nameCondition) : null;
    }

    private BooleanExpression ageEq(Integer ageCondition) {
        return ageCondition != null ? member.age.eq(ageCondition) : null;
    }

    private BooleanExpression allEq(String nameCondition, Integer ageCondition) {
        return member.name.eq(nameCondition).and(member.age.eq(ageCondition));
    }

    @Test
    void bulkUpdate() {

        //member1 = 10 -> DB 비회원
        //member2 = 20 -> DB 비회원
        //member3 = 30 -> DB member3
        //member4 = 40 -> DB member4

        long count = queryFactory
                .update(member)
                .set(member.name,"비회원")
                .where(member.age.lt(28))
                .execute();

        //영속성 컨텍스트가 우선권을 가지게 되서 디비정보를 출력하지 않는다.
        //1 member1 = 10 -> 1 DB 비회원
        //2 member2 = 20 -> 2 DB 비회원
        //3 member3 = 30 -> 3 DB member3
        //4 member4 = 40 -> 4 DB member4

        List<Member> resultList = queryFactory
                .selectFrom(member)
                .fetch();

        for (Member result : resultList) {
            System.out.println("result = " + result);
        }

        em.flush();
        em.clear();

        List<Member> resultList1 = queryFactory
                .selectFrom(member)
                .fetch();

        for (Member result : resultList1) {
            System.out.println("result1 = " + result);
        }
    }

    @Test
    void bulkAdd() {
        long count = queryFactory
                .update(member)
                .set(member.age, member.age.add(1))
                .execute();

        em.flush();
        em.clear();

        List<Member> resultList = queryFactory
                .selectFrom(member)
                .fetch();

        for (Member result : resultList) {
            System.out.println("result = " + result);
        }
    }

    @Test
    void bulkDelete() {
        long count = queryFactory
                .delete(member)
                .where(member.age.gt(18))
                .execute();

        em.flush();
        em.clear();

        List<Member> resultList = queryFactory
                .selectFrom(member)
                .fetch();

        for (Member result : resultList) {
            System.out.println("result = " + result);
        }
    }






}

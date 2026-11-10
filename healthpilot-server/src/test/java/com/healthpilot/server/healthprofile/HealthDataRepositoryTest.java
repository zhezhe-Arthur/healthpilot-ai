
package com.healthpilot.server.healthprofile;

import com.healthpilot.server.healthprofile.entity.HealthProfileEntity;
import com.healthpilot.server.healthprofile.repository.HealthProfileRepository;

import com.healthpilot.server.healthgoal.entity.HealthGoalEntity;
import com.healthpilot.server.healthgoal.repository.HealthGoalRepository;

import com.healthpilot.server.healthcheckin.entity.HealthDailyCheckinEntity;
import com.healthpilot.server.healthcheckin.repository.HealthDailyCheckinRepository;

import com.healthpilot.server.user.entity.UserEntity;
import com.healthpilot.server.user.repository.UserRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class HealthDataRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HealthProfileRepository profileRepository;

    @Autowired
    private HealthGoalRepository goalRepository;

    @Autowired
    private HealthDailyCheckinRepository checkinRepository;

    @PersistenceContext
    private EntityManager entityManager;

    // 创建测试用户，不使用真实用户资料
    private UserEntity createUser() {
        String email = "health-test-"
                + UUID.randomUUID()
                + "@example.com";

        return userRepository.saveAndFlush(
                new UserEntity(email, "test-only-password-hash")
        );
    }

    // 清空JPA一级缓存，保证后面的查询重新读取数据库
    private void clearPersistenceContext() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void shouldSaveAndFindHealthProfile() {

        UserEntity user = createUser();

        HealthProfileEntity profile =
                new HealthProfileEntity(
                        user.getId(),
                        "Asia/Shanghai"
                );

        profile.setBirthYear((short) 1990);
        profile.setHeightCm(new BigDecimal("175.50"));
        profile.setActivityLevel(
                HealthProfileEntity.ActivityLevel.MODERATE
        );
        profile.setSleepSchedule("23:00-07:00");

        profileRepository.saveAndFlush(profile);

        clearPersistenceContext();

        HealthProfileEntity found = profileRepository
                .findByUserId(user.getId())
                .orElseThrow();

        assertNotNull(found.getId());
        assertEquals(user.getId(), found.getUserId());
        assertEquals(
                HealthProfileEntity.ActivityLevel.MODERATE,
                found.getActivityLevel()
        );
        assertEquals("Asia/Shanghai", found.getTimezone());
        assertEquals(0, found.getHeightCm()
                .compareTo(new BigDecimal("175.50")));

        assertTrue(profileRepository.existsByUserId(user.getId()));
    }

    @Test
    void shouldSaveAndQueryHealthGoals() {

        UserEntity user = createUser();

        HealthGoalEntity goal = new HealthGoalEntity(
                user.getId(),
                HealthGoalEntity.GoalCategory.EXERCISE,
                "每周运动150分钟"
        );

        goal.setTargetValue(new BigDecimal("150"));
        goal.setTargetUnit("min/week");
        goal.setTargetDate(LocalDate.of(2027, 1, 31));

        goalRepository.saveAndFlush(goal);

        clearPersistenceContext();

        List<HealthGoalEntity> goals =
                goalRepository
                        .findByUserIdAndStatusOrderByCreatedAtDesc(
                                user.getId(),
                                HealthGoalEntity.GoalStatus.ACTIVE
                        );

        assertEquals(1, goals.size());
        assertEquals("每周运动150分钟", goals.get(0).getTitle());
        assertEquals(
                HealthGoalEntity.GoalCategory.EXERCISE,
                goals.get(0).getCategory()
        );
    }

    @Test
    void shouldSaveAndQueryDailyCheckins() {

        UserEntity user = createUser();

        LocalDate date = LocalDate.of(2026, 11, 10);

        HealthDailyCheckinEntity checkin =
                new HealthDailyCheckinEntity(
                        user.getId(),
                        date
                );

        checkin.setMoodScore((byte) 4);
        checkin.setStressScore((byte) 2);
        checkin.setSleepMinutes((short) 420);
        checkin.setActivityMinutes((short) 30);
        checkin.setWeightKg(new BigDecimal("72.50"));

        checkinRepository.saveAndFlush(checkin);

        clearPersistenceContext();

        HealthDailyCheckinEntity found =
                checkinRepository
                        .findByUserIdAndRecordDate(
                                user.getId(),
                                date
                        )
                        .orElseThrow();

        assertEquals((byte) 4, found.getMoodScore());
        assertEquals((short) 420, found.getSleepMinutes());

        List<HealthDailyCheckinEntity> records =
                checkinRepository
                        .findByUserIdAndRecordDateBetweenOrderByRecordDateAsc(
                                user.getId(),
                                date.minusDays(6),
                                date
                        );

        assertEquals(1, records.size());
        assertEquals(date, records.get(0).getRecordDate());
    }

    @Test
    void shouldNotFindAnotherUsersHealthData() {

        UserEntity userA = createUser();
        UserEntity userB = createUser();

        HealthGoalEntity goal = new HealthGoalEntity(
                userA.getId(),
                HealthGoalEntity.GoalCategory.SLEEP,
                "改善睡眠"
        );

        goalRepository.saveAndFlush(goal);

        clearPersistenceContext();

        // 用户A可以找到自己的目标
        assertTrue(
                goalRepository.findByIdAndUserId(
                        goal.getId(),
                        userA.getId()
                ).isPresent()
        );

        // 用户B不能通过相同的目标ID查询到它
        assertTrue(
                goalRepository.findByIdAndUserId(
                        goal.getId(),
                        userB.getId()
                ).isEmpty()
        );
    }

    @Test
    void shouldIncreaseVersionWhenUpdatingProfile() {

        UserEntity user = createUser();

        HealthProfileEntity profile =
                new HealthProfileEntity(
                        user.getId(),
                        "Asia/Shanghai"
                );

        profile.setHeightCm(new BigDecimal("170.00"));

        profileRepository.saveAndFlush(profile);

        Long initialVersion = profile.getVersion();

        profile.setHeightCm(new BigDecimal("171.00"));

        profileRepository.flush();

        assertEquals(
                initialVersion + 1,
                profile.getVersion()
        );
    }
}

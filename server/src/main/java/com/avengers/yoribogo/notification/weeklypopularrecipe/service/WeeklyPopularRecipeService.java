package com.avengers.yoribogo.notification.weeklypopularrecipe.service;

import com.avengers.yoribogo.common.exception.CommonException;
import com.avengers.yoribogo.common.exception.ErrorCode;
import com.avengers.yoribogo.notification.weeklypopularrecipe.dto.DeleteLikeInMongoEvent;
import com.avengers.yoribogo.notification.weeklypopularrecipe.dto.InsertLikeToMongoEvent;
import com.avengers.yoribogo.notification.weeklypopularrecipe.dto.WeeklyPopularRecipe;
import com.avengers.yoribogo.notification.weeklypopularrecipe.repository.WeeklyPopularRecipeMongoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@Slf4j
public class WeeklyPopularRecipeService {

    @Autowired
    private final WeeklyPopularRecipeMongoRepository weeklyPopularRecipeMongoRepository;

    public WeeklyPopularRecipeService(WeeklyPopularRecipeMongoRepository weeklyPopularRecipeMongoRepository) {
        this.weeklyPopularRecipeMongoRepository = weeklyPopularRecipeMongoRepository;
    }

    // 필기. 레시피 전체 조회 ( 테스트용 )
    public List<WeeklyPopularRecipe> getAllWeeklyPopularRecipes() {
        return Optional.of(weeklyPopularRecipeMongoRepository.findAll())
                .filter(recipes -> !recipes.isEmpty())
                .orElseThrow(() -> new CommonException(ErrorCode.NOT_FOUND_RECIPE));
    }

    // 필기. 레시피 mostLiked 조회
    public WeeklyPopularRecipe getRandomTopLikedRecipe() {

        // 7일 전 날짜 계산 (한국 시간으로 변경)
        LocalDateTime sevenDaysAgo = ZonedDateTime.now(ZoneId.of("Asia/Seoul"))
                .minusDays(7)
                .withNano(0)
                .toLocalDateTime();

        // 7일 이내의 모든 레시피 조회
        List<WeeklyPopularRecipe> recentRecipes = weeklyPopularRecipeMongoRepository.findByCreatedAtAfter(sevenDaysAgo)
                .orElseThrow(() -> new CommonException(ErrorCode.NOT_FOUND_RECIPE));

        // recipe_id 기준으로 그룹화하고 카운트
        Map<String, Long> recipeIdCounts = recentRecipes.stream()
                .collect(Collectors.groupingBy(WeeklyPopularRecipe::getMyRecipeId, Collectors.counting()));

        // 상위 3개 recipe_id 선택
        List<String> topRecipeIds = recipeIdCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (topRecipeIds.isEmpty()) {
            throw new CommonException(ErrorCode.NOT_FOUND_RECIPE);
        }

        // 상위 3개 중 랜덤 선택
        String randomRecipeId = topRecipeIds.get(new Random().nextInt(topRecipeIds.size()));
        log.info("알림 보낼 최종 레시피 선택됨");

        // 선택된 recipe_id에 해당하는 레시피 중 하나를 랜덤하게 반환
        List<WeeklyPopularRecipe> selectedRecipes = recentRecipes.stream()
                .filter(recipe -> recipe.getMyRecipeId().equals(randomRecipeId))
                .collect(Collectors.toList());

        if (selectedRecipes.isEmpty()) {
            throw new CommonException(ErrorCode.NOT_FOUND_RECIPE);
        }

        return selectedRecipes.get(new Random().nextInt(selectedRecipes.size()));
    }

    // 필기. mongodb에 insert 하기 위한 메서드 ( 단, 좋아요 트랜젝션 커밋 이후 실행 )
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void InsertLikeDataToMongo(InsertLikeToMongoEvent event) {

        WeeklyPopularRecipe newLike = new WeeklyPopularRecipe();

        newLike.setUserId(event.getUserId());
        newLike.setMyRecipeId(event.getPostId());

        // 한국 시간으로 변환된 LocalDateTime 사용
        LocalDateTime koreanTime = ZonedDateTime.now(ZoneId.of("Asia/Seoul"))
                .withNano(0)
                .toLocalDateTime();
        newLike.setCreatedAt(koreanTime);

        weeklyPopularRecipeMongoRepository.save(newLike);
        log.info("MongoDB에 좋아요 데이터가 삽입되었습니다. 시간: {}", koreanTime);
    }

    // 필기. 기존에 있던 좋아요를 취소하는 메서드 ( 단, 좋아요 트랜젝션 커밋 이후 실행 )
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void DeleteByUserIdAndMyRecipeId(DeleteLikeInMongoEvent event) {
        weeklyPopularRecipeMongoRepository.deleteByUserIdAndMyRecipeId(event.getUserId(), event.getPostId());
        log.info("MongoDB에 좋아요 데이터가 삭제되었습니다");
    }
}

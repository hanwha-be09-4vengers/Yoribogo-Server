package com.avengers.yoribogo.notification.notification.domain;

import com.avengers.yoribogo.common.exception.CommonException;
import com.avengers.yoribogo.common.exception.ErrorCode;
import com.avengers.yoribogo.notification.notification.service.NotificationService;
import com.avengers.yoribogo.notification.weeklypopularrecipe.dto.WeeklyPopularRecipe;
import com.avengers.yoribogo.notification.weeklypopularrecipe.service.WeeklyPopularRecipeService;
import com.avengers.yoribogo.recipeboard.domain.RecipeBoard;
import com.avengers.yoribogo.recipeboard.repository.RecipeBoardRepository; // 레시피 조회를 위한 Repository
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationScheduler {

    private final WeeklyPopularRecipeService weeklyPopularRecipeService;
    private final NotificationService notificationService;
    private final RecipeBoardRepository recipeBoardRepository; // 레시피 조회를 위한 Repository

    @Autowired
    public NotificationScheduler(WeeklyPopularRecipeService weeklyPopularRecipeService,
                                 NotificationService notificationService,
                                 RecipeBoardRepository recipeBoardRepository) {
        this.weeklyPopularRecipeService = weeklyPopularRecipeService;
        this.notificationService = notificationService;
        this.recipeBoardRepository = recipeBoardRepository;
    }

//    @Scheduled(fixedRate = 900000000)
    @Scheduled(cron = "0 0 11 * * ?") // 매일 11:00에 실행
    public void sendLunchRecipeNotification() {
        sendRecipeNotification("lunch");
    }

//    @Scheduled(fixedRate = 90000000)
    @Scheduled(cron = "0 0 17 * * ?") // 매일 17:00에 실행
    public void sendDinnerRecipeNotification() {
        sendRecipeNotification("dinner");
    }

    // 레시피 알림 전송 메서드
    private void sendRecipeNotification(String mealType) {
        // 좋아요가 가장 많은 레시피 가져오기
        WeeklyPopularRecipe mostLikedRecipe = weeklyPopularRecipeService.getRandomTopLikedRecipe();

        // 레시피 ID로 RecipeBoard 객체 조회
        RecipeBoard recipe = recipeBoardRepository.findById(Long.parseLong(mostLikedRecipe.getMyRecipeId()))
                .orElseThrow(() -> new CommonException(ErrorCode.NOT_FOUND_RECIPE));

        // 알림 메시지 설정 (점심/저녁에 따른 메시지 변경)
        String message;
        if ("lunch".equals(mealType)) {
            message = "🍽️ [요리 시간 알림] 🍽️\n\n점심으로 추천드리는 레시피는: " + recipe.getRecipeBoardMenuName() + " 입니다! 🍳";
        } else {
            message = "🍴 [저녁 요리 추천] 🍴\n\n저녁 메뉴로 추천드리는 레시피는: " + recipe.getRecipeBoardMenuName() + " 입니다! 🍕";
        }

        // 알림 전송 (SSE 방식으로 클라이언트에 전송)
        notificationService.sendNotificationToLoggedInUsers(message);
    }
}
